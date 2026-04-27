"""TEX1 for fallen_halo_burst: divine corruption radial — inner glow / fractures / shadow."""
import base64, math
from PIL import Image
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

divine=hx("#fffff0"); yel=hx("#d0c840"); purp=hx("#6020a0"); blk=hx("#080410"); brightpurp=hx("#c040e0")

def radial_zone(px, x0,y0,cx,cy,maxr):
    r=math.sqrt((x0-cx)**2+(y0-cy)**2)/maxr
    if r<0.15: col=lerp(divine,divine,r/0.15)
    elif r<0.35: col=lerp(divine,yel,(r-0.15)/0.20)
    elif r<0.65: col=lerp(yel,purp,(r-0.35)/0.30)
    elif r<0.90: col=lerp(purp,blk,(r-0.65)/0.25)
    else: col=blk
    if abs(r-0.30)<0.04: col=lerp(col,divine,0.5)
    return col

# Zone A (TL): radial from center (16,16)
cx,cy=16.0,16.0; maxr=math.sqrt(16.0**2+16.0**2)
for y in range(32):
    for x in range(32):
        col=radial_zone(px,x,y,cx,cy,maxr)
        px[x,y]=col+(255,)

# Zone B (TR): radial from (48,16) — slightly offset variant
cx2,cy2=48.0,16.0; maxr2=math.sqrt(16.0**2+16.0**2)
for y in range(32):
    for x in range(32,64):
        col=radial_zone(px,x,y,cx2,cy2,maxr2)
        # slightly cooler hue — shift purple brighter
        r=math.sqrt((x-cx2)**2+(y-cy2)**2)/maxr2
        if 0.35<r<0.65:
            col=lerp(col,brightpurp,0.2)
        px[x,y]=col+(255,)

# Zone C (BL): radial from (16,48) — bright purple disc zone
cx3,cy3=16.0,48.0; maxr3=math.sqrt(16.0**2+16.0**2)
for y in range(32,64):
    for x in range(32):
        r=math.sqrt((x-cx3)**2+(y-cy3)**2)/maxr3
        if r<0.20: col=brightpurp
        elif r<0.45: col=lerp(brightpurp,purp,(r-0.20)/0.25)
        elif r<0.75: col=lerp(purp,blk,(r-0.45)/0.30)
        else: col=blk
        if abs(r-0.22)<0.03: col=lerp(col,divine,0.7)
        px[x,y]=col+(255,)

# Zone D (BR): near-black outer
cx4,cy4=48.0,48.0; maxr4=math.sqrt(16.0**2+16.0**2)
for y in range(32,64):
    for x in range(32,64):
        r=math.sqrt((x-cx4)**2+(y-cy4)**2)/maxr4
        if r<0.25: col=lerp(blk,purp,r/0.25)
        else: col=blk
        px[x,y]=col+(255,)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/fallen_halo_burst_tex1.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m04_tex1_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
