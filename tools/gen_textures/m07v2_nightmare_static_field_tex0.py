import base64, random, os
from PIL import Image

# MODEL 7 — Nightmare Static Field, texture 0
# Static noise: silver-grey base #606878 with random pixel-level variation
# between #404850, #808890, #b0b8c0, #d8e0e8.
# Some pixels near-black #101418, some near-white #e8f0f8.
# Emissive: clusters of 4-6 adjacent near-white pixels at random positions.

W = H = 64
img = Image.new("RGBA", (W, H), (0, 0, 0, 255))
px = img.load()

random.seed(70702)

palette = [
    (0x60, 0x68, 0x78),  # silver-grey base (most common)
    (0x60, 0x68, 0x78),
    (0x60, 0x68, 0x78),
    (0x40, 0x48, 0x50),
    (0x80, 0x88, 0x90),
    (0xb0, 0xb8, 0xc0),
    (0xd8, 0xe0, 0xe8),
    (0x50, 0x58, 0x68),
    (0x70, 0x78, 0x88),
    (0x90, 0x98, 0xa8),
]

for y in range(H):
    for x in range(W):
        r2 = random.random()
        if r2 < 0.04:
            c = (0x10, 0x14, 0x18)  # near-black outlier
        elif r2 < 0.08:
            c = (0xe8, 0xf0, 0xf8)  # near-white outlier
        else:
            c = random.choice(palette)
        px[x, y] = (c[0], c[1], c[2], 255)

# Emissive hotspots: clusters of 4-6 adjacent near-white pixels
random.seed(70703)
num_hotspots = 28
cluster_offsets_options = [
    [(0, 0), (1, 0), (0, 1), (1, 1)],
    [(0, 0), (1, 0), (2, 0), (0, 1), (1, 1)],
    [(0, 0), (1, 0), (0, 1), (1, 1), (2, 1), (1, 2)],
    [(0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)],
    [(0, 0), (1, 0), (0, 1), (-1, 1), (1, -1), (0, -1)],
]
for _ in range(num_hotspots):
    hx = random.randint(2, W - 4)
    hy = random.randint(2, H - 4)
    offsets = random.choice(cluster_offsets_options)
    for ox, oy in offsets:
        cx = (hx + ox) % W
        cy = (hy + oy) % H
        px[cx, cy] = (0xe8, 0xf0, 0xf8, 255)

out = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_static_field_tex.png"
img.save(out)
b64 = base64.b64encode(open(out, 'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m07v2_b64.txt", "w").write(b64)
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
