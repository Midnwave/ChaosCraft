#!/usr/bin/env python3
"""
Generate butterfly_swarm_flutter.bbmodel — pastel butterfly swarm orbital VFX.

Visual: 12 pastel butterflies orbiting at varying heights/radii, each with
4 wings + body, flapping independently. Plus a central flower and 8 scatter
petals. Four-quadrant pastel palette (yellow, blue, coral, lavender) with
3 butterflies per colour.
"""
import sys, math, random
sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder, make_uuid, make_keyframe, basic_cube_faces, uniform_face,
    png_from_pixels, hex_to_rgba, color_lerp, fill_rect, add_noise_overlay,
)

OUT = "D:/CC/ChaosCraft/src/main/resources/models/fluffy/me_attacks/butterfly_swarm_flutter.bbmodel"


# ---------- TEXTURES ----------
def build_butterfly_texture():
    """Texture 0 (64x64): Four pastel quadrants — top-left butter yellow,
    top-right baby blue, bottom-left soft coral, bottom-right lavender.
    Each quadrant has subtle wing-vein lines and a specular highlight."""
    w, h = 64, 64
    # Quadrant base palette
    yellow_base = hex_to_rgba("#fff0b8")
    blue_base = hex_to_rgba("#b8e8ff")
    coral_base = hex_to_rgba("#ffb8a0")
    lav_base = hex_to_rgba("#d8b8ff")

    yellow_dark = hex_to_rgba("#e8c878")
    blue_dark = hex_to_rgba("#80b8e0")
    coral_dark = hex_to_rgba("#d88060")
    lav_dark = hex_to_rgba("#a878d0")

    yellow_bright = hex_to_rgba("#fff8d8")
    blue_bright = hex_to_rgba("#e0f4ff")
    coral_bright = hex_to_rgba("#ffd8c8")
    lav_bright = hex_to_rgba("#f0e0ff")

    specular = hex_to_rgba("#ffffff")
    black_vein = hex_to_rgba("#3a2840")

    # Init pixels list (filled with magenta to detect bugs — should be fully overwritten)
    pixels = [hex_to_rgba("#ff00ff")] * (w * h)

    # ---- Paint each 32x32 quadrant ----
    quads = [
        # (x0, y0, x1, y1, base, dark, bright)
        (0, 0, 32, 32, yellow_base, yellow_dark, yellow_bright),     # TL: butter yellow
        (32, 0, 64, 32, blue_base, blue_dark, blue_bright),          # TR: baby blue
        (0, 32, 32, 64, coral_base, coral_dark, coral_bright),       # BL: soft coral
        (32, 32, 64, 64, lav_base, lav_dark, lav_bright),            # BR: lavender
    ]

    rng = random.Random(101)

    for (x0, y0, x1, y1, base, dark, bright) in quads:
        # Fill base
        fill_rect(pixels, w, h, x0, y0, x1, y1, base)

        qw = x1 - x0
        qh = y1 - y0
        cx = x0 + qw / 2
        cy = y0 + qh / 2

        # Radial gradient inside the quadrant — bright near upper-inner corner,
        # darker toward outer edge. Gives wing a soft volumetric feel.
        # Bright center offset toward upper-left of quadrant
        bcx = x0 + qw * 0.45
        bcy = y0 + qh * 0.35
        max_r = math.sqrt((qw / 2) ** 2 + (qh / 2) ** 2) * 1.2
        for y in range(y0, y1):
            for x in range(x0, x1):
                d = math.sqrt((x - bcx) ** 2 + (y - bcy) ** 2)
                t = min(1.0, d / max_r)
                # Mix bright (center) -> base (mid) -> dark (edge)
                if t < 0.45:
                    c = color_lerp(bright, base, t / 0.45)
                else:
                    c = color_lerp(base, dark, (t - 0.45) / 0.55)
                pixels[y * w + x] = c

        # Wing-vein line pattern: 1px darker irregular curves radiating from
        # an anchor point near the inner edge (mimics butterfly wing veins).
        anchor_x = x0 + qw * 0.15
        anchor_y = y0 + qh * 0.85
        # Main veins — radiating arcs
        for vein_idx in range(7):
            theta = math.radians(20 + vein_idx * 12 + rng.uniform(-4, 4))
            length = qw * (0.6 + rng.uniform(-0.1, 0.2))
            steps = int(length * 1.5)
            curve = rng.uniform(-0.4, 0.4)
            for s in range(steps):
                t = s / max(1, steps - 1)
                # Curving line from anchor outward
                rx = anchor_x + math.cos(theta) * length * t + math.sin(t * math.pi) * curve * 6
                ry = anchor_y - math.sin(theta) * length * t + math.cos(t * math.pi) * curve * 4
                ix, iy = int(rx), int(ry)
                if x0 <= ix < x1 and y0 <= iy < y1:
                    base_p = pixels[iy * w + ix]
                    pixels[iy * w + ix] = color_lerp(base_p, black_vein, 0.55)
                # Anti-alias the side
                if x0 <= ix + 1 < x1 and y0 <= iy < y1:
                    base_p = pixels[iy * w + ix + 1]
                    pixels[iy * w + ix + 1] = color_lerp(base_p, dark, 0.3)

        # Subtle horizontal cross-veins
        for cv in range(4):
            cv_y = y0 + 6 + cv * 6
            cv_off = rng.randint(-2, 2)
            for x in range(x0 + 4, x1 - 4):
                if x0 <= x < x1 and y0 <= cv_y + cv_off < y1:
                    base_p = pixels[(cv_y + cv_off) * w + x]
                    pixels[(cv_y + cv_off) * w + x] = color_lerp(base_p, dark, 0.25)

        # Wing edge — outer rim slightly darker (defines wing silhouette)
        for y in range(y0, y1):
            for x in range(x0, x1):
                # Distance to nearest edge of quadrant
                de = min(x - x0, x1 - 1 - x, y - y0, y1 - 1 - y)
                if de < 2:
                    base_p = pixels[y * w + x]
                    pixels[y * w + x] = color_lerp(base_p, dark, 0.5 - de * 0.2)

        # Specular dot in the quadrant's brightest zone
        spec_x = int(bcx)
        spec_y = int(bcy)
        for sy in range(spec_y - 1, spec_y + 2):
            for sx in range(spec_x - 1, spec_x + 2):
                if x0 <= sx < x1 and y0 <= sy < y1:
                    d = math.sqrt((sx - spec_x) ** 2 + (sy - spec_y) ** 2)
                    if d <= 1.5:
                        base_p = pixels[sy * w + sx]
                        pixels[sy * w + sx] = color_lerp(base_p, specular, 0.8)
        # Single brightest pixel
        if x0 <= spec_x < x1 and y0 <= spec_y < y1:
            pixels[spec_y * w + spec_x] = specular

        # A few small wing-spot ovals (butterfly eye-spots) — random per quadrant
        for spot_i in range(2):
            sxc = rng.randint(x0 + 8, x1 - 8)
            syc = rng.randint(y0 + 6, y1 - 8)
            sr = rng.randint(2, 3)
            for dy in range(-sr - 1, sr + 2):
                for dx in range(-sr - 1, sr + 2):
                    px = sxc + dx
                    py = syc + dy
                    if x0 <= px < x1 and y0 <= py < y1:
                        d = math.sqrt(dx * dx + dy * dy)
                        if d <= sr - 0.5:
                            base_p = pixels[py * w + px]
                            pixels[py * w + px] = color_lerp(base_p, dark, 0.7)
                        elif d <= sr + 0.5:
                            base_p = pixels[py * w + px]
                            pixels[py * w + px] = color_lerp(base_p, bright, 0.4)

    # Light noise overlay for organic shimmer
    add_noise_overlay(pixels, w, h, hex_to_rgba("#80708890"), specular, density=0.012, seed=222)

    return png_from_pixels(pixels, w, h)


def build_flower_texture():
    """Texture 1 (64x64): soft pink flower petals with yellow centre.
    Used by central flower and scatter petals."""
    w, h = 64, 64
    petal_pink = hex_to_rgba("#ffccd8")
    petal_pink_dark = hex_to_rgba("#e090a8")
    petal_pink_bright = hex_to_rgba("#ffe0e8")
    yellow_centre = hex_to_rgba("#ffe860")
    yellow_emissive = hex_to_rgba("#fff070")
    yellow_dark = hex_to_rgba("#d8b840")
    specular = hex_to_rgba("#ffffff")

    pixels = [petal_pink] * (w * h)

    # Vertical pink gradient body
    for y in range(h):
        t = y / (h - 1)
        if t < 0.5:
            row = color_lerp(petal_pink_bright, petal_pink, t * 2)
        else:
            row = color_lerp(petal_pink, petal_pink_dark, (t - 0.5) * 2)
        for x in range(w):
            pixels[y * w + x] = row

    # Petal vein lines (radiating from bottom centre upward)
    rng = random.Random(303)
    for vein in range(9):
        theta = math.radians(60 + vein * 8 + rng.uniform(-3, 3))
        ax, ay = 32, 60
        length = 50
        for s in range(length):
            t = s / max(1, length - 1)
            rx = ax + math.cos(theta) * length * t * 1.0
            ry = ay - math.sin(theta) * length * t
            ix, iy = int(rx), int(ry)
            if 0 <= ix < w and 0 <= iy < h:
                base_p = pixels[iy * w + ix]
                pixels[iy * w + ix] = color_lerp(base_p, petal_pink_dark, 0.4)

    # Yellow emissive flower centre — circle near top
    cx, cy = 32, 18
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            if d <= 7:
                base_p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(yellow_emissive, yellow_centre, d / 7)
            elif d <= 9:
                base_p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(base_p, yellow_dark, (9 - d) / 2 * 0.7)

    # Stamens — small darker dots inside flower centre
    for sx, sy in [(28, 16), (32, 14), (36, 17), (30, 20), (34, 21), (32, 18)]:
        if 0 <= sx < w and 0 <= sy < h:
            base_p = pixels[sy * w + sx]
            pixels[sy * w + sx] = color_lerp(base_p, yellow_dark, 0.7)

    # Specular sparkle dots scattered
    for sx, sy in [(20, 38), (44, 42), (12, 28), (50, 32), (24, 50), (40, 52)]:
        if 0 <= sx < w and 0 <= sy < h:
            pixels[sy * w + sx] = specular

    # Edge darkening (curl)
    for y in range(h):
        for x in range(w):
            cx0, cy0 = w / 2, h / 2
            dx = (x - cx0) / cx0
            dy = (y - cy0) / cy0
            d = math.sqrt(dx * dx + dy * dy)
            if d > 0.88:
                base_p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(base_p, petal_pink_dark, min(1.0, (d - 0.88) * 4))

    add_noise_overlay(pixels, w, h, petal_pink_dark, specular, density=0.015, seed=444)
    return png_from_pixels(pixels, w, h)


# ---------- HELPERS ----------
# Quadrant UV bounds for the four pastel colours in texture 0
QUAD_UVS = {
    "yellow": (0, 0, 32, 32),
    "blue": (32, 0, 64, 32),
    "coral": (0, 32, 32, 64),
    "lavender": (32, 32, 64, 64),
}


def quad_uv(quad_name, tex_index=0):
    """Build a uniform_face dict using the named pastel quadrant."""
    u1, v1, u2, v2 = QUAD_UVS[quad_name]
    return uniform_face(u1, v1, u2, v2, tex_index=tex_index)


# ---------- BUILD ----------
def build():
    b = Builder("butterfly_swarm_flutter", resolution=(64, 64))
    b.add_texture("butterfly_wings", build_butterfly_texture())
    b.add_texture("flower_petals", build_flower_texture())

    rng = random.Random(7777)

    # ========== 12 BUTTERFLIES ==========
    # 3 butterflies per pastel colour. Distribute around caster at varied
    # heights (Y=2..10) and orbit radii (5..9).
    butterfly_quads = (
        ["yellow"] * 3 + ["blue"] * 3 + ["coral"] * 3 + ["lavender"] * 3
    )
    rng.shuffle(butterfly_quads)

    butterfly_groups = []        # list of orbit-parent group dicts (children of root)
    butterfly_meta = []          # list of dicts holding per-butterfly anim info

    for i in range(12):
        quad = butterfly_quads[i]
        # Distribute angle around the caster — golden ratio offset
        ang = (i / 12) * 2 * math.pi + (i * 0.1)
        radius = 5.0 + (i % 5) * 0.85         # 5..9
        height = 2.0 + (i * 0.7) % 8.5        # 2..10

        bx = math.cos(ang) * radius
        bz = math.sin(ang) * radius
        by = height

        # ---- Butterfly local geometry (in butterfly-local space) ----
        # Body: thin long cube along Z axis (head to tail)
        body_sx = 0.18
        body_sy = 0.18
        body_sz = 1.4
        body_face = quad_uv(quad)
        body_uuid = b.add_cube(
            f"bf{i}_body",
            from_=[-body_sx / 2, -body_sy / 2, -body_sz / 2],
            to_=[body_sx / 2, body_sy / 2, body_sz / 2],
            faces=body_face,
            origin=[0, 0, 0],
            rotation=[0, 0, 0],
        )

        # Upper LEFT wing (bone hinges at body left-side near front)
        # Two cubes: large round (closer to body) + smaller (tip)
        ul_face = quad_uv(quad)
        ul_big = b.add_cube(
            f"bf{i}_UL_big",
            from_=[-1.6, -0.04, -0.7],
            to_=[-0.05, 0.04, 0.6],
            faces=ul_face,
            origin=[-0.05, 0, 0],
            rotation=[0, 0, 0],
        )
        ul_small = b.add_cube(
            f"bf{i}_UL_small",
            from_=[-2.0, -0.035, -0.35],
            to_=[-1.4, 0.035, 0.25],
            faces=quad_uv(quad),
            origin=[-1.4, 0, 0],
            rotation=[0, 0, 0],
        )
        ul_bone_uuid = make_uuid()
        ul_bone = b.make_group(
            f"bf{i}_UL_bone",
            origin=[0, 0, 0.1],   # hinge near body, front of body
            children=[ul_big, ul_small],
            rotation=[0, 0, 0],
            group_uuid=ul_bone_uuid,
        )

        # Upper RIGHT wing (mirror)
        ur_big = b.add_cube(
            f"bf{i}_UR_big",
            from_=[0.05, -0.04, -0.7],
            to_=[1.6, 0.04, 0.6],
            faces=quad_uv(quad),
            origin=[0.05, 0, 0],
            rotation=[0, 0, 0],
        )
        ur_small = b.add_cube(
            f"bf{i}_UR_small",
            from_=[1.4, -0.035, -0.35],
            to_=[2.0, 0.035, 0.25],
            faces=quad_uv(quad),
            origin=[1.4, 0, 0],
            rotation=[0, 0, 0],
        )
        ur_bone_uuid = make_uuid()
        ur_bone = b.make_group(
            f"bf{i}_UR_bone",
            origin=[0, 0, 0.1],
            children=[ur_big, ur_small],
            rotation=[0, 0, 0],
            group_uuid=ur_bone_uuid,
        )

        # Lower LEFT wing (single smaller flat slab)
        ll_cube = b.add_cube(
            f"bf{i}_LL",
            from_=[-1.3, -0.035, -0.5],
            to_=[-0.05, 0.035, 0.45],
            faces=quad_uv(quad),
            origin=[-0.05, 0, 0],
            rotation=[0, 0, 0],
        )
        ll_bone_uuid = make_uuid()
        ll_bone = b.make_group(
            f"bf{i}_LL_bone",
            origin=[0, 0, -0.4],   # hinge near body, rear of body
            children=[ll_cube],
            rotation=[0, 0, 0],
            group_uuid=ll_bone_uuid,
        )

        # Lower RIGHT wing (mirror)
        lr_cube = b.add_cube(
            f"bf{i}_LR",
            from_=[0.05, -0.035, -0.5],
            to_=[1.3, 0.035, 0.45],
            faces=quad_uv(quad),
            origin=[0.05, 0, 0],
            rotation=[0, 0, 0],
        )
        lr_bone_uuid = make_uuid()
        lr_bone = b.make_group(
            f"bf{i}_LR_bone",
            origin=[0, 0, -0.4],
            children=[lr_cube],
            rotation=[0, 0, 0],
            group_uuid=lr_bone_uuid,
        )

        # Butterfly body group (collects body + wing bones), rotates with travel direction
        bf_body_group_uuid = make_uuid()
        bf_body_group = b.make_group(
            f"bf{i}_body_grp",
            origin=[0, 0, 0],
            children=[body_uuid, ul_bone, ur_bone, ll_bone, lr_bone],
            rotation=[0, math.degrees(ang) + 90, 0],   # face along orbit direction
            group_uuid=bf_body_group_uuid,
        )

        # Butterfly orbit-parent bone (positioned in world coords; this is what orbits)
        bf_orbit_uuid = make_uuid()
        bf_orbit = b.make_group(
            f"bf{i}_orbit",
            origin=[bx, by, bz],
            children=[bf_body_group],
            rotation=[0, 0, 0],
            group_uuid=bf_orbit_uuid,
        )

        butterfly_groups.append(bf_orbit)
        butterfly_meta.append({
            "i": i,
            "quad": quad,
            "ang0": ang,
            "radius": radius,
            "height": by,
            "pos": (bx, by, bz),
            "orbit_uuid": bf_orbit_uuid,
            "orbit_name": f"bf{i}_orbit",
            "body_grp_uuid": bf_body_group_uuid,
            "body_grp_name": f"bf{i}_body_grp",
            "ul_uuid": ul_bone_uuid, "ul_name": f"bf{i}_UL_bone",
            "ur_uuid": ur_bone_uuid, "ur_name": f"bf{i}_UR_bone",
            "ll_uuid": ll_bone_uuid, "ll_name": f"bf{i}_LL_bone",
            "lr_uuid": lr_bone_uuid, "lr_name": f"bf{i}_LR_bone",
            # Per-butterfly flap parameters — none in sync
            "flap_period": rng.uniform(0.32, 0.52),    # 0.4s ish
            "flap_phase": rng.uniform(0, 2 * math.pi),
            "flap_amp_upper": rng.uniform(20, 28),     # ~25 deg
            "flap_amp_lower": rng.uniform(15, 22),
            "lower_phase_lag": rng.uniform(0.05, 0.12),  # seconds
            # Orbit period
            "orbit_period": rng.uniform(3.0, 7.0),
            "orbit_dir": 1 if rng.random() > 0.5 else -1,
        })

    # ========== CENTRAL FLOWER (Y=0) ==========
    # 6 flat petal slabs around a central cube
    flower_children = []
    flower_petal_meta = []
    flower_centre = b.add_cube(
        "flower_centre",
        from_=[-0.4, 0.0, -0.4],
        to_=[0.4, 0.3, 0.4],
        faces=uniform_face(*QUAD_UVS["yellow"], tex_index=1) if False else uniform_face(0, 0, 64, 64, tex_index=1),
        origin=[0, 0.15, 0],
        rotation=[0, 0, 0],
    )
    flower_centre_bone_uuid = make_uuid()
    flower_centre_bone = b.make_group(
        "flower_centre_b",
        origin=[0, 0.15, 0],
        children=[flower_centre],
        rotation=[0, 0, 0],
        group_uuid=flower_centre_bone_uuid,
    )
    flower_children.append(flower_centre_bone)

    for p in range(6):
        ang = (p / 6) * 2 * math.pi
        # Flat petal slab — radial outward
        px = math.cos(ang) * 0.9
        pz = math.sin(ang) * 0.9
        py = 0.12
        rot_y = math.degrees(ang) + 90
        rot_z = 12  # slight tilt up
        sx, sy, sz = 1.4, 0.08, 0.9
        cube = b.add_cube(
            f"flower_petal_{p}",
            from_=[-sx / 2, -sy / 2, -sz / 2],
            to_=[sx / 2, sy / 2, sz / 2],
            faces=uniform_face(0, 0, 64, 64, tex_index=1),
            origin=[0, 0, 0],
            rotation=[0, 0, 0],
        )
        bone_uuid = make_uuid()
        bone = b.make_group(
            f"flower_petal_{p}_b",
            origin=[px, py, pz],
            children=[cube],
            rotation=[0, rot_y, rot_z],
            group_uuid=bone_uuid,
        )
        flower_children.append(bone)
        flower_petal_meta.append({
            "uuid": bone_uuid, "name": f"flower_petal_{p}_b",
            "pos": (px, py, pz),
            "ang": ang,
        })

    flower_group_uuid = make_uuid()
    flower_group = b.make_group(
        "central_flower",
        origin=[0, 0, 0],
        children=flower_children,
        rotation=[0, 0, 0],
        group_uuid=flower_group_uuid,
    )

    # ========== 8 SCATTER PETALS at varied heights ==========
    scatter_children = []
    scatter_meta = []
    for i in range(8):
        ang = (i / 8) * 2 * math.pi + 0.3
        r = rng.uniform(2.5, 6.5)
        px = math.cos(ang) * r
        pz = math.sin(ang) * r
        py = rng.uniform(0.5, 6.0)
        sx, sy, sz = 1.0, 0.07, 0.65
        cube = b.add_cube(
            f"scatter_{i}",
            from_=[-sx / 2, -sy / 2, -sz / 2],
            to_=[sx / 2, sy / 2, sz / 2],
            faces=uniform_face(0, 0, 64, 64, tex_index=1),
            origin=[0, 0, 0],
            rotation=[0, 0, 0],
        )
        rot_x = rng.uniform(-30, 30)
        rot_y = math.degrees(ang) + rng.uniform(-30, 30)
        rot_z = rng.uniform(-30, 30)
        bone_uuid = make_uuid()
        bone = b.make_group(
            f"scatter_{i}_b",
            origin=[px, py, pz],
            children=[cube],
            rotation=[rot_x, rot_y, rot_z],
            group_uuid=bone_uuid,
        )
        scatter_children.append(bone)
        scatter_meta.append({
            "uuid": bone_uuid, "name": f"scatter_{i}_b",
            "pos": (px, py, pz), "ang": ang,
        })

    scatter_group_uuid = make_uuid()
    scatter_root = b.make_group(
        "petal_scatter",
        origin=[0, 0, 0],
        children=scatter_children,
        group_uuid=scatter_group_uuid,
    )

    # ========== ROOT ==========
    b.add_root_group(
        "root",
        origin=[0, 0, 0],
        children=[flower_group, scatter_root, *butterfly_groups],
    )

    # ========================================================================
    # ANIMATIONS
    # ========================================================================

    # ---------- SPAWN (0.8s, once, override=true) ----------
    # Central flower blooms at 0.0s.
    # Each butterfly materialises one by one, drifting in from outer staging
    # position. Stagger 0.05s, starting at 0.1s.
    spawn_animators = {}

    # Flower group: scale pop
    spawn_animators[flower_group_uuid] = {
        "name": "central_flower",
        "type": "bone",
        "keyframes": [
            make_keyframe("scale", 0.0, 0.05, 0.05, 0.05),
            make_keyframe("scale", 0.18, 1.4, 1.4, 1.4),
            make_keyframe("scale", 0.35, 0.9, 1.05, 0.9),
            make_keyframe("scale", 0.55, 1.05, 0.95, 1.05),
            make_keyframe("scale", 0.8, 1.0, 1.0, 1.0),
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 0.8, 0, 60, 0),
        ],
    }
    spawn_animators[flower_centre_bone_uuid] = {
        "name": "flower_centre_b",
        "type": "bone",
        "keyframes": [
            make_keyframe("scale", 0.0, 0.0, 0.0, 0.0),
            make_keyframe("scale", 0.15, 1.3, 0.5, 1.3),
            make_keyframe("scale", 0.35, 1.0, 1.0, 1.0),
            make_keyframe("scale", 0.8, 1.0, 1.0, 1.0),
        ],
    }
    for fpm in flower_petal_meta:
        # Petals fold out of centre
        spawn_animators[fpm["uuid"]] = {
            "name": fpm["name"],
            "type": "bone",
            "keyframes": [
                make_keyframe("scale", 0.0, 0.0, 0.0, 0.0),
                make_keyframe("scale", 0.1, 0.0, 0.0, 0.0),
                make_keyframe("scale", 0.3, 1.2, 1.0, 1.2),
                make_keyframe("scale", 0.5, 1.0, 1.0, 1.0),
                make_keyframe("scale", 0.8, 1.0, 1.0, 1.0),
                make_keyframe("rotation", 0.0, -40, math.degrees(fpm["ang"]) + 90, 60),
                make_keyframe("rotation", 0.4, 0, math.degrees(fpm["ang"]) + 90, 12),
                make_keyframe("rotation", 0.8, 0, math.degrees(fpm["ang"]) + 90, 12),
            ],
        }

    # Scatter petals: drift in from outside
    for sm in scatter_meta:
        px, py, pz = sm["pos"]
        spawn_animators[sm["uuid"]] = {
            "name": sm["name"],
            "type": "bone",
            "keyframes": [
                make_keyframe("scale", 0.0, 0.0, 0.0, 0.0),
                make_keyframe("scale", 0.4, 0.0, 0.0, 0.0),
                make_keyframe("scale", 0.6, 1.2, 1.2, 1.2),
                make_keyframe("scale", 0.8, 1.0, 1.0, 1.0),
                make_keyframe("position", 0.0, px * 0.4, 1.5, pz * 0.4),
                make_keyframe("position", 0.5, px * 0.2, 0.5, pz * 0.2),
                make_keyframe("position", 0.8, 0, 0, 0),
                make_keyframe("rotation", 0.0, rng.uniform(-90, 90), rng.uniform(-180, 180), rng.uniform(-90, 90)),
                make_keyframe("rotation", 0.8, 0, 0, 0),
            ],
        }

    # Butterflies: stagger 0.05s starting at 0.1s. Each drifts in from outer staging.
    for bm in butterfly_meta:
        i = bm["i"]
        delay = 0.1 + i * 0.05  # 0.10 .. 0.65
        # Settle time: butterfly appears (scale 0->1) over ~0.15s starting at delay
        # Drifts inward from staging position (1.5x its rest radius outward, +random Y)
        bx, by, bz = bm["pos"]
        # Outward staging direction
        norm = math.sqrt(bx * bx + bz * bz)
        if norm < 0.01:
            sx_off, sz_off = 0, 1
        else:
            sx_off, sz_off = bx / norm, bz / norm
        stage_x = sx_off * 4.0 + rng.uniform(-1.0, 1.0)
        stage_y = rng.uniform(-1.5, 2.0)
        stage_z = sz_off * 4.0 + rng.uniform(-1.0, 1.0)

        # Build orbit-parent keyframes (position + scale)
        kfs_orbit = [
            make_keyframe("position", 0.0, stage_x, stage_y, stage_z),
            make_keyframe("position", delay, stage_x, stage_y, stage_z),
            make_keyframe("position", delay + 0.18, stage_x * 0.4, stage_y * 0.4, stage_z * 0.4),
            make_keyframe("position", min(0.8, delay + 0.3), 0, 0, 0),
            make_keyframe("position", 0.8, 0, 0, 0),
            make_keyframe("scale", 0.0, 0.0, 0.0, 0.0),
            make_keyframe("scale", delay, 0.0, 0.0, 0.0),
            make_keyframe("scale", delay + 0.1, 1.2, 1.2, 1.2),
            make_keyframe("scale", min(0.8, delay + 0.22), 1.0, 1.0, 1.0),
            make_keyframe("scale", 0.8, 1.0, 1.0, 1.0),
        ]
        spawn_animators[bm["orbit_uuid"]] = {
            "name": bm["orbit_name"], "type": "bone", "keyframes": kfs_orbit,
        }

        # Body group: rotate Y to face orbit direction (already baked, but spin a bit on entry)
        spawn_animators[bm["body_grp_uuid"]] = {
            "name": bm["body_grp_name"],
            "type": "bone",
            "keyframes": [
                make_keyframe("rotation", 0.0, rng.uniform(-30, 30), rng.uniform(-90, 90), rng.uniform(-30, 30)),
                make_keyframe("rotation", delay + 0.2, 0, 0, 0),
                make_keyframe("rotation", 0.8, 0, 0, 0),
            ],
        }

        # Wings already flapping as it materialises (a few flap cycles during spawn)
        flap_period = bm["flap_period"]
        amp_u = bm["flap_amp_upper"]
        amp_l = bm["flap_amp_lower"]
        phase = bm["flap_phase"]
        lag = bm["lower_phase_lag"]
        # 6 flap keyframes 0..0.8s
        kfs_ul = []
        kfs_ur = []
        kfs_ll = []
        kfs_lr = []
        n_kf = 7
        for k in range(n_kf):
            t = k * (0.8 / (n_kf - 1))
            ang_u = math.sin(2 * math.pi * t / flap_period + phase) * amp_u
            ang_l = math.sin(2 * math.pi * (t - lag) / flap_period + phase) * amp_l
            # Upper L: rotate around Z (flap up/down).  Up = positive Z for left wing.
            kfs_ul.append(make_keyframe("rotation", t, 0, 0, ang_u))
            kfs_ur.append(make_keyframe("rotation", t, 0, 0, -ang_u))
            kfs_ll.append(make_keyframe("rotation", t, 0, 0, ang_l))
            kfs_lr.append(make_keyframe("rotation", t, 0, 0, -ang_l))
        spawn_animators[bm["ul_uuid"]] = {"name": bm["ul_name"], "type": "bone", "keyframes": kfs_ul}
        spawn_animators[bm["ur_uuid"]] = {"name": bm["ur_name"], "type": "bone", "keyframes": kfs_ur}
        spawn_animators[bm["ll_uuid"]] = {"name": bm["ll_name"], "type": "bone", "keyframes": kfs_ll}
        spawn_animators[bm["lr_uuid"]] = {"name": bm["lr_name"], "type": "bone", "keyframes": kfs_lr}

    b.add_animation("spawn", length=0.8, animators=spawn_animators, loop="once", override=True)

    # ---------- IDLE (4.0s, loop, override=false) ----------
    # Each butterfly orbits on its parent bone; wings flap independently.
    idle_animators = {}

    # Central flower: gentle pulse + slow Y rotation
    idle_animators[flower_group_uuid] = {
        "name": "central_flower",
        "type": "bone",
        "keyframes": [
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
            make_keyframe("scale", 1.0, 1.06, 1.0, 1.06),
            make_keyframe("scale", 2.0, 1.0, 1.0, 1.0),
            make_keyframe("scale", 3.0, 1.06, 1.0, 1.06),
            make_keyframe("scale", 4.0, 1.0, 1.0, 1.0),
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 2.0, 0, 30, 0),
            make_keyframe("rotation", 4.0, 0, 60, 0),
        ],
    }

    # Flower petals: gentle wave
    for fpm in flower_petal_meta:
        a = math.degrees(fpm["ang"]) + 90
        idle_animators[fpm["uuid"]] = {
            "name": fpm["name"],
            "type": "bone",
            "keyframes": [
                make_keyframe("rotation", 0.0, 0, a, 12),
                make_keyframe("rotation", 1.0, 4, a, 16),
                make_keyframe("rotation", 2.0, -3, a, 8),
                make_keyframe("rotation", 3.0, 5, a, 14),
                make_keyframe("rotation", 4.0, 0, a, 12),
            ],
        }

    # Scatter petals: float up-down + spin slowly
    for sm in scatter_meta:
        amp = rng.uniform(0.15, 0.4)
        ph = rng.uniform(0, 2 * math.pi)
        spd_y = rng.uniform(60, 180) * (1 if rng.random() > 0.5 else -1)
        idle_animators[sm["uuid"]] = {
            "name": sm["name"],
            "type": "bone",
            "keyframes": [
                make_keyframe("position", 0.0, 0, 0, 0),
                make_keyframe("position", 1.0, math.sin(ph) * amp, math.cos(ph) * amp, math.cos(ph + 1) * amp * 0.8),
                make_keyframe("position", 2.0, math.sin(ph + 1.5) * amp, math.cos(ph + 1) * amp, math.cos(ph + 2) * amp * 0.8),
                make_keyframe("position", 3.0, math.sin(ph + 3) * amp, math.cos(ph + 2.5) * amp, math.cos(ph + 3.5) * amp * 0.8),
                make_keyframe("position", 4.0, 0, 0, 0),
                make_keyframe("rotation", 0.0, 0, 0, 0),
                make_keyframe("rotation", 1.0, 8, spd_y * 0.25, 6),
                make_keyframe("rotation", 2.0, -6, spd_y * 0.5, -4),
                make_keyframe("rotation", 3.0, 10, spd_y * 0.75, 8),
                make_keyframe("rotation", 4.0, 0, spd_y, 0),
            ],
        }

    # Butterflies: orbit on parent bone (rotate around Y in a circle of radius bm["radius"]).
    # We achieve circular orbit by combining position keyframes around the circle.
    # 4.0s loop; orbit_period varies 3..7s, so fractional orbit per loop: 4/period revolutions.
    for bm in butterfly_meta:
        bx, by, bz = bm["pos"]
        radius = bm["radius"]
        ang0 = bm["ang0"]
        period = bm["orbit_period"]
        direction = bm["orbit_dir"]
        # Number of keyframes around the full orbit — sample 8 points along the 4s duration
        n = 9
        kfs_pos = []
        # Compute angle at each sample time
        # Anchor: at t=0, butterfly is at its rest position (delta = 0)
        # Sample delta_x, delta_z = (cos(ang0 + omega*t) - cos(ang0)) * radius etc.
        omega = direction * 2 * math.pi / period
        # Y bobbing
        bob_amp = rng.uniform(0.15, 0.5)
        bob_phase = rng.uniform(0, 2 * math.pi)
        for k in range(n):
            t = k * (4.0 / (n - 1))
            ang_t = ang0 + omega * t
            dx = math.cos(ang_t) * radius - math.cos(ang0) * radius
            dz = math.sin(ang_t) * radius - math.sin(ang0) * radius
            dy = math.sin(2 * math.pi * t / 2.0 + bob_phase) * bob_amp
            kfs_pos.append(make_keyframe("position", t, dx, dy, dz))
        idle_animators[bm["orbit_uuid"]] = {
            "name": bm["orbit_name"], "type": "bone", "keyframes": kfs_pos,
        }

        # Body group: rotate around Y so butterfly faces travel direction.
        # delta yaw at time t = (omega * t) in degrees
        kfs_body = []
        for k in range(n):
            t = k * (4.0 / (n - 1))
            yaw_delta = math.degrees(omega) * t
            # Slight banking based on flap rhythm
            roll = math.sin(2 * math.pi * t / bm["flap_period"] + bm["flap_phase"]) * 4
            kfs_body.append(make_keyframe("rotation", t, 0, yaw_delta, roll))
        idle_animators[bm["body_grp_uuid"]] = {
            "name": bm["body_grp_name"], "type": "bone", "keyframes": kfs_body,
        }

        # Wings — independent flap. Sample over 4s with high resolution to keep
        # ~10 cycles visible. NO TWO BUTTERFLIES IN SYNC (per-butterfly period & phase).
        flap_period = bm["flap_period"]
        amp_u = bm["flap_amp_upper"]
        amp_l = bm["flap_amp_lower"]
        phase = bm["flap_phase"]
        lag = bm["lower_phase_lag"]
        # Sample at 2x flap rate. 4s / period gives ~10 cycles, 4 samples/cycle = ~40 keyframes.
        n_flap = max(20, int(4.0 / flap_period * 4))
        kfs_ul = []
        kfs_ur = []
        kfs_ll = []
        kfs_lr = []
        for k in range(n_flap + 1):
            t = k * (4.0 / n_flap)
            ang_u = math.sin(2 * math.pi * t / flap_period + phase) * amp_u
            ang_l = math.sin(2 * math.pi * (t - lag) / flap_period + phase) * amp_l
            kfs_ul.append(make_keyframe("rotation", t, 0, 0, ang_u))
            kfs_ur.append(make_keyframe("rotation", t, 0, 0, -ang_u))
            kfs_ll.append(make_keyframe("rotation", t, 0, 0, ang_l))
            kfs_lr.append(make_keyframe("rotation", t, 0, 0, -ang_l))
        idle_animators[bm["ul_uuid"]] = {"name": bm["ul_name"], "type": "bone", "keyframes": kfs_ul}
        idle_animators[bm["ur_uuid"]] = {"name": bm["ur_name"], "type": "bone", "keyframes": kfs_ur}
        idle_animators[bm["ll_uuid"]] = {"name": bm["ll_name"], "type": "bone", "keyframes": kfs_ll}
        idle_animators[bm["lr_uuid"]] = {"name": bm["lr_name"], "type": "bone", "keyframes": kfs_lr}

    b.add_animation("idle", length=4.0, animators=idle_animators, loop="loop", override=False)

    # ---------- DISSIPATE (0.6s, once, override=true) ----------
    # Butterflies stop orbiting and fly outward in random directions, wings still flapping,
    # scaling to 0. Central flower petals scatter. Petal pieces follow outward.
    diss_animators = {}

    # Flower group: shrink + spin
    diss_animators[flower_group_uuid] = {
        "name": "central_flower",
        "type": "bone",
        "keyframes": [
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
            make_keyframe("scale", 0.3, 0.6, 0.4, 0.6),
            make_keyframe("scale", 0.6, 0.0, 0.0, 0.0),
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 0.6, 0, 360, 0),
        ],
    }

    # Flower petals: scatter outward
    for fpm in flower_petal_meta:
        px, py, pz = fpm["pos"]
        diss_animators[fpm["uuid"]] = {
            "name": fpm["name"],
            "type": "bone",
            "keyframes": [
                make_keyframe("position", 0.0, 0, 0, 0),
                make_keyframe("position", 0.6, px * 4, 2.0, pz * 4),
                make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
                make_keyframe("scale", 0.4, 0.7, 0.7, 0.7),
                make_keyframe("scale", 0.6, 0.0, 0.0, 0.0),
                make_keyframe("rotation", 0.0, 0, math.degrees(fpm["ang"]) + 90, 12),
                make_keyframe("rotation", 0.6, rng.uniform(-180, 180), math.degrees(fpm["ang"]) + 90 + rng.uniform(-180, 180), rng.uniform(-180, 180)),
            ],
        }

    # Scatter petals: drift outward
    for sm in scatter_meta:
        px, py, pz = sm["pos"]
        norm = max(0.001, math.sqrt(px * px + pz * pz))
        dx, dz = px / norm, pz / norm
        diss_animators[sm["uuid"]] = {
            "name": sm["name"],
            "type": "bone",
            "keyframes": [
                make_keyframe("position", 0.0, 0, 0, 0),
                make_keyframe("position", 0.6, dx * 3.5, 2.5, dz * 3.5),
                make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
                make_keyframe("scale", 0.6, 0.0, 0.0, 0.0),
                make_keyframe("rotation", 0.0, 0, 0, 0),
                make_keyframe("rotation", 0.6, rng.uniform(-180, 180), rng.uniform(-360, 360), rng.uniform(-180, 180)),
            ],
        }

    # Butterflies: each flies outward in random direction (mostly outward + up)
    for bm in butterfly_meta:
        bx, by, bz = bm["pos"]
        norm = max(0.001, math.sqrt(bx * bx + bz * bz))
        # Outward unit vector with random scatter
        dx_unit = bx / norm + rng.uniform(-0.3, 0.3)
        dz_unit = bz / norm + rng.uniform(-0.3, 0.3)
        dy_unit = rng.uniform(0.4, 1.2)
        # Travel distance scaled by random
        dist = rng.uniform(4.0, 7.0)

        diss_animators[bm["orbit_uuid"]] = {
            "name": bm["orbit_name"],
            "type": "bone",
            "keyframes": [
                make_keyframe("position", 0.0, 0, 0, 0),
                make_keyframe("position", 0.3, dx_unit * dist * 0.4, dy_unit * dist * 0.4, dz_unit * dist * 0.4),
                make_keyframe("position", 0.6, dx_unit * dist, dy_unit * dist, dz_unit * dist),
                make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
                make_keyframe("scale", 0.4, 0.7, 0.7, 0.7),
                make_keyframe("scale", 0.6, 0.0, 0.0, 0.0),
            ],
        }
        # Body group spins
        diss_animators[bm["body_grp_uuid"]] = {
            "name": bm["body_grp_name"],
            "type": "bone",
            "keyframes": [
                make_keyframe("rotation", 0.0, 0, 0, 0),
                make_keyframe("rotation", 0.6, rng.uniform(-90, 90), rng.uniform(-360, 360), rng.uniform(-90, 90)),
            ],
        }

        # Wings: keep flapping while flying out
        flap_period = bm["flap_period"]
        amp_u = bm["flap_amp_upper"] * 1.3   # slightly stronger as they flee
        amp_l = bm["flap_amp_lower"] * 1.3
        phase = bm["flap_phase"]
        lag = bm["lower_phase_lag"]
        n_flap = max(8, int(0.6 / flap_period * 4))
        kfs_ul = []
        kfs_ur = []
        kfs_ll = []
        kfs_lr = []
        for k in range(n_flap + 1):
            t = k * (0.6 / n_flap)
            ang_u = math.sin(2 * math.pi * t / flap_period + phase) * amp_u
            ang_l = math.sin(2 * math.pi * (t - lag) / flap_period + phase) * amp_l
            kfs_ul.append(make_keyframe("rotation", t, 0, 0, ang_u))
            kfs_ur.append(make_keyframe("rotation", t, 0, 0, -ang_u))
            kfs_ll.append(make_keyframe("rotation", t, 0, 0, ang_l))
            kfs_lr.append(make_keyframe("rotation", t, 0, 0, -ang_l))
        diss_animators[bm["ul_uuid"]] = {"name": bm["ul_name"], "type": "bone", "keyframes": kfs_ul}
        diss_animators[bm["ur_uuid"]] = {"name": bm["ur_name"], "type": "bone", "keyframes": kfs_ur}
        diss_animators[bm["ll_uuid"]] = {"name": bm["ll_name"], "type": "bone", "keyframes": kfs_ll}
        diss_animators[bm["lr_uuid"]] = {"name": bm["lr_name"], "type": "bone", "keyframes": kfs_lr}

    b.add_animation("dissipate", length=0.6, animators=diss_animators, loop="once", override=True)

    b.write(OUT)
    return OUT


if __name__ == "__main__":
    path = build()
    import os
    print("WROTE", path, os.path.getsize(path) // 1024, "KB")
