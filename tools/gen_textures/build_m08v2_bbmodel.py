"""
MODEL 8 — Infernal Crown Burst
Geometry plan (crown silhouette MUST be visible at Y=10):

  - 9 spike chains: each 3 tapered segments (parent + 2 segments grouped under
    a parent bone). 9 spikes × (1 parent + 3 segment elements) = 9 bones with
    3 sub-elements each. Heights: 8,12,9,14,10,13,9,11,8.        => 9 bones, 27 elements
  - 9 band segments connecting spike bases (Y≈10, ring radius ~4) => 9 bones, 9 elements
  - 9 ornaments between spikes: 4 diamonds + 5 inward teeth alt   => 9 bones, 9 elements
  - 12 inner fire ring slabs at Y=9.5 (texture 1)                 => 12 bones, 12 elements
  - 12 detonation shards inside the crown (orbit on idle, blast on dissipate)
                                                                  => 12 bones, 12 elements
  - 14 ground shockwave slabs at Y=0 (texture 1)                  => 14 bones, 14 elements

  Total bones: 9+9+9+12+12+14 = 65 (≥ 55) ✓
  Total elements: 27+9+9+12+12+14 = 83 (≥ 80) ✓
"""

import json, math, uuid, os

TEX0_B64 = open("D:/CC/ChaosCraft/tools/gen_textures/m08v2_b64.txt").read()
TEX1_B64 = open("D:/CC/ChaosCraft/tools/gen_textures/m08v2_tex1_b64.txt").read()

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

CROWN_Y = 10.0    # head height for crown band
CROWN_R = 4.0     # ring radius for spikes/band
SPIKE_HEIGHTS = [8, 12, 9, 14, 10, 13, 9, 11, 8]  # exact spec values

# ======================================================================
# 1) SPIKE CHAINS — 9 spikes around the ring at Y=10
#    Each spike is 3 tapered segments stacked vertically.
#    Asymmetric heights from spec.
# ======================================================================
for i in range(9):
    angle = (i / 9.0) * 360.0
    rad = math.radians(angle)
    cx = CROWN_R * math.cos(rad)
    cz = CROWN_R * math.sin(rad)
    H_total = SPIKE_HEIGHTS[i]

    # Three tapered segments stacked from CROWN_Y upward
    seg_h = H_total / 3.0
    # Each segment: thicker at base, thinner at tip
    base_w = 0.95
    mid_w = 0.65
    tip_w = 0.30

    seg_eids = []

    # Bottom segment (widest)
    seg_eids.append(add_elem(
        f"spike_{i+1}_seg1",
        [cx - base_w/2, CROWN_Y, cz - base_w/2],
        [cx + base_w/2, CROWN_Y + seg_h, cz + base_w/2],
        tex=0,
    ))
    # Middle segment
    seg_eids.append(add_elem(
        f"spike_{i+1}_seg2",
        [cx - mid_w/2, CROWN_Y + seg_h, cz - mid_w/2],
        [cx + mid_w/2, CROWN_Y + 2*seg_h, cz + mid_w/2],
        tex=0,
    ))
    # Top segment (tip)
    seg_eids.append(add_elem(
        f"spike_{i+1}_seg3",
        [cx - tip_w/2, CROWN_Y + 2*seg_h, cz - tip_w/2],
        [cx + tip_w/2, CROWN_Y + H_total, cz + tip_w/2],
        tex=0,
    ))

    b_uid = uid()
    bone = {
        "name": f"spike_{i+1}",
        "origin": [cx, CROWN_Y, cz],
        "rotation": [0, 0, 0],
        "uuid": b_uid,
        "export": True,
        "isOpen": True,
        "children": seg_eids,
    }
    outliner_bones.append(bone)
    bone_uuids[f"spike_{i+1}"] = b_uid

# ======================================================================
# 2) CROWN BAND — 9 connecting slab segments linking spike bases
#    Each band segment is a chord between two adjacent spike bases.
# ======================================================================
for i in range(9):
    a1 = (i / 9.0) * 360.0
    a2 = ((i+1) / 9.0) * 360.0
    a_mid = (a1 + a2) / 2.0
    rad_mid = math.radians(a_mid)
    cx = CROWN_R * math.cos(rad_mid)
    cz = CROWN_R * math.sin(rad_mid)
    # Chord length between adjacent spikes
    rad1 = math.radians(a1)
    rad2 = math.radians(a2)
    p1 = (CROWN_R * math.cos(rad1), CROWN_R * math.sin(rad1))
    p2 = (CROWN_R * math.cos(rad2), CROWN_R * math.sin(rad2))
    chord_len = math.sqrt((p2[0]-p1[0])**2 + (p2[1]-p1[1])**2)
    # Slab oriented tangent to ring; we use a wide-X thin-Z slab and rotate Y
    # by (a_mid + 90) so its long axis runs tangent to the ring.
    half_w = chord_len / 2.0 + 0.2
    eid = add_elem(
        f"band_{i+1}_slab",
        [cx - half_w, CROWN_Y - 0.7, cz - 0.25],
        [cx + half_w, CROWN_Y + 0.7, cz + 0.25],
        tex=0,
    )
    b_uid = uid()
    bone = {
        "name": f"band_{i+1}",
        "origin": [cx, CROWN_Y, cz],
        "rotation": [0, a_mid + 90, 0],
        "uuid": b_uid,
        "export": True,
        "isOpen": True,
        "children": [eid],
    }
    outliner_bones.append(bone)
    bone_uuids[f"band_{i+1}"] = b_uid

# ======================================================================
# 3) ORNAMENTS — 9 between spikes, alternating diamond / inward tooth
# ======================================================================
for i in range(9):
    a1 = (i / 9.0) * 360.0
    a2 = ((i+1) / 9.0) * 360.0
    a_mid = (a1 + a2) / 2.0
    rad_mid = math.radians(a_mid)
    if i % 2 == 0:
        # Diamond ornament (small flat diamond at band level)
        cx = CROWN_R * 1.10 * math.cos(rad_mid)
        cz = CROWN_R * 1.10 * math.sin(rad_mid)
        eid = add_elem(
            f"orn_dia_{i+1}",
            [cx - 0.45, CROWN_Y - 0.10, cz - 0.45],
            [cx + 0.45, CROWN_Y + 0.10, cz + 0.45],
            tex=0,
        )
        b_uid = uid()
        bone = {
            "name": f"orn_{i+1}",
            "origin": [cx, CROWN_Y, cz],
            "rotation": [0, a_mid, 45],  # 45 deg tilt to look diamond-ish
            "uuid": b_uid,
            "export": True,
            "isOpen": True,
            "children": [eid],
        }
    else:
        # Inward-pointing tooth (spike pointing toward center)
        cx = CROWN_R * 0.85 * math.cos(rad_mid)
        cz = CROWN_R * 0.85 * math.sin(rad_mid)
        # Tooth: longer along X, points inward (will rotate to face center)
        eid = add_elem(
            f"orn_tooth_{i+1}",
            [cx - 0.7, CROWN_Y - 0.15, cz - 0.18],
            [cx + 0.5, CROWN_Y + 0.15, cz + 0.18],
            tex=0,
        )
        b_uid = uid()
        bone = {
            "name": f"orn_{i+1}",
            "origin": [cx, CROWN_Y, cz],
            "rotation": [0, a_mid + 180, 0],  # face inward
            "uuid": b_uid,
            "export": True,
            "isOpen": True,
            "children": [eid],
        }
    outliner_bones.append(bone)
    bone_uuids[f"orn_{i+1}"] = b_uid

# ======================================================================
# 4) INNER FIRE RING — 12 flat horizontal slabs at Y=9.5, texture 1
# ======================================================================
INNER_FIRE_R = 2.6
for i in range(12):
    angle = (i / 12.0) * 360.0
    rad = math.radians(angle)
    cx = INNER_FIRE_R * math.cos(rad)
    cz = INNER_FIRE_R * math.sin(rad)
    eid = add_elem(
        f"fire_{i+1}",
        [cx - 0.55, 9.45, cz - 0.25],
        [cx + 0.55, 9.55, cz + 0.25],
        tex=1,
    )
    b_uid = uid()
    bone = {
        "name": f"fire_{i+1}",
        "origin": [cx, 9.5, cz],
        "rotation": [0, angle, 0],
        "uuid": b_uid,
        "export": True,
        "isOpen": True,
        "children": [eid],
    }
    outliner_bones.append(bone)
    bone_uuids[f"fire_{i+1}"] = b_uid

# ======================================================================
# 5) DETONATION SHARDS — 12 flat shard pieces inside the crown.
#    These orbit slowly during idle and blast outward on dissipate.
# ======================================================================
import random
random.seed(8500)

SHARD_R = 1.8
for i in range(12):
    angle = (i / 12.0) * 360.0 + random.uniform(-8, 8)
    rad = math.radians(angle)
    cx = SHARD_R * math.cos(rad)
    cz = SHARD_R * math.sin(rad)
    cy = 9.0 + random.uniform(0.3, 2.0)
    rx = random.uniform(-30, 30)
    ry = angle + random.uniform(-15, 15)
    rz = random.uniform(-30, 30)
    eid = add_elem(
        f"shard_{i+1}",
        [cx - 0.4, cy - 0.05, cz - 0.4],
        [cx + 0.4, cy + 0.05, cz + 0.4],
        tex=0,
    )
    b_uid = uid()
    bone = {
        "name": f"shard_{i+1}",
        "origin": [cx, cy, cz],
        "rotation": [rx, ry, rz],
        "uuid": b_uid,
        "export": True,
        "isOpen": True,
        "children": [eid],
    }
    outliner_bones.append(bone)
    bone_uuids[f"shard_{i+1}"] = b_uid

# ======================================================================
# 6) GROUND SHOCKWAVE — 14 flat slabs at Y=0, texture 1
# ======================================================================
GROUND_R = 5.5
for i in range(14):
    angle = (i / 14.0) * 360.0
    rad = math.radians(angle)
    cx = GROUND_R * math.cos(rad)
    cz = GROUND_R * math.sin(rad)
    eid = add_elem(
        f"shockwave_{i+1}",
        [cx - 0.85, 0.02, cz - 0.4],
        [cx + 0.85, 0.12, cz + 0.4],
        tex=1,
    )
    b_uid = uid()
    bone = {
        "name": f"shockwave_{i+1}",
        "origin": [cx, 0.05, cz],
        "rotation": [0, angle, 0],
        "uuid": b_uid,
        "export": True,
        "isOpen": True,
        "children": [eid],
    }
    outliner_bones.append(bone)
    bone_uuids[f"shockwave_{i+1}"] = b_uid

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

def bone_index(name, prefix):
    """Extract numeric suffix of a bone like 'spike_5' -> 5 (1-based)."""
    return int(name.split("_")[-1])

# Pre-compute outward unit vectors for each shard from its origin (used in
# dissipate to blast outward from crown center).
shard_outward = {}
for bn, b_uid in bone_uuids.items():
    if bn.startswith("shard_"):
        # Find the bone's origin
        for ob in outliner_bones:
            if ob["uuid"] == b_uid:
                ox, oy, oz = ob["origin"]
                d = math.sqrt(ox*ox + oz*oz)
                if d < 1e-6:
                    shard_outward[bn] = (1, 0, 0)
                else:
                    shard_outward[bn] = (ox/d, 0, oz/d)
                break

# ----------------------------------------------------------------------
# SPAWN — 1.1s
#   - Band segments materialise clockwise 0.06s stagger
#   - Spikes extend upward bottom-to-top as band reaches them, 0.04s seg stagger
#   - Ornaments snap outward after their band section
#   - Inner fire ring at 0.7s
#   - Detonation shards at 0.8s
#   - Ground shockwave at 0.6s
# ----------------------------------------------------------------------
def make_animators_spawn():
    animators = {}
    for bn in all_bone_names:
        kfs = []
        if bn.startswith("band_"):
            idx = bone_index(bn, "band_") - 1     # 0..8
            t0 = idx * 0.06                       # CW stagger
            kfs += [
                scale_kf(0.0,    0, 0, 0),
                scale_kf(t0,     0, 0, 0),
                scale_kf(t0+0.04,0.5,1.4,0.5),
                scale_kf(t0+0.10,1.1,0.95,1.1),
                scale_kf(t0+0.18,0.95,1.05,0.95),
                scale_kf(1.1,    1, 1, 1),
            ]
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(t0+0.05, 0, 0, 8),
                rot_kf(1.1, 0, 0, 0),
            ]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(1.1,0,0,0)]
        elif bn.startswith("spike_"):
            idx = bone_index(bn, "spike_") - 1
            t_band = idx * 0.06 + 0.10            # band reaches → spike starts
            kfs += [
                scale_kf(0.0,        0, 0, 0),
                scale_kf(t_band,     0, 0, 0),
                scale_kf(t_band+0.06,0.4, 0.2, 0.4),
                scale_kf(t_band+0.14,0.9, 0.8, 0.9),
                scale_kf(t_band+0.22,1.05,1.05,1.05),
                scale_kf(1.1,        1, 1, 1),
            ]
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(t_band+0.05, 4, 0, 4),
                rot_kf(t_band+0.20, -2, 0, -2),
                rot_kf(1.1, 0, 0, 0),
            ]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(1.1,0,0,0)]
        elif bn.startswith("orn_"):
            idx = bone_index(bn, "orn_") - 1
            t0 = idx * 0.06 + 0.16
            kfs += [
                scale_kf(0.0,      0, 0, 0),
                scale_kf(t0,       0, 0, 0),
                scale_kf(t0+0.04,  1.5, 1.5, 1.5),
                scale_kf(t0+0.12,  0.9, 0.9, 0.9),
                scale_kf(t0+0.22,  1.05,1.05,1.05),
                scale_kf(1.1,      1, 1, 1),
            ]
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(t0+0.06, 0, 30, 0),
                rot_kf(1.1, 0, 0, 0),
            ]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(1.1,0,0,0)]
        elif bn.startswith("fire_"):
            kfs += [
                scale_kf(0.0,  0, 0, 0),
                scale_kf(0.65, 0, 0, 0),
                scale_kf(0.72, 1.5, 1.5, 1.5),
                scale_kf(0.85, 0.85,0.85,0.85),
                scale_kf(0.98, 1.05,1.05,1.05),
                scale_kf(1.1,  1, 1, 1),
            ]
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(0.7, 0, 30, 0),
                rot_kf(1.1, 0, 0, 0),
            ]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(1.1,0,0,0)]
        elif bn.startswith("shard_"):
            kfs += [
                scale_kf(0.0,  0, 0, 0),
                scale_kf(0.78, 0, 0, 0),
                scale_kf(0.85, 1.5, 1.5, 1.5),
                scale_kf(0.95, 0.85,0.85,0.85),
                scale_kf(1.05, 1.05,1.05,1.05),
                scale_kf(1.1,  1, 1, 1),
            ]
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(0.85, 30, 60, 30),
                rot_kf(1.1, 0, 0, 0),
            ]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(1.1,0,0,0)]
        elif bn.startswith("shockwave_"):
            idx = bone_index(bn, "shockwave_") - 1
            t0 = 0.55 + idx * 0.012
            kfs += [
                scale_kf(0.0,    0.0, 1.0, 0.0),
                scale_kf(t0,     0.0, 1.0, 0.0),
                scale_kf(t0+0.05,0.6, 1.0, 0.6),
                scale_kf(t0+0.12,1.15,1.0, 1.15),
                scale_kf(t0+0.22,0.95,1.0, 0.95),
                scale_kf(1.1,    1.0, 1.0, 1.0),
            ]
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(t0+0.1, 0, 10, 0),
                rot_kf(1.1, 0, 0, 0),
            ]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(1.1,0,0,0)]
        animators[bone_uuids[bn]] = {
            "name": bn, "type": "bone", "keyframes": kfs
        }
    return animators

# ----------------------------------------------------------------------
# IDLE — 5.0s, crown rotates Y, spikes pulse asymmetrically, band pulses
# emissive (we approximate via subtle scale), inner ring counter-rotates,
# shards orbit slowly inside.
# ----------------------------------------------------------------------
def make_animators_idle():
    animators = {}
    for bn in all_bone_names:
        kfs = []
        if bn.startswith("spike_"):
            idx = bone_index(bn, "spike_") - 1
            # Phase offset per spike for asymmetric oscillation
            phase = idx * 0.55 % 5.0
            # Whole crown rotates Y (rotation accumulates 360 over 5s)
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(1.0, 0, 72, 0),
                rot_kf(2.0, 0, 144, 0),
                rot_kf(3.0, 0, 216, 0),
                rot_kf(4.0, 0, 288, 0),
                rot_kf(5.0, 0, 360, 0),
            ]
            # Scale Y oscillates 1.0..1.05 with phase offset
            t_off = phase
            def s(t): return 1.0 + 0.025 * (1 + math.sin((t/5.0)*2*math.pi + idx*0.7))
            kfs += [
                scale_kf(0.0, 1, s(0.0), 1),
                scale_kf(1.0, 1, s(1.0+t_off), 1),
                scale_kf(2.0, 1, s(2.0+t_off), 1),
                scale_kf(3.0, 1, s(3.0+t_off), 1),
                scale_kf(4.0, 1, s(4.0+t_off), 1),
                scale_kf(5.0, 1, s(5.0+t_off), 1),
            ]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(2.5,0,0.04,0), pos_kf(5.0,0,0,0)]
        elif bn.startswith("band_"):
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(1.0, 0, 72, 0),
                rot_kf(2.0, 0, 144, 0),
                rot_kf(3.0, 0, 216, 0),
                rot_kf(4.0, 0, 288, 0),
                rot_kf(5.0, 0, 360, 0),
            ]
            # Band emissive pulse (approximated via subtle scale pulse)
            kfs += [
                scale_kf(0.0, 1, 1, 1),
                scale_kf(1.0, 1.02, 1.04, 1.02),
                scale_kf(2.0, 1, 1, 1),
                scale_kf(3.0, 1.02, 1.04, 1.02),
                scale_kf(4.0, 1, 1, 1),
                scale_kf(5.0, 1, 1, 1),
            ]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(5.0,0,0,0)]
        elif bn.startswith("orn_"):
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(1.0, 0, 72, 0),
                rot_kf(2.0, 0, 144, 0),
                rot_kf(3.0, 0, 216, 0),
                rot_kf(4.0, 0, 288, 0),
                rot_kf(5.0, 0, 360, 0),
            ]
            kfs += [
                scale_kf(0.0, 1, 1, 1),
                scale_kf(1.25, 1.06, 1.06, 1.06),
                scale_kf(2.5, 1, 1, 1),
                scale_kf(3.75, 1.06, 1.06, 1.06),
                scale_kf(5.0, 1, 1, 1),
            ]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(5.0,0,0,0)]
        elif bn.startswith("fire_"):
            # Inner ring counter-rotates
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(1.0, 0, -90, 0),
                rot_kf(2.0, 0, -180, 0),
                rot_kf(3.0, 0, -270, 0),
                rot_kf(4.0, 0, -360, 0),
                rot_kf(5.0, 0, -450, 0),
            ]
            kfs += [
                scale_kf(0.0, 1, 1, 1),
                scale_kf(1.25, 1.05, 1, 1.05),
                scale_kf(2.5, 0.95, 1, 0.95),
                scale_kf(3.75, 1.05, 1, 1.05),
                scale_kf(5.0, 1, 1, 1),
            ]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(2.5,0,0.06,0), pos_kf(5.0,0,0,0)]
        elif bn.startswith("shard_"):
            idx = bone_index(bn, "shard_") - 1
            # Slow orbit — rotate about Y plus minor wobble
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(1.0, 8, 36, -8),
                rot_kf(2.0, -8, 72, 8),
                rot_kf(3.0, 12, 108, -12),
                rot_kf(4.0, -10, 144, 10),
                rot_kf(5.0, 0, 180, 0),
            ]
            # Slight orbital position drift
            ang_off = idx * (2 * math.pi / 12)
            kfs += [
                pos_kf(0.0, 0, 0, 0),
                pos_kf(1.0, 0.15*math.cos(ang_off+1.0), 0.1, 0.15*math.sin(ang_off+1.0)),
                pos_kf(2.0, 0.15*math.cos(ang_off+2.0), -0.05, 0.15*math.sin(ang_off+2.0)),
                pos_kf(3.0, 0.15*math.cos(ang_off+3.0), 0.1, 0.15*math.sin(ang_off+3.0)),
                pos_kf(4.0, 0.15*math.cos(ang_off+4.0), -0.05, 0.15*math.sin(ang_off+4.0)),
                pos_kf(5.0, 0, 0, 0),
            ]
            kfs += [
                scale_kf(0.0, 1, 1, 1),
                scale_kf(2.5, 1.08, 1.08, 1.08),
                scale_kf(5.0, 1, 1, 1),
            ]
        elif bn.startswith("shockwave_"):
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(1.0, 0, 18, 0),
                rot_kf(2.0, 0, 36, 0),
                rot_kf(3.0, 0, 54, 0),
                rot_kf(4.0, 0, 72, 0),
                rot_kf(5.0, 0, 90, 0),
            ]
            kfs += [
                scale_kf(0.0, 1, 1, 1),
                scale_kf(1.25, 1.04, 1, 1.04),
                scale_kf(2.5, 0.97, 1, 0.97),
                scale_kf(3.75, 1.04, 1, 1.04),
                scale_kf(5.0, 1, 1, 1),
            ]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(5.0,0,0,0)]
        animators[bone_uuids[bn]] = {
            "name": bn, "type": "bone", "keyframes": kfs
        }
    return animators

# ----------------------------------------------------------------------
# DISSIPATE — 0.7s
#   - Detonation shards blast outward FIRST (start at 0.0)
#   - Spikes collapse inward then scale to 0 tallest-first
#   - Band crumbles
#   - Ground shockwave expands and snaps
# ----------------------------------------------------------------------
def make_animators_dissipate():
    animators = {}
    # Spike heights ranking: tallest first → shortest last
    # SPIKE_HEIGHTS index → height. Sort indices by descending height.
    height_order = sorted(range(9), key=lambda i: -SPIKE_HEIGHTS[i])
    spike_collapse_time = {}
    for rank, idx in enumerate(height_order):
        spike_collapse_time[idx] = 0.20 + rank * 0.04  # tallest first

    for bn in all_bone_names:
        kfs = []
        if bn.startswith("shard_"):
            ox, _, oz = shard_outward.get(bn, (1, 0, 0))
            kfs += [
                pos_kf(0.0,  0, 0, 0),
                pos_kf(0.05, ox*1.0, 0.5, oz*1.0),
                pos_kf(0.15, ox*3.0, 1.2, oz*3.0),
                pos_kf(0.30, ox*6.0, 1.8, oz*6.0),
                pos_kf(0.50, ox*9.0, 2.0, oz*9.0),
                pos_kf(0.7,  ox*12.0,1.0, oz*12.0),
            ]
            kfs += [
                scale_kf(0.0, 1, 1, 1),
                scale_kf(0.05, 1.5, 1.5, 1.5),
                scale_kf(0.15, 1.3, 1.3, 1.3),
                scale_kf(0.30, 0.8, 0.8, 0.8),
                scale_kf(0.50, 0.3, 0.3, 0.3),
                scale_kf(0.7,  0, 0, 0),
            ]
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(0.3, 90, 180, 90),
                rot_kf(0.7, 360, 540, 360),
            ]
        elif bn.startswith("spike_"):
            idx = bone_index(bn, "spike_") - 1
            tc = spike_collapse_time[idx]
            kfs += [
                scale_kf(0.0,        1, 1, 1),
                scale_kf(tc-0.08,    0.98, 1.0, 0.98),
                scale_kf(tc-0.02,    0.7, 1.05, 0.7),  # collapse inward
                scale_kf(tc+0.02,    0.4, 0.4, 0.4),
                scale_kf(tc+0.10,    0.1, 0.1, 0.1),
                scale_kf(tc+0.18,    0, 0, 0),
            ]
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(tc, 5, 0, 5),
                rot_kf(0.7, 30, 30, 30),
            ]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.7,0,0,0)]
        elif bn.startswith("band_"):
            idx = bone_index(bn, "band_") - 1
            t0 = 0.30 + idx * 0.025
            kfs += [
                scale_kf(0.0,      1, 1, 1),
                scale_kf(t0,       1.05, 1.05, 1.05),
                scale_kf(t0+0.05,  0.7, 0.7, 0.7),
                scale_kf(t0+0.12,  0.4, 0.4, 0.4),
                scale_kf(t0+0.18,  0.1, 0.1, 0.1),
                scale_kf(0.7,      0, 0, 0),
            ]
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(t0+0.05, 0, 0, 20),
                rot_kf(0.7, 0, 30, 60),
            ]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.7,0,-1.0,0)]
        elif bn.startswith("orn_"):
            idx = bone_index(bn, "orn_") - 1
            t0 = 0.30 + idx * 0.025
            kfs += [
                scale_kf(0.0,      1, 1, 1),
                scale_kf(t0,       1.1, 1.1, 1.1),
                scale_kf(t0+0.05,  0.6, 0.6, 0.6),
                scale_kf(t0+0.12,  0.3, 0.3, 0.3),
                scale_kf(t0+0.18,  0.1, 0.1, 0.1),
                scale_kf(0.7,      0, 0, 0),
            ]
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(t0+0.05, 30, 60, 30),
                rot_kf(0.7, 90, 180, 90),
            ]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.7,0,-0.6,0)]
        elif bn.startswith("fire_"):
            kfs += [
                scale_kf(0.0,  1, 1, 1),
                scale_kf(0.10, 1.2, 1, 1.2),
                scale_kf(0.20, 1.5, 0.6, 1.5),
                scale_kf(0.35, 1.0, 0.3, 1.0),
                scale_kf(0.50, 0.4, 0.1, 0.4),
                scale_kf(0.7,  0, 0, 0),
            ]
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(0.35, 0, -180, 0),
                rot_kf(0.7, 0, -360, 0),
            ]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.7,0,0,0)]
        elif bn.startswith("shockwave_"):
            kfs += [
                scale_kf(0.0,  1, 1, 1),
                scale_kf(0.10, 1.4, 1, 1.4),
                scale_kf(0.25, 2.5, 1, 2.5),
                scale_kf(0.40, 4.0, 1, 4.0),
                scale_kf(0.55, 5.0, 1, 5.0),
                scale_kf(0.7,  0, 0, 0),
            ]
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(0.35, 0, 30, 0),
                rot_kf(0.7, 0, 60, 0),
            ]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.7,0,0,0)]
        animators[bone_uuids[bn]] = {
            "name": bn, "type": "bone", "keyframes": kfs
        }
    return animators

animations = [
    {
        "uuid": uid(),
        "name": "animation.infernal_crown_burst.spawn",
        "loop": "once",
        "length": 1.1,
        "snapping": 24,
        "selected": False,
        "saved": False,
        "animators": make_animators_spawn(),
    },
    {
        "uuid": uid(),
        "name": "animation.infernal_crown_burst.idle",
        "loop": "loop",
        "length": 5.0,
        "snapping": 24,
        "selected": False,
        "saved": False,
        "animators": make_animators_idle(),
    },
    {
        "uuid": uid(),
        "name": "animation.infernal_crown_burst.dissipate",
        "loop": "once",
        "length": 0.7,
        "snapping": 24,
        "selected": False,
        "saved": False,
        "animators": make_animators_dissipate(),
    },
]

textures = [
    {
        "id": "0", "name": "infernal_crown_burst_tex",
        "relative_path": "../textures/infernal_crown_burst_tex.png",
        "folder": "devilsdream", "namespace": "", "visible": True,
        "mode": "bitmap", "saved": False, "uuid": uid(),
        "source": f"data:image/png;base64,{TEX0_B64}",
    },
    {
        "id": "1", "name": "infernal_crown_burst_tex1",
        "relative_path": "../textures/infernal_crown_burst_tex1.png",
        "folder": "devilsdream", "namespace": "", "visible": True,
        "mode": "bitmap", "saved": False, "uuid": uid(),
        "source": f"data:image/png;base64,{TEX1_B64}",
    },
]

bbmodel = {
    "meta": {"format_version": "4.10", "model_format": "free", "box_uv": False},
    "name": "infernal_crown_burst",
    "geometry": "geometry.infernal_crown_burst",
    "resolution": {"width": 64, "height": 64},
    "elements": elements,
    "outliner": outliner_bones,
    "textures": textures,
    "animations": animations,
}

out_path = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/infernal_crown_burst.bbmodel"
with open(out_path, "w") as f:
    json.dump(bbmodel, f, separators=(',', ':'))

size = os.path.getsize(out_path)
print(f"Wrote {out_path} : {size} bytes")
print(f"  elements={len(elements)} bones={len(outliner_bones)}")
print(f"  animations: {[a['name'] for a in animations]}")
assert size >= 300_000, f"File too small: {size} < 300000"
