#!/usr/bin/env python3
"""
Generator for candy_planet_sugar_rush.bbmodel — ULTIMATE Fluffy Mode summon.

CANDY PLANET ORBITAL — "Sugar Rush"
Miniature candy solar system orbiting caster. Central candy sun, 5 candy planets
in pastel colours at different orbital radii/heights, each with candy-rings made
of wrapped sweets, 2 with orbiting moons. Whole system slowly rotates like an
orrery made of sweets — LARGEST MOST COMPLEX MODEL IN MODE.
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


# =============================================================================
# TEXTURE 0 — CANDY PLANET (6 zones)
# =============================================================================
SUN_ROWS = (0, 11)
P1_ROWS  = (11, 21)   # pink   #ffb8cc
P2_ROWS  = (21, 31)   # mint   #b8ffd0
P3_ROWS  = (31, 41)   # lemon  #fff0b0
P4_ROWS  = (41, 51)   # lavender #d8b8ff
P5_ROWS  = (51, 64)   # sky    #b8e8ff


def lighten(c, amt=40):
    r, g, b, a = c
    return (min(255, r + amt), min(255, g + amt), min(255, b + amt), a)


def darken(c, amt=40):
    r, g, b, a = c
    return (max(0, r - amt), max(0, g - amt), max(0, b - amt), a)


def paint_candy_zone(pixels, w, y0, y1, base_hex, swirl_offset=0):
    """Solid base colour, candy-wrap swirl stripes, edge highlights, sparkles."""
    base = hex_to_rgba(base_hex)
    hi = lighten(base, 50)
    hi2 = lighten(base, 30)
    lo = darken(base, 50)
    lo2 = darken(base, 25)

    fill_rect(pixels, w, 64, 0, y0, w, y1, base)
    band_h = y1 - y0
    if band_h <= 0:
        return

    fill_rect(pixels, w, 64, 0, y0, w, y0 + 1, hi)
    if band_h >= 4:
        fill_rect(pixels, w, 64, 0, y0 + 1, w, y0 + 2, hi2)
    fill_rect(pixels, w, 64, 0, y1 - 1, w, y1, lo)
    if band_h >= 4:
        fill_rect(pixels, w, 64, 0, y1 - 2, w, y1 - 1, lo2)

    cy = (y0 + y1) * 0.5
    amp = max(1, (band_h - 4) * 0.35)
    for x in range(w):
        phase = (x / w) * math.tau * 2.0 + swirl_offset
        sy_int = int(cy + math.sin(phase) * amp)
        if y0 + 2 <= sy_int < y1 - 1:
            pixels[sy_int * w + x] = hi
        sy2_int = int(cy + math.sin(phase + math.pi) * amp)
        if y0 + 2 <= sy2_int < y1 - 1:
            pixels[sy2_int * w + x] = lo2

    # Diagonal candy-wrap accent
    for diag in range(-w, w * 2, 8):
        for y in range(y0 + 2, y1 - 1):
            x = (diag + (y - y0)) % w
            if 0 <= x < w:
                cur = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(cur, hi, 0.2)

    spark_positions = [(8, y0 + 3), (22, y0 + 5), (36, y0 + 4),
                       (50, y0 + 6), (60, y0 + 3)]
    if band_h >= 6:
        for (sx, sy) in spark_positions:
            if 0 <= sx < w and y0 < sy < y1:
                pixels[sy * w + sx] = (255, 255, 255, 255)
                if sx + 1 < w:
                    pixels[sy * w + sx + 1] = lighten(base, 80)


def build_candy_planet_texture():
    W, H = 64, 64
    pixels = [hex_to_rgba("#000000")] * (W * H)

    paint_candy_zone(pixels, W, *SUN_ROWS, "#fffce0", swirl_offset=0.0)
    paint_candy_zone(pixels, W, *P1_ROWS,  "#ffb8cc", swirl_offset=0.7)
    paint_candy_zone(pixels, W, *P2_ROWS,  "#b8ffd0", swirl_offset=1.4)
    paint_candy_zone(pixels, W, *P3_ROWS,  "#fff0b0", swirl_offset=2.1)
    paint_candy_zone(pixels, W, *P4_ROWS,  "#d8b8ff", swirl_offset=2.8)
    paint_candy_zone(pixels, W, *P5_ROWS,  "#b8e8ff", swirl_offset=3.5)

    add_noise_overlay(pixels, W, H, hex_to_rgba("#ffffff", 200),
                      hex_to_rgba("#fff0e0"), density=0.025, seed=1234)

    for (y0, y1) in [SUN_ROWS, P1_ROWS, P2_ROWS, P3_ROWS, P4_ROWS, P5_ROWS]:
        glint_y = y0 + 2
        if glint_y < y1:
            for x in (16, 32, 48):
                if x < W:
                    pixels[glint_y * W + x] = (255, 255, 255, 255)

    return png_from_pixels(pixels, W, H)


# =============================================================================
# TEXTURE 1 — STARFIELD
# =============================================================================
def build_starfield_texture():
    W, H = 64, 64
    deep_space = hex_to_rgba("#060810")
    star = hex_to_rgba("#ffe0a0")
    star_dim = hex_to_rgba("#c0a060")
    nebula = hex_to_rgba("#301848")
    nebula_dim = hex_to_rgba("#180a28")

    pixels = [deep_space] * (W * H)

    import random as _rng
    rng = _rng.Random(7777)

    for _ in range(16):
        cx = rng.randrange(W)
        cy = rng.randrange(H)
        radius = rng.randint(4, 9)
        for y in range(max(0, cy - radius), min(H, cy + radius)):
            for x in range(max(0, cx - radius), min(W, cx + radius)):
                d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
                if d < radius:
                    t = 1.0 - (d / radius)
                    cur = pixels[y * W + x]
                    chosen = nebula if t > 0.5 else nebula_dim
                    pixels[y * W + x] = color_lerp(cur, chosen, t * 0.6)

    for _ in range(256):
        sx = rng.randrange(W)
        sy = rng.randrange(H)
        pixels[sy * W + sx] = star if rng.random() < 0.7 else star_dim
        if rng.random() < 0.05:
            for (dx, dy) in [(1, 0), (-1, 0), (0, 1), (0, -1)]:
                nx, ny = sx + dx, sy + dy
                if 0 <= nx < W and 0 <= ny < H:
                    pixels[ny * W + nx] = star_dim

    for (sx, sy) in [(8, 12), (24, 28), (45, 18), (52, 50), (16, 56)]:
        if 0 <= sx < W and 0 <= sy < H:
            pixels[sy * W + sx] = (255, 255, 255, 255)
            for (dx, dy) in [(1, 0), (-1, 0), (0, 1), (0, -1)]:
                nx, ny = sx + dx, sy + dy
                if 0 <= nx < W and 0 <= ny < H:
                    pixels[ny * W + nx] = star

    return png_from_pixels(pixels, W, H)


# =============================================================================
# BUILD MODEL
# =============================================================================
b = Builder("candy_planet_sugar_rush", resolution=(64, 64), visible_box=(20, 20, 0))


def sun_uv():
    return [2, SUN_ROWS[0] + 2, 14, SUN_ROWS[1] - 2]


def planet_uv(rows):
    return [2, rows[0] + 2, 14, rows[1] - 2]


def faces_for(uv_rect, tex=0):
    return {
        d: {"uv": uv_rect, "texture": tex}
        for d in ("north", "east", "south", "west", "up", "down")
    }


# -----------------------------------------------------------------------------
# CENTRAL CANDY SUN — 6 overlapping cubes approximating a sphere at Y=10
# -----------------------------------------------------------------------------
SUN_CENTER = (0.0, 10.0, 0.0)

sun_cubes = []
sun_offsets = [
    (0.0, 0.0, 0.0,   1.6, 1.6, 1.6),
    (0.7, 0.3, 0.0,   1.0, 1.0, 1.0),
    (-0.7, 0.3, 0.0,  1.0, 1.0, 1.0),
    (0.0, 0.3, 0.7,   1.0, 1.0, 1.0),
    (0.0, 0.3, -0.7,  1.0, 1.0, 1.0),
    (0.0, 0.9, 0.0,   1.0, 0.7, 1.0),
    (0.5, 0.5, 0.5,   0.7, 0.7, 0.7),  # diagonal nodules
    (-0.5, 0.5, -0.5, 0.7, 0.7, 0.7),
    (0.5, -0.4, 0.5,  0.7, 0.7, 0.7),
    (-0.5, -0.4, -0.5, 0.7, 0.7, 0.7),
    (0.0, -0.7, 0.0,  0.9, 0.5, 0.9),  # bottom cap
]
for i, (ox, oy, oz, sx, sy, sz) in enumerate(sun_offsets):
    cx = SUN_CENTER[0] + ox
    cy = SUN_CENTER[1] + oy
    cz = SUN_CENTER[2] + oz
    cube_uuid = b.add_cube(
        f"sun_core_{i}",
        from_=[cx - sx * 0.5, cy - sy * 0.5, cz - sz * 0.5],
        to_=[cx + sx * 0.5, cy + sy * 0.5, cz + sz * 0.5],
        origin=[cx, cy, cz],
        rotation=[(i * 13) % 60, (i * 23) % 60, (i * 17) % 60],
        faces=faces_for(sun_uv(), tex=0),
    )
    sun_cubes.append(cube_uuid)

sun_bone_dict = b.make_group("sun", origin=list(SUN_CENTER), children=sun_cubes)
sun_bone_uuid = sun_bone_dict["uuid"]


# -----------------------------------------------------------------------------
# SUN CORONA RAYS — 8 tapered spikes radiating from sun
# -----------------------------------------------------------------------------
corona_data = []  # list of (bone_dict, dir_vec)

corona_directions = [
    (1.0, 0.0, 0.0),
    (-1.0, 0.0, 0.0),
    (0.0, 1.0, 0.0),
    (0.0, -1.0, 0.0),
    (0.0, 0.0, 1.0),
    (0.0, 0.0, -1.0),
    (0.707, 0.0, 0.707),
    (-0.707, 0.0, -0.707),
]

SUN_R = 1.4
for r_idx, (dx, dy, dz) in enumerate(corona_directions):
    spike_len = 2.2
    base_x = SUN_CENTER[0] + dx * (SUN_R + 0.2)
    base_y = SUN_CENTER[1] + dy * (SUN_R + 0.2)
    base_z = SUN_CENTER[2] + dz * (SUN_R + 0.2)
    tip_x = base_x + dx * spike_len
    tip_y = base_y + dy * spike_len
    tip_z = base_z + dz * spike_len

    cx = (base_x + tip_x) * 0.5
    cy = (base_y + tip_y) * 0.5
    cz = (base_z + tip_z) * 0.5

    yaw = math.degrees(math.atan2(dz, dx))
    horiz = math.sqrt(dx * dx + dz * dz)
    pitch = math.degrees(math.atan2(dy, horiz)) if horiz > 0.001 else (90 if dy > 0 else -90)

    half_len = spike_len * 0.5
    half_thick = 0.25
    cube_uuid = b.add_cube(
        f"corona_spike_{r_idx}",
        from_=[cx - half_len, cy - half_thick, cz - half_thick],
        to_=[cx + half_len, cy + half_thick, cz + half_thick],
        origin=[cx, cy, cz],
        rotation=[0, yaw, pitch],
        faces=faces_for(sun_uv(), tex=0),
    )
    corona_bone = b.make_group(
        f"corona_bone_{r_idx}",
        origin=[base_x, base_y, base_z],
        children=[cube_uuid],
    )
    corona_data.append((corona_bone, (dx, dy, dz)))


# -----------------------------------------------------------------------------
# 5 PLANETS
# -----------------------------------------------------------------------------
PLANET_CONFIGS = [
    (5.0,  6.0,  P1_ROWS, True,  10,  "planet1_pink"),
    (7.5,  9.0,  P2_ROWS, False, 25,  "planet2_mint"),
    (9.5,  11.0, P3_ROWS, True,  -15, "planet3_lemon"),
    (11.5, 14.0, P4_ROWS, False, 35,  "planet4_lavender"),
    (13.0, 8.0,  P5_ROWS, False, -25, "planet5_sky"),
]

planet_data = []  # list of dicts with all relevant bone refs

for p_idx, (radius, height, zone_rows, has_moon, ring_tilt, name) in enumerate(PLANET_CONFIGS):
    start_angle = (p_idx * 72) % 360

    # Within orbit bone, planet sits at (radius, 0, 0) relative; world at (radius, height, 0)
    px = radius
    py = height
    pz = 0.0

    # === PLANET BODY — 3 overlapping cubes ===
    planet_cubes = []
    body_offsets = [
        (0.0, 0.0, 0.0,   1.0, 1.0, 1.0),
        (0.35, 0.1, 0.0,  0.7, 0.7, 0.7),
        (-0.2, -0.15, 0.25, 0.6, 0.6, 0.6),
    ]
    for c_idx, (ox, oy, oz, sx, sy, sz) in enumerate(body_offsets):
        cx = px + ox
        cy = py + oy
        cz = pz + oz
        cube_uuid = b.add_cube(
            f"{name}_body_{c_idx}",
            from_=[cx - sx * 0.5, cy - sy * 0.5, cz - sz * 0.5],
            to_=[cx + sx * 0.5, cy + sy * 0.5, cz + sz * 0.5],
            origin=[cx, cy, cz],
            rotation=[c_idx * 17, c_idx * 23, c_idx * 11],
            faces=faces_for(planet_uv(zone_rows), tex=0),
        )
        planet_cubes.append(cube_uuid)

    planet_inner_bone = b.make_group(
        f"{name}_body_bone",
        origin=[px, py, pz],
        children=planet_cubes,
    )

    # === PLANET RING — 6 flat slabs around planet ===
    ring_cubes = []
    RING_RADIUS = 1.3
    RING_SLAB_COUNT = 6
    for s_idx in range(RING_SLAB_COUNT):
        a = (s_idx / RING_SLAB_COUNT) * math.tau
        rcx = px + math.cos(a) * RING_RADIUS
        rcy = py
        rcz = pz + math.sin(a) * RING_RADIUS
        chord = 2.0 * RING_RADIUS * math.tan(math.pi / RING_SLAB_COUNT)
        cube_uuid = b.add_cube(
            f"{name}_ring_{s_idx}",
            from_=[rcx - 0.3, rcy - 0.06, rcz - chord * 0.5],
            to_=[rcx + 0.3, rcy + 0.06, rcz + chord * 0.5],
            origin=[rcx, rcy, rcz],
            rotation=[0, math.degrees(a), 0],
            faces=faces_for(planet_uv(zone_rows), tex=0),
        )
        ring_cubes.append(cube_uuid)

    planet_ring_bone = b.make_group(
        f"{name}_ring_bone",
        origin=[px, py, pz],
        children=ring_cubes,
        rotation=[ring_tilt, 0, 0],
    )

    # === MOON ===
    moon_orbit_bone = None
    if has_moon:
        moon_cubes = []
        moon_radius_orbit = 1.8
        moon_x = px + moon_radius_orbit
        moon_y = py + 0.4
        moon_z = pz
        moon_offsets = [
            (0.0, 0.0, 0.0, 0.45, 0.45, 0.45),
            (0.18, 0.05, 0.0, 0.30, 0.30, 0.30),
        ]
        for c_idx, (ox, oy, oz, sx, sy, sz) in enumerate(moon_offsets):
            cx = moon_x + ox
            cy = moon_y + oy
            cz = moon_z + oz
            moon_zone = P5_ROWS if p_idx == 0 else P4_ROWS
            cube_uuid = b.add_cube(
                f"{name}_moon_{c_idx}",
                from_=[cx - sx * 0.5, cy - sy * 0.5, cz - sz * 0.5],
                to_=[cx + sx * 0.5, cy + sy * 0.5, cz + sz * 0.5],
                origin=[cx, cy, cz],
                rotation=[c_idx * 13, c_idx * 19, c_idx * 7],
                faces=faces_for(planet_uv(moon_zone), tex=0),
            )
            moon_cubes.append(cube_uuid)
        moon_orbit_bone = b.make_group(
            f"{name}_moon_orbit",
            origin=[px, py, pz],
            children=moon_cubes,
        )

    # === ORBIT BONE (rotates around Y axis) ===
    orbit_children = [planet_inner_bone, planet_ring_bone]
    if moon_orbit_bone is not None:
        orbit_children.append(moon_orbit_bone)

    orbit_bone = b.make_group(
        f"{name}_orbit",
        origin=[0, height, 0],
        children=orbit_children,
        rotation=[0, start_angle, 0],
    )

    planet_data.append({
        "orbit": orbit_bone,
        "inner": planet_inner_bone,
        "ring": planet_ring_bone,
        "moon": moon_orbit_bone,
        "ring_tilt": ring_tilt,
        "p_idx": p_idx,
        "radius": radius,
        "height": height,
        "start_angle": start_angle,
        "name": name,
    })


# -----------------------------------------------------------------------------
# ORBITAL PATHS — 5 flat rings of 8 slabs each
# -----------------------------------------------------------------------------
orbital_path_data = []  # list of (bone_dict, p_idx)

for p_idx, (radius, height, _, _, _, _) in enumerate(PLANET_CONFIGS):
    path_cubes = []
    PATH_SLAB_COUNT = 8
    for s_idx in range(PATH_SLAB_COUNT):
        a = (s_idx / PATH_SLAB_COUNT) * math.tau
        cx = math.cos(a) * radius
        cz = math.sin(a) * radius
        chord = 2.0 * radius * math.tan(math.pi / PATH_SLAB_COUNT) * 0.85
        cube_uuid = b.add_cube(
            f"orbital_path_{p_idx}_slab_{s_idx}",
            from_=[cx - 0.25, height - 0.05, cz - chord * 0.5],
            to_=[cx + 0.25, height + 0.05, cz + chord * 0.5],
            origin=[cx, height, cz],
            rotation=[0, math.degrees(a), 0],
            faces=faces_for([4, 4, 60, 60], tex=1),
        )
        path_cubes.append(cube_uuid)
    path_bone = b.make_group(
        f"orbital_path_{p_idx}",
        origin=[0, height, 0],
        children=path_cubes,
    )
    orbital_path_data.append((path_bone, p_idx))


# -----------------------------------------------------------------------------
# GROUND MANIFESTATION — 3 concentric rings at Y=0
# -----------------------------------------------------------------------------
ground_ring_data = []  # list of (bone_dict, ring_idx, radius)

GROUND_RING_CONFIGS = [
    (4.0, 12),
    (8.0, 14),
    (12.0, 16),
]

for r_idx, (radius, slab_count) in enumerate(GROUND_RING_CONFIGS):
    ground_cubes = []
    for s_idx in range(slab_count):
        a = (s_idx / slab_count) * math.tau
        cx = math.cos(a) * radius
        cz = math.sin(a) * radius
        chord = 2.0 * radius * math.tan(math.pi / slab_count) * 0.9
        cube_uuid = b.add_cube(
            f"ground_ring_{r_idx}_slab_{s_idx}",
            from_=[cx - 0.4, 0.0, cz - chord * 0.5],
            to_=[cx + 0.4, 0.08, cz + chord * 0.5],
            origin=[cx, 0.04, cz],
            rotation=[0, math.degrees(a), 0],
            faces=faces_for([4, 4, 60, 60], tex=1),
        )
        ground_cubes.append(cube_uuid)
    ring_bone = b.make_group(
        f"ground_ring_{r_idx}",
        origin=[0, 0, 0],
        children=ground_cubes,
    )
    ground_ring_data.append((ring_bone, r_idx, radius))


# -----------------------------------------------------------------------------
# STARDUST — 16 tiny flat cube pieces
# -----------------------------------------------------------------------------
stardust_data = []  # list of (bone_dict, base_pos_tuple)

import random as _rng
sd_rng = _rng.Random(31415)

for sd_idx in range(32):
    ang = sd_rng.random() * math.tau
    rad = 2.0 + sd_rng.random() * 12.0
    height = 0.5 + sd_rng.random() * 14.0
    px = math.cos(ang) * rad
    pz = math.sin(ang) * rad
    size = 0.18 + sd_rng.random() * 0.22

    zone_idx = sd_rng.randrange(6)
    zones = [SUN_ROWS, P1_ROWS, P2_ROWS, P3_ROWS, P4_ROWS, P5_ROWS]
    zone_rows = zones[zone_idx]

    cube_uuid = b.add_cube(
        f"stardust_{sd_idx}",
        from_=[px - size, height - size, pz - size],
        to_=[px + size, height + size, pz + size],
        origin=[px, height, pz],
        rotation=[
            sd_rng.uniform(0, 360),
            sd_rng.uniform(0, 360),
            sd_rng.uniform(0, 360),
        ],
        faces=faces_for(planet_uv(zone_rows), tex=0),
    )
    sd_bone = b.make_group(
        f"stardust_bone_{sd_idx}",
        origin=[px, height, pz],
        children=[cube_uuid],
    )
    stardust_data.append((sd_bone, (px, height, pz, ang, rad)))


# -----------------------------------------------------------------------------
# MASTER ROOT — assemble all top-level groups
# -----------------------------------------------------------------------------
master_children = [sun_bone_dict]
master_children.extend(d[0] for d in corona_data)
master_children.extend(p["orbit"] for p in planet_data)
master_children.extend(d[0] for d in orbital_path_data)
master_children.extend(d[0] for d in ground_ring_data)
master_children.extend(d[0] for d in stardust_data)

master = b.make_group("root", origin=[0, 0, 0], children=master_children)
b.outliner.append(master)


# =============================================================================
# ANIMATIONS
# =============================================================================

# ---------- SPAWN (2.0s, override=true) ----------
spawn_animators = {}

# Ground rings expand at 0.0s
for (bone, r_idx, radius) in ground_ring_data:
    stagger = r_idx * 0.05
    kfs = [
        make_keyframe("scale", 0.0, 0.05, 0.05, 0.05),
        make_keyframe("scale", 0.0 + stagger, 0.05, 0.05, 0.05),
        make_keyframe("scale", 0.15 + stagger, 1.15, 0.6, 1.15),
        make_keyframe("scale", 0.25 + stagger, 1.0, 1.0, 1.0),
        make_keyframe("scale", 2.0, 1.0, 1.0, 1.0),
    ]
    rot_kfs = [
        make_keyframe("rotation", 0.0, 0, r_idx * 30.0, 0),
        make_keyframe("rotation", 2.0, 0, r_idx * 30.0 + 60.0, 0),
    ]
    spawn_animators[bone["uuid"]] = {
        "name": bone["name"], "type": "bone",
        "keyframes": kfs + rot_kfs,
    }

# Sun materialises at 0.2s
sun_spawn_kfs = [
    make_keyframe("scale", 0.0, 0.01, 0.01, 0.01),
    make_keyframe("scale", 0.18, 0.01, 0.01, 0.01),
    make_keyframe("scale", 0.30, 1.6, 1.6, 1.6),
    make_keyframe("scale", 0.45, 0.85, 0.85, 0.85),
    make_keyframe("scale", 0.60, 1.05, 1.05, 1.05),
    make_keyframe("scale", 0.75, 1.0, 1.0, 1.0),
    make_keyframe("scale", 1.78, 1.0, 1.0, 1.0),
    make_keyframe("scale", 1.84, 1.12, 1.12, 1.12),
    make_keyframe("scale", 1.92, 1.0, 1.0, 1.0),
    make_keyframe("scale", 2.0, 1.0, 1.0, 1.0),
]
sun_spawn_rot = [
    make_keyframe("rotation", 0.0, 0, 0, 0),
    make_keyframe("rotation", 0.5, 90, 60, 30),
    make_keyframe("rotation", 1.0, 180, 120, 60),
    make_keyframe("rotation", 1.5, 270, 180, 90),
    make_keyframe("rotation", 2.0, 360, 240, 120),
]
spawn_animators[sun_bone_uuid] = {
    "name": "sun", "type": "bone",
    "keyframes": sun_spawn_kfs + sun_spawn_rot,
}

# Corona rays shoot outward at 0.4s
for (bone, dir_vec) in corona_data:
    dx, dy, dz = dir_vec
    pos_kfs = [
        make_keyframe("position", 0.0, -dx * 0.3, -dy * 0.3, -dz * 0.3),
        make_keyframe("position", 0.40, -dx * 0.3, -dy * 0.3, -dz * 0.3),
        make_keyframe("position", 0.55, dx * 0.3, dy * 0.3, dz * 0.3),
        make_keyframe("position", 0.70, 0, 0, 0),
        make_keyframe("position", 1.78, 0, 0, 0),
        make_keyframe("position", 1.84, dx * 0.15, dy * 0.15, dz * 0.15),
        make_keyframe("position", 1.92, 0, 0, 0),
        make_keyframe("position", 2.0, 0, 0, 0),
    ]
    sc_kfs = [
        make_keyframe("scale", 0.0, 0.01, 0.01, 0.01),
        make_keyframe("scale", 0.40, 0.01, 0.01, 0.01),
        make_keyframe("scale", 0.55, 1.4, 1.4, 1.4),
        make_keyframe("scale", 0.70, 1.0, 1.0, 1.0),
        make_keyframe("scale", 2.0, 1.0, 1.0, 1.0),
    ]
    spawn_animators[bone["uuid"]] = {
        "name": bone["name"], "type": "bone",
        "keyframes": pos_kfs + sc_kfs,
    }

# Orbital paths expand 0.3s-0.6s staggered
for (bone, p_idx) in orbital_path_data:
    stagger = 0.30 + p_idx * 0.06
    kfs = [
        make_keyframe("scale", 0.0, 0.01, 0.01, 0.01),
        make_keyframe("scale", stagger, 0.01, 0.01, 0.01),
        make_keyframe("scale", stagger + 0.08, 1.25, 1.0, 1.25),
        make_keyframe("scale", stagger + 0.18, 1.0, 1.0, 1.0),
        make_keyframe("scale", 2.0, 1.0, 1.0, 1.0),
    ]
    spawn_animators[bone["uuid"]] = {
        "name": bone["name"], "type": "bone",
        "keyframes": kfs,
    }

# Planets fly in from outer space one by one at 0.5s-1.2s staggered
for p in planet_data:
    p_idx = p["p_idx"]
    arrival_t = 0.50 + p_idx * 0.14  # 0.50, 0.64, 0.78, 0.92, 1.06
    # Orbit bone: position pre-arrival is "off-screen high Y" ; scale starts 0
    orbit_uuid = p["orbit"]["uuid"]
    sa = p["start_angle"]
    pos_kfs = [
        make_keyframe("position", 0.0, 0, 30.0, 0),
        make_keyframe("position", arrival_t - 0.20, 0, 30.0, 0),
        make_keyframe("position", arrival_t, 0, 0, 0),
        make_keyframe("position", 2.0, 0, 0, 0),
    ]
    sc_kfs = [
        make_keyframe("scale", 0.0, 0.01, 0.01, 0.01),
        make_keyframe("scale", arrival_t - 0.20, 0.01, 0.01, 0.01),
        make_keyframe("scale", arrival_t, 1.4, 1.4, 1.4),
        make_keyframe("scale", arrival_t + 0.15, 0.9, 0.9, 0.9),
        make_keyframe("scale", arrival_t + 0.25, 1.0, 1.0, 1.0),
        make_keyframe("scale", 1.78, 1.0, 1.0, 1.0),
        make_keyframe("scale", 1.84, 1.10, 1.10, 1.10),
        make_keyframe("scale", 1.92, 1.0, 1.0, 1.0),
        make_keyframe("scale", 2.0, 1.0, 1.0, 1.0),
    ]
    rot_kfs = [
        make_keyframe("rotation", 0.0, 0, sa, 0),
        make_keyframe("rotation", arrival_t, 0, sa + 30, 0),
        make_keyframe("rotation", 2.0, 0, sa + 90, 0),
    ]
    spawn_animators[orbit_uuid] = {
        "name": p["orbit"]["name"], "type": "bone",
        "keyframes": pos_kfs + sc_kfs + rot_kfs,
    }

    # Inner body — gentle wobble during spawn
    inner_uuid = p["inner"]["uuid"]
    inner_rot_kfs = [
        make_keyframe("rotation", 0.0, 0, 0, 0),
        make_keyframe("rotation", arrival_t, 30, 60, 30),
        make_keyframe("rotation", 2.0, 60, 180, 60),
    ]
    spawn_animators[inner_uuid] = {
        "name": p["inner"]["name"], "type": "bone",
        "keyframes": inner_rot_kfs,
    }

    # Ring materialises at planet arrival
    ring_uuid = p["ring"]["uuid"]
    ring_kfs = [
        make_keyframe("scale", 0.0, 0.01, 0.01, 0.01),
        make_keyframe("scale", arrival_t + 0.08, 0.01, 0.01, 0.01),
        make_keyframe("scale", arrival_t + 0.20, 1.3, 1.0, 1.3),
        make_keyframe("scale", arrival_t + 0.32, 1.0, 1.0, 1.0),
        make_keyframe("scale", 2.0, 1.0, 1.0, 1.0),
    ]
    ring_rot_kfs = [
        make_keyframe("rotation", 0.0, p["ring_tilt"], 0, 0),
        make_keyframe("rotation", 2.0, p["ring_tilt"], 60, 0),
    ]
    spawn_animators[ring_uuid] = {
        "name": p["ring"]["name"], "type": "bone",
        "keyframes": ring_kfs + ring_rot_kfs,
    }

    # Moon orbits into position
    if p["moon"] is not None:
        moon_uuid = p["moon"]["uuid"]
        moon_kfs = [
            make_keyframe("scale", 0.0, 0.01, 0.01, 0.01),
            make_keyframe("scale", arrival_t + 0.20, 0.01, 0.01, 0.01),
            make_keyframe("scale", arrival_t + 0.35, 1.2, 1.2, 1.2),
            make_keyframe("scale", arrival_t + 0.45, 1.0, 1.0, 1.0),
            make_keyframe("scale", 2.0, 1.0, 1.0, 1.0),
        ]
        moon_rot_kfs = [
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", arrival_t + 0.45, 0, 90, 0),
            make_keyframe("rotation", 2.0, 0, 240, 0),
        ]
        spawn_animators[moon_uuid] = {
            "name": p["moon"]["name"], "type": "bone",
            "keyframes": moon_kfs + moon_rot_kfs,
        }

# Stardust scatters
for idx, (bone, base_pos) in enumerate(stardust_data):
    px, ph, pz, ang, rad = base_pos
    spawn_t = 0.30 + (idx % 8) * 0.08
    out_x = math.cos(ang) * 0.3
    out_z = math.sin(ang) * 0.3
    pos_kfs = [
        make_keyframe("position", 0.0, -out_x, -ph * 0.3, -out_z),
        make_keyframe("position", spawn_t, -out_x, -ph * 0.3, -out_z),
        make_keyframe("position", spawn_t + 0.20, out_x * 0.5, 0.2, out_z * 0.5),
        make_keyframe("position", spawn_t + 0.40, 0, 0, 0),
        make_keyframe("position", 2.0, 0, 0, 0),
    ]
    sc_kfs = [
        make_keyframe("scale", 0.0, 0.01, 0.01, 0.01),
        make_keyframe("scale", spawn_t, 0.01, 0.01, 0.01),
        make_keyframe("scale", spawn_t + 0.10, 1.5, 1.5, 1.5),
        make_keyframe("scale", spawn_t + 0.25, 1.0, 1.0, 1.0),
        make_keyframe("scale", 2.0, 1.0, 1.0, 1.0),
    ]
    spawn_animators[bone["uuid"]] = {
        "name": bone["name"], "type": "bone",
        "keyframes": pos_kfs + sc_kfs,
    }

b.add_animation("spawn", length=2.0, animators=spawn_animators,
                loop="once", override=True)


# ---------- IDLE (8.0s, loop, override=false) ----------
# All orbital motion — orrery in motion. NOTHING SYNCHRONISED.
idle_animators = {}

# Sun rotates on all 3 axes (slow, organic)
sun_idle_kfs = []
samples = 40
for s in range(samples + 1):
    t = (s / samples) * 8.0
    rx = (t / 6.0) * 360.0
    ry = (t / 8.0) * 360.0
    rz = (t / 10.0) * 360.0
    sun_idle_kfs.append(make_keyframe("rotation", t, rx % 360, ry % 360, rz % 360))
# Sun also has subtle scale pulse
sun_pulse_kfs = []
for s in range(samples + 1):
    t = (s / samples) * 8.0
    val = 1.0 + math.sin((t / 3.5) * math.tau) * 0.04
    sun_pulse_kfs.append(make_keyframe("scale", t, val, val, val))
sun_idle_kfs = sun_idle_kfs + sun_pulse_kfs
idle_animators[sun_bone_uuid] = {
    "name": "sun", "type": "bone",
    "keyframes": sun_idle_kfs,
}

# Corona rays pulse scale in waves (different periods per ray)
for idx, (bone, dir_vec) in enumerate(corona_data):
    period = 1.6 + (idx % 5) * 0.22
    phase = (idx * 0.31) % 1.0
    kfs = []
    samples_c = 32
    for s in range(samples_c + 1):
        t = (s / samples_c) * 8.0
        val = math.sin(((t / period) + phase) * math.tau) * 0.18 + 1.0
        kfs.append(make_keyframe("scale", t, val, val * 1.1, val))
    # Corona ray rotation tumble
    rot_period = 4.5 + (idx % 4) * 0.6
    for s in range(samples_c + 1):
        t = (s / samples_c) * 8.0
        rx = (t / rot_period) * 360.0
        rz = (t / (rot_period * 1.3)) * 360.0
        kfs.append(make_keyframe("rotation", t, rx % 360, 0, rz % 360))
    idle_animators[bone["uuid"]] = {
        "name": bone["name"], "type": "bone",
        "keyframes": kfs,
    }

# Each planet orbits at unique speed (4-9s per orbit)
PLANET_ORBIT_PERIODS = [4.0, 5.5, 6.8, 8.0, 9.0]  # seconds per full orbit
PLANET_ORBIT_DIRECTIONS = [1, -1, 1, -1, 1]

for p in planet_data:
    p_idx = p["p_idx"]
    period = PLANET_ORBIT_PERIODS[p_idx]
    direction = PLANET_ORBIT_DIRECTIONS[p_idx]
    sa = p["start_angle"]
    orbit_uuid = p["orbit"]["uuid"]

    # Y rotation: completes (8.0/period) revolutions over 8 seconds
    revs = 8.0 / period * direction
    samples_p = 40
    rot_kfs = []
    for s in range(samples_p + 1):
        t = (s / samples_p) * 8.0
        deg = sa + (t / 8.0) * revs * 360.0
        rot_kfs.append(make_keyframe("rotation", t, 0, deg, 0))
    # subtle Y bob to make orbit feel 3D
    pos_kfs = []
    bob_period = 3.0 + p_idx * 0.7
    for s in range(samples_p + 1):
        t = (s / samples_p) * 8.0
        bob = math.sin((t / bob_period) * math.tau) * 0.5
        pos_kfs.append(make_keyframe("position", t, 0, bob, 0))
    idle_animators[orbit_uuid] = {
        "name": p["orbit"]["name"], "type": "bone",
        "keyframes": rot_kfs + pos_kfs,
    }

    # Inner body — rotates on multiple axes at unique speed
    inner_uuid = p["inner"]["uuid"]
    inner_period_x = 2.5 + p_idx * 0.4
    inner_period_y = 3.0 + p_idx * 0.5
    inner_period_z = 4.0 + p_idx * 0.3
    inner_kfs = []
    for s in range(samples_p + 1):
        t = (s / samples_p) * 8.0
        rx = (t / inner_period_x) * 360.0
        ry = (t / inner_period_y) * 360.0
        rz = (t / inner_period_z) * 360.0
        inner_kfs.append(make_keyframe("rotation", t, rx % 360, ry % 360, rz % 360))
    idle_animators[inner_uuid] = {
        "name": p["inner"]["name"], "type": "bone",
        "keyframes": inner_kfs,
    }

    # Planet ring — tilts and rotates relative to planet
    ring_uuid = p["ring"]["uuid"]
    ring_period = 3.5 + p_idx * 0.6
    ring_dir = -direction  # opposite to planet orbit
    ring_kfs = []
    for s in range(samples_p + 1):
        t = (s / samples_p) * 8.0
        deg = (t / ring_period) * 360.0 * ring_dir
        # gentle tilt wobble
        tilt = p["ring_tilt"] + math.sin((t / 4.5) * math.tau + p_idx * 0.7) * 8.0
        ring_kfs.append(make_keyframe("rotation", t, tilt, deg % 360, 0))
    idle_animators[ring_uuid] = {
        "name": p["ring"]["name"], "type": "bone",
        "keyframes": ring_kfs,
    }

    # Moon orbits planet
    if p["moon"] is not None:
        moon_uuid = p["moon"]["uuid"]
        moon_period = 1.8 + p_idx * 0.4
        moon_kfs = []
        for s in range(samples_p + 1):
            t = (s / samples_p) * 8.0
            deg = (t / moon_period) * 360.0
            moon_kfs.append(make_keyframe("rotation", t, 0, deg % 360, 0))
        idle_animators[moon_uuid] = {
            "name": p["moon"]["name"], "type": "bone",
            "keyframes": moon_kfs,
        }

# Ground rings rotate opposite to planetary orbits
for (bone, r_idx, radius) in ground_ring_data:
    period = 12.0 - r_idx * 2.0  # outer ring slower
    direction = -1 if r_idx % 2 == 0 else 1
    kfs = []
    for s in range(9):
        t = (s / 8) * 8.0
        deg = (t / period) * 360.0 * direction
        kfs.append(make_keyframe("rotation", t, 0, deg % 360, 0))
    idle_animators[bone["uuid"]] = {
        "name": bone["name"], "type": "bone",
        "keyframes": kfs,
    }

# Orbital paths — slow rotation, opposite direction to their planet
for (bone, p_idx) in orbital_path_data:
    direction = -PLANET_ORBIT_DIRECTIONS[p_idx]
    period = PLANET_ORBIT_PERIODS[p_idx] * 1.6
    kfs = []
    for s in range(9):
        t = (s / 8) * 8.0
        deg = (t / period) * 360.0 * direction
        kfs.append(make_keyframe("rotation", t, 0, deg % 360, 0))
    idle_animators[bone["uuid"]] = {
        "name": bone["name"], "type": "bone",
        "keyframes": kfs,
    }

# Stardust drifts and tumbles at unique rates
for idx, (bone, base_pos) in enumerate(stardust_data):
    px, ph, pz, ang, rad = base_pos
    period_pos = 3.0 + (idx % 7) * 0.4
    phase_pos = (idx * 0.27) % 1.0
    period_rot = 2.0 + (idx % 5) * 0.35
    phase_rot = (idx * 0.43) % 1.0
    pos_kfs = []
    rot_kfs = []
    samples_sd = 24
    for s in range(samples_sd + 1):
        t = (s / samples_sd) * 8.0
        dx = math.sin(((t / period_pos) + phase_pos) * math.tau) * 0.5
        dz = math.cos(((t / period_pos) + phase_pos * 1.3) * math.tau) * 0.5
        dy = math.sin(((t / period_pos) + phase_pos * 0.7) * math.tau) * 0.4
        pos_kfs.append(make_keyframe("position", t, dx, dy, dz))
        rx = (t / period_rot + phase_rot) * 360.0
        ry = (t / (period_rot * 0.85) + phase_rot * 0.6) * 360.0
        rz = (t / (period_rot * 1.15) + phase_rot * 1.3) * 360.0
        rot_kfs.append(make_keyframe("rotation", t, rx % 360, ry % 360, rz % 360))
    idle_animators[bone["uuid"]] = {
        "name": bone["name"], "type": "bone",
        "keyframes": pos_kfs + rot_kfs,
    }

b.add_animation("idle", length=8.0, animators=idle_animators,
                loop="loop", override=False)


# ---------- DISSIPATE (1.5s, override=true) ----------
dissipate_animators = {}

# Sun expands dramatically then collapses to 0
sun_diss_kfs = [
    make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
    make_keyframe("scale", 0.30, 1.4, 1.4, 1.4),
    make_keyframe("scale", 0.60, 2.2, 2.2, 2.2),
    make_keyframe("scale", 0.85, 2.8, 2.8, 2.8),
    make_keyframe("scale", 1.05, 1.8, 1.8, 1.8),
    make_keyframe("scale", 1.30, 0.4, 0.4, 0.4),
    make_keyframe("scale", 1.5, 0.01, 0.01, 0.01),
]
sun_diss_rot = [
    make_keyframe("rotation", 0.0, 0, 0, 0),
    make_keyframe("rotation", 0.5, 90, 180, 90),
    make_keyframe("rotation", 1.0, 270, 360, 270),
    make_keyframe("rotation", 1.5, 540, 720, 540),
]
dissipate_animators[sun_bone_uuid] = {
    "name": "sun", "type": "bone",
    "keyframes": sun_diss_kfs + sun_diss_rot,
}

# Corona rays continue outward and scale to 0
for (bone, dir_vec) in corona_data:
    dx, dy, dz = dir_vec
    pos_kfs = [
        make_keyframe("position", 0.0, 0, 0, 0),
        make_keyframe("position", 0.50, dx * 1.5, dy * 1.5, dz * 1.5),
        make_keyframe("position", 1.0, dx * 3.5, dy * 3.5, dz * 3.5),
        make_keyframe("position", 1.5, dx * 6.0, dy * 6.0, dz * 6.0),
    ]
    sc_kfs = [
        make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
        make_keyframe("scale", 0.40, 1.5, 1.5, 1.5),
        make_keyframe("scale", 0.85, 1.0, 1.0, 1.0),
        make_keyframe("scale", 1.5, 0.01, 0.01, 0.01),
    ]
    dissipate_animators[bone["uuid"]] = {
        "name": bone["name"], "type": "bone",
        "keyframes": pos_kfs + sc_kfs,
    }

# Planets fly outward tangentially from their current orbit position
for p in planet_data:
    p_idx = p["p_idx"]
    sa = p["start_angle"]
    orbit_uuid = p["orbit"]["uuid"]
    # During dissipate, orbit bone keeps rotating outward by scaling its radial out
    # We approximate "fly outward tangentially" by adding outward position to orbit
    # bone — but orbit bone has its origin at (0,height,0), planet sits at +X local.
    # Tangential to current angle = rotate Y by 90 from outward vector.
    direction = PLANET_ORBIT_DIRECTIONS[p_idx]
    diss_t = p_idx * 0.10  # staggered
    # We add outward Y rotation continuing planet orbit, plus expanding scale
    rot_kfs = [
        make_keyframe("rotation", 0.0, 0, sa, 0),
        make_keyframe("rotation", 0.5, 0, sa + direction * 60, 0),
        make_keyframe("rotation", 1.5, 0, sa + direction * 200, 0),
    ]
    sc_kfs = [
        make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
        make_keyframe("scale", diss_t + 0.20, 1.2, 1.2, 1.2),
        make_keyframe("scale", diss_t + 0.50, 1.5, 1.5, 1.5),  # expand
        make_keyframe("scale", diss_t + 0.80, 1.7, 1.7, 1.7),
        make_keyframe("scale", 1.5, 0.01, 0.01, 0.01),
    ]
    # Planet flies upward + outward as orbit expands
    pos_kfs = [
        make_keyframe("position", 0.0, 0, 0, 0),
        make_keyframe("position", 0.60, 0, 1.5 * direction, 0),
        make_keyframe("position", 1.5, 0, 6.0 * direction, 0),
    ]
    dissipate_animators[orbit_uuid] = {
        "name": p["orbit"]["name"], "type": "bone",
        "keyframes": rot_kfs + sc_kfs + pos_kfs,
    }

    # Inner body keeps spinning faster
    inner_uuid = p["inner"]["uuid"]
    inner_kfs = [
        make_keyframe("rotation", 0.0, 0, 0, 0),
        make_keyframe("rotation", 0.5, 180, 240, 120),
        make_keyframe("rotation", 1.0, 540, 600, 360),
        make_keyframe("rotation", 1.5, 1080, 1200, 720),
    ]
    dissipate_animators[inner_uuid] = {
        "name": p["inner"]["name"], "type": "bone",
        "keyframes": inner_kfs,
    }

    # Ring expands dramatically
    ring_uuid = p["ring"]["uuid"]
    ring_kfs = [
        make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
        make_keyframe("scale", 0.50, 1.6, 1.0, 1.6),
        make_keyframe("scale", 1.0, 2.4, 0.5, 2.4),
        make_keyframe("scale", 1.5, 0.01, 0.01, 0.01),
    ]
    ring_rot_kfs = [
        make_keyframe("rotation", 0.0, p["ring_tilt"], 0, 0),
        make_keyframe("rotation", 1.5, p["ring_tilt"] + 180, 540, 0),
    ]
    dissipate_animators[ring_uuid] = {
        "name": p["ring"]["name"], "type": "bone",
        "keyframes": ring_kfs + ring_rot_kfs,
    }

    # Moon follows planet
    if p["moon"] is not None:
        moon_uuid = p["moon"]["uuid"]
        moon_kfs = [
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 0.7, 0, 360, 0),
            make_keyframe("rotation", 1.5, 0, 1080, 0),
        ]
        moon_sc_kfs = [
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
            make_keyframe("scale", 0.8, 1.4, 1.4, 1.4),
            make_keyframe("scale", 1.5, 0.01, 0.01, 0.01),
        ]
        dissipate_animators[moon_uuid] = {
            "name": p["moon"]["name"], "type": "bone",
            "keyframes": moon_kfs + moon_sc_kfs,
        }

# Orbital paths expand and snap
for (bone, p_idx) in orbital_path_data:
    stagger = p_idx * 0.06
    kfs = [
        make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
        make_keyframe("scale", stagger + 0.40, 1.5, 1.0, 1.5),
        make_keyframe("scale", stagger + 0.65, 2.0, 0.4, 2.0),
        make_keyframe("scale", stagger + 0.85, 0.05, 0.05, 0.05),
        make_keyframe("scale", 1.5, 0.01, 0.01, 0.01),
    ]
    dissipate_animators[bone["uuid"]] = {
        "name": bone["name"], "type": "bone",
        "keyframes": kfs,
    }

# Ground rings rapidly expand and snap
for (bone, r_idx, radius) in ground_ring_data:
    stagger = r_idx * 0.05
    kfs = [
        make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
        make_keyframe("scale", stagger + 0.20, 1.4, 1.0, 1.4),
        make_keyframe("scale", stagger + 0.40, 1.8, 0.5, 1.8),
        make_keyframe("scale", stagger + 0.60, 0.05, 0.05, 0.05),
        make_keyframe("scale", 1.5, 0.01, 0.01, 0.01),
    ]
    rot_kfs = [
        make_keyframe("rotation", 0.0, 0, 0, 0),
        make_keyframe("rotation", 1.5, 0, 540 * (1 if r_idx % 2 == 0 else -1), 0),
    ]
    dissipate_animators[bone["uuid"]] = {
        "name": bone["name"], "type": "bone",
        "keyframes": kfs + rot_kfs,
    }

# Stardust scatters violently outward
for idx, (bone, base_pos) in enumerate(stardust_data):
    px, ph, pz, ang, rad = base_pos
    out_x = math.cos(ang) * 4.0
    out_z = math.sin(ang) * 4.0
    out_y = 3.0 + (idx % 4) * 0.5
    pos_kfs = [
        make_keyframe("position", 0.0, 0, 0, 0),
        make_keyframe("position", 0.40, out_x * 0.4, out_y * 0.4, out_z * 0.4),
        make_keyframe("position", 0.90, out_x * 0.8, out_y * 0.8, out_z * 0.8),
        make_keyframe("position", 1.5, out_x, out_y, out_z),
    ]
    sc_kfs = [
        make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
        make_keyframe("scale", 0.40, 1.5, 1.5, 1.5),
        make_keyframe("scale", 0.90, 1.0, 1.0, 1.0),
        make_keyframe("scale", 1.5, 0.01, 0.01, 0.01),
    ]
    rot_kfs = [
        make_keyframe("rotation", 0.0, 0, 0, 0),
        make_keyframe("rotation", 1.5, 720, 540, 360),
    ]
    dissipate_animators[bone["uuid"]] = {
        "name": bone["name"], "type": "bone",
        "keyframes": pos_kfs + sc_kfs + rot_kfs,
    }

b.add_animation("dissipate", length=1.5, animators=dissipate_animators,
                loop="once", override=True)


# =============================================================================
# WRITE
# =============================================================================
b.add_texture("candy_planet", build_candy_planet_texture())
b.add_texture("starfield", build_starfield_texture())

OUT = "D:/CC/ChaosCraft/src/main/resources/models/fluffy/me_attacks/candy_planet_sugar_rush.bbmodel"
path = b.write(OUT)
size_kb = Path(path).stat().st_size // 1024
print(f"Wrote: {path}")
print(f"Size: {size_kb} KB")
print(f"Elements: {len(b.elements)}")
print(f"Animations: {[a['name'] for a in b.animations]}")
print(f"Bones (top-level under root): {len(master_children)}")
