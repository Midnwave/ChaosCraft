"""
Build fallen_seraph_skeleton.bbmodel - 80+ elements, 55+ bones, 3 animations.

Front view reads as a partial winged skeleton:
- Vertical sternum (3 stacked cubes) along Y axis at center, Y=2-7
- 8 rib bones (4 left, 4 right) radiating from sternum, each = 3-segment chain
  rotated 22.5° outward+downward to suggest curve. Ribs span X=±5 wide.
- 4 spine vertebrae stacked above sternum (Y=7-11), each = body + 2 transverse
- 2 wing bone fragments per side (4 total) at X=±5 to ±7
- 5 floating skull fragments above (Y=12-15)
- 6 ground crack slabs at Y=0 (tex1)
- Plus rib detail sub-segments, vertebra inner details, skull suture pieces, sternum joints
"""
import json, math, os

b64_0 = open("D:/CC/ChaosCraft/tools/gen_textures/m22_b64.txt").read().strip()
b64_1 = open("D:/CC/ChaosCraft/tools/gen_textures/m22_tex1_b64.txt").read().strip()

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

# ── 1. STERNUM (3 bones, 3 elements) ────────────────────────────────────────
# Vertical center column along Y axis, Y=2 to 7
stern_bids = []
stern_defs = [
    ("sternum_lower", [-0.55, 2.0, -0.45], [0.55, 3.7, 0.45]),
    ("sternum_mid",   [-0.65, 3.7, -0.50], [0.65, 5.4, 0.50]),
    ("sternum_upper", [-0.55, 5.4, -0.45], [0.55, 7.0, 0.45]),
]
for name, fr, to in stern_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    cx_b = (fr[0]+to[0])/2; cy_b = (fr[1]+to[1])/2
    b = bone(bi, name, [cx_b, cy_b, 0.0], [eid(ei)])
    bones.append(b)
    stern_bids.append(bid(bi)); ei+=1; bi+=1

# ── 2. STERNUM JOINT ACCENTS (3 bones, 3 elements) ──────────────────────────
# Small disk-like accents between sternum cubes (joints)
sj_bids = []
sj_defs = [
    ("stern_joint_1", [-0.45, 3.55, -0.35], [0.45, 3.75, 0.35]),
    ("stern_joint_2", [-0.55, 5.30, -0.40], [0.55, 5.50, 0.40]),
    ("stern_joint_3", [-0.40, 6.95, -0.30], [0.40, 7.15, 0.30]),
]
for name, fr, to in sj_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    cx_b = (fr[0]+to[0])/2; cy_b = (fr[1]+to[1])/2
    b = bone(bi, name, [cx_b, cy_b, 0.0], [eid(ei)])
    bones.append(b)
    sj_bids.append(bid(bi)); ei+=1; bi+=1

# ── 3. RIBS (24 bones, 24 elements) ─────────────────────────────────────────
# 8 ribs total (4 per side), each = 3 sub-segments forming a curve.
# Pivot at sternum-side junction; angles step outward + slightly downward as we go down
rib_bids = []
# Each rib parameter: y_attach (sternum y), seg_lengths, base_angle_deg
# Side: -1 (left) or +1 (right)
rib_y_positions = [3.0, 4.0, 5.0, 6.0]  # 4 ribs per side
rib_attach_x = [0.55, 0.65, 0.65, 0.55]  # sternum X edge at each Y

for side in [-1, 1]:
    for r_i, attach_y in enumerate(rib_y_positions):
        attach_x = side * rib_attach_x[r_i]
        # Each rib has 3 segments curving outward then down
        # Segment lengths and angle progression
        seg_len = 1.6
        # Rib curves: starts horizontal then curves down
        base_angles = [0.0, 22.5, 45.0]  # cumulative angle from horizontal (down) for each segment
        # Segment 1 - horizontal-ish from sternum
        # Segment 2 - curves down 22.5°
        # Segment 3 - curves down further 45° from horizontal
        # We pre-compute world positions with each segment chained from previous
        prev_x = attach_x
        prev_y = attach_y
        for s_i, ang_deg in enumerate(base_angles):
            # angle measured downward+outward from horizontal
            ang = math.radians(ang_deg)
            # outward direction = side, downward as angle increases
            dx = side * seg_len * math.cos(ang)
            dy = -seg_len * math.sin(ang)  # negative Y as ribs curve down
            cur_x = prev_x + dx
            cur_y = prev_y + dy
            # Build slab from (prev) to (cur)
            half_thick_y = 0.20
            half_thick_z = 0.45
            min_x = min(prev_x, cur_x)
            max_x = max(prev_x, cur_x)
            min_y = min(prev_y, cur_y) - half_thick_y
            max_y = max(prev_y, cur_y) + half_thick_y
            fr = [min_x, min_y, -half_thick_z]
            to = [max_x, max_y, +half_thick_z]
            name = f"rib_{'L' if side<0 else 'R'}{r_i+1}_s{s_i+1}"
            e = elem(ei, name, fr, to, UV0); elements.append(e)
            origin_x = prev_x
            origin_y = prev_y
            b = bone(bi, name, [origin_x, origin_y, 0.0], [eid(ei)])
            bones.append(b)
            rib_bids.append(bid(bi)); ei+=1; bi+=1
            prev_x = cur_x; prev_y = cur_y

# ── 4. RIB TIP CAPS (8 bones, 8 elements) ──────────────────────────────────
# Small cap pieces at the tip of each rib (decoration)
rt_bids = []
for side in [-1, 1]:
    for r_i, attach_y in enumerate(rib_y_positions):
        attach_x = side * rib_attach_x[r_i]
        # Compute tip position from segments
        prev_x = attach_x; prev_y = attach_y
        for s_i, ang_deg in enumerate([0.0, 22.5, 45.0]):
            ang = math.radians(ang_deg)
            prev_x += side * 1.6 * math.cos(ang)
            prev_y += -1.6 * math.sin(ang)
        # Now (prev_x, prev_y) is the tip
        name = f"ribtip_{'L' if side<0 else 'R'}{r_i+1}"
        fr = [prev_x - 0.3 if side<0 else prev_x - 0.3, prev_y - 0.30, -0.30]
        to = [prev_x + 0.3 if side<0 else prev_x + 0.3, prev_y + 0.30, +0.30]
        e = elem(ei, name, fr, to, UV0); elements.append(e)
        b = bone(bi, name, [prev_x, prev_y, 0.0], [eid(ei)])
        bones.append(b)
        rt_bids.append(bid(bi)); ei+=1; bi+=1

# ── 5. SPINE VERTEBRAE (4 vertebrae × 3 pieces = 12 bones, 12 elements) ─────
# 4 spine vertebrae stacked above sternum at Y=7-11
# Each = body cube + 2 transverse process slabs
spine_bids = []
vert_y_positions = [7.2, 8.2, 9.2, 10.2]
for v_i, vy in enumerate(vert_y_positions):
    # Body cube
    name_b = f"vert_{v_i+1}_body"
    fr = [-0.5, vy, -0.5]
    to = [+0.5, vy + 0.7, +0.5]
    e = elem(ei, name_b, fr, to, UV0); elements.append(e)
    b = bone(bi, name_b, [0.0, vy + 0.35, 0.0], [eid(ei)])
    bones.append(b)
    spine_bids.append(bid(bi)); ei+=1; bi+=1
    # Left transverse process
    name_tl = f"vert_{v_i+1}_TL"
    fr = [-1.5, vy + 0.15, -0.20]
    to = [-0.5, vy + 0.55, +0.20]
    e = elem(ei, name_tl, fr, to, UV0); elements.append(e)
    b = bone(bi, name_tl, [-1.0, vy + 0.35, 0.0], [eid(ei)])
    bones.append(b)
    spine_bids.append(bid(bi)); ei+=1; bi+=1
    # Right transverse process
    name_tr = f"vert_{v_i+1}_TR"
    fr = [+0.5, vy + 0.15, -0.20]
    to = [+1.5, vy + 0.55, +0.20]
    e = elem(ei, name_tr, fr, to, UV0); elements.append(e)
    b = bone(bi, name_tr, [+1.0, vy + 0.35, 0.0], [eid(ei)])
    bones.append(b)
    spine_bids.append(bid(bi)); ei+=1; bi+=1

# ── 6. WING BONE FRAGMENTS (4 bones, 4 elements) ───────────────────────────
# 2 large flat slabs per side at X=±5 to ±7
# Position: angled outward from sternum at Y=4-6
wing_bids = []
wing_defs = [
    # name, fr, to, rotation
    ("wing_L_upper", [-7.0, 5.5, -0.4], [-5.0, 6.0, 0.4]),
    ("wing_L_lower", [-7.5, 4.0, -0.4], [-5.5, 4.5, 0.4]),
    ("wing_R_upper", [+5.0, 5.5, -0.4], [+7.0, 6.0, 0.4]),
    ("wing_R_lower", [+5.5, 4.0, -0.4], [+7.5, 4.5, 0.4]),
]
for name, fr, to in wing_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    cx_b = (fr[0]+to[0])/2; cy_b = (fr[1]+to[1])/2
    b = bone(bi, name, [cx_b, cy_b, 0.0], [eid(ei)])
    bones.append(b)
    wing_bids.append(bid(bi)); ei+=1; bi+=1

# ── 7. WING BONE DETAIL (8 bones, 8 elements) ──────────────────────────────
# Smaller fragmenting detail pieces near the wing fragments
wd_bids = []
wd_defs = [
    ("wing_L_frag1", [-6.5, 6.05, -0.30], [-5.5, 6.55, 0.30]),
    ("wing_L_frag2", [-7.5, 4.55, -0.30], [-6.7, 5.05, 0.30]),
    ("wing_L_frag3", [-7.0, 3.5,  -0.25], [-6.2, 3.95, 0.25]),
    ("wing_L_frag4", [-5.0, 6.05, -0.25], [-4.4, 6.5,  0.25]),
    ("wing_R_frag1", [+5.5, 6.05, -0.30], [+6.5, 6.55, 0.30]),
    ("wing_R_frag2", [+6.7, 4.55, -0.30], [+7.5, 5.05, 0.30]),
    ("wing_R_frag3", [+6.2, 3.5,  -0.25], [+7.0, 3.95, 0.25]),
    ("wing_R_frag4", [+4.4, 6.05, -0.25], [+5.0, 6.5,  0.25]),
]
for name, fr, to in wd_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    cx_b = (fr[0]+to[0])/2; cy_b = (fr[1]+to[1])/2
    b = bone(bi, name, [cx_b, cy_b, 0.0], [eid(ei)])
    bones.append(b)
    wd_bids.append(bid(bi)); ei+=1; bi+=1

# ── 8. SKULL FRAGMENTS (5 bones, 5 elements) ───────────────────────────────
# 5 irregular floating fragments above the ribcage at Y=12-15
skull_bids = []
skull_defs = [
    # name, fr, to (irregular cubes)
    ("skull_frag_1", [-1.4, 12.5, -0.7], [-0.3, 13.6,  0.5]),   # left fragment
    ("skull_frag_2", [+0.4, 13.0, -0.6], [+1.5, 14.2,  0.5]),   # right fragment
    ("skull_frag_3", [-0.7, 14.3, -0.7], [+0.5, 15.3,  0.7]),   # top fragment (skull cap shard)
    ("skull_frag_4", [-2.0, 12.0, -0.5], [-1.2, 12.8,  0.4]),   # far left fragment
    ("skull_frag_5", [+1.4, 12.2, -0.5], [+2.1, 13.0,  0.4]),   # far right fragment
]
for name, fr, to in skull_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    cx_b = (fr[0]+to[0])/2; cy_b = (fr[1]+to[1])/2
    cz_b = (fr[2]+to[2])/2
    b = bone(bi, name, [cx_b, cy_b, cz_b], [eid(ei)])
    bones.append(b)
    skull_bids.append(bid(bi)); ei+=1; bi+=1

# ── 9. SKULL SUTURE PIECES (5 bones, 5 elements) ───────────────────────────
# Small fragments suggesting cracked sutures
ss_bids = []
ss_defs = [
    ("skull_sut_1", [-0.20, 13.5, -0.25], [+0.20, 13.7, 0.25]),
    ("skull_sut_2", [-0.4,  14.0, -0.25], [-0.05, 14.2, 0.25]),
    ("skull_sut_3", [+0.1,  14.5, -0.25], [+0.5,  14.7, 0.25]),
    ("skull_sut_4", [-1.4,  12.6, -0.20], [-1.05, 12.8, 0.20]),
    ("skull_sut_5", [+1.1,  13.2, -0.20], [+1.45, 13.4, 0.20]),
]
for name, fr, to in ss_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    cx_b = (fr[0]+to[0])/2; cy_b = (fr[1]+to[1])/2
    cz_b = (fr[2]+to[2])/2
    b = bone(bi, name, [cx_b, cy_b, cz_b], [eid(ei)])
    bones.append(b)
    ss_bids.append(bid(bi)); ei+=1; bi+=1

# ── 10. GROUND CRACKS (6 bones, 6 elements) — TEX1 ──────────────────────────
# 6 flat slabs at Y=0 radiating from skeleton's base
gc_bids = []
for i in range(6):
    angle = (i / 6.0) * 2 * math.pi
    name = f"gcrack_{i+1}"
    # Slab radiating outward at ground level (Y=0)
    length = 4.0
    width = 0.5
    fr = [-width, 0.0, 0.0]
    to = [length, 0.05, width*2]
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    b = bone(bi, name, [0, 0, 0], [eid(ei)], rot=[0, math.degrees(angle), 0])
    bones.append(b)
    gc_bids.append(bid(bi)); ei+=1; bi+=1

# ── 11. SPINE VERTEBRA SPINOUS PROCESS (4 bones, 4 elements) ───────────────
# Small back spike on each vertebra (depth detail, makes spine recognizable)
sp_bids = []
for v_i, vy in enumerate(vert_y_positions):
    name = f"vert_{v_i+1}_spinous"
    fr = [-0.20, vy + 0.10, -1.0]
    to = [+0.20, vy + 0.50, -0.5]
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [0.0, vy + 0.35, -0.7], [eid(ei)])
    bones.append(b)
    sp_bids.append(bid(bi)); ei+=1; bi+=1

# ── 12. CRACKED RIB ACCENTS (4 bones, 4 elements) ──────────────────────────
# Small fracture pieces on selected ribs (broken bone detail)
cra_bids = []
cra_defs = [
    ("rib_crack_L1", [-2.6, 3.4, -0.4], [-2.1, 3.7,  0.4]),
    ("rib_crack_R1", [+2.1, 3.4, -0.4], [+2.6, 3.7,  0.4]),
    ("rib_crack_L2", [-3.5, 4.5, -0.4], [-3.0, 4.85, 0.4]),
    ("rib_crack_R2", [+3.0, 4.5, -0.4], [+3.5, 4.85, 0.4]),
]
for name, fr, to in cra_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    cx_b = (fr[0]+to[0])/2; cy_b = (fr[1]+to[1])/2
    b = bone(bi, name, [cx_b, cy_b, 0.0], [eid(ei)])
    bones.append(b)
    cra_bids.append(bid(bi)); ei+=1; bi+=1

# Summary
print(f"Elements: {len(elements)}, Bones: {len(bones)}")
assert len(elements) == len(bones)
assert len(elements) >= 80, f"Need 80+ elements, got {len(elements)}"

all_bids = (stern_bids + sj_bids + rib_bids + rt_bids + spine_bids + wing_bids
            + wd_bids + skull_bids + ss_bids + gc_bids + sp_bids + cra_bids)
assert len(all_bids) == len(bones), f"Bid total {len(all_bids)} != bones {len(bones)}"

bone_name_map = {b["uuid"]: b["name"] for b in bones}

# ══════════════════════════════════════════════════════════════════════════════
# ANIMATIONS
# ══════════════════════════════════════════════════════════════════════════════

def build_spawn():
    """spawn once, 1.6s:
    - ground cracks at 0.2s
    - sternum slams up at 0.4s
    - ribcage ribs deploy outward simultaneously from sternum at 0.5s
    - spine fragments stack upward at 0.7s
    - wing bone fragments fly in from sides at 0.9s
    - skull fragments drift down from above at 1.1s staggered
    """
    animators = {}
    L = 1.6

    # GROUND CRACKS at 0.2s — radial expansion
    for i, buid in enumerate(gc_bids):
        d = 0.18 + i * 0.015
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,1,0,"scale"),
            kf(d,    0,1,0,"scale"),
            kf(d+0.10, 0.5,1,0.5,"scale"),
            kf(d+0.25, 1,1,1,"scale"),
            kf(0.7,  1,1,1,"scale"),
            kf(1.2,  1,1,1,"scale"),
            kf(L,    1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"), kf(0.4,0,0,0,"position"), kf(0.8,0,0,0,"position"),
            kf(1.2,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.4,0,0,0,"rotation"), kf(0.8,0,0,0,"rotation"),
            kf(1.2,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # STERNUM slams up at 0.4s
    for i, buid in enumerate(stern_bids):
        d = 0.40 + i * 0.025
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(d,    0,0,0,"scale"),
            kf(d+0.06, 1.15,1.15,1.15,"scale"),
            kf(d+0.14, 1,1,1,"scale"),
            kf(1.0,  1,1,1,"scale"),
            kf(L,    1,1,1,"scale"),
            kf(0.0,  0,-2.0,0,"position"),
            kf(d,    0,-2.0,0,"position"),
            kf(d+0.10, 0,0.3,0,"position"),
            kf(d+0.20, 0,0,0,"position"),
            kf(1.0,  0,0,0,"position"),
            kf(L,    0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.4,0,0,0,"rotation"), kf(0.8,0,0,0,"rotation"),
            kf(1.2,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # STERNUM JOINTS — appear with sternum
    for i, buid in enumerate(sj_bids):
        d = 0.45 + i * 0.025
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(d,    0,0,0,"scale"),
            kf(d+0.08, 1,1,1,"scale"),
            kf(1.0,  1,1,1,"scale"),
            kf(1.3,  1,1,1,"scale"),
            kf(L,    1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"), kf(0.4,0,0,0,"position"), kf(0.8,0,0,0,"position"),
            kf(1.2,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.4,0,0,0,"rotation"), kf(0.8,0,0,0,"rotation"),
            kf(1.2,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # RIBS deploy outward from sternum at 0.5s
    for i, buid in enumerate(rib_bids):
        d = 0.50 + i * 0.012
        side = -1 if i < len(rib_bids)//2 else 1
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(d,    0,0,0,"scale"),
            kf(d+0.12, 1,1,1,"scale"),
            kf(1.0,  1,1,1,"scale"),
            kf(1.3,  1,1,1,"scale"),
            kf(L,    1,1,1,"scale"),
            kf(0.0,  -side*1.5, 0, 0,"position"),
            kf(d,    -side*1.5, 0, 0,"position"),
            kf(d+0.10, -side*0.5, 0, 0,"position"),
            kf(d+0.20, 0,0,0,"position"),
            kf(1.0,  0,0,0,"position"),
            kf(L,    0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.5,0,0,0,"rotation"), kf(0.9,0,0,0,"rotation"),
            kf(1.2,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # RIB TIP CAPS — deploy with ribs
    for i, buid in enumerate(rt_bids):
        d = 0.62 + i * 0.02
        side = -1 if i < 4 else 1
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(d,    0,0,0,"scale"),
            kf(d+0.10, 1,1,1,"scale"),
            kf(1.0,  1,1,1,"scale"),
            kf(1.3,  1,1,1,"scale"),
            kf(L,    1,1,1,"scale"),
            kf(0.0,  -side*1.0, 0, 0,"position"),
            kf(d,    -side*1.0, 0, 0,"position"),
            kf(d+0.10, 0,0,0,"position"),
            kf(1.0,  0,0,0,"position"),
            kf(1.3,0,0,0,"position"),
            kf(L,    0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.5,0,0,0,"rotation"), kf(0.9,0,0,0,"rotation"),
            kf(1.2,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # CRACKED RIB ACCENTS
    for i, buid in enumerate(cra_bids):
        d = 0.65 + i * 0.025
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(d,    0,0,0,"scale"),
            kf(d+0.10, 1,1,1,"scale"),
            kf(1.0,  1,1,1,"scale"),
            kf(1.3,  1,1,1,"scale"),
            kf(L,    1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"), kf(0.6,0,0,0,"position"), kf(0.9,0,0,0,"position"),
            kf(1.2,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.5,0,0,0,"rotation"), kf(0.9,0,0,0,"rotation"),
            kf(1.2,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # SPINE VERTEBRAE stack upward at 0.7s
    for i, buid in enumerate(spine_bids):
        # 3 bones per vertebra: body, TL, TR — we already structured spine_bids as
        # [v1_body, v1_TL, v1_TR, v2_body, ...]
        v_i = i // 3
        d = 0.70 + v_i * 0.06
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(d,    0,0,0,"scale"),
            kf(d+0.08, 1,1,1,"scale"),
            kf(1.1,  1,1,1,"scale"),
            kf(1.4,  1,1,1,"scale"),
            kf(L,    1,1,1,"scale"),
            kf(0.0,  0,-2.5,0,"position"),
            kf(d,    0,-2.5,0,"position"),
            kf(d+0.10, 0,-0.3,0,"position"),
            kf(d+0.18, 0,0,0,"position"),
            kf(1.4,  0,0,0,"position"),
            kf(L,    0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.5,0,0,0,"rotation"), kf(0.9,0,0,0,"rotation"),
            kf(1.2,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # SPINE SPINOUS PROCESSES — appear with vertebrae
    for i, buid in enumerate(sp_bids):
        d = 0.78 + i * 0.06
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(d,    0,0,0,"scale"),
            kf(d+0.08, 1,1,1,"scale"),
            kf(1.1,  1,1,1,"scale"),
            kf(1.4,  1,1,1,"scale"),
            kf(L,    1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"), kf(0.6,0,0,0,"position"), kf(1.0,0,0,0,"position"),
            kf(1.3,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.5,0,0,0,"rotation"), kf(0.9,0,0,0,"rotation"),
            kf(1.2,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # WING BONE FRAGMENTS fly in from sides at 0.9s
    for i, buid in enumerate(wing_bids):
        d = 0.90 + i * 0.04
        side = -1 if i < 2 else 1
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(d,    0,0,0,"scale"),
            kf(d+0.10, 1,1,1,"scale"),
            kf(1.3,  1,1,1,"scale"),
            kf(1.5,  1,1,1,"scale"),
            kf(L,    1,1,1,"scale"),
            kf(0.0,  side*5, 0, 0,"position"),
            kf(d,    side*5, 0, 0,"position"),
            kf(d+0.12, side*1, 0, 0,"position"),
            kf(d+0.22, 0,0,0,"position"),
            kf(1.3,  0,0,0,"position"),
            kf(L,    0,0,0,"position"),
            kf(0.0,  0,side*30,0,"rotation"),
            kf(d,    0,side*30,0,"rotation"),
            kf(d+0.18, 0,0,0,"rotation"),
            kf(1.3,  0,0,0,"rotation"),
            kf(L,    0,0,0,"rotation"),
        ]}

    # WING DETAIL FRAGMENTS — appear after wing bones
    for i, buid in enumerate(wd_bids):
        d = 1.00 + i * 0.025
        side = -1 if i < 4 else 1
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(d,    0,0,0,"scale"),
            kf(d+0.08, 1,1,1,"scale"),
            kf(1.3,  1,1,1,"scale"),
            kf(1.5,  1,1,1,"scale"),
            kf(L,    1,1,1,"scale"),
            kf(0.0,  side*3, 0, 0,"position"),
            kf(d,    side*3, 0, 0,"position"),
            kf(d+0.12, 0,0,0,"position"),
            kf(1.3,  0,0,0,"position"),
            kf(1.5,0,0,0,"position"),
            kf(L,    0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.5,0,0,0,"rotation"), kf(0.9,0,0,0,"rotation"),
            kf(1.2,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # SKULL FRAGMENTS drift down from above at 1.1s, staggered
    for i, buid in enumerate(skull_bids):
        d = 1.10 + i * 0.06
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(d,    0,0,0,"scale"),
            kf(d+0.08, 0.7,0.7,0.7,"scale"),
            kf(d+0.18, 1.05,1.05,1.05,"scale"),
            kf(d+0.28, 1,1,1,"scale"),
            kf(L,    1,1,1,"scale"),
            kf(0.0,  0,4,0,"position"),
            kf(d,    0,4,0,"position"),
            kf(d+0.10, 0,1.5,0,"position"),
            kf(d+0.22, 0,0.3,0,"position"),
            kf(d+0.32, 0,0,0,"position"),
            kf(L,    0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(d+0.16, 5,10,8,"rotation"),
            kf(L,    0,0,0,"rotation"),
        ]}

    # SKULL SUTURE PIECES — appear with skull
    for i, buid in enumerate(ss_bids):
        d = 1.20 + i * 0.05
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(d,    0,0,0,"scale"),
            kf(d+0.08, 1,1,1,"scale"),
            kf(1.5,  1,1,1,"scale"),
            kf(1.55, 1,1,1,"scale"),
            kf(L,    1,1,1,"scale"),
            kf(0.0,  0,3,0,"position"),
            kf(d,    0,3,0,"position"),
            kf(d+0.10, 0,0.5,0,"position"),
            kf(d+0.20, 0,0,0,"position"),
            kf(1.5,  0,0,0,"position"),
            kf(L,    0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.5,0,0,0,"rotation"), kf(0.9,0,0,0,"rotation"),
            kf(1.2,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    return animators


def build_idle():
    """idle loop, 8.0s:
    - the entire skeleton barely moves — it is bones
    - extremely subtle settling motion ±0.3°
    - each rib oscillates ±1° on a different period
    - skull fragments slowly drift at their heights, each rotating on a unique axis
    - spine vertebrae rock minimally
    - wing bone fragments sway ±0.5°
    """
    animators = {}
    L = 8.0

    # STERNUM — settle ±0.3°
    for i, buid in enumerate(stern_bids):
        phase = i * 0.7
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0, 0, 0,"rotation"),
            kf(2.0,  0.3*math.sin(phase),         0, 0.3*math.sin(phase+0.5),"rotation"),
            kf(4.0,  0.3*math.sin(phase+math.pi/2), 0, 0.3*math.sin(phase+math.pi/2+0.5),"rotation"),
            kf(6.0,  0.3*math.sin(phase+math.pi),   0, 0.3*math.sin(phase+math.pi+0.5),"rotation"),
            kf(8.0,  0, 0, 0,"rotation"),
            kf(0.0,  0,0,0,"position"), kf(2.0,0,0.02*math.sin(phase),0,"position"),
            kf(4.0,0,-0.02*math.sin(phase),0,"position"), kf(6.0,0,0.01*math.cos(phase),0,"position"),
            kf(L,0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"), kf(2.0,1,1,1,"scale"), kf(4.0,1,1,1,"scale"),
            kf(6.0,1,1,1,"scale"), kf(L,1,1,1,"scale"),
        ]}

    # STERNUM JOINTS
    for i, buid in enumerate(sj_bids):
        phase = i * 1.2
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1.0,1.0,1.0,"scale"),
            kf(2.0,  1.0+0.01*math.sin(phase),         1.0,1.0,"scale"),
            kf(4.0,  1.0+0.01*math.sin(phase+math.pi/2), 1.0,1.0,"scale"),
            kf(6.0,  1.0+0.01*math.sin(phase+math.pi),   1.0,1.0,"scale"),
            kf(8.0,  1.0,1.0,1.0,"scale"),
            kf(0.0,  0,0,0,"position"), kf(2.0,0,0,0,"position"), kf(4.0,0,0,0,"position"),
            kf(6.0,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(2.0,0,0,0,"rotation"), kf(4.0,0,0,0,"rotation"),
            kf(6.0,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # RIBS — each ±1° on a different period
    for i, buid in enumerate(rib_bids):
        phase = i * 0.5
        period_off = (i % 5) * 0.4
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0, 0, 1.0*math.sin(phase),"rotation"),
            kf(2.0+period_off, 0, 0, 1.0*math.sin(phase+math.pi/2),"rotation"),
            kf(4.0+period_off, 0, 0, 1.0*math.sin(phase+math.pi),"rotation"),
            kf(6.0+period_off*0.5, 0, 0, 1.0*math.sin(phase+3*math.pi/2),"rotation"),
            kf(8.0,  0, 0, 1.0*math.sin(phase+2*math.pi),"rotation"),
            kf(0.0,  0,0,0,"position"), kf(2.0,0,0,0,"position"), kf(4.0,0,0,0,"position"),
            kf(6.0,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"), kf(2.0,1,1,1,"scale"), kf(4.0,1,1,1,"scale"),
            kf(6.0,1,1,1,"scale"), kf(L,1,1,1,"scale"),
        ]}

    # RIB TIP CAPS
    for i, buid in enumerate(rt_bids):
        phase = i * 0.6
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1.0,1.0,1.0,"scale"),
            kf(2.0,  1.0+0.015*math.sin(phase),     1.0,1.0,"scale"),
            kf(4.0,  1.0+0.015*math.sin(phase+math.pi), 1.0,1.0,"scale"),
            kf(6.0,  1.0+0.015*math.sin(phase+math.pi/3), 1.0,1.0,"scale"),
            kf(8.0,  1.0,1.0,1.0,"scale"),
            kf(0.0,  0,0,0,"position"), kf(2.0,0,0,0,"position"), kf(4.0,0,0,0,"position"),
            kf(6.0,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(2.0,0,0,0,"rotation"), kf(4.0,0,0,0,"rotation"),
            kf(6.0,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # CRACKED RIB ACCENTS
    for i, buid in enumerate(cra_bids):
        phase = i * 0.7
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1.0,1.0,1.0,"scale"),
            kf(2.0,  1.0+0.02*math.sin(phase),     1.0,1.0,"scale"),
            kf(4.0,  1.0+0.02*math.sin(phase+math.pi), 1.0,1.0,"scale"),
            kf(6.0,  1.0+0.02*math.sin(phase+math.pi/2), 1.0,1.0,"scale"),
            kf(8.0,  1.0,1.0,1.0,"scale"),
            kf(0.0,  0,0,0,"position"), kf(2.0,0,0,0,"position"), kf(4.0,0,0,0,"position"),
            kf(6.0,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(2.0,0,0,0,"rotation"), kf(4.0,0,0,0,"rotation"),
            kf(6.0,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # SPINE VERTEBRAE rock minimally ±0.3°
    for i, buid in enumerate(spine_bids):
        v_i = i // 3
        phase = v_i * 0.8
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"rotation"),
            kf(2.0,  0.3*math.sin(phase),         0,0,"rotation"),
            kf(4.0,  0.3*math.sin(phase+math.pi/2), 0,0,"rotation"),
            kf(6.0,  0.3*math.sin(phase+math.pi),   0,0,"rotation"),
            kf(8.0,  0,0,0,"rotation"),
            kf(0.0,  0,0,0,"position"), kf(2.0,0,0,0,"position"), kf(4.0,0,0,0,"position"),
            kf(6.0,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"), kf(2.0,1,1,1,"scale"), kf(4.0,1,1,1,"scale"),
            kf(6.0,1,1,1,"scale"), kf(L,1,1,1,"scale"),
        ]}

    # SPINE SPINOUS PROCESSES
    for i, buid in enumerate(sp_bids):
        phase = i * 0.9
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1.0,1.0,1.0,"scale"),
            kf(2.0,  1.0,1.0,1.0+0.015*math.sin(phase),"scale"),
            kf(4.0,  1.0,1.0,1.0+0.015*math.sin(phase+math.pi/2),"scale"),
            kf(6.0,  1.0,1.0,1.0+0.015*math.sin(phase+math.pi),"scale"),
            kf(8.0,  1.0,1.0,1.0,"scale"),
            kf(0.0,  0,0,0,"position"), kf(2.0,0,0,0,"position"), kf(4.0,0,0,0,"position"),
            kf(6.0,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(2.0,0,0,0,"rotation"), kf(4.0,0,0,0,"rotation"),
            kf(6.0,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # WING BONE FRAGMENTS sway ±0.5°
    for i, buid in enumerate(wing_bids):
        phase = i * 1.1
        side = -1 if i < 2 else 1
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0, 0, 0,"rotation"),
            kf(2.0,  0, 0, side*0.5*math.sin(phase),"rotation"),
            kf(4.0,  0, 0, side*0.5*math.sin(phase+math.pi/2),"rotation"),
            kf(6.0,  0, 0, side*0.5*math.sin(phase+math.pi),"rotation"),
            kf(8.0,  0, 0, 0,"rotation"),
            kf(0.0,  0, 0, 0,"position"),
            kf(2.0,  0, 0.04*math.sin(phase), 0,"position"),
            kf(4.0,  0, 0.04*math.cos(phase), 0,"position"),
            kf(6.0,  0, -0.04*math.sin(phase), 0,"position"),
            kf(8.0,  0, 0, 0,"position"),
            kf(0.0,  1,1,1,"scale"), kf(2.0,1,1,1,"scale"), kf(4.0,1,1,1,"scale"),
            kf(6.0,1,1,1,"scale"), kf(L,1,1,1,"scale"),
        ]}

    # WING DETAIL FRAGMENTS
    for i, buid in enumerate(wd_bids):
        phase = i * 0.9
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0, 0, 0,"rotation"),
            kf(2.0,  0.4*math.sin(phase), 0.4*math.cos(phase), 0,"rotation"),
            kf(4.0,  0.4*math.sin(phase+math.pi/2), 0.4*math.cos(phase+math.pi/2), 0,"rotation"),
            kf(6.0,  0.4*math.sin(phase+math.pi), 0.4*math.cos(phase+math.pi), 0,"rotation"),
            kf(8.0,  0, 0, 0,"rotation"),
            kf(0.0,  0,0,0,"position"), kf(2.0,0,0.02*math.sin(phase),0,"position"),
            kf(4.0,0,0.02*math.cos(phase),0,"position"), kf(6.0,0,-0.02*math.sin(phase),0,"position"),
            kf(L,0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"), kf(2.0,1,1,1,"scale"), kf(4.0,1,1,1,"scale"),
            kf(6.0,1,1,1,"scale"), kf(L,1,1,1,"scale"),
        ]}

    # SKULL FRAGMENTS slowly drift at their heights, each rotating on a unique axis
    for i, buid in enumerate(skull_bids):
        phase = i * 1.2
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0, 0, 0,"rotation"),
            kf(2.0,  10*math.sin(phase),     10*math.cos(phase),     5*math.sin(phase),"rotation"),
            kf(4.0,  10*math.sin(phase+math.pi/2), 10*math.cos(phase+math.pi/2), 5*math.sin(phase+math.pi/2),"rotation"),
            kf(6.0,  10*math.sin(phase+math.pi),   10*math.cos(phase+math.pi),   5*math.sin(phase+math.pi),"rotation"),
            kf(8.0,  0, 0, 0,"rotation"),
            kf(0.0,  0, 0, 0,"position"),
            kf(2.0,  0.08*math.sin(phase), 0.10*math.sin(phase+0.5), 0.05*math.cos(phase),"position"),
            kf(4.0,  0.08*math.cos(phase), 0.10*math.cos(phase+0.5), -0.05*math.sin(phase),"position"),
            kf(6.0,  -0.08*math.sin(phase), -0.10*math.sin(phase+0.5), 0.05*math.cos(phase),"position"),
            kf(8.0,  0, 0, 0,"position"),
            kf(0.0,  1,1,1,"scale"), kf(2.0,1,1,1,"scale"), kf(4.0,1,1,1,"scale"),
            kf(6.0,1,1,1,"scale"), kf(L,1,1,1,"scale"),
        ]}

    # SKULL SUTURE PIECES — drift with skull
    for i, buid in enumerate(ss_bids):
        phase = i * 1.5
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0, 0, 0,"rotation"),
            kf(2.0,  6*math.sin(phase), 6*math.cos(phase), 0,"rotation"),
            kf(4.0,  6*math.sin(phase+math.pi/2), 6*math.cos(phase+math.pi/2), 0,"rotation"),
            kf(6.0,  6*math.sin(phase+math.pi), 6*math.cos(phase+math.pi), 0,"rotation"),
            kf(8.0,  0, 0, 0,"rotation"),
            kf(0.0,  0,0,0,"position"),
            kf(2.0,  0.04*math.sin(phase),0.05*math.cos(phase),0,"position"),
            kf(4.0,  0.04*math.cos(phase),0.05*math.sin(phase),0,"position"),
            kf(6.0,  -0.04*math.sin(phase),-0.05*math.cos(phase),0,"position"),
            kf(L,    0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"), kf(2.0,1,1,1,"scale"), kf(4.0,1,1,1,"scale"),
            kf(6.0,1,1,1,"scale"), kf(L,1,1,1,"scale"),
        ]}

    # GROUND CRACKS — slow flicker (scale)
    for i, buid in enumerate(gc_bids):
        phase = i * 0.5
        base_rot = (i / 6.0) * 360.0
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1.0,1.0,1.0,"scale"),
            kf(2.0,  1.0,1.0,1.0+0.03*math.sin(phase),"scale"),
            kf(4.0,  1.0,1.0,1.0+0.03*math.sin(phase+math.pi/2),"scale"),
            kf(6.0,  1.0,1.0,1.0+0.03*math.sin(phase+math.pi),"scale"),
            kf(8.0,  1.0,1.0,1.0,"scale"),
            kf(0.0,  0,0,0,"position"), kf(2.0,0,0,0,"position"), kf(4.0,0,0,0,"position"),
            kf(6.0,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  0, base_rot, 0,"rotation"),
            kf(2.0,  0, base_rot, 0,"rotation"),
            kf(4.0,  0, base_rot, 0,"rotation"),
            kf(6.0,  0, base_rot, 0,"rotation"),
            kf(8.0,  0, base_rot, 0,"rotation"),
        ]}

    return animators


def build_dissipate():
    """dissipate once, 1.2s:
    - skull fragments drift upward and vanish
    - wing bones lift and dissolve
    - ribs fold inward and sink
    - spine sinks
    - sternum sinks last
    - ground cracks contract
    """
    animators = {}
    L = 1.2

    # SKULL FRAGMENTS drift upward and vanish first (start at 0.0)
    for i, buid in enumerate(skull_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.10, 1,1,1,"scale"),
            kf(0.30, 0.7,0.7,0.7,"scale"),
            kf(0.5,  0.3,0.3,0.3,"scale"),
            kf(L,    0,0,0,"scale"),
            kf(0.0,  0, 0, 0,"position"),
            kf(0.15, 0, 0.5+i*0.1, 0,"position"),
            kf(0.4,  0, 1.5+i*0.2, 0,"position"),
            kf(0.7,  0, 3.0+i*0.3, 0,"position"),
            kf(L,    0, 5.0+i*0.4, 0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.4,  20*(i%3-1), 30, 10,"rotation"),
            kf(L,    60*(i%3-1), 90, 30,"rotation"),
        ]}

    # SKULL SUTURE PIECES — drift up with skull
    for i, buid in enumerate(ss_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.10, 1,1,1,"scale"),
            kf(0.30, 0.5,0.5,0.5,"scale"),
            kf(0.5,  0.2,0.2,0.2,"scale"),
            kf(L,    0,0,0,"scale"),
            kf(0.0,  0, 0, 0,"position"),
            kf(0.15, 0, 0.4+i*0.05, 0,"position"),
            kf(0.4,  0, 1.2+i*0.1, 0,"position"),
            kf(0.7,  0, 2.8+i*0.15, 0,"position"),
            kf(L,    0, 4.5+i*0.2, 0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.4,  10*(i%3-1), 30, 5,"rotation"),
            kf(L,    40*(i%3-1), 90, 20,"rotation"),
        ]}

    # WING BONE FRAGMENTS lift and dissolve at 0.2s
    for i, buid in enumerate(wing_bids):
        side = -1 if i < 2 else 1
        d = 0.20 + i * 0.04
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(d,    1,1,1,"scale"),
            kf(d+0.20, 0.6,0.6,0.6,"scale"),
            kf(0.7,  0.3,0.3,0.3,"scale"),
            kf(L,    0,0,0,"scale"),
            kf(0.0,  0, 0, 0,"position"),
            kf(d,    0, 0, 0,"position"),
            kf(d+0.20, side*0.4, 1.0, 0,"position"),
            kf(0.7,  side*1.0, 2.5, 0,"position"),
            kf(L,    side*1.8, 4.5, 0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(d+0.2, 0, 0, side*15,"rotation"),
            kf(L,    0, 0, side*45,"rotation"),
        ]}

    # WING DETAIL FRAGMENTS — drift up with wings
    for i, buid in enumerate(wd_bids):
        side = -1 if i < 4 else 1
        d = 0.18 + i * 0.02
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(d,    1,1,1,"scale"),
            kf(d+0.18, 0.6,0.6,0.6,"scale"),
            kf(0.7,  0.2,0.2,0.2,"scale"),
            kf(L,    0,0,0,"scale"),
            kf(0.0,  0, 0, 0,"position"),
            kf(d,    0, 0, 0,"position"),
            kf(d+0.18, side*0.5, 1.2, 0,"position"),
            kf(0.7,  side*1.2, 2.8, 0,"position"),
            kf(L,    side*2.0, 5.0, 0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.4,  20, 30, side*15,"rotation"),
            kf(L,    60, 90, side*45,"rotation"),
        ]}

    # RIBS fold inward and sink at 0.4s
    for i, buid in enumerate(rib_bids):
        side = -1 if i < len(rib_bids)//2 else 1
        d = 0.40 + i * 0.005
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(d,    1,1,1,"scale"),
            kf(d+0.20, 0.7,0.7,0.7,"scale"),
            kf(0.9,  0.3,0.3,0.3,"scale"),
            kf(L,    0,0,0,"scale"),
            kf(0.0,  0, 0, 0,"position"),
            kf(d,    0, 0, 0,"position"),
            kf(d+0.20, -side*0.5, -0.4, 0,"position"),
            kf(0.9,  -side*1.0, -1.2, 0,"position"),
            kf(L,    -side*1.5, -2.5, 0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(d+0.20, 0,0,side*5,"rotation"),
            kf(L,    0,0,side*15,"rotation"),
        ]}

    # RIB TIP CAPS — fold with ribs
    for i, buid in enumerate(rt_bids):
        side = -1 if i < 4 else 1
        d = 0.42 + i * 0.015
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(d,    1,1,1,"scale"),
            kf(d+0.18, 0.6,0.6,0.6,"scale"),
            kf(0.9,  0.2,0.2,0.2,"scale"),
            kf(L,    0,0,0,"scale"),
            kf(0.0,  0, 0, 0,"position"),
            kf(d,    0, 0, 0,"position"),
            kf(d+0.20, -side*0.4, -0.5, 0,"position"),
            kf(0.9,  -side*0.9, -1.4, 0,"position"),
            kf(L,    -side*1.4, -2.8, 0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.5,  10,10,side*8,"rotation"),
            kf(L,    30,30,side*20,"rotation"),
        ]}

    # CRACKED RIB ACCENTS
    for i, buid in enumerate(cra_bids):
        d = 0.44 + i * 0.03
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(d,    1,1,1,"scale"),
            kf(d+0.15, 0.5,0.5,0.5,"scale"),
            kf(0.9,  0.2,0.2,0.2,"scale"),
            kf(L,    0,0,0,"scale"),
            kf(0.0,  0, 0, 0,"position"),
            kf(d,    0, 0, 0,"position"),
            kf(d+0.18, 0, -0.5, 0.3,"position"),
            kf(0.9,  0, -1.5, 0.6,"position"),
            kf(L,    0, -3.0, 1.0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.5,15,30,15,"rotation"), kf(L,45,90,45,"rotation"),
        ]}

    # SPINE VERTEBRAE sink at 0.5s
    for i, buid in enumerate(spine_bids):
        v_i = i // 3
        d = 0.50 + v_i * 0.04
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(d,    1,1,1,"scale"),
            kf(d+0.15, 0.7,0.7,0.7,"scale"),
            kf(1.0,  0.3,0.3,0.3,"scale"),
            kf(L,    0,0,0,"scale"),
            kf(0.0,  0, 0, 0,"position"),
            kf(d,    0, 0, 0,"position"),
            kf(d+0.15, 0, -0.6, 0,"position"),
            kf(1.0,  0, -1.8, 0,"position"),
            kf(L,    0, -3.5, 0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.5,  3,0,2,"rotation"),
            kf(L,    8,0,5,"rotation"),
        ]}

    # SPINE SPINOUS PROCESSES — sink with vertebrae
    for i, buid in enumerate(sp_bids):
        d = 0.52 + i * 0.04
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(d,    1,1,1,"scale"),
            kf(d+0.15, 0.6,0.6,0.6,"scale"),
            kf(1.0,  0.2,0.2,0.2,"scale"),
            kf(L,    0,0,0,"scale"),
            kf(0.0,  0, 0, 0,"position"),
            kf(d,    0, 0, 0,"position"),
            kf(d+0.15, 0, -0.6, -0.2,"position"),
            kf(1.0,  0, -1.8, -0.5,"position"),
            kf(L,    0, -3.5, -0.8,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.5,3,0,2,"rotation"), kf(L,8,0,5,"rotation"),
        ]}

    # STERNUM sinks LAST at 0.7s
    for i, buid in enumerate(stern_bids):
        d = 0.70 + i * 0.03
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(d,    1,1,1,"scale"),
            kf(d+0.15, 0.7,0.7,0.7,"scale"),
            kf(1.05, 0.3,0.3,0.3,"scale"),
            kf(L,    0,0,0,"scale"),
            kf(0.0,  0, 0, 0,"position"),
            kf(d,    0, 0, 0,"position"),
            kf(d+0.15, 0, -0.6, 0,"position"),
            kf(1.0,  0, -2.0, 0,"position"),
            kf(L,    0, -3.5, 0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.7,  0,0,0,"rotation"),
            kf(L,    0,0,0,"rotation"),
        ]}

    # STERNUM JOINTS sink with sternum
    for i, buid in enumerate(sj_bids):
        d = 0.72 + i * 0.03
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(d,    1,1,1,"scale"),
            kf(d+0.15, 0.6,0.6,0.6,"scale"),
            kf(1.05, 0.3,0.3,0.3,"scale"),
            kf(L,    0,0,0,"scale"),
            kf(0.0,  0, 0, 0,"position"),
            kf(d,    0, 0, 0,"position"),
            kf(d+0.15, 0, -0.6, 0,"position"),
            kf(1.0,  0, -2.0, 0,"position"),
            kf(L,    0, -3.5, 0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.7,0,0,0,"rotation"), kf(L,0,0,0,"rotation"),
        ]}

    # GROUND CRACKS contract
    for i, buid in enumerate(gc_bids):
        base_rot = (i / 6.0) * 360.0
        d = 0.30 + i * 0.02
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(d,    1,1,1,"scale"),
            kf(d+0.20, 0.7,1,0.7,"scale"),
            kf(0.9,  0.3,1,0.3,"scale"),
            kf(L,    0,1,0,"scale"),
            kf(0.0,  0,0,0,"position"), kf(0.4,0,0,0,"position"), kf(0.8,0,0,0,"position"),
            kf(1.0,0,0,0,"position"), kf(L,0,0,0,"position"),
            kf(0.0,  0, base_rot, 0,"rotation"),
            kf(0.4,  0, base_rot, 0,"rotation"),
            kf(0.8,  0, base_rot, 0,"rotation"),
            kf(1.0,  0, base_rot, 0,"rotation"),
            kf(L,    0, base_rot, 0,"rotation"),
        ]}

    return animators

# ── Build model ──────────────────────────────────────────────────────────────
spawn_anim = {
    "uuid": aid(1), "name": "animation.fallen_seraph_skeleton.spawn",
    "loop": "once", "length": 1.6, "snapping": 24,
    "selected": False, "saved": False, "animators": build_spawn()
}
idle_anim = {
    "uuid": aid(2), "name": "animation.fallen_seraph_skeleton.idle",
    "loop": "loop", "length": 8.0, "snapping": 24,
    "selected": False, "saved": False, "animators": build_idle()
}
dissipate_anim = {
    "uuid": aid(3), "name": "animation.fallen_seraph_skeleton.dissipate",
    "loop": "once", "length": 1.2, "snapping": 24,
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
    {"id":"0","name":"fallen_seraph_skeleton_tex","relative_path":"../textures/fallen_seraph_skeleton_tex.png",
     "folder":"devilsdream","namespace":"","visible":True,"mode":"bitmap","saved":False,
     "uuid":tid(1),"source":f"data:image/png;base64,{b64_0}"},
    {"id":"1","name":"fallen_seraph_skeleton_tex1","relative_path":"../textures/fallen_seraph_skeleton_tex1.png",
     "folder":"devilsdream","namespace":"","visible":True,"mode":"bitmap","saved":False,
     "uuid":tid(2),"source":f"data:image/png;base64,{b64_1}"},
]

model = {
    "meta": {"format_version":"4.10","model_format":"free","box_uv":False},
    "name": "fallen_seraph_skeleton",
    "geometry": "geometry.fallen_seraph_skeleton",
    "resolution": {"width":64,"height":64},
    "elements": elements,
    "outliner": outliner,
    "textures": textures,
    "animations": [spawn_anim, idle_anim, dissipate_anim]
}

out_path = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/fallen_seraph_skeleton.bbmodel"
with open(out_path, "w", encoding="utf-8") as f:
    json.dump(model, f, separators=(',', ':'))

size = os.path.getsize(out_path)
print(f"Written: {out_path}")
print(f"Elements: {len(elements)} | Bones: {len(bones)}")
print(f"Size: {size:,} bytes ({size/1024:.1f} KB)")
pass_fail = "PASS" if size >= 300*1024 else "FAIL"
print(f"{pass_fail}: {'>=300KB' if size>=300*1024 else f'only {size//1024}KB'}")
