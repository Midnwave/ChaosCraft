"""Texture 1 for devils_halo_array - orbital path rings / central cluster.
Deep purple base with faint silver ring detail at 70% radius. Ghost-faint."""
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

rng = random.Random(1718)

PURPLE  = hx('#200840')
PURPLE2 = hx('#180630')
PURPLE3 = hx('#100420')
SILVER  = hx('#706080')
SILVER2 = hx('#605070')
DARK    = hx('#080208')

# Center of texture
cx, cy = W / 2, H / 2
max_r = math.sqrt(cx * cx + cy * cy)

# Radial falloff with deep purple core, dark outer
for y in range(H):
    for x in range(W):
        dx = x - cx
        dy = y - cy
        d = math.sqrt(dx * dx + dy * dy)
        nr = d / max_r  # 0 at center, ~1 at corners

        if nr < 0.3:
            t = nr / 0.3
            base = lerp(PURPLE, PURPLE2, t)
        elif nr < 0.7:
            t = (nr - 0.3) / 0.4
            base = lerp(PURPLE2, PURPLE3, t)
        else:
            t = (nr - 0.7) / 0.3
            base = lerp(PURPLE3, DARK, t)

        # Slight noise
        gn = rng.randint(-4, 4)
        r = max(0, min(255, base[0] + gn))
        g = max(0, min(255, base[1] + gn))
        b = max(0, min(255, base[2] + gn))
        px[x, y] = (r, g, b, 255)

# Faint silver ring at ~70% radius (ghost-like)
ring_r = 0.45 * W  # actual pixel radius (~70% of half-diag)
ring_thickness = 1.5
for y in range(H):
    for x in range(W):
        dx = x - cx
        dy = y - cy
        d = math.sqrt(dx * dx + dy * dy)
        diff = abs(d - ring_r)
        if diff < ring_thickness:
            t = 1.0 - diff / ring_thickness
            cur = px[x, y]
            ring_col = SILVER if rng.random() < 0.6 else SILVER2
            # Very faint blend
            strength = t * 0.35
            blended = lerp(cur[:3], ring_col, strength)
            px[x, y] = (*blended, 255)

# Some scattered ghost specks
for _ in range(35):
    sx = rng.randint(0, W - 1)
    sy = rng.randint(0, H - 1)
    cur = px[sx, sy]
    blended = lerp(cur[:3], SILVER, 0.25)
    px[sx, sy] = (*blended, 255)

# Inner secondary faint ring at 30% radius
inner_r = 0.20 * W
for y in range(H):
    for x in range(W):
        dx = x - cx
        dy = y - cy
        d = math.sqrt(dx * dx + dy * dy)
        diff = abs(d - inner_r)
        if diff < 1.0:
            cur = px[x, y]
            blended = lerp(cur[:3], SILVER2, 0.20)
            px[x, y] = (*blended, 255)

out = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/devils_halo_array_tex1.png"
img.save(out)
b64 = base64.b64encode(open(out, 'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m17_tex1_b64.txt", "w").write(b64)
import os
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
