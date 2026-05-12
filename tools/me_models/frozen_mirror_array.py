#!/usr/bin/env python3
"""Generator for frozen_mirror_array.bbmodel — FreezingIce attack #22.

Visual intent: six flat panes of clear ice floating vertically around caster,
each reflecting cold environment. The mirrors don't attack — they redirect.
A kaleidoscope of winter. Mirrors orbit AND rotate on their own axis.
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
    fill_rect,
)
from _freezingice_helpers import (
    base_fill,
    specular_cluster,
    jitter_inplace,
)

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/freezingice/me_attacks/frozen_mirror_array.bbmodel"


# ---------------------------------------------------------------------------
# TEXTURES — 128x128 each
# ---------------------------------------------------------------------------

def build_mirror_texture():
    """Tex 0: CLEAR ICE MIRROR TECHNIQUE — silver base, blue depth, sky band."""
    w = h = 128
    silver_base = hex_to_rgba("#dce8f0")
    reflective  = hex_to_rgba("#b0c8dc")
    blue_depth  = hex_to_rgba("#6090b0")
    deep_edge   = hex_to_rgba("#2050a0")
    spec_band   = hex_to_rgba("#c8e8ff")
    spec_top    = hex_to_rgba("#ffffff")

    pixels = base_fill(w, h, silver_base)

    # Vertical gradient: top deep_edge -> middle reflective -> bottom blue_depth
    for y in range(h):
        t = y / float(h - 1)
        if t < 0.3:
            c = color_lerp(deep_edge, silver_base, t / 0.3)
        elif t < 0.7:
            c = color_lerp(silver_base, reflective, (t - 0.3) / 0.4)
        else:
            c = color_lerp(reflective, blue_depth, (t - 0.7) / 0.3)
        for x in range(w):
            pixels[y * w + x] = c

    # Bright horizontal specular band (sky reflection) at rows 40-48 (scaled 20-24 in 64x64)
    for y in range(40, 49):
        for x in range(w):
            p = pixels[y * w + x]
            d_band = abs(y - 44) / 4.0
            strength = 1.0 - min(1.0, d_band)
            pixels[y * w + x] = color_lerp(p, spec_band, strength * 0.85)

    # Even brighter centre line on band
    for x in range(w):
        p = pixels[44 * w + x]
        pixels[44 * w + x] = color_lerp(p, spec_top, 0.6)

    # Vertical streaks (mirror surface lengthwise glints)
    rng = random.Random(303)
    for _ in range(18):
        col = rng.randrange(w)
        start = rng.randrange(0, h - 20)
        end = start + rng.randrange(15, 60)
        for y in range(start, min(end, h)):
            for dx in range(-1, 2):
                xx = col + dx
                if 0 <= xx < w:
                    p = pixels[y * w + xx]
                    pixels[y * w + xx] = color_lerp(p, spec_band, 0.32 - abs(dx) * 0.08)

    # Soft cluster highlight near top-right (corner crystal glint)
    specular_cluster(pixels, w, h, 100, 22, 8, spec_top, strength=0.7)
    specular_cluster(pixels, w, h, 30, 70, 5, spec_band, strength=0.65)

    # Smooth blur-ish: average over horizontal strips lightly
    # do a 1-pass horizontal smooth into a copy
    smoothed = list(pixels)
    for y in range(h):
        for x in range(1, w - 1):
            p0 = pixels[y * w + x - 1]
            p1 = pixels[y * w + x]
            p2 = pixels[y * w + x + 1]
            r = (p0[0] + p1[0] * 2 + p2[0]) // 4
            g = (p0[1] + p1[1] * 2 + p2[1]) // 4
            bl = (p0[2] + p1[2] * 2 + p2[2]) // 4
            smoothed[y * w + x] = (r, g, bl, p1[3])
    pixels = smoothed

    # Per-pixel jitter for filesize bulk
    jitter_inplace(pixels, jitter_range=16, seed=505, density=1.0)
    jitter_inplace(pixels, jitter_range=10, seed=707, density=0.5)
    return png_from_pixels(pixels, w, h)


def build_crack_texture():
    """Tex 1: deep navy with diagonal frost-white crack pattern."""
    w = h = 128
    navy = hex_to_rgba("#040e1a")
    crack = hex_to_rgba("#90d0f0")
    crack_hi = hex_to_rgba("#e0f0ff")
    deep_hi = hex_to_rgba("#0c1a30")

    pixels = base_fill(w, h, navy)

    # Main diagonal crack from top-left to bottom-right
    rng = random.Random(919)
    # Multiple jagged diagonal crack lines
    for line_idx in range(5):
        # starting position
        start_y = rng.randrange(0, h // 3)
        start_x = rng.randrange(0, w // 3)
        # jagged walk
        y, x = start_y, start_x
        slope = 1.0 + rng.random() * 0.3
        for step in range(200):
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, crack, 0.85)
                # halo
                for dx in (-1, 1):
                    if 0 <= x + dx < w:
                        p2 = pixels[y * w + x + dx]
                        pixels[y * w + x + dx] = color_lerp(p2, crack, 0.35)
                for dy in (-1, 1):
                    if 0 <= y + dy < h:
                        p3 = pixels[(y + dy) * w + x]
                        pixels[(y + dy) * w + x] = color_lerp(p3, crack, 0.35)
            # walk
            x += 1
            if rng.random() < 0.5:
                y += int(slope)
            else:
                y += int(slope) + (1 if rng.random() < 0.4 else 0)
            if x >= w or y >= h:
                break
            # branch
            if step > 10 and step % 25 == 0:
                bx, by = x, y
                bang = rng.choice([-1, 1]) * (math.pi / 4 + rng.random() * 0.2)
                for s in range(15):
                    bx_i = int(x + math.cos(bang) * s)
                    by_i = int(y + math.sin(bang) * s)
                    if 0 <= bx_i < w and 0 <= by_i < h:
                        p4 = pixels[by_i * w + bx_i]
                        pixels[by_i * w + bx_i] = color_lerp(p4, crack, 0.6)

    # Bright spots at crack intersections (random)
    for _ in range(15):
        cx = rng.randrange(w)
        cy = rng.randrange(h)
        specular_cluster(pixels, w, h, cx, cy, 2, crack_hi, strength=0.85)

    # Sparse noise in dark areas
    rng2 = random.Random(2002)
    for _ in range(220):
        x = rng2.randrange(w)
        y = rng2.randrange(h)
        p = pixels[y * w + x]
        pixels[y * w + x] = color_lerp(p, deep_hi, rng2.random() * 0.5)

    jitter_inplace(pixels, jitter_range=14, seed=4044, density=1.0)
    jitter_inplace(pixels, jitter_range=10, seed=6066, density=0.45)
    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# MODEL
# ---------------------------------------------------------------------------

def build():
    b = Builder("frozen_mirror_array", resolution=(128, 128), visible_box=(20, 14, 0))
    b.add_texture("mirror_panes", build_mirror_texture())
    b.add_texture("mirror_cracks", build_crack_texture())

    def tex0(): return uniform_face(0, 0, 128, 128, tex_index=0)
    def tex1(): return uniform_face(0, 0, 128, 128, tex_index=1)

    # =====================================================================
    # 1) 6 ice mirror panes at radius 7, evenly spaced 60°, vertical, facing inward
    # =====================================================================
    mirror_groups = []  # each entry: (orbit_uuid, orbit_g, inner_uuid, inner_g)
    NUM_MIRRORS = 6
    RADIUS = 7
    for i in range(NUM_MIRRORS):
        ang = (i / NUM_MIRRORS) * 2 * math.pi
        cx_pos = math.cos(ang) * RADIUS
        cz_pos = math.sin(ang) * RADIUS

        cubes = []
        # Main pane: 4 flat slabs arranged in rectangle (tall & wide, very thin Z 0.3)
        # Tall: Y 2-8 (6 units), Wide: X -3..3 (6 units)
        pane_slabs = [
            # central slab
            (f"mirror{i+1}_pane_main", [-3, 2, -0.15], [3, 8, 0.15]),
            # left edge enhancement
            (f"mirror{i+1}_pane_left", [-3.2, 2.2, -0.13], [-2, 7.8, 0.13]),
            # right edge enhancement
            (f"mirror{i+1}_pane_right", [2, 2.2, -0.13], [3.2, 7.8, 0.13]),
            # mid-band reflection enhancement
            (f"mirror{i+1}_pane_band", [-2.8, 4.5, -0.17], [2.8, 5.5, 0.17]),
        ]
        for (name, fr, to) in pane_slabs:
            cubes.append(b.add_cube(name, from_=fr, to_=to, faces=tex0()))

        # Frame: 4 thin frame slabs around pane edges
        frame_slabs = [
            # top
            (f"mirror{i+1}_frame_top", [-3.3, 7.9, -0.2], [3.3, 8.2, 0.2]),
            # bottom
            (f"mirror{i+1}_frame_bot", [-3.3, 1.8, -0.2], [3.3, 2.1, 0.2]),
            # left
            (f"mirror{i+1}_frame_left", [-3.3, 2.0, -0.2], [-3.0, 8.0, 0.2]),
            # right
            (f"mirror{i+1}_frame_right", [3.0, 2.0, -0.2], [3.3, 8.0, 0.2]),
        ]
        for (name, fr, to) in frame_slabs:
            cubes.append(b.add_cube(name, from_=fr, to_=to, faces=tex1()))

        # Corner accents: 4 small flat cubes at corners (ice crystallisation)
        corner_positions = [
            (-3.2, 7.7), (3.2, 7.7), (-3.2, 2.3), (3.2, 2.3),
        ]
        for ci, (cx, cy) in enumerate(corner_positions):
            cubes.append(b.add_cube(
                f"mirror{i+1}_corner_{ci+1}",
                from_=[cx - 0.4, cy - 0.4, -0.25], to_=[cx + 0.4, cy + 0.4, 0.25],
                faces=tex1(),
            ))

        # Crack: 1 thin diagonal slab across pane
        # diagonal — rotate cube around Z
        crack_uuid = b.add_cube(
            f"mirror{i+1}_crack",
            from_=[-2.5, 4.7, -0.18], to_=[2.5, 5.3, 0.18],
            faces=tex1(),
            rotation=[0, 0, 25],
        )
        cubes.append(crack_uuid)

        # Inner group — the mirror pane itself, faces inward. Y axis = self-rotation.
        inner_uuid = make_uuid()
        # Inner group origin is mirror centre at radius 0; orbit group places it.
        inner_g = b.make_group(
            f"mirror{i+1}_inner", [0, 5, 0], cubes,
            rotation=[0, 0, 0], group_uuid=inner_uuid,
        )

        # Orbit group — at origin, contains inner group offset to (cx_pos, 0, cz_pos)
        # The inner group is at (cx_pos, 0, cz_pos) and faces inward
        # To make mirror face inward, rotate group Y by (ang_deg + 90)
        # We use orbit_group to control orbit, inner_group to face inward + self-rotate
        position_group = b.make_group(
            f"mirror{i+1}_position", [cx_pos, 0, cz_pos], [inner_g],
            rotation=[0, math.degrees(ang) + 90, 0],
            group_uuid=make_uuid(),
        )
        # We need inner to be a child positioned at origin offset. The position group
        # acts as the local placement. We'll add another wrapper for the orbit so all
        # 6 mirrors share one ring parent.
        mirror_groups.append((position_group, inner_uuid, inner_g))

    # Mirror ring parent (orbit) — all 6 position groups
    ring_children = [pg for (pg, _i, _ig) in mirror_groups]
    ring_orbit_uuid = make_uuid()
    ring_orbit_g = b.make_group("mirror_ring", [0, 0, 0], ring_children, group_uuid=ring_orbit_uuid)

    # =====================================================================
    # 2) Central reflection node — 4-cube sphere approximation at Y=6
    # =====================================================================
    node_cubes = []
    node_offsets = [
        ( 0,  0,  0,  0.9),
        ( 0.5,  0.4,  0.0, 0.7),
        (-0.5, -0.4,  0.0, 0.7),
        ( 0.0,  0.0,  0.6, 0.7),
    ]
    for i, (ox, oy, oz, sz) in enumerate(node_offsets):
        node_cubes.append(b.add_cube(
            f"node_{i+1}",
            from_=[ox - sz, 6 + oy - sz, oz - sz],
            to_=[ox + sz, 6 + oy + sz, oz + sz],
            faces=tex0(),
        ))
    node_uuid = make_uuid()
    node_group = b.make_group("central_node", [0, 6, 0], node_cubes, group_uuid=node_uuid)

    # =====================================================================
    # 3) Ground reflection ring — 8 flat slabs at Y=0
    # =====================================================================
    ground_cubes = []
    for i in range(8):
        ang = (i / 8.0) * 2 * math.pi
        cx = math.cos(ang) * 4.5
        cz = math.sin(ang) * 4.5
        ground_cubes.append(b.add_cube(
            f"ground_ring_slab_{i+1}",
            from_=[cx - 0.6, -0.1, cz - 0.2], to_=[cx + 0.6, 0.1, cz + 0.2],
            faces=tex1(),
            rotation=[0, math.degrees(ang), 0],
        ))
    ground_uuid = make_uuid()
    ground_g = b.make_group("ground_ring", [0, 0, 0], ground_cubes, group_uuid=ground_uuid)

    # =====================================================================
    # ROOT
    # =====================================================================
    root_uuid = make_uuid()
    root_g = b.make_group("root", [0, 0, 0],
                           [ring_orbit_g, node_group, ground_g],
                           group_uuid=root_uuid)
    b.outliner.append(root_g)

    # =====================================================================
    # ANIMATIONS
    # =====================================================================

    # ----- SPAWN: 1.0s -----
    spawn = {}
    spawn[node_uuid] = {"name": "central_node", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 0, 0, 0),
        make_keyframe("scale", 0.2, 1.3, 1.3, 1.3),
        make_keyframe("scale", 0.35, 1.0, 1.0, 1.0),
        make_keyframe("scale", 1.0, 1.0, 1.0, 1.0),
    ]}
    spawn[ground_uuid] = {"name": "ground_ring", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 0, 1, 0),
        make_keyframe("scale", 0.2, 0, 1, 0),
        make_keyframe("scale", 0.5, 1.0, 1.0, 1.0),
        make_keyframe("scale", 1.0, 1.0, 1.0, 1.0),
    ]}
    # Each mirror flies in from outer radius w/ glass-shard vibration (rapid ±3° oscillation)
    for i, (pg, iu, ig) in enumerate(mirror_groups):
        delay = i * 0.12
        ang = (i / NUM_MIRRORS) * 2 * math.pi
        # Start far away (radius 14 instead of 7)
        far_x = math.cos(ang) * 14
        far_z = math.sin(ang) * 14
        actual_x = math.cos(ang) * 7
        actual_z = math.sin(ang) * 7
        ox = far_x - actual_x
        oz = far_z - actual_z
        pos_kfs = [
            make_keyframe("position", 0.0, ox, 0, oz),
            make_keyframe("position", delay, ox, 0, oz),
            make_keyframe("position", delay + 0.18, 0, 0, 0),
            # vibration: rapid micro-position jitter
            make_keyframe("position", delay + 0.22, 0.05, 0, 0.05),
            make_keyframe("position", delay + 0.25, -0.05, 0, -0.05),
            make_keyframe("position", delay + 0.28, 0.03, 0, 0.03),
            make_keyframe("position", delay + 0.32, 0, 0, 0),
            make_keyframe("position", 1.0, 0, 0, 0),
        ]
        # Rotation vibration ±3°
        rot_kfs = [
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", delay + 0.18, 0, 0, 0),
            make_keyframe("rotation", delay + 0.21, 3, 0, 0),
            make_keyframe("rotation", delay + 0.24, -3, 0, 0),
            make_keyframe("rotation", delay + 0.27, 2, 0, 0),
            make_keyframe("rotation", delay + 0.30, -2, 0, 0),
            make_keyframe("rotation", delay + 0.34, 0, 0, 0),
            make_keyframe("rotation", 1.0, 0, 0, 0),
        ]
        # uuid is the position group's uuid
        pg_uuid = pg["uuid"]
        spawn[pg_uuid] = {"name": pg["name"], "type": "bone",
                          "keyframes": pos_kfs + rot_kfs}
    b.add_animation("spawn", length=1.0, animators=spawn, loop="once", override=True)

    # ----- IDLE: 7.0s loop -----
    idle = {}
    # The 6 mirrors orbit as a group on ring parent (7s orbit)
    ring_kfs = []
    for k in range(20):
        t = (k / 19) * 7.0
        ang_deg = (t / 7.0) * 360
        ring_kfs.append(make_keyframe("rotation", t, 0, ang_deg, 0))
    idle[ring_orbit_uuid] = {"name": "mirror_ring", "type": "bone", "keyframes": ring_kfs}

    # Each individual mirror rotates on its own normal axis (self-rotation)
    for i, (pg, iu, ig) in enumerate(mirror_groups):
        self_period = 3.5 + (i % 3) * 0.7
        kfs = []
        for k in range(16):
            t = (k / 15) * 7.0
            # Rotate around local Y (the mirror normal — since pane is in XY plane facing Z)
            ang_deg = (t / self_period) * 360 * (1 if i % 2 == 0 else -1)
            kfs.append(make_keyframe("rotation", t, 0, ang_deg, 0))
        # Subtle scale shimmer
        for k in range(14):
            t = (k / 13) * 7.0
            s = 1.0 + 0.02 * math.sin(t * 2 * math.pi / 2.5 + i)
            kfs.append(make_keyframe("scale", t, s, s, s))
        idle[iu] = {"name": ig["name"], "type": "bone", "keyframes": kfs}

    # Ground ring counter-rotates
    gr_kfs = []
    for k in range(16):
        t = (k / 15) * 7.0
        ang_deg = -(t / 7.0) * 360
        gr_kfs.append(make_keyframe("rotation", t, 0, ang_deg, 0))
    idle[ground_uuid] = {"name": "ground_ring", "type": "bone", "keyframes": gr_kfs}

    # Central node rotates all 3 axes
    node_kfs = []
    for k in range(20):
        t = (k / 19) * 7.0
        rx = (t / 7.0) * 360
        ry = (t / 7.0) * 720
        rz = (t / 7.0) * 180
        node_kfs.append(make_keyframe("rotation", t, rx, ry, rz))
    for k in range(14):
        t = (k / 13) * 7.0
        s = 1.0 + 0.05 * math.sin(t * 2 * math.pi / 3.5)
        node_kfs.append(make_keyframe("scale", t, s, s, s))
    idle[node_uuid] = {"name": "central_node", "type": "bone", "keyframes": node_kfs}

    b.add_animation("idle", length=7.0, animators=idle, loop="loop", override=False)

    # ----- DISSIPATE: 0.7s — simultaneous shatter -----
    diss = {}
    # All mirrors fly outward at different angles (shatter)
    for i, (pg, iu, ig) in enumerate(mirror_groups):
        ang = (i / NUM_MIRRORS) * 2 * math.pi
        out_x = math.cos(ang) * 12
        out_z = math.sin(ang) * 12
        pg_uuid = pg["uuid"]
        diss[pg_uuid] = {"name": pg["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.4, out_x * 0.7, 1.5, out_z * 0.7),
            make_keyframe("position", 0.7, out_x, -2, out_z),
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 0.4, 30 * (1 if i % 2 == 0 else -1), 90, 60),
            make_keyframe("rotation", 0.7, 80 * (1 if i % 2 == 0 else -1), 180, 120),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.5, 0.8, 0.8, 0.8),
            make_keyframe("scale", 0.7, 0.0, 0.0, 0.0),
        ]}
    # Node fades
    diss[node_uuid] = {"name": "central_node", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 1, 1, 1),
        make_keyframe("scale", 0.4, 1.3, 1.3, 1.3),
        make_keyframe("scale", 0.7, 0.0, 0.0, 0.0),
    ]}
    diss[ground_uuid] = {"name": "ground_ring", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 1, 1, 1),
        make_keyframe("scale", 0.4, 1.4, 1, 1.4),
        make_keyframe("scale", 0.7, 0.0, 0.0, 0.0),
    ]}
    b.add_animation("dissipate", length=0.7, animators=diss, loop="once", override=True)

    b.write(OUTPUT_PATH)
    print(f"Wrote {OUTPUT_PATH}")


if __name__ == "__main__":
    build()
