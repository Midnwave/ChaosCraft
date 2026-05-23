#!/usr/bin/env python3
"""Generator for spectral_chain_forest.bbmodel — Chain Mode "The Hanging".

8 spectral chains erupting from the ground and hanging in the air — not taut,
loose like chains suspended from an invisible ceiling above. Silver-ghost
translucent, gently swaying as if in a breeze that doesn't exist.

Bone hierarchy:
  - 8 hanging chain assemblies × 5 links each = 40 link groups
  - 8 top anchor attachments (2 cubes each)
  - 8 ground emergence points (1 slab each)
  - 8 spectral wisps (small slabs at top)
"""

import sys
import math
import os

sys.path.insert(0, "D:/CC/ChaosCraft/tools/me_models")

from me_bbmodel import (
    Builder,
    make_uuid,
    make_keyframe,
    uniform_face,
    png_from_pixels,
    hex_to_rgba,
    color_lerp,
    fill_rect,
    add_noise_overlay,
)
from _chain_helpers import (
    base_fill,
    jitter_inplace,
    horizontal_grain,
    crack_lines_radial,
    specular_cluster,
    emissive_seam,
    shifted_face,
)

OUTPUT_PATH = "D:/CC/ChaosCraft/src/main/resources/models/chain/me_attacks/spectral_chain_forest.bbmodel"
RES = 128


# ---------------------------------------------------------------------------
# TEXTURE 0 — GHOST IRON (spectral chain links / anchors)
# ---------------------------------------------------------------------------

def build_ghost_iron_texture():
    w = h = RES
    spectral_navy = hex_to_rgba("#06080e")
    ghost_iron    = hex_to_rgba("#181e2c")
    spec_surface  = hex_to_rgba("#304058")
    bright_edge   = hex_to_rgba("#788090")
    emiss_silver  = hex_to_rgba("#a0b8c8")
    glow_high     = hex_to_rgba("#c8d8e0")

    pixels = base_fill(w, h, ghost_iron)

    # Vertical stratification — spectral silver-blue with soft glow zones
    for y in range(h):
        t = y / (h - 1)
        if t < 0.10:
            c = glow_high
        elif t < 0.30:
            local = (t - 0.10) / 0.20
            c = color_lerp(glow_high, emiss_silver, local)
        elif t < 0.55:
            local = (t - 0.30) / 0.25
            c = color_lerp(emiss_silver, bright_edge, local)
        elif t < 0.78:
            local = (t - 0.55) / 0.23
            c = color_lerp(bright_edge, spec_surface, local)
        else:
            local = (t - 0.78) / 0.22
            c = color_lerp(spec_surface, spectral_navy, local)
        for x in range(w):
            pixels[y * w + x] = c

    # Fine HORIZONTAL grain of metal still visible (chain links)
    horizontal_grain(pixels, w, h, spec_surface, density=0.55, seed=37)
    horizontal_grain(pixels, w, h, bright_edge, density=0.30, seed=71)

    # Soft silver-blue glow overlay at darker zones (the spectral nature)
    for y in range(int(h * 0.55), h):
        for x in range(w):
            if (x * 3 + y * 5) % 11 == 0:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, emiss_silver, 0.40)

    # Soft inner glow specular highlights
    specular_cluster(pixels, w, h, 16, 14, 5, glow_high, strength=0.75)
    specular_cluster(pixels, w, h, w - 20, 18, 4, emiss_silver, strength=0.65)
    specular_cluster(pixels, w, h, w // 2, 12, 3, glow_high, strength=0.55)
    specular_cluster(pixels, w, h, w // 3 + 6, 30, 3, emiss_silver, strength=0.5)

    # Emissive seam (ghost glow leaking through)
    emissive_seam(pixels, w, h, w // 4, emiss_silver, intensity=0.4, width=1)
    emissive_seam(pixels, w, h, 3 * w // 4, emiss_silver, intensity=0.4, width=1)

    add_noise_overlay(pixels, w, h, spectral_navy, bright_edge, density=0.18, seed=121)
    jitter_inplace(pixels, jitter_range=14, seed=359)
    jitter_inplace(pixels, jitter_range=8, seed=581, density=0.55)

    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# TEXTURE 1 — GROUND EMERGENCE (silver rings at emergence points / wisps)
# ---------------------------------------------------------------------------

def build_emergence_texture():
    w = h = RES
    earth        = hex_to_rgba("#040408")
    deep         = hex_to_rgba("#020204")
    ring_silver  = hex_to_rgba("#6080a0")
    ring_bright  = hex_to_rgba("#90b0c8")
    ring_glow    = hex_to_rgba("#c0d8e8")

    pixels = base_fill(w, h, earth)

    # Centre glowing spectral ring
    for y in range(h):
        for x in range(w):
            dx = x - w / 2
            dy = y - h / 2
            d = math.sqrt(dx * dx + dy * dy)
            # Ring band at d=~40% of half-width
            ring_target = w * 0.35
            ring_thickness = w * 0.12
            ring_t = 1.0 - min(1.0, abs(d - ring_target) / ring_thickness)
            if ring_t > 0:
                base = color_lerp(ring_silver, ring_glow, ring_t)
                pixels[y * w + x] = base
            else:
                t = min(1.0, d / (w * 0.7))
                pixels[y * w + x] = color_lerp(deep, earth, t)

    # Secondary inner ring
    for y in range(h):
        for x in range(w):
            dx = x - w / 2
            dy = y - h / 2
            d = math.sqrt(dx * dx + dy * dy)
            inner_target = w * 0.15
            inner_thickness = w * 0.04
            inner_t = 1.0 - min(1.0, abs(d - inner_target) / inner_thickness)
            if inner_t > 0:
                p = pixels[y * w + x]
                pixels[y * w + x] = color_lerp(p, ring_bright, 0.85 * inner_t)

    # Spectral wisps embedded as small bright dots
    import random
    rng = random.Random(53)
    for _ in range(30):
        cx = rng.randrange(2, w - 2)
        cy = rng.randrange(2, h - 2)
        d_from_center = math.sqrt((cx - w/2)**2 + (cy - h/2)**2)
        if w * 0.20 < d_from_center < w * 0.50:
            specular_cluster(pixels, w, h, cx, cy, rng.randint(1, 2), ring_bright, strength=0.55)

    add_noise_overlay(pixels, w, h, deep, ring_silver, density=0.18, seed=163)
    jitter_inplace(pixels, jitter_range=16, seed=293)
    jitter_inplace(pixels, jitter_range=8, seed=347, density=0.55)

    return png_from_pixels(pixels, w, h)


# ---------------------------------------------------------------------------
# MODEL BUILD
# ---------------------------------------------------------------------------

def build():
    b = Builder("spectral_chain_forest", resolution=(RES, RES), visible_box=(22, 14, 0))
    b.add_texture("ghost_iron", build_ghost_iron_texture())
    b.add_texture("emergence", build_emergence_texture())

    def iron_face(seed):
        return shifted_face(seed % 100, (seed * 7) % 100, 16, tex_index=0, RES=RES)
    def ground_face(seed):
        return shifted_face(seed % 100, (seed * 11) % 100, 20, tex_index=1, RES=RES)

    # =====================================================================
    # 1) 8 HANGING CHAIN ASSEMBLIES at radius 10 around circle
    #    Each chain: 5 links hanging at Y=10, 8, 6, 4, 2
    #    Plus top anchor at Y=10.5, ground emergence at Y=0, wisp at top
    # =====================================================================
    NUM_CHAINS = 8
    NUM_LINKS = 5

    chain_assemblies = []  # list of dicts: {"chain_idx", "links": [(gu,g,i)], "anchor": (gu,g), "emergence": (gu,g), "wisp": (gu,g), "ang"}

    for chain_idx in range(NUM_CHAINS):
        ang = (chain_idx / NUM_CHAINS) * 2 * math.pi
        chain_cx = math.cos(ang) * 10.0
        chain_cz = math.sin(ang) * 10.0

        # 5 chain links at Y=10, 8, 6, 4, 2
        link_data = []  # (gu, g, link_idx_top_to_bottom)
        link_y_positions = [10.0, 8.0, 6.0, 4.0, 2.0]  # top to bottom
        for li, link_y in enumerate(link_y_positions):
            # horizontal slab
            h_uuid = b.add_cube(
                f"chain{chain_idx}_link{li}_h",
                from_=[chain_cx - 0.40, link_y - 0.12, chain_cz - 0.22],
                to_=  [chain_cx + 0.40, link_y + 0.12, chain_cz + 0.22],
                faces=iron_face(40 + chain_idx * 23 + li * 5),
            )
            # vertical slab
            v_uuid = b.add_cube(
                f"chain{chain_idx}_link{li}_v",
                from_=[chain_cx - 0.22, link_y - 0.40, chain_cz - 0.12],
                to_=  [chain_cx + 0.22, link_y + 0.40, chain_cz + 0.12],
                faces=iron_face(56 + chain_idx * 19 + li * 7),
            )
            gu = make_uuid()
            rot_y = 90.0 if li % 2 == 1 else 0.0
            g = b.make_group(
                f"chain{chain_idx}_link_{li}", [chain_cx, link_y, chain_cz], [h_uuid, v_uuid],
                rotation=[0, rot_y, 0], group_uuid=gu,
            )
            link_data.append((gu, g, li))

        # Top anchor — 2 small cubes at top
        anchor_cubes = []
        for ai, (dy, hw) in enumerate([(0.4, 0.35), (0.0, 0.25)]):
            cu = b.add_cube(
                f"chain{chain_idx}_anchor_{ai+1}",
                from_=[chain_cx - hw, 10.5 + dy - 0.1, chain_cz - hw],
                to_=  [chain_cx + hw, 10.5 + dy + 0.1, chain_cz + hw],
                faces=iron_face(170 + chain_idx * 11 + ai * 3),
            )
            anchor_cubes.append(cu)
        anchor_gu = make_uuid()
        anchor_g = b.make_group(
            f"chain{chain_idx}_anchor", [chain_cx, 10.7, chain_cz], anchor_cubes,
            group_uuid=anchor_gu,
        )

        # Ground emergence point
        em_uuid = b.add_cube(
            f"chain{chain_idx}_emerge",
            from_=[chain_cx - 1.0, -0.05, chain_cz - 1.0],
            to_=  [chain_cx + 1.0, 0.10, chain_cz + 1.0],
            faces=ground_face(230 + chain_idx * 17),
        )
        em_gu = make_uuid()
        em_g = b.make_group(
            f"chain{chain_idx}_emergence", [chain_cx, 0.025, chain_cz], [em_uuid],
            group_uuid=em_gu,
        )

        # Spectral wisp at top
        wisp_uuid = b.add_cube(
            f"chain{chain_idx}_wisp",
            from_=[chain_cx - 0.45, 11.0, chain_cz - 0.45],
            to_=  [chain_cx + 0.45, 11.5, chain_cz + 0.45],
            faces=ground_face(290 + chain_idx * 19),
        )
        wisp_gu = make_uuid()
        wisp_g = b.make_group(
            f"chain{chain_idx}_wisp", [chain_cx, 11.25, chain_cz], [wisp_uuid],
            rotation=[0, chain_idx * 22, 0], group_uuid=wisp_gu,
        )

        chain_assemblies.append({
            "chain_idx": chain_idx,
            "links": link_data,
            "anchor": (anchor_gu, anchor_g),
            "emergence": (em_gu, em_g),
            "wisp": (wisp_gu, wisp_g),
            "ang": ang,
            "cx": chain_cx,
            "cz": chain_cz,
        })

    # ROOT
    root_children = []
    for asm in chain_assemblies:
        for (gu, g, _li) in asm["links"]:
            root_children.append(g)
        root_children.append(asm["anchor"][1])
        root_children.append(asm["emergence"][1])
        root_children.append(asm["wisp"][1])

    root_uuid = make_uuid()
    root_group = b.make_group("root", [0, 0, 0], root_children, group_uuid=root_uuid)
    b.outliner.append(root_group)

    # ===========================================================================
    # ANIMATION — SPAWN (1.0s)
    # Emergence glows 0.0s. Link 1 (bottom) erupts 0.2s, each link 0.06s stagger upward.
    # Top anchor at last link arrival. Wisps materialise.
    # ===========================================================================
    spawn = {}

    # Emergence points glow first
    for asm in chain_assemblies:
        gu, g = asm["emergence"]
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", 0.05, 1.4, 1.4, 1.4),
            make_keyframe("scale", 0.18, 1, 1, 1),
            make_keyframe("scale", 1.0, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Chain links — bottom-to-top stagger (so li=4 is bottom, li=0 is top)
    # link_y_positions = [10, 8, 6, 4, 2] -> li=0 is highest, li=4 is lowest
    # Bottom-first stagger: li=4 first
    for asm in chain_assemblies:
        for (gu, g, li) in asm["links"]:
            stagger_order = 4 - li  # 0..4
            delay = 0.20 + stagger_order * 0.06
            link_y = [10.0, 8.0, 6.0, 4.0, 2.0][li]
            kfs = [
                make_keyframe("position", 0.0, 0, -link_y, 0),
                make_keyframe("position", delay, 0, -link_y, 0),
                make_keyframe("position", delay + 0.15, 0, 0.3, 0),
                make_keyframe("position", delay + 0.25, 0, 0, 0),
                make_keyframe("position", 1.0, 0, 0, 0),
                make_keyframe("scale", 0.0, 0, 0, 0),
                make_keyframe("scale", delay, 0, 0, 0),
                make_keyframe("scale", delay + 0.10, 1.3, 1.3, 1.3),
                make_keyframe("scale", delay + 0.22, 1, 1, 1),
                make_keyframe("scale", 1.0, 1, 1, 1),
            ]
            spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Top anchor appears when all links up (after stagger 4 -> 0.20 + 4*0.06 + 0.25 = 0.69)
    for asm in chain_assemblies:
        gu, g = asm["anchor"]
        delay = 0.70
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.10, 1.2, 1.2, 1.2),
            make_keyframe("scale", delay + 0.18, 1, 1, 1),
            make_keyframe("scale", 1.0, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Wisps materialise at top
    for ai, asm in enumerate(chain_assemblies):
        gu, g = asm["wisp"]
        delay = 0.82 + ai * 0.015
        kfs = [
            make_keyframe("scale", 0.0, 0, 0, 0),
            make_keyframe("scale", delay, 0, 0, 0),
            make_keyframe("scale", delay + 0.10, 1.3, 1.3, 1.3),
            make_keyframe("scale", 1.0, 1, 1, 1),
        ]
        spawn[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("spawn", length=1.0, animators=spawn, loop="once", override=True)

    # ===========================================================================
    # ANIMATION — IDLE (6.0s)
    # Each chain sways in own direction and period — 4-6s slow, 2-3s faster.
    # Within each chain: 0.15s lag per link from the anchor.
    # Wisps drift slowly. Emergence points pulse emissive.
    # ===========================================================================
    idle = {}

    # Per-chain period and direction
    chain_params = []
    import random
    rng = random.Random(151)
    for ci in range(NUM_CHAINS):
        # mix of slow and fast periods
        if ci % 3 == 0:
            period = 2.0 + rng.random() * 1.0  # fast
        else:
            period = 4.0 + rng.random() * 2.0  # slow
        sway_x = (rng.random() - 0.5) * 2 * 9.0  # ±9° in X
        sway_z = (rng.random() - 0.5) * 2 * 9.0  # ±9° in Z
        phase_offset = rng.random() * 2 * math.pi
        chain_params.append((period, sway_x, sway_z, phase_offset))

    # Chain links — each link lags 0.15s per link DOWN from anchor (li=0 is top, near anchor)
    for asm in chain_assemblies:
        ci = asm["chain_idx"]
        period, sway_x, sway_z, ph_off = chain_params[ci]
        for (gu, g, li) in asm["links"]:
            lag = li * 0.15  # li=0 (top) leads, li=4 (bottom) lags most
            kfs = []
            for k in range(13):
                t = (k / 12) * 6.0
                ph = ((t - lag) / period) * 2 * math.pi + ph_off
                rx = math.sin(ph) * sway_x * (0.5 + li * 0.2)
                rz = math.cos(ph * 0.85) * sway_z * (0.5 + li * 0.2)
                kfs.append(make_keyframe("rotation", t, rx, 0, rz))
            idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Anchors hold mostly still but slight wobble
    for asm in chain_assemblies:
        ci = asm["chain_idx"]
        period, sway_x, sway_z, ph_off = chain_params[ci]
        gu, g = asm["anchor"]
        kfs = []
        for k in range(9):
            t = (k / 8) * 6.0
            ph = (t / period) * 2 * math.pi + ph_off
            rx = math.sin(ph) * sway_x * 0.2
            rz = math.cos(ph) * sway_z * 0.2
            kfs.append(make_keyframe("rotation", t, rx, 0, rz))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Wisps drift independently (each its own path, not orbiting)
    for ai, asm in enumerate(chain_assemblies):
        gu, g = asm["wisp"]
        kfs = []
        for k in range(13):
            t = (k / 12) * 6.0
            ph1 = (t / 3.5) * 2 * math.pi + ai * 0.7
            ph2 = (t / 4.7) * 2 * math.pi + ai * 1.1
            dx = math.sin(ph1) * 0.4
            dy = 0.3 + math.cos(ph2) * 0.5
            dz = math.cos(ph1 * 0.7) * 0.4
            kfs.append(make_keyframe("position", t, dx, dy, dz))
            kfs.append(make_keyframe("rotation", t, math.degrees(ph1) * 0.05, ai * 22 + math.degrees(ph2) * 0.03, math.degrees(ph2) * 0.05))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Emergence points pulse
    for ai, asm in enumerate(chain_assemblies):
        gu, g = asm["emergence"]
        kfs = []
        for k in range(11):
            t = (k / 10) * 6.0
            ph = (t / 2.0) * 2 * math.pi + ai * 0.5
            s = 1.0 + 0.15 * (0.5 + 0.5 * math.sin(ph))
            kfs.append(make_keyframe("scale", t, s, 1, s))
        idle[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("idle", length=6.0, animators=idle, loop="loop", override=False)

    # ===========================================================================
    # ANIMATION — DISSIPATE (0.7s)
    # Wisps dissolve first. Top anchor links vanish first, then top-to-bottom 0.06s.
    # Emergence points fade last.
    # ===========================================================================
    diss = {}

    # Wisps dissolve first
    for ai, asm in enumerate(chain_assemblies):
        gu, g = asm["wisp"]
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.10 + ai * 0.005, 1, 1, 1),
            make_keyframe("scale", 0.25 + ai * 0.005, 0, 0, 0),
            make_keyframe("scale", 0.7, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Top anchors vanish
    for asm in chain_assemblies:
        gu, g = asm["anchor"]
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.10, 1, 1, 1),
            make_keyframe("scale", 0.22, 0, 0, 0),
            make_keyframe("scale", 0.7, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Each link fades in sequence top-to-bottom (li=0 first, li=4 last) at 0.06s per link
    for asm in chain_assemblies:
        for (gu, g, li) in asm["links"]:
            start_fade = 0.20 + li * 0.06
            kfs = [
                make_keyframe("scale", 0.0, 1, 1, 1),
                make_keyframe("scale", start_fade, 1, 1, 1),
                make_keyframe("scale", min(0.65, start_fade + 0.10), 0, 0, 0),
                make_keyframe("scale", 0.7, 0, 0, 0),
            ]
            diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    # Emergence points fade last
    for ai, asm in enumerate(chain_assemblies):
        gu, g = asm["emergence"]
        kfs = [
            make_keyframe("scale", 0.0, 1, 1, 1),
            make_keyframe("scale", 0.5, 1.3, 1, 1.3),
            make_keyframe("scale", 0.7, 0, 0, 0),
        ]
        diss[gu] = {"name": g["name"], "type": "bone", "keyframes": kfs}

    b.add_animation("dissipate", length=0.7, animators=diss, loop="once", override=True)

    b.write(OUTPUT_PATH)
    sz = os.path.getsize(OUTPUT_PATH)
    print(f"Wrote {OUTPUT_PATH} ({sz} bytes, {sz/1024:.1f} KB)")


if __name__ == "__main__":
    build()
