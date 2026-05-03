#!/usr/bin/env python3
"""
Generate laser_pointer_fixation.bbmodel — LASER POINTER DOT BEAM "Fixation" VFX.

A red laser pointer dot with highly visible beam — universal cat trigger.
Beam thin and perfectly straight, dot at impact end small bright circle,
dot bounces and darts unpredictably during idle (jerky cat-toy motion).

Smallest geometry in Fluffy Mode set, but the most kinetically complex idle.
Pure red emissive laser texture vs near-black outer glow — the only hard-red
asset in the otherwise pastel Fluffy set. Contrast is the point.
"""
import sys
import math
import random
sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder, make_uuid, make_keyframe, basic_cube_faces, uniform_face,
    png_from_pixels, hex_to_rgba, fill_rect,
)


# ---------------------------------------------------------------------------
# TEXTURES
# ---------------------------------------------------------------------------
def build_emitter_texture():
    """64x64 LASER POINTER HARDWARE — black plastic body with red emissive button."""
    w, h = 64, 64
    plastic = hex_to_rgba("#282828")        # dark plastic grey
    mid = hex_to_rgba("#484848")            # mid grey
    edge = hex_to_rgba("#888888")           # bright edge highlight
    button = hex_to_rgba("#ff2020")         # emissive red button
    button_dark = hex_to_rgba("#a01010")    # button shadow
    spec = hex_to_rgba("#ffffff")           # bright specular

    pixels = [plastic] * (w * h)

    # Subtle horizontal banding for plastic shading (suggests cylindrical body)
    for y in range(h):
        # Top half slightly lighter, bottom half slightly darker
        if y < 16:
            for x in range(w):
                pixels[y * w + x] = mid
        elif y < 24:
            # Gradient blend band
            for x in range(w):
                pixels[y * w + x] = mid if (x + y) % 3 == 0 else plastic
        elif y > 48:
            # Lower body shadow
            for x in range(w):
                if (x + y) % 4 == 0:
                    pixels[y * w + x] = (max(0, plastic[0] - 12),
                                         max(0, plastic[1] - 12),
                                         max(0, plastic[2] - 12), 255)

    # Edge highlight strips on left and right (suggests cylindrical body)
    for y in range(h):
        pixels[y * w + 0] = edge
        pixels[y * w + 1] = edge
        pixels[y * w + (w - 1)] = edge
        pixels[y * w + (w - 2)] = edge

    # Vertical seam stripes for plastic seams
    for y in range(h):
        for x in range(w):
            if x in (16, 17, 47, 48) and y > 4 and y < h - 4:
                pixels[y * w + x] = mid

    # Red emissive button — circular, centred-ish at (32, 28)
    bcx, bcy, br = 32, 28, 5
    for dy in range(-br - 1, br + 2):
        for dx in range(-br - 1, br + 2):
            d = math.sqrt(dx * dx + dy * dy)
            px = bcx + dx
            py = bcy + dy
            if 0 <= px < w and 0 <= py < h:
                if d <= br:
                    pixels[py * w + px] = button
                elif d <= br + 0.8:
                    pixels[py * w + px] = button_dark

    # Inner darker rim for button depth
    for dy in range(-br + 1, br):
        for dx in range(-br + 1, br):
            d = math.sqrt(dx * dx + dy * dy)
            if br - 1.5 <= d <= br - 0.5:
                px = bcx + dx
                py = bcy + dy
                if 0 <= px < w and 0 <= py < h:
                    pixels[py * w + px] = button_dark

    # Specular highlight at (52, 8) — bright white
    for dy in range(-2, 3):
        for dx in range(-2, 3):
            d = math.sqrt(dx * dx + dy * dy)
            if d <= 2.0:
                px = 52 + dx
                py = 8 + dy
                if 0 <= px < w and 0 <= py < h:
                    pixels[py * w + px] = spec

    # Tiny secondary specular on button (button reflection)
    for dy in range(-1, 1):
        for dx in range(-1, 1):
            px = bcx - 2 + dx
            py = bcy - 2 + dy
            if 0 <= px < w and 0 <= py < h:
                pixels[py * w + px] = (255, 180, 180, 255)

    # LED indicator — tiny red dot at (10, 10)
    for dy in range(-1, 2):
        for dx in range(-1, 2):
            d = math.sqrt(dx * dx + dy * dy)
            if d <= 1.0:
                px = 10 + dx
                py = 10 + dy
                if 0 <= px < w and 0 <= py < h:
                    pixels[py * w + px] = button

    # Subtle scratches / wear
    rng = random.Random(303)
    for _ in range(40):
        x = rng.randrange(w)
        y = rng.randrange(h)
        # Avoid clobbering button area
        if abs(x - bcx) < br + 2 and abs(y - bcy) < br + 2:
            continue
        if rng.random() < 0.5:
            pixels[y * w + x] = mid
        else:
            pixels[y * w + x] = edge

    # Brand-style horizontal text strip placeholder — a few darker pixels (60, 56)
    for x in range(20, 44):
        if x % 2 == 0:
            pixels[56 * w + x] = (16, 16, 16, 255)

    return png_from_pixels(pixels, w, h)


def build_laser_texture():
    """64x64 PURE RED LASER — emissive core, deep glow, near-black outer."""
    w, h = 64, 64
    outer = hex_to_rgba("#100000")      # near-black outer
    deep = hex_to_rgba("#600808")       # deep red glow
    bright = hex_to_rgba("#e01010")     # bright red core
    pure = hex_to_rgba("#ff2020")       # pure emissive red

    pixels = [outer] * (w * h)

    # Vertical glow gradient — beam runs vertical in texture
    # Centre column is pure red (2px wide), expanding outward through bright/deep/outer
    cx = 32
    for y in range(h):
        for x in range(w):
            dx = abs(x - cx)
            if dx <= 1:
                pixels[y * w + x] = pure
            elif dx <= 4:
                pixels[y * w + x] = bright
            elif dx <= 10:
                pixels[y * w + x] = deep
            elif dx <= 18:
                # Smooth transition to outer
                t = (dx - 10) / 8.0
                r = int(deep[0] * (1 - t) + outer[0] * t)
                g = int(deep[1] * (1 - t) + outer[1] * t)
                b = int(deep[2] * (1 - t) + outer[2] * t)
                pixels[y * w + x] = (r, g, b, 255)
            else:
                pixels[y * w + x] = outer

    # Add pulse banding — every 8px y, brighten the core a bit (laser shimmer)
    for y in range(h):
        if y % 8 < 2:
            for x in range(cx - 1, cx + 2):
                if 0 <= x < w:
                    pixels[y * w + x] = (255, 80, 80, 255)

    # Speckle some bright dots near core for shimmer
    rng = random.Random(404)
    for _ in range(80):
        y = rng.randrange(h)
        x = cx + rng.randint(-2, 2)
        if 0 <= x < w:
            pixels[y * w + x] = pure

    # Tiny flares scattered far from core (lens-flare-style sparkle)
    for _ in range(30):
        x = rng.randrange(w)
        y = rng.randrange(h)
        if abs(x - cx) > 12 and rng.random() < 0.5:
            pixels[y * w + x] = (60, 4, 4, 255)

    # Horizontal "intensity bands" near top and bottom (suggesting beam flutter)
    for x in range(w):
        dx = abs(x - cx)
        if dx <= 6:
            # row 4
            r = int(bright[0] * 0.85 + pure[0] * 0.15)
            pixels[4 * w + x] = (r, bright[1], bright[2], 255)
            # row h-5
            pixels[(h - 5) * w + x] = (r, bright[1], bright[2], 255)

    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# BUILD MODEL
# ---------------------------------------------------------------------------
def build():
    b = Builder("laser_pointer_fixation", resolution=(64, 64), visible_box=(4, 4, 0))

    # Textures
    b.add_texture("laser_emitter", build_emitter_texture())
    b.add_texture("laser_beam", build_laser_texture())

    emitter_faces = lambda sx, sy, sz: basic_cube_faces(0, 0, sx, sy, sz, tex_index=0)
    laser_faces = lambda sx, sy, sz: basic_cube_faces(0, 0, sx, sy, sz, tex_index=1)

    # ===== EMITTER (laser pointer housing) — 3-cube cluster at beam origin =====
    # Place emitter at (-8, 4, 0), beam runs from emitter toward (+X) to dot at (+8, 0.1, 0)
    emitter_bones = []
    emitter_configs = [
        # Main barrel — long cylinder-like cube
        ("emit_barrel", [-9.0, 3.6, -0.5], [-7.0, 4.4, 0.5], [0, 0, 0]),
        # Tip cap — slightly smaller, where beam exits
        ("emit_tip", [-7.2, 3.7, -0.4], [-6.6, 4.3, 0.4], [0, 0, 0]),
        # Rear cap — battery end
        ("emit_rear", [-9.6, 3.7, -0.4], [-9.0, 4.3, 0.4], [0, 0, 0]),
    ]
    for name, frm, to_, rot in emitter_configs:
        sx = max(1, int(round(to_[0] - frm[0])))
        sy = max(1, int(round(to_[1] - frm[1])))
        sz = max(1, int(round(to_[2] - frm[2])))
        cube_uuid = b.add_cube(name, frm, to_, emitter_faces(sx, sy, sz),
                               origin=[(frm[0] + to_[0]) / 2, 4.0, 0],
                               rotation=rot)
        bone_uuid = make_uuid()
        bone = b.make_group(name,
                            [(frm[0] + to_[0]) / 2, 4.0, 0],
                            [cube_uuid],
                            rotation=rot,
                            group_uuid=bone_uuid)
        emitter_bones.append((name, bone_uuid, cube_uuid, bone))

    # ===== BEAM CORE — single very thin long flat slab =====
    # Beam runs along X from -7 (emitter tip) to +7 (just before dot)
    # 0.2 thick × 0.2 wide × ~14 long (16 spec but kept inside arena range)
    beam_frm = [-7.0, 3.9, -0.1]
    beam_to_ = [7.0, 4.1, 0.1]
    beam_cube = b.add_cube("beam_core", beam_frm, beam_to_,
                           laser_faces(14, 1, 1),
                           origin=[-7.0, 4.0, 0])
    beam_core_bone_uuid = make_uuid()
    beam_core_group = b.make_group("beam_core",
                                   [-7.0, 4.0, 0],
                                   [beam_cube],
                                   group_uuid=beam_core_bone_uuid)

    # ===== BEAM GLOW — 2 slightly larger flat slabs around core =====
    glow_bones = []
    glow_configs = [
        ("beam_glow_a", [-7.0, 3.75, -0.25], [7.0, 4.25, 0.25], [0, 0, 0]),
        ("beam_glow_b", [-7.0, 3.6, -0.4], [7.0, 4.4, 0.4], [0, 0, 0]),
    ]
    for name, frm, to_, rot in glow_configs:
        sx = max(1, int(round(to_[0] - frm[0])))
        sy = max(1, int(round(to_[1] - frm[1])))
        sz = max(1, int(round(to_[2] - frm[2])))
        cube_uuid = b.add_cube(name, frm, to_, laser_faces(sx, sy, sz),
                               origin=[-7.0, 4.0, 0], rotation=rot)
        bone_uuid = make_uuid()
        bone = b.make_group(name, [-7.0, 4.0, 0], [cube_uuid],
                            rotation=rot, group_uuid=bone_uuid)
        glow_bones.append((name, bone_uuid, cube_uuid, bone))

    # ===== IMPACT DOT — 4 flat slabs in cross/plus pattern at Y=0.1 =====
    # Dot located at (+8, 0.1, 0) — beam end where laser hits the floor
    dot_bones = []
    dot_configs = [
        # Horizontal arm (long X, thin Z)
        ("dot_horiz_a", [7.4, 0.05, -0.15], [8.6, 0.15, 0.15], [0, 0, 0]),
        # Vertical arm (long Z, thin X)
        ("dot_horiz_b", [7.85, 0.05, -0.6], [8.15, 0.15, 0.6], [0, 0, 0]),
        # Diagonal arms — rotated 45 deg (still axis-aligned cubes, rotated via bone)
        ("dot_diag_a", [7.5, 0.06, -0.1], [8.5, 0.14, 0.1], [0, 45, 0]),
        ("dot_diag_b", [7.5, 0.06, -0.1], [8.5, 0.14, 0.1], [0, -45, 0]),
    ]
    for name, frm, to_, rot in dot_configs:
        sx = max(1, int(round(to_[0] - frm[0])))
        sy = max(1, int(round(to_[1] - frm[1])))
        sz = max(1, int(round(to_[2] - frm[2])))
        cube_uuid = b.add_cube(name, frm, to_, laser_faces(sx, sy, sz),
                               origin=[8.0, 0.1, 0], rotation=rot)
        bone_uuid = make_uuid()
        bone = b.make_group(name, [8.0, 0.1, 0], [cube_uuid],
                            rotation=rot, group_uuid=bone_uuid)
        dot_bones.append((name, bone_uuid, cube_uuid, bone))

    # ===== DOT RING — 6 small slabs in a circle around dot at Y=0.12 =====
    ring_bones = []
    n_ring = 6
    ring_radius = 0.7
    for i in range(n_ring):
        angle = (2 * math.pi * i) / n_ring
        cx = 8.0 + math.cos(angle) * ring_radius
        cz = math.sin(angle) * ring_radius
        # Tiny slab ~0.3x0.05x0.15
        frm = [cx - 0.15, 0.10, cz - 0.075]
        to_ = [cx + 0.15, 0.14, cz + 0.075]
        sx = max(1, int(round(to_[0] - frm[0])) or 1)
        sy = 1
        sz = max(1, int(round(to_[2] - frm[2])) or 1)
        cube_uuid = b.add_cube(f"ring_{i}", frm, to_, laser_faces(sx, sy, sz),
                               origin=[cx, 0.12, cz],
                               rotation=[0, math.degrees(angle), 0])
        bone_uuid = make_uuid()
        bone = b.make_group(f"ring_{i}_bone", [cx, 0.12, cz], [cube_uuid],
                            rotation=[0, math.degrees(angle), 0],
                            group_uuid=bone_uuid)
        ring_bones.append((f"ring_{i}", bone_uuid, cube_uuid, bone, i))

    # ===== ROOT GROUP =====
    # Sub-groups: emitter (does not move with dot), beam (stretches), dot+ring (darts together)
    emitter_group_uuid = make_uuid()
    emitter_group = b.make_group("emitter", [-8.0, 4.0, 0],
                                 [bone for (_, _, _, bone) in emitter_bones],
                                 group_uuid=emitter_group_uuid)

    beam_group_uuid = make_uuid()
    beam_group = b.make_group("beam", [-7.0, 4.0, 0],
                              [beam_core_group] + [bone for (_, _, _, bone) in glow_bones],
                              group_uuid=beam_group_uuid)

    dot_group_uuid = make_uuid()
    dot_group = b.make_group("dot", [8.0, 0.1, 0],
                             [bone for (_, _, _, bone) in dot_bones]
                             + [bone for (_, _, _, bone, _) in ring_bones],
                             group_uuid=dot_group_uuid)

    root_uuid = b.add_root_group("root", [0, 0, 0],
                                 [emitter_group, beam_group, dot_group])

    # =======================================================================
    # ANIMATIONS
    # =======================================================================

    # ------ SPAWN: 0.2s ------
    # Emitter materialises at 0.0, beam extends from emitter to dot rapidly,
    # dot expands from 0 scale, ring appears.
    spawn_animators = {}

    # Emitter — pop into existence at 0.0 (scale 0 -> 1)
    for (name, bone_uuid, _, _) in emitter_bones:
        kfs = []
        kfs.append(make_keyframe("scale", 0.0, 0.0, 0.0, 0.0, "linear"))
        kfs.append(make_keyframe("scale", 0.02, 1.15, 1.15, 1.15, "linear"))  # tiny pop
        kfs.append(make_keyframe("scale", 0.05, 1.0, 1.0, 1.0, "linear"))
        kfs.append(make_keyframe("scale", 0.2, 1.0, 1.0, 1.0, "linear"))
        spawn_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # Beam core — extend from emitter (scale.x 0 -> 1) very rapidly
    beam_kfs = []
    beam_kfs.append(make_keyframe("scale", 0.0, 0.0, 0.0, 0.0, "linear"))
    beam_kfs.append(make_keyframe("scale", 0.04, 0.0, 0.0, 0.0, "linear"))
    beam_kfs.append(make_keyframe("scale", 0.06, 0.5, 1.0, 1.0, "linear"))
    beam_kfs.append(make_keyframe("scale", 0.10, 1.0, 1.5, 1.5, "linear"))  # flare on connect
    beam_kfs.append(make_keyframe("scale", 0.13, 1.0, 1.0, 1.0, "linear"))
    beam_kfs.append(make_keyframe("scale", 0.2, 1.0, 1.0, 1.0, "linear"))
    spawn_animators[beam_core_bone_uuid] = {"name": "beam_core", "type": "bone", "keyframes": beam_kfs}

    # Glow slabs — flicker in just after core
    for idx, (name, bone_uuid, _, _) in enumerate(glow_bones):
        kfs = []
        kfs.append(make_keyframe("scale", 0.0, 0.0, 0.0, 0.0, "linear"))
        kfs.append(make_keyframe("scale", 0.06 + idx * 0.01, 0.0, 0.0, 0.0, "linear"))
        kfs.append(make_keyframe("scale", 0.10 + idx * 0.01, 1.0, 1.3, 1.3, "linear"))
        kfs.append(make_keyframe("scale", 0.14, 1.0, 1.0, 1.0, "linear"))
        kfs.append(make_keyframe("scale", 0.2, 1.0, 1.0, 1.0, "linear"))
        spawn_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # Dot slabs — expand from 0 scale
    for idx, (name, bone_uuid, _, _) in enumerate(dot_bones):
        kfs = []
        kfs.append(make_keyframe("scale", 0.0, 0.0, 0.0, 0.0, "linear"))
        kfs.append(make_keyframe("scale", 0.10, 0.0, 0.0, 0.0, "linear"))  # appears with beam
        kfs.append(make_keyframe("scale", 0.13, 1.4, 1.4, 1.4, "linear"))   # overshoot
        kfs.append(make_keyframe("scale", 0.16, 0.9, 0.9, 0.9, "linear"))
        kfs.append(make_keyframe("scale", 0.20, 1.0, 1.0, 1.0, "linear"))
        spawn_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # Ring — appear after dot (slight stagger)
    for (name, bone_uuid, _, _, ring_idx) in ring_bones:
        kfs = []
        delay = ring_idx * 0.005
        kfs.append(make_keyframe("scale", 0.0, 0.0, 0.0, 0.0, "linear"))
        kfs.append(make_keyframe("scale", 0.13 + delay, 0.0, 0.0, 0.0, "linear"))
        kfs.append(make_keyframe("scale", 0.16 + delay, 1.3, 1.3, 1.3, "linear"))
        kfs.append(make_keyframe("scale", 0.20, 1.0, 1.0, 1.0, "linear"))
        spawn_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    b.add_animation("spawn", length=0.2, loop="once", override=True, animators=spawn_animators)

    # ------ IDLE: 2.0s, JERKY DART MOTION (dense keyframes) ------
    # Root bone darts around in sharp linear movements with brief holds.
    # Beam follows but slightly lags (dot bone slightly ahead of beam pivot).
    # MUST have many keyframes — this is the kinetic identity of the model.
    idle_animators = {}

    # Generate 32 dart targets across 2.0s with brief holds and sharp transitions.
    # Use a deterministic pseudo-random walker so it looks erratic but stable.
    rng = random.Random(7777)

    # Build a sequence of (time, x, z) keyframes for the DOT bone
    dot_root_keys = []
    cur_x = 0.0
    cur_z = 0.0
    t = 0.0
    dart_log = []  # store (time, x, z) for beam-lag use later
    dot_root_keys.append((0.0, 0.0, 0.0))
    dart_log.append((0.0, 0.0, 0.0))

    # Aim for ~36 keyframes across 2.0s — average ~0.055s spacing
    # Mix dart (sharp move) + hold (no movement, brief pause)
    while t < 1.95:
        # Random dart distance
        dx = rng.uniform(-2.5, 2.5)
        dz = rng.uniform(-2.0, 2.0)
        # Clamp arena
        new_x = max(-3.0, min(3.0, cur_x + dx))
        new_z = max(-2.5, min(2.5, cur_z + dz))
        # Dart duration: very short
        dart_dur = rng.uniform(0.025, 0.045)
        t += dart_dur
        if t >= 2.0:
            break
        dot_root_keys.append((t, new_x, new_z))
        dart_log.append((t, new_x, new_z))
        cur_x = new_x
        cur_z = new_z
        # Optional brief hold (cat freeze)
        if rng.random() < 0.55:
            hold_dur = rng.uniform(0.03, 0.10)
            t += hold_dur
            if t >= 2.0:
                break
            dot_root_keys.append((t, cur_x, cur_z))
            dart_log.append((t, cur_x, cur_z))

    # Force loop closure — return to start at exactly 2.0s
    dot_root_keys.append((2.0, 0.0, 0.0))
    dart_log.append((2.0, 0.0, 0.0))

    # Apply to dot group as position keyframes (sharp linear)
    dot_group_kfs = []
    for (kt, kx, kz) in dot_root_keys:
        dot_group_kfs.append(make_keyframe("position", kt, kx, 0, kz, "linear"))
    # Also add a tiny scale pulse on each dart for "twitch"
    # (just at every ~4th dart key for performance)
    for i, (kt, kx, kz) in enumerate(dot_root_keys):
        if i % 3 == 0:
            pulse = 1.0 + 0.06 * math.sin(i * 1.3)
            dot_group_kfs.append(make_keyframe("scale", kt, pulse, pulse, pulse, "linear"))
    idle_animators[dot_group_uuid] = {"name": "dot", "type": "bone", "keyframes": dot_group_kfs}

    # Individual dot slab spin — adds nervous energy (every dart key)
    for idx, (name, bone_uuid, _, _) in enumerate(dot_bones):
        kfs = []
        for ti, (tt, _, _) in enumerate(dot_root_keys):
            spin = 30 * math.sin(ti * 1.7 + idx * 0.9)
            tilt = 8 * math.cos(ti * 1.3 + idx * 0.7)
            kfs.append(make_keyframe("rotation", tt, tilt, spin, tilt * 0.5, "linear"))
            # Per-slab scale jitter on every other dart
            if ti % 2 == 0:
                jit = 1.0 + 0.10 * math.sin(ti * 2.1 + idx * 1.4)
                kfs.append(make_keyframe("scale", tt, jit, jit, jit, "linear"))
        idle_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # Ring slabs — radial breathing pulse + position jitter on EVERY dart key
    for (name, bone_uuid, _, _, ring_idx) in ring_bones:
        kfs = []
        for i, (kt, _, _) in enumerate(dot_root_keys):
            puls = 1.0 + 0.18 * math.sin(i * 0.8 + ring_idx * 1.05)
            kfs.append(make_keyframe("scale", kt, puls, 1.0, puls, "linear"))
            # Tiny position jitter
            jx = 0.04 * math.sin(i * 1.4 + ring_idx * 0.9)
            jz = 0.04 * math.cos(i * 1.1 + ring_idx * 1.3)
            kfs.append(make_keyframe("position", kt, jx, 0, jz, "linear"))
            # Slight tilt on every other key
            if i % 2 == 0:
                tilt = 6 * math.sin(i * 1.6 + ring_idx * 0.5)
                kfs.append(make_keyframe("rotation", kt, tilt, 0, tilt * 0.5, "linear"))
        idle_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # Add additional micro-keyframes between every dart for the dot group
    # (tiny scale wobble between primary darts to amplify visual jitter)
    extra_dot_kfs = []
    for i in range(len(dot_root_keys) - 1):
        t0 = dot_root_keys[i][0]
        t1 = dot_root_keys[i + 1][0]
        # Insert 2 micro samples between keys for scale shimmer
        for sub in range(1, 3):
            tt = t0 + (t1 - t0) * (sub / 3.0)
            sh = 1.0 + 0.05 * math.sin(i * 3.7 + sub * 1.1)
            extra_dot_kfs.append(make_keyframe("scale", tt, sh, sh, sh, "linear"))
    idle_animators[dot_group_uuid]["keyframes"].extend(extra_dot_kfs)

    # BEAM bone — beam follows dot but with slight LAG (one keyframe behind)
    # We rotate the beam group (origin at emitter end) so the far end tracks the dot.
    # Compute yaw/pitch from emitter (-8, 4) to dot end (8 + dot.x, 0.1 + dot.z)
    beam_group_kfs = []
    LAG_OFFSET = 1  # one dart key behind
    for i, (kt, kx, kz) in enumerate(dot_root_keys):
        # Use lagged dot position
        lag_idx = max(0, i - LAG_OFFSET)
        (_, lx, lz) = dot_root_keys[lag_idx]
        # Vector from emitter (-8, 4, 0) to dot end (8 + lx, 0.1, lz)
        dx_v = (8.0 + lx) - (-8.0)   # ~16 + lx
        dy_v = 0.1 - 4.0             # -3.9 (constant)
        dz_v = lz - 0.0
        # Yaw around Y from +X axis
        yaw_rad = math.atan2(dz_v, dx_v)
        # Pitch (Z rotation in this convention since beam is along X). Use small angle.
        pitch_rad = math.atan2(dy_v, math.sqrt(dx_v * dx_v + dz_v * dz_v))
        # The beam was authored along +X with horizontal at Y=4 -> dot at Y=0.1 = base downward angle.
        # Compute deltas from base orientation (base yaw=0, base pitch already toward dot at origin x,z=0).
        base_dx = 16.0
        base_dy = -3.9
        base_dz = 0.0
        base_yaw = math.atan2(base_dz, base_dx)
        base_pitch = math.atan2(base_dy, math.sqrt(base_dx * base_dx + base_dz * base_dz))
        d_yaw = math.degrees(yaw_rad - base_yaw)
        d_pitch = math.degrees(pitch_rad - base_pitch)
        # Apply as rotation: Y for yaw, Z for pitch
        beam_group_kfs.append(make_keyframe("rotation", kt, 0, d_yaw, d_pitch, "linear"))
    # Also slight beam scale shimmer
    for i in range(0, len(dot_root_keys), 4):
        kt = dot_root_keys[i][0]
        sh = 1.0 + 0.10 * math.sin(i * 0.9)
        beam_group_kfs.append(make_keyframe("scale", kt, 1.0, sh, sh, "linear"))
    idle_animators[beam_group_uuid] = {"name": "beam", "type": "bone", "keyframes": beam_group_kfs}

    # BEAM core — dense flicker for laser shimmer (high-resolution sample)
    beam_core_idle = []
    for ti in range(0, 81):  # 0.025s spacing
        kt = ti * 0.025
        flick = 1.0 + 0.08 * math.sin(ti * 2.7) + 0.04 * math.sin(ti * 5.3)
        beam_core_idle.append(make_keyframe("scale", kt, 1.0, flick, flick, "linear"))
    # Plus a position jitter for laser beam wobble
    for ti in range(0, 41):
        kt = ti * 0.05
        jy = 0.02 * math.sin(ti * 4.1)
        jz = 0.02 * math.cos(ti * 3.7)
        beam_core_idle.append(make_keyframe("position", kt, 0, jy, jz, "linear"))
    idle_animators[beam_core_bone_uuid] = {"name": "beam_core", "type": "bone", "keyframes": beam_core_idle}

    # GLOW slabs — dense shimmer offsets
    for idx, (name, bone_uuid, _, _) in enumerate(glow_bones):
        kfs = []
        for ti in range(0, 81):  # 0.025s spacing
            kt = ti * 0.025
            sh = 1.0 + 0.16 * math.sin(ti * 1.3 + idx * 1.1)
            kfs.append(make_keyframe("scale", kt, 1.0, sh, sh, "linear"))
        # Rotational shimmer (rolls around beam axis)
        for ti in range(0, 41):
            kt = ti * 0.05
            roll = 12 * math.sin(ti * 0.9 + idx * 1.3)
            kfs.append(make_keyframe("rotation", kt, roll, 0, 0, "linear"))
        idle_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # EMITTER bones — bobbing wobble (held by hand of giant cat owner) + dot tracking
    for idx, (name, bone_uuid, _, _) in enumerate(emitter_bones):
        kfs = []
        for ti in range(0, 41):
            kt = ti * 0.05
            wob_y = 0.05 * math.sin(2 * math.pi * kt / 2.0 + idx * 0.5)
            wob_z = 0.03 * math.cos(2 * math.pi * kt / 2.0 + idx * 0.3)
            wob_x = 0.02 * math.sin(2 * math.pi * kt / 1.3 + idx * 0.7)
            kfs.append(make_keyframe("position", kt, wob_x, wob_y, wob_z, "linear"))
            # Tiny tilt
            tilt = 1.5 * math.sin(2 * math.pi * kt / 1.7 + idx * 0.4)
            kfs.append(make_keyframe("rotation", kt, tilt, tilt * 0.6, tilt * 0.3, "linear"))
        # Rotation tracking dot — every dart key for tight aim
        for i, (kt, kx, kz) in enumerate(dot_root_keys):
            d_yaw = math.degrees(math.atan2(kz, 16.0 + kx)) * 0.4
            kfs.append(make_keyframe("rotation", kt, 0, d_yaw, 0, "linear"))
        idle_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    b.add_animation("idle", length=2.0, loop="loop", override=False, animators=idle_animators)

    # ------ DISSIPATE: 0.15s ------
    # Dot rapidly scales to 0, beam retracts to emitter instantly, emitter clicks off.
    diss_animators = {}

    # Emitter — small click flash then vanish
    for idx, (name, bone_uuid, _, _) in enumerate(emitter_bones):
        kfs = []
        kfs.append(make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, "linear"))
        kfs.append(make_keyframe("scale", 0.05, 1.1, 1.1, 1.1, "linear"))    # click pulse
        kfs.append(make_keyframe("scale", 0.10, 0.8, 0.8, 0.8, "linear"))
        kfs.append(make_keyframe("scale", 0.15, 0.0, 0.0, 0.0, "linear"))
        diss_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # Beam core — retract (scale.x 1 -> 0)
    bk = []
    bk.append(make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, "linear"))
    bk.append(make_keyframe("scale", 0.03, 1.0, 1.4, 1.4, "linear"))   # final flare
    bk.append(make_keyframe("scale", 0.06, 0.7, 1.0, 1.0, "linear"))   # snap back
    bk.append(make_keyframe("scale", 0.10, 0.2, 0.4, 0.4, "linear"))
    bk.append(make_keyframe("scale", 0.15, 0.0, 0.0, 0.0, "linear"))
    diss_animators[beam_core_bone_uuid] = {"name": "beam_core", "type": "bone", "keyframes": bk}

    # Glow slabs — fade fast
    for idx, (name, bone_uuid, _, _) in enumerate(glow_bones):
        kfs = []
        kfs.append(make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, "linear"))
        kfs.append(make_keyframe("scale", 0.04, 1.0, 1.5, 1.5, "linear"))  # final pulse
        kfs.append(make_keyframe("scale", 0.08, 0.6, 0.6, 0.6, "linear"))
        kfs.append(make_keyframe("scale", 0.15, 0.0, 0.0, 0.0, "linear"))
        diss_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # Dot slabs — rapid scale to 0 (the dot vanishes first)
    for idx, (name, bone_uuid, _, _) in enumerate(dot_bones):
        kfs = []
        kfs.append(make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, "linear"))
        kfs.append(make_keyframe("scale", 0.02, 1.3, 1.3, 1.3, "linear"))  # final pop
        kfs.append(make_keyframe("scale", 0.06, 0.5, 0.5, 0.5, "linear"))
        kfs.append(make_keyframe("scale", 0.10, 0.0, 0.0, 0.0, "linear"))
        kfs.append(make_keyframe("scale", 0.15, 0.0, 0.0, 0.0, "linear"))
        diss_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    # Dot ring — vanish slightly after dot
    for (name, bone_uuid, _, _, ring_idx) in ring_bones:
        kfs = []
        delay = ring_idx * 0.003
        kfs.append(make_keyframe("scale", 0.0, 1.0, 1.0, 1.0, "linear"))
        kfs.append(make_keyframe("scale", 0.04 + delay, 1.4, 1.0, 1.4, "linear"))
        kfs.append(make_keyframe("scale", 0.10 + delay, 0.0, 0.0, 0.0, "linear"))
        kfs.append(make_keyframe("scale", 0.15, 0.0, 0.0, 0.0, "linear"))
        diss_animators[bone_uuid] = {"name": name, "type": "bone", "keyframes": kfs}

    b.add_animation("dissipate", length=0.15, loop="once", override=True, animators=diss_animators)

    # Write file
    out_path = "D:/CC/ChaosCraft/src/main/resources/models/fluffy/me_attacks/laser_pointer_fixation.bbmodel"
    p = b.write(out_path)
    print(f"Wrote {p}")


if __name__ == "__main__":
    build()
