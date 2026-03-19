package com.blockforge.chaoscraft.api.points;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Configuration for the Mode Points system.
 * Stores point values per action, enabled/disabled state, reward thresholds.
 */
public class ModePointsConfig {

    private final ChaosCraftPlugin plugin;
    private File configFile;
    private YamlConfiguration yaml;

    // Point values per action (configurable)
    private final Map<PointAction, Integer> pointValues = new EnumMap<>(PointAction.class);
    // Enabled/disabled per action
    private final Map<PointAction, Boolean> actionEnabled = new EnumMap<>(PointAction.class);
    // Reward thresholds: points -> list of commands
    private final TreeMap<Integer, List<String>> rewardThresholds = new TreeMap<>();

    // Wave size for chain mode (how many attacks = 1 wave)
    private int chainWaveSize = 10;
    // Corruption blocks to purify a chunk
    private int corruptionPurifyThreshold = 25;
    // Corruption break milestone count
    private int corruptionBreakMilestone = 25;
    // Damage zone survive time (ticks)
    private int damageZoneSurviveTicks = 60; // 3 seconds
    // Overwhelm threshold (simultaneous attacks)
    private int overwhelmThreshold = 3;
    // Fragment/shard drop chance (0.0 - 1.0)
    private double fragmentDropChance = 0.3;
    // Debug mode
    private boolean debug = false;

    public ModePointsConfig(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        configFile = new File(plugin.getDataFolder(), "mode-points.yml");
        if (!configFile.exists()) {
            createDefaults();
        }
        yaml = YamlConfiguration.loadConfiguration(configFile);

        // Load point values
        pointValues.clear();
        actionEnabled.clear();
        ConfigurationSection actionsSection = yaml.getConfigurationSection("actions");
        for (PointAction action : PointAction.values()) {
            String key = action.getConfigKey();
            if (actionsSection != null && actionsSection.contains(key)) {
                pointValues.put(action, actionsSection.getInt(key + ".points", action.getDefaultPoints()));
                actionEnabled.put(action, actionsSection.getBoolean(key + ".enabled", true));
            } else {
                pointValues.put(action, action.getDefaultPoints());
                actionEnabled.put(action, true);
            }
        }

        // Load reward thresholds
        rewardThresholds.clear();
        ConfigurationSection rewardsSection = yaml.getConfigurationSection("rewards");
        if (rewardsSection != null) {
            for (String key : rewardsSection.getKeys(false)) {
                try {
                    int threshold = Integer.parseInt(key);
                    List<String> commands = rewardsSection.getStringList(key);
                    if (!commands.isEmpty()) {
                        rewardThresholds.put(threshold, commands);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        // Load mode-specific settings
        chainWaveSize = yaml.getInt("settings.chain-wave-size", 10);
        corruptionPurifyThreshold = yaml.getInt("settings.corruption-purify-threshold", 25);
        corruptionBreakMilestone = yaml.getInt("settings.corruption-break-milestone", 25);
        damageZoneSurviveTicks = yaml.getInt("settings.damage-zone-survive-ticks", 60);
        overwhelmThreshold = yaml.getInt("settings.overwhelm-threshold", 3);
        fragmentDropChance = yaml.getDouble("settings.fragment-drop-chance", 0.3);
        debug = yaml.getBoolean("debug", false);

        plugin.getLogger().info("[ModePoints] Config loaded. " + rewardThresholds.size() + " reward thresholds configured.");
    }

    private void createDefaults() {
        YamlConfiguration defaults = new YamlConfiguration();

        // Actions section with all defaults
        for (PointAction action : PointAction.values()) {
            String path = "actions." + action.getConfigKey();
            defaults.set(path + ".points", action.getDefaultPoints());
            defaults.set(path + ".enabled", true);
            defaults.set(path + ".description", action.getDescription());
        }

        // Default reward thresholds
        defaults.set("rewards.50", List.of("give %player% diamond 1"));
        defaults.set("rewards.150", List.of("give %player% diamond 3"));
        defaults.set("rewards.300", List.of("give %player% netherite_ingot 1"));
        defaults.set("rewards.500", List.of("give %player% diamond_block 3", "say %player% earned 500 Mode Points!"));

        // Settings
        defaults.set("settings.chain-wave-size", 10);
        defaults.set("settings.corruption-purify-threshold", 25);
        defaults.set("settings.corruption-break-milestone", 25);
        defaults.set("settings.damage-zone-survive-ticks", 60);
        defaults.set("settings.overwhelm-threshold", 3);
        defaults.set("settings.fragment-drop-chance", 0.3);
        defaults.set("debug", false);

        try {
            defaults.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[ModePoints] Failed to save default config", e);
        }
    }

    // ---- Accessors ----

    public int getPoints(PointAction action) {
        return pointValues.getOrDefault(action, action.getDefaultPoints());
    }

    public boolean isEnabled(PointAction action) {
        return actionEnabled.getOrDefault(action, true);
    }

    public TreeMap<Integer, List<String>> getRewardThresholds() {
        return rewardThresholds;
    }

    public int getChainWaveSize() { return chainWaveSize; }
    public int getCorruptionPurifyThreshold() { return corruptionPurifyThreshold; }
    public int getCorruptionBreakMilestone() { return corruptionBreakMilestone; }
    public int getDamageZoneSurviveTicks() { return damageZoneSurviveTicks; }
    public int getOverwhelmThreshold() { return overwhelmThreshold; }
    public double getFragmentDropChance() { return fragmentDropChance; }
    public boolean isDebug() { return debug; }

    public void setDebug(boolean debug) {
        this.debug = debug;
        if (yaml != null) {
            yaml.set("debug", debug);
            try { yaml.save(configFile); } catch (IOException ignored) {}
        }
    }
}
