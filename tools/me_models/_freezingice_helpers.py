#!/usr/bin/env python3
"""Shared helpers for FreezingIce ModelEngine attack generators.

Provides texture-building utilities and common geometry patterns used across
all 10 ground/AOE FreezingIce attack models.
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
# COMMON TEXTURE TECHNIQUES (each freezingice attack overrides with unique style)
# ---------------------------------------------------------------------------

def saturate(p, jitter_range=24, seed=303):
    """Per-pixel RGB jitter for filesize bulk + organic grain. Returns new pixel list."""
    rj = random.Random(seed)
    out = []
    for px in p:
        r, g, b, a = px
        jr = rj.randint(-jitter_range, jitter_range)
        jg = rj.randint(-jitter_range, jitter_range)
        jb = rj.randint(-jitter_range, jitter_range)
        out.append((
            max(0, min(255, r + jr)),
            max(0, min(255, g + jg)),
            max(0, min(255, b + jb)),
            a,
        ))
    return out


def jitter_inplace(pixels, jitter_range=24, seed=303, density=1.0):
    """In-place per-pixel jitter."""
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


def diagonal_facet_lines(pixels, w, h, color, count=4, seed=11):
    """Bright diagonal lines for fractured ice cross-sections."""
    rng = random.Random(seed)
    for _ in range(count):
        slope = rng.choice([-1, 1])
        offset = rng.randrange(-w // 2, h + w // 2)
        for x in range(w):
            y = slope * x + offset
            if 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, color, 0.7)
                if 0 <= y + 1 < h:
                    p2 = pixels[(y + 1) * w + x]
                    pixels[(y + 1) * w + x] = color_lerp(p2, color, 0.45)


def vertical_gradient_full(pixels, w, h, colors):
    """colors is list of (stop_pct, rgba). Fills entire texture with a smooth vertical gradient."""
    # Sort stops by pct
    stops = sorted(colors, key=lambda c: c[0])
    for y in range(h):
        t = y / max(1, h - 1)
        # Find bracketing stops
        c_out = stops[-1][1]
        for i in range(len(stops) - 1):
            t0, c0 = stops[i]
            t1, c1 = stops[i + 1]
            if t0 <= t <= t1:
                local = (t - t0) / max(1e-6, t1 - t0)
                c_out = color_lerp(c0, c1, local)
                break
            elif t < stops[0][0]:
                c_out = stops[0][1]
                break
        for x in range(w):
            pixels[y * w + x] = c_out


def horizontal_band_split(pixels, w, h, bands):
    """bands is list of (x_start, x_end, rgba). Fills vertical strips."""
    for (x0, x1, color) in bands:
        for y in range(h):
            for x in range(max(0, x0), min(w, x1)):
                pixels[y * w + x] = color


def specular_cluster(pixels, w, h, cx, cy, radius, color, strength=0.85):
    """Bright soft spot at (cx,cy)."""
    for dy in range(-radius, radius + 1):
        for dx in range(-radius, radius + 1):
            x, y = cx + dx, cy + dy
            if 0 <= x < w and 0 <= y < h:
                d = math.sqrt(dx * dx + dy * dy)
                t = max(0.0, 1.0 - d / float(radius))
                if t > 0:
                    p = pixels[y * w + x]
                    pixels[y * w + x] = color_lerp(p, color, t * strength)


def crack_lines(pixels, w, h, columns, color, seed=21):
    """Draw vertical bright crack lines at given column indices, with jitter."""
    rng = random.Random(seed)
    for col in columns:
        for y in range(h):
            x = col + rng.randint(-1, 1)
            if 0 <= x < w:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, color, 0.78)
                if x + 1 < w:
                    p2 = pixels[y * w + (x + 1)]
                    pixels[y * w + (x + 1)] = color_lerp(p2, color, 0.4)


def frost_web_lines(pixels, w, h, cx, cy, branches, color, seed=44):
    """Branching frost crystal web from a centre point."""
    rng = random.Random(seed)
    for b in range(branches):
        ang = b * (2 * math.pi / branches) + rng.random() * 0.1
        length = rng.randint(20, 32)
        for r in range(length):
            x = int(cx + math.cos(ang) * r)
            y = int(cy + math.sin(ang) * r)
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, color, 0.7)
            # secondary branches
            if r > 5 and r % 6 == 0:
                sub_count = 2
                for sb in range(sub_count):
                    sang = ang + rng.choice([-1, 1]) * (math.pi / 6 + rng.random() * 0.2)
                    sub_len = rng.randint(3, 7)
                    for s in range(sub_len):
                        sx = int(cx + math.cos(ang) * r + math.cos(sang) * s)
                        sy = int(cy + math.sin(ang) * r + math.sin(sang) * s)
                        if 0 <= sx < w and 0 <= sy < h:
                            p = pixels[sy * w + sx]
                            pixels[sy * w + sx] = color_lerp(p, color, 0.55)


def annual_bands(pixels, w, h, period, color, strength=0.35):
    """Horizontal banding lines every N rows for ice strata."""
    for y in range(0, h, period):
        for x in range(w):
            p = pixels[y * w + x]
            pixels[y * w + x] = color_lerp(p, color, strength)
        if y + 1 < h:
            for x in range(w):
                p = pixels[(y + 1) * w + x]
                pixels[(y + 1) * w + x] = color_lerp(p, color, strength * 0.6)


def base_fill(w, h, color):
    return [color] * (w * h)
