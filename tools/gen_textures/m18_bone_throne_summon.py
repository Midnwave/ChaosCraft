"""Texture for bone_throne_summon. Compacted bone + purple seep."""
import base64,math,random
from PIL import Image
random.seed(18018)
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))
pale=hx("#c8c0b0"); crack=hx("#282018"); mid=hx("#907870"); peak=hx("#e8e0d0"); spec=hx("#f8f0e8"); purp=hx("#8020c0")
nb=hx("#080604"); bonel=hx("#c0b8a8")
for y in range(32):
    for x in range(32):
        col=lerp(peak,pale,y/31.0)
        if random.random()<0.08: col=lerp(col,crack,0.5)
        if random.random()<0.05: col=mid
        if abs(x-7)<=1 and abs(y-3)<=1: col=spec
        if random.random()<0.04: col=lerp(col,purp,0.6)
        px[x,y]=col+(255,)
for y in range(32):
    for x in range(32,64):
        col=lerp(pale,crack,y/31.0)
        if random.random()<0.06: col=mid
        px[x,y]=col+(255,)
for y in range(32,64):
    for x in range(32):
        col=lerp(spec,peak,(y-32)/31.0)
        px[x,y]=col+(255,)
for y in range(32,64):
    for x in range(32,64):
        col=nb
        if (x in [38,46,54]) or (y in [40,50,58]):
            col=lerp(col,bonel,0.7)
        px[x,y]=col+(255,)
out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/bone_throne_summon_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m18_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
