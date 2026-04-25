"""Texture for nightmare_clock. Bone face + obsidian-red blade hands."""
import base64,math
from PIL import Image
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))
bone=hx("#e8e4dc"); ivory=hx("#c0b8a8"); shadow=hx("#282018"); spec=hx("#ffffff"); purp=hx("#8040c0")
obs=hx("#06040a"); blood=hx("#c01010"); blood_e=hx("#e02020")
for y in range(32):
    for x in range(32):
        col=lerp(spec,bone,y/31.0)
        # ghost numeral rings at 40% and 80% radius
        cx,cy=15,15
        r=math.sqrt((x-cx)**2+(y-cy)**2)/15.0
        if abs(r-0.4)<0.04: col=lerp(col,ivory,0.5)
        if abs(r-0.8)<0.04: col=lerp(col,ivory,0.4)
        if y>26 and y%3==0: col=lerp(col,purp,0.3)
        if abs(x-7)<=1 and abs(y-3)<=1: col=spec
        px[x,y]=col+(255,)
for y in range(32):
    for x in range(32,64):
        col=lerp(ivory,shadow,y/31.0)
        if (x-32+y)%6==0: col=lerp(col,bone,0.3)
        px[x,y]=col+(255,)
for y in range(32,64):
    for x in range(32):
        col=lerp(spec,bone,(y-32)/31.0)
        px[x,y]=col+(255,)
# Zone D: clock hands — obsidian black with red edges
for y in range(32,64):
    for x in range(32,64):
        col=obs
        if x in [33,62] or y in [33,62]: col=blood_e
        elif x in [34,61] or y in [34,61]: col=blood
        px[x,y]=col+(255,)
out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_clock_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m21_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
