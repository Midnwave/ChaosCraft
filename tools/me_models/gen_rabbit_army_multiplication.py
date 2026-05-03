#!/usr/bin/env python3
"""
Generator for rabbit_army_multiplication.bbmodel

Six stuffed rabbit plushies orbiting the caster at radius 7.
Each rabbit has body (2 cubes), head (2 cubes), 2 ears (2 cubes each),
2 inner ear slabs, and a tail. Orbit slowly like a ritual.

Plus a central summoning circle (4 flat slabs at Y=0) and an orbit path ring
(8 slabs at Y=4).

Animations:
  - spawn (0.8s..1.0s, override=true)
  - idle  (6.0s, override=false, loop)
  - dissipate (0.7s, override=true)
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
    add_noise_overlay,
    color_lerp,
)


# -----------------------------------------------------------------------------
# Texture 0: Stuffed rabbit white (cotton plushie)
# -----------------------------------------------------------------------------
def build_rabbit_white_texture():
    W, H = 64, 64
    base       = hex_to_rgba("#fefcfa")  # soft white
    shadow     = hex_to_rgba("#e0d8d0")  # barely-warm shadow
    fold       = hex_to_rgba("#c0b8b0")  # deep fold shadow
    spec       = hex_to_rgba("#ffffff")  # bright highlight
    emissive   = hex_to_rgba("#ffeef8")  # very faint lavender glow
    pixels = [base] * (W * H)

    # Horizontal grain every 4 rows (cotton fluff)
    for y in range(0, H, 4):
        for x in range(W):
            r, g, b, a = pixels[y * W + x]
            pixels[y * W + x] = (max(0, r - 6), max(0, g - 6), max(0, b - 6), a)

    # Vertical fold shadow lines (suggests stitched seams)
    for x in range(W):
        if x % 11 == 5:
            for y in range(8, 56):
                pixels[y * W + x] = shadow
        if x % 17 == 8:
            for y in range(12, 52):
                pixels[y * W + x] = fold

    # Mid horizontal seam (ear-to-body fold)
    fill_rect(pixels, W, H, 0, 30, W, 32, fold)
    fill_rect(pixels, W, H, 0, 31, W, 32, shadow)

    # Specular highlights (small clusters scattered)
    for (cx, cy) in [(8, 6), (22, 4), (40, 10), (54, 6),
                     (12, 22), (32, 18), (48, 26),
                     (10, 40), (28, 44), (46, 38), (58, 50)]:
        fill_rect(pixels, W, H, cx, cy, cx + 2, cy + 2, spec)

    # Very faint emissive lavender glow patches
    for (cx, cy) in [(16, 14), (44, 16), (24, 36), (50, 44), (6, 50)]:
        fill_rect(pixels, W, H, cx, cy, cx + 3, cy + 2, emissive)

    # Sparse soft noise
    add_noise_overlay(pixels, W, H, shadow, spec, density=0.03, seed=137)

    return png_from_pixels(pixels, W, H)


# -----------------------------------------------------------------------------
# Texture 1: Inner ear pink (top half) + summoning circle lavender (bottom half)
# -----------------------------------------------------------------------------
def build_pink_lavender_texture():
    W, H = 64, 64
    pink_base    = hex_to_rgba("#ffb8cc")  # baby pink (top half)
    pink_pale    = hex_to_rgba("#ffd8e4")  # pale pink highlight
    pink_deep    = hex_to_rgba("#e895a8")  # deeper pink shadow
    lav_base     = hex_to_rgba("#d8b8ff")  # lavender (bottom half)
    lav_pale     = hex_to_rgba("#ece0ff")  # pale lavender highlight
    lav_deep     = hex_to_rgba("#a888d8")  # deeper lavender
    rune_glow    = hex_to_rgba("#ffffff")  # rune highlights

    pixels = [pink_base] * (W * H)

    # Top half (rows 0..32): pink inner ear zone
    fill_rect(pixels, W, H, 0, 0, W, 32, pink_base)
    gradient_horizontal_band(pixels, W, H, 0, 16, pink_pale, pink_base)
    gradient_horizontal_band(pixels, W, H, 16, 32, pink_base, pink_deep)
    # Pink seam highlights
    for x in range(8, 56):
        pixels[10 * W + x] = pink_pale
        pixels[24 * W + x] = pink_pale
    # Pink center vein
    for y in range(4, 30):
        pixels[y * W + 31] = pink_pale
        pixels[y * W + 32] = pink_pale

    # Bottom half (rows 32..64): lavender summoning circle zone
    fill_rect(pixels, W, H, 0, 32, W, 64, lav_base)
    gradient_horizontal_band(pixels, W, H, 32, 48, lav_pale, lav_base)
    gradient_horizontal_band(pixels, W, H, 48, 64, lav_base, lav_deep)

    # Etch a magic-circle pattern in bottom half
    cx, cy = 32, 48
    # Outer ring
    for ang_step in range(0, 360, 3):
        a = math.radians(ang_step)
        for r in range(13, 15):
            x = int(cx + math.cos(a) * r)
            y = int(cy + math.sin(a) * r)
            if 0 <= x < W and 32 <= y < 64:
                pixels[y * W + x] = lav_pale
    # Inner ring
    for ang_step in range(0, 360, 4):
        a = math.radians(ang_step)
        for r in range(7, 8):
            x = int(cx + math.cos(a) * r)
            y = int(cy + math.sin(a) * r)
            if 0 <= x < W and 32 <= y < 64:
                pixels[y * W + x] = rune_glow
    # 6 rune nodes around outer ring (1 per rabbit)
    for i in range(6):
        a = math.radians(i * 60)
        rx = int(cx + math.cos(a) * 12)
        ry = int(cy + math.sin(a) * 12)
        for dy in range(-1, 2):
            for dx in range(-1, 2):
                xx, yy = rx + dx, ry + dy
                if 0 <= xx < W and 32 <= yy < 64:
                    pixels[yy * W + xx] = rune_glow
    # Cross lines through center
    for r in range(-13, 14):
        x1 = cx + r
        if 0 <= x1 < W:
            pixels[cy * W + x1] = lav_pale
        y1 = cy + r
        if 32 <= y1 < 64:
            pixels[y1 * W + cx] = lav_pale

    # Domestic palette mottling
    add_noise_overlay(pixels, W, H, pink_deep, pink_pale, density=0.04, seed=21)
    add_noise_overlay(pixels, W, H, lav_deep, lav_pale, density=0.04, seed=58)

    return png_from_pixels(pixels, W, H)


# -----------------------------------------------------------------------------
# Builder
# -----------------------------------------------------------------------------
b = Builder("rabbit_army_multiplication", resolution=(64, 64), visible_box=(8, 4, 0))


# UV regions on rabbit white texture (texture 0)
# we'll just reuse small boxes — basic_cube_faces handles the layout
def rabbit_face(u, v, sx, sy, sz):
    return basic_cube_faces(u, v, sx, sy, sz, tex_index=0)


def pink_face(u, v, sx, sy, sz):
    return basic_cube_faces(u, v, sx, sy, sz, tex_index=1)


# -----------------------------------------------------------------------------
# Build a single rabbit at local origin (0, 0, 0).
# Returns (head_bone, left_ear_bone, right_ear_bone, tail_bone, all_root_bone)
# Local frame: X = right, Y = up, Z = forward (direction of orbit travel)
# -----------------------------------------------------------------------------
def make_rabbit(prefix):
    # ---- BODY: 2 overlapping cubes for round oval shape ----
    # Lower wider body
    body_lower = b.add_cube(
        f"{prefix}_body_lower",
        from_=[-1.4, 0.0, -1.1],
        to_=[1.4, 1.6, 1.1],
        faces=rabbit_face(0, 12, 3, 2, 2),
    )
    # Upper narrower body (overlapping — gives round oval)
    body_upper = b.add_cube(
        f"{prefix}_body_upper",
        from_=[-1.2, 1.4, -0.9],
        to_=[1.2, 2.6, 0.9],
        faces=rabbit_face(14, 12, 2, 1, 2),
    )

    # ---- HEAD: 2 overlapping smaller cubes ----
    head_main = b.add_cube(
        f"{prefix}_head_main",
        from_=[-0.9, 2.5, -0.85],
        to_=[0.9, 3.9, 0.85],
        faces=rabbit_face(0, 28, 2, 2, 2),
    )
    head_snout = b.add_cube(
        f"{prefix}_head_snout",
        from_=[-0.55, 2.7, 0.7],
        to_=[0.55, 3.4, 1.4],
        faces=rabbit_face(14, 28, 1, 1, 1),
    )

    # ---- LEFT EAR (2 stacked tall thin cubes) ----
    # ear stands at top of head, slightly outward
    le_lower = b.add_cube(
        f"{prefix}_ear_L_lower",
        from_=[-0.9, 3.7, -0.4],
        to_=[-0.3, 5.4, 0.05],
        faces=rabbit_face(20, 28, 1, 2, 1),
    )
    le_upper = b.add_cube(
        f"{prefix}_ear_L_upper",
        from_=[-0.85, 5.3, -0.4],
        to_=[-0.35, 6.7, 0.0],
        faces=rabbit_face(26, 28, 1, 2, 1),
    )

    # ---- RIGHT EAR (mirror) ----
    re_lower = b.add_cube(
        f"{prefix}_ear_R_lower",
        from_=[0.3, 3.7, -0.4],
        to_=[0.9, 5.4, 0.05],
        faces=rabbit_face(20, 28, 1, 2, 1),
    )
    re_upper = b.add_cube(
        f"{prefix}_ear_R_upper",
        from_=[0.35, 5.3, -0.4],
        to_=[0.85, 6.7, 0.0],
        faces=rabbit_face(26, 28, 1, 2, 1),
    )

    # ---- INNER EARS (thin pink slabs inside ears) ----
    le_inner = b.add_cube(
        f"{prefix}_ear_L_inner",
        from_=[-0.78, 4.0, -0.32],
        to_=[-0.42, 6.4, -0.27],
        faces=pink_face(0, 0, 1, 3, 1),
    )
    re_inner = b.add_cube(
        f"{prefix}_ear_R_inner",
        from_=[0.42, 4.0, -0.32],
        to_=[0.78, 6.4, -0.27],
        faces=pink_face(0, 0, 1, 3, 1),
    )

    # ---- TAIL (round small cube at back of body) ----
    tail = b.add_cube(
        f"{prefix}_tail",
        from_=[-0.45, 0.9, -1.5],
        to_=[0.45, 1.7, -0.95],
        faces=rabbit_face(36, 12, 1, 1, 1),
    )

    # ---- BONES ----
    # Left ear bone (origin at base of ear, inner slab parented under it so it rotates with ear)
    left_ear_bone = b.make_group(
        f"{prefix}_ear_L",
        origin=[-0.6, 3.7, -0.18],
        rotation=[0, 0, 6.0],  # slight outward tilt
        children=[le_lower, le_upper, le_inner],
    )
    right_ear_bone = b.make_group(
        f"{prefix}_ear_R",
        origin=[0.6, 3.7, -0.18],
        rotation=[0, 0, -6.0],
        children=[re_lower, re_upper, re_inner],
    )

    # Head bone (origin at base of head, contains head cubes + ear bones)
    head_bone = b.make_group(
        f"{prefix}_head",
        origin=[0.0, 2.5, 0.0],
        children=[head_main, head_snout, left_ear_bone, right_ear_bone],
    )

    # Tail bone (small wiggle origin)
    tail_bone = b.make_group(
        f"{prefix}_tail_bone",
        origin=[0.0, 1.3, -1.2],
        children=[tail],
    )

    # Rabbit root bone (assembled body + head + tail)
    rabbit_root = b.make_group(
        f"{prefix}_root",
        origin=[0.0, 0.0, 0.0],
        children=[body_lower, body_upper, head_bone, tail_bone],
    )

    return {
        "root": rabbit_root,
        "head": head_bone,
        "ear_L": left_ear_bone,
        "ear_R": right_ear_bone,
        "tail": tail_bone,
    }


# -----------------------------------------------------------------------------
# Build 6 rabbits, parented under orbit bones around the central axis.
# -----------------------------------------------------------------------------
NUM_RABBITS = 6
ORBIT_RADIUS = 7.0

orbit_bones = []  # list of (orbit_bone_dict, theta_initial)
rabbit_dicts = []  # list of make_rabbit() return dicts

for i in range(NUM_RABBITS):
    theta_deg = i * 60.0  # initial angle in degrees
    theta_rad = math.radians(theta_deg)

    rabbit = make_rabbit(f"rabbit{i}")
    rabbit_dicts.append(rabbit)

    # Each rabbit is placed at +Z axis inside its orbit bone (which itself rotates)
    # Inner placement bone: shifts rabbit out radially by ORBIT_RADIUS along Z
    placement = b.make_group(
        f"orbit{i}_placement",
        origin=[0, 0, 0],
        children=[rabbit["root"]],
    )
    # Manually offset rabbit_root's origin to put it at radius
    rabbit["root"]["origin"] = [0.0, 0.0, ORBIT_RADIUS]
    # Also shift each cube? No — we use bone origin; bone origin is the rotation pivot
    # Actually for ME, position offset needs to come from the bone's position keyframe,
    # OR we baked the offset into the cube coords. Easiest: bake offset into the orbit bone's
    # children by shifting rabbit_root origin.
    # Simpler: place rabbit cubes at (X=0, Y=0, Z=ORBIT_RADIUS) by adjusting rabbit_root origin
    # and adding a position keyframe later. We'll instead bake the radial offset directly into
    # each cube. Re-doing: orbit bone rotates Y axis at origin, contains rabbit at +Z=7 offset.

    # Simpler approach: orbit_bone has origin (0,0,0), Y rotation; rabbit_root origin set
    # to (0, 0, 7) so it sits at radius. The model engine will rotate the rabbit_root group
    # around (0,0,0), placing it on a circle.
    # However bbmodel group origins are local pivots, not positions. Children cubes need
    # absolute coords. So we rebuild using cube absolute coordinates.

    # Discard the placement bone — we'll handle differently below.
    orbit_bones.append((None, theta_deg, rabbit, theta_rad))


# We need to redo: the cubes were created at local origin (0,0,0).
# To put them on a circle of radius 7 we have two options:
#   (a) rebuild cubes with absolute x/z offsets — would require re-creating cubes per rabbit
#   (b) wrap rabbit_root in a parent bone with origin (0,0,0) and a position offset
#       baked into the rabbit_root's cubes... cubes are absolute in bbmodel.
#
# Option (a) is cleaner — let's redo: throw away current builder elements and start fresh,
# this time building rabbits with offset cubes.
# -----------------------------------------------------------------------------

# Reset builder state (clear everything we built so far)
b.elements = []
b.outliner = []
b._element_uuids = []
orbit_bones = []
rabbit_dicts = []


def make_rabbit_at(prefix, x_off, y_off, z_off, face_dir_deg=0):
    """Build a rabbit with absolute cube positions offset by (x_off, y_off, z_off).
    face_dir_deg: rotation of head bone around Y axis to face direction of travel.
    Returns dict of bone groups: root (positioned at offset), head, ear_L, ear_R, tail.
    All coordinates baked into cubes — bone origins also offset.
    """

    # Body lower
    body_lower = b.add_cube(
        f"{prefix}_body_lower",
        from_=[x_off - 1.4, y_off + 0.0, z_off - 1.1],
        to_=[x_off + 1.4, y_off + 1.6, z_off + 1.1],
        faces=rabbit_face(0, 12, 3, 2, 2),
    )
    body_upper = b.add_cube(
        f"{prefix}_body_upper",
        from_=[x_off - 1.2, y_off + 1.4, z_off - 0.9],
        to_=[x_off + 1.2, y_off + 2.6, z_off + 0.9],
        faces=rabbit_face(14, 12, 2, 1, 2),
    )
    # Head main
    head_main = b.add_cube(
        f"{prefix}_head_main",
        from_=[x_off - 0.9, y_off + 2.5, z_off - 0.85],
        to_=[x_off + 0.9, y_off + 3.9, z_off + 0.85],
        faces=rabbit_face(0, 28, 2, 2, 2),
    )
    head_snout = b.add_cube(
        f"{prefix}_head_snout",
        from_=[x_off - 0.55, y_off + 2.7, z_off + 0.7],
        to_=[x_off + 0.55, y_off + 3.4, z_off + 1.4],
        faces=rabbit_face(14, 28, 1, 1, 1),
    )
    # Left ear
    le_lower = b.add_cube(
        f"{prefix}_ear_L_lower",
        from_=[x_off - 0.9, y_off + 3.7, z_off - 0.4],
        to_=[x_off - 0.3, y_off + 5.4, z_off + 0.05],
        faces=rabbit_face(20, 28, 1, 2, 1),
    )
    le_upper = b.add_cube(
        f"{prefix}_ear_L_upper",
        from_=[x_off - 0.85, y_off + 5.3, z_off - 0.4],
        to_=[x_off - 0.35, y_off + 6.7, z_off + 0.0],
        faces=rabbit_face(26, 28, 1, 2, 1),
    )
    # Right ear
    re_lower = b.add_cube(
        f"{prefix}_ear_R_lower",
        from_=[x_off + 0.3, y_off + 3.7, z_off - 0.4],
        to_=[x_off + 0.9, y_off + 5.4, z_off + 0.05],
        faces=rabbit_face(20, 28, 1, 2, 1),
    )
    re_upper = b.add_cube(
        f"{prefix}_ear_R_upper",
        from_=[x_off + 0.35, y_off + 5.3, z_off - 0.4],
        to_=[x_off + 0.85, y_off + 6.7, z_off + 0.0],
        faces=rabbit_face(26, 28, 1, 2, 1),
    )
    # Inner ears (pink)
    le_inner = b.add_cube(
        f"{prefix}_ear_L_inner",
        from_=[x_off - 0.78, y_off + 4.0, z_off - 0.32],
        to_=[x_off - 0.42, y_off + 6.4, z_off - 0.27],
        faces=pink_face(0, 0, 1, 3, 1),
    )
    re_inner = b.add_cube(
        f"{prefix}_ear_R_inner",
        from_=[x_off + 0.42, y_off + 4.0, z_off - 0.32],
        to_=[x_off + 0.78, y_off + 6.4, z_off - 0.27],
        faces=pink_face(0, 0, 1, 3, 1),
    )
    # Tail
    tail = b.add_cube(
        f"{prefix}_tail",
        from_=[x_off - 0.45, y_off + 0.9, z_off - 1.5],
        to_=[x_off + 0.45, y_off + 1.7, z_off - 0.95],
        faces=rabbit_face(36, 12, 1, 1, 1),
    )

    # Bones (origins are world-relative pivots in bbmodel)
    left_ear_bone = b.make_group(
        f"{prefix}_ear_L",
        origin=[x_off - 0.6, y_off + 3.7, z_off - 0.18],
        rotation=[0, 0, 6.0],
        children=[le_lower, le_upper, le_inner],
    )
    right_ear_bone = b.make_group(
        f"{prefix}_ear_R",
        origin=[x_off + 0.6, y_off + 3.7, z_off - 0.18],
        rotation=[0, 0, -6.0],
        children=[re_lower, re_upper, re_inner],
    )
    head_bone = b.make_group(
        f"{prefix}_head",
        origin=[x_off + 0.0, y_off + 2.5, z_off + 0.0],
        rotation=[0, face_dir_deg, 0],
        children=[head_main, head_snout, left_ear_bone, right_ear_bone],
    )
    tail_bone = b.make_group(
        f"{prefix}_tail_bone",
        origin=[x_off + 0.0, y_off + 1.3, z_off - 1.2],
        children=[tail],
    )
    rabbit_root = b.make_group(
        f"{prefix}_root",
        origin=[x_off, y_off, z_off],
        children=[body_lower, body_upper, head_bone, tail_bone],
    )
    return {
        "root": rabbit_root,
        "head": head_bone,
        "ear_L": left_ear_bone,
        "ear_R": right_ear_bone,
        "tail": tail_bone,
    }


# Build 6 rabbits at evenly spaced positions on the orbit
# Plus their orbit-parent bones (each rabbit has its own orbit bone whose Y-rotation
# spins it around the center)
orbit_parent_bones = []  # list of dicts: {"orbit": orbit_bone, "rabbit": rabbit_dict, "theta0": deg}

for i in range(NUM_RABBITS):
    theta0_deg = i * 60.0
    theta0_rad = math.radians(theta0_deg)
    # Initial rabbit position on circle
    rx = math.sin(theta0_rad) * ORBIT_RADIUS
    rz = math.cos(theta0_rad) * ORBIT_RADIUS
    # Build rabbit cubes at (rx, 0, rz). Initial head_dir faces tangent direction
    # For circular orbit, tangent at angle theta points in (cos theta, 0, -sin theta).
    # We use a parent orbit bone whose Y rotation is (-theta0_deg) to spin: but easier to
    # bake the position into the cubes (which we did) and animate the orbit bone with
    # an additional Y rotation (rotating around (0,0,0)).
    # Initial face_dir = perpendicular to radial = orbit tangent direction in degrees
    # tangent angle = theta0_deg + 90
    face_dir = theta0_deg + 90

    rabbit = make_rabbit_at(f"rabbit{i}", x_off=rx, y_off=4.0, z_off=rz,
                            face_dir_deg=face_dir)
    rabbit_dicts.append(rabbit)

    # Wrap rabbit_root in an orbit_parent bone whose pivot is (0, 4, 0) center
    # Y-rotation of this bone spins the rabbit around the origin
    orbit_parent = b.make_group(
        f"orbit{i}_parent",
        origin=[0, 4.0, 0],
        rotation=[0, 0, 0],
        children=[rabbit["root"]],
    )
    orbit_parent_bones.append({
        "orbit": orbit_parent,
        "rabbit": rabbit,
        "theta0_deg": theta0_deg,
        "rx": rx, "rz": rz,
    })


# -----------------------------------------------------------------------------
# Central summoning circle: 4 flat slabs at Y=0
# -----------------------------------------------------------------------------
sc_cubes = []
for i in range(4):
    a = math.radians(i * 90)
    cx = math.cos(a) * 1.6
    cz = math.sin(a) * 1.6
    cube = b.add_cube(
        f"summon_circle_slab_{i}",
        from_=[cx - 1.6, -0.1, cz - 0.5],
        to_=[cx + 1.6, 0.1, cz + 0.5],
        faces=basic_cube_faces(0, 32, 3, 1, 1, tex_index=1),
        rotation=[0, math.degrees(a), 0],
        origin=[cx, 0, cz],
    )
    sc_cubes.append(cube)

summoning_circle_bone = b.make_group(
    "summoning_circle",
    origin=[0, 0, 0],
    children=sc_cubes,
)


# -----------------------------------------------------------------------------
# Orbit path ring: 8 flat slabs at Y=4
# -----------------------------------------------------------------------------
ring_cubes = []
for i in range(8):
    a = math.radians(i * 45)
    cx = math.cos(a) * ORBIT_RADIUS
    cz = math.sin(a) * ORBIT_RADIUS
    cube = b.add_cube(
        f"orbit_ring_slab_{i}",
        from_=[cx - 1.4, 3.95, cz - 0.5],
        to_=[cx + 1.4, 4.05, cz + 0.5],
        faces=basic_cube_faces(0, 56, 3, 1, 1, tex_index=1),
        rotation=[0, math.degrees(a) + 90, 0],
        origin=[cx, 4.0, cz],
    )
    ring_cubes.append(cube)

orbit_ring_bone = b.make_group(
    "orbit_ring",
    origin=[0, 4.0, 0],
    children=ring_cubes,
)


# -----------------------------------------------------------------------------
# Master root bone wraps everything
# -----------------------------------------------------------------------------
master_children = [summoning_circle_bone, orbit_ring_bone]
for op in orbit_parent_bones:
    master_children.append(op["orbit"])

master = b.make_group(
    "root",
    origin=[0, 0, 0],
    children=master_children,
)
b.outliner.append(master)


# -----------------------------------------------------------------------------
# ANIMATIONS
# -----------------------------------------------------------------------------

# ===== SPAWN (1.0s, override=true) =====
# - 0.0s summoning circle expands (scale 0.05 -> 1.0 by 0.2s)
# - 0.2s orbit path ring appears (scale 0.05 -> 1.0 by 0.4s)
# - 0.3-0.6s rabbits drop from above (Y=14 -> Y=0 at orbit position) staggered
#   each landing with a settle bounce
# - 0.6-1.0s gentle settle, ears wiggle from impact

spawn_animators = {}

# Summoning circle: scale up
spawn_animators[summoning_circle_bone["uuid"]] = {
    "name": summoning_circle_bone["name"], "type": "bone",
    "keyframes": [
        make_keyframe("scale", 0.0,  0.05, 0.05, 0.05),
        make_keyframe("scale", 0.10, 0.5,  0.05, 0.5),
        make_keyframe("scale", 0.20, 1.15, 0.05, 1.15),
        make_keyframe("scale", 0.25, 1.0,  1.0,  1.0),
        make_keyframe("scale", 1.0,  1.0,  1.0,  1.0),
    ],
}

# Orbit ring: appears at 0.2s, expands
spawn_animators[orbit_ring_bone["uuid"]] = {
    "name": orbit_ring_bone["name"], "type": "bone",
    "keyframes": [
        make_keyframe("scale", 0.0,  0.05, 0.05, 0.05),
        make_keyframe("scale", 0.20, 0.05, 0.05, 0.05),
        make_keyframe("scale", 0.30, 0.6,  1.0,  0.6),
        make_keyframe("scale", 0.40, 1.1,  1.0,  1.1),
        make_keyframe("scale", 0.50, 1.0,  1.0,  1.0),
        make_keyframe("scale", 1.0,  1.0,  1.0,  1.0),
    ],
}

# Each rabbit drops from above (staggered 0.3 -> 0.6s)
for idx, op in enumerate(orbit_parent_bones):
    rabbit = op["rabbit"]
    # Drop time staggered between 0.3 and 0.6 across 6 rabbits
    drop_start = 0.30 + (idx * 0.05)  # 0.30, 0.35, 0.40, 0.45, 0.50, 0.55
    drop_end = drop_start + 0.18
    settle_end = drop_end + 0.10
    # Rabbit root Y position keyframes (relative — starts +14, ends 0)
    spawn_animators[rabbit["root"]["uuid"]] = {
        "name": rabbit["root"]["name"], "type": "bone",
        "keyframes": [
            make_keyframe("position", 0.0, 0, 14.0, 0),
            make_keyframe("position", drop_start, 0, 14.0, 0),
            make_keyframe("position", drop_start + 0.06, 0, 8.0, 0),
            make_keyframe("position", drop_end, 0, -0.4, 0),  # slight squash bounce
            make_keyframe("position", drop_end + 0.05, 0, 0.3, 0),  # bounce up
            make_keyframe("position", settle_end, 0, 0.0, 0),
            make_keyframe("position", 1.0, 0, 0.0, 0),
        ],
    }
    # Squash on landing — body scale Y down briefly
    spawn_animators_scale_kfs = [
        make_keyframe("scale", 0.0, 1.0, 1.0, 1.0),
        make_keyframe("scale", drop_end - 0.02, 1.0, 1.0, 1.0),
        make_keyframe("scale", drop_end + 0.02, 1.15, 0.78, 1.15),
        make_keyframe("scale", drop_end + 0.10, 0.95, 1.06, 0.95),
        make_keyframe("scale", settle_end, 1.0, 1.0, 1.0),
        make_keyframe("scale", 1.0, 1.0, 1.0, 1.0),
    ]
    # Apply squash to the orbit_parent bone (or rabbit root). Use root.
    if rabbit["root"]["uuid"] in spawn_animators:
        spawn_animators[rabbit["root"]["uuid"]]["keyframes"].extend(spawn_animators_scale_kfs)

    # Ears wiggle on landing (left/right ear rotations)
    base_zL = 6.0
    base_zR = -6.0
    ear_L_kfs = [
        make_keyframe("rotation", 0.0,  0, 0, base_zL),
        make_keyframe("rotation", drop_end, 0, 0, base_zL),
        make_keyframe("rotation", drop_end + 0.06, 30, 0, base_zL + 8),
        make_keyframe("rotation", drop_end + 0.14, -10, 0, base_zL - 4),
        make_keyframe("rotation", settle_end, 0, 0, base_zL),
        make_keyframe("rotation", 1.0, 0, 0, base_zL),
    ]
    ear_R_kfs = [
        make_keyframe("rotation", 0.0,  0, 0, base_zR),
        make_keyframe("rotation", drop_end, 0, 0, base_zR),
        make_keyframe("rotation", drop_end + 0.06, 25, 0, base_zR - 8),
        make_keyframe("rotation", drop_end + 0.14, -8, 0, base_zR + 4),
        make_keyframe("rotation", settle_end, 0, 0, base_zR),
        make_keyframe("rotation", 1.0, 0, 0, base_zR),
    ]
    spawn_animators[rabbit["ear_L"]["uuid"]] = {
        "name": rabbit["ear_L"]["name"], "type": "bone", "keyframes": ear_L_kfs,
    }
    spawn_animators[rabbit["ear_R"]["uuid"]] = {
        "name": rabbit["ear_R"]["name"], "type": "bone", "keyframes": ear_R_kfs,
    }

b.add_animation("spawn", length=1.0, animators=spawn_animators, loop="once", override=True)


# ===== IDLE (6.0s, loop, override=false) =====
# - all 6 orbit bones rotate Y from 0 -> 360 in 6s (one full orbit per loop)
# - each rabbit head rotates to track tangent direction (already baked initial; here we
#   add subtle continuous turn; but since the orbit_parent rotates, rabbits already turn
#   visually... however since tangent angle changes, we'd need head bone to follow.
#   Actually if the orbit bone rotates the WHOLE rabbit including head, head stays
#   pointing at same world-relative direction (forward to tangent at start). This keeps
#   head pointing forward in tangent direction throughout — exactly what we want).
#   So head stays aligned automatically!
# - But to add awareness — we want subtle micro head adjustments mid-orbit.
# - Ears bob up/down (rotation X cycles)
# - Tails wiggle (rotation Y cycles)
# - Summoning circle rotates OPPOSITE direction on Y

idle_animators = {}

# Each orbit_parent rotates Y by full circle over 6s
for idx, op in enumerate(orbit_parent_bones):
    orbit_bone = op["orbit"]
    # Rotate +360 degrees over 6s
    kfs = [
        make_keyframe("rotation", 0.0, 0, 0, 0),
        make_keyframe("rotation", 1.0, 0, 60, 0),
        make_keyframe("rotation", 2.0, 0, 120, 0),
        make_keyframe("rotation", 3.0, 0, 180, 0),
        make_keyframe("rotation", 4.0, 0, 240, 0),
        make_keyframe("rotation", 5.0, 0, 300, 0),
        make_keyframe("rotation", 6.0, 0, 360, 0),
    ]
    idle_animators[orbit_bone["uuid"]] = {
        "name": orbit_bone["name"], "type": "bone", "keyframes": kfs,
    }

# Summoning circle rotates OPPOSITE (negative Y, full circle in 6s)
sc_kfs = [
    make_keyframe("rotation", 0.0, 0, 0, 0),
    make_keyframe("rotation", 1.0, 0, -60, 0),
    make_keyframe("rotation", 2.0, 0, -120, 0),
    make_keyframe("rotation", 3.0, 0, -180, 0),
    make_keyframe("rotation", 4.0, 0, -240, 0),
    make_keyframe("rotation", 5.0, 0, -300, 0),
    make_keyframe("rotation", 6.0, 0, -360, 0),
]
idle_animators[summoning_circle_bone["uuid"]] = {
    "name": summoning_circle_bone["name"], "type": "bone", "keyframes": sc_kfs,
}

# Subtle pulse on orbit ring
ring_kfs = []
for s in range(25):
    t = (s / 24.0) * 6.0
    val = 1.0 + math.sin((t / 1.5) * math.tau) * 0.04
    ring_kfs.append(make_keyframe("scale", t, val, 1.0, val))
idle_animators[orbit_ring_bone["uuid"]] = {
    "name": orbit_ring_bone["name"], "type": "bone", "keyframes": ring_kfs,
}
# Orbit ring also has a subtle Y-rotation cycle
ring_rot_kfs = []
for s in range(25):
    t = (s / 24.0) * 6.0
    val = math.sin((t / 3.0) * math.tau) * 4.0
    ring_rot_kfs.append(make_keyframe("rotation", t, 0, val, 0))
idle_animators[orbit_ring_bone["uuid"]]["keyframes"].extend(ring_rot_kfs)

# Each rabbit's ears bob (rotation X cycle), independent phases
for idx, op in enumerate(orbit_parent_bones):
    rabbit = op["rabbit"]
    base_zL = 6.0
    base_zR = -6.0
    # Left ear bob — period varies
    period_L = 1.6 + (idx * 0.13)
    phase_L = (idx * 0.27) % 1.0
    kfs_L = []
    samples = 15
    for s in range(samples + 1):
        t = (s / samples) * 6.0
        val = math.sin(((t / period_L) + phase_L) * math.tau) * 6.0
        kfs_L.append(make_keyframe("rotation", t, val, 0, base_zL))
    idle_animators[rabbit["ear_L"]["uuid"]] = {
        "name": rabbit["ear_L"]["name"], "type": "bone", "keyframes": kfs_L,
    }
    # Right ear bob — opposite phase
    period_R = 1.7 + (idx * 0.11)
    phase_R = (idx * 0.31 + 0.5) % 1.0
    kfs_R = []
    samples_R = 15
    for s in range(samples_R + 1):
        t = (s / samples_R) * 6.0
        val = math.sin(((t / period_R) + phase_R) * math.tau) * 6.0
        kfs_R.append(make_keyframe("rotation", t, val, 0, base_zR))
    idle_animators[rabbit["ear_R"]["uuid"]] = {
        "name": rabbit["ear_R"]["name"], "type": "bone", "keyframes": kfs_R,
    }
    # Tail wiggle — rotation Y cycle
    period_T = 0.9 + (idx * 0.07)
    phase_T = (idx * 0.41) % 1.0
    kfs_T = []
    for s in range(13):
        t = (s / 12.0) * 6.0
        val = math.sin(((t / period_T) + phase_T) * math.tau) * 12.0
        kfs_T.append(make_keyframe("rotation", t, 0, val, 0))
    idle_animators[rabbit["tail"]["uuid"]] = {
        "name": rabbit["tail"]["name"], "type": "bone", "keyframes": kfs_T,
    }
    # Subtle head micro-tracking — slight Y rotation mid-orbit (suggests head turning to face forward)
    # Since the orbit bone already turns the whole rabbit, an additional small head Y rotation
    # adds awareness. Use small ±5° gentle sin
    period_H = 3.0 + (idx * 0.25)
    phase_H = (idx * 0.19) % 1.0
    kfs_H = []
    for s in range(13):
        t = (s / 12.0) * 6.0
        val = math.sin(((t / period_H) + phase_H) * math.tau) * 5.0
        # Initial baked face_dir was theta0_deg + 90; preserve as base
        base_y = op["theta0_deg"] + 90
        kfs_H.append(make_keyframe("rotation", t, 0, base_y + val, 0))
    idle_animators[rabbit["head"]["uuid"]] = {
        "name": rabbit["head"]["name"], "type": "bone", "keyframes": kfs_H,
    }
    # Subtle rabbit body breathing — tiny Y bob
    period_B = 2.4 + (idx * 0.18)
    phase_B = (idx * 0.33) % 1.0
    kfs_B = []
    for s in range(13):
        t = (s / 12.0) * 6.0
        val = math.sin(((t / period_B) + phase_B) * math.tau) * 0.18
        kfs_B.append(make_keyframe("position", t, 0, val, 0))
    idle_animators[rabbit["root"]["uuid"]] = {
        "name": rabbit["root"]["name"], "type": "bone", "keyframes": kfs_B,
    }

b.add_animation("idle", length=6.0, animators=idle_animators, loop="loop", override=False)


# ===== DISSIPATE (0.7s, override=true) =====
# - 0.0-0.15s: all rabbits stop orbiting (orbit bone rotation freezes at last value — implicit)
#   AND all 6 head bones snap inward to look at center (target).
# - 0.15-0.30s: pause — rabbits frozen, looking at target. Suspense.
# - 0.30-0.55s: rabbits all blast outward radially (rabbit_root position scales out by +6)
# - 0.30-0.55s: summoning circle expands (scale 1 -> 2.5) then snap to 0
# - 0.30-0.55s: orbit ring rapidly expands
# - 0.55-0.7s: everything fades (scale to 0)

dissipate_animators = {}

# Heads: snap inward to face center (target). Each rabbit's head should rotate to face the
# direction of the world center. Since head was at orbit angle theta + 90 (tangent),
# new angle to face center is theta + 180 (opposite of radial outward).
# Snap from current to that within 0.10s.
for idx, op in enumerate(orbit_parent_bones):
    rabbit = op["rabbit"]
    base_y = op["theta0_deg"] + 90  # tangent
    target_y = op["theta0_deg"] + 180  # face center
    kfs_H = [
        make_keyframe("rotation", 0.0,  0, base_y,   0),
        make_keyframe("rotation", 0.05, 0, base_y,   0),
        make_keyframe("rotation", 0.12, 0, target_y, 0),  # snap-to-look
        make_keyframe("rotation", 0.30, 0, target_y, 0),  # hold
        make_keyframe("rotation", 0.7,  0, target_y, 0),
    ]
    dissipate_animators[rabbit["head"]["uuid"]] = {
        "name": rabbit["head"]["name"], "type": "bone", "keyframes": kfs_H,
    }

# Rabbits blast outward radially — they're at (rx, 4, rz) so we move them further
# Use rabbit root position to push outward in local Z direction (the radial direction
# baked into placement). The orbit_parent rotates, so "outward" in local frame
# of rabbit_root is +Z direction (since cubes are at rx, rz baked).
# Easier: animate orbit_parent's scale > 1 to push rabbits outward radially.
# But scale on orbit_parent would scale rabbits too. Use rabbit_root position offset
# along its baked radial direction.
# rabbit_root cubes are at (rx, 0, rz) world-baked. To blast outward, move rabbit root
# by (rx_dir, 0, rz_dir) * factor. rx_dir = sin(theta), rz_dir = cos(theta).
for idx, op in enumerate(orbit_parent_bones):
    rabbit = op["rabbit"]
    theta_rad = math.radians(op["theta0_deg"])
    rx_dir = math.sin(theta_rad)
    rz_dir = math.cos(theta_rad)
    blast_dist = 12.0
    kfs_R = [
        make_keyframe("position", 0.0,  0, 0, 0),
        make_keyframe("position", 0.30, 0, 0, 0),  # frozen during snap+pause
        make_keyframe("position", 0.42, rx_dir * 4.0, 1.0, rz_dir * 4.0),
        make_keyframe("position", 0.55, rx_dir * blast_dist, 0.5, rz_dir * blast_dist),
        make_keyframe("position", 0.7,  rx_dir * blast_dist * 1.4, -2.0, rz_dir * blast_dist * 1.4),
    ]
    dissipate_animators[rabbit["root"]["uuid"]] = {
        "name": rabbit["root"]["name"], "type": "bone", "keyframes": kfs_R,
    }
    # Scale to 0 at end (poof)
    sc_kfs = [
        make_keyframe("scale", 0.0,  1.0, 1.0, 1.0),
        make_keyframe("scale", 0.30, 1.0, 1.0, 1.0),
        make_keyframe("scale", 0.45, 1.3, 1.3, 1.3),
        make_keyframe("scale", 0.55, 1.0, 1.0, 1.0),
        make_keyframe("scale", 0.7,  0.05, 0.05, 0.05),
    ]
    # Append scale keyframes onto the same animator (rabbit_root)
    dissipate_animators[rabbit["root"]["uuid"]]["keyframes"].extend(sc_kfs)

# Ears flatten/snap during dissipate — flatten back during snap-to-look
for idx, op in enumerate(orbit_parent_bones):
    rabbit = op["rabbit"]
    base_zL = 6.0
    base_zR = -6.0
    ear_L_kfs = [
        make_keyframe("rotation", 0.0,  0, 0, base_zL),
        make_keyframe("rotation", 0.10, -25, 0, base_zL + 12),  # ears alert/snap
        make_keyframe("rotation", 0.30, -25, 0, base_zL + 12),  # hold alert
        make_keyframe("rotation", 0.45, -45, 0, base_zL + 4),
        make_keyframe("rotation", 0.7,  -90, 0, 0),
    ]
    ear_R_kfs = [
        make_keyframe("rotation", 0.0,  0, 0, base_zR),
        make_keyframe("rotation", 0.10, -25, 0, base_zR - 12),
        make_keyframe("rotation", 0.30, -25, 0, base_zR - 12),
        make_keyframe("rotation", 0.45, -45, 0, base_zR - 4),
        make_keyframe("rotation", 0.7,  -90, 0, 0),
    ]
    dissipate_animators[rabbit["ear_L"]["uuid"]] = {
        "name": rabbit["ear_L"]["name"], "type": "bone", "keyframes": ear_L_kfs,
    }
    dissipate_animators[rabbit["ear_R"]["uuid"]] = {
        "name": rabbit["ear_R"]["name"], "type": "bone", "keyframes": ear_R_kfs,
    }

# Summoning circle expands then snaps
sc_diss_kfs = [
    make_keyframe("scale", 0.0,  1.0,  1.0,  1.0),
    make_keyframe("scale", 0.20, 1.05, 1.0,  1.05),
    make_keyframe("scale", 0.40, 2.2,  1.0,  2.2),
    make_keyframe("scale", 0.55, 2.5,  1.0,  2.5),
    make_keyframe("scale", 0.6,  0.05, 0.05, 0.05),
    make_keyframe("scale", 0.7,  0.0,  0.0,  0.0),
]
dissipate_animators[summoning_circle_bone["uuid"]] = {
    "name": summoning_circle_bone["name"], "type": "bone", "keyframes": sc_diss_kfs,
}

# Orbit ring rapidly expands
ring_diss_kfs = [
    make_keyframe("scale", 0.0,  1.0,  1.0,  1.0),
    make_keyframe("scale", 0.30, 1.0,  1.0,  1.0),
    make_keyframe("scale", 0.50, 2.5,  1.0,  2.5),
    make_keyframe("scale", 0.65, 3.5,  0.3,  3.5),
    make_keyframe("scale", 0.7,  0.05, 0.05, 0.05),
]
dissipate_animators[orbit_ring_bone["uuid"]] = {
    "name": orbit_ring_bone["name"], "type": "bone", "keyframes": ring_diss_kfs,
}

# Tails: stop wiggling, freeze
for idx, op in enumerate(orbit_parent_bones):
    rabbit = op["rabbit"]
    tail_kfs = [
        make_keyframe("rotation", 0.0, 0, 0, 0),
        make_keyframe("rotation", 0.10, 0, 0, 0),
        make_keyframe("rotation", 0.30, 0, 0, 0),
        make_keyframe("rotation", 0.7, 0, 0, 0),
    ]
    dissipate_animators[rabbit["tail"]["uuid"]] = {
        "name": rabbit["tail"]["name"], "type": "bone", "keyframes": tail_kfs,
    }

b.add_animation("dissipate", length=0.7, animators=dissipate_animators, loop="once", override=True)


# -----------------------------------------------------------------------------
# Textures + write
# -----------------------------------------------------------------------------
b.add_texture("rabbit_white", build_rabbit_white_texture())
b.add_texture("rabbit_pink_lavender", build_pink_lavender_texture())

OUT = "D:/CC/ChaosCraft/src/main/resources/models/fluffy/me_attacks/rabbit_army_multiplication.bbmodel"
path = b.write(OUT)
size_kb = Path(path).stat().st_size // 1024
print(f"Wrote: {path}")
print(f"Size: {size_kb} KB")
print(f"Elements: {len(b.elements)}")
print(f"Animations: {[a['name'] for a in b.animations]}")
