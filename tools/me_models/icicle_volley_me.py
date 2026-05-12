#!/usr/bin/env python3
"""Generator: icicle_volley_me.bbmodel — FreezingIce attack 15 'Thousand Needles'.

A precise 3x3 grid of 9 icicles. Mathematical formation, clear ice texture.
"""

import sys
import math
import random

sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder, make_uuid, make_keyframe, uniform_face, png_from_pixels,
)
from _freezingice_common import (
    new_pixels, radial_zone, horiz_band_fade, per_pixel_jitter,
    specular_blob, random_bright_specks, hex_color, fill_rect,
    osc_scale_kfs, osc_rot_kfs,
)

OUTPUT = "D:/CC/ChaosCraft/src/main/resources/models/freezingice/me_attacks/icicle_volley_me.bbmodel"
W = H = 128


def tex_icicle():
    """Clear icicle — light, mostly bright, deep at base, white spec at tip."""
    near_white = hex_color("#e8f8ff")
    pale       = hex_color("#a0d0f0")
    edge       = hex_color("#4090c0")
    deep_navy  = hex_color("#081830")
    spec       = hex_color("#ffffff")
    pixels = new_pixels(W, H, near_white)
    # Vertical gradient: top white, bottom deep navy
    for y in range(H):
        t = y / float(H)
        if t < 0.15:
            col = spec
        elif t < 0.4:
            tt = (t - 0.15) / 0.25
            col = (spec[0] + int((near_white[0] - spec[0]) * tt),
                   spec[1] + int((near_white[1] - spec[1]) * tt),
                   spec[2] + int((near_white[2] - spec[2]) * tt), 255)
        elif t < 0.7:
            tt = (t - 0.4) / 0.3
            col = (near_white[0] + int((pale[0] - near_white[0]) * tt),
                   near_white[1] + int((pale[1] - near_white[1]) * tt),
                   near_white[2] + int((pale[2] - near_white[2]) * tt), 255)
        elif t < 0.9:
            tt = (t - 0.7) / 0.2
            col = (pale[0] + int((edge[0] - pale[0]) * tt),
                   pale[1] + int((edge[1] - pale[1]) * tt),
                   pale[2] + int((edge[2] - pale[2]) * tt), 255)
        else:
            tt = (t - 0.9) / 0.1
            col = (edge[0] + int((deep_navy[0] - edge[0]) * tt),
                   edge[1] + int((deep_navy[1] - edge[1]) * tt),
                   edge[2] + int((deep_navy[2] - edge[2]) * tt), 255)
        for x in range(W):
            pixels[y * W + x] = col
    # Vertical concentrated specular at top
    for y in range(0, 8):
        for x in range(W):
            pixels[y * W + x] = spec
    # Side-lit highlight column
    for x in range(W // 4, W // 4 + 5):
        for y in range(H):
            p = pixels[y * W + x]
            pixels[y * W + x] = (
                min(255, p[0] + 30), min(255, p[1] + 30), min(255, p[2] + 35), 255,
            )
    # Random bubble inclusions
    random_bright_specks(pixels, W, H, 100, spec, seed=11, size_max=2)
    random_bright_specks(pixels, W, H, 60, pale, seed=22, size_max=1)
    per_pixel_jitter(pixels, W, H, magnitude=14, seed=33, density=0.7)
    return png_from_pixels(pixels, W, H)


def tex_ring():
    """Silver-blue ring on near-black."""
    silver = hex_color("#6090b0")
    black  = hex_color("#060e18")
    bright = hex_color("#a0c0e0")
    spec   = hex_color("#ffffff")
    pixels = new_pixels(W, H, black)
    # Concentric ring at 60% radius
    cx, cy = W / 2, H / 2
    for y in range(H):
        for x in range(W):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            r = W * 0.32
            if abs(d - r) < 4:
                pixels[y * W + x] = silver
            elif abs(d - r) < 8:
                t = (abs(d - r) - 4) / 4.0
                pixels[y * W + x] = (
                    silver[0] + int((black[0] - silver[0]) * t),
                    silver[1] + int((black[1] - silver[1]) * t),
                    silver[2] + int((black[2] - silver[2]) * t), 255,
                )
    # Outer thin ring
    for y in range(H):
        for x in range(W):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            r = W * 0.45
            if abs(d - r) < 2:
                pixels[y * W + x] = bright
    random_bright_specks(pixels, W, H, 120, bright, seed=44, size_max=1)
    random_bright_specks(pixels, W, H, 40, spec, seed=55, size_max=1)
    per_pixel_jitter(pixels, W, H, magnitude=14, seed=66, density=0.8)
    return png_from_pixels(pixels, W, H)


def build():
    b = Builder("icicle_volley_me", resolution=(W, H), visible_box=(10, 12, 0))
    b.add_texture("icicle_clear", tex_icicle())
    b.add_texture("formation_ring", tex_ring())

    F0 = lambda: uniform_face(0, 0, W, H, tex_index=0)
    F1 = lambda: uniform_face(0, 0, W, H, tex_index=1)

    # ---- 9 icicles in 3x3 grid, each has 3 tapered segments ----
    icicle_groups = []  # list of (group_uuid, group_dict, tip_uuid, tip_group)
    spacing = 1.8
    # outer row slightly shorter, middle row full
    height_map = {
        (0, 0): 4.5, (0, 1): 5.0, (0, 2): 4.5,
        (1, 0): 5.0, (1, 1): 5.8, (1, 2): 5.0,
        (2, 0): 4.5, (2, 1): 5.0, (2, 2): 4.5,
    }
    for row in range(3):
        for col in range(3):
            idx = row * 3 + col
            total_h = height_map[(row, col)]
            cx = (col - 1) * spacing
            cy = (row - 1) * spacing
            # 3 tapered segments stacked vertically (X=cx, Z=cy, Y goes up)
            seg_heights = [total_h * 0.45, total_h * 0.35, total_h * 0.2]
            seg_widths = [0.6, 0.4, 0.18]
            y_cursor = 0
            seg_cubes = []
            for s, (sh, sw) in enumerate(zip(seg_heights, seg_widths)):
                cube = b.add_cube(
                    f"ic{idx+1}_seg{s+1}",
                    from_=[-sw, y_cursor, -sw], to_=[sw, y_cursor + sh, sw],
                    faces=F0(),
                )
                seg_cubes.append(cube)
                y_cursor += sh
            # Tip glow cluster: small bright emissive at very tip
            tip_cube = b.add_cube(
                f"ic{idx+1}_tipglow",
                from_=[-0.15, y_cursor, -0.15], to_=[0.15, y_cursor + 0.18, 0.15],
                faces=F0(),
            )
            tip_gu = make_uuid()
            tip_g = b.make_group(f"ic{idx+1}_tip_grp",
                                 [0, y_cursor, 0], [tip_cube],
                                 group_uuid=tip_gu)
            ic_gu = make_uuid()
            ic_g = b.make_group(
                f"icicle_{row+1}_{col+1}", [cx, 0, cy],
                seg_cubes + [tip_g],
                group_uuid=ic_gu,
            )
            icicle_groups.append((ic_gu, ic_g, tip_gu, tip_g))

    # ---- Formation ring: 8 slabs at back of formation ----
    ring_segs = []
    ring_y = -0.3
    for i in range(8):
        ang = (i / 8.0) * 2 * math.pi
        cx = math.cos(ang) * 3.0
        cz = math.sin(ang) * 3.0
        cube = b.add_cube(
            f"ring_slab_{i+1}",
            from_=[-0.6, -0.08, -0.3], to_=[0.6, 0.08, 0.3],
            faces=F1(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"ring_slab_grp_{i+1}", [cx, ring_y, cz], [cube],
            rotation=[0, math.degrees(ang) + 90, 0], group_uuid=gu,
        )
        ring_segs.append((gu, g))

    # ---- Ice mist: 4 flat wide slabs around formation ----
    mist_segs = []
    for i in range(4):
        ang = (i / 4.0) * 2 * math.pi
        cx = math.cos(ang) * 3.5
        cy = 1.5 + (i % 2) * 1.0
        cz = math.sin(ang) * 3.5
        cube = b.add_cube(
            f"mist_{i+1}",
            from_=[-2.0, -0.1, -2.0], to_=[2.0, 0.1, 2.0],
            faces=F1(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"mist_grp_{i+1}", [cx, cy, cz], [cube],
            rotation=[math.degrees(ang) * 0.4, math.degrees(ang), 0], group_uuid=gu,
        )
        mist_segs.append((gu, g))

    # ---- ROOT ----
    children = [g for (_u, g, _t, _tg) in icicle_groups]
    children += [g for (_u, g) in ring_segs + mist_segs]
    root_uuid = make_uuid()
    root = b.make_group("root", [0, 0, 0], children, group_uuid=root_uuid)
    b.outliner.append(root)

    # ---- SPAWN: 0.4s ----
    spawn = {}
    # Formation ring at 0.0s
    for i, (gu, g) in enumerate(ring_segs):
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.00, x=0.05, y=0.05, z=0.05),
            make_keyframe("scale", 0.08, x=1.0, y=1.0, z=1.0),
            make_keyframe("scale", 0.40, x=1, y=1, z=1),
        ]}
    # All 9 icicles materialise tip-to-base in 0.04s stagger
    for i, (ic_gu, ic_g, tip_gu, tip_g) in enumerate(icicle_groups):
        t0 = 0.08 + i * 0.04
        spawn[ic_gu] = {"name": ic_g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.00, x=0.05, y=0.05, z=0.05),
            make_keyframe("scale", t0, x=0.05, y=0.05, z=0.05),
            make_keyframe("scale", t0 + 0.08, x=1.0, y=1.0, z=1.0),
            make_keyframe("scale", 0.40, x=1, y=1, z=1),
        ]}
        # Tip glow snaps on at end of formation
        spawn[tip_gu] = {"name": tip_g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.00, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.08, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.10, x=1.4, y=1.4, z=1.4),
            make_keyframe("scale", t0 + 0.14, x=1, y=1, z=1),
            make_keyframe("scale", 0.40, x=1, y=1, z=1),
        ]}
    # Mist expands
    for i, (gu, g) in enumerate(mist_segs):
        t0 = 0.15 + i * 0.04
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.00, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.12, x=1.1, y=1, z=1.1),
            make_keyframe("scale", 0.40, x=1, y=1, z=1),
        ]}
    b.add_animation("spawn", length=0.40, animators=spawn, loop="once", override=True)

    # ---- IDLE: 2.0s ----
    idle = {}
    # Root very slow drill spin on Z (long axis = the firing direction; here Y because icicles vertical)
    idle[root_uuid] = {"name": "root", "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, x=0, y=0, z=0),
        make_keyframe("rotation", 0.5, x=0, y=45, z=0),
        make_keyframe("rotation", 1.0, x=0, y=90, z=0),
        make_keyframe("rotation", 1.5, x=0, y=135, z=0),
        make_keyframe("rotation", 2.0, x=0, y=180, z=0),
    ]}
    # Each icicle independent vibration ±0.5° at tip
    for i, (ic_gu, ic_g, tip_gu, tip_g) in enumerate(icicle_groups):
        kfs = osc_rot_kfs(2.0, 16, amp_x=0.5, amp_y=0, amp_z=0.5,
                          period=0.8 + i * 0.07, phase=i * 0.18)
        idle[ic_gu] = {"name": ic_g["name"], "type": "bone", "keyframes": kfs}
        # Tip glow pulses
        kfs2 = osc_scale_kfs(2.0, 18, base=1.0, amp=0.15,
                             period=1.4 + (i % 3) * 0.2, phase=i * 0.25)
        idle[tip_gu] = {"name": tip_g["name"], "type": "bone", "keyframes": kfs2}
    # Formation ring counter-rotates as a unit — apply opposite rotation on each ring slab so they orbit
    for i, (gu, g) in enumerate(ring_segs):
        ang0 = (i / 8.0) * 2 * math.pi
        period = 2.0
        kfs = []
        steps = 16
        for k in range(steps + 1):
            t = (k / steps) * 2.0
            # Counter-rotate: opposite direction
            ang = ang0 - (t / period) * 2 * math.pi
            cx = math.cos(ang) * 3.0
            cz = math.sin(ang) * 3.0
            dx = cx - math.cos(ang0) * 3.0
            dz = cz - math.sin(ang0) * 3.0
            kfs.append(make_keyframe("position", t, x=dx, y=0, z=dz))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}
    # Mist ambient drift
    for i, (gu, g) in enumerate(mist_segs):
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_rot_kfs(2.0, 14, amp_x=2, amp_y=5, amp_z=2,
                                             period=1.6, phase=i * 0.4)}
    b.add_animation("idle", length=2.0, animators=idle, loop="loop", override=False)

    # ---- DISSIPATE: 0.35s — fan out forward + scale to 0 ----
    diss = {}
    for i, (ic_gu, ic_g, tip_gu, tip_g) in enumerate(icicle_groups):
        row = i // 3
        col = i % 3
        fan_x = (col - 1) * 1.5
        fan_z = (row - 1) * 1.5
        diss[ic_gu] = {"name": ic_g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.35, x=fan_x, y=4.0, z=fan_z + 3.0),  # fly forward
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.35, x=0.0, y=0.0, z=0.0),
        ]}
        diss[tip_gu] = {"name": tip_g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.35, x=0.0, y=0.0, z=0.0),
        ]}
    for i, (gu, g) in enumerate(ring_segs):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.25, x=2.0, y=1, z=2.0),
            make_keyframe("scale", 0.35, x=0.0, y=0.0, z=0.0),
        ]}
    for i, (gu, g) in enumerate(mist_segs):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.35, x=0, y=0, z=-3),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.35, x=0.0, y=0.0, z=0.0),
        ]}
    b.add_animation("dissipate", length=0.35, animators=diss, loop="once", override=True)

    b.write(OUTPUT)
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    build()
