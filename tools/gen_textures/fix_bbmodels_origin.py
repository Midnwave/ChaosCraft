"""Add `origin` and `rotation` fields to every element in every bbmodel.
ModelEngine requires `cubeOrigin` (the bbmodel `origin` field) to be non-null.
Set origin = [0, 0, 0] (geometry pivot at origin) and rotation = [0, 0, 0] for every cube.
"""
import json, os

MODELS_DIR = "D:/CC/ChaosCraft/src/main/resources/models/devilsdream"

def fix(path):
    with open(path, "r", encoding="utf-8") as f:
        m = json.load(f)
    elems = m.get("elements", [])
    n_added_origin = 0
    n_added_rot = 0
    for e in elems:
        if "origin" not in e or e.get("origin") is None:
            # default origin: midpoint of from/to (geometric center) — neutral pivot
            f_, t_ = e.get("from", [0, 0, 0]), e.get("to", [0, 0, 0])
            e["origin"] = [
                (f_[0] + t_[0]) / 2.0,
                (f_[1] + t_[1]) / 2.0,
                (f_[2] + t_[2]) / 2.0,
            ]
            n_added_origin += 1
        if "rotation" not in e or e.get("rotation") is None:
            e["rotation"] = [0, 0, 0]
            n_added_rot += 1
    with open(path, "w", encoding="utf-8") as f:
        json.dump(m, f, indent=2)
    return n_added_origin, n_added_rot

total_o = 0
total_r = 0
files = sorted(f for f in os.listdir(MODELS_DIR) if f.endswith(".bbmodel"))
for f in files:
    a, b = fix(os.path.join(MODELS_DIR, f))
    total_o += a; total_r += b
    print(f"{f:40s} +origin={a:4d}  +rotation={b:4d}")
print(f"\nTotal: +origin={total_o}  +rotation={total_r}  ({len(files)} files)")
