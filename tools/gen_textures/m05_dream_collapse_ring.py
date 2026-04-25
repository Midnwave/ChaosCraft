"""Texture for dream_collapse_ring. Shattered glass + compressed purple radial."""
import base64, math, random
from PIL import Image
random.seed(5005)
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

base=hx("#0a0418"); violet=hx("#3018a0"); bright=hx("#7040e0"); silver=hx("#d0c8f0"); emis=hx("#c060ff"); spec=hx("#ffffff")
purp_emis=hx("#c060ff"); purp_mid=hx("#5020b0"); purp_outer=hx("#1a0850"); void=hx("#060210")

# Zone A (TL): shattered glass primary — dark base with hard-edge crack lines
for y in range(32):
    for x in range(32):
        col=lerp(violet,base,y/31.0)
        # diagonal crack lines at 15°, 45°, 75°
        # 45° line: y == x
        if abs(y-x)<=0 and (x+y)%4<2:
            col=silver
        # 15° line: y ≈ x*0.27
        elif abs(y - x*0.27 - 8) < 0.6:
            col=bright
        # 75° line: y ≈ x*3.7
        elif abs(y - x*3.7 + 5) < 0.6:
            col=bright
        # specular cluster (8,6)
        if abs(x-8)<=1 and abs(y-6)<=1:
            col=spec
        px[x,y]=col+(255,)
# Zone B (TR): glass shadow — darker version
for y in range(32):
    for x in range(32,64):
        lx=x-32
        col=lerp(base,hx("#020108"),y/31.0)
        # subtle crack lines
        if abs(y-lx)<=0:
            col=lerp(col,violet,0.5)
        if (y+lx)%9==0:
            col=lerp(col,bright,0.4)
        px[x,y]=col+(255,)
# Zone C (BL): silver edge highlight zone — bright reflective
for y in range(32,64):
    for x in range(32):
        t=(y-32)/31.0
        col=lerp(silver,bright,t*0.7)
        if (x+y)%5==0:
            col=lerp(col,spec,0.3)
        px[x,y]=col+(255,)
# Zone D (BR): compressed purple radial
cx,cy=47.5,47.5; maxr=math.sqrt(15.5**2+15.5**2)
for y in range(32,64):
    for x in range(32,64):
        r=math.sqrt((x-cx)**2+(y-cy)**2)/maxr
        if r<0.20: col=lerp(purp_emis,bright,r/0.20)
        elif r<0.50: col=lerp(purp_emis,purp_mid,(r-0.20)/0.30)
        elif r<0.80: col=lerp(purp_mid,purp_outer,(r-0.50)/0.30)
        else: col=lerp(purp_outer,void,min(1.0,(r-0.80)/0.20))
        if abs(r-0.30)<0.04: col=lerp(col,bright,0.6)
        px[x,y]=col+(255,)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/dream_collapse_ring_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m05_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
