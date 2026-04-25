"""Texture for fallen_halo_burst. 4-zone 64x64. Sacred bone-white + divine corruption radial."""
import base64, math
from PIL import Image
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

white=hx("#f0ece0"); shadow=hx("#b8b0a0"); edge=hx("#404038"); spec=hx("#ffffff"); emis=hx("#fffff0")
divine=hx("#fffff0"); yel=hx("#d0c840"); purp=hx("#6020a0"); blk=hx("#080410")

# Zone A (TL): sacred bone — bright base
for y in range(32):
    for x in range(32):
        col=lerp(spec,white,y/31.0)
        # concentric ring details at 30% (x=10) and 70% (x=22) of width
        if abs(x-10)<=1 or abs(x-22)<=1:
            col=lerp(col,shadow,0.3)
        if y==31:  # bottom edge dark
            col=edge
        # specular cluster
        if abs(x-10)<=1 and abs(y-4)<=1:
            col=spec
        px[x,y]=col+(255,)
# Zone B (TR): shadow side — darker
for y in range(32):
    for x in range(32,64):
        t=y/31.0
        col=lerp(white,shadow,0.5+t*0.4)
        if y==31:
            col=edge
        px[x,y]=col+(255,)
# Zone C (BL): highlight — emissive white
for y in range(32,64):
    for x in range(32):
        t=(y-32)/31.0
        col=lerp(emis,white,t*0.3)
        px[x,y]=col+(255,)
# Zone D (BR): divine corruption radial — white→yellow→purple→black
cx,cy=47.5,47.5; maxr=math.sqrt(15.5**2+15.5**2)
for y in range(32,64):
    for x in range(32,64):
        r=math.sqrt((x-cx)**2+(y-cy)**2)/maxr
        if r<0.15: col=lerp(divine,white,r/0.15)
        elif r<0.35: col=lerp(divine,yel,(r-0.15)/0.20)
        elif r<0.65: col=lerp(yel,purp,(r-0.35)/0.30)
        elif r<0.90: col=lerp(purp,blk,(r-0.65)/0.25)
        else: col=blk
        if abs(r-0.30)<0.04: col=lerp(col,divine,0.5)
        px[x,y]=col+(255,)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/fallen_halo_burst_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m04_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
