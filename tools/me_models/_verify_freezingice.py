#!/usr/bin/env python3
"""Quick verifier — load each generated bbmodel, count cubes/bones/anims, validate."""
import json
import os
import sys

ATTACKS = [
    "cryo_lance", "avalanche_shot", "frozen_comet_me", "frost_breath_beam",
    "icicle_volley_me", "frozen_sentinel_array", "permafrost_throne",
    "cryo_satellite_ring", "ice_wyrm_orbital", "frozen_clock_me",
]
DIR = "D:/CC/ChaosCraft/src/main/resources/models/freezingice/me_attacks"


def count_bones(outliner):
    """Recursive bone count."""
    n = 0
    for o in outliner:
        if isinstance(o, dict):
            n += 1
            n += count_bones(o.get("children", []))
    return n


for name in ATTACKS:
    path = os.path.join(DIR, f"{name}.bbmodel")
    size = os.path.getsize(path)
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    cubes = len(data["elements"])
    bones = count_bones(data["outliner"])
    anims = len(data["animations"])
    # Check format constraints
    fmt_ok = (
        data["meta"]["format_version"] == "4.10" and
        data["meta"]["model_format"] == "free" and
        data["meta"]["box_uv"] is False
    )
    # Every cube has origin
    cubes_ok = all(("origin" in e) for e in data["elements"])
    # Every animation has override
    anims_ok = all(("override" in a) for a in data["animations"])
    # Every face is per-face uv [u1,v1,u2,v2]
    faces_ok = True
    for e in data["elements"]:
        for d, face in e["faces"].items():
            if "uv" not in face or len(face["uv"]) != 4:
                faces_ok = False
                break
    status = "OK" if (fmt_ok and cubes_ok and anims_ok and faces_ok and size >= 300000) else "FAIL"
    print(f"{status}  {name}: {size//1024} KB | cubes={cubes} bones={bones} anims={anims} "
          f"fmt={fmt_ok} origin={cubes_ok} override={anims_ok} faces={faces_ok}")
