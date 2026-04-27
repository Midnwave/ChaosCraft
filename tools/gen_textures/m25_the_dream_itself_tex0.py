"""Texture for the_dream_itself. ALL-PALETTES CONVERGENCE diagonal.
Top-left zone: bone-white #f0ece0 (feather/wing material).
Bottom-right zone: deep purple-void #0a0420 (sphere/nightmare material).
Diagonal from top-left to bottom-right with blood-red #a01010 band at 40-55% and yellow #a0a010 at 60-70%.
"""
import base64,math
from PIL import Image
W=H=64
img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()
def hx(c): c=c.lstrip('#'); return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

bone_white = hx("#f0ece0")
bone_dark  = hx("#a89d80")
purple_void = hx("#0a0420")
purple_mid  = hx("#200840")
blood_red   = hx("#a01010")
blood_dark  = hx("#400404")
yellow_sick = hx("#a0a010")
yellow_dark = hx("#403c04")
spec        = hx("#ffffff")

# Diagonal from top-left (0,0) to bottom-right (63,63). t ranges 0..1
for y in range(H):
    for x in range(W):
        t = (x + y) / (2 * (W-1))  # 0..1 along anti-diagonal
        # Add subtle perpendicular variation based on (x - y) for surface texture
        perp = (x - y) / (W-1)  # -1..1
        # Choose color based on diagonal zone
        if t < 0.30:
            # Bone-white zone with grain
            grain = ((x*7 + y*3) % 11) / 11.0
            if (x % 5 == 0 or y % 5 == 0) and (x+y) % 7 < 3:
                col = lerp(bone_white, bone_dark, 0.6)
            else:
                col = lerp(bone_white, bone_dark, grain * 0.4)
            # specular highlight in upper-left corner
            if x < 8 and y < 8:
                col = lerp(col, spec, 0.5 * (1 - (x+y)/16.0))
        elif t < 0.40:
            # Transition bone -> blood
            local = (t - 0.30) / 0.10
            col = lerp(bone_dark, blood_red, local)
        elif t < 0.55:
            # Blood-red band
            local = (t - 0.40) / 0.15
            col = lerp(blood_red, blood_dark, abs(local - 0.5) * 2)
            # blood drip detail
            if (x % 4 == 0) and (y % 6 < 3):
                col = lerp(col, blood_dark, 0.5)
        elif t < 0.60:
            # Transition blood -> yellow
            local = (t - 0.55) / 0.05
            col = lerp(blood_red, yellow_sick, local)
        elif t < 0.70:
            # Yellow corruption band
            local = (t - 0.60) / 0.10
            col = lerp(yellow_sick, yellow_dark, abs(local - 0.5) * 2)
            # corruption pock marks
            if ((x*3 + y*5) % 13) < 2:
                col = lerp(col, yellow_dark, 0.6)
        elif t < 0.80:
            # Transition yellow -> purple
            local = (t - 0.70) / 0.10
            col = lerp(yellow_sick, purple_mid, local)
        else:
            # Deep purple void
            local = (t - 0.80) / 0.20
            col = lerp(purple_mid, purple_void, local)
            # cosmic stars
            if ((x * 11 + y * 7) % 23) == 0:
                col = lerp(col, spec, 0.7)
            elif ((x * 13 + y * 17) % 31) == 0:
                col = lerp(col, hx("#c060ff"), 0.5)
        # subtle perpendicular shimmer along diagonal seams
        if abs(perp) > 0.4 and (x + y) % 9 == 0:
            col = lerp(col, spec, 0.15)
        px[x, y] = col + (255,)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/the_dream_itself_tex.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m25_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
