"""Fix all 25 Devils Dream bbmodels for ModelEngine import:
   1. Replace short UUIDs (e001, b001, k001, etc.) with valid UUID4 strings,
      keeping cross-references (outliner.children, animators keys) consistent.
   2. Add `override: false` to every animation (ModelEngine requires non-null).
   3. Add a `hitbox` bone if missing (silences warnings).
"""
import json, os, re, uuid

MODELS_DIR = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream"
UUID_RE = re.compile(r"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", re.I)

def looks_like_uuid(s):
    return isinstance(s, str) and UUID_RE.match(s) is not None

def collect_short_ids(node, ids):
    """Walk JSON, collect all string values that look like short IDs (not real UUIDs)."""
    if isinstance(node, dict):
        for k, v in node.items():
            if k == "uuid" and isinstance(v, str) and not looks_like_uuid(v):
                ids.add(v)
            collect_short_ids(v, ids)
    elif isinstance(node, list):
        for v in node:
            collect_short_ids(v, ids)

def remap(node, mapping):
    """Walk JSON, replace short IDs with mapped UUIDs in uuid fields, children lists, and animator keys."""
    if isinstance(node, dict):
        new = {}
        for k, v in node.items():
            new_key = mapping.get(k, k) if k in mapping else k
            if k == "uuid" and isinstance(v, str) and v in mapping:
                new[new_key] = mapping[v]
            elif k == "children" and isinstance(v, list):
                new[new_key] = [remap(c, mapping) if isinstance(c, dict) else (mapping.get(c, c) if isinstance(c, str) else c) for c in v]
            else:
                new[new_key] = remap(v, mapping)
        return new
    elif isinstance(node, list):
        return [remap(v, mapping) for v in node]
    elif isinstance(node, str) and node in mapping:
        return mapping[node]
    else:
        return node

def fix_animations(model):
    """Ensure every animation has override:false, plus default fields ME expects."""
    anims = model.get("animations", [])
    for a in anims:
        if "override" not in a or a.get("override") is None:
            a["override"] = False
        a.setdefault("anim_time_update", "")
        a.setdefault("blend_weight", "")
        a.setdefault("start_delay", "")
        a.setdefault("loop_delay", "")
        a.setdefault("snapping", 24)
        a.setdefault("selected", False)
        a.setdefault("saved", False)
    return model

def ensure_hitbox(model):
    """Add a minimal hitbox bone+element if missing (silences ModelEngine warning)."""
    outliner = model.get("outliner", [])
    elements = model.get("elements", [])
    has = any(
        (isinstance(b, dict) and b.get("name", "").lower() == "hitbox")
        for b in outliner
    )
    if has:
        return model
    elem_uuid = str(uuid.uuid4())
    bone_uuid = str(uuid.uuid4())
    elements.append({
        "name": "hitbox_box",
        "box_uv": False, "rescale": False, "locked": False,
        "render_order": "default", "allow_mirror_modeling": True,
        "from": [-0.5, 0, -0.5], "to": [0.5, 1, 0.5],
        "autouv": 0, "color": 0, "uuid": elem_uuid,
        "faces": {
            "north": {"uv": [0, 0, 1, 1], "texture": None},
            "east":  {"uv": [0, 0, 1, 1], "texture": None},
            "south": {"uv": [0, 0, 1, 1], "texture": None},
            "west":  {"uv": [0, 0, 1, 1], "texture": None},
            "up":    {"uv": [0, 0, 1, 1], "texture": None},
            "down":  {"uv": [0, 0, 1, 1], "texture": None},
        },
    })
    outliner.append({
        "name": "hitbox",
        "origin": [0, 0, 0], "rotation": [0, 0, 0],
        "uuid": bone_uuid, "export": True, "isOpen": True,
        "children": [elem_uuid],
    })
    model["outliner"] = outliner
    model["elements"] = elements
    return model

def fix_file(path):
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)

    short_ids = set()
    collect_short_ids(data, short_ids)
    mapping = {sid: str(uuid.uuid4()) for sid in short_ids}

    # Need special handling for animator keys (dict keys, not values).
    def fix_animator_keys(node):
        if isinstance(node, dict):
            out = {}
            for k, v in node.items():
                new_k = mapping.get(k, k) if isinstance(k, str) else k
                out[new_k] = fix_animator_keys(v)
            return out
        elif isinstance(node, list):
            return [fix_animator_keys(v) for v in node]
        return node

    data = remap(data, mapping)
    data = fix_animator_keys(data)
    data = fix_animations(data)
    data = ensure_hitbox(data)

    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
    return len(mapping)

def main():
    bbmodels = sorted(f for f in os.listdir(MODELS_DIR) if f.endswith(".bbmodel"))
    total_remapped = 0
    for f in bbmodels:
        path = os.path.join(MODELS_DIR, f)
        before = os.path.getsize(path)
        n = fix_file(path)
        after = os.path.getsize(path)
        total_remapped += n
        print(f"{f:40s} short_ids={n:5d}  size {before:8d} -> {after:8d}")
    print(f"\nTotal short IDs remapped across {len(bbmodels)} models: {total_remapped}")

if __name__ == "__main__":
    main()
