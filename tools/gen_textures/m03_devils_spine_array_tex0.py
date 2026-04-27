"""Texture 0 for devils_spine_array — Ancient Weathered Bone."""
import base64, math, random
from PIL import Image, ImageDraw

random.seed(42)
W = H = 64
img = Image.new("RGBA", (W, H), (0, 0, 0, 255))
px = img.load()

def hx(c):
    c = c.lstrip('#')
    return (int(c[0:2],16), int(c[2:4],16), int(c[4:6],16), 255)

def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(4))

# Base colors
shadow   = hx('#1a1810')
mid_dark = hx('#4a4438')
mid      = hx('#8a8070')
highlight= hx('#d8d0c0')
spec     = hx('#f8f0e8')
emissive = hx('#b090ff')

# Fill with base — deep bone shadow
for y in range(H):
    for x in range(W):
        # Horizontal sediment-like banding every 12 rows
        band = (y % 12) / 12.0
        # Base gradient: dark bottom-left to lighter top-right
        gx = x / (W-1)
        gy = y / (H-1)
        t_base = (gx * 0.5 + (1.0 - gy) * 0.5)
        if t_base < 0.3:
            c = lerp(shadow, mid_dark, t_base / 0.3)
        elif t_base < 0.7:
            c = lerp(mid_dark, mid, (t_base - 0.3) / 0.4)
        else:
            c = lerp(mid, highlight, (t_base - 0.7) / 0.3)
        # Sediment banding — darken in band troughs
        band_factor = 0.85 + 0.15 * math.sin(band * math.pi * 2)
        c = tuple(int(c[i] * band_factor) if i < 3 else c[i] for i in range(4))
        px[x, y] = c

# Rough dither pattern for weathered mid-tones (Zone A: y<32, x<32)
for y in range(32):
    for x in range(32):
        if random.random() < 0.18:
            c = px[x, y]
            dark_factor = 0.55 + random.random() * 0.3
            px[x, y] = tuple(int(c[i] * dark_factor) if i < 3 else c[i] for i in range(4))
        elif random.random() < 0.08:
            px[x, y] = mid_dark

# Zone B (y<32, x>=32) — slightly brighter, fewer dark patches
for y in range(32):
    for x in range(32, 64):
        if random.random() < 0.1:
            c = px[x, y]
            px[x, y] = tuple(int(c[i] * 0.65) if i < 3 else c[i] for i in range(4))

# Zone C (y>=32, x<32) — up face, brighter bone top
for y in range(32, 64):
    for x in range(32):
        gx = x / 31.0
        gy = (y - 32) / 31.0
        t = (gx + gy) * 0.5
        c = lerp(mid, highlight, t)
        if random.random() < 0.07:
            c = lerp(c, shadow, 0.4)
        px[x, y] = c

# Zone D (y>=32, x>=32) — down face, darkest
for y in range(32, 64):
    for x in range(32, 64):
        gx = (x - 32) / 31.0
        gy = (y - 32) / 31.0
        t = (gx + gy) * 0.5
        c = lerp(shadow, mid_dark, t * 0.6)
        px[x, y] = c

# Add irregular dark patches (ancient damage/pitting)
for _ in range(22):
    cx = random.randint(2, 61)
    cy = random.randint(2, 61)
    r = random.randint(1, 4)
    for dy in range(-r, r+1):
        for dx in range(-r, r+1):
            if dx*dx + dy*dy <= r*r:
                nx, ny = cx+dx, cy+dy
                if 0 <= nx < 64 and 0 <= ny < 64:
                    c = px[nx, ny]
                    f = 0.35 + random.random() * 0.25
                    px[nx, ny] = tuple(int(c[i] * f) if i < 3 else c[i] for i in range(4))

# Purple emissive contamination in bone pores — scattered across full texture
for _ in range(80):
    ex = random.randint(0, 63)
    ey = random.randint(0, 63)
    # blend toward emissive purple
    c = px[ex, ey]
    t = 0.25 + random.random() * 0.45
    ec = lerp(c, emissive, t)
    px[ex, ey] = ec

# Purple emissive cluster in Zone D (bottom-right emissive zone)
for y in range(32, 64):
    for x in range(32, 64):
        fx = (x - 32) / 31.0
        fy = (y - 32) / 31.0
        dist = math.sqrt((fx - 0.5)**2 + (fy - 0.5)**2)
        if dist < 0.45:
            t = (0.45 - dist) / 0.45
            c = px[x, y]
            px[x, y] = lerp(c, emissive, t * 0.7)

# Specular highlight — bright spot in Zone A upper-right area
for y in range(2, 12):
    for x in range(18, 30):
        dist = math.sqrt((x-24)**2 + (y-6)**2)
        if dist < 5:
            t = (5 - dist) / 5.0
            px[x, y] = lerp(px[x, y], spec, t * 0.9)

# Add bone texture: slight concentric oval grooves suggesting cross-section
for y in range(4, 28):
    for x in range(2, 30):
        cx, cy = 16, 16
        dist = math.sqrt((x - cx)**2 + (y - cy)**2)
        ring = dist % 5.0
        if ring < 0.7:
            c = px[x, y]
            px[x, y] = tuple(int(c[i] * 0.78) if i < 3 else c[i] for i in range(4))

# Horizontal sediment lines — stronger every 12 rows
for y in [11, 23, 35, 47, 59]:
    for x in range(W):
        c = px[x, y]
        px[x, y] = tuple(int(c[i] * 0.6) if i < 3 else c[i] for i in range(4))

out = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/devils_spine_array_tex.png"
img.save(out)
b64 = base64.b64encode(open(out, 'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m03_b64.txt", "w").write(b64)
import os
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
