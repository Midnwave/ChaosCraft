"""Texture for nightmare_planetarium. 4-zone wrong planets palette."""
import base64,math
from PIL import Image
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))
# 4 horizontal zones for 4 planets
b1=hx("#1a0404"); b2=hx("#c01010")  # blood
n1=hx("#d8d0c0"); n2=hx("#f8f0e8")  # bone
y1=hx("#0c0e08"); y2=hx("#d4c820")  # yellow
p1=hx("#060410"); p2=hx("#c080ff")  # purple
spec=hx("#ffffff")
# Top half: 4 horizontal planet zones (each 8 rows tall)
for y in range(32):
    for x in range(32):
        if y<8: col=lerp(b2,b1,y/7.0)
        elif y<16: col=lerp(n2,n1,(y-8)/7.0)
        elif y<24: col=lerp(y2,y1,(y-16)/7.0)
        else: col=lerp(p2,p1,(y-24)/7.0)
        if abs(x-7)<=1 and abs(y-3)<=1: col=spec
        px[x,y]=col+(255,)
# Top-right: shadow versions
for y in range(32):
    for x in range(32,64):
        if y<8: col=b1
        elif y<16: col=lerp(n1,b1,0.5)
        elif y<24: col=y1
        else: col=p1
        px[x,y]=col+(255,)
# Bottom-left: highlight zone
for y in range(32,64):
    for x in range(32):
        if y<40: col=lerp(spec,n2,(y-32)/7.0)
        elif y<48: col=lerp(n2,y2,(y-40)/7.0)
        else: col=lerp(y2,b2,(y-48)/15.0)
        px[x,y]=col+(255,)
# Bottom-right: emissive purple radial
cx,cy=47.5,47.5; maxr=math.sqrt(15.5**2+15.5**2)
for y in range(32,64):
    for x in range(32,64):
        r=math.sqrt((x-cx)**2+(y-cy)**2)/maxr
        col=lerp(p2,p1,r)
        px[x,y]=col+(255,)
out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_planetarium_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m23_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
