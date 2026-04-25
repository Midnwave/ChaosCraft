"""Texture for nightmare_eye_projectile. Sclera + iris radial."""
import base64,math
from PIL import Image
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))
sclera=hx("#f0ece0"); vessel=hx("#d04050"); grey_blue=hx("#808898"); spec=hx("#ffffff")
iris_dark=hx("#100830"); iris_b=hx("#8050e0"); amber=hx("#c0a020"); emis=hx("#d080ff")
for y in range(32):
    for x in range(32):
        col=lerp(spec,sclera,y/31.0)
        # blood vessel lines (1px horizontal at intervals)
        if y in [8,15,22,28]: col=vessel
        if x>=24: col=lerp(col,grey_blue,0.3)
        if y==31: col=lerp(col,vessel,0.4)
        if abs(x-32)<=1 and abs(y-5)<=1: col=spec
        px[x,y]=col+(255,)
for y in range(32):
    for x in range(32,64):
        col=lerp(sclera,grey_blue,y/31.0)
        if y in [10,20]: col=vessel
        px[x,y]=col+(255,)
for y in range(32,64):
    for x in range(32):
        col=lerp(spec,sclera,(y-32)/31.0)
        px[x,y]=col+(255,)
cx,cy=47.5,47.5; maxr=math.sqrt(15.5**2+15.5**2)
for y in range(32,64):
    for x in range(32,64):
        r=math.sqrt((x-cx)**2+(y-cy)**2)/maxr
        if r<0.15: col=lerp(emis,iris_dark,r/0.15)
        elif r<0.35: col=lerp(iris_dark,iris_b,(r-0.15)/0.20)
        elif r<0.55: col=lerp(iris_b,amber,(r-0.35)/0.20)
        elif r<0.80: col=lerp(amber,iris_dark,(r-0.55)/0.25)
        else: col=iris_dark
        if abs(r-0.40)<0.04: col=emis
        px[x,y]=col+(255,)
out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_eye_projectile_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m13_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
