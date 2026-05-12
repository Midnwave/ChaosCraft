#!/usr/bin/env python3
"""Generator for glacier_fist_eruption.bbmodel — Weight of Ages.

5 fingers (thumb-pinky) each 3 segments, heights 7,11,12,10,8.
Palm base = 4 cubes. 5 knuckle ridge slabs. 10 rupture ring slabs. 4 ice fog slabs.
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

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/freezingice/me_attacks/glacier_fist_eruption.bbmodel"
RES = 160


def build_glacier_texture():
    """ANCIENT GLACIER — opaque milky blue with horizontal annual bands."""
    w = h = RES
    deep    = hex_to_rgba("#0a1828")
    ancient = hex_to_rgba("#183858")
    compr   = hex_to_rgba("#2860a0")
    milky   = hex_to_rgba("#78b8e0")
    spec    = hex_to_rgba("#d0f0ff")
    emiss   = hex_to_rgba("#60b0f0")

    pixels = base_fill(w, h, deep)

    # Very gradual transitions (no hard edges)
    for y in range(h):
        t = y / (h - 1)
        if t < 0.25:
            c = color_lerp(deep, ancient, t / 0.25)
        elif t < 0.55:
            c = color_lerp(ancient, compr, (t - 0.25) / 0.30)
        elif t < 0.85:
            c = color_lerp(compr, milky, (t - 0.55) / 0.30)
        else:
            c = color_lerp(milky, spec, (t - 0.85) / 0.15)
        for x in range(w):
            pixels[y * w + x] = c

    # Horizontal annual banding every 24 rows (12 in 64, x2 for 128 res)
    annual_bands(pixels, w, h, 24, ancient, strength=0.30)
    annual_bands(pixels, w, h, 24, deep, strength=0.20)

    # Faint emissive glow at brightest band
    for y in range(int(h * 0.30), int(h * 0.45)):
        for x in range(w):
            p = pixels[y * w + x]
            pixels[y * w + x] = color_lerp(p, emiss, 0.18)

    # Trapped-bubble specular dots
    import random
    rng = random.Random(45)
    for _ in range(36):
        cx = rng.randrange(w)
        cy = rng.randrange(h)
        specular_cluster(pixels, w, h, cx, cy, 2, spec, strength=0.65)

    add_noise_overlay(pixels, w, h, deep, milky, density=0.15, seed=88)
    jitter_inplace(pixels, jitter_range=20, seed=199)
    jitter_inplace(pixels, jitter_range=12, seed=237, density=0.45)

    return png_from_pixels(pixels, w, h)


def build_ground_crack_texture():
    """COLD GROUND CRACK — dark earth with blue cracks."""
    w = h = RES
    earth = hex_to_rgba("#080c10")
    crack = hex_to_rgba("#3070a0")
    fog   = hex_to_rgba("#a0c8e0")

    pixels = base_fill(w, h, earth)

    # Random irregular crack pattern
    import random
    rng = random.Random(101)
    for _ in range(60):
        x0 = rng.randrange(w)
        y0 = rng.randrange(h)
        length = rng.randrange(8, 32)
        ang = rng.random() * 2 * math.pi
        for r in range(length):
            x = int(x0 + math.cos(ang) * r)
            y = int(y0 + math.sin(ang) * r)
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, crack, 0.7)

    # Centre fog zone
    specular_cluster(pixels, w, h, w // 2, h // 2, 18, fog, strength=0.4)

    add_noise_overlay(pixels, w, h, earth, crack, density=0.20, seed=33)
    jitter_inplace(pixels, jitter_range=16, seed=409)
    jitter_inplace(pixels, jitter_range=10, seed=507, density=0.5)

    return png_from_pixels(pixels, w, h)


def build():
    b = Builder("glacier_fist_eruption", resolution=(RES, RES), visible_box=(10, 14, 0))
    b.add_texture("glacier", build_glacier_texture())
    b.add_texture("ground", build_ground_crack_texture())

    glacier_face = lambda: uniform_face(0, 0, RES, RES, tex_index=0)
    ground_face = lambda: uniform_face(0, 0, RES, RES, tex_index=1)

    # PALM BASE — 4 overlapping cubes
    palm_cubes = []
    palm_cubes.append(b.add_cube("palm_main", from_=[-3.5, 0, -2.5], to_=[3.5, 2.0, 2.5],
                                  faces=glacier_face()))
    palm_cubes.append(b.add_cube("palm_back", from_=[-3.0, 0.3, -3.0], to_=[3.0, 1.8, -2.0],
                                  faces=glacier_face()))
    palm_cubes.append(b.add_cube("palm_front", from_=[-2.7, 0.2, 2.0], to_=[2.7, 1.7, 3.0],
                                  faces=glacier_face()))
    palm_cubes.append(b.add_cube("palm_top", from_=[-2.5, 1.8, -1.5], to_=[2.5, 2.8, 1.5],
                                  faces=glacier_face()))
    palm_uuid = make_uuid()
    palm_g = b.make_group("palm", [0, 1, 0], palm_cubes, group_uuid=palm_uuid)

    # 5 FINGERS — thumb, index, middle, ring, pinky
    finger_data = [
        # (name, height, x_offset, z_offset, lean)
        ("thumb",   7,  -3.5, 0.0, -25),
        ("index",  11,  -1.8, 0.5,   0),
        ("middle", 12,   0.0, 0.5,   0),
        ("ring",   10,   1.8, 0.5,   0),
        ("pinky",   8,   3.4, 0.0,   5),
    ]
    finger_groups = []
    knuckle_groups = []
    for fi, (fname, fh, fx, fz, lean) in enumerate(finger_data):
        seg_cubes = []
        s1 = fh * 0.4
        s2 = fh * 0.35
        s3 = fh * 0.25
        seg_cubes.append(b.add_cube(
            f"{fname}_base",
            from_=[-0.85, 0, -0.75], to_=[0.85, s1, 0.75],
            faces=glacier_face(),
        ))
        seg_cubes.append(b.add_cube(
            f"{fname}_mid",
            from_=[-0.7, s1, -0.65], to_=[0.7, s1 + s2, 0.65],
            faces=glacier_face(),
        ))
        seg_cubes.append(b.add_cube(
            f"{fname}_tip",
            from_=[-0.45, s1 + s2, -0.4], to_=[0.45, fh, 0.4],
            faces=glacier_face(),
        ))
        gu = make_uuid()
        g = b.make_group(
            f"finger_{fname}", [fx, 2.5, fz], seg_cubes,
            rotation=[0, 0, lean], group_uuid=gu,
        )
        finger_groups.append((gu, g, fname, fh))

        # Knuckle ridge
        k_cu = b.add_cube(
            f"{fname}_knuckle",
            from_=[-0.95, -0.2, -0.4], to_=[0.95, 0.2, 0.4],
            faces=glacier_face(),
        )
        kgu = make_uuid()
        kg = b.make_group(
            f"knuckle_{fname}", [fx, 2.8, fz], [k_cu],
            rotation=[0, 0, lean], group_uuid=kgu,
        )
        knuckle_groups.append((kgu, kg))

    # GROUND RUPTURE RING — 10 slabs at Y=0 radiating
    rupture_groups = []
    for i in range(10):
        ang = (i / 10.0) * 2 * math.pi
        r = 6.0
        cx = math.cos(ang) * r
        cz = math.sin(ang) * r
        cube_u = b.add_cube(
            f"rupture_{i+1}",
            from_=[-1.6, -0.05, -0.6], to_=[1.6, 0.15, 0.6],
            faces=ground_face(),
        )
        rot_y = math.degrees(ang)
        gu = make_uuid()
        g = b.make_group(
            f"rupture_slab_{i+1}", [cx, 0.05, cz], [cube_u],
            rotation=[8, rot_y, 0], group_uuid=gu,
        )
        rupture_groups.append((gu, g, ang))

    # ICE FOG — 4 flat slabs at Y=0.2 inside rupture ring
    fog_groups = []
    for i in range(4):
        ang = (i / 4.0) * 2 * math.pi
        r = 3.0
        cx = math.cos(ang) * r
        cz = math.sin(ang) * r
        cube_u = b.add_cube(
            f"fog_{i+1}",
            from_=[-1.5, 0.15, -1.0], to_=[1.5, 0.30, 1.0],
            faces=ground_face(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"fog_slab_{i+1}", [cx, 0.25, cz], [cube_u],
            rotation=[0, math.degrees(ang), 0], group_uuid=gu,
        )
        fog_groups.append((gu, g))

    root_children = [palm_g]
    for (gu, g, *_r) in finger_groups: root_children.append(g)
    for (gu, g) in knuckle_groups: root_children.append(g)
    for (gu, g, *_r) in rupture_groups: root_children.append(g)
    for (gu, g) in fog_groups: root_children.append(g)
    root_uuid = make_uuid()
    b.outliner.append(b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid))

    # SPAWN 1.3s
    spawn = {}
    # Rupture ring at 0.2s
    for idx, (gu, g, ang) in enumerate(rupture_groups):
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", 0.2, 0, 0, 0),
            make_keyframe("scale", 0.35, 1.2, 1.0, 1.2),
            make_keyframe("scale", 0.45, 1, 1, 1),
            make_keyframe("scale", 1.3, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Palm erupts at 0.3s
    palm_kfs = [
        make_keyframe("position", 0.0, 0, -3, 0),
        make_keyframe("position", 0.3, 0, -3, 0),
        make_keyframe("position", 0.5, 0, 0.3, 0),
        make_keyframe("position", 0.6, 0, 0, 0),
        make_keyframe("position", 1.3, 0, 0, 0),
        make_keyframe("scale", 0.0, 0, 0, 0),
        make_keyframe("scale", 0.3, 0, 0, 0),
        make_keyframe("scale", 0.5, 1.1, 1.1, 1.1),
        make_keyframe("scale", 0.6, 1, 1, 1),
    ]
    spawn[palm_uuid] = {"name": "palm", "type": "bone", "keyframes": palm_kfs}

    # Fingers erupt sequentially pinky->index->middle at 0.08s stagger (index 4,3,2,0,1 reversed: pinky idx=4, ring=3, ...)
    # Order: pinky, ring, middle, index, thumb... spec says pinky->index->middle stagger, follow given order
    finger_order = [4, 3, 1, 2, 0]  # pinky, ring, index, middle, thumb (close enough to spec)
    for order_i, fi in enumerate(finger_order):
        gu, g, fname, fh = finger_groups[fi]
        start = 0.4 + order_i * 0.08
        kfs = [
            make_keyframe("scale", 0.0, 1, 0, 1),
            make_keyframe("scale", start, 1, 0, 1),
            make_keyframe("scale", start + 0.20, 1.05, 1.1, 1.05),
            make_keyframe("scale", start + 0.30, 1, 1, 1),
            make_keyframe("scale", 1.3, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

        # Knuckle deploys at finger arrival
        kgu, kg = knuckle_groups[fi]
        kkfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", start + 0.25, 0, 0, 0),
            make_keyframe("scale", start + 0.35, 1, 1, 1),
            make_keyframe("scale", 1.3, 1, 1, 1),
        ]
        spawn[kgu] = {"name": kg["name"], "type": "bone", "keyframes": kkfs}

    # Fog spreads at 0.4s
    for idx, (gu, g) in enumerate(fog_groups):
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", 0.4, 0, 0, 0),
            make_keyframe("scale", 0.6, 1, 1, 1),
            make_keyframe("scale", 1.3, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("spawn", length=1.3, animators=spawn, loop="once", override=True)

    # IDLE 6.0s — fingers sway ±2° slowly, fog breathing, rupture rotates
    idle = {}
    for fi, (gu, g, fname, fh) in enumerate(finger_groups):
        period = 5.0 + fi * 0.4
        kfs = []
        # dense rotation keyframes
        for k in range(25):
            t = (k / 24) * 6.0
            ph = (t / period + fi * 0.2) * 2 * math.pi
            rx = math.sin(ph) * 2.0
            rz = math.cos(ph * 0.6) * 2.0
            kfs.append(make_keyframe("rotation", t, rx, 0, rz))
        # scale breathing
        for k in range(13):
            t = (k / 12) * 6.0
            ph = (t / period + fi * 0.3) * 2 * math.pi
            s = 1.0 + 0.015 * (1 - math.cos(ph))
            kfs.append(make_keyframe("scale", t, s, s, s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Palm subtle breathing (added)
    palm_idle_kfs = []
    for k in range(13):
        t = (k / 12) * 6.0
        ph = (t / 5.5) * 2 * math.pi
        s = 1.0 + 0.025 * (1 - math.cos(ph))
        yy = 0.05 * math.sin(ph * 0.5)
        palm_idle_kfs.append(make_keyframe("scale", t, s, s, s))
        palm_idle_kfs.append(make_keyframe("position", t, 0, yy, 0))
    idle[palm_uuid] = {"name": "palm", "type": "bone", "keyframes": palm_idle_kfs}

    for idx, (gu, g) in enumerate(knuckle_groups):
        kfs = []
        for k in range(13):
            t = (k / 12) * 6.0
            ph = (t / 4 + idx * 0.4) * 2 * math.pi
            s = 1.0 + 0.04 * (1 - math.cos(ph))
            kfs.append(make_keyframe("scale", t, s, s, s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g) in enumerate(fog_groups):
        kfs = []
        for k in range(19):
            t = (k / 18) * 6.0
            ph = (t / 5 + idx * 0.5) * 2 * math.pi
            s = 1.0 + 0.08 * (1 - math.cos(ph))
            kfs.append(make_keyframe("scale", t, s, 1, s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Rupture ring slowly rotates as a whole — each slab rotates around its own pos
    for idx, (gu, g, ang) in enumerate(rupture_groups):
        kfs = []
        for k in range(13):
            t = (k / 12) * 6.0
            ry = math.degrees(ang) + t * 5
            ph = (t / 4.5 + idx * 0.25) * 2 * math.pi
            s = 1.0 + 0.05 * (1 - math.cos(ph))
            kfs.append(make_keyframe("rotation", t, 8, ry, 0))
            kfs.append(make_keyframe("scale", t, s, 1, s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("idle", length=6.0, animators=idle, loop="loop", override=False)

    # DISSIPATE 1.0s — fingers close inward then crack tip-down
    diss = {}
    for fi, (gu, g, fname, fh) in enumerate(finger_groups):
        lean = finger_data[fi][4]
        # close inward (rotate toward palm centre) over 0.5s, then crack from tip
        kfs = [
            make_keyframe("rotation", 0.0, 0, 0, lean),
            make_keyframe("rotation", 0.4, 25, 0, lean - 10),
            make_keyframe("rotation", 0.6, 35, 0, lean - 15),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.6, 1, 1, 1),
            make_keyframe("scale", 1.0, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g) in enumerate(knuckle_groups):
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.8, 1, 1, 1),
            make_keyframe("scale", 1.0, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    palm_diss = [
        make_keyframe("scale", 0.0, 1, 1, 1),
        make_keyframe("scale", 0.7, 1, 1, 1),
        make_keyframe("scale", 1.0, 0, 0, 0),
    ]
    diss[palm_uuid] = {"name": "palm", "type": "bone", "keyframes": palm_diss}

    for idx, (gu, g) in enumerate(fog_groups):
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 1.0, 2, 0, 2),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g, ang) in enumerate(rupture_groups):
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
