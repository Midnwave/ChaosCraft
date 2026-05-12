#!/usr/bin/env python3
"""Generator: frozen_sentinel_array.bbmodel — FreezingIce attack 16 'The Watchers'.

5 frozen sentinel shapes orbiting at different heights — angular dark bodies
with single bright ice-blue eye each. The horror is the attention.
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

OUTPUT = "D:/CC/ChaosCraft/src/main/resources/models/freezingice/me_attacks/frozen_sentinel_array.bbmodel"
W = H = 128


def tex_watcher():
    """Frozen watcher — very dark old ice with sharp bright glint line."""
    deep_navy  = hex_color("#020810")
    dark_glac  = hex_color("#061828")
    deep_body  = hex_color("#0c3050")
    glint      = hex_color("#3080b0")
    edge       = hex_color("#90c8f0")
    emiss      = hex_color("#60b0e0")
    pixels = new_pixels(W, H, deep_navy)
    # Vertical gradient — mostly dark
    for y in range(H):
        t = y / float(H)
        if t < 0.3:
            col = deep_navy
        elif t < 0.6:
            tt = (t - 0.3) / 0.3
            col = (deep_navy[0] + int((dark_glac[0] - deep_navy[0]) * tt),
                   deep_navy[1] + int((dark_glac[1] - deep_navy[1]) * tt),
                   deep_navy[2] + int((dark_glac[2] - deep_navy[2]) * tt), 255)
        else:
            tt = (t - 0.6) / 0.4
            col = (dark_glac[0] + int((deep_body[0] - dark_glac[0]) * tt),
                   dark_glac[1] + int((deep_body[1] - dark_glac[1]) * tt),
                   dark_glac[2] + int((deep_body[2] - dark_glac[2]) * tt), 255)
        for x in range(W):
            pixels[y * W + x] = col
    # Bright diagonal glint line at rows ~40-44 across full width
    for y in range(40, 44):
        for x in range(W):
            offset = int(x * 0.2)
            yy = y + offset % 4
            if 0 <= yy < H:
                pixels[yy * W + x] = emiss if y == 41 or y == 42 else glint
    # Sharp bright edge facet lines
    for x in range(0, W, 16):
        for y in range(H):
            p = pixels[y * W + x]
            pixels[y * W + x] = (
                min(255, p[0] + 25), min(255, p[1] + 30), min(255, p[2] + 35), 255,
            )
    random_bright_specks(pixels, W, H, 60, edge, seed=11, size_max=2)
    random_bright_specks(pixels, W, H, 30, emiss, seed=22, size_max=1)
    per_pixel_jitter(pixels, W, H, magnitude=12, seed=33, density=0.7)
    return png_from_pixels(pixels, W, H)


def tex_eye():
    """Ice eye — pure emissive centre, fading outward."""
    centre   = hex_color("#80d0ff")
    iris     = hex_color("#2060a0")
    outer    = hex_color("#060e1a")
    spec     = hex_color("#ffffff")
    pixels = new_pixels(W, H, outer)
    cx, cy = W / 2, H / 2
    for y in range(H):
        for x in range(W):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            t = min(1.0, d / (W * 0.5))
            if t < 0.18:
                col = spec
            elif t < 0.35:
                tt = (t - 0.18) / 0.17
                col = (spec[0] + int((centre[0] - spec[0]) * tt),
                       spec[1] + int((centre[1] - spec[1]) * tt),
                       spec[2] + int((centre[2] - spec[2]) * tt), 255)
            elif t < 0.65:
                tt = (t - 0.35) / 0.30
                col = (centre[0] + int((iris[0] - centre[0]) * tt),
                       centre[1] + int((iris[1] - centre[1]) * tt),
                       centre[2] + int((iris[2] - centre[2]) * tt), 255)
            else:
                tt = (t - 0.65) / 0.35
                col = (iris[0] + int((outer[0] - iris[0]) * tt),
                       iris[1] + int((outer[1] - iris[1]) * tt),
                       iris[2] + int((outer[2] - iris[2]) * tt), 255)
            pixels[y * W + x] = col
    random_bright_specks(pixels, W, H, 60, centre, seed=12, size_max=1)
    per_pixel_jitter(pixels, W, H, magnitude=10, seed=24, density=0.5)
    return png_from_pixels(pixels, W, H)


def build():
    b = Builder("frozen_sentinel_array", resolution=(W, H), visible_box=(20, 14, 0))
    b.add_texture("watcher", tex_watcher())
    b.add_texture("ice_eye", tex_eye())

    F0 = lambda: uniform_face(0, 0, W, H, tex_index=0)
    F1 = lambda: uniform_face(0, 0, W, H, tex_index=1)

    # ---- 5 sentinels at varied radii/heights ----
    sentinel_specs = [
        (6.0, 3.0, 0),    # radius, Y, phase index
        (7.0, 5.0, 1),
        (8.0, 7.0, 2),
        (9.0, 9.0, 3),
        (6.5, 10.0, 4),
    ]
    sentinels = []  # list of (group_uuid, group, eye_group_uuid, eye_group)
    for i, (rad, yh, ph) in enumerate(sentinel_specs):
        ang = (ph / 5.0) * 2 * math.pi
        sx = math.cos(ang) * rad
        sz = math.sin(ang) * rad
        # Body: 3 angular slabs at 22.5° offsets
        body_cubes = []
        for s in range(3):
            rot_offset = s * 22.5
            cube = b.add_cube(
                f"sent{i+1}_body{s+1}",
                from_=[-1.2, -1.5, -0.2], to_=[1.2, 1.5, 0.2],
                faces=F0(),
                rotation=[0, 0, rot_offset],
            )
            body_cubes.append(cube)
        # Spike accents top & bottom
        spike_top = b.add_cube(
            f"sent{i+1}_spike_top",
            from_=[-0.2, 1.5, -0.2], to_=[0.2, 2.4, 0.2],
            faces=F0(),
        )
        spike_bot = b.add_cube(
            f"sent{i+1}_spike_bot",
            from_=[-0.2, -2.4, -0.2], to_=[0.2, -1.5, 0.2],
            faces=F0(),
        )
        # Eye: 4 flat slabs in circle approximating oval
        eye_cubes = []
        for e in range(4):
            ang_e = (e / 4.0) * 2 * math.pi
            ex = math.cos(ang_e) * 0.5
            ey = math.sin(ang_e) * 0.35
            cube_e = b.add_cube(
                f"sent{i+1}_eye{e+1}",
                from_=[ex - 0.25, ey - 0.18, 0.21], to_=[ex + 0.25, ey + 0.18, 0.35],
                faces=F1(),
            )
            eye_cubes.append(cube_e)
        eye_gu = make_uuid()
        eye_g = b.make_group(f"sent{i+1}_eye_grp", [sx, yh, sz], eye_cubes, group_uuid=eye_gu)

        # Orbital glow ring: 6 small slabs around sentinel
        ring_cubes = []
        for r in range(6):
            ang_r = (r / 6.0) * 2 * math.pi
            rx = math.cos(ang_r) * 1.8
            ry = math.sin(ang_r) * 1.8
            ring_cubes.append(b.add_cube(
                f"sent{i+1}_ring{r+1}",
                from_=[rx - 0.25, ry - 0.05, -0.4], to_=[rx + 0.25, ry + 0.05, 0.4],
                faces=F1(),
                rotation=[0, 0, math.degrees(ang_r)],
            ))

        # All body parts together (no eye — eye is its own bone for head-tracking)
        # Eye is included as a child group of the sentinel parent
        sent_gu = make_uuid()
        sent_children = body_cubes + [spike_top, spike_bot] + ring_cubes + [eye_g]
        sent_g = b.make_group(
            f"sentinel_{i+1}", [sx, yh, sz], sent_children, group_uuid=sent_gu,
        )
        sentinels.append((sent_gu, sent_g, eye_gu, eye_g))

    # ---- Central convergence: 4-cube sphere at Y=8 ----
    conv_cubes = []
    conv_positions = [
        (-1.0, 8.0, -1.0), (1.0, 8.5, -0.5), (-0.5, 8.5, 1.0), (0.5, 7.5, 0.5),
    ]
    for i, (cx, cy, cz) in enumerate(conv_positions):
        cube = b.add_cube(
            f"convergence_{i+1}",
            from_=[cx - 0.8, cy - 0.8, cz - 0.8], to_=[cx + 0.8, cy + 0.8, cz + 0.8],
            faces=F1(),
            rotation=[i * 30, i * 45, i * 20],
        )
        conv_cubes.append(cube)
    conv_gu = make_uuid()
    conv_g = b.make_group("convergence", [0, 8, 0], conv_cubes, group_uuid=conv_gu)

    # ---- Orbit path indicators: 5 rings of 8 slabs each ----
    orbit_paths = []  # list of (path_gu, path_g)
    for i, (rad, yh, ph) in enumerate(sentinel_specs):
        path_cubes = []
        for p in range(8):
            ang = (p / 8.0) * 2 * math.pi
            cx = math.cos(ang) * rad
            cz = math.sin(ang) * rad
            cube = b.add_cube(
                f"path{i+1}_{p+1}",
                from_=[cx - 0.5, yh - 0.05, cz - 0.3], to_=[cx + 0.5, yh + 0.05, cz + 0.3],
                faces=F1(),
                rotation=[0, math.degrees(ang) + 90, 0],
            )
            path_cubes.append(cube)
        path_gu = make_uuid()
        path_g = b.make_group(f"orbit_path_{i+1}", [0, yh, 0], path_cubes, group_uuid=path_gu)
        orbit_paths.append((path_gu, path_g))

    # ---- ROOT ----
    children = [g for (_u, g, _eu, _eg) in sentinels]
    children += [conv_g]
    children += [g for (_u, g) in orbit_paths]
    root_uuid = make_uuid()
    root = b.make_group("root", [0, 0, 0], children, group_uuid=root_uuid)
    b.outliner.append(root)

    # ---- SPAWN: 1.1s ----
    spawn = {}
    # Convergence at 0.0s
    spawn[conv_gu] = {"name": conv_g["name"], "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, x=0, y=0, z=0),
        make_keyframe("scale", 0.15, x=1.1, y=1.1, z=1.1),
        make_keyframe("scale", 0.25, x=1, y=1, z=1),
        make_keyframe("scale", 1.10, x=1, y=1, z=1),
    ]}
    # Orbit paths at 0.2s
    for i, (gu, g) in enumerate(orbit_paths):
        t0 = 0.20 + i * 0.04
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0.05, y=1, z=0.05),
            make_keyframe("scale", t0, x=0.05, y=1, z=0.05),
            make_keyframe("scale", t0 + 0.15, x=1, y=1, z=1),
            make_keyframe("scale", 1.10, x=1, y=1, z=1),
        ]}
    # Sentinels fly in one by one at 0.3s stagger
    for i, (sgu, sg, egu, eg) in enumerate(sentinels):
        t0 = 0.30 + i * 0.15
        rad, yh, ph = sentinel_specs[i]
        ang = (ph / 5.0) * 2 * math.pi
        sx0 = math.cos(ang) * (rad + 8)
        sz0 = math.sin(ang) * (rad + 8)
        sx1 = math.cos(ang) * rad
        sz1 = math.sin(ang) * rad
        spawn[sgu] = {"name": sg["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=sx0 - sx1, y=0, z=sz0 - sz1),
            make_keyframe("position", t0, x=sx0 - sx1, y=0, z=sz0 - sz1),
            make_keyframe("position", t0 + 0.12, x=0, y=0, z=0),
            make_keyframe("scale", 0.0, x=0.05, y=0.05, z=0.05),
            make_keyframe("scale", t0, x=0.05, y=0.05, z=0.05),
            make_keyframe("scale", t0 + 0.08, x=1.15, y=1.15, z=1.15),
            make_keyframe("scale", t0 + 0.15, x=1, y=1, z=1),
            make_keyframe("scale", 1.10, x=1, y=1, z=1),
        ]}
        # Eye opens last
        t_eye = t0 + 0.15
        spawn[egu] = {"name": eg["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t_eye, x=0, y=0, z=0),
            make_keyframe("scale", t_eye + 0.08, x=1.3, y=1.3, z=1.3),
            make_keyframe("scale", t_eye + 0.15, x=1, y=1, z=1),
            make_keyframe("scale", 1.10, x=1, y=1, z=1),
        ]}
    b.add_animation("spawn", length=1.10, animators=spawn, loop="once", override=True)

    # ---- IDLE: 8.0s — orbital + head tracking via Y rotation ----
    idle = {}
    # 5 sentinels orbit at different speeds (6-10s)
    periods = [6.0, 7.0, 8.0, 9.0, 10.0]
    for i, (sgu, sg, egu, eg) in enumerate(sentinels):
        rad, yh, ph = sentinel_specs[i]
        ang0 = (ph / 5.0) * 2 * math.pi
        period = periods[i]
        kfs = []
        steps = 40
        for k in range(steps + 1):
            t = (k / steps) * 8.0
            ang = ang0 + (t / period) * 2 * math.pi
            cx = math.cos(ang) * rad
            cz = math.sin(ang) * rad
            dx = cx - math.cos(ang0) * rad
            dz = cz - math.sin(ang0) * rad
            kfs.append(make_keyframe("position", t, x=dx, y=0, z=dz))
        idle[sgu] = {"name": sg["name"], "type": "bone", "keyframes": kfs}
        # Eye head-tracking — Y rotation slowly turns
        eye_kfs = []
        for k in range(40 + 1):
            t = (k / 40) * 8.0
            # Approximate "look at center" — eye rotates by ang+180
            ang = ang0 + (t / period) * 2 * math.pi
            head_y = math.degrees(ang + math.pi)
            eye_kfs.append(make_keyframe("rotation", t, x=0, y=head_y, z=0))
        idle[egu] = {"name": eg["name"], "type": "bone", "keyframes": eye_kfs}
    # Convergence pulses
    idle[conv_gu] = {"name": conv_g["name"], "type": "bone",
                     "keyframes": osc_scale_kfs(8.0, 32, base=1.0, amp=0.04,
                                                period=2.5, phase=0)}
    # Orbit paths barely rotate (slow Y rotation)
    for i, (gu, g) in enumerate(orbit_paths):
        kfs = []
        for k in range(20 + 1):
            t = (k / 20) * 8.0
            yr = (t / 8.0) * 30  # 30 degrees over 8s
            kfs.append(make_keyframe("rotation", t, x=0, y=yr, z=0))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}
    b.add_animation("idle", length=8.0, animators=idle, loop="loop", override=False)

    # ---- DISSIPATE: 0.9s — eyes dim first, sentinels fly outward ----
    diss = {}
    for i, (sgu, sg, egu, eg) in enumerate(sentinels):
        rad, yh, ph = sentinel_specs[i]
        ang = (ph / 5.0) * 2 * math.pi + math.pi / 2
        diss[egu] = {"name": eg["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.20, x=0.0, y=0.0, z=0.0),
        ]}
        diss[sgu] = {"name": sg["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.90, x=math.cos(ang) * 12, y=2, z=math.sin(ang) * 12),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.90, x=0.0, y=0.0, z=0.0),
            make_keyframe("rotation", 0.0, x=0, y=0, z=0),
            make_keyframe("rotation", 0.90, x=180, y=360, z=180),
        ]}
    for i, (gu, g) in enumerate(orbit_paths):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.70, x=1.8, y=1, z=1.8),
            make_keyframe("scale", 0.90, x=0.0, y=0.0, z=0.0),
        ]}
    diss[conv_gu] = {"name": conv_g["name"], "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, x=1, y=1, z=1),
        make_keyframe("scale", 0.30, x=1.5, y=1.5, z=1.5),
        make_keyframe("scale", 0.90, x=0.0, y=0.0, z=0.0),
    ]}
    b.add_animation("dissipate", length=0.90, animators=diss, loop="once", override=True)

    b.write(OUTPUT)
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    build()
