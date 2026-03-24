package com.blockforge.chaoscraft.modes.devilsdream;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.blockforge.chaoscraft.services.mobspawn.MobSpawnConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Devil's Dream Mode configuration loader.
 * Reads from plugins/ChaosCraft/modes/devilsdream/devilsdream.yml
 *
 * Includes standard mode config (timer, music, commands) plus
 * Dream Adaptation system config and MythicMobs integration config.
 */
public class DevilsDreamConfig {

    private final ChaosCraftPlugin plugin;
    private final File configFile;
    private FileConfiguration config;

    public DevilsDreamConfig(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        File modeDir = new File(plugin.getDataFolder(), "modes/devilsdream");
        if (!modeDir.exists()) modeDir.mkdirs();
        this.configFile = new File(modeDir, "devilsdream.yml");
        load();
    }

    public void load() {
        if (!configFile.exists()) {
            createDefaults();
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // Ensure ALL keys exist — both base mode keys and devilsdream-specific keys.
        boolean needsSave = false;

        // ── Base mode keys ───────────────────────────────────────────────────
        if (!config.contains("config-version")) { config.set("config-version", 2); needsSave = true; }
        if (config.getInt("config-version") < 2) { config.set("config-version", 2); needsSave = true; }
        if (!config.contains("timer.default-seconds")) { config.set("timer.default-seconds", 900); needsSave = true; }
        if (!config.contains("timer.max-seconds")) { config.set("timer.max-seconds", 1800); needsSave = true; }
        if (!config.contains("music.sound-id")) { config.set("music.sound-id", ""); needsSave = true; }
        if (!config.contains("music.loop")) { config.set("music.loop", true); needsSave = true; }
        if (!config.contains("music.duration-ticks")) { config.set("music.duration-ticks", 6000); needsSave = true; }
        if (!config.contains("on-start-commands")) { config.set("on-start-commands", new ArrayList<>()); needsSave = true; }
        if (!config.contains("on-end-commands")) { config.set("on-end-commands", new ArrayList<>()); needsSave = true; }
        if (!config.contains("on-player-ready-commands")) { config.set("on-player-ready-commands", new ArrayList<>()); needsSave = true; }
        if (!config.contains("on-reset-commands")) { config.set("on-reset-commands", new ArrayList<>()); needsSave = true; }
        if (!config.contains("exempt-players")) { config.set("exempt-players", new ArrayList<>()); needsSave = true; }
        if (!config.contains("max-events-per-player")) { config.set("max-events-per-player", 6); needsSave = true; }
        if (!config.contains("rewards.commands")) { config.set("rewards.commands", new ArrayList<>()); needsSave = true; }

        // ── Devil's Dream-specific keys ──────────────────────────────────────
        if (!config.contains("world")) { config.set("world", ""); needsSave = true; }
        if (!config.contains("spawn.base-interval-ticks")) { config.set("spawn.base-interval-ticks", 45); needsSave = true; }
        if (!config.contains("spawn.max-events-per-player")) { config.set("spawn.max-events-per-player", 6); needsSave = true; }
        if (!config.contains("spawn.offset-radius")) { config.set("spawn.offset-radius", 8.0); needsSave = true; }
        if (!config.contains("adaptation.score-per-action")) { config.set("adaptation.score-per-action", 3); needsSave = true; }
        if (!config.contains("adaptation.decay-interval-ticks")) { config.set("adaptation.decay-interval-ticks", 100); needsSave = true; }
        if (!config.contains("adaptation.decay-amount")) { config.set("adaptation.decay-amount", 1); needsSave = true; }
        if (!config.contains("adaptation.threshold")) { config.set("adaptation.threshold", 50); needsSave = true; }
        if (!config.contains("adaptation.max-score")) { config.set("adaptation.max-score", 200); needsSave = true; }
        if (!config.contains("adaptation.max-multiplier")) { config.set("adaptation.max-multiplier", 3.0); needsSave = true; }
        if (!config.contains("mythicmobs.enabled")) { config.set("mythicmobs.enabled", true); needsSave = true; }
        if (!config.contains("mythicmobs.spawn-interval-ticks")) { config.set("mythicmobs.spawn-interval-ticks", 400); needsSave = true; }
        if (!config.contains("mythicmobs.max-alive")) { config.set("mythicmobs.max-alive", 8); needsSave = true; }

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
            plugin.getLogger().severe("Failed to save devilsdream config: " + e.getMessage());
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

    public long getDefaultTimerSeconds() { return config.getLong("timer.default-seconds", 900); }
    public long getMaxTimerSeconds() { return config.getLong("timer.max-seconds", 2400); }

    // ========================
    // Spawning
    // ========================

    public int getBaseSpawnInterval() { return config.getInt("spawn.base-interval-ticks", 45); }
    public int getMaxEventsPerPlayer() { return config.getInt("spawn.max-events-per-player", 6); }
    public double getSpawnOffsetRadius() { return config.getDouble("spawn.offset-radius", 8.0); }

    // ========================
    // Dream Adaptation
    // ========================

    public int getScorePerAction() { return config.getInt("adaptation.score-per-action", 3); }
    public int getDecayIntervalTicks() { return config.getInt("adaptation.decay-interval-ticks", 100); }
    public int getDecayAmount() { return config.getInt("adaptation.decay-amount", 1); }
    public int getAdaptationThreshold() { return config.getInt("adaptation.threshold", 50); }
    public int getMaxScore() { return config.getInt("adaptation.max-score", 200); }
    public double getMaxAdaptationMultiplier() { return config.getDouble("adaptation.max-multiplier", 3.0); }

    // ========================
    // MythicMobs
    // ========================

    public boolean isMythicMobsEnabled() { return config.getBoolean("mythicmobs.enabled", true); }
    public int getMobSpawnInterval() { return config.getInt("mythicmobs.spawn-interval-ticks", 400); }
    public int getMaxMobsAlive() { return config.getInt("mythicmobs.max-alive", 8); }
    public double getMobHealthMultiplier() { return config.getDouble("mythicmobs.health-multiplier", 1.0); }
    public double getMobDamageMultiplier() { return config.getDouble("mythicmobs.damage-multiplier", 1.0); }

    public List<String> getMobIds() {
        return config.getStringList("mythicmobs.mob-ids");
    }

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
    // Default config creation
    // ========================

    private void createDefaults() {
        FileConfiguration defaults = new YamlConfiguration();

        defaults.set("world", "");
        defaults.setComments("world", List.of(
                "World name where Devil's Dream runs. Leave empty for the first loaded world (Overworld).",
                "Example: \"world\", \"nightmare_world\""));

        // Timer
        defaults.set("timer.default-seconds", 900);
        defaults.setComments("timer.default-seconds", List.of(
                "Default duration in seconds. 900 = 15 min. Devil's Dream is longer due to adaptation escalation."));
        defaults.set("timer.max-seconds", 2400);
        defaults.setComments("timer.max-seconds", List.of(
                "Maximum timer value allowed via command. 2400 = 40 min."));

        // Spawning
        defaults.set("spawn.base-interval-ticks", 45);
        defaults.setComments("spawn.base-interval-ticks", List.of(
                "Ticks between attack spawn attempts. 45 = every 2.25 seconds.",
                "Devil's Dream spawns slightly faster than Chain Mode due to dual block display + environmental events."));
        defaults.set("spawn.max-events-per-player", 6);
        defaults.setComments("spawn.max-events-per-player", List.of(
                "Max simultaneous active attacks per player. Higher than Chain Mode to support dual attack types.",
                "Recommended: 4-8."));
        defaults.set("spawn.offset-radius", 8.0);
        defaults.setComments("spawn.offset-radius", List.of(
                "Max distance from the player that non-tracking attacks can spawn."));

        // Dream Adaptation
        defaults.set("adaptation.score-per-action", 3);
        defaults.setComments("adaptation.score-per-action", List.of(
                "How many points each detected action adds to that action's score.",
                "Higher = faster adaptation escalation. Recommended: 1-5."));
        defaults.set("adaptation.decay-interval-ticks", 100);
        defaults.setComments("adaptation.decay-interval-ticks", List.of(
                "Ticks between each score decay tick. 100 = every 5 seconds all scores lose some points.",
                "This prevents scores from permanently maxing out — switching behavior helps."));
        defaults.set("adaptation.decay-amount", 1);
        defaults.setComments("adaptation.decay-amount", List.of(
                "Points removed from every action score each decay tick. Higher = faster recovery."));
        defaults.set("adaptation.threshold", 50);
        defaults.setComments("adaptation.threshold", List.of(
                "Score at which the dream starts actively responding to that action category.",
                "Below this: normal attack spawning. Above this: targeted response attacks increase."));
        defaults.set("adaptation.max-score", 200);
        defaults.setComments("adaptation.max-score", List.of(
                "Maximum score any single action can reach. Caps escalation."));
        defaults.set("adaptation.max-multiplier", 3.0);
        defaults.setComments("adaptation.max-multiplier", List.of(
                "Maximum spawn rate multiplier for adapted attack categories.",
                "At threshold = 1.5x, at 2x threshold = 2.0x, capped at this value."));

        // MythicMobs
        defaults.set("mythicmobs.enabled", true);
        defaults.setComments("mythicmobs.enabled", List.of(
                "Enable MythicMobs nightmare creature spawning during Devil's Dream.",
                "Requires MythicMobs plugin installed. Set to false if not using MythicMobs."));
        defaults.set("mythicmobs.spawn-interval-ticks", 400);
        defaults.setComments("mythicmobs.spawn-interval-ticks", List.of(
                "Ticks between mob spawn attempts. 400 = every 20 seconds."));
        defaults.set("mythicmobs.max-alive", 8);
        defaults.setComments("mythicmobs.max-alive", List.of(
                "Maximum nightmare mobs alive at once across all players."));
        defaults.set("mythicmobs.health-multiplier", 1.0);
        defaults.setComments("mythicmobs.health-multiplier", List.of(
                "Base health multiplier for spawned nightmare mobs. Scales with adaptation scores."));
        defaults.set("mythicmobs.damage-multiplier", 1.0);
        defaults.setComments("mythicmobs.damage-multiplier", List.of(
                "Base damage multiplier for spawned nightmare mobs. Scales with adaptation scores."));
        defaults.set("mythicmobs.mob-ids", List.of(
                "NightmareHound", "DreamWraith", "ShadowStalker", "InfernalImp",
                "NightmareBrute", "SoulHarvester", "DreamPhantom", "BoneRevenant"));
        defaults.setComments("mythicmobs.mob-ids", List.of(
                "List of MythicMobs internal IDs to spawn during Devil's Dream.",
                "These must match mob configs in MythicMobs/Mobs/ YAML files.",
                "The spawner selects mobs based on which adaptation score is highest."));

        // Timer HUD
        defaults.set("timer-hud.display-name", "DEVIL'S DREAM");
        defaults.setComments("timer-hud.display-name", List.of(
                "Text shown on the BetterHud mode timer bar."));
        defaults.set("timer-hud.color", "dark_red");
        defaults.setComments("timer-hud.color", List.of(
                "Color of the mode name text in the timer bar."));
        defaults.set("timer-hud.flash-color", "red");
        defaults.set("timer-hud.flash-threshold-seconds", 60);

        // Music
        defaults.set("music.sound-id", "");
        defaults.setComments("music.sound-id", List.of(
                "Background music sound ID. Example: chaoscraft:music.devils_dream"));
        defaults.set("music.loop", true);
        defaults.set("music.duration-ticks", 6000);

        // Lifecycle
        defaults.set("on-start-commands", new ArrayList<>());
        defaults.setComments("on-start-commands", List.of(
                "Console commands run when Devil's Dream starts. Use %player% for the starting player."));
        defaults.set("on-end-commands", new ArrayList<>());
        defaults.set("on-player-ready-commands", new ArrayList<>());
        defaults.setComments("on-player-ready-commands", List.of(
                "Commands run for each player when they exit title screen or change world during this mode.",
                "Supports wait <ticks>, done, and PlaceholderAPI. Use %player% for the player's name."));
        defaults.set("on-reset-commands", new ArrayList<>());
        defaults.setComments("on-reset-commands", List.of(
                "Per-mode reset commands. Available for manual use or future expansion."));
        defaults.set("exempt-players", new ArrayList<>());
        defaults.set("rewards.commands", new ArrayList<>());

        // ── Universal Mob Spawning ──────────────────────────────────────
        // Devil's Dream already has MythicMobs nightmare creatures above.
        // This section supports additional vanilla or MythicMobs mob waves.
        MobSpawnConfig.writeDefaults(defaults, List.of(
                new MobSpawnConfig.MobSpawnDefaultEntry("NightmareHound", "mythicmobs", 10, 1, 2, 1.0, 1.0),
                new MobSpawnConfig.MobSpawnDefaultEntry("DreamWraith", "mythicmobs", 8, 1, 1, 1.2, 1.0),
                new MobSpawnConfig.MobSpawnDefaultEntry("PHANTOM", "vanilla", 5, 1, 3, 1.5, 1.0)
        ));

        try {
            defaults.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create default devilsdream config: " + e.getMessage());
        }
    }
}
