"""
Texture 1 for nightmare_eye_projectile: IRIS / TENDRILS / ORBITAL GLOW.
Deep purple centre #100830 → bright ring #8050e0 → amber-gold #c0a020 → deep again.
Concentric ring detail. Emissive #d080ff at brightest iris ring.
Tendrils use the outer dark zone.
"""
import base64, math, random
from PIL import Image
W=H=64; img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()

def hx(c):
    c=c.lstrip('#')
    return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))

def lerp(a,b,t):
    return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

rng = random.Random(1414)

IRIS_DEEP   = hx('#100830')
IRIS_RING1  = hx('#8050e0')   # bright purple ring
IRIS_RING2  = hx('#c0a020')   # amber-gold ring
IRIS_OUTER  = hx('#080418')   # outer dark
IRIS_EMISS  = hx('#d080ff')   # emissive
PUPIL       = hx('#000000')

cx, cy = 32, 32

for y in range(H):
    for x in range(W):
        dx = x - cx; dy = y - cy
        d = math.sqrt(dx*dx + dy*dy)
        # angle
        ang = math.atan2(dy, dx)

        # concentric ring zones (radial gradient + ring detail)
        # 0-3: pupil (black)
        # 3-9: deep purple inner
        # 9-14: bright purple ring (emissive)
        # 14-20: amber-gold
        # 20-26: deep purple outer
        # 26-32: outer dark (used by tendrils — outer dark zone)

        if d < 3:
            base = PUPIL
        elif d < 9:
            t = (d - 3) / 6
            base = lerp(PUPIL, IRIS_DEEP, t)
        elif d < 14:
            t = (d - 9) / 5
            # ring brightening — peak at d=11.5
            ring_t = 1 - abs(d - 11.5) / 2.5
            base = lerp(IRIS_DEEP, IRIS_RING1, max(0, ring_t))
        elif d < 20:
            t = (d - 14) / 6
            base = lerp(IRIS_RING1, IRIS_RING2, t)
        elif d < 26:
            t = (d - 20) / 6
            base = lerp(IRIS_RING2, IRIS_DEEP, t)
        else:
            # outer dark zone — used by tendrils
            t = min(1.0, (d - 26) / 6)
            base = lerp(IRIS_DEEP, IRIS_OUTER, t)

        # radial striations (iris fibres)
        striation = math.sin(ang * 24) * 0.08 + math.sin(ang * 60) * 0.04
        # only apply striations in iris zones (3 < d < 26)
        if 3 < d < 26:
            base = lerp(base, IRIS_EMISS if d < 14 else IRIS_DEEP, striation if striation > 0 else 0)

        # ring rim emphasis at d=11.5 (emissive bright ring)
        rim_glow = max(0, 1 - abs(d - 11.5) / 1.0)
        base = lerp(base, IRIS_EMISS, rim_glow * 0.5)

        # subtle noise
        n = rng.random() * 8 - 4
        r = max(0, min(255, base[0] + int(n)))
        g = max(0, min(255, base[1] + int(n*0.8)))
        b = max(0, min(255, base[2] + int(n*1.1)))

        px[x,y] = (r, g, b, 255)

# Inner pupil pure black
for y in range(H):
    for x in range(W):
        d = math.sqrt((x-cx)**2 + (y-cy)**2)
        if d < 2.5:
            px[x,y] = (0, 0, 0, 255)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_eye_projectile_tex1.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m13_tex1_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
