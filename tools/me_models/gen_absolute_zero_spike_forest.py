#!/usr/bin/env python3
"""Generator for absolute_zero_spike_forest.bbmodel — Needlepoint.

Geometry: 16 spike chains in 4x4 grid, heights varied [4,7,5,9,6,8,4,11,7,5,10,6,8,4,9,7].
Each 3 segments. 16 hex base slabs. 6 frost web slabs. 16 tip crystals (each 2 cubes at 22.5°).
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
    vertical_gradient_full,
    specular_cluster,
    frost_web_lines,
    base_fill,
)

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/freezingice/me_attacks/absolute_zero_spike_forest.bbmodel"
RES = 128


def build_needle_texture():
    """ICE NEEDLE — purely vertical gradient: dark bottom -> bright white tip."""
    w = h = RES
    void  = hex_to_rgba("#020810")
    navy  = hex_to_rgba("#081830")
    cold  = hex_to_rgba("#184870")
    pale  = hex_to_rgba("#88c8f0")
    tip   = hex_to_rgba("#f0f8ff")
    emiss = hex_to_rgba("#a0e8ff")

    pixels = base_fill(w, h, void)
    # Inverted: top of texture = spike tip = pale; bottom = void
    vertical_gradient_full(pixels, w, h, [
        (0.0, tip),
        (0.15, pale),
        (0.50, cold),
        (0.80, navy),
        (1.0, void),
    ])

    # Emissive top 15%
    for y in range(int(h * 0.15)):
        t = 1.0 - (y / (h * 0.15))
        for x in range(w):
            p = pixels[y * w + x]
            pixels[y * w + x] = color_lerp(p, emiss, t * 0.4)

    # Specular cluster at top centre (needle point)
    specular_cluster(pixels, w, h, w // 2, int(h * 0.06), 4, tip, strength=1.0)
    specular_cluster(pixels, w, h, w // 2, int(h * 0.03), 2, tip, strength=1.0)

    # No diagonal — purely vertical streaks
    import random
    rng = random.Random(7)
    for _ in range(30):
        x = rng.randrange(w)
        ys = rng.randrange(0, h - 8)
        ye = ys + rng.randrange(4, 12)
        for y in range(ys, min(h, ye)):
            p = pixels[y * w + x]
            pixels[y * w + x] = color_lerp(p, tip, 0.25)

    add_noise_overlay(pixels, w, h, navy, pale, density=0.16, seed=31)
    jitter_inplace(pixels, jitter_range=18, seed=99)
    jitter_inplace(pixels, jitter_range=10, seed=141, density=0.4)

    return png_from_pixels(pixels, w, h)


def build_frost_web_texture():
    """FROST CRYSTAL WEB — branching from centre."""
    w = h = RES
    bg    = hex_to_rgba("#040c10")
    web   = hex_to_rgba("#60b8d8")
    emiss = hex_to_rgba("#80d0e8")

    pixels = base_fill(w, h, bg)

    # Primary 6 + secondary branches
    frost_web_lines(pixels, w, h, w // 2, h // 2, 6, web, seed=33)
    frost_web_lines(pixels, w, h, w // 2, h // 2, 12, emiss, seed=66)

    # Bright centre
    specular_cluster(pixels, w, h, w // 2, h // 2, 6, emiss, strength=0.7)

    add_noise_overlay(pixels, w, h, bg, web, density=0.15, seed=14)
    jitter_inplace(pixels, jitter_range=18, seed=211)
    jitter_inplace(pixels, jitter_range=12, seed=313, density=0.45)

    return png_from_pixels(pixels, w, h)


def build():
    b = Builder("absolute_zero_spike_forest", resolution=(RES, RES), visible_box=(12, 12, 0))
    b.add_texture("needle", build_needle_texture())
    b.add_texture("frost_web", build_frost_web_texture())

    needle_face = lambda: uniform_face(0, 0, RES, RES, tex_index=0)
    web_face = lambda: uniform_face(0, 0, RES, RES, tex_index=1)

    heights = [4, 7, 5, 9, 6, 8, 4, 11, 7, 5, 10, 6, 8, 4, 9, 7]

    spike_groups = []   # (uuid, group, height, gx, gz, row, col)
    base_groups = []
    tip_groups = []

    # 4x4 grid, 2-unit spacing
    spacing = 2.5
    for row in range(4):
        for col in range(4):
            idx = row * 4 + col
            gx = (col - 1.5) * spacing
            gz = (row - 1.5) * spacing
            H = heights[idx]

            # 3 segments: base wide -> mid -> tip narrow
            seg_cubes = []
            seg1_h = H * 0.4
            seg2_h = H * 0.35
            seg3_h = H * 0.25
            seg_cubes.append(b.add_cube(
                f"spike{idx+1}_base",
                from_=[-0.6, 0, -0.6], to_=[0.6, seg1_h, 0.6],
                faces=needle_face(),
            ))
            seg_cubes.append(b.add_cube(
                f"spike{idx+1}_mid",
                from_=[-0.4, seg1_h, -0.4], to_=[0.4, seg1_h + seg2_h, 0.4],
                faces=needle_face(),
            ))
            seg_cubes.append(b.add_cube(
                f"spike{idx+1}_tip",
                from_=[-0.18, seg1_h + seg2_h, -0.18], to_=[0.18, H, 0.18],
                faces=needle_face(),
            ))
            gu = make_uuid()
            g = b.make_group(
                f"spike_{idx+1}", [gx, 0, gz], seg_cubes, group_uuid=gu,
            )
            spike_groups.append((gu, g, H, gx, gz, row, col))

            # Hex base slab at Y=0 — flat hexagonal approx (square slab)
            base_cu = b.add_cube(
                f"spikebase{idx+1}",
                from_=[-0.95, 0, -0.95], to_=[0.95, 0.18, 0.95],
                faces=web_face(),
            )
            bgu = make_uuid()
            bg = b.make_group(
                f"base_{idx+1}", [gx, 0, gz], [base_cu], group_uuid=bgu,
            )
            base_groups.append((bgu, bg))

            # Tip crystal — 2-cube angled accent at top
            tip_cu1 = b.add_cube(
                f"tipcrystal{idx+1}a",
                from_=[-0.22, 0, -0.08], to_=[0.22, 0.5, 0.08],
                faces=needle_face(),
            )
            tip_cu2 = b.add_cube(
                f"tipcrystal{idx+1}b",
                from_=[-0.08, 0, -0.22], to_=[0.08, 0.5, 0.22],
                faces=needle_face(),
            )
            tgu = make_uuid()
            tg = b.make_group(
                f"tip_{idx+1}", [gx, H, gz], [tip_cu1, tip_cu2],
                rotation=[22.5, idx * 22.5, 22.5], group_uuid=tgu,
            )
            tip_groups.append((tgu, tg, gx, gz, H))

    # FROST WEB — 6 thin slabs connecting neighbouring spike bases
    web_groups = []
    web_connections = [
        (0, 5), (5, 10), (10, 15), (3, 6), (6, 9), (9, 12),
    ]
    for i, (a, b_idx) in enumerate(web_connections):
        ra, ca = divmod(a, 4)
        rb, cb = divmod(b_idx, 4)
        x1 = (ca - 1.5) * spacing
        z1 = (ra - 1.5) * spacing
        x2 = (cb - 1.5) * spacing
        z2 = (rb - 1.5) * spacing
        mx = (x1 + x2) / 2
        mz = (z1 + z2) / 2
        dx = x2 - x1
        dz = z2 - z1
        length = math.sqrt(dx * dx + dz * dz)
        ang = math.atan2(dz, dx)
        cube_u = b.add_cube(
            f"web_{i+1}",
            from_=[-length / 2, 0.02, -0.18], to_=[length / 2, 0.10, 0.18],
            faces=web_face(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"webconn_{i+1}", [mx, 0.05, mz], [cube_u],
            rotation=[0, math.degrees(ang), 0], group_uuid=gu,
        )
        web_groups.append((gu, g))

    root_children = []
    for (gu, g, *_rest) in spike_groups: root_children.append(g)
    for (gu, g) in base_groups: root_children.append(g)
    for (gu, g, *_rest) in tip_groups: root_children.append(g)
    for (gu, g) in web_groups: root_children.append(g)
    root_uuid = make_uuid()
    b.outliner.append(b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid))

    # SPAWN 0.9s
    spawn = {}
    # Base slabs appear at 0.0s
    for idx, (gu, g) in enumerate(base_groups):
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", 0.05, 1, 1, 1),
            make_keyframe("scale", 0.9, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Frost web radiates at 0.1s
    for idx, (gu, g) in enumerate(web_groups):
        start = 0.1 + idx * 0.02
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", start, 0, 0, 0),
            make_keyframe("scale", start + 0.1, 1, 1, 1),
            make_keyframe("scale", 0.9, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Spikes erupt in wave from grid centre
    def ring_delay(row, col):
        # centre rows 1,2 cols 1,2 = ring 0 (4 spikes)
        # next ring = ring 1 (rest non-corner)
        # outer corners = ring 2
        dr = abs(row - 1.5)
        dc = abs(col - 1.5)
        d = max(dr, dc)
        if d <= 0.5: return 0.2
        elif d <= 1.5: return 0.3
        else: return 0.4

    for idx, (gu, g, H, gx, gz, row, col) in enumerate(spike_groups):
        rd = ring_delay(row, col)
        kfs = [
            make_keyframe("scale", 0.0, 1, 0, 1),
            make_keyframe("scale", rd, 1, 0, 1),
            make_keyframe("scale", rd + 0.20, 1.05, 1.1, 1.05),
            make_keyframe("scale", rd + 0.30, 1, 1, 1),
            make_keyframe("scale", 0.9, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Tip crystals deploy at spike completion
    for idx, (gu, g, gx, gz, H) in enumerate(tip_groups):
        rd = ring_delay(idx // 4, idx % 4)
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", rd + 0.3, 0, 0, 0),
            make_keyframe("scale", rd + 0.4, 1, 1, 1),
            make_keyframe("scale", 0.9, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("spawn", length=0.9, animators=spawn, loop="once", override=True)

    # IDLE 4.0s
    idle = {}
    for idx, (gu, g, H, gx, gz, row, col) in enumerate(spike_groups):
        period = 2.5 + (idx * 0.13) % 2.0
        kfs = []
        for k in range(13):
            t = (k / 12) * 4.0
            ph = (t / period + idx * 0.31) * 2 * math.pi
            rx = math.sin(ph) * 1.0
            rz = math.cos(ph * 0.7) * 1.0
            kfs.append(make_keyframe("rotation", t, rx, 0, rz))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g, gx, gz, H) in enumerate(tip_groups):
        kfs = []
        for k in range(9):
            t = (k / 8) * 4.0
            ry = idx * 22.5 + t * 30
            kfs.append(make_keyframe("rotation", t, 22.5, ry, 22.5))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g) in enumerate(web_groups):
        kfs = []
        for k in range(11):
            t = (k / 10) * 4.0
            ph = (t / 3.0 - idx * 0.4) * 2 * math.pi
            s = 1.0 + 0.06 * (1 - math.cos(ph))
            kfs.append(make_keyframe("scale", t, s, s, s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g) in enumerate(base_groups):
        kfs = []
        for k in range(7):
            t = (k / 6) * 4.0
            ph = (t / 3.5 + idx * 0.2) * 2 * math.pi
            s = 1.0 + 0.03 * (1 - math.cos(ph))
            kfs.append(make_keyframe("scale", t, s, s, s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("idle", length=4.0, animators=idle, loop="loop", override=False)

    # DISSIPATE 0.6s — all spikes shatter simultaneously
    diss = {}
    import random as _r
    rng_d = _r.Random(7)
    for idx, (gu, g, H, gx, gz, row, col) in enumerate(spike_groups):
        dx = rng_d.uniform(-1, 1)
        dz = rng_d.uniform(-1, 1)
        kfs = [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.6, dx * 2, 4, dz * 2),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.3, 1, 0.6, 1),
            make_keyframe("scale", 0.6, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g, gx, gz, H) in enumerate(tip_groups):
        kfs = [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.6, 0, 5, 0),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.6, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g) in enumerate(base_groups):
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.6, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Frost web cracks from outer to inner
    for idx, (gu, g) in enumerate(web_groups):
        start = idx * 0.05
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", start, 1, 1, 1),
            make_keyframe("scale", start + 0.15, 0, 0, 0),
            make_keyframe("scale", 0.6, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("dissipate", length=0.6, animators=diss, loop="once", override=True)

    b.write(OUTPUT_PATH)
    import os
    sz = os.path.getsize(OUTPUT_PATH)
    print(f"Wrote {OUTPUT_PATH} ({sz} bytes, {sz/1024:.1f} KB)")


if __name__ == "__main__":
    build()
