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

    public static final int CURRENT_CONFIG_VERSION = 1;

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

        // Config version check
        int fileVersion = yaml.getInt("config-version", 0);
        if (fileVersion < CURRENT_CONFIG_VERSION) {
            plugin.getLogger().info("[ModePoints] Upgrading config from v" + fileVersion + " to v" + CURRENT_CONFIG_VERSION);
            yaml.set("config-version", CURRENT_CONFIG_VERSION);
            try { yaml.save(configFile); } catch (IOException ignored) {}
        }

        plugin.getLogger().info("[ModePoints] Config loaded. " + rewardThresholds.size() + " reward thresholds configured.");
    }

    private void createDefaults() {
        YamlConfiguration defaults = new YamlConfiguration();

        defaults.set("config-version", CURRENT_CONFIG_VERSION);
        defaults.setComments("config-version", List.of(
                "Internal version number — do NOT edit manually.",
                "The plugin bumps this when new keys are added and will auto-upgrade your file."));

        // ── Actions ────────────────────────────────────────────────────────────
        // Each action has: points (how many to award), enabled (active?), description (read-only label)
        defaults.setComments("actions", List.of(
                "─────────────────────────────────────────────────────────────",
                "ACTIONS — Points awarded for each player action during a mode.",
                "Each entry has:",
                "  points      — How many Mode Points this action awards when triggered.",
                "  enabled     — Set to false to disable this action from awarding points.",
                "  description — Read-only label explaining what triggers this action.",
                "─────────────────────────────────────────────────────────────"));
        for (PointAction action : PointAction.values()) {
            String path = "actions." + action.getConfigKey();
            defaults.set(path + ".points", action.getDefaultPoints());
            defaults.set(path + ".enabled", true);
            defaults.set(path + ".description", action.getDescription());
            defaults.setComments(path + ".points", List.of(
                    "Points awarded when this action is triggered. Negative values subtract points."));
            defaults.setComments(path + ".enabled", List.of(
                    "Set to false to stop this action from awarding/subtracting points entirely."));
            defaults.setComments(path + ".description", List.of(
                    "What player action triggers these points. (Read-only — for reference only.)"));
        }

        // ── Reward Thresholds ──────────────────────────────────────────────────
        defaults.setComments("rewards", List.of(
                "─────────────────────────────────────────────────────────────",
                "REWARD THRESHOLDS — Commands run when a player crosses a point milestone.",
                "Key = the point total threshold (integer).",
                "Value = list of console commands to run. Use %player% for the player's name.",
                "Thresholds only trigger ONCE per session per player (not repeatedly).",
                "Example:",
                "  100:",
                "    - \"give %player% golden_apple 2\"",
                "  500:",
                "    - \"eco give %player% 1000\"",
                "    - \"broadcast %player% reached 500 Mode Points!\"",
                "─────────────────────────────────────────────────────────────"));
        defaults.set("rewards.50", List.of("give %player% diamond 1"));
        defaults.setComments("rewards.50", List.of(
                "Reward triggered when a player reaches 50 Mode Points in a session."));
        defaults.set("rewards.150", List.of("give %player% diamond 3"));
        defaults.setComments("rewards.150", List.of(
                "Reward triggered when a player reaches 150 Mode Points."));
        defaults.set("rewards.300", List.of("give %player% netherite_ingot 1"));
        defaults.setComments("rewards.300", List.of(
                "Reward triggered when a player reaches 300 Mode Points."));
        defaults.set("rewards.500", List.of("give %player% diamond_block 3", "say %player% earned 500 Mode Points!"));
        defaults.setComments("rewards.500", List.of(
                "Reward triggered when a player reaches 500 Mode Points (high-end milestone)."));

        // ── Mode-Specific Settings ─────────────────────────────────────────────
        defaults.setComments("settings", List.of(
                "─────────────────────────────────────────────────────────────",
                "SETTINGS — Thresholds and tuning values for mode-specific point triggers.",
                "─────────────────────────────────────────────────────────────"));
        defaults.set("settings.chain-wave-size", 10);
        defaults.setComments("settings.chain-wave-size", List.of(
                "Chain Mode: How many chain attacks a player must survive to count as completing a 'wave'.",
                "Points are awarded per completed wave. 10 = every 10 attacks survived = 1 wave bonus."));
        defaults.set("settings.corruption-purify-threshold", 25);
        defaults.setComments("settings.corruption-purify-threshold", List.of(
                "Corruption Mode: Number of corrupted blocks a player must break to earn a purify bonus.",
                "Breaking this many corruption blocks triggers a point award for active cleanup."));
        defaults.set("settings.corruption-break-milestone", 25);
        defaults.setComments("settings.corruption-break-milestone", List.of(
                "Corruption Mode: Block break count milestone for bonus point rewards.",
                "Every time a player breaks this many blocks total, a milestone bonus is awarded."));
        defaults.set("settings.damage-zone-survive-ticks", 60);
        defaults.setComments("settings.damage-zone-survive-ticks", List.of(
                "All modes: Ticks a player must remain inside an active damage zone to earn survival points.",
                "60 = survive inside a damage zone for 3 seconds to earn the bonus. 20 ticks = 1 second."));
        defaults.set("settings.overwhelm-threshold", 3);
        defaults.setComments("settings.overwhelm-threshold", List.of(
                "All modes: Number of simultaneous active attacks targeting a player to count as 'overwhelmed'.",
                "If a player survives with this many attacks at once, they earn overwhelm survival bonus points."));
        defaults.set("settings.fragment-drop-chance", 0.3);
        defaults.setComments("settings.fragment-drop-chance", List.of(
                "Calamity Mode: Probability (0.0–1.0) that a shard/fragment item drops when a gem is collected.",
                "0.3 = 30% chance per gem pickup. 1.0 = guaranteed. 0.0 = disabled."));

        // ── Debug ──────────────────────────────────────────────────────────────
        defaults.set("debug", false);
        defaults.setComments("debug", List.of(
                "Enable debug logging for the Mode Points system.",
                "When true, point awards/deductions are printed to the server console for testing.",
                "Set to false in production — can be toggled live with /cc points debug."));

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
