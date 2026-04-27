"""
Build blood_comet.bbmodel
Comet projectile flying Z+. Dense ball core, tail trailing Z-.
- 8 overlapping core cubes (sizes 2.0 down to 0.8) form ball
- 8 spatter accents close-orbit core (within 1.5 units)
- 10 elongated tail pieces extending Z- (lengths 0.8 -> 4.0)
- 6-slab heat shimmer ring at equator
- Plus: inner core detail, outer blood drops, tail edge accents, secondary ring
Targets 80+ elements, 55+ bones.
"""
import json, math, os, random

b64_0 = open("D:/CC/ChaosCraft/tools/gen_textures/m14_b64.txt").read().strip()
b64_1 = open("D:/CC/ChaosCraft/tools/gen_textures/m14_tex1_b64.txt").read().strip()

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

rng = random.Random(7777)

# ── 1. CORE BALL — 8 overlapping cubes forming dense sphere ───────────────────
# Sizes: 2.0, 1.7, 1.5, 1.3, 1.1, 1.0, 0.9, 0.8
# Centered near origin (0,0,0), tightly clustered
core_defs = [
    # name, half_size, offset (x,y,z), rotation (used by bone)
    ("core_1", 1.00, (0.0, 0.0, 0.0),    [0, 0, 0]),       # 2.0 size — main
    ("core_2", 0.85, (0.4, 0.2, 0.2),    [10, 15, 5]),     # 1.7
    ("core_3", 0.75, (-0.3, 0.3, -0.1),  [20, -10, 15]),   # 1.5
    ("core_4", 0.65, (0.2, -0.4, 0.3),   [-15, 25, -10]),  # 1.3
    ("core_5", 0.55, (-0.4, -0.2, 0.4),  [30, 20, 25]),    # 1.1
    ("core_6", 0.50, (0.5, 0.1, -0.4),   [25, -25, 20]),   # 1.0
    ("core_7", 0.45, (-0.2, -0.5, -0.2), [-20, 35, -15]),  # 0.9
    ("core_8", 0.40, (0.3, 0.5, 0.0),    [40, -15, 30]),   # 0.8
]
core_bids = []
for name, hs, off, rot in core_defs:
    fr = [off[0]-hs, off[1]-hs, off[2]-hs]
    to = [off[0]+hs, off[1]+hs, off[2]+hs]
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [off[0], off[1], off[2]], [eid(ei)], rot=rot); bones.append(b)
    core_bids.append(bid(bi)); ei+=1; bi+=1

# ── 2. SPATTER ACCENTS — 8 small flat slabs orbiting core within 1.5 units ────
spatter_bids = []
for i in range(8):
    name = f"spatter_{i+1}"
    angle = i * 45.0 + 12  # spread around
    rad = math.radians(angle)
    # close orbit at varying radii 0.9-1.4 and varying y heights
    r = 0.9 + (i % 3) * 0.18
    y_off = (i % 4 - 1.5) * 0.35
    cx = r * math.cos(rad)
    cz = r * math.sin(rad)
    # flat irregular slab
    sw = 0.30 + rng.random() * 0.18
    sh = 0.10 + rng.random() * 0.06
    sd = 0.20 + rng.random() * 0.12
    fr = [cx - sw/2, y_off - sh/2, cz - sd/2]
    to = [cx + sw/2, y_off + sh/2, cz + sd/2]
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    # tilt at random angles
    rot = [rng.uniform(-30, 30), rng.uniform(-180, 180), rng.uniform(-30, 30)]
    b = bone(bi, name, [cx, y_off, cz], [eid(ei)], rot=rot); bones.append(b)
    spatter_bids.append(bid(bi)); ei+=1; bi+=1

# ── 3. COMET TAIL — 10 elongated slabs Z- progressively LONGER ────────────────
# Lengths: 0.8 (nearest core) to 4.0 (farthest)
# Tight cone — slab width also tapers
tail_bids = []
tail_lengths = [0.8, 1.1, 1.4, 1.8, 2.2, 2.6, 3.0, 3.3, 3.7, 4.0]
cur_z = -1.0
for i, length in enumerate(tail_lengths):
    name = f"tail_{i+1}"
    # tail width tapers from 0.45 (near core) to 0.18 (far)
    width = 0.45 - i * 0.027
    height = 0.40 - i * 0.024
    # slight spread Y/X at tip
    spread = i * 0.04
    cx = (rng.random() - 0.5) * spread
    cy = (rng.random() - 0.5) * spread
    fr = [cx - width/2, cy - height/2, cur_z - length]
    to = [cx + width/2, cy + height/2, cur_z]
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    b = bone(bi, name, [cx, cy, cur_z], [eid(ei)]); bones.append(b)
    tail_bids.append(bid(bi)); ei+=1; bi+=1
    cur_z -= length * 0.7  # overlap by 30% so tail is continuous

# ── 4. HEAT SHIMMER RING — 6 flat slabs at equator ────────────────────────────
shimmer_bids = []
shim_radius = 1.7
for i in range(6):
    name = f"shimmer_{i+1}"
    angle = i * 60.0
    rad = math.radians(angle)
    cx = shim_radius * math.cos(rad)
    cz = shim_radius * math.sin(rad)
    sw, sh, sd = 0.55, 0.18, 0.12
    fr = [cx - sw/2, -sh/2, cz - sd/2]
    to = [cx + sw/2, sh/2, cz + sd/2]
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    b = bone(bi, name, [cx, 0, cz], [eid(ei)], rot=[0, -angle, 0]); bones.append(b)
    shimmer_bids.append(bid(bi)); ei+=1; bi+=1

# ── 5. INNER CORE DETAIL — 6 small clot cubes nestled inside ──────────────────
inner_bids = []
for i in range(6):
    name = f"inner_{i+1}"
    angle = i * 60.0 + 20
    rad = math.radians(angle)
    r = 0.4
    cx = r * math.cos(rad)
    cy = (i % 3 - 1) * 0.25
    cz = r * math.sin(rad)
    sz = 0.18 + (i % 2) * 0.08
    fr = [cx - sz, cy - sz, cz - sz]
    to = [cx + sz, cy + sz, cz + sz]
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [cx, cy, cz], [eid(ei)], rot=[i*15, i*30, i*45]); bones.append(b)
    inner_bids.append(bid(bi)); ei+=1; bi+=1

# ── 6. OUTER BLOOD DROPS — 12 small cubes scattered close to core ─────────────
drop_bids = []
for i in range(12):
    name = f"drop_{i+1}"
    # spherical-ish scatter at radius 1.5-2.0
    theta = i * (2*math.pi/12)
    phi = (i % 4) * (math.pi/6) - math.pi/6
    r = 1.5 + (i % 3) * 0.15
    cx = r * math.cos(theta) * math.cos(phi)
    cy = r * math.sin(phi)
    cz = r * math.sin(theta) * math.cos(phi)
    sz = 0.10 + (i % 3) * 0.04
    fr = [cx - sz, cy - sz, cz - sz]
    to = [cx + sz, cy + sz, cz + sz]
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [cx, cy, cz], [eid(ei)]); bones.append(b)
    drop_bids.append(bid(bi)); ei+=1; bi+=1

# ── 7. TAIL EDGE ACCENTS — 12 thin slabs along tail edges ─────────────────────
# 2 per main tail segment for the first 6 tail segments — flickering sparks
edge_bids = []
cur_z = -1.0
for i in range(10):
    length = tail_lengths[i]
    for side in [-1, 1]:
        name = f"tedge_{i+1}_{'L' if side<0 else 'R'}"
        width_main = 0.45 - i * 0.027
        # accent on outside edge
        ex = side * (width_main/2 + 0.04)
        accent_w = 0.06
        accent_h = 0.30 - i * 0.018
        accent_d = length * 0.85
        fr = [ex - accent_w/2, -accent_h/2, cur_z - accent_d]
        to = [ex + accent_w/2, accent_h/2, cur_z]
        e = elem(ei, name, fr, to, UV1); elements.append(e)
        b = bone(bi, name, [ex, 0, cur_z], [eid(ei)]); bones.append(b)
        edge_bids.append(bid(bi)); ei+=1; bi+=1
    cur_z -= length * 0.7
# Stop at 12 if we exceed
edge_bids = edge_bids[:12]
# Trim elements/bones if extra
extra = len(edge_bids)
# (we used the loop properly, edge_bids has 20, trim to 12 won't work for elements/bones already added)
# Actually our loop added 20 elements (10*2). We need to keep ALL. So just allow 20 edges.
# Reset: instead use the actual count
# rewind — let's not trim, keep all 20

# Count what we actually added to elements
# We added 20 (10 segs * 2 sides). Let's keep all.
# Reset edge_bids correctly by recounting:
# Since we added them all, edge_bids should already have 20 entries from the loop above
# But we sliced. Let's re-scan:

# Rebuild edge_bids to include all that we actually appended
all_bone_uuids_so_far = [b["uuid"] for b in bones]
edge_bids = all_bone_uuids_so_far[-20:]

# ── 8. SECONDARY RING — 6 outer ring slabs at larger radius ───────────────────
ring2_bids = []
ring2_radius = 2.2
for i in range(6):
    name = f"ring2_{i+1}"
    angle = i * 60.0 + 30
    rad = math.radians(angle)
    cx = ring2_radius * math.cos(rad)
    cz = ring2_radius * math.sin(rad)
    sw, sh, sd = 0.35, 0.14, 0.10
    fr = [cx - sw/2, -sh/2, cz - sd/2]
    to = [cx + sw/2, sh/2, cz + sd/2]
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    b = bone(bi, name, [cx, 0, cz], [eid(ei)], rot=[0, -angle, 0]); bones.append(b)
    ring2_bids.append(bid(bi)); ei+=1; bi+=1

# ── 9. CORE WET HIGHLIGHTS — 4 small bright slabs on top of core ──────────────
wet_bids = []
wet_defs = [
    ("wet_top",   [-0.20, 0.95, -0.20], [0.20, 1.05, 0.20]),
    ("wet_front", [-0.20, -0.10, 0.95], [0.20, 0.10, 1.05]),
    ("wet_R",     [0.95, -0.10, -0.20], [1.05, 0.10, 0.20]),
    ("wet_L",     [-1.05, -0.10, -0.20], [-0.95, 0.10, 0.20]),
]
for name, fr, to in wet_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    cx = (fr[0]+to[0])/2; cy = (fr[1]+to[1])/2; cz = (fr[2]+to[2])/2
    b = bone(bi, name, [cx, cy, cz], [eid(ei)]); bones.append(b)
    wet_bids.append(bid(bi)); ei+=1; bi+=1

# ── 10. LEADING SHOCK FACE — 4 small slabs on Z+ leading edge ─────────────────
shock_bids = []
shock_positions = [
    (-0.30, 0.10, 1.10), (0.30, 0.10, 1.10),
    (-0.10, 0.40, 1.05), (0.10, -0.40, 1.05),
]
for i, (cx, cy, cz) in enumerate(shock_positions):
    name = f"shock_{i+1}"
    sz = 0.12
    fr = [cx - sz, cy - sz, cz - 0.05]
    to = [cx + sz, cy + sz, cz + 0.10]
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    b = bone(bi, name, [cx, cy, cz], [eid(ei)]); bones.append(b)
    shock_bids.append(bid(bi)); ei+=1; bi+=1

print(f"Elements: {len(elements)}, Bones: {len(bones)}")
assert len(elements) == len(bones)
assert len(elements) >= 80, f"Need >=80 got {len(elements)}"
assert len(bones) >= 55, f"Need >=55 bones got {len(bones)}"

# Edge_bids was assigned to last 20 bones — but we then added more.
# Let's recompute edge_bids properly: it's bones that have name starting with "tedge_"
edge_bids = [b["uuid"] for b in bones if b["name"].startswith("tedge_")]

all_bids = (core_bids + spatter_bids + tail_bids + shimmer_bids + inner_bids +
            drop_bids + edge_bids + ring2_bids + wet_bids + shock_bids)

# Verify all bones covered
assert len(all_bids) == len(bones), f"Mismatch: all_bids={len(all_bids)} bones={len(bones)}"

bone_name_map = {b["uuid"]: b["name"] for b in bones}

# ══════════════════════════════════════════════════════════════════════════════
# ANIMATIONS
# ══════════════════════════════════════════════════════════════════════════════

def build_spawn():
    """spawn once 0.30s: core cubes materialise scale-in at different rates,
    spatter orbits immediately, tail extends sequentially, shimmer ring expands."""
    animators = {}

    # Core: scale in at different rates
    for i, buid in enumerate(core_bids):
        delay = i * 0.012
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,            0, 0, 0, "scale"),
            kf(delay,          0, 0, 0, "scale"),
            kf(delay+0.08,     1.2, 1.2, 1.2, "scale"),
            kf(delay+0.16,     0.96, 0.96, 0.96, "scale"),
            kf(0.25,           1, 1, 1, "scale"),
            kf(0.30,           1, 1, 1, "scale"),
            kf(0.0,  0,0,0,"position"), kf(0.15,0,0,0,"position"), kf(0.30,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.15,0,0,0,"rotation"), kf(0.30,0,0,0,"rotation"),
        ]}

    # Spatter: orbits in immediately
    for i, buid in enumerate(spatter_bids):
        delay = 0.04 + i * 0.008
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,            0, 0, 0, "scale"),
            kf(delay,          0, 0, 0, "scale"),
            kf(delay+0.08,     1, 1, 1, "scale"),
            kf(0.25,           1, 1, 1, "scale"),
            kf(0.30,           1, 1, 1, "scale"),
            kf(0.0,            0, 0, 0, "position"),
            kf(delay,          0, 0, 0, "position"),
            kf(delay+0.10,     0, 0, 0, "position"),
            kf(0.30,           0, 0, 0, "position"),
            kf(0.0,  0, 0, 0, "rotation"),
            kf(0.15, 0, 90, 0, "rotation"),
            kf(0.30, 0, 180, 0, "rotation"),
        ]}

    # Tail: extends sequentially Z- (each later one delays more)
    for i, buid in enumerate(tail_bids):
        delay = 0.04 + i * 0.020  # closest first, far last
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,            0, 0, 0, "scale"),
            kf(delay,          0, 0, 0, "scale"),
            kf(delay+0.06,     1, 1, 1, "scale"),
            kf(0.25,           1, 1, 1, "scale"),
            kf(0.30,           1, 1, 1, "scale"),
            kf(0.0,            0, 0, 1.5, "position"),
            kf(delay,          0, 0, 1.5, "position"),
            kf(delay+0.08,     0, 0, 0,   "position"),
            kf(0.25,           0, 0, 0,   "position"),
            kf(0.30,           0, 0, 0,   "position"),
            kf(0.0,  0,0,0,"rotation"), kf(0.15,0,0,0,"rotation"), kf(0.30,0,0,0,"rotation"),
        ]}

    # Heat shimmer ring: expand
    for i, buid in enumerate(shimmer_bids):
        base_ang = i * 60.0
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,            0, 0, 0, "scale"),
            kf(0.06,           0, 0, 0, "scale"),
            kf(0.16,           1.15, 1.15, 1.15, "scale"),
            kf(0.22,           1, 1, 1, "scale"),
            kf(0.30,           1, 1, 1, "scale"),
            kf(0.0,  0,0,0,"position"), kf(0.15,0,0,0,"position"), kf(0.30,0,0,0,"position"),
            kf(0.0,  0, -base_ang, 0, "rotation"),
            kf(0.15, 0, -base_ang+10, 0, "rotation"),
            kf(0.30, 0, -base_ang, 0, "rotation"),
        ]}

    # Inner core, drops, edge accents, ring2, wet, shock — simple scale in
    for buid_list, base_delay in [(inner_bids, 0.05), (drop_bids, 0.10),
                                   (edge_bids, 0.06), (ring2_bids, 0.10),
                                   (wet_bids, 0.04), (shock_bids, 0.08)]:
        for i, buid in enumerate(buid_list):
            delay = base_delay + i * 0.005
            animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
                kf(0.0,           0, 0, 0, "scale"),
                kf(delay,         0, 0, 0, "scale"),
                kf(delay+0.06,    1, 1, 1, "scale"),
                kf(0.25,          1, 1, 1, "scale"),
                kf(0.30,          1, 1, 1, "scale"),
                kf(0.0,  0,0,0,"position"), kf(0.15,0,0,0,"position"), kf(0.30,0,0,0,"position"),
                kf(0.0,  0,0,0,"rotation"), kf(0.15,0,0,0,"rotation"), kf(0.30,0,0,0,"rotation"),
            ]}

    return animators


def build_idle():
    """idle loop 2.0s: core rotates on all 3 axes at different rates,
    spatter orbits at various radii/heights, tail oscillates, shimmer pulses."""
    animators = {}
    L = 2.0

    # Core: rotates on all 3 axes
    for i, buid in enumerate(core_bids):
        rx = 360 * (1 + i*0.1)  # different rates
        ry = 360 * (0.7 + i*0.08)
        rz = 360 * (0.5 + i*0.12)
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0, 0, 0, "rotation"),
            kf(0.40, rx*0.20, ry*0.20, rz*0.20, "rotation"),
            kf(0.80, rx*0.40, ry*0.40, rz*0.40, "rotation"),
            kf(1.20, rx*0.60, ry*0.60, rz*0.60, "rotation"),
            kf(1.60, rx*0.80, ry*0.80, rz*0.80, "rotation"),
            kf(2.00, rx, ry, rz, "rotation"),
            kf(0.0,  1, 1, 1, "scale"),
            kf(0.50, 1.02, 1.02, 1.02, "scale"),
            kf(1.00, 1, 1, 1, "scale"),
            kf(1.50, 0.98, 0.98, 0.98, "scale"),
            kf(2.00, 1, 1, 1, "scale"),
            kf(0.0,  0,0,0,"position"), kf(1.00,0,0,0,"position"), kf(2.00,0,0,0,"position"),
        ]}

    # Spatter: orbits at varying radii/heights
    for i, buid in enumerate(spatter_bids):
        phase = i * 0.5
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0.10*math.sin(phase), 0.06*math.cos(phase), 0.10*math.cos(phase), "position"),
            kf(0.40, 0.10*math.sin(phase+1), 0.06*math.cos(phase+1), 0.10*math.cos(phase+1), "position"),
            kf(0.80, 0.10*math.sin(phase+2), 0.06*math.cos(phase+2), 0.10*math.cos(phase+2), "position"),
            kf(1.20, 0.10*math.sin(phase+3), 0.06*math.cos(phase+3), 0.10*math.cos(phase+3), "position"),
            kf(1.60, 0.10*math.sin(phase+4), 0.06*math.cos(phase+4), 0.10*math.cos(phase+4), "position"),
            kf(2.00, 0.10*math.sin(phase+5), 0.06*math.cos(phase+5), 0.10*math.cos(phase+5), "position"),
            kf(0.0,  0, 0, 0, "rotation"),
            kf(0.50, 0, 90, 0, "rotation"),
            kf(1.00, 0, 180, 0, "rotation"),
            kf(1.50, 0, 270, 0, "rotation"),
            kf(2.00, 0, 360, 0, "rotation"),
            kf(0.0,  1, 1, 1, "scale"), kf(1.00, 0.95, 0.95, 0.95, "scale"), kf(2.00, 1, 1, 1, "scale"),
        ]}

    # Tail: oscillates behind
    for i, buid in enumerate(tail_bids):
        phase = i * 0.4
        amp_x = 0.04 + i * 0.015
        amp_y = 0.03 + i * 0.010
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  amp_x*math.sin(phase), amp_y*math.cos(phase), 0, "position"),
            kf(0.40, amp_x*math.sin(phase+1.2), amp_y*math.cos(phase+1.2), 0, "position"),
            kf(0.80, amp_x*math.sin(phase+2.4), amp_y*math.cos(phase+2.4), 0, "position"),
            kf(1.20, amp_x*math.sin(phase+3.6), amp_y*math.cos(phase+3.6), 0, "position"),
            kf(1.60, amp_x*math.sin(phase+4.8), amp_y*math.cos(phase+4.8), 0, "position"),
            kf(2.00, amp_x*math.sin(phase+6.0), amp_y*math.cos(phase+6.0), 0, "position"),
            kf(0.0,  0, 0, 0, "rotation"),
            kf(0.50, 1.5*math.sin(phase), 1.5*math.cos(phase), 0, "rotation"),
            kf(1.00, -1.5*math.sin(phase), -1.5*math.cos(phase), 0, "rotation"),
            kf(1.50, 1.5*math.cos(phase), 1.5*math.sin(phase), 0, "rotation"),
            kf(2.00, 0, 0, 0, "rotation"),
            kf(0.0,  1, 1, 1, "scale"), kf(1.00, 1, 1, 1, "scale"), kf(2.00, 1, 1, 1, "scale"),
        ]}

    # Shimmer ring: pulses
    for i, buid in enumerate(shimmer_bids):
        base_ang = i * 60.0
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1, 1, 1, "scale"),
            kf(0.40, 1.20, 1.20, 1.0, "scale"),
            kf(0.80, 1, 1, 1, "scale"),
            kf(1.20, 0.80, 0.80, 1.0, "scale"),
            kf(1.60, 1.10, 1.10, 1.0, "scale"),
            kf(2.00, 1, 1, 1, "scale"),
            kf(0.0,  0, -base_ang, 0, "rotation"),
            kf(0.50, 0, -base_ang + 8, 0, "rotation"),
            kf(1.00, 0, -base_ang, 0, "rotation"),
            kf(1.50, 0, -base_ang - 8, 0, "rotation"),
            kf(2.00, 0, -base_ang, 0, "rotation"),
            kf(0.0,  0, 0, 0, "position"), kf(1.00, 0, 0, 0, "position"), kf(2.00, 0, 0, 0, "position"),
        ]}

    # Inner core: rotate
    for i, buid in enumerate(inner_bids):
        rx = 180 * (1 + i*0.2)
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0, 0, 0, "rotation"),
            kf(0.50, rx*0.25, rx*0.20, rx*0.30, "rotation"),
            kf(1.00, rx*0.50, rx*0.40, rx*0.60, "rotation"),
            kf(1.50, rx*0.75, rx*0.60, rx*0.90, "rotation"),
            kf(2.00, rx, rx*0.80, rx*1.20, "rotation"),
            kf(0.0,  1, 1, 1, "scale"),
            kf(0.50, 1.05, 1.05, 1.05, "scale"),
            kf(1.00, 1, 1, 1, "scale"),
            kf(1.50, 0.95, 0.95, 0.95, "scale"),
            kf(2.00, 1, 1, 1, "scale"),
            kf(0.0,  0, 0, 0, "position"), kf(1.00, 0, 0, 0, "position"), kf(2.00, 0, 0, 0, "position"),
        ]}

    # Drops: small wobble
    for i, buid in enumerate(drop_bids):
        phase = i * 0.3
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0.04*math.sin(phase), 0.04*math.cos(phase), 0.04*math.sin(phase+1), "position"),
            kf(0.40, 0.04*math.cos(phase), -0.04*math.sin(phase), 0.04*math.cos(phase+1), "position"),
            kf(0.80, -0.04*math.sin(phase), -0.04*math.cos(phase), -0.04*math.sin(phase+1), "position"),
            kf(1.20, -0.04*math.cos(phase), 0.04*math.sin(phase), -0.04*math.cos(phase+1), "position"),
            kf(1.60, 0.04*math.sin(phase), 0.04*math.cos(phase), 0.04*math.sin(phase+1), "position"),
            kf(2.00, 0.04*math.sin(phase), 0.04*math.cos(phase), 0.04*math.sin(phase+1), "position"),
            kf(0.0,  0, 0, 0, "rotation"),
            kf(1.00, i*30, i*45, i*15, "rotation"),
            kf(2.00, i*60, i*90, i*30, "rotation"),
            kf(0.0,  1, 1, 1, "scale"), kf(1.00, 1, 1, 1, "scale"), kf(2.00, 1, 1, 1, "scale"),
        ]}

    # Edge accents: flicker
    for i, buid in enumerate(edge_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1, 1, 1, "scale"),
            kf(0.40, 1.10, 1.10, 1.05, "scale"),
            kf(0.80, 0.90, 0.90, 0.95, "scale"),
            kf(1.20, 1.15, 1.15, 1.10, "scale"),
            kf(1.60, 0.85, 0.85, 0.90, "scale"),
            kf(2.00, 1, 1, 1, "scale"),
            kf(0.0,  0,0,0,"position"), kf(1.00,0,0,0,"position"), kf(2.00,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(1.00,0,0,0,"rotation"), kf(2.00,0,0,0,"rotation"),
        ]}

    # Secondary ring: counter-pulse
    for i, buid in enumerate(ring2_bids):
        base_ang = i * 60.0 + 30
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1, 1, 1, "scale"),
            kf(0.40, 0.85, 0.85, 1.0, "scale"),
            kf(0.80, 1, 1, 1, "scale"),
            kf(1.20, 1.20, 1.20, 1.0, "scale"),
            kf(1.60, 0.95, 0.95, 1.0, "scale"),
            kf(2.00, 1, 1, 1, "scale"),
            kf(0.0,  0, -base_ang, 0, "rotation"),
            kf(0.50, 0, -base_ang - 6, 0, "rotation"),
            kf(1.00, 0, -base_ang, 0, "rotation"),
            kf(1.50, 0, -base_ang + 6, 0, "rotation"),
            kf(2.00, 0, -base_ang, 0, "rotation"),
            kf(0.0,  0, 0, 0, "position"), kf(1.00, 0, 0, 0, "position"), kf(2.00, 0, 0, 0, "position"),
        ]}

    # Wet highlights: shimmer
    for i, buid in enumerate(wet_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1, 1, 1, "scale"),
            kf(0.40, 1.15, 1.15, 1.15, "scale"),
            kf(0.80, 1, 1, 1, "scale"),
            kf(1.20, 0.85, 0.85, 0.85, "scale"),
            kf(1.60, 1.05, 1.05, 1.05, "scale"),
            kf(2.00, 1, 1, 1, "scale"),
            kf(0.0,  0,0,0,"position"), kf(1.00,0,0,0,"position"), kf(2.00,0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"), kf(1.00,0,0,0,"rotation"), kf(2.00,0,0,0,"rotation"),
        ]}

    # Shock face: flicker
    for i, buid in enumerate(shock_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1, 1, 1, "scale"),
            kf(0.40, 1.10, 1.10, 1.10, "scale"),
            kf(0.80, 0.90, 0.90, 0.90, "scale"),
            kf(1.20, 1.20, 1.20, 1.20, "scale"),
            kf(1.60, 0.95, 0.95, 0.95, "scale"),
            kf(2.00, 1, 1, 1, "scale"),
            kf(0.0,  0, 0, 0, "position"),
            kf(0.50, 0, 0, 0.05, "position"),
            kf(1.00, 0, 0, 0, "position"),
            kf(1.50, 0, 0, 0.05, "position"),
            kf(2.00, 0, 0, 0, "position"),
            kf(0.0,  0,0,0,"rotation"), kf(1.00,0,0,0,"rotation"), kf(2.00,0,0,0,"rotation"),
        ]}

    return animators


def build_dissipate():
    """dissipate once 0.35s: core explodes outward each cube different direction.
    Spatter scatters. Tail continues backward. Shimmer ring expands."""
    animators = {}

    # Core: each in different direction
    explode_dirs = [
        (0, 1, 0), (1, 0.5, 0.5), (-1, 0.3, 0.3), (0.5, -0.8, 0.5),
        (-0.5, 0.5, -0.8), (0.8, 0.2, -0.5), (-0.3, -0.6, 0.7), (0.6, 0.7, -0.3),
    ]
    for i, buid in enumerate(core_bids):
        ed = explode_dirs[i]
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   1, 1, 1, "scale"),
            kf(0.05,  1.4, 1.4, 1.4, "scale"),
            kf(0.18,  0.8, 0.8, 0.8, "scale"),
            kf(0.30,  0.3, 0.3, 0.3, "scale"),
            kf(0.35,  0, 0, 0, "scale"),
            kf(0.0,   0, 0, 0, "position"),
            kf(0.05,  ed[0]*0.3, ed[1]*0.3, ed[2]*0.3, "position"),
            kf(0.18,  ed[0]*1.2, ed[1]*1.2, ed[2]*1.2, "position"),
            kf(0.30,  ed[0]*2.5, ed[1]*2.5, ed[2]*2.5, "position"),
            kf(0.35,  ed[0]*3.5, ed[1]*3.5, ed[2]*3.5, "position"),
            kf(0.0,   0, 0, 0, "rotation"),
            kf(0.18,  i*40, i*30, i*20, "rotation"),
            kf(0.35,  i*80, i*60, i*40, "rotation"),
        ]}

    # Spatter: scatter outward in current orbit direction
    for i, buid in enumerate(spatter_bids):
        ang = i * 45.0
        rad = math.radians(ang)
        sx = 2.5 * math.cos(rad)
        sz = 2.5 * math.sin(rad)
        sy = (i % 4 - 1.5) * 0.8
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   1, 1, 1, "scale"),
            kf(0.10,  1, 1, 1, "scale"),
            kf(0.25,  0.5, 0.5, 0.5, "scale"),
            kf(0.35,  0, 0, 0, "scale"),
            kf(0.0,   0, 0, 0, "position"),
            kf(0.10,  sx*0.3, sy*0.3, sz*0.3, "position"),
            kf(0.25,  sx*0.9, sy*0.9, sz*0.9, "position"),
            kf(0.35,  sx*1.5, sy*1.5, sz*1.5, "position"),
            kf(0.0,   0, 0, 0, "rotation"),
            kf(0.18,  ang, ang*0.5, ang*0.5, "rotation"),
            kf(0.35,  ang*2, ang, ang, "rotation"),
        ]}

    # Tail: continues backward
    for i, buid in enumerate(tail_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   1, 1, 1, "scale"),
            kf(0.10,  1, 1, 1.2, "scale"),
            kf(0.20,  0.8, 0.8, 1.5, "scale"),
            kf(0.30,  0.4, 0.4, 1.0, "scale"),
            kf(0.35,  0, 0, 0, "scale"),
            kf(0.0,   0, 0, 0, "position"),
            kf(0.10,  0, 0, -0.5 - i*0.1, "position"),
            kf(0.20,  0, 0, -1.5 - i*0.2, "position"),
            kf(0.30,  0, 0, -2.5 - i*0.3, "position"),
            kf(0.35,  0, 0, -3.5 - i*0.4, "position"),
            kf(0.0,   0, 0, 0, "rotation"),
            kf(0.18,  i*5, i*3, 0, "rotation"),
            kf(0.35,  i*10, i*6, 0, "rotation"),
        ]}

    # Shimmer ring: expand and snap
    for i, buid in enumerate(shimmer_bids):
        base_ang = i * 60.0
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   1, 1, 1, "scale"),
            kf(0.10,  1.6, 1.6, 1.0, "scale"),
            kf(0.20,  2.5, 2.5, 0.5, "scale"),
            kf(0.30,  3.5, 3.5, 0.0, "scale"),
            kf(0.35,  0, 0, 0, "scale"),
            kf(0.0,   0,0,0,"position"), kf(0.18,0,0,0,"position"), kf(0.35,0,0,0,"position"),
            kf(0.0,   0, -base_ang, 0, "rotation"),
            kf(0.18,  0, -base_ang + 40, 0, "rotation"),
            kf(0.35,  0, -base_ang + 80, 0, "rotation"),
        ]}

    # Inner core, drops, edge, ring2, wet, shock
    for buid_list in [inner_bids, drop_bids, edge_bids, ring2_bids, wet_bids, shock_bids]:
        for i, buid in enumerate(buid_list):
            delay = (i % 6) * 0.015
            ang = i * 30
            rad = math.radians(ang)
            animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
                kf(0.0,         1, 1, 1, "scale"),
                kf(delay,       1, 1, 1, "scale"),
                kf(delay+0.10,  0.5, 0.5, 0.5, "scale"),
                kf(0.30,        0.1, 0.1, 0.1, "scale"),
                kf(0.35,        0, 0, 0, "scale"),
                kf(0.0,         0, 0, 0, "position"),
                kf(delay,       0, 0, 0, "position"),
                kf(0.20,        0.5*math.cos(rad), 0.3, 0.5*math.sin(rad), "position"),
                kf(0.35,        1.2*math.cos(rad), 0.6, 1.2*math.sin(rad), "position"),
                kf(0.0,  0,0,0,"rotation"), kf(0.18,i*15,i*20,0,"rotation"),
                kf(0.35,i*30,i*40,0,"rotation"),
            ]}

    return animators

# ── Build model ──────────────────────────────────────────────────────────────
spawn_anim = {
    "uuid": aid(1), "name": "animation.blood_comet.spawn",
    "loop": "once", "length": 0.30, "snapping": 24,
    "selected": False, "saved": False, "animators": build_spawn()
}
idle_anim = {
    "uuid": aid(2), "name": "animation.blood_comet.idle",
    "loop": "loop", "length": 2.00, "snapping": 24,
    "selected": False, "saved": False, "animators": build_idle()
}
dissipate_anim = {
    "uuid": aid(3), "name": "animation.blood_comet.dissipate",
    "loop": "once", "length": 0.35, "snapping": 24,
    "selected": False, "saved": False, "animators": build_dissipate()
}

outliner = [b["uuid"] for b in bones]

textures = [
    {"id":"0","name":"blood_comet_tex","relative_path":"../textures/blood_comet_tex.png",
     "folder":"devilsdream","namespace":"","visible":True,"mode":"bitmap","saved":False,
     "uuid":tid(1),"source":f"data:image/png;base64,{b64_0}"},
    {"id":"1","name":"blood_comet_tex1","relative_path":"../textures/blood_comet_tex1.png",
     "folder":"devilsdream","namespace":"","visible":True,"mode":"bitmap","saved":False,
     "uuid":tid(2),"source":f"data:image/png;base64,{b64_1}"},
]

model = {
    "meta": {"format_version":"4.10","model_format":"free","box_uv":False},
    "name": "blood_comet",
    "geometry": "geometry.blood_comet",
    "resolution": {"width":64,"height":64},
    "elements": elements,
    "outliner": outliner,
    "textures": textures,
    "animations": [spawn_anim, idle_anim, dissipate_anim]
}

out_path = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/blood_comet.bbmodel"
with open(out_path, "w", encoding="utf-8") as f:
    json.dump(model, f, separators=(',', ':'))

size = os.path.getsize(out_path)
print(f"Written: {out_path}")
print(f"Elements: {len(elements)} / Bones: {len(bones)}")
print(f"Animations: 3 (spawn 0.30s once, idle 2.00s loop, dissipate 0.35s once)")
print(f"Size: {size:,} bytes ({size/1024:.1f} KB)")
pass_fail = "PASS" if size >= 300*1024 else "FAIL"
print(f"{pass_fail}: {'>=300KB' if size>=300*1024 else f'only {size//1024}KB'}")
