package com.blockforge.chaoscraft.api.points;

/**
 * All possible point-earning actions across modes.
 * Each action has a default point value, config key, and description.
 * Modes register which actions they support.
 */
public enum PointAction {

    // === Chain Mode Actions ===
    CHAIN_DODGE_ATTACK("chain.dodge-attack", 5, "Dodge an attack (was in radius, left before damage)"),
    CHAIN_DESTROY_WEAK_POINT("chain.destroy-weak-point", 10, "Destroy an attack's breakable weak point"),
    CHAIN_SURVIVE_WAVE("chain.survive-wave", 15, "Survive a wave of attacks"),
    CHAIN_DAMAGE_ZONE_SURVIVE("chain.damage-zone-survive", 8, "Stand in attack zone 3+ seconds without taking damage"),
    CHAIN_OVERWHELM_BONUS("chain.overwhelm-bonus", 10, "Be targeted by 3+ simultaneous attacks"),
    CHAIN_COLLECT_FRAGMENT("chain.collect-fragment", 3, "Collect a chain fragment dropped by an expired attack"),
    CHAIN_DEPOSIT_FRAGMENTS("chain.deposit-fragments", 20, "Deposit collected fragments at the central point"),

    // === Corruption Mode Actions ===
    CORRUPTION_BREAK_CORRUPTED("corruption.break-corrupted", 2, "Break a corruption-placed block"),
    CORRUPTION_BREAK_25("corruption.break-25-milestone", 15, "Break 25 corrupted blocks (milestone)"),
    CORRUPTION_DESTROY_WEAK_POINT("corruption.destroy-weak-point", 10, "Destroy an attack's breakable weak point"),
    CORRUPTION_DODGE_ATTACK("corruption.dodge-attack", 5, "Dodge a corruption attack"),
    CORRUPTION_SURVIVE_DARKNESS("corruption.survive-darkness", 8, "Touch a cursed floating block and survive the darkness"),
    CORRUPTION_FRONTIER("corruption.frontier-bonus", 5, "Be at the corruption spread edge when it expands"),
    CORRUPTION_COLLECT_SHARD("corruption.collect-shard", 3, "Collect a corruption shard from an expired attack"),
    CORRUPTION_PURIFY_CHUNK("corruption.purify-chunk", 25, "Purify a chunk by breaking enough corruption blocks"),

    // === Universal Actions (available in all modes) ===
    SURVIVE_MINUTE("universal.survive-minute", 10, "Survive for 1 minute"),
    SURVIVE_FULL_TIMER("universal.survive-full-timer", 500, "Survive the full mode timer");

    private final String configKey;
    private final int defaultPoints;
    private final String description;

    PointAction(String configKey, int defaultPoints, String description) {
        this.configKey = configKey;
        this.defaultPoints = defaultPoints;
        this.description = description;
    }

    public String getConfigKey() { return configKey; }
    public int getDefaultPoints() { return defaultPoints; }
    public String getDescription() { return description; }

    public static PointAction fromConfigKey(String key) {
        for (PointAction action : values()) {
            if (action.configKey.equalsIgnoreCase(key)) return action;
        }
        return null;
    }
}
