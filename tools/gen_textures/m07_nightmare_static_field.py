"""Texture for nightmare_static_field. TV static — random pixel scatter."""
import base64, random
from PIL import Image
random.seed(7007)
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))

base=hx("#606878"); v1=hx("#404850"); v2=hx("#808890"); v3=hx("#b0b8c0"); v4=hx("#d8e0e8")
nblk=hx("#101418"); nwhite=hx("#e8f0f8")
b2=hx("#101820"); v2_1=hx("#181e28"); v2_2=hx("#384050"); purp=hx("#8060c0")

# Zone A (TL): silver static
for y in range(32):
    for x in range(32):
        c=random.choice([base,v1,v2,v3,v4,base,base])
        if random.random()<0.04: c=nwhite if random.random()<0.5 else nblk
        px[x,y]=c+(255,)
# Zone B (TR): darker silver static
for y in range(32):
    for x in range(32,64):
        c=random.choice([v1,base,v2,nblk,base])
        if random.random()<0.05: c=v3
        px[x,y]=c+(255,)
# Zone C (BL): brightest static — light cluster
for y in range(32,64):
    for x in range(32):
        c=random.choice([v3,v4,nwhite,v2,v3,v3])
        if random.random()<0.04: c=nblk
        px[x,y]=c+(255,)
# Zone D (BR): purple-shifted static (emissive zone)
for y in range(32,64):
    for x in range(32,64):
        c=random.choice([b2,v2_1,v2_2,b2,purp])
        if random.random()<0.06: c=purp
        px[x,y]=c+(255,)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_static_field_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m07_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
