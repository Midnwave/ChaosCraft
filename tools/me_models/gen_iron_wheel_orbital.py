#!/usr/bin/env python3
"""Generator: iron_wheel_orbital.bbmodel — Chain attack 19 'Punishment'.

A massive medieval breaking wheel orbits the caster, ALSO spinning on its own
axle as it orbits — like a rolling wheel in orbit. The wheel has a rim of 12
flat slab pieces (vertical, perpendicular to ground), 8 spokes radiating from
a 3-cube hub, 10 chain links binding the rim, and 4 axle chain links extending
upward.

UNIQUE vs other modes:
 - Aged USED iron with smooth-grip patches (not forged colossus, not cursed)
 - Wheel SHAPE — recognisable wheel silhouette unlike any other orbital
 - SPIN-WHILE-ORBITING (two simultaneous rotations) — mechanical roll
 - Subtle blood-stain hint in darkest texture areas
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
    chain_link_silhouette, radial_glow,
)
from _freezingice_common import (
    specular_blob, random_bright_specks, hex_color,
)


OUTPUT = "D:/CC/ChaosCraft/src/main/resources/models/chain/me_attacks/iron_wheel_orbital.bbmodel"
W = H = 64


# ---------------------------------------------------------------------------
# TEXTURES
# ---------------------------------------------------------------------------

def tex_wheel_iron():
    """Aged USED iron — outdoor, gripped, lightly bloody in shadows."""
    aged_wheel = hex_color("#10090a")
    worn_dark  = hex_color("#281c18")
    wheel_surf = hex_color("#4c3828")
    spoke_edge = hex_color("#786040")
    spec       = hex_color("#b09868")
    blood_hint = hex_color("#3a0808")
    pixels = base_fill(W, H, worn_dark)
    # Diagonal grain gradient (used wheel look)
    for y in range(H):
        for x in range(W):
            t = (x + y) / float(W + H - 2)
            r = int(aged_wheel[0] + (wheel_surf[0] - aged_wheel[0]) * t)
            g = int(aged_wheel[1] + (wheel_surf[1] - aged_wheel[1]) * t)
            bl = int(aged_wheel[2] + (wheel_surf[2] - aged_wheel[2]) * t)
            pixels[y * W + x] = (r, g, bl, 255)
    # Worn-smooth grip patches (brighter patches where hands gripped)
    rng = random.Random(73)
    for _ in range(8):
        cx = rng.randrange(W)
        cy = rng.randrange(H)
        rr = rng.randint(5, 10)
        for dy in range(-rr, rr + 1):
            for dx in range(-rr, rr + 1):
                d = math.sqrt(dx * dx + dy * dy)
                if d <= rr:
                    x, y = cx + dx, cy + dy
                    if 0 <= x < W and 0 <= y < H:
                        t = 1.0 - d / rr
                        p = pixels[y * W + x]
                        # Brighten toward spoke_edge
                        nr = int(p[0] + (spoke_edge[0] - p[0]) * t * 0.4)
                        ng = int(p[1] + (spoke_edge[1] - p[1]) * t * 0.4)
                        nb = int(p[2] + (spoke_edge[2] - p[2]) * t * 0.4)
                        pixels[y * W + x] = (nr, ng, nb, 255)
    # Rough patches elsewhere — use rust_blotch
    rust_blotch(pixels, W, H, aged_wheel, worn_dark, wheel_surf, count=10, seed=89)
    # Stress lines suggesting wheel rotation grain
    horizontal_stress_lines(pixels, W, H, spoke_edge, count=8, seed=97)
    # Specular highlights on grip patches
    specular_blob(pixels, W, H, 14, 18, spec, radius=4, strength=0.55)
    specular_blob(pixels, W, H, 46, 38, spec, radius=4, strength=0.55)
    specular_blob(pixels, W, H, 30, 50, spec, radius=3, strength=0.5)
    # Blood-stain hint in darkest patches
    rng2 = random.Random(101)
    for _ in range(20):
        cx = rng2.randrange(W)
        cy = rng2.randrange(H)
        size = rng2.randint(2, 4)
        for dy in range(-size, size + 1):
            for dx in range(-size, size + 1):
                if dx * dx + dy * dy <= size * size:
                    x, y = cx + dx, cy + dy
                    if 0 <= x < W and 0 <= y < H:
                        p = pixels[y * W + x]
                        # Only stain where it's already dark
                        if p[0] < 60 and p[1] < 50 and p[2] < 40:
                            t = 0.35
                            pixels[y * W + x] = (
                                int(p[0] + (blood_hint[0] - p[0]) * t),
                                int(p[1] + (blood_hint[1] - p[1]) * t),
                                int(p[2] + (blood_hint[2] - p[2]) * t),
                                255,
                            )
    random_bright_specks(pixels, W, H, 50, spoke_edge, seed=31, size_max=1)
    jitter_inplace(pixels, jitter_range=14, seed=39, density=0.85)
    return png_from_pixels(pixels, W, H)


def tex_wheel_chain_and_shadow():
    """Deep iron with rust-orange chain detail."""
    deep_iron = hex_color("#080608")
    rust_orange = hex_color("#602808")
    bright_rust = hex_color("#a04818")
    pixels = base_fill(W, H, deep_iron)
    # Rust streaks
    rng = random.Random(43)
    for _ in range(22):
        x0 = rng.randrange(W)
        y0 = rng.randrange(H)
        length = rng.randint(8, 18)
        ang = rng.uniform(0, 2 * math.pi)
        for k in range(length):
            x = int(x0 + math.cos(ang) * k)
            y = int(y0 + math.sin(ang) * k)
            if 0 <= x < W and 0 <= y < H:
                p = pixels[y * W + x]
                pixels[y * W + x] = (
                    int(p[0] + (rust_orange[0] - p[0]) * 0.7),
                    int(p[1] + (rust_orange[1] - p[1]) * 0.7),
                    int(p[2] + (rust_orange[2] - p[2]) * 0.7),
                    255,
                )
    # Chain link silhouettes
    chain_link_silhouette(pixels, W, H, rust_orange, bright_rust, count=5, seed=53)
    # Specular pops on rust
    random_bright_specks(pixels, W, H, 60, bright_rust, seed=67, size_max=1)
    jitter_inplace(pixels, jitter_range=12, seed=79, density=0.85)
    return png_from_pixels(pixels, W, H)


# ---------------------------------------------------------------------------
# BUILD
# ---------------------------------------------------------------------------

def build():
    b = Builder("iron_wheel_orbital", resolution=(W, H), visible_box=(20, 16, 0))
    b.add_texture("wheel_iron", tex_wheel_iron())
    b.add_texture("wheel_chain_shadow", tex_wheel_chain_and_shadow())

    F0 = lambda: uniform_face(0, 0, W, H, tex_index=0)
    F1 = lambda: uniform_face(0, 0, W, H, tex_index=1)

    ORBIT_R = 9.0      # wheel orbits at radius 9
    ORBIT_Y = 6.0      # wheel center at Y=6
    WHEEL_R = 3.0      # wheel itself radius

    # ---- Wheel rim: 12 flat slab pieces in a circle, vertical orientation ----
    rim_bones = []
    for ri in range(12):
        ang = (ri / 12.0) * 2 * math.pi
        rx = math.cos(ang) * WHEEL_R
        ry = math.sin(ang) * WHEEL_R
        # Each slab is a thin tall rectangle perpendicular to the rim radius
        cube = b.add_cube(
            f"rim_slab_{ri+1}",
            from_=[-0.50, -0.25, -0.18],
            to_=[0.50, 0.25, 0.18],
            faces=F0(),
        )
        gu = make_uuid()
        # Slabs sit at the rim with rotation tangent to the circle
        g = b.make_group(
            f"rim_slab_grp_{ri+1}",
            [rx, ry, 0],
            [cube],
            rotation=[0, 0, math.degrees(ang) + 90],
            group_uuid=gu,
        )
        rim_bones.append((gu, g, ang))

    # ---- Wheel spokes: 8 flat slabs radiating from hub to rim ----
    spoke_bones = []
    for si in range(8):
        ang = (si / 8.0) * 2 * math.pi
        cube = b.add_cube(
            f"spoke_{si+1}",
            from_=[-0.18, 0.0, -0.18],
            to_=[0.18, WHEEL_R, 0.18],
            faces=F0(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"spoke_grp_{si+1}",
            [0, 0, 0],
            [cube],
            rotation=[0, 0, math.degrees(ang) - 90],
            group_uuid=gu,
        )
        spoke_bones.append((gu, g))

    # ---- Hub: 3-cube central cluster ----
    hub_cubes = []
    for hi, (ox, oy, oz) in enumerate([(0, 0, 0), (-0.2, 0.2, 0.2), (0.2, -0.2, -0.2)]):
        c = b.add_cube(
            f"hub_{hi+1}",
            from_=[ox - 0.55, oy - 0.55, oz - 0.55],
            to_=[ox + 0.55, oy + 0.55, oz + 0.55],
            faces=F0(),
            rotation=[hi * 20, hi * 30, hi * 25],
        )
        hub_cubes.append(c)
    hub_gu = make_uuid()
    hub_g = b.make_group("hub", [0, 0, 0], hub_cubes, group_uuid=hub_gu)

    # ---- Rim chain binding: 10 chain link bone groups around the rim ----
    rim_chain_bones = []
    for ci in range(10):
        ang = (ci / 10.0) * 2 * math.pi
        cx = math.cos(ang) * (WHEEL_R + 0.5)
        cy = math.sin(ang) * (WHEEL_R + 0.5)
        # 3-cube oval link
        top = b.add_cube(
            f"rim_chain{ci+1}_top",
            from_=[-0.30, 0.18, -0.12], to_=[0.30, 0.34, 0.12],
            faces=F1(),
        )
        bot = b.add_cube(
            f"rim_chain{ci+1}_bot",
            from_=[-0.30, -0.34, -0.12], to_=[0.30, -0.18, 0.12],
            faces=F1(),
        )
        side1 = b.add_cube(
            f"rim_chain{ci+1}_side1",
            from_=[-0.30, -0.18, -0.12], to_=[-0.20, 0.18, 0.12],
            faces=F1(),
        )
        side2 = b.add_cube(
            f"rim_chain{ci+1}_side2",
            from_=[0.20, -0.18, -0.12], to_=[0.30, 0.18, 0.12],
            faces=F1(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"rim_chain_grp_{ci+1}",
            [cx, cy, 0],
            [top, bot, side1, side2],
            rotation=[0, 0, math.degrees(ang)],
            group_uuid=gu,
        )
        rim_chain_bones.append((gu, g, ang))

    # ---- Wheel parent group (rim + spokes + hub + rim chains) ----
    wheel_children = ([gu for (gu, _g, _a) in rim_bones]
                       + [gu for (gu, _g) in spoke_bones]
                       + [hub_gu]
                       + [gu for (gu, _g, _a) in rim_chain_bones])
    wheel_gu = make_uuid()
    wheel_g = b.make_group(
        "wheel", [ORBIT_R, ORBIT_Y, 0],
        wheel_children,
        rotation=[0, 90, 0],  # face wheel along orbit tangent
        group_uuid=wheel_gu,
    )

    # ---- Axle chain: 4 chain link bone groups extending from hub upward ----
    axle_chain_bones = []
    for ai in range(4):
        cy_offset = (ai + 1) * 1.2
        # alternating oval orientation
        if ai % 2 == 0:
            top = b.add_cube(
                f"axle_chain{ai+1}_top",
                from_=[-0.32, cy_offset + 0.20, -0.14], to_=[0.32, cy_offset + 0.40, 0.14],
                faces=F1(),
            )
            bot = b.add_cube(
                f"axle_chain{ai+1}_bot",
                from_=[-0.32, cy_offset - 0.40, -0.14], to_=[0.32, cy_offset - 0.20, 0.14],
                faces=F1(),
            )
            side1 = b.add_cube(
                f"axle_chain{ai+1}_side1",
                from_=[-0.32, cy_offset - 0.20, -0.14], to_=[-0.22, cy_offset + 0.20, 0.14],
                faces=F1(),
            )
            side2 = b.add_cube(
                f"axle_chain{ai+1}_side2",
                from_=[0.22, cy_offset - 0.20, -0.14], to_=[0.32, cy_offset + 0.20, 0.14],
                faces=F1(),
            )
        else:
            top = b.add_cube(
                f"axle_chain{ai+1}_top",
                from_=[-0.14, cy_offset + 0.20, -0.32], to_=[0.14, cy_offset + 0.40, 0.32],
                faces=F1(),
            )
            bot = b.add_cube(
                f"axle_chain{ai+1}_bot",
                from_=[-0.14, cy_offset - 0.40, -0.32], to_=[0.14, cy_offset - 0.20, 0.32],
                faces=F1(),
            )
            side1 = b.add_cube(
                f"axle_chain{ai+1}_side1",
                from_=[-0.14, cy_offset - 0.20, -0.32], to_=[0.14, cy_offset + 0.20, -0.22],
                faces=F1(),
            )
            side2 = b.add_cube(
                f"axle_chain{ai+1}_side2",
                from_=[-0.14, cy_offset - 0.20, 0.22], to_=[0.14, cy_offset + 0.20, 0.32],
                faces=F1(),
            )
        gu = make_uuid()
        g = b.make_group(
            f"axle_chain_grp_{ai+1}",
            [ORBIT_R, ORBIT_Y, 0],
            [top, bot, side1, side2],
            group_uuid=gu,
        )
        axle_chain_bones.append((gu, g))

    # ---- Ground orbit shadow: 8 flat slabs at Y=0 below the wheel ----
    ground_shadow_bones = []
    for gi in range(8):
        ang = (gi / 8.0) * 2 * math.pi
        gx = math.cos(ang) * 2.2
        gz = math.sin(ang) * 2.2
        cube = b.add_cube(
            f"ground_shadow_{gi+1}",
            from_=[gx - 1.0, -0.08, gz - 1.0],
            to_=[gx + 1.0, 0.05, gz + 1.0],
            faces=F1(),
        )
        gu = make_uuid()
        g = b.make_group(
            f"ground_shadow_grp_{gi+1}",
            [ORBIT_R, 0, 0],
            [cube], group_uuid=gu,
        )
        ground_shadow_bones.append((gu, g))

    # ---- Orbit parent: holds wheel + axle + shadow ----
    orbit_gu = make_uuid()
    orbit_children = [wheel_g] + [g for (_u, g) in axle_chain_bones] + [g for (_u, g) in ground_shadow_bones]
    orbit_g = b.make_group("wheel_orbit", [0, 0, 0], orbit_children, group_uuid=orbit_gu)

    # ---- ROOT ----
    root_uuid = make_uuid()
    root = b.make_group("root", [0, 0, 0], [orbit_g], group_uuid=root_uuid)
    b.outliner.append(root)

    # =========================================================================
    # ANIMATIONS
    # =========================================================================

    # ---- SPAWN: 1.1s ----
    spawn = {}
    # Axle chain appears at Y+12 at 0.0s — scale-in from top
    for ai, (gu, g) in enumerate(axle_chain_bones):
        t0 = 0.0 + ai * 0.05
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.10, x=1, y=1, z=1),
            make_keyframe("scale", 1.10, x=1, y=1, z=1),
        ]}
    # Wheel materialises at 0.3s
    spawn[wheel_gu] = {"name": wheel_g["name"], "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, x=0, y=0, z=0),
        make_keyframe("scale", 0.30, x=0, y=0, z=0),
        make_keyframe("scale", 0.40, x=1.15, y=1.15, z=1.15),
        make_keyframe("scale", 0.50, x=1, y=1, z=1),
        make_keyframe("scale", 1.10, x=1, y=1, z=1),
    ]}
    # Spokes deploy radially at 0.4s
    for si, (gu, g) in enumerate(spoke_bones):
        t0 = 0.40 + si * 0.015
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.10, x=1, y=1, z=1),
            make_keyframe("scale", 1.10, x=1, y=1, z=1),
        ]}
    # Rim slabs appear at 0.45s
    for ri, (gu, g, _a) in enumerate(rim_bones):
        t0 = 0.45 + ri * 0.01
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.08, x=1, y=1, z=1),
            make_keyframe("scale", 1.10, x=1, y=1, z=1),
        ]}
    # Rim chains appear at 0.5s
    for ci, (gu, g, _a) in enumerate(rim_chain_bones):
        t0 = 0.50 + ci * 0.02
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=0, z=0),
            make_keyframe("scale", t0, x=0, y=0, z=0),
            make_keyframe("scale", t0 + 0.10, x=1, y=1, z=1),
            make_keyframe("scale", 1.10, x=1, y=1, z=1),
        ]}
    # Hub solidifies
    spawn[hub_gu] = {"name": hub_g["name"], "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, x=0, y=0, z=0),
        make_keyframe("scale", 0.45, x=0, y=0, z=0),
        make_keyframe("scale", 0.55, x=1.2, y=1.2, z=1.2),
        make_keyframe("scale", 0.65, x=1, y=1, z=1),
    ]}
    # Ground shadow appears at 0.3s
    for gi, (gu, g) in enumerate(ground_shadow_bones):
        t0 = 0.30 + gi * 0.02
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=0, y=1, z=0),
            make_keyframe("scale", t0, x=0, y=1, z=0),
            make_keyframe("scale", t0 + 0.08, x=1, y=1, z=1),
            make_keyframe("scale", 1.10, x=1, y=1, z=1),
        ]}
    b.add_animation("spawn", length=1.10, animators=spawn, loop="once", override=True)

    # ---- IDLE: 6.0s — orbit + spin + chains ----
    idle = {}
    IDLE_LEN = 6.0
    IDLE_STEPS = 60
    # Orbit parent rotates 360° over 6s
    orbit_kfs = []
    for k in range(IDLE_STEPS + 1):
        t = (k / IDLE_STEPS) * IDLE_LEN
        deg = (t / IDLE_LEN) * 360
        orbit_kfs.append(make_keyframe("rotation", t, x=0, y=deg, z=0))
    idle[orbit_gu] = {"name": orbit_g["name"], "type": "bone", "keyframes": orbit_kfs}
    # Wheel spins on its own Z axis (rolling)
    wheel_spin_kfs = []
    for k in range(IDLE_STEPS + 1):
        t = (k / IDLE_STEPS) * IDLE_LEN
        # Wheel does 3 full spins per orbit (or per 6s)
        deg = (t / IDLE_LEN) * 360 * 3
        wheel_spin_kfs.append(make_keyframe("rotation", t, x=0, y=90, z=deg))
    idle[wheel_gu] = {"name": wheel_g["name"], "type": "bone", "keyframes": wheel_spin_kfs}
    # Hub oscillates
    hub_kfs = []
    for k in range(IDLE_STEPS + 1):
        t = (k / IDLE_STEPS) * IDLE_LEN
        ph = (t / 1.4) * 2 * math.pi
        s = 1.0 + 0.08 * math.sin(ph)
        hub_kfs.append(make_keyframe("scale", t, x=s, y=s, z=s))
    idle[hub_gu] = {"name": hub_g["name"], "type": "bone", "keyframes": hub_kfs}
    # Rim chains sway on individual periods (counter-rotate to wheel for parallax)
    for ci, (gu, g, ang) in enumerate(rim_chain_bones):
        phase = ci * 0.4
        kfs = []
        for k in range(IDLE_STEPS + 1):
            t = (k / IDLE_STEPS) * IDLE_LEN
            ph = (t / 2.0) * 2 * math.pi + phase
            kfs.append(make_keyframe("rotation", t,
                                      x=2.5 * math.sin(ph),
                                      y=0,
                                      z=2.0 * math.cos(ph * 0.9)))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}
    # Axle chain sway
    for ai, (gu, g) in enumerate(axle_chain_bones):
        phase = ai * 0.5
        kfs = []
        for k in range(IDLE_STEPS + 1):
            t = (k / IDLE_STEPS) * IDLE_LEN
            ph = (t / 2.4) * 2 * math.pi + phase
            kfs.append(make_keyframe("rotation", t,
                                      x=3 * math.sin(ph),
                                      y=0,
                                      z=2 * math.cos(ph)))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}
    # Spoke micro-oscillation
    for si, (gu, g) in enumerate(spoke_bones):
        phase = si * 0.3
        kfs = []
        for k in range(IDLE_STEPS + 1):
            t = (k / IDLE_STEPS) * IDLE_LEN
            ph = (t / 2.6) * 2 * math.pi + phase
            kfs.append(make_keyframe("scale", t,
                                      x=1 + 0.015 * math.sin(ph),
                                      y=1.0,
                                      z=1 + 0.015 * math.sin(ph + 0.4)))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}
    # Rim slab subtle oscillation
    for ri, (gu, g, ang) in enumerate(rim_bones):
        phase = ri * 0.25
        kfs = []
        for k in range(IDLE_STEPS + 1):
            t = (k / IDLE_STEPS) * IDLE_LEN
            ph = (t / 2.8) * 2 * math.pi + phase
            kfs.append(make_keyframe("position", t,
                                      x=0,
                                      y=0.02 * math.sin(ph),
                                      z=0.02 * math.cos(ph)))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}
    # Ground shadows follow wheel position by oscillating in place
    for gi, (gu, g) in enumerate(ground_shadow_bones):
        phase = gi * 0.4
        kfs = []
        for k in range(IDLE_STEPS + 1):
            t = (k / IDLE_STEPS) * IDLE_LEN
            ph = (t / 3.0) * 2 * math.pi + phase
            s = 1.0 + 0.06 * math.sin(ph)
            kfs.append(make_keyframe("scale", t, x=s, y=1, z=s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}
    b.add_animation("idle", length=IDLE_LEN, animators=idle, loop="loop", override=False)

    # ---- DISSIPATE: 0.8s ----
    diss = {}
    # Rim chains fly off first
    for ci, (gu, g, ang) in enumerate(rim_chain_bones):
        fly_x = math.cos(ang) * 10
        fly_y = math.sin(ang) * 10
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.30, x=fly_x * 0.4, y=fly_y * 0.4, z=0),
            make_keyframe("position", 0.50, x=fly_x, y=fly_y, z=0),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.50, x=0, y=0, z=0),
        ]}
    # Spokes fly outward from hub
    for si, (gu, g) in enumerate(spoke_bones):
        ang = (si / 8.0) * 2 * math.pi
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.30, x=0, y=0, z=0),
            make_keyframe("position", 0.80, x=math.cos(ang) * 6, y=math.sin(ang) * 6, z=0),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.80, x=0, y=0, z=0),
        ]}
    # Rim pieces scatter
    for ri, (gu, g, ang) in enumerate(rim_bones):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", 0.40, x=0, y=0, z=0),
            make_keyframe("position", 0.80, x=math.cos(ang) * 7, y=math.sin(ang) * 7, z=0),
            make_keyframe("rotation", 0.0, x=0, y=0, z=math.degrees(ang) + 90),
            make_keyframe("rotation", 0.80, x=180, y=180, z=math.degrees(ang) + 90 + 360),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.80, x=0, y=0, z=0),
        ]}
    # Hub drops downward
    diss[hub_gu] = {"name": hub_g["name"], "type": "bone", "keyframes": [
        make_keyframe("position", 0.0, x=0, y=0, z=0),
        make_keyframe("position", 0.80, x=0, y=-8, z=0),
        make_keyframe("rotation", 0.0, x=0, y=0, z=0),
        make_keyframe("rotation", 0.80, x=360, y=540, z=720),
    ]}
    # Axle chain retracts upward
    for ai, (gu, g) in enumerate(axle_chain_bones):
        t_end = 0.60 + ai * 0.05
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, x=0, y=0, z=0),
            make_keyframe("position", t_end, x=0, y=6 + ai, z=0),
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", t_end, x=0, y=0, z=0),
        ]}
    # Ground shadow fades
    for gi, (gu, g) in enumerate(ground_shadow_bones):
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, x=1, y=1, z=1),
            make_keyframe("scale", 0.50, x=1.3, y=1, z=1.3),
            make_keyframe("scale", 0.80, x=0, y=0, z=0),
        ]}
    b.add_animation("dissipate", length=0.80, animators=diss, loop="once", override=True)

    b.write(OUTPUT)
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    build()
