"""TEX1 for devils_spine_array: deep purple-black disc/nerve glow."""
import base64, math
from PIL import Image
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

dark=hx("#0a0418"); darkb=hx("#100824"); bright=hx("#c060e0"); mid=hx("#6030b0"); emis=hx("#e080ff")

# Zone A (TL): mostly dark
for y in range(32):
    for x in range(32):
        px[x,y]=dark+(255,)

# Zone B (TR): slightly less dark with gradient
for y in range(32):
    for x in range(32,64):
        t=y/31.0
        col=lerp(dark,darkb,t)
        px[x,y]=col+(255,)

# Zone C (BL): radial glow from (16,48) — the disc emissive
cx,cy=16.0,48.0; maxr=math.sqrt(16.0**2+16.0**2)
for y in range(32,64):
    for x in range(32):
        r=math.sqrt((x-cx)**2+(y-cy)**2)/maxr
        if r<0.18:
            col=lerp(emis,bright,r/0.18)
        elif r<0.40:
            col=lerp(bright,mid,(r-0.18)/0.22)
        elif r<0.70:
            col=lerp(mid,dark,(r-0.40)/0.30)
        else:
            col=dark
        # bright ring at 20%
        if abs(r-0.20)<0.03:
            col=lerp(col,emis,0.8)
        px[x,y]=col+(255,)

# Zone D (BR): emissive center
cx2,cy2=47.5,47.5; maxr2=math.sqrt(15.5**2+15.5**2)
for y in range(32,64):
    for x in range(32,64):
        r=math.sqrt((x-cx2)**2+(y-cy2)**2)/maxr2
        if r<0.15:
            col=emis
        elif r<0.30:
            col=lerp(emis,bright,(r-0.15)/0.15)
        elif r<0.55:
            col=lerp(bright,mid,(r-0.30)/0.25)
        elif r<0.80:
            col=lerp(mid,dark,(r-0.55)/0.25)
        else:
            col=dark
        px[x,y]=col+(255,)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/devils_spine_array_tex1.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m03_tex1_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
