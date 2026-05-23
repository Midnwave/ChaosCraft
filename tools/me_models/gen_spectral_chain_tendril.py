#!/usr/bin/env python3
"""Generator: spectral_chain_tendril.bbmodel — Chain attack 15 'Reaching'.

A spectral chain that REACHES outward like a grasping tendril, each link
forming ahead of the previous one. Biological/alive — purple emissive at
claw tip. 10 main-tendril links along curved path, 3-piece claw structure
at tip, 3 secondary 3-link branches, origin anchor at base.

Uniqueness vs FreezingIce/Fluffy:
  - Sequential GROWTH (links materialise one-by-one) — unique spawn choreography
  - CLAW at tip (3-piece grasping structure) — distinct geometry
  - SECONDARY BRANCHES splitting from main — unique fork topology
  - REVERSE RETRACTION on dissipate (tip-to-origin vanishing) — unique exit
  - Living purple-spectral palette — distinct from cold blues / pastels
"""

import sys
import math

sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder, make_uuid, make_keyframe, uniform_face,
)
from _chain_proj_helpers import (
    tex_reaching_chain, tex_reaching_anchor,
    add_proj_chain_link,
    osc_scale_kfs, osc_rot_kfs, osc_pos_kfs, link_growth_kfs,
)


OUTPUT = "D:/CC/ChaosCraft/src/main/resources/models/chain/me_attacks/spectral_chain_tendril.bbmodel"
RES = 128

SPAWN_DUR = 0.60
IDLE_DUR = 2.5
DISSIPATE_DUR = 0.50


def build():
    b = Builder("spectral_chain_tendril", resolution=(RES, RES), visible_box=(12, 6, 0))
    b.add_texture("reaching_chain", tex_reaching_chain(RES, RES))
    b.add_texture("reaching_anchor", tex_reaching_anchor(RES, RES))

    F0 = lambda: uniform_face(0, 0, RES, RES, tex_index=0)
    F1 = lambda: uniform_face(0, 0, RES, RES, tex_index=1)

    # =========================================================
    # MAIN TENDRIL — 10 chain links along slightly curved path
    # Each offset 0.3 units from perfectly straight to suggest reaching/organic
    # Path goes from origin (z=0) forward to tip (z=+10)
    # =========================================================
    MAIN_COUNT = 10
    main_segs = []  # (group_uuid, group_dict, idx, position)
    for i in range(MAIN_COUNT):
        gu, g = add_proj_chain_link(
            b, f"main_link_{i+1}", link_radius=0.42, thickness=0.13,
            face_factory=F0,
        )
        # Position along curved path
        zc = 0.5 + i * 1.05
        # Organic curve — alternating sine offsets
        xc = math.sin(i * 0.55) * 0.30
        yc = math.cos(i * 0.45) * 0.25 + math.sin(i * 0.25) * 0.15
        # Link orientation curves through path
        link_rot_x = math.degrees(math.sin(i * 0.3)) * 12
        link_rot_y = math.degrees(math.cos(i * 0.3)) * 8
        link_rot_z = (i * 35) % 360  # alternate planes for chain link geometry
        g["origin"] = [xc, yc, zc]
        g["rotation"] = [link_rot_x, link_rot_y, link_rot_z]
        main_segs.append((gu, g, i, (xc, yc, zc)))

    # =========================================================
    # CLAW STRUCTURE — 3 small bone groups at forward end
    # Form a claw-like grasping structure at z = (last main link z + ~1.2)
    # =========================================================
    last_main_z = 0.5 + (MAIN_COUNT - 1) * 1.05  # ~9.95
    claw_base_z = last_main_z + 1.3
    claw_segs = []
    # 3 claw fingers radiating from claw base
    claw_specs = [
        # (yaw angle of claw "finger", colour identifier)
        (   0,),
        ( 120,),
        (-120,),
    ]
    for i, (yaw_deg,) in enumerate(claw_specs):
        # Each claw finger = 2 cubes (base + tapered tip)
        cu_base = b.add_cube(
            f"claw{i+1}_base",
            from_=[-0.20, -0.10, 0.0], to_=[0.20, 0.10, 0.65],
            faces=F0(),
        )
        cu_tip = b.add_cube(
            f"claw{i+1}_tip",
            from_=[-0.12, -0.06, 0.55], to_=[0.12, 0.06, 1.10],
            faces=F0(),
        )
        gu = make_uuid()
        # Claw fingers angle outward (slight pitch up + yaw)
        g = b.make_group(
            f"claw_finger_{i+1}",
            [0, 0, claw_base_z],
            [cu_base, cu_tip],
            rotation=[-15, yaw_deg, 0],
            group_uuid=gu,
        )
        claw_segs.append((gu, g, i))

    # =========================================================
    # SECONDARY BRANCHES — 3 shorter chain branches splitting from main
    # Each branch = 3 links on its own bone group
    # Parent links: 3, 5, 7 (1-indexed: 4th, 6th, 8th links)
    # =========================================================
    branch_segs = []  # (branch_root_uuid, group_dict, parent_idx, [child_link_groups])
    BRANCH_PARENT_INDICES = [3, 5, 7]  # 0-indexed
    BRANCH_DIRECTIONS = [
        (math.cos(math.radians(60)),  math.sin(math.radians(60)),  0.0),
        (math.cos(math.radians(-50)), math.sin(math.radians(-50)), 0.0),
        (math.cos(math.radians(120)), math.sin(math.radians(120)), 0.0),
    ]
    for b_idx, parent_idx in enumerate(BRANCH_PARENT_INDICES):
        parent_pos = main_segs[parent_idx][3]  # (x, y, z) of parent link
        dx, dy, dz = BRANCH_DIRECTIONS[b_idx]
        branch_link_groups = []
        for l in range(3):
            gu_link, link_g = add_proj_chain_link(
                b, f"branch{b_idx+1}_link_{l+1}",
                link_radius=0.30, thickness=0.10, face_factory=F0,
            )
            # Branch extends from parent link in (dx, dy, dz) direction
            step = (l + 1) * 0.85
            bxc = parent_pos[0] + dx * step
            byc = parent_pos[1] + dy * step
            bzc = parent_pos[2] + dz * step + l * 0.15
            link_g["origin"] = [bxc, byc, bzc]
            link_g["rotation"] = [math.degrees(dy) * 15, math.degrees(dx) * 25, l * 40]
            branch_link_groups.append((gu_link, link_g, l))
        # We don't need a separate root for the branch — each link
        # is its own bone group in the outliner
        branch_segs.append((b_idx, parent_idx, branch_link_groups))

    # =========================================================
    # ORIGIN ANCHOR — dense 3-cube compressed knot at base (z=0)
    # =========================================================
    anchor_specs = [
        ((-0.55, -0.45, -0.45), (0.55, 0.50, 0.55), (0, 0, 0)),
        ((-0.35, -0.65, -0.30), (0.35, 0.35, 0.30), (15, 25, -10)),
        ((-0.45, -0.25, -0.55), (0.30, 0.55, 0.20), (-12, -18, 20)),
    ]
    anchor_segs = []
    for i, (fr, to, rot) in enumerate(anchor_specs):
        cu = b.add_cube(f"anchor_{i+1}", from_=list(fr), to_=list(to), faces=F1())
        gu = make_uuid()
        g = b.make_group(f"anchor_grp_{i+1}", [0, 0, 0], [cu],
                          rotation=list(rot), group_uuid=gu)
        anchor_segs.append((gu, g))

    # Combine anchor segments into a single anchor root
    anchor_root_uuid = make_uuid()
    anchor_root = b.make_group(
        "anchor_root", [0, 0, 0], [g for (_u, g) in anchor_segs],
        group_uuid=anchor_root_uuid,
    )

    # =========================================================
    # ROOT
    # =========================================================
    children = [anchor_root]
    children += [g for (_u, g, _i, _p) in main_segs]
    children += [g for (_u, g, _i) in claw_segs]
    for b_idx, parent_idx, branch_links in branch_segs:
        children += [link_g for (_gu, link_g, _l) in branch_links]

    root_uuid = make_uuid()
    root = b.make_group("root", [0, 0, 0], children, group_uuid=root_uuid)
    b.outliner.append(root)

    # =========================================================
    # SPAWN (0.60s) — GROWING: anchor at 0.0s, then each link sequentially
    # from origin outward, branches sprout off their parent links, claw at end.
    # =========================================================
    spawn = {}
    # Anchor first
    spawn[anchor_root_uuid] = {"name": "anchor_root", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, x=0, y=0, z=0),
        make_keyframe("scale", 0.05, x=1.3, y=1.3, z=1.3),
        make_keyframe("scale", 0.10, x=1.0, y=1.0, z=1.0),
    ]}
    for i, (gu, g) in enumerate(anchor_segs):
        spawn[gu] = {"name": g["name"], "type": "bone",
                     "keyframes": osc_scale_kfs(SPAWN_DUR, 12, base=1.0, amp=0.15,
                                                  period=0.3, phase=i * 0.1)}
    # Main tendril links grow one-by-one: 0.05s per link
    LINK_DELAY = 0.05
    for gu, g, i, _p in main_segs:
        t_start = 0.10 + i * LINK_DELAY
        t_grow = 0.08
        spawn[gu] = {"name": g["name"], "type": "bone",
                     "keyframes": link_growth_kfs(SPAWN_DUR, t_start, t_grow)}
    # Branches grow as their parent link forms — slight delay after parent
    for b_idx, parent_idx, branch_links in branch_segs:
        parent_t_start = 0.10 + parent_idx * LINK_DELAY
        for (gu_link, link_g, l) in branch_links:
            # Each branch link 0.04s after the previous, starting just after parent forms
            t_start = parent_t_start + 0.05 + l * 0.04
            spawn[gu_link] = {"name": link_g["name"], "type": "bone",
                              "keyframes": link_growth_kfs(SPAWN_DUR, t_start, 0.06)}
    # Claw materialises when last main link forms (~0.55s)
    claw_t_start = 0.10 + (MAIN_COUNT - 1) * LINK_DELAY + 0.05
    for gu, g, i in claw_segs:
        t_start = claw_t_start + i * 0.015
        spawn[gu] = {"name": g["name"], "type": "bone",
                     "keyframes": link_growth_kfs(SPAWN_DUR, t_start, 0.05)}
    b.add_animation("spawn", length=SPAWN_DUR, animators=spawn, loop="once", override=True)

    # =========================================================
    # IDLE (2.5s) — claw opens/closes (reaching), links phase-undulate from origin,
    # branches sway with own periods, anchor pulses
    # =========================================================
    idle = {}
    # Anchor pulses
    idle[anchor_root_uuid] = {"name": "anchor_root", "type": "bone",
                              "keyframes": osc_scale_kfs(IDLE_DUR, 24, base=1.0, amp=0.18,
                                                          period=0.8, phase=0.0)}
    for i, (gu, g) in enumerate(anchor_segs):
        idle[gu] = {"name": g["name"], "type": "bone",
                    "keyframes": osc_rot_kfs(IDLE_DUR, 20,
                                              amp_x=10, amp_y=12, amp_z=8,
                                              period=1.2, phase=i * 0.3) +
                                  osc_scale_kfs(IDLE_DUR, 18, base=1.0, amp=0.10,
                                                period=0.6, phase=i * 0.2)}
    # Main tendril links undulate (phase delay from origin)
    for gu, g, i, _p in main_segs:
        # Undulation amplitude builds toward the tip
        amp = 0.10 + i * 0.04
        kfs_pos = osc_pos_kfs(IDLE_DUR, 24,
                              amp_x=amp * 0.8, amp_y=amp, amp_z=amp * 0.4,
                              period=1.8, phase=-i * 0.25)
        kfs_rot = osc_rot_kfs(IDLE_DUR, 20,
                              amp_x=5 + i, amp_y=4 + i, amp_z=6 + i,
                              period=1.8, phase=-i * 0.25)
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs_pos + kfs_rot}
    # Branch tendrils sway with own periods
    for b_idx, parent_idx, branch_links in branch_segs:
        for (gu_link, link_g, l) in branch_links:
            period = 1.2 + b_idx * 0.3 + l * 0.15
            amp = 0.12 + l * 0.05
            kfs_pos = osc_pos_kfs(IDLE_DUR, 20,
                                  amp_x=amp * 0.8, amp_y=amp,
                                  amp_z=amp * 0.5,
                                  period=period, phase=l * 0.4 + b_idx * 0.6)
            kfs_rot = osc_rot_kfs(IDLE_DUR, 18,
                                  amp_x=12 + l * 2,
                                  amp_y=10,
                                  amp_z=14 + l * 2,
                                  period=period, phase=l * 0.4 + b_idx * 0.6)
            idle[gu_link] = {"name": link_g["name"], "type": "bone",
                             "keyframes": kfs_pos + kfs_rot}
    # Claw opens/closes (reaching motion) — fingers pitch in/out
    for (gu, g, i) in claw_segs:
        # Phase aligned so all 3 fingers open/close together
        # Pitch -15 (current rest) -> -45 (open) -> 0 (closed) -> -15
        kfs = []
        for k in range(25):
            t = (k / 24) * IDLE_DUR
            ph = (t / 1.5) * 2 * math.pi
            pitch_offset = math.sin(ph) * 25
            kfs.append(make_keyframe("rotation", t,
                                      x=-15 + pitch_offset,
                                      y=120 * i + math.cos(ph) * 5,
                                      z=math.sin(ph * 1.3) * 6))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}
    b.add_animation("idle", length=IDLE_DUR, animators=idle, loop="loop", override=False)

    # =========================================================
    # DISSIPATE (0.50s) — claw reaches MAX extension, then chain retracts
    # from TIP to ORIGIN (each link vanishes in reverse order), origin collapses last
    # =========================================================
    diss = {}
    # Claw reaches max extension first then vanishes
    for (gu, g, i) in claw_segs:
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("rotation", 0.0, x=-15, y=120 * i, z=0),
            make_keyframe("rotation", 0.08, x=-50, y=120 * i, z=0),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.08, x=1.3, y=1.3, z=1.3),
            make_keyframe("scale", 0.15, x=0, y=0, z=0),
        ]}
    # Main tendril links retract from TIP to ORIGIN
    # Total retraction window: 0.10s -> 0.45s = 0.35s
    # Link MAIN_COUNT-1 (tip) vanishes first, link 0 (origin-end) vanishes last
    RETRACT_START = 0.08
    RETRACT_DUR = 0.35
    per_link = RETRACT_DUR / MAIN_COUNT
    for gu, g, i, _p in main_segs:
        # Reverse index: tip i=MAIN_COUNT-1 vanishes at t=RETRACT_START
        # origin i=0 vanishes at t=RETRACT_START + (MAIN_COUNT-1)*per_link
        reverse_i = MAIN_COUNT - 1 - i
        t_vanish_start = RETRACT_START + reverse_i * per_link
        t_vanish_end = t_vanish_start + per_link * 0.7
        # Safety clamp
        t_vanish_end = min(t_vanish_end, DISSIPATE_DUR - 0.001)
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", max(0, t_vanish_start - 0.001), x=1, y=1, z=1),
            make_keyframe("scale", t_vanish_start, x=1.15, y=1.15, z=1.15),
            make_keyframe("scale", t_vanish_end, x=0, y=0, z=0),
            make_keyframe("scale", DISSIPATE_DUR, x=0, y=0, z=0),
        ]}
    # Branches retract before their parent main link vanishes
    for b_idx, parent_idx, branch_links in branch_segs:
        parent_reverse_i = MAIN_COUNT - 1 - parent_idx
        parent_t_vanish = RETRACT_START + parent_reverse_i * per_link
        # Branches retract slightly before parent (tip to base)
        for (gu_link, link_g, l) in branch_links:
            # tip of branch (l=2) vanishes first, base (l=0) last
            branch_reverse_l = 2 - l
            t_vanish_start = max(0.02, parent_t_vanish - 0.10 + branch_reverse_l * 0.025)
            t_vanish_end = min(parent_t_vanish, t_vanish_start + 0.04)
            diss[gu_link] = {"name": link_g["name"], "type": "bone", "keyframes": [
                make_keyframe("scale", 0.0, x=1, y=1, z=1),
                make_keyframe("scale", max(0, t_vanish_start - 0.001), x=1, y=1, z=1),
                make_keyframe("scale", t_vanish_end, x=0, y=0, z=0),
                make_keyframe("scale", DISSIPATE_DUR, x=0, y=0, z=0),
            ]}
    # Origin anchor collapses LAST
    diss[anchor_root_uuid] = {"name": "anchor_root", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, x=1, y=1, z=1),
        make_keyframe("scale", DISSIPATE_DUR - 0.10, x=1.3, y=1.3, z=1.3),
        make_keyframe("scale", DISSIPATE_DUR, x=0, y=0, z=0),
    ]}
    for i, (gu, g) in enumerate(anchor_segs):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("rotation", 0.0, x=0, y=0, z=0),
            make_keyframe("rotation", DISSIPATE_DUR, x=120 + i * 40, y=180 + i * 50, z=90),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", DISSIPATE_DUR, x=0, y=0, z=0),
        ]}
    b.add_animation("dissipate", length=DISSIPATE_DUR, animators=diss, loop="once", override=True)

    b.write(OUTPUT)
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    build()
