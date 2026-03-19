package com.blockforge.chaoscraft.services.claims;

/**
 * Configurable protection flags for claims.
 * Each flag has a config key, description, and default value.
 */
public enum ClaimFlag {
    // Standard GP flags
    MOB_GRIEFING("mob-griefing", "Mob griefing (creeper explosions, enderman, etc.)", false),
    EXPLOSIONS("explosions", "TNT and other explosions", false),
    PVP("pvp", "Player vs Player combat", false),
    FIRE_SPREAD("fire-spread", "Fire spreading to/from claim", false),
    MOB_SPAWNING("mob-spawning", "Natural mob spawning", true),
    ENDERMAN_PICKUP("enderman-pickup", "Endermen picking up blocks", false),
    PISTON_PROTECTION("piston-protection", "Pistons pushing into/out of claim", true),
    LEAF_DECAY("leaf-decay", "Leaf block decay", true),

    // ChaosCraft-specific flags
    BLOCK_DISPLAY_DAMAGE("block-display-damage", "Mode attacks deal damage inside claims", false),
    CORRUPTION_SPREAD("corruption-spread", "Corruption enters claims", false),
    MODE_EVENTS("mode-events", "Mode events (block displays) spawn inside claims", false);

    private final String configKey;
    private final String description;
    private final boolean defaultValue;

    ClaimFlag(String configKey, String description, boolean defaultValue) {
        this.configKey = configKey;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    public String getConfigKey() { return configKey; }
    public String getDescription() { return description; }
    public boolean getDefaultValue() { return defaultValue; }

    public static ClaimFlag fromConfigKey(String key) {
        for (ClaimFlag flag : values()) {
            if (flag.configKey.equalsIgnoreCase(key)) return flag;
        }
        return null;
    }
}
