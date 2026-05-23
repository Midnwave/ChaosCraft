package com.blockforge.chaoscraft.services.mobspawn;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads and writes the "mob-spawning" section from any mode's YAML config.
 * This is a reusable helper — each mode creates one instance pointing at its own config.
 *
 * <p>YAML structure:
 * <pre>
 * mob-spawning:
 *   enabled: true
 *   spawn-interval-ticks: 400
 *   max-total-mobs: 15
 *   max-mobs-per-player: 5
 *   spawn-distance: 25.0
 *   spawn-y-mode: "surface"
 *   cleanup-on-end: true
 *   mobs:
 *     - id: "NightmareHound"
 *       type: mythicmobs
 *       weight: 10
 *       min-count: 1
 *       max-count: 3
 *       health-multiplier: 1.0
 *       damage-multiplier: 1.0
 *     - id: "ZOMBIE"
 *       type: vanilla
 *       weight: 8
 *       min-count: 2
 *       max-count: 5
 *       health-multiplier: 1.5
 *       damage-multiplier: 1.0
 * </pre>
 */
public class MobSpawnConfig {

    private static final String ROOT = "mob-spawning";

    private final FileConfiguration config;

    // Cached values
    private boolean enabled;
    private int spawnIntervalTicks;
    private int maxTotalMobs;
    private int maxMobsPerPlayer;
    private double spawnDistance;
    private String spawnYMode;
    private boolean cleanupOnEnd;
    private List<MobSpawnEntry> mobs;

    public MobSpawnConfig(FileConfiguration config) {
        this.config = config;
        reload();
    }

    /**
     * Re-reads all values from the underlying FileConfiguration.
     * Call this after the mode's config is reloaded.
     */
    public void reload() {
        this.enabled = config.getBoolean(ROOT + ".enabled", false);
        this.spawnIntervalTicks = config.getInt(ROOT + ".spawn-interval-ticks", 400);
        this.maxTotalMobs = config.getInt(ROOT + ".max-total-mobs", 15);
        this.maxMobsPerPlayer = config.getInt(ROOT + ".max-mobs-per-player", 5);
        this.spawnDistance = config.getDouble(ROOT + ".spawn-distance", 25.0);
        this.spawnYMode = config.getString(ROOT + ".spawn-y-mode", "surface");
        this.cleanupOnEnd = config.getBoolean(ROOT + ".cleanup-on-end", true);

        this.mobs = new ArrayList<>();
        if (config.isList(ROOT + ".mobs")) {
            var mobList = config.getMapList(ROOT + ".mobs");
            for (var map : mobList) {
                String id = String.valueOf(map.get("id") != null ? map.get("id") : "ZOMBIE");
                String typeStr = String.valueOf(map.get("type") != null ? map.get("type") : "vanilla").toUpperCase();
                MobSpawnEntry.MobType type;
                try {
                    type = MobSpawnEntry.MobType.valueOf(typeStr);
                } catch (IllegalArgumentException e) {
                    type = MobSpawnEntry.MobType.VANILLA;
                }
                int weight = toInt(map.get("weight") != null ? map.get("weight") : 10);
                int minCount = toInt(map.get("min-count") != null ? map.get("min-count") : 1);
                int maxCount = toInt(map.get("max-count") != null ? map.get("max-count") : 1);
                double healthMult = toDouble(map.get("health-multiplier") != null ? map.get("health-multiplier") : 1.0);
                double damageMult = toDouble(map.get("damage-multiplier") != null ? map.get("damage-multiplier") : 1.0);

                // Optional chain-attack per-mob override block (Chain mode gimmick).
                // YAML structure:
                //   chain-attack:
                //     enabled: true
                //     reach-radius: 14.0
                //     effect: SLAM
                //     damage: 180.0
                //     effect-duration-ticks: 25
                // Unknown keys are ignored. Empty/missing -> empty map (use defaults).
                Map<String, Object> chainAttack = parseSubMap(map.get("chain-attack"));

                mobs.add(new MobSpawnEntry(id, type, weight, minCount, maxCount, healthMult, damageMult,
                        chainAttack));
            }
        }
    }

    /**
     * Convert a YAML sub-block (typically returned as a Map) into a Map&lt;String, Object&gt;.
     * Returns an empty map if the input is null or not a Map. SnakeYAML/Bukkit may
     * give us either a LinkedHashMap or a MemorySection depending on how the
     * parent list was constructed — we accept both.
     */
    private static Map<String, Object> parseSubMap(Object raw) {
        if (raw == null) return Collections.emptyMap();
        if (raw instanceof Map<?, ?> rawMap) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : rawMap.entrySet()) {
                if (e.getKey() != null) result.put(String.valueOf(e.getKey()), e.getValue());
            }
            return result;
        }
        if (raw instanceof ConfigurationSection cs) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (String k : cs.getKeys(false)) result.put(k, cs.get(k));
            return result;
        }
        return Collections.emptyMap();
    }

    // ========================
    // Getters
    // ========================

    /**
     * Whether mob spawning is enabled for this mode.
     * If false, no mobs will spawn regardless of other settings.
     */
    public boolean isEnabled() { return enabled; }

    /**
     * Ticks between spawn attempts. 20 ticks = 1 second.
     * Lower values = more frequent spawning = more intense.
     * Default: 400 (every 20 seconds).
     */
    public int getSpawnIntervalTicks() { return spawnIntervalTicks; }

    /**
     * Maximum total mobs alive across the entire server for this mode.
     * Once this cap is reached, no more mobs spawn until some die.
     * Default: 15.
     */
    public int getMaxTotalMobs() { return maxTotalMobs; }

    /**
     * Maximum mobs within 30 blocks of any single player.
     * Prevents one player from being overwhelmed.
     * Default: 5.
     */
    public int getMaxMobsPerPlayer() { return maxMobsPerPlayer; }

    /**
     * How far (blocks) from the target player a new mob spawns.
     * The mob appears at a random angle at this distance from a player.
     * Default: 25.0.
     */
    public double getSpawnDistance() { return spawnDistance; }

    /**
     * How the Y coordinate is determined for spawn location.
     * - "surface": spawn at the highest solid block (default, safest)
     * - "player": spawn at the same Y as the target player
     * - "random": spawn between player Y-5 and Y+10 (for flying mobs)
     */
    public String getSpawnYMode() { return spawnYMode; }

    /**
     * Whether to remove all spawned mobs when the mode ends.
     * Default: true. Set to false if you want mobs to persist after the mode.
     */
    public boolean isCleanupOnEnd() { return cleanupOnEnd; }

    /**
     * The list of mob entries that can be spawned.
     * Each entry has a weight, count range, and multipliers.
     * Returns an unmodifiable list.
     */
    public List<MobSpawnEntry> getMobs() { return Collections.unmodifiableList(mobs); }

    /**
     * Total combined weight of all mob entries.
     * Used for weighted random selection.
     */
    public int getTotalWeight() {
        int total = 0;
        for (MobSpawnEntry entry : mobs) total += entry.getWeight();
        return total;
    }

    // ========================
    // Static: write defaults into a FileConfiguration
    // ========================

    /**
     * Writes the default mob-spawning section into the given config.
     * Call this from your mode's createDefaults() method.
     *
     * @param defaults      the FileConfiguration to write into
     * @param defaultMobs   the default mob list (can be empty for modes with no default mobs)
     */
    public static void writeDefaults(FileConfiguration defaults, List<MobSpawnDefaultEntry> defaultMobs) {
        defaults.set(ROOT + ".enabled", false);
        defaults.setComments(ROOT + ".enabled", List.of(
                "",
                "=== MOB SPAWNING ===",
                "Universal mob spawning system. Spawns MythicMobs or vanilla mobs during this mode.",
                "Set to true to enable mob spawning. Requires MythicMobs for mythicmobs-type entries.",
                "Vanilla mobs work without any extra plugins."));

        defaults.set(ROOT + ".spawn-interval-ticks", 400);
        defaults.setComments(ROOT + ".spawn-interval-ticks", List.of(
                "Ticks between mob spawn attempts. 20 ticks = 1 second.",
                "400 = every 20 seconds. Lower = more frequent = more intense.",
                "Recommended: 200 (aggressive) to 600 (relaxed)."));

        defaults.set(ROOT + ".max-total-mobs", 15);
        defaults.setComments(ROOT + ".max-total-mobs", List.of(
                "Maximum total mobs alive at once for this mode across the entire server.",
                "Once this cap is hit, no more mobs spawn until some die.",
                "Recommended: 10-25 depending on player count."));

        defaults.set(ROOT + ".max-mobs-per-player", 5);
        defaults.setComments(ROOT + ".max-mobs-per-player", List.of(
                "Maximum mobs within 30 blocks of any single player.",
                "Prevents one player from being overwhelmed while others are safe.",
                "Recommended: 3-8."));

        defaults.set(ROOT + ".spawn-distance", 25.0);
        defaults.setComments(ROOT + ".spawn-distance", List.of(
                "How far (blocks) from the target player a new mob spawns.",
                "The mob appears at a random angle at this distance.",
                "Too close = unfair surprise, too far = mob may not find the player."));

        defaults.set(ROOT + ".spawn-y-mode", "surface");
        defaults.setComments(ROOT + ".spawn-y-mode", List.of(
                "How the Y coordinate is determined for the spawn location.",
                "Options:",
                "  surface — spawn at the highest solid block (default, safest for ground mobs)",
                "  player  — spawn at the same Y level as the target player",
                "  random  — spawn between player Y-5 and Y+10 (good for flying/floating mobs)"));

        defaults.set(ROOT + ".cleanup-on-end", true);
        defaults.setComments(ROOT + ".cleanup-on-end", List.of(
                "Whether to remove ALL spawned mobs when the mode ends.",
                "true = clean slate after mode (recommended).",
                "false = mobs persist in the world after the mode stops."));

        // Write mob entries
        List<java.util.Map<String, Object>> mobListMaps = new ArrayList<>();
        for (MobSpawnDefaultEntry entry : defaultMobs) {
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", entry.id);
            map.put("type", entry.type);
            map.put("weight", entry.weight);
            map.put("min-count", entry.minCount);
            map.put("max-count", entry.maxCount);
            map.put("health-multiplier", entry.healthMultiplier);
            map.put("damage-multiplier", entry.damageMultiplier);
            mobListMaps.add(map);
        }
        defaults.set(ROOT + ".mobs", mobListMaps);
        defaults.setComments(ROOT + ".mobs", List.of(
                "List of mobs that can spawn during this mode.",
                "Each entry defines:",
                "  id              — MythicMobs internal mob ID (e.g. \"NightmareHound\") or vanilla EntityType (e.g. \"ZOMBIE\")",
                "  type            — \"mythicmobs\" or \"vanilla\"",
                "  weight          — spawn weight (higher = more likely to be chosen). Probability = weight / totalWeight",
                "  min-count       — minimum mobs spawned per wave (always >= 1)",
                "  max-count       — maximum mobs spawned per wave (always >= min-count)",
                "  health-multiplier — multiplied against the mob's base max health (1.0 = normal, 2.0 = double)",
                "  damage-multiplier — multiplied against the mob's base attack damage (1.0 = normal)",
                "",
                "Example: a ZOMBIE with weight 10 and a SKELETON with weight 5 means zombies are 2x more likely.",
                "You can mix MythicMobs and vanilla mobs freely in the same list."));
    }

    /**
     * Ensures all mob-spawning keys exist in the given config. Call from your load() method.
     * Returns true if any keys were added (caller should save).
     */
    public static boolean ensureKeys(FileConfiguration config) {
        boolean changed = false;
        if (!config.contains(ROOT + ".enabled")) { config.set(ROOT + ".enabled", false); changed = true; }
        if (!config.contains(ROOT + ".spawn-interval-ticks")) { config.set(ROOT + ".spawn-interval-ticks", 400); changed = true; }
        if (!config.contains(ROOT + ".max-total-mobs")) { config.set(ROOT + ".max-total-mobs", 15); changed = true; }
        if (!config.contains(ROOT + ".max-mobs-per-player")) { config.set(ROOT + ".max-mobs-per-player", 5); changed = true; }
        if (!config.contains(ROOT + ".spawn-distance")) { config.set(ROOT + ".spawn-distance", 25.0); changed = true; }
        if (!config.contains(ROOT + ".spawn-y-mode")) { config.set(ROOT + ".spawn-y-mode", "surface"); changed = true; }
        if (!config.contains(ROOT + ".cleanup-on-end")) { config.set(ROOT + ".cleanup-on-end", true); changed = true; }
        if (!config.contains(ROOT + ".mobs")) { config.set(ROOT + ".mobs", new ArrayList<>()); changed = true; }
        return changed;
    }

    // ========================
    // Helpers
    // ========================

    private static int toInt(Object obj) {
        if (obj instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(obj)); } catch (NumberFormatException e) { return 1; }
    }

    private static double toDouble(Object obj) {
        if (obj instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(obj)); } catch (NumberFormatException e) { return 1.0; }
    }

    /**
     * Simple data holder for writing default mob entries.
     */
    public static class MobSpawnDefaultEntry {
        public final String id;
        public final String type;
        public final int weight;
        public final int minCount;
        public final int maxCount;
        public final double healthMultiplier;
        public final double damageMultiplier;

        public MobSpawnDefaultEntry(String id, String type, int weight, int minCount, int maxCount,
                                     double healthMultiplier, double damageMultiplier) {
            this.id = id;
            this.type = type;
            this.weight = weight;
            this.minCount = minCount;
            this.maxCount = maxCount;
            this.healthMultiplier = healthMultiplier;
            this.damageMultiplier = damageMultiplier;
        }
    }
}
