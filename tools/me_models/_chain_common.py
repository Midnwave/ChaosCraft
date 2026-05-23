#!/usr/bin/env python3
"""Shared helpers for Chain Mode ModelEngine AOE Burst / Shockwave attacks (6-10).

Provides:
  - oval_cross chain link construction (4 cubes per link forming an oval ring)
  - texture-building utilities (rust passes, void-purple gradients, spectral fade)
  - common animation helpers (link tumble, ring rotation, radial scale burst)

Bone hierarchy / texture palettes are distinct from FreezingIce and FluffyMode so
the AOE burst feel reads as INDUSTRIAL IRON / RATTLING CHAINS rather than ice spikes
or sugar bursts.
"""

import math
import random
import sys

sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    make_uuid,
    uniform_face,
    png_from_pixels,
    hex_to_rgba,
    color_lerp,
    fill_rect,
    gradient_radial,
    add_noise_overlay,
)


# ---------------------------------------------------------------------------
# CHAIN LINK GEOMETRY — the oval cross link used by every Chain attack
# ---------------------------------------------------------------------------
#
# A single chain link = 4 thin cubes forming an oval:
#   - top arc cube (long axis along X, thin Y, thin Z) at +Y
#   - bottom arc cube (mirror) at -Y
#   - left side cube (thin X, long Y, thin Z) at -X
#   - right side cube (mirror) at +X
#
# Each link is its own group ("bone" in ModelEngine terms). The group origin sits
# at the link's center so the link can rotate / scale / translate as one unit.


def add_chain_link(builder, name_prefix, link_radius=0.55, thickness=0.18,
                   face_factory=None, tex_index=0):
    """Add a 4-cube oval chain link as a group. Returns (group_uuid, group_dict).

    The group is NOT added to the outliner — caller chooses where to place it.
    """
    if face_factory is None:
        face_factory = lambda: uniform_face(0, 0, 64, 64, tex_index=tex_index)

    # Top arc
    top_cu = builder.add_cube(
        f"{name_prefix}_top",
        from_=[-link_radius, link_radius - thickness, -thickness],
        to_=[link_radius, link_radius + thickness, thickness],
        faces=face_factory(),
    )
    # Bottom arc
    bot_cu = builder.add_cube(
        f"{name_prefix}_bot",
        from_=[-link_radius, -link_radius - thickness, -thickness],
        to_=[link_radius, -link_radius + thickness, thickness],
        faces=face_factory(),
    )
    # Left side
    left_cu = builder.add_cube(
        f"{name_prefix}_left",
        from_=[-link_radius - thickness, -link_radius + thickness, -thickness],
        to_=[-link_radius + thickness, link_radius - thickness, thickness],
        faces=face_factory(),
    )
    # Right side
    right_cu = builder.add_cube(
        f"{name_prefix}_right",
        from_=[link_radius - thickness, -link_radius + thickness, -thickness],
        to_=[link_radius + thickness, link_radius - thickness, thickness],
        faces=face_factory(),
    )
    gu = make_uuid()
    g = builder.make_group(
        name_prefix, [0, 0, 0], [top_cu, bot_cu, left_cu, right_cu], group_uuid=gu,
    )
    return gu, g


# ---------------------------------------------------------------------------
# TEXTURE BUILDERS — chain palette techniques
# ---------------------------------------------------------------------------


def base_fill(w, h, color):
    return [color] * (w * h)


def jitter_inplace(pixels, jitter_range=18, seed=303, density=1.0):
    rj = random.Random(seed)
    for i in range(len(pixels)):
        if density < 1.0 and rj.random() > density:
            continue
        p = pixels[i]
        jr = rj.randint(-jitter_range, jitter_range)
        jg = rj.randint(-jitter_range, jitter_range)
        jb = rj.randint(-jitter_range, jitter_range)
        pixels[i] = (
            max(0, min(255, p[0] + jr)),
            max(0, min(255, p[1] + jg)),
            max(0, min(255, p[2] + jb)),
            p[3],
        )


def rust_blotch(pixels, w, h, color_dark, color_mid, color_bright, count=18, seed=7):
    """Scatter irregular rust patches across the texture. Used for iron textures."""
    rng = random.Random(seed)
    for _ in range(count):
        cx = rng.randrange(w)
        cy = rng.randrange(h)
        radius = rng.randint(3, 9)
        for dy in range(-radius, radius + 1):
            for dx in range(-radius, radius + 1):
                d = math.sqrt(dx * dx + dy * dy)
                if d > radius:
                    continue
                # Irregular edge — accept based on noise
                accept = rng.random() > (d / radius) * 0.65
                if not accept:
                    continue
                x = (cx + dx) % w
                y = (cy + dy) % h
                t = d / radius
                if t < 0.35:
                    c = color_bright
                elif t < 0.7:
                    c = color_mid
                else:
                    c = color_dark
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, c, 0.72)


def horizontal_stress_lines(pixels, w, h, color, count=14, seed=43):
    """Bright horizontal lines for metal under tension / shear."""
    rng = random.Random(seed)
    for _ in range(count):
        y = rng.randrange(h)
        length = rng.randint(w // 4, w)
        x_start = rng.randrange(0, max(1, w - length // 2))
        for x in range(x_start, min(w, x_start + length)):
            p = pixels[y * w + x]
            pixels[y * w + x] = color_lerp(p, color, rng.uniform(0.5, 0.9))
            if y + 1 < h and rng.random() < 0.4:
                p2 = pixels[(y + 1) * w + x]
                pixels[(y + 1) * w + x] = color_lerp(p2, color, 0.35)


def runic_glyph_ring(pixels, w, h, cx, cy, radius, color, density=0.4, seed=51):
    """Place small bright pixel clusters on a circle — runic sigil dots."""
    rng = random.Random(seed)
    for ang_step in range(48):
        if rng.random() > density:
            continue
        ang = (ang_step / 48.0) * 2 * math.pi
        x = int(cx + math.cos(ang) * radius)
        y = int(cy + math.sin(ang) * radius)
        if not (0 <= x < w and 0 <= y < h):
            continue
        # 2x2 cluster
        for dy in range(-1, 2):
            for dx in range(-1, 2):
                xx, yy = x + dx, y + dy
                if 0 <= xx < w and 0 <= yy < h:
                    if dx * dx + dy * dy <= 2:
                        p = pixels[yy * w + xx]
                        pixels[yy * w + xx] = color_lerp(p, color, 0.78)


def chain_link_silhouette(pixels, w, h, color_link, color_edge, count=4, seed=21):
    """Draw small oval chain link silhouettes on the texture itself."""
    rng = random.Random(seed)
    for _ in range(count):
        cx = rng.randrange(w)
        cy = rng.randrange(h)
        rx = rng.randint(5, 10)
        ry = rng.randint(8, 14)
        thick = 2
        for dy in range(-ry - thick, ry + thick + 1):
            for dx in range(-rx - thick, rx + thick + 1):
                x = (cx + dx) % w
                y = (cy + dy) % h
                d_outer = (dx * dx) / float((rx + thick) ** 2) + (dy * dy) / float((ry + thick) ** 2)
                d_inner = (dx * dx) / float(rx * rx) + (dy * dy) / float(ry * ry)
                if d_outer <= 1.0 and d_inner >= 1.0:
                    p = pixels[y * w + x]
                    pixels[y * w + x] = color_lerp(p, color_link, 0.75)
                    # Edge
                    if abs(d_outer - 1.0) < 0.15:
                        pixels[y * w + x] = color_lerp(pixels[y * w + x], color_edge, 0.4)


def heat_streak(pixels, w, h, color_hot, count=8, seed=33):
    """Bright diagonal heat-shear streaks for stressed metal emissive."""
    rng = random.Random(seed)
    for _ in range(count):
        slope = rng.choice([-1, 1])
        offset = rng.randrange(-w, h + w)
        thick = rng.randint(1, 2)
        for x in range(w):
            y0 = slope * x + offset
            for t in range(thick):
                y = y0 + t
                if 0 <= y < h:
                    p = pixels[y * w + x]
                    pixels[y * w + x] = color_lerp(p, color_hot, 0.55)


def radial_glow(pixels, w, h, cx, cy, max_r, color_centre, color_edge):
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            t = min(1.0, d / max_r)
            c = color_lerp(color_centre, color_edge, t)
            p = pixels[y * w + x]
            pixels[y * w + x] = color_lerp(p, c, 0.85)
