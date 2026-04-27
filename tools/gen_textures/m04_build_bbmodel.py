"""
Build fallen_halo_burst.bbmodel
87 elements, 60 bones, 3 animations, 6+ keyframes per bone per animation.
"""
import json, math, os

b64_0 = open("D:/CC/ChaosCraft/tools/gen_textures/m04_b64.txt").read().strip()
b64_1 = open("D:/CC/ChaosCraft/tools/gen_textures/m04_tex1_b64.txt").read().strip()

# ─── UUID helpers ────────────────────────────────────────────────────────────
_e = 0
_b = 0
_k = 0

def eid():
    global _e; _e += 1
    return f"e{_e:03d}"

def bid():
    global _b; _b += 1
    return f"b{_b:03d}"

def kid():
    global _k; _k += 1
    return f"k{_k:04d}"

# ─── UV faces ────────────────────────────────────────────────────────────────
def uv_faces(tex=0):
    if tex == 0:
        return {
            "north": {"uv": [2,2,30,30],   "texture": 0},
            "east":  {"uv": [34,2,62,30],  "texture": 0},
            "south": {"uv": [2,2,30,30],   "texture": 0},
            "west":  {"uv": [34,2,62,30],  "texture": 0},
            "up":    {"uv": [2,34,30,62],  "texture": 0},
            "down":  {"uv": [34,34,62,62], "texture": 0},
        }
    else:
        return {
            "north": {"uv": [2,2,30,30],   "texture": 1},
            "east":  {"uv": [34,2,62,30],  "texture": 1},
            "south": {"uv": [2,2,30,30],   "texture": 1},
            "west":  {"uv": [34,2,62,30],  "texture": 1},
            "up":    {"uv": [2,34,30,62],  "texture": 1},
            "down":  {"uv": [34,34,62,62], "texture": 1},
        }

# ─── Element factory ─────────────────────────────────────────────────────────
def elem(name, frm, to, tex=0):
    return {
        "name": name, "box_uv": False, "rescale": False, "locked": False,
        "render_order": "default", "allow_mirror_modeling": True,
        "from": frm, "to": to, "autouv": 0, "color": 0,
        "uuid": eid(),
        "faces": uv_faces(tex)
    }

# ─── Bone factory ────────────────────────────────────────────────────────────
def bone(name, origin, children, rot=None):
    b = {
        "name": name, "origin": origin,
        "rotation": rot if rot else [0, 0, 0],
        "uuid": bid(), "export": True, "isOpen": True,
        "children": children
    }
    return b

# ─── Keyframe factory ────────────────────────────────────────────────────────
def kf(time, x, y, z, channel="rotation", interp="catmullrom"):
    return {
        "uuid": kid(), "time": time, "color": -1,
        "interpolation": interp,
        "data_points": [{"x": str(x), "y": str(y), "z": str(z)}],
        "channel": channel
    }

# ─── Animator entry ──────────────────────────────────────────────────────────
def animator(bone_uuid, bone_name, keyframes):
    return {"name": bone_name, "type": "bone", "keyframes": keyframes}

# ═══════════════════════════════════════════════════════════════════════════════
# GEOMETRY CONSTRUCTION
# ═══════════════════════════════════════════════════════════════════════════════

elements      = []
bones         = []
bone_registry = []  # list of (uuid, name)

halo_ring_uuids    = []
inner_glow_uuids   = []   # multiple elems for inner_glow bone → stored separately
fracture_uuids     = []
fracture_b_uuids   = []   # secondary fracture pieces
det_shard_uuids    = []
ground_shadow_uuids= []
micro_uuids        = []
halo_outer_uuids   = []
halo_inner_uuids   = []
glow_accent_uuids  = []
shadow_detail_uuids= []

# ── 16 Halo ring slabs at Y=8, radius ~5, slightly irregular
# Each slab: 1.8×0.25×0.5, Y-rotated to tangent angle
ring_slab_radii = [4.8, 5.0, 5.1, 4.9, 5.2, 5.0, 4.8, 5.3,
                   5.0, 4.9, 5.1, 5.2, 4.8, 5.0, 5.1, 4.9]
for i in range(16):
    angle = (i / 16.0) * 2 * math.pi
    r = ring_slab_radii[i]
    cx = r * math.sin(angle)
    cz = r * math.cos(angle)
    rot_y = math.degrees(angle)  # tangent angle
    # Slab: 1.8 along tangent direction, 0.5 radially, 0.25 tall
    sw = 1.8; sh = 0.25; sd = 0.5
    ring_e = elem(f"halo_ring_{i+1}_e",
                  [cx - sw/2, 8.0, cz - sd/2],
                  [cx + sw/2, 8.0 + sh, cz + sd/2], tex=0)
    elements.append(ring_e)
    ring_b = bone(f"halo_ring_{i+1}", [cx, 8.0, cz], [ring_e["uuid"]], rot=[0, rot_y, 0])
    bones.append(ring_b)
    halo_ring_uuids.append(ring_b["uuid"])
    bone_registry.append((ring_b["uuid"], ring_b["name"]))

# ── Inner glow: 4 overlapping flat slabs at Y=8.2, tex1
# One bone holding 4 elements
inner_glow_elem_uuids = []
glow_sizes = [(2.0, 0.1, 2.0), (2.8, 0.1, 2.8), (3.2, 0.1, 3.2), (3.5, 0.1, 3.5)]
for gs_w, gs_h, gs_d in glow_sizes:
    ig_e = elem(f"inner_glow_{len(inner_glow_elem_uuids)+1}_e",
                [-gs_w/2, 8.2, -gs_d/2],
                [gs_w/2, 8.2 + gs_h, gs_d/2], tex=1)
    elements.append(ig_e)
    inner_glow_elem_uuids.append(ig_e["uuid"])

ig_bone = bone("inner_glow", [0, 8.2, 0], inner_glow_elem_uuids)
bones.append(ig_bone)
inner_glow_uuids.append(ig_bone["uuid"])
bone_registry.append((ig_bone["uuid"], ig_bone["name"]))

# ── 8 Fracture line bones at Y=8.1, various angles, tex1
# Each 4.5×0.1×0.2
frac_angles = [10, 25, 40, 55, 70, 85, 100, 115]
for i, angle_deg in enumerate(frac_angles):
    angle = math.radians(angle_deg)
    # Position fracture across halo face
    fx = math.cos(angle) * 2.5
    fz = math.sin(angle) * 2.5
    fl = 4.5; fh = 0.1; fd = 0.2
    frac_e = elem(f"fracture_{i+1}_e",
                  [fx - fl/2, 8.1, fz - fd/2],
                  [fx + fl/2, 8.1 + fh, fz + fd/2], tex=1)
    elements.append(frac_e)
    frac_b = bone(f"fracture_{i+1}", [fx, 8.1, fz], [frac_e["uuid"]], rot=[0, angle_deg, 0])
    bones.append(frac_b)
    fracture_uuids.append(frac_b["uuid"])
    bone_registry.append((frac_b["uuid"], frac_b["name"]))

# ── 8 Secondary fracture piece bones (smaller, beside main fractures)
for i, angle_deg in enumerate(frac_angles):
    angle = math.radians(angle_deg + 5)
    fx = math.cos(angle) * 1.8
    fz = math.sin(angle) * 1.8
    fl = 2.8; fh = 0.08; fd = 0.15
    frac_b2_e = elem(f"fracture_b_{i+1}_e",
                     [fx - fl/2, 8.12, fz - fd/2],
                     [fx + fl/2, 8.12 + fh, fz + fd/2], tex=1)
    elements.append(frac_b2_e)
    frac_b2 = bone(f"fracture_b_{i+1}", [fx, 8.12, fz], [frac_b2_e["uuid"]], rot=[0, angle_deg+5, 0])
    bones.append(frac_b2)
    fracture_b_uuids.append(frac_b2["uuid"])
    bone_registry.append((frac_b2["uuid"], frac_b2["name"]))

# ── 16 Detonation shard bones: tapered 0.4×1.2×0.3, just inside ring edge pointing outward
for i in range(16):
    angle = (i / 16.0) * 2 * math.pi
    r_det = 4.5
    dx = r_det * math.sin(angle)
    dz = r_det * math.cos(angle)
    dw = 0.4; dh = 1.2; dd = 0.3
    det_e = elem(f"det_shard_{i+1}_e",
                 [dx - dw/2, 8.0, dz - dd/2],
                 [dx + dw/2, 8.0 + dh, dz + dd/2], tex=0)
    elements.append(det_e)
    det_b = bone(f"det_shard_{i+1}", [dx, 8.0, dz], [det_e["uuid"]], rot=[0, math.degrees(angle), 0])
    bones.append(det_b)
    det_shard_uuids.append(det_b["uuid"])
    bone_registry.append((det_b["uuid"], det_b["name"]))

# ── Ground shadow: 4 overlapping flat slabs at Y=0, tex1 (single bone)
shadow_sizes = [(3.0,0.1,3.0),(5.0,0.08,5.0),(6.5,0.06,6.5),(7.5,0.05,7.5)]
gs_elem_uuids = []
for ss_w, ss_h, ss_d in shadow_sizes:
    gs_e = elem(f"ground_shadow_{len(gs_elem_uuids)+1}_e",
                [-ss_w/2, 0.0, -ss_d/2],
                [ss_w/2, ss_h, ss_d/2], tex=1)
    elements.append(gs_e)
    gs_elem_uuids.append(gs_e["uuid"])

gs_bone = bone("ground_shadow", [0, 0, 0], gs_elem_uuids)
bones.append(gs_bone)
ground_shadow_uuids.append(gs_bone["uuid"])
bone_registry.append((gs_bone["uuid"], gs_bone["name"]))

# ── 12 Orbital micro-shards: small 0.4×0.6×0.3 pieces at radii 5.5-6.5, heights 7.5-8.5
micro_radii  = [5.5, 6.0, 6.5, 5.8, 6.2, 5.5, 6.0, 6.5, 5.7, 6.3, 5.9, 6.1]
micro_heights= [8.0, 7.5, 8.3, 8.5, 7.8, 8.1, 7.6, 8.4, 7.9, 8.2, 7.7, 8.5]
for i in range(12):
    angle = (i / 12.0) * 2 * math.pi
    r_m = micro_radii[i]
    mx = r_m * math.sin(angle)
    mz = r_m * math.cos(angle)
    my = micro_heights[i]
    mw = 0.4; mh = 0.6; md = 0.3
    mic_e = elem(f"micro_{i+1}_e",
                 [mx - mw/2, my, mz - md/2],
                 [mx + mw/2, my + mh, mz + md/2], tex=0)
    elements.append(mic_e)
    mic_b = bone(f"micro_{i+1}", [mx, my, mz], [mic_e["uuid"]], rot=[0, math.degrees(angle), 0])
    bones.append(mic_b)
    micro_uuids.append(mic_b["uuid"])
    bone_registry.append((mic_b["uuid"], mic_b["name"]))

# ── 3 Halo outer ring bones at radius 6.5
for i in range(3):
    angle = (i / 3.0) * 2 * math.pi
    r_out = 6.5
    ox = r_out * math.sin(angle)
    oz = r_out * math.cos(angle)
    ow = 2.0; oh = 0.2; od = 0.5
    out_e = elem(f"halo_outer_{i+1}_e",
                 [ox - ow/2, 8.0, oz - od/2],
                 [ox + ow/2, 8.0 + oh, oz + od/2], tex=0)
    elements.append(out_e)
    out_b = bone(f"halo_outer_{i+1}", [ox, 8.0, oz], [out_e["uuid"]], rot=[0, math.degrees(angle), 0])
    bones.append(out_b)
    halo_outer_uuids.append(out_b["uuid"])
    bone_registry.append((out_b["uuid"], out_b["name"]))

# ── 4 Halo inner detail pieces at ring center
halo_inner_defs = [
    (-1.0, 8.15, -1.0,  2.0, 0.08, 0.5),
    ( 0.5, 8.15, -0.8,  1.5, 0.08, 0.4),
    (-0.3, 8.15,  0.9,  1.8, 0.08, 0.4),
    ( 1.0, 8.15,  0.4,  1.6, 0.08, 0.5),
]
for i, (hix, hiy, hiz, hiw, hih, hid) in enumerate(halo_inner_defs):
    hi_e = elem(f"halo_inner_{i+1}_e",
                [hix - hiw/2, hiy, hiz - hid/2],
                [hix + hiw/2, hiy + hih, hiz + hid/2], tex=1)
    elements.append(hi_e)
    hi_b = bone(f"halo_inner_{i+1}", [hix, hiy, hiz], [hi_e["uuid"]])
    bones.append(hi_b)
    halo_inner_uuids.append(hi_b["uuid"])
    bone_registry.append((hi_b["uuid"], hi_b["name"]))

# ── 5 Halo glow accent pieces
glow_accent_defs = [
    (-2.5, 8.25, -2.5, 1.2, 0.1, 0.4),
    ( 2.5, 8.25, -2.5, 1.2, 0.1, 0.4),
    (-2.5, 8.25,  2.5, 1.2, 0.1, 0.4),
    ( 2.5, 8.25,  2.5, 1.2, 0.1, 0.4),
    ( 0.0, 8.28,  0.0, 1.8, 0.1, 1.8),
]
for i, (gx, gy, gz, gw, gh, gd) in enumerate(glow_accent_defs):
    ga_e = elem(f"glow_accent_{i+1}_e",
                [gx - gw/2, gy, gz - gd/2],
                [gx + gw/2, gy + gh, gz + gd/2], tex=1)
    elements.append(ga_e)
    ga_b = bone(f"glow_accent_{i+1}", [gx, gy, gz], [ga_e["uuid"]])
    bones.append(ga_b)
    glow_accent_uuids.append(ga_b["uuid"])
    bone_registry.append((ga_b["uuid"], ga_b["name"]))

# ── 4 Shadow detail pieces
shadow_detail_defs = [
    (-3.5, 0.02, -3.5, 2.0, 0.05, 0.4),
    ( 3.5, 0.02, -3.5, 2.0, 0.05, 0.4),
    (-3.5, 0.02,  3.5, 2.0, 0.05, 0.4),
    ( 3.5, 0.02,  3.5, 2.0, 0.05, 0.4),
]
for i, (sdx, sdy, sdz, sdw, sdh, sdd) in enumerate(shadow_detail_defs):
    sd_e = elem(f"shadow_detail_{i+1}_e",
                [sdx - sdw/2, sdy, sdz - sdd/2],
                [sdx + sdw/2, sdy + sdh, sdz + sdd/2], tex=1)
    elements.append(sd_e)
    sd_b = bone(f"shadow_detail_{i+1}", [sdx, sdy, sdz], [sd_e["uuid"]])
    bones.append(sd_b)
    shadow_detail_uuids.append(sd_b["uuid"])
    bone_registry.append((sd_b["uuid"], sd_b["name"]))

print(f"Elements: {len(elements)}, Bones: {len(bones)}")
assert len(elements) >= 80, f"Need 80+ elements, got {len(elements)}"
assert len(bones) >= 55, f"Need 55+ bones, got {len(bones)}"

# ═══════════════════════════════════════════════════════════════════════════════
# ANIMATIONS
# ═══════════════════════════════════════════════════════════════════════════════

uuid_to_name = {uuid: name for uuid, name in bone_registry}

def make_animators_for_all_bones(kf_map):
    animators = {}
    for uuid, name in bone_registry:
        kfs = kf_map.get(uuid, [])
        animators[uuid] = animator(uuid, name, kfs)
    return animators

# ─── spawn (once, 0.9s) ───────────────────────────────────────────────────────
def make_spawn_animators():
    kf_map = {}

    # Halo ring slabs: all appear simultaneously — scale 0 → 1 over 0.15s
    for i, uuid in enumerate(halo_ring_uuids):
        kf_map[uuid] = [
            kf(0.0,  0, 0, 0,  "position", "linear"),
            kf(0.0,  0,-10, 0, "position", "linear"),
            kf(0.15, 0, 0,  0, "position", "catmullrom"),
            kf(0.9,  0, 0,  0, "position", "catmullrom"),
            kf(0.0,  0, 0,  0, "rotation", "linear"),
            kf(0.05, 0,i*22.5, 0, "rotation","catmullrom"),
            kf(0.2,  0, 0,  0, "rotation", "catmullrom"),
            kf(0.9,  0, 0,  0, "rotation", "catmullrom"),
        ]

    # Orbital micro-shards fly in from above at 0.2s
    for i, uuid in enumerate(micro_uuids):
        angle = (i / 12.0) * 360
        kf_map[uuid] = [
            kf(0.0,  0, 5,  0, "position", "linear"),
            kf(0.2,  0, 5,  0, "position", "linear"),
            kf(0.35, 0, 0,  0, "position", "catmullrom"),
            kf(0.9,  0, 0,  0, "position", "catmullrom"),
            kf(0.0,  0, 0,  0, "rotation", "linear"),
            kf(0.2,  0,angle, 0,"rotation","catmullrom"),
            kf(0.4,  0, 0,  0, "rotation", "catmullrom"),
            kf(0.9,  0, 0,  0, "rotation", "catmullrom"),
        ]

    # Inner glow phases in at 0.25s
    kf_map[ig_bone["uuid"]] = [
        kf(0.0,  0, -0.5, 0, "position", "linear"),
        kf(0.25, 0, -0.5, 0, "position", "linear"),
        kf(0.4,  0,  0,   0, "position", "catmullrom"),
        kf(0.9,  0,  0,   0, "position", "catmullrom"),
        kf(0.0,  0,  0,   0, "rotation", "linear"),
        kf(0.25, 0, 45,   0, "rotation", "catmullrom"),
        kf(0.4,  0,  0,   0, "rotation", "catmullrom"),
        kf(0.9,  0,  0,   0, "rotation", "catmullrom"),
    ]

    # Fracture lines appear at 0.5s
    for i, uuid in enumerate(fracture_uuids):
        kf_map[uuid] = [
            kf(0.0,  0, 0, 0, "position", "linear"),
            kf(0.5,  0, 0, 0, "position", "linear"),
            kf(0.5,  0, 0.3, 0, "position", "catmullrom"),
            kf(0.65, 0, 0,   0, "position", "catmullrom"),
            kf(0.9,  0, 0,   0, "position", "catmullrom"),
            kf(0.0,  0, 0,   0, "rotation", "linear"),
            kf(0.5,  5, 0,   5, "rotation", "catmullrom"),
            kf(0.65, 0, 0,   0, "rotation", "catmullrom"),
            kf(0.9,  0, 0,   0, "rotation", "catmullrom"),
        ]

    # Secondary fracture lines appear at 0.5s + offset
    for i, uuid in enumerate(fracture_b_uuids):
        kf_map[uuid] = [
            kf(0.0,   0, 0, 0, "position", "linear"),
            kf(0.55,  0, 0, 0, "position", "linear"),
            kf(0.55,  0, 0.2, 0,"position","catmullrom"),
            kf(0.7,   0, 0, 0, "position", "catmullrom"),
            kf(0.9,   0, 0, 0, "position", "catmullrom"),
            kf(0.0,   0, 0, 0, "rotation", "linear"),
            kf(0.55,  3, 0, 3, "rotation", "catmullrom"),
            kf(0.7,   0, 0, 0, "rotation", "catmullrom"),
            kf(0.9,   0, 0, 0, "rotation", "catmullrom"),
        ]

    # Ground shadow appears at 0.3s
    kf_map[gs_bone["uuid"]] = [
        kf(0.0,  0, 0, 0, "position", "linear"),
        kf(0.3,  0, 0, 0, "position", "linear"),
        kf(0.45, 0, 0, 0, "position", "catmullrom"),
        kf(0.9,  0, 0, 0, "position", "catmullrom"),
        kf(0.0,  0, 0, 0, "rotation", "linear"),
        kf(0.3,  0,20, 0, "rotation", "catmullrom"),
        kf(0.5,  0, 0, 0, "rotation", "catmullrom"),
        kf(0.9,  0, 0, 0, "rotation", "catmullrom"),
    ]

    # Det shards spawn hidden (same Y position, wait for dissipate)
    for i, uuid in enumerate(det_shard_uuids):
        kf_map[uuid] = [
            kf(0.0, 0, 0, 0, "position", "catmullrom"),
            kf(0.9, 0, 0, 0, "position", "catmullrom"),
            kf(0.0, 0, 0, 0, "rotation", "catmullrom"),
            kf(0.3, 0,i*22.5, 0,"rotation","catmullrom"),
            kf(0.6, 0, 0, 0, "rotation", "catmullrom"),
            kf(0.9, 0, 0, 0, "rotation", "catmullrom"),
        ]

    # Outer ring details
    for i, uuid in enumerate(halo_outer_uuids):
        kf_map[uuid] = [
            kf(0.0,  0, -10, 0, "position", "linear"),
            kf(0.1,  0, -10, 0, "position", "linear"),
            kf(0.25, 0,  0,  0, "position", "catmullrom"),
            kf(0.9,  0,  0,  0, "position", "catmullrom"),
            kf(0.0,  0,  0,  0, "rotation", "linear"),
            kf(0.15, 0,i*120, 0,"rotation","catmullrom"),
            kf(0.3,  0,  0,  0, "rotation", "catmullrom"),
            kf(0.9,  0,  0,  0, "rotation", "catmullrom"),
        ]

    # Halo inner details
    for i, uuid in enumerate(halo_inner_uuids):
        kf_map[uuid] = [
            kf(0.0,  0, 0, 0, "position", "linear"),
            kf(0.25, 0, 0, 0, "position", "linear"),
            kf(0.35, 0, 0, 0, "position", "catmullrom"),
            kf(0.9,  0, 0, 0, "position", "catmullrom"),
            kf(0.0,  0, 0, 0, "rotation", "linear"),
            kf(0.25, 0,30, 0, "rotation", "catmullrom"),
            kf(0.4,  0, 0, 0, "rotation", "catmullrom"),
            kf(0.9,  0, 0, 0, "rotation", "catmullrom"),
        ]

    # Glow accents
    for i, uuid in enumerate(glow_accent_uuids):
        kf_map[uuid] = [
            kf(0.0,  0, -0.3, 0, "position", "linear"),
            kf(0.25, 0, -0.3, 0, "position", "linear"),
            kf(0.4,  0,  0,   0, "position", "catmullrom"),
            kf(0.9,  0,  0,   0, "position", "catmullrom"),
            kf(0.0,  0,  0,   0, "rotation", "linear"),
            kf(0.3,  0, 20,   0, "rotation", "catmullrom"),
            kf(0.45, 0,  0,   0, "rotation", "catmullrom"),
            kf(0.9,  0,  0,   0, "rotation", "catmullrom"),
        ]

    # Shadow details
    for i, uuid in enumerate(shadow_detail_uuids):
        kf_map[uuid] = [
            kf(0.0,  0, 0, 0, "position", "linear"),
            kf(0.35, 0, 0, 0, "position", "linear"),
            kf(0.5,  0, 0, 0, "position", "catmullrom"),
            kf(0.9,  0, 0, 0, "position", "catmullrom"),
            kf(0.0,  0, 0, 0, "rotation", "linear"),
            kf(0.35, 0,15, 0, "rotation", "catmullrom"),
            kf(0.5,  0, 0, 0, "rotation", "catmullrom"),
            kf(0.9,  0, 0, 0, "rotation", "catmullrom"),
        ]

    return make_animators_for_all_bones(kf_map)


# ─── idle (loop, 5.0s) ───────────────────────────────────────────────────────
def make_idle_animators():
    kf_map = {}

    # Halo slowly rotates Y — apply to each slab with staggered bob
    for i, uuid in enumerate(halo_ring_uuids):
        phase = i * (5.0 / 16.0)
        bob = 0.3
        kf_map[uuid] = [
            kf(0.0,           0, 0,   0, "position", "catmullrom"),
            kf(phase+0.625,   0, bob, 0, "position", "catmullrom"),
            kf(phase+1.25,    0, 0,   0, "position", "catmullrom"),
            kf(phase+1.875,   0,-bob, 0, "position", "catmullrom"),
            kf(phase+2.5,     0, 0,   0, "position", "catmullrom"),
            kf(5.0,           0, 0,   0, "position", "catmullrom"),
            # Y rotation drift
            kf(0.0,  0, 0,   0, "rotation", "catmullrom"),
            kf(1.25, 0, 5,   0, "rotation", "catmullrom"),
            kf(2.5,  0, 0,   0, "rotation", "catmullrom"),
            kf(3.75, 0,-5,   0, "rotation", "catmullrom"),
            kf(5.0,  0, 0,   0, "rotation", "catmullrom"),
        ]

    # Inner glow pulses
    kf_map[ig_bone["uuid"]] = [
        kf(0.0,  0, 0,    0, "position", "catmullrom"),
        kf(1.25, 0, 0.05, 0, "position", "catmullrom"),
        kf(2.5,  0, 0,    0, "position", "catmullrom"),
        kf(3.75, 0,-0.05, 0, "position", "catmullrom"),
        kf(5.0,  0, 0,    0, "position", "catmullrom"),
        kf(0.0,  0, 0, 0, "rotation", "catmullrom"),
        kf(2.5,  0, 5, 0, "rotation", "catmullrom"),
        kf(5.0,  0, 0, 0, "rotation", "catmullrom"),
    ]

    # Fracture lines pulse emissive (slight scale wobble via rotation)
    for i, uuid in enumerate(fracture_uuids):
        phase = i * 0.6
        kf_map[uuid] = [
            kf(0.0,          0, 0, 0,  "rotation", "catmullrom"),
            kf(phase+0.5,    1, 0, 0.5,"rotation","catmullrom"),
            kf(phase+1.0,    0, 0, 0,  "rotation", "catmullrom"),
            kf(phase+1.5,   -1, 0,-0.5,"rotation","catmullrom"),
            kf(phase+2.0,    0, 0, 0,  "rotation", "catmullrom"),
            kf(5.0,          0, 0, 0,  "rotation", "catmullrom"),
        ]

    for i, uuid in enumerate(fracture_b_uuids):
        phase = i * 0.55 + 0.2
        kf_map[uuid] = [
            kf(0.0,         0, 0, 0,  "rotation", "catmullrom"),
            kf(phase+0.55,  0.8,0,0.4,"rotation","catmullrom"),
            kf(phase+1.1,   0, 0, 0,  "rotation", "catmullrom"),
            kf(phase+1.65, -0.8,0,-0.4,"rotation","catmullrom"),
            kf(phase+2.2,   0, 0, 0,  "rotation", "catmullrom"),
            kf(5.0,         0, 0, 0,  "rotation", "catmullrom"),
        ]

    # Orbital shards orbit at different radii
    for i, uuid in enumerate(micro_uuids):
        orbit_speed = 5.0 / (1.5 + i * 0.1)
        kf_map[uuid] = [
            kf(0.0,   0, 0, 0, "rotation", "catmullrom"),
            kf(orbit_speed*0.25, 0, 90, 0, "rotation","catmullrom"),
            kf(orbit_speed*0.5,  0, 180,0, "rotation","catmullrom"),
            kf(orbit_speed*0.75, 0, 270,0, "rotation","catmullrom"),
            kf(orbit_speed,      0, 360,0, "rotation","catmullrom"),
            kf(5.0,  0, 0, 0, "rotation", "catmullrom"),
        ]

    # Ground shadow breathes
    kf_map[gs_bone["uuid"]] = [
        kf(0.0,  0, 0,    0, "position", "catmullrom"),
        kf(1.25, 0, 0.02, 0, "position", "catmullrom"),
        kf(2.5,  0, 0,    0, "position", "catmullrom"),
        kf(3.75, 0,-0.02, 0, "position", "catmullrom"),
        kf(5.0,  0, 0,    0, "position", "catmullrom"),
        kf(0.0,  0, 0, 0, "rotation", "catmullrom"),
        kf(2.5,  0, 3, 0, "rotation", "catmullrom"),
        kf(5.0,  0, 0, 0, "rotation", "catmullrom"),
    ]

    # Det shards idle wobble
    for i, uuid in enumerate(det_shard_uuids):
        phase = i * (5.0 / 16.0)
        kf_map[uuid] = [
            kf(0.0,         0, 0, 0, "rotation", "catmullrom"),
            kf(phase+0.625, 1, 0, 0.5,"rotation","catmullrom"),
            kf(phase+1.25,  0, 0, 0, "rotation", "catmullrom"),
            kf(phase+1.875,-1, 0,-0.5,"rotation","catmullrom"),
            kf(phase+2.5,   0, 0, 0, "rotation", "catmullrom"),
            kf(5.0,         0, 0, 0, "rotation", "catmullrom"),
        ]

    for i, uuid in enumerate(halo_outer_uuids):
        phase = i * 1.6
        kf_map[uuid] = [
            kf(0.0,         0, 0, 0,  "rotation", "catmullrom"),
            kf(phase+1.0,   0, 8, 0,  "rotation", "catmullrom"),
            kf(phase+2.0,   0, 0, 0,  "rotation", "catmullrom"),
            kf(phase+3.0,   0,-8, 0,  "rotation", "catmullrom"),
            kf(phase+4.0,   0, 0, 0,  "rotation", "catmullrom"),
            kf(5.0,         0, 0, 0,  "rotation", "catmullrom"),
        ]

    for i, uuid in enumerate(halo_inner_uuids):
        phase = i * 1.2
        kf_map[uuid] = [
            kf(0.0,         0, 0,     0, "position", "catmullrom"),
            kf(phase+0.6,   0, 0.04,  0, "position", "catmullrom"),
            kf(phase+1.2,   0, 0,     0, "position", "catmullrom"),
            kf(phase+1.8,   0,-0.04,  0, "position", "catmullrom"),
            kf(phase+2.4,   0, 0,     0, "position", "catmullrom"),
            kf(5.0,         0, 0,     0, "position", "catmullrom"),
        ]

    for i, uuid in enumerate(glow_accent_uuids):
        phase = i * 1.0
        kf_map[uuid] = [
            kf(0.0,         0, 0,    0, "position", "catmullrom"),
            kf(phase+0.5,   0, 0.06, 0, "position", "catmullrom"),
            kf(phase+1.0,   0, 0,    0, "position", "catmullrom"),
            kf(phase+1.5,   0,-0.06, 0, "position", "catmullrom"),
            kf(phase+2.0,   0, 0,    0, "position", "catmullrom"),
            kf(5.0,         0, 0,    0, "position", "catmullrom"),
        ]

    for i, uuid in enumerate(shadow_detail_uuids):
        phase = i * 1.3
        kf_map[uuid] = [
            kf(0.0,         0, 0, 0, "rotation", "catmullrom"),
            kf(phase+1.0,   0, 4, 0, "rotation", "catmullrom"),
            kf(phase+2.0,   0, 0, 0, "rotation", "catmullrom"),
            kf(phase+3.0,   0,-4, 0, "rotation", "catmullrom"),
            kf(phase+4.0,   0, 0, 0, "rotation", "catmullrom"),
            kf(5.0,         0, 0, 0, "rotation", "catmullrom"),
        ]

    return make_animators_for_all_bones(kf_map)


# ─── dissipate (once, 0.6s) ──────────────────────────────────────────────────
def make_dissipate_animators():
    kf_map = {}

    # Halo slabs expand radially outward then snap to 0 (via position blast)
    for i, uuid in enumerate(halo_ring_uuids):
        angle = (i / 16.0) * 2 * math.pi
        blast_x = math.sin(angle) * 4.0
        blast_z = math.cos(angle) * 4.0
        kf_map[uuid] = [
            kf(0.0,  0,   0,   0, "position", "catmullrom"),
            kf(0.15, blast_x, 0.5, blast_z, "position", "catmullrom"),
            kf(0.35, blast_x*1.5, 0, blast_z*1.5, "position", "catmullrom"),
            kf(0.5,  blast_x*2, -5, blast_z*2, "position", "linear"),
            kf(0.6,  blast_x*2, -5, blast_z*2, "position", "linear"),
            kf(0.0,  0, 0, 0, "rotation", "catmullrom"),
            kf(0.2,  0,i*22.5*2, 30, "rotation","catmullrom"),
            kf(0.4,  0, 0, 15, "rotation", "catmullrom"),
            kf(0.6,  0, 0, 0,  "rotation", "linear"),
        ]

    # Det shards blast outward simultaneously
    for i, uuid in enumerate(det_shard_uuids):
        angle = (i / 16.0) * 2 * math.pi
        blast_x = math.sin(angle) * 6.0
        blast_z = math.cos(angle) * 6.0
        kf_map[uuid] = [
            kf(0.0,  0,   0,   0, "position", "catmullrom"),
            kf(0.05, 0,   0,   0, "position", "catmullrom"),
            kf(0.2,  blast_x, 1.5, blast_z, "position","catmullrom"),
            kf(0.4,  blast_x*1.8, 0, blast_z*1.8,"position","catmullrom"),
            kf(0.6,  blast_x*2.5,-8, blast_z*2.5,"position","linear"),
            kf(0.0,  0, 0, 0, "rotation", "catmullrom"),
            kf(0.1,  0,i*22.5, 30,"rotation","catmullrom"),
            kf(0.4,  0, 0, 0, "rotation", "linear"),
            kf(0.6,  0, 0, 0, "rotation", "linear"),
        ]

    # Inner glow expands 3× then collapses
    kf_map[ig_bone["uuid"]] = [
        kf(0.0,  0, 0, 0, "position", "catmullrom"),
        kf(0.1,  0, 0.2, 0,"position","catmullrom"),
        kf(0.25, 0, 1.5, 0,"position","catmullrom"),
        kf(0.35, 0, 2.0, 0,"position","catmullrom"),
        kf(0.45, 0, -8, 0, "position","linear"),
        kf(0.6,  0, -8, 0, "position","linear"),
        kf(0.0,  0, 0, 0, "rotation", "catmullrom"),
        kf(0.2,  0,90, 0, "rotation", "catmullrom"),
        kf(0.4,  0,180,0, "rotation", "catmullrom"),
        kf(0.6,  0, 0, 0, "rotation", "linear"),
    ]

    # Fracture lines: rapid expand then snap
    for i, uuid in enumerate(fracture_uuids):
        angle = math.radians(frac_angles[i])
        fx_b = math.cos(angle) * 2.0
        fz_b = math.sin(angle) * 2.0
        kf_map[uuid] = [
            kf(0.0,  0, 0, 0, "position", "catmullrom"),
            kf(0.1,  fx_b, 0.3, fz_b, "position","catmullrom"),
            kf(0.25, fx_b*1.5, 0, fz_b*1.5,"position","catmullrom"),
            kf(0.35, fx_b*2,-5, fz_b*2,"position","linear"),
            kf(0.6,  fx_b*2,-5, fz_b*2,"position","linear"),
            kf(0.0,  0, 0, 0, "rotation", "catmullrom"),
            kf(0.15, 0, 45, 0,"rotation","catmullrom"),
            kf(0.3,  0, 0, 0, "rotation","linear"),
            kf(0.6,  0, 0, 0, "rotation","linear"),
        ]

    for i, uuid in enumerate(fracture_b_uuids):
        angle = math.radians(frac_angles[i]+5)
        fx_b = math.cos(angle) * 1.5
        fz_b = math.sin(angle) * 1.5
        kf_map[uuid] = [
            kf(0.0,  0, 0, 0, "position", "catmullrom"),
            kf(0.12, fx_b, 0.2, fz_b,"position","catmullrom"),
            kf(0.28, fx_b*1.5,-5,fz_b*1.5,"position","linear"),
            kf(0.6,  fx_b*1.5,-5,fz_b*1.5,"position","linear"),
            kf(0.0,  0, 0, 0, "rotation", "catmullrom"),
            kf(0.12, 0,30, 0, "rotation","catmullrom"),
            kf(0.3,  0, 0, 0, "rotation","linear"),
            kf(0.6,  0, 0, 0, "rotation","linear"),
        ]

    # Orbital shards follow tangent momentum
    for i, uuid in enumerate(micro_uuids):
        angle = (i / 12.0) * 2 * math.pi + math.pi/2
        tang_x = math.cos(angle) * 5
        tang_z = math.sin(angle) * 5
        kf_map[uuid] = [
            kf(0.0,  0, 0, 0, "position", "catmullrom"),
            kf(0.1,  tang_x*0.3, 0.5, tang_z*0.3,"position","catmullrom"),
            kf(0.3,  tang_x*0.8, 0, tang_z*0.8,"position","catmullrom"),
            kf(0.5,  tang_x,-5, tang_z,"position","linear"),
            kf(0.6,  tang_x,-5, tang_z,"position","linear"),
            kf(0.0,  0, 0, 0, "rotation", "catmullrom"),
            kf(0.2,  0,i*30, 0,"rotation","catmullrom"),
            kf(0.4,  0, 0, 0, "rotation","linear"),
            kf(0.6,  0, 0, 0, "rotation","linear"),
        ]

    # Ground shadow rapidly expands then snaps
    kf_map[gs_bone["uuid"]] = [
        kf(0.0,  0, 0, 0, "position", "catmullrom"),
        kf(0.15, 0, 0, 0, "position", "catmullrom"),
        kf(0.3,  0,0.1,0, "position", "catmullrom"),
        kf(0.45, 0,-5, 0, "position", "linear"),
        kf(0.6,  0,-5, 0, "position", "linear"),
        kf(0.0,  0, 0, 0, "rotation", "catmullrom"),
        kf(0.3,  0,45, 0, "rotation", "catmullrom"),
        kf(0.45, 0, 0, 0, "rotation","linear"),
        kf(0.6,  0, 0, 0, "rotation","linear"),
    ]

    # Outer ring details blast
    for i, uuid in enumerate(halo_outer_uuids):
        angle = (i / 3.0) * 2 * math.pi
        bx = math.sin(angle) * 5
        bz = math.cos(angle) * 5
        kf_map[uuid] = [
            kf(0.0,  0, 0, 0, "position", "catmullrom"),
            kf(0.1,  bx*0.5, 0.5, bz*0.5,"position","catmullrom"),
            kf(0.3,  bx, 0, bz,"position","catmullrom"),
            kf(0.5,  bx*2,-5, bz*2,"position","linear"),
            kf(0.6,  bx*2,-5, bz*2,"position","linear"),
            kf(0.0,  0, 0, 0, "rotation","catmullrom"),
            kf(0.2,  0,i*120, 0,"rotation","catmullrom"),
            kf(0.4,  0, 0, 0, "rotation","linear"),
            kf(0.6,  0, 0, 0, "rotation","linear"),
        ]

    for i, uuid in enumerate(halo_inner_uuids):
        kf_map[uuid] = [
            kf(0.0,  0, 0, 0, "position","catmullrom"),
            kf(0.15, 0, 0.5, 0,"position","catmullrom"),
            kf(0.3,  0,-5, 0, "position","linear"),
            kf(0.6,  0,-5, 0, "position","linear"),
            kf(0.0,  0, 0, 0, "rotation","catmullrom"),
            kf(0.15, 0,45, 0, "rotation","catmullrom"),
            kf(0.3,  0, 0, 0, "rotation","linear"),
            kf(0.6,  0, 0, 0, "rotation","linear"),
        ]

    for i, uuid in enumerate(glow_accent_uuids):
        kf_map[uuid] = [
            kf(0.0,  0, 0, 0, "position","catmullrom"),
            kf(0.1,  0,0.3,0, "position","catmullrom"),
            kf(0.25, 0, 1, 0, "position","catmullrom"),
            kf(0.4,  0,-5, 0, "position","linear"),
            kf(0.6,  0,-5, 0, "position","linear"),
            kf(0.0,  0, 0, 0, "rotation","catmullrom"),
            kf(0.2,  0,30, 0, "rotation","catmullrom"),
            kf(0.4,  0, 0, 0, "rotation","linear"),
            kf(0.6,  0, 0, 0, "rotation","linear"),
        ]

    for i, uuid in enumerate(shadow_detail_uuids):
        kf_map[uuid] = [
            kf(0.0,  0, 0, 0, "position","catmullrom"),
            kf(0.2,  0, 0, 0, "position","catmullrom"),
            kf(0.4,  0,-3, 0, "position","linear"),
            kf(0.6,  0,-3, 0, "position","linear"),
            kf(0.0,  0, 0, 0, "rotation","catmullrom"),
            kf(0.2,  0,20, 0, "rotation","catmullrom"),
            kf(0.4,  0, 0, 0, "rotation","linear"),
            kf(0.6,  0, 0, 0, "rotation","linear"),
        ]

    return make_animators_for_all_bones(kf_map)


# ─── Build animation objects ──────────────────────────────────────────────────
spawn_anims = make_spawn_animators()
idle_anims  = make_idle_animators()
diss_anims  = make_dissipate_animators()

animations = [
    {
        "uuid": "a001",
        "name": "animation.fallen_halo_burst.spawn",
        "loop": "once",
        "length": 0.9,
        "snapping": 24,
        "selected": False,
        "saved": False,
        "animators": spawn_anims
    },
    {
        "uuid": "a002",
        "name": "animation.fallen_halo_burst.idle",
        "loop": "loop",
        "length": 5.0,
        "snapping": 24,
        "selected": False,
        "saved": False,
        "animators": idle_anims
    },
    {
        "uuid": "a003",
        "name": "animation.fallen_halo_burst.dissipate",
        "loop": "once",
        "length": 0.6,
        "snapping": 24,
        "selected": False,
        "saved": False,
        "animators": diss_anims
    },
]

# ─── Outliner ────────────────────────────────────────────────────────────────
outliner = [b["uuid"] for b in bones]

# ─── Textures ────────────────────────────────────────────────────────────────
textures = [
    {
        "id": "0",
        "name": "fallen_halo_burst_tex",
        "relative_path": "../textures/fallen_halo_burst_tex.png",
        "folder": "devilsdream",
        "namespace": "",
        "visible": True,
        "mode": "bitmap",
        "saved": False,
        "uuid": "t001",
        "source": f"data:image/png;base64,{b64_0}"
    },
    {
        "id": "1",
        "name": "fallen_halo_burst_tex1",
        "relative_path": "../textures/fallen_halo_burst_tex1.png",
        "folder": "devilsdream",
        "namespace": "",
        "visible": True,
        "mode": "bitmap",
        "saved": False,
        "uuid": "t002",
        "source": f"data:image/png;base64,{b64_1}"
    },
]

# ─── Assemble bbmodel ─────────────────────────────────────────────────────────
bbmodel = {
    "meta": {
        "format_version": "4.10",
        "model_format": "free",
        "box_uv": False
    },
    "name": "fallen_halo_burst",
    "geometry": "geometry.fallen_halo_burst",
    "resolution": {"width": 64, "height": 64},
    "elements": elements,
    "outliner": outliner,
    "textures": textures,
    "animations": animations
}

out_path = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/fallen_halo_burst.bbmodel"
with open(out_path, "w", encoding="utf-8") as f:
    json.dump(bbmodel, f, separators=(',', ':'))

size = os.path.getsize(out_path)
print(f"File size: {size} bytes ({size/1024:.1f} KB)")
print(f"Elements: {len(elements)}, Bones: {len(bones)}")
if size < 300000:
    print(f"WARNING: File too small! Need 300000+, got {size}")
else:
    print("SIZE OK")
