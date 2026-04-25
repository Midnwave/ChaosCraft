"""Texture for devils_spine_array. 4-zone 64x64. Ancient weathered bone with purple contamination."""
import base64, math
from PIL import Image
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c):
    c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t):
    return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))
shadow=hx("#1a1810"); ivory=hx("#8a8070"); high=hx("#d8d0c0"); spec=hx("#f8f0e8"); emis=hx("#b090ff")
purp_dark=hx("#0a0418"); purp_mid=hx("#6030b0"); purp_bright=hx("#c060e0"); purp_emis=hx("#e080ff")

# Zone A (TL): primary lit bone — bright top, darker bottom
for y in range(32):
    for x in range(32):
        t=y/31.0
        col=lerp(high,shadow,t*0.85)
        # sediment banding every 12 rows
        if y%12==0:
            col=lerp(col,ivory,0.3)
        # specular cluster (8,4)
        if abs(x-8)<=2 and abs(y-4)<=1:
            col=spec
        px[x,y]=col+(255,)
# Zone B (TR): bone shadow — darker
for y in range(32):
    for x in range(32,64):
        t=y/31.0
        col=lerp(ivory,shadow,t*0.95)
        if y%12==0:
            col=lerp(col,hx("#403828"),0.5)
        # purple contamination veins in deepest crevices
        if (y>20) and (x%6==2):
            col=lerp(col,emis,0.35)
        px[x,y]=col+(255,)
# Zone C (BL): highlight zone — brightest bone
for y in range(32,64):
    for x in range(32):
        t=(y-32)/31.0
        col=lerp(spec,high,t)
        if (y%5)==0:
            col=lerp(col,ivory,0.25)
        px[x,y]=col+(255,)
# Zone D (BR): purple emissive radial — discs and nerves
cx,cy=47.5,47.5; maxr=math.sqrt(15.5**2+15.5**2)
for y in range(32,64):
    for x in range(32,64):
        r=math.sqrt((x-cx)**2+(y-cy)**2)/maxr
        if r<0.2:
            col=lerp(purp_emis,purp_bright,r/0.2)
        elif r<0.45:
            col=lerp(purp_bright,purp_mid,(r-0.2)/0.25)
        elif r<0.75:
            col=lerp(purp_mid,purp_dark,(r-0.45)/0.30)
        else:
            col=purp_dark
        # ring at 30%
        if abs(r-0.30)<0.04:
            col=lerp(col,purp_emis,0.7)
        px[x,y]=col+(255,)
out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/devils_spine_array_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m03_b64.txt","w").write(b64)
import os
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
