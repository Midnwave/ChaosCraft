#!/usr/bin/env python3
"""Generator for cryo_pillar_cross.bbmodel — Cardinal Points.

4 pillars at N/S/E/W (radius 8), heights 10,14,10,12. Each 4 stacked segments.
4 horizontal connecting beams. 2 crown caps per pillar. 10 ground frost slabs.
4 base rupture slabs (1 per pillar).
"""

import sys
import math

sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder,
    make_uuid,
    make_keyframe,
    uniform_face,
    png_from_pixels,
    hex_to_rgba,
    color_lerp,
    add_noise_overlay,
)
from _freezingice_helpers import (
    jitter_inplace,
    annual_bands,
    specular_cluster,
    base_fill,
)

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/freezingice/me_attacks/cryo_pillar_cross.bbmodel"
RES = 128


def build_strata_texture():
    """ICE COLUMN STRATA — alternating shaded horizontal bands every 8 rows."""
    w = h = RES
    deep    = hex_to_rgba("#040c20")
    s1      = hex_to_rgba("#081830")
    s2      = hex_to_rgba("#0c2848")
    s3      = hex_to_rgba("#183870")
    s4      = hex_to_rgba("#204888")
    high    = hex_to_rgba("#60b8f0")
    spec    = hex_to_rgba("#e8f4ff")

    pixels = base_fill(w, h, deep)

    # Cycling strata every 16 rows (8x2 for 128 res)
    strata_cycle = [s1, s2, s3, s4]
    period = 16
    for y in range(h):
        idx = (y // period) % len(strata_cycle)
        c = strata_cycle[idx]
        for x in range(w):
            pixels[y * w + x] = c

    # Bright highlight line at each stratum boundary
    for y in range(0, h, period):
        for x in range(w):
            p = pixels[y * w + x]
            pixels[y * w + x] = color_lerp(p, high, 0.50)
        if y + 1 < h:
            for x in range(w):
                p = pixels[(y + 1) * w + x]
                pixels[(y + 1) * w + x] = color_lerp(p, high, 0.25)

    # Frost-white specular at (32,4) → scale to (64,8) for 128
    specular_cluster(pixels, w, h, RES // 2, 8, 5, spec, strength=0.9)

    # Faint emissive at brightest strata (s4 zones)
    for y in range(h):
        idx = (y // period) % len(strata_cycle)
        if strata_cycle[idx] == s4:
            for x in range(w):
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, high, 0.18)

    add_noise_overlay(pixels, w, h, deep, high, density=0.16, seed=33)
    jitter_inplace(pixels, jitter_range=18, seed=99)
    jitter_inplace(pixels, jitter_range=12, seed=121, density=0.45)

    return png_from_pixels(pixels, w, h)


def build_ground_radial_texture():
    """silver-frost radial from each pillar base."""
    w = h = RES
    centre = hex_to_rgba("#c0e8ff")
    mid    = hex_to_rgba("#4080a0")
    outer  = hex_to_rgba("#04101a")

    pixels = base_fill(w, h, outer)

    cx, cy = w // 2, h // 2
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            t = min(1.0, d / (w * 0.5))
            if t < 0.4:
                c = color_lerp(centre, mid, t / 0.4)
            else:
                c = color_lerp(mid, outer, (t - 0.4) / 0.6)
            pixels[y * w + x] = c

    add_noise_overlay(pixels, w, h, outer, centre, density=0.16, seed=88)
    jitter_inplace(pixels, jitter_range=18, seed=311)
    jitter_inplace(pixels, jitter_range=12, seed=417, density=0.5)

    return png_from_pixels(pixels, w, h)


def build():
    b = Builder("cryo_pillar_cross", resolution=(RES, RES), visible_box=(18, 14, 0))
    b.add_texture("strata", build_strata_texture())
    b.add_texture("ground", build_ground_radial_texture())

    strata_face = lambda: uniform_face(0, 0, RES, RES, tex_index=0)
    ground_face = lambda: uniform_face(0, 0, RES, RES, tex_index=1)

    # 4 pillars at N/S/E/W (radius 8)
    pillar_positions = [
        ("N", 0, -8, 10),
        ("S", 0, 8, 14),
        ("E", 8, 0, 10),
        ("W", -8, 0, 12),
    ]
    pillar_groups = []  # (pillar_dir, [(seg_uuid, seg_g)], top_pos)
    crown_groups = []
    base_rupture = []
    for (name, px, pz, height) in pillar_positions:
        segs = []
        seg_h = height / 4
        # widths taper: 3x3 -> 2x2
        widths = [3.0, 2.7, 2.4, 2.0]
        for s in range(4):
            y0 = s * seg_h
            y1 = (s + 1) * seg_h
            hw = widths[s] / 2
            cube_u = b.add_cube(
                f"pillar_{name}_seg{s+1}",
                from_=[-hw, y0, -hw], to_=[hw, y1, hw],
                faces=strata_face(),
            )
            gu = make_uuid()
            g = b.make_group(
                f"pillar_{name}_s{s+1}", [px, (y0 + y1) / 2, pz], [cube_u],
                group_uuid=gu,
            )
            segs.append((gu, g))
        pillar_groups.append((name, segs, height, px, pz))

        # Crown — 2 small slab caps at top, angled outward
        for c in range(2):
            ang = c * math.pi  # 0 and 180 — opposing
            crown_cu = b.add_cube(
                f"crown_{name}_{c+1}",
                from_=[-1.4, 0, -0.5], to_=[1.4, 0.4, 0.5],
                faces=strata_face(),
            )
            cgu = make_uuid()
            cg = b.make_group(
                f"crown_{name}_{c+1}", [px, height, pz], [crown_cu],
                rotation=[10, math.degrees(ang) + (0 if name in ("N", "S") else 90), 0],
                group_uuid=cgu,
            )
            crown_groups.append((cgu, cg))

        # Base rupture slab
        rupture_cu = b.add_cube(
            f"rupture_{name}",
            from_=[-2.0, -0.05, -1.0], to_=[2.0, 0.10, 1.0],
            faces=ground_face(),
        )
        rgu = make_uuid()
        rg = b.make_group(
            f"base_rupture_{name}", [px, 0.05, pz], [rupture_cu],
            group_uuid=rgu,
        )
        base_rupture.append((rgu, rg))

    # 4 connecting beams between adjacent pillar tops (N-E, E-S, S-W, W-N)
    pairs = [("N", "E"), ("E", "S"), ("S", "W"), ("W", "N")]
    beam_groups = []
    pillar_dict = {p[0]: p for p in pillar_positions}
    for (a, b_n) in pairs:
        _, ax, az, ah = pillar_dict[a]
        _, bx, bz, bh = pillar_dict[b_n]
        mx = (ax + bx) / 2
        mz = (az + bz) / 2
        avg_h = (ah + bh) / 2 + 0.5
        dx = bx - ax
        dz = bz - az
        length = math.sqrt(dx * dx + dz * dz)
        ang = math.atan2(dz, dx)
        beam_cu = b.add_cube(
            f"beam_{a}_{b_n}",
            from_=[-length / 2, -0.4, -0.5], to_=[length / 2, 0.4, 0.5],
            faces=strata_face(),
        )
        bgu = make_uuid()
        bg = b.make_group(
            f"beam_{a}_{b_n}", [mx, avg_h, mz], [beam_cu],
            rotation=[0, math.degrees(ang), 0], group_uuid=bgu,
        )
        beam_groups.append((bgu, bg))

    # Ground frost circle — 10 slabs between pillars (radius ~6)
    frost_groups = []
    for i in range(10):
        ang = (i / 10.0) * 2 * math.pi
        r = 6.0
        cx = math.cos(ang) * r
        cz = math.sin(ang) * r
        cube_u = b.add_cube(
            f"gf_{i+1}",
            from_=[-1.2, 0.02, -0.5], to_=[1.2, 0.08, 0.5],
            faces=ground_face(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"ground_frost_{i+1}", [cx, 0.05, cz], [cube_u],
            rotation=[0, math.degrees(ang), 0], group_uuid=gu,
        )
        frost_groups.append((gu, g))

    root_children = []
    for (name, segs, h, px, pz) in pillar_groups:
        for (gu, g) in segs:
            root_children.append(g)
    for (gu, g) in crown_groups: root_children.append(g)
    for (gu, g) in base_rupture: root_children.append(g)
    for (gu, g) in beam_groups: root_children.append(g)
    for (gu, g) in frost_groups: root_children.append(g)
    root_uuid = make_uuid()
    b.outliner.append(b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid))

    # SPAWN 1.2s
    spawn = {}
    # Base rupture at 0.1s
    for idx, (gu, g) in enumerate(base_rupture):
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", 0.1, 0, 0, 0),
            make_keyframe("scale", 0.25, 1.3, 1.0, 1.3),
            make_keyframe("scale", 0.35, 1, 1, 1),
            make_keyframe("scale", 1.2, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # All 4 pillars erupt simultaneously — segments stagger 0.12s
    for p_idx, (name, segs, h, px, pz) in enumerate(pillar_groups):
        for s_idx, (gu, g) in enumerate(segs):
            start = 0.15 + s_idx * 0.12
            kfs = [
                make_keyframe("scale", 0.0, 1, 0, 1),
                make_keyframe("scale", start, 1, 0, 1),
                make_keyframe("scale", start + 0.10, 1.05, 1.1, 1.05),
                make_keyframe("scale", start + 0.20, 1, 1, 1),
                make_keyframe("scale", 1.2, 1, 1, 1),
            ]
            spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Ground frost circle expands at 0.2s
    for idx, (gu, g) in enumerate(frost_groups):
        start = 0.2 + idx * 0.02
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", start, 0, 0, 0),
            make_keyframe("scale", start + 0.12, 1, 1, 1),
            make_keyframe("scale", 1.2, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Beams slide in horizontally at 0.6s (each from outside)
    for idx, (gu, g) in enumerate(beam_groups):
        start = 0.6 + idx * 0.05
        kfs = [
            make_keyframe("scale", 0.0, 0, 1, 1),
            make_keyframe("position", 0.0, 8, 0, 0),
            make_keyframe("position", start, 8, 0, 0),
            make_keyframe("position", start + 0.15, 0, 0, 0),
            make_keyframe("position", 1.2, 0, 0, 0),
            make_keyframe("scale", start, 0, 1, 1),
            make_keyframe("scale", start + 0.15, 1, 1, 1),
            make_keyframe("scale", 1.2, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Crown pieces deploy last
    for idx, (gu, g) in enumerate(crown_groups):
        start = 0.95 + (idx % 4) * 0.04
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", start, 0, 0, 0),
            make_keyframe("scale", start + 0.10, 1.1, 1.1, 1.1),
            make_keyframe("scale", min(1.2, start + 0.20), 1, 1, 1),
            make_keyframe("scale", 1.2, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("spawn", length=1.2, animators=spawn, loop="once", override=True)

    # IDLE 6.0s — N-S lean together, E-W lean together
    idle = {}
    # NS pair sway X (rotation around root won't work per segment; lean each pillar)
    for p_idx, (name, segs, h, px, pz) in enumerate(pillar_groups):
        # Group all segments same lean angle
        for s_idx, (gu, g) in enumerate(segs):
            kfs = []
            for k in range(13):
                t = (k / 12) * 6.0
                ph = (t / 6.0) * 2 * math.pi
                if name == "N":
                    rx = -math.sin(ph) * 1.2 * (s_idx + 1) * 0.3
                    rz = 0
                elif name == "S":
                    rx = math.sin(ph) * 1.2 * (s_idx + 1) * 0.3
                    rz = 0
                elif name == "E":
                    rx = 0
                    rz = -math.sin(ph + math.pi / 2) * 1.2 * (s_idx + 1) * 0.3
                else:  # W
                    rx = 0
                    rz = math.sin(ph + math.pi / 2) * 1.2 * (s_idx + 1) * 0.3
                kfs.append(make_keyframe("rotation", t, rx, 0, rz))
            idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Beams pulse emissive (scale)
    for idx, (gu, g) in enumerate(beam_groups):
        kfs = []
        for k in range(11):
            t = (k / 10) * 6.0
            ph = (t / 3.0 + idx * 0.5) * 2 * math.pi
            s = 1.0 + 0.05 * (1 - math.cos(ph))
            kfs.append(make_keyframe("scale", t, s, s, s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Crowns rotate slowly
    for idx, (gu, g) in enumerate(crown_groups):
        kfs = []
        for k in range(7):
            t = (k / 6) * 6.0
            ry = t * 8 + idx * 30
            kfs.append(make_keyframe("rotation", t, 10, ry, 0))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Ground frost breathes
    for idx, (gu, g) in enumerate(frost_groups):
        kfs = []
        for k in range(7):
            t = (k / 6) * 6.0
            ph = (t / 5 + idx * 0.3) * 2 * math.pi
            s = 1.0 + 0.06 * (1 - math.cos(ph))
            kfs.append(make_keyframe("scale", t, s, 1, s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("idle", length=6.0, animators=idle, loop="loop", override=False)

    # DISSIPATE 1.0s — beams retract, crowns fly out, pillars crumble top-to-bottom
    diss = {}
    for idx, (gu, g) in enumerate(beam_groups):
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.2, 0, 1, 1),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g) in enumerate(crown_groups):
        ang = idx * (math.pi / 4)
        kfs = [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.4, math.cos(ang) * 5, 2, math.sin(ang) * 5),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.4, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for p_idx, (name, segs, h, px, pz) in enumerate(pillar_groups):
        # Crumble top-to-bottom — top seg first
        for s_idx, (gu, g) in enumerate(segs):
            # reverse: top (s_idx=3) first
            start = (3 - s_idx) * 0.18
            kfs = [
                make_keyframe("scale", 0.0, 1, 1, 1),
                make_keyframe("scale", start, 1, 1, 1),
                make_keyframe("scale", start + 0.15, 0, 0, 0),
                make_keyframe("scale", 1.0, 0, 0, 0),
            ]
            diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g) in enumerate(frost_groups):
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 1.0, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g) in enumerate(base_rupture):
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 1.0, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("dissipate", length=1.0, animators=diss, loop="once", override=True)

    b.write(OUTPUT_PATH)
    import os
    sz = os.path.getsize(OUTPUT_PATH)
    print(f"Wrote {OUTPUT_PATH} ({sz} bytes, {sz/1024:.1f} KB)")


if __name__ == "__main__":
    build()
