import base64, math, random
from PIL import Image
W=H=64; img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()

def hx(c):
    c=c.lstrip('#')
    return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))

def lerp(a,b,t):
    return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

rng = random.Random(1516)

# CORRUPTION DRIPS / SHADOW DISC
# pure void black #030308 base with purple corruption rivulets #8030c0
# branching from top edge downward like tears
VOID  = hx('#030308')
PUR   = hx('#8030c0')
PUR2  = hx('#a040d8')
PUR_D = hx('#401060')

# Initialize all to void
for y in range(H):
    for x in range(W):
        px[x,y] = (*VOID, 255)

# Generate 6 main rivulets from top edge
rivulet_starts = [6, 16, 24, 34, 44, 54]
for sx in rivulet_starts:
    cx = sx + rng.randint(-2, 2)
    cy = 0
    width = rng.randint(1, 2)
    while cy < H:
        # Drift sideways slightly
        cx += rng.choice([-1, 0, 0, 0, 1])
        cx = max(0, min(W-1, cx))

        # Determine intensity by depth (brightest near top, dimmer below)
        depth_t = cy / H
        col = lerp(PUR2, PUR_D, depth_t)

        for dx in range(-width, width+1):
            xx = cx + dx
            if 0 <= xx < W:
                edge_factor = 1.0 - abs(dx) / (width + 1)
                r = int(VOID[0] + (col[0] - VOID[0]) * edge_factor)
                g = int(VOID[1] + (col[1] - VOID[1]) * edge_factor)
                b = int(VOID[2] + (col[2] - VOID[2]) * edge_factor)
                # Blend with existing pixel (max)
                exist = px[xx, cy]
                px[xx, cy] = (max(exist[0], r), max(exist[1], g), max(exist[2], b), 255)

        # Branching
        if rng.random() < 0.10 and cy > 8 and cy < H - 4:
            bcx = cx
            bcy = cy
            blen = rng.randint(4, 10)
            bdir = rng.choice([-1, 1])
            for bi in range(blen):
                bcx += bdir
                bcy += rng.choice([0, 0, 1])
                if 0 <= bcx < W and bcy < H:
                    bcol = lerp(PUR, PUR_D, bi / blen)
                    exist = px[bcx, bcy]
                    px[bcx, bcy] = (max(exist[0], bcol[0]), max(exist[1], bcol[1]),
                                    max(exist[2], bcol[2]), 255)

        cy += 1
        if rng.random() < 0.05 and cy > 30:
            break  # rivulet ends

# Add isolated splatter dots
for _ in range(40):
    sx = rng.randint(0, W-1)
    sy = rng.randint(2, H-1)
    sb = rng.random()
    if sb < 0.3:
        col = PUR
    elif sb < 0.7:
        col = PUR_D
    else:
        col = PUR2
    px[sx, sy] = (col[0], col[1], col[2], 255)

# Top edge bright purple band where tears originate
for x in range(W):
    for y in range(2):
        intensity = 1.0 - y * 0.4
        col = lerp(VOID, PUR2, intensity)
        exist = px[x, y]
        if col[0] + col[1] + col[2] > exist[0] + exist[1] + exist[2]:
            px[x, y] = (col[0], col[1], col[2], 255)

# Bottom edge: pool of corruption
for x in range(W):
    for y in range(H-3, H):
        if rng.random() < 0.4:
            depth_t = (y - (H-3)) / 3
            col = lerp(PUR_D, VOID, depth_t)
            exist = px[x, y]
            if col[0] + col[1] + col[2] > exist[0] + exist[1] + exist[2]:
                px[x, y] = (col[0], col[1], col[2], 255)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/fallen_angel_wings_summon_tex1.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m15_tex1_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
