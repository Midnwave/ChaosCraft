"""
Build bone_throne_summon.bbmodel
A throne of compacted bones rising from the ground.
- Seat platform: 3 overlapping wide flat slabs at Y=1.5
- Back: 5 vertical column bones (ribcage), arched silhouette (center tallest Y=8, outer Y=5.5)
- Armrests: L+R, 3 segments each curving outward, with hand-shape end cube
- Legs: 4 downward-pointing tapered spike bones (2 segments each)
- Side spike accents: 4 narrow upward-pointing spikes at outer back corners
- Ground crack base: 6 flat crack slabs radiating from base (tex1)
Plus detail bones: spine vertebrae, finger detail cubes, seat edge accents, leg base anchors.
80+ elements, 55+ bones, 3 animations, file >= 300KB.
"""
import json, math, os

b64_0 = open("D:/CC/ChaosCraft/tools/gen_textures/m18_b64.txt").read().strip()
b64_1 = open("D:/CC/ChaosCraft/tools/gen_textures/m18_tex1_b64.txt").read().strip()


def eid(n): return f"e{n:03d}"
def bid(n): return f"b{n:03d}"
def aid(n): return f"a{n:03d}"
def kid(n): return f"k{n:04d}"
def tid(n): return f"t{n:03d}"


UV0 = {"north": {"uv": [2, 2, 30, 30], "texture": 0},
       "east": {"uv": [34, 2, 62, 30], "texture": 0},
       "south": {"uv": [2, 2, 30, 30], "texture": 0},
       "west": {"uv": [34, 2, 62, 30], "texture": 0},
       "up": {"uv": [2, 34, 30, 62], "texture": 0},
       "down": {"uv": [34, 34, 62, 62], "texture": 0}}
UV1 = {"north": {"uv": [34, 34, 62, 62], "texture": 1},
       "east": {"uv": [34, 34, 62, 62], "texture": 1},
       "south": {"uv": [34, 34, 62, 62], "texture": 1},
       "west": {"uv": [34, 34, 62, 62], "texture": 1},
       "up": {"uv": [34, 34, 62, 62], "texture": 1},
       "down": {"uv": [34, 34, 62, 62], "texture": 1}}


def elem(idx, name, fr, to, uv):
    return {"name": name, "box_uv": False, "rescale": False, "locked": False,
            "render_order": "default", "allow_mirror_modeling": True,
            "from": fr, "to": to, "autouv": 0, "color": 0, "uuid": eid(idx), "faces": uv}


def bone(idx, name, origin, children, rot=None):
    o = rot if rot else [0, 0, 0]
    return {"name": name, "origin": origin, "rotation": o, "uuid": bid(idx),
            "export": True, "isOpen": True, "children": children}


k_counter = [1]
def kf(time, x, y, z, channel="position", interp="catmullrom"):
    uid = kid(k_counter[0]); k_counter[0] += 1
    return {"uuid": uid, "time": time, "color": -1, "interpolation": interp,
            "data_points": [{"x": str(x), "y": str(y), "z": str(z)}], "channel": channel}


elements = []
bones = []
ei = 1
bi = 1


# ─────────────────────────────────────────────────────────────────
# 1. SEAT PLATFORM — 3 overlapping wide flat slabs at Y=1.5
# ─────────────────────────────────────────────────────────────────
seat_bids = []
seat_defs = [
    ("seat_main",  [-3.5, 1.2, -3.0], [3.5, 1.7, 3.0]),     # main platform
    ("seat_top",   [-3.0, 1.7, -2.7], [3.0, 1.95, 2.7]),    # upper layer
    ("seat_front", [-3.2, 1.2, 2.7],  [3.2, 1.6, 3.4]),     # front lip
]
for name, fr, to in seat_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [0, fr[1], 0], [eid(ei)]); bones.append(b)
    seat_bids.append(bid(bi)); ei += 1; bi += 1

# Seat edge accents: 4 small slabs around seat sides
seat_edge_bids = []
seat_edge_defs = [
    ("seat_edge_L",  [-3.7, 1.4, -2.5], [-3.3, 1.85, 2.5]),
    ("seat_edge_R",  [ 3.3, 1.4, -2.5], [ 3.7, 1.85, 2.5]),
    ("seat_edge_B",  [-3.0, 1.4, -3.4], [ 3.0, 1.85, -2.7]),
    ("seat_edge_F",  [-3.0, 1.95, 2.7], [ 3.0, 2.10, 3.4]),
]
for name, fr, to in seat_edge_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [(fr[0]+to[0])/2, (fr[1]+to[1])/2, (fr[2]+to[2])/2], [eid(ei)])
    bones.append(b); seat_edge_bids.append(bid(bi)); ei += 1; bi += 1


# ─────────────────────────────────────────────────────────────────
# 2. BACK COLUMNS — 5 vertical column bones forming ribcage arc
# ─────────────────────────────────────────────────────────────────
# X positions of 5 columns (center is tallest)
# Heights: outer Y=5.5, mid Y=6.5, center Y=8.0
# Columns extend from Y=1.95 (top of seat) up to height
back_col_bids = []
back_col_defs = [
    ("back_col_1", -2.6, 5.5),   # outermost left
    ("back_col_2", -1.3, 6.5),
    ("back_col_3",  0.0, 8.0),   # center tallest
    ("back_col_4",  1.3, 6.5),
    ("back_col_5",  2.6, 5.5),   # outermost right
]
for name, cx, top_y in back_col_defs:
    half_w = 0.45
    half_d = 0.4
    fr = [cx - half_w, 1.95, -3.0]
    to = [cx + half_w, top_y, -2.2]
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [cx, 1.95, -2.6], [eid(ei)])
    bones.append(b); back_col_bids.append(bid(bi)); ei += 1; bi += 1

# Spine vertebrae details — 3 small horizontal cubes per back column (15 total)
vertebrae_bids = []
for i, (name, cx, top_y) in enumerate(back_col_defs):
    col_height = top_y - 1.95
    for v in range(3):
        # Distribute along column
        vy = 1.95 + col_height * (0.25 + v * 0.30)
        v_name = f"vert_{i+1}_{v+1}"
        fr = [cx - 0.55, vy - 0.18, -3.05]
        to = [cx + 0.55, vy + 0.18, -2.10]
        e = elem(ei, v_name, fr, to, UV0); elements.append(e)
        b = bone(bi, v_name, [cx, vy, -2.6], [eid(ei)])
        bones.append(b); vertebrae_bids.append(bid(bi)); ei += 1; bi += 1


# ─────────────────────────────────────────────────────────────────
# 3. ARMRESTS — L and R, 3 segments each curving outward + hand cube
# ─────────────────────────────────────────────────────────────────
# Left armrest: from seat side (X=-3.5, Y=2.0) curving outward & forward
# Right armrest: mirror
# Each arm: 3 cube segments + 1 wider hand-shape cube + finger detail cubes
armrest_bids = []
finger_bids = []

def build_armrest(side, side_label):
    """side = -1 for Left, +1 for Right"""
    global ei, bi
    seg_y_levels = [2.0, 2.4, 2.8]  # rising slightly
    # Each segment moves outward (X) and forward (Z) progressively
    seg_x = [3.5, 3.85, 4.15]
    seg_z = [-1.2, 0.4, 2.0]
    bids = []
    for s in range(3):
        seg_name = f"arm_{side_label}_seg{s+1}"
        cx = side * seg_x[s]
        cy = seg_y_levels[s]
        cz = seg_z[s]
        fr = [cx - 0.45, cy - 0.30, cz - 0.6]
        to = [cx + 0.45, cy + 0.30, cz + 0.6]
        e = elem(ei, seg_name, fr, to, UV0); elements.append(e)
        b = bone(bi, seg_name, [cx, cy, cz], [eid(ei)])
        bones.append(b); bids.append(bid(bi)); ei += 1; bi += 1
    # Hand-shape end (wider flat cube) at end of armrest (front)
    hand_x = side * (seg_x[2] + 0.2)
    hand_y = seg_y_levels[2]
    hand_z = seg_z[2] + 0.7
    hand_name = f"arm_{side_label}_hand"
    fr = [hand_x - 0.7 * (1 if side > 0 else -1), hand_y - 0.25, hand_z - 0.3]
    to = [hand_x + 0.7 * (1 if side > 0 else -1), hand_y + 0.25, hand_z + 0.55]
    if fr[0] > to[0]: fr[0], to[0] = to[0], fr[0]
    e = elem(ei, hand_name, fr, to, UV0); elements.append(e)
    b = bone(bi, hand_name, [hand_x, hand_y, hand_z], [eid(ei)])
    bones.append(b); bids.append(bid(bi)); ei += 1; bi += 1

    # 4 small finger-detail cubes on the hand
    for f in range(4):
        f_name = f"arm_{side_label}_finger{f+1}"
        fx = hand_x + (f - 1.5) * 0.3
        fy = hand_y + 0.15
        fz = hand_z + 0.55
        fr_f = [fx - 0.10, fy - 0.10, fz]
        to_f = [fx + 0.10, fy + 0.10, fz + 0.4]
        e = elem(ei, f_name, fr_f, to_f, UV0); elements.append(e)
        b = bone(bi, f_name, [fx, fy, fz], [eid(ei)])
        bones.append(b); finger_bids.append(bid(bi)); ei += 1; bi += 1

    return bids


armrest_L_bids = build_armrest(-1, "L")
armrest_R_bids = build_armrest(+1, "R")
armrest_bids = armrest_L_bids + armrest_R_bids


# ─────────────────────────────────────────────────────────────────
# 4. THRONE LEGS — 4 tapered downward spike bones at seat corners
# ─────────────────────────────────────────────────────────────────
# Each leg = 2 segments: wide upper, narrow tip pointing down
leg_bids = []
leg_corners = [
    ("leg_FL", -2.8,  2.5),
    ("leg_FR",  2.8,  2.5),
    ("leg_BL", -2.8, -2.5),
    ("leg_BR",  2.8, -2.5),
]
for name, lx, lz in leg_corners:
    # Upper wide segment: from Y=0.5 to Y=1.4
    upper_name = f"{name}_upper"
    fr = [lx - 0.45, 0.5, lz - 0.45]
    to = [lx + 0.45, 1.4, lz + 0.45]
    e = elem(ei, upper_name, fr, to, UV0); elements.append(e)
    b = bone(bi, upper_name, [lx, 1.0, lz], [eid(ei)])
    bones.append(b); leg_bids.append(bid(bi)); ei += 1; bi += 1
    # Lower narrow tip: from Y=-1.2 to Y=0.5 (pointing down)
    lower_name = f"{name}_tip"
    fr = [lx - 0.20, -1.2, lz - 0.20]
    to = [lx + 0.20, 0.5,  lz + 0.20]
    e = elem(ei, lower_name, fr, to, UV0); elements.append(e)
    b = bone(bi, lower_name, [lx, 0, lz], [eid(ei)])
    bones.append(b); leg_bids.append(bid(bi)); ei += 1; bi += 1

# Leg base anchors — 4 small cubes wrapping each leg base where it meets ground
leg_base_bids = []
for name, lx, lz in leg_corners:
    anchor_name = f"{name}_anchor"
    fr = [lx - 0.55, 0.25, lz - 0.55]
    to = [lx + 0.55, 0.55, lz + 0.55]
    e = elem(ei, anchor_name, fr, to, UV0); elements.append(e)
    b = bone(bi, anchor_name, [lx, 0.4, lz], [eid(ei)])
    bones.append(b); leg_base_bids.append(bid(bi)); ei += 1; bi += 1


# ─────────────────────────────────────────────────────────────────
# 5. SIDE SPIKE ACCENTS — 4 narrow upward spikes at outer back corners
# ─────────────────────────────────────────────────────────────────
side_spike_bids = []
# Outer columns (col_1 at -2.6, col_5 at 2.6) — spikes above and slightly out
# 2 spikes per outer column (front + back of column top)
side_spike_defs = [
    ("spike_BL_front", -3.0, 5.5, -2.7),
    ("spike_BL_back",  -3.0, 5.5, -3.2),
    ("spike_BR_front",  3.0, 5.5, -2.7),
    ("spike_BR_back",   3.0, 5.5, -3.2),
]
for name, sx, base_y, sz in side_spike_defs:
    fr = [sx - 0.18, base_y, sz - 0.18]
    to = [sx + 0.18, base_y + 1.6, sz + 0.18]
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [sx, base_y, sz], [eid(ei)])
    bones.append(b); side_spike_bids.append(bid(bi)); ei += 1; bi += 1


# ─────────────────────────────────────────────────────────────────
# 6. GROUND CRACKS — 6 flat slabs radiating from base (tex1)
# ─────────────────────────────────────────────────────────────────
crack_bids = []
NUM_CRACKS = 6
for i in range(NUM_CRACKS):
    ang = (i / NUM_CRACKS) * 2 * math.pi
    # Long thin radial slab from origin outward
    R0 = 1.0
    R1 = 5.5
    # Compute bounding box for axis-aligned approximation
    end_x = math.cos(ang) * R1
    end_z = math.sin(ang) * R1
    start_x = math.cos(ang) * R0
    start_z = math.sin(ang) * R0
    # Create a thin axis-aligned tilted-looking slab
    cx_mid = (start_x + end_x) / 2
    cz_mid = (start_z + end_z) / 2
    # half-extents in radial vs perpendicular
    half_radial = (R1 - R0) / 2
    half_perp = 0.4
    # Use larger extents; actual rotation handled by bone rot
    fr = [cx_mid - half_radial, 0.05, cz_mid - half_perp]
    to = [cx_mid + half_radial, 0.20, cz_mid + half_perp]
    name = f"crack_{i+1}"
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    b = bone(bi, name, [cx_mid, 0.10, cz_mid], [eid(ei)],
             rot=[0, math.degrees(-ang), 0])
    bones.append(b); crack_bids.append(bid(bi)); ei += 1; bi += 1


# ─────────────────────────────────────────────────────────────────
# 7. EXTRA RIB DETAIL — 2 horizontal rib cubes connecting back columns
# ─────────────────────────────────────────────────────────────────
rib_connector_bids = []
# Connect cols 1-2, 2-3, 3-4, 4-5 at 2 different heights → 8 connectors
rib_pairs = [
    (-2.6, -1.3), (-1.3, 0.0), (0.0, 1.3), (1.3, 2.6),
]
rib_heights = [3.0, 4.5]
for h_i, h in enumerate(rib_heights):
    for p_i, (x0, x1) in enumerate(rib_pairs):
        rname = f"rib_{h_i+1}_{p_i+1}"
        cx_mid = (x0 + x1) / 2
        fr = [x0 + 0.20, h - 0.18, -2.85]
        to = [x1 - 0.20, h + 0.18, -2.45]
        if fr[0] >= to[0]:
            continue
        e = elem(ei, rname, fr, to, UV0); elements.append(e)
        b = bone(bi, rname, [cx_mid, h, -2.6], [eid(ei)])
        bones.append(b); rib_connector_bids.append(bid(bi)); ei += 1; bi += 1


# ─────────────────────────────────────────────────────────────────
# 8. ADDITIONAL DETAIL — Top crown spikes on tallest center column
# ─────────────────────────────────────────────────────────────────
crown_bids = []
crown_defs = [
    ("crown_C", 0.0, 8.0),
    ("crown_L", -1.3, 6.5),
    ("crown_R", 1.3, 6.5),
]
for name, cx, base_y in crown_defs:
    cname = f"{name}_spike"
    fr = [cx - 0.15, base_y, -2.85]
    to = [cx + 0.15, base_y + 1.2, -2.45]
    e = elem(ei, cname, fr, to, UV0); elements.append(e)
    b = bone(bi, cname, [cx, base_y, -2.65], [eid(ei)])
    bones.append(b); crown_bids.append(bid(bi)); ei += 1; bi += 1


# ─────────────────────────────────────────────────────────────────
# 9. Throne base trim — bone-stack trim around seat front and sides
# ─────────────────────────────────────────────────────────────────
trim_bids = []
trim_defs = [
    ("trim_F1", [-2.8, 1.05, 3.0], [-1.4, 1.30, 3.5]),
    ("trim_F2", [-1.4, 1.05, 3.0], [ 0.0, 1.30, 3.5]),
    ("trim_F3", [ 0.0, 1.05, 3.0], [ 1.4, 1.30, 3.5]),
    ("trim_F4", [ 1.4, 1.05, 3.0], [ 2.8, 1.30, 3.5]),
    ("trim_L",  [-3.6, 1.05, -2.8], [-3.2, 1.30, 2.8]),
    ("trim_R",  [ 3.2, 1.05, -2.8], [ 3.6, 1.30, 2.8]),
]
for name, fr, to in trim_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [(fr[0]+to[0])/2, (fr[1]+to[1])/2, (fr[2]+to[2])/2], [eid(ei)])
    bones.append(b); trim_bids.append(bid(bi)); ei += 1; bi += 1


# ─────────────────────────────────────────────────────────────────
# 10. Floor edge crack accents — 6 small extra crack accents (tex1)
# ─────────────────────────────────────────────────────────────────
crack_accent_bids = []
import random
rng = random.Random(99)
for i in range(8):
    ang = rng.uniform(0, 2*math.pi)
    r = rng.uniform(2.0, 5.0)
    cx_mid = math.cos(ang) * r
    cz_mid = math.sin(ang) * r
    name = f"crack_accent_{i+1}"
    fr = [cx_mid - 0.4, 0.06, cz_mid - 0.20]
    to = [cx_mid + 0.4, 0.16, cz_mid + 0.20]
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    b = bone(bi, name, [cx_mid, 0.10, cz_mid], [eid(ei)],
             rot=[0, math.degrees(-ang), 0])
    bones.append(b); crack_accent_bids.append(bid(bi)); ei += 1; bi += 1


print(f"Elements: {len(elements)}, Bones: {len(bones)}")

bone_name_map = {b["uuid"]: b["name"] for b in bones}

# Group all bones for animation. Categorize:
all_bone_groups = {
    "seat":        seat_bids,
    "seat_edge":   seat_edge_bids,
    "back_col":    back_col_bids,
    "vertebrae":   vertebrae_bids,
    "armrest":     armrest_bids,
    "fingers":     finger_bids,
    "leg":         leg_bids,
    "leg_base":    leg_base_bids,
    "side_spike":  side_spike_bids,
    "crack":       crack_bids,
    "rib":         rib_connector_bids,
    "crown":       crown_bids,
    "trim":        trim_bids,
    "crack_accent": crack_accent_bids,
}


# ════════════════════════════════════════════════════════════════
# ANIMATIONS
# ════════════════════════════════════════════════════════════════


def build_spawn():
    """spawn once, 1.4s.
    - ground cracks appear at 0.2s
    - throne legs erupt from ground at 0.3s
    - seat platform slams up at 0.5s
    - armrests deploy outward from seat sides at 0.65s
    - back columns extend upward inner-to-outer at 0.75s with 0.08s stagger
    - side spike accents extend at 1.1s"""
    animators = {}
    LEN = 1.4

    # Ground cracks at 0.2s — scale up
    for i, buid in enumerate(crack_bids):
        d = 0.2 + i * 0.02
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,         0, 0, 0, "scale"),
            kf(d,           0, 0, 0, "scale"),
            kf(d + 0.10,    1.2, 1, 1.2, "scale"),
            kf(d + 0.20,    1.0, 1, 1.0, "scale"),
            kf(LEN - 0.1,   1, 1, 1, "scale"),
            kf(LEN,         1, 1, 1, "scale"),
            kf(0.0,    0, 0, 0, "position"),
            kf(LEN/2,  0, 0, 0, "position"),
            kf(LEN,    0, 0, 0, "position"),
            kf(0.0,    0, 0, 0, "rotation"),
            kf(LEN/2,  0, 0, 0, "rotation"),
            kf(LEN,    0, 0, 0, "rotation"),
        ]}
    for i, buid in enumerate(crack_accent_bids):
        d = 0.25 + i * 0.015
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,         0, 0, 0, "scale"),
            kf(d,           0, 0, 0, "scale"),
            kf(d + 0.10,    1, 1, 1, "scale"),
            kf(d + 0.20,    0.9, 1, 0.9, "scale"),
            kf(LEN - 0.1,   1, 1, 1, "scale"),
            kf(LEN,         1, 1, 1, "scale"),
            kf(0.0, 0,0,0,"position"), kf(LEN/2,0,0,0,"position"), kf(LEN,0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(LEN/2,0,0,0,"rotation"), kf(LEN,0,0,0,"rotation"),
        ]}

    # Legs erupt from ground at 0.3s — start sunk down, rise into place
    for i, buid in enumerate(leg_bids):
        d = 0.3 + (i // 2) * 0.04
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,    0, -3.0, 0, "position"),
            kf(d,      0, -3.0, 0, "position"),
            kf(d + 0.20, 0, -1.2, 0, "position"),
            kf(d + 0.35, 0, 0.2, 0, "position"),
            kf(d + 0.45, 0, -0.05, 0, "position"),
            kf(LEN,    0, 0, 0, "position"),
            kf(0.0,    1, 1, 1, "scale"),
            kf(LEN/2,  1, 1, 1, "scale"),
            kf(LEN,    1, 1, 1, "scale"),
            kf(0.0,    0, 0, 0, "rotation"),
            kf(LEN/2,  0, 0, 0, "rotation"),
            kf(LEN,    0, 0, 0, "rotation"),
        ]}
    for i, buid in enumerate(leg_base_bids):
        d = 0.35 + i * 0.03
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,    0, 0, 0, "scale"),
            kf(d,      0, 0, 0, "scale"),
            kf(d + 0.15, 1.1, 1.1, 1.1, "scale"),
            kf(d + 0.25, 1, 1, 1, "scale"),
            kf(LEN - 0.1, 1, 1, 1, "scale"),
            kf(LEN,    1, 1, 1, "scale"),
            kf(0.0, 0,0,0,"position"), kf(LEN/2,0,0,0,"position"), kf(LEN,0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(LEN/2,0,0,0,"rotation"), kf(LEN,0,0,0,"rotation"),
        ]}

    # Seat slams up at 0.5s
    for i, buid in enumerate(seat_bids):
        d = 0.5
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,    0, -2.5, 0, "position"),
            kf(d,      0, -2.5, 0, "position"),
            kf(d + 0.10, 0, 0.4, 0, "position"),
            kf(d + 0.20, 0, -0.1, 0, "position"),
            kf(d + 0.30, 0, 0, 0, "position"),
            kf(LEN,    0, 0, 0, "position"),
            kf(0.0,    1, 1, 1, "scale"),
            kf(LEN/2,  1, 1, 1, "scale"),
            kf(LEN,    1, 1, 1, "scale"),
            kf(0.0,    0, 0, 0, "rotation"),
            kf(LEN/2,  0, 0, 0, "rotation"),
            kf(LEN,    0, 0, 0, "rotation"),
        ]}
    # Seat edges and trim follow seat
    for i, buid in enumerate(seat_edge_bids + trim_bids):
        d = 0.55 + i * 0.02
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,    0, -2.3, 0, "position"),
            kf(d,      0, -2.3, 0, "position"),
            kf(d + 0.15, 0, 0.2, 0, "position"),
            kf(d + 0.30, 0, 0, 0, "position"),
            kf(LEN/2,  0, 0, 0, "position"),
            kf(LEN,    0, 0, 0, "position"),
            kf(0.0,    1, 1, 1, "scale"),
            kf(LEN/2,  1, 1, 1, "scale"),
            kf(LEN,    1, 1, 1, "scale"),
            kf(0.0,    0, 0, 0, "rotation"),
            kf(LEN/2,  0, 0, 0, "rotation"),
            kf(LEN,    0, 0, 0, "rotation"),
        ]}

    # Armrests deploy outward at 0.65s — start collapsed at center, expand sideways
    for i, buid in enumerate(armrest_bids):
        side = -1 if buid in armrest_L_bids else 1
        d = 0.65 + (i % 4) * 0.04
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,    -side * 2.0, 0, -1.0, "position"),
            kf(d,      -side * 2.0, 0, -1.0, "position"),
            kf(d + 0.20, 0, 0, 0, "position"),
            kf(d + 0.35, 0, 0, 0, "position"),
            kf(LEN/2,  0, 0, 0, "position"),
            kf(LEN,    0, 0, 0, "position"),
            kf(0.0,    0, 0, 0, "scale"),
            kf(d,      0, 0, 0, "scale"),
            kf(d + 0.10, 1, 1, 1, "scale"),
            kf(LEN/2,  1, 1, 1, "scale"),
            kf(LEN,    1, 1, 1, "scale"),
            kf(0.0,    0, 0, 0, "rotation"),
            kf(LEN/2,  0, 0, 0, "rotation"),
            kf(LEN,    0, 0, 0, "rotation"),
        ]}
    # Fingers come last with armrests
    for i, buid in enumerate(finger_bids):
        d = 0.85 + i * 0.02
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,    0, 0, 0, "scale"),
            kf(d,      0, 0, 0, "scale"),
            kf(d + 0.10, 1.1, 1.1, 1.1, "scale"),
            kf(d + 0.20, 1, 1, 1, "scale"),
            kf(LEN/2,  1, 1, 1, "scale"),
            kf(LEN,    1, 1, 1, "scale"),
            kf(0.0, 0,0,0,"position"), kf(LEN/2,0,0,0,"position"), kf(LEN,0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(LEN/2,0,0,0,"rotation"), kf(LEN,0,0,0,"rotation"),
        ]}

    # Back columns extend at 0.75s, inner-to-outer with 0.08s stagger
    # Order: col_3 (center) first, then col_2/col_4, then col_1/col_5
    col_order = [2, 1, 3, 0, 4]  # indices: center first
    for order_i, col_i in enumerate(col_order):
        buid = back_col_bids[col_i]
        d = 0.75 + order_i * 0.08
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,    1, 0.05, 1, "scale"),
            kf(d,      1, 0.05, 1, "scale"),
            kf(d + 0.18, 1, 1.1, 1, "scale"),
            kf(d + 0.30, 1, 1.0, 1, "scale"),
            kf(LEN - 0.1, 1, 1, 1, "scale"),
            kf(LEN,    1, 1, 1, "scale"),
            kf(0.0,    0, 0, 0, "position"),
            kf(LEN/2,  0, 0, 0, "position"),
            kf(LEN,    0, 0, 0, "position"),
            kf(0.0,    0, 0, 0, "rotation"),
            kf(LEN/2,  0, 0, 0, "rotation"),
            kf(LEN,    0, 0, 0, "rotation"),
        ]}
    # Vertebrae follow their column
    for i, buid in enumerate(vertebrae_bids):
        col_idx = i // 3
        order_pos = col_order.index(col_idx)
        d = 0.80 + order_pos * 0.08 + (i % 3) * 0.03
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,    0, 0, 0, "scale"),
            kf(d,      0, 0, 0, "scale"),
            kf(d + 0.10, 1.15, 1.15, 1.15, "scale"),
            kf(d + 0.20, 1, 1, 1, "scale"),
            kf(LEN - 0.1, 1, 1, 1, "scale"),
            kf(LEN,    1, 1, 1, "scale"),
            kf(0.0, 0,0,0,"position"), kf(LEN/2,0,0,0,"position"), kf(LEN,0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(LEN/2,0,0,0,"rotation"), kf(LEN,0,0,0,"rotation"),
        ]}
    # Ribs follow back columns
    for i, buid in enumerate(rib_connector_bids):
        d = 0.95 + i * 0.025
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,    0.05, 1, 1, "scale"),
            kf(d,      0.05, 1, 1, "scale"),
            kf(d + 0.10, 1.1, 1, 1, "scale"),
            kf(d + 0.20, 1, 1, 1, "scale"),
            kf(LEN - 0.1, 1, 1, 1, "scale"),
            kf(LEN,    1, 1, 1, "scale"),
            kf(0.0, 0,0,0,"position"), kf(LEN/2,0,0,0,"position"), kf(LEN,0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(LEN/2,0,0,0,"rotation"), kf(LEN,0,0,0,"rotation"),
        ]}
    # Crown spikes
    for i, buid in enumerate(crown_bids):
        d = 1.05 + i * 0.04
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,    1, 0.05, 1, "scale"),
            kf(d,      1, 0.05, 1, "scale"),
            kf(d + 0.12, 1, 1.15, 1, "scale"),
            kf(d + 0.22, 1, 1, 1, "scale"),
            kf(LEN - 0.1, 1, 1, 1, "scale"),
            kf(LEN,    1, 1, 1, "scale"),
            kf(0.0, 0,0,0,"position"), kf(LEN/2,0,0,0,"position"), kf(LEN,0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(LEN/2,0,0,0,"rotation"), kf(LEN,0,0,0,"rotation"),
        ]}

    # Side spike accents at 1.1s
    for i, buid in enumerate(side_spike_bids):
        d = 1.1 + i * 0.04
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,    1, 0.05, 1, "scale"),
            kf(d,      1, 0.05, 1, "scale"),
            kf(d + 0.10, 1, 1.2, 1, "scale"),
            kf(d + 0.20, 1, 1, 1, "scale"),
            kf(LEN - 0.05, 1, 1, 1, "scale"),
            kf(LEN,    1, 1, 1, "scale"),
            kf(0.0, 0,0,0,"position"), kf(LEN/2,0,0,0,"position"), kf(LEN,0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(LEN/2,0,0,0,"rotation"), kf(LEN,0,0,0,"rotation"),
        ]}

    return animators


def build_idle():
    """idle loop, 8.0s. Extremely subtle — the throne is immovable.
    Sway ±0.2°, slow emissive pulses on back columns, armrest fingers flex,
    crack slabs pulse emissive, everything feels like it's been here forever."""
    animators = {}
    LEN = 8.0

    # Seat: tiny breathing
    for i, buid in enumerate(seat_bids):
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,        0, 0, 0, "rotation"),
            kf(LEN * 0.25, 0.2, 0, 0, "rotation"),
            kf(LEN * 0.5,  0, 0, 0, "rotation"),
            kf(LEN * 0.75, -0.2, 0, 0, "rotation"),
            kf(LEN,        0, 0, 0, "rotation"),
            kf(0.0,    0, 0, 0, "position"),
            kf(LEN/2,  0, 0.01, 0, "position"),
            kf(LEN,    0, 0, 0, "position"),
            kf(0.0,    1, 1, 1, "scale"),
            kf(LEN/2,  1, 1, 1, "scale"),
            kf(LEN,    1, 1, 1, "scale"),
        ]}

    # Seat edges + trim — tiny pulse
    for i, buid in enumerate(seat_edge_bids + trim_bids):
        phase = i * 0.7
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,        1, 1, 1, "scale"),
            kf(LEN * 0.25, 1.005 + 0.005 * math.sin(phase), 1.005, 1.005, "scale"),
            kf(LEN * 0.5,  1, 1, 1, "scale"),
            kf(LEN * 0.75, 0.995, 0.995, 0.995, "scale"),
            kf(LEN,        1, 1, 1, "scale"),
            kf(0.0, 0,0,0,"position"), kf(LEN/2,0,0,0,"position"), kf(LEN,0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(LEN/2,0,0,0,"rotation"), kf(LEN,0,0,0,"rotation"),
        ]}

    # Back columns: very slow emissive pulse on individual periods (sim via Y-scale tiny)
    for i, buid in enumerate(back_col_bids):
        phase = i * 1.2
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,        1.0, 1.0 + 0.005 * math.sin(phase),         1.0, "scale"),
            kf(LEN * 0.2,  1.0, 1.0 + 0.005 * math.sin(phase + 1),     1.0, "scale"),
            kf(LEN * 0.4,  1.0, 1.0 + 0.005 * math.sin(phase + 2),     1.0, "scale"),
            kf(LEN * 0.6,  1.0, 1.0 + 0.005 * math.sin(phase + 3),     1.0, "scale"),
            kf(LEN * 0.8,  1.0, 1.0 + 0.005 * math.sin(phase + 4),     1.0, "scale"),
            kf(LEN,        1.0, 1.0 + 0.005 * math.sin(phase + 2*math.pi), 1.0, "scale"),
            kf(0.0,        0, 0, 0, "rotation"),
            kf(LEN * 0.5,  0.15 * math.sin(phase), 0, 0, "rotation"),
            kf(LEN,        0, 0, 0, "rotation"),
            kf(0.0, 0,0,0,"position"), kf(LEN/2,0,0,0,"position"), kf(LEN,0,0,0,"position"),
        ]}

    # Vertebrae — very slow rest with phase variations
    for i, buid in enumerate(vertebrae_bids):
        phase = i * 0.4
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,        1.0, 1.0, 1.0, "scale"),
            kf(LEN * 0.25, 1.01 + 0.005 * math.sin(phase), 1.01, 1.01, "scale"),
            kf(LEN * 0.5,  1.0, 1.0, 1.0, "scale"),
            kf(LEN * 0.75, 0.99, 0.99, 0.99, "scale"),
            kf(LEN,        1.0, 1.0, 1.0, "scale"),
            kf(0.0,        0, 0, 0, "position"),
            kf(LEN/2,      0, 0.005 * math.sin(phase), 0, "position"),
            kf(LEN,        0, 0, 0, "position"),
            kf(0.0,        0, 0, 0, "rotation"),
            kf(LEN/2,      0, 0, 0, "rotation"),
            kf(LEN,        0, 0, 0, "rotation"),
        ]}

    # Armrests — tiny flex
    for i, buid in enumerate(armrest_bids):
        phase = i * 0.5
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,        0, 0, 0, "rotation"),
            kf(LEN * 0.25, 0.15 * math.sin(phase), 0, 0, "rotation"),
            kf(LEN * 0.5,  0, 0, 0, "rotation"),
            kf(LEN * 0.75, -0.15 * math.sin(phase), 0, 0, "rotation"),
            kf(LEN,        0, 0, 0, "rotation"),
            kf(0.0, 0,0,0,"position"), kf(LEN/2,0,0,0,"position"), kf(LEN,0,0,0,"position"),
            kf(0.0, 1,1,1,"scale"), kf(LEN/2,1,1,1,"scale"), kf(LEN,1,1,1,"scale"),
        ]}

    # Fingers — subtle flex (rotation X)
    for i, buid in enumerate(finger_bids):
        phase = i * 0.8
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,        2 + 1 * math.sin(phase), 0, 0, "rotation"),
            kf(LEN * 0.25, 2 + 1 * math.sin(phase + 1), 0, 0, "rotation"),
            kf(LEN * 0.5,  2 + 1 * math.sin(phase + 2), 0, 0, "rotation"),
            kf(LEN * 0.75, 2 + 1 * math.sin(phase + 3), 0, 0, "rotation"),
            kf(LEN,        2 + 1 * math.sin(phase + 2*math.pi), 0, 0, "rotation"),
            kf(0.0, 0,0,0,"position"), kf(LEN/2,0,0,0,"position"), kf(LEN,0,0,0,"position"),
            kf(0.0, 1,1,1,"scale"), kf(LEN/2,1,1,1,"scale"), kf(LEN,1,1,1,"scale"),
        ]}

    # Legs — immovable, just very tiny breathing
    for i, buid in enumerate(leg_bids + leg_base_bids):
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,        1, 1, 1, "scale"),
            kf(LEN * 0.25, 1.002, 1.002, 1.002, "scale"),
            kf(LEN * 0.5,  1, 1, 1, "scale"),
            kf(LEN * 0.75, 0.998, 0.998, 0.998, "scale"),
            kf(LEN,        1, 1, 1, "scale"),
            kf(0.0, 0,0,0,"position"), kf(LEN/2,0,0,0,"position"), kf(LEN,0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(LEN/2,0,0,0,"rotation"), kf(LEN,0,0,0,"rotation"),
        ]}

    # Side spikes — slow sway
    for i, buid in enumerate(side_spike_bids):
        phase = i * 0.6
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,        0, 0, 0.1 * math.sin(phase), "rotation"),
            kf(LEN * 0.25, 0, 0, 0.1 * math.sin(phase + 1), "rotation"),
            kf(LEN * 0.5,  0, 0, 0.1 * math.sin(phase + 2), "rotation"),
            kf(LEN * 0.75, 0, 0, 0.1 * math.sin(phase + 3), "rotation"),
            kf(LEN,        0, 0, 0.1 * math.sin(phase + 2*math.pi), "rotation"),
            kf(0.0, 0,0,0,"position"), kf(LEN/2,0,0,0,"position"), kf(LEN,0,0,0,"position"),
            kf(0.0, 1,1,1,"scale"), kf(LEN/2,1,1,1,"scale"), kf(LEN,1,1,1,"scale"),
        ]}

    # Cracks — pulse emissive (Y-scale modulation)
    for i, buid in enumerate(crack_bids + crack_accent_bids):
        phase = i * 0.45
        p1 = 1.0 + 0.10 * math.sin(phase)
        p2 = 1.0 + 0.10 * math.sin(phase + math.pi/2)
        p3 = 1.0 + 0.10 * math.sin(phase + math.pi)
        p4 = 1.0 + 0.10 * math.sin(phase + 3*math.pi/2)
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,        1, p1, 1, "scale"),
            kf(LEN * 0.25, 1, p2, 1, "scale"),
            kf(LEN * 0.5,  1, p3, 1, "scale"),
            kf(LEN * 0.75, 1, p4, 1, "scale"),
            kf(LEN,        1, p1, 1, "scale"),
            kf(0.0, 0,0,0,"position"), kf(LEN/2,0,0,0,"position"), kf(LEN,0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(LEN/2,0,0,0,"rotation"), kf(LEN,0,0,0,"rotation"),
        ]}

    # Ribs, crown, trim — already handled or tiny
    for i, buid in enumerate(rib_connector_bids + crown_bids):
        phase = i * 0.55
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,        1.0, 1.0, 1.0, "scale"),
            kf(LEN * 0.25, 1.005, 1.005 + 0.005 * math.sin(phase), 1.005, "scale"),
            kf(LEN * 0.5,  1.0, 1.0, 1.0, "scale"),
            kf(LEN * 0.75, 0.995, 0.995, 0.995, "scale"),
            kf(LEN,        1.0, 1.0, 1.0, "scale"),
            kf(0.0, 0,0,0,"position"), kf(LEN/2,0,0,0,"position"), kf(LEN,0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(LEN/2,0,0,0,"rotation"), kf(LEN,0,0,0,"rotation"),
        ]}

    return animators


def build_dissipate():
    """dissipate once, 1.1s.
    - side spikes retract
    - back columns sink top-first
    - armrests fold inward
    - seat sinks below ground
    - legs retract
    - ground cracks contract"""
    animators = {}
    LEN = 1.1

    # Side spikes retract first (at 0.0s)
    for i, buid in enumerate(side_spike_bids):
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,    1, 1, 1, "scale"),
            kf(0.10,   1, 0.6, 1, "scale"),
            kf(0.20,   1, 0.05, 1, "scale"),
            kf(0.25,   0, 0, 0, "scale"),
            kf(LEN/2,  0, 0, 0, "scale"),
            kf(LEN,    0, 0, 0, "scale"),
            kf(0.0, 0,0,0,"position"), kf(LEN/2,0,0,0,"position"), kf(LEN,0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(LEN/2,0,0,0,"rotation"), kf(LEN,0,0,0,"rotation"),
        ]}
    # Crown spikes fold
    for i, buid in enumerate(crown_bids):
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,    1, 1, 1, "scale"),
            kf(0.15,   1, 0.5, 1, "scale"),
            kf(0.30,   0, 0, 0, "scale"),
            kf(LEN/2,  0, 0, 0, "scale"),
            kf(LEN,    0, 0, 0, "scale"),
            kf(0.0, 0,0,0,"position"), kf(LEN/2,0,0,0,"position"), kf(LEN,0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(LEN/2,0,0,0,"rotation"), kf(LEN,0,0,0,"rotation"),
        ]}

    # Back columns sink top-first (at 0.1s, outer-to-inner stagger reversed)
    col_order = [0, 4, 1, 3, 2]  # outer first
    for order_i, col_i in enumerate(col_order):
        buid = back_col_bids[col_i]
        d = 0.1 + order_i * 0.05
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,    1, 1, 1, "scale"),
            kf(d,      1, 1, 1, "scale"),
            kf(d + 0.10, 1, 0.7, 1, "scale"),
            kf(d + 0.20, 1, 0.3, 1, "scale"),
            kf(d + 0.30, 1, 0.05, 1, "scale"),
            kf(d + 0.35, 0, 0, 0, "scale"),
            kf(LEN,    0, 0, 0, "scale"),
            kf(0.0, 0,0,0,"position"), kf(LEN/2,0,0,0,"position"), kf(LEN,0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(LEN/2,0,0,0,"rotation"), kf(LEN,0,0,0,"rotation"),
        ]}
    # Vertebrae sink with cols
    for i, buid in enumerate(vertebrae_bids):
        col_idx = i // 3
        order_pos = col_order.index(col_idx)
        d = 0.12 + order_pos * 0.05 + (i % 3) * 0.02
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,    1, 1, 1, "scale"),
            kf(d,      1, 1, 1, "scale"),
            kf(d + 0.15, 0.6, 0.6, 0.6, "scale"),
            kf(d + 0.25, 0, 0, 0, "scale"),
            kf(LEN,    0, 0, 0, "scale"),
            kf(0.0, 0,0,0,"position"), kf(LEN/2,0,0,0,"position"), kf(LEN,0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(LEN/2,0,0,0,"rotation"), kf(LEN,0,0,0,"rotation"),
        ]}
    # Ribs retract
    for i, buid in enumerate(rib_connector_bids):
        d = 0.15 + i * 0.02
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,    1, 1, 1, "scale"),
            kf(d,      1, 1, 1, "scale"),
            kf(d + 0.15, 0.05, 1, 1, "scale"),
            kf(d + 0.25, 0, 0, 0, "scale"),
            kf(LEN,    0, 0, 0, "scale"),
            kf(0.0, 0,0,0,"position"), kf(LEN/2,0,0,0,"position"), kf(LEN,0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(LEN/2,0,0,0,"rotation"), kf(LEN,0,0,0,"rotation"),
        ]}

    # Armrests fold inward — translate towards center
    for i, buid in enumerate(armrest_bids):
        side = -1 if buid in armrest_L_bids else 1
        d = 0.30 + (i % 4) * 0.04
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,    0, 0, 0, "position"),
            kf(d,      0, 0, 0, "position"),
            kf(d + 0.15, -side * 0.8, 0, -0.5, "position"),
            kf(d + 0.30, -side * 1.5, 0, -1.0, "position"),
            kf(LEN,    -side * 2.0, 0, -1.5, "position"),
            kf(0.0,    1, 1, 1, "scale"),
            kf(d,      1, 1, 1, "scale"),
            kf(d + 0.20, 0.8, 0.8, 0.8, "scale"),
            kf(d + 0.35, 0.4, 0.4, 0.4, "scale"),
            kf(LEN,    0, 0, 0, "scale"),
            kf(0.0, 0,0,0,"rotation"), kf(LEN/2,side*30,0,0,"rotation"), kf(LEN,side*60,0,0,"rotation"),
        ]}
    for i, buid in enumerate(finger_bids):
        d = 0.35 + i * 0.015
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,    1, 1, 1, "scale"),
            kf(d,      1, 1, 1, "scale"),
            kf(d + 0.10, 0.6, 0.6, 0.6, "scale"),
            kf(d + 0.20, 0, 0, 0, "scale"),
            kf(LEN,    0, 0, 0, "scale"),
            kf(0.0, 0,0,0,"position"), kf(LEN/2,0,0,0,"position"), kf(LEN,0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(LEN/2,0,0,0,"rotation"), kf(LEN,0,0,0,"rotation"),
        ]}

    # Seat sinks below ground at 0.6s
    for i, buid in enumerate(seat_bids):
        d = 0.6
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,    0, 0, 0, "position"),
            kf(d,      0, 0, 0, "position"),
            kf(d + 0.15, 0, -0.8, 0, "position"),
            kf(d + 0.30, 0, -1.8, 0, "position"),
            kf(LEN,    0, -3.0, 0, "position"),
            kf(0.0,    1, 1, 1, "scale"),
            kf(d,      1, 1, 1, "scale"),
            kf(d + 0.30, 1, 0.7, 1, "scale"),
            kf(LEN,    1, 0.05, 1, "scale"),
            kf(0.0, 0,0,0,"rotation"), kf(LEN/2,0,0,0,"rotation"), kf(LEN,0,0,0,"rotation"),
        ]}
    # Seat edges + trim sink with seat
    for i, buid in enumerate(seat_edge_bids + trim_bids):
        d = 0.6 + i * 0.01
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,    0, 0, 0, "position"),
            kf(d,      0, 0, 0, "position"),
            kf(d + 0.15, 0, -0.8, 0, "position"),
            kf(LEN,    0, -2.5, 0, "position"),
            kf(0.0,    1, 1, 1, "scale"),
            kf(LEN/2,  1, 0.6, 1, "scale"),
            kf(LEN,    0, 0, 0, "scale"),
            kf(0.0, 0,0,0,"rotation"), kf(LEN/2,0,0,0,"rotation"), kf(LEN,0,0,0,"rotation"),
        ]}

    # Legs retract at 0.7s
    for i, buid in enumerate(leg_bids):
        d = 0.7 + (i // 2) * 0.03
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,    0, 0, 0, "position"),
            kf(d,      0, 0, 0, "position"),
            kf(d + 0.15, 0, -1.2, 0, "position"),
            kf(LEN,    0, -3.5, 0, "position"),
            kf(0.0,    1, 1, 1, "scale"),
            kf(LEN/2,  1, 1, 1, "scale"),
            kf(LEN,    1, 1, 1, "scale"),
            kf(0.0, 0,0,0,"rotation"), kf(LEN/2,0,0,0,"rotation"), kf(LEN,0,0,0,"rotation"),
        ]}
    for i, buid in enumerate(leg_base_bids):
        d = 0.75 + i * 0.02
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,    1, 1, 1, "scale"),
            kf(d,      1, 1, 1, "scale"),
            kf(d + 0.15, 0.5, 0.5, 0.5, "scale"),
            kf(LEN,    0, 0, 0, "scale"),
            kf(0.0, 0,0,0,"position"), kf(LEN/2,0,0,0,"position"), kf(LEN,0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(LEN/2,0,0,0,"rotation"), kf(LEN,0,0,0,"rotation"),
        ]}

    # Ground cracks contract last
    for i, buid in enumerate(crack_bids + crack_accent_bids):
        d = 0.85 + i * 0.012
        animators[buid] = {"name": bone_name_map[buid], "type": "bone", "keyframes": [
            kf(0.0,    1, 1, 1, "scale"),
            kf(d,      1, 1, 1, "scale"),
            kf(d + 0.10, 0.6, 1, 0.6, "scale"),
            kf(d + 0.18, 0, 0, 0, "scale"),
            kf(LEN,    0, 0, 0, "scale"),
            kf(0.0, 0,0,0,"position"), kf(LEN/2,0,0,0,"position"), kf(LEN,0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(LEN/2,0,0,0,"rotation"), kf(LEN,0,0,0,"rotation"),
        ]}

    return animators


# ── Build animations ────────────────────────────────────────────
spawn_anim = {
    "uuid": aid(1), "name": "animation.bone_throne_summon.spawn",
    "loop": "once", "length": 1.4, "snapping": 24,
    "selected": False, "saved": False, "animators": build_spawn()
}
idle_anim = {
    "uuid": aid(2), "name": "animation.bone_throne_summon.idle",
    "loop": "loop", "length": 8.0, "snapping": 24,
    "selected": False, "saved": False, "animators": build_idle()
}
dissipate_anim = {
    "uuid": aid(3), "name": "animation.bone_throne_summon.dissipate",
    "loop": "once", "length": 1.1, "snapping": 24,
    "selected": False, "saved": False, "animators": build_dissipate()
}

outliner = [b["uuid"] for b in bones]

textures = [
    {"id": "0", "name": "bone_throne_summon_tex",
     "relative_path": "../textures/bone_throne_summon_tex.png",
     "folder": "devilsdream", "namespace": "", "visible": True, "mode": "bitmap",
     "saved": False, "uuid": tid(1), "source": f"data:image/png;base64,{b64_0}"},
    {"id": "1", "name": "bone_throne_summon_tex1",
     "relative_path": "../textures/bone_throne_summon_tex1.png",
     "folder": "devilsdream", "namespace": "", "visible": True, "mode": "bitmap",
     "saved": False, "uuid": tid(2), "source": f"data:image/png;base64,{b64_1}"},
]

model = {
    "meta": {"format_version": "4.10", "model_format": "free", "box_uv": False},
    "name": "bone_throne_summon",
    "geometry": "geometry.bone_throne_summon",
    "resolution": {"width": 64, "height": 64},
    "elements": elements,
    "outliner": outliner,
    "textures": textures,
    "animations": [spawn_anim, idle_anim, dissipate_anim]
}

out_path = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/bone_throne_summon.bbmodel"
with open(out_path, "w", encoding="utf-8") as f:
    json.dump(model, f, separators=(',', ':'))

size = os.path.getsize(out_path)
print(f"Written: {out_path}")
print(f"Size: {size:,} bytes ({size/1024:.1f} KB)")
print(f"Elements: {len(elements)}, Bones: {len(bones)}")
print(f"Animations: {len([spawn_anim, idle_anim, dissipate_anim])}")
print(f"Spawn animators: {len(spawn_anim['animators'])}")
print(f"Idle animators: {len(idle_anim['animators'])}")
print(f"Dissipate animators: {len(dissipate_anim['animators'])}")
pf = "PASS" if size >= 300*1024 else "FAIL"
print(f"{pf}: {'>=300KB' if size >= 300*1024 else f'only {size//1024}KB'}")
