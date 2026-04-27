"""Texture 1 for nightmare_clock — clock hands / pendulum.
Deep obsidian black with bright blood-red edge highlights along long edges,
plus emissive red glow. Hands look like blades more than clock hands."""
import base64, math, random
from PIL import Image
W = H = 64
img = Image.new("RGBA", (W, H), (0, 0, 0, 255))
px = img.load()

def hx(c):
    c = c.lstrip('#')
    return (int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16))

def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

rng = random.Random(2122)

OBSIDIAN = hx('#06040a')   # deep obsidian black
OBS_HI   = hx('#181222')   # slight obsidian highlight
RED_EDGE = hx('#c01010')   # blood-red edge highlight
RED_EM   = hx('#e02020')   # emissive red
RED_HOT  = hx('#ff4030')   # hottest red core

# Fill obsidian base with subtle variation
for y in range(H):
    for x in range(W):
        v = rng.uniform(0, 1)
        if v < 0.08:
            px[x, y] = (*OBS_HI, 255)
        else:
            r = OBSIDIAN[0] + rng.randint(-2, 6)
            g = OBSIDIAN[1] + rng.randint(-2, 6)
            b = OBSIDIAN[2] + rng.randint(-2, 8)
            px[x, y] = (max(0, r), max(0, g), max(0, b), 255)

# Top/bottom edges: bright blood-red highlight (long edges of hands)
for x in range(W):
    # Top edge gradient (4 pixels deep)
    for d in range(4):
        col = lerp(RED_HOT, RED_EDGE, d / 3.0)
        gn = rng.randint(-8, 8)
        r = max(0, min(255, col[0] + gn))
        g = max(0, min(255, col[1] + gn))
        b = max(0, min(255, col[2] + gn))
        px[x, d] = (r, g, b, 255)
    # Bottom edge
    for d in range(4):
        col = lerp(RED_HOT, RED_EDGE, d / 3.0)
        gn = rng.randint(-8, 8)
        r = max(0, min(255, col[0] + gn))
        g = max(0, min(255, col[1] + gn))
        b = max(0, min(255, col[2] + gn))
        px[x, H - 1 - d] = (r, g, b, 255)

# Soft bleed from red edge into the body
for x in range(W):
    for y in range(4, 10):
        cur = px[x, y]
        t = (10 - y) / 6.0 * 0.45
        blended = lerp(cur[:3], RED_EDGE, t)
        if rng.random() < 0.7:
            px[x, y] = (*blended, 255)
    for y in range(H - 10, H - 4):
        cur = px[x, y]
        t = (y - (H - 10)) / 6.0 * 0.45
        blended = lerp(cur[:3], RED_EDGE, t)
        if rng.random() < 0.7:
            px[x, y] = (*blended, 255)

# Central bright streak (emissive blade core)
mid_y = H // 2
for x in range(W):
    for d in range(-2, 3):
        y = mid_y + d
        if 0 <= y < H:
            t = (1.0 - abs(d) / 2.5) * 0.25
            cur = px[x, y]
            blended = lerp(cur[:3], RED_EM, t)
            px[x, y] = (*blended, 255)

# Crackle veins of emissive red across the body
for _ in range(12):
    sx = rng.randint(2, W - 3)
    sy = rng.randint(8, H - 8)
    length = rng.randint(5, 12)
    angle = rng.uniform(0, 2*math.pi)
    cxp, cyp = sx, sy
    for _ in range(length):
        ix = int(cxp); iy = int(cyp)
        if 0 <= ix < W and 0 <= iy < H:
            cur = px[ix, iy]
            blended = lerp(cur[:3], RED_EM, 0.5)
            px[ix, iy] = (*blended, 255)
        angle += rng.uniform(-0.5, 0.5)
        cxp += math.cos(angle); cyp += math.sin(angle)

# Specular highlights near edges (catch the light)
for _ in range(20):
    sx = rng.randint(0, W - 1)
    side = rng.choice([0, H - 1])
    sy = side + rng.randint(-2, 2)
    if 0 <= sy < H:
        cur = px[sx, sy]
        blended = lerp(cur[:3], (255, 240, 200), 0.4)
        px[sx, sy] = (*blended, 255)

out = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_clock_tex1.png"
img.save(out)
b64 = base64.b64encode(open(out, 'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m21_tex1_b64.txt", "w").write(b64)
import os
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
