package com.blockforge.chaoscraft.services.stats;

import java.util.UUID;

/**
 * Immutable snapshot of a player's lifetime stats.
 * Kills, S-Kills, and Mode Survivals are lifetime counters — never spent or reduced.
 */
public record PlayerStats(UUID uuid, int kills, int sKills, int modeSurvivals) {

    public static PlayerStats empty(UUID uuid) {
        return new PlayerStats(uuid, 0, 0, 0);
    }

    public boolean meetsRequirements(int requiredKills, int requiredSKills, int requiredSurvivals) {
        return kills >= requiredKills && sKills >= requiredSKills && modeSurvivals >= requiredSurvivals;
    }
}
