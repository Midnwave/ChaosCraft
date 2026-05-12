#!/usr/bin/env python3
"""Quick stats on FreezingIce bbmodel files."""
import json
import os

ATTACKS = [
    "glacial_floor_shatter",
    "absolute_zero_spike_forest",
    "glacier_fist_eruption",
    "tundra_crack_array",
    "cryo_pillar_cross",
    "blizzard_nova",
    "frozen_time_ring",
    "crystalline_aurora_burst",
    "cryo_pressure_collapse",
    "ice_age_terminus",
]

BASE = "D:/CC/ChaosCraft/src/main/resources/models/freezingice/me_attacks"


def count_bones(outliner):
    count = 0
    for item in outliner:
        if isinstance(item, dict):
            count += 1
            count += count_bones(item.get("children", []))
    return count


def main():
    print(f"{'Attack':<32}{'Size KB':>10}{'Cubes':>8}{'Bones':>8}{'Anims':>8}")
    print("-" * 66)
    for name in ATTACKS:
        path = f"{BASE}/{name}.bbmodel"
        sz = os.path.getsize(path) / 1024.0
        with open(path, "r", encoding="utf-8") as f:
            doc = json.load(f)
        cubes = len(doc.get("elements", []))
        bones = count_bones(doc.get("outliner", []))
        anims = len(doc.get("animations", []))
        print(f"{name:<32}{sz:>10.1f}{cubes:>8}{bones:>8}{anims:>8}")


if __name__ == "__main__":
    main()
