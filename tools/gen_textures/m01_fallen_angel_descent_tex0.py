"""Texture 0 for fallen_angel_descent — bone surface: pale off-white base, deep shadow grey, charred impact, bright bone peak, specular."""
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

random.seed(42)

pale_base   = hx("d8d0c8")
shadow_grey = hx("404038")
charred     = hx("181412")
bone_peak   = hx("f0ece0")
specular    = hx("ffffff")
emissive_pu = hx("d8c8ff")

def bone_noise(x, y, w, h):
    """Generate bone surface pixel."""
    nx = x / w
    ny = y / h
    # Vertical brightness gradient: bottom=charred, top=bone peak
    vert = ny  # 0=top bright, 1=bottom dark in image space
    base_t = 1.0 - vert
    # Add noise patches for irregular brightness
    noise1 = math.sin(x*0.7+3.1)*math.cos(y*0.9+1.2)*0.3+0.5
    noise2 = math.sin(x*1.3+y*0.5)*0.2+0.5
    noise3 = math.cos(x*0.4+y*1.1+2.3)*0.15+0.5
    combined = (noise1*0.5 + noise2*0.3 + noise3*0.2)
    # Random scatter patches
    r = random.random()*0.1
    brightness = base_t * 0.7 + combined * 0.25 + r
    brightness = max(0.0, min(1.0, brightness))
    # Interpolate: charred(0) -> shadow_grey(0.25) -> pale_base(0.55) -> bone_peak(0.85) -> specular(1.0)
    if brightness < 0.25:
        t = brightness / 0.25
        col = lerp(charred, shadow_grey, t)
    elif brightness < 0.55:
        t = (brightness - 0.25) / 0.30
        col = lerp(shadow_grey, pale_base, t)
    elif brightness < 0.85:
        t = (brightness - 0.55) / 0.30
        col = lerp(pale_base, bone_peak, t)
    else:
        t = (brightness - 0.85) / 0.15
        col = lerp(bone_peak, specular, t)
    return col

# Zone A (top-left, x 0-31, y 0-31): primary lit faces — bright bone surface
for y in range(32):
    for x in range(32):
        col = bone_noise(x, y, 32, 32)
        # Top rows (y<4): force bright bone peak
        if y < 4:
            t = (4-y)/4.0
            col = lerp(col, bone_peak, t*0.6)
        # Bottom rows (y>27): force charred dark
        if y > 27:
            t = (y-27)/4.0
            col = lerp(col, charred, t*0.7)
        # Emissive purple-white flecks in deep crevice areas (shadow_grey-like zones)
        if col[0] < 80 and col[1] < 80 and random.random() < 0.08:
            col = lerp(col, emissive_pu, 0.4)
        px[x,y] = col+(255,)

# Zone B (top-right, x 32-63, y 0-31): shadow faces — darker, more grey
for y in range(32):
    for x in range(32,64):
        col = bone_noise(x-32, y, 32, 32)
        # Darken by 35%
        col = lerp(col, shadow_grey, 0.35)
        if y > 25:
            t = (y-25)/6.0
            col = lerp(col, charred, t*0.8)
        if col[0] < 70 and random.random() < 0.06:
            col = lerp(col, emissive_pu, 0.3)
        px[x,y] = col+(255,)

# Zone C (bottom-left, x 0-31, y 32-63): top/highlight — very bright exposed bone
for y in range(32,64):
    for x in range(32):
        col = bone_noise(x, y-32, 32, 32)
        # Push very bright — freshly exposed bone top
        col = lerp(col, bone_peak, 0.5)
        # Add specular cluster around (8, 4) in zone space
        zx, zy = x, y-32
        d = math.sqrt((zx-8)**2+(zy-4)**2)
        if d < 5:
            t = (5-d)/5.0
            col = lerp(col, specular, t*0.7)
        px[x,y] = col+(255,)

# Zone D (bottom-right, x 32-63, y 32-63): emissive/bottom — charred with purple glow
for y in range(32,64):
    for x in range(32,64):
        col = bone_noise(x-32, y-32, 32, 32)
        # Dark charred base
        col = lerp(col, charred, 0.65)
        # Faint purple-white emissive contamination
        t = random.random()*0.3
        col = lerp(col, emissive_pu, t)
        # 3px bright purple ring at deepest crevice
        if 14 <= x-32 <= 17 and 14 <= y-32 <= 17:
            col = lerp(col, emissive_pu, 0.9)
        px[x,y] = col+(255,)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/fallen_angel_descent_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m01_b64.txt","w").write(b64)
import os
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
