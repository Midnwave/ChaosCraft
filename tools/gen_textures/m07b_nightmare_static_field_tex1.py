import base64, math, random
from PIL import Image

random.seed(7008)
W = H = 64
img = Image.new("RGBA", (W, H), (0, 0, 0, 255))
px = img.load()

def hx(c):
    c = c.lstrip('#')
    return (int(c[0:2],16), int(c[2:4],16), int(c[4:6],16))

base_col  = hx('#101820')
dark1     = hx('#181e28')
dark2     = hx('#242a38')
mid_dark  = hx('#2c3340')
shift_mid = hx('#384050')
purple_dim= hx('#281830')

palette = [base_col, dark1, dark2, mid_dark, shift_mid, purple_dim]
weights  = [25, 20, 20, 15, 12, 8]

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

# Emissive zone D: purple hotspots
emissive_col = hx('#8060c0')
num_clusters = 14
for _ in range(num_clusters):
    cx = random.randint(34, 62)
    cy = random.randint(34, 62)
    radius = random.randint(1, 3)
    for dy in range(-radius, radius+1):
        for dx in range(-radius, radius+1):
            if dx*dx + dy*dy <= radius*radius:
                nx, ny = cx+dx, cy+dy
                if 34 <= nx <= 62 and 34 <= ny <= 62:
                    cur = px[nx, ny]
                    t = 1.0 - (dx*dx+dy*dy)**0.5 / (radius+0.001)
                    nr = int(cur[0] + (emissive_col[0]-cur[0])*t)
                    ng = int(cur[1] + (emissive_col[1]-cur[1])*t)
                    nb = int(cur[2] + (emissive_col[2]-cur[2])*t)
                    px[nx, ny] = (min(255,nr), min(255,ng), min(255,nb), 255)

out = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_static_field_tex1.png"
img.save(out)
b64 = base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m07b_tex1_b64.txt","w").write(b64)
import os
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
