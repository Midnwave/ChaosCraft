"""Texture 0 for fallen_feather_lance: Bone-white feather vane with diagonal grain, barb separation lines."""
import base64, math
from PIL import Image
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

bone=hx("#e8e4dc"); grey=hx("#a8a098"); shadow=hx("#303028"); spec=hx("#ffffff")

# Zone A (0,0)-(31,31): feather vane primary lit side
for y in range(32):
    for x in range(32):
        t=y/31.0
        col=lerp(spec,grey,t*0.8)
        # diagonal grain at ~15° — runs across the vane
        grain=int(y - x*0.268) % 5
        if grain==0: col=lerp(col,bone,0.55)
        elif grain==1: col=lerp(col,spec,0.2)
        # barb separation lines every 8 rows
        if y%8==0: col=lerp(col,shadow,0.35)
        # top edge pure white
        if y==0: col=spec
        # rachis centre darker
        if 14<=x<=17: col=lerp(col,shadow,0.5)
        px[x,y]=col+(255,)

# Zone B (32,0)-(63,31): feather shadow/underside
for y in range(32):
    for x in range(32,64):
        t=y/31.0
        col=lerp(grey,shadow,t*0.85)
        # softer grain opposite angle
        grain=int((x-32)+y*0.268) % 6
        if grain==0: col=lerp(col,bone,0.4)
        if y%8==0: col=lerp(col,shadow,0.25)
        if y==0: col=lerp(col,spec,0.5)
        px[x,y]=col+(255,)

# Zone C (0,32)-(31,63): brightest barb highlight
for y in range(32,64):
    for x in range(32):
        t=(y-32)/31.0
        col=lerp(spec,bone,t)
        # fine cross-fibre texture
        if (x+y)%3==0: col=lerp(col,grey,0.2)
        if y%8==32: col=lerp(col,shadow,0.2)
        px[x,y]=col+(255,)

# Zone D (32,32)-(63,63): quill shadow + deep corruption
for y in range(32,64):
    for x in range(32,64):
        t=(y-32)/31.0
        col=lerp(shadow,hx("#181810"),t)
        # subtle bone striation
        if (x-32)%7==0: col=lerp(col,bone,0.15)
        if y%9==0: col=lerp(col,grey,0.1)
        px[x,y]=col+(255,)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/fallen_feather_lance_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m09_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
