#!/usr/bin/env python3
"""Generator: chain_colossus_hands.bbmodel — Chain attack 16 'Iron Giant'.

Two enormous iron hands descend from the sky on chains, settle on either side
of the caster, and hover with subtle finger flex. Each hand is built from a
broad multi-cube palm, 4 finger-chain bones (3 cubes each, tapered), a thumb,
knuckle ridge slabs, an ascending chain bone, and a ground impression slab.

UNIQUE vs other modes:
 - Descent FROM ABOVE (not eruption from below) — fundamentally inverted spawn vector
 - Two HUGE bilateral palms (not orbital ring, not single sentinel)
 - "Colossus iron" texture: smoother than rusty chain iron — high-mass forged
"""

import math
import random
import sys

sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder, make_uuid, make_keyframe, uniform_face, png_from_pixels,
)
from _chain_common import (
    base_fill, jitter_inplace, rust_blotch, horizontal_stress_lines,
    chain_link_silhouette, radial_glow,
)
from _freezingice_common import (
    specular_blob, random_bright_specks, hex_color,
)


OUTPUT = "D:/CC/ChaosCraft/src/main/resources/models/chain/me_attacks/chain_colossus_hands.bbmodel"
W = H = 64


# ---------------------------------------------------------------------------
# TEXTURES
# ---------------------------------------------------------------------------

def tex_colossus_iron():
    """Heavy forged iron — smoother than rusty chain iron. Massive-scale surface."""
    deep_colossus = hex_color("#0e0c0c")
    heavy_dark    = hex_color("#261e18")
    surface_iron  = hex_color("#503e30")
    forged_edge   = hex_color("#806448")
    spec          = hex_color("#b09470")
    pixels = base_fill(W, H, heavy_dark)
    # Vertical mass-gradient: top slightly darker
    for y in range(H):
        for x in range(W):
            t = y / float(H - 1)
            r = int(deep_colossus[0] + (surface_iron[0] - deep_colossus[0]) * t)
            g = int(deep_colossus[1] + (surface_iron[1] - deep_colossus[1]) * t)
            bl = int(deep_colossus[2] + (surface_iron[2] - deep_colossus[2]) * t)
            pixels[y * W + x] = (r, g, bl, 255)
    # Scale variation — broad subtle low-contrast horizontal stripes (huge-iron look)
    horizontal_stress_lines(pixels, W, H, forged_edge, count=10, seed=11)
    # Few sparse pits — but not heavy rust
    rust_blotch(pixels, W, H, deep_colossus, heavy_dark, surface_iron, count=6, seed=17)
    # Forged highlight band across mid
    for y in range(28, 36):
        for x in range(W):
            p = pixels[y * W + x]
            pixels[y * W + x] = (
                min(255, p[0] + 10), min(255, p[1] + 8), min(255, p[2] + 6), 255,
            )
    # Specular blobs on a few high points
    specular_blob(pixels, W, H, 14, 12, spec, radius=4, strength=0.7)
    specular_blob(pixels, W, H, 48, 22, spec, radius=3, strength=0.55)
    specular_blob(pixels, W, H, 32, 50, forged_edge, radius=5, strength=0.5)
    random_bright_specks(pixels, W, H, 60, forged_edge, seed=29, size_max=1)
    jitter_inplace(pixels, jitter_range=14, seed=41, density=0.85)
    return png_from_pixels(pixels, W, H)


def tex_ground_impression():
    """Dark compressed ground with iron-grey hand shadow."""
    compressed = hex_color("#060604")
    iron_shadow = hex_color("#1e1c18")
    edge = hex_color("#3a3024")
    pixels = base_fill(W, H, compressed)
    # Central dark hand-shaped shadow blob
    cx, cy = W // 2, H // 2
    for y in range(H):
        for x in range(W):
            # roughly hand-shaped: oval palm + finger extensions
            dx = (x - cx) / 18.0
            dy = (y - cy) / 14.0
            d = math.sqrt(dx * dx + dy * dy)
            if d < 1.0:
                t = d  # 0 center -> 1 edge
                p = pixels[y * W + x]
                pixels[y * W + x] = (
                    int(iron_shadow[0] + (compressed[0] - iron_shadow[0]) * t),
                    int(iron_shadow[1] + (compressed[1] - iron_shadow[1]) * t),
                    int(iron_shadow[2] + (compressed[2] - iron_shadow[2]) * t),
                    255,
                )
    # Compression cracks radiating
    rng = random.Random(91)
    for _ in range(14):
        ang = rng.uniform(0, 2 * math.pi)
        L = rng.randint(8, 22)
        for k in range(L):
            x = int(cx + math.cos(ang) * k)
            y = int(cy + math.sin(ang) * k)
            if 0 <= x < W and 0 <= y < H:
                p = pixels[y * W + x]
                pixels[y * W + x] = (
                    max(0, p[0] - 8), max(0, p[1] - 8), max(0, p[2] - 8), 255,
                )
    # Edge highlight ring
    for ang_i in range(0, 360, 6):
        ang = math.radians(ang_i)
        for r in range(18, 22):
            x = int(cx + math.cos(ang) * r)
            y = int(cy + math.sin(ang) * r * 0.78)
            if 0 <= x < W and 0 <= y < H:
                p = pixels[y * W + x]
                pixels[y * W + x] = (
                    min(255, p[0] + 18), min(255, p[1] + 14), min(255, p[2] + 8), 255,
                )
    jitter_inplace(pixels, jitter_range=10, seed=43, density=0.85)
    return png_from_pixels(pixels, W, H)


# ---------------------------------------------------------------------------
# BUILD
# ---------------------------------------------------------------------------

def build_hand(b, side_name, side_sign):
    """Build one hand (palm + 4 fingers + thumb + knuckles + wrist) and return
    its group dict + uuid plus child bone uuids that need per-bone animation.

    side_sign: -1 for left (x<0), +1 for right.
    """
    F0 = lambda: uniform_face(0, 0, W, H, tex_index=0)

    children_meta = {}  # bone_uuid -> bone group_dict (for per-bone keyframing)

    # ---- Palm: 3 overlapping cubes forming wide flat palm ----
    palm_core = b.add_cube(
        f"{side_name}_palm_core",
        from_=[-2.5, -0.6, -1.8], to_=[2.5, 0.6, 1.8],
        faces=F0(),
    )
    palm_back = b.add_cube(
        f"{side_name}_palm_back",
        from_=[-2.2, -0.4, -2.4], to_=[2.2, 0.4, -1.6],
        faces=F0(),
    )
    palm_front = b.add_cube(
        f"{side_name}_palm_front",
        from_=[-2.0, -0.4, 1.6], to_=[2.0, 0.4, 2.6],
        faces=F0(),
    )
    palm_gu = make_uuid()
    palm_g = b.make_group(
        f"{side_name}_palm",
        [0, 0, 0],
        [palm_core, palm_back, palm_front],
        group_uuid=palm_gu,
    )
    children_meta[palm_gu] = palm_g

    # ---- 4 Fingers: each 3 cubes tapered ----
    finger_groups = []
    for fi in range(4):
        # finger root x position along palm (slightly fanned)
        fx = (fi - 1.5) * 1.1  # spread across palm
        seg_cubes = []
        for si in range(3):
            taper = 1.0 - si * 0.15
            half_w = 0.45 * taper
            half_h = 0.45 * taper
            z0 = 2.6 + si * 1.1
            z1 = z0 + 1.1
            c = b.add_cube(
                f"{side_name}_finger{fi+1}_seg{si+1}",
                from_=[fx - half_w, -half_h, z0],
                to_=[fx + half_w, half_h, z1],
                faces=F0(),
            )
            seg_cubes.append(c)
        fgu = make_uuid()
        fg = b.make_group(
            f"{side_name}_finger{fi+1}",
            [fx, 0, 2.6],
            seg_cubes,
            group_uuid=fgu,
        )
        finger_groups.append((fgu, fg))
        children_meta[fgu] = fg

    # ---- Thumb: 2-cube finger angled outward via bone rotation ----
    thumb_cubes = []
    for si in range(2):
        taper = 1.0 - si * 0.15
        half_w = 0.5 * taper
        half_h = 0.5 * taper
        z0 = si * 1.0
        z1 = z0 + 1.0
        c = b.add_cube(
            f"{side_name}_thumb_seg{si+1}",
            from_=[-half_w, -half_h, z0],
            to_=[half_w, half_h, z1],
            faces=F0(),
        )
        thumb_cubes.append(c)
    thumb_gu = make_uuid()
    # angle thumb outward from palm (side_sign determines direction)
    thumb_rot = [0, side_sign * -45, 0]
    thumb_g = b.make_group(
        f"{side_name}_thumb",
        [side_sign * -2.8, 0, 1.2],
        thumb_cubes,
        rotation=thumb_rot,
        group_uuid=thumb_gu,
    )
    children_meta[thumb_gu] = thumb_g

    # ---- Knuckle ridges: 4 flat slab protrusions ----
    knuckle_cubes = []
    for ki in range(4):
        kx = (ki - 1.5) * 1.1
        c = b.add_cube(
            f"{side_name}_knuckle{ki+1}",
            from_=[kx - 0.45, 0.6, 2.0],
            to_=[kx + 0.45, 1.0, 2.6],
            faces=F0(),
        )
        knuckle_cubes.append(c)
    knuckle_gu = make_uuid()
    knuckle_g = b.make_group(
        f"{side_name}_knuckles",
        [0, 0.8, 2.3],
        knuckle_cubes,
        group_uuid=knuckle_gu,
    )
    children_meta[knuckle_gu] = knuckle_g

    # ---- Wrist nub + ascending chain anchor ----
    wrist_cube = b.add_cube(
        f"{side_name}_wrist",
        from_=[-1.4, -0.6, -3.4], to_=[1.4, 0.6, -2.6],
        faces=F0(),
    )
    wrist_gu = make_uuid()
    wrist_g = b.make_group(
        f"{side_name}_wrist",
        [0, 0, -3.0],
        [wrist_cube],
        group_uuid=wrist_gu,
    )
    children_meta[wrist_gu] = wrist_g

    # ---- Ascending chain: 4-cube oval links extending up to Y+16 ----
    chain_link_cubes = []
    link_count = 18
    for li in range(link_count):
        cy = 1.0 + li * 0.85
        if li % 2 == 0:
            # link oriented with long axis along X
            top = b.add_cube(
                f"{side_name}_chain_link{li+1}_top",
                from_=[-0.55, cy + 0.20, -0.18], to_=[0.55, cy + 0.40, 0.18],
                faces=F0(),
            )
            bot = b.add_cube(
                f"{side_name}_chain_link{li+1}_bot",
                from_=[-0.55, cy - 0.40, -0.18], to_=[0.55, cy - 0.20, 0.18],
                faces=F0(),
            )
            left_l = b.add_cube(
                f"{side_name}_chain_link{li+1}_L",
                from_=[-0.55, cy - 0.20, -0.18], to_=[-0.35, cy + 0.20, 0.18],
                faces=F0(),
            )
            right_l = b.add_cube(
                f"{side_name}_chain_link{li+1}_R",
                from_=[0.35, cy - 0.20, -0.18], to_=[0.55, cy + 0.20, 0.18],
                faces=F0(),
            )
        else:
            # link oriented with long axis along Z
            top = b.add_cube(
                f"{side_name}_chain_link{li+1}_top",
                from_=[-0.18, cy + 0.20, -0.55], to_=[0.18, cy + 0.40, 0.55],
                faces=F0(),
            )
            bot = b.add_cube(
                f"{side_name}_chain_link{li+1}_bot",
                from_=[-0.18, cy - 0.40, -0.55], to_=[0.18, cy - 0.20, 0.55],
                faces=F0(),
            )
            left_l = b.add_cube(
                f"{side_name}_chain_link{li+1}_L",
                from_=[-0.18, cy - 0.20, -0.55], to_=[0.18, cy + 0.20, -0.35],
                faces=F0(),
            )
            right_l = b.add_cube(
                f"{side_name}_chain_link{li+1}_R",
                from_=[-0.18, cy - 0.20, 0.35], to_=[0.18, cy + 0.20, 0.55],
                faces=F0(),
            )
        chain_link_cubes.extend([top, bot, left_l, right_l])
    chain_gu = make_uuid()
    chain_g = b.make_group(
        f"{side_name}_ascending_chain",
        [0, 1, -3.0],
        chain_link_cubes,
        group_uuid=chain_gu,
    )
    children_meta[chain_gu] = chain_g

    # ---- Hand root group: holds everything except chain/ground ----
    hand_children_uuids = [palm_gu] + [fgu for (fgu, _g) in finger_groups] + [thumb_gu, knuckle_gu, wrist_gu]
    hand_gu = make_uuid()
    # Hands sit at Y=6, X = ±7 from caster
    hand_origin = [side_sign * 7, 6, 0]
    hand_g = b.make_group(
        f"{side_name}_hand", hand_origin,
        hand_children_uuids,
        group_uuid=hand_gu,
    )
    children_meta[hand_gu] = hand_g

    return {
        "hand_gu": hand_gu, "hand_g": hand_g,
        "palm_gu": palm_gu,
        "fingers": finger_groups,
        "thumb_gu": thumb_gu,
        "knuckle_gu": knuckle_gu,
        "wrist_gu": wrist_gu,
        "chain_gu": chain_gu, "chain_g": chain_g,
        "hand_origin": hand_origin,
        "children_meta": children_meta,
        "side_sign": side_sign,
    }


def build():
    b = Builder("chain_colossus_hands", resolution=(W, H), visible_box=(20, 20, 0))
    b.add_texture("colossus_iron", tex_colossus_iron())
    b.add_texture("ground_impression", tex_ground_impression())

    F1 = lambda: uniform_face(0, 0, W, H, tex_index=1)

    # ---- 2 hands: L and R ----
    left = build_hand(b, "left", -1)
    right = build_hand(b, "right", +1)

    # ---- Ground impressions: 4 flat slabs below each hand ----
    ground_groups = []
    for hand_data, label in ((left, "left"), (right, "right")):
        side_sign = hand_data["side_sign"]
        slab_cubes = []
        for si in range(4):
            # Quadrant slabs at Y=0 below the hand
            dx = (si % 2) * 2 - 1  # -1 or +1
            dz = (si // 2) * 2 - 1
            cx = side_sign * 7 + dx * 1.5
            cz = dz * 1.5
            c = b.add_cube(
                f"{label}_ground_slab{si+1}",
                from_=[cx - 1.4, -0.15, cz - 1.4],
                to_=[cx + 1.4, 0.05, cz + 1.4],
                faces=F1(),
            )
            slab_cubes.append(c)
        gu = make_uuid()
        g = b.make_group(
            f"{label}_ground_impression",
            [side_sign * 7, 0, 0],
            slab_cubes,
            group_uuid=gu,
        )
        ground_groups.append(gu)
        hand_data["children_meta"][gu] = g
        hand_data["ground_gu"] = gu
        hand_data["ground_g"] = g

    # ---- ROOT ----
    root_children = [
        left["hand_g"], left["chain_g"], left["ground_g"],
        right["hand_g"], right["chain_g"], right["ground_g"],
    ]
    root_uuid = make_uuid()
    root = b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid)
    b.outliner.append(root)

    # =========================================================================
    # ANIMATIONS
    # =========================================================================

    def chain_y_kfs(events):
        """events = list of (time, y_offset)"""
        return [make_keyframe("position", t, x=0, y=y, z=0) for (t, y) in events]

    spawn = {}
    idle = {}
    diss = {}

    for hand in (left, right):
        side = hand["side_sign"]

        # ---- SPAWN: chain at Y+16 at 0, hand descends from Y+20 to Y=6 by 1.0s
        # The hand group's home origin is [side*7, 6, 0]. Use position offsets.
        spawn[hand["hand_gu"]] = {"name": hand["hand_g"]["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=14, z=0),  # +14 above home
            make_keyframe("position", 0.20, x=0, y=14, z=0),
            make_keyframe("position", 1.00, x=0, y=-0.4, z=0),  # overshoot down
            make_keyframe("position", 1.10, x=0, y=0.3, z=0),   # bounce up
            make_keyframe("position", 1.20, x=0, y=0, z=0),
            make_keyframe("position", 1.30, x=0, y=0, z=0),
        ]}
        # Chain appears at Y+16 at 0.0 (already at home); just scale-in
        spawn[hand["chain_gu"]] = {"name": hand["chain_g"]["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", 0.05, x=1, y=1, z=1),
            make_keyframe("scale", 1.30, x=1, y=1, z=1),
            # chain compresses as hand settles
            make_keyframe("scale", 1.00, x=1, y=1.0, z=1),
        ]}
        # Ground impressions appear at 0.8s
        spawn[hand["ground_gu"]] = {"name": hand["ground_g"]["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=1, z=0),
            make_keyframe("scale", 0.80, x=0, y=1, z=0),
            make_keyframe("scale", 0.95, x=1.2, y=1, z=1.2),
            make_keyframe("scale", 1.10, x=1, y=1, z=1),
        ]}
        # Knuckles compress at settle
        spawn[hand["knuckle_gu"]] = {"name": hand["children_meta"][hand["knuckle_gu"]]["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.95, x=0, y=0, z=0),
            make_keyframe("position", 1.05, x=0, y=-0.2, z=0),
            make_keyframe("position", 1.20, x=0, y=0, z=0),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 1.00, x=1, y=0.7, z=1),
            make_keyframe("scale", 1.20, x=1, y=1, z=1),
        ]}
        # Fingers flex on arrival
        for fi, (fgu, fg) in enumerate(hand["fingers"]):
            spawn[fgu] = {"name": fg["name"], "type": "bone", "keyframes": [
                make_keyframe("rotation", 0.0, x=0, y=0, z=0),
                make_keyframe("rotation", 0.90, x=0, y=0, z=0),
                make_keyframe("rotation", 1.05, x=-15, y=0, z=0),  # flex inward
                make_keyframe("rotation", 1.25, x=-3, y=0, z=0),
            ]}
        spawn[hand["thumb_gu"]] = {"name": hand["children_meta"][hand["thumb_gu"]]["name"], "type": "bone", "keyframes": [
            make_keyframe("rotation", 0.0, x=0, y=side * -45, z=0),
            make_keyframe("rotation", 1.00, x=0, y=side * -45, z=0),
            make_keyframe("rotation", 1.15, x=-10, y=side * -55, z=0),
            make_keyframe("rotation", 1.30, x=-3, y=side * -45, z=0),
        ]}

    b.add_animation("spawn", length=1.30, animators=spawn, loop="once", override=True)

    # ---- IDLE: hands hover, fingers slowly curl ±3° on staggered periods ----
    IDLE_STEPS = 50
    IDLE_LEN = 5.0
    for hand in (left, right):
        side = hand["side_sign"]
        # Hand subtle hover up/down
        hand_kfs = []
        for k in range(IDLE_STEPS + 1):
            t = (k / IDLE_STEPS) * IDLE_LEN
            hand_kfs.append(make_keyframe("position", t,
                                            x=0,
                                            y=0.15 * math.sin(t * 1.4 + side * 0.5),
                                            z=0.08 * math.cos(t * 1.1 + side * 0.3)))
        idle[hand["hand_gu"]] = {"name": hand["hand_g"]["name"], "type": "bone", "keyframes": hand_kfs}
        # Chain sway above — slight rotation, more keyframes
        chain_kfs = []
        for k in range(IDLE_STEPS + 1):
            t = (k / IDLE_STEPS) * IDLE_LEN
            ph = (t / 2.5) * 2 * math.pi + side * 0.3
            chain_kfs.append(make_keyframe("rotation", t,
                                            x=1.5 * math.sin(ph),
                                            y=0.5 * math.sin(ph * 0.7),
                                            z=1.5 * math.cos(ph * 0.8)))
        idle[hand["chain_gu"]] = {"name": hand["chain_g"]["name"], "type": "bone", "keyframes": chain_kfs}
        # Fingers slowly curl/extend on staggered periods
        for fi, (fgu, fg) in enumerate(hand["fingers"]):
            phase = fi * 0.7 + side * 0.4
            period = 3.0 + fi * 0.3
            kfs = []
            for k in range(IDLE_STEPS + 1):
                t = (k / IDLE_STEPS) * IDLE_LEN
                ph = (t / period) * 2 * math.pi + phase
                kfs.append(make_keyframe("rotation", t,
                                          x=-3 + 3 * math.sin(ph),
                                          y=0.5 * math.cos(ph * 0.9),
                                          z=0))
            idle[fgu] = {"name": fg["name"], "type": "bone", "keyframes": kfs}
        # Thumb slight curl
        thumb_kfs = []
        for k in range(IDLE_STEPS + 1):
            t = (k / IDLE_STEPS) * IDLE_LEN
            ph = (t / 3.5) * 2 * math.pi + side * 0.6
            thumb_kfs.append(make_keyframe("rotation", t,
                                            x=-3 + 2 * math.sin(ph),
                                            y=side * -45,
                                            z=0))
        idle[hand["thumb_gu"]] = {"name": hand["children_meta"][hand["thumb_gu"]]["name"], "type": "bone", "keyframes": thumb_kfs}
        # Ground impression pulse
        ground_kfs = []
        for k in range(IDLE_STEPS + 1):
            t = (k / IDLE_STEPS) * IDLE_LEN
            ph = (t / 2.0) * 2 * math.pi + side * 0.2
            s = 1.0 + 0.04 * math.sin(ph)
            ground_kfs.append(make_keyframe("scale", t, x=s, y=1, z=s))
        idle[hand["ground_gu"]] = {"name": hand["ground_g"]["name"], "type": "bone", "keyframes": ground_kfs}
        # Knuckle subtle ridge oscillation
        knuck_kfs = []
        for k in range(IDLE_STEPS + 1):
            t = (k / IDLE_STEPS) * IDLE_LEN
            ph = (t / 2.7) * 2 * math.pi + side
            knuck_kfs.append(make_keyframe("position", t,
                                            x=0,
                                            y=0.05 * math.sin(ph),
                                            z=0))
        idle[hand["knuckle_gu"]] = {"name": hand["children_meta"][hand["knuckle_gu"]]["name"], "type": "bone", "keyframes": knuck_kfs}
        # Wrist also gets keyframes for completeness
        wrist_kfs = []
        for k in range(IDLE_STEPS + 1):
            t = (k / IDLE_STEPS) * IDLE_LEN
            ph = (t / 2.3) * 2 * math.pi + side * 0.7
            wrist_kfs.append(make_keyframe("rotation", t,
                                            x=0.8 * math.sin(ph),
                                            y=0,
                                            z=0.5 * math.cos(ph)))
        idle[hand["wrist_gu"]] = {"name": hand["children_meta"][hand["wrist_gu"]]["name"], "type": "bone", "keyframes": wrist_kfs}
        # Palm subtle breathing
        palm_kfs = []
        for k in range(IDLE_STEPS + 1):
            t = (k / IDLE_STEPS) * IDLE_LEN
            ph = (t / 3.1) * 2 * math.pi + side * 0.5
            s = 1.0 + 0.015 * math.sin(ph)
            palm_kfs.append(make_keyframe("scale", t, x=s, y=s * 0.98, z=s))
        idle[hand["palm_gu"]] = {"name": hand["children_meta"][hand["palm_gu"]]["name"], "type": "bone", "keyframes": palm_kfs}

    b.add_animation("idle", length=IDLE_LEN, animators=idle, loop="loop", override=False)

    # ---- DISSIPATE: hands lift rapidly upward, fingers extend straight ----
    for hand in (left, right):
        side = hand["side_sign"]
        diss[hand["hand_gu"]] = {"name": hand["hand_g"]["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.20, x=0, y=0.5, z=0),  # tiny anticipation down
            make_keyframe("position", 0.80, x=0, y=18, z=0),   # rapid lift
        ]}
        diss[hand["chain_gu"]] = {"name": hand["chain_g"]["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.60, x=1, y=1.1, z=1),
            make_keyframe("scale", 0.80, x=1, y=0.05, z=1),  # chain retracts upward
        ]}
        diss[hand["ground_gu"]] = {"name": hand["ground_g"]["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.40, x=1.2, y=1, z=1.2),
            make_keyframe("scale", 0.80, x=0, y=0, z=0),
        ]}
        # Fingers extend straight on lift
        for fi, (fgu, fg) in enumerate(hand["fingers"]):
            diss[fgu] = {"name": fg["name"], "type": "bone", "keyframes": [
                make_keyframe("rotation", 0.0, x=-3, y=0, z=0),
                make_keyframe("rotation", 0.40, x=8, y=0, z=0),  # over-extend
                make_keyframe("rotation", 0.80, x=0, y=0, z=0),
            ]}
        diss[hand["thumb_gu"]] = {"name": hand["children_meta"][hand["thumb_gu"]]["name"], "type": "bone", "keyframes": [
            make_keyframe("rotation", 0.0, x=-3, y=side * -45, z=0),
            make_keyframe("rotation", 0.80, x=5, y=side * -30, z=0),
        ]}
    b.add_animation("dissipate", length=0.80, animators=diss, loop="once", override=True)

    b.write(OUTPUT)
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    build()
