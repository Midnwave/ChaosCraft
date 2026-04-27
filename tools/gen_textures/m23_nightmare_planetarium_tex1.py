"""
Texture 1 for nightmare_planetarium: orbital rings / paths / moons.
Space black #030308 base with silver ring detail at 50% radius #606878.
Faint ghostly emissive zone at centre (rows 34-62 col 34-62).
"""
import base64, math, random
from PIL import Image
W=H=64; img=Image.new("RGBA",(W,H),(0,0,0,255)); px=img.load()

def hx(c):
    c=c.lstrip('#')
    return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))

def lerp(a,b,t):
    return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))

rng = random.Random(2324)

SPACE      = hx('#030308')
SPACE_DEEP = hx('#000000')
SILVER     = hx('#606878')
SILVER_BR  = hx('#a0a8b8')
GHOST      = hx('#3848a0')   # subtle emissive blue-ish
ORBIT_DOT  = hx('#909cb0')

# Pre-place stars
stars = [(rng.randint(0,W-1), rng.randint(0,H-1)) for _ in range(30)]

# Fill base
for y in range(H):
    for x in range(W):
        # Slight gradient — deeper in corners
        nx = (x - W/2) / (W/2)
        ny = (y - H/2) / (H/2)
        d = math.sqrt(nx*nx + ny*ny)
        t = min(1.0, d * 0.6)
        base = lerp(SPACE, SPACE_DEEP, t)
        n = rng.random()*4 - 2
        r,g,b = base
        r=max(0,min(255,r+int(n)))
        g=max(0,min(255,g+int(n)))
        b=max(0,min(255,b+int(n*1.2)))
        px[x,y] = (r,g,b,255)

# Silver ring at 50% radius — circular silver highlight band centered at (32,32)
cx, cy = 31.5, 31.5
for y in range(H):
    for x in range(W):
        d = math.sqrt((x-cx)**2 + (y-cy)**2)
        # Inner silver ring at radius ~16
        if 14 < d < 18:
            t = 1 - abs(d - 16) / 2
            cur = px[x,y]
            blend = lerp(cur[:3], SILVER, t * 0.50)
            px[x,y] = (blend[0], blend[1], blend[2], 255)
        # Bright tick marks at cardinal/intercardinal positions at radius 16
        for ang_deg in [0, 45, 90, 135, 180, 225, 270, 315]:
            rad = math.radians(ang_deg)
            tx = cx + 16 * math.cos(rad)
            ty = cy + 16 * math.sin(rad)
            td = math.sqrt((x-tx)**2 + (y-ty)**2)
            if td < 1.5:
                cur = px[x,y]
                blend = lerp(cur[:3], SILVER_BR, max(0, 1-td/1.5) * 0.85)
                px[x,y] = (blend[0], blend[1], blend[2], 255)

# Faint ghostly emissive zone in centre (UV 34-62 area is the emissive)
# Add subtle emissive blue glow + ring detail
for y in range(34, 62):
    for x in range(34, 62):
        # subtle blue tint
        cur = px[x,y]
        blend = lerp(cur[:3], GHOST, 0.10)
        px[x,y] = (blend[0], blend[1], blend[2], 255)

# Add radial spokes inside emissive zone for ring detail
ec = (47.5, 47.5)
for y in range(34, 62):
    for x in range(34, 62):
        dx = x - ec[0]; dy = y - ec[1]
        d = math.sqrt(dx*dx + dy*dy)
        if 10 < d < 13:
            t = 1 - abs(d - 11.5) / 1.5
            cur = px[x,y]
            blend = lerp(cur[:3], SILVER, t * 0.55)
            px[x,y] = (blend[0], blend[1], blend[2], 255)
        if 6 < d < 7.5:
            t = 1 - abs(d - 6.75) / 0.75
            cur = px[x,y]
            blend = lerp(cur[:3], SILVER_BR, t * 0.45)
            px[x,y] = (blend[0], blend[1], blend[2], 255)

# Stars (tiny bright pixels — distant suns)
for (sx, sy) in stars:
    cur = px[sx, sy]
    intensity = rng.uniform(0.45, 0.95)
    blend = lerp(cur[:3], SILVER_BR, intensity)
    px[sx, sy] = (blend[0], blend[1], blend[2], 255)

# A few orbital dots scattered to suggest tracks
for ang_deg in range(0, 360, 18):
    rad = math.radians(ang_deg)
    for r in [11, 22]:
        ox = int(round(cx + r * math.cos(rad)))
        oy = int(round(cy + r * math.sin(rad)))
        if 0 <= ox < W and 0 <= oy < H:
            cur = px[ox, oy]
            blend = lerp(cur[:3], ORBIT_DOT, 0.25)
            px[ox, oy] = (blend[0], blend[1], blend[2], 255)

out="D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_planetarium_tex1.png"
img.save(out)
b64=base64.b64encode(open(out,'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m23_tex1_b64.txt","w").write(b64)
import os; print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
