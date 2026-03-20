package com.blockforge.chaoscraft.services.mobspawn;

import org.bukkit.entity.EntityType;

/**
 * Represents a single mob type that can be spawned during a mode.
 * Supports both MythicMobs (by string ID) and vanilla mobs (by EntityType).
 *
 * <p>Configuration example in YAML:
 * <pre>
 *   mobs:
 *     - id: "NightmareHound"      # MythicMobs internal mob ID
 *       type: mythicmobs           # "mythicmobs" or "vanilla"
 *       weight: 10                 # Spawn weight (higher = more likely)
 *       min-count: 1              # Minimum mobs per spawn wave
 *       max-count: 3              # Maximum mobs per spawn wave
 *       health-multiplier: 1.0    # Multiplied against the mob's base health
 *       damage-multiplier: 1.0    # Multiplied against the mob's base damage
 *
 *     - id: "ZOMBIE"              # Vanilla EntityType name
 *       type: vanilla
 *       weight: 8
 *       min-count: 2
 *       max-count: 5
 * </pre>
 */
public class MobSpawnEntry {

    private final String id;
    private final MobType type;
    private final int weight;
    private final int minCount;
    private final int maxCount;
    private final double healthMultiplier;
    private final double damageMultiplier;

    /**
     * Whether this entry references a MythicMobs mob or a vanilla Minecraft mob.
     */
    public enum MobType {
        /** A MythicMobs mob — spawned via MythicMobs API using the internal mob ID. */
        MYTHICMOBS,
        /** A vanilla Minecraft mob — spawned via World.spawnEntity() using EntityType. */
        VANILLA
    }

    public MobSpawnEntry(String id, MobType type, int weight, int minCount, int maxCount,
                         double healthMultiplier, double damageMultiplier) {
        this.id = id;
        this.type = type;
        this.weight = Math.max(1, weight);
        this.minCount = Math.max(1, minCount);
        this.maxCount = Math.max(this.minCount, maxCount);
        this.healthMultiplier = Math.max(0.1, healthMultiplier);
        this.damageMultiplier = Math.max(0.1, damageMultiplier);
    }

    /**
     * The mob identifier.
     * For MythicMobs: the internal mob ID (e.g. "NightmareHound").
     * For vanilla: the EntityType name (e.g. "ZOMBIE", "SKELETON").
     */
    public String getId() { return id; }

    /** Whether this is a MythicMobs mob or vanilla mob. */
    public MobType getType() { return type; }

    /**
     * Spawn weight — higher values make this mob more likely to be chosen.
     * The probability of this mob being selected = weight / totalWeight.
     */
    public int getWeight() { return weight; }

    /** Minimum number of this mob type to spawn per wave. Always >= 1. */
    public int getMinCount() { return minCount; }

    /** Maximum number of this mob type to spawn per wave. Always >= minCount. */
    public int getMaxCount() { return maxCount; }

    /**
     * Health multiplier applied to this mob after spawning.
     * 1.0 = normal health, 2.0 = double health, etc.
     * For MythicMobs this modifies the entity's max health attribute after spawn.
     */
    public double getHealthMultiplier() { return healthMultiplier; }

    /**
     * Damage multiplier applied to this mob after spawning.
     * 1.0 = normal damage, 2.0 = double damage.
     * For MythicMobs this modifies the entity's attack damage attribute after spawn.
     */
    public double getDamageMultiplier() { return damageMultiplier; }

    /**
     * Resolve the vanilla EntityType from the id string.
     * Returns null if the id does not match any known EntityType.
     */
    public EntityType resolveEntityType() {
        if (type != MobType.VANILLA) return null;
        try {
            return EntityType.valueOf(id.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return type.name().toLowerCase() + ":" + id + " (w=" + weight
                + ", " + minCount + "-" + maxCount + ", hp×" + healthMultiplier + ", dmg×" + damageMultiplier + ")";
    }
}
