"""
Texture 1 for blood_comet: COMET TAIL / HEAT SHIMMER.
Blood-red fading to sickly yellow-orange at tail tips.
Centre strip #ff2010, mid #c08010, outer #402008, edge near-black.
Burning-blood mix at edges.
"""
import base64, math, random
from PIL import Image
W=H=64; img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()

def hx(c):
    c=c.lstrip('#')
    return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))

def lerp(a,b,t):
    return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

rng = random.Random(2525)

CENTRE     = hx('#ff2010')
MID        = hx('#c08010')
OUTER      = hx('#402008')
EDGE       = hx('#080200')
BRIGHT     = hx('#ff6020')
EMISS      = hx('#ffa040')

for y in range(H):
    for x in range(W):
        nx = x / (W - 1)
        ny = y / (H - 1)

        # horizontal centre-strip gradient — bright middle, fading sides
        centre_dist = abs(nx - 0.5)
        # vertical gradient: top brightest fresh blood-red, bottom yellow-orange tail tips
        # We treat top = near-comet, bottom = tip (sickly yellow)

        if centre_dist < 0.10:
            # centre strip — brightest red
            band = CENTRE
            if ny < 0.4:
                base = lerp(BRIGHT, CENTRE, ny / 0.4)
            elif ny < 0.7:
                t = (ny - 0.4) / 0.3
                base = lerp(CENTRE, MID, t)
            else:
                t = (ny - 0.7) / 0.3
                base = lerp(MID, OUTER, t)
        elif centre_dist < 0.30:
            # mid zone — burning blood mix
            t_h = (centre_dist - 0.10) / 0.20
            if ny < 0.5:
                base = lerp(CENTRE, MID, ny / 0.5)
            else:
                base = lerp(MID, OUTER, (ny - 0.5) / 0.5)
            # fade to outer
            base = lerp(base, OUTER, t_h * 0.4)
        else:
            # outer zone
            t_h = (centre_dist - 0.30) / 0.20
            if ny < 0.5:
                base = lerp(MID, OUTER, ny / 0.5)
            else:
                base = lerp(OUTER, EDGE, (ny - 0.5) / 0.5)
            base = lerp(base, EDGE, min(1.0, t_h * 0.7))

        # heat-shimmer wavy noise
        wave = math.sin(x * 0.4 + y * 0.2) * 0.08 + math.sin(y * 0.6 + x * 0.1) * 0.05
        # noise dither
        n = rng.random() * 18 - 9
        r = max(0, min(255, base[0] + int(n*1.3 + wave*30)))
        g = max(0, min(255, base[1] + int(n*0.7 + wave*15)))
        b = max(0, min(255, base[2] + int(n*0.5 + wave*5)))

        # occasional bright emissive embers in centre band
        if centre_dist < 0.15 and rng.random() < 0.03:
            r = EMISS[0]; g = EMISS[1]; b = EMISS[2]

        # very rare specular sparks in upper centre
        if centre_dist < 0.08 and ny < 0.3 and rng.random() < 0.04:
            r = 255; g = min(255, g + 60); b = min(255, b + 30)

        px[x,y] = (r, g, b, 255)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/blood_comet_tex1.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m14_tex1_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
