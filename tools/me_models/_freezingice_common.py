#!/usr/bin/env python3
"""Shared helpers for FreezingIce ME attack generators (attacks 11-20).

Provides reusable texture builders and keyframe utilities sized so each
output .bbmodel comfortably exceeds 300 KB.
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
    add_noise_overlay,
    make_keyframe,
)


# ---------------------------------------------------------------------------
# TEXTURE PRIMITIVES
# ---------------------------------------------------------------------------

def new_pixels(w, h, base):
    return [base] * (w * h)


def radial_zone(pixels, w, h, cx, cy, inner_color, outer_color, max_r):
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            t = min(1.0, d / max_r)
            pixels[y * w + x] = color_lerp(inner_color, outer_color, t)


def horiz_band_fade(pixels, w, h, y0, y1, top_color, bottom_color):
    if y1 <= y0:
        return
    for y in range(y0, min(h, y1)):
        t = (y - y0) / max(1, y1 - y0 - 1)
        c = color_lerp(top_color, bottom_color, t)
        for x in range(w):
            pixels[y * w + x] = c


def per_pixel_jitter(pixels, w, h, magnitude=20, seed=99, density=1.0):
    rj = random.Random(seed)
    for y in range(h):
        for x in range(w):
            if density < 1.0 and rj.random() > density:
                continue
            p = pixels[y * w + x]
            jr = rj.randint(-magnitude, magnitude)
            jg = rj.randint(-magnitude, magnitude)
            jb = rj.randint(-magnitude, magnitude)
            pixels[y * w + x] = (
                max(0, min(255, p[0] + jr)),
                max(0, min(255, p[1] + jg)),
                max(0, min(255, p[2] + jb)),
                p[3],
            )


def crack_lines(pixels, w, h, count, line_color, seed=33, length_min=8, length_max=22):
    rng = random.Random(seed)
    for _ in range(count):
        x0 = rng.randrange(0, w)
        y0 = rng.randrange(0, h)
        ang = rng.uniform(0, 2 * math.pi)
        length = rng.randrange(length_min, length_max)
        for k in range(length):
            x = int(x0 + math.cos(ang) * k)
            y = int(y0 + math.sin(ang) * k)
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, line_color, 0.85)


def specular_blob(pixels, w, h, cx, cy, color, radius=4, strength=0.85):
    for dy in range(-radius, radius + 1):
        for dx in range(-radius, radius + 1):
            x, y = cx + dx, cy + dy
            if 0 <= x < w and 0 <= y < h:
                d = math.sqrt(dx * dx + dy * dy)
                t = max(0.0, 1.0 - d / float(radius))
                if t > 0:
                    p = pixels[y * w + x]
                    pixels[y * w + x] = color_lerp(p, color, t * strength)


def random_bright_specks(pixels, w, h, count, color, seed=44, size_max=2):
    rng = random.Random(seed)
    for _ in range(count):
        cx = rng.randrange(0, w)
        cy = rng.randrange(0, h)
        r = rng.randrange(1, size_max + 1)
        for dy in range(-r, r + 1):
            for dx in range(-r, r + 1):
                x, y = cx + dx, cy + dy
                if 0 <= x < w and 0 <= y < h and dx * dx + dy * dy <= r * r:
                    p = pixels[y * w + x]
                    pixels[y * w + x] = color_lerp(p, color, 0.85)


# ---------------------------------------------------------------------------
# KEYFRAME DENSITY HELPERS — used to bulk up animation data
# ---------------------------------------------------------------------------

def osc_scale_kfs(length, steps, base=1.0, amp=0.03, period=2.0, phase=0.0):
    out = []
    for k in range(steps + 1):
        t = (k / steps) * length
        ph = ((t + phase) / period) * 2 * math.pi
        s = base + amp * (1.0 - math.cos(ph))
        out.append(make_keyframe("scale", t, x=s, y=s, z=s))
    return out


def osc_rot_kfs(length, steps, amp_x=2.0, amp_y=2.0, amp_z=2.0, period=2.0, phase=0.0):
    out = []
    for k in range(steps + 1):
        t = (k / steps) * length
        ph = ((t + phase) / period) * 2 * math.pi
        out.append(make_keyframe(
            "rotation", t,
            x=amp_x * math.sin(ph),
            y=amp_y * math.cos(ph * 0.9 + 0.3),
            z=amp_z * math.sin(ph * 1.1 + 0.7),
        ))
    return out


def osc_pos_kfs(length, steps, amp_x=0.0, amp_y=0.2, amp_z=0.0, period=2.0, phase=0.0):
    out = []
    for k in range(steps + 1):
        t = (k / steps) * length
        ph = ((t + phase) / period) * 2 * math.pi
        out.append(make_keyframe(
            "position", t,
            x=amp_x * math.sin(ph),
            y=amp_y * math.cos(ph),
            z=amp_z * math.sin(ph * 0.7 + 0.4),
        ))
    return out


def orbit_pos_kfs(length, steps, radius, period, phase=0.0, y_amp=0.0, y_period=None):
    """Circular XZ-plane orbit position keyframes."""
    out = []
    yp = y_period or period
    for k in range(steps + 1):
        t = (k / steps) * length
        ang = ((t / period) + phase) * 2 * math.pi
        x = math.cos(ang) * radius
        z = math.sin(ang) * radius
        y = y_amp * math.sin((t / yp) * 2 * math.pi + phase * math.pi)
        out.append(make_keyframe("position", t, x=x, y=y, z=z))
    return out


def spin_y_kfs(length, steps, total_degrees, phase=0.0):
    out = []
    for k in range(steps + 1):
        t = (k / steps) * length
        out.append(make_keyframe(
            "rotation", t,
            x=0, y=phase + total_degrees * (t / length), z=0,
        ))
    return out


def hex_color(s, a=255):
    return hex_to_rgba(s, a)
