"""Replace hardcoded `getModelScale()` overrides with `config.setModelengineScale()` calls.
This way the scale is the YAML default (configurable by users) instead of being hardcoded
and bypassing the config system entirely.

Strategy: for each `@Override protected double getModelScale() { return X; }` line,
1. capture X
2. find the matching attack class's constructor
3. inject `config.setModelengineScale("X");` after the last `config.set*(...)` line
4. delete the override line
"""
import re

PATH = "D:/CC/ChaosCraft/src/main/java/com/blockforge/chaoscraft/modes/devilsdream/attacks/DevilsDreamModelEngine.java"

with open(PATH, "r", encoding="utf-8") as f:
    text = f.read()

# Find all (id, scale) pairs
# Pattern: getModelId returns "<id>"; followed (within same class) by getModelScale returns <X>;
pairs = []
pattern = re.compile(
    r'@Override protected String getModelId\(\) \{ return "([a-z_]+)"; \}\s*\n\s*'
    r'@Override protected double getModelScale\(\) \{ return ([0-9.]+); \}',
    re.MULTILINE,
)
for m in pattern.finditer(text):
    pairs.append((m.group(1), m.group(2)))

print(f"Found {len(pairs)} (id, scale) pairs")

# Build a map from model_id -> scale string
scale_map = dict(pairs)

# 1) Remove every `@Override protected double getModelScale() { return X; }` line (with leading whitespace)
text = re.sub(
    r'\n[ \t]+@Override protected double getModelScale\(\) \{ return [0-9.]+; \}',
    '',
    text,
)

# 2) For each attack class, inject `config.setModelengineScale("X");` in the constructor.
# Each class follows pattern:
#   public ClassName(ChaosCraftPlugin plugin) {
#       super(plugin, new AttackConfig("<id>", AttackType.MODEL_ENGINE, 1, MODE_PATH));
#       config.setDamage(...);
#       ...
#   }
# We'll find the line `super(plugin, new AttackConfig("<id>", ...))` and inject after the
# last `config.set*(...)` in that constructor (before closing `}`).
# Simpler approach: find ` new AttackConfig("<id>"` and inject `config.setModelengineScale("X");`
# right after the closing `));` of the super() call.

def inject(m):
    indent = m.group(1)
    super_call = m.group(0)
    model_id = m.group(2)
    if model_id not in scale_map:
        return super_call
    scale = scale_map[model_id]
    return super_call + "\n" + indent + f'config.setModelengineScale("{scale}");'

# Match super(plugin, new AttackConfig("model_id", AttackType.MODEL_ENGINE, 1, MODE_PATH));
super_pattern = re.compile(
    r'^([ \t]+)super\(plugin, new AttackConfig\("([a-z_]+)", AttackType\.MODEL_ENGINE, \d+, MODE_PATH\)\);',
    re.MULTILINE,
)
new_text, count = super_pattern.subn(inject, text)
print(f"Injected setModelengineScale on {count} constructors")

with open(PATH, "w", encoding="utf-8") as f:
    f.write(new_text)

# Sanity check
with open(PATH, "r", encoding="utf-8") as f:
    final = f.read()
remaining_overrides = final.count("getModelScale()")
remaining_setters = final.count("setModelengineScale(")
print(f"Remaining getModelScale() lines: {remaining_overrides}")
print(f"setModelengineScale() calls: {remaining_setters}")
