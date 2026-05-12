#!/usr/bin/env python3
"""Generator for hypothermia_halo.bbmodel — FreezingIce attack #24.

Visual intent: ring of ice that orbits caster at head height and shrinks
inward — geometry of hypothermia. Three rings (outer pale, mid, inner dark)
each darker than the last. Inward-pointing spikes. The horror is watching
the space shrink.
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
)

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/freezingice/me_attacks/hypothermia_halo.bbmodel"


# ---------------------------------------------------------------------------
# TEXTURES — 128x128
# ---------------------------------------------------------------------------

def build_intensity_texture():
    """Tex 0: HYPOTHERMIA INTENSITY — horizontal gradient pale->dark.
    Right side: pale ice blue (outer ring uses)
    Centre: medium (mid ring)
    Left side: deep navy (inner ring)
    """
    w = h = 128
    pale_outer = hex_to_rgba("#80b8d8")
    mid_a      = hex_to_rgba("#3878b0")
    deep_a     = hex_to_rgba("#1848a0")
    near_black = hex_to_rgba("#041838")
    spec       = hex_to_rgba("#c8e8ff")

    pixels = base_fill(w, h, pale_outer)

    # Horizontal gradient (left=dark, right=pale)
    for y in range(h):
        for x in range(w):
            t = x / float(w - 1)  # 0 = left (dark), 1 = right (pale)
            if t < 0.25:
                local = t / 0.25
                c = color_lerp(near_black, deep_a, local)
            elif t < 0.5:
                local = (t - 0.25) / 0.25
                c = color_lerp(deep_a, mid_a, local)
            elif t < 0.75:
                local = (t - 0.5) / 0.25
                c = color_lerp(mid_a, color_lerp(mid_a, pale_outer, 0.5), local)
            else:
                local = (t - 0.75) / 0.25
                c = color_lerp(color_lerp(mid_a, pale_outer, 0.5), pale_outer, local)
            pixels[y * w + x] = c

    # Add ice texture: vertical streaks at random positions
    rng = random.Random(202)
    for _ in range(40):
        cx = rng.randrange(w)
        cy = rng.randrange(h)
        length = rng.randint(8, 24)
        for k in range(length):
            yy = cy + k
            if 0 <= yy < h:
                # use a lighter version of current pixel
                p = pixels[yy * w + cx]
                pixels[yy * w + cx] = color_lerp(p, spec, 0.32)
                if cx + 1 < w:
                    p2 = pixels[yy * w + cx + 1]
                    pixels[yy * w + cx + 1] = color_lerp(p2, spec, 0.18)

    # Highlight specular on right side (pale zone)
    specular_cluster(pixels, w, h, 100, 30, 8, spec, strength=0.7)
    specular_cluster(pixels, w, h, 110, 90, 5, spec, strength=0.65)
    # Faint highlight on mid zone
    specular_cluster(pixels, w, h, 60, 60, 4, color_lerp(pale_outer, spec, 0.5), strength=0.5)

    # Annual band lines (subtle horizontal)
    for y in range(0, h, 16):
        for x in range(w):
            p = pixels[y * w + x]
            pixels[y * w + x] = color_lerp(p, color_lerp(p, near_black, 0.4), 0.5)

    # Per-pixel jitter
    jitter_inplace(pixels, jitter_range=16, seed=303, density=1.0)
    jitter_inplace(pixels, jitter_range=10, seed=505, density=0.5)
    return png_from_pixels(pixels, w, h)


def build_ground_texture():
    """Tex 1: ground impression — pale blue centre fading to dark."""
    w = h = 128
    pale = hex_to_rgba("#90c8e8")
    dark = hex_to_rgba("#040c14")
    pale_hi = hex_to_rgba("#e0f0ff")

    pixels = base_fill(w, h, dark)

    # Radial gradient centre -> edge
    cx, cy = 64, 64
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            t = min(1.0, d / 64.0)
            pixels[y * w + x] = color_lerp(pale, dark, t)

    # Brighter centre
    specular_cluster(pixels, w, h, cx, cy, 8, pale_hi, strength=0.75)

    # Frost rings (concentric)
    for r_target in (18, 32, 48):
        for y in range(h):
            for x in range(w):
                d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
                if abs(d - r_target) < 0.8:
                    p = pixels[y * w + x]
                    pixels[y * w + x] = color_lerp(p, pale_hi, 0.45)

    # Frost web spikes outward
    rng = random.Random(515)
    for b_idx in range(12):
        ang = b_idx * (math.pi / 6) + rng.random() * 0.15
        for r in range(0, 50):
            x = int(cx + math.cos(ang) * r)
            y = int(cy + math.sin(ang) * r)
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                strength = 0.55 * (1.0 - r / 50.0)
                pixels[y * w + x] = color_lerp(p, pale_hi, strength)

    jitter_inplace(pixels, jitter_range=14, seed=8181, density=1.0)
    jitter_inplace(pixels, jitter_range=8, seed=9191, density=0.45)
    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# MODEL
# ---------------------------------------------------------------------------

def build():
    b = Builder("hypothermia_halo", resolution=(128, 128), visible_box=(28, 14, 0))
    b.add_texture("intensity", build_intensity_texture())
    b.add_texture("ground", build_ground_texture())

    # UV sub-regions for intensity gradient
    # Right side (pale outer) — for outer ring
    def tex0_outer(): return uniform_face(86, 0, 128, 128, tex_index=0)
    # Centre — for mid ring
    def tex0_mid():   return uniform_face(40, 0, 88, 128, tex_index=0)
    # Left side (dark) — for inner ring
    def tex0_inner(): return uniform_face(0, 0, 42, 128, tex_index=0)
    # For spikes — use full
    def tex0_full():  return uniform_face(0, 0, 128, 128, tex_index=0)
    def tex1():       return uniform_face(0, 0, 128, 128, tex_index=1)

    # =====================================================================
    # 1) Outer halo ring — 16 slabs at Y=9 radius 12 — uses tex0_outer (pale)
    # =====================================================================
    outer_uuids = []
    for i in range(16):
        ang = (i / 16.0) * 2 * math.pi
        cx = math.cos(ang) * 12
        cz = math.sin(ang) * 12
        outer_uuids.append(b.add_cube(
            f"outer_halo_{i+1}",
            from_=[cx - 0.7, 9 - 0.3, cz - 0.3], to_=[cx + 0.7, 9 + 0.3, cz + 0.3],
            faces=tex0_outer(),
            rotation=[0, math.degrees(ang), 0],
        ))
    outer_uuid = make_uuid()
    outer_g = b.make_group("outer_halo", [0, 9, 0], outer_uuids, group_uuid=outer_uuid)

    # =====================================================================
    # 2) Mid ring — 12 slabs at radius 8 — uses tex0_mid
    # =====================================================================
    mid_uuids = []
    for i in range(12):
        ang = (i / 12.0) * 2 * math.pi
        cx = math.cos(ang) * 8
        cz = math.sin(ang) * 8
        mid_uuids.append(b.add_cube(
            f"mid_ring_{i+1}",
            from_=[cx - 0.6, 9 - 0.3, cz - 0.3], to_=[cx + 0.6, 9 + 0.3, cz + 0.3],
            faces=tex0_mid(),
            rotation=[0, math.degrees(ang), 0],
        ))
    mid_uuid = make_uuid()
    mid_g = b.make_group("mid_ring", [0, 9, 0], mid_uuids, group_uuid=mid_uuid)

    # =====================================================================
    # 3) Inner ring — 8 slabs at radius 5 — uses tex0_inner (dark)
    # =====================================================================
    inner_uuids = []
    for i in range(8):
        ang = (i / 8.0) * 2 * math.pi
        cx = math.cos(ang) * 5
        cz = math.sin(ang) * 5
        inner_uuids.append(b.add_cube(
            f"inner_ring_{i+1}",
            from_=[cx - 0.55, 9 - 0.3, cz - 0.3], to_=[cx + 0.55, 9 + 0.3, cz + 0.3],
            faces=tex0_inner(),
            rotation=[0, math.degrees(ang), 0],
        ))
    inner_uuid = make_uuid()
    inner_g = b.make_group("inner_ring", [0, 9, 0], inner_uuids, group_uuid=inner_uuid)

    # =====================================================================
    # 4) Halo spikes — 8 tapered spike bone chains pointing inward from outer ring
    # =====================================================================
    spike_groups = []
    for i in range(8):
        ang = (i / 8.0) * 2 * math.pi
        base_x = math.cos(ang) * 11
        base_z = math.sin(ang) * 11
        # 3-cube tapered spike pointing toward centre
        # Local: cube extends along -X (inward direction); rotation places it
        # Base cube (thick)
        cubes = []
        cubes.append(b.add_cube(
            f"spike{i+1}_base",
            from_=[-0.55, -0.4, -0.4], to_=[0.55, 0.4, 0.4],
            faces=tex0_full(),
        ))
        # Mid cube (thinner)
        cubes.append(b.add_cube(
            f"spike{i+1}_mid",
            from_=[0.55, -0.3, -0.3], to_=[1.6, 0.3, 0.3],
            faces=tex0_full(),
        ))
        # Tip cube (very thin)
        cubes.append(b.add_cube(
            f"spike{i+1}_tip",
            from_=[1.6, -0.15, -0.15], to_=[2.5, 0.15, 0.15],
            faces=tex0_full(),
        ))
        gu = make_uuid()
        # rotation: face inward — point -X direction toward origin
        # Place spike so it points from (base_x, base_z) toward (0,0). Rotate Y by ang+180.
        g = b.make_group(
            f"spike_{i+1}", [base_x, 9, base_z], cubes,
            rotation=[0, math.degrees(ang) + 180, 0],
            group_uuid=gu,
        )
        spike_groups.append((gu, g))

    # =====================================================================
    # 5) Frost crystal ornaments — 12 small flat diamonds orbiting between outer & mid
    # =====================================================================
    orn_groups = []
    for i in range(12):
        ang = (i / 12.0) * 2 * math.pi + 0.15
        rr = 10  # between outer 12 and mid 8
        cx = math.cos(ang) * rr
        cz = math.sin(ang) * rr
        # Diamond — rotated cube
        cube_uuid = b.add_cube(
            f"ornament_{i+1}",
            from_=[-0.45, -0.45, -0.1], to_=[0.45, 0.45, 0.1],
            faces=tex0_full(),
            rotation=[0, 0, 45],
        )
        gu = make_uuid()
        g = b.make_group(
            f"orn_{i+1}", [cx, 9, cz], [cube_uuid],
            rotation=[0, math.degrees(ang), 0],
            group_uuid=gu,
        )
        orn_groups.append((gu, g, ang, rr))

    # =====================================================================
    # 6) Ground impression — 4 overlapping flat slabs at Y=0
    # =====================================================================
    ground_cubes = []
    ground_offsets = [(0, 0), (1.0, 0.6), (-1.0, -0.5), (0.7, -0.7)]
    for i, (ox, oz) in enumerate(ground_offsets):
        sz = 3.5 - i * 0.4
        ground_cubes.append(b.add_cube(
            f"ground_imp_{i+1}",
            from_=[ox - sz, -0.05, oz - sz], to_=[ox + sz, 0.05, oz + sz],
            faces=tex1(),
        ))
    ground_uuid = make_uuid()
    ground_g = b.make_group("ground_impression", [0, 0, 0], ground_cubes, group_uuid=ground_uuid)

    # =====================================================================
    # ROOT
    # =====================================================================
    root_children = [outer_g, mid_g, inner_g, ground_g]
    for (_uu, g) in spike_groups: root_children.append(g)
    for (_uu, g, _a, _r) in orn_groups: root_children.append(g)
    root_uuid = make_uuid()
    root_g = b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid)
    b.outliner.append(root_g)

    # =====================================================================
    # ANIMATIONS
    # =====================================================================

    # ----- SPAWN: 0.8s -----
    spawn = {}
    # Ground at 0.0s
    spawn[ground_uuid] = {"name": "ground_impression", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 0, 1, 0),
        make_keyframe("scale", 0.25, 1.0, 1.0, 1.0),
        make_keyframe("scale", 0.8, 1.0, 1.0, 1.0),
    ]}
    # Outer ring at 0.1s
    spawn[outer_uuid] = {"name": "outer_halo", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 0, 0, 0),
        make_keyframe("scale", 0.1, 0, 0, 0),
        make_keyframe("scale", 0.35, 1.15, 1.15, 1.15),
        make_keyframe("scale", 0.45, 1.0, 1.0, 1.0),
        make_keyframe("scale", 0.8, 1.0, 1.0, 1.0),
    ]}
    # Mid ring at 0.3s
    spawn[mid_uuid] = {"name": "mid_ring", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 0, 0, 0),
        make_keyframe("scale", 0.3, 0, 0, 0),
        make_keyframe("scale", 0.5, 1.15, 1.15, 1.15),
        make_keyframe("scale", 0.6, 1.0, 1.0, 1.0),
        make_keyframe("scale", 0.8, 1.0, 1.0, 1.0),
    ]}
    # Inner ring at 0.5s
    spawn[inner_uuid] = {"name": "inner_ring", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 0, 0, 0),
        make_keyframe("scale", 0.5, 0, 0, 0),
        make_keyframe("scale", 0.7, 1.2, 1.2, 1.2),
        make_keyframe("scale", 0.8, 1.0, 1.0, 1.0),
    ]}
    # Halo spikes deploy at 0.6s pointing inward
    for i, (gu, g) in enumerate(spike_groups):
        delay = 0.6 + i * 0.015
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.15, 1.1, 1.1, 1.1),
            make_keyframe("scale", 0.8, 1.0, 1.0, 1.0),
        ]}
    # Ornaments fly into orbit at 0.4s
    for i, (gu, g, ang, rr) in enumerate(orn_groups):
        delay = 0.4 + i * 0.02
        far_x = math.cos(ang) * (rr + 4)
        far_z = math.sin(ang) * (rr + 4)
        cx = math.cos(ang) * rr
        cz = math.sin(ang) * rr
        ox = far_x - cx
        oz = far_z - cz
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, ox, 0, oz),
            make_keyframe("position", delay, ox, 0, oz),
            make_keyframe("position", delay + 0.2, 0, 0, 0),
            make_keyframe("position", 0.8, 0, 0, 0),
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.15, 1.0, 1.0, 1.0),
            make_keyframe("scale", 0.8, 1.0, 1.0, 1.0),
        ]}
    b.add_animation("spawn", length=0.8, animators=spawn, loop="once", override=True)

    # ----- IDLE: 5.0s loop -----
    idle = {}
    # All 3 rings rotate — inner fastest, outer slowest
    # Outer: 6s/rotation, mid: 4s, inner: 2.5s
    # Plus: slow contraction (scale 1.0 -> 0.95 -> 1.0 seamless)
    # Outer
    outer_kfs = []
    for k in range(20):
        t = (k / 19) * 5.0
        ang_deg = (t / 6.0) * 360
        outer_kfs.append(make_keyframe("rotation", t, 0, ang_deg, 0))
    for k in range(14):
        t = (k / 13) * 5.0
        # contract 1.0 -> 0.95 -> 1.0 seamless
        s = 0.975 + 0.025 * math.cos(t * 2 * math.pi / 5.0)
        outer_kfs.append(make_keyframe("scale", t, s, 1.0, s))
    idle[outer_uuid] = {"name": "outer_halo", "type": "bone", "keyframes": outer_kfs}

    mid_kfs = []
    for k in range(20):
        t = (k / 19) * 5.0
        ang_deg = (t / 4.0) * 360
        mid_kfs.append(make_keyframe("rotation", t, 0, ang_deg, 0))
    for k in range(14):
        t = (k / 13) * 5.0
        s = 0.965 + 0.035 * math.cos(t * 2 * math.pi / 5.0)
        mid_kfs.append(make_keyframe("scale", t, s, 1.0, s))
    idle[mid_uuid] = {"name": "mid_ring", "type": "bone", "keyframes": mid_kfs}

    inner_kfs = []
    for k in range(20):
        t = (k / 19) * 5.0
        ang_deg = (t / 2.5) * 360
        inner_kfs.append(make_keyframe("rotation", t, 0, ang_deg, 0))
    for k in range(14):
        t = (k / 13) * 5.0
        s = 0.95 + 0.05 * math.cos(t * 2 * math.pi / 5.0)
        inner_kfs.append(make_keyframe("scale", t, s, 1.0, s))
    idle[inner_uuid] = {"name": "inner_ring", "type": "bone", "keyframes": inner_kfs}

    # Halo spikes pulse scale (flex inward slightly each pulse)
    for i, (gu, g) in enumerate(spike_groups):
        kfs = []
        for k in range(16):
            t = (k / 15) * 5.0
            # X scale shrinks/grows as if pressing inward
            sx = 1.0 + 0.15 * math.sin(t * 2 * math.pi / 1.5 + i * 0.4)
            kfs.append(make_keyframe("scale", t, sx, 1.0, 1.0))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Ornaments orbit between rings — rotate around centre via position swap
    for i, (gu, g, ang, rr) in enumerate(orn_groups):
        kfs = []
        period = 4.0
        for k in range(16):
            t = (k / 15) * 5.0
            # Orbit angle — different from ring rotation
            local_ang = (t / period) * 2 * math.pi + i * 0.3
            # New position relative to group's local origin (which is at this ornament's initial spot)
            # Calculate offset from initial — this lets ornament drift around
            new_cx = math.cos(ang + local_ang * 0.3) * rr
            new_cz = math.sin(ang + local_ang * 0.3) * rr
            init_cx = math.cos(ang) * rr
            init_cz = math.sin(ang) * rr
            ox = new_cx - init_cx
            oz = new_cz - init_cz
            kfs.append(make_keyframe("position", t, ox, 0, oz))
            # Spin the diamond
            kfs.append(make_keyframe("rotation", t, t * 90, 0, t * 60))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Ground impression slow pulse
    ground_idle = []
    for k in range(14):
        t = (k / 13) * 5.0
        s = 1.0 + 0.04 * math.sin(t * 2 * math.pi / 5.0)
        ground_idle.append(make_keyframe("scale", t, s, 1.0, s))
    idle[ground_uuid] = {"name": "ground_impression", "type": "bone", "keyframes": ground_idle}

    b.add_animation("idle", length=5.0, animators=idle, loop="loop", override=False)

    # ----- DISSIPATE: 0.6s — all rings collapse inward -----
    diss = {}
    diss[outer_uuid] = {"name": "outer_halo", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 1, 1, 1),
        make_keyframe("scale", 0.4, 0.4, 1, 0.4),
        make_keyframe("scale", 0.6, 0, 0, 0),
    ]}
    diss[mid_uuid] = {"name": "mid_ring", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 1, 1, 1),
        make_keyframe("scale", 0.35, 0.3, 1, 0.3),
        make_keyframe("scale", 0.6, 0, 0, 0),
    ]}
    diss[inner_uuid] = {"name": "inner_ring", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 1, 1, 1),
        make_keyframe("scale", 0.3, 0.2, 1, 0.2),
        make_keyframe("scale", 0.6, 0, 0, 0),
    ]}
    # Halo spikes follow inward
    for i, (gu, g) in enumerate(spike_groups):
        ang = (i / 8.0) * 2 * math.pi
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.5, -math.cos(ang) * 8, 0, -math.sin(ang) * 8),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.6, 0, 0, 0),
        ]}
    # Ornaments converge on centre
    for i, (gu, g, ang, rr) in enumerate(orn_groups):
        cx = math.cos(ang) * rr
        cz = math.sin(ang) * rr
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.4, -cx * 0.8, 0, -cz * 0.8),
            make_keyframe("position", 0.6, -cx, 0, -cz),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.5, 1.2, 1.2, 1.2),
            make_keyframe("scale", 0.6, 0, 0, 0),
        ]}
    diss[ground_uuid] = {"name": "ground_impression", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 1, 1, 1),
        make_keyframe("scale", 0.6, 0, 0, 0),
    ]}
    b.add_animation("dissipate", length=0.6, animators=diss, loop="once", override=True)

    b.write(OUTPUT_PATH)
    print(f"Wrote {OUTPUT_PATH}")


if __name__ == "__main__":
    build()
