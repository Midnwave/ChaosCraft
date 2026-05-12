#!/usr/bin/env python3
"""Generator for ice_age_terminus.bbmodel — The Advance.

6 wall slabs in row along X axis, heights 6,9,7,11,8,10 — jagged silhouette.
8 wall face detail slabs embedded at various heights.
10 toe debris slabs at Y=0 in front of wall.
3 wall base wide slabs connecting wall bottom to ground.
6 medium calved chunks (irregular cube clusters) floating nearby.
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
    crack_lines,
    specular_cluster,
    base_fill,
)

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/freezingice/me_attacks/ice_age_terminus.bbmodel"
RES = 128


def build_glacial_wall_texture():
    """GLACIAL WALL FACE — deep VERTICAL crack lines at irregular columns."""
    w = h = RES
    deep    = hex_to_rgba("#04101e")
    compr   = hex_to_rgba("#0c3060")
    interior = hex_to_rgba("#2870b0")
    surface = hex_to_rgba("#70b8e0")
    frost   = hex_to_rgba("#c8eaff")
    emiss   = hex_to_rgba("#90d0ff")
    spec    = hex_to_rgba("#e8f8ff")

    pixels = base_fill(w, h, deep)

    # Base smooth gradient
    for y in range(h):
        t = y / (h - 1)
        if t < 0.3:
            c = color_lerp(frost, surface, t / 0.3)
        elif t < 0.6:
            c = color_lerp(surface, interior, (t - 0.3) / 0.3)
        elif t < 0.85:
            c = color_lerp(interior, compr, (t - 0.6) / 0.25)
        else:
            c = color_lerp(compr, deep, (t - 0.85) / 0.15)
        for x in range(w):
            pixels[y * w + x] = c

    # Deep vertical crack lines at irregular positions (RES=128 scale)
    crack_cols = [12, 28, 44, 67, 84, 102, 118]
    crack_lines(pixels, w, h, crack_cols, deep, seed=15)
    # Emissive cracks interior
    for col in crack_cols:
        for y in range(h):
            for dx in range(-1, 2):
                x = col + dx
                if 0 <= x < w:
                    p = pixels[y * w + x]
                    pixels[y * w + x] = color_lerp(p, emiss, 0.5)

    # Specular at crack intersections (multiple Y positions)
    import random
    rng = random.Random(91)
    for col in crack_cols:
        for _ in range(3):
            cy = rng.randrange(8, h - 8)
            specular_cluster(pixels, w, h, col, cy, 3, spec, strength=0.85)

    add_noise_overlay(pixels, w, h, deep, surface, density=0.16, seed=141)
    jitter_inplace(pixels, jitter_range=18, seed=211)
    jitter_inplace(pixels, jitter_range=12, seed=311, density=0.45)

    return png_from_pixels(pixels, w, h)


def build_crushed_tundra_texture():
    """crushed tundra dark + pale frost overlay."""
    w = h = RES
    dark   = hex_to_rgba("#060a08")
    frost  = hex_to_rgba("#90c8d0")
    bright = hex_to_rgba("#c0e0e8")

    pixels = base_fill(w, h, dark)

    # Pale frost patches scattered
    import random
    rng = random.Random(101)
    for _ in range(80):
        cx = rng.randrange(w)
        cy = rng.randrange(h)
        r = rng.randrange(2, 5)
        for dy in range(-r, r + 1):
            for dx in range(-r, r + 1):
                x, y = cx + dx, cy + dy
                if 0 <= x < w and 0 <= y < h:
                    d = math.sqrt(dx * dx + dy * dy)
                    if d <= r:
                        t = max(0.0, 1.0 - d / r)
                        p = pixels[y * w + x]
                        pixels[y * w + x] = color_lerp(p, frost, t * 0.6)

    # Tracks/scuffs in random directions (crushed ground)
    for _ in range(30):
        x0 = rng.randrange(w)
        y0 = rng.randrange(h)
        length = rng.randrange(6, 20)
        ang = rng.random() * 2 * math.pi
        for r in range(length):
            x = int(x0 + math.cos(ang) * r)
            y = int(y0 + math.sin(ang) * r)
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, bright, 0.35)

    add_noise_overlay(pixels, w, h, dark, frost, density=0.18, seed=33)
    jitter_inplace(pixels, jitter_range=18, seed=271)
    jitter_inplace(pixels, jitter_range=10, seed=331, density=0.5)

    return png_from_pixels(pixels, w, h)


def build():
    b = Builder("ice_age_terminus", resolution=(RES, RES), visible_box=(16, 14, 0))
    b.add_texture("wall", build_glacial_wall_texture())
    b.add_texture("ground", build_crushed_tundra_texture())

    wall_face = lambda: uniform_face(0, 0, RES, RES, tex_index=0)
    ground_face = lambda: uniform_face(0, 0, RES, RES, tex_index=1)

    # 6 wall sections along X, heights 6,9,7,11,8,10
    heights = [6, 9, 7, 11, 8, 10]
    wall_groups = []  # (uuid, group, h, gx)
    section_width = 2.4
    for i, hgt in enumerate(heights):
        gx = (i - 2.5) * section_width
        cube_u = b.add_cube(
            f"wall_{i+1}",
            from_=[-section_width / 2 + 0.05, 0, -1.0],
            to_=[section_width / 2 - 0.05, hgt, 1.0],
            faces=wall_face(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"wall_section_{i+1}", [gx, 0, 0], [cube_u], group_uuid=gu,
        )
        wall_groups.append((gu, g, hgt, gx))

    # 8 face details embedded in wall at various heights/forward offsets
    face_detail_groups = []
    import random
    rng = random.Random(7)
    for i in range(8):
        # Pick random wall section
        wi = i % 6
        gx = (wi - 2.5) * section_width + rng.uniform(-0.7, 0.7)
        gy = rng.uniform(1, heights[wi] - 1)
        cube_u = b.add_cube(
            f"face_det_{i+1}",
            from_=[-0.6, 0, -0.3], to_=[0.6, 0.8, 0.3],
            faces=wall_face(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"wall_face_detail_{i+1}", [gx, gy, 1.0 + rng.uniform(0, 0.4)], [cube_u],
            rotation=[rng.uniform(-8, 8), rng.uniform(-15, 15), rng.uniform(-5, 5)],
            group_uuid=gu,
        )
        face_detail_groups.append((gu, g))

    # 10 toe debris slabs in front of wall (positive Z direction)
    toe_groups = []
    for i in range(10):
        tx = rng.uniform(-7, 7)
        tz = rng.uniform(1.5, 5)
        cube_u = b.add_cube(
            f"toe_{i+1}",
            from_=[-0.7, 0.05, -0.5], to_=[0.7, 0.4, 0.5],
            faces=ground_face(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"toe_debris_{i+1}", [tx, 0.2, tz], [cube_u],
            rotation=[rng.uniform(-12, 12), rng.uniform(0, 360), rng.uniform(-12, 12)],
            group_uuid=gu,
        )
        toe_groups.append((gu, g, tx, tz))

    # 3 wide wall base slabs at Y=0 connecting wall to ground
    base_groups = []
    for i in range(3):
        gx = (i - 1) * 4.5
        cube_u = b.add_cube(
            f"wallbase_{i+1}",
            from_=[-2.5, 0, -1.3], to_=[2.5, 0.3, 1.3],
            faces=ground_face(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"wall_base_{i+1}", [gx, 0.15, -0.2], [cube_u], group_uuid=gu,
        )
        base_groups.append((gu, g))

    # 6 calved chunks — medium irregular cube clusters
    calved_groups = []
    for i in range(6):
        cx = rng.uniform(-7, 7)
        cy = rng.uniform(2, 8)
        cz = rng.uniform(2, 6)
        # cluster of 3 small cubes
        chunk_cubes = []
        for k in range(3):
            offset_x = rng.uniform(-0.5, 0.5)
            offset_y = rng.uniform(-0.5, 0.5)
            offset_z = rng.uniform(-0.5, 0.5)
            sz = rng.uniform(0.3, 0.6)
            chunk_cubes.append(b.add_cube(
                f"calved_{i+1}_{k+1}",
                from_=[offset_x - sz, offset_y - sz, offset_z - sz],
                to_=[offset_x + sz, offset_y + sz, offset_z + sz],
                faces=wall_face(),
            ))
        gu = make_uuid()
        g = b.make_group(
            f"calved_chunk_{i+1}", [cx, cy, cz], chunk_cubes,
            rotation=[rng.uniform(0, 360), rng.uniform(0, 360), rng.uniform(0, 360)],
            group_uuid=gu,
        )
        calved_groups.append((gu, g, cx, cy, cz))

    root_children = []
    for (gu, g, *_r) in wall_groups: root_children.append(g)
    for (gu, g) in face_detail_groups: root_children.append(g)
    for (gu, g, *_r) in toe_groups: root_children.append(g)
    for (gu, g) in base_groups: root_children.append(g)
    for (gu, g, *_r) in calved_groups: root_children.append(g)
    root_uuid = make_uuid()
    b.outliner.append(b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid))

    # SPAWN 1.4s
    spawn = {}
    # Wall base slides in from behind at 0.2s
    for idx, (gu, g) in enumerate(base_groups):
        start = 0.2 + idx * 0.03
        kfs = [
            make_keyframe("position", 0.0, 0, 0.15, -5),
            make_keyframe("position", start, 0, 0.15, -5),
            make_keyframe("position", start + 0.20, 0, 0.15, -0.2),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 1.4, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Wall sections rise at 0.4s — shorter sections arrive first
    sorted_walls = sorted(enumerate(wall_groups), key=lambda x: x[1][2])  # sort by height
    for sort_idx, (orig_idx, (gu, g, hgt, gx)) in enumerate(sorted_walls):
        # All start rising at 0.4s but at different speeds
        # Shorter sections finish faster
        rise_time = 0.4 + (hgt / 11) * 0.4
        kfs = [
            make_keyframe("scale", 0.0, 1, 0, 1),
            make_keyframe("scale", 0.40, 1, 0, 1, interpolation="linear"),
            make_keyframe("scale", rise_time, 1, 1, 1),
            make_keyframe("scale", 1.4, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Wall face details phase in as section rises
    for idx, (gu, g) in enumerate(face_detail_groups):
        start = 0.55 + idx * 0.04
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", start, 0, 0, 0),
            make_keyframe("scale", start + 0.12, 1.1, 1.1, 1.1),
            make_keyframe("scale", start + 0.22, 1, 1, 1),
            make_keyframe("scale", 1.4, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Toe debris at 0.3s
    for idx, (gu, g, tx, tz) in enumerate(toe_groups):
        start = 0.3 + idx * 0.04
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", start, 0, 0, 0),
            make_keyframe("scale", start + 0.15, 1.2, 1, 1.2),
            make_keyframe("scale", start + 0.25, 1, 1, 1),
            make_keyframe("scale", 1.4, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Calved chunks fly off wall at 0.8s
    for idx, (gu, g, cx, cy, cz) in enumerate(calved_groups):
        start = 0.8 + idx * 0.06
        kfs = [
            make_keyframe("position", 0.0, 0, -cy + 0.5, -cz + 0.2),
            make_keyframe("position", start, 0, -cy + 0.5, -cz + 0.2),
            make_keyframe("position", start + 0.20, 0.2, 0.3, 0.3),
            make_keyframe("position", 1.4, 0, 0, 0),
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", start, 0, 0, 0),
            make_keyframe("scale", start + 0.15, 1.1, 1.1, 1.1),
            make_keyframe("scale", min(1.4, start + 0.30), 1, 1, 1),
            make_keyframe("scale", 1.4, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("spawn", length=1.4, animators=spawn, loop="once", override=True)

    # IDLE 8.0s — wall slowly advances forward
    idle = {}
    # ROOT bone drifts +Z 1 unit over 8s — but we have no root animator yet; create per-section drift
    # Apply position drift to each wall section
    for idx, (gu, g, hgt, gx) in enumerate(wall_groups):
        kfs = []
        for k in range(9):
            t = (k / 8) * 8.0
            z_drift = (t / 8.0) * 1.0
            ph = (t / 6 + idx * 0.4) * 2 * math.pi
            sway = math.sin(ph) * 0.8
            kfs.append(make_keyframe("position", t, 0, 0, z_drift))
            kfs.append(make_keyframe("rotation", t, 0, 0, sway))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Face details subtly sway
    for idx, (gu, g) in enumerate(face_detail_groups):
        kfs = []
        for k in range(7):
            t = (k / 6) * 8.0
            ph = (t / 5 + idx * 0.3) * 2 * math.pi
            rx = math.sin(ph) * 1.0
            rz = math.cos(ph * 0.8) * 1.0
            kfs.append(make_keyframe("rotation", t, rx, 0, rz))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Calved chunks drift slowly outward
    for idx, (gu, g, cx, cy, cz) in enumerate(calved_groups):
        kfs = []
        for k in range(9):
            t = (k / 8) * 8.0
            drift_x = (t / 8.0) * 0.6 * (1 if cx > 0 else -1)
            drift_y = math.sin(t * 0.3 + idx) * 0.15
            kfs.append(make_keyframe("position", t, drift_x, drift_y, 0))
            kfs.append(make_keyframe("rotation", t, t * 8 + idx * 30, t * 12 + idx * 40, t * 10 + idx * 50))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Toe debris slowly pushed ahead
    for idx, (gu, g, tx, tz) in enumerate(toe_groups):
        kfs = []
        for k in range(7):
            t = (k / 6) * 8.0
            push = (t / 8.0) * 0.5
            kfs.append(make_keyframe("position", t, 0, 0, push))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("idle", length=8.0, animators=idle, loop="loop", override=False)

    # DISSIPATE 1.0s — wall crumbles forward
    diss = {}
    for idx, (gu, g, hgt, gx) in enumerate(wall_groups):
        kfs = [
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 0.5, 30, 0, 0),
            make_keyframe("rotation", 0.8, 60, 0, 0),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.5, 1, 1, 1),
            make_keyframe("scale", 1.0, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g) in enumerate(face_detail_groups):
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 1.0, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g, cx, cy, cz) in enumerate(calved_groups):
        kfs = [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 1.0, cx * 0.3, -cy * 0.5, cz * 0.6),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 1.0, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g, tx, tz) in enumerate(toe_groups):
        kfs = [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 1.0, tx * 0.3, 0, tz * 0.5),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 1.0, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g) in enumerate(base_groups):
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
