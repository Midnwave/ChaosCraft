"""Texture for fallen_seraph_skeleton. Pure divine bone — clean and white."""
import base64,math
from PIL import Image
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))
white=hx("#f0ece0"); grey=hx("#c0b8a8"); deep=hx("#484038"); spec=hx("#ffffff"); emis=hx("#c0a0ff")
nb=hx("#080604"); bonel=hx("#e8e0d0")
for y in range(32):
    for x in range(32):
        col=lerp(spec,white,y/31.0)
        if y%8==4: col=lerp(col,grey,0.3)
        if abs(x-7)<=1 and abs(y-3)<=1: col=spec
        if y>26 and (x in [4,12,20,28]): col=lerp(col,emis,0.4)
        px[x,y]=col+(255,)
for y in range(32):
    for x in range(32,64):
        col=lerp(white,grey,y/31.0)
        if y%6==0: col=lerp(col,deep,0.4)
        px[x,y]=col+(255,)
for y in range(32,64):
    for x in range(32):
        col=lerp(spec,white,(y-32)/31.0)
        px[x,y]=col+(255,)
for y in range(32,64):
    for x in range(32,64):
        col=nb
        cx,cy=47.5,47.5
        r=math.sqrt((x-cx)**2+(y-cy)**2)/15.5
        # bone-white cracks branching
        if abs(x-47)<=0.5 or abs(y-47)<=0.5: col=bonel
        if r<0.3 and ((x-47+y-47)%4==0): col=lerp(col,bonel,0.7)
        px[x,y]=col+(255,)
out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/fallen_seraph_skeleton_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m22_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
