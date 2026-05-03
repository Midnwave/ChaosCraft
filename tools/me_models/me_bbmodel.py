#!/usr/bin/env python3
"""
Shared toolkit for building ModelEngine 4 .bbmodel files for Fluffy Mode VFX.

Format constraints (do not override):
  - format_version "4.10", model_format "free", box_uv False
  - All UUIDs are proper uuid4 strings (8-4-4-4-12 hex)
  - Every animation has an "override" field (true except for idle/ambient)
  - Every cube element has an "origin" field (cubeOrigin null = ME load failure)
  - Every face uses [u1, v1, u2, v2] format (per-face, not box_uv shorthand)
  - Texture is base64-embedded PNG in textures[0].source

Usage:
    from me_bbmodel import (
        Builder, make_uuid, png_from_pixels, basic_cube_faces
    )
    b = Builder("paw_slam_boop", resolution=(64, 64))
    body_uuid = b.add_cube("paw_pad", from_=[-3, 0, -2], to_=[3, 1, 2],
                            faces=basic_cube_faces(0, 0, 6, 1, 4, tex_index=0))
    b.add_root_group("root", [0, 0, 0], children=[body_uuid])
    b.add_animation("spawn", length=1.0, loop="once", override=True, animators={...})
    b.add_texture("paw_main", png_bytes)
    b.write("/path/to/paw_slam_boop.bbmodel")
"""

import json
import struct
import zlib
import uuid
import base64
import math
from pathlib import Path

MODEL_CORE = "AbF52V0UFVOpXR61kMJRlT4VlVVV1M"
FORMAT_VERSION = "4.10"


def make_uuid() -> str:
    return str(uuid.uuid4())


def basic_cube_faces(u, v, sx, sy, sz, tex_index=0):
    """Standard box-UV-like layout converted to per-face [u1,v1,u2,v2]."""
    return {
        "north": {"uv": [u + sz, v + sz, u + sz + sx, v + sz + sy], "texture": tex_index},
        "east":  {"uv": [u, v + sz, u + sz, v + sz + sy], "texture": tex_index},
        "south": {"uv": [u + sz + sx + sz, v + sz, u + sz + sx + sz + sx, v + sz + sy], "texture": tex_index},
        "west":  {"uv": [u + sz + sx, v + sz, u + sz + sx + sz, v + sz + sy], "texture": tex_index},
        "up":    {"uv": [u + sz + sx, v + sz, u + sz, v], "texture": tex_index},
        "down":  {"uv": [u + sz + sx + sx, v, u + sz + sx, v + sz], "texture": tex_index},
    }


def uniform_face(u1, v1, u2, v2, tex_index=0):
    """Shorthand: every face uses the same UV rectangle (uniform texture mapping)."""
    return {
        d: {"uv": [u1, v1, u2, v2], "texture": tex_index}
        for d in ("north", "east", "south", "west", "up", "down")
    }


def png_from_pixels(pixels, width, height):
    """
    Build a PNG byte string from a flat list of (r,g,b,a) tuples (or 4-tuples).
    Stdlib only — no Pillow dependency.
    Returns bytes ready for base64 encoding.
    """
    assert len(pixels) == width * height, f"Expected {width*height} pixels, got {len(pixels)}"

    def chunk(tag, data):
        crc = zlib.crc32(tag + data)
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", crc & 0xFFFFFFFF)

    sig = b"\x89PNG\r\n\x1a\n"
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)  # 8-bit RGBA

    # Build raw scanline data with filter byte 0 (no filter) per row
    raw = bytearray()
    for y in range(height):
        raw.append(0)
        for x in range(width):
            r, g, bl, a = pixels[y * width + x]
            raw.extend([r & 0xFF, g & 0xFF, bl & 0xFF, a & 0xFF])
    idat = zlib.compress(bytes(raw), 9)

    return sig + chunk(b"IHDR", ihdr) + chunk(b"IDAT", idat) + chunk(b"IEND", b"")


def hex_to_rgba(h, a=255):
    """#rrggbb -> (r, g, b, a)"""
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def make_keyframe(channel, time, x=0.0, y=0.0, z=0.0, interpolation="linear"):
    """Build a single keyframe entry."""
    return {
        "channel": channel,
        "data_points": [{"x": x, "y": y, "z": z}],
        "uuid": make_uuid(),
        "time": float(time),
        "color": -1,
        "interpolation": interpolation,
        "bezier_linked": True,
        "bezier_left_time": [-0.1, -0.1, -0.1],
        "bezier_left_value": [0, 0, 0],
        "bezier_right_time": [0.1, 0.1, 0.1],
        "bezier_right_value": [0, 0, 0],
    }


def make_animator(bone_name, bone_uuid, keyframes):
    return (bone_uuid, {
        "name": bone_name,
        "type": "bone",
        "keyframes": keyframes,
    })


class Builder:
    def __init__(self, name: str, resolution=(64, 64), visible_box=(3, 3, 0)):
        self.name = name
        self.resolution = resolution
        self.visible_box = list(visible_box)
        self.elements = []
        self.outliner = []
        self.textures = []
        self.animations = []
        self._element_uuids = []

    def add_cube(self, name, from_, to_, faces, origin=None, rotation=None) -> str:
        """Add a cube element. Returns its UUID for use in outliner children."""
        if origin is None:
            origin = [(from_[i] + to_[i]) / 2 for i in range(3)]
        if rotation is None:
            rotation = [0, 0, 0]
        cube_uuid = make_uuid()
        self.elements.append({
            "name": name,
            "box_uv": False,
            "rescale": False,
            "locked": False,
            "from": list(from_),
            "to": list(to_),
            "autouv": 0,
            "color": 0,
            "origin": list(origin),
            "rotation": list(rotation),
            "faces": faces,
            "type": "cube",
            "uuid": cube_uuid,
        })
        self._element_uuids.append(cube_uuid)
        return cube_uuid

    def make_group(self, name, origin, children, rotation=None, group_uuid=None):
        """Build a group dict (NOT added to outliner — caller decides where it goes)."""
        if rotation is None:
            rotation = [0, 0, 0]
        if group_uuid is None:
            group_uuid = make_uuid()
        return {
            "name": name,
            "origin": list(origin),
            "color": 0,
            "uuid": group_uuid,
            "export": True,
            "mirror_uv": False,
            "isOpen": True,
            "locked": False,
            "visibility": True,
            "autouv": 0,
            "rotation": list(rotation),
            "children": list(children),
        }

    def add_root_group(self, name, origin, children, rotation=None) -> str:
        g = self.make_group(name, origin, children, rotation)
        self.outliner.append(g)
        return g["uuid"]

    def add_texture(self, name, png_bytes):
        b64 = base64.b64encode(png_bytes).decode("ascii")
        idx = len(self.textures)
        self.textures.append({
            "id": str(idx),
            "uuid": make_uuid(),
            "name": f"{name}.png",
            "folder": "block",
            "namespace": "",
            "particle": False,
            "render_mode": "default",
            "render_sides": "auto",
            "frame_time": 1,
            "frame_order_type": "loop",
            "frame_order": "",
            "frame_interpolate": False,
            "visible": True,
            "mode": "bitmap",
            "saved": True,
            "relative_path": f"../{name}.png",
            "path": "",
            "source": f"data:image/png;base64,{b64}",
        })

    def add_animation(self, name, length, animators, loop="once", override=True, snapping=20):
        """
        animators: dict {bone_uuid: {"name": bone_name, "type": "bone", "keyframes": [...]}}
        """
        self.animations.append({
            "uuid": make_uuid(),
            "name": name,
            "loop": loop,
            "override": override,
            "length": float(length),
            "snapping": snapping,
            "selected": False,
            "anim_time_update": "",
            "blend_weight": "",
            "start_delay": "",
            "loop_delay": "",
            "animators": animators,
        })

    def to_dict(self):
        return {
            "model_core": MODEL_CORE,
            "meta": {
                "format_version": FORMAT_VERSION,
                "model_format": "free",
                "box_uv": False,
            },
            "name": self.name,
            "model_identifier": "",
            "visible_box": self.visible_box,
            "variable_placeholders": "",
            "variable_placeholder_buttons": [],
            "timeline_setups": [],
            "unhandled_root_fields": {},
            "resolution": {"width": self.resolution[0], "height": self.resolution[1]},
            "elements": self.elements,
            "outliner": self.outliner,
            "textures": self.textures,
            "animations": self.animations,
        }

    def write(self, path):
        path = Path(path)
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open("w", encoding="utf-8") as f:
            json.dump(self.to_dict(), f, separators=(",", ":"))
        return path


def lerp(a, b, t):
    return a + (b - a) * t


def color_lerp(c1, c2, t):
    """Linear-interpolate between two RGBA tuples."""
    return tuple(int(round(lerp(c1[i], c2[i], t))) for i in range(4))


def fill_rect(pixels, w, h, x0, y0, x1, y1, color):
    """Fill a rectangle in the flat pixel list with the given RGBA color."""
    for y in range(max(0, y0), min(h, y1)):
        for x in range(max(0, x0), min(w, x1)):
            pixels[y * w + x] = color


def gradient_radial(pixels, w, h, cx, cy, max_radius, inner_color, outer_color):
    """Radial gradient from inner_color at (cx,cy) to outer_color at max_radius."""
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            t = min(1.0, d / max_radius)
            pixels[y * w + x] = color_lerp(inner_color, outer_color, t)


def gradient_horizontal_band(pixels, w, h, y0, y1, top_color, bottom_color):
    """Vertical gradient between two y values (top_color at y0, bottom_color at y1)."""
    for y in range(y0, y1):
        t = (y - y0) / max(1, y1 - y0 - 1)
        c = color_lerp(top_color, bottom_color, t)
        for x in range(w):
            pixels[y * w + x] = c


def add_noise_overlay(pixels, w, h, dark_color, light_color, density=0.05, seed=42):
    """Sprinkle random pixels for shimmer/glitter. Deterministic via seed."""
    import random
    rng = random.Random(seed)
    target = int(w * h * density)
    for _ in range(target):
        x = rng.randrange(w)
        y = rng.randrange(h)
        c = light_color if rng.random() < 0.5 else dark_color
        pixels[y * w + x] = c
