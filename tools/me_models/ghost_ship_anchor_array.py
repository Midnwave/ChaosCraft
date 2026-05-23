#!/usr/bin/env python3
"""Generator for ghost_ship_anchor_array.bbmodel — Chain Mode attack #22 (Dead Reckoning).

Visual intent: 3 enormous ghost-ship anchors descending from above on spectral
chains — different anchor designs (cross-fluke, classic stock, admiralty). Each
anchor's chain goes up into nothing — there is no visible ship.

Distinct material identity: barnacle-encrusted spectral iron — iron that has
been underwater so long it grew sea-life, then passed through the spectral
plane and became translucent. The most unusual material texture in the set.
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
from _chain_helpers import (
    base_fill,
    jitter_inplace,
    rust_patches,
    hammer_marks,
    rivet_grid,
    horizontal_grain,
    crack_lines_radial,
    specular_cluster,
    emissive_seam,
    chain_link_pair,
)

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/chain/me_attacks/ghost_ship_anchor_array.bbmodel"


# ---------------------------------------------------------------------------
# TEXTURE 0 — Barnacle-encrusted spectral iron (128x128 for bulk + detail)
# ---------------------------------------------------------------------------

def build_anchor_iron_texture():
    # Larger texture for filesize bulk + barnacle detail richness
    w = h = 160
    deep = hex_to_rgba("#060c10")
    barnacle = hex_to_rgba("#141e20")
    surface = hex_to_rgba("#283038")
    ghost = hex_to_rgba("#4a6068")
    emit = hex_to_rgba("#7090a0")
    rust_stain = hex_to_rgba("#3a2818")
    spectral_brt = hex_to_rgba("#90b0c0")

    pixels = base_fill(w, h, deep)

    # Base wash — vertical gradient, dark at bottom, ghost at top (anchor is "lit from above")
    for y in range(h):
        t = y / float(h - 1)
        c = color_lerp(deep, ghost, t * 0.55)
        for x in range(w):
            pixels[y * w + x] = c

    # Barnacle clusters — organic rough texture overlay
    rng = random.Random(701)
    for _ in range(85):
        cx = rng.randrange(w)
        cy = rng.randrange(h)
        cluster_n = rng.randint(14, 36)
        for _ in range(cluster_n):
            ox = rng.randint(-12, 12)
            oy = rng.randint(-12, 12)
            r = rng.randint(1, 3)
            for dy in range(-r, r + 1):
                for dx in range(-r, r + 1):
                    if dx * dx + dy * dy > r * r:
                        continue
                    x = (cx + ox + dx) % w
                    y = (cy + oy + dy) % h
                    d = math.sqrt(dx * dx + dy * dy)
                    t = d / max(0.5, r)
                    p = pixels[y * w + x]
                    if t < 0.5:
                        pixels[y * w + x] = color_lerp(p, barnacle, 0.85)
                    elif t < 0.8:
                        pixels[y * w + x] = color_lerp(p, surface, 0.6)
                    else:
                        pixels[y * w + x] = color_lerp(p, ghost, 0.4)

    # Rust stain blotches — places where the iron was once exposed
    rust_patches(pixels, w, h, deep, rust_stain, count=18, seed=99)

    # Spectral seams — vertical bands of brighter ghost-emit
    for sx in (8, 32, 64, 92, 116):
        emissive_seam(pixels, w, h, sx, emit, intensity=0.55, width=2)

    # Spectral horizontal grain — light through ghostly iron
    horizontal_grain(pixels, w, h, emit, density=0.35, seed=51)

    # Bright spectral hotspots
    specular_cluster(pixels, w, h, 64, 32, 12, spectral_brt, strength=0.45)
    specular_cluster(pixels, w, h, 28, 88, 8, spectral_brt, strength=0.35)
    specular_cluster(pixels, w, h, 96, 96, 8, spectral_brt, strength=0.35)
    specular_cluster(pixels, w, h, 96, 16, 5, spectral_brt, strength=0.25)
    specular_cluster(pixels, w, h, 16, 64, 6, spectral_brt, strength=0.3)

    # Additional fine barnacle speckle
    rng = random.Random(231)
    for _ in range(1800):
        x = rng.randrange(w)
        y = rng.randrange(h)
        p = pixels[y * w + x]
        pixels[y * w + x] = color_lerp(p, barnacle, 0.4)
    # Spectral micro-glow speckle
    rng2 = random.Random(811)
    for _ in range(900):
        x = rng2.randrange(w)
        y = rng2.randrange(h)
        p = pixels[y * w + x]
        pixels[y * w + x] = color_lerp(p, spectral_brt, 0.3)

    # Final grain
    jitter_inplace(pixels, jitter_range=12, seed=801, density=0.9)

    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# TEXTURE 1 — Ground impact craters w/ spectral + rust stain rings (128x128)
# ---------------------------------------------------------------------------

def build_crater_texture():
    w = h = 160
    ground = hex_to_rgba("#050604")
    impact = hex_to_rgba("#304858")
    stain = hex_to_rgba("#3a2818")
    bright = hex_to_rgba("#6090b0")

    pixels = base_fill(w, h, ground)

    cx, cy = w // 2, h // 2
    # Spectral impact rings (concentric)
    for r in range(8, 60, 4):
        intensity = max(0.0, 1.0 - (r / 60.0)) * 0.75
        for ang_step in range(0, 360, 1):
            ang = math.radians(ang_step)
            x = int(cx + r * math.cos(ang))
            y = int(cy + r * math.sin(ang))
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, impact, intensity)

    # Rust stain spreading from impact
    rust_patches(pixels, w, h, ground, stain, count=15, seed=44)

    # Crack lines radiating from impact
    crack_lines_radial(pixels, w, h, cx, cy, branches=14, color=impact, seed=199, max_len=55)

    # Center brightness
    specular_cluster(pixels, w, h, cx, cy, 14, bright, strength=0.6)
    specular_cluster(pixels, w, h, cx + 24, cy + 12, 6, bright, strength=0.3)
    specular_cluster(pixels, w, h, cx - 18, cy - 22, 6, bright, strength=0.3)

    jitter_inplace(pixels, jitter_range=10, seed=509, density=0.7)
    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# ANCHOR BUILDERS — 3 designs
# ---------------------------------------------------------------------------

def build_cross_fluke_anchor(b, prefix, base_y, face_fn):
    """Anchor 1: wide crossbar, 4-fluke cross pattern."""
    cubes = []
    # Wide crossbar (top - T-bar)
    cubes.append(b.add_cube(
        f"{prefix}_crossbar",
        from_=[-3.5, base_y + 5.5, -0.45],
        to_=[3.5, base_y + 6.2, 0.45],
        faces=face_fn(),
    ))
    # Vertical shank (4 segments)
    for i in range(4):
        y0 = base_y + 1 + i * 1.1
        y1 = base_y + 1 + (i + 1) * 1.1
        thick = 0.5 - i * 0.04
        cubes.append(b.add_cube(
            f"{prefix}_shank_{i+1}",
            from_=[-thick, y0, -thick],
            to_=[thick, y1, thick],
            faces=face_fn(),
        ))
    # Anchor ring at top of shank (connection to chain)
    cubes.append(b.add_cube(
        f"{prefix}_ring",
        from_=[-0.65, base_y + 5.3, -0.65],
        to_=[0.65, base_y + 6.4, 0.65],
        faces=face_fn(),
    ))
    # 4 cross-pattern flukes radiating from base
    fluke_angles = [0, 90, 180, 270]
    for fi, fa in enumerate(fluke_angles):
        ax = math.cos(math.radians(fa))
        az = math.sin(math.radians(fa))
        # Fluke shaft
        cubes.append(b.add_cube(
            f"{prefix}_fluke_{fi+1}_shaft",
            from_=[ax * 0.5 - 0.25, base_y + 0.25, az * 0.5 - 0.25],
            to_=[ax * 2.2 + 0.25, base_y + 0.65, az * 2.2 + 0.25],
            faces=face_fn(),
        ))
        # Fluke blade (wider tip)
        cubes.append(b.add_cube(
            f"{prefix}_fluke_{fi+1}_blade",
            from_=[ax * 2.0 - 0.55, base_y + 0.05, az * 2.0 - 0.55],
            to_=[ax * 2.7 + 0.55, base_y + 0.5, az * 2.7 + 0.55],
            faces=face_fn(),
        ))
        # Fluke spike (tip extension)
        cubes.append(b.add_cube(
            f"{prefix}_fluke_{fi+1}_spike",
            from_=[ax * 2.6 - 0.2, base_y + 0.15, az * 2.6 - 0.2],
            to_=[ax * 3.1 + 0.2, base_y + 0.4, az * 3.1 + 0.2],
            faces=face_fn(),
        ))
    return cubes


def build_classic_stock_anchor(b, prefix, base_y, face_fn):
    """Anchor 2: classic stock anchor, 2 flukes at 45° outward."""
    cubes = []
    # Stock (horizontal bar near top)
    cubes.append(b.add_cube(
        f"{prefix}_stock",
        from_=[-3.0, base_y + 4.8, -0.3],
        to_=[3.0, base_y + 5.4, 0.3],
        faces=face_fn(),
    ))
    # Vertical shank (4 segments)
    for i in range(4):
        y0 = base_y + 0.8 + i * 1.0
        y1 = base_y + 0.8 + (i + 1) * 1.0
        thick = 0.45 - i * 0.03
        cubes.append(b.add_cube(
            f"{prefix}_shank_{i+1}",
            from_=[-thick, y0, -thick],
            to_=[thick, y1, thick],
            faces=face_fn(),
        ))
    # Ring at top
    cubes.append(b.add_cube(
        f"{prefix}_ring",
        from_=[-0.6, base_y + 4.6, -0.6],
        to_=[0.6, base_y + 5.5, 0.6],
        faces=face_fn(),
    ))
    # 2 flukes at 45° (only on +/- X — true classic stock pattern)
    for side in (-1, 1):
        # Fluke arm going outward and down
        for seg in range(3):
            t0 = seg * 0.7
            t1 = (seg + 1) * 0.7
            x0 = side * (0.5 + t0 * 1.2)
            x1 = side * (0.5 + t1 * 1.2)
            y0 = base_y + 0.5 - t0 * 0.15
            y1 = base_y + 0.5 - t1 * 0.15
            cubes.append(b.add_cube(
                f"{prefix}_fluke_{(side+1)//2 + 1}_seg_{seg+1}",
                from_=[min(x0, x1) - 0.25, min(y0, y1) - 0.18, -0.35],
                to_=[max(x0, x1) + 0.25, max(y0, y1) + 0.18, 0.35],
                faces=face_fn(),
            ))
        # Fluke palm (wide flat at the end)
        palm_x = side * 2.6
        cubes.append(b.add_cube(
            f"{prefix}_fluke_{(side+1)//2 + 1}_palm",
            from_=[palm_x - side * 0.45 - 0.25, base_y + 0.05, -0.55],
            to_=[palm_x + side * 0.45 + 0.25, base_y + 0.55, 0.55],
            faces=face_fn(),
        ))
    return cubes


def build_admiralty_anchor(b, prefix, base_y, face_fn):
    """Anchor 3: admiralty pattern — heavy rounded flukes."""
    cubes = []
    # Heavier T-crossbar
    cubes.append(b.add_cube(
        f"{prefix}_crossbar",
        from_=[-2.8, base_y + 5.3, -0.55],
        to_=[2.8, base_y + 6.0, 0.55],
        faces=face_fn(),
    ))
    # Shank (5 segments, thicker)
    for i in range(5):
        y0 = base_y + 0.8 + i * 0.92
        y1 = base_y + 0.8 + (i + 1) * 0.92
        thick = 0.55 - i * 0.03
        cubes.append(b.add_cube(
            f"{prefix}_shank_{i+1}",
            from_=[-thick, y0, -thick],
            to_=[thick, y1, thick],
            faces=face_fn(),
        ))
    # Ring
    cubes.append(b.add_cube(
        f"{prefix}_ring",
        from_=[-0.7, base_y + 5.2, -0.7],
        to_=[0.7, base_y + 6.1, 0.7],
        faces=face_fn(),
    ))
    # 2 admiralty-style heavy rounded flukes (3 cubes each for rounded shape)
    for side in (-1, 1):
        # Curved fluke — 4 step segments to suggest rounded curve
        for seg in range(4):
            ang = math.radians(seg * 22.5 + 0)
            t = seg / 3.0
            cx = side * (0.5 + math.sin(math.radians(seg * 25)) * 1.8 + t * 0.5)
            cy = base_y + 0.5 - math.cos(math.radians(seg * 18)) * 0.5
            w_size = 0.42 - seg * 0.05
            cubes.append(b.add_cube(
                f"{prefix}_fluke_{(side+1)//2 + 1}_step_{seg+1}",
                from_=[cx - w_size, cy - w_size, -w_size - 0.05],
                to_=[cx + w_size, cy + w_size, w_size + 0.05],
                faces=face_fn(),
            ))
        # Heavy rounded fluke palm
        palm_x = side * 2.4
        cubes.append(b.add_cube(
            f"{prefix}_fluke_{(side+1)//2 + 1}_palm_1",
            from_=[palm_x - side * 0.5 - 0.3, base_y + 0.1, -0.55],
            to_=[palm_x + side * 0.5 + 0.3, base_y + 0.7, 0.55],
            faces=face_fn(),
        ))
        cubes.append(b.add_cube(
            f"{prefix}_fluke_{(side+1)//2 + 1}_palm_2",
            from_=[palm_x - side * 0.4 - 0.2, base_y + 0.3, -0.42],
            to_=[palm_x + side * 0.4 + 0.2, base_y + 0.85, 0.42],
            faces=face_fn(),
        ))
    return cubes


# ---------------------------------------------------------------------------
# BUILD MODEL
# ---------------------------------------------------------------------------

def build():
    b = Builder("ghost_ship_anchor_array", resolution=(160, 160), visible_box=(24, 28, 0))

    def t0_face():
        return uniform_face(0, 0, 160, 160, tex_index=0)
    def t0_face_alt(u, v, sz=20):
        return uniform_face(u, v, u + sz, v + sz, tex_index=0)
    def t1_face():
        return uniform_face(0, 0, 160, 160, tex_index=1)

    # =====================================================================
    # 3 anchors at x = -8, 0, 8
    # =====================================================================
    anchor_specs = [
        (-8, build_cross_fluke_anchor, "anchor_a"),
        ( 0, build_classic_stock_anchor, "anchor_b"),
        ( 8, build_admiralty_anchor, "anchor_c"),
    ]

    anchor_groups = []
    chain_groups = []
    crater_groups = []

    for ax_x, build_fn, prefix in anchor_specs:
        # Build anchor cubes at relative origin (base_y=0)
        anchor_cubes = build_fn(b, prefix, 0, lambda: t0_face_alt(
            (hash(prefix) % 100), ((hash(prefix) * 3) % 100), sz=24,
        ))
        ag_uuid = make_uuid()
        ag = b.make_group(prefix, [ax_x, 0, 0], anchor_cubes, group_uuid=ag_uuid)
        anchor_groups.append((ag_uuid, ag, ax_x, prefix))

        # 8 chain links going UPWARD from anchor ring to Y+22 (slightly taller chain)
        chain_link_uuids = []
        n_links = 8
        for li in range(n_links):
            ly = 6 + li * 1.9  # link center heights
            link_pair = chain_link_pair(
                b, f"{prefix}_chain_link_{li+1}",
                cx=ax_x, cy=ly, cz=0, scale=0.7,
                face_factory=lambda li_capture=li: t0_face_alt(
                    (li_capture * 9) % 100, (li_capture * 13) % 100, sz=18,
                ),
            )
            chain_link_uuids.extend(link_pair)
            # Add a small connector between this link and the next
            if li < n_links - 1:
                chain_link_uuids.append(b.add_cube(
                    f"{prefix}_chain_connector_{li+1}",
                    from_=[ax_x - 0.12, ly + 0.6, -0.12],
                    to_=[ax_x + 0.12, ly + 1.3, 0.12],
                    faces=t0_face_alt((li * 17) % 100, (li * 5) % 100, sz=10),
                ))
        cg_uuid = make_uuid()
        cg = b.make_group(f"{prefix}_chain", [ax_x, 0, 0], chain_link_uuids, group_uuid=cg_uuid)
        chain_groups.append((cg_uuid, cg, ax_x, prefix))

        # Ground impact crater — 2 concentric rings of slabs + central tile
        crater_cubes = []
        # Outer ring (12 slabs)
        for ci in range(12):
            ang = (ci / 12.0) * 2 * math.pi
            mx = math.cos(ang) * 2.4
            mz = math.sin(ang) * 2.4
            crater_cubes.append(b.add_cube(
                f"{prefix}_crater_outer_{ci+1}",
                from_=[mx - 0.65, -0.13, mz - 0.55],
                to_=[mx + 0.65, 0.07, mz + 0.55],
                faces=t1_face(),
                rotation=[0, math.degrees(ang), 0],
            ))
        # Inner ring (8 slabs)
        for ci in range(8):
            ang = (ci / 8.0) * 2 * math.pi + math.pi / 8
            mx = math.cos(ang) * 1.3
            mz = math.sin(ang) * 1.3
            crater_cubes.append(b.add_cube(
                f"{prefix}_crater_inner_{ci+1}",
                from_=[mx - 0.55, -0.12, mz - 0.45],
                to_=[mx + 0.55, 0.08, mz + 0.45],
                faces=t1_face(),
                rotation=[0, math.degrees(ang), 0],
            ))
        # Central tile
        crater_cubes.append(b.add_cube(
            f"{prefix}_crater_center",
            from_=[-1.0, -0.12, -1.0],
            to_=[1.0, 0.08, 1.0],
            faces=t1_face(),
        ))
        cu = make_uuid()
        cgroup = b.make_group(f"{prefix}_crater", [ax_x, 0, 0], crater_cubes, group_uuid=cu)
        crater_groups.append((cu, cgroup, ax_x, prefix))

    # =====================================================================
    # ROOT
    # =====================================================================
    root_children = []
    for ag_uuid, ag, _, _ in anchor_groups: root_children.append(ag)
    for cg_uuid, cg, _, _ in chain_groups: root_children.append(cg)
    for cu, cgr, _, _ in crater_groups: root_children.append(cgr)
    root_uuid = make_uuid()
    root_group = b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid)
    b.outliner.append(root_group)

    # =====================================================================
    # ANIMATIONS
    # =====================================================================

    # ----- SPAWN: 1.5s — anchors descend from Y+25 to Y=0 with staggered slam -----
    spawn = {}
    slam_times = [0.9, 1.0, 1.1]  # 3 anchors slam at staggered times
    for idx, ((ag_uuid, ag, ax_x, prefix), slam_t) in enumerate(zip(anchor_groups, slam_times)):
        spawn[ag_uuid] = {"name": ag["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, 0, 25, 0),
            make_keyframe("position", slam_t - 0.1, 0, 2, 0),
            make_keyframe("position", slam_t, 0, 0, 0),
            make_keyframe("position", slam_t + 0.05, 0, 0.5, 0),  # tiny bounce
            make_keyframe("position", slam_t + 0.15, 0, 0, 0),
            make_keyframe("position", 1.5, 0, 0, 0),
        ]}
    # Chains appear at Y+16 at 0.0s, descend with anchors
    for idx, ((cg_uuid, cg, ax_x, prefix), slam_t) in enumerate(zip(chain_groups, slam_times)):
        spawn[cg_uuid] = {"name": cg["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, 0, 16, 0),
            make_keyframe("position", slam_t, 0, 0, 0),
            make_keyframe("position", 1.5, 0, 0, 0),
        ]}
    # Craters appear as each anchor approaches ground (0.8s)
    for idx, ((cu, cgr, ax_x, prefix), slam_t) in enumerate(zip(crater_groups, slam_times)):
        delay = slam_t - 0.2
        spawn[cu] = {"name": cgr["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", slam_t, 1.3, 1, 1.3),
            make_keyframe("scale", slam_t + 0.15, 1.0, 1, 1.0),
            make_keyframe("scale", 1.5, 1.0, 1, 1.0),
        ]}

    b.add_animation("spawn", length=1.5, animators=spawn, loop="once", override=True)

    # ----- IDLE: 6.0s — anchors sway 1° with different periods -----
    idle = {}
    sway_periods = [3.0, 4.2, 5.0]
    for idx, ((ag_uuid, ag, ax_x, prefix), period) in enumerate(zip(anchor_groups, sway_periods)):
        idle[ag_uuid] = {"name": ag["name"], "type": "bone", "keyframes": [
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", period / 2, 0, 0, 1.0),
            make_keyframe("rotation", period, 0, 0, 0),
            make_keyframe("rotation", period * 1.5, 0, 0, -1.0),
            make_keyframe("rotation", min(6.0, period * 2), 0, 0, 0),
            make_keyframe("rotation", 6.0, 0, 0, 0),
        ]}
    # Chains sway in sync with their anchor
    for idx, ((cg_uuid, cg, ax_x, prefix), period) in enumerate(zip(chain_groups, sway_periods)):
        idle[cg_uuid] = {"name": cg["name"], "type": "bone", "keyframes": [
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", period / 2, 0, 0, 0.8),
            make_keyframe("rotation", period, 0, 0, 0),
            make_keyframe("rotation", period * 1.5, 0, 0, -0.8),
            make_keyframe("rotation", min(6.0, period * 2), 0, 0, 0),
            make_keyframe("rotation", 6.0, 0, 0, 0),
        ]}
    # Craters slowly pulse
    for idx, (cu, cgr, ax_x, prefix) in enumerate(crater_groups):
        idle[cu] = {"name": cgr["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 1.5, 1.05, 1, 1.05),
            make_keyframe("scale", 3.0, 1.0, 1, 1.0),
            make_keyframe("scale", 4.5, 1.05, 1, 1.05),
            make_keyframe("scale", 6.0, 1.0, 1, 1.0),
        ]}
    b.add_animation("idle", length=6.0, animators=idle, loop="loop", override=False)

    # ----- DISSIPATE: 1.2s — chains pull taut, anchors yanked up, craters fade -----
    diss = {}
    for idx, (ag_uuid, ag, ax_x, prefix) in enumerate(anchor_groups):
        delay = idx * 0.04
        diss[ag_uuid] = {"name": ag["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", delay, 0, 0, 0),
            make_keyframe("position", delay + 0.15, 0, -0.5, 0),  # pre-yank dip
            make_keyframe("position", delay + 0.4, 0, 4, 0),
            make_keyframe("position", 1.2, 0, 30, 0),
        ]}
    for idx, (cg_uuid, cg, ax_x, prefix) in enumerate(chain_groups):
        delay = idx * 0.04
        diss[cg_uuid] = {"name": cg["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", delay + 0.2, 0, 2, 0),
            make_keyframe("position", 1.2, 0, 30, 0),
        ]}
    for idx, (cu, cgr, ax_x, prefix) in enumerate(crater_groups):
        delay = 0.2 + idx * 0.04
        diss[cu] = {"name": cgr["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", delay, 1, 1, 1),
            make_keyframe("scale", delay + 0.4, 0, 1, 0),
            make_keyframe("scale", 1.2, 0, 1, 0),
        ]}
    b.add_animation("dissipate", length=1.2, animators=diss, loop="once", override=True)

    # =====================================================================
    # TEXTURES
    # =====================================================================
    b.add_texture("anchor_iron", build_anchor_iron_texture())
    b.add_texture("anchor_craters", build_crater_texture())

    return b


if __name__ == "__main__":
    builder = build()
    out = builder.write(OUTPUT_PATH)
    import os
    size = os.path.getsize(out)
    print(f"Wrote {out}")
    print(f"Size: {size} bytes ({size/1024:.1f} KB)")
