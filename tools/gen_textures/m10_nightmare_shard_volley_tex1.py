"""Texture 1 for nightmare_shard_volley: Pure purple radial emissive for leading glow and wake trail."""
import base64, math
from PIL import Image
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

purp_c=hx("#d060ff"); purp_m=hx("#6020a0"); void=hx("#100820"); edge=hx("#e8f0ff")

# Zone A (0,0)-(31,31): radial glow centre
cx,cy=15.5,15.5; maxr=math.sqrt(15.5**2+15.5**2)
for y in range(32):
    for x in range(32):
        r=math.sqrt((x-cx)**2+(y-cy)**2)/maxr
        if r<0.2: col=lerp(edge,purp_c,r/0.2)
        elif r<0.55: col=lerp(purp_c,purp_m,(r-0.2)/0.35)
        else: col=lerp(purp_m,void,(r-0.55)/0.45)
        px[x,y]=col+(255,)

# Zone B (32,0)-(63,31): secondary glow ring
cx2=47.5
for y in range(32):
    for x in range(32,64):
        r=math.sqrt((x-cx2)**2+(y-cy)**2)/maxr
        if r<0.3: col=lerp(purp_c,purp_m,r/0.3)
        elif r<0.7: col=lerp(purp_m,void,(r-0.3)/0.4)
        else: col=void
        # pulse rings
        ring=int(r*12)%3
        if ring==0: col=lerp(col,purp_c,0.2)
        px[x,y]=col+(255,)

# Zone C (0,32)-(31,63): wake trail — elongated purple streak
for y in range(32,64):
    for x in range(32):
        xr=x; yr=y-32
        # elongated streak horizontally
        dist_from_centre=abs(yr-15.5)/15.5
        col=lerp(purp_m,void,dist_from_centre)
        # bright centre line
        if abs(yr-15.5)<2: col=lerp(col,purp_c,0.6)
        # fade at edges
        edge_fade=min(xr,31-xr)/15.5
        col=lerp(void,col,edge_fade)
        px[x,y]=col+(255,)

# Zone D (32,32)-(63,63): deep void with purple shimmer
for y in range(32,64):
    for x in range(32,64):
        xr=x-32; yr=y-32
        col=void
        # faint purple shimmer rings
        r=math.sqrt((xr-15.5)**2+(yr-15.5)**2)/maxr
        if int(r*8)%2==0: col=lerp(void,purp_m,0.15)
        if r<0.15: col=lerp(purp_m,purp_c,0.5)
        px[x,y]=col+(255,)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_shard_volley_tex1.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m10_tex1_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
