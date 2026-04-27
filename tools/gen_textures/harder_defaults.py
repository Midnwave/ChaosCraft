"""Make Devils Dream ~10x harder to survive across all default configs.

Tuning per the user's 4 specific knobs:
  1. How close attacks spawn to player    → spawn.offset-radius:        8 → 3
  2. Damage amount                         → unchanged (already +50% earlier)
  3. Damage delay before first hit         → damageDelayTicks:           ÷4 (min 0)
  4. Time between damage ticks             → ticksBetweenDamage:         ÷4 (min 2)

Plus mode-level spawn rate/concurrency:
  spawn.base-interval-ticks:   45 → 18
  spawn.max-events-per-player:  6 → 12

Compound effect: tighter spawn + 2.5x faster spawn cadence + 2x more concurrent
+ 4x faster damage cooldown ≈ 10x less survival time.
"""
import os, re

ATTACK_DIR = "D:/CC/ChaosCraft/src/main/java/com/blockforge/chaoscraft/modes/devilsdream/attacks"
MODE_CONFIG = "D:/CC/ChaosCraft/src/main/java/com/blockforge/chaoscraft/modes/devilsdream/DevilsDreamConfig.java"

# Attack files to update
ATTACK_FILES = [
    "DDBlockDisplay1.java", "DDBlockDisplay2.java", "DDBlockDisplay3.java",
    "DDBlockDisplay4.java", "DDBlockDisplay5.java",
    "DDEnvFX1.java", "DDEnvFX2.java", "DDEnvFX3.java", "DDEnvFX4.java", "DDEnvFX5.java",
    "DDEnvironmental1.java", "DDEnvironmental2.java",
    "DevilsDreamModelEngine.java",
]

def divide_int(match):
    """Match config.setTicksBetweenDamage(N) → divide N by 4, min 2."""
    val = int(match.group(1))
    new_val = max(2, val // 4)
    return f"config.setTicksBetweenDamage({new_val})"

def divide_delay(match):
    """Match config.setDamageDelayTicks(N) → divide N by 4, min 0."""
    val = int(match.group(1))
    new_val = max(0, val // 4)
    return f"config.setDamageDelayTicks({new_val})"

total_tbd_changes = 0
total_dd_changes = 0
for fn in ATTACK_FILES:
    p = os.path.join(ATTACK_DIR, fn)
    with open(p, "r", encoding="utf-8") as f:
        text = f.read()

    new_text = text
    # ticksBetweenDamage(N) → ÷4
    new_text, n_tbd = re.subn(
        r"config\.setTicksBetweenDamage\((\d+)\)",
        divide_int, new_text
    )
    # damageDelayTicks(N) → ÷4
    new_text, n_dd = re.subn(
        r"config\.setDamageDelayTicks\((\d+)\)",
        divide_delay, new_text
    )

    if new_text != text:
        with open(p, "w", encoding="utf-8") as f:
            f.write(new_text)
    print(f"{fn:30s}  ticksBetweenDamage:{n_tbd:3d}  damageDelayTicks:{n_dd:3d}")
    total_tbd_changes += n_tbd
    total_dd_changes += n_dd

print(f"\nTotal: ticksBetweenDamage updates {total_tbd_changes}, damageDelayTicks updates {total_dd_changes}")

# === DevilsDreamConfig spawn settings ===
print("\n--- DevilsDreamConfig.java ---")
with open(MODE_CONFIG, "r", encoding="utf-8") as f:
    cfg = f.read()
orig = cfg

cfg = cfg.replace(
    'defaults.set("spawn.offset-radius", 8.0);',
    'defaults.set("spawn.offset-radius", 3.0);'
)
cfg = cfg.replace(
    'defaults.set("spawn.base-interval-ticks", 45);',
    'defaults.set("spawn.base-interval-ticks", 18);'
)
cfg = cfg.replace(
    'defaults.set("spawn.max-events-per-player", 6);',
    'defaults.set("spawn.max-events-per-player", 12);'
)

if cfg != orig:
    with open(MODE_CONFIG, "w", encoding="utf-8") as f:
        f.write(cfg)
    print("  spawn.offset-radius:        8.0 → 3.0")
    print("  spawn.base-interval-ticks:   45 → 18")
    print("  spawn.max-events-per-player: 6  → 12")
else:
    print("  (no changes — values may already be tuned or pattern not found)")
