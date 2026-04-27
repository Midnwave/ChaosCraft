"""Texture 0 for devils_halo_array - TARNISHED HALO TECHNIQUE.
Bright white-silver base with dirty oxidisation stains, dark cracks,
and faint blood-red emissive seeping from cracks."""
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

rng = random.Random(1717)

SILVER  = hx('#e0dcd0')
SILVER2 = hx('#f0ecdc')
SILVER3 = hx('#c8c4b8')
STAIN   = hx('#707060')
STAIN2  = hx('#605040')
CRACK   = hx('#181816')
SHADOW  = hx('#101010')
BLOOD   = hx('#801818')
BLOOD2  = hx('#a02828')

# Base silver fill with subtle variation
for y in range(H):
    for x in range(W):
        nx = x / (W - 1)
        ny = y / (H - 1)
        # Subtle sheen gradient
        if (nx + ny) < 0.6:
            t = (nx + ny) / 0.6
            base = lerp(SILVER2, SILVER, t)
        else:
            t = ((nx + ny) - 0.6) / 1.4
            base = lerp(SILVER, SILVER3, t)
        # Slight grain
        gn = rng.randint(-5, 5)
        r = max(0, min(255, base[0] + gn))
        g = max(0, min(255, base[1] + gn))
        b = max(0, min(255, base[2] + gn))
        px[x, y] = (r, g, b, 255)

# Add irregular oxidisation stain patches (5-7 patches)
num_stains = 6
for _ in range(num_stains):
    cx = rng.randint(8, W - 8)
    cy = rng.randint(8, H - 8)
    radius = rng.randint(4, 9)
    intensity = rng.uniform(0.5, 0.85)
    for y in range(max(0, cy - radius), min(H, cy + radius + 1)):
        for x in range(max(0, cx - radius), min(W, cx + radius + 1)):
            dx = x - cx
            dy = y - cy
            d = math.sqrt(dx * dx + dy * dy)
            if d <= radius:
                # Irregular shape via noise
                edge = 1.0 - (d / radius)
                edge_jitter = edge + rng.uniform(-0.15, 0.15)
                if edge_jitter > 0:
                    cur = px[x, y]
                    stain_col = STAIN if rng.random() < 0.7 else STAIN2
                    blended = lerp(cur[:3], stain_col, intensity * edge_jitter * 0.6)
                    px[x, y] = (*blended, 255)

# Add dark crack lines (diagonal, 1px)
def draw_crack(x0, y0, x1, y1, col):
    steps = max(abs(x1 - x0), abs(y1 - y0))
    if steps == 0:
        return
    for s in range(steps + 1):
        t = s / steps
        # tiny jitter
        jx = rng.choice([-1, 0, 0, 0, 1])
        jy = rng.choice([-1, 0, 0, 0, 1])
        x = int(x0 + (x1 - x0) * t) + jx
        y = int(y0 + (y1 - y0) * t) + jy
        if 0 <= x < W and 0 <= y < H:
            px[x, y] = (*col, 255)

# 8 cracks running diagonally across
crack_starts = [(0, 6), (0, 22), (0, 38), (0, 50), (10, 0), (28, 0), (44, 0), (56, 0)]
for sx, sy in crack_starts:
    # Mostly diagonal
    ex = sx + rng.randint(20, 50)
    ey = sy + rng.randint(20, 50)
    draw_crack(sx, sy, min(W - 1, ex), min(H - 1, ey), CRACK)

# Add some shorter intersecting cracks
for _ in range(6):
    sx = rng.randint(5, W - 5)
    sy = rng.randint(5, H - 5)
    ex = sx + rng.randint(-15, 15)
    ey = sy + rng.randint(-15, 15)
    draw_crack(sx, sy, max(0, min(W - 1, ex)), max(0, min(H - 1, ey)), CRACK)

# Deep shadow specks at random
for _ in range(20):
    sx = rng.randint(0, W - 1)
    sy = rng.randint(0, H - 1)
    px[sx, sy] = (*SHADOW, 255)

# Emissive blood-red seeping near crack lines: re-scan and add blood pixels next to cracks
# Find dark pixels (cracks) and tint adjacent pixels red
crack_neighbors = []
for y in range(H):
    for x in range(W):
        r, g, b, _ = px[x, y]
        if r < 40 and g < 40 and b < 40:
            # crack pixel — bleed into neighbors
            for dx in range(-2, 3):
                for dy in range(-2, 3):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < W and 0 <= ny < H and (dx != 0 or dy != 0):
                        d = math.sqrt(dx * dx + dy * dy)
                        if d > 0 and d <= 2.0:
                            crack_neighbors.append((nx, ny, d))

# Apply blood seep with falloff
for nx, ny, d in crack_neighbors:
    cur = px[nx, ny]
    if cur[0] < 60 and cur[1] < 60 and cur[2] < 60:
        continue  # skip the cracks themselves
    seep_strength = max(0, 1.0 - d / 2.5) * 0.3
    if rng.random() < 0.6:
        col = BLOOD if rng.random() < 0.7 else BLOOD2
        blended = lerp(cur[:3], col, seep_strength)
        px[nx, ny] = (*blended, 255)

# Small bright highlights (sheen specks)
for _ in range(15):
    sx = rng.randint(0, W - 1)
    sy = rng.randint(0, H - 1)
    cur = px[sx, sy]
    if cur[0] > 100:
        r = min(255, cur[0] + 25)
        g = min(255, cur[1] + 25)
        b = min(255, cur[2] + 25)
        px[sx, sy] = (r, g, b, 255)

out = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/devils_halo_array_tex.png"
img.save(out)
b64 = base64.b64encode(open(out, 'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m17_b64.txt", "w").write(b64)
import os
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
