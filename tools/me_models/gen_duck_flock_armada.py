#!/usr/bin/env python3
"""Generator for duck_flock_armada.bbmodel — Giant Rubber Duck Flock 'Armada'.

Visual intent: four rubber ducks orbit the caster in V-formation at radius 8,
height Y=6. Beaks pointed inward. They bob up/down like ducks on water as
they orbit. Below them is a flat blue water disc — the implied "water" they
float on. Splash rings pulse beneath each duck. The horror: who is the water?
"""

import sys
import math

sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder,
    make_uuid,
    make_keyframe,
    basic_cube_faces,
    uniform_face,
    png_from_pixels,
    hex_to_rgba,
    gradient_radial,
    color_lerp,
    fill_rect,
)

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/fluffy/me_attacks/duck_flock_armada.bbmodel"


# ---------------------------------------------------------------------------
# TEXTURES
# ---------------------------------------------------------------------------

def build_body_texture():
    """Texture 0: rubber duck yellow — same palette as the single-duck attack.

    Bright rubber yellow base #ffe840, slightly orange-yellow mid #e8c020,
    deep shadow #b09010, bright specular highlight #fff880, emissive #ffd020.
    Smooth rubber. Wide soft specular cluster centred at (32, 10).
    """
    w = h = 64
    base = hex_to_rgba("#ffe840")
    mid = hex_to_rgba("#e8c020")
    shadow = hex_to_rgba("#b09010")
    spec = hex_to_rgba("#fff880")
    emiss = hex_to_rgba("#ffd020")
    eye_white = hex_to_rgba("#ffffff")
    eye_black = hex_to_rgba("#101010")
    beak_orange = hex_to_rgba("#ff8020")
    beak_deep = hex_to_rgba("#a04808")
    beak_spec = hex_to_rgba("#ffc080")

    pixels = [base] * (w * h)

    # Gentle radial volume wash so the duck reads as rounded
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - 32) ** 2 + (y - 32) ** 2)
            t = min(1.0, d / 44.0)
            pixels[y * w + x] = color_lerp(base, mid, t * 0.35)

    # Faint shadow pooling at the bottom for gravity hint
    for y in range(48, h):
        t = (y - 48) / 16.0
        for x in range(w):
            p = pixels[y * w + x]
            pixels[y * w + x] = color_lerp(p, shadow, t * 0.18)

    # Wide soft specular cluster at (32, 10) — the rubber catch-light
    sx, sy = 32, 10
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - sx) ** 2 + (y - sy) ** 2)
            if d < 16.0:
                t = max(0.0, 1.0 - d / 16.0)
                t = t * t * (3 - 2 * t)
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, spec, t * 0.8)

    # Inner emissive hot core inside the specular cluster
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - sx) ** 2 + (y - sy) ** 2)
            if d < 5.0:
                t = max(0.0, 1.0 - d / 5.0)
                t = t * t
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, emiss, t * 0.45)

    # Secondary smaller specular pop low-right (rim light)
    sx2, sy2 = 50, 46
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - sx2) ** 2 + (y - sy2) ** 2)
            if d < 7.0:
                t = max(0.0, 1.0 - d / 7.0)
                t = t * t * (3 - 2 * t)
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, spec, t * 0.45)

    # Eye zone — small region bottom-left used for eye cubes' uniform face
    fill_rect(pixels, w, h, 0, 56, 8, 64, eye_black)
    fill_rect(pixels, w, h, 1, 57, 4, 60, eye_white)
    fill_rect(pixels, w, h, 5, 60, 7, 62, color_lerp(eye_black, eye_white, 0.45))

    # Beak zone — small region bottom-right (cols 56..63, rows 56..63)
    fill_rect(pixels, w, h, 56, 56, 64, 64, beak_orange)
    # Beak specular highlight
    for y in range(56, 64):
        for x in range(56, 64):
            d = math.sqrt((x - 58) ** 2 + (y - 57) ** 2)
            if d < 3.0:
                t = max(0.0, 1.0 - d / 3.0)
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, beak_spec, t * 0.7)
    # Beak shadow at bottom-right of beak zone
    for y in range(60, 64):
        for x in range(60, 64):
            p = pixels[y * w + x]
            pixels[y * w + x] = color_lerp(p, beak_deep, 0.4)

    return png_from_pixels(pixels, w, h)


def build_water_texture():
    """Texture 1: water surface (sky-blue, viewed from above).

    Sky blue base #b8e8ff, slightly darker water #88c8e8, deep water #506888,
    surface highlight #d8f4ff, emissive #e8f8ff. Reads as water from above:
    lighter at surface, darker at depth. Concentric ring ripple detail at
    40% and 70% radius. ONLY water texture in any ChaosCraft mode.
    """
    w = h = 64
    base = hex_to_rgba("#b8e8ff")
    deeper = hex_to_rgba("#88c8e8")
    deep = hex_to_rgba("#506888")
    spec = hex_to_rgba("#d8f4ff")
    emiss = hex_to_rgba("#e8f8ff")

    pixels = [base] * (w * h)
    cx, cy = 32, 32

    # Radial gradient: lighter at centre (surface) → darker at edge (depth)
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            t = min(1.0, d / 32.0)
            # First half: spec → base, second half: base → deeper, far: deeper → deep
            if t < 0.3:
                tt = t / 0.3
                c = color_lerp(spec, base, tt)
            elif t < 0.7:
                tt = (t - 0.3) / 0.4
                c = color_lerp(base, deeper, tt)
            else:
                tt = (t - 0.7) / 0.3
                c = color_lerp(deeper, deep, tt)
            pixels[y * w + x] = c

    # Concentric ripple ring at 40% radius (~12.8 px from centre)
    ring1_r = 12.8
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            if abs(d - ring1_r) < 1.2:
                t = 1.0 - abs(d - ring1_r) / 1.2
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, spec, t * 0.55)

    # Concentric ripple ring at 70% radius (~22.4 px from centre)
    ring2_r = 22.4
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            if abs(d - ring2_r) < 1.0:
                t = 1.0 - abs(d - ring2_r) / 1.0
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, spec, t * 0.45)

    # Soft inner hot-spot — emissive "sun glint" near centre top
    glint_x, glint_y = 26, 22
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - glint_x) ** 2 + (y - glint_y) ** 2)
            if d < 6.0:
                t = max(0.0, 1.0 - d / 6.0)
                t = t * t
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, emiss, t * 0.45)

    # A few scattered specular highlights for water sparkle
    sparkle_spots = [(14, 18), (48, 16), (22, 46), (50, 44), (18, 32), (46, 30)]
    for (sx, sy) in sparkle_spots:
        for y in range(h):
            for x in range(w):
                d = math.sqrt((x - sx) ** 2 + (y - sy) ** 2)
                if d < 2.5:
                    t = max(0.0, 1.0 - d / 2.5)
                    p = pixels[y * w + x]
                    pixels[y * w + x] = color_lerp(p, spec, t * 0.55)

    # Subtle horizontal "wave" lines for water character
    for wave_y in (20, 28, 36, 44):
        for x in range(0, w, 3):
            for dx in range(2):
                if x + dx < w:
                    p = pixels[wave_y * w + x + dx]
                    pixels[wave_y * w + x + dx] = color_lerp(p, deeper, 0.18)

    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# MODEL BUILD
# ---------------------------------------------------------------------------

def build_one_duck(b, duck_idx, body_face, eye_face, beak_face):
    """Build one duck. Returns (duck_root_group_uuid, duck_root_group_dict,
    inner_bone_uuids_dict).

    The duck is built around a local origin (0,0,0) — its parent group is the
    'orbit anchor' which positions and rotates it around the caster.
    inner_bone_uuids_dict maps internal bone names to their uuids so we can
    keyframe head/beak/tail/wakes per-duck in animations.
    """
    # All cube positions are LOCAL to this duck — its parent orbit bone places it
    # in world space at radius 8, Y=6.

    # ---- BODY: 5 overlapping cubes forming an egg-like oval. Each its own bone. ----
    body_groups = []
    body_specs = [
        # name, from, to, group_origin
        ("body_core",  [-3.0, -2.0, -2.0], [3.0, 2.0, 2.0],   [0, 0, 0]),
        ("body_belly", [-2.6, -2.4, -2.2], [2.6, -0.5, 2.2],  [0, -1.5, 0]),
        ("body_back",  [-2.4,  0.5, -1.8], [2.0,  2.4, 1.8],  [0, 1.5, 0]),
        ("body_front", [ 2.6, -1.4, -1.4], [4.6,  1.4, 1.4],  [3.6, 0, 0]),
        ("body_rear",  [-4.6, -1.2, -1.4], [-2.6, 1.4, 1.4],  [-3.6, 0, 0]),
    ]
    for nm, fr, to_, og in body_specs:
        cube = b.add_cube(f"d{duck_idx}_{nm}", from_=fr, to_=to_, faces=body_face())
        gu = make_uuid()
        body_groups.append((gu, b.make_group(f"d{duck_idx}_{nm}", og, [cube], group_uuid=gu)))

    # ---- HEAD: 3 cubes, one bone ----
    head_cubes = []
    head_cubes.append(b.add_cube(
        f"d{duck_idx}_head_main",
        from_=[4.5, 1.5, -1.5], to_=[7.5, 4.5, 1.5],
        faces=body_face(),
    ))
    head_cubes.append(b.add_cube(
        f"d{duck_idx}_head_top",
        from_=[4.8, 3.5, -1.2], to_=[7.2, 5.4, 1.2],
        faces=body_face(),
    ))
    head_cubes.append(b.add_cube(
        f"d{duck_idx}_head_front",
        from_=[6.8, 2.0, -1.2], to_=[8.4, 4.0, 1.2],
        faces=body_face(),
    ))
    head_uuid = make_uuid()
    head_group = b.make_group(f"d{duck_idx}_head", [6.0, 3.0, 0], head_cubes, group_uuid=head_uuid)

    # ---- BEAK: 2 flat wedge cubes ----
    beak_cubes = []
    beak_cubes.append(b.add_cube(
        f"d{duck_idx}_beak_upper",
        from_=[8.0, 2.5, -1.0], to_=[10.5, 3.4, 1.0],
        faces=beak_face(),
    ))
    beak_cubes.append(b.add_cube(
        f"d{duck_idx}_beak_lower",
        from_=[8.0, 2.0, -0.8], to_=[10.0, 2.6, 0.8],
        faces=beak_face(),
    ))
    beak_uuid = make_uuid()
    beak_group = b.make_group(f"d{duck_idx}_beak", [9.0, 2.7, 0], beak_cubes, group_uuid=beak_uuid)

    # ---- EYES: 2 small flat cubes on sides of head ----
    eye_left_cube = b.add_cube(
        f"d{duck_idx}_eye_left",
        from_=[5.6, 3.5, 1.5], to_=[6.4, 4.3, 1.7],
        faces=eye_face(),
    )
    eye_left_uuid = make_uuid()
    eye_left_group = b.make_group(
        f"d{duck_idx}_eye_left", [6.0, 3.9, 1.6], [eye_left_cube], group_uuid=eye_left_uuid,
    )

    eye_right_cube = b.add_cube(
        f"d{duck_idx}_eye_right",
        from_=[5.6, 3.5, -1.7], to_=[6.4, 4.3, -1.5],
        faces=eye_face(),
    )
    eye_right_uuid = make_uuid()
    eye_right_group = b.make_group(
        f"d{duck_idx}_eye_right", [6.0, 3.9, -1.6], [eye_right_cube], group_uuid=eye_right_uuid,
    )

    # ---- TAIL: 1 small angled cube ----
    tail_cube = b.add_cube(
        f"d{duck_idx}_tail_nub",
        from_=[-5.6, 0.4, -0.9], to_=[-4.4, 2.0, 0.9],
        faces=body_face(),
    )
    tail_uuid = make_uuid()
    tail_group = b.make_group(
        f"d{duck_idx}_tail", [-5.0, 1.2, 0], [tail_cube],
        rotation=[0, 0, 25.0],
        group_uuid=tail_uuid,
    )

    # ---- WAKE: 6 flat slabs in V formation behind duck (water texture) ----
    wake_groups = []
    wake_specs = [
        # x_behind, z_offset, scale
        (-7.0,  1.4, 0.85),
        (-7.0, -1.4, 0.85),
        (-9.5,  2.6, 1.0),
        (-9.5, -2.6, 1.0),
        (-12.0, 3.8, 1.15),
        (-12.0, -3.8, 1.15),
    ]
    for i, (sx_, sz_, sc) in enumerate(wake_specs):
        half_x = 1.6 * sc
        half_y = 0.25
        half_z = 0.6 * sc
        cube_uuid = b.add_cube(
            f"d{duck_idx}_wake_{i+1}",
            from_=[-half_x, -half_y, -half_z], to_=[half_x, half_y, half_z],
            faces=uniform_face(0, 0, 64, 64, tex_index=1),  # water
        )
        rot_y = 15.0 if sz_ > 0 else -15.0
        gu = make_uuid()
        g = b.make_group(
            f"d{duck_idx}_wake_{i+1}", [sx_, -1.2, sz_], [cube_uuid],
            rotation=[0, rot_y, 0], group_uuid=gu,
        )
        wake_groups.append((gu, g))

    # ---- assemble inside this duck's local-root bone ----
    inner_children = []
    for (_uu, g) in body_groups:
        inner_children.append(g)
    inner_children.append(head_group)
    inner_children.append(beak_group)
    inner_children.append(eye_left_group)
    inner_children.append(eye_right_group)
    inner_children.append(tail_group)
    for (_uu, g) in wake_groups:
        inner_children.append(g)

    duck_local_uuid = make_uuid()
    duck_local_group = b.make_group(
        f"d{duck_idx}_local", [0, 0, 0], inner_children, group_uuid=duck_local_uuid,
    )

    bones = {
        "local": duck_local_uuid,
        "head": head_uuid,
        "beak": beak_uuid,
        "eye_left": eye_left_uuid,
        "eye_right": eye_right_uuid,
        "tail": tail_uuid,
        "body_groups": body_groups,
        "wake_groups": wake_groups,
    }
    return duck_local_uuid, duck_local_group, bones


def build():
    b = Builder("duck_flock_armada", resolution=(64, 64), visible_box=(20, 12, 0))

    # ---- TEXTURES ----
    b.add_texture("duck_body", build_body_texture())
    b.add_texture("water", build_water_texture())

    # Face shorthands
    body_face = lambda: uniform_face(0, 0, 56, 56, tex_index=0)
    eye_face = lambda: uniform_face(0, 56, 8, 64, tex_index=0)
    beak_face = lambda: uniform_face(56, 56, 64, 64, tex_index=0)

    # =====================================================================
    # 4 DUCKS — each has an "orbit anchor" parent bone so we can rotate the
    # whole duck around the caster (Y axis) on the orbit. The local-duck bone
    # under it handles the per-duck bobbing / wake-following.
    # Beaks point down +X. We rotate orbit by Y so beak swings tangentially;
    # for the inward-pointing beak we add a tilt in the local bone (or rely
    # on the orbit angle so the duck-front faces the centre — easier: place
    # each orbit anchor pre-rotated so the local +X axis points to the orbit
    # centre).
    # We achieve "beaks pointed inward" by setting orbit-anchor base position
    # at radius 8 and base Y rotation pointing inward (rotation_y = i*90 + 180).
    # =====================================================================
    NUM_DUCKS = 4
    RADIUS = 8.0
    HEIGHT = 6.0

    duck_orbit_uuids = []        # orbit anchor uuids (one per duck)
    duck_orbit_groups = []       # the orbit anchor group dicts
    duck_bone_dicts = []         # inner-bone uuid maps (per duck)
    duck_local_uuids = []        # the per-duck "local" bones (handle bob)

    for i in range(NUM_DUCKS):
        angle_deg = i * 90.0
        # Orbit anchor: this group sits at the centre of orbit. We position the
        # duck on the +X axis at radius 8, and rotate this whole group by
        # angle_deg around Y so the duck rides the orbit.
        local_uuid, local_group, bones = build_one_duck(
            b, duck_idx=i + 1,
            body_face=body_face, eye_face=eye_face, beak_face=beak_face,
        )

        # Wrap the local group inside an "offset" group that places duck at
        # (RADIUS, HEIGHT, 0) and points beak inward (rotate Y +180 so duck +X
        # faces back toward orbit centre).
        offset_uuid = make_uuid()
        offset_group = b.make_group(
            f"d{i+1}_offset",
            [RADIUS, HEIGHT, 0],
            [local_group],
            rotation=[0, 180, 0],   # beak (local +X) now points back toward origin = inward
            group_uuid=offset_uuid,
        )

        # Orbit anchor: at origin, rotated by angle_deg so the duck rides at angle.
        orbit_uuid = make_uuid()
        orbit_group = b.make_group(
            f"d{i+1}_orbit",
            [0, 0, 0],
            [offset_group],
            rotation=[0, angle_deg, 0],
            group_uuid=orbit_uuid,
        )

        duck_orbit_uuids.append(orbit_uuid)
        duck_orbit_groups.append(orbit_group)
        duck_local_uuids.append(local_uuid)
        duck_bone_dicts.append(bones)

    # =====================================================================
    # CENTRAL WATER DISC — 10 flat slabs forming a ring at Y=0.
    # Implied "water" surface beneath the orbiting ducks.
    # =====================================================================
    disc_groups = []
    DISC_RADIUS = 9.0
    DISC_SLABS = 10
    for i in range(DISC_SLABS):
        ang = i * (360.0 / DISC_SLABS)
        rad_ang = math.radians(ang)
        x = math.cos(rad_ang) * DISC_RADIUS
        z = math.sin(rad_ang) * DISC_RADIUS
        # Each slab is a flat trapezoidal slab (we use a flat cube)
        half_x = 2.6
        half_y = 0.18
        half_z = 1.4
        cube_uuid = b.add_cube(
            f"disc_slab_{i+1}",
            from_=[-half_x, -half_y, -half_z], to_=[half_x, half_y, half_z],
            faces=uniform_face(0, 0, 64, 64, tex_index=1),
        )
        gu = make_uuid()
        # Rotate slab so its long X axis is tangent to the disc circle
        g = b.make_group(
            f"disc_slab_{i+1}", [x, 0, z], [cube_uuid],
            rotation=[0, -ang + 90, 0], group_uuid=gu,
        )
        disc_groups.append((gu, g))

    # =====================================================================
    # SPLASH RINGS — 4 small flat slab clusters at Y=0 directly below each duck.
    # Each splash ring is 1 flat circle slab. Each its own bone.
    # =====================================================================
    splash_groups = []
    for i in range(NUM_DUCKS):
        angle_deg = i * 90.0
        rad_ang = math.radians(angle_deg)
        x = math.cos(rad_ang) * RADIUS
        z = math.sin(rad_ang) * RADIUS
        # Splash ring: a flat circular slab (we approximate with a flat square)
        half_x = 2.0
        half_y = 0.12
        half_z = 2.0
        cube_uuid = b.add_cube(
            f"splash_ring_{i+1}",
            from_=[-half_x, -half_y, -half_z], to_=[half_x, half_y, half_z],
            faces=uniform_face(0, 0, 64, 64, tex_index=1),
        )
        gu = make_uuid()
        g = b.make_group(
            f"splash_ring_{i+1}", [x, 0, z], [cube_uuid], group_uuid=gu,
        )
        splash_groups.append((gu, g))

    # =====================================================================
    # WATER DISC ROOT — bone wrapping all disc slabs so we can pulse the disc.
    # =====================================================================
    disc_root_uuid = make_uuid()
    disc_root_group = b.make_group(
        "water_disc", [0, 0, 0],
        [g for (_u, g) in disc_groups],
        group_uuid=disc_root_uuid,
    )

    # =====================================================================
    # ROOT
    # =====================================================================
    root_children = []
    root_children.append(disc_root_group)
    for og in duck_orbit_groups:
        root_children.append(og)
    for (_u, g) in splash_groups:
        root_children.append(g)

    root_uuid = make_uuid()
    root_group = b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid)
    b.outliner.append(root_group)

    # =====================================================================
    # ANIMATIONS
    # =====================================================================

    SPAWN_LEN = 0.9
    IDLE_LEN = 5.0
    DISS_LEN = 0.6

    # ----------------------------- SPAWN -----------------------------
    # 0.0s: water disc expands.
    # 0.0–0.7s: ducks drop from Y+15, staggered 0.2s, each landing with splash.
    # Wakes scale up from 0 as ducks land.
    spawn_animators = {}

    # Root: hold scale 1, no movement
    spawn_animators[root_uuid] = {
        "name": "root", "type": "bone", "keyframes": [
            make_keyframe("scale", 0.00, x=1, y=1, z=1),
            make_keyframe("scale", SPAWN_LEN, x=1, y=1, z=1),
        ],
    }

    # Water disc expands at 0.0s
    disc_kfs = [
        make_keyframe("scale", 0.00, x=0.05, y=0.05, z=0.05),
        make_keyframe("scale", 0.10, x=0.4, y=0.5, z=0.4),
        make_keyframe("scale", 0.25, x=0.85, y=1.05, z=0.85),
        make_keyframe("scale", 0.40, x=1.05, y=1.0, z=1.05),
        make_keyframe("scale", 0.60, x=0.98, y=1.0, z=0.98),
        make_keyframe("scale", SPAWN_LEN, x=1.0, y=1.0, z=1.0),
        make_keyframe("rotation", 0.00, x=0, y=0, z=0),
        make_keyframe("rotation", SPAWN_LEN, x=0, y=20, z=0),
    ]
    spawn_animators[disc_root_uuid] = {
        "name": "water_disc", "type": "bone", "keyframes": disc_kfs,
    }

    # Each duck: drop from Y+15 staggered. Use orbit anchor's Y position to lift.
    # We move the whole orbit anchor down (it starts up).
    for i, orbit_uuid in enumerate(duck_orbit_uuids):
        delay = 0.20 * i
        land = delay + 0.30
        kfs = [
            make_keyframe("position", 0.00, x=0, y=15, z=0),
            make_keyframe("position", delay, x=0, y=15, z=0),
            make_keyframe("position", land - 0.05, x=0, y=1.0, z=0),
            make_keyframe("position", land, x=0, y=-0.4, z=0),       # landing dip
            make_keyframe("position", land + 0.10, x=0, y=0.2, z=0),
            make_keyframe("position", SPAWN_LEN, x=0, y=0, z=0),
            # Scale: pop on landing
            make_keyframe("scale", 0.00, x=0.7, y=1.4, z=0.7),    # squashed-falling
            make_keyframe("scale", delay, x=0.7, y=1.4, z=0.7),
            make_keyframe("scale", land, x=1.25, y=0.7, z=1.25),  # squash on landing
            make_keyframe("scale", land + 0.10, x=0.95, y=1.10, z=0.95),
            make_keyframe("scale", SPAWN_LEN, x=1.0, y=1.0, z=1.0),
        ]
        spawn_animators[orbit_uuid] = {
            "name": f"d{i+1}_orbit", "type": "bone", "keyframes": kfs,
        }

    # Each splash ring: expands at landing time of its corresponding duck.
    for i, (gu, g) in enumerate(splash_groups):
        delay = 0.20 * i
        land = delay + 0.30
        kfs = [
            make_keyframe("scale", 0.00, x=0.05, y=0.05, z=0.05),
            make_keyframe("scale", land - 0.02, x=0.05, y=0.05, z=0.05),
            make_keyframe("scale", land + 0.05, x=1.5, y=1.5, z=1.5),
            make_keyframe("scale", land + 0.20, x=0.9, y=1.0, z=0.9),
            make_keyframe("scale", SPAWN_LEN, x=1.0, y=1.0, z=1.0),
            make_keyframe("rotation", 0.00, x=0, y=0, z=0),
            make_keyframe("rotation", SPAWN_LEN, x=0, y=120, z=0),
        ]
        spawn_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    # Each duck's wakes materialise behind it as it lands.
    # Wakes are children of the duck's local bone, so per-wake animators control
    # them within their local space.
    for i, bones in enumerate(duck_bone_dicts):
        delay = 0.20 * i
        land = delay + 0.30
        for j, (wgu, wg) in enumerate(bones["wake_groups"]):
            stagger = 0.04 * j
            kfs = [
                make_keyframe("scale", 0.00, x=0, y=0, z=0),
                make_keyframe("scale", land + stagger, x=0, y=0, z=0),
                make_keyframe("scale", land + stagger + 0.12, x=1.2, y=1.2, z=1.2),
                make_keyframe("scale", SPAWN_LEN, x=1.0, y=1.0, z=1.0),
            ]
            spawn_animators[wgu] = {
                "name": wg["name"], "type": "bone", "keyframes": kfs,
            }

    # Heads, beaks, eyes, tails of each duck: small landing pop
    for i, bones in enumerate(duck_bone_dicts):
        delay = 0.20 * i
        land = delay + 0.30
        for sub_uuid, sub_name in [
            (bones["head"], f"d{i+1}_head"),
            (bones["beak"], f"d{i+1}_beak"),
            (bones["eye_left"], f"d{i+1}_eye_left"),
            (bones["eye_right"], f"d{i+1}_eye_right"),
            (bones["tail"], f"d{i+1}_tail"),
        ]:
            kfs = [
                make_keyframe("scale", 0.00, x=0.7, y=0.7, z=0.7),
                make_keyframe("scale", land, x=1.15, y=1.15, z=1.15),
                make_keyframe("scale", land + 0.08, x=0.95, y=0.95, z=0.95),
                make_keyframe("scale", SPAWN_LEN, x=1.0, y=1.0, z=1.0),
            ]
            spawn_animators[sub_uuid] = {
                "name": sub_name, "type": "bone", "keyframes": kfs,
            }

    b.add_animation("spawn", length=SPAWN_LEN, animators=spawn_animators,
                    loop="once", override=True)

    # ----------------------------- IDLE -----------------------------
    # 5.0s loop. Each duck orbits 360° (orbit anchor rotates Y 0→360).
    # Each duck bobs Y ±0.5 on a 2.0s period (so 2.5 bobs per loop).
    # Beaks point inward (already baked into offset). Wakes follow direction.
    # Water disc slowly ripples (small Y rotation + scale breathing).
    # Splash rings under each duck pulse scale.
    idle_animators = {}

    STEPS = 60  # dense keyframes for smooth orbit + bob

    # Root: very subtle Y bob
    root_kfs = []
    for k in range(STEPS + 1):
        t = (k / STEPS) * IDLE_LEN
        ph = (t / IDLE_LEN) * 2 * math.pi
        py = math.sin(ph) * 0.08
        root_kfs.append(make_keyframe("position", t, x=0, y=py, z=0))
    idle_animators[root_uuid] = {
        "name": "root", "type": "bone", "keyframes": root_kfs,
    }

    # Water disc: slow rotation + breathing scale
    disc_kfs = []
    for k in range(STEPS + 1):
        t = (k / STEPS) * IDLE_LEN
        ry = (t / IDLE_LEN) * 36.0  # one tenth turn per loop — slow drift
        disc_kfs.append(make_keyframe("rotation", t, x=0, y=ry, z=0))
    for k in range(STEPS + 1):
        t = (k / STEPS) * IDLE_LEN
        ph = (t / IDLE_LEN) * 2 * math.pi
        s = 1.0 + 0.025 * math.sin(ph * 2)
        disc_kfs.append(make_keyframe("scale", t, x=s, y=1.0 + 0.01 * math.sin(ph), z=s))
    for k in range(STEPS + 1):
        t = (k / STEPS) * IDLE_LEN
        ph = (t / IDLE_LEN) * 2 * math.pi
        # Tiny Y bob in disc itself (water level breathing)
        py = math.sin(ph * 1.5) * 0.05
        disc_kfs.append(make_keyframe("position", t, x=0, y=py, z=0))
    idle_animators[disc_root_uuid] = {
        "name": "water_disc", "type": "bone", "keyframes": disc_kfs,
    }

    # Each duck orbit anchor: full 360° rotation around Y across IDLE_LEN
    for i, orbit_uuid in enumerate(duck_orbit_uuids):
        base_angle = i * 90.0
        kfs = []
        for k in range(STEPS + 1):
            t = (k / STEPS) * IDLE_LEN
            ang = base_angle + (t / IDLE_LEN) * 360.0
            kfs.append(make_keyframe("rotation", t, x=0, y=ang, z=0))
        idle_animators[orbit_uuid] = {
            "name": f"d{i+1}_orbit", "type": "bone", "keyframes": kfs,
        }

    # Each duck local bone: bob Y ±0.5 on 2.0s period (offset phase per duck)
    for i, local_uuid in enumerate(duck_local_uuids):
        kfs = []
        period = 2.0
        phase_off = i * (math.pi / 2)  # quarter-phase offset per duck
        for k in range(STEPS + 1):
            t = (k / STEPS) * IDLE_LEN
            ph = (t / period) * 2 * math.pi + phase_off
            py = math.sin(ph) * 0.5
            # Slight roll on Z as it bobs (duck rocking on water)
            rz = math.cos(ph) * 4.0
            # Small forward/back X drift (riding the orbit current)
            px = math.cos(ph * 0.5) * 0.15
            kfs.append(make_keyframe("position", t, x=px, y=py, z=0))
            kfs.append(make_keyframe("rotation", t, x=0, y=0, z=rz))
        idle_animators[local_uuid] = {
            "name": f"d{i+1}_local", "type": "bone", "keyframes": kfs,
        }

    # Per-duck inner bones: head/beak react to bob — head lags slightly
    for i, bones in enumerate(duck_bone_dicts):
        period = 2.0
        phase_off = i * (math.pi / 2)
        # Head: slight nodding lag, plus small inward tilt
        head_kfs = []
        for k in range(STEPS + 1):
            t = (k / STEPS) * IDLE_LEN
            ph = (t / period) * 2 * math.pi + phase_off - 0.4
            rx = math.sin(ph) * 4.0   # nodding pitch
            ry = math.sin(ph * 0.5) * 2.0
            rz = math.cos(ph) * 2.5
            head_kfs.append(make_keyframe("rotation", t, x=rx, y=ry, z=rz))
        for k in range(STEPS + 1):
            t = (k / STEPS) * IDLE_LEN
            ph = (t / period) * 2 * math.pi + phase_off - 0.4
            py = math.sin(ph) * 0.08
            head_kfs.append(make_keyframe("position", t, x=0, y=py, z=0))
        idle_animators[bones["head"]] = {
            "name": f"d{i+1}_head", "type": "bone", "keyframes": head_kfs,
        }

        # Beak: more lag, bigger nod amplitude
        beak_kfs = []
        for k in range(STEPS + 1):
            t = (k / STEPS) * IDLE_LEN
            ph = (t / period) * 2 * math.pi + phase_off - 0.7
            rx = math.sin(ph) * 6.0
            ry = math.sin(ph * 0.7 - 0.2) * 3.0
            rz = math.cos(ph) * 4.0
            beak_kfs.append(make_keyframe("rotation", t, x=rx, y=ry, z=rz))
        idle_animators[bones["beak"]] = {
            "name": f"d{i+1}_beak", "type": "bone", "keyframes": beak_kfs,
        }

        # Eyes: gentle blink-scale + tiny rotation
        for sub_uuid, sub_name, sign in [
            (bones["eye_left"], f"d{i+1}_eye_left", 1.0),
            (bones["eye_right"], f"d{i+1}_eye_right", -1.0),
        ]:
            kfs = []
            for k in range(STEPS + 1):
                t = (k / STEPS) * IDLE_LEN
                ph = (t / period) * 2 * math.pi + phase_off
                # Subtle blink — scale Y dips to 0.4 momentarily on a 5s cycle
                blink_phase = (t / IDLE_LEN) * 2 * math.pi + i * 0.5
                blink = math.sin(blink_phase * 2) ** 16  # sharp pulse
                sy = 1.0 - blink * 0.55
                kfs.append(make_keyframe("scale", t, x=1.0, y=sy, z=1.0))
                ry = math.sin(ph) * 2.5 * sign
                kfs.append(make_keyframe("rotation", t, x=0, y=ry, z=0))
            idle_animators[sub_uuid] = {
                "name": sub_name, "type": "bone", "keyframes": kfs,
            }

        # Tail: flap counter to bob
        tail_kfs = []
        for k in range(STEPS + 1):
            t = (k / STEPS) * IDLE_LEN
            ph = (t / period) * 2 * math.pi + phase_off + 0.5
            rz = 25.0 + math.sin(ph) * 8.0
            rx = math.cos(ph) * 4.0
            ry = math.sin(ph * 2) * 3.0
            tail_kfs.append(make_keyframe("rotation", t, x=rx, y=ry, z=rz))
        idle_animators[bones["tail"]] = {
            "name": f"d{i+1}_tail", "type": "bone", "keyframes": tail_kfs,
        }

        # Wakes: each oscillates as duck bobs — fan in and out
        for j, (wgu, wg) in enumerate(bones["wake_groups"]):
            wake_kfs = []
            wake_period = 1.0 + (j % 3) * 0.15
            for k in range(STEPS + 1):
                t = (k / STEPS) * IDLE_LEN
                ph = (t / wake_period) * 2 * math.pi + phase_off + j * 0.3
                ry = math.sin(ph) * 12.0
                rx = math.cos(ph * 1.5) * 4.0
                rz = math.sin(ph * 0.6) * 3.0
                wake_kfs.append(make_keyframe("rotation", t, x=rx, y=ry, z=rz))
            for k in range(STEPS + 1):
                t = (k / STEPS) * IDLE_LEN
                ph = (t / wake_period) * 2 * math.pi + phase_off + j * 0.3
                xo = math.cos(ph * 0.6) * 0.18
                yo = math.sin(ph) * 0.22
                zo = math.sin(ph * 1.1) * 0.14
                wake_kfs.append(make_keyframe("position", t, x=xo, y=yo, z=zo))
            for k in range(STEPS + 1):
                t = (k / STEPS) * IDLE_LEN
                ph = (t / wake_period) * 2 * math.pi + phase_off + j * 0.3
                s = 1.0 + 0.06 * math.sin(ph * 2)
                wake_kfs.append(make_keyframe("scale", t, x=s, y=s, z=s))
            idle_animators[wgu] = {
                "name": wg["name"], "type": "bone", "keyframes": wake_kfs,
            }

        # Subtle body breathing
        for idx_b, (gu, g) in enumerate(bones["body_groups"]):
            kfs = []
            for k in range(STEPS + 1):
                t = (k / STEPS) * IDLE_LEN
                ph = (t / period) * 2 * math.pi + phase_off + idx_b * 0.25
                s = 1.0 + 0.02 * (1.0 - math.cos(ph))
                kfs.append(make_keyframe("scale", t, x=s, y=s, z=s))
            for k in range(STEPS + 1):
                t = (k / STEPS) * IDLE_LEN
                ph = (t / period) * 2 * math.pi + phase_off + idx_b * 0.25
                xo = math.sin(ph) * 0.04
                yo = math.cos(ph * 1.2) * 0.03
                kfs.append(make_keyframe("position", t, x=xo, y=yo, z=0))
            idle_animators[gu] = {
                "name": g["name"], "type": "bone", "keyframes": kfs,
            }

    # Splash rings: pulse scale per duck (with same phase as duck bob)
    for i, (gu, g) in enumerate(splash_groups):
        period = 2.0
        phase_off = i * (math.pi / 2)
        kfs = []
        for k in range(STEPS + 1):
            t = (k / STEPS) * IDLE_LEN
            ph = (t / period) * 2 * math.pi + phase_off
            # Pulse: scale up when duck is at lowest bob (ph around -pi/2)
            pulse = 0.5 - 0.5 * math.cos(ph)  # 0..1
            sx = 1.0 + 0.4 * pulse
            sz = 1.0 + 0.4 * pulse
            sy = 1.0 + 0.05 * math.sin(ph * 2)
            kfs.append(make_keyframe("scale", t, x=sx, y=sy, z=sz))
        for k in range(STEPS + 1):
            t = (k / STEPS) * IDLE_LEN
            ph = (t / period) * 2 * math.pi + phase_off
            ry = (t / IDLE_LEN) * 60.0  # slow rotation
            kfs.append(make_keyframe("rotation", t, x=0, y=ry, z=0))
        # The splash ring's WORLD position should follow the orbiting duck.
        # Splash ring is in root space at fixed position; we move it in a circle
        # to track the corresponding duck.
        for k in range(STEPS + 1):
            t = (k / STEPS) * IDLE_LEN
            base_angle = i * 90.0
            ang = base_angle + (t / IDLE_LEN) * 360.0
            x = math.cos(math.radians(ang)) * RADIUS
            z = math.sin(math.radians(ang)) * RADIUS
            # Splash ring base position is already at i*90°, so emit deltas
            base_x = math.cos(math.radians(base_angle)) * RADIUS
            base_z = math.sin(math.radians(base_angle)) * RADIUS
            dx = x - base_x
            dz = z - base_z
            kfs.append(make_keyframe("position", t, x=dx, y=0, z=dz))
        idle_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    # Disc slabs: subtle individual ripple
    for i, (gu, g) in enumerate(disc_groups):
        kfs = []
        for k in range(STEPS + 1):
            t = (k / STEPS) * IDLE_LEN
            ph = (t / IDLE_LEN) * 2 * math.pi + i * 0.6
            py = math.sin(ph) * 0.08
            kfs.append(make_keyframe("position", t, x=0, y=py, z=0))
        for k in range(STEPS + 1):
            t = (k / STEPS) * IDLE_LEN
            ph = (t / IDLE_LEN) * 2 * math.pi + i * 0.6
            s = 1.0 + 0.025 * math.sin(ph * 2)
            kfs.append(make_keyframe("scale", t, x=s, y=1.0, z=s))
        idle_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    b.add_animation("idle", length=IDLE_LEN, animators=idle_animators,
                    loop="loop", override=False)

    # ----------------------------- DISSIPATE -----------------------------
    # 0.6s. Ducks all simultaneously increase orbital speed then fly outward
    # on tangent, continuing to bob as they scale to 0. Water disc expands
    # and snaps. Splash rings follow.
    diss_animators = {}

    diss_animators[root_uuid] = {
        "name": "root", "type": "bone", "keyframes": [
            make_keyframe("scale", 0.00, x=1, y=1, z=1),
            make_keyframe("scale", DISS_LEN, x=1, y=1, z=1),
        ],
    }

    # Water disc: expand outward and snap (final scale = 0)
    disc_kfs = [
        make_keyframe("scale", 0.00, x=1.0, y=1.0, z=1.0),
        make_keyframe("scale", 0.20, x=1.4, y=0.6, z=1.4),
        make_keyframe("scale", 0.40, x=2.0, y=0.3, z=2.0),
        make_keyframe("scale", 0.50, x=2.4, y=0.15, z=2.4),
        make_keyframe("scale", DISS_LEN, x=0.0, y=0.0, z=0.0),
        make_keyframe("rotation", 0.00, x=0, y=0, z=0),
        make_keyframe("rotation", DISS_LEN, x=0, y=180, z=0),
    ]
    diss_animators[disc_root_uuid] = {
        "name": "water_disc", "type": "bone", "keyframes": disc_kfs,
    }

    # Each duck orbit: speed up rotation, then after it builds, fly outward
    # Also we fade its scale at end.
    for i, orbit_uuid in enumerate(duck_orbit_uuids):
        base_angle = i * 90.0
        kfs = []
        # Acceleration profile for rotation: 0 → 0.25s slow, 0.25→0.6s fast
        # We'll bake angles directly across DISS_LEN.
        ANG_STEPS = 24
        for k in range(ANG_STEPS + 1):
            t = (k / ANG_STEPS) * DISS_LEN
            # Quadratic accel: angle = base + 720 * (t/DISS_LEN)^2 (1 full + extra)
            extra = 540.0 * ((t / DISS_LEN) ** 1.7)
            ang = base_angle + extra
            kfs.append(make_keyframe("rotation", t, x=0, y=ang, z=0))
        # Scale fade at the end
        kfs.append(make_keyframe("scale", 0.00, x=1.0, y=1.0, z=1.0))
        kfs.append(make_keyframe("scale", 0.40, x=1.0, y=1.0, z=1.0))
        kfs.append(make_keyframe("scale", DISS_LEN, x=0.0, y=0.0, z=0.0))
        diss_animators[orbit_uuid] = {
            "name": f"d{i+1}_orbit", "type": "bone", "keyframes": kfs,
        }

    # Each duck local bone: continue bob, plus tangential outward flight via
    # an outward X offset (relative to duck-local — duck's local +X currently
    # points inward (toward orbit centre) because of the offset rotation 180°).
    # So to fly outward, we need to push -X locally (negative X = outward).
    # We also keep bobbing.
    for i, local_uuid in enumerate(duck_local_uuids):
        period = 2.0
        phase_off = i * (math.pi / 2)
        kfs = []
        # Bob continues
        BOB_STEPS = 24
        for k in range(BOB_STEPS + 1):
            t = (k / BOB_STEPS) * DISS_LEN
            ph = (t / period) * 2 * math.pi + phase_off
            py = math.sin(ph) * 0.5
            rz = math.cos(ph) * 4.0
            # Outward X: ramps from 0 to large negative (local +X is toward centre,
            # so -X is outward in world after the parent's 180° flip)
            outward_t = max(0.0, (t - 0.20) / (DISS_LEN - 0.20))
            ox = -outward_t * 12.0
            kfs.append(make_keyframe("position", t, x=ox, y=py, z=0))
            kfs.append(make_keyframe("rotation", t, x=0, y=0, z=rz))
        # Scale fade: scale down to 0 across the duration
        kfs.append(make_keyframe("scale", 0.00, x=1, y=1, z=1))
        kfs.append(make_keyframe("scale", 0.30, x=1.05, y=1.05, z=1.05))
        kfs.append(make_keyframe("scale", DISS_LEN, x=0.0, y=0.0, z=0.0))
        diss_animators[local_uuid] = {
            "name": f"d{i+1}_local", "type": "bone", "keyframes": kfs,
        }

    # Each duck's inner parts: small flutter as they fly outward
    for i, bones in enumerate(duck_bone_dicts):
        # Head: pitch back slightly (looks back as it flees)
        head_kfs = [
            make_keyframe("rotation", 0.00, x=0, y=0, z=0),
            make_keyframe("rotation", 0.30, x=10, y=15, z=5),
            make_keyframe("rotation", DISS_LEN, x=20, y=30, z=10),
            make_keyframe("scale", 0.00, x=1, y=1, z=1),
            make_keyframe("scale", DISS_LEN, x=1.0, y=1.0, z=1.0),
        ]
        diss_animators[bones["head"]] = {
            "name": f"d{i+1}_head", "type": "bone", "keyframes": head_kfs,
        }
        # Beak: open wider
        beak_kfs = [
            make_keyframe("rotation", 0.00, x=0, y=0, z=0),
            make_keyframe("rotation", 0.20, x=0, y=0, z=-10),
            make_keyframe("rotation", DISS_LEN, x=0, y=0, z=-20),
            make_keyframe("scale", 0.00, x=1, y=1, z=1),
            make_keyframe("scale", 0.40, x=1.15, y=1.15, z=1.15),
            make_keyframe("scale", DISS_LEN, x=1.0, y=1.0, z=1.0),
        ]
        diss_animators[bones["beak"]] = {
            "name": f"d{i+1}_beak", "type": "bone", "keyframes": beak_kfs,
        }
        # Eyes: scale up briefly (alarmed) then continue
        for sub_uuid, sub_name in [
            (bones["eye_left"], f"d{i+1}_eye_left"),
            (bones["eye_right"], f"d{i+1}_eye_right"),
        ]:
            kfs = [
                make_keyframe("scale", 0.00, x=1.0, y=1.0, z=1.0),
                make_keyframe("scale", 0.10, x=1.4, y=1.4, z=1.4),
                make_keyframe("scale", 0.35, x=1.2, y=1.2, z=1.2),
                make_keyframe("scale", DISS_LEN, x=1.0, y=1.0, z=1.0),
            ]
            diss_animators[sub_uuid] = {
                "name": sub_name, "type": "bone", "keyframes": kfs,
            }
        # Tail: flap harder
        tail_kfs = []
        for k in range(13):
            t = (k / 12) * DISS_LEN
            rz = 25.0 + math.sin(t * 30) * 14.0  # rapid flapping
            tail_kfs.append(make_keyframe("rotation", t, x=0, y=0, z=rz))
        diss_animators[bones["tail"]] = {
            "name": f"d{i+1}_tail", "type": "bone", "keyframes": tail_kfs,
        }
        # Wakes: stretch backward and fade
        for j, (wgu, wg) in enumerate(bones["wake_groups"]):
            kfs = [
                make_keyframe("position", 0.00, x=0, y=0, z=0),
                make_keyframe("position", 0.30, x=2.5, y=0, z=0),  # local +X = inward
                make_keyframe("position", DISS_LEN, x=5.0, y=0, z=0),
                make_keyframe("scale", 0.00, x=1, y=1, z=1),
                make_keyframe("scale", 0.20, x=1.4, y=1.0, z=1.4),
                make_keyframe("scale", DISS_LEN, x=0.0, y=0.0, z=0.0),
                make_keyframe("rotation", 0.00, x=0, y=0, z=0),
                make_keyframe("rotation", DISS_LEN, x=0, y=180, z=0),
            ]
            diss_animators[wgu] = {
                "name": wg["name"], "type": "bone", "keyframes": kfs,
            }
        # Body cubes: subtle drift with the local outward motion
        for idx_b, (gu, g) in enumerate(bones["body_groups"]):
            kfs = [
                make_keyframe("position", 0.00, x=0, y=0, z=0),
                make_keyframe("position", DISS_LEN, x=0.4 * (idx_b - 2), y=0.2, z=0.3 * (1 if idx_b % 2 else -1)),
                make_keyframe("rotation", 0.00, x=0, y=0, z=0),
                make_keyframe("rotation", DISS_LEN,
                              x=60 * (1 if idx_b % 2 else -1),
                              y=80 * (1 if idx_b % 3 else -1),
                              z=100 * (1 if idx_b % 2 == 0 else -1)),
                make_keyframe("scale", 0.00, x=1, y=1, z=1),
                make_keyframe("scale", DISS_LEN, x=1.0, y=1.0, z=1.0),
            ]
            diss_animators[gu] = {
                "name": g["name"], "type": "bone", "keyframes": kfs,
            }

    # Splash rings: follow ducks (same orbit motion accelerated), then snap.
    for i, (gu, g) in enumerate(splash_groups):
        base_angle = i * 90.0
        kfs = []
        ANG_STEPS = 24
        for k in range(ANG_STEPS + 1):
            t = (k / ANG_STEPS) * DISS_LEN
            extra = 540.0 * ((t / DISS_LEN) ** 1.7)
            ang = base_angle + extra
            x = math.cos(math.radians(ang)) * RADIUS
            z = math.sin(math.radians(ang)) * RADIUS
            base_x = math.cos(math.radians(base_angle)) * RADIUS
            base_z = math.sin(math.radians(base_angle)) * RADIUS
            # Also stretch outward as duck flies out
            outward_t = max(0.0, (t - 0.20) / (DISS_LEN - 0.20))
            r_extra = outward_t * 8.0
            x_out = math.cos(math.radians(ang)) * (RADIUS + r_extra)
            z_out = math.sin(math.radians(ang)) * (RADIUS + r_extra)
            dx = x_out - base_x
            dz = z_out - base_z
            kfs.append(make_keyframe("position", t, x=dx, y=0, z=dz))
        # Scale: pulse big then snap to 0
        kfs.append(make_keyframe("scale", 0.00, x=1, y=1, z=1))
        kfs.append(make_keyframe("scale", 0.20, x=1.6, y=1.2, z=1.6))
        kfs.append(make_keyframe("scale", 0.45, x=2.2, y=0.4, z=2.2))
        kfs.append(make_keyframe("scale", DISS_LEN, x=0.0, y=0.0, z=0.0))
        diss_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    # Water disc slabs: stretch outward as the disc explodes
    for i, (gu, g) in enumerate(disc_groups):
        ang = i * (360.0 / DISC_SLABS)
        kfs = [
            make_keyframe("position", 0.00, x=0, y=0, z=0),
            make_keyframe("position", 0.40,
                          x=math.cos(math.radians(ang)) * 4.0, y=0,
                          z=math.sin(math.radians(ang)) * 4.0),
            make_keyframe("position", DISS_LEN,
                          x=math.cos(math.radians(ang)) * 9.0, y=0,
                          z=math.sin(math.radians(ang)) * 9.0),
            make_keyframe("scale", 0.00, x=1, y=1, z=1),
            make_keyframe("scale", 0.50, x=1.3, y=0.4, z=1.3),
            make_keyframe("scale", DISS_LEN, x=0.0, y=0.0, z=0.0),
            make_keyframe("rotation", 0.00, x=0, y=0, z=0),
            make_keyframe("rotation", DISS_LEN, x=0, y=180, z=20 * (1 if i % 2 else -1)),
        ]
        diss_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    b.add_animation("dissipate", length=DISS_LEN, animators=diss_animators,
                    loop="once", override=True)

    # ---- WRITE ----
    b.write(OUTPUT_PATH)
    print(f"Wrote {OUTPUT_PATH}")


if __name__ == "__main__":
    build()
