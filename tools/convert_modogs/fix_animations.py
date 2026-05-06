#!/usr/bin/env python3
"""
Strip `animation.<breed>.` prefix from all dog bbmodel animation names so
ModelEngine 4 auto-binds `walk`/`idle` to vanilla movement state.

Also forces walk-style animations to override=False (loop=loop) so they
play alongside other animations rather than overriding them.

Run from repo root.
"""
import json
import sys
from pathlib import Path

DOGS_DIR = Path("src/main/resources/models/fluffy/dogs")

# Animations that should auto-bind to ME's vanilla state machine.
# These need override=false and loop=loop.
AUTO_STATE_NAMES = {"walk", "idle"}


def strip_prefix(name: str) -> str:
    """animation.whippet.walk -> walk;  walk -> walk"""
    if name.startswith("animation."):
        parts = name.split(".", 2)
        if len(parts) == 3:
            return parts[2]
    return name


def fix_bbmodel(path: Path) -> dict:
    with path.open(encoding="utf-8") as f:
        data = json.load(f)

    changed_anims = 0
    for anim in data.get("animations", []):
        old = anim.get("name", "")
        new = strip_prefix(old)
        if new != old:
            anim["name"] = new
            changed_anims += 1
        # Auto-bind animations need override=false + loop=loop
        if anim.get("name") in AUTO_STATE_NAMES:
            anim["override"] = False
            anim["loop"] = "loop"
        # Angry variants: also let them coexist
        if anim.get("name", "").startswith("angry"):
            anim["override"] = False
            anim["loop"] = "loop"

    with path.open("w", encoding="utf-8") as f:
        json.dump(data, f, separators=(",", ":"))

    return {"file": path.name, "renamed": changed_anims}


def main():
    if not DOGS_DIR.exists():
        print(f"ERROR: {DOGS_DIR} not found. Run from D:/CC/ChaosCraft.", file=sys.stderr)
        sys.exit(1)

    files = sorted(DOGS_DIR.glob("*.bbmodel"))
    if not files:
        print("No dog bbmodels found.")
        sys.exit(1)

    total_renamed = 0
    for path in files:
        result = fix_bbmodel(path)
        total_renamed += result["renamed"]

    print(f"Fixed {len(files)} bbmodels — renamed {total_renamed} animations total.")


if __name__ == "__main__":
    main()
