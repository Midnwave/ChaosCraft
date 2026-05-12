#!/usr/bin/env python3
"""Generator: frozen_clock_me.bbmodel — FreezingIce attack 20 'Last Winter'.

Clock face of pure ice — hands frozen, face cracked. Floats and rotates,
but the hands DO NOT MOVE relative to the face. Time has stopped.
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

OUTPUT = "D:/CC/ChaosCraft/src/main/resources/models/freezingice/me_attacks/frozen_clock_me.bbmodel"
W = H = 128


def tex_face():
    """Frozen clock face — looking through thick ice."""
    pale       = hex_color("#d8f0ff")
    body       = hex_color("#90b8d8")
    deep       = hex_color("#2860a0")
    frame_dark = hex_color("#081828")
    emiss      = hex_color("#80c8ff")
    spec       = hex_color("#ffffff")
    pixels = new_pixels(W, H, body)
    # Radial gradient: pale outside -> body -> deep -> emiss centre
    cx, cy = W / 2, H / 2
    for y in range(H):
        for x in range(W):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            t = min(1.0, d / (W * 0.5))
            if t < 0.15:
                col = emiss
            elif t < 0.35:
                tt = (t - 0.15) / 0.20
                col = (emiss[0] + int((deep[0] - emiss[0]) * tt),
                       emiss[1] + int((deep[1] - emiss[1]) * tt),
                       emiss[2] + int((deep[2] - emiss[2]) * tt), 255)
            elif t < 0.7:
                tt = (t - 0.35) / 0.35
                col = (deep[0] + int((body[0] - deep[0]) * tt),
                       deep[1] + int((body[1] - deep[1]) * tt),
                       deep[2] + int((body[2] - deep[2]) * tt), 255)
            elif t < 0.9:
                tt = (t - 0.7) / 0.20
                col = (body[0] + int((pale[0] - body[0]) * tt),
                       body[1] + int((pale[1] - body[1]) * tt),
                       body[2] + int((pale[2] - body[2]) * tt), 255)
            else:
                col = pale
            pixels[y * W + x] = col
    # Faint concentric rings at 30% and 70% radius
    for y in range(H):
        for x in range(W):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            r30 = W * 0.30
            r70 = W * 0.35
            if abs(d - r30) < 1.5:
                p = pixels[y * W + x]
                pixels[y * W + x] = (
                    min(255, p[0] + 20), min(255, p[1] + 25), min(255, p[2] + 30), 255,
                )
            elif abs(d - r70) < 1.5:
                p = pixels[y * W + x]
                pixels[y * W + x] = (
                    min(255, p[0] + 15), min(255, p[1] + 20), min(255, p[2] + 25), 255,
                )
    # Top edge pure white
    for y in range(0, 6):
        for x in range(W):
            pixels[y * W + x] = spec
    random_bright_specks(pixels, W, H, 100, spec, seed=11, size_max=1)
    random_bright_specks(pixels, W, H, 50, emiss, seed=22, size_max=1)
    per_pixel_jitter(pixels, W, H, magnitude=14, seed=33, density=0.7)
    return png_from_pixels(pixels, W, H)


def tex_cracks():
    """Deep navy with bright frost-white crack lines."""
    deep    = hex_color("#040e1a")
    bright  = hex_color("#c0e8ff")
    spec    = hex_color("#ffffff")
    pixels = new_pixels(W, H, deep)
    rng = random.Random(909)
    # Multiple crack lines criss-crossing
    for _ in range(35):
        x0 = rng.randrange(W)
        y0 = rng.randrange(H)
        ang = rng.uniform(0, 2 * math.pi)
        length = rng.randrange(20, 60)
        for k in range(length):
            x = int(x0 + math.cos(ang) * k)
            y = int(y0 + math.sin(ang) * k)
            if 0 <= x < W and 0 <= y < H:
                pixels[y * W + x] = bright
            # Branch
            if k % 10 == 0:
                ang += rng.uniform(-0.4, 0.4)
    random_bright_specks(pixels, W, H, 200, bright, seed=44, size_max=1)
    random_bright_specks(pixels, W, H, 50, spec, seed=55, size_max=1)
    per_pixel_jitter(pixels, W, H, magnitude=14, seed=66, density=0.8)
    return png_from_pixels(pixels, W, H)


def build():
    b = Builder("frozen_clock_me", resolution=(W, H), visible_box=(14, 14, 0))
    b.add_texture("clock_face", tex_face())
    b.add_texture("crack_markers", tex_cracks())

    F0 = lambda: uniform_face(0, 0, W, H, tex_index=0)
    F1 = lambda: uniform_face(0, 0, W, H, tex_index=1)

    # ---- 10 face disc slabs in a circle (perpendicular to ground = facing X) ----
    # The clock stands vertical: Y up, X is face normal. Disc lies in YZ plane.
    face_segs = []
    for i in range(10):
        ang = (i / 10.0) * 2 * math.pi
        py = math.cos(ang) * 4.0
        pz = math.sin(ang) * 4.0
        cube = b.add_cube(
            f"face_slab_{i+1}",
            from_=[-0.2, -0.6, -0.4], to_=[0.2, 0.6, 0.4],
            faces=F0(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"face_slab_grp_{i+1}", [0, py, pz], [cube],
            rotation=[math.degrees(ang) + 90, 0, 0], group_uuid=gu,
        )
        face_segs.append((gu, g))

    # Central face disc — large flat slab (front facing X)
    face_center = b.add_cube(
        "face_center",
        from_=[-0.15, -3.5, -3.5], to_=[0.15, 3.5, 3.5],
        faces=F0(),
    )
    face_center_gu = make_uuid()
    face_center_g = b.make_group("face_center_grp", [0, 0, 0], [face_center],
                                 group_uuid=face_center_gu)

    # ---- 12 hour markers around face ----
    hour_segs = []
    for h in range(12):
        ang = (h / 12.0) * 2 * math.pi - math.pi / 2  # 12 at top
        py = math.cos(ang) * 3.3
        pz = math.sin(ang) * 3.3
        cube = b.add_cube(
            f"hour_{h+1}",
            from_=[-0.12, -0.3, -0.15], to_=[0.18, 0.3, 0.15],
            faces=F1(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"hour_grp_{h+1}", [0.16, py, pz], [cube],
            rotation=[math.degrees(ang) + 90, 0, 0], group_uuid=gu,
        )
        hour_segs.append((gu, g))

    # ---- Frozen hour hand: 3 cubes at 11 o'clock ----
    hour_hand_cubes = []
    hour_hand_cubes.append(b.add_cube(
        "hour_hand_1",
        from_=[0.18, -0.08, -0.08], to_=[0.22, 1.5, 0.08],
        faces=F0(),
    ))
    hour_hand_cubes.append(b.add_cube(
        "hour_hand_2",
        from_=[0.18, 1.4, -0.05], to_=[0.22, 2.0, 0.05],
        faces=F0(),
    ))
    hour_hand_cubes.append(b.add_cube(
        "hour_hand_3",
        from_=[0.18, 1.9, -0.03], to_=[0.22, 2.3, 0.03],
        faces=F0(),
    ))
    hour_hand_gu = make_uuid()
    # 11 o'clock — about 22.5° via bone rotation
    hour_hand_g = b.make_group(
        "hour_hand", [0, 0, 0], hour_hand_cubes,
        rotation=[-30, 0, 0],  # rotate on X to put hand at 11 o'clock
        group_uuid=hour_hand_gu,
    )

    # ---- Frozen minute hand: 2 cubes, longer, at 12 o'clock ----
    min_hand_cubes = []
    min_hand_cubes.append(b.add_cube(
        "min_hand_1",
        from_=[0.22, -0.06, -0.06], to_=[0.26, 2.8, 0.06],
        faces=F0(),
    ))
    min_hand_cubes.append(b.add_cube(
        "min_hand_2",
        from_=[0.22, 2.7, -0.04], to_=[0.26, 3.2, 0.04],
        faces=F0(),
    ))
    min_hand_gu = make_uuid()
    min_hand_g = b.make_group(
        "min_hand", [0, 0, 0], min_hand_cubes,
        rotation=[0, 0, 0],  # 12 o'clock (straight up)
        group_uuid=min_hand_gu,
    )

    # ---- 6 frost cracks across face ----
    crack_segs = []
    for i in range(6):
        ang = (i / 6.0) * math.pi  # cracks across face in various angles
        cube = b.add_cube(
            f"crack_{i+1}",
            from_=[0.21, -3.0, -0.15], to_=[0.25, 3.0, 0.15],
            faces=F1(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"crack_grp_{i+1}", [0, 0, 0], [cube],
            rotation=[math.degrees(ang), 0, 0], group_uuid=gu,
        )
        crack_segs.append((gu, g))

    # ---- Frame border: 4 rectangular slab pieces (square frame) ----
    frame_segs = []
    frame_specs = [
        # top, bottom, left, right of square frame
        (-0.05, 4.0, -4.2, 0.15, 4.5, 4.2),     # top
        (-0.05, -4.5, -4.2, 0.15, -4.0, 4.2),   # bottom
        (-0.05, -4.5, 3.9, 0.15, 4.5, 4.2),     # right
        (-0.05, -4.5, -4.2, 0.15, 4.5, -3.9),   # left
    ]
    for i, (x0, y0, z0, x1, y1, z1) in enumerate(frame_specs):
        cube = b.add_cube(
            f"frame_{i+1}",
            from_=[x0, y0, z0], to_=[x1, y1, z1],
            faces=F0(),
        )
        gu = make_uuid()
        g = b.make_group(f"frame_grp_{i+1}", [0, 0, 0], [cube], group_uuid=gu)
        frame_segs.append((gu, g))

    # ---- Pendulum: 2 cubes (rod + weight) hanging below ----
    pend_cubes = []
    pend_cubes.append(b.add_cube(
        "pend_rod",
        from_=[-0.05, -7.0, -0.05], to_=[0.05, -4.5, 0.05],
        faces=F0(),
    ))
    pend_cubes.append(b.add_cube(
        "pend_weight",
        from_=[-0.5, -7.8, -0.5], to_=[0.5, -6.8, 0.5],
        faces=F0(),
    ))
    pend_gu = make_uuid()
    # Pendulum frozen mid-swing — slight Z rotation
    pend_g = b.make_group("pendulum", [0, -4.5, 0], pend_cubes,
                          rotation=[0, 0, 15], group_uuid=pend_gu)

    # ---- ROOT ----
    # IMPORTANT: hands must NOT be children of the rotating face — so they
    # animate independently. We'll put face on a sub-group that rotates,
    # and hands at root level.
    face_rotating_children = [face_center_g]
    face_rotating_children += [g for (_u, g) in face_segs + hour_segs + crack_segs + frame_segs]
    face_rotating_gu = make_uuid()
    face_rotating_g = b.make_group(
        "face_rotating", [0, 0, 0], face_rotating_children,
        group_uuid=face_rotating_gu,
    )

    root_children = [face_rotating_g, hour_hand_g, min_hand_g, pend_g]
    root_uuid = make_uuid()
    root = b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid)
    b.outliner.append(root)

    # ---- SPAWN: 1.3s ----
    spawn = {}
    # Frame at 0.2s
    for i, (gu, g) in enumerate(frame_segs):
        t0 = 0.20 + i * 0.04
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.10, x=1, y=1, z=1),
            make_keyframe("scale", 1.30, x=1, y=1, z=1),
        ]}
    # Face disc slabs phase in clockwise at 0.3s
    spawn[face_center_gu] = {"name": face_center_g["name"], "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, x=0, y=0, z=0),
        make_keyframe("scale", 0.30, x=0, y=0, z=0),
        make_keyframe("scale", 0.45, x=1, y=1, z=1),
        make_keyframe("scale", 1.30, x=1, y=1, z=1),
    ]}
    for i, (gu, g) in enumerate(face_segs):
        t0 = 0.30 + i * 0.05
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.08, x=1, y=1, z=1),
            make_keyframe("scale", 1.30, x=1, y=1, z=1),
        ]}
    # Hour markers at 0.6s
    for i, (gu, g) in enumerate(hour_segs):
        t0 = 0.60 + i * 0.025
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.05, x=1.2, y=1.2, z=1.2),
            make_keyframe("scale", t0 + 0.08, x=1, y=1, z=1),
            make_keyframe("scale", 1.30, x=1, y=1, z=1),
        ]}
    # Cracks at 0.8s
    for i, (gu, g) in enumerate(crack_segs):
        t0 = 0.80 + i * 0.03
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=1, z=1),
            make_keyframe("scale", t0, x=0, y=1, z=1),
            make_keyframe("scale", t0 + 0.08, x=1, y=1, z=1),
            make_keyframe("scale", 1.30, x=1, y=1, z=1),
        ]}
    # Clock hands at 1.0s — materialise and immediately stop
    spawn[hour_hand_gu] = {"name": hour_hand_g["name"], "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, x=0, y=0, z=0),
        make_keyframe("scale", 1.00, x=0, y=0, z=0),
        make_keyframe("scale", 1.10, x=1, y=1, z=1),
        make_keyframe("scale", 1.30, x=1, y=1, z=1),
    ]}
    spawn[min_hand_gu] = {"name": min_hand_g["name"], "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, x=0, y=0, z=0),
        make_keyframe("scale", 1.00, x=0, y=0, z=0),
        make_keyframe("scale", 1.10, x=1, y=1, z=1),
        make_keyframe("scale", 1.30, x=1, y=1, z=1),
    ]}
    # Pendulum materialises mid-swing and freezes
    spawn[pend_gu] = {"name": pend_g["name"], "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, x=0, y=0, z=0),
        make_keyframe("scale", 1.05, x=0, y=0, z=0),
        make_keyframe("scale", 1.15, x=1, y=1, z=1),
        make_keyframe("scale", 1.30, x=1, y=1, z=1),
    ]}
    b.add_animation("spawn", length=1.30, animators=spawn, loop="once", override=True)

    # ---- IDLE: 6.0s — face rotates Y slowly, hands DO NOT MOVE ----
    idle = {}
    # Face rotates Y axis
    idle[face_rotating_gu] = {"name": face_rotating_g["name"], "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, x=360, y=0, z=0),
        make_keyframe("rotation", 1.5, x=360, y=90, z=0),
        make_keyframe("rotation", 3.0, x=360, y=180, z=0),
        make_keyframe("rotation", 4.5, x=360, y=270, z=0),
        make_keyframe("rotation", 6.0, x=360, y=360, z=0),
    ]}
    # Root floats up/down slightly
    idle[root_uuid] = {"name": "root", "type": "bone", "keyframes": [
        make_keyframe("position", 0.0, x=0, y=0, z=0),
        make_keyframe("position", 3.0, x=0, y=0.3, z=0),
        make_keyframe("position", 6.0, x=0, y=0, z=0),
    ]}
    # Hands DO NOT MOVE — explicit static keyframes
    idle[hour_hand_gu] = {"name": hour_hand_g["name"], "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, x=-30, y=0, z=0),
        make_keyframe("rotation", 6.0, x=-30, y=0, z=0),
    ]}
    idle[min_hand_gu] = {"name": min_hand_g["name"], "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, x=0, y=0, z=0),
        make_keyframe("rotation", 6.0, x=0, y=0, z=0),
    ]}
    # Frost cracks pulse emissive (scale)
    for i, (gu, g) in enumerate(crack_segs):
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_scale_kfs(6.0, 24, base=1.0, amp=0.10,
                                               period=2.5, phase=i * 0.4)}
    # Hour markers flicker
    for i, (gu, g) in enumerate(hour_segs):
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_scale_kfs(6.0, 30, base=1.0, amp=0.06,
                                               period=1.0 + (i % 4) * 0.2, phase=i * 0.3)}
    # Pendulum frozen mid-swing
    idle[pend_gu] = {"name": pend_g["name"], "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, x=0, y=0, z=15),
        make_keyframe("rotation", 6.0, x=0, y=0, z=15),
    ]}
    b.add_animation("idle", length=6.0, animators=idle, loop="loop", override=False)

    # ---- DISSIPATE: 0.8s — pendulum falls first, hands fly off, face cracks apart ----
    diss = {}
    # Pendulum falls and scales to 0
    diss[pend_gu] = {"name": pend_g["name"], "type": "bone", "keyframes": [
        make_keyframe("position", 0.0, x=0, y=0, z=0),
        make_keyframe("position", 0.40, x=0, y=-5, z=0),
        make_keyframe("scale", 0.0, x=1, y=1, z=1),
        make_keyframe("scale", 0.40, x=0, y=0, z=0),
    ]}
    # Hands fly off
    diss[hour_hand_gu] = {"name": hour_hand_g["name"], "type": "bone", "keyframes": [
        make_keyframe("position", 0.0, x=0, y=0, z=0),
        make_keyframe("position", 0.50, x=2, y=3, z=2),
        make_keyframe("scale", 0.0, x=1, y=1, z=1),
        make_keyframe("scale", 0.50, x=0, y=0, z=0),
        make_keyframe("rotation", 0.0, x=-30, y=0, z=0),
        make_keyframe("rotation", 0.50, x=720, y=540, z=360),
    ]}
    diss[min_hand_gu] = {"name": min_hand_g["name"], "type": "bone", "keyframes": [
        make_keyframe("position", 0.0, x=0, y=0, z=0),
        make_keyframe("position", 0.50, x=-2, y=4, z=-1),
        make_keyframe("scale", 0.0, x=1, y=1, z=1),
        make_keyframe("scale", 0.50, x=0, y=0, z=0),
        make_keyframe("rotation", 0.0, x=0, y=0, z=0),
        make_keyframe("rotation", 0.50, x=540, y=720, z=180),
    ]}
    # Cracks spread then crack face apart
    for i, (gu, g) in enumerate(crack_segs):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.40, x=2.5, y=1, z=1),
            make_keyframe("scale", 0.80, x=0, y=0, z=0),
        ]}
    # Face slabs scatter
    for i, (gu, g) in enumerate(face_segs):
        ang = (i / 10.0) * 2 * math.pi
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.30, x=0, y=0, z=0),
            make_keyframe("position", 0.80, x=math.cos(ang) * 5, y=math.sin(ang) * 2,
                          z=math.sin(ang) * 5),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.80, x=0, y=0, z=0),
            make_keyframe("rotation", 0.0, x=0, y=0, z=0),
            make_keyframe("rotation", 0.80, x=360, y=540, z=180),
        ]}
    diss[face_center_gu] = {"name": face_center_g["name"], "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, x=1, y=1, z=1),
        make_keyframe("scale", 0.50, x=1, y=1, z=1),
        make_keyframe("scale", 0.80, x=0, y=0, z=0),
    ]}
    # Hour markers
    for i, (gu, g) in enumerate(hour_segs):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.80, x=0, y=0, z=0),
        ]}
    # Frame collapses
    for i, (gu, g) in enumerate(frame_segs):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.60, x=1, y=1, z=1),
            make_keyframe("scale", 0.80, x=0, y=0, z=0),
        ]}
    b.add_animation("dissipate", length=0.80, animators=diss, loop="once", override=True)

    b.write(OUTPUT)
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    build()
