"""
Texture 0 for nightmare_eye_projectile: SCLERA SURFACE.
Off-white #f0ece0 with horizontal blood vessel lines #d04050.
Grey-blue discolouration #808898 near iris edge.
Bottom redder (blood pooling). Top clean white. Specular at (32,5) pure white.
"""
import base64, math, random
from PIL import Image
W=H=64; img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()

def hx(c):
    c=c.lstrip('#')
    return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))

def lerp(a,b,t):
    return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

rng = random.Random(1313)

SCLERA      = hx('#f0ece0')
SCLERA_TOP  = hx('#fbf9f1')
SCLERA_BOT  = hx('#d8b8b0')   # bottom edge — blood pooling
VESSEL      = hx('#d04050')
VESSEL_FAINT= hx('#a86060')
DISCOLOUR   = hx('#808898')   # grey-blue near iris edge
SPEC_WHITE  = hx('#ffffff')

# Pre-compute vessel "rivers" — random horizontal-ish curves
vessels = []
for _ in range(7):
    y0 = rng.randint(8, 56)
    amp = rng.uniform(1.5, 3.5)
    freq = rng.uniform(0.18, 0.40)
    phase = rng.uniform(0, math.pi*2)
    branch_threshold = rng.uniform(0.30, 0.60)
    vessels.append((y0, amp, freq, phase, branch_threshold))

# Pre-compute thinner vessel branches
branches = []
for _ in range(14):
    y0 = rng.randint(4, 60)
    amp = rng.uniform(0.5, 1.5)
    freq = rng.uniform(0.30, 0.70)
    phase = rng.uniform(0, math.pi*2)
    x_start = rng.randint(2, 40)
    x_end = x_start + rng.randint(8, 20)
    branches.append((y0, amp, freq, phase, x_start, x_end))

for y in range(H):
    for x in range(W):
        nx = x / (W - 1)
        ny = y / (H - 1)

        # vertical gradient: top clean, bottom blood-pooled
        if ny < 0.15:
            base = lerp(SCLERA_TOP, SCLERA, ny/0.15)
        elif ny < 0.75:
            base = SCLERA
        else:
            t = (ny - 0.75) / 0.25
            base = lerp(SCLERA, SCLERA_BOT, t)

        # corners — slight discolouration grey-blue near "iris edge" (assume iris at centre)
        cx, cy = 0.5, 0.5
        dist = math.sqrt((nx-cx)**2 + (ny-cy)**2)
        # iris would be near centre — discolour ring at radius ~0.15-0.25
        if 0.15 < dist < 0.28:
            disc_strength = 1.0 - abs(dist - 0.21) / 0.07
            disc_strength = max(0, disc_strength) * 0.30
            base = lerp(base, DISCOLOUR, disc_strength)

        r, g, b = base

        # subtle warm tint variance
        noise = rng.random() * 6 - 3
        r = max(0, min(255, r + int(noise)))
        g = max(0, min(255, g + int(noise * 0.8)))
        b = max(0, min(255, b + int(noise * 0.6)))

        px[x,y] = (r, g, b, 255)

# overlay vessel curves — 1px red-pink lines
for (y0, amp, freq, phase, branch_threshold) in vessels:
    for x in range(W):
        yv = y0 + amp * math.sin(x * freq + phase)
        yi = int(round(yv))
        if 0 <= yi < H:
            cur = px[x, yi]
            blend = lerp(cur[:3], VESSEL, 0.85)
            px[x, yi] = (blend[0], blend[1], blend[2], 255)
            # tiny anti-alias
            yi2 = yi + 1
            if 0 <= yi2 < H and (yv - yi) > 0.4:
                cur2 = px[x, yi2]
                b2 = lerp(cur2[:3], VESSEL_FAINT, 0.45)
                px[x, yi2] = (b2[0], b2[1], b2[2], 255)

# overlay thin branch lines (shorter)
for (y0, amp, freq, phase, x_start, x_end) in branches:
    for x in range(x_start, min(x_end, W)):
        yv = y0 + amp * math.sin(x * freq + phase)
        yi = int(round(yv))
        if 0 <= yi < H:
            cur = px[x, yi]
            blend = lerp(cur[:3], VESSEL_FAINT, 0.65)
            px[x, yi] = (blend[0], blend[1], blend[2], 255)

# Specular highlight at (32,5) pure white
for dy in range(-1, 2):
    for dx in range(-1, 2):
        xx, yy = 32 + dx, 5 + dy
        if 0 <= xx < W and 0 <= yy < H:
            fall = max(0, 1 - math.sqrt(dx*dx + dy*dy) * 0.5)
            cur = px[xx, yy]
            blend = lerp(cur[:3], SPEC_WHITE, fall)
            px[xx, yy] = (blend[0], blend[1], blend[2], 255)
# bright centre pixel
px[32, 5] = (255, 255, 255, 255)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_eye_projectile_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m13_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
