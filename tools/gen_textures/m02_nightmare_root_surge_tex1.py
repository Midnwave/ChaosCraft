"""Texture 1 for nightmare_root_surge — ground cracks: near-black with sickly yellow crack lines from centre."""
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

random.seed(77)

near_black  = hx("080a04")
crack_col   = hx("b0a010")
emissive_c  = hx("d4c820")
deep_black  = hx("030401")
mid_dark    = hx("141c04")

def crack_network(cx, cy, x, y, zone_w, zone_h, num_cracks=5):
    """Return intensity of crack network at pixel (x,y) relative to zone centre (cx,cy)."""
    dx = x - cx
    dy = y - cy
    r = math.sqrt(dx*dx + dy*dy)
    max_r = zone_w * 0.5 * math.sqrt(2)
    # 5 crack branches at 72° intervals
    best = 0.0
    for i in range(num_cracks):
        angle = i * (2*math.pi / num_cracks)
        # Crack direction vector
        ca = math.cos(angle)
        sa = math.sin(angle)
        # Project point onto crack direction
        proj = dx * ca + dy * sa
        perp = abs(-dx * sa + dy * ca)
        if proj < 0:
            best = max(best, 0.0)
            continue
        # Crack width: narrow at base, slightly wider at tip
        width = 0.8 + proj / max_r * 0.5
        intensity = max(0.0, 1.0 - perp / width)
        # Fade by distance
        fade = max(0.0, 1.0 - r / (max_r * 0.85))
        intensity *= fade
        # Sub-branches: fork at 50% distance
        mid_proj = max_r * 0.4
        if proj > mid_proj:
            for di in [-0.4, 0.4]:
                sub_angle = angle + di
                sca = math.cos(sub_angle)
                ssa = math.sin(sub_angle)
                sub_dx = x - (cx + mid_proj * ca)
                sub_dy = y - (cy + mid_proj * sa)
                s_proj = sub_dx * sca + sub_dy * ssa
                s_perp = abs(-sub_dx * ssa + sub_dy * sca)
                if s_proj > 0:
                    s_int = max(0.0, 1.0 - s_perp / 0.6) * max(0.0, 1.0 - (proj - mid_proj) / (max_r * 0.4)) * 0.5
                    intensity = max(intensity, s_int)
        best = max(best, intensity)
    # Also a small bright centre
    if r < 2.0:
        best = max(best, 1.0 - r/2.0)
    return best

def zone_fill(x_start, x_end, y_start, y_end, emissive=False):
    zone_w = x_end - x_start
    zone_h = y_end - y_start
    cx = x_start + zone_w * 0.5
    cy = y_start + zone_h * 0.5
    for y in range(y_start, y_end):
        for x in range(x_start, x_end):
            t = crack_network(cx, cy, x, y, zone_w, zone_h)
            if t < 0.01:
                col = near_black
                if random.random() < 0.04:
                    col = mid_dark
            elif t < 0.4:
                frac = t / 0.4
                col = lerp(near_black, crack_col, frac)
            else:
                frac = (t - 0.4) / 0.6
                col = lerp(crack_col, emissive_c, frac)
            if emissive and t > 0.3:
                # Push emissive
                col = lerp(col, emissive_c, 0.6)
            px[x,y] = col+(255,)

# All zones: crack network, emissive only in Zone D
zone_fill(0,  32, 0,  32, emissive=False)
zone_fill(32, 64, 0,  32, emissive=False)
zone_fill(0,  32, 32, 64, emissive=False)
zone_fill(32, 64, 32, 64, emissive=True)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_root_surge_tex1.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m02_tex1_b64.txt","w").write(b64)
import os
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
