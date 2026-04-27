"""Texture 1 for fallen_angel_descent - compressed purple radial glow for crater floor/disc."""
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

# For zones A/B/C: dark with radial purple from zone center
def zone_pixel(lx, ly):
    cx=16; cy=16
    dist=math.sqrt((lx-cx)**2+(ly-cy)**2)
    max_d=22.0
    t=min(dist/max_d,1.0)
    void_col=hx('060410')
    build1=hx('401880')
    build2=hx('8040d0')
    if t<0.3:
        base=lerp(void_col,build1,t/0.3)
    elif t<0.4:
        base=lerp(build1,build2,(t-0.3)/0.1)
    else:
        base=lerp(build2,void_col,(t-0.4)/0.6)
    # concentric ring details
    ring_val = math.sin(dist*1.2)*0.5+0.5
    ring_bright = hx('6020a0')
    base = lerp(base, ring_bright, ring_val*0.15)
    return base

# Zone A (TL 0-32 x 0-32)
for y in range(32):
    for x in range(32):
        base = zone_pixel(x, y)
        px[x,y] = base+(255,)

# Zone B (TR 32-64 x 0-32) - same radial from TL local coords
for y in range(32):
    for x in range(32):
        rx=x+32
        base = zone_pixel(x, y)
        # slightly darker
        base = lerp(base, hx('000000'), 0.3)
        px[rx,y] = base+(255,)

# Zone C (BL 0-32 x 32-64) - same
for y in range(32):
    for x in range(32):
        ry=y+32
        base = zone_pixel(x, y)
        # slightly brighter top
        bright_t = max(0, 1.0-(y/15.0))
        base = lerp(base, hx('a060e0'), bright_t*0.25)
        px[x,ry] = base+(255,)

# Zone D (BR 32-64 x 32-64): bright emissive center
cx_d=47; cy_d=47
for y in range(32):
    for x in range(32):
        rx=x+32; ry=y+32
        dist=math.sqrt((rx-cx_d)**2+(ry-cy_d)**2)
        t=min(dist/16.0,1.0)
        center_col=hx('c080ff')
        edge_col=hx('200840')
        base=lerp(center_col, edge_col, t)
        # inner glow bloom
        if dist<6:
            base=lerp(base, hx('ffffff'), (1.0-dist/6.0)*0.4)
        px[rx,ry] = base+(255,)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/fallen_angel_descent_tex1.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m01_tex1_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
