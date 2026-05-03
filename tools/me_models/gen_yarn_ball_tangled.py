#!/usr/bin/env python3
"""
Generate yarn_ball_tangled.bbmodel — YARN BALL IMPACT VFX for Fluffy Mode.

A massive coral-pink yarn ball drops from the sky, slams into the ground with
squash-and-stretch cartoon physics, sends spiralling yarn strands outward, then
bounces once and settles. Loose-yarn aesthetic with multi-coloured strands.
"""
import sys
import math
import random
sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder, make_uuid, make_keyframe, basic_cube_faces, uniform_face,
    png_from_pixels, hex_to_rgba, fill_rect,
)


# ---------------------------------------------------------------------------
# TEXTURES
# ---------------------------------------------------------------------------
def build_yarn_core_texture():
    """64x64 wound yarn — coral pink with darker diagonal winding lines."""
    w, h = 64, 64
    base = hex_to_rgba("#ffb8a0")
    dark = hex_to_rgba("#e89080")
    light = hex_to_rgba("#ffd8c8")
    spec = hex_to_rgba("#fff0e8")
    emis_mint = hex_to_rgba("#b8ffd0")

    pixels = [base] * (w * h)

    # Lighter zones between winding lines (every 6px diagonal stripe)
    for y in range(h):
        for x in range(w):
            # Diagonal 30-degree pattern using x + y*tan(30) ~ x + y*0.577
            diag = (x + int(y * 0.577)) % 6
            if diag == 0 or diag == 1:
                pixels[y * w + x] = dark  # winding line (2px wide)
            elif diag == 3 or diag == 4:
                pixels[y * w + x] = light  # lighter zone

    # Specular highlight at (8,5) — small bright spot
    rng = random.Random(101)
    for dy in range(-2, 3):
        for dx in range(-2, 3):
            d = math.sqrt(dx*dx + dy*dy)
            if d <= 2.0:
                px = 8 + dx
                py = 5 + dy
                if 0 <= px < w and 0 <= py < h:
                    pixels[py * w + px] = spec

    # Secondary specular for visual richness
    for dy in range(-1, 2):
        for dx in range(-1, 2):
            px = 42 + dx
            py = 38 + dy
            if 0 <= px < w and 0 <= py < h:
                pixels[py * w + px] = spec

    # Emissive mint green at random winding line intersections (2px each)
    intersections = [(12, 10), (28, 22), (45, 18), (18, 38), (50, 42),
                     (8, 52), (36, 50), (22, 4), (56, 30), (4, 28),
                     (60, 56), (30, 60)]
    for cx, cy in intersections:
        for dy in range(2):
            for dx in range(2):
                px = cx + dx
                py = cy + dy
                if 0 <= px < w and 0 <= py < h:
                    pixels[py * w + px] = emis_mint

    # Subtle noise speckles for fluff
    for _ in range(120):
        x = rng.randrange(w)
        y = rng.randrange(h)
        if rng.random() < 0.5:
            pixels[y * w + x] = light
        else:
            pixels[y * w + x] = dark

    return png_from_pixels(pixels, w, h)


def build_yarn_strand_texture():
    """64x64 loose yarn — chaotic winding, fraying, alternating coral/lavender."""
    w, h = 64, 64
    base = hex_to_rgba("#ffb8a0")
    dark = hex_to_rgba("#c87060")
    lavender = hex_to_rgba("#d8b8ff")
    lavender_dark = hex_to_rgba("#a888c8")
    bright_strip = hex_to_rgba("#ffc8b0")
    fray = hex_to_rgba("#ffe0d0")

    pixels = [base] * (w * h)

    # Alternating coral and lavender bands (multi-colour yarn look)
    for y in range(h):
        band = (y // 8) % 2
        for x in range(w):
            if band == 1:
                pixels[y * w + x] = lavender

    # Thicker chaotic winding lines (3px wide, varying angles)
    rng = random.Random(202)
    for y in range(h):
        for x in range(w):
            # Multiple overlapping diagonal patterns
            diag1 = (x * 2 + y) % 9
            diag2 = (x - y * 2) % 11
            if diag1 < 2:
                if pixels[y * w + x] == lavender:
                    pixels[y * w + x] = lavender_dark
                else:
                    pixels[y * w + x] = dark
            elif diag2 < 2:
                if pixels[y * w + x] == lavender:
                    pixels[y * w + x] = lavender_dark
                else:
                    pixels[y * w + x] = dark

    # Bright emissive strip down centre — 2px wide
    for y in range(h):
        for x in range(31, 33):
            pixels[y * w + x] = bright_strip

    # Fraying at edges — random transparent/light pixels at top/bottom
    for x in range(w):
        if rng.random() < 0.4:
            for y in range(rng.randint(0, 3)):
                pixels[y * w + x] = fray
        if rng.random() < 0.4:
            for y in range(h - rng.randint(0, 3), h):
                pixels[y * w + x] = fray

    # Side fraying
    for y in range(h):
        if rng.random() < 0.3:
            for x in range(rng.randint(0, 2)):
                pixels[y * w + x] = fray
        if rng.random() < 0.3:
            for x in range(w - rng.randint(0, 2), w):
                pixels[y * w + x] = fray

    # Chaotic noise speckles
    for _ in range(220):
        x = rng.randrange(w)
        y = rng.randrange(h)
        choice = rng.random()
        if choice < 0.33:
            pixels[y * w + x] = dark
        elif choice < 0.66:
            pixels[y * w + x] = bright_strip
        else:
            pixels[y * w + x] = fray

    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# BUILD MODEL
# ---------------------------------------------------------------------------
def build():
    b = Builder("yarn_ball_tangled", resolution=(64, 64), visible_box=(8, 8, 0))

    # Textures
    b.add_texture("yarn_core", build_yarn_core_texture())
    b.add_texture("yarn_strand", build_yarn_strand_texture())

    core_faces = lambda sx, sy, sz: basic_cube_faces(0, 0, sx, sy, sz, tex_index=0)
    strand_faces = lambda sx, sy, sz: basic_cube_faces(0, 0, sx, sy, sz, tex_index=1)

    # ----- BALL CORE: 8 overlapping cubes approximating a sphere ~3 unit radius -----
    # Sphere centred at (0, 12, 0) — ground impact point at Y=0, ball Y offset for fall
    core_bones = []   # list of (name, group_uuid, cube_uuid, base_origin)
    core_offsets = [
        ("core_a", [-2.5, 9.5, -2.5], [2.5, 14.5, 2.5], [0, 0, 0]),
        ("core_b", [-2.2, 10.0, -2.8], [2.8, 14.0, 2.2], [0, 15, 0]),
        ("core_c", [-2.8, 10.2, -2.2], [2.2, 13.8, 2.8], [0, -20, 0]),
        ("core_d", [-2.4, 9.8, -2.4], [2.6, 14.2, 2.6], [12, 8, -5]),
        ("core_e", [-2.6, 10.4, -2.5], [2.4, 13.6, 2.5], [-8, 25, 10]),
        ("core_f", [-2.3, 9.6, -2.6], [2.7, 14.4, 2.4], [5, -15, 7]),
        ("core_g", [-2.5, 10.5, -2.3], [2.5, 13.5, 2.7], [-12, 30, -8]),
        ("core_h", [-2.7, 9.9, -2.5], [2.3, 14.1, 2.5], [10, -25, 12]),
    ]
    for name, frm, to_, rot in core_offsets:
        cube_uuid = b.add_cube(name, frm, to_,
                               core_faces(int(to_[0]-frm[0]), int(to_[1]-frm[1]), int(to_[2]-frm[2])),
                               origin=[0, 12, 0], rotation=rot)
        bone_uuid = make_uuid()
        bone = b.make_group(name, [0, 12, 0], [cube_uuid], rotation=rot, group_uuid=bone_uuid)
        core_bones.append((name, bone_uuid, cube_uuid, bone))

    # ----- YARN WRAP: 4 flat slabs at 45 degree offsets -----
    wrap_bones = []
    wrap_configs = [
        ("wrap_a", [-3.2, 11.5, -3.2], [3.2, 12.5, 3.2], [0, 0, 0]),
        ("wrap_b", [-3.2, 11.5, -3.2], [3.2, 12.5, 3.2], [0, 45, 30]),
        ("wrap_c", [-3.2, 11.5, -3.2], [3.2, 12.5, 3.2], [45, 0, 60]),
        ("wrap_d", [-3.2, 11.5, -3.2], [3.2, 12.5, 3.2], [30, 90, -45]),
    ]
    for name, frm, to_, rot in wrap_configs:
        cube_uuid = b.add_cube(name, frm, to_,
                               core_faces(int(to_[0]-frm[0]), int(to_[1]-frm[1]), int(to_[2]-frm[2])),
                               origin=[0, 12, 0], rotation=rot)
        bone_uuid = make_uuid()
        bone = b.make_group(name, [0, 12, 0], [cube_uuid], rotation=rot, group_uuid=bone_uuid)
        wrap_bones.append((name, bone_uuid, cube_uuid, bone))

    # ----- YARN STRAND ERUPTION: 8 directions, 3 segments each -----
    strand_bones = []  # list of (name, bone_uuid, group_dict)
    n_strands = 8
    for i in range(n_strands):
        angle = (2 * math.pi * i) / n_strands
        cos_a = math.cos(angle)
        sin_a = math.sin(angle)
        prev_dist = 0.0
        for seg in range(3):
            seg_dist = 1.5 + seg * 1.8  # outward distance
            seg_width = max(0.3, 0.7 - seg * 0.18)
            seg_len = 1.6 - seg * 0.3
            # cube extends along radial direction
            cx = cos_a * seg_dist
            cz = sin_a * seg_dist
            # axis-aligned slab approximation; rotation in bone applies orientation
            frm = [-seg_len, 0.0, -seg_width]
            to_ = [seg_len, 0.4, seg_width]
            cube_uuid = b.add_cube(f"strand_{i}_{seg}", frm, to_,
                                   strand_faces(int(to_[0]-frm[0]) or 1,
                                                int(to_[1]-frm[1]) or 1,
                                                int(to_[2]-frm[2]) or 1),
                                   origin=[0, 0, 0], rotation=[0, 0, 0])
            yaw = math.degrees(angle)
            bone_uuid = make_uuid()
            bone = b.make_group(f"strand_{i}_{seg}_bone",
                                [cx, 0.2, cz],
                                [cube_uuid],
                                rotation=[0, yaw, 0],
                                group_uuid=bone_uuid)
            strand_bones.append((f"strand_{i}_{seg}", bone_uuid, bone, i, seg))
            prev_dist = seg_dist

    # ----- IMPACT RING: 10 slabs at Y=0 -----
    ring_bones = []
    n_ring = 10
    for i in range(n_ring):
        angle = (2 * math.pi * i) / n_ring
        cx = math.cos(angle) * 3.5
        cz = math.sin(angle) * 3.5
        frm = [-1.4, 0.0, -0.5]
        to_ = [1.4, 0.3, 0.5]
        cube_uuid = b.add_cube(f"ring_{i}", frm, to_,
                               strand_faces(int(to_[0]-frm[0]) or 1, 1, 1),
                               origin=[0, 0, 0], rotation=[0, 0, 0])
        yaw = math.degrees(angle) + 90
        bone_uuid = make_uuid()
        bone = b.make_group(f"ring_{i}_bone",
                            [cx, 0.15, cz],
                            [cube_uuid],
                            rotation=[0, yaw, 0],
                            group_uuid=bone_uuid)
        ring_bones.append((f"ring_{i}", bone_uuid, bone, i))

    # ----- TRAILING YARN: 4 thin slabs above ball (loose end falling) -----
    trail_bones = []
    trail_configs = [
        ("trail_a", [-0.4, 14.5, -0.4], [0.4, 16.0, 0.4], [0, 0, 15]),
        ("trail_b", [-0.4, 16.0, -0.4], [0.4, 17.5, 0.4], [10, 20, -10]),
        ("trail_c", [-0.4, 17.5, -0.4], [0.4, 19.0, 0.4], [-15, -10, 20]),
        ("trail_d", [-0.4, 19.0, -0.4], [0.4, 20.5, 0.4], [20, 30, -25]),
    ]
    for name, frm, to_, rot in trail_configs:
        cube_uuid = b.add_cube(name, frm, to_,
                               strand_faces(1, int(to_[1]-frm[1]), 1),
                               origin=[0, frm[1], 0], rotation=rot)
        bone_uuid = make_uuid()
        bone = b.make_group(name, [0, frm[1], 0], [cube_uuid], rotation=rot, group_uuid=bone_uuid)
        trail_bones.append((name, bone_uuid, cube_uuid, bone, frm[1]))

    # ----- ROOT GROUP -----
    # Sub-groups for organisation
    core_group_uuid = make_uuid()
    core_group = b.make_group("core", [0, 12, 0],
                              [bone for (_, _, _, bone) in core_bones]
                              + [bone for (_, _, _, bone) in wrap_bones],
                              group_uuid=core_group_uuid)

    strand_group_uuid = make_uuid()
    strand_group = b.make_group("strands", [0, 0, 0],
                                [bone for (_, _, bone, _, _) in strand_bones],
                                group_uuid=strand_group_uuid)

    ring_group_uuid = make_uuid()
    ring_group = b.make_group("ring", [0, 0, 0],
                              [bone for (_, _, bone, _) in ring_bones],
                              group_uuid=ring_group_uuid)

    trail_group_uuid = make_uuid()
    trail_group = b.make_group("trail", [0, 14, 0],
                               [bone for (_, _, _, bone, _) in trail_bones],
                               group_uuid=trail_group_uuid)

    root_uuid = b.add_root_group("root", [0, 0, 0],
                                 [core_group, strand_group, ring_group, trail_group])

    # =======================================================================
    # ANIMATIONS
    # =======================================================================

    # ------ SPAWN: 1.2s, ball drops, squash, bounces, settles ------
    spawn_animators = {}

    # ROOT — ball Y position fall + squash-and-stretch scale
    root_kfs = []
    # Position channel — ball drops from Y+22 to ground
    root_kfs.append(make_keyframe("position", 0.0, 0, 22, 0, "linear"))
    root_kfs.append(make_keyframe("position", 0.4, 0, 14, 0, "linear"))
    root_kfs.append(make_keyframe("position", 0.7, 0, 4, 0, "linear"))
    root_kfs.append(make_keyframe("position", 0.8, 0, 0, 0, "linear"))   # impact
    root_kfs.append(make_keyframe("position", 0.85, 0, 0, 0, "linear"))  # squash hold
    root_kfs.append(make_keyframe("position", 0.9, 0, 4, 0, "linear"))   # bounce up
    root_kfs.append(make_keyframe("position", 1.0, 0, 1.5, 0, "linear"))
    root_kfs.append(make_keyframe("position", 1.1, 0, 0.3, 0, "linear"))
    root_kfs.append(make_keyframe("position", 1.2, 0, 0, 0, "linear"))

    # Scale channel — squash and stretch
    root_kfs.append(make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, "linear"))
    root_kfs.append(make_keyframe("scale", 0.7, 1.0, 1.0, 1.0, "linear"))
    root_kfs.append(make_keyframe("scale", 0.78, 1.05, 0.95, 1.05, "linear"))  # pre-impact compress
    root_kfs.append(make_keyframe("scale", 0.8, 1.3, 0.65, 1.3, "linear"))     # full SQUASH
    root_kfs.append(make_keyframe("scale", 0.85, 1.25, 0.7, 1.25, "linear"))   # squash hold
    root_kfs.append(make_keyframe("scale", 0.9, 0.85, 1.2, 0.85, "linear"))    # STRETCH (overshoot)
    root_kfs.append(make_keyframe("scale", 1.0, 1.0, 1.0, 1.0, "linear"))
    root_kfs.append(make_keyframe("scale", 1.05, 1.1, 0.92, 1.1, "linear"))    # wobble
    root_kfs.append(make_keyframe("scale", 1.12, 0.95, 1.05, 0.95, "linear"))
    root_kfs.append(make_keyframe("scale", 1.2, 1.0, 1.0, 1.0, "linear"))

    # Rotation — slight spin during fall
    root_kfs.append(make_keyframe("rotation", 0.0, 0, 0, 0, "linear"))
    root_kfs.append(make_keyframe("rotation", 0.4, 30, 60, -20, "linear"))
    root_kfs.append(make_keyframe("rotation", 0.8, 60, 120, -45, "linear"))
    root_kfs.append(make_keyframe("rotation", 1.0, 65, 130, -50, "linear"))
    root_kfs.append(make_keyframe("rotation", 1.2, 70, 140, -55, "linear"))

    spawn_animators[root_uuid] = {"name": "root", "type": "bone", "keyframes": root_kfs}

    # CORE cubes — individual jiggle on impact for fluffy feel
    for idx, (name, bone_uuid, cube_uuid, _) in enumerate(core_bones):
        kfs = []
        phase = idx * 0.13
        # Rotation jiggle
        kfs.append(make_keyframe("rotation", 0.0, 0, 0, 0, "linear"))
        kfs.append(make_keyframe("rotation", 0.79, 0, 0, 0, "linear"))
        kfs.append(make_keyframe("rotation", 0.82, 8 * math.cos(phase), 12 * math.sin(phase), 6 * math.cos(phase * 1.3), "linear"))
        kfs.append(make_keyframe("rotation", 0.88, -6 * math.sin(phase), -8 * math.cos(phase), -4 * math.sin(phase * 1.3), "linear"))
        kfs.append(make_keyframe("rotation", 0.95, 4 * math.cos(phase * 0.7), 5 * math.sin(phase * 0.7), 2 * math.cos(phase), "linear"))
        kfs.append(make_keyframe("rotation", 1.05, -2 * math.cos(phase), -3 * math.sin(phase), -1 * math.cos(phase), "linear"))
        kfs.append(make_keyframe("rotation", 1.2, 0, 0, 0, "linear"))
        # Scale ripple on impact
        kfs.append(make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, "linear"))
        kfs.append(make_keyframe("scale", 0.78, 1.0, 1.0, 1.0, "linear"))
        kfs.append(make_keyframe("scale", 0.82, 1.1 + 0.05 * math.cos(phase), 0.85, 1.1 + 0.05 * math.sin(phase), "linear"))
        kfs.append(make_keyframe("scale", 0.9, 0.92, 1.08, 0.92, "linear"))
        kfs.append(make_keyframe("scale", 1.05, 1.03, 0.97, 1.03, "linear"))
        kfs.append(make_keyframe("scale", 1.2, 1.0, 1.0, 1.0, "linear"))
        spawn_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # WRAP slabs — rotate during fall (suggesting spin)
    for idx, (name, bone_uuid, cube_uuid, _) in enumerate(wrap_bones):
        kfs = []
        rx0, ry0, rz0 = 0, 0, 0
        if name == "wrap_b": rx0, ry0, rz0 = 0, 45, 30
        elif name == "wrap_c": rx0, ry0, rz0 = 45, 0, 60
        elif name == "wrap_d": rx0, ry0, rz0 = 30, 90, -45
        # Spin during fall
        kfs.append(make_keyframe("rotation", 0.0, rx0, ry0, rz0, "linear"))
        kfs.append(make_keyframe("rotation", 0.4, rx0 + 60, ry0 + 90, rz0 + 45, "linear"))
        kfs.append(make_keyframe("rotation", 0.8, rx0 + 120, ry0 + 180, rz0 + 90, "linear"))
        kfs.append(make_keyframe("rotation", 1.0, rx0 + 140, ry0 + 200, rz0 + 100, "linear"))
        kfs.append(make_keyframe("rotation", 1.2, rx0 + 160, ry0 + 220, rz0 + 110, "linear"))
        spawn_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # STRANDS — hidden until impact, then blast outward
    for (name, bone_uuid, _, dir_idx, seg_idx) in strand_bones:
        kfs = []
        angle = (2 * math.pi * dir_idx) / n_strands
        cos_a = math.cos(angle)
        sin_a = math.sin(angle)
        # Hidden initially — scale 0
        kfs.append(make_keyframe("scale", 0.0, 0.0, 0.0, 0.0, "linear"))
        kfs.append(make_keyframe("scale", 0.79, 0.0, 0.0, 0.0, "linear"))
        # Pop in at impact (0.8s) — segments stagger
        seg_delay = 0.0 + seg_idx * 0.04
        kfs.append(make_keyframe("scale", 0.8 + seg_delay, 0.5, 0.6, 0.5, "linear"))
        kfs.append(make_keyframe("scale", 0.85 + seg_delay, 1.2, 1.1, 1.2, "linear"))
        kfs.append(make_keyframe("scale", 0.95, 1.0, 1.0, 1.0, "linear"))
        kfs.append(make_keyframe("scale", 1.2, 1.0, 1.0, 1.0, "linear"))
        # Position — shoot outward from origin
        seg_dist = 1.5 + seg_idx * 1.8
        kfs.append(make_keyframe("position", 0.0, 0, 0, 0, "linear"))
        kfs.append(make_keyframe("position", 0.79, 0, 0, 0, "linear"))
        kfs.append(make_keyframe("position", 0.8, -seg_dist * 0.3 * cos_a, 0, -seg_dist * 0.3 * sin_a, "linear"))
        kfs.append(make_keyframe("position", 0.92, 0, 0.4, 0, "linear"))
        kfs.append(make_keyframe("position", 1.05, 0, 0.1, 0, "linear"))
        kfs.append(make_keyframe("position", 1.2, 0, 0, 0, "linear"))
        # Rotation — spiral spin during travel
        kfs.append(make_keyframe("rotation", 0.0, 0, math.degrees(angle), 0, "linear"))
        kfs.append(make_keyframe("rotation", 0.79, 0, math.degrees(angle), 0, "linear"))
        kfs.append(make_keyframe("rotation", 0.85, 15, math.degrees(angle) + 30, 5, "linear"))
        kfs.append(make_keyframe("rotation", 1.0, 20 + seg_idx * 5, math.degrees(angle) + 60, 8, "linear"))
        kfs.append(make_keyframe("rotation", 1.2, 25 + seg_idx * 7, math.degrees(angle) + 75, 10, "linear"))
        spawn_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # RING — hidden until impact, expand rapidly outward
    for (name, bone_uuid, _, ring_idx) in ring_bones:
        kfs = []
        angle = (2 * math.pi * ring_idx) / n_ring
        cos_a = math.cos(angle)
        sin_a = math.sin(angle)
        # Hidden until impact
        kfs.append(make_keyframe("scale", 0.0, 0.0, 0.0, 0.0, "linear"))
        kfs.append(make_keyframe("scale", 0.79, 0.0, 0.0, 0.0, "linear"))
        kfs.append(make_keyframe("scale", 0.8, 0.4, 1.2, 0.4, "linear"))
        kfs.append(make_keyframe("scale", 0.9, 1.5, 1.0, 1.5, "linear"))
        kfs.append(make_keyframe("scale", 1.05, 1.8, 0.7, 1.8, "linear"))
        kfs.append(make_keyframe("scale", 1.2, 2.0, 0.4, 2.0, "linear"))
        # Position — expand outward
        kfs.append(make_keyframe("position", 0.0, 0, 0, 0, "linear"))
        kfs.append(make_keyframe("position", 0.79, 0, 0, 0, "linear"))
        kfs.append(make_keyframe("position", 0.8, cos_a * 0.5, 0, sin_a * 0.5, "linear"))
        kfs.append(make_keyframe("position", 0.95, cos_a * 1.8, 0, sin_a * 1.8, "linear"))
        kfs.append(make_keyframe("position", 1.2, cos_a * 3.5, -0.1, sin_a * 3.5, "linear"))
        spawn_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # TRAIL — falls behind ball, settles after
    for idx, (name, bone_uuid, _, _, base_y) in enumerate(trail_bones):
        kfs = []
        delay = idx * 0.04
        kfs.append(make_keyframe("position", 0.0, 0, 0, 0, "linear"))
        kfs.append(make_keyframe("position", 0.4 + delay, 0, -2, 0, "linear"))
        kfs.append(make_keyframe("position", 0.8 + delay, 0, -base_y - 2 + idx * 0.5, 0, "linear"))
        kfs.append(make_keyframe("position", 0.95 + delay, 0, -base_y - 1 + idx * 0.5, 0, "linear"))
        kfs.append(make_keyframe("position", 1.15 + delay, 0, -base_y + 0.2, 0, "linear"))
        # Wobble
        kfs.append(make_keyframe("rotation", 0.0, 0, 0, 0, "linear"))
        kfs.append(make_keyframe("rotation", 0.5, 20, 30, -25, "linear"))
        kfs.append(make_keyframe("rotation", 0.85, -30, -40, 35, "linear"))
        kfs.append(make_keyframe("rotation", 1.05, 15, 20, -10, "linear"))
        kfs.append(make_keyframe("rotation", 1.2, 0, 0, 0, "linear"))
        spawn_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    b.add_animation("spawn", length=1.2, loop="once", override=True, animators=spawn_animators)

    # ------ IDLE: 4.0s loop, ball rolls, cubes pulse, strands oscillate, ring breathes ------
    idle_animators = {}

    root_kfs = []
    root_kfs.append(make_keyframe("rotation", 0.0, 0, 0, 0, "linear"))
    root_kfs.append(make_keyframe("rotation", 1.0, 5, 90, -3, "linear"))
    root_kfs.append(make_keyframe("rotation", 2.0, 0, 180, 0, "linear"))
    root_kfs.append(make_keyframe("rotation", 3.0, -5, 270, 3, "linear"))
    root_kfs.append(make_keyframe("rotation", 4.0, 0, 360, 0, "linear"))
    root_kfs.append(make_keyframe("position", 0.0, 0, 0, 0, "linear"))
    root_kfs.append(make_keyframe("position", 1.0, 0, 0.3, 0, "linear"))
    root_kfs.append(make_keyframe("position", 2.0, 0, 0, 0, "linear"))
    root_kfs.append(make_keyframe("position", 3.0, 0, 0.2, 0, "linear"))
    root_kfs.append(make_keyframe("position", 4.0, 0, 0, 0, "linear"))
    idle_animators[root_uuid] = {"name": "root", "type": "bone", "keyframes": root_kfs}

    # CORE cubes — subtle pulse waves
    for idx, (name, bone_uuid, _, _) in enumerate(core_bones):
        kfs = []
        phase = idx * (math.pi * 2 / 8)
        for t_step in range(9):
            t = (t_step / 8) * 4.0
            wave = 1.0 + 0.04 * math.sin(2 * math.pi * t / 4.0 + phase)
            kfs.append(make_keyframe("scale", t, wave, wave, wave, "linear"))
        idle_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # WRAP — slow continuous rotation
    for idx, (name, bone_uuid, _, _) in enumerate(wrap_bones):
        kfs = []
        rx0, ry0, rz0 = 0, 0, 0
        if name == "wrap_b": rx0, ry0, rz0 = 0, 45, 30
        elif name == "wrap_c": rx0, ry0, rz0 = 45, 0, 60
        elif name == "wrap_d": rx0, ry0, rz0 = 30, 90, -45
        kfs.append(make_keyframe("rotation", 0.0, rx0, ry0, rz0, "linear"))
        kfs.append(make_keyframe("rotation", 2.0, rx0 + 8, ry0 + 15, rz0 - 10, "linear"))
        kfs.append(make_keyframe("rotation", 4.0, rx0, ry0 + 30, rz0, "linear"))
        idle_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # STRANDS — oscillate at tips (segments further out wobble more)
    for (name, bone_uuid, _, dir_idx, seg_idx) in strand_bones:
        kfs = []
        angle = (2 * math.pi * dir_idx) / n_strands
        amp = 4 + seg_idx * 4  # tips wobble more
        phase = dir_idx * 0.4 + seg_idx * 0.2
        for t_step in range(9):
            t = (t_step / 8) * 4.0
            wob_x = amp * math.sin(2 * math.pi * t / 2.0 + phase)
            wob_z = amp * math.cos(2 * math.pi * t / 2.0 + phase)
            kfs.append(make_keyframe("rotation", t, wob_x, math.degrees(angle), wob_z, "linear"))
        idle_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # RING — slow rotation and breathing scale
    for (name, bone_uuid, _, ring_idx) in ring_bones:
        kfs = []
        angle = (2 * math.pi * ring_idx) / n_ring
        cos_a = math.cos(angle)
        sin_a = math.sin(angle)
        # Position fixed at expanded location
        kfs.append(make_keyframe("position", 0.0, cos_a * 3.5, -0.1, sin_a * 3.5, "linear"))
        kfs.append(make_keyframe("position", 4.0, cos_a * 3.5, -0.1, sin_a * 3.5, "linear"))
        # Breathing scale
        kfs.append(make_keyframe("scale", 0.0, 2.0, 0.4, 2.0, "linear"))
        kfs.append(make_keyframe("scale", 1.0, 2.15, 0.45, 2.15, "linear"))
        kfs.append(make_keyframe("scale", 2.0, 2.0, 0.4, 2.0, "linear"))
        kfs.append(make_keyframe("scale", 3.0, 2.15, 0.45, 2.15, "linear"))
        kfs.append(make_keyframe("scale", 4.0, 2.0, 0.4, 2.0, "linear"))
        # Slow yaw rotation around ring centre
        yaw_base = math.degrees(angle) + 90
        kfs.append(make_keyframe("rotation", 0.0, 0, yaw_base, 0, "linear"))
        kfs.append(make_keyframe("rotation", 2.0, 0, yaw_base + 10, 0, "linear"))
        kfs.append(make_keyframe("rotation", 4.0, 0, yaw_base + 20, 0, "linear"))
        idle_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # TRAIL — settled, gentle sway
    for idx, (name, bone_uuid, _, _, base_y) in enumerate(trail_bones):
        kfs = []
        phase = idx * 0.5
        for t_step in range(9):
            t = (t_step / 8) * 4.0
            wob = 5 * math.sin(2 * math.pi * t / 4.0 + phase)
            kfs.append(make_keyframe("rotation", t, wob, wob * 0.6, -wob * 0.4, "linear"))
        idle_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    b.add_animation("idle", length=4.0, loop="loop", override=False, animators=idle_animators)

    # ------ DISSIPATE: 0.7s, strands retract, ball sinks and shrinks ------
    diss_animators = {}

    root_kfs = []
    root_kfs.append(make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, "linear"))
    root_kfs.append(make_keyframe("scale", 0.2, 1.1, 0.9, 1.1, "linear"))   # squash before sink
    root_kfs.append(make_keyframe("scale", 0.4, 0.7, 0.5, 0.7, "linear"))
    root_kfs.append(make_keyframe("scale", 0.55, 0.3, 0.2, 0.3, "linear"))
    root_kfs.append(make_keyframe("scale", 0.7, 0.0, 0.0, 0.0, "linear"))
    root_kfs.append(make_keyframe("position", 0.0, 0, 0, 0, "linear"))
    root_kfs.append(make_keyframe("position", 0.3, 0, -1, 0, "linear"))
    root_kfs.append(make_keyframe("position", 0.5, 0, -3, 0, "linear"))
    root_kfs.append(make_keyframe("position", 0.7, 0, -6, 0, "linear"))
    root_kfs.append(make_keyframe("rotation", 0.0, 0, 0, 0, "linear"))
    root_kfs.append(make_keyframe("rotation", 0.7, 30, 180, -30, "linear"))
    diss_animators[root_uuid] = {"name": "root", "type": "bone", "keyframes": root_kfs}

    # CORE — shrink chaotically
    for idx, (name, bone_uuid, _, _) in enumerate(core_bones):
        kfs = []
        phase = idx * 0.13
        kfs.append(make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, "linear"))
        kfs.append(make_keyframe("scale", 0.3, 0.9 + 0.05 * math.sin(phase), 0.9, 0.9, "linear"))
        kfs.append(make_keyframe("scale", 0.55, 0.4, 0.4, 0.4, "linear"))
        kfs.append(make_keyframe("scale", 0.7, 0.0, 0.0, 0.0, "linear"))
        kfs.append(make_keyframe("rotation", 0.0, 0, 0, 0, "linear"))
        kfs.append(make_keyframe("rotation", 0.4, 20 * math.cos(phase), 30 * math.sin(phase), 15 * math.cos(phase * 1.3), "linear"))
        kfs.append(make_keyframe("rotation", 0.7, 60 * math.cos(phase), 90 * math.sin(phase), 45 * math.cos(phase * 1.3), "linear"))
        diss_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # WRAP — fast spin while shrinking
    for idx, (name, bone_uuid, _, _) in enumerate(wrap_bones):
        kfs = []
        rx0, ry0, rz0 = 0, 0, 0
        if name == "wrap_b": rx0, ry0, rz0 = 0, 45, 30
        elif name == "wrap_c": rx0, ry0, rz0 = 45, 0, 60
        elif name == "wrap_d": rx0, ry0, rz0 = 30, 90, -45
        kfs.append(make_keyframe("rotation", 0.0, rx0, ry0, rz0, "linear"))
        kfs.append(make_keyframe("rotation", 0.7, rx0 + 360, ry0 + 540, rz0 + 360, "linear"))
        diss_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # STRANDS — retract toward ball rapidly
    for (name, bone_uuid, _, dir_idx, seg_idx) in strand_bones:
        kfs = []
        angle = (2 * math.pi * dir_idx) / n_strands
        # Start at full expanded position
        kfs.append(make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, "linear"))
        kfs.append(make_keyframe("scale", 0.2, 0.8, 0.8, 0.8, "linear"))
        kfs.append(make_keyframe("scale", 0.5, 0.3, 0.3, 0.3, "linear"))
        kfs.append(make_keyframe("scale", 0.7, 0.0, 0.0, 0.0, "linear"))
        # Position retract — pull toward centre
        cos_a = math.cos(angle)
        sin_a = math.sin(angle)
        seg_dist = 1.5 + seg_idx * 1.8
        kfs.append(make_keyframe("position", 0.0, 0, 0, 0, "linear"))
        kfs.append(make_keyframe("position", 0.4, -cos_a * seg_dist * 0.4, seg_idx * 0.5, -sin_a * seg_dist * 0.4, "linear"))
        kfs.append(make_keyframe("position", 0.7, -cos_a * seg_dist * 0.95, seg_idx * 0.8, -sin_a * seg_dist * 0.95, "linear"))
        # Spin during retract
        kfs.append(make_keyframe("rotation", 0.0, 0, math.degrees(angle), 0, "linear"))
        kfs.append(make_keyframe("rotation", 0.7, 60, math.degrees(angle) + 180, 30, "linear"))
        diss_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # RING — expand and snap (vanish suddenly)
    for (name, bone_uuid, _, ring_idx) in ring_bones:
        kfs = []
        angle = (2 * math.pi * ring_idx) / n_ring
        cos_a = math.cos(angle)
        sin_a = math.sin(angle)
        kfs.append(make_keyframe("scale", 0.0, 2.0, 0.4, 2.0, "linear"))
        kfs.append(make_keyframe("scale", 0.3, 2.6, 0.3, 2.6, "linear"))
        kfs.append(make_keyframe("scale", 0.5, 3.2, 0.15, 3.2, "linear"))
        kfs.append(make_keyframe("scale", 0.55, 3.4, 0.1, 3.4, "linear"))
        kfs.append(make_keyframe("scale", 0.6, 0.0, 0.0, 0.0, "linear"))   # snap!
        kfs.append(make_keyframe("scale", 0.7, 0.0, 0.0, 0.0, "linear"))
        kfs.append(make_keyframe("position", 0.0, cos_a * 3.5, -0.1, sin_a * 3.5, "linear"))
        kfs.append(make_keyframe("position", 0.55, cos_a * 5.5, -0.1, sin_a * 5.5, "linear"))
        kfs.append(make_keyframe("position", 0.7, cos_a * 5.5, -0.1, sin_a * 5.5, "linear"))
        diss_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # TRAIL — follow ball downward
    for idx, (name, bone_uuid, _, _, base_y) in enumerate(trail_bones):
        kfs = []
        delay = idx * 0.05
        kfs.append(make_keyframe("position", 0.0, 0, 0, 0, "linear"))
        kfs.append(make_keyframe("position", 0.3 + delay, 0, -2, 0, "linear"))
        kfs.append(make_keyframe("position", 0.7, 0, -base_y - 6, 0, "linear"))
        kfs.append(make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, "linear"))
        kfs.append(make_keyframe("scale", 0.5, 0.6, 0.6, 0.6, "linear"))
        kfs.append(make_keyframe("scale", 0.7, 0.0, 0.0, 0.0, "linear"))
        kfs.append(make_keyframe("rotation", 0.0, 0, 0, 0, "linear"))
        kfs.append(make_keyframe("rotation", 0.4, 25, 30, -20, "linear"))
        kfs.append(make_keyframe("rotation", 0.7, 60, 80, -50, "linear"))
        diss_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    b.add_animation("dissipate", length=0.7, loop="once", override=True, animators=diss_animators)

    # Write file
    out_path = "D:/CC/ChaosCraft/src/main/resources/models/fluffy/me_attacks/yarn_ball_tangled.bbmodel"
    p = b.write(out_path)
    print(f"Wrote {p}")


if __name__ == "__main__":
    build()
