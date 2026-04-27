"""
Texture 1 for infernal_scripture_array: lectern / ink drops.
Deep mahogany wood base #200c08, dark wood grain #100604, blood-red wood stain #601010.
Ink drops use centre emissive zone (purple).
"""
import base64, math, random
from PIL import Image
W=H=64; img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()

def hx(c):
    c=c.lstrip('#')
    return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))

def lerp(a,b,t):
    return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

rng = random.Random(2425)

WOOD       = hx('#200c08')
WOOD_DARK  = hx('#100604')
WOOD_LITE  = hx('#382014')
STAIN_RED  = hx('#601010')
STAIN_DEEP = hx('#902020')
GRAIN      = hx('#180a06')
PURPLE     = hx('#9060c0')
PURPLE_BR  = hx('#c890ff')
PURPLE_DK  = hx('#6038a0')

# Pre-generate vertical wood grain
grain_lines = []
for _ in range(14):
    x0 = rng.randint(0, W-1)
    amp = rng.uniform(0.4, 1.5)
    freq = rng.uniform(0.10, 0.25)
    phase = rng.uniform(0, math.pi*2)
    grain_lines.append((x0, amp, freq, phase))

# Pre-generate stain blotches
red_stains = []
for _ in range(5):
    sx = rng.randint(2, W-3)
    sy = rng.randint(2, H-3)
    sr = rng.uniform(2, 5)
    red_stains.append((sx, sy, sr))

# Fill wood base with vertical gradient
for y in range(H):
    for x in range(W):
        ny = y / (H - 1)
        # darker top, slightly lighter middle
        if ny < 0.5:
            base = lerp(WOOD_DARK, WOOD, ny*2 * 0.7 + 0.3)
        else:
            base = lerp(WOOD, WOOD_LITE, (ny-0.5)*0.6)
        # noise (woody grain)
        n = rng.random() * 12 - 6
        r,g,b = base
        r=max(0,min(255,r+int(n)))
        g=max(0,min(255,g+int(n*0.7)))
        b=max(0,min(255,b+int(n*0.5)))
        px[x,y] = (r,g,b,255)

# Draw vertical grain lines (curved)
for (x0, amp, freq, phase) in grain_lines:
    for y in range(H):
        xv = x0 + amp * math.sin(y * freq + phase)
        xi = int(round(xv))
        if 0 <= xi < W:
            cur = px[xi, y]
            blend = lerp(cur[:3], GRAIN, 0.55)
            px[xi, y] = (blend[0], blend[1], blend[2], 255)

# Red blood-stain blotches
for (sx, sy, sr) in red_stains:
    for dy in range(-int(sr*1.6), int(sr*1.6)+1):
        for dx in range(-int(sr*1.6), int(sr*1.6)+1):
            xx = sx + dx; yy = sy + dy
            # don't paint stains inside emissive zone
            if 34 <= xx < 62 and 34 <= yy < 62: continue
            if 0 <= xx < W and 0 <= yy < H:
                d = math.sqrt(dx*dx + dy*dy)
                if d < sr*1.6:
                    falloff = 1 - d/(sr*1.6)
                    target = STAIN_RED if d > sr*0.5 else STAIN_DEEP
                    cur = px[xx, yy]
                    blend = lerp(cur[:3], target, falloff * 0.55)
                    px[xx, yy] = (blend[0], blend[1], blend[2], 255)

# ─── Emissive zone (rows 34-62, cols 34-62): ink-drop purple glow ───
ec = (47.5, 47.5)
for y in range(34, 62):
    for x in range(34, 62):
        d = math.sqrt((x-ec[0])**2 + (y-ec[1])**2)
        # Bright concentric pattern — drop with halo
        if d < 4:
            t = 1 - d/4
            cur = px[x, y]
            blend = lerp(cur[:3], PURPLE_BR, t * 0.92)
            px[x, y] = (blend[0], blend[1], blend[2], 255)
        elif d < 8:
            t = 1 - (d-4)/4
            cur = px[x, y]
            blend = lerp(cur[:3], PURPLE, t * 0.75)
            px[x, y] = (blend[0], blend[1], blend[2], 255)
        elif d < 13:
            t = 1 - (d-8)/5
            cur = px[x, y]
            blend = lerp(cur[:3], PURPLE_DK, t * 0.55)
            px[x, y] = (blend[0], blend[1], blend[2], 255)
        else:
            # darken background of emissive zone
            cur = px[x, y]
            blend = lerp(cur[:3], WOOD_DARK, 0.40)
            px[x, y] = (blend[0], blend[1], blend[2], 255)

# Add a few smaller ink-drop spatters around emissive centre
spatters = [(40, 40), (54, 38), (38, 54), (56, 56), (44, 58), (58, 46)]
for (sx, sy) in spatters:
    if 34 <= sx < 62 and 34 <= sy < 62:
        cur = px[sx, sy]
        blend = lerp(cur[:3], PURPLE_BR, 0.85)
        px[sx, sy] = (blend[0], blend[1], blend[2], 255)
        # tiny halo
        for dx, dy in [(1,0),(-1,0),(0,1),(0,-1)]:
            xx = sx+dx; yy = sy+dy
            if 34 <= xx < 62 and 34 <= yy < 62:
                cur2 = px[xx, yy]
                blend2 = lerp(cur2[:3], PURPLE, 0.40)
                px[xx, yy] = (blend2[0], blend2[1], blend2[2], 255)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/infernal_scripture_array_tex1.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m24_tex1_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
