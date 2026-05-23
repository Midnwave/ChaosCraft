#!/usr/bin/env python3
"""Generator: spectral_warden_orbital.bbmodel — Chain attack 18 'The Guard'.

4 spectral warden figures orbiting the caster. Each warden has a torso, shoulder
pads, a helmet, arm stubs, and chains hanging from each arm. Wardens face inward
toward the caster. A central authority point + orbit ring tie them together.

UNIQUE vs other modes:
 - Humanoid silhouettes (not bars, not wheels, not anchors)
 - Spectral pale-blue glow (vs warm iron rust, vs cursed purple)
 - Inward-facing rotation in idle — they're watching the caster
 - Wardens fade in spectrally (not snap, not fly-in)
"""

import math
import random
import sys

sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder, make_uuid, make_keyframe, uniform_face, png_from_pixels,
)
from _chain_common import (
    base_fill, jitter_inplace, rust_blotch, horizontal_stress_lines,
    chain_link_silhouette, radial_glow, runic_glyph_ring,
)
from _freezingice_common import (
    specular_blob, random_bright_specks, hex_color,
)


OUTPUT = "D:/CC/ChaosCraft/src/main/resources/models/chain/me_attacks/spectral_warden_orbital.bbmodel"
W = H = 64


# ---------------------------------------------------------------------------
# TEXTURES
# ---------------------------------------------------------------------------

def tex_spectral_warden():
    """Spectral iron armour — pale-blue ghost glow over deep void blue."""
    warden_void   = hex_color("#060810")
    spectral_arm  = hex_color("#141e28")
    ghost_surface = hex_color("#283848")
    edge_bright   = hex_color("#506070")
    emissive      = hex_color("#7090a0")
    pixels = base_fill(W, H, spectral_arm)
    # Vertical gradient: top is brighter ghostly, bottom is deeper
    for y in range(H):
        for x in range(W):
            t = y / float(H - 1)
            r = int(edge_bright[0] + (warden_void[0] - edge_bright[0]) * t)
            g = int(edge_bright[1] + (warden_void[1] - edge_bright[1]) * t)
            bl = int(edge_bright[2] + (warden_void[2] - edge_bright[2]) * t)
            pixels[y * W + x] = (r, g, bl, 255)
    # Rectangular panel lines suggesting armour plating
    for y in (12, 28, 44):
        for x in range(W):
            p = pixels[y * W + x]
            pixels[y * W + x] = (
                min(255, p[0] + (ghost_surface[0] - p[0]) // 2),
                min(255, p[1] + (ghost_surface[1] - p[1]) // 2),
                min(255, p[2] + (ghost_surface[2] - p[2]) // 2),
                255,
            )
    for x in (16, 32, 48):
        for y in range(H):
            p = pixels[y * W + x]
            pixels[y * W + x] = (
                min(255, p[0] + (ghost_surface[0] - p[0]) // 3),
                min(255, p[1] + (ghost_surface[1] - p[1]) // 3),
                min(255, p[2] + (ghost_surface[2] - p[2]) // 3),
                255,
            )
    # Spectral emissive glow blobs
    for cx, cy in [(16, 14), (48, 22), (28, 40), (54, 50), (12, 50)]:
        specular_blob(pixels, W, H, cx, cy, emissive, radius=5, strength=0.75)
    # Ghost edge highlights
    for ang_i in range(0, 360, 8):
        ang = math.radians(ang_i)
        for r in (24, 25):
            x = int(32 + math.cos(ang) * r)
            y = int(32 + math.sin(ang) * r)
            if 0 <= x < W and 0 <= y < H:
                p = pixels[y * W + x]
                pixels[y * W + x] = (
                    min(255, p[0] + (edge_bright[0] - p[0]) // 2),
                    min(255, p[1] + (edge_bright[1] - p[1]) // 2),
                    min(255, p[2] + (edge_bright[2] - p[2]) // 2),
                    255,
                )
    random_bright_specks(pixels, W, H, 70, emissive, seed=19, size_max=1)
    jitter_inplace(pixels, jitter_range=10, seed=23, density=0.85)
    return png_from_pixels(pixels, W, H)


def tex_spectral_authority():
    """Central authority + orbit ring: bright spectral silver center, navy outer."""
    silver_em = hex_color("#90b0c0")
    bright    = hex_color("#c0e0f0")
    deep_navy = hex_color("#080a14")
    mid       = hex_color("#2a3a4c")
    pixels = base_fill(W, H, deep_navy)
    # Radial silver core
    cx, cy = W // 2, H // 2
    for y in range(H):
        for x in range(W):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            max_r = W * 0.55
            t = min(1.0, d / max_r)
            if t < 0.3:
                # bright center
                pixels[y * W + x] = (
                    int(bright[0] + (silver_em[0] - bright[0]) * (t / 0.3)),
                    int(bright[1] + (silver_em[1] - bright[1]) * (t / 0.3)),
                    int(bright[2] + (silver_em[2] - bright[2]) * (t / 0.3)),
                    255,
                )
            elif t < 0.7:
                u = (t - 0.3) / 0.4
                pixels[y * W + x] = (
                    int(silver_em[0] + (mid[0] - silver_em[0]) * u),
                    int(silver_em[1] + (mid[1] - silver_em[1]) * u),
                    int(silver_em[2] + (mid[2] - silver_em[2]) * u),
                    255,
                )
            else:
                u = (t - 0.7) / 0.3
                pixels[y * W + x] = (
                    int(mid[0] + (deep_navy[0] - mid[0]) * u),
                    int(mid[1] + (deep_navy[1] - mid[1]) * u),
                    int(mid[2] + (deep_navy[2] - mid[2]) * u),
                    255,
                )
    # Spectral wisps
    runic_glyph_ring(pixels, W, H, cx, cy, 18, bright, density=0.55, seed=37)
    runic_glyph_ring(pixels, W, H, cx, cy, 24, silver_em, density=0.45, seed=38)
    random_bright_specks(pixels, W, H, 60, bright, seed=41, size_max=1)
    jitter_inplace(pixels, jitter_range=8, seed=47, density=0.7)
    return png_from_pixels(pixels, W, H)


# ---------------------------------------------------------------------------
# BUILD
# ---------------------------------------------------------------------------

def build_warden(b, idx, ang, radius, F0):
    """Build one warden figure as a group rooted at orbit position.

    Returns dict with bone uuids/groups.
    """
    # Warden home position
    cx = math.cos(ang) * radius
    cz = math.sin(ang) * radius
    # All warden cubes are local to the warden group (origin at cx,4,cz)
    # ---- Torso: 2 flat rectangular slab bones ----
    torso_upper = b.add_cube(
        f"warden{idx}_torso_upper",
        from_=[-1.4, 2.0, -0.45], to_=[1.4, 4.6, 0.45],
        faces=F0(),
    )
    torso_lower = b.add_cube(
        f"warden{idx}_torso_lower",
        from_=[-1.2, 0.0, -0.40], to_=[1.2, 2.0, 0.40],
        faces=F0(),
    )
    torso_gu = make_uuid()
    torso_g = b.make_group(
        f"warden{idx}_torso", [0, 0, 0],
        [torso_upper, torso_lower], group_uuid=torso_gu,
    )
    # ---- Shoulder pads: 2 wide flat slab bones at shoulder level ----
    shoulder_L = b.add_cube(
        f"warden{idx}_shoulder_L",
        from_=[-2.2, 4.4, -0.5], to_=[-1.2, 5.0, 0.5],
        faces=F0(),
    )
    shoulder_R = b.add_cube(
        f"warden{idx}_shoulder_R",
        from_=[1.2, 4.4, -0.5], to_=[2.2, 5.0, 0.5],
        faces=F0(),
    )
    shoulder_gu = make_uuid()
    shoulder_g = b.make_group(
        f"warden{idx}_shoulders", [0, 0, 0],
        [shoulder_L, shoulder_R], group_uuid=shoulder_gu,
    )
    # ---- Helmet: 3 cube cluster ----
    helm_core = b.add_cube(
        f"warden{idx}_helm_core",
        from_=[-0.8, 5.2, -0.7], to_=[0.8, 6.4, 0.7],
        faces=F0(),
    )
    helm_top = b.add_cube(
        f"warden{idx}_helm_top",
        from_=[-0.6, 6.4, -0.5], to_=[0.6, 6.8, 0.5],
        faces=F0(),
    )
    helm_visor = b.add_cube(
        f"warden{idx}_helm_visor",
        from_=[-0.8, 5.6, 0.55], to_=[0.8, 6.0, 0.85],
        faces=F0(),
    )
    helm_gu = make_uuid()
    helm_g = b.make_group(
        f"warden{idx}_helmet", [0, 0, 0],
        [helm_core, helm_top, helm_visor], group_uuid=helm_gu,
    )
    # ---- Arm stubs: 2 single cube bones at sides ----
    arm_L = b.add_cube(
        f"warden{idx}_arm_L",
        from_=[-2.0, 2.0, -0.35], to_=[-1.4, 4.4, 0.35],
        faces=F0(),
    )
    arm_L_gu = make_uuid()
    arm_L_g = b.make_group(
        f"warden{idx}_arm_L_grp", [-1.7, 4.4, 0],
        [arm_L], group_uuid=arm_L_gu,
    )
    arm_R = b.add_cube(
        f"warden{idx}_arm_R",
        from_=[1.4, 2.0, -0.35], to_=[2.0, 4.4, 0.35],
        faces=F0(),
    )
    arm_R_gu = make_uuid()
    arm_R_g = b.make_group(
        f"warden{idx}_arm_R_grp", [1.7, 4.4, 0],
        [arm_R], group_uuid=arm_R_gu,
    )
    # ---- Held chains: 3 link cubes hanging from each arm stub ----
    chains_L_cubes = []
    for li in range(3):
        cy = 1.4 - li * 0.7
        if li % 2 == 0:
            f_ = [-2.05, cy - 0.3, -0.20]
            t_ = [-1.55, cy + 0.3, 0.20]
        else:
            f_ = [-1.85, cy - 0.3, -0.40]
            t_ = [-1.65, cy + 0.3, 0.40]
        c = b.add_cube(
            f"warden{idx}_chain_L_link{li+1}",
            from_=f_, to_=t_, faces=F0(),
        )
        chains_L_cubes.append(c)
    chain_L_gu = make_uuid()
    chain_L_g = b.make_group(
        f"warden{idx}_chain_L", [-1.8, 1.8, 0],
        chains_L_cubes, group_uuid=chain_L_gu,
    )
    chains_R_cubes = []
    for li in range(3):
        cy = 1.4 - li * 0.7
        if li % 2 == 0:
            f_ = [1.55, cy - 0.3, -0.20]
            t_ = [2.05, cy + 0.3, 0.20]
        else:
            f_ = [1.65, cy - 0.3, -0.40]
            t_ = [1.85, cy + 0.3, 0.40]
        c = b.add_cube(
            f"warden{idx}_chain_R_link{li+1}",
            from_=f_, to_=t_, faces=F0(),
        )
        chains_R_cubes.append(c)
    chain_R_gu = make_uuid()
    chain_R_g = b.make_group(
        f"warden{idx}_chain_R", [1.8, 1.8, 0],
        chains_R_cubes, group_uuid=chain_R_gu,
    )
    # ---- Warden parent group ----
    warden_gu = make_uuid()
    warden_g = b.make_group(
        f"warden_{idx}", [cx, 0, cz],
        [torso_gu, shoulder_gu, helm_gu, arm_L_gu, arm_R_gu, chain_L_gu, chain_R_gu],
        rotation=[0, -math.degrees(ang), 0],  # face inward toward center
        group_uuid=warden_gu,
    )
    return {
        "warden_gu": warden_gu, "warden_g": warden_g,
        "torso_gu": torso_gu, "torso_g": torso_g,
        "shoulder_gu": shoulder_gu, "shoulder_g": shoulder_g,
        "helm_gu": helm_gu, "helm_g": helm_g,
        "arm_L_gu": arm_L_gu, "arm_L_g": arm_L_g,
        "arm_R_gu": arm_R_gu, "arm_R_g": arm_R_g,
        "chain_L_gu": chain_L_gu, "chain_L_g": chain_L_g,
        "chain_R_gu": chain_R_gu, "chain_R_g": chain_R_g,
        "ang": ang,
    }


def build():
    b = Builder("spectral_warden_orbital", resolution=(W, H), visible_box=(20, 12, 0))
    b.add_texture("spectral_warden", tex_spectral_warden())
    b.add_texture("spectral_authority", tex_spectral_authority())

    F0 = lambda: uniform_face(0, 0, W, H, tex_index=0)
    F1 = lambda: uniform_face(0, 0, W, H, tex_index=1)

    RADIUS = 9.0
    # ---- 4 wardens ----
    wardens = []
    for i in range(4):
        ang = (i / 4.0) * 2 * math.pi
        wardens.append(build_warden(b, i + 1, ang, RADIUS, F0))

    # ---- Central authority point: 3-cube cluster at Y=6 ----
    auth_cubes = []
    auth_positions = [(0, 0, 0), (-0.3, 0.4, 0.3), (0.3, 0.4, -0.3)]
    for ai, (ox, oy, oz) in enumerate(auth_positions):
        c = b.add_cube(
            f"authority_{ai+1}",
            from_=[ox - 0.6, 6 + oy - 0.6, oz - 0.6],
            to_=[ox + 0.6, 6 + oy + 0.6, oz + 0.6],
            faces=F1(),
            rotation=[ai * 25, ai * 35, ai * 30],
        )
        auth_cubes.append(c)
    auth_gu = make_uuid()
    auth_g = b.make_group(
        "central_authority", [0, 6, 0], auth_cubes, group_uuid=auth_gu,
    )

    # ---- Orbit ring: 8 flat slabs at Y=4 ----
    ring_groups = []
    for ri in range(8):
        ang = (ri / 8.0) * 2 * math.pi
        cx = math.cos(ang) * RADIUS
        cz = math.sin(ang) * RADIUS
        cube = b.add_cube(
            f"ring_slab_{ri+1}",
            from_=[-1.0, -0.10, -0.4],
            to_=[1.0, 0.10, 0.4],
            faces=F1(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"ring_slab_grp_{ri+1}",
            [cx, 4, cz], [cube],
            rotation=[0, math.degrees(ang) + 90, 0],
            group_uuid=gu,
        )
        ring_groups.append((gu, g))
    ring_parent_gu = make_uuid()
    ring_parent = b.make_group("orbit_ring", [0, 0, 0],
                                 [g for (_u, g) in ring_groups],
                                 group_uuid=ring_parent_gu)

    # ---- Orbit parent for wardens ----
    orbit_gu = make_uuid()
    orbit_g = b.make_group("warden_orbit", [0, 0, 0],
                            [w["warden_g"] for w in wardens],
                            group_uuid=orbit_gu)

    # ---- ROOT ----
    root_uuid = make_uuid()
    root = b.make_group("root", [0, 0, 0],
                         [orbit_g, ring_parent, auth_g], group_uuid=root_uuid)
    b.outliner.append(root)

    # =========================================================================
    # ANIMATIONS
    # =========================================================================

    # ---- SPAWN: 1.1s ----
    spawn = {}
    # Central authority materialises at 0.0
    spawn[auth_gu] = {"name": auth_g["name"], "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, x=0, y=0, z=0),
        make_keyframe("scale", 0.15, x=1.5, y=1.5, z=1.5),
        make_keyframe("scale", 0.25, x=1, y=1, z=1),
        make_keyframe("scale", 1.10, x=1, y=1, z=1),
    ]}
    # Orbit ring expands at 0.2s
    for ri, (gu, g) in enumerate(ring_groups):
        t0 = 0.20 + ri * 0.015
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=1, z=0),
            make_keyframe("scale", t0, x=0, y=1, z=0),
            make_keyframe("scale", t0 + 0.10, x=1, y=1, z=1),
            make_keyframe("scale", 1.10, x=1, y=1, z=1),
        ]}
    # Wardens fade in spectrally one by one at 0.3s stagger
    for wi, warden in enumerate(wardens):
        t0 = 0.30 + wi * 0.18
        spawn[warden["warden_gu"]] = {"name": warden["warden_g"]["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.05, x=0.3, y=0.3, z=0.3),
            make_keyframe("scale", t0 + 0.20, x=1.05, y=1.05, z=1.05),
            make_keyframe("scale", t0 + 0.30, x=1, y=1, z=1),
            make_keyframe("scale", 1.10, x=1, y=1, z=1),
        ]}
        # Held chains deploy as warden solidifies
        for chain_gu, chain_g in ((warden["chain_L_gu"], warden["chain_L_g"]),
                                    (warden["chain_R_gu"], warden["chain_R_g"])):
            spawn[chain_gu] = {"name": chain_g["name"], "type": "bone", "keyframes": [
                make_keyframe("scale", 0.0, x=0, y=0, z=0),
                make_keyframe("scale", t0 + 0.20, x=0, y=0, z=0),
                make_keyframe("scale", t0 + 0.30, x=1, y=1, z=1),
                make_keyframe("scale", 1.10, x=1, y=1, z=1),
            ]}
    b.add_animation("spawn", length=1.10, animators=spawn, loop="once", override=True)

    # ---- IDLE: 8.0s — orbit + inward face + chain sway + bob ----
    idle = {}
    IDLE_LEN = 8.0
    IDLE_STEPS = 80
    # Warden orbit parent rotates 360° over 8s
    orbit_kfs = []
    for k in range(IDLE_STEPS + 1):
        t = (k / IDLE_STEPS) * IDLE_LEN
        deg = (t / IDLE_LEN) * 360
        orbit_kfs.append(make_keyframe("rotation", t, x=0, y=deg, z=0))
    idle[orbit_gu] = {"name": orbit_g["name"], "type": "bone", "keyframes": orbit_kfs}
    # Ring slow rotation opposite
    ring_kfs = []
    for k in range(IDLE_STEPS + 1):
        t = (k / IDLE_STEPS) * IDLE_LEN
        deg = -(t / IDLE_LEN) * 360 * 0.4
        ring_kfs.append(make_keyframe("rotation", t, x=0, y=deg, z=0))
    idle[ring_parent_gu] = {"name": ring_parent["name"], "type": "bone", "keyframes": ring_kfs}
    # Central authority pulses
    auth_kfs = []
    for k in range(IDLE_STEPS + 1):
        t = (k / IDLE_STEPS) * IDLE_LEN
        ph = (t / 1.6) * 2 * math.pi
        s = 1.0 + 0.12 * math.sin(ph)
        auth_kfs.append(make_keyframe("scale", t, x=s, y=s, z=s))
    idle[auth_gu] = {"name": auth_g["name"], "type": "bone", "keyframes": auth_kfs}
    # Wardens bob subtly + chains sway with secondary lag
    for wi, warden in enumerate(wardens):
        phase = wi * 0.7
        bob_kfs = []
        for k in range(IDLE_STEPS + 1):
            t = (k / IDLE_STEPS) * IDLE_LEN
            ph = (t / 2.4) * 2 * math.pi + phase
            bob_kfs.append(make_keyframe("position", t,
                                          x=0, y=0.3 * math.sin(ph), z=0))
        idle[warden["warden_gu"]] = {"name": warden["warden_g"]["name"], "type": "bone", "keyframes": bob_kfs}
        # Chains lag behind warden bob — opposite phase + smaller amplitude
        for chain_side, (chain_gu, chain_g) in [("L", (warden["chain_L_gu"], warden["chain_L_g"])),
                                                  ("R", (warden["chain_R_gu"], warden["chain_R_g"]))]:
            phase_offset = phase + (0.3 if chain_side == "L" else 0.5)
            chain_kfs = []
            for k in range(IDLE_STEPS + 1):
                t = (k / IDLE_STEPS) * IDLE_LEN
                ph = (t / 2.4) * 2 * math.pi + phase_offset
                chain_kfs.append(make_keyframe("rotation", t,
                                                x=3 * math.sin(ph),
                                                y=0,
                                                z=2 * math.cos(ph * 0.9)))
            idle[chain_gu] = {"name": chain_g["name"], "type": "bone", "keyframes": chain_kfs}
        # Helmet very slight inward tilt oscillation (looking around)
        helm_kfs = []
        for k in range(IDLE_STEPS + 1):
            t = (k / IDLE_STEPS) * IDLE_LEN
            ph = (t / 3.2) * 2 * math.pi + phase * 0.5
            helm_kfs.append(make_keyframe("rotation", t,
                                            x=1.5 * math.sin(ph),
                                            y=4 * math.sin(ph * 0.6),
                                            z=0))
        idle[warden["helm_gu"]] = {"name": warden["helm_g"]["name"], "type": "bone", "keyframes": helm_kfs}
        # Shoulder breathing
        shoulder_kfs = []
        for k in range(IDLE_STEPS + 1):
            t = (k / IDLE_STEPS) * IDLE_LEN
            ph = (t / 3.0) * 2 * math.pi + phase
            s = 1.0 + 0.02 * math.sin(ph)
            shoulder_kfs.append(make_keyframe("scale", t, x=s, y=1, z=s))
        idle[warden["shoulder_gu"]] = {"name": warden["shoulder_g"]["name"], "type": "bone", "keyframes": shoulder_kfs}
        # Arm idle micro-motion
        for arm_gu, arm_g, arm_phase_off in (
            (warden["arm_L_gu"], warden["arm_L_g"], 0.0),
            (warden["arm_R_gu"], warden["arm_R_g"], 0.4),
        ):
            arm_kfs = []
            for k in range(IDLE_STEPS + 1):
                t = (k / IDLE_STEPS) * IDLE_LEN
                ph = (t / 2.8) * 2 * math.pi + phase + arm_phase_off
                arm_kfs.append(make_keyframe("rotation", t,
                                              x=2 * math.sin(ph),
                                              y=0,
                                              z=1.5 * math.cos(ph)))
            idle[arm_gu] = {"name": arm_g["name"], "type": "bone", "keyframes": arm_kfs}
        # Torso breathing
        torso_kfs = []
        for k in range(IDLE_STEPS + 1):
            t = (k / IDLE_STEPS) * IDLE_LEN
            ph = (t / 4.0) * 2 * math.pi + phase
            s = 1.0 + 0.012 * math.sin(ph)
            torso_kfs.append(make_keyframe("scale", t, x=s, y=s * 1.005, z=s))
        idle[warden["torso_gu"]] = {"name": warden["torso_g"]["name"], "type": "bone", "keyframes": torso_kfs}
    b.add_animation("idle", length=IDLE_LEN, animators=idle, loop="loop", override=False)

    # ---- DISSIPATE: 0.9s ----
    diss = {}
    # Chains release and fly outward
    for wi, warden in enumerate(wardens):
        ang = warden["ang"]
        for side_label, chain_gu, chain_g, off_dir in (
            ("L", warden["chain_L_gu"], warden["chain_L_g"], -1),
            ("R", warden["chain_R_gu"], warden["chain_R_g"], +1),
        ):
            fly_x = math.cos(ang + off_dir * 0.5) * 8
            fly_z = math.sin(ang + off_dir * 0.5) * 8
            diss[chain_gu] = {"name": chain_g["name"], "type": "bone", "keyframes": [
                make_keyframe("position", 0.0, x=0, y=0, z=0),
                make_keyframe("position", 0.90, x=fly_x, y=4, z=fly_z),
                make_keyframe("scale", 0.0, x=1, y=1, z=1),
                make_keyframe("scale", 0.90, x=0, y=0, z=0),
            ]}
        # Wardens spectrally fade — scale to 0 over 0.7s
        diss[warden["warden_gu"]] = {"name": warden["warden_g"]["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.20, x=0.95, y=1.05, z=0.95),
            make_keyframe("scale", 0.70, x=0.0, y=0.05, z=0.0),
            make_keyframe("scale", 0.90, x=0, y=0, z=0),
        ]}
    # Ring expands and snaps
    diss[ring_parent_gu] = {"name": ring_parent["name"], "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, x=1, y=1, z=1),
        make_keyframe("scale", 0.60, x=1.5, y=1, z=1.5),
        make_keyframe("scale", 0.75, x=0, y=0, z=0),
    ]}
    # Authority fades last
    diss[auth_gu] = {"name": auth_g["name"], "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, x=1, y=1, z=1),
        make_keyframe("scale", 0.70, x=1, y=1, z=1),
        make_keyframe("scale", 0.90, x=0, y=0, z=0),
    ]}
    b.add_animation("dissipate", length=0.90, animators=diss, loop="once", override=True)

    b.write(OUTPUT)
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    build()
