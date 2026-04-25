"""Texture for infernal_scripture_array. Manuscript text + dark wood lectern."""
import base64,math
from PIL import Image
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))
parch=hx("#c8b890"); aged=hx("#906840"); ink=hx("#181210"); spec=hx("#ffffff"); purp=hx("#9060c0")
wood=hx("#200c08"); wood_d=hx("#100604"); blood=hx("#601010")
for y in range(32):
    for x in range(32):
        col=parch
        # text lines every 4 rows
        if y%4==0: col=ink
        elif y%4==1 and (x%6<3): col=lerp(parch,ink,0.6)
        # purple ink emissive between text
        if y%4==2 and (x%5==0): col=lerp(col,purp,0.7)
        if abs(x-7)<=1 and abs(y-3)<=1: col=spec
        if y%6==3: col=lerp(col,aged,0.3)
        px[x,y]=col+(255,)
for y in range(32):
    for x in range(32,64):
        col=lerp(parch,aged,y/31.0)
        if y%4==0: col=lerp(col,ink,0.5)
        px[x,y]=col+(255,)
for y in range(32,64):
    for x in range(32):
        col=lerp(spec,parch,(y-32)/31.0)
        px[x,y]=col+(255,)
for y in range(32,64):
    for x in range(32,64):
        col=wood
        if (x-32)%4==0: col=wood_d
        if y%6==2: col=lerp(col,blood,0.4)
        px[x,y]=col+(255,)
out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/infernal_scripture_array_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m24_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
