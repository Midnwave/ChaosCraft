"""Texture for devils_halo_array. Tarnished bleeding halos."""
import base64,math
from PIL import Image
import random
random.seed(17017)
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))
silv=hx("#e0dcd0"); stain=hx("#707060"); crack=hx("#181816"); shadow=hx("#101010"); blood=hx("#801818")
purp=hx("#200840"); silvr=hx("#706080")
for y in range(32):
    for x in range(32):
        col=lerp(silv,stain,y/31.0)
        if random.random()<0.06: col=stain
        # diagonal crack lines
        if abs(y-x*0.5) <= 0.5: col=crack
        # blood seep at cracks
        if abs(y-x*0.5) <= 0.7 and random.random()<0.3: col=blood
        if y==31: col=shadow
        px[x,y]=col+(255,)
for y in range(32):
    for x in range(32,64):
        col=lerp(stain,shadow,y/31.0)
        if random.random()<0.05: col=crack
        px[x,y]=col+(255,)
for y in range(32,64):
    for x in range(32):
        col=lerp(silv,stain,(y-32)/31.0*0.5)
        px[x,y]=col+(255,)
cx,cy=47.5,47.5; maxr=math.sqrt(15.5**2+15.5**2)
for y in range(32,64):
    for x in range(32,64):
        r=math.sqrt((x-cx)**2+(y-cy)**2)/maxr
        if r<0.5: col=lerp(purp,shadow,r/0.5)
        elif r<0.7: col=silvr
        else: col=lerp(silvr,shadow,(r-0.7)/0.3)
        px[x,y]=col+(255,)
out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/devils_halo_array_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m17_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
