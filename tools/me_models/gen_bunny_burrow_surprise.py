#!/usr/bin/env python3
"""
Generator for bunny_burrow_surprise.bbmodel

8 pairs of cartoon bunny ears erupting from the ground in a circle (radius 8).
Each ear pair is a separate bone group containing:
  - 2 tall tapered flat slab chains (3 cubes per ear, narrowing to rounded tip)
  - 2 inner ear pink slabs (one per ear)
  - 3 ear-fluff base cubes
  - 8 ground rupture slab pieces around the base

Animations:
  - spawn (0.8s, override=true)
  - idle  (5.0s, override=false, loop=loop)
  - dissipate (0.5s, override=true)
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
# Texture 0: Bunny fur (white with subtle grey shadow + lavender tip emissive)
# -----------------------------------------------------------------------------
def build_fur_texture():
    W, H = 64, 64
    base   = hex_to_rgba("#fefcfa")     # pure soft white
    shadow = hex_to_rgba("#d8d4d0")     # subtle cool grey
    fold   = hex_to_rgba("#b0aca8")     # deep ear-fold shadow
    spec   = hex_to_rgba("#ffffff")     # specular white
    tip    = hex_to_rgba("#e8c8e0")     # lavender-pink emissive
    pixels = [base] * (W * H)

    # Upper ear-tip zone (top 10 rows): subtle gradient base -> lavender tip
    gradient_horizontal_band(pixels, W, H, 0, 10, tip, base)
    # Mid zone is base white, occasional grey shadow column suggesting ear fold
    for x in range(W):
        if x % 9 == 4:
            for y in range(10, 50):
                pixels[y * W + x] = shadow
    # Deep fold shadow band 32-34 (interior shadow stripe)
    fill_rect(pixels, W, H, 0, 32, W, 34, fold)
    fill_rect(pixels, W, H, 0, 33, W, 34, shadow)
    # Bottom 10 rows: a slightly darker base for fluff base region
    gradient_horizontal_band(pixels, W, H, 54, 64, base, shadow)
    # Fine horizontal grain every 4 rows
    for y in range(0, H, 4):
        for x in range(W):
            r, g, b, a = pixels[y * W + x]
            pixels[y * W + x] = (
                max(0, r - 4),
                max(0, g - 4),
                max(0, b - 4),
                a,
            )
    # Specular highlights — small clusters near top center
    for (cx, cy) in [(20, 6), (44, 8), (32, 18), (12, 24), (52, 30)]:
        fill_rect(pixels, W, H, cx, cy, cx + 2, cy + 2, spec)
    # Emissive tip pixels (2x2 spots near top)
    for (cx, cy) in [(8, 2), (28, 1), (50, 3), (60, 0), (16, 4), (40, 4)]:
        fill_rect(pixels, W, H, cx, cy, cx + 2, cy + 2, tip)
    # Light fur noise overlay
    add_noise_overlay(pixels, W, H, shadow, spec, density=0.04, seed=88)
    return png_from_pixels(pixels, W, H)


# -----------------------------------------------------------------------------
# Texture 1: Inner ear baby pink + ground disruption
# -----------------------------------------------------------------------------
def build_pink_texture():
    W, H = 64, 64
    base       = hex_to_rgba("#ffaac0")  # bright baby pink
    deeper     = hex_to_rgba("#e87898")  # deeper pink mid
    pale       = hex_to_rgba("#ffd8e8")  # pale highlight
    edge_glow  = hex_to_rgba("#ffc0d8")  # edge glow (emissive feel)
    soil       = hex_to_rgba("#c8a090")  # ground disruption outer dark
    pixels = [base] * (W * H)

    # Top half: vertical gradient pale -> base for inner ear fade
    gradient_horizontal_band(pixels, W, H, 0, 32, pale, base)
    # Bottom half: gradient base -> deeper for ear interior shadow
    gradient_horizontal_band(pixels, W, H, 32, 56, base, deeper)
    # Outer 8px ring on bottom area for ground/soil texture
    fill_rect(pixels, W, H, 0, 56, W, 64, soil)
    # Soil mottling
    for (cx, cy) in [(4, 58), (14, 60), (24, 57), (34, 61), (44, 59), (54, 60)]:
        fill_rect(pixels, W, H, cx, cy, cx + 3, cy + 2, deeper)
    # Edge glow accents — vertical strips on left/right of ear region
    fill_rect(pixels, W, H, 0, 8, 2, 50, edge_glow)
    fill_rect(pixels, W, H, 62, 8, 64, 50, edge_glow)
    # Specular pale highlights (the shiny inner ear vein)
    for x in range(20, 44):
        pixels[16 * W + x] = pale
        pixels[20 * W + x] = pale
    # Center vein
    for y in range(8, 50):
        pixels[y * W + 31] = pale
        pixels[y * W + 32] = edge_glow
    # Speckle noise
    add_noise_overlay(pixels, W, H, deeper, pale, density=0.05, seed=42)
    return png_from_pixels(pixels, W, H)


# -----------------------------------------------------------------------------
# Build the model
# -----------------------------------------------------------------------------
b = Builder("bunny_burrow_surprise", resolution=(64, 64), visible_box=(6, 6, 0))


def make_ear_chain(prefix, x_off, z_off, mirror=False):
    """Build a single ear (3 stacked cubes, narrowing). Returns list of cube uuids."""
    cube_ids = []
    # Cube A: base wide slab (height 0..6)
    a = b.add_cube(
        f"{prefix}_base", from_=[x_off - 1.4, 0.0, z_off - 0.35],
        to_=[x_off + 1.4, 6.0, z_off + 0.35],
        faces=basic_cube_faces(0, 0, 3, 6, 1, tex_index=0),
    )
    cube_ids.append(a)
    # Cube B: middle slab (height 5.5..10.5) — narrower
    bm = b.add_cube(
        f"{prefix}_mid", from_=[x_off - 1.0, 5.5, z_off - 0.3],
        to_=[x_off + 1.0, 10.5, z_off + 0.3],
        faces=basic_cube_faces(12, 0, 2, 5, 1, tex_index=0),
    )
    cube_ids.append(bm)
    # Cube C: tip rounded slab (height 10..13.2) — narrowest, with side trim cubes
    c = b.add_cube(
        f"{prefix}_tip", from_=[x_off - 0.65, 10.0, z_off - 0.25],
        to_=[x_off + 0.65, 13.2, z_off + 0.25],
        faces=basic_cube_faces(22, 0, 1, 3, 1, tex_index=0),
    )
    cube_ids.append(c)
    return cube_ids


def make_inner_ear(prefix, x_off, z_off):
    """Pink inner ear thin slab embedded in front face of an ear."""
    cube_id = b.add_cube(
        f"{prefix}_inner", from_=[x_off - 0.7, 1.5, z_off - 0.5],
        to_=[x_off + 0.7, 9.5, z_off - 0.34],
        faces=basic_cube_faces(0, 0, 1, 8, 1, tex_index=1),
    )
    return cube_id


def make_fluff_cluster(prefix, cx, cz):
    """3 small fur cubes clustered at base of ear pair."""
    ids = []
    spots = [
        (cx - 1.6, cz + 0.0,  1.0),
        (cx + 0.0, cz - 1.4,  1.1),
        (cx + 1.5, cz + 0.6,  0.9),
    ]
    for i, (px, pz, sz) in enumerate(spots):
        cid = b.add_cube(
            f"{prefix}_fluff_{i}",
            from_=[px - sz, 0.0, pz - sz],
            to_=[px + sz, sz * 1.1, pz + sz],
            faces=basic_cube_faces(40, 40, 2, 2, 2, tex_index=0),
        )
        ids.append(cid)
    return ids


def make_ground_rupture(prefix, cx, cz):
    """8 small rounded flat slab pieces displaced soil at Y=0 around base."""
    ids = []
    for i in range(8):
        ang = (i / 8.0) * math.tau
        rx = cx + math.cos(ang) * 2.4
        rz = cz + math.sin(ang) * 2.4
        cid = b.add_cube(
            f"{prefix}_soil_{i}",
            from_=[rx - 0.7, -0.2, rz - 0.7],
            to_=[rx + 0.7, 0.25, rz + 0.7],
            faces=basic_cube_faces(0, 56, 1, 1, 1, tex_index=1),
        )
        ids.append(cid)
    return ids


# Track bones we need for animation
ear_pair_bones = []     # list of (uuid, name, theta) — root bone of each pair
left_ear_bones = []     # individual ear sub-bones for wiggle
right_ear_bones = []
inner_ear_bones = []    # for scale pulses
fluff_bones = []        # for sink-last
ground_rupture_bones = []  # for blast outward

NUM_PAIRS = 8
RING_RADIUS = 8.0

root_children = []

for i in range(NUM_PAIRS):
    theta = (i / NUM_PAIRS) * math.tau
    cx = math.cos(theta) * RING_RADIUS
    cz = math.sin(theta) * RING_RADIUS

    # Outward angle 22.5° from vertical, axis tangent-perpendicular (radial outward tilt)
    # We'll bake this into the bone rotation: tilt around an axis perpendicular to radial
    # Simplification: rotate group around its own X axis by 22.5° then around Y by theta-degrees
    # so ears tilt outward from vertical
    deg = math.degrees(theta)
    # The 22.5 outward tilt: rotate around X first within local space; then group rotation Y orients it
    # We approximate by placing the whole pair group at (cx,0,cz) with rotation Y=deg, then
    # giving each ear a local +/- X offset and a local Z rotation 22.5 (outward) on left, -22.5 on right

    # --- Left ear (offset -1.6 along local X) ---
    left_cubes = make_ear_chain(f"pair{i}_L", x_off=-1.6, z_off=0.0)
    left_inner = make_inner_ear(f"pair{i}_L", x_off=-1.6, z_off=0.0)
    inner_L_bone = b.make_group(
        f"pair{i}_inner_L",
        origin=[-1.6, 1.5, 0.0],
        children=[left_inner],
    )
    inner_ear_bones.append((inner_L_bone["uuid"], inner_L_bone["name"]))
    left_ear_bone = b.make_group(
        f"pair{i}_ear_L",
        origin=[-1.6, 0.0, 0.0],
        rotation=[0, 0, 22.5],  # outward tilt (left side rolls away to its left)
        children=left_cubes + [inner_L_bone],
    )
    left_ear_bones.append((left_ear_bone["uuid"], left_ear_bone["name"]))

    # --- Right ear (offset +1.6 along local X) ---
    right_cubes = make_ear_chain(f"pair{i}_R", x_off=1.6, z_off=0.0)
    right_inner = make_inner_ear(f"pair{i}_R", x_off=1.6, z_off=0.0)
    inner_R_bone = b.make_group(
        f"pair{i}_inner_R",
        origin=[1.6, 1.5, 0.0],
        children=[right_inner],
    )
    inner_ear_bones.append((inner_R_bone["uuid"], inner_R_bone["name"]))
    right_ear_bone = b.make_group(
        f"pair{i}_ear_R",
        origin=[1.6, 0.0, 0.0],
        rotation=[0, 0, -22.5],
        children=right_cubes + [inner_R_bone],
    )
    right_ear_bones.append((right_ear_bone["uuid"], right_ear_bone["name"]))

    # --- Fluff cluster at base ---
    fluff_cubes = make_fluff_cluster(f"pair{i}", cx=0.0, cz=0.0)
    fluff_bone = b.make_group(
        f"pair{i}_fluff",
        origin=[0, 0, 0],
        children=fluff_cubes,
    )
    fluff_bones.append((fluff_bone["uuid"], fluff_bone["name"]))

    # --- Ground rupture pieces ---
    rupture_cubes = make_ground_rupture(f"pair{i}", cx=0.0, cz=0.0)
    # Wrap into individual sub-bones so they can blast outward independently
    rupture_subbones = []
    for j, rc in enumerate(rupture_cubes):
        sub_ang = (j / 8.0) * math.tau
        sub = b.make_group(
            f"pair{i}_soil_{j}",
            origin=[
                math.cos(sub_ang) * 2.4,
                0.0,
                math.sin(sub_ang) * 2.4,
            ],
            children=[rc],
        )
        rupture_subbones.append(sub)
        ground_rupture_bones.append((sub["uuid"], sub["name"], sub_ang))
    rupture_parent = b.make_group(
        f"pair{i}_rupture",
        origin=[0, 0, 0],
        children=rupture_subbones,
    )

    # --- Pair root bone: positioned on circle, rotated to face outward ---
    pair_bone = b.make_group(
        f"pair{i}_root",
        origin=[cx, 0.0, cz],
        rotation=[0, deg, 0],
        children=[
            left_ear_bone,
            right_ear_bone,
            fluff_bone,
            rupture_parent,
        ],
    )
    ear_pair_bones.append((pair_bone["uuid"], pair_bone["name"], theta))
    root_children.append(pair_bone)


# Wrap everything in a master root bone (so we can shake the soil at start)
master = b.make_group(
    "root",
    origin=[0, 0, 0],
    children=root_children,
)
b.outliner.append(master)


# -----------------------------------------------------------------------------
# ANIMATIONS
# -----------------------------------------------------------------------------

# ===== SPAWN =====
# - master root: brief soil shake Y +/- 0.3 around 0.1s
# - each pair: starts at Y=-3, holds, then bursts to Y=0 between 0.2 and 0.35s
# - each pair: wiggle Z rotation ±5° twice immediately after height
# - inner ear bones: scale 0 -> 1 as ears reach height (~0.35s)
# - ground rupture sub-bones: blast outward at 0.2s
# - fluff bones: rise with pair (parented under pair_root, so they auto follow)
spawn_animators = {}

# Master root soil shake
master_kfs = [
    make_keyframe("position", 0.0,  0, 0, 0),
    make_keyframe("position", 0.05, 0, 0.3, 0),
    make_keyframe("position", 0.1,  0, -0.3, 0),
    make_keyframe("position", 0.15, 0, 0.2, 0),
    make_keyframe("position", 0.2,  0, 0, 0),
    make_keyframe("position", 0.8,  0, 0, 0),
]
spawn_animators[master["uuid"]] = {
    "name": master["name"], "type": "bone", "keyframes": master_kfs,
}

# Each pair: start below ground, burst up
for idx, (uuid_, name, theta) in enumerate(ear_pair_bones):
    kfs = [
        make_keyframe("position", 0.0, 0, -3.0, 0),
        make_keyframe("position", 0.18, 0, -3.0, 0),
        make_keyframe("position", 0.20, 0, -3.0, 0),
        # Burst window 0.20 -> 0.35 (rapid 0.15s) all simultaneous
        make_keyframe("position", 0.30, 0, 0.6, 0, interpolation="linear"),
        make_keyframe("position", 0.35, 0, 0.0, 0, interpolation="linear"),
        make_keyframe("position", 0.45, 0, 0.0, 0),
        make_keyframe("position", 0.8, 0, 0.0, 0),
    ]
    spawn_animators[uuid_] = {
        "name": name, "type": "bone", "keyframes": kfs,
    }

# Each ear (left + right): immediate wiggle after height (0.35s -> 0.7s)
for (uuid_, name) in left_ear_bones + right_ear_bones:
    base_z = 22.5 if "_L" in name else -22.5
    rot_kfs = [
        make_keyframe("rotation", 0.0,  0, 0, base_z),
        make_keyframe("rotation", 0.35, 0, 0, base_z),
        make_keyframe("rotation", 0.43, 0, 0, base_z + 5.0),
        make_keyframe("rotation", 0.51, 0, 0, base_z - 5.0),
        make_keyframe("rotation", 0.59, 0, 0, base_z + 5.0),
        make_keyframe("rotation", 0.67, 0, 0, base_z - 5.0),
        make_keyframe("rotation", 0.75, 0, 0, base_z),
        make_keyframe("rotation", 0.8,  0, 0, base_z),
    ]
    spawn_animators[uuid_] = {
        "name": name, "type": "bone", "keyframes": rot_kfs,
    }

# Inner ear scale 0 -> 1 as ears reach full height
for (uuid_, name) in inner_ear_bones:
    sc_kfs = [
        make_keyframe("scale", 0.0,  0.05, 0.05, 0.05),
        make_keyframe("scale", 0.30, 0.05, 0.05, 0.05),
        make_keyframe("scale", 0.40, 1.15, 1.15, 1.15),
        make_keyframe("scale", 0.50, 1.0,  1.0,  1.0),
        make_keyframe("scale", 0.8,  1.0,  1.0,  1.0),
    ]
    spawn_animators[uuid_] = {
        "name": name, "type": "bone", "keyframes": sc_kfs,
    }

# Ground rupture sub-bones blast outward at 0.2s
for (uuid_, name, sub_ang) in ground_rupture_bones:
    out_x = math.cos(sub_ang) * 1.6
    out_z = math.sin(sub_ang) * 1.6
    pos_kfs = [
        make_keyframe("position", 0.0, 0, 0, 0),
        make_keyframe("position", 0.18, 0, 0, 0),
        make_keyframe("position", 0.22, out_x * 0.4, 0.5, out_z * 0.4),
        make_keyframe("position", 0.30, out_x, 0.2, out_z),
        make_keyframe("position", 0.45, out_x * 1.05, 0.0, out_z * 1.05),
        make_keyframe("position", 0.8, out_x, 0.0, out_z),
    ]
    spawn_animators[uuid_] = {
        "name": name, "type": "bone", "keyframes": pos_kfs,
    }

b.add_animation("spawn", length=0.8, animators=spawn_animators, loop="once", override=True)


# ===== IDLE (5.0s, loop, override=false) =====
idle_animators = {}

# Each ear pair: independent wiggle on Z (±8° on individual periods 2.5-4s)
# Use varied frequencies & phases per pair so they NEVER sync
for idx, (uuid_, name) in enumerate(left_ear_bones + right_ear_bones):
    base_z = 22.5 if "_L" in name else -22.5
    # individual period in seconds
    pair_idx = idx % NUM_PAIRS
    period = 2.5 + (pair_idx * 0.18) + (0.4 if "_R" in name else 0.0)
    phase = (pair_idx * 0.31 + (0.7 if "_R" in name else 0.0)) % 1.0
    kfs = []
    # Sample 6 phases evenly across 5s, but shift each by phase
    samples = 6
    for s in range(samples + 1):
        t = (s / samples) * 5.0
        # Drive a sin wave with this period and phase
        val = math.sin(((t / period) + phase) * math.tau)
        kfs.append(
            make_keyframe("rotation", t, 0, 0, base_z + val * 8.0)
        )
    idle_animators[uuid_] = {
        "name": name, "type": "bone", "keyframes": kfs,
    }

# Inner ear scale pulse (very subtle 0.97 - 1.05) on staggered cycles
for idx, (uuid_, name) in enumerate(inner_ear_bones):
    period = 1.8 + (idx % 7) * 0.13
    phase = (idx * 0.27) % 1.0
    kfs = []
    for s in range(7):
        t = (s / 6.0) * 5.0
        val = math.sin(((t / period) + phase) * math.tau) * 0.04 + 1.0
        kfs.append(make_keyframe("scale", t, val, val, val))
    idle_animators[uuid_] = {
        "name": name, "type": "bone", "keyframes": kfs,
    }

# Ground rupture: subtle pulse Y up/down
for idx, (uuid_, name, sub_ang) in enumerate(ground_rupture_bones):
    out_x = math.cos(sub_ang) * 1.6
    out_z = math.sin(sub_ang) * 1.6
    period = 2.2 + (idx % 5) * 0.17
    phase = (idx * 0.19) % 1.0
    kfs = []
    for s in range(6):
        t = (s / 5.0) * 5.0
        val = math.sin(((t / period) + phase) * math.tau) * 0.18
        kfs.append(make_keyframe("position", t, out_x, val, out_z))
    idle_animators[uuid_] = {
        "name": name, "type": "bone", "keyframes": kfs,
    }

# Pair root: tiny independent breathing motion (Y micro-bob)
for idx, (uuid_, name, theta) in enumerate(ear_pair_bones):
    period = 2.8 + (idx * 0.21)
    phase = (idx * 0.43) % 1.0
    kfs = []
    for s in range(6):
        t = (s / 5.0) * 5.0
        val = math.sin(((t / period) + phase) * math.tau) * 0.08
        kfs.append(make_keyframe("position", t, 0, val, 0))
    idle_animators[uuid_] = {
        "name": name, "type": "bone", "keyframes": kfs,
    }

# Fluff bones: very subtle scale breathe
for idx, (uuid_, name) in enumerate(fluff_bones):
    period = 3.1 + (idx % 6) * 0.22
    phase = (idx * 0.37) % 1.0
    kfs = []
    for s in range(6):
        t = (s / 5.0) * 5.0
        val = math.sin(((t / period) + phase) * math.tau) * 0.05 + 1.0
        kfs.append(make_keyframe("scale", t, val, 1.0, val))
    idle_animators[uuid_] = {
        "name": name, "type": "bone", "keyframes": kfs,
    }

b.add_animation("idle", length=5.0, animators=idle_animators, loop="loop", override=False)


# ===== DISSIPATE (0.5s, override=true) =====
# - ears flatten downward (rotation X -> 90° as if collapsing)
# - each pair sinks below ground with stagger of 0.04s
# - ground rupture pieces contract inward
# - fluff bases sink last
dissipate_animators = {}

# Each pair sinks back below ground, staggered
for idx, (uuid_, name, theta) in enumerate(ear_pair_bones):
    stagger = (idx % NUM_PAIRS) * 0.04
    end_t = 0.5
    kfs = [
        make_keyframe("position", 0.0, 0, 0, 0),
        make_keyframe("position", stagger, 0, 0, 0),
        make_keyframe("position", stagger + 0.18, 0, -1.5, 0),
        make_keyframe("position", end_t, 0, -3.5, 0),
    ]
    dissipate_animators[uuid_] = {
        "name": name, "type": "bone", "keyframes": kfs,
    }

# Ears flatten — bend forward (rotation X -> +60°) before sink
for (uuid_, name) in left_ear_bones + right_ear_bones:
    base_z = 22.5 if "_L" in name else -22.5
    kfs = [
        make_keyframe("rotation", 0.0,  0,  0, base_z),
        make_keyframe("rotation", 0.15, 30, 0, base_z * 0.6),
        make_keyframe("rotation", 0.30, 70, 0, base_z * 0.2),
        make_keyframe("rotation", 0.5,  85, 0, 0),
    ]
    dissipate_animators[uuid_] = {
        "name": name, "type": "bone", "keyframes": kfs,
    }

# Inner ears scale down to 0
for idx, (uuid_, name) in enumerate(inner_ear_bones):
    kfs = [
        make_keyframe("scale", 0.0,  1.0,  1.0,  1.0),
        make_keyframe("scale", 0.25, 0.6,  0.6,  0.6),
        make_keyframe("scale", 0.5,  0.05, 0.05, 0.05),
    ]
    dissipate_animators[uuid_] = {
        "name": name, "type": "bone", "keyframes": kfs,
    }

# Ground rupture contract inward
for (uuid_, name, sub_ang) in ground_rupture_bones:
    out_x = math.cos(sub_ang) * 1.6
    out_z = math.sin(sub_ang) * 1.6
    kfs = [
        make_keyframe("position", 0.0, out_x, 0,    out_z),
        make_keyframe("position", 0.2, out_x * 0.5, 0, out_z * 0.5),
        make_keyframe("position", 0.4, 0,    -0.3, 0),
        make_keyframe("position", 0.5, 0,    -0.6, 0),
    ]
    dissipate_animators[uuid_] = {
        "name": name, "type": "bone", "keyframes": kfs,
    }

# Fluff bones sink last (start sinking at 0.35s)
for idx, (uuid_, name) in enumerate(fluff_bones):
    kfs = [
        make_keyframe("position", 0.0,  0, 0,    0),
        make_keyframe("position", 0.30, 0, 0,    0),
        make_keyframe("position", 0.42, 0, -0.8, 0),
        make_keyframe("position", 0.5,  0, -2.0, 0),
    ]
    dissipate_animators[uuid_] = {
        "name": name, "type": "bone", "keyframes": kfs,
    }

b.add_animation("dissipate", length=0.5, animators=dissipate_animators, loop="once", override=True)


# -----------------------------------------------------------------------------
# Textures + write
# -----------------------------------------------------------------------------
b.add_texture("bunny_fur", build_fur_texture())
b.add_texture("bunny_inner_pink", build_pink_texture())

OUT = "D:/CC/ChaosCraft/src/main/resources/models/fluffy/me_attacks/bunny_burrow_surprise.bbmodel"
path = b.write(OUT)
size_kb = Path(path).stat().st_size // 1024
print(f"Wrote: {path}")
print(f"Size: {size_kb} KB")
print(f"Elements: {len(b.elements)}")
print(f"Animations: {[a['name'] for a in b.animations]}")
