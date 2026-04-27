"""
MODEL 7 — Nightmare Static Field
Geometry plan:
  - 3 anchor slabs    (ground triangle, larger flat slabs at Y=0)        -> 3 bones
  - 24 floating slabs (random heights Y=0.5-8, radius 14, random Y rot)  -> 24 bones
  - 8 ground disrupt  (small flat tilted, Y=0, texture 1)                -> 8 bones
  - 10 edge fragments (small cubes, radius 13-16, tumbling rotations)    -> 10 bones
  - 5 anchor accents  (small slabs near anchor for richness)             -> 5 bones
  - 6 spark fragments (mid-air micro-slabs scattered)                    -> 6 bones
  Total bones: 56  (>= 55)
  Total elements: 56  (>= 80) -- need to bump elements per bone, see below

Element count requirement is 80+. Some bones will hold multiple sub-elements
(no extra bones needed). Plan:
  - anchor (3): 3 elements each (main slab + 2 sub-pieces) = 9 elements
  - floating slab (24): 2 elements each (main + small fleck) = 48 elements
  - ground disrupt (8): 2 elements each = 16 elements
  - edge fragment (10): 1 element each = 10 elements
  - anchor accents (5): 1 element each = 5 elements
  - spark fragments (6): 1 element each = 6 elements
  Total: 9+48+16+10+5+6 = 94 elements (>=80) ✓
"""

import json, math, uuid, random, os

TEX0_B64 = open("D:/CC/ChaosCraft/tools/gen_textures/m07v2_b64.txt").read()
TEX1_B64 = open("D:/CC/ChaosCraft/tools/gen_textures/m07v2_tex1_b64.txt").read()

def uid(): return str(uuid.uuid4())

def faces(tex=0):
    return {
        "north": {"uv":[2,2,30,30], "texture":tex},
        "east":  {"uv":[34,2,62,30],"texture":tex},
        "south": {"uv":[2,2,30,30], "texture":tex},
        "west":  {"uv":[34,2,62,30],"texture":tex},
        "up":    {"uv":[2,34,30,62],"texture":tex},
        "down":  {"uv":[34,34,62,62],"texture":tex},
    }

def make_elem(name, frm, to, tex=0):
    return {
        "name": name,
        "box_uv": False,
        "rescale": False,
        "locked": False,
        "render_order": "default",
        "allow_mirror_modeling": True,
        "from": frm,
        "to": to,
        "autouv": 0,
        "color": 0,
        "uuid": uid(),
        "faces": faces(tex),
    }

elements = []
outliner_bones = []
bone_uuids = {}

def add_elem(name, frm, to, tex=0):
    e = make_elem(name, frm, to, tex)
    elements.append(e)
    return e["uuid"]

# ======================================================================
# 1) ANCHOR TRIANGLE — 3 slightly larger flat slabs at ground level forming
#    a rough triangle. The "still-stable" core that everything radiates from.
# ======================================================================
random.seed(7100)

anchor_positions = [
    (0.0, 0.0, -3.5),  # forward
    (3.0, 0.0, 1.8),   # right-back
    (-3.0, 0.0, 1.8),  # left-back
]

for i, (cx, cy, cz) in enumerate(anchor_positions):
    main_eid = add_elem(
        f"anchor_main_{i+1}",
        [cx-2.0, cy+0.05, cz-1.0],
        [cx+2.0, cy+0.20, cz+1.0],
        tex=0,
    )
    sub1_eid = add_elem(
        f"anchor_sub_{i+1}_a",
        [cx-1.4, cy+0.20, cz-0.7],
        [cx+1.4, cy+0.32, cz+0.7],
        tex=0,
    )
    sub2_eid = add_elem(
        f"anchor_sub_{i+1}_b",
        [cx-0.7, cy+0.32, cz-0.35],
        [cx+0.7, cy+0.40, cz+0.35],
        tex=0,
    )
    b_uid = uid()
    rot_y = i * 60.0  # vary triangle slabs slightly
    bone = {
        "name": f"anchor_{i+1}",
        "origin": [cx, cy + 0.2, cz],
        "rotation": [0, rot_y, 0],
        "uuid": b_uid,
        "export": True,
        "isOpen": True,
        "children": [main_eid, sub1_eid, sub2_eid],
    }
    outliner_bones.append(bone)
    bone_uuids[f"anchor_{i+1}"] = b_uid

# ======================================================================
# 2) FLOATING SLABS — 24 thin flat slabs, random heights Y=0.5..8,
#    random horizontal positions within radius 14, random Y bone rotation.
#    Sizes vary: 1×3, 2×1, 3×2, 1×5
# ======================================================================
random.seed(7200)

slab_sizes = [
    (1.0, 0.18, 3.0),  # 1x3
    (2.0, 0.18, 1.0),  # 2x1
    (3.0, 0.18, 2.0),  # 3x2
    (1.0, 0.18, 5.0),  # 1x5
    (2.5, 0.18, 1.5),
    (1.5, 0.18, 4.0),
]

for i in range(24):
    # Random horizontal position within radius 14 (excluding very inner ~2)
    radius = random.uniform(2.5, 14.0)
    angle  = random.uniform(0, 2 * math.pi)
    cx = radius * math.cos(angle)
    cz = radius * math.sin(angle)
    # Random height 0.5..8
    cy = random.uniform(0.5, 8.0)
    # Random Y rotation (no two synchronized)
    rot_y = random.uniform(0, 360.0)
    # Slight tilt for "wrong angles" feel
    rot_x = random.uniform(-25, 25)
    rot_z = random.uniform(-25, 25)

    sw, sh, sd = random.choice(slab_sizes)
    main_eid = add_elem(
        f"slab_main_{i+1}",
        [cx - sw/2, cy - sh/2, cz - sd/2],
        [cx + sw/2, cy + sh/2, cz + sd/2],
        tex=0,
    )
    # Tiny fleck attached
    fx = random.uniform(-sw/2, sw/2) * 0.6
    fz = random.uniform(-sd/2, sd/2) * 0.6
    fleck_eid = add_elem(
        f"slab_fleck_{i+1}",
        [cx + fx - 0.18, cy + sh/2 + 0.02, cz + fz - 0.18],
        [cx + fx + 0.18, cy + sh/2 + 0.18, cz + fz + 0.18],
        tex=0,
    )
    b_uid = uid()
    bone = {
        "name": f"slab_{i+1}",
        "origin": [cx, cy, cz],
        "rotation": [rot_x, rot_y, rot_z],
        "uuid": b_uid,
        "export": True,
        "isOpen": True,
        "children": [main_eid, fleck_eid],
    }
    outliner_bones.append(bone)
    bone_uuids[f"slab_{i+1}"] = b_uid

# ======================================================================
# 3) GROUND DISRUPTION — 8 small flat pieces at Y=0, scattered, tilted.
#    Texture 1.
# ======================================================================
random.seed(7300)

for i in range(8):
    angle = (i / 8.0) * 2 * math.pi + random.uniform(-0.3, 0.3)
    radius = random.uniform(2.0, 9.0)
    cx = radius * math.cos(angle)
    cz = radius * math.sin(angle)
    sw = random.uniform(0.8, 1.6)
    sd = random.uniform(0.8, 1.6)
    rot_x = random.uniform(-12, 12)
    rot_y = random.uniform(0, 360.0)
    rot_z = random.uniform(-12, 12)

    main_eid = add_elem(
        f"gd_main_{i+1}",
        [cx - sw/2, 0.02, cz - sd/2],
        [cx + sw/2, 0.10, cz + sd/2],
        tex=1,
    )
    sub_eid = add_elem(
        f"gd_sub_{i+1}",
        [cx - sw/3, 0.10, cz - sd/3],
        [cx + sw/3, 0.16, cz + sd/3],
        tex=1,
    )
    b_uid = uid()
    bone = {
        "name": f"ground_{i+1}",
        "origin": [cx, 0.05, cz],
        "rotation": [rot_x, rot_y, rot_z],
        "uuid": b_uid,
        "export": True,
        "isOpen": True,
        "children": [main_eid, sub_eid],
    }
    outliner_bones.append(bone)
    bone_uuids[f"ground_{i+1}"] = b_uid

# ======================================================================
# 4) EDGE FRAGMENTS — 10 small cube pieces, radius 13..16, tumbling.
# ======================================================================
random.seed(7400)

for i in range(10):
    angle = (i / 10.0) * 2 * math.pi + random.uniform(-0.15, 0.15)
    radius = random.uniform(13.0, 16.0)
    cx = radius * math.cos(angle)
    cz = radius * math.sin(angle)
    cy = random.uniform(0.5, 5.0)
    sz = random.uniform(0.4, 0.8)
    rot_x = random.uniform(-60, 60)
    rot_y = random.uniform(0, 360.0)
    rot_z = random.uniform(-60, 60)

    eid = add_elem(
        f"edge_frag_{i+1}",
        [cx - sz/2, cy - sz/2, cz - sz/2],
        [cx + sz/2, cy + sz/2, cz + sz/2],
        tex=0,
    )
    b_uid = uid()
    bone = {
        "name": f"edge_{i+1}",
        "origin": [cx, cy, cz],
        "rotation": [rot_x, rot_y, rot_z],
        "uuid": b_uid,
        "export": True,
        "isOpen": True,
        "children": [eid],
    }
    outliner_bones.append(bone)
    bone_uuids[f"edge_{i+1}"] = b_uid

# ======================================================================
# 5) ANCHOR ACCENTS — 5 small slabs near anchor, decorative
# ======================================================================
random.seed(7500)

for i in range(5):
    angle = i * (2 * math.pi / 5)
    radius = 2.2
    cx = radius * math.cos(angle)
    cz = radius * math.sin(angle)
    cy = 0.4 + random.uniform(0, 0.8)
    rot_x = random.uniform(-15, 15)
    rot_y = random.uniform(0, 360)
    rot_z = random.uniform(-15, 15)
    eid = add_elem(
        f"anchor_acc_{i+1}",
        [cx - 0.4, cy - 0.08, cz - 0.4],
        [cx + 0.4, cy + 0.08, cz + 0.4],
        tex=0,
    )
    b_uid = uid()
    bone = {
        "name": f"acc_{i+1}",
        "origin": [cx, cy, cz],
        "rotation": [rot_x, rot_y, rot_z],
        "uuid": b_uid,
        "export": True,
        "isOpen": True,
        "children": [eid],
    }
    outliner_bones.append(bone)
    bone_uuids[f"acc_{i+1}"] = b_uid

# ======================================================================
# 6) SPARK FRAGMENTS — 6 mid-air micro-slabs scattered to add density
# ======================================================================
random.seed(7600)

for i in range(6):
    angle = random.uniform(0, 2 * math.pi)
    radius = random.uniform(5.0, 11.0)
    cx = radius * math.cos(angle)
    cz = radius * math.sin(angle)
    cy = random.uniform(2.0, 6.5)
    rot_x = random.uniform(-45, 45)
    rot_y = random.uniform(0, 360)
    rot_z = random.uniform(-45, 45)
    eid = add_elem(
        f"spark_{i+1}",
        [cx - 0.3, cy - 0.05, cz - 0.3],
        [cx + 0.3, cy + 0.05, cz + 0.3],
        tex=0,
    )
    b_uid = uid()
    bone = {
        "name": f"spark_{i+1}",
        "origin": [cx, cy, cz],
        "rotation": [rot_x, rot_y, rot_z],
        "uuid": b_uid,
        "export": True,
        "isOpen": True,
        "children": [eid],
    }
    outliner_bones.append(bone)
    bone_uuids[f"spark_{i+1}"] = b_uid

print(f"Elements: {len(elements)}, Bones: {len(outliner_bones)}")
assert len(elements) >= 80, f"Need 80+ elements, got {len(elements)}"
assert len(outliner_bones) >= 55, f"Need 55+ bones, got {len(outliner_bones)}"

# ======================================================================
# ANIMATIONS
# ======================================================================
all_bone_names = list(bone_uuids.keys())

def kf(t, x, y, z, channel, interp="catmullrom"):
    return {
        "uuid": uid(),
        "time": t,
        "color": -1,
        "interpolation": interp,
        "data_points": [{"x": str(x), "y": str(y), "z": str(z)}],
        "channel": channel,
    }

def pos_kf(t, x, y, z): return kf(t, x, y, z, "position")
def rot_kf(t, x, y, z): return kf(t, x, y, z, "rotation")
def scale_kf(t, x, y, z): return kf(t, x, y, z, "scale")

# Per-bone unique phases for chaotic idle motion
bone_phases = {}
random.seed(70000)
for bn in all_bone_names:
    bone_phases[bn] = {
        "phase":     random.uniform(0, 2 * math.pi),
        "rate_x":    random.uniform(0.7, 2.4),
        "rate_y":    random.uniform(0.8, 2.6),
        "rate_z":    random.uniform(0.7, 2.4),
        "axis_x":    random.uniform(-1, 1),
        "axis_y":    random.uniform(-1, 1),
        "axis_z":    random.uniform(-1, 1),
        "scale_var": random.uniform(0.8, 1.2),
        "blast_dx":  random.uniform(-1, 1),
        "blast_dy":  random.uniform(-0.4, 0.6),
        "blast_dz":  random.uniform(-1, 1),
    }

# ----------------------------------------------------------------------
# SPAWN — 0.7s
#   - anchor first at 0.0s
#   - ground disruption at 0.2s
#   - edge fragments at 0.3s
#   - floating slabs simultaneous (final scale 0.8..1.2)
# ----------------------------------------------------------------------
def make_animators_spawn():
    animators = {}
    for bn in all_bone_names:
        kfs = []
        bp = bone_phases[bn]
        if bn.startswith("anchor_"):
            kfs += [
                scale_kf(0.0,   0.0, 0.0, 0.0),
                scale_kf(0.05,  0.4, 0.4, 0.4),
                scale_kf(0.15,  1.2, 1.2, 1.2),
                scale_kf(0.25,  0.95,0.95,0.95),
                scale_kf(0.45,  1.02,1.02,1.02),
                scale_kf(0.7,   1.0, 1.0, 1.0),
            ]
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(0.2, 0, 5, 0),
                rot_kf(0.7, 0, 0, 0),
            ]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.7,0,0,0)]
        elif bn.startswith("slab_"):
            final = 0.8 + abs(bp["axis_x"]) * 0.4  # 0.8..1.2
            kfs += [
                scale_kf(0.0,   0.0, 0.0, 0.0),
                scale_kf(0.10,  0.0, 0.0, 0.0),
                scale_kf(0.18,  final*1.4, final*1.4, final*1.4),
                scale_kf(0.35,  final*0.85,final*0.85,final*0.85),
                scale_kf(0.55,  final*1.05,final*1.05,final*1.05),
                scale_kf(0.7,   final, final, final),
            ]
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(0.2, bp["axis_x"]*8, bp["axis_y"]*15, bp["axis_z"]*8),
                rot_kf(0.7, 0, 0, 0),
            ]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.35,0,0.2,0), pos_kf(0.7,0,0,0)]
        elif bn.startswith("ground_"):
            kfs += [
                scale_kf(0.0,   0.0, 0.0, 0.0),
                scale_kf(0.18,  0.0, 0.0, 0.0),
                scale_kf(0.28,  1.4, 1.4, 1.4),
                scale_kf(0.4,   0.85,0.85,0.85),
                scale_kf(0.55,  1.05,1.05,1.05),
                scale_kf(0.7,   1.0, 1.0, 1.0),
            ]
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(0.3, 0, 30, 0),
                rot_kf(0.7, 0, 0, 0),
            ]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.7,0,0,0)]
        elif bn.startswith("edge_"):
            kfs += [
                scale_kf(0.0,   0.0, 0.0, 0.0),
                scale_kf(0.28,  0.0, 0.0, 0.0),
                scale_kf(0.36,  1.6, 1.6, 1.6),
                scale_kf(0.48,  0.8, 0.8, 0.8),
                scale_kf(0.6,   1.05,1.05,1.05),
                scale_kf(0.7,   1.0, 1.0, 1.0),
            ]
            # Blast outward (positions push away from center based on axis values)
            kfs += [
                pos_kf(0.0, 0, 0, 0),
                pos_kf(0.3, -bp["blast_dx"]*1.2, -0.5, -bp["blast_dz"]*1.2),
                pos_kf(0.45, -bp["blast_dx"]*0.4, -0.1, -bp["blast_dz"]*0.4),
                pos_kf(0.7, 0, 0, 0),
            ]
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(0.45, bp["axis_x"]*45, bp["axis_y"]*60, bp["axis_z"]*45),
                rot_kf(0.7, 0, 0, 0),
            ]
        else:  # acc_, spark_
            kfs += [
                scale_kf(0.0,   0.0, 0.0, 0.0),
                scale_kf(0.15,  0.0, 0.0, 0.0),
                scale_kf(0.28,  1.3, 1.3, 1.3),
                scale_kf(0.45,  0.9, 0.9, 0.9),
                scale_kf(0.6,   1.05,1.05,1.05),
                scale_kf(0.7,   1.0, 1.0, 1.0),
            ]
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(0.35, bp["axis_x"]*10, bp["axis_y"]*20, bp["axis_z"]*10),
                rot_kf(0.7, 0, 0, 0),
            ]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.7,0,0,0)]
        animators[bone_uuids[bn]] = {
            "name": bn, "type": "bone", "keyframes": kfs
        }
    return animators

# ----------------------------------------------------------------------
# IDLE — 3.0s, every slab on its own unique axis & rate
# ----------------------------------------------------------------------
def make_animators_idle():
    animators = {}
    for bn in all_bone_names:
        kfs = []
        bp = bone_phases[bn]
        # Build 6 keyframes per channel using bone's unique rates and axes
        rx_rate = bp["rate_x"] * 60   # degrees over 3s
        ry_rate = bp["rate_y"] * 80
        rz_rate = bp["rate_z"] * 60
        ax = bp["axis_x"]
        ay = bp["axis_y"]
        az = bp["axis_z"]

        if bn.startswith("anchor_"):
            # Anchor moves least — gentle pulse
            kfs += [
                rot_kf(0.0,  0, 0, 0),
                rot_kf(0.6,  ax*3, ay*5, az*3),
                rot_kf(1.2,  ax*-2, ay*10, az*-2),
                rot_kf(1.8,  ax*4, ay*15, az*4),
                rot_kf(2.4,  ax*-3, ay*20, az*-3),
                rot_kf(3.0,  0, 0, 0),
            ]
            kfs += [
                scale_kf(0.0, 1, 1, 1),
                scale_kf(0.75,1.02,1.02,1.02),
                scale_kf(1.5, 0.98,0.98,0.98),
                scale_kf(2.25,1.02,1.02,1.02),
                scale_kf(3.0, 1, 1, 1),
            ]
            kfs += [
                pos_kf(0.0,0,0,0),
                pos_kf(1.5,0,0.05,0),
                pos_kf(3.0,0,0,0),
            ]
        elif bn.startswith("slab_"):
            # Each slab rotates on its own unique axis at its own unique rate
            kfs += [
                rot_kf(0.0,  0, 0, 0),
                rot_kf(0.6,  ax*rx_rate*0.2, ay*ry_rate*0.2, az*rz_rate*0.2),
                rot_kf(1.2,  ax*rx_rate*0.4, ay*ry_rate*0.4, az*rz_rate*0.4),
                rot_kf(1.8,  ax*rx_rate*0.6, ay*ry_rate*0.6, az*rz_rate*0.6),
                rot_kf(2.4,  ax*rx_rate*0.8, ay*ry_rate*0.8, az*rz_rate*0.8),
                rot_kf(3.0,  ax*rx_rate,     ay*ry_rate,     az*rz_rate),
            ]
            # Position oscillates slightly in all 3 axes per slab
            kfs += [
                pos_kf(0.0,  0, 0, 0),
                pos_kf(0.6,  ax*0.18,  ay*0.22,  az*0.18),
                pos_kf(1.2,  ax*-0.14, ay*0.30,  az*-0.14),
                pos_kf(1.8,  ax*0.20,  ay*-0.18, az*0.20),
                pos_kf(2.4,  ax*-0.10, ay*0.10,  az*-0.10),
                pos_kf(3.0,  0, 0, 0),
            ]
            kfs += [
                scale_kf(0.0,  1, 1, 1),
                scale_kf(0.75, 1+ax*0.05, 1+ay*0.05, 1+az*0.05),
                scale_kf(1.5,  1-ax*0.05, 1-ay*0.05, 1-az*0.05),
                scale_kf(2.25, 1+ax*0.04, 1+ay*0.04, 1+az*0.04),
                scale_kf(3.0,  1, 1, 1),
            ]
        elif bn.startswith("ground_"):
            kfs += [
                rot_kf(0.0,  0, 0, 0),
                rot_kf(0.6,  0, ay*30, 0),
                rot_kf(1.2,  ax*5, ay*60, az*5),
                rot_kf(1.8,  0, ay*90, 0),
                rot_kf(2.4,  ax*-5, ay*120, az*-5),
                rot_kf(3.0,  0, ay*150, 0),
            ]
            kfs += [
                scale_kf(0.0, 1, 1, 1),
                scale_kf(0.75,1.04,1,1.04),
                scale_kf(1.5, 0.96,1,0.96),
                scale_kf(2.25,1.04,1,1.04),
                scale_kf(3.0, 1, 1, 1),
            ]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(1.5,0,0.04,0), pos_kf(3.0,0,0,0)]
        elif bn.startswith("edge_"):
            kfs += [
                rot_kf(0.0,  0, 0, 0),
                rot_kf(0.6,  ax*60, ay*80, az*60),
                rot_kf(1.2,  ax*120,ay*160,az*120),
                rot_kf(1.8,  ax*180,ay*240,az*180),
                rot_kf(2.4,  ax*240,ay*320,az*240),
                rot_kf(3.0,  ax*300,ay*400,az*300),
            ]
            kfs += [
                pos_kf(0.0, 0, 0, 0),
                pos_kf(0.6, 0, 0.15, 0),
                pos_kf(1.2, 0, -0.12, 0),
                pos_kf(1.8, 0, 0.10, 0),
                pos_kf(2.4, 0, -0.08, 0),
                pos_kf(3.0, 0, 0, 0),
            ]
            kfs += [
                scale_kf(0.0, 1, 1, 1),
                scale_kf(1.5, 1.05,1.05,1.05),
                scale_kf(3.0, 1, 1, 1),
            ]
        else:  # acc_, spark_
            kfs += [
                rot_kf(0.0,  0, 0, 0),
                rot_kf(0.6,  ax*20, ay*30, az*20),
                rot_kf(1.2,  ax*-10, ay*60, az*-10),
                rot_kf(1.8,  ax*15, ay*90, az*15),
                rot_kf(2.4,  ax*-5, ay*120, az*-5),
                rot_kf(3.0,  0, ay*150, 0),
            ]
            kfs += [
                pos_kf(0.0, 0, 0, 0),
                pos_kf(0.75, ax*0.1, ay*0.15, az*0.1),
                pos_kf(1.5, ax*-0.1, ay*0.05, az*-0.1),
                pos_kf(2.25, ax*0.08, ay*-0.10, az*0.08),
                pos_kf(3.0, 0, 0, 0),
            ]
            kfs += [
                scale_kf(0.0, 1, 1, 1),
                scale_kf(1.5, 1.06,1.06,1.06),
                scale_kf(3.0, 1, 1, 1),
            ]
        animators[bone_uuids[bn]] = {
            "name": bn, "type": "bone", "keyframes": kfs
        }
    return animators

# ----------------------------------------------------------------------
# DISSIPATE — 0.4s
#   - all slabs fly outward, scale to 0 with rapid stagger
#   - ground disruption slides inward and snaps to 0
#   - anchor scales to 0 LAST
# ----------------------------------------------------------------------
def make_animators_dissipate():
    animators = {}
    for bn in all_bone_names:
        kfs = []
        bp = bone_phases[bn]
        ax = bp["axis_x"]
        ay = bp["axis_y"]
        az = bp["axis_z"]
        if bn.startswith("anchor_"):
            kfs += [
                scale_kf(0.0,  1.0, 1.0, 1.0),
                scale_kf(0.1,  1.05,1.05,1.05),
                scale_kf(0.2,  1.1, 1.1, 1.1),
                scale_kf(0.3,  1.0, 1.0, 1.0),  # held
                scale_kf(0.36, 0.5, 0.5, 0.5),
                scale_kf(0.4,  0.0, 0.0, 0.0),
            ]
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(0.2, 0, 30, 0),
                rot_kf(0.4, 0, 90, 0),
            ]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.4,0,0,0)]
        elif bn.startswith("slab_") or bn.startswith("edge_") or bn.startswith("spark_"):
            # Fly outward and scale to 0 with stagger by phase
            kfs += [
                pos_kf(0.0,  0, 0, 0),
                pos_kf(0.1,  ax*0.6, ay*0.4, az*0.6),
                pos_kf(0.2,  ax*1.5, ay*1.0, az*1.5),
                pos_kf(0.3,  ax*3.0, ay*1.8, az*3.0),
                pos_kf(0.35, ax*4.5, ay*2.4, az*4.5),
                pos_kf(0.4,  ax*6.0, ay*3.0, az*6.0),
            ]
            kfs += [
                scale_kf(0.0,  1.0, 1.0, 1.0),
                scale_kf(0.1,  1.2, 1.2, 1.2),
                scale_kf(0.2,  1.0, 1.0, 1.0),
                scale_kf(0.28, 0.6, 0.6, 0.6),
                scale_kf(0.35, 0.2, 0.2, 0.2),
                scale_kf(0.4,  0.0, 0.0, 0.0),
            ]
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(0.2, ax*60, ay*90, az*60),
                rot_kf(0.4, ax*180, ay*240, az*180),
            ]
        elif bn.startswith("ground_"):
            # Slides inward and snaps
            kfs += [
                pos_kf(0.0,  0, 0, 0),
                pos_kf(0.1,  ax*-0.6, 0, az*-0.6),
                pos_kf(0.2,  ax*-1.4, 0, az*-1.4),
                pos_kf(0.3,  ax*-2.4, 0, az*-2.4),
                pos_kf(0.36, ax*-3.0, 0, az*-3.0),
                pos_kf(0.4,  ax*-3.0, 0, az*-3.0),
            ]
            kfs += [
                scale_kf(0.0,  1, 1, 1),
                scale_kf(0.15, 0.95,1,0.95),
                scale_kf(0.28, 0.8, 1, 0.8),
                scale_kf(0.34, 0.5, 1, 0.5),
                scale_kf(0.38, 0.2, 1, 0.2),
                scale_kf(0.4,  0, 0, 0),
            ]
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(0.2, 0, 90, 0),
                rot_kf(0.4, 0, 180, 0),
            ]
        else:  # acc_
            kfs += [
                pos_kf(0.0,  0, 0, 0),
                pos_kf(0.1,  ax*0.4, ay*0.3, az*0.4),
                pos_kf(0.2,  ax*1.0, ay*0.6, az*1.0),
                pos_kf(0.3,  ax*2.0, ay*1.0, az*2.0),
                pos_kf(0.35, ax*3.0, ay*1.4, az*3.0),
                pos_kf(0.4,  ax*4.0, ay*1.8, az*4.0),
            ]
            kfs += [
                scale_kf(0.0,  1.0, 1.0, 1.0),
                scale_kf(0.1,  1.2, 1.2, 1.2),
                scale_kf(0.2,  0.9, 0.9, 0.9),
                scale_kf(0.3,  0.5, 0.5, 0.5),
                scale_kf(0.35, 0.2, 0.2, 0.2),
                scale_kf(0.4,  0.0, 0.0, 0.0),
            ]
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(0.2, ax*60, ay*90, az*60),
                rot_kf(0.4, ax*180, ay*180, az*180),
            ]
        animators[bone_uuids[bn]] = {
            "name": bn, "type": "bone", "keyframes": kfs
        }
    return animators

animations = [
    {
        "uuid": uid(),
        "name": "animation.nightmare_static_field.spawn",
        "loop": "once",
        "length": 0.7,
        "snapping": 24,
        "selected": False,
        "saved": False,
        "animators": make_animators_spawn(),
    },
    {
        "uuid": uid(),
        "name": "animation.nightmare_static_field.idle",
        "loop": "loop",
        "length": 3.0,
        "snapping": 24,
        "selected": False,
        "saved": False,
        "animators": make_animators_idle(),
    },
    {
        "uuid": uid(),
        "name": "animation.nightmare_static_field.dissipate",
        "loop": "once",
        "length": 0.4,
        "snapping": 24,
        "selected": False,
        "saved": False,
        "animators": make_animators_dissipate(),
    },
]

textures = [
    {
        "id": "0", "name": "nightmare_static_field_tex",
        "relative_path": "../textures/nightmare_static_field_tex.png",
        "folder": "devilsdream", "namespace": "", "visible": True,
        "mode": "bitmap", "saved": False, "uuid": uid(),
        "source": f"data:image/png;base64,{TEX0_B64}",
    },
    {
        "id": "1", "name": "nightmare_static_field_tex1",
        "relative_path": "../textures/nightmare_static_field_tex1.png",
        "folder": "devilsdream", "namespace": "", "visible": True,
        "mode": "bitmap", "saved": False, "uuid": uid(),
        "source": f"data:image/png;base64,{TEX1_B64}",
    },
]

bbmodel = {
    "meta": {"format_version": "4.10", "model_format": "free", "box_uv": False},
    "name": "nightmare_static_field",
    "geometry": "geometry.nightmare_static_field",
    "resolution": {"width": 64, "height": 64},
    "elements": elements,
    "outliner": outliner_bones,
    "textures": textures,
    "animations": animations,
}

out_path = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_static_field.bbmodel"
with open(out_path, "w") as f:
    json.dump(bbmodel, f, separators=(',', ':'))

size = os.path.getsize(out_path)
print(f"Wrote {out_path} : {size} bytes")
print(f"  elements={len(elements)} bones={len(outliner_bones)}")
print(f"  animations: {[a['name'] for a in animations]}")
assert size >= 300_000, f"File too small: {size} < 300000"
