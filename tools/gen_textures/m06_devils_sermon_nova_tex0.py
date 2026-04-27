import base64, math, random
from PIL import Image, ImageDraw
W=H=64; img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()

# Blood-red to gold gradient technique
base_blood  = (26, 4, 4)     # #1a0404
dark_crimson= (80, 8, 8)     # #500808
blood_surf  = (160, 16, 16)  # #a01010
bright_gold = (208, 160, 32) # #d0a020
emissive_gw = (255, 224, 64) # #ffe040
top_gold    = (255, 216, 48) # #ffd830
bot_blood   = (8, 2, 2)      # #080202
spec_gold   = (255, 208, 64) # #ffd040

# Base fill: vertical gradient from near-black blood (bottom) to mid crimson (top)
for y in range(H):
    t = y / (H-1)  # 0=top, 1=bottom in PIL coords
    # bottom = near-black, top = crimson-gold
    r = int(bot_blood[0] + (bright_gold[0]-bot_blood[0])*(1-t))
    g = int(bot_blood[1] + (bright_gold[1]-bot_blood[1])*(1-t))
    b = int(bot_blood[2] + (bright_gold[2]-bot_blood[2])*(1-t))
    for x in range(W):
        px[x,y] = (max(0,min(255,r)),max(0,min(255,g)),max(0,min(255,b)),255)

# Add horizontal crimson-to-gold bands from center
cx=W//2; cy=H//2
for y in range(H):
    for x in range(W):
        dx=x-cx; dy=y-cy
        dist=math.sqrt(dx*dx+dy*dy)/math.sqrt(cx*cx+cy*cy)
        # radial: centre blood surface, edge top gold
        r_base = int(blood_surf[0] + (top_gold[0]-blood_surf[0])*min(dist,1.0))
        g_base = int(blood_surf[1] + (top_gold[1]-blood_surf[1])*min(dist,1.0))
        b_base = int(blood_surf[2] + (top_gold[2]-blood_surf[2])*min(dist,1.0))
        # blend with vertical gradient
        vy_r, vy_g, vy_b = px[x,y][:3]
        px[x,y] = (
            max(0,min(255,(r_base+vy_r)//2)),
            max(0,min(255,(g_base+vy_g)//2)),
            max(0,min(255,(b_base+vy_b)//2)),
            255
        )

# Draw gold edge lines (diagonal striping to suggest rays)
for y in range(H):
    for x in range(W):
        # diagonal stripe at 45°
        d = (x + y) % 12
        if d < 2:
            t = d / 2.0
            r,g,b = px[x,y][:3]
            r = min(255, int(r + (emissive_gw[0]-r)*0.8))
            g = min(255, int(g + (emissive_gw[1]-g)*0.8))
            b = min(255, int(b + (emissive_gw[2]-b)*0.8))
            px[x,y] = (r,g,b,255)
        # vertical bright gold stripe (edge catchlight)
        if x >= W-4:
            t = (x-(W-4))/4.0
            r,g,b = px[x,y][:3]
            r = min(255, int(r + (top_gold[0]-r)*t))
            g = min(255, int(g + (top_gold[1]-g)*t))
            b = min(255, int(b + (top_gold[2]-b)*t))
            px[x,y] = (r,g,b,255)

# Specular cluster at (55,5) — pure gold
for dy in range(-2,3):
    for dx in range(-2,3):
        nx,ny = 55+dx, 5+dy
        if 0<=nx<W and 0<=ny<H:
            dist = math.sqrt(dx*dx+dy*dy)
            if dist < 2.5:
                t = 1.0 - dist/2.5
                r,g,b = px[nx,ny][:3]
                r = min(255, int(r + (255-r)*t))
                g = min(255, int(g + (spec_gold[1]-g)*t))
                b = min(255, int(b + (spec_gold[2]-b)*t))
                px[nx,ny] = (r,g,b,255)

# Top row pure gold
for x in range(W):
    px[x,0] = top_gold + (255,)
    px[x,1] = top_gold + (255,)

# Bottom rows near-black
for x in range(W):
    px[x,H-1] = bot_blood + (255,)
    px[x,H-2] = bot_blood + (255,)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/devils_sermon_nova_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m06_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
