"""Build fallen_angel_descent.bbmodel — 85 elements, 58 bones, 3 animations, 300KB+ target."""
import json, math, os

# ── helpers ────────────────────────────────────────────────────────────────
def kf(uid, time, channel, vals):
    return {"uuid": uid, "time": time, "color": -1, "interpolation": "catmullrom",
            "channel": channel,
            "data_points": [{c: str(v) for c, v in zip("xyz", vals)}]}

TEX0_B64 = open("D:/CC/ChaosCraft/tools/gen_textures/m01_b64.txt").read().strip()
TEX1_B64 = open("D:/CC/ChaosCraft/tools/gen_textures/m01_tex1_b64.txt").read().strip()

# UV assignments
UV_NORTH  = [2, 2, 30, 30]
UV_EAST   = [34, 2, 62, 30]
UV_SOUTH  = [2, 2, 30, 30]
UV_WEST   = [34, 2, 62, 30]
UV_UP     = [2, 34, 30, 62]
UV_DOWN   = [34, 34, 62, 62]
UV_EMISSIVE = [34, 34, 62, 62]

def faces(tex=0, emissive=False):
    if emissive:
        return {d: {"uv": UV_EMISSIVE, "texture": tex} for d in ["north","east","south","west","up","down"]}
    return {
        "north": {"uv": UV_NORTH, "texture": tex},
        "east":  {"uv": UV_EAST,  "texture": tex},
        "south": {"uv": UV_SOUTH, "texture": tex},
        "west":  {"uv": UV_WEST,  "texture": tex},
        "up":    {"uv": UV_UP,    "texture": tex},
        "down":  {"uv": UV_DOWN,  "texture": tex},
    }

def elem(uuid, name, frm, to, tex=0, emissive=False):
    return {
        "name": name, "box_uv": False, "rescale": False, "locked": False,
        "render_order": "default", "allow_mirror_modeling": True,
        "from": frm, "to": to, "autouv": 0, "color": 0, "uuid": uuid,
        "faces": faces(tex, emissive)
    }

def bone(uuid, name, origin, children, rotation=None):
    b = {"name": name, "origin": origin, "uuid": uuid,
         "export": True, "isOpen": True, "children": children}
    if rotation:
        b["rotation"] = rotation
    else:
        b["rotation"] = [0, 0, 0]
    return b

elements = []
bones_list = []
e_idx = 1
b_idx = 1
k_idx = 1

def eid():
    global e_idx
    s = f"e{e_idx:03d}"; e_idx += 1; return s

def bid():
    global b_idx
    s = f"b{b_idx:03d}"; b_idx += 1; return s

def kid():
    global k_idx
    s = f"k{k_idx:04d}"; k_idx += 1; return s

# ── GEOMETRY SECTION ────────────────────────────────────────────────────────
# 1) 10 crater rim cluster bones — radius 12, angles 0,36,72,...324°
#    Each cluster: 3 overlapping flat slabs tilted outward
rim_bone_ids = []
for i in range(10):
    angle_deg = i * 36.0
    angle = math.radians(angle_deg)
    cx = math.sin(angle) * 12.0
    cz = math.cos(angle) * 12.0
    # tilt outward: rotation around Y-axis so that the slab faces away from origin
    tilt_rot = [15.0 + (i % 3) * 5.0, angle_deg, 0]  # tilt 15-25° outward
    cluster_elems = []
    for j in range(3):
        ew = 2.5 - j * 0.3
        eh = 0.4 + j * 0.15
        ez = 0.3 + j * 0.25
        eid_val = eid()
        # place relative to bone origin
        frm = [-ew/2, j * 0.4, -ez/2]
        to  = [ ew/2, j * 0.4 + eh, ez/2]
        elements.append(elem(eid_val, f"rim_{i+1}_slab_{j+1}", frm, to, tex=0))
        cluster_elems.append(eid_val)
    b_uuid = bid()
    rim_bone_ids.append(b_uuid)
    bones_list.append(bone(b_uuid, f"crater_rim_{i+1}", [cx, 0.0, cz], cluster_elems, rotation=tilt_rot))

# 2) crater_disc bone: 5 flat slabs at Y=-0.5 (emissive tex1)
disc_elems = []
for i in range(5):
    eid_val = eid()
    w = 2.5 + i * 0.5
    d = 2.0 + i * 0.4
    off_x = (i - 2) * 0.8
    off_z = (i - 2) * 0.6
    frm = [off_x - w/2, -0.6, off_z - d/2]
    to  = [off_x + w/2, -0.5, off_z + d/2]
    elements.append(elem(eid_val, f"crater_disc_{i+1}", frm, to, tex=1, emissive=True))
    disc_elems.append(eid_val)
disc_bone_id = bid()
bones_list.append(bone(disc_bone_id, "crater_disc", [0, 0, 0], disc_elems))

# 3) 14 shockwave ring bones — radius 18, thin upright slabs
shock_bone_ids = []
for i in range(14):
    angle_deg = i * (360.0 / 14.0)
    angle = math.radians(angle_deg)
    sx = math.sin(angle) * 18.0
    sz = math.cos(angle) * 18.0
    eid_val = eid()
    # thin upright slab 0.3×2.5×0.3
    frm = [-0.15, 0.0, -0.15]
    to  = [ 0.15, 2.5,  0.15]
    elements.append(elem(eid_val, f"shockwave_{i+1}_slab", frm, to, tex=1, emissive=True))
    b_uuid = bid()
    shock_bone_ids.append(b_uuid)
    rot_y = angle_deg
    bones_list.append(bone(b_uuid, f"shockwave_{i+1}", [sx, 0.0, sz], [eid_val], rotation=[0, rot_y, 0]))

# 4) 6 impact spire bone chains — 3 bones each (base/mid/tip)
#    Heights 5-9, placed at radius 4-8 from origin inside the rim
spire_bone_ids = []  # list of (base_id, mid_id, tip_id)
for i in range(6):
    angle_deg = i * 60.0
    angle = math.radians(angle_deg)
    r = 4.0 + i * 0.7
    px_c = math.sin(angle) * r
    pz_c = math.cos(angle) * r
    height = 5.0 + i * 0.65  # 5 to ~8.25

    # Base cube: 1.2×2×1.2
    base_eid = eid()
    elements.append(elem(base_eid, f"spire_{i+1}_base_cube", [-0.6, 0.0, -0.6], [0.6, 2.0, 0.6], tex=0))
    # Mid cube: 0.8×2×0.8 at Y=2
    mid_eid = eid()
    elements.append(elem(mid_eid, f"spire_{i+1}_mid_cube", [-0.4, 0.0, -0.4], [0.4, 2.0, 0.4], tex=0))
    # Tip cube: 0.4×1.5×0.4 at Y=4
    tip_eid = eid()
    elements.append(elem(tip_eid, f"spire_{i+1}_tip_cube", [-0.2, 0.0, -0.2], [0.2, 1.5, 0.2], tex=0))

    base_bid = bid()
    mid_bid  = bid()
    tip_bid  = bid()
    spire_bone_ids.append((base_bid, mid_bid, tip_bid))

    # Nested chain: tip inside mid inside base
    tip_bone = bone(tip_bid,  f"spire_{i+1}_tip",  [px_c, height,     pz_c], [tip_eid])
    mid_bone = bone(mid_bid,  f"spire_{i+1}_mid",  [px_c, height-1.5, pz_c], [mid_eid, tip_bone])
    base_bone= bone(base_bid, f"spire_{i+1}_base", [px_c, 0.0,        pz_c], [base_eid, mid_bone])
    bones_list.append(base_bone)

# 5) crater_glow bone: 4 flat overlapping slabs at Y=0, emissive tex1
glow_elems = []
for i in range(4):
    eid_val = eid()
    w = 3.0 + i * 0.7
    d = 3.0 + i * 0.7
    off = (i - 1.5) * 0.3
    frm = [off - w/2, -0.05, off - d/2]
    to  = [off + w/2,  0.05, off + d/2]
    elements.append(elem(eid_val, f"crater_glow_{i+1}", frm, to, tex=1, emissive=True))
    glow_elems.append(eid_val)
glow_bone_id = bid()
bones_list.append(bone(glow_bone_id, "crater_glow", [0, 0, 0], glow_elems))

# 6) 8 fragment bones — small irregular cube pieces ejected outward
frag_bone_ids = []
for i in range(8):
    angle_deg = i * 45.0
    angle = math.radians(angle_deg)
    r = 6.0 + (i % 3) * 2.5
    fx = math.sin(angle) * r
    fz = math.cos(angle) * r
    fy = 1.0 + (i % 4) * 0.8
    s = 0.4 + (i % 3) * 0.2
    eid_val = eid()
    frm = [-s/2, -s/2, -s/2]
    to  = [ s/2,  s/2,  s/2]
    elements.append(elem(eid_val, f"fragment_{i+1}_cube", frm, to, tex=0))
    b_uuid = bid()
    frag_bone_ids.append(b_uuid)
    bones_list.append(bone(b_uuid, f"fragment_{i+1}", [fx, fy, fz], [eid_val],
                           rotation=[(i*17) % 45, (i*23) % 90, (i*11) % 30]))

# 7) 6 accent bones — small slab pieces at rim base
accent_bone_ids = []
for i in range(6):
    angle_deg = i * 60.0 + 18.0  # between rim clusters
    angle = math.radians(angle_deg)
    r = 10.0
    ax = math.sin(angle) * r
    az = math.cos(angle) * r
    eid_val = eid()
    frm = [-0.8, 0.0, -0.3]
    to  = [ 0.8, 0.3,  0.3]
    elements.append(elem(eid_val, f"accent_{i+1}_slab", frm, to, tex=0))
    b_uuid = bid()
    accent_bone_ids.append(b_uuid)
    bones_list.append(bone(b_uuid, f"accent_{i+1}", [ax, 0.0, az], [eid_val]))

print(f"Elements: {len(elements)}, Bones: {len(bones_list)}")

# ── ANIMATIONS ──────────────────────────────────────────────────────────────
# Collect ALL bone UUIDs and names for animators
all_bones_flat = []

def collect_bones(bone_obj):
    if isinstance(bone_obj, dict) and "uuid" in bone_obj and "name" in bone_obj:
        all_bones_flat.append((bone_obj["uuid"], bone_obj["name"]))
        for c in bone_obj.get("children", []):
            if isinstance(c, dict):
                collect_bones(c)

for b in bones_list:
    collect_bones(b)

print(f"All bones (flat): {len(all_bones_flat)}")

def make_animators_spawn():
    """spawn: once, 1.1s — scale-in from below, shockwaves expand, spires erupt."""
    animators = {}
    # shockwave bones: scale from 0 at center expand to full radius over 0.3s
    for i, bid_val in enumerate(shock_bone_ids):
        bname = f"shockwave_{i+1}"
        animators[bid_val] = {
            "name": bname, "type": "bone", "keyframes": [
                kf(kid(), 0.0, "scale",    [0, 0, 0]),
                kf(kid(), 0.15,"scale",    [0.5,1,0.5]),
                kf(kid(), 0.3, "scale",    [1, 1, 1]),
                kf(kid(), 1.1, "scale",    [1, 1, 1]),
                kf(kid(), 0.0, "position", [0,-3,0]),
                kf(kid(), 0.3, "position", [0, 0, 0]),
                kf(kid(), 0.0, "rotation", [0,0,0]),
                kf(kid(), 0.5, "rotation", [0, i*5.0, 0]),
                kf(kid(), 1.1, "rotation", [0, i*5.0, 0]),
            ]
        }
    # rim cluster bones: blast upward at 0.1s staggered clockwise 0.05s each
    for i, bid_val in enumerate(rim_bone_ids):
        bname = f"crater_rim_{i+1}"
        t_start = 0.1 + i * 0.05
        t_land  = min(t_start + 0.3, 1.0)
        animators[bid_val] = {
            "name": bname, "type": "bone", "keyframes": [
                kf(kid(), 0.0,     "position", [0,-3,0]),
                kf(kid(), t_start, "position", [0,-3,0]),
                kf(kid(), t_land,  "position", [0, 0, 0]),
                kf(kid(), 1.1,     "position", [0, 0, 0]),
                kf(kid(), 0.0,     "scale",    [0.5,0.5,0.5]),
                kf(kid(), t_land,  "scale",    [1,1,1]),
                kf(kid(), 0.0,     "rotation", [0,0,0]),
                kf(kid(), t_land,  "rotation", [0, i*36.0, 0]),
                kf(kid(), 1.1,     "rotation", [0, i*36.0, 0]),
            ]
        }
    # crater_disc: appears at 0.2s
    animators[disc_bone_id] = {
        "name": "crater_disc", "type": "bone", "keyframes": [
            kf(kid(), 0.0, "scale",    [0,0,0]),
            kf(kid(), 0.2, "scale",    [0.2,1,0.2]),
            kf(kid(), 0.5, "scale",    [1,1,1]),
            kf(kid(), 1.1, "scale",    [1,1,1]),
            kf(kid(), 0.0, "position", [0,-2,0]),
            kf(kid(), 0.5, "position", [0,0,0]),
            kf(kid(), 0.0, "rotation", [0,0,0]),
            kf(kid(), 1.1, "rotation", [0,0,0]),
        ]
    }
    # spire bones: erupt at 0.4s staggered 0.06s each
    for i, (base_bid_v, mid_bid_v, tip_bid_v) in enumerate(spire_bone_ids):
        t_start = 0.4 + i * 0.06
        t_land  = min(t_start + 0.35, 1.05)
        for (bval, bname, extra_y) in [
            (base_bid_v, f"spire_{i+1}_base", 0),
            (mid_bid_v,  f"spire_{i+1}_mid",  0),
            (tip_bid_v,  f"spire_{i+1}_tip",  0),
        ]:
            animators[bval] = {
                "name": bname, "type": "bone", "keyframes": [
                    kf(kid(), 0.0,     "position", [0,-5,0]),
                    kf(kid(), t_start, "position", [0,-5,0]),
                    kf(kid(), t_land,  "position", [0, 0, 0]),
                    kf(kid(), 1.1,     "position", [0, 0, 0]),
                    kf(kid(), 0.0,     "scale",    [0.2,0.2,0.2]),
                    kf(kid(), t_land,  "scale",    [1,1,1]),
                    kf(kid(), 0.0,     "rotation", [0,0,0]),
                    kf(kid(), t_land,  "rotation", [0, i*60.0, 0]),
                    kf(kid(), 1.1,     "rotation", [0, i*60.0, 0]),
                ]
            }
    # crater_glow: phases in at 0.2s
    animators[glow_bone_id] = {
        "name": "crater_glow", "type": "bone", "keyframes": [
            kf(kid(), 0.0, "scale",    [0,0,0]),
            kf(kid(), 0.2, "scale",    [0.3,1,0.3]),
            kf(kid(), 0.6, "scale",    [1,1,1]),
            kf(kid(), 1.1, "scale",    [1,1,1]),
            kf(kid(), 0.0, "position", [0,0,0]),
            kf(kid(), 1.1, "position", [0,0,0]),
            kf(kid(), 0.0, "rotation", [0,0,0]),
            kf(kid(), 1.1, "rotation", [0,0,0]),
        ]
    }
    # fragment bones: blast outward at 0.3s staggered
    for i, bid_val in enumerate(frag_bone_ids):
        bname = f"fragment_{i+1}"
        t_start = 0.3 + i * 0.04
        t_land  = min(t_start + 0.3, 1.0)
        animators[bid_val] = {
            "name": bname, "type": "bone", "keyframes": [
                kf(kid(), 0.0,     "position", [0,-1,0]),
                kf(kid(), t_start, "position", [0,-1,0]),
                kf(kid(), t_land,  "position", [0, 0, 0]),
                kf(kid(), 1.1,     "position", [0, 0, 0]),
                kf(kid(), 0.0,     "scale",    [0,0,0]),
                kf(kid(), t_land,  "scale",    [1,1,1]),
                kf(kid(), 0.0,     "rotation", [0,0,0]),
                kf(kid(), t_land,  "rotation", [i*30.0, i*45.0, i*15.0]),
                kf(kid(), 1.1,     "rotation", [i*30.0, i*45.0, i*15.0]),
            ]
        }
    # accent bones
    for i, bid_val in enumerate(accent_bone_ids):
        bname = f"accent_{i+1}"
        animators[bid_val] = {
            "name": bname, "type": "bone", "keyframes": [
                kf(kid(), 0.0, "position", [0,-1,0]),
                kf(kid(), 0.4, "position", [0,-1,0]),
                kf(kid(), 0.7, "position", [0, 0, 0]),
                kf(kid(), 1.1, "position", [0, 0, 0]),
                kf(kid(), 0.0, "scale",    [0.5,0.5,0.5]),
                kf(kid(), 0.7, "scale",    [1,1,1]),
                kf(kid(), 0.0, "rotation", [0,0,0]),
                kf(kid(), 1.1, "rotation", [0, i*60.0, 0]),
            ]
        }
    return animators

def make_animators_idle():
    """idle: loop 6.0s — slow organic motion, rocking, pulsing."""
    animators = {}
    # shockwave: slow Y rotation and scale pulse
    for i, bid_val in enumerate(shock_bone_ids):
        bname = f"shockwave_{i+1}"
        phase = i * (6.0 / 14.0)
        animators[bid_val] = {
            "name": bname, "type": "bone", "keyframes": [
                kf(kid(), 0.0, "rotation", [0, i*5.0,        0]),
                kf(kid(), 1.5, "rotation", [0, i*5.0+10,     0]),
                kf(kid(), 3.0, "rotation", [0, i*5.0+20,     0]),
                kf(kid(), 4.5, "rotation", [0, i*5.0+10,     0]),
                kf(kid(), 6.0, "rotation", [0, i*5.0,        0]),
                kf(kid(), 0.0, "scale",    [1,   1,   1]),
                kf(kid(), 1.5, "scale",    [1.05,1,1.05]),
                kf(kid(), 3.0, "scale",    [1.1, 1, 1.1]),
                kf(kid(), 4.5, "scale",    [1.05,1,1.05]),
                kf(kid(), 6.0, "scale",    [1,   1,   1]),
                kf(kid(), 0.0, "position", [0,0,0]),
                kf(kid(), 3.0, "position", [0, 0.05, 0]),
                kf(kid(), 6.0, "position", [0,0,0]),
            ]
        }
    # rim clusters: rock ±2° on individual periods
    for i, bid_val in enumerate(rim_bone_ids):
        bname = f"crater_rim_{i+1}"
        period = 5.0 + (i % 5) * 0.3
        base_ry = i * 36.0
        animators[bid_val] = {
            "name": bname, "type": "bone", "keyframes": [
                kf(kid(), 0.0, "rotation", [0,        base_ry, 0]),
                kf(kid(), 1.0, "rotation", [2,        base_ry, 1]),
                kf(kid(), 2.0, "rotation", [-2,       base_ry,-1]),
                kf(kid(), 3.0, "rotation", [2,        base_ry, 1]),
                kf(kid(), 4.0, "rotation", [-2,       base_ry,-1]),
                kf(kid(), 6.0, "rotation", [0,        base_ry, 0]),
                kf(kid(), 0.0, "position", [0,0,0]),
                kf(kid(), 3.0, "position", [0, 0.1*math.sin(i), 0]),
                kf(kid(), 6.0, "position", [0,0,0]),
                kf(kid(), 0.0, "scale",    [1,1,1]),
                kf(kid(), 3.0, "scale",    [1.02,1.02,1.02]),
                kf(kid(), 6.0, "scale",    [1,1,1]),
            ]
        }
    # crater_disc: breathe
    animators[disc_bone_id] = {
        "name": "crater_disc", "type": "bone", "keyframes": [
            kf(kid(), 0.0, "scale",    [1,   1,   1]),
            kf(kid(), 1.5, "scale",    [1.05,1.05,1.05]),
            kf(kid(), 3.0, "scale",    [1.1, 1.1, 1.1]),
            kf(kid(), 4.5, "scale",    [1.05,1.05,1.05]),
            kf(kid(), 6.0, "scale",    [1,   1,   1]),
            kf(kid(), 0.0, "position", [0,0,0]),
            kf(kid(), 2.0, "position", [0, 0.02, 0]),
            kf(kid(), 4.0, "position", [0,-0.02, 0]),
            kf(kid(), 6.0, "position", [0,0,0]),
            kf(kid(), 0.0, "rotation", [0,0,0]),
            kf(kid(), 3.0, "rotation", [0,5,0]),
            kf(kid(), 6.0, "rotation", [0,0,0]),
        ]
    }
    # spire bones: wobble secondary motion
    for i, (base_bid_v, mid_bid_v, tip_bid_v) in enumerate(spire_bone_ids):
        for j, (bval, bname) in enumerate([(base_bid_v, f"spire_{i+1}_base"),
                                            (mid_bid_v,  f"spire_{i+1}_mid"),
                                            (tip_bid_v,  f"spire_{i+1}_tip")]):
            phase_off = i * 1.0 + j * 0.5
            animators[bval] = {
                "name": bname, "type": "bone", "keyframes": [
                    kf(kid(), 0.0, "rotation", [0,        i*60.0, 0]),
                    kf(kid(), 1.5, "rotation", [2+j,      i*60.0, 1+j]),
                    kf(kid(), 3.0, "rotation", [-2-j,     i*60.0,-1-j]),
                    kf(kid(), 4.5, "rotation", [2+j,      i*60.0, 1+j]),
                    kf(kid(), 6.0, "rotation", [0,        i*60.0, 0]),
                    kf(kid(), 0.0, "position", [0,0,0]),
                    kf(kid(), 2.0, "position", [0.05*j, 0.05,  0.05*j]),
                    kf(kid(), 4.0, "position", [-0.05*j,0.02, -0.05*j]),
                    kf(kid(), 6.0, "position", [0,0,0]),
                    kf(kid(), 0.0, "scale",    [1,1,1]),
                    kf(kid(), 3.0, "scale",    [1+0.02*j, 1+0.02*j, 1+0.02*j]),
                    kf(kid(), 6.0, "scale",    [1,1,1]),
                ]
            }
    # crater_glow: breathe emissive
    animators[glow_bone_id] = {
        "name": "crater_glow", "type": "bone", "keyframes": [
            kf(kid(), 0.0, "scale",    [1,   1,   1]),
            kf(kid(), 1.5, "scale",    [1.08,1.08,1.08]),
            kf(kid(), 3.0, "scale",    [1.15,1.15,1.15]),
            kf(kid(), 4.5, "scale",    [1.08,1.08,1.08]),
            kf(kid(), 6.0, "scale",    [1,   1,   1]),
            kf(kid(), 0.0, "position", [0,0,0]),
            kf(kid(), 3.0, "position", [0, 0.03, 0]),
            kf(kid(), 6.0, "position", [0,0,0]),
            kf(kid(), 0.0, "rotation", [0, 0, 0]),
            kf(kid(), 2.0, "rotation", [0,10, 0]),
            kf(kid(), 4.0, "rotation", [0,20, 0]),
            kf(kid(), 6.0, "rotation", [0, 0, 0]),
        ]
    }
    # fragments: tumble at unique axes/rates
    for i, bid_val in enumerate(frag_bone_ids):
        bname = f"fragment_{i+1}"
        rx = i * 30.0; ry = i * 45.0; rz = i * 15.0
        animators[bid_val] = {
            "name": bname, "type": "bone", "keyframes": [
                kf(kid(), 0.0, "rotation", [rx,        ry,        rz]),
                kf(kid(), 1.5, "rotation", [rx+15,     ry+20,     rz+10]),
                kf(kid(), 3.0, "rotation", [rx+30,     ry+40,     rz+20]),
                kf(kid(), 4.5, "rotation", [rx+15,     ry+20,     rz+10]),
                kf(kid(), 6.0, "rotation", [rx,        ry,        rz]),
                kf(kid(), 0.0, "position", [0,0,0]),
                kf(kid(), 2.0, "position", [0, 0.05*(i%3+1), 0]),
                kf(kid(), 4.0, "position", [0,-0.03*(i%3+1),0]),
                kf(kid(), 6.0, "position", [0,0,0]),
                kf(kid(), 0.0, "scale",    [1,1,1]),
                kf(kid(), 3.0, "scale",    [1.03,1.03,1.03]),
                kf(kid(), 6.0, "scale",    [1,1,1]),
            ]
        }
    # accent bones
    for i, bid_val in enumerate(accent_bone_ids):
        bname = f"accent_{i+1}"
        animators[bid_val] = {
            "name": bname, "type": "bone", "keyframes": [
                kf(kid(), 0.0, "rotation", [0, i*60.0, 0]),
                kf(kid(), 1.5, "rotation", [1, i*60.0, 1]),
                kf(kid(), 3.0, "rotation", [-1,i*60.0,-1]),
                kf(kid(), 4.5, "rotation", [1, i*60.0, 1]),
                kf(kid(), 6.0, "rotation", [0, i*60.0, 0]),
                kf(kid(), 0.0, "position", [0,0,0]),
                kf(kid(), 2.0, "position", [0, 0.04, 0]),
                kf(kid(), 4.0, "position", [0,-0.02, 0]),
                kf(kid(), 6.0, "position", [0,0,0]),
                kf(kid(), 0.0, "scale",    [1,1,1]),
                kf(kid(), 3.0, "scale",    [1.02,1.02,1.02]),
                kf(kid(), 6.0, "scale",    [1,1,1]),
            ]
        }
    return animators

def make_animators_dissipate():
    """dissipate: once, 0.7s — shockwave expands then snaps, spires crumble, rim sinks, glow dims."""
    animators = {}
    # shockwave: rapidly expands then snaps to 0
    for i, bid_val in enumerate(shock_bone_ids):
        bname = f"shockwave_{i+1}"
        animators[bid_val] = {
            "name": bname, "type": "bone", "keyframes": [
                kf(kid(), 0.0, "scale",    [1, 1, 1]),
                kf(kid(), 0.3, "scale",    [2.0,1.5,2.0]),
                kf(kid(), 0.5, "scale",    [0, 0, 0]),
                kf(kid(), 0.7, "scale",    [0, 0, 0]),
                kf(kid(), 0.0, "position", [0,0,0]),
                kf(kid(), 0.3, "position", [0, 0.5, 0]),
                kf(kid(), 0.5, "position", [0, 2.0, 0]),
                kf(kid(), 0.7, "position", [0, 2.0, 0]),
                kf(kid(), 0.0, "rotation", [0, i*5.0, 0]),
                kf(kid(), 0.7, "rotation", [0, i*5.0+30, 0]),
            ]
        }
    # rim clusters: sink back below ground
    for i, bid_val in enumerate(rim_bone_ids):
        bname = f"crater_rim_{i+1}"
        t_start = i * 0.03
        t_end   = min(0.2 + i * 0.03, 0.65)
        animators[bid_val] = {
            "name": bname, "type": "bone", "keyframes": [
                kf(kid(), 0.0,     "position", [0, 0, 0]),
                kf(kid(), t_start, "position", [0, 0.5, 0]),
                kf(kid(), t_end,   "position", [0,-4, 0]),
                kf(kid(), 0.7,     "position", [0,-4, 0]),
                kf(kid(), 0.0,     "scale",    [1,1,1]),
                kf(kid(), t_end,   "scale",    [0.3,0.3,0.3]),
                kf(kid(), 0.7,     "scale",    [0,0,0]),
                kf(kid(), 0.0,     "rotation", [0, i*36.0,  0]),
                kf(kid(), 0.7,     "rotation", [0, i*36.0+20, 0]),
            ]
        }
    # crater_disc: contracts
    animators[disc_bone_id] = {
        "name": "crater_disc", "type": "bone", "keyframes": [
            kf(kid(), 0.0, "scale",    [1,1,1]),
            kf(kid(), 0.4, "scale",    [0.5,0.5,0.5]),
            kf(kid(), 0.7, "scale",    [0,0,0]),
            kf(kid(), 0.0, "position", [0,0,0]),
            kf(kid(), 0.5, "position", [0,-1,0]),
            kf(kid(), 0.7, "position", [0,-2,0]),
            kf(kid(), 0.0, "rotation", [0,0,0]),
            kf(kid(), 0.7, "rotation", [0,30,0]),
        ]
    }
    # spires: crumble outward
    for i, (base_bid_v, mid_bid_v, tip_bid_v) in enumerate(spire_bone_ids):
        for j, (bval, bname) in enumerate([(base_bid_v, f"spire_{i+1}_base"),
                                            (mid_bid_v,  f"spire_{i+1}_mid"),
                                            (tip_bid_v,  f"spire_{i+1}_tip")]):
            t_start = j * 0.05 + i * 0.02
            t_end   = min(t_start + 0.3, 0.65)
            animators[bval] = {
                "name": bname, "type": "bone", "keyframes": [
                    kf(kid(), 0.0,     "position", [0, 0, 0]),
                    kf(kid(), t_start, "position", [0, 0.3, 0]),
                    kf(kid(), t_end,   "position", [(i%2*2-1)*2, -3, 0]),
                    kf(kid(), 0.7,     "position", [(i%2*2-1)*2, -3, 0]),
                    kf(kid(), 0.0,     "scale",    [1,1,1]),
                    kf(kid(), t_end,   "scale",    [0.5,0.5,0.5]),
                    kf(kid(), 0.7,     "scale",    [0,0,0]),
                    kf(kid(), 0.0,     "rotation", [0, i*60.0,   0]),
                    kf(kid(), t_end,   "rotation", [30+i*10, i*60.0+45, 20]),
                    kf(kid(), 0.7,     "rotation", [30+i*10, i*60.0+45, 20]),
                ]
            }
    # crater_glow: dims then contracts
    animators[glow_bone_id] = {
        "name": "crater_glow", "type": "bone", "keyframes": [
            kf(kid(), 0.0, "scale",    [1,1,1]),
            kf(kid(), 0.3, "scale",    [1.2,1.2,1.2]),
            kf(kid(), 0.6, "scale",    [0,0,0]),
            kf(kid(), 0.7, "scale",    [0,0,0]),
            kf(kid(), 0.0, "position", [0,0,0]),
            kf(kid(), 0.5, "position", [0,-0.5,0]),
            kf(kid(), 0.7, "position", [0,-1,0]),
            kf(kid(), 0.0, "rotation", [0,0,0]),
            kf(kid(), 0.7, "rotation", [0,45,0]),
        ]
    }
    # fragments: scatter then disappear
    for i, bid_val in enumerate(frag_bone_ids):
        bname = f"fragment_{i+1}"
        t_start = i * 0.04
        t_end   = min(t_start + 0.3, 0.65)
        animators[bid_val] = {
            "name": bname, "type": "bone", "keyframes": [
                kf(kid(), 0.0,     "position", [0, 0, 0]),
                kf(kid(), t_start, "position", [0, 0.5, 0]),
                kf(kid(), t_end,   "position", [(i%3-1)*3, -2, (i%2-0.5)*2]),
                kf(kid(), 0.7,     "position", [(i%3-1)*3, -2, (i%2-0.5)*2]),
                kf(kid(), 0.0,     "scale",    [1,1,1]),
                kf(kid(), t_end,   "scale",    [0.3,0.3,0.3]),
                kf(kid(), 0.7,     "scale",    [0,0,0]),
                kf(kid(), 0.0,     "rotation", [i*30, i*45, i*15]),
                kf(kid(), t_end,   "rotation", [i*30+90, i*45+60, i*15+45]),
                kf(kid(), 0.7,     "rotation", [i*30+90, i*45+60, i*15+45]),
            ]
        }
    # accents
    for i, bid_val in enumerate(accent_bone_ids):
        bname = f"accent_{i+1}"
        animators[bid_val] = {
            "name": bname, "type": "bone", "keyframes": [
                kf(kid(), 0.0, "position", [0, 0, 0]),
                kf(kid(), 0.3, "position", [0, 0.3, 0]),
                kf(kid(), 0.6, "position", [0,-2, 0]),
                kf(kid(), 0.7, "position", [0,-2, 0]),
                kf(kid(), 0.0, "scale",    [1,1,1]),
                kf(kid(), 0.6, "scale",    [0.2,0.2,0.2]),
                kf(kid(), 0.7, "scale",    [0,0,0]),
                kf(kid(), 0.0, "rotation", [0, i*60.0, 0]),
                kf(kid(), 0.7, "rotation", [0, i*60.0+30, 0]),
            ]
        }
    return animators

spawn_anim = {
    "uuid": "a001", "name": "animation.fallen_angel_descent.spawn",
    "loop": "once", "length": 1.1, "snapping": 24,
    "selected": False, "saved": False,
    "animators": make_animators_spawn()
}
idle_anim = {
    "uuid": "a002", "name": "animation.fallen_angel_descent.idle",
    "loop": "loop", "length": 6.0, "snapping": 24,
    "selected": False, "saved": False,
    "animators": make_animators_idle()
}
dissipate_anim = {
    "uuid": "a003", "name": "animation.fallen_angel_descent.dissipate",
    "loop": "once", "length": 0.7, "snapping": 24,
    "selected": False, "saved": False,
    "animators": make_animators_dissipate()
}

# ── ASSEMBLE FILE ────────────────────────────────────────────────────────────
bbmodel = {
    "meta": {"format_version": "4.10", "model_format": "free", "box_uv": False},
    "name": "fallen_angel_descent",
    "geometry": "geometry.fallen_angel_descent",
    "resolution": {"width": 64, "height": 64},
    "elements": elements,
    "outliner": bones_list,
    "textures": [
        {
            "id": "0", "name": "fallen_angel_descent_tex0",
            "relative_path": "../textures/fallen_angel_descent_tex.png",
            "folder": "devilsdream", "namespace": "", "visible": True,
            "mode": "bitmap", "saved": False, "uuid": "t001",
            "source": f"data:image/png;base64,{TEX0_B64}"
        },
        {
            "id": "1", "name": "fallen_angel_descent_tex1",
            "relative_path": "../textures/fallen_angel_descent_tex1.png",
            "folder": "devilsdream", "namespace": "", "visible": True,
            "mode": "bitmap", "saved": False, "uuid": "t002",
            "source": f"data:image/png;base64,{TEX1_B64}"
        }
    ],
    "animations": [spawn_anim, idle_anim, dissipate_anim]
}

out = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/fallen_angel_descent.bbmodel"
with open(out, "w") as f:
    json.dump(bbmodel, f, separators=(",", ":"))

size = os.path.getsize(out)
print(f"Written: {out}")
print(f"File size: {size:,} bytes ({size/1024:.1f} KB)")
print(f"Elements: {len(elements)}, Bones in outliner: {len(bones_list)}, All bones flat: {len(all_bones_flat)}")
print(f"Keyframe counter: {k_idx-1}")
if size < 300000:
    print("WARNING: FILE UNDER 300KB — NEEDS MORE KEYFRAMES")
else:
    print("PASS: File exceeds 300KB minimum")
