#!/usr/bin/env python3
"""Generator for chain_orrery.bbmodel — Chain Mode attack #21 (Fate's Model).

Visual intent: an orrery made entirely of CHAINS rather than planets. Central
vertical axis column with 4 chain RINGS at different tilts and heights, each
ring composed of chain links circling the axis. Orbital arms connect rings to
axis. Engraved fate-mapping base plate at Y=0. Mechanical orrery movement —
rings spin AND orbit. Distinct from FreezingIce's avalanche_orrery (ice
planets with moons) — here every orbital body is itself a chain ring.

Palette: dark precision iron + purple cursed emissive.
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

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/chain/me_attacks/chain_orrery.bbmodel"


# ---------------------------------------------------------------------------
# TEXTURE 0 — Precision fate-iron with cursed contamination (64x64)
# ---------------------------------------------------------------------------

def build_orrery_iron_texture():
    # Bigger texture = more JSON bulk via base64 PNG
    w = h = 128
    iron_dark = hex_to_rgba("#0a0810")
    iron_craft = hex_to_rgba("#201828")
    iron_surf = hex_to_rgba("#403840")
    iron_edge = hex_to_rgba("#706068")
    curse_dim = hex_to_rgba("#3a1870")
    curse_bright = hex_to_rgba("#5030a0")
    curse_emit = hex_to_rgba("#8050ff")

    pixels = base_fill(w, h, iron_dark)

    # Horizontal gradient bands — precision-tooled iron surface
    for y in range(h):
        t = y / float(h - 1)
        # 3 distinct horizontal sections
        if t < 0.33:
            top, bot = iron_dark, iron_craft
            lt = t / 0.33
        elif t < 0.66:
            top, bot = iron_craft, iron_surf
            lt = (t - 0.33) / 0.33
        else:
            top, bot = iron_surf, iron_edge
            lt = (t - 0.66) / 0.34
        c = color_lerp(top, bot, lt)
        for x in range(w):
            pixels[y * w + x] = c

    # Precision hammer-mark grid (orderly, not chaotic — this is craft iron)
    hammer_marks(pixels, w, h, iron_dark, iron_edge, count=320, seed=331)

    # Subtle horizontal grain (precision filing)
    horizontal_grain(pixels, w, h, iron_edge, density=0.7, seed=7)

    # Rivet grid — precision craftwork rivets
    rivet_grid(pixels, w, h, iron_edge, iron_dark, step_x=14, step_y=14, radius=2)

    # CURSED CONTAMINATION — purple seams creeping through the iron
    # The mode purple contaminates even the precision instruments
    for seam_x in (12, 28, 44, 60, 76, 92, 108):
        emissive_seam(pixels, w, h, seam_x, curse_bright, intensity=0.55, width=1)
    # Curse blotches — where the contamination has spread
    rust_patches(pixels, w, h, curse_dim, curse_bright, count=22, seed=509)

    # Crack lines radiating from curse contamination centres
    crack_lines_radial(pixels, w, h, 64, 64, branches=12, color=curse_bright, seed=711, max_len=40)

    # Brightest curse emit cluster — the chain glow
    specular_cluster(pixels, w, h, 64, 64, 14, curse_emit, strength=0.5)
    specular_cluster(pixels, w, h, 32, 96, 8, curse_emit, strength=0.35)
    specular_cluster(pixels, w, h, 96, 32, 8, curse_emit, strength=0.35)
    specular_cluster(pixels, w, h, 24, 24, 5, curse_emit, strength=0.25)
    specular_cluster(pixels, w, h, 104, 104, 5, curse_emit, strength=0.25)

    # Fine grain noise for organic feel
    jitter_inplace(pixels, jitter_range=10, seed=12, density=0.95)

    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# TEXTURE 1 — Engraved fate-mapping base plate (64x64)
# ---------------------------------------------------------------------------

def build_base_plate_texture():
    w = h = 128
    stone_dark = hex_to_rgba("#0c0a10")
    engrave_dim = hex_to_rgba("#181420")
    engrave_main = hex_to_rgba("#302840")
    engrave_bright = hex_to_rgba("#504070")
    curse_glow = hex_to_rgba("#7050d0")

    pixels = base_fill(w, h, stone_dark)

    # Subtle horizontal grain
    horizontal_grain(pixels, w, h, engrave_dim, density=0.4, seed=99)

    # Concentric circles — fate map
    cx, cy = w // 2, h // 2
    for r in (12, 22, 32, 42, 52, 60):
        for ang_step in range(0, 360, 1):
            ang = math.radians(ang_step)
            x = int(cx + r * math.cos(ang))
            y = int(cy + r * math.sin(ang))
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, engrave_main, 0.8)

    # 48 radial fate lines
    for i in range(48):
        ang = (i / 48.0) * 2 * math.pi
        for step in range(2, 60):
            x = int(cx + step * math.cos(ang))
            y = int(cy + step * math.sin(ang))
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, engrave_main, 0.65)

    # Inscribed triangles — 3 rotated layers
    for tri_layer in range(3):
        rot = (tri_layer / 3.0) * 2 * math.pi / 3
        for v in range(3):
            ang0 = rot + v * (2 * math.pi / 3)
            ang1 = rot + (v + 1) * (2 * math.pi / 3)
            x0 = int(cx + 44 * math.cos(ang0))
            y0 = int(cy + 44 * math.sin(ang0))
            x1 = int(cx + 44 * math.cos(ang1))
            y1 = int(cy + 44 * math.sin(ang1))
            steps = max(abs(x1 - x0), abs(y1 - y0))
            if steps > 0:
                for s in range(steps):
                    t = s / steps
                    x = int(x0 + (x1 - x0) * t)
                    y = int(y0 + (y1 - y0) * t)
                    if 0 <= x < w and 0 <= y < h:
                        p = pixels[y * w + x]
                        pixels[y * w + x] = color_lerp(p, engrave_bright, 0.65)

    # Center curse glow
    specular_cluster(pixels, w, h, cx, cy, 10, curse_glow, strength=0.6)
    # Additional 4 glow points at the cardinal positions
    for px, py in [(cx, 20), (cx, h - 20), (20, cy), (w - 20, cy)]:
        specular_cluster(pixels, w, h, px, py, 4, curse_glow, strength=0.3)

    # Fine grain
    jitter_inplace(pixels, jitter_range=10, seed=233, density=0.8)

    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# BUILD MODEL
# ---------------------------------------------------------------------------

def build():
    b = Builder("chain_orrery", resolution=(64, 64), visible_box=(20, 20, 0))

    # Face factories
    def t0_face_iron():
        return uniform_face(0, 0, 64, 64, tex_index=0)
    def t0_face_iron_alt(u, v, sz=14):
        return uniform_face(u, v, u + sz, v + sz, tex_index=0)
    def t1_face_base():
        return uniform_face(0, 0, 64, 64, tex_index=1)

    # =====================================================================
    # 1) CENTRAL VERTICAL AXIS COLUMN — 4 stacked cube segments Y=4..12
    # =====================================================================
    axis_cubes = []
    for i in range(4):
        y0 = 4 + i * 2
        y1 = 4 + (i + 1) * 2
        # Slightly tapered — wider at bottom, thinner at top
        thick = 0.55 - i * 0.04
        axis_cubes.append(b.add_cube(
            f"axis_seg_{i+1}",
            from_=[-thick, y0, -thick],
            to_=[thick, y1, thick],
            faces=t0_face_iron(),
        ))
    # Axis caps (decorative rings at top + bottom)
    for cap_i, y in enumerate([3.9, 12.0]):
        axis_cubes.append(b.add_cube(
            f"axis_cap_{cap_i+1}",
            from_=[-0.7, y - 0.15, -0.7],
            to_=[0.7, y + 0.15, 0.7],
            faces=t0_face_iron_alt(2, 2),
        ))
    axis_group_uuid = make_uuid()
    axis_group = b.make_group("axis_column", [0, 8, 0], axis_cubes, group_uuid=axis_group_uuid)

    # =====================================================================
    # 2) FOUR CHAIN ORBITAL RINGS — each ring is its own bone with tilt
    # =====================================================================
    # Ring specs: (n_links, radius, tilt_deg_x, tilt_deg_z, link_scale, name)
    # Bumped link counts for richer geometry — keeps "12,10,8,6" target counts but
    # adds bridge segments between each
    ring_specs = [
        (16, 5.0, 0,    0,   0.65, "ring_horiz"),
        (14, 7.0, 22.5, 0,   0.6,  "ring_tilt22"),
        (12, 9.0, 45.0, 0,   0.6,  "ring_tilt45"),
        ( 8, 6.0, 90.0, 0,   0.6,  "ring_vert"),
    ]

    ring_groups = []
    for n_links, radius, tilt_x, tilt_z, link_scale, ring_name in ring_specs:
        link_uuids = []
        for i in range(n_links):
            ang = (i / n_links) * 2 * math.pi
            cx = math.cos(ang) * radius
            cz = math.sin(ang) * radius
            # Each link is oriented tangent to the ring (rotate around Y so its plane faces along orbit)
            # We build via chain_link_pair which builds 2 crossed slabs at position
            link_pair = chain_link_pair(
                b, f"{ring_name}_link_{i+1}",
                cx=cx, cy=0, cz=cz,
                scale=link_scale,
                face_factory=lambda: t0_face_iron_alt(
                    (i * 5) % 50, (i * 7) % 50, sz=12,
                ),
            )
            # Also wrap each link in its own bone for individual oscillation
            # (already a list of cube uuids — bake them into a single tagged group later if needed)
            link_uuids.extend(link_pair)
            # Add a small bridging cube between this and next link (chain segment)
            next_ang = ((i + 1) / n_links) * 2 * math.pi
            nx = math.cos(next_ang) * radius
            nz = math.sin(next_ang) * radius
            mx = (cx + nx) / 2
            mz = (cz + nz) / 2
            link_uuids.append(b.add_cube(
                f"{ring_name}_bridge_{i+1}",
                from_=[mx - 0.18, -0.12, mz - 0.18],
                to_=[mx + 0.18, 0.12, mz + 0.18],
                faces=t0_face_iron_alt(
                    (i * 11) % 50, (i * 13) % 50, sz=8,
                ),
            ))

        # Ring group at Y=8 with X-tilt
        ring_group_uuid = make_uuid()
        ring_group = b.make_group(
            ring_name, [0, 8, 0], link_uuids,
            rotation=[tilt_x, 0, tilt_z],
            group_uuid=ring_group_uuid,
        )
        # Orbit group — at axis, rotates Y so ring orbits the column
        orbit_group_uuid = make_uuid()
        orbit_group = b.make_group(
            f"{ring_name}_orbit", [0, 8, 0], [ring_group],
            rotation=[0, 0, 0],
            group_uuid=orbit_group_uuid,
        )
        ring_groups.append((orbit_group_uuid, orbit_group, ring_group_uuid, ring_group, ring_name))

    # =====================================================================
    # 3) ORBITAL ARM SUPPORTS — 4 thin slabs connecting each ring radius to axis
    # =====================================================================
    arm_groups = []
    arm_specs = [
        ("arm_1", 5.0),
        ("arm_2", 7.0),
        ("arm_3", 9.0),
        ("arm_4", 6.0),
    ]
    for arm_name, max_r in arm_specs:
        # 3 segments along the arm
        arm_cubes = []
        for seg in range(3):
            r0 = (seg / 3.0) * max_r
            r1 = ((seg + 1) / 3.0) * max_r
            arm_cubes.append(b.add_cube(
                f"{arm_name}_seg_{seg+1}",
                from_=[r0, -0.08, -0.18],
                to_=[r1, 0.08, 0.18],
                faces=t0_face_iron_alt((seg * 7) % 50, (seg * 11) % 50, sz=10),
            ))
        # Decorative joint cap at each end
        for jc, jx in enumerate([0.0, max_r]):
            arm_cubes.append(b.add_cube(
                f"{arm_name}_joint_{jc+1}",
                from_=[jx - 0.25, -0.18, -0.25],
                to_=[jx + 0.25, 0.18, 0.25],
                faces=t0_face_iron_alt(3, 3, sz=10),
            ))
        gu = make_uuid()
        g = b.make_group(arm_name, [0, 8, 0], arm_cubes, group_uuid=gu)
        arm_groups.append((gu, g))

    # =====================================================================
    # 4) GROUND BASE PLATE — 3 stacked slabs at Y=0 (engraved fate map)
    # =====================================================================
    base_groups = []
    base_radii = [8.5, 6.0, 3.5]
    for bi, r in enumerate(base_radii):
        # Each base ring is composed of 12 slab segments forming a flat disc
        slab_uuids = []
        seg_count = 12
        for i in range(seg_count):
            ang = (i / seg_count) * 2 * math.pi
            mx = math.cos(ang) * (r * 0.75)
            mz = math.sin(ang) * (r * 0.75)
            slab_uuids.append(b.add_cube(
                f"base_disc_{bi+1}_slab_{i+1}",
                from_=[mx - 0.8, bi * 0.06, mz - 0.55],
                to_=[mx + 0.8, bi * 0.06 + 0.12, mz + 0.55],
                faces=t1_face_base(),
                rotation=[0, math.degrees(ang), 0],
            ))
        # Central tile
        slab_uuids.append(b.add_cube(
            f"base_disc_{bi+1}_center",
            from_=[-0.7, bi * 0.06, -0.7],
            to_=[0.7, bi * 0.06 + 0.1, 0.7],
            faces=t1_face_base(),
        ))
        gu = make_uuid()
        g = b.make_group(f"base_disc_{bi+1}", [0, 0, 0], slab_uuids, group_uuid=gu)
        base_groups.append((gu, g))

    # =====================================================================
    # ROOT GROUP
    # =====================================================================
    root_children = [axis_group]
    for og, ogd, rg, rgd, name in ring_groups:
        root_children.append(ogd)
    for au, ag in arm_groups:
        root_children.append(ag)
    for bu, bg in base_groups:
        root_children.append(bg)

    root_uuid = make_uuid()
    root_group = b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid)
    b.outliner.append(root_group)

    # =====================================================================
    # ANIMATIONS
    # =====================================================================

    # ----- SPAWN: 1.2s -----
    spawn = {}
    # Base plate materialises at 0.0s
    for bi, (gu, g) in enumerate(base_groups):
        delay = 0.0 + bi * 0.05
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.2, 1.1, 1.1, 1.1),
            make_keyframe("scale", delay + 0.35, 1.0, 1.0, 1.0),
            make_keyframe("scale", 1.2, 1.0, 1.0, 1.0),
        ]}
    # Axis extends upward at 0.2s
    spawn[axis_group_uuid] = {"name": "axis_column", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 0, 0, 0),
        make_keyframe("scale", 0.2, 1, 0, 1),
        make_keyframe("scale", 0.45, 1, 1.1, 1),
        make_keyframe("scale", 0.55, 1, 1.0, 1),
        make_keyframe("scale", 1.2, 1, 1.0, 1),
    ]}
    # Orbital arms deploy at 0.5s
    for ai, (gu, g) in enumerate(arm_groups):
        delay = 0.5 + ai * 0.04
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 1, 1),
            make_keyframe("scale", delay + 0.18, 1.1, 1.0, 1.0),
            make_keyframe("scale", delay + 0.32, 1.0, 1.0, 1.0),
            make_keyframe("scale", 1.2, 1.0, 1.0, 1.0),
        ]}
    # Chain rings materialise 0.6-0.9s staggered
    for ri, (og, ogd, rg, rgd, name) in enumerate(ring_groups):
        delay = 0.6 + ri * 0.08
        spawn[rg] = {"name": rgd["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.15, 1.2, 1.2, 1.2),
            make_keyframe("scale", delay + 0.3, 1.0, 1.0, 1.0),
            make_keyframe("scale", 1.2, 1.0, 1.0, 1.0),
        ]}
        # Orbit group already begins orbiting at spawn
        spawn[og] = {"name": ogd["name"], "type": "bone", "keyframes": [
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 1.2, 0, ri * 20, 0),
        ]}
    b.add_animation("spawn", length=1.2, animators=spawn, loop="once", override=True)

    # ----- IDLE: 8.0s -----
    idle = {}
    # Axis rotates slowly (1 turn per 8s)
    idle[axis_group_uuid] = {"name": "axis_column", "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, 0, 0, 0),
        make_keyframe("rotation", 2.0, 0, 90, 0),
        make_keyframe("rotation", 4.0, 0, 180, 0),
        make_keyframe("rotation", 6.0, 0, 270, 0),
        make_keyframe("rotation", 8.0, 0, 360, 0),
    ]}
    # Each ring rotates on its tilt axis (spin) at unique speed
    # AND orbits the central axis via its orbit_group
    ring_spin_rates = [3.0, 4.5, 6.0, 5.0]   # seconds per full spin
    ring_orbit_rates = [8.0, 12.0, 16.0, 20.0]  # seconds per full orbit (ring 4 slowest)
    for ri, (og, ogd, rg, rgd, name) in enumerate(ring_groups):
        spin_period = ring_spin_rates[ri]
        # SPIN — ring rotates on its own Y axis (which is tilted by parent rotation)
        # Compute key positions over 8s based on period
        spin_keys = [make_keyframe("rotation", 0.0, 0, 0, 0)]
        # how many full rotations in 8s
        rots_per_period = 8.0 / spin_period
        # We'll insert 4 evenly-spaced keys
        for k in range(1, 5):
            t = (k / 4.0) * 8.0
            ang = rots_per_period * 360 * (k / 4.0)
            spin_keys.append(make_keyframe("rotation", t, 0, ang, 0))
        idle[rg] = {"name": rgd["name"], "type": "bone", "keyframes": spin_keys}

        # ORBIT — orbit_group rotates Y at orbit rate
        orbit_period = ring_orbit_rates[ri]
        orbits_in_8s = 8.0 / orbit_period
        orbit_keys = [make_keyframe("rotation", 0.0, 0, 0, 0)]
        for k in range(1, 5):
            t = (k / 4.0) * 8.0
            ang = orbits_in_8s * 360 * (k / 4.0)
            orbit_keys.append(make_keyframe("rotation", t, 0, ang, 0))
        idle[og] = {"name": ogd["name"], "type": "bone", "keyframes": orbit_keys}

    # Orbital arms flex slightly (subtle Z-rot oscillation)
    for ai, (gu, g) in enumerate(arm_groups):
        period = 3.0 + ai * 0.4
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", period / 2, 0, 0, 3),
            make_keyframe("rotation", period, 0, 0, 0),
            make_keyframe("rotation", period * 1.5, 0, 0, -3),
            make_keyframe("rotation", min(8.0, period * 2), 0, 0, 0),
            make_keyframe("rotation", 8.0, 0, 0, 0),
        ]}

    # Base plate counter-rotates slowly
    for bi, (gu, g) in enumerate(base_groups):
        direction = -1 if bi % 2 == 0 else 1
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 4.0, 0, direction * 22, 0),
            make_keyframe("rotation", 8.0, 0, direction * 45, 0),
        ]}

    b.add_animation("idle", length=8.0, animators=idle, loop="loop", override=False)

    # ----- DISSIPATE: 1.0s -----
    diss = {}
    # Rings detach + fly outward on tangent (scale to 0)
    for ri, (og, ogd, rg, rgd, name) in enumerate(ring_groups):
        delay = ri * 0.04
        diss[rg] = {"name": rgd["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", delay, 1, 1, 1),
            make_keyframe("scale", delay + 0.3, 1.4, 1.4, 1.4),
            make_keyframe("scale", delay + 0.5, 0, 0, 0),
            make_keyframe("scale", 1.0, 0, 0, 0),
        ]}
    # Arms retract
    for ai, (gu, g) in enumerate(arm_groups):
        delay = 0.4 + ai * 0.05
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", delay, 1, 1, 1),
            make_keyframe("scale", delay + 0.25, 0, 1, 1),
            make_keyframe("scale", 1.0, 0, 1, 1),
        ]}
    # Axis contracts top down
    diss[axis_group_uuid] = {"name": "axis_column", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 1, 1, 1),
        make_keyframe("scale", 0.5, 1, 1, 1),
        make_keyframe("scale", 0.8, 1, 0, 1),
        make_keyframe("scale", 1.0, 0, 0, 0),
    ]}
    # Base plate folds
    for bi, (gu, g) in enumerate(base_groups):
        delay = 0.6 + bi * 0.06
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", delay, 1, 1, 1),
            make_keyframe("scale", delay + 0.25, 0, 0.5, 0),
            make_keyframe("scale", 1.0, 0, 0, 0),
        ]}
    b.add_animation("dissipate", length=1.0, animators=diss, loop="once", override=True)

    # =====================================================================
    # TEXTURES
    # =====================================================================
    b.add_texture("chain_orrery_iron", build_orrery_iron_texture())
    b.add_texture("chain_orrery_base", build_base_plate_texture())

    return b


if __name__ == "__main__":
    builder = build()
    out = builder.write(OUTPUT_PATH)
    import os
    size = os.path.getsize(out)
    print(f"Wrote {out}")
    print(f"Size: {size} bytes ({size/1024:.1f} KB)")
