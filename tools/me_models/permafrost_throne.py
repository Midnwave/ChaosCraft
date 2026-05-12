#!/usr/bin/env python3
"""Generator: permafrost_throne.bbmodel — FreezingIce attack 17 'The Seat of Cold'.

A throne of ancient permafrost ice. Architecturally readable: seat, back columns
of varying height, armrests, legs, frost crown, ground freeze.
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

OUTPUT = "D:/CC/ChaosCraft/src/main/resources/models/freezingice/me_attacks/permafrost_throne.bbmodel"
W = H = 128


def tex_permafrost():
    """Permafrost ancient ice — banding for age layers, with crown emissive."""
    deep_bg    = hex_color("#081830")
    mid        = hex_color("#1c4870")
    dense_ice  = hex_color("#3080b0")
    hoarfrost  = hex_color("#90c8e8")
    spec       = hex_color("#d8f4ff")
    emiss      = hex_color("#70b8f0")
    pixels = new_pixels(W, H, deep_bg)
    # Slight horizontal age-banding every 10 rows
    for y in range(H):
        band = (y // 10) % 5
        if band == 0:
            base_col = deep_bg
        elif band == 1:
            base_col = mid
        elif band == 2:
            base_col = dense_ice
        elif band == 3:
            base_col = hoarfrost
        else:
            base_col = spec
        # add slight darker line at band boundary
        for x in range(W):
            if y % 10 == 0:
                p = base_col
                pixels[y * W + x] = (
                    max(0, p[0] - 25), max(0, p[1] - 25), max(0, p[2] - 25), 255,
                )
            else:
                pixels[y * W + x] = base_col
    # 3 darker patches randomly placed (trapped air/soil)
    rng = random.Random(404)
    for _ in range(3):
        cx = rng.randrange(20, W - 20)
        cy = rng.randrange(20, H - 20)
        r = rng.randrange(6, 12)
        for dy in range(-r, r + 1):
            for dx in range(-r, r + 1):
                if dx * dx + dy * dy <= r * r:
                    x, y = cx + dx, cy + dy
                    if 0 <= x < W and 0 <= y < H:
                        p = pixels[y * W + x]
                        t = 1.0 - math.sqrt(dx * dx + dy * dy) / r
                        pixels[y * W + x] = (
                            max(0, p[0] - int(60 * t)),
                            max(0, p[1] - int(60 * t)),
                            max(0, p[2] - int(60 * t)), 255,
                        )
    # Top emissive band — crown of throne
    for y in range(0, 12):
        for x in range(W):
            pixels[y * W + x] = emiss
    random_bright_specks(pixels, W, H, 100, hoarfrost, seed=11, size_max=2)
    random_bright_specks(pixels, W, H, 60, spec, seed=22, size_max=1)
    per_pixel_jitter(pixels, W, H, magnitude=14, seed=33, density=0.7)
    return png_from_pixels(pixels, W, H)


def tex_ground():
    """Frozen dark earth with pale crack lines."""
    earth = hex_color("#080c0a")
    frost = hex_color("#a8d8e8")
    spec  = hex_color("#ffffff")
    pixels = new_pixels(W, H, earth)
    # Branching frost lines from centre
    rng = random.Random(101)
    cx, cy = W / 2, H / 2
    for _ in range(20):
        ang = rng.uniform(0, 2 * math.pi)
        length = rng.randrange(20, 50)
        x, y = cx, cy
        for k in range(length):
            x += math.cos(ang)
            y += math.sin(ang)
            xi, yi = int(x), int(y)
            if 0 <= xi < W and 0 <= yi < H:
                pixels[yi * W + xi] = frost
            # Branch
            if k % 6 == 0:
                ang += rng.uniform(-0.5, 0.5)
    random_bright_specks(pixels, W, H, 200, frost, seed=44, size_max=1)
    random_bright_specks(pixels, W, H, 40, spec, seed=55, size_max=1)
    per_pixel_jitter(pixels, W, H, magnitude=14, seed=66, density=0.7)
    return png_from_pixels(pixels, W, H)


def build():
    b = Builder("permafrost_throne", resolution=(W, H), visible_box=(14, 16, 0))
    b.add_texture("permafrost", tex_permafrost())
    b.add_texture("ground_freeze", tex_ground())

    F0 = lambda: uniform_face(0, 0, W, H, tex_index=0)
    F1 = lambda: uniform_face(0, 0, W, H, tex_index=1)

    # ---- THRONE SEAT: 3 wide flat slabs at Y=1.5 ----
    seat_segs = []
    seat_specs = [
        (-3.0, 1.3, -2.0, 3.0, 2.0, 2.0),  # main
        (-3.2, 1.5, -2.0, -1.5, 1.8, 2.0),  # left lip
        (1.5, 1.5, -2.0, 3.2, 1.8, 2.0),    # right lip
    ]
    for i, (x0, y0, z0, x1, y1, z1) in enumerate(seat_specs):
        cube = b.add_cube(f"seat_{i+1}", from_=[x0, y0, z0], to_=[x1, y1, z1], faces=F0())
        gu = make_uuid()
        g = b.make_group(f"seat_grp_{i+1}", [0, 1.5, 0], [cube], group_uuid=gu)
        seat_segs.append((gu, g))

    # ---- BACK COLUMNS: 5 vertical slabs of varying heights (8,12,10,13,9) ----
    col_heights = [8, 12, 10, 13, 9]
    col_segs = []
    col_x_positions = [-3.0, -1.5, 0, 1.5, 3.0]
    for i, (cx, h) in enumerate(zip(col_x_positions, col_heights)):
        cube = b.add_cube(
            f"col_{i+1}",
            from_=[cx - 0.6, 1.8, -2.2], to_=[cx + 0.6, 1.8 + h, -1.5],
            faces=F0(),
        )
        gu = make_uuid()
        g = b.make_group(f"col_grp_{i+1}", [cx, 1.8, -1.85], [cube], group_uuid=gu)
        col_segs.append((gu, g))

    # ---- LEFT ARMREST: 3 cube segments ----
    larm_segs = []
    larm_specs = [
        (-4.2, 1.8, -1.5, -3.0, 2.6, 2.0),
        (-4.2, 1.8, 1.5, -3.0, 2.6, 2.5),
        (-4.2, 1.8, -1.8, -3.0, 2.6, -1.0),
    ]
    for i, (x0, y0, z0, x1, y1, z1) in enumerate(larm_specs):
        cube = b.add_cube(f"larm_{i+1}", from_=[x0, y0, z0], to_=[x1, y1, z1], faces=F0())
        gu = make_uuid()
        g = b.make_group(f"larm_grp_{i+1}", [-3.6, 2.2, 0], [cube], group_uuid=gu)
        larm_segs.append((gu, g))

    # ---- RIGHT ARMREST: mirror ----
    rarm_segs = []
    rarm_specs = [
        (3.0, 1.8, -1.5, 4.2, 2.6, 2.0),
        (3.0, 1.8, 1.5, 4.2, 2.6, 2.5),
        (3.0, 1.8, -1.8, 4.2, 2.6, -1.0),
    ]
    for i, (x0, y0, z0, x1, y1, z1) in enumerate(rarm_specs):
        cube = b.add_cube(f"rarm_{i+1}", from_=[x0, y0, z0], to_=[x1, y1, z1], faces=F0())
        gu = make_uuid()
        g = b.make_group(f"rarm_grp_{i+1}", [3.6, 2.2, 0], [cube], group_uuid=gu)
        rarm_segs.append((gu, g))

    # ---- ARMREST HAND/FRONT: 2 wide cubes at front of each armrest ----
    hand_segs = []
    hand_specs = [
        (-4.4, 2.6, 1.6, -2.8, 3.2, 2.8),    # left front
        (2.8, 2.6, 1.6, 4.4, 3.2, 2.8),       # right front
    ]
    for i, (x0, y0, z0, x1, y1, z1) in enumerate(hand_specs):
        cube = b.add_cube(f"hand_{i+1}", from_=[x0, y0, z0], to_=[x1, y1, z1], faces=F0())
        gu = make_uuid()
        g = b.make_group(f"hand_grp_{i+1}",
                         [-3.6 if i == 0 else 3.6, 2.9, 2.2], [cube], group_uuid=gu)
        hand_segs.append((gu, g))

    # ---- THRONE LEGS: 4 downward tapered spike pairs ----
    leg_segs = []
    leg_positions = [(-3.0, -1.8), (3.0, -1.8), (-3.0, 1.8), (3.0, 1.8)]
    for i, (lx, lz) in enumerate(leg_positions):
        # 2 cubes per leg pair (tapered)
        c1 = b.add_cube(
            f"leg{i+1}_upper",
            from_=[lx - 0.5, 0.5, lz - 0.5], to_=[lx + 0.5, 1.5, lz + 0.5],
            faces=F0(),
        )
        c2 = b.add_cube(
            f"leg{i+1}_lower",
            from_=[lx - 0.3, -0.5, lz - 0.3], to_=[lx + 0.3, 0.5, lz + 0.3],
            faces=F0(),
        )
        gu = make_uuid()
        g = b.make_group(f"leg_grp_{i+1}", [lx, 0.5, lz], [c1, c2], group_uuid=gu)
        leg_segs.append((gu, g))

    # ---- FROST CROWN: 4 thin upright spike accents on back outer columns ----
    crown_segs = []
    crown_positions = [(-3.0, 9.8), (-1.5, 13.8), (1.5, 14.8), (3.0, 10.8)]
    # Use column tops: col0 top=9.8, col1=13.8, col2=11.8, col3=14.8, col4=10.8
    # Pick 4 outer columns
    crown_positions = [
        (-3.0, 1.8 + 8),
        (-1.5, 1.8 + 12),
        (1.5, 1.8 + 13),
        (3.0, 1.8 + 9),
    ]
    for i, (cx, cy) in enumerate(crown_positions):
        cube = b.add_cube(
            f"crown_{i+1}",
            from_=[cx - 0.2, cy, -1.95], to_=[cx + 0.2, cy + 2.0, -1.55],
            faces=F0(),
        )
        gu = make_uuid()
        g = b.make_group(f"crown_grp_{i+1}", [cx, cy + 1.0, -1.75], [cube], group_uuid=gu)
        crown_segs.append((gu, g))

    # ---- GROUND FREEZE: 6 flat crack slabs at Y=0 ----
    ground_segs = []
    for i in range(6):
        ang = (i / 6.0) * 2 * math.pi
        cx = math.cos(ang) * 5.0
        cz = math.sin(ang) * 5.0
        cube = b.add_cube(
            f"ground_{i+1}",
            from_=[-2.0, 0.0, -0.5], to_=[2.0, 0.05, 0.5],
            faces=F1(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"ground_grp_{i+1}", [cx, 0, cz], [cube],
            rotation=[0, math.degrees(ang) + 90, 0], group_uuid=gu,
        )
        ground_segs.append((gu, g))

    # ---- ROOT ----
    children = [g for (_u, g) in seat_segs + col_segs + larm_segs + rarm_segs +
                hand_segs + leg_segs + crown_segs + ground_segs]
    root_uuid = make_uuid()
    root = b.make_group("root", [0, 0, 0], children, group_uuid=root_uuid)
    b.outliner.append(root)

    # ---- SPAWN: 1.5s ----
    spawn = {}
    # Ground freeze cracks radiate at 0.2s
    for i, (gu, g) in enumerate(ground_segs):
        t0 = 0.20 + i * 0.04
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=1, z=0),
            make_keyframe("scale", t0, x=0, y=1, z=0),
            make_keyframe("scale", t0 + 0.15, x=1, y=1, z=1),
            make_keyframe("scale", 1.50, x=1, y=1, z=1),
        ]}
    # Throne legs erupt at 0.3s
    for i, (gu, g) in enumerate(leg_segs):
        t0 = 0.30 + i * 0.05
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=-4, z=0),
            make_keyframe("position", t0, x=0, y=-4, z=0),
            make_keyframe("position", t0 + 0.15, x=0, y=0, z=0),
            make_keyframe("scale", 0.0, x=1, y=0, z=1),
            make_keyframe("scale", t0, x=1, y=0, z=1),
            make_keyframe("scale", t0 + 0.15, x=1, y=1, z=1),
            make_keyframe("scale", 1.50, x=1, y=1, z=1),
        ]}
    # Seat slams up at 0.5s
    for i, (gu, g) in enumerate(seat_segs):
        t0 = 0.50 + i * 0.03
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=-3, z=0),
            make_keyframe("position", t0, x=0, y=-3, z=0),
            make_keyframe("position", t0 + 0.10, x=0, y=0, z=0),
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.10, x=1, y=1, z=1),
            make_keyframe("scale", 1.50, x=1, y=1, z=1),
        ]}
    # Armrests deploy at 0.65s
    for i, (gu, g) in enumerate(larm_segs + rarm_segs):
        t0 = 0.65 + i * 0.03
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.10, x=1, y=1, z=1),
            make_keyframe("scale", 1.50, x=1, y=1, z=1),
        ]}
    # Back columns at 0.75s, inner to outer with 0.08s stagger
    col_order = [2, 1, 3, 0, 4]  # inner to outer
    for ord_i, col_i in enumerate(col_order):
        gu, g = col_segs[col_i]
        t0 = 0.75 + ord_i * 0.08
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=0, z=1),
            make_keyframe("scale", t0, x=1, y=0, z=1),
            make_keyframe("scale", t0 + 0.15, x=1, y=1, z=1),
            make_keyframe("scale", 1.50, x=1, y=1, z=1),
        ]}
    # Armrest front pieces at 1.0s
    for i, (gu, g) in enumerate(hand_segs):
        t0 = 1.00 + i * 0.04
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.10, x=1, y=1, z=1),
            make_keyframe("scale", 1.50, x=1, y=1, z=1),
        ]}
    # Frost crown accents at 1.1s
    for i, (gu, g) in enumerate(crown_segs):
        t0 = 1.10 + i * 0.05
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.10, x=1.2, y=1, z=1.2),
            make_keyframe("scale", t0 + 0.15, x=1, y=1, z=1),
            make_keyframe("scale", 1.50, x=1, y=1, z=1),
        ]}
    b.add_animation("spawn", length=1.50, animators=spawn, loop="once", override=True)

    # ---- IDLE: 9.0s — almost completely still, just subtle sway ----
    idle = {}
    idle[root_uuid] = {"name": "root", "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, x=0, y=0, z=0),
        make_keyframe("rotation", 2.25, x=0.1, y=0.2, z=0),
        make_keyframe("rotation", 4.5, x=0, y=0, z=0),
        make_keyframe("rotation", 6.75, x=-0.1, y=-0.2, z=0),
        make_keyframe("rotation", 9.0, x=0, y=0, z=0),
    ]}
    # Crown emissive pulses on long staggered periods
    for i, (gu, g) in enumerate(crown_segs):
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_scale_kfs(9.0, 30, base=1.0, amp=0.06,
                                               period=3.5 + i * 0.4, phase=i * 0.7)}
    # Column tops pulse - via column scale on Y
    for i, (gu, g) in enumerate(col_segs):
        kfs = osc_scale_kfs(9.0, 24, base=1.0, amp=0.02,
                            period=4.0 + i * 0.3, phase=i * 0.5)
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}
    # Ground freeze pulses slowly
    for i, (gu, g) in enumerate(ground_segs):
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_scale_kfs(9.0, 24, base=1.0, amp=0.05,
                                               period=4.5, phase=i * 0.5)}
    b.add_animation("idle", length=9.0, animators=idle, loop="loop", override=False)

    # ---- DISSIPATE: 1.2s ----
    diss = {}
    # Frost crown pops off first
    for i, (gu, g) in enumerate(crown_segs):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.20, x=0, y=3, z=0),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.20, x=0.0, y=0.0, z=0.0),
        ]}
    # Back columns crumble outer in
    for ord_i, col_i in enumerate([0, 4, 1, 3, 2]):
        gu, g = col_segs[col_i]
        t0 = 0.25 + ord_i * 0.08
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", t0, x=1, y=1, z=1),
            make_keyframe("scale", t0 + 0.15, x=1, y=0.05, z=1),
            make_keyframe("scale", 1.20, x=1, y=0, z=1),
        ]}
    # Armrests retract
    for i, (gu, g) in enumerate(larm_segs + rarm_segs):
        t0 = 0.60 + i * 0.04
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", t0, x=1, y=1, z=1),
            make_keyframe("scale", t0 + 0.10, x=0.05, y=1, z=1),
            make_keyframe("scale", 1.20, x=0, y=0, z=0),
        ]}
    for i, (gu, g) in enumerate(hand_segs):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.50, x=0, y=0, z=0),
        ]}
    # Seat sinks
    for i, (gu, g) in enumerate(seat_segs):
        t0 = 0.85 + i * 0.04
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", t0, x=0, y=0, z=0),
            make_keyframe("position", t0 + 0.20, x=0, y=-3, z=0),
            make_keyframe("scale", t0 + 0.20, x=0, y=0, z=0),
            make_keyframe("scale", 1.20, x=0, y=0, z=0),
        ]}
    # Legs retract last
    for i, (gu, g) in enumerate(leg_segs):
        t0 = 1.00 + i * 0.04
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", t0, x=0, y=0, z=0),
            make_keyframe("position", 1.20, x=0, y=-4, z=0),
            make_keyframe("scale", 1.20, x=1, y=0, z=1),
        ]}
    # Ground freeze contracts
    for i, (gu, g) in enumerate(ground_segs):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 1.20, x=0, y=1, z=0),
        ]}
    b.add_animation("dissipate", length=1.20, animators=diss, loop="once", override=True)

    b.write(OUTPUT)
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    build()
