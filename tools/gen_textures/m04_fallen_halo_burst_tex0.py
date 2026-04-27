"""Texture 0 for fallen_halo_burst — Sacred Bone-White Halo."""
import base64, math, random
from PIL import Image

random.seed(77)
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
near_white  = hx('#f0ece0')
warm_grey   = hx('#b8b0a0')
deep_shadow = hx('#404038')
spec_white  = hx('#ffffff')
emissive    = hx('#fffff0')

# Fill base — near-white across all zones
for y in range(H):
    for x in range(W):
        # Very subtle gradient — nearly uniform white
        gx = x / (W-1)
        gy = y / (H-1)
        # Central brightness with subtle edge falloff
        dx = abs(gx - 0.5) * 2.0  # 0 at center, 1 at edge
        dy = abs(gy - 0.5) * 2.0
        dist = math.sqrt(dx*dx + dy*dy) / math.sqrt(2)
        t = dist * 0.15
        c = lerp(near_white, warm_grey, t)
        px[x, y] = c

# Zone A (y<32, x<32): north/south face — faint concentric ring detail
for y in range(32):
    for x in range(32):
        cx_a, cy_a = 16, 16
        dist = math.sqrt((x - cx_a)**2 + (y - cy_a)**2)
        # Concentric ring markers at 30% and 70% of ~14 radius
        ring_30 = 14 * 0.3  # ~4.2
        ring_70 = 14 * 0.7  # ~9.8
        c = px[x, y]
        for ring_r in [ring_30, ring_70]:
            diff = abs(dist - ring_r)
            if diff < 0.8:
                t = (0.8 - diff) / 0.8
                # Subtle engraved geometry lines — slightly darker
                c = lerp(c, warm_grey, t * 0.35)
        px[x, y] = c

# Zone B (y<32, x>=32): east/west face — similar subtle rings
for y in range(32):
    for x in range(32, 64):
        cx_b, cy_b = 48, 16
        dist = math.sqrt((x - cx_b)**2 + (y - cy_b)**2)
        ring_30 = 14 * 0.3
        ring_70 = 14 * 0.7
        c = px[x, y]
        for ring_r in [ring_30, ring_70]:
            diff = abs(dist - ring_r)
            if diff < 0.8:
                t = (0.8 - diff) / 0.8
                c = lerp(c, warm_grey, t * 0.35)
        px[x, y] = c

# Bottom row of Zone A/B: 1px dark shadow edge
for x in range(W):
    if 30 <= x < 32 or 30 <= x <= 31:
        pass
    px[x, 31] = lerp(px[x,31], deep_shadow, 0.6)

# Also bottom row overall (y=63) for down face
for x in range(W):
    px[x, 63] = lerp(px[x,63], deep_shadow, 0.7)

# Specular highlight: pure white 5px at (10,4) area — Zone A
for y in range(1, 9):
    for x in range(6, 16):
        dist = math.sqrt((x - 10)**2 + (y - 4)**2)
        if dist < 5:
            t = (5 - dist) / 5.0
            px[x, y] = lerp(px[x,y], spec_white, t * 0.95)

# Emissive: 5px near-pure-white in Zone D (down face, bottom right)
for y in range(32, 64):
    for x in range(32, 64):
        cx_d, cy_d = 48, 48
        dist = math.sqrt((x - cx_d)**2 + (y - cy_d)**2)
        if dist < 11:
            t = (11 - dist) / 11.0
            px[x, y] = lerp(px[x,y], emissive, t * 0.85)

# Zone C (up face): very bright — the luminous top of the halo
for y in range(32, 64):
    for x in range(32):
        # Nearly pure white, slight warm tint at edges
        gx = x / 31.0
        gy = (y - 32) / 31.0
        dist_c = math.sqrt((gx - 0.5)**2 + (gy - 0.5)**2) * 1.4
        c = lerp(emissive, near_white, min(1.0, dist_c * 0.5))
        px[x, y] = c

# Very subtle random grain to add life (the texture should look almost featureless)
for _ in range(200):
    nx = random.randint(0, 63)
    ny = random.randint(0, 63)
    c = px[nx, ny]
    f = 0.93 + random.random() * 0.1
    px[nx, ny] = tuple(min(255, int(c[i] * f)) if i < 3 else c[i] for i in range(4))

# Fine sacred geometry: very faint cross-hatch suggestion
for y in range(2, 30):
    for x in range(2, 30):
        if (x + y) % 8 == 0:
            c = px[x, y]
            px[x, y] = lerp(c, warm_grey, 0.12)

out = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/fallen_halo_burst_tex.png"
img.save(out)
b64 = base64.b64encode(open(out, 'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m04_b64.txt", "w").write(b64)
import os
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
