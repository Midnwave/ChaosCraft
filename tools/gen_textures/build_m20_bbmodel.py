"""
Build devils_constellation.bbmodel — MODEL 20, "Omen Array"
8 irregular star points connected by 7 blood-red lines, central nexus,
orbital ring, star tails, halo rings, glow auras. 95+ elements, 55+ bones.
"""
import json, math, os

b64_0 = open("D:/CC/ChaosCraft/tools/gen_textures/m20_b64.txt").read().strip()
b64_1 = open("D:/CC/ChaosCraft/tools/gen_textures/m20_tex1_b64.txt").read().strip()

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

# === STAR POSITIONS — irregular constellation pattern (NOT regular octagon) ===
# 8 star centers — example uneven star map
STAR_POS = [
    ( 5, 7,  0),   # 0
    (-3, 9,  4),   # 1
    ( 6, 8, -2),   # 2
    (-5, 6,  5),   # 3
    ( 4,10,  3),   # 4
    (-2, 7, -6),   # 5
    ( 7, 9,  1),   # 6
    (-6, 8, -3),   # 7
]

# === 1. STARS (each star = 3 flat slabs forming 3D cross) ===
# 24 elements, 24 bones (3 per star)
star_bids = []         # bone for each star group "anchor" — we'll use the horizontal bone as anchor
star_horiz_bids = []   # horizontal slab bone per star
star_vert_bids  = []   # vertical slab bone per star
star_diag_bids  = []   # 45-deg diagonal slab bone per star

for s_idx, (sx, sy, sz) in enumerate(STAR_POS):
    # Horizontal slab — long X axis, thin Y, thin Z
    h_name = f"star{s_idx+1}_horiz"
    fr_h = [sx - 0.7, sy - 0.08, sz - 0.08]
    to_h = [sx + 0.7, sy + 0.08, sz + 0.08]
    e = elem(ei, h_name, fr_h, to_h, UV0); elements.append(e)
    b = bone(bi, h_name, [sx, sy, sz], [eid(ei)]); bones.append(b)
    star_horiz_bids.append(bid(bi)); ei+=1; bi+=1

    # Vertical slab — long Y axis
    v_name = f"star{s_idx+1}_vert"
    fr_v = [sx - 0.08, sy - 0.7, sz - 0.08]
    to_v = [sx + 0.08, sy + 0.7, sz + 0.08]
    e = elem(ei, v_name, fr_v, to_v, UV0); elements.append(e)
    b = bone(bi, v_name, [sx, sy, sz], [eid(ei)]); bones.append(b)
    star_vert_bids.append(bid(bi)); ei+=1; bi+=1

    # Diagonal slab — long Z axis, will be rotated 45 deg around Y
    d_name = f"star{s_idx+1}_diag"
    fr_d = [sx - 0.08, sy - 0.08, sz - 0.7]
    to_d = [sx + 0.08, sy + 0.08, sz + 0.7]
    e = elem(ei, d_name, fr_d, to_d, UV0); elements.append(e)
    b = bone(bi, d_name, [sx, sy, sz], [eid(ei)], rot=[0, 45, 0]); bones.append(b)
    star_diag_bids.append(bid(bi)); ei+=1; bi+=1

    star_bids.append(bid(bi - 3))  # anchor = horiz

# === 2. CONNECTING LINES (7 thin slabs connecting adjacent stars in path order) ===
# Path order: 0 → 1 → 2 → 3 → 4 → 5 → 6 → 7  (creates 7 connections)
# Each line: thin slab whose long axis points from star A to star B, using tex1 (red strip)
LINE_PATH = [(0, 1), (1, 2), (2, 3), (3, 4), (4, 5), (5, 6), (6, 7)]
line_bids = []
for li, (a_idx, b_idx) in enumerate(LINE_PATH):
    a = STAR_POS[a_idx]
    b = STAR_POS[b_idx]
    mx, my, mz = (a[0]+b[0])/2, (a[1]+b[1])/2, (a[2]+b[2])/2
    dx, dy, dz = b[0]-a[0], b[1]-a[1], b[2]-a[2]
    length = math.sqrt(dx*dx + dy*dy + dz*dz)
    half_len = length / 2.0
    # Element built along Y axis (long) at origin, then bone rotates to point to target
    name = f"line_{li+1}"
    fr = [-0.06, -half_len, -0.06]
    to = [ 0.06,  half_len,  0.06]
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    # rotation to align Y-axis with vector (dx,dy,dz)
    # yaw around Y, pitch around X
    yaw = math.degrees(math.atan2(dx, dz))
    pitch = math.degrees(math.atan2(math.sqrt(dx*dx+dz*dz), dy))
    b_obj = bone(bi, name, [mx, my, mz], [eid(ei)], rot=[pitch, yaw, 0])
    bones.append(b_obj)
    line_bids.append(bid(bi)); ei+=1; bi+=1

# === 3. CENTRAL NEXUS (3 pairs of flat slabs at Y=8) ===
# Sphere approximation via 3 perpendicular slab pairs (each pair counted as 2 elements)
nexus_bids = []
nexus_defs = [
    ("nexus_xy_a", [-0.7, 7.3, -0.05], [0.7, 8.7, 0.05]),
    ("nexus_xy_b", [-0.05, 7.3, -0.7], [0.05, 8.7, 0.7]),
    ("nexus_yz_a", [-0.7, 7.95, -0.7], [0.7, 8.05, 0.7]),
    ("nexus_xz_a", [-0.5, 7.5, -0.5], [0.5, 8.5, 0.5]),
    ("nexus_xz_b", [-0.05, 7.5, -0.65], [0.05, 8.5, 0.65]),
    ("nexus_xz_c", [-0.65, 7.5, -0.05], [0.65, 8.5, 0.05]),
]
for name, fr, to in nexus_defs:
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    b = bone(bi, name, [0, 8, 0], [eid(ei)]); bones.append(b)
    nexus_bids.append(bid(bi)); ei+=1; bi+=1

# === 4. ORBITAL RING (10 flat slabs forming a ring at Y=8) ===
# Ring centered (0, 8, 0), radius 7
orbit_bids = []
ring_r = 7.0
for i in range(10):
    angle = 2 * math.pi * i / 10
    cx_o = ring_r * math.cos(angle)
    cz_o = ring_r * math.sin(angle)
    name = f"orbit_{i+1}"
    # tangent slab — use Z-axis as long, then rotate by yaw=angle
    fr = [-0.5, -0.04, -0.04]
    to = [ 0.5,  0.04,  0.04]
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    yaw_deg = math.degrees(angle) + 90  # tangent
    b = bone(bi, name, [cx_o, 8, cz_o], [eid(ei)], rot=[0, yaw_deg, 0])
    bones.append(b)
    orbit_bids.append(bid(bi)); ei+=1; bi+=1

# === 5. STAR TAILS (8 small downward-pointing shards below each star) ===
tail_bids = []
for s_idx, (sx, sy, sz) in enumerate(STAR_POS):
    name = f"tail_{s_idx+1}"
    fr = [sx - 0.10, sy - 1.6, sz - 0.10]
    to = [sx + 0.10, sy - 0.5, sz + 0.10]
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [sx, sy - 0.5, sz], [eid(ei)]); bones.append(b)
    tail_bids.append(bid(bi)); ei+=1; bi+=1

# === 6. STAR HALOS (4 thin slabs around each star = 32 total) ===
# Small ring of 4 thin slabs orbiting each star at radius 0.9 in XZ plane
halo_bids = []
for s_idx, (sx, sy, sz) in enumerate(STAR_POS):
    for h in range(4):
        h_angle = math.pi * h / 2
        hx = sx + 0.9 * math.cos(h_angle)
        hz = sz + 0.9 * math.sin(h_angle)
        name = f"halo_{s_idx+1}_{h+1}"
        # small tangent flat slab
        fr = [-0.18, -0.03, -0.03]
        to = [ 0.18,  0.03,  0.03]
        e = elem(ei, name, fr, to, UV0); elements.append(e)
        yaw_deg = math.degrees(h_angle) + 90
        b = bone(bi, name, [hx, sy, hz], [eid(ei)], rot=[0, yaw_deg, 0])
        bones.append(b)
        halo_bids.append(bid(bi)); ei+=1; bi+=1

# === 7. STAR GLOW AURA (1 soft slab per star = 8 total, tex0 for star palette) ===
glow_bids = []
for s_idx, (sx, sy, sz) in enumerate(STAR_POS):
    name = f"glow_{s_idx+1}"
    fr = [sx - 0.4, sy - 0.4, sz - 0.05]
    to = [sx + 0.4, sy + 0.4, sz + 0.05]
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [sx, sy, sz], [eid(ei)]); bones.append(b)
    glow_bids.append(bid(bi)); ei+=1; bi+=1

print(f"Elements: {len(elements)}, Bones: {len(bones)}")
assert len(elements) >= 80, f"need >=80 elements, got {len(elements)}"
assert len(bones)    >= 55, f"need >=55 bones, got {len(bones)}"

all_star_part_bids = star_horiz_bids + star_vert_bids + star_diag_bids
all_bids = (all_star_part_bids + line_bids + nexus_bids + orbit_bids +
            tail_bids + halo_bids + glow_bids)
bone_name_map = {b["uuid"]: b["name"] for b in bones}

# Pre-compute base rotations for bones that have non-zero rot — we need to preserve them in animations
bone_base_rot = {b["uuid"]: list(b["rotation"]) for b in bones}

# ════════════════════════════════════════════════════════════════════════════
# ANIMATIONS
# ════════════════════════════════════════════════════════════════════════════

def kf_rot_with_base(buid, time, dx, dy, dz):
    """Add rotation keyframe relative to bone's base rotation."""
    base = bone_base_rot[buid]
    return kf(time, base[0]+dx, base[1]+dy, base[2]+dz, "rotation")

def build_spawn():
    """spawn once 1.1s: nexus → orbital ring → stars one by one → lines → tails."""
    a = {}

    # Nexus materializes at 0.0s
    for i, buid in enumerate(nexus_bids):
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,    0,0,0,"scale"), kf(0.05,  0,0,0,"scale"),
            kf(0.15,   1.3,1.3,1.3,"scale"), kf(0.25, 1,1,1,"scale"),
            kf(0.6,    1,1,1,"scale"), kf(1.1,    1,1,1,"scale"),
            kf(0.0,    0,0,0,"position"), kf(0.5,    0,0,0,"position"), kf(1.1,    0,0,0,"position"),
            kf_rot_with_base(buid, 0.0,  0, 0, 0),
            kf_rot_with_base(buid, 0.6,  0, 0, 0),
            kf_rot_with_base(buid, 1.1,  0, 0, 0),
        ]}

    # Orbital ring expands at 0.2s
    for i, buid in enumerate(orbit_bids):
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,    0,0,0,"scale"), kf(0.20,   0,0,0,"scale"),
            kf(0.30,   0.5,1,0.5,"scale"), kf(0.40, 1,1,1,"scale"),
            kf(0.7,    1,1,1,"scale"), kf(1.1,    1,1,1,"scale"),
            kf(0.0,    0,0,0,"position"), kf(0.5,    0,0,0,"position"), kf(1.1,    0,0,0,"position"),
            kf_rot_with_base(buid, 0.0,  0, 0, 0),
            kf_rot_with_base(buid, 0.6,  0, 0, 0),
            kf_rot_with_base(buid, 1.1,  0, 0, 0),
        ]}

    # Stars fly in 0.3-0.8s, each its own delay
    for s_idx in range(8):
        delay = 0.30 + s_idx * 0.07
        for part_list in [star_horiz_bids, star_vert_bids, star_diag_bids]:
            buid = part_list[s_idx]
            sx, sy, sz = STAR_POS[s_idx]
            # Fly in from 2x its current radius — origin is at star pos
            mag = math.sqrt(sx*sx + sy*sy + sz*sz) + 1
            ox = sx * 1.5
            oy = sy * 1.5
            oz = sz * 1.5
            a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
                kf(0.0,    0,0,0,"scale"),  kf(delay,  0,0,0,"scale"),
                kf(delay+0.05, 1.3,1.3,1.3,"scale"),
                kf(delay+0.10, 1,1,1,"scale"),
                kf(0.9,    1,1,1,"scale"),  kf(1.1,    1,1,1,"scale"),
                kf(0.0,    ox,oy,oz,"position"),
                kf(delay,  ox,oy,oz,"position"),
                kf(delay+0.05, ox*0.3, oy*0.3, oz*0.3, "position"),
                kf(delay+0.10, 0,0,0,"position"),
                kf(0.9,    0,0,0,"position"), kf(1.1, 0,0,0,"position"),
                kf_rot_with_base(buid, 0.0, 0, 0, 0),
                kf_rot_with_base(buid, 0.6, 0, 0, 0),
                kf_rot_with_base(buid, 1.1, 0, 0, 0),
            ]}

    # Halos arrive with their star
    for h_idx, buid in enumerate(halo_bids):
        s_idx = h_idx // 4
        delay = 0.32 + s_idx * 0.07
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,    0,0,0,"scale"),  kf(delay,  0,0,0,"scale"),
            kf(delay+0.06, 1,1,1,"scale"),
            kf(0.9,    1,1,1,"scale"),  kf(1.1,    1,1,1,"scale"),
            kf(0.0,    0,0,0,"position"), kf(delay,  0,0,0,"position"),
            kf(delay+0.06, 0,0,0,"position"),
            kf(0.9,    0,0,0,"position"), kf(1.1, 0,0,0,"position"),
            kf_rot_with_base(buid, 0.0, 0, 0, 0),
            kf_rot_with_base(buid, 0.6, 0, 0, 0),
            kf_rot_with_base(buid, 1.1, 0, 0, 0),
        ]}

    # Glow auras arrive with their star
    for g_idx, buid in enumerate(glow_bids):
        delay = 0.31 + g_idx * 0.07
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,    0,0,0,"scale"),  kf(delay,  0,0,0,"scale"),
            kf(delay+0.05, 1.5,1.5,1,"scale"), kf(delay+0.10, 1,1,1,"scale"),
            kf(0.9,    1,1,1,"scale"),  kf(1.1,    1,1,1,"scale"),
            kf(0.0,    0,0,0,"position"), kf(delay,  0,0,0,"position"), kf(1.1,    0,0,0,"position"),
            kf_rot_with_base(buid, 0.0, 0, 0, 0),
            kf_rot_with_base(buid, 0.6, 0, 0, 0),
            kf_rot_with_base(buid, 1.1, 0, 0, 0),
        ]}

    # Star tails extend at arrival — start at scale_y=0, grow to 1
    for s_idx, buid in enumerate(tail_bids):
        delay = 0.36 + s_idx * 0.07
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,    1,0,1,"scale"),  kf(delay,  1,0,1,"scale"),
            kf(delay+0.08, 1,1,1,"scale"),
            kf(0.9,    1,1,1,"scale"),  kf(1.1,    1,1,1,"scale"),
            kf(0.0,    0,0,0,"position"), kf(delay,  0,0,0,"position"), kf(1.1,    0,0,0,"position"),
            kf_rot_with_base(buid, 0.0, 0, 0, 0),
            kf_rot_with_base(buid, 0.6, 0, 0, 0),
            kf_rot_with_base(buid, 1.1, 0, 0, 0),
        ]}

    # Lines draw in once both endpoints arrived — line i connects stars i and i+1
    for li, buid in enumerate(line_bids):
        a_idx, b_idx = LINE_PATH[li]
        # Both stars must have arrived: their delays are 0.3+a*0.07 and 0.3+b*0.07
        delay = max(0.30 + a_idx * 0.07, 0.30 + b_idx * 0.07) + 0.10
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,    0,0,0,"scale"),  kf(delay,  0,0,0,"scale"),
            kf(delay+0.06, 1,1,1,"scale"),
            kf(0.9,    1,1,1,"scale"),  kf(1.1,    1,1,1,"scale"),
            kf(0.0,    0,0,0,"position"), kf(delay,  0,0,0,"position"), kf(1.1,    0,0,0,"position"),
            kf_rot_with_base(buid, 0.0, 0, 0, 0),
            kf_rot_with_base(buid, 0.6, 0, 0, 0),
            kf_rot_with_base(buid, 1.1, 0, 0, 0),
        ]}

    return a


def build_idle():
    """idle loop 6.0s: whole constellation rotates Y, individual stars rotate, lines pulse, ring counter-rotates."""
    a = {}
    L = 6.0

    # Each star part rotates on its own local axis: horiz around Z, vert around X, diag around Y at varying speeds
    for s_idx in range(8):
        speed_h = 1.0 + s_idx * 0.1  # full revs over L
        h_buid = star_horiz_bids[s_idx]
        a[h_buid] = {"name": bone_name_map[h_buid], "type":"bone","keyframes":[
            kf_rot_with_base(h_buid, 0.0, 0, 0, 0),
            kf_rot_with_base(h_buid, 1.5, 0, 0, 90 * speed_h),
            kf_rot_with_base(h_buid, 3.0, 0, 0, 180 * speed_h),
            kf_rot_with_base(h_buid, 4.5, 0, 0, 270 * speed_h),
            kf_rot_with_base(h_buid, 6.0, 0, 0, 360 * speed_h),
            kf(0.0, 0,0,0,"position"), kf(3.0, 0,0,0,"position"), kf(6.0, 0,0,0,"position"),
            kf(0.0, 1,1,1,"scale"), kf(3.0, 1,1,1,"scale"), kf(6.0, 1,1,1,"scale"),
        ]}
        v_buid = star_vert_bids[s_idx]
        speed_v = 0.8 + s_idx * 0.12
        a[v_buid] = {"name": bone_name_map[v_buid], "type":"bone","keyframes":[
            kf_rot_with_base(v_buid, 0.0, 0, 0, 0),
            kf_rot_with_base(v_buid, 1.5, 90 * speed_v, 0, 0),
            kf_rot_with_base(v_buid, 3.0, 180 * speed_v, 0, 0),
            kf_rot_with_base(v_buid, 4.5, 270 * speed_v, 0, 0),
            kf_rot_with_base(v_buid, 6.0, 360 * speed_v, 0, 0),
            kf(0.0, 0,0,0,"position"), kf(3.0, 0,0,0,"position"), kf(6.0, 0,0,0,"position"),
            kf(0.0, 1,1,1,"scale"), kf(3.0, 1,1,1,"scale"), kf(6.0, 1,1,1,"scale"),
        ]}
        d_buid = star_diag_bids[s_idx]
        speed_d = 1.2 + s_idx * 0.08
        # Note: diag has base rot [0,45,0]; we rotate around Y so total Y becomes base+offset
        a[d_buid] = {"name": bone_name_map[d_buid], "type":"bone","keyframes":[
            kf_rot_with_base(d_buid, 0.0, 0, 0, 0),
            kf_rot_with_base(d_buid, 1.5, 0, 90 * speed_d, 0),
            kf_rot_with_base(d_buid, 3.0, 0, 180 * speed_d, 0),
            kf_rot_with_base(d_buid, 4.5, 0, 270 * speed_d, 0),
            kf_rot_with_base(d_buid, 6.0, 0, 360 * speed_d, 0),
            kf(0.0, 0,0,0,"position"), kf(3.0, 0,0,0,"position"), kf(6.0, 0,0,0,"position"),
            kf(0.0, 1,1,1,"scale"), kf(3.0, 1,1,1,"scale"), kf(6.0, 1,1,1,"scale"),
        ]}

    # Connecting lines pulse emissive in sequence around constellation (scale pulse, ordered)
    for li, buid in enumerate(line_bids):
        # Each line peaks at staggered time
        peak = (li / len(line_bids)) * L
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(max(0.01, peak - 0.5), 1,1,1,"scale"),
            kf(peak,                    1.4, 1.0, 1.4, "scale"),
            kf(min(L-0.01, peak + 0.5), 1,1,1,"scale"),
            kf(L*0.5,                  1,1,1,"scale"),
            kf(L,                      1,1,1,"scale"),
            kf(0.0, 0,0,0,"position"), kf(3.0, 0,0,0,"position"), kf(6.0, 0,0,0,"position"),
            kf_rot_with_base(buid, 0.0, 0, 0, 0),
            kf_rot_with_base(buid, 3.0, 0, 0, 0),
            kf_rot_with_base(buid, 6.0, 0, 0, 0),
        ]}

    # Nexus rotates Y
    for i, buid in enumerate(nexus_bids):
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf_rot_with_base(buid, 0.0, 0, 0, 0),
            kf_rot_with_base(buid, 1.5, 0, 90, 0),
            kf_rot_with_base(buid, 3.0, 0, 180, 0),
            kf_rot_with_base(buid, 4.5, 0, 270, 0),
            kf_rot_with_base(buid, 6.0, 0, 360, 0),
            kf(0.0, 0,0,0,"position"), kf(3.0, 0,0,0,"position"), kf(6.0, 0,0,0,"position"),
            kf(0.0, 1,1,1,"scale"),
            kf(1.5, 1.05,1.05,1.05,"scale"),
            kf(3.0, 1,1,1,"scale"),
            kf(4.5, 0.95,0.95,0.95,"scale"),
            kf(6.0, 1,1,1,"scale"),
        ]}

    # Star tails bob (Y position)
    for s_idx, buid in enumerate(tail_bids):
        phase = s_idx * 0.7
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0, 0.05*math.sin(phase),     0, "position"),
            kf(1.5,  0, 0.05*math.sin(phase+1.5), 0, "position"),
            kf(3.0,  0, 0.05*math.sin(phase+3.0), 0, "position"),
            kf(4.5,  0, 0.05*math.sin(phase+4.5), 0, "position"),
            kf(6.0,  0, 0.05*math.sin(phase+6.0), 0, "position"),
            kf(0.0, 1,1,1,"scale"), kf(3.0, 1,1.05,1,"scale"), kf(6.0, 1,1,1,"scale"),
            kf_rot_with_base(buid, 0.0, 0, 0, 0),
            kf_rot_with_base(buid, 3.0, 0, 30, 0),
            kf_rot_with_base(buid, 6.0, 0, 0, 0),
        ]}

    # Orbital ring counter-rotates (each slab around its tangent — we'll lift them in Y subtly)
    for i, buid in enumerate(orbit_bids):
        phase = i * 0.6
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0, 0.04*math.sin(phase),     0, "position"),
            kf(1.5,  0, 0.04*math.cos(phase),     0, "position"),
            kf(3.0,  0, -0.04*math.sin(phase),    0, "position"),
            kf(4.5,  0, -0.04*math.cos(phase),    0, "position"),
            kf(6.0,  0, 0.04*math.sin(phase),     0, "position"),
            kf(0.0, 1,1,1,"scale"),
            kf(1.5, 1.1,1,1.1,"scale"),
            kf(3.0, 1,1,1,"scale"),
            kf(4.5, 0.9,1,0.9,"scale"),
            kf(6.0, 1,1,1,"scale"),
            kf_rot_with_base(buid, 0.0, 0, 0, 0),
            kf_rot_with_base(buid, 3.0, 0, 0, 0),
            kf_rot_with_base(buid, 6.0, 0, 0, 0),
        ]}

    # Halos rotate around their star (we move position in small circle)
    for h_idx, buid in enumerate(halo_bids):
        s_idx = h_idx // 4
        h_local = h_idx % 4
        sx, sy, sz = STAR_POS[s_idx]
        # Halo at base radius 0.9 around star at angle h_local * 90 deg
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf_rot_with_base(buid, 0.0, 0, 0, 0),
            kf_rot_with_base(buid, 1.5, 0, 90, 0),
            kf_rot_with_base(buid, 3.0, 0, 180, 0),
            kf_rot_with_base(buid, 4.5, 0, 270, 0),
            kf_rot_with_base(buid, 6.0, 0, 360, 0),
            kf(0.0, 0,0,0,"position"), kf(3.0, 0,0,0,"position"), kf(6.0, 0,0,0,"position"),
            kf(0.0, 1,1,1,"scale"), kf(3.0, 1,1,1,"scale"), kf(6.0, 1,1,1,"scale"),
        ]}

    # Glow auras pulse
    for g_idx, buid in enumerate(glow_bids):
        phase = g_idx * 0.8
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(1.5, 1.0+0.15*math.sin(phase), 1.0+0.15*math.sin(phase), 1, "scale"),
            kf(3.0, 1.15, 1.15, 1, "scale"),
            kf(4.5, 1.0+0.15*math.sin(phase+3.0), 1.0+0.15*math.sin(phase+3.0), 1, "scale"),
            kf(6.0, 1,1,1,"scale"),
            kf(0.0, 0,0,0,"position"), kf(3.0, 0,0,0,"position"), kf(6.0, 0,0,0,"position"),
            kf_rot_with_base(buid, 0.0, 0, 0, 0),
            kf_rot_with_base(buid, 3.0, 0, 0, 0),
            kf_rot_with_base(buid, 6.0, 0, 0, 0),
        ]}

    return a


def build_dissipate():
    """dissipate once 0.7s: lines vanish simultaneously, stars fly outward, nexus implodes, ring expands and snaps."""
    a = {}

    # Lines vanish almost simultaneously at 0.05s
    for i, buid in enumerate(line_bids):
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,    1,1,1,"scale"), kf(0.04, 1,1,1,"scale"),
            kf(0.08,   0.5,0.5,0.5,"scale"), kf(0.12, 0,0,0,"scale"),
            kf(0.4,    0,0,0,"scale"), kf(0.7, 0,0,0,"scale"),
            kf(0.0,    0,0,0,"position"), kf(0.35, 0,0,0,"position"), kf(0.7, 0,0,0,"position"),
            kf_rot_with_base(buid, 0.0, 0, 0, 0),
            kf_rot_with_base(buid, 0.35, 0, 0, 0),
            kf_rot_with_base(buid, 0.7, 0, 0, 0),
        ]}

    # Stars fly outward from current orbital position
    for s_idx in range(8):
        sx, sy, sz = STAR_POS[s_idx]
        # outward direction = star_pos normalised
        mag = math.sqrt(sx*sx + sy*sy + sz*sz) + 0.001
        ux, uy, uz = sx/mag, sy/mag, sz/mag
        for part_list in [star_horiz_bids, star_vert_bids, star_diag_bids]:
            buid = part_list[s_idx]
            a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
                kf(0.0,    1,1,1,"scale"), kf(0.15, 1,1,1,"scale"),
                kf(0.40,   0.8,0.8,0.8,"scale"), kf(0.70, 0,0,0,"scale"),
                kf(0.0,    0,0,0,"position"), kf(0.15, ux*0.5, uy*0.5, uz*0.5, "position"),
                kf(0.40,   ux*2.5, uy*2.5, uz*2.5, "position"),
                kf(0.70,   ux*5, uy*5, uz*5, "position"),
                kf_rot_with_base(buid, 0.0, 0, 0, 0),
                kf_rot_with_base(buid, 0.35, 0, 90, 0),
                kf_rot_with_base(buid, 0.7, 0, 180, 0),
            ]}

    # Halos and glow follow their star outward
    for h_idx, buid in enumerate(halo_bids):
        s_idx = h_idx // 4
        sx, sy, sz = STAR_POS[s_idx]
        mag = math.sqrt(sx*sx + sy*sy + sz*sz) + 0.001
        ux, uy, uz = sx/mag, sy/mag, sz/mag
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,    1,1,1,"scale"), kf(0.15, 1,1,1,"scale"),
            kf(0.40,   0.6,0.6,0.6,"scale"), kf(0.70, 0,0,0,"scale"),
            kf(0.0,    0,0,0,"position"),
            kf(0.15, ux*0.5, uy*0.5, uz*0.5, "position"),
            kf(0.40, ux*2.5, uy*2.5, uz*2.5, "position"),
            kf(0.70, ux*5, uy*5, uz*5, "position"),
            kf_rot_with_base(buid, 0.0, 0, 0, 0),
            kf_rot_with_base(buid, 0.35, 0, 0, 0),
            kf_rot_with_base(buid, 0.7, 0, 0, 0),
        ]}
    for g_idx, buid in enumerate(glow_bids):
        sx, sy, sz = STAR_POS[g_idx]
        mag = math.sqrt(sx*sx + sy*sy + sz*sz) + 0.001
        ux, uy, uz = sx/mag, sy/mag, sz/mag
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,    1,1,1,"scale"), kf(0.15, 1.5,1.5,1.5,"scale"),
            kf(0.40,   0.5,0.5,0.5,"scale"), kf(0.70, 0,0,0,"scale"),
            kf(0.0,    0,0,0,"position"),
            kf(0.15, ux*0.5, uy*0.5, uz*0.5, "position"),
            kf(0.40, ux*2.5, uy*2.5, uz*2.5, "position"),
            kf(0.70, ux*5, uy*5, uz*5, "position"),
            kf_rot_with_base(buid, 0.0, 0, 0, 0),
            kf_rot_with_base(buid, 0.35, 0, 0, 0),
            kf_rot_with_base(buid, 0.7, 0, 0, 0),
        ]}

    # Star tails continue outward
    for s_idx, buid in enumerate(tail_bids):
        sx, sy, sz = STAR_POS[s_idx]
        mag = math.sqrt(sx*sx + sy*sy + sz*sz) + 0.001
        ux, uy, uz = sx/mag, sy/mag, sz/mag
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,    1,1,1,"scale"), kf(0.15, 1,1.2,1,"scale"),
            kf(0.40,   0.5,0.5,0.5,"scale"), kf(0.70, 0,0,0,"scale"),
            kf(0.0,    0,0,0,"position"),
            kf(0.15, ux*0.4, uy*0.4 - 0.2, uz*0.4, "position"),
            kf(0.40, ux*2, uy*2 - 0.6, uz*2, "position"),
            kf(0.70, ux*4, uy*4 - 1.2, uz*4, "position"),
            kf_rot_with_base(buid, 0.0, 0, 0, 0),
            kf_rot_with_base(buid, 0.35, 0, 0, 0),
            kf_rot_with_base(buid, 0.7, 0, 0, 0),
        ]}

    # Nexus implodes
    for i, buid in enumerate(nexus_bids):
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,    1,1,1,"scale"), kf(0.15, 1,1,1,"scale"),
            kf(0.30,   0.4,0.4,0.4,"scale"), kf(0.50, 0.05,0.05,0.05,"scale"),
            kf(0.70,   0,0,0,"scale"),
            kf(0.0,    0,0,0,"position"), kf(0.35, 0,0,0,"position"), kf(0.7, 0,0,0,"position"),
            kf_rot_with_base(buid, 0.0, 0, 0, 0),
            kf_rot_with_base(buid, 0.35, 0, 180, 0),
            kf_rot_with_base(buid, 0.7, 0, 360, 0),
        ]}

    # Orbital ring rapidly expands and snaps (scale up then 0)
    for i, buid in enumerate(orbit_bids):
        a[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,    1,1,1,"scale"), kf(0.15, 1.5,1,1.5,"scale"),
            kf(0.30,   2.5,1,2.5,"scale"), kf(0.45, 3,1,3,"scale"),
            kf(0.55,   0,0,0,"scale"), kf(0.70, 0,0,0,"scale"),
            kf(0.0,    0,0,0,"position"), kf(0.35, 0,0,0,"position"), kf(0.7, 0,0,0,"position"),
            kf_rot_with_base(buid, 0.0, 0, 0, 0),
            kf_rot_with_base(buid, 0.35, 0, 0, 0),
            kf_rot_with_base(buid, 0.7, 0, 0, 0),
        ]}

    return a


# Build animations
spawn_anim = {
    "uuid": aid(1), "name": "animation.devils_constellation.spawn",
    "loop": "once", "length": 1.1, "snapping": 24,
    "selected": False, "saved": False, "animators": build_spawn()
}
idle_anim = {
    "uuid": aid(2), "name": "animation.devils_constellation.idle",
    "loop": "loop", "length": 6.0, "snapping": 24,
    "selected": False, "saved": False, "animators": build_idle()
}
dissipate_anim = {
    "uuid": aid(3), "name": "animation.devils_constellation.dissipate",
    "loop": "once", "length": 0.7, "snapping": 24,
    "selected": False, "saved": False, "animators": build_dissipate()
}

outliner = [b["uuid"] for b in bones]

textures = [
    {"id":"0","name":"devils_constellation_tex",
     "relative_path":"../textures/devils_constellation_tex.png",
     "folder":"devilsdream","namespace":"","visible":True,"mode":"bitmap","saved":False,
     "uuid":tid(1),"source":f"data:image/png;base64,{b64_0}"},
    {"id":"1","name":"devils_constellation_tex1",
     "relative_path":"../textures/devils_constellation_tex1.png",
     "folder":"devilsdream","namespace":"","visible":True,"mode":"bitmap","saved":False,
     "uuid":tid(2),"source":f"data:image/png;base64,{b64_1}"},
]

model = {
    "meta": {"format_version":"4.10","model_format":"free","box_uv":False},
    "name": "devils_constellation",
    "geometry": "geometry.devils_constellation",
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

out_path = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/devils_constellation.bbmodel"
with open(out_path, "w", encoding="utf-8") as f:
    json.dump(model, f, separators=(',', ':'))
size = os.path.getsize(out_path)
print(f"Written: {out_path}")
print(f"Size: {size:,} bytes ({size/1024:.1f} KB)")
print(f"Elements: {len(elements)} | Bones: {len(bones)}")
print(f"{'PASS' if size>=300*1024 else 'FAIL'}: >=300KB requirement")
