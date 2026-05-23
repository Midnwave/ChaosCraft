#!/usr/bin/env python3
"""Generator for phantom_chain_collapse.bbmodel — Weight of Souls (Attack 10).

A sphere of phantom chains COLLAPSES INWARD — not an explosion outward but
COMPRESSION INWARD, like gravity made of chains. Identity = INWARD COLLAPSE
(both on spawn AND dissipate), cold spectral silver-grey.

Geometry:
  - 3 shell rings of chain links at Y=8:
    outer (18 links, radius 8), mid (14 links, radius 6), inner (10 links, radius 4)
  - 8 phantom chain strands running from outer shell to inner shell
  - 6 overlapping flat slabs at centre (collapse point) — tex 1
  - 10 ground collapse ring slabs at Y=0
"""

import sys
import math

sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder,
    make_uuid,
    make_keyframe,
    uniform_face,
    png_from_pixels,
    hex_to_rgba,
    color_lerp,
    add_noise_overlay,
)
from _chain_common import (
    add_chain_link,
    base_fill,
    jitter_inplace,
    chain_link_silhouette,
    radial_glow,
)


OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/chain/me_attacks/phantom_chain_collapse.bbmodel"
RES = 128
SHELL_Y = 8.0


def build_phantom_chain_texture():
    """PHANTOM CHAIN TECHNIQUE — chains made of spectral matter, halfway between
    iron and nothing. Base #060410, phantom navy #101828, ghost chain #282840,
    spectral silver #505868, edge glow #8090a0. Soft edges everywhere.
    Emissive: #6070a0 cold spectral silver-blue.
    """
    w = h = RES
    spectral_void = hex_to_rgba("#060410")
    phantom_navy  = hex_to_rgba("#101828")
    ghost_chain   = hex_to_rgba("#282840")
    spec_silver   = hex_to_rgba("#505868")
    edge_glow     = hex_to_rgba("#8090a0")
    emissive      = hex_to_rgba("#6070a0")
    super_glow    = hex_to_rgba("#a0b0c8")

    pixels = base_fill(w, h, spectral_void)

    cx, cy = w // 2, h // 2
    # Soft radial gradient — brighter at centre (where the collapse happens),
    # darker at corners
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            max_r = w * 0.62
            t = min(1.0, d / max_r)
            if t < 0.20:
                c = color_lerp(super_glow, edge_glow, t / 0.20)
            elif t < 0.45:
                c = color_lerp(edge_glow, spec_silver, (t - 0.20) / 0.25)
            elif t < 0.75:
                c = color_lerp(spec_silver, ghost_chain, (t - 0.45) / 0.30)
            else:
                c = color_lerp(ghost_chain, phantom_navy, (t - 0.75) / 0.25)
            pixels[y * w + x] = c

    # Soft phantom chain silhouettes — many overlapping, ghostly
    chain_link_silhouette(pixels, w, h, spec_silver, super_glow, count=8, seed=23)
    chain_link_silhouette(pixels, w, h, ghost_chain, edge_glow, count=6, seed=43)

    # Soft "dissolving" feathered edges — random pixel noise pulled toward base
    import random
    rng = random.Random(73)
    for _ in range(300):
        x = rng.randrange(w)
        y = rng.randrange(h)
        p = pixels[y * w + x]
        # Pull a fraction toward emissive (soft halo effect)
        pixels[y * w + x] = color_lerp(p, emissive, rng.uniform(0.15, 0.45))

    # Vertical phantom streaks — chains dissolving upward
    for _ in range(28):
        x = rng.randrange(w)
        length = rng.randint(10, 40)
        y0 = rng.randrange(h)
        for d in range(length):
            y = (y0 + d) % h
            for dx in (-1, 0, 1):
                xx = (x + dx) % w
                p = pixels[y * w + xx]
                intensity = 0.4 if dx == 0 else 0.18
                intensity *= 1.0 - (d / length) * 0.6
                pixels[y * w + xx] = color_lerp(p, edge_glow, intensity)

    # Concentric phantom rings (extremely soft)
    for ring_r in [16, 28, 40, 52]:
        for ang_step in range(360):
            ang = math.radians(ang_step)
            for offs in (-1, 0, 1):
                rr = ring_r + offs
                x = int(cx + math.cos(ang) * rr)
                y = int(cy + math.sin(ang) * rr)
                if 0 <= x < w and 0 <= y < h:
                    p = pixels[y * w + x]
                    intensity = 0.32 if offs == 0 else 0.16
                    pixels[y * w + x] = color_lerp(p, emissive, intensity)

    # Per-pixel jitter — feather everything
    jitter_inplace(pixels, jitter_range=18, seed=131)
    jitter_inplace(pixels, jitter_range=12, seed=181, density=0.55)
    add_noise_overlay(pixels, w, h, phantom_navy, super_glow, density=0.10, seed=223)

    return png_from_pixels(pixels, w, h)


def build_collapse_point_texture():
    """Collapse point / ground ring: COMPRESSED VOID-PURPLE where void and iron meet.
    Centre #a040e0 emissive, mid #5020a0, outer #080412."""
    w = h = RES
    centre_bright = hex_to_rgba("#d080ff")
    centre        = hex_to_rgba("#a040e0")
    mid           = hex_to_rgba("#5020a0")
    outer         = hex_to_rgba("#080412")
    silver_blue   = hex_to_rgba("#6070a0")
    pixels = base_fill(w, h, outer)

    cx, cy = w // 2, h // 2
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            max_r = w * 0.55
            t = min(1.0, d / max_r)
            if t < 0.10:
                c = color_lerp(centre_bright, centre, t / 0.10)
            elif t < 0.40:
                c = color_lerp(centre, mid, (t - 0.10) / 0.30)
            elif t < 0.85:
                c = color_lerp(mid, outer, (t - 0.40) / 0.45)
            else:
                c = outer
            pixels[y * w + x] = c

    # Spiral collapse pattern
    import random
    rng = random.Random(41)
    for arm in range(6):
        ang_start = (arm / 6.0) * 2 * math.pi
        for r in range(2, int(w * 0.52)):
            spiral_ang = ang_start + (r / 12.0) * 0.6
            x = int(cx + math.cos(spiral_ang) * r)
            y = int(cy + math.sin(spiral_ang) * r)
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                falloff = 1.0 - (r / (w * 0.55))
                pixels[y * w + x] = color_lerp(p, centre_bright, 0.5 * falloff)
                # Halo
                for dy in (-1, 1):
                    yy = y + dy
                    if 0 <= yy < h:
                        p2 = pixels[yy * w + x]
                        pixels[yy * w + x] = color_lerp(p2, centre, 0.3 * falloff)

    # Where void meets iron — silver-blue accent ring
    for ang_step in range(360):
        ang = math.radians(ang_step)
        r = 28
        x = int(cx + math.cos(ang) * r)
        y = int(cy + math.sin(ang) * r)
        if 0 <= x < w and 0 <= y < h:
            p = pixels[y * w + x]
            pixels[y * w + x] = color_lerp(p, silver_blue, 0.55)
            for dr in (-1, 1):
                rx = r + dr
                xx = int(cx + math.cos(ang) * rx)
                yy = int(cy + math.sin(ang) * rx)
                if 0 <= xx < w and 0 <= yy < h:
                    p2 = pixels[yy * w + xx]
                    pixels[yy * w + xx] = color_lerp(p2, silver_blue, 0.3)

    add_noise_overlay(pixels, w, h, outer, centre_bright, density=0.14, seed=127)
    jitter_inplace(pixels, jitter_range=20, seed=153)
    jitter_inplace(pixels, jitter_range=12, seed=199, density=0.5)

    return png_from_pixels(pixels, w, h)


def build():
    b = Builder("phantom_chain_collapse", resolution=(RES, RES), visible_box=(14, 12, 0))
    b.add_texture("phantom_chain", build_phantom_chain_texture())
    b.add_texture("collapse", build_collapse_point_texture())

    phantom_face = lambda: uniform_face(0, 0, RES, RES, tex_index=0)
    coll_face = lambda: uniform_face(0, 0, RES, RES, tex_index=1)

    # Shell specs: (count, radius, link_size)
    shells = [
        ("outer", 18, 8.0, 0.55),
        ("mid",   14, 6.0, 0.45),
        ("inner", 10, 4.0, 0.35),
    ]
    # Final radii (these are the contracted radii after collapse phase)
    # During spawn, shells start at 2x then collapse INWARD to these
    SHELL_START_SCALE = 2.0  # all shells start visually 2x larger via scale

    # ============== 3 SHELL RINGS of chain links ==============
    shell_groups = []  # list of (gu, grp, shell_name, ang, radius, i)
    for shell_name, count, radius, link_size in shells:
        for i in range(count):
            ang = (i / count) * 2 * math.pi
            cx = math.cos(ang) * radius
            cz = math.sin(ang) * radius
            link_yaw = math.degrees(ang) + 90.0

            gu, grp = add_chain_link(
                b, f"{shell_name}_l{i+1}",
                link_radius=link_size, thickness=0.16,
                face_factory=phantom_face, tex_index=0,
            )
            grp["origin"] = [cx, SHELL_Y, cz]
            grp["rotation"] = [0, link_yaw, 0]
            shell_groups.append((gu, grp, shell_name, ang, radius, i, count))

    # ============== PHANTOM CHAIN STRANDS — 8 long thin slabs connecting layers ==============
    strand_groups = []
    for i in range(8):
        ang = (i / 8.0) * 2 * math.pi
        # Strand spans from outer (r=8) to inner (r=4) at this angle
        # Midpoint placement
        mid_r = 6.0
        cx = math.cos(ang) * mid_r
        cz = math.sin(ang) * mid_r
        # Length = 4 (from r=4 to r=8 radially)
        strand_len = 4.0
        cu = b.add_cube(
            f"strand_{i+1}",
            from_=[-strand_len / 2, -0.10, -0.10], to_=[strand_len / 2, 0.10, 0.10],
            faces=phantom_face(),
        )
        # Slab oriented radially (long axis pointing outward)
        gu = make_uuid()
        grp = b.make_group(
            f"phantom_strand_{i+1}", [cx, SHELL_Y, cz], [cu],
            rotation=[0, math.degrees(ang), 0], group_uuid=gu,
        )
        strand_groups.append((gu, grp, ang, i))

    # ============== COLLAPSE POINT — 6 overlapping flat slabs at centre Y=SHELL_Y ==============
    collapse_groups = []
    for i in range(6):
        rot_x = i * 30
        rot_y = i * 60
        rot_z = i * 45
        cu = b.add_cube(
            f"collapse_{i+1}",
            from_=[-1.5, -1.5, -0.1], to_=[1.5, 1.5, 0.1],
            faces=coll_face(),
        )
        gu = make_uuid()
        grp = b.make_group(
            f"collapse_point_{i+1}", [0, SHELL_Y, 0], [cu],
            rotation=[rot_x, rot_y, rot_z], group_uuid=gu,
        )
        collapse_groups.append((gu, grp, i))

    # ============== GROUND COLLAPSE RING — 10 flat slabs at Y=0 ==============
    ground_groups = []
    for i in range(10):
        ang = (i / 10.0) * 2 * math.pi
        cx = math.cos(ang) * 7.0
        cz = math.sin(ang) * 7.0
        cu = b.add_cube(
            f"gcr_{i+1}",
            from_=[-1.7, 0.02, -0.6], to_=[1.7, 0.10, 0.6],
            faces=coll_face(),
        )
        gu = make_uuid()
        grp = b.make_group(
            f"ground_collapse_{i+1}", [cx, 0.05, cz], [cu],
            rotation=[0, math.degrees(ang), 0], group_uuid=gu,
        )
        ground_groups.append((gu, grp, ang))

    # ============== ROOT ==============
    root_children = []
    for (gu, g, *_r) in shell_groups: root_children.append(g)
    for (gu, g, *_r) in strand_groups: root_children.append(g)
    for (gu, g, *_r) in collapse_groups: root_children.append(g)
    for (gu, g, *_r) in ground_groups: root_children.append(g)
    root_uuid = make_uuid()
    b.outliner.append(b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid))

    # ============== SPAWN — 0.9s — collapse INWARD ==============
    spawn = {}

    # Collapse point materialises at 0.0s
    for (gu, grp, i) in collapse_groups:
        kfs = [
            make_keyframe("scale", 0.00, 0, 0, 0),
            make_keyframe("scale", 0.10, 1.4, 1.4, 1.4),
            make_keyframe("scale", 0.25, 1.0, 1.0, 1.0),
            make_keyframe("scale", 0.90, 1.0, 1.0, 1.0),
            make_keyframe("rotation", 0.00, i * 30, i * 60, i * 45),
            make_keyframe("rotation", 0.90, i * 30 + 90, i * 60 + 180, i * 45 + 120),
        ]
        spawn[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Ground ring expands at 0.1s
    for (gu, grp, ang) in ground_groups:
        kfs = [
            make_keyframe("scale", 0.00, 0, 0, 0),
            make_keyframe("scale", 0.10, 0, 0, 0),
            make_keyframe("scale", 0.30, 1.4, 1, 1.4),
            make_keyframe("scale", 0.55, 1.0, 1, 1.0),
            make_keyframe("scale", 0.90, 1.0, 1.0, 1.0),
        ]
        spawn[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Shell rings materialise at scale 2.0 at 0.2s, then COLLAPSE INWARD to final scale 1.0
    # The shells contract from 2.0x out to 1.0x (their actual placement radius)
    # Outer shell contracts MOST (highest start), inner shell contracts LEAST
    shell_collapse_factor = {"outer": 2.2, "mid": 1.8, "inner": 1.5}
    for (gu, grp, shell_name, ang, radius, i, count) in shell_groups:
        # Start position: angular position is fixed, only radius is scaled.
        # To get "shell at radius 2x" we offset the group's position outward.
        start_factor = shell_collapse_factor[shell_name]
        # Position offset = (cos*r*(factor-1), 0, sin*r*(factor-1))
        offs_x = math.cos(ang) * radius * (start_factor - 1)
        offs_z = math.sin(ang) * radius * (start_factor - 1)
        # Slight individual link tumble during collapse
        tumble_x = (i * 47) % 360
        tumble_z = (i * 31) % 360
        kfs = [
            make_keyframe("scale", 0.00, 0, 0, 0),
            make_keyframe("scale", 0.20, 1.0, 1.0, 1.0),  # appears at 0.2s
            make_keyframe("scale", 0.70, 1.0, 1.0, 1.0),
            make_keyframe("scale", 0.90, 1.0, 1.0, 1.0),
            make_keyframe("position", 0.00, offs_x, 0, offs_z),
            make_keyframe("position", 0.20, offs_x, 0, offs_z),  # holds at large radius
            make_keyframe("position", 0.45, offs_x * 0.55, 0, offs_z * 0.55),  # collapsing inward
            make_keyframe("position", 0.70, 0, 0, 0),  # reaches final radius
            make_keyframe("position", 0.90, 0, 0, 0),
            make_keyframe("rotation", 0.00, 0, math.degrees(ang) + 90, 0),
            make_keyframe("rotation", 0.45, tumble_x * 0.6, math.degrees(ang) + 90 + 45, tumble_z * 0.6),
            make_keyframe("rotation", 0.70, tumble_x, math.degrees(ang) + 90 + 90, tumble_z),
            make_keyframe("rotation", 0.90, tumble_x, math.degrees(ang) + 90 + 90, tumble_z),
        ]
        spawn[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Phantom strands connect between shells as they converge — scale up from 0 during collapse
    for (gu, grp, ang, i) in strand_groups:
        kfs = [
            make_keyframe("scale", 0.00, 0, 0, 0),
            make_keyframe("scale", 0.25, 0, 0, 0),
            make_keyframe("scale", 0.45, 1.3, 1.3, 1.3),
            make_keyframe("scale", 0.70, 1.0, 1.0, 1.0),
            make_keyframe("scale", 0.90, 1.0, 1.0, 1.0),
            make_keyframe("rotation", 0.00, 0, math.degrees(ang), 0),
            make_keyframe("rotation", 0.70, 30, math.degrees(ang) + 60, 0),
            make_keyframe("rotation", 0.90, 30, math.degrees(ang) + 90, 0),
        ]
        spawn[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("spawn", length=0.9, animators=spawn, loop="once", override=True)

    # ============== IDLE — 4.0s — shells rotate at different rates and directions ==============
    idle = {}
    shell_rate = {"outer": 0.4, "mid": -0.55, "inner": 0.7}
    for (gu, grp, shell_name, ang, radius, i, count) in shell_groups:
        rate = shell_rate[shell_name]
        kfs = []
        for k in range(9):
            t = (k / 8.0) * 4.0
            phase = (t / 4.0) * 2 * math.pi * rate
            new_ang = ang + phase
            init_cx = math.cos(ang) * radius
            init_cz = math.sin(ang) * radius
            new_cx = math.cos(new_ang) * radius
            new_cz = math.sin(new_ang) * radius
            kfs.append(make_keyframe("position", t, new_cx - init_cx, 0, new_cz - init_cz))
            # Slow link tumble
            kfs.append(make_keyframe("rotation", t,
                                     (t / 4.0) * 180 * (1 if i % 2 == 0 else -1),
                                     math.degrees(new_ang) + 90,
                                     (t / 4.0) * 90 * (1 if i % 3 == 0 else -1)))
        idle[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Strands pulse
    for (gu, grp, ang, i) in strand_groups:
        kfs = []
        for k in range(9):
            t = (k / 8.0) * 4.0
            ph = (t / 4.0 + i * 0.13) * 2 * math.pi * 2
            s = 1.0 + 0.18 * math.sin(ph)
            kfs.append(make_keyframe("scale", t, s, s, s))
        idle[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Collapse point breathes
    for (gu, grp, i) in collapse_groups:
        kfs = []
        for k in range(9):
            t = (k / 8.0) * 4.0
            ph = (t / 4.0 + i * 0.18) * 2 * math.pi
            s = 1.0 + 0.15 * math.sin(ph)
            kfs.append(make_keyframe("scale", t, s, s, s))
            kfs.append(make_keyframe("rotation", t,
                                     i * 30 + (t / 4.0) * 60,
                                     i * 60 + (t / 4.0) * 90,
                                     i * 45 - (t / 4.0) * 45))
        idle[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Ground ring breathes
    for (gu, grp, ang) in ground_groups:
        kfs = []
        for k in range(9):
            t = (k / 8.0) * 4.0
            ph = (t / 4.0) * 2 * math.pi
            s = 1.0 + 0.06 * math.sin(ph)
            kfs.append(make_keyframe("scale", t, s, 1, s))
        idle[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("idle", length=4.0, animators=idle, loop="loop", override=False)

    # ============== DISSIPATE — 0.5s — RAPID inward collapse to centre ==============
    diss = {}
    # All shells rapidly collapse to centre point and scale to 0
    for (gu, grp, shell_name, ang, radius, i, count) in shell_groups:
        init_cx = math.cos(ang) * radius
        init_cz = math.sin(ang) * radius
        kfs = [
            make_keyframe("scale", 0.00, 1, 1, 1),
            make_keyframe("scale", 0.30, 0.7, 0.7, 0.7),
            make_keyframe("scale", 0.50, 0, 0, 0),
            make_keyframe("position", 0.00, 0, 0, 0),
            make_keyframe("position", 0.30, -init_cx * 0.5, 0, -init_cz * 0.5),
            make_keyframe("position", 0.50, -init_cx, 0, -init_cz),
            make_keyframe("rotation", 0.00, 0, math.degrees(ang) + 90, 0),
            make_keyframe("rotation", 0.50, 360 * (1 if i % 2 == 0 else -1),
                          math.degrees(ang) + 90 + 270, 180),
        ]
        diss[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Strands follow — also collapse inward
    for (gu, grp, ang, i) in strand_groups:
        kfs = [
            make_keyframe("scale", 0.00, 1, 1, 1),
            make_keyframe("scale", 0.30, 0.4, 1, 1),
            make_keyframe("scale", 0.50, 0, 0, 0),
        ]
        diss[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Collapse point expands briefly then snaps
    for (gu, grp, i) in collapse_groups:
        kfs = [
            make_keyframe("scale", 0.00, 1, 1, 1),
            make_keyframe("scale", 0.25, 2.5, 2.5, 2.5),
            make_keyframe("scale", 0.50, 0, 0, 0),
            make_keyframe("rotation", 0.00, i * 30, i * 60, i * 45),
            make_keyframe("rotation", 0.50, i * 30 + 360, i * 60 + 720, i * 45 + 540),
        ]
        diss[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    # Ground ring expands and snaps
    for (gu, grp, ang) in ground_groups:
        kfs = [
            make_keyframe("scale", 0.00, 1, 1, 1),
            make_keyframe("scale", 0.30, 1.7, 1, 1.7),
            make_keyframe("scale", 0.50, 0, 0, 0),
        ]
        diss[gu] = {"name": grp["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("dissipate", length=0.5, animators=diss, loop="once", override=True)

    out = b.write(OUTPUT_PATH)
    size = out.stat().st_size
    print(f"WROTE {out} ({size} bytes)")
    return out, size


if __name__ == "__main__":
    build()
