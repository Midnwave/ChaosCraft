"""Texture 0 for bone_throne_summon - COMPACTED BONE TECHNIQUE.
Pale bone base with deep shadow cracks, varying brightness zones,
purple-blood emissive in cracks (nightmare marrow energy)."""
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

rng = random.Random(1819)

BONE      = hx('#c8c0b0')
BONE_MID  = hx('#907870')
BONE_PEAK = hx('#e8e0d0')
SPEC      = hx('#f8f0e8')
CRACK     = hx('#282018')
SHADOW    = hx('#1a140c')
PURPLE    = hx('#8020c0')
PURPLE2   = hx('#9028d0')

# Phase 1: Generate Voronoi-like compacted bone zones
# Each zone gets a slightly different brightness (different "bone" age)
num_zones = 18
zone_centers = []
for _ in range(num_zones):
    zx = rng.randint(0, W - 1)
    zy = rng.randint(0, H - 1)
    # Brightness offset for this zone
    offset = rng.uniform(-0.25, 0.30)
    zone_centers.append((zx, zy, offset))

for y in range(H):
    for x in range(W):
        # Find nearest zone
        best = None
        best_d = 1e9
        for zx, zy, off in zone_centers:
            dx = x - zx
            dy = y - zy
            d = dx * dx + dy * dy
            if d < best_d:
                best_d = d
                best = (zx, zy, off)
        zx, zy, off = best

        # Base bone color, modulate brightness by zone
        if off >= 0:
            base = lerp(BONE, BONE_PEAK, off * 2)
        else:
            base = lerp(BONE, BONE_MID, -off * 2)

        # Slight noise
        gn = rng.randint(-6, 6)
        r = max(0, min(255, base[0] + gn))
        g = max(0, min(255, base[1] + gn))
        b = max(0, min(255, base[2] + gn))
        px[x, y] = (r, g, b, 255)

# Phase 2: Draw cracks BETWEEN zones (the lines between compressed bones)
# For each pixel, find 2nd-nearest zone; if 1st & 2nd are nearly equidistant, it's an edge
for y in range(H):
    for x in range(W):
        ds = []
        for zx, zy, off in zone_centers:
            dx = x - zx
            dy = y - zy
            d = math.sqrt(dx * dx + dy * dy)
            ds.append(d)
        ds.sort()
        if ds[1] - ds[0] < 0.7:
            # Edge between two zones — draw crack
            px[x, y] = (*CRACK, 255)
        elif ds[1] - ds[0] < 1.4 and rng.random() < 0.5:
            cur = px[x, y]
            blended = lerp(cur[:3], CRACK, 0.55)
            px[x, y] = (*blended, 255)

# Phase 3: Add irregular dark patches (compressed dark lines)
for _ in range(8):
    sx = rng.randint(2, W - 2)
    sy = rng.randint(2, H - 2)
    sz = rng.randint(2, 5)
    for y in range(max(0, sy - sz), min(H, sy + sz + 1)):
        for x in range(max(0, sx - sz), min(W, sx + sz + 1)):
            dx = x - sx
            dy = y - sy
            d = math.sqrt(dx * dx + dy * dy)
            if d <= sz and rng.random() < 0.4:
                cur = px[x, y]
                blended = lerp(cur[:3], SHADOW, 0.5)
                px[x, y] = (*blended, 255)

# Phase 4: Purple-blood emissive seeping from cracks (nightmare marrow)
# Find dark pixels and bleed purple into neighbors
purple_seeps = []
for y in range(H):
    for x in range(W):
        r, g, b, _ = px[x, y]
        if r < 60 and g < 60 and b < 60:
            for dx in range(-2, 3):
                for dy in range(-2, 3):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < W and 0 <= ny < H and (dx != 0 or dy != 0):
                        d = math.sqrt(dx * dx + dy * dy)
                        if 0 < d <= 2.0:
                            purple_seeps.append((nx, ny, d))

for nx, ny, d in purple_seeps:
    cur = px[nx, ny]
    if cur[0] < 80 and cur[1] < 80 and cur[2] < 80:
        continue
    strength = max(0, 1.0 - d / 2.5) * 0.28
    if rng.random() < 0.55:
        col = PURPLE if rng.random() < 0.7 else PURPLE2
        blended = lerp(cur[:3], col, strength)
        px[nx, ny] = (*blended, 255)

# Phase 5: Specular highlights (bone surface peaks)
for _ in range(18):
    sx = rng.randint(0, W - 1)
    sy = rng.randint(0, H - 1)
    cur = px[sx, sy]
    if cur[0] > 130:
        blended = lerp(cur[:3], SPEC, 0.5)
        px[sx, sy] = (*blended, 255)

# Phase 6: Bone-grain micro-flecks
for _ in range(40):
    sx = rng.randint(0, W - 1)
    sy = rng.randint(0, H - 1)
    cur = px[sx, sy]
    if cur[0] > 100:
        delta = rng.randint(8, 18)
        r = min(255, cur[0] + delta)
        g = min(255, cur[1] + delta)
        b = min(255, cur[2] + delta)
        px[sx, sy] = (r, g, b, 255)

out = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/bone_throne_summon_tex.png"
img.save(out)
b64 = base64.b64encode(open(out, 'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m18_b64.txt", "w").write(b64)
import os
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
