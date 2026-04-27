"""Texture 0 for nightmare_clock — BONE CLOCK FACE TECHNIQUE.
Pale bone-white base with aged ivory zones, deep shadow, specular highlights,
and purple-blood emissive in deepest shadow zones (corruption seeping through).
Faint circular ring detail at 40% / 80% radius — ghosts of engraved numerals."""
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

rng = random.Random(2121)

BONE      = hx('#e8e4dc')   # pale bone-white base
IVORY     = hx('#c0b8a8')   # aged ivory
SHADOW    = hx('#282018')   # deep shadow
SPEC      = hx('#ffffff')   # specular
PURPLE    = hx('#8040c0')   # purple contamination emissive
PURPLE2   = hx('#a050d8')

# Phase 1: Bone-white base with subtle zone variations
num_zones = 14
zone_centers = []
for _ in range(num_zones):
    zx = rng.randint(0, W - 1)
    zy = rng.randint(0, H - 1)
    offset = rng.uniform(-0.18, 0.20)
    zone_centers.append((zx, zy, offset))

for y in range(H):
    for x in range(W):
        best = None
        best_d = 1e9
        for zx, zy, off in zone_centers:
            dx = x - zx; dy = y - zy
            d = dx*dx + dy*dy
            if d < best_d:
                best_d = d; best = (zx, zy, off)
        zx, zy, off = best
        if off >= 0:
            base = lerp(BONE, SPEC, off * 1.5)
        else:
            base = lerp(BONE, IVORY, -off * 2.0)
        gn = rng.randint(-4, 4)
        r = max(0, min(255, base[0] + gn))
        g = max(0, min(255, base[1] + gn))
        b = max(0, min(255, base[2] + gn))
        px[x, y] = (r, g, b, 255)

# Phase 2: Faint circular ring detail at 40% and 80% radius
cx, cy = W / 2, H / 2
max_r = math.sqrt(cx*cx + cy*cy)
ring1 = max_r * 0.40
ring2 = max_r * 0.80
for y in range(H):
    for x in range(W):
        dx = x - cx; dy = y - cy
        d = math.sqrt(dx*dx + dy*dy)
        if abs(d - ring1) < 0.7 or abs(d - ring2) < 0.7:
            cur = px[x, y]
            blended = lerp(cur[:3], IVORY, 0.45)
            px[x, y] = (*blended, 255)
        elif abs(d - ring1) < 1.4 or abs(d - ring2) < 1.4:
            if rng.random() < 0.4:
                cur = px[x, y]
                blended = lerp(cur[:3], IVORY, 0.25)
                px[x, y] = (*blended, 255)

# Phase 3: Engraved numeral ghosts — subtle hatch marks at the ring
# 12 hour positions, faint dark notches on outer ring
for h in range(12):
    angle = (h / 12.0) * 2.0 * math.pi - math.pi / 2
    rx = cx + math.cos(angle) * ring2
    ry = cy + math.sin(angle) * ring2
    for dy in range(-2, 3):
        for dx in range(-2, 3):
            nx, ny = int(rx + dx), int(ry + dy)
            if 0 <= nx < W and 0 <= ny < H:
                d = math.sqrt(dx*dx + dy*dy)
                if d < 1.7:
                    cur = px[nx, ny]
                    blended = lerp(cur[:3], SHADOW, max(0, 0.35 - d*0.15))
                    px[nx, ny] = (*blended, 255)

# Phase 4: Deep shadow cracks — like fissures in old bone
for _ in range(7):
    sx = rng.randint(2, W - 3)
    sy = rng.randint(2, H - 3)
    length = rng.randint(6, 14)
    angle = rng.uniform(0, 2*math.pi)
    cxp, cyp = sx, sy
    for step in range(length):
        ix = int(cxp); iy = int(cyp)
        if 0 <= ix < W and 0 <= iy < H:
            cur = px[ix, iy]
            blended = lerp(cur[:3], SHADOW, 0.7)
            px[ix, iy] = (*blended, 255)
            # widen
            for dd in [-1, 1]:
                if 0 <= ix+dd < W:
                    cur2 = px[ix+dd, iy]
                    if rng.random() < 0.4:
                        blended2 = lerp(cur2[:3], SHADOW, 0.3)
                        px[ix+dd, iy] = (*blended2, 255)
        angle += rng.uniform(-0.4, 0.4)
        cxp += math.cos(angle); cyp += math.sin(angle)

# Phase 5: Purple contamination emissive seeping in deepest shadow zones
seeps = []
for y in range(H):
    for x in range(W):
        r, g, b, _ = px[x, y]
        if r < 70 and g < 70 and b < 70:
            for dx in range(-2, 3):
                for dy in range(-2, 3):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < W and 0 <= ny < H and (dx != 0 or dy != 0):
                        d = math.sqrt(dx*dx + dy*dy)
                        if 0 < d <= 2.0:
                            seeps.append((nx, ny, d))
for nx, ny, d in seeps:
    cur = px[nx, ny]
    if cur[0] < 80 and cur[1] < 80 and cur[2] < 80:
        continue
    strength = max(0, 1.0 - d / 2.5) * 0.30
    if rng.random() < 0.55:
        col = PURPLE if rng.random() < 0.65 else PURPLE2
        blended = lerp(cur[:3], col, strength)
        px[nx, ny] = (*blended, 255)

# Phase 6: Specular highlights (bright bone peaks)
for _ in range(22):
    sx = rng.randint(0, W - 1)
    sy = rng.randint(0, H - 1)
    cur = px[sx, sy]
    if cur[0] > 180:
        blended = lerp(cur[:3], SPEC, 0.5)
        px[sx, sy] = (*blended, 255)

# Phase 7: Bone-grain micro-flecks
for _ in range(45):
    sx = rng.randint(0, W - 1)
    sy = rng.randint(0, H - 1)
    cur = px[sx, sy]
    if cur[0] > 130:
        delta = rng.randint(8, 16)
        r = min(255, cur[0] + delta)
        g = min(255, cur[1] + delta)
        b = min(255, cur[2] + delta)
        px[sx, sy] = (r, g, b, 255)

out = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_clock_tex.png"
img.save(out)
b64 = base64.b64encode(open(out, 'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m21_b64.txt", "w").write(b64)
import os
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
