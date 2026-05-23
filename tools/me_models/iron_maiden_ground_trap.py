#!/usr/bin/env python3
"""Generator for iron_maiden_ground_trap.bbmodel — Chain Mode "Open Arms".

The top half of an iron maiden erupting from the ground — just the opening
doors and the interior spikes visible, the chains that hold it shut snapping
taut and then releasing as it opens. Medieval dungeon iron — cold grey-purple
rather than rusty.

Bone hierarchy:
  - 2 door panel groups (each: 3 flat slab pieces forming a half-oval)
  - 12 door spikes (6 per panel, on inner face)
  - 1 hinge column bone
  - 3 binding chain link groups (top/middle/bottom)
  - 2 ground platform slab bones
  - 8 ground rupture pieces
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
    emissive_seam,
    shifted_face,
)

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/chain/me_attacks/iron_maiden_ground_trap.bbmodel"
RES = 128


# ---------------------------------------------------------------------------
# TEXTURE 0 — DUNGEON IRON (door panels / spikes / hinge)
#   Medieval cold iron, not rust — grey-purple
# ---------------------------------------------------------------------------

def build_dungeon_iron_texture():
    w = h = RES
    iron_black   = hex_to_rgba("#0c0a0e")
    dark_metal   = hex_to_rgba("#24202a")
    aged         = hex_to_rgba("#484050")
    plate_edge   = hex_to_rgba("#706870")
    spec         = hex_to_rgba("#a098a0")
    faint_purp   = hex_to_rgba("#5040a0")

    pixels = base_fill(w, h, dark_metal)

    # Vertical stratification — cold grey-purple
    for y in range(h):
        t = y / (h - 1)
        if t < 0.07:
            c = spec
        elif t < 0.20:
            local = (t - 0.07) / 0.13
            c = color_lerp(spec, plate_edge, local)
        elif t < 0.45:
            local = (t - 0.20) / 0.25
            c = color_lerp(plate_edge, aged, local)
        elif t < 0.72:
            local = (t - 0.45) / 0.27
            c = color_lerp(aged, dark_metal, local)
        else:
            local = (t - 0.72) / 0.28
            c = color_lerp(dark_metal, iron_black, local)
        for x in range(w):
            pixels[y * w + x] = c

    # RIVETED PANEL TEXTURE — the dungeon iron identity
    rivet_grid(pixels, w, h, plate_edge, iron_black, step_x=14, step_y=14, radius=2)

    # Hammer marks (older forging)
    hammer_marks(pixels, w, h, iron_black, plate_edge, count=24, seed=43)

    # Horizontal aging grain
    horizontal_grain(pixels, w, h, dark_metal, density=0.45, seed=87)

    # Faint purple emissive in darkest recesses — old dungeon iron absorbed curse
    for y in range(int(h * 0.75), h):
        for x in range(w):
            if (x * 5 + y * 3) % 13 == 0:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, faint_purp, 0.32)

    emissive_seam(pixels, w, h, w // 4 + 3, faint_purp, intensity=0.30, width=1)
    emissive_seam(pixels, w, h, 3 * w // 4 - 3, faint_purp, intensity=0.30, width=1)

    # Specular at top edges
    specular_cluster(pixels, w, h, 16, 8, 4, spec, strength=0.78)
    specular_cluster(pixels, w, h, w - 18, 12, 3, plate_edge, strength=0.65)
    specular_cluster(pixels, w, h, w // 2, 6, 3, spec, strength=0.6)

    add_noise_overlay(pixels, w, h, iron_black, aged, density=0.20, seed=137)
    jitter_inplace(pixels, jitter_range=16, seed=379)
    jitter_inplace(pixels, jitter_range=10, seed=531, density=0.5)

    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# TEXTURE 1 — DUNGEON STONE (ground platform / rupture / binding chains)
# ---------------------------------------------------------------------------

def build_dungeon_stone_texture():
    w = h = RES
    stone_dark   = hex_to_rgba("#080608")
    stone_mid    = hex_to_rgba("#181420")
    joint_line   = hex_to_rgba("#201828")
    joint_bright = hex_to_rgba("#382838")

    pixels = base_fill(w, h, stone_mid)

    # Vertical stratification — slightly brighter top
    for y in range(h):
        t = y / (h - 1)
        if t < 0.3:
            local = t / 0.3
            c = color_lerp(joint_bright, stone_mid, local)
        elif t < 0.7:
            local = (t - 0.3) / 0.4
            c = color_lerp(stone_mid, stone_dark, local * 0.5)
        else:
            local = (t - 0.7) / 0.3
            c = color_lerp(stone_mid, stone_dark, 0.5 + local * 0.5)
        for x in range(w):
            pixels[y * w + x] = c

    # Cracked joint lines — flagstone pattern
    # Horizontal joints every ~32 px
    joint_rows = [int(h * 0.28), int(h * 0.55), int(h * 0.82)]
    for jr in joint_rows:
        for x in range(w):
            for dy in range(-1, 2):
                yy = jr + dy
                if 0 <= yy < h:
                    p = pixels[yy * w + x]
                    pixels[yy * w + x] = color_lerp(p, joint_line, 0.75 if dy == 0 else 0.4)
    # Vertical joints
    joint_cols = [int(w * 0.3), int(w * 0.65)]
    for jc in joint_cols:
        for y in range(h):
            for dx in range(-1, 2):
                xx = jc + dx
                if 0 <= xx < w:
                    p = pixels[y * w + xx]
                    pixels[y * w + xx] = color_lerp(p, joint_line, 0.7 if dx == 0 else 0.35)

    # Hammered stone speckle
    hammer_marks(pixels, w, h, stone_dark, joint_bright, count=30, seed=51)

    add_noise_overlay(pixels, w, h, stone_dark, joint_bright, density=0.22, seed=171)
    jitter_inplace(pixels, jitter_range=16, seed=389)
    jitter_inplace(pixels, jitter_range=10, seed=433, density=0.55)

    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# MODEL BUILD
# ---------------------------------------------------------------------------

def build():
    b = Builder("iron_maiden_ground_trap", resolution=(RES, RES), visible_box=(12, 10, 0))
    b.add_texture("dungeon_iron", build_dungeon_iron_texture())
    b.add_texture("dungeon_stone", build_dungeon_stone_texture())

    def iron_face(seed):
        return shifted_face(seed % 100, (seed * 7) % 100, 22, tex_index=0, RES=RES)
    def stone_face(seed):
        return shifted_face(seed % 100, (seed * 11) % 100, 20, tex_index=1, RES=RES)

    # =====================================================================
    # 1) DOOR PANELS — left and right, each 3 flat slab pieces (half-oval)
    # =====================================================================
    door_panels = []  # (group_uuid, group, side, hinge_x)
    for ci, (side, sign) in enumerate([("left", -1), ("right", 1)]):
        cubes = []
        # 3 slabs forming a half-oval: tall narrow centre, wider mid, widest top
        slab_data = [
            # (y_center, hw, hd, name_suffix)
            (5.5, 1.6, 0.30, "top"),     # widest top
            (3.5, 2.0, 0.30, "mid"),     # wider middle
            (1.5, 1.4, 0.30, "low"),     # narrower bottom
        ]
        for si, (cy, hw, hd, suffix) in enumerate(slab_data):
            # Mounted on the inside-facing side of the panel
            cu = b.add_cube(
                f"door_{side}_{suffix}",
                from_=[sign * 0.2, cy - 1.0, -hw],
                to_=  [sign * 0.2 + sign * hd * 2, cy + 1.0, hw],
                faces=iron_face(40 + ci * 50 + si * 7),
            )
            cubes.append(cu)
        gu = make_uuid()
        # Hinge x = 0 (centre), panels open outward via rotation around Y
        g = b.make_group(
            f"door_panel_{side}", [0, 3.5, 0], cubes,
            rotation=[0, 0, 0], group_uuid=gu,
        )
        door_panels.append((gu, g, side, sign))

    # =====================================================================
    # 2) DOOR SPIKES — 6 per panel on inner face (12 total)
    # =====================================================================
    spike_groups = []  # (gu, g, side, sign)
    for ci, (side, sign) in enumerate([("left", -1), ("right", 1)]):
        # 6 spikes arranged in a 2x3 grid on inner face
        spike_positions_local = [
            # (cy, cz, length)
            (5.0,  0.7, 0.8),
            (5.0, -0.7, 0.8),
            (3.5,  0.7, 1.0),
            (3.5, -0.7, 1.0),
            (2.0,  0.7, 0.7),
            (2.0, -0.7, 0.7),
        ]
        for si, (cy, cz, length) in enumerate(spike_positions_local):
            # spike points inward (toward x=0 from each panel)
            # Place at the inner face of the panel
            # Inner face x: left panel inner ~ x=0.4, right panel inner ~ x=-0.4
            inner_x = sign * 0.45
            tip_x = inner_x + (-sign) * length  # tips point inward
            # Build narrow tapered cube
            cu = b.add_cube(
                f"spike_{side}_{si+1}",
                from_=[min(inner_x, tip_x), cy - 0.10, cz - 0.10],
                to_=  [max(inner_x, tip_x), cy + 0.10, cz + 0.10],
                faces=iron_face(180 + ci * 30 + si * 4),
            )
            gu = make_uuid()
            g = b.make_group(
                f"door_spike_{side}_{si+1}", [(inner_x + tip_x) / 2, cy, cz], [cu],
                group_uuid=gu,
            )
            spike_groups.append((gu, g, side, sign))

    # =====================================================================
    # 3) HINGE COLUMN — central vertical slab between the two panels
    # =====================================================================
    hinge_uuid = b.add_cube(
        "hinge_column",
        from_=[-0.18, 0.5, -0.5], to_=[0.18, 6.5, 0.5],
        faces=iron_face(310),
    )
    hinge_gu = make_uuid()
    hinge_g = b.make_group(
        "hinge_column", [0, 3.5, 0], [hinge_uuid], group_uuid=hinge_gu,
    )

    # =====================================================================
    # 4) BINDING CHAINS — 3 chain link groups at top, middle, bottom
    # =====================================================================
    binding_groups = []
    binding_y_positions = [(6.0, "top"), (3.5, "middle"), (1.5, "bottom")]
    for bi, (by, label) in enumerate(binding_y_positions):
        # Each binding chain spans from left panel to right panel
        # 3 links per binding
        cubes = []
        for li in range(3):
            t = li / 2.0
            cx = -1.6 + t * 3.2
            # horizontal slab
            h_uuid = b.add_cube(
                f"bind_{label}_l{li+1}_h",
                from_=[cx - 0.36, by - 0.10, -0.18],
                to_=  [cx + 0.36, by + 0.10,  0.18],
                faces=stone_face(360 + bi * 20 + li * 5),
            )
            # vertical slab
            v_uuid = b.add_cube(
                f"bind_{label}_l{li+1}_v",
                from_=[cx - 0.18, by - 0.36, -0.10],
                to_=  [cx + 0.18, by + 0.36,  0.10],
                faces=stone_face(375 + bi * 20 + li * 5),
            )
            cubes.append(h_uuid)
            cubes.append(v_uuid)
        gu = make_uuid()
        g = b.make_group(
            f"binding_chain_{label}", [0, by, 0], cubes, group_uuid=gu,
        )
        binding_groups.append((gu, g, label))

    # =====================================================================
    # 5) GROUND PLATFORM — 2 wide flat slabs at Y=0
    # =====================================================================
    platform_groups = []
    for pi, (z0, z1) in enumerate([(-2.5, 0), (0, 2.5)]):
        cu = b.add_cube(
            f"platform_{pi+1}",
            from_=[-3.5, -0.20, z0], to_=[3.5, 0.10, z1],
            faces=stone_face(430 + pi * 25),
        )
        gu = make_uuid()
        g = b.make_group(
            f"ground_platform_{pi+1}", [0, -0.05, (z0 + z1) / 2], [cu], group_uuid=gu,
        )
        platform_groups.append((gu, g))

    # =====================================================================
    # 6) GROUND RUPTURE — 8 cracks around platform
    # =====================================================================
    rupture_groups = []
    for i in range(8):
        ang = (i / 8.0) * 2 * math.pi + 0.06
        r = 5.0
        cx = math.cos(ang) * r
        cz = math.sin(ang) * r
        cu = b.add_cube(
            f"rupture_{i+1}",
            from_=[-1.4, 0.02, -0.50], to_=[1.4, 0.16, 0.50],
            faces=stone_face(480 + i * 17),
        )
        rot_y = math.degrees(ang)
        gu = make_uuid()
        g = b.make_group(
            f"ground_rupture_{i+1}", [cx, 0.10, cz], [cu],
            rotation=[0, rot_y, 0], group_uuid=gu,
        )
        rupture_groups.append((gu, g, ang))

    # ROOT
    root_children = []
    for (gu, g, _side, _sign) in door_panels:
        root_children.append(g)
    for (gu, g, _side, _sign) in spike_groups:
        root_children.append(g)
    root_children.append(hinge_g)
    for (gu, g, _label) in binding_groups:
        root_children.append(g)
    for (gu, g) in platform_groups:
        root_children.append(g)
    for (gu, g, _ang) in rupture_groups:
        root_children.append(g)

    root_uuid = make_uuid()
    root_group = b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid)
    b.outliner.append(root_group)

    # ===========================================================================
    # ANIMATION — SPAWN (1.1s)
    # Ground rupture 0.2s. Platform slams 0.3s. Hinge column 0.4s.
    # Door panels emerge closed at 0.5s. Binding chains materialise (taut) 0.6s.
    # Doors burst open at 0.8s with overshoot.
    # Binding chains snap taut then scale to 0 ("break free").
    # ===========================================================================
    spawn = {}

    # Ground rupture
    for idx, (gu, g, ang) in enumerate(rupture_groups):
        delay = 0.2 + idx * 0.02
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.10, 1.3, 1.3, 1.3),
            make_keyframe("scale", delay + 0.18, 1, 1, 1),
            make_keyframe("scale", 1.1, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Platform slams up at 0.3s
    for idx, (gu, g) in enumerate(platform_groups):
        delay = 0.3 + idx * 0.03
        kfs = [
            make_keyframe("position", 0.0, 0, -2.5, 0),
            make_keyframe("position", delay, 0, -2.5, 0),
            make_keyframe("position", delay + 0.12, 0, 0.2, 0),
            make_keyframe("position", delay + 0.20, 0, 0, 0),
            make_keyframe("position", 1.1, 0, 0, 0),
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.10, 1.1, 1.2, 1.1),
            make_keyframe("scale", delay + 0.20, 1, 1, 1),
            make_keyframe("scale", 1.1, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Hinge column at 0.4s
    kfs = [
        make_keyframe("position", 0.0, 0, -4, 0),
        make_keyframe("position", 0.4, 0, -4, 0),
        make_keyframe("position", 0.55, 0, 0.3, 0),
        make_keyframe("position", 0.65, 0, 0, 0),
        make_keyframe("position", 1.1, 0, 0, 0),
        make_keyframe("scale", 0.0, 0, 0, 0),
        make_keyframe("scale", 0.4, 0, 0, 0),
        make_keyframe("scale", 0.52, 1.1, 1.3, 1.1),
        make_keyframe("scale", 0.65, 1, 1, 1),
        make_keyframe("scale", 1.1, 1, 1, 1),
    ]
    spawn[hinge_gu] = {"name": hinge_g["name"], "type": "bone", "keyframes": kfs}

    # Door panels emerge CLOSED at 0.5s (still closed = rotation 0)
    for ci, (gu, g, side, sign) in enumerate(door_panels):
        delay = 0.5 + ci * 0.02
        # final open rotation: 75° outward
        open_rot = sign * 75
        kfs = [
            make_keyframe("position", 0.0, 0, -3, 0),
            make_keyframe("position", delay, 0, -3, 0),
            make_keyframe("position", delay + 0.15, 0, 0.2, 0),
            make_keyframe("position", delay + 0.25, 0, 0, 0),
            make_keyframe("position", 1.1, 0, 0, 0),
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.12, 1, 1, 1),
            make_keyframe("scale", 1.1, 1, 1, 1),
            # closed initially
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 0.78, 0, 0, 0),
            # burst open at 0.8s — overshoot then settle
            make_keyframe("rotation", 0.88, 0, open_rot * 1.20, 0),
            make_keyframe("rotation", 0.98, 0, open_rot * 0.92, 0),
            make_keyframe("rotation", 1.08, 0, open_rot, 0),
            make_keyframe("rotation", 1.1, 0, open_rot, 0),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Door spikes follow the panel they're on — same delay
    for idx, (gu, g, side, sign) in enumerate(spike_groups):
        delay = 0.55 + idx * 0.01
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.10, 1, 1, 1),
            make_keyframe("scale", 1.1, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Binding chains materialise at 0.6s (taut, holding doors shut)
    # Then SNAP at 0.8s — scale 0 ("break free")
    for bi, (gu, g, label) in enumerate(binding_groups):
        delay = 0.6 + bi * 0.04
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.10, 1, 1, 1),
            # hold until doors burst (0.80)
            make_keyframe("scale", 0.78, 1, 1, 1),
            # snap taut briefly
            make_keyframe("scale", 0.82, 1.2, 0.8, 1.2),
            # break free — scale to 0
            make_keyframe("scale", 0.92, 0, 0, 0),
            make_keyframe("scale", 1.1, 0, 0, 0),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("spawn", length=1.1, animators=spawn, loop="once", override=True)

    # ===========================================================================
    # ANIMATION — IDLE (5.0s)
    # Door panels very slowly breathe open/close ±3° on a 5s period.
    # Interior spikes oscillate ±1° individually.
    # Hinge column holds still.
    # ===========================================================================
    idle = {}

    # Door panels breathe
    for ci, (gu, g, side, sign) in enumerate(door_panels):
        open_rot = sign * 75
        kfs = []
        for k in range(11):
            t = (k / 10) * 5.0
            ph = (t / 5.0) * 2 * math.pi
            ry = open_rot + math.sin(ph) * 3.0 * sign
            kfs.append(make_keyframe("rotation", t, 0, ry, 0))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Spikes oscillate ±1°
    for idx, (gu, g, side, sign) in enumerate(spike_groups):
        kfs = []
        for k in range(9):
            t = (k / 8) * 5.0
            ph = (t / 3.5) * 2 * math.pi + idx * 0.4
            rx = math.sin(ph) * 1.0
            rz = math.cos(ph * 0.9) * 1.0
            kfs.append(make_keyframe("rotation", t, rx, 0, rz))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Hinge holds still — but very subtle Y wobble
    kfs = []
    for k in range(9):
        t = (k / 8) * 5.0
        ph = (t / 5.0) * 2 * math.pi
        kfs.append(make_keyframe("position", t, 0, math.sin(ph) * 0.05, 0))
    idle[hinge_gu] = {"name": hinge_g["name"], "type": "bone", "keyframes": kfs}

    # Platform breathes
    for idx, (gu, g) in enumerate(platform_groups):
        kfs = []
        for k in range(9):
            t = (k / 8) * 5.0
            ph = (t / 4.5) * 2 * math.pi + idx * 0.3
            s = 1.0 + 0.03 * math.sin(ph)
            kfs.append(make_keyframe("scale", t, s, 1, s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Ground rupture pulse
    for idx, (gu, g, ang) in enumerate(rupture_groups):
        kfs = []
        for k in range(9):
            t = (k / 8) * 5.0
            ph = (t / 4.0) * 2 * math.pi + idx * 0.4
            s = 1.0 + 0.05 * math.sin(ph)
            kfs.append(make_keyframe("scale", t, s, 1, s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("idle", length=5.0, animators=idle, loop="loop", override=False)

    # ===========================================================================
    # ANIMATION — DISSIPATE (0.7s)
    # Doors slam closed rapidly. Binding chains re-materialise briefly then
    # whole structure sinks. Platform retracts. Ground rupture contracts.
    # ===========================================================================
    diss = {}

    # Doors slam closed (rotation back to 0) at 0.0-0.15s
    for ci, (gu, g, side, sign) in enumerate(door_panels):
        kfs = [
            make_keyframe("rotation", 0.0, 0, sign * 75, 0),
            make_keyframe("rotation", 0.08, 0, sign * 30, 0),
            make_keyframe("rotation", 0.15, 0, sign * -8, 0),
            make_keyframe("rotation", 0.20, 0, 0, 0),
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.4, 0, -1, 0),
            make_keyframe("position", 0.7, 0, -4, 0),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.45, 1, 1, 1),
            make_keyframe("scale", 0.7, 0.3, 0.3, 0.3),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Spikes hide as doors close
    for idx, (gu, g, side, sign) in enumerate(spike_groups):
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.15, 1, 1, 1),
            make_keyframe("scale", 0.25, 0, 0, 0),
            make_keyframe("scale", 0.7, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Binding chains re-materialise briefly
    for bi, (gu, g, label) in enumerate(binding_groups):
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", 0.05, 0, 0, 0),
            make_keyframe("scale", 0.18, 1, 1, 1),
            make_keyframe("scale", 0.30, 1, 1, 1),
            make_keyframe("scale", 0.40, 0, 0, 0),
            make_keyframe("scale", 0.7, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Hinge column sinks
    kfs = [
        make_keyframe("position", 0.0, 0, 0, 0),
        make_keyframe("position", 0.4, 0, -2, 0),
        make_keyframe("position", 0.7, 0, -5, 0),
        make_keyframe("scale", 0.0, 1, 1, 1),
        make_keyframe("scale", 0.5, 1, 1, 1),
        make_keyframe("scale", 0.7, 0.3, 0.3, 0.3),
    ]
    diss[hinge_gu] = {"name": hinge_g["name"], "type": "bone", "keyframes": kfs}

    # Platform retracts
    for idx, (gu, g) in enumerate(platform_groups):
        kfs = [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.5, 0, -0.5, 0),
            make_keyframe("position", 0.7, 0, -2, 0),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.5, 1, 1, 1),
            make_keyframe("scale", 0.7, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Ground rupture contracts
    for idx, (gu, g, ang) in enumerate(rupture_groups):
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.55, 1, 1, 1),
            make_keyframe("scale", 0.7, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("dissipate", length=0.7, animators=diss, loop="once", override=True)

    b.write(OUTPUT_PATH)
    sz = os.path.getsize(OUTPUT_PATH)
    print(f"Wrote {OUTPUT_PATH} ({sz} bytes, {sz/1024:.1f} KB)")


if __name__ == "__main__":
    build()
