#!/usr/bin/env python3
"""Generator for anchor_eruption.bbmodel — Chain Mode "Dead Weight".

A massive rusted ship anchor erupting from below the ground on a length of
heavy industrial chain. The anchor snaps taut, the chain doesn't sway, the
weight communicates itself through deliberate slow motion.

Bone hierarchy:
  - 4 anchor shank segments (decreasing width top -> bottom)
  - 2 crossbar slab bones
  - 4 fluke bones (2 per side, angled outward at the shank base)
  - 6 anchor ring slabs (top of shank)
  - 8 chain link groups (2 cubes each, oval-cross)
  - 10 ground rupture slabs (Y=0 radiating)
  - 6 chain tension dust pieces
"""

import sys
import math
import os

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
    add_noise_overlay,
)
from _chain_helpers import (
    base_fill,
    jitter_inplace,
    rust_patches,
    hammer_marks,
    horizontal_grain,
    crack_lines_radial,
    specular_cluster,
    shifted_face,
)

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/chain/me_attacks/anchor_eruption.bbmodel"
RES = 128


# ---------------------------------------------------------------------------
# TEXTURE 0 — HEAVY RUST IRON (anchor body / chain links)
# ---------------------------------------------------------------------------

def build_rust_iron_texture():
    w = h = RES
    iron_black  = hex_to_rgba("#0e0a08")
    rust_dark   = hex_to_rgba("#3a1808")
    rust_mid    = hex_to_rgba("#6a2c10")
    rust_hi     = hex_to_rgba("#a04818")
    edge_shine  = hex_to_rgba("#c8701e")
    top_edge    = hex_to_rgba("#d88030")
    iron_silver = hex_to_rgba("#b09068")

    pixels = base_fill(w, h, iron_black)

    # Vertical stratification: top bright (edge catches light), deep dark at bottom
    for y in range(h):
        t = y / (h - 1)
        if t < 0.04:
            c = top_edge
        elif t < 0.12:
            local = (t - 0.04) / 0.08
            c = color_lerp(top_edge, edge_shine, local)
        elif t < 0.32:
            local = (t - 0.12) / 0.20
            c = color_lerp(edge_shine, rust_hi, local)
        elif t < 0.55:
            local = (t - 0.32) / 0.23
            c = color_lerp(rust_hi, rust_mid, local)
        elif t < 0.78:
            local = (t - 0.55) / 0.23
            c = color_lerp(rust_mid, rust_dark, local)
        else:
            local = (t - 0.78) / 0.22
            c = color_lerp(rust_dark, iron_black, local)
        for x in range(w):
            pixels[y * w + x] = c

    # Heavy organic rust patches — deeply embedded
    rust_patches(pixels, w, h, rust_mid, rust_hi, count=22, seed=11)
    rust_patches(pixels, w, h, rust_dark, rust_mid, count=18, seed=37)
    rust_patches(pixels, w, h, iron_black, rust_dark, count=12, seed=63)

    # Faint horizontal grain — forged iron direction
    horizontal_grain(pixels, w, h, rust_dark, density=0.45, seed=51)

    # Single specular at (8 * RES/64, 4 * RES/64) = (16, 8) for 128 — iron-silver glint
    specular_cluster(pixels, w, h, RES // 8, RES // 16, 5, iron_silver, strength=0.78)

    # Tiny additional speculars on top edge
    specular_cluster(pixels, w, h, RES // 2 + 12, 6, 3, top_edge, strength=0.6)
    specular_cluster(pixels, w, h, RES - 18, 10, 3, edge_shine, strength=0.55)

    # No emissive — heavy non-glowing metal
    add_noise_overlay(pixels, w, h, iron_black, rust_dark, density=0.20, seed=99)
    jitter_inplace(pixels, jitter_range=20, seed=303)
    jitter_inplace(pixels, jitter_range=12, seed=505, density=0.45)

    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# TEXTURE 1 — BROKEN GROUND (rupture / dust / anchor ring)
# ---------------------------------------------------------------------------

def build_broken_ground_texture():
    w = h = RES
    earth_dark  = hex_to_rgba("#080604")
    crack_rust  = hex_to_rgba("#7a2c0c")
    rust_stain  = hex_to_rgba("#4a1c08")
    deep_black  = hex_to_rgba("#040202")

    pixels = base_fill(w, h, earth_dark)

    # Subtle radial darkness — centre slightly darker
    for y in range(h):
        for x in range(w):
            dx = x - w / 2
            dy = y - h / 2
            d = math.sqrt(dx * dx + dy * dy)
            t = min(1.0, d / (w * 0.62))
            pixels[y * w + x] = color_lerp(deep_black, earth_dark, t)

    # Rust-orange crack lines radiating from the centre (the impact point)
    crack_lines_radial(pixels, w, h, w // 2, h // 2, branches=11, color=crack_rust, seed=22)
    crack_lines_radial(pixels, w, h, w // 2, h // 2, branches=7, color=rust_stain, seed=45)

    # Random rust stain blobs (where the chain passed through and stained the earth)
    rust_patches(pixels, w, h, rust_stain, crack_rust, count=14, seed=71)

    add_noise_overlay(pixels, w, h, deep_black, rust_stain, density=0.18, seed=83)
    jitter_inplace(pixels, jitter_range=18, seed=271)
    jitter_inplace(pixels, jitter_range=10, seed=313, density=0.5)

    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# MODEL BUILD
# ---------------------------------------------------------------------------

def build():
    b = Builder("anchor_eruption", resolution=(RES, RES), visible_box=(14, 12, 0))
    b.add_texture("rust_iron", build_rust_iron_texture())
    b.add_texture("broken_ground", build_broken_ground_texture())

    # Use shifted_face for JSON bulk per cube — every cube has unique UV across faces
    def iron_face(seed):
        return shifted_face(seed % 100, (seed * 7) % 100, 18, tex_index=0, RES=RES)
    def ground_face(seed):
        return shifted_face(seed % 100, (seed * 11) % 100, 16, tex_index=1, RES=RES)

    # =====================================================================
    # 1) ANCHOR SHANK — 4 stacked cube segments, widest at top
    # =====================================================================
    shank_groups = []
    shank_data = [
        # (y0, y1, hw, hd)
        (3.0, 5.0, 0.95, 0.55),   # top — widest
        (1.5, 3.0, 0.80, 0.48),
        (0.0, 1.5, 0.65, 0.40),
        (-1.4, 0.0, 0.52, 0.32),  # bottom — narrowest
    ]
    for i, (y0, y1, hw, hd) in enumerate(shank_data):
        cu = b.add_cube(
            f"shank_seg_{i+1}",
            from_=[-hw, y0, -hd], to_=[hw, y1, hd],
            faces=iron_face(20 + i * 13),
        )
        gu = make_uuid()
        g = b.make_group(
            f"anchor_shank_{i+1}", [0, (y0 + y1) / 2, 0], [cu], group_uuid=gu,
        )
        shank_groups.append((gu, g))

    # =====================================================================
    # 2) CROSSBAR — 2 wide flat slabs forming T-bar top
    # =====================================================================
    crossbar_groups = []
    # left half
    cu_l = b.add_cube(
        "crossbar_left",
        from_=[-4.5, 4.5, -0.5], to_=[-0.1, 5.6, 0.5],
        faces=iron_face(80),
    )
    gu = make_uuid()
    g = b.make_group("crossbar_l", [-2.3, 5.05, 0], [cu_l], group_uuid=gu)
    crossbar_groups.append((gu, g, "left"))

    cu_r = b.add_cube(
        "crossbar_right",
        from_=[0.1, 4.5, -0.5], to_=[4.5, 5.6, 0.5],
        faces=iron_face(93),
    )
    gu = make_uuid()
    g = b.make_group("crossbar_r", [2.3, 5.05, 0], [cu_r], group_uuid=gu)
    crossbar_groups.append((gu, g, "right"))

    # =====================================================================
    # 3) FLUKES — 2 curved flukes at bottom, each 2 flat slab bones at 45°
    # =====================================================================
    fluke_groups = []
    # Left fluke — 2 angled slabs
    for i, (offset_z, ang_z) in enumerate([(0, 35), (0.4, 55)]):
        cu = b.add_cube(
            f"fluke_left_{i+1}",
            from_=[-2.2, -1.5, -0.45], to_=[-0.2, -1.0, 0.45],
            faces=iron_face(110 + i),
        )
        gu = make_uuid()
        g = b.make_group(
            f"fluke_l_{i+1}", [-1.2, -1.2, offset_z], [cu],
            rotation=[0, 0, ang_z], group_uuid=gu,
        )
        fluke_groups.append((gu, g, "left"))

    # Right fluke — 2 angled slabs
    for i, (offset_z, ang_z) in enumerate([(0, -35), (0.4, -55)]):
        cu = b.add_cube(
            f"fluke_right_{i+1}",
            from_=[0.2, -1.5, -0.45], to_=[2.2, -1.0, 0.45],
            faces=iron_face(130 + i),
        )
        gu = make_uuid()
        g = b.make_group(
            f"fluke_r_{i+1}", [1.2, -1.2, offset_z], [cu],
            rotation=[0, 0, ang_z], group_uuid=gu,
        )
        fluke_groups.append((gu, g, "right"))

    # =====================================================================
    # 4) ANCHOR RING — 6 flat slabs in a circle at top of shank
    # =====================================================================
    ring_groups = []
    ring_y = 6.0
    for i in range(6):
        ang = (i / 6.0) * 2 * math.pi
        cx = math.cos(ang) * 0.7
        cz_off = math.sin(ang) * 0.18
        cu = b.add_cube(
            f"ring_seg_{i+1}",
            from_=[-0.4, -0.16, -0.18], to_=[0.4, 0.16, 0.18],
            faces=ground_face(160 + i * 7),
        )
        rot_z = math.degrees(ang)
        gu = make_uuid()
        g = b.make_group(
            f"anchor_ring_{i+1}", [cx, ring_y, cz_off], [cu],
            rotation=[0, 0, rot_z], group_uuid=gu,
        )
        ring_groups.append((gu, g))

    # =====================================================================
    # 5) CHAIN LINK GROUPS — 8 links hanging below the ring (going up actually)
    #    Each link = oval-cross (horizontal + vertical flat cubes)
    # =====================================================================
    chain_groups = []
    # Chain extends upward from anchor ring at Y=6 to Y=11
    for i in range(8):
        link_y = 6.5 + i * 0.7
        # horizontal slab
        h_uuid = b.add_cube(
            f"chain{i+1}_h",
            from_=[-0.42, link_y - 0.12, -0.22],
            to_=  [ 0.42, link_y + 0.12,  0.22],
            faces=iron_face(200 + i * 9),
        )
        # vertical slab — rotated 90° via narrow X / wider Y geometry
        v_uuid = b.add_cube(
            f"chain{i+1}_v",
            from_=[-0.22, link_y - 0.35, -0.12],
            to_=  [ 0.22, link_y + 0.35,  0.12],
            faces=iron_face(213 + i * 11),
        )
        gu = make_uuid()
        # Each link rotated 90° from previous around Y
        rot_y = 90.0 if i % 2 == 1 else 0.0
        g = b.make_group(
            f"chain_link_{i+1}", [0, link_y, 0], [h_uuid, v_uuid],
            rotation=[0, rot_y, 0], group_uuid=gu,
        )
        chain_groups.append((gu, g, i))

    # =====================================================================
    # 6) GROUND RUPTURE — 10 flat slabs at Y=0 radiating
    # =====================================================================
    rupture_groups = []
    for i in range(10):
        ang = (i / 10.0) * 2 * math.pi + 0.03
        r = 4.5
        cx = math.cos(ang) * r
        cz = math.sin(ang) * r
        cu = b.add_cube(
            f"rupture_{i+1}",
            from_=[-1.6, 0.02, -0.55], to_=[1.6, 0.18, 0.55],
            faces=ground_face(260 + i * 13),
        )
        rot_y = math.degrees(ang)
        gu = make_uuid()
        g = b.make_group(
            f"ground_rupture_{i+1}", [cx, 0.1, cz], [cu],
            rotation=[0, rot_y, 0], group_uuid=gu,
        )
        rupture_groups.append((gu, g, ang))

    # =====================================================================
    # 7) CHAIN TENSION DUST — 6 small flat cubes at base of chain
    # =====================================================================
    dust_groups = []
    dust_positions = [
        (1.2, 0.3, 0.6, 24), (-1.0, 0.4, -0.5, 67), (0.6, 0.5, 1.1, 113),
        (-0.8, 0.35, 0.8, 158), (0.9, 0.45, -1.0, 202), (-1.2, 0.6, 0.2, 251),
    ]
    for i, (px, py, pz, rot) in enumerate(dust_positions):
        cu = b.add_cube(
            f"dust_{i+1}",
            from_=[-0.32, -0.08, -0.32], to_=[0.32, 0.08, 0.32],
            faces=ground_face(310 + i * 17),
        )
        gu = make_uuid()
        g = b.make_group(
            f"chain_dust_{i+1}", [px, py, pz], [cu],
            rotation=[0, rot, 0], group_uuid=gu,
        )
        dust_groups.append((gu, g, px, py, pz))

    # ROOT
    root_children = []
    for (gu, g) in shank_groups:
        root_children.append(g)
    for (gu, g, _) in crossbar_groups:
        root_children.append(g)
    for (gu, g, _) in fluke_groups:
        root_children.append(g)
    for (gu, g) in ring_groups:
        root_children.append(g)
    for (gu, g, _) in chain_groups:
        root_children.append(g)
    for (gu, g, _) in rupture_groups:
        root_children.append(g)
    for (gu, g, _px, _py, _pz) in dust_groups:
        root_children.append(g)

    root_uuid = make_uuid()
    root_group = b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid)
    b.outliner.append(root_group)

    # ===========================================================================
    # ANIMATION — SPAWN (1.3s)
    # Ground rupture blasts 0.2s. Chains erupt bottom-up at 0.1s stagger.
    # Shank erupts upward at 0.4s. Flukes deploy at 0.6s. Crossbar at 0.7s.
    # Anchor ring materialises at 0.8s. Chain snaps taut at 0.9s.
    # ===========================================================================
    spawn = {}

    # Ground rupture — blast outward at 0.2s
    for idx, (gu, g, ang) in enumerate(rupture_groups):
        delay = 0.2 + idx * 0.02
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.10, 1.3, 1.3, 1.3),
            make_keyframe("scale", delay + 0.20, 1, 1, 1),
            make_keyframe("scale", 1.3, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Chain links — erupt bottom-to-top 0.1s stagger, snap taut at 0.9s
    for idx, (gu, g, i) in enumerate(chain_groups):
        delay = 0.1 + i * 0.07
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.08, 1.0, 1.0, 1.0),
            # snap taut at 0.9s
            make_keyframe("scale", 0.85, 1.0, 1.0, 1.0),
            make_keyframe("scale", 0.92, 1.15, 1.25, 1.15),
            make_keyframe("scale", 1.05, 1.0, 1.0, 1.0),
            make_keyframe("scale", 1.3, 1.0, 1.0, 1.0),
            make_keyframe("position", 0.0, 0, -2, 0),
            make_keyframe("position", delay, 0, -2, 0),
            make_keyframe("position", delay + 0.10, 0, 0, 0),
            make_keyframe("position", 1.3, 0, 0, 0),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Shank segments — erupt at 0.4s, extend upward
    for i, (gu, g) in enumerate(shank_groups):
        delay = 0.4 + i * 0.04
        kfs = [
            make_keyframe("position", 0.0, 0, -3, 0),
            make_keyframe("position", delay, 0, -3, 0),
            make_keyframe("position", delay + 0.18, 0, 0.2, 0),
            make_keyframe("position", delay + 0.28, 0, 0, 0),
            make_keyframe("position", 1.3, 0, 0, 0),
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.10, 1.2, 1.3, 1.2),
            make_keyframe("scale", delay + 0.25, 1, 1, 1),
            make_keyframe("scale", 1.3, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Flukes — deploy at 0.6s with rotation
    for idx, (gu, g, side) in enumerate(fluke_groups):
        delay = 0.6 + (idx % 2) * 0.05
        sign = 1 if side == "left" else -1
        final_z = 35 * sign if idx % 2 == 0 else 55 * sign
        kfs = [
            make_keyframe("position", 0.0, 0, -1, 0),
            make_keyframe("position", delay, 0, -1, 0),
            make_keyframe("position", delay + 0.18, 0, 0, 0),
            make_keyframe("position", 1.3, 0, 0, 0),
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", delay, 0, 0, 0),
            make_keyframe("rotation", delay + 0.15, 0, 0, final_z * 1.15),
            make_keyframe("rotation", delay + 0.25, 0, 0, final_z),
            make_keyframe("rotation", 1.3, 0, 0, final_z),
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.12, 1, 1, 1),
            make_keyframe("scale", 1.3, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Crossbar — deploy at 0.7s
    for idx, (gu, g, side) in enumerate(crossbar_groups):
        delay = 0.7 + idx * 0.04
        sign = -1 if side == "left" else 1
        kfs = [
            make_keyframe("position", 0.0, -sign * 2, 0, 0),
            make_keyframe("position", delay, -sign * 2, 0, 0),
            make_keyframe("position", delay + 0.15, sign * 0.3, 0, 0),
            make_keyframe("position", delay + 0.22, 0, 0, 0),
            make_keyframe("position", 1.3, 0, 0, 0),
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.12, 1, 1, 1),
            make_keyframe("scale", 1.3, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Anchor ring — materialise at 0.8s
    for idx, (gu, g) in enumerate(ring_groups):
        delay = 0.8 + idx * 0.02
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.08, 1.2, 1.2, 1.2),
            make_keyframe("scale", delay + 0.15, 1, 1, 1),
            make_keyframe("scale", 1.3, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Chain tension dust — float up at 0.4s
    for idx, (gu, g, px, py, pz) in enumerate(dust_groups):
        delay = 0.4 + idx * 0.05
        kfs = [
            make_keyframe("position", 0.0, 0, -1, 0),
            make_keyframe("position", delay, 0, -1, 0),
            make_keyframe("position", delay + 0.20, 0, 0.6, 0),
            make_keyframe("position", 1.3, 0, 1.0, 0),
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.10, 1.4, 1.4, 1.4),
            make_keyframe("scale", delay + 0.25, 1, 1, 1),
            make_keyframe("scale", 1.3, 0.6, 0.6, 0.6),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("spawn", length=1.3, animators=spawn, loop="once", override=True)

    # ===========================================================================
    # ANIMATION — IDLE (5.0s)
    # Anchor very slowly sways ±2° on a 6s period.
    # Chain links have secondary motion — 0.1s lag per link down.
    # Ground rupture settles. Dust floats.
    # ===========================================================================
    idle = {}

    # Shank sway — slow ±2° around Z
    for i, (gu, g) in enumerate(shank_groups):
        kfs = []
        for k in range(11):
            t = (k / 10) * 5.0
            ph = (t / 5.0) * 2 * math.pi
            rz = math.sin(ph) * 2.0
            kfs.append(make_keyframe("rotation", t, 0, 0, rz))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Crossbar sway — follow shank
    for idx, (gu, g, side) in enumerate(crossbar_groups):
        kfs = []
        for k in range(11):
            t = (k / 10) * 5.0
            ph = (t / 5.0) * 2 * math.pi
            rz = math.sin(ph) * 2.0
            kfs.append(make_keyframe("rotation", t, 0, 0, rz))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Flukes — follow shank with slight lag
    for idx, (gu, g, side) in enumerate(fluke_groups):
        sign = 1 if side == "left" else -1
        final_z = 35 * sign if idx % 2 == 0 else 55 * sign
        kfs = []
        for k in range(11):
            t = (k / 10) * 5.0
            ph = ((t - 0.2) / 5.0) * 2 * math.pi
            rz = final_z + math.sin(ph) * 1.5
            kfs.append(make_keyframe("rotation", t, 0, 0, rz))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Anchor ring — follow shank
    for idx, (gu, g) in enumerate(ring_groups):
        kfs = []
        for k in range(11):
            t = (k / 10) * 5.0
            ph = (t / 5.0) * 2 * math.pi
            rz = math.sin(ph) * 2.0
            ang = (idx / 6.0) * 2 * math.pi
            kfs.append(make_keyframe("rotation", t, 0, 0, math.degrees(ang) + rz))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Chain links — secondary motion lag, 0.1s per link DOWN (i.e. link 0 leads, link 7 lags most)
    for idx, (gu, g, i) in enumerate(chain_groups):
        lag = i * 0.1
        kfs = []
        for k in range(13):
            t = (k / 12) * 5.0
            ph = ((t - lag) / 5.0) * 2 * math.pi
            rz = math.sin(ph) * 1.5
            # subtle X-axis tension oscillation
            rx = math.cos(ph) * 0.8
            kfs.append(make_keyframe("rotation", t, rx, 0, rz))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Ground rupture — slow settle (pulse scale)
    for idx, (gu, g, ang) in enumerate(rupture_groups):
        kfs = []
        for k in range(9):
            t = (k / 8) * 5.0
            ph = (t / 4.5) * 2 * math.pi + idx * 0.4
            s = 1.0 + 0.04 * math.sin(ph)
            kfs.append(make_keyframe("scale", t, s, s, s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Dust — float drift
    for idx, (gu, g, px, py, pz) in enumerate(dust_groups):
        kfs = []
        for k in range(11):
            t = (k / 10) * 5.0
            ph = (t / 4.0) * 2 * math.pi + idx * 0.5
            yy = 1.0 + 0.4 * math.sin(ph)
            xx = math.sin(ph * 0.7) * 0.3
            kfs.append(make_keyframe("position", t, xx, yy, 0))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("idle", length=5.0, animators=idle, loop="loop", override=False)

    # ===========================================================================
    # ANIMATION — DISSIPATE (0.9s)
    # Anchor retracts downward — yanked back below ground.
    # Chain links follow bottom-to-top rapid.
    # Ground rupture contracts. Dust settles.
    # ===========================================================================
    diss = {}

    # Anchor ring vanishes first
    for idx, (gu, g) in enumerate(ring_groups):
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.15, 1, 1, 1),
            make_keyframe("scale", 0.25, 0, 0, 0),
            make_keyframe("scale", 0.9, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Crossbar collapses
    for idx, (gu, g, side) in enumerate(crossbar_groups):
        sign = -1 if side == "left" else 1
        kfs = [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.3, sign * 1.5, -0.5, 0),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.25, 1, 1, 1),
            make_keyframe("scale", 0.35, 0, 0, 0),
            make_keyframe("scale", 0.9, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Flukes retract
    for idx, (gu, g, side) in enumerate(fluke_groups):
        kfs = [
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.35, 1, 1, 1),
            make_keyframe("scale", 0.5, 0, 0, 0),
            make_keyframe("scale", 0.9, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Shank yanks downward — bottom segment first (it's being pulled DOWN)
    for i, (gu, g) in enumerate(shank_groups):
        # Reverse stagger: bottom segment (i=3) yanks first
        delay = (3 - i) * 0.05
        kfs = [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", delay + 0.10, 0, -1.5, 0),
            make_keyframe("position", delay + 0.25, 0, -4, 0),
            make_keyframe("position", 0.9, 0, -6, 0),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", delay + 0.30, 1, 1, 1),
            make_keyframe("scale", 0.9, 0.3, 0.3, 0.3),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Chain links follow bottom-to-top (i=0 is bottom, follows shank)
    for idx, (gu, g, i) in enumerate(chain_groups):
        delay = 0.05 + i * 0.04
        kfs = [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", delay, 0, 0, 0),
            make_keyframe("position", delay + 0.15, 0, -2, 0),
            make_keyframe("position", 0.9, 0, -5, 0),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", delay + 0.15, 1, 1, 1),
            make_keyframe("scale", 0.9, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Ground rupture contracts
    for idx, (gu, g, ang) in enumerate(rupture_groups):
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.5, 1, 1, 1),
            make_keyframe("scale", 0.9, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Dust settles
    for idx, (gu, g, px, py, pz) in enumerate(dust_groups):
        kfs = [
            make_keyframe("position", 0.0, 0, 1, 0),
            make_keyframe("position", 0.9, 0, -0.5, 0),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.6, 1, 1, 1),
            make_keyframe("scale", 0.9, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("dissipate", length=0.9, animators=diss, loop="once", override=True)

    b.write(OUTPUT_PATH)
    sz = os.path.getsize(OUTPUT_PATH)
    print(f"Wrote {OUTPUT_PATH} ({sz} bytes, {sz/1024:.1f} KB)")


if __name__ == "__main__":
    build()
