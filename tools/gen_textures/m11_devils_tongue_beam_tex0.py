import base64, math, random
from PIL import Image
W=H=64; img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()

def hx(c):
    c=c.lstrip('#')
    return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))

def lerp(a,b,t):
    return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

# TONGUE SURFACE TECHNIQUE — blood-red + sickly yellow beam texture
# base blood-red #1a0404, crimson #601008, bright blood #c02010, sickly yellow #d4c010
# top edge #e0d020 yellow, bottom edge #080202 near-black

rng = random.Random(42)

for y in range(H):
    for x in range(W):
        # normalized position
        ny = y / (H - 1)  # 0=top, 1=bottom in image coords
        nx = x / (W - 1)

        # vertical gradient — top=yellow, center-upper=bright blood, center=crimson, bottom=near-black
        # The tongue centre strip is around ny=0.35-0.55
        if ny < 0.08:
            # Top edge: pure yellow
            t = ny / 0.08
            base = lerp(hx('#e0d020'), hx('#d4c010'), t)
        elif ny < 0.28:
            # Upper bright zone: yellow fading to bright blood
            t = (ny - 0.08) / 0.20
            base = lerp(hx('#d4c010'), hx('#c02010'), t)
        elif ny < 0.50:
            # Centre bright blood zone
            t = (ny - 0.28) / 0.22
            base = lerp(hx('#c02010'), hx('#901018'), t)
        elif ny < 0.72:
            # Lower crimson zone
            t = (ny - 0.50) / 0.22
            base = lerp(hx('#901018'), hx('#601008'), t)
        elif ny < 0.92:
            # Deep blood
            t = (ny - 0.72) / 0.20
            base = lerp(hx('#601008'), hx('#1a0404'), t)
        else:
            # Bottom edge: near-black blood
            t = (ny - 0.92) / 0.08
            base = lerp(hx('#1a0404'), hx('#080202'), t)

        # Horizontal variation: slight tongue grain (vertical streaks)
        streak = math.sin(nx * math.pi * 14 + rng.uniform(-0.3, 0.3)) * 0.06
        r = max(0, min(255, base[0] + int(streak * 40)))
        g = max(0, min(255, base[1] + int(streak * 20)))
        b = max(0, min(255, base[2] + int(streak * 8)))

        # Barb grouping bands every 6 rows (slightly darker)
        if (y % 6) == 0:
            r = max(0, r - 12)
            g = max(0, g - 8)
            b = max(0, b - 4)

        # Occasional yellow hot-spot flecks
        if rng.random() < 0.03 and ny < 0.4:
            fleck_t = rng.uniform(0.5, 1.0)
            r = min(255, int(r * (1-fleck_t) + 220 * fleck_t))
            g = min(255, int(g * (1-fleck_t) + 200 * fleck_t))
            b = min(255, int(b * (1-fleck_t) + 16 * fleck_t))

        px[x,y] = (r, g, b, 255)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/devils_tongue_beam_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m11_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
