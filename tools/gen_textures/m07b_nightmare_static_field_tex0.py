import base64, math, random
from PIL import Image

random.seed(7007)
W = H = 64
img = Image.new("RGBA", (W, H), (0, 0, 0, 255))
px = img.load()

def hx(c):
    c = c.lstrip('#')
    return (int(c[0:2],16), int(c[2:4],16), int(c[4:6],16))

base_col = hx('#606878')
dark_col = hx('#404850')
mid_col  = hx('#808890')
light_col= hx('#b0b8c0')
bright_col=hx('#d8e0e8')
near_black=hx('#101418')
near_white=hx('#e8f0f8')

palette = [base_col, dark_col, mid_col, light_col, bright_col, near_black, near_white]
weights  = [30, 15, 20, 15, 10, 5, 5]

# Fill entire image with weighted random static noise
for y in range(H):
    for x in range(W):
        r = random.random() * sum(weights)
        acc = 0
        chosen = palette[0]
        for i, w in enumerate(weights):
            acc += w
            if r < acc:
                chosen = palette[i]
                break
        px[x, y] = chosen + (255,)

# Add emissive near-white hotspot clusters (zone D: bottom-right 32x32)
# Zone A top-left 32x32 = primary static pattern
# Zone B top-right 32x32 = east/west faces
# Zone C bottom-left 32x32 = up face
# Zone D bottom-right 32x32 = emissive

# Emissive zone D: bright hotspots
num_clusters = 18
for _ in range(num_clusters):
    cx = random.randint(34, 62)
    cy = random.randint(34, 62)
    radius = random.randint(1, 4)
    for dy in range(-radius, radius+1):
        for dx in range(-radius, radius+1):
            if dx*dx + dy*dy <= radius*radius:
                nx, ny = cx+dx, cy+dy
                if 34 <= nx <= 62 and 34 <= ny <= 62:
                    # blend toward near-white
                    cur = px[nx, ny]
                    t = 1.0 - (dx*dx+dy*dy)**0.5 / radius
                    nr = int(cur[0] + (near_white[0]-cur[0])*t)
                    ng = int(cur[1] + (near_white[1]-cur[1])*t)
                    nb = int(cur[2] + (near_white[2]-cur[2])*t)
                    px[nx, ny] = (min(255,nr), min(255,ng), min(255,nb), 255)

out = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_static_field_tex.png"
img.save(out)
b64 = base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m07b_b64.txt","w").write(b64)
import os
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
