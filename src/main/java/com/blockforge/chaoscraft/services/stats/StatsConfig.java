package com.blockforge.chaoscraft.services.stats;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Configuration for kill values per entity type.
 * File: plugins/ChaosCraft/stats.yml
 */
public class StatsConfig {

    private static final int CURRENT_CONFIG_VERSION = 1;

    private final ChaosCraftPlugin plugin;
    private final File configFile;
    private YamlConfiguration config;

    // Entity type -> kill value
    private final Map<String, Integer> entityKillValues = new HashMap<>();
    // MythicMobs mob ID -> kill value
    private final Map<String, Integer> mythicMobKillValues = new HashMap<>();
    // Category defaults
    private int passiveDefault = 1;
    private int hostileDefault = 2;
    private int netherDefault = 3;
    private int endDefault = 4;
    private int miniBossDefault = 10;
    private int chaoscraftEntityDefault = 10;

    // Passive mob types
    private static final Set<EntityType> PASSIVE_MOBS = EnumSet.of(
            EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.CHICKEN,
            EntityType.HORSE, EntityType.DONKEY, EntityType.MULE, EntityType.RABBIT,
            EntityType.MOOSHROOM, EntityType.TURTLE, EntityType.PARROT, EntityType.CAT,
            EntityType.WOLF, EntityType.FOX, EntityType.AXOLOTL, EntityType.FROG,
            EntityType.GOAT, EntityType.SQUID, EntityType.GLOW_SQUID, EntityType.COD,
            EntityType.SALMON, EntityType.PUFFERFISH, EntityType.TROPICAL_FISH,
            EntityType.DOLPHIN, EntityType.BEE, EntityType.STRIDER, EntityType.CAMEL,
            EntityType.SNIFFER, EntityType.ARMADILLO, EntityType.ALLAY
    );

    // Nether mob types
    private static final Set<EntityType> NETHER_MOBS = EnumSet.of(
            EntityType.BLAZE, EntityType.GHAST, EntityType.MAGMA_CUBE,
            EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.HOGLIN,
            EntityType.ZOGLIN, EntityType.WITHER_SKELETON, EntityType.ZOMBIFIED_PIGLIN
    );

    // End mob types
    private static final Set<EntityType> END_MOBS = EnumSet.of(
            EntityType.ENDERMAN, EntityType.ENDERMITE, EntityType.SHULKER
    );

    // Mini-boss types
    private static final Set<EntityType> MINI_BOSS_MOBS = EnumSet.of(
            EntityType.WARDEN, EntityType.ELDER_GUARDIAN
    );

    public StatsConfig(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "stats.yml");
    }

    public void load() {
        if (!configFile.exists()) {
            createDefaults();
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Version check
        int version = config.getInt("config-version", 0);
        if (version < CURRENT_CONFIG_VERSION) {
            config.set("config-version", CURRENT_CONFIG_VERSION);
            try { config.save(configFile); } catch (IOException ignored) {}
        }

        // Load category defaults
        passiveDefault = config.getInt("kill-values.passive", 1);
        hostileDefault = config.getInt("kill-values.hostile", 2);
        netherDefault = config.getInt("kill-values.nether", 3);
        endDefault = config.getInt("kill-values.end", 4);
        miniBossDefault = config.getInt("kill-values.mini-boss", 10);
        chaoscraftEntityDefault = config.getInt("kill-values.chaoscraft-entity", 10);

        // Load specific entity overrides
        entityKillValues.clear();
        var overrides = config.getConfigurationSection("kill-values.entity-overrides");
        if (overrides != null) {
            for (String key : overrides.getKeys(false)) {
                entityKillValues.put(key.toUpperCase(), overrides.getInt(key));
            }
        }

        // Load MythicMobs overrides
        mythicMobKillValues.clear();
        var mythicSection = config.getConfigurationSection("kill-values.mythicmobs");
        if (mythicSection != null) {
            for (String key : mythicSection.getKeys(false)) {
                mythicMobKillValues.put(key, mythicSection.getInt(key));
            }
        }

        plugin.getLogger().info("[Stats] Config loaded. " + entityKillValues.size() + " entity overrides, "
                + mythicMobKillValues.size() + " MythicMobs overrides.");
    }

    private void createDefaults() {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("config-version", CURRENT_CONFIG_VERSION);

        defaults.setComments("config-version", List.of(
                "Internal version number - do NOT edit manually.",
                "The plugin bumps this when new config keys are added."));

        defaults.set("kill-values.passive", 1);
        defaults.setComments("kill-values.passive", List.of(
                "Kills earned for killing passive mobs (cow, pig, sheep, chicken, etc.)"));

        defaults.set("kill-values.hostile", 2);
        defaults.setComments("kill-values.hostile", List.of(
                "Kills earned for hostile overworld mobs (zombie, skeleton, spider, creeper, etc.)"));

        defaults.set("kill-values.nether", 3);
        defaults.setComments("kill-values.nether", List.of(
                "Kills earned for nether mobs (blaze, ghast, piglin, hoglin, wither_skeleton, etc.)"));

        defaults.set("kill-values.end", 4);
        defaults.setComments("kill-values.end", List.of(
                "Kills earned for end mobs (enderman, shulker, endermite)"));

        defaults.set("kill-values.mini-boss", 10);
        defaults.setComments("kill-values.mini-boss", List.of(
                "Kills earned for mini-boss mobs (warden, elder_guardian)"));

        defaults.set("kill-values.wither", 25);
        defaults.setComments("kill-values.wither", List.of(
                "Kills earned for killing the Wither"));

        defaults.set("kill-values.ender-dragon", 50);
        defaults.setComments("kill-values.ender-dragon", List.of(
                "Kills earned for killing the Ender Dragon"));

        defaults.set("kill-values.chaoscraft-entity", 10);
        defaults.setComments("kill-values.chaoscraft-entity", List.of(
                "Default kills earned for killing ChaosCraft mode entities"));

        defaults.set("kill-values.entity-overrides.WARDEN", 10);
        defaults.setComments("kill-values.entity-overrides", List.of(
                "Override kill values for specific entity types (use Bukkit EntityType names).",
                "Example: WARDEN: 10, ENDER_DRAGON: 50"));
        defaults.set("kill-values.entity-overrides.ENDER_DRAGON", 50);
        defaults.set("kill-values.entity-overrides.WITHER", 25);
        defaults.set("kill-values.entity-overrides.ELDER_GUARDIAN", 10);

        defaults.set("kill-values.mythicmobs.example_mob", 5);
        defaults.setComments("kill-values.mythicmobs", List.of(
                "Kill values for MythicMobs custom mobs, keyed by internal mob ID.",
                "Example: my_custom_boss: 50"));

        try {
            defaults.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[Stats] Failed to save default stats.yml", e);
        }
    }

    /**
     * Get the kill value for a Bukkit EntityType.
     */
    public int getKillValue(String entityTypeName) {
        String upper = entityTypeName.toUpperCase();

        // Check specific overrides first
        if (entityKillValues.containsKey(upper)) {
            return entityKillValues.get(upper);
        }

        // Check by category
        try {
            EntityType type = EntityType.valueOf(upper);
            if (type == EntityType.ENDER_DRAGON) return config.getInt("kill-values.ender-dragon", 50);
            if (type == EntityType.WITHER) return config.getInt("kill-values.wither", 25);
            if (MINI_BOSS_MOBS.contains(type)) return miniBossDefault;
            if (NETHER_MOBS.contains(type)) return netherDefault;
            if (END_MOBS.contains(type)) return endDefault;
            if (PASSIVE_MOBS.contains(type)) return passiveDefault;
        } catch (IllegalArgumentException ignored) {}

        // Default to hostile
        return hostileDefault;
    }

    /**
     * Get the kill value for a MythicMobs mob by internal name.
     * Returns -1 if not configured (falls back to entity type logic).
     */
    public int getMythicMobKillValue(String mythicMobId) {
        return mythicMobKillValues.getOrDefault(mythicMobId, -1);
    }
}
