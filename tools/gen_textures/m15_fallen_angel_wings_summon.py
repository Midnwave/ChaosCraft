"""Texture for fallen_angel_wings_summon. White→sickly→purple gradient."""
import base64,math
from PIL import Image
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))
bone=hx("#e8e4dc"); yel=hx("#7a9010"); purp=hx("#3010a0"); spec=hx("#ffffff"); shadow=hx("#202020")
nb=hx("#030308"); pcorr=hx("#8030c0")
for y in range(32):
    for x in range(32):
        # Horizontal gradient: bone (left) → yellow (mid) → purple (right)
        if x<11: col=bone
        elif x<22: col=lerp(bone,yel,(x-11)/11.0)
        else: col=lerp(yel,purp,(x-22)/10.0)
        col=lerp(col,shadow,y/31.0*0.4)
        if y%6==0: col=lerp(col,shadow,0.3)
        if abs(x-5)<=1 and abs(y-3)<=1: col=spec
        px[x,y]=col+(255,)
for y in range(32):
    for x in range(32,64):
        lx=x-32
        if lx<11: col=lerp(bone,shadow,0.3)
        elif lx<22: col=lerp(yel,shadow,0.3)
        else: col=lerp(purp,shadow,0.3)
        col=lerp(col,nb,y/31.0*0.5)
        px[x,y]=col+(255,)
for y in range(32,64):
    for x in range(32):
        col=lerp(spec,bone,(y-32)/31.0)
        px[x,y]=col+(255,)
for y in range(32,64):
    for x in range(32,64):
        col=nb
        if (x in [38,46,52]) and y>40:
            col=lerp(col,pcorr,0.7)
        px[x,y]=col+(255,)
out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/fallen_angel_wings_summon_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m15_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
