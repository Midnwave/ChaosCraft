"""
Build fallen_feather_lance.bbmodel
85 elements, 85 bones, 3 animations, every bone animated.
"""
import json, os, math

TEX0_PATH = "D:/CC/ChaosCraft/tools/gen_textures/m09_b64.txt"
TEX1_PATH = "D:/CC/ChaosCraft/tools/gen_textures/m09_tex1_b64.txt"
OUT_PATH  = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/fallen_feather_lance.bbmodel"

b64_0 = open(TEX0_PATH).read().strip()
b64_1 = open(TEX1_PATH).read().strip()

# ── UUID counters ──────────────────────────────────────────────────────────────
ec = [0]  # element counter
bc = [0]  # bone counter
kc = [0]  # keyframe counter
ac = [1]  # animation uuid counter (a001-a003)

def eid():
    ec[0]+=1
    return f"e{ec[0]:03d}"

def bid():
    bc[0]+=1
    return f"b{bc[0]:03d}"

def kid():
    kc[0]+=1
    return f"k{kc[0]:04d}"

# ── UV helpers ─────────────────────────────────────────────────────────────────
def faces(tex=0):
    if tex==0:
        return {
            "north": {"uv":[2,2,30,30],"texture":0},
            "east":  {"uv":[34,2,62,30],"texture":0},
            "south": {"uv":[2,2,30,30],"texture":0},
            "west":  {"uv":[34,2,62,30],"texture":0},
            "up":    {"uv":[2,34,30,62],"texture":0},
            "down":  {"uv":[34,34,62,62],"texture":0}
        }
    else:
        return {
            "north": {"uv":[34,34,62,62],"texture":1},
            "east":  {"uv":[34,34,62,62],"texture":1},
            "south": {"uv":[34,34,62,62],"texture":1},
            "west":  {"uv":[34,34,62,62],"texture":1},
            "up":    {"uv":[34,34,62,62],"texture":1},
            "down":  {"uv":[34,34,62,62],"texture":1}
        }

def elem(name, frm, to, tex=0):
    return {
        "name": name, "box_uv": False, "rescale": False, "locked": False,
        "render_order": "default", "allow_mirror_modeling": True,
        "from": frm, "to": to, "autouv": 0, "color": 0,
        "uuid": eid(), "faces": faces(tex)
    }

def bone(name, origin, children, rot=None):
    b = {
        "name": name, "origin": origin,
        "rotation": rot if rot else [0,0,0],
        "uuid": bid(), "export": True, "isOpen": True,
        "children": children
    }
    return b

# ── Keyframe helpers ───────────────────────────────────────────────────────────
def kf(time, x, y, z, ch="rotation"):
    return {
        "uuid": kid(), "time": time, "color": -1,
        "interpolation": "catmullrom",
        "data_points": [{"x": str(x), "y": str(y), "z": str(z)}],
        "channel": ch
    }

def kf_pos(time, x, y, z):
    return kf(time, x, y, z, "position")

def kf_scale(time, x, y, z):
    return kf(time, x, y, z, "scale")

def animator(bone_uuid, bone_name, keyframes):
    return {bone_uuid: {"name": bone_name, "type": "bone", "keyframes": keyframes}}

# ── Build all elements and bones ───────────────────────────────────────────────
elements = []
outliner = []
# We'll store (bone_uuid, bone_name) for animation building
all_bones = []  # list of [bone_uuid, bone_name, category]

def make_bone_and_elem(bone_name, elem_name, origin, frm, to, tex=0, rot=None, extra_elems=None):
    """Create 1 bone + 1 element (optionally more elements). Returns bone dict."""
    e = elem(elem_name, frm, to, tex)
    elements.append(e)
    children = [e["uuid"]]
    if extra_elems:
        for ee in extra_elems:
            elements.append(ee)
            children.append(ee["uuid"])
    b = bone(bone_name, origin, children, rot)
    return b

# ══════════════════════════════════════════════════════════════════════════════
# 1. MAIN RACHIS (5 bones, 5 elements)
# ══════════════════════════════════════════════════════════════════════════════
rachis_bones = []
rachis_specs = [
    ("rach_1", [-1.5,0,0],   [-1.5,0,0],    [1.5,0.4,3.0]),
    ("rach_2", [-1.25,0,3],  [-1.25,0,3],   [1.25,0.35,6]),
    ("rach_3", [-1.0,0,6],   [-1.0,0,6],    [1.0,0.3,9]),
    ("rach_4", [-0.75,0,9],  [-0.75,0,9],   [0.75,0.25,12]),
    ("rach_5", [-0.4,0,12],  [-0.4,0,12],   [0.4,0.2,15]),
]
for bn, orig, frm, to in rachis_specs:
    b = make_bone_and_elem(bn, bn+"_el", orig, frm, to, tex=0)
    rachis_bones.append(b)
    all_bones.append([b["uuid"], bn, "rachis"])

# ══════════════════════════════════════════════════════════════════════════════
# 2. BARBS (20 barb bones, one per side/position on each rachis segment)
#    4 barb bones per rachis: left_inner, right_inner, left_outer, right_outer
# ══════════════════════════════════════════════════════════════════════════════
barb_specs = [
    # seg 1 (z=0..3) — barbs at z=0.3..1.0 and z=1.8..2.5
    ("barb_1_li", [0,0,0],   [-3.5,0,0.3],  [-1.5,0.2,1.0]),
    ("barb_1_ri", [0,0,0],   [1.5,0,0.3],   [3.5,0.2,1.0]),
    ("barb_1_lo", [0,0,0],   [-3.0,0,1.8],  [-1.5,0.2,2.5]),
    ("barb_1_ro", [0,0,0],   [1.5,0,1.8],   [3.0,0.2,2.5]),
    # seg 2 (z=3..6)
    ("barb_2_li", [0,0,3],   [-3.0,0,3.3],  [-1.25,0.18,4.0]),
    ("barb_2_ri", [0,0,3],   [1.25,0,3.3],  [3.0,0.18,4.0]),
    ("barb_2_lo", [0,0,3],   [-2.7,0,4.5],  [-1.25,0.18,5.3]),
    ("barb_2_ro", [0,0,3],   [1.25,0,4.5],  [2.7,0.18,5.3]),
    # seg 3 (z=6..9)
    ("barb_3_li", [0,0,6],   [-2.5,0,6.2],  [-1.0,0.15,7.0]),
    ("barb_3_ri", [0,0,6],   [1.0,0,6.2],   [2.5,0.15,7.0]),
    ("barb_3_lo", [0,0,6],   [-2.2,0,7.5],  [-1.0,0.15,8.2]),
    ("barb_3_ro", [0,0,6],   [1.0,0,7.5],   [2.2,0.15,8.2]),
    # seg 4 (z=9..12)
    ("barb_4_li", [0,0,9],   [-2.0,0,9.2],  [-0.75,0.12,10.0]),
    ("barb_4_ri", [0,0,9],   [0.75,0,9.2],  [2.0,0.12,10.0]),
    ("barb_4_lo", [0,0,9],   [-1.8,0,10.5], [-0.75,0.12,11.2]),
    ("barb_4_ro", [0,0,9],   [0.75,0,10.5], [1.8,0.12,11.2]),
    # seg 5 (z=12..15)
    ("barb_5_li", [0,0,12],  [-1.5,0,12.2], [-0.4,0.1,12.9]),
    ("barb_5_ri", [0,0,12],  [0.4,0,12.2],  [1.5,0.1,12.9]),
    ("barb_5_lo", [0,0,12],  [-1.3,0,13.3], [-0.4,0.1,14.0]),
    ("barb_5_ro", [0,0,12],  [0.4,0,13.3],  [1.3,0.1,14.0]),
]
barb_bones = []
for bn, orig, frm, to in barb_specs:
    b = make_bone_and_elem(bn, bn+"_el", orig, frm, to, tex=0)
    barb_bones.append(b)
    all_bones.append([b["uuid"], bn, "barb"])

# ══════════════════════════════════════════════════════════════════════════════
# 3. FEATHER TIP (1 bone, 1 element)
# ══════════════════════════════════════════════════════════════════════════════
tip_e = elem("tip_el", [-0.2,0,15], [0.2,0.15,16.5], tex=0)
elements.append(tip_e)
tip_b = bone("feather_tip", [-0.2,0,15], [tip_e["uuid"]])
all_bones.append([tip_b["uuid"], "feather_tip", "tip"])

# ══════════════════════════════════════════════════════════════════════════════
# 4. FORMATION FEATHERS (4 × 7 = 28 bones, 28 elements)
#    Each: 3 rachis + 4 barb bones
# ══════════════════════════════════════════════════════════════════════════════
form_configs = [
    ("form1", [-3.0,-0.2,-2], 20),
    ("form2", [-5.0,-0.3,-5], 35),
    ("form3", [3.0,-0.2,-2],  -20),
    ("form4", [5.0,-0.3,-5],  -35),
]
form_bones = []
for fname, (ox,oy,oz), angle in form_configs:
    ry = angle  # Y-rotation in degrees
    # rachis cubes (shrunk scale for formation feathers)
    for i,(dz1,dz2) in enumerate([(0,2),(2,4),(4,6)]):
        ename = f"{fname}_rach_{i+1}"
        w = 0.9 - i*0.15
        e = elem(ename, [ox-w,oy,oz+dz1], [ox+w,oy+0.25,oz+dz2], tex=0)
        elements.append(e)
        bname = f"{fname}_r{i+1}"
        b = bone(bname, [ox,oy,oz], [e["uuid"]], rot=[0,ry,0])
        form_bones.append(b)
        all_bones.append([b["uuid"], bname, "form_rachis"])
    # barb pairs (4 per formation feather)
    for i,(dz,side) in enumerate([(0.5,'l'),(0.5,'r'),(3.0,'l'),(3.0,'r')]):
        ename = f"{fname}_barb_{i+1}"
        if side=='l':
            frm2=[ox-2.0,oy,oz+dz]; to2=[ox-0.8,oy+0.15,oz+dz+0.7]
        else:
            frm2=[ox+0.8,oy,oz+dz]; to2=[ox+2.0,oy+0.15,oz+dz+0.7]
        e = elem(ename, frm2, to2, tex=0)
        elements.append(e)
        bname = f"{fname}_b{i+1}"
        b = bone(bname, [ox,oy,oz], [e["uuid"]], rot=[0,ry,0])
        form_bones.append(b)
        all_bones.append([b["uuid"], bname, "form_barb"])

# ══════════════════════════════════════════════════════════════════════════════
# 5. CORRUPTION DRIPS (6 bones, 6 elements, tex1)
# ══════════════════════════════════════════════════════════════════════════════
drip_specs = [
    ("drip_1", [0.3,-0.5,1.5]),
    ("drip_2", [-0.2,-0.6,3.5]),
    ("drip_3", [0.1,-0.5,5.5]),
    ("drip_4", [-0.3,-0.7,7.5]),
    ("drip_5", [0.2,-0.5,9.5]),
    ("drip_6", [-0.1,-0.6,11.5]),
]
drip_bones = []
for dn, (dx,dy,dz) in drip_specs:
    frm3=[dx-0.15,dy-0.4,dz-0.15]; to3=[dx+0.15,dy,dz+0.15]
    b = make_bone_and_elem(dn, dn+"_el", [dx,dy,dz], frm3, to3, tex=1)
    drip_bones.append(b)
    all_bones.append([b["uuid"], dn, "drip"])

# ══════════════════════════════════════════════════════════════════════════════
# 6. CORRUPTION AURA (5 bones, 5 elements, tex1)
# ══════════════════════════════════════════════════════════════════════════════
aura_specs = [
    ("caura_1", 0,  0, 1.5),
    ("caura_2", 0,  0, 4.5),
    ("caura_3", 0,  0, 7.5),
    ("caura_4", 0,  0,10.5),
    ("caura_5", 0,  0,13.5),
]
aura_bones = []
for an, ax, ay, az in aura_specs:
    frm4=[ax-1.8,ay-0.05,az-0.4]; to4=[ax+1.8,ay+0.05,az+0.4]
    b = make_bone_and_elem(an, an+"_el", [ax,ay,az], frm4, to4, tex=1)
    aura_bones.append(b)
    all_bones.append([b["uuid"], an, "aura"])

# ══════════════════════════════════════════════════════════════════════════════
# 7. WING_VANE_DETAIL (20 bones, 20 elements)
#    Small accent slab pairs on formation feathers — 5 per formation feather
# ══════════════════════════════════════════════════════════════════════════════
vane_bones = []
vane_idx = 1
for fname, (ox,oy,oz), angle in form_configs:
    ry = angle
    for vi in range(5):
        dz = vi*1.1
        vname = f"wvd_{vane_idx}"
        e = elem(vname+"_el", [ox-1.5,oy+0.05,oz+dz], [ox+1.5,oy+0.1,oz+dz+0.4], tex=0)
        elements.append(e)
        b = bone(vname, [ox,oy,oz], [e["uuid"]], rot=[0,ry,0])
        vane_bones.append(b)
        all_bones.append([b["uuid"], vname, "vane"])
        vane_idx += 1

# ══════════════════════════════════════════════════════════════════════════════
# Build outliner (flat list of bone dicts for top-level)
# ══════════════════════════════════════════════════════════════════════════════
all_bone_objects = rachis_bones + barb_bones + [tip_b] + form_bones + drip_bones + aura_bones + vane_bones
outliner = all_bone_objects

print(f"Elements: {len(elements)}, Bones: {len(all_bone_objects)}, all_bones entries: {len(all_bones)}")

# ══════════════════════════════════════════════════════════════════════════════
# ANIMATIONS
# Three animations: spawn (once,0.3s), idle (loop,2.0s), dissipate (once,0.3s)
# Every bone gets 6+ keyframes per animation (position + rotation channels)
# ══════════════════════════════════════════════════════════════════════════════

def make_animators(anim_idx):
    """Generate animators dict for all bones. anim_idx: 0=spawn,1=idle,2=dissipate"""
    animators = {}
    for i, (buuid, bname, cat) in enumerate(all_bones):
        phase = (i % 7) * 0.01  # slight stagger
        if anim_idx == 0:  # SPAWN (0.3s)
            kfs = [
                kf_pos(0.0, 0, -6*(1+i%3), 0),
                kf_pos(0.04+phase, 0, -3, 0),
                kf_pos(0.1+phase, 0, -1, 0),
                kf_pos(0.18, 0, 0, 0),
                kf_pos(0.24, 0, 0.3, 0),
                kf_pos(0.3, 0, 0, 0),
                kf(0.0, 0, (i%5)*8-20, 0),
                kf(0.08+phase, 0, (i%5)*4-10, 0),
                kf(0.16, 0, (i%3)*2-3, 0),
                kf(0.24, 0, 0, 0),
                kf(0.3, 0, 0, 0),
            ]
        elif anim_idx == 1:  # IDLE (2.0s)
            # feather rolls on Z axis, barbs flex
            roll_base = (i%5) * (2*math.pi/5)
            kfs = []
            for t_step in range(8):
                t = t_step * (2.0/7)
                ang_z = math.sin(t * math.pi + roll_base) * 15
                ang_x = math.cos(t * math.pi * 0.7 + roll_base) * (2 if cat=="barb" else 0)
                kfs.append(kf(round(t,3), ang_x, 0, ang_z))
            # position oscillation for drips/aura
            py_osc = math.sin(roll_base) * 0.1 if cat in ("drip","aura") else 0
            for t_step in range(6):
                t = t_step * (2.0/5)
                py = math.sin(t*math.pi*2+roll_base)*0.15 if cat in ("drip","aura") else 0
                kfs.append(kf_pos(round(t,3), 0, py, 0))
        else:  # DISSIPATE (0.3s)
            # shatter outward
            dir_x = math.cos(i*0.7)*3 * (1 if cat not in ("rachis",) else 0.5)
            dir_y = math.sin(i*1.1)*2 - 1
            dir_z = math.sin(i*0.5)*2
            kfs = [
                kf_pos(0.0, 0, 0, 0),
                kf_pos(0.08, dir_x*0.3, dir_y*0.2, dir_z*0.2),
                kf_pos(0.15, dir_x*0.6, dir_y*0.5, dir_z*0.5),
                kf_pos(0.22, dir_x, dir_y, dir_z),
                kf_pos(0.28, dir_x*1.3, dir_y*1.2, dir_z*1.1),
                kf_pos(0.3, dir_x*1.5, dir_y*1.4, dir_z*1.3),
                kf(0.0, 0, 0, 0),
                kf(0.06, i%2*15-7, i%3*12-12, i%4*10-15),
                kf(0.12, i%2*30-15, i%3*25-20, i%4*20-30),
                kf(0.2, i%2*60-30, i%3*50-40, i%4*45-60),
                kf(0.26, i%2*90-45, i%3*80-60, i%4*70-90),
                kf(0.3, i%2*120-60, i%3*100-80, i%4*90-120),
                kf_scale(0.0, 1, 1, 1),
                kf_scale(0.15, 0.9, 0.9, 0.9),
                kf_scale(0.25, 0.5, 0.5, 0.5),
                kf_scale(0.3, 0.0, 0.0, 0.0),
            ]
        animators[buuid] = {"name": bname, "type": "bone", "keyframes": kfs}
    return animators

anim_spawn = {
    "uuid": "a001",
    "name": "animation.fallen_feather_lance.spawn",
    "loop": "once",
    "length": 0.3,
    "snapping": 24,
    "selected": False,
    "saved": False,
    "animators": make_animators(0)
}

anim_idle = {
    "uuid": "a002",
    "name": "animation.fallen_feather_lance.idle",
    "loop": "loop",
    "length": 2.0,
    "snapping": 24,
    "selected": False,
    "saved": False,
    "animators": make_animators(1)
}

anim_dissipate = {
    "uuid": "a003",
    "name": "animation.fallen_feather_lance.dissipate",
    "loop": "once",
    "length": 0.3,
    "snapping": 24,
    "selected": False,
    "saved": False,
    "animators": make_animators(2)
}

# ══════════════════════════════════════════════════════════════════════════════
# TEXTURES
# ══════════════════════════════════════════════════════════════════════════════
textures = [
    {
        "id": "0", "name": "fallen_feather_lance_tex",
        "relative_path": "../textures/fallen_feather_lance_tex.png",
        "folder": "devilsdream", "namespace": "",
        "visible": True, "mode": "bitmap", "saved": False,
        "uuid": "t001",
        "source": f"data:image/png;base64,{b64_0}"
    },
    {
        "id": "1", "name": "fallen_feather_lance_tex1",
        "relative_path": "../textures/fallen_feather_lance_tex1.png",
        "folder": "devilsdream", "namespace": "",
        "visible": True, "mode": "bitmap", "saved": False,
        "uuid": "t002",
        "source": f"data:image/png;base64,{b64_1}"
    }
]

# ══════════════════════════════════════════════════════════════════════════════
# ASSEMBLE MODEL
# ══════════════════════════════════════════════════════════════════════════════
model = {
    "meta": {"format_version": "4.10", "model_format": "free", "box_uv": False},
    "name": "fallen_feather_lance",
    "geometry": "geometry.fallen_feather_lance",
    "resolution": {"width": 64, "height": 64},
    "elements": elements,
    "outliner": outliner,
    "textures": textures,
    "animations": [anim_spawn, anim_idle, anim_dissipate]
}

out_str = json.dumps(model, separators=(',', ':'))
with open(OUT_PATH, 'w') as f:
    f.write(out_str)

size = os.path.getsize(OUT_PATH)
print(f"Written: {OUT_PATH}")
print(f"Size: {size:,} bytes ({size/1024:.1f} KB)")
print(f"Elements: {len(elements)}, Bones: {len(all_bone_objects)}")
print(f"Total keyframes: {kc[0]}")
