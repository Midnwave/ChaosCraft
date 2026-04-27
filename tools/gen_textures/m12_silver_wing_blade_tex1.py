import base64, math, random
from PIL import Image
W=H=64; img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()

def hx(c):
    c=c.lstrip('#')
    return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))

def lerp(a,b,t):
    return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

rng = random.Random(55)

# RAZOR EDGE TEXTURE: void black #030308 with two bright silver edge lines
# at cols 30-31 and 32-33 — the absolute razor edge catching light
# Emissive #a8d0ff

for y in range(H):
    for x in range(W):
        # Almost entirely black
        r, g, b = 3, 3, 8

        # The two razor lines at x=30,31 and x=32,33
        if x == 30 or x == 31:
            # Left edge of razor — bright silver
            t = (x - 30) / 1.0
            edge_c = lerp(hx('#a8d0ff'), hx('#c8e0f8'), t)
            r, g, b = edge_c
        elif x == 32 or x == 33:
            # Right edge of razor — slightly less bright
            t = (x - 32) / 1.0
            edge_c = lerp(hx('#c8e0f8'), hx('#a8d0ff'), t)
            r, g, b = edge_c

        # Very subtle near-black ambient for depth near the lines
        elif x in (28, 29):
            shadow = int((x - 27) * 4)
            r, g, b = shadow, shadow, shadow + 6
        elif x in (34, 35):
            shadow = int((36 - x) * 4)
            r, g, b = shadow, shadow, shadow + 6

        # A couple sparse specular dust flecks in upper half (trailing dust particles)
        elif y < 32 and rng.random() < 0.008:
            r = rng.randint(80, 180)
            g = rng.randint(90, 200)
            b = rng.randint(120, 240)

        px[x,y] = (r, g, b, 255)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/silver_wing_blade_tex1.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m12_tex1_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
