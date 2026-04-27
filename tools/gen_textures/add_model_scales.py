"""Add `getModelScale()` overrides to all 25 attack classes in DevilsDreamModelEngine.java.
Matches the pattern used by BlueMoonModelEngine/DoomModelEngine.

Per-attack scale chosen by bbox + intent:
- Big architectural models (cathedrals, throne, planetarium, dream_itself): smaller scale
- Long thin projectiles (feather, blade, beam, shards, comet): bigger scale to make features visible
- Already-big spans (spine, crown): moderate scale
"""
import re

PATH = "D:/CC/ChaosCraft/src/main/java/com/blockforge/chaoscraft/modes/devilsdream/attacks/DevilsDreamModelEngine.java"

SCALES = {
    "fallen_angel_descent":      4.0,
    "nightmare_root_surge":      4.0,
    "devils_spine_array":        3.0,
    "fallen_halo_burst":         4.0,
    "dream_collapse_ring":       4.0,
    "devils_sermon_nova":        4.0,
    "nightmare_static_field":    3.5,
    "infernal_crown_burst":      3.0,
    "fallen_feather_lance":      4.0,
    "nightmare_shard_volley":    4.0,
    "devils_tongue_beam":        3.5,
    "silver_wing_blade":         4.0,
    "nightmare_eye_projectile":  5.0,
    "blood_comet":               4.0,
    "fallen_angel_wings_summon": 3.0,
    "nightmare_cathedral":       3.0,
    "devils_halo_array":         4.0,
    "bone_throne_summon":        4.0,
    "silver_mirror_portal":      3.0,
    "devils_constellation":      4.0,
    "nightmare_clock":           4.0,
    "fallen_seraph_skeleton":    3.0,
    "nightmare_planetarium":     3.0,
    "infernal_scripture_array":  4.0,
    "the_dream_itself":          2.5,
}

with open(PATH, "r", encoding="utf-8") as f:
    text = f.read()

# Skip if already has scale overrides
if "getModelScale()" in text:
    print("Already has getModelScale() — exiting without modification.")
    raise SystemExit(0)

# For each model id, find `@Override protected String getModelId() { return "<id>"; }`
# and inject a new line right after with `@Override protected double getModelScale() { return X; }`
def inject(match):
    indent = match.group(1)
    line = match.group(0)
    model_id = match.group(2)
    if model_id not in SCALES:
        return line
    scale = SCALES[model_id]
    return line + "\n" + indent + f"@Override protected double getModelScale() {{ return {scale}; }}"

pattern = re.compile(
    r'^([ \t]+)@Override protected String getModelId\(\) \{ return "([a-z_]+)"; \}',
    re.MULTILINE,
)
new_text, count = pattern.subn(inject, text)

with open(PATH, "w", encoding="utf-8") as f:
    f.write(new_text)

print(f"Inserted getModelScale() override on {count} attacks.")
for k, v in SCALES.items():
    print(f"  {k:30s} -> {v}x")
