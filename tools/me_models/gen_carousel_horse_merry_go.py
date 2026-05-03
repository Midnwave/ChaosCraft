#!/usr/bin/env python3
"""
Generator for carousel_horse_merry_go.bbmodel

CAROUSEL HORSE ORBITAL - "Merry Go"
Three carousel horses orbiting the caster on invisible poles, going up and
down as they orbit - the classic carousel motion. Each horse white with
pastel pink and gold accents. Carousel pole above each horse connects to
central canopy disc.

Geometry:
  - 3 carousel horse bone groups orbiting at radius 8, Y=3
    Each horse:
      * Body: 3 overlapping cubes (barrel chest)
      * Head: 2 cubes
      * Mane: 2 flat slabs on neck
      * Legs: 4 small cubes (cantering pose via rotation)
      * Tail: 2 flat slabs angled backward
  - Carousel pole: thin long cube per horse, Y=3 -> Y=12, own bone
  - Central canopy disc: ring of 12 slabs at Y=12, own bone
  - Canopy decorations: 8 small slabs around canopy edge, each own bone
  - Ground ring: 8 flat slabs at Y=0, own bone

Textures:
  - 0 (carousel_horse_white, 64x64): pearl white horse body
  - 1 (carnival_gold, 64x64): poles, canopy, ground ring

Animations:
  - spawn (1.2s, override=true)
  - idle (4.0s, loop, override=false)
  - dissipate (0.8s, override=true)
"""

import math
import sys
from pathlib import Path

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
    gradient_horizontal_band,
    gradient_radial,
    add_noise_overlay,
    color_lerp,
)


# -----------------------------------------------------------------------------
# Texture 0 - Carousel Horse White (pearl white, painted carved wood)
#   Pearl white base #f8f4f0, pearl shadow #d8d0c8, carved detail #b0a898,
#   bright highlight #ffffff, emissive faint pink #ffe8f0.
# Specular at (10,6) pure white large cluster. Pink emissive accents.
# -----------------------------------------------------------------------------
def build_carousel_horse_white_texture():
    W, H = 64, 64
    base   = hex_to_rgba("#f8f4f0")  # pearl white
    shadow = hex_to_rgba("#d8d0c8")  # pearl shadow
    carved = hex_to_rgba("#b0a898")  # carved wood detail
    spec   = hex_to_rgba("#ffffff")  # pure white highlight
    pink   = hex_to_rgba("#ffe8f0")  # faint pink emissive
    pixels = [base] * (W * H)

    # Top quarter (head/face area, 0-16): pearl gradient with subtle pink emissive top
    gradient_horizontal_band(pixels, W, H, 0, 12, pink, base)
    gradient_horizontal_band(pixels, W, H, 12, 22, base, shadow)

    # Mid (body area, 22-44): mostly base white with carved wood grain
    fill_rect(pixels, W, H, 0, 22, W, 44, base)
    # Vertical wood-grain lines suggesting carved planks
    for x in range(W):
        if x % 7 == 3:
            for y in range(22, 44):
                pixels[y * W + x] = shadow
        if x % 11 == 5:
            for y in range(22, 44):
                pixels[y * W + x] = carved

    # Carved horizontal banding (saddle / chest detail)
    fill_rect(pixels, W, H, 0, 28, W, 30, shadow)
    fill_rect(pixels, W, H, 0, 29, W, 30, carved)
    fill_rect(pixels, W, H, 0, 36, W, 38, shadow)
    fill_rect(pixels, W, H, 0, 37, W, 38, carved)

    # Lower quarter (legs, 44-56): pearl with leg muscle shadows
    gradient_horizontal_band(pixels, W, H, 44, 56, base, shadow)
    # Vertical leg shading
    for x in range(W):
        if x % 5 == 2:
            for y in range(44, 56):
                r, g, b, a = pixels[y * W + x]
                pixels[y * W + x] = (max(0, r - 12), max(0, g - 12), max(0, b - 12), a)

    # Bottom strip (hoof area, 56-64): darker carved
    gradient_horizontal_band(pixels, W, H, 56, 64, shadow, carved)

    # Specular cluster at (10,6) — bright pure-white large cluster
    fill_rect(pixels, W, H, 10, 6, 18, 12, spec)
    fill_rect(pixels, W, H, 11, 7, 17, 11, spec)
    # Soft halo around it
    for y in range(4, 14):
        for x in range(8, 20):
            d = math.sqrt((x - 14) ** 2 + (y - 8) ** 2)
            if d < 6 and pixels[y * W + x] != spec:
                base_p = pixels[y * W + x]
                pixels[y * W + x] = color_lerp(spec, base_p, d / 6.0)

    # Additional smaller specular highlights scattered (carved wood gloss)
    spec_spots = [(34, 4), (52, 8), (28, 24), (48, 30), (20, 38), (44, 46),
                  (8, 50), (36, 54), (54, 58), (24, 60)]
    for (cx, cy) in spec_spots:
        fill_rect(pixels, W, H, cx, cy, cx + 2, cy + 2, spec)
        if cx + 3 < W and cy + 1 < H:
            base_p = pixels[cy * W + cx + 3]
            pixels[cy * W + cx + 3] = color_lerp(spec, base_p, 0.5)

    # Pink emissive accents (decorative paint) — 2x2 dots
    pink_spots = [(6, 14), (32, 16), (54, 18), (16, 32), (42, 34),
                  (8, 46), (50, 48), (28, 56), (60, 60)]
    for (cx, cy) in pink_spots:
        fill_rect(pixels, W, H, cx, cy, min(W, cx + 2), min(H, cy + 2), pink)

    # Carved curl/flourish decoration arcs (small)
    for cx in [16, 36, 48]:
        cy = 24
        for ang_deg in range(0, 180, 15):
            ang = math.radians(ang_deg)
            px = int(cx + math.cos(ang) * 4)
            py = int(cy + math.sin(ang) * 2)
            if 0 <= px < W and 0 <= py < H:
                pixels[py * W + px] = carved

    # Light noise to suggest wood grain
    add_noise_overlay(pixels, W, H, carved, spec, density=0.04, seed=137)
    return png_from_pixels(pixels, W, H)


# -----------------------------------------------------------------------------
# Texture 1 - Carnival Gold (deep gold, bright gold, highlight, shadow, emissive)
#   Deep gold base #c09020, bright gold mid #e8b830, highlight #ffe870,
#   shadow #806010, emissive #ffd040.
# -----------------------------------------------------------------------------
def build_carnival_gold_texture():
    W, H = 64, 64
    deep   = hex_to_rgba("#c09020")  # deep gold base
    bright = hex_to_rgba("#e8b830")  # bright gold mid
    hi     = hex_to_rgba("#ffe870")  # highlight
    sh     = hex_to_rgba("#806010")  # shadow
    em     = hex_to_rgba("#ffd040")  # emissive
    pixels = [bright] * (W * H)

    # Top band: highlight at top -> bright (reflective top of pole / canopy crown)
    gradient_horizontal_band(pixels, W, H, 0, 14, hi, bright)
    # Mid band: bright gold with vertical pole-like banding
    fill_rect(pixels, W, H, 0, 14, W, 36, bright)
    # Vertical bands every 8px (pole rib pattern)
    for x in range(W):
        if x % 8 == 0:
            for y in range(14, 36):
                pixels[y * W + x] = deep
        if x % 8 == 4:
            for y in range(14, 36):
                pixels[y * W + x] = hi

    # Lower mid: bright -> deep gold
    gradient_horizontal_band(pixels, W, H, 36, 52, bright, deep)
    # Bottom band: deep gold -> shadow (base/ground)
    gradient_horizontal_band(pixels, W, H, 52, 64, deep, sh)

    # Decorative diamond pattern (canopy ornament look) in mid band
    diamond_spots = [(8, 22), (20, 26), (32, 22), (44, 26), (56, 22),
                     (14, 30), (28, 30), (42, 30), (54, 30)]
    for (cx, cy) in diamond_spots:
        # diamond shape
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                if abs(dx) + abs(dy) <= 2:
                    px, py = cx + dx, cy + dy
                    if 0 <= px < W and 0 <= py < H:
                        if abs(dx) + abs(dy) == 2:
                            pixels[py * W + px] = sh
                        elif abs(dx) + abs(dy) == 1:
                            pixels[py * W + px] = deep
                        else:
                            pixels[py * W + px] = em

    # Strong specular streaks (gold reflection)
    for y in range(2, 12):
        for x in range(0, W):
            if (x + y) % 5 == 0:
                pixels[y * W + x] = hi
            if (x + y) % 11 == 0:
                pixels[y * W + x] = (255, 255, 255, 255)

    # Emissive sparkle dots
    sparkles = [(12, 8), (24, 4), (40, 10), (52, 6), (60, 12),
                (4, 40), (16, 44), (32, 42), (48, 46), (58, 40),
                (10, 56), (28, 58), (44, 56), (54, 60)]
    for (cx, cy) in sparkles:
        fill_rect(pixels, W, H, cx, cy, min(W, cx + 2), min(H, cy + 2), em)

    # Carved twirl decoration in canopy section
    for arc_cx, arc_cy in [(16, 20), (32, 20), (48, 20)]:
        for ang_deg in range(0, 360, 20):
            ang = math.radians(ang_deg)
            px = int(arc_cx + math.cos(ang) * 3)
            py = int(arc_cy + math.sin(ang) * 3)
            if 0 <= px < W and 0 <= py < H:
                pixels[py * W + px] = sh

    # Bottom shadow grain
    for x in range(W):
        if x % 6 == 2:
            for y in range(54, 64):
                r, g, bl, a = pixels[y * W + x]
                pixels[y * W + x] = (max(0, r - 30), max(0, g - 24), max(0, bl - 12), a)

    # Light noise for gold lustre
    add_noise_overlay(pixels, W, H, sh, hi, density=0.05, seed=219)
    return png_from_pixels(pixels, W, H)


# -----------------------------------------------------------------------------
# Build model
# -----------------------------------------------------------------------------
b = Builder("carousel_horse_merry_go", resolution=(64, 64), visible_box=(10, 14, 0))


# Standard horse-body face UV pulls from texture 0
def horse_uv(u, v, sx=10, sy=10, tex=0):
    rect = [u, v, u + sx, v + sy]
    return {d: {"uv": rect, "texture": tex} for d in ("north", "east", "south", "west", "up", "down")}


def gold_uv(u, v, sx=10, sy=10, tex=1):
    rect = [u, v, u + sx, v + sy]
    return {d: {"uv": rect, "texture": tex} for d in ("north", "east", "south", "west", "up", "down")}


# Build a single carousel horse and return (horse_bone_dict, pole_bone_dict)
def build_horse(idx, angle_deg, orbit_radius, base_y):
    """
    Build a horse facing tangent to its orbit (forward = direction of travel).
    All cube positions are in horse-local space; the surrounding bone group
    handles orbit position and rotation. Forward = +X local. Up = +Y local.
    """
    # Local origin: hooves at y = base_y level (set bone origin to orbit point)
    cubes_horse = []

    # ---- Body: 3 overlapping barrel-chest cubes (along X, horse facing +X) ----
    # main barrel
    body_main = b.add_cube(
        f"horse{idx}_body_main",
        from_=[-1.5, 1.0, -1.0],
        to_=[1.7, 2.6, 1.0],
        faces=horse_uv(2, 22, 12, 16),
    )
    cubes_horse.append(body_main)
    # chest bulge (forward)
    body_chest = b.add_cube(
        f"horse{idx}_body_chest",
        from_=[1.2, 1.2, -0.9],
        to_=[2.4, 2.5, 0.9],
        faces=horse_uv(20, 24, 10, 12),
    )
    cubes_horse.append(body_chest)
    # rear haunch
    body_rear = b.add_cube(
        f"horse{idx}_body_rear",
        from_=[-2.2, 1.1, -0.95],
        to_=[-1.0, 2.55, 0.95],
        faces=horse_uv(34, 24, 10, 12),
    )
    cubes_horse.append(body_rear)

    # ---- Head: 2 cubes ----
    # neck (angled forward and up via rotation on cube)
    neck = b.add_cube(
        f"horse{idx}_neck",
        from_=[1.8, 2.4, -0.5],
        to_=[2.8, 4.0, 0.5],
        origin=[2.0, 2.4, 0.0],
        rotation=[0, 0, 25],  # tilt forward
        faces=horse_uv(46, 22, 8, 14),
    )
    cubes_horse.append(neck)
    # head
    head = b.add_cube(
        f"horse{idx}_head",
        from_=[2.6, 3.6, -0.55],
        to_=[4.1, 4.6, 0.55],
        origin=[2.6, 3.8, 0.0],
        rotation=[0, 0, -10],  # slight downward muzzle
        faces=horse_uv(2, 4, 14, 14),
    )
    cubes_horse.append(head)

    # ---- Mane: 2 flat slab pieces on neck side ----
    mane_top = b.add_cube(
        f"horse{idx}_mane_top",
        from_=[1.7, 3.4, -0.15],
        to_=[2.6, 4.6, 0.15],
        origin=[1.9, 3.4, 0.0],
        rotation=[0, 0, 25],
        faces=horse_uv(2, 50, 10, 12),
    )
    cubes_horse.append(mane_top)
    mane_lower = b.add_cube(
        f"horse{idx}_mane_lower",
        from_=[1.4, 2.6, -0.12],
        to_=[2.2, 3.8, 0.12],
        origin=[1.6, 2.6, 0.0],
        rotation=[0, 0, 35],
        faces=horse_uv(14, 50, 10, 12),
    )
    cubes_horse.append(mane_lower)

    # ---- Legs: 4 small cube legs in cantering pose ----
    # Front-right (forward, raised)
    leg_fr = b.add_cube(
        f"horse{idx}_leg_fr",
        from_=[0.8, -0.2, 0.4],
        to_=[1.4, 1.2, 0.9],
        origin=[1.1, 1.2, 0.65],
        rotation=[0, 0, -20],  # angled forward (cantering)
        faces=horse_uv(26, 50, 6, 12),
    )
    cubes_horse.append(leg_fr)
    # Front-left (forward, lower)
    leg_fl = b.add_cube(
        f"horse{idx}_leg_fl",
        from_=[0.8, -0.2, -0.9],
        to_=[1.4, 1.2, -0.4],
        origin=[1.1, 1.2, -0.65],
        rotation=[0, 0, -10],
        faces=horse_uv(34, 50, 6, 12),
    )
    cubes_horse.append(leg_fl)
    # Back-right (rear, planted-back)
    leg_br = b.add_cube(
        f"horse{idx}_leg_br",
        from_=[-1.8, -0.1, 0.4],
        to_=[-1.2, 1.3, 0.9],
        origin=[-1.5, 1.3, 0.65],
        rotation=[0, 0, 18],  # angled rearward
        faces=horse_uv(42, 50, 6, 12),
    )
    cubes_horse.append(leg_br)
    # Back-left (rear, slightly raised)
    leg_bl = b.add_cube(
        f"horse{idx}_leg_bl",
        from_=[-1.8, 0.0, -0.9],
        to_=[-1.2, 1.4, -0.4],
        origin=[-1.5, 1.4, -0.65],
        rotation=[0, 0, 10],
        faces=horse_uv(50, 50, 6, 12),
    )
    cubes_horse.append(leg_bl)

    # ---- Tail: 2 flat slab pieces angling backward ----
    tail_upper = b.add_cube(
        f"horse{idx}_tail_upper",
        from_=[-3.0, 1.8, -0.18],
        to_=[-2.0, 2.7, 0.18],
        origin=[-2.0, 2.5, 0.0],
        rotation=[0, 0, -25],  # angles backward and down
        faces=horse_uv(2, 38, 10, 10),
    )
    cubes_horse.append(tail_upper)
    tail_lower = b.add_cube(
        f"horse{idx}_tail_lower",
        from_=[-3.6, 1.0, -0.16],
        to_=[-2.7, 2.0, 0.16],
        origin=[-2.7, 2.0, 0.0],
        rotation=[0, 0, -45],
        faces=horse_uv(14, 38, 10, 10),
    )
    cubes_horse.append(tail_lower)

    # Bone for the horse
    # Place at orbit position. Rotate bone Y so horse faces tangent to orbit.
    angle_rad = math.radians(angle_deg)
    cx = math.cos(angle_rad) * orbit_radius
    cz = math.sin(angle_rad) * orbit_radius
    # Forward direction (tangent) is angle + 90 degrees
    facing_deg = angle_deg + 90.0

    horse_bone = b.make_group(
        f"horse{idx}",
        origin=[cx, base_y, cz],
        children=cubes_horse,
        rotation=[0, facing_deg, 0],
    )

    # Pole: thin long cube, runs from horse back (Y=base_y+2) up to Y=12
    pole_top_y = 12.0
    pole_bottom_y = base_y + 1.5
    pole_cube = b.add_cube(
        f"pole{idx}_cube",
        from_=[cx - 0.18, pole_bottom_y, cz - 0.18],
        to_=[cx + 0.18, pole_top_y, cz + 0.18],
        faces=gold_uv(20, 4, 8, 56),
    )
    pole_bone = b.make_group(
        f"pole{idx}",
        origin=[cx, pole_top_y, cz],  # origin at top so it can retract upward
        children=[pole_cube],
    )

    return horse_bone, pole_bone, (cx, cz, angle_deg)


# Build the 3 horses spaced 120 deg apart at radius 8, base_y = 3
HORSE_RADIUS = 8.0
HORSE_BASE_Y = 3.0
NUM_HORSES = 3

horse_bones = []   # list of (bone_uuid, name, angle_deg, cx, cz)
pole_bones = []    # list of (bone_uuid, name, cx, cz)
horse_root_children = []
pole_root_children = []

for i in range(NUM_HORSES):
    ang = i * (360.0 / NUM_HORSES)
    h_bone, p_bone, (cx, cz, ang_deg) = build_horse(i, ang, HORSE_RADIUS, HORSE_BASE_Y)
    horse_bones.append((h_bone["uuid"], h_bone["name"], ang_deg, cx, cz))
    pole_bones.append((p_bone["uuid"], p_bone["name"], cx, cz))
    horse_root_children.append(h_bone)
    pole_root_children.append(p_bone)


# -----------------------------------------------------------------------------
# Central canopy disc - flat ring of 12 slabs at Y=12
# -----------------------------------------------------------------------------
CANOPY_Y = 12.0
CANOPY_RADIUS = 9.0
CANOPY_SLAB_COUNT = 12

canopy_cubes = []
for s_idx in range(CANOPY_SLAB_COUNT):
    a0 = (s_idx / CANOPY_SLAB_COUNT) * math.tau
    a1 = ((s_idx + 1) / CANOPY_SLAB_COUNT) * math.tau
    mid_a = (a0 + a1) * 0.5
    cos_a = math.cos(mid_a)
    sin_a = math.sin(mid_a)
    cx = CANOPY_RADIUS * cos_a
    cz = CANOPY_RADIUS * sin_a
    slab_w_radial = 2.4
    slab_w_tang = 2.0 * CANOPY_RADIUS * math.tan(math.pi / CANOPY_SLAB_COUNT) * 1.05

    cube_uuid = b.add_cube(
        f"canopy_slab_{s_idx}",
        from_=[cx - slab_w_radial * 0.5, CANOPY_Y - 0.25, cz - slab_w_tang * 0.5],
        to_=[cx + slab_w_radial * 0.5, CANOPY_Y + 0.25, cz + slab_w_tang * 0.5],
        origin=[cx, CANOPY_Y, cz],
        rotation=[0, math.degrees(mid_a), 0],
        faces=gold_uv(2, 14, 28, 20),
    )
    canopy_cubes.append(cube_uuid)

# Central canopy hub (small cube on top center to "anchor" the disc visually)
canopy_hub = b.add_cube(
    "canopy_hub",
    from_=[-0.6, CANOPY_Y - 0.1, -0.6],
    to_=[0.6, CANOPY_Y + 1.0, 0.6],
    faces=gold_uv(40, 2, 18, 18),
)
canopy_cubes.append(canopy_hub)

canopy_bone = b.make_group(
    "canopy_disc",
    origin=[0, CANOPY_Y, 0],
    children=canopy_cubes,
)


# -----------------------------------------------------------------------------
# Canopy decorations - 8 small flat slab pieces around canopy edge,
# angled slightly downward. Each own bone.
# -----------------------------------------------------------------------------
DECO_COUNT = 8
DECO_RADIUS = CANOPY_RADIUS + 0.3

deco_bones = []   # list of (uuid, name, angle_rad, cx, cz)
deco_root_children = []

for d_idx in range(DECO_COUNT):
    mid_a = ((d_idx + 0.5) / DECO_COUNT) * math.tau
    cos_a = math.cos(mid_a)
    sin_a = math.sin(mid_a)
    cx = DECO_RADIUS * cos_a
    cz = DECO_RADIUS * sin_a

    deco_cube = b.add_cube(
        f"deco_slab_{d_idx}",
        from_=[cx - 0.3, CANOPY_Y - 0.6, cz - 0.9],
        to_=[cx + 0.3, CANOPY_Y + 0.4, cz + 0.9],
        origin=[cx, CANOPY_Y, cz],
        rotation=[0, math.degrees(mid_a), -25],  # angled downward outward
        faces=gold_uv(34, 36, 16, 24),
    )
    deco_bone = b.make_group(
        f"deco_bone_{d_idx}",
        origin=[cx, CANOPY_Y, cz],
        children=[deco_cube],
    )
    deco_bones.append((deco_bone["uuid"], deco_bone["name"], mid_a, cx, cz))
    deco_root_children.append(deco_bone)


# -----------------------------------------------------------------------------
# Ground ring - 8 flat slabs at Y=0
# -----------------------------------------------------------------------------
GROUND_RADIUS = 9.5
GROUND_COUNT = 8

ground_cubes = []
for g_idx in range(GROUND_COUNT):
    mid_a = ((g_idx + 0.5) / GROUND_COUNT) * math.tau
    cos_a = math.cos(mid_a)
    sin_a = math.sin(mid_a)
    cx = GROUND_RADIUS * cos_a
    cz = GROUND_RADIUS * sin_a
    slab_w_radial = 2.0
    slab_w_tang = 2.0 * GROUND_RADIUS * math.tan(math.pi / GROUND_COUNT) * 1.05

    cube_uuid = b.add_cube(
        f"ground_slab_{g_idx}",
        from_=[cx - slab_w_radial * 0.5, -0.05, cz - slab_w_tang * 0.5],
        to_=[cx + slab_w_radial * 0.5, 0.25, cz + slab_w_tang * 0.5],
        origin=[cx, 0.1, cz],
        rotation=[0, math.degrees(mid_a), 0],
        faces=gold_uv(2, 36, 28, 12),
    )
    ground_cubes.append(cube_uuid)

ground_bone = b.make_group(
    "ground_ring",
    origin=[0, 0, 0],
    children=ground_cubes,
)


# -----------------------------------------------------------------------------
# Master root bone - holds everything for the global carousel rotation
# -----------------------------------------------------------------------------
master = b.make_group(
    "carousel_root",
    origin=[0, 0, 0],
    children=(
        [canopy_bone]
        + deco_root_children
        + pole_root_children
        + horse_root_children
        + [ground_bone]
    ),
)
b.outliner.append(master)


# =============================================================================
# ANIMATIONS
# =============================================================================

# ---------- SPAWN (1.2s, override=true) ----------
# 0.0s: canopy disc appears at Y=12
# 0.2s: poles extend downward from canopy + ground ring expands
# 0.4s: canopy decorations deploy
# 0.5s: horses materialise at pole bases
spawn_animators = {}

# Canopy disc: scale up at 0.0s
spawn_animators[canopy_bone["uuid"]] = {
    "name": canopy_bone["name"], "type": "bone",
    "keyframes": [
        make_keyframe("scale", 0.0, 0.05, 0.05, 0.05),
        make_keyframe("scale", 0.20, 1.10, 1.05, 1.10),
        make_keyframe("scale", 0.35, 1.0, 1.0, 1.0),
        make_keyframe("scale", 1.2, 1.0, 1.0, 1.0),
        make_keyframe("rotation", 0.0, 0, -45, 0),
        make_keyframe("rotation", 0.35, 0, 5, 0),
        make_keyframe("rotation", 1.2, 0, 0, 0),
    ],
}

# Poles: extend downward from canopy at 0.2s (origin at top, scale Y from 0 -> 1)
for (uuid_, name, cx, cz) in pole_bones:
    spawn_animators[uuid_] = {
        "name": name, "type": "bone",
        "keyframes": [
            make_keyframe("scale", 0.0, 0.5, 0.0, 0.5),
            make_keyframe("scale", 0.20, 0.5, 0.0, 0.5),
            make_keyframe("scale", 0.45, 1.1, 1.0, 1.1),
            make_keyframe("scale", 0.55, 1.0, 1.0, 1.0),
            make_keyframe("scale", 1.2, 1.0, 1.0, 1.0),
        ],
    }

# Ground ring: expand outward at 0.2s
spawn_animators[ground_bone["uuid"]] = {
    "name": ground_bone["name"], "type": "bone",
    "keyframes": [
        make_keyframe("scale", 0.0, 0.05, 0.05, 0.05),
        make_keyframe("scale", 0.20, 0.05, 0.05, 0.05),
        make_keyframe("scale", 0.40, 1.15, 1.0, 1.15),
        make_keyframe("scale", 0.55, 1.0, 1.0, 1.0),
        make_keyframe("scale", 1.2, 1.0, 1.0, 1.0),
        make_keyframe("rotation", 0.0, 0, 0, 0),
        make_keyframe("rotation", 0.55, 0, 22.5, 0),
        make_keyframe("rotation", 1.2, 0, 22.5, 0),
    ],
}

# Canopy decorations: deploy at 0.4s
for idx, (uuid_, name, mid_a, dx, dz) in enumerate(deco_bones):
    stagger = 0.40 + (idx * 0.015)
    spawn_animators[uuid_] = {
        "name": name, "type": "bone",
        "keyframes": [
            make_keyframe("scale", 0.0, 0.01, 0.01, 0.01),
            make_keyframe("scale", stagger, 0.01, 0.01, 0.01),
            make_keyframe("scale", stagger + 0.10, 1.20, 1.20, 1.20),
            make_keyframe("scale", stagger + 0.18, 1.0, 1.0, 1.0),
            make_keyframe("scale", 1.2, 1.0, 1.0, 1.0),
            make_keyframe("position", 0.0, 0, 1.5, 0),
            make_keyframe("position", stagger, 0, 1.5, 0),
            make_keyframe("position", stagger + 0.15, 0, 0, 0),
            make_keyframe("position", 1.2, 0, 0, 0),
        ],
    }

# Horses: materialise at pole bases at 0.5s
for idx, (uuid_, name, ang_deg, cx, cz) in enumerate(horse_bones):
    stagger = 0.50 + (idx * 0.06)
    spawn_animators[uuid_] = {
        "name": name, "type": "bone",
        "keyframes": [
            make_keyframe("scale", 0.0, 0.01, 0.01, 0.01),
            make_keyframe("scale", stagger, 0.01, 0.01, 0.01),
            make_keyframe("scale", stagger + 0.08, 1.18, 1.18, 1.18),
            make_keyframe("scale", stagger + 0.18, 0.95, 1.05, 0.95),
            make_keyframe("scale", stagger + 0.28, 1.0, 1.0, 1.0),
            make_keyframe("scale", 1.2, 1.0, 1.0, 1.0),
            make_keyframe("position", 0.0, 0, 1.5, 0),
            make_keyframe("position", stagger, 0, 1.5, 0),
            make_keyframe("position", stagger + 0.18, 0, -0.4, 0),
            make_keyframe("position", stagger + 0.28, 0, 0, 0),
            make_keyframe("position", 1.2, 0, 0, 0),
        ],
    }

b.add_animation("spawn", length=1.2, animators=spawn_animators, loop="once", override=True)


# ---------- IDLE (4.0s, loop, override=false) ----------
# Carousel turning: each horse independently oscillates Y by +/-1.5 on different
# phases. Each pole follows its horse's Y. Heads/manes get secondary motion.
# Canopy slowly rotates. Ground ring slowly counter-rotates.
idle_animators = {}

# Canopy: slow continuous Y rotation (one full revolution in 4s)
canopy_rot = []
samples = 16
for s in range(samples + 1):
    t = (s / samples) * 4.0
    deg = (t / 4.0) * 360.0
    canopy_rot.append(make_keyframe("rotation", t, 0, deg, 0))
idle_animators[canopy_bone["uuid"]] = {
    "name": canopy_bone["name"], "type": "bone",
    "keyframes": canopy_rot,
}

# Ground ring: slow counter-rotation (half revolution in 4s, opposite dir)
ground_rot = []
for s in range(samples + 1):
    t = (s / samples) * 4.0
    deg = -(t / 4.0) * 180.0 + 22.5  # offset start
    ground_rot.append(make_keyframe("rotation", t, 0, deg, 0))
idle_animators[ground_bone["uuid"]] = {
    "name": ground_bone["name"], "type": "bone",
    "keyframes": ground_rot,
}

# Horses: bob up and down +/-1.5 with independent phases
# All 3 horses orbit (rotate around Y at world origin) — but the carousel root
# rotation handles ORBIT for the canopy/ground; horses themselves we'll move
# along orbit by rotating the bone's parent transform. Since each horse bone
# is positioned at a fixed cx/cz, we apply rotation around y to move them
# along the orbit. We do this by adding rotation to the master root via
# canopy already rotating; but horses are siblings under master. We'll
# rotate horse bones via their own rotation keyframes.
# To implement "carousel turning" while keeping horses facing tangent, each
# horse keeps its local facing and we rotate the *whole assembly* by adding
# orbital rotation to a Y-position via cyclic position keyframes.
#
# Approach: each horse bone gets ROTATION keyframes around Y to spin around
# world origin. Because the horse bone's origin is at (cx, base_y, cz), a Y
# rotation on this bone rotates the horse around its OWN origin, NOT world
# origin. Instead, we use POSITION keyframes that trace the orbit circle.
# For pure circular motion we sample the orbit.
ORBIT_REVS_IDLE = 1.0  # one full revolution in 4s
BOB_AMPLITUDE = 1.5

phase_per_horse = [0.0, 1.0 / 3.0, 2.0 / 3.0]
bob_period = 1.4  # seconds per bob cycle (independent of orbit)

orbit_samples = 24
for idx, (uuid_, name, ang_deg, cx0, cz0) in enumerate(horse_bones):
    base_phase_rad = math.radians(ang_deg)
    bob_phase = phase_per_horse[idx]
    pos_kfs = []
    rot_kfs = []
    for s in range(orbit_samples + 1):
        t = (s / orbit_samples) * 4.0
        # orbit angle = base + (t / 4) * 2pi
        orbit_a = base_phase_rad + (t / 4.0) * ORBIT_REVS_IDLE * math.tau
        new_cx = HORSE_RADIUS * math.cos(orbit_a)
        new_cz = HORSE_RADIUS * math.sin(orbit_a)
        # delta from bone's resting cx0/cz0 -> new position
        dx = new_cx - cx0
        dz = new_cz - cz0
        # bob
        bob = math.sin(((t / bob_period) + bob_phase) * math.tau) * BOB_AMPLITUDE
        pos_kfs.append(make_keyframe("position", t, dx, bob, dz))
        # facing rotation: tangent = orbit_a + 90 deg
        # original facing was ang_deg + 90. new facing should be degrees(orbit_a) + 90
        new_facing = math.degrees(orbit_a) + 90.0
        original_facing = ang_deg + 90.0
        d_facing = new_facing - original_facing
        rot_kfs.append(make_keyframe("rotation", t, 0, d_facing, 0))
    idle_animators[uuid_] = {
        "name": name, "type": "bone",
        "keyframes": pos_kfs + rot_kfs,
    }

# Poles follow their horse's bob and orbit (same orbit, top stays at canopy)
# Pole bone origin is at the TOP (canopy_y), so we keep top fixed while bottom
# moves with horse. Easiest: orbit pole around world origin same as horse,
# but DON'T bob (the pole connects to canopy which doesn't bob). Instead we
# just orbit it. In reality the pole would stretch/follow; we approximate by
# rotating the pole bone in Y around world origin via position keyframes.
for idx, (uuid_, name, cx0, cz0) in enumerate(pole_bones):
    base_phase_rad = math.radians(idx * (360.0 / NUM_HORSES))
    pos_kfs = []
    for s in range(orbit_samples + 1):
        t = (s / orbit_samples) * 4.0
        orbit_a = base_phase_rad + (t / 4.0) * ORBIT_REVS_IDLE * math.tau
        new_cx = HORSE_RADIUS * math.cos(orbit_a)
        new_cz = HORSE_RADIUS * math.sin(orbit_a)
        dx = new_cx - cx0
        dz = new_cz - cz0
        # also subtle bob in scale Y to suggest the pole "follows"
        pos_kfs.append(make_keyframe("position", t, dx, 0, dz))
    bob_phase = phase_per_horse[idx]
    sc_kfs = []
    for s in range(8 + 1):
        t = (s / 8) * 4.0
        bob = math.sin(((t / bob_period) + bob_phase) * math.tau) * BOB_AMPLITUDE
        # pole length scales with bob — when horse goes up, pole compresses (Y < 1)
        sy = 1.0 - (bob / 9.0)  # 9 unit pole length, +/-1.5 bob -> ~+/-0.17
        sc_kfs.append(make_keyframe("scale", t, 1.0, sy, 1.0))
    idle_animators[uuid_] = {
        "name": name, "type": "bone",
        "keyframes": pos_kfs + sc_kfs,
    }

# Canopy decorations: gentle sway in rotation Z, rotate around canopy
# Note: deco bones are children of root, not canopy, so they need to orbit too
# at canopy's rotation rate. We keyframe them to follow canopy rotation.
for idx, (uuid_, name, mid_a, dcx, dcz) in enumerate(deco_bones):
    base_a = mid_a
    pos_kfs = []
    rot_kfs = []
    sway_phase = idx * 0.13
    for s in range(orbit_samples + 1):
        t = (s / orbit_samples) * 4.0
        new_a = base_a + (t / 4.0) * math.tau  # follow canopy 1 rev / 4s
        new_cx = DECO_RADIUS * math.cos(new_a)
        new_cz = DECO_RADIUS * math.sin(new_a)
        dx = new_cx - dcx
        dz = new_cz - dcz
        pos_kfs.append(make_keyframe("position", t, dx, 0, dz))
        # rotation: face outward (radial), with slight sway in Z
        new_y_rot = math.degrees(new_a)
        orig_y_rot = math.degrees(base_a)
        d_y = new_y_rot - orig_y_rot
        sway = math.sin(((t / 1.6) + sway_phase) * math.tau) * 4.0
        rot_kfs.append(make_keyframe("rotation", t, 0, d_y, sway))
    idle_animators[uuid_] = {
        "name": name, "type": "bone",
        "keyframes": pos_kfs + rot_kfs,
    }

b.add_animation("idle", length=4.0, animators=idle_animators, loop="loop", override=False)


# ---------- DISSIPATE (0.8s, override=true) ----------
# Horses continue orbit but scale to 0 one by one (stagger 0.1s)
# Poles retract upward (scale Y -> 0 from top origin)
# Canopy disc shatters outward (slabs scale up to 1.4 horizontally then to 0)
# Ground ring contracts to 0
# Carousel winds down (rotation slows)
dissipate_animators = {}

# Horses: orbit a bit further then scale to 0, staggered
for idx, (uuid_, name, ang_deg, cx0, cz0) in enumerate(horse_bones):
    base_phase_rad = math.radians(ang_deg)
    stagger = idx * 0.10
    pos_kfs = []
    rot_kfs = []
    # continue orbit for a moment (~30 deg)
    for s in range(8 + 1):
        t = (s / 8) * 0.8
        orbit_a = base_phase_rad + (t / 0.8) * (math.pi / 6)  # ~30 deg over 0.8s
        new_cx = HORSE_RADIUS * math.cos(orbit_a)
        new_cz = HORSE_RADIUS * math.sin(orbit_a)
        dx = new_cx - cx0
        dz = new_cz - cz0
        # spiraling slightly up at end
        bob = (t / 0.8) * 0.5
        pos_kfs.append(make_keyframe("position", t, dx, bob, dz))
        new_facing = math.degrees(orbit_a) + 90.0
        original_facing = ang_deg + 90.0
        d_facing = new_facing - original_facing
        rot_kfs.append(make_keyframe("rotation", t, 0, d_facing, 0))
    sc_kfs = [
        make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
        make_keyframe("scale", stagger, 1.0, 1.0, 1.0),
        make_keyframe("scale", stagger + 0.10, 1.15, 0.85, 1.15),
        make_keyframe("scale", stagger + 0.30, 0.50, 0.50, 0.50),
        make_keyframe("scale", min(0.8, stagger + 0.45), 0.01, 0.01, 0.01),
        make_keyframe("scale", 0.8, 0.01, 0.01, 0.01),
    ]
    dissipate_animators[uuid_] = {
        "name": name, "type": "bone",
        "keyframes": pos_kfs + rot_kfs + sc_kfs,
    }

# Poles: retract upward (scale Y -> 0 from top origin)
for idx, (uuid_, name, cx0, cz0) in enumerate(pole_bones):
    stagger = idx * 0.10
    sc_kfs = [
        make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
        make_keyframe("scale", stagger + 0.05, 1.0, 1.0, 1.0),
        make_keyframe("scale", stagger + 0.30, 1.0, 0.4, 1.0),
        make_keyframe("scale", min(0.8, stagger + 0.50), 0.5, 0.01, 0.5),
        make_keyframe("scale", 0.8, 0.01, 0.01, 0.01),
    ]
    dissipate_animators[uuid_] = {
        "name": name, "type": "bone",
        "keyframes": sc_kfs,
    }

# Canopy: shatter outward and scale to 0
canopy_dis_kfs = [
    make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
    make_keyframe("scale", 0.20, 1.30, 0.95, 1.30),
    make_keyframe("scale", 0.45, 1.50, 0.50, 1.50),
    make_keyframe("scale", 0.8, 0.01, 0.01, 0.01),
    make_keyframe("rotation", 0.0, 0, 0, 0),
    make_keyframe("rotation", 0.4, 0, 80, 5),
    make_keyframe("rotation", 0.8, 0, 180, 15),
    make_keyframe("position", 0.0, 0, 0, 0),
    make_keyframe("position", 0.45, 0, 1.0, 0),
    make_keyframe("position", 0.8, 0, 3.0, 0),
]
dissipate_animators[canopy_bone["uuid"]] = {
    "name": canopy_bone["name"], "type": "bone",
    "keyframes": canopy_dis_kfs,
}

# Canopy decorations: blow outward
for idx, (uuid_, name, mid_a, dcx, dcz) in enumerate(deco_bones):
    out_x = math.cos(mid_a) * 2.0
    out_z = math.sin(mid_a) * 2.0
    pos_kfs = [
        make_keyframe("position", 0.0, 0, 0, 0),
        make_keyframe("position", 0.30, out_x * 0.5, 1.0, out_z * 0.5),
        make_keyframe("position", 0.8, out_x, 3.0, out_z),
    ]
    sc_kfs = [
        make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
        make_keyframe("scale", 0.40, 0.7, 0.7, 0.7),
        make_keyframe("scale", 0.8, 0.01, 0.01, 0.01),
    ]
    rot_kfs = [
        make_keyframe("rotation", 0.0, 0, math.degrees(mid_a), 0),
        make_keyframe("rotation", 0.4, 30, math.degrees(mid_a) + 60, 30),
        make_keyframe("rotation", 0.8, 90, math.degrees(mid_a) + 180, 90),
    ]
    dissipate_animators[uuid_] = {
        "name": name, "type": "bone",
        "keyframes": pos_kfs + sc_kfs + rot_kfs,
    }

# Ground ring: contract to 0
ground_dis_kfs = [
    make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
    make_keyframe("scale", 0.30, 0.85, 1.5, 0.85),
    make_keyframe("scale", 0.60, 0.40, 0.40, 0.40),
    make_keyframe("scale", 0.8, 0.01, 0.01, 0.01),
    make_keyframe("rotation", 0.0, 0, 22.5, 0),
    make_keyframe("rotation", 0.8, 0, -90, 0),
]
dissipate_animators[ground_bone["uuid"]] = {
    "name": ground_bone["name"], "type": "bone",
    "keyframes": ground_dis_kfs,
}

b.add_animation("dissipate", length=0.8, animators=dissipate_animators,
                loop="once", override=True)


# -----------------------------------------------------------------------------
# Textures + write
# -----------------------------------------------------------------------------
b.add_texture("carousel_horse_white", build_carousel_horse_white_texture())
b.add_texture("carnival_gold", build_carnival_gold_texture())

OUT = "D:/CC/ChaosCraft/src/main/resources/models/fluffy/me_attacks/carousel_horse_merry_go.bbmodel"
path = b.write(OUT)
size_kb = Path(path).stat().st_size // 1024
print(f"Wrote: {path}")
print(f"Size: {size_kb} KB")
print(f"Elements: {len(b.elements)}")
print(f"Animations: {[a['name'] for a in b.animations]}")
