"""Texture for fallen_feather_lance. Bone-white feather vane + purple corruption."""
import base64, math
from PIL import Image
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

bone=hx("#e8e4dc"); grey=hx("#a8a098"); shadow=hx("#303028"); spec=hx("#ffffff")
nblk=hx("#030308"); purp=hx("#8040c0")

# Zone A: feather vane primary lit
for y in range(32):
    for x in range(32):
        t=y/31.0
        col=lerp(spec,grey,t)
        # diagonal grain at 15° (every ~4 rows)
        if int(y - x*0.27) % 4 == 0: col=lerp(col,bone,0.5)
        # darker stripe every 8
        if y%8==0: col=lerp(col,shadow,0.3)
        if y==31: col=shadow
        px[x,y]=col+(255,)
# Zone B: feather shadow side
for y in range(32):
    for x in range(32,64):
        t=y/31.0
        col=lerp(grey,shadow,t)
        if (x-32+y)%5==0: col=lerp(col,bone,0.4)
        px[x,y]=col+(255,)
# Zone C: brightest highlight
for y in range(32,64):
    for x in range(32):
        t=(y-32)/31.0
        col=lerp(spec,bone,t)
        px[x,y]=col+(255,)
# Zone D: void black with purple corruption veins
cx,cy=47.5,47.5; maxr=math.sqrt(15.5**2+15.5**2)
for y in range(32,64):
    for x in range(32,64):
        col=nblk
        # purple corruption rivulets from top
        if 36<=x<=58 and (x%6==0 or x%7==0):
            col=lerp(col,purp,0.7)
        # radial purple
        r=math.sqrt((x-cx)**2+(y-cy)**2)/maxr
        if r<0.4: col=lerp(col,purp,0.5*(1-r/0.4))
        px[x,y]=col+(255,)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/fallen_feather_lance_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m09_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
