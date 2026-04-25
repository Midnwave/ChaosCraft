"""Texture for silver_wing_blade. Cold silver-blue feather + razor edges."""
import base64,math
from PIL import Image
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))
silv=hx("#d8dce8"); mid=hx("#8090a8"); shadow=hx("#202830"); spec=hx("#ffffff"); emis=hx("#c8d8f8")
nb=hx("#030308"); razor=hx("#c8e0f8")
for y in range(32):
    for x in range(32):
        col=lerp(spec,silv,y/31.0)
        if int(y - x*0.36) % 6 == 0: col=lerp(col,mid,0.4)
        if y%6==0: col=lerp(col,shadow,0.2)
        px[x,y]=col+(255,)
for y in range(32):
    for x in range(32,64):
        col=lerp(silv,mid,y/31.0)
        if (x-32+y)%5==0: col=lerp(col,shadow,0.3)
        px[x,y]=col+(255,)
for y in range(32,64):
    for x in range(32):
        col=lerp(spec,emis,(y-32)/31.0)
        px[x,y]=col+(255,)
for y in range(32,64):
    for x in range(32,64):
        col=nb
        if 47<=x<=49 or 46<=x<=48:
            col=razor
        px[x,y]=col+(255,)
out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/silver_wing_blade_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m12_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
