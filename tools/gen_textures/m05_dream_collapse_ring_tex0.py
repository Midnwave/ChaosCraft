import base64, math, random
from PIL import Image, ImageDraw
W=H=64; img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()

# Colors
base_dark = (10,4,24)       # #0a0418 deep purple-black
mid_violet = (48,24,160)    # #3018a0
bright_purple = (112,64,224) # #7040e0
silver_white = (208,200,240) # #d0c8f0
emissive = (192,96,255)     # #c060ff

# Fill base
for y in range(H):
    for x in range(W):
        px[x,y] = base_dark + (255,)

# Draw shattered glass cracks - hard edge lines at ~15, 45, 75 degrees
draw = ImageDraw.Draw(img)

# Deep purple zones
for y in range(H):
    for x in range(W):
        # Diagonal bands
        d = (x + y) % 20
        if d < 8:
            t = d / 8.0
            c = tuple(int(base_dark[i] + (mid_violet[i]-base_dark[i])*t) for i in range(3))
            px[x,y] = c + (255,)
        else:
            t = (d-8)/12.0
            c = tuple(int(mid_violet[i] + (bright_purple[i]-mid_violet[i])*t) for i in range(3))
            px[x,y] = c + (255,)

# Crack lines at 15 degrees (pixel approximation: every 4 right, 1 up)
for start_x in range(-H, W, 8):
    for step in range(W+H):
        cx = start_x + step
        cy = step // 4
        if 0 <= cx < W and 0 <= cy < H:
            px[cx,cy] = silver_white + (255,)
            if cx+1 < W: px[cx+1,cy] = silver_white + (255,)

# Crack lines at 45 degrees
for start_x in range(-H, W, 10):
    for step in range(W+H):
        cx = start_x + step
        cy = step
        if 0 <= cx < W and 0 <= cy < H:
            px[cx,cy] = silver_white + (255,)

# Crack lines at 75 degrees (4 up, 1 right)
for start_y in range(-W, H, 9):
    for step in range(W+H):
        cx = step // 4
        cy = start_y + step
        if 0 <= cx < W and 0 <= cy < H:
            px[cx,cy] = silver_white + (255,)

# Bright purple emissive zones between cracks
for y in range(H):
    for x in range(W):
        r,g,b,a = px[x,y]
        if (r,g,b) != silver_white:
            # Add emissive tint in certain bands
            band = (x*2 + y) % 16
            if band < 4:
                px[x,y] = emissive + (255,)

# Specular cluster at (8,6) - bright shard
for dy in range(-2,3):
    for dx in range(-2,3):
        nx,ny = 8+dx, 6+dy
        if 0<=nx<W and 0<=ny<H:
            dist = math.sqrt(dx*dx+dy*dy)
            if dist < 2.5:
                t = 1.0 - dist/2.5
                c = tuple(int(silver_white[i]+(255-silver_white[i])*t) for i in range(3))
                px[nx,ny] = c + (255,)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/dream_collapse_ring_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m05_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
