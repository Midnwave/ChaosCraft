#!/usr/bin/env python3
"""Generator: ghost_chain_volley.bbmodel — Chain attack 13 'Thirteen Links'.

13 spectral chain links flying in a tight cluster — 5 central + 8 outer.
6 wisp slabs trail the formation. A bright lead-glow cube leads the formation.

Uniqueness vs FreezingIce/Fluffy:
  - 13 individual chain LINKS — distinct count, formation-based identity
  - Each link tumbles on its own axis (13 independent rotations) — unique
  - Simultaneous SCATTER on dissipate — ghost-dispersing
  - Spectral silver-blue palette — distinct from snow blue / pastel pink
"""

import sys
import math
import random

sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder, make_uuid, make_keyframe, uniform_face,
)
from _chain_proj_helpers import (
    tex_spectral_link, tex_spectral_glow,
    add_proj_chain_link,
    osc_scale_kfs, osc_rot_kfs, osc_pos_kfs,
)


OUTPUT = "D:/CC/ChaosCraft/src/main/resources/models/chain/me_attacks/ghost_chain_volley.bbmodel"
RES = 128


def build():
    b = Builder("ghost_chain_volley", resolution=(RES, RES), visible_box=(6, 6, 0))
    b.add_texture("spectral_link", tex_spectral_link(RES, RES))
    b.add_texture("spectral_glow", tex_spectral_glow(RES, RES))

    F0 = lambda: uniform_face(0, 0, RES, RES, tex_index=0)
    F1 = lambda: uniform_face(0, 0, RES, RES, tex_index=1)

    # =========================================================
    # 13 CHAIN LINKS — 5 core + 8 outer
    # Each link a 4-cube oval, slight scale/angle differences
    # =========================================================
    # Cluster positions
    core_positions = [
        # (x, y, z, scale, rot_x, rot_y, rot_z)
        ( 0.00, 0.00, 0.00, 1.10,  0,  0,  0),
        ( 0.45, 0.15, 0.10, 1.00, 12, 22,  7),
        (-0.45, 0.10, 0.20, 1.00, -8, 18, -5),
        ( 0.15, -0.40, -0.10, 0.95, 15, -20, 10),
        (-0.20, -0.30, 0.30, 0.95, -10, -16, 8),
    ]
    outer_positions = [
        ( 0.90, 0.65, 0.30, 0.85,  20,  30, 12),
        (-0.95, 0.55, 0.20, 0.85, -18,  35, -8),
        ( 0.95, -0.65, 0.10, 0.85,  25, -30, 14),
        (-0.85, -0.60, 0.40, 0.85, -22, -34, -6),
        ( 0.30, 0.90, -0.30, 0.80,   8,  16, 20),
        (-0.35, 0.85, -0.40, 0.80, -10,  20, -16),
        ( 0.30, -0.90, -0.20, 0.80,  10, -22, 22),
        (-0.30, -0.85, -0.35, 0.80, -12, -18, -18),
    ]
    all_positions = core_positions + outer_positions
    assert len(all_positions) == 13, "Must be exactly 13 links"

    link_segs = []  # (group_uuid, group_dict, idx, base_pos, base_scale, base_rot)
    for i, (px, py, pz, sc, rx, ry, rz) in enumerate(all_positions):
        gu, g = add_proj_chain_link(
            b, f"ghost_link_{i+1}", link_radius=0.32 * sc, thickness=0.10 * sc,
            face_factory=F0,
        )
        g["origin"] = [px, py, pz]
        g["rotation"] = [rx, ry, rz]
        link_segs.append((gu, g, i, (px, py, pz), sc, (rx, ry, rz)))

    # =========================================================
    # FORMATION WAKE — 6 spectral wisp slabs trailing the cluster
    # =========================================================
    wake_segs = []
    for i in range(6):
        ang = (i / 6.0) * 2 * math.pi
        # Stagger Z depth so wisps fan
        zc = -1.0 - i * 0.5
        # Spread laterally in a slight cone
        rx = math.cos(ang) * 0.4 * (1 + i * 0.15)
        ry = math.sin(ang) * 0.4 * (1 + i * 0.15)
        cu = b.add_cube(
            f"wake{i+1}",
            from_=[-0.55, -0.03, -0.85], to_=[0.55, 0.03, 0.85],
            faces=F0(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"wake_{i+1}", [rx, ry, zc], [cu],
            rotation=[math.degrees(ang) * 0.3, math.degrees(ang) * 0.4, math.degrees(ang)],
            group_uuid=gu,
        )
        wake_segs.append((gu, g, ang, i))

    # =========================================================
    # LEAD GLOW — emissive small cube at the front of the formation
    # =========================================================
    lead_cu = b.add_cube(
        "lead_glow",
        from_=[-0.30, -0.30, -0.30], to_=[0.30, 0.30, 0.30],
        faces=F1(),
    )
    lead_gu = make_uuid()
    lead_grp = b.make_group(
        "lead_glow_grp", [0, 0, 1.40], [lead_cu],
        group_uuid=lead_gu,
    )

    # Additional small lead glow inner core for richer look
    lead_inner_cu = b.add_cube(
        "lead_glow_inner",
        from_=[-0.16, -0.16, -0.16], to_=[0.16, 0.16, 0.16],
        faces=F1(),
    )
    lead_inner_gu = make_uuid()
    lead_inner_grp = b.make_group(
        "lead_glow_inner_grp", [0, 0, 1.40], [lead_inner_cu],
        rotation=[45, 45, 0],
        group_uuid=lead_inner_gu,
    )

    # =========================================================
    # ROOT — entire cluster rotates as a unit (tumbling formation)
    # =========================================================
    children = [g for (_u, g, _i, _p, _s, _r) in link_segs]
    children += [g for (_u, g, _a, _i) in wake_segs]
    children += [lead_grp, lead_inner_grp]

    root_uuid = make_uuid()
    root = b.make_group("root", [0, 0, 0], children, group_uuid=root_uuid)
    b.outliner.append(root)

    # =========================================================
    # SPAWN (0.30s) — all 13 materialise simultaneously, lead first, wake fans
    # =========================================================
    spawn = {}
    # Lead glow appears FIRST
    spawn[lead_gu] = {"name": "lead_glow_grp", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, x=0, y=0, z=0),
        make_keyframe("scale", 0.04, x=1.3, y=1.3, z=1.3),
        make_keyframe("scale", 0.10, x=1.0, y=1.0, z=1.0),
    ]}
    spawn[lead_inner_gu] = {"name": "lead_glow_inner_grp", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, x=0, y=0, z=0),
        make_keyframe("scale", 0.02, x=0, y=0, z=0),
        make_keyframe("scale", 0.06, x=1.5, y=1.5, z=1.5),
        make_keyframe("scale", 0.12, x=1.0, y=1.0, z=1.0),
    ]}
    # All 13 links materialise simultaneously at ~0.10s
    for gu, g, i, _p, _s, _r in link_segs:
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", 0.09, x=0, y=0, z=0),
            make_keyframe("scale", 0.13, x=0.1, y=0.1, z=0.1),
            make_keyframe("scale", 0.18, x=1.15, y=1.15, z=1.15),
            make_keyframe("scale", 0.24, x=1.0, y=1.0, z=1.0),
        ]}
    # Formation wake fans behind sequentially
    for gu, g, ang, i in wake_segs:
        t_app = 0.18 + i * 0.018
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", max(0, t_app - 0.001), x=0, y=0, z=0),
            make_keyframe("scale", t_app + 0.05, x=1.0, y=1.0, z=1.0),
        ]}
    b.add_animation("spawn", length=0.30, animators=spawn, loop="once", override=True)

    # =========================================================
    # IDLE (2.0s) — cluster tumbles as unit, EACH link rotates independently,
    # formation wake oscillates, lead pulses
    # =========================================================
    idle = {}
    # Entire root tumbles slowly as unit
    idle[root_uuid] = {"name": "root", "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0,  x=0,   y=0,   z=0),
        make_keyframe("rotation", 0.5,  x=22,  y=45,  z=15),
        make_keyframe("rotation", 1.0,  x=44,  y=90,  z=30),
        make_keyframe("rotation", 1.5,  x=66,  y=135, z=45),
        make_keyframe("rotation", 2.0,  x=88,  y=180, z=60),
    ]}
    # Each of 13 links rotates on its own axis at its own rate
    for gu, g, i, _p, _s, (rx, ry, rz) in link_segs:
        # Each link unique rotation period & phase
        period = 1.0 + (i * 0.07)
        kfs = osc_rot_kfs(2.0, 24,
                          amp_x=20 + i * 2,
                          amp_y=25 + (i % 4) * 5,
                          amp_z=15 + (i % 3) * 3,
                          period=period,
                          phase=i * 0.31)
        # Slight position oscillation (formation breathing)
        kfs_pos = osc_pos_kfs(2.0, 16,
                              amp_x=0.04, amp_y=0.04, amp_z=0.04,
                              period=1.5 + i * 0.05, phase=i * 0.25)
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs + kfs_pos}
    # Formation wake oscillates
    for gu, g, ang, i in wake_segs:
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_pos_kfs(2.0, 20, amp_x=0.10, amp_y=0.15, amp_z=0.20,
                                              period=1.2, phase=i * 0.25) +
                                  osc_rot_kfs(2.0, 18, amp_x=5, amp_y=8, amp_z=10,
                                               period=1.2, phase=i * 0.25) +
                                  osc_scale_kfs(2.0, 18, base=1.0, amp=0.12,
                                                period=1.2, phase=i * 0.25)}
    # Lead glow pulses
    idle[lead_gu] = {"name": "lead_glow_grp", "type": "bone",
                     "keyframes": osc_scale_kfs(2.0, 24, base=1.0, amp=0.35,
                                                 period=0.5, phase=0.0)}
    idle[lead_inner_gu] = {"name": "lead_glow_inner_grp", "type": "bone",
                           "keyframes": osc_scale_kfs(2.0, 20, base=1.0, amp=0.55,
                                                       period=0.4, phase=0.2) +
                                        [make_keyframe("rotation", t, x=t * 240, y=t * 360, z=t * 180)
                                         for t in [k * 2.0 / 12 for k in range(13)]]}
    b.add_animation("idle", length=2.0, animators=idle, loop="loop", override=False)

    # =========================================================
    # DISSIPATE (0.30s) — all 13 SIMULTANEOUSLY scatter outward
    # =========================================================
    diss = {}
    # Pre-generate scatter directions: each unique
    rng = random.Random(1313)
    for gu, g, i, (px, py, pz), sc, (rx, ry, rz) in link_segs:
        # Pick a scatter direction that's roughly away from cluster centre
        cx = px + rng.uniform(-0.3, 0.3)
        cy = py + rng.uniform(-0.3, 0.3)
        cz = pz + rng.uniform(-0.3, 0.3)
        mag = math.sqrt(cx * cx + cy * cy + cz * cz) + 0.001
        cx /= mag; cy /= mag; cz /= mag
        # Distance varies per link
        dist = 5 + (i % 4) * 1.5
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.30,
                           x=cx * dist, y=cy * dist, z=cz * dist),
            make_keyframe("rotation", 0.0, x=0, y=0, z=0),
            make_keyframe("rotation", 0.30,
                           x=180 + i * 40, y=360 + i * 30, z=240 + i * 20),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.10, x=1.4, y=1.4, z=1.4),
            make_keyframe("scale", 0.30, x=0, y=0, z=0),
        ]}
    # Lead glow flares and vanishes
    diss[lead_gu] = {"name": "lead_glow_grp", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, x=1, y=1, z=1),
        make_keyframe("scale", 0.08, x=2.5, y=2.5, z=2.5),
        make_keyframe("scale", 0.18, x=0, y=0, z=0),
    ]}
    diss[lead_inner_gu] = {"name": "lead_glow_inner_grp", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, x=1, y=1, z=1),
        make_keyframe("scale", 0.06, x=3.0, y=3.0, z=3.0),
        make_keyframe("scale", 0.16, x=0, y=0, z=0),
    ]}
    # Wake continues backward
    for gu, g, ang, i in wake_segs:
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.30,
                           x=math.cos(ang) * 1.5,
                           y=math.sin(ang) * 1.5,
                           z=-2.5),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.30, x=0, y=0, z=0),
        ]}
    b.add_animation("dissipate", length=0.30, animators=diss, loop="once", override=True)

    b.write(OUTPUT)
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    build()
