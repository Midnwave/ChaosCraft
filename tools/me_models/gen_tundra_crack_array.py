#!/usr/bin/env python3
"""Generator for tundra_crack_array.bbmodel — Fault Lines.

5 parallel crack lines on Z, spaced 3 units on X. Each: 4 floor slabs.
Between slabs: 3 upright walls (15 total). Far end of each crack: 2-segment cursed spike (5 total).
8 ground frost slabs. 4-cube origin cluster at convergence.
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

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/freezingice/me_attacks/tundra_crack_array.bbmodel"
RES = 128


def build_cursed_ice_texture():
    """CURSED ICE VEIN — vertical teal veins, dark teal-black base."""
    w = h = RES
    dark    = hex_to_rgba("#04100e")
    teal    = hex_to_rgba("#0a3028")
    bright  = hex_to_rgba("#10a870")
    emiss   = hex_to_rgba("#20d890")
    spec    = hex_to_rgba("#a0ffe0")

    pixels = base_fill(w, h, dark)

    # Slight vertical gradient — darker top, slightly more saturated mid
    for y in range(h):
        t = y / (h - 1)
        if t < 0.5:
            c = color_lerp(dark, teal, t / 0.5)
        else:
            c = color_lerp(teal, dark, (t - 0.5) / 0.5)
        for x in range(w):
            pixels[y * w + x] = c

    # Hard-edge vein lines at columns 10, 28, 46 (scale to RES=128: 20, 56, 92)
    veins = [int(10 * RES / 64), int(28 * RES / 64), int(46 * RES / 64)]
    for col in veins:
        for y in range(h):
            for dx in range(-2, 3):
                x = col + dx
                if 0 <= x < w:
                    intensity = 1.0 - abs(dx) * 0.25
                    p = pixels[y * w + x]
                    pixels[y * w + x] = color_lerp(p, bright, intensity * 0.85)
        # Inner emissive core
        for y in range(h):
            for dx in range(-1, 2):
                x = col + dx
                if 0 <= x < w:
                    p = pixels[y * w + x]
                    pixels[y * w + x] = color_lerp(p, emiss, 0.5)

    # Specular flickers along veins
    import random
    rng = random.Random(99)
    for _ in range(24):
        col = veins[rng.randrange(3)]
        cy = rng.randrange(h)
        specular_cluster(pixels, w, h, col, cy, 2, spec, strength=0.85)

    add_noise_overlay(pixels, w, h, dark, bright, density=0.16, seed=14)
    jitter_inplace(pixels, jitter_range=18, seed=421)
    jitter_inplace(pixels, jitter_range=12, seed=509, density=0.45)

    return png_from_pixels(pixels, w, h)


def build_tundra_floor_texture():
    """near-black tundra with sickly green-teal cracks branching from centre."""
    w = h = RES
    bg    = hex_to_rgba("#040808")
    crack = hex_to_rgba("#18a060")
    emiss = hex_to_rgba("#30d090")

    pixels = base_fill(w, h, bg)

    import random
    rng = random.Random(77)
    # Branching teal cracks from centre
    for branch in range(8):
        ang = branch * (2 * math.pi / 8) + rng.random() * 0.2
        length = rng.randrange(28, 50)
        x0, y0 = w // 2, h // 2
        for r in range(length):
            x = int(x0 + math.cos(ang) * r)
            y = int(y0 + math.sin(ang) * r)
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, crack, 0.85)
                if x + 1 < w:
                    p2 = pixels[y * w + x + 1]
                    pixels[y * w + x + 1] = color_lerp(p2, crack, 0.4)
            if r % 5 == 0 and r > 5:
                for sb_dir in (-1, 1):
                    sang = ang + sb_dir * (math.pi / 4 + rng.random() * 0.2)
                    for s in range(rng.randrange(3, 9)):
                        sx = int(x0 + math.cos(ang) * r + math.cos(sang) * s)
                        sy = int(y0 + math.sin(ang) * r + math.sin(sang) * s)
                        if 0 <= sx < w and 0 <= sy < h:
                            p = pixels[sy * w + sx]
                            pixels[sy * w + sx] = color_lerp(p, crack, 0.6)

    specular_cluster(pixels, w, h, w // 2, h // 2, 6, emiss, strength=0.9)

    add_noise_overlay(pixels, w, h, bg, crack, density=0.18, seed=63)
    jitter_inplace(pixels, jitter_range=18, seed=141)
    jitter_inplace(pixels, jitter_range=12, seed=199, density=0.5)

    return png_from_pixels(pixels, w, h)


def build():
    b = Builder("tundra_crack_array", resolution=(RES, RES), visible_box=(14, 6, 0))
    b.add_texture("cursed_ice", build_cursed_ice_texture())
    b.add_texture("tundra", build_tundra_floor_texture())

    cursed_face = lambda: uniform_face(0, 0, RES, RES, tex_index=0)
    tundra_face = lambda: uniform_face(0, 0, RES, RES, tex_index=1)

    # 5 crack lines along Z axis, spaced 3 on X
    crack_parents = []  # (parent_uuid, parent_group, line_idx)
    all_slabs = []  # (slab_uuid, slab_group, line_idx, slab_idx)
    all_walls = []
    cursed_spikes = []

    for line in range(5):
        x = (line - 2) * 3.0
        # 4 floor slabs along Z, with crack origin at z=0 extending forward (positive Z)
        line_slabs = []
        for s in range(4):
            z_pos = -6 + s * 4 + 2  # spans from -6 to 8
            # slight offset and rotation to fake rough crack
            offset = (s % 2) * 0.3 - 0.15
            cube_u = b.add_cube(
                f"crack{line+1}_slab{s+1}",
                from_=[-1.0, -0.05, -1.6], to_=[1.0, 0.10, 1.6],
                faces=tundra_face(),
            )
            sgu = make_uuid()
            sg = b.make_group(
                f"line{line+1}_slab{s+1}", [x + offset, 0.05, z_pos], [cube_u],
                rotation=[0, (s * 7) - 14, 0], group_uuid=sgu,
            )
            line_slabs.append(sg)
            all_slabs.append((sgu, sg, line, s))

            # 3 upright walls between slabs (only between consecutive slabs)
            if s < 3:
                for w_idx in range(3):
                    wz = z_pos + 1 + w_idx
                    wall_cu = b.add_cube(
                        f"line{line+1}_wall{s+1}_{w_idx+1}",
                        from_=[-0.3, 0, -0.7], to_=[0.3, 1.6, 0.7],
                        faces=cursed_face(),
                    )
                    wgu = make_uuid()
                    wg = b.make_group(
                        f"line{line+1}_wall{s+1}_{w_idx+1}", [x + offset, 0.8, wz], [wall_cu],
                        rotation=[0, (w_idx * 8) - 8, 0], group_uuid=wgu,
                    )
                    all_walls.append((wgu, wg, line, s, w_idx))

        # 2-segment cursed spike at far end of crack (line_slabs[-1] z_pos = 8)
        spike_cu1 = b.add_cube(
            f"line{line+1}_spike_base",
            from_=[-0.5, 0, -0.5], to_=[0.5, 1.5, 0.5],
            faces=cursed_face(),
        )
        spike_cu2 = b.add_cube(
            f"line{line+1}_spike_tip",
            from_=[-0.25, 1.5, -0.25], to_=[0.25, 3.0, 0.25],
            faces=cursed_face(),
        )
        psgu = make_uuid()
        psg = b.make_group(
            f"line{line+1}_spike", [x, 0, 8], [spike_cu1, spike_cu2],
            group_uuid=psgu,
        )
        cursed_spikes.append((psgu, psg))

        # Parent group for the whole crack line
        children_for_line = list(line_slabs)
        pgu = make_uuid()
        pg = b.make_group(
            f"crack_line_{line+1}", [x, 0, 0], [],
            group_uuid=pgu,
        )
        # We'll add line group but children remain at top-level for individual control
        crack_parents.append((pgu, pg, line))

    # Ground frost — 8 irregular slabs scattered
    frost_groups = []
    frost_positions = [
        (-8, -3), (8, -2), (-9, 5), (9, 6), (-7, 8), (7, 9), (-5, -5), (6, -4),
    ]
    for i, (px, pz) in enumerate(frost_positions):
        cube_u = b.add_cube(
            f"frost_{i+1}",
            from_=[-0.9, 0.02, -0.6], to_=[0.9, 0.08, 0.6],
            faces=tundra_face(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"ground_frost_{i+1}", [px, 0.05, pz], [cube_u],
            rotation=[0, i * 23, 0], group_uuid=gu,
        )
        frost_groups.append((gu, g))

    # Origin cluster — 4-cube dense assembly at convergence
    origin_cubes = []
    origin_cubes.append(b.add_cube("origin_a", from_=[-0.7, 0, -0.7], to_=[0.7, 1.2, 0.7],
                                    faces=cursed_face()))
    origin_cubes.append(b.add_cube("origin_b", from_=[-0.5, 0.6, -0.9], to_=[0.5, 1.8, 0.9],
                                    faces=cursed_face()))
    origin_cubes.append(b.add_cube("origin_c", from_=[-0.9, 0.3, -0.5], to_=[0.9, 1.5, 0.5],
                                    faces=cursed_face()))
    origin_cubes.append(b.add_cube("origin_d", from_=[-0.4, 1.4, -0.4], to_=[0.4, 2.4, 0.4],
                                    faces=cursed_face()))
    origin_uuid = make_uuid()
    origin_g = b.make_group("origin_cluster", [0, 0, -7], origin_cubes, group_uuid=origin_uuid)

    root_children = [origin_g]
    for (sgu, sg, _l, _s) in all_slabs: root_children.append(sg)
    for (wgu, wg, *_r) in all_walls: root_children.append(wg)
    for (psgu, psg) in cursed_spikes: root_children.append(psg)
    for (gu, g) in frost_groups: root_children.append(g)
    root_uuid = make_uuid()
    b.outliner.append(b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid))

    # SPAWN 1.1s
    spawn = {}
    # Origin at 0.0s
    spawn[origin_uuid] = {"name": "origin_cluster", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 0, 0, 0),
        make_keyframe("scale", 0.08, 1.2, 1.2, 1.2),
        make_keyframe("scale", 0.18, 1, 1, 1),
        make_keyframe("scale", 1.1, 1, 1, 1),
    ]}

    # All 5 cracks extend simultaneously from origin outward, slab stagger 0.05s
    for (sgu, sg, line, s) in all_slabs:
        start = 0.05 + s * 0.05
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", start, 0, 0, 0),
            make_keyframe("scale", start + 0.10, 1, 1, 1),
            make_keyframe("scale", 1.1, 1, 1, 1),
        ]
        spawn[sgu] = {"name": sg["name"], "type": "bone", "keyframes": kfs}

    # Walls erupt at their slab section appearance
    for (wgu, wg, line, s, w_idx) in all_walls:
        start = 0.10 + s * 0.05 + w_idx * 0.02
        kfs = [
            make_keyframe("scale", 0.0, 1, 0, 1),
            make_keyframe("scale", start, 1, 0, 1),
            make_keyframe("scale", start + 0.15, 1.1, 1.15, 1.1),
            make_keyframe("scale", start + 0.25, 1, 1, 1),
            make_keyframe("scale", 1.1, 1, 1, 1),
        ]
        spawn[wgu] = {"name": wg["name"], "type": "bone", "keyframes": kfs}

    # Ground frost at 0.2s
    for idx, (gu, g) in enumerate(frost_groups):
        start = 0.2 + idx * 0.03
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", start, 0, 0, 0),
            make_keyframe("scale", start + 0.10, 1, 1, 1),
            make_keyframe("scale", 1.1, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Cursed spikes at 0.5s
    for idx, (psgu, psg) in enumerate(cursed_spikes):
        start = 0.5 + idx * 0.04
        kfs = [
            make_keyframe("scale", 0.0, 1, 0, 1),
            make_keyframe("scale", start, 1, 0, 1),
            make_keyframe("scale", start + 0.15, 1.1, 1.2, 1.1),
            make_keyframe("scale", start + 0.25, 1, 1, 1),
            make_keyframe("scale", 1.1, 1, 1, 1),
        ]
        spawn[psgu] = {"name": psg["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("spawn", length=1.1, animators=spawn, loop="once", override=True)

    # IDLE 5.0s — wave wall heights along each crack
    idle = {}
    for (wgu, wg, line, s, w_idx) in all_walls:
        kfs = []
        # phase travels along crack
        phase_offset = s * 0.5 + w_idx * 0.15
        for k in range(11):
            t = (k / 10) * 5.0
            ph = (t / 3.0 - phase_offset) * 2 * math.pi
            sy = 1.0 + 0.12 * (1 - math.cos(ph))
            kfs.append(make_keyframe("scale", t, 1, sy, 1))
        idle[wgu] = {"name": wg["name"], "type": "bone", "keyframes": kfs}

    # Cursed spikes pulse emissive (scale pulse)
    for idx, (psgu, psg) in enumerate(cursed_spikes):
        period = 2.0 + idx * 0.5
        kfs = []
        for k in range(11):
            t = (k / 10) * 5.0
            ph = (t / period + idx * 0.3) * 2 * math.pi
            s = 1.0 + 0.10 * (1 - math.cos(ph))
            kfs.append(make_keyframe("scale", t, s, s, s))
        idle[psgu] = {"name": psg["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g) in enumerate(frost_groups):
        kfs = []
        for k in range(7):
            t = (k / 6) * 5.0
            ph = (t / 4 + idx * 0.3) * 2 * math.pi
            s = 1.0 + 0.05 * (1 - math.cos(ph))
            kfs.append(make_keyframe("scale", t, s, s, s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Origin slow rotation
    idle[origin_uuid] = {"name": "origin_cluster", "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, 0, 0, 0),
        make_keyframe("rotation", 1.25, 0, 30, 0),
        make_keyframe("rotation", 2.5, 0, 60, 0),
        make_keyframe("rotation", 3.75, 0, 90, 0),
        make_keyframe("rotation", 5.0, 0, 120, 0),
    ]}

    b.add_animation("idle", length=5.0, animators=idle, loop="loop", override=False)

    # DISSIPATE 0.8s — spikes flash then retract, walls sink outer to inner
    diss = {}
    for idx, (psgu, psg) in enumerate(cursed_spikes):
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.1, 1.4, 1.4, 1.4),
            make_keyframe("scale", 0.2, 1, 1, 1),
            make_keyframe("scale", 0.5, 0, 0, 0),
        ]
        diss[psgu] = {"name": psg["name"], "type": "bone", "keyframes": kfs}

    # Walls sink from outer cracks (s=3) inward (s=0)
    for (wgu, wg, line, s, w_idx) in all_walls:
        # outer first (s=3 starts at 0, s=0 at 0.3)
        start = (3 - s) * 0.08
        kfs = [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", start, 0, 0, 0),
            make_keyframe("position", start + 0.15, 0, -1.6, 0),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", start + 0.10, 1, 1, 1),
            make_keyframe("scale", start + 0.20, 0, 0, 0),
            make_keyframe("scale", 0.8, 0, 0, 0),
        ]
        diss[wgu] = {"name": wg["name"], "type": "bone", "keyframes": kfs}

    for (sgu, sg, line, s) in all_slabs:
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.5, 1, 1, 1),
            make_keyframe("scale", 0.8, 0, 0, 0),
        ]
        diss[sgu] = {"name": sg["name"], "type": "bone", "keyframes": kfs}

    for idx, (gu, g) in enumerate(frost_groups):
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.8, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    diss[origin_uuid] = {"name": "origin_cluster", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 1, 1, 1),
        make_keyframe("scale", 0.5, 1.5, 1.5, 1.5),
        make_keyframe("scale", 0.8, 0, 0, 0),
    ]}

    b.add_animation("dissipate", length=0.8, animators=diss, loop="once", override=True)

    b.write(OUTPUT_PATH)
    import os
    sz = os.path.getsize(OUTPUT_PATH)
    print(f"Wrote {OUTPUT_PATH} ({sz} bytes, {sz/1024:.1f} KB)")


if __name__ == "__main__":
    build()
