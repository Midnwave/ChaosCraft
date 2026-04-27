import base64, math
from PIL import Image
W=H=64; img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()

cx=W//2; cy=H//2
# near-black with blood-red radial glow, gold ring at 60%
max_r = math.sqrt(cx*cx+cy*cy)

for y in range(H):
    for x in range(W):
        dx=x-cx; dy=y-cy
        dist=math.sqrt(dx*dx+dy*dy)/max_r
        t=min(dist,1.0)

        if t < 0.08:
            # emissive centre #e04010
            t2=t/0.08
            r=int(224+(255-224)*(1-t2))
            g=int(64*(1-t2))
            b=int(16*(1-t2))
        elif t < 0.25:
            # #e04010 to #c01010
            t2=(t-0.08)/0.17
            r=int(224+(192-224)*t2)
            g=int(64*(1-t2))
            b=int(16*(1-t2))
        elif t < 0.50:
            # #c01010 to #501010
            t2=(t-0.25)/0.25
            r=int(192+(80-192)*t2)
            g=int(16*(1-t2))
            b=int(16*(1-t2))
        elif t < 0.80:
            # #501010 to void #060208
            t2=(t-0.50)/0.30
            r=int(80+(6-80)*t2)
            g=int(16+(2-16)*t2)
            b=int(16+(8-16)*t2)
        else:
            r=int(6*(1-(t-0.80)/0.20))
            g=int(2*(1-(t-0.80)/0.20))
            b=int(8*(1-(t-0.80)/0.20))

        px[x,y]=(max(0,min(255,r)),max(0,min(255,g)),max(0,min(255,b)),255)

# Gold ring detail at 60% radius = 0.60 * max_r
ring_r = max_r * 0.60
gold = (128, 96, 16)
for y in range(H):
    for x in range(W):
        dx=x-cx; dy=y-cy
        dist=math.sqrt(dx*dx+dy*dy)
        if abs(dist-ring_r) < 1.5:
            t=1.0-abs(dist-ring_r)/1.5
            r2,g2,b2=px[x,y][:3]
            r=min(255,int(r2+(gold[0]-r2)*t))
            g=min(255,int(g2+(gold[1]-g2)*t))
            b=min(255,int(b2+(gold[2]-b2)*t))
            px[x,y]=(r,g,b,255)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/devils_sermon_nova_tex1.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m06_tex1_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
