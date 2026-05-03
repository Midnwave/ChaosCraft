#!/usr/bin/env python3
"""
modogs (Geckolib geo.json + animation.json + texture variants) -> ModelEngine bbmodel converter.

Per-breed-and-variant output: one .bbmodel per (breed, texture variant) combination.

Key conversions:
  - Y -= 24 on every pivot/origin/from/to (Bedrock pivot convention -> Blockbench free format)
  - Animation X-position channels negated (Geckolib -> Blockbench)
  - Per-face UVs computed from Bedrock box_uv [u, v] shorthand
  - Cube rotations not in {0, +-22.5, +-45} or with multiple non-zero axes get wrapped
    in a parent outliner group with the original rotation, cube rotation set to [0, 0, 0].
    (User tip: 'create new bone, set cube rotation to 0, set bone to original rotation'.)
  - Texture embedded as base64 in textures[0].source
  - Proper UUID4 strings for everything

Usage:
    python convert_modogs.py <modogs_root> <output_dir>
"""

import json
import base64
import sys
import uuid
from pathlib import Path

ALLOWED_CUBE_ROTATIONS = {0.0, 22.5, -22.5, 45.0, -45.0}
# Geckolib geos use Y=0 at the entity's feet (not the Bedrock-vanilla Y=24 convention),
# so no Y offset is applied. The entity stands naturally on Y=0.
Y_OFFSET = 0
MODEL_CORE = "AbF52V0UFVOpXR61kMJRlT4VlVVV1M"  # opaque, reused from working squirrel sample

# Bone name remapping for ModelEngine special-prefix recognition.
# - Bones starting with `h_` are flagged by ModelEngine as the head bone
#   and auto-rotate with the entity's view direction.
BONE_NAME_REMAP = {
    "head": "h_head",
}


def remap_bone_name(name: str) -> str:
    return BONE_NAME_REMAP.get(name, name)


# Tail bones get rotated to point up-in-the-air by default.
# -115 X is the user-tested value that gives a natural tail-up posture
# across the modogs breed set. All tail animation keyframes are offset
# by (-115 - original_rotation_x) so any tail-wag animations still play,
# just in the up-pose orientation.
TAIL_BONE_NAMES = {"tail"}
TAIL_TARGET_X_ROTATION = -115.0

# Multiplier baked into every animation's length and keyframe times.
# Keep this at 1.0 — speed is controlled at runtime via:
#   - MythicMobs state mechanic: state{mid=model;s=walk;sp=2.0}
#   - ModelEngine API: animationHandler.playAnimation(name, lerpIn, lerpOut, speed=2.0, force)
ANIMATION_SPEED_MULTIPLIER = 1.0


def compute_tail_offsets(bones):
    """For every tail bone, compute the rotation offset to add to animation keyframes."""
    offsets = {}
    for bone in bones:
        if bone["name"] in TAIL_BONE_NAMES:
            orig = bone.get("rotation", [0.0, 0.0, 0.0])
            offsets[bone["name"]] = [
                TAIL_TARGET_X_ROTATION - orig[0],
                0.0 - orig[1],
                0.0 - orig[2],
            ]
    return offsets


def override_tail_rotation(bone_name, original_rotation):
    if bone_name in TAIL_BONE_NAMES:
        return [TAIL_TARGET_X_ROTATION, 0.0, 0.0]
    return original_rotation


def make_uuid() -> str:
    return str(uuid.uuid4())


def is_allowed_cube_rotation(rot):
    """Single non-zero axis with value in the allowed set."""
    nonzero = [r for r in rot if r != 0]
    if len(nonzero) > 1:
        return False
    if not nonzero:
        return True
    return nonzero[0] in ALLOWED_CUBE_ROTATIONS


def calc_uv_per_face(uv_value, size, mirror=False):
    """
    Convert Geckolib UV to Blockbench per-face uv [u1, v1, u2, v2].
    Handles both formats:
      - Box-UV shorthand:  uv = [u, v]
      - Per-face explicit: uv = {"north": {"uv": [u, v], "uv_size": [w, h]}, ...}
    """
    # Per-face explicit format
    if isinstance(uv_value, dict):
        faces = {}
        for face_name in ("north", "east", "south", "west", "up", "down"):
            face_data = uv_value.get(face_name)
            if face_data is None:
                # Missing face: zero-area UV
                faces[face_name] = [0, 0, 0, 0]
                continue
            u1, v1 = face_data["uv"]
            uw, vh = face_data.get("uv_size", [0, 0])
            faces[face_name] = [u1, v1, u1 + uw, v1 + vh]
        if mirror:
            for face in ("east", "west"):
                f = faces[face]
                faces[face] = [f[2], f[1], f[0], f[3]]
        return faces

    # Box-UV shorthand
    u, v = uv_value
    sx, sy, sz = size
    faces = {
        "north": [u + sz,             v + sz, u + sz + sx,           v + sz + sy],
        "east":  [u,                  v + sz, u + sz,                v + sz + sy],
        "south": [u + sz + sx + sz,   v + sz, u + sz + sx + sz + sx, v + sz + sy],
        "west":  [u + sz + sx,        v + sz, u + sz + sx + sz,      v + sz + sy],
        "up":    [u + sz + sx,        v + sz, u + sz,                v],
        "down":  [u + sz + sx + sx,   v,      u + sz + sx,           v + sz],
    }
    if mirror:
        for face in ("east", "west"):
            f = faces[face]
            faces[face] = [f[2], f[1], f[0], f[3]]
    return faces


def build_cube_element(cube, parent_pivot, name_hint):
    """Build a bbmodel cube element. Returns (element_dict, optional_wrapper_group)."""
    origin = cube["origin"]
    size = cube["size"]
    uv_origin = cube.get("uv", [0, 0])
    inflate = cube.get("inflate", 0)
    mirror = cube.get("mirror", False)
    cube_rotation = cube.get("rotation", [0, 0, 0])
    cube_pivot = cube.get("pivot", parent_pivot)

    # Apply Y-24 offset to from/to and origin
    f = [origin[0] - inflate,
         origin[1] - Y_OFFSET - inflate,
         origin[2] - inflate]
    t = [origin[0] + size[0] + inflate,
         origin[1] - Y_OFFSET + size[1] + inflate,
         origin[2] + size[2] + inflate]
    cube_origin = [cube_pivot[0], cube_pivot[1] - Y_OFFSET, cube_pivot[2]]

    # Per-face UVs
    faces = {fn: {"uv": uv, "texture": 0} for fn, uv in calc_uv_per_face(uv_origin, size, mirror).items()}

    cube_uuid = make_uuid()
    needs_wrapper = cube_rotation != [0, 0, 0] and not is_allowed_cube_rotation(cube_rotation)

    element = {
        "name": name_hint,
        "box_uv": False,
        "rescale": False,
        "locked": False,
        "from": f,
        "to": t,
        "autouv": 0,
        "color": 0,
        "origin": cube_origin,
        "rotation": [0, 0, 0] if needs_wrapper else cube_rotation,
        "faces": faces,
        "type": "cube",
        "uuid": cube_uuid,
    }

    if needs_wrapper:
        wrapper = {
            "name": f"{name_hint}_rot",
            "origin": cube_origin,
            "color": 0,
            "uuid": make_uuid(),
            "export": True,
            "mirror_uv": False,
            "isOpen": True,
            "locked": False,
            "visibility": True,
            "autouv": 0,
            "rotation": cube_rotation,
            "children": [cube_uuid],
        }
        return element, wrapper
    return element, None


def convert_bone(bone, bones_by_name, all_elements, bone_uuids):
    """Recursively convert a Geckolib bone (and its child bones) into a bbmodel outliner group."""
    bone_name = bone["name"]
    pivot = bone.get("pivot", [0, Y_OFFSET, 0])
    rotation = override_tail_rotation(bone_name, bone.get("rotation", [0, 0, 0]))

    children = []

    # Cubes belonging directly to this bone
    for i, cube in enumerate(bone.get("cubes", [])):
        name_hint = f"{bone_name}_{i}" if i > 0 else bone_name
        element, wrapper = build_cube_element(cube, pivot, name_hint)
        all_elements.append(element)
        if wrapper:
            children.append(wrapper)
        else:
            children.append(element["uuid"])

    # Child bones
    for child_bone in [b for b in bones_by_name.values() if b.get("parent") == bone_name]:
        children.append(convert_bone(child_bone, bones_by_name, all_elements, bone_uuids))

    return {
        "name": remap_bone_name(bone_name),
        "origin": [pivot[0], pivot[1] - Y_OFFSET, pivot[2]],
        "color": 0,
        "uuid": bone_uuids[bone_name],
        "export": True,
        "mirror_uv": False,
        "isOpen": True,
        "locked": False,
        "visibility": True,
        "autouv": 0,
        "rotation": rotation,
        "children": children,
    }


def convert_animation(anim_name, anim_data, bone_uuids, tail_offsets=None):
    tail_offsets = tail_offsets or {}
    animators = {}
    for bone_name, channels in anim_data.get("bones", {}).items():
        if bone_name not in bone_uuids:
            continue
        keyframes = []
        for channel_name in ("rotation", "position", "scale"):
            if channel_name not in channels:
                continue
            ch = channels[channel_name]
            entries = []
            if isinstance(ch, dict):
                for time_str, vec_data in ch.items():
                    try:
                        t = float(time_str)
                    except (TypeError, ValueError):
                        continue
                    v = vec_data.get("vector", [0, 0, 0]) if isinstance(vec_data, dict) else vec_data
                    entries.append((t, v))
            elif isinstance(ch, list):
                entries.append((0.0, ch))
            else:
                continue

            for t, v in entries:
                v = list(v) if v else [0, 0, 0]
                while len(v) < 3:
                    v.append(0)
                if channel_name == "position":
                    v = [-v[0], v[1], v[2]]
                if channel_name == "rotation" and bone_name in tail_offsets:
                    off = tail_offsets[bone_name]
                    v = [v[0] + off[0], v[1] + off[1], v[2] + off[2]]
                keyframes.append({
                    "channel": channel_name,
                    "data_points": [{"x": v[0], "y": v[1], "z": v[2]}],
                    "uuid": make_uuid(),
                    "time": t / ANIMATION_SPEED_MULTIPLIER,
                    "color": -1,
                    "interpolation": "linear",
                    "bezier_linked": True,
                    "bezier_left_time": [-0.1, -0.1, -0.1],
                    "bezier_left_value": [0, 0, 0],
                    "bezier_right_time": [0.1, 0.1, 0.1],
                    "bezier_right_value": [0, 0, 0],
                })
        if keyframes:
            animators[bone_uuids[bone_name]] = {
                "name": bone_name,
                "type": "bone",
                "keyframes": keyframes,
            }

    is_idle = "idle" in anim_name.lower()
    return {
        "uuid": make_uuid(),
        "name": anim_name,
        "loop": "loop" if anim_data.get("loop", False) else "once",
        "override": not is_idle,
        "length": anim_data.get("animation_length", 1.0) / ANIMATION_SPEED_MULTIPLIER,
        "snapping": 20,
        "selected": False,
        "anim_time_update": "",
        "blend_weight": "",
        "start_delay": "",
        "loop_delay": "",
        "animators": animators,
    }


def read_png_dimensions(png_path: Path):
    with png_path.open("rb") as f:
        data = f.read(24)
    if len(data) < 24 or data[:8] != b"\x89PNG\r\n\x1a\n":
        return None, None
    width = int.from_bytes(data[16:20], "big")
    height = int.from_bytes(data[20:24], "big")
    return width, height


def build_bbmodel(geo_path: Path, anim_path: Path, texture_path: Path, model_name: str):
    geo = json.loads(geo_path.read_text(encoding="utf-8"))
    anim = json.loads(anim_path.read_text(encoding="utf-8"))

    geometry = geo["minecraft:geometry"][0]
    desc = geometry["description"]
    bones = geometry["bones"]
    tex_width = desc.get("texture_width", 64)
    tex_height = desc.get("texture_height", 64)

    bones_by_name = {b["name"]: b for b in bones}
    bone_uuids = {b["name"]: make_uuid() for b in bones}

    elements = []
    outliner_roots = []
    for root_bone in [b for b in bones if "parent" not in b]:
        outliner_roots.append(convert_bone(root_bone, bones_by_name, elements, bone_uuids))

    tail_offsets = compute_tail_offsets(bones)
    animations_list = [
        convert_animation(name, data, bone_uuids, tail_offsets)
        for name, data in anim.get("animations", {}).items()
    ]

    tex_b64 = base64.b64encode(texture_path.read_bytes()).decode("ascii")

    # Use texture's actual PNG dimensions if they differ from geo's declared values (they shouldn't, but safety)
    png_w, png_h = read_png_dimensions(texture_path)
    if png_w and png_h:
        # Trust the geo's declared dims for UV math; the bbmodel resolution is what UVs are normalised to
        tex_width = tex_width or png_w
        tex_height = tex_height or png_h

    textures_list = [{
        "id": "0",
        "uuid": make_uuid(),
        "name": f"{model_name}.png",
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
        "relative_path": f"../{model_name}.png",
        "path": "",
        "source": f"data:image/png;base64,{tex_b64}",
    }]

    return {
        "model_core": MODEL_CORE,
        "meta": {
            "format_version": "4.5",
            "model_format": "free",
            "box_uv": False,
        },
        "name": model_name,
        "model_identifier": "",
        "visible_box": [
            desc.get("visible_bounds_width", 2),
            desc.get("visible_bounds_height", 2.5),
            0,
        ],
        "variable_placeholders": "",
        "variable_placeholder_buttons": [],
        "timeline_setups": [],
        "unhandled_root_fields": {},
        "resolution": {
            "width": tex_width,
            "height": tex_height,
        },
        "elements": elements,
        "outliner": outliner_roots,
        "textures": textures_list,
        "animations": animations_list,
    }


def main():
    if len(sys.argv) < 3:
        print("Usage: python convert_modogs.py <modogs_root> <output_dir> [--filter breed]")
        sys.exit(1)

    modogs_root = Path(sys.argv[1])
    output_dir = Path(sys.argv[2])
    breed_filter = None
    if "--filter" in sys.argv:
        idx = sys.argv.index("--filter")
        breed_filter = sys.argv[idx + 1] if idx + 1 < len(sys.argv) else None

    output_dir.mkdir(parents=True, exist_ok=True)

    geo_dir = modogs_root / "assets" / "modogs" / "geo"
    anim_dir = modogs_root / "assets" / "modogs" / "animations"
    tex_dir = modogs_root / "assets" / "modogs" / "textures" / "entity"

    if not geo_dir.exists():
        print(f"ERROR: geo dir not found: {geo_dir}")
        sys.exit(1)

    converted = 0
    failed = 0
    skipped = 0

    for geo_file in sorted(geo_dir.glob("*.geo.json")):
        breed = geo_file.stem.replace(".geo", "")
        if breed_filter and breed != breed_filter:
            continue

        anim_file = anim_dir / f"{breed}.animation.json"
        breed_tex_dir = tex_dir / breed

        if not anim_file.exists():
            print(f"  SKIP {breed}: missing {anim_file.name}")
            skipped += 1
            continue
        if not breed_tex_dir.exists():
            print(f"  SKIP {breed}: missing texture dir")
            skipped += 1
            continue

        variants = sorted(breed_tex_dir.glob("*.png"))
        if not variants:
            print(f"  SKIP {breed}: no PNG variants")
            skipped += 1
            continue

        for tex_file in variants:
            variant = tex_file.stem
            try:
                bb = build_bbmodel(geo_file, anim_file, tex_file, variant)
                output_file = output_dir / f"{variant}.bbmodel"
                with output_file.open("w", encoding="utf-8") as f:
                    json.dump(bb, f, separators=(",", ":"))
                converted += 1
                size_kb = output_file.stat().st_size // 1024
                print(f"  OK   {variant}.bbmodel ({size_kb} KB)")
            except Exception as e:
                failed += 1
                print(f"  FAIL {variant}: {type(e).__name__}: {e}")

    print(f"\nDone. Converted: {converted}, failed: {failed}, breeds skipped: {skipped}")


if __name__ == "__main__":
    main()
