#!/usr/bin/env python3
"""
Generator: kitty_headbutt_love_bump.bbmodel
A cat headbutt scaled up to AOE size — concentric pink shockwave rings,
heart particles, whisker lines, and sparkles. Affection IS the attack.
"""

import sys
import math
import random

sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder,
    make_uuid,
    make_keyframe,
    basic_cube_faces,
    uniform_face,
    png_from_pixels,
    hex_to_rgba,
    fill_rect,
    gradient_radial,
    add_noise_overlay,
    color_lerp,
)

OUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/fluffy/me_attacks/kitty_headbutt_love_bump.bbmodel"


# ----------------------------------------------------------------------------
# TEXTURE 0 — Pastel shimmer (rings, whiskers, sparkles)
# ----------------------------------------------------------------------------
def make_texture_pastel_shimmer():
    W, H = 64, 64
    base = hex_to_rgba("#ffd0e0")
    mid = hex_to_rgba("#f0b0c8")
    shadow = hex_to_rgba("#d890a8")
    highlight = hex_to_rgba("#fff0f8")
    sparkle_white = hex_to_rgba("#ffffff")

    pixels = [base] * (W * H)

    # Soft radial pastel gradient — base across the whole sheet
    gradient_radial(pixels, W, H, 32, 32, 40, base, mid)

    # Iridescent fine shimmer pattern — alternating bright/less-bright every 2 rows
    for y in range(H):
        row_phase = (y // 2) % 2
        for x in range(W):
            curr = pixels[y * W + x]
            if row_phase == 0:
                # bright row — lift toward highlight slightly
                pixels[y * W + x] = color_lerp(curr, highlight, 0.18)
            else:
                # shadow row — push toward mid slightly
                pixels[y * W + x] = color_lerp(curr, shadow, 0.10)

    # Fine vertical shimmer streaks every 4 cols
    for x in range(0, W, 4):
        for y in range(H):
            curr = pixels[y * W + x]
            pixels[y * W + x] = color_lerp(curr, highlight, 0.08)

    # Specular at (6,4) — wide soft falloff radius 5
    spec_cx, spec_cy = 6, 4
    spec_radius = 5
    for y in range(max(0, spec_cy - spec_radius - 1), min(H, spec_cy + spec_radius + 2)):
        for x in range(max(0, spec_cx - spec_radius - 1), min(W, spec_cx + spec_radius + 2)):
            d = math.sqrt((x - spec_cx) ** 2 + (y - spec_cy) ** 2)
            if d <= spec_radius:
                t = 1.0 - (d / spec_radius)
                # Wide soft — ease out so center is bright but falloff is gentle
                strength = t * t * 0.85 + t * 0.15
                pixels[y * W + x] = color_lerp(pixels[y * W + x], sparkle_white, strength)

    # Emissive scattered white pixels — ~1 per 12 pixels density
    rng = random.Random(2024)
    target = (W * H) // 12
    for _ in range(target):
        x = rng.randrange(W)
        y = rng.randrange(H)
        # avoid clobbering specular center too much
        if math.sqrt((x - spec_cx) ** 2 + (y - spec_cy) ** 2) < 2:
            continue
        # small chance — pure white sparkle
        if rng.random() < 0.55:
            pixels[y * W + x] = sparkle_white
        else:
            pixels[y * W + x] = color_lerp(pixels[y * W + x], sparkle_white, 0.7)

    # A handful of larger 2x2 glitter stars
    for _ in range(14):
        sx = rng.randrange(2, W - 2)
        sy = rng.randrange(2, H - 2)
        pixels[sy * W + sx] = sparkle_white
        pixels[sy * W + sx + 1] = color_lerp(pixels[sy * W + sx + 1], sparkle_white, 0.8)
        pixels[(sy + 1) * W + sx] = color_lerp(pixels[(sy + 1) * W + sx], sparkle_white, 0.8)
        # cross arms
        pixels[(sy - 1) * W + sx] = color_lerp(pixels[(sy - 1) * W + sx], sparkle_white, 0.4)
        pixels[sy * W + (sx - 1)] = color_lerp(pixels[sy * W + (sx - 1)], sparkle_white, 0.4)

    return png_from_pixels(pixels, W, H)


# ----------------------------------------------------------------------------
# TEXTURE 1 — Warm nose print (central headbutt mark)
# ----------------------------------------------------------------------------
def make_texture_warm_nose():
    W, H = 64, 64
    base = hex_to_rgba("#ffb8c8")
    centre_warm = hex_to_rgba("#ffcca8")
    ring_detail = hex_to_rgba("#e898b0")
    emissive_centre = hex_to_rgba("#ffd8e8")

    pixels = [base] * (W * H)

    cx, cy = 32, 32
    max_r = 32

    for y in range(H):
        for x in range(W):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            t = min(1.0, d / max_r)

            if t <= 0.25:
                # innermost 25% — warm peach centre, blended to base toward edge of zone
                tt = t / 0.25
                pixels[y * W + x] = color_lerp(centre_warm, base, tt * 0.6)
            elif t <= 0.5:
                # ring detail zone around 50% radius
                tt = (t - 0.25) / 0.25
                # Make a darker ring band peaking near 50%
                ring_strength = math.sin(tt * math.pi)
                pixels[y * W + x] = color_lerp(base, ring_detail, ring_strength * 0.7)
            else:
                # outer — base with subtle outward fade
                tt = (t - 0.5) / 0.5
                pixels[y * W + x] = color_lerp(base, ring_detail, tt * 0.25)

    # Emissive pulse at warm centre — bright pink-white tile
    for y in range(cy - 4, cy + 5):
        for x in range(cx - 4, cx + 5):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            if d <= 4:
                strength = 1.0 - (d / 4)
                pixels[y * W + x] = color_lerp(pixels[y * W + x], emissive_centre, strength * 0.9)

    # Soft scatter of brighter pink dots for "fur impression" texture
    rng = random.Random(7777)
    for _ in range(80):
        x = rng.randrange(W)
        y = rng.randrange(H)
        d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
        if d > max_r:
            continue
        pixels[y * W + x] = color_lerp(pixels[y * W + x], emissive_centre, 0.5)

    # A few darker warmth pockets for depth
    for _ in range(28):
        x = rng.randrange(W)
        y = rng.randrange(H)
        pixels[y * W + x] = color_lerp(pixels[y * W + x], ring_detail, 0.4)

    return png_from_pixels(pixels, W, H)


# ----------------------------------------------------------------------------
# Geometry
# ----------------------------------------------------------------------------
def build():
    b = Builder("kitty_headbutt_love_bump", resolution=(64, 64), visible_box=(6, 6, 0))

    # ----- Central headbutt mark (4 overlapping flat slabs at Y=0.2) -----
    central_disc_uuids = []
    disc_offsets = [
        (0, 0, 0),       # main centre
        (-1.2, 0, 0.8),  # offset NW
        (1.0, 0, -1.0),  # offset SE
        (0.4, 0, 1.4),   # offset N
    ]
    for i, (ox, oy, oz) in enumerate(disc_offsets):
        sx = 2.6 - i * 0.2
        sz = 2.6 - i * 0.2
        cube_uuid = b.add_cube(
            f"central_disc_{i}",
            from_=[ox - sx, 0.2 + oy, oz - sz],
            to_=[ox + sx, 0.5 + oy, oz + sz],
            faces=uniform_face(0, 0, 64, 64, tex_index=1),
        )
        central_disc_uuids.append(cube_uuid)
    central_disc_group_uuid = make_uuid()
    central_disc_group = b.make_group(
        "central_disc",
        origin=[0, 0.2, 0],
        children=central_disc_uuids,
        group_uuid=central_disc_group_uuid,
    )

    # ----- 3 concentric rings of flat horizontal slabs (12 per ring) -----
    def make_ring(name_prefix, radius, slab_w, slab_d, slab_h_base=0.3, slab_thickness=0.25, count=12):
        slab_uuids = []
        for i in range(count):
            angle = (i / count) * math.tau
            cx = radius * math.cos(angle)
            cz = radius * math.sin(angle)
            # Orient slab to face outward — use rotation around Y axis
            rot_y = math.degrees(angle) + 90
            cube_uuid = b.add_cube(
                f"{name_prefix}_slab_{i}",
                from_=[cx - slab_w / 2, slab_h_base, cz - slab_d / 2],
                to_=[cx + slab_w / 2, slab_h_base + slab_thickness, cz + slab_d / 2],
                faces=uniform_face(0, 0, 64, 64, tex_index=0),
                origin=[cx, slab_h_base + slab_thickness / 2, cz],
                rotation=[0, rot_y, 0],
            )
            slab_uuids.append(cube_uuid)
        return slab_uuids

    inner_slabs = make_ring("inner_ring", radius=5, slab_w=2.4, slab_d=0.9, slab_thickness=0.30)
    inner_ring_group_uuid = make_uuid()
    inner_ring_group = b.make_group(
        "inner_ring", origin=[0, 0.3, 0], children=inner_slabs,
        group_uuid=inner_ring_group_uuid,
    )

    mid_slabs = make_ring("mid_ring", radius=9, slab_w=3.2, slab_d=1.1, slab_thickness=0.32)
    mid_ring_group_uuid = make_uuid()
    mid_ring_group = b.make_group(
        "mid_ring", origin=[0, 0.3, 0], children=mid_slabs,
        group_uuid=mid_ring_group_uuid,
    )

    outer_slabs = make_ring("outer_ring", radius=13, slab_w=4.0, slab_d=1.3, slab_thickness=0.35)
    outer_ring_group_uuid = make_uuid()
    outer_ring_group = b.make_group(
        "outer_ring", origin=[0, 0.3, 0], children=outer_slabs,
        group_uuid=outer_ring_group_uuid,
    )

    # ----- Heart particles (8 hearts, each 2 overlapping flat cubes forming + tilted 45°) -----
    heart_groups = []
    heart_group_uuids = []
    rng = random.Random(404)
    for i in range(8):
        # Position above impact at various radii and heights
        ang = (i / 8) * math.tau + 0.3
        hr = 1.5 + (i % 3) * 0.8
        hx = hr * math.cos(ang)
        hz = hr * math.sin(ang)
        hy = 1.5 + (i % 4) * 0.6  # height variance

        # 2 overlapping cubes forming a + shape
        # horizontal bar
        h_horiz = b.add_cube(
            f"heart_{i}_horiz",
            from_=[-0.6, -0.2, -0.2],
            to_=[0.6, 0.2, 0.2],
            faces=uniform_face(0, 0, 64, 64, tex_index=0),
            origin=[0, 0, 0],
        )
        # vertical bar
        h_vert = b.add_cube(
            f"heart_{i}_vert",
            from_=[-0.2, -0.6, -0.2],
            to_=[0.2, 0.6, 0.2],
            faces=uniform_face(0, 0, 64, 64, tex_index=0),
            origin=[0, 0, 0],
        )

        heart_group_uuid = make_uuid()
        # Tilt the entire group 45° around Z axis
        heart_group = b.make_group(
            f"heart_{i}",
            origin=[hx, hy, hz],
            children=[h_horiz, h_vert],
            rotation=[0, rng.uniform(0, 360), 45],
            group_uuid=heart_group_uuid,
        )
        heart_groups.append(heart_group)
        heart_group_uuids.append(heart_group_uuid)

    # ----- Whisker lines (6 long thin flat slabs radiating from centre at ground) -----
    whisker_groups = []
    whisker_group_uuids = []
    for i in range(6):
        ang = (i / 6) * math.tau
        rot_y = math.degrees(ang)
        # The slab extends from centre outward along +X (after rotation)
        w_cube = b.add_cube(
            f"whisker_{i}_slab",
            from_=[0.5, 0.05, -0.08],
            to_=[6.5, 0.18, 0.08],
            faces=uniform_face(0, 0, 64, 64, tex_index=0),
            origin=[0, 0.1, 0],
        )
        wgrp_uuid = make_uuid()
        wgrp = b.make_group(
            f"whisker_{i}",
            origin=[0, 0.1, 0],
            children=[w_cube],
            rotation=[0, rot_y, 0],
            group_uuid=wgrp_uuid,
        )
        whisker_groups.append(wgrp)
        whisker_group_uuids.append(wgrp_uuid)

    # ----- Sparkle accents (10 tiny cubes at various heights around outer ring) -----
    sparkle_groups = []
    sparkle_group_uuids = []
    rng2 = random.Random(909)
    SPARKLE_COUNT = 10
    for i in range(SPARKLE_COUNT):
        ang = (i / SPARKLE_COUNT) * math.tau + rng2.uniform(-0.2, 0.2)
        sr = 13 + rng2.uniform(-1.5, 1.5)
        sx = sr * math.cos(ang)
        sz = sr * math.sin(ang)
        sy = 0.6 + rng2.uniform(0, 2.4)
        s_cube = b.add_cube(
            f"sparkle_{i}_cube",
            from_=[-0.25, -0.25, -0.25],
            to_=[0.25, 0.25, 0.25],
            faces=uniform_face(0, 0, 64, 64, tex_index=0),
            origin=[0, 0, 0],
        )
        sgrp_uuid = make_uuid()
        sgrp = b.make_group(
            f"sparkle_{i}",
            origin=[sx, sy, sz],
            children=[s_cube],
            group_uuid=sgrp_uuid,
        )
        sparkle_groups.append(sgrp)
        sparkle_group_uuids.append(sgrp_uuid)

    # ----- Root outliner -----
    root_children = [
        central_disc_group,
        inner_ring_group,
        mid_ring_group,
        outer_ring_group,
    ]
    root_children.extend(heart_groups)
    root_children.extend(whisker_groups)
    root_children.extend(sparkle_groups)
    b.add_root_group("root", [0, 0, 0], children=root_children)

    # ----- Textures -----
    b.add_texture("kitty_pastel_shimmer", make_texture_pastel_shimmer())
    b.add_texture("kitty_warm_nose", make_texture_warm_nose())

    # ----- Animations -----
    add_animations(
        b,
        central_disc_group_uuid=central_disc_group_uuid,
        inner_ring_group_uuid=inner_ring_group_uuid,
        mid_ring_group_uuid=mid_ring_group_uuid,
        outer_ring_group_uuid=outer_ring_group_uuid,
        heart_group_uuids=heart_group_uuids,
        whisker_group_uuids=whisker_group_uuids,
        sparkle_group_uuids=sparkle_group_uuids,
    )

    return b


# ----------------------------------------------------------------------------
# Animations
# ----------------------------------------------------------------------------
def add_animations(b, *, central_disc_group_uuid, inner_ring_group_uuid,
                   mid_ring_group_uuid, outer_ring_group_uuid,
                   heart_group_uuids, whisker_group_uuids, sparkle_group_uuids):

    # ============================================================
    # SPAWN — once, 0.7s, override=true
    # ============================================================
    spawn_animators = {}

    # Central disc — slams in from Y+0 at 0.1s with scale overshoot
    spawn_animators[central_disc_group_uuid] = {
        "name": "central_disc", "type": "bone",
        "keyframes": [
            make_keyframe("scale", 0.0, 0.0, 0.0, 0.0),
            make_keyframe("scale", 0.05, 0.0, 0.0, 0.0),
            make_keyframe("position", 0.0, 0, 6, 0),
            make_keyframe("position", 0.10, 0, 0, 0),
            make_keyframe("scale", 0.10, 1.4, 1.4, 1.4),
            make_keyframe("scale", 0.20, 1.0, 0.85, 1.0),
            make_keyframe("scale", 0.32, 1.05, 1.0, 1.05),
            make_keyframe("scale", 0.50, 1.0, 1.0, 1.0),
            make_keyframe("scale", 0.70, 1.0, 1.0, 1.0),
            make_keyframe("position", 0.70, 0, 0, 0),
        ],
    }

    # Inner ring — start scale 0, expand at 0.1s
    spawn_animators[inner_ring_group_uuid] = {
        "name": "inner_ring", "type": "bone",
        "keyframes": [
            make_keyframe("scale", 0.0, 0.0, 0.0, 0.0),
            make_keyframe("scale", 0.10, 0.0, 0.0, 0.0),
            make_keyframe("scale", 0.30, 1.15, 1.0, 1.15),
            make_keyframe("scale", 0.50, 1.0, 1.0, 1.0),
            make_keyframe("scale", 0.70, 1.0, 1.0, 1.0),
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 0.70, 0, 12, 0),
        ],
    }

    # Mid ring — expand at 0.2s
    spawn_animators[mid_ring_group_uuid] = {
        "name": "mid_ring", "type": "bone",
        "keyframes": [
            make_keyframe("scale", 0.0, 0.0, 0.0, 0.0),
            make_keyframe("scale", 0.20, 0.0, 0.0, 0.0),
            make_keyframe("scale", 0.40, 1.18, 1.0, 1.18),
            make_keyframe("scale", 0.60, 1.0, 1.0, 1.0),
            make_keyframe("scale", 0.70, 1.0, 1.0, 1.0),
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 0.70, 0, -8, 0),
        ],
    }

    # Outer ring — expand at 0.3s
    spawn_animators[outer_ring_group_uuid] = {
        "name": "outer_ring", "type": "bone",
        "keyframes": [
            make_keyframe("scale", 0.0, 0.0, 0.0, 0.0),
            make_keyframe("scale", 0.30, 0.0, 0.0, 0.0),
            make_keyframe("scale", 0.50, 1.20, 1.0, 1.20),
            make_keyframe("scale", 0.65, 1.0, 1.0, 1.0),
            make_keyframe("scale", 0.70, 1.0, 1.0, 1.0),
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 0.70, 0, 6, 0),
        ],
    }

    # Hearts blast upward from centre at 0.2s, staggered
    for i, h_uuid in enumerate(heart_group_uuids):
        delay = 0.20 + i * 0.025  # staggered start
        spawn_animators[h_uuid] = {
            "name": f"heart_{i}", "type": "bone",
            "keyframes": [
                make_keyframe("scale", 0.0, 0.0, 0.0, 0.0),
                make_keyframe("scale", delay, 0.0, 0.0, 0.0),
                make_keyframe("scale", delay + 0.08, 1.5, 1.5, 1.5),
                make_keyframe("scale", delay + 0.18, 1.0, 1.0, 1.0),
                make_keyframe("scale", 0.70, 1.0, 1.0, 1.0),
                # blast upward — relative offset
                make_keyframe("position", 0.0, 0, -2.0, 0),
                make_keyframe("position", delay, 0, -2.0, 0),
                make_keyframe("position", delay + 0.18, 0, 0.5, 0),
                make_keyframe("position", 0.70, 0, 0, 0),
                make_keyframe("rotation", 0.0, 0, 0, 0),
                make_keyframe("rotation", 0.70, 0, 90, 0),
            ],
        }

    # Whisker lines extend outward from centre at 0.15s
    for i, w_uuid in enumerate(whisker_group_uuids):
        delay = 0.15 + i * 0.015
        spawn_animators[w_uuid] = {
            "name": f"whisker_{i}", "type": "bone",
            "keyframes": [
                make_keyframe("scale", 0.0, 0.0, 1.0, 1.0),
                make_keyframe("scale", delay, 0.0, 1.0, 1.0),
                make_keyframe("scale", delay + 0.20, 1.0, 1.0, 1.0),
                make_keyframe("scale", 0.70, 1.0, 1.0, 1.0),
            ],
        }

    # Sparkles scatter at 0.3s
    for i, s_uuid in enumerate(sparkle_group_uuids):
        delay = 0.30 + (i % 5) * 0.02
        spawn_animators[s_uuid] = {
            "name": f"sparkle_{i}", "type": "bone",
            "keyframes": [
                make_keyframe("scale", 0.0, 0.0, 0.0, 0.0),
                make_keyframe("scale", delay, 0.0, 0.0, 0.0),
                make_keyframe("scale", delay + 0.10, 1.6, 1.6, 1.6),
                make_keyframe("scale", delay + 0.25, 1.0, 1.0, 1.0),
                make_keyframe("scale", 0.70, 1.0, 1.0, 1.0),
                make_keyframe("rotation", 0.0, 0, 0, 0),
                make_keyframe("rotation", 0.70, 360, 360, 0),
            ],
        }

    b.add_animation("spawn", length=0.7, animators=spawn_animators, loop="once", override=True)

    # ============================================================
    # IDLE — loop, 4.0s, override=false
    # ============================================================
    idle_animators = {}

    # Central disc — soft pulse
    idle_animators[central_disc_group_uuid] = {
        "name": "central_disc", "type": "bone",
        "keyframes": [
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
            make_keyframe("scale", 1.0, 1.08, 0.92, 1.08),
            make_keyframe("scale", 2.0, 1.0, 1.0, 1.0),
            make_keyframe("scale", 3.0, 1.05, 0.95, 1.05),
            make_keyframe("scale", 4.0, 1.0, 1.0, 1.0),
        ],
    }

    # Inner ring — slow rotation (fastest of the three)
    idle_animators[inner_ring_group_uuid] = {
        "name": "inner_ring", "type": "bone",
        "keyframes": [
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 1.0, 0, 25, 0),
            make_keyframe("rotation", 2.0, 0, 50, 0),
            make_keyframe("rotation", 3.0, 0, 75, 0),
            make_keyframe("rotation", 4.0, 0, 100, 0),
        ],
    }

    # Mid ring — slower
    idle_animators[mid_ring_group_uuid] = {
        "name": "mid_ring", "type": "bone",
        "keyframes": [
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 1.0, 0, -15, 0),
            make_keyframe("rotation", 2.0, 0, -30, 0),
            make_keyframe("rotation", 3.0, 0, -45, 0),
            make_keyframe("rotation", 4.0, 0, -60, 0),
        ],
    }

    # Outer ring — slowest
    idle_animators[outer_ring_group_uuid] = {
        "name": "outer_ring", "type": "bone",
        "keyframes": [
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 1.0, 0, 8, 0),
            make_keyframe("rotation", 2.0, 0, 16, 0),
            make_keyframe("rotation", 3.0, 0, 24, 0),
            make_keyframe("rotation", 4.0, 0, 32, 0),
        ],
    }

    # Hearts bob upward and downward on independent periods
    for i, h_uuid in enumerate(heart_group_uuids):
        period = 1.6 + (i % 4) * 0.5  # 1.6s..3.1s
        phase = (i / 8) * math.tau
        # build bob positions across 4s — dense sampling
        kfs = []
        for t_step in range(17):  # 0, 0.25, 0.5, ..., 4.0
            t = t_step * 0.25
            y_off = math.sin((t / period) * math.tau + phase) * 0.6
            x_off = math.cos((t / (period * 1.3)) * math.tau + phase) * 0.25
            kfs.append(make_keyframe("position", t, x_off, y_off, x_off * 0.5))
        # subtle scale shimmer — dense
        for t_step in range(9):
            t = t_step * 0.5
            s = 1.0 + math.sin((t / 2.0) * math.tau + phase) * 0.1
            kfs.append(make_keyframe("scale", t, s, s, s))
        # subtle rotation drift — dense samples for slow swirl
        for t_step in range(9):
            t = t_step * 0.5
            ry = (t / 4.0) * (30 + i * 5)
            rx = math.sin((t / 2.0) * math.tau + phase) * 5
            kfs.append(make_keyframe("rotation", t, rx, ry, 0))
        idle_animators[h_uuid] = {
            "name": f"heart_{i}", "type": "bone",
            "keyframes": kfs,
        }

    # Whisker lines — very subtly flex (tiny scale wobble) — dense sampling
    for i, w_uuid in enumerate(whisker_group_uuids):
        phase = (i / 6) * math.tau
        kfs = []
        for t_step in range(17):
            t = t_step * 0.25
            wobble = 1.0 + math.sin((t / 2.0) * math.tau + phase) * 0.04
            kfs.append(make_keyframe("scale", t, wobble, 1.0, 1.0))
        # subtle rotation flex
        for t_step in range(9):
            t = t_step * 0.5
            ry_flex = math.sin((t / 4.0) * math.tau + phase) * 1.5
            kfs.append(make_keyframe("rotation", t, 0, ry_flex, 0))
        idle_animators[w_uuid] = {
            "name": f"whisker_{i}", "type": "bone",
            "keyframes": kfs,
        }

    # Sparkles — drift upward slowly in looping Y positions, dense sampling
    for i, s_uuid in enumerate(sparkle_group_uuids):
        phase_offset = (i / 10) * 4.0  # start at different points in 4s loop
        kfs = []
        for t_step in range(17):
            t = t_step * 0.25
            phase = ((t + phase_offset) / 4.0) * math.tau
            y_off = math.sin(phase) * 0.5 + math.cos(phase * 0.5) * 0.3
            x_off = math.sin(phase * 0.7) * 0.2
            z_off = math.cos(phase * 0.7) * 0.2
            kfs.append(make_keyframe("position", t, x_off, y_off, z_off))
        # twinkle scale
        for t_step in range(9):
            t = t_step * 0.5
            s = 1.0 + math.sin((t / 1.0) * math.tau + (i * 0.7)) * 0.25
            kfs.append(make_keyframe("scale", t, s, s, s))
        # spin rotation
        for t_step in range(5):
            t = t_step * 1.0
            kfs.append(make_keyframe("rotation", t, t * 90, t * 60, 0))
        idle_animators[s_uuid] = {
            "name": f"sparkle_{i}", "type": "bone",
            "keyframes": kfs,
        }

    # Rings — denser rotation samples for smoother visible spin in editor
    inner_kfs = []
    for t_step in range(9):
        t = t_step * 0.5
        inner_kfs.append(make_keyframe("rotation", t, 0, t * 25, 0))
        # subtle vertical bob too
        inner_kfs.append(make_keyframe("position", t, 0, math.sin((t / 4.0) * math.tau) * 0.05, 0))
    idle_animators[inner_ring_group_uuid]["keyframes"] = inner_kfs

    mid_kfs = []
    for t_step in range(9):
        t = t_step * 0.5
        mid_kfs.append(make_keyframe("rotation", t, 0, -t * 15, 0))
        mid_kfs.append(make_keyframe("position", t, 0, math.cos((t / 4.0) * math.tau) * 0.04, 0))
    idle_animators[mid_ring_group_uuid]["keyframes"] = mid_kfs

    outer_kfs = []
    for t_step in range(9):
        t = t_step * 0.5
        outer_kfs.append(make_keyframe("rotation", t, 0, t * 8, 0))
        outer_kfs.append(make_keyframe("position", t, 0, math.sin((t / 4.0) * math.tau + 1.0) * 0.03, 0))
    idle_animators[outer_ring_group_uuid]["keyframes"] = outer_kfs

    b.add_animation("idle", length=4.0, animators=idle_animators, loop="loop", override=False)

    # ============================================================
    # DISSIPATE — once, 0.5s, override=true
    # ============================================================
    diss_animators = {}

    # Central disc — brief brightness pulse first then scale to 0
    diss_animators[central_disc_group_uuid] = {
        "name": "central_disc", "type": "bone",
        "keyframes": [
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
            make_keyframe("scale", 0.10, 1.4, 1.0, 1.4),  # bright pulse expand
            make_keyframe("scale", 0.30, 0.6, 0.4, 0.6),
            make_keyframe("scale", 0.50, 0.0, 0.0, 0.0),
        ],
    }

    # Inner ring — expand outward and snap
    diss_animators[inner_ring_group_uuid] = {
        "name": "inner_ring", "type": "bone",
        "keyframes": [
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
            make_keyframe("scale", 0.25, 1.4, 0.6, 1.4),
            make_keyframe("scale", 0.45, 1.7, 0.2, 1.7),
            make_keyframe("scale", 0.50, 0.0, 0.0, 0.0),
        ],
    }

    # Mid ring — expand outward and snap
    diss_animators[mid_ring_group_uuid] = {
        "name": "mid_ring", "type": "bone",
        "keyframes": [
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
            make_keyframe("scale", 0.25, 1.35, 0.5, 1.35),
            make_keyframe("scale", 0.45, 1.65, 0.15, 1.65),
            make_keyframe("scale", 0.50, 0.0, 0.0, 0.0),
        ],
    }

    # Outer ring — expand outward and snap
    diss_animators[outer_ring_group_uuid] = {
        "name": "outer_ring", "type": "bone",
        "keyframes": [
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
            make_keyframe("scale", 0.25, 1.30, 0.4, 1.30),
            make_keyframe("scale", 0.45, 1.60, 0.10, 1.60),
            make_keyframe("scale", 0.50, 0.0, 0.0, 0.0),
        ],
    }

    # Hearts drift upward and vanish
    for i, h_uuid in enumerate(heart_group_uuids):
        delay = i * 0.012
        diss_animators[h_uuid] = {
            "name": f"heart_{i}", "type": "bone",
            "keyframes": [
                make_keyframe("position", 0.0, 0, 0, 0),
                make_keyframe("position", 0.30, 0, 2.5, 0),
                make_keyframe("position", 0.50, 0, 5.0, 0),
                make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
                make_keyframe("scale", 0.30 + delay, 1.2, 1.2, 1.2),
                make_keyframe("scale", 0.50, 0.0, 0.0, 0.0),
                make_keyframe("rotation", 0.0, 0, 0, 0),
                make_keyframe("rotation", 0.50, 0, 180, 0),
            ],
        }

    # Whiskers retract toward centre (X scale to 0)
    for i, w_uuid in enumerate(whisker_group_uuids):
        diss_animators[w_uuid] = {
            "name": f"whisker_{i}", "type": "bone",
            "keyframes": [
                make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
                make_keyframe("scale", 0.30, 0.5, 1.0, 1.0),
                make_keyframe("scale", 0.50, 0.0, 1.0, 1.0),
            ],
        }

    # Sparkles scatter and vanish
    for i, s_uuid in enumerate(sparkle_group_uuids):
        delay = (i % 5) * 0.015
        diss_animators[s_uuid] = {
            "name": f"sparkle_{i}", "type": "bone",
            "keyframes": [
                make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
                make_keyframe("scale", 0.15 + delay, 1.8, 1.8, 1.8),
                make_keyframe("scale", 0.50, 0.0, 0.0, 0.0),
                # outward scatter (relative to bone origin)
                make_keyframe("position", 0.0, 0, 0, 0),
                make_keyframe("position", 0.50, 0, 3.0, 0),
                make_keyframe("rotation", 0.0, 0, 0, 0),
                make_keyframe("rotation", 0.50, 720, 360, 0),
            ],
        }

    b.add_animation("dissipate", length=0.5, animators=diss_animators, loop="once", override=True)


# ----------------------------------------------------------------------------
def main():
    b = build()
    b.write(OUT_PATH)
    import os
    print(f"WROTE {OUT_PATH}")
    print(f"size_kb {os.path.getsize(OUT_PATH) // 1024}")
    print(f"elements {len(b.elements)}")
    print(f"bones (root + children) {len(b.outliner[0]['children'])}")


if __name__ == "__main__":
    main()
