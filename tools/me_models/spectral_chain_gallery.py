#!/usr/bin/env python3
"""Generator for spectral_chain_gallery.bbmodel — Chain Mode attack #24 (The Exhibition).

Visual intent: 6 spectral chains hanging perfectly straight from invisible
points around a circle of radius 9. Each at a different length / different
spectral grey tone. Display markers at top, ground marks below, central
exhibit label. Horror is in the absolute stillness — chains that don't move.

Distinct identity: 6 spectral zones in the texture, each chain UV-mapped to
its corresponding row band. Different greys distinguish the chains as if
each came from a different ghost.
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

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/chain/me_attacks/spectral_chain_gallery.bbmodel"


# ---------------------------------------------------------------------------
# TEXTURE 0 — Gallery: 6 horizontal zones, one per chain (192x192 for richness)
# ---------------------------------------------------------------------------

def build_gallery_texture():
    # Use 192x192 — 32 rows per zone for 6 zones, gives breathing room for detail
    w = h = 192

    # 6 spectral tones — each chain reads slightly different
    zone_colors = [
        (hex_to_rgba("#7090a0"), hex_to_rgba("#506070")),  # coolest spectral grey
        (hex_to_rgba("#708090"), hex_to_rgba("#506070")),  # warmer
        (hex_to_rgba("#607080"), hex_to_rgba("#404858")),  # neutral
        (hex_to_rgba("#685878"), hex_to_rgba("#403848")),  # purple-tinted
        (hex_to_rgba("#506070"), hex_to_rgba("#304048")),  # darker
        (hex_to_rgba("#405060"), hex_to_rgba("#283038")),  # faintest
    ]
    soft_glow = hex_to_rgba("#8090a8")
    spectral_brt = hex_to_rgba("#a0b0c0")

    pixels = base_fill(w, h, hex_to_rgba("#404850"))

    # 6 zones, 32 rows each
    rows_per_zone = h // 6
    for zone_idx, (zone_bright, zone_dark) in enumerate(zone_colors):
        y0 = zone_idx * rows_per_zone
        y1 = (zone_idx + 1) * rows_per_zone
        # Vertical gradient within zone — bright at top, darker at bottom
        for y in range(y0, y1):
            t = (y - y0) / float(rows_per_zone - 1)
            c = color_lerp(zone_bright, zone_dark, t * 0.6)
            for x in range(w):
                pixels[y * w + x] = c

        # Spectral seam — vertical emit lines in each zone
        for sx in (16, 48, 80, 112, 144, 176):
            for y in range(y0, y1):
                p = pixels[y * w + sx]
                pixels[y * w + sx] = color_lerp(p, soft_glow, 0.4)

        # Horizontal grain — each zone has different density to read distinct
        density = 0.3 + zone_idx * 0.05
        rng = random.Random(331 + zone_idx)
        for y in range(y0, y1):
            if rng.random() > density:
                continue
            length = rng.randint(w // 4, w)
            x_start = rng.randrange(0, max(1, w - length))
            strength = 0.2 + rng.random() * 0.4
            for x in range(x_start, x_start + length):
                if 0 <= x < w:
                    p = pixels[y * w + x]
                    pixels[y * w + x] = color_lerp(p, soft_glow, strength)

        # Specular highlights — 3 per zone
        for sp in range(3):
            cx = 24 + (sp * 64) % w
            cy = y0 + rows_per_zone // 2
            specular_cluster(pixels, w, h, cx, cy, 5, spectral_brt, strength=0.4)

        # Hammer mark texture per zone
        hammer_marks(pixels, w, h, zone_dark, soft_glow, count=40, seed=701 + zone_idx)

    # Cross-zone fine noise
    jitter_inplace(pixels, jitter_range=8, seed=1001, density=0.85)

    # Bright micro speckles for ghost shimmer
    rng = random.Random(401)
    for _ in range(2400):
        x = rng.randrange(w)
        y = rng.randrange(h)
        p = pixels[y * w + x]
        pixels[y * w + x] = color_lerp(p, spectral_brt, 0.35)

    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# TEXTURE 1 — Ground marks + exhibit label (192x192)
# ---------------------------------------------------------------------------

def build_gallery_floor_texture():
    w = h = 192
    floor_dark = hex_to_rgba("#060408")
    mark_dim = hex_to_rgba("#181420")
    mark_main = hex_to_rgba("#302840")
    label_text = hex_to_rgba("#3a3248")
    label_bright = hex_to_rgba("#6058a0")
    glow = hex_to_rgba("#7068a8")

    pixels = base_fill(w, h, floor_dark)

    # Subtle horizontal grain
    horizontal_grain(pixels, w, h, mark_dim, density=0.3, seed=131)

    cx, cy = w // 2, h // 2
    # Precise circle marks — 4 concentric rings
    for r in (28, 50, 70, 88):
        for ang_step in range(0, 360, 1):
            ang = math.radians(ang_step)
            x = int(cx + r * math.cos(ang))
            y = int(cy + r * math.sin(ang))
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, mark_main, 0.8)

    # 6 angular mark points around the inner ring (where chains hang)
    for chain_idx in range(6):
        ang = (chain_idx / 6.0) * 2 * math.pi
        mx = int(cx + 70 * math.cos(ang))
        my = int(cy + 70 * math.sin(ang))
        specular_cluster(pixels, w, h, mx, my, 6, label_bright, strength=0.6)

    # Label text — horizontal lines suggesting plaque inscription, top half
    for line_y in range(50, 85, 5):
        # Each "line" is several short segments
        line_segs = 0
        x = 60
        while x < w - 60 and line_segs < 8:
            seg_len = random.Random(line_y * 7 + line_segs).randint(8, 18)
            for px in range(x, x + seg_len):
                if 0 <= px < w:
                    for dy in range(2):
                        if 0 <= line_y + dy < h:
                            p = pixels[(line_y + dy) * w + px]
                            pixels[(line_y + dy) * w + px] = color_lerp(p, label_text, 0.85)
            x += seg_len + 4
            line_segs += 1

    # Frame around the label area
    for x in range(50, w - 50):
        for y in (40, 95):
            if 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, label_text, 0.7)
    for y in range(40, 96):
        for x in (50, w - 50):
            if 0 <= x < w:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, label_text, 0.7)

    # Center bright glow
    specular_cluster(pixels, w, h, cx, cy, 16, glow, strength=0.55)
    specular_cluster(pixels, w, h, cx, 70, 5, glow, strength=0.3)

    # Crack lines through floor — fine
    crack_lines_radial(pixels, w, h, cx, cy + 40, branches=10, color=mark_main, seed=233, max_len=60)

    # Fine grain
    jitter_inplace(pixels, jitter_range=8, seed=991, density=0.75)
    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# BUILD MODEL
# ---------------------------------------------------------------------------

def build():
    b = Builder("spectral_chain_gallery", resolution=(192, 192), visible_box=(24, 18, 0))

    def t0_face_zone(zone_idx, sz=24):
        """UV face mapped to a specific zone (1 of 6) — vertical bands."""
        # Each zone is 32 rows tall, so v0 = zone_idx * 32
        v0 = zone_idx * 32
        # Use a region within that zone
        u0 = (zone_idx * 21) % (192 - sz)
        return uniform_face(u0, v0 + 4, u0 + sz, v0 + 4 + sz, tex_index=0)

    def t1_face():
        return uniform_face(0, 0, 192, 192, tex_index=1)
    def t1_face_alt(u, v, sz=24):
        return uniform_face(u, v, u + sz, v + sz, tex_index=1)

    # 6 chains at evenly spaced positions around radius 9
    chain_specs = [
        # (chain_idx, end_y, n_links) — varying lengths per spec
        (0, 2, 9),  # zone 1 (coolest), ends at Y=2, 9 links
        (1, 1, 10), # zone 2, ends at Y=1, 10 links
        (2, 4, 7),  # zone 3, ends at Y=4, 7 links
        (3, 0, 11), # zone 4, ends at Y=0, 11 links
        (4, 3, 8),  # zone 5, ends at Y=3, 8 links
        (5, 1, 10), # zone 6, ends at Y=1, 10 links
    ]

    chain_groups = []
    marker_groups = []
    ground_groups = []

    for zone_idx, end_y, n_links in chain_specs:
        angle = (zone_idx / 6.0) * 2 * math.pi
        cx_world = math.cos(angle) * 9
        cz_world = math.sin(angle) * 9

        # Build chain links from Y=12 down to end_y
        link_uuids = []
        top_y = 12
        link_spacing = (top_y - end_y) / n_links
        for li in range(n_links):
            ly = top_y - (li + 0.5) * link_spacing
            link_pair = chain_link_pair(
                b, f"chain_{zone_idx+1}_link_{li+1}",
                cx=0, cy=ly, cz=0, scale=0.5,
                face_factory=lambda zi=zone_idx: t0_face_zone(zi, sz=18),
            )
            link_uuids.extend(link_pair)
            # Small connector between links
            if li < n_links - 1:
                link_uuids.append(b.add_cube(
                    f"chain_{zone_idx+1}_connector_{li+1}",
                    from_=[-0.09, ly - link_spacing * 0.55, -0.09],
                    to_=[0.09, ly - link_spacing * 0.3, 0.09],
                    faces=t0_face_zone(zone_idx, sz=10),
                ))
        chain_group_uuid = make_uuid()
        chain_group = b.make_group(
            f"chain_{zone_idx+1}", [cx_world, 0, cz_world], link_uuids,
            group_uuid=chain_group_uuid,
        )
        chain_groups.append((chain_group_uuid, chain_group, zone_idx))

        # DISPLAY MARKER at top (Y=12) — small flat slab
        marker_cubes = []
        marker_cubes.append(b.add_cube(
            f"marker_{zone_idx+1}_plate",
            from_=[-0.55, 12.0, -0.55],
            to_=[0.55, 12.18, 0.55],
            faces=t0_face_zone(zone_idx, sz=20),
        ))
        # Secondary glow ring
        marker_cubes.append(b.add_cube(
            f"marker_{zone_idx+1}_glow",
            from_=[-0.7, 12.18, -0.7],
            to_=[0.7, 12.25, 0.7],
            faces=t0_face_zone(zone_idx, sz=14),
        ))
        mu = make_uuid()
        mg = b.make_group(f"marker_{zone_idx+1}", [cx_world, 0, cz_world], marker_cubes, group_uuid=mu)
        marker_groups.append((mu, mg, zone_idx))

        # GROUND MARK at base
        ground_cubes = []
        for gi in range(6):
            ga = (gi / 6.0) * 2 * math.pi
            mx_loc = math.cos(ga) * 0.85
            mz_loc = math.sin(ga) * 0.85
            ground_cubes.append(b.add_cube(
                f"ground_{zone_idx+1}_slab_{gi+1}",
                from_=[mx_loc - 0.32, 0.0, mz_loc - 0.32],
                to_=[mx_loc + 0.32, 0.1, mz_loc + 0.32],
                faces=t1_face_alt((gi * 22) % 160, (gi * 28) % 160, sz=18),
                rotation=[0, math.degrees(ga), 0],
            ))
        # Center mark
        ground_cubes.append(b.add_cube(
            f"ground_{zone_idx+1}_center",
            from_=[-0.4, 0.0, -0.4],
            to_=[0.4, 0.09, 0.4],
            faces=t1_face_alt(60, 60, sz=18),
        ))
        gu = make_uuid()
        ggroup = b.make_group(f"ground_{zone_idx+1}", [cx_world, 0, cz_world], ground_cubes, group_uuid=gu)
        ground_groups.append((gu, ggroup, zone_idx))

    # CENTRAL EXHIBIT LABEL — flat rectangular slab at Y=5
    label_cubes = []
    # Main plaque
    label_cubes.append(b.add_cube(
        "exhibit_label_plaque",
        from_=[-2.0, 4.5, -0.18],
        to_=[2.0, 5.5, 0.18],
        faces=t1_face(),
    ))
    # Frame top/bottom
    label_cubes.append(b.add_cube(
        "exhibit_label_top_frame",
        from_=[-2.1, 5.5, -0.22],
        to_=[2.1, 5.65, 0.22],
        faces=t1_face_alt(0, 0, sz=30),
    ))
    label_cubes.append(b.add_cube(
        "exhibit_label_bot_frame",
        from_=[-2.1, 4.35, -0.22],
        to_=[2.1, 4.5, 0.22],
        faces=t1_face_alt(0, 30, sz=30),
    ))
    # Side frames
    label_cubes.append(b.add_cube(
        "exhibit_label_left_frame",
        from_=[-2.2, 4.4, -0.22],
        to_=[-2.0, 5.6, 0.22],
        faces=t1_face_alt(30, 0, sz=30),
    ))
    label_cubes.append(b.add_cube(
        "exhibit_label_right_frame",
        from_=[2.0, 4.4, -0.22],
        to_=[2.2, 5.6, 0.22],
        faces=t1_face_alt(30, 30, sz=30),
    ))
    # Pedestal posts (2)
    for px in (-1.5, 1.5):
        for seg in range(3):
            y0 = seg * 1.4
            y1 = (seg + 1) * 1.4
            label_cubes.append(b.add_cube(
                f"exhibit_post_{1 if px<0 else 2}_seg_{seg+1}",
                from_=[px - 0.13, y0, -0.13],
                to_=[px + 0.13, y1, 0.13],
                faces=t1_face_alt((seg * 17) % 160, (seg * 11) % 160, sz=20),
            ))

    label_uuid = make_uuid()
    label_group = b.make_group("exhibit_label", [0, 0, 0], label_cubes, group_uuid=label_uuid)

    # =====================================================================
    # ROOT
    # =====================================================================
    root_children = [label_group]
    for cu, cg, zi in chain_groups: root_children.append(cg)
    for mu, mg, zi in marker_groups: root_children.append(mg)
    for gu, gg, zi in ground_groups: root_children.append(gg)
    root_uuid = make_uuid()
    root_group = b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid)
    b.outliner.append(root_group)

    # =====================================================================
    # ANIMATIONS
    # =====================================================================

    # ----- SPAWN: 1.3s -----
    spawn = {}
    # Markers appear at 0.0s
    for mi, (mu, mg, zi) in enumerate(marker_groups):
        spawn[mu] = {"name": mg["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", 0.05 + zi * 0.02, 0, 0, 0),
            make_keyframe("scale", 0.15 + zi * 0.02, 1.2, 1.2, 1.2),
            make_keyframe("scale", 0.3 + zi * 0.02, 1.0, 1.0, 1.0),
            make_keyframe("scale", 1.3, 1.0, 1.0, 1.0),
        ]}
    # Chains lower from above (position Y from +6 to 0 — lowering effect via parent)
    for ci, (cu, cg, zi) in enumerate(chain_groups):
        start_delay = 0.1 + zi * 0.04
        spawn[cu] = {"name": cg["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, 0, 6, 0),
            make_keyframe("position", start_delay, 0, 6, 0),
            make_keyframe("position", start_delay + 0.5, 0, 0, 0),
            make_keyframe("position", 1.3, 0, 0, 0),
        ]}
        # Also fade in via scale
        spawn[cu]["keyframes"].extend([
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", start_delay, 0, 0, 0),
            make_keyframe("scale", start_delay + 0.2, 1, 1, 1),
        ])
    # Ground marks appear at end of chain descent
    for gi, (gu, gg, zi) in enumerate(ground_groups):
        delay = 0.6 + zi * 0.04
        spawn[gu] = {"name": gg["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.2, 1.1, 1, 1.1),
            make_keyframe("scale", delay + 0.35, 1.0, 1, 1.0),
            make_keyframe("scale", 1.3, 1.0, 1, 1.0),
        ]}
    # Exhibit label materialises at 0.8s
    spawn[label_uuid] = {"name": "exhibit_label", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 0, 0, 0),
        make_keyframe("scale", 0.8, 0, 0, 0),
        make_keyframe("scale", 1.0, 1.2, 1.2, 1.2),
        make_keyframe("scale", 1.15, 1.0, 1.0, 1.0),
        make_keyframe("scale", 1.3, 1.0, 1.0, 1.0),
    ]}
    b.add_animation("spawn", length=1.3, animators=spawn, loop="once", override=True)

    # ----- IDLE: 8.0s loop — chains DO NOT MOVE. Only markers + label breath -----
    idle = {}
    # Marker very subtle scale breath (1.0 -> 1.02 -> 1.0 over 8s)
    for mi, (mu, mg, zi) in enumerate(marker_groups):
        phase = zi * 0.5
        idle[mu] = {"name": mg["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 2.0, 1.015, 1.015, 1.015),
            make_keyframe("scale", 4.0, 1.0, 1.0, 1.0),
            make_keyframe("scale", 6.0, 1.02, 1.02, 1.02),
            make_keyframe("scale", 8.0, 1.0, 1.0, 1.0),
        ]}
    # Exhibit label glows subtly
    idle[label_uuid] = {"name": "exhibit_label", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 1, 1, 1),
        make_keyframe("scale", 4.0, 1.01, 1.01, 1.01),
        make_keyframe("scale", 8.0, 1.0, 1.0, 1.0),
    ]}
    b.add_animation("idle", length=8.0, animators=idle, loop="loop", override=False)

    # ----- DISSIPATE: 1.0s — chains retract upward link by link -----
    diss = {}
    for ci, (cu, cg, zi) in enumerate(chain_groups):
        delay = zi * 0.03
        diss[cu] = {"name": cg["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", delay, 0, 0, 0),
            make_keyframe("position", delay + 0.5, 0, 6, 0),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", delay + 0.3, 1, 1, 1),
            make_keyframe("scale", delay + 0.7, 0, 0, 0),
            make_keyframe("scale", 1.0, 0, 0, 0),
        ]}
    for mi, (mu, mg, zi) in enumerate(marker_groups):
        delay = 0.5 + zi * 0.03
        diss[mu] = {"name": mg["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", delay, 1, 1, 1),
            make_keyframe("scale", 1.0, 0, 0, 0),
        ]}
    for gi, (gu, gg, zi) in enumerate(ground_groups):
        delay = 0.2 + zi * 0.03
        diss[gu] = {"name": gg["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", delay, 1, 1, 1),
            make_keyframe("scale", delay + 0.4, 0, 1, 0),
            make_keyframe("scale", 1.0, 0, 1, 0),
        ]}
    diss[label_uuid] = {"name": "exhibit_label", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 1, 1, 1),
        make_keyframe("scale", 0.4, 1, 1, 1),
        make_keyframe("scale", 1.0, 0, 0, 0),
    ]}
    b.add_animation("dissipate", length=1.0, animators=diss, loop="once", override=True)

    # =====================================================================
    # TEXTURES
    # =====================================================================
    b.add_texture("gallery_chains", build_gallery_texture())
    b.add_texture("gallery_floor", build_gallery_floor_texture())

    return b


if __name__ == "__main__":
    builder = build()
    out = builder.write(OUTPUT_PATH)
    import os
    size = os.path.getsize(out)
    print(f"Wrote {out}")
    print(f"Size: {size} bytes ({size/1024:.1f} KB)")
