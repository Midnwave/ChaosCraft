"""
Build silver_mirror_portal.bbmodel — MODEL 19, "The Reflection"
A tall standing mirror: 2 vertical pillars (4 stacked segments each), curved arch (5 slabs),
wide base (3 slabs), 3 mirror surface slabs, 4 ornament clusters, 3 base feet, 2 ripple layers,
crescent crown, plus engraving + carved details to push past 80 elements / 55 bones.
"""
import json, math, os

b64_0 = open("D:/CC/ChaosCraft/tools/gen_textures/m19_b64.txt").read().strip()
b64_1 = open("D:/CC/ChaosCraft/tools/gen_textures/m19_tex1_b64.txt").read().strip()

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

# Bone group lists for animation
left_pillar_bids   = []   # 4 segments
right_pillar_bids  = []   # 4 segments
arch_bids          = []   # 5 slabs
base_bids          = []   # 3 slabs
ornament_bids      = []   # 4 clusters
mirror_bids        = []   # 3 surface slabs
crescent_bids      = []   # 2 angled slabs
foot_bids          = []   # 3 base feet
ripple_bids        = []   # 2 ripple layers
engraving_bids     = []   # 8 engravings
carved_pillar_bids = []   # 16 carved bits (2 per segment, 8 per pillar)
keystone_bids      = []   # 3 keystone bits at arch apex
ornament_inner_bids= []   # 8 inner ornament pieces (2 per cluster)
crescent_orn_bids  = []   # 2 crescent decorations
mirror_glow_bids   = []   # 4 inner-frame purple glow strips

# === 1. LEFT PILLAR (4 stacked segments) ===
# Each segment 1.5 wide x 0.6 deep x 2.5 tall, X centered at -2.5
for i in range(4):
    y0 = i * 2.5
    y1 = y0 + 2.5
    name = f"pillar_L{i+1}"
    fr = [-3.25, y0, -0.3]
    to = [-1.75, y1, 0.3]
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [-2.5, y0, 0], [eid(ei)]); bones.append(b)
    left_pillar_bids.append(bid(bi)); ei+=1; bi+=1

# === 2. RIGHT PILLAR (4 stacked segments) ===
for i in range(4):
    y0 = i * 2.5
    y1 = y0 + 2.5
    name = f"pillar_R{i+1}"
    fr = [1.75, y0, -0.3]
    to = [3.25, y1, 0.3]
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [2.5, y0, 0], [eid(ei)]); bones.append(b)
    right_pillar_bids.append(bid(bi)); ei+=1; bi+=1

# === 3. CURVED ARCH (5 flat slabs in a semicircle from pillar top L to pillar top R) ===
# Pillar tops at y=10. Arch peaks at y=12. Arch spans x = -3.25 to +3.25.
# 5 slab positions along the arch: angles spread from 180° (left) to 0° (right) through 90° (top)
arch_cx, arch_cy = 0.0, 10.0
arch_r = 3.25
for i in range(5):
    # angle from 180 to 0 (left to right via top)
    angle = math.radians(180 - i * 45)  # 180, 135, 90, 45, 0
    px_ = arch_cx + arch_r * math.cos(angle)
    py_ = arch_cy + 2.0 * math.sin(angle)  # squished arch — 2 unit tall
    # slab: 1.4 wide, 0.6 thick, 0.6 deep, rotated to follow tangent
    name = f"arch_{i+1}"
    fr = [px_ - 0.7, py_ - 0.3, -0.3]
    to = [px_ + 0.7, py_ + 0.3, 0.3]
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    rot_z = (90 - (180 - i * 45))  # tangent-ish: 90 at left=180→-90, top=90→0, right=0→90
    b = bone(bi, name, [px_, py_, 0], [eid(ei)], rot=[0, 0, rot_z]); bones.append(b)
    arch_bids.append(bid(bi)); ei+=1; bi+=1

# === 4. WIDE BASE (3 flat slabs at y=0) ===
# Base extends past pillars: total width 8 split into 3 slabs
base_defs = [
    ("base_L", [-4.0, -0.4, -0.6], [-1.5, 0.0, 0.6]),
    ("base_C", [-1.5, -0.4, -0.6], [ 1.5, 0.0, 0.6]),
    ("base_R", [ 1.5, -0.4, -0.6], [ 4.0, 0.0, 0.6]),
]
for name, fr, to in base_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [(fr[0]+to[0])/2, fr[1], 0], [eid(ei)]); bones.append(b)
    base_bids.append(bid(bi)); ei+=1; bi+=1

# === 5. ORNAMENT CLUSTERS (4 small decorative cubes — 2 per pillar) ===
# Heights at y=3.5 and y=7.5 on each pillar, projecting slightly outward and forward
ornament_defs = [
    ("orn_L_low",  [-3.55, 3.2, -0.4], [-3.15, 4.0, 0.4]),
    ("orn_L_high", [-3.55, 7.2, -0.4], [-3.15, 8.0, 0.4]),
    ("orn_R_low",  [ 3.15, 3.2, -0.4], [ 3.55, 4.0, 0.4]),
    ("orn_R_high", [ 3.15, 7.2, -0.4], [ 3.55, 8.0, 0.4]),
]
for name, fr, to in ornament_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [(fr[0]+to[0])/2, (fr[1]+to[1])/2, 0], [eid(ei)]); bones.append(b)
    ornament_bids.append(bid(bi)); ei+=1; bi+=1

# === 6. MIRROR SURFACE (3 large flat slabs filling interior, tex1) ===
# Interior is x: -1.75..+1.75, y: 0..10, z: 0 plane
mirror_defs = [
    ("mirror_low", [-1.75, 0.2, -0.05], [1.75, 3.5, 0.05]),
    ("mirror_mid", [-1.75, 3.5, -0.05], [1.75, 6.8, 0.05]),
    ("mirror_top", [-1.75, 6.8, -0.05], [1.75,10.0, 0.05]),
]
for name, fr, to in mirror_defs:
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    b = bone(bi, name, [0, (fr[1]+to[1])/2, 0], [eid(ei)]); bones.append(b)
    mirror_bids.append(bid(bi)); ei+=1; bi+=1

# === 7. CRESCENT CROWN (2 angled overlapping slabs at top of arch) ===
# Apex of arch is at (0, 12). Crescent two slabs angled into a moon shape.
crescent_defs = [
    ("crescent_L", [-1.6, 11.8, -0.2], [0.4, 13.2, 0.2], [0, 0,  35]),
    ("crescent_R", [-0.4, 11.8, -0.2], [1.6, 13.2, 0.2], [0, 0, -35]),
]
for name, fr, to, rot in crescent_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [(fr[0]+to[0])/2, 12.5, 0], [eid(ei)], rot=rot); bones.append(b)
    crescent_bids.append(bid(bi)); ei+=1; bi+=1

# === 8. BASE FEET (3 flat slab feet radiating outward) ===
foot_defs = [
    ("foot_L",   [-5.0, -0.3, -0.4], [-4.0, 0.0, 0.4],  [0, 0, -10]),
    ("foot_C",   [-0.6, -0.5, -1.4], [ 0.6, -0.1, -0.6],[0, 0,  0]),
    ("foot_R",   [ 4.0, -0.3, -0.4], [ 5.0, 0.0, 0.4],  [0, 0,  10]),
]
for name, fr, to, rot in foot_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [(fr[0]+to[0])/2, fr[1], (fr[2]+to[2])/2], [eid(ei)], rot=rot); bones.append(b)
    foot_bids.append(bid(bi)); ei+=1; bi+=1

# === 9. REFLECTION RIPPLE LAYERS (2 thin slabs inside mirror at slight Z offset, tex1) ===
ripple_defs = [
    ("ripple_close", [-1.55, 1.0, 0.06], [1.55, 9.0, 0.10]),
    ("ripple_far",   [-1.35, 2.0, 0.11], [1.35, 8.0, 0.15]),
]
for name, fr, to in ripple_defs:
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    b = bone(bi, name, [0, (fr[1]+to[1])/2, fr[2]], [eid(ei)]); bones.append(b)
    ripple_bids.append(bid(bi)); ei+=1; bi+=1

# === 10. FRAME ENGRAVINGS (8 thin horizontal strips on pillars) ===
# 4 per pillar, evenly spaced at y=2,4,6,8
for side_idx, side_x in enumerate([-2.5, 2.5]):
    for j, eng_y in enumerate([1.8, 4.3, 6.8, 9.3]):
        name = f"engr_{'L' if side_idx==0 else 'R'}_{j+1}"
        fr = [side_x - 0.78, eng_y - 0.08, 0.30]
        to = [side_x + 0.78, eng_y + 0.08, 0.36]
        e = elem(ei, name, fr, to, UV0); elements.append(e)
        b = bone(bi, name, [side_x, eng_y, 0.33], [eid(ei)]); bones.append(b)
        engraving_bids.append(bid(bi)); ei+=1; bi+=1

# === 11. CARVED PILLAR DETAILS (16 small carved-recess pieces — 2 per segment, 8 per pillar) ===
# Each segment gets 2 small recessed decorative slabs on its front face
for side_idx, side_x in enumerate([-2.5, 2.5]):
    for seg in range(4):
        for piece in range(2):
            seg_y = seg * 2.5
            piece_y = seg_y + 0.6 + piece * 1.1
            name = f"carved_{'L' if side_idx==0 else 'R'}{seg+1}_{piece+1}"
            fr = [side_x - 0.45, piece_y, 0.30]
            to = [side_x + 0.45, piece_y + 0.5, 0.36]
            e = elem(ei, name, fr, to, UV0); elements.append(e)
            b = bone(bi, name, [side_x, piece_y + 0.25, 0.33], [eid(ei)]); bones.append(b)
            carved_pillar_bids.append(bid(bi)); ei+=1; bi+=1

# === 12. ARCH KEYSTONE (3 inner pieces at arch apex) ===
keystone_defs = [
    ("keystone_C",  [-0.4, 11.5, -0.35], [0.4, 12.3, 0.35]),
    ("keystone_L",  [-1.0, 11.0, -0.30], [-0.4, 11.7, 0.30]),
    ("keystone_R",  [ 0.4, 11.0, -0.30], [ 1.0, 11.7, 0.30]),
]
for name, fr, to in keystone_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [(fr[0]+to[0])/2, (fr[1]+to[1])/2, 0], [eid(ei)]); bones.append(b)
    keystone_bids.append(bid(bi)); ei+=1; bi+=1

# === 13. ORNAMENT INNER PIECES (2 per ornament, 8 total) ===
for orn_idx, (cx_orn, cy_orn) in enumerate([(-3.35, 3.6), (-3.35, 7.6), (3.35, 3.6), (3.35, 7.6)]):
    for sub in range(2):
        sub_y = cy_orn + (-0.3 if sub == 0 else 0.3)
        name = f"orninner_{orn_idx+1}_{sub+1}"
        fr = [cx_orn - 0.18, sub_y - 0.15, -0.18]
        to = [cx_orn + 0.18, sub_y + 0.15,  0.18]
        e = elem(ei, name, fr, to, UV0); elements.append(e)
        b = bone(bi, name, [cx_orn, sub_y, 0], [eid(ei)]); bones.append(b)
        ornament_inner_bids.append(bid(bi)); ei+=1; bi+=1

# === 14. CRESCENT DECORATIONS (2 small dots on the crescent tips) ===
crescent_orn_defs = [
    ("crescent_dot_L", [-1.45, 12.4, -0.18], [-1.05, 12.8, 0.18]),
    ("crescent_dot_R", [ 1.05, 12.4, -0.18], [ 1.45, 12.8, 0.18]),
]
for name, fr, to in crescent_orn_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [(fr[0]+to[0])/2, (fr[1]+to[1])/2, 0], [eid(ei)]); bones.append(b)
    crescent_orn_bids.append(bid(bi)); ei+=1; bi+=1

# === 14b. PILLAR SIDE TRIM (16 thin slabs — 2 per segment per pillar, on outer side face) ===
pillar_side_bids = []
for side_idx, side_x in enumerate([-3.30, 3.30]):
    for seg in range(4):
        for piece in range(2):
            seg_y = seg * 2.5
            piece_y = seg_y + 0.4 + piece * 1.2
            name = f"pside_{'L' if side_idx==0 else 'R'}{seg+1}_{piece+1}"
            sign = -1 if side_idx == 0 else 1
            fr = [side_x + sign*-0.05 - 0.06, piece_y, -0.18]
            to = [side_x + sign*-0.05 + 0.06, piece_y + 0.7, 0.18]
            # ensure fr<to
            fr_x, to_x = sorted([fr[0], to[0]])
            fr[0], to[0] = fr_x, to_x
            e = elem(ei, name, fr, to, UV0); elements.append(e)
            b = bone(bi, name, [side_x, piece_y + 0.35, 0], [eid(ei)]); bones.append(b)
            pillar_side_bids.append(bid(bi)); ei+=1; bi+=1

# === 15. MIRROR INNER GLOW STRIPS (4 thin purple emissive bars on the inner frame edges, tex1) ===
glow_defs = [
    ("glow_left",   [-1.85, 0.2, -0.08], [-1.75, 10.0, 0.08]),  # left inner edge
    ("glow_right",  [ 1.75, 0.2, -0.08], [ 1.85, 10.0, 0.08]),  # right inner edge
    ("glow_top",    [-1.75,10.0, -0.08], [ 1.75, 10.15, 0.08]), # top inner edge
    ("glow_bottom", [-1.75, 0.05,-0.08], [ 1.75, 0.20, 0.08]),  # bottom inner edge
]
for name, fr, to in glow_defs:
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    b = bone(bi, name, [(fr[0]+to[0])/2, (fr[1]+to[1])/2, 0], [eid(ei)]); bones.append(b)
    mirror_glow_bids.append(bid(bi)); ei+=1; bi+=1

print(f"Elements: {len(elements)}, Bones: {len(bones)}")
assert len(elements) >= 80, f"need >=80 elements, got {len(elements)}"
assert len(bones)    >= 55, f"need >=55 bones, got {len(bones)}"

all_bids = (left_pillar_bids + right_pillar_bids + arch_bids + base_bids +
            ornament_bids + mirror_bids + crescent_bids + foot_bids +
            ripple_bids + engraving_bids + carved_pillar_bids + keystone_bids +
            ornament_inner_bids + crescent_orn_bids + pillar_side_bids + mirror_glow_bids)

bone_name_map = {b["uuid"]: b["name"] for b in bones}

# ════════════════════════════════════════════════════════════════════════════
# ANIMATIONS
# ════════════════════════════════════════════════════════════════════════════

def build_spawn():
    """spawn once 1.5s: base→pillars→arch→ornaments→mirror→ripple→crescent."""
    a = {}

    # Base slabs appear at 0.2s
    for i, buid in enumerate(base_bids):
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"), kf(0.18, 0,0,0,"scale"),
            kf(0.22, 1,1,1,"scale"), kf(0.55, 1,1,1,"scale"),
            kf(1.0,  1,1,1,"scale"), kf(1.5,  1,1,1,"scale"),
            kf(0.0,  0,-1,0,"position"), kf(0.18, 0,-1,0,"position"),
            kf(0.22, 0,0,0,"position"), kf(0.5,  0,0,0,"position"),
            kf(1.0,  0,0,0,"position"), kf(1.5,  0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.5,  0,0,0,"rotation"),
            kf(1.0,  0,0,0,"rotation"), kf(1.5,  0,0,0,"rotation"),
        ]}

    # Base feet — same time as base
    for i, buid in enumerate(foot_bids):
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"), kf(0.20, 0,0,0,"scale"),
            kf(0.30, 1,1,1,"scale"), kf(0.6,  1,1,1,"scale"),
            kf(1.0,  1,1,1,"scale"), kf(1.5,  1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"), kf(0.20, 0,0,0,"position"),
            kf(0.30, 0,0,0,"position"), kf(1.0,  0,0,0,"position"),
            kf(1.5,  0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.5,  0,0,0,"rotation"),
            kf(1.0,  0,0,0,"rotation"), kf(1.5,  0,0,0,"rotation"),
        ]}

    # Pillars extend upward segment by segment at 0.4s, stagger 0.04 per segment
    def pillar_keys(buid, seg, side_label):
        delay = 0.40 + seg * 0.04
        return {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,    0,0,0,"scale"), kf(delay,      0,0,0,"scale"),
            kf(delay+0.05, 1,1,1,"scale"), kf(delay+0.15, 1,1,1,"scale"),
            kf(1.0,    1,1,1,"scale"), kf(1.5,    1,1,1,"scale"),
            kf(0.0,    0,-2,0,"position"), kf(delay,  0,-2,0,"position"),
            kf(delay+0.05, 0,0,0,"position"), kf(1.0, 0,0,0,"position"),
            kf(1.5,    0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.5,  0,0,0,"rotation"),
            kf(1.0,  0,0,0,"rotation"), kf(1.5,  0,0,0,"rotation"),
        ]}
    for i, buid in enumerate(left_pillar_bids):  a[buid] = pillar_keys(buid, i, "L")
    for i, buid in enumerate(right_pillar_bids): a[buid] = pillar_keys(buid, i, "R")

    # Carved pillar detail — appears with its parent segment + small extra delay
    for i, buid in enumerate(carved_pillar_bids):
        seg = (i // 2) % 4
        delay = 0.45 + seg * 0.04 + (i % 2) * 0.02
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,    0,0,0,"scale"), kf(delay,      0,0,0,"scale"),
            kf(delay+0.05, 1,1,1,"scale"), kf(0.9,    1,1,1,"scale"),
            kf(1.2,    1,1,1,"scale"), kf(1.5,    1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"), kf(delay,0,0,0,"position"),
            kf(delay+0.05, 0,0,0,"position"), kf(1.0,0,0,0,"position"),
            kf(1.5,  0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.5,  0,0,0,"rotation"),
            kf(1.0,  0,0,0,"rotation"), kf(1.5,  0,0,0,"rotation"),
        ]}

    # Pillar side trim — same window as carved
    for i, buid in enumerate(pillar_side_bids):
        seg = (i // 2) % 4
        delay = 0.48 + seg * 0.04 + (i % 2) * 0.02
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,    0,0,0,"scale"), kf(delay,      0,0,0,"scale"),
            kf(delay+0.05, 1,1,1,"scale"), kf(0.9,    1,1,1,"scale"),
            kf(1.2,    1,1,1,"scale"), kf(1.5,    1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"), kf(delay,0,0,0,"position"),
            kf(delay+0.05, 0,0,0,"position"), kf(1.0,0,0,0,"position"),
            kf(1.5,  0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.5,  0,0,0,"rotation"),
            kf(1.0,  0,0,0,"rotation"), kf(1.5,  0,0,0,"rotation"),
        ]}

    # Engravings — same window
    for i, buid in enumerate(engraving_bids):
        delay = 0.55 + (i % 4) * 0.03
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,    0,0,0,"scale"), kf(delay,      0,0,0,"scale"),
            kf(delay+0.05, 1,1,1,"scale"), kf(0.9,    1,1,1,"scale"),
            kf(1.2,    1,1,1,"scale"), kf(1.5,    1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"), kf(delay,0,0,0,"position"),
            kf(delay+0.05, 0,0,0,"position"), kf(1.5,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.75, 0,0,0,"rotation"), kf(1.5,  0,0,0,"rotation"),
        ]}

    # Ornaments at 0.8s
    for i, buid in enumerate(ornament_bids):
        delay = 0.80 + i * 0.04
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,    0,0,0,"scale"), kf(delay,      0,0,0,"scale"),
            kf(delay+0.06, 1.2,1.2,1.2,"scale"), kf(delay+0.12, 1,1,1,"scale"),
            kf(1.2,    1,1,1,"scale"), kf(1.5,    1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"), kf(delay,0,0,0,"position"),
            kf(delay+0.05, 0,0,0,"position"), kf(1.5,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.75, 0,0,0,"rotation"), kf(1.5,  0,0,0,"rotation"),
        ]}

    # Ornament inner pieces just after
    for i, buid in enumerate(ornament_inner_bids):
        delay = 0.85 + i * 0.02
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,    0,0,0,"scale"), kf(delay,      0,0,0,"scale"),
            kf(delay+0.06, 1,1,1,"scale"), kf(1.2,    1,1,1,"scale"),
            kf(1.5,    1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"), kf(delay,0,0,0,"position"), kf(1.5,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.75, 0,0,0,"rotation"), kf(1.5,  0,0,0,"rotation"),
        ]}

    # Arch deploys at 0.9s
    for i, buid in enumerate(arch_bids):
        delay = 0.90 + i * 0.03
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,    0,0,0,"scale"), kf(delay,      0,0,0,"scale"),
            kf(delay+0.05, 1,1,1,"scale"), kf(1.2,    1,1,1,"scale"),
            kf(1.5,    1,1,1,"scale"),
            kf(0.0,  0,1,0,"position"), kf(delay,0,1,0,"position"),
            kf(delay+0.05, 0,0,0,"position"), kf(1.5,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.75, 0,0,0,"rotation"), kf(1.5,  0,0,0,"rotation"),
        ]}

    # Keystone with arch
    for i, buid in enumerate(keystone_bids):
        delay = 0.95 + i * 0.03
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,    0,0,0,"scale"), kf(delay,      0,0,0,"scale"),
            kf(delay+0.05, 1,1,1,"scale"), kf(1.2,    1,1,1,"scale"),
            kf(1.5,    1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"), kf(delay,0,0,0,"position"),
            kf(delay+0.05, 0,0,0,"position"), kf(1.5,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.75, 0,0,0,"rotation"), kf(1.5,  0,0,0,"rotation"),
        ]}

    # Mirror surface phases in from top to bottom at 1.1s
    # mirror_bids order: [low, mid, top]. Top first → low last.
    mirror_order = [mirror_bids[2], mirror_bids[1], mirror_bids[0]]
    for i, buid in enumerate(mirror_order):
        delay = 1.10 + i * 0.05
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,    0,0,0,"scale"), kf(delay,      0,0,0,"scale"),
            kf(delay+0.06, 1,1.1,1,"scale"), kf(delay+0.15, 1,1,1,"scale"),
            kf(1.5,    1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"), kf(delay,0,0,0,"position"),
            kf(delay+0.06, 0,0,0,"position"), kf(1.5,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.75, 0,0,0,"rotation"), kf(1.5,  0,0,0,"rotation"),
        ]}

    # Mirror inner glow strips at 1.15s
    for i, buid in enumerate(mirror_glow_bids):
        delay = 1.15 + i * 0.03
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,    0,0,0,"scale"), kf(delay,      0,0,0,"scale"),
            kf(delay+0.06, 1,1,1,"scale"), kf(1.5,    1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"), kf(delay,0,0,0,"position"), kf(1.5,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.75, 0,0,0,"rotation"), kf(1.5,  0,0,0,"rotation"),
        ]}

    # Crescent descends at 1.3s
    for i, buid in enumerate(crescent_bids):
        delay = 1.30 + i * 0.03
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,    0,0,0,"scale"), kf(delay,      0,0,0,"scale"),
            kf(delay+0.06, 1,1,1,"scale"), kf(1.5,    1,1,1,"scale"),
            kf(0.0,  0,2,0,"position"), kf(delay,0,2,0,"position"),
            kf(delay+0.06, 0,0,0,"position"), kf(1.5,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.75, 0,0,0,"rotation"), kf(1.5,  0,0,0,"rotation"),
        ]}

    for i, buid in enumerate(crescent_orn_bids):
        delay = 1.35 + i * 0.02
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,    0,0,0,"scale"), kf(delay,      0,0,0,"scale"),
            kf(delay+0.06, 1,1,1,"scale"), kf(1.5,    1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"), kf(delay,0,0,0,"position"), kf(1.5,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.75, 0,0,0,"rotation"), kf(1.5,  0,0,0,"rotation"),
        ]}

    # Reflection ripple layers at 1.4s
    for i, buid in enumerate(ripple_bids):
        delay = 1.40 + i * 0.04
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,    0,0,0,"scale"), kf(delay,      0,0,0,"scale"),
            kf(delay+0.05, 1,1,1,"scale"), kf(1.5,    1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"), kf(delay,0,0,0,"position"), kf(1.5,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.75, 0,0,0,"rotation"), kf(1.5,  0,0,0,"rotation"),
        ]}

    return a


def build_idle():
    """idle loop 8.0s: mirror ripples, frame sways, crescent rotates, ornaments pulse."""
    a = {}
    L = 8.0

    # Pillars sway imperceptibly Z-tilt sin
    for i, buid in enumerate(left_pillar_bids + right_pillar_bids):
        side = 1 if i >= 4 else -1
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0, 0, side*0.3, "rotation"),
            kf(2.0,  0, 0, side*0.6, "rotation"),
            kf(4.0,  0, 0, side*0.3, "rotation"),
            kf(6.0,  0, 0, side*0.0, "rotation"),
            kf(8.0,  0, 0, side*0.3, "rotation"),
            kf(0.0,  0,0,0,"position"), kf(2.0, 0, 0.02, 0, "position"),
            kf(4.0,  0,0,0,"position"), kf(6.0, 0, -0.02, 0, "position"),
            kf(8.0,  0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"), kf(2.0,1,1,1,"scale"), kf(4.0,1,1,1,"scale"),
            kf(6.0,  1,1,1,"scale"), kf(8.0,1,1,1,"scale"),
        ]}

    # Arch slabs gentle wave
    for i, buid in enumerate(arch_bids):
        phase = i * 0.5
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0, 0, 0, "rotation"),
            kf(2.0,  0, 0, 1.0*math.sin(phase),     "rotation"),
            kf(4.0,  0, 0, 1.0*math.sin(phase+1.5), "rotation"),
            kf(6.0,  0, 0, 1.0*math.sin(phase+3.0), "rotation"),
            kf(8.0,  0, 0, 0, "rotation"),
            kf(0.0,  0,0,0,"position"), kf(2.0,0,0.03,0,"position"),
            kf(4.0,  0,0,0,"position"), kf(6.0,0,-0.03,0,"position"), kf(8.0,0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"), kf(4.0,1,1,1,"scale"), kf(8.0,1,1,1,"scale"),
        ]}

    # Base slabs: completely still but keyed
    for buid in base_bids + foot_bids:
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"position"), kf(2.0,0,0,0,"position"), kf(4.0,0,0,0,"position"),
            kf(6.0,  0,0,0,"position"), kf(8.0,0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"), kf(2.0,1,1,1,"scale"), kf(4.0,1.005,1.005,1.005,"scale"),
            kf(6.0,  1,1,1,"scale"), kf(8.0,1,1,1,"scale"),
            kf(0.0,  0,0,0,"rotation"), kf(4.0,0,0,0,"rotation"), kf(8.0,0,0,0,"rotation"),
        ]}

    # Mirror surface: subtle scale ripple (each slab offset phase)
    for i, buid in enumerate(mirror_bids):
        phase = i * 1.5
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1.0, 1.0, 1.0, "scale"),
            kf(2.0,  1.02+0.005*math.sin(phase), 1.02-0.005*math.sin(phase), 1.0, "scale"),
            kf(4.0,  0.98, 1.02, 1.0, "scale"),
            kf(6.0,  1.02, 0.98, 1.0, "scale"),
            kf(8.0,  1.0, 1.0, 1.0, "scale"),
            kf(0.0,  0,0,0,"position"), kf(2.0,0,0,0.01,"position"), kf(4.0,0,0,-0.01,"position"),
            kf(6.0,  0,0,0.01,"position"), kf(8.0,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(4.0,0,0,0,"rotation"), kf(8.0,0,0,0,"rotation"),
        ]}

    # Crescent slow Y rotate
    for i, buid in enumerate(crescent_bids):
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0, 0, 0, "rotation"),
            kf(2.0,  0, 90, 0, "rotation"),
            kf(4.0,  0, 180, 0, "rotation"),
            kf(6.0,  0, 270, 0, "rotation"),
            kf(8.0,  0, 360, 0, "rotation"),
            kf(0.0,  0,0,0,"position"), kf(4.0,0,0.05,0,"position"), kf(8.0,0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"), kf(4.0,1,1,1,"scale"), kf(8.0,1,1,1,"scale"),
        ]}

    for i, buid in enumerate(crescent_orn_bids):
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"), kf(2.0, 1.1,1.1,1.1,"scale"),
            kf(4.0,  1,1,1,"scale"), kf(6.0, 0.95,0.95,0.95,"scale"),
            kf(8.0,  1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"), kf(4.0,0,0,0,"position"), kf(8.0,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(4.0,0,0,0,"rotation"), kf(8.0,0,0,0,"rotation"),
        ]}

    # Ornaments pulse emissive (scale)
    for i, buid in enumerate(ornament_bids):
        phase = i * 1.0
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1.0, 1.0, 1.0, "scale"),
            kf(2.0,  1.0+0.08*math.sin(phase), 1.0+0.08*math.sin(phase), 1.0, "scale"),
            kf(4.0,  1.08, 1.08, 1.0, "scale"),
            kf(6.0,  1.0, 1.0, 1.0, "scale"),
            kf(8.0,  1.0, 1.0, 1.0, "scale"),
            kf(0.0,  0,0,0,"position"), kf(4.0,0,0,0,"position"), kf(8.0,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(4.0,0,0,0,"rotation"), kf(8.0,0,0,0,"rotation"),
        ]}

    for i, buid in enumerate(ornament_inner_bids):
        phase = i * 0.7
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1.0, 1.0, 1.0, "scale"),
            kf(2.0,  1.05+0.04*math.sin(phase), 1.05, 1.0, "scale"),
            kf(4.0,  1.0, 1.0, 1.0, "scale"),
            kf(6.0,  0.95, 0.95, 1.0, "scale"),
            kf(8.0,  1.0, 1.0, 1.0, "scale"),
            kf(0.0,  0,0,0,"position"), kf(4.0,0,0,0,"position"), kf(8.0,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(4.0,0,0,0,"rotation"), kf(8.0,0,0,0,"rotation"),
        ]}

    # Engravings + carved details + pillar side trim: gentle scale breathe
    for i, buid in enumerate(engraving_bids + carved_pillar_bids + keystone_bids + pillar_side_bids):
        phase = i * 0.3
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1.0, 1.0, 1.0, "scale"),
            kf(2.0,  1.02, 1.02, 1.0, "scale"),
            kf(4.0,  1.0, 1.0, 1.0, "scale"),
            kf(6.0,  0.98, 0.98, 1.0, "scale"),
            kf(8.0,  1.0, 1.0, 1.0, "scale"),
            kf(0.0,  0,0,0,"position"), kf(4.0,0,0,0,"position"), kf(8.0,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(4.0,0,0,0,"rotation"), kf(8.0,0,0,0,"rotation"),
        ]}

    # Reflection ripple layers phase in and out (scale 0..1.05)
    for i, buid in enumerate(ripple_bids):
        phase = i * 1.2
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1.0, 1.0, 1.0, "scale"),
            kf(2.0,  0.6, 0.7, 1.0, "scale"),
            kf(4.0,  1.05, 1.0, 1.0, "scale"),
            kf(6.0,  0.5, 0.6, 1.0, "scale"),
            kf(8.0,  1.0, 1.0, 1.0, "scale"),
            kf(0.0,  0,0,0,"position"), kf(2.0,0,0,0.02,"position"), kf(4.0,0,0,0,"position"),
            kf(6.0,  0,0,-0.02,"position"), kf(8.0,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(4.0,0,0,0,"rotation"), kf(8.0,0,0,0,"rotation"),
        ]}

    # Mirror inner glow: pulses
    for i, buid in enumerate(mirror_glow_bids):
        phase = i * 0.9
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1.0, 1.0, 1.0, "scale"),
            kf(2.0,  1.0+0.06*math.sin(phase), 1.0, 1.0, "scale"),
            kf(4.0,  1.0+0.06*math.sin(phase+1.5), 1.0, 1.0, "scale"),
            kf(6.0,  1.0+0.06*math.sin(phase+3.0), 1.0, 1.0, "scale"),
            kf(8.0,  1.0, 1.0, 1.0, "scale"),
            kf(0.0,  0,0,0,"position"), kf(4.0,0,0,0,"position"), kf(8.0,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(4.0,0,0,0,"rotation"), kf(8.0,0,0,0,"rotation"),
        ]}

    return a


def build_dissipate():
    """dissipate once 1.2s: mirror shatters, crescent lifts, arch collapses inward, pillars sink."""
    a = {}

    # Mirror shatters — 3 layers fly different directions
    shatter_dirs = [(-3, 1, 2), (3, 0, -2), (0, 3, 2)]
    for i, buid in enumerate(mirror_bids):
        dx, dy, dz = shatter_dirs[i]
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   1,1,1,"scale"), kf(0.10, 1,1,1,"scale"),
            kf(0.40, 0.9,0.9,0.9,"scale"), kf(0.80, 0.4,0.4,0.4,"scale"),
            kf(1.20, 0,0,0,"scale"),
            kf(0.0,   0,0,0,"position"), kf(0.10, 0,0,0,"position"),
            kf(0.40, dx*0.3, dy*0.3, dz*0.3, "position"),
            kf(0.80, dx*0.7, dy*0.7, dz*0.7, "position"),
            kf(1.20, dx, dy, dz, "position"),
            kf(0.0,   0,0,0,"rotation"), kf(0.40, 30,30,0,"rotation"),
            kf(0.80, 60,60,0,"rotation"), kf(1.20, 120,90,0,"rotation"),
        ]}

    # Ripple layers — also fly outward
    for i, buid in enumerate(ripple_bids):
        dx = -2 if i == 0 else 2
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   1,1,1,"scale"), kf(0.10, 1,1,1,"scale"),
            kf(0.50, 0.5,0.5,0.5,"scale"), kf(1.20, 0,0,0,"scale"),
            kf(0.0,   0,0,0,"position"), kf(0.10, 0,0,0,"position"),
            kf(0.50, dx*0.5, 1, 0, "position"),
            kf(1.20, dx*1.2, 2.5, 0, "position"),
            kf(0.0,   0,0,0,"rotation"), kf(0.5, 45,0,0,"rotation"), kf(1.20, 90,0,0,"rotation"),
        ]}

    # Crescent lifts and tumbles
    for i, buid in enumerate(crescent_bids):
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   1,1,1,"scale"), kf(0.30, 1,1,1,"scale"),
            kf(0.80, 1,1,1,"scale"), kf(1.20, 0,0,0,"scale"),
            kf(0.0,   0,0,0,"position"), kf(0.30, 0,0.5,0,"position"),
            kf(0.80, 0,2.5,0,"position"), kf(1.20, 0,5,0,"position"),
            kf(0.0,   0,0,0,"rotation"), kf(0.40, 0,90,0,"rotation"),
            kf(0.80, 0,180,0,"rotation"), kf(1.20, 0,360,0,"rotation"),
        ]}
    for i, buid in enumerate(crescent_orn_bids):
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   1,1,1,"scale"), kf(0.40, 1,1,1,"scale"),
            kf(0.80, 0.5,0.5,0.5,"scale"), kf(1.20, 0,0,0,"scale"),
            kf(0.0,   0,0,0,"position"), kf(0.40, 0,1,0,"position"),
            kf(0.80, 0,2.5,0,"position"), kf(1.20, 0,4,0,"position"),
            kf(0.0,   0,0,0,"rotation"), kf(0.6, 60,0,0,"rotation"), kf(1.20, 120,0,0,"rotation"),
        ]}

    # Arch collapses inward (z+1, scale->0)
    for i, buid in enumerate(arch_bids):
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   1,1,1,"scale"), kf(0.30, 1,1,1,"scale"),
            kf(0.70, 0.6,0.6,0.6,"scale"), kf(1.20, 0,0,0,"scale"),
            kf(0.0,   0,0,0,"position"),
            kf(0.30, 0,0,0,"position"),
            kf(0.70, (-1 if i<3 else 1)*0.4, -0.5, 0.4, "position"),
            kf(1.20, (-1 if i<3 else 1)*0.8, -1.5, 1.2, "position"),
            kf(0.0,   0,0,0,"rotation"), kf(0.5, 0,0,15,"rotation"), kf(1.20, 0,0,45,"rotation"),
        ]}
    for i, buid in enumerate(keystone_bids):
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   1,1,1,"scale"), kf(0.30, 1,1,1,"scale"),
            kf(0.70, 0.7,0.7,0.7,"scale"), kf(1.20, 0,0,0,"scale"),
            kf(0.0,   0,0,0,"position"), kf(0.30, 0,0,0,"position"),
            kf(0.70, 0, -1, 0, "position"), kf(1.20, 0, -2.5, 0, "position"),
            kf(0.0,   0,0,0,"rotation"), kf(0.6, 30,0,0,"rotation"), kf(1.20, 90,0,0,"rotation"),
        ]}

    # Pillars sink below ground
    for i, buid in enumerate(left_pillar_bids + right_pillar_bids):
        seg = i % 4
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   1,1,1,"scale"), kf(0.40, 1,1,1,"scale"),
            kf(0.90, 1,0.7,1,"scale"), kf(1.20, 0,0,0,"scale"),
            kf(0.0,   0,0,0,"position"), kf(0.40, 0,0,0,"position"),
            kf(0.90, 0, -1.5 - seg*0.2, 0, "position"),
            kf(1.20, 0, -4 - seg*0.4, 0, "position"),
            kf(0.0,   0,0,0,"rotation"), kf(0.6, 0,0,0,"rotation"), kf(1.20, 0,0,0,"rotation"),
        ]}

    # Carved + engravings + side trim — sink with pillars
    for i, buid in enumerate(carved_pillar_bids + engraving_bids + pillar_side_bids):
        seg_y = (i % 4) * 0.3
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   1,1,1,"scale"), kf(0.40, 1,1,1,"scale"),
            kf(0.90, 0.5,0.5,0.5,"scale"), kf(1.20, 0,0,0,"scale"),
            kf(0.0,   0,0,0,"position"), kf(0.40, 0,0,0,"position"),
            kf(0.90, 0, -1.5 - seg_y, 0, "position"),
            kf(1.20, 0, -4 - seg_y, 0, "position"),
            kf(0.0,   0,0,0,"rotation"), kf(0.6, 0,0,0,"rotation"), kf(1.20, 0,0,0,"rotation"),
        ]}

    # Base retracts
    for i, buid in enumerate(base_bids + foot_bids):
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   1,1,1,"scale"), kf(0.50, 1,1,1,"scale"),
            kf(0.90, 0.7,0.4,0.7,"scale"), kf(1.20, 0,0,0,"scale"),
            kf(0.0,   0,0,0,"position"), kf(0.50, 0,0,0,"position"),
            kf(0.90, 0, -0.8, 0, "position"), kf(1.20, 0, -2.5, 0, "position"),
            kf(0.0,   0,0,0,"rotation"), kf(0.6, 0,0,0,"rotation"), kf(1.20, 0,0,0,"rotation"),
        ]}

    # Ornaments scatter outward, scale to 0
    for i, buid in enumerate(ornament_bids):
        side = -1 if i in (0,1) else 1
        vert = 1 if i in (1,3) else 0
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   1,1,1,"scale"), kf(0.20, 1,1,1,"scale"),
            kf(0.70, 0.6,0.6,0.6,"scale"), kf(1.20, 0,0,0,"scale"),
            kf(0.0,   0,0,0,"position"), kf(0.20, 0,0,0,"position"),
            kf(0.70, side*0.6, vert*0.5, 0, "position"),
            kf(1.20, side*1.5, vert*1.5, 0, "position"),
            kf(0.0,   0,0,0,"rotation"), kf(0.6, side*30, 0, 0, "rotation"),
            kf(1.20, side*90, 0, 0, "rotation"),
        ]}
    for i, buid in enumerate(ornament_inner_bids):
        side = -1 if i < 4 else 1
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   1,1,1,"scale"), kf(0.30, 1,1,1,"scale"),
            kf(0.70, 0.4,0.4,0.4,"scale"), kf(1.20, 0,0,0,"scale"),
            kf(0.0,   0,0,0,"position"), kf(0.30, 0,0,0,"position"),
            kf(0.70, side*0.5, 0.3, 0, "position"),
            kf(1.20, side*1.2, 0.8, 0, "position"),
            kf(0.0,   0,0,0,"rotation"), kf(0.6, 0,30,0,"rotation"), kf(1.20, 0,90,0,"rotation"),
        ]}

    # Mirror inner glow strips fade
    for i, buid in enumerate(mirror_glow_bids):
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   1,1,1,"scale"), kf(0.10, 1,1,1,"scale"),
            kf(0.40, 0.5,0.5,0.5,"scale"), kf(1.20, 0,0,0,"scale"),
            kf(0.0,   0,0,0,"position"), kf(0.40, 0,0,0,"position"), kf(1.20, 0,0,0,"position"),
            kf(0.0,   0,0,0,"rotation"), kf(0.6, 0,0,0,"rotation"), kf(1.20, 0,0,0,"rotation"),
        ]}

    return a


# Build animations
spawn_anim = {
    "uuid": aid(1), "name": "animation.silver_mirror_portal.spawn",
    "loop": "once", "length": 1.5, "snapping": 24,
    "selected": False, "saved": False, "animators": build_spawn()
}
idle_anim = {
    "uuid": aid(2), "name": "animation.silver_mirror_portal.idle",
    "loop": "loop", "length": 8.0, "snapping": 24,
    "selected": False, "saved": False, "animators": build_idle()
}
dissipate_anim = {
    "uuid": aid(3), "name": "animation.silver_mirror_portal.dissipate",
    "loop": "once", "length": 1.2, "snapping": 24,
    "selected": False, "saved": False, "animators": build_dissipate()
}

outliner = [b["uuid"] for b in bones]

textures = [
    {"id":"0","name":"silver_mirror_portal_tex",
     "relative_path":"../textures/silver_mirror_portal_tex.png",
     "folder":"devilsdream","namespace":"","visible":True,"mode":"bitmap","saved":False,
     "uuid":tid(1),"source":f"data:image/png;base64,{b64_0}"},
    {"id":"1","name":"silver_mirror_portal_tex1",
     "relative_path":"../textures/silver_mirror_portal_tex1.png",
     "folder":"devilsdream","namespace":"","visible":True,"mode":"bitmap","saved":False,
     "uuid":tid(2),"source":f"data:image/png;base64,{b64_1}"},
]

model = {
    "meta": {"format_version":"4.10","model_format":"free","box_uv":False},
    "name": "silver_mirror_portal",
    "geometry": "geometry.silver_mirror_portal",
    "resolution": {"width":64,"height":64},
    "elements": elements,
    "outliner": outliner,
    "textures": textures,
    "animations": [spawn_anim, idle_anim, dissipate_anim]
}

# Verify every bone has 6+ keyframes per anim
for anim in model["animations"]:
    for buid in all_bids:
        assert buid in anim["animators"], f"missing bone {bone_name_map[buid]} in anim {anim['name']}"
        n = len(anim["animators"][buid]["keyframes"])
        assert n >= 6, f"bone {bone_name_map[buid]} only {n} keyframes in {anim['name']}"

out_path = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/silver_mirror_portal.bbmodel"
with open(out_path, "w", encoding="utf-8") as f:
    json.dump(model, f, separators=(',', ':'))
size = os.path.getsize(out_path)
print(f"Written: {out_path}")
print(f"Size: {size:,} bytes ({size/1024:.1f} KB)")
print(f"Elements: {len(elements)} | Bones: {len(bones)}")
print(f"{'PASS' if size>=300*1024 else 'FAIL'}: >=300KB requirement")
