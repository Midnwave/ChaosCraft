#!/usr/bin/env python3
"""
Generate teddy_hug_squeeze.bbmodel — Fluffy Mode ME attack.

A ring of 8 teddy bear arms reaching inward simultaneously toward the centre.
Each arm: upper -> elbow -> forearm -> hand cluster, with seam slabs and buttons.
Ground ring of 10 cream slabs marks the AOE.

Animations:
  - spawn (once, 0.9s, override=true)
  - idle (loop, 5.0s, override=false)
  - dissipate (once, 0.6s, override=true)
"""
import sys
import math
sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder, make_uuid, make_keyframe, basic_cube_faces, uniform_face,
    png_from_pixels, hex_to_rgba, fill_rect, gradient_radial,
    add_noise_overlay, color_lerp, lerp,
)

OUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/fluffy/me_attacks/teddy_hug_squeeze.bbmodel"

# ----------------------------- TEXTURES ---------------------------------- #

def make_fabric_texture():
    """Texture 0: warm honey-brown stuffed-animal fabric, 64x64."""
    W = H = 64
    # Palette
    base = hex_to_rgba("#c89060")
    plush_shadow = hex_to_rgba("#906840")
    seam_dark = hex_to_rgba("#604828")
    plush_high = hex_to_rgba("#e8b880")
    specular = hex_to_rgba("#fff0d8")
    emissive = hex_to_rgba("#ffc040")

    pixels = [base] * (W * H)

    # Soft radial shading: highlight centre, shadow edges (per 32x32 quadrant feel)
    for y in range(H):
        for x in range(W):
            # vertical band shading (top brighter)
            t_v = y / (H - 1)
            # horizontal subtle gradient
            t_h = abs(x - W / 2) / (W / 2)
            tone = (1.0 - t_v) * 0.5 + (1.0 - t_h) * 0.3
            if tone > 0.55:
                pixels[y * W + x] = color_lerp(base, plush_high, (tone - 0.55) * 1.6)
            elif tone < 0.30:
                pixels[y * W + x] = color_lerp(base, plush_shadow, (0.30 - tone) * 2.0)

    # Crosshatch fabric weave: every 4 rows + every 4 columns (seam_dark thin)
    for y in range(0, H, 4):
        for x in range(W):
            cur = pixels[y * W + x]
            pixels[y * W + x] = color_lerp(cur, seam_dark, 0.35)
    for x in range(0, W, 4):
        for y in range(H):
            cur = pixels[y * W + x]
            pixels[y * W + x] = color_lerp(cur, seam_dark, 0.35)

    # Plush highlight noise (warm fluff)
    add_noise_overlay(pixels, W, H, plush_shadow, plush_high, density=0.12, seed=2024)

    # Specular shine band — small bright spots in upper-left of each quadrant
    for cy, cx in [(8, 12), (8, 44), (40, 12), (40, 44)]:
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                d = math.sqrt(dy * dy + dx * dx)
                if d <= 2.5:
                    px = cx + dx
                    py = cy + dy
                    if 0 <= px < W and 0 <= py < H:
                        cur = pixels[py * W + px]
                        pixels[py * W + px] = color_lerp(cur, specular, 1.0 - d / 2.5)

    # Emissive 3px warm golden-orange dots at brightest zones (4 spots)
    for cy, cx in [(6, 10), (6, 46), (38, 10), (38, 46)]:
        for dy in range(-1, 2):
            for dx in range(-1, 2):
                px = cx + dx
                py = cy + dy
                if 0 <= px < W and 0 <= py < H:
                    pixels[py * W + px] = emissive

    # Seam diagonal lines — two faint diagonals representing stitching seams
    for i in range(W):
        y = (i + 8) % H
        x = i
        cur = pixels[y * W + x]
        pixels[y * W + x] = color_lerp(cur, seam_dark, 0.55)
        y2 = (H - 1 - i + 8) % H
        cur2 = pixels[y2 * W + x]
        pixels[y2 * W + x] = color_lerp(cur2, seam_dark, 0.45)

    return png_from_pixels(pixels, W, H)


def make_ground_texture():
    """Texture 1: soft cream ground with faint honey-brown ring at 60% radius."""
    W = H = 64
    cream = hex_to_rgba("#fff8f0")
    ring = hex_to_rgba("#d0a870")

    pixels = [cream] * (W * H)

    cx, cy = W / 2 - 0.5, H / 2 - 0.5
    target_r = 0.60 * (W / 2)

    for y in range(H):
        for x in range(W):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            # Faint band around target_r ± 4 px
            band_dist = abs(d - target_r)
            if band_dist < 4.0:
                t = 1.0 - band_dist / 4.0
                # max blend ~25% so it stays "very faint"
                pixels[y * W + x] = color_lerp(cream, ring, t * 0.25)

    # Very subtle plush noise
    plush_shadow = hex_to_rgba("#e8d8b8")
    plush_high = hex_to_rgba("#fffaf2")
    add_noise_overlay(pixels, W, H, plush_shadow, plush_high, density=0.04, seed=909)

    return png_from_pixels(pixels, W, H)


# ----------------------------- BUILD ------------------------------------- #

def build():
    b = Builder("teddy_hug_squeeze", resolution=(64, 64), visible_box=(4, 4, 0))

    # Add textures first so face indices are valid
    b.add_texture("teddy_fabric", make_fabric_texture())
    b.add_texture("teddy_ground", make_ground_texture())

    # ---------------- 8 ARM RING ---------------- #
    NUM_ARMS = 8
    RING_RADIUS = 12.0  # outer end of upper arm sits here (arms reach inward from this)

    arm_root_uuids = []          # for outliner
    arm_root_lookup = []         # ordered list: each entry is dict with bone uuids per arm
    # Each arm's bones we need to animate:
    #   shoulder (root translates inward), elbow (rotation), forearm (translation),
    #   hand (scale/translate), seam_a, seam_b (scale), btn_a, btn_b (translate)
    # The arm is built in local "arm frame": +X axis = inward direction.
    # A root bone at (cos*R, 1.5, sin*R) rotated around Y so that local +X points to centre.

    for i in range(NUM_ARMS):
        angle = (i / NUM_ARMS) * 2 * math.pi
        # outward unit position (away from centre)
        ox = math.cos(angle) * RING_RADIUS
        oz = math.sin(angle) * RING_RADIUS

        # === Build cubes in LOCAL space, then bones rotate them around Y ===
        # Local frame: arm extends along -X (toward centre). x=0 is shoulder,
        # x=-3 is elbow, x=-6 is wrist, x=-7.5 is hand.
        # Y is up. Z is sideways (cylindrical thickness).

        # --- Upper arm (local x in [-3, 0]) ---
        upper_uuid = b.add_cube(
            f"upper_arm_{i}",
            from_=[-3.0, -1.0, -1.0],
            to_=[0.0, 1.0, 1.0],
            faces=basic_cube_faces(0, 0, 3, 2, 2, tex_index=0),
            origin=[0.0, 0.0, 0.0],
        )
        # Seam slabs along upper arm (top and bottom thin flat strips)
        seam_upper_top = b.add_cube(
            f"seam_upper_top_{i}",
            from_=[-3.0, 0.95, -0.15],
            to_=[0.0, 1.05, 0.15],
            faces=uniform_face(0, 30, 16, 32, tex_index=0),
            origin=[-1.5, 1.0, 0.0],
        )
        seam_upper_bot = b.add_cube(
            f"seam_upper_bot_{i}",
            from_=[-3.0, -1.05, -0.15],
            to_=[0.0, -0.95, 0.15],
            faces=uniform_face(0, 30, 16, 32, tex_index=0),
            origin=[-1.5, -1.0, 0.0],
        )

        # --- Elbow joint cube (local x in [-3.4, -2.6]) ---
        elbow_uuid = b.add_cube(
            f"elbow_{i}",
            from_=[-3.4, -1.1, -1.1],
            to_=[-2.6, 1.1, 1.1],
            faces=basic_cube_faces(0, 0, 1, 2, 2, tex_index=0),
            origin=[-3.0, 0.0, 0.0],
        )

        # --- Forearm (local x in [-6, -3]) — slight bend will be applied to whole forearm bone ---
        forearm_uuid = b.add_cube(
            f"forearm_{i}",
            from_=[-6.0, -0.9, -0.9],
            to_=[-3.0, 0.9, 0.9],
            faces=basic_cube_faces(0, 0, 3, 2, 2, tex_index=0),
            origin=[-3.0, 0.0, 0.0],
        )
        seam_forearm_top = b.add_cube(
            f"seam_forearm_top_{i}",
            from_=[-6.0, 0.85, -0.15],
            to_=[-3.0, 0.95, 0.15],
            faces=uniform_face(0, 30, 16, 32, tex_index=0),
            origin=[-4.5, 0.9, 0.0],
        )
        seam_forearm_bot = b.add_cube(
            f"seam_forearm_bot_{i}",
            from_=[-6.0, -0.95, -0.15],
            to_=[-3.0, -0.85, 0.15],
            faces=uniform_face(0, 30, 16, 32, tex_index=0),
            origin=[-4.5, -0.9, 0.0],
        )

        # --- Buttons on elbow area (2 small flat decorative cubes) ---
        button_a = b.add_cube(
            f"button_a_{i}",
            from_=[-3.3, -0.4, 1.0],
            to_=[-2.7, 0.2, 1.2],
            faces=uniform_face(20, 20, 28, 28, tex_index=0),
            origin=[-3.0, -0.1, 1.1],
        )
        button_b = b.add_cube(
            f"button_b_{i}",
            from_=[-3.3, -0.4, -1.2],
            to_=[-2.7, 0.2, -1.0],
            faces=uniform_face(20, 20, 28, 28, tex_index=0),
            origin=[-3.0, -0.1, -1.1],
        )

        # --- Hand cluster: 2 cubes — palm + thumb ---
        palm_uuid = b.add_cube(
            f"palm_{i}",
            from_=[-7.5, -1.0, -1.0],
            to_=[-6.0, 1.0, 1.0],
            faces=basic_cube_faces(40, 0, 1, 2, 2, tex_index=0),
            origin=[-6.75, 0.0, 0.0],
        )
        thumb_uuid = b.add_cube(
            f"thumb_{i}",
            from_=[-7.0, 0.5, -1.6],
            to_=[-6.2, 1.5, -0.9],
            faces=basic_cube_faces(48, 0, 1, 1, 1, tex_index=0),
            origin=[-6.6, 1.0, -1.25],
        )

        # ====== BONE STRUCTURE ======
        # hand bone (animates scale)
        hand_bone_uuid = make_uuid()
        hand_group = b.make_group(
            f"hand_{i}",
            origin=[-6.75, 0.0, 0.0],
            children=[palm_uuid, thumb_uuid],
            group_uuid=hand_bone_uuid,
        )

        # button bones
        btn_a_bone_uuid = make_uuid()
        btn_a_group = b.make_group(
            f"btn_a_{i}",
            origin=[-3.0, -0.1, 1.1],
            children=[button_a],
            group_uuid=btn_a_bone_uuid,
        )
        btn_b_bone_uuid = make_uuid()
        btn_b_group = b.make_group(
            f"btn_b_{i}",
            origin=[-3.0, -0.1, -1.1],
            children=[button_b],
            group_uuid=btn_b_bone_uuid,
        )

        # seam upper / forearm bones (each own bone, animates scale)
        seam_upper_top_bone = make_uuid()
        seam_upper_top_group = b.make_group(
            f"seam_upper_top_{i}",
            origin=[-1.5, 1.0, 0.0],
            children=[seam_upper_top],
            group_uuid=seam_upper_top_bone,
        )
        seam_upper_bot_bone = make_uuid()
        seam_upper_bot_group = b.make_group(
            f"seam_upper_bot_{i}",
            origin=[-1.5, -1.0, 0.0],
            children=[seam_upper_bot],
            group_uuid=seam_upper_bot_bone,
        )
        seam_fore_top_bone = make_uuid()
        seam_fore_top_group = b.make_group(
            f"seam_fore_top_{i}",
            origin=[-4.5, 0.9, 0.0],
            children=[seam_forearm_top],
            group_uuid=seam_fore_top_bone,
        )
        seam_fore_bot_bone = make_uuid()
        seam_fore_bot_group = b.make_group(
            f"seam_fore_bot_{i}",
            origin=[-4.5, -0.9, 0.0],
            children=[seam_forearm_bot],
            group_uuid=seam_fore_bot_bone,
        )

        # forearm bone (origin at elbow, contains forearm cube + forearm seams + hand bone)
        forearm_bone_uuid = make_uuid()
        forearm_group = b.make_group(
            f"forearm_{i}",
            origin=[-3.0, 0.0, 0.0],
            children=[forearm_uuid, seam_fore_top_group, seam_fore_bot_group, hand_group],
            rotation=[0, 0, 0],
            group_uuid=forearm_bone_uuid,
        )

        # elbow bone — pivots forearm 22.5° (catches the bend). Origin at elbow joint.
        # Bend axis: rotate forearm -22.5° on Z so wrist droops slightly downward.
        elbow_bone_uuid = make_uuid()
        elbow_group = b.make_group(
            f"elbow_{i}",
            origin=[-3.0, 0.0, 0.0],
            children=[elbow_uuid, forearm_group, btn_a_group, btn_b_group],
            rotation=[0, 0, -22.5],
            group_uuid=elbow_bone_uuid,
        )

        # upper arm bone (contains upper arm cube + upper seams + elbow group)
        upper_bone_uuid = make_uuid()
        upper_group = b.make_group(
            f"upper_{i}",
            origin=[0.0, 0.0, 0.0],
            children=[upper_uuid, seam_upper_top_group, seam_upper_bot_group, elbow_group],
            group_uuid=upper_bone_uuid,
        )

        # Shoulder root — handles arm position (inward translation animation).
        # The root sits at outward position (ox, 1.5, oz) with rotation pointing inward.
        # Local +X = inward direction. The cubes have their forearm/hand at NEGATIVE local X,
        # so the local axis convention here: arm extends in -X direction. To make the arm
        # point toward the centre from (ox, 0, oz), we rotate around Y by (angle in degrees + 180).
        # angle_deg measured from +X axis CCW. We want the model's -X local axis to point
        # toward centre i.e. opposite of (ox, oz). So rotate by angle_deg.
        angle_deg = math.degrees(angle)
        shoulder_uuid = make_uuid()
        shoulder_group = b.make_group(
            f"shoulder_{i}",
            origin=[ox, 1.5, oz],
            children=[upper_group],
            rotation=[0, angle_deg, 0],
            group_uuid=shoulder_uuid,
        )

        b.outliner.append(shoulder_group)
        arm_root_uuids.append(shoulder_uuid)

        arm_root_lookup.append({
            "shoulder": shoulder_uuid,
            "upper": upper_bone_uuid,
            "elbow": elbow_bone_uuid,
            "forearm": forearm_bone_uuid,
            "hand": hand_bone_uuid,
            "btn_a": btn_a_bone_uuid,
            "btn_b": btn_b_bone_uuid,
            "seam_upper_top": seam_upper_top_bone,
            "seam_upper_bot": seam_upper_bot_bone,
            "seam_fore_top": seam_fore_top_bone,
            "seam_fore_bot": seam_fore_bot_bone,
            "ox": ox,
            "oz": oz,
            "angle_deg": angle_deg,
        })

    # ---------------- GROUND RING (10 flat slabs) ---------------- #
    NUM_RING = 10
    RING_OUTER = 13.0
    ring_children = []
    for i in range(NUM_RING):
        a = (i / NUM_RING) * 2 * math.pi
        cx = math.cos(a) * RING_OUTER
        cz = math.sin(a) * RING_OUTER
        slab = b.add_cube(
            f"ground_slab_{i}",
            from_=[cx - 1.6, 0.0, cz - 0.6],
            to_=[cx + 1.6, 0.2, cz + 0.6],
            faces=uniform_face(0, 0, 64, 64, tex_index=1),
            origin=[cx, 0.1, cz],
            rotation=[0, math.degrees(a), 0],
        )
        ring_children.append(slab)

    ground_bone_uuid = make_uuid()
    ground_group = b.make_group(
        "ground_ring",
        origin=[0, 0, 0],
        children=ring_children,
        group_uuid=ground_bone_uuid,
    )
    b.outliner.append(ground_group)

    # ============== ANIMATIONS ============== #

    # ---------------- SPAWN (0.9s, once, override=true) ---------------- #
    # Arms start fully extended outward (translated outward in their local +X by +5).
    # 0.0s: slight inward (anticipation) -> 0.0 translation toward centre
    # 0.2s: arms begin moving inward -> -1.5 in local +X (toward centre)
    # 0.7s: converged (translate to inner converged) -> -8.0 along local +X means more inward
    # 0.9s: hands compress slightly, buttons pop outward
    #
    # Local convention: shoulder is at outward ring position. The arm geometry extends
    # toward NEGATIVE local X (which is inward thanks to rotation). So translating the
    # SHOULDER bone in WORLD space along (-cos(angle), 0, -sin(angle)) brings arm inward.
    # ModelEngine bone position keyframes are in WORLD space relative to bone origin.

    spawn_animators = {}
    SPAWN_LEN = 0.9
    for i, arm in enumerate(arm_root_lookup):
        ox = arm["ox"]
        oz = arm["oz"]
        # Inward unit direction (toward centre): (-ox, 0, -oz) normalised
        inv = math.sqrt(ox * ox + oz * oz)
        ix = -ox / inv
        iz = -oz / inv

        # Stagger phase per arm so the hug isn't perfectly synchronised
        phase = (i % 4) * 0.03

        # Start: extended outward by +5 in OUTWARD direction (i.e. translation = (-ix*5, 0, -iz*5))
        out_start = (-ix * 5.0, 0.0, -iz * 5.0)
        # 0.0s anticipation: come back ~0.5 (still outward)
        anticipation = (-ix * 4.0, 0.0, -iz * 4.0)
        # 0.2s + phase: begin moving inward (zero translation = original ring position)
        begin = (0.0, 0.0, 0.0)
        # 0.7s: converged inward
        converged = (ix * 6.0, 0.0, iz * 6.0)
        # 0.9s: hold + slight further squeeze
        hold = (ix * 6.5, -0.5, iz * 6.5)

        shoulder_kfs = [
            make_keyframe("position", 0.0, *out_start, interpolation="linear"),
            make_keyframe("position", 0.05 + phase, *anticipation, interpolation="catmullrom"),
            make_keyframe("position", 0.20 + phase, *begin, interpolation="catmullrom"),
            make_keyframe("position", 0.70 + phase, *converged, interpolation="catmullrom"),
            make_keyframe("position", SPAWN_LEN, *hold, interpolation="catmullrom"),
        ]
        # Shoulder rotation: small wiggle for warmth
        shoulder_kfs += [
            make_keyframe("rotation", 0.0, 0, 0, 5, interpolation="linear"),
            make_keyframe("rotation", 0.5, 0, 0, -3, interpolation="catmullrom"),
            make_keyframe("rotation", SPAWN_LEN, 0, 0, 0, interpolation="catmullrom"),
        ]
        spawn_animators[arm["shoulder"]] = {
            "name": f"shoulder_{i}",
            "type": "bone",
            "keyframes": shoulder_kfs,
        }

        # Elbow bend — start straighter (-10°), bend to -22.5° as hug closes
        elbow_kfs = [
            make_keyframe("rotation", 0.0, 0, 0, 12, interpolation="linear"),
            make_keyframe("rotation", 0.20, 0, 0, 6, interpolation="catmullrom"),
            make_keyframe("rotation", 0.55, 0, 0, -10, interpolation="catmullrom"),
            make_keyframe("rotation", SPAWN_LEN, 0, 0, -5, interpolation="catmullrom"),
        ]
        spawn_animators[arm["elbow"]] = {
            "name": f"elbow_{i}",
            "type": "bone",
            "keyframes": elbow_kfs,
        }

        # Hand: compress slightly when reaching centre
        hand_kfs = [
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
            make_keyframe("scale", 0.50, 1.05, 1.05, 1.05, interpolation="catmullrom"),
            make_keyframe("scale", 0.75, 0.85, 1.10, 0.85, interpolation="catmullrom"),
            make_keyframe("scale", SPAWN_LEN, 0.95, 1.0, 0.95, interpolation="catmullrom"),
        ]
        spawn_animators[arm["hand"]] = {
            "name": f"hand_{i}",
            "type": "bone",
            "keyframes": hand_kfs,
        }

        # Buttons pop outward on arrival
        for key in ("btn_a", "btn_b"):
            sign = 1.0 if key == "btn_a" else -1.0  # btn_a is on +Z side, btn_b on -Z
            btn_kfs = [
                make_keyframe("position", 0.0, 0, 0, 0, interpolation="linear"),
                make_keyframe("position", 0.65, 0, 0, sign * 0.0, interpolation="catmullrom"),
                make_keyframe("position", 0.78, 0, 0.2, sign * 0.4, interpolation="catmullrom"),
                make_keyframe("position", SPAWN_LEN, 0, 0.1, sign * 0.25, interpolation="catmullrom"),
            ]
            spawn_animators[arm[key]] = {
                "name": f"{key}_{i}",
                "type": "bone",
                "keyframes": btn_kfs,
            }

        # Seam slabs subtle scale during spawn
        for key in ("seam_upper_top", "seam_upper_bot", "seam_fore_top", "seam_fore_bot"):
            seam_kfs = [
                make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
                make_keyframe("scale", 0.4, 1.0, 1.0, 1.2, interpolation="catmullrom"),
                make_keyframe("scale", SPAWN_LEN, 1.0, 1.0, 1.0, interpolation="catmullrom"),
            ]
            spawn_animators[arm[key]] = {
                "name": f"{key}_{i}",
                "type": "bone",
                "keyframes": seam_kfs,
            }

    # Ground ring — rises into existence
    ground_spawn_kfs = [
        make_keyframe("scale", 0.0, 0.0, 0.0, 0.0, interpolation="linear"),
        make_keyframe("scale", 0.3, 0.6, 0.4, 0.6, interpolation="catmullrom"),
        make_keyframe("scale", 0.7, 1.05, 1.0, 1.05, interpolation="catmullrom"),
        make_keyframe("scale", SPAWN_LEN, 1.0, 1.0, 1.0, interpolation="catmullrom"),
        make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="linear"),
        make_keyframe("rotation", SPAWN_LEN, 0, 25, 0, interpolation="catmullrom"),
    ]
    spawn_animators[ground_bone_uuid] = {
        "name": "ground_ring",
        "type": "bone",
        "keyframes": ground_spawn_kfs,
    }

    b.add_animation("spawn", length=SPAWN_LEN, animators=spawn_animators,
                    loop="once", override=True)

    # ---------------- IDLE (5.0s, loop, override=false) ---------------- #
    # Arms slowly pulse inward/outward ±1.5 units. Seam slabs subtle scale pulse.
    # Hands open and close slightly. Ground ring rotates very slowly.

    idle_animators = {}
    IDLE_LEN = 5.0
    for i, arm in enumerate(arm_root_lookup):
        ox = arm["ox"]
        oz = arm["oz"]
        inv = math.sqrt(ox * ox + oz * oz)
        ix = -ox / inv
        iz = -oz / inv

        # phase per arm — staggered so hug feels alive
        phase = (i / NUM_ARMS) * IDLE_LEN

        def pos_at(t):
            local_t = (t + phase) % IDLE_LEN
            cycle = math.sin((local_t / IDLE_LEN) * 2 * math.pi)
            mag = ix * 1.5 * cycle, 0.2 * math.sin((local_t / IDLE_LEN) * 4 * math.pi), iz * 1.5 * cycle
            return mag

        # Build pulse — start at base (converged hold), pulse forward and backward
        # Note: idle layers on TOP of spawn end-state, so this is RELATIVE motion.
        idle_shoulder_kfs = []
        for tick in [0.0, 1.0, 2.0, 2.5, 3.0, 4.0, 5.0]:
            x, y, z = pos_at(tick)
            idle_shoulder_kfs.append(
                make_keyframe("position", tick, x, y, z, interpolation="catmullrom")
            )
        idle_animators[arm["shoulder"]] = {
            "name": f"shoulder_{i}",
            "type": "bone",
            "keyframes": idle_shoulder_kfs,
        }

        # Hand open/close pulse
        hand_idle_kfs = [
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="catmullrom"),
            make_keyframe("scale", 1.25 + (i % 3) * 0.1, 1.1, 0.95, 1.1, interpolation="catmullrom"),
            make_keyframe("scale", 2.5, 0.95, 1.05, 0.95, interpolation="catmullrom"),
            make_keyframe("scale", 3.75, 1.1, 0.95, 1.1, interpolation="catmullrom"),
            make_keyframe("scale", IDLE_LEN, 1.0, 1.0, 1.0, interpolation="catmullrom"),
        ]
        idle_animators[arm["hand"]] = {
            "name": f"hand_{i}",
            "type": "bone",
            "keyframes": hand_idle_kfs,
        }

        # Elbow gentle flex
        elbow_idle_kfs = [
            make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="catmullrom"),
            make_keyframe("rotation", 1.5 + (i % 4) * 0.15, 0, 0, -3, interpolation="catmullrom"),
            make_keyframe("rotation", 3.0, 0, 0, 4, interpolation="catmullrom"),
            make_keyframe("rotation", IDLE_LEN, 0, 0, 0, interpolation="catmullrom"),
        ]
        idle_animators[arm["elbow"]] = {
            "name": f"elbow_{i}",
            "type": "bone",
            "keyframes": elbow_idle_kfs,
        }

        # Seam slabs subtle scale pulse
        for k_idx, key in enumerate(("seam_upper_top", "seam_upper_bot", "seam_fore_top", "seam_fore_bot")):
            base_t = 0.5 + (k_idx + i) * 0.2
            seam_kfs = [
                make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="catmullrom"),
                make_keyframe("scale", base_t % IDLE_LEN, 1.0, 1.0, 1.15, interpolation="catmullrom"),
                make_keyframe("scale", (base_t + 2.0) % IDLE_LEN, 1.0, 1.0, 0.85, interpolation="catmullrom"),
                make_keyframe("scale", IDLE_LEN, 1.0, 1.0, 1.0, interpolation="catmullrom"),
            ]
            # ensure keyframes are sorted by time
            seam_kfs.sort(key=lambda k: k["time"])
            idle_animators[arm[key]] = {
                "name": f"{key}_{i}",
                "type": "bone",
                "keyframes": seam_kfs,
            }

    # Ground ring slow rotation
    ground_idle_kfs = [
        make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="linear"),
        make_keyframe("rotation", IDLE_LEN, 0, 30, 0, interpolation="linear"),
    ]
    idle_animators[ground_bone_uuid] = {
        "name": "ground_ring",
        "type": "bone",
        "keyframes": ground_idle_kfs,
    }

    b.add_animation("idle", length=IDLE_LEN, animators=idle_animators,
                    loop="loop", override=False)

    # ---------------- DISSIPATE (0.6s, once, override=true) ---------------- #
    # Arms extend back outward (follow-through), scale to 0 from hand to shoulder.
    # Ground ring contracts and snaps.

    dissipate_animators = {}
    DISS_LEN = 0.6
    for i, arm in enumerate(arm_root_lookup):
        ox = arm["ox"]
        oz = arm["oz"]
        inv = math.sqrt(ox * ox + oz * oz)
        ix = -ox / inv
        iz = -oz / inv

        # extend outward
        diss_shoulder_kfs = [
            make_keyframe("position", 0.0, ix * 6.0, 0.0, iz * 6.0, interpolation="linear"),
            make_keyframe("position", 0.15, ix * 4.0, 0.5, iz * 4.0, interpolation="catmullrom"),
            make_keyframe("position", 0.4, -ix * 3.0, 1.5, -iz * 3.0, interpolation="catmullrom"),
            make_keyframe("position", DISS_LEN, -ix * 6.0, 2.5, -iz * 6.0, interpolation="catmullrom"),
        ]
        dissipate_animators[arm["shoulder"]] = {
            "name": f"shoulder_{i}",
            "type": "bone",
            "keyframes": diss_shoulder_kfs,
        }

        # Hand collapses first (reverse materialise from hand outward)
        hand_diss_kfs = [
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
            make_keyframe("scale", 0.15, 0.5, 0.5, 0.5, interpolation="catmullrom"),
            make_keyframe("scale", 0.25, 0.0, 0.0, 0.0, interpolation="catmullrom"),
            make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="linear"),
        ]
        dissipate_animators[arm["hand"]] = {
            "name": f"hand_{i}",
            "type": "bone",
            "keyframes": hand_diss_kfs,
        }

        # Forearm collapses second
        forearm_diss_kfs = [
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
            make_keyframe("scale", 0.30, 0.7, 0.7, 0.7, interpolation="catmullrom"),
            make_keyframe("scale", 0.45, 0.0, 0.0, 0.0, interpolation="catmullrom"),
            make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="linear"),
        ]
        dissipate_animators[arm["forearm"]] = {
            "name": f"forearm_{i}",
            "type": "bone",
            "keyframes": forearm_diss_kfs,
        }

        # Upper arm collapses last (shoulder side)
        upper_diss_kfs = [
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
            make_keyframe("scale", 0.45, 0.85, 0.85, 0.85, interpolation="catmullrom"),
            make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="catmullrom"),
        ]
        dissipate_animators[arm["upper"]] = {
            "name": f"upper_{i}",
            "type": "bone",
            "keyframes": upper_diss_kfs,
        }

        # Buttons fall + fade
        for key in ("btn_a", "btn_b"):
            sign = 1.0 if key == "btn_a" else -1.0
            btn_kfs = [
                make_keyframe("position", 0.0, 0, 0.1, sign * 0.25, interpolation="linear"),
                make_keyframe("position", DISS_LEN, 0, -2.0, sign * 0.5, interpolation="catmullrom"),
                make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
                make_keyframe("scale", 0.2, 1.2, 1.2, 1.2, interpolation="catmullrom"),
                make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="catmullrom"),
            ]
            dissipate_animators[arm[key]] = {
                "name": f"{key}_{i}",
                "type": "bone",
                "keyframes": btn_kfs,
            }

        # Seam slabs collapse with arm sections
        for key in ("seam_upper_top", "seam_upper_bot"):
            seam_kfs = [
                make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
                make_keyframe("scale", 0.4, 0.5, 0.5, 0.5, interpolation="catmullrom"),
                make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="catmullrom"),
            ]
            dissipate_animators[arm[key]] = {
                "name": f"{key}_{i}",
                "type": "bone",
                "keyframes": seam_kfs,
            }
        for key in ("seam_fore_top", "seam_fore_bot"):
            seam_kfs = [
                make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
                make_keyframe("scale", 0.25, 0.5, 0.5, 0.5, interpolation="catmullrom"),
                make_keyframe("scale", 0.4, 0.0, 0.0, 0.0, interpolation="catmullrom"),
                make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="linear"),
            ]
            dissipate_animators[arm[key]] = {
                "name": f"{key}_{i}",
                "type": "bone",
                "keyframes": seam_kfs,
            }

        # Elbow opens up as it dissipates
        elbow_diss_kfs = [
            make_keyframe("rotation", 0.0, 0, 0, -5, interpolation="linear"),
            make_keyframe("rotation", 0.3, 0, 0, 25, interpolation="catmullrom"),
            make_keyframe("rotation", DISS_LEN, 0, 0, 45, interpolation="catmullrom"),
        ]
        dissipate_animators[arm["elbow"]] = {
            "name": f"elbow_{i}",
            "type": "bone",
            "keyframes": elbow_diss_kfs,
        }

    # Ground ring contracts and snaps
    ground_diss_kfs = [
        make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
        make_keyframe("scale", 0.35, 0.5, 1.5, 0.5, interpolation="catmullrom"),
        make_keyframe("scale", 0.5, 1.3, 0.2, 1.3, interpolation="catmullrom"),
        make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="catmullrom"),
        make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="linear"),
        make_keyframe("rotation", DISS_LEN, 0, -60, 0, interpolation="catmullrom"),
    ]
    dissipate_animators[ground_bone_uuid] = {
        "name": "ground_ring",
        "type": "bone",
        "keyframes": ground_diss_kfs,
    }

    b.add_animation("dissipate", length=DISS_LEN, animators=dissipate_animators,
                    loop="once", override=True)

    return b


def main():
    b = build()
    out = b.write(OUT_PATH)
    import os
    sz = os.path.getsize(out)
    print(f"WROTE {out}")
    print(f"size_bytes={sz}  size_kb={sz//1024}")
    print(f"elements={len(b.elements)}  outliner_roots={len(b.outliner)}  animations={len(b.animations)}")


if __name__ == "__main__":
    main()
