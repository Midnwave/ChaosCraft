import base64, math, random
from PIL import Image
W=H=64; img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()

def hx(c):
    c=c.lstrip('#')
    return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))

def lerp(a,b,t):
    return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

rng = random.Random(77)

# TONGUE TIP / EMITTER / IMPACT — blood-red radial
# Centre #ff1010 emissive, #901010 mid, #1a0404 outer

cx, cy = W/2, H/2
max_r = math.sqrt(cx*cx + cy*cy)

for y in range(H):
    for x in range(W):
        dx = x - cx
        dy = y - cy
        r = math.sqrt(dx*dx + dy*dy)
        nr = min(1.0, r / (max_r * 0.7))

        # Radial gradient: centre bright, outer dark
        if nr < 0.15:
            base = hx('#ff1010')
        elif nr < 0.45:
            t = (nr - 0.15) / 0.30
            base = lerp(hx('#ff1010'), hx('#901010'), t)
        elif nr < 0.80:
            t = (nr - 0.45) / 0.35
            base = lerp(hx('#901010'), hx('#401008'), t)
        else:
            t = min(1.0, (nr - 0.80) / 0.20)
            base = lerp(hx('#401008'), hx('#1a0404'), t)

        # Radial streaks for blood vein look
        angle = math.atan2(dy, dx)
        streak = math.sin(angle * 8 + rng.uniform(-0.5, 0.5)) * 0.08
        red = max(0, min(255, base[0] + int(streak * 40)))
        green = max(0, min(255, base[1] + int(streak * 10)))
        blue = max(0, min(255, base[2] + int(streak * 5)))

        # Hot emissive core spot
        if nr < 0.08:
            red = min(255, red + 30)
            green = min(255, green + 5)

        # Random bright blood specks near centre
        if nr < 0.3 and rng.random() < 0.04:
            red = min(255, red + rng.randint(20, 60))

        px[x,y] = (red, green, blue, 255)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/devils_tongue_beam_tex1.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m11_tex1_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
