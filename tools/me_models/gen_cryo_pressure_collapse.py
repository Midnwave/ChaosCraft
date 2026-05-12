#!/usr/bin/env python3
"""Generator for cryo_pressure_collapse.bbmodel — Crush Depth.

3 horizontal disc rings descending from Y=15, radii 5,8,11. Each 12 flat slabs.
10 downward stalactite shards below outer ring (2-segment).
8 pressure wake diamonds above disc.
12 ground impact slabs at Y=0. 5 compression cracks inside.
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

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/freezingice/me_attacks/cryo_pressure_collapse.bbmodel"
RES = 128


def build_crush_depth_texture():
    """CRUSH DEPTH — dark top, bright bottom (compression below)."""
    w = h = RES
    ocean_navy = hex_to_rgba("#02060e")
    slate      = hex_to_rgba("#081828")
    edge       = hex_to_rgba("#80d0ff")
    under      = hex_to_rgba("#c0e8ff")
    spec       = hex_to_rgba("#ffffff")

    pixels = base_fill(w, h, ocean_navy)

    # Top = ocean-navy (upper face dark); bottom = bright
    vertical_gradient_full(pixels, w, h, [
        (0.0, ocean_navy),
        (0.35, slate),
        (0.70, slate),
        (0.85, edge),
        (0.95, under),
        (1.0, spec),
    ])

    # Edge highlight at bottom 4 rows pure emissive
    for y in range(h - 6, h):
        for x in range(w):
            t = (y - (h - 6)) / 6.0
            p = pixels[y * w + x]
            pixels[y * w + x] = color_lerp(p, edge, t * 0.85)

    # Pressure stress streaks (vertical lines)
    import random
    rng = random.Random(45)
    for _ in range(20):
        x = rng.randrange(w)
        y0 = rng.randrange(h - 12)
        for y in range(y0, y0 + 12):
            if 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, edge, 0.2)

    # Specular at bottom edge
    specular_cluster(pixels, w, h, w // 2, h - 4, 8, spec, strength=0.95)

    add_noise_overlay(pixels, w, h, ocean_navy, under, density=0.17, seed=77)
    jitter_inplace(pixels, jitter_range=18, seed=131)
    jitter_inplace(pixels, jitter_range=12, seed=193, density=0.5)

    return png_from_pixels(pixels, w, h)


def build_impact_texture():
    """dark compressed ground with bright radial pressure lines."""
    w = h = RES
    bg     = hex_to_rgba("#060810")
    line   = hex_to_rgba("#5090c0")
    spec   = hex_to_rgba("#c0e0f8")

    pixels = base_fill(w, h, bg)

    cx, cy = w // 2, h // 2
    import random
    rng = random.Random(33)
    # Bright radial pressure lines from centre
    for i in range(36):
        ang = i * (2 * math.pi / 36) + rng.random() * 0.05
        for r in range(4, 56):
            x = int(cx + math.cos(ang) * r)
            y = int(cy + math.sin(ang) * r)
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, line, 0.55)

    # Centre emissive
    specular_cluster(pixels, w, h, cx, cy, 8, spec, strength=0.85)

    add_noise_overlay(pixels, w, h, bg, line, density=0.17, seed=121)
    jitter_inplace(pixels, jitter_range=16, seed=311)
    jitter_inplace(pixels, jitter_range=10, seed=403, density=0.5)

    return png_from_pixels(pixels, w, h)


def build():
    b = Builder("cryo_pressure_collapse", resolution=(RES, RES), visible_box=(16, 18, 0))
    b.add_texture("crush_depth", build_crush_depth_texture())
    b.add_texture("impact", build_impact_texture())

    cd_face = lambda: uniform_face(0, 0, RES, RES, tex_index=0)
    impact_face = lambda: uniform_face(0, 0, RES, RES, tex_index=1)

    # 3 disc rings — radii 5, 8, 11; each 12 flat slabs
    disc_rings = []  # (ring_idx, [(uuid, group, ang)])
    ring_radii = [5.0, 8.0, 11.0]
    for r_idx, r in enumerate(ring_radii):
        slabs = []
        for i in range(12):
            ang = (i / 12.0) * 2 * math.pi
            cx = math.cos(ang) * r
            cz = math.sin(ang) * r
            # Flat horizontal slab — wide, short height (compresses look)
            cube_u = b.add_cube(
                f"disc{r_idx+1}_{i+1}",
                from_=[-1.4, -0.4, -0.7], to_=[1.4, 0.4, 0.7],
                faces=cd_face(),
            )
            gu = make_uuid()
            g = b.make_group(
                f"disc_ring{r_idx+1}_slab{i+1}", [cx, 0, cz], [cube_u],
                rotation=[0, math.degrees(ang), 0], group_uuid=gu,
            )
            slabs.append((gu, g, ang))
        disc_rings.append((r_idx, slabs))

    # 10 downward stalactite shards hanging below outer ring (radius 11)
    stalactite_groups = []
    for i in range(10):
        ang = (i / 10.0) * 2 * math.pi
        r = 11.0
        cx = math.cos(ang) * r
        cz = math.sin(ang) * r
        # 2-segment (wide top, narrow tip) — placed below disc
        top_cu = b.add_cube(
            f"stal{i+1}_top",
            from_=[-0.5, -1.2, -0.5], to_=[0.5, 0, 0.5],
            faces=cd_face(),
        )
        tip_cu = b.add_cube(
            f"stal{i+1}_tip",
            from_=[-0.22, -2.0, -0.22], to_=[0.22, -1.2, 0.22],
            faces=cd_face(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"stalactite_{i+1}", [cx, -0.3, cz], [top_cu, tip_cu],
            rotation=[0, math.degrees(ang), 0], group_uuid=gu,
        )
        stalactite_groups.append((gu, g, ang))

    # 8 wake diamonds above the disc
    wake_groups = []
    for i in range(8):
        ang = (i / 8.0) * 2 * math.pi + 0.15
        r = 9.0
        cx = math.cos(ang) * r
        cz = math.sin(ang) * r
        cube_u = b.add_cube(
            f"wake_{i+1}",
            from_=[-0.45, -0.3, -0.45], to_=[0.45, 0.3, 0.45],
            faces=cd_face(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"wake_diamond_{i+1}", [cx, 2.0, cz], [cube_u],
            rotation=[45, math.degrees(ang) + 45, 45], group_uuid=gu,
        )
        wake_groups.append((gu, g, ang))

    # 12 ground impact slabs at Y=0
    impact_groups = []
    for i in range(12):
        ang = (i / 12.0) * 2 * math.pi
        r = 10.0
        cx = math.cos(ang) * r
        cz = math.sin(ang) * r
        cube_u = b.add_cube(
            f"impact_{i+1}",
            from_=[-1.4, 0.02, -0.6], to_=[1.4, 0.10, 0.6],
            faces=impact_face(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"ground_impact_{i+1}", [cx, 0.05, cz], [cube_u],
            rotation=[0, math.degrees(ang), 0], group_uuid=gu,
        )
        impact_groups.append((gu, g))

    # 5 compression cracks inside impact ring
    crack_groups = []
    for i in range(5):
        ang = (i / 5.0) * 2 * math.pi + 0.1
        r = 4.5
        cx = math.cos(ang) * r
        cz = math.sin(ang) * r
        cube_u = b.add_cube(
            f"compr_{i+1}",
            from_=[-1.4, 0.02, -0.4], to_=[1.4, 0.10, 0.4],
            faces=impact_face(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"compression_crack_{i+1}", [cx, 0.05, cz], [cube_u],
            rotation=[0, math.degrees(ang), 0], group_uuid=gu,
        )
        crack_groups.append((gu, g))

    # Wrap discs+stalactites+wake in a parent disc-bone for descent animation
    disc_bone_children = []
    for (r_idx, slabs) in disc_rings:
        for (gu, g, ang) in slabs:
            disc_bone_children.append(g)
    for (gu, g, ang) in stalactite_groups:
        disc_bone_children.append(g)
    for (gu, g, ang) in wake_groups:
        disc_bone_children.append(g)
    disc_root_uuid = make_uuid()
    disc_root_g = b.make_group("descending_disc", [0, 0, 0], disc_bone_children,
                                group_uuid=disc_root_uuid)

    root_children = [disc_root_g]
    for (gu, g) in impact_groups: root_children.append(g)
    for (gu, g) in crack_groups: root_children.append(g)
    root_uuid = make_uuid()
    b.outliner.append(b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid))

    # SPAWN 1.3s
    spawn = {}
    # Disc descent from Y=15 — linear, heavy
    spawn[disc_root_uuid] = {"name": "descending_disc", "type": "bone", "keyframes": [
        make_keyframe("position", 0.0, 0, 15, 0),
        make_keyframe("position", 0.2, 0, 13, 0, interpolation="linear"),
        make_keyframe("position", 0.5, 0, 9, 0, interpolation="linear"),
        make_keyframe("position", 0.9, 0, 0.5, 0, interpolation="linear"),  # impact
        make_keyframe("position", 1.0, 0, 0.7, 0),  # tiny rebound
        make_keyframe("position", 1.3, 0, 0.5, 0),
    ]}

    # Ground impact ring expands rapidly at 0.9s
    for idx, (gu, g) in enumerate(impact_groups):
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", 0.9, 0, 0, 0),
            make_keyframe("scale", 1.0, 1.3, 1.0, 1.3),
            make_keyframe("scale", 1.1, 1, 1, 1),
            make_keyframe("scale", 1.3, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Compression cracks slam up
    for idx, (gu, g) in enumerate(crack_groups):
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", 0.95, 0, 0, 0),
            make_keyframe("scale", 1.05, 1.2, 1.2, 1.2),
            make_keyframe("scale", 1.15, 1, 1, 1),
            make_keyframe("scale", 1.3, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Stalactites sway from impact momentum
    for idx, (gu, g, ang) in enumerate(stalactite_groups):
        kfs = [
            make_keyframe("rotation", 0.0, 0, math.degrees(ang), 0),
            make_keyframe("rotation", 0.9, 0, math.degrees(ang), 0),
            make_keyframe("rotation", 1.05, 12, math.degrees(ang), 12),
            make_keyframe("rotation", 1.20, -6, math.degrees(ang), -6),
            make_keyframe("rotation", 1.3, 0, math.degrees(ang), 0),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("spawn", length=1.3, animators=spawn, loop="once", override=True)

    # IDLE 6.0s
    idle = {}
    # Disc rotates slowly on Y
    idle[disc_root_uuid] = {"name": "descending_disc", "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, 0, 0, 0),
        make_keyframe("rotation", 1.5, 0, 22, 0),
        make_keyframe("rotation", 3.0, 0, 45, 0),
        make_keyframe("rotation", 4.5, 0, 67, 0),
        make_keyframe("rotation", 6.0, 0, 90, 0),
        # Position drift
        make_keyframe("position", 0.0, 0, 0.5, 0),
        make_keyframe("position", 3.0, 0, 0.7, 0),
        make_keyframe("position", 6.0, 0, 0.5, 0),
    ]}

    # Stalactites sway ±3° phase lag
    for idx, (gu, g, ang) in enumerate(stalactite_groups):
        kfs = []
        for k in range(11):
            t = (k / 10) * 6.0
            ph = (t / 4.0 + idx * 0.3) * 2 * math.pi
            rx = math.sin(ph) * 3
            rz = math.cos(ph * 0.7) * 3
            kfs.append(make_keyframe("rotation", t, rx, math.degrees(ang), rz))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Wake diamonds orbit
    for idx, (gu, g, ang) in enumerate(wake_groups):
        kfs = []
        for k in range(13):
            t = (k / 12) * 6.0
            new_ang = ang + (t / 6.0) * 2 * math.pi * 0.4
            init_x = math.cos(ang) * 9
            init_z = math.sin(ang) * 9
            new_x = math.cos(new_ang) * 9
            new_z = math.sin(new_ang) * 9
            kfs.append(make_keyframe("position", t, new_x - init_x, 0, new_z - init_z))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Ground ring breathes
    for idx, (gu, g) in enumerate(impact_groups):
        kfs = []
        for k in range(7):
            t = (k / 6) * 6.0
            ph = (t / 5.0 + idx * 0.2) * 2 * math.pi
            s = 1.0 + 0.05 * (1 - math.cos(ph))
            kfs.append(make_keyframe("scale", t, s, 1, s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Compression cracks pulse faintly
    for idx, (gu, g) in enumerate(crack_groups):
        kfs = []
        for k in range(7):
            t = (k / 6) * 6.0
            ph = (t / 4.5 + idx * 0.4) * 2 * math.pi
            s = 1.0 + 0.04 * (1 - math.cos(ph))
            kfs.append(make_keyframe("scale", t, s, s, s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("idle", length=6.0, animators=idle, loop="loop", override=False)

    # DISSIPATE 0.7s — disc lifts upward fast (gravity restoring)
    diss = {}
    diss[disc_root_uuid] = {"name": "descending_disc", "type": "bone", "keyframes": [
        make_keyframe("position", 0.0, 0, 0.5, 0),
        make_keyframe("position", 0.3, 0, 5, 0),
        make_keyframe("position", 0.7, 0, 22, 0),
        make_keyframe("scale", 0.0, 1, 1, 1),
        make_keyframe("scale", 0.5, 1, 1, 1),
        make_keyframe("scale", 0.7, 0, 0, 0),
    ]}

    for idx, (gu, g, ang) in enumerate(stalactite_groups):
        kfs = [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.7, 0, -1.5, 0),
            make_keyframe("rotation", 0.0, 0, math.degrees(ang), 0),
            make_keyframe("rotation", 0.7, idx * 8, math.degrees(ang), -idx * 8),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g, ang) in enumerate(wake_groups):
        kfs = [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.7, 0, 4, 0),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.7, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g) in enumerate(impact_groups):
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.4, 1.5, 1, 1.5),
            make_keyframe("scale", 0.7, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g) in enumerate(crack_groups):
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.7, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("dissipate", length=0.7, animators=diss, loop="once", override=True)

    b.write(OUTPUT_PATH)
    import os
    sz = os.path.getsize(OUTPUT_PATH)
    print(f"Wrote {OUTPUT_PATH} ({sz} bytes, {sz/1024:.1f} KB)")


if __name__ == "__main__":
    build()
