import base64, math, random, os
from PIL import Image

W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255))
px=img.load()

random.seed(7008)

# Darker purple-shifted static palette
palette_dark=[
    (0x10,0x18,0x20),  # dark base
    (0x18,0x1e,0x28),  # slightly lighter
    (0x20,0x28,0x38),  # mid-dark
    (0x28,0x30,0x40),  # mid
    (0x38,0x40,0x50),  # upper-mid
    (0x0c,0x10,0x18),  # near-black
    (0x20,0x18,0x30),  # purple-tinted dark
    (0x18,0x14,0x28),  # deep purple-dark
    (0x30,0x28,0x48),  # purple mid
]

# Fill entire image with random dark purple-shifted pixels
for y in range(H):
    for x in range(W):
        c = random.choice(palette_dark)
        px[x,y] = (c[0],c[1],c[2],255)

# Emissive purple hotspots
random.seed(7008+1)
num_hotspots = 20
for _ in range(num_hotspots):
    hx2 = random.randint(1, W-3)
    hy2 = random.randint(1, H-3)
    cluster_size = random.randint(4,6)
    offsets = [(0,0),(1,0),(0,1),(1,1),(-1,0),(0,-1)]
    for i in range(cluster_size):
        ox,oy = offsets[i % len(offsets)]
        cx2 = (hx2+ox) % W
        cy2 = (hy2+oy) % H
        # #8060c0 purple emissive
        px[cx2,cy2] = (0x80,0x60,0xc0,255)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_static_field_tex1.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m07_tex1_b64.txt","w").write(b64)
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
