"""Texture 1 for fallen_angel_descent — compressed purple radial: crater floor emissive."""
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

near_black   = hx("060410")
deep_royal   = hx("401880")
bright_violet= hx("8040d0")
ring_purple  = hx("6030b0")
emissive_pur = hx("c080ff")

def radial_purple(cx, cy, x, y, zone_w, zone_h):
    """Generate radial purple gradient from centre."""
    nx = (x - cx) / (zone_w * 0.5)
    ny = (y - cy) / (zone_h * 0.5)
    r = math.sqrt(nx*nx + ny*ny)
    r = min(r, 1.0)
    # Profile: 0 -> near_black, 0.2 -> deep_royal, 0.4 -> bright_violet, 0.6 -> near_black, 1.0 -> near_black
    if r < 0.2:
        t = r / 0.2
        col = lerp(near_black, deep_royal, t)
    elif r < 0.4:
        t = (r - 0.2) / 0.2
        col = lerp(deep_royal, bright_violet, t)
    elif r < 0.6:
        t = (r - 0.4) / 0.2
        col = lerp(bright_violet, deep_royal, t)
    else:
        t = (r - 0.6) / 0.4
        col = lerp(deep_royal, near_black, t)
    # Concentric ring detail at 25% and 55%
    ring_dist = min(abs(r - 0.25), abs(r - 0.55))
    if ring_dist < 0.04:
        ring_t = (0.04 - ring_dist) / 0.04
        col = lerp(col, ring_purple, ring_t * 0.6)
    return col, r

def fill_zone(x_start, x_end, y_start, y_end, is_emissive=False):
    zone_w = x_end - x_start
    zone_h = y_end - y_start
    cx = x_start + zone_w * 0.5
    cy = y_start + zone_h * 0.5
    for y in range(y_start, y_end):
        for x in range(x_start, x_end):
            col, r = radial_purple(cx, cy, x, y, zone_w, zone_h)
            if is_emissive:
                # 4px bright emissive ring at brightest ring area
                if abs(r - 0.4) < 0.08:
                    ring_t = (0.08 - abs(r-0.4)) / 0.08
                    col = lerp(col, emissive_pur, ring_t * 0.95)
                # Extra glow at centre
                if r < 0.08:
                    col = lerp(col, emissive_pur, 0.5)
            px[x,y] = col+(255,)

# All four zones use the radial purple
# Zone A (x 0-31, y 0-31): primary lit — slightly brighter
fill_zone(0, 32, 0, 32, is_emissive=False)
for y in range(32):
    for x in range(32):
        c = px[x,y]
        c = lerp(c[:3], bright_violet, 0.1)
        px[x,y] = c+(255,)

# Zone B (x 32-63, y 0-31): shadow — slightly darker
fill_zone(32, 64, 0, 32, is_emissive=False)
for y in range(32):
    for x in range(32,64):
        c = px[x,y]
        c = lerp(c[:3], near_black, 0.2)
        px[x,y] = c+(255,)

# Zone C (x 0-31, y 32-63): top highlight
fill_zone(0, 32, 32, 64, is_emissive=False)
for y in range(32,64):
    for x in range(32):
        c = px[x,y]
        c = lerp(c[:3], ring_purple, 0.15)
        px[x,y] = c+(255,)

# Zone D (x 32-63, y 32-63): emissive bottom — full emissive glow
fill_zone(32, 64, 32, 64, is_emissive=True)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/fallen_angel_descent_tex1.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m01_tex1_b64.txt","w").write(b64)
import os
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
