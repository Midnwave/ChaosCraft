"""
Build nightmare_eye_projectile.bbmodel
Eyeball-shaped projectile pointing Z+ (forward).
- 6 eyeball cubes overlapping to form an oval (~3.5x2.5x2.5 widest)
- 4 iris slabs as cross on front (Z+ face)
- 2 eyelid slabs over top/bottom
- 5 nerve tendril chains (3 segments each = 15 tendril bones)
- Orbital glow ring of 8 slabs around equator
- Plus extra detail: eyelid sub-pieces, eye-aura layers, tendril sub-segs, secondary ring
Targets 80+ elements, 55+ bones.
"""
import json, math, os

b64_0 = open("D:/CC/ChaosCraft/tools/gen_textures/m13_b64.txt").read().strip()
b64_1 = open("D:/CC/ChaosCraft/tools/gen_textures/m13_tex1_b64.txt").read().strip()

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

# ── 1. EYEBALL CORE (6 elements/bones) ────────────────────────────────────────
# Eyeball oval: widest 3.5x2.5x2.5, centered at origin. Z is forward (projectile direction).
# 4 face-forward, 2 side-profile.
eye_defs = [
    # name,           from,                to                — face-forward main spheres (Z varies, X,Y span)
    ("eye_main",      [-1.75, -1.25, -0.6], [1.75, 1.25, 0.6]),    # widest center
    ("eye_front",     [-1.40, -1.00, 0.4],  [1.40, 1.00, 1.4]),    # front lens
    ("eye_back",      [-1.40, -1.00, -1.4], [1.40, 1.00, -0.4]),   # back hemisphere
    ("eye_top",       [-1.20, 0.6, -0.7],   [1.20, 1.30, 0.7]),    # upper bulge
    # 2 side-profile (narrower X but extending Z)
    ("eye_sideL",     [-1.85, -0.80, -0.3], [-1.30, 0.80, 0.3]),   # left side
    ("eye_sideR",     [1.30, -0.80, -0.3],  [1.85, 0.80, 0.3]),    # right side
]
eye_bids = []
for name, fr, to in eye_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    cx = (fr[0]+to[0])/2; cy = (fr[1]+to[1])/2; cz = (fr[2]+to[2])/2
    b = bone(bi, name, [cx, cy, cz], [eid(ei)]); bones.append(b)
    eye_bids.append(bid(bi)); ei+=1; bi+=1

# ── 2. IRIS CROSS (4 elements/bones) ──────────────────────────────────────────
# 4 flat slabs forming a cross on the front (Z+) face. Each its own bone.
# Iris embedded slightly into front of eye, centered at (0,0, ~1.3)
iris_defs = [
    # vertical bar of cross
    ("iris_v",   [-0.30, -1.00, 1.15], [0.30, 1.00, 1.30]),
    # horizontal bar of cross
    ("iris_h",   [-1.00, -0.30, 1.15], [1.00, 0.30, 1.30]),
    # diagonal NW-SE
    ("iris_d1",  [-0.85, -0.85, 1.18], [0.85, 0.85, 1.28]),  # narrow strip rotated
    # diagonal NE-SW
    ("iris_d2",  [-0.85, -0.85, 1.18], [0.85, 0.85, 1.28]),
]
iris_bids = []
iris_rotations = [0, 0, 45, -45]  # last two are diagonals
for i, (name, fr, to) in enumerate(iris_defs):
    # Make diagonals thinner
    if i >= 2:
        # narrow strip
        fr = [-0.20, -0.95, 1.20]
        to = [0.20, 0.95, 1.27]
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    b = bone(bi, name, [0, 0, 1.22], [eid(ei)], rot=[0, 0, iris_rotations[i]]); bones.append(b)
    iris_bids.append(bid(bi)); ei+=1; bi+=1

# Pupil - dark center bone
e = elem(ei, "pupil", [-0.25, -0.25, 1.25], [0.25, 0.25, 1.35], UV1); elements.append(e)
b = bone(bi, "pupil", [0, 0, 1.30], [eid(ei)]); bones.append(b)
pupil_bid = bid(bi); ei+=1; bi+=1
iris_bids.append(pupil_bid)

# ── 3. EYELIDS (2 elements/bones — main upper/lower) ──────────────────────────
# Curved flat slabs above and below iris. Upper slightly overhanging.
eyelid_defs = [
    ("eyelid_upper", [-1.60, 0.85,  0.10], [1.60, 1.45, 1.30]),  # overhangs
    ("eyelid_lower", [-1.50, -1.40, 0.10], [1.50, -0.85, 1.20]),
]
eyelid_bids = []
for name, fr, to in eyelid_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    cx = (fr[0]+to[0])/2; cy = (fr[1]+to[1])/2; cz = (fr[2]+to[2])/2
    b = bone(bi, name, [cx, cy, cz], [eid(ei)]); bones.append(b)
    eyelid_bids.append(bid(bi)); ei+=1; bi+=1

# Eyelid sub-pieces (curved approximation — extra slabs at edges of lids)
# 6 sub pieces: 3 upper edge tabs, 3 lower edge tabs
eyelid_sub_bids = []
eyelid_sub_defs = [
    # upper outer corners (curving down at sides)
    ("eyelid_upR", [1.20, 0.55, 0.20], [1.65, 1.10, 1.20]),
    ("eyelid_upL", [-1.65, 0.55, 0.20], [-1.20, 1.10, 1.20]),
    # upper centre overhang
    ("eyelid_upC", [-0.60, 1.20, 0.30], [0.60, 1.55, 1.20]),
    # lower outer corners
    ("eyelid_loR", [1.20, -1.10, 0.20], [1.55, -0.55, 1.10]),
    ("eyelid_loL", [-1.55, -1.10, 0.20], [-1.20, -0.55, 1.10]),
    # lower centre
    ("eyelid_loC", [-0.50, -1.50, 0.30], [0.50, -1.20, 1.10]),
]
for name, fr, to in eyelid_sub_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    cx = (fr[0]+to[0])/2; cy = (fr[1]+to[1])/2; cz = (fr[2]+to[2])/2
    b = bone(bi, name, [cx, cy, cz], [eid(ei)]); bones.append(b)
    eyelid_sub_bids.append(bid(bi)); ei+=1; bi+=1

# ── 4. NERVE TENDRILS — 5 chains, 3 segments each = 15 elements/bones ─────────
# Trail behind on Z- direction. Each chain root attached to eye back.
# 5 chains positioned at varying Y/X around back of eye, segments getting shorter.
tendril_chain_origins = [
    (0.0,  0.9, -1.4),   # top
    (0.9,  0.4, -1.4),   # upper-right
    (-0.9, 0.4, -1.4),   # upper-left
    (0.6, -0.7, -1.4),   # lower-right
    (-0.6, -0.7, -1.4),  # lower-left
]
tendril_seg_lengths = [1.6, 1.2, 0.9]  # progressive decrease
tendril_seg_thickness = [0.20, 0.16, 0.12]

tendril_bids = [[] for _ in range(5)]  # 5 chains
for ci, (ox, oy, oz) in enumerate(tendril_chain_origins):
    cur_z = oz
    for si, (sl, st) in enumerate(zip(tendril_seg_lengths, tendril_seg_thickness)):
        # Each segment is a flat slab, slight curve via rotation
        name = f"tendril_c{ci+1}_s{si+1}"
        # slight wobble offset per segment
        wob_x = ox + (si - 1) * 0.15 * (1 if ci%2==0 else -1)
        wob_y = oy + (si - 1) * 0.10
        fr = [wob_x - st, wob_y - st, cur_z - sl]
        to = [wob_x + st, wob_y + st, cur_z]
        e = elem(ei, name, fr, to, UV1); elements.append(e)
        b = bone(bi, name, [wob_x, wob_y, cur_z], [eid(ei)]); bones.append(b)
        tendril_bids[ci].append(bid(bi)); ei+=1; bi+=1
        cur_z -= sl

tendril_all_bids = [b for chain in tendril_bids for b in chain]

# ── 5. ORBITAL GLOW RING (8 elements/bones) ───────────────────────────────────
# Flat ring of 8 slabs around eye equator (XZ plane at Y=0).
orbit_radius = 2.4
orbit_bids = []
for i in range(8):
    angle = i * 45.0  # degrees
    name = f"orbit_{i+1}"
    rad = math.radians(angle)
    cx = orbit_radius * math.cos(rad)
    cz = orbit_radius * math.sin(rad)
    # flat slab pointing tangentially
    sw, sh, sd = 0.45, 0.20, 0.12
    fr = [cx - sw/2, -sh/2, cz - sd/2]
    to = [cx + sw/2, sh/2, cz + sd/2]
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    b = bone(bi, name, [cx, 0, cz], [eid(ei)], rot=[0, -angle, 0]); bones.append(b)
    orbit_bids.append(bid(bi)); ei+=1; bi+=1

# ── 6. SECONDARY ORBITAL RING (8 elements/bones) ──────────────────────────────
# Larger radius, offset rotation
orbit2_bids = []
orbit2_radius = 2.85
for i in range(8):
    angle = i * 45.0 + 22.5  # offset by half-step
    name = f"orbit2_{i+1}"
    rad = math.radians(angle)
    cx = orbit2_radius * math.cos(rad)
    cz = orbit2_radius * math.sin(rad)
    sw, sh, sd = 0.30, 0.15, 0.08
    fr = [cx - sw/2, -sh/2, cz - sd/2]
    to = [cx + sw/2, sh/2, cz + sd/2]
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    b = bone(bi, name, [cx, 0, cz], [eid(ei)], rot=[0, -angle, 0]); bones.append(b)
    orbit2_bids.append(bid(bi)); ei+=1; bi+=1

# ── 7. EYE AURA LAYERS (6 elements/bones) ─────────────────────────────────────
# Thin glow slabs hugging the outside of the eyeball
aura_defs = [
    ("aura_top",    [-1.20, 1.30, -0.4], [1.20, 1.45, 0.4]),
    ("aura_bot",    [-1.20, -1.40, -0.4], [1.20, -1.25, 0.4]),
    ("aura_L",      [-1.95, -0.50, -0.3], [-1.85, 0.50, 0.3]),
    ("aura_R",      [1.85, -0.50, -0.3],  [1.95, 0.50, 0.3]),
    ("aura_front",  [-1.10, -0.60, 1.40], [1.10, 0.60, 1.50]),
    ("aura_back",   [-1.10, -0.60, -1.50], [1.10, 0.60, -1.40]),
]
aura_bids = []
for name, fr, to in aura_defs:
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    cx = (fr[0]+to[0])/2; cy = (fr[1]+to[1])/2; cz = (fr[2]+to[2])/2
    b = bone(bi, name, [cx, cy, cz], [eid(ei)]); bones.append(b)
    aura_bids.append(bid(bi)); ei+=1; bi+=1

# ── 8. TENDRIL SUB-SEGMENTS (10 elements/bones) ───────────────────────────────
# Smaller offshoots for biological detail. 2 per chain.
tendril_sub_bids = []
for ci, (ox, oy, oz) in enumerate(tendril_chain_origins):
    # 2 sub-pieces per chain — small offshoots between segments
    for si in range(2):
        name = f"tendril_sub_c{ci+1}_{si+1}"
        z_off = -1.4 - (si+1) * 0.9
        side = 0.25 * (1 if (ci+si) % 2 == 0 else -1)
        sw = 0.10
        fr = [ox + side - sw, oy - sw, z_off - 0.4]
        to = [ox + side + sw, oy + sw, z_off + 0.1]
        e = elem(ei, name, fr, to, UV1); elements.append(e)
        b = bone(bi, name, [ox + side, oy, z_off], [eid(ei)]); bones.append(b)
        tendril_sub_bids.append(bid(bi)); ei+=1; bi+=1

# ── 9. IRIS DETAIL RINGS (4 elements/bones) ───────────────────────────────────
# 4 small slabs at corners of iris cross — concentric ring detail
iris_detail_defs = [
    ("iris_dn", [-0.70, 0.60, 1.18], [0.70, 0.85, 1.28]),   # top arc
    ("iris_ds", [-0.70, -0.85, 1.18], [0.70, -0.60, 1.28]), # bottom arc
    ("iris_dl", [-0.85, -0.70, 1.18], [-0.60, 0.70, 1.28]), # left arc
    ("iris_dr", [0.60, -0.70, 1.18], [0.85, 0.70, 1.28]),   # right arc
]
iris_detail_bids = []
for name, fr, to in iris_detail_defs:
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    cx = (fr[0]+to[0])/2; cy = (fr[1]+to[1])/2
    b = bone(bi, name, [cx, cy, 1.23], [eid(ei)]); bones.append(b)
    iris_detail_bids.append(bid(bi)); ei+=1; bi+=1

# ── 10. EYELASH/RIM ACCENTS (8 elements/bones) ────────────────────────────────
# Tiny slabs along eyelid edges for rim detail
rim_bids = []
for i in range(8):
    name = f"rim_{i+1}"
    side = 1 if i < 4 else -1  # upper or lower rim
    pos = (i % 4 - 1.5) * 0.7  # x-position from -1.05 to 1.05
    if side > 0:
        # upper rim
        fr = [pos - 0.18, 0.80, 0.85]
        to = [pos + 0.18, 0.92, 1.20]
        cy = 0.86
    else:
        # lower rim
        fr = [pos - 0.18, -0.92, 0.85]
        to = [pos + 0.18, -0.80, 1.15]
        cy = -0.86
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [pos, cy, 1.0], [eid(ei)]); bones.append(b)
    rim_bids.append(bid(bi)); ei+=1; bi+=1

# ── 11. BACK NERVE BUNDLES (4 elements/bones) ─────────────────────────────────
# Small slabs at root of tendrils — looks like nerve attachment plates
nerve_root_bids = []
nerve_root_defs = [
    ("nerve_root_top", [-0.4, 0.7, -1.55], [0.4, 1.0, -1.30]),
    ("nerve_root_bot", [-0.4, -0.85, -1.55], [0.4, -0.55, -1.30]),
    ("nerve_root_L",   [-1.05, -0.3, -1.55], [-0.7, 0.3, -1.30]),
    ("nerve_root_R",   [0.7, -0.3, -1.55], [1.05, 0.3, -1.30]),
]
for name, fr, to in nerve_root_defs:
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    cx = (fr[0]+to[0])/2; cy = (fr[1]+to[1])/2; cz = (fr[2]+to[2])/2
    b = bone(bi, name, [cx, cy, cz], [eid(ei)]); bones.append(b)
    nerve_root_bids.append(bid(bi)); ei+=1; bi+=1

# ── 12. EQUATOR HIGHLIGHT (4 elements/bones) ──────────────────────────────────
# Small slabs at the equator of eyeball for highlight banding
eq_bids = []
eq_defs = [
    ("eq_F",  [-0.30, -0.05, 1.42], [0.30, 0.08, 1.48]),  # front
    ("eq_B",  [-0.30, -0.05, -1.48], [0.30, 0.08, -1.42]),
    ("eq_L",  [-1.92, -0.05, -0.10], [-1.85, 0.08, 0.10]),
    ("eq_R",  [1.85, -0.05, -0.10], [1.92, 0.08, 0.10]),
]
for name, fr, to in eq_defs:
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    cx = (fr[0]+to[0])/2; cy = (fr[1]+to[1])/2; cz = (fr[2]+to[2])/2
    b = bone(bi, name, [cx, cy, cz], [eid(ei)]); bones.append(b)
    eq_bids.append(bid(bi)); ei+=1; bi+=1

print(f"Elements: {len(elements)}, Bones: {len(bones)}")
assert len(elements) == len(bones)
assert len(elements) >= 80, f"Need >=80 got {len(elements)}"
assert len(bones) >= 55, f"Need >=55 bones got {len(bones)}"

all_bids = (eye_bids + iris_bids + eyelid_bids + eyelid_sub_bids +
            tendril_all_bids + orbit_bids + orbit2_bids + aura_bids +
            tendril_sub_bids + iris_detail_bids + rim_bids +
            nerve_root_bids + eq_bids)

bone_name_map = {b["uuid"]: b["name"] for b in bones}

# ══════════════════════════════════════════════════════════════════════════════
# ANIMATIONS
# ══════════════════════════════════════════════════════════════════════════════

def build_spawn():
    """spawn once 0.3s: eyeball materialises scale 0->1, iris opens, eyelids fly open, tendrils extend, glow expands."""
    animators = {}

    # Eyeball: scale 0 → 1 rapidly with wobble
    for i, buid in enumerate(eye_bids):
        delay = i * 0.015
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,            0, 0, 0, "scale"),
            kf(delay,          0, 0, 0, "scale"),
            kf(delay+0.10,     1.2, 1.2, 1.2, "scale"),
            kf(delay+0.18,     0.95, 0.95, 0.95, "scale"),
            kf(0.25,           1, 1, 1, "scale"),
            kf(0.30,           1, 1, 1, "scale"),
            kf(0.0,  0,0,0,"position"), kf(0.15,0,0,0,"position"), kf(0.30,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.15,0,0,0,"rotation"), kf(0.30,0,0,0,"rotation"),
        ]}

    # Iris pieces: scale from 0 (delayed start)
    for i, buid in enumerate(iris_bids):
        delay = 0.08 + i * 0.02
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,            0, 0, 0, "scale"),
            kf(delay,          0, 0, 0, "scale"),
            kf(delay+0.08,     1.1, 1.1, 1.1, "scale"),
            kf(delay+0.14,     1, 1, 1, "scale"),
            kf(0.25,           1, 1, 1, "scale"),
            kf(0.30,           1, 1, 1, "scale"),
            kf(0.0,  0,0,0,"position"), kf(0.15,0,0,0,"position"), kf(0.30,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.15,0,0,0,"rotation"), kf(0.30,0,0,0,"rotation"),
        ]}

    # Eyelids: fly open from closed (rotate from closed pos)
    for i, buid in enumerate(eyelid_bids):
        sign = 1 if i == 0 else -1  # upper rotates up, lower rotates down
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,            1, 1, 1, "scale"),
            kf(0.10,           1, 1, 1, "scale"),
            kf(0.20,           1, 1, 1, "scale"),
            kf(0.30,           1, 1, 1, "scale"),
            kf(0.0,            sign*-25, 0, 0, "rotation"),
            kf(0.05,           sign*-25, 0, 0, "rotation"),
            kf(0.18,           sign*5, 0, 0, "rotation"),
            kf(0.25,           sign*-2, 0, 0, "rotation"),
            kf(0.30,           0, 0, 0, "rotation"),
            kf(0.0,  0,sign*0.4,0,"position"), kf(0.15,0,sign*0.1,0,"position"),
            kf(0.30,0,0,0,"position"),
        ]}

    # Eyelid sub pieces: scale in
    for i, buid in enumerate(eyelid_sub_bids):
        delay = 0.05 + i * 0.01
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,           0, 0, 0, "scale"),
            kf(delay,         0, 0, 0, "scale"),
            kf(delay+0.10,    1, 1, 1, "scale"),
            kf(0.25,          1, 1, 1, "scale"),
            kf(0.30,          1, 1, 1, "scale"),
            kf(0.0,  0,0,0,"position"), kf(0.15,0,0,0,"position"), kf(0.30,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.15,0,0,0,"rotation"), kf(0.30,0,0,0,"rotation"),
        ]}

    # Tendrils: extend backward (Z-) from origin
    for ci, chain in enumerate(tendril_bids):
        for si, buid in enumerate(chain):
            delay = 0.08 + ci * 0.01 + si * 0.025
            animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
                kf(0.0,            0, 0, 0, "scale"),
                kf(delay,          0, 0, 0, "scale"),
                kf(delay+0.06,     1, 1, 1, "scale"),
                kf(0.25,           1, 1, 1, "scale"),
                kf(0.30,           1, 1, 1, "scale"),
                kf(0.0,            0, 0, 1.0, "position"),
                kf(delay,          0, 0, 1.0, "position"),
                kf(delay+0.10,     0, 0, 0,   "position"),
                kf(0.25,           0, 0, 0,   "position"),
                kf(0.30,           0, 0, 0,   "position"),
                kf(0.0,  0,0,0,"rotation"), kf(0.15,0,0,0,"rotation"), kf(0.30,0,0,0,"rotation"),
            ]}

    # Tendril sub-pieces
    for i, buid in enumerate(tendril_sub_bids):
        delay = 0.12 + i * 0.008
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,           0, 0, 0, "scale"),
            kf(delay,         0, 0, 0, "scale"),
            kf(delay+0.06,    1, 1, 1, "scale"),
            kf(0.25,          1, 1, 1, "scale"),
            kf(0.30,          1, 1, 1, "scale"),
            kf(0.0,  0,0,0,"position"), kf(0.15,0,0,0,"position"), kf(0.30,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.15,0,0,0,"rotation"), kf(0.30,0,0,0,"rotation"),
        ]}

    # Orbital glow rings: expand from 0
    for i, buid in enumerate(orbit_bids + orbit2_bids):
        delay = 0.05 + (i % 8) * 0.005
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,           0, 0, 0, "scale"),
            kf(delay,         0, 0, 0, "scale"),
            kf(delay+0.10,    1.1, 1.1, 1.1, "scale"),
            kf(delay+0.16,    1, 1, 1, "scale"),
            kf(0.25,          1, 1, 1, "scale"),
            kf(0.30,          1, 1, 1, "scale"),
            kf(0.0,  0,0,0,"position"), kf(0.15,0,0,0,"position"), kf(0.30,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.15,0,0,0,"rotation"), kf(0.30,0,0,0,"rotation"),
        ]}

    # Aura layers
    for i, buid in enumerate(aura_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0, 0, 0, "scale"),
            kf(0.08, 0, 0, 0, "scale"),
            kf(0.18, 1.05, 1.05, 1.05, "scale"),
            kf(0.25, 1, 1, 1, "scale"),
            kf(0.30, 1, 1, 1, "scale"),
            kf(0.0,  0,0,0,"position"), kf(0.15,0,0,0,"position"), kf(0.30,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.15,0,0,0,"rotation"), kf(0.30,0,0,0,"rotation"),
        ]}

    # Iris detail rings, rim accents, nerve roots, equator highlights — simple scale-in
    for buid_list in [iris_detail_bids, rim_bids, nerve_root_bids, eq_bids]:
        for i, buid in enumerate(buid_list):
            delay = 0.10 + i * 0.005
            animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
                kf(0.0,           0, 0, 0, "scale"),
                kf(delay,         0, 0, 0, "scale"),
                kf(delay+0.08,    1, 1, 1, "scale"),
                kf(0.25,          1, 1, 1, "scale"),
                kf(0.30,          1, 1, 1, "scale"),
                kf(0.0,  0,0,0,"position"), kf(0.15,0,0,0,"position"), kf(0.30,0,0,0,"position"),
                kf(0.0,  0,0,0,"rotation"), kf(0.15,0,0,0,"rotation"), kf(0.30,0,0,0,"rotation"),
            ]}

    return animators


def build_idle():
    """idle loop 1.5s: iris very slow rotate (tracking), eyelids occasionally blink, tendrils writhe with phase delay,
    orbital glow pulses, eyeball subtly oscillates."""
    animators = {}
    L = 1.5

    # Eyeball: subtle oscillation as if correcting trajectory
    for i, buid in enumerate(eye_bids):
        phase = i * 0.5
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0.05*math.sin(phase), 0.04*math.cos(phase), 0, "position"),
            kf(0.30, 0.05*math.cos(phase), -0.04*math.sin(phase), 0, "position"),
            kf(0.60, -0.05*math.sin(phase), -0.04*math.cos(phase), 0, "position"),
            kf(0.90, -0.05*math.cos(phase), 0.04*math.sin(phase), 0, "position"),
            kf(1.20, 0.05*math.sin(phase), 0.04*math.cos(phase)*0.5, 0, "position"),
            kf(1.50, 0.05*math.sin(phase), 0.04*math.cos(phase), 0, "position"),
            kf(0.0,  1, 1, 1, "scale"),
            kf(0.50, 1.01, 1.01, 1.01, "scale"),
            kf(1.00, 1, 1, 1, "scale"),
            kf(1.50, 1, 1, 1, "scale"),
            kf(0.0,  0, 0, 0, "rotation"),
            kf(0.50, 0, 1, 0, "rotation"),
            kf(1.00, 0, 0, 0, "rotation"),
            kf(1.50, 0, 0, 0, "rotation"),
        ]}

    # Iris: very slow rotation (tracking forward) — rotate slightly to track
    for i, buid in enumerate(iris_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0, 0, 0, "rotation"),
            kf(0.30, 0, 3, 0, "rotation"),
            kf(0.60, 0, 5, 0, "rotation"),
            kf(0.90, 0, 3, 0, "rotation"),
            kf(1.20, 0, -2, 0, "rotation"),
            kf(1.50, 0, 0, 0, "rotation"),
            kf(0.0,  1, 1, 1, "scale"),
            kf(0.50, 1.02, 1.02, 1.02, "scale"),
            kf(1.00, 1, 1, 1, "scale"),
            kf(1.50, 1, 1, 1, "scale"),
            kf(0.0,  0, 0, 0, "position"),
            kf(0.75, 0, 0, 0.02, "position"),
            kf(1.50, 0, 0, 0, "position"),
        ]}

    # Eyelids: occasional blink near time 1.0
    for i, buid in enumerate(eyelid_bids):
        sign = 1 if i == 0 else -1
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0, 0, 0, "rotation"),
            kf(0.30, 0, 0, 0, "rotation"),
            kf(0.90, 0, 0, 0, "rotation"),
            kf(1.00, sign*-25, 0, 0, "rotation"),  # blink closed
            kf(1.05, sign*-25, 0, 0, "rotation"),
            kf(1.15, 0, 0, 0, "rotation"),  # open
            kf(1.50, 0, 0, 0, "rotation"),
            kf(0.0,  0, 0, 0, "position"),
            kf(0.50, 0, 0, 0, "position"),
            kf(1.00, 0, sign*-0.1, 0, "position"),
            kf(1.10, 0, sign*0.05, 0, "position"),
            kf(1.50, 0, 0, 0, "position"),
            kf(0.0,  1, 1, 1, "scale"),
            kf(0.75, 1, 1, 1, "scale"),
            kf(1.50, 1, 1, 1, "scale"),
        ]}

    # Eyelid sub pieces: blink-follow
    for i, buid in enumerate(eyelid_sub_bids):
        is_upper = i < 3
        sign = 1 if is_upper else -1
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0, 0, 0, "position"),
            kf(0.50, 0, 0, 0, "position"),
            kf(1.00, 0, sign*-0.08, 0, "position"),
            kf(1.10, 0, 0, 0, "position"),
            kf(1.50, 0, 0, 0, "position"),
            kf(0.0,  0, 0, 0, "rotation"),
            kf(0.50, 0, 0, 0, "rotation"),
            kf(1.00, sign*-15, 0, 0, "rotation"),
            kf(1.10, 0, 0, 0, "rotation"),
            kf(1.50, 0, 0, 0, "rotation"),
            kf(0.0,  1, 1, 1, "scale"), kf(0.75, 1, 1, 1, "scale"), kf(1.50, 1, 1, 1, "scale"),
        ]}

    # Tendrils: writhe with phase delay down chain
    for ci, chain in enumerate(tendril_bids):
        for si, buid in enumerate(chain):
            phase = ci * 0.5 + si * 0.6  # phase delay down chain
            amp = 6 + si * 3  # tip wobbles more
            animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
                kf(0.0,  amp*math.sin(phase), amp*math.cos(phase), 0, "rotation"),
                kf(0.30, amp*math.sin(phase+0.6), amp*math.cos(phase+0.6), 0, "rotation"),
                kf(0.60, amp*math.sin(phase+1.2), amp*math.cos(phase+1.2), 0, "rotation"),
                kf(0.90, amp*math.sin(phase+1.8), amp*math.cos(phase+1.8), 0, "rotation"),
                kf(1.20, amp*math.sin(phase+2.4), amp*math.cos(phase+2.4), 0, "rotation"),
                kf(1.50, amp*math.sin(phase+3.0), amp*math.cos(phase+3.0), 0, "rotation"),
                kf(0.0,  0.05*math.sin(phase), 0.05*math.cos(phase), 0, "position"),
                kf(0.50, 0.05*math.cos(phase), -0.05*math.sin(phase), 0, "position"),
                kf(1.00, -0.05*math.sin(phase), -0.05*math.cos(phase), 0, "position"),
                kf(1.50, 0.05*math.sin(phase), 0.05*math.cos(phase), 0, "position"),
                kf(0.0,  1, 1, 1, "scale"), kf(0.75, 1, 1, 1, "scale"), kf(1.50, 1, 1, 1, "scale"),
            ]}

    # Tendril sub: small wobble
    for i, buid in enumerate(tendril_sub_bids):
        phase = i * 0.4
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0, 0, 0, "position"),
            kf(0.30, 0.06*math.sin(phase), 0.04, 0, "position"),
            kf(0.60, 0.06*math.cos(phase), 0, 0, "position"),
            kf(0.90, -0.06*math.sin(phase), -0.04, 0, "position"),
            kf(1.20, -0.06*math.cos(phase), 0, 0, "position"),
            kf(1.50, 0, 0, 0, "position"),
            kf(0.0,  0, 0, 0, "rotation"),
            kf(0.50, 0, 8*math.sin(phase), 0, "rotation"),
            kf(1.00, 0, -8*math.sin(phase), 0, "rotation"),
            kf(1.50, 0, 0, 0, "rotation"),
            kf(0.0,  1, 1, 1, "scale"), kf(0.75, 1, 1, 1, "scale"), kf(1.50, 1, 1, 1, "scale"),
        ]}

    # Orbital glow: pulses + slow orbit rotation
    for i, buid in enumerate(orbit_bids):
        base_angle = i * 45.0
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1, 1, 1, "scale"),
            kf(0.30, 1.15, 1.15, 1.0, "scale"),
            kf(0.60, 1, 1, 1, "scale"),
            kf(0.90, 0.85, 0.85, 1.0, "scale"),
            kf(1.20, 1.05, 1.05, 1.0, "scale"),
            kf(1.50, 1, 1, 1, "scale"),
            kf(0.0,  0, -base_angle, 0, "rotation"),
            kf(0.50, 0, -base_angle + 4, 0, "rotation"),
            kf(1.00, 0, -base_angle - 4, 0, "rotation"),
            kf(1.50, 0, -base_angle, 0, "rotation"),
            kf(0.0,  0, 0, 0, "position"), kf(0.75, 0, 0, 0, "position"), kf(1.50, 0, 0, 0, "position"),
        ]}

    # Secondary orbital ring: counter-pulse
    for i, buid in enumerate(orbit2_bids):
        base_angle = i * 45.0 + 22.5
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1, 1, 1, "scale"),
            kf(0.30, 0.85, 0.85, 1.0, "scale"),
            kf(0.60, 1, 1, 1, "scale"),
            kf(0.90, 1.15, 1.15, 1.0, "scale"),
            kf(1.20, 0.95, 0.95, 1.0, "scale"),
            kf(1.50, 1, 1, 1, "scale"),
            kf(0.0,  0, -base_angle, 0, "rotation"),
            kf(0.50, 0, -base_angle - 5, 0, "rotation"),
            kf(1.00, 0, -base_angle + 5, 0, "rotation"),
            kf(1.50, 0, -base_angle, 0, "rotation"),
            kf(0.0,  0, 0, 0, "position"), kf(0.75, 0, 0, 0, "position"), kf(1.50, 0, 0, 0, "position"),
        ]}

    # Aura layers: pulse opacity (use scale)
    for i, buid in enumerate(aura_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1, 1, 1, "scale"),
            kf(0.30, 1.10, 1.10, 1.10, "scale"),
            kf(0.60, 1, 1, 1, "scale"),
            kf(0.90, 0.92, 0.92, 0.92, "scale"),
            kf(1.20, 1.05, 1.05, 1.05, "scale"),
            kf(1.50, 1, 1, 1, "scale"),
            kf(0.0,  0,0,0,"position"), kf(0.75,0,0,0,"position"), kf(1.50,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.75,0,0,0,"rotation"), kf(1.50,0,0,0,"rotation"),
        ]}

    # Iris detail rings: pulse
    for i, buid in enumerate(iris_detail_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1, 1, 1, "scale"),
            kf(0.30, 1.06, 1.06, 1.0, "scale"),
            kf(0.60, 1, 1, 1, "scale"),
            kf(0.90, 0.94, 0.94, 1.0, "scale"),
            kf(1.20, 1.03, 1.03, 1.0, "scale"),
            kf(1.50, 1, 1, 1, "scale"),
            kf(0.0,  0,0,0,"position"), kf(0.75,0,0,0,"position"), kf(1.50,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.75,0,0,0,"rotation"), kf(1.50,0,0,0,"rotation"),
        ]}

    # Rim accents: blink-follow
    for i, buid in enumerate(rim_bids):
        is_upper = i < 4
        sign = 1 if is_upper else -1
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1, 1, 1, "scale"),
            kf(0.50, 1, 1, 1, "scale"),
            kf(1.00, 1, 0.05, 1, "scale"),
            kf(1.10, 1, 1, 1, "scale"),
            kf(1.50, 1, 1, 1, "scale"),
            kf(0.0,  0, 0, 0, "position"),
            kf(1.00, 0, sign*-0.05, 0, "position"),
            kf(1.10, 0, 0, 0, "position"),
            kf(1.50, 0, 0, 0, "position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.75,0,0,0,"rotation"), kf(1.50,0,0,0,"rotation"),
        ]}

    # Nerve roots: pulse
    for i, buid in enumerate(nerve_root_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1, 1, 1, "scale"),
            kf(0.30, 1.05, 1.05, 1, "scale"),
            kf(0.60, 1, 1, 1, "scale"),
            kf(0.90, 0.95, 0.95, 1, "scale"),
            kf(1.20, 1.02, 1.02, 1, "scale"),
            kf(1.50, 1, 1, 1, "scale"),
            kf(0.0,  0,0,0,"position"), kf(0.75,0,0,0,"position"), kf(1.50,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.75,0,0,0,"rotation"), kf(1.50,0,0,0,"rotation"),
        ]}

    # Equator highlights
    for i, buid in enumerate(eq_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1, 1, 1, "scale"),
            kf(0.30, 1.08, 1.08, 1, "scale"),
            kf(0.60, 1, 1, 1, "scale"),
            kf(0.90, 0.92, 0.92, 1, "scale"),
            kf(1.20, 1.04, 1.04, 1, "scale"),
            kf(1.50, 1, 1, 1, "scale"),
            kf(0.0,  0,0,0,"position"), kf(0.75,0,0,0,"position"), kf(1.50,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.75,0,0,0,"rotation"), kf(1.50,0,0,0,"rotation"),
        ]}

    return animators


def build_dissipate():
    """dissipate once 0.25s: eyeball explodes outward. Iris pieces scatter. Eyelids fly apart.
    Tendrils continue backward momentum. Orbital glow expands and snaps."""
    animators = {}

    # Eyeball: explode outward then vanish
    explode_dirs = [
        (0, 0, 0.5), (0, 0, 1.5), (0, 0, -1.5),
        (0, 1.5, 0), (-1.5, 0, 0), (1.5, 0, 0),
    ]
    for i, buid in enumerate(eye_bids):
        ed = explode_dirs[i]
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   1, 1, 1, "scale"),
            kf(0.05,  1.3, 1.3, 1.3, "scale"),
            kf(0.15,  0.5, 0.5, 0.5, "scale"),
            kf(0.25,  0, 0, 0, "scale"),
            kf(0.0,   0, 0, 0, "position"),
            kf(0.05,  ed[0]*0.3, ed[1]*0.3, ed[2]*0.3, "position"),
            kf(0.15,  ed[0]*1.0, ed[1]*1.0, ed[2]*1.0, "position"),
            kf(0.25,  ed[0]*1.8, ed[1]*1.8, ed[2]*1.8, "position"),
            kf(0.0,   0, 0, 0, "rotation"),
            kf(0.12,  i*30, i*15, 0, "rotation"),
            kf(0.25,  i*60, i*30, 0, "rotation"),
        ]}

    # Iris: scatter outward (forward direction)
    for i, buid in enumerate(iris_bids):
        ang = i * 72  # spread directions
        rad = math.radians(ang)
        sx = 1.5 * math.cos(rad)
        sy = 1.5 * math.sin(rad)
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   1, 1, 1, "scale"),
            kf(0.10,  1, 1, 1, "scale"),
            kf(0.20,  0.3, 0.3, 0.3, "scale"),
            kf(0.25,  0, 0, 0, "scale"),
            kf(0.0,   0, 0, 0, "position"),
            kf(0.10,  sx*0.5, sy*0.5, 0.5, "position"),
            kf(0.20,  sx*1.5, sy*1.5, 1.5, "position"),
            kf(0.25,  sx*2.5, sy*2.5, 2.5, "position"),
            kf(0.0,   0, 0, 0, "rotation"),
            kf(0.12,  ang, ang*0.5, 0, "rotation"),
            kf(0.25,  ang*2, ang, 0, "rotation"),
        ]}

    # Eyelids: fly apart
    for i, buid in enumerate(eyelid_bids):
        sign = 1 if i == 0 else -1
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   1, 1, 1, "scale"),
            kf(0.10,  1, 1, 1, "scale"),
            kf(0.20,  0.5, 0.5, 0.5, "scale"),
            kf(0.25,  0, 0, 0, "scale"),
            kf(0.0,   0, 0, 0, "position"),
            kf(0.10,  0, sign*0.6, 0, "position"),
            kf(0.20,  0, sign*1.5, 0.3, "position"),
            kf(0.25,  0, sign*2.0, 0.5, "position"),
            kf(0.0,   0, 0, 0, "rotation"),
            kf(0.12,  sign*60, 0, 0, "rotation"),
            kf(0.25,  sign*120, 0, 0, "rotation"),
        ]}

    # Eyelid sub: scatter
    for i, buid in enumerate(eyelid_sub_bids):
        is_upper = i < 3
        sign = 1 if is_upper else -1
        side = (i % 3 - 1) * 0.7
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   1, 1, 1, "scale"),
            kf(0.12,  1, 1, 1, "scale"),
            kf(0.20,  0.3, 0.3, 0.3, "scale"),
            kf(0.25,  0, 0, 0, "scale"),
            kf(0.0,   0, 0, 0, "position"),
            kf(0.12,  side*0.6, sign*1.0, 0.2, "position"),
            kf(0.25,  side*1.5, sign*2.5, 0.5, "position"),
            kf(0.0,   0, 0, 0, "rotation"),
            kf(0.12,  sign*45, side*30, 0, "rotation"),
            kf(0.25,  sign*90, side*60, 0, "rotation"),
        ]}

    # Tendrils: continue backward momentum
    for ci, chain in enumerate(tendril_bids):
        for si, buid in enumerate(chain):
            animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
                kf(0.0,   1, 1, 1, "scale"),
                kf(0.10,  1, 1, 1, "scale"),
                kf(0.18,  1, 1, 1.5, "scale"),
                kf(0.25,  0, 0, 0, "scale"),
                kf(0.0,   0, 0, 0, "position"),
                kf(0.10,  0, 0, -0.5 - si*0.3, "position"),
                kf(0.20,  0, 0, -1.5 - si*0.5, "position"),
                kf(0.25,  0, 0, -2.5 - si*0.6, "position"),
                kf(0.0,   0, 0, 0, "rotation"),
                kf(0.12,  ci*15, si*10, 0, "rotation"),
                kf(0.25,  ci*30, si*20, 0, "rotation"),
            ]}

    # Tendril sub
    for i, buid in enumerate(tendril_sub_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   1, 1, 1, "scale"),
            kf(0.10,  1, 1, 1, "scale"),
            kf(0.20,  0.4, 0.4, 0.4, "scale"),
            kf(0.25,  0, 0, 0, "scale"),
            kf(0.0,   0, 0, 0, "position"),
            kf(0.12,  0, 0, -1.0, "position"),
            kf(0.25,  0, 0, -2.5, "position"),
            kf(0.0,   0, 0, 0, "rotation"),
            kf(0.12,  i*20, 0, 0, "rotation"),
            kf(0.25,  i*40, 0, 0, "rotation"),
        ]}

    # Orbital ring: expand and snap
    for i, buid in enumerate(orbit_bids + orbit2_bids):
        base_ang = (i % 8) * 45.0
        if i >= 8: base_ang += 22.5
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   1, 1, 1, "scale"),
            kf(0.08,  1.5, 1.5, 1.5, "scale"),
            kf(0.16,  2.5, 2.5, 0.3, "scale"),
            kf(0.22,  3.5, 3.5, 0.0, "scale"),
            kf(0.25,  0, 0, 0, "scale"),
            kf(0.0,   0, 0, 0, "position"),
            kf(0.12,  0, 0, 0, "position"),
            kf(0.25,  0, 0, 0, "position"),
            kf(0.0,   0, -base_ang, 0, "rotation"),
            kf(0.12,  0, -base_ang+30, 0, "rotation"),
            kf(0.25,  0, -base_ang+60, 0, "rotation"),
        ]}

    # Aura layers
    for i, buid in enumerate(aura_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   1, 1, 1, "scale"),
            kf(0.08,  1.4, 1.4, 1.4, "scale"),
            kf(0.16,  1.8, 1.8, 1.8, "scale"),
            kf(0.22,  2.2, 2.2, 2.2, "scale"),
            kf(0.25,  0, 0, 0, "scale"),
            kf(0.0,   0,0,0,"position"), kf(0.12,0,0,0,"position"), kf(0.25,0,0,0,"position"),
            kf(0.0,   0,0,0,"rotation"), kf(0.12,0,0,0,"rotation"), kf(0.25,0,0,0,"rotation"),
        ]}

    # Iris detail, rim, nerve roots, equator: fade out
    for buid_list in [iris_detail_bids, rim_bids, nerve_root_bids, eq_bids]:
        for i, buid in enumerate(buid_list):
            delay = (i % 5) * 0.015
            animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
                kf(0.0,         1, 1, 1, "scale"),
                kf(delay,       1, 1, 1, "scale"),
                kf(delay+0.06,  0.5, 0.5, 0.5, "scale"),
                kf(0.20,        0.1, 0.1, 0.1, "scale"),
                kf(0.25,        0, 0, 0, "scale"),
                kf(0.0,  0,0,0,"position"), kf(0.12,0,0,0,"position"), kf(0.25,0,0,0,"position"),
                kf(0.0,  0,0,0,"rotation"), kf(0.12,0,0,0,"rotation"), kf(0.25,0,0,0,"rotation"),
            ]}

    return animators

# ── Build model ──────────────────────────────────────────────────────────────
spawn_anim = {
    "uuid": aid(1), "name": "animation.nightmare_eye_projectile.spawn",
    "loop": "once", "length": 0.30, "snapping": 24,
    "selected": False, "saved": False, "animators": build_spawn()
}
idle_anim = {
    "uuid": aid(2), "name": "animation.nightmare_eye_projectile.idle",
    "loop": "loop", "length": 1.50, "snapping": 24,
    "selected": False, "saved": False, "animators": build_idle()
}
dissipate_anim = {
    "uuid": aid(3), "name": "animation.nightmare_eye_projectile.dissipate",
    "loop": "once", "length": 0.25, "snapping": 24,
    "selected": False, "saved": False, "animators": build_dissipate()
}

outliner = [b["uuid"] for b in bones]

textures = [
    {"id":"0","name":"nightmare_eye_projectile_tex","relative_path":"../textures/nightmare_eye_projectile_tex.png",
     "folder":"devilsdream","namespace":"","visible":True,"mode":"bitmap","saved":False,
     "uuid":tid(1),"source":f"data:image/png;base64,{b64_0}"},
    {"id":"1","name":"nightmare_eye_projectile_tex1","relative_path":"../textures/nightmare_eye_projectile_tex1.png",
     "folder":"devilsdream","namespace":"","visible":True,"mode":"bitmap","saved":False,
     "uuid":tid(2),"source":f"data:image/png;base64,{b64_1}"},
]

model = {
    "meta": {"format_version":"4.10","model_format":"free","box_uv":False},
    "name": "nightmare_eye_projectile",
    "geometry": "geometry.nightmare_eye_projectile",
    "resolution": {"width":64,"height":64},
    "elements": elements,
    "outliner": outliner,
    "textures": textures,
    "animations": [spawn_anim, idle_anim, dissipate_anim]
}

out_path = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_eye_projectile.bbmodel"
with open(out_path, "w", encoding="utf-8") as f:
    json.dump(model, f, separators=(',', ':'))

size = os.path.getsize(out_path)
print(f"Written: {out_path}")
print(f"Elements: {len(elements)} / Bones: {len(bones)}")
print(f"Animations: 3 (spawn 0.30s once, idle 1.50s loop, dissipate 0.25s once)")
print(f"Size: {size:,} bytes ({size/1024:.1f} KB)")
pass_fail = "PASS" if size >= 300*1024 else "FAIL"
print(f"{pass_fail}: {'>=300KB' if size>=300*1024 else f'only {size//1024}KB'}")
