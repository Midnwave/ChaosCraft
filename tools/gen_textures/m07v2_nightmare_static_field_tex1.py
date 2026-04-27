import base64, random, os
from PIL import Image

# MODEL 7 — Nightmare Static Field, texture 1
# Darker static — base #101820, variations between #181e28 and #384050.
# Purple-shifted static. Emissive: #8060c0 purple static hotspots.

W = H = 64
img = Image.new("RGBA", (W, H), (0, 0, 0, 255))
px = img.load()

random.seed(70704)

palette = [
    (0x10, 0x18, 0x20),  # base
    (0x10, 0x18, 0x20),
    (0x10, 0x18, 0x20),
    (0x18, 0x1e, 0x28),  # min variation
    (0x20, 0x26, 0x32),
    (0x28, 0x2e, 0x3c),
    (0x30, 0x36, 0x46),
    (0x38, 0x40, 0x50),  # max variation
    (0x18, 0x14, 0x28),  # purple-tinted dark
    (0x22, 0x18, 0x36),  # purple-tinted mid
    (0x14, 0x1c, 0x2c),  # blue-tinted dark
]

for y in range(H):
    for x in range(W):
        r2 = random.random()
        if r2 < 0.03:
            c = (0x08, 0x0c, 0x14)  # near-black outlier
        else:
            c = random.choice(palette)
        px[x, y] = (c[0], c[1], c[2], 255)

# Purple emissive hotspots
random.seed(70705)
num_hotspots = 22
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
        px[cx, cy] = (0x80, 0x60, 0xc0, 255)

out = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_static_field_tex1.png"
img.save(out)
b64 = base64.b64encode(open(out, 'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m07v2_tex1_b64.txt", "w").write(b64)
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
