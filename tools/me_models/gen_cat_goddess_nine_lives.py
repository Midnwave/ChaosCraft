#!/usr/bin/env python3
"""
Generate cat_goddess_nine_lives.bbmodel — divine cat-icon orbital halo.

Visual: 9 cat-ear silhouette icons orbiting at varying radii (6-9), each with
4-slab head disc, 2 triangular ears, 6-slab halo ring. Plus central nexus
(3-cube cluster at Y=8) and 9 orbital path slabs forming loose sphere.
Mint pastel cats with gold halos. Nine miniature deities orbiting caster.
"""
import sys, math, random
sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder, make_uuid, make_keyframe, basic_cube_faces, uniform_face,
    png_from_pixels, hex_to_rgba, gradient_radial, color_lerp, fill_rect,
    add_noise_overlay,
)

OUT = "D:/CC/ChaosCraft/src/main/resources/models/fluffy/me_attacks/cat_goddess_nine_lives.bbmodel"


# ---------- TEXTURES ----------
def build_cat_silhouette_texture():
    """
    Texture 0: CAT SILHOUETTE PASTEL.
    Mint green pastel — flat coloured glass disc feeling.
    Wide soft specular, emissive glow zone.
    """
    w, h = 64, 64
    base_mint = hex_to_rgba("#b8f8d0")
    shadow_mint = hex_to_rgba("#90d8b0")
    bright_high = hex_to_rgba("#e0fff0")
    specular = hex_to_rgba("#ffffff")
    emissive = hex_to_rgba("#d0ffe8")

    pixels = [base_mint] * (w * h)

    # Whole texture flat mint base
    for y in range(h):
        for x in range(w):
            pixels[y * w + x] = base_mint

    # Subtle vertical mint shading — slightly deeper at the bottom
    for y in range(h):
        t = y / (h - 1)
        if t < 0.4:
            row = color_lerp(bright_high, base_mint, t / 0.4)
        elif t < 0.85:
            row = color_lerp(base_mint, base_mint, (t - 0.4) / 0.45)
        else:
            row = color_lerp(base_mint, shadow_mint, (t - 0.85) / 0.15)
        for x in range(w):
            pixels[y * w + x] = row

    # Soft wide specular highlight (large oval near upper-left)
    cx, cy = 22, 20
    spec_radius_outer = 22
    spec_radius_inner = 6
    for y in range(h):
        for x in range(w):
            dx = x - cx
            dy = (y - cy) * 1.1
            d = math.sqrt(dx * dx + dy * dy)
            if d < spec_radius_outer:
                if d < spec_radius_inner:
                    blend = 0.55
                else:
                    blend = 0.55 * (1.0 - (d - spec_radius_inner) / (spec_radius_outer - spec_radius_inner))
                base = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(base, specular, blend)

    # Wider gentle emissive glow centre — flat mint glow
    for y in range(h):
        for x in range(w):
            dx = (x - 32) / 32
            dy = (y - 32) / 32
            d = math.sqrt(dx * dx + dy * dy)
            if d < 0.85:
                base = pixels[y * w + x]
                blend = (1.0 - d / 0.85) * 0.18
                pixels[y * w + x] = color_lerp(base, emissive, blend)

    # Brighter specular pinpoint
    for sx, sy in [(20, 18), (22, 17), (24, 19)]:
        if 0 <= sx < w and 0 <= sy < h:
            pixels[sy * w + sx] = specular

    # Soft inner glow ring — emissive
    for y in range(h):
        for x in range(w):
            dx = (x - 32)
            dy = (y - 32)
            d = math.sqrt(dx * dx + dy * dy)
            if 26 < d < 30:
                base = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(base, emissive, 0.25)

    # Subtle edge softening — fade slightly toward shadow at corners
    for y in range(h):
        for x in range(w):
            cxx, cyy = w / 2, h / 2
            dx = (x - cxx) / cxx
            dy = (y - cyy) / cyy
            d = math.sqrt(dx * dx + dy * dy)
            if d > 0.92:
                base = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(base, shadow_mint, min(1.0, (d - 0.92) * 4))

    # Light shimmer
    add_noise_overlay(pixels, w, h, shadow_mint, bright_high, density=0.012, seed=909)

    return png_from_pixels(pixels, w, h)


def build_halo_gold_texture():
    """
    Texture 1: HALO GOLD-WHITE.
    Bright #ffe870 emissive centre, fading to #c0a830 gold,
    then #604800 dark gold at edges.
    """
    w, h = 64, 64
    centre = hex_to_rgba("#ffe870")
    mid = hex_to_rgba("#c0a830")
    edge = hex_to_rgba("#604800")
    bright_white = hex_to_rgba("#fffce0")
    sparkle = hex_to_rgba("#ffffff")

    pixels = [edge] * (w * h)

    cx, cy = w / 2, h / 2
    max_d = math.sqrt(cx * cx + cy * cy)

    for y in range(h):
        for x in range(w):
            dx = x - cx
            dy = y - cy
            d = math.sqrt(dx * dx + dy * dy)
            t = d / max_d  # 0 = centre, 1 = corner
            if t < 0.18:
                # bright white-gold core
                local = t / 0.18
                col = color_lerp(bright_white, centre, local)
            elif t < 0.5:
                local = (t - 0.18) / 0.32
                col = color_lerp(centre, mid, local)
            else:
                local = (t - 0.5) / 0.5
                col = color_lerp(mid, edge, local)
            pixels[y * w + x] = col

    # Bright radial rays (cross + diagonals at low intensity)
    for r in range(0, 32):
        for ang_deg in [0, 45, 90, 135, 180, 225, 270, 315]:
            ang = math.radians(ang_deg)
            x = int(cx + math.cos(ang) * r)
            y = int(cy + math.sin(ang) * r)
            if 0 <= x < w and 0 <= y < h:
                base = pixels[y * w + x]
                fade = max(0, 1.0 - r / 32)
                pixels[y * w + x] = color_lerp(base, bright_white, fade * 0.45)

    # Inner halo highlight ring
    for y in range(h):
        for x in range(w):
            dx = x - cx
            dy = y - cy
            d = math.sqrt(dx * dx + dy * dy)
            if 4 < d < 8:
                base = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(base, bright_white, 0.55)

    # Sparkle glints
    for sx, sy in [(20, 22), (44, 18), (32, 12), (16, 36), (48, 42), (32, 52), (10, 32), (54, 32)]:
        if 0 <= sx < w and 0 <= sy < h:
            pixels[sy * w + sx] = sparkle
            for d2 in [(1, 0), (-1, 0), (0, 1), (0, -1)]:
                nx, ny = sx + d2[0], sy + d2[1]
                if 0 <= nx < w and 0 <= ny < h:
                    base = pixels[ny * w + nx]
                    pixels[ny * w + nx] = color_lerp(base, sparkle, 0.55)

    # Light noise gold shimmer
    add_noise_overlay(pixels, w, h, mid, bright_white, density=0.025, seed=314)

    return png_from_pixels(pixels, w, h)


# ---------- BUILD ----------
def build():
    b = Builder("cat_goddess_nine_lives", resolution=(64, 64))
    b.add_texture("cat_silhouette", build_cat_silhouette_texture())
    b.add_texture("halo_gold", build_halo_gold_texture())

    rng = random.Random(91919)

    # ========== CENTRAL ORBITAL NEXUS (3-cube cluster at Y=8) ==========
    nexus_children = []
    nexus_meta = []  # (bone_uuid, name)
    nexus_cube_offsets = [
        (0.0, 0.0, 0.0, 1.4, 1.4, 1.4),
        (0.0, 0.9, 0.0, 0.9, 0.9, 0.9),
        (0.0, -0.7, 0.0, 1.1, 0.5, 1.1),
    ]
    for ci, (ox, oy, oz, sx, sy, sz) in enumerate(nexus_cube_offsets):
        f_uv = uniform_face(0, 0, 64, 64, tex_index=1)
        cube_uuid = b.add_cube(
            f"nexus_core_{ci}",
            from_=[ox - sx / 2, oy - sy / 2, oz - sz / 2],
            to_=[ox + sx / 2, oy + sy / 2, oz + sz / 2],
            faces=f_uv,
            origin=[ox, oy, oz],
            rotation=[ci * 23, ci * 41, ci * 17],
        )
        nexus_children.append(cube_uuid)

    nexus_bone_uuid = make_uuid()
    nexus_bone = b.make_group(
        "orbital_nexus",
        origin=[0, 8, 0],
        children=nexus_children,
        group_uuid=nexus_bone_uuid,
    )
    nexus_meta.append((nexus_bone_uuid, "orbital_nexus"))

    # ========== 9 CAT ICONS ==========
    cat_orbit_bones = []  # outer parent bones (for orbit rotation)
    cat_orbit_meta = []  # (orbit_bone_uuid, name, radius, base_angle, orbit_period)
    cat_inner_meta = []  # (inner_bone_uuid, name) — for player tracking spin
    cat_halo_meta = []  # (halo_group_uuid, name) — for halo rotation
    cat_halo_slab_meta = []  # individual halo slabs

    # Varying radii 6-9
    cat_radii = [6.0, 6.8, 7.5, 8.2, 9.0, 7.0, 6.4, 8.6, 7.8]
    cat_heights = [0.5, 1.8, -1.2, 2.5, -0.5, 3.2, -2.0, 1.0, -2.5]
    cat_orbit_periods = [5.0, 6.0, 7.0, 8.0, 9.0, 5.5, 6.5, 7.5, 8.5]

    for i in range(9):
        base_angle = (i / 9) * 2 * math.pi
        radius = cat_radii[i]
        height = cat_heights[i]
        orbit_period = cat_orbit_periods[i]

        cx_pos = math.cos(base_angle) * radius
        cz_pos = math.sin(base_angle) * radius
        cy_pos = height

        # ---- Cat icon assembly (in local coordinates around origin 0,0,0) ----
        # HEAD DISC: 4 flat slabs forming round head outline
        head_cubes = []
        head_radius = 0.7
        head_slab_thickness = 0.12
        for hi in range(4):
            ang = (hi / 4) * 2 * math.pi + math.pi / 4
            hx = math.cos(ang) * head_radius * 0.55
            hz = math.sin(ang) * head_radius * 0.55
            sx = 0.65
            sy = head_slab_thickness
            sz = 0.65
            f_uv = uniform_face(0, 0, 64, 64, tex_index=0)
            head_cube = b.add_cube(
                f"cat{i}_head_{hi}",
                from_=[hx - sx / 2, -sy / 2, hz - sz / 2],
                to_=[hx + sx / 2, sy / 2, hz + sz / 2],
                faces=f_uv,
                origin=[hx, 0, hz],
                rotation=[0, math.degrees(ang), 0],
            )
            head_cubes.append(head_cube)

        # LEFT EAR: 2 stacked triangular slabs (wider base, narrower top)
        ear_left_children = []
        # base slab (wider)
        sxL1, syL1, szL1 = 0.55, 0.1, 0.08
        f_uv = uniform_face(0, 0, 64, 64, tex_index=0)
        ear_l_base = b.add_cube(
            f"cat{i}_earL_base",
            from_=[-sxL1 / 2, -syL1 / 2, -szL1 / 2],
            to_=[sxL1 / 2, syL1 / 2, szL1 / 2],
            faces=f_uv,
            origin=[0, 0, 0],
            rotation=[0, 0, 0],
        )
        ear_left_children.append(ear_l_base)
        # top slab (narrower, stacked above)
        sxL2, syL2, szL2 = 0.3, 0.08, 0.07
        ear_l_top = b.add_cube(
            f"cat{i}_earL_top",
            from_=[-sxL2 / 2, syL1 / 2 + 0.02, -szL2 / 2],
            to_=[sxL2 / 2, syL1 / 2 + 0.02 + syL2, szL2 / 2],
            faces=f_uv,
            origin=[0, syL1 / 2 + syL2 / 2 + 0.02, 0],
            rotation=[0, 0, 0],
        )
        ear_left_children.append(ear_l_top)
        ear_left_bone_uuid = make_uuid()
        ear_left_bone = b.make_group(
            f"cat{i}_ear_left",
            origin=[-0.45, 0.55, 0],
            children=ear_left_children,
            rotation=[0, 0, -18],
            group_uuid=ear_left_bone_uuid,
        )

        # RIGHT EAR: mirror of left
        ear_right_children = []
        ear_r_base = b.add_cube(
            f"cat{i}_earR_base",
            from_=[-sxL1 / 2, -syL1 / 2, -szL1 / 2],
            to_=[sxL1 / 2, syL1 / 2, szL1 / 2],
            faces=f_uv,
            origin=[0, 0, 0],
            rotation=[0, 0, 0],
        )
        ear_right_children.append(ear_r_base)
        ear_r_top = b.add_cube(
            f"cat{i}_earR_top",
            from_=[-sxL2 / 2, syL1 / 2 + 0.02, -szL2 / 2],
            to_=[sxL2 / 2, syL1 / 2 + 0.02 + syL2, szL2 / 2],
            faces=f_uv,
            origin=[0, syL1 / 2 + syL2 / 2 + 0.02, 0],
            rotation=[0, 0, 0],
        )
        ear_right_children.append(ear_r_top)
        ear_right_bone_uuid = make_uuid()
        ear_right_bone = b.make_group(
            f"cat{i}_ear_right",
            origin=[0.45, 0.55, 0],
            children=ear_right_children,
            rotation=[0, 0, 18],
            group_uuid=ear_right_bone_uuid,
        )

        # HALO RING: 6 flat slabs in small ring at Y+0.5 above head
        halo_children = []
        halo_individual_meta = []
        halo_radius = 0.55
        for ji in range(6):
            ang = (ji / 6) * 2 * math.pi
            hx2 = math.cos(ang) * halo_radius
            hz2 = math.sin(ang) * halo_radius
            sxH, syH, szH = 0.32, 0.08, 0.16
            f_uv_halo = uniform_face(0, 0, 64, 64, tex_index=1)
            halo_cube = b.add_cube(
                f"cat{i}_halo_{ji}",
                from_=[-sxH / 2, -syH / 2, -szH / 2],
                to_=[sxH / 2, syH / 2, szH / 2],
                faces=f_uv_halo,
                origin=[0, 0, 0],
                rotation=[0, 0, 0],
            )
            halo_slab_uuid = make_uuid()
            halo_slab_bone = b.make_group(
                f"cat{i}_halo_slab_{ji}_b",
                origin=[hx2, 1.4, hz2],
                children=[halo_cube],
                rotation=[0, math.degrees(ang) + 90, 0],
                group_uuid=halo_slab_uuid,
            )
            halo_children.append(halo_slab_bone)
            halo_individual_meta.append((halo_slab_uuid, f"cat{i}_halo_slab_{ji}_b"))

        halo_group_uuid = make_uuid()
        halo_group = b.make_group(
            f"cat{i}_halo",
            origin=[0, 1.4, 0],
            children=halo_children,
            group_uuid=halo_group_uuid,
        )
        cat_halo_meta.append((halo_group_uuid, f"cat{i}_halo"))
        cat_halo_slab_meta.extend(halo_individual_meta)

        # Inner cat bone (head disc + ears + halo) — Y-tracking for player gaze
        inner_children = list(head_cubes) + [ear_left_bone, ear_right_bone, halo_group]
        cat_inner_uuid = make_uuid()
        cat_inner = b.make_group(
            f"cat{i}_inner",
            origin=[0, 0, 0],
            children=inner_children,
            rotation=[0, 0, 0],
            group_uuid=cat_inner_uuid,
        )
        cat_inner_meta.append((cat_inner_uuid, f"cat{i}_inner"))

        # Outer orbit bone — placed at orbit position, will rotate to orbit
        # We model orbit by rotating an orbit-parent bone whose origin is centre,
        # and the cat-position bone offset from it. Simpler: position the bone
        # at orbit position, and animate rotation around centre via separate
        # parent. Use the position-based approach: parent rotation acts as orbit.
        cat_position_uuid = make_uuid()
        cat_position = b.make_group(
            f"cat{i}_pos",
            origin=[cx_pos, cy_pos, cz_pos],
            children=[cat_inner],
            rotation=[0, 0, 0],
            group_uuid=cat_position_uuid,
        )
        cat_orbit_meta.append((cat_position_uuid, f"cat{i}_pos", radius, base_angle, orbit_period, height))
        cat_orbit_bones.append(cat_position)

    # Orbit container — for global rotation if needed
    cat_orbit_container_uuid = make_uuid()
    cat_orbit_container = b.make_group(
        "cat_orbiters",
        origin=[0, 4, 0],
        children=cat_orbit_bones,
        group_uuid=cat_orbit_container_uuid,
    )

    # ========== ORBITAL TRAIL (9 thin path slabs forming loose sphere) ==========
    trail_bones = []
    trail_meta = []
    for ti in range(9):
        # Distribute on sphere (golden ratio spiral)
        phi = math.acos(1 - 2 * (ti + 0.5) / 9)
        theta = math.pi * (1 + 5 ** 0.5) * (ti + 0.5)
        radius = 7.5
        tx = radius * math.sin(phi) * math.cos(theta)
        ty = 4.0 + radius * math.cos(phi) * 0.6
        tz = radius * math.sin(phi) * math.sin(theta)

        # Path-indicator slab: thin elongated arc-piece
        sx_t, sy_t, sz_t = 1.6, 0.05, 0.18
        f_uv = uniform_face(0, 0, 64, 64, tex_index=1)
        trail_cube = b.add_cube(
            f"trail_{ti}",
            from_=[-sx_t / 2, -sy_t / 2, -sz_t / 2],
            to_=[sx_t / 2, sy_t / 2, sz_t / 2],
            faces=f_uv,
            origin=[0, 0, 0],
            rotation=[0, 0, 0],
        )
        # Tangent rotation — face orbital direction
        rot_y_t = math.degrees(theta) + 90
        rot_x_t = math.degrees(phi) - 90
        trail_uuid = make_uuid()
        trail_bone = b.make_group(
            f"trail_{ti}_b",
            origin=[tx, ty, tz],
            children=[trail_cube],
            rotation=[rot_x_t, rot_y_t, 0],
            group_uuid=trail_uuid,
        )
        trail_bones.append(trail_bone)
        trail_meta.append((trail_uuid, f"trail_{ti}_b", (tx, ty, tz)))

    trail_container_uuid = make_uuid()
    trail_container = b.make_group(
        "orbital_trails",
        origin=[0, 4, 0],
        children=trail_bones,
        group_uuid=trail_container_uuid,
    )

    # ========== ROOT ==========
    b.add_root_group(
        "root",
        origin=[0, 0, 0],
        children=[nexus_bone, cat_orbit_container, trail_container],
    )

    # ========== ANIMATIONS ==========
    # ---- SPAWN (1.0s, once, override=true) ----
    spawn_animators = {}

    # Nexus: appears at 0.0s scale 0 -> 1.2 -> 1.0
    spawn_animators[nexus_bone_uuid] = {
        "name": "orbital_nexus",
        "type": "bone",
        "keyframes": [
            make_keyframe("scale", 0.0, 0.0, 0.0, 0.0),
            make_keyframe("scale", 0.15, 1.3, 1.3, 1.3),
            make_keyframe("scale", 0.3, 1.0, 1.0, 1.0),
            make_keyframe("scale", 1.0, 1.0, 1.0, 1.0),
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 1.0, 0, 360, 0),
        ],
    }

    # Cat icons: fly in from outer space one by one with 0.1s stagger
    for idx, (pos_uuid, name, radius, base_angle, period, height) in enumerate(cat_orbit_meta):
        arrival_t = 0.05 + idx * 0.08  # 0.1s stagger (compressed slightly to fit 1.0s)
        bounce_t = arrival_t + 0.1
        settle_t = arrival_t + 0.22
        # Start position: 2x outward from rest pos
        cx_pos = math.cos(base_angle) * radius
        cz_pos = math.sin(base_angle) * radius
        start_offset_x = math.cos(base_angle) * radius * 1.2
        start_offset_z = math.sin(base_angle) * radius * 1.2
        start_offset_y = 4.0  # high above

        kfs = []
        # Hidden until arrival
        kfs.append(make_keyframe("scale", 0.0, 0.0, 0.0, 0.0))
        kfs.append(make_keyframe("scale", arrival_t - 0.001, 0.0, 0.0, 0.0))
        # Bounce in
        kfs.append(make_keyframe("scale", arrival_t, 0.05, 0.05, 0.05))
        kfs.append(make_keyframe("scale", bounce_t, 1.2, 1.2, 1.2))
        kfs.append(make_keyframe("scale", settle_t, 1.0, 1.0, 1.0))
        kfs.append(make_keyframe("scale", 1.0, 1.0, 1.0, 1.0))
        # Position fly-in
        kfs.append(make_keyframe("position", 0.0, start_offset_x, start_offset_y, start_offset_z))
        kfs.append(make_keyframe("position", arrival_t - 0.001, start_offset_x, start_offset_y, start_offset_z))
        kfs.append(make_keyframe("position", arrival_t, start_offset_x * 0.6, start_offset_y * 0.6, start_offset_z * 0.6))
        kfs.append(make_keyframe("position", settle_t, 0, 0, 0))
        kfs.append(make_keyframe("position", 1.0, 0, 0, 0))
        spawn_animators[pos_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # Cat inner — slight Y tracking spin during spawn
    for inner_uuid, name in cat_inner_meta:
        spawn_animators[inner_uuid] = {
            "name": name,
            "type": "bone",
            "keyframes": [
                make_keyframe("rotation", 0.0, 0, 0, 0),
                make_keyframe("rotation", 1.0, 0, 90, 0),
            ],
        }

    # Halos materialise at icon arrival + 0.1s (scale up from 0)
    for halo_idx, (halo_uuid, name) in enumerate(cat_halo_meta):
        arrival_t = 0.05 + halo_idx * 0.08 + 0.1
        if arrival_t >= 0.99:
            arrival_t = 0.95
        kfs = [
            make_keyframe("scale", 0.0, 0.0, 0.0, 0.0),
            make_keyframe("scale", arrival_t - 0.001, 0.0, 0.0, 0.0),
            make_keyframe("scale", arrival_t, 0.2, 0.2, 0.2),
            make_keyframe("scale", min(arrival_t + 0.1, 1.0), 1.3, 1.3, 1.3),
            make_keyframe("scale", min(arrival_t + 0.2, 1.0), 1.0, 1.0, 1.0),
            make_keyframe("scale", 1.0, 1.0, 1.0, 1.0),
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 1.0, 0, 180, 0),
        ]
        spawn_animators[halo_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # Orbital trails appear at 0.5s
    for tr_uuid, name, (tx, ty, tz) in trail_meta:
        spawn_animators[tr_uuid] = {
            "name": name,
            "type": "bone",
            "keyframes": [
                make_keyframe("scale", 0.0, 0.0, 0.0, 0.0),
                make_keyframe("scale", 0.499, 0.0, 0.0, 0.0),
                make_keyframe("scale", 0.5, 0.1, 0.1, 0.1),
                make_keyframe("scale", 0.7, 1.2, 1.2, 1.2),
                make_keyframe("scale", 0.85, 1.0, 1.0, 1.0),
                make_keyframe("scale", 1.0, 1.0, 1.0, 1.0),
                make_keyframe("rotation", 0.0, 0, 0, 0),
                make_keyframe("rotation", 1.0, rng.uniform(-30, 30), rng.uniform(-90, 90), rng.uniform(-30, 30)),
            ],
        }

    # Trail container slow rotate during spawn
    spawn_animators[trail_container_uuid] = {
        "name": "orbital_trails",
        "type": "bone",
        "keyframes": [
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 1.0, 0, 60, 0),
        ],
    }

    b.add_animation("spawn", length=1.0, animators=spawn_animators, loop="once", override=True)

    # ---- IDLE (7.0s, loop, override=false) ----
    idle_animators = {}

    # Nexus: slow spin + soft pulse
    idle_animators[nexus_bone_uuid] = {
        "name": "orbital_nexus",
        "type": "bone",
        "keyframes": [
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 1.75, 30, 90, -20),
            make_keyframe("rotation", 3.5, -20, 180, 30),
            make_keyframe("rotation", 5.25, 25, 270, -15),
            make_keyframe("rotation", 7.0, 0, 360, 0),
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
            make_keyframe("scale", 1.75, 1.12, 1.05, 1.12),
            make_keyframe("scale", 3.5, 1.0, 1.0, 1.0),
            make_keyframe("scale", 5.25, 1.08, 1.03, 1.08),
            make_keyframe("scale", 7.0, 1.0, 1.0, 1.0),
        ],
    }

    # Cat icons orbit — different speeds (5s-9s per orbit)
    # We rotate position around origin: parameterised position keyframes
    for idx, (pos_uuid, name, radius, base_angle, period, height) in enumerate(cat_orbit_meta):
        # Number of full orbits in 7s
        orbits = 7.0 / period
        # 6 keyframes for smooth circular motion (rotating linearly between them)
        n_steps = 8
        kfs_pos = []
        for s in range(n_steps + 1):
            t_norm = s / n_steps
            t = t_norm * 7.0
            ang = base_angle + 2 * math.pi * orbits * t_norm
            x_at = math.cos(ang) * radius
            z_at = math.sin(ang) * radius
            # Rest position is (cos(base)*r, height, sin(base)*r)
            # Animator delta from rest:
            rest_x = math.cos(base_angle) * radius
            rest_z = math.sin(base_angle) * radius
            dx = x_at - rest_x
            dz = z_at - rest_z
            # Slight Y wobble
            dy = math.sin(t * 0.9 + idx) * 0.4
            kfs_pos.append(make_keyframe("position", t, dx, dy, dz))

        idle_animators[pos_uuid] = {"name": name, "type": "bone", "keyframes": kfs_pos}

    # Cat inner — slowly rotates on Y axis to "track player" (continuous turn)
    for inner_idx, (inner_uuid, name) in enumerate(cat_inner_meta):
        period = cat_orbit_periods[inner_idx]
        # Track simulated as smooth Y rotation matching orbit (counter-rotate so cat faces inward)
        orbits = 7.0 / period
        kfs_track = []
        n_steps = 6
        for s in range(n_steps + 1):
            t_norm = s / n_steps
            t = t_norm * 7.0
            # Counter-rotate: cat faces inward as it orbits
            y_rot = -360.0 * orbits * t_norm
            # subtle bob rotation
            x_rot = math.sin(t * 1.2 + inner_idx * 0.7) * 8
            z_rot = math.cos(t * 0.9 + inner_idx) * 6
            kfs_track.append(make_keyframe("rotation", t, x_rot, y_rot, z_rot))
        idle_animators[inner_uuid] = {"name": name, "type": "bone", "keyframes": kfs_track}

    # Halo rings slowly rotate
    for hi, (halo_uuid, name) in enumerate(cat_halo_meta):
        idle_animators[halo_uuid] = {
            "name": name,
            "type": "bone",
            "keyframes": [
                make_keyframe("rotation", 0.0, 0, 0, 0),
                make_keyframe("rotation", 1.75, 0, 90, 0),
                make_keyframe("rotation", 3.5, 0, 180, 0),
                make_keyframe("rotation", 5.25, 0, 270, 0),
                make_keyframe("rotation", 7.0, 0, 360, 0),
                make_keyframe("position", 0.0, 0, 0, 0),
                make_keyframe("position", 3.5, 0, 0.1, 0),
                make_keyframe("position", 7.0, 0, 0, 0),
            ],
        }

    # Halo individual slabs — slight wobble
    for hsi, (slab_uuid, slab_name) in enumerate(cat_halo_slab_meta):
        idle_animators[slab_uuid] = {
            "name": slab_name,
            "type": "bone",
            "keyframes": [
                make_keyframe("rotation", 0.0, 0, 0, 0),
                make_keyframe("rotation", 2.33, rng.uniform(-6, 6), 0, rng.uniform(-6, 6)),
                make_keyframe("rotation", 4.66, rng.uniform(-6, 6), 0, rng.uniform(-6, 6)),
                make_keyframe("rotation", 7.0, 0, 0, 0),
            ],
        }

    # Orbital trails follow / drift
    for tr_uuid, name, (tx, ty, tz) in trail_meta:
        idle_animators[tr_uuid] = {
            "name": name,
            "type": "bone",
            "keyframes": [
                make_keyframe("rotation", 0.0, 0, 0, 0),
                make_keyframe("rotation", 2.33, rng.uniform(-15, 15), 60, rng.uniform(-15, 15)),
                make_keyframe("rotation", 4.66, rng.uniform(-15, 15), 120, rng.uniform(-15, 15)),
                make_keyframe("rotation", 7.0, 0, 180, 0),
                make_keyframe("position", 0.0, 0, 0, 0),
                make_keyframe("position", 3.5, rng.uniform(-0.2, 0.2), rng.uniform(-0.3, 0.3), rng.uniform(-0.2, 0.2)),
                make_keyframe("position", 7.0, 0, 0, 0),
            ],
        }

    # Trail container slow orbit
    idle_animators[trail_container_uuid] = {
        "name": "orbital_trails",
        "type": "bone",
        "keyframes": [
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 3.5, 0, 180, 0),
            make_keyframe("rotation", 7.0, 0, 360, 0),
        ],
    }

    b.add_animation("idle", length=7.0, animators=idle_animators, loop="loop", override=False)

    # ---- DISSIPATE (0.9s, once, override=true) ----
    diss_animators = {}

    # Nexus: implodes
    diss_animators[nexus_bone_uuid] = {
        "name": "orbital_nexus",
        "type": "bone",
        "keyframes": [
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
            make_keyframe("scale", 0.3, 1.4, 1.4, 1.4),
            make_keyframe("scale", 0.6, 0.3, 0.3, 0.3),
            make_keyframe("scale", 0.9, 0.0, 0.0, 0.0),
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 0.9, 0, 720, 0),
        ],
    }

    # Cat icons continue orbit tangent outward
    for idx, (pos_uuid, name, radius, base_angle, period, height) in enumerate(cat_orbit_meta):
        rest_x = math.cos(base_angle) * radius
        rest_z = math.sin(base_angle) * radius
        # Tangent direction (perpendicular to radial, in XZ plane)
        tangent_x = -math.sin(base_angle)
        tangent_z = math.cos(base_angle)
        # Outward tangent fly: travel in tangent + outward direction
        radial_x = math.cos(base_angle)
        radial_z = math.sin(base_angle)
        # Combined: tangent + slight outward
        fly_x = (tangent_x * 5.0 + radial_x * 3.0)
        fly_z = (tangent_z * 5.0 + radial_z * 3.0)
        kfs = [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.4, fly_x * 0.4, 0.5, fly_z * 0.4),
            make_keyframe("position", 0.9, fly_x, 1.5, fly_z),
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
            make_keyframe("scale", 0.5, 0.85, 0.85, 0.85),
            make_keyframe("scale", 0.9, 0.0, 0.0, 0.0),
        ]
        diss_animators[pos_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # Cat inner — spin during dissipate
    for idx, (inner_uuid, name) in enumerate(cat_inner_meta):
        diss_animators[inner_uuid] = {
            "name": name,
            "type": "bone",
            "keyframes": [
                make_keyframe("rotation", 0.0, 0, 0, 0),
                make_keyframe("rotation", 0.9, rng.uniform(-90, 90), rng.uniform(-540, 540), rng.uniform(-90, 90)),
            ],
        }

    # Halos fly off slightly before icon itself (start at 0.0, fly out earlier)
    for hi, (halo_uuid, name) in enumerate(cat_halo_meta):
        diss_animators[halo_uuid] = {
            "name": name,
            "type": "bone",
            "keyframes": [
                make_keyframe("position", 0.0, 0, 0, 0),
                make_keyframe("position", 0.15, 0, 1.0, 0),
                make_keyframe("position", 0.4, 0, 3.0, 0),
                make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
                make_keyframe("scale", 0.2, 1.4, 1.4, 1.4),
                make_keyframe("scale", 0.4, 0.0, 0.0, 0.0),
                make_keyframe("rotation", 0.0, 0, 0, 0),
                make_keyframe("rotation", 0.4, 0, 540, 0),
            ],
        }

    # Halo individual slabs — scatter slightly
    for hsi, (slab_uuid, slab_name) in enumerate(cat_halo_slab_meta):
        diss_animators[slab_uuid] = {
            "name": slab_name,
            "type": "bone",
            "keyframes": [
                make_keyframe("rotation", 0.0, 0, 0, 0),
                make_keyframe("rotation", 0.4, rng.uniform(-180, 180), rng.uniform(-360, 360), rng.uniform(-180, 180)),
                make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
                make_keyframe("scale", 0.4, 0.0, 0.0, 0.0),
            ],
        }

    # Orbital trails: expand and snap
    for tr_uuid, name, (tx, ty, tz) in trail_meta:
        norm = max(0.001, math.sqrt(tx * tx + (ty - 4) ** 2 + tz * tz))
        dirx = tx / norm
        diry = (ty - 4) / norm
        dirz = tz / norm
        kfs = [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.5, dirx * 1.5, diry * 1.5, dirz * 1.5),
            make_keyframe("position", 0.9, dirx * 4.5, diry * 4.5, dirz * 4.5),
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
            make_keyframe("scale", 0.4, 1.6, 1.6, 0.4),
            make_keyframe("scale", 0.7, 0.4, 0.4, 0.1),
            make_keyframe("scale", 0.9, 0.0, 0.0, 0.0),
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 0.9, rng.uniform(-180, 180), rng.uniform(-360, 360), rng.uniform(-180, 180)),
        ]
        diss_animators[tr_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    b.add_animation("dissipate", length=0.9, animators=diss_animators, loop="once", override=True)

    b.write(OUT)
    return OUT


if __name__ == "__main__":
    path = build()
    import os
    print("WROTE", path, os.path.getsize(path) // 1024, "KB")
