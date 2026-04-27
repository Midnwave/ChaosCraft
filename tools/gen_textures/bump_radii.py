"""Bump damage/impact radius for ground-slam style attacks across DevilsDream.

Strategy:
  - setImpactRadius(N)  → N * 1.6  (impact-only attacks: meteor/falling/slam types
                                     should have a WIDE blast zone matching the visual)
  - For attacks that ALSO set damageOnImpactOnly(true), the damageRadius is unused.
  - For continuous-AOE attacks, leave setDamageRadius alone — those are tuned for
    area control and were already bumped in earlier work.

Per-attack interpretation: setImpactRadius is the strong signal that the attack
is an impact-style (meteor, slam, drop, eruption). Bump those uniformly.
"""
import os, re

ATTACK_DIR = "D:/CC/ChaosCraft/src/main/java/com/blockforge/chaoscraft/modes/devilsdream/attacks"

ATTACK_FILES = [
    "DDBlockDisplay1.java", "DDBlockDisplay2.java", "DDBlockDisplay3.java",
    "DDBlockDisplay4.java", "DDBlockDisplay5.java",
    "DDEnvFX1.java", "DDEnvFX2.java", "DDEnvFX3.java", "DDEnvFX4.java", "DDEnvFX5.java",
    "DDEnvironmental1.java", "DDEnvironmental2.java",
    "DevilsDreamModelEngine.java",
]

def bump_impact(match):
    val = float(match.group(1))
    new_val = round(val * 1.6, 1)
    return f"config.setImpactRadius({new_val})"

total = 0
for fn in ATTACK_FILES:
    p = os.path.join(ATTACK_DIR, fn)
    with open(p, "r", encoding="utf-8") as f:
        text = f.read()
    new_text, n = re.subn(
        r"config\.setImpactRadius\(([\d.]+)\)",
        bump_impact, text
    )
    if n > 0:
        with open(p, "w", encoding="utf-8") as f:
            f.write(new_text)
    print(f"{fn:30s}  setImpactRadius bumped: {n}")
    total += n
print(f"\nTotal impact-radius updates: {total} (multiplier 1.6x)")
