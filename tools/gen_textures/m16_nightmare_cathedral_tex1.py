import base64, math, random
from PIL import Image
W=H=64; img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()

def hx(c):
    c=c.lstrip('#')
    return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))

def lerp(a,b,t):
    return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

rng = random.Random(1617)

# STAINED GLASS INTERIOR — pure emissive purple
# Centre #e060ff near-white-purple, outer #6020b0 deep violet, edge #100820 near-black
# Light coming through it from the other side
CENTER = hx('#e060ff')
OUTER  = hx('#6020b0')
EDGE   = hx('#100820')
DEEP   = hx('#040208')
HOT    = hx('#ff90ff')

cx_, cy_ = W/2.0, H/2.0
maxdist = math.sqrt(cx_**2 + cy_**2)

for y in range(H):
    for x in range(W):
        dx = x - cx_
        dy = y - cy_
        dist = math.sqrt(dx*dx + dy*dy)
        nd = dist / maxdist  # 0=center, 1=corner

        # Radial gradient: center hot -> middle outer -> edge dark
        if nd < 0.30:
            t = nd / 0.30
            base = lerp(HOT, CENTER, t)
        elif nd < 0.65:
            t = (nd - 0.30) / 0.35
            base = lerp(CENTER, OUTER, t)
        elif nd < 0.90:
            t = (nd - 0.65) / 0.25
            base = lerp(OUTER, EDGE, t)
        else:
            t = (nd - 0.90) / 0.10
            base = lerp(EDGE, DEEP, t)

        r, g, b = base

        # Stained glass leading (lead came framework) — dark lines
        # Vertical lines every 12 cols, horizontal every 14 rows
        if (x % 12 == 0) or (x % 12 == 11):
            r = max(0, r // 4)
            g = max(0, g // 4)
            b = max(0, b // 4)
        if (y % 14 == 0) or (y % 14 == 13):
            r = max(0, r // 4)
            g = max(0, g // 4)
            b = max(0, b // 4)

        # Diagonal lead came forming diamond pattern
        if abs((x + y) % 16 - 8) < 1 and dist < maxdist * 0.7:
            r = max(0, r // 3)
            g = max(0, g // 3)
            b = max(0, b // 3)

        # Light shimmer rays from center
        ang = math.atan2(dy, dx)
        ray = math.sin(ang * 8) * 0.08 + 1.0
        r = min(255, int(r * ray))
        g = min(255, int(g * ray))
        b = min(255, int(b * ray))

        # Bright bloom at center
        if nd < 0.15:
            bloom = (1.0 - nd / 0.15) * 30
            r = min(255, int(r + bloom * 0.5))
            g = min(255, int(g + bloom * 0.3))
            b = min(255, int(b + bloom * 0.6))

        # Stained glass speckles (impurities)
        if rng.random() < 0.015:
            r = max(0, min(255, r + rng.randint(-20, 25)))
            g = max(0, min(255, g + rng.randint(-20, 15)))
            b = max(0, min(255, b + rng.randint(-15, 30)))

        px[x,y] = (r, g, b, 255)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_cathedral_tex1.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m16_tex1_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
