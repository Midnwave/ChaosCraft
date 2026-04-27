"""Texture 1 for fallen_halo_burst — Divine Corruption (inner glow/fracture/shadow)."""
import base64, math, random
from PIL import Image

random.seed(55)
W = H = 64
img = Image.new("RGBA", (W, H), (0, 0, 0, 255))
px = img.load()

def hx(c):
    c = c.lstrip('#')
    return (int(c[0:2],16), int(c[2:4],16), int(c[4:6],16), 255)

def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(4))

# Colors — divine corruption gradient
bright_white  = hx('#fffff0')
sickly_yellow = hx('#d0c840')
corrupt_purple= hx('#6020a0')
near_black    = hx('#080410')

def radial_corrupt(cx, cy, max_r, x_range, y_range):
    """Fill a zone with divine corruption radial gradient."""
    for y in y_range:
        for x in x_range:
            dx = x - cx
            dy = y - cy
            dist = math.sqrt(dx*dx + dy*dy)
            if dist > max_r:
                px[x, y] = near_black
                continue
            t = dist / max_r
            # white → yellow → purple → black
            if t < 0.25:
                c = lerp(bright_white, sickly_yellow, t / 0.25)
            elif t < 0.55:
                c = lerp(sickly_yellow, corrupt_purple, (t - 0.25) / 0.30)
            elif t < 0.85:
                c = lerp(corrupt_purple, near_black, (t - 0.55) / 0.30)
            else:
                c = near_black
            px[x, y] = c

# Zone A (north/south face, y<32, x<32): radial from center
radial_corrupt(16, 16, 15, range(32), range(32))

# Zone B (east/west face, y<32, x>=32): radial from zone center
radial_corrupt(48, 16, 15, range(32, 64), range(32))

# Zone C (up face, y>=32, x<32): up-face — fracture line detail, radial
# More compressed — the fracture texture should show corruption spreading
for y in range(32, 64):
    for x in range(32):
        cx_c, cy_c = 16, 48
        dx = x - cx_c
        dy = y - cy_c
        dist = math.sqrt(dx*dx + dy*dy)
        t = min(1.0, dist / 13.0)
        # Add diagonal fracture veins
        vein_angle = math.atan2(dy, dx)
        vein_pattern = abs(math.sin(vein_angle * 4))  # 4 fracture directions
        vein_strength = vein_pattern * max(0, 1.0 - t)
        if t < 0.3:
            c = lerp(bright_white, sickly_yellow, t / 0.3)
        elif t < 0.65:
            c = lerp(sickly_yellow, corrupt_purple, (t - 0.3) / 0.35)
        else:
            c = lerp(corrupt_purple, near_black, (t - 0.65) / 0.35)
        # Brighten along vein lines
        if vein_strength > 0.7:
            vt = (vein_strength - 0.7) / 0.3
            c = lerp(c, bright_white, vt * 0.5)
        px[x, y] = c

# Zone D (down face/emissive, y>=32, x>=32): most intense — pure emissive
for y in range(32, 64):
    for x in range(32, 64):
        cx_d, cy_d = 48, 48
        dx = x - cx_d
        dy = y - cy_d
        dist = math.sqrt(dx*dx + dy*dy)
        t = min(1.0, dist / 14.0)
        # Very bright centre, sharp falloff to deep purple
        if t < 0.15:
            c = bright_white
        elif t < 0.35:
            c = lerp(bright_white, sickly_yellow, (t - 0.15) / 0.20)
        elif t < 0.6:
            c = lerp(sickly_yellow, corrupt_purple, (t - 0.35) / 0.25)
        else:
            c = lerp(corrupt_purple, near_black, (t - 0.6) / 0.4)
        px[x, y] = c

# Scatter corruption spark dots
for _ in range(60):
    sx = random.randint(0, 63)
    sy = random.randint(0, 63)
    c = px[sx, sy]
    # Blend toward bright yellow-white
    target = lerp(sickly_yellow, bright_white, random.random())
    px[sx, sy] = lerp(c, target, 0.5 + random.random() * 0.4)

# Fine corruption veins — thin diagonal lines
for _ in range(8):
    vx = random.randint(5, 59)
    vy = random.randint(5, 59)
    angle = random.random() * math.pi
    length = random.randint(6, 14)
    for step in range(length):
        nx = int(vx + step * math.cos(angle))
        ny = int(vy + step * math.sin(angle))
        if 0 <= nx < 64 and 0 <= ny < 64:
            px[nx, ny] = lerp(px[nx, ny], corrupt_purple, 0.7)

out = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/fallen_halo_burst_tex1.png"
img.save(out)
b64 = base64.b64encode(open(out, 'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m04_tex1_b64.txt", "w").write(b64)
import os
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
