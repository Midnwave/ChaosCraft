#!/usr/bin/env python3
"""Generator for avalanche_orrery.bbmodel — FreezingIce attack #21.

Visual intent: a miniature solar system made entirely of ice. Central ice sun
(silver-white) with corona spikes, 4 ice planets at different heights and radii
each with their own ring of small ice chunks, 2 planets with moons. Orbital
paths visualised as flat rings on the ground. Horror is mathematical.
"""

import sys
import math
import random

sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder,
    make_uuid,
    make_keyframe,
    basic_cube_faces,
    uniform_face,
    png_from_pixels,
    hex_to_rgba,
    color_lerp,
    fill_rect,
)
from _freezingice_helpers import (
    base_fill,
    annual_bands,
    diagonal_facet_lines,
    specular_cluster,
    jitter_inplace,
)

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/freezingice/me_attacks/avalanche_orrery.bbmodel"


# ---------------------------------------------------------------------------
# TEXTURES (128x128 for filesize bulk)
# ---------------------------------------------------------------------------

def build_planets_texture():
    """Tex 0: ICE PLANET TECHNIQUE — 5 horizontal zones for 4 planets + sun."""
    w = h = 128
    # Spec colors (rescaled from 64 -> 128 row counts):
    # Sun zone rows 0-24 (was 0-12)
    # P1 zone rows 25-48 (was 13-24)
    # P2 zone rows 49-72 (was 25-36)
    # P3 zone rows 73-96 (was 37-48)
    # P4 zone rows 97-128 (was 49-64)
    sun       = hex_to_rgba("#e0f0ff")
    sun_dark  = hex_to_rgba("#a8c8e0")
    p1_main   = hex_to_rgba("#082040")  # deep navy
    p1_high   = hex_to_rgba("#204878")
    p2_main   = hex_to_rgba("#204878")  # glacier blue
    p2_high   = hex_to_rgba("#5090c0")
    p3_main   = hex_to_rgba("#5090c0")  # clear ice
    p3_high   = hex_to_rgba("#a0d0f0")
    p4_main   = hex_to_rgba("#a0d0f0")  # pale frost
    p4_high   = hex_to_rgba("#e0f0ff")
    spec      = hex_to_rgba("#ffffff")

    pixels = base_fill(w, h, sun)

    # SUN ZONE (rows 0-24): radial gradient + soft halo
    for y in range(0, 25):
        for x in range(w):
            d = math.sqrt((x - 64) ** 2 + (y - 12) ** 2)
            t = min(1.0, d / 50.0)
            pixels[y * w + x] = color_lerp(sun, sun_dark, t * 0.6)

    # P1 ZONE (rows 25-48): deep navy — crystalline cracks pattern
    for y in range(25, 49):
        for x in range(w):
            pixels[y * w + x] = p1_main
    diagonal_facet_lines(pixels, w, 49, p1_high, count=8, seed=11)
    # crystalline pock pattern in P1 zone
    rng1 = random.Random(331)
    for _ in range(40):
        cx = rng1.randrange(w)
        cy = rng1.randrange(25, 49)
        radius = rng1.randint(1, 3)
        specular_cluster(pixels, w, h, cx, cy, radius, p1_high, strength=0.6)

    # P2 ZONE (rows 49-72): glacier blue — annual band strata
    for y in range(49, 73):
        for x in range(w):
            pixels[y * w + x] = p2_main
    # local annual bands inside zone
    for y in range(49, 73, 4):
        for x in range(w):
            p = pixels[y * w + x]
            pixels[y * w + x] = color_lerp(p, p2_high, 0.45)

    # P3 ZONE (rows 73-96): clear ice — frost web micro pattern
    for y in range(73, 97):
        for x in range(w):
            pixels[y * w + x] = p3_main
    rng3 = random.Random(553)
    # mini frost crystals
    for _ in range(35):
        cx = rng3.randrange(w)
        cy = rng3.randrange(73, 97)
        length = rng3.randint(4, 8)
        ang = rng3.random() * 2 * math.pi
        for r in range(length):
            xx = int(cx + math.cos(ang) * r)
            yy = int(cy + math.sin(ang) * r)
            if 0 <= xx < w and 73 <= yy < 97:
                p = pixels[yy * w + xx]
                pixels[yy * w + xx] = color_lerp(p, p3_high, 0.75)

    # P4 ZONE (rows 97-128): pale frost — smooth sky gradient
    for y in range(97, 128):
        for x in range(w):
            t = (y - 97) / 31.0
            pixels[y * w + x] = color_lerp(p4_main, p4_high, t * 0.65)
    # Add subtle horizontal frost streaks
    for y in range(97, 128, 3):
        for x in range(0, w, 5):
            for k in range(3):
                if x + k < w:
                    p = pixels[y * w + x + k]
                    pixels[y * w + x + k] = color_lerp(p, p4_high, 0.55)

    # Specular highlights per zone
    specular_cluster(pixels, w, h, 26, 8, 5, spec, strength=0.8)         # Sun
    specular_cluster(pixels, w, h, 100, 32, 3, p1_high, strength=0.7)    # P1
    specular_cluster(pixels, w, h, 22, 56, 3, p2_high, strength=0.7)     # P2
    specular_cluster(pixels, w, h, 92, 82, 3, p3_high, strength=0.85)    # P3
    specular_cluster(pixels, w, h, 60, 110, 4, spec, strength=0.85)      # P4

    # Per-pixel jitter for filesize bulk
    jitter_inplace(pixels, jitter_range=20, seed=909, density=1.0)
    jitter_inplace(pixels, jitter_range=12, seed=1313, density=0.45)

    return png_from_pixels(pixels, w, h)


def build_orbit_texture():
    """Tex 1: orbital paths / rings / ground — near-black void w/ ring lines."""
    w = h = 128
    void  = hex_to_rgba("#020408")
    ring  = hex_to_rgba("#70b0d0")
    ring_hi = hex_to_rgba("#c8e8ff")
    void_hi = hex_to_rgba("#0c1828")

    pixels = base_fill(w, h, void)

    # Two ring bands at 55% and 65% radius from centre (64,64)
    cx, cy = 64, 64
    for r_pct in (0.55, 0.65):
        r_target = 64 * r_pct
        for y in range(h):
            for x in range(w):
                d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
                if abs(d - r_target) < 1.2:
                    p = pixels[y * w + x]
                    pixels[y * w + x] = color_lerp(p, ring, 0.85)
                elif abs(d - r_target) < 2.4:
                    p = pixels[y * w + x]
                    pixels[y * w + x] = color_lerp(p, ring, 0.4)

    # Sparse star noise — stars in cold space
    rngs = random.Random(202)
    for _ in range(160):
        x = rngs.randrange(w)
        y = rngs.randrange(h)
        p = pixels[y * w + x]
        pixels[y * w + x] = color_lerp(p, ring_hi, rngs.random() * 0.6 + 0.2)

    # subtle void variations
    rngd = random.Random(45)
    for _ in range(400):
        x = rngd.randrange(w)
        y = rngd.randrange(h)
        p = pixels[y * w + x]
        pixels[y * w + x] = color_lerp(p, void_hi, 0.35)

    jitter_inplace(pixels, jitter_range=14, seed=707, density=1.0)
    jitter_inplace(pixels, jitter_range=8, seed=909, density=0.5)
    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# MODEL
# ---------------------------------------------------------------------------

def build():
    b = Builder("avalanche_orrery", resolution=(128, 128), visible_box=(16, 18, 0))

    b.add_texture("orrery_planets", build_planets_texture())
    b.add_texture("orrery_orbits", build_orbit_texture())

    # Face helpers (planets tex uses 128x128, choose zone by row range)
    def tex0_face_sun(): return uniform_face(0, 0, 128, 24, tex_index=0)
    def tex0_face_p1():  return uniform_face(0, 25, 128, 48, tex_index=0)
    def tex0_face_p2():  return uniform_face(0, 49, 128, 72, tex_index=0)
    def tex0_face_p3():  return uniform_face(0, 73, 128, 96, tex_index=0)
    def tex0_face_p4():  return uniform_face(0, 97, 128, 128, tex_index=0)
    def tex1_face():     return uniform_face(0, 0, 128, 128, tex_index=1)

    # =====================================================================
    # 1) Central ice sun — 5 overlapping cubes approximating sphere at Y=10
    # =====================================================================
    sun_cubes = []
    sun_offsets = [
        ( 0, 0,  0, 1.6),
        ( 0.9, 0.4,  0.9, 1.2),
        (-0.9, 0.4, -0.9, 1.2),
        ( 0.9, -0.4, -0.9, 1.2),
        (-0.9, -0.4,  0.9, 1.2),
    ]
    for i, (ox, oy, oz, sz) in enumerate(sun_offsets):
        sun_cubes.append(b.add_cube(
            f"sun_core_{i+1}",
            from_=[ox - sz, 10 + oy - sz, oz - sz],
            to_=[ox + sz, 10 + oy + sz, oz + sz],
            faces=tex0_face_sun(),
        ))
    sun_group_uuid = make_uuid()
    sun_group = b.make_group("sun_core", [0, 10, 0], sun_cubes, group_uuid=sun_group_uuid)

    # 2) Corona spikes — 8 tapered spikes radiating from sun
    corona_groups = []
    for i in range(8):
        ang = (i / 8.0) * 2 * math.pi
        # 2 cubes per spike: thick base + thin tip
        cubes = []
        cubes.append(b.add_cube(
            f"corona_base_{i+1}",
            from_=[-0.5, 9.5, -0.5], to_=[0.5, 10.5, 0.5],
            faces=tex0_face_sun(),
        ))
        cubes.append(b.add_cube(
            f"corona_tip_{i+1}",
            from_=[-0.25, 10, 1.0], to_=[0.25, 10.4, 2.6],
            faces=tex0_face_sun(),
        ))
        gu = make_uuid()
        g = b.make_group(
            f"corona_{i+1}", [0, 10, 0], cubes,
            rotation=[0, math.degrees(ang), 0], group_uuid=gu,
        )
        corona_groups.append((gu, g))

    # =====================================================================
    # 3) 4 ice planets at radii 5, 7, 9, 12 and heights Y=7, 10, 13, 8
    # Each planet: 3 overlapping cubes + ring of 6 slabs + optional moon
    # =====================================================================
    planet_data = [
        (5,  7,  tex0_face_p1, 0.0,    "planet1", True),
        (7,  10, tex0_face_p2, 1.2,    "planet2", False),
        (9,  13, tex0_face_p3, 2.5,    "planet3", True),
        (12, 8,  tex0_face_p4, 4.0,    "planet4", False),
    ]
    planet_orbit_groups = []  # each entry: (orbit_uuid, orbit_group, planet_inner_uuid, ring_uuids, moon_uuids)
    for (radius, y_height, face_fn, start_ang, name, has_moon) in planet_data:
        # Planet body — 3 overlapping cubes
        body_cubes = []
        sizes = [1.2, 0.9, 0.9]
        offsets = [(0, 0, 0), (0.6, 0.4, 0.0), (-0.6, -0.3, 0.4)]
        for i, ((ox, oy, oz), sz) in enumerate(zip(offsets, sizes)):
            body_cubes.append(b.add_cube(
                f"{name}_body_{i+1}",
                from_=[ox - sz, oy - sz, oz - sz],
                to_=[ox + sz, oy + sz, oz + sz],
                faces=face_fn(),
            ))

        # Planet's ring — 6 flat slabs in a circle around planet
        ring_cubes = []
        for i in range(6):
            ang = (i / 6.0) * 2 * math.pi
            rx = math.cos(ang) * 2.4
            rz = math.sin(ang) * 2.4
            slab_uuid = b.add_cube(
                f"{name}_ring_{i+1}",
                from_=[rx - 0.4, -0.1, rz - 0.15],
                to_=[rx + 0.4, 0.1, rz + 0.15],
                faces=tex1_face(),
                rotation=[0, math.degrees(ang), 0],
            )
            ring_cubes.append(slab_uuid)

        # Moon (if applicable) — small 2-cube sphere offset from planet
        moon_uuids = []
        if has_moon:
            for i in range(2):
                mx_off = 3.5 + (0.3 if i else 0)
                moon_uuids.append(b.add_cube(
                    f"{name}_moon_{i+1}",
                    from_=[mx_off - 0.4, -0.4 + i * 0.2, -0.4 + i * 0.1],
                    to_=[mx_off + 0.4, 0.4 + i * 0.2, 0.4 + i * 0.1],
                    faces=face_fn(),
                ))

        # Inner planet group (the planet body + its ring + moon — orbits with planet)
        inner_children = list(body_cubes) + list(ring_cubes) + list(moon_uuids)
        inner_uuid = make_uuid()
        inner_group = b.make_group(f"{name}_inner", [radius, y_height, 0], inner_children,
                                    group_uuid=inner_uuid)

        # Orbit group — at origin, rotates Y to orbit. Contains inner_group.
        orbit_uuid = make_uuid()
        orbit_group = b.make_group(f"{name}_orbit", [0, y_height, 0], [inner_group],
                                    rotation=[0, math.degrees(start_ang), 0],
                                    group_uuid=orbit_uuid)
        planet_orbit_groups.append((orbit_uuid, orbit_group, inner_uuid, inner_group, name, y_height, radius))

    # =====================================================================
    # 4) Orbital paths — 4 flat rings at orbit heights, 8 slabs each
    # =====================================================================
    orbital_path_groups = []
    orbit_radii = [5, 7, 9, 12]
    orbit_heights = [7, 10, 13, 8]
    for path_idx, (r, h_y) in enumerate(zip(orbit_radii, orbit_heights)):
        slab_uuids = []
        for i in range(8):
            ang = (i / 8.0) * 2 * math.pi
            cx = math.cos(ang) * r
            cz = math.sin(ang) * r
            slab_uuids.append(b.add_cube(
                f"orbital_path_{path_idx+1}_slab_{i+1}",
                from_=[cx - 0.6, h_y - 0.08, cz - 0.25],
                to_=[cx + 0.6, h_y + 0.08, cz + 0.25],
                faces=tex1_face(),
                rotation=[0, math.degrees(ang), 0],
            ))
        gu = make_uuid()
        g = b.make_group(f"orbital_path_{path_idx+1}", [0, h_y, 0],
                         slab_uuids, group_uuid=gu)
        orbital_path_groups.append((gu, g))

    # =====================================================================
    # 5) Ground manifestation — 3 concentric rings at Y=0
    # =====================================================================
    ground_groups = []
    ground_radii = [3, 6, 9]
    for gi, r in enumerate(ground_radii):
        slab_uuids = []
        seg_count = 12
        for i in range(seg_count):
            ang = (i / seg_count) * 2 * math.pi
            cx = math.cos(ang) * r
            cz = math.sin(ang) * r
            slab_uuids.append(b.add_cube(
                f"ground_ring_{gi+1}_slab_{i+1}",
                from_=[cx - 0.5, -0.1, cz - 0.18],
                to_=[cx + 0.5, 0.1, cz + 0.18],
                faces=tex1_face(),
                rotation=[0, math.degrees(ang), 0],
            ))
        gu = make_uuid()
        g = b.make_group(f"ground_ring_{gi+1}", [0, 0, 0], slab_uuids, group_uuid=gu)
        ground_groups.append((gu, g))

    # =====================================================================
    # ROOT
    # =====================================================================
    root_children = [sun_group]
    for (_uu, g) in corona_groups: root_children.append(g)
    for (ou, og, iu, ig, name, hy, rr) in planet_orbit_groups: root_children.append(og)
    for (_uu, g) in orbital_path_groups: root_children.append(g)
    for (_uu, g) in ground_groups: root_children.append(g)

    root_uuid = make_uuid()
    root_group = b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid)
    b.outliner.append(root_group)

    # =====================================================================
    # ANIMATIONS
    # =====================================================================

    # ----- SPAWN: 1.5s -----
    spawn = {}
    # Sun materialises at 0.3s — scale 0→1
    spawn[sun_group_uuid] = {"name": "sun_core", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 0, 0, 0),
        make_keyframe("scale", 0.3, 0, 0, 0),
        make_keyframe("scale", 0.55, 1.2, 1.2, 1.2),
        make_keyframe("scale", 0.7, 1.0, 1.0, 1.0),
        make_keyframe("scale", 1.5, 1.0, 1.0, 1.0),
    ]}
    # Corona spikes deploy at 0.5s
    for i, (gu, g) in enumerate(corona_groups):
        delay = 0.5 + i * 0.02
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.15, 1.2, 1.2, 1.2),
            make_keyframe("scale", delay + 0.3, 1.0, 1.0, 1.0),
            make_keyframe("scale", 1.5, 1.0, 1.0, 1.0),
        ]}
    # Orbital paths expand at 0.3-0.6s
    for pi, (gu, g) in enumerate(orbital_path_groups):
        delay = 0.3 + pi * 0.08
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 0, 1, 0),
            make_keyframe("scale", delay, 0, 1, 0),
            make_keyframe("scale", delay + 0.25, 1.0, 1.0, 1.0),
            make_keyframe("scale", 1.5, 1.0, 1.0, 1.0),
        ]}
    # Planets fly in staggered at 0.5-1.0s — orbit group rotates while scaling in
    for pi, (ou, og, iu, ig, name, hy, rr) in enumerate(planet_orbit_groups):
        delay = 0.5 + pi * 0.12
        spawn[iu] = {"name": ig["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.18, 1.3, 1.3, 1.3),
            make_keyframe("scale", delay + 0.35, 1.0, 1.0, 1.0),
            make_keyframe("scale", 1.5, 1.0, 1.0, 1.0),
        ]}
    # Ground rings expand at 0.2s
    for gi, (gu, g) in enumerate(ground_groups):
        delay = 0.2 + gi * 0.05
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 0, 1, 0),
            make_keyframe("scale", delay, 0, 1, 0),
            make_keyframe("scale", delay + 0.3, 1.0, 1.0, 1.0),
            make_keyframe("scale", 1.5, 1.0, 1.0, 1.0),
        ]}
    b.add_animation("spawn", length=1.5, animators=spawn, loop="once", override=True)

    # ----- IDLE: 8.0s loop -----
    idle = {}
    # Sun rotates on all 3 axes — dense keyframes
    sun_kfs = []
    steps = 32
    for k in range(steps + 1):
        t = (k / steps) * 8.0
        rx = (t / 8.0) * 360
        ry = (t / 8.0) * 720
        rz = (t / 8.0) * 180
        sun_kfs.append(make_keyframe("rotation", t, rx, ry, rz))
    # Pulse scale
    for k in range(steps + 1):
        t = (k / steps) * 8.0
        s = 1.0 + 0.04 * math.sin(t * math.pi)
        sun_kfs.append(make_keyframe("scale", t, s, s, s))
    idle[sun_group_uuid] = {"name": "sun_core", "type": "bone", "keyframes": sun_kfs}

    # Corona spikes pulse — staggered
    for i, (gu, g) in enumerate(corona_groups):
        kfs = []
        phase = i * 0.3
        for k in range(20):
            t = (k / 19) * 8.0
            ph = (t / 1.5) * 2 * math.pi + phase
            s = 1.0 + 0.15 * (1.0 - math.cos(ph))
            kfs.append(make_keyframe("scale", t, s, s, s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Each planet orbits at unique speed (4-9s) — dense Y rotation keyframes
    planet_orbit_speeds = [4.0, 6.0, 7.5, 9.0]
    for pi, ((ou, og, iu, ig, name, hy, rr), period) in enumerate(zip(planet_orbit_groups, planet_orbit_speeds)):
        kfs = []
        for k in range(24):
            t = (k / 23) * 8.0
            ang_deg = (t / period) * 360 + pi * 30
            kfs.append(make_keyframe("rotation", t, 0, ang_deg, 0))
        idle[ou] = {"name": og["name"], "type": "bone", "keyframes": kfs}
        # Planet ring/body tilts subtly on its own
        inner_kfs = []
        for k in range(16):
            t = (k / 15) * 8.0
            rxd = 12 * math.sin(t * 2 * math.pi / (period * 0.6))
            rzd = 8 * math.cos(t * 2 * math.pi / (period * 0.7))
            ryd = (t / (period * 0.5)) * 360 + pi * 45  # self-rotation
            inner_kfs.append(make_keyframe("rotation", t, rxd, ryd, rzd))
        idle[iu] = {"name": ig["name"], "type": "bone", "keyframes": inner_kfs}

    # Ground rings counter-rotate
    for gi, (gu, g) in enumerate(ground_groups):
        kfs = []
        period = 6.0 + gi * 1.5
        direction = -1 if gi % 2 == 0 else 1
        for k in range(18):
            t = (k / 17) * 8.0
            ang_deg = direction * (t / period) * 360
            kfs.append(make_keyframe("rotation", t, 0, ang_deg, 0))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Orbital paths shimmer (subtle scale pulse)
    for pi, (gu, g) in enumerate(orbital_path_groups):
        kfs = []
        for k in range(16):
            t = (k / 15) * 8.0
            s = 1.0 + 0.025 * math.sin(t * 2 * math.pi / 4.0 + pi)
            kfs.append(make_keyframe("scale", t, s, 1.0, s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("idle", length=8.0, animators=idle, loop="loop", override=False)

    # ----- DISSIPATE: 1.2s -----
    diss = {}
    # Planets fly outward tangentially
    for pi, (ou, og, iu, ig, name, hy, rr) in enumerate(planet_orbit_groups):
        diss[iu] = {"name": ig["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, rr, 0, 0),
            make_keyframe("position", 0.6, rr * 1.4, 1.0, rr * 0.6),
            make_keyframe("position", 1.2, rr * 2.0, -3.0, rr * 1.5),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.8, 0.7, 0.7, 0.7),
            make_keyframe("scale", 1.2, 0.0, 0.0, 0.0),
        ]}
    # Sun expands then collapses
    diss[sun_group_uuid] = {"name": "sun_core", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 1, 1, 1),
        make_keyframe("scale", 0.5, 2.2, 2.2, 2.2),
        make_keyframe("scale", 0.9, 0.5, 0.5, 0.5),
        make_keyframe("scale", 1.2, 0.0, 0.0, 0.0),
    ]}
    # Corona spikes scatter
    for i, (gu, g) in enumerate(corona_groups):
        ang = (i / 8.0) * 2 * math.pi
        out_x = math.cos(ang) * 6
        out_z = math.sin(ang) * 6
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 1.2, out_x, 2, out_z),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 1.2, 0.0, 0.0, 0.0),
        ]}
    # Ground rings expand and snap
    for gi, (gu, g) in enumerate(ground_groups):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.6, 1.5, 1, 1.5),
            make_keyframe("scale", 1.2, 0.0, 0.0, 0.0),
        ]}
    # Orbital paths fade
    for pi, (gu, g) in enumerate(orbital_path_groups):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.5, 1.2, 1, 1.2),
            make_keyframe("scale", 1.2, 0.0, 0.0, 0.0),
        ]}
    b.add_animation("dissipate", length=1.2, animators=diss, loop="once", override=True)

    b.write(OUTPUT_PATH)
    print(f"Wrote {OUTPUT_PATH}")


if __name__ == "__main__":
    build()
