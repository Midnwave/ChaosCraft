"""Texture for devils_constellation. Star points + red lines."""
import base64,math
from PIL import Image
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))
nb=hx("#030308"); gold=hx("#ffe040"); blood_b=hx("#e02020"); blood_d=hx("#601010"); spec=hx("#ffffff"); red=hx("#d01010"); rede=hx("#ff2020")
cx,cy=15,15
for y in range(32):
    for x in range(32):
        r=math.sqrt((x-cx)**2+(y-cy)**2)/15.0
        if r<0.15: col=lerp(spec,gold,r/0.15)
        elif r<0.4: col=lerp(gold,blood_b,(r-0.15)/0.25)
        elif r<0.7: col=lerp(blood_b,blood_d,(r-0.4)/0.3)
        else: col=lerp(blood_d,nb,(r-0.7)/0.3)
        px[x,y]=col+(255,)
for y in range(32):
    for x in range(32,64):
        col=nb
        if (x-32+y)%4==0: col=lerp(col,blood_d,0.5)
        px[x,y]=col+(255,)
for y in range(32,64):
    for x in range(32):
        col=nb
        # red strip in horizontal center of zone C (around y=47-48)
        if 47<=y<=48:
            col=red
        elif 46<=y<=49:
            col=lerp(col,rede,0.5)
        px[x,y]=col+(255,)
for y in range(32,64):
    for x in range(32,64):
        col=nb
        if 47<=y<=48:
            col=red
        px[x,y]=col+(255,)
out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/devils_constellation_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m20_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
