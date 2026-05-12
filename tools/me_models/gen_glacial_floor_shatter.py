#!/usr/bin/env python3
"""Generator for glacial_floor_shatter.bbmodel — Freezing Ice Permafrost Break.

Visual intent: 8 large ice plate bones in a circle at radius 10, tilted outward.
Inner hex cavity of 6 downward slabs. Central 5-segment ice geyser. Frost spread
of 10 thin slabs. 8 ice shrapnel pieces.
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
    fill_rect,
    add_noise_overlay,
)
from _freezingice_helpers import (
    jitter_inplace,
    diagonal_facet_lines,
    crack_lines,
    specular_cluster,
    base_fill,
)

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/freezingice/me_attacks/glacial_floor_shatter.bbmodel"
RES = 128  # higher resolution for filesize bulk and quality


# ---------------------------------------------------------------------------
# TEXTURES — FRACTURED ICE PLATE TECHNIQUE
# ---------------------------------------------------------------------------

def build_plate_texture():
    w = h = RES
    deep_navy   = hex_to_rgba("#06101e")
    glacier     = hex_to_rgba("#102848")
    interior    = hex_to_rgba("#1c5a90")
    edge        = hex_to_rgba("#6ab8e0")
    spec        = hex_to_rgba("#d8f4ff")
    frost_top   = hex_to_rgba("#e8f8ff")
    emiss       = hex_to_rgba("#80d8ff")

    pixels = base_fill(w, h, deep_navy)

    # Vertical stratification: top = frost-white, mid = interior, bottom = deep navy
    for y in range(h):
        t = y / (h - 1)
        if t < 0.08:
            c = frost_top
        elif t < 0.35:
            local = (t - 0.08) / 0.27
            c = color_lerp(frost_top, edge, local)
        elif t < 0.6:
            local = (t - 0.35) / 0.25
            c = color_lerp(edge, interior, local)
        elif t < 0.85:
            local = (t - 0.6) / 0.25
            c = color_lerp(interior, glacier, local)
        else:
            local = (t - 0.85) / 0.15
            c = color_lerp(glacier, deep_navy, local)
        for x in range(w):
            pixels[y * w + x] = c

    # HARD GEOMETRIC FACET LINES — bright diagonal lines
    diagonal_facet_lines(pixels, w, h, edge, count=7, seed=2)
    diagonal_facet_lines(pixels, w, h, spec, count=4, seed=17)

    # Bright fracture line at row 30 (scaled to 60 in 128)
    fracture_row = int(0.47 * h)
    for x in range(w):
        for dy in range(-1, 2):
            yy = fracture_row + dy
            if 0 <= yy < h:
                t = 1.0 - abs(dy) * 0.4
                p = pixels[yy * w + x]
                pixels[yy * w + x] = color_lerp(p, emiss, t)

    # Specular highlights at random ice crystals
    import random
    rng = random.Random(33)
    for _ in range(14):
        cx = rng.randrange(8, w - 8)
        cy = rng.randrange(8, h // 2)
        specular_cluster(pixels, w, h, cx, cy, 3, spec, strength=0.9)

    # Heavy noise overlay + per-pixel jitter
    add_noise_overlay(pixels, w, h, glacier, edge, density=0.18, seed=88)
    add_noise_overlay(pixels, w, h, deep_navy, spec, density=0.10, seed=144)
    jitter_inplace(pixels, jitter_range=22, seed=303)
    jitter_inplace(pixels, jitter_range=14, seed=505, density=0.4)

    return png_from_pixels(pixels, w, h)


def build_cavity_texture():
    """Texture 1 — VOID BENEATH ICE."""
    w = h = RES
    void        = hex_to_rgba("#02080e")
    deep_navy   = hex_to_rgba("#081828")
    crack       = hex_to_rgba("#4090c0")

    pixels = base_fill(w, h, void)

    # Slight brightness toward centre
    for y in range(h):
        for x in range(w):
            dx = x - w / 2
            dy = y - h / 2
            d = math.sqrt(dx * dx + dy * dy)
            t = min(1.0, d / (w * 0.7))
            pixels[y * w + x] = color_lerp(deep_navy, void, t)

    # Single thin bright crack at row 60 (proportional to row 30 in 64)
    crack_row = int(0.47 * h)
    for x in range(w):
        y = crack_row + (x % 3) - 1
        if 0 <= y < h:
            p = pixels[y * w + x]
            pixels[y * w + x] = color_lerp(p, crack, 0.85)
        # Secondary trace
        if 0 <= crack_row + 1 < h:
            p = pixels[(crack_row + 1) * w + x]
            pixels[(crack_row + 1) * w + x] = color_lerp(p, crack, 0.4)

    add_noise_overlay(pixels, w, h, void, deep_navy, density=0.20, seed=99)
    jitter_inplace(pixels, jitter_range=16, seed=271)
    jitter_inplace(pixels, jitter_range=10, seed=313, density=0.5)

    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# MODEL BUILD
# ---------------------------------------------------------------------------

def build():
    b = Builder("glacial_floor_shatter", resolution=(RES, RES), visible_box=(12, 8, 0))
    b.add_texture("plate", build_plate_texture())
    b.add_texture("cavity", build_cavity_texture())

    plate_face = lambda: uniform_face(0, 0, RES, RES, tex_index=0)
    cavity_face = lambda: uniform_face(0, 0, RES, RES, tex_index=1)

    # =====================================================================
    # 1) ICE PLATES — 8 plates in circle at radius 10. Each tilted 22.5° outward.
    #    3 overlapping cubes per plate (wide → mid → small for thickness).
    # =====================================================================
    plate_groups = []  # (uuid, group, ang)
    for i in range(8):
        ang = (i / 8.0) * 2 * math.pi
        cx = math.cos(ang) * 10.0
        cz = math.sin(ang) * 10.0
        cubes = []
        # large slab (widest, base of plate)
        cubes.append(b.add_cube(
            f"plate{i+1}_base",
            from_=[-3.0, -0.3, -1.6], to_=[3.0, 0.3, 1.6],
            faces=plate_face(),
        ))
        # mid slab on top
        cubes.append(b.add_cube(
            f"plate{i+1}_mid",
            from_=[-2.4, 0.2, -1.2], to_=[2.4, 0.8, 1.2],
            faces=plate_face(),
        ))
        # small top slab
        cubes.append(b.add_cube(
            f"plate{i+1}_top",
            from_=[-1.6, 0.7, -0.8], to_=[1.6, 1.3, 0.8],
            faces=plate_face(),
        ))
        # tilt 22.5° outward (rotate around tangent axis)
        rot_y = math.degrees(ang) + 90  # face perpendicular to radial
        gu = make_uuid()
        g = b.make_group(
            f"ice_plate_{i+1}", [cx, 0.5, cz], cubes,
            rotation=[22.5, rot_y, 0], group_uuid=gu,
        )
        plate_groups.append((gu, g, ang))

    # =====================================================================
    # 2) INNER CAVITY — 6 downward hex slabs at Y=-0.3
    # =====================================================================
    cavity_groups = []
    for i in range(6):
        ang = (i / 6.0) * 2 * math.pi
        r = 2.5
        cx = math.cos(ang) * r
        cz = math.sin(ang) * r
        cube_u = b.add_cube(
            f"cavity{i+1}",
            from_=[-1.5, -0.5, -0.8], to_=[1.5, -0.1, 0.8],
            faces=cavity_face(),
        )
        rot_y = math.degrees(ang) + 90
        gu = make_uuid()
        g = b.make_group(
            f"cavity_{i+1}", [cx, -0.3, cz], [cube_u],
            rotation=[-12, rot_y, 0], group_uuid=gu,
        )
        cavity_groups.append((gu, g))

    # =====================================================================
    # 3) CENTRAL GEYSER — 5-segment tapered spike chain
    # =====================================================================
    geyser_segs = []
    seg_data = [
        # (y0, y1, half_width)
        (0.0, 1.5, 1.2),
        (1.5, 3.5, 0.95),
        (3.5, 5.5, 0.7),
        (5.5, 7.0, 0.45),
        (7.0, 8.5, 0.22),
    ]
    for i, (y0, y1, hw) in enumerate(seg_data):
        cu = b.add_cube(
            f"geyser_seg_{i+1}",
            from_=[-hw, y0, -hw], to_=[hw, y1, hw],
            faces=plate_face(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"geyser_{i+1}", [0, (y0 + y1) / 2, 0], [cu], group_uuid=gu,
        )
        geyser_segs.append((gu, g))

    # =====================================================================
    # 4) FROST SPREAD — 10 thin flat slabs radiating outward at Y=0.1
    # =====================================================================
    frost_groups = []
    for i in range(10):
        ang = (i / 10.0) * 2 * math.pi + 0.05
        r = 13.0
        cx = math.cos(ang) * r
        cz = math.sin(ang) * r
        cu = b.add_cube(
            f"frost_{i+1}",
            from_=[-2.0, 0.05, -0.5], to_=[2.0, 0.15, 0.5],
            faces=cavity_face(),
        )
        rot_y = math.degrees(ang)
        gu = make_uuid()
        g = b.make_group(
            f"frost_slab_{i+1}", [cx, 0.1, cz], [cu],
            rotation=[0, rot_y, 0], group_uuid=gu,
        )
        frost_groups.append((gu, g))

    # =====================================================================
    # 5) ICE SHRAPNEL — 8 small irregular cubes at various heights
    # =====================================================================
    shrap_groups = []
    shrap_positions = [
        (-7, 2.0, 4, 0.8), (6, 3.5, -5, 0.6), (-3, 5.5, -7, 0.7),
        (8, 1.8, 6, 0.5), (-9, 4.0, -2, 0.65), (2, 6.0, 8, 0.55),
        (5, 2.8, -8, 0.7), (-6, 5.2, 5, 0.45),
    ]
    for i, (px, py, pz, sz) in enumerate(shrap_positions):
        cu = b.add_cube(
            f"shrap_{i+1}",
            from_=[-sz, -sz, -sz], to_=[sz, sz, sz],
            faces=plate_face(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"shrapnel_{i+1}", [px, py, pz], [cu],
            rotation=[i * 17, i * 23, i * 11], group_uuid=gu,
        )
        shrap_groups.append((gu, g, px, py, pz))

    # ROOT
    root_children = []
    for (gu, g, _ang) in plate_groups:
        root_children.append(g)
    for (gu, g) in cavity_groups:
        root_children.append(g)
    for (gu, g) in geyser_segs:
        root_children.append(g)
    for (gu, g) in frost_groups:
        root_children.append(g)
    for (gu, g, _px, _py, _pz) in shrap_groups:
        root_children.append(g)

    root_uuid = make_uuid()
    root_group = b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid)
    b.outliner.append(root_group)

    # ANIMATIONS ----------------------------------------------------------
    # SPAWN 1.1s
    spawn = {}
    # Cavity slabs: drop to Y=-0.5 at 0.1s
    for idx, (gu, g) in enumerate(cavity_groups):
        kfs = [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.1, 0, -0.5, 0, interpolation="linear"),
            make_keyframe("position", 1.1, 0, -0.5, 0),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 1.1, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Ice plates: blast outward clockwise stagger 0.05s, overshoot tilt
    for idx, (gu, g, ang) in enumerate(plate_groups):
        delay = 0.2 + idx * 0.05
        out_x = math.cos(ang) * 4.0
        out_z = math.sin(ang) * 4.0
        kfs = [
            make_keyframe("position", 0.0, -out_x, -1.0, -out_z),
            make_keyframe("position", delay, -out_x, -1.0, -out_z),
            make_keyframe("position", delay + 0.15, out_x * 0.2, 0.3, out_z * 0.2),
            make_keyframe("position", delay + 0.25, 0, 0, 0),
            make_keyframe("position", 1.1, 0, 0, 0),
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.10, 1.2, 1.2, 1.2),
            make_keyframe("scale", delay + 0.25, 1, 1, 1),
            make_keyframe("rotation", delay, 5, math.degrees(ang) + 90, 0),
            make_keyframe("rotation", delay + 0.12, 32, math.degrees(ang) + 90, 0),
            make_keyframe("rotation", delay + 0.25, 22.5, math.degrees(ang) + 90, 0),
            make_keyframe("rotation", 1.1, 22.5, math.degrees(ang) + 90, 0),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Geyser segments: extend bottom-to-top at 0.25s
    for i, (gu, g) in enumerate(geyser_segs):
        start = 0.25 + i * 0.06
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", start, 0, 0, 0),
            make_keyframe("scale", start + 0.08, 1.2, 1.3, 1.2),
            make_keyframe("scale", start + 0.20, 1, 1, 1),
            make_keyframe("scale", 1.1, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Frost spread at 0.3s
    for idx, (gu, g) in enumerate(frost_groups):
        start = 0.3 + idx * 0.02
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", start, 0, 0, 0),
            make_keyframe("scale", start + 0.12, 1, 1, 1),
            make_keyframe("scale", 1.1, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Shrapnel at 0.25s staggered
    for idx, (gu, g, px, py, pz) in enumerate(shrap_groups):
        start = 0.25 + idx * 0.04
        kfs = [
            make_keyframe("position", 0.0, -px * 0.7, -py - 1, -pz * 0.7),
            make_keyframe("position", start, -px * 0.7, -py - 1, -pz * 0.7),
            make_keyframe("position", start + 0.20, 0, 0, 0),
            make_keyframe("position", 1.1, 0, 0, 0),
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", start, 0, 0, 0),
            make_keyframe("scale", start + 0.10, 1.3, 1.3, 1.3),
            make_keyframe("scale", start + 0.22, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("spawn", length=1.1, animators=spawn, loop="once", override=True)

    # IDLE 5.0s
    idle = {}
    # Plates rock ±2° staggered
    for idx, (gu, g, ang) in enumerate(plate_groups):
        period = 3.0 + (idx % 4) * 0.5
        kfs = []
        steps = 20
        for k in range(steps + 1):
            t = (k / steps) * 5.0
            ph = ((t / period) + idx * 0.4) * 2 * math.pi
            rx = 22.5 + math.sin(ph) * 2.0
            kfs.append(make_keyframe("rotation", t, rx, math.degrees(ang) + 90, 0))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Geyser tip oscillation
    for i, (gu, g) in enumerate(geyser_segs):
        kfs = []
        for k in range(11):
            t = (k / 10) * 5.0
            ph = (t / 2.0) * 2 * math.pi + i * 0.6
            rx = math.sin(ph) * 1.0 * (i + 1) / 5
            rz = math.cos(ph * 0.7) * 1.0 * (i + 1) / 5
            kfs.append(make_keyframe("rotation", t, rx, 0, rz))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Frost spread pulse scale
    for idx, (gu, g) in enumerate(frost_groups):
        kfs = []
        for k in range(11):
            t = (k / 10) * 5.0
            ph = (t / 4.0) * 2 * math.pi + idx * 0.3
            s = 1.0 + 0.05 * (1 - math.cos(ph))
            kfs.append(make_keyframe("scale", t, s, s, s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Cavity slabs subtle wobble
    for idx, (gu, g) in enumerate(cavity_groups):
        kfs = []
        for k in range(9):
            t = (k / 8) * 5.0
            ph = (t / 3.5) * 2 * math.pi + idx * 0.5
            yy = -0.5 + 0.05 * math.sin(ph)
            kfs.append(make_keyframe("position", t, 0, yy, 0))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Shrapnel slow rotation
    for idx, (gu, g, _px, _py, _pz) in enumerate(shrap_groups):
        kfs = []
        for k in range(11):
            t = (k / 10) * 5.0
            ph = (t / 4.0) * 2 * math.pi + idx * 0.4
            kfs.append(make_keyframe("rotation", t,
                idx * 17 + math.sin(ph) * 8,
                idx * 23 + t * 12,
                idx * 11 + math.cos(ph) * 8))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("idle", length=5.0, animators=idle, loop="loop", override=False)

    # DISSIPATE 0.7s
    diss = {}
    # Geyser retract top-to-bottom
    for i, (gu, g) in enumerate(geyser_segs):
        end = 0.4 - i * 0.05
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", max(0.0, end), 1, 1, 1),
            make_keyframe("scale", min(0.7, end + 0.15), 0, 0, 0),
            make_keyframe("scale", 0.7, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Plates crumble outward and scale to 0
    for idx, (gu, g, ang) in enumerate(plate_groups):
        out_x = math.cos(ang) * 5.0
        out_z = math.sin(ang) * 5.0
        kfs = [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.5, out_x, -1, out_z),
            make_keyframe("position", 0.7, out_x * 1.4, -2, out_z * 1.4),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.4, 1, 1, 1),
            make_keyframe("scale", 0.7, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Cavity slabs rise back then scale to 0
    for idx, (gu, g) in enumerate(cavity_groups):
        kfs = [
            make_keyframe("position", 0.0, 0, -0.5, 0),
            make_keyframe("position", 0.4, 0, 0, 0),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.4, 1, 1, 1),
            make_keyframe("scale", 0.7, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Frost spread fade
    for idx, (gu, g) in enumerate(frost_groups):
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.7, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Shrapnel continues outward
    for idx, (gu, g, px, py, pz) in enumerate(shrap_groups):
        kfs = [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.7, px * 0.8, py * 1.2, pz * 0.8),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.5, 1, 1, 1),
            make_keyframe("scale", 0.7, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("dissipate", length=0.7, animators=diss, loop="once", override=True)

    b.write(OUTPUT_PATH)
    import os
    sz = os.path.getsize(OUTPUT_PATH)
    print(f"Wrote {OUTPUT_PATH} ({sz} bytes, {sz/1024:.1f} KB)")


if __name__ == "__main__":
    build()
