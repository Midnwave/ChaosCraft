"""
Build devils_halo_array.bbmodel
7 broken halos orbiting at 7 different heights (Y=3..14), radii 4.0..5.5,
each halo = 12 flat slabs forming a horizontal ring (face-up like a halo).
Plus 7 ghost orbital path rings (8 thin slabs each), 7 crack details, 4 center cluster cubes.
Element/bone count well above 80/55, file size >= 300KB.
"""
import json, math, os

b64_0 = open("D:/CC/ChaosCraft/tools/gen_textures/m17_b64.txt").read().strip()
b64_1 = open("D:/CC/ChaosCraft/tools/gen_textures/m17_tex1_b64.txt").read().strip()


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

# 7 halos at 7 distinct heights, slightly different radii, slightly different tilts
HALO_DEFS = [
    # (Y_height, radius, tilt_deg_x, tilt_deg_z)
    (3.0,  4.0, 0,   0),
    (5.0,  4.3, 8,   2),
    (6.5,  4.7, -5,  4),
    (8.5,  5.5, 0,   0),
    (10.0, 5.2, 12,  -3),
    (12.0, 4.9, -8,  6),
    (14.0, 4.4, 5,   -2),
]

# Each halo = 12 flat slabs in a horizontal circle.
# Slab dimensions: each slab is a small flat tile, ~ 0.8 wide tangentially,
# 0.25 thick (Y), 0.6 deep radially. We'll place them around the radius.
halo_parent_bids = []
halo_slab_bids = []  # [[12 slabs]] per halo
halo_crack_bids = []  # [[1-2 cracks]] per halo

for h_idx, (cy, R, tilt_x, tilt_z) in enumerate(HALO_DEFS):
    # Halo parent bone — its origin is the halo center; rotation is the tilt
    parent_uid = bid(bi)
    halo_parent_bids.append(parent_uid)
    parent_children = []
    bi_parent = bi
    bi += 1

    # 12 slabs around the ring
    slabs_for_this_halo = []
    NUM_SLABS = 12
    for s in range(NUM_SLABS):
        ang = (s / NUM_SLABS) * 2 * math.pi
        sx = math.cos(ang) * R
        sz = math.sin(ang) * R
        # Slab: small flat horizontal tile centered around (sx, cy, sz).
        # Make it 0.7 wide tangentially, 0.25 tall (Y), 0.55 deep radially
        # We'll axis-align for simplicity (slight breaking of perfect circle is fine)
        half_w = 0.36
        half_h = 0.12
        half_d = 0.28
        fr = [sx - half_w, cy - half_h, sz - half_d]
        to = [sx + half_w, cy + half_h, sz + half_d]
        e_name = f"halo{h_idx + 1}_slab{s + 1}"
        e = elem(ei, e_name, fr, to, UV0); elements.append(e)
        # Each slab is its own child bone for individual animation
        b = bone(bi, e_name, [sx, cy, sz], [eid(ei)],
                 rot=[0, math.degrees(-ang), 0])
        bones.append(b)
        slabs_for_this_halo.append(bid(bi))
        parent_children.append(bid(bi))
        ei += 1
        bi += 1

    halo_slab_bids.append(slabs_for_this_halo)

    # 1-2 crack detail bones — thin slabs crossing the ring at an angle
    crack_count = 1 + (h_idx % 2)  # 1 or 2 cracks per halo
    cracks_for_this_halo = []
    for c in range(crack_count):
        # Crack slab: a long thin diagonal piece across part of the ring
        ang0 = (c * 0.7 + 0.2) * 2 * math.pi  # angle on the ring
        ang1 = ang0 + math.radians(30)  # spans 30°
        x0 = math.cos(ang0) * (R - 0.4)
        z0 = math.sin(ang0) * (R - 0.4)
        x1 = math.cos(ang1) * (R + 0.2)
        z1 = math.sin(ang1) * (R + 0.2)
        fx0, fx1 = sorted([x0, x1])
        fz0, fz1 = sorted([z0, z1])
        # Make it thin
        if fx1 - fx0 < 0.3:
            fx0 -= 0.15; fx1 += 0.15
        if fz1 - fz0 < 0.3:
            fz0 -= 0.15; fz1 += 0.15
        fr = [fx0, cy - 0.05, fz0]
        to = [fx1, cy + 0.05, fz1]
        e_name = f"halo{h_idx + 1}_crack{c + 1}"
        e = elem(ei, e_name, fr, to, UV0); elements.append(e)
        cx_orig = (fx0 + fx1) / 2
        cz_orig = (fz0 + fz1) / 2
        b = bone(bi, e_name, [cx_orig, cy, cz_orig], [eid(ei)])
        bones.append(b)
        cracks_for_this_halo.append(bid(bi))
        parent_children.append(bid(bi))
        ei += 1
        bi += 1
    halo_crack_bids.append(cracks_for_this_halo)

    # Now create the halo parent bone with all children (slabs + cracks)
    halo_parent = bone(bi_parent, f"halo{h_idx + 1}_parent", [0, cy, 0],
                       parent_children, rot=[tilt_x, 0, tilt_z])
    bones.append(halo_parent)

# Center cluster: 4 small cubes at center Y=8.5
center_cluster_bids = []
center_cluster_offsets = [
    (0.3, 0.3, 0.3),
    (-0.3, 0.3, -0.3),
    (0.3, -0.3, -0.3),
    (-0.3, -0.3, 0.3),
]
for i, (cdx, cdy, cdz) in enumerate(center_cluster_offsets):
    fr = [cdx - 0.25, 8.5 + cdy - 0.25, cdz - 0.25]
    to = [cdx + 0.25, 8.5 + cdy + 0.25, cdz + 0.25]
    e_name = f"center_{i + 1}"
    e = elem(ei, e_name, fr, to, UV1); elements.append(e)
    b = bone(bi, e_name, [cdx, 8.5 + cdy, cdz], [eid(ei)])
    bones.append(b)
    center_cluster_bids.append(bid(bi))
    ei += 1
    bi += 1

# Orbital path rings: 7 ghost-faint rings at the 7 halo heights, each 8 slabs (very thin)
orbital_path_bids = []  # [[8 slabs]] per orbital
orbital_parent_bids = []
for h_idx, (cy, R, _, _) in enumerate(HALO_DEFS):
    parent_uid = bid(bi)
    orbital_parent_bids.append(parent_uid)
    parent_children = []
    bi_parent = bi
    bi += 1

    NUM = 8
    slab_list = []
    for s in range(NUM):
        ang = (s / NUM) * 2 * math.pi + 0.2  # offset slightly so not aligned with halo slabs
        sx = math.cos(ang) * (R + 0.15)  # very slightly outside halo
        sz = math.sin(ang) * (R + 0.15)
        # Very thin slab
        half_w = 0.28
        half_h = 0.04  # super thin in Y
        half_d = 0.20
        fr = [sx - half_w, cy - half_h, sz - half_d]
        to = [sx + half_w, cy + half_h, sz + half_d]
        e_name = f"orbital{h_idx + 1}_slab{s + 1}"
        e = elem(ei, e_name, fr, to, UV1); elements.append(e)
        b = bone(bi, e_name, [sx, cy, sz], [eid(ei)],
                 rot=[0, math.degrees(-ang), 0])
        bones.append(b)
        slab_list.append(bid(bi))
        parent_children.append(bid(bi))
        ei += 1
        bi += 1
    orbital_path_bids.append(slab_list)

    orbital_parent = bone(bi_parent, f"orbital{h_idx + 1}_parent",
                          [0, cy, 0], parent_children)
    bones.append(orbital_parent)

print(f"Elements: {len(elements)}, Bones: {len(bones)}")

# Build flat list of all leaf-level (non-parent) bones for animation purposes:
# We animate the 7 halo parents (rotation), then individual slabs (scale pulses),
# crack bones (emissive pulse approximated by scale), center cluster (scale + rotate),
# and orbital parents + orbital slabs.

bone_name_map = {b["uuid"]: b["name"] for b in bones}
all_leaf_bids = []
for slabs in halo_slab_bids:
    all_leaf_bids.extend(slabs)
for cracks in halo_crack_bids:
    all_leaf_bids.extend(cracks)
for slabs in orbital_path_bids:
    all_leaf_bids.extend(slabs)
all_leaf_bids.extend(center_cluster_bids)
# parents
all_parent_bids = halo_parent_bids + orbital_parent_bids


# ════════════════════════════════════════════════════════════════
# ANIMATIONS
# ════════════════════════════════════════════════════════════════


def build_spawn():
    """spawn once, 1.3s. Center cluster materialises 0.0s.
    Orbital path rings expand from 0 at 0.2s sequentially.
    7 halos fly in from outer space at staggered approach angles, snap into orbit with bounce."""
    animators = {}
    LEN = 1.3

    # Halo parents — fly in from outer space at angle, settle bounce
    for h_idx, parent_uid in enumerate(halo_parent_bids):
        cy, R, tilt_x, tilt_z = HALO_DEFS[h_idx]
        # Approach from a unique direction far away
        appr_angle = h_idx * (2 * math.pi / 7) + 0.4
        appr_dist = 30
        ax = math.cos(appr_angle) * appr_dist
        az = math.sin(appr_angle) * appr_dist
        ay = 8 + (h_idx - 3) * 4  # vertical variance — translate offset, since parent origin already at cy

        delay = h_idx * 0.12
        end_t = min(delay + 0.45, LEN)
        bounce_t = min(delay + 0.55, LEN)
        animators[parent_uid] = {"name": bone_name_map[parent_uid], "type": "bone",
                                 "keyframes": [
            kf(0.0,    ax, ay - cy, az, "position"),
            kf(delay,  ax, ay - cy, az, "position"),
            kf(delay + 0.30, ax * 0.2, (ay - cy) * 0.2, az * 0.2, "position"),
            kf(end_t, 0, 0.5, 0, "position"),  # overshoot
            kf(bounce_t, 0, -0.2, 0, "position"),
            kf(LEN,    0, 0, 0, "position"),
            kf(0.0,    0, 0, 0, "scale"),
            kf(delay,  0, 0, 0, "scale"),
            kf(delay + 0.30, 0.6, 0.6, 0.6, "scale"),
            kf(end_t,  1.1, 1.1, 1.1, "scale"),
            kf(bounce_t, 0.95, 0.95, 0.95, "scale"),
            kf(LEN,    1, 1, 1, "scale"),
            kf(0.0,    tilt_x, h_idx * 30, tilt_z, "rotation"),
            kf(delay,  tilt_x, h_idx * 30, tilt_z, "rotation"),
            kf(end_t,  tilt_x, h_idx * 60, tilt_z, "rotation"),
            kf(LEN,    tilt_x, h_idx * 60, tilt_z, "rotation"),
        ]}

    # Halo slabs — local scale ramp, default
    for h_idx, slabs in enumerate(halo_slab_bids):
        delay = h_idx * 0.12
        for s_idx, suid in enumerate(slabs):
            slab_delay = delay + s_idx * 0.005
            animators[suid] = {"name": bone_name_map[suid], "type": "bone",
                               "keyframes": [
                kf(0.0,        1, 1, 1, "scale"),
                kf(slab_delay, 1, 1, 1, "scale"),
                kf(slab_delay + 0.05, 0.85, 0.85, 0.85, "scale"),
                kf(slab_delay + 0.2,  1.05, 1.05, 1.05, "scale"),
                kf(LEN - 0.1, 1, 1, 1, "scale"),
                kf(LEN,       1, 1, 1, "scale"),
                kf(0.0,    0, 0, 0, "position"),
                kf(LEN/3,  0, 0, 0, "position"),
                kf(2*LEN/3, 0, 0, 0, "position"),
                kf(LEN,    0, 0, 0, "position"),
                kf(0.0,    0, 0, 0, "rotation"),
                kf(LEN/2,  0, 0, 0, "rotation"),
                kf(LEN,    0, 0, 0, "rotation"),
            ]}

    # Halo cracks
    for h_idx, cracks in enumerate(halo_crack_bids):
        delay = h_idx * 0.12 + 0.1
        for c_idx, cuid in enumerate(cracks):
            animators[cuid] = {"name": bone_name_map[cuid], "type": "bone",
                               "keyframes": [
                kf(0.0,           0, 0, 0, "scale"),
                kf(delay,         0, 0, 0, "scale"),
                kf(delay + 0.10,  1, 1, 1, "scale"),
                kf(delay + 0.20,  0.9, 0.9, 0.9, "scale"),
                kf(LEN - 0.1,     1, 1, 1, "scale"),
                kf(LEN,           1, 1, 1, "scale"),
                kf(0.0,    0, 0, 0, "position"),
                kf(LEN/2,  0, 0, 0, "position"),
                kf(LEN,    0, 0, 0, "position"),
                kf(0.0,    0, 0, 0, "rotation"),
                kf(LEN/2,  0, 0, 0, "rotation"),
                kf(LEN,    0, 0, 0, "rotation"),
            ]}

    # Center cluster materializes at 0.0s
    for i, cuid in enumerate(center_cluster_bids):
        animators[cuid] = {"name": bone_name_map[cuid], "type": "bone",
                           "keyframes": [
            kf(0.0,    0, 0, 0, "scale"),
            kf(0.05,   0, 0, 0, "scale"),
            kf(0.15,   1.4, 1.4, 1.4, "scale"),
            kf(0.30,   1.0, 1.0, 1.0, "scale"),
            kf(LEN/2,  1.0, 1.0, 1.0, "scale"),
            kf(LEN,    1.0, 1.0, 1.0, "scale"),
            kf(0.0,    0, 0, 0, "position"),
            kf(LEN/3,  0, 0, 0, "position"),
            kf(2*LEN/3, 0, 0, 0, "position"),
            kf(LEN,    0, 0, 0, "position"),
            kf(0.0,    0, 0, 0, "rotation"),
            kf(LEN/2,  0, 180 * (1 if i % 2 == 0 else -1), 0, "rotation"),
            kf(LEN,    0, 360 * (1 if i % 2 == 0 else -1), 0, "rotation"),
        ]}

    # Orbital parents — ring scale up sequentially starting at 0.2s
    for h_idx, puid in enumerate(orbital_parent_bids):
        d = 0.2 + h_idx * 0.08
        animators[puid] = {"name": bone_name_map[puid], "type": "bone",
                           "keyframes": [
            kf(0.0,    0, 0, 0, "scale"),
            kf(d,      0, 0, 0, "scale"),
            kf(d + 0.15, 1.1, 1.1, 1.1, "scale"),
            kf(d + 0.30, 1.0, 1.0, 1.0, "scale"),
            kf(LEN - 0.1, 1, 1, 1, "scale"),
            kf(LEN,    1, 1, 1, "scale"),
            kf(0.0,    0, 0, 0, "position"),
            kf(LEN/2,  0, 0, 0, "position"),
            kf(LEN,    0, 0, 0, "position"),
            kf(0.0,    0, 0, 0, "rotation"),
            kf(LEN/2,  0, -45, 0, "rotation"),
            kf(LEN,    0, -90, 0, "rotation"),
        ]}

    # Orbital slabs
    for h_idx, slabs in enumerate(orbital_path_bids):
        for s_idx, suid in enumerate(slabs):
            d = 0.2 + h_idx * 0.08 + s_idx * 0.005
            animators[suid] = {"name": bone_name_map[suid], "type": "bone",
                               "keyframes": [
                kf(0.0,    1, 1, 1, "scale"),
                kf(d,      1, 1, 1, "scale"),
                kf(d + 0.05, 0.8, 0.8, 0.8, "scale"),
                kf(d + 0.15, 1.05, 1.05, 1.05, "scale"),
                kf(LEN - 0.1, 1, 1, 1, "scale"),
                kf(LEN,    1, 1, 1, "scale"),
                kf(0.0,    0, 0, 0, "position"),
                kf(LEN/2,  0, 0, 0, "position"),
                kf(LEN,    0, 0, 0, "position"),
                kf(0.0,    0, 0, 0, "rotation"),
                kf(LEN/2,  0, 0, 0, "rotation"),
                kf(LEN,    0, 0, 0, "rotation"),
            ]}

    return animators


def build_idle():
    """idle loop, 7.0s. Each halo orbits on Y axis at speeds 4-9s per orbit.
    Individual halo slabs pulse scale slightly. Cracks pulse on own periods.
    Central cluster rotates. Orbital path rings counter-rotate at half speed."""
    animators = {}
    LEN = 7.0
    # Halo parents — Y rotation, full orbit speed varies per halo
    halo_orbit_seconds = [4.0, 5.0, 5.5, 6.5, 7.0, 8.0, 9.0]
    for h_idx, parent_uid in enumerate(halo_parent_bids):
        cy, R, tilt_x, tilt_z = HALO_DEFS[h_idx]
        period = halo_orbit_seconds[h_idx]
        # Number of full rotations during 7s loop
        rotations = LEN / period
        # We need to loop seamlessly — final angle must equal initial angle (mod 360)
        # so we make total = round(rotations) * 360
        total_deg = round(rotations) * 360 if rotations >= 1 else 360
        direction = 1 if h_idx % 2 == 0 else -1
        animators[parent_uid] = {"name": bone_name_map[parent_uid], "type": "bone",
                                 "keyframes": [
            kf(0.0,         tilt_x, 0,                              tilt_z, "rotation"),
            kf(LEN * 0.2,   tilt_x, direction * total_deg * 0.2,    tilt_z, "rotation"),
            kf(LEN * 0.4,   tilt_x, direction * total_deg * 0.4,    tilt_z, "rotation"),
            kf(LEN * 0.6,   tilt_x, direction * total_deg * 0.6,    tilt_z, "rotation"),
            kf(LEN * 0.8,   tilt_x, direction * total_deg * 0.8,    tilt_z, "rotation"),
            kf(LEN,         tilt_x, direction * total_deg,           tilt_z, "rotation"),
            kf(0.0,    0, 0, 0, "position"),
            kf(LEN/3,  0, 0.05 * math.sin(h_idx), 0, "position"),
            kf(2*LEN/3, 0, -0.05 * math.sin(h_idx), 0, "position"),
            kf(LEN,    0, 0, 0, "position"),
            kf(0.0,    1, 1, 1, "scale"),
            kf(LEN/3,  1.01, 1.01, 1.01, "scale"),
            kf(2*LEN/3, 0.99, 0.99, 0.99, "scale"),
            kf(LEN,    1, 1, 1, "scale"),
        ]}

    # Halo slabs: pulse scale on individual periods
    for h_idx, slabs in enumerate(halo_slab_bids):
        for s_idx, suid in enumerate(slabs):
            phase = (s_idx + h_idx * 1.7) * 0.5
            p1 = 1.0 + 0.06 * math.sin(phase)
            p2 = 1.0 + 0.06 * math.sin(phase + math.pi/3)
            p3 = 1.0 + 0.06 * math.sin(phase + 2*math.pi/3)
            p4 = 1.0 + 0.06 * math.sin(phase + math.pi)
            p5 = 1.0 + 0.06 * math.sin(phase + 4*math.pi/3)
            animators[suid] = {"name": bone_name_map[suid], "type": "bone",
                               "keyframes": [
                kf(0.0,        p1, p1, p1, "scale"),
                kf(LEN * 0.2,  p2, p2, p2, "scale"),
                kf(LEN * 0.4,  p3, p3, p3, "scale"),
                kf(LEN * 0.6,  p4, p4, p4, "scale"),
                kf(LEN * 0.8,  p5, p5, p5, "scale"),
                kf(LEN,        p1, p1, p1, "scale"),
                kf(0.0,    0, 0, 0, "position"),
                kf(LEN/2,  0, 0.02 * math.sin(phase), 0, "position"),
                kf(LEN,    0, 0, 0, "position"),
                kf(0.0,    0, 0, 0, "rotation"),
                kf(LEN/2,  0, 0, 0, "rotation"),
                kf(LEN,    0, 0, 0, "rotation"),
            ]}

    # Cracks — emissive pulse approximated by scale Y pulse + slight wobble
    for h_idx, cracks in enumerate(halo_crack_bids):
        for c_idx, cuid in enumerate(cracks):
            phase = h_idx * 0.7 + c_idx * 1.3
            animators[cuid] = {"name": bone_name_map[cuid], "type": "bone",
                               "keyframes": [
                kf(0.0,        1.0, 1.0 + 0.10 * math.sin(phase), 1.0, "scale"),
                kf(LEN * 0.2,  1.0, 1.0 + 0.10 * math.sin(phase + math.pi/2), 1.0, "scale"),
                kf(LEN * 0.4,  1.0, 1.0 + 0.10 * math.sin(phase + math.pi), 1.0, "scale"),
                kf(LEN * 0.6,  1.0, 1.0 + 0.10 * math.sin(phase + 3*math.pi/2), 1.0, "scale"),
                kf(LEN * 0.8,  1.0, 1.0 + 0.10 * math.sin(phase + 2*math.pi), 1.0, "scale"),
                kf(LEN,        1.0, 1.0 + 0.10 * math.sin(phase), 1.0, "scale"),
                kf(0.0,    0, 0, 0, "position"),
                kf(LEN/2,  0, 0, 0, "position"),
                kf(LEN,    0, 0, 0, "position"),
                kf(0.0,    0, 0, 0, "rotation"),
                kf(LEN/2,  0, 0, 0, "rotation"),
                kf(LEN,    0, 0, 0, "rotation"),
            ]}

    # Center cluster — slow Y rotation
    for i, cuid in enumerate(center_cluster_bids):
        direction = 1 if i % 2 == 0 else -1
        animators[cuid] = {"name": bone_name_map[cuid], "type": "bone",
                           "keyframes": [
            kf(0.0,        0, 0, 0, "rotation"),
            kf(LEN * 0.2,  0, direction * 72, 0, "rotation"),
            kf(LEN * 0.4,  0, direction * 144, 0, "rotation"),
            kf(LEN * 0.6,  0, direction * 216, 0, "rotation"),
            kf(LEN * 0.8,  0, direction * 288, 0, "rotation"),
            kf(LEN,        0, direction * 360, 0, "rotation"),
            kf(0.0,    0, 0, 0, "position"),
            kf(LEN/3,  0.04 * math.sin(i), 0.02, 0, "position"),
            kf(2*LEN/3, -0.04 * math.sin(i), -0.02, 0, "position"),
            kf(LEN,    0, 0, 0, "position"),
            kf(0.0,    1, 1, 1, "scale"),
            kf(LEN/2,  1.04, 1.04, 1.04, "scale"),
            kf(LEN,    1, 1, 1, "scale"),
        ]}

    # Orbital parents — counter-rotate at half speed of corresponding halo
    for h_idx, puid in enumerate(orbital_parent_bids):
        cy, R, _, _ = HALO_DEFS[h_idx]
        period = halo_orbit_seconds[h_idx] * 2  # half speed
        rotations = LEN / period
        total_deg = max(180, round(rotations) * 360)
        direction = -1 if h_idx % 2 == 0 else 1  # counter to halo
        animators[puid] = {"name": bone_name_map[puid], "type": "bone",
                           "keyframes": [
            kf(0.0,        0, 0,                            0, "rotation"),
            kf(LEN * 0.2,  0, direction * total_deg * 0.2,  0, "rotation"),
            kf(LEN * 0.4,  0, direction * total_deg * 0.4,  0, "rotation"),
            kf(LEN * 0.6,  0, direction * total_deg * 0.6,  0, "rotation"),
            kf(LEN * 0.8,  0, direction * total_deg * 0.8,  0, "rotation"),
            kf(LEN,        0, direction * total_deg,        0, "rotation"),
            kf(0.0,    0, 0, 0, "position"),
            kf(LEN/2,  0, 0, 0, "position"),
            kf(LEN,    0, 0, 0, "position"),
            kf(0.0,    1, 1, 1, "scale"),
            kf(LEN/2,  1, 1, 1, "scale"),
            kf(LEN,    1, 1, 1, "scale"),
        ]}

    # Orbital slabs — gentle pulse
    for h_idx, slabs in enumerate(orbital_path_bids):
        for s_idx, suid in enumerate(slabs):
            phase = (s_idx + h_idx * 1.3) * 0.4
            p1 = 1.0 + 0.04 * math.sin(phase)
            p2 = 1.0 + 0.04 * math.sin(phase + math.pi/2)
            p3 = 1.0 + 0.04 * math.sin(phase + math.pi)
            p4 = 1.0 + 0.04 * math.sin(phase + 3*math.pi/2)
            animators[suid] = {"name": bone_name_map[suid], "type": "bone",
                               "keyframes": [
                kf(0.0,        p1, p1, p1, "scale"),
                kf(LEN * 0.25, p2, p2, p2, "scale"),
                kf(LEN * 0.5,  p3, p3, p3, "scale"),
                kf(LEN * 0.75, p4, p4, p4, "scale"),
                kf(LEN,        p1, p1, p1, "scale"),
                kf(0.0,    0, 0, 0, "position"),
                kf(LEN/2,  0, 0, 0, "position"),
                kf(LEN,    0, 0, 0, "position"),
                kf(0.0,    0, 0, 0, "rotation"),
                kf(LEN/2,  0, 0, 0, "rotation"),
                kf(LEN,    0, 0, 0, "rotation"),
            ]}

    return animators


def build_dissipate():
    """dissipate once, 0.9s. All 7 halos fly outward in their orbit tangent direction,
    each scaling to 0 at maximum distance. Orbital path rings expand and snap.
    Central cluster implodes."""
    animators = {}
    LEN = 0.9

    # Halo parents — fly outward (translate sideways) and scale to 0
    for h_idx, parent_uid in enumerate(halo_parent_bids):
        cy, R, tilt_x, tilt_z = HALO_DEFS[h_idx]
        # tangent direction = different per halo
        tangent_angle = h_idx * (2 * math.pi / 7) + 0.3
        fly_dist = 25
        fx = math.cos(tangent_angle) * fly_dist
        fz = math.sin(tangent_angle) * fly_dist
        animators[parent_uid] = {"name": bone_name_map[parent_uid], "type": "bone",
                                 "keyframes": [
            kf(0.0,    0, 0, 0, "position"),
            kf(0.20,   fx * 0.15, 0, fz * 0.15, "position"),
            kf(0.50,   fx * 0.5,  -1, fz * 0.5, "position"),
            kf(0.75,   fx * 0.85, -2, fz * 0.85, "position"),
            kf(LEN,    fx,        -3, fz, "position"),
            kf(0.0,    1, 1, 1, "scale"),
            kf(0.20,   1.1, 1.1, 1.1, "scale"),
            kf(0.50,   0.7, 0.7, 0.7, "scale"),
            kf(0.75,   0.3, 0.3, 0.3, "scale"),
            kf(LEN,    0, 0, 0, "scale"),
            kf(0.0,    tilt_x, 0,    tilt_z, "rotation"),
            kf(0.30,   tilt_x + 10, 90, tilt_z + 5, "rotation"),
            kf(0.60,   tilt_x + 25, 180, tilt_z + 10, "rotation"),
            kf(LEN,    tilt_x + 45, 360, tilt_z + 20, "rotation"),
        ]}

    # Halo slabs — gentle scale fade with separation
    for h_idx, slabs in enumerate(halo_slab_bids):
        for s_idx, suid in enumerate(slabs):
            d = (s_idx % 4) * 0.04
            animators[suid] = {"name": bone_name_map[suid], "type": "bone",
                               "keyframes": [
                kf(0.0,           1, 1, 1, "scale"),
                kf(0.10 + d,      1.05, 1.05, 1.05, "scale"),
                kf(0.30 + d,      0.8, 0.8, 0.8, "scale"),
                kf(0.60 + d,      0.4, 0.4, 0.4, "scale"),
                kf(LEN,           0, 0, 0, "scale"),
                kf(0.0,    0, 0, 0, "position"),
                kf(0.30,   0, 0.05, 0, "position"),
                kf(0.60,   0, 0.10, 0, "position"),
                kf(LEN,    0, 0.15, 0, "position"),
                kf(0.0,    0, 0, 0, "rotation"),
                kf(0.45,   0, s_idx * 5, 0, "rotation"),
                kf(LEN,    0, s_idx * 10, 0, "rotation"),
            ]}

    # Halo cracks — fade with halo
    for h_idx, cracks in enumerate(halo_crack_bids):
        for c_idx, cuid in enumerate(cracks):
            animators[cuid] = {"name": bone_name_map[cuid], "type": "bone",
                               "keyframes": [
                kf(0.0,    1, 1, 1, "scale"),
                kf(0.20,   1.2, 1.2, 1.2, "scale"),
                kf(0.45,   0.9, 0.9, 0.9, "scale"),
                kf(0.70,   0.4, 0.4, 0.4, "scale"),
                kf(LEN,    0, 0, 0, "scale"),
                kf(0.0,    0, 0, 0, "position"),
                kf(LEN/2,  0, 0, 0, "position"),
                kf(LEN,    0, 0, 0, "position"),
                kf(0.0,    0, 0, 0, "rotation"),
                kf(LEN/2,  10, 0, 0, "rotation"),
                kf(LEN,    20, 0, 0, "rotation"),
            ]}

    # Center cluster — implodes
    for i, cuid in enumerate(center_cluster_bids):
        animators[cuid] = {"name": bone_name_map[cuid], "type": "bone",
                           "keyframes": [
            kf(0.0,    1, 1, 1, "scale"),
            kf(0.20,   1.5, 1.5, 1.5, "scale"),
            kf(0.40,   0.6, 0.6, 0.6, "scale"),
            kf(0.60,   0.2, 0.2, 0.2, "scale"),
            kf(LEN,    0, 0, 0, "scale"),
            kf(0.0,    0, 0, 0, "position"),
            kf(0.30,   0, 0, 0, "position"),
            kf(0.60,   0, 0, 0, "position"),
            kf(LEN,    0, 0, 0, "position"),
            kf(0.0,    0, 0, 0, "rotation"),
            kf(0.45,   0, 270 * (1 if i % 2 == 0 else -1), 0, "rotation"),
            kf(LEN,    0, 720 * (1 if i % 2 == 0 else -1), 0, "rotation"),
        ]}

    # Orbital parents — rapidly expand & snap
    for h_idx, puid in enumerate(orbital_parent_bids):
        animators[puid] = {"name": bone_name_map[puid], "type": "bone",
                           "keyframes": [
            kf(0.0,    1, 1, 1, "scale"),
            kf(0.15,   1.4, 1.4, 1.4, "scale"),
            kf(0.30,   1.8, 1.8, 1.8, "scale"),
            kf(0.45,   2.2, 2.2, 2.2, "scale"),
            kf(0.60,   0.5, 0.5, 0.5, "scale"),
            kf(LEN,    0, 0, 0, "scale"),
            kf(0.0,    0, 0, 0, "position"),
            kf(LEN/2,  0, 0, 0, "position"),
            kf(LEN,    0, 0, 0, "position"),
            kf(0.0,    0, 0, 0, "rotation"),
            kf(LEN/2,  0, 90, 0, "rotation"),
            kf(LEN,    0, 180, 0, "rotation"),
        ]}

    # Orbital slabs
    for h_idx, slabs in enumerate(orbital_path_bids):
        for s_idx, suid in enumerate(slabs):
            animators[suid] = {"name": bone_name_map[suid], "type": "bone",
                               "keyframes": [
                kf(0.0,    1, 1, 1, "scale"),
                kf(0.20,   0.9, 0.9, 0.9, "scale"),
                kf(0.40,   0.6, 0.6, 0.6, "scale"),
                kf(0.60,   0.3, 0.3, 0.3, "scale"),
                kf(LEN,    0, 0, 0, "scale"),
                kf(0.0,    0, 0, 0, "position"),
                kf(LEN/2,  0, 0, 0, "position"),
                kf(LEN,    0, 0, 0, "position"),
                kf(0.0,    0, 0, 0, "rotation"),
                kf(LEN/2,  0, 0, 0, "rotation"),
                kf(LEN,    0, 0, 0, "rotation"),
            ]}

    return animators


# ── Build animations ────────────────────────────────────────────
spawn_anim = {
    "uuid": aid(1), "name": "animation.devils_halo_array.spawn",
    "loop": "once", "length": 1.3, "snapping": 24,
    "selected": False, "saved": False, "animators": build_spawn()
}
idle_anim = {
    "uuid": aid(2), "name": "animation.devils_halo_array.idle",
    "loop": "loop", "length": 7.0, "snapping": 24,
    "selected": False, "saved": False, "animators": build_idle()
}
dissipate_anim = {
    "uuid": aid(3), "name": "animation.devils_halo_array.dissipate",
    "loop": "once", "length": 0.9, "snapping": 24,
    "selected": False, "saved": False, "animators": build_dissipate()
}

# Outliner: top-level halo parents and orbital parents only.
# Each parent's children list already references the slabs/cracks.
# Center cluster bones are top-level.
outliner = []
outliner.extend(halo_parent_bids)
outliner.extend(orbital_parent_bids)
outliner.extend(center_cluster_bids)

textures = [
    {"id": "0", "name": "devils_halo_array_tex",
     "relative_path": "../textures/devils_halo_array_tex.png",
     "folder": "devilsdream", "namespace": "", "visible": True, "mode": "bitmap",
     "saved": False, "uuid": tid(1), "source": f"data:image/png;base64,{b64_0}"},
    {"id": "1", "name": "devils_halo_array_tex1",
     "relative_path": "../textures/devils_halo_array_tex1.png",
     "folder": "devilsdream", "namespace": "", "visible": True, "mode": "bitmap",
     "saved": False, "uuid": tid(2), "source": f"data:image/png;base64,{b64_1}"},
]

model = {
    "meta": {"format_version": "4.10", "model_format": "free", "box_uv": False},
    "name": "devils_halo_array",
    "geometry": "geometry.devils_halo_array",
    "resolution": {"width": 64, "height": 64},
    "elements": elements,
    "outliner": outliner,
    "textures": textures,
    "animations": [spawn_anim, idle_anim, dissipate_anim]
}

out_path = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/devils_halo_array.bbmodel"
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
