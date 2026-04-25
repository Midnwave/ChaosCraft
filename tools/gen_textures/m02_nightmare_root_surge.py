"""Texture for nightmare_root_surge.
4-zone 64x64 layout per MASTER_UV_STRATEGY.md.
Palette: sickly yellow-black (toxic, not warm).
"""
import base64, math, random
from PIL import Image

random.seed(2002)
W = H = 64
img = Image.new("RGBA", (W, H), (0,0,0,255))
px = img.load()

def hx(c):
    c = c.lstrip('#')
    return (int(c[0:2],16), int(c[2:4],16), int(c[4:6],16))

def lerp(a,b,t):
    return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

void   = hx("#0c0e08")  # base
greenB = hx("#141c08")  # mid green-black
mid    = hx("#3a4808")  # sickly yellow-green
high   = hx("#8a9010")  # toxic yellow
emis   = hx("#d4c820")  # emissive
spec   = hx("#e8d830")  # specular

# Zone A (TL 0..31, 0..31): primary-lit bark — vertical gradient bright→dark
for y in range(32):
    for x in range(32):
        t = y / 31.0
        col = lerp(high, void, t*0.85)
        # horizontal banding (grain)
        if y % 4 == 0:
            col = lerp(col, mid, 0.4)
        # specular cluster at (8,4)
        if abs(x-8) <= 2 and abs(y-4) <= 1:
            col = spec
        px[x,y] = col + (255,)

# Zone B (TR 32..63, 0..31): primary-shadow — 50% darker version
for y in range(32):
    for x in range(32, 64):
        t = y / 31.0
        col = lerp(greenB, void, t*0.9)
        if y % 4 == 0:
            col = lerp(col, hx("#1a2208"), 0.5)
        px[x,y] = col + (255,)

# Zone C (BL 0..31, 32..63): top/highlight — brightest yellow-green
for y in range(32, 64):
    for x in range(32):
        t = (y - 32) / 31.0
        col = lerp(spec, mid, t)
        if (y % 5) == 0:
            col = lerp(col, emis, 0.3)
        px[x,y] = col + (255,)

# Zone D (BR 32..63, 32..63): emissive radial
cx, cy = 47.5, 47.5
maxr = math.sqrt(15.5**2 + 15.5**2)
for y in range(32, 64):
    for x in range(32, 64):
        r = math.sqrt((x-cx)**2 + (y-cy)**2) / maxr
        if r < 0.25:
            col = lerp(spec, emis, r/0.25)
        elif r < 0.6:
            col = lerp(emis, mid, (r-0.25)/0.35)
        else:
            col = lerp(mid, void, min(1.0, (r-0.6)/0.4))
        # ring detail at 35%
        if abs(r - 0.35) < 0.04:
            col = lerp(col, hx("#b0a010"), 0.6)
        px[x,y] = col + (255,)

out_png = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_root_surge_tex.png"
img.save(out_png)
b64 = base64.b64encode(open(out_png,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m02_b64.txt","w").write(b64)
import os
print(f"PNG: {os.path.getsize(out_png)}, b64: {len(b64)}")
