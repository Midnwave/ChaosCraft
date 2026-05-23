#!/usr/bin/env python3
"""Generator: chain_comet_me.bbmodel — Chain attack 14 'Heavy Throw'.

A compressed ball of chains thrown like a comet — 8-cube dense iron core
with barely-visible chain texture, 10-link chain tail trailing behind with
phased whip, 6-slab compression ring at the core equator, 4-slab wake
turbulence in the immediate wake.

Filename uses _me suffix to avoid collision with the existing chain_comet
BlockDisplay attack (same pattern as frozen_comet_me).

Uniqueness vs FreezingIce/Fluffy:
  - Core reads as SOLID iron sphere (barely-visible chain pattern) — distinct
    from frozen_comet_me's space ice or rubber_duck_squeak's single body
  - Chain TAIL is individual visible chain links — distinct from comet ion tail
  - Compression ring at equator catching light — unique geometry
  - Rust + warm-iron palette — distinct from cold blue or pastel
"""

import sys
import math

sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder, make_uuid, make_keyframe, uniform_face,
)
from _chain_proj_helpers import (
    tex_compressed_core, tex_compression_ring,
    add_proj_chain_link,
    fast_spin_kfs, chain_whip_kfs, osc_rot_kfs, osc_scale_kfs, osc_pos_kfs,
    tumble_kfs,
)


OUTPUT = "D:/CC/ChaosCraft/src/main/resources/models/chain/me_attacks/chain_comet_me.bbmodel"
RES = 128


def build():
    b = Builder("chain_comet_me", resolution=(RES, RES), visible_box=(14, 6, 0))
    b.add_texture("compressed_core", tex_compressed_core(RES, RES))
    b.add_texture("compression_ring", tex_compression_ring(RES, RES))

    F0 = lambda: uniform_face(0, 0, RES, RES, tex_index=0)
    F1 = lambda: uniform_face(0, 0, RES, RES, tex_index=1)

    # =========================================================
    # COMPRESSED CORE — 8 overlapping cubes of varying sizes forming dense sphere
    # =========================================================
    core_specs = [
        # (from, to, rot, name)
        ((-1.30, -1.05, -0.95), (1.30, 0.95, 1.05), (5, 8, -4),   "core_a"),
        ((-1.00, -0.65, -1.20), (1.10, 1.20, 0.95), (-9, 14, 7),  "core_b"),
        ((-1.20, -0.85, -0.50), (0.85, 1.10, 1.15), (12, -6, 11), "core_c"),
        (( 0.10, -1.10, -0.85), (1.25, 0.65, 1.10), (-5, 20, -9), "core_d"),
        ((-0.90, -1.15, -0.60), (0.85, 0.25, 0.85), (15, -10, 5), "core_e"),
        ((-0.50,  0.40, -1.10), (0.95, 1.05, 0.45), (-8, 7, 16),  "core_f"),
        ((-1.10,  0.00, -0.30), (0.30, 0.80, 1.15), (10, 18, -6), "core_g"),
        (( 0.50, -0.40, -1.10), (1.20, 0.95, -0.10), (8, -14, 12),"core_h"),
    ]
    core_segs = []
    for fr, to, rot, name in core_specs:
        cu = b.add_cube(name, from_=list(fr), to_=list(to), faces=F0())
        gu = make_uuid()
        g = b.make_group(f"{name}_grp", [0, 0, 0], [cu], rotation=list(rot), group_uuid=gu)
        core_segs.append((gu, g))

    # =========================================================
    # CHAIN TAIL — 10 link bones in comet tail behind core
    # Wider spacing and smaller scale toward end of tail
    # =========================================================
    tail_segs = []
    TAIL_COUNT = 10
    for i in range(TAIL_COUNT):
        # spacing increases with distance (more open at tail end)
        z_spacing_factor = 1.0 + i * 0.18
        link_scale = max(0.45, 1.0 - i * 0.06)
        gu, g = add_proj_chain_link(
            b, f"tail_link_{i+1}",
            link_radius=0.40 * link_scale,
            thickness=0.13 * link_scale,
            face_factory=F0,
        )
        zc = -(2.0 + i * 1.1 * z_spacing_factor)
        # Small Y/X variation creates a 3d tail
        yc = math.sin(i * 0.5) * 0.30
        xc = math.cos(i * 0.4) * 0.25
        # Alternate link orientation
        link_rot_z = (i * 25) % 360
        g["origin"] = [xc, yc, zc]
        g["rotation"] = [10 + i * 4, math.degrees(math.sin(i * 0.3)) * 8, link_rot_z]
        tail_segs.append((gu, g, i, link_scale))

    # =========================================================
    # COMPRESSION RING — 6 flat slabs in circle at sphere equator
    # =========================================================
    ring_segs = []
    for i in range(6):
        ang = (i / 6.0) * 2 * math.pi
        rx = math.cos(ang) * 1.30
        rz = math.sin(ang) * 1.30
        cu = b.add_cube(
            f"ring_{i+1}",
            from_=[-0.55, -0.06, -0.18], to_=[0.55, 0.06, 0.18],
            faces=F1(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"ring_seg_{i+1}", [rx, 0, rz], [cu],
            rotation=[0, math.degrees(ang) + 90, 0],
            group_uuid=gu,
        )
        ring_segs.append((gu, g, ang, i))

    # Container group for the ring (rotates with core)
    ring_group_children = [g for (_u, g, _a, _i) in ring_segs]
    ring_root_uuid = make_uuid()
    ring_root = b.make_group(
        "ring_root", [0, 0, 0], ring_group_children, group_uuid=ring_root_uuid,
    )

    # =========================================================
    # WAKE TURBULENCE — 4 flat slabs in immediate wake of core
    # =========================================================
    wake_segs = []
    for i in range(4):
        ang = (i / 4.0) * 2 * math.pi + math.pi / 8
        # Just behind core (small -z)
        zc = -1.3 - i * 0.45
        rx = math.cos(ang) * 0.55
        ry = math.sin(ang) * 0.55
        cu = b.add_cube(
            f"wake{i+1}",
            from_=[-0.85, -0.04, -0.50], to_=[0.85, 0.04, 0.50],
            faces=F1(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"wake_{i+1}", [rx, ry, zc], [cu],
            rotation=[math.degrees(ang) * 0.4, math.degrees(ang) * 0.6, math.degrees(ang)],
            group_uuid=gu,
        )
        wake_segs.append((gu, g, ang, i))

    # =========================================================
    # CORE TUMBLE GROUP — contains all 8 core cubes; rotates as a single
    # tumbling unit on all 3 axes
    # =========================================================
    core_root_uuid = make_uuid()
    core_root = b.make_group(
        "core_root", [0, 0, 0],
        [g for (_u, g) in core_segs] + [ring_root],
        group_uuid=core_root_uuid,
    )

    # =========================================================
    # ROOT
    # =========================================================
    children = [core_root]
    children += [g for (_u, g, _i, _s) in tail_segs]
    children += [g for (_u, g, _a, _i) in wake_segs]

    root_uuid = make_uuid()
    root = b.make_group("root", [0, 0, 0], children, group_uuid=root_uuid)
    b.outliner.append(root)

    # =========================================================
    # SPAWN (0.30s) — core materialises spinning, tail sequentially, ring expands, wake materialises
    # =========================================================
    spawn = {}
    # Core segments materialise very fast
    for i, (gu, g) in enumerate(core_segs):
        t_app = i * 0.005
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", max(0, t_app - 0.001), x=0, y=0, z=0),
            make_keyframe("scale", t_app + 0.02, x=0.1, y=0.1, z=0.1),
            make_keyframe("scale", t_app + 0.06, x=1.15, y=1.15, z=1.15),
            make_keyframe("scale", t_app + 0.10, x=1.0, y=1.0, z=1.0),
        ]}
    # Core root already spinning at spawn
    spawn[core_root_uuid] = {"name": "core_root", "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, x=0, y=0, z=0),
        make_keyframe("rotation", 0.30, x=80, y=160, z=60),
    ]}
    # Tail links appear sequentially from core outward
    for gu, g, i, link_scale in tail_segs:
        t_app = 0.05 + i * 0.020
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", max(0, t_app - 0.001), x=0, y=0, z=0),
            make_keyframe("scale", t_app, x=0.05, y=0.05, z=0.05),
            make_keyframe("scale", t_app + 0.05, x=1.0, y=1.0, z=1.0),
        ]}
    # Compression ring expands
    spawn[ring_root_uuid] = {"name": "ring_root", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, x=0, y=1, z=0),
        make_keyframe("scale", 0.05, x=0, y=1, z=0),
        make_keyframe("scale", 0.18, x=1.25, y=1, z=1.25),
        make_keyframe("scale", 0.28, x=1.0, y=1, z=1.0),
    ]}
    for gu, g, ang, i in ring_segs:
        t_app = 0.06 + i * 0.012
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", max(0, t_app - 0.001), x=0, y=0, z=0),
            make_keyframe("scale", t_app + 0.04, x=1.0, y=1.0, z=1.0),
        ]}
    # Wake materialises
    for gu, g, ang, i in wake_segs:
        t_app = 0.18 + i * 0.020
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", max(0, t_app - 0.001), x=0, y=0, z=0),
            make_keyframe("scale", t_app + 0.05, x=1.0, y=1.0, z=1.0),
        ]}
    b.add_animation("spawn", length=0.30, animators=spawn, loop="once", override=True)

    # =========================================================
    # IDLE (2.0s) — core tumbles all 3 axes, tail links phase-whip behind,
    # compression ring spins with core, wake follows
    # =========================================================
    idle = {}
    # Core tumbles slowly on 3 axes (not the rapid throw spin of a flail —
    # this comet is heavy and slow)
    idle[core_root_uuid] = {"name": "core_root", "type": "bone",
                            "keyframes": tumble_kfs(2.0, 24,
                                                    deg_x_total=200, deg_y_total=360, deg_z_total=120)}
    # Each core segment wobbles slightly within the tumble
    for i, (gu, g) in enumerate(core_segs):
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_rot_kfs(2.0, 16, amp_x=3, amp_y=4, amp_z=2,
                                              period=1.5, phase=i * 0.30) +
                                  osc_scale_kfs(2.0, 16, base=1.0, amp=0.04,
                                                period=1.5, phase=i * 0.30)}
    # Tail links phase-whip behind core — looser amplitude toward tail end
    for gu, g, i, link_scale in tail_segs:
        amp = 0.15 + i * 0.08
        kfs_pos = chain_whip_kfs(2.0, 24,
                                  amp_x=amp, amp_y=amp * 1.1, amp_z=amp * 0.6,
                                  period=2.0, phase=-i * 0.22)
        kfs_rot = osc_rot_kfs(2.0, 20,
                               amp_x=8 + i * 2,
                               amp_y=5 + i,
                               amp_z=10 + i * 2,
                               period=2.0, phase=-i * 0.20)
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs_pos + kfs_rot}
    # Compression ring spins with core (independent additional rotation)
    idle[ring_root_uuid] = {"name": "ring_root", "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, x=0, y=0, z=0),
        make_keyframe("rotation", 0.5, x=0, y=180, z=0),
        make_keyframe("rotation", 1.0, x=0, y=360, z=0),
        make_keyframe("rotation", 1.5, x=0, y=540, z=0),
        make_keyframe("rotation", 2.0, x=0, y=720, z=0),
    ]}
    # Individual ring segments oscillate scale (catching light)
    for gu, g, ang, i in ring_segs:
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_scale_kfs(2.0, 20, base=1.0, amp=0.20,
                                                period=0.8, phase=i * 0.20)}
    # Wake follows core (oscillating turbulence)
    for gu, g, ang, i in wake_segs:
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_pos_kfs(2.0, 20, amp_x=0.15, amp_y=0.20, amp_z=0.30,
                                              period=1.2, phase=-i * 0.30) +
                                  osc_rot_kfs(2.0, 18, amp_x=8, amp_y=10, amp_z=12,
                                               period=1.2, phase=-i * 0.30) +
                                  osc_scale_kfs(2.0, 18, base=1.0, amp=0.18,
                                                period=1.2, phase=-i * 0.30)}
    b.add_animation("idle", length=2.0, animators=idle, loop="loop", override=False)

    # =========================================================
    # DISSIPATE (0.35s) — core explodes (each cube flies different direction),
    # tail continues backward + scatters, ring expands rapidly
    # =========================================================
    diss = {}
    # Each core cube flies in a different direction
    for i, (gu, g) in enumerate(core_segs):
        ang = (i / len(core_specs)) * 2 * math.pi
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.35,
                           x=math.cos(ang) * 6,
                           y=math.sin(ang * 0.6) * 4,
                           z=math.sin(ang) * 6),
            make_keyframe("rotation", 0.0, x=0, y=0, z=0),
            make_keyframe("rotation", 0.35,
                           x=360 + i * 50, y=540 + i * 70, z=180 + i * 40),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.20, x=1.25, y=1.25, z=1.25),
            make_keyframe("scale", 0.35, x=0, y=0, z=0),
        ]}
    # Tail links continue backward AND scatter
    for gu, g, i, link_scale in tail_segs:
        # Bias scatter direction by link index for variety
        scatter_x = math.cos(i * 0.7) * (1.0 + i * 0.15)
        scatter_y = math.sin(i * 0.5) * (1.0 + i * 0.10)
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.35,
                           x=scatter_x,
                           y=scatter_y,
                           z=-3.5 - i * 0.6),
            make_keyframe("rotation", 0.0, x=0, y=0, z=0),
            make_keyframe("rotation", 0.35, x=240, y=360 + i * 30, z=180),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.35, x=0, y=0, z=0),
        ]}
    # Compression ring expands rapidly
    diss[ring_root_uuid] = {"name": "ring_root", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, x=1, y=1, z=1),
        make_keyframe("scale", 0.20, x=2.5, y=1, z=2.5),
        make_keyframe("scale", 0.35, x=3.5, y=0, z=3.5),
    ]}
    for gu, g, ang, i in ring_segs:
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.35,
                           x=math.cos(ang) * 2.5,
                           y=0,
                           z=math.sin(ang) * 2.5),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.35, x=0, y=0, z=0),
        ]}
    # Wake fades
    for gu, g, ang, i in wake_segs:
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.35, x=0, y=0, z=0),
        ]}
    b.add_animation("dissipate", length=0.35, animators=diss, loop="once", override=True)

    b.write(OUTPUT)
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    build()
