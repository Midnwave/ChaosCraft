"""
Build nightmare_cathedral.bbmodel
A skeletal Gothic cathedral arch — two side pillars + pointed Gothic arch + keystone +
stained glass interior + foundation + flying buttresses.
Target: 80+ elements, 55+ bones, 3 animations, 6+ keyframes per bone per animation, >=300KB.
Uniqueness anchor: from front view, MUST read as a Gothic cathedral arch — two
vertical pillars (left X=-4, right X=+4) topped by pointed arch curving inward to
keystone at top center (Y=15+).
"""
import json, math, os

b64_0 = open("D:/CC/ChaosCraft/tools/gen_textures/m16_b64.txt").read().strip()
b64_1 = open("D:/CC/ChaosCraft/tools/gen_textures/m16_tex1_b64.txt").read().strip()

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

# ── 1. PILLAR SEGMENTS (4 stacked × 2 pillars = 8 bones) ─────────────────────
# Left pillar at X=-4, right at X=+4
# Each pillar: 4 stacked segments. Widest base 3×3, tapering to 2×2 at top
pillar_bids_L = []
pillar_bids_R = []

# Pillar segments: heights stack from Y=0 to Y=12 (each 3 tall)
pillar_segs_def = [
    # (y_lo, y_hi, half_width)
    (0.0,  3.0, 1.5),   # base segment - 3 wide
    (3.0,  6.0, 1.4),
    (6.0,  9.0, 1.2),
    (9.0, 12.0, 1.0),   # top segment - 2 wide
]

for side_i, sx in enumerate([-4.0, 4.0]):
    side_label = ['L', 'R'][side_i]
    for s, (y_lo, y_hi, hw) in enumerate(pillar_segs_def):
        name = f"pillar_{side_label}_seg{s+1}"
        fr = [sx - hw, y_lo, -hw]
        to = [sx + hw, y_hi, hw]
        e = elem(ei, name, fr, to, UV0); elements.append(e)
        b = bone(bi, name, [sx, y_lo, 0], [eid(ei)]); bones.append(b)
        if side_i == 0:
            pillar_bids_L.append(bid(bi))
        else:
            pillar_bids_R.append(bid(bi))
        ei += 1; bi += 1

# ── 2. PILLAR CARVED DETAILS (2 details per segment × 8 = 16 bones) ──────────
# Each pillar segment gets 2 carved detail accents on the front face
pillar_detail_bids = []
for side_i, sx in enumerate([-4.0, 4.0]):
    side_label = ['L', 'R'][side_i]
    for s, (y_lo, y_hi, hw) in enumerate(pillar_segs_def):
        for d in range(2):
            name = f"pillar_{side_label}_seg{s+1}_carv{d+1}"
            cy = y_lo + (d * 1.5) + 0.5
            cy2 = cy + 0.8
            fr = [sx - hw + 0.1, cy, -hw - 0.12]
            to = [sx + hw - 0.1, cy2, -hw - 0.05]
            e = elem(ei, name, fr, to, UV0); elements.append(e)
            b = bone(bi, name, [sx, cy, -hw], [eid(ei)]); bones.append(b)
            pillar_detail_bids.append(bid(bi))
            ei += 1; bi += 1

# ── 3. ARCH SLABS (6 bones - 3 per side curving inward) ──────────────────────
# Pointed Gothic arch from pillar tops at Y=12 inward to keystone at Y~16
# Left side: slabs go from X=-4 angling inward and upward
# 3 slabs per side, increasing height toward keystone
arch_bids_L = []
arch_bids_R = []

# Arch spec: each slab is positioned along an arc-like curve
# Slab 1: anchored near pillar top, slight angle inward
# Slab 2: middle, more inward
# Slab 3: closest to keystone, highest
arch_slab_defs = [
    # (relative position along arc t, angle degrees from vertical)
    (0.0, 18),   # bottom slab — tilts inward 18 deg
    (0.5, 45),   # middle slab — 45 deg
    (1.0, 72),   # top slab near keystone — 72 deg (almost horizontal)
]

# Compute arch slab geometry per side
def arch_slab_pos(side_i, idx):
    """Returns (origin_x, origin_y, angle_z_deg) for arch slab."""
    sx = -4.0 if side_i == 0 else 4.0
    # The arch curves from (sx, 12) inward to (0, 16). Use parametric ellipse.
    t, ang = arch_slab_defs[idx]
    # Position along the arc
    # x = sx * cos(t * 90deg)  -- starts at sx, ends at 0
    # y = 12 + sin(t * 90deg) * 4  -- starts at 12, ends at 16
    px = sx * math.cos(t * math.pi/2)
    py = 12.0 + math.sin(t * math.pi/2) * 4.0
    # angle: tilt the slab so it follows the curve. Sign depends on side.
    sign = 1 if side_i == 0 else -1   # left side rotates +Z (counterclockwise as looking from +Z)
    return px, py, sign * ang

for side_i in range(2):
    side_label = ['L', 'R'][side_i]
    for a in range(3):
        ox, oy, az = arch_slab_pos(side_i, a)
        name = f"arch_{side_label}_{a+1}"
        # Slab dimensions: long horizontally (along the arc tangent), thin vertically
        slab_len = 1.6
        slab_h = 0.8
        slab_d = 0.6
        # local cube before bone rotation
        fr = [-slab_len/2, -slab_h/2, -slab_d/2]
        to = [ slab_len/2,  slab_h/2,  slab_d/2]
        # but bbmodel cubes are axis-aligned in element space, then bone rotation
        # rotates them. Origin is the bone origin; element fr/to are absolute.
        # We'll create the cube around (ox, oy, 0) and let the bone rotate it.
        ax_fr = [ox - slab_len/2, oy - slab_h/2, -slab_d/2]
        ax_to = [ox + slab_len/2, oy + slab_h/2,  slab_d/2]
        e = elem(ei, name, ax_fr, ax_to, UV0); elements.append(e)
        b = bone(bi, name, [ox, oy, 0], [eid(ei)], rot=[0, 0, az]); bones.append(b)
        if side_i == 0:
            arch_bids_L.append(bid(bi))
        else:
            arch_bids_R.append(bid(bi))
        ei += 1; bi += 1

arch_bids = arch_bids_L + arch_bids_R

# ── 4. KEYSTONE + DETAIL (2 bones, 2 elements) ───────────────────────────────
# Wide flat slab at arch apex Y=16, with carved detail cube
keystone_bids = []
# Main keystone slab
ks_fr = [-1.2, 15.5, -0.6]
ks_to = [ 1.2, 16.5,  0.6]
e = elem(ei, "keystone_slab", ks_fr, ks_to, UV0); elements.append(e)
b = bone(bi, "keystone_slab", [0, 16.0, 0], [eid(ei)]); bones.append(b)
keystone_bids.append(bid(bi)); ei += 1; bi += 1

# Carved detail cube on keystone front
ksd_fr = [-0.4, 15.7, -0.7]
ksd_to = [ 0.4, 16.3, -0.55]
e = elem(ei, "keystone_carving", ksd_fr, ksd_to, UV0); elements.append(e)
b = bone(bi, "keystone_carving", [0, 16.0, -0.6], [eid(ei)]); bones.append(b)
keystone_bids.append(bid(bi)); ei += 1; bi += 1

# ── 5. STAINED GLASS INTERIOR (3 bones, 3 elements) ──────────────────────────
# 3 large flat slabs filling interior of arch vertically
glass_bids = []
glass_defs = [
    ("glass_lower",  [-3.0, 0.5, -0.05], [3.0, 5.0, 0.05]),
    ("glass_mid",    [-2.7, 5.0, -0.05], [2.7, 9.5, 0.05]),
    ("glass_upper",  [-2.3, 9.5, -0.05], [2.3, 14.5, 0.05]),
]
for name, fr, to in glass_defs:
    e = elem(ei, name, fr, to, UV1); elements.append(e)
    b = bone(bi, name, [0, fr[1], 0], [eid(ei)]); bones.append(b)
    glass_bids.append(bid(bi)); ei += 1; bi += 1

# ── 6. STAINED GLASS FRAMING (10 bones, 10 elements) ─────────────────────────
# Lead came framework around glass — narrow vertical/horizontal accents
glass_frame_bids = []
gframe_defs = [
    ("gframe_left_v",   [-3.05, 0.5, -0.08], [-2.95, 14.5, 0.08]),
    ("gframe_right_v",  [ 2.95, 0.5, -0.08], [ 3.05, 14.5, 0.08]),
    ("gframe_top_h",    [-3.0, 14.4, -0.08], [3.0, 14.5, 0.08]),
    ("gframe_bot_h",    [-3.0, 0.5,  -0.08], [3.0, 0.6,  0.08]),
    ("gframe_mid1_h",   [-3.0, 5.0,  -0.08], [3.0, 5.1,  0.08]),
    ("gframe_mid2_h",   [-3.0, 9.5,  -0.08], [3.0, 9.6,  0.08]),
    ("gframe_center_v", [-0.05, 0.5, -0.08], [0.05, 14.5, 0.08]),
    ("gframe_diag1",    [-0.8, 6.5, -0.08], [0.8, 7.5, 0.08]),
    ("gframe_diag2",    [-0.8, 11.0, -0.08], [0.8, 12.0, 0.08]),
    ("gframe_diag3",    [-0.8, 2.5, -0.08], [0.8, 3.5, 0.08]),
]
for name, fr, to in gframe_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [(fr[0]+to[0])/2, fr[1], 0], [eid(ei)]); bones.append(b)
    glass_frame_bids.append(bid(bi)); ei += 1; bi += 1

# ── 7. BASE FOUNDATION (2 wide bones extending outward, plus extras = 4 total) ─
# 2 wide flat slabs from each pillar base extending outward
foundation_bids = []
foundation_defs = [
    ("foundation_L_outer", [-7.0, -0.4, -2.0], [-2.5, 0.0, 2.0]),
    ("foundation_R_outer", [ 2.5, -0.4, -2.0], [ 7.0, 0.0, 2.0]),
    # Also a small front slab for visual mass
    ("foundation_L_front", [-5.5, -0.3, -3.0], [-2.5, 0.0, -2.0]),
    ("foundation_R_front", [ 2.5, -0.3, -3.0], [ 5.5, 0.0, -2.0]),
]
for name, fr, to in foundation_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [(fr[0]+to[0])/2, fr[1], 0], [eid(ei)]); bones.append(b)
    foundation_bids.append(bid(bi)); ei += 1; bi += 1

# ── 8. FLYING BUTTRESSES (2 bones, 2 elements) ───────────────────────────────
# Diagonal slab arms from mid-pillar outward
buttress_bids = []
# Left buttress: from X=-4 Y=8 sloping outward to X=-7 Y=2
# Use a rotated slab
for side_i, sx in enumerate([-4.0, 4.0]):
    side_label = ['L', 'R'][side_i]
    name = f"buttress_{side_label}"
    # The slab is long, thin, oriented along a diagonal
    slab_len = 5.0
    sign = 1 if side_i == 1 else -1
    # Place it so when rotated, it goes from mid-pillar outward
    # Origin at the mid-pillar attachment point: (sx, 7.5, 0)
    ox = sx
    oy = 7.5
    # Build the slab in axis-aligned form, rotate 50 deg around Z
    # Element along +X direction from origin
    # Going outward: for left side, it should extend to negative X
    ax_fr = [ox if sign > 0 else ox - slab_len, oy - 0.3, -0.4]
    ax_to = [ox + slab_len if sign > 0 else ox, oy + 0.3, 0.4]
    angle_z = -55 if sign > 0 else 55  # tilt down outward
    e = elem(ei, name, ax_fr, ax_to, UV0); elements.append(e)
    b = bone(bi, name, [ox, oy, 0], [eid(ei)], rot=[0, 0, angle_z]); bones.append(b)
    buttress_bids.append(bid(bi)); ei += 1; bi += 1

# ── 9. ADDITIONAL ARCH ORNAMENTS (8 bones — small accent cubes on arch) ──────
arch_orn_bids = []
# 4 ornaments per arch side at the seams between arch slabs
for side_i in range(2):
    side_label = ['L', 'R'][side_i]
    sign = -1 if side_i == 0 else 1
    for o in range(4):
        # Place ornaments along the arch curve at sub-positions
        t = 0.1 + o * 0.25
        sx = -4.0 if side_i == 0 else 4.0
        ox = sx * math.cos(t * math.pi/2)
        oy = 12.0 + math.sin(t * math.pi/2) * 4.0
        name = f"arch_orn_{side_label}_{o+1}"
        size = 0.25
        ax_fr = [ox - size, oy - size, -size - 0.5]
        ax_to = [ox + size, oy + size, -size - 0.3]
        e = elem(ei, name, ax_fr, ax_to, UV0); elements.append(e)
        b = bone(bi, name, [ox, oy, -0.4], [eid(ei)]); bones.append(b)
        arch_orn_bids.append(bid(bi))
        ei += 1; bi += 1

# ── 10. PILLAR CAPITAL (decorative top) ─ 2 bones ────────────────────────────
# Wider stone block atop each pillar before the arch starts
capital_bids = []
for side_i, sx in enumerate([-4.0, 4.0]):
    side_label = ['L', 'R'][side_i]
    name = f"capital_{side_label}"
    fr = [sx - 1.3, 11.8, -1.3]
    to = [sx + 1.3, 12.4, 1.3]
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [sx, 12.0, 0], [eid(ei)]); bones.append(b)
    capital_bids.append(bid(bi)); ei += 1; bi += 1

# ── 11. PILLAR BASE (decorative bottom) ─ 2 bones ────────────────────────────
pillar_base_bids = []
for side_i, sx in enumerate([-4.0, 4.0]):
    side_label = ['L', 'R'][side_i]
    name = f"pbase_{side_label}"
    fr = [sx - 1.7, -0.05, -1.7]
    to = [sx + 1.7, 0.50, 1.7]
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [sx, 0, 0], [eid(ei)]); bones.append(b)
    pillar_base_bids.append(bid(bi)); ei += 1; bi += 1

# ── 12. SMALL FOUNDATION DETAILS (4 bones) ───────────────────────────────────
fnd_detail_bids = []
fdetail_defs = [
    ("fdetail_1", [-6.5, 0.0, -1.5], [-6.0, 0.3, 1.5]),
    ("fdetail_2", [ 6.0, 0.0, -1.5], [ 6.5, 0.3, 1.5]),
    ("fdetail_3", [-3.0, 0.0, -2.5], [3.0, 0.2, -2.0]),
    ("fdetail_4", [-3.0, 0.0,  2.0], [3.0, 0.2,  2.5]),
]
for name, fr, to in fdetail_defs:
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [(fr[0]+to[0])/2, fr[1], 0], [eid(ei)]); bones.append(b)
    fnd_detail_bids.append(bid(bi)); ei += 1; bi += 1

# ── 13. SPIRES (2 small spires atop capitals) ──────────────────────────────
spire_bids = []
for side_i, sx in enumerate([-4.0, 4.0]):
    side_label = ['L', 'R'][side_i]
    name = f"spire_{side_label}"
    fr = [sx - 0.3, 12.4, -0.3]
    to = [sx + 0.3, 13.4, 0.3]
    e = elem(ei, name, fr, to, UV0); elements.append(e)
    b = bone(bi, name, [sx, 12.4, 0], [eid(ei)]); bones.append(b)
    spire_bids.append(bid(bi)); ei += 1; bi += 1

# ── 14. KEYSTONE TOP CAP (1 bone) ────────────────────────────────────────────
ks_cap_bids = []
e = elem(ei, "keystone_cap", [-0.5, 16.5, -0.3], [0.5, 17.2, 0.3], UV0); elements.append(e)
b = bone(bi, "keystone_cap", [0, 16.5, 0], [eid(ei)]); bones.append(b)
ks_cap_bids.append(bid(bi)); ei += 1; bi += 1

# ── 15. ARCH INNER GLOW STRIPS (4 bones, tex1) ───────────────────────────────
# Thin emissive purple strips along the inside of the arch
arch_glow_bids = []
for side_i in range(2):
    side_label = ['L', 'R'][side_i]
    sign = -1 if side_i == 0 else 1
    for g in range(2):
        t = 0.2 + g * 0.4
        sx = -4.0 if side_i == 0 else 4.0
        ox = sx * math.cos(t * math.pi/2) * 0.85  # slightly inward
        oy = 12.0 + math.sin(t * math.pi/2) * 4.0
        name = f"arch_glow_{side_label}_{g+1}"
        size = 0.18
        fr = [ox - 0.5, oy - 0.1, -0.04]
        to = [ox + 0.5, oy + 0.1, 0.04]
        e = elem(ei, name, fr, to, UV1); elements.append(e)
        b = bone(bi, name, [ox, oy, 0], [eid(ei)]); bones.append(b)
        arch_glow_bids.append(bid(bi)); ei += 1; bi += 1

# ── 16. PILLAR SIDE FLUTES (additional vertical fluting on pillars - 8 bones) ─
# 4 thin vertical slabs on each pillar's outer face — fluting
flute_bids = []
for side_i, sx in enumerate([-4.0, 4.0]):
    side_label = ['L', 'R'][side_i]
    for s, (y_lo, y_hi, hw) in enumerate(pillar_segs_def):
        name = f"flute_{side_label}_seg{s+1}"
        outer_x = sx - hw - 0.08 if side_i == 0 else sx + hw + 0.08
        # Thin slab along the outer edge of pillar
        if side_i == 0:
            fr = [outer_x, y_lo + 0.2, -hw + 0.2]
            to = [outer_x + 0.06, y_hi - 0.2, hw - 0.2]
        else:
            fr = [outer_x - 0.06, y_lo + 0.2, -hw + 0.2]
            to = [outer_x, y_hi - 0.2, hw - 0.2]
        e = elem(ei, name, fr, to, UV0); elements.append(e)
        b = bone(bi, name, [outer_x, y_lo, 0], [eid(ei)]); bones.append(b)
        flute_bids.append(bid(bi)); ei += 1; bi += 1

# ── 17. TRANSEPT KEYSTONES (carved details on each pillar capital — 4 bones) ─
transept_bids = []
for side_i, sx in enumerate([-4.0, 4.0]):
    side_label = ['L', 'R'][side_i]
    for d in range(2):
        name = f"transept_{side_label}_{d+1}"
        offset_x = -0.4 + d * 0.8
        fr = [sx + offset_x - 0.2, 12.0, -1.4]
        to = [sx + offset_x + 0.2, 12.4, -1.25]
        e = elem(ei, name, fr, to, UV0); elements.append(e)
        b = bone(bi, name, [sx + offset_x, 12.0, -1.3], [eid(ei)]); bones.append(b)
        transept_bids.append(bid(bi)); ei += 1; bi += 1

print(f"Elements: {len(elements)}, Bones: {len(bones)}")
assert len(elements) >= 80, f"Need 80+ elements, got {len(elements)}"
assert len(bones) >= 55, f"Need 55+ bones, got {len(bones)}"

# All bone groups
all_pillar_bids = pillar_bids_L + pillar_bids_R
all_bids = (all_pillar_bids + pillar_detail_bids + arch_bids + keystone_bids +
            glass_bids + glass_frame_bids + foundation_bids + buttress_bids +
            arch_orn_bids + capital_bids + pillar_base_bids + fnd_detail_bids +
            spire_bids + ks_cap_bids + arch_glow_bids + flute_bids + transept_bids)

bone_name_map = {b["uuid"]: b["name"] for b in bones}

# Save the rotation of each bone (for arch slabs / buttresses) so idle/dissipate
# preserve it. Build a base_rot map.
base_rot = {}
for b in bones:
    base_rot[b["uuid"]] = list(b["rotation"])

# ══════════════════════════════════════════════════════════════════════════════
# ANIMATIONS
# ══════════════════════════════════════════════════════════════════════════════

def build_spawn():
    """spawn once 1.6s: foundation 0.2s, pillar seg1 0.4s, seg2 0.55s, seg3 0.70s,
    seg4 0.85s, arch slabs 0.9s+0.08s stagger, keystone drops 1.1s, glass 1.2s,
    buttresses 1.3s."""
    animators = {}

    # Pillar bases (decorative bottom): appear with foundation
    for i, buid in enumerate(pillar_base_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"scale"),
            kf(0.20, 0,0,0,"scale"),
            kf(0.30, 1,1,1,"scale"),
            kf(0.6,  1,1,1,"scale"),
            kf(1.1,  1,1,1,"scale"),
            kf(1.6,  1,1,1,"scale"),
            kf(0.0,  0,-2,0,"position"),
            kf(0.20, 0,-2,0,"position"),
            kf(0.30, 0,0,0,"position"),
            kf(0.8,  0,0,0,"position"),
            kf(1.2,  0,0,0,"position"),
            kf(1.6,  0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.5,  0,0,0,"rotation"),
            kf(1.0,  0,0,0,"rotation"),
            kf(1.6,  0,0,0,"rotation"),
        ]}

    # Foundation: slam down at 0.2s
    for i, buid in enumerate(foundation_bids + fnd_detail_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   0,0,0,"scale"),
            kf(0.20,  0,0,0,"scale"),
            kf(0.32,  1,1,1,"scale"),
            kf(0.7,   1,1,1,"scale"),
            kf(1.1,   1,1,1,"scale"),
            kf(1.6,   1,1,1,"scale"),
            kf(0.0,   0,5,0,"position"),
            kf(0.20,  0,5,0,"position"),
            kf(0.27,  0,0,0,"position"),
            kf(0.32,  0,-0.15,0,"position"),  # slam impact
            kf(0.4,   0,0,0,"position"),
            kf(1.6,   0,0,0,"position"),
            kf(0.0,   0,0,0,"rotation"),
            kf(0.8,   0,0,0,"rotation"),
            kf(1.6,   0,0,0,"rotation"),
        ]}

    # Pillar segments stagger 0.4 / 0.55 / 0.70 / 0.85
    pillar_delays = [0.40, 0.55, 0.70, 0.85]
    for i, buid in enumerate(pillar_bids_L + pillar_bids_R):
        seg = i % 4
        delay = pillar_delays[seg]
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,        0,0,0,"scale"),
            kf(delay,      0,0,0,"scale"),
            kf(delay+0.10, 1,1,1,"scale"),
            kf(1.1,        1,1,1,"scale"),
            kf(1.4,        1,1,1,"scale"),
            kf(1.6,        1,1,1,"scale"),
            kf(0.0,        0,-3,0,"position"),
            kf(delay,      0,-3,0,"position"),
            kf(delay+0.10, 0,0,0,"position"),
            kf(1.1,        0,0,0,"position"),
            kf(1.4,        0,0,0,"position"),
            kf(1.6,        0,0,0,"position"),
            kf(0.0,        0,0,0,"rotation"),
            kf(0.8,        0,0,0,"rotation"),
            kf(1.6,        0,0,0,"rotation"),
        ]}

    # Pillar carved details: appear with their pillar seg
    for i, buid in enumerate(pillar_detail_bids):
        seg = (i % 8) // 2  # 4 segs per side, 2 details per seg
        delay = pillar_delays[seg] + 0.08
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,        0,0,0,"scale"),
            kf(delay,      0,0,0,"scale"),
            kf(delay+0.06, 1,1,1,"scale"),
            kf(1.1,        1,1,1,"scale"),
            kf(1.4,        1,1,1,"scale"),
            kf(1.6,        1,1,1,"scale"),
            kf(0.0,        0,0,0,"position"),
            kf(delay,      0,0,0,"position"),
            kf(1.1,        0,0,0,"position"),
            kf(1.6,        0,0,0,"position"),
            kf(0.0,        0,0,0,"rotation"),
            kf(0.8,        0,0,0,"rotation"),
            kf(1.6,        0,0,0,"rotation"),
        ]}

    # Pillar flutes: appear with their pillar seg
    for i, buid in enumerate(flute_bids):
        seg = i % 4
        delay = pillar_delays[seg] + 0.05
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,        0,0,0,"scale"),
            kf(delay,      0,0,0,"scale"),
            kf(delay+0.06, 1,1,1,"scale"),
            kf(1.1,        1,1,1,"scale"),
            kf(1.4,        1,1,1,"scale"),
            kf(1.6,        1,1,1,"scale"),
            kf(0.0,        0,0,0,"position"),
            kf(delay,      0,0,0,"position"),
            kf(1.1,        0,0,0,"position"),
            kf(1.6,        0,0,0,"position"),
            kf(0.0,        0,0,0,"rotation"),
            kf(0.8,        0,0,0,"rotation"),
            kf(1.6,        0,0,0,"rotation"),
        ]}

    # Transepts: appear with capitals
    for i, buid in enumerate(transept_bids):
        delay = 0.90 + (i % 2) * 0.04
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,        0,0,0,"scale"),
            kf(delay,      0,0,0,"scale"),
            kf(delay+0.06, 1,1,1,"scale"),
            kf(1.1,        1,1,1,"scale"),
            kf(1.4,        1,1,1,"scale"),
            kf(1.6,        1,1,1,"scale"),
            kf(0.0,        0,0,0,"position"),
            kf(delay,      0,0,0,"position"),
            kf(1.1,        0,0,0,"position"),
            kf(1.6,        0,0,0,"position"),
            kf(0.0,        0,0,0,"rotation"),
            kf(0.8,        0,0,0,"rotation"),
            kf(1.6,        0,0,0,"rotation"),
        ]}

    # Capital + spires: appear with seg4
    for buid in capital_bids + spire_bids:
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   0,0,0,"scale"),
            kf(0.85,  0,0,0,"scale"),
            kf(0.95,  1,1,1,"scale"),
            kf(1.2,   1,1,1,"scale"),
            kf(1.4,   1,1,1,"scale"),
            kf(1.6,   1,1,1,"scale"),
            kf(0.0,   0,1,0,"position"),
            kf(0.85,  0,1,0,"position"),
            kf(0.95,  0,0,0,"position"),
            kf(1.2,   0,0,0,"position"),
            kf(1.6,   0,0,0,"position"),
            kf(0.0,   0,0,0,"rotation"),
            kf(0.8,   0,0,0,"rotation"),
            kf(1.6,   0,0,0,"rotation"),
        ]}

    # Arch slabs: deploy from pillar tops inward at 0.9s with 0.08s stagger
    for i, buid in enumerate(arch_bids):
        side_i = 0 if i < 3 else 1
        a = i % 3
        delay = 0.90 + a * 0.08
        # original rotation
        sign = 1 if side_i == 0 else -1
        final_z = base_rot[buid][2]   # final tilt
        # start un-tilted (vertical) and rotate into place
        start_z = 0  # vertical, will rotate to final
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,        0,0,0,"scale"),
            kf(delay,      0,0,0,"scale"),
            kf(delay+0.10, 1,1,1,"scale"),
            kf(1.3,        1,1,1,"scale"),
            kf(1.5,        1,1,1,"scale"),
            kf(1.6,        1,1,1,"scale"),
            kf(0.0,        sign*1.5, 0, 0,"position"),
            kf(delay,      sign*1.5, 0, 0,"position"),
            kf(delay+0.10, 0, 0, 0,"position"),
            kf(1.3,        0, 0, 0,"position"),
            kf(1.6,        0, 0, 0,"position"),
            kf(0.0,        0, 0, start_z,"rotation"),
            kf(delay,      0, 0, start_z,"rotation"),
            kf(delay+0.15, 0, 0, final_z,"rotation"),
            kf(1.3,        0, 0, final_z,"rotation"),
            kf(1.5,        0, 0, final_z,"rotation"),
            kf(1.6,        0, 0, final_z,"rotation"),
        ]}

    # Arch ornaments
    for i, buid in enumerate(arch_orn_bids):
        delay = 1.0 + (i % 4) * 0.05
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,        0,0,0,"scale"),
            kf(delay,      0,0,0,"scale"),
            kf(delay+0.06, 1,1,1,"scale"),
            kf(1.3,        1,1,1,"scale"),
            kf(1.5,        1,1,1,"scale"),
            kf(1.6,        1,1,1,"scale"),
            kf(0.0,        0,0,0,"position"),
            kf(delay,      0,0,0,"position"),
            kf(1.3,        0,0,0,"position"),
            kf(1.6,        0,0,0,"position"),
            kf(0.0,        0,0,0,"rotation"),
            kf(0.8,        0,0,0,"rotation"),
            kf(1.6,        0,0,0,"rotation"),
        ]}

    # Keystone: drops from above at 1.1s and locks in
    for buid in keystone_bids + ks_cap_bids:
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,   0,0,0,"scale"),
            kf(1.10,  0,0,0,"scale"),
            kf(1.18,  1,1,1,"scale"),
            kf(1.3,   1,1,1,"scale"),
            kf(1.5,   1,1,1,"scale"),
            kf(1.6,   1,1,1,"scale"),
            kf(0.0,   0,5,0,"position"),
            kf(1.10,  0,5,0,"position"),
            kf(1.20,  0,0.2,0,"position"),
            kf(1.25,  0,-0.1,0,"position"),  # impact bounce
            kf(1.3,   0,0,0,"position"),
            kf(1.6,   0,0,0,"position"),
            kf(0.0,   0,0,0,"rotation"),
            kf(0.8,   0,0,0,"rotation"),
            kf(1.6,   0,0,0,"rotation"),
        ]}

    # Stained glass: phases in from top to bottom at 1.2s
    for i, buid in enumerate(glass_bids):
        # i=0 lower, 1 mid, 2 upper. Top first means upper(2) earliest.
        order = 2 - i  # upper=0, mid=1, lower=2
        delay = 1.20 + order * 0.05
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,        0,0,0,"scale"),
            kf(delay,      0,0,0,"scale"),
            kf(delay+0.10, 1,1,0.001,"scale"),
            kf(delay+0.20, 1,1,1,"scale"),
            kf(1.5,        1,1,1,"scale"),
            kf(1.6,        1,1,1,"scale"),
            kf(0.0,        0,0,0,"position"),
            kf(delay,      0,0,0,"position"),
            kf(1.5,        0,0,0,"position"),
            kf(1.6,        0,0,0,"position"),
            kf(0.0,        0,0,0,"rotation"),
            kf(0.8,        0,0,0,"rotation"),
            kf(1.6,        0,0,0,"rotation"),
        ]}

    # Glass framing
    for i, buid in enumerate(glass_frame_bids):
        delay = 1.18 + (i % 5) * 0.03
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,        0,0,0,"scale"),
            kf(delay,      0,0,0,"scale"),
            kf(delay+0.06, 1,1,1,"scale"),
            kf(1.5,        1,1,1,"scale"),
            kf(1.6,        1,1,1,"scale"),
            kf(0.0,        0,0,0,"position"),
            kf(delay,      0,0,0,"position"),
            kf(1.5,        0,0,0,"position"),
            kf(1.6,        0,0,0,"position"),
            kf(0.0,        0,0,0,"rotation"),
            kf(0.8,        0,0,0,"rotation"),
            kf(1.6,        0,0,0,"rotation"),
        ]}

    # Arch glow strips: appear with glass
    for i, buid in enumerate(arch_glow_bids):
        delay = 1.25 + i * 0.03
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,        0,0,0,"scale"),
            kf(delay,      0,0,0,"scale"),
            kf(delay+0.05, 1,1,1,"scale"),
            kf(1.5,        1,1,1,"scale"),
            kf(1.6,        1,1,1,"scale"),
            kf(0.0,        0,0,0,"position"),
            kf(delay,      0,0,0,"position"),
            kf(1.5,        0,0,0,"position"),
            kf(1.6,        0,0,0,"position"),
            kf(0.0,        0,0,0,"rotation"),
            kf(0.8,        0,0,0,"rotation"),
            kf(1.6,        0,0,0,"rotation"),
        ]}

    # Flying buttresses: deploy at 1.3s
    for i, buid in enumerate(buttress_bids):
        sign = -1 if i == 0 else 1
        final_z = base_rot[buid][2]
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,        0,0,0,"scale"),
            kf(1.30,       0,0,0,"scale"),
            kf(1.40,       1,1,1,"scale"),
            kf(1.5,        1,1,1,"scale"),
            kf(1.6,        1,1,1,"scale"),
            kf(0.0,        0,0,0,"position"),
            kf(1.30,       0,0,0,"position"),
            kf(1.5,        0,0,0,"position"),
            kf(1.6,        0,0,0,"position"),
            kf(0.0,        0,0,0,"rotation"),
            kf(1.30,       0,0,0,"rotation"),
            kf(1.42,       0,0,final_z*1.1,"rotation"),  # slight overshoot
            kf(1.5,        0,0,final_z,"rotation"),
            kf(1.6,        0,0,final_z,"rotation"),
        ]}

    return animators


def build_idle():
    """idle loop, 9.0s. Cathedral barely moves. Subtle sway ±0.3°. Glass pulses
    emissive (use scale). Pillar carved details subtly oscillate. Keystone slow Y
    rotation. Buttresses still."""
    animators = {}

    sway_amp = 0.3

    # Pillars: subtle sway
    for i, buid in enumerate(all_pillar_bids := pillar_bids_L + pillar_bids_R):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"rotation"),
            kf(2.25, 0,0,sway_amp*0.5,"rotation"),
            kf(4.5,  0,0,0,"rotation"),
            kf(6.75, 0,0,-sway_amp*0.5,"rotation"),
            kf(9.0,  0,0,0,"rotation"),
            kf(0.0,  0,0,0,"position"),
            kf(2.25, 0,0.02,0,"position"),
            kf(4.5,  0,0,0,"position"),
            kf(6.75, 0,-0.02,0,"position"),
            kf(9.0,  0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"),
            kf(4.5,  1.005,1.005,1.005,"scale"),
            kf(9.0,  1,1,1,"scale"),
        ]}

    # Pillar bases / capitals / spires / pillar details / flutes / transepts: barely move
    for buid in pillar_base_bids + capital_bids + spire_bids + pillar_detail_bids + flute_bids + transept_bids:
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"rotation"),
            kf(2.25, 0,0,sway_amp*0.3,"rotation"),
            kf(4.5,  0,0,0,"rotation"),
            kf(6.75, 0,0,-sway_amp*0.3,"rotation"),
            kf(9.0,  0,0,0,"rotation"),
            kf(0.0,  0,0,0,"position"),
            kf(4.5,  0,0.01,0,"position"),
            kf(9.0,  0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"),
            kf(2.25, 1.01,1.0,1.01,"scale"),
            kf(4.5,  1,1,1,"scale"),
            kf(6.75, 0.99,1.0,0.99,"scale"),
            kf(9.0,  1,1,1,"scale"),
        ]}

    # Arch slabs: subtle sway preserving base rot
    for i, buid in enumerate(arch_bids):
        base_z = base_rot[buid][2]
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,base_z,"rotation"),
            kf(2.25, 0,0,base_z+sway_amp*0.3,"rotation"),
            kf(4.5,  0,0,base_z,"rotation"),
            kf(6.75, 0,0,base_z-sway_amp*0.3,"rotation"),
            kf(9.0,  0,0,base_z,"rotation"),
            kf(0.0,  0,0,0,"position"),
            kf(4.5,  0,0.01,0,"position"),
            kf(9.0,  0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"),
            kf(4.5,  1,1,1,"scale"),
            kf(9.0,  1,1,1,"scale"),
        ]}

    # Arch ornaments: barely move
    for i, buid in enumerate(arch_orn_bids):
        phase = i * 0.4
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"rotation"),
            kf(2.25, 0,sway_amp*math.sin(phase),0,"rotation"),
            kf(4.5,  0,0,0,"rotation"),
            kf(6.75, 0,-sway_amp*math.sin(phase),0,"rotation"),
            kf(9.0,  0,0,0,"rotation"),
            kf(0.0,  0,0,0,"position"),
            kf(4.5,  0,0,0,"position"),
            kf(9.0,  0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"),
            kf(2.25, 1.02,1.02,1.02,"scale"),
            kf(4.5,  1,1,1,"scale"),
            kf(6.75, 0.98,0.98,0.98,"scale"),
            kf(9.0,  1,1,1,"scale"),
        ]}

    # Keystone: very slowly rotates Y. Very slow.
    for buid in keystone_bids + ks_cap_bids:
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,0,"rotation"),
            kf(2.25, 0,3,0,"rotation"),
            kf(4.5,  0,6,0,"rotation"),
            kf(6.75, 0,3,0,"rotation"),
            kf(9.0,  0,0,0,"rotation"),
            kf(0.0,  0,0,0,"position"),
            kf(4.5,  0,0.02,0,"position"),
            kf(9.0,  0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"),
            kf(4.5,  1.005,1.005,1.005,"scale"),
            kf(9.0,  1,1,1,"scale"),
        ]}

    # Stained glass: pulses emissive (scale subtly to fake brightness pulse)
    for i, buid in enumerate(glass_bids):
        phase = i * 1.2
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(1.5,  1.02,1.02,2.0,"scale"),
            kf(3.0,  1,1,1,"scale"),
            kf(4.5,  1.04,1.04,3.0,"scale"),
            kf(6.0,  1,1,1,"scale"),
            kf(7.5,  1.02,1.02,2.0,"scale"),
            kf(9.0,  1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(4.5,  0,0,0.02,"position"),
            kf(9.0,  0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(4.5,  0,0,0,"rotation"),
            kf(9.0,  0,0,0,"rotation"),
        ]}

    # Glass framing: still
    for i, buid in enumerate(glass_frame_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(2.25, 1,1,1,"scale"),
            kf(4.5,  1,1,1,"scale"),
            kf(6.75, 1,1,1,"scale"),
            kf(9.0,  1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(2.25, 0,0,0,"position"),
            kf(4.5,  0,0.005,0,"position"),
            kf(6.75, 0,0,0,"position"),
            kf(9.0,  0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(4.5,  0,0,0,"rotation"),
            kf(9.0,  0,0,0,"rotation"),
        ]}

    # Foundation: still
    for buid in foundation_bids + fnd_detail_bids:
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(2.25, 1,1,1,"scale"),
            kf(4.5,  1,1,1,"scale"),
            kf(6.75, 1,1,1,"scale"),
            kf(9.0,  1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(4.5,  0,0,0,"position"),
            kf(9.0,  0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(4.5,  0,0,0,"rotation"),
            kf(9.0,  0,0,0,"rotation"),
        ]}

    # Buttresses: hold still (preserve rot)
    for i, buid in enumerate(buttress_bids):
        base_z = base_rot[buid][2]
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  0,0,base_z,"rotation"),
            kf(2.25, 0,0,base_z,"rotation"),
            kf(4.5,  0,0,base_z,"rotation"),
            kf(6.75, 0,0,base_z,"rotation"),
            kf(9.0,  0,0,base_z,"rotation"),
            kf(0.0,  0,0,0,"position"),
            kf(4.5,  0,0.005,0,"position"),
            kf(9.0,  0,0,0,"position"),
            kf(0.0,  1,1,1,"scale"),
            kf(4.5,  1,1,1,"scale"),
            kf(9.0,  1,1,1,"scale"),
        ]}

    # Arch glow strips: pulse with glass
    for i, buid in enumerate(arch_glow_bids):
        phase = i * 0.5
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(1.5,  1.05,1.05,1.5,"scale"),
            kf(3.0,  1,1,1,"scale"),
            kf(4.5,  1.10,1.10,2.0,"scale"),
            kf(6.0,  1,1,1,"scale"),
            kf(7.5,  1.05,1.05,1.5,"scale"),
            kf(9.0,  1,1,1,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(4.5,  0,0,0,"position"),
            kf(9.0,  0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(4.5,  0,0,0,"rotation"),
            kf(9.0,  0,0,0,"rotation"),
        ]}

    return animators


def build_dissipate():
    """dissipate once 1.3s: glass dims first. Keystone lifts out. Arch slabs
    separate from centre outward. Pillar segments sink top-first. Foundation
    contracts. De-assembles in reverse construction order."""
    animators = {}

    # Glass: dims first (scale Z to 0)
    for i, buid in enumerate(glass_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.05, 1,1,1,"scale"),
            kf(0.20, 1,1,0.1,"scale"),  # dim Z (becomes thin)
            kf(0.30, 0,0,0,"scale"),
            kf(0.7,  0,0,0,"scale"),
            kf(1.3,  0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.30, 0,0,0,"position"),
            kf(0.7,  0,0,0,"position"),
            kf(1.3,  0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.65, 0,0,0,"rotation"),
            kf(1.3,  0,0,0,"rotation"),
        ]}

    # Glass framing dims with glass
    for i, buid in enumerate(glass_frame_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.10, 1,1,1,"scale"),
            kf(0.25, 0.5,0.5,0.5,"scale"),
            kf(0.35, 0,0,0,"scale"),
            kf(0.7,  0,0,0,"scale"),
            kf(1.3,  0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.35, 0,0,0,"position"),
            kf(0.7,  0,0,0,"position"),
            kf(1.3,  0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.65, 0,0,0,"rotation"),
            kf(1.3,  0,0,0,"rotation"),
        ]}

    # Arch glow dims first
    for i, buid in enumerate(arch_glow_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.10, 1,1,1,"scale"),
            kf(0.20, 0.3,0.3,0.3,"scale"),
            kf(0.25, 0,0,0,"scale"),
            kf(0.7,  0,0,0,"scale"),
            kf(1.3,  0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.25, 0,0,0,"position"),
            kf(0.7,  0,0,0,"position"),
            kf(1.3,  0,0,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.65, 0,0,0,"rotation"),
            kf(1.3,  0,0,0,"rotation"),
        ]}

    # Keystone: lifts out at 0.3s
    for buid in keystone_bids + ks_cap_bids:
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.30, 1,1,1,"scale"),
            kf(0.60, 1,1,1,"scale"),
            kf(0.80, 0.5,0.5,0.5,"scale"),
            kf(1.0,  0,0,0,"scale"),
            kf(1.3,  0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.30, 0,0.5,0,"position"),
            kf(0.60, 0,2.5,0,"position"),
            kf(1.0,  0,5.0,0,"position"),
            kf(1.3,  0,8.0,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.65, 0,180,0,"rotation"),
            kf(1.3,  0,360,0,"rotation"),
        ]}

    # Arch slabs: separate from centre outward at 0.4s. Top arch slabs (a=2) first
    # — they're closest to keystone — then a=1 then a=0.
    for i, buid in enumerate(arch_bids):
        side_i = 0 if i < 3 else 1
        a = i % 3
        delay = 0.40 + (2 - a) * 0.06
        sign = -1 if side_i == 0 else 1
        base_z = base_rot[buid][2]
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,        1,1,1,"scale"),
            kf(delay,      1,1,1,"scale"),
            kf(delay+0.20, 1,1,1,"scale"),
            kf(delay+0.40, 0.5,0.5,0.5,"scale"),
            kf(1.3,        0,0,0,"scale"),
            kf(0.0,        0,0,0,"position"),
            kf(delay,      0,0,0,"position"),
            kf(delay+0.30, sign*1.5, 0.5, 0,"position"),
            kf(1.3,        sign*4.0, -3.0, 0,"position"),  # falls outward and down
            kf(0.0,        0,0,base_z,"rotation"),
            kf(delay,      0,0,base_z,"rotation"),
            kf(delay+0.30, 0,0,base_z + sign*30,"rotation"),
            kf(1.3,        0,0,base_z + sign*120,"rotation"),
        ]}

    # Arch ornaments: fall with arch slabs
    for i, buid in enumerate(arch_orn_bids):
        side_i = 0 if i < 4 else 1
        sign = -1 if side_i == 0 else 1
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.40, 1,1,1,"scale"),
            kf(0.70, 0.5,0.5,0.5,"scale"),
            kf(1.3,  0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.40, 0,0,0,"position"),
            kf(0.70, sign*0.5, -1.5, 0,"position"),
            kf(1.3,  sign*1.2, -4.0, 0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.65, 0, 90, 0,"rotation"),
            kf(1.3,  0, 360, 0,"rotation"),
        ]}

    # Pillar segments: sink top-first. seg4 (top) sinks first.
    for i, buid in enumerate(pillar_bids_L + pillar_bids_R):
        seg = i % 4  # 0=base, 3=top
        delay = 0.55 + (3 - seg) * 0.10  # top (seg=3) at 0.55, base (seg=0) at 0.85
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,        1,1,1,"scale"),
            kf(delay,      1,1,1,"scale"),
            kf(delay+0.15, 1,0.5,1,"scale"),
            kf(delay+0.30, 0.3,0.1,0.3,"scale"),
            kf(1.3,        0,0,0,"scale"),
            kf(0.0,        0,0,0,"position"),
            kf(delay,      0,0,0,"position"),
            kf(delay+0.20, 0,-1,0,"position"),
            kf(delay+0.40, 0,-2,0,"position"),
            kf(1.3,        0,-3,0,"position"),
            kf(0.0,        0,0,0,"rotation"),
            kf(0.65,       0,0,0,"rotation"),
            kf(1.3,        0,0,0,"rotation"),
        ]}

    # Pillar details / capitals / spires sink with their pillar segs
    for i, buid in enumerate(pillar_detail_bids):
        seg = (i % 8) // 2
        delay = 0.55 + (3 - seg) * 0.10
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,        1,1,1,"scale"),
            kf(delay,      1,1,1,"scale"),
            kf(delay+0.15, 0.5,0.5,0.5,"scale"),
            kf(1.3,        0,0,0,"scale"),
            kf(0.0,        0,0,0,"position"),
            kf(delay,      0,0,0,"position"),
            kf(1.3,        0,-3,0,"position"),
            kf(0.0,        0,0,0,"rotation"),
            kf(0.65,       0,0,0,"rotation"),
            kf(1.3,        0,0,0,"rotation"),
        ]}

    # Pillar flutes
    for i, buid in enumerate(flute_bids):
        seg = i % 4
        delay = 0.55 + (3 - seg) * 0.10
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,        1,1,1,"scale"),
            kf(delay,      1,1,1,"scale"),
            kf(delay+0.15, 0.5,0.5,0.5,"scale"),
            kf(1.3,        0,0,0,"scale"),
            kf(0.0,        0,0,0,"position"),
            kf(delay,      0,0,0,"position"),
            kf(1.3,        0,-3,0,"position"),
            kf(0.0,        0,0,0,"rotation"),
            kf(0.65,       0,0,0,"rotation"),
            kf(1.3,        0,0,0,"rotation"),
        ]}

    # Transepts: fall with capitals
    for i, buid in enumerate(transept_bids):
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.55, 1,1,1,"scale"),
            kf(0.75, 0.5,0.5,0.5,"scale"),
            kf(0.95, 0,0,0,"scale"),
            kf(1.3,  0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.55, 0,0,0,"position"),
            kf(0.95, 0,-2,0,"position"),
            kf(1.3,  0,-3,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.65, 0,0,0,"rotation"),
            kf(1.3,  0,0,0,"rotation"),
        ]}

    for buid in capital_bids + spire_bids:
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.55, 1,1,1,"scale"),
            kf(0.75, 0.5,0.5,0.5,"scale"),
            kf(0.95, 0,0,0,"scale"),
            kf(1.3,  0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.55, 0,0,0,"position"),
            kf(0.95, 0,-2,0,"position"),
            kf(1.3,  0,-3,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.65, 0, 30, 0,"rotation"),
            kf(1.3,  0, 90, 0,"rotation"),
        ]}

    # Pillar bases: sink with the rest
    for buid in pillar_base_bids:
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.85, 1,1,1,"scale"),
            kf(1.05, 0.5,0.5,0.5,"scale"),
            kf(1.3,  0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.85, 0,0,0,"position"),
            kf(1.3,  0,-2,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.65, 0,0,0,"rotation"),
            kf(1.3,  0,0,0,"rotation"),
        ]}

    # Buttresses: fall outward
    for i, buid in enumerate(buttress_bids):
        sign = -1 if i == 0 else 1
        base_z = base_rot[buid][2]
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.50, 1,1,1,"scale"),
            kf(0.80, 0.5,0.5,0.5,"scale"),
            kf(1.3,  0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.50, 0,0,0,"position"),
            kf(0.80, sign*1.5, -1.0, 0,"position"),
            kf(1.3,  sign*3.5, -3.5, 0,"position"),
            kf(0.0,  0,0,base_z,"rotation"),
            kf(0.50, 0,0,base_z,"rotation"),
            kf(0.80, 0,0,base_z + sign*30,"rotation"),
            kf(1.3,  0,0,base_z + sign*120,"rotation"),
        ]}

    # Foundation: contracts last
    for buid in foundation_bids + fnd_detail_bids:
        animators[buid] = {"name": bone_name_map[buid], "type":"bone","keyframes":[
            kf(0.0,  1,1,1,"scale"),
            kf(0.95, 1,1,1,"scale"),
            kf(1.10, 0.7,1,0.7,"scale"),
            kf(1.20, 0.3,1,0.3,"scale"),
            kf(1.3,  0,0,0,"scale"),
            kf(0.0,  0,0,0,"position"),
            kf(0.95, 0,0,0,"position"),
            kf(1.3,  0,-0.5,0,"position"),
            kf(0.0,  0,0,0,"rotation"),
            kf(0.65, 0,0,0,"rotation"),
            kf(1.3,  0,0,0,"rotation"),
        ]}

    return animators


# ── Build model ──────────────────────────────────────────────────────────────
spawn_anim = {
    "uuid": aid(1), "name": "animation.nightmare_cathedral.spawn",
    "loop": "once", "length": 1.6, "snapping": 24,
    "selected": False, "saved": False, "animators": build_spawn()
}
idle_anim = {
    "uuid": aid(2), "name": "animation.nightmare_cathedral.idle",
    "loop": "loop", "length": 9.0, "snapping": 24,
    "selected": False, "saved": False, "animators": build_idle()
}
dissipate_anim = {
    "uuid": aid(3), "name": "animation.nightmare_cathedral.dissipate",
    "loop": "once", "length": 1.3, "snapping": 24,
    "selected": False, "saved": False, "animators": build_dissipate()
}

outliner = [b["uuid"] for b in bones]

textures = [
    {"id":"0","name":"nightmare_cathedral_tex","relative_path":"../textures/nightmare_cathedral_tex.png",
     "folder":"devilsdream","namespace":"","visible":True,"mode":"bitmap","saved":False,
     "uuid":tid(1),"source":f"data:image/png;base64,{b64_0}"},
    {"id":"1","name":"nightmare_cathedral_tex1","relative_path":"../textures/nightmare_cathedral_tex1.png",
     "folder":"devilsdream","namespace":"","visible":True,"mode":"bitmap","saved":False,
     "uuid":tid(2),"source":f"data:image/png;base64,{b64_1}"},
]

model = {
    "meta": {"format_version":"4.10","model_format":"free","box_uv":False},
    "name": "nightmare_cathedral",
    "geometry": "geometry.nightmare_cathedral",
    "resolution": {"width":64,"height":64},
    "elements": elements,
    "outliner": outliner,
    "textures": textures,
    "animations": [spawn_anim, idle_anim, dissipate_anim]
}

out_path = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_cathedral.bbmodel"
with open(out_path, "w", encoding="utf-8") as f:
    json.dump(model, f, separators=(',', ':'))

size = os.path.getsize(out_path)
print(f"Written: {out_path}")
print(f"Elements: {len(elements)}, Bones: {len(bones)}")
print(f"Size: {size:,} bytes ({size/1024:.1f} KB)")
print(f"{'PASS' if size>=300*1024 else 'FAIL'}: {'>=300KB' if size>=300*1024 else f'only {size//1024}KB'}")
