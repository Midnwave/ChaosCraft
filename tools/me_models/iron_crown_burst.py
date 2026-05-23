#!/usr/bin/env python3
"""Generator for iron_crown_burst.bbmodel — Coronation of Ruin (Attack 9).

A massive RUSTED IRON CROWN materialises above the target then detonates.
The crown has chains hanging from each spike. Then it explodes outward.
Identity = CATASTROPHIC RUST, the most corroded model in the set. Crown shape
must be recognisable. Purple ornament emissive against all-rust iron.

Geometry:
  - Hero: 7 crown spike bone chains arranged in a ring at Y=10
    Each spike: 3 tapered segments. Heights: 8, 12, 9, 13, 10, 12, 9.
  - Crown band: 7 connecting flat slab segments
  - Chain hangings: from each spike tip, a 2-link chain hanging down (7 total)
  - Band ornaments: 7 small flat diamond pieces (purple emissive)
  - Ground shockwave: 14 flat slabs at Y=0
  - Rust drip accents: 4 thin flat slabs near the crown
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
from _chain_common import (
    base_fill,
    jitter_inplace,
    rust_blotch,
    horizontal_stress_lines,
    chain_link_silhouette,
    radial_glow,
)


OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/chain/me_attacks/iron_crown_burst.bbmodel"
RES = 128
CROWN_Y_BASE = 10.0
CROWN_RADIUS = 4.0
SPIKE_HEIGHTS = [8, 12, 9, 13, 10, 12, 9]  # 7 spikes


def build_crown_rust_texture():
    """IRON CROWN RUST — catastrophic corrosion, mostly rust over iron.
    Base #0e0806 corroded iron, #3c1408 heavy rust, #6e2c10 rust bloom,
    #a04c1c bright rust edge, #c07030 iron shine. NO emissive on crown,
    but ornament diamonds have faint purple #8030a0 glow.
    """
    w = h = RES
    deep_iron       = hex_to_rgba("#0e0806")
    heavy_rust      = hex_to_rgba("#3c1408")
    rust_bloom      = hex_to_rgba("#6e2c10")
    bright_rust     = hex_to_rgba("#a04c1c")
    iron_shine      = hex_to_rgba("#c07030")
    purple_orn      = hex_to_rgba("#8030a0")
    purple_bright   = hex_to_rgba("#b060d0")

    # Start with heavy rust as base (the crown is MOSTLY rust)
    pixels = base_fill(w, h, heavy_rust)

    # Vertical gradient — top has deeper rust shadows, middle is bright rust,
    # bottom shows iron underneath
    for y in range(h):
        t = y / max(1, h - 1)
        if t < 0.15:
            c = color_lerp(deep_iron, heavy_rust, t / 0.15)
        elif t < 0.45:
            c = color_lerp(heavy_rust, rust_bloom, (t - 0.15) / 0.30)
        elif t < 0.75:
            c = color_lerp(rust_bloom, bright_rust, (t - 0.45) / 0.30)
        else:
            c = color_lerp(bright_rust, iron_shine, (t - 0.75) / 0.25)
        for x in range(w):
            pixels[y * w + x] = c

    # AGGRESSIVE LARGE RUST PATCHES — irregular, catastrophic corrosion
    rust_blotch(pixels, w, h, heavy_rust, rust_bloom, bright_rust, count=22, seed=7)
    rust_blotch(pixels, w, h, deep_iron, heavy_rust, rust_bloom, count=14, seed=37)
    rust_blotch(pixels, w, h, rust_bloom, bright_rust, iron_shine, count=18, seed=61)

    # Iron underneath patches (where rust has flaked off) — darker
    import random
    rng = random.Random(89)
    for _ in range(12):
        cx = rng.randrange(w)
        cy = rng.randrange(h)
        radius = rng.randint(4, 8)
        for dy in range(-radius, radius + 1):
            for dx in range(-radius, radius + 1):
                d = math.sqrt(dx * dx + dy * dy)
                if d > radius:
                    continue
                accept = rng.random() > (d / radius) * 0.75
                if not accept:
                    continue
                x, y = (cx + dx) % w, (cy + dy) % h
                t = d / radius
                p = pixels[y * w + x]
                if t < 0.4:
                    pixels[y * w + x] = color_lerp(p, deep_iron, 0.85)
                else:
                    pixels[y * w + x] = color_lerp(p, heavy_rust, 0.55)

    # Drip streaks — rust dripping vertically
    for _ in range(28):
        x0 = rng.randrange(w)
        y0 = rng.randrange(h // 3)
        length = rng.randint(10, 35)
        for d in range(length):
            y = y0 + d
            if y >= h:
                break
            for dx in (-1, 0, 1):
                xx = x0 + dx
                if 0 <= xx < w:
                    p = pixels[y * w + xx]
                    intensity = 0.55 if dx == 0 else 0.30
                    intensity *= 1.0 - (d / length) * 0.5
                    pixels[y * w + xx] = color_lerp(p, rust_bloom, intensity)

    # A few purple ornament-emissive sparkles (sparse, as if jewels)
    for _ in range(8):
        cx = rng.randrange(w)
        cy = rng.randrange(h)
        radius = rng.randint(2, 4)
        for dy in range(-radius, radius + 1):
            for dx in range(-radius, radius + 1):
                if dx * dx + dy * dy > radius * radius:
                    continue
                x, y = (cx + dx) % w, (cy + dy) % h
                p = pixels[y * w + x]
                t_dist = math.sqrt(dx * dx + dy * dy) / radius
                target = purple_bright if t_dist < 0.4 else purple_orn
                pixels[y * w + x] = color_lerp(p, target, 0.7 * (1.0 - t_dist))

    # Horizontal corrosion lines
    horizontal_stress_lines(pixels, w, h, heavy_rust, count=16, seed=109)
    horizontal_stress_lines(pixels, w, h, deep_iron, count=10, seed=131)

    # Jitter for grain
    jitter_inplace(pixels, jitter_range=24, seed=151)
    jitter_inplace(pixels, jitter_range=14, seed=183, density=0.55)
    add_noise_overlay(pixels, w, h, deep_iron, iron_shine, density=0.10, seed=211)

    return png_from_pixels(pixels, w, h)


def build_ground_drip_texture():
    """Ground shockwave / rust drips: dark ground #060604 with rust-orange impact
    circles #7a2c0c at the crown's shadow point and drip lines flowing outward."""
    w = h = RES
    dark_ground  = hex_to_rgba("#060604")
    rust_orange  = hex_to_rgba("#7a2c0c")
    bright_drip  = hex_to_rgba("#a04420")
    soot         = hex_to_rgba("#1a0c08")
    pixels = base_fill(w, h, dark_ground)

    cx, cy = w // 2, h // 2

    # Impact circles — concentric, fading outward
    for ring_r in [12, 24, 36, 48]:
        thickness = 3
        for ang_step in range(360):
            ang = math.radians(ang_step)
            for tk in range(-thickness, thickness + 1):
                rr = ring_r + tk
                x = int(cx + math.cos(ang) * rr)
                y = int(cy + math.sin(ang) * rr)
                if 0 <= x < w and 0 <= y < h:
                    p = pixels[y * w + x]
                    intensity = 0.65 if abs(tk) < 2 else 0.30
                    falloff = 1.0 - (ring_r / 60.0)
                    target = bright_drip if abs(tk) < 1 else rust_orange
                    pixels[y * w + x] = color_lerp(p, target, intensity * falloff)

    # Radial rust drip lines flowing outward
    import random
    rng = random.Random(31)
    for s in range(36):
        ang = (s / 36.0) * 2 * math.pi + rng.uniform(-0.05, 0.05)
        for r in range(8, int(w * 0.55)):
            x = int(cx + math.cos(ang) * r)
            y = int(cy + math.sin(ang) * r)
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                falloff = 1.0 - (r / (w * 0.55))
                pixels[y * w + x] = color_lerp(p, rust_orange, 0.4 * falloff)

    # Burned soot around the impact area
    for _ in range(40):
        rx = rng.randrange(w)
        ry = rng.randrange(h)
        d_centre = math.sqrt((rx - cx) ** 2 + (ry - cy) ** 2)
        if d_centre < 30:
            radius = rng.randint(2, 5)
            for dy in range(-radius, radius + 1):
                for dx in range(-radius, radius + 1):
                    if dx * dx + dy * dy > radius * radius:
                        continue
                    x, y = (rx + dx) % w, (ry + dy) % h
                    p = pixels[y * w + x]
                    pixels[y * w + x] = color_lerp(p, soot, 0.45)

    add_noise_overlay(pixels, w, h, soot, bright_drip, density=0.10, seed=83)
    jitter_inplace(pixels, jitter_range=18, seed=121)
    jitter_inplace(pixels, jitter_range=11, seed=171, density=0.5)

    return png_from_pixels(pixels, w, h)


def build():
    b = Builder("iron_crown_burst", resolution=(RES, RES), visible_box=(14, 22, 0))
    b.add_texture("crown_rust", build_crown_rust_texture())
    b.add_texture("ground_drip", build_ground_drip_texture())

    crown_face = lambda: uniform_face(0, 0, RES, RES, tex_index=0)
    ground_face = lambda: uniform_face(0, 0, RES, RES, tex_index=1)

    # ============== CROWN SPIKES — 7 spike bones, each with 3 tapered segments ==============
    spike_groups = []
    chain_hang_groups = []
    spike_top_world_positions = []  # for chain hangings later
    for spike_idx, spike_h in enumerate(SPIKE_HEIGHTS):
        ang = (spike_idx / 7.0) * 2 * math.pi
        sx = math.cos(ang) * CROWN_RADIUS
        sz = math.sin(ang) * CROWN_RADIUS

        # 3 tapered segments stacked vertically from spike base upward
        seg_h = spike_h / 3.0
        spike_cubes = []
        for seg in range(3):
            # Taper: bottom is widest, top is narrowest
            base_factor = 1.0 - (seg / 3.0)  # 1, 0.667, 0.333
            top_factor = 1.0 - ((seg + 1) / 3.0)  # 0.667, 0.333, 0
            base_w = 0.7 * base_factor + 0.15
            top_w = 0.7 * top_factor + 0.10
            avg_w = (base_w + top_w) / 2.0
            y_low = seg * seg_h
            y_high = (seg + 1) * seg_h
            # Use the larger half-width; the taper is implied by the smaller upper width
            # but a single cube needs uniform width so we approximate per segment
            cu = b.add_cube(
                f"spike{spike_idx+1}_seg{seg+1}",
                from_=[-avg_w, y_low, -avg_w], to_=[avg_w, y_high, avg_w],
                faces=crown_face(),
            )
            spike_cubes.append(cu)

        gu = make_uuid()
        # Spike rotated to lean slightly inward at top? No — keep upright per spec
        grp = b.make_group(
            f"crown_spike_{spike_idx+1}",
            [sx, CROWN_Y_BASE, sz], spike_cubes,
            rotation=[0, math.degrees(ang) - 90, 0],
            group_uuid=gu,
        )
        spike_groups.append((gu, grp, spike_idx, spike_h, ang))
        # Spike tip world position for chain hanging
        spike_top_world_positions.append((sx, CROWN_Y_BASE + spike_h, sz, ang, spike_idx))

    # ============== CROWN BAND — 7 connecting slabs ==============
    band_groups = []
    for i in range(7):
        ang1 = (i / 7.0) * 2 * math.pi
        ang2 = ((i + 1) / 7.0) * 2 * math.pi
        # Midpoint of band segment
        mid_ang = (ang1 + ang2) / 2.0
        bx = math.cos(mid_ang) * CROWN_RADIUS
        bz = math.sin(mid_ang) * CROWN_RADIUS
        # Slab length spans between adjacent spikes
        slab_len = 2.0 * CROWN_RADIUS * math.sin(math.pi / 7.0)
        cu = b.add_cube(
            f"band_{i+1}",
            from_=[-slab_len / 2, -0.6, -0.35], to_=[slab_len / 2, 0.6, 0.35],
            faces=crown_face(),
        )
        gu = make_uuid()
        # Band slab tangent to the circle — rotate so long axis points along the chord
        slab_yaw = math.degrees(mid_ang) + 90
        grp = b.make_group(
            f"crown_band_{i+1}", [bx, CROWN_Y_BASE - 0.3, bz], [cu],
            rotation=[0, slab_yaw, 0], group_uuid=gu,
        )
        band_groups.append((gu, grp, mid_ang, i))

    # ============== CHAIN HANGINGS — from each spike tip, 2-link chain hanging down ==============
    # Each link = 4-cube oval, rotated horizontally so they hang vertically chained
    hanging_groups = []
    for (sx, sy, sz, ang, spike_idx) in spike_top_world_positions:
        # 2 links per hanging, link 1 immediately below tip, link 2 below link 1
        link_groups = []
        for link_idx in range(2):
            link_y_offset = -1.2 - link_idx * 1.0  # hanging down
            # Each link is 4 cubes (oval cross), thin
            top_cu = b.add_cube(
                f"hang{spike_idx+1}_l{link_idx+1}_top",
                from_=[-0.35, link_y_offset + 0.35, -0.13],
                to_=[0.35, link_y_offset + 0.50, 0.13],
                faces=crown_face(),
            )
            bot_cu = b.add_cube(
                f"hang{spike_idx+1}_l{link_idx+1}_bot",
                from_=[-0.35, link_y_offset - 0.50, -0.13],
                to_=[0.35, link_y_offset - 0.35, 0.13],
                faces=crown_face(),
            )
            # Sides: if link_idx is even, sides are X-facing; if odd, sides are Z-facing
            if link_idx % 2 == 0:
                left_cu = b.add_cube(
                    f"hang{spike_idx+1}_l{link_idx+1}_left",
                    from_=[-0.48, link_y_offset - 0.35, -0.13],
                    to_=[-0.22, link_y_offset + 0.35, 0.13],
                    faces=crown_face(),
                )
                right_cu = b.add_cube(
                    f"hang{spike_idx+1}_l{link_idx+1}_right",
                    from_=[0.22, link_y_offset - 0.35, -0.13],
                    to_=[0.48, link_y_offset + 0.35, 0.13],
                    faces=crown_face(),
                )
            else:
                left_cu = b.add_cube(
                    f"hang{spike_idx+1}_l{link_idx+1}_left",
                    from_=[-0.13, link_y_offset - 0.35, -0.48],
                    to_=[0.13, link_y_offset + 0.35, -0.22],
                    faces=crown_face(),
                )
                right_cu = b.add_cube(
                    f"hang{spike_idx+1}_l{link_idx+1}_right",
                    from_=[-0.13, link_y_offset - 0.35, 0.22],
                    to_=[0.13, link_y_offset + 0.35, 0.48],
                    faces=crown_face(),
                )
            link_groups.extend([top_cu, bot_cu, left_cu, right_cu])

        gu = make_uuid()
        grp = b.make_group(
            f"chain_hang_{spike_idx+1}", [sx, sy, sz], link_groups,
            rotation=[0, math.degrees(ang), 0], group_uuid=gu,
        )
        hanging_groups.append((gu, grp, sx, sy, sz, ang, spike_idx))

    # ============== BAND ORNAMENTS — 7 diamond pieces with purple emissive ==============
    ornament_groups = []
    for i in range(7):
        ang = (i / 7.0) * 2 * math.pi + math.pi / 7  # offset between spikes
        ox = math.cos(ang) * CROWN_RADIUS * 1.05
        oz = math.sin(ang) * CROWN_RADIUS * 1.05
        # Diamond = rotated cube (45° around Y)
        cu = b.add_cube(
            f"ornament_{i+1}",
            from_=[-0.30, -0.35, -0.30], to_=[0.30, 0.35, 0.30],
            faces=crown_face(),
        )
        gu = make_uuid()
        grp = b.make_group(
            f"band_ornament_{i+1}", [ox, CROWN_Y_BASE - 0.3, oz], [cu],
            rotation=[0, math.degrees(ang) + 45, 45], group_uuid=gu,
        )
        ornament_groups.append((gu, grp, ang, i))

    # ============== GROUND SHOCKWAVE — 14 flat slabs at Y=0 ==============
    ground_groups = []
    for i in range(14):
        ang = (i / 14.0) * 2 * math.pi
        cx = math.cos(ang) * 6.5
        cz = math.sin(ang) * 6.5
        cu = b.add_cube(
            f"gshock_{i+1}",
            from_=[-1.3, 0.02, -0.50], to_=[1.3, 0.10, 0.50],
            faces=ground_face(),
        )
        gu = make_uuid()
        grp = b.make_group(
            f"ground_shock_{i+1}", [cx, 0.05, cz], [cu],
            rotation=[0, math.degrees(ang), 0], group_uuid=gu,
        )
        ground_groups.append((gu, grp, ang))

    # ============== RUST DRIP ACCENTS — 4 thin slabs at various heights ==============
    drip_groups = []
    drip_specs = [
        (3.5, 7.0, math.pi / 4),
        (5.5, 6.0, math.pi),
        (4.5, 8.5, 3 * math.pi / 2),
        (3.0, 5.5, math.pi / 7),
    ]
    for (dx_r, dy, ang) in drip_specs:
        rx = math.cos(ang) * dx_r
        rz = math.sin(ang) * dx_r
        cu = b.add_cube(
            f"drip_{ang:.2f}",
            from_=[-0.15, -1.5, -0.15], to_=[0.15, 1.5, 0.15],
            faces=ground_face(),
        )
        gu = make_uuid()
        grp = b.make_group(
            f"rust_drip_{ang:.2f}", [rx, dy, rz], [cu],
            rotation=[10 * math.sin(ang), math.degrees(ang), 5 * math.cos(ang)],
            group_uuid=gu,
        )
        drip_groups.append((gu, grp, ang, dy))

    # ============== ROOT ==============
    root_children = []
    for (gu, g, *_r) in spike_groups: root_children.append(g)
    for (gu, g, *_r) in band_groups: root_children.append(g)
    for (gu, g, *_r) in hanging_groups: root_children.append(g)
    for (gu, g, *_r) in ornament_groups: root_children.append(g)
    for (gu, g, *_r) in ground_groups: root_children.append(g)
    for (gu, g, *_r) in drip_groups: root_children.append(g)
    root_uuid = make_uuid()
    b.outliner.append(b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid))

    # ============== SPAWN — 1.0s ==============
    spawn = {}
    # Ground shockwave expands at 0.0s
    for (gu, grp, ang) in ground_groups:
        kfs = [
            make_keyframe("scale", 0.00, 0, 0, 0),
            make_keyframe("scale", 0.20, 1.4, 1, 1.4),
            make_keyframe("scale", 0.45, 1.0, 1.0, 1.0),
            make_keyframe("scale", 1.00, 1.0, 1.0, 1.0),
        ]
        spawn[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Band materialises at 0.2s
    for (gu, grp, mid_ang, i) in band_groups:
        kfs = [
            make_keyframe("scale", 0.00, 0, 0, 0),
            make_keyframe("scale", 0.20, 0, 0, 0),
            make_keyframe("scale", 0.35, 1.3, 1.3, 1.3),
            make_keyframe("scale", 0.50, 1.0, 1.0, 1.0),
            make_keyframe("scale", 1.00, 1.0, 1.0, 1.0),
        ]
        spawn[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Spikes extend upward from band at 0.3s, staggered clockwise 0.06s each
    for (gu, grp, spike_idx, spike_h, ang) in spike_groups:
        t_start = 0.30 + spike_idx * 0.06
        kfs = [
            make_keyframe("scale", 0.00, 1, 0, 1),
            make_keyframe("scale", t_start, 1, 0, 1),
            make_keyframe("scale", t_start + 0.10, 1, 0.6, 1),
            make_keyframe("scale", t_start + 0.18, 1, 1.0, 1),
            make_keyframe("scale", 1.00, 1, 1, 1),
        ]
        spawn[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Chain hangings deploy at spike arrival
    for (gu, grp, sx, sy, sz, ang, spike_idx) in hanging_groups:
        t_start = 0.30 + spike_idx * 0.06 + 0.15
        kfs = [
            make_keyframe("scale", 0.00, 0, 0, 0),
            make_keyframe("scale", t_start, 0, 0, 0),
            make_keyframe("scale", t_start + 0.10, 1.2, 1.4, 1.2),
            make_keyframe("scale", t_start + 0.18, 1.0, 1.0, 1.0),
            make_keyframe("scale", 1.00, 1.0, 1.0, 1.0),
        ]
        spawn[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Ornaments snap in
    for (gu, grp, ang, i) in ornament_groups:
        t_start = 0.40 + i * 0.04
        kfs = [
            make_keyframe("scale", 0.00, 0, 0, 0),
            make_keyframe("scale", t_start, 0, 0, 0),
            make_keyframe("scale", t_start + 0.05, 1.6, 1.6, 1.6),
            make_keyframe("scale", t_start + 0.12, 1.0, 1.0, 1.0),
            make_keyframe("scale", 1.00, 1.0, 1.0, 1.0),
        ]
        spawn[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Rust drips materialise at 0.7s
    for (gu, grp, ang, dy) in drip_groups:
        kfs = [
            make_keyframe("scale", 0.00, 0, 0, 0),
            make_keyframe("scale", 0.70, 0, 0, 0),
            make_keyframe("scale", 0.85, 1.1, 1.4, 1.1),
            make_keyframe("scale", 1.00, 1.0, 1.0, 1.0),
        ]
        spawn[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("spawn", length=1.0, animators=spawn, loop="once", override=True)

    # ============== IDLE — 5.0s ==============
    idle = {}
    # Whole crown rotates Y slowly (we rotate each spike's group position around centre)
    for (gu, grp, spike_idx, spike_h, ang) in spike_groups:
        kfs = []
        for k in range(11):
            t = (k / 10.0) * 5.0
            phase = (t / 5.0) * 2 * math.pi * 0.18  # very slow rotation
            new_ang = ang + phase
            init_cx = math.cos(ang) * CROWN_RADIUS
            init_cz = math.sin(ang) * CROWN_RADIUS
            new_cx = math.cos(new_ang) * CROWN_RADIUS
            new_cz = math.sin(new_ang) * CROWN_RADIUS
            kfs.append(make_keyframe("position", t, new_cx - init_cx, 0, new_cz - init_cz))
            # Oscillate scale Y (each spike pulsates 1.0->1.04 staggered)
            ph = (t / 5.0 + spike_idx * 0.14) * 2 * math.pi
            sy = 1.0 + 0.04 * math.sin(ph)
            kfs.append(make_keyframe("scale", t, 1.0, sy, 1.0))
        idle[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Band slowly rotates Y with the spikes (same orbital phase)
    for (gu, grp, mid_ang, i) in band_groups:
        kfs = []
        for k in range(11):
            t = (k / 10.0) * 5.0
            phase = (t / 5.0) * 2 * math.pi * 0.18
            new_ang = mid_ang + phase
            init_cx = math.cos(mid_ang) * CROWN_RADIUS
            init_cz = math.sin(mid_ang) * CROWN_RADIUS
            new_cx = math.cos(new_ang) * CROWN_RADIUS
            new_cz = math.sin(new_ang) * CROWN_RADIUS
            kfs.append(make_keyframe("position", t, new_cx - init_cx, 0, new_cz - init_cz))
        idle[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Chain hangings sway with secondary lag
    for (gu, grp, sx, sy, sz, ang, spike_idx) in hanging_groups:
        kfs = []
        for k in range(11):
            t = (k / 10.0) * 5.0
            phase = (t / 5.0) * 2 * math.pi * 0.18
            new_ang = ang + phase
            init_cx = math.cos(ang) * CROWN_RADIUS
            init_cz = math.sin(ang) * CROWN_RADIUS
            new_cx = math.cos(new_ang) * CROWN_RADIUS
            new_cz = math.sin(new_ang) * CROWN_RADIUS
            # Position follows the spike (orbital) plus sway
            sway_x = math.sin((t / 5.0 + spike_idx * 0.12) * 2 * math.pi) * 0.3
            sway_z = math.cos((t / 5.0 + spike_idx * 0.12) * 2 * math.pi) * 0.3
            kfs.append(make_keyframe("position", t,
                                     new_cx - init_cx + sway_x,
                                     0,
                                     new_cz - init_cz + sway_z))
            # Sway rotation (lag behind primary motion)
            kfs.append(make_keyframe("rotation", t,
                                     5 * math.sin((t / 5.0 + spike_idx * 0.14) * 2 * math.pi),
                                     math.degrees(ang + phase),
                                     8 * math.cos((t / 5.0 + spike_idx * 0.14) * 2 * math.pi)))
        idle[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Ornaments pulse purple emissive (scale pulse)
    for (gu, grp, ang, i) in ornament_groups:
        kfs = []
        for k in range(11):
            t = (k / 10.0) * 5.0
            phase = (t / 5.0) * 2 * math.pi * 0.18
            new_ang = ang + phase
            r = CROWN_RADIUS * 1.05
            init_cx = math.cos(ang) * r
            init_cz = math.sin(ang) * r
            new_cx = math.cos(new_ang) * r
            new_cz = math.sin(new_ang) * r
            kfs.append(make_keyframe("position", t, new_cx - init_cx, 0, new_cz - init_cz))
            ph = (t / 5.0 + i * 0.18) * 2 * math.pi * 2  # faster pulse than the orbit
            s = 1.0 + 0.20 * max(0, math.sin(ph))
            kfs.append(make_keyframe("scale", t, s, s, s))
        idle[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Rust drips drift downward on looping Y positions
    for (gu, grp, ang, dy) in drip_groups:
        kfs = []
        for k in range(11):
            t = (k / 10.0) * 5.0
            ph = (t / 5.0) * 2 * math.pi
            offs_y = -math.sin(ph * 2) * 0.5
            kfs.append(make_keyframe("position", t, 0, offs_y, 0))
        idle[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Ground shockwave breathes
    for (gu, grp, ang) in ground_groups:
        kfs = []
        for k in range(11):
            t = (k / 10.0) * 5.0
            ph = (t / 5.0) * 2 * math.pi
            s = 1.0 + 0.05 * math.sin(ph)
            kfs.append(make_keyframe("scale", t, s, 1, s))
        idle[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("idle", length=5.0, animators=idle, loop="loop", override=False)

    # ============== DISSIPATE — 0.7s — Crown EXPLODES ==============
    diss = {}
    # Chain hangings FLY outward from spike tips
    for (gu, grp, sx, sy, sz, ang, spike_idx) in hanging_groups:
        out_x = math.cos(ang) * 6.0
        out_z = math.sin(ang) * 6.0
        kfs = [
            make_keyframe("scale", 0.00, 1, 1, 1),
            make_keyframe("scale", 0.70, 0, 0, 0),
            make_keyframe("position", 0.00, 0, 0, 0),
            make_keyframe("position", 0.30, out_x * 0.5, 2, out_z * 0.5),
            make_keyframe("position", 0.70, out_x, -2, out_z),
            make_keyframe("rotation", 0.00, 0, math.degrees(ang), 0),
            make_keyframe("rotation", 0.70, 360, math.degrees(ang) + 720, 270),
        ]
        diss[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Spikes collapse inward then scale to 0 — tallest first
    # Sort spike indices by height descending
    spike_order = sorted(range(7), key=lambda i: -SPIKE_HEIGHTS[i])
    spike_collapse_time = {idx: 0.05 + i * 0.08 for i, idx in enumerate(spike_order)}
    for (gu, grp, spike_idx, spike_h, ang) in spike_groups:
        t_coll = spike_collapse_time[spike_idx]
        in_x = -math.cos(ang) * 1.2
        in_z = -math.sin(ang) * 1.2
        kfs = [
            make_keyframe("scale", 0.00, 1, 1, 1),
            make_keyframe("scale", t_coll, 1.1, 0.95, 1.1),
            make_keyframe("scale", t_coll + 0.15, 0.5, 0.4, 0.5),
            make_keyframe("scale", 0.70, 0, 0, 0),
            make_keyframe("position", 0.00, 0, 0, 0),
            make_keyframe("position", t_coll, in_x * 0.6, -0.3, in_z * 0.6),
            make_keyframe("position", t_coll + 0.15, in_x, -0.8, in_z),
            make_keyframe("position", 0.70, in_x * 0.5, -1.5, in_z * 0.5),
        ]
        diss[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Band crumbles
    for (gu, grp, mid_ang, i) in band_groups:
        kfs = [
            make_keyframe("scale", 0.00, 1, 1, 1),
            make_keyframe("scale", 0.40, 1.0, 0.3, 1.0),
            make_keyframe("scale", 0.70, 0, 0, 0),
            make_keyframe("position", 0.00, 0, 0, 0),
            make_keyframe("position", 0.70, 0, -2.0, 0),
        ]
        diss[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Ornaments scatter and fade
    for (gu, grp, ang, i) in ornament_groups:
        scatter_x = math.cos(ang) * 3.5
        scatter_z = math.sin(ang) * 3.5
        kfs = [
            make_keyframe("scale", 0.00, 1, 1, 1),
            make_keyframe("scale", 0.35, 1.6, 1.6, 1.6),
            make_keyframe("scale", 0.70, 0, 0, 0),
            make_keyframe("position", 0.00, 0, 0, 0),
            make_keyframe("position", 0.70, scatter_x, -3, scatter_z),
        ]
        diss[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Rust drips fall
    for (gu, grp, ang, dy) in drip_groups:
        kfs = [
            make_keyframe("scale", 0.00, 1, 1, 1),
            make_keyframe("scale", 0.70, 0, 0, 0),
            make_keyframe("position", 0.00, 0, 0, 0),
            make_keyframe("position", 0.70, 0, -dy, 0),
        ]
        diss[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Ground shockwave expands and snaps
    for (gu, grp, ang) in ground_groups:
        kfs = [
            make_keyframe("scale", 0.00, 1, 1, 1),
            make_keyframe("scale", 0.50, 1.7, 1, 1.7),
            make_keyframe("scale", 0.70, 0, 0, 0),
        ]
        diss[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("dissipate", length=0.7, animators=diss, loop="once", override=True)

    out = b.write(OUTPUT_PATH)
    size = out.stat().st_size
    print(f"WROTE {out} ({size} bytes)")
    return out, size


if __name__ == "__main__":
    build()
