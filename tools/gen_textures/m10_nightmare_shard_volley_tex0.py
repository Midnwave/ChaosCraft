"""Texture 0 for nightmare_shard_volley: Shattered mirror silver with angular fracture lines and purple emissive."""
import base64, math, random
from PIL import Image
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

silv=hx("#c0c8d8"); shadow=hx("#181e2a"); edge=hx("#e8f0ff"); spec=hx("#ffffff"); purp=hx("#b080ff")
random.seed(10)

def draw_line(px,x0,y0,x1,y1,col,xmin=0,xmax=31,ymin=0,ymax=31):
    dx=abs(x1-x0); dy=abs(y1-y0)
    sx=1 if x0<x1 else -1; sy=1 if y0<y1 else -1
    err=dx-dy
    while True:
        if xmin<=x0<=xmax and ymin<=y0<=ymax: px[x0,y0]=col+(255,)
        if x0==x1 and y0==y1: break
        e2=2*err
        if e2>-dy: err-=dy; x0+=sx
        if e2<dx: err+=dx; y0+=sy

# Zone A (0,0)-(31,31): primary shattered mirror face
for y in range(32):
    for x in range(32):
        # Base silver gradient bright->mid
        col=lerp(edge,silv,y/31.0)
        # angular fracture lines at various angles
        # 30 degrees
        if abs(y - x*0.577) < 0.7: col=shadow
        # 60 degrees
        if abs(y - x*1.732 + 10) < 0.7: col=shadow
        # 120 degrees (steep)
        if abs(y + x*0.577 - 22) < 0.7: col=shadow
        # 45 degree
        if abs(y - x + 5) < 0.7: col=shadow
        # specular hot spot
        if x<=4 and y<=4: col=spec
        # purple emissive at sharpest points
        if (14<=x<=16) and (12<=y<=14): col=purp
        if (24<=x<=26) and (8<=y<=10): col=purp
        if (5<=x<=7) and (20<=y<=22): col=lerp(purp,silv,0.5)
        px[x,y]=col+(255,)

# Zone B (32,0)-(63,31): shadow/side face
for y in range(32):
    for x in range(32,64):
        col=lerp(silv,shadow,y/31.0)
        # softer fractures
        xr=x-32
        if abs(y-xr*0.268+3)<0.7: col=lerp(shadow,silv,0.3)
        if (xr+y)%7==0: col=lerp(col,edge,0.3)
        if xr<=3 and y<=3: col=lerp(col,spec,0.7)
        px[x,y]=col+(255,)

# Zone C (0,32)-(31,63): bright mirror edge
for y in range(32,64):
    for x in range(32):
        yr=y-32
        col=lerp(spec,edge,yr/31.0)
        if (x+yr)%4==0: col=lerp(col,silv,0.4)
        if abs(yr-x*0.5)<0.8: col=shadow
        px[x,y]=col+(255,)

# Zone D (32,32)-(63,63): deep shadow interior
for y in range(32,64):
    for x in range(32,64):
        xr=x-32; yr=y-32
        col=lerp(shadow,hx("#0c1018"),yr/31.0)
        if (xr*2+yr*3)%11==0: col=lerp(col,silv,0.15)
        if xr<=2 and yr<=2: col=purp
        px[x,y]=col+(255,)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_shard_volley_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m10_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
