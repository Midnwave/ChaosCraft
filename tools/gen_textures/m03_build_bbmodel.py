"""
Build devils_spine_array.bbmodel
85 elements, 55 bones, 3 animations, 6+ keyframes per bone per animation.
"""
import json, math, os

b64_0 = open("D:/CC/ChaosCraft/tools/gen_textures/m03_b64.txt").read().strip()
b64_1 = open("D:/CC/ChaosCraft/tools/gen_textures/m03_tex1_b64.txt").read().strip()

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
            "north": {"uv": [2,2,30,30], "texture": 0},
            "east":  {"uv": [34,2,62,30], "texture": 0},
            "south": {"uv": [2,2,30,30], "texture": 0},
            "west":  {"uv": [34,2,62,30], "texture": 0},
            "up":    {"uv": [2,34,30,62], "texture": 0},
            "down":  {"uv": [34,34,62,62], "texture": 0},
        }
    else:
        return {
            "north": {"uv": [2,2,30,30], "texture": 1},
            "east":  {"uv": [34,2,62,30], "texture": 1},
            "south": {"uv": [2,2,30,30], "texture": 1},
            "west":  {"uv": [34,2,62,30], "texture": 1},
            "up":    {"uv": [2,34,30,62], "texture": 1},
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
    return {
        "name": bone_name, "type": "bone",
        "keyframes": keyframes
    }

# ═══════════════════════════════════════════════════════════════════════════════
# GEOMETRY CONSTRUCTION
# ═══════════════════════════════════════════════════════════════════════════════

elements = []
bones    = []
# map bone_uuid → (bone_name) for animations
bone_registry = []  # list of (uuid, name)

# Vertebra positions: (z_center, x_offset) — 7 vertebrae
vpos = [
    (-9.0, 0.0),
    (-6.0, 0.4),
    (-3.0, 0.8),
    ( 0.0, 1.0),
    ( 3.0, 0.8),
    ( 6.0, 0.2),
    ( 9.0,-0.4),
]
# Varying sizes: (bw, bh, bd) body width/height/depth
vsizes = [
    (1.8, 2.0, 1.5),
    (1.9, 2.2, 1.6),
    (2.0, 2.3, 1.7),
    (2.1, 2.4, 1.8),
    (2.0, 2.2, 1.7),
    (1.9, 2.1, 1.6),
    (1.7, 1.9, 1.5),
]

# Track bone UUIDs for animation use
vert_body_uuids    = []
vert_left_uuids    = []
vert_right_uuids   = []
vert_process_uuids = []
vert_inner_uuids   = []
disc_uuids         = []
rupture_uuids      = []
nerve_uuids        = []
crack_uuids        = []
disc_extra_uuids   = []

# ── 7 Vertebrae: each = body + left flange + right flange + spinous process + inner detail
for i, ((z, xo), (bw, bh, bd)) in enumerate(zip(vpos, vsizes)):
    n = i + 1
    y_base = 0.0  # erupts from ground

    # Body cube
    body_e = elem(f"vert_{n}_body_e",
                  [xo - bw/2, y_base, z - bd/2],
                  [xo + bw/2, y_base + bh, z + bd/2], tex=0)
    elements.append(body_e)
    body_b = bone(f"vert_{n}_body", [xo, y_base + bh/2, z], [body_e["uuid"]])
    bones.append(body_b)
    vert_body_uuids.append(body_b["uuid"])
    bone_registry.append((body_b["uuid"], body_b["name"]))

    # Left transverse process flange
    flange_w = bw * 1.6
    flange_h = 0.4
    flange_d = bd * 0.7
    left_e = elem(f"vert_{n}_left_e",
                  [xo - bw/2 - flange_w, y_base + bh*0.5, z - flange_d/2],
                  [xo - bw/2,             y_base + bh*0.5 + flange_h, z + flange_d/2], tex=0)
    elements.append(left_e)
    left_b = bone(f"vert_{n}_left", [xo - bw/2, y_base + bh*0.5, z], [left_e["uuid"]])
    bones.append(left_b)
    vert_left_uuids.append(left_b["uuid"])
    bone_registry.append((left_b["uuid"], left_b["name"]))

    # Right transverse process flange (mirror)
    right_e = elem(f"vert_{n}_right_e",
                   [xo + bw/2,              y_base + bh*0.5, z - flange_d/2],
                   [xo + bw/2 + flange_w,  y_base + bh*0.5 + flange_h, z + flange_d/2], tex=0)
    elements.append(right_e)
    right_b = bone(f"vert_{n}_right", [xo + bw/2, y_base + bh*0.5, z], [right_e["uuid"]])
    bones.append(right_b)
    vert_right_uuids.append(right_b["uuid"])
    bone_registry.append((right_b["uuid"], right_b["name"]))

    # Spinous process (upward-pointing narrow slab)
    sp_w = 0.5
    sp_h = 2.2
    sp_d = 0.4
    proc_e = elem(f"vert_{n}_process_e",
                  [xo - sp_w/2, y_base + bh, z - sp_d/2],
                  [xo + sp_w/2, y_base + bh + sp_h, z + sp_d/2], tex=0)
    elements.append(proc_e)
    proc_b = bone(f"vert_{n}_process", [xo, y_base + bh, z], [proc_e["uuid"]])
    bones.append(proc_b)
    vert_process_uuids.append(proc_b["uuid"])
    bone_registry.append((proc_b["uuid"], proc_b["name"]))

    # Inner detail cube (smaller inset)
    iw = bw * 0.55
    ih = bh * 0.6
    id_ = bd * 0.55
    inner_e = elem(f"vert_{n}_inner_e",
                   [xo - iw/2, y_base + bh*0.2, z - id_/2],
                   [xo + iw/2, y_base + bh*0.2 + ih, z + id_/2], tex=0)
    elements.append(inner_e)
    inner_b = bone(f"vert_{n}_inner", [xo, y_base + bh*0.5, z], [inner_e["uuid"]])
    bones.append(inner_b)
    vert_inner_uuids.append(inner_b["uuid"])
    bone_registry.append((inner_b["uuid"], inner_b["name"]))

# ── 6 Intervertebral discs (between each pair of vertebrae, tex1)
for i in range(6):
    z1 = vpos[i][0];   x1 = vpos[i][1]
    z2 = vpos[i+1][0]; x2 = vpos[i+1][1]
    dz = (z1 + z2) / 2.0
    dx = (x1 + x2) / 2.0
    dy = 0.9  # mid-height of vertebra
    dw = 1.5; dh = 0.35; dd = 1.5
    disc_e = elem(f"disc_{i+1}_e",
                  [dx - dw/2, dy, dz - dd/2],
                  [dx + dw/2, dy + dh, dz + dd/2], tex=1)
    elements.append(disc_e)
    disc_b = bone(f"disc_{i+1}", [dx, dy, dz], [disc_e["uuid"]])
    bones.append(disc_b)
    disc_uuids.append(disc_b["uuid"])
    bone_registry.append((disc_b["uuid"], disc_b["name"]))

# ── 6 Ground rupture slabs radiating from spine base
rupture_defs = [
    ( 3.5, 0.0, -5.0,  45),
    (-2.5, 0.0, -7.0, -30),
    ( 4.0, 0.0,  2.0,  60),
    (-3.0, 0.0,  5.0, -45),
    ( 2.0, 0.0,  8.0,  20),
    (-4.0, 0.0,  0.0, -60),
]
for i, (rx, ry, rz, rot_y) in enumerate(rupture_defs):
    rw = 2.2 + i*0.15; rh = 0.15; rd = 0.8
    rup_e = elem(f"rupture_{i+1}_e",
                 [rx - rw/2, ry, rz - rd/2],
                 [rx + rw/2, ry + rh, rz + rd/2], tex=0)
    elements.append(rup_e)
    rup_b = bone(f"rupture_{i+1}", [rx, ry, rz], [rup_e["uuid"]], rot=[0, rot_y, 0])
    bones.append(rup_b)
    rupture_uuids.append(rup_b["uuid"])
    bone_registry.append((rup_b["uuid"], rup_b["name"]))

# ── 4 Nerve/tendon strands running parallel to spine (tex1)
nerve_defs = [
    # (x_offset, height, z_center)
    ( 0.3, 0.5,  0.0),
    (-0.2, 1.0,  0.2),
    ( 0.5, 1.5, -0.1),
    (-0.4, 2.0,  0.15),
]
for i, (nx, ny, nz_off) in enumerate(nerve_defs):
    nw = 14.0; nh = 0.15; nd = 0.3
    nerve_e = elem(f"nerve_{i+1}_e",
                   [nx - nw/2, ny, nz_off - nd/2],
                   [nx + nw/2, ny + nh, nz_off + nd/2], tex=1)
    elements.append(nerve_e)
    nerve_b = bone(f"nerve_{i+1}", [nx, ny, nz_off], [nerve_e["uuid"]])
    bones.append(nerve_b)
    nerve_uuids.append(nerve_b["uuid"])
    bone_registry.append((nerve_b["uuid"], nerve_b["name"]))

# ── 8 Crack accent pieces around spine base
crack_defs = [
    (-2.0, 0.0,  1.0,  15, 1.5, 0.1, 0.5),
    ( 3.5, 0.0, -2.0, -20, 1.8, 0.1, 0.4),
    (-1.5, 0.0, -4.0,  35, 1.3, 0.1, 0.6),
    ( 2.5, 0.0,  4.0, -10, 2.0, 0.1, 0.3),
    (-3.0, 0.0,  7.0,  50, 1.6, 0.1, 0.5),
    ( 1.5, 0.0,  6.0, -40, 1.4, 0.1, 0.7),
    (-2.5, 0.0, -7.5,  25, 1.9, 0.1, 0.4),
    ( 3.0, 0.0,  0.5, -55, 1.7, 0.1, 0.6),
]
for i, (cx, cy, cz, cr, cw, ch, cd) in enumerate(crack_defs):
    crack_e = elem(f"crack_accent_{i+1}_e",
                   [cx - cw/2, cy, cz - cd/2],
                   [cx + cw/2, cy + ch, cz + cd/2], tex=0)
    elements.append(crack_e)
    crack_b = bone(f"crack_accent_{i+1}", [cx, cy, cz], [crack_e["uuid"]], rot=[0, cr, 0])
    bones.append(crack_b)
    crack_uuids.append(crack_b["uuid"])
    bone_registry.append((crack_b["uuid"], crack_b["name"]))

# ── 3 Extra disc highlight bones (tex1)
extra_disc_defs = [(-3.0, 0.9, 1.5, 0.3, 1.5), (0.0, 0.9, 1.5, 0.3, 1.5), (6.0, 0.9, 1.5, 0.3, 1.5)]
for i, (edx, edy, edw, edh, edd) in enumerate(extra_disc_defs):
    # Use the nearest z
    ez = vpos[i*2][0] + 1.5
    ed_e = elem(f"disc_extra_{i+1}_e",
                [edx - edw/2, edy, ez - edd/2],
                [edx + edw/2, edy + edh, ez + edd/2], tex=1)
    elements.append(ed_e)
    ed_b = bone(f"disc_extra_{i+1}", [edx, edy, ez], [ed_e["uuid"]])
    bones.append(ed_b)
    disc_extra_uuids.append(ed_b["uuid"])
    bone_registry.append((ed_b["uuid"], ed_b["name"]))

# ── 23 Extra accent pieces (shard details) — 14 more to hit 85 elements total
accent_defs = [
    ( 1.0, 0.3, -8.0,  0.4, 0.6, 0.2, 0),
    (-1.2, 0.5, -5.5,  0.5, 0.8, 0.2, 0),
    ( 0.5, 0.8, -2.5,  0.3, 0.5, 0.2, 0),
    (-0.8, 1.2,  0.5,  0.6, 0.7, 0.25,0),
    ( 1.5, 0.4,  3.5,  0.4, 0.6, 0.2, 0),
    (-1.0, 0.6,  6.0,  0.5, 0.9, 0.2, 0),
    ( 0.7, 1.0,  8.5,  0.3, 0.5, 0.2, 0),
    (-2.0, 0.2, -1.0,  0.8, 0.4, 0.3, 0),
    ( 2.5, 0.3,  2.0,  0.7, 0.5, 0.25,0),
    # 14 additional accents
    ( 0.2, 0.4, -9.5,  0.5, 0.7, 0.2, 0),
    (-1.5, 0.3, -6.8,  0.4, 0.5, 0.2, 0),
    ( 1.8, 0.5, -4.0,  0.6, 0.8, 0.25,0),
    (-0.3, 0.9, -1.5,  0.3, 0.6, 0.2, 0),
    ( 2.2, 0.2,  1.0,  0.5, 0.4, 0.2, 0),
    (-1.7, 0.7,  4.5,  0.4, 0.7, 0.2, 0),
    ( 0.9, 1.1,  7.0,  0.5, 0.9, 0.2, 0),
    (-2.3, 0.3,  9.5,  0.6, 0.5, 0.3, 0),
    ( 3.2, 0.4, -3.0,  0.4, 0.6, 0.2, 0),
    (-0.5, 0.6,  0.0,  0.7, 0.8, 0.25,0),
    ( 1.1, 0.2, -7.5,  0.3, 0.4, 0.2, 0),
    (-2.8, 0.8,  3.0,  0.5, 0.7, 0.2, 0),
    ( 0.4, 1.3,  5.5,  0.4, 0.5, 0.2, 0),
    (-1.8, 0.1,  8.0,  0.6, 0.6, 0.25,0),
]
extra_accent_uuids = []
for i, (ax, ay, az, aw, ah, ad, _) in enumerate(accent_defs):
    acc_e = elem(f"accent_{i+1}_e",
                 [ax - aw/2, ay, az - ad/2],
                 [ax + aw/2, ay + ah, az + ad/2], tex=0)
    elements.append(acc_e)
    acc_b = bone(f"accent_{i+1}", [ax, ay, az], [acc_e["uuid"]])
    bones.append(acc_b)
    extra_accent_uuids.append(acc_b["uuid"])
    bone_registry.append((acc_b["uuid"], acc_b["name"]))

print(f"Elements: {len(elements)}")
print(f"Bones: {len(bones)}")
assert len(elements) >= 80, f"Need 80+ elements, got {len(elements)}"
assert len(bones) >= 55, f"Need 55+ bones, got {len(bones)}"

# ═══════════════════════════════════════════════════════════════════════════════
# ANIMATIONS
# ═══════════════════════════════════════════════════════════════════════════════
# Map uuid → name for quick lookup
uuid_to_name = {uuid: name for uuid, name in bone_registry}

def make_animators_for_all_bones(bone_uuid_kf_map):
    """bone_uuid_kf_map: {uuid: [keyframes]}"""
    animators = {}
    for uuid, name in bone_registry:
        kfs = bone_uuid_kf_map.get(uuid, [])
        animators[uuid] = animator(uuid, name, kfs)
    return animators

# ─── spawn animation (once, 1.3s) ────────────────────────────────────────────
def make_spawn_animators():
    kf_map = {}

    # Vertebra bodies: erupt from ground sequentially
    for i, uuid in enumerate(vert_body_uuids):
        t_start = 0.2 + i * 0.12
        t_end   = t_start + 0.18
        kf_map[uuid] = [
            kf(0.0,       0, -12, 0, "position", "linear"),
            kf(t_start,   0, -12, 0, "position", "linear"),
            kf(t_end,     0,  0,  0, "position", "catmullrom"),
            kf(1.3,       0,  0,  0, "position", "catmullrom"),
            # rotation keyframes
            kf(0.0,       0,  0,  0, "rotation", "linear"),
            kf(t_start,   2,  0,  1, "rotation", "catmullrom"),
            kf(t_end,     0,  0,  0, "rotation", "catmullrom"),
            kf(1.3,       0,  0,  0, "rotation", "catmullrom"),
        ]

    # Flanges deploy 0.06s after body
    for i, (lu, ru) in enumerate(zip(vert_left_uuids, vert_right_uuids)):
        t_body  = 0.2 + i * 0.12
        t_start = t_body + 0.06
        t_end   = t_start + 0.12
        for uuid, sign in [(lu, 1), (ru, -1)]:
            kf_map[uuid] = [
                kf(0.0,     0, 0, 0, "position", "linear"),
                kf(t_start, 0, -3, 0, "position", "linear"),
                kf(t_end,   0,  0, 0, "position", "catmullrom"),
                kf(1.3,     0,  0, 0, "position", "catmullrom"),
                kf(0.0,     0, 0, 0, "rotation", "linear"),
                kf(t_start, 0, 0, sign*30, "rotation", "catmullrom"),
                kf(t_end,   0, 0, 0, "rotation", "catmullrom"),
                kf(1.3,     0, 0, 0, "rotation", "catmullrom"),
            ]

    # Spinous processes shoot up 0.08s after flanges
    for i, uuid in enumerate(vert_process_uuids):
        t_body  = 0.2 + i * 0.12
        t_start = t_body + 0.14
        t_end   = t_start + 0.14
        kf_map[uuid] = [
            kf(0.0,     0, 0, 0, "position", "linear"),
            kf(t_start, 0, -4, 0, "position", "linear"),
            kf(t_end,   0,  0, 0, "position", "catmullrom"),
            kf(1.3,     0,  0, 0, "position", "catmullrom"),
            kf(0.0,     0, 0, 0, "rotation", "linear"),
            kf(t_start, 8, 0, 0, "rotation", "catmullrom"),
            kf(t_end,   0, 0, 0, "rotation", "catmullrom"),
            kf(1.3,     0, 0, 0, "rotation", "catmullrom"),
        ]

    # Inner details follow body
    for i, uuid in enumerate(vert_inner_uuids):
        t_body = 0.2 + i * 0.12
        t_end  = t_body + 0.2
        kf_map[uuid] = [
            kf(0.0,    0, -12, 0, "position", "linear"),
            kf(t_body, 0, -12, 0, "position", "linear"),
            kf(t_end,  0,   0, 0, "position", "catmullrom"),
            kf(1.3,    0,   0, 0, "position", "catmullrom"),
            kf(0.0,    0,   0, 0, "rotation", "linear"),
            kf(t_body, 3,   0, 0, "rotation", "catmullrom"),
            kf(t_end,  0,   0, 0, "rotation", "catmullrom"),
            kf(1.3,    0,   0, 0, "rotation", "catmullrom"),
        ]

    # Discs appear as neighbours arrive
    for i, uuid in enumerate(disc_uuids):
        t_appear = 0.2 + (i + 0.5) * 0.12 + 0.1
        t_end    = t_appear + 0.1
        kf_map[uuid] = [
            kf(0.0,      0, -12, 0, "position", "linear"),
            kf(t_appear, 0, -12, 0, "position", "linear"),
            kf(t_end,    0,   0, 0, "position", "catmullrom"),
            kf(1.3,      0,   0, 0, "position", "catmullrom"),
            kf(0.0,      0,   0, 0, "rotation", "linear"),
            kf(t_appear, 0,   5, 0, "rotation", "catmullrom"),
            kf(t_end,    0,   0, 0, "rotation", "catmullrom"),
            kf(1.3,      0,   0, 0, "rotation", "catmullrom"),
        ]

    # Rupture slabs radiate at 0.25s
    for i, uuid in enumerate(rupture_uuids):
        t = 0.25
        kf_map[uuid] = [
            kf(0.0,  0, 0, 0, "position", "linear"),
            kf(t,    0, 0, 0, "position", "linear"),
            kf(t+0.1, 0, 0.3, 0, "position", "catmullrom"),
            kf(t+0.2, 0, 0.1, 0, "position", "catmullrom"),
            kf(1.3,   0, 0, 0, "position", "catmullrom"),
            kf(0.0,   0, 0, 0, "rotation", "linear"),
            kf(t,     5, 0, 5, "rotation", "catmullrom"),
            kf(t+0.2, 0, 0, 0, "rotation", "catmullrom"),
            kf(1.3,   0, 0, 0, "rotation", "catmullrom"),
        ]

    # Nerve strands materialise last at 0.9s
    for i, uuid in enumerate(nerve_uuids):
        kf_map[uuid] = [
            kf(0.0, 0, -0.5, 0, "position", "linear"),
            kf(0.9, 0, -0.5, 0, "position", "linear"),
            kf(1.1, 0,  0,   0, "position", "catmullrom"),
            kf(1.3, 0,  0,   0, "position", "catmullrom"),
            kf(0.0, 0,  0,   0, "rotation", "linear"),
            kf(0.9, 5,  0,   5, "rotation", "catmullrom"),
            kf(1.1, 0,  0,   0, "rotation", "catmullrom"),
            kf(1.3, 0,  0,   0, "rotation", "catmullrom"),
        ]

    # Crack accents erupt with rupture slabs
    for i, uuid in enumerate(crack_uuids):
        t = 0.25 + i * 0.02
        kf_map[uuid] = [
            kf(0.0,   0, -0.8, 0, "position", "linear"),
            kf(t,     0, -0.8, 0, "position", "linear"),
            kf(t+0.1, 0,  0,   0, "position", "catmullrom"),
            kf(1.3,   0,  0,   0, "position", "catmullrom"),
            kf(0.0,   0,  0,   0, "rotation", "linear"),
            kf(t,     3,  0,   3, "rotation", "catmullrom"),
            kf(t+0.1, 0,  0,   0, "rotation", "catmullrom"),
            kf(1.3,   0,  0,   0, "rotation", "catmullrom"),
        ]

    # Extra disc highlights
    for i, uuid in enumerate(disc_extra_uuids):
        t = 0.6 + i * 0.05
        kf_map[uuid] = [
            kf(0.0, 0, -12, 0, "position", "linear"),
            kf(t,   0, -12, 0, "position", "linear"),
            kf(t+0.1, 0, 0, 0, "position", "catmullrom"),
            kf(1.3, 0, 0, 0, "position", "catmullrom"),
            kf(0.0, 0, 0, 0, "rotation", "linear"),
            kf(t,   0, 3, 0, "rotation", "catmullrom"),
            kf(t+0.1, 0, 0, 0, "rotation", "catmullrom"),
            kf(1.3, 0, 0, 0, "rotation", "catmullrom"),
        ]

    # Extra accent pieces
    for i, uuid in enumerate(extra_accent_uuids):
        t = 0.25 + i * 0.03
        kf_map[uuid] = [
            kf(0.0,   0, -1, 0, "position", "linear"),
            kf(t,     0, -1, 0, "position", "linear"),
            kf(t+0.1, 0,  0, 0, "position", "catmullrom"),
            kf(1.3,   0,  0, 0, "position", "catmullrom"),
            kf(0.0,   0,  0, 0, "rotation", "linear"),
            kf(t,     0,  8, 0, "rotation", "catmullrom"),
            kf(t+0.1, 0,  0, 0, "rotation", "catmullrom"),
            kf(1.3,   0,  0, 0, "rotation", "catmullrom"),
        ]

    return make_animators_for_all_bones(kf_map)


# ─── idle animation (loop, 7.0s) ─────────────────────────────────────────────
def make_idle_animators():
    import math
    kf_map = {}

    # Each vertebra independently rocks slightly staggered
    for i, uuid in enumerate(vert_body_uuids):
        phase = i * (7.0 / 7.0)  # staggered phase
        kf_map[uuid] = [
            kf(0.0,            0, 0, 0,    "rotation", "catmullrom"),
            kf(phase + 0.875,  1.5,0, 0.5, "rotation", "catmullrom"),
            kf(phase + 1.75,   0, 0, 0,    "rotation", "catmullrom"),
            kf(phase + 2.625, -1.5,0,-0.5, "rotation", "catmullrom"),
            kf(phase + 3.5,    0, 0, 0,    "rotation", "catmullrom"),
            kf(7.0,            0, 0, 0,    "rotation", "catmullrom"),
        ]

    # Flanges flex ±2°
    for i, (lu, ru) in enumerate(zip(vert_left_uuids, vert_right_uuids)):
        phase = i * (7.0 / 7.0) + 0.3
        for uuid, sign in [(lu, 1), (ru, -1)]:
            kf_map[uuid] = [
                kf(0.0,            0, 0, 0,       "rotation", "catmullrom"),
                kf(phase + 0.875,  0, 0, sign*2,  "rotation", "catmullrom"),
                kf(phase + 1.75,   0, 0, 0,       "rotation", "catmullrom"),
                kf(phase + 2.625,  0, 0, -sign*2, "rotation", "catmullrom"),
                kf(phase + 3.5,    0, 0, 0,       "rotation", "catmullrom"),
                kf(7.0,            0, 0, 0,       "rotation", "catmullrom"),
            ]

    # Spinous processes wobble ±3°
    for i, uuid in enumerate(vert_process_uuids):
        phase = i * (7.0 / 7.0) + 0.5
        kf_map[uuid] = [
            kf(0.0,           0, 0, 0,  "rotation", "catmullrom"),
            kf(phase + 0.875, 3, 0, 1,  "rotation", "catmullrom"),
            kf(phase + 1.75,  0, 0, 0,  "rotation", "catmullrom"),
            kf(phase + 2.625,-3, 0,-1,  "rotation", "catmullrom"),
            kf(phase + 3.5,   0, 0, 0,  "rotation", "catmullrom"),
            kf(7.0,           0, 0, 0,  "rotation", "catmullrom"),
        ]

    # Inner details follow body subtly
    for i, uuid in enumerate(vert_inner_uuids):
        phase = i * (7.0 / 7.0) + 0.2
        kf_map[uuid] = [
            kf(0.0,           0, 0, 0,   "rotation", "catmullrom"),
            kf(phase + 0.9,   1, 0, 0.3,"rotation", "catmullrom"),
            kf(phase + 1.8,   0, 0, 0,   "rotation", "catmullrom"),
            kf(phase + 2.7,  -1, 0,-0.3,"rotation", "catmullrom"),
            kf(phase + 3.6,   0, 0, 0,   "rotation", "catmullrom"),
            kf(7.0,           0, 0, 0,   "rotation", "catmullrom"),
        ]

    # Disc slabs pulse emissive (Y oscillation)
    for i, uuid in enumerate(disc_uuids):
        phase = i * 1.1
        kf_map[uuid] = [
            kf(0.0,          0, 0, 0, "position", "catmullrom"),
            kf(phase + 1.0,  0, 0.08, 0, "position", "catmullrom"),
            kf(phase + 2.0,  0, 0, 0, "position", "catmullrom"),
            kf(phase + 3.0,  0,-0.05, 0, "position", "catmullrom"),
            kf(phase + 4.0,  0, 0, 0, "position", "catmullrom"),
            kf(7.0,          0, 0, 0, "position", "catmullrom"),
        ]

    # Rupture slabs settle with micro-tremor
    for i, uuid in enumerate(rupture_uuids):
        phase = i * 1.2
        kf_map[uuid] = [
            kf(0.0,          0, 0, 0, "rotation", "catmullrom"),
            kf(phase + 1.5,  1, 0, 0.5, "rotation", "catmullrom"),
            kf(phase + 3.0,  0, 0, 0, "rotation", "catmullrom"),
            kf(phase + 4.5, -0.5, 0,-0.3,"rotation","catmullrom"),
            kf(phase + 6.0,  0, 0, 0, "rotation", "catmullrom"),
            kf(7.0,          0, 0, 0, "rotation", "catmullrom"),
        ]

    # Nerve strands slowly oscillate Y
    for i, uuid in enumerate(nerve_uuids):
        phase = i * 1.7
        amp = 0.06 + i * 0.02
        kf_map[uuid] = [
            kf(0.0,          0, 0, 0, "position", "catmullrom"),
            kf(phase + 1.75, 0, amp, 0, "position", "catmullrom"),
            kf(phase + 3.5,  0, 0, 0, "position", "catmullrom"),
            kf(phase + 5.25, 0,-amp, 0, "position", "catmullrom"),
            kf(phase + 7.0,  0, 0, 0, "position", "catmullrom"),
            kf(7.0,          0, 0, 0, "position", "catmullrom"),
        ]

    # Cracks micro-tremble
    for i, uuid in enumerate(crack_uuids):
        phase = i * 0.8
        kf_map[uuid] = [
            kf(0.0,          0, 0, 0, "rotation", "catmullrom"),
            kf(phase + 1.0,  0.5, 0, 0.3, "rotation", "catmullrom"),
            kf(phase + 2.0,  0, 0, 0, "rotation", "catmullrom"),
            kf(phase + 3.0, -0.5, 0,-0.3,"rotation","catmullrom"),
            kf(phase + 4.0,  0, 0, 0, "rotation", "catmullrom"),
            kf(7.0,          0, 0, 0, "rotation", "catmullrom"),
        ]

    # Extra disc highlights pulse
    for i, uuid in enumerate(disc_extra_uuids):
        phase = i * 2.1
        kf_map[uuid] = [
            kf(0.0,         0, 0, 0, "position", "catmullrom"),
            kf(phase+1.5,   0,0.06, 0,"position","catmullrom"),
            kf(phase+3.0,   0, 0, 0, "position", "catmullrom"),
            kf(phase+4.5,   0,-0.04,0,"position","catmullrom"),
            kf(phase+6.0,   0, 0, 0, "position", "catmullrom"),
            kf(7.0,         0, 0, 0, "position", "catmullrom"),
        ]

    # Extra accents tremble
    for i, uuid in enumerate(extra_accent_uuids):
        phase = i * 0.7
        kf_map[uuid] = [
            kf(0.0,         0, 0, 0, "rotation", "catmullrom"),
            kf(phase+0.9,   0.8,0,0.4,"rotation","catmullrom"),
            kf(phase+1.8,   0, 0, 0, "rotation", "catmullrom"),
            kf(phase+2.7,  -0.8,0,-0.4,"rotation","catmullrom"),
            kf(phase+3.6,   0, 0, 0, "rotation", "catmullrom"),
            kf(7.0,         0, 0, 0, "rotation", "catmullrom"),
        ]

    return make_animators_for_all_bones(kf_map)


# ─── dissipate animation (once, 1.0s) ────────────────────────────────────────
def make_dissipate_animators():
    kf_map = {}

    # Reverse order: last to erupt first to sink
    for i, uuid in enumerate(vert_body_uuids):
        idx_rev = 6 - i  # reverse order
        t_start = 0.05 + idx_rev * 0.10
        t_end   = t_start + 0.18
        kf_map[uuid] = [
            kf(0.0,     0, 0, 0,   "position", "catmullrom"),
            kf(t_start, 0, 0, 0,   "position", "catmullrom"),
            kf(t_end,   0, -12, 0, "position", "linear"),
            kf(1.0,     0, -12, 0, "position", "linear"),
            kf(0.0,     0, 0, 0,   "rotation", "catmullrom"),
            kf(t_start, 3, 0, 2,   "rotation", "catmullrom"),
            kf(t_end,   0, 0, 0,   "rotation", "linear"),
            kf(1.0,     0, 0, 0,   "rotation", "linear"),
        ]

    # Flanges fold inward before each vertebra sinks
    for i, (lu, ru) in enumerate(zip(vert_left_uuids, vert_right_uuids)):
        idx_rev = 6 - i
        t_fold  = 0.05 + idx_rev * 0.10 - 0.05
        t_sink  = t_fold + 0.08
        for uuid, sign in [(lu, 1), (ru, -1)]:
            kf_map[uuid] = [
                kf(0.0,     0, 0, 0,        "rotation", "catmullrom"),
                kf(t_fold,  0, 0, sign*(-20),"rotation","catmullrom"),
                kf(t_sink,  0, 0, 0,        "rotation", "catmullrom"),
                kf(1.0,     0, 0, 0,        "rotation", "linear"),
                kf(0.0,     0, 0, 0,        "position", "catmullrom"),
                kf(t_sink,  0, 0, 0,        "position", "catmullrom"),
                kf(t_sink+0.15, 0,-12, 0,   "position", "linear"),
                kf(1.0,     0,-12, 0,        "position", "linear"),
            ]

    # Processes retract
    for i, uuid in enumerate(vert_process_uuids):
        idx_rev = 6 - i
        t_start = 0.03 + idx_rev * 0.10
        t_end   = t_start + 0.15
        kf_map[uuid] = [
            kf(0.0,     0, 0, 0,  "position", "catmullrom"),
            kf(t_start, 0, 0, 0,  "position", "catmullrom"),
            kf(t_end,   0,-4, 0,  "position", "linear"),
            kf(1.0,     0,-4, 0,  "position", "linear"),
            kf(0.0,     0, 0, 0,  "rotation", "catmullrom"),
            kf(t_start,-5, 0,-2,  "rotation", "catmullrom"),
            kf(t_end,   0, 0, 0,  "rotation", "linear"),
            kf(1.0,     0, 0, 0,  "rotation", "linear"),
        ]

    for i, uuid in enumerate(vert_inner_uuids):
        idx_rev = 6 - i
        t_start = 0.05 + idx_rev * 0.10
        t_end   = t_start + 0.2
        kf_map[uuid] = [
            kf(0.0,     0, 0, 0,   "position", "catmullrom"),
            kf(t_start, 0, 0, 0,   "position", "catmullrom"),
            kf(t_end,   0,-12, 0,  "position", "linear"),
            kf(1.0,     0,-12, 0,  "position", "linear"),
            kf(0.0,     0, 0, 0,   "rotation", "catmullrom"),
            kf(t_start, 2, 0, 1,   "rotation", "catmullrom"),
            kf(t_end,   0, 0, 0,   "rotation", "linear"),
            kf(1.0,     0, 0, 0,   "rotation", "linear"),
        ]

    # Discs snap to 0 as vertebrae leave
    for i, uuid in enumerate(disc_uuids):
        idx_rev = 5 - i
        t_snap = 0.05 + idx_rev * 0.10 + 0.05
        kf_map[uuid] = [
            kf(0.0,    0, 0, 0,   "position", "catmullrom"),
            kf(t_snap, 0, 0, 0,   "position", "catmullrom"),
            kf(t_snap+0.05, 0,-12,0,"position","linear"),
            kf(1.0,    0,-12, 0,  "position", "linear"),
            kf(0.0,    0, 0, 0,   "rotation", "catmullrom"),
            kf(t_snap, 0,10, 0,   "rotation", "catmullrom"),
            kf(t_snap+0.05,0, 0, 0,"rotation","linear"),
            kf(1.0,    0, 0, 0,   "rotation", "linear"),
        ]

    # Nerve strands dissolve
    for i, uuid in enumerate(nerve_uuids):
        kf_map[uuid] = [
            kf(0.0,  0, 0, 0, "position", "catmullrom"),
            kf(0.4,  0, 0, 0, "position", "catmullrom"),
            kf(0.7,  0, 0.5, 0,"position","catmullrom"),
            kf(0.9,  0, -0.5,0,"position","catmullrom"),
            kf(1.0,  0, -8, 0, "position", "linear"),
            kf(0.0,  0, 0, 0, "rotation", "catmullrom"),
            kf(0.5,  5, 0, 5, "rotation", "catmullrom"),
            kf(1.0,  0, 0, 0, "rotation", "linear"),
        ]

    # Rupture slabs contract
    for i, uuid in enumerate(rupture_uuids):
        t = 0.6 + i * 0.04
        kf_map[uuid] = [
            kf(0.0, 0, 0, 0, "position", "catmullrom"),
            kf(t,   0, 0, 0, "position", "catmullrom"),
            kf(t+0.15, 0,-1,0,"position","linear"),
            kf(1.0, 0,-1, 0, "position", "linear"),
            kf(0.0, 0, 0, 0, "rotation", "catmullrom"),
            kf(t,  -5, 0,-5, "rotation", "catmullrom"),
            kf(1.0, 0, 0, 0, "rotation", "linear"),
        ]

    # Crack accents contract
    for i, uuid in enumerate(crack_uuids):
        t = 0.5 + i * 0.04
        kf_map[uuid] = [
            kf(0.0, 0, 0, 0, "position", "catmullrom"),
            kf(t,   0, 0, 0, "position", "catmullrom"),
            kf(t+0.15,0,-1,0,"position","linear"),
            kf(1.0, 0,-1, 0, "position", "linear"),
            kf(0.0, 0, 0, 0, "rotation", "catmullrom"),
            kf(t,   3, 0, 3, "rotation", "catmullrom"),
            kf(1.0, 0, 0, 0, "rotation", "linear"),
        ]

    for i, uuid in enumerate(disc_extra_uuids):
        t = 0.3 + i * 0.1
        kf_map[uuid] = [
            kf(0.0, 0, 0, 0, "position", "catmullrom"),
            kf(t,   0, 0, 0, "position", "catmullrom"),
            kf(t+0.1,0,-12,0,"position","linear"),
            kf(1.0, 0,-12,0, "position", "linear"),
            kf(0.0, 0, 0, 0, "rotation", "catmullrom"),
            kf(t,   0, 5, 0, "rotation", "catmullrom"),
            kf(1.0, 0, 0, 0, "rotation", "linear"),
        ]

    for i, uuid in enumerate(extra_accent_uuids):
        t = 0.5 + i * 0.04
        kf_map[uuid] = [
            kf(0.0, 0, 0, 0, "position", "catmullrom"),
            kf(t,   0, 0, 0, "position", "catmullrom"),
            kf(t+0.12,0,-1.5,0,"position","linear"),
            kf(1.0, 0,-1.5,0, "position", "linear"),
            kf(0.0, 0, 0, 0, "rotation", "catmullrom"),
            kf(t,   0,15, 0, "rotation", "catmullrom"),
            kf(1.0, 0, 0, 0, "rotation", "linear"),
        ]

    return make_animators_for_all_bones(kf_map)


# ─── Build animation objects ──────────────────────────────────────────────────
spawn_anims = make_spawn_animators()
idle_anims  = make_idle_animators()
diss_anims  = make_dissipate_animators()

animations = [
    {
        "uuid": "a001",
        "name": "animation.devils_spine_array.spawn",
        "loop": "once",
        "length": 1.3,
        "snapping": 24,
        "selected": False,
        "saved": False,
        "animators": spawn_anims
    },
    {
        "uuid": "a002",
        "name": "animation.devils_spine_array.idle",
        "loop": "loop",
        "length": 7.0,
        "snapping": 24,
        "selected": False,
        "saved": False,
        "animators": idle_anims
    },
    {
        "uuid": "a003",
        "name": "animation.devils_spine_array.dissipate",
        "loop": "once",
        "length": 1.0,
        "snapping": 24,
        "selected": False,
        "saved": False,
        "animators": diss_anims
    },
]

# ─── Outliner (flat — each bone is top-level) ─────────────────────────────────
outliner = [b["uuid"] for b in bones]

# ─── Textures ─────────────────────────────────────────────────────────────────
textures = [
    {
        "id": "0",
        "name": "devils_spine_array_tex",
        "relative_path": "../textures/devils_spine_array_tex.png",
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
        "name": "devils_spine_array_tex1",
        "relative_path": "../textures/devils_spine_array_tex1.png",
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
    "name": "devils_spine_array",
    "geometry": "geometry.devils_spine_array",
    "resolution": {"width": 64, "height": 64},
    "elements": elements,
    "outliner": outliner,
    "textures": textures,
    "animations": animations
}

out_path = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/devils_spine_array.bbmodel"
with open(out_path, "w", encoding="utf-8") as f:
    json.dump(bbmodel, f, separators=(',', ':'))

size = os.path.getsize(out_path)
print(f"File size: {size} bytes ({size/1024:.1f} KB)")
print(f"Elements: {len(elements)}, Bones: {len(bones)}")

if size < 300000:
    print(f"WARNING: File too small! Need 300000+, got {size}")
else:
    print("SIZE OK")
