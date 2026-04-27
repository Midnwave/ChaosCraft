"""
Texture 0 for blood_comet: CLOTTED BLOOD SURFACE.
Deep dried-blood #1a0404, dark clot #300808, wet blood #700e0e, bright fresh #c01010.
Specular #ff2020. Emissive #e02020 at wettest zones. Bottom near-black, top brightest.
Irregular dithered transitions — biological feel.
"""
import base64, math, random
from PIL import Image
W=H=64; img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()

def hx(c):
    c=c.lstrip('#')
    return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))

def lerp(a,b,t):
    return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

rng = random.Random(2424)

DRIED      = hx('#1a0404')
CLOT       = hx('#300808')
WET        = hx('#700e0e')
FRESH      = hx('#c01010')
SPEC       = hx('#ff2020')
EMISS      = hx('#e02020')
NEAR_BLACK = hx('#080202')

# generate "wet zones" — irregular blobs where blood is fresh
wet_blobs = []
for _ in range(12):
    bx = rng.randint(4, 60)
    by = rng.randint(2, 48)
    br = rng.uniform(3, 8)
    intensity = rng.uniform(0.5, 1.0)
    wet_blobs.append((bx, by, br, intensity))

# generate "clot lumps" — darker spots
clot_blobs = []
for _ in range(15):
    bx = rng.randint(2, 62)
    by = rng.randint(2, 62)
    br = rng.uniform(2, 5)
    clot_blobs.append((bx, by, br))

for y in range(H):
    for x in range(W):
        nx = x / (W - 1)
        ny = y / (H - 1)

        # vertical: top brightest fresh, bottom dried near-black
        if ny < 0.30:
            t = ny / 0.30
            base = lerp(FRESH, WET, t)
        elif ny < 0.60:
            t = (ny - 0.30) / 0.30
            base = lerp(WET, CLOT, t)
        elif ny < 0.90:
            t = (ny - 0.60) / 0.30
            base = lerp(CLOT, DRIED, t)
        else:
            t = (ny - 0.90) / 0.10
            base = lerp(DRIED, NEAR_BLACK, t)

        # apply wet blobs (brighten + redden)
        wet_strength = 0
        for bx, by, br, inten in wet_blobs:
            d = math.sqrt((x-bx)**2 + (y-by)**2)
            if d < br:
                ws = (1 - d/br) * inten
                wet_strength = max(wet_strength, ws)
        if wet_strength > 0:
            base = lerp(base, FRESH, wet_strength * 0.7)

        # apply clot blobs (darken)
        clot_strength = 0
        for bx, by, br in clot_blobs:
            d = math.sqrt((x-bx)**2 + (y-by)**2)
            if d < br:
                cs = (1 - d/br)
                clot_strength = max(clot_strength, cs)
        if clot_strength > 0:
            base = lerp(base, CLOT, clot_strength * 0.6)

        # heavy random dither for irregular biological feel
        # transition between dark/bright is uneven
        dither = rng.random()
        if dither < 0.10:
            # random fresh fleck
            base = lerp(base, FRESH, 0.4)
        elif dither < 0.20:
            # random clot fleck
            base = lerp(base, DRIED, 0.4)

        # smaller rgb noise
        rn = rng.random() * 14 - 7
        r = max(0, min(255, base[0] + int(rn*1.3)))
        g = max(0, min(255, base[1] + int(rn*0.5)))
        b = max(0, min(255, base[2] + int(rn*0.5)))

        # specular highlights — small bright pixels in wettest zones
        if wet_strength > 0.7 and rng.random() < 0.07:
            r = SPEC[0]; g = SPEC[1]; b = SPEC[2]

        # emissive boost at brightest wet zones
        if wet_strength > 0.85:
            r = min(255, r + 20)

        px[x,y] = (r, g, b, 255)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/blood_comet_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m14_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
