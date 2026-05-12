#!/usr/bin/env python3
"""Generator: cryo_lance.bbmodel — FreezingIce attack 11 'Absolute'.

A spear of absolute-zero compressed ice. Dark-at-core, bright-at-edge texture.
Multi-segment shaft, fins, compression aura, satellites, and trailing wake.
"""

import sys
import math

sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder, make_uuid, make_keyframe, uniform_face, png_from_pixels,
)
from _freezingice_common import (
    new_pixels, radial_zone, horiz_band_fade, per_pixel_jitter,
    specular_blob, random_bright_specks, hex_color,
    osc_scale_kfs, osc_rot_kfs, orbit_pos_kfs,
)

OUTPUT = "D:/CC/ChaosCraft/src/main/resources/models/freezingice/me_attacks/cryo_lance.bbmodel"
W = H = 128


# ---------------------------------------------------------------------------
# TEXTURES
# ---------------------------------------------------------------------------

def tex_shaft():
    """Absolute zero — dark core, bright edge. Inverted from typical."""
    core    = hex_color("#020612")
    deep    = hex_color("#081830")
    mid     = hex_color("#204070")
    bright  = hex_color("#6090d0")
    edge    = hex_color("#c0e8ff")
    top     = hex_color("#e0f4ff")
    # Inverted radial: dark in centre, bright at edges
    pixels = new_pixels(W, H, core)
    # Radial from edge bright -> core dark
    for y in range(H):
        for x in range(W):
            dx = x - W / 2
            dy = y - H / 2
            d = math.sqrt(dx * dx + dy * dy)
            t = min(1.0, d / (W * 0.55))
            # invert: outside bright, inside dark
            if t < 0.25:
                col = core
            elif t < 0.5:
                col = (core[0] + int((deep[0] - core[0]) * (t - 0.25) / 0.25),
                       core[1] + int((deep[1] - core[1]) * (t - 0.25) / 0.25),
                       core[2] + int((deep[2] - core[2]) * (t - 0.25) / 0.25),
                       255)
            elif t < 0.75:
                col = (deep[0] + int((mid[0] - deep[0]) * (t - 0.5) / 0.25),
                       deep[1] + int((mid[1] - deep[1]) * (t - 0.5) / 0.25),
                       deep[2] + int((mid[2] - deep[2]) * (t - 0.5) / 0.25),
                       255)
            elif t < 0.95:
                col = (mid[0] + int((bright[0] - mid[0]) * (t - 0.75) / 0.20),
                       mid[1] + int((bright[1] - mid[1]) * (t - 0.75) / 0.20),
                       mid[2] + int((bright[2] - mid[2]) * (t - 0.75) / 0.20),
                       255)
            else:
                col = edge
            pixels[y * W + x] = col

    # Top edge pure blinding band
    for y in range(0, 4):
        for x in range(W):
            pixels[y * W + x] = top

    # Bright interface specks at edge zone
    random_bright_specks(pixels, W, H, 120, edge, seed=11, size_max=2)
    random_bright_specks(pixels, W, H, 80, top, seed=23, size_max=1)

    # Subtle noise overall
    per_pixel_jitter(pixels, W, H, magnitude=14, seed=77, density=0.7)

    return png_from_pixels(pixels, W, H)


def tex_aux():
    """Fins / wake / aura / satellites — bright cold white to deep navy."""
    bright = hex_color("#d0ecff")
    deep   = hex_color("#040e1e")
    mid    = hex_color("#6090c0")
    spec   = hex_color("#ffffff")
    pixels = new_pixels(W, H, deep)
    radial_zone(pixels, W, H, W / 2, H / 2, bright, deep, W * 0.55)
    # Bright streaks
    for y in range(H):
        for x in range(W):
            if (x + y) % 11 == 0:
                p = pixels[y * W + x]
                pixels[y * W + x] = (
                    min(255, p[0] + 30), min(255, p[1] + 40), min(255, p[2] + 50), 255,
                )
    specular_blob(pixels, W, H, 40, 40, spec, radius=6, strength=0.8)
    specular_blob(pixels, W, H, 90, 88, mid, radius=8, strength=0.5)
    random_bright_specks(pixels, W, H, 140, bright, seed=51, size_max=2)
    per_pixel_jitter(pixels, W, H, magnitude=18, seed=83, density=0.9)
    return png_from_pixels(pixels, W, H)


# ---------------------------------------------------------------------------
# BUILD
# ---------------------------------------------------------------------------

def build():
    b = Builder("cryo_lance", resolution=(W, H), visible_box=(12, 12, 0))
    b.add_texture("cryo_lance_shaft", tex_shaft())
    b.add_texture("cryo_lance_aux", tex_aux())

    F_SHAFT = lambda: uniform_face(0, 0, W, H, tex_index=0)
    F_AUX   = lambda: uniform_face(0, 0, W, H, tex_index=1)

    # -------- SHAFT: 7 segments tapering, each 3 overlap cubes, each its own bone --------
    shaft_widths = [3.5, 3.0, 2.5, 2.0, 1.5, 1.0, 0.5]
    shaft_segs = []  # (uuid, group)
    z = 0.0
    seg_len = 3.0
    for i, w in enumerate(shaft_widths):
        zc = z + seg_len / 2
        cubes = []
        cubes.append(b.add_cube(
            f"shaft{i+1}_core",
            from_=[-w / 2, -w / 2, z], to_=[w / 2, w / 2, z + seg_len],
            faces=F_SHAFT(),
        ))
        cubes.append(b.add_cube(
            f"shaft{i+1}_xshear",
            from_=[-w / 2 * 1.15, -w / 2 * 0.7, z + 0.2], to_=[w / 2 * 1.15, w / 2 * 0.7, z + seg_len - 0.2],
            faces=F_SHAFT(),
        ))
        cubes.append(b.add_cube(
            f"shaft{i+1}_yshear",
            from_=[-w / 2 * 0.7, -w / 2 * 1.15, z + 0.2], to_=[w / 2 * 0.7, w / 2 * 1.15, z + seg_len - 0.2],
            faces=F_SHAFT(),
        ))
        gu = make_uuid()
        g = b.make_group(f"shaft_seg{i+1}", [0, 0, zc], cubes, group_uuid=gu)
        shaft_segs.append((gu, g))
        z += seg_len

    # -------- 3 TIP bones beyond shaft, each its own bone, tapering to zero --------
    tip_segs = []
    tip_widths = [0.5, 0.3, 0.1]
    for i, w in enumerate(tip_widths):
        zc = z + 1.0 / 2
        cubes = [b.add_cube(
            f"tip{i+1}",
            from_=[-w / 2, -w / 2, z], to_=[w / 2, w / 2, z + 1.0],
            faces=F_SHAFT(),
        )]
        gu = make_uuid()
        g = b.make_group(f"tip_seg{i+1}", [0, 0, zc], cubes, group_uuid=gu)
        tip_segs.append((gu, g))
        z += 1.0

    # -------- 4 compression aura slabs alongside shaft --------
    aura_segs = []
    for i in range(4):
        ang = (i / 4.0) * 2 * math.pi
        cx = math.cos(ang) * 3.5
        cy = math.sin(ang) * 3.5
        cube = b.add_cube(
            f"aura_slab_{i+1}",
            from_=[-0.3, -0.05, 0.5], to_=[0.3, 0.05, 16.0],
            faces=F_AUX(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"aura_{i+1}", [cx, cy, 8.0], [cube],
            rotation=[math.degrees(ang) * 0.2, 0, math.degrees(ang)],
            group_uuid=gu,
        )
        aura_segs.append((gu, g))

    # -------- 4 frost fins at trailing end --------
    fin_segs = []
    for i in range(4):
        ang = (i / 4.0) * 2 * math.pi
        cube = b.add_cube(
            f"fin_{i+1}",
            from_=[-2.0, -0.1, -2.5], to_=[2.0, 0.1, 0.5],
            faces=F_AUX(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"fin_grp_{i+1}", [0, 0, 1.0], [cube],
            rotation=[0, 0, math.degrees(ang)],
            group_uuid=gu,
        )
        fin_segs.append((gu, g))

    # -------- 6 crystal satellite diamonds orbiting at mid-shaft --------
    sat_segs = []
    mid_z = (z * 0.45)  # near middle of shaft
    for i in range(6):
        ang = (i / 6.0) * 2 * math.pi
        cx = math.cos(ang) * 4.5
        cy = math.sin(ang) * 4.5
        cube = b.add_cube(
            f"sat_{i+1}",
            from_=[-0.4, -0.4, -0.2], to_=[0.4, 0.4, 0.2],
            faces=F_AUX(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"sat_grp_{i+1}", [cx, cy, mid_z], [cube],
            rotation=[0, 0, math.degrees(ang) + 45.0],
            group_uuid=gu,
        )
        sat_segs.append((gu, g))

    # -------- 8 trailing wake slabs --------
    wake_segs = []
    for i in range(8):
        zc = -(i + 1) * 1.4
        cube = b.add_cube(
            f"wake_{i+1}",
            from_=[-(1.0 + i * 0.1), -0.08, -0.5], to_=[(1.0 + i * 0.1), 0.08, 0.5],
            faces=F_AUX(),
        )
        gu = make_uuid()
        g = b.make_group(f"wake_grp_{i+1}", [0, 0, zc], [cube], group_uuid=gu)
        wake_segs.append((gu, g))

    # -------- ROOT --------
    children = []
    for (_u, g) in shaft_segs + tip_segs + aura_segs + fin_segs + sat_segs + wake_segs:
        children.append(g)
    root_uuid = make_uuid()
    root = b.make_group("root", [0, 0, 0], children, group_uuid=root_uuid)
    b.outliner.append(root)

    # =====================================================================
    # ANIMATIONS
    # =====================================================================

    # ---- SPAWN: 0.35s, tip first then shaft tip-to-base ----
    spawn = {}
    # tip materializes first
    for i, (gu, g) in enumerate(tip_segs):
        t0 = i * 0.02
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.00, x=0.0, y=0.0, z=0.0),
            make_keyframe("scale", t0, x=0.0, y=0.0, z=0.0),
            make_keyframe("scale", t0 + 0.06, x=1.2, y=1.2, z=1.2),
            make_keyframe("scale", t0 + 0.10, x=1.0, y=1.0, z=1.0),
            make_keyframe("scale", 0.35, x=1, y=1, z=1),
        ]}
    # shaft materializes from tip backward (last segment first)
    for i, (gu, g) in enumerate(reversed(shaft_segs)):
        t0 = 0.06 + i * 0.025
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.00, x=0.0, y=0.0, z=0.0),
            make_keyframe("scale", t0, x=0.0, y=0.0, z=0.0),
            make_keyframe("scale", t0 + 0.05, x=1.15, y=1.15, z=1.15),
            make_keyframe("scale", t0 + 0.10, x=1.0, y=1.0, z=1.0),
            make_keyframe("scale", 0.35, x=1, y=1, z=1),
        ]}
    # fins deploy
    for i, (gu, g) in enumerate(fin_segs):
        t0 = 0.18 + i * 0.02
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.00, x=0.0, y=0.0, z=0.0),
            make_keyframe("scale", t0, x=0.0, y=0.0, z=0.0),
            make_keyframe("scale", t0 + 0.08, x=1.0, y=1.0, z=1.0),
            make_keyframe("scale", 0.35, x=1, y=1, z=1),
        ]}
    # aura
    for i, (gu, g) in enumerate(aura_segs):
        t0 = 0.20 + i * 0.015
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.00, x=0.0, y=0.0, z=0.0),
            make_keyframe("scale", t0, x=0.0, y=0.0, z=0.0),
            make_keyframe("scale", t0 + 0.08, x=1.0, y=1.0, z=1.0),
            make_keyframe("scale", 0.35, x=1, y=1, z=1),
        ]}
    # satellites fly in
    for i, (gu, g) in enumerate(sat_segs):
        t0 = 0.22 + i * 0.012
        ang = (i / 6.0) * 2 * math.pi
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.00, x=math.cos(ang) * 6, y=math.sin(ang) * 6, z=0),
            make_keyframe("position", t0, x=math.cos(ang) * 6, y=math.sin(ang) * 6, z=0),
            make_keyframe("position", t0 + 0.10, x=0, y=0, z=0),
            make_keyframe("scale", 0.00, x=0.0, y=0.0, z=0.0),
            make_keyframe("scale", t0 + 0.10, x=1.0, y=1.0, z=1.0),
            make_keyframe("scale", 0.35, x=1, y=1, z=1),
        ]}
    # wake fans out behind
    for i, (gu, g) in enumerate(wake_segs):
        t0 = 0.18 + i * 0.012
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.00, x=0.0, y=0.0, z=0.0),
            make_keyframe("scale", t0, x=0.0, y=0.0, z=0.0),
            make_keyframe("scale", t0 + 0.08, x=1.0, y=1.0, z=1.0),
            make_keyframe("scale", 0.35, x=1, y=1, z=1),
        ]}
    b.add_animation("spawn", length=0.35, animators=spawn, loop="once", override=True)

    # ---- IDLE: 2.0s, lance rolls long axis, segments pulse, sats orbit ----
    idle = {}
    # Root rolls slowly on Z axis (long axis)
    idle[root_uuid] = {"name": "root", "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, x=0, y=0, z=0),
        make_keyframe("rotation", 0.5, x=0, y=0, z=90),
        make_keyframe("rotation", 1.0, x=0, y=0, z=180),
        make_keyframe("rotation", 1.5, x=0, y=0, z=270),
        make_keyframe("rotation", 2.0, x=0, y=0, z=360),
    ]}
    # Each shaft segment independent pulse
    for i, (gu, g) in enumerate(shaft_segs):
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_scale_kfs(2.0, 20, base=1.0, amp=0.04,
                                               period=1.8, phase=i * 0.18)}
    # Tip pulses emissive (via scale flicker)
    for i, (gu, g) in enumerate(tip_segs):
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_scale_kfs(2.0, 16, base=1.0, amp=0.06,
                                               period=1.2, phase=i * 0.2)}
    # Sats orbit Z plane around shaft
    for i, (gu, g) in enumerate(sat_segs):
        ang0 = (i / 6.0) * 2 * math.pi
        # Combine orbit with own keyframe set
        kfs = []
        steps = 24
        for k in range(steps + 1):
            t = (k / steps) * 2.0
            ang = ang0 + (t / 2.0) * 2 * math.pi
            cx = math.cos(ang) * 4.5
            cy = math.sin(ang) * 4.5
            # position is delta from group origin; group origin already at start position
            dx = cx - math.cos(ang0) * 4.5
            dy = cy - math.sin(ang0) * 4.5
            kfs.append(make_keyframe("position", t, x=dx, y=dy, z=0))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}
    # Fins flex
    for i, (gu, g) in enumerate(fin_segs):
        kfs = osc_rot_kfs(2.0, 16, amp_x=0.0, amp_y=0.0, amp_z=1.0,
                          period=1.5, phase=i * 0.25)
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}
    # Wake ambient
    for i, (gu, g) in enumerate(wake_segs):
        kfs = osc_scale_kfs(2.0, 14, base=1.0, amp=0.05, period=1.7, phase=i * 0.13)
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}
    # Aura ambient
    for i, (gu, g) in enumerate(aura_segs):
        kfs = osc_scale_kfs(2.0, 14, base=1.0, amp=0.07, period=1.6, phase=i * 0.4)
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}
    b.add_animation("idle", length=2.0, animators=idle, loop="loop", override=False)

    # ---- DISSIPATE: 0.3s — tip continues forward, shafts scatter ----
    diss = {}
    for i, (gu, g) in enumerate(tip_segs):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.00, x=0, y=0, z=0),
            make_keyframe("position", 0.30, x=0, y=0, z=5.0),
            make_keyframe("scale", 0.00, x=1, y=1, z=1),
            make_keyframe("scale", 0.30, x=0.0, y=0.0, z=0.0),
        ]}
    for i, (gu, g) in enumerate(shaft_segs):
        ang = (i / 7.0) * 2 * math.pi
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.00, x=0, y=0, z=0),
            make_keyframe("position", 0.30, x=math.cos(ang) * 6, y=math.sin(ang) * 6, z=0),
            make_keyframe("scale", 0.00, x=1, y=1, z=1),
            make_keyframe("scale", 0.30, x=0.05, y=0.05, z=0.05),
            make_keyframe("rotation", 0.0, x=0, y=0, z=0),
            make_keyframe("rotation", 0.30, x=180 + i * 20, y=90 + i * 30, z=120),
        ]}
    for i, (gu, g) in enumerate(fin_segs):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("rotation", 0.00, x=0, y=0, z=0),
            make_keyframe("rotation", 0.30, x=720, y=360, z=0),
            make_keyframe("scale", 0.00, x=1, y=1, z=1),
            make_keyframe("scale", 0.30, x=0.0, y=0.0, z=0.0),
        ]}
    for i, (gu, g) in enumerate(aura_segs):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.00, x=1, y=1, z=1),
            make_keyframe("scale", 0.20, x=2.0, y=2.0, z=2.0),
            make_keyframe("scale", 0.30, x=0.0, y=0.0, z=0.0),
        ]}
    for i, (gu, g) in enumerate(sat_segs):
        ang0 = (i / 6.0) * 2 * math.pi
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.00, x=0, y=0, z=0),
            make_keyframe("position", 0.30, x=math.cos(ang0 + math.pi/2) * 8,
                          y=math.sin(ang0 + math.pi/2) * 8, z=0),
            make_keyframe("scale", 0.00, x=1, y=1, z=1),
            make_keyframe("scale", 0.30, x=0.1, y=0.1, z=0.1),
        ]}
    for i, (gu, g) in enumerate(wake_segs):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.00, x=0, y=0, z=0),
            make_keyframe("position", 0.30, x=0, y=0, z=-3.0 - i * 0.5),
            make_keyframe("scale", 0.00, x=1, y=1, z=1),
            make_keyframe("scale", 0.30, x=0.0, y=0.0, z=0.0),
        ]}
    b.add_animation("dissipate", length=0.3, animators=diss, loop="once", override=True)

    b.write(OUTPUT)
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    build()
