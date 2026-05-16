#!/usr/bin/env python3
"""Bake 12x harder values into FreezingIce per-attack constructor defaults.

Transforms config.setX(...) calls in-place per the rules:
- setDamage / setImpactDamage         -> x * 12
- setDamageRadius / setImpactRadius   -> min(18.0, x * 1.5)
- setTicksBetweenDamage               -> max(4, x // 12)
- setCooldownTicks                    -> max(60, x // 12)
- setDamageDelayTicks                 -> max(0, x // 12)
- setModelengineScale("N")            -> match the new (post-transform) damage radius if known
- setDurationTicks / setEnabled / setChance / setTracksPlayer /
  setDamageOnImpactOnly / setFollowAi* -> unchanged

Tracks per-file modifications and per-attack damage-radius (so the matching
setModelengineScale("...") in the same attack block can be updated).
"""

import os
import re
import sys

ATTACKS_DIR = r"D:\CC\ChaosCraft\src\main\java\com\blockforge\chaoscraft\modes\freezingice\attacks"

FILES = [
    "FreezingIceBlockDisplay.java",
    "FreezingIceBlockDisplay2.java",
    "FreezingIceBlockDisplay3.java",
    "FreezingIceBlockDisplay4.java",
    "FreezingIceBlockDisplay5.java",
    "FreezingIceBlockDisplay6.java",
    "FreezingIceBlockDisplay7.java",
    "FreezingIceBlockDisplay8.java",
    "FreezingIceEnvironmental.java",
    "FreezingIceEnvironmental2.java",
    "FreezingIceEnvironmental3.java",
    "FreezingIceEnvironmental4.java",
    "FreezingIceEnvironmental5.java",
    "FreezingIceModelEngine.java",
]


def fmt_double(v: float) -> str:
    """Format a double the way the existing code does: prefer N.0 form, but
    keep one decimal for non-integers (e.g., 12.0, 13.5)."""
    if v == int(v):
        return f"{int(v)}.0"
    # Keep up to a couple decimals, strip trailing zeros but keep at least one.
    s = f"{v:.6f}".rstrip("0").rstrip(".")
    if "." not in s:
        s += ".0"
    return s


def transform_number(method: str, raw: str) -> str:
    """Return the new literal value as a string for method(raw).

    raw is the original literal text (e.g., '150.0', '12', '0', '7.5').
    """
    # Parse number (int or float)
    is_float = "." in raw or "e" in raw or "E" in raw
    if is_float:
        x = float(raw)
    else:
        x = int(raw)

    if method in ("setDamage", "setImpactDamage"):
        new_v = x * 12
        # Always emit as a double if original looked like a double; otherwise
        # the underlying field is a double anyway, so emit double.
        return fmt_double(float(new_v))

    if method in ("setDamageRadius", "setImpactRadius"):
        new_v = min(18.0, float(x) * 1.5)
        return fmt_double(new_v)

    if method == "setTicksBetweenDamage":
        new_v = max(4, int(x) // 12)
        return str(new_v)

    if method == "setCooldownTicks":
        new_v = max(60, int(x) // 12)
        return str(new_v)

    if method == "setDamageDelayTicks":
        new_v = max(0, int(x) // 12)
        return str(new_v)

    raise ValueError(f"Unhandled method: {method}")


NUMERIC_METHODS = {
    "setDamage",
    "setImpactDamage",
    "setDamageRadius",
    "setImpactRadius",
    "setTicksBetweenDamage",
    "setCooldownTicks",
    "setDamageDelayTicks",
}

# Regex to match: config.setMethod(  <number>  )
# Captures: method name, literal number (with optional sign and decimal)
NUMERIC_CALL_RE = re.compile(
    r"config\.(set[A-Za-z]+)\(\s*(-?\d+(?:\.\d+)?)\s*\)"
)

# For setModelengineScale("..."), capture the string literal value.
MES_CALL_RE = re.compile(
    r'config\.setModelengineScale\(\s*"([^"]*)"\s*\)'
)


def process_file(path: str) -> tuple[int, int, int]:
    """Process a single file in place.

    Returns (numeric_replacements, mes_replacements, attack_blocks_seen).

    "attack_blocks_seen" counts how many distinct constructor blocks were
    found (used as a sanity check). We approximate this by counting how
    many times we reset the per-attack damage-radius tracker, which we do
    on each new method/constructor line `protected void register...` or
    similar — but since attack defaults appear in sequence inside register
    blocks delimited by attack registrations, we instead track radius
    state per-line and reset it when we see a line containing
    "registerAttack(" or "register(" or a new "{ ... AttackConfig config".
    Simpler: track only the most recent setDamageRadius value within the
    *file*, but reset it when we see a setModelengineScale or after each
    matching call. That's not robust enough.

    Better approach: scan lines in order. Maintain `current_radius` =
    most-recent transformed setDamageRadius value (as a string). When we
    hit setModelengineScale("..."), replace its content with
    current_radius. Reset current_radius whenever we encounter the
    `AttackConfig config = new AttackConfig` line (start of a new attack
    block).
    """
    with open(path, "r", encoding="utf-8") as f:
        text = f.read()

    lines = text.split("\n")
    out_lines = []
    numeric_count = 0
    mes_count = 0
    attack_blocks = 0
    current_radius_str = None  # most recent transformed damage radius for THIS attack
    # Used to recognize the start of a new attack block
    attack_start_re = re.compile(r"new\s+AttackConfig\b")

    for line in lines:
        if attack_start_re.search(line):
            attack_blocks += 1
            current_radius_str = None

        # First, handle numeric calls (multiple may be on same line).
        def numeric_sub(m):
            nonlocal numeric_count, current_radius_str
            method = m.group(1)
            raw = m.group(2)
            if method not in NUMERIC_METHODS:
                return m.group(0)
            new_val = transform_number(method, raw)
            numeric_count += 1
            if method == "setDamageRadius":
                current_radius_str = new_val
            return f"config.{method}({new_val})"

        new_line = NUMERIC_CALL_RE.sub(numeric_sub, line)

        # Then, handle setModelengineScale("..."). If we have a known
        # current_radius_str, use it. Otherwise leave unchanged.
        def mes_sub(m):
            nonlocal mes_count
            if current_radius_str is None:
                return m.group(0)
            mes_count += 1
            return f'config.setModelengineScale("{current_radius_str}")'

        new_line = MES_CALL_RE.sub(mes_sub, new_line)

        out_lines.append(new_line)

    new_text = "\n".join(out_lines)
    if new_text != text:
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(new_text)

    return numeric_count, mes_count, attack_blocks


def main():
    total_numeric = 0
    total_mes = 0
    total_attacks = 0
    print(f"Processing {len(FILES)} files in {ATTACKS_DIR}\n")
    for fname in FILES:
        path = os.path.join(ATTACKS_DIR, fname)
        if not os.path.isfile(path):
            print(f"  MISSING: {fname}")
            continue
        n, m, a = process_file(path)
        total_numeric += n
        total_mes += m
        total_attacks += a
        print(f"  {fname:<40s}  attacks={a:3d}  numeric={n:3d}  ME={m:2d}")

    print()
    print(f"TOTAL: attacks={total_attacks}  numeric_calls_rewritten={total_numeric}  modelengine_scale_rewritten={total_mes}")


if __name__ == "__main__":
    main()
