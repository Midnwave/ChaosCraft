#!/usr/bin/env python3
"""Generator: cursed_prison_orbital.bbmodel — Chain attack 17 'Cell Block'.

6 prison cell bar segments orbit the caster in a hexagon. Each bar segment is
3 vertical flat slabs. Cursed chains connect adjacent bars; curse nodes glow
at each connection. A top cap ring closes the cell, and a ground hexagon sits
at Y=0.

UNIQUE vs other modes:
 - Hexagonal CELL silhouette (not a circular ring)
 - Bars are vertical, parallel, institutional — strong identity
 - Cursed purple-grey iron texture (not the warden's spectral, not the wheel's aged)
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
    chain_link_silhouette, radial_glow,
)
from _freezingice_common import (
    specular_blob, random_bright_specks, hex_color,
)


OUTPUT = "D:/CC/ChaosCraft/src/main/resources/models/chain/me_attacks/cursed_prison_orbital.bbmodel"
W = H = 64


# ---------------------------------------------------------------------------
# TEXTURES
# ---------------------------------------------------------------------------

def tex_cursed_bar():
    """Institutional iron contaminated purple-grey by curse. Strong vertical bars."""
    institutional = hex_color("#0c0810")
    cell_iron     = hex_color("#241c28")
    tainted       = hex_color("#484050")
    bright_edge   = hex_color("#706878")
    curse_em      = hex_color("#7030c0")
    pixels = base_fill(W, H, cell_iron)
    # Vertical institutional bars: 3 wide bars across texture
    # bar width 14, gap 6 — fits 3 bars in 64px
    bar_width = 14
    gap = 6
    x = 4
    while x + bar_width <= W:
        for xb in range(x, x + bar_width):
            for y in range(H):
                p = pixels[y * W + xb]
                pixels[y * W + xb] = (
                    int(p[0] * 0.6 + institutional[0] * 0.4),
                    int(p[1] * 0.6 + institutional[1] * 0.4),
                    int(p[2] * 0.6 + institutional[2] * 0.4),
                    255,
                )
        # bright edge on the right side
        edge = x + bar_width - 1
        for y in range(H):
            p = pixels[y * W + edge]
            pixels[y * W + edge] = (
                min(255, p[0] + (bright_edge[0] - p[0]) // 2),
                min(255, p[1] + (bright_edge[1] - p[1]) // 2),
                min(255, p[2] + (bright_edge[2] - p[2]) // 2),
                255,
            )
        # left dark edge
        ledge = x
        for y in range(H):
            p = pixels[y * W + ledge]
            pixels[y * W + ledge] = (
                int(p[0] * 0.5), int(p[1] * 0.5), int(p[2] * 0.5), 255,
            )
        x += bar_width + gap
    # Manufacturing marks every 12 rows across the bars (regular spacing)
    for y in (10, 22, 34, 46, 58):
        for x in range(W):
            p = pixels[y * W + x]
            # only mark on bar regions, slight darken
            pixels[y * W + x] = (
                max(0, p[0] - 16), max(0, p[1] - 14), max(0, p[2] - 12), 255,
            )
    # Tainted purple wash (texture-wide subtle tint)
    for y in range(H):
        for x in range(W):
            p = pixels[y * W + x]
            pixels[y * W + x] = (
                int(p[0] * 0.85 + tainted[0] * 0.15),
                int(p[1] * 0.85 + tainted[1] * 0.15),
                int(p[2] * 0.85 + tainted[2] * 0.15),
                255,
            )
    # Curse nodes — bright purple emissive dots at intersections
    for cx, cy in [(8, 12), (8, 40), (32, 24), (32, 52), (56, 16), (56, 44)]:
        specular_blob(pixels, W, H, cx, cy, curse_em, radius=5, strength=0.85)
    random_bright_specks(pixels, W, H, 80, curse_em, seed=15, size_max=1)
    jitter_inplace(pixels, jitter_range=12, seed=27, density=0.85)
    return png_from_pixels(pixels, W, H)


def tex_cursed_void():
    """Deep cursed void with purple lines."""
    void = hex_color("#060410")
    purple_line = hex_color("#6020a0")
    bright = hex_color("#9040d0")
    pixels = base_fill(W, H, void)
    # Diagonal purple void lines
    rng = random.Random(53)
    for _ in range(28):
        slope = rng.choice([-1, 1])
        offset = rng.randrange(-W, H + W)
        thick = rng.randint(1, 2)
        intensity = rng.uniform(0.4, 0.85)
        for x in range(W):
            y0 = slope * x + offset
            for t in range(thick):
                y = y0 + t
                if 0 <= y < H:
                    p = pixels[y * W + x]
                    pixels[y * W + x] = (
                        int(p[0] * (1 - intensity) + purple_line[0] * intensity),
                        int(p[1] * (1 - intensity) + purple_line[1] * intensity),
                        int(p[2] * (1 - intensity) + purple_line[2] * intensity),
                        255,
                    )
    # Bright void specks
    random_bright_specks(pixels, W, H, 60, bright, seed=71, size_max=1)
    # Concentrated curse glow centers
    runic_glyph_ring(pixels, W, H, 32, 32, 20, bright, density=0.7, seed=82)
    runic_glyph_ring(pixels, W, H, 32, 32, 10, purple_line, density=0.8, seed=83)
    jitter_inplace(pixels, jitter_range=10, seed=91, density=0.75)
    return png_from_pixels(pixels, W, H)


# ---------------------------------------------------------------------------
# BUILD
# ---------------------------------------------------------------------------

def build():
    b = Builder("cursed_prison_orbital", resolution=(W, H), visible_box=(20, 12, 0))
    b.add_texture("cursed_bar", tex_cursed_bar())
    b.add_texture("cursed_void", tex_cursed_void())

    F0 = lambda: uniform_face(0, 0, W, H, tex_index=0)
    F1 = lambda: uniform_face(0, 0, W, H, tex_index=1)

    RADIUS = 8.0
    NUM_SEG = 6
    BAR_HEIGHT = 6.0  # bars go from Y=0 to Y=6

    # ---- 6 hexagonal bar segments ----
    bar_segments = []  # list of dicts with bone uuids
    for si in range(NUM_SEG):
        ang = (si / float(NUM_SEG)) * 2 * math.pi
        cx = math.cos(ang) * RADIUS
        cz = math.sin(ang) * RADIUS
        # Each segment: 3 vertical flat slab bones side-by-side
        bar_bone_data = []
        for bi in range(3):
            # bar offset along tangent of the orbit
            offset = (bi - 1) * 1.0  # -1, 0, +1
            # cubes are tall vertical slabs
            cube = b.add_cube(
                f"seg{si+1}_bar{bi+1}",
                from_=[-0.30, 0.0, -0.25],
                to_=[0.30, BAR_HEIGHT, 0.25],
                faces=F0(),
            )
            # Position each bar within the segment along the tangent
            tangent_x = -math.sin(ang) * offset
            tangent_z = math.cos(ang) * offset
            bar_gu = make_uuid()
            bar_g = b.make_group(
                f"seg{si+1}_bar{bi+1}_grp",
                [tangent_x, 0, tangent_z],
                [cube],
                group_uuid=bar_gu,
            )
            bar_bone_data.append((bar_gu, bar_g, offset))

        # Connecting chain: 3-link chain at mid-height connecting to NEXT segment
        next_ang = ((si + 1) / float(NUM_SEG)) * 2 * math.pi
        next_cx = math.cos(next_ang) * RADIUS
        next_cz = math.sin(next_ang) * RADIUS
        # Chain bone group sits between current and next segment (at mid)
        mid_x = (cx + next_cx) / 2 - cx
        mid_z = (cz + next_cz) / 2 - cz
        chain_cubes = []
        # 3 link cubes along the connection arc
        for li in range(3):
            t = (li + 1) / 4.0  # 0.25, 0.50, 0.75
            link_x = (1 - t) * 0 + t * (mid_x * 2)
            link_z = (1 - t) * 0 + t * (mid_z * 2)
            # alternating orientation
            if li % 2 == 0:
                f_ = [link_x - 0.35, BAR_HEIGHT * 0.5 - 0.25, link_z - 0.18]
                t_ = [link_x + 0.35, BAR_HEIGHT * 0.5 + 0.25, link_z + 0.18]
            else:
                f_ = [link_x - 0.18, BAR_HEIGHT * 0.5 - 0.25, link_z - 0.35]
                t_ = [link_x + 0.18, BAR_HEIGHT * 0.5 + 0.25, link_z + 0.35]
            cc = b.add_cube(
                f"seg{si+1}_chain_link{li+1}",
                from_=f_, to_=t_, faces=F0(),
            )
            chain_cubes.append(cc)
        chain_gu = make_uuid()
        chain_g = b.make_group(
            f"seg{si+1}_chain_grp",
            [0, 0, 0],
            chain_cubes,
            group_uuid=chain_gu,
        )

        # Curse node at the connection point (the join to next segment)
        node_cubes = []
        for ci in range(2):
            ox = mid_x + ci * 0.1
            oy = BAR_HEIGHT * 0.5 + ci * 0.2
            oz = mid_z + ci * 0.1
            cc = b.add_cube(
                f"seg{si+1}_curse_node{ci+1}",
                from_=[ox - 0.30, oy - 0.30, oz - 0.30],
                to_=[ox + 0.30, oy + 0.30, oz + 0.30],
                faces=F0(),
            )
            node_cubes.append(cc)
        node_gu = make_uuid()
        node_g = b.make_group(
            f"seg{si+1}_curse_node_grp",
            [0, 0, 0], node_cubes, group_uuid=node_gu,
        )

        # Top cap segment connecting bar tops in hex ring
        cap_cube = b.add_cube(
            f"seg{si+1}_top_cap",
            from_=[mid_x - 0.55, BAR_HEIGHT - 0.20, mid_z - 0.55],
            to_=[mid_x + 0.55, BAR_HEIGHT, mid_z + 0.55],
            faces=F1(),
        )
        cap_gu = make_uuid()
        cap_g = b.make_group(
            f"seg{si+1}_top_cap_grp",
            [0, 0, 0], [cap_cube], group_uuid=cap_gu,
        )

        # Ground hexagon slab for this segment
        ground_cube = b.add_cube(
            f"seg{si+1}_ground_slab",
            from_=[-1.0, -0.12, -1.0],
            to_=[1.0, 0.08, 1.0],
            faces=F1(),
        )
        ground_gu = make_uuid()
        ground_g = b.make_group(
            f"seg{si+1}_ground_grp",
            [0, 0, 0], [ground_cube], group_uuid=ground_gu,
        )

        # Segment parent group at (cx, 0, cz) — rotates outward facing the ring center
        seg_children = [bg[0] for bg in bar_bone_data] + [chain_gu, node_gu, cap_gu, ground_gu]
        seg_gu = make_uuid()
        seg_g = b.make_group(
            f"prison_seg_{si+1}",
            [cx, 0, cz],
            seg_children,
            rotation=[0, -math.degrees(ang), 0],  # face center
            group_uuid=seg_gu,
        )
        bar_segments.append({
            "seg_gu": seg_gu, "seg_g": seg_g,
            "bars": bar_bone_data,
            "chain_gu": chain_gu, "chain_g": chain_g,
            "node_gu": node_gu, "node_g": node_g,
            "cap_gu": cap_gu, "cap_g": cap_g,
            "ground_gu": ground_gu, "ground_g": ground_g,
            "angle": ang,
        })

    # ---- Orbit parent: holds all 6 segments and rotates them ----
    orbit_gu = make_uuid()
    orbit_g = b.make_group(
        "orbit_parent",
        [0, 0, 0],
        [seg["seg_g"] for seg in bar_segments],
        group_uuid=orbit_gu,
    )

    # ---- Ground hexagon center: 6 flat slabs ----
    ground_hex_groups = []
    for gi in range(6):
        ang = (gi / 6.0) * 2 * math.pi
        gx = math.cos(ang) * 4.0
        gz = math.sin(ang) * 4.0
        cube = b.add_cube(
            f"ground_hex_{gi+1}",
            from_=[gx - 1.4, -0.10, gz - 1.4],
            to_=[gx + 1.4, 0.06, gz + 1.4],
            faces=F1(),
            rotation=[0, math.degrees(ang) + 30, 0],
        )
        gu = make_uuid()
        g = b.make_group(
            f"ground_hex_grp_{gi+1}",
            [0, 0, 0], [cube], group_uuid=gu,
        )
        ground_hex_groups.append((gu, g))

    ground_hex_parent_gu = make_uuid()
    ground_hex_parent = b.make_group(
        "ground_hexagon", [0, 0, 0],
        [g for (_u, g) in ground_hex_groups],
        group_uuid=ground_hex_parent_gu,
    )

    # ---- ROOT ----
    root_uuid = make_uuid()
    root = b.make_group("root", [0, 0, 0],
                         [orbit_g, ground_hex_parent], group_uuid=root_uuid)
    b.outliner.append(root)

    # =========================================================================
    # ANIMATIONS
    # =========================================================================

    # ---- SPAWN: 1.2s ----
    spawn = {}
    # Ground hexagon expands at 0.0
    for gi, (gu, g) in enumerate(ground_hex_groups):
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=1, z=0),
            make_keyframe("scale", 0.10, x=1.2, y=1, z=1.2),
            make_keyframe("scale", 0.20, x=1, y=1, z=1),
            make_keyframe("scale", 1.20, x=1, y=1, z=1),
        ]}
    # Bar segments fly in from outer radius one by one clockwise stagger 0.12s
    for si, seg in enumerate(bar_segments):
        t0 = 0.10 + si * 0.12
        ang = seg["angle"]
        fly_x = math.cos(ang) * 14  # start far outside
        fly_z = math.sin(ang) * 14
        cx = math.cos(ang) * RADIUS
        cz = math.sin(ang) * RADIUS
        # Use position offsets relative to home (which is cx, 0, cz)
        # Initial offset: (fly_x - cx, 0, fly_z - cz)
        spawn[seg["seg_gu"]] = {"name": seg["seg_g"]["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=fly_x - cx, y=2, z=fly_z - cz),
            make_keyframe("position", t0, x=fly_x - cx, y=2, z=fly_z - cz),
            make_keyframe("position", t0 + 0.10, x=-0.3, y=0.2, z=-0.3),
            make_keyframe("position", t0 + 0.14, x=0.1, y=-0.1, z=0.1),
            make_keyframe("position", t0 + 0.18, x=0, y=0, z=0),
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.08, x=1.1, y=1, z=1.1),
            make_keyframe("scale", t0 + 0.12, x=1, y=1, z=1),
        ]}
        # Chains materialise as adjacent bars arrive
        spawn[seg["chain_gu"]] = {"name": seg["chain_g"]["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.20, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.28, x=1, y=1, z=1),
            make_keyframe("scale", 1.20, x=1, y=1, z=1),
        ]}
        # Curse nodes glow on
        spawn[seg["node_gu"]] = {"name": seg["node_g"]["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.28, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.34, x=1.4, y=1.4, z=1.4),
            make_keyframe("scale", t0 + 0.40, x=1, y=1, z=1),
            make_keyframe("scale", 1.20, x=1, y=1, z=1),
        ]}
        # Top cap slides into place after bar
        spawn[seg["cap_gu"]] = {"name": seg["cap_g"]["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=4, z=0),
            make_keyframe("position", t0 + 0.20, x=0, y=4, z=0),
            make_keyframe("position", t0 + 0.30, x=0, y=0, z=0),
            make_keyframe("position", 1.20, x=0, y=0, z=0),
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.20, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.30, x=1, y=1, z=1),
        ]}
    b.add_animation("spawn", length=1.20, animators=spawn, loop="once", override=True)

    # ---- IDLE: 7.0s — orbit + bar oscillation + node pulse ----
    idle = {}
    IDLE_LEN = 7.0
    IDLE_STEPS = 70
    # Orbit parent rotates 360° over 7s
    orbit_kfs = []
    for k in range(IDLE_STEPS + 1):
        t = (k / IDLE_STEPS) * IDLE_LEN
        deg = (t / IDLE_LEN) * 360
        orbit_kfs.append(make_keyframe("rotation", t, x=0, y=deg, z=0))
    idle[orbit_gu] = {"name": orbit_g["name"], "type": "bone", "keyframes": orbit_kfs}
    # Ground hexagon parent counter-rotates
    g_hex_kfs = []
    for k in range(IDLE_STEPS + 1):
        t = (k / IDLE_STEPS) * IDLE_LEN
        deg = -(t / IDLE_LEN) * 360 * 0.6
        g_hex_kfs.append(make_keyframe("rotation", t, x=0, y=deg, z=0))
    idle[ground_hex_parent_gu] = {"name": ground_hex_parent["name"], "type": "bone", "keyframes": g_hex_kfs}
    # Per-bar oscillation (rattle)
    for si, seg in enumerate(bar_segments):
        for bi, (bgu, bg, off) in enumerate(seg["bars"]):
            kfs = []
            for k in range(IDLE_STEPS + 1):
                t = (k / IDLE_STEPS) * IDLE_LEN
                ph = (t / 2.5) * 2 * math.pi + si * 0.4 + bi * 0.7
                kfs.append(make_keyframe("rotation", t,
                                          x=0,
                                          y=0,
                                          z=0.5 * math.sin(ph)))
            idle[bgu] = {"name": bg["name"], "type": "bone", "keyframes": kfs}
        # Connecting chain sway with secondary lag
        chain_kfs = []
        for k in range(IDLE_STEPS + 1):
            t = (k / IDLE_STEPS) * IDLE_LEN
            ph = (t / 2.5) * 2 * math.pi + si * 0.5 + 0.4
            chain_kfs.append(make_keyframe("rotation", t,
                                            x=2 * math.sin(ph * 0.9),
                                            y=0,
                                            z=1.5 * math.cos(ph)))
        idle[seg["chain_gu"]] = {"name": seg["chain_g"]["name"], "type": "bone", "keyframes": chain_kfs}
        # Curse node pulse
        node_kfs = []
        for k in range(IDLE_STEPS + 1):
            t = (k / IDLE_STEPS) * IDLE_LEN
            ph = (t / 1.8) * 2 * math.pi + si * 0.6
            s = 1.0 + 0.18 * math.sin(ph)
            node_kfs.append(make_keyframe("scale", t, x=s, y=s, z=s))
        idle[seg["node_gu"]] = {"name": seg["node_g"]["name"], "type": "bone", "keyframes": node_kfs}
        # Cap oscillation
        cap_kfs = []
        for k in range(IDLE_STEPS + 1):
            t = (k / IDLE_STEPS) * IDLE_LEN
            ph = (t / 3.2) * 2 * math.pi + si * 0.3
            cap_kfs.append(make_keyframe("position", t,
                                          x=0, y=0.05 * math.sin(ph), z=0))
        idle[seg["cap_gu"]] = {"name": seg["cap_g"]["name"], "type": "bone", "keyframes": cap_kfs}
        # Segment scale heartbeat (extra keyframes)
        seg_kfs = []
        for k in range(IDLE_STEPS + 1):
            t = (k / IDLE_STEPS) * IDLE_LEN
            ph = (t / 4.0) * 2 * math.pi + si * 0.7
            seg_kfs.append(make_keyframe("scale", t,
                                          x=1 + 0.01 * math.sin(ph),
                                          y=1 + 0.005 * math.cos(ph),
                                          z=1 + 0.01 * math.sin(ph + 0.3)))
        idle[seg["seg_gu"]] = {"name": seg["seg_g"]["name"], "type": "bone", "keyframes": seg_kfs}
    b.add_animation("idle", length=IDLE_LEN, animators=idle, loop="loop", override=False)

    # ---- DISSIPATE: 0.8s ----
    diss = {}
    for si, seg in enumerate(bar_segments):
        ang = seg["angle"]
        # Bar segments fly outward tangentially
        tan_x = -math.sin(ang) * 14
        tan_z = math.cos(ang) * 14
        diss[seg["seg_gu"]] = {"name": seg["seg_g"]["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.30, x=tan_x * 0.2, y=1, z=tan_z * 0.2),
            make_keyframe("position", 0.80, x=tan_x, y=4, z=tan_z),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.80, x=0.2, y=0.2, z=0.2),
        ]}
        # Chains flash bright then vanish
        diss[seg["chain_gu"]] = {"name": seg["chain_g"]["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.10, x=1.5, y=1.5, z=1.5),
            make_keyframe("scale", 0.20, x=0, y=0, z=0),
        ]}
        # Curse nodes fly in random directions
        rng_ang = (si * 1.7) % (2 * math.pi)
        diss[seg["node_gu"]] = {"name": seg["node_g"]["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.80, x=math.cos(rng_ang) * 8, y=3, z=math.sin(rng_ang) * 8),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.80, x=0, y=0, z=0),
        ]}
        # Top cap pieces scatter
        diss[seg["cap_gu"]] = {"name": seg["cap_g"]["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.80, x=0, y=6, z=0),
            make_keyframe("rotation", 0.0, x=0, y=0, z=0),
            make_keyframe("rotation", 0.80, x=180, y=360, z=180),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.80, x=0, y=0, z=0),
        ]}
    # Ground hexagon contracts
    diss[ground_hex_parent_gu] = {"name": ground_hex_parent["name"], "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, x=1, y=1, z=1),
        make_keyframe("scale", 0.80, x=0, y=1, z=0),
    ]}
    b.add_animation("dissipate", length=0.80, animators=diss, loop="once", override=True)

    b.write(OUTPUT)
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    build()
