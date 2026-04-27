"""Texture 0 for fallen_seraph_skeleton — DIVINE BONE TECHNIQUE.
Pure bone-white base, much cleaner than compacted/aged-bone.
Pale grey shadows, deep crevices, pure specular,
and SUBTLE pale purple emissive — divine grief, not corruption."""
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

rng = random.Random(2222)

BONE     = hx('#f0ece0')   # pure bone white base
PALE_GR  = hx('#c0b8a8')   # pale grey shadow
CREVICE  = hx('#484038')   # deep crevice
SPEC     = hx('#ffffff')   # pure specular
DIV_PURP = hx('#c0a0ff')   # pale purple emissive (divine memory)

# Phase 1: Pure clean bone base, very gentle Voronoi for minimal variation
num_zones = 10
zone_centers = []
for _ in range(num_zones):
    zx = rng.randint(0, W - 1)
    zy = rng.randint(0, H - 1)
    offset = rng.uniform(-0.10, 0.10)
    zone_centers.append((zx, zy, offset))

for y in range(H):
    for x in range(W):
        best = None; best_d = 1e9
        for zx, zy, off in zone_centers:
            dx = x - zx; dy = y - zy
            d = dx*dx + dy*dy
            if d < best_d:
                best_d = d; best = (zx, zy, off)
        zx, zy, off = best
        if off >= 0:
            base = lerp(BONE, SPEC, off * 1.8)
        else:
            base = lerp(BONE, PALE_GR, -off * 2.0)
        gn = rng.randint(-3, 3)
        r = max(0, min(255, base[0] + gn))
        g = max(0, min(255, base[1] + gn))
        b = max(0, min(255, base[2] + gn))
        px[x, y] = (r, g, b, 255)

# Phase 2: Long-grain bone striations (vertical-ish striations, like long bone surface)
for _ in range(11):
    cx = rng.randint(2, W - 3)
    direction = rng.choice([-1, 1])
    cur_x = cx
    for y in range(H):
        if 0 <= cur_x < W:
            cur = px[cur_x, y]
            blended = lerp(cur[:3], PALE_GR, 0.30)
            px[cur_x, y] = (*blended, 255)
        if rng.random() < 0.20:
            cur_x += direction
        if rng.random() < 0.05:
            direction *= -1

# Phase 3: Hairline crevices — narrow, rare, deep shadow lines
for _ in range(5):
    sx = rng.randint(2, W - 3)
    sy = rng.randint(2, H - 3)
    length = rng.randint(8, 16)
    angle = rng.uniform(0, 2*math.pi)
    cxp, cyp = sx, sy
    for _ in range(length):
        ix = int(cxp); iy = int(cyp)
        if 0 <= ix < W and 0 <= iy < H:
            cur = px[ix, iy]
            blended = lerp(cur[:3], CREVICE, 0.65)
            px[ix, iy] = (*blended, 255)
        angle += rng.uniform(-0.3, 0.3)
        cxp += math.cos(angle); cyp += math.sin(angle)

# Phase 4: SUBTLE pale purple emissive in deepest crevice pixels (memory of divinity)
for y in range(H):
    for x in range(W):
        r, g, b, _ = px[x, y]
        if r < 90 and g < 90 and b < 90:
            for dx in range(-2, 3):
                for dy in range(-2, 3):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < W and 0 <= ny < H and (dx != 0 or dy != 0):
                        d = math.sqrt(dx*dx + dy*dy)
                        if 0 < d <= 2.0:
                            cur = px[nx, ny]
                            if cur[0] < 90:
                                continue
                            strength = max(0, 1.0 - d / 2.5) * 0.18
                            if rng.random() < 0.40:
                                blended = lerp(cur[:3], DIV_PURP, strength)
                                px[nx, ny] = (*blended, 255)

# Phase 5: Specular highlights — bone catches divine light cleanly
for _ in range(28):
    sx = rng.randint(0, W - 1)
    sy = rng.randint(0, H - 1)
    cur = px[sx, sy]
    if cur[0] > 200:
        blended = lerp(cur[:3], SPEC, 0.55)
        px[sx, sy] = (*blended, 255)

# Phase 6: Soft halo glow patches (rare bright zones — divine residue)
for _ in range(4):
    cx = rng.randint(8, W - 8)
    cy = rng.randint(8, H - 8)
    rd = rng.randint(3, 5)
    for y in range(max(0, cy - rd), min(H, cy + rd + 1)):
        for x in range(max(0, cx - rd), min(W, cx + rd + 1)):
            d = math.sqrt((x - cx)**2 + (y - cy)**2)
            if d <= rd:
                cur = px[x, y]
                t = (1.0 - d / rd) * 0.20
                blended = lerp(cur[:3], DIV_PURP, t)
                px[x, y] = (*blended, 255)

# Phase 7: Bone-grain micro-flecks
for _ in range(35):
    sx = rng.randint(0, W - 1)
    sy = rng.randint(0, H - 1)
    cur = px[sx, sy]
    if cur[0] > 180:
        delta = rng.randint(6, 14)
        r = min(255, cur[0] + delta)
        g = min(255, cur[1] + delta)
        b = min(255, cur[2] + delta)
        px[sx, sy] = (r, g, b, 255)

out = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/fallen_seraph_skeleton_tex.png"
img.save(out)
b64 = base64.b64encode(open(out, 'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m22_b64.txt", "w").write(b64)
import os
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
