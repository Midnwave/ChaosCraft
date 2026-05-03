#!/usr/bin/env python3
"""
Generate candy_cannon_hyperglycemic.bbmodel — CANDY CANNON SHOT VFX for Fluffy Mode.

A large wrapped hard candy fired like a cannonball — cylindrical body with twisted
wrapper ends that flap independently in the air, helical swirl strips, and a comet
trail of sparkle dust. Pastel pink + mint green + cream cellophane.
"""
import sys
import math
import random
sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder, make_uuid, make_keyframe, basic_cube_faces, uniform_face,
    png_from_pixels, hex_to_rgba,
)


# ---------------------------------------------------------------------------
# TEXTURES
# ---------------------------------------------------------------------------
def build_candy_body_texture():
    """64x64 hard candy swirl — pastel pink with diagonal bright stripes,
    deep pink shadows, white speculars, and emissive mint at intersections."""
    w, h = 64, 64
    base       = hex_to_rgba("#ffc8d8")  # pastel pink base
    mid        = hex_to_rgba("#f0a0b8")  # saturated mid pink
    shadow     = hex_to_rgba("#d07090")  # deep pink shadow
    highlight  = hex_to_rgba("#fff0f8")  # bright candy highlight
    spec       = hex_to_rgba("#ffffff")  # specular white
    emis_mint  = hex_to_rgba("#b8ffd0")  # mint green swirl
    between    = hex_to_rgba("#e8a8c0")  # darker pink between stripes

    pixels = [base] * (w * h)

    # Slight darker pink "between stripe" base
    for y in range(h):
        for x in range(w):
            pixels[y * w + x] = between

    # Diagonal bright stripes at ~30 degrees: x + y*tan(30) ~ x + y*0.577
    # Stripe pattern repeating every 8px — 3px bright, 5px gap (between)
    for y in range(h):
        for x in range(w):
            diag = (x + int(y * 0.577)) % 8
            if diag == 0:
                pixels[y * w + x] = mid
            elif diag in (1, 2):
                pixels[y * w + x] = base
            elif diag == 3:
                pixels[y * w + x] = highlight  # bright pink stripe centre
            # 4..7: leave as 'between' (darker)

    # Deep pink shadow ridges — ~30 degree offset from stripes (every 12px)
    for y in range(h):
        for x in range(w):
            diag = (x + int(y * 0.577)) % 12
            if diag == 6:
                pixels[y * w + x] = shadow

    # Specular highlights — small bright zones
    rng = random.Random(303)
    for cx, cy in [(10, 8), (44, 22), (28, 40), (54, 50), (16, 56), (38, 6)]:
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                d = math.sqrt(dx*dx + dy*dy)
                if d <= 1.5:
                    px = cx + dx
                    py = cy + dy
                    if 0 <= px < w and 0 <= py < h:
                        pixels[py * w + px] = spec

    # Emissive mint green at stripe intersections — 3px clusters
    intersections = [
        (8, 12), (24, 8), (40, 14), (56, 18),
        (14, 28), (32, 24), (48, 30), (60, 36),
        (6, 44), (22, 48), (38, 42), (54, 56),
        (16, 60), (44, 60),
    ]
    for cx, cy in intersections:
        for dy in range(-1, 2):
            for dx in range(-1, 2):
                d = abs(dx) + abs(dy)
                if d <= 2:
                    px = cx + dx
                    py = cy + dy
                    if 0 <= px < w and 0 <= py < h:
                        pixels[py * w + px] = emis_mint

    # Sparkle speckles for fluffy candy look
    for _ in range(140):
        x = rng.randrange(w)
        y = rng.randrange(h)
        roll = rng.random()
        if roll < 0.4:
            pixels[y * w + x] = highlight
        elif roll < 0.7:
            pixels[y * w + x] = mid
        else:
            pixels[y * w + x] = shadow

    return png_from_pixels(pixels, w, h)


def build_wrapper_texture():
    """64x64 cream cellophane — alternating bright/dim shimmer with golden emissive."""
    w, h = 64, 64
    base       = hex_to_rgba("#fff8e0")  # cream cellophane base
    dim        = hex_to_rgba("#f0e8c8")  # slightly less bright shimmer
    deep       = hex_to_rgba("#d8c890")  # deeper cream shadow
    bright     = hex_to_rgba("#fffce8")  # brightest shimmer zone
    gold       = hex_to_rgba("#ffe880")  # golden sparkle (emissive)
    gold_hot   = hex_to_rgba("#fff0a0")  # bright gold core
    edge       = hex_to_rgba("#e0d0a0")  # cellophane edge tone

    pixels = [base] * (w * h)

    # Alternating bright/dim every 2px — shimmer pattern
    for y in range(h):
        for x in range(w):
            if ((x // 2) + (y // 2)) % 2 == 0:
                pixels[y * w + x] = base
            else:
                pixels[y * w + x] = dim

    # Crinkle creases — diagonal lines (cellophane wrinkle texture)
    for y in range(h):
        for x in range(w):
            d1 = (x - y) % 9
            d2 = (x + y) % 11
            if d1 == 0:
                pixels[y * w + x] = deep
            elif d2 == 0:
                pixels[y * w + x] = edge

    # Brightest shimmer bands — vertical stripes at certain x ranges
    for x_band in [12, 32, 50]:
        for y in range(h):
            for x in range(x_band - 1, x_band + 2):
                if 0 <= x < w:
                    pixels[y * w + x] = bright

    # Emissive golden sparkle clusters at brightest zones
    sparkle_centers = [
        (12, 10), (12, 28), (12, 46),
        (32, 6), (32, 24), (32, 42), (32, 58),
        (50, 14), (50, 32), (50, 52),
        (4, 20), (4, 50), (60, 18), (60, 48),
    ]
    rng = random.Random(404)
    for cx, cy in sparkle_centers:
        # 3px gold core
        for dy in range(-1, 2):
            for dx in range(-1, 2):
                px = cx + dx
                py = cy + dy
                if 0 <= px < w and 0 <= py < h:
                    if dx == 0 and dy == 0:
                        pixels[py * w + px] = gold_hot
                    else:
                        pixels[py * w + px] = gold

    # Random tiny gold sparkle dots
    for _ in range(50):
        x = rng.randrange(w)
        y = rng.randrange(h)
        pixels[y * w + x] = gold

    # Subtle noise
    for _ in range(80):
        x = rng.randrange(w)
        y = rng.randrange(h)
        pixels[y * w + x] = bright if rng.random() < 0.5 else deep

    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# BUILD MODEL
# ---------------------------------------------------------------------------
def build():
    b = Builder("candy_cannon_hyperglycemic", resolution=(64, 64), visible_box=(6, 6, 0))

    # Textures
    b.add_texture("candy_body", build_candy_body_texture())
    b.add_texture("candy_wrapper", build_wrapper_texture())

    body_faces   = lambda sx, sy, sz: basic_cube_faces(0, 0, sx, sy, sz, tex_index=0)
    wrap_faces   = lambda sx, sy, sz: basic_cube_faces(0, 0, sx, sy, sz, tex_index=1)

    # ----- CANDY CYLINDER BODY: 3 stacked cubes (slightly overlapping) -----
    # Cylinder oriented along Y axis. Wide in X and Z (~2.5), shorter in Y (~3 total).
    # Three cubes overlap to suggest roundness.
    body_bones = []
    body_configs = [
        # name, from, to, rotation
        ("body_top",    [-1.25, 0.5,  -1.25], [1.25, 1.6,  1.25], [0, 0, 0]),
        ("body_mid",    [-1.4,  -0.5, -1.4 ], [1.4,  0.6,  1.4 ], [0, 18, 0]),
        ("body_bottom", [-1.25, -1.6, -1.25], [1.25, -0.4, 1.25], [0, -22, 0]),
    ]
    for name, frm, to_, rot in body_configs:
        sx = max(1, int(round(to_[0] - frm[0])))
        sy = max(1, int(round(to_[1] - frm[1])))
        sz = max(1, int(round(to_[2] - frm[2])))
        cube_uuid = b.add_cube(name, frm, to_,
                               body_faces(sx, sy, sz),
                               origin=[0, 0, 0], rotation=rot)
        bone_uuid = make_uuid()
        bone = b.make_group(name, [0, 0, 0], [cube_uuid],
                            rotation=rot, group_uuid=bone_uuid)
        body_bones.append((name, bone_uuid, cube_uuid, bone))

    # ----- WRAPPER ENDS: TOP — 4 flat slab "twist" pieces fanning outward -----
    wrapper_top_bones = []
    n_twist = 4
    for i in range(n_twist):
        angle = (2 * math.pi * i) / n_twist
        cos_a = math.cos(angle)
        sin_a = math.sin(angle)
        # Slab extends radially outward from top of cylinder
        # Local cube: thin in Y, long in X (radial), narrow in Z
        frm = [0.0, 0.0, -0.5]
        to_ = [2.2, 0.35, 0.5]
        sx, sy, sz = 2, 1, 1
        cube_uuid = b.add_cube(f"wrap_top_{i}", frm, to_,
                               wrap_faces(sx, sy, sz),
                               origin=[0, 0, 0], rotation=[0, 0, 0])
        # Bone origin at top of cylinder, rotated about Y to fan
        yaw = math.degrees(angle)
        # Slight upward tilt so they look "twisted up"
        pitch = -25
        bone_uuid = make_uuid()
        bone = b.make_group(f"wrap_top_{i}_bone",
                            [0, 1.7, 0],
                            [cube_uuid],
                            rotation=[pitch, yaw, 0],
                            group_uuid=bone_uuid)
        wrapper_top_bones.append((f"wrap_top_{i}", bone_uuid, cube_uuid, bone, i))

    # Wrapper top group root — pivot for the entire top-end fan
    wrapper_top_group_uuid = make_uuid()
    wrapper_top_group = b.make_group("wrapper_top",
                                     [0, 1.7, 0],
                                     [bone for (_, _, _, bone, _) in wrapper_top_bones],
                                     group_uuid=wrapper_top_group_uuid)

    # ----- WRAPPER ENDS: BOTTOM — same but pointing the other direction -----
    wrapper_bot_bones = []
    for i in range(n_twist):
        angle = (2 * math.pi * i) / n_twist + math.pi / n_twist  # offset from top
        frm = [0.0, -0.35, -0.5]
        to_ = [2.2, 0.0, 0.5]
        sx, sy, sz = 2, 1, 1
        cube_uuid = b.add_cube(f"wrap_bot_{i}", frm, to_,
                               wrap_faces(sx, sy, sz),
                               origin=[0, 0, 0], rotation=[0, 0, 0])
        yaw = math.degrees(angle)
        pitch = 25  # tilt downward
        bone_uuid = make_uuid()
        bone = b.make_group(f"wrap_bot_{i}_bone",
                            [0, -1.7, 0],
                            [cube_uuid],
                            rotation=[pitch, yaw, 0],
                            group_uuid=bone_uuid)
        wrapper_bot_bones.append((f"wrap_bot_{i}", bone_uuid, cube_uuid, bone, i))

    wrapper_bot_group_uuid = make_uuid()
    wrapper_bot_group = b.make_group("wrapper_bot",
                                     [0, -1.7, 0],
                                     [bone for (_, _, _, bone, _) in wrapper_bot_bones],
                                     group_uuid=wrapper_bot_group_uuid)

    # ----- HELICAL SWIRL STRIPS: 4 thin slabs at 90 degree offsets, varying heights -----
    swirl_bones = []
    n_swirl = 4
    swirl_heights = [-0.9, -0.3, 0.3, 0.9]
    for i in range(n_swirl):
        # Each strip wraps around the cylinder body. Approximate as a thin radial slab.
        angle_offset = (2 * math.pi * i) / n_swirl
        h_y = swirl_heights[i]
        # Local cube: thin in Y, wide tangential (Z), wraps around radius ~1.5
        frm = [-0.15, -0.15, -1.6]
        to_ = [0.15, 0.15, 1.6]
        sx, sy, sz = 1, 1, 3
        cube_uuid = b.add_cube(f"swirl_{i}", frm, to_,
                               body_faces(sx, sy, sz),
                               origin=[0, 0, 0], rotation=[0, 0, 0])
        # Bone places at correct height with rotation around Y
        yaw = math.degrees(angle_offset)
        # Helical tilt — each successive strip tilts slightly more
        roll = 12 + i * 6
        bone_uuid = make_uuid()
        bone = b.make_group(f"swirl_{i}_bone",
                            [0, h_y, 0],
                            [cube_uuid],
                            rotation=[0, yaw, roll],
                            group_uuid=bone_uuid)
        swirl_bones.append((f"swirl_{i}", bone_uuid, cube_uuid, bone, i, yaw, roll))

    # ----- TRAILING SPARKLE COMET TAIL: 8 small cubes behind candy -----
    # Spec says 8; we keep exactly 8 here.
    sparkle_bones = []
    n_sparkle = 8
    for i in range(n_sparkle):
        # Position behind candy along negative Z axis, with offset/scatter
        z_back = -2.5 - i * 0.9
        x_off = 0.4 * math.sin(i * 1.3)
        y_off = 0.3 * math.cos(i * 0.9)
        size = max(0.18, 0.5 - i * 0.04)
        frm = [-size, -size, -size]
        to_ = [size, size, size]
        cube_uuid = b.add_cube(f"sparkle_{i}", frm, to_,
                               wrap_faces(1, 1, 1),
                               origin=[0, 0, 0], rotation=[0, 0, 0])
        bone_uuid = make_uuid()
        bone = b.make_group(f"sparkle_{i}_bone",
                            [x_off, y_off, z_back],
                            [cube_uuid],
                            rotation=[0, 0, 0],
                            group_uuid=bone_uuid)
        sparkle_bones.append((f"sparkle_{i}", bone_uuid, cube_uuid, bone, i,
                              x_off, y_off, z_back))

    # ----- ROOT GROUP — assemble everything -----
    body_group_uuid = make_uuid()
    body_group = b.make_group("body", [0, 0, 0],
                              [bone for (_, _, _, bone) in body_bones],
                              group_uuid=body_group_uuid)

    swirl_group_uuid = make_uuid()
    swirl_group = b.make_group("swirls", [0, 0, 0],
                               [bone for (_, _, _, bone, _, _, _) in swirl_bones],
                               group_uuid=swirl_group_uuid)

    sparkle_group_uuid = make_uuid()
    sparkle_group = b.make_group("sparkles", [0, 0, 0],
                                 [bone for (_, _, _, bone, _, _, _, _) in sparkle_bones],
                                 group_uuid=sparkle_group_uuid)

    root_uuid = b.add_root_group("root", [0, 0, 0],
                                 [body_group, wrapper_top_group, wrapper_bot_group,
                                  swirl_group, sparkle_group])

    # =======================================================================
    # ANIMATIONS
    # =======================================================================

    # ---- SPAWN: 0.25s — assemble from centre outward ----
    spawn_animators = {}

    # Root — slight scale pop
    root_kfs = []
    root_kfs.append(make_keyframe("scale", 0.0, 0.0, 0.0, 0.0, "linear"))
    root_kfs.append(make_keyframe("scale", 0.05, 0.4, 0.4, 0.4, "linear"))
    root_kfs.append(make_keyframe("scale", 0.15, 1.15, 1.15, 1.15, "linear"))
    root_kfs.append(make_keyframe("scale", 0.20, 0.95, 0.95, 0.95, "linear"))
    root_kfs.append(make_keyframe("scale", 0.25, 1.0, 1.0, 1.0, "linear"))
    spawn_animators[root_uuid] = {"name": "root", "type": "bone", "keyframes": root_kfs}

    # BODY cubes — assemble first (0..0.10s)
    for idx, (name, bone_uuid, _, _) in enumerate(body_bones):
        kfs = []
        delay = idx * 0.015
        kfs.append(make_keyframe("scale", 0.0, 0.0, 0.0, 0.0, "linear"))
        kfs.append(make_keyframe("scale", 0.0 + delay, 0.0, 0.0, 0.0, "linear"))
        kfs.append(make_keyframe("scale", 0.06 + delay, 1.2, 1.2, 1.2, "linear"))
        kfs.append(make_keyframe("scale", 0.10 + delay, 1.0, 1.0, 1.0, "linear"))
        kfs.append(make_keyframe("scale", 0.25, 1.0, 1.0, 1.0, "linear"))
        spawn_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # WRAPPER TOPS — unfurl outward (rotate from collapsed to fanned)
    for (name, bone_uuid, _, _, i) in wrapper_top_bones:
        kfs = []
        delay = 0.10 + i * 0.01
        # Start collapsed (scale 0)
        kfs.append(make_keyframe("scale", 0.0, 0.0, 0.0, 0.0, "linear"))
        kfs.append(make_keyframe("scale", delay, 0.0, 0.0, 0.0, "linear"))
        kfs.append(make_keyframe("scale", delay + 0.05, 1.2, 1.0, 1.2, "linear"))
        kfs.append(make_keyframe("scale", delay + 0.10, 1.0, 1.0, 1.0, "linear"))
        kfs.append(make_keyframe("scale", 0.25, 1.0, 1.0, 1.0, "linear"))
        # Unfurl rotation — pitch from -80 (collapsed up) to -25 (fanned)
        yaw = math.degrees((2 * math.pi * i) / n_twist)
        kfs.append(make_keyframe("rotation", 0.0, -80, yaw, 0, "linear"))
        kfs.append(make_keyframe("rotation", delay, -80, yaw, 0, "linear"))
        kfs.append(make_keyframe("rotation", delay + 0.08, -10, yaw + 20, 0, "linear"))
        kfs.append(make_keyframe("rotation", 0.25, -25, yaw, 0, "linear"))
        spawn_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # WRAPPER BOTTOMS — same unfurl
    for (name, bone_uuid, _, _, i) in wrapper_bot_bones:
        kfs = []
        delay = 0.10 + i * 0.01
        yaw = math.degrees((2 * math.pi * i) / n_twist + math.pi / n_twist)
        kfs.append(make_keyframe("scale", 0.0, 0.0, 0.0, 0.0, "linear"))
        kfs.append(make_keyframe("scale", delay, 0.0, 0.0, 0.0, "linear"))
        kfs.append(make_keyframe("scale", delay + 0.05, 1.2, 1.0, 1.2, "linear"))
        kfs.append(make_keyframe("scale", delay + 0.10, 1.0, 1.0, 1.0, "linear"))
        kfs.append(make_keyframe("scale", 0.25, 1.0, 1.0, 1.0, "linear"))
        kfs.append(make_keyframe("rotation", 0.0, 80, yaw, 0, "linear"))
        kfs.append(make_keyframe("rotation", delay, 80, yaw, 0, "linear"))
        kfs.append(make_keyframe("rotation", delay + 0.08, 10, yaw - 20, 0, "linear"))
        kfs.append(make_keyframe("rotation", 0.25, 25, yaw, 0, "linear"))
        spawn_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # SWIRL strips — materialise after body
    for (name, bone_uuid, _, _, i, yaw, roll) in swirl_bones:
        kfs = []
        delay = 0.08 + i * 0.015
        kfs.append(make_keyframe("scale", 0.0, 0.0, 0.0, 0.0, "linear"))
        kfs.append(make_keyframe("scale", delay, 0.0, 0.0, 0.0, "linear"))
        kfs.append(make_keyframe("scale", delay + 0.06, 1.4, 1.4, 1.4, "linear"))
        kfs.append(make_keyframe("scale", delay + 0.12, 1.0, 1.0, 1.0, "linear"))
        kfs.append(make_keyframe("scale", 0.25, 1.0, 1.0, 1.0, "linear"))
        # Rotation — settle to base orientation
        kfs.append(make_keyframe("rotation", 0.0, 0, yaw, roll, "linear"))
        kfs.append(make_keyframe("rotation", 0.25, 0, yaw, roll, "linear"))
        spawn_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # SPARKLE TRAIL — fan outward behind from origin to back positions
    for (name, bone_uuid, _, _, i, x_off, y_off, z_back) in sparkle_bones:
        kfs = []
        delay = 0.05 + i * 0.020
        # Position — start at body center, move to trail location
        kfs.append(make_keyframe("position", 0.0, -x_off, -y_off, -z_back, "linear"))
        kfs.append(make_keyframe("position", delay, -x_off, -y_off, -z_back, "linear"))
        kfs.append(make_keyframe("position", delay + 0.06, -x_off * 0.5, -y_off * 0.5,
                                 -z_back * 0.5, "linear"))
        kfs.append(make_keyframe("position", 0.25, 0, 0, 0, "linear"))
        # Scale pop in
        kfs.append(make_keyframe("scale", 0.0, 0.0, 0.0, 0.0, "linear"))
        kfs.append(make_keyframe("scale", delay, 0.0, 0.0, 0.0, "linear"))
        kfs.append(make_keyframe("scale", delay + 0.05, 1.5, 1.5, 1.5, "linear"))
        kfs.append(make_keyframe("scale", delay + 0.10, 1.0, 1.0, 1.0, "linear"))
        kfs.append(make_keyframe("scale", 0.25, 1.0, 1.0, 1.0, "linear"))
        spawn_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    b.add_animation("spawn", length=0.25, loop="once", override=True, animators=spawn_animators)

    # ---- IDLE: 1.5s loop — spinning candy in flight ----
    idle_animators = {}

    # Root — full Z rotation (projectile spin)
    # Body axis is Y; "spin like fired projectile" => spin about Z
    root_kfs = []
    root_kfs.append(make_keyframe("rotation", 0.0,  0,  0,    0, "linear"))
    root_kfs.append(make_keyframe("rotation", 0.5,  0,  0,  120, "linear"))
    root_kfs.append(make_keyframe("rotation", 1.0,  0,  0,  240, "linear"))
    root_kfs.append(make_keyframe("rotation", 1.5,  0,  0,  360, "linear"))
    idle_animators[root_uuid] = {"name": "root", "type": "bone", "keyframes": root_kfs}

    # BODY cubes — subtle pulse + tiny rotation jitter (high keyframe density)
    for idx, (name, bone_uuid, _, _) in enumerate(body_bones):
        kfs = []
        phase = idx * (math.pi * 2 / 3)
        for t_step in range(16):
            t = (t_step / 15) * 1.5
            wave = 1.0 + 0.03 * math.sin(2 * math.pi * t / 1.5 + phase)
            kfs.append(make_keyframe("scale", t, wave, wave, wave, "linear"))
        for t_step in range(16):
            t = (t_step / 15) * 1.5
            jx = 1.5 * math.sin(2 * math.pi * t / 0.6 + phase)
            jy = 1.5 * math.cos(2 * math.pi * t / 0.6 + phase * 1.3)
            jz = 1.0 * math.sin(2 * math.pi * t / 0.45 + phase * 0.7)
            kfs.append(make_keyframe("rotation", t, jx, jy, jz, "linear"))
        idle_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # WRAPPER TOP GROUP — spin FASTER than body around Y axis (air drag flap)
    # Group itself rotates around its origin
    wrap_top_kfs = []
    wrap_top_kfs.append(make_keyframe("rotation", 0.0,  0,    0, 0, "linear"))
    wrap_top_kfs.append(make_keyframe("rotation", 0.5,  0,  240, 0, "linear"))
    wrap_top_kfs.append(make_keyframe("rotation", 1.0,  0,  480, 0, "linear"))
    wrap_top_kfs.append(make_keyframe("rotation", 1.5,  0,  720, 0, "linear"))
    idle_animators[wrapper_top_group_uuid] = {"name": "wrapper_top", "type": "bone",
                                              "keyframes": wrap_top_kfs}

    # WRAPPER BOT GROUP — spin in OPPOSITE direction faster (flap effect)
    wrap_bot_kfs = []
    wrap_bot_kfs.append(make_keyframe("rotation", 0.0,  0,    0, 0, "linear"))
    wrap_bot_kfs.append(make_keyframe("rotation", 0.5,  0, -260, 0, "linear"))
    wrap_bot_kfs.append(make_keyframe("rotation", 1.0,  0, -520, 0, "linear"))
    wrap_bot_kfs.append(make_keyframe("rotation", 1.5,  0, -780, 0, "linear"))
    idle_animators[wrapper_bot_group_uuid] = {"name": "wrapper_bot", "type": "bone",
                                              "keyframes": wrap_bot_kfs}

    # Individual wrapper twist pieces — small flap wobble on top of group spin
    for (name, bone_uuid, _, _, i) in wrapper_top_bones:
        kfs = []
        yaw = math.degrees((2 * math.pi * i) / n_twist)
        phase = i * 0.5
        for t_step in range(20):
            t = (t_step / 19) * 1.5
            wob = 8 * math.sin(2 * math.pi * t / 0.5 + phase)
            roll = 4 * math.cos(2 * math.pi * t / 0.5 + phase * 1.2)
            kfs.append(make_keyframe("rotation", t, -25 + wob, yaw, roll, "linear"))
        for t_step in range(16):
            t = (t_step / 15) * 1.5
            sc = 1.0 + 0.05 * math.sin(2 * math.pi * t / 0.4 + phase)
            kfs.append(make_keyframe("scale", t, sc, sc, sc, "linear"))
        idle_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    for (name, bone_uuid, _, _, i) in wrapper_bot_bones:
        kfs = []
        yaw = math.degrees((2 * math.pi * i) / n_twist + math.pi / n_twist)
        phase = i * 0.5 + math.pi
        for t_step in range(20):
            t = (t_step / 19) * 1.5
            wob = 8 * math.sin(2 * math.pi * t / 0.5 + phase)
            roll = 4 * math.cos(2 * math.pi * t / 0.5 + phase * 1.2)
            kfs.append(make_keyframe("rotation", t, 25 - wob, yaw, roll, "linear"))
        for t_step in range(16):
            t = (t_step / 15) * 1.5
            sc = 1.0 + 0.05 * math.sin(2 * math.pi * t / 0.4 + phase)
            kfs.append(make_keyframe("scale", t, sc, sc, sc, "linear"))
        idle_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # SWIRL strips — rotate WITH the body (relative to body, no extra spin needed,
    # so we add only a slight breathing wobble + scale shimmer)
    for (name, bone_uuid, _, _, i, yaw_base, roll_base) in swirl_bones:
        kfs = []
        phase = i * 0.7
        for t_step in range(20):
            t = (t_step / 19) * 1.5
            extra_roll = roll_base + 4 * math.sin(2 * math.pi * t / 1.5 + phase)
            extra_pitch = 2 * math.cos(2 * math.pi * t / 0.75 + phase)
            kfs.append(make_keyframe("rotation", t, extra_pitch, yaw_base, extra_roll, "linear"))
        for t_step in range(16):
            t = (t_step / 15) * 1.5
            sc = 1.0 + 0.04 * math.sin(2 * math.pi * t / 0.5 + phase * 1.4)
            kfs.append(make_keyframe("scale", t, sc, sc, sc, "linear"))
        idle_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # SPARKLE trail — oscillate side-to-side (comet wiggle)
    for (name, bone_uuid, _, _, i, x_off, y_off, z_back) in sparkle_bones:
        kfs = []
        phase = i * 0.6
        amp_x = 0.25 + i * 0.05
        amp_y = 0.18 + i * 0.04
        for t_step in range(24):
            t = (t_step / 23) * 1.5
            ox = amp_x * math.sin(2 * math.pi * t / 0.75 + phase)
            oy = amp_y * math.cos(2 * math.pi * t / 0.75 + phase)
            oz = 0.15 * math.sin(2 * math.pi * t / 0.5 + phase * 1.3)
            kfs.append(make_keyframe("position", t, ox, oy, oz, "linear"))
        # Scale twinkle (denser)
        for t_step in range(24):
            t = (t_step / 23) * 1.5
            tw = 1.0 + 0.25 * math.sin(2 * math.pi * t / 0.4 + phase * 1.7)
            kfs.append(make_keyframe("scale", t, tw, tw, tw, "linear"))
        # Rotation twinkle for facets
        for t_step in range(16):
            t = (t_step / 15) * 1.5
            rr = 30 * math.sin(2 * math.pi * t / 0.6 + phase)
            kfs.append(make_keyframe("rotation", t, rr, rr * 0.6, -rr * 0.5, "linear"))
        idle_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    b.add_animation("idle", length=1.5, loop="loop", override=False, animators=idle_animators)

    # ---- DISSIPATE: 0.3s — IMPACT, candy shatters ----
    diss_animators = {}

    # Root — flash scale 1.5 then shrink to 0
    root_kfs = []
    root_kfs.append(make_keyframe("scale", 0.0,  1.0, 1.0, 1.0, "linear"))
    root_kfs.append(make_keyframe("scale", 0.05, 1.5, 1.5, 1.5, "linear"))  # flash
    root_kfs.append(make_keyframe("scale", 0.10, 1.4, 1.4, 1.4, "linear"))
    root_kfs.append(make_keyframe("scale", 0.20, 1.2, 1.2, 1.2, "linear"))
    root_kfs.append(make_keyframe("scale", 0.30, 0.0, 0.0, 0.0, "linear"))
    # Slight rotation continuation
    root_kfs.append(make_keyframe("rotation", 0.0, 0, 0,   0, "linear"))
    root_kfs.append(make_keyframe("rotation", 0.30, 0, 0, 90, "linear"))
    diss_animators[root_uuid] = {"name": "root", "type": "bone", "keyframes": root_kfs}

    # BODY cubes — fly outward (each in a chosen direction)
    body_dirs = [(0, 1, 0), (1, 0, 0.5), (-1, -0.3, -0.5)]
    for idx, (name, bone_uuid, _, _) in enumerate(body_bones):
        kfs = []
        dx, dy, dz = body_dirs[idx % len(body_dirs)]
        kfs.append(make_keyframe("position", 0.0, 0, 0, 0, "linear"))
        kfs.append(make_keyframe("position", 0.05, dx * 0.3, dy * 0.3, dz * 0.3, "linear"))
        kfs.append(make_keyframe("position", 0.30, dx * 4.0, dy * 4.0, dz * 4.0, "linear"))
        kfs.append(make_keyframe("rotation", 0.0,  0, 0, 0, "linear"))
        kfs.append(make_keyframe("rotation", 0.30, 90 * dx, 90 * dy, 90 * dz, "linear"))
        kfs.append(make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, "linear"))
        kfs.append(make_keyframe("scale", 0.05, 1.3, 1.3, 1.3, "linear"))
        kfs.append(make_keyframe("scale", 0.20, 0.8, 0.8, 0.8, "linear"))
        kfs.append(make_keyframe("scale", 0.30, 0.0, 0.0, 0.0, "linear"))
        diss_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # WRAPPER TOPS — spiral away spinning
    for (name, bone_uuid, _, _, i) in wrapper_top_bones:
        kfs = []
        angle = (2 * math.pi * i) / n_twist
        cos_a = math.cos(angle)
        sin_a = math.sin(angle)
        yaw = math.degrees(angle)
        kfs.append(make_keyframe("position", 0.0, 0, 0, 0, "linear"))
        kfs.append(make_keyframe("position", 0.30, cos_a * 5.0, 3.0, sin_a * 5.0, "linear"))
        # Spiral spin
        kfs.append(make_keyframe("rotation", 0.0, -25, yaw, 0, "linear"))
        kfs.append(make_keyframe("rotation", 0.30, 360, yaw + 540, 360, "linear"))
        kfs.append(make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, "linear"))
        kfs.append(make_keyframe("scale", 0.10, 1.3, 1.3, 1.3, "linear"))
        kfs.append(make_keyframe("scale", 0.30, 0.2, 0.2, 0.2, "linear"))
        diss_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # WRAPPER BOTS — spiral down and out
    for (name, bone_uuid, _, _, i) in wrapper_bot_bones:
        kfs = []
        angle = (2 * math.pi * i) / n_twist + math.pi / n_twist
        cos_a = math.cos(angle)
        sin_a = math.sin(angle)
        yaw = math.degrees(angle)
        kfs.append(make_keyframe("position", 0.0, 0, 0, 0, "linear"))
        kfs.append(make_keyframe("position", 0.30, cos_a * 5.0, -3.0, sin_a * 5.0, "linear"))
        kfs.append(make_keyframe("rotation", 0.0, 25, yaw, 0, "linear"))
        kfs.append(make_keyframe("rotation", 0.30, -360, yaw - 540, -360, "linear"))
        kfs.append(make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, "linear"))
        kfs.append(make_keyframe("scale", 0.10, 1.3, 1.3, 1.3, "linear"))
        kfs.append(make_keyframe("scale", 0.30, 0.2, 0.2, 0.2, "linear"))
        diss_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # SWIRL strips — scatter outward (random directions)
    swirl_dirs = [(1.5, 1.0, 0), (-1.5, 0.5, 1.0), (0.5, -1.0, -1.5), (-0.5, 1.5, -0.5)]
    for (name, bone_uuid, _, _, i, yaw_base, roll_base) in swirl_bones:
        kfs = []
        dx, dy, dz = swirl_dirs[i % len(swirl_dirs)]
        kfs.append(make_keyframe("position", 0.0, 0, 0, 0, "linear"))
        kfs.append(make_keyframe("position", 0.30, dx * 3.5, dy * 3.5, dz * 3.5, "linear"))
        kfs.append(make_keyframe("rotation", 0.0, 0, yaw_base, roll_base, "linear"))
        kfs.append(make_keyframe("rotation", 0.30, 270, yaw_base + 360, roll_base + 360, "linear"))
        kfs.append(make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, "linear"))
        kfs.append(make_keyframe("scale", 0.30, 0.0, 0.0, 0.0, "linear"))
        diss_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # SPARKLE trail — continues backward (away from impact)
    for (name, bone_uuid, _, _, i, x_off, y_off, z_back) in sparkle_bones:
        kfs = []
        # Continue further back
        kfs.append(make_keyframe("position", 0.0, 0, 0, 0, "linear"))
        kfs.append(make_keyframe("position", 0.30, x_off * 1.5, y_off * 1.5,
                                 -2.0 - i * 0.4, "linear"))
        # Twinkle out
        kfs.append(make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, "linear"))
        kfs.append(make_keyframe("scale", 0.10, 1.4, 1.4, 1.4, "linear"))
        kfs.append(make_keyframe("scale", 0.30, 0.0, 0.0, 0.0, "linear"))
        diss_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    b.add_animation("dissipate", length=0.3, loop="once", override=True, animators=diss_animators)

    # Write file
    out_path = "D:/CC/ChaosCraft/src/main/resources/models/fluffy/me_attacks/candy_cannon_hyperglycemic.bbmodel"
    p = b.write(out_path)
    print(f"Wrote {p}")


if __name__ == "__main__":
    build()
