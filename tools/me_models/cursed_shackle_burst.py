#!/usr/bin/env python3
"""Generator for cursed_shackle_burst.bbmodel — Chain Mode "Bound Soil".

Massive cursed shackle cuffs erupting from the ground — bound by an ancient
curse. Purple void energy seeps from the cracks. The horror is wondering what
the shackles were binding.

Bone hierarchy:
  - 2 shackle cuff bone groups (each: 6 D-ring slabs + 2 cuff sides) — 16 cubes/bones
  - 6 connecting chain link bone groups (oval-cross)
  - 8 void energy seam bones
  - 10 ground rupture slabs
  - 4 purple mist slabs
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
    rust_patches,  # used here for "purple stain patches"
    horizontal_grain,
    crack_lines_radial,
    specular_cluster,
    emissive_seam,
    shifted_face,
)

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/chain/me_attacks/cursed_shackle_burst.bbmodel"
RES = 128


# ---------------------------------------------------------------------------
# TEXTURE 0 — CURSED IRON (shackle cuffs / chain links)
#   Iron exposed to dark magic — rust replaced by purple contamination
# ---------------------------------------------------------------------------

def build_cursed_iron_texture():
    w = h = RES
    iron_black   = hex_to_rgba("#080608")
    cursed_dark  = hex_to_rgba("#180820")
    tainted_iron = hex_to_rgba("#382840")
    surface      = hex_to_rgba("#604870")
    bright_edge  = hex_to_rgba("#908098")
    spec_purp    = hex_to_rgba("#b090b8")
    emiss_purp   = hex_to_rgba("#6030a0")

    pixels = base_fill(w, h, cursed_dark)

    # Vertical stratification — purple-grey not warm
    for y in range(h):
        t = y / (h - 1)
        if t < 0.06:
            c = bright_edge
        elif t < 0.20:
            local = (t - 0.06) / 0.14
            c = color_lerp(bright_edge, surface, local)
        elif t < 0.42:
            local = (t - 0.20) / 0.22
            c = color_lerp(surface, tainted_iron, local)
        elif t < 0.68:
            local = (t - 0.42) / 0.26
            c = color_lerp(tainted_iron, cursed_dark, local)
        else:
            local = (t - 0.68) / 0.32
            c = color_lerp(cursed_dark, iron_black, local)
        for x in range(w):
            pixels[y * w + x] = c

    # PURPLE CONTAMINATION PATCHES — irregular, embedded
    rust_patches(pixels, w, h, tainted_iron, surface, count=20, seed=37)
    rust_patches(pixels, w, h, cursed_dark, tainted_iron, count=14, seed=61)

    # Horizontal grain
    horizontal_grain(pixels, w, h, cursed_dark, density=0.40, seed=83)

    # EMISSIVE SEAMS — purple glow at darkest iron zones
    emissive_seam(pixels, w, h, w // 4, emiss_purp, intensity=0.55, width=1)
    emissive_seam(pixels, w, h, 3 * w // 4, emiss_purp, intensity=0.42, width=1)
    # Horizontal emissive band at bottom (darkest area)
    for y in range(int(h * 0.78), h):
        for x in range(w):
            if (x + y) % 7 == 0:  # sparse glow
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, emiss_purp, 0.35)

    # Specular at (8 * scale, 6 * scale) = (16, 12) for 128
    specular_cluster(pixels, w, h, 16, 12, 4, spec_purp, strength=0.82)
    specular_cluster(pixels, w, h, w // 2 + 4, 8, 3, spec_purp, strength=0.6)
    specular_cluster(pixels, w, h, w - 18, 10, 3, bright_edge, strength=0.62)

    add_noise_overlay(pixels, w, h, iron_black, surface, density=0.20, seed=109)
    jitter_inplace(pixels, jitter_range=18, seed=323)
    jitter_inplace(pixels, jitter_range=10, seed=541, density=0.50)

    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# TEXTURE 1 — CURSED GROUND (void seams / ground rupture / mist)
# ---------------------------------------------------------------------------

def build_cursed_ground_texture():
    w = h = RES
    earth        = hex_to_rgba("#060408")
    void_crack   = hex_to_rgba("#8030d0")
    void_bright  = hex_to_rgba("#c060ff")
    deep_void    = hex_to_rgba("#020104")
    saturated    = hex_to_rgba("#400870")

    pixels = base_fill(w, h, earth)

    # Centre void intensity — the ground saturated with curse
    for y in range(h):
        for x in range(w):
            dx = x - w / 2
            dy = y - h / 2
            d = math.sqrt(dx * dx + dy * dy)
            t = min(1.0, d / (w * 0.65))
            if t < 0.25:
                local = t / 0.25
                c = color_lerp(saturated, void_crack, 0.3 + local * 0.5)
            elif t < 0.55:
                local = (t - 0.25) / 0.30
                c = color_lerp(void_crack, saturated, local)
            else:
                local = (t - 0.55) / 0.45
                c = color_lerp(saturated, earth, local)
            pixels[y * w + x] = c

    # Vivid purple crack lines
    crack_lines_radial(pixels, w, h, w // 2, h // 2, branches=13, color=void_bright, seed=29)
    crack_lines_radial(pixels, w, h, w // 2, h // 2, branches=9, color=void_crack, seed=53)

    # Add bright emissive spots at intensity zones
    import random
    rng = random.Random(91)
    for _ in range(40):
        cx = rng.randrange(2, w - 2)
        cy = rng.randrange(2, h - 2)
        d_from_center = math.sqrt((cx - w/2)**2 + (cy - h/2)**2)
        if d_from_center < w * 0.35:
            specular_cluster(pixels, w, h, cx, cy, rng.randint(1, 3), void_bright, strength=0.55)

    add_noise_overlay(pixels, w, h, deep_void, void_crack, density=0.22, seed=147)
    jitter_inplace(pixels, jitter_range=20, seed=281)
    jitter_inplace(pixels, jitter_range=12, seed=331, density=0.55)

    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# MODEL BUILD
# ---------------------------------------------------------------------------

def build():
    b = Builder("cursed_shackle_burst", resolution=(RES, RES), visible_box=(14, 10, 0))
    b.add_texture("cursed_iron", build_cursed_iron_texture())
    b.add_texture("cursed_ground", build_cursed_ground_texture())

    def iron_face(seed):
        return shifted_face(seed % 100, (seed * 7) % 100, 22, tex_index=0, RES=RES)
    def void_face(seed):
        return shifted_face(seed % 100, (seed * 11) % 100, 18, tex_index=1, RES=RES)

    # =====================================================================
    # 1) SHACKLE CUFFS — 2 cuffs at radius 4, facing each other
    #    Each cuff: 6 flat slabs forming a half-circle (D-ring) + 2 cuff sides
    # =====================================================================
    cuff_groups = []  # (group_uuid, group, side_label)
    cuff_positions = [
        # (cx, side_label, facing_inward_yaw)
        (-4.0, "left",  0.0),
        ( 4.0, "right", 180.0),
    ]
    for ci, (cx, side, yaw) in enumerate(cuff_positions):
        cubes = []
        # D-ring half-circle — 6 slabs arranged in a half-circle (facing the other cuff)
        for di in range(6):
            ang = (di / 5.0) * math.pi - math.pi / 2  # half circle from -90 to +90 around X
            slab_x = math.cos(ang) * 1.4
            slab_y = 3.0 + math.sin(ang) * 1.4
            cu = b.add_cube(
                f"cuff_{side}_d{di+1}",
                from_=[-0.45, -0.20, -0.55], to_=[0.45, 0.20, 0.55],
                faces=iron_face(50 + ci * 30 + di * 4),
            )
            # Build as direct cube; we'll rotate via local origin
            # Use a sub-group for the slab so rotation pivots correctly
            sgu = make_uuid()
            sg = b.make_group(
                f"d_ring_{side}_{di+1}", [slab_x, slab_y, 0], [cu],
                rotation=[0, 0, math.degrees(ang)], group_uuid=sgu,
            )
            cubes.append(sg)
        # 2 cuff sides — flat slabs on either side
        for si, z_off in enumerate([-0.55, 0.55]):
            cu = b.add_cube(
                f"cuff_{side}_side_{si+1}",
                from_=[-1.4, -1.4, -0.08], to_=[1.4, 1.4, 0.08],
                faces=iron_face(80 + ci * 30 + si * 5),
            )
            sgu = make_uuid()
            sg = b.make_group(
                f"cuff_side_{side}_{si+1}", [0, 3.0, z_off], [cu], group_uuid=sgu,
            )
            cubes.append(sg)
        gu = make_uuid()
        # Children include sub-groups (which are dicts), not just cubes
        # We add as children of the cuff group
        g = b.make_group(
            f"shackle_cuff_{side}", [cx, 0, 0], cubes,
            rotation=[0, yaw, 0], group_uuid=gu,
        )
        cuff_groups.append((gu, g, side, cx))

    # =====================================================================
    # 2) CONNECTING CHAIN — 6 links between the cuffs at Y=2-4
    # =====================================================================
    chain_groups = []
    for i in range(6):
        # Distribute links from x=-3 to x=3 (between cuffs)
        t = i / 5.0
        cx = -3.0 + t * 6.0
        # Slight catenary curve — links sag in the middle
        sag = -math.sin(t * math.pi) * 0.3
        cy = 3.0 + sag
        # horizontal slab
        h_uuid = b.add_cube(
            f"chain_link{i+1}_h",
            from_=[cx - 0.42, cy - 0.13, -0.22],
            to_=  [cx + 0.42, cy + 0.13,  0.22],
            faces=iron_face(140 + i * 11),
        )
        # vertical slab
        v_uuid = b.add_cube(
            f"chain_link{i+1}_v",
            from_=[cx - 0.22, cy - 0.42, -0.13],
            to_=  [cx + 0.22, cy + 0.42,  0.13],
            faces=iron_face(155 + i * 13),
        )
        gu = make_uuid()
        # Alternating rotation per link
        rot_x = 90.0 if i % 2 == 1 else 0.0
        g = b.make_group(
            f"chain_link_{i+1}", [cx, cy, 0], [h_uuid, v_uuid],
            rotation=[rot_x, 0, 0], group_uuid=gu,
        )
        chain_groups.append((gu, g, i))

    # =====================================================================
    # 3) VOID ENERGY SEAMS — 8 thin slabs at Y=0 around/between cuffs
    # =====================================================================
    seam_groups = []
    seam_positions = []
    # 4 seams between the cuffs (X axis aligned)
    for i in range(4):
        x = -2.5 + i * (5.0 / 3)
        seam_positions.append((x, 0, 25.0 + i * 7))
    # 4 seams around the cuffs (Z axis)
    for i in range(4):
        ang = (i / 4.0) * 2 * math.pi
        seam_positions.append((math.cos(ang) * 5.5, math.sin(ang) * 2.5, math.degrees(ang)))

    for i, (px, pz, rot_y) in enumerate(seam_positions):
        cu = b.add_cube(
            f"void_seam_{i+1}",
            from_=[-1.0, 0.02, -0.18], to_=[1.0, 0.18, 0.18],
            faces=void_face(190 + i * 13),
        )
        gu = make_uuid()
        g = b.make_group(
            f"void_seam_{i+1}", [px, 0.1, pz], [cu],
            rotation=[0, rot_y, 0], group_uuid=gu,
        )
        seam_groups.append((gu, g))

    # =====================================================================
    # 4) GROUND RUPTURE — 10 slabs in a wider ring
    # =====================================================================
    rupture_groups = []
    for i in range(10):
        ang = (i / 10.0) * 2 * math.pi + 0.07
        r = 6.5
        cx = math.cos(ang) * r
        cz = math.sin(ang) * r
        cu = b.add_cube(
            f"rupture_{i+1}",
            from_=[-1.5, 0.02, -0.55], to_=[1.5, 0.15, 0.55],
            faces=void_face(240 + i * 17),
        )
        rot_y = math.degrees(ang)
        gu = make_uuid()
        g = b.make_group(
            f"ground_rupture_{i+1}", [cx, 0.08, cz], [cu],
            rotation=[0, rot_y, 0], group_uuid=gu,
        )
        rupture_groups.append((gu, g, ang))

    # =====================================================================
    # 5) PURPLE MIST — 4 overlapping flat slabs at Y=0.3 between shackles
    # =====================================================================
    mist_groups = []
    mist_positions = [
        (-1.5, 0.3,  0.3),
        ( 1.5, 0.3, -0.3),
        ( 0.0, 0.4,  0.6),
        ( 0.0, 0.35, -0.6),
    ]
    for i, (px, py, pz) in enumerate(mist_positions):
        cu = b.add_cube(
            f"mist_{i+1}",
            from_=[-1.4, -0.05, -1.0], to_=[1.4, 0.05, 1.0],
            faces=void_face(300 + i * 19),
        )
        gu = make_uuid()
        g = b.make_group(
            f"purple_mist_{i+1}", [px, py, pz], [cu],
            rotation=[0, i * 22, 0], group_uuid=gu,
        )
        mist_groups.append((gu, g))

    # ROOT
    root_children = []
    for (gu, g, _side, _cx) in cuff_groups:
        root_children.append(g)
    for (gu, g, _i) in chain_groups:
        root_children.append(g)
    for (gu, g) in seam_groups:
        root_children.append(g)
    for (gu, g, _ang) in rupture_groups:
        root_children.append(g)
    for (gu, g) in mist_groups:
        root_children.append(g)

    root_uuid = make_uuid()
    root_group = b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid)
    b.outliner.append(root_group)

    # ===========================================================================
    # ANIMATION — SPAWN (1.2s)
    # Ground rupture 0.1s. Void seams 0.2s. Left cuff 0.3s, right cuff 0.4s.
    # Chain links materialise sequentially 0.5s. Purple mist 0.6s.
    # ===========================================================================
    spawn = {}

    # Ground rupture
    for idx, (gu, g, ang) in enumerate(rupture_groups):
        delay = 0.1 + idx * 0.02
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.10, 1.3, 1.3, 1.3),
            make_keyframe("scale", delay + 0.18, 1, 1, 1),
            make_keyframe("scale", 1.2, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Void seams
    for idx, (gu, g) in enumerate(seam_groups):
        delay = 0.2 + idx * 0.03
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.10, 1.2, 1.5, 1.2),
            make_keyframe("scale", delay + 0.20, 1, 1, 1),
            make_keyframe("scale", 1.2, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Shackle cuffs — left at 0.3s from below, right at 0.4s
    for ci, (gu, g, side, cx) in enumerate(cuff_groups):
        delay = 0.3 if side == "left" else 0.4
        kfs = [
            make_keyframe("position", 0.0, 0, -3, 0),
            make_keyframe("position", delay, 0, -3, 0),
            make_keyframe("position", delay + 0.20, 0, 0.4, 0),
            make_keyframe("position", delay + 0.30, 0, 0, 0),
            make_keyframe("position", 1.2, 0, 0, 0),
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.12, 1.15, 1.25, 1.15),
            make_keyframe("scale", delay + 0.28, 1, 1, 1),
            make_keyframe("scale", 1.2, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Chain links — materialise sequentially at 0.5s
    for idx, (gu, g, i) in enumerate(chain_groups):
        delay = 0.5 + i * 0.04
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.10, 1.2, 1.2, 1.2),
            make_keyframe("scale", delay + 0.18, 1, 1, 1),
            make_keyframe("scale", 1.2, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Purple mist — spread at 0.6s
    for idx, (gu, g) in enumerate(mist_groups):
        delay = 0.6 + idx * 0.04
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.20, 1.4, 1, 1.4),
            make_keyframe("scale", delay + 0.40, 1, 1, 1),
            make_keyframe("scale", 1.2, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("spawn", length=1.2, animators=spawn, loop="once", override=True)

    # ===========================================================================
    # ANIMATION — IDLE (5.0s)
    # Shackle cuffs slowly pull against each other (subtle Y oscillation).
    # Chain links sway with secondary lag.
    # Void seams pulse purple emissive (via scale + position).
    # ===========================================================================
    idle = {}

    # Cuffs pull against each other — subtle X oscillation (the binding pulling)
    for ci, (gu, g, side, cx) in enumerate(cuff_groups):
        sign = -1 if side == "left" else 1
        kfs = []
        for k in range(11):
            t = (k / 10) * 5.0
            ph = (t / 5.0) * 2 * math.pi
            # pull inward (toward 0) then release
            dx = sign * (-math.sin(ph) * 0.18 + math.sin(ph * 0.5) * 0.05)
            dy = math.sin(ph * 1.2) * 0.10
            kfs.append(make_keyframe("position", t, dx, dy, 0))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Chain links — secondary lag along the chain
    for idx, (gu, g, i) in enumerate(chain_groups):
        lag = i * 0.08
        kfs = []
        for k in range(13):
            t = (k / 12) * 5.0
            ph = ((t - lag) / 5.0) * 2 * math.pi
            yy = math.sin(ph) * 0.15
            rz = math.cos(ph * 0.9) * 4.0
            kfs.append(make_keyframe("position", t, 0, yy, 0))
            kfs.append(make_keyframe("rotation", t, 0, 0, rz))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Void seams pulse — scale + slight Y movement
    for idx, (gu, g) in enumerate(seam_groups):
        kfs = []
        for k in range(11):
            t = (k / 10) * 5.0
            ph = (t / 2.5) * 2 * math.pi + idx * 0.5
            s = 1.0 + 0.18 * (0.5 + 0.5 * math.sin(ph))
            kfs.append(make_keyframe("scale", t, s, 1.0 + 0.3 * math.sin(ph), s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Ground rupture — slow pulse
    for idx, (gu, g, ang) in enumerate(rupture_groups):
        kfs = []
        for k in range(9):
            t = (k / 8) * 5.0
            ph = (t / 4.0) * 2 * math.pi + idx * 0.3
            s = 1.0 + 0.05 * math.sin(ph)
            kfs.append(make_keyframe("scale", t, s, 1, s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Purple mist — drift
    for idx, (gu, g) in enumerate(mist_groups):
        kfs = []
        for k in range(11):
            t = (k / 10) * 5.0
            ph = (t / 4.5) * 2 * math.pi + idx * 0.8
            kfs.append(make_keyframe("rotation", t, 0, idx * 22 + math.degrees(ph) * 0.05, 0))
            yy = 0.4 + math.sin(ph) * 0.2
            kfs.append(make_keyframe("position", t, math.cos(ph) * 0.3, yy, math.sin(ph) * 0.3))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("idle", length=5.0, animators=idle, loop="loop", override=False)

    # ===========================================================================
    # ANIMATION — DISSIPATE (0.8s)
    # Chain links fly apart simultaneously. Cuffs sink. Void energy flares then collapses.
    # ===========================================================================
    diss = {}

    # Chain links fly apart — random outward directions
    import random
    rng = random.Random(11)
    for idx, (gu, g, i) in enumerate(chain_groups):
        ang = rng.random() * 2 * math.pi
        out_x = math.cos(ang) * 4
        out_y = 1 + rng.random() * 2
        out_z = math.sin(ang) * 4
        kfs = [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.4, out_x, out_y, out_z),
            make_keyframe("position", 0.8, out_x * 1.5, out_y - 2, out_z * 1.5),
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 0.8, rng.randint(-180, 180), rng.randint(-180, 180), rng.randint(-180, 180)),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.5, 1, 1, 1),
            make_keyframe("scale", 0.8, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Cuffs sink below ground
    for ci, (gu, g, side, cx) in enumerate(cuff_groups):
        kfs = [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.3, 0, 0.2, 0),
            make_keyframe("position", 0.6, 0, -2, 0),
            make_keyframe("position", 0.8, 0, -4, 0),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.5, 1, 1, 1),
            make_keyframe("scale", 0.8, 0.5, 0.5, 0.5),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Void seams flare then collapse
    for idx, (gu, g) in enumerate(seam_groups):
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.15, 1.8, 2.0, 1.8),
            make_keyframe("scale", 0.4, 1, 1, 1),
            make_keyframe("scale", 0.8, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Mist disperses outward
    for idx, (gu, g) in enumerate(mist_groups):
        ang = idx * (math.pi / 2)
        kfs = [
            make_keyframe("position", 0.0, 0, 0.3, 0),
            make_keyframe("position", 0.8, math.cos(ang) * 3, 1.5, math.sin(ang) * 3),
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.4, 1.5, 1, 1.5),
            make_keyframe("scale", 0.8, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Ground rupture contracts
    for idx, (gu, g, ang) in enumerate(rupture_groups):
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.6, 1, 1, 1),
            make_keyframe("scale", 0.8, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("dissipate", length=0.8, animators=diss, loop="once", override=True)

    b.write(OUTPUT_PATH)
    sz = os.path.getsize(OUTPUT_PATH)
    print(f"Wrote {OUTPUT_PATH} ({sz} bytes, {sz/1024:.1f} KB)")


if __name__ == "__main__":
    build()
