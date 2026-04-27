"""Texture 1 for devils_constellation — connecting blood-line texture.
Near-black with a single 2px bright red emissive strip at horizontal
center (cols 31-32). Used edge-on for the constellation lines."""
import base64, math, random
from PIL import Image
W = H = 64
img = Image.new("RGBA", (W, H), (0, 0, 0, 255))
px = img.load()

def hx(c):
    c = c.lstrip('#')
    return (int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16))

def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

rng = random.Random(2021)

VOID       = hx('#040208')
VOID_DEEP  = hx('#020106')
RED_HOT    = hx('#ff2020')
RED        = hx('#d01010')
RED_DEEP   = hx('#601010')

# Fill with near-black void
for y in range(H):
    for x in range(W):
        base = VOID if (x + y) % 4 != 0 else VOID_DEEP
        gn = rng.randint(-1, 1)
        r = max(0, min(255, base[0] + gn))
        g = max(0, min(255, base[1] + gn))
        b = max(0, min(255, base[2] + gn))
        px[x, y] = (r, g, b, 255)

# Vertical bright red strip at cols 31-32 (the line)
for y in range(H):
    px[31, y] = (*RED, 255)
    px[32, y] = (*RED, 255)

# Hot emissive core highlight every few px
for y in range(H):
    if y % 3 == 0:
        px[31, y] = (*RED_HOT, 255)
        px[32, y] = (*RED_HOT, 255)

# Falloff cols 30 and 33 (deep red)
for y in range(H):
    cur30 = px[30, y]
    cur33 = px[33, y]
    blended30 = lerp(cur30[:3], RED_DEEP, 0.6)
    blended33 = lerp(cur33[:3], RED_DEEP, 0.6)
    px[30, y] = (*blended30, 255)
    px[33, y] = (*blended33, 255)

# Even softer falloff cols 29 and 34
for y in range(H):
    cur29 = px[29, y]
    cur34 = px[34, y]
    blended29 = lerp(cur29[:3], RED_DEEP, 0.25)
    blended34 = lerp(cur34[:3], RED_DEEP, 0.25)
    px[29, y] = (*blended29, 255)
    px[34, y] = (*blended34, 255)

# Subtle horizontal red flicker dashes elsewhere (broken energy)
for _ in range(8):
    fy = rng.randint(2, 62)
    fx = rng.choice([5, 12, 18, 45, 52, 58])
    px[fx, fy] = (*RED_DEEP, 255)
    if fx + 1 < W:
        px[fx + 1, fy] = (*RED_DEEP, 255)

out = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/devils_constellation_tex1.png"
img.save(out)
b64 = base64.b64encode(open(out, 'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m20_tex1_b64.txt", "w").write(b64)
import os
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
