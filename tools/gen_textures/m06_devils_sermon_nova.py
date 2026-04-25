"""Texture for devils_sermon_nova. Blood-red→gold infernal authority palette."""
import base64, math
from PIL import Image
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

base=hx("#1a0404"); crim=hx("#500808"); blood=hx("#a01010"); gold=hx("#d0a020"); emis=hx("#ffe040"); spec_g=hx("#ffd040")
nb=hx("#060208"); blood_c=hx("#c01010"); blood_m=hx("#501010"); gold_ring=hx("#806010"); emis_b=hx("#e04010")

# Zone A (TL): blood→gold lit. Top edge gold.
for y in range(32):
    for x in range(32):
        t=y/31.0
        if t<0.15: col=lerp(emis,gold,t/0.15)
        elif t<0.5: col=lerp(gold,blood,(t-0.15)/0.35)
        elif t<0.85: col=lerp(blood,crim,(t-0.5)/0.35)
        else: col=lerp(crim,base,(t-0.85)/0.15)
        # specular cluster (8,5)
        if abs(x-8)<=1 and abs(y-5)<=1: col=spec_g
        if y==31: col=lerp(col,hx("#080202"),0.5)
        px[x,y]=col+(255,)
# Zone B (TR): shadow side
for y in range(32):
    for x in range(32,64):
        t=y/31.0
        col=lerp(crim,base,t)
        if (x-32+y)%5==0: col=lerp(col,hx("#300404"),0.4)
        px[x,y]=col+(255,)
# Zone C (BL): top highlight zone — gold-bright
for y in range(32,64):
    for x in range(32):
        t=(y-32)/31.0
        col=lerp(emis,gold,t*0.6)
        if (x+y)%4==0: col=lerp(col,spec_g,0.4)
        px[x,y]=col+(255,)
# Zone D (BR): blood-red radial with gold ring
cx,cy=47.5,47.5; maxr=math.sqrt(15.5**2+15.5**2)
for y in range(32,64):
    for x in range(32,64):
        r=math.sqrt((x-cx)**2+(y-cy)**2)/maxr
        if r<0.30: col=lerp(blood_c,emis_b,r/0.30)
        elif r<0.65: col=lerp(blood_c,blood_m,(r-0.30)/0.35)
        else: col=lerp(blood_m,nb,min(1.0,(r-0.65)/0.35))
        if abs(r-0.55)<0.04: col=lerp(col,gold_ring,0.7)
        px[x,y]=col+(255,)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/devils_sermon_nova_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m06_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
