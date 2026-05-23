#!/usr/bin/env python3
"""Generator for chain_shockwave_ring.bbmodel — Rattling (Attack 6).

A ring of CHAIN LINKS explodes outward from the caster. Each link is the
oval-cross construction (4-cube link). Identity = stressed hot iron, warm
orange-gold emissive.

Geometry:
  - Ring 1: 20 chain links at Y=2, starting radius 1, expanding to radius 12
  - Ring 2: 16 smaller links at Y=2.5, radius 0.8 -> 11
  - Ground ring: 12 flat slabs at Y=0
  - 12 flying fragments blasting outward
  - 8 upright shockwave accent slabs
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
    add_chain_link,
    base_fill,
    jitter_inplace,
    rust_blotch,
    horizontal_stress_lines,
    heat_streak,
    chain_link_silhouette,
    radial_glow,
)


OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/chain/me_attacks/chain_shockwave_ring.bbmodel"
RES = 128


def build_rattling_chain_texture():
    """RATTLING CHAIN TECHNIQUE — iron under stress, slightly warm-toned.
    Base #0a0806, stressed metal #302010, shear bright #706040, edge glint #b09060,
    stress emissive #c08030. Heat streaks where the metal is being deformed.
    """
    w = h = RES
    deep_iron      = hex_to_rgba("#0a0806")
    stressed       = hex_to_rgba("#302010")
    shear          = hex_to_rgba("#706040")
    edge_glint     = hex_to_rgba("#b09060")
    stress_emit    = hex_to_rgba("#c08030")
    hot_emit       = hex_to_rgba("#a07020")
    cold_shadow    = hex_to_rgba("#050402")

    pixels = base_fill(w, h, deep_iron)

    # Vertical gradient: top is cooler, bottom warmer (heat collects at base of link)
    for y in range(h):
        t = y / max(1, h - 1)
        if t < 0.25:
            c = color_lerp(cold_shadow, deep_iron, t / 0.25)
        elif t < 0.55:
            c = color_lerp(deep_iron, stressed, (t - 0.25) / 0.30)
        elif t < 0.85:
            c = color_lerp(stressed, shear, (t - 0.55) / 0.30)
        else:
            c = color_lerp(shear, edge_glint, (t - 0.85) / 0.15)
        for x in range(w):
            pixels[y * w + x] = c

    # Add heat shear streaks (diagonal warm emissive lines)
    heat_streak(pixels, w, h, hot_emit, count=14, seed=37)
    heat_streak(pixels, w, h, stress_emit, count=10, seed=71)

    # Horizontal stress lines (shear bright)
    horizontal_stress_lines(pixels, w, h, shear, count=28, seed=43)
    horizontal_stress_lines(pixels, w, h, edge_glint, count=12, seed=57)

    # Random small dark deformation pockets (low areas)
    import random
    rng = random.Random(101)
    for _ in range(40):
        cx = rng.randrange(w)
        cy = rng.randrange(h)
        radius = rng.randint(2, 5)
        for dy in range(-radius, radius + 1):
            for dx in range(-radius, radius + 1):
                if dx * dx + dy * dy > radius * radius:
                    continue
                x, y = (cx + dx) % w, (cy + dy) % h
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, cold_shadow, 0.4)

    # Bright stress-emissive highlight clusters
    for _ in range(28):
        cx = rng.randrange(w)
        cy = rng.randrange(h)
        radius = rng.randint(1, 3)
        for dy in range(-radius, radius + 1):
            for dx in range(-radius, radius + 1):
                if dx * dx + dy * dy > radius * radius:
                    continue
                x, y = (cx + dx) % w, (cy + dy) % h
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, stress_emit, 0.75)

    # Per-pixel jitter for organic stress grain
    jitter_inplace(pixels, jitter_range=22, seed=121)
    jitter_inplace(pixels, jitter_range=14, seed=171, density=0.55)
    add_noise_overlay(pixels, w, h, deep_iron, edge_glint, density=0.08, seed=199)

    return png_from_pixels(pixels, w, h)


def build_ground_ring_texture():
    """Ground ring: dark soil #060604 with bright orange ring line #907030."""
    w = h = RES
    ground       = hex_to_rgba("#060604")
    ring_warm    = hex_to_rgba("#907030")
    ring_bright  = hex_to_rgba("#c0a060")
    ash          = hex_to_rgba("#181208")
    pixels = base_fill(w, h, ground)

    cx, cy = w // 2, h // 2
    # Annular ring: bright at distance ~0.4-0.55 of half-width
    inner_r = w * 0.32
    outer_r = w * 0.50
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            if inner_r <= d <= outer_r:
                t = (d - inner_r) / (outer_r - inner_r)
                # Peak brightness in the middle of the ring
                bell = 1.0 - abs(t - 0.5) * 2.0
                bell = max(0, bell)
                c = color_lerp(ring_warm, ring_bright, bell * bell)
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, c, 0.85)
            elif d < inner_r * 0.85:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, ash, 0.5)

    # Radial rust drip lines
    import random
    rng = random.Random(67)
    for s in range(28):
        ang = (s / 28.0) * 2 * math.pi + rng.uniform(-0.06, 0.06)
        for r in range(int(inner_r * 0.6), int(outer_r * 1.15)):
            x = int(cx + math.cos(ang) * r)
            y = int(cy + math.sin(ang) * r)
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, ring_warm, 0.32)

    add_noise_overlay(pixels, w, h, ash, ring_bright, density=0.10, seed=83)
    jitter_inplace(pixels, jitter_range=18, seed=151)
    jitter_inplace(pixels, jitter_range=10, seed=211, density=0.5)

    return png_from_pixels(pixels, w, h)


def build():
    b = Builder("chain_shockwave_ring", resolution=(RES, RES), visible_box=(18, 6, 0))
    b.add_texture("rattling_chain", build_rattling_chain_texture())
    b.add_texture("ground_ring", build_ground_ring_texture())

    chain_face = lambda: uniform_face(0, 0, RES, RES, tex_index=0)
    ground_face = lambda: uniform_face(0, 0, RES, RES, tex_index=1)

    RING1_Y = 2.0
    RING1_R = 1.0   # starting radius (compressed)
    RING1_FINAL_R = 12.0
    RING1_COUNT = 20

    RING2_Y = 2.5
    RING2_R = 0.8
    RING2_FINAL_R = 11.0
    RING2_COUNT = 16

    # ============== RING 1 — 20 oval-cross chain links ==============
    ring1_groups = []
    for i in range(RING1_COUNT):
        ang = (i / RING1_COUNT) * 2 * math.pi
        cx = math.cos(ang) * RING1_R
        cz = math.sin(ang) * RING1_R
        # Link tangent to circle — rotated so the long axis points along the ring
        link_yaw = math.degrees(ang) + 90.0
        gu, grp = add_chain_link(b, f"r1_link{i+1}", link_radius=0.65, thickness=0.20,
                                  face_factory=chain_face, tex_index=0)
        # Move group origin
        grp["origin"] = [cx, RING1_Y, cz]
        grp["rotation"] = [0, link_yaw, 0]
        ring1_groups.append((gu, grp, ang, RING1_R, RING1_FINAL_R, i))

    # ============== RING 2 — 16 smaller chain links, slightly higher ==============
    ring2_groups = []
    for i in range(RING2_COUNT):
        ang = (i / RING2_COUNT) * 2 * math.pi + math.pi / RING2_COUNT  # offset half-step
        cx = math.cos(ang) * RING2_R
        cz = math.sin(ang) * RING2_R
        link_yaw = math.degrees(ang) + 90.0
        gu, grp = add_chain_link(b, f"r2_link{i+1}", link_radius=0.5, thickness=0.16,
                                  face_factory=chain_face, tex_index=0)
        grp["origin"] = [cx, RING2_Y, cz]
        grp["rotation"] = [math.degrees(ang) * 0.15, link_yaw, 0]
        ring2_groups.append((gu, grp, ang, RING2_R, RING2_FINAL_R, i))

    # ============== GROUND RING — 12 flat slabs ==============
    ground_groups = []
    for i in range(12):
        ang = (i / 12.0) * 2 * math.pi
        r = 6.0  # base position (will scale outward)
        cx = math.cos(ang) * r
        cz = math.sin(ang) * r
        cu = b.add_cube(
            f"gring{i+1}",
            from_=[-1.4, 0.02, -0.55], to_=[1.4, 0.10, 0.55],
            faces=ground_face(),
        )
        gu = make_uuid()
        grp = b.make_group(
            f"ground_ring_{i+1}", [cx, 0.05, cz], [cu],
            rotation=[0, math.degrees(ang), 0], group_uuid=gu,
        )
        ground_groups.append((gu, grp, ang))

    # ============== FLYING FRAGMENTS — 12 small slabs that BLAST outward ==============
    fragment_groups = []
    for i in range(12):
        ang = (i / 12.0) * 2 * math.pi + 0.15
        # Vary altitude
        fy = RING1_Y + math.sin(ang * 3) * 1.5
        # Final direction (where it flies to)
        cu = b.add_cube(
            f"frag{i+1}",
            from_=[-0.55, -0.18, -0.55], to_=[0.55, 0.18, 0.55],
            faces=chain_face(),
        )
        gu = make_uuid()
        grp = b.make_group(
            f"fragment_{i+1}", [0, fy, 0], [cu],
            rotation=[math.degrees(ang) * 0.4, math.degrees(ang), 0],
            group_uuid=gu,
        )
        # Final destination
        fx_final = math.cos(ang) * 14.0
        fz_final = math.sin(ang) * 14.0
        fragment_groups.append((gu, grp, ang, fy, fx_final, fz_final))

    # ============== SHOCKWAVE ACCENTS — 8 upright slabs on outer edge ==============
    accent_groups = []
    for i in range(8):
        ang = (i / 8.0) * 2 * math.pi + math.pi / 16
        cu = b.add_cube(
            f"accent{i+1}",
            from_=[-0.22, 0, -0.22], to_=[0.22, 2.4, 0.22],
            faces=chain_face(),
        )
        gu = make_uuid()
        # Outer position — at base radius 5, will scale outward to ~13 during expansion
        cx = math.cos(ang) * 5.0
        cz = math.sin(ang) * 5.0
        grp = b.make_group(
            f"accent_{i+1}", [cx, 1.4, cz], [cu],
            rotation=[0, math.degrees(ang), 0], group_uuid=gu,
        )
        accent_groups.append((gu, grp, ang))

    # ============== ROOT GROUP ==============
    root_children = []
    for (gu, g, *_r) in ring1_groups: root_children.append(g)
    for (gu, g, *_r) in ring2_groups: root_children.append(g)
    for (gu, g, *_r) in ground_groups: root_children.append(g)
    for (gu, g, *_r) in fragment_groups: root_children.append(g)
    for (gu, g, *_r) in accent_groups: root_children.append(g)
    root_uuid = make_uuid()
    b.outliner.append(b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid))

    # ============== SPAWN — 0.6s — rings BLAST OUTWARD from compressed centre ==============
    spawn = {}
    # Ring 1: scale 0 -> 1.0 over 0.05s-0.45s, position expanding radially.
    # We animate position so the link travels from compressed radius (1.0) to final (12.0).
    for (gu, grp, ang, r0, r_final, i) in ring1_groups:
        init_cx = math.cos(ang) * r0
        init_cz = math.sin(ang) * r0
        final_cx = math.cos(ang) * r_final
        final_cz = math.sin(ang) * r_final
        kfs = [
            make_keyframe("scale",    0.00, 0, 0, 0),
            make_keyframe("scale",    0.05, 0.6, 0.6, 0.6),
            make_keyframe("scale",    0.10, 1.0, 1.0, 1.0),
            make_keyframe("scale",    0.45, 1.0, 1.0, 1.0),
            make_keyframe("scale",    0.60, 0.85, 0.85, 0.85),
            # Position offset from origin (which is at r0)
            make_keyframe("position", 0.00, 0, 0, 0),
            make_keyframe("position", 0.10, (final_cx - init_cx) * 0.15, 0.3, (final_cz - init_cz) * 0.15),
            make_keyframe("position", 0.30, (final_cx - init_cx) * 0.75, 0.05, (final_cz - init_cz) * 0.75),
            make_keyframe("position", 0.45, final_cx - init_cx, 0, final_cz - init_cz),
            make_keyframe("position", 0.60, final_cx - init_cx, 0, final_cz - init_cz),
            # Tumbling rotation as it flies
            make_keyframe("rotation", 0.00, 0, 0, 0),
            make_keyframe("rotation", 0.30, 90, (i * 47) % 360, 60),
            make_keyframe("rotation", 0.60, 180, (i * 47) % 360 + 180, 120),
        ]
        spawn[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Ring 2: same pattern, 0.05s delay
    for (gu, grp, ang, r0, r_final, i) in ring2_groups:
        init_cx = math.cos(ang) * r0
        init_cz = math.sin(ang) * r0
        final_cx = math.cos(ang) * r_final
        final_cz = math.sin(ang) * r_final
        kfs = [
            make_keyframe("scale",    0.00, 0, 0, 0),
            make_keyframe("scale",    0.10, 0.5, 0.5, 0.5),
            make_keyframe("scale",    0.15, 1.0, 1.0, 1.0),
            make_keyframe("scale",    0.50, 1.0, 1.0, 1.0),
            make_keyframe("scale",    0.60, 0.85, 0.85, 0.85),
            make_keyframe("position", 0.00, 0, 0, 0),
            make_keyframe("position", 0.15, (final_cx - init_cx) * 0.15, 0.4, (final_cz - init_cz) * 0.15),
            make_keyframe("position", 0.35, (final_cx - init_cx) * 0.75, 0.1, (final_cz - init_cz) * 0.75),
            make_keyframe("position", 0.50, final_cx - init_cx, 0.05, final_cz - init_cz),
            make_keyframe("position", 0.60, final_cx - init_cx, 0, final_cz - init_cz),
            make_keyframe("rotation", 0.00, 0, 0, 0),
            make_keyframe("rotation", 0.30, 60, (i * 53) % 360, 100),
            make_keyframe("rotation", 0.60, 120, (i * 53) % 360 + 200, 180),
        ]
        spawn[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Ground ring expands at 0.1s
    for (gu, grp, ang) in ground_groups:
        kfs = [
            make_keyframe("scale",    0.00, 0, 0, 0),
            make_keyframe("scale",    0.10, 0.2, 0.2, 0.2),
            make_keyframe("scale",    0.30, 1.3, 1.3, 1.3),
            make_keyframe("scale",    0.50, 1.0, 1.0, 1.0),
            make_keyframe("scale",    0.60, 1.0, 1.0, 1.0),
        ]
        spawn[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Fragments: position keyframes that take each frag from origin (0,fy,0)
    # outward to the final destination, with significant arc
    for (gu, grp, ang, fy, fx_final, fz_final) in fragment_groups:
        kfs = [
            make_keyframe("scale",    0.00, 0, 0, 0),
            make_keyframe("scale",    0.10, 0, 0, 0),
            make_keyframe("scale",    0.15, 1.2, 1.2, 1.2),
            make_keyframe("scale",    0.60, 0.9, 0.9, 0.9),
            make_keyframe("position", 0.00, 0, 0, 0),
            make_keyframe("position", 0.10, 0, 0, 0),
            make_keyframe("position", 0.30, fx_final * 0.45, 1.0, fz_final * 0.45),
            make_keyframe("position", 0.45, fx_final * 0.80, 0.5, fz_final * 0.80),
            make_keyframe("position", 0.60, fx_final, -0.3, fz_final),
            make_keyframe("rotation", 0.00, 0, 0, 0),
            make_keyframe("rotation", 0.30, 180, 180, 90),
            make_keyframe("rotation", 0.60, 360, 540, 270),
        ]
        spawn[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Accents materialise at 0.20s, expand to outer ring position
    for idx, (gu, grp, ang) in enumerate(accent_groups):
        # Final position = radius 13
        init_cx = math.cos(ang) * 5.0
        init_cz = math.sin(ang) * 5.0
        final_cx = math.cos(ang) * 13.0
        final_cz = math.sin(ang) * 13.0
        kfs = [
            make_keyframe("scale",    0.00, 0, 0, 0),
            make_keyframe("scale",    0.20, 0, 0, 0),
            make_keyframe("scale",    0.30, 1.5, 1.5, 1.5),
            make_keyframe("scale",    0.50, 1.0, 1.0, 1.0),
            make_keyframe("position", 0.00, 0, 0, 0),
            make_keyframe("position", 0.20, 0, 0, 0),
            make_keyframe("position", 0.40, (final_cx - init_cx) * 0.7, 0.4, (final_cz - init_cz) * 0.7),
            make_keyframe("position", 0.55, final_cx - init_cx, 0, final_cz - init_cz),
            make_keyframe("position", 0.60, final_cx - init_cx, 0, final_cz - init_cz),
        ]
        spawn[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("spawn", length=0.6, animators=spawn, loop="once", override=True)

    # ============== IDLE — 3.0s — rings counter-rotate, links tumble ==============
    idle = {}
    # Ring 1 links: each link tumbles on its own axis + the ring slowly rotates (orbital sweep)
    for (gu, grp, ang, r0, r_final, i) in ring1_groups:
        kfs = []
        # Tumbling rotation — each link rotates differently
        for k in range(7):
            t = (k / 6.0) * 3.0
            rx = (t / 3.0) * 360 * (1 if i % 2 == 0 else -1)
            ry = math.degrees(ang) + 90 + (t / 3.0) * 60
            rz = (t / 3.0) * 180 * (1 if i % 3 == 0 else -1)
            kfs.append(make_keyframe("rotation", t, rx, ry, rz))
        # Orbital sweep: very slow rotation around centre
        for k in range(5):
            t = (k / 4.0) * 3.0
            # rotate by ang_offset over time
            phase = (t / 3.0) * 2 * math.pi * 0.25
            new_ang = ang + phase
            init_cx = math.cos(ang) * r_final
            init_cz = math.sin(ang) * r_final
            new_cx = math.cos(new_ang) * r_final
            new_cz = math.sin(new_ang) * r_final
            kfs.append(make_keyframe("position", t, new_cx - init_cx, 0, new_cz - init_cz))
        idle[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Ring 2: counter-rotate
    for (gu, grp, ang, r0, r_final, i) in ring2_groups:
        kfs = []
        for k in range(7):
            t = (k / 6.0) * 3.0
            rx = (t / 3.0) * -360 * (1 if i % 2 == 0 else -1)
            ry = math.degrees(ang) + 90 - (t / 3.0) * 60
            rz = (t / 3.0) * -180
            kfs.append(make_keyframe("rotation", t, rx, ry, rz))
        for k in range(5):
            t = (k / 4.0) * 3.0
            phase = -(t / 3.0) * 2 * math.pi * 0.25
            new_ang = ang + phase
            init_cx = math.cos(ang) * r_final
            init_cz = math.sin(ang) * r_final
            new_cx = math.cos(new_ang) * r_final
            new_cz = math.sin(new_ang) * r_final
            kfs.append(make_keyframe("position", t, new_cx - init_cx, 0.1 * math.sin(phase * 2), new_cz - init_cz))
        idle[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Accents pulse scale
    for idx, (gu, grp, ang) in enumerate(accent_groups):
        kfs = []
        for k in range(7):
            t = (k / 6.0) * 3.0
            ph = (t / 3.0 + idx * 0.13) * 2 * math.pi
            s = 1.0 + 0.12 * math.sin(ph)
            kfs.append(make_keyframe("scale", t, s, s, s))
        idle[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Ground ring: breathe
    for idx, (gu, grp, ang) in enumerate(ground_groups):
        kfs = []
        for k in range(7):
            t = (k / 6.0) * 3.0
            ph = (t / 3.0 + idx * 0.08) * 2 * math.pi
            s = 1.0 + 0.06 * math.sin(ph)
            kfs.append(make_keyframe("scale", t, s, 1, s))
        idle[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("idle", length=3.0, animators=idle, loop="loop", override=False)

    # ============== DISSIPATE — 0.5s ==============
    diss = {}
    for (gu, grp, ang, r0, r_final, i) in ring1_groups:
        init_cx = math.cos(ang) * r_final
        init_cz = math.sin(ang) * r_final
        kfs = [
            make_keyframe("scale",    0.00, 1, 1, 1),
            make_keyframe("scale",    0.30, 1.4, 1.4, 1.4),
            make_keyframe("scale",    0.50, 0, 0, 0),
            make_keyframe("position", 0.00, 0, 0, 0),
            make_keyframe("position", 0.50, init_cx * 0.4, 0, init_cz * 0.4),
        ]
        diss[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}
    for (gu, grp, ang, r0, r_final, i) in ring2_groups:
        init_cx = math.cos(ang) * r_final
        init_cz = math.sin(ang) * r_final
        kfs = [
            make_keyframe("scale",    0.00, 1, 1, 1),
            make_keyframe("scale",    0.30, 1.4, 1.4, 1.4),
            make_keyframe("scale",    0.50, 0, 0, 0),
            make_keyframe("position", 0.00, 0, 0, 0),
            make_keyframe("position", 0.50, init_cx * 0.4, 0, init_cz * 0.4),
        ]
        diss[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}
    for (gu, grp, ang, fy, fx_final, fz_final) in fragment_groups:
        kfs = [
            make_keyframe("scale",    0.00, 1, 1, 1),
            make_keyframe("scale",    0.50, 0, 0, 0),
            make_keyframe("position", 0.00, 0, 0, 0),
            make_keyframe("position", 0.50, fx_final * 0.4, -1.0, fz_final * 0.4),
        ]
        diss[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}
    for (gu, grp, ang) in accent_groups:
        kfs = [
            make_keyframe("scale", 0.00, 1, 1, 1),
            make_keyframe("scale", 0.25, 1.6, 0.4, 1.6),
            make_keyframe("scale", 0.50, 0, 0, 0),
        ]
        diss[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}
    for (gu, grp, ang) in ground_groups:
        kfs = [
            make_keyframe("scale", 0.00, 1, 1, 1),
            make_keyframe("scale", 0.30, 1.5, 1, 1.5),
            make_keyframe("scale", 0.50, 0, 0, 0),
        ]
        diss[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("dissipate", length=0.5, animators=diss, loop="once", override=True)

    out = b.write(OUTPUT_PATH)
    size = out.stat().st_size
    print(f"WROTE {out} ({size} bytes)")
    return out, size


if __name__ == "__main__":
    build()
