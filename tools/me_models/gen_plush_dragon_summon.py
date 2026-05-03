#!/usr/bin/env python3
"""
Generate plush_dragon_summon.bbmodel — Fluffy Mode ME attack.

"Stuffed Apocalypse" — a plush dragon materialises piece-by-piece, idles with
soft breathing/blinking, then comes apart at the seams in a stuffing explosion.

Visuals:
  - Lavender plush body (4 overlapping cubes), cream belly patch (3 slabs)
  - 3-cube round head + 2-cube round snout
  - 2 button eyes (flat slabs)
  - 2 horn pairs (2 cubes each = 4 cubes total)
  - 2 tiny stubby wings (2 feather slabs + 1 membrane = 3 cubes per wing)
  - 3 segment curling tail
  - 6 seam-line slabs at body joins

Textures:
  - tex 0: pale lavender plush velvet (#d8c0f0) with mint #b8ffd0 emissive accents
  - tex 1: warm cream belly with slightly darker stitch stripe

Animations:
  - spawn      (once, 1.3s, override=true)
  - idle       (loop, 4.0s, override=false)
  - dissipate  (once, 0.8s, override=true)
"""
import sys
import math
sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder, make_uuid, make_keyframe, basic_cube_faces, uniform_face,
    png_from_pixels, hex_to_rgba, fill_rect, gradient_radial,
    add_noise_overlay, color_lerp, lerp,
)

OUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/fluffy/me_attacks/plush_dragon_summon.bbmodel"


# ----------------------------- TEXTURES ---------------------------------- #

def make_lavender_plush_texture():
    """Texture 0: pale lavender plush velvet with mint emissive accents (64x64)."""
    W = H = 64
    base = hex_to_rgba("#d8c0f0")        # soft lavender
    plush_shadow = hex_to_rgba("#b098d0")
    deep_fold = hex_to_rgba("#8070b0")
    plush_high = hex_to_rgba("#f0e8ff")
    specular = hex_to_rgba("#ffffff")
    emissive_mint = hex_to_rgba("#b8ffd0")

    pixels = [base] * (W * H)

    # Velvet shading: soft top-down + radial highlight per quadrant
    for y in range(H):
        for x in range(W):
            t_v = y / (H - 1)
            t_h = abs(x - W / 2) / (W / 2)
            tone = (1.0 - t_v) * 0.45 + (1.0 - t_h) * 0.30
            if tone > 0.55:
                pixels[y * W + x] = color_lerp(base, plush_high, (tone - 0.55) * 1.6)
            elif tone < 0.30:
                pixels[y * W + x] = color_lerp(base, plush_shadow, (0.30 - tone) * 2.2)

    # Soft fold / shadow blob in lower-right quadrant for plush volume feel
    for y in range(H):
        for x in range(W):
            d = math.sqrt((x - 48) ** 2 + (y - 48) ** 2)
            if d < 14:
                t = 1.0 - d / 14
                cur = pixels[y * W + x]
                pixels[y * W + x] = color_lerp(cur, deep_fold, t * 0.35)

    # Velvet weave: subtle crosshatch every 5 rows + 5 columns (very fine, low opacity)
    for y in range(0, H, 5):
        for x in range(W):
            cur = pixels[y * W + x]
            pixels[y * W + x] = color_lerp(cur, deep_fold, 0.18)
    for x in range(0, W, 5):
        for y in range(H):
            cur = pixels[y * W + x]
            pixels[y * W + x] = color_lerp(cur, deep_fold, 0.18)

    # Plush noise — fine velvet shimmer
    add_noise_overlay(pixels, W, H, plush_shadow, plush_high, density=0.10, seed=4711)

    # Specular shine — small bright spots top-left of each "panel"
    for cy, cx in [(8, 10), (8, 42), (40, 10), (40, 42)]:
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                d = math.sqrt(dy * dy + dx * dx)
                if d <= 2.5:
                    px = cx + dx
                    py = cy + dy
                    if 0 <= px < W and 0 <= py < H:
                        cur = pixels[py * W + px]
                        pixels[py * W + px] = color_lerp(cur, specular, 1.0 - d / 2.5)

    # Emissive 2px mint green accents at "wing zone" patches (4 small dots)
    # These will be in the UV region wings sample (top-right area)
    for cy, cx in [(4, 50), (4, 56), (10, 53), (16, 51)]:
        for dy in range(-1, 2):
            for dx in range(-1, 2):
                if abs(dy) + abs(dx) <= 1:  # plus sign shape
                    px = cx + dx
                    py = cy + dy
                    if 0 <= px < W and 0 <= py < H:
                        pixels[py * W + px] = emissive_mint

    # Mint accent stripe in lower-right corner (additional wing accent zone)
    for y in range(54, 62):
        for x in range(52, 60):
            if (x + y) % 3 == 0:
                pixels[y * W + x] = emissive_mint

    # Subtle stitching lines — diagonals across the texture (fabric panels)
    for i in range(W):
        y = (i * 3 + 12) % H
        x = i
        cur = pixels[y * W + x]
        pixels[y * W + x] = color_lerp(cur, deep_fold, 0.55)

    return png_from_pixels(pixels, W, H)


def make_belly_cream_texture():
    """Texture 1: warm cream belly with subtle peach shadow + darker centre stripe (64x64)."""
    W = H = 64
    cream = hex_to_rgba("#fff8f0")
    peach_shadow = hex_to_rgba("#ffe0c8")
    stripe_dark = hex_to_rgba("#d0c0b0")
    high = hex_to_rgba("#fffefb")

    pixels = [cream] * (W * H)

    # Soft peach shadow blobs in upper-left and lower-right
    for cy, cx, r in [(12, 14, 14), (52, 50, 16)]:
        for y in range(H):
            for x in range(W):
                d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
                if d < r:
                    t = 1.0 - d / r
                    cur = pixels[y * W + x]
                    pixels[y * W + x] = color_lerp(cur, peach_shadow, t * 0.55)

    # Highlight band in upper-right (light hits here)
    for y in range(H):
        for x in range(W):
            d = math.sqrt((x - 50) ** 2 + (y - 12) ** 2)
            if d < 10:
                t = 1.0 - d / 10
                cur = pixels[y * W + x]
                pixels[y * W + x] = color_lerp(cur, high, t * 0.6)

    # Centre vertical seam stripe — slightly darker stitch line through middle
    cx_line = W // 2
    for y in range(H):
        for dx in (-1, 0, 1):
            x = cx_line + dx
            if 0 <= x < W:
                cur = pixels[y * W + x]
                blend = 0.55 if dx == 0 else 0.25
                pixels[y * W + x] = color_lerp(cur, stripe_dark, blend)

    # Cross stitches every 4 rows along the seam (X marks)
    for y in range(2, H, 4):
        for dx in (-2, -1, 0, 1, 2):
            x = cx_line + dx
            if 0 <= x < W and 0 <= y < H:
                pixels[y * W + x] = stripe_dark
        for dy in (-1, 0, 1):
            yy = y + dy
            if 0 <= yy < H:
                pixels[yy * W + cx_line] = stripe_dark

    # Subtle plush noise — very faint fabric grain
    add_noise_overlay(pixels, W, H, peach_shadow, high, density=0.05, seed=88)

    # Faint ring of stuffing visible — diagonal hatch in corners
    for i in range(W):
        # top-left diag
        y = i // 2
        x = i // 2
        if 0 <= x < 12 and 0 <= y < 12 and (x + y) % 3 == 0:
            cur = pixels[y * W + x]
            pixels[y * W + x] = color_lerp(cur, peach_shadow, 0.5)

    return png_from_pixels(pixels, W, H)


# ----------------------------- BUILD ------------------------------------- #

def build():
    b = Builder("plush_dragon_summon", resolution=(64, 64), visible_box=(6, 6, 0))

    # Add textures (index 0 = lavender plush, 1 = belly cream)
    b.add_texture("plush_dragon_lavender", make_lavender_plush_texture())
    b.add_texture("plush_dragon_belly", make_belly_cream_texture())

    # Convenience UV pickers — pull regions from each texture
    LAV_BODY = lambda: basic_cube_faces(0, 0, 8, 6, 8, tex_index=0)
    LAV_BODY_2 = lambda: basic_cube_faces(8, 8, 6, 4, 6, tex_index=0)
    LAV_HEAD = lambda: basic_cube_faces(16, 0, 6, 6, 6, tex_index=0)
    LAV_SNOUT = lambda: basic_cube_faces(32, 0, 4, 3, 4, tex_index=0)
    LAV_HORN = lambda: basic_cube_faces(0, 40, 1, 3, 1, tex_index=0)
    LAV_WING = lambda: uniform_face(40, 36, 60, 56, tex_index=0)
    LAV_TAIL_LRG = lambda: basic_cube_faces(40, 0, 4, 3, 4, tex_index=0)
    LAV_TAIL_MED = lambda: basic_cube_faces(40, 16, 3, 2, 3, tex_index=0)
    LAV_TAIL_SML = lambda: basic_cube_faces(50, 16, 2, 2, 2, tex_index=0)
    LAV_EYE_BLACK = lambda: uniform_face(2, 50, 6, 54, tex_index=0)  # dark spot in deep_fold zone
    LAV_EAR = lambda: basic_cube_faces(8, 40, 1, 2, 1, tex_index=0)

    BELLY_PATCH = lambda: uniform_face(4, 4, 60, 60, tex_index=1)
    BELLY_SEAM = lambda: uniform_face(28, 0, 36, 64, tex_index=1)  # vertical centre stripe band

    # ------------------------------- BODY ------------------------------- #
    # Plump oval body of 4 overlapping cubes. Y=0 is approx ground level under body.
    # Body sits roughly y in [4, 11], centred on origin in X/Z.
    # Each body cube is its own bone for stuffed-toy deformation.

    body_bones = []
    body_a = b.add_cube("body_a_main",
                        from_=[-3.5, 4.5, -2.5], to_=[3.5, 10.5, 2.5],
                        faces=LAV_BODY(),
                        origin=[0.0, 7.5, 0.0])
    body_b = b.add_cube("body_b_lower",
                        from_=[-3.0, 3.5, -2.0], to_=[3.0, 6.5, 2.0],
                        faces=LAV_BODY_2(),
                        origin=[0.0, 5.0, 0.0])
    body_c = b.add_cube("body_c_upper",
                        from_=[-3.0, 8.5, -2.0], to_=[3.0, 11.5, 2.0],
                        faces=LAV_BODY_2(),
                        origin=[0.0, 10.0, 0.0])
    body_d = b.add_cube("body_d_belly_pooch",
                        from_=[-2.5, 5.0, -3.0], to_=[2.5, 9.0, 0.0],
                        faces=LAV_BODY_2(),
                        origin=[0.0, 7.0, -1.5])

    body_a_bone = make_uuid()
    body_a_group = b.make_group("body_a", origin=[0.0, 7.5, 0.0],
                                 children=[body_a], group_uuid=body_a_bone)
    body_b_bone = make_uuid()
    body_b_group = b.make_group("body_b", origin=[0.0, 5.0, 0.0],
                                 children=[body_b], group_uuid=body_b_bone)
    body_c_bone = make_uuid()
    body_c_group = b.make_group("body_c", origin=[0.0, 10.0, 0.0],
                                 children=[body_c], group_uuid=body_c_bone)
    body_d_bone = make_uuid()
    body_d_group = b.make_group("body_d", origin=[0.0, 7.0, -1.5],
                                 children=[body_d], group_uuid=body_d_bone)

    # ------------------------------- BELLY PATCH ------------------------------- #
    # 3 slabs on front of body (front = -Z). Each its own bone (so they fly off in dissipate).
    belly_top = b.add_cube("belly_top",
                            from_=[-1.8, 8.0, -3.05], to_=[1.8, 9.5, -2.85],
                            faces=BELLY_PATCH(),
                            origin=[0.0, 8.75, -2.95])
    belly_mid = b.add_cube("belly_mid",
                            from_=[-2.0, 6.5, -3.10], to_=[2.0, 8.0, -2.90],
                            faces=BELLY_PATCH(),
                            origin=[0.0, 7.25, -3.0])
    belly_bot = b.add_cube("belly_bot",
                            from_=[-1.6, 5.0, -3.05], to_=[1.6, 6.5, -2.85],
                            faces=BELLY_PATCH(),
                            origin=[0.0, 5.75, -2.95])

    belly_top_bone = make_uuid()
    belly_top_group = b.make_group("belly_top", origin=[0, 8.75, -2.95],
                                    children=[belly_top], group_uuid=belly_top_bone)
    belly_mid_bone = make_uuid()
    belly_mid_group = b.make_group("belly_mid", origin=[0, 7.25, -3.0],
                                    children=[belly_mid], group_uuid=belly_mid_bone)
    belly_bot_bone = make_uuid()
    belly_bot_group = b.make_group("belly_bot", origin=[0, 5.75, -2.95],
                                    children=[belly_bot], group_uuid=belly_bot_bone)

    # ------------------------------- HEAD ------------------------------- #
    # Head sits forward of body (front = -Z), above. Origin ~ (0, 13, -3.5)
    head_a = b.add_cube("head_a_main",
                        from_=[-3.0, 11.5, -6.0], to_=[3.0, 16.5, -1.0],
                        faces=LAV_HEAD(),
                        origin=[0.0, 14.0, -3.5])
    head_b = b.add_cube("head_b_top",
                        from_=[-2.5, 14.0, -5.5], to_=[2.5, 17.5, -1.5],
                        faces=LAV_HEAD(),
                        origin=[0.0, 15.75, -3.5])
    head_c = b.add_cube("head_c_cheek",
                        from_=[-3.2, 12.5, -5.0], to_=[3.2, 15.0, -2.0],
                        faces=LAV_HEAD(),
                        origin=[0.0, 13.75, -3.5])

    head_a_bone = make_uuid()
    head_a_group = b.make_group("head_a", origin=[0, 14.0, -3.5],
                                 children=[head_a], group_uuid=head_a_bone)
    head_b_bone = make_uuid()
    head_b_group = b.make_group("head_b", origin=[0, 15.75, -3.5],
                                 children=[head_b], group_uuid=head_b_bone)
    head_c_bone = make_uuid()
    head_c_group = b.make_group("head_c", origin=[0, 13.75, -3.5],
                                 children=[head_c], group_uuid=head_c_bone)

    # ------------------------------- SNOUT ------------------------------- #
    snout_a = b.add_cube("snout_a",
                          from_=[-1.8, 12.0, -8.0], to_=[1.8, 14.5, -5.5],
                          faces=LAV_SNOUT(),
                          origin=[0.0, 13.25, -6.75])
    snout_b = b.add_cube("snout_b_tip",
                          from_=[-1.4, 12.5, -8.8], to_=[1.4, 14.0, -7.5],
                          faces=LAV_SNOUT(),
                          origin=[0.0, 13.25, -8.15])

    snout_a_bone = make_uuid()
    snout_a_group = b.make_group("snout_a", origin=[0, 13.25, -6.75],
                                  children=[snout_a], group_uuid=snout_a_bone)
    snout_b_bone = make_uuid()
    snout_b_group = b.make_group("snout_b", origin=[0, 13.25, -8.15],
                                  children=[snout_b], group_uuid=snout_b_bone)

    # ------------------------------- EYES ------------------------------- #
    # Flat single-cube button eyes (very thin in Z, slightly forward-facing on cheek)
    eye_l = b.add_cube("eye_left",
                        from_=[-2.4, 14.5, -5.65], to_=[-1.2, 15.7, -5.45],
                        faces=LAV_EYE_BLACK(),
                        origin=[-1.8, 15.1, -5.55])
    eye_r = b.add_cube("eye_right",
                        from_=[1.2, 14.5, -5.65], to_=[2.4, 15.7, -5.45],
                        faces=LAV_EYE_BLACK(),
                        origin=[1.8, 15.1, -5.55])

    eye_l_bone = make_uuid()
    eye_l_group = b.make_group("eye_left", origin=[-1.8, 15.1, -5.55],
                                children=[eye_l], group_uuid=eye_l_bone)
    eye_r_bone = make_uuid()
    eye_r_group = b.make_group("eye_right", origin=[1.8, 15.1, -5.55],
                                children=[eye_r], group_uuid=eye_r_bone)

    # ------------------------------- HORNS / EARS ------------------------------- #
    # 2 horn-clusters on top of head. Each horn = 2 cubes: base + tip.
    # left horn: base at (-2, 17.5, -3.5), tip slightly back & up
    horn_l_base = b.add_cube("horn_l_base",
                              from_=[-2.6, 16.8, -4.2], to_=[-1.4, 18.6, -2.8],
                              faces=LAV_HORN(),
                              origin=[-2.0, 17.5, -3.5])
    horn_l_tip = b.add_cube("horn_l_tip",
                             from_=[-2.4, 18.4, -3.8], to_=[-1.6, 20.0, -2.6],
                             faces=LAV_HORN(),
                             origin=[-2.0, 19.2, -3.2])

    horn_r_base = b.add_cube("horn_r_base",
                              from_=[1.4, 16.8, -4.2], to_=[2.6, 18.6, -2.8],
                              faces=LAV_HORN(),
                              origin=[2.0, 17.5, -3.5])
    horn_r_tip = b.add_cube("horn_r_tip",
                             from_=[1.6, 18.4, -3.8], to_=[2.4, 20.0, -2.6],
                             faces=LAV_HORN(),
                             origin=[2.0, 19.2, -3.2])

    horn_l_bone = make_uuid()
    horn_l_group = b.make_group("horn_l", origin=[-2.0, 17.5, -3.5],
                                 children=[horn_l_base, horn_l_tip], group_uuid=horn_l_bone)
    horn_r_bone = make_uuid()
    horn_r_group = b.make_group("horn_r", origin=[2.0, 17.5, -3.5],
                                 children=[horn_r_base, horn_r_tip], group_uuid=horn_r_bone)

    # ------------------------------- WINGS ------------------------------- #
    # Each wing: 2 feather-substitute flat slabs + 1 membrane fill slab.
    # Wings are SMALL and stubby. Mounted on upper body sides.
    # Left wing on +X side, Right wing on -X side. Wings extend outward + slightly back.
    # Each wing element own bone.

    wing_l_feather_a = b.add_cube("wing_l_feather_a",
                                    from_=[3.4, 9.0, -1.0], to_=[6.5, 11.0, -0.7],
                                    faces=LAV_WING(),
                                    origin=[3.4, 10.0, -0.85])
    wing_l_feather_b = b.add_cube("wing_l_feather_b",
                                    from_=[3.4, 9.0, 0.7], to_=[6.0, 10.5, 1.0],
                                    faces=LAV_WING(),
                                    origin=[3.4, 9.75, 0.85])
    wing_l_membrane = b.add_cube("wing_l_membrane",
                                   from_=[3.4, 8.5, -0.7], to_=[5.5, 11.0, 0.7],
                                   faces=LAV_WING(),
                                   origin=[3.4, 9.75, 0.0])

    wing_l_fa_bone = make_uuid()
    wing_l_fa_group = b.make_group("wing_l_fa", origin=[3.4, 10.0, -0.85],
                                    children=[wing_l_feather_a], group_uuid=wing_l_fa_bone)
    wing_l_fb_bone = make_uuid()
    wing_l_fb_group = b.make_group("wing_l_fb", origin=[3.4, 9.75, 0.85],
                                    children=[wing_l_feather_b], group_uuid=wing_l_fb_bone)
    wing_l_mb_bone = make_uuid()
    wing_l_mb_group = b.make_group("wing_l_mb", origin=[3.4, 9.75, 0.0],
                                    children=[wing_l_membrane], group_uuid=wing_l_mb_bone)

    # Wing root for left wing — pivots whole left wing during flap
    wing_l_root_bone = make_uuid()
    wing_l_root_group = b.make_group("wing_l_root", origin=[3.4, 10.0, 0.0],
                                      children=[wing_l_fa_group, wing_l_fb_group, wing_l_mb_group],
                                      group_uuid=wing_l_root_bone)

    wing_r_feather_a = b.add_cube("wing_r_feather_a",
                                    from_=[-6.5, 9.0, -1.0], to_=[-3.4, 11.0, -0.7],
                                    faces=LAV_WING(),
                                    origin=[-3.4, 10.0, -0.85])
    wing_r_feather_b = b.add_cube("wing_r_feather_b",
                                    from_=[-6.0, 9.0, 0.7], to_=[-3.4, 10.5, 1.0],
                                    faces=LAV_WING(),
                                    origin=[-3.4, 9.75, 0.85])
    wing_r_membrane = b.add_cube("wing_r_membrane",
                                   from_=[-5.5, 8.5, -0.7], to_=[-3.4, 11.0, 0.7],
                                   faces=LAV_WING(),
                                   origin=[-3.4, 9.75, 0.0])

    wing_r_fa_bone = make_uuid()
    wing_r_fa_group = b.make_group("wing_r_fa", origin=[-3.4, 10.0, -0.85],
                                    children=[wing_r_feather_a], group_uuid=wing_r_fa_bone)
    wing_r_fb_bone = make_uuid()
    wing_r_fb_group = b.make_group("wing_r_fb", origin=[-3.4, 9.75, 0.85],
                                    children=[wing_r_feather_b], group_uuid=wing_r_fb_bone)
    wing_r_mb_bone = make_uuid()
    wing_r_mb_group = b.make_group("wing_r_mb", origin=[-3.4, 9.75, 0.0],
                                    children=[wing_r_membrane], group_uuid=wing_r_mb_bone)
    wing_r_root_bone = make_uuid()
    wing_r_root_group = b.make_group("wing_r_root", origin=[-3.4, 10.0, 0.0],
                                      children=[wing_r_fa_group, wing_r_fb_group, wing_r_mb_group],
                                      group_uuid=wing_r_root_bone)

    # ------------------------------- TAIL ------------------------------- #
    # 3 segments decreasing in size, attached at body back (+Z).
    # Hierarchy: tail_1 (large, rooted at body) -> tail_2 -> tail_3
    # Tail curls backward via bone rotations on each segment.
    tail_1 = b.add_cube("tail_1",
                         from_=[-1.5, 5.5, 2.5], to_=[1.5, 8.0, 5.5],
                         faces=LAV_TAIL_LRG(),
                         origin=[0.0, 6.75, 2.5])
    tail_2 = b.add_cube("tail_2",
                         from_=[-1.0, 6.0, 5.5], to_=[1.0, 8.0, 8.0],
                         faces=LAV_TAIL_MED(),
                         origin=[0.0, 7.0, 5.5])
    tail_3 = b.add_cube("tail_3_tip",
                         from_=[-0.7, 6.5, 8.0], to_=[0.7, 8.0, 9.5],
                         faces=LAV_TAIL_SML(),
                         origin=[0.0, 7.25, 8.0])

    tail_3_bone = make_uuid()
    tail_3_group = b.make_group("tail_3", origin=[0, 7.25, 8.0],
                                 children=[tail_3], rotation=[20, 0, 0],
                                 group_uuid=tail_3_bone)
    tail_2_bone = make_uuid()
    tail_2_group = b.make_group("tail_2", origin=[0, 7.0, 5.5],
                                 children=[tail_2, tail_3_group], rotation=[12, 0, 0],
                                 group_uuid=tail_2_bone)
    tail_1_bone = make_uuid()
    tail_1_group = b.make_group("tail_1", origin=[0, 6.75, 2.5],
                                 children=[tail_1, tail_2_group], rotation=[6, 0, 0],
                                 group_uuid=tail_1_bone)

    # ------------------------------- SEAM LINES ------------------------------- #
    # 6 thin flat slabs at body join lines.
    # 1 - body-head neck seam (horizontal across neck)
    # 2 - body-tail seam (horizontal across body back)
    # 3 - body left side seam (vertical along +X side)
    # 4 - body right side seam (vertical along -X side)
    # 5 - left wing-body seam (vertical at +X wing root)
    # 6 - right wing-body seam (vertical at -X wing root)
    seam_neck = b.add_cube("seam_neck",
                             from_=[-2.5, 11.4, -2.0], to_=[2.5, 11.55, 0.5],
                             faces=BELLY_SEAM(),
                             origin=[0.0, 11.475, -0.75])
    seam_tail = b.add_cube("seam_tail",
                             from_=[-1.5, 6.4, 2.4], to_=[1.5, 7.55, 2.55],
                             faces=BELLY_SEAM(),
                             origin=[0.0, 6.975, 2.475])
    seam_side_l = b.add_cube("seam_side_l",
                               from_=[3.45, 5.0, -1.5], to_=[3.55, 10.5, 1.5],
                               faces=BELLY_SEAM(),
                               origin=[3.5, 7.75, 0.0])
    seam_side_r = b.add_cube("seam_side_r",
                               from_=[-3.55, 5.0, -1.5], to_=[-3.45, 10.5, 1.5],
                               faces=BELLY_SEAM(),
                               origin=[-3.5, 7.75, 0.0])
    seam_wing_l = b.add_cube("seam_wing_l",
                               from_=[3.35, 8.5, -1.0], to_=[3.45, 11.0, 1.0],
                               faces=BELLY_SEAM(),
                               origin=[3.4, 9.75, 0.0])
    seam_wing_r = b.add_cube("seam_wing_r",
                               from_=[-3.45, 8.5, -1.0], to_=[-3.35, 11.0, 1.0],
                               faces=BELLY_SEAM(),
                               origin=[-3.4, 9.75, 0.0])

    seam_neck_bone = make_uuid()
    seam_neck_group = b.make_group("seam_neck", origin=[0, 11.475, -0.75],
                                    children=[seam_neck], group_uuid=seam_neck_bone)
    seam_tail_bone = make_uuid()
    seam_tail_group = b.make_group("seam_tail", origin=[0, 6.975, 2.475],
                                    children=[seam_tail], group_uuid=seam_tail_bone)
    seam_side_l_bone = make_uuid()
    seam_side_l_group = b.make_group("seam_side_l", origin=[3.5, 7.75, 0.0],
                                      children=[seam_side_l], group_uuid=seam_side_l_bone)
    seam_side_r_bone = make_uuid()
    seam_side_r_group = b.make_group("seam_side_r", origin=[-3.5, 7.75, 0.0],
                                      children=[seam_side_r], group_uuid=seam_side_r_bone)
    seam_wing_l_bone = make_uuid()
    seam_wing_l_group = b.make_group("seam_wing_l", origin=[3.4, 9.75, 0.0],
                                      children=[seam_wing_l], group_uuid=seam_wing_l_bone)
    seam_wing_r_bone = make_uuid()
    seam_wing_r_group = b.make_group("seam_wing_r", origin=[-3.4, 9.75, 0.0],
                                      children=[seam_wing_r], group_uuid=seam_wing_r_bone)

    # ------------------------------- ROOT ASSEMBLY ------------------------------- #
    root_uuid = b.add_root_group(
        "plush_dragon",
        origin=[0, 0, 0],
        children=[
            body_a_group, body_b_group, body_c_group, body_d_group,
            belly_top_group, belly_mid_group, belly_bot_group,
            head_a_group, head_b_group, head_c_group,
            snout_a_group, snout_b_group,
            eye_l_group, eye_r_group,
            horn_l_group, horn_r_group,
            wing_l_root_group, wing_r_root_group,
            tail_1_group,
            seam_neck_group, seam_tail_group,
            seam_side_l_group, seam_side_r_group,
            seam_wing_l_group, seam_wing_r_group,
        ],
    )

    # =============================================================
    #                       ANIMATIONS
    # =============================================================

    # All bone uuids we'll reference (lookup table)
    bones = {
        "body_a": body_a_bone, "body_b": body_b_bone,
        "body_c": body_c_bone, "body_d": body_d_bone,
        "belly_top": belly_top_bone, "belly_mid": belly_mid_bone, "belly_bot": belly_bot_bone,
        "head_a": head_a_bone, "head_b": head_b_bone, "head_c": head_c_bone,
        "snout_a": snout_a_bone, "snout_b": snout_b_bone,
        "eye_l": eye_l_bone, "eye_r": eye_r_bone,
        "horn_l": horn_l_bone, "horn_r": horn_r_bone,
        "wing_l_root": wing_l_root_bone,
        "wing_l_fa": wing_l_fa_bone, "wing_l_fb": wing_l_fb_bone, "wing_l_mb": wing_l_mb_bone,
        "wing_r_root": wing_r_root_bone,
        "wing_r_fa": wing_r_fa_bone, "wing_r_fb": wing_r_fb_bone, "wing_r_mb": wing_r_mb_bone,
        "tail_1": tail_1_bone, "tail_2": tail_2_bone, "tail_3": tail_3_bone,
        "seam_neck": seam_neck_bone, "seam_tail": seam_tail_bone,
        "seam_side_l": seam_side_l_bone, "seam_side_r": seam_side_r_bone,
        "seam_wing_l": seam_wing_l_bone, "seam_wing_r": seam_wing_r_bone,
    }

    # ---------------- SPAWN (once, 1.3s, override=true) ---------------- #
    # Pieces materialise in stages with bouncy scale-up:
    #   t=0.00 : body cubes scale 0 -> 1.1 -> 1.0
    #   t=0.30 : head cubes scale 0 -> 1.1 -> 1.0
    #   t=0.40 : snout + belly slabs
    #   t=0.50 : wings (unfold from folded position) + horns
    #   t=0.60 : tail uncurls
    #   t=0.70 : eyes pop in (0 -> 1.3 -> 1.0)
    # Seam lines appear staged in lockstep with the body parts they join.

    SPAWN_LEN = 1.3
    spawn = {}

    def staged_scale(start, peak_t=None, settle_t=None, peak=1.1):
        if peak_t is None:
            peak_t = start + 0.18
        if settle_t is None:
            settle_t = start + 0.32
        return [
            make_keyframe("scale", 0.0, 0.001, 0.001, 0.001, interpolation="linear"),
            make_keyframe("scale", max(0.001, start - 0.001), 0.001, 0.001, 0.001, interpolation="linear"),
            make_keyframe("scale", start, 0.05, 0.05, 0.05, interpolation="catmullrom"),
            make_keyframe("scale", peak_t, peak, peak, peak, interpolation="catmullrom"),
            make_keyframe("scale", settle_t, 1.0, 1.0, 1.0, interpolation="catmullrom"),
            make_keyframe("scale", SPAWN_LEN, 1.0, 1.0, 1.0, interpolation="linear"),
        ]

    # Body (4 cubes) — staggered 0.00, 0.05, 0.08, 0.12
    spawn[bones["body_a"]] = {"name": "body_a", "type": "bone",
                                "keyframes": staged_scale(0.00, 0.22, 0.38)}
    spawn[bones["body_b"]] = {"name": "body_b", "type": "bone",
                                "keyframes": staged_scale(0.05, 0.27, 0.42)}
    spawn[bones["body_c"]] = {"name": "body_c", "type": "bone",
                                "keyframes": staged_scale(0.08, 0.30, 0.45)}
    spawn[bones["body_d"]] = {"name": "body_d", "type": "bone",
                                "keyframes": staged_scale(0.12, 0.34, 0.48)}

    # Head (3 cubes) at 0.30
    spawn[bones["head_a"]] = {"name": "head_a", "type": "bone",
                                "keyframes": staged_scale(0.30, 0.50, 0.62)}
    spawn[bones["head_b"]] = {"name": "head_b", "type": "bone",
                                "keyframes": staged_scale(0.34, 0.54, 0.66)}
    spawn[bones["head_c"]] = {"name": "head_c", "type": "bone",
                                "keyframes": staged_scale(0.32, 0.52, 0.64)}

    # Snout at 0.40
    spawn[bones["snout_a"]] = {"name": "snout_a", "type": "bone",
                                 "keyframes": staged_scale(0.40, 0.58, 0.70)}
    spawn[bones["snout_b"]] = {"name": "snout_b", "type": "bone",
                                 "keyframes": staged_scale(0.44, 0.62, 0.74)}

    # Belly patches at 0.40
    spawn[bones["belly_top"]] = {"name": "belly_top", "type": "bone",
                                   "keyframes": staged_scale(0.40, 0.56, 0.68)}
    spawn[bones["belly_mid"]] = {"name": "belly_mid", "type": "bone",
                                   "keyframes": staged_scale(0.42, 0.58, 0.70)}
    spawn[bones["belly_bot"]] = {"name": "belly_bot", "type": "bone",
                                   "keyframes": staged_scale(0.44, 0.60, 0.72)}

    # Horns at 0.50 — pop up with rotation kick
    spawn[bones["horn_l"]] = {"name": "horn_l", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 0.001, 0.001, 0.001, interpolation="linear"),
        make_keyframe("scale", 0.50, 0.05, 0.05, 0.05, interpolation="catmullrom"),
        make_keyframe("scale", 0.66, 1.2, 1.2, 1.2, interpolation="catmullrom"),
        make_keyframe("scale", 0.78, 1.0, 1.0, 1.0, interpolation="catmullrom"),
        make_keyframe("scale", SPAWN_LEN, 1.0, 1.0, 1.0, interpolation="linear"),
        make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="linear"),
        make_keyframe("rotation", 0.50, 0, 0, -8, interpolation="catmullrom"),
        make_keyframe("rotation", 0.78, 0, 0, 4, interpolation="catmullrom"),
        make_keyframe("rotation", SPAWN_LEN, 0, 0, 0, interpolation="catmullrom"),
    ]}
    spawn[bones["horn_r"]] = {"name": "horn_r", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 0.001, 0.001, 0.001, interpolation="linear"),
        make_keyframe("scale", 0.52, 0.05, 0.05, 0.05, interpolation="catmullrom"),
        make_keyframe("scale", 0.68, 1.2, 1.2, 1.2, interpolation="catmullrom"),
        make_keyframe("scale", 0.80, 1.0, 1.0, 1.0, interpolation="catmullrom"),
        make_keyframe("scale", SPAWN_LEN, 1.0, 1.0, 1.0, interpolation="linear"),
        make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="linear"),
        make_keyframe("rotation", 0.52, 0, 0, 8, interpolation="catmullrom"),
        make_keyframe("rotation", 0.80, 0, 0, -4, interpolation="catmullrom"),
        make_keyframe("rotation", SPAWN_LEN, 0, 0, 0, interpolation="catmullrom"),
    ]}

    # Wings unfold at 0.50: start folded inward (rotated -60° around Y so they're tucked
    # against body), unfold to neutral. Plus scale-in.
    spawn[bones["wing_l_root"]] = {"name": "wing_l_root", "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, 0, -70, -10, interpolation="linear"),
        make_keyframe("rotation", 0.50, 0, -70, -10, interpolation="linear"),
        make_keyframe("rotation", 0.75, 0, 5, 8, interpolation="catmullrom"),
        make_keyframe("rotation", 0.95, 0, -3, -2, interpolation="catmullrom"),
        make_keyframe("rotation", SPAWN_LEN, 0, 0, 0, interpolation="catmullrom"),
    ]}
    spawn[bones["wing_r_root"]] = {"name": "wing_r_root", "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, 0, 70, 10, interpolation="linear"),
        make_keyframe("rotation", 0.52, 0, 70, 10, interpolation="linear"),
        make_keyframe("rotation", 0.77, 0, -5, -8, interpolation="catmullrom"),
        make_keyframe("rotation", 0.97, 0, 3, 2, interpolation="catmullrom"),
        make_keyframe("rotation", SPAWN_LEN, 0, 0, 0, interpolation="catmullrom"),
    ]}
    for wkey, t0 in (("wing_l_fa", 0.52), ("wing_l_fb", 0.54), ("wing_l_mb", 0.50),
                       ("wing_r_fa", 0.54), ("wing_r_fb", 0.56), ("wing_r_mb", 0.52)):
        spawn[bones[wkey]] = {"name": wkey, "type": "bone",
                                "keyframes": staged_scale(t0, t0 + 0.14, t0 + 0.26, peak=1.15)}

    # Tail uncurls at 0.60 — segments rotate from tightly curled to natural curve
    spawn[bones["tail_1"]] = {"name": "tail_1", "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, 60, 0, 0, interpolation="linear"),
        make_keyframe("rotation", 0.60, 60, 0, 0, interpolation="linear"),
        make_keyframe("rotation", 0.85, 0, 0, 0, interpolation="catmullrom"),
        make_keyframe("rotation", SPAWN_LEN, 6, 0, 0, interpolation="catmullrom"),
        make_keyframe("scale", 0.0, 0.001, 0.001, 0.001, interpolation="linear"),
        make_keyframe("scale", 0.60, 0.05, 0.05, 0.05, interpolation="catmullrom"),
        make_keyframe("scale", 0.78, 1.1, 1.1, 1.1, interpolation="catmullrom"),
        make_keyframe("scale", SPAWN_LEN, 1.0, 1.0, 1.0, interpolation="catmullrom"),
    ]}
    spawn[bones["tail_2"]] = {"name": "tail_2", "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, 80, 0, 0, interpolation="linear"),
        make_keyframe("rotation", 0.62, 80, 0, 0, interpolation="linear"),
        make_keyframe("rotation", 0.90, 0, 0, 0, interpolation="catmullrom"),
        make_keyframe("rotation", SPAWN_LEN, 12, 0, 0, interpolation="catmullrom"),
    ]}
    spawn[bones["tail_3"]] = {"name": "tail_3", "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, 100, 0, 0, interpolation="linear"),
        make_keyframe("rotation", 0.64, 100, 0, 0, interpolation="linear"),
        make_keyframe("rotation", 0.95, 0, 0, 0, interpolation="catmullrom"),
        make_keyframe("rotation", SPAWN_LEN, 20, 0, 0, interpolation="catmullrom"),
    ]}

    # Eyes pop in at 0.70 — scale 0 -> 1.3 -> 1.0
    spawn[bones["eye_l"]] = {"name": "eye_l", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 0.001, 0.001, 0.001, interpolation="linear"),
        make_keyframe("scale", 0.70, 0.05, 0.05, 0.05, interpolation="catmullrom"),
        make_keyframe("scale", 0.85, 1.3, 1.3, 1.3, interpolation="catmullrom"),
        make_keyframe("scale", 0.97, 0.92, 0.92, 0.92, interpolation="catmullrom"),
        make_keyframe("scale", 1.10, 1.0, 1.0, 1.0, interpolation="catmullrom"),
        make_keyframe("scale", SPAWN_LEN, 1.0, 1.0, 1.0, interpolation="linear"),
    ]}
    spawn[bones["eye_r"]] = {"name": "eye_r", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 0.001, 0.001, 0.001, interpolation="linear"),
        make_keyframe("scale", 0.72, 0.05, 0.05, 0.05, interpolation="catmullrom"),
        make_keyframe("scale", 0.87, 1.3, 1.3, 1.3, interpolation="catmullrom"),
        make_keyframe("scale", 0.99, 0.92, 0.92, 0.92, interpolation="catmullrom"),
        make_keyframe("scale", 1.12, 1.0, 1.0, 1.0, interpolation="catmullrom"),
        make_keyframe("scale", SPAWN_LEN, 1.0, 1.0, 1.0, interpolation="linear"),
    ]}

    # Seam lines appear staged in lockstep with body joins
    seam_stage = {
        "seam_neck": 0.30,        # appears with head
        "seam_tail": 0.60,        # appears with tail
        "seam_side_l": 0.10,      # appears with body
        "seam_side_r": 0.10,
        "seam_wing_l": 0.50,      # appears with wing
        "seam_wing_r": 0.52,
    }
    for skey, t0 in seam_stage.items():
        spawn[bones[skey]] = {"name": skey, "type": "bone",
                                "keyframes": staged_scale(t0, t0 + 0.16, t0 + 0.30, peak=1.05)}

    b.add_animation("spawn", length=SPAWN_LEN, animators=spawn,
                    loop="once", override=True)

    # ---------------- IDLE (loop, 4.0s, override=false) ---------------- #
    # Body breathes (scale up to 1.04 over 4s).
    # Head tilts side to side gently.
    # Wings flap tiny ±5°.
    # Tail swishes slowly back and forth.
    # Eyes blink (Y-scale to 0.1 then back) at ~3s.
    # Horns subtle bob with head.

    IDLE_LEN = 4.0
    idle = {}

    # Body breathing — all 4 body cubes pulse together with rich keyframes (16 per bone)
    for bi, bkey in enumerate(("body_a", "body_b", "body_c", "body_d")):
        ph = bi * 0.05  # tiny phase per cube for soft jiggle
        idle[bones[bkey]] = {"name": bkey, "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="catmullrom"),
            make_keyframe("scale", 0.5 + ph, 1.02, 1.01, 1.02, interpolation="catmullrom"),
            make_keyframe("scale", 1.0 + ph, 1.04, 1.02, 1.04, interpolation="catmullrom"),
            make_keyframe("scale", 1.5 + ph, 1.03, 1.015, 1.03, interpolation="catmullrom"),
            make_keyframe("scale", 2.0, 1.0, 1.0, 1.0, interpolation="catmullrom"),
            make_keyframe("scale", 2.5 + ph, 1.02, 1.01, 1.02, interpolation="catmullrom"),
            make_keyframe("scale", 3.0 + ph, 1.04, 1.02, 1.04, interpolation="catmullrom"),
            make_keyframe("scale", 3.5 + ph, 1.02, 1.01, 1.02, interpolation="catmullrom"),
            make_keyframe("scale", IDLE_LEN, 1.0, 1.0, 1.0, interpolation="catmullrom"),
            make_keyframe("position", 0.0, 0, 0, 0, interpolation="catmullrom"),
            make_keyframe("position", 1.0 + ph, 0, 0.04, 0, interpolation="catmullrom"),
            make_keyframe("position", 2.0, 0, 0, 0, interpolation="catmullrom"),
            make_keyframe("position", 3.0 + ph, 0, 0.04, 0, interpolation="catmullrom"),
            make_keyframe("position", IDLE_LEN, 0, 0, 0, interpolation="catmullrom"),
            make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="catmullrom"),
            make_keyframe("rotation", 1.0 + ph, 0.5, 0, 0.5, interpolation="catmullrom"),
            make_keyframe("rotation", 2.0, 0, 0, 0, interpolation="catmullrom"),
            make_keyframe("rotation", 3.0 + ph, -0.5, 0, -0.5, interpolation="catmullrom"),
            make_keyframe("rotation", IDLE_LEN, 0, 0, 0, interpolation="catmullrom"),
        ]}
        # Sort to keep keyframes ordered per channel
        idle[bones[bkey]]["keyframes"].sort(key=lambda k: (k["channel"], k["time"]))

    # Belly patches breathe slightly less — also rich keyframes
    for bi, bkey in enumerate(("belly_top", "belly_mid", "belly_bot")):
        ph = bi * 0.07
        idle[bones[bkey]] = {"name": bkey, "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="catmullrom"),
            make_keyframe("scale", 0.5 + ph, 1.01, 1.0, 1.01, interpolation="catmullrom"),
            make_keyframe("scale", 1.0 + ph, 1.02, 1.0, 1.02, interpolation="catmullrom"),
            make_keyframe("scale", 1.5 + ph, 1.01, 1.0, 1.01, interpolation="catmullrom"),
            make_keyframe("scale", 2.0, 0.99, 1.0, 0.99, interpolation="catmullrom"),
            make_keyframe("scale", 2.5 + ph, 1.0, 1.0, 1.0, interpolation="catmullrom"),
            make_keyframe("scale", 3.0 + ph, 1.02, 1.0, 1.02, interpolation="catmullrom"),
            make_keyframe("scale", 3.5 + ph, 1.01, 1.0, 1.01, interpolation="catmullrom"),
            make_keyframe("scale", IDLE_LEN, 1.0, 1.0, 1.0, interpolation="catmullrom"),
            make_keyframe("position", 0.0, 0, 0, 0, interpolation="catmullrom"),
            make_keyframe("position", 1.0 + ph, 0, 0, -0.04, interpolation="catmullrom"),
            make_keyframe("position", 2.0, 0, 0, 0, interpolation="catmullrom"),
            make_keyframe("position", 3.0 + ph, 0, 0, -0.04, interpolation="catmullrom"),
            make_keyframe("position", IDLE_LEN, 0, 0, 0, interpolation="catmullrom"),
        ]}
        idle[bones[bkey]]["keyframes"].sort(key=lambda k: (k["channel"], k["time"]))

    # Head tilts side to side. Apply rotation to all 3 head cubes (matched).
    head_kfs = [
        make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="catmullrom"),
        make_keyframe("rotation", 1.0, 2, -3, 4, interpolation="catmullrom"),
        make_keyframe("rotation", 2.0, -1, 2, -2, interpolation="catmullrom"),
        make_keyframe("rotation", 3.0, 2, 4, 3, interpolation="catmullrom"),
        make_keyframe("rotation", IDLE_LEN, 0, 0, 0, interpolation="catmullrom"),
        make_keyframe("position", 0.0, 0, 0, 0, interpolation="catmullrom"),
        make_keyframe("position", 1.5, 0, 0.15, 0, interpolation="catmullrom"),
        make_keyframe("position", 3.0, 0, -0.05, 0, interpolation="catmullrom"),
        make_keyframe("position", IDLE_LEN, 0, 0, 0, interpolation="catmullrom"),
    ]
    for hkey in ("head_a", "head_b", "head_c"):
        idle[bones[hkey]] = {"name": hkey, "type": "bone",
                               "keyframes": [dict(k) for k in head_kfs] }
        # Need fresh uuids so animations independent
        for k in idle[bones[hkey]]["keyframes"]:
            k["uuid"] = make_uuid()

    # Snout follows head tilt (subtle)
    for skey in ("snout_a", "snout_b"):
        snout_kfs = [
            make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="catmullrom"),
            make_keyframe("rotation", 1.0, 1, -2, 2, interpolation="catmullrom"),
            make_keyframe("rotation", 2.0, -0.5, 1, -1, interpolation="catmullrom"),
            make_keyframe("rotation", 3.0, 1, 2, 1.5, interpolation="catmullrom"),
            make_keyframe("rotation", IDLE_LEN, 0, 0, 0, interpolation="catmullrom"),
        ]
        idle[bones[skey]] = {"name": skey, "type": "bone", "keyframes": snout_kfs}

    # Horns — subtle bob with head
    horn_kfs = [
        make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="catmullrom"),
        make_keyframe("rotation", 1.0, 1.5, 0, 1, interpolation="catmullrom"),
        make_keyframe("rotation", 2.0, -0.5, 0, -0.5, interpolation="catmullrom"),
        make_keyframe("rotation", 3.0, 1, 0, 1, interpolation="catmullrom"),
        make_keyframe("rotation", IDLE_LEN, 0, 0, 0, interpolation="catmullrom"),
    ]
    for hkey in ("horn_l", "horn_r"):
        idle[bones[hkey]] = {"name": hkey, "type": "bone",
                               "keyframes": [dict(k) for k in horn_kfs]}
        for k in idle[bones[hkey]]["keyframes"]:
            k["uuid"] = make_uuid()

    # Wings tiny flapping ±5° on Z axis (raise/lower)
    idle[bones["wing_l_root"]] = {"name": "wing_l_root", "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="catmullrom"),
        make_keyframe("rotation", 0.5, 0, 0, 5, interpolation="catmullrom"),
        make_keyframe("rotation", 1.0, 0, 0, -3, interpolation="catmullrom"),
        make_keyframe("rotation", 1.5, 0, 0, 4, interpolation="catmullrom"),
        make_keyframe("rotation", 2.0, 0, 0, -2, interpolation="catmullrom"),
        make_keyframe("rotation", 2.5, 0, 0, 5, interpolation="catmullrom"),
        make_keyframe("rotation", 3.0, 0, 0, -3, interpolation="catmullrom"),
        make_keyframe("rotation", 3.5, 0, 0, 4, interpolation="catmullrom"),
        make_keyframe("rotation", IDLE_LEN, 0, 0, 0, interpolation="catmullrom"),
    ]}
    idle[bones["wing_r_root"]] = {"name": "wing_r_root", "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="catmullrom"),
        make_keyframe("rotation", 0.5, 0, 0, -5, interpolation="catmullrom"),
        make_keyframe("rotation", 1.0, 0, 0, 3, interpolation="catmullrom"),
        make_keyframe("rotation", 1.5, 0, 0, -4, interpolation="catmullrom"),
        make_keyframe("rotation", 2.0, 0, 0, 2, interpolation="catmullrom"),
        make_keyframe("rotation", 2.5, 0, 0, -5, interpolation="catmullrom"),
        make_keyframe("rotation", 3.0, 0, 0, 3, interpolation="catmullrom"),
        make_keyframe("rotation", 3.5, 0, 0, -4, interpolation="catmullrom"),
        make_keyframe("rotation", IDLE_LEN, 0, 0, 0, interpolation="catmullrom"),
    ]}

    # Tail swishes slowly back and forth (rotation Y on tail_1) — rich keyframes
    idle[bones["tail_1"]] = {"name": "tail_1", "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, 6, 0, 0, interpolation="catmullrom"),
        make_keyframe("rotation", 0.5, 5, 6, 0, interpolation="catmullrom"),
        make_keyframe("rotation", 1.0, 4, 12, 0, interpolation="catmullrom"),
        make_keyframe("rotation", 1.5, 6, 8, 0, interpolation="catmullrom"),
        make_keyframe("rotation", 2.0, 8, 0, 0, interpolation="catmullrom"),
        make_keyframe("rotation", 2.5, 6, -8, 0, interpolation="catmullrom"),
        make_keyframe("rotation", 3.0, 4, -12, 0, interpolation="catmullrom"),
        make_keyframe("rotation", 3.5, 5, -6, 0, interpolation="catmullrom"),
        make_keyframe("rotation", IDLE_LEN, 6, 0, 0, interpolation="catmullrom"),
        make_keyframe("position", 0.0, 0, 0, 0, interpolation="catmullrom"),
        make_keyframe("position", 1.0, 0, 0.1, 0, interpolation="catmullrom"),
        make_keyframe("position", 2.0, 0, 0, 0, interpolation="catmullrom"),
        make_keyframe("position", 3.0, 0, 0.1, 0, interpolation="catmullrom"),
        make_keyframe("position", IDLE_LEN, 0, 0, 0, interpolation="catmullrom"),
    ]}
    idle[bones["tail_2"]] = {"name": "tail_2", "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, 12, 0, 0, interpolation="catmullrom"),
        make_keyframe("rotation", 0.5, 11, 4, 0, interpolation="catmullrom"),
        make_keyframe("rotation", 1.0, 10, 8, 0, interpolation="catmullrom"),
        make_keyframe("rotation", 1.5, 12, 4, 0, interpolation="catmullrom"),
        make_keyframe("rotation", 2.0, 14, 0, 0, interpolation="catmullrom"),
        make_keyframe("rotation", 2.5, 12, -4, 0, interpolation="catmullrom"),
        make_keyframe("rotation", 3.0, 10, -8, 0, interpolation="catmullrom"),
        make_keyframe("rotation", 3.5, 11, -4, 0, interpolation="catmullrom"),
        make_keyframe("rotation", IDLE_LEN, 12, 0, 0, interpolation="catmullrom"),
    ]}
    idle[bones["tail_3"]] = {"name": "tail_3", "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, 20, 0, 0, interpolation="catmullrom"),
        make_keyframe("rotation", 0.5, 19, 3, 0, interpolation="catmullrom"),
        make_keyframe("rotation", 1.0, 18, 6, 0, interpolation="catmullrom"),
        make_keyframe("rotation", 1.5, 20, 3, 0, interpolation="catmullrom"),
        make_keyframe("rotation", 2.0, 22, 0, 0, interpolation="catmullrom"),
        make_keyframe("rotation", 2.5, 20, -3, 0, interpolation="catmullrom"),
        make_keyframe("rotation", 3.0, 18, -6, 0, interpolation="catmullrom"),
        make_keyframe("rotation", 3.5, 19, -3, 0, interpolation="catmullrom"),
        make_keyframe("rotation", IDLE_LEN, 20, 0, 0, interpolation="catmullrom"),
    ]}

    # Eyes blink — squash Y-scale at t=1.5s and t=3.0s. Each blink 0.10s.
    blink_kfs_l = [
        make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
        make_keyframe("scale", 1.40, 1.0, 1.0, 1.0, interpolation="linear"),
        make_keyframe("scale", 1.50, 1.0, 0.10, 1.0, interpolation="catmullrom"),
        make_keyframe("scale", 1.60, 1.0, 1.0, 1.0, interpolation="linear"),
        make_keyframe("scale", 2.90, 1.0, 1.0, 1.0, interpolation="linear"),
        make_keyframe("scale", 3.00, 1.0, 0.10, 1.0, interpolation="catmullrom"),
        make_keyframe("scale", 3.10, 1.0, 1.0, 1.0, interpolation="linear"),
        make_keyframe("scale", IDLE_LEN, 1.0, 1.0, 1.0, interpolation="linear"),
    ]
    blink_kfs_r = [dict(k) for k in blink_kfs_l]
    for k in blink_kfs_r:
        k["uuid"] = make_uuid()
        # offset right eye blink by tiny amount for "natural" feel
        k["time"] = max(0.0, k["time"] + 0.02 if 1.4 < k["time"] < 1.7 or 2.9 < k["time"] < 3.2 else k["time"])
    idle[bones["eye_l"]] = {"name": "eye_l", "type": "bone", "keyframes": blink_kfs_l}
    idle[bones["eye_r"]] = {"name": "eye_r", "type": "bone", "keyframes": blink_kfs_r}

    # Seam lines: subtle pulse with breathing — rich keyframes
    for skey, off in (("seam_neck", 0.0), ("seam_tail", 0.5),
                        ("seam_side_l", 0.2), ("seam_side_r", 0.2),
                        ("seam_wing_l", 0.7), ("seam_wing_r", 0.7)):
        kfs = [
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="catmullrom"),
            make_keyframe("scale", 0.5 + off * 0.3, 1.025, 1.0, 1.025, interpolation="catmullrom"),
            make_keyframe("scale", 1.0 + off * 0.3, 1.05, 1.0, 1.05, interpolation="catmullrom"),
            make_keyframe("scale", 1.5 + off * 0.3, 1.025, 1.0, 1.025, interpolation="catmullrom"),
            make_keyframe("scale", 2.0, 1.0, 1.0, 1.0, interpolation="catmullrom"),
            make_keyframe("scale", 2.5 + off * 0.3, 1.025, 1.0, 1.025, interpolation="catmullrom"),
            make_keyframe("scale", 3.0 + off * 0.3, 1.05, 1.0, 1.05, interpolation="catmullrom"),
            make_keyframe("scale", 3.5 + off * 0.3, 1.025, 1.0, 1.025, interpolation="catmullrom"),
            make_keyframe("scale", IDLE_LEN, 1.0, 1.0, 1.0, interpolation="catmullrom"),
            make_keyframe("position", 0.0, 0, 0, 0, interpolation="catmullrom"),
            make_keyframe("position", 1.0 + off * 0.3, 0, 0.02, 0, interpolation="catmullrom"),
            make_keyframe("position", 2.0, 0, 0, 0, interpolation="catmullrom"),
            make_keyframe("position", 3.0 + off * 0.3, 0, -0.02, 0, interpolation="catmullrom"),
            make_keyframe("position", IDLE_LEN, 0, 0, 0, interpolation="catmullrom"),
            make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="catmullrom"),
            make_keyframe("rotation", 1.5, 0.5, 0.5, 0, interpolation="catmullrom"),
            make_keyframe("rotation", 3.0, -0.5, -0.5, 0, interpolation="catmullrom"),
            make_keyframe("rotation", IDLE_LEN, 0, 0, 0, interpolation="catmullrom"),
        ]
        kfs.sort(key=lambda k: (k["channel"], k["time"]))
        idle[bones[skey]] = {"name": skey, "type": "bone", "keyframes": kfs}

    b.add_animation("idle", length=IDLE_LEN, animators=idle,
                    loop="loop", override=False)

    # ---------------- DISSIPATE (once, 0.8s, override=true) ---------------- #
    # Stuffed toy comes apart at the seams.
    # 0.0s: still intact
    # 0.1s: seams pop (scale up briefly then to 0)
    # 0.2s: belly patches fly outward
    # 0.3s: wings flap once then separate
    # 0.4s: eyes pop off (random directions)
    # 0.5s: tail uncurls and flies off
    # 0.6s: head + horns fly up
    # 0.7s: body cubes scatter
    # 0.8s: all gone

    DISS_LEN = 0.8
    diss = {}

    # Seams: pop briefly then collapse to 0
    for skey, dx, dy, dz in (("seam_neck", 0, 1.5, -1),
                                ("seam_tail", 0, 0.5, 2.5),
                                ("seam_side_l", 2.5, 0, 0),
                                ("seam_side_r", -2.5, 0, 0),
                                ("seam_wing_l", 3.0, 1, 0.5),
                                ("seam_wing_r", -3.0, 1, 0.5)):
        diss[bones[skey]] = {"name": skey, "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
            make_keyframe("scale", 0.10, 1.4, 1.4, 1.4, interpolation="catmullrom"),
            make_keyframe("scale", 0.25, 0.6, 0.6, 0.6, interpolation="catmullrom"),
            make_keyframe("scale", 0.40, 0.0, 0.0, 0.0, interpolation="catmullrom"),
            make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="linear"),
            make_keyframe("position", 0.0, 0, 0, 0, interpolation="linear"),
            make_keyframe("position", 0.25, dx, dy, dz, interpolation="catmullrom"),
            make_keyframe("position", DISS_LEN, dx * 2, dy * 2, dz * 2, interpolation="catmullrom"),
        ]}

    # Belly patches fly outward in cloud (stuffing)
    for bkey, dx, dy, dz in (("belly_top", -1.5, 4.0, -3.0),
                                ("belly_mid", 1.0, 3.0, -3.5),
                                ("belly_bot", -0.8, 2.0, -2.5)):
        diss[bones[bkey]] = {"name": bkey, "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, 0, 0, 0, interpolation="linear"),
            make_keyframe("position", 0.20, 0, 0, 0, interpolation="linear"),
            make_keyframe("position", 0.50, dx * 1.2, dy, dz, interpolation="catmullrom"),
            make_keyframe("position", DISS_LEN, dx * 2, dy * 1.4 - 2, dz * 1.5, interpolation="catmullrom"),
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
            make_keyframe("scale", 0.20, 1.2, 1.2, 1.2, interpolation="catmullrom"),
            make_keyframe("scale", 0.55, 1.4, 1.4, 1.4, interpolation="catmullrom"),
            make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="catmullrom"),
            make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="linear"),
            make_keyframe("rotation", DISS_LEN, dx * 90, dy * 60, dz * 90, interpolation="linear"),
        ]}

    # Wings flap once then separate
    diss[bones["wing_l_root"]] = {"name": "wing_l_root", "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="linear"),
        make_keyframe("rotation", 0.10, 0, 0, -25, interpolation="catmullrom"),
        make_keyframe("rotation", 0.25, 0, 0, 35, interpolation="catmullrom"),
        make_keyframe("rotation", 0.40, 0, -20, -10, interpolation="catmullrom"),
        make_keyframe("rotation", DISS_LEN, 0, -180, 60, interpolation="catmullrom"),
        make_keyframe("position", 0.0, 0, 0, 0, interpolation="linear"),
        make_keyframe("position", 0.30, 0, 0.5, 0, interpolation="catmullrom"),
        make_keyframe("position", DISS_LEN, 4.0, -3.5, 1.5, interpolation="catmullrom"),
        make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
        make_keyframe("scale", 0.40, 1.0, 1.0, 1.0, interpolation="linear"),
        make_keyframe("scale", DISS_LEN, 0.3, 0.3, 0.3, interpolation="catmullrom"),
    ]}
    diss[bones["wing_r_root"]] = {"name": "wing_r_root", "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="linear"),
        make_keyframe("rotation", 0.10, 0, 0, 25, interpolation="catmullrom"),
        make_keyframe("rotation", 0.25, 0, 0, -35, interpolation="catmullrom"),
        make_keyframe("rotation", 0.40, 0, 20, 10, interpolation="catmullrom"),
        make_keyframe("rotation", DISS_LEN, 0, 180, -60, interpolation="catmullrom"),
        make_keyframe("position", 0.0, 0, 0, 0, interpolation="linear"),
        make_keyframe("position", 0.30, 0, 0.5, 0, interpolation="catmullrom"),
        make_keyframe("position", DISS_LEN, -4.0, -3.5, 1.5, interpolation="catmullrom"),
        make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
        make_keyframe("scale", 0.40, 1.0, 1.0, 1.0, interpolation="linear"),
        make_keyframe("scale", DISS_LEN, 0.3, 0.3, 0.3, interpolation="catmullrom"),
    ]}
    # Wing feathers/membrane within each wing also fly off slightly
    for wkey, dx, dy, dz in (("wing_l_fa", 1.5, 1.5, -1.0),
                                ("wing_l_fb", 1.0, 0.5, 1.5),
                                ("wing_l_mb", 0.5, 1.0, 0.0),
                                ("wing_r_fa", -1.5, 1.5, -1.0),
                                ("wing_r_fb", -1.0, 0.5, 1.5),
                                ("wing_r_mb", -0.5, 1.0, 0.0)):
        diss[bones[wkey]] = {"name": wkey, "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, 0, 0, 0, interpolation="linear"),
            make_keyframe("position", 0.40, 0, 0, 0, interpolation="linear"),
            make_keyframe("position", DISS_LEN, dx, dy, dz, interpolation="catmullrom"),
            make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="linear"),
            make_keyframe("rotation", DISS_LEN, dx * 30, dz * 30, dy * 30, interpolation="catmullrom"),
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
            make_keyframe("scale", 0.40, 1.0, 1.0, 1.0, interpolation="linear"),
            make_keyframe("scale", DISS_LEN, 0.4, 0.4, 0.4, interpolation="catmullrom"),
        ]}

    # Eyes pop off in random directions
    diss[bones["eye_l"]] = {"name": "eye_l", "type": "bone", "keyframes": [
        make_keyframe("position", 0.0, 0, 0, 0, interpolation="linear"),
        make_keyframe("position", 0.40, 0, 0, 0, interpolation="linear"),
        make_keyframe("position", 0.55, -1.5, 1.0, -2.5, interpolation="catmullrom"),
        make_keyframe("position", DISS_LEN, -3.5, -2.5, -4.0, interpolation="catmullrom"),
        make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
        make_keyframe("scale", 0.40, 1.0, 1.0, 1.0, interpolation="linear"),
        make_keyframe("scale", 0.55, 1.5, 1.5, 1.5, interpolation="catmullrom"),
        make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="catmullrom"),
        make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="linear"),
        make_keyframe("rotation", DISS_LEN, 540, 360, 720, interpolation="linear"),
    ]}
    diss[bones["eye_r"]] = {"name": "eye_r", "type": "bone", "keyframes": [
        make_keyframe("position", 0.0, 0, 0, 0, interpolation="linear"),
        make_keyframe("position", 0.42, 0, 0, 0, interpolation="linear"),
        make_keyframe("position", 0.57, 1.8, 1.5, -2.0, interpolation="catmullrom"),
        make_keyframe("position", DISS_LEN, 4.0, -1.0, -3.5, interpolation="catmullrom"),
        make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
        make_keyframe("scale", 0.42, 1.0, 1.0, 1.0, interpolation="linear"),
        make_keyframe("scale", 0.57, 1.5, 1.5, 1.5, interpolation="catmullrom"),
        make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="catmullrom"),
        make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="linear"),
        make_keyframe("rotation", DISS_LEN, -480, -270, -650, interpolation="linear"),
    ]}

    # Tail uncurls and flies off backward
    diss[bones["tail_1"]] = {"name": "tail_1", "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, 6, 0, 0, interpolation="linear"),
        make_keyframe("rotation", 0.30, -20, 0, 0, interpolation="catmullrom"),
        make_keyframe("rotation", DISS_LEN, -45, 60, 0, interpolation="catmullrom"),
        make_keyframe("position", 0.0, 0, 0, 0, interpolation="linear"),
        make_keyframe("position", 0.50, 0, 1.5, 2.0, interpolation="catmullrom"),
        make_keyframe("position", DISS_LEN, 1.0, 0.5, 5.5, interpolation="catmullrom"),
        make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
        make_keyframe("scale", 0.50, 1.0, 1.0, 1.0, interpolation="linear"),
        make_keyframe("scale", DISS_LEN, 0.3, 0.3, 0.3, interpolation="catmullrom"),
    ]}
    diss[bones["tail_2"]] = {"name": "tail_2", "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, 12, 0, 0, interpolation="linear"),
        make_keyframe("rotation", 0.30, -10, 0, 0, interpolation="catmullrom"),
        make_keyframe("rotation", DISS_LEN, -40, -45, 0, interpolation="catmullrom"),
    ]}
    diss[bones["tail_3"]] = {"name": "tail_3", "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, 20, 0, 0, interpolation="linear"),
        make_keyframe("rotation", 0.30, -5, 0, 0, interpolation="catmullrom"),
        make_keyframe("rotation", DISS_LEN, -30, 60, 0, interpolation="catmullrom"),
    ]}

    # Head cubes fly up + apart
    for hkey, dx, dy, dz in (("head_a", 0.5, 4.0, -1.0),
                                ("head_b", -0.5, 4.5, 0.5),
                                ("head_c", 0.0, 3.5, -1.5)):
        diss[bones[hkey]] = {"name": hkey, "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, 0, 0, 0, interpolation="linear"),
            make_keyframe("position", 0.55, 0, 0.5, 0, interpolation="catmullrom"),
            make_keyframe("position", DISS_LEN, dx, dy, dz, interpolation="catmullrom"),
            make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="linear"),
            make_keyframe("rotation", DISS_LEN, dx * 60, dy * 30, dz * 60, interpolation="catmullrom"),
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
            make_keyframe("scale", 0.55, 1.0, 1.0, 1.0, interpolation="linear"),
            make_keyframe("scale", DISS_LEN, 0.4, 0.4, 0.4, interpolation="catmullrom"),
        ]}

    # Snout flies off (forward)
    for skey, dx, dy, dz in (("snout_a", 0.0, 1.0, -3.5),
                                ("snout_b", 0.5, 0.5, -5.0)):
        diss[bones[skey]] = {"name": skey, "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, 0, 0, 0, interpolation="linear"),
            make_keyframe("position", 0.45, 0, 0, 0, interpolation="linear"),
            make_keyframe("position", DISS_LEN, dx, dy, dz, interpolation="catmullrom"),
            make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="linear"),
            make_keyframe("rotation", DISS_LEN, 180, 90, 45, interpolation="catmullrom"),
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
            make_keyframe("scale", 0.45, 1.0, 1.0, 1.0, interpolation="linear"),
            make_keyframe("scale", DISS_LEN, 0.3, 0.3, 0.3, interpolation="catmullrom"),
        ]}

    # Horns fly up & spinning
    diss[bones["horn_l"]] = {"name": "horn_l", "type": "bone", "keyframes": [
        make_keyframe("position", 0.0, 0, 0, 0, interpolation="linear"),
        make_keyframe("position", 0.35, 0, 0, 0, interpolation="linear"),
        make_keyframe("position", DISS_LEN, -2.0, 5.0, -2.0, interpolation="catmullrom"),
        make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="linear"),
        make_keyframe("rotation", DISS_LEN, 360, 180, 540, interpolation="linear"),
        make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
        make_keyframe("scale", 0.35, 1.0, 1.0, 1.0, interpolation="linear"),
        make_keyframe("scale", DISS_LEN, 0.2, 0.2, 0.2, interpolation="catmullrom"),
    ]}
    diss[bones["horn_r"]] = {"name": "horn_r", "type": "bone", "keyframes": [
        make_keyframe("position", 0.0, 0, 0, 0, interpolation="linear"),
        make_keyframe("position", 0.37, 0, 0, 0, interpolation="linear"),
        make_keyframe("position", DISS_LEN, 2.0, 5.0, -2.0, interpolation="catmullrom"),
        make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="linear"),
        make_keyframe("rotation", DISS_LEN, -360, -180, -540, interpolation="linear"),
        make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
        make_keyframe("scale", 0.37, 1.0, 1.0, 1.0, interpolation="linear"),
        make_keyframe("scale", DISS_LEN, 0.2, 0.2, 0.2, interpolation="catmullrom"),
    ]}

    # Body cubes scatter outward (last to go) — rich pre-burst tremor + scatter
    for bkey, dx, dy, dz in (("body_a", 0.0, -1.0, 0.0),
                                ("body_b", -2.0, -2.5, 1.5),
                                ("body_c", 2.0, 1.5, -1.0),
                                ("body_d", 0.5, -3.0, -2.5)):
        diss[bones[bkey]] = {"name": bkey, "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, 0, 0, 0, interpolation="linear"),
            make_keyframe("position", 0.15, 0.05, 0.0, 0.05, interpolation="catmullrom"),
            make_keyframe("position", 0.30, -0.05, 0.0, -0.05, interpolation="catmullrom"),
            make_keyframe("position", 0.45, 0.05, 0.0, 0.05, interpolation="catmullrom"),
            make_keyframe("position", 0.55, 0, 0, 0, interpolation="catmullrom"),
            make_keyframe("position", 0.65, dx * 0.25, dy * 0.25, dz * 0.25, interpolation="catmullrom"),
            make_keyframe("position", 0.70, dx * 0.5, dy * 0.5, dz * 0.5, interpolation="catmullrom"),
            make_keyframe("position", 0.75, dx * 0.7, dy * 0.7, dz * 0.7, interpolation="catmullrom"),
            make_keyframe("position", DISS_LEN, dx, dy, dz, interpolation="catmullrom"),
            make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="linear"),
            make_keyframe("rotation", 0.30, 2, 0, -2, interpolation="catmullrom"),
            make_keyframe("rotation", 0.55, -2, 0, 2, interpolation="catmullrom"),
            make_keyframe("rotation", 0.70, dx * 22, dz * 22, dy * 22, interpolation="catmullrom"),
            make_keyframe("rotation", DISS_LEN, dx * 45, dz * 45, dy * 45, interpolation="catmullrom"),
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
            make_keyframe("scale", 0.20, 1.02, 0.98, 1.02, interpolation="catmullrom"),
            make_keyframe("scale", 0.40, 0.98, 1.02, 0.98, interpolation="catmullrom"),
            make_keyframe("scale", 0.55, 1.05, 1.05, 1.05, interpolation="catmullrom"),
            make_keyframe("scale", 0.65, 1.08, 1.08, 1.08, interpolation="catmullrom"),
            make_keyframe("scale", 0.70, 1.1, 1.1, 1.1, interpolation="catmullrom"),
            make_keyframe("scale", 0.75, 0.9, 0.9, 0.9, interpolation="catmullrom"),
            make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="catmullrom"),
        ]}

    b.add_animation("dissipate", length=DISS_LEN, animators=diss,
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
