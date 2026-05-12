#!/usr/bin/env python3
"""Generator: ice_wyrm_orbital.bbmodel — FreezingIce attack 19 'Serpent Below'.

A sinuous ice serpent circling at ground level — only dorsal plates and
occasional head visible. The implied body beneath ice is the horror.
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
    osc_scale_kfs, osc_rot_kfs,
)

OUTPUT = "D:/CC/ChaosCraft/src/main/resources/models/freezingice/me_attacks/ice_wyrm_orbital.bbmodel"
W = H = 128


def tex_scale():
    """Ice wyrm scale — overlapping semi-circle bright zones, biological irregular."""
    deep    = hex_color("#04121e")
    shadow  = hex_color("#0c2840")
    surface = hex_color("#2868a0")
    edge    = hex_color("#70b8e0")
    spec    = hex_color("#c8f0ff")
    emiss   = hex_color("#50a8e0")
    pixels = new_pixels(W, H, deep)
    # Base scale gradient
    for y in range(H):
        t = y / float(H)
        if t < 0.4:
            col = shadow
        elif t < 0.75:
            tt = (t - 0.4) / 0.35
            col = (shadow[0] + int((surface[0] - shadow[0]) * tt),
                   shadow[1] + int((surface[1] - shadow[1]) * tt),
                   shadow[2] + int((surface[2] - shadow[2]) * tt), 255)
        else:
            tt = (t - 0.75) / 0.25
            col = (surface[0] + int((edge[0] - surface[0]) * tt),
                   surface[1] + int((edge[1] - surface[1]) * tt),
                   surface[2] + int((edge[2] - surface[2]) * tt), 255)
        for x in range(W):
            pixels[y * W + x] = col
    # Biological scales — overlapping semi-circles at irregular intervals
    rng = random.Random(606)
    for _ in range(35):
        cx = rng.randrange(W)
        cy = rng.randrange(H)
        r  = rng.randrange(6, 14)
        # Semi-circle bright zone (only upper half)
        for dy in range(-r, 0):
            for dx in range(-r, r + 1):
                d = math.sqrt(dx * dx + dy * dy)
                if d <= r:
                    x, y = cx + dx, cy + dy
                    if 0 <= x < W and 0 <= y < H:
                        t = 1.0 - d / r
                        p = pixels[y * W + x]
                        pixels[y * W + x] = (
                            min(255, p[0] + int(edge[0] * t * 0.5)),
                            min(255, p[1] + int(edge[1] * t * 0.5)),
                            min(255, p[2] + int(edge[2] * t * 0.5)), 255,
                        )
    # Bright edge highlights at top of each scale
    rng2 = random.Random(707)
    for _ in range(25):
        x0 = rng2.randrange(W)
        y0 = rng2.randrange(H)
        length = rng2.randrange(5, 15)
        for k in range(length):
            x = (x0 + k) % W
            y = y0
            if 0 <= y < H:
                p = pixels[y * W + x]
                pixels[y * W + x] = (
                    min(255, p[0] + 40), min(255, p[1] + 50), min(255, p[2] + 60), 255,
                )
    specular_blob(pixels, W, H, 40, 30, spec, radius=6, strength=0.6)
    random_bright_specks(pixels, W, H, 100, emiss, seed=11, size_max=1)
    random_bright_specks(pixels, W, H, 60, edge, seed=22, size_max=2)
    per_pixel_jitter(pixels, W, H, magnitude=14, seed=33, density=0.7)
    return png_from_pixels(pixels, W, H)


def tex_disturbance():
    """Cracked ice dark with bright crack lines radiating outward."""
    dark   = hex_color("#060c12")
    bright = hex_color("#90d0e8")
    spec   = hex_color("#ffffff")
    pixels = new_pixels(W, H, dark)
    # Crack lines from centre radiating outward
    cx, cy = W / 2, H / 2
    rng = random.Random(808)
    for _ in range(40):
        ang = rng.uniform(0, 2 * math.pi)
        length = rng.randrange(20, 60)
        x, y = cx, cy
        for k in range(length):
            x += math.cos(ang)
            y += math.sin(ang)
            xi, yi = int(x), int(y)
            if 0 <= xi < W and 0 <= yi < H:
                pixels[yi * W + xi] = bright
            # Branch occasionally
            if k % 8 == 0:
                ang += rng.uniform(-0.4, 0.4)
    specular_blob(pixels, W, H, W // 2, H // 2, spec, radius=8, strength=0.7)
    random_bright_specks(pixels, W, H, 100, bright, seed=44, size_max=1)
    per_pixel_jitter(pixels, W, H, magnitude=14, seed=55, density=0.7)
    return png_from_pixels(pixels, W, H)


def build():
    b = Builder("ice_wyrm_orbital", resolution=(W, H), visible_box=(22, 6, 0))
    b.add_texture("wyrm_scale", tex_scale())
    b.add_texture("disturbance", tex_disturbance())

    F0 = lambda: uniform_face(0, 0, W, H, tex_index=0)
    F1 = lambda: uniform_face(0, 0, W, H, tex_index=1)

    ORBIT_R = 10.0
    PLATE_Y = 0.2

    # ---- 8 dorsal plate bone groups around orbit, varying size ----
    plates = []  # list of (gu, g, idx)
    # Plate sizes: small -> large at middle -> small (head idx 0)
    plate_sizes = [1.2, 1.5, 1.8, 2.0, 2.0, 1.8, 1.5, 1.2]
    for i, sz in enumerate(plate_sizes):
        ang = (i / 8.0) * 2 * math.pi
        px = math.cos(ang) * ORBIT_R
        pz = math.sin(ang) * ORBIT_R
        # 2-3 overlapping flat slabs, tilted inward (toward body direction)
        plate_cubes = []
        for s in range(3):
            cube = b.add_cube(
                f"plate{i+1}_slab{s+1}",
                from_=[-sz * 0.6, -0.05 + s * 0.05, -sz * 0.4 + s * 0.1],
                to_=[sz * 0.6, 0.15 + s * 0.05, sz * 0.4 + s * 0.1],
                faces=F0(),
                rotation=[0, 0, (s - 1) * 8],  # tilt inward
            )
            plate_cubes.append(cube)
        gu = make_uuid()
        # The plate "faces" tangent — rotate Y by angle+90
        g = b.make_group(
            f"plate_{i+1}", [px, PLATE_Y, pz], plate_cubes,
            rotation=[8, math.degrees(ang) + 90, 0], group_uuid=gu,
        )
        plates.append((gu, g, i))

    # ---- 8 ground disturbance crack slabs around each plate position ----
    disturb_segs = []
    for i in range(8):
        ang = (i / 8.0) * 2 * math.pi
        px = math.cos(ang) * ORBIT_R
        pz = math.sin(ang) * ORBIT_R
        cube = b.add_cube(
            f"disturb_{i+1}",
            from_=[-2.0, 0.0, -2.0], to_=[2.0, 0.08, 2.0],
            faces=F1(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"disturb_grp_{i+1}", [px, 0, pz], [cube],
            rotation=[0, math.degrees(ang) + 90, 0], group_uuid=gu,
        )
        disturb_segs.append((gu, g))

    # ---- Head emergence at "front" of orbit (angle=0) ----
    head_cubes = []
    # 4 main cubes for head shape
    head_cubes.append(b.add_cube(
        "head_main", from_=[-1.0, -0.3, -1.5], to_=[1.0, 0.8, 0.5], faces=F0(),
    ))
    head_cubes.append(b.add_cube(
        "head_snout", from_=[-0.6, 0.0, 0.5], to_=[0.6, 0.5, 1.5], faces=F0(),
    ))
    head_cubes.append(b.add_cube(
        "head_crown", from_=[-0.8, 0.7, -1.2], to_=[0.8, 1.2, -0.4], faces=F0(),
    ))
    head_cubes.append(b.add_cube(
        "head_lower", from_=[-0.8, -0.5, -1.2], to_=[0.8, -0.3, 0.6], faces=F0(),
    ))
    # 2 eye flat slabs
    head_cubes.append(b.add_cube(
        "head_eye_L", from_=[-0.9, 0.1, -0.4], to_=[-0.5, 0.4, -0.0], faces=F1(),
    ))
    head_cubes.append(b.add_cube(
        "head_eye_R", from_=[0.5, 0.1, -0.4], to_=[0.9, 0.4, -0.0], faces=F1(),
    ))
    # 2 jaw flat slabs
    head_cubes.append(b.add_cube(
        "head_jaw_top", from_=[-0.5, 0.0, 0.9], to_=[0.5, 0.15, 1.4], faces=F0(),
    ))
    head_cubes.append(b.add_cube(
        "head_jaw_bot", from_=[-0.5, -0.25, 0.9], to_=[0.5, -0.15, 1.4], faces=F0(),
    ))
    head_gu = make_uuid()
    head_g = b.make_group(
        "head", [ORBIT_R, PLATE_Y + 0.5, 0], head_cubes,
        rotation=[0, 90, 0], group_uuid=head_gu,
    )

    # ---- Wake bubbles: 8 small flat cubes trailing behind head ----
    wake_segs = []
    for i in range(8):
        # Place along orbit behind head (i.e., at angles slightly less than 0)
        ang = -0.15 * (i + 1)  # behind head in orbit direction
        wx = math.cos(ang) * ORBIT_R
        wz = math.sin(ang) * ORBIT_R
        sz_ = max(0.15, 0.4 - i * 0.025)
        cube = b.add_cube(
            f"wake_{i+1}",
            from_=[-sz_, -sz_ * 0.5, -sz_], to_=[sz_, sz_ * 0.5, sz_],
            faces=F1(),
        )
        gu = make_uuid()
        g = b.make_group(f"wake_grp_{i+1}", [wx, PLATE_Y, wz], [cube], group_uuid=gu)
        wake_segs.append((gu, g))

    # ---- ROOT ----
    children = [g for (_u, g, _i) in plates]
    children += [g for (_u, g) in disturb_segs + wake_segs]
    children.append(head_g)
    root_uuid = make_uuid()
    root = b.make_group("root", [0, 0, 0], children, group_uuid=root_uuid)
    b.outliner.append(root)

    # ---- SPAWN: 1.2s ----
    spawn = {}
    # Disturbance cracks at 0.2s in sequence
    for i, (gu, g) in enumerate(disturb_segs):
        t0 = 0.20 + i * 0.05
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=1, z=0),
            make_keyframe("scale", t0, x=0, y=1, z=0),
            make_keyframe("scale", t0 + 0.10, x=1, y=1, z=1),
            make_keyframe("scale", 1.20, x=1, y=1, z=1),
        ]}
    # Dorsal plates emerge from Y=-1 one by one at 0.3s
    for i, (gu, g, idx) in enumerate(plates):
        t0 = 0.30 + i * 0.06
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=-1, z=0),
            make_keyframe("position", t0, x=0, y=-1, z=0),
            make_keyframe("position", t0 + 0.12, x=0, y=0, z=0),
            make_keyframe("scale", 0.0, x=1, y=0, z=1),
            make_keyframe("scale", t0, x=1, y=0, z=1),
            make_keyframe("scale", t0 + 0.12, x=1, y=1, z=1),
            make_keyframe("scale", 1.20, x=1, y=1, z=1),
        ]}
    # Head at 0.8s with scale overshoot
    spawn[head_gu] = {"name": head_g["name"], "type": "bone", "keyframes": [
        make_keyframe("position", 0.0, x=0, y=-2, z=0),
        make_keyframe("position", 0.80, x=0, y=-2, z=0),
        make_keyframe("position", 0.95, x=0, y=0.3, z=0),
        make_keyframe("position", 1.05, x=0, y=0, z=0),
        make_keyframe("scale", 0.0, x=1, y=0, z=1),
        make_keyframe("scale", 0.80, x=1, y=0, z=1),
        make_keyframe("scale", 0.92, x=1.2, y=1.2, z=1.2),
        make_keyframe("scale", 1.00, x=1, y=1, z=1),
        make_keyframe("scale", 1.20, x=1, y=1, z=1),
    ]}
    # Wake bubbles
    for i, (gu, g) in enumerate(wake_segs):
        t0 = 1.00 + i * 0.02
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.05, x=1, y=1, z=1),
            make_keyframe("scale", 1.20, x=1, y=1, z=1),
        ]}
    b.add_animation("spawn", length=1.20, animators=spawn, loop="once", override=True)

    # ---- IDLE: 5.0s — orbital rotation + body undulation ----
    idle = {}
    # Root rotates the entire wyrm around Y axis (5s per orbit)
    idle[root_uuid] = {"name": "root", "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, x=0, y=0, z=0),
        make_keyframe("rotation", 1.25, x=0, y=90, z=0),
        make_keyframe("rotation", 2.5, x=0, y=180, z=0),
        make_keyframe("rotation", 3.75, x=0, y=270, z=0),
        make_keyframe("rotation", 5.0, x=0, y=360, z=0),
    ]}
    # Each dorsal plate undulates Y ±0.3
    for i, (gu, g, idx) in enumerate(plates):
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            *[make_keyframe("position", t, x=0,
                            y=0.3 * math.sin((t / 1.5 + idx * 0.4) * 2 * math.pi),
                            z=0)
              for t in [0, 0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0,
                        2.25, 2.5, 2.75, 3.0, 3.25, 3.5, 3.75, 4.0,
                        4.25, 4.5, 4.75, 5.0]],
        ]}
    # Head submerges and re-emerges at same point each orbit
    head_kfs = []
    for t in [0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0]:
        # Submerge at t=2.5, re-emerge at t=5.0 (same orbit position)
        phase = (t / 5.0) * 2 * math.pi
        # Submerge halfway through
        if t < 2.5:
            yy = 0
        elif t < 3.5:
            yy = -2.5  # submerged
        elif t < 4.0:
            yy = -2.5
        else:
            yy = 0  # re-emerging
        head_kfs.append(make_keyframe("position", t, x=0, y=yy, z=0))
    idle[head_gu] = {"name": head_g["name"], "type": "bone", "keyframes": head_kfs}
    # Wake bubbles follow head — rotate slowly
    for i, (gu, g) in enumerate(wake_segs):
        kfs = osc_pos_kfs_y = []
        for t in [0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0]:
            phase = (t / 1.0 + i * 0.3) * 2 * math.pi
            yy = 0.2 * math.sin(phase)
            kfs.append(make_keyframe("position", t, x=0, y=yy, z=0))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}
    # Disturbance cracks pulse
    for i, (gu, g) in enumerate(disturb_segs):
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_scale_kfs(5.0, 24, base=1.0, amp=0.08,
                                               period=1.5, phase=i * 0.6)}
    b.add_animation("idle", length=5.0, animators=idle, loop="loop", override=False)

    # ---- DISSIPATE: 0.8s — head submerges, plates retract in sequence ----
    diss = {}
    diss[head_gu] = {"name": head_g["name"], "type": "bone", "keyframes": [
        make_keyframe("position", 0.0, x=0, y=0, z=0),
        make_keyframe("position", 0.20, x=0, y=-2, z=0),
        make_keyframe("scale", 0.0, x=1, y=1, z=1),
        make_keyframe("scale", 0.20, x=1, y=0, z=1),
        make_keyframe("scale", 0.80, x=0, y=0, z=0),
    ]}
    for i, (gu, g, idx) in enumerate(plates):
        t0 = 0.20 + i * 0.06
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", t0, x=0, y=0, z=0),
            make_keyframe("position", t0 + 0.10, x=0, y=-1, z=0),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", t0, x=1, y=1, z=1),
            make_keyframe("scale", t0 + 0.10, x=1, y=0, z=1),
            make_keyframe("scale", 0.80, x=0, y=0, z=0),
        ]}
    for i, (gu, g) in enumerate(disturb_segs):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.80, x=0, y=0, z=0),
        ]}
    for i, (gu, g) in enumerate(wake_segs):
        ang = -0.15 * (i + 1)
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.80, x=math.cos(ang) * 3, y=1, z=math.sin(ang) * 3),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.80, x=0, y=0, z=0),
        ]}
    b.add_animation("dissipate", length=0.80, animators=diss, loop="once", override=True)

    b.write(OUTPUT)
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    build()
