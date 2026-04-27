"""Build nightmare_root_surge.bbmodel — 80 elements, 56 bones, 3 animations, 300KB+ target."""
import json, math, os

TEX0_B64 = open("D:/CC/ChaosCraft/tools/gen_textures/m02_b64.txt").read().strip()
TEX1_B64 = open("D:/CC/ChaosCraft/tools/gen_textures/m02_tex1_b64.txt").read().strip()

UV_NORTH  = [2, 2, 30, 30]
UV_EAST   = [34, 2, 62, 30]
UV_SOUTH  = [2, 2, 30, 30]
UV_WEST   = [34, 2, 62, 30]
UV_UP     = [2, 34, 30, 62]
UV_DOWN   = [34, 34, 62, 62]
UV_EMISSIVE = [34, 34, 62, 62]

def faces(tex=0, emissive=False):
    if emissive:
        return {d: {"uv": UV_EMISSIVE, "texture": tex} for d in ["north","east","south","west","up","down"]}
    return {
        "north": {"uv": UV_NORTH, "texture": tex},
        "east":  {"uv": UV_EAST,  "texture": tex},
        "south": {"uv": UV_SOUTH, "texture": tex},
        "west":  {"uv": UV_WEST,  "texture": tex},
        "up":    {"uv": UV_UP,    "texture": tex},
        "down":  {"uv": UV_DOWN,  "texture": tex},
    }

def elem(uuid, name, frm, to, tex=0, emissive=False):
    return {
        "name": name, "box_uv": False, "rescale": False, "locked": False,
        "render_order": "default", "allow_mirror_modeling": True,
        "from": frm, "to": to, "autouv": 0, "color": 0, "uuid": uuid,
        "faces": faces(tex, emissive)
    }

def bone(uuid, name, origin, children, rotation=None):
    b = {"name": name, "origin": origin, "uuid": uuid,
         "export": True, "isOpen": True, "children": children}
    b["rotation"] = rotation if rotation else [0, 0, 0]
    return b

def kf(uid, time, channel, vals):
    return {"uuid": uid, "time": time, "color": -1, "interpolation": "catmullrom",
            "channel": channel,
            "data_points": [{c: str(v) for c, v in zip("xyz", vals)}]}

elements = []
bones_list = []
e_idx = 1
b_idx = 1
k_idx = 1

def eid():
    global e_idx
    s = f"e{e_idx:03d}"; e_idx += 1; return s

def bid():
    global b_idx
    s = f"b{b_idx:03d}"; b_idx += 1; return s

def kid():
    global k_idx
    s = f"k{k_idx:04d}"; k_idx += 1; return s

# ── GEOMETRY ────────────────────────────────────────────────────────────────

# 1) centre_knot bone: 5 cubes 1.2-2.0 wide, tangled at ground Y=0-1
knot_elems = []
for i in range(5):
    w = 1.2 + i * 0.2
    h = 0.8 + i * 0.15
    off_x = (i - 2) * 0.3
    off_z = (i - 2) * 0.25
    eid_v = eid()
    frm = [off_x - w/2, 0.0, off_z - w/2]
    to  = [off_x + w/2, h,   off_z + w/2]
    elements.append(elem(eid_v, f"knot_cube_{i+1}", frm, to, tex=0))
    knot_elems.append(eid_v)
knot_bone_id = bid()
bones_list.append(bone(knot_bone_id, "centre_knot", [0, 0, 0], knot_elems))

# 2) 5 root chains — 72° apart in XZ plane
# Segment distances from origin: 1.5, 3.2, 5.0, 6.8, 8.5
# Each segment: progressive Y rise +1.5, alternating ±1.5 sideways
SEGMENT_DISTS  = [1.5, 3.2, 5.0, 6.8, 8.5]
SEGMENT_WIDTHS = [1.8, 1.5, 1.2, 0.8, 0.5]
SEGMENT_HEIGHTS= [1.8, 1.5, 1.2, 0.8, 0.5]

root_segment_bone_ids = []  # root_segment_bone_ids[root][seg] = bid_val
root_node_bone_ids    = []  # root_node_bone_ids[root][node] = bid_val
root_tip_bone_ids     = []  # root_tip_bone_ids[root] = bid_val
root_base_bulge_ids   = []  # root_base_bulge_ids[root] = bid_val

for root_i in range(5):
    root_angle_deg = root_i * 72.0
    root_angle = math.radians(root_angle_deg)
    # Perpendicular direction (for sideways offset)
    perp_angle = root_angle + math.pi / 2

    seg_bone_ids_this_root = []
    node_bone_ids_this_root = []

    # -- base bulge bone at root origin
    bulge_eid = eid()
    bx = math.sin(root_angle) * 1.0
    bz = math.cos(root_angle) * 1.0
    elements.append(elem(bulge_eid, f"root_{root_i+1}_bulge", [-1.0, 0, -1.0], [1.0, 1.8, 1.0], tex=0))
    bulge_bid = bid()
    root_base_bulge_ids.append(bulge_bid)
    bones_list.append(bone(bulge_bid, f"root_{root_i+1}_base_bulge", [bx, 0.0, bz], [bulge_eid]))

    for seg_j in range(5):
        dist = SEGMENT_DISTS[seg_j]
        w    = SEGMENT_WIDTHS[seg_j]
        h    = SEGMENT_HEIGHTS[seg_j]
        # Sideways offset alternates ±1.5 per segment
        side_sign = 1.0 if seg_j % 2 == 0 else -1.0
        side_offset = side_sign * 1.5
        # Position: radial + sideways perpendicular
        sx = math.sin(root_angle) * dist + math.cos(perp_angle) * side_offset
        sz = math.cos(root_angle) * dist - math.sin(perp_angle) * side_offset
        sy = seg_j * 1.5  # Y rise: 0, 1.5, 3.0, 4.5, 6.0

        seg_eid = eid()
        frm = [-w/2, 0.0, -w/2]
        to  = [ w/2, h,    w/2]
        elements.append(elem(seg_eid, f"root_{root_i+1}_seg_{seg_j+1}", frm, to, tex=0))
        seg_bid = bid()
        seg_bone_ids_this_root.append(seg_bid)
        bones_list.append(bone(seg_bid, f"root_{root_i+1}_s{seg_j+1}", [sx, sy, sz], [seg_eid],
                               rotation=[0, root_angle_deg + seg_j * 8.0, 0]))

    root_segment_bone_ids.append(seg_bone_ids_this_root)

    # -- 3 growth nodes per root — positioned at seg 1, 2, 3 locations
    node_ids_this_root = []
    for node_k in range(3):
        seg_idx = node_k + 1  # nodes at segments 1, 2, 3
        dist_n = SEGMENT_DISTS[seg_idx]
        w_n    = SEGMENT_WIDTHS[seg_idx]
        side_sign_n = 1.0 if seg_idx % 2 == 0 else -1.0
        side_off_n  = side_sign_n * 1.5
        nx = math.sin(root_angle) * dist_n + math.cos(perp_angle) * side_off_n
        nz = math.cos(root_angle) * dist_n - math.sin(perp_angle) * side_off_n
        ny = seg_idx * 1.5
        # Side offset for node protrusion: perpendicular to root
        node_perp_x = math.cos(root_angle) * 0.8
        node_perp_z = -math.sin(root_angle) * 0.8
        # Two small cubes off the root side
        n_eid1 = eid()
        n_eid2 = eid()
        s = 0.4
        elements.append(elem(n_eid1, f"root_{root_i+1}_node_{node_k+1}_a",
                              [node_perp_x-s/2, 0, node_perp_z-s/2],
                              [node_perp_x+s/2, s, node_perp_z+s/2], tex=0))
        elements.append(elem(n_eid2, f"root_{root_i+1}_node_{node_k+1}_b",
                              [node_perp_x-s/4, s*0.5, node_perp_z-s/4],
                              [node_perp_x+s/4, s*1.5, node_perp_z+s/4], tex=0))
        node_bid = bid()
        node_ids_this_root.append(node_bid)
        bones_list.append(bone(node_bid, f"root_{root_i+1}_node_{node_k+1}",
                               [nx, ny, nz], [n_eid1, n_eid2]))
    root_node_bone_ids.append(node_ids_this_root)

    # -- tip bone: 2 cubes splayed outward
    tip_dist = SEGMENT_DISTS[-1] + 1.5
    tip_side = -1.5
    tx = math.sin(root_angle) * tip_dist + math.cos(perp_angle) * tip_side
    tz = math.cos(root_angle) * tip_dist - math.sin(perp_angle) * tip_side
    ty = 4 * 1.5
    t_eid1 = eid()
    t_eid2 = eid()
    s = 0.4
    elements.append(elem(t_eid1, f"root_{root_i+1}_tip_a",
                          [-s, 0.0, -s], [s, s*1.5, s], tex=0))
    elements.append(elem(t_eid2, f"root_{root_i+1}_tip_b",
                          [s*0.5, 0.0, -s*0.5], [s*1.5, s*2, s*0.5], tex=0))
    tip_bid = bid()
    root_tip_bone_ids.append(tip_bid)
    bones_list.append(bone(tip_bid, f"root_{root_i+1}_tip", [tx, ty, tz], [t_eid1, t_eid2]))

# 3) 5 ground_crack bones — flat slabs at Y=0, tex1 emissive
crack_bone_ids = []
for i in range(5):
    crack_angle_deg = i * 72.0 + 36.0  # between root bases
    crack_angle = math.radians(crack_angle_deg)
    cr = 3.0
    cx = math.sin(crack_angle) * cr
    cz = math.cos(crack_angle) * cr
    c_eid = eid()
    # Flat slab 0.15 tall × 2.5 wide × 0.4 deep
    frm = [-1.25, -0.075, -0.2]
    to  = [ 1.25,  0.075,  0.2]
    elements.append(elem(c_eid, f"ground_crack_{i+1}", frm, to, tex=1, emissive=True))
    c_bid = bid()
    crack_bone_ids.append(c_bid)
    bones_list.append(bone(c_bid, f"ground_crack_{i+1}", [cx, 0.0, cz], [c_eid],
                           rotation=[0, crack_angle_deg, 0]))

print(f"Elements: {len(elements)}, Bones: {len(bones_list)}")

# Verify footprint: max X/Z reach should be at least ±3
max_reach = max(SEGMENT_DISTS) + 2.0
print(f"Max footprint reach: ±{max_reach:.1f} units")

# ── ANIMATIONS ──────────────────────────────────────────────────────────────

def make_spawn():
    """spawn: once, 1.2s."""
    animators = {}

    # centre knot: slams up at 0.0s
    animators[knot_bone_id] = {
        "name": "centre_knot", "type": "bone", "keyframes": [
            kf(kid(), 0.0,  "position", [0,-3,0]),
            kf(kid(), 0.15, "position", [0, 0.5, 0]),
            kf(kid(), 0.3,  "position", [0, 0, 0]),
            kf(kid(), 1.2,  "position", [0, 0, 0]),
            kf(kid(), 0.0,  "scale",    [0,0,0]),
            kf(kid(), 0.15, "scale",    [1.2,1.2,1.2]),
            kf(kid(), 0.3,  "scale",    [1,1,1]),
            kf(kid(), 0.0,  "rotation", [0,0,0]),
            kf(kid(), 0.3,  "rotation", [0,15,0]),
            kf(kid(), 1.2,  "rotation", [0,30,0]),
        ]
    }

    # root base bulge bones: emerge at 0.05s
    for i, b_id in enumerate(root_base_bulge_ids):
        bname = f"root_{i+1}_base_bulge"
        animators[b_id] = {
            "name": bname, "type": "bone", "keyframes": [
                kf(kid(), 0.0,  "position", [0,-2,0]),
                kf(kid(), 0.05, "position", [0,-2,0]),
                kf(kid(), 0.3,  "position", [0, 0, 0]),
                kf(kid(), 1.2,  "position", [0, 0, 0]),
                kf(kid(), 0.0,  "scale",    [0,0,0]),
                kf(kid(), 0.3,  "scale",    [1,1,1]),
                kf(kid(), 0.0,  "rotation", [0, i*72.0, 0]),
                kf(kid(), 1.2,  "rotation", [0, i*72.0, 0]),
            ]
        }

    # root segments: extend from origin outward — first seg of each root at 0.2s, 0.08s stagger per segment, 0.06s between roots
    for root_i in range(5):
        root_start = 0.2 + root_i * 0.06
        for seg_j in range(5):
            b_id = root_segment_bone_ids[root_i][seg_j]
            bname = f"root_{root_i+1}_s{seg_j+1}"
            t_seg_start = root_start + seg_j * 0.08
            t_seg_land  = min(t_seg_start + 0.25, 1.1)
            animators[b_id] = {
                "name": bname, "type": "bone", "keyframes": [
                    kf(kid(), 0.0,         "position", [0,-4,0]),
                    kf(kid(), t_seg_start, "position", [0,-4,0]),
                    kf(kid(), t_seg_land,  "position", [0, 0, 0]),
                    kf(kid(), 1.2,         "position", [0, 0, 0]),
                    kf(kid(), 0.0,         "scale",    [0,0,0]),
                    kf(kid(), t_seg_land,  "scale",    [1,1,1]),
                    kf(kid(), 0.0,         "rotation", [0, root_i*72.0+seg_j*8.0, 0]),
                    kf(kid(), 1.2,         "rotation", [0, root_i*72.0+seg_j*8.0, 0]),
                ]
            }

    # growth nodes: pop out as their root segment arrives
    for root_i in range(5):
        root_start = 0.2 + root_i * 0.06
        for node_k in range(3):
            b_id = root_node_bone_ids[root_i][node_k]
            bname = f"root_{root_i+1}_node_{node_k+1}"
            seg_idx = node_k + 1
            t_node = root_start + seg_idx * 0.08 + 0.05
            t_land = min(t_node + 0.2, 1.1)
            animators[b_id] = {
                "name": bname, "type": "bone", "keyframes": [
                    kf(kid(), 0.0,    "scale",    [0,0,0]),
                    kf(kid(), t_node, "scale",    [0,0,0]),
                    kf(kid(), t_land, "scale",    [1.2,1.2,1.2]),
                    kf(kid(), 1.2,    "scale",    [1,1,1]),
                    kf(kid(), 0.0,    "position", [0,-1,0]),
                    kf(kid(), t_land, "position", [0, 0, 0]),
                    kf(kid(), 0.0,    "rotation", [0,0,0]),
                    kf(kid(), 1.2,    "rotation", [0, node_k*30.0, 0]),
                ]
            }

    # tip bones: open at final position
    for i, b_id in enumerate(root_tip_bone_ids):
        bname = f"root_{i+1}_tip"
        t_tip = 0.2 + i * 0.06 + 4 * 0.08
        t_land= min(t_tip + 0.25, 1.15)
        animators[b_id] = {
            "name": bname, "type": "bone", "keyframes": [
                kf(kid(), 0.0,    "scale",    [0,0,0]),
                kf(kid(), t_tip,  "scale",    [0,0,0]),
                kf(kid(), t_land, "scale",    [1.3,1,1.3]),
                kf(kid(), 1.2,    "scale",    [1,1,1]),
                kf(kid(), 0.0,    "position", [0,-2,0]),
                kf(kid(), t_land, "position", [0, 0, 0]),
                kf(kid(), 0.0,    "rotation", [0, i*72.0, 0]),
                kf(kid(), 1.2,    "rotation", [0, i*72.0+15, 0]),
            ]
        }

    # ground cracks: radiate outward at 0.1s
    for i, b_id in enumerate(crack_bone_ids):
        bname = f"ground_crack_{i+1}"
        t_crack = 0.1 + i * 0.04
        t_land  = min(t_crack + 0.3, 1.0)
        animators[b_id] = {
            "name": bname, "type": "bone", "keyframes": [
                kf(kid(), 0.0,    "scale",    [0,0,0]),
                kf(kid(), t_crack,"scale",    [0,0,0]),
                kf(kid(), t_land, "scale",    [1,1,1]),
                kf(kid(), 1.2,    "scale",    [1,1,1]),
                kf(kid(), 0.0,    "position", [0,0,0]),
                kf(kid(), t_land, "position", [0,0,0]),
                kf(kid(), 0.0,    "rotation", [0, i*72.0+36.0, 0]),
                kf(kid(), 1.2,    "rotation", [0, i*72.0+36.0, 0]),
            ]
        }

    return animators

def make_idle():
    """idle: loop 5.0s — organic winding motion, nodes pulse, tips open/close, knot rotates, cracks pulse."""
    animators = {}

    # centre knot: slow Y rotation
    animators[knot_bone_id] = {
        "name": "centre_knot", "type": "bone", "keyframes": [
            kf(kid(), 0.0, "rotation", [0,  0, 0]),
            kf(kid(), 1.25,"rotation", [0, 30, 0]),
            kf(kid(), 2.5, "rotation", [0, 60, 0]),
            kf(kid(), 3.75,"rotation", [0, 30, 0]),
            kf(kid(), 5.0, "rotation", [0,  0, 0]),
            kf(kid(), 0.0, "position", [0, 0, 0]),
            kf(kid(), 2.5, "position", [0, 0.1, 0]),
            kf(kid(), 5.0, "position", [0, 0, 0]),
            kf(kid(), 0.0, "scale",    [1,1,1]),
            kf(kid(), 1.25,"scale",    [1.04,1.04,1.04]),
            kf(kid(), 2.5, "scale",    [1.08,1.08,1.08]),
            kf(kid(), 3.75,"scale",    [1.04,1.04,1.04]),
            kf(kid(), 5.0, "scale",    [1,1,1]),
        ]
    }

    # base bulge bones (dense keyframes)
    for i, b_id in enumerate(root_base_bulge_ids):
        bname = f"root_{i+1}_base_bulge"
        brb = i*72.0
        animators[b_id] = {
            "name": bname, "type": "bone", "keyframes": [
                # rotation — 9 keyframes
                kf(kid(), 0.0,  "rotation", [0,  brb,    0]),
                kf(kid(), 0.625,"rotation", [1,  brb,    1]),
                kf(kid(), 1.0,  "rotation", [2,  brb,    2]),
                kf(kid(), 1.875,"rotation", [0,  brb,    0]),
                kf(kid(), 2.5,  "rotation", [-2, brb,   -2]),
                kf(kid(), 3.125,"rotation", [-1, brb,   -1]),
                kf(kid(), 4.0,  "rotation", [2,  brb,    2]),
                kf(kid(), 4.625,"rotation", [1,  brb,    1]),
                kf(kid(), 5.0,  "rotation", [0,  brb,    0]),
                # position — 7 keyframes
                kf(kid(), 0.0,  "position", [0, 0,    0]),
                kf(kid(), 0.625,"position", [0, 0.02, 0]),
                kf(kid(), 1.25, "position", [0, 0.04, 0]),
                kf(kid(), 2.5,  "position", [0, 0.08, 0]),
                kf(kid(), 3.75, "position", [0, 0.04, 0]),
                kf(kid(), 4.375,"position", [0, 0.02, 0]),
                kf(kid(), 5.0,  "position", [0, 0,    0]),
                # scale — 7 keyframes
                kf(kid(), 0.0,  "scale", [1,    1,    1]),
                kf(kid(), 0.625,"scale", [1.01, 1.01, 1.01]),
                kf(kid(), 1.25, "scale", [1.02, 1.02, 1.02]),
                kf(kid(), 2.5,  "scale", [1.03, 1.03, 1.03]),
                kf(kid(), 3.75, "scale", [1.02, 1.02, 1.02]),
                kf(kid(), 4.375,"scale", [1.01, 1.01, 1.01]),
                kf(kid(), 5.0,  "scale", [1,    1,    1]),
            ]
        }

    # root segments: oscillate with phase delay down chain (dense keyframes for size)
    for root_i in range(5):
        for seg_j in range(5):
            b_id = root_segment_bone_ids[root_i][seg_j]
            bname = f"root_{root_i+1}_s{seg_j+1}"
            phase = root_i * 1.0 + seg_j * 0.5
            base_ry = root_i*72.0 + seg_j*8.0
            period = 4.5 + root_i * 0.15
            sx = 0.05*(seg_j+1); sy = 0.08*(seg_j+1); sz = 0.03*(seg_j+1)
            sc = 1+0.01*seg_j
            animators[b_id] = {
                "name": bname, "type": "bone", "keyframes": [
                    # position — 9 keyframes
                    kf(kid(), 0.0,         "position", [0, 0, 0]),
                    kf(kid(), period*0.125,"position", [sx*0.5, sy*0.4, sz*0.5]),
                    kf(kid(), period*0.25, "position", [sx, sy, sz]),
                    kf(kid(), period*0.375,"position", [sx*0.3, sy*0.6, sz*0.3]),
                    kf(kid(), period*0.5,  "position", [-sx*0.8, sy*0.4, -sz*0.8]),
                    kf(kid(), period*0.625,"position", [-sx*0.5, sy*0.3, -sz*0.4]),
                    kf(kid(), period*0.75, "position", [sx*0.6, sy*0.5, sz*0.4]),
                    kf(kid(), period*0.875,"position", [sx*0.3, sy*0.2, sz*0.2]),
                    kf(kid(), 5.0,         "position", [0, 0, 0]),
                    # rotation — 9 keyframes
                    kf(kid(), 0.0,         "rotation", [0,        base_ry,    0]),
                    kf(kid(), period*0.125,"rotation", [1.5,      base_ry+2,  1]),
                    kf(kid(), period*0.25, "rotation", [3,        base_ry+5,  2]),
                    kf(kid(), period*0.375,"rotation", [1,        base_ry+8,  0.5]),
                    kf(kid(), period*0.5,  "rotation", [-3,       base_ry+10,-2]),
                    kf(kid(), period*0.625,"rotation", [-1.5,     base_ry+7, -1]),
                    kf(kid(), period*0.75, "rotation", [2,        base_ry+5,  1]),
                    kf(kid(), period*0.875,"rotation", [1,        base_ry+2,  0.5]),
                    kf(kid(), 5.0,         "rotation", [0,        base_ry,    0]),
                    # scale — 7 keyframes
                    kf(kid(), 0.0,  "scale", [1,  1,  1]),
                    kf(kid(), 0.625,"scale", [sc*0.99, sc*1.01, sc*0.99]),
                    kf(kid(), 1.25, "scale", [sc, sc, sc]),
                    kf(kid(), 2.5,  "scale", [sc+0.005, sc+0.005, sc+0.005]),
                    kf(kid(), 3.75, "scale", [sc, sc, sc]),
                    kf(kid(), 4.375,"scale", [sc*0.995, sc*1.005, sc*0.995]),
                    kf(kid(), 5.0,  "scale", [1,  1,  1]),
                ]
            }

    # growth nodes: pulse scale (dense keyframes)
    for root_i in range(5):
        for node_k in range(3):
            b_id = root_node_bone_ids[root_i][node_k]
            bname = f"root_{root_i+1}_node_{node_k+1}"
            nrb = node_k * 30.0
            animators[b_id] = {
                "name": bname, "type": "bone", "keyframes": [
                    # scale — 9 keyframes
                    kf(kid(), 0.0,  "scale", [1,   1,   1]),
                    kf(kid(), 0.5,  "scale", [1.08,1.08,1.08]),
                    kf(kid(), 1.0,  "scale", [1.15,1.15,1.15]),
                    kf(kid(), 1.5,  "scale", [1.22,1.22,1.22]),
                    kf(kid(), 2.0,  "scale", [1.3, 1.3, 1.3]),
                    kf(kid(), 2.5,  "scale", [1.22,1.22,1.22]),
                    kf(kid(), 3.0,  "scale", [1.15,1.15,1.15]),
                    kf(kid(), 4.0,  "scale", [1.05,1.05,1.05]),
                    kf(kid(), 5.0,  "scale", [1,   1,   1]),
                    # position — 7 keyframes
                    kf(kid(), 0.0,  "position", [0, 0,    0]),
                    kf(kid(), 0.625,"position", [0, 0.02, 0]),
                    kf(kid(), 1.25, "position", [0, 0.04, 0]),
                    kf(kid(), 2.5,  "position", [0, 0.05, 0]),
                    kf(kid(), 3.75, "position", [0, 0.03, 0]),
                    kf(kid(), 4.375,"position", [0, 0.01, 0]),
                    kf(kid(), 5.0,  "position", [0, 0,    0]),
                    # rotation — 7 keyframes
                    kf(kid(), 0.0,  "rotation", [0, nrb,    0]),
                    kf(kid(), 0.625,"rotation", [1, nrb+4,  1]),
                    kf(kid(), 1.25, "rotation", [2, nrb+8,  2]),
                    kf(kid(), 2.5,  "rotation", [0, nrb+15, 0]),
                    kf(kid(), 3.75, "rotation", [-2,nrb+8, -2]),
                    kf(kid(), 4.375,"rotation", [-1,nrb+4, -1]),
                    kf(kid(), 5.0,  "rotation", [0, nrb,    0]),
                ]
            }

    # tips: open/close slightly (dense keyframes)
    for i, b_id in enumerate(root_tip_bone_ids):
        bname = f"root_{i+1}_tip"
        trb = i*72.0
        animators[b_id] = {
            "name": bname, "type": "bone", "keyframes": [
                # scale — 9 keyframes
                kf(kid(), 0.0,  "scale", [1,   1,   1]),
                kf(kid(), 0.625,"scale", [1.05,0.95,1.05]),
                kf(kid(), 1.25, "scale", [1.1, 0.9, 1.1]),
                kf(kid(), 1.875,"scale", [1.15,0.85,1.15]),
                kf(kid(), 2.5,  "scale", [1.2, 0.8, 1.2]),
                kf(kid(), 3.125,"scale", [1.15,0.85,1.15]),
                kf(kid(), 3.75, "scale", [1.1, 0.9, 1.1]),
                kf(kid(), 4.375,"scale", [1.05,0.95,1.05]),
                kf(kid(), 5.0,  "scale", [1,   1,   1]),
                # rotation — 7 keyframes
                kf(kid(), 0.0,  "rotation", [0,   trb,    0]),
                kf(kid(), 0.833,"rotation", [2,   trb+5,  2]),
                kf(kid(), 1.666,"rotation", [4,   trb+10, 4]),
                kf(kid(), 2.5,  "rotation", [5,   trb+15, 5]),
                kf(kid(), 3.333,"rotation", [3,   trb+10, 3]),
                kf(kid(), 4.166,"rotation", [1,   trb+5,  1]),
                kf(kid(), 5.0,  "rotation", [0,   trb,    0]),
                # position — 7 keyframes
                kf(kid(), 0.0,  "position", [0, 0,   0]),
                kf(kid(), 0.833,"position", [0, 0.03,0]),
                kf(kid(), 1.666,"position", [0, 0.06,0]),
                kf(kid(), 2.5,  "position", [0, 0.1, 0]),
                kf(kid(), 3.333,"position", [0, 0.06,0]),
                kf(kid(), 4.166,"position", [0, 0.03,0]),
                kf(kid(), 5.0,  "position", [0, 0,   0]),
            ]
        }

    # ground cracks: pulse emissive (dense keyframes)
    for i, b_id in enumerate(crack_bone_ids):
        bname = f"ground_crack_{i+1}"
        crb = i*72.0+36.0
        animators[b_id] = {
            "name": bname, "type": "bone", "keyframes": [
                # scale — 9 keyframes
                kf(kid(), 0.0,  "scale", [1,   1,   1]),
                kf(kid(), 0.625,"scale", [1.025,1.025,1.025]),
                kf(kid(), 1.25, "scale", [1.05,1.05,1.05]),
                kf(kid(), 1.875,"scale", [1.075,1.075,1.075]),
                kf(kid(), 2.5,  "scale", [1.1, 1.1, 1.1]),
                kf(kid(), 3.125,"scale", [1.075,1.075,1.075]),
                kf(kid(), 3.75, "scale", [1.05,1.05,1.05]),
                kf(kid(), 4.375,"scale", [1.025,1.025,1.025]),
                kf(kid(), 5.0,  "scale", [1,   1,   1]),
                # position — 7 keyframes
                kf(kid(), 0.0,  "position", [0, 0,    0]),
                kf(kid(), 0.833,"position", [0, 0.007,0]),
                kf(kid(), 1.666,"position", [0, 0.014,0]),
                kf(kid(), 2.5,  "position", [0, 0.02, 0]),
                kf(kid(), 3.333,"position", [0, 0.014,0]),
                kf(kid(), 4.166,"position", [0, 0.007,0]),
                kf(kid(), 5.0,  "position", [0, 0,    0]),
                # rotation — 7 keyframes
                kf(kid(), 0.0,  "rotation", [0, crb,   0]),
                kf(kid(), 0.833,"rotation", [0, crb+1, 0]),
                kf(kid(), 1.666,"rotation", [0, crb+2, 0]),
                kf(kid(), 2.5,  "rotation", [0, crb+4, 0]),
                kf(kid(), 3.333,"rotation", [0, crb+2, 0]),
                kf(kid(), 4.166,"rotation", [0, crb+1, 0]),
                kf(kid(), 5.0,  "rotation", [0, crb,   0]),
            ]
        }

    return animators

def make_dissipate():
    """dissipate: once, 0.8s — tips retract first, then segments from tip to base, knot sinks, cracks contract."""
    animators = {}

    # centre knot: sinks
    animators[knot_bone_id] = {
        "name": "centre_knot", "type": "bone", "keyframes": [
            kf(kid(), 0.0, "position", [0, 0, 0]),
            kf(kid(), 0.4, "position", [0,-1, 0]),
            kf(kid(), 0.8, "position", [0,-4, 0]),
            kf(kid(), 0.0, "scale",    [1,1,1]),
            kf(kid(), 0.5, "scale",    [0.5,0.5,0.5]),
            kf(kid(), 0.8, "scale",    [0,0,0]),
            kf(kid(), 0.0, "rotation", [0,30,0]),
            kf(kid(), 0.8, "rotation", [0,80,0]),
        ]
    }

    # base bulge bones
    for i, b_id in enumerate(root_base_bulge_ids):
        bname = f"root_{i+1}_base_bulge"
        animators[b_id] = {
            "name": bname, "type": "bone", "keyframes": [
                kf(kid(), 0.0, "position", [0, 0, 0]),
                kf(kid(), 0.3, "position", [0,-0.5, 0]),
                kf(kid(), 0.7, "position", [0,-3, 0]),
                kf(kid(), 0.8, "position", [0,-3, 0]),
                kf(kid(), 0.0, "scale",    [1,1,1]),
                kf(kid(), 0.6, "scale",    [0.3,0.3,0.3]),
                kf(kid(), 0.8, "scale",    [0,0,0]),
                kf(kid(), 0.0, "rotation", [0, i*72.0, 0]),
                kf(kid(), 0.8, "rotation", [0, i*72.0+20, 0]),
            ]
        }

    # root segments: retract from tip to base (seg 5 first, seg 1 last)
    for root_i in range(5):
        for seg_j in range(5):
            b_id = root_segment_bone_ids[root_i][seg_j]
            bname = f"root_{root_i+1}_s{seg_j+1}"
            # Tip=seg4 at 0.0s, base=seg0 at later time
            seg_from_tip = 4 - seg_j
            t_start = seg_from_tip * 0.06 + root_i * 0.01
            t_end   = min(t_start + 0.25, 0.75)
            base_ry = root_i*72.0 + seg_j*8.0
            animators[b_id] = {
                "name": bname, "type": "bone", "keyframes": [
                    kf(kid(), 0.0,     "position", [0, 0, 0]),
                    kf(kid(), t_start, "position", [0, 0.2, 0]),
                    kf(kid(), t_end,   "position", [0,-4, 0]),
                    kf(kid(), 0.8,     "position", [0,-4, 0]),
                    kf(kid(), 0.0,     "scale",    [1,1,1]),
                    kf(kid(), t_end,   "scale",    [0.3,0.3,0.3]),
                    kf(kid(), 0.8,     "scale",    [0,0,0]),
                    kf(kid(), 0.0,     "rotation", [0, base_ry, 0]),
                    kf(kid(), t_end,   "rotation", [15, base_ry+20, 10]),
                    kf(kid(), 0.8,     "rotation", [15, base_ry+20, 10]),
                ]
            }

    # nodes: retract with segments
    for root_i in range(5):
        for node_k in range(3):
            b_id = root_node_bone_ids[root_i][node_k]
            bname = f"root_{root_i+1}_node_{node_k+1}"
            seg_idx = node_k + 1
            seg_from_tip = 4 - seg_idx
            t_start = seg_from_tip * 0.06 + root_i * 0.01
            t_end   = min(t_start + 0.2, 0.72)
            animators[b_id] = {
                "name": bname, "type": "bone", "keyframes": [
                    kf(kid(), 0.0,     "scale",    [1,1,1]),
                    kf(kid(), t_start, "scale",    [1.1,1.1,1.1]),
                    kf(kid(), t_end,   "scale",    [0,0,0]),
                    kf(kid(), 0.8,     "scale",    [0,0,0]),
                    kf(kid(), 0.0,     "position", [0,0,0]),
                    kf(kid(), t_end,   "position", [0,-1,0]),
                    kf(kid(), 0.0,     "rotation", [0, node_k*30.0, 0]),
                    kf(kid(), 0.8,     "rotation", [0, node_k*30.0+30, 0]),
                ]
            }

    # tips retract first
    for i, b_id in enumerate(root_tip_bone_ids):
        bname = f"root_{i+1}_tip"
        t_start = i * 0.02
        t_end   = min(t_start + 0.2, 0.5)
        animators[b_id] = {
            "name": bname, "type": "bone", "keyframes": [
                kf(kid(), 0.0,     "scale",    [1,1,1]),
                kf(kid(), t_start, "scale",    [1.2,0.8,1.2]),
                kf(kid(), t_end,   "scale",    [0,0,0]),
                kf(kid(), 0.8,     "scale",    [0,0,0]),
                kf(kid(), 0.0,     "position", [0, 0, 0]),
                kf(kid(), t_end,   "position", [0,-2, 0]),
                kf(kid(), 0.0,     "rotation", [0, i*72.0+15, 0]),
                kf(kid(), t_end,   "rotation", [20, i*72.0+30, 10]),
                kf(kid(), 0.8,     "rotation", [20, i*72.0+30, 10]),
            ]
        }

    # cracks contract inward
    for i, b_id in enumerate(crack_bone_ids):
        bname = f"ground_crack_{i+1}"
        t_start = i * 0.04
        t_end   = min(t_start + 0.35, 0.75)
        animators[b_id] = {
            "name": bname, "type": "bone", "keyframes": [
                kf(kid(), 0.0,     "scale",    [1,1,1]),
                kf(kid(), t_start, "scale",    [1.1,1.1,1.1]),
                kf(kid(), t_end,   "scale",    [0,0,0]),
                kf(kid(), 0.8,     "scale",    [0,0,0]),
                kf(kid(), 0.0,     "position", [0,0,0]),
                kf(kid(), t_end,   "position", [0,-0.5,0]),
                kf(kid(), 0.0,     "rotation", [0, i*72.0+36.0, 0]),
                kf(kid(), 0.8,     "rotation", [0, i*72.0+50.0, 0]),
            ]
        }

    return animators

spawn_anim = {
    "uuid": "a001", "name": "animation.nightmare_root_surge.spawn",
    "loop": "once", "length": 1.2, "snapping": 24,
    "selected": False, "saved": False,
    "animators": make_spawn()
}
idle_anim = {
    "uuid": "a002", "name": "animation.nightmare_root_surge.idle",
    "loop": "loop", "length": 5.0, "snapping": 24,
    "selected": False, "saved": False,
    "animators": make_idle()
}
dissipate_anim = {
    "uuid": "a003", "name": "animation.nightmare_root_surge.dissipate",
    "loop": "once", "length": 0.8, "snapping": 24,
    "selected": False, "saved": False,
    "animators": make_dissipate()
}

# ── ASSEMBLE ────────────────────────────────────────────────────────────────
bbmodel = {
    "meta": {"format_version": "4.10", "model_format": "free", "box_uv": False},
    "name": "nightmare_root_surge",
    "geometry": "geometry.nightmare_root_surge",
    "resolution": {"width": 64, "height": 64},
    "elements": elements,
    "outliner": bones_list,
    "textures": [
        {
            "id": "0", "name": "nightmare_root_surge_tex0",
            "relative_path": "../textures/nightmare_root_surge_tex.png",
            "folder": "devilsdream", "namespace": "", "visible": True,
            "mode": "bitmap", "saved": False, "uuid": "t001",
            "source": f"data:image/png;base64,{TEX0_B64}"
        },
        {
            "id": "1", "name": "nightmare_root_surge_tex1",
            "relative_path": "../textures/nightmare_root_surge_tex1.png",
            "folder": "devilsdream", "namespace": "", "visible": True,
            "mode": "bitmap", "saved": False, "uuid": "t002",
            "source": f"data:image/png;base64,{TEX1_B64}"
        }
    ],
    "animations": [spawn_anim, idle_anim, dissipate_anim]
}

out = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_root_surge.bbmodel"
with open(out, "w") as f:
    json.dump(bbmodel, f, separators=(",", ":"))

size = os.path.getsize(out)
print(f"Written: {out}")
print(f"File size: {size:,} bytes ({size/1024:.1f} KB)")
print(f"Elements: {len(elements)}, Bones in outliner: {len(bones_list)}")
print(f"Keyframe counter: {k_idx-1}")
if size < 300000:
    print("WARNING: FILE UNDER 300KB — NEEDS MORE KEYFRAMES")
else:
    print("PASS: File exceeds 300KB minimum")
