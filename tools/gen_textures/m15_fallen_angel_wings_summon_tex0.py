import base64, math, random
from PIL import Image
W=H=64; img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()

def hx(c):
    c=c.lstrip('#')
    return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))

def lerp(a,b,t):
    return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

rng = random.Random(1515)

# CORRUPTED FEATHER TECHNIQUE
# Bone-white #e8e4dc at base (left) -> sickly yellow-green #7a9010 in middle ->
# deep contamination-purple #3010a0 at tip (right)
# Hard-edge horizontal barb lines.
WHITE  = hx('#e8e4dc')
WHITE2 = hx('#f8f4ec')
SICK   = hx('#7a9010')
SICK2  = hx('#8aa018')
PURPLE = hx('#3010a0')
PURPLE2= hx('#5020c0')
SHADOW = hx('#1a1812')
DEEP   = hx('#08040a')

for y in range(H):
    for x in range(W):
        nx = x / (W - 1)  # 0=left (base/white) -> 1=right (tip/purple)
        ny = y / (H - 1)

        # 3-stop gradient based on nx
        if nx < 0.40:
            t = nx / 0.40
            base = lerp(WHITE2, WHITE, t)
        elif nx < 0.55:
            t = (nx - 0.40) / 0.15
            base = lerp(WHITE, SICK, t)
        elif nx < 0.70:
            t = (nx - 0.55) / 0.15
            base = lerp(SICK, SICK2, t)
        elif nx < 0.85:
            t = (nx - 0.70) / 0.15
            base = lerp(SICK2, PURPLE2, t)
        else:
            t = (nx - 0.85) / 0.15
            base = lerp(PURPLE2, PURPLE, t)

        r, g, b = base

        # Hard-edge horizontal barb lines every 4 rows
        # Barbs angle slightly off horizontal (~5 deg)
        barb_y = y + int(x * 0.08)
        if (barb_y % 4) == 0:
            # darker barb line separator
            r = max(0, r - 30)
            g = max(0, g - 30)
            b = max(0, b - 22)
        elif (barb_y % 4) == 3:
            # slight highlight on barb top
            r = min(255, r + 14)
            g = min(255, g + 14)
            b = min(255, b + 14)

        # Central rachis (vertical strip along middle Y) is darker
        cy = abs(ny - 0.5)
        if cy < 0.04:
            r = max(0, r - 40)
            g = max(0, g - 40)
            b = max(0, b - 30)

        # Edge darkening at top/bottom (vane edge falloff)
        if ny < 0.10:
            edge_t = ny / 0.10
            r = int(lerp((r,g,b), SHADOW, 1.0 - edge_t)[0])
            g = int(lerp((r,g,b), SHADOW, 1.0 - edge_t)[1])
            b = int(lerp((r,g,b), SHADOW, 1.0 - edge_t)[2])
        elif ny > 0.90:
            edge_t = (1.0 - ny) / 0.10
            mixed = lerp((r,g,b), SHADOW, 1.0 - edge_t)
            r,g,b = mixed

        # Corruption splotches in tip half
        if nx > 0.55 and rng.random() < 0.025:
            r = min(255, r + rng.randint(-10, 30))
            g = min(255, max(0, g + rng.randint(-30, 10)))
            b = min(255, b + rng.randint(0, 50))

        # Bright micro-flecks (texture grain) on white half
        if nx < 0.40 and rng.random() < 0.012:
            r = min(255, r + 18)
            g = min(255, g + 18)
            b = min(255, b + 18)

        # Ensure transition zone visible: yellow-green area gets a subtle veining
        if 0.55 <= nx <= 0.70:
            vein = math.sin(y * 0.8 + x * 0.4) * 10
            r = max(0, min(255, int(r + vein)))
            g = max(0, min(255, int(g + vein * 1.2)))

        px[x,y] = (r, g, b, 255)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/fallen_angel_wings_summon_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m15_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
