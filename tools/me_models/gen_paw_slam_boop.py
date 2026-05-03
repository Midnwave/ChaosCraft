#!/usr/bin/env python3
"""Generator for paw_slam_boop.bbmodel — Giant Paw Slam 'Boop' attack.

Visual intent: enormous soft cat paw descending from above and pressing into
the ground. Pink paw pad, fluffy cream-white fur, cute toe beans. Horror lives
entirely in the scale.
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
    add_noise_overlay,
    color_lerp,
    fill_rect,
)

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/fluffy/me_attacks/paw_slam_boop.bbmodel"


# ---------------------------------------------------------------------------
# TEXTURES
# ---------------------------------------------------------------------------

def build_fur_texture():
    """Texture 0: plush cream fur for toe beans + fur overflow pieces."""
    w = h = 64
    base   = hex_to_rgba("#fff8f0")
    mid    = hex_to_rgba("#f0e0d0")
    shadow = hex_to_rgba("#d0b8a8")
    deep   = hex_to_rgba("#9a7860")
    spec   = hex_to_rgba("#ffffff")
    emiss  = hex_to_rgba("#ffb8d0")

    pixels = [base] * (w * h)

    # Soft radial mid-tone wash for plush volumetric feel
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - 32) ** 2 + (y - 32) ** 2)
            t = min(1.0, d / 40.0)
            pixels[y * w + x] = color_lerp(base, mid, t * 0.55)

    # Fur direction bands every 5 rows — slightly darker
    for y in range(h):
        if y % 5 == 0:
            for x in range(w):
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, shadow, 0.35)
        elif y % 5 == 1:
            for x in range(w):
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, shadow, 0.18)

    # Deep fold shadows: a few diagonal streaks for fluff clumps
    import random
    rng = random.Random(101)
    for _ in range(28):
        sx = rng.randrange(0, w - 6)
        sy = rng.randrange(2, h - 4)
        length = rng.randrange(3, 7)
        for k in range(length):
            xx = sx + k
            yy = sy + (k // 3)
            if 0 <= xx < w and 0 <= yy < h:
                p = pixels[yy * w + xx]
                pixels[yy * w + xx] = color_lerp(p, deep, 0.55)

    # Specular cluster around (10,6)
    for dy in range(-2, 3):
        for dx in range(-2, 3):
            x = 10 + dx
            y = 6 + dy
            if 0 <= x < w and 0 <= y < h:
                d = math.sqrt(dx * dx + dy * dy)
                t = max(0.0, 1.0 - d / 2.5)
                if t > 0:
                    p = pixels[y * w + x]
                    pixels[y * w + x] = color_lerp(p, spec, t)

    # Scattered emissive baby-pink 3px pops at brightest zones
    rng2 = random.Random(7)
    pink_zones = [(10, 6), (50, 10), (20, 26), (44, 34), (12, 48), (54, 50), (32, 18)]
    for (cx, cy) in pink_zones:
        for dy in range(-1, 2):
            for dx in range(-1, 2):
                x = cx + dx
                y = cy + dy
                if 0 <= x < w and 0 <= y < h:
                    if abs(dx) + abs(dy) <= 2:
                        p = pixels[y * w + x]
                        pixels[y * w + x] = color_lerp(p, emiss, 0.55)

    # Final dust of light noise for plush grain — high density for detail
    add_noise_overlay(pixels, w, h, shadow, base, density=0.18, seed=33)
    add_noise_overlay(pixels, w, h, mid, spec, density=0.12, seed=77)
    add_noise_overlay(pixels, w, h, deep, mid, density=0.08, seed=121)

    # Per-pixel jitter — independent per-channel for max entropy
    import random as _r
    rj = _r.Random(909)
    for y in range(h):
        for x in range(w):
            p = pixels[y * w + x]
            jr = rj.randint(-32, 32)
            jg = rj.randint(-32, 32)
            jb = rj.randint(-32, 32)
            r2 = max(0, min(255, p[0] + jr))
            g2 = max(0, min(255, p[1] + jg))
            b2 = max(0, min(255, p[2] + jb))
            pixels[y * w + x] = (r2, g2, b2, p[3])

    # Extra fine fur fibre stippling: per-pixel deeper offset every 4th pixel
    rj2 = _r.Random(1313)
    for i in range(len(pixels)):
        if rj2.random() < 0.3:
            p = pixels[i]
            jr = rj2.randint(-40, 40)
            jg = rj2.randint(-40, 40)
            jb = rj2.randint(-40, 40)
            pixels[i] = (
                max(0, min(255, p[0] + jr)),
                max(0, min(255, p[1] + jg)),
                max(0, min(255, p[2] + jb)),
                p[3],
            )

    return png_from_pixels(pixels, w, h)


def build_pad_texture():
    """Texture 1: pink paw pad for hero pad + ground impression cracks + dust."""
    w = h = 64
    base   = hex_to_rgba("#ffb8c8")
    deep   = hex_to_rgba("#e890a8")
    shadow = hex_to_rgba("#c06880")
    high   = hex_to_rgba("#ffd0dc")
    spec   = hex_to_rgba("#fff0f4")
    emiss  = hex_to_rgba("#ffe8f0")

    pixels = [base] * (w * h)

    # Radial gradient: highlight centre → base → deep
    cx, cy = 32, 32
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            r = min(1.0, d / 32.0)
            if r < 0.35:
                t = r / 0.35
                pixels[y * w + x] = color_lerp(high, base, t)
            elif r < 0.75:
                t = (r - 0.35) / 0.40
                pixels[y * w + x] = color_lerp(base, deep, t)
            else:
                t = (r - 0.75) / 0.25
                pixels[y * w + x] = color_lerp(deep, shadow, t)

    # Concentric oval rings at 35% and 65% of the radius
    for y in range(h):
        for x in range(w):
            dx = (x - cx)
            dy = (y - cy) * 1.15  # slight oval
            d = math.sqrt(dx * dx + dy * dy)
            r35 = 32 * 0.35
            r65 = 32 * 0.65
            if abs(d - r35) < 0.7:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, deep, 0.55)
            elif abs(d - r65) < 0.7:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, shadow, 0.45)

    # Specular highlight smear top-left of centre
    for dy in range(-3, 4):
        for dx in range(-4, 5):
            x = 26 + dx
            y = 26 + dy
            if 0 <= x < w and 0 <= y < h:
                d = math.sqrt(dx * dx + dy * dy)
                t = max(0.0, 1.0 - d / 4.0)
                if t > 0:
                    p = pixels[y * w + x]
                    pixels[y * w + x] = color_lerp(p, spec, t * 0.65)

    # Emissive bright pink-white 3px at centre highlight
    for dy in range(-1, 2):
        for dx in range(-1, 2):
            x = 32 + dx
            y = 30 + dy
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, emiss, 0.85)

    # Pink noise for skin grain — denser for filesize bulk and texture detail
    add_noise_overlay(pixels, w, h, shadow, high, density=0.16, seed=88)
    add_noise_overlay(pixels, w, h, deep, spec, density=0.10, seed=144)
    add_noise_overlay(pixels, w, h, base, emiss, density=0.06, seed=211)

    # Per-pixel jitter — independent per-channel for organic skin grain
    import random as _r
    rj = _r.Random(303)
    for y in range(h):
        for x in range(w):
            p = pixels[y * w + x]
            jr = rj.randint(-28, 28)
            jg = rj.randint(-28, 28)
            jb = rj.randint(-28, 28)
            r2 = max(0, min(255, p[0] + jr))
            g2 = max(0, min(255, p[1] + jg))
            b2 = max(0, min(255, p[2] + jb))
            pixels[y * w + x] = (r2, g2, b2, p[3])

    # Extra dimpling: random skin pores
    rj2 = _r.Random(515)
    for i in range(len(pixels)):
        if rj2.random() < 0.28:
            p = pixels[i]
            jr = rj2.randint(-36, 36)
            jg = rj2.randint(-36, 36)
            jb = rj2.randint(-36, 36)
            pixels[i] = (
                max(0, min(255, p[0] + jr)),
                max(0, min(255, p[1] + jg)),
                max(0, min(255, p[2] + jb)),
                p[3],
            )

    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# MODEL BUILD
# ---------------------------------------------------------------------------

def build():
    b = Builder("paw_slam_boop", resolution=(64, 64), visible_box=(8, 8, 0))

    # ---- TEXTURES ----
    b.add_texture("paw_fur", build_fur_texture())
    b.add_texture("paw_pad", build_pad_texture())

    # Common face shorthands
    fur_face = lambda: uniform_face(0, 0, 64, 64, tex_index=0)
    pad_face = lambda: uniform_face(0, 0, 64, 64, tex_index=1)
    pad_outer_face = lambda: uniform_face(48, 48, 64, 64, tex_index=1)  # darker outer ring zone

    # =====================================================================
    # 1) MAIN PAW PAD — 4 overlapping flat slabs forming a wide oval at Y=0
    #    Wide on X, shorter on Z. Texture 1.
    # =====================================================================
    pad_cubes = []
    # Big central slab
    pad_cubes.append(b.add_cube(
        "pad_center",
        from_=[-7, 0, -4], to_=[7, 1.2, 4],
        faces=pad_face(),
    ))
    # Left bulge
    pad_cubes.append(b.add_cube(
        "pad_left",
        from_=[-9, 0.05, -3], to_=[-5, 1.15, 3],
        faces=pad_face(),
    ))
    # Right bulge
    pad_cubes.append(b.add_cube(
        "pad_right",
        from_=[5, 0.05, -3], to_=[9, 1.15, 3],
        faces=pad_face(),
    ))
    # Front bulge (tapers shorter Z)
    pad_cubes.append(b.add_cube(
        "pad_front",
        from_=[-6, 0.1, 3.5], to_=[6, 1.1, 5],
        faces=pad_face(),
    ))
    pad_group_uuid = make_uuid()
    pad_group = b.make_group("paw_pad", [0, 0, 0], pad_cubes, group_uuid=pad_group_uuid)

    # =====================================================================
    # 2) TOE BEANS — 4 toes, each 3 overlapping cubes, gentle arc at Y=0.5
    #    Each its own bone.
    # =====================================================================
    # Arc positions across X, with toes curving slightly forward (negative Z = behind pad? we'll place them above pad on +Z side)
    # Spec: gentle arc above the main pad at Y=0.5. We'll place on -Z side of pad (behind, away from front bulge).
    toe_positions = [
        (-6.0, -0.2),   # toe 1: leftmost, slightly back
        (-2.2, -1.4),   # toe 2: inner-left, forward
        ( 2.2, -1.4),   # toe 3: inner-right, forward
        ( 6.0, -0.2),   # toe 4: rightmost, slightly back
    ]
    toe_groups = []  # (group_uuid, group_dict)
    for i, (tx, tz) in enumerate(toe_positions):
        toe_cubes = []
        # base cube — central rounded ball
        toe_cubes.append(b.add_cube(
            f"toe{i+1}_core",
            from_=[tx - 1.6, 0.5, tz - 1.6], to_=[tx + 1.6, 2.4, tz + 1.6],
            faces=fur_face(),
        ))
        # offset cube up-left for soft overlap
        toe_cubes.append(b.add_cube(
            f"toe{i+1}_upper",
            from_=[tx - 1.3, 0.9, tz - 1.3], to_=[tx + 1.3, 2.7, tz + 1.3],
            faces=fur_face(),
        ))
        # bottom slab for pad contact
        toe_cubes.append(b.add_cube(
            f"toe{i+1}_pad",
            from_=[tx - 1.4, 0.3, tz - 1.4], to_=[tx + 1.4, 1.0, tz + 1.4],
            faces=pad_face(),
        ))
        gu = make_uuid()
        g = b.make_group(f"toe_{i+1}", [tx, 1.4, tz], toe_cubes, group_uuid=gu)
        toe_groups.append((gu, g))

    # =====================================================================
    # 3) FUR OVERFLOW — 12 small flat slab pieces around perimeter at Y=0.1,
    #    angled outward and slightly upward. Each its own bone.
    # =====================================================================
    fur_groups = []
    for i in range(12):
        ang = (i / 12.0) * 2 * math.pi
        # perimeter radius ~ 9 on X, 5 on Z
        r_x = 9.0
        r_z = 5.0
        cx = math.cos(ang) * r_x
        cz = math.sin(ang) * r_z
        # cube is a small flat slab pointing outward
        # local cube: small thin wedge
        cube_uuid = b.add_cube(
            f"fur_overflow_{i+1}",
            from_=[cx - 0.9, 0.1, cz - 0.6], to_=[cx + 0.9, 0.5, cz + 0.6],
            faces=fur_face(),
        )
        # Outward+upward angle
        rot_y = math.degrees(ang)
        rot_x = -8.0
        gu = make_uuid()
        g = b.make_group(
            f"fur_{i+1}", [cx, 0.3, cz], [cube_uuid],
            rotation=[rot_x, rot_y, 0], group_uuid=gu,
        )
        fur_groups.append((gu, g))

    # =====================================================================
    # 4) GROUND IMPRESSION CRACKS — 6 flat slabs radiating from below pad,
    #    using texture 1 outer/dark zone.
    # =====================================================================
    crack_groups = []
    for i in range(6):
        ang = (i / 6.0) * 2 * math.pi + 0.3
        r0 = 4.0
        r1 = 9.5
        # Place crack along radial direction
        cx = math.cos(ang) * (r0 + r1) / 2
        cz = math.sin(ang) * (r0 + r1) / 2
        length = r1 - r0
        cube_uuid = b.add_cube(
            f"crack_slab_{i+1}",
            from_=[-length / 2, -0.05, -0.4], to_=[length / 2, 0.05, 0.4],
            faces=pad_outer_face(),
        )
        rot_y = math.degrees(ang)
        gu = make_uuid()
        g = b.make_group(
            f"crack_{i+1}", [cx, 0, cz], [cube_uuid],
            rotation=[0, rot_y, 0], group_uuid=gu,
        )
        crack_groups.append((gu, g))

    # =====================================================================
    # 5) DUST PUFFS — 8 small flat cubes blasting outward at ground level.
    #    Texture 1.
    # =====================================================================
    dust_groups = []
    for i in range(8):
        ang = (i / 8.0) * 2 * math.pi + 0.15
        r = 6.0
        cx = math.cos(ang) * r
        cz = math.sin(ang) * r
        cube_uuid = b.add_cube(
            f"dust_puff_{i+1}",
            from_=[-0.9, 0.0, -0.9], to_=[0.9, 0.6, 0.9],
            faces=pad_face(),
        )
        rot_y = math.degrees(ang)
        gu = make_uuid()
        g = b.make_group(
            f"dust_{i+1}", [cx, 0.3, cz], [cube_uuid],
            rotation=[0, rot_y, 0], group_uuid=gu,
        )
        dust_groups.append((gu, g))

    # =====================================================================
    # ROOT BONE — contains everything for global motion.
    # =====================================================================
    root_children = [pad_group]
    for (_uu, g) in toe_groups:
        root_children.append(g)
    for (_uu, g) in fur_groups:
        root_children.append(g)
    for (_uu, g) in crack_groups:
        root_children.append(g)
    for (_uu, g) in dust_groups:
        root_children.append(g)

    root_uuid = make_uuid()
    root_group = b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid)
    b.outliner.append(root_group)

    # =====================================================================
    # ANIMATIONS
    # =====================================================================

    # ----- SPAWN: 1.0s, override=true -----
    spawn_animators = {}

    # ROOT bone: descends from Y+20 → drift up at Y=3 → SLAM to Y=0
    root_kfs = [
        # position channel
        make_keyframe("position", 0.00, x=0, y=20, z=0),
        make_keyframe("position", 0.55, x=0, y=3, z=0),     # slow descent
        make_keyframe("position", 0.65, x=0, y=3.5, z=0),   # anticipation drift up
        make_keyframe("position", 0.72, x=0, y=3.3, z=0),   # held
        make_keyframe("position", 0.78, x=0, y=0, z=0, interpolation="linear"),  # SLAM
        make_keyframe("position", 0.82, x=0, y=-0.15, z=0), # tiny squash
        make_keyframe("position", 0.90, x=0, y=0.05, z=0),  # bounce
        make_keyframe("position", 1.00, x=0, y=0, z=0),
        # scale: very subtle squash on impact
        make_keyframe("scale", 0.00, x=1, y=1, z=1),
        make_keyframe("scale", 0.78, x=1, y=1, z=1),
        make_keyframe("scale", 0.82, x=1.06, y=0.92, z=1.06),
        make_keyframe("scale", 0.92, x=0.98, y=1.02, z=0.98),
        make_keyframe("scale", 1.00, x=1, y=1, z=1),
    ]
    spawn_animators[root_uuid] = {
        "name": "root", "type": "bone", "keyframes": root_kfs,
    }

    # PAW PAD: stays attached to root but a tiny landing flatten
    pad_kfs_spawn = [
        make_keyframe("scale", 0.00, x=1, y=1, z=1),
        make_keyframe("scale", 0.78, x=1, y=1, z=1),
        make_keyframe("scale", 0.82, x=1.04, y=0.85, z=1.04),
        make_keyframe("scale", 0.92, x=1.0, y=1.02, z=1.0),
        make_keyframe("scale", 1.00, x=1, y=1, z=1),
    ]
    spawn_animators[pad_group_uuid] = {
        "name": "paw_pad", "type": "bone", "keyframes": pad_kfs_spawn,
    }

    # TOE BEANS: each compresses on impact, then bounces back (2-frame settle)
    for idx, (gu, g) in enumerate(toe_groups):
        kfs = [
            make_keyframe("scale", 0.00, x=1, y=1, z=1),
            make_keyframe("scale", 0.78, x=1, y=1, z=1),
            # Impact compress
            make_keyframe("scale", 0.82, x=1.10, y=0.65, z=1.10),
            # First settle frame
            make_keyframe("scale", 0.88, x=0.95, y=1.10, z=0.95),
            # Second settle frame
            make_keyframe("scale", 0.94, x=1.02, y=0.97, z=1.02),
            make_keyframe("scale", 1.00, x=1, y=1, z=1),
            # Tiny vertical bounce
            make_keyframe("position", 0.00, x=0, y=0, z=0),
            make_keyframe("position", 0.78, x=0, y=0, z=0),
            make_keyframe("position", 0.82, x=0, y=-0.15, z=0),
            make_keyframe("position", 0.88, x=0, y=0.10, z=0),
            make_keyframe("position", 0.94, x=0, y=-0.03, z=0),
            make_keyframe("position", 1.00, x=0, y=0, z=0),
        ]
        spawn_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    # FUR OVERFLOW: deploy from perimeter at impact (snap outward + scale up)
    for idx, (gu, g) in enumerate(fur_groups):
        ang = (idx / 12.0) * 2 * math.pi
        out_x = math.cos(ang) * 1.4
        out_z = math.sin(ang) * 1.0
        kfs = [
            make_keyframe("scale", 0.00, x=0.05, y=0.05, z=0.05),
            make_keyframe("scale", 0.78, x=0.05, y=0.05, z=0.05),
            make_keyframe("scale", 0.82, x=1.2, y=1.2, z=1.2),
            make_keyframe("scale", 0.90, x=1.0, y=1.0, z=1.0),
            make_keyframe("scale", 1.00, x=1, y=1, z=1),
            make_keyframe("position", 0.00, x=-out_x * 0.5, y=-0.4, z=-out_z * 0.5),
            make_keyframe("position", 0.78, x=-out_x * 0.5, y=-0.4, z=-out_z * 0.5),
            make_keyframe("position", 0.82, x=out_x, y=0.2, z=out_z),
            make_keyframe("position", 0.95, x=out_x * 0.7, y=0.0, z=out_z * 0.7),
            make_keyframe("position", 1.00, x=0, y=0, z=0),
        ]
        spawn_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    # GROUND CRACKS: blast outward at impact
    for idx, (gu, g) in enumerate(crack_groups):
        ang = (idx / 6.0) * 2 * math.pi + 0.3
        kfs = [
            make_keyframe("scale", 0.00, x=0.0, y=0.0, z=0.0),
            make_keyframe("scale", 0.78, x=0.0, y=0.0, z=0.0),
            make_keyframe("scale", 0.82, x=1.4, y=1.0, z=1.2),
            make_keyframe("scale", 0.92, x=1.0, y=1.0, z=1.0),
            make_keyframe("scale", 1.00, x=1, y=1, z=1),
        ]
        spawn_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    # DUST PUFFS: blast outward at impact
    for idx, (gu, g) in enumerate(dust_groups):
        ang = (idx / 8.0) * 2 * math.pi + 0.15
        out_x = math.cos(ang) * 3.0
        out_z = math.sin(ang) * 3.0
        kfs = [
            make_keyframe("scale", 0.00, x=0.0, y=0.0, z=0.0),
            make_keyframe("scale", 0.78, x=0.0, y=0.0, z=0.0),
            make_keyframe("scale", 0.82, x=1.5, y=1.5, z=1.5),
            make_keyframe("scale", 0.95, x=1.2, y=1.2, z=1.2),
            make_keyframe("scale", 1.00, x=1, y=1, z=1),
            make_keyframe("position", 0.00, x=0, y=0, z=0),
            make_keyframe("position", 0.78, x=0, y=0, z=0),
            make_keyframe("position", 0.82, x=out_x * 0.4, y=0.5, z=out_z * 0.4),
            make_keyframe("position", 0.95, x=out_x * 0.7, y=0.2, z=out_z * 0.7),
            make_keyframe("position", 1.00, x=out_x * 0.6, y=0, z=out_z * 0.6),
        ]
        spawn_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    b.add_animation("spawn", length=1.0, animators=spawn_animators,
                    loop="once", override=True)

    # ----- IDLE: 4.0s, override=false, loop -----
    idle_animators = {}

    # Root subtle weight shift ±0.5° on X
    root_idle = [
        make_keyframe("rotation", 0.0, x=0, y=0, z=0),
        make_keyframe("rotation", 1.0, x=0.5, y=0, z=0),
        make_keyframe("rotation", 2.0, x=0, y=0, z=0),
        make_keyframe("rotation", 3.0, x=-0.5, y=0, z=0),
        make_keyframe("rotation", 4.0, x=0, y=0, z=0),
        # subtle vertical breathing
        make_keyframe("position", 0.0, x=0, y=0, z=0),
        make_keyframe("position", 2.0, x=0, y=0.08, z=0),
        make_keyframe("position", 4.0, x=0, y=0, z=0),
    ]
    idle_animators[root_uuid] = {
        "name": "root", "type": "bone", "keyframes": root_idle,
    }

    # Each toe pulses scale 1.0→1.04 staggered 2-3s periods
    toe_periods = [2.0, 2.5, 2.7, 3.0]
    toe_phases  = [0.0, 0.6, 1.2, 1.8]
    for i, ((gu, g), period, phase) in enumerate(zip(toe_groups, toe_periods, toe_phases)):
        # Dense keyframes across 4s using cosine pulse
        kfs = []
        steps = 20
        for k in range(steps + 1):
            t = (k / steps) * 4.0
            phase_t = ((t + phase) / period) * 2 * math.pi
            s = 1.0 + 0.02 * (1.0 - math.cos(phase_t))  # 1.0 to 1.04
            kfs.append(make_keyframe("scale", t, x=s, y=s, z=s))
        # Plus a small position breathing
        for k in range(steps + 1):
            t = (k / steps) * 4.0
            phase_t = ((t + phase) / period) * 2 * math.pi
            yy = 0.04 * math.sin(phase_t)
            kfs.append(make_keyframe("position", t, x=0, y=yy, z=0))
        idle_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    # Fur overflow pieces flutter — dense rotation oscillation
    for idx, (gu, g) in enumerate(fur_groups):
        ang = (idx / 12.0) * 2 * math.pi
        period = 1.5 + (idx % 3) * 0.4
        kfs = []
        steps = 16
        for k in range(steps + 1):
            t = (k / steps) * 4.0
            ph = ((t / period) + idx * 0.3) * 2 * math.pi
            rx = math.sin(ph) * 2.5
            ry = math.cos(ph * 1.3) * 1.0
            rz = math.cos(ph) * 1.5
            kfs.append(make_keyframe("rotation", t, x=rx, y=ry, z=rz))
        # Subtle scale shimmer too
        for k in range(steps + 1):
            t = (k / steps) * 4.0
            ph = ((t / period) + idx * 0.3) * 2 * math.pi
            s = 1.0 + 0.03 * (1.0 - math.cos(ph))
            kfs.append(make_keyframe("scale", t, x=s, y=s, z=s))
        idle_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    # Ground impression slabs pulse faintly — dense
    for idx, (gu, g) in enumerate(crack_groups):
        kfs = []
        steps = 16
        for k in range(steps + 1):
            t = (k / steps) * 4.0
            phase_t = ((t / 3.5) + idx * 0.3) * 2 * math.pi
            s = 1.0 + 0.04 * (1.0 - math.cos(phase_t))
            kfs.append(make_keyframe("scale", t, x=s, y=s, z=s))
        idle_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    # Dust puffs gentle idle drift (small ambient motion)
    for idx, (gu, g) in enumerate(dust_groups):
        kfs = []
        steps = 12
        ang = (idx / 8.0) * 2 * math.pi + 0.15
        for k in range(steps + 1):
            t = (k / steps) * 4.0
            ph = ((t / 3.0) + idx * 0.4) * 2 * math.pi
            rx = math.sin(ph) * 1.5
            ry = math.cos(ph * 0.7) * 0.8
            kfs.append(make_keyframe("rotation", t, x=rx, y=ry, z=0))
        idle_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    b.add_animation("idle", length=4.0, animators=idle_animators,
                    loop="loop", override=False)

    # ----- DISSIPATE: 0.6s, override=true -----
    diss_animators = {}

    # Root paw lifts upward at increasing speed
    root_diss = [
        make_keyframe("position", 0.00, x=0, y=0, z=0),
        make_keyframe("position", 0.20, x=0, y=1.5, z=0),    # slow start
        make_keyframe("position", 0.40, x=0, y=6.0, z=0),    # accelerate
        make_keyframe("position", 0.60, x=0, y=18.0, z=0),   # snap up
    ]
    diss_animators[root_uuid] = {
        "name": "root", "type": "bone", "keyframes": root_diss,
    }

    # Toes lift slightly BEFORE pad — leading the lift
    for idx, (gu, g) in enumerate(toe_groups):
        kfs = [
            make_keyframe("position", 0.00, x=0, y=0, z=0),
            make_keyframe("position", 0.10, x=0, y=0.4, z=0),     # toes lift first
            make_keyframe("position", 0.30, x=0, y=0.6, z=0),
            make_keyframe("position", 0.60, x=0, y=0, z=0),
        ]
        diss_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    # Pad scales slightly down trailing
    pad_diss_kfs = [
        make_keyframe("scale", 0.00, x=1, y=1, z=1),
        make_keyframe("scale", 0.30, x=1.0, y=0.95, z=1.0),
        make_keyframe("scale", 0.60, x=0.95, y=0.85, z=0.95),
    ]
    diss_animators[pad_group_uuid] = {
        "name": "paw_pad", "type": "bone", "keyframes": pad_diss_kfs,
    }

    # Fur pieces trail downward then snap
    for idx, (gu, g) in enumerate(fur_groups):
        ang = (idx / 12.0) * 2 * math.pi
        out_x = math.cos(ang) * 0.5
        out_z = math.sin(ang) * 0.3
        kfs = [
            make_keyframe("position", 0.00, x=0, y=0, z=0),
            make_keyframe("position", 0.20, x=0, y=-0.4, z=0),   # trail down
            make_keyframe("position", 0.40, x=out_x, y=-0.6, z=out_z),
            make_keyframe("position", 0.55, x=out_x * 1.5, y=0.5, z=out_z * 1.5),  # snap
            make_keyframe("position", 0.60, x=0, y=0, z=0),
            make_keyframe("scale", 0.00, x=1, y=1, z=1),
            make_keyframe("scale", 0.55, x=1.0, y=1.0, z=1.0),
            make_keyframe("scale", 0.60, x=0.05, y=0.05, z=0.05),
        ]
        diss_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    # Ground impression slabs fade (scale to 0)
    for idx, (gu, g) in enumerate(crack_groups):
        kfs = [
            make_keyframe("scale", 0.00, x=1, y=1, z=1),
            make_keyframe("scale", 0.30, x=0.7, y=0.8, z=0.7),
            make_keyframe("scale", 0.60, x=0.0, y=0.0, z=0.0),
        ]
        diss_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    # Dust puffs continue outward and scale to 0
    for idx, (gu, g) in enumerate(dust_groups):
        ang = (idx / 8.0) * 2 * math.pi + 0.15
        out_x = math.cos(ang) * 4.0
        out_z = math.sin(ang) * 4.0
        kfs = [
            make_keyframe("position", 0.00, x=0, y=0, z=0),
            make_keyframe("position", 0.30, x=out_x * 0.5, y=0.6, z=out_z * 0.5),
            make_keyframe("position", 0.60, x=out_x, y=1.0, z=out_z),
            make_keyframe("scale", 0.00, x=1, y=1, z=1),
            make_keyframe("scale", 0.30, x=1.2, y=1.2, z=1.2),
            make_keyframe("scale", 0.60, x=0.0, y=0.0, z=0.0),
        ]
        diss_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    b.add_animation("dissipate", length=0.6, animators=diss_animators,
                    loop="once", override=True)

    # ---- WRITE ----
    b.write(OUTPUT_PATH)
    print(f"Wrote {OUTPUT_PATH}")


if __name__ == "__main__":
    build()
