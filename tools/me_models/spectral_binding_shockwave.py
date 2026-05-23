#!/usr/bin/env python3
"""Generator for spectral_binding_shockwave.bbmodel — Unfinished Business (Attack 8).

A SLOW SAD shockwave of spectral chain energy. Silver-ghost translucent rings
expanding slowly — has been trying to reach this far for a long time.
Identity = HAUNTING (not explosion), SADNESS, ghost-iron.

Geometry:
  - 3 concentric spectral rings at Y=1 (14 upright thin slabs each, radius 4, 8, 12)
  - 12 spectral chain wisps (2 ghost links each) floating at various heights
  - Origin point: 4-cube ghost-sphere at centre Y=4
  - 2 trailing echo rings at Y=0.5 and Y=2 (smaller radii, faint)
  - 6 ground impression slabs at Y=0 (tex 1)
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
    chain_link_silhouette,
    radial_glow,
)


OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/chain/me_attacks/spectral_binding_shockwave.bbmodel"
RES = 128


def build_spectral_echo_texture():
    """SPECTRAL ECHO TECHNIQUE — ghost-iron, brighter at edges, darker centre.
    Base #040810, ghost surface #102030, spectral bright #304858,
    silver ghost edge #708098, emissive #a0b8c8. Soft inner glow.
    """
    w = h = RES
    spectral_navy = hex_to_rgba("#040810")
    ghost_surf    = hex_to_rgba("#102030")
    spec_bright   = hex_to_rgba("#304858")
    silver_edge   = hex_to_rgba("#708098")
    emissive      = hex_to_rgba("#a0b8c8")
    super_silver  = hex_to_rgba("#c0d8e8")

    pixels = base_fill(w, h, spectral_navy)

    cx, cy = w // 2, h // 2
    # Reversed radial gradient — darker centre, brighter edge (ghost manifests at edges)
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            max_r = w * 0.62
            t = min(1.0, d / max_r)
            if t < 0.30:
                c = color_lerp(spectral_navy, ghost_surf, t / 0.30)
            elif t < 0.65:
                c = color_lerp(ghost_surf, spec_bright, (t - 0.30) / 0.35)
            elif t < 0.92:
                c = color_lerp(spec_bright, silver_edge, (t - 0.65) / 0.27)
            else:
                c = color_lerp(silver_edge, emissive, (t - 0.92) / 0.08)
            pixels[y * w + x] = c

    # Soft horizontal wisps — ghost trails
    import random
    rng = random.Random(73)
    for _ in range(22):
        y = rng.randrange(h)
        length = rng.randint(w // 3, w)
        x_start = rng.randrange(0, max(1, w - length // 2))
        for x in range(x_start, min(w, x_start + length)):
            for dy in (-1, 0, 1):
                yy = y + dy
                if 0 <= yy < h:
                    p = pixels[yy * w + x]
                    intensity = 0.4 if dy == 0 else 0.20
                    pixels[yy * w + x] = color_lerp(p, emissive, intensity * rng.uniform(0.5, 1.0))

    # Soft chain-link silhouettes (faint, ghost-like)
    chain_link_silhouette(pixels, w, h, silver_edge, super_silver, count=6, seed=43)

    # Faint emissive ring traces (echoes)
    for ring_r in [22, 38, 54]:
        for ang_step in range(360):
            ang = math.radians(ang_step)
            x = int(cx + math.cos(ang) * ring_r)
            y = int(cy + math.sin(ang) * ring_r)
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, emissive, 0.32)
                # Soft halo
                for dy in (-1, 1):
                    yy = y + dy
                    if 0 <= yy < w:
                        p2 = pixels[yy * w + x]
                        pixels[yy * w + x] = color_lerp(p2, emissive, 0.18)

    # Sparse silver sparkles
    for _ in range(150):
        x = rng.randrange(w)
        y = rng.randrange(h)
        if rng.random() < 0.6:
            p = pixels[y * w + x]
            pixels[y * w + x] = color_lerp(p, super_silver, rng.uniform(0.3, 0.7))

    jitter_inplace(pixels, jitter_range=14, seed=137)
    jitter_inplace(pixels, jitter_range=10, seed=183, density=0.5)
    add_noise_overlay(pixels, w, h, ghost_surf, super_silver, density=0.08, seed=227)

    return png_from_pixels(pixels, w, h)


def build_ground_impression_texture():
    """Ground impression / echo rings: near-black #040608 with extremely faint
    spectral silver ring traces #304050."""
    w = h = RES
    near_black   = hex_to_rgba("#040608")
    faint_trace  = hex_to_rgba("#304050")
    ghost_mid    = hex_to_rgba("#506070")
    pixels = base_fill(w, h, near_black)

    cx, cy = w // 2, h // 2
    # Multiple concentric extremely-faint ring traces — overlapping passes
    for ring_r in [14, 22, 30, 38, 46, 54]:
        intensity_base = 0.45 - (ring_r / 70.0)
        for ang_step in range(360):
            ang = math.radians(ang_step)
            for offs in (-1, 0, 1):
                rr = ring_r + offs
                x = int(cx + math.cos(ang) * rr)
                y = int(cy + math.sin(ang) * rr)
                if 0 <= x < w and 0 <= y < h:
                    p = pixels[y * w + x]
                    intensity = intensity_base if offs == 0 else intensity_base * 0.55
                    target = faint_trace if ring_r > 32 else ghost_mid
                    pixels[y * w + x] = color_lerp(p, target, intensity)

    # Soft cross marks (where the rings have intersected before)
    import random
    rng = random.Random(89)
    for _ in range(30):
        cx0 = rng.randrange(w)
        cy0 = rng.randrange(h)
        size = rng.randint(2, 4)
        for d in range(-size, size + 1):
            xx, yy = cx0 + d, cy0
            if 0 <= xx < w and 0 <= yy < h:
                p = pixels[yy * w + xx]
                pixels[yy * w + xx] = color_lerp(p, faint_trace, 0.35)
            xx, yy = cx0, cy0 + d
            if 0 <= xx < w and 0 <= yy < h:
                p = pixels[yy * w + xx]
                pixels[yy * w + xx] = color_lerp(p, faint_trace, 0.35)

    add_noise_overlay(pixels, w, h, near_black, ghost_mid, density=0.10, seed=121)
    jitter_inplace(pixels, jitter_range=10, seed=171)
    jitter_inplace(pixels, jitter_range=7, seed=221, density=0.5)

    return png_from_pixels(pixels, w, h)


def build():
    b = Builder("spectral_binding_shockwave", resolution=(RES, RES), visible_box=(18, 8, 0))
    b.add_texture("spectral", build_spectral_echo_texture())
    b.add_texture("ground_impression", build_ground_impression_texture())

    spec_face = lambda: uniform_face(0, 0, RES, RES, tex_index=0)
    ground_face = lambda: uniform_face(0, 0, RES, RES, tex_index=1)

    RING_Y = 1.0
    RING_RADII = [4.0, 8.0, 12.0]
    RING_COUNT = 14

    # ============== 3 CONCENTRIC SPECTRAL RINGS (upright thin slabs) ==============
    ring_groups = []
    for ring_idx, ring_r in enumerate(RING_RADII):
        for i in range(RING_COUNT):
            ang = (i / RING_COUNT) * 2 * math.pi + (ring_idx * math.pi / RING_COUNT)
            cx = math.cos(ang) * ring_r
            cz = math.sin(ang) * ring_r
            cu = b.add_cube(
                f"ring{ring_idx+1}_{i+1}",
                from_=[-0.18, 0, -0.18], to_=[0.18, 2.2, 0.18],
                faces=spec_face(),
            )
            gu = make_uuid()
            grp = b.make_group(
                f"spectral_ring_{ring_idx+1}_{i+1}",
                [cx, RING_Y, cz], [cu],
                rotation=[0, math.degrees(ang), 0], group_uuid=gu,
            )
            ring_groups.append((gu, grp, ring_idx, ring_r, ang, i))

    # ============== SPECTRAL CHAIN WISPS — 12 wisps, 2 ghost links each, drifting ==============
    wisp_groups = []
    import random
    rng = random.Random(177)
    for w_idx in range(12):
        # Place wisp at semi-random angle and radius within ring area
        ang = (w_idx / 12.0) * 2 * math.pi + rng.uniform(-0.15, 0.15)
        radius = rng.uniform(4.5, 11.5)
        height = rng.uniform(1.5, 6.0)
        cx = math.cos(ang) * radius
        cz = math.sin(ang) * radius

        # Each wisp = 2 small chain links offset along a slight axis
        # Link 1
        link1_top = b.add_cube(f"wisp{w_idx+1}_l1_top",
                               from_=[-0.32, 0.15, -0.10], to_=[0.32, 0.30, 0.10],
                               faces=spec_face())
        link1_bot = b.add_cube(f"wisp{w_idx+1}_l1_bot",
                               from_=[-0.32, -0.30, -0.10], to_=[0.32, -0.15, 0.10],
                               faces=spec_face())
        link1_lf = b.add_cube(f"wisp{w_idx+1}_l1_lf",
                              from_=[-0.42, -0.20, -0.10], to_=[-0.22, 0.20, 0.10],
                              faces=spec_face())
        link1_rt = b.add_cube(f"wisp{w_idx+1}_l1_rt",
                              from_=[0.22, -0.20, -0.10], to_=[0.42, 0.20, 0.10],
                              faces=spec_face())
        # Link 2 — perpendicular orientation (next link in chain)
        link2_top = b.add_cube(f"wisp{w_idx+1}_l2_top",
                               from_=[-0.10, 0.50, -0.30], to_=[0.10, 0.65, 0.30],
                               faces=spec_face())
        link2_bot = b.add_cube(f"wisp{w_idx+1}_l2_bot",
                               from_=[-0.10, 0.20, -0.30], to_=[0.10, 0.35, 0.30],
                               faces=spec_face())
        link2_fr = b.add_cube(f"wisp{w_idx+1}_l2_fr",
                              from_=[-0.10, 0.30, 0.22], to_=[0.10, 0.55, 0.42],
                              faces=spec_face())
        link2_bk = b.add_cube(f"wisp{w_idx+1}_l2_bk",
                              from_=[-0.10, 0.30, -0.42], to_=[0.10, 0.55, -0.22],
                              faces=spec_face())

        gu = make_uuid()
        grp = b.make_group(
            f"wisp_{w_idx+1}", [cx, height, cz],
            [link1_top, link1_bot, link1_lf, link1_rt,
             link2_top, link2_bot, link2_fr, link2_bk],
            rotation=[rng.uniform(-15, 15), math.degrees(ang) + 90, rng.uniform(-15, 15)],
            group_uuid=gu,
        )
        # Drift parameters for idle
        drift_dx = rng.uniform(-0.8, 0.8)
        drift_dy = rng.uniform(-0.4, 0.4)
        drift_dz = rng.uniform(-0.8, 0.8)
        wisp_groups.append((gu, grp, ang, radius, height, drift_dx, drift_dy, drift_dz, w_idx))

    # ============== ORIGIN POINT — 4-cube ghost sphere at centre Y=4 ==============
    origin_groups = []
    origin_cubes = []
    for i in range(4):
        rot_y = i * 45
        cu = b.add_cube(
            f"origin_{i+1}",
            from_=[-0.7, -0.7, -0.7], to_=[0.7, 0.7, 0.7],
            faces=spec_face(),
        )
        origin_cubes.append(cu)
    gu = make_uuid()
    grp = b.make_group(
        "origin_sphere", [0, 4.0, 0], origin_cubes, group_uuid=gu,
    )
    origin_groups.append((gu, grp))

    # ============== TRAILING ECHO RINGS — 2 faint smaller rings ==============
    echo_groups = []
    echo_specs = [(0.5, 3.0), (2.0, 6.5)]  # (y, radius)
    for echo_idx, (ey, er) in enumerate(echo_specs):
        for i in range(10):
            ang = (i / 10.0) * 2 * math.pi + echo_idx * math.pi / 10
            cx = math.cos(ang) * er
            cz = math.sin(ang) * er
            cu = b.add_cube(
                f"echo{echo_idx+1}_{i+1}",
                from_=[-0.12, 0, -0.12], to_=[0.12, 1.4, 0.12],
                faces=ground_face(),
            )
            gu = make_uuid()
            grp = b.make_group(
                f"echo_{echo_idx+1}_{i+1}",
                [cx, ey, cz], [cu],
                rotation=[0, math.degrees(ang), 0], group_uuid=gu,
            )
            echo_groups.append((gu, grp, echo_idx, ang, er))

    # ============== GROUND IMPRESSION — 6 flat slabs at Y=0 ==============
    impression_groups = []
    for i in range(6):
        ang = (i / 6.0) * 2 * math.pi
        cx = math.cos(ang) * 9.0
        cz = math.sin(ang) * 9.0
        cu = b.add_cube(
            f"imp_{i+1}",
            from_=[-2.0, 0.02, -0.5], to_=[2.0, 0.08, 0.5],
            faces=ground_face(),
        )
        gu = make_uuid()
        grp = b.make_group(
            f"impression_{i+1}", [cx, 0.04, cz], [cu],
            rotation=[0, math.degrees(ang), 0], group_uuid=gu,
        )
        impression_groups.append((gu, grp, ang))

    # ============== ROOT ==============
    root_children = []
    for (gu, g, *_r) in ring_groups: root_children.append(g)
    for (gu, g, *_r) in wisp_groups: root_children.append(g)
    for (gu, g) in origin_groups: root_children.append(g)
    for (gu, g, *_r) in echo_groups: root_children.append(g)
    for (gu, g, *_r) in impression_groups: root_children.append(g)
    root_uuid = make_uuid()
    b.outliner.append(b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid))

    # ============== SPAWN — 1.0s — SLOW haunting expansion (half normal speed) ==============
    spawn = {}
    # Origin: fades in slowly from 0.0s (not snapping)
    for (gu, grp) in origin_groups:
        kfs = [
            make_keyframe("scale", 0.00, 0.1, 0.1, 0.1),
            make_keyframe("scale", 0.20, 0.5, 0.5, 0.5),
            make_keyframe("scale", 0.50, 0.9, 0.9, 0.9),
            make_keyframe("scale", 1.00, 1.0, 1.0, 1.0),
        ]
        spawn[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Echo rings appear at 0.2s at smaller radii
    for (gu, grp, echo_idx, ang, er) in echo_groups:
        t_start = 0.20 + echo_idx * 0.05
        kfs = [
            make_keyframe("scale", 0.00, 0, 0, 0),
            make_keyframe("scale", t_start, 0, 0, 0),
            make_keyframe("scale", t_start + 0.20, 0.7, 0.7, 0.7),
            make_keyframe("scale", t_start + 0.40, 1.0, 1.0, 1.0),
            make_keyframe("scale", 1.00, 1.0, 1.0, 1.0),
        ]
        spawn[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Main rings: expand from origin outward at 0.3s, SLOWLY
    # Each ring starts at radius 0 (visually compressed to origin) and grows to its final radius
    for (gu, grp, ring_idx, ring_r, ang, i) in ring_groups:
        # Position offset: animate from origin (0, RING_Y, 0) to (cos*r, RING_Y, sin*r)
        init_cx = math.cos(ang) * ring_r
        init_cz = math.sin(ang) * ring_r
        # Offset from origin position back to actual position is (0 - cx, 0 - cz) at t=0
        offs_x = -init_cx
        offs_z = -init_cz
        t_start = 0.30 + ring_idx * 0.05
        kfs = [
            make_keyframe("scale", 0.00, 0.4, 0.4, 0.4),
            make_keyframe("scale", t_start, 0.6, 0.6, 0.6),
            make_keyframe("scale", t_start + 0.30, 1.0, 1.0, 1.0),
            make_keyframe("scale", 1.00, 1.0, 1.0, 1.0),
            make_keyframe("position", 0.00, offs_x, 3, offs_z),
            make_keyframe("position", t_start, offs_x * 0.85, 1.0, offs_z * 0.85),
            make_keyframe("position", t_start + 0.30, offs_x * 0.45, 0.3, offs_z * 0.45),
            make_keyframe("position", t_start + 0.55, offs_x * 0.20, 0.1, offs_z * 0.20),
            make_keyframe("position", 1.00, 0, 0, 0),
        ]
        spawn[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Chain wisps drift into position at 0.4s
    for (gu, grp, ang, radius, height, dx, dy, dz, w_idx) in wisp_groups:
        t_start = 0.40 + (w_idx % 5) * 0.05
        kfs = [
            make_keyframe("scale", 0.00, 0, 0, 0),
            make_keyframe("scale", t_start, 0, 0, 0),
            make_keyframe("scale", t_start + 0.25, 1.0, 1.0, 1.0),
            make_keyframe("scale", 1.00, 1.0, 1.0, 1.0),
            make_keyframe("position", 0.00, dx * 2, dy * 2, dz * 2),
            make_keyframe("position", t_start, dx * 1.5, dy * 1.5, dz * 1.5),
            make_keyframe("position", t_start + 0.30, dx * 0.5, dy * 0.5, dz * 0.5),
            make_keyframe("position", 1.00, 0, 0, 0),
        ]
        spawn[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Ground impression appears at 0.5s
    for (gu, grp, ang) in impression_groups:
        kfs = [
            make_keyframe("scale", 0.00, 0, 0, 0),
            make_keyframe("scale", 0.50, 0, 0, 0),
            make_keyframe("scale", 0.75, 1.3, 1, 1.3),
            make_keyframe("scale", 1.00, 1.0, 1.0, 1.0),
        ]
        spawn[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("spawn", length=1.0, animators=spawn, loop="once", override=True)

    # ============== IDLE — 6.0s — rings rotate slowly, wisps drift independently ==============
    idle = {}
    ring_rates = [0.18, -0.14, 0.10]  # very slow, different per ring
    for (gu, grp, ring_idx, ring_r, ang, i) in ring_groups:
        rate = ring_rates[ring_idx]
        kfs = []
        for k in range(13):
            t = (k / 12.0) * 6.0
            phase = (t / 6.0) * 2 * math.pi * rate
            new_ang = ang + phase
            init_cx = math.cos(ang) * ring_r
            init_cz = math.sin(ang) * ring_r
            new_cx = math.cos(new_ang) * ring_r
            new_cz = math.sin(new_ang) * ring_r
            kfs.append(make_keyframe("position", t, new_cx - init_cx, 0.1 * math.sin(phase * 2), new_cz - init_cz))
        idle[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Chain wisps drift independently — each on its own path
    for (gu, grp, ang, radius, height, dx, dy, dz, w_idx) in wisp_groups:
        kfs = []
        for k in range(13):
            t = (k / 12.0) * 6.0
            # Drift pattern is figure-8 / wandering
            phase_a = (t / 6.0) * 2 * math.pi + w_idx * 0.5
            phase_b = (t / 6.0) * 2 * math.pi * 1.3 + w_idx * 0.7
            offset_x = math.sin(phase_a) * 0.8 + math.cos(phase_b) * 0.3
            offset_y = math.sin(phase_b) * 0.5
            offset_z = math.cos(phase_a) * 0.8 + math.sin(phase_b * 0.7) * 0.3
            kfs.append(make_keyframe("position", t, offset_x, offset_y, offset_z))
            # Slow rotation
            kfs.append(make_keyframe("rotation", t,
                                     (t / 6.0) * 60 * (1 if w_idx % 2 == 0 else -1),
                                     math.degrees(ang) + 90 + (t / 6.0) * 90,
                                     (t / 6.0) * 30 * (1 if w_idx % 3 == 0 else -1)))
        idle[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Origin pulses slowly
    for (gu, grp) in origin_groups:
        kfs = []
        for k in range(13):
            t = (k / 12.0) * 6.0
            ph = (t / 6.0) * 2 * math.pi
            s = 1.0 + 0.15 * math.sin(ph)
            kfs.append(make_keyframe("scale", t, s, s, s))
        idle[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Echo rings fade in/out on long periods (scale-based fading)
    for (gu, grp, echo_idx, ang, er) in echo_groups:
        kfs = []
        for k in range(13):
            t = (k / 12.0) * 6.0
            ph = (t / 6.0 + echo_idx * 0.4) * 2 * math.pi
            s = 0.6 + 0.4 * (0.5 + 0.5 * math.sin(ph))
            kfs.append(make_keyframe("scale", t, s, s, s))
        idle[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Ground impression breathes
    for (gu, grp, ang) in impression_groups:
        kfs = []
        for k in range(13):
            t = (k / 12.0) * 6.0
            ph = (t / 6.0) * 2 * math.pi
            s = 1.0 + 0.05 * math.sin(ph)
            kfs.append(make_keyframe("scale", t, s, 1, s))
        idle[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("idle", length=6.0, animators=idle, loop="loop", override=False)

    # ============== DISSIPATE — 0.8s — slow fade (inner to outer) ==============
    diss = {}
    # Wisps fade one by one
    for (gu, grp, ang, radius, height, dx, dy, dz, w_idx) in wisp_groups:
        t_fade = 0.05 + (w_idx / 12.0) * 0.50  # staggered fade
        kfs = [
            make_keyframe("scale", 0.00, 1, 1, 1),
            make_keyframe("scale", t_fade, 1, 1, 1),
            make_keyframe("scale", t_fade + 0.15, 0, 0, 0),
            make_keyframe("scale", 0.80, 0, 0, 0),
        ]
        diss[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Rings: inner first, then outer (gradual fade, not snap)
    for (gu, grp, ring_idx, ring_r, ang, i) in ring_groups:
        t_fade = 0.10 + ring_idx * 0.20
        kfs = [
            make_keyframe("scale", 0.00, 1, 1, 1),
            make_keyframe("scale", t_fade, 1, 1, 1),
            make_keyframe("scale", t_fade + 0.30, 1.0, 0.3, 1.0),
            make_keyframe("scale", 0.80, 0, 0, 0),
        ]
        diss[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Echo rings fade with rings
    for (gu, grp, echo_idx, ang, er) in echo_groups:
        kfs = [
            make_keyframe("scale", 0.00, 0.8, 0.8, 0.8),
            make_keyframe("scale", 0.30, 0.5, 0.5, 0.5),
            make_keyframe("scale", 0.80, 0, 0, 0),
        ]
        diss[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Origin is last
    for (gu, grp) in origin_groups:
        kfs = [
            make_keyframe("scale", 0.00, 1, 1, 1),
            make_keyframe("scale", 0.60, 1.0, 1.0, 1.0),
            make_keyframe("scale", 0.80, 0, 0, 0),
        ]
        diss[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Ground impression fades with everything
    for (gu, grp, ang) in impression_groups:
        kfs = [
            make_keyframe("scale", 0.00, 1, 1, 1),
            make_keyframe("scale", 0.80, 0, 0, 0),
        ]
        diss[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("dissipate", length=0.8, animators=diss, loop="once", override=True)

    out = b.write(OUTPUT_PATH)
    size = out.stat().st_size
    print(f"WROTE {out} ({size} bytes)")
    return out, size


if __name__ == "__main__":
    build()
