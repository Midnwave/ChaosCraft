#!/usr/bin/env python3
"""Generator for blizzard_nova.bbmodel — White Wall.

Sphere approx: 3 rings on 3 axes (horizontal H, vertical NS, vertical EW), each 12 slabs at Y=6.
Outer shockwave: 2 rings at 1.5x radius. 20 blizzard streaks (10 long, 10 short).
14 ground ring slabs. 6 snowflake bones (3 slabs each at 60° offset).
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
    vertical_gradient_full,
    specular_cluster,
    base_fill,
)

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/freezingice/me_attacks/blizzard_nova.bbmodel"
RES = 128
SPHERE_RADIUS = 5.0


def build_compressed_blizzard_texture():
    """COMPRESSED BLIZZARD — silver-white dominant, fine horizontal grain."""
    w = h = RES
    silver  = hex_to_rgba("#d8e8f8")
    silver_mid = hex_to_rgba("#90a8c8")
    slate   = hex_to_rgba("#304060")
    void    = hex_to_rgba("#081428")
    pure    = hex_to_rgba("#ffffff")

    pixels = base_fill(w, h, silver)

    # Top half mostly pure white, bottom darker
    vertical_gradient_full(pixels, w, h, [
        (0.0, pure),
        (0.2, silver),
        (0.55, silver_mid),
        (0.85, slate),
        (1.0, void),
    ])

    # Fine horizontal grain (wind direction)
    import random
    rng = random.Random(11)
    for y in range(h):
        if y % 2 == 0:
            for x in range(w):
                p = pixels[y * w + x]
                # Slight brightness variation per row
                v = rng.randint(-12, 12)
                r2 = max(0, min(255, p[0] + v))
                g2 = max(0, min(255, p[1] + v))
                bl2 = max(0, min(255, p[2] + v))
                pixels[y * w + x] = (r2, g2, bl2, p[3])

    # Top rows pure white
    for y in range(8):
        for x in range(w):
            pixels[y * w + x] = pure

    # Specular emissive
    specular_cluster(pixels, w, h, w // 2, 4, 8, pure, strength=1.0)

    add_noise_overlay(pixels, w, h, silver_mid, pure, density=0.18, seed=66)
    jitter_inplace(pixels, jitter_range=14, seed=121)
    jitter_inplace(pixels, jitter_range=10, seed=171, density=0.45)

    return png_from_pixels(pixels, w, h)


def build_ground_radial_texture():
    """deep navy with silver-white radial burst."""
    w = h = RES
    centre = hex_to_rgba("#c0d8f0")
    deep   = hex_to_rgba("#04101e")
    spec   = hex_to_rgba("#ffffff")

    pixels = base_fill(w, h, deep)

    cx, cy = w // 2, h // 2
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            t = min(1.0, d / (w * 0.55))
            c = color_lerp(centre, deep, t)
            pixels[y * w + x] = c

    # Radial burst streaks
    import random
    rng = random.Random(31)
    for s in range(36):
        ang = s * (2 * math.pi / 36) + rng.random() * 0.05
        for r in range(8, 50):
            x = int(cx + math.cos(ang) * r)
            y = int(cy + math.sin(ang) * r)
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, spec, 0.45)

    add_noise_overlay(pixels, w, h, deep, centre, density=0.17, seed=99)
    jitter_inplace(pixels, jitter_range=16, seed=271)
    jitter_inplace(pixels, jitter_range=10, seed=303, density=0.5)

    return png_from_pixels(pixels, w, h)


def build():
    b = Builder("blizzard_nova", resolution=(RES, RES), visible_box=(16, 14, 0))
    b.add_texture("blizzard", build_compressed_blizzard_texture())
    b.add_texture("ground", build_ground_radial_texture())

    bliz_face = lambda: uniform_face(0, 0, RES, RES, tex_index=0)
    ground_face = lambda: uniform_face(0, 0, RES, RES, tex_index=1)

    SPHERE_Y = 6.0

    # 3 sphere rings on 3 axes — each 12 upright flat slabs
    ring_groups = []
    for axis_idx, axis in enumerate(["H", "NS", "EW"]):
        for i in range(12):
            ang = (i / 12.0) * 2 * math.pi
            r = SPHERE_RADIUS
            if axis == "H":
                # Horizontal ring at Y=SPHERE_Y
                cx = math.cos(ang) * r
                cy = SPHERE_Y
                cz = math.sin(ang) * r
                rot = [0, math.degrees(ang), 0]
            elif axis == "NS":
                # Vertical ring on XY plane (Z=0)
                cx = math.cos(ang) * r
                cy = SPHERE_Y + math.sin(ang) * r
                cz = 0
                rot = [0, 0, math.degrees(ang)]
            else:  # EW
                cx = 0
                cy = SPHERE_Y + math.sin(ang) * r
                cz = math.cos(ang) * r
                rot = [math.degrees(ang), 90, 0]

            cube_u = b.add_cube(
                f"ring_{axis}_{i+1}",
                from_=[-0.3, -0.9, -0.3], to_=[0.3, 0.9, 0.3],
                faces=bliz_face(),
            )
            gu = make_uuid()
            g = b.make_group(
                f"sphere_{axis}_{i+1}", [cx, cy, cz], [cube_u],
                rotation=rot, group_uuid=gu,
            )
            ring_groups.append((gu, g, axis, i))

    # Outer shockwave sphere: 2 rings at 1.5x radius (horiz + vertical)
    outer_groups = []
    for axis in ["H", "V"]:
        for i in range(14):
            ang = (i / 14.0) * 2 * math.pi
            r = SPHERE_RADIUS * 1.5
            if axis == "H":
                cx = math.cos(ang) * r
                cy = SPHERE_Y
                cz = math.sin(ang) * r
                rot = [0, math.degrees(ang), 0]
            else:
                cx = math.cos(ang) * r
                cy = SPHERE_Y + math.sin(ang) * r
                cz = 0
                rot = [0, 0, math.degrees(ang)]
            cube_u = b.add_cube(
                f"shockwave_{axis}_{i+1}",
                from_=[-0.25, -0.7, -0.25], to_=[0.25, 0.7, 0.25],
                faces=ground_face(),
            )
            gu = make_uuid()
            g = b.make_group(
                f"shock_{axis}_{i+1}", [cx, cy, cz], [cube_u],
                rotation=rot, group_uuid=gu,
            )
            outer_groups.append((gu, g, axis))

    # 20 blizzard streaks — 10 long, 10 short, alternating
    streak_groups = []
    for i in range(20):
        # spherical coordinates
        phi = (i / 20.0) * 2 * math.pi
        theta = ((i * 7) % 13) / 13.0 * math.pi  # vary altitude
        is_long = i % 2 == 0
        length = 4.0 if is_long else 2.2
        # Direction unit vector
        dx = math.cos(phi) * math.sin(theta)
        dy = math.cos(theta)
        dz = math.sin(phi) * math.sin(theta)
        # Place outward from sphere centre
        cx = dx * (SPHERE_RADIUS + length / 2)
        cy = SPHERE_Y + dy * (SPHERE_RADIUS + length / 2)
        cz = dz * (SPHERE_RADIUS + length / 2)
        cube_u = b.add_cube(
            f"streak_{i+1}",
            from_=[-length / 2, -0.15, -0.15], to_=[length / 2, 0.15, 0.15],
            faces=bliz_face(),
        )
        # Rotation aligning to direction
        yaw = math.degrees(math.atan2(dz, dx))
        pitch = math.degrees(math.asin(dy))
        gu = make_uuid()
        g = b.make_group(
            f"blizzard_streak_{i+1}", [cx, cy, cz], [cube_u],
            rotation=[0, yaw, pitch], group_uuid=gu,
        )
        streak_groups.append((gu, g, length, dx, dy, dz))

    # 14 ground ring slabs
    ground_ring = []
    for i in range(14):
        ang = (i / 14.0) * 2 * math.pi
        r = 9.0
        cx = math.cos(ang) * r
        cz = math.sin(ang) * r
        cube_u = b.add_cube(
            f"gring_{i+1}",
            from_=[-1.2, 0.02, -0.5], to_=[1.2, 0.10, 0.5],
            faces=ground_face(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"ground_ring_{i+1}", [cx, 0.05, cz], [cube_u],
            rotation=[0, math.degrees(ang), 0], group_uuid=gu,
        )
        ground_ring.append((gu, g))

    # 6 snowflake accents orbiting the sphere
    snowflake_groups = []
    for i in range(6):
        ang = (i / 6.0) * 2 * math.pi
        orbit_r = SPHERE_RADIUS + 1.5
        sx = math.cos(ang) * orbit_r
        sy = SPHERE_Y + ((i % 3) - 1) * 1.5
        sz = math.sin(ang) * orbit_r
        # Each snowflake = 3 flat slabs at 60° offsets
        cubes = []
        for k in range(3):
            rot_y = k * 60
            c = b.add_cube(
                f"flake_{i+1}_arm{k+1}",
                from_=[-0.7, -0.05, -0.1], to_=[0.7, 0.05, 0.1],
                faces=ground_face(),
            )
            cubes.append(c)
        # We need each arm individually with rotation — use nested groups
        # For simplicity: bake as one group with 3 cubes (rotations baked into geometry isn't supported per-cube,
        # so we'll create individual sub-bones)
        arm_groups = []
        for k in range(3):
            rot_y = k * 60
            arm_cu = b.add_cube(
                f"flake{i+1}_a{k+1}",
                from_=[-0.7, -0.05, -0.1], to_=[0.7, 0.05, 0.1],
                faces=ground_face(),
            )
            agu = make_uuid()
            ag = b.make_group(
                f"flake{i+1}_arm{k+1}", [0, 0, 0], [arm_cu],
                rotation=[0, rot_y, 0], group_uuid=agu,
            )
            arm_groups.append(ag)
        # Use only one group total per snowflake (containing the arm groups)
        sgu = make_uuid()
        sg = b.make_group(
            f"snowflake_{i+1}", [sx, sy, sz], arm_groups, group_uuid=sgu,
        )
        snowflake_groups.append((sgu, sg, ang, orbit_r))

    root_children = []
    for (gu, g, *_r) in ring_groups: root_children.append(g)
    for (gu, g, *_r) in outer_groups: root_children.append(g)
    for (gu, g, *_r) in streak_groups: root_children.append(g)
    for (gu, g) in ground_ring: root_children.append(g)
    for (gu, g, *_r) in snowflake_groups: root_children.append(g)
    root_uuid = make_uuid()
    b.outliner.append(b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid))

    # SPAWN 0.7s — compressed windup then BURST
    spawn = {}
    # Sphere rings: 0.4 -> 0.2 (windup) -> 1.3 (burst at 0.3s)
    for idx, (gu, g, axis, i) in enumerate(ring_groups):
        kfs = [
            make_keyframe("scale", 0.0, 0.4, 0.4, 0.4),
            make_keyframe("scale", 0.20, 0.2, 0.2, 0.2),
            make_keyframe("scale", 0.30, 1.3, 1.3, 1.3, interpolation="linear"),
            make_keyframe("scale", 0.55, 1.1, 1.1, 1.1),
            make_keyframe("scale", 0.70, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Shockwave at 0.3s
    for idx, (gu, g, axis) in enumerate(outer_groups):
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", 0.30, 0, 0, 0),
            make_keyframe("scale", 0.50, 1.2, 1.2, 1.2),
            make_keyframe("scale", 0.70, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Streaks blast outward at 0.3s
    for idx, (gu, g, length, dx, dy, dz) in enumerate(streak_groups):
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", 0.30, 0, 0, 0),
            make_keyframe("scale", 0.45, 1.4, 1.4, 1.4),
            make_keyframe("scale", 0.65, 1, 1, 1),
            make_keyframe("position", 0.0, -dx * 3, -dy * 3, -dz * 3),
            make_keyframe("position", 0.30, -dx * 3, -dy * 3, -dz * 3),
            make_keyframe("position", 0.55, dx * 0.5, dy * 0.5, dz * 0.5),
            make_keyframe("position", 0.70, 0, 0, 0),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Ground ring at 0.3s
    for idx, (gu, g) in enumerate(ground_ring):
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", 0.30, 0, 0, 0),
            make_keyframe("scale", 0.50, 1.2, 1.2, 1.2),
            make_keyframe("scale", 0.70, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Snowflakes materialise at 0.4s
    for idx, (gu, g, ang, orbit_r) in enumerate(snowflake_groups):
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", 0.40, 0, 0, 0),
            make_keyframe("scale", 0.55, 1.2, 1.2, 1.2),
            make_keyframe("scale", 0.70, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("spawn", length=0.7, animators=spawn, loop="once", override=True)

    # IDLE 4.0s — rings rotate on their axes at different rates
    idle = {}
    for idx, (gu, g, axis, i) in enumerate(ring_groups):
        kfs = []
        for k in range(7):
            t = (k / 6) * 4.0
            # Tiny per-slab orbit — animate around centre via position
            ph = (i / 12.0) * 2 * math.pi
            if axis == "H":
                # H ring rotates around Y axis (slow)
                base_ang = ph + (t / 4.0) * 2 * math.pi * 0.5
                cx = math.cos(base_ang) * SPHERE_RADIUS
                cz = math.sin(base_ang) * SPHERE_RADIUS
                # We need delta from initial position
                init_cx = math.cos(ph) * SPHERE_RADIUS
                init_cz = math.sin(ph) * SPHERE_RADIUS
                kfs.append(make_keyframe("position", t, cx - init_cx, 0, cz - init_cz))
            else:
                # Just rotate cube
                kfs.append(make_keyframe("rotation", t, t * 30, idx * 10, t * 20))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Streaks pulse scale in outward wave
    for idx, (gu, g, length, dx, dy, dz) in enumerate(streak_groups):
        kfs = []
        for k in range(7):
            t = (k / 6) * 4.0
            ph = (t / 2.0 + idx * 0.2) * 2 * math.pi
            s = 1.0 + 0.05 * (1 - math.cos(ph))
            kfs.append(make_keyframe("scale", t, s, s, s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Snowflakes orbit
    for idx, (gu, g, ang, orbit_r) in enumerate(snowflake_groups):
        kfs = []
        for k in range(11):
            t = (k / 10) * 4.0
            new_ang = ang + (t / 4.0) * 2 * math.pi * 0.5
            init_cx = math.cos(ang) * orbit_r
            init_cz = math.sin(ang) * orbit_r
            new_cx = math.cos(new_ang) * orbit_r
            new_cz = math.sin(new_ang) * orbit_r
            kfs.append(make_keyframe("position", t, new_cx - init_cx, 0, new_cz - init_cz))
        # Rotation spin
        for k in range(7):
            t = (k / 6) * 4.0
            kfs.append(make_keyframe("rotation", t, 0, t * 80 + idx * 30, 0))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Shockwave counter-rotates
    for idx, (gu, g, axis) in enumerate(outer_groups):
        kfs = []
        for k in range(5):
            t = (k / 4) * 4.0
            kfs.append(make_keyframe("rotation", t, 0, -t * 25, 0))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Ground ring breathes
    for idx, (gu, g) in enumerate(ground_ring):
        kfs = []
        for k in range(7):
            t = (k / 6) * 4.0
            ph = (t / 3.5 + idx * 0.2) * 2 * math.pi
            s = 1.0 + 0.05 * (1 - math.cos(ph))
            kfs.append(make_keyframe("scale", t, s, 1, s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("idle", length=4.0, animators=idle, loop="loop", override=False)

    # DISSIPATE 0.5s — streaks continue, rings expand and snap
    diss = {}
    for idx, (gu, g, length, dx, dy, dz) in enumerate(streak_groups):
        kfs = [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.5, dx * 4, dy * 4, dz * 4),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.5, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g, axis, i) in enumerate(ring_groups):
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.3, 1.6, 1.6, 1.6),
            make_keyframe("scale", 0.5, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g, axis) in enumerate(outer_groups):
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.5, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g, ang, orbit_r) in enumerate(snowflake_groups):
        kfs = [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.5, math.cos(ang + math.pi / 2) * 4, 0, math.sin(ang + math.pi / 2) * 4),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.5, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g) in enumerate(ground_ring):
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.3, 1.4, 1, 1.4),
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
