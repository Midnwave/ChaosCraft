#!/usr/bin/env python3
"""
generate_badge_textures.py
Generates 64x64 pixel art badge textures for ChaosCraft.

Each badge gets:
  - {id}.png           (full color)
  - {id}_gray.png      (grayscale)
  - question_mark.png  (shared "?" icon)

Usage:
    pip install Pillow
    python generate_badge_textures.py
"""

import os
import math
from PIL import Image, ImageDraw

OUTPUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "badge_textures")
SIZE = 64

# ---------------------------------------------------------------------------
# Helper utilities
# ---------------------------------------------------------------------------

def new_canvas(bg=(0, 0, 0, 0)):
    """Return a fresh 64x64 RGBA image with optional background."""
    return Image.new("RGBA", (SIZE, SIZE), bg)


def add_border(img, color=(20, 20, 20, 255)):
    """Draw a 1-pixel dark border around the canvas."""
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, SIZE - 1, SIZE - 1], outline=color)
    return img


def fill_bg(img, color):
    """Fill the entire image with a solid colour, then return draw context."""
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, SIZE - 1, SIZE - 1], fill=color)
    return d


def to_grayscale(img):
    """Convert RGBA image to grayscale (keeping alpha)."""
    return img.convert("LA").convert("RGBA")


def save_badge(img, badge_id):
    """Save coloured + grayscale variants and return count of files saved."""
    add_border(img)
    color_path = os.path.join(OUTPUT_DIR, f"{badge_id}.png")
    gray_path = os.path.join(OUTPUT_DIR, f"{badge_id}_gray.png")
    img.save(color_path)
    to_grayscale(img).save(gray_path)
    return 2


def draw_circle(d, cx, cy, r, fill):
    """Draw a filled circle centred at (cx, cy) with radius r."""
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=fill)


def draw_pixel_rect(d, x, y, w, h, fill):
    """Draw a filled rectangle."""
    d.rectangle([x, y, x + w - 1, y + h - 1], fill=fill)


def draw_star(d, cx, cy, r_outer, r_inner, points, fill):
    """Draw a star polygon."""
    coords = []
    for i in range(points * 2):
        angle = math.pi / 2 + i * math.pi / points
        r = r_outer if i % 2 == 0 else r_inner
        coords.append((cx + r * math.cos(angle), cy - r * math.sin(angle)))
    d.polygon(coords, fill=fill)


# ---------------------------------------------------------------------------
# Badge drawing functions
# ---------------------------------------------------------------------------

def draw_calamity_event_1():
    img = new_canvas()
    d = fill_bg(img, (30, 10, 10, 255))
    # Flame shape - orange/red
    flame_colors = [(255, 100, 0, 255), (255, 60, 0, 255), (255, 180, 0, 255)]
    # Outer flame
    d.polygon([(32, 6), (48, 28), (44, 50), (38, 56), (26, 56), (20, 50), (16, 28)],
              fill=(255, 100, 0, 255))
    # Inner flame
    d.polygon([(32, 12), (42, 28), (40, 44), (36, 50), (28, 50), (24, 44), (22, 28)],
              fill=(255, 60, 0, 255))
    # Hot core
    d.polygon([(32, 18), (38, 30), (36, 40), (34, 44), (30, 44), (28, 40), (26, 30)],
              fill=(255, 180, 0, 255))
    # Skull silhouette inside
    d.ellipse([26, 24, 38, 36], fill=(40, 0, 0, 255))  # skull head
    draw_pixel_rect(d, 28, 26, 3, 3, (20, 0, 0, 255))  # left eye
    draw_pixel_rect(d, 33, 26, 3, 3, (20, 0, 0, 255))  # right eye
    draw_pixel_rect(d, 30, 32, 4, 2, (20, 0, 0, 255))  # mouth
    draw_pixel_rect(d, 30, 36, 4, 6, (40, 0, 0, 255))  # jaw
    return img


def draw_calamity_event_2():
    img = new_canvas()
    d = fill_bg(img, (20, 5, 30, 255))
    # Purple/dark flame
    d.polygon([(32, 4), (50, 26), (46, 50), (40, 58), (24, 58), (18, 50), (14, 26)],
              fill=(120, 30, 160, 255))
    d.polygon([(32, 10), (44, 26), (42, 46), (38, 52), (26, 52), (22, 46), (20, 26)],
              fill=(80, 10, 120, 255))
    d.polygon([(32, 16), (38, 28), (36, 42), (34, 46), (30, 46), (28, 42), (26, 28)],
              fill=(60, 0, 90, 255))
    # Eye in center
    d.ellipse([24, 26, 40, 38], fill=(255, 255, 255, 255))
    d.ellipse([28, 28, 36, 36], fill=(180, 0, 255, 255))
    d.ellipse([30, 30, 34, 34], fill=(0, 0, 0, 255))
    # Glint
    draw_pixel_rect(d, 31, 30, 2, 2, (255, 255, 255, 255))
    return img


def draw_1x1x1x1():
    img = new_canvas()
    d = fill_bg(img, (10, 0, 0, 255))
    # Glitchy grid of "1"s
    ones_color = [(255, 0, 0, 255), (200, 0, 0, 255), (160, 0, 0, 255), (255, 40, 40, 255)]
    glitch_offsets = [(0, 0), (1, 0), (-1, 0), (0, 1)]
    for row in range(4):
        for col in range(4):
            x = 6 + col * 14
            y = 6 + row * 14
            c = ones_color[(row + col) % len(ones_color)]
            ox, oy = glitch_offsets[(row * 4 + col) % len(glitch_offsets)]
            # Draw a pixel-art "1"
            draw_pixel_rect(d, x + 4 + ox, y + oy, 3, 2, c)
            draw_pixel_rect(d, x + 2 + ox, y + 2 + oy, 2, 2, c)
            draw_pixel_rect(d, x + 4 + ox, y + 2 + oy, 3, 8, c)
            draw_pixel_rect(d, x + 1 + ox, y + 10 + oy, 9, 2, c)
    # Glitch scan lines
    for y in range(0, 64, 8):
        d.rectangle([0, y, 63, y], fill=(0, 0, 0, 80))
    return img


def draw_blue_moon_event_2023():
    img = new_canvas()
    d = fill_bg(img, (5, 5, 30, 255))
    # Crescent moon
    d.ellipse([16, 10, 48, 54], fill=(80, 130, 255, 255))
    d.ellipse([24, 8, 52, 52], fill=(5, 5, 30, 255))  # cut out for crescent
    # Stars
    star_positions = [(10, 12), (50, 8), (8, 40), (52, 44), (14, 56), (48, 56), (30, 4)]
    for sx, sy in star_positions:
        draw_pixel_rect(d, sx, sy, 2, 2, (200, 220, 255, 255))
        draw_pixel_rect(d, sx - 1, sy + 1, 1, 1, (150, 180, 255, 180))
        draw_pixel_rect(d, sx + 2, sy + 1, 1, 1, (150, 180, 255, 180))
    return img


def draw_hallows_2023():
    img = new_canvas()
    d = fill_bg(img, (30, 15, 0, 255))
    # Pumpkin body
    d.ellipse([10, 14, 54, 54], fill=(230, 130, 0, 255))
    d.ellipse([14, 16, 50, 52], fill=(255, 150, 20, 255))
    # Stem
    draw_pixel_rect(d, 28, 8, 8, 8, (60, 120, 30, 255))
    draw_pixel_rect(d, 30, 6, 4, 4, (50, 100, 20, 255))
    # Jack-o-lantern face
    # Eyes - triangles
    d.polygon([(22, 26), (30, 26), (26, 34)], fill=(255, 220, 50, 255))
    d.polygon([(34, 26), (42, 26), (38, 34)], fill=(255, 220, 50, 255))
    # Mouth - jagged
    d.polygon([(18, 38), (46, 38), (44, 48), (38, 42), (32, 48), (26, 42), (20, 48)],
              fill=(255, 220, 50, 255))
    return img


def draw_hallows_2024():
    img = new_canvas()
    d = fill_bg(img, (15, 5, 25, 255))
    # Haunted house silhouette
    house_color = (40, 10, 50, 255)
    # Main body
    draw_pixel_rect(d, 14, 30, 36, 26, house_color)
    # Roof triangle
    d.polygon([(12, 30), (52, 30), (32, 14)], fill=house_color)
    # Tower
    draw_pixel_rect(d, 38, 16, 10, 20, house_color)
    draw_pixel_rect(d, 36, 14, 14, 4, house_color)
    # Windows (glowing yellow)
    win = (255, 220, 80, 255)
    draw_pixel_rect(d, 20, 36, 6, 6, win)
    draw_pixel_rect(d, 38, 36, 6, 6, win)
    draw_pixel_rect(d, 28, 46, 8, 10, win)  # door
    draw_pixel_rect(d, 40, 20, 6, 6, win)  # tower window
    # Lightning bolt
    d.polygon([(50, 2), (46, 16), (50, 16), (44, 28), (48, 28), (42, 14), (46, 14)],
              fill=(255, 255, 100, 255))
    # Lightning bolt right side
    d.polygon([(8, 4), (12, 4), (10, 12), (14, 12), (8, 22), (10, 14), (6, 14)],
              fill=(200, 200, 80, 200))
    return img


def draw_anniversary_2024():
    img = new_canvas()
    d = fill_bg(img, (30, 20, 10, 255))
    # Gold party hat
    d.polygon([(32, 6), (46, 50), (18, 50)], fill=(255, 200, 0, 255))
    d.polygon([(32, 8), (44, 48), (20, 48)], fill=(255, 220, 50, 255))
    # Stripes on hat
    for i, y in enumerate(range(16, 48, 8)):
        stripe_color = (255, 100, 50, 255) if i % 2 == 0 else (50, 150, 255, 255)
        w = int((y - 6) * 0.5)
        draw_pixel_rect(d, 32 - w, y, w * 2, 3, stripe_color)
    # Hat brim
    draw_pixel_rect(d, 12, 48, 40, 4, (255, 200, 0, 255))
    # Star on top
    draw_star(d, 32, 8, 5, 2, 5, (255, 255, 200, 255))
    # Confetti
    confetti_colors = [(255, 50, 50, 255), (50, 255, 50, 255), (50, 100, 255, 255),
                       (255, 255, 50, 255), (255, 50, 255, 255)]
    confetti_pos = [(8, 12), (52, 10), (6, 30), (56, 28), (10, 50), (54, 48),
                    (4, 20), (58, 38), (12, 42), (50, 54)]
    for i, (cx, cy) in enumerate(confetti_pos):
        draw_pixel_rect(d, cx, cy, 3, 3, confetti_colors[i % len(confetti_colors)])
    return img


def draw_april_fools_2025():
    img = new_canvas()
    d = fill_bg(img, (20, 15, 30, 255))
    # Rainbow jester hat - three points
    rainbow = [(255, 0, 0, 255), (0, 200, 0, 255), (80, 80, 255, 255)]
    # Left point
    d.polygon([(6, 42), (22, 42), (8, 12)], fill=rainbow[0])
    # Middle point
    d.polygon([(20, 42), (44, 42), (32, 6)], fill=rainbow[1])
    # Right point
    d.polygon([(42, 42), (58, 42), (56, 12)], fill=rainbow[2])
    # Hat brim - curve
    d.ellipse([4, 38, 60, 52], fill=(255, 220, 0, 255))
    d.ellipse([6, 36, 58, 46], fill=(255, 200, 0, 255))
    # Bells on tips
    bell = (255, 220, 50, 255)
    draw_circle(d, 8, 12, 4, bell)
    draw_circle(d, 32, 6, 4, bell)
    draw_circle(d, 56, 12, 4, bell)
    # Diamond pattern on hat
    for x in range(12, 56, 8):
        draw_pixel_rect(d, x, 34, 3, 3, (255, 255, 255, 200))
    # Face area below brim
    draw_pixel_rect(d, 16, 48, 32, 12, (20, 15, 30, 255))
    # Smile
    d.arc([22, 48, 42, 60], 0, 180, fill=(255, 255, 100, 255), width=2)
    return img


def draw_tutorial():
    img = new_canvas()
    d = fill_bg(img, (10, 30, 15, 255))
    # Green book
    book_green = (40, 160, 60, 255)
    book_dark = (30, 120, 40, 255)
    # Book cover
    draw_pixel_rect(d, 12, 12, 40, 44, book_green)
    draw_pixel_rect(d, 14, 14, 36, 40, book_dark)
    # Spine
    draw_pixel_rect(d, 12, 12, 4, 44, (20, 90, 30, 255))
    # Pages
    draw_pixel_rect(d, 16, 14, 34, 40, (240, 230, 200, 255))
    draw_pixel_rect(d, 18, 16, 30, 36, (255, 245, 220, 255))
    # Text lines on page
    for y in range(18, 48, 4):
        w = 24 if y % 8 == 2 else 20
        draw_pixel_rect(d, 20, y, w, 2, (100, 90, 70, 255))
    # Glow effect around book
    glow = (100, 255, 120, 60)
    d.rectangle([8, 8, 55, 59], outline=glow)
    d.rectangle([9, 9, 54, 58], outline=(80, 255, 100, 40))
    # Star/sparkle on cover
    draw_star(d, 50, 14, 4, 2, 4, (200, 255, 200, 255))
    return img


def draw_freezing_ice():
    img = new_canvas()
    d = fill_bg(img, (10, 15, 40, 255))
    # Snowflake crystal
    center = 32
    arms = 6
    crystal_color = (150, 210, 255, 255)
    crystal_bright = (200, 240, 255, 255)
    crystal_core = (255, 255, 255, 255)
    for i in range(arms):
        angle = i * math.pi / 3
        # Main arm
        for dist in range(4, 24, 2):
            x = int(center + dist * math.cos(angle))
            y = int(center + dist * math.sin(angle))
            c = crystal_bright if dist % 4 == 0 else crystal_color
            draw_pixel_rect(d, x - 1, y - 1, 3, 3, c)
        # Branch at midpoint
        branch_angle1 = angle + math.pi / 6
        branch_angle2 = angle - math.pi / 6
        for dist in range(2, 10, 2):
            bx1 = int(center + 12 * math.cos(angle) + dist * math.cos(branch_angle1))
            by1 = int(center + 12 * math.sin(angle) + dist * math.sin(branch_angle1))
            bx2 = int(center + 12 * math.cos(angle) + dist * math.cos(branch_angle2))
            by2 = int(center + 12 * math.sin(angle) + dist * math.sin(branch_angle2))
            draw_pixel_rect(d, bx1, by1, 2, 2, crystal_color)
            draw_pixel_rect(d, bx2, by2, 2, 2, crystal_color)
    # Center dot
    draw_circle(d, center, center, 3, crystal_core)
    return img


def draw_blue_moon():
    img = new_canvas()
    d = fill_bg(img, (5, 5, 25, 255))
    # Full blue moon
    d.ellipse([12, 10, 52, 50], fill=(60, 100, 200, 255))
    d.ellipse([14, 12, 50, 48], fill=(80, 130, 230, 255))
    # Craters
    d.ellipse([20, 20, 26, 26], fill=(60, 100, 190, 255))
    d.ellipse([34, 16, 40, 22], fill=(55, 95, 185, 255))
    d.ellipse([28, 34, 36, 40], fill=(65, 105, 195, 255))
    d.ellipse([38, 32, 44, 38], fill=(55, 90, 180, 255))
    # Aurora wisps
    wisp_colors = [(0, 255, 150, 80), (0, 200, 255, 60), (100, 255, 200, 70)]
    d.arc([2, 48, 62, 64], 200, 340, fill=(0, 255, 150, 120), width=2)
    d.arc([0, 52, 64, 68], 200, 340, fill=(0, 200, 255, 100), width=2)
    d.arc([4, 44, 60, 62], 210, 330, fill=(100, 255, 200, 80), width=2)
    # Glow around moon
    d.ellipse([10, 8, 54, 52], outline=(100, 160, 255, 80))
    return img


def draw_fish():
    img = new_canvas()
    d = fill_bg(img, (10, 40, 60, 255))
    # Clownfish body - orange
    d.ellipse([12, 20, 48, 46], fill=(255, 130, 0, 255))
    # White stripes
    draw_pixel_rect(d, 22, 18, 4, 30, (255, 255, 255, 255))
    draw_pixel_rect(d, 34, 20, 4, 26, (255, 255, 255, 255))
    # Black outlines on stripes
    draw_pixel_rect(d, 21, 18, 1, 30, (0, 0, 0, 255))
    draw_pixel_rect(d, 26, 18, 1, 30, (0, 0, 0, 255))
    draw_pixel_rect(d, 33, 20, 1, 26, (0, 0, 0, 255))
    draw_pixel_rect(d, 38, 20, 1, 26, (0, 0, 0, 255))
    # Tail
    d.polygon([(46, 28), (58, 20), (58, 44), (46, 38)], fill=(255, 140, 0, 255))
    draw_pixel_rect(d, 50, 22, 2, 20, (255, 255, 255, 200))
    # Eye
    d.ellipse([16, 28, 22, 34], fill=(255, 255, 255, 255))
    d.ellipse([18, 29, 22, 33], fill=(0, 0, 0, 255))
    # Fin on top
    d.polygon([(26, 22), (34, 22), (30, 14)], fill=(255, 100, 0, 255))
    # Bubbles
    d.ellipse([6, 10, 12, 16], outline=(150, 220, 255, 180))
    d.ellipse([8, 4, 12, 8], outline=(150, 220, 255, 150))
    d.ellipse([2, 14, 6, 18], outline=(150, 220, 255, 120))
    return img


def draw_sonic():
    img = new_canvas()
    d = fill_bg(img, (10, 10, 30, 255))
    blue = (30, 80, 220, 255)
    dark_blue = (20, 50, 160, 255)
    skin = (255, 200, 140, 255)
    # Head circle
    d.ellipse([18, 14, 48, 44], fill=blue)
    # Spikes (back of head)
    d.polygon([(38, 16), (58, 8), (50, 22)], fill=dark_blue)
    d.polygon([(42, 22), (60, 18), (54, 30)], fill=dark_blue)
    d.polygon([(44, 30), (62, 26), (56, 38)], fill=dark_blue)
    # Face/skin area
    d.ellipse([22, 24, 40, 42], fill=skin)
    # Eye
    d.ellipse([26, 24, 38, 36], fill=(255, 255, 255, 255))
    d.ellipse([30, 26, 38, 34], fill=(0, 120, 0, 255))
    d.ellipse([32, 28, 36, 32], fill=(0, 0, 0, 255))
    # Speed lines
    speed_color = (100, 180, 255, 150)
    for y_off in range(-8, 12, 4):
        draw_pixel_rect(d, 2, 28 + y_off, 12, 2, speed_color)
    return img


def draw_infested():
    img = new_canvas()
    d = fill_bg(img, (10, 20, 10, 255))
    green = (50, 180, 30, 255)
    dark_green = (30, 120, 15, 255)
    # Bug body
    d.ellipse([20, 22, 44, 50], fill=green)  # abdomen
    d.ellipse([24, 14, 40, 30], fill=dark_green)  # thorax
    d.ellipse([28, 8, 36, 18], fill=green)  # head
    # Eyes
    d.ellipse([28, 9, 32, 14], fill=(255, 50, 50, 255))
    d.ellipse([32, 9, 36, 14], fill=(255, 50, 50, 255))
    # Mandibles
    d.line([(29, 16), (24, 22)], fill=(200, 200, 100, 255), width=2)
    d.line([(35, 16), (40, 22)], fill=(200, 200, 100, 255), width=2)
    # Legs
    for side in [-1, 1]:
        for i, y in enumerate([24, 32, 40]):
            x_start = 32 + side * 10
            x_end = 32 + side * 24
            d.line([(x_start, y), (x_end, y - 4 + i * 2)], fill=dark_green, width=2)
    # Spots on abdomen
    d.ellipse([26, 30, 30, 34], fill=(30, 140, 10, 255))
    d.ellipse([34, 32, 38, 36], fill=(30, 140, 10, 255))
    d.ellipse([28, 40, 32, 44], fill=(30, 140, 10, 255))
    return img


def draw_chef():
    img = new_canvas()
    d = fill_bg(img, (40, 25, 15, 255))
    # Chef hat - tall white toque
    d.ellipse([14, 2, 50, 24], fill=(255, 255, 255, 255))
    d.ellipse([16, 0, 48, 18], fill=(250, 250, 250, 255))
    draw_pixel_rect(d, 18, 14, 28, 14, (255, 255, 255, 255))
    # Hat band
    draw_pixel_rect(d, 18, 26, 28, 3, (200, 200, 200, 255))
    # Golden fork and knife crossed
    fork_color = (220, 180, 50, 255)
    knife_color = (200, 170, 40, 255)
    # Fork (left to right diagonal)
    d.line([(16, 56), (48, 32)], fill=fork_color, width=3)
    # Fork prongs
    d.line([(46, 32), (50, 28)], fill=fork_color, width=2)
    d.line([(46, 32), (52, 30)], fill=fork_color, width=2)
    d.line([(46, 32), (52, 34)], fill=fork_color, width=2)
    # Knife (right to left diagonal)
    d.line([(48, 56), (16, 32)], fill=knife_color, width=3)
    # Knife blade
    d.polygon([(16, 32), (12, 28), (18, 30)], fill=(220, 220, 220, 255))
    return img


def draw_devils_dream():
    img = new_canvas()
    d = fill_bg(img, (30, 5, 10, 255))
    red = (220, 30, 30, 255)
    dark_red = (160, 20, 20, 255)
    # Demon horns
    # Left horn
    d.polygon([(10, 38), (18, 38), (6, 8), (12, 10)], fill=red)
    d.polygon([(10, 38), (6, 8), (8, 6)], fill=dark_red)
    # Right horn
    d.polygon([(46, 38), (54, 38), (58, 8), (52, 10)], fill=red)
    d.polygon([(54, 38), (58, 8), (56, 6)], fill=dark_red)
    # Purple eye between horns
    d.ellipse([22, 24, 42, 44], fill=(120, 0, 180, 255))
    d.ellipse([26, 28, 38, 40], fill=(180, 50, 255, 255))
    d.ellipse([29, 31, 35, 37], fill=(0, 0, 0, 255))
    # Eye shine
    draw_pixel_rect(d, 30, 31, 2, 2, (255, 200, 255, 255))
    # Glow below eye
    d.ellipse([24, 42, 40, 50], fill=(100, 0, 140, 80))
    return img


def draw_corrupted_corruption():
    img = new_canvas()
    d = fill_bg(img, (10, 0, 15, 255))
    # Void spiral
    for i in range(200):
        angle = i * 0.15
        r = 2 + i * 0.12
        x = int(32 + r * math.cos(angle))
        y = int(32 + r * math.sin(angle))
        if 0 <= x < 64 and 0 <= y < 64:
            intensity = max(0, 255 - i)
            c = (intensity // 2, 0, intensity, 255)
            draw_pixel_rect(d, x, y, 2, 2, c)
    # Glitch artifacts
    glitch_colors = [(255, 0, 255, 180), (0, 255, 255, 150), (255, 0, 0, 120)]
    import random
    rng = random.Random(42)  # deterministic
    for _ in range(15):
        gx = rng.randint(0, 60)
        gy = rng.randint(0, 60)
        gw = rng.randint(2, 8)
        gh = rng.randint(1, 3)
        gc = glitch_colors[rng.randint(0, 2)]
        draw_pixel_rect(d, gx, gy, gw, gh, gc)
    # Center void
    draw_circle(d, 32, 32, 5, (0, 0, 0, 255))
    draw_circle(d, 32, 32, 3, (40, 0, 60, 255))
    return img


def draw_doom():
    img = new_canvas()
    d = fill_bg(img, (25, 5, 5, 255))
    red = (200, 20, 20, 255)
    bright_red = (255, 50, 30, 255)
    # Pentagram
    points = []
    for i in range(5):
        angle = -math.pi / 2 + i * 2 * math.pi / 5
        points.append((int(32 + 22 * math.cos(angle)), int(32 + 22 * math.sin(angle))))
    # Draw star lines (pentagram = connect every other point)
    star_order = [0, 2, 4, 1, 3, 0]
    for i in range(len(star_order) - 1):
        p1 = points[star_order[i]]
        p2 = points[star_order[i + 1]]
        d.line([p1, p2], fill=red, width=2)
    # Circle around pentagram
    d.ellipse([8, 8, 56, 56], outline=bright_red)
    d.ellipse([9, 9, 55, 55], outline=red)
    # Flames at bottom
    for fx in range(8, 56, 6):
        flame_h = 8 + (fx % 12)
        d.polygon([(fx, 58), (fx + 4, 58), (fx + 2, 58 - flame_h)],
                  fill=(255, 100 + fx % 80, 0, 200))
    # Center glow
    draw_circle(d, 32, 32, 4, (255, 80, 30, 200))
    return img


def draw_chain():
    img = new_canvas()
    d = fill_bg(img, (20, 20, 25, 255))
    silver = (180, 190, 200, 255)
    dark_silver = (120, 130, 140, 255)
    highlight = (220, 225, 230, 255)
    # Chain links in a circle - 6 links
    for i in range(6):
        angle = i * math.pi / 3
        cx = int(32 + 16 * math.cos(angle))
        cy = int(32 + 16 * math.sin(angle))
        # Each link is a small oval
        w, h = 10, 6
        cos_a = math.cos(angle + math.pi / 2)
        sin_a = math.sin(angle + math.pi / 2)
        d.ellipse([cx - w // 2, cy - h // 2, cx + w // 2, cy + h // 2],
                  outline=silver, width=2)
        d.ellipse([cx - w // 2 + 1, cy - h // 2 + 1, cx + w // 2 - 1, cy + h // 2 - 1],
                  outline=dark_silver)
    # Connecting chain between links
    for i in range(6):
        angle1 = i * math.pi / 3
        angle2 = (i + 1) * math.pi / 3
        x1 = int(32 + 16 * math.cos(angle1))
        y1 = int(32 + 16 * math.sin(angle1))
        x2 = int(32 + 16 * math.cos(angle2))
        y2 = int(32 + 16 * math.sin(angle2))
        d.line([(x1, y1), (x2, y2)], fill=dark_silver, width=2)
    # Center emblem
    draw_circle(d, 32, 32, 6, silver)
    draw_circle(d, 32, 32, 4, dark_silver)
    draw_circle(d, 32, 32, 2, highlight)
    return img


def draw_creeper_infestation():
    img = new_canvas()
    d = fill_bg(img, (15, 30, 10, 255))
    green = (70, 200, 70, 255)
    dark_green = (40, 140, 40, 255)
    # Creeper face
    draw_pixel_rect(d, 12, 10, 40, 44, green)
    draw_pixel_rect(d, 14, 12, 36, 40, (80, 210, 80, 255))
    # Eyes
    draw_pixel_rect(d, 18, 18, 10, 10, (0, 0, 0, 255))
    draw_pixel_rect(d, 36, 18, 10, 10, (0, 0, 0, 255))
    # Mouth
    draw_pixel_rect(d, 26, 30, 12, 6, (0, 0, 0, 255))
    draw_pixel_rect(d, 22, 36, 8, 10, (0, 0, 0, 255))
    draw_pixel_rect(d, 34, 36, 8, 10, (0, 0, 0, 255))
    # Cracks spreading outward
    crack_color = (30, 30, 30, 200)
    # Cracks from edges
    d.line([(12, 10), (6, 4)], fill=crack_color, width=2)
    d.line([(52, 10), (58, 2)], fill=crack_color, width=2)
    d.line([(12, 54), (4, 60)], fill=crack_color, width=2)
    d.line([(52, 54), (60, 62)], fill=crack_color, width=2)
    d.line([(8, 30), (2, 32)], fill=crack_color, width=2)
    d.line([(56, 30), (62, 28)], fill=crack_color, width=2)
    # More cracks on face
    d.line([(28, 18), (26, 12)], fill=crack_color, width=1)
    d.line([(46, 22), (52, 20)], fill=crack_color, width=1)
    d.line([(30, 46), (28, 52)], fill=crack_color, width=1)
    return img


def draw_unpredictable_randomness():
    img = new_canvas()
    d = fill_bg(img, (15, 15, 20, 255))
    # Rainbow dice
    dice_colors = [(255, 60, 60, 255), (60, 200, 60, 255), (60, 60, 255, 255)]
    # Die 1 - red, top left
    draw_pixel_rect(d, 4, 4, 22, 22, dice_colors[0])
    draw_pixel_rect(d, 6, 6, 18, 18, (200, 40, 40, 255))
    # Pips: showing 3
    draw_pixel_rect(d, 9, 9, 3, 3, (255, 255, 255, 255))
    draw_pixel_rect(d, 14, 14, 3, 3, (255, 255, 255, 255))
    draw_pixel_rect(d, 19, 19, 3, 3, (255, 255, 255, 255))
    # Die 2 - green, top right
    draw_pixel_rect(d, 30, 6, 22, 22, dice_colors[1])
    draw_pixel_rect(d, 32, 8, 18, 18, (40, 160, 40, 255))
    # Pips: showing 5
    draw_pixel_rect(d, 34, 10, 3, 3, (255, 255, 255, 255))
    draw_pixel_rect(d, 45, 10, 3, 3, (255, 255, 255, 255))
    draw_pixel_rect(d, 40, 16, 3, 3, (255, 255, 255, 255))
    draw_pixel_rect(d, 34, 21, 3, 3, (255, 255, 255, 255))
    draw_pixel_rect(d, 45, 21, 3, 3, (255, 255, 255, 255))
    # Die 3 - blue, bottom center
    draw_pixel_rect(d, 16, 34, 22, 22, dice_colors[2])
    draw_pixel_rect(d, 18, 36, 18, 18, (40, 40, 200, 255))
    # Pips: showing 1
    draw_pixel_rect(d, 25, 43, 4, 4, (255, 255, 255, 255))
    # Rainbow sparkles
    sparkle_colors = [(255, 255, 0, 200), (255, 0, 255, 200), (0, 255, 255, 200)]
    for i, (sx, sy) in enumerate([(2, 30), (58, 2), (56, 56), (28, 2), (44, 40)]):
        draw_pixel_rect(d, sx, sy, 3, 3, sparkle_colors[i % 3])
    return img


def draw_crazy_minecrafter():
    img = new_canvas()
    d = fill_bg(img, (15, 15, 25, 255))
    # Diamond pickaxe head
    diamond = (80, 220, 240, 255)
    diamond_dark = (50, 180, 200, 255)
    # Handle
    d.line([(32, 44), (32, 56)], fill=(140, 100, 50, 255), width=4)
    d.line([(30, 56), (34, 56)], fill=(120, 80, 40, 255), width=2)
    # Pickaxe head - horizontal bar
    draw_pixel_rect(d, 10, 18, 44, 8, diamond)
    draw_pixel_rect(d, 12, 20, 40, 4, diamond_dark)
    # Curved tips
    draw_pixel_rect(d, 8, 22, 4, 6, diamond)
    draw_pixel_rect(d, 52, 22, 4, 6, diamond)
    # Connect to handle
    draw_pixel_rect(d, 28, 26, 8, 18, (140, 100, 50, 255))
    draw_pixel_rect(d, 30, 26, 4, 4, diamond_dark)
    # Lightning bolts
    bolt_color = (255, 255, 80, 255)
    # Left bolt
    d.polygon([(8, 6), (14, 6), (10, 16), (16, 16), (6, 30), (10, 18), (4, 18)],
              fill=bolt_color)
    # Right bolt
    d.polygon([(50, 4), (56, 4), (52, 14), (58, 14), (48, 28), (52, 16), (46, 16)],
              fill=bolt_color)
    return img


def draw_seer():
    img = new_canvas()
    d = fill_bg(img, (20, 5, 35, 255))
    # Mystical aura - outer glow rings
    d.ellipse([6, 6, 58, 58], fill=(60, 20, 100, 100))
    d.ellipse([10, 10, 54, 54], fill=(80, 30, 130, 120))
    # Large purple eyeball
    d.ellipse([14, 16, 50, 48], fill=(255, 255, 255, 255))
    d.ellipse([16, 18, 48, 46], fill=(240, 240, 250, 255))
    # Iris
    d.ellipse([24, 24, 40, 40], fill=(140, 40, 200, 255))
    d.ellipse([26, 26, 38, 38], fill=(170, 60, 240, 255))
    # Pupil
    d.ellipse([29, 29, 35, 35], fill=(0, 0, 0, 255))
    # Eye shine
    draw_pixel_rect(d, 30, 29, 3, 3, (255, 255, 255, 255))
    # Rune symbols around the eye
    rune_color = (200, 150, 255, 200)
    rune_positions = [(8, 4), (50, 4), (4, 32), (56, 32), (10, 54), (48, 54), (28, 2), (28, 58)]
    for rx, ry in rune_positions:
        draw_pixel_rect(d, rx, ry, 4, 4, rune_color)
        draw_pixel_rect(d, rx + 1, ry + 1, 2, 2, (255, 200, 255, 255))
    return img


def draw_neko():
    img = new_canvas()
    d = fill_bg(img, (40, 25, 35, 255))
    pink = (255, 160, 180, 255)
    white = (255, 255, 255, 255)
    light_pink = (255, 200, 210, 255)
    # Face - white circle
    d.ellipse([12, 20, 52, 56], fill=white)
    d.ellipse([14, 22, 50, 54], fill=(255, 245, 250, 255))
    # Cat ears
    # Left ear
    d.polygon([(12, 24), (22, 24), (10, 4)], fill=pink)
    d.polygon([(14, 22), (20, 22), (12, 8)], fill=light_pink)
    # Inner ear
    d.polygon([(15, 20), (19, 20), (13, 10)], fill=(255, 120, 150, 255))
    # Right ear
    d.polygon([(42, 24), (52, 24), (54, 4)], fill=pink)
    d.polygon([(44, 22), (50, 22), (52, 8)], fill=light_pink)
    # Inner ear
    d.polygon([(45, 20), (49, 20), (51, 10)], fill=(255, 120, 150, 255))
    # Eyes - big anime style
    d.ellipse([18, 28, 30, 40], fill=(100, 200, 100, 255))
    d.ellipse([34, 28, 46, 40], fill=(100, 200, 100, 255))
    d.ellipse([22, 30, 28, 36], fill=(0, 0, 0, 255))
    d.ellipse([38, 30, 44, 36], fill=(0, 0, 0, 255))
    # Eye shines
    draw_pixel_rect(d, 23, 30, 2, 2, white)
    draw_pixel_rect(d, 39, 30, 2, 2, white)
    # Nose
    d.polygon([(30, 40), (34, 40), (32, 43)], fill=pink)
    # Mouth
    d.arc([26, 42, 32, 48], 0, 180, fill=(200, 100, 120, 255), width=1)
    d.arc([32, 42, 38, 48], 0, 180, fill=(200, 100, 120, 255), width=1)
    # Whiskers
    d.line([(6, 36), (18, 38)], fill=(200, 200, 200, 255), width=1)
    d.line([(6, 40), (18, 42)], fill=(200, 200, 200, 255), width=1)
    d.line([(46, 38), (58, 36)], fill=(200, 200, 200, 255), width=1)
    d.line([(46, 42), (58, 40)], fill=(200, 200, 200, 255), width=1)
    return img


def draw_fallen():
    img = new_canvas()
    d = fill_bg(img, (15, 12, 18, 255))
    gray = (100, 100, 110, 255)
    dark = (50, 50, 60, 255)
    black = (20, 20, 25, 255)
    # Left wing (broken)
    d.polygon([(32, 28), (4, 12), (8, 22), (2, 30), (10, 28), (6, 38), (14, 34),
               (18, 44), (24, 38), (28, 46)], fill=gray)
    d.polygon([(32, 30), (8, 16), (10, 24), (6, 32), (12, 30), (10, 38), (16, 36),
               (20, 44)], fill=dark)
    # Right wing (broken)
    d.polygon([(32, 28), (60, 12), (56, 22), (62, 30), (54, 28), (58, 38), (50, 34),
               (46, 44), (40, 38), (36, 46)], fill=gray)
    d.polygon([(32, 30), (56, 16), (54, 24), (58, 32), (52, 30), (54, 38), (48, 36),
               (44, 44)], fill=dark)
    # Broken feather tips
    d.line([(4, 12), (2, 8)], fill=black, width=2)
    d.line([(60, 12), (62, 8)], fill=black, width=2)
    # Halo (broken/tilted)
    d.arc([20, 4, 44, 18], 20, 320, fill=(150, 150, 100, 180), width=2)
    # Gap in halo
    d.arc([22, 6, 42, 16], 340, 380, fill=(15, 12, 18, 255), width=3)
    # Dark tears/drops
    for dy in range(48, 60, 4):
        draw_pixel_rect(d, 30, dy, 4, 3, (60, 20, 40, 200))
    return img


def draw_bruh():
    img = new_canvas()
    d = fill_bg(img, (30, 30, 35, 255))
    yellow = (255, 210, 50, 255)
    dark_yellow = (200, 170, 30, 255)
    # Face circle
    d.ellipse([8, 8, 56, 56], fill=yellow)
    d.ellipse([10, 10, 54, 54], fill=(255, 220, 60, 255))
    # Deadpan eyes - flat lines
    d.line([(18, 26), (28, 26)], fill=(60, 40, 20, 255), width=3)
    d.line([(36, 26), (46, 26)], fill=(60, 40, 20, 255), width=3)
    # Flat mouth - straight line
    d.line([(20, 42), (44, 42)], fill=(60, 40, 20, 255), width=3)
    # Slight shadow under eyes for extra deadpan
    d.line([(18, 30), (28, 30)], fill=(220, 180, 40, 255), width=1)
    d.line([(36, 30), (46, 30)], fill=(220, 180, 40, 255), width=1)
    return img


def draw_ghost():
    img = new_canvas()
    d = fill_bg(img, (10, 15, 30, 255))
    white = (240, 245, 255, 255)
    light = (220, 230, 250, 255)
    # Ghost body
    d.ellipse([14, 6, 50, 40], fill=white)
    draw_pixel_rect(d, 14, 24, 36, 28, white)
    # Wavy bottom
    d.polygon([(14, 48), (14, 56), (22, 48), (28, 56), (36, 48), (42, 56), (50, 48), (50, 52)],
              fill=white)
    d.rectangle([14, 52, 50, 63], fill=(10, 15, 30, 255))
    d.polygon([(14, 50), (22, 58), (28, 50), (36, 58), (42, 50), (50, 58)],
              fill=white)
    # Shading
    d.ellipse([16, 8, 48, 38], fill=light)
    # Eyes - glowing blue
    d.ellipse([20, 20, 30, 32], fill=(50, 100, 255, 255))
    d.ellipse([34, 20, 44, 32], fill=(50, 100, 255, 255))
    # Inner glow
    d.ellipse([22, 22, 28, 30], fill=(100, 180, 255, 255))
    d.ellipse([36, 22, 42, 30], fill=(100, 180, 255, 255))
    # Pupils
    d.ellipse([24, 24, 27, 28], fill=(200, 230, 255, 255))
    d.ellipse([38, 24, 41, 28], fill=(200, 230, 255, 255))
    # Mouth - small O
    d.ellipse([28, 36, 36, 44], fill=(180, 190, 210, 255))
    d.ellipse([30, 38, 34, 42], fill=(10, 15, 30, 255))
    return img


def draw_insanity_rampage():
    img = new_canvas()
    d = fill_bg(img, (30, 5, 5, 255))
    # Red spiral
    for i in range(300):
        angle = i * 0.12
        r = 1 + i * 0.08
        x = int(32 + r * math.cos(angle))
        y = int(32 + r * math.sin(angle))
        if 0 <= x < 64 and 0 <= y < 64:
            intensity = min(255, 100 + i % 100)
            c = (intensity, 10, 10, 255)
            draw_pixel_rect(d, x, y, 2, 2, c)
    # Screaming face outline in center
    d.ellipse([20, 18, 44, 46], outline=(255, 200, 200, 255), width=2)
    # Eyes - wide open
    d.ellipse([24, 24, 30, 32], fill=(255, 255, 255, 255))
    d.ellipse([34, 24, 40, 32], fill=(255, 255, 255, 255))
    d.ellipse([26, 26, 28, 30], fill=(0, 0, 0, 255))
    d.ellipse([36, 26, 38, 30], fill=(0, 0, 0, 255))
    # Screaming mouth
    d.ellipse([26, 34, 38, 46], fill=(0, 0, 0, 255))
    d.ellipse([28, 36, 36, 44], fill=(80, 0, 0, 255))
    return img


def draw_total_chaos():
    img = new_canvas()
    d = fill_bg(img, (10, 10, 10, 255))
    # Explosion of colors
    import random
    rng = random.Random(123)
    chaos_colors = [
        (255, 50, 50, 255), (50, 255, 50, 255), (50, 50, 255, 255),
        (255, 255, 50, 255), (255, 50, 255, 255), (50, 255, 255, 255),
        (255, 150, 0, 255), (255, 255, 255, 255)
    ]
    # Central explosion burst
    for _ in range(60):
        angle = rng.uniform(0, 2 * math.pi)
        dist = rng.uniform(2, 28)
        x = int(32 + dist * math.cos(angle))
        y = int(32 + dist * math.sin(angle))
        size = rng.randint(2, 6)
        color = chaos_colors[rng.randint(0, len(chaos_colors) - 1)]
        shape = rng.randint(0, 2)
        if shape == 0:
            draw_pixel_rect(d, x, y, size, size, color)
        elif shape == 1:
            d.ellipse([x, y, x + size, y + size], fill=color)
        else:
            d.polygon([(x, y - size), (x + size, y + size), (x - size, y + size)],
                      fill=color)
    # Bright center
    draw_circle(d, 32, 32, 6, (255, 255, 255, 255))
    draw_circle(d, 32, 32, 4, (255, 255, 200, 255))
    return img


def draw_chaotic_determination():
    img = new_canvas()
    d = fill_bg(img, (25, 10, 15, 255))
    gold = (255, 200, 50, 255)
    gold_dark = (200, 150, 20, 255)
    red = (200, 30, 30, 255)
    dark_red = (150, 20, 20, 255)
    # Cracked heart
    # Heart shape using two circles + triangle
    d.ellipse([14, 18, 34, 38], fill=red)
    d.ellipse([30, 18, 50, 38], fill=red)
    d.polygon([(14, 32), (50, 32), (32, 54)], fill=red)
    # Darker inner heart
    d.ellipse([18, 22, 32, 34], fill=dark_red)
    d.ellipse([32, 22, 46, 34], fill=dark_red)
    d.polygon([(18, 30), (46, 30), (32, 48)], fill=dark_red)
    # Cracks in heart
    crack = (40, 10, 10, 255)
    d.line([(28, 22), (32, 34), (36, 26)], fill=crack, width=2)
    d.line([(32, 34), (30, 44)], fill=crack, width=1)
    d.line([(32, 34), (38, 42)], fill=crack, width=1)
    # Golden sword through heart
    # Blade
    draw_pixel_rect(d, 30, 2, 4, 48, gold)
    draw_pixel_rect(d, 31, 4, 2, 44, gold_dark)
    # Crossguard
    draw_pixel_rect(d, 22, 40, 20, 4, gold)
    draw_pixel_rect(d, 24, 41, 16, 2, gold_dark)
    # Handle
    draw_pixel_rect(d, 30, 44, 4, 10, (140, 100, 50, 255))
    # Pommel
    draw_pixel_rect(d, 29, 54, 6, 4, gold)
    # Sword tip
    d.polygon([(30, 2), (34, 2), (32, 0)], fill=(255, 255, 200, 255))
    return img


def draw_nightmare():
    img = new_canvas()
    d = fill_bg(img, (15, 5, 25, 255))
    purple = (100, 30, 140, 255)
    dark_purple = (60, 15, 90, 255)
    # Sleeping eye - closed lid
    d.ellipse([8, 16, 56, 48], fill=purple)
    # Eyelid - filled over top half
    d.pieslice([8, 16, 56, 48], 180, 360, fill=dark_purple)
    # Eyelash line
    d.arc([8, 16, 56, 48], 0, 180, fill=(140, 60, 180, 255), width=3)
    # Red tears
    tear_red = (200, 20, 20, 255)
    # Left tears
    d.polygon([(20, 38), (22, 38), (21, 52)], fill=tear_red)
    d.polygon([(16, 40), (18, 40), (17, 56)], fill=tear_red)
    # Right tears
    d.polygon([(42, 38), (44, 38), (43, 52)], fill=tear_red)
    d.polygon([(46, 40), (48, 40), (47, 56)], fill=tear_red)
    # Tear drops at bottom
    draw_circle(d, 21, 54, 2, tear_red)
    draw_circle(d, 17, 58, 2, tear_red)
    draw_circle(d, 43, 54, 2, tear_red)
    draw_circle(d, 47, 58, 2, tear_red)
    # Z's for sleeping
    zz_color = (180, 140, 220, 200)
    # Small z
    draw_pixel_rect(d, 46, 6, 6, 2, zz_color)
    d.line([(52, 8), (46, 14)], fill=zz_color, width=2)
    draw_pixel_rect(d, 46, 14, 6, 2, zz_color)
    # Bigger Z
    draw_pixel_rect(d, 52, 2, 8, 2, zz_color)
    d.line([(60, 4), (52, 12)], fill=zz_color, width=2)
    draw_pixel_rect(d, 52, 12, 8, 2, zz_color)
    return img


def draw_lost_moon():
    img = new_canvas()
    d = fill_bg(img, (10, 10, 15, 255))
    gray = (140, 140, 150, 255)
    dark_gray = (80, 80, 90, 255)
    # Cracked moon
    d.ellipse([10, 10, 54, 54], fill=gray)
    d.ellipse([12, 12, 52, 52], fill=(160, 160, 170, 255))
    # Craters
    d.ellipse([18, 18, 26, 26], fill=dark_gray)
    d.ellipse([34, 14, 44, 24], fill=dark_gray)
    d.ellipse([24, 36, 34, 44], fill=dark_gray)
    d.ellipse([40, 34, 48, 42], fill=dark_gray)
    # Major cracks
    crack_color = (40, 40, 50, 255)
    d.line([(32, 12), (28, 32), (36, 42), (32, 52)], fill=crack_color, width=2)
    d.line([(28, 32), (18, 36)], fill=crack_color, width=2)
    d.line([(36, 42), (46, 48)], fill=crack_color, width=2)
    d.line([(28, 32), (22, 22)], fill=crack_color, width=1)
    d.line([(36, 42), (44, 38)], fill=crack_color, width=1)
    # Pieces falling away
    d.ellipse([2, 52, 10, 60], fill=dark_gray)
    d.ellipse([52, 50, 62, 60], fill=dark_gray)
    # Small debris
    draw_pixel_rect(d, 6, 48, 3, 3, dark_gray)
    draw_pixel_rect(d, 56, 46, 3, 3, dark_gray)
    draw_pixel_rect(d, 48, 56, 2, 2, (100, 100, 110, 255))
    return img


def draw_bloody_eclipse():
    img = new_canvas()
    d = fill_bg(img, (10, 0, 0, 255))
    # Eclipse - black circle with red corona
    # Red glow / corona
    d.ellipse([6, 6, 58, 58], fill=(120, 10, 10, 255))
    d.ellipse([8, 8, 56, 56], fill=(160, 20, 10, 255))
    d.ellipse([10, 10, 54, 54], fill=(100, 10, 5, 255))
    # Black moon (eclipsing body)
    d.ellipse([14, 14, 50, 50], fill=(5, 0, 0, 255))
    # Blood drips from bottom
    blood = (180, 10, 10, 255)
    blood_dark = (120, 5, 5, 255)
    drip_positions = [(22, 48), (30, 50), (38, 49), (44, 47)]
    for dx, dy in drip_positions:
        draw_pixel_rect(d, dx, dy, 3, 8, blood)
        draw_circle(d, dx + 1, dy + 10, 2, blood_dark)
    # Red rim highlight
    d.arc([12, 12, 52, 52], 200, 340, fill=(255, 40, 20, 255), width=2)
    return img


def draw_armageddon():
    img = new_canvas()
    d = fill_bg(img, (60, 15, 5, 255))
    # Red/orange sky gradient (simple)
    for y in range(0, 20):
        r = min(255, 80 + y * 8)
        d.line([(0, y), (63, y)], fill=(r, 30 + y * 2, 0, 255))
    # Mushroom cloud
    # Mushroom cap
    d.ellipse([8, 10, 56, 36], fill=(200, 150, 80, 255))
    d.ellipse([12, 12, 52, 32], fill=(220, 180, 100, 255))
    d.ellipse([16, 8, 48, 26], fill=(240, 200, 120, 255))
    # Bright top
    d.ellipse([22, 6, 42, 20], fill=(255, 230, 150, 255))
    # Stem
    draw_pixel_rect(d, 24, 30, 16, 26, (200, 140, 60, 255))
    draw_pixel_rect(d, 26, 32, 12, 22, (220, 160, 80, 255))
    # Base expansion
    d.ellipse([18, 48, 46, 60], fill=(180, 120, 50, 255))
    # Bright core in stem
    draw_pixel_rect(d, 28, 34, 8, 16, (255, 200, 100, 255))
    # Ground line
    draw_pixel_rect(d, 0, 58, 64, 6, (40, 20, 10, 255))
    return img


def draw_oblivion():
    img = new_canvas()
    d = fill_bg(img, (0, 0, 0, 255))
    # Pure black void - everything is black
    # Subtle gradient - very very dark rings
    for r in range(30, 4, -2):
        intensity = max(0, 5 - r // 8)
        d.ellipse([32 - r, 32 - r, 32 + r, 32 + r], fill=(intensity, intensity, intensity + 1, 255))
    # Single white dot in center
    draw_pixel_rect(d, 31, 31, 2, 2, (255, 255, 255, 255))
    # Very subtle aura around the dot
    draw_pixel_rect(d, 30, 31, 1, 2, (40, 40, 45, 255))
    draw_pixel_rect(d, 33, 31, 1, 2, (40, 40, 45, 255))
    draw_pixel_rect(d, 31, 30, 2, 1, (40, 40, 45, 255))
    draw_pixel_rect(d, 31, 33, 2, 1, (40, 40, 45, 255))
    return img


def draw_god_eater():
    img = new_canvas()
    d = fill_bg(img, (20, 10, 5, 255))
    gold = (255, 200, 50, 255)
    gold_bright = (255, 230, 100, 255)
    gold_dark = (180, 140, 20, 255)
    # Star in background
    draw_star(d, 32, 28, 20, 8, 5, (255, 220, 80, 200))
    draw_star(d, 32, 28, 16, 6, 5, (255, 240, 120, 200))
    # Golden jaw/teeth biting through star
    # Upper jaw
    d.polygon([(8, 24), (56, 24), (52, 34), (12, 34)], fill=gold)
    # Upper teeth
    for tx in range(14, 52, 6):
        d.polygon([(tx, 34), (tx + 5, 34), (tx + 2, 42)], fill=gold_bright)
    # Lower jaw
    d.polygon([(10, 44), (54, 44), (56, 34), (8, 34)], fill=gold_dark)
    # Lower teeth
    for tx in range(14, 52, 6):
        d.polygon([(tx, 44), (tx + 5, 44), (tx + 2, 36)], fill=gold_bright)
    # Jaw lines
    d.line([(8, 24), (56, 24)], fill=(140, 100, 10, 255), width=2)
    d.line([(8, 34), (56, 34)], fill=(100, 70, 5, 200), width=1)
    d.line([(10, 44), (54, 44)], fill=(140, 100, 10, 255), width=2)
    # Glow/energy from bitten star
    d.line([(4, 18), (10, 24)], fill=gold_bright, width=1)
    d.line([(58, 16), (54, 24)], fill=gold_bright, width=1)
    d.line([(32, 4), (32, 14)], fill=gold_bright, width=2)
    # Broken star fragments
    draw_pixel_rect(d, 4, 8, 4, 4, (255, 200, 80, 180))
    draw_pixel_rect(d, 54, 6, 4, 4, (255, 200, 80, 180))
    draw_pixel_rect(d, 28, 48, 3, 3, (255, 200, 80, 120))
    return img


def draw_question_mark():
    """Shared question mark texture - dark gray bg with white '?' in pixel font."""
    img = new_canvas()
    d = fill_bg(img, (40, 40, 45, 255))
    white = (255, 255, 255, 255)
    light_gray = (180, 180, 190, 255)
    # Pixel art "?" character, large and centered
    # Top curve of ?
    draw_pixel_rect(d, 22, 10, 20, 4, white)   # top bar
    draw_pixel_rect(d, 18, 14, 6, 4, white)     # top-left curve
    draw_pixel_rect(d, 40, 14, 6, 4, white)     # top-right
    draw_pixel_rect(d, 40, 18, 6, 6, white)     # right side down
    draw_pixel_rect(d, 36, 24, 6, 4, white)     # curve inward
    draw_pixel_rect(d, 30, 28, 8, 4, white)     # middle descender
    draw_pixel_rect(d, 28, 32, 8, 6, white)     # vertical stem
    # Dot
    draw_pixel_rect(d, 28, 44, 8, 8, white)
    # Subtle shadow
    draw_pixel_rect(d, 24, 12, 20, 2, light_gray)
    return img


# ---------------------------------------------------------------------------
# Main: generate all badges
# ---------------------------------------------------------------------------

BADGES = {
    "calamity_event_1": draw_calamity_event_1,
    "calamity_event_2": draw_calamity_event_2,
    "1x1x1x1": draw_1x1x1x1,
    "blue_moon_event_2023": draw_blue_moon_event_2023,
    "hallows_2023": draw_hallows_2023,
    "hallows_2024": draw_hallows_2024,
    "anniversary_2024": draw_anniversary_2024,
    "april_fools_2025": draw_april_fools_2025,
    "tutorial": draw_tutorial,
    "freezing_ice": draw_freezing_ice,
    "blue_moon": draw_blue_moon,
    "fish": draw_fish,
    "sonic": draw_sonic,
    "infested": draw_infested,
    "chef": draw_chef,
    "devils_dream": draw_devils_dream,
    "corrupted_corruption": draw_corrupted_corruption,
    "doom": draw_doom,
    "chain": draw_chain,
    "creeper_infestation": draw_creeper_infestation,
    "unpredictable_randomness": draw_unpredictable_randomness,
    "crazy_minecrafter": draw_crazy_minecrafter,
    "seer": draw_seer,
    "neko": draw_neko,
    "fallen": draw_fallen,
    "bruh": draw_bruh,
    "ghost": draw_ghost,
    "insanity_rampage": draw_insanity_rampage,
    "total_chaos": draw_total_chaos,
    "chaotic_determination": draw_chaotic_determination,
    "nightmare": draw_nightmare,
    "lost_moon": draw_lost_moon,
    "bloody_eclipse": draw_bloody_eclipse,
    "armageddon": draw_armageddon,
    "oblivion": draw_oblivion,
    "god_eater": draw_god_eater,
}


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    file_count = 0

    print(f"Generating badge textures in: {OUTPUT_DIR}")
    print(f"Badge count: {len(BADGES)} badges + 1 shared question mark")
    print("-" * 50)

    # Generate each badge (colored + grayscale)
    for badge_id, draw_func in BADGES.items():
        img = draw_func()
        saved = save_badge(img, badge_id)
        file_count += saved
        print(f"  [OK] {badge_id}.png + {badge_id}_gray.png")

    # Generate shared question mark
    qm = draw_question_mark()
    add_border(qm)
    qm_path = os.path.join(OUTPUT_DIR, "question_mark.png")
    qm.save(qm_path)
    file_count += 1
    print(f"  [OK] question_mark.png (shared)")

    print("-" * 50)
    print(f"Done! Generated {file_count} files total.")
    print(f"  - {len(BADGES)} colored badges")
    print(f"  - {len(BADGES)} grayscale badges")
    print(f"  - 1 shared question mark")
    print(f"  = {file_count} files")


if __name__ == "__main__":
    main()
