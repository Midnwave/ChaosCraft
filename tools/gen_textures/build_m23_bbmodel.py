"""
Build nightmare_planetarium.bbmodel
Central infernal star + 4 wrong planets orbiting + orbital rings + paths + 2 moons + comet.

Layout:
  Central STAR sphere (8 slabs forming approximate sphere) at Y=8 origin.
  4 PLANETS at radii 4/6/8/10 and heights Y=6/8/9/11 (3 cubes each = small spheres).
    - planet1 = blood-red (UV row 0-16)
    - planet2 = bone-white (UV row 16-32)
    - planet3 = sickly yellow (UV row 32-48)
    - planet4 = void purple (UV row 48-64)
  4 ORBITAL RINGS — 8 slabs each in horizontal circle at planet's orbit height/radius.
  2 MOONS — small cubes attached to 2 of the planets.
  4 ORBITAL PATH RINGS — 6 slabs each, faint, tex1.
  COMET — 3-cube tapered chain on highly elliptical orbit (tilted ±30°).
  STAR has 8 ray spikes radiating outward.
  Each planet gets 2 surface accent details.
  Each orbital ring gets 4 evenly-spaced bright marker accents.
  Comet trail has 3 tail sparkle pieces.
"""
import json, math, os

b64_0 = open("D:/CC/ChaosCraft/tools/gen_textures/m23_b64.txt").read().strip()
b64_1 = open("D:/CC/ChaosCraft/tools/gen_textures/m23_tex1_b64.txt").read().strip()

def eid(n): return f"e{n:03d}"
def bid(n): return f"b{n:03d}"
def aid(n): return f"a{n:03d}"
def kid(n): return f"k{n:04d}"
def tid(n): return f"t{n:03d}"

# Standard UV (whole face from texture 0)
UV0 = {"north":{"uv":[2,2,30,30],"texture":0},"east":{"uv":[34,2,62,30],"texture":0},
       "south":{"uv":[2,2,30,30],"texture":0},"west":{"uv":[34,2,62,30],"texture":0},
       "up":{"uv":[2,34,30,62],"texture":0},"down":{"uv":[34,34,62,62],"texture":0}}

# UV1 — emissive zone of texture 1
UV1 = {"north":{"uv":[34,34,62,62],"texture":1},"east":{"uv":[34,34,62,62],"texture":1},
       "south":{"uv":[34,34,62,62],"texture":1},"west":{"uv":[34,34,62,62],"texture":1},
       "up":{"uv":[34,34,62,62],"texture":1},"down":{"uv":[34,34,62,62],"texture":1}}

# Per-planet UV — sample specific zone of tex0 corresponding to that planet's colour.
# Texture 0 zones: rows 0-16 blood, 16-32 bone, 32-48 toxic yellow, 48-64 void purple.
# We pick a 14x14 area within each zone.
def uv_zone(zone):
    """zone 0=blood, 1=bone, 2=toxic, 3=void"""
    y_starts = [1, 17, 33, 49]
    ys = y_starts[zone]
    ye = ys + 14
    # 28x28 window for n/s/e/w faces
    side = {"north":{"uv":[2,ys,30,ye+14],"texture":0},
            "east": {"uv":[34,ys,62,ye+14],"texture":0},
            "south":{"uv":[2,ys,30,ye+14],"texture":0},
            "west": {"uv":[34,ys,62,ye+14],"texture":0},
            "up":   {"uv":[2,ys,30,ye+14],"texture":0},
            "down": {"uv":[34,ys,62,ye+14],"texture":0}}
    # Clamp to 64
    for face in side.values():
        if face["uv"][3] > 64: face["uv"][3] = 64
        if face["uv"][1] >= 64: face["uv"][1] = 63
    return side

UV_BLOOD = uv_zone(0)
UV_BONE  = uv_zone(1)
UV_TOX   = uv_zone(2)
UV_VOID  = uv_zone(3)
PLANET_UVS = [UV_BLOOD, UV_BONE, UV_TOX, UV_VOID]

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

# ── 1. CENTRAL STAR (8 slabs forming sphere approximation) ─────────────────────
# Each slab is a flat disc on a different rotational plane, at Y=8 origin.
# 4 planes × 2 slabs each (paired perpendicular).
star_bids = []
star_planes = [
    # (rot_x, rot_y, rot_z), name suffix
    (0, 0, 0,    "core_xy"),
    (0, 90, 0,   "core_yz"),
    (90, 0, 0,   "core_xz"),
    (45, 45, 0,  "core_d1"),
    (-45, 45, 0, "core_d2"),
    (45, 0, 45,  "core_d3"),
    (0, 45, 45,  "core_d4"),
    (30, 60, 30, "core_d5"),
]
star_origin_y = 8.0
for (rx, ry, rz, suf) in star_planes:
    # flat slab thicker than wide on Z axis (a "disc")
    fr = [-1.4, -1.4, -0.18]
    to = [1.4, 1.4, 0.18]
    e = elem(ei, f"star_{suf}", fr, to, UV0); elements.append(e)
    b = bone(bi, f"star_{suf}", [0, star_origin_y, 0], [eid(ei)], rot=[rx, ry, rz]); bones.append(b)
    star_bids.append(bid(bi)); ei+=1; bi+=1

# Star CORE — single tight cube with toxic-yellow zone (bright center)
e = elem(ei, "star_nucleus", [-0.5, star_origin_y-0.5, -0.5], [0.5, star_origin_y+0.5, 0.5], UV_TOX)
elements.append(e)
b = bone(bi, "star_nucleus", [0, star_origin_y, 0], [eid(ei)]); bones.append(b)
star_nucleus_bid = bid(bi); ei+=1; bi+=1

# ── 2. STAR RAY SPIKES (8 small pointed cubes) ─────────────────────────────────
# 8 spikes radiating outward in horizontal/vertical/diagonal directions
ray_bids = []
ray_dirs = [
    ( 1.8, 0, 0),
    (-1.8, 0, 0),
    ( 0, 1.8, 0),
    ( 0,-1.8, 0),
    ( 0, 0, 1.8),
    ( 0, 0,-1.8),
    ( 1.3, 1.3, 0),
    (-1.3,-1.3, 0),
]
for i, (dx, dy, dz) in enumerate(ray_dirs):
    cx = dx; cy = star_origin_y + dy; cz = dz
    sw = 0.20
    fr = [cx - sw, cy - sw, cz - sw]
    to = [cx + sw, cy + sw, cz + sw]
    e = elem(ei, f"star_ray_{i+1}", fr, to, UV_TOX); elements.append(e)
    b = bone(bi, f"star_ray_{i+1}", [cx, cy, cz], [eid(ei)]); bones.append(b)
    ray_bids.append(bid(bi)); ei+=1; bi+=1

# ── 3. PLANETS (4 planets × 3 cubes each = 12 elements) ────────────────────────
# Each planet at distinct radius and height, made of 3 overlapping cubes.
# We will offset each planet in X by its starting orbit phase so they are visible.
# Their orbital ring bones (separate) will rotate them.
planet_specs = [
    # (radius, height, zone_idx, body_name, init_angle_deg)
    (4.0,  6.0, 0, "planet_red",    0),
    (6.0,  8.0, 1, "planet_bone",  90),
    (8.0,  9.0, 2, "planet_yellow",180),
    (10.0,11.0, 3, "planet_void",  270),
]

planet_body_bids = []  # 4 planet body bone groups (parented under their orbit ring bones)
planet_orbit_bids = [] # 4 orbit ring bones (the ones that ROTATE around Y to move planets)
planet_centers = []    # initial XZ centres (used for orbital ring positioning)

# We design planet body bones so they orbit because their ORBIT BONE rotates
# (We'll use a separate "planet_orbit_<i>" bone whose origin is (0, height, 0) and contains the planet body.)
for pi_idx, (r, h, zone, pname, init_ang) in enumerate(planet_specs):
    # Orbit bone (origin at vertical axis, will rotate around Y)
    porb_bid = bid(bi)
    # planet body offset along +X by r
    body_cx = r
    body_cy = h
    body_cz = 0
    # 3 overlapping cubes making sphere
    sphere_offsets = [
        (-0.6, 0, 0, 1.2, 0.9, 0.9),  # central wide
        (0, -0.5, 0, 0.9, 1.2, 0.9),  # taller bulge
        (0, 0, -0.5, 0.9, 0.9, 1.2),  # deeper bulge
    ]
    sub_eids = []
    for si, (ox, oy, oz, sx, sy, sz) in enumerate(sphere_offsets):
        fr = [body_cx + ox - sx/2, body_cy + oy - sy/2, body_cz + oz - sz/2]
        to = [body_cx + ox + sx/2, body_cy + oy + sy/2, body_cz + oz + sz/2]
        e = elem(ei, f"{pname}_s{si+1}", fr, to, PLANET_UVS[zone]); elements.append(e)
        sub_eids.append(eid(ei)); ei += 1

    # Body bone — origin at planet centre. Holds the 3 cubes.
    body_bid = bid(bi)
    b = bone(bi, f"{pname}_body", [body_cx, body_cy, body_cz], sub_eids); bones.append(b)
    bi += 1

    # Orbit bone — origin at (0, h, 0) with init rotation angle. Contains body bone.
    porb_bid = bid(bi)
    b = bone(bi, f"{pname}_orbit", [0, h, 0], [body_bid], rot=[0, init_ang, 0]); bones.append(b)
    bi += 1

    planet_body_bids.append(body_bid)
    planet_orbit_bids.append(porb_bid)
    planet_centers.append((body_cx, body_cy, body_cz, r, h, init_ang))

# ── 4. PLANET SURFACE ACCENT DETAILS (4 planets × 2 accents = 8 cubes) ─────────
# Small cubes on each planet (children of body bone)
accent_bids = []
for pi_idx, (r, h, zone, pname, init_ang) in enumerate(planet_specs):
    body_bid = planet_body_bids[pi_idx]
    # find body bone, append accent eids to its children
    body_bone_obj = next(b for b in bones if b["uuid"] == body_bid)
    body_cx = r
    body_cy = h
    body_cz = 0
    accents = [
        (0.45, 0.45, 0.0,  0.30, 0.30, 0.30),   # NE bulge
        (-0.40, -0.30, 0.40, 0.25, 0.25, 0.25), # SW bulge
    ]
    for ai, (ox, oy, oz, sx, sy, sz) in enumerate(accents):
        fr = [body_cx + ox - sx/2, body_cy + oy - sy/2, body_cz + oz - sz/2]
        to = [body_cx + ox + sx/2, body_cy + oy + sy/2, body_cz + oz + sz/2]
        e = elem(ei, f"{pname}_acc{ai+1}", fr, to, PLANET_UVS[zone]); elements.append(e)
        # Make this its own bone too, child of orbit bone (so it follows planet)
        ax_b = bone(bi, f"{pname}_acc{ai+1}", [body_cx + ox, body_cy + oy, body_cz + oz], [eid(ei)])
        bones.append(ax_b)
        accent_bids.append(bid(bi))
        # add to body bone's children
        body_bone_obj["children"].append(bid(bi))
        ei += 1; bi += 1

# ── 5. ORBITAL RINGS (4 rings × 8 slabs each = 32 elements) ────────────────────
# Each ring bone at its own height; 8 slabs in circular arrangement at radius r.
orbit_ring_bids = []  # the parent rotating bone for each ring
orbit_ring_slab_bids = []
for pi_idx, (r, h, zone, pname, init_ang) in enumerate(planet_specs):
    # Parent ring bone at (0, h, 0) — rotates around Y for animation
    ring_slab_eids = []
    ring_slab_local_bids = []
    for si in range(8):
        ang = si * 45.0  # local angle
        rad = math.radians(ang)
        cx = r * math.cos(rad)
        cz = r * math.sin(rad)
        sw, sh, sd = 0.55, 0.12, 0.18
        fr = [cx - sw/2, h - sh/2, cz - sd/2]
        to = [cx + sw/2, h + sh/2, cz + sd/2]
        e = elem(ei, f"ring{pi_idx+1}_slab{si+1}", fr, to, UV1); elements.append(e)
        # each slab its own bone (child of ring parent)
        sl_b = bone(bi, f"ring{pi_idx+1}_slab{si+1}", [cx, h, cz], [eid(ei)], rot=[0, -ang, 0])
        bones.append(sl_b)
        ring_slab_local_bids.append(bid(bi))
        orbit_ring_slab_bids.append(bid(bi))
        ei += 1; bi += 1

    # Parent ring bone (rotates around Y for animation; contains 8 slab bones)
    ring_b = bone(bi, f"ring{pi_idx+1}_orbit", [0, h, 0], list(ring_slab_local_bids), rot=[0, 0, 0])
    bones.append(ring_b)
    orbit_ring_bids.append(bid(bi))
    bi += 1

# ── 6. ORBITAL RING MARKER ACCENTS (4 rings × 4 markers = 16 elements) ─────────
# Bright slabs at 4 evenly-spaced positions on each ring (offset 22.5° for visibility)
marker_bids = []
for pi_idx, (r, h, zone, pname, init_ang) in enumerate(planet_specs):
    parent_ring_bid = orbit_ring_bids[pi_idx]
    parent_ring_obj = next(b for b in bones if b["uuid"] == parent_ring_bid)
    for mi in range(4):
        ang = mi * 90.0 + 22.5
        rad = math.radians(ang)
        cx = r * math.cos(rad)
        cz = r * math.sin(rad)
        sw, sh, sd = 0.30, 0.22, 0.30
        fr = [cx - sw/2, h - sh/2, cz - sd/2]
        to = [cx + sw/2, h + sh/2, cz + sd/2]
        e = elem(ei, f"ring{pi_idx+1}_mark{mi+1}", fr, to, UV1); elements.append(e)
        m_b = bone(bi, f"ring{pi_idx+1}_mark{mi+1}", [cx, h, cz], [eid(ei)])
        bones.append(m_b)
        marker_bids.append(bid(bi))
        # child of ring parent so it orbits with ring
        parent_ring_obj["children"].append(bid(bi))
        ei += 1; bi += 1

# ── 7. ORBITAL PATH RINGS (4 paths × 6 slabs = 24 elements) ────────────────────
# Faint flat rings at each orbit height — 6 slabs in horizontal circle, tex1
# Their own bones, separate from planet orbit (counter-rotate at half speed).
path_ring_bids = []
path_slab_bids = []
for pi_idx, (r, h, zone, pname, init_ang) in enumerate(planet_specs):
    path_slabs = []
    for si in range(6):
        ang = si * 60.0 + 15.0
        rad = math.radians(ang)
        cx = r * math.cos(rad)
        cz = r * math.sin(rad)
        sw, sh, sd = 0.80, 0.05, 0.10
        fr = [cx - sw/2, h - sh/2 - 0.02, cz - sd/2]
        to = [cx + sw/2, h + sh/2 - 0.02, cz + sd/2]
        e = elem(ei, f"path{pi_idx+1}_s{si+1}", fr, to, UV1); elements.append(e)
        ps_b = bone(bi, f"path{pi_idx+1}_s{si+1}", [cx, h-0.02, cz], [eid(ei)], rot=[0, -ang, 0])
        bones.append(ps_b)
        path_slabs.append(bid(bi))
        path_slab_bids.append(bid(bi))
        ei += 1; bi += 1
    # Parent path bone (counter-rotates)
    pp_b = bone(bi, f"path{pi_idx+1}_orbit", [0, h-0.02, 0], list(path_slabs), rot=[0, 0, 0])
    bones.append(pp_b)
    path_ring_bids.append(bid(bi))
    bi += 1

# ── 8. MOONS (2 moons attached to 2 planets) ───────────────────────────────────
# Moon 1 attached to planet_red (idx 0), moon 2 attached to planet_bone (idx 1)
moon_bids = []
moon_orbit_bids = []
moon_specs = [
    (0, 1.2, 0.5, "moon_red"),    # planet idx 0, moon radius 1.2 around it, height offset 0.5
    (1, 1.5, -0.4, "moon_bone"),  # planet idx 1
]
for (pi_idx, mr, h_off, mname) in moon_specs:
    body_bid = planet_body_bids[pi_idx]
    body_bone_obj = next(b for b in bones if b["uuid"] == body_bid)
    r, h, _, _, _ = planet_specs[pi_idx][:5]
    body_cx = r; body_cy = h; body_cz = 0
    # moon cube at body_cx + mr (will be rotated by moon_orbit)
    moon_offset_x = mr
    fr = [body_cx + moon_offset_x - 0.18, body_cy + h_off - 0.18, body_cz - 0.18]
    to = [body_cx + moon_offset_x + 0.18, body_cy + h_off + 0.18, body_cz + 0.18]
    e = elem(ei, mname, fr, to, UV1); elements.append(e)
    # Moon body bone (this is what gets orbited)
    m_b = bone(bi, mname, [body_cx + moon_offset_x, body_cy + h_off, body_cz], [eid(ei)])
    bones.append(m_b)
    moon_body_bid = bid(bi); ei += 1; bi += 1

    # Moon orbit bone (origin at planet body centre — rotates around Y)
    mo_b = bone(bi, f"{mname}_orbit", [body_cx, body_cy, body_cz], [moon_body_bid], rot=[0, 0, 0])
    bones.append(mo_b)
    moon_orbit_bids.append(bid(bi))
    moon_bids.append(moon_body_bid)
    # Parent under planet's body bone? No — under planet's orbit bone so it follows planet around.
    # Actually we want moon to orbit the planet's CURRENT position, so put under planet_orbit_bid.
    porb_obj = next(b for b in bones if b["uuid"] == planet_orbit_bids[pi_idx])
    porb_obj["children"].append(bid(bi))
    bi += 1

# ── 9. COMET — 3-cube tapered chain on tilted elliptical orbit ─────────────────
# We'll build a comet body bone (3 cubes) parented to a comet_orbit bone (origin at star)
# that has a tilt rotation to make orbit elliptical/tilted.
comet_eids = []
comet_radius = 12.0
# tapered cubes at offsets along +X
comet_cube_defs = [
    (12.0, 0.0, 0.0, 0.50, 0.50, 0.50, "comet_head"),    # head — biggest
    (11.2, 0.0, 0.0, 0.35, 0.35, 0.35, "comet_mid"),
    (10.5, 0.0, 0.0, 0.22, 0.22, 0.22, "comet_tail"),
]
comet_body_eids = []
for (cx, cy, cz, sx, sy, sz, cname) in comet_cube_defs:
    fr = [cx - sx/2, 8 + cy - sy/2, cz - sz/2]
    to = [cx + sx/2, 8 + cy + sy/2, cz + sz/2]
    e = elem(ei, cname, fr, to, UV_VOID); elements.append(e)
    comet_body_eids.append(eid(ei))
    ei += 1

comet_body_bid = bid(bi)
b = bone(bi, "comet_body", [11.2, 8, 0], comet_body_eids); bones.append(b)
bi += 1

# Comet orbit bone — tilted plane (±30°)
comet_orbit_bid = bid(bi)
b = bone(bi, "comet_orbit", [0, 8, 0], [comet_body_bid], rot=[30, 0, 15]); bones.append(b)
bi += 1

# ── 10. COMET TRAIL SPARKLES (3 small cubes behind comet) ──────────────────────
trail_bids = []
trail_defs = [
    (10.0, 0.0, 0.0, 0.18, "trail1"),
    ( 9.5, 0.0, 0.0, 0.14, "trail2"),
    ( 9.0, 0.0, 0.0, 0.10, "trail3"),
]
trail_eids = []
for (cx, cy, cz, sz, tname) in trail_defs:
    fr = [cx - sz/2, 8 + cy - sz/2, cz - sz/2]
    to = [cx + sz/2, 8 + cy + sz/2, cz + sz/2]
    e = elem(ei, tname, fr, to, UV1); elements.append(e)
    trail_eids.append(eid(ei))
    # each its own bone child of comet_orbit
    t_b = bone(bi, tname, [cx, 8, cz], [eid(ei)])
    bones.append(t_b)
    trail_bids.append(bid(bi))
    # parent under comet_orbit
    co_obj = next(bb for bb in bones if bb["uuid"] == comet_orbit_bid)
    co_obj["children"].append(bid(bi))
    ei += 1; bi += 1

# ── 11. EXTRA STAR DETAIL: 4 inner star halo cubes ─────────────────────────────
halo_bids = []
halo_defs = [
    (0.8, 0.0, 0.8, "halo_ne"),
    (-0.8, 0.0, 0.8, "halo_nw"),
    (0.8, 0.0, -0.8, "halo_se"),
    (-0.8, 0.0, -0.8, "halo_sw"),
]
for (dx, dy, dz, hname) in halo_defs:
    cx, cy, cz = dx, star_origin_y + dy, dz
    fr = [cx - 0.18, cy - 0.18, cz - 0.18]
    to = [cx + 0.18, cy + 0.18, cz + 0.18]
    e = elem(ei, hname, fr, to, UV1); elements.append(e)
    h_b = bone(bi, hname, [cx, cy, cz], [eid(ei)])
    bones.append(h_b)
    halo_bids.append(bid(bi))
    ei += 1; bi += 1

# ── 12. PLANET SURFACE EXTRA DETAILS (4 more accents) ──────────────────────────
extra_accent_bids = []
for pi_idx, (r, h, zone, pname, init_ang) in enumerate(planet_specs):
    body_bid = planet_body_bids[pi_idx]
    body_bone_obj = next(b for b in bones if b["uuid"] == body_bid)
    body_cx = r; body_cy = h; body_cz = 0
    # one polar cap / pole feature per planet
    fr = [body_cx - 0.20, body_cy + 0.55, body_cz - 0.20]
    to = [body_cx + 0.20, body_cy + 0.85, body_cz + 0.20]
    e = elem(ei, f"{pname}_pole", fr, to, PLANET_UVS[zone]); elements.append(e)
    p_b = bone(bi, f"{pname}_pole", [body_cx, body_cy + 0.7, body_cz], [eid(ei)])
    bones.append(p_b)
    extra_accent_bids.append(bid(bi))
    body_bone_obj["children"].append(bid(bi))
    ei += 1; bi += 1

print(f"Elements: {len(elements)}, Bones: {len(bones)}")
assert len(elements) >= 80, f"Need >=80 got {len(elements)}"
assert len(bones) >= 55, f"Need >=55 bones got {len(bones)}"

# Build complete bone-uuid → name map
bone_name_map = {b["uuid"]: b["name"] for b in bones}

# Collect ALL bone uuids that we need to keyframe in animations
all_bids = (star_bids + [star_nucleus_bid] + ray_bids +
            planet_body_bids + planet_orbit_bids + accent_bids + extra_accent_bids +
            orbit_ring_bids + orbit_ring_slab_bids + marker_bids +
            path_ring_bids + path_slab_bids +
            moon_bids + moon_orbit_bids +
            [comet_body_bid, comet_orbit_bid] + trail_bids + halo_bids)

# verify we got all bones covered
all_bone_uuids = set(b["uuid"] for b in bones)
missing = all_bone_uuids - set(all_bids)
extra = set(all_bids) - all_bone_uuids
assert not missing, f"missing bones from all_bids: {missing}"
assert not extra, f"extra bones: {extra}"

# ══════════════════════════════════════════════════════════════════════════════
# Animation builders
# ══════════════════════════════════════════════════════════════════════════════

def make_kf_set(time_data, channel="position"):
    """Convenience: time_data is list of (t, x, y, z). Returns list of keyframes."""
    return [kf(t, x, y, z, channel) for (t, x, y, z) in time_data]

def standard_pos_zero(times):
    """Generate position keyframes all at 0,0,0 for given times list."""
    return [kf(t, 0, 0, 0, "position") for t in times]

def standard_rot_zero(times):
    return [kf(t, 0, 0, 0, "rotation") for t in times]

def standard_scale_one(times):
    return [kf(t, 1, 1, 1, "scale") for t in times]

# ══════════════════════════════════════════════════════════════════════════════
# SPAWN — 1.0s once
# ══════════════════════════════════════════════════════════════════════════════

def build_spawn():
    L = 1.0
    animators = {}
    times = [0.0, 0.25, 0.5, 0.75, 1.0]

    # Star slabs: scale 0 → 1.2 → 1.0, expand from origin (0.0s)
    for i, buid in enumerate(star_bids):
        rx, ry, rz, _ = star_planes[i]
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0, 0, 0, "scale"),
            kf(0.10, 0, 0, 0, "scale"),
            kf(0.20, 1.2, 1.2, 1.2, "scale"),
            kf(0.30, 0.95, 0.95, 0.95, "scale"),
            kf(0.50, 1, 1, 1, "scale"),
            kf(1.0,  1, 1, 1, "scale"),
            kf(0.0,  rx, ry, rz, "rotation"),
            kf(0.25, rx, ry, rz, "rotation"),
            kf(0.50, rx, ry, rz, "rotation"),
            kf(0.75, rx, ry, rz, "rotation"),
            kf(1.0,  rx, ry, rz, "rotation"),
            *standard_pos_zero(times),
        ]}
    # Star nucleus: scale in
    animators[star_nucleus_bid] = {"name": bone_name_map[star_nucleus_bid], "type":"bone","keyframes":[
        kf(0.0, 0,0,0,"scale"),
        kf(0.05, 0,0,0,"scale"),
        kf(0.18, 1.4,1.4,1.4,"scale"),
        kf(0.30, 1,1,1,"scale"),
        kf(0.50, 1,1,1,"scale"),
        kf(1.0, 1,1,1,"scale"),
        *standard_pos_zero(times), *standard_rot_zero(times),
    ]}
    # Ray spikes — bloom out at 0.0–0.2s
    for i, buid in enumerate(ray_bids):
        delay = i * 0.012
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(delay, 0,0,0,"scale"),
            kf(delay+0.10, 1.3,1.3,1.3,"scale"),
            kf(delay+0.18, 1,1,1,"scale"),
            kf(0.50, 1,1,1,"scale"),
            kf(1.0, 1,1,1,"scale"),
            *standard_pos_zero(times), *standard_rot_zero(times),
        ]}
    # Halo cubes
    for i, buid in enumerate(halo_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 0,0,0,"scale"),
            kf(0.08, 0,0,0,"scale"),
            kf(0.20, 1.1,1.1,1.1,"scale"),
            kf(0.30, 1,1,1,"scale"),
            kf(0.50, 1,1,1,"scale"),
            kf(1.0, 1,1,1,"scale"),
            *standard_pos_zero(times), *standard_rot_zero(times),
        ]}
    # Orbital paths expand at 0.2s
    for i, buid in enumerate(path_ring_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 0,0,0,"scale"),
            kf(0.18, 0,0,0,"scale"),
            kf(0.30, 1.05,0.5,1.05,"scale"),
            kf(0.45, 1,1,1,"scale"),
            kf(0.70, 1,1,1,"scale"),
            kf(1.0, 1,1,1,"scale"),
            *standard_pos_zero(times), *standard_rot_zero(times),
        ]}
    # Path slabs (children) — small individual scale
    for i, buid in enumerate(path_slab_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 0,0,0,"scale"),
            kf(0.20, 0,0,0,"scale"),
            kf(0.32, 1,1,1,"scale"),
            kf(0.50, 1,1,1,"scale"),
            kf(0.75, 1,1,1,"scale"),
            kf(1.0, 1,1,1,"scale"),
            *standard_pos_zero(times),
            kf(0.0, 0, 0, 0, "rotation"),
            kf(0.25, 0, 0, 0, "rotation"),
            kf(0.50, 0, 0, 0, "rotation"),
            kf(0.75, 0, 0, 0, "rotation"),
            kf(1.0, 0, 0, 0, "rotation"),
        ]}

    # Planets — fly in from above one by one at 0.3s, 0.4s, 0.5s, 0.6s
    for pi_idx, buid in enumerate(planet_orbit_bids):
        delay = 0.3 + pi_idx * 0.10
        _, _, _, _, _, init_ang = planet_centers[pi_idx]
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1, 1, 1, "scale"),
            kf(0.50, 1, 1, 1, "scale"),
            kf(1.0,  1, 1, 1, "scale"),
            kf(0.0,  0, 0, 0, "rotation"),
            kf(delay, 0, init_ang, 0, "rotation"),
            kf(delay+0.20, 0, init_ang, 0, "rotation"),
            kf(0.80, 0, init_ang, 0, "rotation"),
            kf(1.0,  0, init_ang, 0, "rotation"),
            kf(0.0,  0,0,0,"position"),
            kf(0.50, 0,0,0,"position"),
            kf(1.0,  0,0,0,"position"),
        ]}
    # Planet bodies — appear with planet, drop from above
    for pi_idx, buid in enumerate(planet_body_bids):
        delay = 0.3 + pi_idx * 0.10
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 0,0,0,"scale"),
            kf(delay, 0,0,0,"scale"),
            kf(delay+0.10, 1.2,1.2,1.2,"scale"),
            kf(delay+0.20, 1,1,1,"scale"),
            kf(0.85, 1,1,1,"scale"),
            kf(1.0, 1,1,1,"scale"),
            kf(0.0, 0, 8.0, 0, "position"),
            kf(delay, 0, 8.0, 0, "position"),
            kf(delay+0.20, 0, 0, 0, "position"),
            kf(0.85, 0, 0, 0, "position"),
            kf(1.0, 0, 0, 0, "position"),
            *standard_rot_zero(times),
        ]}
    # Planet accents — appear with their planet
    for ai, buid in enumerate(accent_bids):
        pi_idx = ai // 2
        delay = 0.32 + pi_idx * 0.10
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 0,0,0,"scale"),
            kf(delay, 0,0,0,"scale"),
            kf(delay+0.10, 1,1,1,"scale"),
            kf(0.80, 1,1,1,"scale"),
            kf(1.0, 1,1,1,"scale"),
            *standard_pos_zero(times), *standard_rot_zero(times),
        ]}
    for ai, buid in enumerate(extra_accent_bids):
        delay = 0.34 + ai * 0.10
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 0,0,0,"scale"),
            kf(delay, 0,0,0,"scale"),
            kf(delay+0.10, 1,1,1,"scale"),
            kf(0.80, 1,1,1,"scale"),
            kf(1.0, 1,1,1,"scale"),
            *standard_pos_zero(times), *standard_rot_zero(times),
        ]}

    # Orbital rings (parent) — materialise after their planet arrives (delay+0.1)
    for pi_idx, buid in enumerate(orbit_ring_bids):
        delay = 0.45 + pi_idx * 0.10
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(0.50, 1,1,1,"scale"),
            kf(1.0, 1,1,1,"scale"),
            *standard_pos_zero(times), *standard_rot_zero(times),
        ]}
    # Ring slabs — pop in
    for si, buid in enumerate(orbit_ring_slab_bids):
        ring_idx = si // 8
        slab_idx = si % 8
        delay = 0.45 + ring_idx * 0.10 + slab_idx * 0.005
        local_ang = slab_idx * 45.0
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 0,0,0,"scale"),
            kf(delay, 0,0,0,"scale"),
            kf(delay+0.08, 1.1,1.1,1.1,"scale"),
            kf(delay+0.14, 1,1,1,"scale"),
            kf(0.85, 1,1,1,"scale"),
            kf(1.0, 1,1,1,"scale"),
            *standard_pos_zero(times),
            kf(0.0, 0, -local_ang, 0, "rotation"),
            kf(0.25, 0, -local_ang, 0, "rotation"),
            kf(0.50, 0, -local_ang, 0, "rotation"),
            kf(0.75, 0, -local_ang, 0, "rotation"),
            kf(1.0, 0, -local_ang, 0, "rotation"),
        ]}
    # Markers
    for mi, buid in enumerate(marker_bids):
        ring_idx = mi // 4
        delay = 0.50 + ring_idx * 0.10 + (mi % 4) * 0.01
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 0,0,0,"scale"),
            kf(delay, 0,0,0,"scale"),
            kf(delay+0.10, 1,1,1,"scale"),
            kf(0.85, 1,1,1,"scale"),
            kf(1.0, 1,1,1,"scale"),
            *standard_pos_zero(times), *standard_rot_zero(times),
        ]}

    # Moons — orbit into position at 0.5s
    for mi, buid in enumerate(moon_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 0,0,0,"scale"),
            kf(0.55, 0,0,0,"scale"),
            kf(0.70, 1.2,1.2,1.2,"scale"),
            kf(0.80, 1,1,1,"scale"),
            kf(1.0, 1,1,1,"scale"),
            *standard_pos_zero(times), *standard_rot_zero(times),
        ]}
    for mi, buid in enumerate(moon_orbit_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1, 1, 1, "scale"),
            kf(0.50, 1, 1, 1, "scale"),
            kf(1.0, 1, 1, 1, "scale"),
            kf(0.0,  0, 0, 0, "rotation"),
            kf(0.55, 0, 0, 0, "rotation"),
            kf(0.80, 0, 90, 0, "rotation"),
            kf(1.0,  0, 120, 0, "rotation"),
            *standard_pos_zero(times),
        ]}

    # Comet — materialise at 0.8s
    animators[comet_body_bid] = {"name": bone_name_map[comet_body_bid], "type":"bone","keyframes":[
        kf(0.0, 0,0,0,"scale"),
        kf(0.78, 0,0,0,"scale"),
        kf(0.88, 1.3,1.3,1.3,"scale"),
        kf(0.95, 1,1,1,"scale"),
        kf(1.0, 1,1,1,"scale"),
        *standard_pos_zero(times), *standard_rot_zero(times),
    ]}
    animators[comet_orbit_bid] = {"name": bone_name_map[comet_orbit_bid], "type":"bone","keyframes":[
        kf(0.0, 1,1,1,"scale"),
        kf(0.5, 1,1,1,"scale"),
        kf(1.0, 1,1,1,"scale"),
        kf(0.0, 30, 0, 15, "rotation"),
        kf(0.25, 30, 0, 15, "rotation"),
        kf(0.5, 30, 0, 15, "rotation"),
        kf(0.78, 30, 0, 15, "rotation"),
        kf(1.0, 30, 30, 15, "rotation"),
        *standard_pos_zero(times),
    ]}
    for i, buid in enumerate(trail_bids):
        delay = 0.82 + i * 0.04
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 0,0,0,"scale"),
            kf(delay, 0,0,0,"scale"),
            kf(delay+0.06, 1,1,1,"scale"),
            kf(0.98, 1,1,1,"scale"),
            kf(1.0, 1,1,1,"scale"),
            *standard_pos_zero(times), *standard_rot_zero(times),
        ]}

    # Ring planet ORBIT bones (the planet_orbit_bids already covered above)
    return animators


# ══════════════════════════════════════════════════════════════════════════════
# IDLE — 7.0s loop
# ══════════════════════════════════════════════════════════════════════════════

def build_idle():
    """Loop 7s. Each planet orbits at unique 4-8s period.
    Star rotates all 3 axes. Path rings counter-rotate at half speed."""
    L = 7.0
    animators = {}
    times7 = [0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0]

    # Each planet orbit period — must complete integer cycles in 7s for loop
    # planet 0: period 7s (1 cycle)
    # planet 1: period 3.5s (2 cycles)
    # planet 2: period 7s/3 ≈ 2.333s (3 cycles)... actually let's use unique non-sync periods that complete exactly in 7s
    # Use cycle counts: 1, 2, 3, 4 — gives periods 7, 3.5, 2.33, 1.75 — all unique, all loop
    planet_cycles = [1, 2, 3, 4]

    # ─── Star slabs: rotate slowly all axes, 1 cycle each in 7s ───
    for i, buid in enumerate(star_bids):
        rx, ry, rz, _ = star_planes[i]
        # add rotation animation: slow rotate around all axes
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(1.75, 1.04,1.04,1.04,"scale"),
            kf(3.5, 1,1,1,"scale"),
            kf(5.25, 0.96,0.96,0.96,"scale"),
            kf(7.0, 1,1,1,"scale"),
            kf(0.0, rx, ry, rz, "rotation"),
            kf(1.75, rx+30, ry+45, rz+15, "rotation"),
            kf(3.5, rx+90, ry+90, rz+45, "rotation"),
            kf(5.25, rx+150, ry+135, rz+75, "rotation"),
            kf(7.0, rx+360, ry+360, rz+360, "rotation"),
            kf(0.0, 0,0,0,"position"),
            kf(3.5, 0,0,0,"position"),
            kf(7.0, 0,0,0,"position"),
        ]}
    # Nucleus pulse
    animators[star_nucleus_bid] = {"name": bone_name_map[star_nucleus_bid], "type":"bone","keyframes":[
        kf(0.0, 1,1,1,"scale"),
        kf(1.0, 1.10,1.10,1.10,"scale"),
        kf(2.0, 1,1,1,"scale"),
        kf(3.0, 1.15,1.15,1.15,"scale"),
        kf(4.0, 1,1,1,"scale"),
        kf(5.0, 0.92,0.92,0.92,"scale"),
        kf(6.0, 1.05,1.05,1.05,"scale"),
        kf(7.0, 1,1,1,"scale"),
        kf(0.0, 0,0,0,"rotation"),
        kf(2.33, 0,120,0,"rotation"),
        kf(4.66, 0,240,0,"rotation"),
        kf(7.0, 0,360,0,"rotation"),
        *standard_pos_zero([0.0, 1.75, 3.5, 5.25, 7.0]),
    ]}
    # Rays — pulse + slow co-rotate
    for i, buid in enumerate(ray_bids):
        ph = i * 0.7
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(1.0, 1+0.10*math.sin(ph),1+0.10*math.sin(ph),1+0.10*math.sin(ph),"scale"),
            kf(2.0, 1,1,1,"scale"),
            kf(3.5, 1+0.15*math.sin(ph+1),1+0.15*math.sin(ph+1),1+0.15*math.sin(ph+1),"scale"),
            kf(5.0, 1,1,1,"scale"),
            kf(6.0, 1+0.08*math.sin(ph+2),1+0.08*math.sin(ph+2),1+0.08*math.sin(ph+2),"scale"),
            kf(7.0, 1,1,1,"scale"),
            *standard_pos_zero([0.0, 1.75, 3.5, 5.25, 7.0]),
            *standard_rot_zero([0.0, 1.75, 3.5, 5.25, 7.0]),
        ]}
    # Halo orbits very slowly
    for i, buid in enumerate(halo_bids):
        ph = i * math.pi/2
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(2.0, 1.1,1.1,1.1,"scale"),
            kf(4.0, 1,1,1,"scale"),
            kf(5.5, 0.9,0.9,0.9,"scale"),
            kf(7.0, 1,1,1,"scale"),
            *standard_pos_zero([0.0, 1.75, 3.5, 5.25, 7.0]),
            *standard_rot_zero([0.0, 1.75, 3.5, 5.25, 7.0]),
        ]}

    # ─── Planet orbit bones (rotate around Y) — each unique cycle ───
    for pi_idx, buid in enumerate(planet_orbit_bids):
        cycles = planet_cycles[pi_idx]
        init_ang = planet_specs[pi_idx][4]
        # Total rotation = cycles * 360 degrees
        total = cycles * 360
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(3.5, 1,1,1,"scale"),
            kf(7.0, 1,1,1,"scale"),
            kf(0.0, 0, init_ang, 0, "rotation"),
            kf(1.75, 0, init_ang + total*0.25, 0, "rotation"),
            kf(3.5, 0, init_ang + total*0.5, 0, "rotation"),
            kf(5.25, 0, init_ang + total*0.75, 0, "rotation"),
            kf(7.0, 0, init_ang + total, 0, "rotation"),
            *standard_pos_zero([0.0, 1.75, 3.5, 5.25, 7.0]),
        ]}

    # Planet bodies — slight self-rotation, axial tilt
    for pi_idx, buid in enumerate(planet_body_bids):
        ph = pi_idx * 0.7
        spin_total = (pi_idx+1) * 360 * 2  # spin much faster than orbit
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(3.5, 1+0.04*math.sin(ph),1+0.04*math.sin(ph),1+0.04*math.sin(ph),"scale"),
            kf(7.0, 1,1,1,"scale"),
            kf(0.0, 0, 0, 0, "rotation"),
            kf(1.75, 10, spin_total*0.25, 5, "rotation"),
            kf(3.5, 10, spin_total*0.5, 5, "rotation"),
            kf(5.25, 10, spin_total*0.75, 5, "rotation"),
            kf(7.0, 0, spin_total, 0, "rotation"),
            *standard_pos_zero([0.0, 1.75, 3.5, 5.25, 7.0]),
        ]}
    # Planet accents — minor pulse
    for ai, buid in enumerate(accent_bids):
        ph = ai * 0.5
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(2.0, 1+0.08*math.sin(ph),1+0.08*math.sin(ph),1+0.08*math.sin(ph),"scale"),
            kf(4.0, 1,1,1,"scale"),
            kf(5.5, 1+0.05*math.sin(ph+1),1+0.05*math.sin(ph+1),1+0.05*math.sin(ph+1),"scale"),
            kf(7.0, 1,1,1,"scale"),
            *standard_pos_zero([0.0, 1.75, 3.5, 5.25, 7.0]),
            *standard_rot_zero([0.0, 1.75, 3.5, 5.25, 7.0]),
        ]}
    for ai, buid in enumerate(extra_accent_bids):
        ph = ai * 0.4
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(2.5, 1+0.06*math.sin(ph),1+0.06*math.sin(ph),1+0.06*math.sin(ph),"scale"),
            kf(5.0, 1,1,1,"scale"),
            kf(6.0, 1+0.04*math.sin(ph+0.5),1+0.04*math.sin(ph+0.5),1+0.04*math.sin(ph+0.5),"scale"),
            kf(7.0, 1,1,1,"scale"),
            *standard_pos_zero([0.0, 1.75, 3.5, 5.25, 7.0]),
            *standard_rot_zero([0.0, 1.75, 3.5, 5.25, 7.0]),
        ]}

    # ─── Orbital rings (parent) — rotate with their planet at SAME speed (so markers appear to orbit) ───
    # Wait — the spec says rings rotate with planet. Use same cycles.
    for pi_idx, buid in enumerate(orbit_ring_bids):
        cycles = planet_cycles[pi_idx]
        total = cycles * 360
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(3.5, 1,1,1,"scale"),
            kf(7.0, 1,1,1,"scale"),
            kf(0.0, 0, 0, 0, "rotation"),
            kf(1.75, 0, total*0.25, 0, "rotation"),
            kf(3.5, 0, total*0.5, 0, "rotation"),
            kf(5.25, 0, total*0.75, 0, "rotation"),
            kf(7.0, 0, total, 0, "rotation"),
            *standard_pos_zero([0.0, 1.75, 3.5, 5.25, 7.0]),
        ]}
    # Ring slab children (their rotation is fixed in local space) — small pulse only
    for si, buid in enumerate(orbit_ring_slab_bids):
        ring_idx = si // 8
        slab_idx = si % 8
        local_ang = slab_idx * 45.0
        ph = si * 0.35
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(1.75, 1+0.05*math.sin(ph),1+0.05*math.sin(ph),1+0.05*math.sin(ph),"scale"),
            kf(3.5, 1,1,1,"scale"),
            kf(5.25, 1+0.04*math.sin(ph+1),1+0.04*math.sin(ph+1),1+0.04*math.sin(ph+1),"scale"),
            kf(7.0, 1,1,1,"scale"),
            *standard_pos_zero([0.0, 1.75, 3.5, 5.25, 7.0]),
            kf(0.0, 0, -local_ang, 0, "rotation"),
            kf(1.75, 0, -local_ang, 0, "rotation"),
            kf(3.5, 0, -local_ang, 0, "rotation"),
            kf(5.25, 0, -local_ang, 0, "rotation"),
            kf(7.0, 0, -local_ang, 0, "rotation"),
        ]}
    # Markers (children of ring parent — already orbiting via parent)
    for mi, buid in enumerate(marker_bids):
        ph = mi * 0.4
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(1.5, 1+0.10*math.sin(ph),1+0.10*math.sin(ph),1+0.10*math.sin(ph),"scale"),
            kf(3.0, 1,1,1,"scale"),
            kf(4.5, 1+0.10*math.sin(ph+1),1+0.10*math.sin(ph+1),1+0.10*math.sin(ph+1),"scale"),
            kf(6.0, 1,1,1,"scale"),
            kf(7.0, 1,1,1,"scale"),
            *standard_pos_zero([0.0, 1.75, 3.5, 5.25, 7.0]),
            *standard_rot_zero([0.0, 1.75, 3.5, 5.25, 7.0]),
        ]}

    # ─── Path rings counter-rotate at HALF speed ───
    for pi_idx, buid in enumerate(path_ring_bids):
        cycles = planet_cycles[pi_idx]
        # half speed, opposite direction
        total = -cycles * 180
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(3.5, 1,1,1,"scale"),
            kf(7.0, 1,1,1,"scale"),
            kf(0.0, 0, 0, 0, "rotation"),
            kf(1.75, 0, total*0.25, 0, "rotation"),
            kf(3.5, 0, total*0.5, 0, "rotation"),
            kf(5.25, 0, total*0.75, 0, "rotation"),
            kf(7.0, 0, total, 0, "rotation"),
            *standard_pos_zero([0.0, 1.75, 3.5, 5.25, 7.0]),
        ]}
    # Path slabs — keep their fixed rotation in local space, small fade pulse
    for si, buid in enumerate(path_slab_bids):
        ring_idx = si // 6
        slab_idx = si % 6
        local_ang = slab_idx * 60.0 + 15.0
        ph = si * 0.2
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(1.5, 1+0.06*math.sin(ph),1+0.06*math.sin(ph),1+0.06*math.sin(ph),"scale"),
            kf(3.5, 1,1,1,"scale"),
            kf(5.5, 1+0.04*math.cos(ph),1+0.04*math.cos(ph),1+0.04*math.cos(ph),"scale"),
            kf(7.0, 1,1,1,"scale"),
            *standard_pos_zero([0.0, 1.75, 3.5, 5.25, 7.0]),
            kf(0.0, 0, -local_ang, 0, "rotation"),
            kf(1.75, 0, -local_ang, 0, "rotation"),
            kf(3.5, 0, -local_ang, 0, "rotation"),
            kf(5.25, 0, -local_ang, 0, "rotation"),
            kf(7.0, 0, -local_ang, 0, "rotation"),
        ]}

    # ─── Moon orbits — fast (3 cycles in 7s for moon1, 4 for moon2) ───
    moon_cycles = [3, 4]
    for mi, buid in enumerate(moon_orbit_bids):
        cycles = moon_cycles[mi]
        total = cycles * 360
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(3.5, 1,1,1,"scale"),
            kf(7.0, 1,1,1,"scale"),
            kf(0.0, 0, 0, 0, "rotation"),
            kf(1.75, 0, total*0.25, 0, "rotation"),
            kf(3.5, 0, total*0.5, 0, "rotation"),
            kf(5.25, 0, total*0.75, 0, "rotation"),
            kf(7.0, 0, total, 0, "rotation"),
            *standard_pos_zero([0.0, 1.75, 3.5, 5.25, 7.0]),
        ]}
    for mi, buid in enumerate(moon_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(1.75, 1.1,1.1,1.1,"scale"),
            kf(3.5, 1,1,1,"scale"),
            kf(5.25, 0.9,0.9,0.9,"scale"),
            kf(7.0, 1,1,1,"scale"),
            *standard_pos_zero([0.0, 1.75, 3.5, 5.25, 7.0]),
            *standard_rot_zero([0.0, 1.75, 3.5, 5.25, 7.0]),
        ]}

    # ─── Comet orbit — tilted, 1 cycle in 7s ───
    animators[comet_orbit_bid] = {"name": bone_name_map[comet_orbit_bid], "type":"bone","keyframes":[
        kf(0.0, 1,1,1,"scale"),
        kf(3.5, 1,1,1,"scale"),
        kf(7.0, 1,1,1,"scale"),
        kf(0.0, 30, 30, 15, "rotation"),
        kf(1.75, 30, 120, 15, "rotation"),
        kf(3.5, 30, 210, 15, "rotation"),
        kf(5.25, 30, 300, 15, "rotation"),
        kf(7.0, 30, 390, 15, "rotation"),
        *standard_pos_zero([0.0, 1.75, 3.5, 5.25, 7.0]),
    ]}
    animators[comet_body_bid] = {"name": bone_name_map[comet_body_bid], "type":"bone","keyframes":[
        kf(0.0, 1,1,1,"scale"),
        kf(1.75, 1.05,1.05,1.05,"scale"),
        kf(3.5, 1,1,1,"scale"),
        kf(5.25, 0.95,0.95,0.95,"scale"),
        kf(7.0, 1,1,1,"scale"),
        kf(0.0, 0,0,0,"rotation"),
        kf(1.75, 0, 60, 0, "rotation"),
        kf(3.5, 0, 120, 0, "rotation"),
        kf(5.25, 0, 180, 0, "rotation"),
        kf(7.0, 0, 240, 0, "rotation"),
        *standard_pos_zero([0.0, 1.75, 3.5, 5.25, 7.0]),
    ]}
    for i, buid in enumerate(trail_bids):
        ph = i * 0.5
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(1.75, 1+0.20*math.sin(ph),1+0.20*math.sin(ph),1+0.20*math.sin(ph),"scale"),
            kf(3.5, 1,1,1,"scale"),
            kf(5.25, 1+0.15*math.sin(ph+1),1+0.15*math.sin(ph+1),1+0.15*math.sin(ph+1),"scale"),
            kf(7.0, 1,1,1,"scale"),
            *standard_pos_zero([0.0, 1.75, 3.5, 5.25, 7.0]),
            *standard_rot_zero([0.0, 1.75, 3.5, 5.25, 7.0]),
        ]}

    return animators


# ══════════════════════════════════════════════════════════════════════════════
# DISSIPATE — 0.8s once
# ══════════════════════════════════════════════════════════════════════════════

def build_dissipate():
    """Planets fly tangentially outward. Star expands and collapses. Paths expand and snap.
    Moons follow tangents. Comet scales to 0."""
    animators = {}
    times = [0.0, 0.2, 0.4, 0.6, 0.8]

    # Star slabs: expand 1.5x then collapse to 0
    for i, buid in enumerate(star_bids):
        rx, ry, rz, _ = star_planes[i]
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(0.20, 1.6,1.6,1.6,"scale"),
            kf(0.40, 2.0,2.0,2.0,"scale"),
            kf(0.55, 1.0,1.0,1.0,"scale"),
            kf(0.70, 0.4,0.4,0.4,"scale"),
            kf(0.80, 0,0,0,"scale"),
            kf(0.0, rx, ry, rz, "rotation"),
            kf(0.20, rx+45, ry+45, rz+45, "rotation"),
            kf(0.40, rx+90, ry+90, rz+90, "rotation"),
            kf(0.60, rx+135, ry+135, rz+135, "rotation"),
            kf(0.80, rx+180, ry+180, rz+180, "rotation"),
            *standard_pos_zero(times),
        ]}
    animators[star_nucleus_bid] = {"name": bone_name_map[star_nucleus_bid], "type":"bone","keyframes":[
        kf(0.0, 1,1,1,"scale"),
        kf(0.20, 2.0,2.0,2.0,"scale"),
        kf(0.40, 3.0,3.0,3.0,"scale"),
        kf(0.60, 1.0,1.0,1.0,"scale"),
        kf(0.80, 0,0,0,"scale"),
        *standard_pos_zero(times), *standard_rot_zero(times),
    ]}
    # Rays — fly outward then vanish
    for i, buid in enumerate(ray_bids):
        dx, dy, dz = ray_dirs[i]
        nx, ny, nz = dx, dy, dz
        # extend along ray direction
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(0.30, 1.5,1.5,1.5,"scale"),
            kf(0.60, 0.5,0.5,0.5,"scale"),
            kf(0.80, 0,0,0,"scale"),
            kf(0.0, 0,0,0,"position"),
            kf(0.30, nx*0.5, ny*0.5, nz*0.5, "position"),
            kf(0.60, nx*1.2, ny*1.2, nz*1.2, "position"),
            kf(0.80, nx*1.8, ny*1.8, nz*1.8, "position"),
            *standard_rot_zero(times),
        ]}
    for i, buid in enumerate(halo_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(0.30, 1.5,1.5,1.5,"scale"),
            kf(0.60, 0.6,0.6,0.6,"scale"),
            kf(0.80, 0,0,0,"scale"),
            *standard_pos_zero(times), *standard_rot_zero(times),
        ]}

    # Planet orbit bones — continue rotating + fly outward via planet body position
    for pi_idx, buid in enumerate(planet_orbit_bids):
        init_ang = planet_specs[pi_idx][4]
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
    # Planet body — fly outward (in local +X direction, which after rotation = tangent)
    for pi_idx, buid in enumerate(planet_body_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(0.30, 1.1,1.1,1.1,"scale"),
            kf(0.60, 0.6,0.6,0.6,"scale"),
            kf(0.80, 0,0,0,"scale"),
            kf(0.0, 0, 0, 0, "position"),
            kf(0.20, 0, 0, 1, "position"),
            kf(0.40, 0, 0, 3, "position"),
            kf(0.60, 0, 1, 6, "position"),
            kf(0.80, 0, 2, 9, "position"),
            kf(0.0, 0, 0, 0, "rotation"),
            kf(0.40, 0, 90, 0, "rotation"),
            kf(0.80, 0, 180, 0, "rotation"),
        ]}
    for ai, buid in enumerate(accent_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(0.30, 1,1,1,"scale"),
            kf(0.60, 0.5,0.5,0.5,"scale"),
            kf(0.80, 0,0,0,"scale"),
            *standard_pos_zero(times), *standard_rot_zero(times),
        ]}
    for ai, buid in enumerate(extra_accent_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(0.30, 1,1,1,"scale"),
            kf(0.60, 0.5,0.5,0.5,"scale"),
            kf(0.80, 0,0,0,"scale"),
            *standard_pos_zero(times), *standard_rot_zero(times),
        ]}

    # Orbital rings (parent) — expand and snap
    for pi_idx, buid in enumerate(orbit_ring_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(0.20, 1.3,1.3,1.3,"scale"),
            kf(0.40, 1.7,1.7,0.4,"scale"),
            kf(0.60, 2.2,2.2,0.1,"scale"),
            kf(0.80, 0,0,0,"scale"),
            *standard_pos_zero(times), *standard_rot_zero(times),
        ]}
    for si, buid in enumerate(orbit_ring_slab_bids):
        slab_idx = si % 8
        local_ang = slab_idx * 45.0
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(0.30, 1.2,1.2,1.2,"scale"),
            kf(0.60, 0.6,0.6,0.6,"scale"),
            kf(0.80, 0,0,0,"scale"),
            *standard_pos_zero(times),
            kf(0.0, 0, -local_ang, 0, "rotation"),
            kf(0.40, 0, -local_ang, 0, "rotation"),
            kf(0.80, 0, -local_ang, 0, "rotation"),
        ]}
    for mi, buid in enumerate(marker_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(0.30, 1.2,1.2,1.2,"scale"),
            kf(0.60, 0.4,0.4,0.4,"scale"),
            kf(0.80, 0,0,0,"scale"),
            *standard_pos_zero(times), *standard_rot_zero(times),
        ]}
    # Path rings — expand and snap
    for pi_idx, buid in enumerate(path_ring_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(0.30, 1.5,1.5,1.5,"scale"),
            kf(0.50, 2.5,2.5,2.5,"scale"),
            kf(0.70, 3.0,3.0,0.0,"scale"),
            kf(0.80, 0,0,0,"scale"),
            *standard_pos_zero(times), *standard_rot_zero(times),
        ]}
    for si, buid in enumerate(path_slab_bids):
        slab_idx = si % 6
        local_ang = slab_idx * 60.0 + 15.0
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(0.30, 1.2,1.2,1.2,"scale"),
            kf(0.60, 0.4,0.4,0.4,"scale"),
            kf(0.80, 0,0,0,"scale"),
            *standard_pos_zero(times),
            kf(0.0, 0, -local_ang, 0, "rotation"),
            kf(0.40, 0, -local_ang, 0, "rotation"),
            kf(0.80, 0, -local_ang, 0, "rotation"),
        ]}

    # Moons — follow planet tangents
    for mi, buid in enumerate(moon_orbit_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(0.40, 1,1,1,"scale"),
            kf(0.80, 1,1,1,"scale"),
            kf(0.0, 0, 0, 0, "rotation"),
            kf(0.20, 0, 60, 0, "rotation"),
            kf(0.40, 0, 120, 0, "rotation"),
            kf(0.60, 0, 180, 0, "rotation"),
            kf(0.80, 0, 240, 0, "rotation"),
            *standard_pos_zero(times),
        ]}
    for mi, buid in enumerate(moon_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(0.30, 1.2,1.2,1.2,"scale"),
            kf(0.60, 0.5,0.5,0.5,"scale"),
            kf(0.80, 0,0,0,"scale"),
            *standard_pos_zero(times), *standard_rot_zero(times),
        ]}

    # Comet — continue ellipse, scale to 0
    animators[comet_orbit_bid] = {"name": bone_name_map[comet_orbit_bid], "type":"bone","keyframes":[
        kf(0.0, 1,1,1,"scale"),
        kf(0.40, 1,1,1,"scale"),
        kf(0.80, 1,1,1,"scale"),
        kf(0.0, 30, 30, 15, "rotation"),
        kf(0.20, 30, 90, 15, "rotation"),
        kf(0.40, 30, 150, 15, "rotation"),
        kf(0.60, 30, 210, 15, "rotation"),
        kf(0.80, 30, 270, 15, "rotation"),
        *standard_pos_zero(times),
    ]}
    animators[comet_body_bid] = {"name": bone_name_map[comet_body_bid], "type":"bone","keyframes":[
        kf(0.0, 1,1,1,"scale"),
        kf(0.30, 1.2,1.2,1.2,"scale"),
        kf(0.55, 0.7,0.7,0.7,"scale"),
        kf(0.80, 0,0,0,"scale"),
        *standard_pos_zero(times), *standard_rot_zero(times),
    ]}
    for i, buid in enumerate(trail_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0, 1,1,1,"scale"),
            kf(0.20, 1.3,1.3,1.3,"scale"),
            kf(0.50, 0.5,0.5,0.5,"scale"),
            kf(0.80, 0,0,0,"scale"),
            *standard_pos_zero(times), *standard_rot_zero(times),
        ]}

    return animators


# ── Build model ──────────────────────────────────────────────────────────────
spawn_anim = {
    "uuid": aid(1), "name": "animation.nightmare_planetarium.spawn",
    "loop": "once", "length": 1.0, "snapping": 24,
    "selected": False, "saved": False, "animators": build_spawn()
}
idle_anim = {
    "uuid": aid(2), "name": "animation.nightmare_planetarium.idle",
    "loop": "loop", "length": 7.0, "snapping": 24,
    "selected": False, "saved": False, "animators": build_idle()
}
dissipate_anim = {
    "uuid": aid(3), "name": "animation.nightmare_planetarium.dissipate",
    "loop": "once", "length": 0.8, "snapping": 24,
    "selected": False, "saved": False, "animators": build_dissipate()
}

# Verify each animator has every bone
for anim in [spawn_anim, idle_anim, dissipate_anim]:
    keyed = set(anim["animators"].keys())
    if keyed != set(b["uuid"] for b in bones):
        missing = set(b["uuid"] for b in bones) - keyed
        print(f"ANIM {anim['name']} missing bones: {[bone_name_map[m] for m in missing]}")
        assert False

# Outliner: top-level bones only.
# Top-level = those NOT in another bone's children list.
all_child_bids = set()
for b in bones:
    for c in b["children"]:
        if isinstance(c, str) and c.startswith("b"):
            all_child_bids.add(c)
outliner = [b["uuid"] for b in bones if b["uuid"] not in all_child_bids]

textures = [
    {"id":"0","name":"nightmare_planetarium_tex","relative_path":"../textures/nightmare_planetarium_tex.png",
     "folder":"devilsdream","namespace":"","visible":True,"mode":"bitmap","saved":False,
     "uuid":tid(1),"source":f"data:image/png;base64,{b64_0}"},
    {"id":"1","name":"nightmare_planetarium_tex1","relative_path":"../textures/nightmare_planetarium_tex1.png",
     "folder":"devilsdream","namespace":"","visible":True,"mode":"bitmap","saved":False,
     "uuid":tid(2),"source":f"data:image/png;base64,{b64_1}"},
]

model = {
    "meta": {"format_version":"4.10","model_format":"free","box_uv":False},
    "name": "nightmare_planetarium",
    "geometry": "geometry.nightmare_planetarium",
    "resolution": {"width":64,"height":64},
    "elements": elements,
    "outliner": outliner,
    "textures": textures,
    "animations": [spawn_anim, idle_anim, dissipate_anim]
}

out_path = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_planetarium.bbmodel"
with open(out_path, "w", encoding="utf-8") as f:
    json.dump(model, f, separators=(',', ':'))

size = os.path.getsize(out_path)
print(f"Written: {out_path}")
print(f"Elements: {len(elements)} / Bones: {len(bones)}")
print(f"Animations: 3 (spawn 1.0s once, idle 7.0s loop, dissipate 0.8s once)")
print(f"Size: {size:,} bytes ({size/1024:.1f} KB)")
pass_fail = "PASS" if size >= 300*1024 else "FAIL"
print(f"{pass_fail}: {'>=300KB' if size>=300*1024 else f'only {size//1024}KB'}")
