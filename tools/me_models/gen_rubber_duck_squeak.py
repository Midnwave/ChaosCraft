#!/usr/bin/env python3
"""Generator for rubber_duck_squeak.bbmodel — Rubber Duck Launch 'Squeak' attack.

Visual intent: a giant rubber bath duck flying through the air as a projectile,
spinning slightly, squeaking on impact. Body yellow, beak orange, eyes black
with white highlight. Smoothest cleanest texture in any ChaosCraft mode.
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

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/fluffy/me_attacks/rubber_duck_squeak.bbmodel"


# ---------------------------------------------------------------------------
# TEXTURES
# ---------------------------------------------------------------------------

def build_body_texture():
    """Texture 0: rubber duck yellow — clean, flat, smooth.

    Spec: bright rubber yellow base #ffe840, slightly orange-yellow mid #e8c020,
    deep shadow #b09010, bright specular highlight #fff880, emissive #ffd020.
    Smooth rubber, no grain. Wide soft specular cluster centred at (32, 10).
    The "flattest cleanest texture in any ChaosCraft mode" — but still 64x64
    full RGBA so we get plenty of bytes for the final filesize budget.
    """
    w = h = 64
    base = hex_to_rgba("#ffe840")
    mid = hex_to_rgba("#e8c020")
    shadow = hex_to_rgba("#b09010")
    spec = hex_to_rgba("#fff880")
    emiss = hex_to_rgba("#ffd020")
    eye_white = hex_to_rgba("#ffffff")
    eye_black = hex_to_rgba("#101010")

    pixels = [base] * (w * h)

    # Very gentle radial volume wash so the rubber body reads as rounded.
    # Centre stays near base/spec, edges drift toward mid.
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - 32) ** 2 + (y - 32) ** 2)
            t = min(1.0, d / 44.0)
            # Blend toward mid only a tiny amount — keep it clean
            pixels[y * w + x] = color_lerp(base, mid, t * 0.35)

    # Faint shadow at very bottom (gravity pooling) — still smooth
    for y in range(48, h):
        t = (y - 48) / 16.0
        for x in range(w):
            p = pixels[y * w + x]
            pixels[y * w + x] = color_lerp(p, shadow, t * 0.18)

    # Wide soft specular cluster at (32, 10) — the round rubber catch-light.
    # Big falloff radius for the smooth glossy look.
    sx, sy = 32, 10
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - sx) ** 2 + (y - sy) ** 2)
            if d < 16.0:
                t = max(0.0, 1.0 - d / 16.0)
                # Smoothstep-like falloff
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

    # Eye zone — a small region in the corner used for the eye cubes' uniform face.
    # We'll point eye cubes at this rectangle so we get a proper black-with-white-dot eye.
    # Reserve rows 56..63, cols 0..7 for "eye" UV.
    fill_rect(pixels, w, h, 0, 56, 8, 64, eye_black)
    # White highlight dot at top-left of eye square
    fill_rect(pixels, w, h, 1, 57, 4, 60, eye_white)
    # Tiny mid-tone reflection
    fill_rect(pixels, w, h, 5, 60, 7, 62, color_lerp(eye_black, eye_white, 0.45))

    return png_from_pixels(pixels, w, h)


def build_beak_splash_texture():
    """Texture 1: beak (top half) + splash wake (bottom half).

    Spec: bright orange #ff8020 for beak zone, sky blue-white #d8f0ff for
    splash wake zone. Two zones divided horizontally at row 32.
    """
    w = h = 64
    beak_base = hex_to_rgba("#ff8020")
    beak_mid = hex_to_rgba("#e06010")
    beak_deep = hex_to_rgba("#a04808")
    beak_spec = hex_to_rgba("#ffc080")
    beak_emiss = hex_to_rgba("#ffa040")

    splash_base = hex_to_rgba("#d8f0ff")
    splash_deep = hex_to_rgba("#80b8d8")
    splash_spec = hex_to_rgba("#ffffff")
    splash_emiss = hex_to_rgba("#e8f8ff")

    pixels = [beak_base] * (w * h)

    # ----- TOP HALF: BEAK ZONE (rows 0..31) -----
    # Vertical gradient: spec at top → base → slightly deeper at row 32 boundary
    for y in range(0, 32):
        t = y / 31.0
        if t < 0.4:
            tt = t / 0.4
            row_color = color_lerp(beak_spec, beak_base, tt)
        else:
            tt = (t - 0.4) / 0.6
            row_color = color_lerp(beak_base, beak_mid, tt)
        for x in range(w):
            pixels[y * w + x] = row_color

    # Beak specular cluster centred at (32, 8)
    for y in range(0, 32):
        for x in range(w):
            d = math.sqrt((x - 32) ** 2 + (y - 8) ** 2)
            if d < 12.0:
                t = max(0.0, 1.0 - d / 12.0)
                t = t * t * (3 - 2 * t)
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, beak_spec, t * 0.7)

    # Inner emissive on beak
    for y in range(0, 32):
        for x in range(w):
            d = math.sqrt((x - 32) ** 2 + (y - 8) ** 2)
            if d < 4.0:
                t = max(0.0, 1.0 - d / 4.0)
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, beak_emiss, t * 0.5)

    # Subtle deeper crease at the bottom of the beak zone
    for y in range(26, 32):
        t = (y - 26) / 5.0
        for x in range(w):
            p = pixels[y * w + x]
            pixels[y * w + x] = color_lerp(p, beak_deep, t * 0.35)

    # ----- BOTTOM HALF: SPLASH WAKE ZONE (rows 32..63) -----
    # Vertical gradient: spec at top boundary → base → deeper at bottom
    for y in range(32, 64):
        t = (y - 32) / 31.0
        if t < 0.3:
            tt = t / 0.3
            row_color = color_lerp(splash_spec, splash_base, tt)
        else:
            tt = (t - 0.3) / 0.7
            row_color = color_lerp(splash_base, splash_deep, tt)
        for x in range(w):
            pixels[y * w + x] = row_color

    # A few horizontal "ripple lines" for water hint — still clean
    for ry in (40, 48, 56):
        for x in range(w):
            p = pixels[ry * w + x]
            pixels[ry * w + x] = color_lerp(p, splash_deep, 0.22)

    # Splash specular cluster around (32, 40)
    for y in range(32, 64):
        for x in range(w):
            d = math.sqrt((x - 32) ** 2 + (y - 40) ** 2)
            if d < 10.0:
                t = max(0.0, 1.0 - d / 10.0)
                t = t * t * (3 - 2 * t)
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, splash_spec, t * 0.6)

    # Inner emissive on splash
    for y in range(32, 64):
        for x in range(w):
            d = math.sqrt((x - 32) ** 2 + (y - 40) ** 2)
            if d < 4.0:
                t = max(0.0, 1.0 - d / 4.0)
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, splash_emiss, t * 0.55)

    # Boundary line emphasis at row 32 — clean separation
    for x in range(w):
        p = pixels[32 * w + x]
        pixels[32 * w + x] = color_lerp(p, beak_deep, 0.4)

    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# MODEL BUILD
# ---------------------------------------------------------------------------

def build():
    b = Builder("rubber_duck_squeak", resolution=(64, 64), visible_box=(8, 8, 0))

    # ---- TEXTURES ----
    b.add_texture("duck_body", build_body_texture())
    b.add_texture("duck_beak_splash", build_beak_splash_texture())

    # Common face shorthands
    body_face = lambda: uniform_face(0, 0, 56, 56, tex_index=0)  # avoid the eye corner
    eye_face = lambda: uniform_face(0, 56, 8, 64, tex_index=0)   # the dedicated eye square
    beak_face = lambda: uniform_face(0, 0, 64, 32, tex_index=1)  # top half = beak
    splash_face = lambda: uniform_face(0, 32, 64, 64, tex_index=1)  # bottom half = wake

    # =====================================================================
    # 1) DUCK BODY — 5 overlapping cubes forming an egg-like oval.
    #    Largest at centre, smaller at head and tail ends.
    #    Body roughly 3x2x4 units — we use ~6x4x8 BlockBench units (scale 0.5 ~ ME default).
    #    Each cube its own bone so they can fly apart in dissipate.
    # =====================================================================
    body_groups = []  # (group_uuid, group_dict)

    # body_core — biggest central cube
    body_core_cube = b.add_cube(
        "body_core",
        from_=[-3.0, -2.0, -2.0], to_=[3.0, 2.0, 2.0],
        faces=body_face(),
    )
    gu = make_uuid()
    body_groups.append((gu, b.make_group("body_core", [0, 0, 0], [body_core_cube], group_uuid=gu)))

    # body_belly — slightly lower, slightly wider at midline
    body_belly_cube = b.add_cube(
        "body_belly",
        from_=[-2.6, -2.4, -2.2], to_=[2.6, -0.5, 2.2],
        faces=body_face(),
    )
    gu = make_uuid()
    body_groups.append((gu, b.make_group("body_belly", [0, -1.5, 0], [body_belly_cube], group_uuid=gu)))

    # body_back — upper hump for floating-back curve
    body_back_cube = b.add_cube(
        "body_back",
        from_=[-2.4, 0.5, -1.8], to_=[2.0, 2.4, 1.8],
        faces=body_face(),
    )
    gu = make_uuid()
    body_groups.append((gu, b.make_group("body_back", [0, 1.5, 0], [body_back_cube], group_uuid=gu)))

    # body_front — taper toward head
    body_front_cube = b.add_cube(
        "body_front",
        from_=[2.6, -1.4, -1.4], to_=[4.6, 1.4, 1.4],
        faces=body_face(),
    )
    gu = make_uuid()
    body_groups.append((gu, b.make_group("body_front", [3.6, 0, 0], [body_front_cube], group_uuid=gu)))

    # body_rear — taper toward tail
    body_rear_cube = b.add_cube(
        "body_rear",
        from_=[-4.6, -1.2, -1.4], to_=[-2.6, 1.4, 1.4],
        faces=body_face(),
    )
    gu = make_uuid()
    body_groups.append((gu, b.make_group("body_rear", [-3.6, 0, 0], [body_rear_cube], group_uuid=gu)))

    # =====================================================================
    # 2) HEAD — smaller spherical cluster (3 overlapping cubes) attached at
    #    front-top of body. Own bone (single bone for the whole head cluster).
    # =====================================================================
    head_cubes = []
    # head_main
    head_cubes.append(b.add_cube(
        "head_main",
        from_=[4.5, 1.5, -1.5], to_=[7.5, 4.5, 1.5],
        faces=body_face(),
    ))
    # head_top
    head_cubes.append(b.add_cube(
        "head_top",
        from_=[4.8, 3.5, -1.2], to_=[7.2, 5.4, 1.2],
        faces=body_face(),
    ))
    # head_front
    head_cubes.append(b.add_cube(
        "head_front",
        from_=[6.8, 2.0, -1.2], to_=[8.4, 4.0, 1.2],
        faces=body_face(),
    ))
    head_uuid = make_uuid()
    head_group = b.make_group("head", [6.0, 3.0, 0], head_cubes, group_uuid=head_uuid)

    # =====================================================================
    # 3) BEAK — 2 flat cubes forming wedge shape protruding from front of head.
    #    Own bone.
    # =====================================================================
    beak_cubes = []
    # beak_upper — wider, flatter
    beak_cubes.append(b.add_cube(
        "beak_upper",
        from_=[8.0, 2.5, -1.0], to_=[10.5, 3.4, 1.0],
        faces=beak_face(),
    ))
    # beak_lower — narrower, slightly under
    beak_cubes.append(b.add_cube(
        "beak_lower",
        from_=[8.0, 2.0, -0.8], to_=[10.0, 2.6, 0.8],
        faces=beak_face(),
    ))
    beak_uuid = make_uuid()
    beak_group = b.make_group("beak", [9.0, 2.7, 0], beak_cubes, group_uuid=beak_uuid)

    # =====================================================================
    # 4) EYES — 2 small single cubes on either side of head. Each its own bone.
    # =====================================================================
    eye_left_cube = b.add_cube(
        "eye_left",
        from_=[5.6, 3.5, 1.5], to_=[6.4, 4.3, 1.7],
        faces=eye_face(),
    )
    eye_left_uuid = make_uuid()
    eye_left_group = b.make_group("eye_left", [6.0, 3.9, 1.6], [eye_left_cube], group_uuid=eye_left_uuid)

    eye_right_cube = b.add_cube(
        "eye_right",
        from_=[5.6, 3.5, -1.7], to_=[6.4, 4.3, -1.5],
        faces=eye_face(),
    )
    eye_right_uuid = make_uuid()
    eye_right_group = b.make_group("eye_right", [6.0, 3.9, -1.6], [eye_right_cube], group_uuid=eye_right_uuid)

    # =====================================================================
    # 5) TAIL NUB — 1 small cube at back of body angled upward slightly.
    #    Own bone.
    # =====================================================================
    tail_cube = b.add_cube(
        "tail_nub",
        from_=[-5.6, 0.4, -0.9], to_=[-4.4, 2.0, 0.9],
        faces=body_face(),
    )
    tail_uuid = make_uuid()
    tail_group = b.make_group(
        "tail", [-5.0, 1.2, 0], [tail_cube],
        rotation=[0, 0, 25.0],  # angled upward
        group_uuid=tail_uuid,
    )

    # =====================================================================
    # 6) SPLASH WAKE — 6 flat slab pieces trailing behind the duck in V shape.
    #    Wake as it "swims" through air. Each its own bone.
    #    Behind = -X direction. V opens wider as we go further back.
    # =====================================================================
    splash_groups = []
    # Three pairs — each pair (top of V, bottom of V) at increasing distance behind
    splash_specs = [
        # (centre_x_behind, z_offset, scale_factor)
        (-7.0,  1.4, 0.85),   # near pair — closest to duck, tighter
        (-7.0, -1.4, 0.85),
        (-9.5,  2.6, 1.0),    # mid pair
        (-9.5, -2.6, 1.0),
        (-12.0, 3.8, 1.15),   # far pair — widest
        (-12.0, -3.8, 1.15),
    ]
    for i, (sx_, sz_, sc) in enumerate(splash_specs):
        # Flat slab — wide on X, very thin on Y, narrow on Z
        half_x = 1.6 * sc
        half_y = 0.25
        half_z = 0.6 * sc
        cube_uuid = b.add_cube(
            f"splash_slab_{i+1}",
            from_=[-half_x, -half_y, -half_z], to_=[half_x, half_y, half_z],
            faces=splash_face(),
        )
        # Angle each slab so they fan away in V
        # Top-row z>0 slab tilts slightly outward (+Z) and down; bottom-row z<0 mirrors
        rot_y = 15.0 if sz_ > 0 else -15.0
        gu = make_uuid()
        g = b.make_group(
            f"splash_{i+1}", [sx_, -1.2, sz_], [cube_uuid],
            rotation=[0, rot_y, 0], group_uuid=gu,
        )
        splash_groups.append((gu, g))

    # =====================================================================
    # ROOT BONE — contains everything for global motion (Z-axis tumble etc.)
    # =====================================================================
    root_children = []
    for (_uu, g) in body_groups:
        root_children.append(g)
    root_children.append(head_group)
    root_children.append(beak_group)
    root_children.append(eye_left_group)
    root_children.append(eye_right_group)
    root_children.append(tail_group)
    for (_uu, g) in splash_groups:
        root_children.append(g)

    root_uuid = make_uuid()
    root_group = b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid)
    b.outliner.append(root_group)

    # =====================================================================
    # ANIMATIONS
    # =====================================================================

    # ----- SPAWN: 0.3s, override=true -----
    # Duck materialises assembled, already rotating on Z (wobbling as it flies).
    # Splash wake extends behind it.
    spawn_animators = {}

    # Root: starts already in slight rotation, scale up from 0.
    # Dense keyframes for the materialise-already-tumbling feel.
    root_spawn_kfs = []
    spawn_steps = 30
    for k in range(spawn_steps + 1):
        t = (k / spawn_steps) * 0.3
        # Scale: bouncy pop from 0.05 to 1.0
        if t < 0.10:
            tt = t / 0.10
            s = 0.05 + (1.10 - 0.05) * tt
        elif t < 0.20:
            tt = (t - 0.10) / 0.10
            s = 1.10 + (0.98 - 1.10) * tt
        else:
            tt = (t - 0.20) / 0.10
            s = 0.98 + (1.0 - 0.98) * tt
        # Add tiny wobble overlay so it feels rubbery during materialisation
        wobble = 0.02 * math.sin(t * 30.0)
        root_spawn_kfs.append(make_keyframe("scale", t,
                                            x=s + wobble,
                                            y=s - wobble,
                                            z=s + wobble))
    # Already tumbling on Z — full rotation across spawn
    for k in range(spawn_steps + 1):
        t = (k / spawn_steps) * 0.3
        rz = (t / 0.3) * 90.0
        rx = math.sin(t * 20.0) * 3.0
        root_spawn_kfs.append(make_keyframe("rotation", t, x=rx, y=0, z=rz))
    spawn_animators[root_uuid] = {
        "name": "root", "type": "bone", "keyframes": root_spawn_kfs,
    }

    # Splash wake pieces extend behind: scale from 0 to 1 with a stagger,
    # and offset further back as they appear. Dense keyframes for smooth feel.
    for idx, (gu, g) in enumerate(splash_groups):
        stagger = 0.02 * idx
        kfs = []
        for k in range(spawn_steps + 1):
            t = (k / spawn_steps) * 0.3
            # Scale curve
            start_t = 0.05 + stagger
            peak_t = 0.18 + stagger
            if t < start_t:
                s = 0.0
            elif t < peak_t:
                tt = (t - start_t) / (peak_t - start_t)
                s = 1.15 * tt
            else:
                tt = min(1.0, (t - peak_t) / (0.30 - peak_t + 0.001))
                s = 1.15 + (1.0 - 1.15) * tt
            kfs.append(make_keyframe("scale", t, x=s, y=s, z=s))
        for k in range(spawn_steps + 1):
            t = (k / spawn_steps) * 0.3
            # Position: start tucked in at +X then ease back to origin
            peak_t = 0.18 + stagger
            if t < peak_t:
                tt = t / peak_t
                px = 2.0 * (1.0 - tt)
            else:
                px = 0.0
            kfs.append(make_keyframe("position", t, x=px, y=0, z=0))
        spawn_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    # Head, beak, eyes, tail: small pop-in for that "all assembled" hit.
    # Dense keyframes for smooth pop.
    for sub_uuid, sub_name, phase in [
        (head_uuid, "head", 0.0),
        (beak_uuid, "beak", 0.02),
        (eye_left_uuid, "eye_left", 0.04),
        (eye_right_uuid, "eye_right", 0.04),
        (tail_uuid, "tail", 0.06),
    ]:
        kfs = []
        for k in range(spawn_steps + 1):
            t = (k / spawn_steps) * 0.3
            # Scale curve: 0.6 → 1.10 at 0.15+phase → 1.0 at 0.30
            if t < 0.15 + phase:
                tt = t / (0.15 + phase)
                s = 0.6 + (1.10 - 0.6) * tt
            else:
                tt = (t - (0.15 + phase)) / max(0.001, 0.30 - (0.15 + phase))
                s = 1.10 + (1.0 - 1.10) * tt
            # Tiny wobble
            wobble = 0.015 * math.sin(t * 25.0 + phase * 50)
            kfs.append(make_keyframe("scale", t,
                                     x=s + wobble,
                                     y=s,
                                     z=s + wobble))
        # Tail keeps its base rotation
        if sub_name == "tail":
            for k in range(spawn_steps + 1):
                t = (k / spawn_steps) * 0.3
                kfs.append(make_keyframe("rotation", t, x=0, y=0, z=25))
        spawn_animators[sub_uuid] = {
            "name": sub_name, "type": "bone", "keyframes": kfs,
        }

    # Body sub-cubes during spawn — scale pop in sync with root
    for idx, (gu, g) in enumerate(body_groups):
        kfs = []
        ph_off = idx * 0.015
        for k in range(spawn_steps + 1):
            t = (k / spawn_steps) * 0.3
            tt = min(1.0, t / 0.30)
            s = 0.5 + 0.5 * tt
            wobble = 0.02 * math.sin(t * 28.0 + ph_off * 60)
            kfs.append(make_keyframe("scale", t,
                                     x=s + wobble,
                                     y=s - wobble,
                                     z=s + wobble))
        spawn_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    b.add_animation("spawn", length=0.3, animators=spawn_animators,
                    loop="once", override=True)

    # ----- IDLE: 1.5s, override=false, loop -----
    # Duck rotates on Z (forward tumble through air) — about 1 full rotation per loop.
    # Head bone has slight lag behind body rotation. Beak even more lag.
    # Splash wake pieces oscillate behind. Eyes catch the rotation.
    idle_animators = {}

    # Root: 1 full Z rotation across 1.5s, looping cleanly.
    root_idle_kfs = [
        make_keyframe("rotation", 0.00, x=0, y=0, z=0),
        make_keyframe("rotation", 0.375, x=0, y=0, z=90),
        make_keyframe("rotation", 0.75, x=0, y=0, z=180),
        make_keyframe("rotation", 1.125, x=0, y=0, z=270),
        make_keyframe("rotation", 1.50, x=0, y=0, z=360),
        # Subtle bob in flight (Y position breathing)
        make_keyframe("position", 0.00, x=0, y=0, z=0),
        make_keyframe("position", 0.75, x=0, y=0.15, z=0),
        make_keyframe("position", 1.50, x=0, y=0, z=0),
    ]
    idle_animators[root_uuid] = {
        "name": "root", "type": "bone", "keyframes": root_idle_kfs,
    }

    # Head: lags about 12 degrees behind root rotation, in Z (counter-rotates relative).
    # We sin-wobble its local Z so it looks like a delayed lag.
    # Dense keyframes (40 steps) for smooth lag wobble
    head_idle_kfs = []
    steps = 40
    for k in range(steps + 1):
        t = (k / steps) * 1.5
        # Lag wobble: sinusoidal, 1 cycle per loop, peaks at -10° and +10°
        ph = (t / 1.5) * 2 * math.pi
        rz = math.sin(ph) * 8.0
        rx = math.cos(ph) * 3.0
        ry = math.sin(ph * 2) * 1.5
        head_idle_kfs.append(make_keyframe("rotation", t, x=rx, y=ry, z=rz))
    # Position lag too — head bobs as it lags
    for k in range(steps + 1):
        t = (k / steps) * 1.5
        ph = (t / 1.5) * 2 * math.pi
        py = math.sin(ph) * 0.08
        head_idle_kfs.append(make_keyframe("position", t, x=0, y=py, z=0))
    idle_animators[head_uuid] = {
        "name": "head", "type": "bone", "keyframes": head_idle_kfs,
    }

    # Beak: even more lag — phase-shifted, larger amplitude wobble.
    beak_idle_kfs = []
    for k in range(steps + 1):
        t = (k / steps) * 1.5
        ph = (t / 1.5) * 2 * math.pi - 0.6  # phase shift = more lag
        rz = math.sin(ph) * 12.0
        rx = math.cos(ph) * 5.0
        ry = math.sin(ph * 2 - 0.3) * 2.5
        beak_idle_kfs.append(make_keyframe("rotation", t, x=rx, y=ry, z=rz))
    for k in range(steps + 1):
        t = (k / steps) * 1.5
        ph = (t / 1.5) * 2 * math.pi - 0.6
        py = math.sin(ph) * 0.12
        px = math.cos(ph) * 0.06
        beak_idle_kfs.append(make_keyframe("position", t, x=px, y=py, z=0))
    idle_animators[beak_uuid] = {
        "name": "beak", "type": "bone", "keyframes": beak_idle_kfs,
    }

    # Eyes: catch the rotation — small wobble plus position shimmer
    for sub_uuid, sub_name, sign in [
        (eye_left_uuid, "eye_left", 1.0),
        (eye_right_uuid, "eye_right", -1.0),
    ]:
        kfs = []
        for k in range(steps + 1):
            t = (k / steps) * 1.5
            ph = (t / 1.5) * 2 * math.pi
            ry = math.sin(ph) * 4.0 * sign
            rx = math.cos(ph) * 2.0
            rz = math.sin(ph * 2) * 1.0 * sign
            kfs.append(make_keyframe("rotation", t, x=rx, y=ry, z=rz))
        # Tiny scale glint
        for k in range(steps + 1):
            t = (k / steps) * 1.5
            ph = (t / 1.5) * 2 * math.pi
            s = 1.0 + 0.04 * abs(math.sin(ph * 3))
            kfs.append(make_keyframe("scale", t, x=s, y=s, z=s))
        idle_animators[sub_uuid] = {
            "name": sub_name, "type": "bone", "keyframes": kfs,
        }

    # Tail: small flap counter to root rotation
    tail_idle_kfs = []
    for k in range(steps + 1):
        t = (k / steps) * 1.5
        ph = (t / 1.5) * 2 * math.pi
        rz = 25.0 + math.sin(ph) * 6.0  # base 25° (tilt) + wobble
        rx = math.cos(ph) * 2.5
        ry = math.sin(ph * 2) * 4.0
        tail_idle_kfs.append(make_keyframe("rotation", t, x=rx, y=ry, z=rz))
    for k in range(steps + 1):
        t = (k / steps) * 1.5
        ph = (t / 1.5) * 2 * math.pi
        py = math.sin(ph) * 0.1
        tail_idle_kfs.append(make_keyframe("position", t, x=0, y=py, z=0))
    idle_animators[tail_uuid] = {
        "name": "tail", "type": "bone", "keyframes": tail_idle_kfs,
    }

    # Splash wake pieces oscillate behind — Y-position bob and small rotation.
    for idx, (gu, g) in enumerate(splash_groups):
        kfs = []
        period = 0.75 + (idx % 3) * 0.1
        for k in range(steps + 1):
            t = (k / steps) * 1.5
            ph = (t / period + idx * 0.4) * 2 * math.pi
            ry = math.sin(ph) * 12.0
            rx = math.cos(ph * 1.5) * 4.0
            rz = math.sin(ph * 0.7) * 3.0
            kfs.append(make_keyframe("rotation", t, x=rx, y=ry, z=rz))
        for k in range(steps + 1):
            t = (k / steps) * 1.5
            ph = (t / period + idx * 0.4) * 2 * math.pi
            yo = math.sin(ph) * 0.25
            xo = math.cos(ph * 0.6) * 0.15
            zo = math.sin(ph * 1.3) * 0.12
            kfs.append(make_keyframe("position", t, x=xo, y=yo, z=zo))
        # Scale shimmer
        for k in range(steps + 1):
            t = (k / steps) * 1.5
            ph = (t / period + idx * 0.4) * 2 * math.pi
            s = 1.0 + 0.05 * math.sin(ph * 2)
            kfs.append(make_keyframe("scale", t, x=s, y=s, z=s))
        idle_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    # Body sub-cubes: very subtle scale breathing so the egg feels rubbery
    for idx, (gu, g) in enumerate(body_groups):
        kfs = []
        for k in range(steps + 1):
            t = (k / steps) * 1.5
            ph = (t / 1.5 + idx * 0.2) * 2 * math.pi
            s = 1.0 + 0.015 * (1.0 - math.cos(ph))
            kfs.append(make_keyframe("scale", t, x=s, y=s, z=s))
        # Tiny position drift for rubbery jiggle
        for k in range(steps + 1):
            t = (k / steps) * 1.5
            ph = (t / 1.5 + idx * 0.2) * 2 * math.pi
            xo = math.sin(ph) * 0.04
            yo = math.cos(ph * 1.3) * 0.03
            kfs.append(make_keyframe("position", t, x=xo, y=yo, z=0))
        idle_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    b.add_animation("idle", length=1.5, animators=idle_animators,
                    loop="loop", override=False)

    # ----- DISSIPATE: 0.35s, override=true -----
    # SQUEAK — duck compresses (squash: scale Y=0.6, X/Z=1.4 instantaneously).
    # Then explodes outward — each body cube flies in different direction.
    # Head, beak, tail all fly separately. Splash wake continues backward.
    diss_animators = {}

    # Root: at t=0 still rotating; squash on hit at t=0.05; then settle at 0.10;
    # rest of time pieces explode independently (root stays at scale 1).
    root_diss_kfs = [
        make_keyframe("scale", 0.00, x=1.0, y=1.0, z=1.0),
        # SQUEAK squash — instant, two adjacent keyframes for snap
        make_keyframe("scale", 0.04, x=1.0, y=1.0, z=1.0),
        make_keyframe("scale", 0.05, x=1.4, y=0.6, z=1.4, interpolation="linear"),
        make_keyframe("scale", 0.10, x=1.4, y=0.6, z=1.4),
        # Recover slightly so the explosion plays with sane scales
        make_keyframe("scale", 0.18, x=1.15, y=0.85, z=1.15),
        make_keyframe("scale", 0.35, x=1.0, y=1.0, z=1.0),
        # Continue tumbling on Z while exploding
        make_keyframe("rotation", 0.00, x=0, y=0, z=0),
        make_keyframe("rotation", 0.35, x=0, y=0, z=140),
    ]
    diss_animators[root_uuid] = {
        "name": "root", "type": "bone", "keyframes": root_diss_kfs,
    }

    # Body sub-cubes — each flies in a different direction after the squash.
    # Dense keyframes (30 steps) so the explosion looks smooth.
    body_explode_dirs = [
        (0.0, 4.0, 0.0),    # body_core flies up
        (-1.0, -3.5, 1.0),  # body_belly down-left
        (1.5, 3.0, -1.0),   # body_back up-right
        (4.0, 1.5, 1.5),    # body_front forward-up-right
        (-4.5, 1.0, -1.5),  # body_rear back-up-left
    ]
    diss_steps = 30
    for idx, ((gu, g), dirv) in enumerate(zip(body_groups, body_explode_dirs)):
        dx, dy, dz = dirv
        rxe = 180 * (1 if idx % 2 else -1)
        rye = 120 * (1 if idx % 3 else -1)
        rze = 240 * (1 if idx % 2 == 0 else -1)
        kfs = []
        for k in range(diss_steps + 1):
            t = (k / diss_steps) * 0.35
            # Position: held during squash (0..0.05), then ease into explosion
            if t < 0.05:
                px, py, pz = 0.0, 0.0, 0.0
            else:
                # Ease-out trajectory with arc (gravity pull on Y)
                tt = (t - 0.05) / 0.30
                tt2 = 1.0 - (1.0 - tt) ** 2  # ease-out
                px = dx * tt2
                py = dy * tt2 - 1.5 * tt2 * tt2  # gravity arc
                pz = dz * tt2
                # Tumble jitter
                px += math.sin(t * 40.0 + idx) * 0.08
                pz += math.cos(t * 35.0 + idx) * 0.08
            kfs.append(make_keyframe("position", t, x=px, y=py, z=pz))
        for k in range(diss_steps + 1):
            t = (k / diss_steps) * 0.35
            if t < 0.05:
                rx, ry, rz = 0.0, 0.0, 0.0
            else:
                tt = (t - 0.05) / 0.30
                rx = rxe * tt
                ry = rye * tt
                rz = rze * tt
            kfs.append(make_keyframe("rotation", t, x=rx, y=ry, z=rz))
        for k in range(diss_steps + 1):
            t = (k / diss_steps) * 0.35
            # Scale: 1.0 held until 0.20, then ease to 0.1
            if t < 0.20:
                s = 1.0
            else:
                tt = (t - 0.20) / 0.15
                s = 1.0 + (0.1 - 1.0) * tt
            kfs.append(make_keyframe("scale", t, x=s, y=s, z=s))
        diss_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    # Helper: dense fly-apart keyframes for head/beak/eyes/tail
    def fly_apart_kfs(target_pos, target_rot, base_z_rot=0.0, scale_end=0.1):
        dx, dy, dz = target_pos
        rxe, rye, rze = target_rot
        kfs = []
        for k in range(diss_steps + 1):
            t = (k / diss_steps) * 0.35
            if t < 0.05:
                px, py, pz = 0.0, 0.0, 0.0
            else:
                tt = (t - 0.05) / 0.30
                tt2 = 1.0 - (1.0 - tt) ** 2
                px = dx * tt2
                py = dy * tt2 - 1.0 * tt2 * tt2
                pz = dz * tt2
            kfs.append(make_keyframe("position", t, x=px, y=py, z=pz))
        for k in range(diss_steps + 1):
            t = (k / diss_steps) * 0.35
            if t < 0.05:
                rx, ry, rz = 0.0, 0.0, base_z_rot
            else:
                tt = (t - 0.05) / 0.30
                rx = rxe * tt
                ry = rye * tt
                rz = base_z_rot + (rze - base_z_rot) * tt
            kfs.append(make_keyframe("rotation", t, x=rx, y=ry, z=rz))
        for k in range(diss_steps + 1):
            t = (k / diss_steps) * 0.35
            if t < 0.20:
                s = 1.0
            else:
                tt = (t - 0.20) / 0.15
                s = 1.0 + (scale_end - 1.0) * tt
            kfs.append(make_keyframe("scale", t, x=s, y=s, z=s))
        return kfs

    # Head flies up-forward
    diss_animators[head_uuid] = {
        "name": "head", "type": "bone",
        "keyframes": fly_apart_kfs((4.0, 5.0, 1.0), (180, 240, 120)),
    }

    # Beak flies forward fast
    diss_animators[beak_uuid] = {
        "name": "beak", "type": "bone",
        "keyframes": fly_apart_kfs((7.0, 1.5, -0.8), (240, 180, 300)),
    }

    # Eyes pop straight outward to their respective sides
    diss_animators[eye_left_uuid] = {
        "name": "eye_left", "type": "bone",
        "keyframes": fly_apart_kfs((1.5, 2.0, 4.0), (360, 360, 180), scale_end=0.05),
    }
    diss_animators[eye_right_uuid] = {
        "name": "eye_right", "type": "bone",
        "keyframes": fly_apart_kfs((1.5, 2.0, -4.0), (360, 360, 180), scale_end=0.05),
    }

    # Tail flies backward-up (preserves its base 25° tilt)
    diss_animators[tail_uuid] = {
        "name": "tail", "type": "bone",
        "keyframes": fly_apart_kfs((-5.5, 4.0, 1.0), (300, 240, 380), base_z_rot=25.0),
    }

    # Splash wake continues backward — they don't squash; they just keep flying.
    # Dense keyframes for smooth trailing.
    for idx, (gu, g) in enumerate(splash_groups):
        kfs = []
        for k in range(diss_steps + 1):
            t = (k / diss_steps) * 0.35
            tt = t / 0.35
            tt2 = 1.0 - (1.0 - tt) ** 2
            px = -7.0 * tt2
            py = -1.0 * tt2
            pz = math.sin(tt * 8.0 + idx) * 0.4 * tt
            kfs.append(make_keyframe("position", t, x=px, y=py, z=pz))
        for k in range(diss_steps + 1):
            t = (k / diss_steps) * 0.35
            tt = t / 0.35
            ry = 180 * tt
            kfs.append(make_keyframe("rotation", t, x=0, y=ry, z=0))
        for k in range(diss_steps + 1):
            t = (k / diss_steps) * 0.35
            if t < 0.20:
                tt = t / 0.20
                s = 1.0 + (1.2 - 1.0) * tt
            else:
                tt = (t - 0.20) / 0.15
                s = 1.2 + (0.0 - 1.2) * tt
            kfs.append(make_keyframe("scale", t, x=s, y=s, z=s))
        diss_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    b.add_animation("dissipate", length=0.35, animators=diss_animators,
                    loop="once", override=True)

    # ---- WRITE ----
    b.write(OUTPUT_PATH)
    print(f"Wrote {OUTPUT_PATH}")


if __name__ == "__main__":
    build()
