#!/usr/bin/env python3
"""
generate_shop_configs.py

Reads EconomyShopGUI shop and section configuration files and generates
ChaosCraft-format shop YAML configs with 4x multiplied prices.

Usage:
    python generate_shop_configs.py

Input:  D:/CC/archive-2026-03-20T150626Z/EconomyShopGUI/shops/*.yml
        D:/CC/archive-2026-03-20T150626Z/EconomyShopGUI/sections/*.yml
Output: D:/CC/ChaosCraft/tools/shop_configs/<category>.yml
"""

import os
import sys
import glob

try:
    import yaml
except ImportError:
    print("PyYAML not found. Installing...")
    os.system(f"{sys.executable} -m pip install pyyaml")
    import yaml


# ---------------------------------------------------------------------------
# Paths
# ---------------------------------------------------------------------------
SHOPS_DIR = "D:/CC/archive-2026-03-20T150626Z/EconomyShopGUI/shops"
SECTIONS_DIR = "D:/CC/archive-2026-03-20T150626Z/EconomyShopGUI/sections"
OUTPUT_DIR = "D:/CC/ChaosCraft/tools/shop_configs"

PRICE_MULTIPLIER = 4


# ---------------------------------------------------------------------------
# Hex-color map for category display names.
# Sourced from the section configs' legacy color codes, converted to hex.
# ---------------------------------------------------------------------------
LEGACY_TO_HEX = {
    "&0": "#000000",  # black
    "&1": "#0000AA",  # dark blue
    "&2": "#00AA00",  # dark green
    "&3": "#00AAAA",  # dark aqua
    "&4": "#AA0000",  # dark red
    "&5": "#AA00AA",  # dark purple
    "&6": "#FFAA00",  # gold
    "&7": "#AAAAAA",  # gray
    "&8": "#555555",  # dark gray
    "&9": "#5555FF",  # blue
    "&a": "#55FF55",  # green
    "&b": "#55FFFF",  # aqua
    "&c": "#FF5555",  # red
    "&d": "#FF55FF",  # light purple
    "&e": "#FFFF55",  # yellow
    "&f": "#FFFFFF",  # white
}

# Fallback colors if a section config is missing or unparseable
FALLBACK_COLORS = {
    "Blocks": "#8B4513",
    "Decoration": "#5555FF",
    "Dyes": "#00AA00",
    "Enchanting": "#FF55FF",
    "Farming": "#55FF55",
    "Food": "#FFAA00",
    "Miscellaneous": "#FF5555",
    "Mobs": "#AAAAAA",
    "Music": "#FFAA00",
    "Ores": "#FFD700",
    "Potions": "#FF55FF",
    "Redstone": "#FF5555",
    "SpawnEggs": "#555555",
    "Spawners": "#555555",
    "Workstations": "#AAAAAA",
    "Z_EverythingElse": "#555555",
}


def load_yaml(path: str) -> dict:
    """Load a YAML file and return its contents as a dict."""
    with open(path, "r", encoding="utf-8") as f:
        return yaml.safe_load(f) or {}


def parse_section(section_path: str) -> dict:
    """
    Parse a section config and return:
      { "display_name": str, "icon": str, "hex_color": str }
    """
    data = load_yaml(section_path)
    item = data.get("item", {})
    raw_name = item.get("displayname", "") or item.get("name", "")
    material = item.get("material", "STONE")

    # Extract the legacy color code (e.g. "&2&l") and the plain name
    hex_color = None
    plain_name = raw_name

    # Strip formatting codes like &2&l, &c&l etc.
    import re
    # Find the first color code (non-format)
    match = re.match(r"(&[0-9a-fA-F])", raw_name)
    if match:
        code = match.group(1).lower()
        hex_color = LEGACY_TO_HEX.get(code)

    # Remove all & formatting codes to get plain name
    plain_name = re.sub(r"&[0-9a-fA-Fk-oK-OrR]", "", raw_name).strip()

    return {
        "display_name": plain_name,
        "icon": material,
        "hex_color": hex_color,
    }


def multiply_price(price, multiplier: float) -> float:
    """Multiply a price by the given multiplier, preserving -1 as disabled."""
    if price is None:
        return -1
    price = float(price)
    if price == -1:
        return -1
    return round(price * multiplier, 2)


def parse_shop_items(shop_path: str) -> list:
    """
    Parse an EconomyShopGUI shop file and return a list of dicts:
      [ { "material": str, "buy": float, "sell": float }, ... ]

    Items are collected from all pages. Duplicate materials (e.g. POTION
    with different potion types, SPAWNER with different spawner types,
    ENCHANTED_BOOK with different enchantments) get a suffix to make them
    unique keys in the output.
    """
    data = load_yaml(shop_path)
    pages = data.get("pages", {})
    items = []

    for page_key in sorted(pages.keys()):
        page = pages[page_key]
        if not page or "items" not in page:
            continue
        page_items = page["items"]
        for slot_key in sorted(page_items.keys(), key=lambda x: int(x)):
            entry = page_items[slot_key]
            if not entry or "material" not in entry:
                continue

            material = entry["material"]
            buy = entry.get("buy", -1)
            sell = entry.get("sell", -1)

            # Build a unique key for items that share the same material
            unique_key = material

            # Spawners: append spawner type
            if "spawnertype" in entry:
                unique_key = f"{entry['spawnertype']}_SPAWNER"

            # Potions / tipped arrows: append potion type
            if "potiontypes" in entry:
                ptypes = entry["potiontypes"]
                if ptypes:
                    ptype = ptypes[0].upper().replace(" ", "_")
                    unique_key = f"{material}_{ptype}"

            # Enchanted books: append enchantment
            if "enchantments" in entry:
                enchants = entry["enchantments"]
                if enchants:
                    ench = enchants[0].upper().replace(":", "_")
                    unique_key = f"ENCHANTED_BOOK_{ench}"

            items.append({
                "key": unique_key,
                "material": material,
                "buy": buy,
                "sell": sell,
            })

    return items


def generate_output(category: str, section_info: dict, items: list, multiplier: float) -> str:
    """
    Generate ChaosCraft shop YAML content as a string.
    """
    hex_color = section_info.get("hex_color") or FALLBACK_COLORS.get(category, "#FFFFFF")
    display_name = section_info.get("display_name") or category
    icon = section_info.get("icon") or "STONE"

    # Build the hex color without the # for the &#RRGGBB format
    hex_stripped = hex_color.lstrip("#")

    lines = []
    lines.append(f"# Auto-generated shop config — prices are {multiplier}x EconomyShopGUI defaults")
    lines.append(f"config-version: 1")
    lines.append(f'display-name: "&#{ hex_stripped }{ display_name }"')
    lines.append(f"icon: {icon}")
    lines.append("items:")

    for item in items:
        key = item["key"]
        buy = multiply_price(item["buy"], multiplier)
        sell = multiply_price(item["sell"], multiplier)

        # Format prices: show as int if whole number, else 2 decimal places
        buy_str = format_price(buy)
        sell_str = format_price(sell)

        lines.append(f"  {key}:")
        lines.append(f"    buy: {buy_str}")
        lines.append(f"    sell: {sell_str}")
        lines.append(f"    kills-required: 0")
        lines.append(f"    s-kills-required: 0")
        lines.append(f"    survivals-required: 0")
        lines.append(f'    badge-required: ""')
        lines.append(f"    lore: []")

    return "\n".join(lines) + "\n"


def format_price(price: float) -> str:
    """Format a price value for YAML output."""
    if price == -1:
        return "-1"
    if price == int(price):
        return f"{int(price)}.0"
    return f"{price}"


def main():
    # Ensure output directory exists
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    shop_files = sorted(glob.glob(os.path.join(SHOPS_DIR, "*.yml")))

    if not shop_files:
        print(f"ERROR: No shop files found in {SHOPS_DIR}")
        sys.exit(1)

    total_items = 0
    total_categories = 0
    min_buy = float("inf")
    max_buy = 0.0
    min_sell = float("inf")
    max_sell = 0.0

    print(f"Reading shop files from: {SHOPS_DIR}")
    print(f"Reading section configs from: {SECTIONS_DIR}")
    print(f"Output directory: {OUTPUT_DIR}")
    print(f"Price multiplier: {PRICE_MULTIPLIER}x")
    print("-" * 60)

    for shop_path in shop_files:
        filename = os.path.basename(shop_path)
        category = os.path.splitext(filename)[0]

        # Load section info
        section_path = os.path.join(SECTIONS_DIR, filename)
        if os.path.exists(section_path):
            section_info = parse_section(section_path)
        else:
            section_info = {
                "display_name": category,
                "icon": "STONE",
                "hex_color": FALLBACK_COLORS.get(category, "#FFFFFF"),
            }

        # Parse shop items
        items = parse_shop_items(shop_path)
        if not items:
            print(f"  SKIP  {category:25s} — no items found")
            continue

        # Track stats
        total_categories += 1
        total_items += len(items)

        for item in items:
            buy = multiply_price(item["buy"], PRICE_MULTIPLIER)
            sell = multiply_price(item["sell"], PRICE_MULTIPLIER)
            if buy != -1:
                min_buy = min(min_buy, buy)
                max_buy = max(max_buy, buy)
            if sell != -1:
                min_sell = min(min_sell, sell)
                max_sell = max(max_sell, sell)

        # Generate and write output
        output_content = generate_output(category, section_info, items, PRICE_MULTIPLIER)
        output_path = os.path.join(OUTPUT_DIR, f"{category}.yml")
        with open(output_path, "w", encoding="utf-8") as f:
            f.write(output_content)

        print(f"  OK    {category:25s} — {len(items):4d} items -> {output_path}")

    print("-" * 60)
    print(f"\nSummary:")
    print(f"  Categories generated:  {total_categories}")
    print(f"  Total items:           {total_items}")
    if min_buy != float("inf"):
        print(f"  Buy price range:       {format_price(min_buy)} — {format_price(max_buy)}")
    if min_sell != float("inf"):
        print(f"  Sell price range:      {format_price(min_sell)} — {format_price(max_sell)}")
    print(f"\nAll configs written to: {OUTPUT_DIR}")


if __name__ == "__main__":
    main()
