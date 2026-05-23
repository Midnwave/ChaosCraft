#!/usr/bin/env python3
"""Generator for the_chain.bbmodel — Chain Mode FINALE attack #25 (Everything Bound).

THE SIGNATURE. The single colossal chain that binds everything. A 24-link
rising spiral around the caster from Y=0 to Y=12, where each link is offset
by 15° angularly and 0.5 vertically from the previous, creating the upward
spiral. The chain's character changes by height — bottom is RUST IRON, middle
is CURSED PURPLE, top is SPECTRAL SILVER. The three-zone diagonal texture
maps these palettes onto links via their UV placement.

This is the only ChaosCraft attack with a narrative beginning, middle, end:
spawn = binding being applied, idle = binding holding, dissipate = binding
broken and the three-colour burst.

Distinct identity vs all other modes — the chain that contains every chain
that ever existed.
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

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/chain/me_attacks/the_chain.bbmodel"


# ---------------------------------------------------------------------------
# TEXTURE 0 — THE GREAT CHAIN: 3-zone diagonal convergence (256x256)
# ---------------------------------------------------------------------------
# Zone 1 (diagonal 0-40): rust iron - bottom links sample this
# Zone 2 (diagonal 40-80): cursed iron - middle links sample this
# Zone 3 (diagonal 80+): spectral silver - top links sample this

def build_great_chain_texture():
    # 256x256 — the signature attack deserves the largest texture
    w = h = 256

    # Three palettes — every chain in the mode in one texture
    rust_base = hex_to_rgba("#3a1808")
    rust_main = hex_to_rgba("#6a2c10")
    rust_high = hex_to_rgba("#a04020")
    rust_edge = hex_to_rgba("#d06030")
    rust_emit = hex_to_rgba("#e08030")

    curse_base = hex_to_rgba("#1c0840")
    curse_main = hex_to_rgba("#3818a0")
    curse_high = hex_to_rgba("#5030b0")
    curse_edge = hex_to_rgba("#7050d0")
    curse_emit = hex_to_rgba("#a060d0")
    curse_brt = hex_to_rgba("#c080ff")

    spec_base = hex_to_rgba("#283848")
    spec_main = hex_to_rgba("#405868")
    spec_high = hex_to_rgba("#506070")
    spec_edge = hex_to_rgba("#7090a8")
    spec_emit = hex_to_rgba("#a0c0e0")

    pixels = base_fill(w, h, curse_main)

    # Diagonal zones — diagonal index = x + y
    diag_max = w + h - 2  # 510 for 256x256
    z1_max = diag_max * 0.4   # 204
    z2_max = diag_max * 0.7   # 357

    for y in range(h):
        for x in range(w):
            diag = x + y
            if diag < z1_max:
                # ZONE 1 — RUST IRON
                t = diag / z1_max
                # Rust gradient — darker at low diag, brighter at high
                if t < 0.5:
                    c = color_lerp(rust_base, rust_main, t * 2)
                else:
                    c = color_lerp(rust_main, rust_high, (t - 0.5) * 2)
            elif diag < z2_max:
                # ZONE 2 — CURSED IRON
                t = (diag - z1_max) / (z2_max - z1_max)
                if t < 0.5:
                    c = color_lerp(curse_base, curse_main, t * 2)
                else:
                    c = color_lerp(curse_main, curse_high, (t - 0.5) * 2)
            else:
                # ZONE 3 — SPECTRAL SILVER
                t = (diag - z2_max) / (diag_max - z2_max)
                if t < 0.5:
                    c = color_lerp(spec_base, spec_main, t * 2)
                else:
                    c = color_lerp(spec_main, spec_high, (t - 0.5) * 2)
            pixels[y * w + x] = c

    # Add diagonal transition shimmer between zones
    for y in range(h):
        for x in range(w):
            diag = x + y
            if abs(diag - z1_max) < 12:
                # Rust → cursed transition seam — orange purple mix
                seam_intensity = 1.0 - abs(diag - z1_max) / 12
                p = pixels[y * w + x]
                trans = color_lerp(rust_emit, curse_brt, 0.5)
                pixels[y * w + x] = color_lerp(p, trans, seam_intensity * 0.6)
            elif abs(diag - z2_max) < 12:
                # Cursed → spectral transition seam
                seam_intensity = 1.0 - abs(diag - z2_max) / 12
                p = pixels[y * w + x]
                trans = color_lerp(curse_brt, spec_emit, 0.5)
                pixels[y * w + x] = color_lerp(p, trans, seam_intensity * 0.6)

    # Heavy hammer marks (this is composite forged iron — every chain pattern in one)
    hammer_marks(pixels, w, h, rust_base, rust_edge, count=400, seed=801)
    hammer_marks(pixels, w, h, curse_base, curse_edge, count=400, seed=802)
    hammer_marks(pixels, w, h, spec_base, spec_edge, count=400, seed=803)

    # Horizontal grain across all zones
    horizontal_grain(pixels, w, h, rust_edge, density=0.4, seed=901)
    horizontal_grain(pixels, w, h, curse_edge, density=0.4, seed=902)
    horizontal_grain(pixels, w, h, spec_edge, density=0.4, seed=903)

    # CURSED EMIT — center of zone 2 (the brightest curse glow)
    # Zone 2 center is at diag ~280, so x+y ~280
    specular_cluster(pixels, w, h, 96, 184, 24, curse_brt, strength=0.65)
    specular_cluster(pixels, w, h, 184, 96, 24, curse_brt, strength=0.65)
    specular_cluster(pixels, w, h, 140, 140, 18, curse_brt, strength=0.55)

    # Rust emit — zone 1
    specular_cluster(pixels, w, h, 40, 40, 16, rust_emit, strength=0.55)
    specular_cluster(pixels, w, h, 60, 80, 10, rust_emit, strength=0.4)
    specular_cluster(pixels, w, h, 80, 60, 10, rust_emit, strength=0.4)

    # Spectral emit — zone 3
    specular_cluster(pixels, w, h, 215, 215, 16, spec_emit, strength=0.5)
    specular_cluster(pixels, w, h, 200, 240, 10, spec_emit, strength=0.4)
    specular_cluster(pixels, w, h, 240, 200, 10, spec_emit, strength=0.4)

    # Crack lines in each zone
    crack_lines_radial(pixels, w, h, 40, 40, branches=10, color=rust_edge, seed=701, max_len=70)
    crack_lines_radial(pixels, w, h, 128, 128, branches=14, color=curse_edge, seed=702, max_len=80)
    crack_lines_radial(pixels, w, h, 215, 215, branches=10, color=spec_edge, seed=703, max_len=70)

    # Rivet patches in each zone
    rivet_grid(pixels, w, h, rust_edge, rust_base, step_x=18, step_y=18, radius=2)

    # Rust blotch patches in zone 1
    rng_rust = random.Random(551)
    for _ in range(40):
        cx = rng_rust.randrange(0, 130)
        cy = rng_rust.randrange(0, 130 - cx)  # stay in zone 1 (diag < ~130)
        if cx + cy < z1_max - 5:
            r = rng_rust.randint(3, 8)
            for dy in range(-r, r + 1):
                for dx in range(-r, r + 1):
                    if dx * dx + dy * dy > r * r:
                        continue
                    x = cx + dx
                    y = cy + dy
                    if 0 <= x < w and 0 <= y < h:
                        d = math.sqrt(dx * dx + dy * dy)
                        t = d / r
                        p = pixels[y * w + x]
                        if t < 0.4:
                            pixels[y * w + x] = color_lerp(p, rust_high, 0.7)
                        else:
                            pixels[y * w + x] = color_lerp(p, rust_main, 0.5)

    # Cursed contamination blotches in zone 2
    rng_curse = random.Random(553)
    for _ in range(50):
        cx = rng_curse.randrange(40, 220)
        cy = rng_curse.randrange(40, 220)
        diag = cx + cy
        if z1_max < diag < z2_max:
            r = rng_curse.randint(4, 10)
            for dy in range(-r, r + 1):
                for dx in range(-r, r + 1):
                    if dx * dx + dy * dy > r * r:
                        continue
                    x = cx + dx
                    y = cy + dy
                    if 0 <= x < w and 0 <= y < h:
                        d = math.sqrt(dx * dx + dy * dy)
                        t = d / r
                        p = pixels[y * w + x]
                        if t < 0.35:
                            pixels[y * w + x] = color_lerp(p, curse_emit, 0.7)
                        else:
                            pixels[y * w + x] = color_lerp(p, curse_main, 0.55)

    # Spectral shimmer in zone 3
    rng_spec = random.Random(557)
    for _ in range(60):
        cx = rng_spec.randrange(120, w)
        cy = rng_spec.randrange(120, h)
        if cx + cy > z2_max:
            r = rng_spec.randint(2, 6)
            for dy in range(-r, r + 1):
                for dx in range(-r, r + 1):
                    if dx * dx + dy * dy > r * r:
                        continue
                    x = cx + dx
                    y = cy + dy
                    if 0 <= x < w and 0 <= y < h:
                        d = math.sqrt(dx * dx + dy * dy)
                        t = d / r
                        p = pixels[y * w + x]
                        pixels[y * w + x] = color_lerp(p, spec_emit, 0.5 * (1 - t))

    # Fine noise everywhere
    jitter_inplace(pixels, jitter_range=11, seed=1233, density=0.9)

    # Massive micro speckle — bulk + sparkle
    rng = random.Random(8001)
    for _ in range(4500):
        x = rng.randrange(w)
        y = rng.randrange(h)
        diag = x + y
        if diag < z1_max:
            color = rust_emit
        elif diag < z2_max:
            color = curse_brt
        else:
            color = spec_emit
        p = pixels[y * w + x]
        pixels[y * w + x] = color_lerp(p, color, 0.4)

    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# TEXTURE 1 — Binding glow + ground binding marks (256x256)
# ---------------------------------------------------------------------------
# Triple-colour radial: rust-orange + cursed purple + spectral silver meet at centre

def build_binding_texture():
    w = h = 256
    void = hex_to_rgba("#020208")

    rust_o = hex_to_rgba("#e06020")
    rust_mid = hex_to_rgba("#a04015")
    rust_dim = hex_to_rgba("#4a1808")

    curse_p = hex_to_rgba("#c050ff")
    curse_mid = hex_to_rgba("#6028a8")
    curse_dim = hex_to_rgba("#1c0840")

    spec_s = hex_to_rgba("#a0c8e8")
    spec_mid = hex_to_rgba("#608090")
    spec_dim = hex_to_rgba("#1a2838")

    pixels = base_fill(w, h, void)

    # Triple-colour radial — 3 lobes at 120° apart
    cx, cy = w // 2, h // 2
    for y in range(h):
        for x in range(w):
            dx, dy = x - cx, y - cy
            r = math.sqrt(dx * dx + dy * dy)
            ang = math.atan2(dy, dx)  # -pi to pi

            # Determine which lobe (0=rust, 1=curse, 2=spec)
            # Rust lobe centered at 90° (top), curse at 210° (lower-right), spec at 330° (lower-left)
            ang_deg = math.degrees(ang)
            if ang_deg < 0:
                ang_deg += 360
            # Distance to each lobe center
            d_rust = min(abs(ang_deg - 270), 360 - abs(ang_deg - 270))   # rust at 270 (top in image space)
            d_curse = min(abs(ang_deg - 30), 360 - abs(ang_deg - 30))
            d_spec = min(abs(ang_deg - 150), 360 - abs(ang_deg - 150))

            # Pick closest lobe
            min_d = min(d_rust, d_curse, d_spec)
            t_ang = min_d / 120.0  # 0 at lobe center, 1 at boundary
            t_ang = max(0, min(1, t_ang))

            r_norm = min(1.0, r / (w * 0.45))  # radial intensity

            if min_d == d_rust:
                lobe_color = rust_o
                lobe_mid = rust_mid
                lobe_dim = rust_dim
            elif min_d == d_curse:
                lobe_color = curse_p
                lobe_mid = curse_mid
                lobe_dim = curse_dim
            else:
                lobe_color = spec_s
                lobe_mid = spec_mid
                lobe_dim = spec_dim

            # Inner — bright lobe color blended toward void at edge
            if r_norm < 0.15:
                # Convergence center — white-hot
                c = color_lerp(hex_to_rgba("#ffffff"), lobe_color, r_norm / 0.15)
            elif r_norm < 0.5:
                c = color_lerp(lobe_color, lobe_mid, (r_norm - 0.15) / 0.35)
            else:
                c = color_lerp(lobe_mid, lobe_dim, (r_norm - 0.5) / 0.5)

            # Angular fade between lobes
            c = color_lerp(c, void, t_ang * 0.3 + r_norm * 0.2)

            pixels[y * w + x] = c

    # Add 8 ground binding circles concentric
    for r in (40, 60, 80, 100, 120):
        for ang_step in range(0, 360, 1):
            ang = math.radians(ang_step)
            x = int(cx + r * math.cos(ang))
            y = int(cy + r * math.sin(ang))
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, hex_to_rgba("#ffffff"), 0.25)

    # Crack lines spreading outward — split into 3 thirds (rust top, curse right, spec left)
    crack_lines_radial(pixels, w, h, cx, cy, branches=24, color=hex_to_rgba("#ff80c0"), seed=4001, max_len=110)

    # Bright spec clusters at lobe centers
    for lobe_ang in (270, 30, 150):
        ang_rad = math.radians(lobe_ang)
        for r_dist in (40, 80, 120):
            xp = int(cx + r_dist * math.cos(ang_rad))
            yp = int(cy + r_dist * math.sin(ang_rad))
            if 0 <= xp < w and 0 <= yp < h:
                if lobe_ang == 270:
                    color = rust_o
                elif lobe_ang == 30:
                    color = curse_p
                else:
                    color = spec_s
                specular_cluster(pixels, w, h, xp, yp, 8, color, strength=0.5)

    # Massive sparkle speckle
    rng = random.Random(11)
    for _ in range(2400):
        x = rng.randrange(w)
        y = rng.randrange(h)
        # Choose color based on angle from center
        dx, dy = x - cx, y - cy
        ang = math.atan2(dy, dx)
        ang_deg = math.degrees(ang) % 360
        if 210 <= ang_deg < 330 or ang_deg < 30 or ang_deg >= 330:
            tone = rust_o
        elif 30 <= ang_deg < 150:
            tone = curse_p
        else:
            tone = spec_s
        p = pixels[y * w + x]
        pixels[y * w + x] = color_lerp(p, tone, 0.4)

    jitter_inplace(pixels, jitter_range=14, seed=33, density=0.85)
    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# BUILD MODEL
# ---------------------------------------------------------------------------

def build():
    b = Builder("the_chain", resolution=(256, 256), visible_box=(28, 28, 0))

    # Face factories — UV samples 3 different texture zones
    # Zone 1 (rust): u+v < 100 (bottom-left of 256x256 tex)
    # Zone 2 (curse): u+v in middle
    # Zone 3 (spec): u+v > 200 (top-right)
    def t0_face_rust(u_off=0, v_off=0, sz=20):
        u = (10 + u_off) % 70
        v = (10 + v_off) % 70
        return uniform_face(u, v, u + sz, v + sz, tex_index=0)
    def t0_face_curse(u_off=0, v_off=0, sz=22):
        u = (90 + u_off) % 80 + 80
        v = (90 + v_off) % 80 + 80
        # Make sure u+v lies in cursed diagonal zone
        return uniform_face(u, v, u + sz, v + sz, tex_index=0)
    def t0_face_spec(u_off=0, v_off=0, sz=20):
        u = (170 + u_off) % 60 + 170
        v = (170 + v_off) % 60 + 170
        return uniform_face(u, v, u + sz, v + sz, tex_index=0)
    def t1_face():
        return uniform_face(0, 0, 256, 256, tex_index=1)
    def t1_face_alt(u, v, sz=32):
        return uniform_face(u, v, u + sz, v + sz, tex_index=1)

    # =====================================================================
    # THE GREAT SPIRAL — 24 chain links rising Y=0 to Y=12
    # Each link: angular offset 15°, vertical offset 0.5
    # =====================================================================
    spiral_links = []
    n_links = 24
    for li in range(n_links):
        ang = math.radians(li * 15)
        ly = li * 0.5
        # Spiral radius — slight contraction at top
        sr = 5.5 - (li / n_links) * 1.5  # 5.5 at base, ~4 at top
        cx = math.cos(ang) * sr
        cz = math.sin(ang) * sr

        # Link scale — larger at base, smaller toward top
        scale = 0.9 - (li / n_links) * 0.25

        # Pick face function by spiral height (zone)
        if li < 8:
            face_fn = lambda li_c=li: t0_face_rust(li_c * 3, li_c * 5, sz=22)
        elif li < 16:
            face_fn = lambda li_c=li: t0_face_curse(li_c * 3, li_c * 5, sz=24)
        else:
            face_fn = lambda li_c=li: t0_face_spec(li_c * 3, li_c * 5, sz=22)

        link_pair = chain_link_pair(
            b, f"spiral_link_{li+1}",
            cx=0, cy=0, cz=0, scale=scale,
            face_factory=face_fn,
        )
        # Add corner detail cubes on each link for extra geometry bulk + rounded feel
        extra_cubes = []
        # 4 corner studs
        corner_dirs = [(1, 1), (-1, 1), (1, -1), (-1, -1)]
        for ci, (cdx, cdy) in enumerate(corner_dirs):
            sx = cdx * scale * 0.55
            sy = cdy * scale * 0.55
            extra_cubes.append(b.add_cube(
                f"spiral_link_{li+1}_stud_{ci+1}",
                from_=[sx - 0.12, sy - 0.12, -0.12],
                to_=[sx + 0.12, sy + 0.12, 0.12],
                faces=face_fn(),
            ))
        link_uuids = list(link_pair) + extra_cubes
        link_group_uuid = make_uuid()
        # Tangent rotation — link faces along the spiral path direction
        tangent_y_deg = math.degrees(ang) + 90
        link_group = b.make_group(
            f"spiral_link_{li+1}",
            [cx, ly, cz],
            link_uuids,
            rotation=[15, tangent_y_deg, 0],
            group_uuid=link_group_uuid,
        )
        spiral_links.append((link_group_uuid, link_group, li))

    # =====================================================================
    # CHAIN TERMINUS — top link sealed with 3 accent pieces
    # =====================================================================
    terminus_cubes = []
    term_y = 12
    term_ang = math.radians(n_links * 15)
    term_r = 4
    term_x = math.cos(term_ang) * term_r
    term_z = math.sin(term_ang) * term_r
    # Main seal disc
    terminus_cubes.append(b.add_cube(
        "terminus_seal_disc",
        from_=[-0.9, -0.2, -0.9],
        to_=[0.9, 0.4, 0.9],
        faces=t0_face_spec(0, 0, sz=22),
    ))
    # Inner core
    terminus_cubes.append(b.add_cube(
        "terminus_inner_core",
        from_=[-0.5, 0.4, -0.5],
        to_=[0.5, 1.2, 0.5],
        faces=t0_face_spec(20, 20, sz=20),
    ))
    # Top spike
    terminus_cubes.append(b.add_cube(
        "terminus_top_spike",
        from_=[-0.25, 1.2, -0.25],
        to_=[0.25, 2.0, 0.25],
        faces=t0_face_spec(40, 40, sz=18),
    ))
    # 4 lock studs around the disc
    for si in range(4):
        sa = (si / 4.0) * 2 * math.pi
        sx_off = math.cos(sa) * 0.8
        sz_off = math.sin(sa) * 0.8
        terminus_cubes.append(b.add_cube(
            f"terminus_lock_stud_{si+1}",
            from_=[sx_off - 0.18, -0.05, sz_off - 0.18],
            to_=[sx_off + 0.18, 0.25, sz_off + 0.18],
            faces=t0_face_spec((si * 8) % 60, (si * 12) % 60, sz=14),
        ))
    terminus_uuid = make_uuid()
    terminus_group = b.make_group(
        "chain_terminus", [term_x, term_y, term_z], terminus_cubes,
        group_uuid=terminus_uuid,
    )

    # =====================================================================
    # SPIRAL ANCHOR POINTS — 6 flat slabs at Y=0 at chain emergence points
    # =====================================================================
    anchor_groups = []
    for ai in range(6):
        ang = (ai / 6.0) * 2 * math.pi
        ax_loc = math.cos(ang) * 5.8
        az_loc = math.sin(ang) * 5.8
        anchor_cubes = []
        # Main anchor plate
        anchor_cubes.append(b.add_cube(
            f"anchor_{ai+1}_plate",
            from_=[-0.8, 0.0, -0.55],
            to_=[0.8, 0.16, 0.55],
            faces=t1_face_alt((ai * 22) % 200, (ai * 30) % 200, sz=40),
        ))
        # Anchor cross
        anchor_cubes.append(b.add_cube(
            f"anchor_{ai+1}_cross",
            from_=[-0.18, 0.0, -0.85],
            to_=[0.18, 0.18, 0.85],
            faces=t1_face_alt((ai * 25) % 200, (ai * 33) % 200, sz=40),
        ))
        # Anchor stud center
        anchor_cubes.append(b.add_cube(
            f"anchor_{ai+1}_stud",
            from_=[-0.3, 0.0, -0.3],
            to_=[0.3, 0.35, 0.3],
            faces=t1_face_alt((ai * 18) % 200, (ai * 24) % 200, sz=36),
        ))
        au = make_uuid()
        ag = b.make_group(
            f"anchor_{ai+1}", [ax_loc, 0, az_loc], anchor_cubes,
            rotation=[0, math.degrees(ang), 0],
            group_uuid=au,
        )
        anchor_groups.append((au, ag))

    # =====================================================================
    # BINDING GLOW — 4 overlapping flat slabs at spiral center Y=6
    # =====================================================================
    binding_glow_cubes = []
    for bi in range(4):
        ba = (bi / 4.0) * 2 * math.pi
        bx_off = math.cos(ba) * 0.4
        bz_off = math.sin(ba) * 0.4
        # Each binding slab is a flat disc rotated to face outward
        binding_glow_cubes.append(b.add_cube(
            f"binding_glow_{bi+1}",
            from_=[bx_off - 1.5, 5.8, bz_off - 1.5],
            to_=[bx_off + 1.5, 6.2, bz_off + 1.5],
            faces=t1_face(),
            rotation=[0, math.degrees(ba), 0],
        ))
    # Central glow orb (3 nested cubes for depth)
    for gi in range(3):
        sz = 0.6 - gi * 0.15
        binding_glow_cubes.append(b.add_cube(
            f"binding_glow_core_{gi+1}",
            from_=[-sz, 6 - sz * 0.4, -sz],
            to_=[sz, 6 + sz * 0.4, sz],
            faces=t1_face(),
        ))
    binding_uuid = make_uuid()
    binding_group = b.make_group("binding_glow", [0, 0, 0], binding_glow_cubes, group_uuid=binding_uuid)

    # =====================================================================
    # GROUND BINDING MARKS — 8 flat slabs in a circle at Y=0
    # =====================================================================
    ground_groups = []
    for gi in range(8):
        ga = (gi / 8.0) * 2 * math.pi
        gx_loc = math.cos(ga) * 7
        gz_loc = math.sin(ga) * 7
        gm_cubes = []
        gm_cubes.append(b.add_cube(
            f"ground_mark_{gi+1}_plate",
            from_=[-1.0, 0.0, -0.7],
            to_=[1.0, 0.1, 0.7],
            faces=t1_face_alt((gi * 28) % 200, (gi * 20) % 200, sz=48),
        ))
        # Secondary ring around plate
        for rs in range(4):
            ra = (rs / 4.0) * 2 * math.pi
            rx_off = math.cos(ra) * 0.9
            rz_off = math.sin(ra) * 0.55
            gm_cubes.append(b.add_cube(
                f"ground_mark_{gi+1}_satellite_{rs+1}",
                from_=[rx_off - 0.18, 0.0, rz_off - 0.18],
                to_=[rx_off + 0.18, 0.12, rz_off + 0.18],
                faces=t1_face_alt((rs * 30) % 200, (rs * 22) % 200, sz=24),
            ))
        gu = make_uuid()
        gg = b.make_group(
            f"ground_mark_{gi+1}", [gx_loc, 0, gz_loc], gm_cubes,
            rotation=[0, math.degrees(ga), 0],
            group_uuid=gu,
        )
        ground_groups.append((gu, gg))

    # =====================================================================
    # ROOT
    # =====================================================================
    root_children = []
    for au, ag in anchor_groups: root_children.append(ag)
    root_children.append(binding_group)
    for lu, lg, li in spiral_links: root_children.append(lg)
    root_children.append(terminus_group)
    for gu, gg in ground_groups: root_children.append(gg)
    root_uuid = make_uuid()
    root_group = b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid)
    b.outliner.append(root_group)

    # =====================================================================
    # ANIMATIONS
    # =====================================================================

    # ----- SPAWN: 2.0s — the binding being applied -----
    spawn = {}
    # Ground binding marks appear at 0.0s
    for gi, (gu, gg) in enumerate(ground_groups):
        delay = 0.0 + gi * 0.01
        spawn[gu] = {"name": gg["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.15, 1.2, 1, 1.2),
            make_keyframe("scale", delay + 0.3, 1.0, 1, 1.0),
            make_keyframe("scale", 2.0, 1.0, 1, 1.0),
        ]}
    # Ground anchors materialise at 0.2s
    for ai, (au, ag) in enumerate(anchor_groups):
        delay = 0.2 + ai * 0.015
        spawn[au] = {"name": ag["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.15, 1.1, 1.1, 1.1),
            make_keyframe("scale", delay + 0.3, 1.0, 1.0, 1.0),
            make_keyframe("scale", 2.0, 1.0, 1.0, 1.0),
        ]}
    # Chain links appear one by one — bottom to top — 0.07s stagger
    for li_idx, (lu, lg, li) in enumerate(spiral_links):
        link_delay = 0.2 + li * 0.07
        spawn[lu] = {"name": lg["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", link_delay, 0, 0, 0),
            make_keyframe("scale", link_delay + 0.1, 1.25, 1.25, 1.25),
            make_keyframe("scale", link_delay + 0.22, 1.0, 1.0, 1.0),
            make_keyframe("scale", 2.0, 1.0, 1.0, 1.0),
        ]}
    # Binding glow materialises at 1.0s (when chain passes Y=6)
    spawn[binding_uuid] = {"name": "binding_glow", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 0, 0, 0),
        make_keyframe("scale", 1.0, 0, 0, 0),
        make_keyframe("scale", 1.25, 1.3, 1.3, 1.3),
        make_keyframe("scale", 1.45, 1.0, 1.0, 1.0),
        make_keyframe("scale", 2.0, 1.0, 1.0, 1.0),
    ]}
    # Terminus seals at 1.9s — the final snap
    spawn[terminus_uuid] = {"name": "chain_terminus", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 0, 0, 0),
        make_keyframe("scale", 1.9, 0, 0, 0),
        make_keyframe("scale", 1.95, 1.5, 1.5, 1.5),
        make_keyframe("scale", 2.0, 1.0, 1.0, 1.0),
    ]}
    b.add_animation("spawn", length=2.0, animators=spawn, loop="once", override=True)

    # ----- IDLE: 6.0s loop — chain rotates slowly, links oscillate slightly -----
    idle = {}
    # Each spiral link slightly oscillates ±1° rotation
    for li_idx, (lu, lg, li) in enumerate(spiral_links):
        phase = li * 0.4
        idle[lu] = {"name": lg["name"], "type": "bone", "keyframes": [
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 1.5, 0, 0, math.sin(phase) * 1.5),
            make_keyframe("rotation", 3.0, 0, 0, math.sin(phase + 1.5) * 1.5),
            make_keyframe("rotation", 4.5, 0, 0, math.sin(phase + 3.0) * 1.5),
            make_keyframe("rotation", 6.0, 0, 0, math.sin(phase + 4.5) * 1.5),
        ]}
    # Binding glow pulses
    idle[binding_uuid] = {"name": "binding_glow", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 1, 1, 1),
        make_keyframe("scale", 1.0, 1.12, 1, 1.12),
        make_keyframe("scale", 2.0, 1.0, 1, 1.0),
        make_keyframe("scale", 3.0, 1.18, 1, 1.18),
        make_keyframe("scale", 4.0, 1.0, 1, 1.0),
        make_keyframe("scale", 5.0, 1.12, 1, 1.12),
        make_keyframe("scale", 6.0, 1.0, 1, 1.0),
        make_keyframe("rotation", 0.0, 0, 0, 0),
        make_keyframe("rotation", 6.0, 0, 360, 0),
    ]}
    # Ground marks rotate slowly
    for gi, (gu, gg) in enumerate(ground_groups):
        idle[gu] = {"name": gg["name"], "type": "bone", "keyframes": [
            make_keyframe("rotation", 0.0, 0, 0, 0),
            make_keyframe("rotation", 3.0, 0, 30, 0),
            make_keyframe("rotation", 6.0, 0, 60, 0),
        ]}
    # Terminus pulses
    idle[terminus_uuid] = {"name": "chain_terminus", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 1, 1, 1),
        make_keyframe("scale", 1.5, 1.05, 1.05, 1.05),
        make_keyframe("scale", 3.0, 1.0, 1.0, 1.0),
        make_keyframe("scale", 4.5, 1.05, 1.05, 1.05),
        make_keyframe("scale", 6.0, 1.0, 1.0, 1.0),
        make_keyframe("rotation", 0.0, 0, 0, 0),
        make_keyframe("rotation", 6.0, 0, 360, 0),
    ]}
    b.add_animation("idle", length=6.0, animators=idle, loop="loop", override=False)

    # ----- DISSIPATE: 1.5s — chain unravels top-to-bottom, three-colour burst -----
    diss = {}
    # Terminus breaks first
    diss[terminus_uuid] = {"name": "chain_terminus", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 1, 1, 1),
        make_keyframe("scale", 0.1, 1.4, 1.4, 1.4),  # snap outward
        make_keyframe("scale", 0.2, 0, 0, 0),
    ]}
    # Links vanish top-to-bottom (reverse of spawn)
    for li_idx, (lu, lg, li) in enumerate(spiral_links):
        # Top link disappears first, bottom last
        delay = 0.1 + (n_links - 1 - li) * 0.04
        diss[lu] = {"name": lg["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", delay, 1, 1, 1),
            make_keyframe("scale", delay + 0.1, 1.3, 1.3, 1.3),  # flash before vanish
            make_keyframe("scale", delay + 0.2, 0, 0, 0),
            make_keyframe("scale", 1.5, 0, 0, 0),
        ]}
    # Binding glow expands and releases — three-colour burst
    diss[binding_uuid] = {"name": "binding_glow", "type": "bone", "keyframes": [
        make_keyframe("scale", 0.0, 1, 1, 1),
        make_keyframe("scale", 0.3, 1.5, 1.5, 1.5),
        make_keyframe("scale", 0.6, 3.0, 1.5, 3.0),
        make_keyframe("scale", 1.0, 4.5, 1.5, 4.5),
        make_keyframe("scale", 1.5, 0, 0, 0),
    ]}
    # Ground marks expand and snap
    for gi, (gu, gg) in enumerate(ground_groups):
        delay = 0.5 + gi * 0.01
        diss[gu] = {"name": gg["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", delay, 1, 1, 1),
            make_keyframe("scale", delay + 0.2, 1.8, 1, 1.8),
            make_keyframe("scale", delay + 0.4, 0, 1, 0),
            make_keyframe("scale", 1.5, 0, 1, 0),
        ]}
    # Anchors retract
    for ai, (au, ag) in enumerate(anchor_groups):
        delay = 1.0 + ai * 0.02
        diss[au] = {"name": ag["name"], "type": "bone", "keyframes": [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", delay, 1, 1, 1),
            make_keyframe("scale", 1.5, 0, 0, 0),
        ]}
    b.add_animation("dissipate", length=1.5, animators=diss, loop="once", override=True)

    # =====================================================================
    # TEXTURES
    # =====================================================================
    b.add_texture("the_chain_great", build_great_chain_texture())
    b.add_texture("the_chain_binding", build_binding_texture())

    return b


if __name__ == "__main__":
    builder = build()
    out = builder.write(OUTPUT_PATH)
    import os
    size = os.path.getsize(out)
    print(f"Wrote {out}")
    print(f"Size: {size} bytes ({size/1024:.1f} KB)")
