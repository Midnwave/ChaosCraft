import base64, math, random
from PIL import Image
W=H=64; img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()

def hx(c):
    c=c.lstrip('#')
    return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))

def lerp(a,b,t):
    return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

rng = random.Random(99)

# SILVER FEATHER TECHNIQUE
# bright silver-white #d8dce8, mid silver #8090a8, deep quill shadow #202830
# specular #ffffff, emissive #c8d8f8
# Fine diagonal grain (~20 deg from horizontal) — feather barbs
# Centre strip brightest, vane edges slightly darker

SILVER_BRIGHT = hx('#d8dce8')
SILVER_MID    = hx('#8090a8')
SILVER_DEEP   = hx('#202830')
SILVER_WHITE  = hx('#ffffff')
SILVER_EMISS  = hx('#c8d8f8')

for y in range(H):
    for x in range(W):
        nx = x / (W - 1)  # 0=left, 1=right
        ny = y / (H - 1)  # 0=top, 1=bottom

        # Centre strip is brightest (nx around 0.45-0.55)
        centre_dist = abs(nx - 0.50)
        centre_factor = max(0.0, 1.0 - centre_dist / 0.35)  # falloff from centre

        # Diagonal grain at ~20 degrees
        # barb direction: mostly horizontal but tilted slightly
        grain_coord = x * math.cos(math.radians(20)) + y * math.sin(math.radians(20))
        grain = math.sin(grain_coord * 0.8) * 0.04 + math.sin(grain_coord * 2.1) * 0.02

        # Base vertical gradient: top=bright, bottom=dark (quill shadow)
        if ny < 0.15:
            base = lerp(SILVER_WHITE, SILVER_BRIGHT, ny/0.15)
        elif ny < 0.55:
            t = (ny - 0.15) / 0.40
            base = lerp(SILVER_BRIGHT, SILVER_MID, t)
        elif ny < 0.85:
            t = (ny - 0.55) / 0.30
            base = lerp(SILVER_MID, SILVER_DEEP, t)
        else:
            t = (ny - 0.85) / 0.15
            base = lerp(SILVER_DEEP, hx('#101820'), t)

        # Centre brightening
        boost = int(centre_factor * 30)
        r = min(255, base[0] + boost + int(grain * 20))
        g = min(255, base[1] + boost + int(grain * 20))
        b = min(255, base[2] + boost + int(grain * 25))

        # Barb grouping bands every 6 rows (slightly darker separation)
        if (y % 6) in (0, 1):
            band_dark = 8
            r = max(0, r - band_dark)
            g = max(0, g - band_dark)
            b = max(0, b - band_dark)

        # Emissive cold blue tint near top
        if ny < 0.3:
            blue_boost = int((0.3 - ny) / 0.3 * 15)
            b = min(255, b + blue_boost)

        # Occasional specular fleck
        if rng.random() < 0.015 and centre_dist < 0.15 and ny < 0.4:
            r = min(255, r + 40)
            g = min(255, g + 40)
            b = min(255, b + 50)

        px[x,y] = (r, g, b, 255)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/silver_wing_blade_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m12_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
