package com.blockforge.chaoscraft.modes.freezingice;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.blockforge.chaoscraft.services.mobspawn.MobSpawnConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Freezing Ice Mode configuration loader.
 * Reads from plugins/ChaosCraft/modes/freezingice/freezingice.yml
 *
 * Freezing Ice is a survival timer mode with aggressive living-ice attacks,
 * a temperature tracker, and powder snow freeze mechanics.
 */
public class FreezingIceConfig {

    private final ChaosCraftPlugin plugin;
    private final File configFile;
    private FileConfiguration config;

    public FreezingIceConfig(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        File modeDir = new File(plugin.getDataFolder(), "modes/freezingice");
        if (!modeDir.exists()) modeDir.mkdirs();
        this.configFile = new File(modeDir, "freezingice.yml");
        load();
    }

    public void load() {
        if (!configFile.exists()) {
            createDefaults();
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        boolean needsSave = false;

        // ── Base mode keys ──────────────────────────────────────────────
        if (!config.contains("config-version")) { config.set("config-version", 2); needsSave = true; }
        // Auto-upgrade from version 1 → 2 (adds mob-spawning section)
        if (config.getInt("config-version") < 2) { config.set("config-version", 2); needsSave = true; }
        if (!config.contains("timer.default-seconds")) { config.set("timer.default-seconds", 600); needsSave = true; }
        if (!config.contains("timer.max-seconds")) { config.set("timer.max-seconds", 1800); needsSave = true; }
        if (!config.contains("music.sound-id")) { config.set("music.sound-id", ""); needsSave = true; }
        if (!config.contains("music.loop")) { config.set("music.loop", true); needsSave = true; }
        if (!config.contains("music.duration-ticks")) { config.set("music.duration-ticks", 6000); needsSave = true; }
        if (!config.contains("on-start-commands")) { config.set("on-start-commands", new ArrayList<>()); needsSave = true; }
        if (!config.contains("on-end-commands")) { config.set("on-end-commands", new ArrayList<>()); needsSave = true; }
        if (!config.contains("exempt-players")) { config.set("exempt-players", new ArrayList<>()); needsSave = true; }
        if (!config.contains("max-events-per-player")) { config.set("max-events-per-player", 5); needsSave = true; }
        if (!config.contains("rewards.commands")) { config.set("rewards.commands", new ArrayList<>()); needsSave = true; }

        // ── Freezing Ice specific keys ──────────────────────────────────
        if (!config.contains("world")) { config.set("world", ""); needsSave = true; }
        if (!config.contains("spawn.base-interval-ticks")) { config.set("spawn.base-interval-ticks", 50); needsSave = true; }
        if (!config.contains("spawn.max-events-per-player")) { config.set("spawn.max-events-per-player", 5); needsSave = true; }
        if (!config.contains("spawn.offset-radius")) { config.set("spawn.offset-radius", 10.0); needsSave = true; }

        // ── Freeze system ──────────────────────────────────────────────
        if (!config.contains("freeze.enabled")) { config.set("freeze.enabled", true); needsSave = true; }
        if (!config.contains("freeze.base-freeze-rate")) { config.set("freeze.base-freeze-rate", 1); needsSave = true; }
        if (!config.contains("freeze.attack-freeze-bonus")) { config.set("freeze.attack-freeze-bonus", 20); needsSave = true; }
        if (!config.contains("freeze.max-freeze-ticks")) { config.set("freeze.max-freeze-ticks", 300); needsSave = true; }
        if (!config.contains("freeze.thaw-near-heat")) { config.set("freeze.thaw-near-heat", true); needsSave = true; }
        if (!config.contains("freeze.thaw-rate")) { config.set("freeze.thaw-rate", 5); needsSave = true; }
        if (!config.contains("freeze.leather-boots-protect")) { config.set("freeze.leather-boots-protect", false); needsSave = true; }

        // ── Temperature system ─────────────────────────────────────────
        if (!config.contains("temperature.enabled")) { config.set("temperature.enabled", true); needsSave = true; }
        if (!config.contains("temperature.start-value")) { config.set("temperature.start-value", 100); needsSave = true; }
        if (!config.contains("temperature.decay-rate")) { config.set("temperature.decay-rate", 1); needsSave = true; }
        if (!config.contains("temperature.decay-interval-ticks")) { config.set("temperature.decay-interval-ticks", 100); needsSave = true; }
        if (!config.contains("temperature.speed-threshold-75")) { config.set("temperature.speed-threshold-75", -0.03); needsSave = true; }
        if (!config.contains("temperature.speed-threshold-50")) { config.set("temperature.speed-threshold-50", -0.06); needsSave = true; }
        if (!config.contains("temperature.speed-threshold-25")) { config.set("temperature.speed-threshold-25", -0.10); needsSave = true; }
        if (!config.contains("temperature.freeze-overlay-threshold")) { config.set("temperature.freeze-overlay-threshold", 50); needsSave = true; }
        if (!config.contains("temperature.heat-source-restore")) { config.set("temperature.heat-source-restore", 5); needsSave = true; }
        if (!config.contains("temperature.heat-source-radius")) { config.set("temperature.heat-source-radius", 3.0); needsSave = true; }

        // ── Timer HUD ──────────────────────────────────────────────────
        if (!config.contains("timer-hud.display-name")) { config.set("timer-hud.display-name", "FREEZING ICE"); needsSave = true; }
        if (!config.contains("timer-hud.color")) { config.set("timer-hud.color", "aqua"); needsSave = true; }
        if (!config.contains("timer-hud.flash-color")) { config.set("timer-hud.flash-color", "blue"); needsSave = true; }
        if (!config.contains("timer-hud.flash-threshold-seconds")) { config.set("timer-hud.flash-threshold-seconds", 60); needsSave = true; }

        // ── Universal mob spawning ───────────────────────────────────
        if (MobSpawnConfig.ensureKeys(config)) needsSave = true;

        if (needsSave) {
            save();
            config = YamlConfiguration.loadConfiguration(configFile);
        }
    }

    /** Returns a MobSpawnConfig backed by this mode's YAML. */
    public MobSpawnConfig getMobSpawnConfig() {
        return new MobSpawnConfig(config);
    }

    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save freezingice config: " + e.getMessage());
        }
    }

    public FileConfiguration get() { return config; }

    // ========================
    // World
    // ========================

    public String getWorldName() { return config.getString("world", ""); }

    // ========================
    // Timer
    // ========================

    public long getDefaultTimerSeconds() { return config.getLong("timer.default-seconds", 600); }
    public long getMaxTimerSeconds() { return config.getLong("timer.max-seconds", 1800); }

    // ========================
    // Spawning
    // ========================

    public int getBaseSpawnInterval() { return config.getInt("spawn.base-interval-ticks", 50); }
    public int getMaxEventsPerPlayer() { return config.getInt("spawn.max-events-per-player", 5); }
    public double getSpawnOffsetRadius() { return config.getDouble("spawn.offset-radius", 10.0); }

    // ========================
    // Music
    // ========================

    public String getMusicSoundId() { return config.getString("music.sound-id", ""); }
    public boolean isMusicLooped() { return config.getBoolean("music.loop", true); }
    public long getMusicDurationTicks() { return config.getLong("music.duration-ticks", 6000); }

    // ========================
    // Lifecycle
    // ========================

    public List<String> getOnStartCommands() { return config.getStringList("on-start-commands"); }
    public List<String> getOnEndCommands() { return config.getStringList("on-end-commands"); }
    public List<String> getExemptPlayers() { return config.getStringList("exempt-players"); }
    public List<String> getRewardCommands() { return config.getStringList("rewards.commands"); }

    // ========================
    // Freeze System
    // ========================

    public boolean isFreezeEnabled() { return config.getBoolean("freeze.enabled", true); }
    public int getBaseFreezeRate() { return config.getInt("freeze.base-freeze-rate", 1); }
    public int getAttackFreezeBonus() { return config.getInt("freeze.attack-freeze-bonus", 20); }
    public int getMaxFreezeTicks() { return config.getInt("freeze.max-freeze-ticks", 300); }
    public boolean isThawNearHeat() { return config.getBoolean("freeze.thaw-near-heat", true); }
    public int getThawRate() { return config.getInt("freeze.thaw-rate", 5); }
    public boolean isLeatherBootsProtect() { return config.getBoolean("freeze.leather-boots-protect", false); }

    // ========================
    // Temperature System
    // ========================

    public boolean isTemperatureEnabled() { return config.getBoolean("temperature.enabled", true); }
    public int getTemperatureStartValue() { return config.getInt("temperature.start-value", 100); }
    public int getTemperatureDecayRate() { return config.getInt("temperature.decay-rate", 1); }
    public int getTemperatureDecayInterval() { return config.getInt("temperature.decay-interval-ticks", 100); }
    public double getSpeedThreshold75() { return config.getDouble("temperature.speed-threshold-75", -0.03); }
    public double getSpeedThreshold50() { return config.getDouble("temperature.speed-threshold-50", -0.06); }
    public double getSpeedThreshold25() { return config.getDouble("temperature.speed-threshold-25", -0.10); }
    public int getFreezeOverlayThreshold() { return config.getInt("temperature.freeze-overlay-threshold", 50); }
    public int getHeatSourceRestore() { return config.getInt("temperature.heat-source-restore", 5); }
    public double getHeatSourceRadius() { return config.getDouble("temperature.heat-source-radius", 3.0); }

    // ========================
    // Default config creation
    // ========================

    private void createDefaults() {
        FileConfiguration defaults = new YamlConfiguration();

        // ── World ────────────────────────────────────────────────────────
        defaults.set("world", "");
        defaults.setComments("world", List.of(
                "World name where Freezing Ice Mode runs.",
                "Leave empty (\"\") to use the first loaded world (Overworld)."));

        // ── Timer ────────────────────────────────────────────────────────
        defaults.set("timer.default-seconds", 600);
        defaults.setComments("timer.default-seconds", List.of(
                "Default duration in seconds when started without a time argument.",
                "600 = 10 min | 900 = 15 min | 1200 = 20 min."));
        defaults.set("timer.max-seconds", 1800);
        defaults.setComments("timer.max-seconds", List.of(
                "Maximum timer value (seconds) allowed via command."));

        // ── Spawning ─────────────────────────────────────────────────────
        defaults.set("spawn.base-interval-ticks", 50);
        defaults.setComments("spawn.base-interval-ticks", List.of(
                "Ticks between attack spawn attempts. 20 ticks = 1 second.",
                "50 = attempt every 2.5 seconds."));
        defaults.set("spawn.max-events-per-player", 5);
        defaults.setComments("spawn.max-events-per-player", List.of(
                "Maximum simultaneous active attacks per player. Recommended: 3–8."));
        defaults.set("spawn.offset-radius", 10.0);
        defaults.setComments("spawn.offset-radius", List.of(
                "Max distance from player that attacks can spawn."));

        // ── Timer HUD ────────────────────────────────────────────────────
        defaults.set("timer-hud.display-name", "FREEZING ICE");
        defaults.setComments("timer-hud.display-name", List.of(
                "Mode name displayed on the BetterHud timer bar."));
        defaults.set("timer-hud.color", "aqua");
        defaults.setComments("timer-hud.color", List.of(
                "Color of the mode name text (normal state)."));
        defaults.set("timer-hud.flash-color", "blue");
        defaults.setComments("timer-hud.flash-color", List.of(
                "Color of the timer text when flashing (low time warning)."));
        defaults.set("timer-hud.flash-threshold-seconds", 60);
        defaults.setComments("timer-hud.flash-threshold-seconds", List.of(
                "Seconds remaining at which the timer starts flashing."));

        // ── Music ────────────────────────────────────────────────────────
        defaults.set("music.sound-id", "");
        defaults.setComments("music.sound-id", List.of(
                "Namespaced sound ID for background music. Leave empty to disable."));
        defaults.set("music.loop", true);
        defaults.setComments("music.loop", List.of("Whether the music loops continuously."));
        defaults.set("music.duration-ticks", 6000);
        defaults.setComments("music.duration-ticks", List.of(
                "Duration of one music loop in ticks. 6000 = 5 minutes."));

        // ── Lifecycle Commands ───────────────────────────────────────────
        defaults.set("on-start-commands", new ArrayList<>());
        defaults.setComments("on-start-commands", List.of(
                "Console commands run when Freezing Ice starts. Use %player% for the starting player."));
        defaults.set("on-end-commands", new ArrayList<>());
        defaults.setComments("on-end-commands", List.of("Console commands run when the mode ends."));
        defaults.set("exempt-players", new ArrayList<>());
        defaults.setComments("exempt-players", List.of(
                "Player names that receive ZERO damage from all ice attacks."));
        defaults.set("rewards.commands", new ArrayList<>());
        defaults.setComments("rewards.commands", List.of(
                "Commands run for each surviving player. Use %player%."));

        // ── Freeze System ────────────────────────────────────────────────
        defaults.set("freeze.enabled", true);
        defaults.setComments("freeze.enabled", List.of(
                "Enable the powder snow freeze overlay system.",
                "Uses Player.setFreezeTicks() to simulate walking in powder snow."));
        defaults.set("freeze.base-freeze-rate", 1);
        defaults.setComments("freeze.base-freeze-rate", List.of(
                "Freeze ticks added per second passively during the mode.",
                "Higher = players freeze faster just by existing in the mode."));
        defaults.set("freeze.attack-freeze-bonus", 20);
        defaults.setComments("freeze.attack-freeze-bonus", List.of(
                "Extra freeze ticks applied when hit by an ice attack.",
                "Stacks with base rate. 20 = noticeable frost burst on each hit."));
        defaults.set("freeze.max-freeze-ticks", 300);
        defaults.setComments("freeze.max-freeze-ticks", List.of(
                "Maximum freeze ticks cap. 300 = full powder snow freezing effect.",
                "At 140+ ticks: vanilla freeze damage starts (1 heart per 40 ticks)."));
        defaults.set("freeze.thaw-near-heat", true);
        defaults.setComments("freeze.thaw-near-heat", List.of(
                "Whether standing near heat sources (torches, campfires, lava) reduces freeze ticks."));
        defaults.set("freeze.thaw-rate", 5);
        defaults.setComments("freeze.thaw-rate", List.of(
                "Freeze ticks removed per second when near a heat source."));
        defaults.set("freeze.leather-boots-protect", false);
        defaults.setComments("freeze.leather-boots-protect", List.of(
                "Whether leather boots reduce the freeze rate (vanilla powder snow mechanic).",
                "Set to false to prevent players from trivially countering the mode."));

        // ── Temperature System ───────────────────────────────────────────
        defaults.set("temperature.enabled", true);
        defaults.setComments("temperature.enabled", List.of(
                "Enable the temperature tracker. Players start warm and get colder over time.",
                "Temperature affects movement speed, freeze overlay, and damage taken."));
        defaults.set("temperature.start-value", 100);
        defaults.setComments("temperature.start-value", List.of(
                "Starting temperature for each player. 100 = fully warm."));
        defaults.set("temperature.decay-rate", 1);
        defaults.setComments("temperature.decay-rate", List.of(
                "Temperature points lost per decay tick. Higher = faster cooling."));
        defaults.set("temperature.decay-interval-ticks", 100);
        defaults.setComments("temperature.decay-interval-ticks", List.of(
                "Ticks between temperature decay ticks. 100 = every 5 seconds."));
        defaults.set("temperature.speed-threshold-75", -0.03);
        defaults.setComments("temperature.speed-threshold-75", List.of(
                "Movement speed modifier (attribute) when temperature drops below 75.",
                "Negative values slow the player. -0.03 = slight slowdown."));
        defaults.set("temperature.speed-threshold-50", -0.06);
        defaults.setComments("temperature.speed-threshold-50", List.of(
                "Movement speed modifier when temperature drops below 50."));
        defaults.set("temperature.speed-threshold-25", -0.10);
        defaults.setComments("temperature.speed-threshold-25", List.of(
                "Movement speed modifier when temperature drops below 25. -0.10 = very slow."));
        defaults.set("temperature.freeze-overlay-threshold", 50);
        defaults.setComments("temperature.freeze-overlay-threshold", List.of(
                "Temperature at which the freeze overlay (powder snow visual) begins.",
                "Below this, setFreezeTicks() is called periodically to maintain the frost screen effect."));
        defaults.set("temperature.heat-source-restore", 5);
        defaults.setComments("temperature.heat-source-restore", List.of(
                "Temperature points restored per second when near a heat source."));
        defaults.set("temperature.heat-source-radius", 3.0);
        defaults.setComments("temperature.heat-source-radius", List.of(
                "Block radius to search for heat sources (torches, campfires, lava, fire)."));

        // ── Universal Mob Spawning ──────────────────────────────────────
        MobSpawnConfig.writeDefaults(defaults, List.of(
                new MobSpawnConfig.MobSpawnDefaultEntry("STRAY", "vanilla", 10, 1, 3, 1.5, 1.0),
                new MobSpawnConfig.MobSpawnDefaultEntry("SKELETON", "vanilla", 6, 1, 2, 1.2, 1.0),
                new MobSpawnConfig.MobSpawnDefaultEntry("POLAR_BEAR", "vanilla", 4, 1, 1, 2.0, 1.5)
        ));

        try {
            defaults.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create default freezingice config: " + e.getMessage());
        }
    }
}
