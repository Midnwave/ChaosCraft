import base64, math, os
from PIL import Image

# MODEL 8 — Infernal Crown Burst, texture 1
# Blood-red and purple mixed radial.
# Centre #ff2020 emissive blood-red, mid ring #8020a0 purple, outer #180810 void.

W = H = 64
img = Image.new("RGBA", (W, H), (0x18, 0x08, 0x10, 255))
px = img.load()

C_CENTER = (0xff, 0x20, 0x20)  # blood red
C_MID    = (0x80, 0x20, 0xa0)  # purple
C_OUTER  = (0x18, 0x08, 0x10)  # void
C_HOT    = (0xff, 0x80, 0x60)  # hot transition
C_DEEP   = (0x40, 0x10, 0x40)  # deep purple

cx_, cy_ = W / 2.0, H / 2.0
maxr = math.sqrt(cx_ * cx_ + cy_ * cy_)

import random
random.seed(80901)

def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

for y in range(H):
    for x in range(W):
        dx = x - cx_
        dy = y - cy_
        r = math.sqrt(dx * dx + dy * dy) / maxr  # 0 at center, 1 at corner
        if r < 0.18:
            c = C_CENTER
        elif r < 0.32:
            t = (r - 0.18) / 0.14
            c = lerp(C_CENTER, C_HOT, t)
        elif r < 0.5:
            t = (r - 0.32) / 0.18
            c = lerp(C_HOT, C_MID, t)
        elif r < 0.72:
            t = (r - 0.5) / 0.22
            c = lerp(C_MID, C_DEEP, t)
        else:
            t = min(1.0, (r - 0.72) / 0.28)
            c = lerp(C_DEEP, C_OUTER, t)

        # Ripple modulation
        ang = math.atan2(dy, dx)
        ripple = 0.5 + 0.5 * math.cos(ang * 6 + r * 9)
        # Mix in a stripe of the more saturated color
        if r < 0.6:
            c2 = C_MID if r > 0.3 else C_CENTER
            mix = 0.18 * ripple
            c = tuple(int(c[i] * (1 - mix) + c2[i] * mix) for i in range(3))

        # Per-pixel noise
        nr = random.randint(-8, 8)
        ng = random.randint(-6, 6)
        nb = random.randint(-8, 8)
        px[x, y] = (
            max(0, min(255, c[0] + nr)),
            max(0, min(255, c[1] + ng)),
            max(0, min(255, c[2] + nb)),
            255
        )

# Hot specks scattered through center
random.seed(80902)
for _ in range(22):
    sx = random.randint(W // 2 - 12, W // 2 + 12)
    sy = random.randint(H // 2 - 12, H // 2 + 12)
    if 0 <= sx < W and 0 <= sy < H:
        px[sx, sy] = (0xff, 0x60, 0x40, 255)

# Purple specks in mid ring
random.seed(80903)
for _ in range(30):
    ang = random.uniform(0, 2 * math.pi)
    rr = random.uniform(0.45, 0.7) * maxr
    sx = int(cx_ + rr * math.cos(ang))
    sy = int(cy_ + rr * math.sin(ang))
    if 0 <= sx < W and 0 <= sy < H:
        px[sx, sy] = (0xb0, 0x40, 0xe0, 255)

out = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/infernal_crown_burst_tex1.png"
img.save(out)
b64 = base64.b64encode(open(out, 'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m08v2_tex1_b64.txt", "w").write(b64)
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
