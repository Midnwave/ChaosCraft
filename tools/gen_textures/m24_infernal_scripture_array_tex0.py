"""
Texture 0 for infernal_scripture_array: MANUSCRIPT TECHNIQUE.
Pale parchment base #c8b890, aged stain #906840, ink-dark #181210.
Horizontal dark text-lines every 4 rows, varying darkness. Between lines: parchment.
Emissive zone (rows 34-62, col 34-62): #9060c0 purple ink glowing.
Most unique texture in set — must read as actual written text.
"""
import base64, math, random
from PIL import Image
W=H=64; img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()

def hx(c):
    c=c.lstrip('#')
    return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))

def lerp(a,b,t):
    return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

rng = random.Random(2424)

PARCHMENT  = hx('#c8b890')
PARCH_DARK = hx('#a89868')
PARCH_LITE = hx('#dccca8')
STAIN      = hx('#906840')
STAIN_DEEP = hx('#704028')
INK        = hx('#181210')
INK_FADE   = hx('#403028')
PURPLE     = hx('#9060c0')
PURPLE_BR  = hx('#c090f0')

# Pre-compute stains
stains = []
for _ in range(6):
    sx = rng.randint(0, W-1)
    sy = rng.randint(0, H-1)
    sr = rng.uniform(3, 7)
    stains.append((sx, sy, sr))

# Pre-generate text per line: 4 lines (one per 4-row block in rows 0-32 area)
# Each "text line" has segments to simulate words
text_lines = []
# Place text lines at rows 1, 5, 9, 13, 17, 21, 25, 29 (8 lines in non-emissive area)
text_row_ys = [1, 5, 9, 13, 17, 21, 25, 29]
for ty in text_row_ys:
    # Generate word segments along the row
    segments = []
    cur_x = rng.randint(1, 3)
    while cur_x < W - 2:
        word_len = rng.randint(2, 8)
        gap = rng.randint(1, 3)
        if cur_x + word_len < W:
            segments.append((cur_x, cur_x + word_len))
        cur_x += word_len + gap
    text_lines.append((ty, segments))

# Fill base (parchment)
for y in range(H):
    for x in range(W):
        nx = x / (W - 1)
        ny = y / (H - 1)
        # subtle vertical aging — slightly darker at edges
        edge = 1.0
        if nx < 0.10: edge -= (0.10 - nx) * 1.5
        if nx > 0.90: edge -= (nx - 0.90) * 1.5
        if ny < 0.05: edge -= (0.05 - ny) * 2.0
        if ny > 0.95: edge -= (ny - 0.95) * 2.0
        edge = max(0.65, edge)
        base = lerp(PARCH_DARK, PARCH_LITE, edge * 0.5 + 0.5)
        # noise for fibre texture
        n = rng.random() * 18 - 9
        r,g,b = base
        r=max(0,min(255,r+int(n)))
        g=max(0,min(255,g+int(n*0.85)))
        b=max(0,min(255,b+int(n*0.6)))
        px[x,y] = (r,g,b,255)

# Stains (aged blotches)
for (sx, sy, sr) in stains:
    for dy in range(-int(sr*1.5), int(sr*1.5)+1):
        for dx in range(-int(sr*1.5), int(sr*1.5)+1):
            xx = sx + dx; yy = sy + dy
            if 0 <= xx < W and 0 <= yy < H:
                d = math.sqrt(dx*dx + dy*dy)
                if d < sr*1.5:
                    falloff = 1 - d/(sr*1.5)
                    strength = falloff * 0.40
                    target = STAIN if d > sr*0.5 else STAIN_DEEP
                    cur = px[xx, yy]
                    blend = lerp(cur[:3], target, strength)
                    px[xx, yy] = (blend[0], blend[1], blend[2], 255)

# Render text lines (rows 0-32 only — outside emissive zone)
for (ty, segments) in text_lines:
    if ty >= 34: continue   # don't render in emissive zone
    # Each line has 1-2 row thickness
    darkness = rng.uniform(0.65, 0.92)
    for (xs, xe) in segments:
        for x in range(xs, xe):
            if 0 <= x < W and 0 <= ty < H:
                # Each character pixel: occasionally darker (ink), occasionally lighter (worn)
                pix_dark = darkness + rng.uniform(-0.20, 0.10)
                pix_dark = max(0.40, min(1.0, pix_dark))
                target = INK if pix_dark > 0.60 else INK_FADE
                cur = px[x, ty]
                blend = lerp(cur[:3], target, pix_dark)
                px[x, ty] = (blend[0], blend[1], blend[2], 255)
                # Sometimes dark on adjacent row for ascender/descender
                if rng.random() < 0.20 and ty + 1 < H and ty + 1 < 34:
                    cur2 = px[x, ty+1]
                    blend2 = lerp(cur2[:3], INK_FADE, 0.55)
                    px[x, ty+1] = (blend2[0], blend2[1], blend2[2], 255)

# ─── Emissive zone (rows 34-62, cols 34-62) ─────────────────────
# Purple ink glow zone — different "writing" style
ec_y_lines = [37, 41, 45, 49, 53, 57]
for ty in ec_y_lines:
    # generate word segments inside emissive zone (cols 34-62)
    cur_x = 34 + rng.randint(0, 2)
    segments = []
    while cur_x < 60:
        word_len = rng.randint(2, 6)
        gap = rng.randint(1, 2)
        if cur_x + word_len < 62:
            segments.append((cur_x, cur_x + word_len))
        cur_x += word_len + gap
    for (xs, xe) in segments:
        for x in range(xs, xe):
            if 0 <= x < W and 0 <= ty < H:
                # Brighter purple glowing ink
                bright = rng.uniform(0.70, 0.98)
                target = PURPLE_BR if bright > 0.85 else PURPLE
                cur = px[x, ty]
                blend = lerp(cur[:3], target, bright)
                px[x, ty] = (blend[0], blend[1], blend[2], 255)

# Subtle parchment base in emissive zone (slightly darker — ink shows up better)
for y in range(34, 62):
    for x in range(34, 62):
        # darken slightly for contrast against purple
        cur = px[x, y]
        blend = lerp(cur[:3], PARCH_DARK, 0.20)
        # only if not already an ink pixel
        if cur[0] > 100 and cur[1] > 80:
            px[x, y] = (blend[0], blend[1], blend[2], 255)

# Add a decorative initial — one large illuminated capital in upper-left
# A larger purple block at (3-7, 1-6) suggesting an illuminated capital
for y in range(1, 7):
    for x in range(3, 9):
        if 0 <= x < W and 0 <= y < H:
            d = math.sqrt((x-6)**2 + (y-4)**2)
            if d < 3:
                cur = px[x, y]
                blend = lerp(cur[:3], PURPLE, 0.55)
                px[x, y] = (blend[0], blend[1], blend[2], 255)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/infernal_scripture_array_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m24_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
