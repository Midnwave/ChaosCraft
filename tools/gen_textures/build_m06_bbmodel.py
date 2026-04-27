import json, math, uuid, os

TEX0_B64 = open("D:/CC/ChaosCraft/tools/gen_textures/m06_b64.txt").read()
TEX1_B64 = open("D:/CC/ChaosCraft/tools/gen_textures/m06_tex1_b64.txt").read()

def uid(): return str(uuid.uuid4())

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

elements = []
outliner_bones = []
bone_uuids = {}

def add_elem(name, frm, to, tex=0):
    e = make_elem(name, frm, to, tex)
    elements.append(e)
    return e["uuid"]

# ========== RAY SPHERE DISTRIBUTION ==========
# 10 long rays: 2 cubes each (base 0.5x0.5x4.0, tip 0.3x0.3x3.0)
# Sphere distribution: varied pitch and yaw
long_ray_angles = [
    (0, 0),    (36, 5),   (72, -8),  (108, 12), (144, -5),
    (180, 40), (216, 55), (252, 70), (288, -35), (324, -50)
]

for i, (yaw_deg, pitch_deg) in enumerate(long_ray_angles):
    yaw = math.radians(yaw_deg)
    pitch = math.radians(pitch_deg)
    # Base cube: extends along Z from 0 to 4.0
    e1 = add_elem(f"ray_long_{i+1}_base", [-0.25, -0.25, 0], [0.25, 0.25, 4.0])
    # Tip cube: from 4.0 to 7.0
    e2 = add_elem(f"ray_long_{i+1}_tip", [-0.15, -0.15, 4.0], [0.15, 0.15, 7.0])
    b_uid = uid()
    # bone at origin, rotated by pitch (X) and yaw (Y)
    bone = {"name":f"ray_long_{i+1}","origin":[0,5,0],
            "rotation":[pitch_deg, yaw_deg, 0],
            "uuid":b_uid,"export":True,"isOpen":True,"children":[e1,e2]}
    outliner_bones.append(bone)
    bone_uuids[f"ray_long_{i+1}"] = b_uid

# 10 short rays: 2 cubes each
short_ray_angles = [
    (18, 15),  (54, -20), (90, 30),  (126, -25), (162, 18),
    (198, -40),(234, 45), (270, -15), (306, 60), (342, -55)
]

for i, (yaw_deg, pitch_deg) in enumerate(short_ray_angles):
    e1 = add_elem(f"ray_short_{i+1}_base", [-0.2, -0.2, 0], [0.2, 0.2, 3.0])
    e2 = add_elem(f"ray_short_{i+1}_tip", [-0.125, -0.125, 3.0], [0.125, 0.125, 5.0])
    b_uid = uid()
    bone = {"name":f"ray_short_{i+1}","origin":[0,5,0],
            "rotation":[pitch_deg, yaw_deg, 0],
            "uuid":b_uid,"export":True,"isOpen":True,"children":[e1,e2]}
    outliner_bones.append(bone)
    bone_uuids[f"ray_short_{i+1}"] = b_uid

# ========== AUTHORITY ORB: 6 cubes at Y=5, slight offsets ==========
orb_sizes = [1.5, 1.2, 1.0, 0.8, 0.6, 0.4]
orb_offsets = [(0,0,0),(0.1,0.05,0.1),(-0.1,0.1,-0.05),(0.05,-0.1,0.1),(-0.05,0.05,-0.1),(0,0.08,0)]
orb_children = []
for i, (sz, off) in enumerate(zip(orb_sizes, orb_offsets)):
    h = sz/2
    e = add_elem(f"orb_cube_{i+1}",
                 [off[0]-h, 5+off[1]-h, off[2]-h],
                 [off[0]+h, 5+off[1]+h, off[2]+h], tex=0)
    orb_children.append(e)

b_uid_orb = uid()
bone_orb = {"name":"authority_orb","origin":[0,5,0],"rotation":[0,0,0],
            "uuid":b_uid_orb,"export":True,"isOpen":True,"children":orb_children}
outliner_bones.append(bone_orb)
bone_uuids["authority_orb"] = b_uid_orb

# ========== GROUND CIRCLE: 14 bones, radius 6, Y=0, tex1 ==========
for i in range(14):
    angle = (i / 14) * 360.0
    rad = math.radians(angle)
    cx = 6 * math.cos(rad)
    cz = 6 * math.sin(rad)
    e = add_elem(f"gc_{i+1}",
                 [cx-0.6, -0.075, cz-0.2],
                 [cx+0.6, 0.075, cz+0.2], tex=1)
    b_uid = uid()
    bone = {"name":f"gc_{i+1}","origin":[cx,0,cz],"rotation":[0,angle,0],
            "uuid":b_uid,"export":True,"isOpen":True,"children":[e]}
    outliner_bones.append(bone)
    bone_uuids[f"gc_{i+1}"] = b_uid

# ========== DECREE RINGS: 3 bones, 10 children each ==========
# dr_y0 at Y=0, dr_y3 at Y=3, dr_y6 at Y=6
for ring_idx, (ring_name, ring_y, ring_r) in enumerate([("dr_y0",0.0,3.0),("dr_y3",3.0,4.0),("dr_y6",6.0,5.0)]):
    ring_children = []
    for j in range(10):
        ang = (j/10)*360.0
        rad = math.radians(ang)
        cx = ring_r * math.cos(rad)
        cz = ring_r * math.sin(rad)
        e = add_elem(f"{ring_name}_slab_{j+1}",
                     [cx-0.5, ring_y-0.075, cz-0.15],
                     [cx+0.5, ring_y+0.075, cz+0.15], tex=1)
        ring_children.append(e)
    b_uid = uid()
    bone = {"name":ring_name,"origin":[0,ring_y,0],"rotation":[0,0,0],
            "uuid":b_uid,"export":True,"isOpen":True,"children":ring_children}
    outliner_bones.append(bone)
    bone_uuids[ring_name] = b_uid

# ========== AUTHORITY SEALS: 6 bones, cross shape (2 cubes each) ==========
for i in range(6):
    angle = (i/6)*360.0
    rad = math.radians(angle)
    cx = 4.0 * math.cos(rad)
    cz = 4.0 * math.sin(rad)
    # cross: horizontal bar + vertical bar
    e1 = add_elem(f"seal_{i+1}_h", [cx-1.0, 4.45, cz-0.05], [cx+1.0, 4.55, cz+0.05])
    e2 = add_elem(f"seal_{i+1}_v", [cx-0.05, 3.95, cz-0.4], [cx+0.05, 4.95, cz+0.4])
    b_uid = uid()
    bone = {"name":f"seal_{i+1}","origin":[cx,4.5,cz],"rotation":[0,angle,0],
            "uuid":b_uid,"export":True,"isOpen":True,"children":[e1,e2]}
    outliner_bones.append(bone)
    bone_uuids[f"seal_{i+1}"] = b_uid

# ========== ORB ACCENTS: 6 small orbiting pieces ==========
for i in range(6):
    angle = (i/6)*360.0
    rad = math.radians(angle)
    cx = 2.5 * math.cos(rad)
    cz = 2.5 * math.sin(rad)
    cy = 5.0 + 0.8 * math.sin(math.radians(angle*0.5))
    e = add_elem(f"orb_acc_{i+1}", [cx-0.15, cy-0.15, cz-0.15], [cx+0.15, cy+0.15, cz+0.15])
    b_uid = uid()
    bone = {"name":f"orb_acc_{i+1}","origin":[0,5,0],"rotation":[0,angle,0],
            "uuid":b_uid,"export":True,"isOpen":True,"children":[e]}
    outliner_bones.append(bone)
    bone_uuids[f"orb_acc_{i+1}"] = b_uid

# ========== EXTRA ACCENT BONES (5 more to hit 55+) ==========
# ray accent tips — glowing tips at extreme outer radius
for i in range(5):
    angle = (i/5)*360.0
    rad = math.radians(angle)
    cx = 7.5 * math.cos(rad)
    cz = 7.5 * math.sin(rad)
    e = add_elem(f"ray_ext_{i+1}", [cx-0.1, 4.9, cz-0.1], [cx+0.1, 5.1, cz+0.1])
    b_uid = uid()
    bone = {"name":f"ray_ext_{i+1}","origin":[cx,5,cz],"rotation":[0,angle,0],
            "uuid":b_uid,"export":True,"isOpen":True,"children":[e]}
    outliner_bones.append(bone)
    bone_uuids[f"ray_ext_{i+1}"] = b_uid

print(f"Elements: {len(elements)}, Bones: {len(outliner_bones)}")
assert len(elements) >= 80, f"Need 80+ elements, got {len(elements)}"
assert len(outliner_bones) >= 55, f"Need 55+ bones, got {len(outliner_bones)}"

# ========== ANIMATIONS ==========
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
    """spawn (once, 0.8s)"""
    animators = {}
    for bn in all_bone_names:
        kfs = []
        if bn.startswith("ray_long_"):
            # blast outward at 0.3s
            kfs += [
                scale_kf(0.0, 0.0, 0.0, 0.0),
                scale_kf(0.28, 0.0, 0.0, 0.0),
                scale_kf(0.32, 1.5, 1.5, 1.5),
                scale_kf(0.5, 0.9, 0.9, 0.9),
                scale_kf(0.65, 1.05, 1.05, 1.05),
                scale_kf(0.8, 1.0, 1.0, 1.0),
            ]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(0.8,0,0,0)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.8,0,0,0)]
        elif bn.startswith("ray_short_"):
            # follow at 0.4s
            kfs += [
                scale_kf(0.0, 0.0, 0.0, 0.0),
                scale_kf(0.38, 0.0, 0.0, 0.0),
                scale_kf(0.43, 1.4, 1.4, 1.4),
                scale_kf(0.55, 0.85, 0.85, 0.85),
                scale_kf(0.7, 1.05, 1.05, 1.05),
                scale_kf(0.8, 1.0, 1.0, 1.0),
            ]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(0.8,0,0,0)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.8,0,0,0)]
        elif bn == "authority_orb":
            # compression then burst
            kfs += [
                scale_kf(0.0, 0.0, 0.0, 0.0),
                scale_kf(0.1, 0.3, 0.3, 0.3),
                scale_kf(0.3, 1.5, 1.5, 1.5),
                scale_kf(0.5, 0.9, 0.9, 0.9),
                scale_kf(0.65, 1.05, 1.05, 1.05),
                scale_kf(0.8, 1.0, 1.0, 1.0),
            ]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(0.3,15,30,15), rot_kf(0.8,0,0,0)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.8,0,0,0)]
        elif bn.startswith("gc_"):
            kfs += [
                scale_kf(0.0, 0.0, 1.0, 0.0),
                scale_kf(0.2, 1.3, 1.0, 1.3),
                scale_kf(0.4, 0.9, 1.0, 0.9),
                scale_kf(0.6, 1.0, 1.0, 1.0),
                scale_kf(0.7, 1.0, 1.0, 1.0),
                scale_kf(0.8, 1.0, 1.0, 1.0),
            ]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(0.8,0,0,0)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.8,0,0,0)]
        elif bn == "dr_y0":
            kfs += [
                scale_kf(0.0, 0.0, 0.0, 0.0),
                scale_kf(0.28, 0.0, 0.0, 0.0),
                scale_kf(0.38, 1.5, 1.0, 1.5),
                scale_kf(0.5, 0.9, 1.0, 0.9),
                scale_kf(0.65, 1.0, 1.0, 1.0),
                scale_kf(0.8, 1.0, 1.0, 1.0),
            ]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(0.8,0,5,0)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.8,0,0,0)]
        elif bn == "dr_y3":
            kfs += [
                scale_kf(0.0, 0.0, 0.0, 0.0),
                scale_kf(0.33, 0.0, 0.0, 0.0),
                scale_kf(0.43, 1.5, 1.0, 1.5),
                scale_kf(0.55, 0.9, 1.0, 0.9),
                scale_kf(0.68, 1.0, 1.0, 1.0),
                scale_kf(0.8, 1.0, 1.0, 1.0),
            ]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(0.8,0,-5,0)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.8,0,0,0)]
        elif bn == "dr_y6":
            kfs += [
                scale_kf(0.0, 0.0, 0.0, 0.0),
                scale_kf(0.38, 0.0, 0.0, 0.0),
                scale_kf(0.48, 1.5, 1.0, 1.5),
                scale_kf(0.6, 0.9, 1.0, 0.9),
                scale_kf(0.72, 1.0, 1.0, 1.0),
                scale_kf(0.8, 1.0, 1.0, 1.0),
            ]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(0.8,0,8,0)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.8,0,0,0)]
        elif bn.startswith("seal_"):
            kfs += [
                scale_kf(0.0, 0.0, 0.0, 0.0),
                rot_kf(0.0, 0, 0, 0),
                scale_kf(0.3, 0.0, 0.0, 0.0),
                rot_kf(0.3, 0, -90, 0),
                scale_kf(0.4, 1.3, 1.3, 1.3),
                rot_kf(0.5, 0, 0, 0),
                scale_kf(0.65, 0.95, 0.95, 0.95),
                scale_kf(0.8, 1.0, 1.0, 1.0),
            ]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.8,0,0,0)]
        else:
            # orb_acc, ray_ext
            kfs += [
                scale_kf(0.0, 0.0, 0.0, 0.0),
                scale_kf(0.3, 1.2, 1.2, 1.2),
                scale_kf(0.45, 0.9, 0.9, 0.9),
                scale_kf(0.6, 1.0, 1.0, 1.0),
                scale_kf(0.7, 1.0, 1.0, 1.0),
                scale_kf(0.8, 1.0, 1.0, 1.0),
            ]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(0.8,0,0,0)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.8,0,0,0)]

        animators[bone_uuids[bn]] = {"name": bn, "type": "bone", "keyframes": kfs}
    return animators

def make_animators_idle():
    """idle (loop, 4.0s)"""
    animators = {}
    for i, bn in enumerate(all_bone_names):
        kfs = []
        if bn.startswith("ray_long_"):
            # pulse scale in waves
            phase = (i * 0.4) % 4.0
            kfs += [
                scale_kf(0.0, 1.0, 1.0, 1.0),
                scale_kf(0.7, 1.1, 1.1, 1.1),
                scale_kf(1.4, 0.95, 0.95, 0.95),
                scale_kf(2.1, 1.08, 1.08, 1.08),
                scale_kf(3.2, 0.97, 0.97, 0.97),
                scale_kf(4.0, 1.0, 1.0, 1.0),
            ]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(2.0,0,3,0), rot_kf(4.0,0,0,0)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(4.0,0,0,0)]
        elif bn.startswith("ray_short_"):
            # opposite phase
            kfs += [
                scale_kf(0.0, 0.95, 0.95, 0.95),
                scale_kf(0.7, 1.0, 1.0, 1.0),
                scale_kf(1.4, 1.1, 1.1, 1.1),
                scale_kf(2.1, 0.95, 0.95, 0.95),
                scale_kf(3.2, 1.05, 1.05, 1.05),
                scale_kf(4.0, 0.95, 0.95, 0.95),
            ]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(2.0,0,-3,0), rot_kf(4.0,0,0,0)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(4.0,0,0,0)]
        elif bn == "authority_orb":
            # rotates all 3 axes
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(1.0, 15, 30, 10),
                rot_kf(2.0, 30, 60, 20),
                rot_kf(3.0, 45, 90, 30),
                rot_kf(3.5, 50, 105, 35),
                rot_kf(4.0, 60, 120, 40),
            ]
            kfs += [scale_kf(0.0,1,1,1), scale_kf(2.0,1.03,1.03,1.03), scale_kf(4.0,1,1,1)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(4.0,0,0,0)]
        elif bn.startswith("gc_"):
            # breathe and rotate
            kfs += [
                scale_kf(0.0, 1.0, 1.0, 1.0),
                rot_kf(0.0, 0, 0, 0),
                scale_kf(1.0, 1.05, 1.0, 1.05),
                rot_kf(1.0, 0, 8, 0),
                scale_kf(2.0, 0.97, 1.0, 0.97),
                rot_kf(2.0, 0, 16, 0),
                scale_kf(3.0, 1.03, 1.0, 1.03),
                rot_kf(3.0, 0, 24, 0),
                scale_kf(4.0, 1.0, 1.0, 1.0),
                rot_kf(4.0, 0, 32, 0),
            ]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(4.0,0,0,0)]
        elif bn == "dr_y0":
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(1.0, 0, -20, 0),
                rot_kf(2.0, 0, -40, 0),
                rot_kf(3.0, 0, -60, 0),
                rot_kf(3.5, 0, -70, 0),
                rot_kf(4.0, 0, -80, 0),
            ]
            kfs += [scale_kf(0.0,1,1,1), scale_kf(4.0,1,1,1)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(4.0,0,0,0)]
        elif bn == "dr_y3":
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(1.0, 0, 25, 0),
                rot_kf(2.0, 0, 50, 0),
                rot_kf(3.0, 0, 75, 0),
                rot_kf(3.5, 0, 87.5, 0),
                rot_kf(4.0, 0, 100, 0),
            ]
            kfs += [scale_kf(0.0,1,1,1), scale_kf(4.0,1,1,1)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(4.0,0,0,0)]
        elif bn == "dr_y6":
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(1.0, 0, -15, 0),
                rot_kf(2.0, 0, -30, 0),
                rot_kf(3.0, 0, -45, 0),
                rot_kf(3.5, 0, -52.5, 0),
                rot_kf(4.0, 0, -60, 0),
            ]
            kfs += [scale_kf(0.0,1,1,1), scale_kf(4.0,1,1,1)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(4.0,0,0,0)]
        elif bn.startswith("seal_"):
            # orbit equator
            idx = int(bn.split("_")[1])
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(1.0, 0, idx*12, 0),
                rot_kf(2.0, 0, idx*24, 0),
                rot_kf(3.0, 0, idx*36, 0),
                rot_kf(3.5, 0, idx*42, 0),
                rot_kf(4.0, 0, idx*48, 0),
            ]
            kfs += [scale_kf(0.0,1,1,1), scale_kf(2.0,1.05,1.05,1.05), scale_kf(4.0,1,1,1)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(4.0,0,0,0)]
        elif bn.startswith("orb_acc_"):
            idx = int(bn.split("_")[2])
            kfs += [
                rot_kf(0.0, 0, 0, 0),
                rot_kf(1.0, 0, idx*20, 0),
                rot_kf(2.0, 0, idx*40, 0),
                rot_kf(3.0, 0, idx*60, 0),
                rot_kf(3.5, 0, idx*70, 0),
                rot_kf(4.0, 0, idx*80, 0),
            ]
            kfs += [scale_kf(0.0,1,1,1), scale_kf(4.0,1,1,1)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(4.0,0,0,0)]
        else:
            # ray_ext
            kfs += [
                scale_kf(0.0, 1.0, 1.0, 1.0),
                scale_kf(0.8, 1.1, 1.1, 1.1),
                scale_kf(1.6, 0.9, 0.9, 0.9),
                scale_kf(2.4, 1.05, 1.05, 1.05),
                scale_kf(3.2, 0.95, 0.95, 0.95),
                scale_kf(4.0, 1.0, 1.0, 1.0),
            ]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(2.0,0,15,0), rot_kf(4.0,0,30,0)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(4.0,0,0,0)]

        animators[bone_uuids[bn]] = {"name": bn, "type": "bone", "keyframes": kfs}
    return animators

def make_animators_dissipate():
    """dissipate (once, 0.5s)"""
    animators = {}
    for bn in all_bone_names:
        kfs = []
        if bn.startswith("ray_long_") or bn.startswith("ray_short_"):
            # continue outward and snap to 0
            kfs += [
                scale_kf(0.0, 1.0, 1.0, 1.0),
                scale_kf(0.15, 1.5, 1.5, 1.5),
                scale_kf(0.3, 2.5, 2.5, 2.5),
                scale_kf(0.4, 4.0, 4.0, 4.0),
                scale_kf(0.45, 5.0, 5.0, 5.0),
                scale_kf(0.5, 0.0, 0.0, 0.0),
            ]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(0.5,0,10,0)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.5,0,0,0)]
        elif bn == "authority_orb":
            # rapidly expands then collapses
            kfs += [
                scale_kf(0.0, 1.0, 1.0, 1.0),
                scale_kf(0.15, 2.0, 2.0, 2.0),
                scale_kf(0.25, 3.0, 3.0, 3.0),
                scale_kf(0.35, 0.5, 0.5, 0.5),
                scale_kf(0.45, 0.1, 0.1, 0.1),
                scale_kf(0.5, 0.0, 0.0, 0.0),
            ]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(0.25,45,90,45), rot_kf(0.5,90,180,90)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.5,0,0,0)]
        elif bn.startswith("gc_") or bn.startswith("dr_"):
            # expand then snap
            kfs += [
                scale_kf(0.0, 1.0, 1.0, 1.0),
                scale_kf(0.15, 1.5, 1.0, 1.5),
                scale_kf(0.3, 2.5, 1.0, 2.5),
                scale_kf(0.4, 3.5, 1.0, 3.5),
                scale_kf(0.45, 5.0, 1.0, 5.0),
                scale_kf(0.5, 0.0, 0.0, 0.0),
            ]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(0.5,0,20,0)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.5,0,0,0)]
        elif bn.startswith("seal_"):
            kfs += [
                scale_kf(0.0, 1.0, 1.0, 1.0),
                scale_kf(0.1, 1.3, 1.3, 1.3),
                rot_kf(0.0, 0, 0, 0),
                rot_kf(0.2, 0, 45, 0),
                scale_kf(0.25, 2.0, 2.0, 2.0),
                scale_kf(0.35, 3.0, 3.0, 3.0),
                scale_kf(0.45, 4.0, 4.0, 4.0),
                scale_kf(0.5, 0.0, 0.0, 0.0),
            ]
            kfs += [rot_kf(0.5, 0, 90, 0)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.5,0,0,0)]
        else:
            kfs += [
                scale_kf(0.0, 1.0, 1.0, 1.0),
                scale_kf(0.1, 1.5, 1.5, 1.5),
                scale_kf(0.25, 2.5, 2.5, 2.5),
                scale_kf(0.35, 3.5, 3.5, 3.5),
                scale_kf(0.45, 4.5, 4.5, 4.5),
                scale_kf(0.5, 0.0, 0.0, 0.0),
            ]
            kfs += [rot_kf(0.0,0,0,0), rot_kf(0.5,0,15,0)]
            kfs += [pos_kf(0.0,0,0,0), pos_kf(0.5,0,0,0)]

        animators[bone_uuids[bn]] = {"name": bn, "type": "bone", "keyframes": kfs}
    return animators

animations = [
    {
        "uuid": uid(),
        "name": "animation.devils_sermon_nova.spawn",
        "loop": "once",
        "length": 0.8,
        "snapping": 24,
        "selected": False,
        "saved": False,
        "animators": make_animators_spawn()
    },
    {
        "uuid": uid(),
        "name": "animation.devils_sermon_nova.idle",
        "loop": "loop",
        "length": 4.0,
        "snapping": 24,
        "selected": False,
        "saved": False,
        "animators": make_animators_idle()
    },
    {
        "uuid": uid(),
        "name": "animation.devils_sermon_nova.dissipate",
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
        "id": "0", "name": "devils_sermon_nova_tex",
        "relative_path": "../textures/devils_sermon_nova_tex.png",
        "folder": "devilsdream", "namespace": "", "visible": True,
        "mode": "bitmap", "saved": False, "uuid": uid(),
        "source": f"data:image/png;base64,{TEX0_B64}"
    },
    {
        "id": "1", "name": "devils_sermon_nova_tex1",
        "relative_path": "../textures/devils_sermon_nova_tex1.png",
        "folder": "devilsdream", "namespace": "", "visible": True,
        "mode": "bitmap", "saved": False, "uuid": uid(),
        "source": f"data:image/png;base64,{TEX1_B64}"
    }
]

bbmodel = {
    "meta": {"format_version": "4.10", "model_format": "free", "box_uv": False},
    "name": "devils_sermon_nova",
    "geometry": "geometry.devils_sermon_nova",
    "resolution": {"width": 64, "height": 64},
    "elements": elements,
    "outliner": outliner_bones,
    "textures": textures,
    "animations": animations
}

out_path = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/devils_sermon_nova.bbmodel"
with open(out_path, "w") as f:
    json.dump(bbmodel, f, separators=(',', ':'))

sz = os.path.getsize(out_path)
print(f"Written: {out_path}")
print(f"Size: {sz} bytes ({sz/1024:.1f} KB)")
if sz < 300*1024:
    print(f"WARNING: File is {sz/1024:.1f}KB, need 300KB!")
else:
    print("OK: >=300KB")
