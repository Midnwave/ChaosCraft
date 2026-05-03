#!/usr/bin/env python3
"""
Generate plushie_hydra_good_boy.bbmodel - Fluffy Mode ME attack.

PLUSHIE HYDRA SUMMON - "Good Boy x 3"
A three-headed plush hydra materialising. Each head a different pastel colour
(pink, mint, lavender). Each neck is a separate bone chain of 3 segments.
Each head has button eyes, smile mouth, and small ear nubs. Heads look in
different directions on different periods (2.5s / 3.5s / 4.5s).

Geometry:
  - Body: 4 overlapping cubes forming round plush body, each own bone
  - 3 neck chains: each 3 cube segments (decreasing width), each segment own bone
  - 3 head bone groups, each containing:
      head globe (3 overlapping cubes) + 2 button eyes + 2 mouth slabs (V smile)
      + 2 ear/horn nubs
  - 3 tail nubs on body, each own bone
  - Ground shadow: 4 overlapping flat slabs at Y=0 (texture 1)

Textures:
  - Texture 0 (64x64): pastel pink plush
  - Texture 1 (64x64): top half mint green, bottom half lavender, plus ground shadow region

Animations:
  - spawn (1.4s, once, override=true)
  - idle (6.0s, loop, override=false) - INDEPENDENT three-neck motion
  - dissipate (1.0s, once, override=true)
"""
import sys
import math
sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder, make_uuid, make_keyframe, basic_cube_faces, uniform_face,
    png_from_pixels, hex_to_rgba, fill_rect, gradient_radial,
    add_noise_overlay, color_lerp, lerp,
)

OUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/fluffy/me_attacks/plushie_hydra_good_boy.bbmodel"


# ----------------------------- TEXTURES ---------------------------------- #

def make_pink_plush_texture():
    """Texture 0: pastel pink plush 64x64 (used for body, neck 1, head 1)."""
    W = H = 64
    base = hex_to_rgba("#ffc8d8")
    deeper = hex_to_rgba("#f0a0b8")
    shadow = hex_to_rgba("#d07090")
    highlight = hex_to_rgba("#fff0f8")
    emissive = hex_to_rgba("#ffe0ec")

    pixels = [base] * (W * H)

    # Soft radial-ish gradient: brighter top, softer at sides
    for y in range(H):
        for x in range(W):
            t_v = y / (H - 1)
            t_h = abs(x - W / 2) / (W / 2)
            tone = (1.0 - t_v) * 0.55 + (1.0 - t_h) * 0.35
            if tone > 0.6:
                pixels[y * W + x] = color_lerp(base, highlight, (tone - 0.6) * 1.6)
            elif tone > 0.4:
                pixels[y * W + x] = color_lerp(base, deeper, (0.6 - tone) * 0.6)
            else:
                pixels[y * W + x] = color_lerp(base, shadow, (0.4 - tone) * 1.5)

    # Crosshatch fabric weave (every 4 rows + every 4 cols, faint)
    for y in range(0, H, 4):
        for x in range(W):
            cur = pixels[y * W + x]
            pixels[y * W + x] = color_lerp(cur, shadow, 0.30)
    for x in range(0, W, 4):
        for y in range(H):
            cur = pixels[y * W + x]
            pixels[y * W + x] = color_lerp(cur, shadow, 0.28)

    # Plush noise (warm pinks)
    add_noise_overlay(pixels, W, H, deeper, highlight, density=0.13, seed=3001)

    # Specular shine spots (4 quadrants)
    for cy, cx in [(8, 12), (8, 44), (40, 12), (40, 44)]:
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                d = math.sqrt(dy * dy + dx * dx)
                if d <= 2.5:
                    px = cx + dx
                    py = cy + dy
                    if 0 <= px < W and 0 <= py < H:
                        cur = pixels[py * W + px]
                        pixels[py * W + px] = color_lerp(cur, highlight, 1.0 - d / 2.5)

    # Emissive warm pink dots
    for cy, cx in [(6, 10), (6, 46), (38, 10), (38, 46)]:
        for dy in range(-1, 2):
            for dx in range(-1, 2):
                px = cx + dx
                py = cy + dy
                if 0 <= px < W and 0 <= py < H:
                    pixels[py * W + px] = emissive

    # Faint stitching diagonals
    for i in range(W):
        y = (i + 8) % H
        x = i
        cur = pixels[y * W + x]
        pixels[y * W + x] = color_lerp(cur, shadow, 0.45)

    return png_from_pixels(pixels, W, H)


def make_mint_lavender_ground_texture():
    """Texture 1: top half (rows 0..31) mint green plush,
       bottom half (rows 32..63) lavender plush.
       The ground shadow uses the small region around (rows 0..15, cols 32..63)
       which we paint with cream/grey for the ground shadow specifically:
       to keep simple, ground shadow UV uses a small dedicated patch
       at rows 0..7, cols 0..15 painted #f0f0f0.
       Actually we'll paint a dedicated grey patch at rows 56..63, cols 0..15
       (corner) and have ground UV reference that.
       Hm — easier: paint mint top 32, lavender bottom 32, then overpaint a
       16x16 grey square at top-left corner [0..15, 0..15] for ground.
    """
    W = H = 64
    mint = hex_to_rgba("#b8ffd0")
    mint_deep = hex_to_rgba("#80d8a8")
    mint_shadow = hex_to_rgba("#50a070")
    mint_high = hex_to_rgba("#e0fff0")
    mint_emit = hex_to_rgba("#d0ffe0")

    lav = hex_to_rgba("#d8b8ff")
    lav_deep = hex_to_rgba("#b088e0")
    lav_shadow = hex_to_rgba("#7050a0")
    lav_high = hex_to_rgba("#f0e0ff")
    lav_emit = hex_to_rgba("#e8d0ff")

    ground = hex_to_rgba("#f0f0f0")
    ground_dark = hex_to_rgba("#a0a0a0")

    pixels = [mint] * (W * H)

    # Paint MINT top half (rows 0..31)
    for y in range(0, 32):
        for x in range(W):
            t_v = y / 31.0
            t_h = abs(x - W / 2) / (W / 2)
            tone = (1.0 - t_v) * 0.55 + (1.0 - t_h) * 0.35
            if tone > 0.6:
                pixels[y * W + x] = color_lerp(mint, mint_high, (tone - 0.6) * 1.6)
            elif tone > 0.4:
                pixels[y * W + x] = color_lerp(mint, mint_deep, (0.6 - tone) * 0.6)
            else:
                pixels[y * W + x] = color_lerp(mint, mint_shadow, (0.4 - tone) * 1.5)

    # Paint LAVENDER bottom half (rows 32..63)
    for y in range(32, 64):
        for x in range(W):
            t_v = (y - 32) / 31.0
            t_h = abs(x - W / 2) / (W / 2)
            tone = (1.0 - t_v) * 0.55 + (1.0 - t_h) * 0.35
            if tone > 0.6:
                pixels[y * W + x] = color_lerp(lav, lav_high, (tone - 0.6) * 1.6)
            elif tone > 0.4:
                pixels[y * W + x] = color_lerp(lav, lav_deep, (0.6 - tone) * 0.6)
            else:
                pixels[y * W + x] = color_lerp(lav, lav_shadow, (0.4 - tone) * 1.5)

    # Crosshatch fabric weave (whole texture)
    for y in range(0, H, 4):
        for x in range(W):
            cur = pixels[y * W + x]
            shadow_c = mint_shadow if y < 32 else lav_shadow
            pixels[y * W + x] = color_lerp(cur, shadow_c, 0.30)
    for x in range(0, W, 4):
        for y in range(H):
            cur = pixels[y * W + x]
            shadow_c = mint_shadow if y < 32 else lav_shadow
            pixels[y * W + x] = color_lerp(cur, shadow_c, 0.28)

    # Plush noise per half
    add_noise_overlay(pixels, W, H, mint_deep, mint_high, density=0.07, seed=4002)
    add_noise_overlay(pixels, W, H, lav_deep, lav_high, density=0.07, seed=4003)

    # Specular shine on each half
    for cy, cx in [(6, 12), (6, 44)]:
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                d = math.sqrt(dy * dy + dx * dx)
                if d <= 2.5:
                    px = cx + dx
                    py = cy + dy
                    if 0 <= px < W and 0 <= py < H:
                        cur = pixels[py * W + px]
                        pixels[py * W + px] = color_lerp(cur, mint_high, 1.0 - d / 2.5)
    for cy, cx in [(38, 12), (38, 44)]:
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                d = math.sqrt(dy * dy + dx * dx)
                if d <= 2.5:
                    px = cx + dx
                    py = cy + dy
                    if 0 <= px < W and 0 <= py < H:
                        cur = pixels[py * W + px]
                        pixels[py * W + px] = color_lerp(cur, lav_high, 1.0 - d / 2.5)

    # Emissive dots
    for cy, cx in [(4, 10), (4, 46), (28, 10), (28, 46)]:
        for dy in range(-1, 2):
            for dx in range(-1, 2):
                px = cx + dx
                py = cy + dy
                if 0 <= px < W and 0 <= py < H:
                    pixels[py * W + px] = mint_emit
    for cy, cx in [(36, 10), (36, 46), (60, 10), (60, 46)]:
        for dy in range(-1, 2):
            for dx in range(-1, 2):
                px = cx + dx
                py = cy + dy
                if 0 <= px < W and 0 <= py < H:
                    pixels[py * W + px] = lav_emit

    # Ground shadow patch in a dedicated 16x16 corner that won't be used
    # for plush body parts — overwrite top-left corner [rows 0..15, cols 48..63]
    # since none of our plush UV samples that small corner.
    # We'll paint a soft circular grey patch.
    cx, cy = 56.0, 8.0
    for y in range(0, 16):
        for x in range(48, 64):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            if d <= 7.5:
                t = d / 7.5
                pixels[y * W + x] = color_lerp(ground, ground_dark, t * 0.6)
            else:
                pixels[y * W + x] = ground_dark
    # Soft inner ring darker for shadow effect
    for y in range(0, 16):
        for x in range(48, 64):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            if d > 5.0 and d < 7.5:
                cur = pixels[y * W + x]
                pixels[y * W + x] = color_lerp(cur, ground_dark, 0.4)

    return png_from_pixels(pixels, W, H)


# ----------------------------- BUILD ------------------------------------- #

def build():
    b = Builder("plushie_hydra_good_boy", resolution=(64, 64), visible_box=(6, 8, 0))

    # Add textures (index 0 pink, index 1 mint+lavender+ground)
    b.add_texture("plushie_pink", make_pink_plush_texture())
    b.add_texture("plushie_mint_lavender_ground", make_mint_lavender_ground_texture())

    # UV regions on texture 1:
    #  - Mint plush:     rows 0..31      -> y range [0, 32)
    #  - Lavender plush: rows 32..63     -> y range [32, 64)
    #  - Ground shadow:  rows 0..15, cols 48..63 -> [u=48..63, v=0..15]
    # We'll choose cube faces on texture 1 to sample either the top half (mint)
    # or bottom half (lavender) by passing v ranges within the appropriate band.
    # uniform_face takes (u1, v1, u2, v2). We'll keep all U within 0..47 to avoid
    # stepping into the ground patch corner.

    # Helper UV rects (we just want the FACE to sample SOMEWHERE in the right band):
    MINT_RECT = (0, 0, 32, 28)        # mint band
    LAV_RECT = (0, 36, 32, 60)        # lavender band
    GROUND_RECT = (48, 0, 64, 16)     # ground shadow patch

    # Bone bookkeeping
    body_bone_uuids = []   # list of body cube bone uuids
    tail_bone_uuids = []   # list of tail bone uuids
    neck_chains = []       # list of 3 dicts: each {"segments": [seg1_bone, seg2, seg3], "head": head_bone, "head_parts": dict}

    # ---------------- BODY (4 overlapping cubes, each own bone) ---------------- #
    # Body sits roughly y=2..6, hovering above ground.
    body_specs = [
        ("body_core",   [-3.0, 2.0, -2.5], [3.0, 5.0, 2.5], [0, 3.5, 0]),
        ("body_belly",  [-2.7, 1.5, -2.2], [2.7, 4.0, 2.2], [0, 2.7, 0]),
        ("body_back",   [-2.5, 3.0, -2.8], [2.5, 5.5, 2.0], [0, 4.2, -0.4]),
        ("body_haunch", [-3.2, 2.5, -1.0], [3.2, 5.2, 2.7], [0, 3.8, 0.85]),
    ]
    for name, fr, to, origin in body_specs:
        cube_uuid = b.add_cube(
            name, from_=fr, to_=to,
            faces=basic_cube_faces(0, 0, 6, 3, 5, tex_index=0),
            origin=origin,
        )
        bone_uuid = make_uuid()
        bone_group = b.make_group(name, origin=origin, children=[cube_uuid], group_uuid=bone_uuid)
        b.outliner.append(bone_group)
        body_bone_uuids.append(bone_uuid)

    # ---------------- 3 TAIL NUBS on body rear (each own bone) ---------------- #
    # Tails poke out from the back side (positive Z back).
    tail_specs = [
        ("tail_a", [-1.4, 3.0, 2.5], [-0.6, 3.8, 3.6], [-1.0, 3.4, 3.05]),
        ("tail_b", [-0.4, 2.7, 2.5], [0.4, 3.5, 3.5], [0.0, 3.1, 3.0]),
        ("tail_c", [0.6, 3.0, 2.5], [1.4, 3.8, 3.6], [1.0, 3.4, 3.05]),
    ]
    for name, fr, to, origin in tail_specs:
        cube_uuid = b.add_cube(
            name, from_=fr, to_=to,
            faces=basic_cube_faces(20, 30, 1, 1, 1, tex_index=0),
            origin=origin,
        )
        bone_uuid = make_uuid()
        # tail bone origin sits at base attached to body so it can wag
        bone_origin = [origin[0], origin[1], 2.5]
        bone_group = b.make_group(name, origin=bone_origin, children=[cube_uuid], group_uuid=bone_uuid)
        b.outliner.append(bone_group)
        tail_bone_uuids.append(bone_uuid)

    # ---------------- 3 NECK CHAINS + HEADS ---------------- #
    # Each neck attaches to top of body at differing X offsets, curves outward.
    # Neck colours / texture mapping: neck 0 = pink (tex 0), neck 1 = mint (tex 1, top),
    # neck 2 = lavender (tex 1, bottom).
    #
    # Neck chain: 3 segments stacked vertically with decreasing width.
    # Segment lengths approx 1.6 each; total neck height ~4.8 above body top (y=5).
    # Heads sit at top (y ~ 9.8 to 12.8 roughly). Different X/Z lean per neck.

    NECK_CONFIGS = [
        {  # Neck 0: PINK — leans LEFT
            "name": "pink",
            "tex_index": 0,
            "uv_rect": (0, 0, 32, 24),  # generic pink UV
            "base_x": -1.2, "base_z": -0.6,
            "lean_x": -2.5, "lean_z": -0.3,
            "head_color_label": "pink",
        },
        {  # Neck 1: MINT — leans CENTER-FORWARD
            "name": "mint",
            "tex_index": 1,
            "uv_rect": MINT_RECT,
            "base_x": 0.0, "base_z": -0.6,
            "lean_x": 0.0, "lean_z": -2.0,
            "head_color_label": "mint",
        },
        {  # Neck 2: LAVENDER — leans RIGHT
            "name": "lavender",
            "tex_index": 1,
            "uv_rect": LAV_RECT,
            "base_x": 1.2, "base_z": -0.6,
            "lean_x": 2.5, "lean_z": -0.3,
            "head_color_label": "lavender",
        },
    ]

    for cfg in NECK_CONFIGS:
        nname = cfg["name"]
        tidx = cfg["tex_index"]
        uvr = cfg["uv_rect"]
        bx = cfg["base_x"]
        bz = cfg["base_z"]
        lx = cfg["lean_x"]
        lz = cfg["lean_z"]

        # Three neck segments — interpolate from (bx, 5, bz) up to (bx + lx, 9.8, bz + lz)
        # Segment pivots are at the BOTTOM of each segment so rotations bend naturally.
        seg_origins = []
        seg_widths = [0.85, 0.7, 0.55]   # decreasing
        seg_heights = [1.7, 1.6, 1.5]
        # World start point of segment 0
        sx0 = bx
        sy0 = 5.0
        sz0 = bz
        # Direction increments per segment (so chain reaches end point)
        dx = lx / 3.0
        dz = lz / 3.0

        # Build segment cubes in WORLD space; each segment gets its own bone with origin
        # at the BOTTOM-CENTRE of that segment (so rotations bend the chain).
        # To make the chain hierarchical (parent->child rotations cascade), we will
        # nest seg2 inside seg1's bone, seg3 inside seg2's bone, head inside seg3's bone.

        # Segment cubes (we nest later)
        seg_cube_uuids = []
        for s in range(3):
            sw = seg_widths[s]
            sh = seg_heights[s]
            ox = sx0 + dx * s
            oy = sy0 + sum(seg_heights[:s])
            oz = sz0 + dz * s
            cube_uuid = b.add_cube(
                f"neck_{nname}_seg{s}",
                from_=[ox - sw, oy, oz - sw],
                to_=[ox + sw, oy + sh, oz + sw],
                faces=uniform_face(*uvr, tex_index=tidx),
                origin=[ox, oy, oz],
            )
            seg_cube_uuids.append((cube_uuid, [ox, oy, oz]))
            seg_origins.append([ox, oy, oz])

        # Head parts — situated at top of segment 2
        hx = sx0 + dx * 3
        hy = sy0 + sum(seg_heights)  # top of last segment
        hz = sz0 + dz * 3

        # Head globe: 3 overlapping cubes
        head_cube_uuids = []
        head_globe_specs = [
            (f"head_{nname}_globe_a", [hx - 1.4, hy + 0.0, hz - 1.4], [hx + 1.4, hy + 2.4, hz + 1.4], [hx, hy + 1.2, hz]),
            (f"head_{nname}_globe_b", [hx - 1.2, hy + 0.3, hz - 1.6], [hx + 1.2, hy + 2.2, hz + 1.6], [hx, hy + 1.25, hz]),
            (f"head_{nname}_globe_c", [hx - 1.6, hy + 0.4, hz - 1.2], [hx + 1.6, hy + 2.0, hz + 1.2], [hx, hy + 1.2, hz]),
        ]
        for name, fr, to, origin in head_globe_specs:
            cu = b.add_cube(name, from_=fr, to_=to,
                            faces=uniform_face(*uvr, tex_index=tidx),
                            origin=origin)
            head_cube_uuids.append(cu)

        # Eyes (2 small flat cubes — black-ish flat — but we paint via the same tex region)
        # We'll position eyes on the FRONT of the head (negative Z side).
        eye_uuid_left = b.add_cube(
            f"head_{nname}_eye_left",
            from_=[hx - 0.9, hy + 1.4, hz - 1.65],
            to_=[hx - 0.3, hy + 2.0, hz - 1.55],
            faces=uniform_face(20, 18, 28, 26, tex_index=0),  # use a darker pink shadow patch
            origin=[hx - 0.6, hy + 1.7, hz - 1.6],
        )
        eye_uuid_right = b.add_cube(
            f"head_{nname}_eye_right",
            from_=[hx + 0.3, hy + 1.4, hz - 1.65],
            to_=[hx + 0.9, hy + 2.0, hz - 1.55],
            faces=uniform_face(20, 18, 28, 26, tex_index=0),
            origin=[hx + 0.6, hy + 1.7, hz - 1.6],
        )

        # Mouth slabs forming smile/V (2 slabs angled inward)
        mouth_left = b.add_cube(
            f"head_{nname}_mouth_left",
            from_=[hx - 0.7, hy + 0.5, hz - 1.65],
            to_=[hx - 0.05, hy + 0.7, hz - 1.55],
            faces=uniform_face(40, 40, 52, 44, tex_index=0),
            origin=[hx - 0.05, hy + 0.6, hz - 1.6],
            rotation=[0, 0, 18],
        )
        mouth_right = b.add_cube(
            f"head_{nname}_mouth_right",
            from_=[hx + 0.05, hy + 0.5, hz - 1.65],
            to_=[hx + 0.7, hy + 0.7, hz - 1.55],
            faces=uniform_face(40, 40, 52, 44, tex_index=0),
            origin=[hx + 0.05, hy + 0.6, hz - 1.6],
            rotation=[0, 0, -18],
        )

        # Two small ear/horn nubs on top of head
        ear_left = b.add_cube(
            f"head_{nname}_ear_left",
            from_=[hx - 1.2, hy + 2.3, hz - 0.4],
            to_=[hx - 0.6, hy + 2.95, hz + 0.4],
            faces=uniform_face(*uvr, tex_index=tidx),
            origin=[hx - 0.9, hy + 2.6, hz],
        )
        ear_right = b.add_cube(
            f"head_{nname}_ear_right",
            from_=[hx + 0.6, hy + 2.3, hz - 0.4],
            to_=[hx + 1.2, hy + 2.95, hz + 0.4],
            faces=uniform_face(*uvr, tex_index=tidx),
            origin=[hx + 0.9, hy + 2.6, hz],
        )

        # Eye bones (each own bone for blink/scale)
        eye_left_bone = make_uuid()
        eye_left_group = b.make_group(
            f"eye_left_{nname}", origin=[hx - 0.6, hy + 1.7, hz - 1.6],
            children=[eye_uuid_left], group_uuid=eye_left_bone)
        eye_right_bone = make_uuid()
        eye_right_group = b.make_group(
            f"eye_right_{nname}", origin=[hx + 0.6, hy + 1.7, hz - 1.6],
            children=[eye_uuid_right], group_uuid=eye_right_bone)

        # Mouth bones
        mouth_left_bone = make_uuid()
        mouth_left_group = b.make_group(
            f"mouth_left_{nname}", origin=[hx - 0.05, hy + 0.6, hz - 1.6],
            children=[mouth_left], group_uuid=mouth_left_bone)
        mouth_right_bone = make_uuid()
        mouth_right_group = b.make_group(
            f"mouth_right_{nname}", origin=[hx + 0.05, hy + 0.6, hz - 1.6],
            children=[mouth_right], group_uuid=mouth_right_bone)

        # Ear bones
        ear_left_bone = make_uuid()
        ear_left_group = b.make_group(
            f"ear_left_{nname}", origin=[hx - 0.9, hy + 2.6, hz],
            children=[ear_left], group_uuid=ear_left_bone)
        ear_right_bone = make_uuid()
        ear_right_group = b.make_group(
            f"ear_right_{nname}", origin=[hx + 0.9, hy + 2.6, hz],
            children=[ear_right], group_uuid=ear_right_bone)

        # Head bone — origin at base of head (top of seg2)
        head_bone_uuid = make_uuid()
        head_group = b.make_group(
            f"head_{nname}", origin=[hx, hy, hz],
            children=head_cube_uuids + [
                eye_left_group, eye_right_group,
                mouth_left_group, mouth_right_group,
                ear_left_group, ear_right_group,
            ],
            group_uuid=head_bone_uuid,
        )

        # Now build neck segments as a HIERARCHY:
        # seg2 bone (origin = bottom of seg2) wraps seg2 cube + head_group
        # seg1 bone (origin = bottom of seg1) wraps seg1 cube + seg2 bone
        # seg0 bone (origin = bottom of seg0) wraps seg0 cube + seg1 bone
        # Bottom of segment s is at y = sy0 + sum(seg_heights[:s]).
        seg_bone_uuids = []
        # Build innermost first (seg 2)
        seg2_bone = make_uuid()
        seg2_origin = [sx0 + dx * 2, sy0 + seg_heights[0] + seg_heights[1], sz0 + dz * 2]
        seg2_group = b.make_group(
            f"neck_{nname}_seg2_bone",
            origin=seg2_origin,
            children=[seg_cube_uuids[2][0], head_group],
            group_uuid=seg2_bone,
        )
        seg_bone_uuids.append(seg2_bone)

        seg1_bone = make_uuid()
        seg1_origin = [sx0 + dx * 1, sy0 + seg_heights[0], sz0 + dz * 1]
        seg1_group = b.make_group(
            f"neck_{nname}_seg1_bone",
            origin=seg1_origin,
            children=[seg_cube_uuids[1][0], seg2_group],
            group_uuid=seg1_bone,
        )
        seg_bone_uuids.append(seg1_bone)

        seg0_bone = make_uuid()
        seg0_origin = [sx0, sy0, sz0]
        seg0_group = b.make_group(
            f"neck_{nname}_seg0_bone",
            origin=seg0_origin,
            children=[seg_cube_uuids[0][0], seg1_group],
            group_uuid=seg0_bone,
        )
        seg_bone_uuids.append(seg0_bone)

        # seg_bone_uuids is currently [seg2, seg1, seg0]; reverse for clarity
        seg_bone_uuids = list(reversed(seg_bone_uuids))  # now [seg0, seg1, seg2]

        # Add seg0 root group to outliner (top-level)
        b.outliner.append(seg0_group)

        neck_chains.append({
            "name": nname,
            "seg0": seg_bone_uuids[0],
            "seg1": seg_bone_uuids[1],
            "seg2": seg_bone_uuids[2],
            "head": head_bone_uuid,
            "eye_left": eye_left_bone,
            "eye_right": eye_right_bone,
            "mouth_left": mouth_left_bone,
            "mouth_right": mouth_right_bone,
            "ear_left": ear_left_bone,
            "ear_right": ear_right_bone,
            "base_pos": [sx0, sy0, sz0],
            "head_pos": [hx, hy, hz],
        })

    # ---------------- GROUND SHADOW (4 overlapping flat slabs at Y=0) ---------------- #
    ground_specs = [
        ("ground_a", [-3.5, 0.0, -3.0], [3.5, 0.15, 3.0], [0, 0.075, 0]),
        ("ground_b", [-2.8, 0.0, -3.6], [2.8, 0.15, 3.6], [0, 0.075, 0]),
        ("ground_c", [-3.6, 0.0, -2.4], [3.6, 0.15, 2.4], [0, 0.075, 0]),
        ("ground_d", [-2.4, 0.0, -2.4], [2.4, 0.15, 2.4], [0, 0.075, 0]),
    ]
    ground_children = []
    for name, fr, to, origin in ground_specs:
        cube_uuid = b.add_cube(
            name, from_=fr, to_=to,
            faces=uniform_face(*GROUND_RECT, tex_index=1),
            origin=origin,
        )
        ground_children.append(cube_uuid)

    ground_bone_uuid = make_uuid()
    ground_group = b.make_group(
        "ground_shadow", origin=[0, 0, 0],
        children=ground_children, group_uuid=ground_bone_uuid,
    )
    b.outliner.append(ground_group)

    # ============== ANIMATIONS ============== #

    # ---------------- SPAWN (1.4s, once, override=true) ---------------- #
    # 0.0-0.2s: invisible (scale 0)
    # 0.2s: body emerges from ground (scale -> full)
    # 0.5s, 0.65s, 0.80s: necks extend (each scale 0->1 with bouncy)
    # head materialises at neck-extension-end + 0.1s
    # Eyes pop in with scale bounce
    # Tails unfurl

    SPAWN_LEN = 1.4
    spawn_animators = {}

    # Body cubes: emerge from ground (scale 0 -> 1.1 -> 1.0)
    for i, body_bone in enumerate(body_bone_uuids):
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0, interpolation="linear"),
            make_keyframe("scale", 0.20, 0.0, 0.0, 0.0, interpolation="linear"),
            make_keyframe("scale", 0.35 + i * 0.02, 0.6, 0.4, 0.6, interpolation="catmullrom"),
            make_keyframe("scale", 0.50 + i * 0.02, 1.15, 1.10, 1.15, interpolation="catmullrom"),
            make_keyframe("scale", 0.65 + i * 0.02, 1.0, 1.0, 1.0, interpolation="catmullrom"),
            make_keyframe("scale", SPAWN_LEN, 1.0, 1.0, 1.0, interpolation="linear"),
            # Position: start submerged
            make_keyframe("position", 0.0, 0, -3, 0, interpolation="linear"),
            make_keyframe("position", 0.20, 0, -3, 0, interpolation="linear"),
            make_keyframe("position", 0.50, 0, 0.3, 0, interpolation="catmullrom"),
            make_keyframe("position", SPAWN_LEN, 0, 0, 0, interpolation="catmullrom"),
        ]
        spawn_animators[body_bone] = {"name": f"body_{i}", "type": "bone", "keyframes": kfs}

    # Neck extension stagger
    NECK_START = [0.50, 0.65, 0.80]
    HEAD_TIMES = [t + 0.10 for t in NECK_START]
    for n_idx, neck in enumerate(neck_chains):
        nstart = NECK_START[n_idx]
        for s_idx, key in enumerate(("seg0", "seg1", "seg2")):
            seg_kfs = [
                make_keyframe("scale", 0.0, 0, 0, 0, interpolation="linear"),
                make_keyframe("scale", nstart - 0.01, 0.0, 0.0, 0.0, interpolation="linear"),
                make_keyframe("scale", nstart + 0.04 + s_idx * 0.04, 1.1, 0.4, 1.1, interpolation="catmullrom"),
                make_keyframe("scale", nstart + 0.12 + s_idx * 0.04, 0.9, 1.15, 0.9, interpolation="catmullrom"),
                make_keyframe("scale", nstart + 0.20 + s_idx * 0.04, 1.0, 1.0, 1.0, interpolation="catmullrom"),
                make_keyframe("scale", SPAWN_LEN, 1.0, 1.0, 1.0, interpolation="linear"),
                # Slight wobble rotation as neck rises
                make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="linear"),
                make_keyframe("rotation", nstart + 0.05, 0, 0, (n_idx - 1) * 5, interpolation="catmullrom"),
                make_keyframe("rotation", nstart + 0.20, 0, 0, (n_idx - 1) * -3, interpolation="catmullrom"),
                make_keyframe("rotation", SPAWN_LEN, 0, 0, 0, interpolation="catmullrom"),
            ]
            spawn_animators[neck[key]] = {"name": f"neck_{neck['name']}_{key}", "type": "bone", "keyframes": seg_kfs}

        # Head materialise
        htime = HEAD_TIMES[n_idx]
        head_kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0, interpolation="linear"),
            make_keyframe("scale", htime - 0.01, 0.0, 0.0, 0.0, interpolation="linear"),
            make_keyframe("scale", htime + 0.10, 1.4, 1.4, 1.4, interpolation="catmullrom"),
            make_keyframe("scale", htime + 0.20, 0.85, 0.85, 0.85, interpolation="catmullrom"),
            make_keyframe("scale", htime + 0.30, 1.05, 1.05, 1.05, interpolation="catmullrom"),
            make_keyframe("scale", htime + 0.40, 1.0, 1.0, 1.0, interpolation="catmullrom"),
            make_keyframe("scale", SPAWN_LEN, 1.0, 1.0, 1.0, interpolation="linear"),
        ]
        spawn_animators[neck["head"]] = {"name": f"head_{neck['name']}", "type": "bone", "keyframes": head_kfs}

        # Eyes pop in with bigger bounce after head materialises
        for eye_key in ("eye_left", "eye_right"):
            eye_kfs = [
                make_keyframe("scale", 0.0, 0, 0, 0, interpolation="linear"),
                make_keyframe("scale", htime + 0.20, 0.0, 0.0, 0.0, interpolation="linear"),
                make_keyframe("scale", htime + 0.32, 1.6, 1.6, 1.6, interpolation="catmullrom"),
                make_keyframe("scale", htime + 0.42, 0.7, 0.7, 0.7, interpolation="catmullrom"),
                make_keyframe("scale", htime + 0.52, 1.1, 1.1, 1.1, interpolation="catmullrom"),
                make_keyframe("scale", htime + 0.60, 1.0, 1.0, 1.0, interpolation="catmullrom"),
                make_keyframe("scale", SPAWN_LEN, 1.0, 1.0, 1.0, interpolation="linear"),
            ]
            spawn_animators[neck[eye_key]] = {"name": f"{eye_key}_{neck['name']}", "type": "bone", "keyframes": eye_kfs}

        # Mouth slabs slide into position
        for mouth_key in ("mouth_left", "mouth_right"):
            mouth_kfs = [
                make_keyframe("scale", 0.0, 0, 0, 0, interpolation="linear"),
                make_keyframe("scale", htime + 0.30, 0.0, 0.0, 0.0, interpolation="linear"),
                make_keyframe("scale", htime + 0.45, 1.2, 0.6, 1.2, interpolation="catmullrom"),
                make_keyframe("scale", htime + 0.55, 1.0, 1.0, 1.0, interpolation="catmullrom"),
                make_keyframe("scale", SPAWN_LEN, 1.0, 1.0, 1.0, interpolation="linear"),
            ]
            spawn_animators[neck[mouth_key]] = {"name": f"{mouth_key}_{neck['name']}", "type": "bone", "keyframes": mouth_kfs}

        # Ears pop with slight delay
        for ear_key in ("ear_left", "ear_right"):
            ear_kfs = [
                make_keyframe("scale", 0.0, 0, 0, 0, interpolation="linear"),
                make_keyframe("scale", htime + 0.40, 0.0, 0.0, 0.0, interpolation="linear"),
                make_keyframe("scale", htime + 0.55, 1.4, 1.4, 1.4, interpolation="catmullrom"),
                make_keyframe("scale", htime + 0.70, 1.0, 1.0, 1.0, interpolation="catmullrom"),
                make_keyframe("scale", SPAWN_LEN, 1.0, 1.0, 1.0, interpolation="linear"),
            ]
            spawn_animators[neck[ear_key]] = {"name": f"{ear_key}_{neck['name']}", "type": "bone", "keyframes": ear_kfs}

    # Tails unfurl (rotate from tucked into out)
    for t_idx, tail_bone in enumerate(tail_bone_uuids):
        t_phase = 0.85 + t_idx * 0.05
        tail_kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0, interpolation="linear"),
            make_keyframe("scale", 0.6, 0.0, 0.0, 0.0, interpolation="linear"),
            make_keyframe("scale", t_phase, 1.0, 1.0, 1.0, interpolation="catmullrom"),
            make_keyframe("scale", SPAWN_LEN, 1.0, 1.0, 1.0, interpolation="linear"),
            make_keyframe("rotation", 0.0, -50, 0, 0, interpolation="linear"),
            make_keyframe("rotation", t_phase, -50, 0, 0, interpolation="linear"),
            make_keyframe("rotation", t_phase + 0.2, 15, 0, (t_idx - 1) * 10, interpolation="catmullrom"),
            make_keyframe("rotation", SPAWN_LEN, 0, 0, 0, interpolation="catmullrom"),
        ]
        spawn_animators[tail_bone] = {"name": f"tail_{t_idx}", "type": "bone", "keyframes": tail_kfs}

    # Ground shadow grows
    ground_spawn_kfs = [
        make_keyframe("scale", 0.0, 0.0, 1.0, 0.0, interpolation="linear"),
        make_keyframe("scale", 0.15, 1.2, 1.0, 1.2, interpolation="catmullrom"),
        make_keyframe("scale", 0.35, 1.0, 1.0, 1.0, interpolation="catmullrom"),
        make_keyframe("scale", SPAWN_LEN, 1.0, 1.0, 1.0, interpolation="linear"),
    ]
    spawn_animators[ground_bone_uuid] = {"name": "ground_shadow", "type": "bone", "keyframes": ground_spawn_kfs}

    b.add_animation("spawn", length=SPAWN_LEN, animators=spawn_animators,
                    loop="once", override=True)

    # ---------------- IDLE (6.0s, loop, override=false) ---------------- #
    # Most complex animation: each neck moves on a different period.
    # Neck 0 (pink): period 2.5s, swings left/right
    # Neck 1 (mint): period 3.5s, swings forward/back
    # Neck 2 (lavender): period 4.5s, twists circularly
    # Heads look in different directions simultaneously.
    # Eyes blink independently on different intervals.
    # Body breathes at 6s period.
    # Tails wag independently.

    IDLE_LEN = 6.0
    idle_animators = {}

    # Body breathing — each cube scales subtly on a 6s period
    for i, body_bone in enumerate(body_bone_uuids):
        # Each body cube has slightly offset breathing
        ph = i * 0.4
        breath_kfs = []
        for tick in [0.0, 1.5, 3.0, 4.5, 6.0]:
            local_t = (tick + ph) % IDLE_LEN
            phase_pos = (local_t / IDLE_LEN) * 2 * math.pi
            sx = 1.0 + 0.05 * math.sin(phase_pos)
            sy = 1.0 + 0.04 * math.cos(phase_pos)
            sz = 1.0 + 0.05 * math.sin(phase_pos + 0.5)
            breath_kfs.append(make_keyframe("scale", tick, sx, sy, sz, interpolation="catmullrom"))
        idle_animators[body_bone] = {"name": f"body_{i}", "type": "bone", "keyframes": breath_kfs}

    # NECK 0 (pink) - period 2.5s, sways left/right (Z rotation), light Y twist
    pink_neck = neck_chains[0]
    period_pink = 2.5
    # Build keyframes at fine resolution across IDLE_LEN
    def add_neck_anim(anim_dict, neck, period, axis_pattern, name):
        """
        axis_pattern: function(t_norm) -> (rx, ry, rz) per segment level magnitude.
        We apply different magnitudes to seg0, seg1, seg2 (compounds bend).
        """
        # 15 keyframes evenly spaced
        for s_idx, key in enumerate(("seg0", "seg1", "seg2")):
            mag_factor = 0.5 + s_idx * 0.4  # seg2 bends most
            kfs = []
            n_keys = 13
            for k in range(n_keys + 1):
                tick = (k / n_keys) * IDLE_LEN
                t_norm = ((tick % period) / period) * 2 * math.pi
                rx, ry, rz = axis_pattern(t_norm)
                kfs.append(make_keyframe("rotation", tick,
                                         rx * mag_factor, ry * mag_factor, rz * mag_factor,
                                         interpolation="catmullrom"))
            anim_dict[neck[key]] = {"name": f"neck_{name}_{key}", "type": "bone", "keyframes": kfs}

    # Pink: sways left-right (Z axis tilt) with subtle Y twist
    add_neck_anim(idle_animators, pink_neck, period_pink,
                  lambda t: (2 * math.sin(t * 0.5), 5 * math.sin(t), 12 * math.sin(t)),
                  "pink")

    # Mint: forward-back nod (X axis) on 3.5s period
    mint_neck = neck_chains[1]
    period_mint = 3.5
    add_neck_anim(idle_animators, mint_neck, period_mint,
                  lambda t: (10 * math.sin(t), 3 * math.cos(t * 0.7), 2 * math.cos(t)),
                  "mint")

    # Lavender: circular twist (combine X + Z to make ellipse motion) on 4.5s
    lav_neck = neck_chains[2]
    period_lav = 4.5
    add_neck_anim(idle_animators, lav_neck, period_lav,
                  lambda t: (8 * math.sin(t), 7 * math.cos(t * 1.2), 8 * math.cos(t)),
                  "lavender")

    # HEADS: each head looks in different direction with independent rotation
    # These layer ON TOP of the segment chain animations.
    head_periods = [2.5, 3.5, 4.5]
    head_phases = [0.0, 1.2, 2.4]
    for n_idx, neck in enumerate(neck_chains):
        period = head_periods[n_idx]
        phase = head_phases[n_idx]
        head_kfs = []
        n_keys = 13
        for k in range(n_keys + 1):
            tick = (k / n_keys) * IDLE_LEN
            t = ((tick + phase) % period) / period * 2 * math.pi
            # Different axis patterns per head
            if n_idx == 0:  # pink: looks side to side, slight tilt
                rx = 8 * math.sin(t * 1.3)
                ry = 25 * math.sin(t)
                rz = 10 * math.cos(t)
            elif n_idx == 1:  # mint: looks up/down with subtle twist
                rx = 18 * math.sin(t)
                ry = 8 * math.sin(t * 1.5)
                rz = 5 * math.cos(t * 0.8)
            else:  # lavender: circular gaze
                rx = 12 * math.sin(t * 1.1)
                ry = 20 * math.cos(t)
                rz = 12 * math.sin(t)
            head_kfs.append(make_keyframe("rotation", tick, rx, ry, rz, interpolation="catmullrom"))
        idle_animators[neck["head"]] = {"name": f"head_{neck['name']}", "type": "bone", "keyframes": head_kfs}

    # EYES blink independently on different intervals per head
    # Pink eyes: blink at 1.0s, 3.5s, 5.5s
    # Mint eyes: blink at 0.6s, 2.8s, 4.7s
    # Lavender eyes: blink at 1.7s, 3.0s, 5.0s
    blink_times = [
        [1.0, 3.5, 5.5],   # pink
        [0.6, 2.8, 4.7],   # mint
        [1.7, 3.0, 5.0],   # lavender
    ]
    for n_idx, neck in enumerate(neck_chains):
        for eye_key in ("eye_left", "eye_right"):
            kfs = [make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear")]
            for bt in blink_times[n_idx]:
                if bt - 0.05 > 0:
                    kfs.append(make_keyframe("scale", bt - 0.05, 1.0, 1.0, 1.0, interpolation="linear"))
                kfs.append(make_keyframe("scale", bt, 1.0, 0.05, 1.0, interpolation="linear"))
                kfs.append(make_keyframe("scale", bt + 0.10, 1.0, 1.0, 1.0, interpolation="linear"))
            kfs.append(make_keyframe("scale", IDLE_LEN, 1.0, 1.0, 1.0, interpolation="linear"))
            kfs.sort(key=lambda k: k["time"])
            idle_animators[neck[eye_key]] = {"name": f"{eye_key}_{neck['name']}", "type": "bone", "keyframes": kfs}

    # Mouth subtle opening per head on its own period
    for n_idx, neck in enumerate(neck_chains):
        period = head_periods[n_idx]
        for mouth_key in ("mouth_left", "mouth_right"):
            mouth_kfs = []
            n_keys = 9
            for k in range(n_keys + 1):
                tick = (k / n_keys) * IDLE_LEN
                t = (tick % period) / period * 2 * math.pi
                sy = 1.0 + 0.3 * abs(math.sin(t * 1.5))
                mouth_kfs.append(make_keyframe("scale", tick, 1.0, sy, 1.0, interpolation="catmullrom"))
            idle_animators[neck[mouth_key]] = {"name": f"{mouth_key}_{neck['name']}", "type": "bone", "keyframes": mouth_kfs}

    # Ears wiggle on their own period
    for n_idx, neck in enumerate(neck_chains):
        period = head_periods[n_idx]
        for e_idx, ear_key in enumerate(("ear_left", "ear_right")):
            ear_kfs = []
            sign = 1 if e_idx == 0 else -1
            n_keys = 9
            for k in range(n_keys + 1):
                tick = (k / n_keys) * IDLE_LEN
                t = (tick % period) / period * 2 * math.pi
                rx = 8 * math.sin(t)
                rz = sign * 12 * math.cos(t * 1.2)
                ear_kfs.append(make_keyframe("rotation", tick, rx, 0, rz, interpolation="catmullrom"))
            idle_animators[neck[ear_key]] = {"name": f"{ear_key}_{neck['name']}", "type": "bone", "keyframes": ear_kfs}

    # Tails wag on their own periods (independent)
    tail_periods = [1.8, 2.2, 1.5]
    for t_idx, tail_bone in enumerate(tail_bone_uuids):
        period = tail_periods[t_idx]
        tail_kfs = []
        n_keys = 11
        for k in range(n_keys + 1):
            tick = (k / n_keys) * IDLE_LEN
            t = (tick % period) / period * 2 * math.pi
            rx = 5 * math.sin(t)
            ry = 18 * math.sin(t * 1.1)
            rz = (t_idx - 1) * 6
            tail_kfs.append(make_keyframe("rotation", tick, rx, ry, rz, interpolation="catmullrom"))
        idle_animators[tail_bone] = {"name": f"tail_{t_idx}", "type": "bone", "keyframes": tail_kfs}

    # Ground shadow subtle pulse
    ground_idle_kfs = [
        make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="catmullrom"),
        make_keyframe("scale", 1.5, 1.05, 1.0, 1.05, interpolation="catmullrom"),
        make_keyframe("scale", 3.0, 0.95, 1.0, 0.95, interpolation="catmullrom"),
        make_keyframe("scale", 4.5, 1.05, 1.0, 1.05, interpolation="catmullrom"),
        make_keyframe("scale", IDLE_LEN, 1.0, 1.0, 1.0, interpolation="catmullrom"),
        make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="linear"),
        make_keyframe("rotation", IDLE_LEN, 0, 15, 0, interpolation="linear"),
    ]
    idle_animators[ground_bone_uuid] = {"name": "ground_shadow", "type": "bone", "keyframes": ground_idle_kfs}

    b.add_animation("idle", length=IDLE_LEN, animators=idle_animators,
                    loop="loop", override=False)

    # ---------------- DISSIPATE (1.0s, once, override=true) ---------------- #
    # Each head pulls backward (retract for strike) — segments rotate opposite of head pull
    # Then whole model rapidly scales to 0
    # Tails wag once before vanishing.

    DISS_LEN = 1.0
    dissipate_animators = {}

    # Each head retracts then everything collapses
    for n_idx, neck in enumerate(neck_chains):
        # Retract: rotation seg2 pulls back, head tilts up, then collapses
        for s_idx, key in enumerate(("seg0", "seg1", "seg2")):
            mag = 5 + s_idx * 8
            seg_kfs = [
                make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="linear"),
                make_keyframe("rotation", 0.30, mag, 0, (n_idx - 1) * 8, interpolation="catmullrom"),
                make_keyframe("rotation", 0.50, mag - 4, 0, (n_idx - 1) * 4, interpolation="catmullrom"),
                make_keyframe("rotation", DISS_LEN, 0, 0, 0, interpolation="catmullrom"),
                make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
                make_keyframe("scale", 0.55, 1.0, 1.0, 1.0, interpolation="linear"),
                make_keyframe("scale", 0.80, 0.7, 0.7, 0.7, interpolation="catmullrom"),
                make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="catmullrom"),
            ]
            dissipate_animators[neck[key]] = {"name": f"neck_{neck['name']}_{key}", "type": "bone", "keyframes": seg_kfs}

        head_kfs = [
            make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="linear"),
            make_keyframe("rotation", 0.30, -25, (n_idx - 1) * 15, 0, interpolation="catmullrom"),
            make_keyframe("rotation", 0.55, -10, 0, 0, interpolation="catmullrom"),
            make_keyframe("rotation", DISS_LEN, 0, 0, 0, interpolation="catmullrom"),
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
            make_keyframe("scale", 0.50, 1.15, 1.15, 1.15, interpolation="catmullrom"),
            make_keyframe("scale", 0.75, 0.6, 0.6, 0.6, interpolation="catmullrom"),
            make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="catmullrom"),
        ]
        dissipate_animators[neck["head"]] = {"name": f"head_{neck['name']}", "type": "bone", "keyframes": head_kfs}

        # Eyes squeeze shut then vanish
        for eye_key in ("eye_left", "eye_right"):
            eye_kfs = [
                make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
                make_keyframe("scale", 0.40, 1.2, 0.2, 1.2, interpolation="catmullrom"),
                make_keyframe("scale", 0.60, 1.0, 0.0, 1.0, interpolation="catmullrom"),
                make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="catmullrom"),
            ]
            dissipate_animators[neck[eye_key]] = {"name": f"{eye_key}_{neck['name']}", "type": "bone", "keyframes": eye_kfs}

        # Mouth opens wide then collapses
        for mouth_key in ("mouth_left", "mouth_right"):
            mouth_kfs = [
                make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
                make_keyframe("scale", 0.35, 1.5, 1.8, 1.5, interpolation="catmullrom"),
                make_keyframe("scale", 0.65, 0.5, 0.5, 0.5, interpolation="catmullrom"),
                make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="catmullrom"),
            ]
            dissipate_animators[neck[mouth_key]] = {"name": f"{mouth_key}_{neck['name']}", "type": "bone", "keyframes": mouth_kfs}

        # Ears flatten
        for ear_key in ("ear_left", "ear_right"):
            ear_kfs = [
                make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
                make_keyframe("scale", 0.4, 1.0, 0.3, 1.0, interpolation="catmullrom"),
                make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="catmullrom"),
            ]
            dissipate_animators[neck[ear_key]] = {"name": f"{ear_key}_{neck['name']}", "type": "bone", "keyframes": ear_kfs}

    # Body collapses last
    for i, body_bone in enumerate(body_bone_uuids):
        body_kfs = [
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
            make_keyframe("scale", 0.50, 1.05, 1.05, 1.05, interpolation="catmullrom"),
            make_keyframe("scale", 0.80, 0.85, 0.5, 0.85, interpolation="catmullrom"),
            make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="catmullrom"),
            make_keyframe("position", 0.0, 0, 0, 0, interpolation="linear"),
            make_keyframe("position", DISS_LEN, 0, -1.5, 0, interpolation="catmullrom"),
        ]
        dissipate_animators[body_bone] = {"name": f"body_{i}", "type": "bone", "keyframes": body_kfs}

    # Tails wag once then vanish
    for t_idx, tail_bone in enumerate(tail_bone_uuids):
        tail_kfs = [
            make_keyframe("rotation", 0.0, 0, 0, 0, interpolation="linear"),
            make_keyframe("rotation", 0.20, 0, 35, 0, interpolation="catmullrom"),
            make_keyframe("rotation", 0.40, 0, -35, 0, interpolation="catmullrom"),
            make_keyframe("rotation", 0.60, 0, 15, 0, interpolation="catmullrom"),
            make_keyframe("rotation", DISS_LEN, 0, 0, 0, interpolation="catmullrom"),
            make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
            make_keyframe("scale", 0.6, 1.0, 1.0, 1.0, interpolation="linear"),
            make_keyframe("scale", DISS_LEN, 0.0, 0.0, 0.0, interpolation="catmullrom"),
        ]
        dissipate_animators[tail_bone] = {"name": f"tail_{t_idx}", "type": "bone", "keyframes": tail_kfs}

    # Ground shadow collapses
    ground_diss_kfs = [
        make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, interpolation="linear"),
        make_keyframe("scale", 0.5, 1.3, 1.0, 1.3, interpolation="catmullrom"),
        make_keyframe("scale", 0.8, 0.6, 1.0, 0.6, interpolation="catmullrom"),
        make_keyframe("scale", DISS_LEN, 0.0, 1.0, 0.0, interpolation="catmullrom"),
    ]
    dissipate_animators[ground_bone_uuid] = {"name": "ground_shadow", "type": "bone", "keyframes": ground_diss_kfs}

    b.add_animation("dissipate", length=DISS_LEN, animators=dissipate_animators,
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
