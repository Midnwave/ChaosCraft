#!/usr/bin/env python3
"""Generator for cursed_pendulum_array.bbmodel — Chain Mode attack #23 (Swing).

Visual intent: 4 massive cursed pendulums swinging in different planes around
the caster. Each pendulum: a heavy spiked weight on a long cursed chain. The
4 pendulums have different swing periods, creating constant crossing danger.

Distinct identity: precision pendulum iron (heavier, denser than regular
chain iron) + cursed-purple emissive seams pulsing along the chain. Ground
arcs traced by each pendulum's burnt energy passage.
"""

import sys
import math
import random

sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder,
    make_uuid,
    make_keyframe,
    basic_cube_faces,
    uniform_face,
    png_from_pixels,
    hex_to_rgba,
    color_lerp,
    fill_rect,
)
from _chain_helpers import (
    base_fill,
    jitter_inplace,
    rust_patches,
    hammer_marks,
    rivet_grid,
    horizontal_grain,
    crack_lines_radial,
    specular_cluster,
    emissive_seam,
    chain_link_pair,
)

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/chain/me_attacks/cursed_pendulum_array.bbmodel"


# ---------------------------------------------------------------------------
# TEXTURE 0 — Precision pendulum iron + cursed emissive (160x160)
# ---------------------------------------------------------------------------

def build_pendulum_iron_texture():
    w = h = 160
    iron_dark = hex_to_rgba("#0e0c0c")
    iron_weighted = hex_to_rgba("#201810")
    iron_surf = hex_to_rgba("#404030")
    iron_edge = hex_to_rgba("#706850")
    curse_dark = hex_to_rgba("#2a1058")
    curse_main = hex_to_rgba("#6030a0")
    curse_emit = hex_to_rgba("#a060e0")

    pixels = base_fill(w, h, iron_dark)

    # Vertical gradient bands — top is heavier weighted dark, bottom edge-bright
    for y in range(h):
        t = y / float(h - 1)
        if t < 0.4:
            top, bot = iron_dark, iron_weighted
            lt = t / 0.4
        elif t < 0.75:
            top, bot = iron_weighted, iron_surf
            lt = (t - 0.4) / 0.35
        else:
            top, bot = iron_surf, iron_edge
            lt = (t - 0.75) / 0.25
        c = color_lerp(top, bot, lt)
        for x in range(w):
            pixels[y * w + x] = c

    # Heavy hammer marks (this is forged precision iron — many marks)
    hammer_marks(pixels, w, h, iron_dark, iron_edge, count=500, seed=131)

    # Horizontal grain
    horizontal_grain(pixels, w, h, iron_edge, density=0.55, seed=331)

    # Rivet grid (heavy bolts on pendulum weight)
    rivet_grid(pixels, w, h, iron_edge, iron_dark, step_x=16, step_y=16, radius=2)

    # CURSED ENERGY conducting along the chain — vertical seams + glow
    for sx in (12, 36, 60, 84, 108, 132, 156):
        emissive_seam(pixels, w, h, sx, curse_main, intensity=0.6, width=2)

    # Curse contamination blotches
    rust_patches(pixels, w, h, curse_dark, curse_main, count=28, seed=509)

    # Curse pool clusters (the brightest cursed energy points)
    specular_cluster(pixels, w, h, 80, 80, 16, curse_emit, strength=0.55)
    specular_cluster(pixels, w, h, 40, 120, 10, curse_emit, strength=0.4)
    specular_cluster(pixels, w, h, 120, 40, 10, curse_emit, strength=0.4)
    specular_cluster(pixels, w, h, 24, 24, 6, curse_emit, strength=0.3)
    specular_cluster(pixels, w, h, 136, 136, 6, curse_emit, strength=0.3)

    # Curse crack lines radiating
    crack_lines_radial(pixels, w, h, 80, 80, branches=14, color=curse_main, seed=911, max_len=55)

    # Fine grain
    jitter_inplace(pixels, jitter_range=11, seed=42, density=0.9)

    # Bright micro speckles — curse sparks
    rng = random.Random(151)
    for _ in range(1400):
        x = rng.randrange(w)
        y = rng.randrange(h)
        p = pixels[y * w + x]
        pixels[y * w + x] = color_lerp(p, curse_emit, 0.35)

    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# TEXTURE 1 — Ground swing arcs w/ cursed energy trail (160x160)
# ---------------------------------------------------------------------------

def build_swing_arc_texture():
    w = h = 160
    ground_dark = hex_to_rgba("#060604")
    arc_dim = hex_to_rgba("#28104a")
    arc_main = hex_to_rgba("#5020a0")
    arc_bright = hex_to_rgba("#8050d0")

    pixels = base_fill(w, h, ground_dark)

    # Several curved arcs across the texture — pendulum trail records
    cx, cy = w // 2, h // 2
    for arc_i in range(5):
        radius = 30 + arc_i * 18
        thickness = 3
        # Half-arc only, varying start angle
        start_ang = arc_i * 0.4
        for ang_step in range(0, 180, 1):
            ang = math.radians(ang_step) + start_ang
            for t in range(-thickness, thickness + 1):
                x = int(cx + (radius + t) * math.cos(ang))
                y = int(cy + (radius + t) * math.sin(ang))
                if 0 <= x < w and 0 <= y < h:
                    dist_factor = abs(t) / float(thickness + 1)
                    p = pixels[y * w + x]
                    if dist_factor < 0.4:
                        pixels[y * w + x] = color_lerp(p, arc_bright, 0.85)
                    elif dist_factor < 0.7:
                        pixels[y * w + x] = color_lerp(p, arc_main, 0.7)
                    else:
                        pixels[y * w + x] = color_lerp(p, arc_dim, 0.5)

    # Burn streaks along arc paths
    crack_lines_radial(pixels, w, h, cx, cy, branches=18, color=arc_main, seed=77, max_len=72)

    # Curse smolder pools
    rust_patches(pixels, w, h, arc_dim, arc_main, count=20, seed=801)

    # Bright burn centres
    specular_cluster(pixels, w, h, cx, cy, 14, arc_bright, strength=0.7)
    specular_cluster(pixels, w, h, 30, 50, 6, arc_bright, strength=0.4)
    specular_cluster(pixels, w, h, 130, 110, 6, arc_bright, strength=0.4)
    specular_cluster(pixels, w, h, 40, 130, 5, arc_bright, strength=0.35)

    # Smolder speckles
    rng = random.Random(901)
    for _ in range(1100):
        x = rng.randrange(w)
        y = rng.randrange(h)
        p = pixels[y * w + x]
        pixels[y * w + x] = color_lerp(p, arc_bright, 0.3)

    jitter_inplace(pixels, jitter_range=10, seed=88, density=0.7)
    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# PENDULUM BUILDER
# ---------------------------------------------------------------------------

def build_pendulum_weight(b, prefix, face_fn):
    """Heavy pendulum mass — 4 cubes + 4 spike bones. Returns (weight_cube_uuids, spike_group_uuids)."""
    weight_cubes = []
    # Central core (dense cube)
    weight_cubes.append(b.add_cube(
        f"{prefix}_core",
        from_=[-1.2, -1.0, -1.2],
        to_=[1.2, 1.0, 1.2],
        faces=face_fn(),
    ))
    # Top hemisphere (slightly smaller)
    weight_cubes.append(b.add_cube(
        f"{prefix}_top_hemi",
        from_=[-1.0, 0.8, -1.0],
        to_=[1.0, 1.6, 1.0],
        faces=face_fn(),
    ))
    # Bottom point (taper)
    weight_cubes.append(b.add_cube(
        f"{prefix}_bot_taper",
        from_=[-0.7, -1.45, -0.7],
        to_=[0.7, -0.9, 0.7],
        faces=face_fn(),
    ))
    # Cap below taper (tip)
    weight_cubes.append(b.add_cube(
        f"{prefix}_tip",
        from_=[-0.35, -1.8, -0.35],
        to_=[0.35, -1.4, 0.35],
        faces=face_fn(),
    ))

    # 4 spike bones (each its own group via spike_uuids list — caller wraps them)
    spike_groups = []
    spike_dirs = [(1, 0), (-1, 0), (0, 1), (0, -1)]
    for si, (sx, sz) in enumerate(spike_dirs):
        s_cubes = []
        # Spike base
        s_cubes.append(b.add_cube(
            f"{prefix}_spike_{si+1}_base",
            from_=[sx * 1.1 - 0.25, -0.3, sz * 1.1 - 0.25],
            to_=[sx * 1.4 + 0.25, 0.3, sz * 1.4 + 0.25],
            faces=face_fn(),
        ))
        # Spike tip
        s_cubes.append(b.add_cube(
            f"{prefix}_spike_{si+1}_tip",
            from_=[sx * 1.6 - 0.15, -0.15, sz * 1.6 - 0.15],
            to_=[sx * 2.0 + 0.15, 0.15, sz * 2.0 + 0.15],
            faces=face_fn(),
        ))
        gu = make_uuid()
        g = b.make_group(f"{prefix}_spike_{si+1}", [0, 0, 0], s_cubes, group_uuid=gu)
        spike_groups.append((gu, g))

    return weight_cubes, spike_groups


# ---------------------------------------------------------------------------
# BUILD MODEL
# ---------------------------------------------------------------------------

def build():
    b = Builder("cursed_pendulum_array", resolution=(160, 160), visible_box=(28, 20, 0))

    def t0_face():
        return uniform_face(0, 0, 160, 160, tex_index=0)
    def t0_alt(u, v, sz=22):
        return uniform_face(u, v, u + sz, v + sz, tex_index=0)
    def t1_face():
        return uniform_face(0, 0, 160, 160, tex_index=1)

    # 4 pendulums: positions around caster at different angles
    # Each pendulum's pivot is at Y=14 at angular position around origin
    pendulum_specs = [
        # (pivot_x, pivot_z, swing_axis_y_deg, name, swing_period, start_phase)
        ( 0,  9, 0,   "pend_n", 2.0, 0.0),
        ( 9,  0, 90,  "pend_e", 2.8, 0.5),
        ( 0, -9, 180, "pend_s", 3.4, 1.0),
        (-9,  0, 270, "pend_w", 2.2, 1.5),
    ]

    pendulum_units = []  # each: (parent_uuid, parent_group, chain_uuids, weight_uuid, weight_group)
    arc_groups = []
    energy_groups = []

    for px, pz, swing_ax, name, period, phase in pendulum_specs:
        # CHAIN — 6 chain links vertical from Y=14 (pivot) down to Y=6 (weight)
        chain_link_uuids = []
        n_links = 6
        for li in range(n_links):
            ly = 14 - (li + 1) * 1.3  # 12.7, 11.4, 10.1, 8.8, 7.5, 6.2
            link_pair = chain_link_pair(
                b, f"{name}_link_{li+1}",
                cx=0, cy=ly, cz=0, scale=0.55,
                face_factory=lambda li_capture=li: t0_alt(
                    (li_capture * 11) % 140, (li_capture * 17) % 140, sz=18,
                ),
            )
            chain_link_uuids.extend(link_pair)
            # connector bar
            if li < n_links - 1:
                chain_link_uuids.append(b.add_cube(
                    f"{name}_link_connector_{li+1}",
                    from_=[-0.1, ly - 0.8, -0.1],
                    to_=[0.1, ly - 0.4, 0.1],
                    faces=t0_alt((li * 13) % 140, (li * 19) % 140, sz=10),
                ))

        # CURSE ENERGY pieces — 8 small bones along chain
        curse_cubes_per_chain = []
        for ei in range(8):
            ey = 13.5 - ei * 1.0
            ec_cubes = [b.add_cube(
                f"{name}_curse_{ei+1}",
                from_=[-0.22, ey - 0.18, -0.22],
                to_=[0.22, ey + 0.18, 0.22],
                faces=t0_alt((ei * 23) % 140, (ei * 29) % 140, sz=12),
            )]
            cgu = make_uuid()
            cgr = b.make_group(f"{name}_curse_{ei+1}", [0, ey, 0], ec_cubes, group_uuid=cgu)
            curse_cubes_per_chain.append((cgu, cgr))

        # WEIGHT — at Y=6 with spikes
        weight_cubes, spike_groups = build_pendulum_weight(b, name + "_weight", t0_face)
        weight_group_uuid = make_uuid()
        weight_children = list(weight_cubes) + [sg for (gu, sg) in spike_groups]
        weight_group = b.make_group(
            f"{name}_weight", [0, 6, 0], weight_children, group_uuid=weight_group_uuid,
        )

        # Assemble the pendulum: chain + curses + weight all share a common parent at the pivot
        pendulum_inner_children = list(chain_link_uuids) + [cgr for (cgu, cgr) in curse_cubes_per_chain] + [weight_group]
        # The pivot-relative inner group at origin
        inner_uuid = make_uuid()
        inner_group = b.make_group(
            f"{name}_inner", [0, 0, 0], pendulum_inner_children, group_uuid=inner_uuid,
        )
        # The pivot parent — positioned at world (px, 14, pz). This is what rotates.
        # However we also want it Y-rotated to face its swing plane: swing_ax_y_deg
        pivot_uuid = make_uuid()
        pivot_group = b.make_group(
            f"{name}_pivot", [px, 14, pz], [inner_group],
            rotation=[0, swing_ax, 0],
            group_uuid=pivot_uuid,
        )
        pendulum_units.append((pivot_uuid, pivot_group, inner_uuid, inner_group,
                                weight_group_uuid, weight_group,
                                name, period, phase,
                                [cgu for (cgu, _) in curse_cubes_per_chain]))

        # GROUND SWING ARC — curved flat slab piece beneath each pendulum
        # 9 slabs arranged in an arc beneath the swing path
        arc_cubes = []
        arc_radius = 7
        for ai in range(9):
            arc_ang = math.radians((ai - 4) * 8)  # +/- 32 degrees
            ax_loc = math.sin(arc_ang) * arc_radius
            az_loc = math.cos(arc_ang) * arc_radius - 7
            arc_cubes.append(b.add_cube(
                f"{name}_arc_slab_{ai+1}",
                from_=[ax_loc - 0.6, -0.08, az_loc - 0.5],
                to_=[ax_loc + 0.6, 0.08, az_loc + 0.5],
                faces=t1_face(),
                rotation=[0, math.degrees(arc_ang), 0],
            ))
        # Arc group positioned at (px, 0, pz), Y-rotated to face same plane
        agu = make_uuid()
        ag = b.make_group(
            f"{name}_arc", [px, 0, pz], arc_cubes,
            rotation=[0, swing_ax, 0],
            group_uuid=agu,
        )
        arc_groups.append((agu, ag, name))

    # =====================================================================
    # ROOT
    # =====================================================================
    root_children = []
    for pu, pg, iu, ig, wu, wg, nm, per, ph, ce in pendulum_units: root_children.append(pg)
    for au, ag, nm in arc_groups: root_children.append(ag)
    root_uuid = make_uuid()
    root_group = b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid)
    b.outliner.append(root_group)

    # =====================================================================
    # ANIMATIONS
    # =====================================================================

    # ----- SPAWN: 1.2s — chains lower top-to-bottom, weights appear at bottom -----
    spawn = {}
    for idx, (pu, pg, iu, ig, wu, wg, nm, per, ph, ce) in enumerate(pendulum_units):
        # Inner group descends from above (chain lowering effect)
        spawn[iu] = {"name": ig["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, 0, 6, 0),
            make_keyframe("position", 0.1 + idx * 0.02, 0, 6, 0),
            make_keyframe("position", 0.6, 0, 0, 0),
            make_keyframe("position", 1.2, 0, 0, 0),
        ]}
        # Weight scales in at 0.6s
        spawn[wu] = {"name": wg["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", 0.6, 0, 0, 0),
            make_keyframe("scale", 0.75, 1.2, 1.2, 1.2),
            make_keyframe("scale", 0.9, 1.0, 1.0, 1.0),
            make_keyframe("scale", 1.2, 1.0, 1.0, 1.0),
        ]}
        # Pivot begins gentle swing from spawn start phase
        start_swing_ang = math.sin(ph * 0.5) * 15.0
        spawn[pu] = {"name": pg["name"], "type": "bone", "keyframes": [
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 1.0, 0, 0, start_swing_ang * 0.5),
            make_keyframe("rotation", 1.2, 0, 0, start_swing_ang),
        ]}
    # Arcs appear at 0.5s
    for ai, (au, ag, nm) in enumerate(arc_groups):
        spawn[au] = {"name": ag["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", 0.5, 0, 0, 0),
            make_keyframe("scale", 0.7, 1.1, 1, 1.1),
            make_keyframe("scale", 0.85, 1.0, 1, 1.0),
            make_keyframe("scale", 1.2, 1.0, 1, 1.0),
        ]}
    b.add_animation("spawn", length=1.2, animators=spawn, loop="once", override=True)

    # ----- IDLE: 4.0s loop — each pendulum oscillates at its period -----
    idle = {}
    for idx, (pu, pg, iu, ig, wu, wg, nm, per, ph, ce) in enumerate(pendulum_units):
        # Z-rotation oscillation on pivot — swing amplitude ±30°
        # Compute number of swing cycles in 4.0s
        cycles = 4.0 / per
        keys = []
        n_samples = 24
        for k in range(n_samples + 1):
            t = (k / n_samples) * 4.0
            phase_t = ph + (t / per) * 2 * math.pi
            ang = math.sin(phase_t) * 30.0
            keys.append(make_keyframe("rotation", t, 0, 0, ang))
        idle[pu] = {"name": pg["name"], "type": "bone", "keyframes": keys}

        # Inner group has small Z-lag (chain lag behind weight)
        lag_keys = []
        for k in range(n_samples + 1):
            t = (k / n_samples) * 4.0
            phase_t = ph + (t / per) * 2 * math.pi - 0.4  # phase lag
            ang = math.sin(phase_t) * 4.0
            lag_keys.append(make_keyframe("rotation", t, 0, 0, ang))
        idle[iu] = {"name": ig["name"], "type": "bone", "keyframes": lag_keys}

        # Curse energy pulse — slight scale oscillation on each piece
        for ci, cgu in enumerate(ce):
            pulse_keys = []
            n_p = 12
            for k in range(n_p + 1):
                t = (k / n_p) * 4.0
                phase_t = ph + ci * 0.3 + (t / per) * 2 * math.pi
                s = 1.0 + 0.15 * math.sin(phase_t)
                pulse_keys.append(make_keyframe("scale", t, s, s, s))
            idle[cgu] = {"name": f"{nm}_curse_{ci+1}", "type": "bone", "keyframes": pulse_keys}

    # Ground arcs pulse gently
    for ai, (au, ag, nm) in enumerate(arc_groups):
        idle[au] = {"name": ag["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 1.0, 1.04, 1, 1.04),
            make_keyframe("scale", 2.0, 1.0, 1, 1.0),
            make_keyframe("scale", 3.0, 1.04, 1, 1.04),
            make_keyframe("scale", 4.0, 1.0, 1, 1.0),
        ]}
    b.add_animation("idle", length=4.0, animators=idle, loop="loop", override=False)

    # ----- DISSIPATE: 0.9s — pendulums swing to max amplitude, then chains retract -----
    diss = {}
    for idx, (pu, pg, iu, ig, wu, wg, nm, per, ph, ce) in enumerate(pendulum_units):
        max_amp = 45.0 if idx % 2 == 0 else -45.0
        diss[pu] = {"name": pg["name"], "type": "bone", "keyframes": [
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 0.3, 0, 0, max_amp),
            make_keyframe("rotation", 0.5, 0, 0, max_amp * 0.6),
            make_keyframe("rotation", 0.9, 0, 0, 0),
        ]}
        # Chain retracts upward — inner Y position rises
        diss[iu] = {"name": ig["name"], "type": "bone", "keyframes": [
            make_keyframe("position", 0.0, 0, 0, 0),
            make_keyframe("position", 0.4, 0, 0, 0),
            make_keyframe("position", 0.9, 0, 8, 0),
        ]}
        # Weight scales to 0 last
        diss[wu] = {"name": wg["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.6, 1, 1, 1),
            make_keyframe("scale", 0.9, 0, 0, 0),
        ]}
    for ai, (au, ag, nm) in enumerate(arc_groups):
        diss[au] = {"name": ag["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.4, 1, 1, 1),
            make_keyframe("scale", 0.9, 0, 1, 0),
        ]}
    b.add_animation("dissipate", length=0.9, animators=diss, loop="once", override=True)

    # =====================================================================
    # TEXTURES
    # =====================================================================
    b.add_texture("pendulum_iron", build_pendulum_iron_texture())
    b.add_texture("swing_arcs", build_swing_arc_texture())

    return b


if __name__ == "__main__":
    builder = build()
    out = builder.write(OUTPUT_PATH)
    import os
    size = os.path.getsize(out)
    print(f"Wrote {out}")
    print(f"Size: {size} bytes ({size/1024:.1f} KB)")
