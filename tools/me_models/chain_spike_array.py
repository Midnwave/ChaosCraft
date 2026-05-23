#!/usr/bin/env python3
"""Generator for chain_spike_array.bbmodel — Chain Mode "Iron Bed".

A field of chain-tipped spikes erupting from the ground in a precise 3x4 grid.
Each spike is topped with a short length of chain that whips outward on impact.
Newly forged black iron — no rust, no magic, pure industrial threat.

Bone hierarchy:
  - 12 spike bones in 3x4 grid (each spike = 3 tapered segments)
  - 12 chain top assemblies (each = 3 link chain)
  - 3 base plate slab bones
  - 8 ground rupture crack pieces
  - 4 chain whip trail pieces (corner spikes)
"""

import sys
import math
import os

sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder,
    make_uuid,
    make_keyframe,
    uniform_face,
    png_from_pixels,
    hex_to_rgba,
    color_lerp,
    fill_rect,
    add_noise_overlay,
)
from _chain_helpers import (
    base_fill,
    jitter_inplace,
    hammer_marks,
    rivet_grid,
    horizontal_grain,
    crack_lines_radial,
    specular_cluster,
    shifted_face,
)

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/chain/me_attacks/chain_spike_array.bbmodel"
RES = 128


# ---------------------------------------------------------------------------
# TEXTURE 0 — FORGED IRON (spikes / chain links / chain tops)
# ---------------------------------------------------------------------------

def build_forged_iron_texture():
    w = h = RES
    iron_black  = hex_to_rgba("#0a0808")
    forged_dark = hex_to_rgba("#201410")
    surface     = hex_to_rgba("#483828")
    bright_edge = hex_to_rgba("#807060")
    spec        = hex_to_rgba("#c0b090")
    cutting     = hex_to_rgba("#d8ccac")

    pixels = base_fill(w, h, forged_dark)

    # Vertical stratification: cutting edge top, surface mid, deep dark bottom
    for y in range(h):
        t = y / (h - 1)
        if t < 0.05:
            c = cutting
        elif t < 0.13:
            local = (t - 0.05) / 0.08
            c = color_lerp(cutting, bright_edge, local)
        elif t < 0.35:
            local = (t - 0.13) / 0.22
            c = color_lerp(bright_edge, surface, local)
        elif t < 0.62:
            local = (t - 0.35) / 0.27
            c = color_lerp(surface, forged_dark, local)
        else:
            local = (t - 0.62) / 0.38
            c = color_lerp(forged_dark, iron_black, local)
        for x in range(w):
            pixels[y * w + x] = c

    # Hammer marks scattered (the key forge texture identity)
    hammer_marks(pixels, w, h, iron_black, bright_edge, count=42, seed=21)
    hammer_marks(pixels, w, h, forged_dark, surface, count=28, seed=55)

    # Horizontal forge grain — manufactured directional
    horizontal_grain(pixels, w, h, forged_dark, density=0.55, seed=77)

    # Cutting edge specular cluster
    specular_cluster(pixels, w, h, RES // 4, 6, 4, cutting, strength=0.85)
    specular_cluster(pixels, w, h, RES // 2 + 8, 8, 3, spec, strength=0.7)
    specular_cluster(pixels, w, h, RES - 24, 5, 4, spec, strength=0.78)

    # No emissive — newly forged
    add_noise_overlay(pixels, w, h, iron_black, surface, density=0.22, seed=101)
    jitter_inplace(pixels, jitter_range=18, seed=313)
    jitter_inplace(pixels, jitter_range=10, seed=521, density=0.50)

    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# TEXTURE 1 — IRON FLOOR PLATE (base plate / ground cracks)
# ---------------------------------------------------------------------------

def build_floor_plate_texture():
    w = h = RES
    plate_dark   = hex_to_rgba("#0c0a08")
    plate_mid    = hex_to_rgba("#1c1814")
    bolt_bright  = hex_to_rgba("#a09080")
    bolt_shadow  = hex_to_rgba("#040303")
    rust_bleed   = hex_to_rgba("#501808")

    pixels = base_fill(w, h, plate_dark)

    # Vertical band — slightly brighter middle
    for y in range(h):
        t = y / (h - 1)
        if t < 0.5:
            local = t / 0.5
            c = color_lerp(plate_dark, plate_mid, local)
        else:
            local = (t - 0.5) / 0.5
            c = color_lerp(plate_mid, plate_dark, local)
        for x in range(w):
            pixels[y * w + x] = c

    # BOLT-HEAD RIVET GRID — manufactured detail
    rivet_grid(pixels, w, h, bolt_bright, bolt_shadow, step_x=16, step_y=16, radius=2)

    # Some rust-bleed at corners (where ground crack lines stain)
    cracker_seed = 27
    crack_lines_radial(pixels, w, h, w // 2, h // 2, branches=5, color=rust_bleed, seed=cracker_seed, max_len=int(w * 0.4))

    add_noise_overlay(pixels, w, h, plate_dark, plate_mid, density=0.20, seed=131)
    jitter_inplace(pixels, jitter_range=16, seed=379)
    jitter_inplace(pixels, jitter_range=10, seed=419, density=0.55)

    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# MODEL BUILD
# ---------------------------------------------------------------------------

def build():
    b = Builder("chain_spike_array", resolution=(RES, RES), visible_box=(18, 14, 0))
    b.add_texture("forged_iron", build_forged_iron_texture())
    b.add_texture("floor_plate", build_floor_plate_texture())

    def iron_face(seed):
        return shifted_face(seed % 100, (seed * 7) % 100, 20, tex_index=0, RES=RES)
    def plate_face(seed):
        return shifted_face(seed % 100, (seed * 11) % 100, 18, tex_index=1, RES=RES)

    # =====================================================================
    # 1) SPIKE GRID — 12 spikes in 3x4 grid pattern, varied heights 5-9
    # =====================================================================
    grid_xs = [-6, -2, 2, 6]   # 4 columns
    grid_zs = [-4, 0, 4]       # 3 rows
    # Heights varied per cell, 5-9
    heights = [
        [6, 8, 5, 7],
        [9, 6, 8, 6],
        [7, 5, 9, 8],
    ]

    spike_data = []  # (group_uuid, group, gx, gz, total_h, ring_idx)
    grid_centre = (0, 0)
    for ri, gz in enumerate(grid_zs):
        for ci, gx in enumerate(grid_xs):
            total_h = heights[ri][ci]
            seg_count = 3
            cubes = []
            # 3 tapered segments — wide base -> mid -> narrow tip
            seg_heights = [total_h * 0.35, total_h * 0.40, total_h * 0.25]
            widths = [0.85, 0.55, 0.22]
            depths = [0.85, 0.55, 0.22]
            y_cursor = 0.0
            for si in range(seg_count):
                y0 = y_cursor
                y1 = y_cursor + seg_heights[si]
                hw = widths[si]
                hd = depths[si]
                cu = b.add_cube(
                    f"spike_r{ri}c{ci}_s{si+1}",
                    from_=[-hw, y0, -hd], to_=[hw, y1, hd],
                    faces=iron_face(40 + ri * 30 + ci * 7 + si * 3),
                )
                cubes.append(cu)
                y_cursor = y1
            # ring distance from grid centre to compute spawn order
            ring_idx = max(abs(ci - 1.5), abs(ri - 1))
            gu = make_uuid()
            g = b.make_group(
                f"spike_r{ri}c{ci}", [gx, 0.5, gz], cubes, group_uuid=gu,
            )
            spike_data.append((gu, g, gx, gz, total_h, ring_idx))

    # =====================================================================
    # 2) CHAIN TOPS — 3-link chain at each spike tip
    # =====================================================================
    chain_top_data = []  # (group_uuid, group, parent_spike_idx, corner_flag)
    corner_set = {(0, 0), (0, 3), (2, 0), (2, 3)}  # 4 corners
    for ri, gz in enumerate(grid_zs):
        for ci, gx in enumerate(grid_xs):
            total_h = heights[ri][ci]
            tip_y = total_h
            is_corner = (ri, ci) in corner_set
            cubes = []
            # 3 chain links above the spike tip
            for li in range(3):
                link_y = tip_y + 0.5 + li * 0.7
                # horizontal slab
                h_uuid = b.add_cube(
                    f"chain_top_r{ri}c{ci}_l{li+1}_h",
                    from_=[-0.32, link_y - 0.10, -0.18],
                    to_=  [ 0.32, link_y + 0.10,  0.18],
                    faces=iron_face(180 + ri * 17 + ci * 5 + li * 3),
                )
                # vertical slab — alternates 90° per link
                v_uuid = b.add_cube(
                    f"chain_top_r{ri}c{ci}_l{li+1}_v",
                    from_=[-0.18, link_y - 0.30, -0.10],
                    to_=  [ 0.18, link_y + 0.30,  0.10],
                    faces=iron_face(195 + ri * 13 + ci * 5 + li * 3),
                )
                cubes.append(h_uuid)
                cubes.append(v_uuid)
            gu = make_uuid()
            g = b.make_group(
                f"chain_top_r{ri}c{ci}", [gx, tip_y, gz], cubes, group_uuid=gu,
            )
            chain_top_data.append((gu, g, ri, ci, is_corner))

    # =====================================================================
    # 3) BASE PLATE — 3 wide flat slabs at Y=0
    # =====================================================================
    plate_groups = []
    plate_data = [
        # (x0, x1, z0, z1)
        (-8, 8, -6, -2),
        (-8, 8, -2,  2),
        (-8, 8,  2,  6),
    ]
    for i, (x0, x1, z0, z1) in enumerate(plate_data):
        cu = b.add_cube(
            f"plate_{i+1}",
            from_=[x0, -0.18, z0], to_=[x1, 0.05, z1],
            faces=plate_face(280 + i * 19),
        )
        gu = make_uuid()
        g = b.make_group(
            f"base_plate_{i+1}", [(x0 + x1) / 2, -0.06, (z0 + z1) / 2], [cu], group_uuid=gu,
        )
        plate_groups.append((gu, g))

    # =====================================================================
    # 4) GROUND RUPTURE — 8 cracks around the base plate
    # =====================================================================
    rupture_groups = []
    for i in range(8):
        ang = (i / 8.0) * 2 * math.pi
        r = 10.0
        cx = math.cos(ang) * r
        cz = math.sin(ang) * r
        cu = b.add_cube(
            f"rupture_{i+1}",
            from_=[-1.8, 0.02, -0.45], to_=[1.8, 0.16, 0.45],
            faces=plate_face(330 + i * 21),
        )
        rot_y = math.degrees(ang)
        gu = make_uuid()
        g = b.make_group(
            f"rupture_{i+1}", [cx, 0.1, cz], [cu],
            rotation=[0, rot_y, 0], group_uuid=gu,
        )
        rupture_groups.append((gu, g, ang))

    # =====================================================================
    # 5) CHAIN WHIP TRAILS — 4 thin slabs at corner spikes
    # =====================================================================
    whip_groups = []
    whip_specs = [
        # (gx, gz, dir_ang_deg)
        (-6, -4,  225),  # top-left corner
        ( 6, -4,  315),  # top-right
        (-6,  4,  135),  # bot-left
        ( 6,  4,   45),  # bot-right
    ]
    for i, (gx, gz, ang_deg) in enumerate(whip_specs):
        # Place at the spike tip, oriented outward
        tip_y = 7
        ang_rad = math.radians(ang_deg)
        offset_x = math.cos(ang_rad) * 1.6
        offset_z = math.sin(ang_rad) * 1.6
        cu = b.add_cube(
            f"whip_{i+1}",
            from_=[-1.4, -0.08, -0.18], to_=[1.4, 0.08, 0.18],
            faces=iron_face(400 + i * 23),
        )
        gu = make_uuid()
        g = b.make_group(
            f"whip_{i+1}", [gx + offset_x, tip_y, gz + offset_z], [cu],
            rotation=[0, ang_deg, 0], group_uuid=gu,
        )
        whip_groups.append((gu, g, i))

    # ROOT
    root_children = []
    for (gu, g, *_rest) in spike_data:
        root_children.append(g)
    for (gu, g, *_rest) in chain_top_data:
        root_children.append(g)
    for (gu, g) in plate_groups:
        root_children.append(g)
    for (gu, g, _ang) in rupture_groups:
        root_children.append(g)
    for (gu, g, _i) in whip_groups:
        root_children.append(g)

    root_uuid = make_uuid()
    root_group = b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid)
    b.outliner.append(root_group)

    # ===========================================================================
    # ANIMATION — SPAWN (0.8s)
    # Base plate slams at 0.1s. Ground cracks 0.15s.
    # Spikes erupt grid-centre-outward wave at 0.2s, 0.08s per ring.
    # Chain tops whip outward at spike arrival.
    # Whip trails fly outward at corner spikes at 0.5s.
    # ===========================================================================
    spawn = {}

    # Base plate slams down at 0.1s
    for idx, (gu, g) in enumerate(plate_groups):
        delay = 0.1 + idx * 0.02
        kfs = [
            make_keyframe("position", 0.0, 0, 4, 0),
            make_keyframe("position", delay, 0, 4, 0),
            make_keyframe("position", delay + 0.10, 0, -0.15, 0),
            make_keyframe("position", delay + 0.16, 0, 0, 0),
            make_keyframe("position", 0.8, 0, 0, 0),
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.08, 1.1, 1.2, 1.1),
            make_keyframe("scale", delay + 0.16, 1, 1, 1),
            make_keyframe("scale", 0.8, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Ground cracks radiate at 0.15s
    for idx, (gu, g, ang) in enumerate(rupture_groups):
        delay = 0.15 + idx * 0.02
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.10, 1.25, 1.25, 1.25),
            make_keyframe("scale", delay + 0.18, 1, 1, 1),
            make_keyframe("scale", 0.8, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Spikes erupt grid-centre-outward — 0.08s per ring (centre at ring_idx=0.5)
    for idx, (gu, g, gx, gz, total_h, ring_idx) in enumerate(spike_data):
        delay = 0.2 + ring_idx * 0.08
        kfs = [
            make_keyframe("position", 0.0, 0, -total_h - 1, 0),
            make_keyframe("position", delay, 0, -total_h - 1, 0),
            make_keyframe("position", delay + 0.12, 0, 0.3, 0),
            make_keyframe("position", delay + 0.20, 0, 0, 0),
            make_keyframe("position", 0.8, 0, 0, 0),
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.08, 1.15, 1.3, 1.15),
            make_keyframe("scale", delay + 0.18, 1, 1, 1),
            make_keyframe("scale", 0.8, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Chain tops whip outward on spike arrival
    for idx, (gu, g, ri, ci, is_corner) in enumerate(chain_top_data):
        ring_idx = max(abs(ci - 1.5), abs(ri - 1))
        delay = 0.2 + ring_idx * 0.08 + 0.18
        # whip direction — pick angle based on grid position
        ang_to_outside = math.atan2(ri - 1, ci - 1.5)
        whip_amp = 25.0
        kfs = [
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", delay, 0, 0, 0),
            make_keyframe("rotation", delay + 0.10, 0, math.degrees(ang_to_outside), 0),
            make_keyframe("rotation", delay + 0.18,
                          math.cos(ang_to_outside) * whip_amp, math.degrees(ang_to_outside),
                          math.sin(ang_to_outside) * whip_amp),
            make_keyframe("rotation", delay + 0.30, 0, math.degrees(ang_to_outside), 0),
            make_keyframe("rotation", 0.8, 0, math.degrees(ang_to_outside), 0),
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.10, 1, 1, 1),
            make_keyframe("scale", 0.8, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Whip trails fly outward at 0.5s
    for idx, (gu, g, i) in enumerate(whip_groups):
        delay = 0.5 + idx * 0.03
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.10, 1.4, 0.6, 1.4),
            make_keyframe("scale", delay + 0.20, 1, 1, 1),
            make_keyframe("scale", 0.8, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("spawn", length=0.8, animators=spawn, loop="once", override=True)

    # ===========================================================================
    # ANIMATION — IDLE (4.0s)
    # Spike tips oscillate ±1°. Chain tops sway on individual stagger.
    # Ground cracks pulse faintly. Base plate holds still.
    # ===========================================================================
    idle = {}

    # Spikes oscillate ±1°
    for idx, (gu, g, gx, gz, total_h, ring_idx) in enumerate(spike_data):
        kfs = []
        for k in range(11):
            t = (k / 10) * 4.0
            ph = ((t + idx * 0.3) / 3.5) * 2 * math.pi
            rx = math.sin(ph) * 1.0
            rz = math.cos(ph * 0.7) * 1.0
            kfs.append(make_keyframe("rotation", t, rx, 0, rz))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Chain tops — sway on individual stagger
    for idx, (gu, g, ri, ci, is_corner) in enumerate(chain_top_data):
        period = 2.0 + (idx % 5) * 0.4
        phase_offset = idx * 0.25
        kfs = []
        for k in range(13):
            t = (k / 12) * 4.0
            ph = ((t / period) + phase_offset) * 2 * math.pi
            rx = math.sin(ph) * 5.0
            rz = math.cos(ph * 0.9) * 5.0
            kfs.append(make_keyframe("rotation", t, rx, 0, rz))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Ground cracks pulse
    for idx, (gu, g, ang) in enumerate(rupture_groups):
        kfs = []
        for k in range(9):
            t = (k / 8) * 4.0
            ph = (t / 3.5) * 2 * math.pi + idx * 0.5
            s = 1.0 + 0.05 * math.sin(ph)
            kfs.append(make_keyframe("scale", t, s, 1, s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Whip trails subtle drift
    for idx, (gu, g, i) in enumerate(whip_groups):
        kfs = []
        for k in range(9):
            t = (k / 8) * 4.0
            ph = (t / 3.0) * 2 * math.pi + idx * 0.7
            yy = math.sin(ph) * 0.15
            kfs.append(make_keyframe("position", t, 0, yy, 0))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("idle", length=4.0, animators=idle, loop="loop", override=False)

    # ===========================================================================
    # ANIMATION — DISSIPATE (0.6s)
    # Chain tops retract first. Spikes retract centre-outward (reverse of spawn).
    # Base plate is last. Ground cracks close.
    # ===========================================================================
    diss = {}

    # Chain tops retract first
    for idx, (gu, g, ri, ci, is_corner) in enumerate(chain_top_data):
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.10, 1, 1, 1),
            make_keyframe("scale", 0.20, 0, 0, 0),
            make_keyframe("scale", 0.6, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Whip trails retract
    for idx, (gu, g, i) in enumerate(whip_groups):
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.15, 0, 0, 0),
            make_keyframe("scale", 0.6, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Spikes retract centre-outward
    for idx, (gu, g, gx, gz, total_h, ring_idx) in enumerate(spike_data):
        delay = ring_idx * 0.06 + 0.1
        kfs = [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", delay, 0, 0, 0),
            make_keyframe("position", delay + 0.12, 0, -total_h - 0.5, 0),
            make_keyframe("position", 0.6, 0, -total_h - 1, 0),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", delay + 0.12, 1, 1, 1),
            make_keyframe("scale", 0.6, 0.4, 0.4, 0.4),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Base plate is last
    for idx, (gu, g) in enumerate(plate_groups):
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.45, 1, 1, 1),
            make_keyframe("scale", 0.6, 0, 0, 0),
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.6, 0, -1, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Ground cracks close
    for idx, (gu, g, ang) in enumerate(rupture_groups):
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.4, 1, 1, 1),
            make_keyframe("scale", 0.6, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("dissipate", length=0.6, animators=diss, loop="once", override=True)

    b.write(OUTPUT_PATH)
    sz = os.path.getsize(OUTPUT_PATH)
    print(f"Wrote {OUTPUT_PATH} ({sz} bytes, {sz/1024:.1f} KB)")


if __name__ == "__main__":
    build()
