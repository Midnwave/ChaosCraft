"""Texture for infernal_crown_burst. Tarnished purple metal + blood/purple emissive."""
import base64, math
from PIL import Image
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

obs=hx("#100818"); aged=hx("#2a1840"); pol=hx("#604890"); bright=hx("#b090d0"); spec=hx("#e8d8ff"); emis=hx("#c080ff")
blood=hx("#ff2020"); pur=hx("#8020a0"); void=hx("#180810")

# Zone A (TL): tarnished metal — bright top, dark base
for y in range(32):
    for x in range(32):
        t=y/31.0
        if t<0.15: col=lerp(spec,bright,t/0.15)
        elif t<0.4: col=lerp(bright,pol,(t-0.15)/0.25)
        else: col=lerp(pol,obs,(t-0.4)/0.6)
        # specular cluster
        if abs(x-6)<=1 and abs(y-3)<=1: col=spec
        # left-edge emissive seam
        if x<2 and 8<y<24: col=lerp(col,emis,0.5)
        px[x,y]=col+(255,)
# Zone B (TR): metal shadow side
for y in range(32):
    for x in range(32,64):
        t=y/31.0
        col=lerp(aged,obs,t)
        if (x+y)%6==0: col=lerp(col,pol,0.3)
        px[x,y]=col+(255,)
# Zone C (BL): top highlight
for y in range(32,64):
    for x in range(32):
        t=(y-32)/31.0
        col=lerp(spec,bright,t*0.6)
        px[x,y]=col+(255,)
# Zone D (BR): blood-red+purple radial
cx,cy=47.5,47.5; maxr=math.sqrt(15.5**2+15.5**2)
for y in range(32,64):
    for x in range(32,64):
        r=math.sqrt((x-cx)**2+(y-cy)**2)/maxr
        if r<0.20: col=lerp(blood,hx("#ff4040"),r/0.20)
        elif r<0.50: col=lerp(blood,pur,(r-0.20)/0.30)
        elif r<0.80: col=lerp(pur,void,(r-0.50)/0.30)
        else: col=void
        if abs(r-0.30)<0.04: col=lerp(col,emis,0.5)
        px[x,y]=col+(255,)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/infernal_crown_burst_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m08_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
