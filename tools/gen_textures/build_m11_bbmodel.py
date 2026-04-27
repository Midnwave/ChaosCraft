"""
Build devils_tongue_beam.bbmodel
85 elements, 85 bones, 3 animations, 6+ keyframes per bone per animation
"""
import json, math, os

# ── Read textures ──────────────────────────────────────────────────────────────
b64_0 = open("D:/CC/ChaosCraft/tools/gen_textures/m11_b64.txt").read().strip()
b64_1 = open("D:/CC/ChaosCraft/tools/gen_textures/m11_tex1_b64.txt").read().strip()

# ── UUID helpers ───────────────────────────────────────────────────────────────
def eid(n): return f"e{n:03d}"
def bid(n): return f"b{n:03d}"
def aid(n): return f"a{n:03d}"
def kid(n): return f"k{n:04d}"
def tid(n): return f"t{n:03d}"

# ── UV sets ────────────────────────────────────────────────────────────────────
UV0 = {"north":{"uv":[2,2,30,30],"texture":0},"east":{"uv":[34,2,62,30],"texture":0},
       "south":{"uv":[2,2,30,30],"texture":0},"west":{"uv":[34,2,62,30],"texture":0},
       "up":{"uv":[2,34,30,62],"texture":0},"down":{"uv":[34,34,62,62],"texture":0}}
UV1 = {"north":{"uv":[34,34,62,62],"texture":1},"east":{"uv":[34,34,62,62],"texture":1},
       "south":{"uv":[34,34,62,62],"texture":1},"west":{"uv":[34,34,62,62],"texture":1},
       "up":{"uv":[34,34,62,62],"texture":1},"down":{"uv":[34,34,62,62],"texture":1}}

# ── Element builder ────────────────────────────────────────────────────────────
def elem(idx, name, fr, to, uv):
    return {"name":name,"box_uv":False,"rescale":False,"locked":False,
            "render_order":"default","allow_mirror_modeling":True,
            "from":fr,"to":to,"autouv":0,"color":0,"uuid":eid(idx),"faces":uv}

# ── Bone builder ───────────────────────────────────────────────────────────────
def bone(idx, name, origin, children, rot=None):
    o = rot if rot else [0,0,0]
    return {"name":name,"origin":origin,"rotation":o,"uuid":bid(idx),
            "export":True,"isOpen":True,"children":children}

# ── Keyframe builder ───────────────────────────────────────────────────────────
k_counter = [1]
def kf(time, x, y, z, channel="position", interp="catmullrom"):
    uid = kid(k_counter[0]); k_counter[0]+=1
    return {"uuid":uid,"time":time,"color":-1,"interpolation":interp,
            "data_points":[{"x":str(x),"y":str(y),"z":str(z)}],"channel":channel}

# ══════════════════════════════════════════════════════════════════════════════
# BUILD ELEMENTS & BONES
# ══════════════════════════════════════════════════════════════════════════════
elements = []
bones = []
ei = 1  # element index
bi = 1  # bone index

# ── 1. BEAM LAYERS (6 bones, 6 elements) ──────────────────────────────────────
beam_layer_defs = [
    ("beam_1", [-1.5,-0.6, 0],[1.5,-0.4,12]),
    ("beam_2", [-1.4,-0.4, 0],[1.4,-0.2,12]),
    ("beam_3", [-1.3,-0.2, 0],[1.3, 0.0,12]),
    ("beam_4", [-1.2, 0.0, 0],[1.2, 0.2,12]),
    ("beam_5", [-1.1, 0.2, 0],[1.1, 0.35,12]),
    ("beam_6", [-0.9, 0.35,0],[0.9, 0.5,12]),
]
beam_layer_bids = []
for name, fr, to in beam_layer_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [0,0,0], [eid(ei)]); bones.append(b)
    beam_layer_bids.append(bid(bi)); ei+=1; bi+=1

# ── 2. TONGUE TIP (4 bones, 4 elements) ───────────────────────────────────────
ttip_defs = [
    ("ttip_1", [-2.0,-0.1,12],[-0.5, 0.1,13.5]),
    ("ttip_2", [ 0.5,-0.1,12],[ 2.0, 0.1,13.5]),
    ("ttip_3", [-1.5,-0.3,12],[-0.2, 0.0,14.0]),
    ("ttip_4", [ 0.2,-0.3,12],[ 1.5, 0.0,14.0]),
]
ttip_bids = []
for name, fr, to in ttip_defs:
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    b = bone(bi, name, [0,0,12], [eid(ei)]); bones.append(b)
    ttip_bids.append(bid(bi)); ei+=1; bi+=1

# ── 3. FLICKER SLABS (8 bones, 8 elements) ────────────────────────────────────
flick_z = [1.0, 2.5, 4.0, 5.5, 7.0, 8.5, 10.0, 11.5]
flick_bids = []
for i, fz in enumerate(flick_z):
    name = f"flick_{i+1}"
    off_x = (i % 3 - 1) * 0.2   # slight X offset variation
    fr = [-1.0+off_x, 0.55, fz]; to = [1.0+off_x, 0.70, fz+0.8]
    rot_y = (i % 4) * 3.0        # slight Y-rotation variation per flicker
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [0,0,fz], [eid(ei)], rot=[0,rot_y,0]); bones.append(b)
    flick_bids.append(bid(bi)); ei+=1; bi+=1

# ── 4. EMITTER PETALS (6 bones, 6 elements) ───────────────────────────────────
emit_bids = []
for i in range(6):
    angle = i * 60.0
    name = f"emit_{i+1}"
    fr = [-0.15, -0.15, 0.0]; to = [0.15, 0.15, 1.5]
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    b = bone(bi, name, [0,0,0], [eid(ei)], rot=[0, angle, 0]); bones.append(b)
    emit_bids.append(bid(bi)); ei+=1; bi+=1

# ── 5. IMPACT BURST (8 bones, 8 elements) ─────────────────────────────────────
iburst_bids = []
for i in range(8):
    angle = i * 45.0
    name = f"iburst_{i+1}"
    # Fan outward from Z=12 at various angles
    r = 1.2 + (i%3)*0.3
    ax = r * math.cos(math.radians(angle))
    ay = r * math.sin(math.radians(angle))
    fr = [ax-0.3, ay-0.1, 12.0]; to = [ax+0.3, ay+0.1, 12.8]
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    b = bone(bi, name, [0,0,12], [eid(ei)], rot=[0,angle,0]); bones.append(b)
    iburst_bids.append(bid(bi)); ei+=1; bi+=1

# ── 6. BEAM EDGE ACCENTS (12 bones, 12 elements) ──────────────────────────────
bedge_bids = []
for i in range(12):
    name = f"bedge_{i+1}"
    z_pos = i * 1.0  # every 1 unit along beam
    side = 1 if i % 2 == 0 else -1
    y_edge = 0.55 if i < 6 else -0.65  # top/bottom edge alternating
    fr = [side*1.4, y_edge, z_pos]; to = [side*1.6, y_edge+0.12, z_pos+0.6]
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [0,0,z_pos], [eid(ei)]); bones.append(b)
    bedge_bids.append(bid(bi)); ei+=1; bi+=1

# ── 7. TONGUE SURFACE RIPPLES (8 bones, 8 elements) ───────────────────────────
tsr_bids = []
for i in range(8):
    name = f"tsr_{i+1}"
    z_pos = 1.2 + i * 1.3
    x_off = (i % 3 - 1) * 0.3
    fr = [-0.9+x_off, 0.45, z_pos]; to = [0.9+x_off, 0.58, z_pos+0.9]
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [0,0,z_pos], [eid(ei)], rot=[0,(i%5)*6.0,0]); bones.append(b)
    tsr_bids.append(bid(bi)); ei+=1; bi+=1

# ── 8. EMITTER RING (8 bones, 8 elements) ─────────────────────────────────────
er_bids = []
for i in range(8):
    angle = i * 45.0
    name = f"er_{i+1}"
    r = 1.8
    ax = r * math.cos(math.radians(angle))
    ay = r * math.sin(math.radians(angle))
    fr = [ax-0.25, ay-0.08, -0.2]; to = [ax+0.25, ay+0.08, 0.2]
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    b = bone(bi, name, [0,0,0], [eid(ei)], rot=[0,angle,0]); bones.append(b)
    er_bids.append(bid(bi)); ei+=1; bi+=1

# ── 9. BEAM CENTER GLOW (5 bones, 5 elements) ─────────────────────────────────
bcg_bids = []
for i in range(5):
    name = f"bcg_{i+1}"
    z_start = i * 2.4
    fr = [-0.4, -0.05, z_start]; to = [0.4, 0.05, z_start+2.4]
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    b = bone(bi, name, [0,0,z_start], [eid(ei)]); bones.append(b)
    bcg_bids.append(bid(bi)); ei+=1; bi+=1

# ── 10. TIP AURA (10 bones, 10 elements) ──────────────────────────────────────
tip_aura_bids = []
for i in range(10):
    angle = i * 36.0
    name = f"tip_aura_{i+1}"
    r = 0.8 + (i%3)*0.3
    ax = r * math.cos(math.radians(angle))
    ay = r * math.sin(math.radians(angle))
    fr = [ax-0.2, ay-0.08, 13.5]; to = [ax+0.2, ay+0.08, 14.5]
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    b = bone(bi, name, [0,0,14], [eid(ei)], rot=[0,angle,0]); bones.append(b)
    tip_aura_bids.append(bid(bi)); ei+=1; bi+=1

# ── 11. WAKE BEAM EXTENSION (10 bones, 10 elements) ───────────────────────────
wake_bids = []
for i in range(10):
    name = f"wake_beam_{i+1}"
    z_pos = 12.0 + i * 0.8
    fade = 1.0 - i * 0.08  # gradually narrowing
    fr = [-0.6*fade, -0.1, z_pos]; to = [0.6*fade, 0.1, z_pos+0.8]
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [0,0,z_pos], [eid(ei)]); bones.append(b)
    wake_bids.append(bid(bi)); ei+=1; bi+=1

print(f"Elements: {len(elements)}, Bones: {len(bones)}")
assert len(elements) == 85, f"Expected 85 elements, got {len(elements)}"
assert len(bones) == 85, f"Expected 85 bones, got {len(bones)}"

# ══════════════════════════════════════════════════════════════════════════════
# BUILD ANIMATIONS
# ══════════════════════════════════════════════════════════════════════════════
# All bone UUIDs in order
all_bids = (beam_layer_bids + ttip_bids + flick_bids + emit_bids + iburst_bids +
            bedge_bids + tsr_bids + er_bids + bcg_bids + tip_aura_bids + wake_bids)

def make_animator(bone_uuid, bone_name, kf_list):
    return {bone_uuid: {"name": bone_name, "type": "bone", "keyframes": kf_list}}

def build_animators_spawn():
    animators = {}
    # spawn (once, 0.5s): emitter opens, beam layers extend, flickers appear, tip opens, impact burst
    for i, (buid, bname) in enumerate(zip(beam_layer_bids, [d[0] for d in beam_layer_defs])):
        delay = i * 0.04
        animators[buid] = {"name":bname,"type":"bone","keyframes":[
            kf(0.0,         0,  0, 0, "scale"),
            kf(delay,       0,  0, 0, "scale"),
            kf(delay+0.06,  1,  1, 1, "scale"),
            kf(0.5,         1,  1, 1, "scale"),
            kf(0.0,         0,  0, -6, "position"),
            kf(delay,       0,  0, -6, "position"),
            kf(delay+0.08,  0,  0,  0, "position"),
            kf(0.5,         0,  0,  0, "position"),
            kf(0.0,  0,0,0, "rotation"),
            kf(0.25, 0,0,0, "rotation"),
            kf(0.5,  0,0,0, "rotation"),
        ]}
    for i, (buid, ttname) in enumerate(zip(ttip_bids, [d[0] for d in ttip_defs])):
        animators[buid] = {"name":ttname,"type":"bone","keyframes":[
            kf(0.0,   0,0,0,"scale"),
            kf(0.38,  0,0,0,"scale"),
            kf(0.46,  1,1,1,"scale"),
            kf(0.5,   1,1,1,"scale"),
            kf(0.0,   0,0,0,"position"),
            kf(0.38,  0,0,0,"position"),
            kf(0.5,   0,0,0,"position"),
            kf(0.0,   0,0,0,"rotation"),
            kf(0.25,  0,0,0,"rotation"),
            kf(0.5,   0,0,0,"rotation"),
        ]}
    for i, (buid, fname) in enumerate(zip(flick_bids, [f"flick_{j+1}" for j in range(8)])):
        animators[buid] = {"name":fname,"type":"bone","keyframes":[
            kf(0.0,   0,0,0,"scale"),
            kf(0.18,  0,0,0,"scale"),
            kf(0.26,  1,1,1,"scale"),
            kf(0.5,   1,1,1,"scale"),
            kf(0.0,   0,0,0,"position"),
            kf(0.18,  0,0,0,"position"),
            kf(0.5,   0,0,0,"position"),
            kf(0.0,   0,0,0,"rotation"),
            kf(0.25,  0,0,0,"rotation"),
            kf(0.5,   0,0,0,"rotation"),
        ]}
    for i, (buid, ename) in enumerate(zip(emit_bids, [f"emit_{j+1}" for j in range(6)])):
        animators[buid] = {"name":ename,"type":"bone","keyframes":[
            kf(0.0,   0,0,0,"scale"),
            kf(0.04,  1,1,1,"scale"),
            kf(0.5,   1,1,1,"scale"),
            kf(0.0,   0,0,0,"position"),
            kf(0.04,  0,0,0,"position"),
            kf(0.5,   0,0,0,"position"),
            kf(0.0,   0,0,0,"rotation"),
            kf(0.04,  0,0,0,"rotation"),
            kf(0.5,   0,0,0,"rotation"),
        ]}
    for i, (buid, iname) in enumerate(zip(iburst_bids, [f"iburst_{j+1}" for j in range(8)])):
        animators[buid] = {"name":iname,"type":"bone","keyframes":[
            kf(0.0,   0,0,0,"scale"),
            kf(0.43,  0,0,0,"scale"),
            kf(0.5,   1,1,1,"scale"),
            kf(0.0,   0,0,0,"position"),
            kf(0.43,  0,0,0,"position"),
            kf(0.5,   0,0,0,"position"),
            kf(0.0,   0,0,0,"rotation"),
            kf(0.25,  0,0,0,"rotation"),
            kf(0.5,   0,0,0,"rotation"),
        ]}
    for i, (buid, bname) in enumerate(zip(bedge_bids, [f"bedge_{j+1}" for j in range(12)])):
        delay = (i % 6) * 0.04
        animators[buid] = {"name":bname,"type":"bone","keyframes":[
            kf(0.0,        0,0,0,"scale"),
            kf(delay+0.04, 1,1,1,"scale"),
            kf(0.5,        1,1,1,"scale"),
            kf(0.0,        0,0,0,"position"),
            kf(delay+0.04, 0,0,0,"position"),
            kf(0.5,        0,0,0,"position"),
            kf(0.0,        0,0,0,"rotation"),
            kf(0.25,       0,0,0,"rotation"),
            kf(0.5,        0,0,0,"rotation"),
        ]}
    for i, (buid, rname) in enumerate(zip(tsr_bids, [f"tsr_{j+1}" for j in range(8)])):
        animators[buid] = {"name":rname,"type":"bone","keyframes":[
            kf(0.0,   0,0,0,"scale"),
            kf(0.22,  1,1,1,"scale"),
            kf(0.5,   1,1,1,"scale"),
            kf(0.0,   0,0,0,"position"),
            kf(0.22,  0,0,0,"position"),
            kf(0.5,   0,0,0,"position"),
            kf(0.0,   0,0,0,"rotation"),
            kf(0.25,  0,0,0,"rotation"),
            kf(0.5,   0,0,0,"rotation"),
        ]}
    for i, (buid, rname) in enumerate(zip(er_bids, [f"er_{j+1}" for j in range(8)])):
        animators[buid] = {"name":rname,"type":"bone","keyframes":[
            kf(0.0,   0,0,0,"scale"),
            kf(0.04,  1,1,1,"scale"),
            kf(0.5,   1,1,1,"scale"),
            kf(0.0,   0,0,0,"position"),
            kf(0.04,  0,0,0,"position"),
            kf(0.5,   0,0,0,"position"),
            kf(0.0,   0,0,0,"rotation"),
            kf(0.04,  0,0,0,"rotation"),
            kf(0.5,   0,0,0,"rotation"),
        ]}
    for i, (buid, gname) in enumerate(zip(bcg_bids, [f"bcg_{j+1}" for j in range(5)])):
        animators[buid] = {"name":gname,"type":"bone","keyframes":[
            kf(0.0,   0,0,0,"scale"),
            kf(0.12,  1,1,1,"scale"),
            kf(0.5,   1,1,1,"scale"),
            kf(0.0,   0,0,0,"position"),
            kf(0.12,  0,0,0,"position"),
            kf(0.5,   0,0,0,"position"),
            kf(0.0,   0,0,0,"rotation"),
            kf(0.25,  0,0,0,"rotation"),
            kf(0.5,   0,0,0,"rotation"),
        ]}
    for i, (buid, aname) in enumerate(zip(tip_aura_bids, [f"tip_aura_{j+1}" for j in range(10)])):
        animators[buid] = {"name":aname,"type":"bone","keyframes":[
            kf(0.0,   0,0,0,"scale"),
            kf(0.42,  1,1,1,"scale"),
            kf(0.5,   1,1,1,"scale"),
            kf(0.0,   0,0,0,"position"),
            kf(0.42,  0,0,0,"position"),
            kf(0.5,   0,0,0,"position"),
            kf(0.0,   0,0,0,"rotation"),
            kf(0.25,  0,0,0,"rotation"),
            kf(0.5,   0,0,0,"rotation"),
        ]}
    for i, (buid, wname) in enumerate(zip(wake_bids, [f"wake_beam_{j+1}" for j in range(10)])):
        animators[buid] = {"name":wname,"type":"bone","keyframes":[
            kf(0.0,   0,0,0,"scale"),
            kf(0.35,  1,1,1,"scale"),
            kf(0.5,   1,1,1,"scale"),
            kf(0.0,   0,0,0,"position"),
            kf(0.35,  0,0,0,"position"),
            kf(0.5,   0,0,0,"position"),
            kf(0.0,   0,0,0,"rotation"),
            kf(0.25,  0,0,0,"rotation"),
            kf(0.5,   0,0,0,"rotation"),
        ]}
    return animators


def build_animators_idle():
    animators = {}
    # idle (loop, 2.5s): beam undulates, flickers drift, tip opens/closes, emitter rotates, impact rotates
    for i, (buid, bname) in enumerate(zip(beam_layer_bids, [d[0] for d in beam_layer_defs])):
        phase = i * 0.3
        amp = 0.4
        animators[buid] = {"name":bname,"type":"bone","keyframes":[
            kf(0.0,   0, amp*math.sin(phase),            0, "position"),
            kf(0.42,  0, amp*math.sin(phase+math.pi/2),  0, "position"),
            kf(0.83,  0, amp*math.sin(phase+math.pi),    0, "position"),
            kf(1.25,  0, amp*math.sin(phase+3*math.pi/2),0, "position"),
            kf(1.67,  0, amp*math.sin(phase+2*math.pi),  0, "position"),
            kf(2.08,  0, amp*math.sin(phase+5*math.pi/2),0, "position"),
            kf(2.5,   0, amp*math.sin(phase),            0, "position"),
            kf(0.0,   0,0,0, "rotation"),
            kf(0.5,   0, i*1.5-3, 0, "rotation"),
            kf(1.25,  0, 0,       0, "rotation"),
            kf(2.0,   0, -(i*1.5-3), 0, "rotation"),
            kf(2.5,   0, 0,       0, "rotation"),
            kf(0.0,   1,1,1, "scale"),
            kf(1.25,  1,1,1, "scale"),
            kf(2.5,   1,1,1, "scale"),
        ]}
    for i, (buid, ttname) in enumerate(zip(ttip_bids, [d[0] for d in ttip_defs])):
        side = 1 if i % 2 == 0 else -1
        animators[buid] = {"name":ttname,"type":"bone","keyframes":[
            kf(0.0,   0, 0, 0, "position"),
            kf(0.5,   side*0.2, 0.05, 0, "position"),
            kf(1.0,   0, 0, 0.15, "position"),
            kf(1.5,   side*-0.1, -0.05, 0, "position"),
            kf(2.0,   0, 0, 0, "position"),
            kf(2.5,   0, 0, 0, "position"),
            kf(0.0,   0, 0, 0, "rotation"),
            kf(0.625, side*5, 0, 0, "rotation"),
            kf(1.25,  0, 0, 0, "rotation"),
            kf(1.875, side*-5, 0, 0, "rotation"),
            kf(2.5,   0, 0, 0, "rotation"),
            kf(0.0,   1,1,1, "scale"),
            kf(1.25,  1,1,1, "scale"),
            kf(2.5,   1,1,1, "scale"),
        ]}
    for i, (buid, fname) in enumerate(zip(flick_bids, [f"flick_{j+1}" for j in range(8)])):
        drift = 12.0 / 8  # drift from emitter to impact over loop
        animators[buid] = {"name":fname,"type":"bone","keyframes":[
            kf(0.0,   0, 0, -i*drift*0.15, "position"),
            kf(0.5,   0, 0.08, -i*drift*0.05, "position"),
            kf(1.0,   0, 0, drift*0.3, "position"),
            kf(1.5,   0, -0.08, drift*0.6, "position"),
            kf(2.0,   0, 0, drift, "position"),
            kf(2.5,   0, 0, -i*drift*0.15, "position"),
            kf(0.0,   0, i*5.0, 0, "rotation"),
            kf(0.83,  0, i*5.0+15, 0, "rotation"),
            kf(1.67,  0, i*5.0+30, 0, "rotation"),
            kf(2.5,   0, i*5.0+45, 0, "rotation"),
            kf(0.0,   1,1,1, "scale"),
            kf(1.25,  1,1,1, "scale"),
            kf(2.5,   1,1,1, "scale"),
        ]}
    for i, (buid, ename) in enumerate(zip(emit_bids, [f"emit_{j+1}" for j in range(6)])):
        spin = 360.0 / 6
        animators[buid] = {"name":ename,"type":"bone","keyframes":[
            kf(0.0,   0, i*spin, 0, "rotation"),
            kf(0.5,   0, i*spin+30, 0, "rotation"),
            kf(1.0,   0, i*spin+60, 0, "rotation"),
            kf(1.5,   0, i*spin+90, 0, "rotation"),
            kf(2.0,   0, i*spin+120, 0, "rotation"),
            kf(2.5,   0, i*spin+150, 0, "rotation"),
            kf(0.0,   0, 0, 0, "position"),
            kf(0.83,  0, 0, 0.1, "position"),
            kf(1.67,  0, 0, -0.1, "position"),
            kf(2.5,   0, 0, 0, "position"),
            kf(0.0,   1,1,1, "scale"),
            kf(1.25,  1,1,1, "scale"),
            kf(2.5,   1,1,1, "scale"),
        ]}
    for i, (buid, iname) in enumerate(zip(iburst_bids, [f"iburst_{j+1}" for j in range(8)])):
        speed = 360.0 / 2.5
        animators[buid] = {"name":iname,"type":"bone","keyframes":[
            kf(0.0,   0, i*45, 0, "rotation"),
            kf(0.5,   0, i*45+speed*0.5, 0, "rotation"),
            kf(1.0,   0, i*45+speed, 0, "rotation"),
            kf(1.5,   0, i*45+speed*1.5, 0, "rotation"),
            kf(2.0,   0, i*45+speed*2.0, 0, "rotation"),
            kf(2.5,   0, i*45+speed*2.5, 0, "rotation"),
            kf(0.0,   0, 0, 0, "position"),
            kf(0.83,  0, 0.1, 0, "position"),
            kf(1.67,  0, -0.1, 0, "position"),
            kf(2.5,   0, 0, 0, "position"),
            kf(0.0,   1,1,1, "scale"),
            kf(1.25,  1,1,1, "scale"),
            kf(2.5,   1,1,1, "scale"),
        ]}
    for i, (buid, bname) in enumerate(zip(bedge_bids, [f"bedge_{j+1}" for j in range(12)])):
        phase = i * 0.2
        animators[buid] = {"name":bname,"type":"bone","keyframes":[
            kf(0.0,   0, 0.05*math.sin(phase), 0, "position"),
            kf(0.5,   0, 0.05*math.sin(phase+1), 0, "position"),
            kf(1.0,   0, 0.05*math.sin(phase+2), 0, "position"),
            kf(1.5,   0, 0.05*math.sin(phase+3), 0, "position"),
            kf(2.0,   0, 0.05*math.sin(phase+4), 0, "position"),
            kf(2.5,   0, 0.05*math.sin(phase), 0, "position"),
            kf(0.0,   0, 0, 0, "rotation"),
            kf(0.83,  0, 2, 0, "rotation"),
            kf(1.67,  0, -2, 0, "rotation"),
            kf(2.5,   0, 0, 0, "rotation"),
            kf(0.0,   1,1,1, "scale"),
            kf(1.25,  1,1,1, "scale"),
            kf(2.5,   1,1,1, "scale"),
        ]}
    for i, (buid, rname) in enumerate(zip(tsr_bids, [f"tsr_{j+1}" for j in range(8)])):
        drift_z = 0.5 + i * 0.08
        animators[buid] = {"name":rname,"type":"bone","keyframes":[
            kf(0.0,   0, 0, 0, "position"),
            kf(0.42,  0, 0.06, drift_z*0.25, "position"),
            kf(0.83,  0, 0, drift_z*0.5, "position"),
            kf(1.25,  0, -0.06, drift_z*0.75, "position"),
            kf(1.67,  0, 0, drift_z, "position"),
            kf(2.5,   0, 0, 0, "position"),
            kf(0.0,   0, i*8, 0, "rotation"),
            kf(0.83,  0, i*8+10, 0, "rotation"),
            kf(1.67,  0, i*8+20, 0, "rotation"),
            kf(2.5,   0, i*8+30, 0, "rotation"),
            kf(0.0,   1,1,1, "scale"),
            kf(1.25,  1,1,1, "scale"),
            kf(2.5,   1,1,1, "scale"),
        ]}
    for i, (buid, rname) in enumerate(zip(er_bids, [f"er_{j+1}" for j in range(8)])):
        animators[buid] = {"name":rname,"type":"bone","keyframes":[
            kf(0.0,   0, i*45, 0, "rotation"),
            kf(0.5,   0, i*45+18, 0, "rotation"),
            kf(1.0,   0, i*45+36, 0, "rotation"),
            kf(1.5,   0, i*45+54, 0, "rotation"),
            kf(2.0,   0, i*45+72, 0, "rotation"),
            kf(2.5,   0, i*45+90, 0, "rotation"),
            kf(0.0,   0, 0, 0, "position"),
            kf(0.83,  0.05*math.sin(i), 0, 0, "position"),
            kf(1.67,  -0.05*math.sin(i), 0, 0, "position"),
            kf(2.5,   0, 0, 0, "position"),
            kf(0.0,   1,1,1, "scale"),
            kf(1.25,  1,1,1, "scale"),
            kf(2.5,   1,1,1, "scale"),
        ]}
    for i, (buid, gname) in enumerate(zip(bcg_bids, [f"bcg_{j+1}" for j in range(5)])):
        phase = i * 0.4
        animators[buid] = {"name":gname,"type":"bone","keyframes":[
            kf(0.0,   1, 1, 1, "scale"),
            kf(0.5,   1.05, 1.05, 1, "scale"),
            kf(1.0,   1, 1, 1, "scale"),
            kf(1.5,   0.95, 0.95, 1, "scale"),
            kf(2.0,   1, 1, 1, "scale"),
            kf(2.5,   1, 1, 1, "scale"),
            kf(0.0,   0, 0, 0, "position"),
            kf(0.83,  0, 0.05*math.sin(phase), 0, "position"),
            kf(1.67,  0, -0.05*math.sin(phase), 0, "position"),
            kf(2.5,   0, 0, 0, "position"),
            kf(0.0,   0,0,0, "rotation"),
            kf(1.25,  0,0,0, "rotation"),
            kf(2.5,   0,0,0, "rotation"),
        ]}
    for i, (buid, aname) in enumerate(zip(tip_aura_bids, [f"tip_aura_{j+1}" for j in range(10)])):
        animators[buid] = {"name":aname,"type":"bone","keyframes":[
            kf(0.0,   0, i*36, 0, "rotation"),
            kf(0.5,   0, i*36+20, 0, "rotation"),
            kf(1.0,   0, i*36+40, 0, "rotation"),
            kf(1.5,   0, i*36+60, 0, "rotation"),
            kf(2.0,   0, i*36+80, 0, "rotation"),
            kf(2.5,   0, i*36+100, 0, "rotation"),
            kf(0.0,   0, 0, 0, "position"),
            kf(0.83,  0.08*math.sin(i), 0, 0, "position"),
            kf(1.67,  -0.08*math.sin(i), 0, 0, "position"),
            kf(2.5,   0, 0, 0, "position"),
            kf(0.0,   1,1,1, "scale"),
            kf(1.25,  1,1,1, "scale"),
            kf(2.5,   1,1,1, "scale"),
        ]}
    for i, (buid, wname) in enumerate(zip(wake_bids, [f"wake_beam_{j+1}" for j in range(10)])):
        phase = i * 0.25
        animators[buid] = {"name":wname,"type":"bone","keyframes":[
            kf(0.0,   0, 0.06*math.sin(phase), 0, "position"),
            kf(0.5,   0, 0.06*math.sin(phase+1), 0, "position"),
            kf(1.0,   0, 0.06*math.sin(phase+2), 0, "position"),
            kf(1.5,   0, 0.06*math.sin(phase+3), 0, "position"),
            kf(2.0,   0, 0.06*math.sin(phase+4), 0, "position"),
            kf(2.5,   0, 0.06*math.sin(phase), 0, "position"),
            kf(0.0,   0, 0, 0, "rotation"),
            kf(0.83,  0, 5, 0, "rotation"),
            kf(1.67,  0, -5, 0, "rotation"),
            kf(2.5,   0, 0, 0, "rotation"),
            kf(0.0,   1,1,1, "scale"),
            kf(1.25,  1,1,1, "scale"),
            kf(2.5,   1,1,1, "scale"),
        ]}
    return animators


def build_animators_dissipate():
    animators = {}
    # dissipate (once, 0.4s): tip retracts, beam layers scale to 0 from impact end, flickers fade, emitter closes, impact bursts
    for i, (buid, bname) in enumerate(zip(beam_layer_bids, [d[0] for d in beam_layer_defs])):
        # Collapse from impact end backward — layer 6 first, layer 1 last
        delay = (5 - i) * 0.04
        animators[buid] = {"name":bname,"type":"bone","keyframes":[
            kf(0.0,        1,1,1, "scale"),
            kf(delay,      1,1,1, "scale"),
            kf(delay+0.08, 0,0,0, "scale"),
            kf(0.4,        0,0,0, "scale"),
            kf(0.0,   0, 0, 0, "position"),
            kf(delay, 0, 0, 0, "position"),
            kf(delay+0.08, 0, 0, 2, "position"),
            kf(0.4,   0, 0, 2, "position"),
            kf(0.0,   0,0,0, "rotation"),
            kf(0.2,   0,0,0, "rotation"),
            kf(0.4,   0,0,0, "rotation"),
        ]}
    for i, (buid, ttname) in enumerate(zip(ttip_bids, [d[0] for d in ttip_defs])):
        side = 1 if i % 2 == 0 else -1
        animators[buid] = {"name":ttname,"type":"bone","keyframes":[
            kf(0.0,   1,1,1, "scale"),
            kf(0.08,  1,1,1, "scale"),
            kf(0.18,  0,0,0, "scale"),
            kf(0.4,   0,0,0, "scale"),
            kf(0.0,   0, 0, 0, "position"),
            kf(0.08,  side*0.3, 0.2, -0.5, "position"),
            kf(0.18,  side*0.5, 0.4, -1.0, "position"),
            kf(0.4,   side*0.8, 0.6, -2.0, "position"),
            kf(0.0,   0, 0, 0, "rotation"),
            kf(0.18,  side*-40, 0, 0, "rotation"),
            kf(0.4,   side*-80, 0, 0, "rotation"),
        ]}
    for i, (buid, fname) in enumerate(zip(flick_bids, [f"flick_{j+1}" for j in range(8)])):
        delay = i * 0.02
        animators[buid] = {"name":fname,"type":"bone","keyframes":[
            kf(0.0,        1,1,1, "scale"),
            kf(delay+0.05, 0,0,0, "scale"),
            kf(0.4,        0,0,0, "scale"),
            kf(0.0,   0, 0, 0, "position"),
            kf(delay, 0, 0.1, 0, "position"),
            kf(delay+0.05, 0, 0.3, 0.5, "position"),
            kf(0.4,   0, 0.5, 1.0, "position"),
            kf(0.0,   0,0,0, "rotation"),
            kf(0.2,   0,0,0, "rotation"),
            kf(0.4,   0,0,0, "rotation"),
        ]}
    for i, (buid, ename) in enumerate(zip(emit_bids, [f"emit_{j+1}" for j in range(6)])):
        animators[buid] = {"name":ename,"type":"bone","keyframes":[
            kf(0.0,   1,1,1, "scale"),
            kf(0.2,   1,1,1, "scale"),
            kf(0.35,  0,0,0, "scale"),
            kf(0.4,   0,0,0, "scale"),
            kf(0.0,   0, 0, 0, "position"),
            kf(0.2,   0, 0, 0, "position"),
            kf(0.4,   0, 0, 0, "position"),
            kf(0.0,   0,0,0, "rotation"),
            kf(0.2,   0, 45, 0, "rotation"),
            kf(0.4,   0, 90, 0, "rotation"),
        ]}
    for i, (buid, iname) in enumerate(zip(iburst_bids, [f"iburst_{j+1}" for j in range(8)])):
        animators[buid] = {"name":iname,"type":"bone","keyframes":[
            kf(0.0,   1,1,1, "scale"),
            kf(0.15,  1.5,1.5,1.5, "scale"),
            kf(0.3,   2.0,2.0,2.0, "scale"),
            kf(0.4,   0,0,0, "scale"),
            kf(0.0,   0, 0, 0, "position"),
            kf(0.15,  0, 0.2, 0, "position"),
            kf(0.3,   0, 0.4, 0, "position"),
            kf(0.4,   0, 0.6, 0, "position"),
            kf(0.0,   0, i*45, 0, "rotation"),
            kf(0.2,   0, i*45+60, 0, "rotation"),
            kf(0.4,   0, i*45+120, 0, "rotation"),
        ]}
    for i, (buid, bname) in enumerate(zip(bedge_bids, [f"bedge_{j+1}" for j in range(12)])):
        delay = (11 - i) * 0.02
        animators[buid] = {"name":bname,"type":"bone","keyframes":[
            kf(0.0,        1,1,1, "scale"),
            kf(delay,      1,1,1, "scale"),
            kf(delay+0.06, 0,0,0, "scale"),
            kf(0.4,        0,0,0, "scale"),
            kf(0.0,   0,0,0, "position"),
            kf(delay, 0,0,0, "position"),
            kf(0.4,   0,0,0, "position"),
            kf(0.0,   0,0,0, "rotation"),
            kf(0.2,   0,0,0, "rotation"),
            kf(0.4,   0,0,0, "rotation"),
        ]}
    for i, (buid, rname) in enumerate(zip(tsr_bids, [f"tsr_{j+1}" for j in range(8)])):
        animators[buid] = {"name":rname,"type":"bone","keyframes":[
            kf(0.0,   1,1,1, "scale"),
            kf(0.15,  0.5,0.5,0.5, "scale"),
            kf(0.3,   0,0,0, "scale"),
            kf(0.4,   0,0,0, "scale"),
            kf(0.0,   0, 0, 0, "position"),
            kf(0.15,  0, 0.15, 0, "position"),
            kf(0.3,   0, 0.3, 0, "position"),
            kf(0.4,   0, 0.5, 0, "position"),
            kf(0.0,   0,0,0, "rotation"),
            kf(0.2,   0,0,0, "rotation"),
            kf(0.4,   0,0,0, "rotation"),
        ]}
    for i, (buid, rname) in enumerate(zip(er_bids, [f"er_{j+1}" for j in range(8)])):
        animators[buid] = {"name":rname,"type":"bone","keyframes":[
            kf(0.0,   1,1,1, "scale"),
            kf(0.25,  1,1,1, "scale"),
            kf(0.4,   0,0,0, "scale"),
            kf(0.0,   0,0,0, "position"),
            kf(0.25,  0,0,0, "position"),
            kf(0.4,   0,0,0, "position"),
            kf(0.0,   0, i*45, 0, "rotation"),
            kf(0.2,   0, i*45+30, 0, "rotation"),
            kf(0.4,   0, i*45+60, 0, "rotation"),
        ]}
    for i, (buid, gname) in enumerate(zip(bcg_bids, [f"bcg_{j+1}" for j in range(5)])):
        animators[buid] = {"name":gname,"type":"bone","keyframes":[
            kf(0.0,   1,1,1, "scale"),
            kf(0.15,  1.2,1.2,1, "scale"),
            kf(0.3,   0,0,0, "scale"),
            kf(0.4,   0,0,0, "scale"),
            kf(0.0,   0,0,0, "position"),
            kf(0.15,  0,0,0, "position"),
            kf(0.4,   0,0,0, "position"),
            kf(0.0,   0,0,0, "rotation"),
            kf(0.2,   0,0,0, "rotation"),
            kf(0.4,   0,0,0, "rotation"),
        ]}
    for i, (buid, aname) in enumerate(zip(tip_aura_bids, [f"tip_aura_{j+1}" for j in range(10)])):
        animators[buid] = {"name":aname,"type":"bone","keyframes":[
            kf(0.0,   1,1,1, "scale"),
            kf(0.1,   2,2,2, "scale"),
            kf(0.25,  0,0,0, "scale"),
            kf(0.4,   0,0,0, "scale"),
            kf(0.0,   0, 0, 0, "position"),
            kf(0.1,   0, 0.2, 0, "position"),
            kf(0.25,  0, 0.5, 0, "position"),
            kf(0.4,   0, 1.0, 0, "position"),
            kf(0.0,   0, i*36, 0, "rotation"),
            kf(0.2,   0, i*36+45, 0, "rotation"),
            kf(0.4,   0, i*36+90, 0, "rotation"),
        ]}
    for i, (buid, wname) in enumerate(zip(wake_bids, [f"wake_beam_{j+1}" for j in range(10)])):
        delay = (9 - i) * 0.015
        animators[buid] = {"name":wname,"type":"bone","keyframes":[
            kf(0.0,        1,1,1, "scale"),
            kf(delay,      1,1,1, "scale"),
            kf(delay+0.05, 0,0,0, "scale"),
            kf(0.4,        0,0,0, "scale"),
            kf(0.0,   0,0,0, "position"),
            kf(delay, 0,0,0, "position"),
            kf(0.4,   0,0,0, "position"),
            kf(0.0,   0,0,0, "rotation"),
            kf(0.2,   0,0,0, "rotation"),
            kf(0.4,   0,0,0, "rotation"),
        ]}
    return animators

# ── Build model ─────────────────────────────────────────────────────────────
spawn_anim = {
    "uuid": aid(1),
    "name": "animation.devils_tongue_beam.spawn",
    "loop": "once",
    "length": 0.5,
    "snapping": 24,
    "selected": False,
    "saved": False,
    "animators": build_animators_spawn()
}
idle_anim = {
    "uuid": aid(2),
    "name": "animation.devils_tongue_beam.idle",
    "loop": "loop",
    "length": 2.5,
    "snapping": 24,
    "selected": False,
    "saved": False,
    "animators": build_animators_idle()
}
dissipate_anim = {
    "uuid": aid(3),
    "name": "animation.devils_tongue_beam.dissipate",
    "loop": "once",
    "length": 0.4,
    "snapping": 24,
    "selected": False,
    "saved": False,
    "animators": build_animators_dissipate()
}

# Outliner = list of bone UUIDs (flat top-level)
outliner = [b["uuid"] for b in bones]

textures = [
    {"id":"0","name":"devils_tongue_beam_tex","relative_path":"../textures/devils_tongue_beam_tex.png",
     "folder":"devilsdream","namespace":"","visible":True,"mode":"bitmap","saved":False,
     "uuid":tid(1),"source":f"data:image/png;base64,{b64_0}"},
    {"id":"1","name":"devils_tongue_beam_tex1","relative_path":"../textures/devils_tongue_beam_tex1.png",
     "folder":"devilsdream","namespace":"","visible":True,"mode":"bitmap","saved":False,
     "uuid":tid(2),"source":f"data:image/png;base64,{b64_1}"},
]

model = {
    "meta": {"format_version":"4.10","model_format":"free","box_uv":False},
    "name": "devils_tongue_beam",
    "geometry": "geometry.devils_tongue_beam",
    "resolution": {"width":64,"height":64},
    "elements": elements,
    "outliner": outliner,
    "textures": textures,
    "animations": [spawn_anim, idle_anim, dissipate_anim]
}

out_path = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/devils_tongue_beam.bbmodel"
with open(out_path, "w", encoding="utf-8") as f:
    json.dump(model, f, separators=(',', ':'))

size = os.path.getsize(out_path)
print(f"Written: {out_path}")
print(f"Size: {size:,} bytes ({size/1024:.1f} KB)")
if size < 300*1024:
    print(f"WARNING: {size/1024:.1f} KB < 300 KB minimum!")
else:
    print("PASS: ≥300 KB")
