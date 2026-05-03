#!/usr/bin/env python3
"""
Generator for rainbow_crash_full_spectrum.bbmodel

RAINBOW CRASH WAVE - "Full Spectrum"
A shockwave of pure rainbow energy rolling outward.

Geometry:
  - 7 concentric flat ring bones at Y=0..1, one per rainbow colour, each ring
    composed of 12 flat slabs. Stacked 0.15 vertical apart so cross-section
    reads as a rainbow.
  - 12 upright flat slab pieces angled upward at outer ring (rising arc peak).
    Each own bone.
  - 6 flat aftermath slabs at Y=0 inside inner ring (texture 1).
  - 16 sparkle scatter cubes at various heights (each own bone).

Textures:
  - 0 (rainbow_band): 7 horizontal colour bands red->violet, 9 rows each.
  - 1 (rainbow_aftermath): pastel cream with faded coloured arcs.

Animations:
  - spawn (0.5s, override=true)
  - idle (3.0s, loop, override=false)
  - dissipate (0.5s, override=true)
"""

import math
import sys
from pathlib import Path

sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder,
    make_uuid,
    make_keyframe,
    basic_cube_faces,
    uniform_face,
    png_from_pixels,
    hex_to_rgba,
    fill_rect,
    gradient_horizontal_band,
    gradient_radial,
    add_noise_overlay,
    color_lerp,
)


# -----------------------------------------------------------------------------
# Texture 0 — Rainbow band (7 horizontal bands, 9 rows each)
#   row  0- 8 : red     #ff6060
#   row  9-17 : orange  #ffb040
#   row 18-26 : yellow  #ffe040
#   row 27-35 : green   #80e860
#   row 36-44 : blue    #60b0ff
#   row 45-53 : indigo  #8060e0
#   row 54-62 : violet  #c060e0
#   row 63    : leftover row — paint violet edge shadow
# Each band: top-edge highlight (lighter row at the top), bottom-edge shadow.
# -----------------------------------------------------------------------------
def build_rainbow_band_texture():
    W, H = 64, 64
    base_colors = [
        ("#ff6060", 0,  9),   # red
        ("#ffb040", 9,  18),  # orange
        ("#ffe040", 18, 27),  # yellow
        ("#80e860", 27, 36),  # green
        ("#60b0ff", 36, 45),  # blue
        ("#8060e0", 45, 54),  # indigo
        ("#c060e0", 54, 63),  # violet
    ]
    pixels = [hex_to_rgba("#000000")] * (W * H)

    def lighten(c, amt=40):
        r, g, b, a = c
        return (min(255, r + amt), min(255, g + amt), min(255, b + amt), a)

    def darken(c, amt=50):
        r, g, b, a = c
        return (max(0, r - amt), max(0, g - amt), max(0, b - amt), a)

    for hex_color, y0, y1 in base_colors:
        col = hex_to_rgba(hex_color)
        hi  = lighten(col, 50)
        lo  = darken(col, 60)
        # main band fill
        fill_rect(pixels, W, H, 0, y0, W, y1, col)
        # top-edge highlight (1 row)
        fill_rect(pixels, W, H, 0, y0, W, y0 + 1, hi)
        # second highlight row (slightly lighter still)
        fill_rect(pixels, W, H, 0, y0 + 1, W, y0 + 2, lighten(col, 25))
        # bottom-edge shadow (1 row)
        fill_rect(pixels, W, H, 0, y1 - 1, W, y1, lo)
        # vertical glints every 8 px to break up flat colour
        for x in range(4, W, 8):
            pixels[(y0 + 3) * W + x] = hi
            pixels[(y0 + 5) * W + x] = lighten(col, 15)
            pixels[(y1 - 3) * W + x] = darken(col, 25)

    # row 63 violet shadow
    violet_lo = darken(hex_to_rgba("#c060e0"), 80)
    fill_rect(pixels, W, H, 0, 63, W, 64, violet_lo)

    # subtle scanline shimmer every 4 columns
    for y in range(H):
        for x in range(W):
            if x % 16 == 0:
                r, g, b, a = pixels[y * W + x]
                pixels[y * W + x] = (
                    min(255, r + 18),
                    min(255, g + 18),
                    min(255, b + 18),
                    a,
                )

    # sparse white sparkles (rainbow specular)
    spec = (255, 255, 255, 255)
    sparkle_spots = [
        (5, 2), (14, 4), (29, 6), (45, 3), (58, 5),
        (8, 12), (24, 14), (40, 11), (54, 13),
        (12, 21), (32, 23), (50, 20),
        (6, 30), (22, 32), (44, 30), (60, 33),
        (10, 40), (28, 41), (47, 39),
        (14, 49), (36, 47), (56, 50),
        (18, 58), (38, 56), (52, 58),
    ]
    for (sx, sy) in sparkle_spots:
        if 0 <= sx < W - 1 and 0 <= sy < H - 1:
            pixels[sy * W + sx] = spec
            pixels[sy * W + sx + 1] = spec

    # Light noise to break up banding so it doesn't look flat
    add_noise_overlay(pixels, W, H, hex_to_rgba("#000000", 60), spec, density=0.03, seed=77)
    return png_from_pixels(pixels, W, H)


# -----------------------------------------------------------------------------
# Texture 1 — Aftermath / inner disc
# Pastel rainbow faded to cream with faint coloured arcs.
# -----------------------------------------------------------------------------
def build_aftermath_texture():
    W, H = 64, 64
    cream = hex_to_rgba("#fff8f0")
    pale_red    = hex_to_rgba("#ffd8d8")
    pale_orange = hex_to_rgba("#ffe6c8")
    pale_yellow = hex_to_rgba("#fff4c8")
    pale_green  = hex_to_rgba("#d8f4c8")
    pale_blue   = hex_to_rgba("#c8e0ff")
    pale_indigo = hex_to_rgba("#d4c8f0")
    pale_violet = hex_to_rgba("#e8c8f0")
    pixels = [cream] * (W * H)

    # Faint coloured arcs centred at bottom (y=64) curving across the texture
    arc_colors = [pale_red, pale_orange, pale_yellow, pale_green, pale_blue, pale_indigo, pale_violet]
    cx, cy = 32, 70
    for i, col in enumerate(arc_colors):
        radius_outer = 64 - i * 7
        radius_inner = radius_outer - 5
        for y in range(H):
            for x in range(W):
                d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
                if radius_inner <= d <= radius_outer:
                    # blend toward pastel
                    base = pixels[y * W + x]
                    pixels[y * W + x] = color_lerp(base, col, 0.55)

    # Soft radial bloom from centre (slight pinkish-cream emissive)
    bloom = hex_to_rgba("#fff0e8")
    for y in range(H):
        for x in range(W):
            d = math.sqrt((x - 32) ** 2 + (y - 32) ** 2)
            if d < 18:
                t = d / 18.0
                base = pixels[y * W + x]
                pixels[y * W + x] = color_lerp(bloom, base, t)

    # Tiny pastel sparkle dots scattered
    sparkles = [
        (12, 8), (40, 14), (28, 20), (52, 26),
        (16, 32), (44, 36), (8, 44), (36, 50), (24, 56),
    ]
    for (sx, sy) in sparkles:
        if 0 <= sx < W - 1 and 0 <= sy < H - 1:
            pixels[sy * W + sx] = (255, 252, 248, 255)
            pixels[sy * W + sx + 1] = (250, 240, 240, 255)

    # Very light noise
    add_noise_overlay(pixels, W, H, hex_to_rgba("#e8d8c8"), (255, 255, 255, 255),
                      density=0.04, seed=33)
    return png_from_pixels(pixels, W, H)


# -----------------------------------------------------------------------------
# Build model
# -----------------------------------------------------------------------------
b = Builder("rainbow_crash_full_spectrum", resolution=(64, 64), visible_box=(8, 8, 0))


# Ring config — 7 colours
# Each ring i has its V band on texture 0
# Inner ring smallest, outer ring largest
# Vertical spacing 0.15 units => total cross-section height ~ 1.0
NUM_RINGS = 7
SLABS_PER_RING = 12

# UV row windows for each colour (top, bottom inclusive-exclusive)
ring_uv_rows = [
    (1,  8),    # red    (skip top hi-row + bottom lo-row to land on solid colour)
    (10, 17),   # orange
    (19, 26),   # yellow
    (28, 35),   # green
    (37, 44),   # blue
    (46, 53),   # indigo
    (55, 62),   # violet
]

# Ring geometry: radii go from 3.0 (inner) to 9.0 (outer)
INNER_R = 3.0
OUTER_R = 9.0

ring_bones = []        # (uuid, name, ring_index)
ring_root_children_per_ring = []  # list of slab cube uuids per ring


for r_idx in range(NUM_RINGS):
    t = r_idx / (NUM_RINGS - 1)
    radius = INNER_R + t * (OUTER_R - INNER_R)
    y_base = r_idx * 0.15  # stack vertically 0.15 apart
    y_top  = y_base + 0.14  # very thin slab (flat)

    # UV strip for this ring
    v0, v1 = ring_uv_rows[r_idx]

    slab_cubes = []
    for s_idx in range(SLABS_PER_RING):
        a0 = (s_idx / SLABS_PER_RING) * math.tau
        a1 = ((s_idx + 1) / SLABS_PER_RING) * math.tau
        # midpoint angle
        mid_a = (a0 + a1) * 0.5

        # Slab approximation: place a flat radial slab, rotate via group
        # We model each slab as a flat rectangle in local space, then wrap in
        # bone with rotation Y = math.degrees(mid_a). Place along +X axis.
        slab_w = 2.0 * radius * math.tan(math.pi / SLABS_PER_RING)  # chord-ish width
        slab_thick = 0.85  # radial thickness (along X)
        slab_height = y_top - y_base

        # cube bounds (centred on radius along +X)
        from_x = radius - slab_thick * 0.5
        to_x   = radius + slab_thick * 0.5
        from_y = y_base
        to_y   = y_top
        from_z = -slab_w * 0.5
        to_z   =  slab_w * 0.5

        # Per-face UVs sized to this ring's V band
        uv = [0, v0, 16, v1]
        faces = {
            "north": {"uv": uv, "texture": 0},
            "east":  {"uv": uv, "texture": 0},
            "south": {"uv": uv, "texture": 0},
            "west":  {"uv": uv, "texture": 0},
            "up":    {"uv": [0, v0, 16, v1], "texture": 0},
            "down":  {"uv": [0, v0, 16, v1], "texture": 0},
        }

        # We'll place every slab in its own positioning bone so the slab can
        # rotate around the ring's centre — but to save bone count we just
        # bake the rotation into the geometry by computing the world coords:
        cos_a = math.cos(mid_a)
        sin_a = math.sin(mid_a)
        # Local corners (slab_thick along radial X, slab_w tangential along Z)
        # Build rotated-in-XZ AA cube. Since cube is axis-aligned, we approximate
        # by giving the cube an explicit non-zero rotation Y on the element.
        cx = radius * cos_a
        cz = radius * sin_a
        cy_mid = (from_y + to_y) * 0.5
        cube_uuid = b.add_cube(
            f"ring{r_idx}_slab{s_idx}",
            from_=[cx - slab_thick * 0.5, from_y, cz - slab_w * 0.5],
            to_=[cx + slab_thick * 0.5,   to_y,   cz + slab_w * 0.5],
            origin=[cx, cy_mid, cz],
            rotation=[0, math.degrees(mid_a), 0],
            faces=faces,
        )
        slab_cubes.append(cube_uuid)

    ring_bone = b.make_group(
        f"ring{r_idx}",
        origin=[0, y_base + 0.07, 0],
        children=slab_cubes,
    )
    ring_bones.append((ring_bone["uuid"], ring_bone["name"], r_idx))
    ring_root_children_per_ring.append(ring_bone)


# -----------------------------------------------------------------------------
# Arc peak — 12 upright flat slabs at outer ring radius, angled upward slightly
# Each own bone for individual animation.
# -----------------------------------------------------------------------------
arc_peak_bones = []  # (uuid, name, mid_a)
arc_root_children = []

ARC_COUNT = 12
ARC_RADIUS = OUTER_R + 0.4  # just outside outer ring
ARC_HEIGHT = 3.0

for s_idx in range(ARC_COUNT):
    mid_a = ((s_idx + 0.5) / ARC_COUNT) * math.tau
    cos_a = math.cos(mid_a)
    sin_a = math.sin(mid_a)
    cx = ARC_RADIUS * cos_a
    cz = ARC_RADIUS * sin_a

    # Upright slab: thin in X (radial), thin in Z (tangential), tall in Y
    slab_w_radial = 0.5
    slab_w_tang = 1.6
    slab_h = ARC_HEIGHT

    # UV: span all rainbow rows (full spectrum vertical strip on each arc piece)
    arc_uv = [0, 1, 16, 62]
    faces = {
        "north": {"uv": arc_uv, "texture": 0},
        "east":  {"uv": arc_uv, "texture": 0},
        "south": {"uv": arc_uv, "texture": 0},
        "west":  {"uv": arc_uv, "texture": 0},
        "up":    {"uv": [0, 1, 8, 8], "texture": 0},
        "down":  {"uv": [0, 1, 8, 8], "texture": 0},
    }

    cube_uuid = b.add_cube(
        f"arc_peak_{s_idx}",
        from_=[cx - slab_w_radial * 0.5, 0.4, cz - slab_w_tang * 0.5],
        to_=[cx + slab_w_radial * 0.5,   0.4 + slab_h, cz + slab_w_tang * 0.5],
        origin=[cx, 0.4, cz],
        rotation=[0, math.degrees(mid_a), -8],  # slight outward tilt
        faces=faces,
    )

    arc_bone = b.make_group(
        f"arc_peak_bone_{s_idx}",
        origin=[cx, 0.4, cz],
        children=[cube_uuid],
    )
    arc_peak_bones.append((arc_bone["uuid"], arc_bone["name"], mid_a))
    arc_root_children.append(arc_bone)


# -----------------------------------------------------------------------------
# Inner aftermath disc — 6 flat slabs at Y=0 inside inner ring (texture 1)
# -----------------------------------------------------------------------------
inner_disc_bones = []  # single bone holding all 6 cubes, plus we add one bone
INNER_DISC_COUNT = 6
INNER_DISC_RADIUS = INNER_R - 0.6  # comfortably inside inner ring

inner_disc_cubes = []
for s_idx in range(INNER_DISC_COUNT):
    mid_a = (s_idx / INNER_DISC_COUNT) * math.tau
    cx = INNER_DISC_RADIUS * 0.5 * math.cos(mid_a)
    cz = INNER_DISC_RADIUS * 0.5 * math.sin(mid_a)

    slab_w = INNER_DISC_RADIUS * 0.9
    aftermath_uv = [4, 4, 60, 60]
    faces = {
        d: {"uv": aftermath_uv, "texture": 1}
        for d in ("north", "east", "south", "west", "up", "down")
    }
    cube_uuid = b.add_cube(
        f"inner_disc_{s_idx}",
        from_=[cx - slab_w * 0.5, -0.05, cz - slab_w * 0.5],
        to_=[cx + slab_w * 0.5,    0.05, cz + slab_w * 0.5],
        origin=[cx, 0.0, cz],
        rotation=[0, math.degrees(mid_a), 0],
        faces=faces,
    )
    inner_disc_cubes.append(cube_uuid)

inner_disc_bone = b.make_group(
    "inner_disc",
    origin=[0, 0, 0],
    children=inner_disc_cubes,
)
inner_disc_bones.append((inner_disc_bone["uuid"], inner_disc_bone["name"]))


# -----------------------------------------------------------------------------
# Sparkle scatter — 16 tiny flat cubes at varying heights/positions
# Each own bone.
# -----------------------------------------------------------------------------
sparkle_bones = []  # (uuid, name, base_pos)
sparkle_root_children = []

import random as _rng
_rng_inst = _rng.Random(2025)

for sp_idx in range(16):
    # Random angle, random radius between 1.0 and OUTER_R + 1.5
    ang = _rng_inst.random() * math.tau
    rad = 1.0 + _rng_inst.random() * (OUTER_R + 0.5)
    height = 0.2 + _rng_inst.random() * 2.6
    px = math.cos(ang) * rad
    pz = math.sin(ang) * rad
    size = 0.25 + _rng_inst.random() * 0.25

    # Pick which rainbow band this sparkle uses
    band = _rng_inst.randrange(NUM_RINGS)
    v0, v1 = ring_uv_rows[band]
    sp_uv = [2, v0, 8, v1]
    faces = {
        d: {"uv": sp_uv, "texture": 0}
        for d in ("north", "east", "south", "west", "up", "down")
    }

    cube_uuid = b.add_cube(
        f"sparkle_{sp_idx}",
        from_=[px - size, height - size, pz - size],
        to_=[px + size,   height + size, pz + size],
        origin=[px, height, pz],
        rotation=[
            _rng_inst.uniform(-30, 30),
            _rng_inst.uniform(0, 360),
            _rng_inst.uniform(-30, 30),
        ],
        faces=faces,
    )
    sp_bone = b.make_group(
        f"sparkle_bone_{sp_idx}",
        origin=[px, height, pz],
        children=[cube_uuid],
    )
    sparkle_bones.append((sp_bone["uuid"], sp_bone["name"], (px, height, pz, ang)))
    sparkle_root_children.append(sp_bone)


# -----------------------------------------------------------------------------
# Master root bone
# -----------------------------------------------------------------------------
master = b.make_group(
    "root",
    origin=[0, 0, 0],
    children=(
        ring_root_children_per_ring
        + arc_root_children
        + [inner_disc_bone]
        + sparkle_root_children
    ),
)
b.outliner.append(master)


# =============================================================================
# ANIMATIONS
# =============================================================================

# ---------- SPAWN (0.5s, override=true) ----------
# All 7 rings: scale 0 -> 1.08 (overshoot at 0.3s) -> 1.0 simultaneously
# Arc peak pieces: rise upward at 0.3s
# Sparkles scatter at 0.2s
# Inner disc fades in at 0.3s
spawn_animators = {}

for (uuid_, name, r_idx) in ring_bones:
    kfs = [
        make_keyframe("scale", 0.0,  0.05, 0.05, 0.05),
        make_keyframe("scale", 0.10, 0.10, 0.20, 0.10),  # quick early reveal vertically
        make_keyframe("scale", 0.30, 1.08, 1.08, 1.08),  # overshoot
        make_keyframe("scale", 0.40, 1.0,  1.0,  1.0),
        make_keyframe("scale", 0.5,  1.0,  1.0,  1.0),
    ]
    spawn_animators[uuid_] = {
        "name": name, "type": "bone", "keyframes": kfs,
    }

for (uuid_, name, mid_a) in arc_peak_bones:
    # Rise upward: position Y from -1.0 (hidden in ring) to +0.5 by 0.3s
    pos_kfs = [
        make_keyframe("position", 0.0, 0, -1.5, 0),
        make_keyframe("position", 0.15, 0, -1.5, 0),
        make_keyframe("position", 0.30, 0,  0.6, 0),
        make_keyframe("position", 0.4,  0,  0.4, 0),
        make_keyframe("position", 0.5,  0,  0.4, 0),
    ]
    sc_kfs = [
        make_keyframe("scale", 0.0, 0.05, 0.05, 0.05),
        make_keyframe("scale", 0.20, 0.05, 0.05, 0.05),
        make_keyframe("scale", 0.30, 1.0, 1.0, 1.0),
        make_keyframe("scale", 0.5, 1.0, 1.0, 1.0),
    ]
    spawn_animators[uuid_] = {
        "name": name, "type": "bone", "keyframes": pos_kfs + sc_kfs,
    }

for idx, (uuid_, name, base_pos) in enumerate(sparkle_bones):
    px, ph, pz, ang = base_pos
    # Sparkles burst outward + upward at 0.2s
    out_x = math.cos(ang) * 0.6
    out_z = math.sin(ang) * 0.6
    pos_kfs = [
        make_keyframe("position", 0.0, 0, -ph * 0.5, 0),  # below initial
        make_keyframe("position", 0.18, 0, -ph * 0.5, 0),
        make_keyframe("position", 0.30, out_x * 0.6, 0.4, out_z * 0.6),
        make_keyframe("position", 0.5, out_x, 0.0, out_z),
    ]
    sc_kfs = [
        make_keyframe("scale", 0.0,  0.01, 0.01, 0.01),
        make_keyframe("scale", 0.20, 0.01, 0.01, 0.01),
        make_keyframe("scale", 0.28, 1.4, 1.4, 1.4),
        make_keyframe("scale", 0.40, 1.0, 1.0, 1.0),
        make_keyframe("scale", 0.5,  1.0, 1.0, 1.0),
    ]
    spawn_animators[uuid_] = {
        "name": name, "type": "bone", "keyframes": pos_kfs + sc_kfs,
    }

for (uuid_, name) in inner_disc_bones:
    sc_kfs = [
        make_keyframe("scale", 0.0,  0.01, 0.01, 0.01),
        make_keyframe("scale", 0.28, 0.01, 0.01, 0.01),
        make_keyframe("scale", 0.42, 1.05, 1.0,  1.05),
        make_keyframe("scale", 0.5,  1.0,  1.0,  1.0),
    ]
    spawn_animators[uuid_] = {
        "name": name, "type": "bone", "keyframes": sc_kfs,
    }

b.add_animation("spawn", length=0.5, animators=spawn_animators, loop="once", override=True)


# ---------- IDLE (3.0s, loop, override=false) ----------
# Each of 7 rings rotate Y at slightly different speeds (inner fastest)
# Inner-fastest-to-outer-slowest creates rainbow spiral effect
idle_animators = {}

# Ring rotations — full revolution speed varies per ring
# Inner ring (0) does ~1.0 rev / 3s; outer ring (6) does ~0.4 rev / 3s
for (uuid_, name, r_idx) in ring_bones:
    inv_t = (NUM_RINGS - 1 - r_idx) / (NUM_RINGS - 1)
    revs = 0.4 + inv_t * 0.6  # 0.4 (outer) -> 1.0 (inner)
    direction = 1 if r_idx % 2 == 0 else -1  # alternate for spiral feel
    samples = 6
    rot_kfs = []
    for s in range(samples + 1):
        t = (s / samples) * 3.0
        deg = direction * (t / 3.0) * revs * 360.0
        rot_kfs.append(make_keyframe("rotation", t, 0, deg, 0))
    idle_animators[uuid_] = {
        "name": name, "type": "bone", "keyframes": rot_kfs,
    }

# Arc peak: pulse scale 0.92 - 1.08
for idx, (uuid_, name, mid_a) in enumerate(arc_peak_bones):
    period = 1.6 + (idx % 4) * 0.18
    phase = (idx * 0.27) % 1.0
    kfs = []
    samples = 7
    for s in range(samples + 1):
        t = (s / samples) * 3.0
        val = math.sin(((t / period) + phase) * math.tau) * 0.08 + 1.0
        kfs.append(make_keyframe("scale", t, val, val * 1.05, val))
    idle_animators[uuid_] = {
        "name": name, "type": "bone", "keyframes": kfs,
    }

# Sparkles drift + tumble
for idx, (uuid_, name, base_pos) in enumerate(sparkle_bones):
    px, ph, pz, ang = base_pos
    period_pos = 2.0 + (idx % 5) * 0.25
    phase_pos = (idx * 0.31) % 1.0
    period_rot = 1.5 + (idx % 6) * 0.18
    phase_rot = (idx * 0.41) % 1.0
    pos_kfs = []
    rot_kfs = []
    samples = 6
    for s in range(samples + 1):
        t = (s / samples) * 3.0
        # drift in plane
        dx = math.sin(((t / period_pos) + phase_pos) * math.tau) * 0.4
        dz = math.cos(((t / period_pos) + phase_pos * 1.3) * math.tau) * 0.4
        dy = math.sin(((t / period_pos) + phase_pos * 0.7) * math.tau) * 0.3
        pos_kfs.append(make_keyframe("position", t, dx, dy, dz))
        # tumble
        rx = (t / period_rot + phase_rot) * 360.0
        ry = (t / (period_rot * 0.8) + phase_rot * 0.6) * 360.0
        rz = (t / (period_rot * 1.1) + phase_rot * 1.2) * 360.0
        rot_kfs.append(make_keyframe("rotation", t, rx % 360, ry % 360, rz % 360))
    idle_animators[uuid_] = {
        "name": name, "type": "bone",
        "keyframes": pos_kfs + rot_kfs,
    }

# Inner disc slowly rotates
for (uuid_, name) in inner_disc_bones:
    rot_kfs = [
        make_keyframe("rotation", 0.0, 0, 0,   0),
        make_keyframe("rotation", 1.0, 0, 60,  0),
        make_keyframe("rotation", 2.0, 0, 120, 0),
        make_keyframe("rotation", 3.0, 0, 180, 0),
    ]
    idle_animators[uuid_] = {
        "name": name, "type": "bone", "keyframes": rot_kfs,
    }

b.add_animation("idle", length=3.0, animators=idle_animators, loop="loop", override=False)


# ---------- DISSIPATE (0.5s, override=true) ----------
# Rings rapidly expand outward and scale to 0 from inner -> outer with 0.04s stagger.
# Sparkles follow outward momentum.
# Arc pieces continue upward and scale to 0.
dissipate_animators = {}

for (uuid_, name, r_idx) in ring_bones:
    stagger = r_idx * 0.04
    kfs = [
        make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
        make_keyframe("scale", stagger, 1.0, 1.0, 1.0),
        make_keyframe("scale", stagger + 0.10, 1.45, 0.8, 1.45),  # expand outward
        make_keyframe("scale", stagger + 0.22, 1.7, 0.3, 1.7),
        make_keyframe("scale", min(0.5, stagger + 0.28), 0.05, 0.05, 0.05),
        make_keyframe("scale", 0.5, 0.0, 0.0, 0.0),
    ]
    dissipate_animators[uuid_] = {
        "name": name, "type": "bone", "keyframes": kfs,
    }

for idx, (uuid_, name, mid_a) in enumerate(arc_peak_bones):
    out_x = math.cos(mid_a) * 1.2
    out_z = math.sin(mid_a) * 1.2
    pos_kfs = [
        make_keyframe("position", 0.0, 0, 0.4, 0),
        make_keyframe("position", 0.20, out_x * 0.5, 1.5, out_z * 0.5),
        make_keyframe("position", 0.5,  out_x, 3.5, out_z),
    ]
    sc_kfs = [
        make_keyframe("scale", 0.0,  1.0, 1.0, 1.0),
        make_keyframe("scale", 0.25, 0.7, 1.2, 0.7),
        make_keyframe("scale", 0.5,  0.01, 0.01, 0.01),
    ]
    dissipate_animators[uuid_] = {
        "name": name, "type": "bone", "keyframes": pos_kfs + sc_kfs,
    }

for idx, (uuid_, name, base_pos) in enumerate(sparkle_bones):
    px, ph, pz, ang = base_pos
    out_x = math.cos(ang) * 2.0
    out_z = math.sin(ang) * 2.0
    pos_kfs = [
        make_keyframe("position", 0.0, 0, 0, 0),
        make_keyframe("position", 0.25, out_x * 0.6, 0.6, out_z * 0.6),
        make_keyframe("position", 0.5, out_x, 1.4, out_z),
    ]
    sc_kfs = [
        make_keyframe("scale", 0.0,  1.0, 1.0, 1.0),
        make_keyframe("scale", 0.30, 0.7, 0.7, 0.7),
        make_keyframe("scale", 0.5,  0.01, 0.01, 0.01),
    ]
    dissipate_animators[uuid_] = {
        "name": name, "type": "bone", "keyframes": pos_kfs + sc_kfs,
    }

for (uuid_, name) in inner_disc_bones:
    sc_kfs = [
        make_keyframe("scale", 0.0,  1.0, 1.0, 1.0),
        make_keyframe("scale", 0.20, 1.3, 1.0, 1.3),
        make_keyframe("scale", 0.5,  0.01, 0.01, 0.01),
    ]
    dissipate_animators[uuid_] = {
        "name": name, "type": "bone", "keyframes": sc_kfs,
    }

b.add_animation("dissipate", length=0.5, animators=dissipate_animators,
                loop="once", override=True)


# -----------------------------------------------------------------------------
# Textures + write
# -----------------------------------------------------------------------------
b.add_texture("rainbow_band", build_rainbow_band_texture())
b.add_texture("rainbow_aftermath", build_aftermath_texture())

OUT = "D:/CC/ChaosCraft/src/main/resources/models/fluffy/me_attacks/rainbow_crash_full_spectrum.bbmodel"
path = b.write(OUT)
size_kb = Path(path).stat().st_size // 1024
print(f"Wrote: {path}")
print(f"Size: {size_kb} KB")
print(f"Elements: {len(b.elements)}")
print(f"Animations: {[a['name'] for a in b.animations]}")
