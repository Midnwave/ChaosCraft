#!/usr/bin/env python3
"""Shared helpers for Chain mode ModelEngine attack generators.

Texture & geometry utilities tuned for industrial wrought iron, rust patches,
cursed-purple contamination, and spectral silver-blue ghost variants.
"""

import math
import random

import sys
sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    png_from_pixels,
    hex_to_rgba,
    color_lerp,
    fill_rect,
    gradient_radial,
    gradient_horizontal_band,
    add_noise_overlay,
)


# ---------------------------------------------------------------------------
# IN-PLACE PIXEL UTILITIES
# ---------------------------------------------------------------------------

def base_fill(w, h, color):
    return [color] * (w * h)


def jitter_inplace(pixels, jitter_range=20, seed=303, density=1.0):
    """Per-pixel RGB jitter; affords filesize bulk + organic grain."""
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


def rust_patches(pixels, w, h, base_rust, bright_rust, count=14, seed=7):
    """Organic irregular rust patches (not geometric circles) — embedded into the iron."""
    rng = random.Random(seed)
    for _ in range(count):
        cx = rng.randrange(4, w - 4)
        cy = rng.randrange(4, h - 4)
        max_r = rng.randint(3, 9)
        # irregular blob — sample noise per angle
        spokes = 18
        radii = [max_r * (0.55 + rng.random() * 0.8) for _ in range(spokes)]
        for y in range(max(0, cy - max_r - 2), min(h, cy + max_r + 3)):
            for x in range(max(0, cx - max_r - 2), min(w, cx + max_r + 3)):
                dx = x - cx
                dy = y - cy
                d = math.sqrt(dx * dx + dy * dy)
                ang = math.atan2(dy, dx)
                spoke = int(((ang + math.pi) / (2 * math.pi)) * spokes) % spokes
                r_at = radii[spoke]
                if d <= r_at:
                    t = 1.0 - (d / r_at)
                    edge_mix = bright_rust if t < 0.3 else base_rust
                    p = pixels[y * w + x]
                    pixels[y * w + x] = color_lerp(p, edge_mix, 0.85 if t > 0.55 else 0.5)


def hammer_marks(pixels, w, h, dark, light, count=22, seed=11):
    """Irregular hammer mark dimples scattered across forged iron."""
    rng = random.Random(seed)
    for _ in range(count):
        cx = rng.randrange(2, w - 2)
        cy = rng.randrange(2, h - 2)
        r = rng.randint(1, 3)
        for dy in range(-r, r + 1):
            for dx in range(-r, r + 1):
                x, y = cx + dx, cy + dy
                if 0 <= x < w and 0 <= y < h:
                    d = math.sqrt(dx * dx + dy * dy)
                    if d <= r:
                        t = 1.0 - (d / max(0.5, r))
                        p = pixels[y * w + x]
                        # darker centre, slight light rim
                        if d > r * 0.7:
                            pixels[y * w + x] = color_lerp(p, light, 0.35)
                        else:
                            pixels[y * w + x] = color_lerp(p, dark, 0.55 * t)


def rivet_grid(pixels, w, h, color_bright, color_shadow, step_x=12, step_y=12, radius=2):
    """Bolt-head / rivet pattern at regular grid positions."""
    for cy in range(step_y // 2, h, step_y):
        for cx in range(step_x // 2, w, step_x):
            for dy in range(-radius - 1, radius + 2):
                for dx in range(-radius - 1, radius + 2):
                    x, y = cx + dx, cy + dy
                    if 0 <= x < w and 0 <= y < h:
                        d = math.sqrt(dx * dx + dy * dy)
                        if d <= radius:
                            t = 1.0 - (d / max(0.5, radius))
                            p = pixels[y * w + x]
                            if d < radius * 0.5:
                                pixels[y * w + x] = color_lerp(p, color_bright, 0.85 * t)
                            else:
                                pixels[y * w + x] = color_lerp(p, color_shadow, 0.55)


def horizontal_grain(pixels, w, h, color, density=0.5, seed=33):
    """Fine horizontal grain lines for forged metal direction."""
    rng = random.Random(seed)
    for y in range(h):
        if rng.random() > density:
            continue
        length = rng.randint(8, w)
        x_start = rng.randrange(0, max(1, w - length))
        strength = 0.15 + rng.random() * 0.35
        for x in range(x_start, x_start + length):
            if 0 <= x < w:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, color, strength)


def crack_lines_radial(pixels, w, h, cx, cy, branches, color, seed=88, max_len=None):
    """Branching crack lines radiating from a centre point."""
    if max_len is None:
        max_len = int(min(w, h) * 0.45)
    rng = random.Random(seed)
    for b in range(branches):
        ang = (b / branches) * 2 * math.pi + rng.random() * 0.12
        length = rng.randint(int(max_len * 0.5), max_len)
        last_x, last_y = cx, cy
        for r in range(length):
            ang_drift = ang + math.sin(r * 0.3) * 0.15
            x = int(cx + math.cos(ang_drift) * r)
            y = int(cy + math.sin(ang_drift) * r)
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, color, 0.78)
                if x + 1 < w:
                    p2 = pixels[y * w + (x + 1)]
                    pixels[y * w + (x + 1)] = color_lerp(p2, color, 0.42)
            # secondary forks every ~7 px
            if r > 4 and r % 7 == 0:
                fang = ang + rng.choice([-1, 1]) * (math.pi / 8 + rng.random() * 0.2)
                flen = rng.randint(3, 8)
                for s in range(flen):
                    fx = int(cx + math.cos(ang) * r + math.cos(fang) * s)
                    fy = int(cy + math.sin(ang) * r + math.sin(fang) * s)
                    if 0 <= fx < w and 0 <= fy < h:
                        p = pixels[fy * w + fx]
                        pixels[fy * w + fx] = color_lerp(p, color, 0.55)


def specular_cluster(pixels, w, h, cx, cy, radius, color, strength=0.85):
    """Bright soft spot — single edge highlight."""
    for dy in range(-radius, radius + 1):
        for dx in range(-radius, radius + 1):
            x, y = cx + dx, cy + dy
            if 0 <= x < w and 0 <= y < h:
                d = math.sqrt(dx * dx + dy * dy)
                t = max(0.0, 1.0 - d / float(radius))
                if t > 0:
                    p = pixels[y * w + x]
                    pixels[y * w + x] = color_lerp(p, color, t * strength)


def emissive_seam(pixels, w, h, x_center, color, intensity=0.7, width=2):
    """Vertical emissive seam (curse/spectral glow through metal)."""
    for y in range(h):
        for dx in range(-width, width + 1):
            x = x_center + dx
            if 0 <= x < w:
                t = max(0.0, 1.0 - abs(dx) / float(width + 1))
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, color, intensity * t)


# ---------------------------------------------------------------------------
# GEOMETRY HELPERS — CHAIN LINK OVAL-CROSS CONSTRUCTION
# ---------------------------------------------------------------------------

def chain_link_pair(builder, name_prefix, cx, cy, cz, scale=1.0, face_factory=None):
    """Returns two cube uuids forming an oval link via crossed flat slabs.
    One horizontal flat slab + one vertical flat slab, crossed at 90°.
    """
    if face_factory is None:
        raise ValueError("face_factory required")
    s = scale
    # horizontal slab (flat, oval-ish via narrow Y, wide X+Z)
    h_uuid = builder.add_cube(
        f"{name_prefix}_h",
        from_=[cx - 0.55 * s, cy - 0.12 * s, cz - 0.3 * s],
        to_=  [cx + 0.55 * s, cy + 0.12 * s, cz + 0.3 * s],
        faces=face_factory(),
    )
    # vertical slab (rotated 90° in YZ via geometry — make narrow X, wider in Y dir)
    v_uuid = builder.add_cube(
        f"{name_prefix}_v",
        from_=[cx - 0.3 * s, cy - 0.55 * s, cz - 0.12 * s],
        to_=  [cx + 0.3 * s, cy + 0.55 * s, cz + 0.12 * s],
        faces=face_factory(),
    )
    return [h_uuid, v_uuid]


# ---------------------------------------------------------------------------
# COMMON UV-SHIFTED FACES (lightweight bulk via varied face coords)
# ---------------------------------------------------------------------------

def shifted_face(u_shift, v_shift, sz, tex_index=0, RES=128):
    """Return a face dict where each direction maps to a different sub-rect.
    Adds JSON bulk per cube vs uniform_face."""
    out = {}
    dirs = ["north", "east", "south", "west", "up", "down"]
    for i, d in enumerate(dirs):
        u = (u_shift + i * sz) % RES
        v = (v_shift + (i // 3) * sz) % RES
        u2 = u + sz
        v2 = v + sz
        if u2 > RES:
            u, u2 = 0, sz
        if v2 > RES:
            v, v2 = 0, sz
        out[d] = {"uv": [u, v, u2, v2], "texture": tex_index}
    return out
