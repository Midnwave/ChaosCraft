"""Texture 0 for nightmare_root_surge — nightmare bark: dark base, sickly yellow-green, toxic highlight, emissive."""
import base64, math, random
from PIL import Image

W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255))
px=img.load()

def hx(c):
    c=c.lstrip('#')
    return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))

def lerp(a,b,t):
    return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

random.seed(99)

very_dark    = hx("0c0e08")
sick_green   = hx("141c08")
sickly_mid   = hx("3a4808")
toxic_yellow = hx("8a9010")
emissive_col = hx("d4c820")
specular_col = hx("e8d830")
deep_black   = hx("030402")

def bark_pixel(x, y, w, h):
    """Generate nightmare bark texture with horizontal banding."""
    nx = x / w
    ny = y / h
    # Horizontal banding: repeating dark/bright bands
    band_freq = 8.0
    band = math.sin(ny * math.pi * band_freq) * 0.5 + 0.5
    # Brightness: bottom=dark(buried), top=brightest yellow-green(surface)
    vert_bright = ny  # 0=top=brightest, 1=bottom=darkest in image
    vert_t = 1.0 - vert_bright  # 0=bottom darkest, 1=top brightest
    # Combine: band modulates the vertical gradient
    combined = vert_t * 0.6 + band * 0.35 + random.random() * 0.05
    combined = max(0.0, min(1.0, combined))
    # Map to palette: deep_black(0) -> very_dark(0.2) -> sick_green(0.4) -> sickly_mid(0.6) -> toxic_yellow(0.82) -> emissive_col(1.0)
    if combined < 0.2:
        t = combined / 0.2
        col = lerp(deep_black, very_dark, t)
    elif combined < 0.4:
        t = (combined - 0.2) / 0.2
        col = lerp(very_dark, sick_green, t)
    elif combined < 0.6:
        t = (combined - 0.4) / 0.2
        col = lerp(sick_green, sickly_mid, t)
    elif combined < 0.82:
        t = (combined - 0.6) / 0.22
        col = lerp(sickly_mid, toxic_yellow, t)
    else:
        t = (combined - 0.82) / 0.18
        col = lerp(toxic_yellow, emissive_col, t)
    return col

# Zone A (x 0-31, y 0-31): primary lit faces — standard bark
for y in range(32):
    for x in range(32):
        col = bark_pixel(x, y, 32, 32)
        # Emissive pixels at brightest band rows
        if y < 6:
            t = (6-y)/6.0
            col = lerp(col, emissive_col, t*0.4)
        px[x,y] = col+(255,)

# Zone B (x 32-63, y 0-31): shadow faces — darker bark
for y in range(32):
    for x in range(32,64):
        col = bark_pixel(x-32, y, 32, 32)
        col = lerp(col, very_dark, 0.45)
        if y < 4:
            t = (4-y)/4.0
            col = lerp(col, sick_green, t*0.3)
        px[x,y] = col+(255,)

# Zone C (x 0-31, y 32-63): top/highlight — brightest, most emissive
for y in range(32,64):
    for x in range(32):
        col = bark_pixel(x, y-32, 32, 32)
        col = lerp(col, toxic_yellow, 0.4)
        # Specular cluster at (8,4) in zone space
        zx, zy = x, y-32
        d = math.sqrt((zx-8)**2+(zy-4)**2)
        if d < 4:
            t = (4-d)/4.0
            col = lerp(col, specular_col, t*0.85)
        px[x,y] = col+(255,)

# Zone D (x 32-63, y 32-63): emissive/bottom — near-black with emissive contamination
for y in range(32,64):
    for x in range(32,64):
        col = bark_pixel(x-32, y-32, 32, 32)
        col = lerp(col, deep_black, 0.7)
        # Emissive yellow glow contamination at certain pixels
        zy = y-32
        if zy < 5:
            t = (5-zy)/5.0
            col = lerp(col, emissive_col, t*0.7)
        px[x,y] = col+(255,)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_root_surge_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m02_b64.txt","w").write(b64)
import os
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
