#!/usr/bin/env python3
"""Generator for jack_in_box_surprise.bbmodel — Jack-in-the-Box Summon "Surprise!"

Visual intent: a large jack-in-the-box appearing, brightly coloured carnival
checker box with a side crank. The lid snaps open, a silver spring rapidly
uncoils upward, and a pale jester puppet head bursts out and grins too wide.
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
    color_lerp,
    fill_rect,
)

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/fluffy/me_attacks/jack_in_box_surprise.bbmodel"


# ---------------------------------------------------------------------------
# TEXTURES
# ---------------------------------------------------------------------------

def build_box_texture():
    """Texture 0: CARNIVAL BOX 64x64.

    Bright red/yellow checkerboard 8x8 squares. Yellow squares get an
    emissive lift toward #ffb030. Crank uses corner of this same texture
    via uniform_face mapping (the dedicated 'crank' rectangle).
    """
    w = h = 64
    red = hex_to_rgba("#e83030")
    red_dk = hex_to_rgba("#a02020")
    red_hl = hex_to_rgba("#ff6060")
    yellow = hex_to_rgba("#ffe030")
    yellow_dk = hex_to_rgba("#c8a808")
    yellow_em = hex_to_rgba("#ffb030")
    yellow_hl = hex_to_rgba("#fff878")

    pixels = [red] * (w * h)

    # Lay down 8x8 checker pattern. (cx + cy) even -> red, odd -> yellow.
    for cy in range(8):
        for cx in range(8):
            base = red if (cx + cy) % 2 == 0 else yellow
            x0, y0 = cx * 8, cy * 8
            x1, y1 = x0 + 8, y0 + 8
            fill_rect(pixels, w, h, x0, y0, x1, y1, base)

    # Add subtle shading per square (top-left highlight, bottom-right shadow)
    # to keep them visually rich without breaking the geometric pattern.
    for cy in range(8):
        for cx in range(8):
            is_yellow = (cx + cy) % 2 == 1
            x0, y0 = cx * 8, cy * 8
            base = yellow if is_yellow else red
            hl = yellow_hl if is_yellow else red_hl
            dk = yellow_dk if is_yellow else red_dk

            # Top-edge highlight (1px)
            for x in range(x0, x0 + 8):
                pixels[y0 * w + x] = color_lerp(base, hl, 0.55)
            # Left-edge highlight (1px)
            for y in range(y0, y0 + 8):
                pixels[y * w + x0] = color_lerp(base, hl, 0.45)
            # Bottom-edge shadow (1px)
            for x in range(x0, x0 + 8):
                pixels[(y0 + 7) * w + x] = color_lerp(base, dk, 0.55)
            # Right-edge shadow (1px)
            for y in range(y0, y0 + 8):
                pixels[y * w + (x0 + 7)] = color_lerp(base, dk, 0.45)

            # Yellow squares: add an emissive interior tint near centre
            if is_yellow:
                cxp, cyp = x0 + 3, y0 + 3
                for y in range(y0 + 1, y0 + 7):
                    for x in range(x0 + 1, x0 + 7):
                        d = math.sqrt((x - cxp) ** 2 + (y - cyp) ** 2)
                        if d < 3.0:
                            t = max(0.0, 1.0 - d / 3.0)
                            t = t * t
                            p = pixels[y * w + x]
                            pixels[y * w + x] = color_lerp(p, yellow_em, t * 0.55)

    # Reserve a CRANK zone in the corner — overwrite the bottom-right 16x16
    # block with a metallic grey gradient + bolt detail. Crank cubes will
    # uniform_face to (48,48,64,64).
    crank_base = hex_to_rgba("#888890")
    crank_dk = hex_to_rgba("#404048")
    crank_hl = hex_to_rgba("#d8d8e0")
    crank_bolt = hex_to_rgba("#202024")

    fill_rect(pixels, w, h, 48, 48, 64, 64, crank_base)
    # Vertical metallic gradient inside crank zone
    for y in range(48, 64):
        t = (y - 48) / 15.0
        if t < 0.4:
            tt = t / 0.4
            row_color = color_lerp(crank_hl, crank_base, tt)
        else:
            tt = (t - 0.4) / 0.6
            row_color = color_lerp(crank_base, crank_dk, tt)
        for x in range(48, 64):
            pixels[y * w + x] = row_color

    # Diagonal sheen line across crank zone
    for i in range(16):
        x = 48 + i
        y = 48 + (15 - i) // 2
        if 48 <= y < 64:
            p = pixels[y * w + x]
            pixels[y * w + x] = color_lerp(p, crank_hl, 0.6)

    # Bolt at the centre of crank zone
    for y in range(54, 58):
        for x in range(54, 58):
            d = math.sqrt((x - 55.5) ** 2 + (y - 55.5) ** 2)
            if d < 1.8:
                pixels[y * w + x] = crank_bolt
            elif d < 2.4:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, crank_bolt, 0.6)

    # Border around crank zone for visual separation
    for x in range(48, 64):
        pixels[48 * w + x] = crank_dk
        pixels[63 * w + x] = crank_dk
    for y in range(48, 64):
        pixels[y * w + 48] = crank_dk
        pixels[y * w + 63] = crank_dk

    return png_from_pixels(pixels, w, h)


def build_spring_jester_texture():
    """Texture 1: 64x64.

    Top 32 rows: silver-white spring #d0d8e0 with metallic sheen + coil rings.
    Bottom 32 rows: jester face pale #ffeedd with lavender accent #c8a8e0 and
    dedicated zones for eye buttons and a too-wide smile.
    """
    w = h = 64
    silver = hex_to_rgba("#d0d8e0")
    silver_hl = hex_to_rgba("#f8fafc")
    silver_dk = hex_to_rgba("#7888a0")
    silver_emiss = hex_to_rgba("#e8eef8")

    pale = hex_to_rgba("#ffeedd")
    pale_dk = hex_to_rgba("#d0a888")
    lavender = hex_to_rgba("#c8a8e0")
    lavender_dk = hex_to_rgba("#7858a0")
    lavender_hl = hex_to_rgba("#e8d8f8")

    eye_black = hex_to_rgba("#101018")
    eye_white = hex_to_rgba("#ffffff")
    eye_red = hex_to_rgba("#a02828")

    smile_red = hex_to_rgba("#c01818")
    smile_dk = hex_to_rgba("#600808")
    smile_white = hex_to_rgba("#ffffff")

    pixels = [silver] * (w * h)

    # ----- TOP HALF: SPRING ZONE (rows 0..31) -----
    # Vertical metallic gradient
    for y in range(0, 32):
        t = y / 31.0
        if t < 0.5:
            tt = t / 0.5
            row_color = color_lerp(silver_hl, silver, tt)
        else:
            tt = (t - 0.5) / 0.5
            row_color = color_lerp(silver, silver_dk, tt)
        for x in range(w):
            pixels[y * w + x] = row_color

    # Coil rings — horizontal dark bands every 4 rows for spring helix illusion
    for ring_y in range(2, 32, 4):
        for x in range(w):
            p = pixels[ring_y * w + x]
            pixels[ring_y * w + x] = color_lerp(p, silver_dk, 0.45)
        if ring_y + 1 < 32:
            for x in range(w):
                p = pixels[(ring_y + 1) * w + x]
                pixels[(ring_y + 1) * w + x] = color_lerp(p, silver_dk, 0.18)

    # Vertical highlight stripe (specular line) at x=18..22
    for y in range(0, 32):
        for x in range(18, 23):
            d = abs(x - 20)
            t = max(0.0, 1.0 - d / 2.5)
            t = t * t
            p = pixels[y * w + x]
            pixels[y * w + x] = color_lerp(p, silver_hl, t * 0.7)

    # Secondary thin highlight at x=42..46
    for y in range(0, 32):
        for x in range(42, 47):
            d = abs(x - 44)
            t = max(0.0, 1.0 - d / 2.5)
            t = t * t * 0.6
            p = pixels[y * w + x]
            pixels[y * w + x] = color_lerp(p, silver_hl, t)

    # Emissive shimmer dots
    for (sx, sy) in [(10, 6), (32, 14), (54, 8), (24, 24), (50, 26)]:
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                x, y = sx + dx, sy + dy
                if 0 <= x < w and 0 <= y < 32:
                    d = math.sqrt(dx * dx + dy * dy)
                    if d < 2.5:
                        t = max(0.0, 1.0 - d / 2.5)
                        t = t * t
                        p = pixels[y * w + x]
                        pixels[y * w + x] = color_lerp(p, silver_emiss, t * 0.7)

    # ----- BOTTOM HALF: JESTER FACE ZONE (rows 32..63) -----
    # Pale base with subtle lavender wash
    for y in range(32, 64):
        t = (y - 32) / 31.0
        if t < 0.3:
            tt = t / 0.3
            row_color = color_lerp(pale, color_lerp(pale, lavender, 0.12), tt)
        else:
            tt = (t - 0.3) / 0.7
            row_color = color_lerp(color_lerp(pale, lavender, 0.12),
                                    color_lerp(pale, pale_dk, 0.4), tt)
        for x in range(w):
            pixels[y * w + x] = row_color

    # Cheek blush — two soft circles in lavender-pink
    cheek_pink = color_lerp(pale, lavender, 0.55)
    for cx, cy in [(16, 50), (48, 50)]:
        for y in range(32, 64):
            for x in range(w):
                d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
                if d < 6.0:
                    t = max(0.0, 1.0 - d / 6.0)
                    t = t * t * (3 - 2 * t)
                    p = pixels[y * w + x]
                    pixels[y * w + x] = color_lerp(p, cheek_pink, t * 0.6)

    # Lavender accent diamonds — 4 small diamonds for jester pattern
    for (cx, cy) in [(8, 38), (56, 38), (8, 58), (56, 58)]:
        for y in range(max(32, cy - 3), min(64, cy + 4)):
            for x in range(max(0, cx - 3), min(w, cx + 4)):
                if abs(x - cx) + abs(y - cy) < 3:
                    p = pixels[y * w + x]
                    pixels[y * w + x] = color_lerp(p, lavender, 0.85)
                elif abs(x - cx) + abs(y - cy) == 3:
                    p = pixels[y * w + x]
                    pixels[y * w + x] = color_lerp(p, lavender_dk, 0.6)

    # Lavender vertical edge bands (hat trim hint)
    for y in range(32, 36):
        for x in range(w):
            p = pixels[y * w + x]
            pixels[y * w + x] = color_lerp(p, lavender, 0.7)
    for y in range(34, 36):
        for x in range(w):
            p = pixels[y * w + x]
            pixels[y * w + x] = color_lerp(p, lavender_hl, 0.4)

    # Hat tip emissive highlight
    for y in range(32, 38):
        for x in range(28, 36):
            d = math.sqrt((x - 32) ** 2 + (y - 33) ** 2)
            if d < 4.0:
                t = max(0.0, 1.0 - d / 4.0)
                t = t * t
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, lavender_hl, t * 0.5)

    # ----- DEDICATED EYE ZONE: rows 40..48, cols 0..16 -----
    # Two button eyes side-by-side. Ringed pupils.
    fill_rect(pixels, w, h, 0, 40, 16, 48, eye_white)
    # Left button
    for y in range(40, 48):
        for x in range(0, 8):
            d = math.sqrt((x - 3.5) ** 2 + (y - 43.5) ** 2)
            if d < 3.5:
                pixels[y * w + x] = eye_black
            elif d < 4.0:
                pixels[y * w + x] = color_lerp(eye_black, eye_red, 0.6)
    # Tiny white catch-light on left eye
    pixels[41 * w + 2] = eye_white
    pixels[41 * w + 3] = color_lerp(eye_white, eye_black, 0.3)
    # Right button
    for y in range(40, 48):
        for x in range(8, 16):
            d = math.sqrt((x - 11.5) ** 2 + (y - 43.5) ** 2)
            if d < 3.5:
                pixels[y * w + x] = eye_black
            elif d < 4.0:
                pixels[y * w + x] = color_lerp(eye_black, eye_red, 0.6)
    # Tiny white catch-light on right eye
    pixels[41 * w + 10] = eye_white
    pixels[41 * w + 11] = color_lerp(eye_white, eye_black, 0.3)

    # Stitching marks around buttons (4 cross marks each)
    for cx, cy in [(3, 43), (11, 43)]:
        for offs in [(-2, 0), (2, 0), (0, -2), (0, 2)]:
            ox, oy = cx + offs[0], cy + offs[1]
            if 0 <= ox < 16 and 40 <= oy < 48:
                p = pixels[oy * w + ox]
                pixels[oy * w + ox] = color_lerp(p, eye_red, 0.7)

    # ----- DEDICATED SMILE ZONE: rows 52..63, cols 16..56 (40 wide x 12 tall)
    # A wide unsettling smile slab — too wide for the face.
    fill_rect(pixels, w, h, 16, 52, 56, 64, smile_dk)
    # Inner lip
    fill_rect(pixels, w, h, 17, 53, 55, 63, smile_red)
    # Teeth — vertical white bars across the smile, 8 of them
    for ti in range(8):
        tx0 = 18 + ti * 5
        tx1 = tx0 + 3
        fill_rect(pixels, w, h, tx0, 54, tx1, 62, smile_white)
        # Slight shade between teeth (gum line shadow)
        for y in range(54, 62):
            if tx1 < 56:
                p = pixels[y * w + tx1]
                pixels[y * w + tx1] = color_lerp(p, smile_dk, 0.7)
    # Top + bottom lip darken
    for x in range(16, 56):
        pixels[52 * w + x] = smile_dk
        pixels[63 * w + x] = smile_dk
    # Highlight strip across upper teeth
    for x in range(18, 53):
        if 18 <= x < 53:
            p = pixels[55 * w + x]
            pixels[55 * w + x] = color_lerp(p, hex_to_rgba("#ffe8c8"), 0.4)

    # Tiny "tongue" red dot at centre bottom of smile
    fill_rect(pixels, w, h, 34, 60, 38, 63, color_lerp(smile_red, smile_dk, 0.4))

    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# MODEL BUILD
# ---------------------------------------------------------------------------

def build():
    b = Builder("jack_in_box_surprise", resolution=(64, 64), visible_box=(8, 12, 0))

    # ---- TEXTURES ----
    b.add_texture("box_carnival", build_box_texture())
    b.add_texture("spring_jester", build_spring_jester_texture())

    # Face shorthands
    box_face = lambda: uniform_face(0, 0, 48, 48, tex_index=0)        # checker only (avoid crank zone)
    crank_face = lambda: uniform_face(48, 48, 64, 64, tex_index=0)    # metallic crank zone
    spring_face = lambda: uniform_face(0, 0, 64, 32, tex_index=1)     # silver spring half
    head_face = lambda: uniform_face(20, 32, 60, 52, tex_index=1)     # pale face area
    hat_face = lambda: uniform_face(20, 32, 60, 40, tex_index=1)      # top hat band area
    eye_face = lambda: uniform_face(0, 40, 16, 48, tex_index=1)       # eye buttons zone
    smile_face = lambda: uniform_face(16, 52, 56, 64, tex_index=1)    # wide smile zone

    # =====================================================================
    # 1) BOX BODY — 4 side panels + 1 base slab. Each side own bone.
    #    Box ~ 6x6x6 BB units (centred at y=0..6 for the closed cube).
    # =====================================================================
    side_groups = []  # (group_uuid, group_dict)

    # Front side panel (faces +Z)
    front_cube = b.add_cube(
        "box_front",
        from_=[-3.0, 0.2, 2.7], to_=[3.0, 5.8, 3.0],
        faces=box_face(),
    )
    front_uuid = make_uuid()
    side_groups.append((front_uuid, b.make_group(
        "box_front", [0, 3.0, 2.85], [front_cube], group_uuid=front_uuid)))

    # Back side panel (faces -Z)
    back_cube = b.add_cube(
        "box_back",
        from_=[-3.0, 0.2, -3.0], to_=[3.0, 5.8, -2.7],
        faces=box_face(),
    )
    back_uuid = make_uuid()
    side_groups.append((back_uuid, b.make_group(
        "box_back", [0, 3.0, -2.85], [back_cube], group_uuid=back_uuid)))

    # Left side panel (faces -X)
    left_cube = b.add_cube(
        "box_left",
        from_=[-3.0, 0.2, -2.7], to_=[-2.7, 5.8, 2.7],
        faces=box_face(),
    )
    left_uuid = make_uuid()
    side_groups.append((left_uuid, b.make_group(
        "box_left", [-2.85, 3.0, 0], [left_cube], group_uuid=left_uuid)))

    # Right side panel (faces +X) — also where the crank attaches
    right_cube = b.add_cube(
        "box_right",
        from_=[2.7, 0.2, -2.7], to_=[3.0, 5.8, 2.7],
        faces=box_face(),
    )
    right_uuid = make_uuid()
    side_groups.append((right_uuid, b.make_group(
        "box_right", [2.85, 3.0, 0], [right_cube], group_uuid=right_uuid)))

    # Base slab (the floor of the box, slightly thicker)
    base_cube = b.add_cube(
        "box_base",
        from_=[-3.0, 0.0, -3.0], to_=[3.0, 0.5, 3.0],
        faces=box_face(),
    )
    base_uuid = make_uuid()
    side_groups.append((base_uuid, b.make_group(
        "box_base", [0, 0.25, 0], [base_cube], group_uuid=base_uuid)))

    # =====================================================================
    # 2) BOX LID — 1 flat slab on top, hinged at the back. Own bone with
    #    pivot at the back hinge so it flips open about that line.
    # =====================================================================
    lid_cube = b.add_cube(
        "box_lid",
        from_=[-3.0, 5.8, -3.0], to_=[3.0, 6.4, 3.0],
        faces=box_face(),
    )
    lid_uuid = make_uuid()
    lid_group = b.make_group(
        "box_lid", [0, 6.1, -3.0], [lid_cube], group_uuid=lid_uuid)

    # =====================================================================
    # 3) CRANK — 2 cubes forming an L on the right side of the box.
    #    A short stub poking out, then a perpendicular handle.
    # =====================================================================
    crank_cubes = []
    # Stub axle (sticks out +X from right side)
    crank_cubes.append(b.add_cube(
        "crank_axle",
        from_=[3.0, 2.5, -0.4], to_=[4.6, 3.5, 0.4],
        faces=crank_face(),
    ))
    # Handle (perpendicular bar, hangs down from axle tip)
    crank_cubes.append(b.add_cube(
        "crank_handle",
        from_=[4.2, 1.3, -0.4], to_=[4.8, 2.6, 0.4],
        faces=crank_face(),
    ))
    crank_uuid = make_uuid()
    crank_group = b.make_group(
        "crank", [3.0, 3.0, 0], crank_cubes, group_uuid=crank_uuid)

    # =====================================================================
    # 4) SPRING — 4 zigzag segments stacked vertically. Each own bone.
    #    All four sit collapsed at Y=0 inside the box at spawn, then
    #    extend in the spawn animation up to roughly Y=8.
    # =====================================================================
    spring_groups = []
    # Each segment has alternating X offset (zigzag visual). When collapsed
    # they stack at base of box; when extended they spread out vertically.
    spring_cubes_specs = [
        # (name, x_offs, segment_y, half_x, half_y, half_z, twist)
        ("spring_seg_1",  0.6, 0.0, 1.2, 0.4, 1.2, 0.0),
        ("spring_seg_2", -0.6, 0.0, 1.2, 0.4, 1.2, 0.0),
        ("spring_seg_3",  0.6, 0.0, 1.1, 0.4, 1.1, 0.0),
        ("spring_seg_4", -0.6, 0.0, 1.0, 0.4, 1.0, 0.0),
    ]
    for (sname, sx, sy_, hx, hy, hz, twist) in spring_cubes_specs:
        cu = b.add_cube(
            sname,
            from_=[sx - hx, sy_ - hy, -hz], to_=[sx + hx, sy_ + hy, hz],
            faces=spring_face(),
        )
        gu = make_uuid()
        g = b.make_group(
            sname, [sx, sy_, 0], [cu],
            rotation=[0, twist, 0], group_uuid=gu,
        )
        spring_groups.append((gu, g))

    # =====================================================================
    # 5) JESTER HEAD — 3 overlapping cubes. One bone for the head cluster.
    # =====================================================================
    head_cubes = []
    # head_main — central
    head_cubes.append(b.add_cube(
        "head_main",
        from_=[-1.6, 7.4, -1.6], to_=[1.6, 10.0, 1.6],
        faces=head_face(),
    ))
    # head_top — slightly smaller, on top to give roundness
    head_cubes.append(b.add_cube(
        "head_top",
        from_=[-1.3, 9.7, -1.3], to_=[1.3, 11.2, 1.3],
        faces=head_face(),
    ))
    # head_bottom — chin/jaw
    head_cubes.append(b.add_cube(
        "head_bottom",
        from_=[-1.4, 6.8, -1.4], to_=[1.4, 7.8, 1.4],
        faces=head_face(),
    ))
    head_uuid = make_uuid()
    head_group = b.make_group("head", [0, 9.0, 0], head_cubes, group_uuid=head_uuid)

    # =====================================================================
    # 6) JESTER HAT — 2 cubes forming pointed hat.
    # =====================================================================
    hat_cubes = []
    # hat_base — wide flat band sitting on top of head
    hat_cubes.append(b.add_cube(
        "hat_base",
        from_=[-1.4, 11.0, -1.4], to_=[1.4, 11.6, 1.4],
        faces=hat_face(),
    ))
    # hat_point — narrower upper section pointed forward
    hat_cubes.append(b.add_cube(
        "hat_point",
        from_=[-0.8, 11.5, -0.4], to_=[0.8, 13.4, 1.4],
        faces=hat_face(),
    ))
    hat_uuid = make_uuid()
    hat_group = b.make_group(
        "hat", [0, 12.0, 0.5], hat_cubes,
        rotation=[15.0, 0, 0],  # tipped forward
        group_uuid=hat_uuid,
    )

    # =====================================================================
    # 7) FACE — 2 button eyes + 1 wide smile slab. Each own bone.
    # =====================================================================
    eye_left_cube = b.add_cube(
        "eye_left",
        from_=[-1.0, 8.6, 1.55], to_=[-0.2, 9.4, 1.75],
        faces=eye_face(),
    )
    eye_left_uuid = make_uuid()
    eye_left_group = b.make_group(
        "eye_left", [-0.6, 9.0, 1.65], [eye_left_cube], group_uuid=eye_left_uuid)

    eye_right_cube = b.add_cube(
        "eye_right",
        from_=[0.2, 8.6, 1.55], to_=[1.0, 9.4, 1.75],
        faces=eye_face(),
    )
    eye_right_uuid = make_uuid()
    eye_right_group = b.make_group(
        "eye_right", [0.6, 9.0, 1.65], [eye_right_cube], group_uuid=eye_right_uuid)

    # SMILE — a wide slab. Wider than the head's natural face. Too wide.
    smile_cube = b.add_cube(
        "smile",
        from_=[-1.9, 7.4, 1.55], to_=[1.9, 8.2, 1.78],
        faces=smile_face(),
    )
    smile_uuid = make_uuid()
    smile_group = b.make_group(
        "smile", [0, 7.8, 1.65], [smile_cube], group_uuid=smile_uuid)

    # =====================================================================
    # 8) GROUND BASE — 2 flat slabs slightly extending from box base.
    # =====================================================================
    ground_cubes = []
    ground_cubes.append(b.add_cube(
        "ground_pad_a",
        from_=[-3.6, -0.2, -3.6], to_=[3.6, 0.0, 3.6],
        faces=box_face(),
    ))
    ground_cubes.append(b.add_cube(
        "ground_pad_b",
        from_=[-4.2, -0.4, -4.2], to_=[4.2, -0.2, 4.2],
        faces=box_face(),
    ))
    ground_uuid = make_uuid()
    ground_group = b.make_group(
        "ground_base", [0, -0.2, 0], ground_cubes, group_uuid=ground_uuid)

    # =====================================================================
    # ROOT BONE — contains everything for global motion.
    # =====================================================================
    root_children = []
    for (_uu, g) in side_groups:
        root_children.append(g)
    root_children.append(lid_group)
    root_children.append(crank_group)
    for (_uu, g) in spring_groups:
        root_children.append(g)
    root_children.append(head_group)
    root_children.append(hat_group)
    root_children.append(eye_left_group)
    root_children.append(eye_right_group)
    root_children.append(smile_group)
    root_children.append(ground_group)

    root_uuid = make_uuid()
    root_group = b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid)
    b.outliner.append(root_group)

    # =====================================================================
    # ANIMATIONS
    # =====================================================================

    # ----- SPAWN: 1.2s, override=True -----
    # Box materialises assembled (lid closed) at 0.0s.
    # 0.2s..0.8s: crank rotates (winding up) — slow, ominous.
    # 0.8s: lid snaps open (rapid rotation).
    # 0.8..0.88s: spring rapidly extends from Y=0 to Y=8.
    # 0.9s: jester head materialises with scale 0->1.5->1.0 pop.
    # 0.9..1.2s: head wobbles side-to-side.
    spawn_animators = {}
    spawn_steps = 60  # dense across 1.2s

    # Root: stays at scale 1, but small ground pulse on lid pop and head pop.
    root_spawn_kfs = []
    for k in range(spawn_steps + 1):
        t = (k / spawn_steps) * 1.2
        # Tiny shake pulse during winding (0.2..0.8) and big spike at 0.8 lid open
        if 0.2 <= t < 0.8:
            sh = math.sin(t * 60.0) * 0.03
            sx = 1.0 + sh
            sy = 1.0 - sh * 0.5
            sz = 1.0 + sh
        elif 0.8 <= t < 0.95:
            tt = (t - 0.8) / 0.15
            sh = math.sin(t * 90.0) * 0.08 * (1.0 - tt)
            sx = 1.0 + sh
            sy = 1.0 + sh
            sz = 1.0 + sh
        else:
            sx = sy = sz = 1.0
        root_spawn_kfs.append(make_keyframe("scale", t, x=sx, y=sy, z=sz))
    # Initial materialise: scale starts very small for first 0.05s
    root_spawn_kfs.append(make_keyframe("scale", 0.0, x=0.05, y=0.05, z=0.05))
    root_spawn_kfs.append(make_keyframe("scale", 0.06, x=1.05, y=1.05, z=1.05))
    root_spawn_kfs.append(make_keyframe("scale", 0.12, x=1.0, y=1.0, z=1.0))
    spawn_animators[root_uuid] = {
        "name": "root", "type": "bone", "keyframes": root_spawn_kfs,
    }

    # Box sides + base: small material-in pop + shake during winding.
    for idx, (gu, g) in enumerate(side_groups):
        kfs = []
        ph_off = idx * 0.07
        for k in range(spawn_steps + 1):
            t = (k / spawn_steps) * 1.2
            # Material-in scale 0..1 across first 0.12s (staggered)
            if t < 0.04 + idx * 0.01:
                s = 0.05
            elif t < 0.14 + idx * 0.01:
                tt = (t - (0.04 + idx * 0.01)) / 0.10
                s = 0.05 + (1.0 - 0.05) * tt
            else:
                s = 1.0
            # Shake during winding
            if 0.2 <= t < 0.8:
                sh = math.sin(t * 50.0 + ph_off * 30) * 0.015
                s += sh
            kfs.append(make_keyframe("scale", t, x=s, y=s, z=s))
        # Tiny X/Z shake position during winding
        for k in range(spawn_steps + 1):
            t = (k / spawn_steps) * 1.2
            if 0.2 <= t < 0.8:
                xo = math.sin(t * 70.0 + ph_off * 40) * 0.04
                zo = math.cos(t * 65.0 + ph_off * 40) * 0.04
            elif 0.8 <= t < 0.95:
                tt = (t - 0.8) / 0.15
                xo = math.sin(t * 110.0) * 0.10 * (1.0 - tt)
                zo = math.cos(t * 100.0) * 0.10 * (1.0 - tt)
            else:
                xo = zo = 0.0
            kfs.append(make_keyframe("position", t, x=xo, y=0, z=zo))
        spawn_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    # Lid: closed (rotation 0) for 0..0.8, then SNAP open at 0.8 (rotates -135°
    # around its hinge at the back). Slight bounce overshoot then settle.
    lid_kfs = [
        make_keyframe("scale", 0.0, x=0.05, y=0.05, z=0.05),
        make_keyframe("scale", 0.06, x=1.05, y=1.05, z=1.05),
        make_keyframe("scale", 0.12, x=1.0, y=1.0, z=1.0),
        make_keyframe("rotation", 0.00, x=0, y=0, z=0),
        make_keyframe("rotation", 0.78, x=0, y=0, z=0),
        # Snap open in 0.04s
        make_keyframe("rotation", 0.82, x=-150, y=0, z=0, interpolation="linear"),
        # Overshoot + bounce
        make_keyframe("rotation", 0.86, x=-120, y=0, z=0),
        make_keyframe("rotation", 0.92, x=-140, y=0, z=0),
        make_keyframe("rotation", 0.98, x=-130, y=0, z=0),
        make_keyframe("rotation", 1.20, x=-135, y=0, z=0),
    ]
    # Add tiny shake during winding (0.2..0.8)
    for k in range(20):
        t = 0.2 + (k / 20.0) * 0.6
        rx = math.sin(t * 50.0) * 1.5
        lid_kfs.append(make_keyframe("rotation", t, x=rx, y=0, z=0))
    spawn_animators[lid_uuid] = {
        "name": "box_lid", "type": "bone", "keyframes": lid_kfs,
    }

    # Crank: slow rotate during winding 0.2..0.8 (about 3 full rotations).
    # Stays still after 0.8.
    crank_kfs = [
        make_keyframe("scale", 0.0, x=0.05, y=0.05, z=0.05),
        make_keyframe("scale", 0.08, x=1.05, y=1.05, z=1.05),
        make_keyframe("scale", 0.14, x=1.0, y=1.0, z=1.0),
    ]
    # Crank rotates around X axis (since axle is in +X); spin is on X.
    crank_kfs.append(make_keyframe("rotation", 0.00, x=0, y=0, z=0))
    crank_kfs.append(make_keyframe("rotation", 0.20, x=0, y=0, z=0))
    # 3 full rotations = 1080° from t=0.2 to t=0.8 — but slow start, faster end.
    wind_steps = 30
    for k in range(wind_steps + 1):
        t = 0.2 + (k / wind_steps) * 0.6
        # Ease-in: slow at first then accelerate (tension building)
        tt = k / wind_steps
        ease = tt * tt
        rx = ease * 1080.0
        crank_kfs.append(make_keyframe("rotation", t, x=rx, y=0, z=0))
    # Stop at 0.8 (post-snap)
    crank_kfs.append(make_keyframe("rotation", 0.82, x=1080, y=0, z=0))
    # Tiny twitch back from recoil
    crank_kfs.append(make_keyframe("rotation", 0.86, x=1095, y=0, z=0))
    crank_kfs.append(make_keyframe("rotation", 0.92, x=1080, y=0, z=0))
    crank_kfs.append(make_keyframe("rotation", 1.20, x=1080, y=0, z=0))
    spawn_animators[crank_uuid] = {
        "name": "crank", "type": "bone", "keyframes": crank_kfs,
    }

    # Spring segments: collapsed (scale Y small, position low) until 0.8;
    # at 0.80..0.88 (very fast pop) extend up to their final stacked positions.
    # Final extended positions: seg1 y=2, seg2 y=4, seg3 y=6, seg4 y=8 (approx).
    spring_extend_y = [1.0, 2.8, 4.6, 6.4]
    for idx, ((gu, g), final_y) in enumerate(zip(spring_groups, spring_extend_y)):
        kfs = []
        # Hidden (scale 0) until 0.78
        kfs.append(make_keyframe("scale", 0.00, x=0, y=0, z=0))
        kfs.append(make_keyframe("scale", 0.78, x=0, y=0, z=0))
        # Pop scale: 0 -> 1.2 -> 1.0 across 0.08s (very fast)
        # Stagger so each segment appears slightly later (zigzag uncoiling)
        delay = 0.78 + idx * 0.012
        kfs.append(make_keyframe("scale", delay, x=0.2, y=0.2, z=0.2))
        kfs.append(make_keyframe("scale", delay + 0.04, x=1.2, y=1.2, z=1.2))
        kfs.append(make_keyframe("scale", delay + 0.08, x=1.0, y=1.0, z=1.0))
        # Position: held at Y=0 then jumped up to final_y at delay + 0.08
        kfs.append(make_keyframe("position", 0.00, x=0, y=-final_y, z=0))
        kfs.append(make_keyframe("position", delay, x=0, y=-final_y, z=0))
        kfs.append(make_keyframe("position", delay + 0.04, x=0, y=-final_y * 0.4, z=0))
        kfs.append(make_keyframe("position", delay + 0.08, x=0, y=0, z=0))
        # Slight overshoot bounce
        kfs.append(make_keyframe("position", delay + 0.12, x=0, y=0.2, z=0))
        kfs.append(make_keyframe("position", delay + 0.18, x=0, y=0, z=0))
        # After extension: small wobble until end
        for k in range(15):
            t = (delay + 0.18) + (k / 15.0) * (1.2 - delay - 0.18)
            if t > 1.2:
                t = 1.2
            ph = (t - delay - 0.18) * 12.0
            yo = math.sin(ph + idx * 0.5) * 0.08 * math.exp(-(t - delay - 0.18) * 2.0)
            xo = math.cos(ph * 0.7 + idx * 0.4) * 0.05 * math.exp(-(t - delay - 0.18) * 2.0)
            kfs.append(make_keyframe("position", t, x=xo, y=yo, z=0))
        spawn_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    # Head: hidden until 0.9, then scale 0 -> 1.5 -> 1.0 pop.
    head_kfs = []
    head_kfs.append(make_keyframe("scale", 0.00, x=0, y=0, z=0))
    head_kfs.append(make_keyframe("scale", 0.88, x=0, y=0, z=0))
    head_kfs.append(make_keyframe("scale", 0.90, x=0.5, y=0.5, z=0.5))
    head_kfs.append(make_keyframe("scale", 0.95, x=1.5, y=1.5, z=1.5))
    head_kfs.append(make_keyframe("scale", 1.00, x=1.0, y=1.0, z=1.0))
    head_kfs.append(make_keyframe("scale", 1.20, x=1.0, y=1.0, z=1.0))
    # Wobble side-to-side after appearing
    wobble_steps = 24
    for k in range(wobble_steps + 1):
        t = 0.95 + (k / wobble_steps) * (1.20 - 0.95)
        ph = (t - 0.95) * 30.0
        rz = math.sin(ph) * 12.0 * math.exp(-(t - 0.95) * 4.0)
        rx = math.cos(ph * 1.3) * 4.0 * math.exp(-(t - 0.95) * 4.0)
        ry = math.sin(ph * 0.7) * 5.0
        head_kfs.append(make_keyframe("rotation", t, x=rx, y=ry, z=rz))
    head_kfs.append(make_keyframe("rotation", 0.00, x=0, y=0, z=0))
    head_kfs.append(make_keyframe("rotation", 0.90, x=0, y=0, z=0))
    spawn_animators[head_uuid] = {
        "name": "head", "type": "bone", "keyframes": head_kfs,
    }

    # Hat: appears with head (0.9) then bobs.
    hat_kfs = []
    hat_kfs.append(make_keyframe("scale", 0.00, x=0, y=0, z=0))
    hat_kfs.append(make_keyframe("scale", 0.90, x=0, y=0, z=0))
    hat_kfs.append(make_keyframe("scale", 0.94, x=0.6, y=0.6, z=0.6))
    hat_kfs.append(make_keyframe("scale", 0.98, x=1.3, y=1.3, z=1.3))
    hat_kfs.append(make_keyframe("scale", 1.05, x=1.0, y=1.0, z=1.0))
    hat_kfs.append(make_keyframe("scale", 1.20, x=1.0, y=1.0, z=1.0))
    for k in range(wobble_steps + 1):
        t = 0.98 + (k / wobble_steps) * (1.20 - 0.98)
        ph = (t - 0.98) * 35.0
        rx = 15.0 + math.sin(ph) * 12.0 * math.exp(-(t - 0.98) * 5.0)
        rz = math.cos(ph * 1.2) * 8.0 * math.exp(-(t - 0.98) * 5.0)
        ry = math.sin(ph * 0.8) * 6.0
        hat_kfs.append(make_keyframe("rotation", t, x=rx, y=ry, z=rz))
    hat_kfs.append(make_keyframe("rotation", 0.00, x=15, y=0, z=0))
    hat_kfs.append(make_keyframe("rotation", 0.95, x=15, y=0, z=0))
    spawn_animators[hat_uuid] = {
        "name": "hat", "type": "bone", "keyframes": hat_kfs,
    }

    # Eyes: appear at 0.92, scale pulse 0 -> 1.4 -> 1.0
    for sub_uuid, sub_name, sign in [
        (eye_left_uuid, "eye_left", 1.0),
        (eye_right_uuid, "eye_right", -1.0),
    ]:
        kfs = []
        kfs.append(make_keyframe("scale", 0.00, x=0, y=0, z=0))
        kfs.append(make_keyframe("scale", 0.92, x=0, y=0, z=0))
        kfs.append(make_keyframe("scale", 0.96, x=1.4, y=1.4, z=1.4))
        kfs.append(make_keyframe("scale", 1.02, x=1.0, y=1.0, z=1.0))
        # Eye pulse during wobble
        for k in range(20):
            t = 1.02 + (k / 20.0) * (1.20 - 1.02)
            ph = (t - 1.02) * 30.0
            s = 1.0 + 0.08 * abs(math.sin(ph))
            kfs.append(make_keyframe("scale", t, x=s, y=s, z=s))
        # Tiny rotation following head wobble
        for k in range(15):
            t = 1.0 + (k / 15.0) * (1.20 - 1.0)
            ph = (t - 1.0) * 25.0
            ry = math.sin(ph) * 8.0 * sign
            kfs.append(make_keyframe("rotation", t, x=0, y=ry, z=0))
        spawn_animators[sub_uuid] = {
            "name": sub_name, "type": "bone", "keyframes": kfs,
        }

    # Smile: appears with face — a delayed pulse (0.94) and stretches WIDER.
    smile_kfs = []
    smile_kfs.append(make_keyframe("scale", 0.00, x=0, y=0, z=0))
    smile_kfs.append(make_keyframe("scale", 0.94, x=0, y=0, z=0))
    smile_kfs.append(make_keyframe("scale", 0.98, x=0.6, y=1.4, z=0.6))
    smile_kfs.append(make_keyframe("scale", 1.04, x=1.3, y=1.0, z=1.0))
    smile_kfs.append(make_keyframe("scale", 1.10, x=1.05, y=1.0, z=1.0))
    # Slowly stretch even wider over time (unsettling)
    for k in range(12):
        t = 1.10 + (k / 12.0) * (1.20 - 1.10)
        tt = (t - 1.10) / 0.10
        sx = 1.05 + tt * 0.18
        smile_kfs.append(make_keyframe("scale", t, x=sx, y=1.0, z=1.0))
    spawn_animators[smile_uuid] = {
        "name": "smile", "type": "bone", "keyframes": smile_kfs,
    }

    # Ground base — quick material-in pulse.
    ground_kfs = []
    ground_kfs.append(make_keyframe("scale", 0.00, x=0.05, y=0.05, z=0.05))
    ground_kfs.append(make_keyframe("scale", 0.04, x=1.1, y=1.1, z=1.1))
    ground_kfs.append(make_keyframe("scale", 0.10, x=1.0, y=1.0, z=1.0))
    # Tiny pulse on lid snap (0.82) — feedback hit
    ground_kfs.append(make_keyframe("scale", 0.80, x=1.0, y=1.0, z=1.0))
    ground_kfs.append(make_keyframe("scale", 0.83, x=1.08, y=0.92, z=1.08))
    ground_kfs.append(make_keyframe("scale", 0.88, x=1.0, y=1.0, z=1.0))
    ground_kfs.append(make_keyframe("scale", 1.20, x=1.0, y=1.0, z=1.0))
    spawn_animators[ground_uuid] = {
        "name": "ground_base", "type": "bone", "keyframes": ground_kfs,
    }

    b.add_animation("spawn", length=1.2, animators=spawn_animators,
                    loop="once", override=True)

    # ----- IDLE: 4.0s loop, override=False -----
    # Spring oscillates Y position +-0.8 (spring bouncing).
    # Head follows with lag.
    # Head slowly rotates Y.
    # Eyes and smile pulse slightly.
    # Box stays still. Crank occasionally twitches.
    idle_animators = {}
    idle_steps = 80  # dense for smooth oscillation across 4s

    # Root: small breathing (slight Y bob, the whole assembly settling).
    root_idle_kfs = []
    for k in range(idle_steps + 1):
        t = (k / idle_steps) * 4.0
        ph = (t / 4.0) * 2 * math.pi
        yo = math.sin(ph) * 0.06
        root_idle_kfs.append(make_keyframe("position", t, x=0, y=yo, z=0))
    for k in range(idle_steps + 1):
        t = (k / idle_steps) * 4.0
        ph = (t / 4.0) * 2 * math.pi
        s = 1.0 + 0.01 * math.sin(ph * 2)
        root_idle_kfs.append(make_keyframe("scale", t, x=s, y=s, z=s))
    idle_animators[root_uuid] = {
        "name": "root", "type": "bone", "keyframes": root_idle_kfs,
    }

    # Spring segments: bounce Y by +-0.8 with a phase offset per segment so the
    # bottom segment leads and top segment lags (spring physics feel).
    for idx, (gu, g) in enumerate(spring_groups):
        kfs = []
        # Each segment lags a bit. Period 1.0s, amplitude grows with height.
        phase_off = idx * 0.6  # higher segs lag more
        amp = 0.45 + idx * 0.12  # higher segs swing more
        for k in range(idle_steps + 1):
            t = (k / idle_steps) * 4.0
            ph = (t / 1.0) * 2 * math.pi - phase_off
            yo = math.sin(ph) * amp
            xo = math.cos(ph * 0.8 + idx * 0.7) * 0.15
            zo = math.sin(ph * 1.1 + idx * 0.4) * 0.12
            kfs.append(make_keyframe("position", t, x=xo, y=yo, z=zo))
        # Slight scale squash on Y as spring compresses
        for k in range(idle_steps + 1):
            t = (k / idle_steps) * 4.0
            ph = (t / 1.0) * 2 * math.pi - phase_off
            sy_ = 1.0 - 0.08 * (1.0 - math.cos(ph))
            sx_ = 1.0 + 0.05 * (1.0 - math.cos(ph))
            kfs.append(make_keyframe("scale", t, x=sx_, y=sy_, z=sx_))
        # Tiny rotation jitter
        for k in range(idle_steps + 1):
            t = (k / idle_steps) * 4.0
            ph = (t / 1.0) * 2 * math.pi - phase_off
            rz = math.sin(ph * 0.5) * 4.0
            rx = math.cos(ph * 0.7 + idx * 0.3) * 3.0
            kfs.append(make_keyframe("rotation", t, x=rx, y=0, z=rz))
        idle_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    # Head: lags spring oscillation. Slow Y rotation across the loop.
    head_idle_kfs = []
    for k in range(idle_steps + 1):
        t = (k / idle_steps) * 4.0
        # Position lag: opposite phase from top spring segment, smaller amp.
        ph = (t / 1.0) * 2 * math.pi - 3.0  # heavy lag
        yo = math.sin(ph) * 0.35
        xo = math.cos(ph * 0.6) * 0.10
        head_idle_kfs.append(make_keyframe("position", t, x=xo, y=yo, z=0))
    # Rotation: full slow Y rotation across 4s + bobble
    for k in range(idle_steps + 1):
        t = (k / idle_steps) * 4.0
        ry = (t / 4.0) * 360.0
        ph = (t / 1.0) * 2 * math.pi - 3.0
        rz = math.sin(ph) * 6.0
        rx = math.cos(ph * 1.3) * 3.0
        head_idle_kfs.append(make_keyframe("rotation", t, x=rx, y=ry, z=rz))
    # Scale breathing
    for k in range(idle_steps + 1):
        t = (k / idle_steps) * 4.0
        ph = (t / 4.0) * 2 * math.pi
        s = 1.0 + 0.04 * (1.0 - math.cos(ph))
        head_idle_kfs.append(make_keyframe("scale", t, x=s, y=s, z=s))
    idle_animators[head_uuid] = {
        "name": "head", "type": "bone", "keyframes": head_idle_kfs,
    }

    # Hat: extra lag, extra wobble. Floppy point.
    hat_idle_kfs = []
    for k in range(idle_steps + 1):
        t = (k / idle_steps) * 4.0
        ph = (t / 1.0) * 2 * math.pi - 3.5
        # Hat tilts with each bounce
        rx = 15.0 + math.sin(ph) * 14.0
        rz = math.cos(ph * 0.9) * 12.0
        ry = math.sin(ph * 0.5) * 8.0
        hat_idle_kfs.append(make_keyframe("rotation", t, x=rx, y=ry, z=rz))
    for k in range(idle_steps + 1):
        t = (k / idle_steps) * 4.0
        ph = (t / 1.0) * 2 * math.pi - 3.5
        yo = math.sin(ph) * 0.2
        zo = math.cos(ph * 0.8) * 0.1
        hat_idle_kfs.append(make_keyframe("position", t, x=0, y=yo, z=zo))
    idle_animators[hat_uuid] = {
        "name": "hat", "type": "bone", "keyframes": hat_idle_kfs,
    }

    # Eyes: pulse slightly (heartbeat at 1Hz).
    for sub_uuid, sub_name, sign in [
        (eye_left_uuid, "eye_left", 1.0),
        (eye_right_uuid, "eye_right", -1.0),
    ]:
        kfs = []
        for k in range(idle_steps + 1):
            t = (k / idle_steps) * 4.0
            ph = (t / 1.0) * 2 * math.pi
            s = 1.0 + 0.10 * abs(math.sin(ph))
            kfs.append(make_keyframe("scale", t, x=s, y=s, z=s))
        # Slight Y rotation (head-wobble follow)
        for k in range(idle_steps + 1):
            t = (k / idle_steps) * 4.0
            ph = (t / 4.0) * 2 * math.pi
            ry = math.sin(ph) * 4.0 * sign
            rz = math.cos(ph * 1.3) * 2.0 * sign
            kfs.append(make_keyframe("rotation", t, x=0, y=ry, z=rz))
        idle_animators[sub_uuid] = {
            "name": sub_name, "type": "bone", "keyframes": kfs,
        }

    # Smile: slow pulse — gradually getting wider then easing back.
    smile_idle_kfs = []
    for k in range(idle_steps + 1):
        t = (k / idle_steps) * 4.0
        ph = (t / 4.0) * 2 * math.pi
        sx = 1.05 + 0.12 * (1.0 - math.cos(ph))
        sy = 1.0 + 0.04 * math.sin(ph * 2)
        smile_idle_kfs.append(make_keyframe("scale", t, x=sx, y=sy, z=1.0))
    # Tiny vertical bob
    for k in range(idle_steps + 1):
        t = (k / idle_steps) * 4.0
        ph = (t / 1.0) * 2 * math.pi - 3.0
        yo = math.sin(ph) * 0.3
        smile_idle_kfs.append(make_keyframe("position", t, x=0, y=yo, z=0))
    idle_animators[smile_uuid] = {
        "name": "smile", "type": "bone", "keyframes": smile_idle_kfs,
    }

    # Crank: occasional twitch — 4 small twitches across the 4s loop.
    crank_idle_kfs = []
    twitch_times = [0.5, 1.4, 2.3, 3.1]
    for k in range(idle_steps + 1):
        t = (k / idle_steps) * 4.0
        rx_total = 0.0
        for tt_ in twitch_times:
            dt = t - tt_
            if 0 <= dt < 0.25:
                # quick small rotation pulse
                rx_total += math.sin(dt / 0.25 * math.pi) * 25.0
        crank_idle_kfs.append(make_keyframe("rotation", t, x=rx_total, y=0, z=0))
    idle_animators[crank_uuid] = {
        "name": "crank", "type": "bone", "keyframes": crank_idle_kfs,
    }

    # Lid: stays at -135 (open) but floats slightly — small rotation breath.
    lid_idle_kfs = []
    for k in range(idle_steps + 1):
        t = (k / idle_steps) * 4.0
        ph = (t / 4.0) * 2 * math.pi
        rx = -135.0 + math.sin(ph) * 4.0
        lid_idle_kfs.append(make_keyframe("rotation", t, x=rx, y=0, z=0))
    idle_animators[lid_uuid] = {
        "name": "box_lid", "type": "bone", "keyframes": lid_idle_kfs,
    }

    # Sides: very subtle scale breathing.
    for idx, (gu, g) in enumerate(side_groups):
        kfs = []
        for k in range(idle_steps + 1):
            t = (k / idle_steps) * 4.0
            ph = (t / 4.0 + idx * 0.15) * 2 * math.pi
            s = 1.0 + 0.012 * (1.0 - math.cos(ph))
            kfs.append(make_keyframe("scale", t, x=s, y=s, z=s))
        idle_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    b.add_animation("idle", length=4.0, animators=idle_animators,
                    loop="loop", override=False)

    # ----- DISSIPATE: 0.7s, override=True -----
    # Head retracts down spring rapidly (0.0..0.25)
    # Lid snaps closed (0.25..0.35)
    # Spring coils back (0.0..0.30)
    # Box rapidly scales to 0 (0.45..0.7)
    diss_animators = {}
    diss_steps = 30

    # Root: stays at scale 1 through the retract+close, then rapidly scales to 0
    # in the final 0.25s.
    root_diss_kfs = []
    for k in range(diss_steps + 1):
        t = (k / diss_steps) * 0.7
        if t < 0.45:
            s = 1.0
        else:
            tt = (t - 0.45) / 0.25
            tt = min(1.0, max(0.0, tt))
            tt2 = tt * tt
            s = 1.0 + (0.0 - 1.0) * tt2
        # Tiny shudder during the close
        if 0.20 <= t < 0.40:
            sh = math.sin(t * 60.0) * 0.04
            s_x = s + sh
            s_y = s - sh * 0.5
            s_z = s + sh
        else:
            s_x = s_y = s_z = s
        root_diss_kfs.append(make_keyframe("scale", t, x=s_x, y=s_y, z=s_z))
    # Spin slightly during shrink
    for k in range(diss_steps + 1):
        t = (k / diss_steps) * 0.7
        if t < 0.45:
            ry = 0.0
        else:
            tt = (t - 0.45) / 0.25
            ry = tt * 180.0
        root_diss_kfs.append(make_keyframe("rotation", t, x=0, y=ry, z=0))
    diss_animators[root_uuid] = {
        "name": "root", "type": "bone", "keyframes": root_diss_kfs,
    }

    # Head: retracts rapidly down — Y position from 0 to -8 across 0..0.22.
    # Then disappears (scale 0) at 0.22.
    head_diss_kfs = [
        make_keyframe("position", 0.00, x=0, y=0, z=0),
        make_keyframe("position", 0.05, x=0, y=-1.5, z=0),
        make_keyframe("position", 0.12, x=0, y=-4.0, z=0),
        make_keyframe("position", 0.18, x=0, y=-7.0, z=0),
        make_keyframe("position", 0.22, x=0, y=-8.5, z=0),
        make_keyframe("position", 0.70, x=0, y=-8.5, z=0),
        # Squash as it retracts
        make_keyframe("scale", 0.00, x=1.0, y=1.0, z=1.0),
        make_keyframe("scale", 0.10, x=1.2, y=0.6, z=1.2),
        make_keyframe("scale", 0.20, x=0.6, y=0.3, z=0.6),
        make_keyframe("scale", 0.22, x=0.0, y=0.0, z=0.0),
        make_keyframe("scale", 0.70, x=0.0, y=0.0, z=0.0),
        # Spinning as it falls
        make_keyframe("rotation", 0.00, x=0, y=0, z=0),
        make_keyframe("rotation", 0.22, x=0, y=720, z=180),
    ]
    diss_animators[head_uuid] = {
        "name": "head", "type": "bone", "keyframes": head_diss_kfs,
    }

    # Hat: flies off a bit before head vanishes.
    hat_diss_kfs = [
        make_keyframe("position", 0.00, x=0, y=0, z=0),
        make_keyframe("position", 0.06, x=0, y=1.5, z=-0.5),
        make_keyframe("position", 0.14, x=-0.5, y=2.0, z=-1.5),
        make_keyframe("position", 0.22, x=-1.2, y=0.5, z=-2.5),
        make_keyframe("position", 0.30, x=-2.0, y=-2.0, z=-3.0),
        make_keyframe("position", 0.70, x=-2.0, y=-2.0, z=-3.0),
        make_keyframe("rotation", 0.00, x=15, y=0, z=0),
        make_keyframe("rotation", 0.30, x=420, y=480, z=300),
        make_keyframe("scale", 0.00, x=1.0, y=1.0, z=1.0),
        make_keyframe("scale", 0.25, x=0.7, y=0.7, z=0.7),
        make_keyframe("scale", 0.40, x=0.0, y=0.0, z=0.0),
        make_keyframe("scale", 0.70, x=0.0, y=0.0, z=0.0),
    ]
    diss_animators[hat_uuid] = {
        "name": "hat", "type": "bone", "keyframes": hat_diss_kfs,
    }

    # Eyes: vanish with head (slightly earlier — they go dark first)
    for sub_uuid, sub_name, sign in [
        (eye_left_uuid, "eye_left", 1.0),
        (eye_right_uuid, "eye_right", -1.0),
    ]:
        kfs = [
            make_keyframe("scale", 0.00, x=1.0, y=1.0, z=1.0),
            make_keyframe("scale", 0.04, x=1.4, y=1.4, z=1.4),  # widen first
            make_keyframe("scale", 0.10, x=1.0, y=0.1, z=1.0),  # blink shut
            make_keyframe("scale", 0.20, x=0.0, y=0.0, z=0.0),
            make_keyframe("scale", 0.70, x=0.0, y=0.0, z=0.0),
            make_keyframe("position", 0.00, x=0, y=0, z=0),
            make_keyframe("position", 0.20, x=0, y=-3.0, z=0),
            make_keyframe("rotation", 0.00, x=0, y=0, z=0),
            make_keyframe("rotation", 0.20, x=0, y=180 * sign, z=90 * sign),
        ]
        diss_animators[sub_uuid] = {
            "name": sub_name, "type": "bone", "keyframes": kfs,
        }

    # Smile: lingers a moment longer (creepy), then snaps shut.
    smile_diss_kfs = [
        make_keyframe("scale", 0.00, x=1.05, y=1.0, z=1.0),
        make_keyframe("scale", 0.08, x=1.4, y=1.0, z=1.0),  # gets WIDER first
        make_keyframe("scale", 0.16, x=1.6, y=0.8, z=1.0),
        make_keyframe("scale", 0.22, x=1.4, y=0.1, z=1.0),
        make_keyframe("scale", 0.26, x=0.0, y=0.0, z=0.0),
        make_keyframe("scale", 0.70, x=0.0, y=0.0, z=0.0),
        make_keyframe("position", 0.00, x=0, y=0, z=0),
        make_keyframe("position", 0.20, x=0, y=-2.5, z=0),
    ]
    diss_animators[smile_uuid] = {
        "name": "smile", "type": "bone", "keyframes": smile_diss_kfs,
    }

    # Spring segments: coil back rapidly. Y returns to -final_y across 0.0..0.30.
    for idx, ((gu, g), final_y) in enumerate(zip(spring_groups, spring_extend_y)):
        kfs = []
        # Position: start at 0 (extended), retract to -final_y.
        kfs.append(make_keyframe("position", 0.00, x=0, y=0, z=0))
        # Top segments retract first (idx 3 is highest)
        delay = (3 - idx) * 0.03  # top segs lead
        retract_end = 0.30
        kfs.append(make_keyframe("position", delay, x=0, y=0, z=0))
        kfs.append(make_keyframe("position", delay + 0.05, x=0, y=-final_y * 0.4, z=0))
        kfs.append(make_keyframe("position", delay + 0.10, x=0, y=-final_y * 0.8, z=0))
        kfs.append(make_keyframe("position", delay + 0.18, x=0, y=-final_y, z=0))
        kfs.append(make_keyframe("position", retract_end, x=0, y=-final_y, z=0))
        kfs.append(make_keyframe("position", 0.70, x=0, y=-final_y, z=0))
        # Scale: shrinks at the end with the rest of the box
        kfs.append(make_keyframe("scale", 0.00, x=1.0, y=1.0, z=1.0))
        kfs.append(make_keyframe("scale", delay + 0.05, x=1.0, y=1.4, z=1.0))  # squash from compression
        kfs.append(make_keyframe("scale", retract_end, x=1.0, y=1.0, z=1.0))
        kfs.append(make_keyframe("scale", 0.45, x=1.0, y=1.0, z=1.0))
        kfs.append(make_keyframe("scale", 0.55, x=0.5, y=0.5, z=0.5))
        kfs.append(make_keyframe("scale", 0.70, x=0.0, y=0.0, z=0.0))
        # Rotation: small jitter during retraction
        for k in range(10):
            t = delay + (k / 10.0) * 0.18
            ph = k * 1.2
            rz = math.sin(ph) * 8.0
            rx = math.cos(ph) * 5.0
            kfs.append(make_keyframe("rotation", t, x=rx, y=0, z=rz))
        diss_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    # Lid: snaps closed at 0.25..0.35 — rotation goes from -135 back to 0.
    lid_diss_kfs = [
        make_keyframe("rotation", 0.00, x=-135, y=0, z=0),
        make_keyframe("rotation", 0.20, x=-135, y=0, z=0),
        # Snap closed in 0.10s
        make_keyframe("rotation", 0.25, x=-100, y=0, z=0),
        make_keyframe("rotation", 0.30, x=-30, y=0, z=0),
        make_keyframe("rotation", 0.34, x=10, y=0, z=0),  # overshoot
        make_keyframe("rotation", 0.38, x=-5, y=0, z=0),  # bounce back
        make_keyframe("rotation", 0.42, x=0, y=0, z=0),
        make_keyframe("rotation", 0.70, x=0, y=0, z=0),
        # Scale stays 1 then shrinks at end
        make_keyframe("scale", 0.00, x=1.0, y=1.0, z=1.0),
        make_keyframe("scale", 0.45, x=1.0, y=1.0, z=1.0),
        make_keyframe("scale", 0.70, x=0.0, y=0.0, z=0.0),
    ]
    diss_animators[lid_uuid] = {
        "name": "box_lid", "type": "bone", "keyframes": lid_diss_kfs,
    }

    # Crank: stops moving, just shrinks at end.
    crank_diss_kfs = [
        make_keyframe("rotation", 0.00, x=1080, y=0, z=0),
        make_keyframe("rotation", 0.30, x=1080, y=0, z=0),
        # Reverse spin slightly during box close (recoiling tension)
        make_keyframe("rotation", 0.40, x=1000, y=0, z=0),
        make_keyframe("rotation", 0.45, x=980, y=0, z=0),
        make_keyframe("rotation", 0.70, x=980, y=0, z=0),
        make_keyframe("scale", 0.00, x=1.0, y=1.0, z=1.0),
        make_keyframe("scale", 0.45, x=1.0, y=1.0, z=1.0),
        make_keyframe("scale", 0.70, x=0.0, y=0.0, z=0.0),
    ]
    diss_animators[crank_uuid] = {
        "name": "crank", "type": "bone", "keyframes": crank_diss_kfs,
    }

    # Box sides: rapid scale to 0 in the final phase. Each side flies inward
    # slightly first, then disappears.
    inward_dirs = [
        (0, 0, -2.5),   # front pulled back
        (0, 0,  2.5),   # back pulled forward
        ( 2.5, 0, 0),   # left pulled right
        (-2.5, 0, 0),   # right pulled left
        (0, 0, 0),      # base stays
    ]
    for idx, ((gu, g), dirv) in enumerate(zip(side_groups, inward_dirs)):
        dx, dy, dz = dirv
        kfs = []
        kfs.append(make_keyframe("scale", 0.00, x=1.0, y=1.0, z=1.0))
        kfs.append(make_keyframe("scale", 0.45, x=1.0, y=1.0, z=1.0))
        # Compress on lid close
        kfs.append(make_keyframe("scale", 0.30, x=1.05, y=0.95, z=1.05))
        kfs.append(make_keyframe("scale", 0.40, x=1.0, y=1.0, z=1.0))
        # Implode
        kfs.append(make_keyframe("scale", 0.55, x=0.5, y=0.5, z=0.5))
        kfs.append(make_keyframe("scale", 0.70, x=0.0, y=0.0, z=0.0))
        # Position: pulled inward in final shrink
        kfs.append(make_keyframe("position", 0.00, x=0, y=0, z=0))
        kfs.append(make_keyframe("position", 0.45, x=0, y=0, z=0))
        kfs.append(make_keyframe("position", 0.70, x=dx, y=dy, z=dz))
        # Rotation: tumble during implode
        rxe = 90 * (1 if idx % 2 else -1)
        rye = 60 * (1 if idx % 3 else -1)
        kfs.append(make_keyframe("rotation", 0.00, x=0, y=0, z=0))
        kfs.append(make_keyframe("rotation", 0.45, x=0, y=0, z=0))
        kfs.append(make_keyframe("rotation", 0.70, x=rxe, y=rye, z=0))
        diss_animators[gu] = {
            "name": g["name"], "type": "bone", "keyframes": kfs,
        }

    # Ground base — fades last with a flat collapse.
    ground_diss_kfs = [
        make_keyframe("scale", 0.00, x=1.0, y=1.0, z=1.0),
        make_keyframe("scale", 0.30, x=1.05, y=0.95, z=1.05),
        make_keyframe("scale", 0.50, x=1.0, y=1.0, z=1.0),
        make_keyframe("scale", 0.60, x=0.7, y=0.2, z=0.7),
        make_keyframe("scale", 0.70, x=0.0, y=0.0, z=0.0),
        make_keyframe("position", 0.00, x=0, y=0, z=0),
        make_keyframe("position", 0.55, x=0, y=-0.3, z=0),
        make_keyframe("position", 0.70, x=0, y=-0.6, z=0),
    ]
    diss_animators[ground_uuid] = {
        "name": "ground_base", "type": "bone", "keyframes": ground_diss_kfs,
    }

    b.add_animation("dissipate", length=0.7, animators=diss_animators,
                    loop="once", override=True)

    # ---- WRITE ----
    b.write(OUTPUT_PATH)
    print(f"Wrote {OUTPUT_PATH}")


if __name__ == "__main__":
    build()
