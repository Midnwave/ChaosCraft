"""Texture for blood_comet. Clotted blood + burning tail."""
import base64,math
from PIL import Image
import random
random.seed(14014)
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))
dried=hx("#1a0404"); clot=hx("#300808"); wet=hx("#700e0e"); fresh=hx("#c01010"); spec=hx("#ff2020"); emis=hx("#e02020")
red_c=hx("#ff2010"); ymid=hx("#c08010"); ylow=hx("#402008"); nb=hx("#080202")
for y in range(32):
    for x in range(32):
        col=lerp(spec,wet,y/31.0)
        if random.random()<0.4: col=lerp(col,clot,0.3+random.random()*0.4)
        if random.random()<0.15: col=fresh
        if y==0: col=spec
        if y==31: col=dried
        px[x,y]=col+(255,)
for y in range(32):
    for x in range(32,64):
        col=lerp(wet,dried,y/31.0)
        if random.random()<0.3: col=lerp(col,clot,0.5)
        px[x,y]=col+(255,)
for y in range(32,64):
    for x in range(32):
        col=lerp(emis,fresh,(y-32)/31.0)
        px[x,y]=col+(255,)
cx,cy=47.5,47.5; maxr=math.sqrt(15.5**2+15.5**2)
for y in range(32,64):
    for x in range(32,64):
        r=math.sqrt((x-cx)**2+(y-cy)**2)/maxr
        if r<0.3: col=lerp(red_c,fresh,r/0.3*0.4)
        elif r<0.65: col=lerp(red_c,ymid,(r-0.3)/0.35)
        elif r<0.85: col=lerp(ymid,ylow,(r-0.65)/0.20)
        else: col=lerp(ylow,nb,(r-0.85)/0.15)
        px[x,y]=col+(255,)
out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/blood_comet_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m14_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
