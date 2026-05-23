#!/usr/bin/env python3
"""Generator: cursed_chain_laser.bbmodel — Chain attack 12 'Binding Ray'.

A sustained beam of cursed chain energy — 5 flat horizontal slab layers form
the beam body, 8 chain links stream THROUGH the beam (looping Z drift),
4 edge slabs overflow the beam boundary, a 6-piece emitter at the origin,
8 fan slabs at the impact, and 3 rotating bind rings at the impact point.

Uniqueness vs FreezingIce/Fluffy:
  - PURPLE-VOID palette with cursed bright bands — distinct from ice / pastels
  - Stream of chain links flying THROUGH the beam — unique flow animation
  - Bind-effect rotating ground rings at impact — distinct from fluffy/ice impacts
  - Long-Z beam shape — distinct from the wide-fog frost_breath_beam
"""

import sys
import math

sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder, make_uuid, make_keyframe, uniform_face,
)
from _chain_proj_helpers import (
    tex_cursed_beam, tex_cursed_emitter,
    add_proj_chain_link,
    osc_scale_kfs, osc_rot_kfs, osc_pos_kfs, stream_flow_kfs,
)


OUTPUT = "D:/CC/ChaosCraft/src/main/resources/models/chain/me_attacks/cursed_chain_laser.bbmodel"
RES = 128

BEAM_LENGTH = 16.0  # along Z


def build():
    b = Builder("cursed_chain_laser", resolution=(RES, RES), visible_box=(8, 4, 0))
    b.add_texture("cursed_beam", tex_cursed_beam(RES, RES))
    b.add_texture("cursed_emitter", tex_cursed_emitter(RES, RES))

    F0 = lambda: uniform_face(0, 0, RES, RES, tex_index=0)
    F1 = lambda: uniform_face(0, 0, RES, RES, tex_index=1)

    # =========================================================
    # BEAM LAYERS — 5 flat horizontal slabs stacked on Y
    # Wide on X, thin on Y, long on Z
    # =========================================================
    BEAM_HALF_Z = BEAM_LENGTH / 2.0
    beam_segs = []
    layer_specs = [
        # (half_width_x, half_height_y, y_offset, scale_hint)
        (1.0, 0.04, 0.00),
        (0.85, 0.04, 0.20),
        (0.85, 0.04, -0.20),
        (0.65, 0.04, 0.40),
        (0.65, 0.04, -0.40),
    ]
    for i, (hx, hy, yo) in enumerate(layer_specs):
        cu = b.add_cube(
            f"beam_layer{i+1}",
            from_=[-hx, -hy, -BEAM_HALF_Z],
            to_=[hx, hy, BEAM_HALF_Z],
            faces=F0(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"beam_layer_{i+1}", [0, yo, 0], [cu], group_uuid=gu,
        )
        beam_segs.append((gu, g))

    # =========================================================
    # CHAIN LINK STREAM — 8 chain links along beam, looping Z drift
    # =========================================================
    stream_segs = []
    STREAM_COUNT = 8
    for i in range(STREAM_COUNT):
        gu, g = add_proj_chain_link(
            b, f"stream_link{i+1}", link_radius=0.30, thickness=0.10,
            face_factory=F0,
        )
        # Initial z spread along the beam
        zc = -BEAM_HALF_Z + (i / STREAM_COUNT) * BEAM_LENGTH
        # Slight Y offset to put them inside beam centre
        g["origin"] = [0, 0, zc]
        # Orient link cross-axis (links flat to beam)
        g["rotation"] = [0, 0, 90]
        stream_segs.append((gu, g, i, zc))

    # =========================================================
    # BEAM EDGE OVERFLOW — 4 thin slabs slightly larger than beam
    # =========================================================
    edge_segs = []
    edge_specs = [
        # (x_offset, y_offset, rot_z)
        ( 1.20, 0.0, 0),
        (-1.20, 0.0, 0),
        (0.0,  0.65, 0),
        (0.0, -0.65, 0),
    ]
    for i, (ex, ey, rz) in enumerate(edge_specs):
        # Edges are very thin slabs
        if abs(ex) > 0.01:
            cu = b.add_cube(
                f"edge_v{i+1}",
                from_=[-0.05, -0.50, -BEAM_HALF_Z],
                to_=[0.05, 0.50, BEAM_HALF_Z],
                faces=F0(),
            )
        else:
            cu = b.add_cube(
                f"edge_h{i+1}",
                from_=[-1.25, -0.04, -BEAM_HALF_Z],
                to_=[1.25, 0.04, BEAM_HALF_Z],
                faces=F0(),
            )
        gu = make_uuid()
        g = b.make_group(
            f"edge_{i+1}", [ex, ey, 0], [cu],
            rotation=[0, 0, rz], group_uuid=gu,
        )
        edge_segs.append((gu, g))

    # =========================================================
    # EMITTER — 6-slab compressed burst at beam origin (z=-BEAM_HALF_Z)
    # =========================================================
    emitter_segs = []
    for i in range(6):
        ang = (i / 6.0) * 2 * math.pi
        cu = b.add_cube(
            f"emit{i+1}",
            from_=[-0.4, -0.05, -0.4], to_=[0.4, 0.05, 0.4],
            faces=F1(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"emitter_{i+1}", [0, 0, -BEAM_HALF_Z], [cu],
            rotation=[
                math.degrees(ang) * 0.5,
                math.degrees(ang),
                math.degrees(ang) * 0.3,
            ],
            group_uuid=gu,
        )
        emitter_segs.append((gu, g, i))

    # =========================================================
    # IMPACT BURST — 8 fan slabs at beam endpoint (z=+BEAM_HALF_Z)
    # Chain-link shaped (small ovals)
    # =========================================================
    impact_segs = []
    for i in range(8):
        ang = (i / 8.0) * 2 * math.pi
        # Position offset radially from impact centre
        rx = math.cos(ang) * 0.8
        ry = math.sin(ang) * 0.8
        cu = b.add_cube(
            f"impact{i+1}",
            from_=[-0.5, -0.06, -0.25], to_=[0.5, 0.06, 0.25],
            faces=F1(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"impact_{i+1}", [rx, ry, BEAM_HALF_Z + 0.5], [cu],
            rotation=[0, math.degrees(ang), math.degrees(ang) * 0.5],
            group_uuid=gu,
        )
        impact_segs.append((gu, g, ang))

    # =========================================================
    # BIND RINGS — 3 flat rings at impact (at Y=0, expanding)
    # =========================================================
    bind_segs = []
    bind_ring_radii = [1.0, 1.6, 2.2]
    for i, r in enumerate(bind_ring_radii):
        cu = b.add_cube(
            f"bind_ring{i+1}",
            from_=[-r, -0.04, -r], to_=[r, 0.04, r],
            faces=F1(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"bind_ring_{i+1}", [0, -0.1, BEAM_HALF_Z + 0.5], [cu],
            group_uuid=gu,
        )
        bind_segs.append((gu, g, i, r))

    # =========================================================
    # ROOT
    # =========================================================
    children = []
    children += [g for (_u, g) in beam_segs]
    children += [g for (_u, g, _i, _z) in stream_segs]
    children += [g for (_u, g) in edge_segs]
    children += [g for (_u, g, _i) in emitter_segs]
    children += [g for (_u, g, _a) in impact_segs]
    children += [g for (_u, g, _i, _r) in bind_segs]

    root_uuid = make_uuid()
    root = b.make_group("root", [0, 0, 0], children, group_uuid=root_uuid)
    b.outliner.append(root)

    # =========================================================
    # SPAWN (0.50s) — emitter at 0, beam extends from emitter, links appear, edge, impact, rings
    # =========================================================
    spawn = {}
    # Emitter opens at 0.0s — sudden burst
    for i, (gu, g, _) in enumerate(emitter_segs):
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", 0.05, x=1.3, y=1.3, z=1.3),
            make_keyframe("scale", 0.10, x=1.0, y=1.0, z=1.0),
        ]}
    # Beam layers extend from emitter outward (z-scale grows from 0)
    for i, (gu, g) in enumerate(beam_segs):
        t_app = 0.04 + i * 0.015
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=0),
            make_keyframe("scale", t_app, x=1, y=1, z=0),
            make_keyframe("scale", t_app + 0.12, x=1, y=1, z=1),
            make_keyframe("scale", 0.50, x=1, y=1, z=1),
        ]}
    # Chain link stream appears at 0.20s
    for gu, g, i, zc in stream_segs:
        t_app = 0.20 + i * 0.012
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", max(0, t_app - 0.001), x=0, y=0, z=0),
            make_keyframe("scale", t_app + 0.06, x=1.0, y=1.0, z=1.0),
            make_keyframe("scale", 0.50, x=1.0, y=1.0, z=1.0),
        ]}
    # Edge slabs materialise alongside chain stream
    for i, (gu, g) in enumerate(edge_segs):
        t_app = 0.18 + i * 0.02
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", max(0, t_app - 0.001), x=0, y=0, z=0),
            make_keyframe("scale", t_app + 0.05, x=1.0, y=1.0, z=1.0),
        ]}
    # Impact burst opens at 0.40s
    for i, (gu, g, ang) in enumerate(impact_segs):
        t_app = 0.40 + i * 0.005
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", max(0, t_app - 0.001), x=0, y=0, z=0),
            make_keyframe("scale", t_app, x=0.1, y=0.1, z=0.1),
            make_keyframe("scale", t_app + 0.04, x=1.2, y=1.2, z=1.2),
            make_keyframe("scale", t_app + 0.08, x=1.0, y=1.0, z=1.0),
            # Fan position
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.50, x=0, y=0, z=0),
        ]}
    # Bind rings expand at 0.40s
    for gu, g, i, r in bind_segs:
        t_app = 0.40 + i * 0.025
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=1, z=0),
            make_keyframe("scale", max(0, t_app - 0.001), x=0, y=1, z=0),
            make_keyframe("scale", t_app + 0.05, x=1.2, y=1, z=1.2),
            make_keyframe("scale", 0.50, x=1.0, y=1, z=1.0),
        ]}
    b.add_animation("spawn", length=0.50, animators=spawn, loop="once", override=True)

    # =========================================================
    # IDLE (2.0s) — beam turbulence, chain stream flows, edges pulse, rings rotate
    # =========================================================
    idle = {}
    # Beam layers oscillate slightly on Y (turbulence)
    for i, (gu, g) in enumerate(beam_segs):
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_pos_kfs(2.0, 24, amp_x=0.0, amp_y=0.06, amp_z=0.0,
                                              period=0.4, phase=i * 0.15) +
                                  osc_scale_kfs(2.0, 20, base=1.0, amp=0.06,
                                                period=0.6, phase=i * 0.20)}
    # Chain link stream: looping Z drift from emitter toward impact
    # Each link offset in phase so the stream looks continuous
    for gu, g, i, zc in stream_segs:
        z_init = -BEAM_HALF_Z + (i / STREAM_COUNT) * BEAM_LENGTH
        # Subtract the current origin offset so stream_flow_kfs gives us the delta
        kfs_pos = stream_flow_kfs(2.0, 30,
                                   z_start=-BEAM_HALF_Z - zc,
                                   z_end=BEAM_HALF_Z - zc,
                                   period=2.0,
                                   phase=-i / STREAM_COUNT)
        kfs_rot = osc_rot_kfs(2.0, 20, amp_x=15, amp_y=15, amp_z=8,
                               period=1.0, phase=i * 0.20)
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs_pos + kfs_rot}
    # Edges pulse
    for i, (gu, g) in enumerate(edge_segs):
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_scale_kfs(2.0, 20, base=1.0, amp=0.30,
                                                period=0.5, phase=i * 0.25)}
    # Emitter pulses
    for gu, g, i in emitter_segs:
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_scale_kfs(2.0, 20, base=1.0, amp=0.20,
                                                period=0.4, phase=i * 0.20) +
                                  osc_rot_kfs(2.0, 20, amp_x=0, amp_y=10, amp_z=0,
                                               period=2.0, phase=i * 0.30)}
    # Impact burst pulses
    for i, (gu, g, ang) in enumerate(impact_segs):
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_scale_kfs(2.0, 20, base=1.0, amp=0.25,
                                                period=0.6, phase=i * 0.18)}
    # Bind rings ROTATE and pulse
    for gu, g, i, r in bind_segs:
        spin_dir = 1 if i % 2 == 0 else -1
        kfs_rot = []
        for k in range(25):
            t = (k / 24) * 2.0
            kfs_rot.append(make_keyframe(
                "rotation", t,
                x=0, y=spin_dir * 360 * (t / 2.0) * (1 + i * 0.3), z=0,
            ))
        kfs_scale = osc_scale_kfs(2.0, 20, base=1.0, amp=0.12,
                                   period=1.0, phase=i * 0.3)
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs_rot + kfs_scale}
    b.add_animation("idle", length=2.0, animators=idle, loop="loop", override=False)

    # =========================================================
    # DISSIPATE (0.40s) — beam scales 0 from impact backward, stream disperses, impact snaps, rings contract
    # =========================================================
    diss = {}
    # Beam layers scale z to 0 — from impact (z=+) collapsing back toward emitter
    for i, (gu, g) in enumerate(beam_segs):
        # Beam contracts on z-axis from impact end first
        # We don't have one-sided scale, so just collapse symmetrically with delay
        t_diss = 0.05 + i * 0.02
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", t_diss, x=1, y=1, z=1),
            make_keyframe("scale", 0.30, x=1, y=1, z=0.0),
            make_keyframe("scale", 0.40, x=0, y=0, z=0),
        ]}
    # Chain stream continues and disperses
    for gu, g, i, zc in stream_segs:
        ang = (i / STREAM_COUNT) * 2 * math.pi
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.40,
                           x=math.cos(ang) * 3,
                           y=math.sin(ang * 0.7) * 2,
                           z=4),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.40, x=0, y=0, z=0),
        ]}
    # Edges fade
    for i, (gu, g) in enumerate(edge_segs):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.40, x=0, y=0, z=0),
        ]}
    # Emitter collapses inward
    for gu, g, i in emitter_segs:
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.15, x=0.4, y=0.4, z=0.4),
            make_keyframe("scale", 0.40, x=0, y=0, z=0),
        ]}
    # Impact burst expands and snaps
    for i, (gu, g, ang) in enumerate(impact_segs):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.20,
                           x=math.cos(ang) * 1.5,
                           y=math.sin(ang * 0.7) * 1.0,
                           z=0.5),
            make_keyframe("position", 0.40,
                           x=math.cos(ang) * 4,
                           y=math.sin(ang * 0.7) * 2.5,
                           z=1.0),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.18, x=1.8, y=1.8, z=1.8),
            make_keyframe("scale", 0.40, x=0, y=0, z=0),
        ]}
    # Bind rings contract
    for gu, g, i, r in bind_segs:
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.40, x=0, y=1, z=0),
        ]}
    b.add_animation("dissipate", length=0.40, animators=diss, loop="once", override=True)

    b.write(OUTPUT)
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    build()
