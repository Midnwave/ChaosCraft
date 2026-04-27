"""Texture 1 for fallen_feather_lance: Pure void black with purple corruption veins branching downward."""
import base64, math, random
from PIL import Image
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

nblk=hx("#030308"); purp=hx("#8040c0"); purp_b=hx("#c060ff"); purp_d=hx("#401060")
random.seed(9)

# Base: void black
for y in range(64):
    for x in range(64):
        px[x,y]=nblk+(255,)

# Purple corruption veins — branching from top edge downward
def draw_vein(x0,y0,dx,length,width,bright):
    cx=x0
    for step in range(length):
        cy=y0+step
        for w in range(-width,width+1):
            nx=cx+w
            if 0<=nx<64 and 0<=cy<64:
                t=step/max(1,length-1)
                col=lerp(bright,purp_d,t)
                # add some flicker
                if random.random()<0.3: col=lerp(col,purp_b,0.4)
                px[nx,cy]=col+(255,)
        # slight wander
        if random.random()<0.3: cx+=random.choice([-1,0,1])
        cx=max(0,min(63,cx))

# Primary veins from top
for vx in [10,22,35,48,58]:
    draw_vein(vx,0,0,random.randint(35,55),1,purp)
    # sub-branches
    branch_y=random.randint(8,20)
    draw_vein(vx-2,branch_y,-1,random.randint(15,25),0,purp_d)
    draw_vein(vx+3,branch_y,1,random.randint(12,22),0,purp)

# Zone A (0,0)-(31,31): heavy vein concentration
for y in range(32):
    for x in range(32):
        if (x*3+y*7)%11==0:
            col=lerp(nblk,purp,0.6)
            px[x,y]=col+(255,)

# Zone B (32,0)-(63,31): lighter vein pattern
for y in range(32):
    for x in range(32,64):
        if (x+y*5)%13==0:
            px[x,y]=lerp(nblk,purp_d,0.5)+(255,)

# Zone C (0,32)-(31,63): soft purple glow pool
cx2,cy2=15.5,47.5
maxr=math.sqrt(15.5**2+15.5**2)
for y in range(32,64):
    for x in range(32):
        r=math.sqrt((x-cx2)**2+(y-cy2)**2)/maxr
        if r<0.5:
            px[x,y]=lerp(purp,nblk,r/0.5)+(255,)

# Zone D (32,32)-(63,63): deepest void with faint purple
for y in range(32,64):
    for x in range(32,64):
        col=nblk
        if (x+y)%17==0: col=lerp(nblk,purp_d,0.3)
        px[x,y]=col+(255,)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/fallen_feather_lance_tex1.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m09_tex1_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
