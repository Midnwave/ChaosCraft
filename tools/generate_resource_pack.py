#!/usr/bin/env python3
"""
Resource Pack Generator for ChaosCraft Badge System.
Generates a complete resource pack ZIP with badge textures and model overrides.

Usage:
    python generate_resource_pack.py

Reads badge textures from: ./badge_textures/
Outputs: ./ChaosCraft-Badges-ResourcePack.zip

Requires: Pillow (for texture verification), but textures must already be generated
by generate_badge_textures.py first.
"""

import json
import os
import zipfile
from pathlib import Path

# Badge IDs in order (determines custom_model_data numbering, starting at 100001)
BADGE_IDS = [
    "calamity_event_1", "calamity_event_2", "1x1x1x1", "blue_moon_event_2023",
    "hallows_2023", "hallows_2024", "anniversary_2024", "april_fools_2025",
    "tutorial", "freezing_ice", "blue_moon", "fish", "sonic", "infested",
    "chef", "devils_dream", "corrupted_corruption", "doom", "chain",
    "creeper_infestation", "unpredictable_randomness", "crazy_minecrafter",
    "seer", "neko", "fallen", "bruh", "ghost", "insanity_rampage",
    "total_chaos", "chaotic_determination", "nightmare", "lost_moon",
    "bloody_eclipse", "armageddon", "oblivion", "god_eater"
]

PACK_FORMAT = 46  # 1.21.4
BASE_ITEM = "paper"  # Item to override with custom_model_data
CMD_START = 100001  # Starting custom_model_data value
TEXTURE_DIR = Path("badge_textures")
OUTPUT_ZIP = "ChaosCraft-Badges-ResourcePack.zip"


def generate_pack_mcmeta():
    return json.dumps({
        "pack": {
            "pack_format": PACK_FORMAT,
            "description": "ChaosCraft Badge Textures"
        }
    }, indent=2)


def generate_item_model():
    """Generate the paper.json model file with custom_model_data overrides."""
    overrides = []
    cmd = CMD_START

    for badge_id in BADGE_IDS:
        # Colored variant
        overrides.append({
            "predicate": {"custom_model_data": cmd},
            "model": f"chaoscraft:badge/{badge_id}"
        })
        cmd += 1

        # Grayscale variant
        overrides.append({
            "predicate": {"custom_model_data": cmd},
            "model": f"chaoscraft:badge/{badge_id}_gray"
        })
        cmd += 1

    # Question mark (shared)
    overrides.append({
        "predicate": {"custom_model_data": cmd},
        "model": "chaoscraft:badge/question_mark"
    })

    return json.dumps({
        "parent": "minecraft:item/generated",
        "textures": {
            "layer0": "minecraft:item/paper"
        },
        "overrides": overrides
    }, indent=2)


def generate_badge_model(badge_id, variant=""):
    """Generate a simple item model for a badge texture."""
    suffix = f"_{variant}" if variant else ""
    return json.dumps({
        "parent": "minecraft:item/generated",
        "textures": {
            "layer0": f"chaoscraft:badge/{badge_id}{suffix}"
        }
    }, indent=2)


def build_zip():
    texture_path = TEXTURE_DIR
    if not texture_path.exists():
        print(f"ERROR: Texture directory '{texture_path}' not found!")
        print("Run generate_badge_textures.py first to create badge textures.")
        return False

    with zipfile.ZipFile(OUTPUT_ZIP, 'w', zipfile.ZIP_DEFLATED) as zf:
        # pack.mcmeta
        zf.writestr("pack.mcmeta", generate_pack_mcmeta())
        print("[+] pack.mcmeta")

        # Paper item model override
        zf.writestr(
            f"assets/minecraft/models/item/{BASE_ITEM}.json",
            generate_item_model()
        )
        print(f"[+] assets/minecraft/models/item/{BASE_ITEM}.json")

        texture_count = 0
        model_count = 0

        for badge_id in BADGE_IDS:
            # Colored texture
            colored_file = texture_path / f"{badge_id}.png"
            if colored_file.exists():
                zf.write(colored_file, f"assets/chaoscraft/textures/badge/{badge_id}.png")
                texture_count += 1
            else:
                print(f"  WARN: Missing texture: {colored_file}")

            # Colored model
            zf.writestr(
                f"assets/chaoscraft/models/badge/{badge_id}.json",
                generate_badge_model(badge_id)
            )
            model_count += 1

            # Grayscale texture
            gray_file = texture_path / f"{badge_id}_gray.png"
            if gray_file.exists():
                zf.write(gray_file, f"assets/chaoscraft/textures/badge/{badge_id}_gray.png")
                texture_count += 1
            else:
                print(f"  WARN: Missing grayscale: {gray_file}")

            # Grayscale model
            zf.writestr(
                f"assets/chaoscraft/models/badge/{badge_id}_gray.json",
                generate_badge_model(badge_id, "gray")
            )
            model_count += 1

        # Question mark texture
        qm_file = texture_path / "question_mark.png"
        if qm_file.exists():
            zf.write(qm_file, "assets/chaoscraft/textures/badge/question_mark.png")
            texture_count += 1
        else:
            print("  WARN: Missing question_mark.png")

        # Question mark model
        zf.writestr(
            "assets/chaoscraft/models/badge/question_mark.json",
            generate_badge_model("question_mark")
        )
        model_count += 1

    print(f"\n=== Resource Pack Generated ===")
    print(f"Output: {OUTPUT_ZIP}")
    print(f"Textures: {texture_count}")
    print(f"Models: {model_count}")
    print(f"Base item: {BASE_ITEM}")
    print(f"CMD range: {CMD_START} - {CMD_START + len(BADGE_IDS) * 2}")
    print(f"Pack format: {PACK_FORMAT} (1.21.4)")

    # Print CMD mapping for reference
    print(f"\n=== Custom Model Data Mapping ===")
    cmd = CMD_START
    for badge_id in BADGE_IDS:
        print(f"  {cmd}: {badge_id} (colored)")
        cmd += 1
        print(f"  {cmd}: {badge_id} (grayscale)")
        cmd += 1
    print(f"  {cmd}: question_mark")

    return True


if __name__ == "__main__":
    os.chdir(Path(__file__).parent)
    print("ChaosCraft Badge Resource Pack Generator")
    print("=" * 45)

    if build_zip():
        print(f"\nDone! Place {OUTPUT_ZIP} in your server's resource pack folder.")
    else:
        print("\nFailed. Fix the errors above and try again.")
