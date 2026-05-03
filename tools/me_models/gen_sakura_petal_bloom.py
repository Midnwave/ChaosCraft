#!/usr/bin/env python3
"""
Generate sakura_petal_bloom.bbmodel — a cherry blossom petal storm burst.

Visual: 24 floating petals + 3 burst rings (8 petals each) + 5-slab central bloom
+ 8 ground scatter petals. Pure cherry blossom pink, weightless, beautiful AOE.
"""
import sys, math, random
sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder, make_uuid, make_keyframe, basic_cube_faces, uniform_face,
    png_from_pixels, hex_to_rgba, gradient_radial, gradient_horizontal_band,
    add_noise_overlay, color_lerp, fill_rect,
)

OUT = "D:/CC/ChaosCraft/src/main/resources/models/fluffy/me_attacks/sakura_petal_bloom.bbmodel"


# ---------- TEXTURES ----------
def build_petal_texture():
    """Texture 0: SAKURA PETAL — pale pink with veins, bright tip, emissive zone."""
    w, h = 64, 64
    # Base pale cherry blossom pink
    base_pale = hex_to_rgba("#fce8ee")
    vein_pink = hex_to_rgba("#f0c0d0")
    deep_base = hex_to_rgba("#e898b0")
    bright_tip = hex_to_rgba("#fff0f8")
    specular = hex_to_rgba("#ffffff")
    emissive = hex_to_rgba("#fff8fc")

    pixels = [base_pale] * (w * h)

    # Vertical petal gradient: deeper at base (bottom of texture), brighter at tip (top)
    for y in range(h):
        t = y / (h - 1)  # 0 at top (tip), 1 at bottom (base)
        if t < 0.18:
            # tip zone — bright
            row_color = color_lerp(bright_tip, base_pale, t / 0.18)
        elif t < 0.7:
            # body — pale to slightly deeper
            local = (t - 0.18) / 0.52
            row_color = color_lerp(base_pale, vein_pink, local * 0.55)
        else:
            # base — deep
            local = (t - 0.7) / 0.3
            row_color = color_lerp(vein_pink, deep_base, local)
        for x in range(w):
            pixels[y * w + x] = row_color

    # Fine horizontal vein lines (petal venation)
    rng = random.Random(11)
    for vy in range(6, h - 4, 4):
        # main vein
        for x in range(2, w - 2):
            base = pixels[vy * w + x]
            shaded = color_lerp(base, deep_base, 0.35)
            pixels[vy * w + x] = shaded
        # subtle drift line below
        if vy + 1 < h:
            for x in range(3, w - 3):
                base = pixels[(vy + 1) * w + x]
                shaded = color_lerp(base, vein_pink, 0.2)
                pixels[(vy + 1) * w + x] = shaded

    # Subtle radial brightness near tip center
    for y in range(0, 22):
        for x in range(w):
            d = math.sqrt((x - w / 2) ** 2 + (y - 4) ** 2)
            if d < 14:
                base = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(base, bright_tip, max(0, 1.0 - d / 14) * 0.45)

    # Emissive 3px zone at brightest tip
    for y in range(2, 7):
        for x in range(28, 36):
            if abs(x - 32) + abs(y - 4) < 6:
                pixels[y * w + x] = emissive

    # Specular highlights (small white sparkle dots)
    for sx, sy in [(20, 14), (44, 18), (32, 10), (16, 28), (48, 32)]:
        if 0 <= sx < w and 0 <= sy < h:
            pixels[sy * w + sx] = specular
            if sx + 1 < w:
                pixels[sy * w + sx + 1] = color_lerp(pixels[sy * w + sx + 1], specular, 0.6)

    # Soft edge fade (rounded petal feel) — darken corners slightly
    for y in range(h):
        for x in range(w):
            # corner falloff
            cx, cy = w / 2, h / 2
            dx = (x - cx) / cx
            dy = (y - cy) / cy
            d = math.sqrt(dx * dx + dy * dy)
            if d > 0.9:
                base = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(base, deep_base, min(1.0, (d - 0.9) * 3))

    # Light noise sparkle
    add_noise_overlay(pixels, w, h, vein_pink, specular, density=0.018, seed=77)

    return png_from_pixels(pixels, w, h)


def build_ground_petal_texture():
    """Texture 1: ground scatter petals — slightly more saturated, ground shadow at bottom, wilted."""
    w, h = 64, 64
    deep_pink = hex_to_rgba("#f0a8c0")
    deeper = hex_to_rgba("#c87898")
    body = hex_to_rgba("#f8c8d8")
    wilt_brown = hex_to_rgba("#b88090")
    shadow = hex_to_rgba("#604858")

    pixels = [body] * (w * h)

    # Vertical gradient — saturated body fading to shadow at bottom
    for y in range(h):
        t = y / (h - 1)
        if t < 0.5:
            row = color_lerp(body, deep_pink, t * 2)
        elif t < 0.85:
            row = color_lerp(deep_pink, deeper, (t - 0.5) / 0.35)
        else:
            row = color_lerp(deeper, shadow, (t - 0.85) / 0.15)
        for x in range(w):
            pixels[y * w + x] = row

    # Wilting hints — brownish-pink patches near edges
    rng = random.Random(23)
    for _ in range(40):
        x = rng.randrange(w)
        y = rng.randrange(h)
        sz = rng.randint(2, 4)
        for dy in range(-sz, sz + 1):
            for dx in range(-sz, sz + 1):
                if 0 <= x + dx < w and 0 <= y + dy < h and dx * dx + dy * dy <= sz * sz:
                    base = pixels[(y + dy) * w + (x + dx)]
                    pixels[(y + dy) * w + (x + dx)] = color_lerp(base, wilt_brown, 0.25)

    # Stronger horizontal vein lines
    for vy in range(8, h - 8, 5):
        for x in range(2, w - 2):
            base = pixels[vy * w + x]
            pixels[vy * w + x] = color_lerp(base, deeper, 0.4)

    # Ground shadow band along bottom
    for y in range(h - 8, h):
        t = (y - (h - 8)) / 8
        for x in range(w):
            base = pixels[y * w + x]
            pixels[y * w + x] = color_lerp(base, shadow, t * 0.6)

    # Edge darkening (curl appearance)
    for y in range(h):
        for x in range(w):
            cx, cy = w / 2, h / 2
            dx = (x - cx) / cx
            dy = (y - cy) / cy
            d = math.sqrt(dx * dx + dy * dy)
            if d > 0.85:
                base = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(base, deeper, min(1.0, (d - 0.85) * 4))

    add_noise_overlay(pixels, w, h, deeper, body, density=0.02, seed=131)
    return png_from_pixels(pixels, w, h)


# ---------- BUILD ----------
def build():
    b = Builder("sakura_petal_bloom", resolution=(64, 64))
    b.add_texture("sakura_petal", build_petal_texture())
    b.add_texture("sakura_ground", build_ground_petal_texture())

    rng = random.Random(2024)

    # ========== CENTRAL BLOOM (5 overlapping flat slabs at Y=3) ==========
    central_bone_uuids = []
    central_meta = []  # (bone_uuid, name) for animations
    for i in range(5):
        ang = (i / 5) * math.pi  # spread around (180 deg)
        rot_y = (i * 36) % 360 - 180
        rot_z = (i * 22) - 50
        # Flat slab petal cluster cube
        sx = 2.6
        sy = 0.18
        sz = 1.8
        cx = math.cos(ang) * 0.4
        cz = math.sin(ang) * 0.4
        cy = 3.0
        f_uv = uniform_face(0, 0, 64, 64, tex_index=0)
        cube_uuid = b.add_cube(
            f"bloom_slab_{i}",
            from_=[cx - sx / 2, cy - sy / 2, cz - sz / 2],
            to_=[cx + sx / 2, cy + sy / 2, cz + sz / 2],
            faces=f_uv,
            origin=[cx, cy, cz],
            rotation=[0, rot_y, rot_z],
        )
        bone_uuid = make_uuid()
        bone = b.make_group(
            f"bloom_slab_{i}_b",
            origin=[cx, cy, cz],
            children=[cube_uuid],
            rotation=[0, rot_y, rot_z],
            group_uuid=bone_uuid,
        )
        central_bone_uuids.append(bone)
        central_meta.append((bone_uuid, f"bloom_slab_{i}_b"))

    central_group_uuid = make_uuid()
    central_group = b.make_group(
        "central_bloom",
        origin=[0, 3, 0],
        children=central_bone_uuids,
        group_uuid=central_group_uuid,
    )
    central_meta.append((central_group_uuid, "central_bloom"))

    # ========== 24 FLOATING PETALS (each = 2 overlapping flat cubes in own bone) ==========
    floating_groups = []
    floating_meta = []  # list of (bone_uuid, name, drift_dir_xyz)
    for i in range(24):
        # Distribute on sphere — golden ratio spiral
        phi = math.acos(1 - 2 * (i + 0.5) / 24)
        theta = math.pi * (1 + 5 ** 0.5) * (i + 0.5)
        radius = 4.5 + (i % 3) * 0.4
        px = radius * math.sin(phi) * math.cos(theta)
        py = 4.0 + radius * math.cos(phi) * 0.7
        pz = radius * math.sin(phi) * math.sin(theta)

        # Petal local rotation
        rot_x = rng.uniform(-30, 30)
        rot_y = rng.uniform(-180, 180)
        rot_z = rng.uniform(-30, 30)

        # Two overlapping flat cubes per petal
        # Cube A: larger flat
        sxA, syA, szA = 1.4, 0.12, 1.0
        f_uvA = uniform_face(0, 0, 64, 64, tex_index=0)
        cubeA = b.add_cube(
            f"petal_{i}_A",
            from_=[-sxA / 2, -syA / 2, -szA / 2],
            to_=[sxA / 2, syA / 2, szA / 2],
            faces=f_uvA,
            origin=[0, 0, 0],
            rotation=[0, 0, 0],
        )
        # Cube B: slightly smaller, in own sub-bone rotated 22.5 deg
        sxB, syB, szB = 1.2, 0.1, 0.85
        f_uvB = uniform_face(0, 0, 64, 64, tex_index=0)
        cubeB = b.add_cube(
            f"petal_{i}_B",
            from_=[-sxB / 2, -syB / 2, -szB / 2],
            to_=[sxB / 2, syB / 2, szB / 2],
            faces=f_uvB,
            origin=[0, 0, 0],
            rotation=[0, 0, 0],
        )
        sub_bone_uuid = make_uuid()
        sub_bone = b.make_group(
            f"petal_{i}_sub",
            origin=[0, 0, 0],
            children=[cubeB],
            rotation=[0, 0, 22.5],
            group_uuid=sub_bone_uuid,
        )

        bone_uuid = make_uuid()
        bone = b.make_group(
            f"petal_{i}_b",
            origin=[px, py, pz],
            children=[cubeA, sub_bone],
            rotation=[rot_x, rot_y, rot_z],
            group_uuid=bone_uuid,
        )
        floating_groups.append(bone)
        # Drift direction = outward from origin
        norm = max(0.001, math.sqrt(px * px + py * py + pz * pz))
        drift = (px / norm, py / norm, pz / norm)
        floating_meta.append((bone_uuid, f"petal_{i}_b", drift, (px, py, pz)))

    floating_group_uuid = make_uuid()
    floating_group = b.make_group(
        "floating_petals",
        origin=[0, 4, 0],
        children=floating_groups,
        group_uuid=floating_group_uuid,
    )

    # ========== 3 BURST RINGS (8 petals each, ground / mid / top) ==========
    ring_groups = []
    ring_meta = []
    ring_heights = [0.4, 4.0, 7.5]
    ring_radii = [3.5, 4.0, 3.0]
    for r_idx, (ry, rr) in enumerate(zip(ring_heights, ring_radii)):
        ring_children = []
        for j in range(8):
            ang = (j / 8) * 2 * math.pi
            px = math.cos(ang) * rr
            pz = math.sin(ang) * rr
            py = ry
            sx, sy, sz = 1.3, 0.1, 0.95
            f_uv = uniform_face(0, 0, 64, 64, tex_index=0)
            cube_uuid = b.add_cube(
                f"ring{r_idx}_petal_{j}",
                from_=[-sx / 2, -sy / 2, -sz / 2],
                to_=[sx / 2, sy / 2, sz / 2],
                faces=f_uv,
                origin=[0, 0, 0],
                rotation=[0, 0, 0],
            )
            rot_y = math.degrees(ang) + 90
            rot_x = rng.uniform(-20, 20)
            rot_z = rng.uniform(-20, 20)
            bone_uuid = make_uuid()
            bone = b.make_group(
                f"ring{r_idx}_petal_{j}_b",
                origin=[px, py, pz],
                children=[cube_uuid],
                rotation=[rot_x, rot_y, rot_z],
                group_uuid=bone_uuid,
            )
            ring_children.append(bone)
            ring_meta.append((bone_uuid, f"ring{r_idx}_petal_{j}_b", r_idx, (math.cos(ang), 0.0, math.sin(ang))))

        ring_group_uuid = make_uuid()
        ring_group = b.make_group(
            f"burst_ring_{r_idx}",
            origin=[0, ry, 0],
            children=ring_children,
            group_uuid=ring_group_uuid,
        )
        ring_groups.append(ring_group)

    # ========== GROUND SCATTER (8 flat petals at Y=0.1, lying flat) ==========
    scatter_groups = []
    scatter_meta = []
    for i in range(8):
        ang = (i / 8) * 2 * math.pi + 0.4
        r = rng.uniform(2.5, 5.5)
        px = math.cos(ang) * r
        pz = math.sin(ang) * r
        py = 0.1
        sx, sy, sz = 1.4, 0.08, 1.0
        f_uv = uniform_face(0, 0, 64, 64, tex_index=1)
        cube_uuid = b.add_cube(
            f"scatter_{i}",
            from_=[-sx / 2, -sy / 2, -sz / 2],
            to_=[sx / 2, sy / 2, sz / 2],
            faces=f_uv,
            origin=[0, 0, 0],
            rotation=[0, 0, 0],
        )
        rot_y = math.degrees(ang) + rng.uniform(-30, 30)
        bone_uuid = make_uuid()
        bone = b.make_group(
            f"scatter_{i}_b",
            origin=[px, py, pz],
            children=[cube_uuid],
            rotation=[0, rot_y, 0],
            group_uuid=bone_uuid,
        )
        scatter_groups.append(bone)
        scatter_meta.append((bone_uuid, f"scatter_{i}_b", (px, py, pz)))

    scatter_group_uuid = make_uuid()
    scatter_root = b.make_group(
        "ground_scatter",
        origin=[0, 0.1, 0],
        children=scatter_groups,
        group_uuid=scatter_group_uuid,
    )

    # ========== ROOT ==========
    root_uuid = b.add_root_group(
        "root",
        origin=[0, 0, 0],
        children=[central_group, floating_group, ring_groups[0], ring_groups[1], ring_groups[2], scatter_root],
    )

    # ========== ANIMATIONS ==========
    # ---- SPAWN (0.6s, once, override=true) ----
    spawn_animators = {}

    # Central bloom: scale 0 -> 1 rapidly. Apply on each slab + central group.
    for bone_uuid, name in central_meta:
        kfs = []
        if name == "central_bloom":
            kfs.append(make_keyframe("scale", 0.0, 0.05, 0.05, 0.05))
            kfs.append(make_keyframe("scale", 0.15, 1.4, 1.4, 1.4))
            kfs.append(make_keyframe("scale", 0.3, 1.0, 1.0, 1.0))
            kfs.append(make_keyframe("scale", 0.6, 1.0, 1.0, 1.0))
            kfs.append(make_keyframe("rotation", 0.0, 0, 0, 0))
            kfs.append(make_keyframe("rotation", 0.6, 0, 180, 0))
        else:
            # individual slab spin
            kfs.append(make_keyframe("rotation", 0.0, 0, rng.uniform(-180, 180), rng.uniform(-30, 30)))
            kfs.append(make_keyframe("rotation", 0.6, rng.uniform(-90, 90), rng.uniform(-180, 180), rng.uniform(-30, 30)))
            kfs.append(make_keyframe("scale", 0.0, 0.1, 0.1, 0.1))
            kfs.append(make_keyframe("scale", 0.2, 1.2, 1.2, 1.2))
            kfs.append(make_keyframe("scale", 0.6, 1.0, 1.0, 1.0))
        spawn_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # Floating petals: appear mid-motion (not at rest)
    for bone_uuid, name, drift, (px, py, pz) in floating_meta:
        kfs = []
        # Position: start near center, end at rest position (mid-motion drift)
        start_x = px * 0.3
        start_y = py * 0.3 + 1.0
        start_z = pz * 0.3
        kfs.append(make_keyframe("position", 0.0, start_x - px, start_y - py, start_z - pz))
        kfs.append(make_keyframe("position", 0.4, (start_x - px) * 0.3, (start_y - py) * 0.3, (start_z - pz) * 0.3))
        kfs.append(make_keyframe("position", 0.6, 0, 0, 0))
        # Rotation: continuously spinning since spawn
        rspeed = rng.uniform(120, 360)
        kfs.append(make_keyframe("rotation", 0.0, rng.uniform(-90, 90), 0, rng.uniform(-90, 90)))
        kfs.append(make_keyframe("rotation", 0.3, rng.uniform(-180, 180), rspeed * 0.3, rng.uniform(-180, 180)))
        kfs.append(make_keyframe("rotation", 0.6, rng.uniform(-180, 180), rspeed * 0.6, rng.uniform(-180, 180)))
        # Scale: appear slightly smaller, settle
        kfs.append(make_keyframe("scale", 0.0, 0.4, 0.4, 0.4))
        kfs.append(make_keyframe("scale", 0.15, 1.15, 1.15, 1.15))
        kfs.append(make_keyframe("scale", 0.6, 1.0, 1.0, 1.0))
        spawn_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # Burst rings: expand outward starting at 0.1s
    for bone_uuid, name, r_idx, (dx, dy, dz) in ring_meta:
        kfs = []
        # Position: start collapsed, expand outward at 0.1s+
        kfs.append(make_keyframe("position", 0.0, -dx * 2.5, 0, -dz * 2.5))
        kfs.append(make_keyframe("position", 0.1, -dx * 2.0, 0, -dz * 2.0))
        kfs.append(make_keyframe("position", 0.35, dx * 0.4, 0.2, dz * 0.4))
        kfs.append(make_keyframe("position", 0.6, 0, 0, 0))
        # Rotation: spinning
        kfs.append(make_keyframe("rotation", 0.0, 0, 0, 0))
        kfs.append(make_keyframe("rotation", 0.6, rng.uniform(-180, 180), rng.uniform(-360, 360), rng.uniform(-180, 180)))
        # Scale: grow
        kfs.append(make_keyframe("scale", 0.0, 0.0, 0.0, 0.0))
        kfs.append(make_keyframe("scale", 0.1, 0.2, 0.2, 0.2))
        kfs.append(make_keyframe("scale", 0.3, 1.3, 1.3, 1.3))
        kfs.append(make_keyframe("scale", 0.6, 1.0, 1.0, 1.0))
        spawn_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # Ground scatter: drift down from various heights starting at 0.2s
    for bone_uuid, name, (px, py, pz) in scatter_meta:
        start_h = rng.uniform(2.5, 5.0)
        kfs = []
        kfs.append(make_keyframe("position", 0.0, 0, start_h, 0))
        kfs.append(make_keyframe("position", 0.2, 0, start_h * 0.8, 0))
        kfs.append(make_keyframe("position", 0.45, 0, start_h * 0.25, 0))
        kfs.append(make_keyframe("position", 0.6, 0, 0, 0))
        kfs.append(make_keyframe("rotation", 0.0, rng.uniform(-90, 90), rng.uniform(-180, 180), rng.uniform(-90, 90)))
        kfs.append(make_keyframe("rotation", 0.6, 0, rng.uniform(-180, 180), 0))
        kfs.append(make_keyframe("scale", 0.0, 0.5, 0.5, 0.5))
        kfs.append(make_keyframe("scale", 0.6, 1.0, 1.0, 1.0))
        spawn_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    b.add_animation("spawn", length=0.6, animators=spawn_animators, loop="once", override=True)

    # ---- IDLE (3.0s, loop, override=false) ----
    idle_animators = {}

    # Central bloom: pulse + slow rotation
    for bone_uuid, name in central_meta:
        kfs = []
        if name == "central_bloom":
            kfs.append(make_keyframe("rotation", 0.0, 0, 0, 0))
            kfs.append(make_keyframe("rotation", 1.5, 0, 60, 0))
            kfs.append(make_keyframe("rotation", 3.0, 0, 120, 0))
            kfs.append(make_keyframe("scale", 0.0, 1.0, 1.0, 1.0))
            kfs.append(make_keyframe("scale", 0.75, 1.12, 1.05, 1.12))
            kfs.append(make_keyframe("scale", 1.5, 1.0, 1.0, 1.0))
            kfs.append(make_keyframe("scale", 2.25, 1.08, 1.03, 1.08))
            kfs.append(make_keyframe("scale", 3.0, 1.0, 1.0, 1.0))
        else:
            spd = rng.uniform(60, 200)
            kfs.append(make_keyframe("rotation", 0.0, 0, 0, 0))
            kfs.append(make_keyframe("rotation", 1.0, rng.uniform(-30, 30), spd / 3, rng.uniform(-30, 30)))
            kfs.append(make_keyframe("rotation", 2.0, rng.uniform(-30, 30), spd * 2 / 3, rng.uniform(-30, 30)))
            kfs.append(make_keyframe("rotation", 3.0, 0, spd, 0))
            kfs.append(make_keyframe("position", 0.0, 0, 0, 0))
            kfs.append(make_keyframe("position", 1.5, rng.uniform(-0.2, 0.2), rng.uniform(-0.15, 0.15), rng.uniform(-0.2, 0.2)))
            kfs.append(make_keyframe("position", 3.0, 0, 0, 0))
        idle_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # Floating petals: rotate on own axis, drift in 3 axes
    for bone_uuid, name, drift, (px, py, pz) in floating_meta:
        rspeed_y = rng.uniform(80, 280)
        rspeed_x = rng.uniform(-150, 150)
        rspeed_z = rng.uniform(-150, 150)
        kfs = []
        # Continuous rotation (3 keyframes for smooth loop)
        kfs.append(make_keyframe("rotation", 0.0, 0, 0, 0))
        kfs.append(make_keyframe("rotation", 1.0, rspeed_x / 3, rspeed_y / 3, rspeed_z / 3))
        kfs.append(make_keyframe("rotation", 2.0, rspeed_x * 2 / 3, rspeed_y * 2 / 3, rspeed_z * 2 / 3))
        kfs.append(make_keyframe("rotation", 3.0, rspeed_x, rspeed_y, rspeed_z))
        # Position drift (subtle in 3 axes)
        amp = rng.uniform(0.3, 0.7)
        ph = rng.uniform(0, 2 * math.pi)
        kfs.append(make_keyframe("position", 0.0, 0, 0, 0))
        kfs.append(make_keyframe("position", 0.75, math.sin(ph) * amp, math.cos(ph) * amp * 0.7, math.cos(ph + 1) * amp))
        kfs.append(make_keyframe("position", 1.5, math.sin(ph + 1.5) * amp, math.cos(ph + 1) * amp * 0.7, math.cos(ph + 2) * amp))
        kfs.append(make_keyframe("position", 2.25, math.sin(ph + 3) * amp, math.cos(ph + 2.5) * amp * 0.7, math.cos(ph + 3.5) * amp))
        kfs.append(make_keyframe("position", 3.0, 0, 0, 0))
        idle_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # Whole floating cloud slow rotates
    idle_animators[floating_group_uuid] = {
        "name": "floating_petals",
        "type": "bone",
        "keyframes": [
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 1.5, 0, 30, 0),
            make_keyframe("rotation", 3.0, 0, 60, 0),
        ],
    }

    # Burst rings: rotate
    for bone_uuid, name, r_idx, (dx, dy, dz) in ring_meta:
        spd = rng.uniform(80, 220)
        kfs = []
        kfs.append(make_keyframe("rotation", 0.0, 0, 0, 0))
        kfs.append(make_keyframe("rotation", 1.5, rng.uniform(-20, 20), spd / 2, rng.uniform(-20, 20)))
        kfs.append(make_keyframe("rotation", 3.0, 0, spd, 0))
        kfs.append(make_keyframe("position", 0.0, 0, 0, 0))
        kfs.append(make_keyframe("position", 1.5, rng.uniform(-0.15, 0.15), rng.uniform(-0.2, 0.2), rng.uniform(-0.15, 0.15)))
        kfs.append(make_keyframe("position", 3.0, 0, 0, 0))
        idle_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # Ground scatter: flutter slightly
    for bone_uuid, name, (px, py, pz) in scatter_meta:
        kfs = []
        kfs.append(make_keyframe("rotation", 0.0, 0, 0, 0))
        kfs.append(make_keyframe("rotation", 0.75, rng.uniform(-8, 8), rng.uniform(-15, 15), rng.uniform(-8, 8)))
        kfs.append(make_keyframe("rotation", 1.5, rng.uniform(-8, 8), rng.uniform(-15, 15), rng.uniform(-8, 8)))
        kfs.append(make_keyframe("rotation", 2.25, rng.uniform(-8, 8), rng.uniform(-15, 15), rng.uniform(-8, 8)))
        kfs.append(make_keyframe("rotation", 3.0, 0, 0, 0))
        kfs.append(make_keyframe("position", 0.0, 0, 0, 0))
        kfs.append(make_keyframe("position", 0.75, rng.uniform(-0.1, 0.1), rng.uniform(0, 0.15), rng.uniform(-0.1, 0.1)))
        kfs.append(make_keyframe("position", 1.5, rng.uniform(-0.1, 0.1), rng.uniform(0, 0.15), rng.uniform(-0.1, 0.1)))
        kfs.append(make_keyframe("position", 2.25, rng.uniform(-0.1, 0.1), rng.uniform(0, 0.15), rng.uniform(-0.1, 0.1)))
        kfs.append(make_keyframe("position", 3.0, 0, 0, 0))
        idle_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    b.add_animation("idle", length=3.0, animators=idle_animators, loop="loop", override=False)

    # ---- DISSIPATE (0.5s, once, override=true) ----
    diss_animators = {}

    # Central bloom: rapidly scale to 0
    for bone_uuid, name in central_meta:
        kfs = []
        if name == "central_bloom":
            kfs.append(make_keyframe("scale", 0.0, 1.0, 1.0, 1.0))
            kfs.append(make_keyframe("scale", 0.2, 0.6, 0.6, 0.6))
            kfs.append(make_keyframe("scale", 0.5, 0.0, 0.0, 0.0))
            kfs.append(make_keyframe("rotation", 0.0, 0, 0, 0))
            kfs.append(make_keyframe("rotation", 0.5, 0, 240, 0))
        else:
            kfs.append(make_keyframe("scale", 0.0, 1.0, 1.0, 1.0))
            kfs.append(make_keyframe("scale", 0.5, 0.0, 0.0, 0.0))
            kfs.append(make_keyframe("rotation", 0.0, 0, 0, 0))
            kfs.append(make_keyframe("rotation", 0.5, rng.uniform(-90, 90), rng.uniform(-360, 360), rng.uniform(-90, 90)))
        diss_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # Floating petals: continue outward, scale to 0
    for bone_uuid, name, (dx, dy, dz), (px, py, pz) in floating_meta:
        kfs = []
        # Travel outward in drift direction
        kfs.append(make_keyframe("position", 0.0, 0, 0, 0))
        kfs.append(make_keyframe("position", 0.25, dx * 1.8, dy * 1.8 + 0.5, dz * 1.8))
        kfs.append(make_keyframe("position", 0.5, dx * 4.5, dy * 4.5 + 1.2, dz * 4.5))
        kfs.append(make_keyframe("scale", 0.0, 1.0, 1.0, 1.0))
        kfs.append(make_keyframe("scale", 0.3, 0.7, 0.7, 0.7))
        kfs.append(make_keyframe("scale", 0.5, 0.0, 0.0, 0.0))
        kfs.append(make_keyframe("rotation", 0.0, 0, 0, 0))
        kfs.append(make_keyframe("rotation", 0.5, rng.uniform(-180, 180), rng.uniform(-540, 540), rng.uniform(-180, 180)))
        diss_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # Burst rings: continue outward
    for bone_uuid, name, r_idx, (dx, dy, dz) in ring_meta:
        kfs = []
        kfs.append(make_keyframe("position", 0.0, 0, 0, 0))
        kfs.append(make_keyframe("position", 0.5, dx * 5.0, 1.0 + r_idx * 0.5, dz * 5.0))
        kfs.append(make_keyframe("scale", 0.0, 1.0, 1.0, 1.0))
        kfs.append(make_keyframe("scale", 0.5, 0.0, 0.0, 0.0))
        kfs.append(make_keyframe("rotation", 0.0, 0, 0, 0))
        kfs.append(make_keyframe("rotation", 0.5, rng.uniform(-180, 180), rng.uniform(-360, 360), rng.uniform(-180, 180)))
        diss_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # Ground scatter: drift further down
    for bone_uuid, name, (px, py, pz) in scatter_meta:
        kfs = []
        kfs.append(make_keyframe("position", 0.0, 0, 0, 0))
        kfs.append(make_keyframe("position", 0.5, rng.uniform(-0.3, 0.3), -1.0, rng.uniform(-0.3, 0.3)))
        kfs.append(make_keyframe("scale", 0.0, 1.0, 1.0, 1.0))
        kfs.append(make_keyframe("scale", 0.5, 0.0, 0.0, 0.0))
        kfs.append(make_keyframe("rotation", 0.0, 0, 0, 0))
        kfs.append(make_keyframe("rotation", 0.5, 0, rng.uniform(-180, 180), 0))
        diss_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    b.add_animation("dissipate", length=0.5, animators=diss_animators, loop="once", override=True)

    b.write(OUT)
    return OUT


if __name__ == "__main__":
    path = build()
    import os
    print("WROTE", path, os.path.getsize(path) // 1024, "KB")
