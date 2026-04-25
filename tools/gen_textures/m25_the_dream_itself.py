"""Texture for the_dream_itself. ALL-PALETTES — bone/blood/yellow/purple."""
import base64,math
from PIL import Image
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))
bone=hx("#f0ece0"); blood=hx("#a01010"); yel=hx("#a0a010"); void=hx("#0a0420"); purp=hx("#c060ff"); pmid=hx("#6020a0"); pe=hx("#200840")
# Top-left: bone-white
for y in range(32):
    for x in range(32):
        col=lerp(bone,hx("#c0b8a8"),y/31.0)
        if y%6==0: col=lerp(col,hx("#404038"),0.3)
        px[x,y]=col+(255,)
# Top-right: blood-red
for y in range(32):
    for x in range(32,64):
        col=lerp(blood,hx("#400404"),y/31.0)
        px[x,y]=col+(255,)
# Bottom-left: sickly yellow
for y in range(32,64):
    for x in range(32):
        col=lerp(yel,hx("#404008"),(y-32)/31.0)
        px[x,y]=col+(255,)
# Bottom-right: deep purple emissive radial
cx,cy=47.5,47.5; maxr=math.sqrt(15.5**2+15.5**2)
for y in range(32,64):
    for x in range(32,64):
        r=math.sqrt((x-cx)**2+(y-cy)**2)/maxr
        if r<0.3: col=lerp(purp,pmid,r/0.3)
        elif r<0.7: col=lerp(pmid,pe,(r-0.3)/0.4)
        else: col=lerp(pe,void,(r-0.7)/0.3)
        px[x,y]=col+(255,)
out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/the_dream_itself_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m25_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
