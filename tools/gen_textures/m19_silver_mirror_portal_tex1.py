"""Texture 1 for silver_mirror_portal — THE WRONG PLACE.
Near-black void with pale ghostly arch/pillar shapes and a single
bright purple emissive eye looking back at (32,20)."""
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

rng = random.Random(1920)

VOID         = hx('#04040c')
VOID_DEEP    = hx('#02020a')
GHOST        = hx('#181820')
GHOST_LIGHT  = hx('#22222c')
GHOST_DIM    = hx('#0e0e16')
EYE          = hx('#9060d0')
EYE_BRIGHT   = hx('#c890f0')
EYE_DARK     = hx('#3010a0')

# Fill with void
for y in range(H):
    for x in range(W):
        # subtle radial darkness toward center, lighter near edges
        nx = (x - 32) / 32.0
        ny = (y - 32) / 32.0
        d = math.sqrt(nx * nx + ny * ny)
        if d > 0.7:
            base = lerp(VOID, VOID_DEEP, (d - 0.7) / 0.7)
        else:
            base = VOID
        gn = rng.randint(-2, 2)
        r = max(0, min(255, base[0] + gn))
        g = max(0, min(255, base[1] + gn))
        b = max(0, min(255, base[2] + gn))
        px[x, y] = (r, g, b, 255)

# Ghost pillar shapes (two faint vertical zones suggesting reflected pillars)
# Left pillar — cols 8-16, rows 12-58
for y in range(12, 58):
    for x in range(8, 17):
        # falloff to edges
        dx = abs(x - 12)
        edge_t = 1.0 - dx / 5.0
        if edge_t > 0:
            cur = px[x, y]
            target = GHOST_LIGHT if rng.random() < 0.4 else GHOST
            blended = lerp(cur[:3], target, edge_t * 0.7)
            px[x, y] = (*blended, 255)

# Right pillar — cols 47-55
for y in range(12, 58):
    for x in range(47, 56):
        dx = abs(x - 51)
        edge_t = 1.0 - dx / 5.0
        if edge_t > 0:
            cur = px[x, y]
            target = GHOST_LIGHT if rng.random() < 0.4 else GHOST
            blended = lerp(cur[:3], target, edge_t * 0.7)
            px[x, y] = (*blended, 255)

# Ghost arch shape — semicircle from (12,12) to (51,12) through (32,4)
arch_cx, arch_cy = 31.5, 14
arch_r = 22
for y in range(0, 18):
    for x in range(8, 56):
        dx = x - arch_cx
        dy = y - arch_cy
        d = math.sqrt(dx * dx + dy * dy)
        diff = abs(d - arch_r)
        if diff < 4 and y < 14:
            cur = px[x, y]
            t = 1.0 - diff / 4.0
            target = GHOST_LIGHT if rng.random() < 0.4 else GHOST
            blended = lerp(cur[:3], target, t * 0.6)
            px[x, y] = (*blended, 255)

# Ghost floor suggestion — bottom 3 rows slight gradient
for y in range(58, 64):
    for x in range(W):
        cur = px[x, y]
        t = (y - 58) / 5.0
        blended = lerp(cur[:3], GHOST_DIM, 0.3 + t * 0.3)
        px[x, y] = (*blended, 255)

# THE EYE — single bright purple emissive at (32, 20) — small but unmistakable
eye_x, eye_y = 32, 20
# outer halo
for dy in range(-4, 5):
    for dx in range(-4, 5):
        d = math.sqrt(dx * dx + dy * dy)
        if d <= 4:
            xx, yy = eye_x + dx, eye_y + dy
            if 0 <= xx < W and 0 <= yy < H:
                cur = px[xx, yy]
                t = max(0, 1.0 - d / 4.0)
                blended = lerp(cur[:3], EYE_DARK, t * 0.6)
                px[xx, yy] = (*blended, 255)
# mid glow
for dy in range(-2, 3):
    for dx in range(-2, 3):
        d = math.sqrt(dx * dx + dy * dy)
        if d <= 2:
            xx, yy = eye_x + dx, eye_y + dy
            if 0 <= xx < W and 0 <= yy < H:
                t = max(0, 1.0 - d / 2.0)
                cur = px[xx, yy]
                blended = lerp(cur[:3], EYE, 0.7 + t * 0.3)
                px[xx, yy] = (*blended, 255)
# bright core
for dy in range(-1, 2):
    for dx in range(-1, 2):
        if abs(dx) + abs(dy) <= 1:
            px[eye_x + dx, eye_y + dy] = (*EYE_BRIGHT, 255)
px[eye_x, eye_y] = (255, 240, 255, 255)

# Faint distant ghost specks (stars in the wrong place's sky)
for _ in range(40):
    sx = rng.randint(0, W - 1)
    sy = rng.randint(0, 30)
    cur = px[sx, sy]
    if cur[0] < 30:
        blended = lerp(cur[:3], GHOST_LIGHT, 0.5)
        px[sx, sy] = (*blended, 255)

out = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/silver_mirror_portal_tex1.png"
img.save(out)
b64 = base64.b64encode(open(out, 'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m19_tex1_b64.txt", "w").write(b64)
import os
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
