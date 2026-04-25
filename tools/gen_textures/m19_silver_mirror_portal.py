"""Texture for silver_mirror_portal. Tarnished silver baroque + mirror surface."""
import base64,math
from PIL import Image
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))
silv=hx("#1a1c28"); aged=hx("#484860"); pol=hx("#909098"); edge=hx("#c8c8d8"); spec=hx("#e8e8f0"); purp=hx("#9060d0")
void=hx("#04040c"); ghost=hx("#181820"); eye=hx("#9060d0")
for y in range(32):
    for x in range(32):
        col=lerp(edge,aged,y/31.0)
        if y%8==0: col=lerp(col,silv,0.4)
        if x<2: col=purp  # inner edge emissive
        if abs(x-7)<=1 and abs(y-3)<=1: col=spec
        px[x,y]=col+(255,)
for y in range(32):
    for x in range(32,64):
        col=lerp(aged,silv,y/31.0)
        if y%8==0: col=lerp(col,pol,0.3)
        px[x,y]=col+(255,)
for y in range(32,64):
    for x in range(32):
        col=lerp(spec,edge,(y-32)/31.0)
        px[x,y]=col+(255,)
# Zone D: WRONG PLACE — void with ghost shapes + eye
for y in range(32,64):
    for x in range(32,64):
        col=void
        # ghost arch shape
        if 38<=x<=58 and 35<=y<=55:
            col=ghost
        # eye looking back at (32+16, 32+20) = (48, 52)
        if abs(x-48)<=2 and abs(y-52)<=2:
            col=eye
        px[x,y]=col+(255,)
out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/silver_mirror_portal_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m19_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
