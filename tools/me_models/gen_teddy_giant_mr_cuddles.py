#!/usr/bin/env python3
"""
Generate teddy_giant_mr_cuddles.bbmodel — Fluffy Mode ME summon.

A GIANT classic teddy bear ("Mr. Cuddles") materialising at twice player height.
Honey-brown plush, cream belly, red ribbon bow at neck, x-stitch eyes (loved-to-pieces).

Geometry summary (all bones, all cubes have origin):
  - 3 torso cubes  (each own bone)
  - 3 head cubes   (each own bone)
  - 2 ears x 2 cubes each (each cube own bone)
  - 2 arms: upper x 2 + lower x 2 cubes each (each segment own bone)
  - 2 legs x 2 cubes each (each cube own bone)
  - 1 nose cube
  - 2 eye-x bones, each containing 2 crossing slab cubes (rotated 45°)
  - 3 bow cubes (left loop, knot, right loop) each own bone
  - 2 belly patch slabs (each own bone) on cream texture

Animations:
  - spawn (once, 1.5s, override=true) — materialises feet first up to head/eyes/bow
  - idle  (loop, 5.0s, override=false) — breathing, head tilt, ear twitch, bow sway
  - dissipate (once, 1.0s, override=true) — hug inward, sink backward into ground
"""
import sys
import math
sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder, make_uuid, make_keyframe, basic_cube_faces, uniform_face,
    png_from_pixels, hex_to_rgba, fill_rect, color_lerp, lerp,
    add_noise_overlay,
)

OUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/fluffy/me_attacks/teddy_giant_mr_cuddles.bbmodel"


# ============================================================
# TEXTURES
# ============================================================

def make_body_texture():
    """Texture 0 (64x64): classic warm honey-brown plush teddy fabric."""
    W = H = 64
    base         = hex_to_rgba("#c89060")
    plush_shadow = hex_to_rgba("#906840")
    seam_dark    = hex_to_rgba("#604830")
    plush_high   = hex_to_rgba("#e8b070")
    specular     = hex_to_rgba("#ffd898")
    emissive     = hex_to_rgba("#ffb040")

    pixels = [base] * (W * H)

    # Soft tonal variation (top/center brighter, edges shadowed)
    for y in range(H):
        for x in range(W):
            t_v = y / (H - 1)
            t_h = abs(x - W / 2) / (W / 2)
            tone = (1.0 - t_v) * 0.55 + (1.0 - t_h) * 0.30
            if tone > 0.55:
                pixels[y * W + x] = color_lerp(base, plush_high, (tone - 0.55) * 1.7)
            elif tone < 0.30:
                pixels[y * W + x] = color_lerp(base, plush_shadow, (0.30 - tone) * 2.0)

    # Fine crosshatch fabric weave every 4 rows + every 4 columns
    for y in range(0, H, 4):
        for x in range(W):
            cur = pixels[y * W + x]
            pixels[y * W + x] = color_lerp(cur, seam_dark, 0.30)
    for x in range(0, W, 4):
        for y in range(H):
            cur = pixels[y * W + x]
            pixels[y * W + x] = color_lerp(cur, seam_dark, 0.30)

    # Plush noise — warm fluff
    add_noise_overlay(pixels, W, H, plush_shadow, plush_high, density=0.13, seed=70707)

    # Specular dabs in upper-left of each quadrant
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

    # 2px emissive warm-orange dots at brightest zones (4 spots, 2x2 each)
    for cy, cx in [(7, 11), (7, 45), (39, 11), (39, 45)]:
        for dy in range(0, 2):
            for dx in range(0, 2):
                px = cx + dx
                py = cy + dy
                if 0 <= px < W and 0 <= py < H:
                    pixels[py * W + px] = emissive

    # Faint diagonal stitching seams — give plush a hand-sewn feel
    for i in range(W):
        y = (i + 6) % H
        x = i
        cur = pixels[y * W + x]
        pixels[y * W + x] = color_lerp(cur, seam_dark, 0.50)
        y2 = (H - 1 - i + 6) % H
        cur2 = pixels[y2 * W + x]
        pixels[y2 * W + x] = color_lerp(cur2, seam_dark, 0.40)

    return png_from_pixels(pixels, W, H)


def make_belly_ribbon_texture():
    """Texture 1 (64x64): cream belly (top half) + red ribbon (bottom half)."""
    W = H = 64
    cream_base   = hex_to_rgba("#fff8e8")
    cream_shadow = hex_to_rgba("#e8d8b8")
    cream_high   = hex_to_rgba("#fffcf2")
    ribbon_base  = hex_to_rgba("#e04040")
    ribbon_dark  = hex_to_rgba("#a02020")
    ribbon_high  = hex_to_rgba("#ff7060")
    seam         = hex_to_rgba("#8a4040")

    pixels = [cream_base] * (W * H)

    # ========== Top half: cream belly ==========
    for y in range(0, 32):
        for x in range(W):
            t_v = y / 31.0
            t_h = abs(x - W / 2) / (W / 2)
            # subtle radial-ish gradient: brightest top center
            tone = (1.0 - t_v) * 0.6 + (1.0 - t_h) * 0.3
            if tone > 0.55:
                pixels[y * W + x] = color_lerp(cream_base, cream_high, (tone - 0.55) * 1.5)
            elif tone < 0.30:
                pixels[y * W + x] = color_lerp(cream_base, cream_shadow, (0.30 - tone) * 1.8)

    # cream noise in top half
    for y in range(0, 32):
        for x in range(W):
            if (x * 7 + y * 13) % 17 == 3:
                cur = pixels[y * W + x]
                pixels[y * W + x] = color_lerp(cur, cream_shadow, 0.4)
            elif (x * 11 + y * 5) % 19 == 1:
                cur = pixels[y * W + x]
                pixels[y * W + x] = color_lerp(cur, cream_high, 0.5)

    # ========== Bottom half: ribbon red ==========
    fill_rect(pixels, W, H, 0, 32, W, H, ribbon_base)

    for y in range(32, H):
        for x in range(W):
            t_v = (y - 32) / 31.0
            t_h = abs(x - W / 2) / (W / 2)
            tone = (1.0 - t_v) * 0.55 + (1.0 - t_h) * 0.30
            if tone > 0.55:
                pixels[y * W + x] = color_lerp(ribbon_base, ribbon_high, (tone - 0.55) * 1.6)
            elif tone < 0.30:
                pixels[y * W + x] = color_lerp(ribbon_base, ribbon_dark, (0.30 - tone) * 2.0)

    # ribbon weave: every 4 cols/rows in bottom half
    for y in range(32, H, 4):
        for x in range(W):
            cur = pixels[y * W + x]
            pixels[y * W + x] = color_lerp(cur, seam, 0.3)
    for x in range(0, W, 4):
        for y in range(32, H):
            cur = pixels[y * W + x]
            pixels[y * W + x] = color_lerp(cur, seam, 0.3)

    # specular ribbon shine in bottom half
    for cy, cx in [(38, 12), (38, 50), (52, 12), (52, 50)]:
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                d = math.sqrt(dy * dy + dx * dx)
                if d <= 2.0:
                    px = cx + dx
                    py = cy + dy
                    if 0 <= px < W and 32 <= py < H:
                        cur = pixels[py * W + px]
                        pixels[py * W + px] = color_lerp(cur, ribbon_high, 1.0 - d / 2.0)

    return png_from_pixels(pixels, W, H)


# ============================================================
# BUILD
# ============================================================

def build():
    b = Builder("teddy_giant_mr_cuddles", resolution=(64, 64), visible_box=(8, 8, 0))

    b.add_texture("teddy_body", make_body_texture())
    b.add_texture("teddy_belly_bow", make_belly_ribbon_texture())

    # ----------------------------------------------------------------
    # Scale: ~2x player height. Player = 1.8m = 28.8 units. We aim ~36 units tall.
    # Origin (0,0,0) is between feet on ground level. +Y up.
    # ----------------------------------------------------------------

    # ---------------- BODY (3 overlapping cubes -> round barrel) ----------------
    # Each cube own bone.
    body_lower_uuid = b.add_cube(
        "body_lower",
        from_=[-7.0, 10.0, -5.5],
        to_=[7.0, 18.0, 5.5],
        faces=basic_cube_faces(0, 0, 14, 8, 11, tex_index=0),
        origin=[0.0, 14.0, 0.0],
    )
    body_mid_uuid = b.add_cube(
        "body_mid",
        from_=[-8.0, 14.0, -6.0],
        to_=[8.0, 22.0, 6.0],
        faces=basic_cube_faces(0, 16, 16, 8, 12, tex_index=0),
        origin=[0.0, 18.0, 0.0],
    )
    body_upper_uuid = b.add_cube(
        "body_upper",
        from_=[-6.5, 19.0, -5.0],
        to_=[6.5, 25.0, 5.0],
        faces=basic_cube_faces(20, 32, 13, 6, 10, tex_index=0),
        origin=[0.0, 22.0, 0.0],
    )

    body_lower_bone = make_uuid()
    body_lower_grp = b.make_group(
        "bone_body_lower", origin=[0.0, 14.0, 0.0],
        children=[body_lower_uuid], group_uuid=body_lower_bone,
    )
    body_mid_bone = make_uuid()
    body_mid_grp = b.make_group(
        "bone_body_mid", origin=[0.0, 18.0, 0.0],
        children=[body_mid_uuid], group_uuid=body_mid_bone,
    )
    body_upper_bone = make_uuid()
    body_upper_grp = b.make_group(
        "bone_body_upper", origin=[0.0, 22.0, 0.0],
        children=[body_upper_uuid], group_uuid=body_upper_bone,
    )

    # ---------------- BELLY PATCH (2 overlapping flat slabs, texture 1) ----------------
    belly_a_uuid = b.add_cube(
        "belly_patch_a",
        from_=[-4.5, 13.0, -6.4],
        to_=[4.5, 19.0, -6.0],
        faces=uniform_face(0, 0, 64, 32, tex_index=1),  # use cream half
        origin=[0.0, 16.0, -6.2],
    )
    belly_b_uuid = b.add_cube(
        "belly_patch_b",
        from_=[-4.0, 17.0, -6.4],
        to_=[4.0, 22.0, -6.0],
        faces=uniform_face(0, 0, 64, 32, tex_index=1),
        origin=[0.0, 19.5, -6.2],
    )
    belly_a_bone = make_uuid()
    belly_a_grp = b.make_group("bone_belly_a", origin=[0.0, 16.0, -6.2],
                                children=[belly_a_uuid], group_uuid=belly_a_bone)
    belly_b_bone = make_uuid()
    belly_b_grp = b.make_group("bone_belly_b", origin=[0.0, 19.5, -6.2],
                                children=[belly_b_uuid], group_uuid=belly_b_bone)

    # Snout pad (cream) + foot pads (cream) — add more belly-tex slabs for richness
    snout_pad_uuid = b.add_cube(
        "snout_pad",
        from_=[-2.5, 28.0, -6.7],
        to_=[2.5, 30.5, -6.4],
        faces=uniform_face(0, 0, 64, 32, tex_index=1),
        origin=[0.0, 29.25, -6.55],
    )
    snout_pad_bone = make_uuid()
    snout_pad_grp = b.make_group("bone_snout_pad", origin=[0.0, 29.25, -6.55],
                                  children=[snout_pad_uuid], group_uuid=snout_pad_bone)

    foot_pad_l_uuid = b.add_cube(
        "foot_pad_l",
        from_=[1.5, 0.4, -3.1],
        to_=[6.5, 1.0, 1.0],
        faces=uniform_face(0, 0, 64, 32, tex_index=1),
        origin=[4.0, 0.7, -1.0],
    )
    foot_pad_r_uuid = b.add_cube(
        "foot_pad_r",
        from_=[-6.5, 0.4, -3.1],
        to_=[-1.5, 1.0, 1.0],
        faces=uniform_face(0, 0, 64, 32, tex_index=1),
        origin=[-4.0, 0.7, -1.0],
    )
    foot_pad_l_bone = make_uuid()
    foot_pad_l_grp = b.make_group("bone_foot_pad_l", origin=[4.0, 0.7, -1.0],
                                   children=[foot_pad_l_uuid], group_uuid=foot_pad_l_bone)
    foot_pad_r_bone = make_uuid()
    foot_pad_r_grp = b.make_group("bone_foot_pad_r", origin=[-4.0, 0.7, -1.0],
                                   children=[foot_pad_r_uuid], group_uuid=foot_pad_r_bone)

    # Paw pads on hands/lower arms (cream)
    paw_pad_l_uuid = b.add_cube(
        "paw_pad_l",
        from_=[8.4, 6.5, -1.65],
        to_=[11.6, 8.5, -1.55],
        faces=uniform_face(0, 0, 64, 32, tex_index=1),
        origin=[10.0, 7.5, -1.6],
    )
    paw_pad_r_uuid = b.add_cube(
        "paw_pad_r",
        from_=[-11.6, 6.5, -1.65],
        to_=[-8.4, 8.5, -1.55],
        faces=uniform_face(0, 0, 64, 32, tex_index=1),
        origin=[-10.0, 7.5, -1.6],
    )
    paw_pad_l_bone = make_uuid()
    paw_pad_l_grp = b.make_group("bone_paw_pad_l", origin=[10.0, 7.5, -1.6],
                                  children=[paw_pad_l_uuid], group_uuid=paw_pad_l_bone)
    paw_pad_r_bone = make_uuid()
    paw_pad_r_grp = b.make_group("bone_paw_pad_r", origin=[-10.0, 7.5, -1.6],
                                  children=[paw_pad_r_uuid], group_uuid=paw_pad_r_bone)

    # ---------------- HEAD (3 overlapping cubes -> round head) ----------------
    head_main_uuid = b.add_cube(
        "head_main",
        from_=[-6.0, 26.0, -5.0],
        to_=[6.0, 36.0, 5.0],
        faces=basic_cube_faces(34, 0, 12, 10, 10, tex_index=0),
        origin=[0.0, 31.0, 0.0],
    )
    head_top_uuid = b.add_cube(
        "head_top",
        from_=[-5.0, 33.0, -4.0],
        to_=[5.0, 37.5, 4.0],
        faces=basic_cube_faces(34, 22, 10, 5, 8, tex_index=0),
        origin=[0.0, 35.0, 0.0],
    )
    head_snout_uuid = b.add_cube(
        "head_snout",
        from_=[-3.5, 27.5, -6.5],
        to_=[3.5, 32.0, -4.5],
        faces=basic_cube_faces(34, 36, 7, 4, 2, tex_index=0),
        origin=[0.0, 29.5, -5.5],
    )

    head_main_bone = make_uuid()
    head_main_grp = b.make_group("bone_head_main", origin=[0.0, 31.0, 0.0],
                                  children=[head_main_uuid], group_uuid=head_main_bone)
    head_top_bone = make_uuid()
    head_top_grp = b.make_group("bone_head_top", origin=[0.0, 35.0, 0.0],
                                 children=[head_top_uuid], group_uuid=head_top_bone)
    head_snout_bone = make_uuid()
    head_snout_grp = b.make_group("bone_head_snout", origin=[0.0, 29.5, -5.5],
                                   children=[head_snout_uuid], group_uuid=head_snout_bone)

    # ---------------- EARS (2 ears x 2 cubes each, each cube own bone) ----------------
    # Left ear at +X side, right ear at -X side.
    # Each ear: outer (larger) + inner (smaller, slightly offset toward front)
    ear_l_outer_uuid = b.add_cube(
        "ear_l_outer",
        from_=[3.5, 36.0, -2.0],
        to_=[6.5, 39.5, 2.0],
        faces=basic_cube_faces(34, 44, 3, 4, 4, tex_index=0),
        origin=[5.0, 37.5, 0.0],
    )
    ear_l_inner_uuid = b.add_cube(
        "ear_l_inner",
        from_=[4.0, 36.5, -1.5],
        to_=[6.0, 39.0, 1.5],
        faces=basic_cube_faces(48, 44, 2, 3, 3, tex_index=0),
        origin=[5.0, 37.5, 0.0],
    )
    ear_r_outer_uuid = b.add_cube(
        "ear_r_outer",
        from_=[-6.5, 36.0, -2.0],
        to_=[-3.5, 39.5, 2.0],
        faces=basic_cube_faces(34, 44, 3, 4, 4, tex_index=0),
        origin=[-5.0, 37.5, 0.0],
    )
    ear_r_inner_uuid = b.add_cube(
        "ear_r_inner",
        from_=[-6.0, 36.5, -1.5],
        to_=[-4.0, 39.0, 1.5],
        faces=basic_cube_faces(48, 44, 2, 3, 3, tex_index=0),
        origin=[-5.0, 37.5, 0.0],
    )

    ear_l_outer_bone = make_uuid()
    ear_l_outer_grp = b.make_group("bone_ear_l_outer", origin=[5.0, 37.5, 0.0],
                                    children=[ear_l_outer_uuid], group_uuid=ear_l_outer_bone)
    ear_l_inner_bone = make_uuid()
    ear_l_inner_grp = b.make_group("bone_ear_l_inner", origin=[5.0, 37.5, 0.0],
                                    children=[ear_l_inner_uuid], group_uuid=ear_l_inner_bone)
    ear_r_outer_bone = make_uuid()
    ear_r_outer_grp = b.make_group("bone_ear_r_outer", origin=[-5.0, 37.5, 0.0],
                                    children=[ear_r_outer_uuid], group_uuid=ear_r_outer_bone)
    ear_r_inner_bone = make_uuid()
    ear_r_inner_grp = b.make_group("bone_ear_r_inner", origin=[-5.0, 37.5, 0.0],
                                    children=[ear_r_inner_uuid], group_uuid=ear_r_inner_bone)

    # ---------------- NOSE (1 small flat cube on snout) ----------------
    nose_uuid = b.add_cube(
        "nose",
        from_=[-1.2, 29.5, -7.0],
        to_=[1.2, 31.0, -6.5],
        faces=uniform_face(56, 32, 64, 40, tex_index=0),  # darker zone of body texture
        origin=[0.0, 30.25, -6.75],
    )
    nose_bone = make_uuid()
    nose_grp = b.make_group("bone_nose", origin=[0.0, 30.25, -6.75],
                             children=[nose_uuid], group_uuid=nose_bone)

    # ---------------- EYES (x-stitches: each eye = 2 thin slabs crossing) ----------------
    # Each eye bone holds 2 thin slabs; we rotate one slab via its own bone to make X.
    # Approach: each eye = parent bone at eye centre. Children = two slab-bones, each
    # rotated +/- 45° on Z axis. Slabs are textured with seam_dark area of body texture.
    # Place eyes on front of head at z=-5 (face plane).
    def build_eye(name_prefix, ex, ey, ez):
        slab_a_uuid = b.add_cube(
            f"{name_prefix}_slab_a",
            from_=[-1.2, -0.15, -0.15],
            to_=[1.2, 0.15, 0.15],
            faces=uniform_face(56, 56, 64, 64, tex_index=0),  # dark seam corner
            origin=[0.0, 0.0, 0.0],
        )
        slab_b_uuid = b.add_cube(
            f"{name_prefix}_slab_b",
            from_=[-1.2, -0.15, -0.15],
            to_=[1.2, 0.15, 0.15],
            faces=uniform_face(56, 56, 64, 64, tex_index=0),
            origin=[0.0, 0.0, 0.0],
        )
        slab_a_bone = make_uuid()
        slab_a_grp = b.make_group(
            f"bone_{name_prefix}_slab_a",
            origin=[0.0, 0.0, 0.0],
            children=[slab_a_uuid], rotation=[0, 0, 45],
            group_uuid=slab_a_bone,
        )
        slab_b_bone = make_uuid()
        slab_b_grp = b.make_group(
            f"bone_{name_prefix}_slab_b",
            origin=[0.0, 0.0, 0.0],
            children=[slab_b_uuid], rotation=[0, 0, -45],
            group_uuid=slab_b_bone,
        )
        eye_bone = make_uuid()
        eye_grp = b.make_group(
            f"bone_{name_prefix}",
            origin=[ex, ey, ez],
            children=[slab_a_grp, slab_b_grp],
            group_uuid=eye_bone,
        )
        return eye_grp, eye_bone, slab_a_bone, slab_b_bone

    eye_l_grp, eye_l_bone, eye_l_a_bone, eye_l_b_bone = build_eye("eye_l", 2.5, 32.5, -5.05)
    eye_r_grp, eye_r_bone, eye_r_a_bone, eye_r_b_bone = build_eye("eye_r", -2.5, 32.5, -5.05)

    # ---------------- ARMS (2 arms; each: upper x 2 cubes + lower x 2 cubes; each segment own bone) ----------------
    # Arms held slightly out from body. Left arm: +X side. Right arm: -X side.
    # Build in WORLD coords since arm pivot at shoulder; bones are origin at shoulder.
    def build_arm(side_sign, name):
        sx = side_sign  # +1 left, -1 right
        # Shoulder pivot at (sx*8, 22, 0)
        sh_x, sh_y, sh_z = sx * 8.0, 22.0, 0.0

        # Upper arm: 2 cubes (a top, b bottom) — slightly angled outward
        upper_a_uuid = b.add_cube(
            f"{name}_upper_a",
            from_=[sx * 8.5 - 2.0 if sx > 0 else sx * 8.5 - 2.0, 18.0, -2.0],
            to_=  [sx * 8.5 + 2.0 if sx > 0 else sx * 8.5 + 2.0, 22.0, 2.0],
            faces=basic_cube_faces(0, 40, 4, 4, 4, tex_index=0),
            origin=[sh_x, 22.0, 0.0],
        )
        upper_b_uuid = b.add_cube(
            f"{name}_upper_b",
            from_=[sx * 9.0 - 2.0, 14.0, -2.0],
            to_=[sx * 9.0 + 2.0, 18.5, 2.0],
            faces=basic_cube_faces(16, 40, 4, 4, 4, tex_index=0),
            origin=[sh_x, 22.0, 0.0],
        )
        # Lower arm: 2 cubes
        lower_a_uuid = b.add_cube(
            f"{name}_lower_a",
            from_=[sx * 9.5 - 1.8, 10.0, -1.8],
            to_=[sx * 9.5 + 1.8, 14.5, 1.8],
            faces=basic_cube_faces(32, 40, 4, 4, 4, tex_index=0),
            origin=[sx * 9.0, 14.0, 0.0],
        )
        lower_b_uuid = b.add_cube(
            f"{name}_lower_b",
            from_=[sx * 10.0 - 1.6, 6.5, -1.6],
            to_=[sx * 10.0 + 1.6, 10.5, 1.6],
            faces=basic_cube_faces(48, 40, 4, 4, 4, tex_index=0),
            origin=[sx * 9.0, 14.0, 0.0],
        )

        # Each segment own bone
        upper_a_bone = make_uuid()
        upper_a_grp = b.make_group(f"bone_{name}_upper_a",
                                     origin=[sh_x, 22.0, 0.0],
                                     children=[upper_a_uuid], group_uuid=upper_a_bone)
        upper_b_bone = make_uuid()
        upper_b_grp = b.make_group(f"bone_{name}_upper_b",
                                     origin=[sh_x, 22.0, 0.0],
                                     children=[upper_b_uuid], group_uuid=upper_b_bone)
        lower_a_bone = make_uuid()
        lower_a_grp = b.make_group(f"bone_{name}_lower_a",
                                     origin=[sx * 9.0, 14.0, 0.0],
                                     children=[lower_a_uuid], group_uuid=lower_a_bone)
        lower_b_bone = make_uuid()
        lower_b_grp = b.make_group(f"bone_{name}_lower_b",
                                     origin=[sx * 9.0, 14.0, 0.0],
                                     children=[lower_b_uuid], group_uuid=lower_b_bone)

        # Wrap whole arm in a "shoulder" parent bone for easy bulk animation
        shoulder_bone = make_uuid()
        shoulder_grp = b.make_group(
            f"bone_{name}_shoulder",
            origin=[sh_x, 22.0, 0.0],
            children=[upper_a_grp, upper_b_grp, lower_a_grp, lower_b_grp],
            group_uuid=shoulder_bone,
            # angle outward slightly initially
            rotation=[0, 0, sx * -8.0],
        )
        return shoulder_grp, {
            "shoulder": shoulder_bone,
            "upper_a": upper_a_bone,
            "upper_b": upper_b_bone,
            "lower_a": lower_a_bone,
            "lower_b": lower_b_bone,
            "side_sign": sx,
        }

    arm_l_grp, arm_l = build_arm(+1, "arm_l")
    arm_r_grp, arm_r = build_arm(-1, "arm_r")

    # ---------------- LEGS (2 legs x 2 cubes each, each cube own bone) ----------------
    def build_leg(side_sign, name):
        sx = side_sign
        thigh_uuid = b.add_cube(
            f"{name}_thigh",
            from_=[sx * 4.0 - 2.5, 5.0, -2.5],
            to_=[sx * 4.0 + 2.5, 11.0, 2.5],
            faces=basic_cube_faces(0, 50, 5, 6, 5, tex_index=0),
            origin=[sx * 4.0, 11.0, 0.0],
        )
        foot_uuid = b.add_cube(
            f"{name}_foot",
            from_=[sx * 4.0 - 3.0, 0.0, -3.0],
            to_=[sx * 4.0 + 3.0, 5.5, 3.5],
            faces=basic_cube_faces(20, 50, 6, 5, 6, tex_index=0),
            origin=[sx * 4.0, 5.0, 0.0],
        )
        thigh_bone = make_uuid()
        thigh_grp = b.make_group(f"bone_{name}_thigh",
                                   origin=[sx * 4.0, 11.0, 0.0],
                                   children=[thigh_uuid], group_uuid=thigh_bone)
        foot_bone = make_uuid()
        foot_grp = b.make_group(f"bone_{name}_foot",
                                  origin=[sx * 4.0, 5.0, 0.0],
                                  children=[foot_uuid], group_uuid=foot_bone)
        # Wrap leg in a hip parent
        hip_bone = make_uuid()
        hip_grp = b.make_group(
            f"bone_{name}_hip",
            origin=[sx * 4.0, 11.0, 0.0],
            children=[thigh_grp, foot_grp],
            group_uuid=hip_bone,
        )
        return hip_grp, {
            "hip": hip_bone,
            "thigh": thigh_bone,
            "foot": foot_bone,
            "side_sign": sx,
        }

    leg_l_grp, leg_l = build_leg(+1, "leg_l")
    leg_r_grp, leg_r = build_leg(-1, "leg_r")

    # ---------------- BOW (3 flat slabs forming bow tie at neck, texture 1 ribbon section) ----------------
    # Position: just below head at neck/upper torso level (~y=24-25), in front (z=-5).
    # Bow: left loop, knot center, right loop.
    # All textured with bottom (ribbon) half of texture 1.
    bow_loop_l_uuid = b.add_cube(
        "bow_loop_l",
        from_=[1.0, 23.0, -6.6],
        to_=[5.5, 26.5, -6.0],
        faces=uniform_face(0, 32, 64, 64, tex_index=1),
        origin=[3.25, 24.75, -6.3],
    )
    bow_loop_r_uuid = b.add_cube(
        "bow_loop_r",
        from_=[-5.5, 23.0, -6.6],
        to_=[-1.0, 26.5, -6.0],
        faces=uniform_face(0, 32, 64, 64, tex_index=1),
        origin=[-3.25, 24.75, -6.3],
    )
    bow_knot_uuid = b.add_cube(
        "bow_knot",
        from_=[-1.5, 23.5, -6.8],
        to_=[1.5, 26.0, -5.8],
        faces=uniform_face(0, 32, 64, 64, tex_index=1),
        origin=[0.0, 24.75, -6.3],
    )
    bow_loop_l_bone = make_uuid()
    bow_loop_l_grp = b.make_group("bone_bow_loop_l",
                                   origin=[3.25, 24.75, -6.3],
                                   children=[bow_loop_l_uuid],
                                   rotation=[0, 0, -8],
                                   group_uuid=bow_loop_l_bone)
    bow_loop_r_bone = make_uuid()
    bow_loop_r_grp = b.make_group("bone_bow_loop_r",
                                   origin=[-3.25, 24.75, -6.3],
                                   children=[bow_loop_r_uuid],
                                   rotation=[0, 0, 8],
                                   group_uuid=bow_loop_r_bone)
    bow_knot_bone = make_uuid()
    bow_knot_grp = b.make_group("bone_bow_knot",
                                 origin=[0.0, 24.75, -6.3],
                                 children=[bow_knot_uuid],
                                 group_uuid=bow_knot_bone)

    # ----------------------------------------------------------------
    # ROOT — single bear bone wrapping everything (used for spawn sink/rise & dissipate)
    # ----------------------------------------------------------------
    bear_root_bone = make_uuid()
    bear_root_grp = b.make_group(
        "bone_bear_root",
        origin=[0.0, 0.0, 0.0],
        children=[
            # Legs first
            leg_l_grp, leg_r_grp,
            # Body
            body_lower_grp, body_mid_grp, body_upper_grp,
            belly_a_grp, belly_b_grp,
            # Arms
            arm_l_grp, arm_r_grp,
            # Head
            head_main_grp, head_top_grp, head_snout_grp,
            ear_l_outer_grp, ear_l_inner_grp,
            ear_r_outer_grp, ear_r_inner_grp,
            nose_grp, snout_pad_grp,
            eye_l_grp, eye_r_grp,
            # Bow
            bow_loop_l_grp, bow_knot_grp, bow_loop_r_grp,
            # Pads
            foot_pad_l_grp, foot_pad_r_grp,
            paw_pad_l_grp, paw_pad_r_grp,
        ],
        group_uuid=bear_root_bone,
    )
    b.outliner.append(bear_root_grp)

    # ============================================================
    # ANIMATIONS
    # ============================================================

    # ----------------- SPAWN (1.5s, once, override=true) -----------------
    # Bear materialises from ground up:
    #   0.0s — everything scaled to 0
    #   0.2s — legs appear
    #   0.5s — torso appears
    #   0.6s — arms appear, swing outward
    #   0.8s — head
    #   0.9s — ears
    #   1.0s — facial features (nose, eyes) + bow appear
    #   1.2s — arms reach "bear hug ready" position
    SPAWN_LEN = 1.5
    spawn_animators = {}

    def hide_until(bone, t_appear, t_pop=None, scale_pop=None, t_settle=None):
        """Helper: make a bone scale 0 -> 1 at given time, with optional pop overshoot."""
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0, interpolation="linear"),
            make_keyframe("scale", max(0.0, t_appear - 0.001), 0, 0, 0, interpolation="linear"),
            make_keyframe("scale", t_appear, 0.3, 0.3, 0.3, interpolation="catmullrom"),
        ]
        if t_pop is not None and scale_pop is not None:
            kfs.append(make_keyframe("scale", t_pop, *scale_pop, interpolation="catmullrom"))
        if t_settle is not None:
            kfs.append(make_keyframe("scale", t_settle, 1.0, 1.0, 1.0, interpolation="catmullrom"))
        else:
            kfs.append(make_keyframe("scale", min(SPAWN_LEN, t_appear + 0.3), 1.0, 1.0, 1.0, interpolation="catmullrom"))
        kfs.append(make_keyframe("scale", SPAWN_LEN, 1.0, 1.0, 1.0, interpolation="linear"))
        return kfs

    # Legs (0.2s)
    for leg in (leg_l, leg_r):
        spawn_animators[leg["hip"]] = {
            "name": "leg_hip", "type": "bone",
            "keyframes": hide_until(leg["hip"], 0.20, t_pop=0.32, scale_pop=(1.15, 1.15, 1.15), t_settle=0.45),
        }
        spawn_animators[leg["thigh"]] = {
            "name": "leg_thigh", "type": "bone",
            "keyframes": hide_until(leg["thigh"], 0.22, t_pop=0.34, scale_pop=(1.1, 1.1, 1.1), t_settle=0.5),
        }
        spawn_animators[leg["foot"]] = {
            "name": "leg_foot", "type": "bone",
            "keyframes": hide_until(leg["foot"], 0.20, t_pop=0.30, scale_pop=(1.2, 0.9, 1.2), t_settle=0.45),
        }

    # Torso (0.5s)
    for bn, t in [(body_lower_bone, 0.50), (body_mid_bone, 0.55), (body_upper_bone, 0.60)]:
        spawn_animators[bn] = {
            "name": "torso", "type": "bone",
            "keyframes": hide_until(bn, t, t_pop=t + 0.15, scale_pop=(1.1, 1.05, 1.1), t_settle=t + 0.30),
        }
    # Belly patches with torso
    for bn in (belly_a_bone, belly_b_bone):
        spawn_animators[bn] = {
            "name": "belly", "type": "bone",
            "keyframes": hide_until(bn, 0.65, t_pop=0.78, scale_pop=(1.1, 1.1, 1.1), t_settle=0.90),
        }

    # Arms (0.6s) — appear AND swing outward from sides as if awakening
    for side_arm, side_label in [(arm_l, "l"), (arm_r, "r")]:
        sx = side_arm["side_sign"]
        spawn_animators[side_arm["shoulder"]] = {
            "name": f"shoulder_{side_label}", "type": "bone",
            "keyframes": [
                # appear
                make_keyframe("scale", 0.0, 0, 0, 0, interpolation="linear"),
                make_keyframe("scale", 0.55, 0, 0, 0, interpolation="linear"),
                make_keyframe("scale", 0.62, 0.4, 0.4, 0.4, interpolation="catmullrom"),
                make_keyframe("scale", 0.85, 1.1, 1.1, 1.1, interpolation="catmullrom"),
                make_keyframe("scale", 1.05, 1.0, 1.0, 1.0, interpolation="catmullrom"),
                make_keyframe("scale", SPAWN_LEN, 1.0, 1.0, 1.0, interpolation="linear"),
                # rotation: swing outward (anticipation), then settle into bear-hug ready (slight forward + outward)
                make_keyframe("rotation", 0.0, 0, 0, sx * -8, interpolation="linear"),
                make_keyframe("rotation", 0.62, -10, sx * -10, sx * -8, interpolation="catmullrom"),
                make_keyframe("rotation", 0.80, -5, sx * -25, sx * -25, interpolation="catmullrom"),
                make_keyframe("rotation", 1.20, -10, sx * -15, sx * -18, interpolation="catmullrom"),
                make_keyframe("rotation", SPAWN_LEN, -10, sx * -15, sx * -18, interpolation="catmullrom"),
            ],
        }
        # Per-segment scale pops
        for seg_key, t_app in [("upper_a", 0.62), ("upper_b", 0.66),
                               ("lower_a", 0.70), ("lower_b", 0.74)]:
            spawn_animators[side_arm[seg_key]] = {
                "name": f"{seg_key}_{side_label}", "type": "bone",
                "keyframes": hide_until(side_arm[seg_key], t_app,
                                         t_pop=t_app + 0.12, scale_pop=(1.1, 1.05, 1.1),
                                         t_settle=t_app + 0.25),
            }

    # Head (0.8s)
    for bn, t in [(head_main_bone, 0.80), (head_top_bone, 0.85), (head_snout_bone, 0.88)]:
        spawn_animators[bn] = {
            "name": "head", "type": "bone",
            "keyframes": hide_until(bn, t, t_pop=t + 0.12, scale_pop=(1.12, 1.08, 1.12), t_settle=t + 0.25),
        }

    # Ears (0.9s)
    for bn in (ear_l_outer_bone, ear_l_inner_bone, ear_r_outer_bone, ear_r_inner_bone):
        spawn_animators[bn] = {
            "name": "ear", "type": "bone",
            "keyframes": hide_until(bn, 0.92, t_pop=1.02, scale_pop=(1.2, 1.2, 1.2), t_settle=1.15),
        }

    # Facial features (1.0s) — nose first
    spawn_animators[nose_bone] = {
        "name": "nose", "type": "bone",
        "keyframes": hide_until(nose_bone, 1.00, t_pop=1.08, scale_pop=(1.3, 1.3, 1.3), t_settle=1.18),
    }
    # Snout pad with snout
    spawn_animators[snout_pad_bone] = {
        "name": "snout_pad", "type": "bone",
        "keyframes": hide_until(snout_pad_bone, 0.90, t_pop=1.00, scale_pop=(1.15, 1.15, 1.15), t_settle=1.15),
    }
    # Foot pads with feet
    for bn in (foot_pad_l_bone, foot_pad_r_bone):
        spawn_animators[bn] = {
            "name": "foot_pad", "type": "bone",
            "keyframes": hide_until(bn, 0.25, t_pop=0.36, scale_pop=(1.2, 1.2, 1.2), t_settle=0.50),
        }
    # Paw pads with arms
    for bn in (paw_pad_l_bone, paw_pad_r_bone):
        spawn_animators[bn] = {
            "name": "paw_pad", "type": "bone",
            "keyframes": hide_until(bn, 0.78, t_pop=0.88, scale_pop=(1.2, 1.2, 1.2), t_settle=1.00),
        }
    # Eyes — both x stitches blink-pop in
    for eye_bone, eye_a, eye_b in [(eye_l_bone, eye_l_a_bone, eye_l_b_bone),
                                    (eye_r_bone, eye_r_a_bone, eye_r_b_bone)]:
        spawn_animators[eye_bone] = {
            "name": "eye", "type": "bone",
            "keyframes": hide_until(eye_bone, 1.05, t_pop=1.15, scale_pop=(1.3, 1.3, 1.3), t_settle=1.30),
        }
        # Slabs: gentle scale pop too (rotation already applied via group rotation)
        for slab_bone in (eye_a, eye_b):
            spawn_animators[slab_bone] = {
                "name": "eye_slab", "type": "bone",
                "keyframes": [
                    make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
                    make_keyframe("scale", 1.05, 0.6, 1.0, 1.0, interpolation="catmullrom"),
                    make_keyframe("scale", 1.20, 1.2, 1.0, 1.0, interpolation="catmullrom"),
                    make_keyframe("scale", SPAWN_LEN, 1.0, 1.0, 1.0, interpolation="catmullrom"),
                ],
            }

    # Bow (1.0s) — knot first, loops second
    spawn_animators[bow_knot_bone] = {
        "name": "bow_knot", "type": "bone",
        "keyframes": hide_until(bow_knot_bone, 1.00, t_pop=1.10, scale_pop=(1.4, 1.4, 1.4), t_settle=1.25),
    }
    for bn, t in [(bow_loop_l_bone, 1.05), (bow_loop_r_bone, 1.05)]:
        spawn_animators[bn] = {
            "name": "bow_loop", "type": "bone",
            "keyframes": [
                make_keyframe("scale", 0.0, 0, 0, 0, interpolation="linear"),
                make_keyframe("scale", t - 0.001, 0, 0, 0, interpolation="linear"),
                make_keyframe("scale", t, 0.2, 0.2, 0.2, interpolation="catmullrom"),
                make_keyframe("scale", t + 0.12, 1.4, 1.4, 1.4, interpolation="catmullrom"),
                make_keyframe("scale", t + 0.25, 1.0, 1.0, 1.0, interpolation="catmullrom"),
                make_keyframe("scale", SPAWN_LEN, 1.0, 1.0, 1.0, interpolation="linear"),
            ],
        }

    # Root: gentle "rising" motion — entire bear lifts slightly from underground
    spawn_animators[bear_root_bone] = {
        "name": "bear_root", "type": "bone",
        "keyframes": [
            make_keyframe("position", 0.0, 0, -3, 0, interpolation="linear"),
            make_keyframe("position", 0.20, 0, -2.0, 0, interpolation="catmullrom"),
            make_keyframe("position", 0.50, 0, -0.8, 0, interpolation="catmullrom"),
            make_keyframe("position", 0.80, 0, 0.0, 0, interpolation="catmullrom"),
            make_keyframe("position", 1.20, 0, 0.3, 0, interpolation="catmullrom"),
            make_keyframe("position", SPAWN_LEN, 0, 0, 0, interpolation="catmullrom"),
            make_keyframe("rotation", 0.0, 0, -8, 0, interpolation="linear"),
            make_keyframe("rotation", 0.50, 0, -4, 0, interpolation="catmullrom"),
            make_keyframe("rotation", SPAWN_LEN, 0, 0, 0, interpolation="catmullrom"),
        ],
    }

    b.add_animation("spawn", length=SPAWN_LEN, animators=spawn_animators,
                    loop="once", override=True)

    # ----------------- IDLE (5.0s, loop, override=false) -----------------
    # Breathing (torso scale 1.0->1.05), arms sway, head tilt, ears twitch, bow sway.
    IDLE_LEN = 5.0
    idle_animators = {}

    # Breathing torso — densely keyframed for smooth feel
    for bn, phase in [(body_lower_bone, 0.0), (body_mid_bone, 0.1), (body_upper_bone, 0.15)]:
        kfs = []
        N = 20
        for i in range(N + 1):
            t = (i / N) * IDLE_LEN
            # 2 full breath cycles over 5s
            cycle = math.sin((t / IDLE_LEN) * 4 * math.pi + phase)
            sx_v = 1.0 + 0.04 * cycle
            sy_v = 1.0 + 0.05 * cycle
            sz_v = 1.0 + 0.04 * cycle
            kfs.append(make_keyframe("scale", t, sx_v, sy_v, sz_v, interpolation="catmullrom"))
        idle_animators[bn] = {
            "name": "torso_breathe", "type": "bone", "keyframes": kfs,
        }
    # Belly patches sympathetic to body
    for bn, phase in [(belly_a_bone, 0.0), (belly_b_bone, 0.2)]:
        kfs = []
        N = 18
        for i in range(N + 1):
            t = (i / N) * IDLE_LEN
            cycle = math.sin((t / IDLE_LEN) * 4 * math.pi + phase)
            sx_v = 1.0 + 0.03 * cycle
            sy_v = 1.0 + 0.04 * cycle
            kfs.append(make_keyframe("scale", t, sx_v, sy_v, 1.0, interpolation="catmullrom"))
        idle_animators[bn] = {
            "name": "belly_breathe", "type": "bone", "keyframes": kfs,
        }
    # Snout pad subtle pulse
    snout_kfs = []
    N = 14
    for i in range(N + 1):
        t = (i / N) * IDLE_LEN
        cycle = math.sin((t / IDLE_LEN) * 2 * math.pi)
        snout_kfs.append(make_keyframe("scale", t, 1.0 + 0.02 * cycle, 1.0 + 0.02 * cycle, 1.0, interpolation="catmullrom"))
    idle_animators[snout_pad_bone] = {
        "name": "snout_pad_idle", "type": "bone", "keyframes": snout_kfs,
    }
    # Foot pads minor flex
    for bn, phase in [(foot_pad_l_bone, 0.0), (foot_pad_r_bone, 0.5)]:
        kfs = []
        N = 12
        for i in range(N + 1):
            t = (i / N) * IDLE_LEN
            cycle = math.sin((t / IDLE_LEN) * 2 * math.pi + phase)
            kfs.append(make_keyframe("scale", t, 1.0 + 0.04 * cycle, 1.0, 1.0 + 0.03 * cycle, interpolation="catmullrom"))
        idle_animators[bn] = {
            "name": "foot_pad_idle", "type": "bone", "keyframes": kfs,
        }
    # Paw pads sway with arms
    for bn, phase in [(paw_pad_l_bone, 0.0), (paw_pad_r_bone, 0.7)]:
        kfs = []
        N = 12
        for i in range(N + 1):
            t = (i / N) * IDLE_LEN
            cycle = math.sin((t / IDLE_LEN) * 2 * math.pi + phase)
            kfs.append(make_keyframe("scale", t, 1.0 + 0.05 * cycle, 1.0 + 0.03 * cycle, 1.0, interpolation="catmullrom"))
        idle_animators[bn] = {
            "name": "paw_pad_idle", "type": "bone", "keyframes": kfs,
        }

    # Head tilts side to side on slow period (single full cycle over 5s)
    head_bones_phase = [(head_main_bone, 1.0, 0.0), (head_top_bone, 1.0, 0.0), (head_snout_bone, 0.6, 0.0)]
    for bn, amp_mul, ph in head_bones_phase:
        kfs = []
        N = 20
        for i in range(N + 1):
            t = (i / N) * IDLE_LEN
            cycle_y = math.sin((t / IDLE_LEN) * 2 * math.pi + ph)
            cycle_z = math.cos((t / IDLE_LEN) * 2 * math.pi + ph)
            cycle_x = math.sin((t / IDLE_LEN) * 4 * math.pi + ph)
            kfs.append(make_keyframe(
                "rotation", t,
                amp_mul * 2 * cycle_x,
                amp_mul * 5 * cycle_y,
                amp_mul * 4 * cycle_z,
                interpolation="catmullrom",
            ))
        idle_animators[bn] = {
            "name": "head_tilt", "type": "bone", "keyframes": kfs,
        }

    # Ears very slight twitch (densified — multiple small twitch events over 5s)
    for ear_bn, sign, ph in [(ear_l_outer_bone, +1, 0.0),
                              (ear_l_inner_bone, +1, 0.15),
                              (ear_r_outer_bone, -1, 0.4),
                              (ear_r_inner_bone, -1, 0.55)]:
        kfs = []
        N = 22
        for i in range(N + 1):
            t = (i / N) * IDLE_LEN
            # mix of slow base sway and quick twitch pulses
            base_x = 1.5 * math.sin((t / IDLE_LEN) * 2 * math.pi + ph)
            base_z = sign * 1.2 * math.cos((t / IDLE_LEN) * 2 * math.pi + ph)
            # twitch pulses every ~1.7s
            twitch = 2.5 * math.sin((t / IDLE_LEN) * 6 * math.pi + ph * 3)
            kfs.append(make_keyframe(
                "rotation", t,
                base_x + 0.5 * twitch,
                0,
                base_z + sign * 0.3 * twitch,
                interpolation="catmullrom",
            ))
        idle_animators[ear_bn] = {
            "name": "ear_twitch", "type": "bone", "keyframes": kfs,
        }

    # Arms sway gently (densified)
    for side_arm, side_label, phase in [(arm_l, "l", 0.0), (arm_r, "r", 0.4)]:
        sx = side_arm["side_sign"]
        kfs = []
        N = 20
        for i in range(N + 1):
            t = (i / N) * IDLE_LEN
            sway_x = math.sin((t / IDLE_LEN) * 2 * math.pi + phase)
            sway_y = math.cos((t / IDLE_LEN) * 2 * math.pi + phase)
            sway_z = math.sin((t / IDLE_LEN) * 4 * math.pi + phase)
            kfs.append(make_keyframe(
                "rotation", t,
                -10 + 2 * sway_x,
                sx * (-15 + 3 * sway_y),
                sx * (-18 + 4 * sway_z),
                interpolation="catmullrom",
            ))
        idle_animators[side_arm["shoulder"]] = {
            "name": f"shoulder_{side_label}_sway", "type": "bone", "keyframes": kfs,
        }
        # Each segment subtle dangle
        for seg_key, amp, off in [("upper_a", 1.5, 0.0), ("upper_b", 2.0, 0.1),
                                   ("lower_a", 2.5, 0.2), ("lower_b", 3.0, 0.3)]:
            kfs = []
            N = 14
            for i in range(N + 1):
                t = (i / N) * IDLE_LEN
                wave = math.sin((t / IDLE_LEN) * 2 * math.pi + phase + off)
                wave2 = math.cos((t / IDLE_LEN) * 4 * math.pi + phase + off)
                kfs.append(make_keyframe(
                    "rotation", t,
                    sx * amp * wave,
                    0,
                    amp * 0.6 * wave2,
                    interpolation="catmullrom",
                ))
            idle_animators[side_arm[seg_key]] = {
                "name": f"{seg_key}_{side_label}", "type": "bone", "keyframes": kfs,
            }

    # Bow sway at neck (densified)
    bow_kfs_knot = []
    N = 18
    for i in range(N + 1):
        t = (i / N) * IDLE_LEN
        sway = math.sin((t / IDLE_LEN) * 2 * math.pi)
        bob = math.cos((t / IDLE_LEN) * 4 * math.pi)
        bow_kfs_knot.append(make_keyframe(
            "rotation", t, 1 * bob, 2 * sway, 4 * sway,
            interpolation="catmullrom",
        ))
    idle_animators[bow_knot_bone] = {
        "name": "bow_knot_sway", "type": "bone", "keyframes": bow_kfs_knot,
    }
    for bn, base_z, ph in [(bow_loop_l_bone, -8, 0.0), (bow_loop_r_bone, 8, 0.5)]:
        kfs = []
        N = 18
        for i in range(N + 1):
            t = (i / N) * IDLE_LEN
            sway = math.sin((t / IDLE_LEN) * 2 * math.pi + ph)
            bob = math.cos((t / IDLE_LEN) * 4 * math.pi + ph)
            kfs.append(make_keyframe(
                "rotation", t,
                2 * sway, 1.5 * bob, base_z + 4 * sway,
                interpolation="catmullrom",
            ))
        idle_animators[bn] = {
            "name": "bow_loop_sway", "type": "bone", "keyframes": kfs,
        }

    # Root tiny rocking — feels lumbering and patient
    idle_animators[bear_root_bone] = {
        "name": "bear_root_lumber", "type": "bone",
        "keyframes": [
            make_keyframe("position", 0.0, 0, 0, 0, interpolation="catmullrom"),
            make_keyframe("position", 1.25, 0, 0.15, 0, interpolation="catmullrom"),
            make_keyframe("position", 2.5, 0, 0, 0, interpolation="catmullrom"),
            make_keyframe("position", 3.75, 0, 0.15, 0, interpolation="catmullrom"),
            make_keyframe("position", IDLE_LEN, 0, 0, 0, interpolation="catmullrom"),
            make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="catmullrom"),
            make_keyframe("rotation", 1.25, 1, 2, 0, interpolation="catmullrom"),
            make_keyframe("rotation", 2.5, 0, 0, 0, interpolation="catmullrom"),
            make_keyframe("rotation", 3.75, 1, -2, 0, interpolation="catmullrom"),
            make_keyframe("rotation", IDLE_LEN, 0, 0, 0, interpolation="catmullrom"),
        ],
    }

    b.add_animation("idle", length=IDLE_LEN, animators=idle_animators,
                    loop="loop", override=False)

    # ----------------- DISSIPATE (1.0s, once, override=true) -----------------
    # Arms try to hug inward (farewell). Then bear sinks backward into ground from feet up.
    # Head is last to go. Eyes are very last visible.
    DISS_LEN = 1.0
    diss_animators = {}

    # Arms hug inward — rotate Z so arms cross in front
    for side_arm, side_label in [(arm_l, "l"), (arm_r, "r")]:
        sx = side_arm["side_sign"]
        diss_animators[side_arm["shoulder"]] = {
            "name": f"shoulder_{side_label}_diss", "type": "bone",
            "keyframes": [
                make_keyframe("rotation", 0.0, -10, sx * -15, sx * -18, interpolation="linear"),
                # Anticipation
                make_keyframe("rotation", 0.10, -5, sx * -22, sx * -25, interpolation="catmullrom"),
                # Hug! cross arms in front (z=−5 forward), inward Z rotation
                make_keyframe("rotation", 0.30, -25, sx * 35, sx * 30, interpolation="catmullrom"),
                # Arms scale to 0 as bear sinks
                make_keyframe("rotation", DISS_LEN, -25, sx * 35, sx * 30, interpolation="linear"),
                make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
                make_keyframe("scale", 0.45, 1.0, 1.0, 1.0, interpolation="catmullrom"),
                make_keyframe("scale", 0.65, 0.6, 0.6, 0.6, interpolation="catmullrom"),
                make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="catmullrom"),
            ],
        }

    # Legs sink first (feet first)
    for leg in (leg_l, leg_r):
        for bn in (leg["foot"], leg["thigh"], leg["hip"]):
            diss_animators[bn] = {
                "name": "leg_sink", "type": "bone",
                "keyframes": [
                    make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
                    make_keyframe("scale", 0.20, 1.05, 0.95, 1.05, interpolation="catmullrom"),
                    make_keyframe("scale", 0.30, 1.0, 1.0, 1.0, interpolation="catmullrom"),
                    make_keyframe("scale", 0.40, 0.85, 0.6, 0.85, interpolation="catmullrom"),
                    make_keyframe("scale", 0.50, 0.5, 0.3, 0.5, interpolation="catmullrom"),
                    make_keyframe("scale", 0.58, 0.25, 0.15, 0.25, interpolation="catmullrom"),
                    make_keyframe("scale", 0.65, 0.0, 0.0, 0.0, interpolation="catmullrom"),
                    make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="linear"),
                ],
            }
    # Foot pads sink with feet
    for bn in (foot_pad_l_bone, foot_pad_r_bone):
        diss_animators[bn] = {
            "name": "foot_pad_sink", "type": "bone",
            "keyframes": [
                make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
                make_keyframe("scale", 0.30, 1.0, 1.0, 1.0, interpolation="catmullrom"),
                make_keyframe("scale", 0.50, 0.5, 0.3, 0.5, interpolation="catmullrom"),
                make_keyframe("scale", 0.62, 0.0, 0.0, 0.0, interpolation="catmullrom"),
                make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="linear"),
            ],
        }
    # Paw pads sink with arms
    for bn in (paw_pad_l_bone, paw_pad_r_bone):
        diss_animators[bn] = {
            "name": "paw_pad_sink", "type": "bone",
            "keyframes": [
                make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
                make_keyframe("scale", 0.45, 1.0, 1.0, 1.0, interpolation="catmullrom"),
                make_keyframe("scale", 0.65, 0.6, 0.6, 0.6, interpolation="catmullrom"),
                make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="catmullrom"),
            ],
        }

    # Torso sinks second
    for bn in (body_lower_bone, body_mid_bone, body_upper_bone, belly_a_bone, belly_b_bone):
        diss_animators[bn] = {
            "name": "body_sink", "type": "bone",
            "keyframes": [
                make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
                make_keyframe("scale", 0.50, 1.0, 1.0, 1.0, interpolation="catmullrom"),
                make_keyframe("scale", 0.65, 0.7, 0.4, 0.7, interpolation="catmullrom"),
                make_keyframe("scale", 0.80, 0.0, 0.0, 0.0, interpolation="catmullrom"),
                make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="linear"),
            ],
        }

    # Bow goes with torso
    for bn in (bow_knot_bone, bow_loop_l_bone, bow_loop_r_bone):
        diss_animators[bn] = {
            "name": "bow_sink", "type": "bone",
            "keyframes": [
                make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
                make_keyframe("scale", 0.50, 1.0, 1.0, 1.0, interpolation="catmullrom"),
                make_keyframe("scale", 0.70, 1.2, 1.2, 1.2, interpolation="catmullrom"),
                make_keyframe("scale", 0.82, 0.0, 0.0, 0.0, interpolation="catmullrom"),
                make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="linear"),
            ],
        }

    # Ears sink before head fully gone
    for bn in (ear_l_outer_bone, ear_l_inner_bone, ear_r_outer_bone, ear_r_inner_bone):
        diss_animators[bn] = {
            "name": "ear_sink", "type": "bone",
            "keyframes": [
                make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
                make_keyframe("scale", 0.70, 1.0, 1.0, 1.0, interpolation="catmullrom"),
                make_keyframe("scale", 0.85, 0.4, 0.4, 0.4, interpolation="catmullrom"),
                make_keyframe("scale", 0.92, 0.0, 0.0, 0.0, interpolation="catmullrom"),
                make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="linear"),
            ],
        }

    # Head main / top / snout sink third (just before eyes)
    for bn, t_gone in [(head_top_bone, 0.90), (head_main_bone, 0.95), (head_snout_bone, 0.93)]:
        diss_animators[bn] = {
            "name": "head_sink", "type": "bone",
            "keyframes": [
                make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
                make_keyframe("scale", 0.78, 1.0, 1.0, 1.0, interpolation="catmullrom"),
                make_keyframe("scale", t_gone - 0.07, 0.7, 0.7, 0.7, interpolation="catmullrom"),
                make_keyframe("scale", t_gone, 0.0, 0.0, 0.0, interpolation="catmullrom"),
                make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="linear"),
            ],
        }

    # Nose sinks with head
    diss_animators[nose_bone] = {
        "name": "nose_sink", "type": "bone",
        "keyframes": [
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
            make_keyframe("scale", 0.85, 1.0, 1.0, 1.0, interpolation="catmullrom"),
            make_keyframe("scale", 0.92, 0.0, 0.0, 0.0, interpolation="catmullrom"),
            make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="linear"),
        ],
    }
    # Snout pad with snout
    diss_animators[snout_pad_bone] = {
        "name": "snout_pad_sink", "type": "bone",
        "keyframes": [
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
            make_keyframe("scale", 0.80, 1.0, 1.0, 1.0, interpolation="catmullrom"),
            make_keyframe("scale", 0.90, 0.5, 0.5, 0.5, interpolation="catmullrom"),
            make_keyframe("scale", 0.94, 0.0, 0.0, 0.0, interpolation="catmullrom"),
            make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="linear"),
        ],
    }

    # Eyes are very last — linger
    for eye_bn in (eye_l_bone, eye_r_bone):
        diss_animators[eye_bn] = {
            "name": "eye_linger", "type": "bone",
            "keyframes": [
                make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
                make_keyframe("scale", 0.85, 1.0, 1.0, 1.0, interpolation="catmullrom"),
                make_keyframe("scale", 0.92, 1.4, 1.4, 1.4, interpolation="catmullrom"),
                make_keyframe("scale", 0.97, 0.6, 0.6, 0.6, interpolation="catmullrom"),
                make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="catmullrom"),
            ],
        }
    # Eye slabs — slight blink-out before vanishing
    for slab_bn in (eye_l_a_bone, eye_l_b_bone, eye_r_a_bone, eye_r_b_bone):
        diss_animators[slab_bn] = {
            "name": "eye_slab_blink", "type": "bone",
            "keyframes": [
                make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
                make_keyframe("scale", 0.92, 1.0, 1.0, 1.0, interpolation="catmullrom"),
                make_keyframe("scale", 0.96, 1.2, 1.0, 1.0, interpolation="catmullrom"),
                make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="catmullrom"),
            ],
        }

    # Root sinks backward into ground — Z back, Y down, X tilt back
    diss_animators[bear_root_bone] = {
        "name": "bear_root_sink", "type": "bone",
        "keyframes": [
            make_keyframe("position", 0.0, 0, 0, 0, interpolation="linear"),
            make_keyframe("position", 0.30, 0, 0.5, 0.5, interpolation="catmullrom"),
            make_keyframe("position", 0.55, 0, -1.0, 1.5, interpolation="catmullrom"),
            make_keyframe("position", 0.80, 0, -3.5, 2.5, interpolation="catmullrom"),
            make_keyframe("position", DISS_LEN, 0, -6.0, 3.0, interpolation="catmullrom"),
            make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="linear"),
            make_keyframe("rotation", 0.30, -3, 0, 0, interpolation="catmullrom"),
            make_keyframe("rotation", 0.65, -10, 0, 0, interpolation="catmullrom"),
            make_keyframe("rotation", DISS_LEN, -18, 0, 0, interpolation="catmullrom"),
        ],
    }

    b.add_animation("dissipate", length=DISS_LEN, animators=diss_animators,
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
