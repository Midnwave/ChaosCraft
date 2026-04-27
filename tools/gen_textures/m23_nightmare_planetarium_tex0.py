"""
Texture 0 for nightmare_planetarium: WRONG PLANET TECHNIQUE.
4 zones top-to-bottom, each 16 rows.
- Rows 0-16: blood-red planet surface (blood)
- Rows 16-32: bone-white planet surface (bone)
- Rows 32-48: sickly yellow planet surface (toxic)
- Rows 48-64: pure void purple planet surface (compressed purple)
"""
import base64, math, random
from PIL import Image
W=H=64; img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()

def hx(c):
    c=c.lstrip('#')
    return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))

def lerp(a,b,t):
    return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

rng = random.Random(2323)

# ─── Blood-red zone (rows 0-16) ─────────────────────────────────
BLOOD_BASE  = hx('#7c0a18')
BLOOD_DARK  = hx('#400408')
BLOOD_BRIGHT= hx('#c01828')
BLOOD_VEIN  = hx('#ff3040')

# ─── Bone-white zone (rows 16-32) ───────────────────────────────
BONE_BASE   = hx('#e0d4b8')
BONE_DARK   = hx('#88806c')
BONE_BRIGHT = hx('#fff8e0')
BONE_CRACK  = hx('#403828')

# ─── Sickly yellow zone (rows 32-48) ────────────────────────────
TOX_BASE    = hx('#a8a020')
TOX_DARK    = hx('#504810')
TOX_BRIGHT  = hx('#dcd048')
TOX_GLOW    = hx('#f8f880')

# ─── Void purple zone (rows 48-64) ──────────────────────────────
VOID_BASE   = hx('#2a0844')
VOID_DARK   = hx('#0c0218')
VOID_BRIGHT = hx('#5028a0')
VOID_STAR   = hx('#a880ff')

# Pre-generate continent/crater seeds for each zone
blood_continents = [(rng.randint(0,W), rng.randint(0,15), rng.randint(3,7)) for _ in range(8)]
bone_craters     = [(rng.randint(0,W), rng.randint(16,31), rng.randint(2,5)) for _ in range(12)]
tox_pools        = [(rng.randint(0,W), rng.randint(32,47), rng.randint(2,6)) for _ in range(7)]
void_stars       = [(rng.randint(0,W-1), rng.randint(48,63)) for _ in range(20)]

# Vein lines for blood zone
blood_veins = []
for _ in range(5):
    y0 = rng.randint(2, 14)
    amp = rng.uniform(0.8, 2.0)
    freq = rng.uniform(0.25, 0.55)
    phase = rng.uniform(0, math.pi*2)
    blood_veins.append((y0, amp, freq, phase))

# Crack lines for bone zone
bone_cracks = []
for _ in range(6):
    y0 = rng.randint(18, 30)
    amp = rng.uniform(0.5, 1.5)
    freq = rng.uniform(0.30, 0.60)
    phase = rng.uniform(0, math.pi*2)
    x_start = rng.randint(0, 30)
    x_end = x_start + rng.randint(10, 30)
    bone_cracks.append((y0, amp, freq, phase, x_start, x_end))

for y in range(H):
    for x in range(W):
        nx = x / (W - 1)

        # ── Zone 1: Blood (rows 0-16) ───────────────────────────
        if y < 16:
            ny = y / 15.0
            # base gradient (darker top, brighter middle)
            if ny < 0.5:
                base = lerp(BLOOD_DARK, BLOOD_BASE, ny/0.5)
            else:
                base = lerp(BLOOD_BASE, BLOOD_BRIGHT, (ny-0.5)/0.5*0.6)
            # continent blobs
            for (cx, cy, cr) in blood_continents:
                d = math.sqrt((x-cx)**2 + (y-cy)**2)
                if d < cr:
                    t = 1 - d/cr
                    base = lerp(base, BLOOD_BRIGHT, t*0.55)
            # noise
            n = rng.random()*30 - 15
            r,g,b = base
            r=max(0,min(255,r+int(n)))
            g=max(0,min(255,g+int(n*0.4)))
            b=max(0,min(255,b+int(n*0.4)))
            px[x,y] = (r,g,b,255)

        # ── Zone 2: Bone (rows 16-32) ───────────────────────────
        elif y < 32:
            ny = (y-16) / 15.0
            if ny < 0.5:
                base = lerp(BONE_DARK, BONE_BASE, 0.5 + ny)
            else:
                base = lerp(BONE_BASE, BONE_BRIGHT, (ny-0.5)*0.7)
            # craters: dark rings
            for (cx, cy, cr) in bone_craters:
                d = math.sqrt((x-cx)**2 + (y-cy)**2)
                if cr*0.5 < d < cr:
                    base = lerp(base, BONE_DARK, 0.65)
                elif d <= cr*0.5:
                    base = lerp(base, BONE_BRIGHT, 0.40)
            n = rng.random()*16 - 8
            r,g,b = base
            r=max(0,min(255,r+int(n)))
            g=max(0,min(255,g+int(n)))
            b=max(0,min(255,b+int(n*0.7)))
            px[x,y] = (r,g,b,255)

        # ── Zone 3: Toxic Yellow (rows 32-48) ───────────────────
        elif y < 48:
            ny = (y-32) / 15.0
            base = lerp(TOX_DARK, TOX_BASE, ny if ny < 0.7 else 0.7)
            if ny > 0.7:
                base = lerp(TOX_BASE, TOX_BRIGHT, (ny-0.7)/0.3)
            for (cx, cy, cr) in tox_pools:
                d = math.sqrt((x-cx)**2 + (y-cy)**2)
                if d < cr:
                    t = 1 - d/cr
                    base = lerp(base, TOX_GLOW, t*0.65)
            n = rng.random()*22 - 11
            r,g,b = base
            r=max(0,min(255,r+int(n)))
            g=max(0,min(255,g+int(n)))
            b=max(0,min(255,b+int(n*0.3)))
            px[x,y] = (r,g,b,255)

        # ── Zone 4: Void Purple (rows 48-64) ────────────────────
        else:
            ny = (y-48) / 15.0
            # darker overall — void
            t = 0.3 + ny*0.6
            base = lerp(VOID_DARK, VOID_BASE, t)
            # subtle bright spots already baked from VOID_BRIGHT below
            n = rng.random()*14 - 7
            r,g,b = base
            r=max(0,min(255,r+int(n*0.6)))
            g=max(0,min(255,g+int(n*0.3)))
            b=max(0,min(255,b+int(n)))
            px[x,y] = (r,g,b,255)

# ── Overlay blood veins (zone 1) ────────────────────────────────
for (y0, amp, freq, phase) in blood_veins:
    for x in range(W):
        yv = y0 + amp * math.sin(x * freq + phase)
        yi = int(round(yv))
        if 0 <= yi < 16:
            cur = px[x, yi]
            blend = lerp(cur[:3], BLOOD_VEIN, 0.75)
            px[x, yi] = (blend[0], blend[1], blend[2], 255)

# ── Overlay bone cracks (zone 2) ────────────────────────────────
for (y0, amp, freq, phase, x_start, x_end) in bone_cracks:
    for x in range(x_start, min(x_end, W)):
        yv = y0 + amp * math.sin(x * freq + phase)
        yi = int(round(yv))
        if 16 <= yi < 32:
            cur = px[x, yi]
            blend = lerp(cur[:3], BONE_CRACK, 0.80)
            px[x, yi] = (blend[0], blend[1], blend[2], 255)

# ── Overlay void stars (zone 4) ─────────────────────────────────
for (sx, sy) in void_stars:
    if 0 <= sx < W and 48 <= sy < 64:
        px[sx, sy] = (VOID_STAR[0], VOID_STAR[1], VOID_STAR[2], 255)
        # halo
        for dx in [-1, 1]:
            xx = sx + dx
            if 0 <= xx < W:
                cur = px[xx, sy]
                blend = lerp(cur[:3], VOID_STAR, 0.40)
                px[xx, sy] = (blend[0], blend[1], blend[2], 255)
        for dy in [-1, 1]:
            yy = sy + dy
            if 48 <= yy < 64:
                cur = px[sx, yy]
                blend = lerp(cur[:3], VOID_STAR, 0.40)
                px[sx, yy] = (blend[0], blend[1], blend[2], 255)

# Add a few bright void rifts (small purple-bright sparks)
for _ in range(8):
    rx = rng.randint(0, W-1); ry = rng.randint(48, 63)
    cur = px[rx, ry]
    blend = lerp(cur[:3], VOID_BRIGHT, 0.55)
    px[rx, ry] = (blend[0], blend[1], blend[2], 255)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_planetarium_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m23_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
