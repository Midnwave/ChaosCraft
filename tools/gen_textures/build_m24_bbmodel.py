"""
Build infernal_scripture_array.bbmodel
Lectern at origin + 10 orbiting page slabs + corner curls + ink drops.

Layout:
  CENTRAL LECTERN (5 cubes): wide flat top, V-shape support, base block, two column ornaments.
  10 PAGE slabs orbiting at radii 4–7, heights 2–7. Each page = 2 overlapping slabs (outer + inner).
  Each page has 2 corner-curl bones (small cubes bent 22.5°).
  8 INK DROP bones orbiting at varied radii/heights.
  Each page has a "binding spine" cube on one edge.
  Lectern has additional ornament cubes on base.
  Ink drops have sub-pieces.

Goal: 80+ elements, 55+ bones, ≥300KB.
"""
import json, math, os

b64_0 = open("D:/CC/ChaosCraft/tools/gen_textures/m24_b64.txt").read().strip()
b64_1 = open("D:/CC/ChaosCraft/tools/gen_textures/m24_tex1_b64.txt").read().strip()

def eid(n): return f"e{n:03d}"
def bid(n): return f"b{n:03d}"
def aid(n): return f"a{n:03d}"
def kid(n): return f"k{n:04d}"
def tid(n): return f"t{n:03d}"

# Standard UV for tex0 (parchment text)
UV0 = {"north":{"uv":[2,2,30,30],"texture":0},"east":{"uv":[34,2,62,30],"texture":0},
       "south":{"uv":[2,2,30,30],"texture":0},"west":{"uv":[34,2,62,30],"texture":0},
       "up":{"uv":[2,34,30,62],"texture":0},"down":{"uv":[34,34,62,62],"texture":0}}

# UV0 emissive zone (purple ink) — for spines
UV0_EMIT = {"north":{"uv":[34,34,62,62],"texture":0},"east":{"uv":[34,34,62,62],"texture":0},
            "south":{"uv":[34,34,62,62],"texture":0},"west":{"uv":[34,34,62,62],"texture":0},
            "up":{"uv":[34,34,62,62],"texture":0},"down":{"uv":[34,34,62,62],"texture":0}}

# UV1 — wood from tex1
UV1 = {"north":{"uv":[2,2,30,30],"texture":1},"east":{"uv":[34,2,62,30],"texture":1},
       "south":{"uv":[2,2,30,30],"texture":1},"west":{"uv":[34,2,62,30],"texture":1},
       "up":{"uv":[2,34,30,62],"texture":1},"down":{"uv":[34,34,62,62],"texture":1}}

# UV1 emissive zone — purple ink drops
UV1_EMIT = {"north":{"uv":[34,34,62,62],"texture":1},"east":{"uv":[34,34,62,62],"texture":1},
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

# ── 1. CENTRAL LECTERN ─────────────────────────────────────────────────────────
# Lectern parts (their own bone group):
#   - top flat slab (book stand surface) at Y=2-2.5
#   - 2 V-shape support cubes meeting at bottom
#   - base block at Y=0-0.5
#   - 2 column ornaments
lectern_part_eids = []
lectern_part_bids = []

# Top flat slab — wide angled top (book rest)
e = elem(ei, "lectern_top", [-1.0, 2.0, -0.75], [1.0, 2.5, 0.75], UV1); elements.append(e)
b = bone(bi, "lectern_top", [0, 2.25, 0], [eid(ei)], rot=[18, 0, 0]); bones.append(b)
lectern_part_bids.append(bid(bi)); ei+=1; bi+=1

# V-shape supports — left and right angled inward
e = elem(ei, "lectern_supL", [-0.45, 0.5, -0.30], [-0.10, 2.0, 0.30], UV1); elements.append(e)
b = bone(bi, "lectern_supL", [-0.275, 1.25, 0], [eid(ei)], rot=[0, 0, -10]); bones.append(b)
lectern_part_bids.append(bid(bi)); ei+=1; bi+=1
e = elem(ei, "lectern_supR", [0.10, 0.5, -0.30], [0.45, 2.0, 0.30], UV1); elements.append(e)
b = bone(bi, "lectern_supR", [0.275, 1.25, 0], [eid(ei)], rot=[0, 0, 10]); bones.append(b)
lectern_part_bids.append(bid(bi)); ei+=1; bi+=1

# Base block — wide
e = elem(ei, "lectern_base", [-1.1, 0.0, -0.85], [1.1, 0.5, 0.85], UV1); elements.append(e)
b = bone(bi, "lectern_base", [0, 0.25, 0], [eid(ei)]); bones.append(b)
lectern_part_bids.append(bid(bi)); ei+=1; bi+=1

# Base ornaments (4 corner posts)
ornament_defs = [
    (-0.95, -0.10, -0.70, 0.20, 0.6, 0.20, "lectern_post_NW"),
    ( 0.75, -0.10, -0.70, 0.20, 0.6, 0.20, "lectern_post_NE"),
    (-0.95, -0.10,  0.50, 0.20, 0.6, 0.20, "lectern_post_SW"),
    ( 0.75, -0.10,  0.50, 0.20, 0.6, 0.20, "lectern_post_SE"),
]
for (cx, cy, cz, sx, sy, sz, name) in ornament_defs:
    fr = [cx, cy, cz]
    to = [cx + sx, cy + sy, cz + sz]
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    bnobj = bone(bi, name, [(fr[0]+to[0])/2, (fr[1]+to[1])/2, (fr[2]+to[2])/2], [eid(ei)]); bones.append(bnobj)
    lectern_part_bids.append(bid(bi)); ei+=1; bi+=1

# Column ornaments on V-supports
column_defs = [
    (-0.50, 1.20, -0.18, 0.10, 0.40, 0.36, "lectern_colL"),
    ( 0.40, 1.20, -0.18, 0.10, 0.40, 0.36, "lectern_colR"),
]
for (cx, cy, cz, sx, sy, sz, name) in column_defs:
    fr = [cx, cy, cz]
    to = [cx + sx, cy + sy, cz + sz]
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    bnobj = bone(bi, name, [(fr[0]+to[0])/2, (fr[1]+to[1])/2, (fr[2]+to[2])/2], [eid(ei)]); bones.append(bnobj)
    lectern_part_bids.append(bid(bi)); ei+=1; bi+=1

# Lectern top spine ornament (decoration in front)
e = elem(ei, "lectern_crest", [-0.30, 2.45, -0.65], [0.30, 2.85, -0.30], UV1_EMIT); elements.append(e)
b = bone(bi, "lectern_crest", [0, 2.65, -0.475], [eid(ei)], rot=[18, 0, 0]); bones.append(b)
lectern_part_bids.append(bid(bi)); ei+=1; bi+=1

# Total lectern parts so far: 10

# ── 2. PAGES (10 pages, each 2 overlapping slabs) ──────────────────────────────
# Pages orbit at varied radii/heights. Each has its own orbit bone (rotates around Y),
# and a body bone holding 2 page slabs.
# Page is oriented so its FLAT FACE points outward from lectern (i.e. local +X faces out).

page_specs = [
    # (radius, height, init_angle_deg)
    (4.0, 2.5,   0),
    (5.0, 3.0,  36),
    (6.0, 4.0,  72),
    (4.5, 5.0, 108),
    (5.5, 5.5, 144),
    (6.5, 4.5, 180),
    (7.0, 3.5, 216),
    (4.0, 6.0, 252),
    (5.0, 6.5, 288),
    (6.0, 7.0, 324),
]

page_orbit_bids = []   # parent bone (rotates around Y)
page_body_bids = []    # contains the 2 page slabs (rotates around its own Y for "page turning")
page_corner_bids = []  # 2 per page = 20

for pi, (r, h, init_ang) in enumerate(page_specs):
    # Outer page slab: 2.0 wide × 0.10 thick × 2.5 tall
    # Centred at (r, h, 0), thin in X axis (so flat face is YZ plane facing X+)
    body_cx = r; body_cy = h; body_cz = 0
    # outer slab
    fr1 = [body_cx - 0.05, body_cy - 1.25, body_cz - 1.0]
    to1 = [body_cx + 0.05, body_cy + 1.25, body_cz + 1.0]
    # inner slab (slightly smaller, offset slightly so 2 layers visible)
    fr2 = [body_cx + 0.05, body_cy - 1.15, body_cz - 0.90]
    to2 = [body_cx + 0.13, body_cy + 1.15, body_cz + 0.90]

    e1 = elem(ei, f"page{pi+1}_outer", fr1, to1, UV0); elements.append(e1); ei += 1
    e2 = elem(ei, f"page{pi+1}_inner", fr2, to2, UV0); elements.append(e2); ei += 1

    # Binding spine — 3rd small slab on one edge (top edge)
    fr_spine = [body_cx - 0.06, body_cy + 1.10, body_cz - 0.95]
    to_spine = [body_cx + 0.14, body_cy + 1.30, body_cz - 0.75]
    e3 = elem(ei, f"page{pi+1}_spine", fr_spine, to_spine, UV0_EMIT); elements.append(e3); ei += 1

    # Body bone — origin at page centre, contains the 3 page elements
    body_b = bone(bi, f"page{pi+1}_body", [body_cx, body_cy, body_cz],
                  [e1["uuid"], e2["uuid"], e3["uuid"]])
    bones.append(body_b)
    body_bid = bid(bi); bi += 1

    # 2 corner curl cubes (small) — at top-front and bottom-back diagonal corners
    corners_defs = [
        (body_cx + 0.05, body_cy + 0.95, body_cz + 0.85,
         body_cx + 0.20, body_cy + 1.20, body_cz + 1.05,
         f"page{pi+1}_curlA", [22.5, 0, 0]),
        (body_cx + 0.05, body_cy - 1.20, body_cz - 1.05,
         body_cx + 0.20, body_cy - 0.95, body_cz - 0.85,
         f"page{pi+1}_curlB", [-22.5, 0, 0]),
    ]
    page_curl_bids_local = []
    for (fx, fy, fz, tx, ty, tz, cname, crot) in corners_defs:
        fr = [fx, fy, fz]; to = [tx, ty, tz]
        e = elem(ei, cname, fr, to, UV0); elements.append(e)
        cb = bone(bi, cname, [(fx+tx)/2, (fy+ty)/2, (fz+tz)/2], [eid(ei)], rot=crot)
        bones.append(cb)
        page_corner_bids.append(bid(bi))
        page_curl_bids_local.append(bid(bi))
        ei += 1; bi += 1

    # Orbit bone — origin at (0, h, 0), rotates around Y. Children = body + corners
    orbit_b = bone(bi, f"page{pi+1}_orbit", [0, h, 0],
                   [body_bid] + page_curl_bids_local, rot=[0, init_ang, 0])
    bones.append(orbit_b)
    page_orbit_bids.append(bid(bi))
    page_body_bids.append(body_bid)
    bi += 1

# ── 3. INK DROPS (8 ink drops + 8 sub-pieces = 16 elements) ────────────────────
# Each ink drop has its own orbit bone and a body bone with main + sub piece.
ink_specs = [
    # (radius, height, init_angle_deg)
    (3.5, 3.5,  20),
    (4.0, 5.5,  60),
    (4.5, 4.5, 100),
    (5.0, 6.0, 140),
    (3.5, 6.0, 180),
    (5.5, 4.0, 220),
    (4.0, 3.0, 260),
    (4.5, 7.0, 300),
]
ink_orbit_bids = []
ink_body_bids = []
ink_sub_bids = []

for ii, (r, h, init_ang) in enumerate(ink_specs):
    body_cx = r; body_cy = h; body_cz = 0
    # main ink drop (small cube)
    fr = [body_cx - 0.18, body_cy - 0.18, body_cz - 0.18]
    to = [body_cx + 0.18, body_cy + 0.18, body_cz + 0.18]
    e_main = elem(ei, f"ink{ii+1}_drop", fr, to, UV1_EMIT); elements.append(e_main); ei += 1
    # sub piece (drip behind)
    fr_s = [body_cx - 0.10, body_cy - 0.30, body_cz + 0.20]
    to_s = [body_cx + 0.10, body_cy - 0.10, body_cz + 0.40]
    e_sub = elem(ei, f"ink{ii+1}_sub", fr_s, to_s, UV1_EMIT); elements.append(e_sub); ei += 1

    body_b = bone(bi, f"ink{ii+1}_body", [body_cx, body_cy, body_cz], [e_main["uuid"]])
    bones.append(body_b)
    body_bid = bid(bi); bi += 1

    sub_b = bone(bi, f"ink{ii+1}_sub", [body_cx, body_cy - 0.20, body_cz + 0.30], [e_sub["uuid"]])
    bones.append(sub_b)
    sub_bid = bid(bi); bi += 1

    orbit_b = bone(bi, f"ink{ii+1}_orbit", [0, h, 0], [body_bid, sub_bid], rot=[0, init_ang, 0])
    bones.append(orbit_b)
    ink_orbit_bids.append(bid(bi))
    ink_body_bids.append(body_bid)
    ink_sub_bids.append(sub_bid)
    bi += 1

# ── 4. EXTRA INK SPARKLES (8 small ones — orbiting independently) ─────────────
# Smaller cubes at higher orbits, brighter
sparkle_orbit_bids = []
sparkle_body_bids = []
sparkle_specs = [
    (3.0, 4.5,  10),
    (3.5, 5.0,  55),
    (4.0, 5.0, 100),
    (4.5, 5.5, 145),
    (5.0, 6.5, 190),
    (3.0, 6.5, 235),
    (4.0, 7.5, 280),
    (3.5, 4.0, 325),
]
for si, (r, h, init_ang) in enumerate(sparkle_specs):
    body_cx = r; body_cy = h; body_cz = 0
    fr = [body_cx - 0.10, body_cy - 0.10, body_cz - 0.10]
    to = [body_cx + 0.10, body_cy + 0.10, body_cz + 0.10]
    e = elem(ei, f"spark{si+1}", fr, to, UV1_EMIT); elements.append(e)
    body_b = bone(bi, f"spark{si+1}", [body_cx, body_cy, body_cz], [eid(ei)])
    bones.append(body_b)
    sparkle_body_bids.append(bid(bi)); ei += 1; bi += 1
    orbit_b = bone(bi, f"spark{si+1}_orbit", [0, h, 0], [bid(bi-1)], rot=[0, init_ang, 0])
    bones.append(orbit_b)
    sparkle_orbit_bids.append(bid(bi)); bi += 1

print(f"Elements: {len(elements)}, Bones: {len(bones)}")
assert len(elements) >= 80, f"Need >=80 got {len(elements)}"
assert len(bones) >= 55, f"Need >=55 got {len(bones)}"

bone_name_map = {b["uuid"]: b["name"] for b in bones}

all_bids = (lectern_part_bids + page_orbit_bids + page_body_bids + page_corner_bids +
            ink_orbit_bids + ink_body_bids + ink_sub_bids +
            sparkle_orbit_bids + sparkle_body_bids)

all_bone_uuids = set(b["uuid"] for b in bones)
missing = all_bone_uuids - set(all_bids)
extra = set(all_bids) - all_bone_uuids
assert not missing, f"missing: {missing}"
assert not extra, f"extra: {extra}"

# ══════════════════════════════════════════════════════════════════════════════
# Animations
# ══════════════════════════════════════════════════════════════════════════════
def standard_pos_zero(times):
    return [kf(t, 0, 0, 0, "position") for t in times]
def standard_rot_zero(times):
    return [kf(t, 0, 0, 0, "rotation") for t in times]
def standard_scale_one(times):
    return [kf(t, 1, 1, 1, "scale") for t in times]

# ─────────────── SPAWN 1.1s once ───────────────
def build_spawn():
    L = 1.1
    times = [0.0, 0.275, 0.55, 0.825, 1.1]
    animators = {}

    # Lectern parts — assemble at 0.2s (scale up 0→1, slight bounce)
    for i, buid in enumerate(lectern_part_bids):
        delay = 0.10 + i * 0.015
        # remember its rotation if any (lectern_top has rotation, supports too)
        # Simpler: animate scale only, leave rotation untouched in keyframes
        # We need rotation keyframes too. Get current rotation from bone obj.
        bobj = next(b for b in bones if b["uuid"] == buid)
        rx, ry, rz = bobj["rotation"]
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 0,0,0,"scale"),
            kf(delay, 0,0,0,"scale"),
            kf(delay+0.10, 1.2,1.2,1.2,"scale"),
            kf(delay+0.18, 0.95,0.95,0.95,"scale"),
            kf(0.45, 1,1,1,"scale"),
            kf(1.1, 1,1,1,"scale"),
            kf(0.0, rx, ry, rz, "rotation"),
            kf(0.275, rx, ry, rz, "rotation"),
            kf(0.55, rx, ry, rz, "rotation"),
            kf(0.825, rx, ry, rz, "rotation"),
            kf(1.1, rx, ry, rz, "rotation"),
            *standard_pos_zero(times),
        ]}

    # Pages — fly in from outside, each arriving with a flutter, 0.10s stagger
    for pi, buid in enumerate(page_orbit_bids):
        delay = 0.20 + pi * 0.07
        init_ang = page_specs[pi][2]
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(0.55, 1,1,1,"scale"),
            kf(1.1, 1,1,1,"scale"),
            kf(0.0, 0, init_ang, 0, "rotation"),
            kf(0.275, 0, init_ang, 0, "rotation"),
            kf(0.55, 0, init_ang, 0, "rotation"),
            kf(0.825, 0, init_ang, 0, "rotation"),
            kf(1.1, 0, init_ang, 0, "rotation"),
            *standard_pos_zero(times),
        ]}
    for pi, buid in enumerate(page_body_bids):
        delay = 0.20 + pi * 0.07
        # fly in from outside (radial position) — start far, end at 0
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 0,0,0,"scale"),
            kf(delay, 0,0,0,"scale"),
            kf(delay+0.05, 0.5,0.5,0.5,"scale"),
            kf(delay+0.15, 1.1,1.1,1.1,"scale"),
            kf(delay+0.25, 1,1,1,"scale"),
            kf(1.1, 1,1,1,"scale"),
            kf(0.0, 6, 0, 0, "position"),  # 6 units further out radially
            kf(delay, 6, 0, 0, "position"),
            kf(delay+0.20, 0, 0, 0, "position"),
            kf(0.85, 0, 0, 0, "position"),
            kf(1.1, 0, 0, 0, "position"),
            kf(0.0, 0, 0, 0, "rotation"),
            kf(delay+0.10, 15, 0, 0, "rotation"),
            kf(delay+0.20, -10, 0, 0, "rotation"),
            kf(delay+0.30, 0, 0, 0, "rotation"),
            kf(1.1, 0, 0, 0, "rotation"),
        ]}
    # Page corners — appear with page
    for ci, buid in enumerate(page_corner_bids):
        pi = ci // 2
        delay = 0.22 + pi * 0.07 + (ci % 2) * 0.02
        bobj = next(b for b in bones if b["uuid"] == buid)
        rx, ry, rz = bobj["rotation"]
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 0,0,0,"scale"),
            kf(delay, 0,0,0,"scale"),
            kf(delay+0.10, 1,1,1,"scale"),
            kf(0.85, 1,1,1,"scale"),
            kf(1.1, 1,1,1,"scale"),
            *standard_pos_zero(times),
            kf(0.0, rx, ry, rz, "rotation"),
            kf(0.275, rx, ry, rz, "rotation"),
            kf(0.55, rx, ry, rz, "rotation"),
            kf(0.825, rx, ry, rz, "rotation"),
            kf(1.1, rx, ry, rz, "rotation"),
        ]}

    # Ink drops materialise at 0.8s
    for i, buid in enumerate(ink_orbit_bids):
        delay = 0.80 + i * 0.015
        init_ang = ink_specs[i][2]
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(0.55, 1,1,1,"scale"),
            kf(1.1, 1,1,1,"scale"),
            kf(0.0, 0, init_ang, 0, "rotation"),
            kf(0.275, 0, init_ang, 0, "rotation"),
            kf(0.55, 0, init_ang, 0, "rotation"),
            kf(0.825, 0, init_ang, 0, "rotation"),
            kf(1.1, 0, init_ang+5, 0, "rotation"),
            *standard_pos_zero(times),
        ]}
    for i, buid in enumerate(ink_body_bids):
        delay = 0.80 + i * 0.015
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 0,0,0,"scale"),
            kf(delay, 0,0,0,"scale"),
            kf(delay+0.10, 1.3,1.3,1.3,"scale"),
            kf(delay+0.18, 1,1,1,"scale"),
            kf(1.0, 1,1,1,"scale"),
            kf(1.1, 1,1,1,"scale"),
            *standard_pos_zero(times), *standard_rot_zero(times),
        ]}
    for i, buid in enumerate(ink_sub_bids):
        delay = 0.82 + i * 0.012
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 0,0,0,"scale"),
            kf(delay, 0,0,0,"scale"),
            kf(delay+0.10, 1,1,1,"scale"),
            kf(1.0, 1,1,1,"scale"),
            kf(1.1, 1,1,1,"scale"),
            *standard_pos_zero(times), *standard_rot_zero(times),
        ]}

    # Sparkles — appear with ink drops
    for i, buid in enumerate(sparkle_orbit_bids):
        init_ang = sparkle_specs[i][2]
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(0.55, 1,1,1,"scale"),
            kf(1.1, 1,1,1,"scale"),
            kf(0.0, 0, init_ang, 0, "rotation"),
            kf(0.275, 0, init_ang, 0, "rotation"),
            kf(0.55, 0, init_ang, 0, "rotation"),
            kf(0.825, 0, init_ang, 0, "rotation"),
            kf(1.1, 0, init_ang, 0, "rotation"),
            *standard_pos_zero(times),
        ]}
    for i, buid in enumerate(sparkle_body_bids):
        delay = 0.85 + i * 0.012
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 0,0,0,"scale"),
            kf(delay, 0,0,0,"scale"),
            kf(delay+0.08, 1.3,1.3,1.3,"scale"),
            kf(delay+0.14, 1,1,1,"scale"),
            kf(1.05, 1,1,1,"scale"),
            kf(1.1, 1,1,1,"scale"),
            *standard_pos_zero(times), *standard_rot_zero(times),
        ]}

    return animators


# ─────────────── IDLE 6.0s loop ───────────────
def build_idle():
    """Pages orbit lectern (each at unique cycle in 6s). Pages also rotate slowly on own normal.
    Page corners curl further on staggered periods. Ink drops orbit. Lectern still."""
    L = 6.0
    animators = {}
    times = [0.0, 1.5, 3.0, 4.5, 6.0]

    # Lectern parts — fully still (but include keyframes)
    for buid in lectern_part_bids:
        bobj = next(b for b in bones if b["uuid"] == buid)
        rx, ry, rz = bobj["rotation"]
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(1.5, 1,1,1,"scale"),
            kf(3.0, 1,1,1,"scale"),
            kf(4.5, 1,1,1,"scale"),
            kf(6.0, 1,1,1,"scale"),
            kf(0.0, rx, ry, rz, "rotation"),
            kf(1.5, rx, ry, rz, "rotation"),
            kf(3.0, rx, ry, rz, "rotation"),
            kf(4.5, rx, ry, rz, "rotation"),
            kf(6.0, rx, ry, rz, "rotation"),
            *standard_pos_zero(times),
        ]}

    # Page orbit cycles — each unique. Use cycle counts 1,2,1,2,3,1,2,3,1,2 — but with phase offset given by init angle
    page_cycles = [1, 2, 1, 2, 3, 1, 2, 3, 1, 2]
    # Use unique periods that ALL complete in 6s — cycles must be integer or simple ratio. Use 1,2,3.
    for pi, buid in enumerate(page_orbit_bids):
        cycles = page_cycles[pi]
        init_ang = page_specs[pi][2]
        total = cycles * 360
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(3.0, 1,1,1,"scale"),
            kf(6.0, 1,1,1,"scale"),
            kf(0.0, 0, init_ang, 0, "rotation"),
            kf(1.5, 0, init_ang + total*0.25, 0, "rotation"),
            kf(3.0, 0, init_ang + total*0.5, 0, "rotation"),
            kf(4.5, 0, init_ang + total*0.75, 0, "rotation"),
            kf(6.0, 0, init_ang + total, 0, "rotation"),
            *standard_pos_zero(times),
        ]}

    # Page body — slow rotation on own normal axis (Y). Use unique spin counts.
    page_spins = [1, -1, 2, -2, 1, -1, 2, -2, 1, -1]  # spin or counter-spin
    for pi, buid in enumerate(page_body_bids):
        spin = page_spins[pi]
        total = spin * 360
        ph = pi * 0.6
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(1.5, 1+0.04*math.sin(ph),1,1+0.04*math.sin(ph),"scale"),
            kf(3.0, 1,1,1,"scale"),
            kf(4.5, 1+0.04*math.cos(ph),1,1+0.04*math.cos(ph),"scale"),
            kf(6.0, 1,1,1,"scale"),
            kf(0.0, 0, 0, 0, "rotation"),
            kf(1.5, 0, total*0.25, 0, "rotation"),
            kf(3.0, 0, total*0.5, 0, "rotation"),
            kf(4.5, 0, total*0.75, 0, "rotation"),
            kf(6.0, 0, total, 0, "rotation"),
            *standard_pos_zero(times),
        ]}

    # Page corners — curl further on staggered periods
    for ci, buid in enumerate(page_corner_bids):
        pi = ci // 2
        which = ci % 2
        bobj = next(b for b in bones if b["uuid"] == buid)
        rx, ry, rz = bobj["rotation"]
        # Add ±15° additional curl, staggered phase
        ph = ci * 0.5
        amp = 12  # additional degrees
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(3.0, 1,1,1,"scale"),
            kf(6.0, 1,1,1,"scale"),
            kf(0.0, rx + amp*math.sin(ph), ry, rz, "rotation"),
            kf(1.5, rx + amp*math.sin(ph+1.0), ry, rz, "rotation"),
            kf(3.0, rx + amp*math.sin(ph+2.0), ry, rz, "rotation"),
            kf(4.5, rx + amp*math.sin(ph+3.0), ry, rz, "rotation"),
            kf(6.0, rx + amp*math.sin(ph+4.0), ry, rz, "rotation"),
            *standard_pos_zero(times),
        ]}

    # Ink drops — orbit at unique radii/heights
    ink_cycles = [2, 3, 2, 4, 3, 2, 3, 4]
    for i, buid in enumerate(ink_orbit_bids):
        cycles = ink_cycles[i]
        init_ang = ink_specs[i][2]
        total = cycles * 360
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(3.0, 1,1,1,"scale"),
            kf(6.0, 1,1,1,"scale"),
            kf(0.0, 0, init_ang, 0, "rotation"),
            kf(1.5, 0, init_ang + total*0.25, 0, "rotation"),
            kf(3.0, 0, init_ang + total*0.5, 0, "rotation"),
            kf(4.5, 0, init_ang + total*0.75, 0, "rotation"),
            kf(6.0, 0, init_ang + total, 0, "rotation"),
            *standard_pos_zero(times),
        ]}
    # Ink body — tumble independently
    for i, buid in enumerate(ink_body_bids):
        ph = i * 0.7
        spin_total = (i+1) * 360 * 2
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(1.5, 1+0.10*math.sin(ph),1+0.10*math.sin(ph),1+0.10*math.sin(ph),"scale"),
            kf(3.0, 1,1,1,"scale"),
            kf(4.5, 1+0.08*math.cos(ph),1+0.08*math.cos(ph),1+0.08*math.cos(ph),"scale"),
            kf(6.0, 1,1,1,"scale"),
            kf(0.0, 0, 0, 0, "rotation"),
            kf(1.5, spin_total*0.25, spin_total*0.30, spin_total*0.20, "rotation"),
            kf(3.0, spin_total*0.5, spin_total*0.6, spin_total*0.4, "rotation"),
            kf(4.5, spin_total*0.75, spin_total*0.9, spin_total*0.6, "rotation"),
            kf(6.0, spin_total, spin_total, spin_total*0.8, "rotation"),
            *standard_pos_zero(times),
        ]}
    for i, buid in enumerate(ink_sub_bids):
        ph = i * 0.5
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(1.5, 1+0.08*math.sin(ph),1+0.08*math.sin(ph),1+0.08*math.sin(ph),"scale"),
            kf(3.0, 1,1,1,"scale"),
            kf(4.5, 1+0.06*math.cos(ph),1+0.06*math.cos(ph),1+0.06*math.cos(ph),"scale"),
            kf(6.0, 1,1,1,"scale"),
            kf(0.0, 0,0,0,"rotation"),
            kf(1.5, 30, 60, 0, "rotation"),
            kf(3.0, 60, 120, 0, "rotation"),
            kf(4.5, 90, 180, 0, "rotation"),
            kf(6.0, 0,0,0,"rotation"),
            *standard_pos_zero(times),
        ]}

    # Sparkles — orbit independently
    sparkle_cycles = [3, 4, 3, 5, 4, 3, 4, 5]
    for i, buid in enumerate(sparkle_orbit_bids):
        cycles = sparkle_cycles[i]
        init_ang = sparkle_specs[i][2]
        total = cycles * 360
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(3.0, 1,1,1,"scale"),
            kf(6.0, 1,1,1,"scale"),
            kf(0.0, 0, init_ang, 0, "rotation"),
            kf(1.5, 0, init_ang + total*0.25, 0, "rotation"),
            kf(3.0, 0, init_ang + total*0.5, 0, "rotation"),
            kf(4.5, 0, init_ang + total*0.75, 0, "rotation"),
            kf(6.0, 0, init_ang + total, 0, "rotation"),
            *standard_pos_zero(times),
        ]}
    for i, buid in enumerate(sparkle_body_bids):
        ph = i * 0.6
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(1.5, 1+0.20*math.sin(ph),1+0.20*math.sin(ph),1+0.20*math.sin(ph),"scale"),
            kf(3.0, 1,1,1,"scale"),
            kf(4.5, 1+0.20*math.sin(ph+1),1+0.20*math.sin(ph+1),1+0.20*math.sin(ph+1),"scale"),
            kf(6.0, 1,1,1,"scale"),
            *standard_pos_zero(times), *standard_rot_zero(times),
        ]}

    return animators


# ─────────────── DISSIPATE 0.8s once ───────────────
def build_dissipate():
    """Pages scatter outward, each on tangent + rotating. Ink drops scatter. Lectern collapses inward."""
    animators = {}
    times = [0.0, 0.2, 0.4, 0.6, 0.8]

    # Lectern parts — collapse inward (move to origin), shrink to 0
    for i, buid in enumerate(lectern_part_bids):
        bobj = next(b for b in bones if b["uuid"] == buid)
        rx, ry, rz = bobj["rotation"]
        # collapse: move toward (0, 0, 0) — so inverse of origin
        bx, by, bz = bobj["origin"]
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(0.20, 0.9,0.9,0.9,"scale"),
            kf(0.40, 0.6,0.6,0.6,"scale"),
            kf(0.60, 0.3,0.3,0.3,"scale"),
            kf(0.80, 0,0,0,"scale"),
            kf(0.0, 0, 0, 0, "position"),
            kf(0.20, -bx*0.2, -by*0.2, -bz*0.2, "position"),
            kf(0.40, -bx*0.5, -by*0.5, -bz*0.5, "position"),
            kf(0.60, -bx*0.8, -by*0.8, -bz*0.8, "position"),
            kf(0.80, -bx, -by, -bz, "position"),
            kf(0.0, rx, ry, rz, "rotation"),
            kf(0.40, rx, ry+30, rz, "rotation"),
            kf(0.80, rx, ry+60, rz, "rotation"),
        ]}

    # Pages — scatter outward on tangent + rotate
    for pi, buid in enumerate(page_orbit_bids):
        init_ang = page_specs[pi][2]
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(0.40, 1,1,1,"scale"),
            kf(0.80, 1,1,1,"scale"),
            kf(0.0, 0, init_ang, 0, "rotation"),
            kf(0.20, 0, init_ang+30, 0, "rotation"),
            kf(0.40, 0, init_ang+60, 0, "rotation"),
            kf(0.60, 0, init_ang+90, 0, "rotation"),
            kf(0.80, 0, init_ang+120, 0, "rotation"),
            *standard_pos_zero(times),
        ]}
    # Page bodies — fly tangentially outward (local +Z is tangent direction in our coord frame)
    for pi, buid in enumerate(page_body_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(0.30, 1.1,1.1,1.1,"scale"),
            kf(0.60, 0.5,0.5,0.5,"scale"),
            kf(0.80, 0,0,0,"scale"),
            kf(0.0, 0, 0, 0, "position"),
            kf(0.20, 0, 0, 1, "position"),
            kf(0.40, 0, 0, 3, "position"),
            kf(0.60, 0, 1, 5, "position"),
            kf(0.80, 0, 2, 8, "position"),
            kf(0.0, 0, 0, 0, "rotation"),
            kf(0.40, 90, 60, 30, "rotation"),
            kf(0.80, 180, 120, 60, "rotation"),
        ]}
    for ci, buid in enumerate(page_corner_bids):
        bobj = next(b for b in bones if b["uuid"] == buid)
        rx, ry, rz = bobj["rotation"]
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(0.30, 1,1,1,"scale"),
            kf(0.60, 0.4,0.4,0.4,"scale"),
            kf(0.80, 0,0,0,"scale"),
            *standard_pos_zero(times),
            kf(0.0, rx, ry, rz, "rotation"),
            kf(0.40, rx + 30, ry, rz, "rotation"),
            kf(0.80, rx + 60, ry, rz, "rotation"),
        ]}

    # Ink drops — scatter
    for i, buid in enumerate(ink_orbit_bids):
        init_ang = ink_specs[i][2]
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(0.40, 1,1,1,"scale"),
            kf(0.80, 1,1,1,"scale"),
            kf(0.0, 0, init_ang, 0, "rotation"),
            kf(0.20, 0, init_ang+45, 0, "rotation"),
            kf(0.40, 0, init_ang+90, 0, "rotation"),
            kf(0.60, 0, init_ang+135, 0, "rotation"),
            kf(0.80, 0, init_ang+180, 0, "rotation"),
            *standard_pos_zero(times),
        ]}
    for i, buid in enumerate(ink_body_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(0.30, 1.3,1.3,1.3,"scale"),
            kf(0.60, 0.5,0.5,0.5,"scale"),
            kf(0.80, 0,0,0,"scale"),
            kf(0.0, 0, 0, 0, "position"),
            kf(0.40, 0, 0, 2, "position"),
            kf(0.80, 0, 0, 5, "position"),
            kf(0.0, 0,0,0,"rotation"),
            kf(0.40, 60, 60, 60, "rotation"),
            kf(0.80, 120, 120, 120, "rotation"),
        ]}
    for i, buid in enumerate(ink_sub_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(0.30, 1,1,1,"scale"),
            kf(0.60, 0.3,0.3,0.3,"scale"),
            kf(0.80, 0,0,0,"scale"),
            *standard_pos_zero(times), *standard_rot_zero(times),
        ]}

    # Sparkles — scatter quickly
    for i, buid in enumerate(sparkle_orbit_bids):
        init_ang = sparkle_specs[i][2]
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(0.40, 1,1,1,"scale"),
            kf(0.80, 1,1,1,"scale"),
            kf(0.0, 0, init_ang, 0, "rotation"),
            kf(0.20, 0, init_ang+60, 0, "rotation"),
            kf(0.40, 0, init_ang+120, 0, "rotation"),
            kf(0.60, 0, init_ang+180, 0, "rotation"),
            kf(0.80, 0, init_ang+240, 0, "rotation"),
            *standard_pos_zero(times),
        ]}
    for i, buid in enumerate(sparkle_body_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(0.20, 1.5,1.5,1.5,"scale"),
            kf(0.50, 0.6,0.6,0.6,"scale"),
            kf(0.80, 0,0,0,"scale"),
            *standard_pos_zero(times), *standard_rot_zero(times),
        ]}

    return animators


# ── Build model ──────────────────────────────────────────────────────────────
spawn_anim = {
    "uuid": aid(1), "name": "animation.infernal_scripture_array.spawn",
    "loop": "once", "length": 1.1, "snapping": 24,
    "selected": False, "saved": False, "animators": build_spawn()
}
idle_anim = {
    "uuid": aid(2), "name": "animation.infernal_scripture_array.idle",
    "loop": "loop", "length": 6.0, "snapping": 24,
    "selected": False, "saved": False, "animators": build_idle()
}
dissipate_anim = {
    "uuid": aid(3), "name": "animation.infernal_scripture_array.dissipate",
    "loop": "once", "length": 0.8, "snapping": 24,
    "selected": False, "saved": False, "animators": build_dissipate()
}

# Verify animators cover all bones
for anim in [spawn_anim, idle_anim, dissipate_anim]:
    keyed = set(anim["animators"].keys())
    if keyed != set(b["uuid"] for b in bones):
        missing = set(b["uuid"] for b in bones) - keyed
        print(f"ANIM {anim['name']} missing bones: {[bone_name_map[m] for m in missing]}")
        assert False

# Outliner top-level
all_child_bids = set()
for b in bones:
    for c in b["children"]:
        if isinstance(c, str) and c.startswith("b"):
            all_child_bids.add(c)
outliner = [b["uuid"] for b in bones if b["uuid"] not in all_child_bids]

textures = [
    {"id":"0","name":"infernal_scripture_array_tex","relative_path":"../textures/infernal_scripture_array_tex.png",
     "folder":"devilsdream","namespace":"","visible":True,"mode":"bitmap","saved":False,
     "uuid":tid(1),"source":f"data:image/png;base64,{b64_0}"},
    {"id":"1","name":"infernal_scripture_array_tex1","relative_path":"../textures/infernal_scripture_array_tex1.png",
     "folder":"devilsdream","namespace":"","visible":True,"mode":"bitmap","saved":False,
     "uuid":tid(2),"source":f"data:image/png;base64,{b64_1}"},
]

model = {
    "meta": {"format_version":"4.10","model_format":"free","box_uv":False},
    "name": "infernal_scripture_array",
    "geometry": "geometry.infernal_scripture_array",
    "resolution": {"width":64,"height":64},
    "elements": elements,
    "outliner": outliner,
    "textures": textures,
    "animations": [spawn_anim, idle_anim, dissipate_anim]
}

out_path = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/infernal_scripture_array.bbmodel"
with open(out_path, "w", encoding="utf-8") as f:
    json.dump(model, f, separators=(',', ':'))

size = os.path.getsize(out_path)
print(f"Written: {out_path}")
print(f"Elements: {len(elements)} / Bones: {len(bones)}")
print(f"Animations: 3 (spawn 1.1s once, idle 6.0s loop, dissipate 0.8s once)")
print(f"Size: {size:,} bytes ({size/1024:.1f} KB)")
pass_fail = "PASS" if size >= 300*1024 else "FAIL"
print(f"{pass_fail}: {'>=300KB' if size>=300*1024 else f'only {size//1024}KB'}")
