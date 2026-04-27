"""Texture 0 for devils_constellation — INFERNAL STAR TECHNIQUE.
Pure void black base with a centered radial: white-gold core ->
blood-red -> deep red -> black. Hard radial bands, not a smooth gradient."""
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

rng = random.Random(2020)

VOID       = hx('#030308')
GOLD       = hx('#ffe040')
GOLD_HOT   = hx('#fff080')
BLOOD_HOT  = hx('#e02020')
BLOOD_DEEP = hx('#601010')
SPEC       = hx('#ffffff')

cx, cy = 31.5, 31.5
maxr = math.sqrt(31.5 * 31.5 + 31.5 * 31.5)

for y in range(H):
    for x in range(W):
        dx = x - cx
        dy = y - cy
        d = math.sqrt(dx * dx + dy * dy)
        nr = d / maxr  # 0 at center, 1 at corners
        # HARD radial bands (compressed zones, not smooth)
        if nr < 0.04:
            # pure spec / center hot
            base = SPEC
        elif nr < 0.10:
            t = (nr - 0.04) / 0.06
            base = lerp(SPEC, GOLD_HOT, t)
        elif nr < 0.18:
            t = (nr - 0.10) / 0.08
            base = lerp(GOLD_HOT, GOLD, t)
        elif nr < 0.32:
            t = (nr - 0.18) / 0.14
            base = lerp(GOLD, BLOOD_HOT, t)
        elif nr < 0.55:
            t = (nr - 0.32) / 0.23
            base = lerp(BLOOD_HOT, BLOOD_DEEP, t)
        elif nr < 0.80:
            t = (nr - 0.55) / 0.25
            base = lerp(BLOOD_DEEP, VOID, t)
        else:
            base = VOID
        # micro noise but only in mid bands so the core stays clean
        if 0.18 < nr < 0.80:
            gn = rng.randint(-4, 4)
            r = max(0, min(255, base[0] + gn))
            g = max(0, min(255, base[1] + gn))
            b = max(0, min(255, base[2] + gn))
            px[x, y] = (r, g, b, 255)
        else:
            px[x, y] = (*base, 255)

# Star-cross spikes — 4 radial bright spikes from center to ~r=20
def spike(angle_deg, length, brightness_col):
    rd = math.radians(angle_deg)
    for r in range(length):
        for off in range(-1, 2):
            xx = int(cx + r * math.cos(rd) + off * math.sin(rd))
            yy = int(cy + r * math.sin(rd) - off * math.cos(rd))
            if 0 <= xx < W and 0 <= yy < H:
                cur = px[xx, yy]
                t = max(0, 1.0 - r / length)
                w = t * (0.8 if off == 0 else 0.4)
                blended = lerp(cur[:3], brightness_col, w * 0.6)
                px[xx, yy] = (*blended, 255)

spike(0, 22, GOLD_HOT)
spike(90, 22, GOLD_HOT)
spike(180, 22, GOLD_HOT)
spike(270, 22, GOLD_HOT)
spike(45, 14, BLOOD_HOT)
spike(135, 14, BLOOD_HOT)
spike(225, 14, BLOOD_HOT)
spike(315, 14, BLOOD_HOT)

# Pure white center pixel
px[31, 31] = (*SPEC, 255)
px[32, 31] = (*SPEC, 255)
px[31, 32] = (*SPEC, 255)
px[32, 32] = (*SPEC, 255)

# Scattered dim red specks in the void (distant stars)
for _ in range(30):
    sx = rng.randint(0, W - 1)
    sy = rng.randint(0, H - 1)
    cur = px[sx, sy]
    if cur[0] < 20 and cur[1] < 20:
        blended = lerp(cur[:3], BLOOD_DEEP, 0.5)
        px[sx, sy] = (*blended, 255)

out = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/devils_constellation_tex.png"
img.save(out)
b64 = base64.b64encode(open(out, 'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m20_b64.txt", "w").write(b64)
import os
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
