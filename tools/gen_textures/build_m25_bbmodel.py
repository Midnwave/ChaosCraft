"""
Build the_dream_itself.bbmodel — THE FINAL ULTIMATE Devil's Dream model.
- Central nightmare core: 4 ring planes (horizontal, vertical NS, vertical EW, diagonal) x 12 slabs each = 48 slab elements at Y=12
- 2 Gothic arches flanking sphere (X=-7, X=+7): 8 pillar segments + 6 arch slabs + 3 stained glass + keystone each = 36 elements
- 2 Wings behind arches: 9 feathers per side, multi-segment = ~60 elements
- 16 Orbital fragments: bone shards + mirror fragments + star-cross shapes
- Ground manifestation: 24 ring slabs + 5 crossing lines + 12 spike elements = 41 elements
- 5 Crown halos at varying heights (Y=16,18,19.5,21,22), 8 slabs each = 40 elements
TOTAL: 240+ elements, 100+ bones
"""
import json, math, os

b64_0 = open("D:/CC/ChaosCraft/tools/gen_textures/m25_b64.txt").read().strip()
b64_1 = open("D:/CC/ChaosCraft/tools/gen_textures/m25_tex1_b64.txt").read().strip()

def eid(n): return f"e{n:04d}"
def bid(n): return f"b{n:04d}"
def aid(n): return f"a{n:03d}"
def kid(n): return f"k{n:05d}"
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
ei = [1]
bi = [1]

def add_bone(name, origin, child_eids, rot=None):
    b = bone(bi[0], name, origin, child_eids, rot=rot)
    bones.append(b)
    uid = bid(bi[0])
    bi[0] += 1
    return uid

def add_elem(name, fr, to, uv):
    e = elem(ei[0], name, fr, to, uv)
    elements.append(e)
    uid = eid(ei[0])
    ei[0] += 1
    return uid

# Track bones by category for animation
sphere_bids = []        # 48 sphere slabs
arch_left_bids = []     # 18 arch left
arch_right_bids = []    # 18 arch right
arch_glass_left_bids = []  # 3 stained glass left
arch_glass_right_bids = [] # 3 stained glass right
wing_left_bids = []     # ~30 wing left
wing_right_bids = []    # ~30 wing right
orbital_bids = []       # 16 orbital fragments
ground_ring_bids = []   # 24 ground rings
ground_line_bids = []   # 5 crossing lines
spike_bids = []         # 12 spike elements
halo_bids = [[] for _ in range(5)]  # 5 halos x 8 = 40

# ============================================================================
# 1. CENTRAL NIGHTMARE SPHERE — 4 ring planes (12 slabs each) at Y=12
# ============================================================================
SPHERE_Y = 12.0
SPHERE_R = 3.0

# Ring 1: horizontal ring at Y=12 (XZ plane, 12 slabs around)
for i in range(12):
    ang = i * 30.0
    rad = math.radians(ang)
    cx = SPHERE_R * math.cos(rad)
    cz = SPHERE_R * math.sin(rad)
    sw, sh, sd = 0.45, 0.40, 0.20
    fr = [cx - sw/2, SPHERE_Y - sh/2, cz - sd/2]
    to = [cx + sw/2, SPHERE_Y + sh/2, cz + sd/2]
    e = add_elem(f"sphere_h_{i+1}", fr, to, UV0)
    b = add_bone(f"sphere_h_{i+1}", [cx, SPHERE_Y, cz], [e], rot=[0, -ang, 0])
    sphere_bids.append(b)

# Ring 2: vertical NS ring (XY plane, 12 slabs around) — at z=0
for i in range(12):
    ang = i * 30.0
    rad = math.radians(ang)
    cx = SPHERE_R * math.cos(rad)
    cy = SPHERE_Y + SPHERE_R * math.sin(rad)
    sw, sh, sd = 0.40, 0.20, 0.45
    fr = [cx - sw/2, cy - sh/2, -sd/2]
    to = [cx + sw/2, cy + sh/2, sd/2]
    e = add_elem(f"sphere_ns_{i+1}", fr, to, UV0)
    b = add_bone(f"sphere_ns_{i+1}", [cx, cy, 0], [e], rot=[0, 0, -ang])
    sphere_bids.append(b)

# Ring 3: vertical EW ring (YZ plane, 12 slabs) — at x=0
for i in range(12):
    ang = i * 30.0
    rad = math.radians(ang)
    cz = SPHERE_R * math.cos(rad)
    cy = SPHERE_Y + SPHERE_R * math.sin(rad)
    sw, sh, sd = 0.45, 0.20, 0.40
    fr = [-sw/2, cy - sh/2, cz - sd/2]
    to = [sw/2, cy + sh/2, cz + sd/2]
    e = add_elem(f"sphere_ew_{i+1}", fr, to, UV0)
    b = add_bone(f"sphere_ew_{i+1}", [0, cy, cz], [e], rot=[-ang, 0, 0])
    sphere_bids.append(b)

# Ring 4: diagonal ring (45° tilt around Y, then 12 slabs)
for i in range(12):
    ang = i * 30.0
    rad = math.radians(ang)
    # Sphere coord on the tilted ring (rotated about Y by 45°)
    raw_x = SPHERE_R * math.cos(rad)
    raw_z = SPHERE_R * math.sin(rad)
    # tilt 30° around Z so this ring is offset
    tilt = math.radians(30)
    cx = raw_x * math.cos(tilt) - 0
    cy_off = raw_x * math.sin(tilt)
    cz = raw_z
    cy = SPHERE_Y + cy_off
    sw, sh, sd = 0.40, 0.30, 0.30
    fr = [cx - sw/2, cy - sh/2, cz - sd/2]
    to = [cx + sw/2, cy + sh/2, cz + sd/2]
    e = add_elem(f"sphere_d_{i+1}", fr, to, UV0)
    b = add_bone(f"sphere_d_{i+1}", [cx, cy, cz], [e], rot=[0, -ang, 30])
    sphere_bids.append(b)

# Sphere parent organisational bones (purely structural — group rings)
sphere_h_group = add_bone("sphere_h_ring", [0, SPHERE_Y, 0], sphere_bids[0:12])
sphere_ns_group = add_bone("sphere_ns_ring", [0, SPHERE_Y, 0], sphere_bids[12:24])
sphere_ew_group = add_bone("sphere_ew_ring", [0, SPHERE_Y, 0], sphere_bids[24:36])
sphere_d_group = add_bone("sphere_d_ring", [0, SPHERE_Y, 0], sphere_bids[36:48])

# ============================================================================
# 2. GOTHIC ARCHES — flanking left (X=-7) and right (X=+7)
# ============================================================================
def build_arch(name_prefix, base_x, mirror=False):
    """Build a gothic arch: 8 pillar segments + 6 arch slabs + 3 stained glass + 1 keystone = 18 elements/bones."""
    bids = []
    glass_bids = []
    # 2 pillars × 4 segments each = 8 pillar segments
    pillar_height = 10.0
    seg_h = pillar_height / 4
    pillar_w = 0.8
    pillar_d = 1.0
    for side in [-1, 1]:
        for s in range(4):
            y_low = s * seg_h
            y_high = y_low + seg_h
            cx_pil = base_x + side * 1.6
            fr = [cx_pil - pillar_w/2, y_low, -pillar_d/2]
            to = [cx_pil + pillar_w/2, y_high, pillar_d/2]
            e = add_elem(f"{name_prefix}_pil_{side}_{s+1}", fr, to, UV0)
            b = add_bone(f"{name_prefix}_pil_{side}_{s+1}", [cx_pil, (y_low+y_high)/2, 0], [e])
            bids.append(b)
    # 6 arch slabs forming the curved top (semicircle from x-1.6 → x+1.6 at height 10..14)
    arch_top_y = 14.0
    arch_base_y = 10.0
    for i in range(6):
        ang = 180.0 - (i+1) * (180.0/7)  # 6 segments span the half-arc
        rad = math.radians(ang)
        ax = base_x + 1.6 * math.cos(rad)
        ay = arch_base_y + 4.0 * math.sin(rad)
        sw, sh, sd = 0.6, 0.6, 0.9
        fr = [ax - sw/2, ay - sh/2, -sd/2]
        to = [ax + sw/2, ay + sh/2, sd/2]
        e = add_elem(f"{name_prefix}_arch_{i+1}", fr, to, UV0)
        b = add_bone(f"{name_prefix}_arch_{i+1}", [ax, ay, 0], [e], rot=[0, 0, -(ang-90)])
        bids.append(b)
    # 3 stained glass — interior of arch — separate so animations can target them
    for i in range(3):
        gy = 6.0 + i * 1.5
        sw, sh, sd = 2.4, 1.2, 0.15
        fr = [base_x - sw/2, gy - sh/2, -sd/2]
        to = [base_x + sw/2, gy + sh/2, sd/2]
        e = add_elem(f"{name_prefix}_glass_{i+1}", fr, to, UV1)
        b = add_bone(f"{name_prefix}_glass_{i+1}", [base_x, gy, 0], [e])
        glass_bids.append(b)
    # Keystone
    fr = [base_x - 0.5, arch_top_y - 0.4, -0.5]
    to = [base_x + 0.5, arch_top_y + 0.4, 0.5]
    e = add_elem(f"{name_prefix}_keystone", fr, to, UV0)
    b = add_bone(f"{name_prefix}_keystone", [base_x, arch_top_y, 0], [e])
    bids.append(b)
    return bids, glass_bids

arch_left_bids, arch_glass_left_bids = build_arch("arch_L", -7)
arch_right_bids, arch_glass_right_bids = build_arch("arch_R", 7)

# Group bones for arches
arch_left_group = add_bone("arch_left", [-7, 7, 0], arch_left_bids + arch_glass_left_bids)
arch_right_group = add_bone("arch_right", [7, 7, 0], arch_right_bids + arch_glass_right_bids)

# ============================================================================
# 3. WINGS — bone-white feather geometry behind arches
# ============================================================================
def build_wing(name_prefix, root_x, side):
    """Build a wing — 3 primary feathers (largest, outer) + 2 secondary + 1 covert layer.
    Each feather has multiple segments (rachis + barbs).
    side=-1 for left, +1 for right (flips fan direction).
    """
    bids = []
    wing_root_y = 10.0
    # 3 primary feathers (outermost, largest)
    primary_angles = [60, 75, 90]  # angles from horizontal (degrees)
    primary_lengths = [9.0, 10.5, 9.5]
    for i, (ang, length) in enumerate(zip(primary_angles, primary_lengths)):
        rad = math.radians(ang)
        # base of feather
        bx = root_x + side * 1.5
        by = wing_root_y
        # 3 segments per feather (rachis)
        seg_count = 3
        for s in range(seg_count):
            t0 = s / seg_count
            t1 = (s+1) / seg_count
            x0 = bx + side * length * math.cos(rad) * t0
            y0 = by + length * math.sin(rad) * t0
            x1 = bx + side * length * math.cos(rad) * t1
            y1 = by + length * math.sin(rad) * t1
            sw = 0.5 - s * 0.08
            sh_ = 0.4 - s * 0.05
            sd = 0.25
            cx = (x0 + x1) / 2
            cy = (y0 + y1) / 2
            fr = [cx - sw/2, cy - sh_/2, -sd/2]
            to = [cx + sw/2, cy + sh_/2, sd/2]
            e = add_elem(f"{name_prefix}_pri{i+1}_s{s+1}", fr, to, UV0)
            seg_ang_z = -side * (90 - ang)
            b = add_bone(f"{name_prefix}_pri{i+1}_s{s+1}", [cx, cy, 0], [e], rot=[0, 0, seg_ang_z])
            bids.append(b)
        # 2 barbs per primary (small slabs perpendicular)
        for s in range(2):
            tt = (s+1) / 3
            bx_b = bx + side * length * math.cos(rad) * tt
            by_b = by + length * math.sin(rad) * tt
            barb_len = 1.4 - s * 0.3
            bbx = bx_b + side * barb_len * math.cos(rad + math.pi/2)
            bby = by_b + barb_len * math.sin(rad + math.pi/2)
            cx = (bx_b + bbx)/2
            cy = (by_b + bby)/2
            sw = 0.7
            sh_ = 0.18
            sd = 0.18
            fr = [cx - sw/2, cy - sh_/2, -sd/2]
            to = [cx + sw/2, cy + sh_/2, sd/2]
            e = add_elem(f"{name_prefix}_pri{i+1}_barb{s+1}", fr, to, UV0)
            barb_ang_z = -side * (90 - ang) - side * 90
            b = add_bone(f"{name_prefix}_pri{i+1}_barb{s+1}", [cx, cy, 0], [e], rot=[0, 0, barb_ang_z])
            bids.append(b)

    # 2 secondary feathers (inner, shorter)
    secondary_angles = [40, 105]
    secondary_lengths = [6.5, 7.0]
    for i, (ang, length) in enumerate(zip(secondary_angles, secondary_lengths)):
        rad = math.radians(ang)
        bx = root_x + side * 1.0
        by = wing_root_y - 0.5
        for s in range(3):
            t0 = s / 3
            t1 = (s+1) / 3
            x0 = bx + side * length * math.cos(rad) * t0
            y0 = by + length * math.sin(rad) * t0
            x1 = bx + side * length * math.cos(rad) * t1
            y1 = by + length * math.sin(rad) * t1
            sw = 0.4 - s * 0.06
            sh_ = 0.32 - s * 0.04
            sd = 0.22
            cx = (x0 + x1)/2; cy = (y0 + y1)/2
            fr = [cx - sw/2, cy - sh_/2, -sd/2]
            to = [cx + sw/2, cy + sh_/2, sd/2]
            e = add_elem(f"{name_prefix}_sec{i+1}_s{s+1}", fr, to, UV0)
            b = add_bone(f"{name_prefix}_sec{i+1}_s{s+1}", [cx, cy, 0], [e], rot=[0, 0, -side*(90-ang)])
            bids.append(b)
        # 1 barb each
        bx_b = bx + side * length * math.cos(rad) * 0.5
        by_b = by + length * math.sin(rad) * 0.5
        bbx = bx_b + side * 0.8 * math.cos(rad + math.pi/2)
        bby = by_b + 0.8 * math.sin(rad + math.pi/2)
        cx = (bx_b + bbx)/2; cy = (by_b + bby)/2
        fr = [cx - 0.4, cy - 0.10, -0.10]
        to = [cx + 0.4, cy + 0.10, 0.10]
        e = add_elem(f"{name_prefix}_sec{i+1}_barb", fr, to, UV0)
        b = add_bone(f"{name_prefix}_sec{i+1}_barb", [cx, cy, 0], [e], rot=[0, 0, -side*(90-ang) - side*90])
        bids.append(b)

    # 1 covert layer — fanned cluster of small feathers near wing root
    covert_angles = [30, 50, 70, 90, 110, 130]
    for i, ang in enumerate(covert_angles):
        rad = math.radians(ang)
        length = 3.0
        bx = root_x + side * 0.8
        by = wing_root_y - 1.2
        cx = bx + side * length * math.cos(rad) * 0.5
        cy = by + length * math.sin(rad) * 0.5
        sw, sh_, sd = 0.6, 0.30, 0.18
        fr = [cx - sw/2, cy - sh_/2, -sd/2]
        to = [cx + sw/2, cy + sh_/2, sd/2]
        e = add_elem(f"{name_prefix}_cov_{i+1}", fr, to, UV0)
        b = add_bone(f"{name_prefix}_cov_{i+1}", [cx, cy, 0], [e], rot=[0, 0, -side*(90-ang)])
        bids.append(b)
    return bids

wing_left_bids = build_wing("wing_L", -10, -1)
wing_right_bids = build_wing("wing_R", 10, 1)

wing_left_group = add_bone("wing_left", [-10, 10, 0], wing_left_bids)
wing_right_group = add_bone("wing_right", [10, 10, 0], wing_right_bids)

# ============================================================================
# 4. ORBITAL FRAGMENTS — 16 mixed pieces orbiting central sphere
# ============================================================================
# 6 bone shard cubes, 5 mirror fragments, 5 star-cross shapes
orbital_data = []  # (name, type, radius, ang, y_off)
# 6 bone shards
for i in range(6):
    ang = i * 60.0 + 15
    radius = 4.5 + (i % 3) * 0.8
    y_off = SPHERE_Y + (i % 4 - 1.5) * 1.5
    orbital_data.append((f"orb_bone_{i+1}", "bone", radius, ang, y_off))
# 5 mirror fragments
for i in range(5):
    ang = i * 72.0 + 36
    radius = 5.5 + (i % 2) * 1.0
    y_off = SPHERE_Y + (i - 2) * 1.6
    orbital_data.append((f"orb_mir_{i+1}", "mirror", radius, ang, y_off))
# 5 star-cross shapes (each = 3 small slabs forming cross — but represented by 3 elements per cross)
star_centers = []
for i in range(5):
    ang = i * 72.0 + 54
    radius = 6.5 + (i % 3) * 0.6
    y_off = SPHERE_Y + (i - 2) * 1.4
    star_centers.append((f"orb_star_{i+1}", radius, ang, y_off))

for name, type_, radius, ang, y_off in orbital_data:
    rad = math.radians(ang)
    cx = radius * math.cos(rad)
    cz = radius * math.sin(rad)
    if type_ == "bone":
        # bone shard — elongated cube
        sw, sh_, sd = 0.45, 0.65, 0.20
        fr = [cx - sw/2, y_off - sh_/2, cz - sd/2]
        to = [cx + sw/2, y_off + sh_/2, cz + sd/2]
        e = add_elem(name, fr, to, UV0)
    else:
        # mirror fragment — flat irregular slab
        sw, sh_, sd = 0.55, 0.55, 0.10
        fr = [cx - sw/2, y_off - sh_/2, cz - sd/2]
        to = [cx + sw/2, y_off + sh_/2, cz + sd/2]
        e = add_elem(name, fr, to, UV1)
    b = add_bone(name, [cx, y_off, cz], [e], rot=[0, -ang, 15])
    orbital_bids.append(b)

# Star-crosses: each is 3 small slabs forming a + cross
star_bids_grouped = []
for sname, radius, ang, y_off in star_centers:
    rad = math.radians(ang)
    cx = radius * math.cos(rad)
    cz = radius * math.sin(rad)
    # 3 cross slabs
    cross_eids = []
    # horizontal X
    fr = [cx - 0.4, y_off - 0.06, cz - 0.06]; to = [cx + 0.4, y_off + 0.06, cz + 0.06]
    cross_eids.append(add_elem(f"{sname}_h", fr, to, UV1))
    # vertical Y
    fr = [cx - 0.06, y_off - 0.4, cz - 0.06]; to = [cx + 0.06, y_off + 0.4, cz + 0.06]
    cross_eids.append(add_elem(f"{sname}_v", fr, to, UV1))
    # depth Z
    fr = [cx - 0.06, y_off - 0.06, cz - 0.4]; to = [cx + 0.06, y_off + 0.06, cz + 0.4]
    cross_eids.append(add_elem(f"{sname}_d", fr, to, UV1))
    b = add_bone(sname, [cx, y_off, cz], cross_eids, rot=[0, -ang, 0])
    orbital_bids.append(b)

orbital_group = add_bone("orbital_fragments", [0, SPHERE_Y, 0], orbital_bids)

# ============================================================================
# 5. GROUND PATTERN — 3 concentric rings + 5 crossing lines + 6 spike clusters
# ============================================================================
# 3 concentric rings at radii 6, 9, 12 — 8 slabs each = 24 slabs
ring_radii = [6.0, 9.0, 12.0]
for ri, radius in enumerate(ring_radii):
    for i in range(8):
        ang = i * 45.0
        rad = math.radians(ang)
        cx = radius * math.cos(rad)
        cz = radius * math.sin(rad)
        # tangential slab
        sw, sh_, sd = 1.4, 0.30, 0.40
        fr = [cx - sw/2, -sh_/2, cz - sd/2]
        to = [cx + sw/2, sh_/2, cz + sd/2]
        e = add_elem(f"ring{ri+1}_{i+1}", fr, to, UV1)
        b = add_bone(f"ring{ri+1}_{i+1}", [cx, 0, cz], [e], rot=[0, -ang, 0])
        ground_ring_bids.append(b)

# 5 crossing line slabs (long flat radials at 0°, 72°, 144°, 216°, 288°)
for i in range(5):
    ang = i * 72.0
    rad = math.radians(ang)
    sw, sh_, sd = 13.0, 0.20, 0.50
    cx = 0; cz = 0
    fr = [-sw/2, -sh_/2, -sd/2]
    to = [sw/2, sh_/2, sd/2]
    e = add_elem(f"line_{i+1}", fr, to, UV1)
    b = add_bone(f"line_{i+1}", [0, 0, 0], [e], rot=[0, -ang, 0])
    ground_line_bids.append(b)

# 6 rupture spike clusters at outer perimeter radius 13 (each = 2 spike cubes)
for i in range(6):
    ang = i * 60.0 + 30
    rad = math.radians(ang)
    cx = 13.0 * math.cos(rad)
    cz = 13.0 * math.sin(rad)
    # spike 1 (taller)
    sw1, sh1, sd1 = 0.55, 2.5, 0.55
    fr = [cx - sw1/2, 0, cz - sd1/2]
    to = [cx + sw1/2, sh1, cz + sd1/2]
    e1 = add_elem(f"spike_{i+1}_a", fr, to, UV1)
    b1 = add_bone(f"spike_{i+1}_a", [cx, 0, cz], [e1], rot=[0, -ang, 0])
    spike_bids.append(b1)
    # spike 2 (shorter, offset)
    sw2, sh2, sd2 = 0.45, 1.6, 0.45
    cx2 = cx + 0.6 * math.cos(rad + 0.5)
    cz2 = cz + 0.6 * math.sin(rad + 0.5)
    fr = [cx2 - sw2/2, 0, cz2 - sd2/2]
    to = [cx2 + sw2/2, sh2, cz2 + sd2/2]
    e2 = add_elem(f"spike_{i+1}_b", fr, to, UV1)
    b2 = add_bone(f"spike_{i+1}_b", [cx2, 0, cz2], [e2], rot=[0, -ang, 5])
    spike_bids.append(b2)

ground_ring_group = add_bone("ground_rings_all", [0,0,0], ground_ring_bids)
ground_line_group = add_bone("ground_lines_all", [0,0,0], ground_line_bids)
spike_group = add_bone("spikes_all", [0,0,0], spike_bids)

# ============================================================================
# 6. CROWN OF HALOS — 5 broken halo rings at varying heights
# ============================================================================
halo_heights = [16.0, 18.0, 19.5, 21.0, 22.0]
halo_radii = [3.0, 2.6, 2.4, 2.2, 2.0]
for hi, (h_y, h_r) in enumerate(zip(halo_heights, halo_radii)):
    for i in range(8):
        ang = i * 45.0 + hi * 11  # offset each halo for "broken" appearance
        rad = math.radians(ang)
        cx = h_r * math.cos(rad)
        cz = h_r * math.sin(rad)
        # tangential slab — break some by skipping or smaller
        sw, sh_, sd = 0.9, 0.18, 0.30
        # broken: every halo missing one slab (replace with smaller)
        if i == hi % 8:
            sw = 0.4; sh_ = 0.12
        fr = [cx - sw/2, h_y - sh_/2, cz - sd/2]
        to = [cx + sw/2, h_y + sh_/2, cz + sd/2]
        e = add_elem(f"halo{hi+1}_{i+1}", fr, to, UV1)
        b = add_bone(f"halo{hi+1}_{i+1}", [cx, h_y, cz], [e], rot=[0, -ang, hi*3])
        halo_bids[hi].append(b)

halo_groups = []
for hi in range(5):
    halo_groups.append(add_bone(f"halo_{hi+1}_ring", [0, halo_heights[hi], 0], halo_bids[hi]))

# ============================================================================
# COUNTS / VERIFICATION
# ============================================================================
print(f"[SPHERE] {len(sphere_bids)} bones (4 ring planes × 12)")
print(f"[ARCHES] L:{len(arch_left_bids)+len(arch_glass_left_bids)} R:{len(arch_right_bids)+len(arch_glass_right_bids)}")
print(f"[WINGS] L:{len(wing_left_bids)} R:{len(wing_right_bids)}")
print(f"[ORBITAL] {len(orbital_bids)} bones")
print(f"[GROUND] rings:{len(ground_ring_bids)} lines:{len(ground_line_bids)} spikes:{len(spike_bids)}")
print(f"[HALOS] 5 halos × 8 = {sum(len(h) for h in halo_bids)} bones")
print(f"[TOTAL] elements:{len(elements)} bones:{len(bones)}")

assert len(elements) >= 150, f"Need >=150 elements, got {len(elements)}"
assert len(bones) >= 100, f"Need >=100 bones, got {len(bones)}"

bone_name_map = {b["uuid"]: b["name"] for b in bones}

# Build outliner — top-level group bones containing children. Other bones at root too.
# Strategy: include all bones at top level for simplicity (ME doesn't require strict hierarchy).
all_individual = (
    sphere_bids + arch_left_bids + arch_glass_left_bids + arch_right_bids +
    arch_glass_right_bids + wing_left_bids + wing_right_bids +
    orbital_bids + ground_ring_bids + ground_line_bids + spike_bids
)
for hb in halo_bids:
    all_individual.extend(hb)

# Group bones (parent-only structural)
group_bones = [
    sphere_h_group, sphere_ns_group, sphere_ew_group, sphere_d_group,
    arch_left_group, arch_right_group,
    wing_left_group, wing_right_group,
    orbital_group,
    ground_ring_group, ground_line_group, spike_group,
] + halo_groups

# outliner has only root bone uuids (the groups). Children referenced by uuid.
outliner = [g for g in group_bones]

# ============================================================================
# ANIMATIONS
# ============================================================================

def build_spawn():
    """spawn 2.0s once: layered arrival sequence."""
    animators = {}

    # Ground rings expand at 0.2s
    for i, buid in enumerate(ground_ring_bids):
        delay = 0.20 + (i % 8) * 0.01
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,    0,0,0, "scale"),
            kf(delay,  0,0,0, "scale"),
            kf(delay+0.10, 1.3,0.5,1.3, "scale"),
            kf(delay+0.20, 1.0,1.0,1.0, "scale"),
            kf(0.6,    1.0,1.0,1.0, "scale"),
            kf(1.0,    1.0,1.0,1.0, "scale"),
            kf(1.5,    1.0,1.0,1.0, "scale"),
            kf(1.8,    1.05,1.05,1.05, "scale"),
            kf(2.0,    1.0,1.0,1.0, "scale"),
            kf(0.0, 0,0,0, "position"), kf(0.5, 0,0,0, "position"),
            kf(1.0, 0,0,0, "position"), kf(1.5, 0,0,0, "position"), kf(2.0, 0,0,0, "position"),
            kf(0.0, 0,0,0, "rotation"), kf(0.5, 0,0,0, "rotation"),
            kf(1.0, 0,0,0, "rotation"), kf(1.5, 0,0,0, "rotation"), kf(2.0, 0,0,0, "rotation"),
        ]}

    # Ground lines expand at 0.2s (similar)
    for i, buid in enumerate(ground_line_bids):
        delay = 0.20 + i * 0.02
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   0,0,0,"scale"),
            kf(delay, 0,0,0,"scale"),
            kf(delay+0.12, 1.0,1.2,1.0,"scale"),
            kf(delay+0.20, 1.0,1.0,1.0,"scale"),
            kf(0.8, 1.0,1.0,1.0,"scale"),
            kf(1.4, 1.0,1.0,1.0,"scale"),
            kf(1.8, 1.05,1.0,1.05,"scale"),
            kf(2.0, 1.0,1.0,1.0,"scale"),
            kf(0.0, 0,0,0, "position"), kf(0.7, 0,0,0, "position"),
            kf(1.4, 0,0,0, "position"), kf(2.0, 0,0,0, "position"),
            kf(0.0, 0,0,0, "rotation"), kf(0.7, 0,0,0, "rotation"),
            kf(1.4, 0,0,0, "rotation"), kf(2.0, 0,0,0, "rotation"),
        ]}

    # Rupture spikes erupt at 0.3s
    for i, buid in enumerate(spike_bids):
        delay = 0.30 + (i // 2) * 0.04
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,        1,0,1,"scale"),
            kf(delay,      1,0,1,"scale"),
            kf(delay+0.10, 1,1.2,1,"scale"),
            kf(delay+0.18, 1,1.0,1,"scale"),
            kf(0.9, 1,1.0,1,"scale"),
            kf(1.4, 1,1.0,1,"scale"),
            kf(1.8, 1.05,1.05,1.05,"scale"),
            kf(2.0, 1,1,1,"scale"),
            kf(0.0,        0,0,0,"position"),
            kf(delay,      0,0,0,"position"),
            kf(delay+0.18, 0,0,0,"position"),
            kf(1.0, 0,0,0,"position"), kf(2.0, 0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(0.5, 0,0,0,"rotation"),
            kf(1.0, 0,0,0,"rotation"), kf(2.0, 0,0,0,"rotation"),
        ]}

    # Arch pillars + arch slabs + keystone extend upward at 0.4s
    arch_combined = arch_left_bids + arch_right_bids
    for i, buid in enumerate(arch_combined):
        delay = 0.40 + (i % 18) * 0.015
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,        1,0,1,"scale"),
            kf(delay,      1,0,1,"scale"),
            kf(delay+0.15, 1,1.1,1,"scale"),
            kf(delay+0.25, 1,1.0,1,"scale"),
            kf(1.0, 1,1,1,"scale"),
            kf(1.5, 1,1,1,"scale"),
            kf(1.8, 1.04,1.04,1.04,"scale"),
            kf(2.0, 1,1,1,"scale"),
            kf(0.0, 0,0,0,"position"), kf(0.7, 0,0,0,"position"),
            kf(1.4, 0,0,0,"position"), kf(2.0, 0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(0.7, 0,0,0,"rotation"),
            kf(1.4, 0,0,0,"rotation"), kf(2.0, 0,0,0,"rotation"),
        ]}

    # Wing coverts at 0.6s, secondaries at 0.8s, primaries at 0.9s
    def wing_phase_delay(name):
        if "_cov_" in name: return 0.60
        if "_sec" in name: return 0.80
        return 0.90

    for buid in wing_left_bids + wing_right_bids:
        nm = bone_name_map[buid]
        delay = wing_phase_delay(nm) + (hash(nm) % 100) * 0.002
        animators[buid] = {"name": nm, "type":"bone","keyframes":[
            kf(0.0,        0,0,0,"scale"),
            kf(delay,      0,0,0,"scale"),
            kf(delay+0.15, 1.1,1.1,1.1,"scale"),
            kf(delay+0.25, 1.0,1.0,1.0,"scale"),
            kf(1.4, 1,1,1,"scale"),
            kf(1.8, 1.06,1.06,1.06,"scale"),
            kf(2.0, 1,1,1,"scale"),
            kf(0.0, 0,0,0,"position"), kf(0.7, 0,0,0,"position"),
            kf(1.4, 0,0,0,"position"), kf(2.0, 0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(0.7, 0,0,0,"rotation"),
            kf(1.4, 0,0,0,"rotation"), kf(2.0, 0,0,0,"rotation"),
        ]}

    # Sphere materialises flat then expands to full at 1.0s (scale XZ large, Y~0 -> 1.0)
    for i, buid in enumerate(sphere_bids):
        delay = (i * 0.005)  # tiny stagger
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   1.5,0.05,1.5,"scale"),
            kf(0.4,   1.5,0.10,1.5,"scale"),
            kf(0.7,   1.3,0.30,1.3,"scale"),
            kf(0.85,  1.1,0.7,1.1,"scale"),
            kf(1.0,   1.0,1.0,1.0,"scale"),
            kf(1.3,   1.02,1.02,1.02,"scale"),
            kf(1.5,   1.0,1.0,1.0,"scale"),
            kf(1.8,   1.15,1.15,1.15,"scale"),
            kf(2.0,   1.0,1.0,1.0,"scale"),
            kf(0.0, 0,0,0, "position"), kf(0.7, 0,0,0, "position"),
            kf(1.4, 0,0,0, "position"), kf(2.0, 0,0,0, "position"),
            kf(0.0, 0,0,0, "rotation"), kf(0.7, 0,0,0, "rotation"),
            kf(1.4, 0,0,0, "rotation"), kf(2.0, 0,0,0, "rotation"),
        ]}

    # Sphere group bones (parent — minimal animation)
    for buid in [sphere_h_group, sphere_ns_group, sphere_ew_group, sphere_d_group]:
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1, "scale"), kf(0.5, 1,1,1, "scale"),
            kf(1.0, 1,1,1, "scale"), kf(1.5, 1,1,1, "scale"),
            kf(1.8, 1.05,1.05,1.05, "scale"), kf(2.0, 1,1,1, "scale"),
            kf(0.0, 0,0,0, "position"), kf(0.7, 0,0,0, "position"),
            kf(1.4, 0,0,0, "position"), kf(2.0, 0,0,0, "position"),
            kf(0.0, 0,0,0, "rotation"), kf(0.7, 0,0,0, "rotation"),
            kf(1.4, 0,0,0, "rotation"), kf(2.0, 0,0,0, "rotation"),
        ]}

    # Stained glass phases in at 1.2s
    for buid in arch_glass_left_bids + arch_glass_right_bids:
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 0,0,0, "scale"),
            kf(1.2, 0,0,0, "scale"),
            kf(1.35, 1.1,1.1,1.1, "scale"),
            kf(1.45, 1.0,1.0,1.0, "scale"),
            kf(1.6, 1.0,1.0,1.0, "scale"),
            kf(1.8, 1.04,1.04,1.04, "scale"),
            kf(2.0, 1,1,1, "scale"),
            kf(0.0, 0,0,0, "position"), kf(0.7, 0,0,0, "position"),
            kf(1.4, 0,0,0, "position"), kf(2.0, 0,0,0, "position"),
            kf(0.0, 0,0,0, "rotation"), kf(0.7, 0,0,0, "rotation"),
            kf(1.4, 0,0,0, "rotation"), kf(2.0, 0,0,0, "rotation"),
        ]}

    # Orbital fragments fly in from outer space at 1.2-1.6s (start far away, position 0 by 1.6)
    for i, buid in enumerate(orbital_bids):
        delay = 1.2 + (i / max(1,len(orbital_bids))) * 0.4
        # name inherits an angle — use index to spread directions
        ang = (i * 360.0 / max(1,len(orbital_bids)))
        rad = math.radians(ang)
        far_x = 8.0 * math.cos(rad)
        far_z = 8.0 * math.sin(rad)
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,    0,0,0,"scale"),
            kf(delay,  0,0,0,"scale"),
            kf(delay+0.10, 1.2,1.2,1.2,"scale"),
            kf(delay+0.20, 1.0,1.0,1.0,"scale"),
            kf(1.8, 1.05,1.05,1.05,"scale"),
            kf(2.0, 1,1,1,"scale"),
            kf(0.0,         far_x, 4, far_z, "position"),
            kf(delay,       far_x, 4, far_z, "position"),
            kf(delay+0.20,  0,0,0, "position"),
            kf(1.8, 0,0,0,"position"), kf(2.0, 0,0,0,"position"),
            kf(0.0,    0, ang, 0, "rotation"),
            kf(delay,  0, ang, 0, "rotation"),
            kf(delay+0.20, 0,0,0,"rotation"),
            kf(2.0, 0,0,0,"rotation"),
        ]}

    # Crown halos descend from above at 1.5s
    for hi in range(5):
        for i, buid in enumerate(halo_bids[hi]):
            delay = 1.50 + (hi * 0.04) + (i * 0.005)
            animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
                kf(0.0,    0,0,0,"scale"),
                kf(delay,  0,0,0,"scale"),
                kf(delay+0.10, 1.2,1.2,1.2,"scale"),
                kf(delay+0.20, 1.0,1.0,1.0,"scale"),
                kf(1.8, 1.08,1.08,1.08,"scale"),
                kf(2.0, 1,1,1,"scale"),
                kf(0.0,        0, 6, 0, "position"),
                kf(delay,      0, 6, 0, "position"),
                kf(delay+0.20, 0, 0, 0, "position"),
                kf(2.0, 0,0,0, "position"),
                kf(0.0, 0,0,0,"rotation"), kf(0.5, 0,0,0,"rotation"),
                kf(1.0, 0,0,0,"rotation"), kf(2.0, 0,0,0,"rotation"),
            ]}

    # Halo group bones
    for hg in halo_groups:
        animators[hg] = {"name": bone_name_map[hg], "type":"bone","keyframes":[
            kf(0.0, 1,1,1, "scale"), kf(0.5, 1,1,1, "scale"),
            kf(1.0, 1,1,1, "scale"), kf(1.5, 1,1,1, "scale"),
            kf(1.8, 1.05,1.05,1.05, "scale"), kf(2.0, 1,1,1, "scale"),
            kf(0.0, 0,0,0, "position"), kf(0.7, 0,0,0, "position"),
            kf(1.4, 0,0,0, "position"), kf(2.0, 0,0,0, "position"),
            kf(0.0, 0,0,0, "rotation"), kf(0.7, 0,0,0, "rotation"),
            kf(1.4, 0,0,0, "rotation"), kf(2.0, 0,0,0, "rotation"),
        ]}

    # Group structural bones (arches, wings, orbital, ground)
    for bg in [arch_left_group, arch_right_group, wing_left_group, wing_right_group,
               orbital_group, ground_ring_group, ground_line_group, spike_group]:
        animators[bg] = {"name": bone_name_map[bg], "type":"bone","keyframes":[
            kf(0.0, 1,1,1, "scale"), kf(0.5, 1,1,1, "scale"),
            kf(1.0, 1,1,1, "scale"), kf(1.5, 1,1,1, "scale"),
            kf(1.8, 1.04,1.04,1.04, "scale"), kf(2.0, 1,1,1, "scale"),
            kf(0.0, 0,0,0, "position"), kf(0.7, 0,0,0, "position"),
            kf(1.4, 0,0,0, "position"), kf(2.0, 0,0,0, "position"),
            kf(0.0, 0,0,0, "rotation"), kf(0.7, 0,0,0, "rotation"),
            kf(1.4, 0,0,0, "rotation"), kf(2.0, 0,0,0, "rotation"),
        ]}
    return animators


def build_idle():
    """idle 10.0s loop: nothing perfectly synchronises."""
    animators = {}
    L = 10.0

    # Sphere ring rotations on 3 axes — different speeds (5s, 7s, 10s)
    # sphere_bids[0:12] = h ring, [12:24] = ns, [24:36] = ew, [36:48] = d
    for i, buid in enumerate(sphere_bids):
        # determine ring type
        ring = i // 12
        idx = i % 12
        base_ang = idx * 30.0
        # Each ring rotates around its own axis
        if ring == 0:
            # horizontal ring rotates Y axis. Period 5s -> full 720° in 10s
            rot_kf = [
                ("rotation", 0.0, 0, -base_ang, 0),
                ("rotation", 2.5, 0, -base_ang + 180, 0),
                ("rotation", 5.0, 0, -base_ang + 360, 0),
                ("rotation", 7.5, 0, -base_ang + 540, 0),
                ("rotation", 10.0, 0, -base_ang + 720, 0),
            ]
        elif ring == 1:
            # NS ring rotates Z axis. Period 7s
            rot_kf = [
                ("rotation", 0.0, 0, 0, -base_ang),
                ("rotation", 1.75, 90, 0, -base_ang),
                ("rotation", 3.5, 180, 0, -base_ang),
                ("rotation", 5.25, 270, 0, -base_ang),
                ("rotation", 7.0, 360, 0, -base_ang),
                ("rotation", 8.5, 437, 0, -base_ang),
                ("rotation", 10.0, 514, 0, -base_ang),
            ]
        elif ring == 2:
            # EW ring rotates X axis. Period 10s
            rot_kf = [
                ("rotation", 0.0, -base_ang, 0, 0),
                ("rotation", 2.5, -base_ang + 90, 0, 0),
                ("rotation", 5.0, -base_ang + 180, 0, 0),
                ("rotation", 7.5, -base_ang + 270, 0, 0),
                ("rotation", 10.0, -base_ang + 360, 0, 0),
            ]
        else:
            # diagonal ring — Y axis with offset, period 6s
            rot_kf = [
                ("rotation", 0.0, 0, -base_ang, 30),
                ("rotation", 1.5, 0, -base_ang + 90, 30),
                ("rotation", 3.0, 0, -base_ang + 180, 30),
                ("rotation", 4.5, 0, -base_ang + 270, 30),
                ("rotation", 6.0, 0, -base_ang + 360, 30),
                ("rotation", 8.0, 0, -base_ang + 480, 30),
                ("rotation", 10.0, 0, -base_ang + 600, 30),
            ]
        kfs = []
        for ch, t, x, y, z in rot_kf:
            kfs.append(kf(t, x, y, z, ch))
        # add scale + position
        for t in [0.0, 2.0, 4.0, 6.0, 8.0, 10.0]:
            kfs.append(kf(t, 1.0+0.02*math.sin(t+i*0.2), 1.0+0.02*math.cos(t+i*0.2), 1.0, "scale"))
            kfs.append(kf(t, 0.05*math.sin(t+i*0.3), 0.05*math.cos(t+i*0.3), 0, "position"))
        animators[buid] = {"name": bone_name_map[buid], "type":"bone", "keyframes": kfs}

    # Sphere group bones — minimal idle anims
    for buid in [sphere_h_group, sphere_ns_group, sphere_ew_group, sphere_d_group]:
        animators[buid] = {"name": bone_name_map[buid], "type":"bone", "keyframes":[
            kf(0.0, 1,1,1,"scale"), kf(2.0, 1.01,1.01,1.01,"scale"),
            kf(5.0, 1,1,1,"scale"), kf(7.0, 1.01,1.01,1.01,"scale"),
            kf(10.0, 1,1,1,"scale"),
            kf(0.0, 0,0,0,"position"), kf(2.0, 0,0,0,"position"),
            kf(5.0, 0,0,0,"position"), kf(7.0, 0,0,0,"position"), kf(10.0, 0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(2.0, 0,0,0,"rotation"),
            kf(5.0, 0,0,0,"rotation"), kf(7.0, 0,0,0,"rotation"), kf(10.0, 0,0,0,"rotation"),
        ]}

    # Wings breathe (all feathers oscillate together) ±4° + individual flutter ±1.5°
    for side, wing_bids in [(-1, wing_left_bids), (1, wing_right_bids)]:
        for i, buid in enumerate(wing_bids):
            phase = i * 0.3
            kfs = []
            for tt in [0.0, 1.25, 2.5, 3.75, 5.0, 6.25, 7.5, 8.75, 10.0]:
                # global breathe ±4° in Z, individual flutter ±1.5°
                breathe = 4.0 * math.sin(tt * 2 * math.pi / 5.0) * (-side)
                flutter = 1.5 * math.sin(tt * 2 * math.pi / 1.7 + phase)
                kfs.append(kf(tt, 0, 0, breathe + flutter, "rotation"))
            for tt in [0.0, 2.5, 5.0, 7.5, 10.0]:
                kfs.append(kf(tt, 1.0+0.02*math.sin(tt+phase), 1.0, 1.0, "scale"))
                kfs.append(kf(tt, 0.04*math.sin(tt+phase), 0.04*math.cos(tt+phase), 0, "position"))
            animators[buid] = {"name": bone_name_map[buid], "type":"bone", "keyframes": kfs}

    # Wing group bones — slow breathe
    for wg in [wing_left_group, wing_right_group]:
        animators[wg] = {"name": bone_name_map[wg], "type":"bone", "keyframes":[
            kf(0.0, 1,1,1, "scale"), kf(2.5, 1.02,1.02,1.02,"scale"),
            kf(5.0, 1,1,1, "scale"), kf(7.5, 1.02,1.02,1.02,"scale"),
            kf(10.0, 1,1,1, "scale"),
            kf(0.0, 0,0,0,"position"), kf(2.5, 0,0.05,0,"position"),
            kf(5.0, 0,0,0,"position"), kf(7.5, 0,-0.05,0,"position"), kf(10.0, 0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(2.5, 0,0,0,"rotation"),
            kf(5.0, 0,0,0,"rotation"), kf(7.5, 0,0,0,"rotation"), kf(10.0, 0,0,0,"rotation"),
        ]}

    # Arches hold still (stained glass animated separately)
    for buid in arch_left_bids + arch_right_bids:
        animators[buid] = {"name": bone_name_map[buid], "type":"bone", "keyframes":[
            kf(0.0, 1,1,1,"scale"), kf(2.5, 1.005,1.005,1.005,"scale"),
            kf(5.0, 1,1,1,"scale"), kf(7.5, 1.005,1.005,1.005,"scale"),
            kf(10.0, 1,1,1,"scale"),
            kf(0.0, 0,0,0,"position"), kf(2.5, 0,0,0,"position"),
            kf(5.0, 0,0,0,"position"), kf(7.5, 0,0,0,"position"), kf(10.0, 0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(2.5, 0,0,0,"rotation"),
            kf(5.0, 0,0,0,"rotation"), kf(7.5, 0,0,0,"rotation"), kf(10.0, 0,0,0,"rotation"),
        ]}

    # Stained glass ripples
    for i, buid in enumerate(arch_glass_left_bids + arch_glass_right_bids):
        phase = i * 0.4
        kfs = []
        for tt in [0.0, 1.25, 2.5, 3.75, 5.0, 6.25, 7.5, 8.75, 10.0]:
            ripple = 0.05 * math.sin(tt * 2 * math.pi / 3.0 + phase)
            kfs.append(kf(tt, 1.0 + ripple, 1.0 + ripple*0.5, 1.0, "scale"))
        for tt in [0.0, 2.5, 5.0, 7.5, 10.0]:
            kfs.append(kf(tt, 0, 0.04*math.sin(tt+phase), 0, "position"))
            kfs.append(kf(tt, 0,0,0,"rotation"))
        animators[buid] = {"name": bone_name_map[buid], "type":"bone", "keyframes": kfs}

    # Arch group bones
    for ag in [arch_left_group, arch_right_group]:
        animators[ag] = {"name": bone_name_map[ag], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"), kf(2.5, 1,1,1,"scale"),
            kf(5.0, 1,1,1,"scale"), kf(7.5, 1,1,1,"scale"), kf(10.0, 1,1,1,"scale"),
            kf(0.0, 0,0,0,"position"), kf(2.5, 0,0,0,"position"),
            kf(5.0, 0,0,0,"position"), kf(7.5, 0,0,0,"position"), kf(10.0, 0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(2.5, 0,0,0,"rotation"),
            kf(5.0, 0,0,0,"rotation"), kf(7.5, 0,0,0,"rotation"), kf(10.0, 0,0,0,"rotation"),
        ]}

    # Orbital fragments — each unique orbit speed (3s-9s periods)
    for i, buid in enumerate(orbital_bids):
        period = 3.0 + (i % 7)  # 3s..9s
        loops = L / period
        kfs = []
        # Y rotation cycles
        for j in range(9):
            tt = j * (L / 8)
            ang_deg = (tt / period) * 360.0
            base_ang = (i * 30.0) % 360
            kfs.append(kf(tt, 0, base_ang + ang_deg, 0, "rotation"))
        for tt in [0.0, 2.0, 4.0, 6.0, 8.0, 10.0]:
            wob = 0.15 * math.sin(tt + i * 0.4)
            kfs.append(kf(tt, wob, wob*0.5, wob*0.7, "position"))
            kfs.append(kf(tt, 1.0 + 0.04*math.sin(tt+i), 1.0 + 0.04*math.cos(tt+i), 1.0, "scale"))
        animators[buid] = {"name": bone_name_map[buid], "type":"bone", "keyframes": kfs}

    # Orbital group
    animators[orbital_group] = {"name": bone_name_map[orbital_group], "type":"bone","keyframes":[
        kf(0.0, 0, 0, 0, "rotation"),
        kf(2.5, 0, 30, 0, "rotation"),
        kf(5.0, 0, 60, 0, "rotation"),
        kf(7.5, 0, 90, 0, "rotation"),
        kf(10.0, 0, 120, 0, "rotation"),
        kf(0.0, 0,0,0,"position"), kf(5.0, 0,0,0,"position"), kf(10.0, 0,0,0,"position"),
        kf(0.0, 1,1,1,"scale"), kf(5.0, 1,1,1,"scale"), kf(10.0, 1,1,1,"scale"),
    ]}

    # Crown halos rotate at different rates: halo 0=4s, 1=5s, 2=6s, 3=7s, 4=8s
    halo_periods = [4.0, 5.0, 6.0, 7.0, 8.0]
    for hi in range(5):
        period = halo_periods[hi]
        # Each halo's individual slabs rotate as a group via group bone, but we anim each slab too
        for i, buid in enumerate(halo_bids[hi]):
            base_ang = i * 45.0 + hi * 11
            kfs = []
            for j in range(9):
                tt = j * (L / 8)
                ang_offset = (tt / period) * 360.0 * (1 if hi % 2 == 0 else -1)
                kfs.append(kf(tt, 0, -base_ang + ang_offset, hi*3, "rotation"))
            for tt in [0.0, 2.5, 5.0, 7.5, 10.0]:
                kfs.append(kf(tt, 1.0+0.03*math.sin(tt+i+hi), 1.0, 1.0, "scale"))
                kfs.append(kf(tt, 0, 0.05*math.sin(tt+hi), 0, "position"))
            animators[buid] = {"name": bone_name_map[buid], "type":"bone", "keyframes": kfs}

    # Halo groups — slow drift
    for hi, hg in enumerate(halo_groups):
        period = halo_periods[hi]
        kfs = []
        for j in range(11):
            tt = j * 1.0
            ang_deg = (tt / period) * 360.0
            kfs.append(kf(tt, 0, ang_deg * 0.1, 0, "rotation"))
        for tt in [0.0, 2.5, 5.0, 7.5, 10.0]:
            kfs.append(kf(tt, 0, 0.1*math.sin(tt + hi), 0, "position"))
            kfs.append(kf(tt, 1, 1, 1, "scale"))
        animators[hg] = {"name": bone_name_map[hg], "type":"bone", "keyframes": kfs}

    # Ground rings rotate at 3 different speeds — outer slowest
    # ring1 (r=6) period 6s, ring2 (r=9) period 8s, ring3 (r=12) period 10s
    ring_periods = [6.0, 8.0, 10.0]
    for ri in range(3):
        period = ring_periods[ri]
        for i in range(8):
            buid = ground_ring_bids[ri*8 + i]
            base_ang = i * 45.0
            kfs = []
            for j in range(9):
                tt = j * (L / 8)
                ang_offset = (tt / period) * 360.0 * (1 if ri % 2 == 0 else -1)
                kfs.append(kf(tt, 0, -base_ang + ang_offset, 0, "rotation"))
            for tt in [0.0, 2.5, 5.0, 7.5, 10.0]:
                kfs.append(kf(tt, 1.0 + 0.02*math.sin(tt+i+ri), 1.0, 1.0, "scale"))
                kfs.append(kf(tt, 0, 0, 0, "position"))
            animators[buid] = {"name": bone_name_map[buid], "type":"bone", "keyframes": kfs}

    # Ground lines — pulse
    for i, buid in enumerate(ground_line_bids):
        phase = i * 0.5
        kfs = []
        for tt in [0.0, 1.25, 2.5, 3.75, 5.0, 6.25, 7.5, 8.75, 10.0]:
            puls = 0.06 * math.sin(tt * 2 * math.pi / 4.0 + phase)
            kfs.append(kf(tt, 1.0 + puls, 1.0, 1.0, "scale"))
        for tt in [0.0, 2.5, 5.0, 7.5, 10.0]:
            kfs.append(kf(tt, 0, 0, 0, "position"))
            kfs.append(kf(tt, 0, -i*72, 0, "rotation"))
        animators[buid] = {"name": bone_name_map[buid], "type":"bone", "keyframes": kfs}

    # Spikes — slow shimmer
    for i, buid in enumerate(spike_bids):
        phase = i * 0.3
        kfs = []
        for tt in [0.0, 1.25, 2.5, 3.75, 5.0, 6.25, 7.5, 8.75, 10.0]:
            puls = 0.04 * math.sin(tt * 2 * math.pi / 3.5 + phase)
            kfs.append(kf(tt, 1.0, 1.0 + puls, 1.0, "scale"))
        for tt in [0.0, 2.5, 5.0, 7.5, 10.0]:
            kfs.append(kf(tt, 0, 0, 0, "position"))
            kfs.append(kf(tt, 0, 0, 0, "rotation"))
        animators[buid] = {"name": bone_name_map[buid], "type":"bone", "keyframes": kfs}

    # Group bones for ground/spike — minimal anims
    for bg in [ground_ring_group, ground_line_group, spike_group]:
        animators[bg] = {"name": bone_name_map[bg], "type":"bone","keyframes":[
            kf(0.0, 1,1,1, "scale"), kf(2.5, 1,1,1,"scale"),
            kf(5.0, 1,1,1, "scale"), kf(7.5, 1,1,1,"scale"), kf(10.0, 1,1,1,"scale"),
            kf(0.0, 0,0,0,"position"), kf(2.5, 0,0,0,"position"),
            kf(5.0, 0,0,0,"position"), kf(7.5, 0,0,0,"position"), kf(10.0, 0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(2.5, 0,0,0,"rotation"),
            kf(5.0, 0,0,0,"rotation"), kf(7.5, 0,0,0,"rotation"), kf(10.0, 0,0,0,"rotation"),
        ]}
    return animators


def build_dissipate():
    """dissipate 1.8s once: layered exit."""
    animators = {}

    # Crown halos drift upward 0-0.4s and dissolve
    for hi in range(5):
        for i, buid in enumerate(halo_bids[hi]):
            animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
                kf(0.0,  1,1,1,"scale"),
                kf(0.2,  1.1,1.1,1.1,"scale"),
                kf(0.4,  0.6,0.6,0.6,"scale"),
                kf(0.6,  0,0,0,"scale"),
                kf(1.0,  0,0,0,"scale"),
                kf(1.4,  0,0,0,"scale"),
                kf(1.8,  0,0,0,"scale"),
                kf(0.0,  0,0,0,"position"),
                kf(0.2,  0,1.5,0,"position"),
                kf(0.4,  0,3.5,0,"position"),
                kf(0.6,  0,5.0,0,"position"),
                kf(1.0,  0,6.5,0,"position"),
                kf(1.8,  0,8,0,"position"),
                kf(0.0, 0,0,0,"rotation"), kf(0.6, 0, hi*30, 0,"rotation"),
                kf(1.2, 0, hi*60, 0,"rotation"), kf(1.8, 0, hi*90, 0,"rotation"),
            ]}

    for hg in halo_groups:
        animators[hg] = {"name": bone_name_map[hg], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"), kf(0.4, 1,1,1,"scale"),
            kf(0.9, 1,1,1,"scale"), kf(1.4, 1,1,1,"scale"), kf(1.8, 1,1,1,"scale"),
            kf(0.0, 0,0,0,"position"), kf(0.4, 0,0,0,"position"),
            kf(0.9, 0,0,0,"position"), kf(1.4, 0,0,0,"position"), kf(1.8, 0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(0.4, 0,0,0,"rotation"),
            kf(0.9, 0,0,0,"rotation"), kf(1.4, 0,0,0,"rotation"), kf(1.8, 0,0,0,"rotation"),
        ]}

    # Orbital fragments scatter outward 0-0.6s
    for i, buid in enumerate(orbital_bids):
        ang = (i * 360.0 / max(1,len(orbital_bids)))
        rad = math.radians(ang)
        far_x = 12.0 * math.cos(rad)
        far_z = 12.0 * math.sin(rad)
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.2,  1.2,1.2,1.2,"scale"),
            kf(0.4,  0.6,0.6,0.6,"scale"),
            kf(0.6,  0,0,0,"scale"),
            kf(1.0,  0,0,0,"scale"),
            kf(1.8,  0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.2,  far_x*0.3, 1, far_z*0.3, "position"),
            kf(0.4,  far_x*0.7, 2, far_z*0.7, "position"),
            kf(0.6,  far_x, 3, far_z, "position"),
            kf(1.8,  far_x*1.2, 4, far_z*1.2, "position"),
            kf(0.0, 0,0,0,"rotation"),
            kf(0.3, ang*1.5, ang*2, 0,"rotation"),
            kf(0.6, ang*3, ang*4, 0,"rotation"),
            kf(1.8, ang*5, ang*6, 0,"rotation"),
        ]}

    animators[orbital_group] = {"name": bone_name_map[orbital_group], "type":"bone","keyframes":[
        kf(0.0, 1,1,1,"scale"), kf(0.6, 1,1,1,"scale"),
        kf(1.2, 1,1,1,"scale"), kf(1.8, 1,1,1,"scale"),
        kf(0.0, 0,0,0,"position"), kf(0.6, 0,0,0,"position"),
        kf(1.2, 0,0,0,"position"), kf(1.8, 0,0,0,"position"),
        kf(0.0, 0,0,0,"rotation"), kf(0.6, 0,0,0,"rotation"),
        kf(1.2, 0,0,0,"rotation"), kf(1.8, 0,0,0,"rotation"),
    ]}

    # Wing feathers fold 0.4-1.0s and dissolve
    for side, wing_bids in [(-1, wing_left_bids), (1, wing_right_bids)]:
        for i, buid in enumerate(wing_bids):
            animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
                kf(0.0,  1,1,1,"scale"),
                kf(0.4,  1,1,1,"scale"),
                kf(0.7,  0.8,0.8,0.8,"scale"),
                kf(1.0,  0.4,0.4,0.4,"scale"),
                kf(1.3,  0,0,0,"scale"),
                kf(1.8,  0,0,0,"scale"),
                kf(0.0, 0,0,0,"position"),
                kf(0.4, 0,0,0,"position"),
                kf(0.7, side*0.5, -0.5, 0,"position"),
                kf(1.0, side*1.0, -1.0, 0,"position"),
                kf(1.8, side*1.5, -2.0, 0,"position"),
                kf(0.0, 0,0,0,"rotation"),
                kf(0.4, 0,0,0,"rotation"),
                kf(0.7, 0, 0, side*30,"rotation"),
                kf(1.0, 0, 0, side*60,"rotation"),
                kf(1.8, 0, 0, side*90,"rotation"),
            ]}

    for wg in [wing_left_group, wing_right_group]:
        animators[wg] = {"name": bone_name_map[wg], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"), kf(0.6, 1,1,1,"scale"),
            kf(1.2, 1,1,1,"scale"), kf(1.8, 1,1,1,"scale"),
            kf(0.0, 0,0,0,"position"), kf(0.6, 0,0,0,"position"),
            kf(1.2, 0,0,0,"position"), kf(1.8, 0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(0.6, 0,0,0,"rotation"),
            kf(1.2, 0,0,0,"rotation"), kf(1.8, 0,0,0,"rotation"),
        ]}

    # Arch stained glass shatter 0.6-1.0s
    for i, buid in enumerate(arch_glass_left_bids + arch_glass_right_bids):
        is_left = i < len(arch_glass_left_bids)
        sd = -1 if is_left else 1
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.6,  1,1,1,"scale"),
            kf(0.7,  1.2,1.2,0.5,"scale"),
            kf(0.85, 0.6,0.6,0.2,"scale"),
            kf(1.0,  0,0,0,"scale"),
            kf(1.8,  0,0,0,"scale"),
            kf(0.0, 0,0,0,"position"),
            kf(0.6, 0,0,0,"position"),
            kf(0.8, sd*0.5, 0, 0.5, "position"),
            kf(1.0, sd*1.5, -0.5, 1.0, "position"),
            kf(1.8, sd*2.5, -1.5, 2.0, "position"),
            kf(0.0, 0,0,0,"rotation"),
            kf(0.7, 0, 0, sd*15,"rotation"),
            kf(1.0, sd*45, sd*30, sd*60,"rotation"),
            kf(1.8, sd*90, sd*60, sd*120,"rotation"),
        ]}

    # Other arch parts — stay solid until end
    for buid in arch_left_bids + arch_right_bids:
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.6,  1,1,1,"scale"),
            kf(1.0,  1,1,1,"scale"),
            kf(1.4,  0.7,0.7,0.7,"scale"),
            kf(1.8,  0,0,0,"scale"),
            kf(0.0, 0,0,0,"position"), kf(0.6, 0,0,0,"position"),
            kf(1.0, 0,0,0,"position"), kf(1.4, 0, -1.0, 0, "position"),
            kf(1.8, 0, -3.0, 0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(0.6, 0,0,0,"rotation"),
            kf(1.0, 0,0,0,"rotation"), kf(1.4, 0,0,0,"rotation"),
            kf(1.8, 0,0,0,"rotation"),
        ]}

    for ag in [arch_left_group, arch_right_group]:
        animators[ag] = {"name": bone_name_map[ag], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"), kf(0.6, 1,1,1,"scale"),
            kf(1.2, 1,1,1,"scale"), kf(1.8, 1,1,1,"scale"),
            kf(0.0, 0,0,0,"position"), kf(0.6, 0,0,0,"position"),
            kf(1.2, 0,0,0,"position"), kf(1.8, 0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(0.6, 0,0,0,"rotation"),
            kf(1.2, 0,0,0,"rotation"), kf(1.8, 0,0,0,"rotation"),
        ]}

    # Sphere rings fly apart on their axes 0.8-1.4s
    for i, buid in enumerate(sphere_bids):
        ring = i // 12
        idx = i % 12
        ang = idx * 30.0
        rad = math.radians(ang)
        # Direction depends on ring
        if ring == 0:
            dx, dy, dz = 4*math.cos(rad), 0, 4*math.sin(rad)
        elif ring == 1:
            dx, dy, dz = 4*math.cos(rad), 4*math.sin(rad), 0
        elif ring == 2:
            dx, dy, dz = 0, 4*math.sin(rad), 4*math.cos(rad)
        else:
            dx, dy, dz = 4*math.cos(rad)*0.7, 4*math.cos(rad)*0.7, 4*math.sin(rad)
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.6,  1,1,1,"scale"),
            kf(0.8,  1.3,1.3,1.3,"scale"),
            kf(1.1,  0.8,0.8,0.8,"scale"),
            kf(1.4,  0.3,0.3,0.3,"scale"),
            kf(1.8,  0,0,0,"scale"),
            kf(0.0, 0,0,0,"position"),
            kf(0.8, 0,0,0,"position"),
            kf(1.1, dx*0.5, dy*0.5, dz*0.5, "position"),
            kf(1.4, dx*1.0, dy*1.0, dz*1.0, "position"),
            kf(1.8, dx*1.6, dy*1.6, dz*1.6, "position"),
            kf(0.0, 0,0,0,"rotation"),
            kf(0.8, 0,0,0,"rotation"),
            kf(1.1, ang*0.5, 0, ang*0.3,"rotation"),
            kf(1.4, ang, 0, ang*0.6,"rotation"),
            kf(1.8, ang*1.5, 0, ang,"rotation"),
        ]}

    for buid in [sphere_h_group, sphere_ns_group, sphere_ew_group, sphere_d_group]:
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"), kf(0.6, 1,1,1,"scale"),
            kf(1.2, 1,1,1,"scale"), kf(1.8, 1,1,1,"scale"),
            kf(0.0, 0,0,0,"position"), kf(0.6, 0,0,0,"position"),
            kf(1.2, 0,0,0,"position"), kf(1.8, 0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(0.6, 0,0,0,"rotation"),
            kf(1.2, 0,0,0,"rotation"), kf(1.8, 0,0,0,"rotation"),
        ]}

    # Ground rings rapidly expand and snap 1.2-1.6s
    for i, buid in enumerate(ground_ring_bids):
        ri = i // 8
        idx = i % 8
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(1.0,  1,1,1,"scale"),
            kf(1.2,  1.5,1.5,1.5,"scale"),
            kf(1.4,  2.5,0.3,2.5,"scale"),
            kf(1.6,  3.5,0,3.5,"scale"),
            kf(1.8,  0,0,0,"scale"),
            kf(0.0, 0,0,0,"position"), kf(1.0, 0,0,0,"position"),
            kf(1.4, 0,0,0,"position"), kf(1.8, 0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"),
            kf(1.0, 0,0,0,"rotation"),
            kf(1.4, 0, idx*15, 0,"rotation"),
            kf(1.8, 0, idx*30, 0,"rotation"),
        ]}

    # Ground lines rapid stretch and snap
    for i, buid in enumerate(ground_line_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(1.0,  1,1,1,"scale"),
            kf(1.2,  1.5,1,1,"scale"),
            kf(1.4,  2.5,0.3,1,"scale"),
            kf(1.6,  3.0,0,1,"scale"),
            kf(1.8,  0,0,0,"scale"),
            kf(0.0, 0,0,0,"position"), kf(1.0, 0,0,0,"position"),
            kf(1.8, 0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(1.0, 0,0,0,"rotation"),
            kf(1.8, 0,0,0,"rotation"),
        ]}

    # Spikes sink 1.4-1.8s
    for i, buid in enumerate(spike_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(1.2,  1,1,1,"scale"),
            kf(1.4,  1,0.7,1,"scale"),
            kf(1.6,  1,0.3,1,"scale"),
            kf(1.8,  0,0,0,"scale"),
            kf(0.0, 0,0,0,"position"),
            kf(1.2, 0,0,0,"position"),
            kf(1.4, 0,-1.0,0,"position"),
            kf(1.6, 0,-2.5,0,"position"),
            kf(1.8, 0,-4.0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(1.0, 0,0,0,"rotation"),
            kf(1.4, 0,0,0,"rotation"), kf(1.8, 0,0,0,"rotation"),
        ]}

    for bg in [ground_ring_group, ground_line_group, spike_group]:
        animators[bg] = {"name": bone_name_map[bg], "type":"bone","keyframes":[
            kf(0.0, 1,1,1, "scale"), kf(0.6, 1,1,1,"scale"),
            kf(1.2, 1,1,1, "scale"), kf(1.8, 1,1,1,"scale"),
            kf(0.0, 0,0,0,"position"), kf(0.6, 0,0,0,"position"),
            kf(1.2, 0,0,0,"position"), kf(1.8, 0,0,0,"position"),
            kf(0.0, 0,0,0,"rotation"), kf(0.6, 0,0,0,"rotation"),
            kf(1.2, 0,0,0,"rotation"), kf(1.8, 0,0,0,"rotation"),
        ]}

    return animators


# Build animations
spawn_anim = {
    "uuid": aid(1), "name": "animation.the_dream_itself.spawn",
    "loop": "once", "length": 2.0, "snapping": 24,
    "selected": False, "saved": False, "animators": build_spawn()
}
idle_anim = {
    "uuid": aid(2), "name": "animation.the_dream_itself.idle",
    "loop": "loop", "length": 10.0, "snapping": 24,
    "selected": False, "saved": False, "animators": build_idle()
}
dissipate_anim = {
    "uuid": aid(3), "name": "animation.the_dream_itself.dissipate",
    "loop": "once", "length": 1.8, "snapping": 24,
    "selected": False, "saved": False, "animators": build_dissipate()
}

textures = [
    {"id":"0","name":"the_dream_itself_tex","relative_path":"../textures/the_dream_itself_tex.png",
     "folder":"devilsdream","namespace":"","visible":True,"mode":"bitmap","saved":False,
     "uuid":tid(1),"source":f"data:image/png;base64,{b64_0}"},
    {"id":"1","name":"the_dream_itself_tex1","relative_path":"../textures/the_dream_itself_tex1.png",
     "folder":"devilsdream","namespace":"","visible":True,"mode":"bitmap","saved":False,
     "uuid":tid(2),"source":f"data:image/png;base64,{b64_1}"},
]

model = {
    "meta": {"format_version":"4.10","model_format":"free","box_uv":False},
    "name": "the_dream_itself",
    "geometry": "geometry.the_dream_itself",
    "resolution": {"width":64,"height":64},
    "elements": elements,
    "outliner": outliner,
    "textures": textures,
    "animations": [spawn_anim, idle_anim, dissipate_anim]
}

out_path = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/the_dream_itself.bbmodel"
with open(out_path, "w", encoding="utf-8") as f:
    json.dump(model, f, separators=(',', ':'))

size = os.path.getsize(out_path)
print(f"Written: {out_path}")
print(f"Elements: {len(elements)} / Bones: {len(bones)}")
print(f"Animations: 3 (spawn 2.0s once, idle 10.0s loop, dissipate 1.8s once)")
print(f"Size: {size:,} bytes ({size/1024:.1f} KB)")
pass_fail = "PASS" if size >= 600*1024 else "FAIL"
print(f"{pass_fail}: {'>=600KB' if size>=600*1024 else f'only {size//1024}KB'}")

# Footprint check
all_x = []; all_z = []
for e in elements:
    all_x.extend([e["from"][0], e["to"][0]])
    all_z.extend([e["from"][2], e["to"][2]])
print(f"Footprint X: [{min(all_x):.1f}, {max(all_x):.1f}], Z: [{min(all_z):.1f}, {max(all_z):.1f}]")
