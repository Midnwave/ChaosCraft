import json, math, uuid, os

TEX0_B64 = open("D:/CC/ChaosCraft/tools/gen_textures/m05_b64.txt").read()
TEX1_B64 = open("D:/CC/ChaosCraft/tools/gen_textures/m05_tex1_b64.txt").read()

def uid(): return str(uuid.uuid4())

# UV faces helpers
def faces0():
    return {
        "north":{"uv":[2,2,30,30],"texture":0},
        "east":{"uv":[34,2,62,30],"texture":0},
        "south":{"uv":[2,2,30,30],"texture":0},
        "west":{"uv":[34,2,62,30],"texture":0},
        "up":{"uv":[2,34,30,62],"texture":0},
        "down":{"uv":[34,34,62,62],"texture":0}
    }
def faces1():
    return {
        "north":{"uv":[2,2,30,30],"texture":1},
        "east":{"uv":[34,2,62,30],"texture":1},
        "south":{"uv":[2,2,30,30],"texture":1},
        "west":{"uv":[34,2,62,30],"texture":1},
        "up":{"uv":[2,34,30,62],"texture":1},
        "down":{"uv":[34,34,62,62],"texture":1}
    }

def make_elem(name, frm, to, tex=0):
    f = faces0() if tex==0 else faces1()
    return {"name":name,"box_uv":False,"rescale":False,"locked":False,
            "render_order":"default","allow_mirror_modeling":True,
            "from":frm,"to":to,"autouv":0,"color":0,"uuid":uid(),"faces":f}

def make_bone(name, origin, children, rotation=None):
    b = {"name":name,"origin":origin,"uuid":uid(),"export":True,"isOpen":True,"children":children}
    if rotation: b["rotation"] = rotation
    else: b["rotation"] = [0,0,0]
    return b

# ---- ELEMENTS AND BONES ----
elements = []
outliner_bones = []

bone_uuids = {}  # bone_name -> uuid

# Helper: add element and return its uuid
def add_elem(name, frm, to, tex=0):
    e = make_elem(name, frm, to, tex)
    elements.append(e)
    return e["uuid"]

# 1) OUTER RING: 18 bones, each a flat slab at radius 15
# Slab: 0.4 wide, 3.0 tall, 0.2 deep, face inward
# Placed at (cx-0.2, 0, cz-1.5) to (cx+0.2, 3.0, cz+1.5) then rotate
outer_bone_list = []
for i in range(18):
    angle = (i / 18) * 360.0
    rad = math.radians(angle)
    cx = 15 * math.cos(rad)
    cz = 15 * math.sin(rad)
    # slab from/to at origin, then origin placed at (cx, 0, cz)
    # The slab extends from origin: [-0.2, 0, -0.1] to [0.2, 3.0, 0.1]
    eid = add_elem(f"outer_slab_{i+1}",
                   [cx-0.2, 0, cz-0.1],
                   [cx+0.2, 3.0, cz+0.1], tex=0)
    b_uid = uid()
    bone = {"name":f"outer_{i+1}","origin":[cx,0,cz],"rotation":[0, angle, 0],
            "uuid":b_uid,"export":True,"isOpen":True,"children":[eid]}
    outliner_bones.append(bone)
    bone_uuids[f"outer_{i+1}"] = b_uid
    outer_bone_list.append(f"outer_{i+1}")

# 2) INNER RING: 12 bones, radius 10, slab 0.4×2.0×0.2
inner_bone_list = []
for i in range(12):
    angle = (i / 12) * 360.0
    rad = math.radians(angle)
    cx = 10 * math.cos(rad)
    cz = 10 * math.sin(rad)
    eid = add_elem(f"inner_slab_{i+1}",
                   [cx-0.2, 0, cz-0.1],
                   [cx+0.2, 2.0, cz+0.1], tex=0)
    b_uid = uid()
    bone = {"name":f"inner_{i+1}","origin":[cx,0,cz],"rotation":[0, angle, 0],
            "uuid":b_uid,"export":True,"isOpen":True,"children":[eid]}
    outliner_bones.append(bone)
    bone_uuids[f"inner_{i+1}"] = b_uid
    inner_bone_list.append(f"inner_{i+1}")

# 3) DETONATION NODE: 3 parent bones, each with 8 slabs
# node_h: horizontal ring at Y=5, radius 1.5, XZ plane
node_h_children = []
for i in range(8):
    angle = (i / 8) * 360.0
    rad = math.radians(angle)
    cx = 1.5 * math.cos(rad)
    cz = 1.5 * math.sin(rad)
    eid = add_elem(f"node_h_slab_{i+1}",
                   [cx-0.15, 4.85, cz-0.1],
                   [cx+0.15, 5.15, cz+0.1], tex=0)
    node_h_children.append(eid)

b_uid_nh = uid()
bone_nh = {"name":"node_h","origin":[0,5,0],"rotation":[0,0,0],
           "uuid":b_uid_nh,"export":True,"isOpen":True,"children":node_h_children}
outliner_bones.append(bone_nh)
bone_uuids["node_h"] = b_uid_nh

# node_vns: vertical ring NS (YZ plane) at Y=5
node_vns_children = []
for i in range(8):
    angle = (i / 8) * 360.0
    rad = math.radians(angle)
    cy = 5 + 1.5 * math.sin(rad)
    cz = 1.5 * math.cos(rad)
    eid = add_elem(f"node_vns_slab_{i+1}",
                   [-0.1, cy-0.15, cz-0.15],
                   [0.1, cy+0.15, cz+0.15], tex=0)
    node_vns_children.append(eid)

b_uid_vns = uid()
bone_vns = {"name":"node_vns","origin":[0,5,0],"rotation":[0,0,0],
            "uuid":b_uid_vns,"export":True,"isOpen":True,"children":node_vns_children}
outliner_bones.append(bone_vns)
bone_uuids["node_vns"] = b_uid_vns

# node_vew: vertical ring EW (XY plane) at Y=5
node_vew_children = []
for i in range(8):
    angle = (i / 8) * 360.0
    rad = math.radians(angle)
    cx = 1.5 * math.cos(rad)
    cy = 5 + 1.5 * math.sin(rad)
    eid = add_elem(f"node_vew_slab_{i+1}",
                   [cx-0.15, cy-0.15, -0.1],
                   [cx+0.15, cy+0.15, 0.1], tex=0)
    node_vew_children.append(eid)

b_uid_vew = uid()
bone_vew = {"name":"node_vew","origin":[0,5,0],"rotation":[0,0,0],
            "uuid":b_uid_vew,"export":True,"isOpen":True,"children":node_vew_children}
outliner_bones.append(bone_vew)
bone_uuids["node_vew"] = b_uid_vew

# 4) REALITY TEARS: 8 bones, jagged flat slabs at various heights/radii/tilts
tear_bone_list = []
tear_configs = [
    (8, 1, 0, 15, 45),   (10, 2, 45, 30, 20),  (-9, 3, 90, 20, 35),
    (11, 4, 135, 25, 10), (-8, 5, 180, 40, 15), (9, 2, 225, 35, 25),
    (-10, 3, 270, 12, 42), (8, 4, 315, 28, 18)
]
for i, (radius, height, ang, rx, rz) in enumerate(tear_configs):
    rad = math.radians(ang)
    cx = abs(radius) * math.cos(rad) * (1 if radius > 0 else -1)
    cz = abs(radius) * math.sin(rad) * (1 if radius > 0 else -1)
    eid = add_elem(f"tear_{i+1}",
                   [cx-0.5, height, cz-0.1],
                   [cx+0.5, height+1.5, cz+0.1], tex=0)
    b_uid = uid()
    bone = {"name":f"tear_{i+1}","origin":[cx, height, cz],
            "rotation":[rx, ang, rz],
            "uuid":b_uid,"export":True,"isOpen":True,"children":[eid]}
    outliner_bones.append(bone)
    bone_uuids[f"tear_{i+1}"] = b_uid
    tear_bone_list.append(f"tear_{i+1}")

# 5) GROUND RING: 12 bones, flat slabs at Y=0, radius 12
gr_bone_list = []
for i in range(12):
    angle = (i / 12) * 360.0
    rad = math.radians(angle)
    cx = 12 * math.cos(rad)
    cz = 12 * math.sin(rad)
    eid = add_elem(f"gr_slab_{i+1}",
                   [cx-0.75, -0.075, cz-0.2],
                   [cx+0.75, 0.075, cz+0.2], tex=1)
    b_uid = uid()
    bone = {"name":f"gr_{i+1}","origin":[cx, 0, cz],"rotation":[0, angle, 0],
            "uuid":b_uid,"export":True,"isOpen":True,"children":[eid]}
    outliner_bones.append(bone)
    bone_uuids[f"gr_{i+1}"] = b_uid
    gr_bone_list.append(f"gr_{i+1}")

# 6) EXTRA BONES: 5 outer_accent, 5 inner_accent, 3 node_glow = 13 bones
# outer_accent: small pieces near outer ring
for i in range(5):
    angle = (i / 5) * 360.0 + 10.0
    rad = math.radians(angle)
    cx = 14.0 * math.cos(rad)
    cz = 14.0 * math.sin(rad)
    eid = add_elem(f"outer_acc_{i+1}",
                   [cx-0.15, 0.5, cz-0.1],
                   [cx+0.15, 1.5, cz+0.1], tex=0)
    b_uid = uid()
    bone = {"name":f"outer_acc_{i+1}","origin":[cx, 1.0, cz],"rotation":[0, angle+5, 10],
            "uuid":b_uid,"export":True,"isOpen":True,"children":[eid]}
    outliner_bones.append(bone)
    bone_uuids[f"outer_acc_{i+1}"] = b_uid

# inner_accent: between inner ring slabs
for i in range(5):
    angle = (i / 5) * 360.0 + 15.0
    rad = math.radians(angle)
    cx = 10.5 * math.cos(rad)
    cz = 10.5 * math.sin(rad)
    eid = add_elem(f"inner_acc_{i+1}",
                   [cx-0.1, 0.2, cz-0.05],
                   [cx+0.1, 1.2, cz+0.05], tex=0)
    b_uid = uid()
    bone = {"name":f"inner_acc_{i+1}","origin":[cx, 0.7, cz],"rotation":[0, angle, 5],
            "uuid":b_uid,"export":True,"isOpen":True,"children":[eid]}
    outliner_bones.append(bone)
    bone_uuids[f"inner_acc_{i+1}"] = b_uid

# node_glow: 3 flat disc pieces at Y=5 center
for i in range(3):
    angle = i * 60.0
    eid = add_elem(f"node_glow_{i+1}",
                   [-0.6, 4.95+(i*0.05), -0.6],
                   [0.6, 5.0+(i*0.05), 0.6], tex=1)
    b_uid = uid()
    bone = {"name":f"node_glow_{i+1}","origin":[0, 5, 0],"rotation":[0, angle, 0],
            "uuid":b_uid,"export":True,"isOpen":True,"children":[eid]}
    outliner_bones.append(bone)
    bone_uuids[f"node_glow_{i+1}"] = b_uid

print(f"Elements: {len(elements)}, Bones: {len(outliner_bones)}")
assert len(elements) >= 80, f"Need 80+ elements, got {len(elements)}"
assert len(outliner_bones) >= 55, f"Need 55+ bones, got {len(outliner_bones)}"

# ---- ANIMATIONS ----
# We need 3 animations. Every bone must appear in every animation with 6+ keyframes each.

all_bone_names = list(bone_uuids.keys())

def kf(t, x, y, z, channel, interp="catmullrom"):
    return {"uuid":uid(),"time":t,"color":-1,"interpolation":interp,
            "data_points":[{"x":str(x),"y":str(y),"z":str(z)}],"channel":channel}

def rot_kf(t, x, y, z, interp="catmullrom"):
    return kf(t, x, y, z, "rotation", interp)

def pos_kf(t, x, y, z, interp="catmullrom"):
    return kf(t, x, y, z, "position", interp)

def scale_kf(t, x, y, z, interp="catmullrom"):
    return kf(t, x, y, z, "scale", interp)

def make_animators_spawn():
    """spawn (once, 1.2s): rings contract inward, centre explodes at 0.7s"""
    animators = {}
    for bn in all_bone_names:
        kfs = []
        if bn.startswith("outer_") and not bn.startswith("outer_acc"):
            # outer ring contracts: scale 2.0 -> 1.0 during 0-0.7s, bounce at 0.7
            kfs += [
                scale_kf(0.0, 2.0, 2.0, 2.0),
                scale_kf(0.3, 1.7, 1.7, 1.7),
                scale_kf(0.6, 1.1, 1.1, 1.1),
                scale_kf(0.7, 0.9, 0.9, 0.9),
                scale_kf(0.9, 1.05, 1.05, 1.05),
                scale_kf(1.2, 1.0, 1.0, 1.0),
            ]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.7,0,0,0)]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(1.2,0,0,0)]
        elif bn.startswith("inner_") and not bn.startswith("inner_acc"):
            kfs += [
                scale_kf(0.0, 2.0, 2.0, 2.0),
                scale_kf(0.35, 1.6, 1.6, 1.6),
                scale_kf(0.65, 1.05, 1.05, 1.05),
                scale_kf(0.7, 0.85, 0.85, 0.85),
                scale_kf(0.9, 1.05, 1.05, 1.05),
                scale_kf(1.2, 1.0, 1.0, 1.0),
            ]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.7,0,0,0)]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(1.2,0,0,0)]
        elif bn in ("node_h","node_vns","node_vew"):
            # Hidden until detonation at 0.7s
            kfs += [
                scale_kf(0.0, 0.0, 0.0, 0.0),
                scale_kf(0.68, 0.0, 0.0, 0.0),
                scale_kf(0.72, 1.8, 1.8, 1.8),
                scale_kf(0.9, 1.0, 1.0, 1.0),
                scale_kf(1.1, 1.0, 1.0, 1.0),
                scale_kf(1.2, 1.0, 1.0, 1.0),
            ]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(0.7,0,0,0), rot_kf(1.2,0,30,0)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(1.2,0,0,0)]
        elif bn.startswith("tear_"):
            # Materialise at 0.3s
            kfs += [
                scale_kf(0.0, 0.0, 0.0, 0.0),
                scale_kf(0.28, 0.0, 0.0, 0.0),
                scale_kf(0.35, 1.3, 1.3, 1.3),
                scale_kf(0.5, 0.9, 0.9, 0.9),
                scale_kf(0.8, 1.0, 1.0, 1.0),
                scale_kf(1.2, 1.0, 1.0, 1.0),
            ]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(0.35,5,10,5), rot_kf(1.2,0,0,0)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(1.2,0,0,0)]
        elif bn.startswith("gr_"):
            # Ground ring expands at 0.0s
            kfs += [
                scale_kf(0.0, 0.0, 1.0, 0.0),
                scale_kf(0.2, 1.2, 1.0, 1.2),
                scale_kf(0.4, 0.95, 1.0, 0.95),
                scale_kf(0.7, 1.0, 1.0, 1.0),
                scale_kf(1.0, 1.0, 1.0, 1.0),
                scale_kf(1.2, 1.0, 1.0, 1.0),
            ]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(1.2,0,0,0)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(1.2,0,0,0)]
        elif bn.startswith("node_glow_"):
            kfs += [
                scale_kf(0.0, 0.0, 0.0, 0.0),
                scale_kf(0.68, 0.0, 0.0, 0.0),
                scale_kf(0.72, 2.0, 2.0, 2.0),
                scale_kf(0.85, 0.8, 0.8, 0.8),
                scale_kf(1.0, 1.0, 1.0, 1.0),
                scale_kf(1.2, 1.0, 1.0, 1.0),
            ]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(1.2,0,45,0)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(1.2,0,0,0)]
        else:
            # outer_acc / inner_acc
            kfs += [
                scale_kf(0.0, 0.0, 0.0, 0.0),
                scale_kf(0.3, 1.2, 1.2, 1.2),
                scale_kf(0.5, 0.9, 0.9, 0.9),
                scale_kf(0.7, 1.0, 1.0, 1.0),
                scale_kf(1.0, 1.0, 1.0, 1.0),
                scale_kf(1.2, 1.0, 1.0, 1.0),
            ]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(1.2,0,0,0)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(1.2,0,0,0)]

        animators[bone_uuids[bn]] = {"name": bn, "type": "bone", "keyframes": kfs}
    return animators

def make_animators_idle():
    """idle (loop, 5.0s)"""
    animators = {}
    for i, bn in enumerate(all_bone_names):
        kfs = []
        phase = (i * 0.4) % 5.0
        if bn.startswith("outer_") and not bn.startswith("outer_acc"):
            # outer ring rotates CW slowly
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(1.25, 0, 18, 0),
                rot_kf(2.5, 0, 36, 0),
                rot_kf(3.75, 0, 54, 0),
                rot_kf(4.5, 0, 64.8, 0),
                rot_kf(5.0, 0, 72, 0),
            ]
            kfs += [scale_kf(0.0,1,1,1), scale_kf(2.5,1,1,1), scale_kf(5.0,1,1,1)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(5.0,0,0,0)]
        elif bn.startswith("inner_") and not bn.startswith("inner_acc"):
            # inner ring counter-CW
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(1.25, 0, -22.5, 0),
                rot_kf(2.5, 0, -45, 0),
                rot_kf(3.75, 0, -67.5, 0),
                rot_kf(4.5, 0, -81, 0),
                rot_kf(5.0, 0, -90, 0),
            ]
            kfs += [scale_kf(0.0,1,1,1), scale_kf(2.5,1,1,1), scale_kf(5.0,1,1,1)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(5.0,0,0,0)]
        elif bn == "node_h":
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(1.0, 0, 45, 0),
                rot_kf(2.5, 0, 90, 0),
                rot_kf(3.5, 0, 135, 0),
                rot_kf(4.5, 0, 180, 0),
                rot_kf(5.0, 0, 225, 0),
            ]
            kfs += [scale_kf(0.0,1,1,1), scale_kf(5.0,1,1,1)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(5.0,0,0,0)]
        elif bn == "node_vns":
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(1.0, 30, 0, 0),
                rot_kf(2.5, 60, 0, 0),
                rot_kf(3.5, 90, 0, 0),
                rot_kf(4.5, 120, 0, 0),
                rot_kf(5.0, 150, 0, 0),
            ]
            kfs += [scale_kf(0.0,1,1,1), scale_kf(5.0,1,1,1)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(5.0,0,0,0)]
        elif bn == "node_vew":
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(1.0, 0, 0, 30),
                rot_kf(2.5, 0, 0, 60),
                rot_kf(3.5, 0, 0, 90),
                rot_kf(4.5, 0, 0, 120),
                rot_kf(5.0, 0, 0, 150),
            ]
            kfs += [scale_kf(0.0,1,1,1), scale_kf(5.0,1,1,1)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(5.0,0,0,0)]
        elif bn.startswith("tear_"):
            # pulse scale
            kfs += [
                scale_kf(0.0, 1.0, 1.0, 1.0),
                scale_kf(0.8, 1.15, 1.15, 1.15),
                scale_kf(1.6, 0.9, 0.9, 0.9),
                scale_kf(2.5, 1.1, 1.1, 1.1),
                scale_kf(3.8, 0.95, 0.95, 0.95),
                scale_kf(5.0, 1.0, 1.0, 1.0),
            ]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(2.5,5,0,5), rot_kf(5.0,0,0,0)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(5.0,0,0,0)]
        elif bn.startswith("gr_"):
            kfs += [
                scale_kf(0.0, 1.0, 1.0, 1.0),
                rot_kf(0.0, 0, 0, 0),
                rot_kf(1.25, 0, 5, 0),
                rot_kf(2.5, 0, 10, 0),
                rot_kf(3.75, 0, 15, 0),
                rot_kf(5.0, 0, 20, 0),
            ]
            kfs += [
                scale_kf(1.25, 1.03, 1.0, 1.03),
                scale_kf(2.5, 0.97, 1.0, 0.97),
                scale_kf(3.75, 1.02, 1.0, 1.02),
                scale_kf(5.0, 1.0, 1.0, 1.0),
            ]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(5.0,0,0,0)]
        elif bn.startswith("node_glow_"):
            idx = int(bn.split("_")[-1])
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(1.0, 0, idx*30, 0),
                rot_kf(2.5, 0, idx*60, 0),
                rot_kf(3.5, 0, idx*90, 0),
                rot_kf(4.5, 0, idx*120, 0),
                rot_kf(5.0, 0, idx*150, 0),
            ]
            kfs += [scale_kf(0.0,1,1,1), scale_kf(2.5,1.05,1,1.05), scale_kf(5.0,1,1,1)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(5.0,0,0,0)]
        else:
            # accent bones
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(1.0, 3, 10, 3),
                rot_kf(2.0, -3, 20, -3),
                rot_kf(3.0, 3, 30, 3),
                rot_kf(4.0, -3, 40, -3),
                rot_kf(5.0, 0, 50, 0),
            ]
            kfs += [scale_kf(0.0,1,1,1), scale_kf(2.5,1.05,1.05,1.05), scale_kf(5.0,1,1,1)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(5.0,0,0,0)]

        animators[bone_uuids[bn]] = {"name": bn, "type": "bone", "keyframes": kfs}
    return animators

def make_animators_dissipate():
    """dissipate (once, 0.5s)"""
    animators = {}
    for i, bn in enumerate(all_bone_names):
        kfs = []
        if bn.startswith("outer_") and not bn.startswith("outer_acc"):
            kfs += [
                scale_kf(0.0, 1.0, 1.0, 1.0),
                scale_kf(0.15, 1.5, 1.5, 1.5),
                scale_kf(0.3, 2.5, 2.5, 2.5),
                scale_kf(0.4, 4.0, 4.0, 4.0),
                scale_kf(0.45, 5.0, 5.0, 5.0),
                scale_kf(0.5, 0.0, 0.0, 0.0),
            ]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(0.5,0,15,0)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.5,0,0,0)]
        elif bn.startswith("inner_") and not bn.startswith("inner_acc"):
            kfs += [
                scale_kf(0.0, 1.0, 1.0, 1.0),
                scale_kf(0.1, 1.4, 1.4, 1.4),
                scale_kf(0.25, 2.2, 2.2, 2.2),
                scale_kf(0.35, 3.5, 3.5, 3.5),
                scale_kf(0.42, 4.5, 4.5, 4.5),
                scale_kf(0.5, 0.0, 0.0, 0.0),
            ]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(0.5,0,-15,0)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.5,0,0,0)]
        elif bn in ("node_h","node_vns","node_vew","node_glow_1","node_glow_2","node_glow_3"):
            # Centre node implodes
            kfs += [
                scale_kf(0.0, 1.0, 1.0, 1.0),
                scale_kf(0.1, 1.3, 1.3, 1.3),
                scale_kf(0.25, 2.0, 2.0, 2.0),
                scale_kf(0.35, 0.5, 0.5, 0.5),
                scale_kf(0.45, 0.1, 0.1, 0.1),
                scale_kf(0.5, 0.0, 0.0, 0.0),
            ]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(0.25,45,45,45), rot_kf(0.5,90,90,90)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.5,0,0,0)]
        elif bn.startswith("tear_"):
            kfs += [
                scale_kf(0.0, 1.0, 1.0, 1.0),
                scale_kf(0.1, 1.5, 0.5, 1.5),
                scale_kf(0.2, 2.5, 0.2, 2.5),
                scale_kf(0.3, 3.5, 0.1, 3.5),
                scale_kf(0.4, 4.0, 0.0, 4.0),
                scale_kf(0.5, 0.0, 0.0, 0.0),
            ]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(0.3,20,30,20), rot_kf(0.5,45,60,45)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.5,0,2,0)]
        elif bn.startswith("gr_"):
            kfs += [
                scale_kf(0.0, 1.0, 1.0, 1.0),
                scale_kf(0.15, 1.5, 1.0, 1.5),
                scale_kf(0.3, 2.5, 1.0, 2.5),
                scale_kf(0.4, 4.0, 1.0, 4.0),
                scale_kf(0.45, 5.0, 1.0, 5.0),
                scale_kf(0.5, 0.0, 0.0, 0.0),
            ]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(0.5,0,20,0)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.5,0,0,0)]
        else:
            kfs += [
                scale_kf(0.0, 1.0, 1.0, 1.0),
                scale_kf(0.1, 1.3, 1.3, 1.3),
                scale_kf(0.25, 2.0, 2.0, 2.0),
                scale_kf(0.35, 3.0, 3.0, 3.0),
                scale_kf(0.45, 4.0, 4.0, 4.0),
                scale_kf(0.5, 0.0, 0.0, 0.0),
            ]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(0.5,10,20,10)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.5,0,0,0)]

        animators[bone_uuids[bn]] = {"name": bn, "type": "bone", "keyframes": kfs}
    return animators

animations = [
    {
        "uuid": uid(),
        "name": "animation.dream_collapse_ring.spawn",
        "loop": "once",
        "length": 1.2,
        "snapping": 24,
        "selected": False,
        "saved": False,
        "animators": make_animators_spawn()
    },
    {
        "uuid": uid(),
        "name": "animation.dream_collapse_ring.idle",
        "loop": "loop",
        "length": 5.0,
        "snapping": 24,
        "selected": False,
        "saved": False,
        "animators": make_animators_idle()
    },
    {
        "uuid": uid(),
        "name": "animation.dream_collapse_ring.dissipate",
        "loop": "once",
        "length": 0.5,
        "snapping": 24,
        "selected": False,
        "saved": False,
        "animators": make_animators_dissipate()
    },
]

textures = [
    {
        "id": "0", "name": "dream_collapse_ring_tex",
        "relative_path": "../textures/dream_collapse_ring_tex.png",
        "folder": "devilsdream", "namespace": "", "visible": True,
        "mode": "bitmap", "saved": False, "uuid": uid(),
        "source": f"data:image/png;base64,{TEX0_B64}"
    },
    {
        "id": "1", "name": "dream_collapse_ring_tex1",
        "relative_path": "../textures/dream_collapse_ring_tex1.png",
        "folder": "devilsdream", "namespace": "", "visible": True,
        "mode": "bitmap", "saved": False, "uuid": uid(),
        "source": f"data:image/png;base64,{TEX1_B64}"
    }
]

bbmodel = {
    "meta": {"format_version": "4.10", "model_format": "free", "box_uv": False},
    "name": "dream_collapse_ring",
    "geometry": "geometry.dream_collapse_ring",
    "resolution": {"width": 64, "height": 64},
    "elements": elements,
    "outliner": outliner_bones,
    "textures": textures,
    "animations": animations
}

out_path = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/dream_collapse_ring.bbmodel"
with open(out_path, "w") as f:
    json.dump(bbmodel, f, separators=(',', ':'))

sz = os.path.getsize(out_path)
print(f"Written: {out_path}")
print(f"Size: {sz} bytes ({sz/1024:.1f} KB)")
if sz < 300*1024:
    print(f"WARNING: File is {sz/1024:.1f}KB, need 300KB!")
else:
    print("OK: >=300KB")
