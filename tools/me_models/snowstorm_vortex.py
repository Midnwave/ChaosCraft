#!/usr/bin/env python3
"""Generator for snowstorm_vortex.bbmodel — FreezingIce attack #23.

Visual intent: contained blizzard vortex — spinning column of compressed snow,
calm dark centre (the eye), violently spinning walls. Rings increase in radius
top-to-bottom (3, 4, 5, 7, 9). Eye column NEVER ROTATES — everything else does.
"""

import sys
import math
import random

sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder,
    make_uuid,
    make_keyframe,
    uniform_face,
    png_from_pixels,
    hex_to_rgba,
    color_lerp,
)
from _freezingice_helpers import (
    base_fill,
    specular_cluster,
    jitter_inplace,
)

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/freezingice/me_attacks/snowstorm_vortex.bbmodel"


# ---------------------------------------------------------------------------
# TEXTURES
# ---------------------------------------------------------------------------

def build_vortex_wall_texture():
    """Tex 0: SPINNING VORTEX WALL — diagonal streaks ~70° from horizontal."""
    w = h = 128
    navy   = hex_to_rgba("#040c1a")
    streak = hex_to_rgba("#70b0e0")
    hi     = hex_to_rgba("#90d0ff")
    spec   = hex_to_rgba("#ffffff")
    void_hi= hex_to_rgba("#0c1830")

    pixels = base_fill(w, h, navy)

    # DIAGONAL STREAKS at ~70° from horizontal (slope = tan(70°) ≈ 2.75)
    # This means y advances ~2.75 per x advance — steep diagonals
    rng = random.Random(404)
    SLOPE = 2.75
    NUM_STREAKS = 28
    # Different streak intensities for layering
    streak_offsets = [rng.randrange(-200, 200) for _ in range(NUM_STREAKS)]
    streak_strengths = [0.4 + rng.random() * 0.5 for _ in range(NUM_STREAKS)]
    streak_widths = [rng.randint(2, 4) for _ in range(NUM_STREAKS)]
    for idx, offset in enumerate(streak_offsets):
        strength = streak_strengths[idx]
        sw = streak_widths[idx]
        for x in range(w):
            y_center = int(SLOPE * x + offset)
            for dy in range(-sw, sw + 1):
                y = y_center + dy
                if 0 <= y < h:
                    d_norm = abs(dy) / float(sw + 1)
                    p = pixels[y * w + x]
                    pixels[y * w + x] = color_lerp(p, streak, strength * (1.0 - d_norm))

    # Brighter highlight streaks (fewer, brighter)
    for _ in range(6):
        offset = rng.randrange(-200, 200)
        for x in range(w):
            y_center = int(SLOPE * x + offset)
            for dy in range(-2, 3):
                y = y_center + dy
                if 0 <= y < h:
                    d_norm = abs(dy) / 2.5
                    p = pixels[y * w + x]
                    pixels[y * w + x] = color_lerp(p, hi, 0.85 * (1.0 - d_norm))

    # Sparse pure-white speculars (snowflake highlights)
    for _ in range(30):
        cx = rng.randrange(w)
        cy = rng.randrange(h)
        specular_cluster(pixels, w, h, cx, cy, 2, spec, strength=0.85)

    # Dark void filler
    rngd = random.Random(2424)
    for _ in range(500):
        x = rngd.randrange(w)
        y = rngd.randrange(h)
        if pixels[y * w + x] == navy:
            p = pixels[y * w + x]
            pixels[y * w + x] = color_lerp(p, void_hi, rngd.random() * 0.5)

    jitter_inplace(pixels, jitter_range=18, seed=8181, density=1.0)
    jitter_inplace(pixels, jitter_range=12, seed=9090, density=0.5)
    return png_from_pixels(pixels, w, h)


def build_eye_texture():
    """Tex 1: near-pure black eye + base funnel pattern with one ring."""
    w = h = 128
    black = hex_to_rgba("#020408")
    dark_hi = hex_to_rgba("#0a1018")
    bright_ring = hex_to_rgba("#90d0ff")

    pixels = base_fill(w, h, black)

    # Tight dark radial — almost imperceptible
    cx, cy = 64, 64
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            t = min(1.0, d / 80.0)
            pixels[y * w + x] = color_lerp(dark_hi, black, t)

    # Single bright ring at 20% radius (~12-14 px)
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            if abs(d - 12.8) < 1.0:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, bright_ring, 0.85)
            elif abs(d - 12.8) < 2.0:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, bright_ring, 0.4)

    # Tiny sparse stars
    rng = random.Random(101)
    for _ in range(60):
        x = rng.randrange(w)
        y = rng.randrange(h)
        p = pixels[y * w + x]
        pixels[y * w + x] = color_lerp(p, bright_ring, rng.random() * 0.4)

    jitter_inplace(pixels, jitter_range=10, seed=2020, density=1.0)
    jitter_inplace(pixels, jitter_range=6, seed=3030, density=0.5)
    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# MODEL
# ---------------------------------------------------------------------------

def build():
    b = Builder("snowstorm_vortex", resolution=(128, 128), visible_box=(20, 20, 0))
    b.add_texture("vortex_wall", build_vortex_wall_texture())
    b.add_texture("vortex_eye", build_eye_texture())

    def tex0(): return uniform_face(0, 0, 128, 128, tex_index=0)
    def tex1(): return uniform_face(0, 0, 128, 128, tex_index=1)

    # =====================================================================
    # 1) Vortex wall — 5 horizontal disc rings at Y=1,3,6,10,14
    #    Each ring 12 upright flat slabs leaned 22.5° outward
    #    Radii: 3, 4, 5, 7, 9
    # =====================================================================
    ring_heights = [1, 3, 6, 10, 14]
    ring_radii   = [3, 4, 5,  7,  9]
    ring_groups = []
    NUM_SLABS = 12
    for ri, (ry, rr) in enumerate(zip(ring_heights, ring_radii)):
        slabs = []
        for i in range(NUM_SLABS):
            ang = (i / NUM_SLABS) * 2 * math.pi
            cx = math.cos(ang) * rr
            cz = math.sin(ang) * rr
            # Slab dimensions — upright flat (thin Y, mid X, tall along radial)
            # Cube local: extends outward from origin slightly
            slab_uuid = b.add_cube(
                f"ring{ri+1}_slab_{i+1}",
                from_=[cx - 0.5, ry - 1.2, cz - 0.12],
                to_=[cx + 0.5, ry + 1.2, cz + 0.12],
                faces=tex0(),
                # Lean outward 22.5° by rotating around tangent axis
                rotation=[22.5 * math.sin(ang), math.degrees(ang), 22.5 * math.cos(ang)],
            )
            slabs.append(slab_uuid)
        gu = make_uuid()
        g = b.make_group(f"vortex_ring_{ri+1}", [0, ry, 0], slabs, group_uuid=gu)
        ring_groups.append((gu, g, rr))

    # =====================================================================
    # 2) Eye centre column — 3 thin cubes stacked from Y=0 to Y=14
    # =====================================================================
    eye_cubes = []
    eye_segments = [(0, 5), (5, 10), (10, 14)]
    for i, (y0, y1) in enumerate(eye_segments):
        eye_cubes.append(b.add_cube(
            f"eye_segment_{i+1}",
            from_=[-0.55, y0, -0.55], to_=[0.55, y1, 0.55],
            faces=tex1(),
        ))
    eye_uuid = make_uuid()
    eye_group = b.make_group("eye_column", [0, 7, 0], eye_cubes, group_uuid=eye_uuid)

    # =====================================================================
    # 3) Blizzard streaks — 16 thin diagonal flat slabs around vortex wall mid-height
    # =====================================================================
    streak_groups = []
    NUM_STREAKS = 16
    for i in range(NUM_STREAKS):
        ang = (i / NUM_STREAKS) * 2 * math.pi
        h_y = 5 + (i % 3) * 2  # vary heights between rings
        rr = 5 + (i % 4) * 0.4
        cx = math.cos(ang) * rr
        cz = math.sin(ang) * rr
        slab_uuid = b.add_cube(
            f"blizzard_streak_{i+1}",
            from_=[-1.5, -0.08, -0.06], to_=[1.5, 0.08, 0.06],
            faces=tex0(),
            rotation=[20, 0, 25],
        )
        gu = make_uuid()
        g = b.make_group(
            f"streak_{i+1}", [cx, h_y, cz], [slab_uuid],
            rotation=[0, math.degrees(ang) + 45, 0],
            group_uuid=gu,
        )
        streak_groups.append((gu, g))

    # =====================================================================
    # 4) Base funnel — 6 flat slabs at Y=0 in tight circle
    # =====================================================================
    base_cubes = []
    for i in range(6):
        ang = (i / 6.0) * 2 * math.pi
        cx = math.cos(ang) * 2.2
        cz = math.sin(ang) * 2.2
        base_cubes.append(b.add_cube(
            f"base_funnel_{i+1}",
            from_=[cx - 0.6, -0.05, cz - 0.25], to_=[cx + 0.6, 0.05, cz + 0.25],
            faces=tex1(),
            rotation=[0, math.degrees(ang), 0],
        ))
    base_uuid = make_uuid()
    base_group = b.make_group("base_funnel", [0, 0, 0], base_cubes, group_uuid=base_uuid)

    # =====================================================================
    # 5) Ejected ice chunks — 10 small irregular cubes at outer edge mid-height
    # =====================================================================
    chunk_groups = []
    for i in range(10):
        ang = (i / 10.0) * 2 * math.pi + 0.2
        rr = 10 + (i % 3) * 0.5
        cx = math.cos(ang) * rr
        cz = math.sin(ang) * rr
        h_y = 6 + (i % 4) * 1.5
        sz = 0.4 + (i % 3) * 0.15
        chunk_uuid = b.add_cube(
            f"ejected_chunk_{i+1}",
            from_=[-sz, -sz, -sz], to_=[sz, sz, sz],
            faces=tex0(),
            rotation=[i * 20, i * 30, i * 15],
        )
        gu = make_uuid()
        g = b.make_group(
            f"chunk_{i+1}", [cx, h_y, cz], [chunk_uuid],
            rotation=[0, math.degrees(ang), 0],
            group_uuid=gu,
        )
        chunk_groups.append((gu, g, ang, rr, h_y))

    # =====================================================================
    # ROOT
    # =====================================================================
    root_children = [eye_group, base_group]
    for (_uu, g, _rr) in ring_groups: root_children.append(g)
    for (_uu, g) in streak_groups: root_children.append(g)
    for (_uu, g, *_x) in chunk_groups: root_children.append(g)
    root_uuid = make_uuid()
    root_g = b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid)
    b.outliner.append(root_g)

    # =====================================================================
    # ANIMATIONS
    # =====================================================================

    # ----- SPAWN: 1.0s -----
    spawn = {}
    # Base funnel at 0.0s expanding
    spawn[base_uuid] = {"name": "base_funnel", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 0, 1, 0),
        make_keyframe("scale", 0.3, 1.0, 1.0, 1.0),
        make_keyframe("scale", 1.0, 1.0, 1.0, 1.0),
    ]}
    # Bottom ring at 0.2s, each subsequent 0.12s after
    for ri, (gu, g, rr) in enumerate(ring_groups):
        delay = 0.2 + ri * 0.12
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.15, 1.15, 1.15, 1.15),
            make_keyframe("scale", delay + 0.25, 1.0, 1.0, 1.0),
            make_keyframe("scale", 1.0, 1.0, 1.0, 1.0),
        ]}
    # Blizzard streaks at 0.5s
    for si, (gu, g) in enumerate(streak_groups):
        delay = 0.5 + si * 0.015
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.1, 1.2, 1.2, 1.2),
            make_keyframe("scale", delay + 0.2, 1.0, 1.0, 1.0),
            make_keyframe("scale", 1.0, 1.0, 1.0, 1.0),
        ]}
    # Eye column last at 0.8s
    spawn[eye_uuid] = {"name": "eye_column", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 1, 0, 1),
        make_keyframe("scale", 0.8, 1, 0, 1),
        make_keyframe("scale", 1.0, 1.0, 1.0, 1.0),
    ]}
    # Ejected chunks fly outward at 0.7s
    for ci, (gu, g, ang, rr, h_y) in enumerate(chunk_groups):
        delay = 0.7 + ci * 0.015
        out_x = math.cos(ang) * 4
        out_z = math.sin(ang) * 4
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, -out_x * 0.4, 0, -out_z * 0.4),
            make_keyframe("position", delay, -out_x * 0.4, 0, -out_z * 0.4),
            make_keyframe("position", 1.0, 0, 0, 0),
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", 1.0, 1, 1, 1),
        ]}
    b.add_animation("spawn", length=1.0, animators=spawn, loop="once", override=True)

    # ----- IDLE: 3.0s loop -----
    idle = {}
    # Rings rotate Y at DIFFERENT speeds — bottom slowest, top fastest
    # Ring 1 (bottom, Y=1): 3.0s per rotation
    # Ring 2 (Y=3): 2.4s
    # Ring 3 (Y=6): 1.8s
    # Ring 4 (Y=10): 1.2s
    # Ring 5 (top, Y=14): 0.8s
    ring_periods = [3.0, 2.4, 1.8, 1.2, 0.8]
    for ri, ((gu, g, rr), period) in enumerate(zip(ring_groups, ring_periods)):
        kfs = []
        steps = 16
        for k in range(steps + 1):
            t = (k / steps) * 3.0
            ang_deg = (t / period) * 360
            kfs.append(make_keyframe("rotation", t, 0, ang_deg, 0))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Blizzard streaks rotate at intermediate speeds — varied per streak
    for si, (gu, g) in enumerate(streak_groups):
        kfs = []
        period = 1.5 + (si % 4) * 0.3
        for k in range(14):
            t = (k / 13) * 3.0
            ang_deg = (t / period) * 360 + si * 22.5
            kfs.append(make_keyframe("rotation", t, 0, ang_deg, 0))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Eye column DOES NOT ROTATE — but subtle scale shimmer for "life"
    eye_idle = []
    for k in range(12):
        t = (k / 11) * 3.0
        s = 1.0 + 0.015 * math.sin(t * 2 * math.pi / 2.0)
        eye_idle.append(make_keyframe("scale", t, s, 1.0, s))
    idle[eye_uuid] = {"name": "eye_column", "type": "bone", "keyframes": eye_idle}

    # Ejected chunks orbit at outer radius — tumbling
    for ci, (gu, g, ang, rr, h_y) in enumerate(chunk_groups):
        kfs = []
        for k in range(14):
            t = (k / 13) * 3.0
            ang_deg = (t / 2.0) * 360 + ci * 36
            kfs.append(make_keyframe("rotation", t, ci * 20 + t * 60, ang_deg, ci * 30 + t * 90))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Base funnel rotates slowly
    base_idle = []
    for k in range(12):
        t = (k / 11) * 3.0
        ang_deg = (t / 3.0) * 360
        base_idle.append(make_keyframe("rotation", t, 0, ang_deg, 0))
    idle[base_uuid] = {"name": "base_funnel", "type": "bone", "keyframes": base_idle}

    b.add_animation("idle", length=3.0, animators=idle, loop="loop", override=False)

    # ----- DISSIPATE: 0.7s -----
    diss = {}
    # Rings spin up to maximum then scale to 0 from bottom to top
    for ri, (gu, g, rr) in enumerate(ring_groups):
        delay = ri * 0.08  # bottom first
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 0.3, 0, 720, 0),
            make_keyframe("rotation", 0.7, 0, 1440, 0),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", delay + 0.2, 1.2, 1.2, 1.2),
            make_keyframe("scale", min(delay + 0.5, 0.7), 0, 0, 0),
        ]}
    # Eye column fades last
    diss[eye_uuid] = {"name": "eye_column", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 1, 1, 1),
        make_keyframe("scale", 0.5, 1, 1, 1),
        make_keyframe("scale", 0.7, 0, 0, 0),
    ]}
    # Ejected chunks fly outward
    for ci, (gu, g, ang, rr, h_y) in enumerate(chunk_groups):
        out_x = math.cos(ang) * 8
        out_z = math.sin(ang) * 8
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.7, out_x, -2, out_z),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.7, 0, 0, 0),
        ]}
    # Base funnel expands and snaps
    diss[base_uuid] = {"name": "base_funnel", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 1, 1, 1),
        make_keyframe("scale", 0.4, 1.5, 1, 1.5),
        make_keyframe("scale", 0.7, 0, 0, 0),
    ]}
    # Streaks fly outward
    for si, (gu, g) in enumerate(streak_groups):
        ang = (si / 16.0) * 2 * math.pi
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.7, math.cos(ang) * 5, 1, math.sin(ang) * 5),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.7, 0, 0, 0),
        ]}
    b.add_animation("dissipate", length=0.7, animators=diss, loop="once", override=True)

    b.write(OUTPUT_PATH)
    print(f"Wrote {OUTPUT_PATH}")


if __name__ == "__main__":
    build()
