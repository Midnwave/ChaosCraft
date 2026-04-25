"""Texture for nightmare_cathedral. Gothic stone + purple stained glass."""
import base64,math
from PIL import Image
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))
lime=hx("#b0a898"); shadow=hx("#2a2820"); carve=hx("#707060"); peak=hx("#d8d0c0"); purp=hx("#9060d0")
gemis=hx("#e060ff"); gmid=hx("#6020b0"); gedge=hx("#100820")
for y in range(32):
    for x in range(32):
        col=lerp(peak,lime,y/31.0)
        if y%10==0: col=lerp(col,shadow,0.4)
        if y%6==2: col=lerp(col,carve,0.3)
        if abs(x-6)<=1 and abs(y-3)<=1: col=peak
        if y==31: col=shadow
        px[x,y]=col+(255,)
for y in range(32):
    for x in range(32,64):
        col=lerp(lime,shadow,y/31.0)
        if y%10==0: col=lerp(col,carve,0.4)
        if (x-32+y)%5==0: col=lerp(col,purp,0.3)
        px[x,y]=col+(255,)
for y in range(32,64):
    for x in range(32):
        col=lerp(peak,lime,(y-32)/31.0)
        px[x,y]=col+(255,)
cx,cy=47.5,47.5; maxr=math.sqrt(15.5**2+15.5**2)
for y in range(32,64):
    for x in range(32,64):
        r=math.sqrt((x-cx)**2+(y-cy)**2)/maxr
        if r<0.3: col=gemis
        elif r<0.7: col=lerp(gemis,gmid,(r-0.3)/0.4)
        else: col=lerp(gmid,gedge,(r-0.7)/0.3)
        px[x,y]=col+(255,)
out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_cathedral_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m16_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
