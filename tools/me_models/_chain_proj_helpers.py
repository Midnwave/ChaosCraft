#!/usr/bin/env python3
"""Shared helpers for Chain Mode ModelEngine PROJECTILE / BEAM attacks (11-15).

These are distinct from `_chain_common` (which is tuned for AOE bursts) — the
texture palettes here lean into:
  - battle-iron (used flail, blood stains) — distinct from rattling stress
  - cursed void purple (Binding Ray) — distinct from rust-orange shockwaves
  - spectral silver-blue (13 Links) — distinct from anchor's grounded iron
  - compressed iron with barely-visible chain pattern (Heavy Throw comet)
  - reaching purple-living chain with claw emissive (Reaching tendril)

Also exports keyframe helpers specific to projectile motion:
  - fast_spin_kfs — flail head spinning rapidly mid-flight
  - chain_whip_kfs — phased oscillation down a chain of segments
  - stream_flow_kfs — looping Z drift simulating links flying through a beam
  - link_growth_kfs — sequential materialise/grow used by reaching tendril spawn
  - tumble_kfs — single tumble used by chain comet idle / dissipate
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
    make_keyframe,
)


# ---------------------------------------------------------------------------
# COLOR / PIXEL PRIMITIVES
# ---------------------------------------------------------------------------

def hexc(s, a=255):
    return hex_to_rgba(s, a)


def new_pixels(w, h, base):
    return [base] * (w * h)


def jitter_inplace(pixels, w, h, magnitude=14, seed=99, density=0.85):
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


def random_specks(pixels, w, h, count, color, seed=44, size_max=2, strength=0.85):
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
                    pixels[y * w + x] = color_lerp(p, color, strength)


def blood_stain(pixels, w, h, cx, cy, radius, blood_color, seed=0):
    rng = random.Random(seed)
    for dy in range(-radius - 2, radius + 3):
        for dx in range(-radius - 2, radius + 3):
            x, y = cx + dx, cy + dy
            if 0 <= x < w and 0 <= y < h:
                d = math.sqrt(dx * dx + dy * dy)
                local_r = radius + rng.uniform(-1.2, 1.2)
                if d < local_r:
                    t = 1.0 - d / max(local_r, 1)
                    p = pixels[y * w + x]
                    pixels[y * w + x] = color_lerp(p, blood_color, t * 0.75)


def horizontal_stripes(pixels, w, h, stripe_y_period, stripe_color, strength=0.55, thickness=2):
    """Bright horizontal bands every N rows. Used by cursed chain beam (passing links suggestion)."""
    for y in range(h):
        phase = y % stripe_y_period
        if phase < thickness:
            t = (thickness - phase) / float(thickness)
            for x in range(w):
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, stripe_color, t * strength)


def diagonal_scratches(pixels, w, h, count, color, seed=77, length=18):
    rng = random.Random(seed)
    for _ in range(count):
        x0 = rng.randrange(0, w)
        y0 = rng.randrange(0, h)
        ang_choice = math.pi / 4 if rng.random() < 0.5 else -math.pi / 4
        ang = rng.uniform(-math.pi / 6, math.pi / 6) + ang_choice
        L = rng.randrange(int(length * 0.5), max(2, length))
        for k in range(L):
            x = int(x0 + math.cos(ang) * k)
            y = int(y0 + math.sin(ang) * k)
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, color, 0.55)


def chain_link_oval_imprint(pixels, w, h, cx, cy, ax, ay, ring_color, strength=0.85):
    """Draw a subtle chain-link oval outline into texture. Cosmetic suggestion."""
    for dy in range(-ay - 2, ay + 3):
        for dx in range(-ax - 2, ax + 3):
            x, y = cx + dx, cy + dy
            if 0 <= x < w and 0 <= y < h:
                eq = (dx / max(ax, 1)) ** 2 + (dy / max(ay, 1)) ** 2
                if 0.6 <= eq <= 1.05:
                    p = pixels[y * w + x]
                    pixels[y * w + x] = color_lerp(p, ring_color, strength)


def radial_gradient(pixels, w, h, cx, cy, stops, max_radius=None):
    """stops = [(r_norm, color), ...]"""
    if max_radius is None:
        max_radius = math.sqrt(cx * cx + cy * cy)
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            t = min(1.0, d / max_radius)
            col = stops[0][1]
            for i in range(len(stops) - 1):
                r0, c0 = stops[i]
                r1, c1 = stops[i + 1]
                if r0 <= t <= r1:
                    tt = (t - r0) / max(1e-9, (r1 - r0))
                    col = color_lerp(c0, c1, tt)
                    break
            if t >= stops[-1][0]:
                col = stops[-1][1]
            pixels[y * w + x] = col


# ---------------------------------------------------------------------------
# TEXTURES — projectile/beam palette
# ---------------------------------------------------------------------------

def tex_battle_iron(w=128, h=128):
    """Battle iron: dark used iron, smooth handle zone, blood stains, scratches.

    Used by Flail Projectile (head + spikes + chain links).
    """
    base   = hexc("#0c0a0a")
    used   = hexc("#241818")
    surf   = hexc("#484030")
    glint  = hexc("#807868")
    spec   = hexc("#b0a890")
    blood  = hexc("#5a2818")
    blood_dark = hexc("#321008")
    pixels = new_pixels(w, h, base)
    # rough surface variation
    for y in range(h):
        for x in range(w):
            r = (x * 17 + y * 13) % 31
            if r < 6:
                pixels[y * w + x] = used
            elif r < 12:
                pixels[y * w + x] = surf
            else:
                pixels[y * w + x] = base
    # Smooth iron polished band (right edge — simulates handle / smoothed chain link surface)
    for y in range(h):
        for x in range(int(w * 0.7), w):
            t = (x - w * 0.7) / (w * 0.3)
            p = pixels[y * w + x]
            polished = color_lerp(used, glint, 0.4 + t * 0.3)
            pixels[y * w + x] = color_lerp(p, polished, 0.7)
    # blood patches (4 large + several small)
    for sx, sy, sr, seed in [(int(w*0.30), int(h*0.18), int(w*0.07), 5),
                              (int(w*0.45), int(h*0.55), int(w*0.06), 11),
                              (int(w*0.78), int(h*0.20), int(w*0.05), 7),
                              (int(w*0.18), int(h*0.75), int(w*0.06), 13)]:
        blood_stain(pixels, w, h, sx, sy, sr, blood, seed)
        blood_stain(pixels, w, h, sx + 1, sy + 1, max(2, sr - 2), blood_dark, seed + 1)
    for i in range(12):
        rng = random.Random(100 + i)
        cx = rng.randrange(int(w*0.2), w)
        cy = rng.randrange(0, h)
        blood_stain(pixels, w, h, cx, cy, 3, blood, seed=200 + i)
    # rough scratches
    diagonal_scratches(pixels, w, h, 40, surf, seed=33, length=int(w * 0.18))
    diagonal_scratches(pixels, w, h, 18, glint, seed=44, length=int(w * 0.10))
    # Highlight specs
    random_specks(pixels, w, h, 80, glint, seed=55, size_max=2, strength=0.6)
    random_specks(pixels, w, h, 24, spec, seed=66, size_max=1, strength=0.7)
    jitter_inplace(pixels, w, h, magnitude=12, seed=77, density=0.85)
    return png_from_pixels(pixels, w, h)


def tex_leather_wake(w=128, h=128):
    """Texture 1 for Flail: leather wrap on left, near-black impact wake on right."""
    leather    = hexc("#3a2010")
    leather_dk = hexc("#1c1008")
    leather_hi = hexc("#603820")
    wake       = hexc("#060604")
    wake_var   = hexc("#10100c")
    pixels = new_pixels(w, h, wake)
    # Leather zone — left half
    for y in range(h):
        for x in range(w // 2):
            pixels[y * w + x] = leather
    # Leather grain — vertical stitches
    for x in range(0, w // 2):
        for y in range(h):
            r = (x * 3 + y * 5) % 11
            if r < 3:
                pixels[y * w + x] = leather_dk
            elif (x * 7 + y * 2) % 13 < 2:
                pixels[y * w + x] = leather_hi
    # Wake right half — near-black with slight variation
    for y in range(h):
        for x in range(w // 2, w):
            n = (x * 5 + y * 9) % 17
            if n < 4:
                pixels[y * w + x] = wake_var
    # Cross-band edge separation between leather and wake
    for y in range(h):
        x = w // 2
        if 0 <= x < w:
            pixels[y * w + x] = leather_dk
    jitter_inplace(pixels, w, h, magnitude=8, seed=99, density=0.7)
    return png_from_pixels(pixels, w, h)


def tex_cursed_beam(w=128, h=128):
    """CHAIN BEAM TECHNIQUE — purple-void beam with chain-link suggesting stripes."""
    outer   = hexc("#040210")
    deep    = hexc("#2010a0")
    bright  = hexc("#6030d0")
    core    = hexc("#c060ff")
    centre  = hexc("#d070ff")
    pixels = new_pixels(w, h, outer)
    # Vertical gradient: edges dark -> centre core emissive
    for y in range(h):
        dy = abs(y - h / 2.0)
        t = min(1.0, dy / (h * 0.5))
        if t < 0.08:
            col = centre
        elif t < 0.22:
            col = color_lerp(centre, core, (t - 0.08) / 0.14)
        elif t < 0.5:
            col = color_lerp(core, bright, (t - 0.22) / 0.28)
        elif t < 0.8:
            col = color_lerp(bright, deep, (t - 0.5) / 0.3)
        else:
            col = color_lerp(deep, outer, (t - 0.8) / 0.2)
        for x in range(w):
            pixels[y * w + x] = col
    # Hard centre emissive strip (~3 rows)
    cy = h // 2
    for y in (cy - 1, cy, cy + 1):
        for x in range(w):
            pixels[y * w + x] = centre
    # Horizontal bright bands every 8 rows simulating passing chain links
    horizontal_stripes(pixels, w, h, stripe_y_period=8, stripe_color=core, strength=0.55, thickness=2)
    horizontal_stripes(pixels, w, h, stripe_y_period=16, stripe_color=centre, strength=0.45, thickness=1)
    # Chain-link oval imprints along centre line
    for i in range(12):
        chain_link_oval_imprint(pixels, w, h, 8 + i * 10, h // 2, 4, 3, bright, strength=0.7)
    # Specks (cursed sparkles)
    random_specks(pixels, w, h, 120, core, seed=11, size_max=2, strength=0.7)
    random_specks(pixels, w, h, 40, centre, seed=22, size_max=1, strength=0.85)
    jitter_inplace(pixels, w, h, magnitude=14, seed=33, density=0.85)
    return png_from_pixels(pixels, w, h)


def tex_cursed_emitter(w=128, h=128):
    """Emitter / impact / bind effect: intense purple void radial."""
    centre = hexc("#d060ff")
    bright = hexc("#7030c0")
    dim    = hexc("#2008a0")
    dark   = hexc("#060210")
    pixels = new_pixels(w, h, dark)
    cx, cy = w // 2, h // 2
    radial_gradient(pixels, w, h, cx, cy, [
        (0.0, centre),
        (0.15, centre),
        (0.35, bright),
        (0.65, dim),
        (1.0, dark),
    ], max_radius=w * 0.5)
    # Radiating spike rays
    for ang_deg in range(0, 360, 15):
        ang = math.radians(ang_deg)
        for r in range(2, int(w * 0.42)):
            x = int(cx + math.cos(ang) * r)
            y = int(cy + math.sin(ang) * r)
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, bright, 0.35)
    random_specks(pixels, w, h, 120, centre, seed=88, size_max=1, strength=0.85)
    random_specks(pixels, w, h, 30, bright, seed=98, size_max=2, strength=0.7)
    jitter_inplace(pixels, w, h, magnitude=14, seed=108, density=0.85)
    return png_from_pixels(pixels, w, h)


def tex_spectral_link(w=128, h=128):
    """13 LINKS TECHNIQUE — ghost iron silver-blue."""
    base    = hexc("#060810")
    ghost   = hexc("#141e28")
    surf    = hexc("#283848")
    bright  = hexc("#506070")
    edge    = hexc("#80a0b0")
    emiss   = hexc("#7090b0")
    pixels = new_pixels(w, h, base)
    # Subtle vertical translucency gradient
    for y in range(h):
        t = y / float(h - 1)
        if t < 0.5:
            col = color_lerp(base, ghost, t * 2.0)
        else:
            col = color_lerp(ghost, surf, (t - 0.5) * 2.0)
        for x in range(w):
            pixels[y * w + x] = col
    # Spectral ripples — wave-shaped bright horizontal bands
    for cy_band, seed in [(20, 1), (44, 2), (68, 3), (90, 4), (108, 5)]:
        for y in range(cy_band - 3, cy_band + 4):
            if 0 <= y < h:
                for x in range(w):
                    wav = math.sin((x / w) * math.pi * 4 + cy_band * 0.3)
                    d = abs(y - (cy_band + wav * 2.0))
                    if d < 2.0:
                        p = pixels[y * w + x]
                        pixels[y * w + x] = color_lerp(p, emiss, (2.0 - d) / 2.0 * 0.6)
    # Chain-link oval imprints
    for cx, cy, ax, ay in [
        (24, 30, 6, 4), (76, 42, 7, 5), (44, 80, 6, 4), (96, 96, 6, 4),
        (16, 110, 5, 3), (52, 12, 5, 3),
    ]:
        chain_link_oval_imprint(pixels, w, h, cx, cy, ax, ay, bright, strength=0.85)
    # Edge glow specks (translucency hints)
    random_specks(pixels, w, h, 180, edge, seed=121, size_max=2, strength=0.6)
    random_specks(pixels, w, h, 60, emiss, seed=131, size_max=2, strength=0.65)
    jitter_inplace(pixels, w, h, magnitude=14, seed=141, density=0.85)
    return png_from_pixels(pixels, w, h)


def tex_spectral_glow(w=128, h=128):
    """Pure emissive spectral white-blue lead glow — radial."""
    core = hexc("#c8e8ff")
    mid  = hexc("#5080a0")
    edge = hexc("#101820")
    pixels = new_pixels(w, h, edge)
    cx, cy = w // 2, h // 2
    radial_gradient(pixels, w, h, cx, cy, [
        (0.0, core),
        (0.18, core),
        (0.5, mid),
        (1.0, edge),
    ], max_radius=w * 0.5)
    # subtle ripple rings
    for r in (24, 38, 52):
        for ang_deg in range(0, 360, 5):
            ang = math.radians(ang_deg)
            x = int(cx + math.cos(ang) * r)
            y = int(cy + math.sin(ang) * r)
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, core, 0.35)
    # Cross-shape highlight
    for d in range(-4, 5):
        for r in range(0, int(w * 0.4)):
            x = cx + r
            y = cy + d
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, core, 0.45)
            x = cx - r
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, core, 0.45)
    random_specks(pixels, w, h, 80, core, seed=222, size_max=1, strength=0.85)
    jitter_inplace(pixels, w, h, magnitude=10, seed=232, density=0.85)
    return png_from_pixels(pixels, w, h)


def tex_compressed_core(w=128, h=128):
    """COMPRESSED CHAIN COMET TECHNIQUE — chain pattern barely visible in core."""
    base   = hexc("#0c0a0a")
    dark   = hexc("#201810")
    barely = hexc("#403828")
    edge   = hexc("#706050")
    spec   = hexc("#a09070")
    pixels = new_pixels(w, h, base)
    # Outer brighter zone (right half from col 50% onward — the compressed metal interface)
    boundary_a = int(w * 0.45)
    boundary_b = int(w * 0.78)
    for y in range(h):
        for x in range(w):
            if x >= boundary_b:
                t = (x - boundary_b) / max(1, (w - boundary_b))
                col = color_lerp(dark, edge, t)
            elif x >= boundary_a:
                t = (x - boundary_a) / max(1, (boundary_b - boundary_a))
                col = color_lerp(base, dark, t * 0.6)
            else:
                col = base
            pixels[y * w + x] = col
    # Core zone — barely visible chain ovals at 20-30% brightness (cols 0-boundary_a)
    rng = random.Random(303)
    for _ in range(14):
        cx = rng.randrange(4, boundary_a)
        cy = rng.randrange(4, h - 4)
        ax = rng.randint(4, 7)
        ay = rng.randint(3, 5)
        for dy in range(-ay - 1, ay + 2):
            for dx in range(-ax - 1, ax + 2):
                x, y = cx + dx, cy + dy
                if 0 <= x < w and 0 <= y < h:
                    eq = (dx / max(ax, 1)) ** 2 + (dy / max(ay, 1)) ** 2
                    if 0.55 <= eq <= 1.05:
                        p = pixels[y * w + x]
                        pixels[y * w + x] = color_lerp(p, barely, 0.30)
    # Compression edge highlights at right side
    for y in range(h):
        for x in range(boundary_b, w):
            if (x + y) % 6 == 0:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, spec, 0.5)
    # Pitting on the dense core
    random_specks(pixels, w, h, 60, dark, seed=313, size_max=1, strength=0.7)
    random_specks(pixels, w, h, 36, edge, seed=323, size_max=1, strength=0.6)
    diagonal_scratches(pixels, w, h, 20, edge, seed=333, length=16)
    # Subtle bright spec highlights on right edge
    for i in range(8):
        specular_blob(pixels, w, h, int(w * 0.85), 10 + i * 14, spec, radius=3, strength=0.55)
    jitter_inplace(pixels, w, h, magnitude=10, seed=343, density=0.85)
    return png_from_pixels(pixels, w, h)


def tex_compression_ring(w=128, h=128):
    """Bright iron-silver compression ring on near-black — texture 1 for comet."""
    ring   = hexc("#a08060")
    inner  = hexc("#604838")
    bg     = hexc("#080604")
    spec   = hexc("#d8c0a0")
    pixels = new_pixels(w, h, bg)
    cx, cy = w // 2, h // 2
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            t = min(1.2, d / (w * 0.5))
            if 0.5 <= t <= 0.85:
                if t < 0.62:
                    col = color_lerp(inner, ring, (t - 0.5) / 0.12)
                elif t < 0.72:
                    col = color_lerp(ring, spec, (t - 0.62) / 0.10)
                else:
                    col = color_lerp(spec, bg, (t - 0.72) / 0.13)
            else:
                col = bg
            pixels[y * w + x] = col
    # Top/side highlights
    specular_blob(pixels, w, h, cx, cy - 44, spec, radius=6, strength=0.85)
    specular_blob(pixels, w, h, cx + 38, cy, spec, radius=5, strength=0.7)
    specular_blob(pixels, w, h, cx - 38, cy, spec, radius=5, strength=0.7)
    specular_blob(pixels, w, h, cx, cy + 44, spec, radius=4, strength=0.55)
    # Ring tick marks
    for ang_deg in range(0, 360, 12):
        ang = math.radians(ang_deg)
        for r in (int(w * 0.32), int(w * 0.37)):
            x = int(cx + math.cos(ang) * r)
            y = int(cy + math.sin(ang) * r)
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, spec, 0.65)
    random_specks(pixels, w, h, 40, spec, seed=404, size_max=2, strength=0.65)
    jitter_inplace(pixels, w, h, magnitude=12, seed=414, density=0.8)
    return png_from_pixels(pixels, w, h)


def tex_reaching_chain(w=128, h=128):
    """REACHING TENDRIL TECHNIQUE — chain alive with spectral will."""
    base   = hexc("#080412")
    living = hexc("#1c0840")
    bright = hexc("#5030b0")
    edge   = hexc("#9060e0")
    claw   = hexc("#c080ff")
    emiss  = hexc("#b070ff")
    pixels = new_pixels(w, h, base)
    # Organic non-mechanical gradient (radial from offset centre)
    cx = int(w * 0.40)
    cy = int(h * 0.55)
    radial_gradient(pixels, w, h, cx, cy, [
        (0.0, bright),
        (0.15, bright),
        (0.4, living),
        (0.75, base),
        (1.0, base),
    ], max_radius=w * 0.75)
    # Outer edge bright purple stripes (top + bottom 2-row bands)
    for x in range(w):
        for y_band in (2, h - 4):
            for dy in (-1, 0, 1, 2):
                y = y_band + dy
                if 0 <= y < h:
                    p = pixels[y * w + x]
                    pixels[y * w + x] = color_lerp(p, edge, 0.85)
    # Living-chain link suggestions — irregular ovals
    rng = random.Random(505)
    for _ in range(12):
        lcx = rng.randrange(4, w - 4)
        lcy = rng.randrange(8, h - 8)
        ax = rng.randint(4, 6)
        ay = rng.randint(3, 4)
        chain_link_oval_imprint(pixels, w, h, lcx, lcy, ax, ay, edge, strength=0.85)
    # Claw-tip emissive zone (top-right corner)
    cx2, cy2 = int(w * 0.85), int(h * 0.15)
    for y in range(max(0, cy2 - 28), min(h, cy2 + 28)):
        for x in range(max(0, cx2 - 28), min(w, cx2 + 28)):
            d = math.sqrt((x - cx2) ** 2 + (y - cy2) ** 2)
            if d < 26:
                t = 1.0 - d / 26
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, claw, t * 0.85)
    # Will-veins (curving lines)
    for veinseed in (711, 712, 713, 714):
        rngv = random.Random(veinseed)
        sx = rngv.randrange(w)
        sy = rngv.randrange(h)
        ang = rngv.uniform(0, 2 * math.pi)
        for k in range(60):
            ang += rngv.uniform(-0.18, 0.18)
            sx += math.cos(ang)
            sy += math.sin(ang)
            x = int(sx)
            y = int(sy)
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, edge, 0.55)
    random_specks(pixels, w, h, 160, emiss, seed=515, size_max=2, strength=0.65)
    random_specks(pixels, w, h, 40, claw, seed=525, size_max=2, strength=0.85)
    jitter_inplace(pixels, w, h, magnitude=14, seed=535, density=0.85)
    return png_from_pixels(pixels, w, h)


def tex_reaching_anchor(w=128, h=128):
    """Deep compressed void — origin where chain emerges from spectral plane."""
    centre = hexc("#a050ff")
    bright = hexc("#7030d0")
    mid    = hexc("#3008a0")
    dark   = hexc("#080208")
    pixels = new_pixels(w, h, dark)
    cx, cy = w // 2, h // 2
    radial_gradient(pixels, w, h, cx, cy, [
        (0.0, centre),
        (0.12, centre),
        (0.3, bright),
        (0.6, mid),
        (1.0, dark),
    ], max_radius=w * 0.5)
    # Concentric portal rings
    for r in (16, 26, 38, 50):
        for ang_deg in range(0, 360, 4):
            ang = math.radians(ang_deg)
            x = int(cx + math.cos(ang) * r)
            y = int(cy + math.sin(ang) * r)
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, centre, 0.55)
    # Spiral arms
    for arm in range(5):
        base_ang = arm * (2 * math.pi / 5)
        for r in range(4, int(w * 0.45)):
            ang = base_ang + r * 0.05
            x = int(cx + math.cos(ang) * r)
            y = int(cy + math.sin(ang) * r)
            if 0 <= x < w and 0 <= y < h:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, bright, 0.55)
    random_specks(pixels, w, h, 90, centre, seed=606, size_max=2, strength=0.85)
    random_specks(pixels, w, h, 36, bright, seed=616, size_max=2, strength=0.75)
    jitter_inplace(pixels, w, h, magnitude=14, seed=626, density=0.85)
    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# CHAIN-LINK GEOMETRY (single shared builder kept here for projectile-style links)
# ---------------------------------------------------------------------------

def add_proj_chain_link(builder, name_prefix, link_radius=0.55, thickness=0.18,
                        face_factory=None, tex_index=0):
    """Add a 4-cube oval chain link (top/bottom/left/right) — same shape as
    `_chain_common.add_chain_link` but kept here so projectile generators don't
    couple to the AOE module."""
    if face_factory is None:
        face_factory = lambda: uniform_face(0, 0, 128, 128, tex_index=tex_index)

    top = builder.add_cube(
        f"{name_prefix}_top",
        from_=[-link_radius, link_radius - thickness, -thickness],
        to_=[link_radius, link_radius + thickness, thickness],
        faces=face_factory(),
    )
    bot = builder.add_cube(
        f"{name_prefix}_bot",
        from_=[-link_radius, -link_radius - thickness, -thickness],
        to_=[link_radius, -link_radius + thickness, thickness],
        faces=face_factory(),
    )
    left = builder.add_cube(
        f"{name_prefix}_left",
        from_=[-link_radius - thickness, -link_radius + thickness, -thickness],
        to_=[-link_radius + thickness, link_radius - thickness, thickness],
        faces=face_factory(),
    )
    right = builder.add_cube(
        f"{name_prefix}_right",
        from_=[link_radius - thickness, -link_radius + thickness, -thickness],
        to_=[link_radius + thickness, link_radius - thickness, thickness],
        faces=face_factory(),
    )
    gu = make_uuid()
    g = builder.make_group(
        name_prefix, [0, 0, 0], [top, bot, left, right], group_uuid=gu,
    )
    return gu, g


# ---------------------------------------------------------------------------
# KEYFRAME HELPERS specific to projectile motion
# ---------------------------------------------------------------------------

def fast_spin_kfs(length, steps, rps_x=4.0, rps_y=6.0, rps_z=3.0):
    """Rapid spinning rotation — used for flail head being thrown."""
    out = []
    for k in range(steps + 1):
        t = (k / steps) * length
        out.append(make_keyframe(
            "rotation", t,
            x=rps_x * t * 360,
            y=rps_y * t * 360,
            z=rps_z * t * 360,
        ))
    return out


def osc_scale_kfs(length, steps, base=1.0, amp=0.05, period=2.0, phase=0.0):
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


def chain_whip_kfs(length, steps, amp_x=0.3, amp_y=0.4, amp_z=0.2, period=2.0, phase=0.0):
    """Phased whipping motion — each link in chain trails behind with phase delay."""
    out = []
    for k in range(steps + 1):
        t = (k / steps) * length
        ph = ((t + phase) / period) * 2 * math.pi
        out.append(make_keyframe(
            "position", t,
            x=amp_x * math.sin(ph),
            y=amp_y * math.cos(ph * 0.85),
            z=amp_z * math.sin(ph * 0.7 + 0.4),
        ))
    return out


def stream_flow_kfs(length, steps, z_start, z_end, period, phase=0.0):
    """Looping Z drift — chain link stream flying through cursed beam."""
    out = []
    for k in range(steps + 1):
        t = (k / steps) * length
        cycle_t = ((t / period) + phase) % 1.0
        z = z_start + (z_end - z_start) * cycle_t
        out.append(make_keyframe("position", t, x=0, y=0, z=z))
    return out


def tumble_kfs(length, steps, deg_x_total=240, deg_y_total=540, deg_z_total=180, phase=0.0):
    out = []
    for k in range(steps + 1):
        t = (k / steps) * length
        f = t / length
        out.append(make_keyframe(
            "rotation", t,
            x=phase + deg_x_total * f,
            y=phase * 1.7 + deg_y_total * f,
            z=phase * 0.7 + deg_z_total * f,
        ))
    return out


def link_growth_kfs(length, t_start, t_grow_dur):
    """Sequential materialise: 0 until t_start, ramp 0->1 over t_grow_dur, hold."""
    safe_start = max(0.0, t_start - 0.001)
    return [
        make_keyframe("scale", 0.0, x=0, y=0, z=0),
        make_keyframe("scale", safe_start, x=0, y=0, z=0),
        make_keyframe("scale", t_start, x=0.05, y=0.05, z=0.05),
        make_keyframe("scale", t_start + t_grow_dur * 0.5, x=1.15, y=1.15, z=1.15),
        make_keyframe("scale", t_start + t_grow_dur, x=1.0, y=1.0, z=1.0),
        make_keyframe("scale", length, x=1.0, y=1.0, z=1.0),
    ]
