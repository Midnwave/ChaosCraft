"""Texture for fallen_angel_descent - bone/stone crater surface with purple emissive contamination."""
import base64, math
from PIL import Image
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()

def hx(c):
    c=c.lstrip('#')
    return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))

def lerp(a,b,t):
    t=max(0.0,min(1.0,t))
    return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

def lerp3(a,b,c,t):
    if t<0.5: return lerp(a,b,t*2)
    else: return lerp(b,c,(t-0.5)*2)

# Zone A (TL 0-32 x 0-32): primary lit bone/stone surface
col_top_a = hx('f0ece0')
col_mid_a = hx('8a8070')
col_bot_a = hx('181412')
for y in range(32):
    for x in range(32):
        t = y / 31.0
        base = lerp3(col_top_a, col_mid_a, col_bot_a, t)
        # subtle noise
        n = ((x*7+y*13) % 17) / 17.0 * 0.06
        base = lerp(base, hx('ffffff'), n)
        # specular highlight near (6,3)
        dist_spec = math.sqrt((x-6)**2+(y-3)**2)
        if dist_spec < 2.0:
            spec_t = (2.0-dist_spec)/2.0
            base = lerp(base, hx('ffffff'), spec_t*0.6)
        px[x,y] = base+(255,)

# Zone B (TR 32-64 x 0-32): shadow side
col_top_b = hx('404038')
col_bot_b = hx('181412')
for y in range(32):
    for x in range(32):
        rx = x+32
        t = y/31.0
        base = lerp(col_top_b, col_bot_b, t)
        # diagonal stripe
        if (x+y)%8==0:
            base = lerp(base, hx('d8d0c8'), 0.3)
        # subtle specular at (10,5)
        dist_spec = math.sqrt((x-10)**2+(y-5)**2)
        if dist_spec < 2.5:
            spec_t = (2.5-dist_spec)/2.5
            base = lerp(base, hx('c0b8a8'), spec_t*0.4)
        px[rx,y] = base+(255,)

# Zone C (BL 0-32 x 32-64): top highlight (bright bone)
col_top_c = hx('f0ece0')
col_bot_c = hx('d8d0c8')
for y in range(32):
    for x in range(32):
        ry = y+32
        t = y/31.0
        base = lerp(col_top_c, col_bot_c, t)
        # fine grain
        n = ((x*11+y*5) % 13)/13.0 * 0.04
        base = lerp(base, hx('ffffff'), n)
        px[x,ry] = base+(255,)

# Zone D (BR 32-64 x 32-64): emissive purple contamination
cx_d = 47; cy_d = 47
for y in range(32):
    for x in range(32):
        rx = x+32; ry = y+32
        # distance from center of zone
        dist = math.sqrt((rx-cx_d)**2+(ry-cy_d)**2)
        max_dist = 22.0
        t = min(dist/max_dist, 1.0)
        center_col = hx('d8c8ff')
        mid_col = hx('8040c0')
        edge_col = hx('100808')
        if t < 0.4:
            base = lerp(center_col, mid_col, t/0.4)
        else:
            base = lerp(mid_col, edge_col, (t-0.4)/0.6)
        # emissive hot spots
        hot_t = max(0, 1.0 - dist/10.0)
        base = lerp(base, hx('ffffff'), hot_t*0.15)
        px[rx,ry] = base+(255,)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/fallen_angel_descent_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m01_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
