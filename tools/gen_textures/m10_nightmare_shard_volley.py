"""Texture for nightmare_shard_volley. Shattered mirror silver + purple radial."""
import base64, math
from PIL import Image
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

silv=hx("#c0c8d8"); shadow=hx("#181e2a"); edge=hx("#e8f0ff"); spec=hx("#ffffff"); purp=hx("#b080ff")
purp_c=hx("#d060ff"); purp_m=hx("#6020a0"); void=hx("#100820")

# Zone A: shattered mirror silver
for y in range(32):
    for x in range(32):
        col=lerp(edge,silv,y/31.0)
        # angular fracture lines at 30°, 60°, 120°
        if abs(y - x*0.577) <= 0.5: col=shadow
        if abs(y - x*1.732 + 8) <= 0.5: col=shadow
        if abs(y + x*0.577 - 24) <= 0.5: col=shadow
        if abs(x-8)<=1 and abs(y-3)<=1: col=spec
        # purple at sharpest points
        if (x in [15,28]) and (y in [10,20]): col=purp
        px[x,y]=col+(255,)
# Zone B: shadow side
for y in range(32):
    for x in range(32,64):
        col=lerp(silv,shadow,y/31.0)
        if (x-32+y)%5==0: col=lerp(col,edge,0.4)
        px[x,y]=col+(255,)
# Zone C: brightest highlight
for y in range(32,64):
    for x in range(32):
        col=lerp(spec,edge,(y-32)/31.0)
        px[x,y]=col+(255,)
# Zone D: pure purple radial
cx,cy=47.5,47.5; maxr=math.sqrt(15.5**2+15.5**2)
for y in range(32,64):
    for x in range(32,64):
        r=math.sqrt((x-cx)**2+(y-cy)**2)/maxr
        if r<0.3: col=lerp(purp_c,edge,r/0.3*0.3)
        elif r<0.65: col=lerp(purp_c,purp_m,(r-0.3)/0.35)
        else: col=lerp(purp_m,void,(r-0.65)/0.35)
        px[x,y]=col+(255,)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_shard_volley_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m10_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
