#!/usr/bin/env python3
"""Generator: frozen_comet_me.bbmodel — FreezingIce attack 13 'Oort Cloud'.

A piece of space ice — comet nucleus, coma haze, ion tail, dust tail, surface pits.
"""

import sys
import math
import random

sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder, make_uuid, make_keyframe, uniform_face, png_from_pixels,
)
from _freezingice_common import (
    new_pixels, radial_zone, per_pixel_jitter, specular_blob,
    random_bright_specks, hex_color, fill_rect,
    osc_scale_kfs, osc_rot_kfs,
)

OUTPUT = "D:/CC/ChaosCraft/src/main/resources/models/freezingice/me_attacks/frozen_comet_me.bbmodel"
W = H = 128


def tex_nucleus():
    """Space ice — extremely dark, sparse bright sublimation zones."""
    black     = hex_color("#020408")
    space_grey = hex_color("#0c1018")
    dirty     = hex_color("#2a3848")
    bright    = hex_color("#5088b0")
    emiss     = hex_color("#90c8f0")
    pixels = new_pixels(W, H, black)
    # Mostly dark base
    for y in range(H):
        for x in range(W):
            t = (x + y) / float(W + H)
            if (x * 13 + y * 7) % 19 < 6:
                pixels[y * W + x] = space_grey
            elif (x * 5 + y * 11) % 23 < 4:
                pixels[y * W + x] = dirty
            else:
                pixels[y * W + x] = black
    # Sparse bright pseudo-random patches
    rng = random.Random(2021)
    for _ in range(40):
        cx = rng.randrange(W)
        cy = rng.randrange(H)
        r = rng.randrange(2, 6)
        for dy in range(-r, r + 1):
            for dx in range(-r, r + 1):
                if dx * dx + dy * dy <= r * r:
                    x, y = cx + dx, cy + dy
                    if 0 <= x < W and 0 <= y < H:
                        d = math.sqrt(dx * dx + dy * dy)
                        t = 1.0 - d / r
                        p = pixels[y * W + x]
                        pixels[y * W + x] = (
                            min(255, p[0] + int(bright[0] * t * 0.7)),
                            min(255, p[1] + int(bright[1] * t * 0.7)),
                            min(255, p[2] + int(bright[2] * t * 0.7)),
                            255,
                        )
    random_bright_specks(pixels, W, H, 80, emiss, seed=33, size_max=1)
    random_bright_specks(pixels, W, H, 30, bright, seed=44, size_max=2)
    per_pixel_jitter(pixels, W, H, magnitude=14, seed=55, density=0.7)
    return png_from_pixels(pixels, W, H)


def tex_tail():
    """Ion tail — bright centre strip on black."""
    centre    = hex_color("#b0e0ff")
    mid       = hex_color("#4080b0")
    deep      = hex_color("#060e18")
    spec      = hex_color("#ffffff")
    pixels = new_pixels(W, H, deep)
    # Bright vertical centre column
    for y in range(H):
        for x in range(W):
            dx = abs(x - W / 2)
            t = min(1.0, dx / (W * 0.25))
            if t < 0.3:
                col = centre
            elif t < 0.65:
                col = (centre[0] + int((mid[0] - centre[0]) * (t - 0.3) / 0.35),
                       centre[1] + int((mid[1] - centre[1]) * (t - 0.3) / 0.35),
                       centre[2] + int((mid[2] - centre[2]) * (t - 0.3) / 0.35), 255)
            else:
                col = (mid[0] + int((deep[0] - mid[0]) * (t - 0.65) / 0.35),
                       mid[1] + int((deep[1] - mid[1]) * (t - 0.65) / 0.35),
                       mid[2] + int((deep[2] - mid[2]) * (t - 0.65) / 0.35), 255)
            pixels[y * W + x] = col
    specular_blob(pixels, W, H, W // 2, 30, spec, radius=4, strength=0.85)
    specular_blob(pixels, W, H, W // 2, 90, spec, radius=4, strength=0.6)
    random_bright_specks(pixels, W, H, 60, spec, seed=11, size_max=1)
    per_pixel_jitter(pixels, W, H, magnitude=16, seed=22, density=0.85)
    return png_from_pixels(pixels, W, H)


def build():
    b = Builder("frozen_comet_me", resolution=(W, H), visible_box=(14, 14, 0))
    b.add_texture("comet_nucleus", tex_nucleus())
    b.add_texture("comet_tail", tex_tail())

    F0 = lambda: uniform_face(0, 0, W, H, tex_index=0)
    F1 = lambda: uniform_face(0, 0, W, H, tex_index=1)

    # ---- NUCLEUS: 5 irregular overlapping cubes ----
    nucleus_specs = [
        ((-1.6, -1.0, -0.8), (1.6, 1.4, 0.8),  (4, 8, -3)),
        ((-2.2, -0.6, -0.4), (-0.4, 1.2, 1.2), (-7, 12, 5)),
        ((0.4, -1.4, -1.2), (1.8, 0.8, 0.6),  (9, -5, 7)),
        ((-1.0, 0.6, -0.6), (1.4, 1.8, 1.4),  (-3, 18, -8)),
        ((-0.6, -1.8, -0.2), (1.0, -0.2, 0.6),(6, -10, 4)),
    ]
    nucleus_segs = []
    for i, (fr, to, rot) in enumerate(nucleus_specs):
        cube = b.add_cube(f"nucleus_{i+1}", from_=list(fr), to_=list(to), faces=F0())
        gu = make_uuid()
        g = b.make_group(f"nucleus_grp_{i+1}", [0, 0, 0], [cube],
                         rotation=list(rot), group_uuid=gu)
        nucleus_segs.append((gu, g))

    # ---- COMA HAZE: 4 flat wide slabs ----
    coma_segs = []
    for i in range(4):
        ang = (i / 4.0) * 2 * math.pi
        cube = b.add_cube(
            f"coma_{i+1}",
            from_=[-3.0, -0.05, -3.0], to_=[3.0, 0.05, 3.0],
            faces=F1(),
        )
        gu = make_uuid()
        g = b.make_group(f"coma_grp_{i+1}", [0, 0, 0], [cube],
                         rotation=[math.degrees(ang) * 0.7, math.degrees(ang), 0],
                         group_uuid=gu)
        coma_segs.append((gu, g))

    # ---- ION TAIL: 8 thin elongated slabs in tight V-cone behind ----
    ion_segs = []
    for i in range(8):
        length = 2.0 + i * 1.0
        narrow = max(0.15, 0.6 - i * 0.06)
        # alternating V-cone arrangement: small Y offset and X offset
        side = -1 if i % 2 == 0 else 1
        ang_offset = (i // 2) * 4.0  # degrees
        zc = -1.5 - i * 1.5
        cube = b.add_cube(
            f"ion_{i+1}",
            from_=[-narrow, -narrow * 0.6, -length / 2], to_=[narrow, narrow * 0.6, length / 2],
            faces=F1(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"ion_grp_{i+1}", [side * 0.3, 0, zc], [cube],
            rotation=[0, side * ang_offset, 0], group_uuid=gu,
        )
        ion_segs.append((gu, g))

    # ---- DUST TAIL: 4 wider shorter flat pieces in slight arc above ion ----
    dust_segs = []
    for i in range(4):
        length = 2.0 + i * 0.4
        # arc curves upward (Y) as we go back
        zc = -2.5 - i * 1.4
        yc = 0.5 + i * 0.4
        cube = b.add_cube(
            f"dust_{i+1}",
            from_=[-0.7, -0.05, -length / 2], to_=[0.7, 0.05, length / 2],
            faces=F1(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"dust_grp_{i+1}", [0, yc, zc], [cube],
            rotation=[15 + i * 5, 0, 0], group_uuid=gu,
        )
        dust_segs.append((gu, g))

    # ---- SURFACE PITS: 4 small flat slabs on nucleus surface ----
    pit_segs = []
    pit_positions = [(1.5, 0.5, 0.3), (-1.4, 0.2, -0.8), (0.4, -1.2, 0.6), (-0.8, 1.2, 0.5)]
    for i, (px, py, pz) in enumerate(pit_positions):
        cube = b.add_cube(
            f"pit_{i+1}",
            from_=[-0.4, -0.06, -0.4], to_=[0.4, 0.06, 0.4],
            faces=F0(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"pit_grp_{i+1}", [px, py, pz], [cube],
            rotation=[i * 30, i * 60, i * 90], group_uuid=gu,
        )
        pit_segs.append((gu, g))

    # ---- ROOT ----
    children = [g for (_u, g) in nucleus_segs + coma_segs + ion_segs + dust_segs + pit_segs]
    root_uuid = make_uuid()
    root = b.make_group("root", [0, 0, 0], children, group_uuid=root_uuid)
    b.outliner.append(root)

    # ---- SPAWN: 0.3s ----
    spawn = {}
    for i, (gu, g) in enumerate(nucleus_segs):
        t0 = i * 0.02
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.10, x=1, y=1, z=1),
            make_keyframe("scale", 0.30, x=1, y=1, z=1),
            make_keyframe("rotation", 0.0, x=0, y=0, z=0),
            make_keyframe("rotation", 0.30, x=20 + i * 5, y=30 + i * 4, z=10),
        ]}
    for i, (gu, g) in enumerate(coma_segs):
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0.05, y=0.05, z=0.05),
            make_keyframe("scale", 0.05, x=0.05, y=0.05, z=0.05),
            make_keyframe("scale", 0.25, x=1.1, y=1.0, z=1.1),
            make_keyframe("scale", 0.30, x=1, y=1, z=1),
        ]}
    for i, (gu, g) in enumerate(ion_segs):
        t0 = 0.05 + i * 0.012
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.08, x=1, y=1, z=1),
            make_keyframe("scale", 0.30, x=1, y=1, z=1),
        ]}
    for i, (gu, g) in enumerate(dust_segs):
        t0 = 0.10 + i * 0.02
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.10, x=1, y=1, z=1),
            make_keyframe("scale", 0.30, x=1, y=1, z=1),
        ]}
    for i, (gu, g) in enumerate(pit_segs):
        t0 = 0.15 + i * 0.02
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.05, x=1, y=1, z=1),
            make_keyframe("scale", 0.30, x=1, y=1, z=1),
        ]}
    b.add_animation("spawn", length=0.30, animators=spawn, loop="once", override=True)

    # ---- IDLE: 3.0s — slow tumble, coma pulse, tail oscillates ----
    idle = {}
    # Root very slow tumble
    idle[root_uuid] = {"name": "root", "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, x=0, y=0, z=0),
        make_keyframe("rotation", 1.0, x=12, y=20, z=8),
        make_keyframe("rotation", 2.0, x=24, y=40, z=16),
        make_keyframe("rotation", 3.0, x=36, y=60, z=24),
    ]}
    for i, (gu, g) in enumerate(nucleus_segs):
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_scale_kfs(3.0, 18, base=1.0, amp=0.025,
                                               period=2.0, phase=i * 0.4)}
    for i, (gu, g) in enumerate(coma_segs):
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_scale_kfs(3.0, 20, base=1.0, amp=0.10,
                                               period=2.5, phase=i * 0.5)}
    for i, (gu, g) in enumerate(ion_segs):
        kfs = osc_rot_kfs(3.0, 18, amp_x=0, amp_y=2.0, amp_z=0,
                          period=2.0, phase=i * 0.2)
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}
    for i, (gu, g) in enumerate(dust_segs):
        kfs = osc_rot_kfs(3.0, 16, amp_x=4.0, amp_y=0, amp_z=2.0,
                          period=2.5, phase=i * 0.3)
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}
    for i, (gu, g) in enumerate(pit_segs):
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_scale_kfs(3.0, 18, base=1.0, amp=0.15,
                                               period=1.5 + i * 0.3, phase=i * 0.7)}
    b.add_animation("idle", length=3.0, animators=idle, loop="loop", override=False)

    # ---- DISSIPATE: 0.35s — fragment ----
    diss = {}
    for i, (gu, g) in enumerate(nucleus_segs):
        ang = (i / 5.0) * 2 * math.pi
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.35, x=math.cos(ang) * 6, y=math.sin(ang) * 3, z=math.sin(ang) * 6),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.35, x=0.0, y=0.0, z=0.0),
            make_keyframe("rotation", 0.0, x=0, y=0, z=0),
            make_keyframe("rotation", 0.35, x=360, y=540 + i * 60, z=180),
        ]}
    for i, (gu, g) in enumerate(coma_segs):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.25, x=2.5, y=1.0, z=2.5),
            make_keyframe("scale", 0.35, x=0.0, y=0.0, z=0.0),
        ]}
    for i, (gu, g) in enumerate(ion_segs):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.35, x=0, y=0, z=-5),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.35, x=0.0, y=0.0, z=0.0),
        ]}
    for i, (gu, g) in enumerate(dust_segs):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.35, x=0, y=2 + i, z=-4),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.35, x=0.0, y=0.0, z=0.0),
        ]}
    for i, (gu, g) in enumerate(pit_segs):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.35, x=0.0, y=0.0, z=0.0),
        ]}
    b.add_animation("dissipate", length=0.35, animators=diss, loop="once", override=True)

    b.write(OUTPUT)
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    build()
