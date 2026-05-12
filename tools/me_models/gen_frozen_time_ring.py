#!/usr/bin/env python3
"""Generator for frozen_time_ring.bbmodel — Standstill.

3 concentric horizontal rings at Y=0.5: inner contracts inward (collapsing). Each 14 slabs.
Ring leading edge: 14 thin upright slabs angled forward.
Freeze crystals: 10 small flat diamonds materialising in wake.
Centre detonation: 4 overlapping flat slabs at Y=0.5 (texture 1).
Ground impression: 6 flat slabs at Y=0 inside inner ring (texture 1).
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
    specular_cluster,
    base_fill,
)

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/freezingice/me_attacks/frozen_time_ring.bbmodel"
RES = 128


def build_frozen_moment_texture():
    """FROZEN MOMENT — directional left dark -> right bright leading edge."""
    w = h = RES
    void    = hex_to_rgba("#020810")
    navy    = hex_to_rgba("#061828")
    cold    = hex_to_rgba("#103060")
    edge    = hex_to_rgba("#80c8f0")
    emiss   = hex_to_rgba("#c0e8ff")

    pixels = base_fill(w, h, void)

    # Horizontal directional gradient: left = void, right = emissive
    for y in range(h):
        for x in range(w):
            t = x / (w - 1)
            if t < 0.35:
                c = color_lerp(void, navy, t / 0.35)
            elif t < 0.7:
                c = color_lerp(navy, cold, (t - 0.35) / 0.35)
            elif t < 0.92:
                c = color_lerp(cold, edge, (t - 0.7) / 0.22)
            else:
                c = color_lerp(edge, emiss, (t - 0.92) / 0.08)
            pixels[y * w + x] = c

    # Bright vertical streak at the leading edge (col 60+ in 64-res; 120+ in 128)
    leading_col = int(w * 0.94)
    for y in range(h):
        for dx in range(-2, 3):
            x = leading_col + dx
            if 0 <= x < w:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, emiss, 0.8)

    # Specular emissive cluster on the right
    specular_cluster(pixels, w, h, int(w * 0.95), h // 2, 6, emiss, strength=1.0)

    # Some vertical streaks of cold motion (frozen time)
    import random
    rng = random.Random(33)
    for _ in range(28):
        x = rng.randrange(w)
        y0 = rng.randrange(h - 6)
        for y in range(y0, y0 + 6):
            if 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, edge, 0.25)

    add_noise_overlay(pixels, w, h, navy, edge, density=0.16, seed=77)
    jitter_inplace(pixels, jitter_range=18, seed=141)
    jitter_inplace(pixels, jitter_range=12, seed=201, density=0.45)

    return png_from_pixels(pixels, w, h)


def build_detonation_texture():
    """pure emissive ice-white centre, fading to near-black."""
    w = h = RES
    centre = hex_to_rgba("#e0f4ff")
    mid    = hex_to_rgba("#2060a0")
    outer  = hex_to_rgba("#04101a")

    pixels = base_fill(w, h, outer)
    cx, cy = w // 2, h // 2
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            t = min(1.0, d / (w * 0.5))
            if t < 0.3:
                c = color_lerp(centre, mid, t / 0.3)
            else:
                c = color_lerp(mid, outer, (t - 0.3) / 0.7)
            pixels[y * w + x] = c

    add_noise_overlay(pixels, w, h, outer, centre, density=0.16, seed=88)
    jitter_inplace(pixels, jitter_range=16, seed=303)
    jitter_inplace(pixels, jitter_range=10, seed=441, density=0.5)

    return png_from_pixels(pixels, w, h)


def build():
    b = Builder("frozen_time_ring", resolution=(RES, RES), visible_box=(16, 4, 0))
    b.add_texture("frozen_moment", build_frozen_moment_texture())
    b.add_texture("detonation", build_detonation_texture())

    fm_face = lambda: uniform_face(0, 0, RES, RES, tex_index=0)
    # Leading-edge face: right portion of texture
    fm_lead_face = lambda: uniform_face(int(RES * 0.7), 0, RES, RES, tex_index=0)
    # Trailing face: left portion
    fm_trail_face = lambda: uniform_face(0, 0, int(RES * 0.3), RES, tex_index=0)
    det_face = lambda: uniform_face(0, 0, RES, RES, tex_index=1)

    # 3 concentric rings at Y=0.5
    ring_radii = [3.0, 6.0, 9.0]
    rings = []  # list of (ring_idx, [(uuid, group, ang), ...])
    for r_idx, r in enumerate(ring_radii):
        slabs = []
        for i in range(14):
            ang = (i / 14.0) * 2 * math.pi
            cx = math.cos(ang) * r
            cz = math.sin(ang) * r
            # Use leading-edge-aware face — for inner rings use trail, outer uses lead
            face = fm_lead_face() if r_idx == 2 else (fm_face() if r_idx == 1 else fm_trail_face())
            cube_u = b.add_cube(
                f"ring{r_idx+1}_{i+1}",
                from_=[-1.0, 0, -0.25], to_=[1.0, 0.9, 0.25],
                faces=face,
            )
            gu = make_uuid()
            g = b.make_group(
                f"ring{r_idx+1}_slab{i+1}", [cx, 0.5, cz], [cube_u],
                rotation=[0, math.degrees(ang) + 90, 0], group_uuid=gu,
            )
            slabs.append((gu, g, ang))
        rings.append((r_idx, slabs))

    # Leading edge — 14 thin upright pieces on outer ring perimeter, angled forward
    leading_groups = []
    for i in range(14):
        ang = (i / 14.0) * 2 * math.pi
        r = 9.5
        cx = math.cos(ang) * r
        cz = math.sin(ang) * r
        cube_u = b.add_cube(
            f"lead_{i+1}",
            from_=[-0.7, 0, -0.1], to_=[0.7, 1.3, 0.1],
            faces=fm_lead_face(),
        )
        gu = make_uuid()
        # angled forward (radial outward tilt)
        rot_y = math.degrees(ang) + 90
        g = b.make_group(
            f"leading_edge_{i+1}", [cx, 0.5, cz], [cube_u],
            rotation=[-15, rot_y, 0], group_uuid=gu,
        )
        leading_groups.append((gu, g, ang))

    # 10 freeze crystals — small flat diamond pieces in wake
    crystal_groups = []
    for i in range(10):
        ang = (i / 10.0) * 2 * math.pi + 0.2
        r = 8.0
        cx = math.cos(ang) * r
        cz = math.sin(ang) * r
        cube_u = b.add_cube(
            f"crystal_{i+1}",
            from_=[-0.45, 0, -0.45], to_=[0.45, 0.8, 0.45],
            faces=fm_face(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"freeze_crystal_{i+1}", [cx, 0.5, cz], [cube_u],
            rotation=[0, math.degrees(ang) + 45, 45], group_uuid=gu,
        )
        crystal_groups.append((gu, g, ang))

    # Centre detonation — 4 overlapping flat slabs at Y=0.5
    deto_cubes = []
    deto_cubes.append(b.add_cube("deto_main", from_=[-1.4, 0.4, -1.4], to_=[1.4, 0.7, 1.4], faces=det_face()))
    deto_cubes.append(b.add_cube("deto_b", from_=[-1.1, 0.5, -1.6], to_=[1.1, 0.8, 1.6], faces=det_face()))
    deto_cubes.append(b.add_cube("deto_c", from_=[-1.6, 0.5, -1.1], to_=[1.6, 0.8, 1.1], faces=det_face()))
    deto_cubes.append(b.add_cube("deto_d", from_=[-0.8, 0.6, -0.8], to_=[0.8, 1.0, 0.8], faces=det_face()))
    deto_uuid = make_uuid()
    deto_g = b.make_group("centre_detonation", [0, 0.5, 0], deto_cubes, group_uuid=deto_uuid)

    # Ground impression — 6 flat slabs at Y=0 inside inner ring
    impress_groups = []
    for i in range(6):
        ang = (i / 6.0) * 2 * math.pi
        r = 1.5
        cx = math.cos(ang) * r
        cz = math.sin(ang) * r
        cube_u = b.add_cube(
            f"impress_{i+1}",
            from_=[-0.8, -0.05, -0.5], to_=[0.8, 0.10, 0.5],
            faces=det_face(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"ground_impression_{i+1}", [cx, 0.05, cz], [cube_u],
            rotation=[0, math.degrees(ang), 0], group_uuid=gu,
        )
        impress_groups.append((gu, g))

    root_children = []
    for (r_idx, slabs) in rings:
        for (gu, g, ang) in slabs:
            root_children.append(g)
    for (gu, g, ang) in leading_groups: root_children.append(g)
    for (gu, g, ang) in crystal_groups: root_children.append(g)
    root_children.append(deto_g)
    for (gu, g) in impress_groups: root_children.append(g)
    root_uuid = make_uuid()
    b.outliner.append(b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid))

    # SPAWN 1.2s — rings start at scale 2.0, contract inward over 0.7s
    spawn = {}
    for (r_idx, slabs) in rings:
        target_r = ring_radii[r_idx]
        for (gu, g, ang) in slabs:
            # Start at radius 2.0 * target (outward); contract to 1.0
            init_cx = math.cos(ang) * target_r * 2.0
            init_cz = math.sin(ang) * target_r * 2.0
            true_cx = math.cos(ang) * target_r
            true_cz = math.sin(ang) * target_r
            kfs = [
                make_keyframe("position", 0.0, init_cx - true_cx, 0, init_cz - true_cz),
                make_keyframe("position", 0.7, 0, 0, 0),
                make_keyframe("position", 1.2, 0, 0, 0),
                make_keyframe("scale", 0.0, 1, 1, 1),
                make_keyframe("scale", 1.2, 1, 1, 1),
            ]
            spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Centre detonation materialises at 0.7s
    spawn[deto_uuid] = {"name": "centre_detonation", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 0, 0, 0),
        make_keyframe("scale", 0.7, 0, 0, 0),
        make_keyframe("scale", 0.85, 1.3, 1.3, 1.3),
        make_keyframe("scale", 1.0, 1, 1, 1),
        make_keyframe("scale", 1.2, 1, 1, 1),
    ]}

    # Leading edge reveals at 0.7s
    for idx, (gu, g, ang) in enumerate(leading_groups):
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", 0.7, 0, 0, 0),
            make_keyframe("scale", 0.85, 1.1, 1.1, 1.1),
            make_keyframe("scale", 1.0, 1, 1, 1),
            make_keyframe("scale", 1.2, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Freeze crystals materialise in wake — stagger 0.4-1.0s
    for idx, (gu, g, ang) in enumerate(crystal_groups):
        start = 0.4 + idx * 0.06
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", start, 0, 0, 0),
            make_keyframe("scale", start + 0.10, 1, 1, 1),
            make_keyframe("scale", 1.2, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Ground impression at 0.85s
    for idx, (gu, g) in enumerate(impress_groups):
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", 0.85, 0, 0, 0),
            make_keyframe("scale", 1.0, 1, 1, 1),
            make_keyframe("scale", 1.2, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("spawn", length=1.2, animators=spawn, loop="once", override=True)

    # IDLE 5.0s — rings rotate slowly at diff rates
    idle = {}
    for (r_idx, slabs) in rings:
        rotation_speed = (r_idx + 1) * 12  # degrees per 5s
        target_r = ring_radii[r_idx]
        for (gu, g, ang) in slabs:
            kfs = []
            for k in range(13):
                t = (k / 12) * 5.0
                new_ang = ang + (t / 5.0) * math.radians(rotation_speed)
                new_cx = math.cos(new_ang) * target_r
                new_cz = math.sin(new_ang) * target_r
                init_cx = math.cos(ang) * target_r
                init_cz = math.sin(ang) * target_r
                kfs.append(make_keyframe("position", t, new_cx - init_cx, 0, new_cz - init_cz))
            idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Leading edge pulses emissive in wave
    for idx, (gu, g, ang) in enumerate(leading_groups):
        kfs = []
        for k in range(11):
            t = (k / 10) * 5.0
            ph = (t / 4.0 - idx * 0.15) * 2 * math.pi
            s = 1.0 + 0.10 * (1 - math.cos(ph))
            kfs.append(make_keyframe("scale", t, s, s, s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Freeze crystals drift outward slowly
    for idx, (gu, g, ang) in enumerate(crystal_groups):
        kfs = []
        for k in range(6):
            t = (k / 5) * 5.0
            drift = t * 0.15
            kfs.append(make_keyframe("position", t,
                                     math.cos(ang) * drift, 0, math.sin(ang) * drift))
            kfs.append(make_keyframe("rotation", t, 0, idx * 36 + t * 30, 45 + t * 8))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Centre detonation breathes
    idle[deto_uuid] = {"name": "centre_detonation", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 1, 1, 1),
        make_keyframe("scale", 1.25, 1.10, 1.05, 1.10),
        make_keyframe("scale", 2.5, 1, 1, 1),
        make_keyframe("scale", 3.75, 1.10, 1.05, 1.10),
        make_keyframe("scale", 5.0, 1, 1, 1),
    ]}

    for idx, (gu, g) in enumerate(impress_groups):
        kfs = []
        for k in range(7):
            t = (k / 6) * 5.0
            ph = (t / 4 + idx * 0.3) * 2 * math.pi
            s = 1.0 + 0.05 * (1 - math.cos(ph))
            kfs.append(make_keyframe("scale", t, s, 1, s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("idle", length=5.0, animators=idle, loop="loop", override=False)

    # DISSIPATE 0.5s — rings expand outward (inverse of spawn)
    diss = {}
    for (r_idx, slabs) in rings:
        target_r = ring_radii[r_idx]
        for (gu, g, ang) in slabs:
            true_cx = math.cos(ang) * target_r
            true_cz = math.sin(ang) * target_r
            final_cx = math.cos(ang) * target_r * 2.5
            final_cz = math.sin(ang) * target_r * 2.5
            kfs = [
                make_keyframe("position", 0.0, 0, 0, 0),
                make_keyframe("position", 0.5, final_cx - true_cx, 0, final_cz - true_cz),
                make_keyframe("scale", 0.0, 1, 1, 1),
                make_keyframe("scale", 0.4, 1, 1, 1),
                make_keyframe("scale", 0.5, 0, 0, 0),
            ]
            diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    diss[deto_uuid] = {"name": "centre_detonation", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 1, 1, 1),
        make_keyframe("scale", 0.3, 1.8, 1.8, 1.8),
        make_keyframe("scale", 0.5, 0, 0, 0),
    ]}

    for idx, (gu, g, ang) in enumerate(crystal_groups):
        kfs = [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.5, math.cos(ang) * 4, 0, math.sin(ang) * 4),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.5, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g, ang) in enumerate(leading_groups):
        kfs = [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.5, math.cos(ang) * 3, 0, math.sin(ang) * 3),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.5, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g) in enumerate(impress_groups):
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.5, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("dissipate", length=0.5, animators=diss, loop="once", override=True)

    b.write(OUTPUT_PATH)
    import os
    sz = os.path.getsize(OUTPUT_PATH)
    print(f"Wrote {OUTPUT_PATH} ({sz} bytes, {sz/1024:.1f} KB)")


if __name__ == "__main__":
    build()
