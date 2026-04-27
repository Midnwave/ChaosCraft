"""
Build silver_wing_blade.bbmodel
81 elements, 81 bones, 3 animations, 6+ keyframes per bone per animation
Large feather blade pointing in Z direction, ~18 units long, spans +-3 on X
"""
import json, math, os

b64_0 = open("D:/CC/ChaosCraft/tools/gen_textures/m12_b64.txt").read().strip()
b64_1 = open("D:/CC/ChaosCraft/tools/gen_textures/m12_tex1_b64.txt").read().strip()

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

# ── 1. RACHIS SPINE (5 bones, 5 elements) ─────────────────────────────────────
# Large feather blade from Z=0 to Z=15, with quill behind at Z<0
# Spans X: -1.75 to +1.75 at widest (seg 1)
rach_defs = [
    ("rach_1", [-1.75,0,0],[1.75,0.4,3]),    # widest base
    ("rach_2", [-1.5, 0,3],[1.5, 0.35,6]),
    ("rach_3", [-1.2, 0,6],[1.2, 0.3, 9]),
    ("rach_4", [-0.8, 0,9],[0.8, 0.25,12]),
    ("rach_5", [-0.4, 0,12],[0.4,0.2,15]),   # narrowest tip
]
rach_bids = []
for name, fr, to in rach_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [0,0,fr[2]], [eid(ei)]); bones.append(b)
    rach_bids.append(bid(bi)); ei+=1; bi+=1

# ── 2. VANE PANELS (10 bones, 10 elements) ─────────────────────────────────────
# Left and Right panels for each rachis segment
# Vane extends from rachis edge sideways; widths taper
vane_extents = [2.75, 2.30, 1.80, 1.20, 0.60]
vane_bids_L = []; vane_bids_R = []
for i, (rd_name, fr, to) in enumerate(rach_defs):
    z0 = fr[2] + 0.3
    z1 = to[2] - 0.3
    ext = vane_extents[i]
    rach_x_edge = fr[0]  # -1.75, -1.5, etc.
    # Left vane: from [-rach_x_edge - ext, 0, z0] to [rach_x_edge, 0.25, z1]
    lname = f"vane_L{i+1}"
    lf = [rach_x_edge - ext, 0, z0]; lt = [rach_x_edge, 0.25, z1]
    e = elem(ei, lname, lf, lt, UV0); elements.append(e)
    b = bone(bi, lname, [rach_x_edge, 0, z0], [eid(ei)]); bones.append(b)
    vane_bids_L.append(bid(bi)); ei+=1; bi+=1
    # Right vane: mirror
    rname = f"vane_R{i+1}"
    rx_edge = to[0]  # positive X edge of rachis
    rf = [rx_edge, 0, z0]; rt = [rx_edge + ext, 0.25, z1]
    e = elem(ei, rname, rf, rt, UV0); elements.append(e)
    b = bone(bi, rname, [rx_edge, 0, z0], [eid(ei)]); bones.append(b)
    vane_bids_R.append(bid(bi)); ei+=1; bi+=1

vane_bids = vane_bids_L + vane_bids_R

# ── 3. BLADE EDGE ACCENTS (4 bones, 4 elements) ─────────────────────────────────
# Thin slab along leading edge of vane segs 1-2 both sides, tex1
bedge_defs = [
    ("bedge_1", [-4.5+0.0, 0.2, 0.3], [-4.35, 0.35, 3.0]),   # L seg1 outer
    ("bedge_2", [ 1.75,    0.2, 0.3], [ 4.5,  0.35, 3.0]),    # R seg1 outer
    ("bedge_3", [-3.8+0.0, 0.2, 3.3], [-3.65, 0.35, 5.7]),   # L seg2 outer
    ("bedge_4", [ 1.5,     0.2, 3.3], [ 3.8,  0.35, 5.7]),   # R seg2 outer
]
bedge_bids = []
for name, fr, to in bedge_defs:
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    b = bone(bi, name, [(fr[0]+to[0])/2, 0, fr[2]], [eid(ei)]); bones.append(b)
    bedge_bids.append(bid(bi)); ei+=1; bi+=1

# ── 4. QUILL (2 bones, 2 elements) ────────────────────────────────────────────
quill_defs = [
    ("quill_1", [-0.6,0,-2.5],[0.6,0.5,-0.5]),
    ("quill_2", [-0.4,-0.2,-3.5],[0.4,0.7,-2.5]),
]
quill_bids = []
for name, fr, to in quill_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [0,0,fr[2]], [eid(ei)]); bones.append(b)
    quill_bids.append(bid(bi)); ei+=1; bi+=1

# ── 5. TRAILING DUST (8 bones, 8 elements) ────────────────────────────────────
# tiny 0.3x0.3x0.2 cubes in comet tail Z=-4 to -10, scattered X/Y, tex1
dust_positions = [
    (0.4, 0.2, -4.5), (-0.5, 0.3, -5.2), (0.2, -0.1, -6.0), (-0.3, 0.4, -6.8),
    (0.5, 0.1, -7.5), (-0.1, -0.2, -8.3), (0.3, 0.3, -9.0), (-0.4, 0.0, -9.8),
]
dust_bids = []
for i, (dx, dy, dz) in enumerate(dust_positions):
    name = f"dust_{i+1}"
    fr = [dx-0.15, dy-0.15, dz]; to = [dx+0.15, dy+0.15, dz+0.2]
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    b = bone(bi, name, [dx, dy, dz], [eid(ei)]); bones.append(b)
    dust_bids.append(bid(bi)); ei+=1; bi+=1

# ── 6. VANE DETAIL (16 bones, 16 elements) ────────────────────────────────────
# Thin accent slab pairs along vane surfaces at intervals (horizontal barb lines)
vd_bids = []
for i in range(16):
    name = f"vd_{i+1}"
    seg = i // 4  # 4 details per pair of segs
    side = 1 if i % 2 == 0 else -1
    seg_z = seg * 3.0 + 0.5 + (i % 4) * 0.5
    ext = vane_extents[min(seg, 4)] * 0.8
    rach_x = rach_defs[min(seg, 4)][1][0] + 0.05  # positive X edge
    if side > 0:
        fr = [rach_x, 0.22, seg_z]; to = [rach_x + ext, 0.30, seg_z + 0.12]
    else:
        fr = [-rach_x - ext, 0.22, seg_z]; to = [-rach_x, 0.30, seg_z + 0.12]
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [fr[0], 0, seg_z], [eid(ei)]); bones.append(b)
    vd_bids.append(bid(bi)); ei+=1; bi+=1

# ── 7. RACHIS DETAIL (10 bones, 10 elements) ──────────────────────────────────
# Small raised center strip on each rachis segment (5 segs * 2 detail pieces)
rd_bids = []
for i in range(10):
    name = f"rd_{i+1}"
    seg = i // 2
    piece = i % 2
    fr_z = rach_defs[seg][1][2] if piece == 0 else rach_defs[seg][2][2] - 1.5
    to_z = fr_z + 1.3
    fr = [-0.12, 0.38, fr_z]; to = [0.12, 0.52, to_z]
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [0, 0, fr_z], [eid(ei)]); bones.append(b)
    rd_bids.append(bid(bi)); ei+=1; bi+=1

# ── 8. DUST EXTENDED (8 bones, 8 elements) ────────────────────────────────────
# More dust pieces extending the comet tail further, tex1
dust_ext_positions = [
    (0.6, 0.1, -10.5), (-0.5, 0.4, -11.2), (0.1, -0.3, -11.8), (-0.3, 0.2, -12.5),
    (0.4, 0.3, -13.0), (-0.2, -0.1, -13.6), (0.3, 0.1, -14.2), (-0.1, 0.3, -14.8),
]
de_bids = []
for i, (dx, dy, dz) in enumerate(dust_ext_positions):
    name = f"de_{i+1}"
    fr = [dx-0.12, dy-0.12, dz]; to = [dx+0.12, dy+0.12, dz+0.15]
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    b = bone(bi, name, [dx, dy, dz], [eid(ei)]); bones.append(b)
    de_bids.append(bid(bi)); ei+=1; bi+=1

# ── 9. AURA GLOW SLABS (8 bones, 8 elements) ──────────────────────────────────
# Thin flat glow slabs around the blade edge, tex1
ag_bids = []
ag_positions = [
    ([-1.85, 0.1, 0.5], [-1.70, 0.35, 2.5]),
    ([ 1.70, 0.1, 0.5], [ 1.85, 0.35, 2.5]),
    ([-4.55, 0.1, 0.5], [-4.40, 0.35, 2.5]),
    ([ 4.40, 0.1, 0.5], [ 4.55, 0.35, 2.5]),
    ([-1.60, 0.1, 3.5], [-1.45, 0.32, 5.5]),
    ([ 1.45, 0.1, 3.5], [ 1.60, 0.32, 5.5]),
    ([-0.45, 0.15, 12.5], [-0.30, 0.28, 14.8]),
    ([ 0.30, 0.15, 12.5], [ 0.45, 0.28, 14.8]),
]
for i, (fr, to) in enumerate(ag_positions):
    name = f"ag_{i+1}"
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    orig = [(fr[0]+to[0])/2, 0, fr[2]]
    b = bone(bi, name, orig, [eid(ei)]); bones.append(b)
    ag_bids.append(bid(bi)); ei+=1; bi+=1

# ── 10. WING TIP ACCENTS (10 bones, 10 elements) ──────────────────────────────
# At tip/point of rachis_5, fan of small accent pieces
wta_bids = []
for i in range(10):
    name = f"wta_{i+1}"
    angle = (i - 4.5) * 12.0  # fan from -54 to +54 degrees
    # Small flat tab extending from tip at Z=15
    r = 0.5 + (i % 3) * 0.15
    ax = r * math.sin(math.radians(angle))
    ay = r * math.cos(math.radians(angle)) * 0.3
    fr = [ax - 0.12, ay - 0.06, 15.0]; to = [ax + 0.12, ay + 0.06, 15.6]
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [0, 0, 15], [eid(ei)], rot=[0, angle, 0]); bones.append(b)
    wta_bids.append(bid(bi)); ei+=1; bi+=1

print(f"Elements: {len(elements)}, Bones: {len(bones)}")
assert len(elements) == 81, f"Expected 81, got {len(elements)}"
assert len(bones) == 81, f"Expected 81, got {len(bones)}"

all_bids = (rach_bids + vane_bids + bedge_bids + quill_bids + dust_bids +
            vd_bids + rd_bids + de_bids + ag_bids + wta_bids)

bone_name_map = {b["uuid"]: b["name"] for b in bones}

# ══════════════════════════════════════════════════════════════════════════════
# ANIMATIONS
# ══════════════════════════════════════════════════════════════════════════════

def build_spawn():
    """spawn once, 0.35s: tip-to-base rachis appear, vanes deploy, etc."""
    animators = {}

    # Rachis: appear tip-to-base with 0.03s stagger
    for i, buid in enumerate(rach_bids):
        delay = (4 - i) * 0.03   # tip first (rach_5 = i=4 → delay=0)
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,        0, 0, 0, "scale"),
            kf(delay,      0, 0, 0, "scale"),
            kf(delay+0.04, 1, 1, 1, "scale"),
            kf(0.35,       1, 1, 1, "scale"),
            kf(0.0,        0, 0, 1.5, "position"),
            kf(delay,      0, 0, 1.5, "position"),
            kf(delay+0.04, 0, 0, 0,   "position"),
            kf(0.35,       0, 0, 0,   "position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.17,0,0,0,"rotation"), kf(0.35,0,0,0,"rotation"),
        ]}

    # Vane panels: deploy from rachis outward with 0.04s stagger
    for i, buid in enumerate(vane_bids):
        seg = i % 5
        side = 1 if i < 5 else -1  # L then R
        rach_delay = (4 - seg) * 0.03
        vane_delay = rach_delay + 0.02 + (i % 2) * 0.04
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,             0, 0, 0, "scale"),
            kf(vane_delay,      0, 0, 0, "scale"),
            kf(vane_delay+0.05, 1, 1, 1, "scale"),
            kf(0.35,            1, 1, 1, "scale"),
            kf(0.0,             -side*2, 0, 0, "position"),
            kf(vane_delay,      -side*2, 0, 0, "position"),
            kf(vane_delay+0.05,  0,      0, 0, "position"),
            kf(0.35,             0,      0, 0, "position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.17,0,0,0,"rotation"), kf(0.35,0,0,0,"rotation"),
        ]}

    # Blade edges: snap into place
    for i, buid in enumerate(bedge_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   0,0,0,"scale"), kf(0.12, 1,1,1,"scale"), kf(0.35, 1,1,1,"scale"),
            kf(0.0,   0,0,0,"position"), kf(0.12, 0,0,0,"position"), kf(0.35, 0,0,0,"position"),
            kf(0.0,   0,0,0,"rotation"), kf(0.17, 0,0,0,"rotation"), kf(0.35, 0,0,0,"rotation"),
        ]}

    # Quill forms at base
    for i, buid in enumerate(quill_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   0,0,0,"scale"), kf(0.20, 1,1,1,"scale"), kf(0.35, 1,1,1,"scale"),
            kf(0.0,   0,-0.5,0,"position"), kf(0.20, 0,0,0,"position"), kf(0.35, 0,0,0,"position"),
            kf(0.0,   0,0,0,"rotation"), kf(0.17, 0,0,0,"rotation"), kf(0.35, 0,0,0,"rotation"),
        ]}

    # Trailing dust fans out behind
    for i, buid in enumerate(dust_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   0,0,0,"scale"), kf(0.22+i*0.008, 1,1,1,"scale"), kf(0.35, 1,1,1,"scale"),
            kf(0.0,   0,0,-2,"position"), kf(0.22, 0,0,0,"position"), kf(0.35, 0,0,0,"position"),
            kf(0.0,   0,0,0,"rotation"), kf(0.17, 0,0,0,"rotation"), kf(0.35, 0,0,0,"rotation"),
        ]}

    # Vane details, rachis details, dust extended, aura glow, wing tip accents
    for buid_list in [vd_bids, rd_bids, de_bids, ag_bids, wta_bids]:
        for i, buid in enumerate(buid_list):
            animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
                kf(0.0,  0,0,0,"scale"), kf(0.15, 1,1,1,"scale"), kf(0.35, 1,1,1,"scale"),
                kf(0.0,  0,0,0,"position"), kf(0.15, 0,0,0,"position"), kf(0.35, 0,0,0,"position"),
                kf(0.0,  0,0,0,"rotation"), kf(0.17, 0,0,0,"rotation"), kf(0.35, 0,0,0,"rotation"),
            ]}

    return animators


def build_idle():
    """idle loop, 2.0s: slow roll on long axis, vanes flutter, dust oscillates, blade edge breathes"""
    animators = {}

    # Rachis: very slow roll on Z axis (the long axis is Z, so roll = Z rotation)
    for i, buid in enumerate(rach_bids):
        phase = i * 0.3
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0, 0, 0+phase*2, "rotation"),
            kf(0.5,  0, 0, 3+phase*2, "rotation"),
            kf(1.0,  0, 0, 6+phase*2, "rotation"),
            kf(1.5,  0, 0, 3+phase*2, "rotation"),
            kf(2.0,  0, 0, 0+phase*2, "rotation"),
            kf(0.0,  0, 0, 0, "position"),
            kf(0.5,  0, 0.03*math.sin(phase), 0, "position"),
            kf(1.0,  0, 0, 0, "position"),
            kf(1.5,  0, -0.03*math.sin(phase), 0, "position"),
            kf(2.0,  0, 0, 0, "position"),
            kf(0.0,  1,1,1,"scale"), kf(1.0,1,1,1,"scale"), kf(2.0,1,1,1,"scale"),
        ]}

    # Vane panels: oscillate ±1.5 degrees (flutter)
    for i, buid in enumerate(vane_bids):
        side = 1 if i < 5 else -1
        phase = i * 0.4
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0, 0, side*1.5*math.sin(phase), "rotation"),
            kf(0.5,  0, 0, side*1.5*math.sin(phase+math.pi/2), "rotation"),
            kf(1.0,  0, 0, side*1.5*math.sin(phase+math.pi), "rotation"),
            kf(1.5,  0, 0, side*1.5*math.sin(phase+3*math.pi/2), "rotation"),
            kf(2.0,  0, 0, side*1.5*math.sin(phase+2*math.pi), "rotation"),
            kf(0.0,  0, 0.02*math.sin(phase), 0, "position"),
            kf(0.5,  0, 0.02*math.cos(phase), 0, "position"),
            kf(1.0,  0, -0.02*math.sin(phase), 0, "position"),
            kf(1.5,  0, -0.02*math.cos(phase), 0, "position"),
            kf(2.0,  0, 0.02*math.sin(phase), 0, "position"),
            kf(0.0,  1,1,1,"scale"), kf(1.0,1,1,1,"scale"), kf(2.0,1,1,1,"scale"),
        ]}

    # Blade edges: scale breathe 1.0->1.04
    for i, buid in enumerate(bedge_bids):
        phase = i * 0.5
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1.0,1.0,1.0,"scale"),
            kf(0.5,  1.04,1.04,1.0,"scale"),
            kf(1.0,  1.0,1.0,1.0,"scale"),
            kf(1.5,  0.98,0.98,1.0,"scale"),
            kf(2.0,  1.0,1.0,1.0,"scale"),
            kf(0.0,  0,0,0,"position"), kf(1.0,0,0,0,"position"), kf(2.0,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(1.0,0,0,0,"rotation"), kf(2.0,0,0,0,"rotation"),
        ]}

    # Quill: slight wobble
    for i, buid in enumerate(quill_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0, 0, 0, "rotation"),
            kf(0.5,  2, 0, 0, "rotation"),
            kf(1.0,  0, 0, 0, "rotation"),
            kf(1.5,  -2, 0, 0, "rotation"),
            kf(2.0,  0, 0, 0, "rotation"),
            kf(0.0,  0,0,0,"position"), kf(1.0,0,0,0,"position"), kf(2.0,0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"), kf(1.0,1,1,1,"scale"), kf(2.0,1,1,1,"scale"),
        ]}

    # Trailing dust: oscillates behind, slight sine drift
    for i, buid in enumerate(dust_bids):
        phase = i * 0.6
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0.05*math.sin(phase), 0.03*math.cos(phase), 0, "position"),
            kf(0.5,  0.05*math.cos(phase), 0.03*math.sin(phase), 0, "position"),
            kf(1.0,  -0.05*math.sin(phase), -0.03*math.cos(phase), 0, "position"),
            kf(1.5,  -0.05*math.cos(phase), -0.03*math.sin(phase), 0, "position"),
            kf(2.0,  0.05*math.sin(phase), 0.03*math.cos(phase), 0, "position"),
            kf(0.0,  1,1,1,"scale"), kf(0.5,0.9,0.9,0.9,"scale"), kf(1.0,1,1,1,"scale"),
            kf(1.5,  0.95,0.95,0.95,"scale"), kf(2.0,1,1,1,"scale"),
            kf(0.0,  0,0,0,"rotation"), kf(1.0,0,10,0,"rotation"), kf(2.0,0,0,0,"rotation"),
        ]}

    # Vane details: gentle flutter
    for i, buid in enumerate(vd_bids):
        phase = i * 0.25
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"position"), kf(0.5, 0, 0.02*math.sin(phase), 0, "position"),
            kf(1.0,  0,0,0,"position"), kf(1.5, 0, -0.02*math.sin(phase), 0, "position"),
            kf(2.0,  0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.67, 0, i*0.5, 0, "rotation"),
            kf(1.33, 0,0,0,"rotation"), kf(2.0, 0, 0, 0, "rotation"),
            kf(0.0,  1,1,1,"scale"), kf(1.0,1,1,1,"scale"), kf(2.0,1,1,1,"scale"),
        ]}

    # Rachis details: slow breathing
    for i, buid in enumerate(rd_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"), kf(0.5, 1.02,1.02,1.0,"scale"),
            kf(1.0,  1,1,1,"scale"), kf(1.5, 0.98,0.98,1.0,"scale"), kf(2.0,1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"), kf(1.0,0,0,0,"position"), kf(2.0,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(1.0,0,0,0,"rotation"), kf(2.0,0,0,0,"rotation"),
        ]}

    # Extended dust
    for i, buid in enumerate(de_bids):
        phase = i * 0.5 + 0.3
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0.04*math.sin(phase), 0.02*math.cos(phase), 0, "position"),
            kf(0.5,  0.04*math.cos(phase), 0.02*math.sin(phase), 0, "position"),
            kf(1.0,  -0.04*math.sin(phase), -0.02*math.cos(phase), 0, "position"),
            kf(1.5,  -0.04*math.cos(phase), -0.02*math.sin(phase), 0, "position"),
            kf(2.0,  0.04*math.sin(phase), 0.02*math.cos(phase), 0, "position"),
            kf(0.0,  1,1,1,"scale"), kf(0.67,0.85,0.85,0.85,"scale"),
            kf(1.33, 1,1,1,"scale"), kf(2.0,1,1,1,"scale"),
            kf(0.0,  0,0,0,"rotation"), kf(1.0,0,8,0,"rotation"), kf(2.0,0,0,0,"rotation"),
        ]}

    # Aura glow
    for i, buid in enumerate(ag_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"), kf(0.5, 1.05,1.05,1.0,"scale"),
            kf(1.0,  1,1,1,"scale"), kf(1.5, 0.95,0.95,1.0,"scale"), kf(2.0,1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"), kf(1.0,0,0,0,"position"), kf(2.0,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(1.0,0,0,0,"rotation"), kf(2.0,0,0,0,"rotation"),
        ]}

    # Wing tip accents: gentle fan oscillation
    for i, buid in enumerate(wta_bids):
        base_angle = (i - 4.5) * 12.0
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0, base_angle, 0, "rotation"),
            kf(0.5,  0, base_angle+3, 0, "rotation"),
            kf(1.0,  0, base_angle, 0, "rotation"),
            kf(1.5,  0, base_angle-3, 0, "rotation"),
            kf(2.0,  0, base_angle, 0, "rotation"),
            kf(0.0,  0,0,0,"position"), kf(0.5,0,0.02,0,"position"),
            kf(1.0,  0,0,0,"position"), kf(1.5,0,-0.02,0,"position"), kf(2.0,0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"), kf(1.0,1,1,1,"scale"), kf(2.0,1,1,1,"scale"),
        ]}

    return animators


def build_dissipate():
    """dissipate once, 0.3s: vanes scatter, rachis forward+scale 0, quill backward, dust continues"""
    animators = {}

    # Rachis: continue forward and scale to 0
    for i, buid in enumerate(rach_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   1,1,1,"scale"),
            kf(0.12,  1.1,1.1,1.0,"scale"),
            kf(0.25,  0,0,0,"scale"),
            kf(0.3,   0,0,0,"scale"),
            kf(0.0,   0,0,0,"position"),
            kf(0.12,  0,0,0.5+i*0.1,"position"),
            kf(0.25,  0,0,1.5+i*0.2,"position"),
            kf(0.3,   0,0,2.0+i*0.2,"position"),
            kf(0.0,   0,0,0,"rotation"),
            kf(0.15,  0,0,0,"rotation"),
            kf(0.3,   0,0,0,"rotation"),
        ]}

    # Vane panels: scatter outward
    for i, buid in enumerate(vane_bids):
        side = 1 if i < 5 else -1
        scatter_x = side * (1.0 + (i%5) * 0.4)
        scatter_y = 0.5 + (i%3) * 0.3
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.12, 1,1,1,"scale"),
            kf(0.25, 0,0,0,"scale"),
            kf(0.3,  0,0,0,"scale"),
            kf(0.0,  0, 0, 0, "position"),
            kf(0.12, scatter_x*0.4, scatter_y*0.4, 0, "position"),
            kf(0.25, scatter_x*1.0, scatter_y*1.0, 0.3, "position"),
            kf(0.3,  scatter_x*1.5, scatter_y*1.5, 0.5, "position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.15, side*20, 0, 0, "rotation"),
            kf(0.3,  side*45, 0, 0, "rotation"),
        ]}

    # Blade edges: stay briefly then vanish
    for i, buid in enumerate(bedge_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"), kf(0.1, 1,1,1,"scale"),
            kf(0.2,  0,0,0,"scale"), kf(0.3, 0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"), kf(0.1,0,0,0,"position"), kf(0.3,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.15,0,0,0,"rotation"), kf(0.3,0,0,0,"rotation"),
        ]}

    # Quill: flies backward
    for i, buid in enumerate(quill_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.15, 1,1,1,"scale"),
            kf(0.3,  0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.15, 0,0.3,-1.5,"position"),
            kf(0.3,  0,0.8,-4.0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.15, 30,0,0,"rotation"),
            kf(0.3,  60,0,0,"rotation"),
        ]}

    # Dust: continues drifting and scales to 0
    for i, buid in enumerate(dust_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.15, 0.7,0.7,0.7,"scale"),
            kf(0.3,  0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.15, 0,0,-0.5,"position"),
            kf(0.3,  0,0,-1.5,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.15,0,10,0,"rotation"), kf(0.3,0,20,0,"rotation"),
        ]}

    # All remaining: simple scale to 0
    for buid_list in [vd_bids, rd_bids, de_bids, ag_bids, wta_bids]:
        for i, buid in enumerate(buid_list):
            delay = (i % 5) * 0.02
            animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
                kf(0.0,         1,1,1,"scale"),
                kf(delay,       1,1,1,"scale"),
                kf(delay+0.08,  0,0,0,"scale"),
                kf(0.3,         0,0,0,"scale"),
                kf(0.0,  0,0,0,"position"), kf(delay,0,0,0,"position"), kf(0.3,0,0,0,"position"),
                kf(0.0,  0,0,0,"rotation"), kf(0.15,0,0,0,"rotation"), kf(0.3,0,0,0,"rotation"),
            ]}

    return animators

# ── Build model ──────────────────────────────────────────────────────────────
spawn_anim = {
    "uuid": aid(1), "name": "animation.silver_wing_blade.spawn",
    "loop": "once", "length": 0.35, "snapping": 24,
    "selected": False, "saved": False, "animators": build_spawn()
}
idle_anim = {
    "uuid": aid(2), "name": "animation.silver_wing_blade.idle",
    "loop": "loop", "length": 2.0, "snapping": 24,
    "selected": False, "saved": False, "animators": build_idle()
}
dissipate_anim = {
    "uuid": aid(3), "name": "animation.silver_wing_blade.dissipate",
    "loop": "once", "length": 0.3, "snapping": 24,
    "selected": False, "saved": False, "animators": build_dissipate()
}

outliner = [b["uuid"] for b in bones]

textures = [
    {"id":"0","name":"silver_wing_blade_tex","relative_path":"../textures/silver_wing_blade_tex.png",
     "folder":"devilsdream","namespace":"","visible":True,"mode":"bitmap","saved":False,
     "uuid":tid(1),"source":f"data:image/png;base64,{b64_0}"},
    {"id":"1","name":"silver_wing_blade_tex1","relative_path":"../textures/silver_wing_blade_tex1.png",
     "folder":"devilsdream","namespace":"","visible":True,"mode":"bitmap","saved":False,
     "uuid":tid(2),"source":f"data:image/png;base64,{b64_1}"},
]

model = {
    "meta": {"format_version":"4.10","model_format":"free","box_uv":False},
    "name": "silver_wing_blade",
    "geometry": "geometry.silver_wing_blade",
    "resolution": {"width":64,"height":64},
    "elements": elements,
    "outliner": outliner,
    "textures": textures,
    "animations": [spawn_anim, idle_anim, dissipate_anim]
}

out_path = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/silver_wing_blade.bbmodel"
with open(out_path, "w", encoding="utf-8") as f:
    json.dump(model, f, separators=(',', ':'))

size = os.path.getsize(out_path)
print(f"Written: {out_path}")
print(f"Size: {size:,} bytes ({size/1024:.1f} KB)")
pass_fail = "PASS" if size >= 300*1024 else "FAIL"
print(f"{pass_fail}: {'>=300KB' if size>=300*1024 else f'only {size//1024}KB'}")
