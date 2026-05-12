#!/usr/bin/env python3
"""Generator: frost_breath_beam.bbmodel — FreezingIce attack 14 'Exhale'.

A sustained beam of condensed cold breath — wide flat, rolling like fog.
"""

import sys
import math
import random

sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder, make_uuid, make_keyframe, uniform_face, png_from_pixels,
)
from _freezingice_common import (
    new_pixels, radial_zone, horiz_band_fade, per_pixel_jitter,
    specular_blob, random_bright_specks, hex_color, fill_rect,
    osc_scale_kfs, osc_rot_kfs, osc_pos_kfs,
)

OUTPUT = "D:/CC/ChaosCraft/src/main/resources/models/freezingice/me_attacks/frost_breath_beam.bbmodel"
W = H = 128


def tex_beam():
    """Silvery-white-blue beam with bright centre strip."""
    pale     = hex_color("#c8dce8")
    mid      = hex_color("#889ab0")
    shadow   = hex_color("#304050")
    bright   = hex_color("#e8f4ff")
    emiss    = hex_color("#d0ecff")
    pixels   = new_pixels(W, H, pale)
    # Vertical gradient: shadow at edges, bright centre band on Y
    for y in range(H):
        dy = abs(y - H / 2)
        t = min(1.0, dy / (H * 0.4))
        if t < 0.2:
            col = bright
        elif t < 0.5:
            tt = (t - 0.2) / 0.3
            col = (bright[0] + int((pale[0] - bright[0]) * tt),
                   bright[1] + int((pale[1] - bright[1]) * tt),
                   bright[2] + int((pale[2] - bright[2]) * tt), 255)
        elif t < 0.8:
            tt = (t - 0.5) / 0.3
            col = (pale[0] + int((mid[0] - pale[0]) * tt),
                   pale[1] + int((mid[1] - pale[1]) * tt),
                   pale[2] + int((mid[2] - pale[2]) * tt), 255)
        else:
            tt = (t - 0.8) / 0.2
            col = (mid[0] + int((shadow[0] - mid[0]) * tt),
                   mid[1] + int((shadow[1] - mid[1]) * tt),
                   mid[2] + int((shadow[2] - mid[2]) * tt), 255)
        for x in range(W):
            pixels[y * W + x] = col
    # Soft fog horizontal pattern — random blobs of bright
    rng = random.Random(909)
    for _ in range(60):
        cx = rng.randrange(W)
        cy = rng.randrange(H)
        r = rng.randrange(4, 12)
        for dy in range(-r, r + 1):
            for dx in range(-r, r + 1):
                d = math.sqrt(dx * dx + dy * dy)
                if d <= r:
                    x, y = cx + dx, cy + dy
                    if 0 <= x < W and 0 <= y < H:
                        t = 1.0 - d / r
                        p = pixels[y * W + x]
                        pixels[y * W + x] = (
                            min(255, p[0] + int(40 * t)),
                            min(255, p[1] + int(40 * t)),
                            min(255, p[2] + int(40 * t)), 255,
                        )
    random_bright_specks(pixels, W, H, 80, emiss, seed=33, size_max=1)
    per_pixel_jitter(pixels, W, H, magnitude=18, seed=44, density=0.85)
    return png_from_pixels(pixels, W, H)


def tex_frost():
    """Pure white frost with pale blue edges."""
    base   = hex_color("#f4f8f8")
    edge   = hex_color("#90b8c8")
    spec   = hex_color("#ffffff")
    pixels = new_pixels(W, H, base)
    radial_zone(pixels, W, H, W / 2, H / 2, base, edge, W * 0.6)
    # Crystalline frost lines
    rng = random.Random(123)
    for _ in range(30):
        x0 = rng.randrange(W)
        y0 = rng.randrange(H)
        ang = rng.uniform(0, 2 * math.pi)
        length = rng.randrange(10, 24)
        for k in range(length):
            x = int(x0 + math.cos(ang) * k)
            y = int(y0 + math.sin(ang) * k)
            if 0 <= x < W and 0 <= y < H:
                p = pixels[y * W + x]
                pixels[y * W + x] = (
                    min(255, p[0] + 30), min(255, p[1] + 30), min(255, p[2] + 40), 255,
                )
    specular_blob(pixels, W, H, 30, 30, spec, radius=8, strength=0.7)
    specular_blob(pixels, W, H, 95, 95, spec, radius=6, strength=0.5)
    random_bright_specks(pixels, W, H, 180, spec, seed=22, size_max=2)
    per_pixel_jitter(pixels, W, H, magnitude=16, seed=55, density=0.85)
    return png_from_pixels(pixels, W, H)


def build():
    b = Builder("frost_breath_beam", resolution=(W, H), visible_box=(16, 8, 0))
    b.add_texture("breath_beam", tex_beam())
    b.add_texture("breath_frost", tex_frost())

    F0 = lambda: uniform_face(0, 0, W, H, tex_index=0)
    F1 = lambda: uniform_face(0, 0, W, H, tex_index=1)

    BEAM_LEN = 16.0
    # ---- 5 horizontal flat slab beam layers ----
    beam_layers = []
    for i in range(5):
        y_off = (i - 2) * 0.4
        # widens toward impact end (Z+): taper reversed (wider on Z+)
        # easier: build with from/to mid-X, mid-Y, +Z extending
        cube = b.add_cube(
            f"beam_layer_{i+1}",
            from_=[-2.0, y_off - 0.15, 0.0],
            to_=[2.8, y_off + 0.15, BEAM_LEN],
            faces=F0(),
        )
        gu = make_uuid()
        g = b.make_group(f"beam_layer_grp_{i+1}", [0, y_off, BEAM_LEN / 2],
                         [cube], group_uuid=gu)
        beam_layers.append((gu, g))

    # ---- 8 frost accumulation slabs at impact end (Z=BEAM_LEN) ----
    frost_segs = []
    for i in range(8):
        ang = (i / 8.0) * 2 * math.pi
        cx = math.cos(ang) * 2.2
        cz = BEAM_LEN + 0.5 + math.sin(ang) * 1.5
        cube = b.add_cube(
            f"frost_acc_{i+1}",
            from_=[-0.8, -0.1, -0.6], to_=[0.8, 0.1, 0.6],
            faces=F1(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"frost_acc_grp_{i+1}", [cx, 0, cz], [cube],
            rotation=[0, math.degrees(ang) + 90, i * 7], group_uuid=gu,
        )
        frost_segs.append((gu, g))

    # ---- 6 condensation rolls distributed along beam ----
    roll_segs = []
    for i in range(6):
        zc = 1.5 + i * 2.3
        cube = b.add_cube(
            f"roll_{i+1}",
            from_=[-2.5, -0.5, -0.5], to_=[2.5, 0.5, 0.5],
            faces=F0(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"roll_grp_{i+1}", [0, 0, zc], [cube],
            rotation=[i * 6, 0, i * 4], group_uuid=gu,
        )
        roll_segs.append((gu, g))

    # ---- 5 concentric emitter oval slabs at beam origin ----
    emitter_segs = []
    for i in range(5):
        r = 0.5 + i * 0.3
        cube = b.add_cube(
            f"emitter_{i+1}",
            from_=[-r, -r * 0.7, -0.1], to_=[r, r * 0.7, 0.1],
            faces=F1(),
        )
        gu = make_uuid()
        g = b.make_group(f"emitter_grp_{i+1}", [0, 0, -0.2 - i * 0.1],
                         [cube], group_uuid=gu)
        emitter_segs.append((gu, g))

    # ---- 8 impact burst fan slabs at far end ----
    burst_segs = []
    for i in range(8):
        ang = (i / 8.0) * 2 * math.pi
        cube = b.add_cube(
            f"burst_{i+1}",
            from_=[-1.5, -0.05, -0.4], to_=[1.5, 0.05, 0.4],
            faces=F1(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"burst_grp_{i+1}", [0, 0, BEAM_LEN + 0.8], [cube],
            rotation=[0, 0, math.degrees(ang)], group_uuid=gu,
        )
        burst_segs.append((gu, g))

    # ---- ROOT ----
    children = [g for (_u, g) in beam_layers + roll_segs + emitter_segs + frost_segs + burst_segs]
    root_uuid = make_uuid()
    root = b.make_group("root", [0, 0, 0], children, group_uuid=root_uuid)
    b.outliner.append(root)

    # ---- SPAWN: 0.6s ----
    spawn = {}
    # Emitter opens at 0.0s
    for i, (gu, g) in enumerate(emitter_segs):
        t0 = i * 0.015
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0.05, y=0.05, z=0.05),
            make_keyframe("scale", t0, x=0.05, y=0.05, z=0.05),
            make_keyframe("scale", t0 + 0.10, x=1, y=1, z=1),
            make_keyframe("scale", 0.60, x=1, y=1, z=1),
        ]}
    # Beam layers extend from emitter outward 0.05s per layer
    for i, (gu, g) in enumerate(beam_layers):
        t0 = 0.05 + i * 0.05
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=0.05),
            make_keyframe("scale", t0, x=1, y=1, z=0.05),
            make_keyframe("scale", t0 + 0.20, x=1, y=1, z=1),
            make_keyframe("scale", 0.60, x=1, y=1, z=1),
        ]}
    # Condensation rolls materialise along beam at 0.3s
    for i, (gu, g) in enumerate(roll_segs):
        t0 = 0.30 + i * 0.02
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.08, x=1, y=1, z=1),
            make_keyframe("scale", 0.60, x=1, y=1, z=1),
        ]}
    # Frost accumulation spreads at 0.4s
    for i, (gu, g) in enumerate(frost_segs):
        t0 = 0.40 + i * 0.02
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.10, x=1, y=1, z=1),
            make_keyframe("scale", 0.60, x=1, y=1, z=1),
        ]}
    # Impact burst opens at 0.5s
    for i, (gu, g) in enumerate(burst_segs):
        t0 = 0.50 + i * 0.005
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.08, x=1, y=1, z=1),
            make_keyframe("scale", 0.60, x=1, y=1, z=1),
        ]}
    b.add_animation("spawn", length=0.60, animators=spawn, loop="once", override=True)

    # ---- IDLE: 2.5s ----
    idle = {}
    # Each beam layer oscillates Y on staggered phase
    for i, (gu, g) in enumerate(beam_layers):
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_pos_kfs(2.5, 20, amp_x=0, amp_y=0.4,
                                             amp_z=0, period=2.0, phase=i * 0.3)}
    # Condensation rolls drift Z (looping forward)
    for i, (gu, g) in enumerate(roll_segs):
        kfs = []
        steps = 20
        for k in range(steps + 1):
            t = (k / steps) * 2.5
            # Loop Z by period
            zoff = (t * 1.5 + i * 0.4) % 2.3 - 1.15
            kfs.append(make_keyframe("position", t, x=0, y=0, z=zoff))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}
    # Frost accumulation slowly expands
    for i, (gu, g) in enumerate(frost_segs):
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_scale_kfs(2.5, 16, base=1.05, amp=0.05,
                                               period=2.0, phase=i * 0.25)}
    # Emitter pulses
    for i, (gu, g) in enumerate(emitter_segs):
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_scale_kfs(2.5, 18, base=1.0, amp=0.10,
                                               period=1.5, phase=i * 0.2)}
    # Burst gentle motion
    for i, (gu, g) in enumerate(burst_segs):
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_scale_kfs(2.5, 14, base=1.0, amp=0.08,
                                               period=1.8, phase=i * 0.18)}
    b.add_animation("idle", length=2.5, animators=idle, loop="loop", override=False)

    # ---- DISSIPATE: 0.4s — beam scales to 0 from impact end backward ----
    diss = {}
    for i, (gu, g) in enumerate(beam_layers):
        # later layers don't scale here — beam scales from impact end, which is +Z end
        # We'll have all beam layers retract z
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.40, x=1, y=0.05, z=0.0),
        ]}
    # Frost accumulation HOLDS (frozen in place)
    for i, (gu, g) in enumerate(frost_segs):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.40, x=1, y=1, z=1),
        ]}
    for i, (gu, g) in enumerate(roll_segs):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.40, x=0, y=0, z=-3),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.40, x=0, y=0, z=0),
        ]}
    for i, (gu, g) in enumerate(emitter_segs):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.40, x=0.05, y=0.05, z=0.05),
        ]}
    for i, (gu, g) in enumerate(burst_segs):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.20, x=2, y=2, z=2),
            make_keyframe("scale", 0.40, x=0, y=0, z=0),
        ]}
    b.add_animation("dissipate", length=0.40, animators=diss, loop="once", override=True)

    b.write(OUTPUT)
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    build()
