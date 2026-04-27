"""Texture for the_dream_itself tex1. Pure deep purple #100428 with bright emissive purple #c060ff at centre,
transitioning through #6020a0 to #200840. MAXIMUM EMISSIVE INTENSITY.
"""
import base64,math
from PIL import Image
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

deep_purple = hx("#100428")
mid_purple  = hx("#200840")
emissive_purple = hx("#c060ff")
trans_purple = hx("#6020a0")
spec = hx("#ffffff")

# Tile both halves with radial pattern. Top-half (0-31) primary, bottom-half (32-63) emissive max.
for y in range(H):
    for x in range(W):
        # determine center per-quadrant
        if x < 32 and y < 32:
            cx, cy, maxr = 16, 16, 22
        elif x >= 32 and y < 32:
            cx, cy, maxr = 47, 16, 22
        elif x < 32 and y >= 32:
            cx, cy, maxr = 16, 47, 22
        else:
            cx, cy, maxr = 47, 47, 22
        r = math.sqrt((x-cx)**2 + (y-cy)**2) / maxr
        r = min(r, 1.0)
        # Radial: emissive_purple at center -> trans_purple at mid -> mid_purple -> deep_purple
        if r < 0.15:
            col = emissive_purple
            # core spec
            if (x % 4 == 0 and y % 4 == 0):
                col = lerp(col, spec, 0.4)
        elif r < 0.40:
            local = (r - 0.15) / 0.25
            col = lerp(emissive_purple, trans_purple, local)
        elif r < 0.70:
            local = (r - 0.40) / 0.30
            col = lerp(trans_purple, mid_purple, local)
        else:
            local = (r - 0.70) / 0.30
            col = lerp(mid_purple, deep_purple, local)
        # Add radial light streaks
        ang = math.atan2(y-cy, x-cx)
        streak = abs(math.sin(ang*8))
        if streak > 0.92 and r < 0.7:
            col = lerp(col, emissive_purple, 0.4)
        # Add purple sparkles
        if ((x * 13 + y * 7) % 29) == 0:
            col = lerp(col, emissive_purple, 0.6)
        # Bottom-right quadrant: extra brightness boost
        if x >= 32 and y >= 32:
            col = lerp(col, emissive_purple, 0.15)
        px[x, y] = col + (255,)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/the_dream_itself_tex1.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m25_tex1_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
