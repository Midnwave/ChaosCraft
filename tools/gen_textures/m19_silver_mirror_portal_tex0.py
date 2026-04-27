"""Texture 0 for silver_mirror_portal — TARNISHED SILVER BAROQUE TECHNIQUE.
Dark silver base, aged silver mid, polished, bright edge, specular,
purple emissive on inner-facing frame edges. Horizontal engraving every 8 rows."""
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

rng = random.Random(1919)

DARK_SILVER  = hx('#1a1c28')
AGED_SILVER  = hx('#484860')
POLISHED     = hx('#909098')
BRIGHT_EDGE  = hx('#c8c8d8')
SPECULAR     = hx('#e8e8f0')
PURPLE_EM    = hx('#9060d0')
PURPLE_DARK  = hx('#603090')

# Base ornate silver fill across whole texture with band detail every 8 rows
for y in range(H):
    for x in range(W):
        nx = x / (W - 1)
        ny = y / (H - 1)
        # diagonal sheen
        sheen = (nx + ny) * 0.5
        if sheen < 0.4:
            base = lerp(POLISHED, BRIGHT_EDGE, sheen / 0.4)
        elif sheen < 0.7:
            base = lerp(BRIGHT_EDGE, AGED_SILVER, (sheen - 0.4) / 0.3)
        else:
            base = lerp(AGED_SILVER, DARK_SILVER, (sheen - 0.7) / 0.3)
        # horizontal engraving band detail every 8 rows
        if y % 8 == 0 or y % 8 == 7:
            base = lerp(base, DARK_SILVER, 0.55)
        # subtle noise grain
        gn = rng.randint(-6, 6)
        r = max(0, min(255, base[0] + gn))
        g = max(0, min(255, base[1] + gn))
        b = max(0, min(255, base[2] + gn))
        px[x, y] = (r, g, b, 255)

# Inner-facing frame edges — left column and a stripe near top-left = purple emissive
# We treat the leftmost 3 cols as the "inner edge" of the frame element
for y in range(H):
    for x in range(3):
        cur = px[x, y]
        em = PURPLE_EM if rng.random() < 0.75 else PURPLE_DARK
        # gradient out from x=0 emissive
        t = 1.0 - x / 3.0
        blended = lerp(cur[:3], em, 0.55 * t + 0.4)
        px[x, y] = (*blended, 255)

# Specular highlights — small bright spots on the polished mid-zone (top-left quadrant)
spec_spots = [(7, 3), (12, 9), (20, 5), (24, 14), (15, 22), (8, 18), (28, 11)]
for sx, sy in spec_spots:
    for dy in range(-1, 2):
        for dx in range(-1, 2):
            xx, yy = sx + dx, sy + dy
            if 0 <= xx < W and 0 <= yy < H:
                cur = px[xx, yy]
                blended = lerp(cur[:3], SPECULAR, 0.6 if (dx == 0 and dy == 0) else 0.3)
                px[xx, yy] = (*blended, 255)

# Add ornate scrollwork-suggesting darker swirls (3-4 short curve segments)
for cx, cy, rad in [(15, 30, 5), (45, 35, 6), (50, 12, 4), (38, 50, 5)]:
    for theta in range(0, 180, 8):
        rd = math.radians(theta)
        for r in range(rad - 1, rad + 1):
            xx = int(cx + r * math.cos(rd))
            yy = int(cy + r * math.sin(rd))
            if 0 <= xx < W and 0 <= yy < H:
                cur = px[xx, yy]
                blended = lerp(cur[:3], DARK_SILVER, 0.5)
                px[xx, yy] = (*blended, 255)

# Aged tarnish patches
for _ in range(8):
    cx = rng.randint(8, W - 8)
    cy = rng.randint(8, H - 8)
    radius = rng.randint(3, 6)
    for y in range(max(0, cy - radius), min(H, cy + radius + 1)):
        for x in range(max(0, cx - radius), min(W, cx + radius + 1)):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            if d <= radius:
                edge = 1.0 - d / radius
                if edge + rng.uniform(-0.2, 0.2) > 0.2:
                    cur = px[x, y]
                    blended = lerp(cur[:3], DARK_SILVER, edge * 0.4)
                    px[x, y] = (*blended, 255)

# A few bright sheen specks scattered
for _ in range(20):
    sx = rng.randint(0, W - 1)
    sy = rng.randint(0, H - 1)
    cur = px[sx, sy]
    if cur[0] > 100:
        blended = lerp(cur[:3], SPECULAR, 0.6)
        px[sx, sy] = (*blended, 255)

out = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/silver_mirror_portal_tex.png"
img.save(out)
b64 = base64.b64encode(open(out, 'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m19_b64.txt", "w").write(b64)
import os
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
