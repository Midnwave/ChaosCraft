#!/usr/bin/env python3
"""Generator: avalanche_shot.bbmodel — FreezingIce attack 12 'Mountain's Wrath'.

A compressed ball of avalanche debris — irregular dense sphere of ice/snow/rock.
Surface shards, snow powder trail, compression ring.
"""

import sys
import math
import random

sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder, make_uuid, make_keyframe, uniform_face, png_from_pixels,
)
from _freezingice_common import (
    new_pixels, radial_zone, per_pixel_jitter, specular_blob,
    random_bright_specks, hex_color, fill_rect,
    osc_scale_kfs, osc_rot_kfs,
)

OUTPUT = "D:/CC/ChaosCraft/src/main/resources/models/freezingice/me_attacks/avalanche_shot.bbmodel"
W = H = 128


def tex_debris():
    """Chaotic mixed ice + rock + snow patches."""
    rock      = hex_color("#303840")
    ice       = hex_color("#a8c8e0")
    shadow    = hex_color("#102848")
    emiss     = hex_color("#80c8f0")
    dark      = hex_color("#0a1018")
    pixels = new_pixels(W, H, shadow)
    # Chaotic blobby patches
    rng = random.Random(1234)
    for _ in range(220):
        cx = rng.randrange(0, W)
        cy = rng.randrange(0, H)
        r  = rng.randrange(3, 14)
        choice = rng.random()
        if choice < 0.4:
            col = rock
        elif choice < 0.7:
            col = ice
        elif choice < 0.85:
            col = shadow
        elif choice < 0.95:
            col = emiss
        else:
            col = dark
        for dy in range(-r, r + 1):
            for dx in range(-r, r + 1):
                if dx * dx + dy * dy <= r * r:
                    x, y = cx + dx, cy + dy
                    if 0 <= x < W and 0 <= y < H:
                        pixels[y * W + x] = col
    # Strong specks of bright/dark
    random_bright_specks(pixels, W, H, 180, ice, seed=55, size_max=2)
    random_bright_specks(pixels, W, H, 90, emiss, seed=66, size_max=1)
    random_bright_specks(pixels, W, H, 140, dark, seed=77, size_max=2)
    per_pixel_jitter(pixels, W, H, magnitude=28, seed=88)
    return png_from_pixels(pixels, W, H)


def tex_powder():
    """Soft white snow powder + compression ring."""
    base    = hex_color("#f0f8ff")
    fade    = hex_color("#c0d8f0")
    spec    = hex_color("#ffffff")
    pixels  = new_pixels(W, H, fade)
    radial_zone(pixels, W, H, W / 2, H / 2, base, fade, W * 0.6)
    specular_blob(pixels, W, H, 40, 40, spec, radius=10, strength=0.6)
    specular_blob(pixels, W, H, 90, 88, spec, radius=8, strength=0.4)
    random_bright_specks(pixels, W, H, 200, spec, seed=12, size_max=2)
    per_pixel_jitter(pixels, W, H, magnitude=18, seed=21, density=0.9)
    return png_from_pixels(pixels, W, H)


def build():
    b = Builder("avalanche_shot", resolution=(W, H), visible_box=(10, 10, 0))
    b.add_texture("avalanche_debris", tex_debris())
    b.add_texture("avalanche_powder", tex_powder())

    F0 = lambda: uniform_face(0, 0, W, H, tex_index=0)
    F1 = lambda: uniform_face(0, 0, W, H, tex_index=1)

    # ---- HERO: 7 irregular overlapping cubes ----
    sphere_specs = [
        ((-1.0, -0.6, -0.5), (1.0, 0.4, 0.5),  (4, 12,  3)),
        ((-1.4, -1.0, -0.8), (0.6, 0.4, 0.8), (-5,  8, -7)),
        ((-0.4, -0.5, -1.4), (1.4, 1.5, 0.6),  (7, -6, 11)),
        ((-1.6, 0.0, -0.4), (-0.4, 1.6, 1.2),  (12, 18, -4)),
        ((0.2, -0.6, -0.4), (1.6, 0.6, 1.4),  (-9, 22,  5)),
        ((-0.7, 0.2, -1.1), (0.7, 1.4, 0.1),  (3, -12, 14)),
        ((-0.4, -1.2, -0.4), (0.4, -0.4, 0.4),(0, 5, -3)),
    ]
    sphere_segs = []
    for i, (fr, to, rot) in enumerate(sphere_specs):
        cube = b.add_cube(f"core_{i+1}", from_=list(fr), to_=list(to), faces=F0())
        gu = make_uuid()
        g = b.make_group(f"core_grp_{i+1}", [0, 0, 0], [cube],
                         rotation=[rot[0], rot[1], rot[2]], group_uuid=gu)
        sphere_segs.append((gu, g))

    # ---- 8 surface shards around equator ----
    shard_segs = []
    for i in range(8):
        ang = (i / 8.0) * 2 * math.pi + 0.1
        cx = math.cos(ang) * 1.7
        cz = math.sin(ang) * 1.7
        cube = b.add_cube(
            f"shard_{i+1}",
            from_=[-0.5, -0.15, -0.3], to_=[0.5, 0.15, 0.3],
            faces=F0(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"shard_grp_{i+1}", [cx, 0, cz], [cube],
            rotation=[math.degrees(ang) * 0.3, math.degrees(ang), i * 11],
            group_uuid=gu,
        )
        shard_segs.append((gu, g))

    # ---- 10 powder trail flat cubes ----
    powder_segs = []
    for i in range(10):
        # decreasing size, getting further behind
        size = max(0.3, 1.4 - i * 0.10)
        zc = -2.0 - i * 1.2
        cube = b.add_cube(
            f"powder_{i+1}",
            from_=[-size, -size * 0.5, -size * 0.5], to_=[size, size * 0.5, size * 0.5],
            faces=F1(),
        )
        gu = make_uuid()
        g = b.make_group(f"powder_grp_{i+1}", [0, 0, zc], [cube], group_uuid=gu)
        powder_segs.append((gu, g))

    # ---- 6 compression ring slabs ----
    ring_segs = []
    for i in range(6):
        ang = (i / 6.0) * 2 * math.pi
        cx = math.cos(ang) * 2.4
        cz = math.sin(ang) * 2.4
        cube = b.add_cube(
            f"ring_{i+1}",
            from_=[-0.7, -0.08, -0.3], to_=[0.7, 0.08, 0.3],
            faces=F1(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"ring_grp_{i+1}", [cx, 0, cz], [cube],
            rotation=[0, math.degrees(ang) + 90, 0], group_uuid=gu,
        )
        ring_segs.append((gu, g))

    # ---- ROOT ----
    children = [g for (_u, g) in sphere_segs + shard_segs + powder_segs + ring_segs]
    root_uuid = make_uuid()
    root = b.make_group("root", [0, 0, 0], children, group_uuid=root_uuid)
    b.outliner.append(root)

    # ---- SPAWN: 0.3s ----
    spawn = {}
    # All sphere cubes phase in simultaneously already tumbling
    for i, (gu, g) in enumerate(sphere_segs):
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", 0.12, x=1.2, y=1.2, z=1.2),
            make_keyframe("scale", 0.20, x=1.0, y=1.0, z=1.0),
            make_keyframe("scale", 0.30, x=1.0, y=1.0, z=1.0),
            make_keyframe("rotation", 0.0, x=0, y=0, z=0),
            make_keyframe("rotation", 0.30, x=360 + i * 12, y=180 + i * 15, z=90 + i * 10),
        ]}
    for i, (gu, g) in enumerate(shard_segs):
        t0 = i * 0.02
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.06, x=1, y=1, z=1),
            make_keyframe("scale", 0.30, x=1, y=1, z=1),
        ]}
    for i, (gu, g) in enumerate(powder_segs):
        t0 = 0.05 + i * 0.015
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.08, x=1, y=1, z=1),
            make_keyframe("scale", 0.30, x=1, y=1, z=1),
        ]}
    for i, (gu, g) in enumerate(ring_segs):
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", 0.08, x=0, y=0, z=0),
            make_keyframe("scale", 0.20, x=1.2, y=1.2, z=1.2),
            make_keyframe("scale", 0.30, x=1, y=1, z=1),
        ]}
    b.add_animation("spawn", length=0.3, animators=spawn, loop="once", override=True)

    # ---- IDLE: 2.5s ----
    idle = {}
    # Root tumble all 3 axes
    idle[root_uuid] = {"name": "root", "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, x=0, y=0, z=0),
        make_keyframe("rotation", 0.625, x=90, y=120, z=60),
        make_keyframe("rotation", 1.25, x=180, y=240, z=120),
        make_keyframe("rotation", 1.875, x=270, y=360, z=180),
        make_keyframe("rotation", 2.5, x=360, y=480, z=240),
    ]}
    # Sphere cubes individual subtle pulse
    for i, (gu, g) in enumerate(sphere_segs):
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_scale_kfs(2.5, 18, base=1.0, amp=0.04,
                                               period=1.5 + i * 0.1, phase=i * 0.2)}
    # Shards orbit at various radii
    for i, (gu, g) in enumerate(shard_segs):
        ang0 = (i / 8.0) * 2 * math.pi + 0.1
        period = 2.0 + i * 0.15
        kfs = []
        steps = 24
        for k in range(steps + 1):
            t = (k / steps) * 2.5
            ang = ang0 + (t / period) * 2 * math.pi
            cx = math.cos(ang) * 1.7
            cz = math.sin(ang) * 1.7
            dx = cx - math.cos(ang0) * 1.7
            dz = cz - math.sin(ang0) * 1.7
            kfs.append(make_keyframe("position", t, x=dx, y=0.2 * math.sin(t * 4 + i), z=dz))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}
    # Powder trail streams behind
    for i, (gu, g) in enumerate(powder_segs):
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_rot_kfs(2.5, 16, amp_x=8, amp_y=12, amp_z=8,
                                             period=1.8, phase=i * 0.3)}
    # Compression ring spins
    for i, (gu, g) in enumerate(ring_segs):
        ang0 = (i / 6.0) * 2 * math.pi
        period = 2.5
        kfs = []
        steps = 20
        for k in range(steps + 1):
            t = (k / steps) * 2.5
            ang = ang0 + (t / period) * 2 * math.pi
            cx = math.cos(ang) * 2.4
            cz = math.sin(ang) * 2.4
            dx = cx - math.cos(ang0) * 2.4
            dz = cz - math.sin(ang0) * 2.4
            kfs.append(make_keyframe("position", t, x=dx, y=0, z=dz))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}
    b.add_animation("idle", length=2.5, animators=idle, loop="loop", override=False)

    # ---- DISSIPATE: 0.35s — explode ----
    diss = {}
    for i, (gu, g) in enumerate(sphere_segs):
        ang = (i / 7.0) * 2 * math.pi + 0.3
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.35, x=math.cos(ang) * 7, y=math.sin(ang * 0.7) * 3,
                          z=math.sin(ang) * 7),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.35, x=0.0, y=0.0, z=0.0),
            make_keyframe("rotation", 0.0, x=0, y=0, z=0),
            make_keyframe("rotation", 0.35, x=720 + i * 30, y=540, z=360),
        ]}
    for i, (gu, g) in enumerate(shard_segs):
        ang = (i / 8.0) * 2 * math.pi + 0.1
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.35, x=math.cos(ang) * 5, y=math.sin(ang) * 2, z=math.sin(ang) * 5),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.35, x=0.0, y=0.0, z=0.0),
        ]}
    for i, (gu, g) in enumerate(powder_segs):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.35, x=0, y=0, z=-4 - i * 0.3),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.35, x=0.1, y=0.1, z=0.1),
        ]}
    for i, (gu, g) in enumerate(ring_segs):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.2, x=2.5, y=2.5, z=2.5),
            make_keyframe("scale", 0.35, x=0.0, y=0.0, z=0.0),
        ]}
    b.add_animation("dissipate", length=0.35, animators=diss, loop="once", override=True)

    b.write(OUTPUT)
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    build()
