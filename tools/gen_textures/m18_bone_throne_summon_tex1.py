"""Texture 1 for bone_throne_summon - ground cracks.
Near-black with bone-white branching crack lines from a central source."""
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

rng = random.Random(1820)

VOID    = hx('#080604')
VOID2   = hx('#100804')
CRACK   = hx('#c0b8a8')
CRACK2  = hx('#d8d0c0')
CRACK_D = hx('#605040')

# Fill with deep black with subtle variation
for y in range(H):
    for x in range(W):
        if rng.random() < 0.05:
            px[x, y] = (*VOID2, 255)
        else:
            gn = rng.randint(-2, 4)
            r = max(0, min(255, VOID[0] + gn))
            g = max(0, min(255, VOID[1] + gn))
            b = max(0, min(255, VOID[2] + gn))
            px[x, y] = (r, g, b, 255)

# Central source point
cx, cy = W // 2, H // 2

def draw_branch(x0, y0, angle, length, depth, intensity):
    if depth < 0 or length < 1:
        return
    x = x0
    y = y0
    for step in range(length):
        # Move in angle direction
        nx = int(x + math.cos(angle))
        ny = int(y + math.sin(angle))
        if not (0 <= nx < W and 0 <= ny < H):
            return
        # Draw with intensity
        cur = px[nx, ny]
        col = CRACK if rng.random() < 0.7 else CRACK2
        falloff = 1.0 - step / length
        strength = intensity * falloff
        blended = lerp(cur[:3], col, strength)
        px[nx, ny] = (*blended, 255)
        x, y = nx, ny

        # Possibly branch
        if rng.random() < 0.15 and depth > 0 and step > 2:
            new_angle = angle + rng.uniform(-1.0, 1.0)
            new_length = max(2, length // 2 - rng.randint(0, 2))
            draw_branch(x, y, new_angle, new_length, depth - 1, intensity * 0.7)

        # Slight angle drift
        angle += rng.uniform(-0.15, 0.15)

# Draw 8 main cracks radiating from center
num_main = 8
for i in range(num_main):
    base_angle = (i / num_main) * 2 * math.pi + rng.uniform(-0.3, 0.3)
    main_length = rng.randint(20, 36)
    draw_branch(cx, cy, base_angle, main_length, 3, 0.95)

# Add 6 secondary cracks at varying angles
for _ in range(6):
    sx = rng.randint(8, W - 8)
    sy = rng.randint(8, H - 8)
    angle = rng.uniform(0, 2 * math.pi)
    length = rng.randint(10, 18)
    draw_branch(sx, sy, angle, length, 2, 0.7)

# Central bright spot — the source
for y in range(cy - 3, cy + 4):
    for x in range(cx - 3, cx + 4):
        if 0 <= x < W and 0 <= y < H:
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            if d <= 3:
                t = 1.0 - d / 3
                col = lerp(CRACK_D, CRACK2, t)
                cur = px[x, y]
                blended = lerp(cur[:3], col, t * 0.85)
                px[x, y] = (*blended, 255)

# Scattered debris specks
for _ in range(60):
    sx = rng.randint(0, W - 1)
    sy = rng.randint(0, H - 1)
    cur = px[sx, sy]
    if cur[0] < 40:
        col = CRACK_D
        blended = lerp(cur[:3], col, 0.4)
        px[sx, sy] = (*blended, 255)

out = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/bone_throne_summon_tex1.png"
img.save(out)
b64 = base64.b64encode(open(out, 'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m18_tex1_b64.txt", "w").write(b64)
import os
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
