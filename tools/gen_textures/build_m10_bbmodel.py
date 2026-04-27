"""
Build nightmare_shard_volley.bbmodel
84 elements, 75 bones, 3 animations, every bone animated.
"""
import json, os, math

TEX0_PATH = "D:/CC/ChaosCraft/tools/gen_textures/m10_b64.txt"
TEX1_PATH = "D:/CC/ChaosCraft/tools/gen_textures/m10_tex1_b64.txt"
OUT_PATH  = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_shard_volley.bbmodel"

b64_0 = open(TEX0_PATH).read().strip()
b64_1 = open(TEX1_PATH).read().strip()

# ── UUID counters ──────────────────────────────────────────────────────────────
ec = [0]; bc = [0]; kc = [0]

def eid():
    ec[0]+=1; return f"e{ec[0]:03d}"

def bid():
    bc[0]+=1; return f"b{bc[0]:03d}"

def kid():
    kc[0]+=1; return f"k{kc[0]:04d}"

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
    return {
        "name": name, "origin": origin,
        "rotation": rot if rot else [0,0,0],
        "uuid": bid(), "export": True, "isOpen": True,
        "children": children
    }

# ── Keyframe helpers ───────────────────────────────────────────────────────────
def kf(time, x, y, z, ch="rotation"):
    return {"uuid": kid(), "time": time, "color": -1,
            "interpolation": "catmullrom",
            "data_points": [{"x": str(x), "y": str(y), "z": str(z)}],
            "channel": ch}

def kf_pos(t,x,y,z): return kf(t,x,y,z,"position")
def kf_scale(t,x,y,z): return kf(t,x,y,z,"scale")

# ══════════════════════════════════════════════════════════════════════════════
elements = []
outliner = []
all_bones = []  # [uuid, name, category]

def make_bone_elem(bname, ename, origin, frm, to, tex=0, rot=None, extras=None):
    e = elem(ename, frm, to, tex)
    elements.append(e)
    children = [e["uuid"]]
    if extras:
        for ee in extras:
            elements.append(ee)
            children.append(ee["uuid"])
    b = bone(bname, origin, children, rot)
    return b

# ══════════════════════════════════════════════════════════════════════════════
# 1. CORE 9 SHARDS (9 bones, 18 elements — 2 elements per shard)
# ══════════════════════════════════════════════════════════════════════════════
# Centre shard
e_base = elem("sh1_base", [-0.5,-0.3,0], [0.5,0.3,2.5], tex=0)
e_tip  = elem("sh1_tip",  [-0.25,-0.2,2.5], [0.25,0.2,4.5], tex=0)
elements.extend([e_base, e_tip])
b = bone("shard_1", [0,0,0], [e_base["uuid"], e_tip["uuid"]])
all_bones.append([b["uuid"], "shard_1", "shard"])
outliner.append(b)

# 8 outer shards at offsets
outer_offsets = [
    ( 0.9, 0.4,-0.2,  8, 5,12),
    (-0.9, 0.4,-0.2, -8, 5,10),
    ( 0.7,-0.4, 0.3,  5,-8,15),
    (-0.7,-0.4, 0.3, -5,-8,12),
    ( 1.1, 0.0,-0.3, 10, 3, 8),
    (-1.1, 0.0,-0.3,-10, 3, 8),
    ( 0.5, 0.6, 0.2,  6,10,20),
    (-0.5, 0.6, 0.2, -6,10,20),
]
for idx,(ox,oy,oz_off,rx,ry,rz) in enumerate(outer_offsets):
    i = idx+2
    eb = elem(f"sh{i}_base", [ox-0.45,oy-0.25,oz_off], [ox+0.45,oy+0.25,oz_off+2.2], tex=0)
    et = elem(f"sh{i}_tip",  [ox-0.22,oy-0.18,oz_off+2.2], [ox+0.22,oy+0.18,oz_off+4.0], tex=0)
    elements.extend([eb, et])
    b = bone(f"shard_{i}", [ox,oy,oz_off], [eb["uuid"], et["uuid"]], rot=[rx,ry,rz])
    all_bones.append([b["uuid"], f"shard_{i}", "shard"])
    outliner.append(b)

# ══════════════════════════════════════════════════════════════════════════════
# 2. MICRO-SHARDS (4 bones, 4 elements)
# ══════════════════════════════════════════════════════════════════════════════
micro_specs = [
    ("mshard_1", [0.0, 0.5, 1.5], [-0.15,-0.1,0.8], [0.15,0.1,2.3]),
    ("mshard_2", [-0.8,-0.3, 2.0], [-0.25,-0.1,1.2], [0.05,0.1,2.7]),
    ("mshard_3", [0.8,-0.5, 1.8], [-0.1,-0.1,1.0], [0.3,0.1,2.5]),
    ("mshard_4", [0.0,-0.6, 0.5], [-0.15,-0.1,0.1], [0.15,0.1,1.6]),
]
for mn,(ox,oy,oz),frm,to in micro_specs:
    b = make_bone_elem(mn, mn+"_el", [ox,oy,oz], frm, to, tex=0, rot=[15,30,10])
    all_bones.append([b["uuid"], mn, "micro"])
    outliner.append(b)

# ══════════════════════════════════════════════════════════════════════════════
# 3. LEADING GLOW (3 bones, 3 elements, tex1)
# ══════════════════════════════════════════════════════════════════════════════
for i,(z_pos,scale) in enumerate([(4.5,2.5),(4.7,2.0),(4.9,1.5)]):
    s=scale/2
    b = make_bone_elem(f"lead_{i+1}", f"lead_{i+1}_el", [0,0,z_pos],
                       [-s,-s,z_pos], [s,s,z_pos+0.1], tex=1)
    all_bones.append([b["uuid"], f"lead_{i+1}", "glow"])
    outliner.append(b)

# ══════════════════════════════════════════════════════════════════════════════
# 4. WAKE TRAIL (6 bones, 6 elements, tex1)
# ══════════════════════════════════════════════════════════════════════════════
for i in range(6):
    z = -(i+1)*0.9
    scale = 1.0 - i*0.13
    b = make_bone_elem(f"wake_{i+1}", f"wake_{i+1}_el", [0,0,z],
                       [-scale,-scale*0.5,z-0.05], [scale,scale*0.5,z+0.55], tex=1)
    all_bones.append([b["uuid"], f"wake_{i+1}", "wake"])
    outliner.append(b)

# ══════════════════════════════════════════════════════════════════════════════
# 5. SHARD FRAGMENTS (12 bones, 12 elements)
# ══════════════════════════════════════════════════════════════════════════════
import math as _math
for i in range(12):
    angle = i * (2*_math.pi/12)
    r = 1.4 + (i%3)*0.3
    sx = r*_math.cos(angle); sy = (i%4-1.5)*0.4; sz = r*_math.sin(angle)*0.5+1.0
    b = make_bone_elem(f"sfrag_{i+1}", f"sfrag_{i+1}_el", [sx,sy,sz],
                       [sx-0.1,sy-0.15,sz-0.25], [sx+0.1,sy+0.15,sz+0.25], tex=0,
                       rot=[i*15, i*20, i*12])
    all_bones.append([b["uuid"], f"sfrag_{i+1}", "frag"])
    outliner.append(b)

# ══════════════════════════════════════════════════════════════════════════════
# 6. AURA RINGS (8 bones, 8 elements, mix tex)
# ══════════════════════════════════════════════════════════════════════════════
for i in range(8):
    r = 1.5 + i*0.2
    z = i*0.3
    tex_i = 1 if i%2==0 else 0
    b = make_bone_elem(f"aura_{i+1}", f"aura_{i+1}_el", [0,0,z],
                       [-r,-0.05,z-r], [r,0.05,z+r], tex=tex_i, rot=[0,i*22,0])
    all_bones.append([b["uuid"], f"aura_{i+1}", "aura"])
    outliner.append(b)

# ══════════════════════════════════════════════════════════════════════════════
# 7. CRYSTAL SPIKES (6 bones, 6 elements)
# ══════════════════════════════════════════════════════════════════════════════
spike_configs = [
    ( 1.2, 0.0, 2.0, [0,0,0]),
    (-1.2, 0.0, 2.0, [0,180,0]),
    ( 0.0, 1.0, 2.0, [0,0,90]),
    ( 0.0,-1.0, 2.0, [0,0,-90]),
    ( 0.85, 0.85, 2.0, [0,45,45]),
    (-0.85, 0.85, 2.0, [0,-45,45]),
]
for i,(sx,sy,sz,rot) in enumerate(spike_configs):
    b = make_bone_elem(f"cspike_{i+1}", f"cspike_{i+1}_el", [sx,sy,sz],
                       [sx-0.075,sy-0.075,sz], [sx+0.075,sy+0.075,sz+1.2], tex=0, rot=rot)
    all_bones.append([b["uuid"], f"cspike_{i+1}", "spike"])
    outliner.append(b)

# ══════════════════════════════════════════════════════════════════════════════
# 8. SHOCKWAVE DISCS (4 bones, 4 elements, tex1)
# ══════════════════════════════════════════════════════════════════════════════
for i,z in enumerate([3.0,4.0,5.0,6.0]):
    r = 1.2 + i*0.2
    b = make_bone_elem(f"sdisc_{i+1}", f"sdisc_{i+1}_el", [0,0,z],
                       [-r,-0.05,z], [r,0.05,z+0.1], tex=1)
    all_bones.append([b["uuid"], f"sdisc_{i+1}", "disc"])
    outliner.append(b)

# ══════════════════════════════════════════════════════════════════════════════
# 9. EXTENDED WAKE (8 bones, 8 elements, tex1)
# ══════════════════════════════════════════════════════════════════════════════
for i in range(8):
    z = -(i+7)*0.8
    s = 0.5 - i*0.04
    b = make_bone_elem(f"wake_ext_{i+1}", f"wext_{i+1}_el", [0,0,z],
                       [-s,-s*0.4,z-0.05], [s,s*0.4,z+0.45], tex=1)
    all_bones.append([b["uuid"], f"wake_ext_{i+1}", "wake"])
    outliner.append(b)

# ══════════════════════════════════════════════════════════════════════════════
# 10. CRYSTAL DETAIL (15 bones, 15 elements)
# ══════════════════════════════════════════════════════════════════════════════
for i in range(15):
    angle = i * (2*_math.pi/15)
    r = 0.6 + (i%4)*0.35
    dx = r*_math.cos(angle); dy = r*_math.sin(angle)*0.6; dz = 1.0+(i%5)*0.4
    t = 0 if i%3!=0 else 1
    b = make_bone_elem(f"cdet_{i+1}", f"cdet_{i+1}_el", [dx,dy,dz],
                       [dx-0.12,dy-0.08,dz-0.2], [dx+0.12,dy+0.08,dz+0.4], tex=t,
                       rot=[i*10, i*14, i*9])
    all_bones.append([b["uuid"], f"cdet_{i+1}", "detail"])
    outliner.append(b)

print(f"Elements: {len(elements)}, Bones: {len(outliner)}, all_bones: {len(all_bones)}")

# ══════════════════════════════════════════════════════════════════════════════
# ANIMATIONS
# ══════════════════════════════════════════════════════════════════════════════

def make_animators(anim_idx):
    animators = {}
    for i, (buuid, bname, cat) in enumerate(all_bones):
        phase = (i % 9) * 0.012
        if anim_idx == 0:  # SPAWN (0.3s)
            # All shards materialize from tip backward
            start_z = -3 if cat in ("shard","micro") else (3 if cat=="glow" else 0)
            kfs = [
                kf_pos(0.0, 0, 0, start_z),
                kf_pos(0.06+phase, 0, 0, start_z*0.6),
                kf_pos(0.12+phase, 0, 0, start_z*0.3),
                kf_pos(0.18, 0, 0, 0),
                kf_pos(0.24, 0, 0.2, 0),
                kf_pos(0.3, 0, 0, 0),
                kf_scale(0.0, 0.0, 0.0, 0.0),
                kf_scale(0.05+phase, 0.3, 0.3, 0.3),
                kf_scale(0.12, 0.7, 0.7, 0.7),
                kf_scale(0.2, 1.1, 1.1, 1.1),
                kf_scale(0.28, 0.95, 0.95, 0.95),
                kf_scale(0.3, 1.0, 1.0, 1.0),
                kf(0.0, (i%5)*10-20, (i%4)*15-22, (i%6)*12-30),
                kf(0.1+phase, (i%5)*5-10, (i%4)*7-10, (i%6)*5-12),
                kf(0.2, 0, 0, 0),
                kf(0.3, 0, 0, 0),
            ]
        elif anim_idx == 1:  # IDLE (1.5s)
            # Cluster rotates as unit, each shard tilts independently
            base_phase = i * (2*_math.pi/len(all_bones))
            kfs = []
            for t_step in range(7):
                t = t_step*(1.5/6)
                # Z spin for whole cluster
                ang_z = t_step*(360/6) if cat=="shard" and i==0 else 0
                # individual tilt for shards
                ang_x = _math.sin(t*_math.pi*2+base_phase)*3 if cat in ("shard","micro") else 0
                ang_y = _math.cos(t*_math.pi*1.5+base_phase)*3 if cat in ("shard","micro") else 0
                kfs.append(kf(round(t,3), ang_x, ang_y, ang_z))
            # glow pulse (scale)
            for t_step in range(7):
                t = t_step*(1.5/6)
                if cat=="glow":
                    s = 1.0 + _math.sin(t*_math.pi*4+base_phase)*0.2
                    kfs.append(kf_scale(round(t,3), s, s, s))
                else:
                    kfs.append(kf_scale(round(t,3), 1.0, 1.0, 1.0))
            # wake oscillation
            for t_step in range(6):
                t = t_step*(1.5/5)
                py = _math.sin(t*_math.pi*3+base_phase)*0.1 if cat=="wake" else 0
                kfs.append(kf_pos(round(t,3), 0, py, 0))
        else:  # DISSIPATE (0.25s)
            # shards scatter from formation
            dir_x = _math.cos(i*0.8)*2.5
            dir_y = _math.sin(i*1.2)*1.5
            dir_z = _math.sin(i*0.6)*2 + (1.5 if cat in ("shard","micro") else -0.5)
            kfs = [
                kf_pos(0.0, 0, 0, 0),
                kf_pos(0.05, dir_x*0.2, dir_y*0.15, dir_z*0.2),
                kf_pos(0.1, dir_x*0.5, dir_y*0.4, dir_z*0.5),
                kf_pos(0.17, dir_x*0.85, dir_y*0.7, dir_z*0.85),
                kf_pos(0.22, dir_x*1.2, dir_y*1.0, dir_z*1.2),
                kf_pos(0.25, dir_x*1.5, dir_y*1.3, dir_z*1.5),
                kf_scale(0.0, 1.0, 1.0, 1.0),
                kf_scale(0.08, 1.1, 1.1, 1.1),
                kf_scale(0.15, 0.8, 0.8, 0.8),
                kf_scale(0.2, 0.4, 0.4, 0.4),
                kf_scale(0.25, 0.0, 0.0, 0.0),
                kf(0.0, 0, 0, 0),
                kf(0.05, i%3*15-15, i%4*12-18, i%5*10-20),
                kf(0.1, i%3*35-35, i%4*30-45, i%5*25-50),
                kf(0.17, i%3*65-65, i%4*55-80, i%5*50-100),
                kf(0.22, i%3*90-90, i%4*80-110, i%5*75-150),
                kf(0.25, i%3*120-120, i%4*110-150, i%5*100-200),
            ]
        animators[buuid] = {"name": bname, "type": "bone", "keyframes": kfs}
    return animators

anim_spawn = {
    "uuid": "a001",
    "name": "animation.nightmare_shard_volley.spawn",
    "loop": "once", "length": 0.3, "snapping": 24,
    "selected": False, "saved": False,
    "animators": make_animators(0)
}
anim_idle = {
    "uuid": "a002",
    "name": "animation.nightmare_shard_volley.idle",
    "loop": "loop", "length": 1.5, "snapping": 24,
    "selected": False, "saved": False,
    "animators": make_animators(1)
}
anim_dissipate = {
    "uuid": "a003",
    "name": "animation.nightmare_shard_volley.dissipate",
    "loop": "once", "length": 0.25, "snapping": 24,
    "selected": False, "saved": False,
    "animators": make_animators(2)
}

# ══════════════════════════════════════════════════════════════════════════════
# TEXTURES
# ══════════════════════════════════════════════════════════════════════════════
textures = [
    {
        "id": "0", "name": "nightmare_shard_volley_tex",
        "relative_path": "../textures/nightmare_shard_volley_tex.png",
        "folder": "devilsdream", "namespace": "",
        "visible": True, "mode": "bitmap", "saved": False,
        "uuid": "t001",
        "source": f"data:image/png;base64,{b64_0}"
    },
    {
        "id": "1", "name": "nightmare_shard_volley_tex1",
        "relative_path": "../textures/nightmare_shard_volley_tex1.png",
        "folder": "devilsdream", "namespace": "",
        "visible": True, "mode": "bitmap", "saved": False,
        "uuid": "t002",
        "source": f"data:image/png;base64,{b64_1}"
    }
]

# ══════════════════════════════════════════════════════════════════════════════
# ASSEMBLE
# ══════════════════════════════════════════════════════════════════════════════
model = {
    "meta": {"format_version": "4.10", "model_format": "free", "box_uv": False},
    "name": "nightmare_shard_volley",
    "geometry": "geometry.nightmare_shard_volley",
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
print(f"Elements: {len(elements)}, Bones: {len(outliner)}")
print(f"Total keyframes: {kc[0]}")

# Verify span
all_froms = [e["from"] for e in elements]
all_tos   = [e["to"]   for e in elements]
xs = [c[0] for c in all_froms+all_tos]
zs = [c[2] for c in all_froms+all_tos]
print(f"X span: {min(xs):.2f} to {max(xs):.2f}  (width {max(xs)-min(xs):.2f})")
print(f"Z span: {min(zs):.2f} to {max(zs):.2f}  (depth {max(zs)-min(zs):.2f})")
