"""Texture for devils_tongue_beam. Blood-red + sickly yellow tongue."""
import base64, math
from PIL import Image
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

base=hx("#1a0404"); crim=hx("#601008"); blood=hx("#c02010"); yel=hx("#d4c010"); yel_t=hx("#e0d020")
nb=hx("#080202"); blood_c=hx("#ff1010"); blood_m=hx("#901010")

# Zone A: tongue surface — yellow centre strip, blood gradient
for y in range(32):
    for x in range(32):
        cy_dist=abs(y-15)/15.0
        if cy_dist<0.15: col=lerp(yel_t,yel,cy_dist/0.15)
        elif cy_dist<0.5: col=lerp(yel,blood,(cy_dist-0.15)/0.35)
        else: col=lerp(blood,crim,(cy_dist-0.5)/0.5)
        if y==0: col=yel_t
        if y==31: col=nb
        px[x,y]=col+(255,)
# Zone B: shadow side — darker blood
for y in range(32):
    for x in range(32,64):
        col=lerp(crim,base,y/31.0)
        px[x,y]=col+(255,)
# Zone C: brightest highlight — yellow zone
for y in range(32,64):
    for x in range(32):
        col=lerp(yel_t,yel,(y-32)/31.0)
        px[x,y]=col+(255,)
# Zone D: blood-red radial
cx,cy=47.5,47.5; maxr=math.sqrt(15.5**2+15.5**2)
for y in range(32,64):
    for x in range(32,64):
        r=math.sqrt((x-cx)**2+(y-cy)**2)/maxr
        if r<0.3: col=lerp(blood_c,blood,r/0.3*0.5)
        elif r<0.7: col=lerp(blood,blood_m,(r-0.3)/0.4)
        else: col=lerp(blood_m,base,(r-0.7)/0.3)
        px[x,y]=col+(255,)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/devils_tongue_beam_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m11_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
