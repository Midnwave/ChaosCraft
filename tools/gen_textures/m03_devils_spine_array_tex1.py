"""Texture 1 for devils_spine_array — Intervertebral Disc Glow."""
import base64, math, random
from PIL import Image

random.seed(99)
W = H = 64
img = Image.new("RGBA", (W, H), (0, 0, 0, 255))
px = img.load()

def hx(c):
    c = c.lstrip('#')
    return (int(c[0:2],16), int(c[2:4],16), int(c[4:6],16), 255)

def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(4))

# Colors
deep_void    = hx('#0a0418')
inner_glow   = hx('#6030b0')
bright_ring  = hx('#c060e0')
emissive     = hx('#e080ff')
near_white   = hx('#f0d8ff')

# Fill with deep void base
for y in range(H):
    for x in range(W):
        px[x, y] = deep_void

# Radial from center function
def fill_radial(cx, cy, max_r, zone_x0=0, zone_y0=0):
    for y in range(zone_y0, zone_y0 + 32):
        for x in range(zone_x0, zone_x0 + 32):
            dx = (x - cx)
            dy = (y - cy)
            dist = math.sqrt(dx*dx + dy*dy)
            if dist > max_r:
                px[x, y] = deep_void
                continue
            t = dist / max_r
            # Radial glow: center bright, outer dark
            if t < 0.2:
                c = lerp(near_white, emissive, t / 0.2)
            elif t < 0.5:
                c = lerp(emissive, bright_ring, (t - 0.2) / 0.3)
            elif t < 0.8:
                c = lerp(bright_ring, inner_glow, (t - 0.5) / 0.3)
            else:
                c = lerp(inner_glow, deep_void, (t - 0.8) / 0.2)
            px[x, y] = c

# Zone A (y<32, x<32): disc glow center
fill_radial(16, 16, 14, 0, 0)

# Zone B (y<32, x>=32): offset disc — shows compressed ring emphasis
for y in range(32):
    for x in range(32, 64):
        dx = x - 48
        dy = y - 16
        dist = math.sqrt(dx*dx + dy*dy)
        # Ring pattern: annular glow
        ring_r = 8.0
        ring_width = 3.5
        dist_from_ring = abs(dist - ring_r)
        if dist_from_ring < ring_width:
            t = 1.0 - dist_from_ring / ring_width
            c = lerp(inner_glow, bright_ring, t)
            if t > 0.6:
                c = lerp(bright_ring, emissive, (t - 0.6) / 0.4)
            px[x, y] = c
        elif dist < ring_r - ring_width:
            # Inner core: deep void with faint glow
            t = dist / (ring_r - ring_width)
            px[x, y] = lerp(deep_void, inner_glow, t * 0.3)
        else:
            px[x, y] = deep_void

# Zone C (y>=32, x<32): nerve strand texture — linear streaks
for y in range(32, 64):
    for x in range(32):
        # Diagonal nerve streaks
        lx = x / 31.0
        ly = (y - 32) / 31.0
        streak_v = math.sin((lx * 6 + ly * 2) * math.pi)
        streak_h = math.sin((ly * 8 + lx * 1.5) * math.pi)
        combined = max(streak_v, streak_h)
        if combined > 0.65:
            t = (combined - 0.65) / 0.35
            c = lerp(inner_glow, emissive, t)
            px[x, y] = c
        else:
            t = max(0, combined) / 0.65
            px[x, y] = lerp(deep_void, inner_glow, t * 0.4)

# Zone D (y>=32, x>=32): emissive zone — pulsing bright center
for y in range(32, 64):
    for x in range(32, 64):
        dx = (x - 48)
        dy = (y - 48)
        dist = math.sqrt(dx*dx + dy*dy)
        if dist < 12:
            t = (12 - dist) / 12.0
            if t > 0.7:
                c = lerp(emissive, near_white, (t - 0.7) / 0.3)
            elif t > 0.35:
                c = lerp(bright_ring, emissive, (t - 0.35) / 0.35)
            else:
                c = lerp(inner_glow, bright_ring, t / 0.35)
            px[x, y] = c
        else:
            px[x, y] = deep_void

# Scatter bright emissive sparks
for _ in range(35):
    sx = random.randint(0, 63)
    sy = random.randint(0, 63)
    c = px[sx, sy]
    px[sx, sy] = lerp(c, emissive, 0.7 + random.random() * 0.3)

# Add fine noise grain to prevent banding
for _ in range(150):
    nx = random.randint(0, 63)
    ny = random.randint(0, 63)
    c = px[nx, ny]
    f = 0.8 + random.random() * 0.4
    px[nx, ny] = tuple(min(255, int(c[i] * f)) if i < 3 else c[i] for i in range(4))

out = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/devils_spine_array_tex1.png"
img.save(out)
b64 = base64.b64encode(open(out, 'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m03_tex1_b64.txt", "w").write(b64)
import os
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
