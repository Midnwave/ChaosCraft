#!/usr/bin/env python3
"""Generator: flail_projectile.bbmodel — Chain attack 11 'Knucklehead'.

A heavy iron flail head on a length of chain, spinning rapidly as it flies.
The chain WHIPS behind with secondary motion (each link oscillates with phase
delay from the head). 4 impact-wake slabs trail the chain. A leather grip
handle sits at the far tail.

Uniqueness vs FreezingIce/Fluffy projectiles:
  - Spiked sphere (6 overlapping cubes + 8 spike pairs) — NOT a pointed shaft (cryo_lance)
  - Trailing 7-link CHAIN body — NOT a smooth head+tail (frozen_comet_me)
  - Phased chain whip animation — distinct from formation drift / orbital
  - Battle-iron blood-stained palette — distinct from sub-zero blues / pastels
"""

import sys
import math

sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder, make_uuid, make_keyframe, uniform_face,
)
from _chain_proj_helpers import (
    tex_battle_iron, tex_leather_wake,
    add_proj_chain_link,
    fast_spin_kfs, chain_whip_kfs, osc_rot_kfs, osc_scale_kfs, osc_pos_kfs,
)


OUTPUT = "D:/CC/ChaosCraft/src/main/resources/models/chain/me_attacks/flail_projectile.bbmodel"
RES = 128


def build():
    b = Builder("flail_projectile", resolution=(RES, RES), visible_box=(14, 6, 0))
    b.add_texture("battle_iron", tex_battle_iron(RES, RES))
    b.add_texture("leather_wake", tex_leather_wake(RES, RES))

    F0 = lambda: uniform_face(0, 0, RES, RES, tex_index=0)
    F1 = lambda: uniform_face(0, 0, RES, RES, tex_index=1)

    # =========================================================
    # FLAIL HEAD — 6 overlapping cubes forming a heavy iron sphere
    # =========================================================
    head_specs = [
        # (from, to, rotation, name)
        ((-1.10, -0.70, -0.60), (1.10, 0.90, 0.80),  (8, 12, -5),  "head_a"),
        ((-1.00,  0.20, -0.95), (1.30, 1.20, 0.55),  (-7, 18, 6),  "head_b"),
        ((-1.30, -0.45,  0.10), (0.50, 1.10, 1.10),  (12, -5, 10), "head_c"),
        (( 0.20, -0.95, -0.55), (1.20, 0.55, 0.95),  (-4, 24, -8), "head_d"),
        ((-0.80, -1.05, -0.40), (0.95, 0.30, 0.70),  (16, -12, 4), "head_e"),
        ((-0.55,  0.35, -1.00), (0.85, 0.90, 0.40),  (-9, 6, 18),  "head_f"),
    ]
    head_segs = []  # (group_uuid, group_dict)
    for fr, to, rot, name in head_specs:
        cu = b.add_cube(name, from_=list(fr), to_=list(to), faces=F0())
        gu = make_uuid()
        g = b.make_group(f"{name}_grp", [0, 0, 0], [cu], rotation=list(rot), group_uuid=gu)
        head_segs.append((gu, g))

    # =========================================================
    # SPIKES — 8 spike pairs (each spike = 2 tapered cubes)
    # spikes protrude in 8 directions from the head surface
    # =========================================================
    spike_dirs = [
        (0, 1, 0), (0, -1, 0), (1, 0, 0), (-1, 0, 0),
        (0, 0, 1), (0, 0, -1),
        (0.7, 0.7, 0), (-0.7, -0.7, 0),
    ]
    spike_segs = []
    SPIKE_LEN1 = 0.65
    SPIKE_LEN2 = 0.45
    for i, (dx, dy, dz) in enumerate(spike_dirs):
        # Compute rotation to align local -Z (or +Y) with this direction
        # Use simple yaw+pitch decomposition
        yaw = math.degrees(math.atan2(dz, dx)) if (dx or dz) else 0
        pitch = math.degrees(math.atan2(dy, math.sqrt(dx * dx + dz * dz + 0.0001)))
        # Base spike cube (wider)
        cu_a = b.add_cube(
            f"spike{i+1}_base",
            from_=[-0.28, 0.0, -0.28], to_=[0.28, SPIKE_LEN1, 0.28],
            faces=F0(),
        )
        # Tip spike cube (narrower, on top)
        cu_b = b.add_cube(
            f"spike{i+1}_tip",
            from_=[-0.16, SPIKE_LEN1 - 0.05, -0.16],
            to_=[0.16, SPIKE_LEN1 + SPIKE_LEN2, 0.16],
            faces=F0(),
        )
        # Group origin at sphere surface in the spike direction
        sx, sy, sz = dx * 0.95, dy * 0.95, dz * 0.95
        gu = make_uuid()
        # Local +Y of group points in spike direction
        # Aim with pitch around X axis, yaw around Y axis
        g = b.make_group(
            f"spike_{i+1}", [sx, sy, sz], [cu_a, cu_b],
            rotation=[pitch, yaw, 0], group_uuid=gu,
        )
        spike_segs.append((gu, g))

    # =========================================================
    # CHAIN BODY — 7 chain link bone groups trailing in arc behind head
    # =========================================================
    chain_segs = []  # (group_uuid, group_dict, link_index)
    CHAIN_COUNT = 7
    for i in range(CHAIN_COUNT):
        gu, g = add_proj_chain_link(
            b, f"chain{i+1}", link_radius=0.42, thickness=0.13,
            face_factory=F0,
        )
        # Place along arc behind head (head at z=0 forward; chain trails -z)
        # Arc curves slightly upward as it goes back (gravity opposite — being whipped)
        zc = -(1.5 + i * 1.05)
        yc = math.sin(i * 0.5) * 0.35
        xc = math.cos(i * 0.3) * 0.20
        # Alternate link orientation 90° (vertical vs horizontal links)
        link_rot_z = 90 if i % 2 == 1 else 0
        g["origin"] = [xc, yc, zc]
        g["rotation"] = [0, 0, link_rot_z]
        chain_segs.append((gu, g, i))

    # =========================================================
    # HANDLE GRIP — 2 flat slabs at far trailing end (leather)
    # =========================================================
    handle_cu_a = b.add_cube(
        "handle_a",
        from_=[-0.35, -0.10, -0.95], to_=[0.35, 0.10, 0.40],
        faces=F1(),
    )
    handle_cu_b = b.add_cube(
        "handle_b",
        from_=[-0.30, -0.08, -0.50], to_=[0.30, 0.08, 0.55],
        faces=F1(),
    )
    handle_gu = make_uuid()
    handle_grp = b.make_group(
        "handle_grp",
        [0, 0, -(1.5 + (CHAIN_COUNT - 1) * 1.05 + 0.85)],
        [handle_cu_a, handle_cu_b],
        rotation=[6, 0, 0],
        group_uuid=handle_gu,
    )

    # =========================================================
    # IMPACT WAKE — 4 flat slabs trailing the chain arc (air displacement)
    # =========================================================
    wake_segs = []
    for i in range(4):
        # Place wake behind chain at sloped Y above
        zc = -(2.0 + i * 1.6)
        yc = 0.6 + i * 0.35
        cu = b.add_cube(
            f"wake{i+1}",
            from_=[-0.55 - i * 0.10, -0.04, -0.85 - i * 0.20],
            to_=[0.55 + i * 0.10, 0.04, 0.85 + i * 0.20],
            faces=F1(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"wake_{i+1}", [0, yc, zc], [cu],
            rotation=[10 + i * 6, math.sin(i * 0.7) * 18, 0],
            group_uuid=gu,
        )
        wake_segs.append((gu, g))

    # =========================================================
    # ROOT
    # =========================================================
    head_grp_uuid = make_uuid()
    head_root = b.make_group(
        "head_root", [0, 0, 0],
        [g for (_u, g) in head_segs + spike_segs],
        group_uuid=head_grp_uuid,
    )
    children = [head_root]
    children += [g for (_u, g, _i) in chain_segs]
    children += [handle_grp]
    children += [g for (_u, g) in wake_segs]

    root_uuid = make_uuid()
    root = b.make_group("root", [0, 0, 0], children, group_uuid=root_uuid)
    b.outliner.append(root)

    # =========================================================
    # SPAWN (0.30s) — ball materialises spinning, then chain sequentially, then handle, then wakes
    # =========================================================
    spawn = {}
    # Head segments materialise FIRST (0.0-0.05s) already spinning
    for i, (gu, g) in enumerate(head_segs):
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", 0.02, x=0.05, y=0.05, z=0.05),
            make_keyframe("scale", 0.05, x=1.15, y=1.15, z=1.15),
            make_keyframe("scale", 0.08, x=1.0, y=1.0, z=1.0),
        ]}
    # Head root rotation (already spinning at spawn)
    spawn[head_grp_uuid] = {"name": "head_root", "type": "bone", "keyframes": [
        make_keyframe("rotation", 0.0, x=0, y=0, z=0),
        make_keyframe("rotation", 0.30, x=120, y=200, z=80),
    ]}
    # Spikes materialise sequentially right after head
    for i, (gu, g) in enumerate(spike_segs):
        t_app = 0.03 + i * 0.005
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", max(0, t_app - 0.001), x=0, y=0, z=0),
            make_keyframe("scale", t_app, x=0.05, y=0.05, z=0.05),
            make_keyframe("scale", t_app + 0.04, x=1.1, y=1.1, z=1.1),
            make_keyframe("scale", t_app + 0.06, x=1.0, y=1.0, z=1.0),
        ]}
    # Chain links materialise SEQUENTIALLY from ball outward (0.08s onward)
    for gu, g, i in chain_segs:
        t_app = 0.08 + i * 0.025
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", max(0, t_app - 0.001), x=0, y=0, z=0),
            make_keyframe("scale", t_app, x=0.05, y=0.05, z=0.05),
            make_keyframe("scale", t_app + 0.05, x=1.0, y=1.0, z=1.0),
            make_keyframe("scale", 0.30, x=1.0, y=1.0, z=1.0),
        ]}
    # Handle at the very end
    spawn[handle_gu] = {"name": "handle_grp", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, x=0, y=0, z=0),
        make_keyframe("scale", 0.27, x=0, y=0, z=0),
        make_keyframe("scale", 0.30, x=1.0, y=1.0, z=1.0),
    ]}
    # Wakes appear after chain
    for i, (gu, g) in enumerate(wake_segs):
        t_app = 0.20 + i * 0.02
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", max(0, t_app - 0.001), x=0, y=0, z=0),
            make_keyframe("scale", t_app + 0.05, x=1.0, y=1.0, z=1.0),
        ]}
    b.add_animation("spawn", length=0.30, animators=spawn, loop="once", override=True)

    # =========================================================
    # IDLE (2.0s) — rapid spin on all 3 axes, chain whips with phase delay
    # =========================================================
    idle = {}
    # Head root: fast spin (3 axes)
    idle[head_grp_uuid] = {"name": "head_root", "type": "bone",
                            "keyframes": fast_spin_kfs(2.0, 24, rps_x=4.0, rps_y=6.0, rps_z=3.0)}
    # Individual head segments wobble slightly
    for i, (gu, g) in enumerate(head_segs):
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_rot_kfs(2.0, 18, amp_x=3, amp_y=4, amp_z=2,
                                              period=1.5, phase=i * 0.3)}
    # Spikes individually wobble slightly relative to head
    for i, (gu, g) in enumerate(spike_segs):
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_rot_kfs(2.0, 16, amp_x=2, amp_y=2, amp_z=2,
                                              period=2.0, phase=i * 0.25)}
    # Chain links: phased whip motion — phase delay increases down chain from head
    for gu, g, i in chain_segs:
        # Amplitude increases with distance from head (whip gets bigger at the tail)
        amp = 0.20 + i * 0.10
        kfs_pos = chain_whip_kfs(2.0, 24, amp_x=amp, amp_y=amp * 1.2, amp_z=amp * 0.5,
                                  period=2.0, phase=-i * 0.20)
        kfs_rot = osc_rot_kfs(2.0, 20, amp_x=6 + i * 2, amp_y=4, amp_z=6 + i * 2,
                               period=2.0, phase=-i * 0.18)
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs_pos + kfs_rot}
    # Handle has MOST lag — largest amplitude oscillation
    idle[handle_gu] = {"name": "handle_grp", "type": "bone",
                       "keyframes": chain_whip_kfs(2.0, 24, amp_x=0.7, amp_y=0.85, amp_z=0.5,
                                                    period=2.0, phase=-1.4) +
                                    osc_rot_kfs(2.0, 20, amp_x=12, amp_y=8, amp_z=14,
                                                period=2.0, phase=-1.4)}
    # Wakes follow chain arc
    for i, (gu, g) in enumerate(wake_segs):
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_pos_kfs(2.0, 18, amp_x=0.15, amp_y=0.25, amp_z=0.30,
                                              period=2.0, phase=-i * 0.30) +
                                  osc_scale_kfs(2.0, 18, base=1.0, amp=0.20,
                                                period=2.0, phase=-i * 0.30)}
    b.add_animation("idle", length=2.0, animators=idle, loop="loop", override=False)

    # =========================================================
    # DISSIPATE (0.35s) — ball explodes, spikes scatter, chain snaps taut then backward
    # =========================================================
    diss = {}
    # Head segments fly outward in different directions
    for i, (gu, g) in enumerate(head_segs):
        ang = (i / len(head_specs)) * 2 * math.pi
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.35,
                           x=math.cos(ang) * 5,
                           y=math.sin(ang * 0.7) * 4,
                           z=math.sin(ang) * 5),
            make_keyframe("rotation", 0.0, x=0, y=0, z=0),
            make_keyframe("rotation", 0.35, x=360 + i * 60, y=420 + i * 80, z=240 + i * 40),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.20, x=1.3, y=1.3, z=1.3),
            make_keyframe("scale", 0.35, x=0, y=0, z=0),
        ]}
    # Spikes scatter outward fast
    for i, (gu, g) in enumerate(spike_segs):
        ang = (i / len(spike_dirs)) * 2 * math.pi
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.35,
                           x=math.cos(ang) * 7,
                           y=math.sin(ang * 0.6) * 5,
                           z=math.sin(ang) * 7),
            make_keyframe("rotation", 0.0, x=0, y=0, z=0),
            make_keyframe("rotation", 0.35, x=720, y=540 + i * 30, z=360),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.35, x=0, y=0, z=0),
        ]}
    # Chain snaps taut (straightens) then flies backward
    for gu, g, i in chain_segs:
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            # First: snap taut — go to perfectly aligned position
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.10, x=0, y=0, z=0),
            # Then: fly backward
            make_keyframe("position", 0.35, x=0, y=-1 - i * 0.3, z=-3 - i * 1.5),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.30, x=1, y=1, z=1),
            make_keyframe("scale", 0.35, x=0, y=0, z=0),
        ]}
    # Handle flies backward
    diss[handle_gu] = {"name": "handle_grp", "type": "bone", "keyframes": [
        make_keyframe("position", 0.0, x=0, y=0, z=0),
        make_keyframe("position", 0.35, x=0, y=-3, z=-8),
        make_keyframe("rotation", 0.0, x=0, y=0, z=0),
        make_keyframe("rotation", 0.35, x=180, y=90, z=0),
        make_keyframe("scale", 0.0, x=1, y=1, z=1),
        make_keyframe("scale", 0.35, x=0, y=0, z=0),
    ]}
    # Wakes burst outward
    for i, (gu, g) in enumerate(wake_segs):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.18, x=2.5, y=1, z=2.5),
            make_keyframe("scale", 0.35, x=0, y=0, z=0),
        ]}
    b.add_animation("dissipate", length=0.35, animators=diss, loop="once", override=True)

    b.write(OUTPUT)
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    build()
