#!/usr/bin/env python3
"""Generator: void_chain_anchor_ring.bbmodel — Chain attack 20 'Mooring'.

5 void-cursed chain anchors orbiting in a ring at ground level. Each anchor has
a T-bar crossbar, 3-cube shank, and 2 angled flukes. A connection chain runs
from each anchor to a central 4-cube void node at Y=3. 6 void wisp cubes orbit
the node at different heights. Ground ring 8 flat slabs at Y=0.

UNIQUE vs other modes:
 - 5 ANCHORS shape (not bars, wardens, wheels) — clearly recognisable hardware
 - Anchors share ONE central node — visual identity of mooring/dependency
 - Void-purple emissive (maximum intensity at center) — visually loudest of the set
 - Pendulum-rocking of anchors on their chains (vertical oscillation per anchor)
"""

import math
import random
import sys

sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder, make_uuid, make_keyframe, uniform_face, png_from_pixels,
)
from _chain_common import (
    base_fill, jitter_inplace, rust_blotch, runic_glyph_ring,
    chain_link_silhouette, radial_glow, horizontal_stress_lines,
)
from _freezingice_common import (
    specular_blob, random_bright_specks, hex_color,
)


OUTPUT = "D:/CC/ChaosCraft/src/main/resources/models/chain/me_attacks/void_chain_anchor_ring.bbmodel"
W = H = 64


# ---------------------------------------------------------------------------
# TEXTURES
# ---------------------------------------------------------------------------

def tex_cursed_anchor_iron():
    """Cursed iron with anchor-specific tone variation: corroded crossbar/flukes,
    brighter shank."""
    base       = hex_color("#080608")
    dark_corro = hex_color("#181018")
    surface    = hex_color("#302028")
    bright_shank = hex_color("#5a4858")
    purple_em  = hex_color("#4020a0")
    bright_pur = hex_color("#a060ff")
    pixels = base_fill(W, H, base)
    # Upper third = darker (crossbar/flukes corroded), middle = brighter (shank)
    for y in range(H):
        for x in range(W):
            if y < H // 3:
                t = y / float(H // 3)
                r = int(base[0] + (dark_corro[0] - base[0]) * t)
                g = int(base[1] + (dark_corro[1] - base[1]) * t)
                bl = int(base[2] + (dark_corro[2] - base[2]) * t)
            elif y < 2 * H // 3:
                t = (y - H // 3) / float(H // 3)
                r = int(dark_corro[0] + (bright_shank[0] - dark_corro[0]) * t)
                g = int(dark_corro[1] + (bright_shank[1] - dark_corro[1]) * t)
                bl = int(dark_corro[2] + (bright_shank[2] - dark_corro[2]) * t)
            else:
                t = (y - 2 * H // 3) / float(H // 3)
                r = int(bright_shank[0] + (surface[0] - bright_shank[0]) * t)
                g = int(bright_shank[1] + (surface[1] - bright_shank[1]) * t)
                bl = int(bright_shank[2] + (surface[2] - bright_shank[2]) * t)
            pixels[y * W + x] = (r, g, bl, 255)
    # Heavy corrosion in upper band (the crossbar/flukes)
    rust_blotch(pixels, W, H, base, dark_corro, surface, count=14, seed=53)
    # Vertical stress grain on the shank band
    for y in range(H // 3, 2 * H // 3):
        for x in range(0, W, 4):
            xx = x + (y % 3)
            if 0 <= xx < W:
                p = pixels[y * W + xx]
                pixels[y * W + xx] = (
                    min(255, p[0] + (bright_shank[0] - p[0]) // 3),
                    min(255, p[1] + (bright_shank[1] - p[1]) // 3),
                    min(255, p[2] + (bright_shank[2] - p[2]) // 3),
                    255,
                )
    # Purple curse emissive blobs scattered
    for cx, cy in [(12, 14), (44, 22), (28, 38), (50, 50), (18, 52)]:
        specular_blob(pixels, W, H, cx, cy, purple_em, radius=4, strength=0.7)
        specular_blob(pixels, W, H, cx, cy, bright_pur, radius=2, strength=0.55)
    # Purple thread veins
    horizontal_stress_lines(pixels, W, H, purple_em, count=6, seed=71)
    chain_link_silhouette(pixels, W, H, purple_em, bright_pur, count=4, seed=83)
    random_bright_specks(pixels, W, H, 50, bright_pur, seed=91, size_max=1)
    jitter_inplace(pixels, jitter_range=11, seed=97, density=0.85)
    return png_from_pixels(pixels, W, H)


def tex_void_node():
    """Maximum void intensity: bright purple core, rapid falloff to near-black."""
    bright_void = hex_color("#d060ff")
    mid_void    = hex_color("#7028b0")
    deep_void   = hex_color("#040210")
    super_bright = hex_color("#ffa0ff")
    pixels = base_fill(W, H, deep_void)
    cx, cy = W / 2, H / 2
    max_r = W * 0.55
    for y in range(H):
        for x in range(W):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            t = min(1.0, d / max_r)
            if t < 0.15:
                # bright core
                u = t / 0.15
                r = int(super_bright[0] + (bright_void[0] - super_bright[0]) * u)
                g = int(super_bright[1] + (bright_void[1] - super_bright[1]) * u)
                bl = int(super_bright[2] + (bright_void[2] - super_bright[2]) * u)
            elif t < 0.45:
                u = (t - 0.15) / 0.30
                r = int(bright_void[0] + (mid_void[0] - bright_void[0]) * u)
                g = int(bright_void[1] + (mid_void[1] - bright_void[1]) * u)
                bl = int(bright_void[2] + (mid_void[2] - bright_void[2]) * u)
            else:
                u = (t - 0.45) / 0.55
                r = int(mid_void[0] + (deep_void[0] - mid_void[0]) * u)
                g = int(mid_void[1] + (deep_void[1] - mid_void[1]) * u)
                bl = int(mid_void[2] + (deep_void[2] - mid_void[2]) * u)
            pixels[y * W + x] = (r, g, bl, 255)
    # Void runic glyph rings
    runic_glyph_ring(pixels, W, H, int(cx), int(cy), 14, super_bright, density=0.7, seed=37)
    runic_glyph_ring(pixels, W, H, int(cx), int(cy), 22, bright_void, density=0.6, seed=41)
    runic_glyph_ring(pixels, W, H, int(cx), int(cy), 28, mid_void, density=0.5, seed=43)
    # Bright specks
    random_bright_specks(pixels, W, H, 90, super_bright, seed=51, size_max=1)
    random_bright_specks(pixels, W, H, 40, bright_void, seed=53, size_max=2)
    jitter_inplace(pixels, jitter_range=10, seed=61, density=0.65)
    return png_from_pixels(pixels, W, H)


# ---------------------------------------------------------------------------
# BUILD
# ---------------------------------------------------------------------------

def build_anchor(b, idx, ang, radius, F0):
    """Build one anchor as a group at orbit position."""
    cx = math.cos(ang) * radius
    cz = math.sin(ang) * radius
    # ---- Crossbar (T-bar): wide flat slab at top ----
    crossbar = b.add_cube(
        f"anchor{idx}_crossbar",
        from_=[-1.6, 1.6, -0.30], to_=[1.6, 1.95, 0.30],
        faces=F0(),
    )
    crossbar_gu = make_uuid()
    crossbar_g = b.make_group(
        f"anchor{idx}_crossbar_grp", [0, 0, 0],
        [crossbar], group_uuid=crossbar_gu,
    )
    # ---- Shank: 3 cube segments stacked vertically ----
    shank_cubes = []
    for si in range(3):
        cy0 = 0.0 + si * 0.6
        cy1 = cy0 + 0.6
        c = b.add_cube(
            f"anchor{idx}_shank{si+1}",
            from_=[-0.30, cy0, -0.30], to_=[0.30, cy1, 0.30],
            faces=F0(),
        )
        shank_cubes.append(c)
    shank_gu = make_uuid()
    shank_g = b.make_group(
        f"anchor{idx}_shank_grp", [0, 0, 0],
        shank_cubes, group_uuid=shank_gu,
    )
    # ---- Flukes: 2 angled flat slabs at bottom ----
    fluke_L = b.add_cube(
        f"anchor{idx}_fluke_L",
        from_=[-1.4, -0.45, -0.25], to_=[-0.3, -0.10, 0.25],
        faces=F0(),
    )
    fluke_L_gu = make_uuid()
    fluke_L_g = b.make_group(
        f"anchor{idx}_fluke_L_grp", [-0.5, -0.20, 0],
        [fluke_L], rotation=[0, 0, -25], group_uuid=fluke_L_gu,
    )
    fluke_R = b.add_cube(
        f"anchor{idx}_fluke_R",
        from_=[0.3, -0.45, -0.25], to_=[1.4, -0.10, 0.25],
        faces=F0(),
    )
    fluke_R_gu = make_uuid()
    fluke_R_g = b.make_group(
        f"anchor{idx}_fluke_R_grp", [0.5, -0.20, 0],
        [fluke_R], rotation=[0, 0, 25], group_uuid=fluke_R_gu,
    )
    # ---- Anchor parent group at orbit position, at Y=1 ----
    anchor_children = [crossbar_gu, shank_gu, fluke_L_gu, fluke_R_gu]
    anchor_gu = make_uuid()
    anchor_g = b.make_group(
        f"anchor_{idx}", [cx, 1, cz],
        anchor_children,
        rotation=[0, -math.degrees(ang), 0],  # face center
        group_uuid=anchor_gu,
    )
    return {
        "anchor_gu": anchor_gu, "anchor_g": anchor_g,
        "crossbar_gu": crossbar_gu, "crossbar_g": crossbar_g,
        "shank_gu": shank_gu, "shank_g": shank_g,
        "fluke_L_gu": fluke_L_gu, "fluke_L_g": fluke_L_g,
        "fluke_R_gu": fluke_R_gu, "fluke_R_g": fluke_R_g,
        "ang": ang,
        "cx": cx, "cz": cz,
    }


def build():
    b = Builder("void_chain_anchor_ring", resolution=(W, H), visible_box=(20, 8, 0))
    b.add_texture("cursed_anchor", tex_cursed_anchor_iron())
    b.add_texture("void_node", tex_void_node())

    F0 = lambda: uniform_face(0, 0, W, H, tex_index=0)
    F1 = lambda: uniform_face(0, 0, W, H, tex_index=1)

    RADIUS = 8.0
    NODE_Y = 3.0

    # ---- 5 anchors ----
    anchors = []
    for i in range(5):
        ang = (i / 5.0) * 2 * math.pi
        anchors.append(build_anchor(b, i + 1, ang, RADIUS, F0))

    # ---- Connection chains: 5 short 3-link chains from each anchor to center ----
    connection_chain_data = []
    for i, anchor in enumerate(anchors):
        # Chain runs from anchor crossbar position (cx, 2.5, cz) to center (0, NODE_Y, 0)
        sx, sy, sz = anchor["cx"], 2.5, anchor["cz"]
        ex, ey, ez = 0.0, NODE_Y, 0.0
        chain_cubes = []
        for li in range(3):
            t = (li + 1) / 4.0  # 0.25, 0.50, 0.75
            lx = sx + (ex - sx) * t
            ly = sy + (ey - sy) * t
            lz = sz + (ez - sz) * t
            # oval link with alternating orientation
            if li % 2 == 0:
                top = b.add_cube(
                    f"conn{i+1}_link{li+1}_top",
                    from_=[lx - 0.30, ly + 0.18, lz - 0.12],
                    to_=[lx + 0.30, ly + 0.34, lz + 0.12],
                    faces=F0(),
                )
                bot = b.add_cube(
                    f"conn{i+1}_link{li+1}_bot",
                    from_=[lx - 0.30, ly - 0.34, lz - 0.12],
                    to_=[lx + 0.30, ly - 0.18, lz + 0.12],
                    faces=F0(),
                )
                side1 = b.add_cube(
                    f"conn{i+1}_link{li+1}_s1",
                    from_=[lx - 0.30, ly - 0.18, lz - 0.12],
                    to_=[lx - 0.20, ly + 0.18, lz + 0.12],
                    faces=F0(),
                )
                side2 = b.add_cube(
                    f"conn{i+1}_link{li+1}_s2",
                    from_=[lx + 0.20, ly - 0.18, lz - 0.12],
                    to_=[lx + 0.30, ly + 0.18, lz + 0.12],
                    faces=F0(),
                )
            else:
                top = b.add_cube(
                    f"conn{i+1}_link{li+1}_top",
                    from_=[lx - 0.12, ly + 0.18, lz - 0.30],
                    to_=[lx + 0.12, ly + 0.34, lz + 0.30],
                    faces=F0(),
                )
                bot = b.add_cube(
                    f"conn{i+1}_link{li+1}_bot",
                    from_=[lx - 0.12, ly - 0.34, lz - 0.30],
                    to_=[lx + 0.12, ly - 0.18, lz + 0.30],
                    faces=F0(),
                )
                side1 = b.add_cube(
                    f"conn{i+1}_link{li+1}_s1",
                    from_=[lx - 0.12, ly - 0.18, lz - 0.30],
                    to_=[lx + 0.12, ly + 0.18, lz - 0.20],
                    faces=F0(),
                )
                side2 = b.add_cube(
                    f"conn{i+1}_link{li+1}_s2",
                    from_=[lx - 0.12, ly - 0.18, lz + 0.20],
                    to_=[lx + 0.12, ly + 0.18, lz + 0.30],
                    faces=F0(),
                )
            chain_cubes.extend([top, bot, side1, side2])
        conn_gu = make_uuid()
        conn_g = b.make_group(
            f"connection_chain_{i+1}", [0, 0, 0],
            chain_cubes, group_uuid=conn_gu,
        )
        connection_chain_data.append((conn_gu, conn_g, anchor["ang"]))

    # ---- Central void node: 4-cube compressed sphere at Y=3 ----
    node_cubes = []
    node_positions = [
        (0, 0, 0), (-0.5, 0.4, 0.3), (0.5, -0.3, 0.5), (0.2, 0.3, -0.5),
    ]
    for ni, (ox, oy, oz) in enumerate(node_positions):
        c = b.add_cube(
            f"void_node_{ni+1}",
            from_=[ox - 0.75, NODE_Y + oy - 0.75, oz - 0.75],
            to_=[ox + 0.75, NODE_Y + oy + 0.75, oz + 0.75],
            faces=F1(),
            rotation=[ni * 25, ni * 30, ni * 35],
        )
        node_cubes.append(c)
    node_gu = make_uuid()
    node_g = b.make_group(
        "void_node", [0, NODE_Y, 0], node_cubes, group_uuid=node_gu,
    )

    # ---- Ground ring: 8 flat slabs at Y=0 ----
    ground_ring_groups = []
    for gi in range(8):
        ang = (gi / 8.0) * 2 * math.pi
        gx = math.cos(ang) * 5.0
        gz = math.sin(ang) * 5.0
        cube = b.add_cube(
            f"ground_ring_{gi+1}",
            from_=[gx - 1.2, -0.10, gz - 1.2],
            to_=[gx + 1.2, 0.05, gz + 1.2],
            faces=F1(),
            rotation=[0, math.degrees(ang) + 22.5, 0],
        )
        gu = make_uuid()
        g = b.make_group(
            f"ground_ring_grp_{gi+1}", [0, 0, 0], [cube], group_uuid=gu,
        )
        ground_ring_groups.append((gu, g, ang))
    ground_ring_parent_gu = make_uuid()
    ground_ring_parent = b.make_group("ground_ring", [0, 0, 0],
                                        [g for (_u, g, _a) in ground_ring_groups],
                                        group_uuid=ground_ring_parent_gu)

    # ---- Void wisps: 6 small flat cubes orbiting node at different heights ----
    wisp_groups = []
    for wi in range(6):
        ang = (wi / 6.0) * 2 * math.pi
        wisp_radius = 2.0
        cx = math.cos(ang) * wisp_radius
        cz = math.sin(ang) * wisp_radius
        # Different heights for each wisp
        wy = NODE_Y + 0.5 + (wi % 3) * 0.6 - 1.0
        cube = b.add_cube(
            f"void_wisp_{wi+1}",
            from_=[-0.30, -0.18, -0.30],
            to_=[0.30, 0.18, 0.30],
            faces=F1(),
            rotation=[wi * 30, wi * 45, wi * 25],
        )
        gu = make_uuid()
        g = b.make_group(
            f"void_wisp_grp_{wi+1}",
            [cx, wy, cz], [cube], group_uuid=gu,
        )
        wisp_groups.append((gu, g, ang, wy))

    # ---- Anchor orbit parent (rotates anchors around) ----
    orbit_gu = make_uuid()
    orbit_g = b.make_group("anchor_orbit", [0, 0, 0],
                            [a["anchor_g"] for a in anchors],
                            group_uuid=orbit_gu)

    # ---- Wisp orbit parent ----
    wisp_orbit_gu = make_uuid()
    wisp_orbit_g = b.make_group("wisp_orbit", [0, 0, 0],
                                  [g for (_u, g, _a, _y) in wisp_groups],
                                  group_uuid=wisp_orbit_gu)

    # ---- ROOT ----
    root_children = [orbit_g, ground_ring_parent, node_g, wisp_orbit_g] + [g for (_u, g, _a) in connection_chain_data]
    root_uuid = make_uuid()
    root = b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid)
    b.outliner.append(root)

    # =========================================================================
    # ANIMATIONS
    # =========================================================================

    # ---- SPAWN: 1.0s ----
    spawn = {}
    # Void node at 0.0
    spawn[node_gu] = {"name": node_g["name"], "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, x=0, y=0, z=0),
        make_keyframe("scale", 0.10, x=1.6, y=1.6, z=1.6),
        make_keyframe("scale", 0.20, x=1, y=1, z=1),
        make_keyframe("scale", 1.00, x=1, y=1, z=1),
    ]}
    # Ground ring expands at 0.1s
    for gi, (gu, g, _a) in enumerate(ground_ring_groups):
        t0 = 0.10 + gi * 0.01
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=1, z=0),
            make_keyframe("scale", t0, x=0, y=1, z=0),
            make_keyframe("scale", t0 + 0.08, x=1, y=1, z=1),
            make_keyframe("scale", 1.00, x=1, y=1, z=1),
        ]}
    # Connection chains at 0.2s
    for i, (gu, g, _a) in enumerate(connection_chain_data):
        t0 = 0.20 + i * 0.02
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.08, x=1, y=1, z=1),
            make_keyframe("scale", 1.00, x=1, y=1, z=1),
        ]}
    # Anchors fly in 0.3-0.6 staggered
    for i, anchor in enumerate(anchors):
        t0 = 0.30 + i * 0.06
        ang = anchor["ang"]
        fly_x = math.cos(ang) * 16 - anchor["cx"]
        fly_z = math.sin(ang) * 16 - anchor["cz"]
        spawn[anchor["anchor_gu"]] = {"name": anchor["anchor_g"]["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=fly_x, y=5, z=fly_z),
            make_keyframe("position", t0, x=fly_x, y=5, z=fly_z),
            make_keyframe("position", t0 + 0.18, x=0, y=-0.3, z=0),
            make_keyframe("position", t0 + 0.22, x=0, y=0.15, z=0),
            make_keyframe("position", t0 + 0.26, x=0, y=0, z=0),
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.10, x=1.1, y=1.1, z=1.1),
            make_keyframe("scale", t0 + 0.18, x=1, y=1, z=1),
        ]}
    # Void wisps at 0.7s
    for wi, (gu, g, _a, _y) in enumerate(wisp_groups):
        t0 = 0.70 + wi * 0.03
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.08, x=1.2, y=1.2, z=1.2),
            make_keyframe("scale", t0 + 0.12, x=1, y=1, z=1),
            make_keyframe("scale", 1.00, x=1, y=1, z=1),
        ]}
    b.add_animation("spawn", length=1.00, animators=spawn, loop="once", override=True)

    # ---- IDLE: 6.0s ----
    idle = {}
    IDLE_LEN = 6.0
    IDLE_STEPS = 60
    # Anchor orbit rotates
    orbit_kfs = []
    for k in range(IDLE_STEPS + 1):
        t = (k / IDLE_STEPS) * IDLE_LEN
        deg = (t / IDLE_LEN) * 360
        orbit_kfs.append(make_keyframe("rotation", t, x=0, y=deg, z=0))
    idle[orbit_gu] = {"name": orbit_g["name"], "type": "bone", "keyframes": orbit_kfs}
    # Ground ring counter-rotates
    g_ring_kfs = []
    for k in range(IDLE_STEPS + 1):
        t = (k / IDLE_STEPS) * IDLE_LEN
        deg = -(t / IDLE_LEN) * 360 * 0.6
        g_ring_kfs.append(make_keyframe("rotation", t, x=0, y=deg, z=0))
    idle[ground_ring_parent_gu] = {"name": ground_ring_parent["name"], "type": "bone", "keyframes": g_ring_kfs}
    # Wisp orbit rotates faster, at slight tilt
    wisp_kfs = []
    for k in range(IDLE_STEPS + 1):
        t = (k / IDLE_STEPS) * IDLE_LEN
        deg = (t / IDLE_LEN) * 360 * 2.5
        # Slight wobble in X/Z
        wisp_kfs.append(make_keyframe("rotation", t,
                                       x=15 * math.sin(t * 0.8),
                                       y=deg,
                                       z=10 * math.cos(t * 0.6)))
    idle[wisp_orbit_gu] = {"name": wisp_orbit_g["name"], "type": "bone", "keyframes": wisp_kfs}
    # Void node pulses and rotates all 3 axes
    node_kfs = []
    for k in range(IDLE_STEPS + 1):
        t = (k / IDLE_STEPS) * IDLE_LEN
        ph = (t / 1.2) * 2 * math.pi
        s = 1.0 + 0.15 * math.sin(ph)
        node_kfs.append(make_keyframe("scale", t, x=s, y=s, z=s))
        node_kfs.append(make_keyframe("rotation", t,
                                        x=(t / IDLE_LEN) * 360 * 1.5,
                                        y=(t / IDLE_LEN) * 360 * 2.0,
                                        z=(t / IDLE_LEN) * 360 * 1.2))
    idle[node_gu] = {"name": node_g["name"], "type": "bone", "keyframes": node_kfs}
    # Per-anchor pendulum rock
    for i, anchor in enumerate(anchors):
        phase = i * 0.6
        anchor_kfs = []
        for k in range(IDLE_STEPS + 1):
            t = (k / IDLE_STEPS) * IDLE_LEN
            ph = (t / 2.5) * 2 * math.pi + phase
            # Vertical bob like a pendulum hanging
            anchor_kfs.append(make_keyframe("position", t,
                                              x=0,
                                              y=0.2 * math.sin(ph),
                                              z=0))
            anchor_kfs.append(make_keyframe("rotation", t,
                                              x=4 * math.sin(ph),
                                              y=0,
                                              z=2 * math.cos(ph * 0.8)))
        idle[anchor["anchor_gu"]] = {"name": anchor["anchor_g"]["name"], "type": "bone", "keyframes": anchor_kfs}
        # Crossbar subtle scale
        cb_kfs = []
        for k in range(IDLE_STEPS + 1):
            t = (k / IDLE_STEPS) * IDLE_LEN
            ph = (t / 3.0) * 2 * math.pi + phase
            s = 1.0 + 0.02 * math.sin(ph)
            cb_kfs.append(make_keyframe("scale", t, x=s, y=1, z=s))
        idle[anchor["crossbar_gu"]] = {"name": anchor["crossbar_g"]["name"], "type": "bone", "keyframes": cb_kfs}
        # Flukes subtle outward sway
        for fluke_label, fluke_gu, fluke_g, base_rot_z in (
            ("L", anchor["fluke_L_gu"], anchor["fluke_L_g"], -25),
            ("R", anchor["fluke_R_gu"], anchor["fluke_R_g"], 25),
        ):
            f_kfs = []
            for k in range(IDLE_STEPS + 1):
                t = (k / IDLE_STEPS) * IDLE_LEN
                ph = (t / 2.7) * 2 * math.pi + phase + (0.3 if fluke_label == "L" else 0.5)
                f_kfs.append(make_keyframe("rotation", t,
                                             x=0,
                                             y=0,
                                             z=base_rot_z + 2 * math.sin(ph)))
            idle[fluke_gu] = {"name": fluke_g["name"], "type": "bone", "keyframes": f_kfs}
        # Shank subtle scale
        sh_kfs = []
        for k in range(IDLE_STEPS + 1):
            t = (k / IDLE_STEPS) * IDLE_LEN
            ph = (t / 3.5) * 2 * math.pi + phase
            s = 1.0 + 0.01 * math.sin(ph)
            sh_kfs.append(make_keyframe("scale", t, x=s, y=s, z=s))
        idle[anchor["shank_gu"]] = {"name": anchor["shank_g"]["name"], "type": "bone", "keyframes": sh_kfs}
    # Connection chains sway with secondary lag
    for i, (gu, g, ang) in enumerate(connection_chain_data):
        phase = i * 0.5 + 0.4  # lag behind anchor
        c_kfs = []
        for k in range(IDLE_STEPS + 1):
            t = (k / IDLE_STEPS) * IDLE_LEN
            ph = (t / 2.5) * 2 * math.pi + phase
            c_kfs.append(make_keyframe("rotation", t,
                                         x=2 * math.sin(ph),
                                         y=1 * math.cos(ph * 0.9),
                                         z=2 * math.cos(ph)))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": c_kfs}
    b.add_animation("idle", length=IDLE_LEN, animators=idle, loop="loop", override=False)

    # ---- DISSIPATE: 0.8s ----
    diss = {}
    # Connection chains detach and retract to node
    for i, (gu, g, ang) in enumerate(connection_chain_data):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.30, x=0.5, y=0.5, z=0.5),
            make_keyframe("scale", 0.50, x=0, y=0, z=0),
        ]}
    # Anchors fly outward tangentially
    for i, anchor in enumerate(anchors):
        ang = anchor["ang"]
        tan_x = -math.sin(ang) * 14
        tan_z = math.cos(ang) * 14
        diss[anchor["anchor_gu"]] = {"name": anchor["anchor_g"]["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.30, x=tan_x * 0.2, y=2, z=tan_z * 0.2),
            make_keyframe("position", 0.80, x=tan_x, y=5, z=tan_z),
            make_keyframe("rotation", 0.0, x=0, y=0, z=0),
            make_keyframe("rotation", 0.80, x=180, y=270, z=180),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.80, x=0.2, y=0.2, z=0.2),
        ]}
    # Void node implodes
    diss[node_gu] = {"name": node_g["name"], "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, x=1, y=1, z=1),
        make_keyframe("scale", 0.30, x=2.0, y=2.0, z=2.0),
        make_keyframe("scale", 0.50, x=0.6, y=0.6, z=0.6),
        make_keyframe("scale", 0.80, x=0, y=0, z=0),
    ]}
    # Ground ring expands and snaps
    diss[ground_ring_parent_gu] = {"name": ground_ring_parent["name"], "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, x=1, y=1, z=1),
        make_keyframe("scale", 0.50, x=1.5, y=1, z=1.5),
        make_keyframe("scale", 0.80, x=0, y=0, z=0),
    ]}
    # Wisps scatter
    for wi, (gu, g, ang, _y) in enumerate(wisp_groups):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.80, x=math.cos(ang) * 6, y=4, z=math.sin(ang) * 6),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.80, x=0, y=0, z=0),
        ]}
    b.add_animation("dissipate", length=0.80, animators=diss, loop="once", override=True)

    b.write(OUTPUT)
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    build()
