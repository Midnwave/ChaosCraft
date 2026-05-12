#!/usr/bin/env python3
"""Generator: cryo_satellite_ring.bbmodel — FreezingIce attack 18 'Orbit Decay'.

8 cryo satellite units orbiting in ring — each with body, solar panels, sensor,
thrusters. Progressive orbit decay: satellite N tilts N degrees.
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

OUTPUT = "D:/CC/ChaosCraft/src/main/resources/models/freezingice/me_attacks/cryo_satellite_ring.bbmodel"
W = H = 128


def tex_satellite():
    """Ice-coated metal panels — almost-visible panel grid beneath frost."""
    metal     = hex_color("#101418")
    frost_meta = hex_color("#283848")
    coated    = hex_color("#4878a0")
    panel     = hex_color("#90c8e8")
    spec      = hex_color("#d0ecff")
    emiss     = hex_color("#60b0e0")
    pixels = new_pixels(W, H, frost_meta)
    # Base gradient — frost-coated metal
    for y in range(H):
        for x in range(W):
            t = (x + y) / float(W + H)
            tt = t * 1.2
            r = int(metal[0] + (coated[0] - metal[0]) * min(1, tt))
            g = int(metal[1] + (coated[1] - metal[1]) * min(1, tt))
            bl = int(metal[2] + (coated[2] - metal[2]) * min(1, tt))
            pixels[y * W + x] = (r, g, bl, 255)
    # Panel grid lines — horizontal at rows 48, 80 (rows 24, 40 scaled to 128)
    for y in [40, 48, 80, 88]:
        for x in range(W):
            p = pixels[y * W + x]
            pixels[y * W + x] = (
                min(255, p[0] + 25), min(255, p[1] + 30), min(255, p[2] + 35), 255,
            )
    # Vertical panel seams every 32 px
    for x in [32, 64, 96]:
        for y in range(H):
            p = pixels[y * W + x]
            pixels[y * W + x] = (
                min(255, p[0] + 20), min(255, p[1] + 25), min(255, p[2] + 30), 255,
            )
    # Bright sensor blob in lower-right
    specular_blob(pixels, W, H, 100, 100, emiss, radius=8, strength=0.85)
    # Frost panel patches
    rng = random.Random(303)
    for _ in range(40):
        cx = rng.randrange(W)
        cy = rng.randrange(H)
        r = rng.randrange(3, 8)
        for dy in range(-r, r + 1):
            for dx in range(-r, r + 1):
                if dx * dx + dy * dy <= r * r:
                    x, y = cx + dx, cy + dy
                    if 0 <= x < W and 0 <= y < H:
                        d = math.sqrt(dx * dx + dy * dy)
                        t = 1.0 - d / r
                        p = pixels[y * W + x]
                        pixels[y * W + x] = (
                            min(255, p[0] + int(panel[0] * t * 0.4)),
                            min(255, p[1] + int(panel[1] * t * 0.4)),
                            min(255, p[2] + int(panel[2] * t * 0.4)), 255,
                        )
    specular_blob(pixels, W, H, 40, 40, spec, radius=6, strength=0.6)
    random_bright_specks(pixels, W, H, 120, panel, seed=11, size_max=1)
    per_pixel_jitter(pixels, W, H, magnitude=14, seed=22, density=0.8)
    return png_from_pixels(pixels, W, H)


def tex_orbital():
    """Deep space-cold black with precise orbital ring lines."""
    deep    = hex_color("#020406")
    ring1   = hex_color("#3060a0")
    ring2   = hex_color("#5080c0")
    spec    = hex_color("#a0c0e0")
    pixels = new_pixels(W, H, deep)
    cx, cy = W / 2, H / 2
    # Ring lines at 60% and 70%
    for y in range(H):
        for x in range(W):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            r60 = W * 0.30
            r70 = W * 0.35
            if abs(d - r60) < 1.5:
                pixels[y * W + x] = ring1
            elif abs(d - r70) < 1.5:
                pixels[y * W + x] = ring2
    # Sparse stars
    random_bright_specks(pixels, W, H, 80, spec, seed=44, size_max=1)
    random_bright_specks(pixels, W, H, 30, ring2, seed=55, size_max=1)
    per_pixel_jitter(pixels, W, H, magnitude=8, seed=66, density=0.5)
    return png_from_pixels(pixels, W, H)


def build():
    b = Builder("cryo_satellite_ring", resolution=(W, H), visible_box=(18, 10, 0))
    b.add_texture("satellite", tex_satellite())
    b.add_texture("orbital_ring", tex_orbital())

    F0 = lambda: uniform_face(0, 0, W, H, tex_index=0)
    F1 = lambda: uniform_face(0, 0, W, H, tex_index=1)

    RING_R = 8.0
    RING_Y = 6.0

    # ---- 8 satellite bone groups evenly spaced ----
    satellites = []  # (sat_gu, sat_g, tilt_index)
    for i in range(8):
        ang = (i / 8.0) * 2 * math.pi
        sx = math.cos(ang) * RING_R
        sz = math.sin(ang) * RING_R
        # Body: 3-cube rectangular cluster
        body_cubes = []
        body_cubes.append(b.add_cube(
            f"sat{i+1}_body_core",
            from_=[-0.6, -0.4, -0.5], to_=[0.6, 0.4, 0.5],
            faces=F0(),
        ))
        body_cubes.append(b.add_cube(
            f"sat{i+1}_body_upper",
            from_=[-0.4, 0.4, -0.4], to_=[0.4, 0.6, 0.4],
            faces=F0(),
        ))
        body_cubes.append(b.add_cube(
            f"sat{i+1}_body_lower",
            from_=[-0.4, -0.6, -0.4], to_=[0.4, -0.4, 0.4],
            faces=F0(),
        ))
        # Solar panels: 2 wide flat slabs each side
        panel_left = b.add_cube(
            f"sat{i+1}_panel_L",
            from_=[-2.2, -0.05, -0.6], to_=[-0.6, 0.05, 0.6],
            faces=F0(),
        )
        panel_right = b.add_cube(
            f"sat{i+1}_panel_R",
            from_=[0.6, -0.05, -0.6], to_=[2.2, 0.05, 0.6],
            faces=F0(),
        )
        # Sensor: small cube on thin rod
        rod = b.add_cube(
            f"sat{i+1}_rod",
            from_=[-0.05, -0.05, 0.5], to_=[0.05, 0.05, 1.0],
            faces=F0(),
        )
        sensor = b.add_cube(
            f"sat{i+1}_sensor",
            from_=[-0.2, -0.2, 1.0], to_=[0.2, 0.2, 1.4],
            faces=F0(),
        )
        # Thrusters: 2 small flat cubes at back
        thr_l = b.add_cube(
            f"sat{i+1}_thr_L",
            from_=[-0.3, -0.1, -0.7], to_=[-0.1, 0.1, -0.5],
            faces=F0(),
        )
        thr_r = b.add_cube(
            f"sat{i+1}_thr_R",
            from_=[0.1, -0.1, -0.7], to_=[0.3, 0.1, -0.5],
            faces=F0(),
        )
        sat_gu = make_uuid()
        sat_g = b.make_group(
            f"satellite_{i+1}", [sx, RING_Y, sz],
            body_cubes + [panel_left, panel_right, rod, sensor, thr_l, thr_r],
            rotation=[0, math.degrees(ang) + 90, 0],  # face tangent
            group_uuid=sat_gu,
        )
        satellites.append((sat_gu, sat_g, i + 1))  # tilt index 1-8

    # ---- Central orbital station: 5-cube assembly ----
    stat_cubes = []
    stat_positions = [
        (0, 0, 0), (-0.8, 0.4, 0), (0.8, 0.4, 0), (0, 0.4, -0.8), (0, 0.4, 0.8),
    ]
    for i, (sx, sy, sz) in enumerate(stat_positions):
        cube = b.add_cube(
            f"station_{i+1}",
            from_=[sx - 0.6, sy + RING_Y - 0.4, sz - 0.6],
            to_=[sx + 0.6, sy + RING_Y + 0.4, sz + 0.6],
            faces=F0(),
            rotation=[i * 20, i * 30, i * 25],
        )
        stat_cubes.append(cube)
    stat_gu = make_uuid()
    stat_g = b.make_group("station", [0, RING_Y, 0], stat_cubes, group_uuid=stat_gu)

    # ---- Orbital ring indicator: 10 flat slabs ----
    ring_segs = []
    for i in range(10):
        ang = (i / 10.0) * 2 * math.pi
        cx = math.cos(ang) * RING_R
        cz = math.sin(ang) * RING_R
        cube = b.add_cube(
            f"ring_slab_{i+1}",
            from_=[-0.6, -0.05, -0.3], to_=[0.6, 0.05, 0.3],
            faces=F1(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"ring_slab_grp_{i+1}", [cx, RING_Y - 0.5, cz], [cube],
            rotation=[0, math.degrees(ang) + 90, 0], group_uuid=gu,
        )
        ring_segs.append((gu, g))

    # ---- Ice debris field: 10 small irregular cubes around ring ----
    debris_segs = []
    rng = random.Random(909)
    for i in range(10):
        ang = rng.uniform(0, 2 * math.pi)
        r = RING_R + rng.uniform(-1.5, 1.5)
        cx = math.cos(ang) * r
        cz = math.sin(ang) * r
        cy = RING_Y + rng.uniform(-0.5, 0.5)
        sz_ = rng.uniform(0.2, 0.5)
        cube = b.add_cube(
            f"debris_{i+1}",
            from_=[-sz_, -sz_, -sz_], to_=[sz_, sz_, sz_],
            faces=F0(),
            rotation=[i * 20, i * 30, i * 45],
        )
        gu = make_uuid()
        g = b.make_group(f"debris_grp_{i+1}", [cx, cy, cz], [cube], group_uuid=gu)
        debris_segs.append((gu, g))

    # ---- ROOT ----
    children = [g for (_u, g, _t) in satellites]
    children += [stat_g]
    children += [g for (_u, g) in ring_segs + debris_segs]
    root_uuid = make_uuid()
    root = b.make_group("root", [0, 0, 0], children, group_uuid=root_uuid)
    b.outliner.append(root)

    # ---- SPAWN: 1.0s ----
    spawn = {}
    spawn[stat_gu] = {"name": stat_g["name"], "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, x=0, y=0, z=0),
        make_keyframe("scale", 0.12, x=1.2, y=1.2, z=1.2),
        make_keyframe("scale", 0.20, x=1, y=1, z=1),
        make_keyframe("scale", 1.00, x=1, y=1, z=1),
    ]}
    # Orbital ring expands at 0.2s
    for i, (gu, g) in enumerate(ring_segs):
        t0 = 0.20 + i * 0.02
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=1, z=0),
            make_keyframe("scale", t0, x=0, y=1, z=0),
            make_keyframe("scale", t0 + 0.10, x=1, y=1, z=1),
            make_keyframe("scale", 1.00, x=1, y=1, z=1),
        ]}
    # Satellites fly in one by one at 0.1s stagger
    for i, (sgu, sg, ti) in enumerate(satellites):
        t0 = 0.30 + i * 0.08
        ang = (i / 8.0) * 2 * math.pi
        # initial position offset outward
        spawn[sgu] = {"name": sg["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=math.cos(ang) * 6, y=0, z=math.sin(ang) * 6),
            make_keyframe("position", t0, x=math.cos(ang) * 6, y=0, z=math.sin(ang) * 6),
            make_keyframe("position", t0 + 0.10, x=-0.2, y=0, z=-0.2),  # small settle overshoot
            make_keyframe("position", t0 + 0.15, x=0, y=0, z=0),
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.10, x=1, y=1, z=1),
            make_keyframe("scale", 1.00, x=1, y=1, z=1),
        ]}
    # Ice debris at 0.6s
    for i, (gu, g) in enumerate(debris_segs):
        t0 = 0.60 + i * 0.025
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.08, x=1, y=1, z=1),
            make_keyframe("scale", 1.00, x=1, y=1, z=1),
        ]}
    b.add_animation("spawn", length=1.00, animators=spawn, loop="once", override=True)

    # ---- IDLE: 6.0s — orbit + progressive tilt ----
    idle = {}
    # Each satellite orbits + tilts (orbit by re-positioning around ring + tilt amount = i degrees)
    for i, (sgu, sg, ti) in enumerate(satellites):
        ang0 = (i / 8.0) * 2 * math.pi
        period = 6.0
        kfs = []
        steps = 36
        for k in range(steps + 1):
            t = (k / steps) * 6.0
            ang = ang0 + (t / period) * 2 * math.pi
            cx = math.cos(ang) * RING_R
            cz = math.sin(ang) * RING_R
            dx = cx - math.cos(ang0) * RING_R
            dz = cz - math.sin(ang0) * RING_R
            kfs.append(make_keyframe("position", t, x=dx, y=0, z=dz))
        # Tilt rotation — keep tilt static (progressive decay)
        tilt_deg = ti  # 1-8 degrees
        kfs.append(make_keyframe("rotation", 0.0, x=tilt_deg, y=0, z=tilt_deg * 0.5))
        kfs.append(make_keyframe("rotation", 3.0, x=tilt_deg + 1, y=0, z=tilt_deg * 0.5 + 0.5))
        kfs.append(make_keyframe("rotation", 6.0, x=tilt_deg, y=0, z=tilt_deg * 0.5))
        idle[sgu] = {"name": sg["name"], "type": "bone", "keyframes": kfs}
    # Sensors glow (via station scale pulse as proxy)
    idle[stat_gu] = {"name": stat_g["name"], "type": "bone",
                     "keyframes": osc_scale_kfs(6.0, 24, base=1.0, amp=0.04,
                                                period=2.0, phase=0)}
    # Debris field orbits at different speed
    for i, (gu, g) in enumerate(debris_segs):
        # treat each debris position relative to its own start. Just rotate it slightly
        kfs = osc_rot_kfs(6.0, 20, amp_x=20, amp_y=30, amp_z=20,
                          period=2.5, phase=i * 0.3)
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}
    # Ring slow rotation
    for i, (gu, g) in enumerate(ring_segs):
        kfs = osc_scale_kfs(6.0, 18, base=1.0, amp=0.03, period=3.0, phase=i * 0.15)
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}
    b.add_animation("idle", length=6.0, animators=idle, loop="loop", override=False)

    # ---- DISSIPATE: 0.8s — satellites fly outward tangent ----
    diss = {}
    for i, (sgu, sg, ti) in enumerate(satellites):
        ang = (i / 8.0) * 2 * math.pi + math.pi / 2
        diss[sgu] = {"name": sg["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.80, x=math.cos(ang) * 12, y=3, z=math.sin(ang) * 12),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.80, x=0.0, y=0.0, z=0.0),
            make_keyframe("rotation", 0.0, x=ti, y=0, z=ti * 0.5),
            make_keyframe("rotation", 0.80, x=360 + ti, y=720, z=360),
        ]}
    for i, (gu, g) in enumerate(debris_segs):
        ang = (i / 10.0) * 2 * math.pi
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.80, x=math.cos(ang) * 8, y=2, z=math.sin(ang) * 8),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.80, x=0.0, y=0.0, z=0.0),
        ]}
    for i, (gu, g) in enumerate(ring_segs):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.60, x=2, y=1, z=2),
            make_keyframe("scale", 0.80, x=0, y=0, z=0),
        ]}
    diss[stat_gu] = {"name": stat_g["name"], "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, x=1, y=1, z=1),
        make_keyframe("scale", 0.80, x=0, y=0, z=0),
    ]}
    b.add_animation("dissipate", length=0.80, animators=diss, loop="once", override=True)

    b.write(OUTPUT)
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    build()
