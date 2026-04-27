"""
Build nightmare_clock.bbmodel - 80+ elements, 55+ bones, 3 animations.
STANDING VERTICAL CLOCK structure:
- 12 face slabs in a CIRCLE at Z=0 plane facing +Z, centered at (0, 8, 0), radius 4
- 12 hour markers OUTSIDE face perimeter (radius 4.3)
- Hour hand (3 cubes tapered) rotating around (0,8,0) on Z axis
- Minute hand (2 cubes longer/thinner) on Z rotation
- Second hand (1 thin cube) on Z rotation
- Rectangular outer frame (4 border slabs)
- Pendulum (rod + weight) hanging below at Y=0-4
- Frame corners, hour marker accents, hand pivot, pendulum chain links
"""
import json, math, os

b64_0 = open("D:/CC/ChaosCraft/tools/gen_textures/m21_b64.txt").read().strip()
b64_1 = open("D:/CC/ChaosCraft/tools/gen_textures/m21_tex1_b64.txt").read().strip()

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

# Clock center: (0, 8, 0) — face is in X-Y plane, facing +Z
CX, CY, CZ = 0.0, 8.0, 0.0
FACE_R = 4.0          # face radius
MARKER_R = 4.3        # outer marker radius
ACCENT_R = 3.55       # inner accent ring

# ── 1. CLOCK FACE SLABS (12 bones, 12 elements) ─────────────────────────────
# 12 wedge-like flat slabs arranged in a circle, each spanning 30° of the face
face_bids = []
for i in range(12):
    angle = (i / 12.0) * 2 * math.pi - math.pi / 2  # -90° starts at top (12 o'clock)
    # Slab placed at radius 2.0 from center (mid-radius), each its own bone
    rx = math.cos(angle) * 2.0
    ry = math.sin(angle) * 2.0
    name = f"face_{i+1}"
    # Slab dimensions: 1.6 wide, 1.6 tall, 0.25 deep — centered at (CX+rx, CY+ry, CZ)
    fr = [CX + rx - 0.8, CY + ry - 0.8, CZ - 0.125]
    to = [CX + rx + 0.8, CY + ry + 0.8, CZ + 0.125]
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [CX + rx, CY + ry, CZ], [eid(ei)], rot=[0, 0, math.degrees(angle)+90])
    bones.append(b)
    face_bids.append(bid(bi)); ei+=1; bi+=1

# ── 2. HOUR MARKERS (12 bones, 12 elements) ─────────────────────────────────
# 12 small rectangular slabs at each hour position, OUTSIDE face circle
hm_bids = []
for i in range(12):
    angle = (i / 12.0) * 2 * math.pi - math.pi / 2
    rx = math.cos(angle) * MARKER_R
    ry = math.sin(angle) * MARKER_R
    name = f"hourmark_{i+1}"
    # 12, 3, 6, 9 are larger
    is_cardinal = (i % 3 == 0)
    w = 0.55 if is_cardinal else 0.35
    h = 0.55 if is_cardinal else 0.35
    fr = [CX + rx - w/2, CY + ry - h/2, CZ - 0.10]
    to = [CX + rx + w/2, CY + ry + h/2, CZ + 0.20]
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [CX + rx, CY + ry, CZ], [eid(ei)])
    bones.append(b)
    hm_bids.append(bid(bi)); ei+=1; bi+=1

# ── 3. HOUR MARKER ACCENTS (12 bones, 12 elements) ─────────────────────────
# Small inner accent piece behind each marker (depth detail)
hma_bids = []
for i in range(12):
    angle = (i / 12.0) * 2 * math.pi - math.pi / 2
    rx = math.cos(angle) * (MARKER_R - 0.5)
    ry = math.sin(angle) * (MARKER_R - 0.5)
    name = f"hourmark_acc_{i+1}"
    fr = [CX + rx - 0.18, CY + ry - 0.18, CZ - 0.05]
    to = [CX + rx + 0.18, CY + ry + 0.18, CZ + 0.10]
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [CX + rx, CY + ry, CZ], [eid(ei)])
    bones.append(b)
    hma_bids.append(bid(bi)); ei+=1; bi+=1

# ── 4. HOUR HAND (3 bones, 3 elements) ──────────────────────────────────────
# Tapered: wide base, narrow tip. Pivots at (CX, CY, CZ) on Z axis
# Base segment, mid segment, tip segment — all stem from pivot pointing +Y by default
hh_bids = []
hh_segs = [
    # name, fr_local (rel to pivot), to_local
    ("hourhand_base", [-0.30, 0.0, 0.30], [0.30, 0.7, 0.55]),
    ("hourhand_mid",  [-0.22, 0.7, 0.30], [0.22, 1.5, 0.55]),
    ("hourhand_tip",  [-0.12, 1.5, 0.30], [0.12, 2.4, 0.55]),
]
for name, fr_loc, to_loc in hh_segs:
    fr = [CX + fr_loc[0], CY + fr_loc[1], CZ + fr_loc[2]]
    to = [CX + to_loc[0], CY + to_loc[1], CZ + to_loc[2]]
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    # Pivot is clock center
    b = bone(bi, name, [CX, CY, CZ], [eid(ei)])
    bones.append(b)
    hh_bids.append(bid(bi)); ei+=1; bi+=1

# ── 5. MINUTE HAND (2 bones, 2 elements) ────────────────────────────────────
# Longer, thinner than hour hand. Pivots at clock center
mh_bids = []
mh_segs = [
    ("minutehand_base", [-0.18, 0.0, 0.55], [0.18, 1.6, 0.75]),
    ("minutehand_tip",  [-0.10, 1.6, 0.55], [0.10, 3.4, 0.75]),
]
for name, fr_loc, to_loc in mh_segs:
    fr = [CX + fr_loc[0], CY + fr_loc[1], CZ + fr_loc[2]]
    to = [CX + to_loc[0], CY + to_loc[1], CZ + to_loc[2]]
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    b = bone(bi, name, [CX, CY, CZ], [eid(ei)])
    bones.append(b)
    mh_bids.append(bid(bi)); ei+=1; bi+=1

# ── 6. SECOND HAND (1 bone, 1 element) ──────────────────────────────────────
sh_bids = []
fr = [CX - 0.06, CY - 0.4, CZ + 0.75]
to = [CX + 0.06, CY + 3.7, CZ + 0.92]
e = elem(ei, "secondhand", fr, to, UV1); elements.append(e)
b = bone(bi, "secondhand", [CX, CY, CZ], [eid(ei)])
bones.append(b)
sh_bids.append(bid(bi)); ei+=1; bi+=1

# ── 7. HAND PIVOT DECORATION (1 bone, 1 element) ────────────────────────────
# Center pivot decoration where hands meet — a small circular cap
piv_bids = []
fr = [CX - 0.45, CY - 0.45, CZ + 0.92]
to = [CX + 0.45, CY + 0.45, CZ + 1.10]
e = elem(ei, "pivot_cap", fr, to, UV1); elements.append(e)
b = bone(bi, "pivot_cap", [CX, CY, CZ], [eid(ei)])
bones.append(b)
piv_bids.append(bid(bi)); ei+=1; bi+=1

# ── 8. CLOCK FRAME (4 bones, 4 elements) ────────────────────────────────────
# Rectangular outer frame around clock face, square spanning ±5 from center
FRAME_HALF = 5.0
FRAME_THICK = 0.5
frame_bids = []
frame_defs = [
    # name, fr, to
    ("frame_top",    [CX - FRAME_HALF - FRAME_THICK, CY + FRAME_HALF, CZ - 0.30],
                     [CX + FRAME_HALF + FRAME_THICK, CY + FRAME_HALF + FRAME_THICK, CZ + 0.30]),
    ("frame_bot",    [CX - FRAME_HALF - FRAME_THICK, CY - FRAME_HALF - FRAME_THICK, CZ - 0.30],
                     [CX + FRAME_HALF + FRAME_THICK, CY - FRAME_HALF, CZ + 0.30]),
    ("frame_left",   [CX - FRAME_HALF - FRAME_THICK, CY - FRAME_HALF, CZ - 0.30],
                     [CX - FRAME_HALF, CY + FRAME_HALF, CZ + 0.30]),
    ("frame_right",  [CX + FRAME_HALF, CY - FRAME_HALF, CZ - 0.30],
                     [CX + FRAME_HALF + FRAME_THICK, CY + FRAME_HALF, CZ + 0.30]),
]
for name, fr, to in frame_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    cx_b = (fr[0]+to[0])/2; cy_b = (fr[1]+to[1])/2
    b = bone(bi, name, [cx_b, cy_b, CZ], [eid(ei)])
    bones.append(b)
    frame_bids.append(bid(bi)); ei+=1; bi+=1

# ── 9. FRAME CORNER PIECES (4 bones, 4 elements) ───────────────────────────
# Decorative corner pieces for the frame
fc_bids = []
fc_defs = [
    ("frame_corner_TL", [CX - FRAME_HALF - FRAME_THICK - 0.4, CY + FRAME_HALF, CZ - 0.40],
                        [CX - FRAME_HALF, CY + FRAME_HALF + FRAME_THICK + 0.4, CZ + 0.40]),
    ("frame_corner_TR", [CX + FRAME_HALF, CY + FRAME_HALF, CZ - 0.40],
                        [CX + FRAME_HALF + FRAME_THICK + 0.4, CY + FRAME_HALF + FRAME_THICK + 0.4, CZ + 0.40]),
    ("frame_corner_BL", [CX - FRAME_HALF - FRAME_THICK - 0.4, CY - FRAME_HALF - FRAME_THICK - 0.4, CZ - 0.40],
                        [CX - FRAME_HALF, CY - FRAME_HALF, CZ + 0.40]),
    ("frame_corner_BR", [CX + FRAME_HALF, CY - FRAME_HALF - FRAME_THICK - 0.4, CZ - 0.40],
                        [CX + FRAME_HALF + FRAME_THICK + 0.4, CY - FRAME_HALF, CZ + 0.40]),
]
for name, fr, to in fc_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    cx_b = (fr[0]+to[0])/2; cy_b = (fr[1]+to[1])/2
    b = bone(bi, name, [cx_b, cy_b, CZ], [eid(ei)])
    bones.append(b)
    fc_bids.append(bid(bi)); ei+=1; bi+=1

# ── 10. PENDULUM ROD (1 bone, 1 element) ────────────────────────────────────
# Pendulum hanging below clock at Y=0-4. Pivot at top of rod (frame bottom)
PEND_PIVOT_Y = CY - FRAME_HALF - FRAME_THICK  # = 8 - 5 - 0.5 = 2.5
pend_rod_bids = []
fr = [CX - 0.12, PEND_PIVOT_Y - 2.5, CZ - 0.12]
to = [CX + 0.12, PEND_PIVOT_Y, CZ + 0.12]
e = elem(ei, "pendulum_rod", fr, to, UV1); elements.append(e)
b = bone(bi, "pendulum_rod", [CX, PEND_PIVOT_Y, CZ], [eid(ei)])
bones.append(b)
pend_rod_bids.append(bid(bi)); ei+=1; bi+=1

# ── 11. PENDULUM WEIGHT (1 bone, 1 element) ─────────────────────────────────
# Weight at bottom of pendulum
pend_w_bids = []
fr = [CX - 0.7, PEND_PIVOT_Y - 3.3, CZ - 0.5]
to = [CX + 0.7, PEND_PIVOT_Y - 2.4, CZ + 0.5]
e = elem(ei, "pendulum_weight", fr, to, UV1); elements.append(e)
b = bone(bi, "pendulum_weight", [CX, PEND_PIVOT_Y, CZ], [eid(ei)])
bones.append(b)
pend_w_bids.append(bid(bi)); ei+=1; bi+=1

# ── 12. PENDULUM CHAIN LINKS (4 bones, 4 elements) ─────────────────────────
# Decorative chain links between pendulum rod and clock body
pcl_bids = []
for i in range(4):
    name = f"pend_link_{i+1}"
    y_pos = PEND_PIVOT_Y - 0.4 - i * 0.55
    fr = [CX - 0.20, y_pos - 0.20, CZ - 0.20]
    to = [CX + 0.20, y_pos + 0.20, CZ + 0.20]
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    b = bone(bi, name, [CX, y_pos, CZ], [eid(ei)])
    bones.append(b)
    pcl_bids.append(bid(bi)); ei+=1; bi+=1

# ── 13. INNER ACCENT RING (12 bones, 12 elements) ──────────────────────────
# Small circular accent dots at radius 3.55 between hour positions
iar_bids = []
for i in range(12):
    angle = (i / 12.0) * 2 * math.pi - math.pi / 2 + math.pi / 12  # offset half
    rx = math.cos(angle) * ACCENT_R
    ry = math.sin(angle) * ACCENT_R
    name = f"inner_acc_{i+1}"
    fr = [CX + rx - 0.14, CY + ry - 0.14, CZ + 0.10]
    to = [CX + rx + 0.14, CY + ry + 0.14, CZ + 0.25]
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [CX + rx, CY + ry, CZ], [eid(ei)])
    bones.append(b)
    iar_bids.append(bid(bi)); ei+=1; bi+=1

# ── 14. FRAME EDGE STUDS (8 bones, 8 elements) ─────────────────────────────
# Small decorative studs along frame edges (2 per side)
fes_bids = []
fes_defs = []
for i in range(2):
    t = -2.5 + i * 5.0
    fes_defs.append((f"stud_top_{i+1}",   [CX + t - 0.20, CY + FRAME_HALF + FRAME_THICK, CZ - 0.40],
                                           [CX + t + 0.20, CY + FRAME_HALF + FRAME_THICK + 0.30, CZ + 0.40]))
    fes_defs.append((f"stud_bot_{i+1}",   [CX + t - 0.20, CY - FRAME_HALF - FRAME_THICK - 0.30, CZ - 0.40],
                                           [CX + t + 0.20, CY - FRAME_HALF - FRAME_THICK, CZ + 0.40]))
    fes_defs.append((f"stud_left_{i+1}",  [CX - FRAME_HALF - FRAME_THICK - 0.30, CY + t - 0.20, CZ - 0.40],
                                           [CX - FRAME_HALF - FRAME_THICK, CY + t + 0.20, CZ + 0.40]))
    fes_defs.append((f"stud_right_{i+1}", [CX + FRAME_HALF + FRAME_THICK, CY + t - 0.20, CZ - 0.40],
                                           [CX + FRAME_HALF + FRAME_THICK + 0.30, CY + t + 0.20, CZ + 0.40]))
for name, fr, to in fes_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    cx_b = (fr[0]+to[0])/2; cy_b = (fr[1]+to[1])/2
    b = bone(bi, name, [cx_b, cy_b, CZ], [eid(ei)])
    bones.append(b)
    fes_bids.append(bid(bi)); ei+=1; bi+=1

# ── 15. KEYHOLE / WINDING NOTCH (3 bones, 3 elements) ──────────────────────
# Decorative keyhole below pivot — three small slabs in cross arrangement
key_bids = []
key_defs = [
    ("keyhole_top",   [CX - 0.18, CY - 1.6, CZ + 0.10], [CX + 0.18, CY - 1.2, CZ + 0.30]),
    ("keyhole_left",  [CX - 0.5,  CY - 1.45, CZ + 0.10], [CX - 0.18, CY - 1.25, CZ + 0.30]),
    ("keyhole_right", [CX + 0.18, CY - 1.45, CZ + 0.10], [CX + 0.5,  CY - 1.25, CZ + 0.30]),
]
for name, fr, to in key_defs:
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    cx_b = (fr[0]+to[0])/2; cy_b = (fr[1]+to[1])/2
    b = bone(bi, name, [cx_b, cy_b, CZ], [eid(ei)])
    bones.append(b)
    key_bids.append(bid(bi)); ei+=1; bi+=1

# Assert counts
print(f"Elements: {len(elements)}, Bones: {len(bones)}")
assert len(elements) == len(bones)
assert len(elements) >= 80, f"Need 80+ elements, got {len(elements)}"

all_bids = (face_bids + hm_bids + hma_bids + hh_bids + mh_bids + sh_bids + piv_bids
            + frame_bids + fc_bids + pend_rod_bids + pend_w_bids + pcl_bids
            + iar_bids + fes_bids + key_bids)
assert len(all_bids) == len(bones), f"Bid total {len(all_bids)} != bones {len(bones)}"

bone_name_map = {b["uuid"]: b["name"] for b in bones}

# ══════════════════════════════════════════════════════════════════════════════
# ANIMATIONS
# ══════════════════════════════════════════════════════════════════════════════

def build_spawn():
    """spawn once, 1.2s:
    - frame materialises at 0.2s
    - face slabs phase in clockwise at 0.3s, 0.06s stagger
    - hour markers drop from above at 0.6s one by one
    - clock hands materialise and immediately begin spinning
    """
    animators = {}
    L = 1.2

    # FRAME (4 bides) at 0.2s
    for i, buid in enumerate(frame_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(0.18, 0,0,0,"scale"),
            kf(0.30, 1,1,1,"scale"),
            kf(0.6,  1,1,1,"scale"),
            kf(0.9,  1,1,1,"scale"),
            kf(L,    1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.18, 0,0,0,"position"),
            kf(0.30, 0,0,0,"position"),
            kf(0.6,  0,0,0,"position"),
            kf(0.9,  0,0,0,"position"),
            kf(L,    0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.3,  0,0,0,"rotation"),
            kf(0.6,  0,0,0,"rotation"),
            kf(L,    0,0,0,"rotation"),
        ]}

    # FRAME CORNERS — slightly later
    for i, buid in enumerate(fc_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(0.22, 0,0,0,"scale"),
            kf(0.34, 1,1,1,"scale"),
            kf(0.6,  1,1,1,"scale"),
            kf(0.9,  1,1,1,"scale"),
            kf(L,    1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"), kf(0.3,0,0,0,"position"), kf(0.6,0,0,0,"position"),
            kf(0.9,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.3,0,0,0,"rotation"), kf(0.6,0,0,0,"rotation"),
            kf(0.9,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # FRAME STUDS — even later
    for i, buid in enumerate(fes_bids):
        d = 0.30 + (i % 4) * 0.02
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(d,    0,0,0,"scale"),
            kf(d+0.08, 1,1,1,"scale"),
            kf(0.6,  1,1,1,"scale"),
            kf(0.9,  1,1,1,"scale"),
            kf(L,    1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"), kf(0.3,0,0,0,"position"), kf(0.6,0,0,0,"position"),
            kf(0.9,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.3,0,0,0,"rotation"), kf(0.6,0,0,0,"rotation"),
            kf(0.9,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # FACE SLABS phase in clockwise at 0.3s with 0.06s stagger
    for i, buid in enumerate(face_bids):
        d = 0.30 + i * 0.025  # stagger slightly tighter to fit 12 in window
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(d,    0,0,0,"scale"),
            kf(d+0.06, 1,1,1,"scale"),
            kf(0.85, 1,1,1,"scale"),
            kf(L,    1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(d,    0,0,0,"position"),
            kf(d+0.06, 0,0,0,"position"),
            kf(0.85, 0,0,0,"position"),
            kf(L,    0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.3,0,0,0,"rotation"), kf(0.6,0,0,0,"rotation"),
            kf(0.9,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # INNER ACCENT RING — phase in with face
    for i, buid in enumerate(iar_bids):
        d = 0.40 + i * 0.025
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(d,    0,0,0,"scale"),
            kf(d+0.05, 1,1,1,"scale"),
            kf(0.85, 1,1,1,"scale"),
            kf(L,    1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"), kf(0.4,0,0,0,"position"), kf(0.6,0,0,0,"position"),
            kf(0.9,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.4,0,0,0,"rotation"), kf(0.7,0,0,0,"rotation"),
            kf(0.9,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # HOUR MARKERS drop from above at 0.6s, one by one
    for i, buid in enumerate(hm_bids):
        d = 0.60 + i * 0.03
        # drop from +3 in Y, scale 1
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(d,    0,0,0,"scale"),
            kf(d+0.04, 1,1,1,"scale"),
            kf(0.95, 1,1,1,"scale"),
            kf(L,    1,1,1,"scale"),
            kf(0.0,  0,3.5,0,"position"),
            kf(d,    0,3.5,0,"position"),
            kf(d+0.10, 0,-0.3,0,"position"),
            kf(d+0.18, 0,0,0,"position"),
            kf(0.95, 0,0,0,"position"),
            kf(L,    0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.4,0,0,0,"rotation"), kf(0.7,0,0,0,"rotation"),
            kf(0.9,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # HOUR MARKER ACCENTS — appear with markers
    for i, buid in enumerate(hma_bids):
        d = 0.65 + i * 0.03
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(d,    0,0,0,"scale"),
            kf(d+0.04, 1,1,1,"scale"),
            kf(0.95, 1,1,1,"scale"),
            kf(L,    1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"), kf(0.5,0,0,0,"position"), kf(0.8,0,0,0,"position"),
            kf(0.95,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.5,0,0,0,"rotation"), kf(0.8,0,0,0,"rotation"),
            kf(0.95,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # HOUR HAND — materialise, then spin (a partial rotation during spawn)
    for i, buid in enumerate(hh_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(0.85, 0,0,0,"scale"),
            kf(0.95, 1,1,1,"scale"),
            kf(1.0,  1,1,1,"scale"),
            kf(1.1,  1,1,1,"scale"),
            kf(L,    1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"), kf(0.6,0,0,0,"position"), kf(0.9,0,0,0,"position"),
            kf(1.0,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.85, 0,0,0,"rotation"),
            kf(0.95, 0,0,30,"rotation"),
            kf(1.05, 0,0,80,"rotation"),
            kf(1.10, 0,0,140,"rotation"),
            kf(L,    0,0,200,"rotation"),
        ]}

    # MINUTE HAND — same
    for i, buid in enumerate(mh_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(0.85, 0,0,0,"scale"),
            kf(0.93, 1,1,1,"scale"),
            kf(1.0,  1,1,1,"scale"),
            kf(1.1,  1,1,1,"scale"),
            kf(L,    1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"), kf(0.6,0,0,0,"position"), kf(0.9,0,0,0,"position"),
            kf(1.0,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.85, 0,0,0,"rotation"),
            kf(0.93, 0,0,-50,"rotation"),
            kf(1.0,  0,0,-130,"rotation"),
            kf(1.1,  0,0,-260,"rotation"),
            kf(L,    0,0,-380,"rotation"),
        ]}

    # SECOND HAND — fastest, immediately spinning
    for i, buid in enumerate(sh_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(0.85, 0,0,0,"scale"),
            kf(0.91, 1,1,1,"scale"),
            kf(1.0,  1,1,1,"scale"),
            kf(1.1,  1,1,1,"scale"),
            kf(L,    1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"), kf(0.6,0,0,0,"position"), kf(0.9,0,0,0,"position"),
            kf(1.0,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.85, 0,0,0,"rotation"),
            kf(0.91, 0,0,-90,"rotation"),
            kf(1.0,  0,0,-300,"rotation"),
            kf(1.1,  0,0,-600,"rotation"),
            kf(L,    0,0,-1000,"rotation"),
        ]}

    # PIVOT CAP — appears at 0.85s
    for buid in piv_bids:
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(0.80, 0,0,0,"scale"),
            kf(0.92, 1.2,1.2,1.2,"scale"),
            kf(1.0,  1,1,1,"scale"),
            kf(1.1,  1,1,1,"scale"),
            kf(L,    1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"), kf(0.6,0,0,0,"position"), kf(0.9,0,0,0,"position"),
            kf(1.0,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.6,0,0,0,"rotation"), kf(0.9,0,0,0,"rotation"),
            kf(1.0,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # PENDULUM ROD — appears at 0.45s, drops in
    for buid in pend_rod_bids:
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(0.44, 0,0,0,"scale"),
            kf(0.55, 1,1,1,"scale"),
            kf(0.85, 1,1,1,"scale"),
            kf(1.0,  1,1,1,"scale"),
            kf(L,    1,1,1,"scale"),
            kf(0.0,  0,1.5,0,"position"),
            kf(0.44, 0,1.5,0,"position"),
            kf(0.55, 0,0.2,0,"position"),
            kf(0.65, 0,0,0,"position"),
            kf(0.85, 0,0,0,"position"),
            kf(L,    0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.5,0,0,0,"rotation"), kf(0.8,0,0,3,"rotation"),
            kf(1.0,0,0,-3,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # PENDULUM WEIGHT — drops with rod
    for buid in pend_w_bids:
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(0.50, 0,0,0,"scale"),
            kf(0.62, 1.1,1.1,1.1,"scale"),
            kf(0.7,  1,1,1,"scale"),
            kf(1.0,  1,1,1,"scale"),
            kf(L,    1,1,1,"scale"),
            kf(0.0,  0,2.0,0,"position"),
            kf(0.50, 0,2.0,0,"position"),
            kf(0.62, 0,0.3,0,"position"),
            kf(0.72, 0,0,0,"position"),
            kf(1.0,  0,0,0,"position"),
            kf(L,    0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.5,0,0,0,"rotation"), kf(0.8,0,0,3,"rotation"),
            kf(1.0,0,0,-3,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # PENDULUM CHAIN LINKS — appear with rod
    for i, buid in enumerate(pcl_bids):
        d = 0.46 + i * 0.04
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(d,    0,0,0,"scale"),
            kf(d+0.08, 1,1,1,"scale"),
            kf(0.85, 1,1,1,"scale"),
            kf(1.0,  1,1,1,"scale"),
            kf(L,    1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"), kf(0.5,0,0,0,"position"), kf(0.8,0,0,0,"position"),
            kf(1.0,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.5,0,0,0,"rotation"), kf(0.8,0,0,0,"rotation"),
            kf(1.0,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # KEYHOLE — appears with face slabs
    for i, buid in enumerate(key_bids):
        d = 0.55 + i * 0.04
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(d,    0,0,0,"scale"),
            kf(d+0.06, 1,1,1,"scale"),
            kf(0.85, 1,1,1,"scale"),
            kf(1.0,  1,1,1,"scale"),
            kf(L,    1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"), kf(0.5,0,0,0,"position"), kf(0.8,0,0,0,"position"),
            kf(1.0,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.5,0,0,0,"rotation"), kf(0.8,0,0,0,"rotation"),
            kf(1.0,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    return animators


def build_idle():
    """idle loop, 6.0s:
    - second hand spins COUNTER-CLOCKWISE rapidly (multiple full rotations)
    - minute hand counter-clockwise but slower
    - hour hand barely moves but in WRONG direction
    - pendulum swings back and forth on X rotation, 3s period
    - hour markers pulse emissive on individual periods (scale pulse)
    - clock face slabs subtly pulse
    """
    animators = {}
    L = 6.0

    # FACE SLABS subtle pulse (scale 1.0 -> 1.02 -> 1.0)
    for i, buid in enumerate(face_bids):
        phase = (i / 12.0) * 2 * math.pi
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1.0,1.0,1.0,"scale"),
            kf(1.5,  1.0+0.018*math.sin(phase),         1.0+0.018*math.sin(phase),         1.0,"scale"),
            kf(3.0,  1.0+0.018*math.sin(phase+math.pi/2), 1.0+0.018*math.sin(phase+math.pi/2), 1.0,"scale"),
            kf(4.5,  1.0+0.018*math.sin(phase+math.pi),   1.0+0.018*math.sin(phase+math.pi),   1.0,"scale"),
            kf(6.0,  1.0,1.0,1.0,"scale"),
            kf(0.0,  0,0,0,"position"), kf(2.0,0,0,0,"position"), kf(4.0,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(2.0,0,0,0,"rotation"), kf(4.0,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # INNER ACCENT RING pulse
    for i, buid in enumerate(iar_bids):
        phase = (i / 12.0) * 2 * math.pi
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1.0,1.0,1.0,"scale"),
            kf(1.5,  1.0+0.04*math.sin(phase),     1.0+0.04*math.sin(phase),     1.0,"scale"),
            kf(3.0,  1.0+0.04*math.sin(phase+math.pi), 1.0+0.04*math.sin(phase+math.pi), 1.0,"scale"),
            kf(4.5,  1.0+0.04*math.sin(phase+2*math.pi/3), 1.0+0.04*math.sin(phase+2*math.pi/3), 1.0,"scale"),
            kf(6.0,  1.0,1.0,1.0,"scale"),
            kf(0.0,  0,0,0,"position"), kf(2.0,0,0,0,"position"), kf(4.0,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(2.0,0,0,0,"rotation"), kf(4.0,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # HOUR MARKERS — emissive pulse (scale)
    for i, buid in enumerate(hm_bids):
        phase = (i / 12.0) * 2 * math.pi
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1.0,1.0,1.0,"scale"),
            kf(1.5,  1.0+0.07*math.sin(phase),         1.0+0.07*math.sin(phase),         1.0,"scale"),
            kf(3.0,  1.0+0.07*math.sin(phase+math.pi/2), 1.0+0.07*math.sin(phase+math.pi/2), 1.0,"scale"),
            kf(4.5,  1.0+0.07*math.sin(phase+math.pi),   1.0+0.07*math.sin(phase+math.pi),   1.0,"scale"),
            kf(6.0,  1.0,1.0,1.0,"scale"),
            kf(0.0,  0,0,0,"position"), kf(2.0,0,0,0,"position"), kf(4.0,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(2.0,0,0,0,"rotation"), kf(4.0,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # HOUR MARKER ACCENTS pulse offset
    for i, buid in enumerate(hma_bids):
        phase = (i / 12.0) * 2 * math.pi + math.pi / 4
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1.0,1.0,1.0,"scale"),
            kf(1.5,  1.0+0.05*math.sin(phase),         1.0+0.05*math.sin(phase),         1.0,"scale"),
            kf(3.0,  1.0+0.05*math.sin(phase+math.pi/2), 1.0+0.05*math.sin(phase+math.pi/2), 1.0,"scale"),
            kf(4.5,  1.0+0.05*math.sin(phase+math.pi),   1.0+0.05*math.sin(phase+math.pi),   1.0,"scale"),
            kf(6.0,  1.0,1.0,1.0,"scale"),
            kf(0.0,  0,0,0,"position"), kf(2.0,0,0,0,"position"), kf(4.0,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(2.0,0,0,0,"rotation"), kf(4.0,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # HOUR HAND — barely moves, WRONG (counter-clockwise) ~5° over 6s
    for buid in hh_bids:
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"rotation"),
            kf(1.5,  0,0,-1.25,"rotation"),
            kf(3.0,  0,0,-2.5,"rotation"),
            kf(4.5,  0,0,-3.75,"rotation"),
            kf(6.0,  0,0,-5.0,"rotation"),
            kf(0.0,  0,0,0,"position"), kf(2.0,0,0,0,"position"), kf(4.0,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"), kf(2.0,1,1,1,"scale"), kf(4.0,1,1,1,"scale"), kf(L,1,1,1,"scale"),
        ]}

    # MINUTE HAND — counter-clockwise but slower than second; ~3 full rotations over 6s = -1080°
    for buid in mh_bids:
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"rotation"),
            kf(1.0,  0,0,-180,"rotation"),
            kf(2.0,  0,0,-360,"rotation"),
            kf(3.0,  0,0,-540,"rotation"),
            kf(4.0,  0,0,-720,"rotation"),
            kf(5.0,  0,0,-900,"rotation"),
            kf(6.0,  0,0,-1080,"rotation"),
            kf(0.0,  0,0,0,"position"), kf(2.0,0,0,0,"position"), kf(4.0,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"), kf(2.0,1,1,1,"scale"), kf(4.0,1,1,1,"scale"), kf(L,1,1,1,"scale"),
        ]}

    # SECOND HAND — counter-clockwise rapidly, 12 full rotations over 6s = -4320°
    for buid in sh_bids:
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"rotation"),
            kf(1.0,  0,0,-720,"rotation"),
            kf(2.0,  0,0,-1440,"rotation"),
            kf(3.0,  0,0,-2160,"rotation"),
            kf(4.0,  0,0,-2880,"rotation"),
            kf(5.0,  0,0,-3600,"rotation"),
            kf(6.0,  0,0,-4320,"rotation"),
            kf(0.0,  0,0,0,"position"), kf(2.0,0,0,0,"position"), kf(4.0,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"), kf(2.0,1,1,1,"scale"), kf(4.0,1,1,1,"scale"), kf(L,1,1,1,"scale"),
        ]}

    # PIVOT CAP — slow Z rotation pulse
    for buid in piv_bids:
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1.0,1.0,1.0,"scale"),
            kf(1.5,  1.05,1.05,1.0,"scale"),
            kf(3.0,  1.0,1.0,1.0,"scale"),
            kf(4.5,  0.95,0.95,1.0,"scale"),
            kf(6.0,  1.0,1.0,1.0,"scale"),
            kf(0.0,  0,0,0,"position"), kf(2.0,0,0,0,"position"), kf(4.0,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(1.5,0,0,30,"rotation"), kf(3.0,0,0,60,"rotation"),
            kf(4.5,0,0,90,"rotation"), kf(L,0,0,120,"rotation"),
        ]}

    # PENDULUM ROD — swings on X rotation, 3s period (so 2 full swings in 6s loop)
    for buid in pend_rod_bids:
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0, 0,"rotation"),
            kf(0.75, 0,0, 14,"rotation"),
            kf(1.5,  0,0, 0,"rotation"),
            kf(2.25, 0,0,-14,"rotation"),
            kf(3.0,  0,0, 0,"rotation"),
            kf(3.75, 0,0, 14,"rotation"),
            kf(4.5,  0,0, 0,"rotation"),
            kf(5.25, 0,0,-14,"rotation"),
            kf(6.0,  0,0, 0,"rotation"),
            kf(0.0,  0,0,0,"position"), kf(2.0,0,0,0,"position"), kf(4.0,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"), kf(2.0,1,1,1,"scale"), kf(4.0,1,1,1,"scale"), kf(L,1,1,1,"scale"),
        ]}

    # PENDULUM WEIGHT — same swing
    for buid in pend_w_bids:
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0, 0,"rotation"),
            kf(0.75, 0,0, 14,"rotation"),
            kf(1.5,  0,0, 0,"rotation"),
            kf(2.25, 0,0,-14,"rotation"),
            kf(3.0,  0,0, 0,"rotation"),
            kf(3.75, 0,0, 14,"rotation"),
            kf(4.5,  0,0, 0,"rotation"),
            kf(5.25, 0,0,-14,"rotation"),
            kf(6.0,  0,0, 0,"rotation"),
            kf(0.0,  0,0,0,"position"), kf(2.0,0,0,0,"position"), kf(4.0,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"), kf(2.0,1,1,1,"scale"), kf(4.0,1,1,1,"scale"), kf(L,1,1,1,"scale"),
        ]}

    # CHAIN LINKS — same partial swing
    for i, buid in enumerate(pcl_bids):
        amp = 14 * (i + 1) / 5.0
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0, 0,"rotation"),
            kf(0.75, 0,0, amp,"rotation"),
            kf(1.5,  0,0, 0,"rotation"),
            kf(2.25, 0,0,-amp,"rotation"),
            kf(3.0,  0,0, 0,"rotation"),
            kf(3.75, 0,0, amp,"rotation"),
            kf(4.5,  0,0, 0,"rotation"),
            kf(5.25, 0,0,-amp,"rotation"),
            kf(6.0,  0,0, 0,"rotation"),
            kf(0.0,  0,0,0,"position"), kf(2.0,0,0,0,"position"), kf(4.0,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"), kf(2.0,1,1,1,"scale"), kf(4.0,1,1,1,"scale"), kf(L,1,1,1,"scale"),
        ]}

    # KEYHOLE — pulse with hour markers
    for i, buid in enumerate(key_bids):
        phase = i * 0.5 + 0.3
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1.0,1.0,1.0,"scale"),
            kf(1.5,  1.0+0.04*math.sin(phase),     1.0+0.04*math.sin(phase),     1.0,"scale"),
            kf(3.0,  1.0+0.04*math.sin(phase+math.pi/2), 1.0+0.04*math.sin(phase+math.pi/2), 1.0,"scale"),
            kf(4.5,  1.0+0.04*math.sin(phase+math.pi), 1.0+0.04*math.sin(phase+math.pi), 1.0,"scale"),
            kf(6.0,  1.0,1.0,1.0,"scale"),
            kf(0.0,  0,0,0,"position"), kf(2.0,0,0,0,"position"), kf(4.0,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(2.0,0,0,0,"rotation"), kf(4.0,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # FRAME / FRAME CORNERS / FRAME STUDS — very subtle settle
    for buid_list in [frame_bids, fc_bids, fes_bids]:
        for i, buid in enumerate(buid_list):
            phase = i * 0.4
            animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
                kf(0.0,  1.0,1.0,1.0,"scale"),
                kf(1.5,  1.0+0.005*math.sin(phase),     1.0+0.005*math.sin(phase),     1.0,"scale"),
                kf(3.0,  1.0+0.005*math.sin(phase+math.pi/2), 1.0+0.005*math.sin(phase+math.pi/2), 1.0,"scale"),
                kf(4.5,  1.0+0.005*math.sin(phase+math.pi), 1.0+0.005*math.sin(phase+math.pi), 1.0,"scale"),
                kf(6.0,  1.0,1.0,1.0,"scale"),
                kf(0.0,  0,0,0,"position"), kf(2.0,0,0,0,"position"), kf(4.0,0,0,0,"position"), kf(L,0,0,0,"position"),
                kf(0.0,  0,0,0,"rotation"), kf(2.0,0,0,0,"rotation"), kf(4.0,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
            ]}

    return animators


def build_dissipate():
    """dissipate once, 0.8s:
    - clock hands spin increasingly fast then detach and fly outward
    - face slabs scatter
    - frame collapses
    - pendulum falls downward and scales to 0
    """
    animators = {}
    L = 0.8

    # FACE SLABS — scatter outward from center based on angle
    for i, buid in enumerate(face_bids):
        angle = (i / 12.0) * 2 * math.pi - math.pi / 2
        sx = math.cos(angle) * 4.0
        sy = math.sin(angle) * 4.0
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.20, 1,1,1,"scale"),
            kf(0.45, 0.7,0.7,0.7,"scale"),
            kf(0.65, 0.3,0.3,0.3,"scale"),
            kf(L,    0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.20, sx*0.3, sy*0.3, 0,"position"),
            kf(0.45, sx*0.8, sy*0.8, 0,"position"),
            kf(0.65, sx*1.2, sy*1.2, 0,"position"),
            kf(L,    sx*1.6, sy*1.6, 0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.4,  20,30,40,"rotation"),
            kf(L,    60,90,180,"rotation"),
        ]}

    # INNER ACCENT RING — scatter similarly
    for i, buid in enumerate(iar_bids):
        angle = (i / 12.0) * 2 * math.pi - math.pi / 2 + math.pi / 12
        sx = math.cos(angle) * 3.5
        sy = math.sin(angle) * 3.5
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.2,  1,1,1,"scale"),
            kf(0.5,  0.4,0.4,0.4,"scale"),
            kf(L,    0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.2,  sx*0.3,sy*0.3,0,"position"),
            kf(0.5,  sx*0.8,sy*0.8,0,"position"),
            kf(L,    sx*1.4,sy*1.4,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.4,  10,20,30,"rotation"),
            kf(L,    40,60,90,"rotation"),
        ]}

    # HOUR MARKERS — fly outward
    for i, buid in enumerate(hm_bids):
        angle = (i / 12.0) * 2 * math.pi - math.pi / 2
        sx = math.cos(angle) * 5.5
        sy = math.sin(angle) * 5.5
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.15, 1,1,1,"scale"),
            kf(0.4,  0.6,0.6,0.6,"scale"),
            kf(L,    0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.15, sx*0.3,sy*0.3,0.5,"position"),
            kf(0.4,  sx*0.7,sy*0.7,1.5,"position"),
            kf(L,    sx*1.3,sy*1.3,3.0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.4,  30,40,50,"rotation"),
            kf(L,    90,120,180,"rotation"),
        ]}

    # HOUR MARKER ACCENTS
    for i, buid in enumerate(hma_bids):
        angle = (i / 12.0) * 2 * math.pi - math.pi / 2
        sx = math.cos(angle) * 4.0
        sy = math.sin(angle) * 4.0
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.15, 1,1,1,"scale"),
            kf(0.4,  0.5,0.5,0.5,"scale"),
            kf(L,    0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.15, sx*0.3,sy*0.3,0,"position"),
            kf(0.4,  sx*0.7,sy*0.7,0.8,"position"),
            kf(L,    sx*1.2,sy*1.2,2.0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.4,  20,30,40,"rotation"),
            kf(L,    80,110,160,"rotation"),
        ]}

    # HOUR HAND — spin faster then detach and fly outward
    for i, buid in enumerate(hh_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.25, 1,1,1,"scale"),
            kf(0.5,  0.8,0.8,0.8,"scale"),
            kf(L,    0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.25, 0,0,0,"position"),
            kf(0.5,  -1.5,1.5,1,"position"),
            kf(L,    -3.5,3.5,3,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.10, 0,0,90,"rotation"),
            kf(0.20, 0,0,300,"rotation"),
            kf(0.30, 0,0,720,"rotation"),
            kf(0.5,  0,0,1080,"rotation"),
            kf(L,    0,0,1440,"rotation"),
        ]}

    # MINUTE HAND
    for i, buid in enumerate(mh_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.25, 1,1,1,"scale"),
            kf(0.5,  0.8,0.8,0.8,"scale"),
            kf(L,    0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.25, 0,0,0,"position"),
            kf(0.5,  2,1.5,1,"position"),
            kf(L,    4,3.5,3,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.10, 0,0,-180,"rotation"),
            kf(0.20, 0,0,-540,"rotation"),
            kf(0.30, 0,0,-1080,"rotation"),
            kf(0.5,  0,0,-1620,"rotation"),
            kf(L,    0,0,-2160,"rotation"),
        ]}

    # SECOND HAND
    for i, buid in enumerate(sh_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.25, 1,1,1,"scale"),
            kf(0.5,  0.7,0.7,0.7,"scale"),
            kf(L,    0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.25, 0,0,0,"position"),
            kf(0.5,  1,-2,1,"position"),
            kf(L,    3,-4,3,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.10, 0,0,-360,"rotation"),
            kf(0.20, 0,0,-1080,"rotation"),
            kf(0.30, 0,0,-2160,"rotation"),
            kf(0.5,  0,0,-3240,"rotation"),
            kf(L,    0,0,-4320,"rotation"),
        ]}

    # PIVOT CAP — vanishes
    for buid in piv_bids:
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.2,  1.3,1.3,1.3,"scale"),
            kf(0.4,  0.6,0.6,0.6,"scale"),
            kf(L,    0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"), kf(0.3,0,0,0,"position"), kf(0.5,0,0,0,"position"),
            kf(L,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.3,0,0,180,"rotation"), kf(0.5,0,0,360,"rotation"),
            kf(L,0,0,720,"rotation"),
        ]}

    # FRAME — collapses (scale to 0)
    for i, buid in enumerate(frame_bids):
        delay = 0.10 + i * 0.04
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(delay, 1,1,1,"scale"),
            kf(delay+0.15, 0.5,0.5,0.5,"scale"),
            kf(0.6,  0.2,0.2,0.2,"scale"),
            kf(L,    0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(delay, 0,0,0,"position"),
            kf(0.6,  0,-0.5,0,"position"),
            kf(L,    0,-1.5,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.4,  5,0,5,"rotation"),
            kf(L,    15,0,15,"rotation"),
        ]}

    # FRAME CORNERS — collapse outward
    for i, buid in enumerate(fc_bids):
        sx = (-1 if i in [0, 2] else 1) * 2.0
        sy = (1 if i in [0, 1] else -1) * 2.0
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.2,  1,1,1,"scale"),
            kf(0.5,  0.5,0.5,0.5,"scale"),
            kf(L,    0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.2,  sx*0.2,sy*0.2,0,"position"),
            kf(0.5,  sx*0.7,sy*0.7,0.5,"position"),
            kf(L,    sx*1.5,sy*1.5,1.5,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.4,  20,20,30,"rotation"),
            kf(L,    60,60,90,"rotation"),
        ]}

    # FRAME STUDS — fall away
    for i, buid in enumerate(fes_bids):
        delay = 0.05 + (i % 4) * 0.04
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(delay, 1,1,1,"scale"),
            kf(delay+0.15, 0.4,0.4,0.4,"scale"),
            kf(L,    0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(delay, 0,0,0,"position"),
            kf(delay+0.15, 0,-0.3,0,"position"),
            kf(L,    0,-2.0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.4,15,30,15,"rotation"), kf(L,45,90,45,"rotation"),
        ]}

    # PENDULUM ROD — falls downward and scales to 0
    for buid in pend_rod_bids:
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.2,  1,1,1,"scale"),
            kf(0.5,  0.8,0.5,0.8,"scale"),
            kf(L,    0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.2,  0,-0.3,0,"position"),
            kf(0.5,  0,-1.5,0,"position"),
            kf(L,    0,-3.5,0,"position"),
            kf(0.0,  0,0, 0,"rotation"),
            kf(0.4,  0,0,18,"rotation"),
            kf(L,    0,0,40,"rotation"),
        ]}

    # PENDULUM WEIGHT — falls
    for buid in pend_w_bids:
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.2,  1,1,1,"scale"),
            kf(0.5,  0.7,0.7,0.7,"scale"),
            kf(L,    0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.2,  0,-0.4,0,"position"),
            kf(0.5,  0,-2.0,0,"position"),
            kf(L,    0,-4.5,0,"position"),
            kf(0.0,  0,0, 0,"rotation"),
            kf(0.4,  0,0,30,"rotation"),
            kf(L,    0,0,90,"rotation"),
        ]}

    # PENDULUM CHAIN LINKS — fall with rod
    for i, buid in enumerate(pcl_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.15, 1,1,1,"scale"),
            kf(0.4,  0.6,0.6,0.6,"scale"),
            kf(L,    0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.15, 0,-0.2,0,"position"),
            kf(0.4,  0,-1.0-i*0.2,0,"position"),
            kf(L,    0,-2.5-i*0.4,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.3,  0,0,12,"rotation"),
            kf(L,    0,0,30,"rotation"),
        ]}

    # KEYHOLE — fly forward and dissolve
    for i, buid in enumerate(key_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.2,  1,1,1,"scale"),
            kf(0.5,  0.5,0.5,0.5,"scale"),
            kf(L,    0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.2,  0,0,0.5,"position"),
            kf(0.5,  (i-1)*0.5,-0.5,1.5,"position"),
            kf(L,    (i-1)*1.2,-1.5,3.5,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.4,  20,30,40,"rotation"),
            kf(L,    60,90,120,"rotation"),
        ]}

    return animators

# ── Build model ──────────────────────────────────────────────────────────────
spawn_anim = {
    "uuid": aid(1), "name": "animation.nightmare_clock.spawn",
    "loop": "once", "length": 1.2, "snapping": 24,
    "selected": False, "saved": False, "animators": build_spawn()
}
idle_anim = {
    "uuid": aid(2), "name": "animation.nightmare_clock.idle",
    "loop": "loop", "length": 6.0, "snapping": 24,
    "selected": False, "saved": False, "animators": build_idle()
}
dissipate_anim = {
    "uuid": aid(3), "name": "animation.nightmare_clock.dissipate",
    "loop": "once", "length": 0.8, "snapping": 24,
    "selected": False, "saved": False, "animators": build_dissipate()
}

# Verify all bones have animators in all 3 anims, with 6+ keyframes each
for anim_name, anim in [("spawn", spawn_anim), ("idle", idle_anim), ("dissipate", dissipate_anim)]:
    animators = anim["animators"]
    for buid in all_bids:
        assert buid in animators, f"Bone {buid} missing animator in {anim_name}"
        kfs = animators[buid]["keyframes"]
        assert len(kfs) >= 6, f"Bone {buid} only has {len(kfs)} keyframes in {anim_name}"

outliner = [b["uuid"] for b in bones]

textures = [
    {"id":"0","name":"nightmare_clock_tex","relative_path":"../textures/nightmare_clock_tex.png",
     "folder":"devilsdream","namespace":"","visible":True,"mode":"bitmap","saved":False,
     "uuid":tid(1),"source":f"data:image/png;base64,{b64_0}"},
    {"id":"1","name":"nightmare_clock_tex1","relative_path":"../textures/nightmare_clock_tex1.png",
     "folder":"devilsdream","namespace":"","visible":True,"mode":"bitmap","saved":False,
     "uuid":tid(2),"source":f"data:image/png;base64,{b64_1}"},
]

model = {
    "meta": {"format_version":"4.10","model_format":"free","box_uv":False},
    "name": "nightmare_clock",
    "geometry": "geometry.nightmare_clock",
    "resolution": {"width":64,"height":64},
    "elements": elements,
    "outliner": outliner,
    "textures": textures,
    "animations": [spawn_anim, idle_anim, dissipate_anim]
}

out_path = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_clock.bbmodel"
with open(out_path, "w", encoding="utf-8") as f:
    json.dump(model, f, separators=(',', ':'))

size = os.path.getsize(out_path)
print(f"Written: {out_path}")
print(f"Elements: {len(elements)} | Bones: {len(bones)}")
print(f"Size: {size:,} bytes ({size/1024:.1f} KB)")
pass_fail = "PASS" if size >= 300*1024 else "FAIL"
print(f"{pass_fail}: {'>=300KB' if size>=300*1024 else f'only {size//1024}KB'}")
