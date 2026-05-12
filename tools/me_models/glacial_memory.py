#!/usr/bin/env python3
"""Generator for glacial_memory.bbmodel — FreezingIce attack #25.

Visual intent: signature ultimate summon — the glacier itself materialising.
Massive wall of ancient ice with 3 orbiting formations, deep navy void
behind, columns of compressed cold rising from base. Largest and most
complex model in the mode. "Players should stop moving and look."
"""

import sys
import math
import random

sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder,
    make_uuid,
    make_keyframe,
    uniform_face,
    png_from_pixels,
    hex_to_rgba,
    color_lerp,
)
from _freezingice_helpers import (
    base_fill,
    specular_cluster,
    jitter_inplace,
    annual_bands,
)

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/freezingice/me_attacks/glacial_memory.bbmodel"


# ---------------------------------------------------------------------------
# TEXTURES — 128x128
# ---------------------------------------------------------------------------

def build_glacier_face_texture():
    """Tex 0: GLACIAL MEMORY TECHNIQUE — diagonal zones, deep-to-surface cross-section."""
    w = h = 128
    ancient_dark = hex_to_rgba("#02060e")   # top-left x+y<48 (scaled to <96)
    compressed   = hex_to_rgba("#103060")   # mid
    surface      = hex_to_rgba("#7ab8e0")   # bottom-right x+y>80 (scaled to >160)
    frost_spec   = hex_to_rgba("#e0f0ff")
    emiss_mid    = hex_to_rgba("#50a0e0")

    pixels = base_fill(w, h, compressed)

    # Diagonal zones based on (x+y)
    # 64x64 spec: x+y<48 = dark, x+y>80 = surface
    # 128x128 scaled: x+y<96 = dark, x+y>160 = surface
    for y in range(h):
        for x in range(w):
            s = x + y  # 0 to 254
            if s < 96:
                t = s / 96.0
                c = color_lerp(ancient_dark, compressed, t * 0.8)
            elif s < 160:
                t = (s - 96) / 64.0
                c = color_lerp(compressed, color_lerp(compressed, surface, 0.5), t)
            else:
                t = (s - 160) / 94.0
                c = color_lerp(color_lerp(compressed, surface, 0.5), surface, t)
            pixels[y * w + x] = c

    # Annual strata horizontal lines every 16 rows (was every 8 in 64x64)
    for y in range(0, h, 16):
        for x in range(w):
            p = pixels[y * w + x]
            pixels[y * w + x] = color_lerp(p, ancient_dark, 0.3)
        if y + 1 < h:
            for x in range(w):
                p = pixels[(y + 1) * w + x]
                pixels[(y + 1) * w + x] = color_lerp(p, ancient_dark, 0.2)

    # Emissive glow in mid zone — large soft cluster
    cx, cy = 64, 64
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            if d < 28:
                strength = (1.0 - d / 28.0) ** 2 * 0.35
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, emiss_mid, strength)

    # Frost-white specular at scaled (110, 12) — was (55, 6) in 64x64
    specular_cluster(pixels, w, h, 110, 12, 7, frost_spec, strength=0.9)
    # Additional speculars in surface zone
    specular_cluster(pixels, w, h, 100, 30, 4, frost_spec, strength=0.7)
    specular_cluster(pixels, w, h, 85, 50, 3, frost_spec, strength=0.6)

    # Compression cracks — diagonal lines along the gradient direction
    rng = random.Random(404)
    for _ in range(8):
        # Crack goes perpendicular to diagonal (so along x-y or fixed offset)
        start_x = rng.randrange(0, w)
        start_y = rng.randrange(0, h)
        length = rng.randint(20, 50)
        # Direction perpendicular to (1,1): (-1, 1) or (1, -1)
        dx, dy = rng.choice([(-1, 1), (1, -1)])
        for k in range(length):
            xx = start_x + dx * k
            yy = start_y + dy * k
            if 0 <= xx < w and 0 <= yy < h:
                p = pixels[yy * w + xx]
                pixels[yy * w + xx] = color_lerp(p, frost_spec, 0.45)
                # Halo
                for d_off in (-1, 1):
                    nx, ny = xx + d_off * dy, yy + d_off * (-dx)
                    if 0 <= nx < w and 0 <= ny < h:
                        p2 = pixels[ny * w + nx]
                        pixels[ny * w + nx] = color_lerp(p2, frost_spec, 0.2)

    # Trapped object hints — small dark blobs in mid zone
    for _ in range(8):
        bx = rng.randrange(30, 90)
        by = rng.randrange(30, 90)
        specular_cluster(pixels, w, h, bx, by, 2, ancient_dark, strength=0.75)

    jitter_inplace(pixels, jitter_range=18, seed=2424, density=1.0)
    jitter_inplace(pixels, jitter_range=12, seed=4848, density=0.5)
    return png_from_pixels(pixels, w, h)


def build_glacier_void_texture():
    """Tex 1: DEEP GLACIAL VOID — near-black with faint ancient ice detail
    and a single thin emissive line at row 32 (scaled to 64)."""
    w = h = 128
    void_blue = hex_to_rgba("#020610")
    faint     = hex_to_rgba("#082030")
    thin_light = hex_to_rgba("#3070b0")
    trap_dark = hex_to_rgba("#0a1828")
    spec      = hex_to_rgba("#80b0e0")

    pixels = base_fill(w, h, void_blue)

    # Faint ancient ice background pattern — barely visible
    rng = random.Random(606)
    for _ in range(80):
        cx = rng.randrange(w)
        cy = rng.randrange(h)
        radius = rng.randint(2, 5)
        for dy in range(-radius, radius + 1):
            for dx in range(-radius, radius + 1):
                x, y = cx + dx, cy + dy
                if 0 <= x < w and 0 <= y < h:
                    d = math.sqrt(dx * dx + dy * dy)
                    if d <= radius:
                        t = 1.0 - d / float(radius)
                        p = pixels[y * w + x]
                        pixels[y * w + x] = color_lerp(p, faint, t * 0.45)

    # Single thin emissive line at row 64 (scaled from row 32 in 64x64)
    for x in range(w):
        p = pixels[64 * w + x]
        pixels[64 * w + x] = color_lerp(p, thin_light, 0.85)
        if 65 < h:
            p2 = pixels[65 * w + x]
            pixels[65 * w + x] = color_lerp(p2, thin_light, 0.55)
        if 63 >= 0:
            p3 = pixels[63 * w + x]
            pixels[63 * w + x] = color_lerp(p3, thin_light, 0.55)

    # Add a few faint speculars along emissive line
    for _ in range(6):
        sx = rng.randrange(w)
        specular_cluster(pixels, w, h, sx, 64, 3, spec, strength=0.55)

    # Trapped dark shapes — silhouettes barely visible in the depths
    trap_positions = [(20, 30), (50, 90), (75, 25), (100, 100), (30, 110), (95, 50)]
    for (tx, ty) in trap_positions:
        for dy in range(-3, 4):
            for dx in range(-4, 5):
                x, y = tx + dx, ty + dy
                if 0 <= x < w and 0 <= y < h:
                    d = math.sqrt(dx * dx + dy * dy)
                    if d <= 4:
                        t = 1.0 - d / 4.0
                        p = pixels[y * w + x]
                        pixels[y * w + x] = color_lerp(p, trap_dark, t * 0.7)

    # Sparse noise for void texture
    rngd = random.Random(909)
    for _ in range(300):
        x = rngd.randrange(w)
        y = rngd.randrange(h)
        p = pixels[y * w + x]
        pixels[y * w + x] = color_lerp(p, faint, rngd.random() * 0.4)

    jitter_inplace(pixels, jitter_range=14, seed=5151, density=1.0)
    jitter_inplace(pixels, jitter_range=10, seed=7373, density=0.5)
    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# MODEL
# ---------------------------------------------------------------------------

def build():
    b = Builder("glacial_memory", resolution=(128, 128), visible_box=(48, 28, 0))
    b.add_texture("glacier_face", build_glacier_face_texture())
    b.add_texture("glacier_void", build_glacier_void_texture())

    def tex0(): return uniform_face(0, 0, 128, 128, tex_index=0)
    def tex1(): return uniform_face(0, 0, 128, 128, tex_index=1)

    # =====================================================================
    # 1) Central glacier face — 7 large flat slab bones in arc
    # Heights: 12, 16, 14, 18, 14, 16, 12
    # =====================================================================
    face_heights = [12, 16, 14, 18, 14, 16, 12]
    face_groups = []
    face_width = 4
    # Arc curvature: forward Z bias for outer slabs
    for i, fh in enumerate(face_heights):
        offset_idx = i - 3  # -3 to +3
        x_pos = offset_idx * face_width
        # Slight forward bias on outer (curving the wall)
        z_bias = abs(offset_idx) * 0.5  # outer slabs come forward (positive Z)
        slab_uuid = b.add_cube(
            f"face_slab_{i+1}",
            from_=[-face_width / 2 + 0.05, 0, -0.6],
            to_=[face_width / 2 - 0.05, fh, 0.6],
            faces=tex0(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"face_{i+1}", [x_pos, 0, z_bias], [slab_uuid],
            rotation=[0, -offset_idx * 6, 0],  # slight rotation for arc
            group_uuid=gu,
        )
        face_groups.append((gu, g, fh))

    # =====================================================================
    # 2) Glacier interior depth — 4 flat slab bones set 2 units behind face
    # =====================================================================
    interior_groups = []
    for i in range(4):
        x_pos = (i - 1.5) * 6
        interior_uuid = b.add_cube(
            f"interior_slab_{i+1}",
            from_=[-3, 1, -0.4], to_=[3, 16, 0.4],
            faces=tex1(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"interior_{i+1}", [x_pos, 0, -2], [interior_uuid],
            group_uuid=gu,
        )
        interior_groups.append((gu, g))

    # =====================================================================
    # 3) Base pressure columns — 5 wide column bone groups at the base
    # Each column: 3 stacked segments decreasing in width
    # =====================================================================
    column_groups = []  # list of (group_uuid, group, segment_uuids[])
    for ci in range(5):
        x_pos = (ci - 2) * 5
        seg_cubes = []
        # Bottom segment: widest
        seg_cubes.append(b.add_cube(
            f"col{ci+1}_seg1",
            from_=[-1.8, 0, -1.2], to_=[1.8, 2, 1.2],
            faces=tex0(),
        ))
        # Mid segment
        seg_cubes.append(b.add_cube(
            f"col{ci+1}_seg2",
            from_=[-1.3, 2, -0.9], to_=[1.3, 3.8, 0.9],
            faces=tex0(),
        ))
        # Top segment: narrowest
        seg_cubes.append(b.add_cube(
            f"col{ci+1}_seg3",
            from_=[-0.8, 3.8, -0.6], to_=[0.8, 5.4, 0.6],
            faces=tex0(),
        ))
        gu = make_uuid()
        g = b.make_group(
            f"column_{ci+1}", [x_pos, 0, 1.5], seg_cubes,
            group_uuid=gu,
        )
        column_groups.append((gu, g))

    # =====================================================================
    # 4) Orbiting formations — 3 ice formation groups at radius 14
    # Each: 4 cubes of varying sizes + 2 spike accents
    # =====================================================================
    formation_groups = []
    for fi in range(3):
        ang_start = fi * (2 * math.pi / 3)
        cx = math.cos(ang_start) * 14
        cz = math.sin(ang_start) * 14
        cy = 8

        cubes = []
        # 4 varying-sized cubes — irregular cluster
        cluster_offsets = [
            (0,    0,    0,    1.2),
            (1.0,  0.6,  0.3,  0.8),
            (-0.8, 0.4, -0.6,  1.0),
            (0.3, -0.5,  0.9,  0.7),
        ]
        for i, (ox, oy, oz, sz) in enumerate(cluster_offsets):
            cubes.append(b.add_cube(
                f"formation{fi+1}_cube{i+1}",
                from_=[ox - sz, oy - sz, oz - sz],
                to_=[ox + sz, oy + sz, oz + sz],
                faces=tex0(),
            ))
        # 2 spike accents
        for i in range(2):
            sang = i * math.pi  # opposite sides
            sx_o = math.cos(sang) * 1.5
            sz_o = math.sin(sang) * 1.5
            cubes.append(b.add_cube(
                f"formation{fi+1}_spike{i+1}",
                from_=[sx_o - 0.25, 0.5 + i * 0.3, sz_o - 0.25],
                to_=[sx_o + 0.25, 2.5 + i * 0.3, sz_o + 0.25],
                faces=tex0(),
            ))

        inner_uuid = make_uuid()
        inner_g = b.make_group(f"formation{fi+1}_inner", [14, cy, 0], cubes,
                                group_uuid=inner_uuid)

        # Orbit group (rotates around origin)
        orbit_uuid = make_uuid()
        orbit_g = b.make_group(
            f"formation{fi+1}_orbit", [0, cy, 0], [inner_g],
            rotation=[0, math.degrees(ang_start), 0],
            group_uuid=orbit_uuid,
        )
        formation_groups.append((orbit_uuid, orbit_g, inner_uuid, inner_g))

    # =====================================================================
    # 5) Ice fall — 10 flat slab pieces at various heights in front of glacier
    # =====================================================================
    fall_groups = []
    for i in range(10):
        x_pos = (random.Random(i + 100).random() - 0.5) * 20
        y_pos = 4 + (random.Random(i + 200).random()) * 12
        z_pos = 2 + random.Random(i + 300).random() * 3
        slab_uuid = b.add_cube(
            f"ice_fall_{i+1}",
            from_=[-0.6, 0, -0.4], to_=[0.6, 0.3, 0.4],
            faces=tex0(),
            rotation=[i * 7, i * 11, i * 13],
        )
        gu = make_uuid()
        g = b.make_group(
            f"fall_{i+1}", [x_pos, y_pos, z_pos], [slab_uuid],
            group_uuid=gu,
        )
        fall_groups.append((gu, g, y_pos))

    # =====================================================================
    # 6) Ground freeze advance — 12 flat slabs at Y=0 spreading from base
    # =====================================================================
    ground_groups = []
    for i in range(12):
        # Spread in arc in front of glacier
        ang = (i / 12.0) * math.pi - math.pi / 2  # -90° to +90° (forward arc)
        rr = 4 + (i % 4) * 1.5
        cx = math.cos(ang) * rr
        cz = math.sin(ang) * rr + 3  # offset forward
        slab_uuid = b.add_cube(
            f"ground_freeze_{i+1}",
            from_=[-1.0, -0.08, -0.6], to_=[1.0, 0.08, 0.6],
            faces=tex1(),
            rotation=[0, math.degrees(ang), 0],
        )
        gu = make_uuid()
        g = b.make_group(
            f"ground_{i+1}", [cx, 0, cz], [slab_uuid], group_uuid=gu,
        )
        ground_groups.append((gu, g))

    # =====================================================================
    # 7) Crown of ice — 6 large spike bones across top of glacier wall
    # =====================================================================
    crown_groups = []
    for i in range(6):
        offset_idx = i - 2.5
        x_pos = offset_idx * 4.5
        # 3-cube tapered spike
        cubes = []
        cubes.append(b.add_cube(
            f"crown{i+1}_base",
            from_=[-0.8, 0, -0.8], to_=[0.8, 1.5, 0.8],
            faces=tex0(),
        ))
        cubes.append(b.add_cube(
            f"crown{i+1}_mid",
            from_=[-0.55, 1.5, -0.55], to_=[0.55, 2.8, 0.55],
            faces=tex0(),
        ))
        cubes.append(b.add_cube(
            f"crown{i+1}_tip",
            from_=[-0.3, 2.8, -0.3], to_=[0.3, 4, 0.3],
            faces=tex0(),
        ))
        # Position at top of glacier wall (using face_heights for height under each)
        # use height 16 as base for crown placement
        gu = make_uuid()
        g = b.make_group(
            f"crown_{i+1}", [x_pos, 16, 0.2], cubes,
            rotation=[(-1)**i * 3, i * 8, 0],
            group_uuid=gu,
        )
        crown_groups.append((gu, g))

    # =====================================================================
    # ROOT
    # =====================================================================
    root_children = []
    for (_uu, g, _h) in face_groups: root_children.append(g)
    for (_uu, g) in interior_groups: root_children.append(g)
    for (_uu, g) in column_groups: root_children.append(g)
    for (ou, og, iu, ig) in formation_groups: root_children.append(og)
    for (_uu, g, _y) in fall_groups: root_children.append(g)
    for (_uu, g) in ground_groups: root_children.append(g)
    for (_uu, g) in crown_groups: root_children.append(g)

    root_uuid = make_uuid()
    root_g = b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid)
    b.outliner.append(root_g)

    # =====================================================================
    # ANIMATIONS
    # =====================================================================

    # ----- SPAWN: 2.0s -----
    spawn = {}
    # Ground freeze advance at 0.2s
    for i, (gu, g) in enumerate(ground_groups):
        delay = 0.2 + i * 0.02
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 0, 1, 0),
            make_keyframe("scale", delay, 0, 1, 0),
            make_keyframe("scale", delay + 0.3, 1.0, 1.0, 1.0),
            make_keyframe("scale", 2.0, 1.0, 1.0, 1.0),
        ]}
    # Base pressure columns erupt at 0.3s segment by segment — use group scale
    for ci, (gu, g) in enumerate(column_groups):
        delay = 0.3 + ci * 0.04
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 1, 0, 1),
            make_keyframe("scale", delay, 1, 0, 1),
            make_keyframe("scale", delay + 0.3, 1, 1.2, 1),
            make_keyframe("scale", delay + 0.5, 1, 1.0, 1),
            make_keyframe("scale", 2.0, 1, 1, 1),
        ]}
    # Glacier face sections rise — height-based stagger (shortest first, tallest last)
    # 0.15s per metre. h=12 → fast, h=18 → slow.
    for fi, (gu, g, fh) in enumerate(face_groups):
        # delay scaled by height: shorter rises faster
        rise_start = 0.3
        rise_duration = fh * 0.05  # 0.6s for h=12, 0.9s for h=18
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 1, 0, 1),
            make_keyframe("scale", rise_start, 1, 0, 1),
            make_keyframe("scale", rise_start + rise_duration, 1, 1.05, 1),
            make_keyframe("scale", rise_start + rise_duration + 0.1, 1, 1.0, 1),
            make_keyframe("scale", 2.0, 1, 1, 1),
        ]}
    # Interior depth slabs at 0.8s
    for ii, (gu, g) in enumerate(interior_groups):
        delay = 0.8 + ii * 0.05
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.25, 1.0, 1.0, 1.0),
            make_keyframe("scale", 2.0, 1.0, 1.0, 1.0),
        ]}
    # Crown spikes extend at 1.2s
    for ci, (gu, g) in enumerate(crown_groups):
        delay = 1.2 + ci * 0.03
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.25, 1.1, 1.1, 1.1),
            make_keyframe("scale", delay + 0.35, 1.0, 1.0, 1.0),
            make_keyframe("scale", 2.0, 1.0, 1.0, 1.0),
        ]}
    # Ice fall pieces begin falling at 0.8s — positions move
    for fi, (gu, g, y_initial) in enumerate(fall_groups):
        delay = 0.8 + fi * 0.04
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.15, 1.0, 1.0, 1.0),
            make_keyframe("scale", 2.0, 1.0, 1.0, 1.0),
        ]}
    # Orbiting formations fly in from outer space at 1.2-1.5s
    for fi, (ou, og, iu, ig) in enumerate(formation_groups):
        delay = 1.2 + fi * 0.1
        spawn[iu] = {"name": ig["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, 10, 0, 0),
            make_keyframe("position", delay, 10, 0, 0),
            make_keyframe("position", delay + 0.25, 0, 0, 0),
            make_keyframe("position", 2.0, 0, 0, 0),
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.2, 1.0, 1.0, 1.0),
            make_keyframe("scale", 2.0, 1.0, 1.0, 1.0),
        ]}
    # At 1.8s: entire glacier pulses scale 1.0->1.08->1.0
    spawn[root_uuid] = {"name": "root", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 1, 1, 1),
        make_keyframe("scale", 1.8, 1, 1, 1),
        make_keyframe("scale", 1.9, 1.08, 1.08, 1.08),
        make_keyframe("scale", 2.0, 1.0, 1.0, 1.0),
    ]}
    b.add_animation("spawn", length=2.0, animators=spawn, loop="once", override=True)

    # ----- IDLE: 10.0s loop -----
    idle = {}
    # Root slowly drifts Z (glacier advancing 0.8 units)
    root_idle = []
    for k in range(24):
        t = (k / 23) * 10.0
        # 0 -> 0.8 -> 0 over 10s — sinusoidal forward drift
        z_drift = 0.4 * (1.0 - math.cos(t * 2 * math.pi / 10.0))
        root_idle.append(make_keyframe("position", t, 0, 0, z_drift))
    idle[root_uuid] = {"name": "root", "type": "bone", "keyframes": root_idle}

    # Crown spikes very subtly sway — different periods
    for ci, (gu, g) in enumerate(crown_groups):
        kfs = []
        period = 7.0 + ci * 1.0
        for k in range(16):
            t = (k / 15) * 10.0
            rx = 1.5 * math.sin(t * 2 * math.pi / period + ci * 0.5)
            rz = 1.0 * math.cos(t * 2 * math.pi / (period * 0.7) + ci * 0.3)
            kfs.append(make_keyframe("rotation", t, rx, 0, rz))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Each face section has independent ±0.2° sway on periods of 7-13s
    for fi, (gu, g, fh) in enumerate(face_groups):
        kfs = []
        period = 7 + fi * 1.0
        for k in range(14):
            t = (k / 13) * 10.0
            rz = 0.2 * math.sin(t * 2 * math.pi / period + fi * 0.7)
            rx = 0.15 * math.cos(t * 2 * math.pi / (period * 0.8) + fi * 0.5)
            kfs.append(make_keyframe("rotation", t, rx, 0, rz))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Orbiting formations orbit at 5-9s per orbit
    formation_periods = [5.0, 7.0, 9.0]
    for fi, ((ou, og, iu, ig), period) in enumerate(zip(formation_groups, formation_periods)):
        kfs = []
        for k in range(20):
            t = (k / 19) * 10.0
            ang_deg = (t / period) * 360 + fi * 120
            kfs.append(make_keyframe("rotation", t, 0, ang_deg, 0))
        idle[ou] = {"name": og["name"], "type": "bone", "keyframes": kfs}
        # Inner formation tumbles
        inner_kfs = []
        for k in range(16):
            t = (k / 15) * 10.0
            inner_kfs.append(make_keyframe("rotation", t,
                                           t * 36 + fi * 30,
                                           t * 24 + fi * 60,
                                           t * 18 + fi * 45))
        idle[iu] = {"name": ig["name"], "type": "bone", "keyframes": inner_kfs}

    # Ice fall pieces loop (top -> bottom -> reset)
    for fi, (gu, g, y_initial) in enumerate(fall_groups):
        kfs = []
        phase = fi * 0.7
        for k in range(20):
            t = (k / 19) * 10.0
            # loop: y goes from +6 to -6 over 4s then resets
            cycle_t = ((t + phase) % 4.0) / 4.0
            y_off = 6 - cycle_t * 12
            kfs.append(make_keyframe("position", t, 0, y_off, 0))
            # Rotation tumble
            kfs.append(make_keyframe("rotation", t,
                                     t * 60 + fi * 30, t * 45, t * 30))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Ground freeze advance slowly expands
    for gi, (gu, g) in enumerate(ground_groups):
        kfs = []
        for k in range(14):
            t = (k / 13) * 10.0
            s = 1.0 + 0.04 * math.sin(t * 2 * math.pi / 10.0 + gi * 0.3)
            kfs.append(make_keyframe("scale", t, s, 1.0, s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Columns subtle scale breathing
    for ci, (gu, g) in enumerate(column_groups):
        kfs = []
        period = 6.0 + ci * 0.8
        for k in range(12):
            t = (k / 11) * 10.0
            s = 1.0 + 0.025 * math.sin(t * 2 * math.pi / period + ci * 0.5)
            kfs.append(make_keyframe("scale", t, s, 1.0, s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("idle", length=10.0, animators=idle, loop="loop", override=False)

    # ----- DISSIPATE: 2.0s — glacier retreats -----
    diss = {}
    # Ice fall accelerates and all pieces fall to Y=0
    for fi, (gu, g, y_initial) in enumerate(fall_groups):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 1.0, 0, -y_initial * 0.5, 0),
            make_keyframe("position", 2.0, 0, -y_initial, 0),
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 2.0, 540, 360, 720),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 1.5, 0.7, 0.7, 0.7),
            make_keyframe("scale", 2.0, 0, 0, 0),
        ]}
    # Crown spikes calve off and fall
    for ci, (gu, g) in enumerate(crown_groups):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.3, 0, 1, 0),
            make_keyframe("position", 2.0, (ci - 2.5) * 2, -16, 4),
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 2.0, 90 + ci * 30, ci * 60, 45),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 2.0, 0, 0, 0),
        ]}
    # Orbiting formations fly outward tangentially
    for fi, (ou, og, iu, ig) in enumerate(formation_groups):
        ang = fi * (2 * math.pi / 3)
        # tangent direction is perpendicular to radial
        tx = -math.sin(ang) * 25
        tz = math.cos(ang) * 25
        diss[iu] = {"name": ig["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, 14, 0, 0),
            make_keyframe("position", 2.0, 14 + tx, 5, tz),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 1.5, 0.6, 0.6, 0.6),
            make_keyframe("scale", 2.0, 0, 0, 0),
        ]}
    # Face sections crack apart from crown downward sequentially
    # higher face sections collapse first
    for fi, (gu, g, fh) in enumerate(face_groups):
        # delay inversely proportional to height — tallest first
        delay = (18 - fh) * 0.08
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", delay, 0, 0, 0),
            make_keyframe("rotation", delay + 0.4, (fi - 3) * 15, 0, (fi - 3) * 25),
            make_keyframe("rotation", 2.0, (fi - 3) * 45, 0, (fi - 3) * 60),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", delay + 0.4, 1, 0.8, 1),
            make_keyframe("scale", 2.0, 0, 0, 0),
        ]}
    # Interior depth slabs fade slowly
    for ii, (gu, g) in enumerate(interior_groups):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 1.0, 1, 1, 1),
            make_keyframe("scale", 2.0, 0, 0, 0),
        ]}
    # Base columns crumble outward
    for ci, (gu, g) in enumerate(column_groups):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 2.0, (ci - 2) * 3, -1, 2),
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 2.0, 30, ci * 20, (ci - 2) * 30),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 2.0, 0, 0, 0),
        ]}
    # Ground freeze contracts slowly
    for gi, (gu, g) in enumerate(ground_groups):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 1.5, 0.5, 1, 0.5),
            make_keyframe("scale", 2.0, 0, 0, 0),
        ]}
    b.add_animation("dissipate", length=2.0, animators=diss, loop="once", override=True)

    b.write(OUTPUT_PATH)
    print(f"Wrote {OUTPUT_PATH}")


if __name__ == "__main__":
    build()
