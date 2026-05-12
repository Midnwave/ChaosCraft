#!/usr/bin/env python3
"""Generator for crystalline_aurora_burst.bbmodel — Northern Lights.

16 tall thin vertical aurora curtain slabs (h=8, thin Z, wide X) in pairs every 22.5°.
8 secondary half-height curtains. 12 ground corona slabs. Central 3-cube light column.
10 horizontal shimmer bands at Y=2, Y=5, Y=8.
"""

import sys
import math

sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder,
    make_uuid,
    make_keyframe,
    uniform_face,
    png_from_pixels,
    hex_to_rgba,
    color_lerp,
    add_noise_overlay,
)
from _freezingice_helpers import (
    jitter_inplace,
    horizontal_band_split,
    specular_cluster,
    base_fill,
)

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/freezingice/me_attacks/crystalline_aurora_burst.bbmodel"
RES = 128


def build_aurora_curtain_texture():
    """AURORA CURTAIN — 4 vertical bands of distinct aurora phase colours."""
    w = h = RES
    silver_navy = hex_to_rgba("#081828")
    cold_teal   = hex_to_rgba("#104848")
    bright_blue = hex_to_rgba("#3090c0")
    pale_aurora = hex_to_rgba("#a8d8f8")
    emiss_white = hex_to_rgba("#c0f0ff")

    pixels = base_fill(w, h, silver_navy)

    # 4 vertical bands
    q = w // 4
    horizontal_band_split(pixels, w, h, [
        (0, q, silver_navy),
        (q, 2 * q, cold_teal),
        (2 * q, 3 * q, bright_blue),
        (3 * q, w, pale_aurora),
    ])

    # Smooth transitions between bands (small lerp zone)
    for boundary in [q, 2 * q, 3 * q]:
        for dx in range(-3, 4):
            x = boundary + dx
            if 0 <= x < w:
                blend_strength = 1.0 - abs(dx) / 3.0
                for y in range(h):
                    p = pixels[y * w + x]
                    if dx < 0:
                        target = pixels[y * w + min(w - 1, boundary)]
                    else:
                        target = pixels[y * w + max(0, boundary - 1)]
                    pixels[y * w + x] = color_lerp(p, target, blend_strength * 0.3)

    # Emissive pure aurora in right-quarter
    for y in range(h):
        for x in range(3 * q, w):
            p = pixels[y * w + x]
            t = (x - 3 * q) / max(1, w - 3 * q)
            pixels[y * w + x] = color_lerp(p, emiss_white, t * 0.3)

    # Aurora ribbon shimmer effect — horizontal sine bands
    import random
    rng = random.Random(99)
    for y in range(h):
        wave = int(2 * math.sin(y * 0.15) + rng.randint(-1, 1))
        for x in range(w):
            xx = x + wave
            if 0 <= xx < w:
                p = pixels[y * w + xx]
                pixels[y * w + x] = color_lerp(pixels[y * w + x], p, 0.2)

    # Specular hot spot in bright band
    specular_cluster(pixels, w, h, int(w * 0.85), h // 2, 8, emiss_white, strength=0.85)

    add_noise_overlay(pixels, w, h, silver_navy, pale_aurora, density=0.16, seed=171)
    jitter_inplace(pixels, jitter_range=18, seed=221)
    jitter_inplace(pixels, jitter_range=10, seed=313, density=0.45)

    return png_from_pixels(pixels, w, h)


def build_ground_corona_texture():
    """silver-teal aurora glow at ground level."""
    w = h = RES
    centre = hex_to_rgba("#40d8c0")
    mid    = hex_to_rgba("#106840")
    outer  = hex_to_rgba("#040e08")

    pixels = base_fill(w, h, outer)
    cx, cy = w // 2, h // 2
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            t = min(1.0, d / (w * 0.5))
            if t < 0.4:
                c = color_lerp(centre, mid, t / 0.4)
            else:
                c = color_lerp(mid, outer, (t - 0.4) / 0.6)
            pixels[y * w + x] = c

    add_noise_overlay(pixels, w, h, outer, centre, density=0.17, seed=88)
    jitter_inplace(pixels, jitter_range=16, seed=193)
    jitter_inplace(pixels, jitter_range=10, seed=251, density=0.5)

    return png_from_pixels(pixels, w, h)


def build():
    b = Builder("crystalline_aurora_burst", resolution=(RES, RES), visible_box=(14, 12, 0))
    b.add_texture("aurora_curtain", build_aurora_curtain_texture())
    b.add_texture("ground_corona", build_ground_corona_texture())

    aurora_face = lambda: uniform_face(0, 0, RES, RES, tex_index=0)
    ground_face = lambda: uniform_face(0, 0, RES, RES, tex_index=1)

    # 16 tall thin curtains in pairs every 22.5°
    curtain_groups = []
    for i in range(16):
        # pairs every 22.5° means 8 pair positions × 2 curtains each, but spec says 16 in a ring radiating
        ang = (i / 16.0) * 2 * math.pi
        r = 5.5
        cx = math.cos(ang) * r
        cz = math.sin(ang) * r
        # Tall on Y (8), thin Z, wide X
        cube_u = b.add_cube(
            f"curtain_{i+1}",
            from_=[-1.0, 0, -0.15], to_=[1.0, 8.0, 0.15],
            faces=aurora_face(),
        )
        gu = make_uuid()
        rot_y = math.degrees(ang) + 90
        g = b.make_group(
            f"aurora_curtain_{i+1}", [cx, 0, cz], [cube_u],
            rotation=[0, rot_y, 0], group_uuid=gu,
        )
        curtain_groups.append((gu, g, ang))

    # 8 secondary half-height curtains, offset positions
    secondary_groups = []
    for i in range(8):
        ang = (i / 8.0) * 2 * math.pi + math.pi / 8
        r = 4.0
        cx = math.cos(ang) * r
        cz = math.sin(ang) * r
        cube_u = b.add_cube(
            f"secondary_{i+1}",
            from_=[-0.7, 0, -0.12], to_=[0.7, 4.0, 0.12],
            faces=aurora_face(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"aurora_secondary_{i+1}", [cx, 0, cz], [cube_u],
            rotation=[0, math.degrees(ang) + 90, 0], group_uuid=gu,
        )
        secondary_groups.append((gu, g, ang))

    # 12 ground corona slabs at Y=0
    corona_groups = []
    for i in range(12):
        ang = (i / 12.0) * 2 * math.pi
        r = 7.0
        cx = math.cos(ang) * r
        cz = math.sin(ang) * r
        cube_u = b.add_cube(
            f"corona_{i+1}",
            from_=[-1.3, 0.02, -0.6], to_=[1.3, 0.10, 0.6],
            faces=ground_face(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"ground_corona_{i+1}", [cx, 0.05, cz], [cube_u],
            rotation=[0, math.degrees(ang), 0], group_uuid=gu,
        )
        corona_groups.append((gu, g))

    # Central light column — thin tall 3-cube stack
    col_cubes = []
    col_cubes.append(b.add_cube("col_base", from_=[-0.35, 0, -0.35], to_=[0.35, 3.0, 0.35],
                                  faces=aurora_face()))
    col_cubes.append(b.add_cube("col_mid", from_=[-0.28, 3.0, -0.28], to_=[0.28, 6.0, 0.28],
                                  faces=aurora_face()))
    col_cubes.append(b.add_cube("col_top", from_=[-0.20, 6.0, -0.20], to_=[0.20, 9.0, 0.20],
                                  faces=aurora_face()))
    col_uuid = make_uuid()
    col_g = b.make_group("light_column", [0, 0, 0], col_cubes, group_uuid=col_uuid)

    # Aurora shimmer bands — 10 thin horizontal slabs at Y=2, Y=5, Y=8 within ring
    shimmer_groups = []
    band_ys = [2.0, 5.0, 8.0]
    shimmer_idx = 0
    for band_idx, by in enumerate(band_ys):
        count = [4, 4, 2][band_idx]  # total = 10
        for j in range(count):
            ang = (j / count) * 2 * math.pi + band_idx * 0.3
            r = 3.5
            cx = math.cos(ang) * r
            cz = math.sin(ang) * r
            cube_u = b.add_cube(
                f"shimmer_{shimmer_idx+1}",
                from_=[-0.9, 0, -0.5], to_=[0.9, 0.15, 0.5],
                faces=aurora_face(),
            )
            gu = make_uuid()
            g = b.make_group(
                f"shimmer_band_{shimmer_idx+1}", [cx, by, cz], [cube_u],
                rotation=[0, math.degrees(ang), 0], group_uuid=gu,
            )
            shimmer_groups.append((gu, g, by))
            shimmer_idx += 1

    root_children = [col_g]
    for (gu, g, ang) in curtain_groups: root_children.append(g)
    for (gu, g, ang) in secondary_groups: root_children.append(g)
    for (gu, g) in corona_groups: root_children.append(g)
    for (gu, g, by) in shimmer_groups: root_children.append(g)
    root_uuid = make_uuid()
    b.outliner.append(b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid))

    # SPAWN 0.8s
    spawn = {}
    # Light column at 0.0s
    spawn[col_uuid] = {"name": "light_column", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 1, 0, 1),
        make_keyframe("scale", 0.15, 1.1, 1.1, 1.1),
        make_keyframe("scale", 0.30, 1, 1, 1),
        make_keyframe("scale", 0.8, 1, 1, 1),
    ]}

    # Ground corona at 0.1s
    for idx, (gu, g) in enumerate(corona_groups):
        start = 0.10 + idx * 0.015
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", start, 0, 0, 0),
            make_keyframe("scale", start + 0.10, 1, 1, 1),
            make_keyframe("scale", 0.8, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Curtains blast outward at 0.2s, slight velocity differences
    for idx, (gu, g, ang) in enumerate(curtain_groups):
        out_x = math.cos(ang) * 3
        out_z = math.sin(ang) * 3
        kfs = [
            make_keyframe("position", 0.0, -out_x, 0, -out_z),
            make_keyframe("position", 0.20, -out_x, 0, -out_z),
            make_keyframe("position", 0.40, 0, 0, 0),
            make_keyframe("position", 0.8, 0, 0, 0),
            make_keyframe("scale", 0.0, 1, 0, 1),
            make_keyframe("scale", 0.20, 1, 0, 1),
            make_keyframe("scale", 0.35, 1.1, 1.1, 1.1),
            make_keyframe("scale", 0.50, 1, 1, 1),
            make_keyframe("scale", 0.8, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Secondary curtains at 0.25s
    for idx, (gu, g, ang) in enumerate(secondary_groups):
        kfs = [
            make_keyframe("scale", 0.0, 1, 0, 1),
            make_keyframe("scale", 0.25, 1, 0, 1),
            make_keyframe("scale", 0.40, 1, 1, 1),
            make_keyframe("scale", 0.8, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Shimmer bands at 0.3s
    for idx, (gu, g, by) in enumerate(shimmer_groups):
        start = 0.30 + idx * 0.02
        kfs = [
            make_keyframe("scale", 0.0, 0, 1, 0),
            make_keyframe("scale", start, 0, 1, 0),
            make_keyframe("scale", start + 0.10, 1, 1, 1),
            make_keyframe("scale", 0.8, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("spawn", length=0.8, animators=spawn, loop="once", override=True)

    # IDLE 4.0s — curtains ripple in wave around ring
    idle = {}
    for idx, (gu, g, ang) in enumerate(curtain_groups):
        kfs = []
        for k in range(13):
            t = (k / 12) * 4.0
            ph = (t / 4.0 + idx / 16.0) * 2 * math.pi
            sy = 1.0 + 0.18 * math.sin(ph)
            kfs.append(make_keyframe("scale", t, 1, sy, 1))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g, ang) in enumerate(secondary_groups):
        kfs = []
        for k in range(11):
            t = (k / 10) * 4.0
            ph = (t / 3.5 - idx / 8.0) * 2 * math.pi
            sy = 1.0 + 0.15 * math.sin(ph)
            kfs.append(make_keyframe("scale", t, 1, sy, 1))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Shimmer bands rotate slowly
    for idx, (gu, g, by) in enumerate(shimmer_groups):
        kfs = []
        for k in range(9):
            t = (k / 8) * 4.0
            ry = t * 22 + idx * 36
            kfs.append(make_keyframe("rotation", t, 0, ry, 0))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Ground corona pulses
    for idx, (gu, g) in enumerate(corona_groups):
        kfs = []
        for k in range(7):
            t = (k / 6) * 4.0
            ph = (t / 3.5 + idx * 0.2) * 2 * math.pi
            s = 1.0 + 0.07 * (1 - math.cos(ph))
            kfs.append(make_keyframe("scale", t, s, 1, s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Light column breathes
    idle[col_uuid] = {"name": "light_column", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 1, 1, 1),
        make_keyframe("scale", 1.0, 1.1, 1.05, 1.1),
        make_keyframe("scale", 2.0, 1, 1, 1),
        make_keyframe("scale", 3.0, 1.1, 1.05, 1.1),
        make_keyframe("scale", 4.0, 1, 1, 1),
    ]}

    b.add_animation("idle", length=4.0, animators=idle, loop="loop", override=False)

    # DISSIPATE 0.6s — curtains continue outward and scale to 0 stagger
    diss = {}
    for idx, (gu, g, ang) in enumerate(curtain_groups):
        start = idx * 0.02
        out_x = math.cos(ang) * 5
        out_z = math.sin(ang) * 5
        kfs = [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.6, out_x, 0, out_z),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", start + 0.4, 1, 1, 1),
            make_keyframe("scale", 0.6, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g, ang) in enumerate(secondary_groups):
        kfs = [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.6, math.cos(ang) * 4, 0, math.sin(ang) * 4),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.6, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g) in enumerate(corona_groups):
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.4, 1.6, 1, 1.6),
            make_keyframe("scale", 0.6, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g, by) in enumerate(shimmer_groups):
        kfs = [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.6, 0, 4, 0),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.6, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    diss[col_uuid] = {"name": "light_column", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 1, 1, 1),
        make_keyframe("scale", 0.6, 0, 0, 0),
    ]}

    b.add_animation("dissipate", length=0.6, animators=diss, loop="once", override=True)

    b.write(OUTPUT_PATH)
    import os
    sz = os.path.getsize(OUTPUT_PATH)
    print(f"Wrote {OUTPUT_PATH} ({sz} bytes, {sz/1024:.1f} KB)")


if __name__ == "__main__":
    build()
