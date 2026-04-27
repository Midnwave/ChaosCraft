import base64, math
from PIL import Image
W=H=64; img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()

cx=W//2; cy=H//2
for y in range(H):
    for x in range(W):
        dx=x-cx; dy=y-cy
        dist=math.sqrt(dx*dx+dy*dy)
        max_r=math.sqrt(cx*cx+cy*cy)
        t=min(dist/max_r,1.0)
        # compressed purple radial: bright centre, void edge
        if t < 0.10:
            # bright emissive core #c060ff
            t2=t/0.10
            r=int(192+(255-192)*(1-t2))
            g=int(96+(255-96)*(1-t2))
            b=int(255)
        elif t < 0.20:
            # #c060ff to #5020b0
            t2=(t-0.10)/0.10
            r=int(192+(80-192)*t2)
            g=int(96+(32-96)*t2)
            b=int(255+(176-255)*t2)
        elif t < 0.45:
            # #5020b0 to #1a0850
            t2=(t-0.20)/0.25
            r=int(80+(26-80)*t2)
            g=int(32+(8-32)*t2)
            b=int(176+(80-176)*t2)
        elif t < 0.75:
            # #1a0850 to #060210
            t2=(t-0.45)/0.30
            r=int(26+(6-26)*t2)
            g=int(8+(2-8)*t2)
            b=int(80+(16-80)*t2)
        else:
            # void edge #060210
            t2=(t-0.75)/0.25
            r=int(6*(1-t2*0.5))
            g=int(2*(1-t2*0.5))
            b=int(16*(1-t2*0.5))
        px[x,y]=(max(0,min(255,r)),max(0,min(255,g)),max(0,min(255,b)),255)

# Gold ring detail at 60% radius
ring_r=max_r*0.60
for y in range(H):
    for x in range(W):
        dx=x-cx; dy=y-cy
        dist=math.sqrt(dx*dx+dy*dy)
        if abs(dist-ring_r) < 1.5:
            t=1.0-abs(dist-ring_r)/1.5
            r2,g2,b2=px[x,y][:3]
            r=int(r2+(128-r2)*t)
            g=int(g2+(96-g2)*t)
            b=int(b2+(16-b2)*t)
            px[x,y]=(r,g,b,255)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/dream_collapse_ring_tex1.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m05_tex1_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
