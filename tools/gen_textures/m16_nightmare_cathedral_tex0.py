import base64, math, random
from PIL import Image
W=H=64; img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()

def hx(c):
    c=c.lstrip('#')
    return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))

def lerp(a,b,t):
    return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

rng = random.Random(1616)

# GOTHIC STONE TECHNIQUE
# Aged limestone #b0a898, deep shadow #2a2820, carved detail #707060,
# bright weathered peak #d8d0c0, purple emissive #9060d0 at deepest grooves.
# Horizontal sediment banding every 10 rows. Top lighter, bottom darker.
LIME    = hx('#b0a898')
SHADOW  = hx('#2a2820')
CARVED  = hx('#707060')
PEAK    = hx('#d8d0c0')
EMISS   = hx('#9060d0')
EMISS_D = hx('#5030a0')
DEEP    = hx('#181410')

for y in range(H):
    for x in range(W):
        nx = x / (W - 1)
        ny = y / (H - 1)

        # Vertical gradient: top bright (peak/exposed) -> bottom dark (foundation buried)
        if ny < 0.20:
            base = lerp(PEAK, LIME, ny / 0.20)
        elif ny < 0.70:
            t = (ny - 0.20) / 0.50
            base = lerp(LIME, CARVED, t * 0.6)
        elif ny < 0.90:
            t = (ny - 0.70) / 0.20
            base = lerp(CARVED, SHADOW, t)
        else:
            t = (ny - 0.90) / 0.10
            base = lerp(SHADOW, DEEP, t)

        r, g, b = base

        # Horizontal sediment banding every 10 rows (slight darker bands)
        band_pos = y % 10
        if band_pos == 0:
            r = max(0, r - 35)
            g = max(0, g - 35)
            b = max(0, b - 30)
        elif band_pos == 9:
            r = max(0, r - 18)
            g = max(0, g - 18)
            b = max(0, b - 15)

        # Vertical carving grooves (every 8 cols)
        col_pos = x % 8
        if col_pos == 0 or col_pos == 7:
            # carved groove darker
            r = max(0, r - 25)
            g = max(0, g - 25)
            b = max(0, b - 22)

        # Stone irregularity: random darker mortar lines / cracks
        noise = (math.sin(x * 0.3 + y * 0.7) + math.sin(x * 0.9 - y * 0.4)) * 8
        r = max(0, min(255, int(r + noise)))
        g = max(0, min(255, int(g + noise)))
        b = max(0, min(255, int(b + noise * 0.8)))

        # Deep groove emissive: at exact intersection of band+groove, purple shows through
        if (band_pos == 0 and (col_pos == 0 or col_pos == 7)):
            t = rng.random()
            ec = lerp(EMISS_D, EMISS, t)
            r, g, b = ec
        elif (band_pos == 0 and col_pos in (3, 4)) and rng.random() < 0.4:
            # additional groove crack
            r = max(0, r - 20)
            g = max(0, g - 20)
            b = min(255, b + 25)

        # Stone speckles
        if rng.random() < 0.04:
            sp = rng.randint(-15, 12)
            r = max(0, min(255, r + sp))
            g = max(0, min(255, g + sp))
            b = max(0, min(255, b + sp))

        # Bright weathered highlights randomly near top
        if ny < 0.3 and rng.random() < 0.02:
            r = min(255, r + 30)
            g = min(255, g + 30)
            b = min(255, b + 28)

        # Purple corruption seeping from random crack points
        if rng.random() < 0.008 and 0.2 < ny < 0.8:
            r = max(0, r - 10)
            b = min(255, b + 60)
            g = max(0, g - 5)

        px[x,y] = (r, g, b, 255)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_cathedral_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m16_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
