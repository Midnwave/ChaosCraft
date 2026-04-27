"""
Build fallen_angel_wings_summon.bbmodel
Two enormous angel wings unfurling from a central spine + corruption drips + shadow disc.
Target: 80+ elements, 55+ bones, 3 animations, 6+ keyframes per bone per animation, >=300KB.
Uniqueness anchor: from front view, two wing silhouettes spreading sideways from
central spine. Left wing extends to negative X, right wing to positive X. Each wing has
5 primary feathers (longest, outer) + 3 secondaries (mid) + 2 covert layers (inner).
"""
import json, math, os

b64_0 = open("D:/CC/ChaosCraft/tools/gen_textures/m15_b64.txt").read().strip()
b64_1 = open("D:/CC/ChaosCraft/tools/gen_textures/m15_tex1_b64.txt").read().strip()

def eid(n): return f"e{n:03d}"
def bid(n): return f"b{n:03d}"
def aid(n): return f"a{n:03d}"
def kid(n): return f"k{n:04d}"
def tid(n): return f"t{n:03d}"

UV0 = {"north":{"uv":[2,2,30,30],"texture":0},"east":{"uv":[34,2,62,30],"texture":0},
       "south":{"uv":[2,2,30,30],"texture":0},"west":{"uv":[34,2,62,30],"texture":0},
       "up":{"uv":[2,34,30,62],"texture":0},"down":{"uv":[34,34,62,62],"texture":0}}
UV1 = {"north":{"uv":[34,34,62,62],"texture":1},"east":{"uv":[34,34,62,62],"texture":1},
       "south":{"uv":[34,34,62,62],"texture":1},"west":{"uv":[34,34,62,62],"texture":1},
       "up":{"uv":[34,34,62,62],"texture":1},"down":{"uv":[34,34,62,62],"texture":1}}

def elem(idx, name, fr, to, uv):
    return {"name":name,"box_uv":False,"rescale":False,"locked":False,
            "render_order":"default","allow_mirror_modeling":True,
            "from":fr,"to":to,"autouv":0,"color":0,"uuid":eid(idx),"faces":uv}

def bone(idx, name, origin, children, rot=None):
    o = rot if rot else [0,0,0]
    return {"name":name,"origin":origin,"rotation":o,"uuid":bid(idx),
            "export":True,"isOpen":True,"children":children}

k_counter = [1]
def kf(time, x, y, z, channel="position", interp="catmullrom"):
    uid = kid(k_counter[0]); k_counter[0]+=1
    return {"uuid":uid,"time":time,"color":-1,"interpolation":interp,
            "data_points":[{"x":str(x),"y":str(y),"z":str(z)}],"channel":channel}

elements = []
bones = []
ei = 1
bi = 1

# ── 1. CENTRAL SPINE (4 cubes / 4 bones) ─────────────────────────────────────
# Vertical column from Y=-2 to Y=10, X=0, Z=0
spine_defs = [
    ("spine_1", [-0.6, -2.0, -0.6], [0.6, 1.0, 0.6]),
    ("spine_2", [-0.5,  1.0, -0.5], [0.5, 4.0, 0.5]),
    ("spine_3", [-0.5,  4.0, -0.5], [0.5, 7.0, 0.5]),
    ("spine_4", [-0.4,  7.0, -0.4], [0.4, 10.0, 0.4]),
]
spine_bids = []
for name, fr, to in spine_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [0, fr[1], 0], [eid(ei)]); bones.append(b)
    spine_bids.append(bid(bi)); ei+=1; bi+=1

# ── 2. WING ROOTS (2 bones, 2 elements) ──────────────────────────────────────
# A bone-attached anchor on each side of the spine where wings emerge
wroot_bids = []
for side, sx in [("L", -1), ("R", 1)]:
    name = f"wing_root_{side}"
    fr = [sx*1.0 - 0.5, 3.5, -0.5] if sx<0 else [sx*1.0 - 0.5, 3.5, -0.5]
    fr = [sx*1.0 - 0.4, 3.0, -0.4]
    to = [sx*1.0 + 0.4, 6.0, 0.4]
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [sx*1.0, 4.5, 0], [eid(ei)]); bones.append(b)
    wroot_bids.append(bid(bi)); ei+=1; bi+=1

# ── 3. PRIMARY FEATHERS (5 per wing × 2 = 10 bones) ──────────────────────────
# Each primary feather: 3 rachis segments + 2 barb pairs
# Primaries fan outward at angles. Inner primary closest to body, outer primary
# extends farthest. Total per primary: 3 rachis + 4 barbs = 7 elements/feather.
# 10 primary feathers × 7 = 70 elements... too many. We use group bones with
# 1 bone per feather, but the feather contains 3 rachis + 2 barb pairs (4 sub-elements)
# all under that bone. We'll create the bone + child sub-elements and
# track the primary BONE (not children bones).

# To keep ≥55 bones AND ≥80 elements, we'll structure:
#  - Primaries: 10 primary feather bones (one per feather), each containing 5
#    elements (3 rachis + 2 barb cubes) but those elements are attached to that
#    single bone (so multiple elements per bone). That gives 50 elements.
#  - Secondaries: 6 secondary feather bones with 3 elements each (smaller)
#    = 18 elements.
#  - Coverts: 4 covert feather bones with 2 elements each = 8 elements.
#  - Quill base accents on each primary: actually attach as elements to the
#    same primary bone.
# Then: spine 4, wing roots 2, primaries 10, secondaries 6, coverts 4,
# corruption drips 16 (8 per wing, 1 bone each), shadow disc 4 (1 bone each),
# vane edge accents 8 (8 bones). Bones total: 4+2+10+6+4+16+4+8 = 54 — need 55+.
# Add 1 master/structural-feel bone? Better: split coverts into 6 instead of 4.
# Add 2 extra "shoulder pad" bones at wing roots = 56 bones.

primary_bids = []
# Each primary feather: positioned along the wing root, fanning outward.
# Primary index 0 = innermost (closest to spine), index 4 = outermost (tip)
for side_i, side in enumerate([-1, 1]):
    for p in range(5):
        # Angle outward and slightly upward; outer feathers more horizontal/down
        # Inner feathers point upward (covering shoulder), outer point sideways
        base_angle_z = -20 + p * 12  # rotation around Z (outward fan in XY plane)
        # length increases for outer feathers
        feather_len = 5.0 + p * 1.6  # 5.0 to 11.4
        feather_width = 1.4 - p * 0.10  # 1.4 to 1.0

        # Wing root attaches at side*1.0, Y=4.5
        # Anchor point on wing root
        ax = side * 1.2
        ay = 4.0 + p * 0.4   # stacked along the wing root
        az = 0.0

        bone_name = f"primary_{['L','R'][side_i]}_{p+1}"
        # Build 3 rachis segments + 2 barb pairs all as children elements of one bone
        # Rachis 1 (closest to root)
        # We orient the feather along X axis (sideways) — feather "length" is X distance from root
        seg_len = feather_len / 3.0
        rachis_y_thickness = 0.18
        rachis_z_thickness = 0.18
        bone_origin = [ax, ay, az]

        children_eids = []
        # Rachis segments (3)
        for s in range(3):
            x0 = side * (1.2 + s * seg_len)
            x1 = side * (1.2 + (s+1) * seg_len)
            xa, xb = (x0, x1) if x0 < x1 else (x1, x0)
            # rachis taper: thinner toward tip
            taper = 1.0 - s * 0.20
            ry = rachis_y_thickness * taper
            rz = rachis_z_thickness * taper
            elem_name = f"{bone_name}_rachis{s+1}"
            fr = [xa, ay - ry, -rz]
            to = [xb, ay + ry, rz]
            e = elem(ei, elem_name, fr, to, UV0); elements.append(e)
            children_eids.append(eid(ei)); ei += 1

        # Barb pairs (2 pairs at seg1-seg2 junction and seg2-seg3 junction)
        for pair in range(2):
            barb_x_center = side * (1.2 + (pair + 1) * seg_len)
            barb_half = (feather_width - pair * 0.2) / 2.0
            # upper barb (on +Y side of rachis)
            x_b = barb_x_center
            elem_name_u = f"{bone_name}_barb{pair+1}_U"
            # barb is a flat rectangle: along X axis a bit, +Y direction
            fru = [x_b - 0.5, ay + 0.05, -0.06]
            tou = [x_b + 0.5, ay + barb_half, 0.06]
            e = elem(ei, elem_name_u, fru, tou, UV0); elements.append(e)
            children_eids.append(eid(ei)); ei += 1
            # lower barb (on -Y side)
            elem_name_l = f"{bone_name}_barb{pair+1}_L"
            frl = [x_b - 0.5, ay - barb_half, -0.06]
            tol = [x_b + 0.5, ay - 0.05, 0.06]
            e = elem(ei, elem_name_l, frl, tol, UV0); elements.append(e)
            children_eids.append(eid(ei)); ei += 1

        # Bone with rotation oriented to fan
        b = bone(bi, bone_name, bone_origin, children_eids,
                 rot=[0, 0, side * base_angle_z])
        bones.append(b)
        primary_bids.append(bid(bi)); bi += 1

# ── 4. SECONDARY FEATHERS (3 per wing × 2 = 6 bones) ─────────────────────────
secondary_bids = []
for side_i, side in enumerate([-1, 1]):
    for s in range(3):
        bone_name = f"secondary_{['L','R'][side_i]}_{s+1}"
        # secondaries are mid, smaller, behind primaries closer to body
        feather_len = 3.5 - s * 0.4   # smaller than primaries
        ax = side * 0.9
        ay = 4.5 - s * 0.6
        az = 0.4   # a bit behind
        bone_origin = [ax, ay, az]
        children_eids = []
        # 3 short rachis
        seg_len = feather_len / 3.0
        for sg in range(3):
            x0 = side * (0.9 + sg * seg_len)
            x1 = side * (0.9 + (sg+1) * seg_len)
            xa, xb = (x0, x1) if x0 < x1 else (x1, x0)
            ry = 0.14 * (1 - sg*0.25)
            elem_name = f"{bone_name}_r{sg+1}"
            fr = [xa, ay - ry, az - 0.1]
            to = [xb, ay + ry, az + 0.1]
            e = elem(ei, elem_name, fr, to, UV0); elements.append(e)
            children_eids.append(eid(ei)); ei += 1
        b = bone(bi, bone_name, bone_origin, children_eids,
                 rot=[0, 0, side * (-15 + s * 8)])
        bones.append(b)
        secondary_bids.append(bid(bi)); bi += 1

# ── 5. COVERT FEATHERS (3 per wing × 2 = 6 bones) ────────────────────────────
covert_bids = []
for side_i, side in enumerate([-1, 1]):
    for c in range(3):
        bone_name = f"covert_{['L','R'][side_i]}_{c+1}"
        # coverts are smallest, layer over the base of primaries/secondaries
        feather_len = 2.2 - c * 0.3
        ax = side * 0.7
        ay = 5.2 - c * 0.5
        az = 0.7
        bone_origin = [ax, ay, az]
        children_eids = []
        # 2 short slabs per covert
        for sg in range(2):
            x0 = side * (0.7 + sg * (feather_len/2))
            x1 = side * (0.7 + (sg+1) * (feather_len/2))
            xa, xb = (x0, x1) if x0 < x1 else (x1, x0)
            ry = 0.18 * (1 - sg*0.2)
            elem_name = f"{bone_name}_r{sg+1}"
            fr = [xa, ay - ry, az - 0.08]
            to = [xb, ay + ry, az + 0.08]
            e = elem(ei, elem_name, fr, to, UV0); elements.append(e)
            children_eids.append(eid(ei)); ei += 1
        b = bone(bi, bone_name, bone_origin, children_eids,
                 rot=[0, 0, side * (-5 + c * 4)])
        bones.append(b)
        covert_bids.append(bid(bi)); bi += 1

# ── 6. CORRUPTION DRIPS (8 per wing × 2 = 16 bones, 16 elements) ─────────────
# small cubes beneath each wing — purple contamination dripping from feather tips
drip_bids = []
for side_i, side in enumerate([-1, 1]):
    for d in range(8):
        bone_name = f"drip_{['L','R'][side_i]}_{d+1}"
        # Drips below the wing — fanning out beneath primary feather tips
        # spread from inner X (close to body) to outer X (far)
        t = d / 7.0
        dx = side * (2.0 + t * 7.0)
        dy = 2.0 - d * 0.4   # falling down trajectory
        dz = 0.0 + (d % 3) * 0.15
        sz = 0.18 + (d % 3) * 0.04
        fr = [dx - sz, dy - sz, dz - sz]
        to = [dx + sz, dy + sz, dz + sz]
        e = elem(ei, bone_name, fr, to, UV1); elements.append(e)
        b = bone(bi, bone_name, [dx, dy, dz], [eid(ei)]); bones.append(b)
        drip_bids.append(bid(bi)); ei += 1; bi += 1

# ── 7. SHADOW DISC (4 overlapping flat slabs at Y=0, 4 bones) ────────────────
disc_bids = []
disc_defs = [
    ("disc_1", [-5.0, -0.15, -5.0], [5.0, -0.05, 5.0]),
    ("disc_2", [-4.5, -0.10, -4.5], [4.5, -0.02, 4.5]),
    ("disc_3", [-3.8, -0.20, -3.8], [3.8, -0.10, 3.8]),
    ("disc_4", [-2.8, -0.25, -2.8], [2.8, -0.15, 2.8]),
]
for name, fr, to in disc_defs:
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    b = bone(bi, name, [0, fr[1], 0], [eid(ei)]); bones.append(b)
    disc_bids.append(bid(bi)); ei += 1; bi += 1

# ── 8. SHOULDER PADS (2 bones) ───────────────────────────────────────────────
# Bigger anchors at the top of each wing root for visual mass
shoulder_bids = []
for side_i, side in enumerate([-1, 1]):
    bone_name = f"shoulder_{['L','R'][side_i]}"
    fr = [side * 1.6 - 0.3 if side > 0 else side * 1.6 - 0.3,
          5.5, -0.5]
    fr = [side * 1.6 - (0.5 if side > 0 else 0.5), 5.5, -0.5]
    if side < 0:
        fr = [side * 1.6 - 0.5, 5.5, -0.5]
        to = [side * 1.6 + 0.5, 6.5, 0.5]
    else:
        fr = [side * 1.6 - 0.5, 5.5, -0.5]
        to = [side * 1.6 + 0.5, 6.5, 0.5]
    e = elem(ei, bone_name, fr, to, UV0); elements.append(e)
    b = bone(bi, bone_name, [side * 1.6, 6.0, 0], [eid(ei)]); bones.append(b)
    shoulder_bids.append(bid(bi)); ei += 1; bi += 1

# ── 9. CORRUPTION RIBBONS along feather tips (8 bones) ───────────────────────
# extra purple contamination ribbons attached to outer primary tips, tex1
ribbon_bids = []
for side_i, side in enumerate([-1, 1]):
    for r in range(4):
        bone_name = f"ribbon_{['L','R'][side_i]}_{r+1}"
        # Located at the tip of the outer primaries (primary 3,4,5)
        rx = side * (5.5 + r * 0.8)
        ry = 4.5 - r * 0.3
        rz = 0.0
        fr = [rx - 0.20, ry - 0.10, rz - 0.04]
        to = [rx + 0.20, ry + 0.10, rz + 0.04]
        e = elem(ei, bone_name, fr, to, UV1); elements.append(e)
        b = bone(bi, bone_name, [rx, ry, 0], [eid(ei)]); bones.append(b)
        ribbon_bids.append(bid(bi)); ei += 1; bi += 1

print(f"Elements: {len(elements)}, Bones: {len(bones)}")
assert len(elements) >= 80, f"Need 80+ elements, got {len(elements)}"
assert len(bones) >= 55, f"Need 55+ bones, got {len(bones)}"

all_bids = (spine_bids + wroot_bids + primary_bids + secondary_bids + covert_bids +
            drip_bids + disc_bids + shoulder_bids + ribbon_bids)

bone_name_map = {b["uuid"]: b["name"] for b in bones}

# ══════════════════════════════════════════════════════════════════════════════
# ANIMATIONS
# ══════════════════════════════════════════════════════════════════════════════

def build_spawn():
    """spawn once, 1.4s: spine 0.0s, wing roots 0.1s, coverts 0.2s, secondaries 0.4s,
    primaries 0.5s inner-to-outer 0.06s stagger, drips 1.0s, shadow 0.3s."""
    animators = {}

    # Spine: materialize at 0.0s (scale 0->1 by 0.15)
    for i, buid in enumerate(spine_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(0.05+i*0.02, 0,0,0,"scale"),
            kf(0.15+i*0.02, 1,1,1,"scale"),
            kf(0.7,  1,1,1,"scale"),
            kf(1.0,  1,1,1,"scale"),
            kf(1.4,  1,1,1,"scale"),
            kf(0.0,  0,-2,0,"position"),
            kf(0.05+i*0.02, 0,-1,0,"position"),
            kf(0.15+i*0.02, 0,0,0,"position"),
            kf(0.7,  0,0,0,"position"),
            kf(1.0,  0,0,0,"position"),
            kf(1.4,  0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.35, 0,0,0,"rotation"),
            kf(0.7,  0,0,0,"rotation"),
            kf(1.0,  0,0,0,"rotation"),
            kf(1.4,  0,0,0,"rotation"),
        ]}

    # Wing roots: appear at 0.1s
    for i, buid in enumerate(wroot_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(0.10, 0,0,0,"scale"),
            kf(0.22, 1,1,1,"scale"),
            kf(0.7,  1,1,1,"scale"),
            kf(1.0,  1,1,1,"scale"),
            kf(1.4,  1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.10, 0,0,0,"position"),
            kf(0.22, 0,0,0,"position"),
            kf(0.7,  0,0,0,"position"),
            kf(1.0,  0,0,0,"position"),
            kf(1.4,  0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.35, 0,0,0,"rotation"),
            kf(0.7,  0,0,0,"rotation"),
            kf(1.0,  0,0,0,"rotation"),
            kf(1.4,  0,0,0,"rotation"),
        ]}

    # Shoulder pads: appear at 0.15s
    for i, buid in enumerate(shoulder_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(0.15, 0,0,0,"scale"),
            kf(0.28, 1,1,1,"scale"),
            kf(0.7,  1,1,1,"scale"),
            kf(1.0,  1,1,1,"scale"),
            kf(1.4,  1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.15, 0,0,0,"position"),
            kf(0.7,  0,0,0,"position"),
            kf(1.0,  0,0,0,"position"),
            kf(1.4,  0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.35, 0,0,0,"rotation"),
            kf(0.7,  0,0,0,"rotation"),
            kf(1.0,  0,0,0,"rotation"),
            kf(1.4,  0,0,0,"rotation"),
        ]}

    # Covert feathers: deploy at 0.2s, stagger by index
    for i, buid in enumerate(covert_bids):
        side = -1 if i < 3 else 1
        c = i % 3
        delay = 0.20 + c * 0.04
        # The covert feathers unfurl: start scaled 0 and rotated tucked into spine
        tucked_rot_z = side * 60   # tucked toward spine
        final_rot_z = side * (-5 + c * 4)
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,            0,0,0,"scale"),
            kf(delay,          0,0,0,"scale"),
            kf(delay+0.10,     1,1,1,"scale"),
            kf(0.8,            1,1,1,"scale"),
            kf(1.0,            1,1,1,"scale"),
            kf(1.4,            1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(delay, 0,0,0,"position"),
            kf(delay+0.10, 0,0,0,"position"),
            kf(0.8,  0,0,0,"position"),
            kf(1.0,  0,0,0,"position"),
            kf(1.4,  0,0,0,"position"),
            kf(0.0,            0,0,tucked_rot_z,"rotation"),
            kf(delay,          0,0,tucked_rot_z,"rotation"),
            kf(delay+0.18,     0,0,final_rot_z,"rotation"),
            kf(0.8,            0,0,final_rot_z,"rotation"),
            kf(1.0,            0,0,final_rot_z,"rotation"),
            kf(1.4,            0,0,final_rot_z,"rotation"),
        ]}

    # Secondaries: deploy at 0.4s
    for i, buid in enumerate(secondary_bids):
        side = -1 if i < 3 else 1
        s = i % 3
        delay = 0.40 + s * 0.05
        tucked_rot_z = side * 50
        final_rot_z = side * (-15 + s * 8)
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,        0,0,0,"scale"),
            kf(delay,      0,0,0,"scale"),
            kf(delay+0.12, 1,1,1,"scale"),
            kf(0.9,        1,1,1,"scale"),
            kf(1.1,        1,1,1,"scale"),
            kf(1.4,        1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(delay, 0,0,0,"position"),
            kf(0.9,  0,0,0,"position"),
            kf(1.1,  0,0,0,"position"),
            kf(1.4,  0,0,0,"position"),
            kf(0.0,        0,0,tucked_rot_z,"rotation"),
            kf(delay,      0,0,tucked_rot_z,"rotation"),
            kf(delay+0.20, 0,0,final_rot_z,"rotation"),
            kf(0.9,        0,0,final_rot_z,"rotation"),
            kf(1.1,        0,0,final_rot_z,"rotation"),
            kf(1.4,        0,0,final_rot_z,"rotation"),
        ]}

    # Primaries: deploy at 0.5s, inner-to-outer stagger 0.06s
    for i, buid in enumerate(primary_bids):
        side = -1 if i < 5 else 1
        p = i % 5  # 0=inner, 4=outer
        delay = 0.50 + p * 0.06
        tucked_rot_z = side * 70  # most tucked
        final_rot_z = side * (-20 + p * 12)
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,        0,0,0,"scale"),
            kf(delay,      0,0,0,"scale"),
            kf(delay+0.15, 1,1,1,"scale"),
            kf(1.0,        1,1,1,"scale"),
            kf(1.2,        1,1,1,"scale"),
            kf(1.4,        1,1,1,"scale"),
            kf(0.0,        0,0,0,"position"),
            kf(delay,      0,0,0,"position"),
            kf(delay+0.15, 0,0,0,"position"),
            kf(1.0,        0,0,0,"position"),
            kf(1.2,        0,0,0,"position"),
            kf(1.4,        0,0,0,"position"),
            kf(0.0,        0,0,tucked_rot_z,"rotation"),
            kf(delay,      0,0,tucked_rot_z,"rotation"),
            kf(delay+0.25, 0,0,final_rot_z + side*5,"rotation"),  # slight overshoot
            kf(delay+0.40, 0,0,final_rot_z,"rotation"),
            kf(1.2,        0,0,final_rot_z,"rotation"),
            kf(1.4,        0,0,final_rot_z,"rotation"),
        ]}

    # Corruption drips: materialize at 1.0s
    for i, buid in enumerate(drip_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   0,0,0,"scale"),
            kf(1.0,   0,0,0,"scale"),
            kf(1.0+i*0.01, 0,0,0,"scale"),
            kf(1.15+i*0.01,1,1,1,"scale"),
            kf(1.3,   1,1,1,"scale"),
            kf(1.4,   1,1,1,"scale"),
            kf(0.0,   0,0.5,0,"position"),
            kf(1.0,   0,0.5,0,"position"),
            kf(1.15,  0,0,0,"position"),
            kf(1.3,   0,0,0,"position"),
            kf(1.4,   0,0,0,"position"),
            kf(0.0,   0,0,0,"rotation"),
            kf(0.7,   0,0,0,"rotation"),
            kf(1.4,   0,0,0,"rotation"),
        ]}

    # Shadow disc: expand at 0.3s
    for i, buid in enumerate(disc_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,    0,0,0,"scale"),
            kf(0.30,   0,0,0,"scale"),
            kf(0.30+i*0.03, 0.05,1,0.05,"scale"),
            kf(0.55+i*0.03, 1,1,1,"scale"),
            kf(1.0,    1,1,1,"scale"),
            kf(1.4,    1,1,1,"scale"),
            kf(0.0,    0,0,0,"position"),
            kf(0.55,   0,0,0,"position"),
            kf(1.0,    0,0,0,"position"),
            kf(1.4,    0,0,0,"position"),
            kf(0.0,    0,0,0,"rotation"),
            kf(0.7,    0,0,0,"rotation"),
            kf(1.4,    0,0,0,"rotation"),
        ]}

    # Ribbons: appear with primaries
    for i, buid in enumerate(ribbon_bids):
        side = -1 if i < 4 else 1
        r = i % 4
        delay = 0.7 + r * 0.05
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   0,0,0,"scale"),
            kf(delay, 0,0,0,"scale"),
            kf(delay+0.10, 1,1,1,"scale"),
            kf(1.0,   1,1,1,"scale"),
            kf(1.2,   1,1,1,"scale"),
            kf(1.4,   1,1,1,"scale"),
            kf(0.0,   0,0,0,"position"),
            kf(delay, 0,0,0,"position"),
            kf(1.0,   0,0,0,"position"),
            kf(1.2,   0,0,0,"position"),
            kf(1.4,   0,0,0,"position"),
            kf(0.0,   0,0,0,"rotation"),
            kf(0.7,   0,0,0,"rotation"),
            kf(1.4,   0,0,0,"rotation"),
        ]}

    return animators


def build_idle():
    """idle loop, 5.0s: wings breathe ±4°, individual flutter ±1.5° staggered,
    drips drift, disc breathes."""
    animators = {}

    # Spine: very slight sway
    for i, buid in enumerate(spine_bids):
        phase = i * 0.4
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   0,0,0+phase*0.2, "rotation"),
            kf(1.0,   0.5*math.sin(phase),0,1+phase*0.2, "rotation"),
            kf(2.0,   0.5*math.cos(phase),0,0+phase*0.2, "rotation"),
            kf(3.0,   -0.5*math.sin(phase),0,-1+phase*0.2, "rotation"),
            kf(4.0,   -0.5*math.cos(phase),0,0+phase*0.2, "rotation"),
            kf(5.0,   0,0,0+phase*0.2, "rotation"),
            kf(0.0,   0,0,0,"position"),
            kf(1.25,  0,0.05,0,"position"),
            kf(2.5,   0,0,0,"position"),
            kf(3.75,  0,-0.05,0,"position"),
            kf(5.0,   0,0,0,"position"),
            kf(0.0,   1,1,1,"scale"),
            kf(2.5,   1,1.02,1,"scale"),
            kf(5.0,   1,1,1,"scale"),
        ]}

    # Wing roots: slight breathe
    for i, buid in enumerate(wroot_bids):
        side = -1 if i == 0 else 1
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"rotation"),
            kf(1.25, 0,0,side*1, "rotation"),
            kf(2.5,  0,0,0,"rotation"),
            kf(3.75, 0,0,side*-1, "rotation"),
            kf(5.0,  0,0,0,"rotation"),
            kf(0.0,  0,0,0,"position"),
            kf(2.5,  0,0.05,0,"position"),
            kf(5.0,  0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"),
            kf(2.5,  1.02,1.02,1.02,"scale"),
            kf(5.0,  1,1,1,"scale"),
        ]}

    # Shoulder: still
    for i, buid in enumerate(shoulder_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"rotation"),
            kf(1.25, 0,0,0,"rotation"),
            kf(2.5,  0,0,0,"rotation"),
            kf(3.75, 0,0,0,"rotation"),
            kf(5.0,  0,0,0,"rotation"),
            kf(0.0,  0,0,0,"position"),
            kf(2.5,  0,0.03,0,"position"),
            kf(5.0,  0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"),
            kf(2.5,  1.01,1.01,1.01,"scale"),
            kf(5.0,  1,1,1,"scale"),
        ]}

    # Primaries: WINGS BREATHE — all primary feathers oscillate ±4° in unison
    # each feather also flutter ±1.5° staggered
    for i, buid in enumerate(primary_bids):
        side = -1 if i < 5 else 1
        p = i % 5
        base = side * (-20 + p * 12)
        # unison breathe (Z) + individual flutter (X)
        phase = (i * 0.7) % (2*math.pi)
        flutter_amp = 1.5
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  flutter_amp*math.sin(phase), 0, base + side*0, "rotation"),
            kf(1.0,  flutter_amp*math.sin(phase+1.0), 0, base + side*4, "rotation"),
            kf(2.0,  flutter_amp*math.sin(phase+2.0), 0, base + side*0, "rotation"),
            kf(3.0,  flutter_amp*math.sin(phase+3.0), 0, base - side*4, "rotation"),
            kf(4.0,  flutter_amp*math.sin(phase+4.0), 0, base + side*0, "rotation"),
            kf(5.0,  flutter_amp*math.sin(phase+5.0), 0, base + side*4, "rotation"),
            kf(0.0,  0,0,0,"position"),
            kf(1.25, 0,0.04,0,"position"),
            kf(2.5,  0,0,0,"position"),
            kf(3.75, 0,-0.04,0,"position"),
            kf(5.0,  0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"),
            kf(2.5,  1.02,1,1.02,"scale"),
            kf(5.0,  1,1,1,"scale"),
        ]}

    # Secondaries: gentle flutter
    for i, buid in enumerate(secondary_bids):
        side = -1 if i < 3 else 1
        s = i % 3
        base = side * (-15 + s * 8)
        phase = i * 0.55
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0, base+1.5*math.sin(phase), "rotation"),
            kf(1.0,  0,0, base+1.5*math.sin(phase+1.2), "rotation"),
            kf(2.0,  0,0, base+1.5*math.sin(phase+2.4), "rotation"),
            kf(3.0,  0,0, base+1.5*math.sin(phase+3.6), "rotation"),
            kf(4.0,  0,0, base+1.5*math.sin(phase+4.8), "rotation"),
            kf(5.0,  0,0, base+1.5*math.sin(phase+6.0), "rotation"),
            kf(0.0,  0,0,0,"position"),
            kf(2.5,  0,0.03,0,"position"),
            kf(5.0,  0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"),
            kf(2.5,  1.01,1.01,1.01,"scale"),
            kf(5.0,  1,1,1,"scale"),
        ]}

    # Coverts: subtle ripple
    for i, buid in enumerate(covert_bids):
        side = -1 if i < 3 else 1
        c = i % 3
        base = side * (-5 + c * 4)
        phase = i * 0.4
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0, base+1*math.sin(phase), "rotation"),
            kf(1.0,  0,0, base+1*math.sin(phase+1.2), "rotation"),
            kf(2.0,  0,0, base+1*math.sin(phase+2.4), "rotation"),
            kf(3.0,  0,0, base+1*math.sin(phase+3.6), "rotation"),
            kf(4.0,  0,0, base+1*math.sin(phase+4.8), "rotation"),
            kf(5.0,  0,0, base+1*math.sin(phase+6.0), "rotation"),
            kf(0.0,  0,0,0,"position"),
            kf(2.5,  0,0.02,0,"position"),
            kf(5.0,  0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"),
            kf(2.5,  1.01,1.01,1.01,"scale"),
            kf(5.0,  1,1,1,"scale"),
        ]}

    # Corruption drips: drift downward in looping Y positions
    for i, buid in enumerate(drip_bids):
        phase = i * 0.45
        # Y offset loops between 0 (start) and -2 (drift down) then resets
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0, 0, 0, "position"),
            kf(1.0,  0.05*math.sin(phase), -0.4, 0, "position"),
            kf(2.0,  0.05*math.cos(phase), -0.8, 0, "position"),
            kf(3.0,  -0.05*math.sin(phase), -1.2, 0, "position"),
            kf(4.0,  -0.05*math.cos(phase), -0.6, 0, "position"),
            kf(5.0,  0, 0, 0, "position"),
            kf(0.0,  1,1,1,"scale"),
            kf(1.0,  1.1,1.1,1.1,"scale"),
            kf(2.0,  0.9,0.9,0.9,"scale"),
            kf(3.0,  1.0,1.0,1.0,"scale"),
            kf(4.0,  1.05,1.05,1.05,"scale"),
            kf(5.0,  1,1,1,"scale"),
            kf(0.0,  0,0,0,"rotation"),
            kf(2.5,  0,30,0,"rotation"),
            kf(5.0,  0,0,0,"rotation"),
        ]}

    # Shadow disc: breathes
    for i, buid in enumerate(disc_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(1.0,  1.03,1,1.03,"scale"),
            kf(2.0,  1,1,1,"scale"),
            kf(3.0,  0.97,1,0.97,"scale"),
            kf(4.0,  1,1,1,"scale"),
            kf(5.0,  1.02,1,1.02,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(1.0,  0,-0.02,0,"position"),
            kf(2.0,  0,0,0,"position"),
            kf(3.0,  0,0.02,0,"position"),
            kf(4.0,  0,0,0,"position"),
            kf(5.0,  0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(2.5,  0,5,0,"rotation"),
            kf(5.0,  0,10,0,"rotation"),
        ]}

    # Ribbons: trail-like wave
    for i, buid in enumerate(ribbon_bids):
        side = -1 if i < 4 else 1
        phase = i * 0.6
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"position"),
            kf(1.0,  0,0.1*math.sin(phase),0,"position"),
            kf(2.0,  0,0.1*math.cos(phase),0,"position"),
            kf(3.0,  0,-0.1*math.sin(phase),0,"position"),
            kf(4.0,  0,-0.1*math.cos(phase),0,"position"),
            kf(5.0,  0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"),
            kf(2.5,  1.1,1.1,1.1,"scale"),
            kf(5.0,  1,1,1,"scale"),
            kf(0.0,  0,0,0,"rotation"),
            kf(2.5,  0,0,side*5,"rotation"),
            kf(5.0,  0,0,0,"rotation"),
        ]}

    return animators


def build_dissipate():
    """dissipate once, 0.9s: wings fold closed rapidly outer-primaries first,
    inner-secondaries last. Drips scatter upward (inverted). Disc contracts.
    Spine contracts and scales to 0."""
    animators = {}

    # Spine: contracts and scales to 0
    for i, buid in enumerate(spine_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.30, 1,1,1,"scale"),
            kf(0.55, 1,0.5,1,"scale"),
            kf(0.75, 0.3,0.1,0.3,"scale"),
            kf(0.85, 0,0,0,"scale"),
            kf(0.9,  0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.45, 0,-0.5,0,"position"),
            kf(0.75, 0,-1.5,0,"position"),
            kf(0.9,  0,-2.5,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.45, 0,0,0,"rotation"),
            kf(0.9,  0,0,0,"rotation"),
        ]}

    # Wing roots: contract
    for i, buid in enumerate(wroot_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.50, 1,1,1,"scale"),
            kf(0.75, 0.5,0.5,0.5,"scale"),
            kf(0.9,  0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.45, 0,0,0,"position"),
            kf(0.9,  0,-0.5,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.45, 0,0,0,"rotation"),
            kf(0.9,  0,0,0,"rotation"),
        ]}

    # Shoulders
    for i, buid in enumerate(shoulder_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.40, 1,1,1,"scale"),
            kf(0.65, 0.5,0.5,0.5,"scale"),
            kf(0.9,  0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.45, 0,0,0,"position"),
            kf(0.9,  0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.45, 0,0,0,"rotation"),
            kf(0.9,  0,0,0,"rotation"),
        ]}

    # Primaries: outer first fold inward
    for i, buid in enumerate(primary_bids):
        side = -1 if i < 5 else 1
        p = i % 5  # 0=inner, 4=outer
        # outer feathers fold first — outer = larger p, fold earlier
        fold_start = 0.0 + (4 - p) * 0.05  # outer (p=4) starts at 0.0, inner (p=0) at 0.20
        fold_end = fold_start + 0.30
        base = side * (-20 + p * 12)
        tucked_z = side * 70  # fold all the way in
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,        1,1,1,"scale"),
            kf(fold_end,   1,1,1,"scale"),
            kf(fold_end+0.15, 0.7,0.5,0.7,"scale"),
            kf(0.9,        0,0,0,"scale"),
            kf(0.0,        0,0,0,"position"),
            kf(0.45,       0,0,0,"position"),
            kf(0.9,        0,-0.3,0,"position"),
            kf(0.0,        0,0,base,"rotation"),
            kf(fold_start, 0,0,base,"rotation"),
            kf(fold_end,   0,0,tucked_z,"rotation"),
            kf(0.9,        0,0,tucked_z,"rotation"),
        ]}

    # Secondaries: fold last
    for i, buid in enumerate(secondary_bids):
        side = -1 if i < 3 else 1
        s = i % 3
        fold_start = 0.30 + s * 0.05
        fold_end = fold_start + 0.30
        base = side * (-15 + s * 8)
        tucked_z = side * 60
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,        1,1,1,"scale"),
            kf(fold_end,   1,1,1,"scale"),
            kf(fold_end+0.10, 0.7,0.7,0.7,"scale"),
            kf(0.9,        0,0,0,"scale"),
            kf(0.0,        0,0,0,"position"),
            kf(0.45,       0,0,0,"position"),
            kf(0.9,        0,0,0,"position"),
            kf(0.0,        0,0,base,"rotation"),
            kf(fold_start, 0,0,base,"rotation"),
            kf(fold_end,   0,0,tucked_z,"rotation"),
            kf(0.9,        0,0,tucked_z,"rotation"),
        ]}

    # Coverts: fold mid
    for i, buid in enumerate(covert_bids):
        side = -1 if i < 3 else 1
        c = i % 3
        fold_start = 0.20 + c * 0.04
        fold_end = fold_start + 0.30
        base = side * (-5 + c * 4)
        tucked_z = side * 50
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,        1,1,1,"scale"),
            kf(fold_end,   1,1,1,"scale"),
            kf(fold_end+0.10, 0.7,0.7,0.7,"scale"),
            kf(0.9,        0,0,0,"scale"),
            kf(0.0,        0,0,0,"position"),
            kf(0.45,       0,0,0,"position"),
            kf(0.9,        0,0,0,"position"),
            kf(0.0,        0,0,base,"rotation"),
            kf(fold_start, 0,0,base,"rotation"),
            kf(fold_end,   0,0,tucked_z,"rotation"),
            kf(0.9,        0,0,tucked_z,"rotation"),
        ]}

    # Drips: SCATTER UPWARD (inverted gravity)
    for i, buid in enumerate(drip_bids):
        side = -1 if i < 8 else 1
        d = i % 8
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.30, 1.2,1.2,1.2,"scale"),
            kf(0.60, 0.8,0.8,0.8,"scale"),
            kf(0.9,  0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.30, side*0.3, 1.0+d*0.15, 0, "position"),
            kf(0.60, side*0.6, 2.5+d*0.25, 0, "position"),
            kf(0.9,  side*0.9, 4.5+d*0.35, 0, "position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.45, 0, 60, 0, "rotation"),
            kf(0.9,  0, 180, 0, "rotation"),
        ]}

    # Disc: contracts
    for i, buid in enumerate(disc_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.30, 1,1,1,"scale"),
            kf(0.60, 0.5,1,0.5,"scale"),
            kf(0.85, 0.1,1,0.1,"scale"),
            kf(0.9,  0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.45, 0,0,0,"position"),
            kf(0.9,  0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.45, 0,30,0,"rotation"),
            kf(0.9,  0,90,0,"rotation"),
        ]}

    # Ribbons: scatter with drips
    for i, buid in enumerate(ribbon_bids):
        side = -1 if i < 4 else 1
        r = i % 4
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.30, 1.1,1.1,1.1,"scale"),
            kf(0.60, 0.7,0.7,0.7,"scale"),
            kf(0.9,  0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.30, side*0.4, 0.8+r*0.2, 0, "position"),
            kf(0.60, side*0.8, 2.0+r*0.3, 0, "position"),
            kf(0.9,  side*1.2, 3.5+r*0.4, 0, "position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.45, 0,90,0,"rotation"),
            kf(0.9,  0,180,0,"rotation"),
        ]}

    return animators


# ── Build model ──────────────────────────────────────────────────────────────
spawn_anim = {
    "uuid": aid(1), "name": "animation.fallen_angel_wings_summon.spawn",
    "loop": "once", "length": 1.4, "snapping": 24,
    "selected": False, "saved": False, "animators": build_spawn()
}
idle_anim = {
    "uuid": aid(2), "name": "animation.fallen_angel_wings_summon.idle",
    "loop": "loop", "length": 5.0, "snapping": 24,
    "selected": False, "saved": False, "animators": build_idle()
}
dissipate_anim = {
    "uuid": aid(3), "name": "animation.fallen_angel_wings_summon.dissipate",
    "loop": "once", "length": 0.9, "snapping": 24,
    "selected": False, "saved": False, "animators": build_dissipate()
}

outliner = [b["uuid"] for b in bones]

textures = [
    {"id":"0","name":"fallen_angel_wings_summon_tex","relative_path":"../textures/fallen_angel_wings_summon_tex.png",
     "folder":"devilsdream","namespace":"","visible":True,"mode":"bitmap","saved":False,
     "uuid":tid(1),"source":f"data:image/png;base64,{b64_0}"},
    {"id":"1","name":"fallen_angel_wings_summon_tex1","relative_path":"../textures/fallen_angel_wings_summon_tex1.png",
     "folder":"devilsdream","namespace":"","visible":True,"mode":"bitmap","saved":False,
     "uuid":tid(2),"source":f"data:image/png;base64,{b64_1}"},
]

model = {
    "meta": {"format_version":"4.10","model_format":"free","box_uv":False},
    "name": "fallen_angel_wings_summon",
    "geometry": "geometry.fallen_angel_wings_summon",
    "resolution": {"width":64,"height":64},
    "elements": elements,
    "outliner": outliner,
    "textures": textures,
    "animations": [spawn_anim, idle_anim, dissipate_anim]
}

out_path = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/fallen_angel_wings_summon.bbmodel"
with open(out_path, "w", encoding="utf-8") as f:
    json.dump(model, f, separators=(',', ':'))

size = os.path.getsize(out_path)
print(f"Written: {out_path}")
print(f"Elements: {len(elements)}, Bones: {len(bones)}")
print(f"Size: {size:,} bytes ({size/1024:.1f} KB)")
print(f"{'PASS' if size>=300*1024 else 'FAIL'}: {'>=300KB' if size>=300*1024 else f'only {size//1024}KB'}")
