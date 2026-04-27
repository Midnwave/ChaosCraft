"""Texture 1 for fallen_seraph_skeleton — ground cracks.
Near-black with pale bone-white cracks branching from centre. Cracks look like bone fractures."""
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

rng = random.Random(2223)

DARK     = hx('#080608')
DARK2    = hx('#141016')
BONE_C   = hx('#e8e0d0')
BONE_HOT = hx('#ffffff')
DIV_PURP = hx('#a890e0')

# Fill near-black background with subtle variation
for y in range(H):
    for x in range(W):
        r = DARK[0] + rng.randint(-2, 6)
        g = DARK[1] + rng.randint(-2, 6)
        b = DARK[2] + rng.randint(-2, 8)
        if rng.random() < 0.05:
            r, g, b = DARK2
        px[x, y] = (max(0, r), max(0, g), max(0, b), 255)

# Central impact node (darker spot)
cx, cy = W // 2, H // 2
for y in range(H):
    for x in range(W):
        d = math.sqrt((x - cx)**2 + (y - cy)**2)
        if d < 5:
            cur = px[x, y]
            blended = lerp(cur[:3], (4, 2, 4), 0.7)
            px[x, y] = (*blended, 255)

# Branching cracks radiating from centre
def branch_crack(start_x, start_y, angle, length, depth=0):
    if depth > 3 or length < 2:
        return
    cxp, cyp = start_x, start_y
    cur_angle = angle
    for step in range(length):
        ix = int(cxp); iy = int(cyp)
        if 0 <= ix < W and 0 <= iy < H:
            cur = px[ix, iy]
            blended = lerp(cur[:3], BONE_C, 0.85)
            px[ix, iy] = (*blended, 255)
            # widen
            if rng.random() < 0.4:
                for dd_x, dd_y in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
                    nx, ny = ix + dd_x, iy + dd_y
                    if 0 <= nx < W and 0 <= ny < H:
                        cur2 = px[nx, ny]
                        blended2 = lerp(cur2[:3], BONE_C, 0.40)
                        px[nx, ny] = (*blended2, 255)
        cur_angle += rng.uniform(-0.35, 0.35)
        cxp += math.cos(cur_angle)
        cyp += math.sin(cur_angle)
        # spawn a sub-branch occasionally
        if rng.random() < 0.10 and step > 2:
            new_angle = cur_angle + rng.uniform(-1.2, 1.2)
            new_len = rng.randint(3, max(4, length // 2))
            branch_crack(cxp, cyp, new_angle, new_len, depth + 1)

# 8 main branches from centre
for i in range(8):
    angle = (i / 8.0) * 2 * math.pi + rng.uniform(-0.25, 0.25)
    length = rng.randint(20, 32)
    branch_crack(cx, cy, angle, length)

# Hot spots along cracks (specular)
for y in range(H):
    for x in range(W):
        r, g, b, _ = px[x, y]
        if r > 180 and rng.random() < 0.10:
            cur = px[x, y]
            blended = lerp(cur[:3], BONE_HOT, 0.45)
            px[x, y] = (*blended, 255)

# Subtle purple bleed near cracks (divine grief)
for y in range(H):
    for x in range(W):
        r, g, b, _ = px[x, y]
        if r > 150 and r < 230:
            for dx in range(-1, 2):
                for dy in range(-1, 2):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < W and 0 <= ny < H and (dx != 0 or dy != 0):
                        cur = px[nx, ny]
                        if cur[0] < 30 and rng.random() < 0.15:
                            blended = lerp(cur[:3], DIV_PURP, 0.20)
                            px[nx, ny] = (*blended, 255)

out = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/fallen_seraph_skeleton_tex1.png"
img.save(out)
b64 = base64.b64encode(open(out, 'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m22_tex1_b64.txt", "w").write(b64)
import os
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
