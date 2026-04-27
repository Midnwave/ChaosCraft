import base64, random, os
from PIL import Image

# MODEL 8 — Infernal Crown Burst, texture 0
# Tarnished purple metal:
#   dark obsidian-purple base #100818
#   aged metal #2a1840
#   polished purple-silver mid #604890
#   bright silver edge #b090d0
#   specular #e8d8ff
# Emissive: #c080ff on inner-face band edge / spike tip zones.

W = H = 64
img = Image.new("RGBA", (W, H), (0x10, 0x08, 0x18, 255))
px = img.load()

random.seed(80801)

C_VOID    = (0x10, 0x08, 0x18)  # dark obsidian-purple base
C_AGED    = (0x2a, 0x18, 0x40)  # aged metal
C_MID     = (0x60, 0x48, 0x90)  # polished purple-silver mid
C_EDGE    = (0xb0, 0x90, 0xd0)  # bright silver edge
C_SPEC    = (0xe8, 0xd8, 0xff)  # specular highlight
C_EMIS    = (0xc0, 0x80, 0xff)  # emissive purple

# Background tarnish wash — primarily aged metal with random void streaks
for y in range(H):
    for x in range(W):
        r = random.random()
        if r < 0.55:
            c = C_AGED
        elif r < 0.85:
            c = C_VOID
        else:
            c = C_MID
        # Slight per-pixel noise
        nr = random.randint(-6, 6)
        ng = random.randint(-6, 6)
        nb = random.randint(-6, 6)
        px[x, y] = (
            max(0, min(255, c[0] + nr)),
            max(0, min(255, c[1] + ng)),
            max(0, min(255, c[2] + nb)),
            255
        )

# Tarnish streaks (vertical-ish smears of aged-metal color)
random.seed(80802)
for _ in range(18):
    sx = random.randint(0, W - 1)
    sy = random.randint(0, H - 12)
    slen = random.randint(6, 14)
    for k in range(slen):
        yy = sy + k
        if 0 <= yy < H:
            for dx in range(-1, 2):
                xx = (sx + dx) % W
                base = C_VOID if random.random() < 0.5 else C_AGED
                px[xx, yy] = (base[0], base[1], base[2], 255)

# Polished mid-tone band running across (suggests crown band metal)
band_top = 22
band_bot = 42
for y in range(band_top, band_bot):
    for x in range(W):
        r = random.random()
        if r < 0.55:
            c = C_MID
        elif r < 0.85:
            c = C_AGED
        else:
            c = C_EDGE
        nr = random.randint(-4, 4)
        ng = random.randint(-4, 4)
        nb = random.randint(-4, 4)
        px[x, y] = (
            max(0, min(255, c[0] + nr)),
            max(0, min(255, c[1] + ng)),
            max(0, min(255, c[2] + nb)),
            255
        )

# Edge highlight rim — top/bottom of band
for x in range(W):
    px[x, band_top] = C_EDGE + (255,)
    px[x, band_top + 1] = C_EDGE + (255,)
    px[x, band_bot - 1] = C_EDGE + (255,)
    px[x, band_bot - 2] = C_EDGE + (255,)

# Specular flecks scattered across band
random.seed(80803)
for _ in range(40):
    sx = random.randint(0, W - 1)
    sy = random.randint(band_top + 2, band_bot - 3)
    px[sx, sy] = C_SPEC + (255,)
    if random.random() < 0.5:
        px[(sx + 1) % W, sy] = C_SPEC + (255,)

# Emissive inner-band stripes (top + bottom edge of band)
for x in range(W):
    if (x // 2) % 3 == 0:
        px[x, band_top - 1] = C_EMIS + (255,)
        px[x, band_bot] = C_EMIS + (255,)

# Spike tip zones — 4 vertical emissive runs at corners of texture
random.seed(80804)
spike_columns = [4, 18, 34, 50]
for col in spike_columns:
    for yy in range(0, 8):
        if random.random() < 0.7:
            px[col, yy] = C_EMIS + (255,)
        if random.random() < 0.5:
            px[col + 1, yy] = C_SPEC + (255,)
    for yy in range(56, 64):
        if random.random() < 0.7:
            px[col, yy] = C_EMIS + (255,)
        if random.random() < 0.5:
            px[col + 1, yy] = C_SPEC + (255,)

# Tarnish patches across whole image (random dark blotches on band)
random.seed(80805)
for _ in range(14):
    sx = random.randint(2, W - 4)
    sy = random.randint(band_top + 2, band_bot - 4)
    sz = random.randint(2, 4)
    for dy in range(sz):
        for dx in range(sz):
            if random.random() < 0.6:
                px[(sx + dx) % W, (sy + dy) % H] = C_VOID + (255,)

out = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream/infernal_crown_burst_tex.png"
img.save(out)
b64 = base64.b64encode(open(out, 'rb').read()).decode()
open("D:/CC/ChaosCraft/tools/gen_textures/m08v2_b64.txt", "w").write(b64)
print(f"PNG:{os.path.getsize(out)} b64:{len(b64)}")
