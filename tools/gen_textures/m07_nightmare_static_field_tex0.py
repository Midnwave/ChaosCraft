import base64, math, random, os
from PIL import Image

W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255))
px=img.load()

random.seed(7007)

# Palette for static noise
palette_base=[
    (0x60,0x68,0x78),  # silver-grey base
    (0x40,0x48,0x50),  # dark
    (0x80,0x88,0x90),  # mid
    (0xb0,0xb8,0xc0),  # light
    (0xd8,0xe0,0xe8),  # near-white
    (0x10,0x14,0x18),  # near-black
    (0xe8,0xf0,0xf8),  # white
    (0x50,0x58,0x68),  # dark-mid
    (0x90,0x98,0xa0),  # upper-mid
    (0xc0,0xc8,0xd0),  # bright
]

# Fill entire image with random pixel variation
for y in range(H):
    for x in range(W):
        c = random.choice(palette_base)
        # Occasional outliers
        r2 = random.random()
        if r2 < 0.03:
            c = (0x10,0x14,0x18,255)  # near-black outlier
        elif r2 < 0.06:
            c = (0xe8,0xf0,0xf8,255)  # near-white outlier
        else:
            c = (c[0],c[1],c[2],255)
        px[x,y] = c

# Emissive hotspots: clusters of 4-6 adjacent near-white pixels
random.seed(7007+1)
num_hotspots = 24
for _ in range(num_hotspots):
    hx2 = random.randint(1, W-3)
    hy2 = random.randint(1, H-3)
    cluster_size = random.randint(4,6)
    offsets = [(0,0),(1,0),(0,1),(1,1),(-1,0),(0,-1)]
    for i in range(cluster_size):
        ox,oy = offsets[i % len(offsets)]
        cx2 = (hx2+ox) % W
        cy2 = (hy2+oy) % H
        # near-white emissive
        px[cx2,cy2] = (0xe8,0xf0,0xf8,255)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_static_field_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m07_b64.txt","w").write(b64)
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
