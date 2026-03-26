package com.blockforge.chaoscraft.modes.bluemoon;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.blockforge.chaoscraft.services.mobspawn.MobSpawnConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Blue Moon Mode configuration loader.
 * Reads from plugins/ChaosCraft/modes/bluemoon/bluemoon.yml
 */
public class BlueMoonConfig {

    private static final int CURRENT_CONFIG_VERSION = 2;

    private final ChaosCraftPlugin plugin;
    private final File configFile;
    private FileConfiguration config;

    public BlueMoonConfig(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        File modeDir = new File(plugin.getDataFolder(), "modes/bluemoon");
        if (!modeDir.exists()) modeDir.mkdirs();
        this.configFile = new File(modeDir, "bluemoon.yml");
        load();
    }

    public void load() {
        if (!configFile.exists()) {
            createDefaults();
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        boolean needsSave = false;

        // ── Base mode keys ──────────────────────────────────────────────
        if (!config.contains("config-version")) { config.set("config-version", CURRENT_CONFIG_VERSION); needsSave = true; }
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
        if (!config.contains("max-events-per-player")) { config.set("max-events-per-player", 5); needsSave = true; }
        if (!config.contains("rewards.commands")) { config.set("rewards.commands", new ArrayList<>()); needsSave = true; }
        if (!config.contains("rewards.survived.money")) { config.set("rewards.survived.money", 0); needsSave = true; }
        if (!config.contains("rewards.survived.items")) { config.set("rewards.survived.items", new ArrayList<>()); needsSave = true; }
        if (!config.contains("rewards.survived.badges")) { config.set("rewards.survived.badges", new ArrayList<>()); needsSave = true; }
        if (!config.contains("rewards.survived.commands")) { config.set("rewards.survived.commands", new ArrayList<>()); needsSave = true; }
        if (!config.contains("rewards.died.money")) { config.set("rewards.died.money", 0); needsSave = true; }
        if (!config.contains("rewards.died.items")) { config.set("rewards.died.items", new ArrayList<>()); needsSave = true; }
        if (!config.contains("rewards.died.badges")) { config.set("rewards.died.badges", new ArrayList<>()); needsSave = true; }
        if (!config.contains("rewards.died.commands")) { config.set("rewards.died.commands", new ArrayList<>()); needsSave = true; }
        if (!config.contains("restrictions.allow-world-change")) { config.set("restrictions.allow-world-change", false); needsSave = true; }
        if (!config.contains("restrictions.water-to-glass")) { config.set("restrictions.water-to-glass", false); needsSave = true; }
        if (!config.contains("restrictions.allow-respawn")) { config.set("restrictions.allow-respawn", true); needsSave = true; }
        if (!config.contains("restrictions.allow-elytra")) { config.set("restrictions.allow-elytra", false); needsSave = true; }
        if (!config.contains("restrictions.blocked-commands")) { config.set("restrictions.blocked-commands", new ArrayList<>()); needsSave = true; }

        // ── Blue Moon specific keys ─────────────────────────────────────
        if (!config.contains("world")) { config.set("world", ""); needsSave = true; }
        if (!config.contains("force-night")) { config.set("force-night", true); needsSave = true; }
        if (!config.contains("spawn.base-interval-ticks")) { config.set("spawn.base-interval-ticks", 50); needsSave = true; }
        if (!config.contains("spawn.max-events-per-player")) { config.set("spawn.max-events-per-player", 5); needsSave = true; }
        if (!config.contains("spawn.offset-radius")) { config.set("spawn.offset-radius", 10.0); needsSave = true; }

        // ── Boss config ─────────────────────────────────────────────────
        if (!config.contains("boss.enabled")) { config.set("boss.enabled", true); needsSave = true; }
        if (!config.contains("boss.mythicmob-id")) { config.set("boss.mythicmob-id", "blue_moon_boss"); needsSave = true; }
        if (!config.contains("boss.modelengine-id")) { config.set("boss.modelengine-id", "blue_moon_boss"); needsSave = true; }
        if (!config.contains("boss.health")) { config.set("boss.health", 500.0); needsSave = true; }
        if (!config.contains("boss.float-height")) { config.set("boss.float-height", 25.0); needsSave = true; }
        if (!config.contains("boss.orbit-radius")) { config.set("boss.orbit-radius", 15.0); needsSave = true; }
        if (!config.contains("boss.orbit-speed")) { config.set("boss.orbit-speed", 0.02); needsSave = true; }
        if (!config.contains("boss.phase2-threshold")) { config.set("boss.phase2-threshold", 0.75); needsSave = true; }
        if (!config.contains("boss.phase3-threshold")) { config.set("boss.phase3-threshold", 0.50); needsSave = true; }
        if (!config.contains("boss.phase4-threshold")) { config.set("boss.phase4-threshold", 0.25); needsSave = true; }
        if (!config.contains("boss.phase4-enrage-seconds")) { config.set("boss.phase4-enrage-seconds", 60); needsSave = true; }
        if (!config.contains("boss.spawn-delay-ticks")) { config.set("boss.spawn-delay-ticks", 100); needsSave = true; }

        // ── Super Laser config ──────────────────────────────────────────
        if (!config.contains("boss.super-laser.enabled")) { config.set("boss.super-laser.enabled", true); needsSave = true; }
        if (!config.contains("boss.super-laser.damage")) { config.set("boss.super-laser.damage", 12.0); needsSave = true; }
        if (!config.contains("boss.super-laser.beam-damage-multiplier")) { config.set("boss.super-laser.beam-damage-multiplier", 3.0); needsSave = true; }
        if (!config.contains("boss.super-laser.charge-ticks")) { config.set("boss.super-laser.charge-ticks", 40); needsSave = true; }
        if (!config.contains("boss.super-laser.duration-ticks")) { config.set("boss.super-laser.duration-ticks", 60); needsSave = true; }
        if (!config.contains("boss.super-laser.cooldown-ticks")) { config.set("boss.super-laser.cooldown-ticks", 600); needsSave = true; }

        // ── Rewards ─────────────────────────────────────────────────────
        if (!config.contains("rewards.early-kill-bonus-commands")) { config.set("rewards.early-kill-bonus-commands", new ArrayList<>()); needsSave = true; }

        // ── Timer HUD ───────────────────────────────────────────────────
        if (!config.contains("timer-hud.display-name")) { config.set("timer-hud.display-name", "BLUE MOON"); needsSave = true; }
        if (!config.contains("timer-hud.color")) { config.set("timer-hud.color", "#88CCFF"); needsSave = true; }
        if (!config.contains("timer-hud.flash-color")) { config.set("timer-hud.flash-color", "red"); needsSave = true; }
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

    private void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[BlueMoon] Failed to save config: " + e.getMessage());
        }
    }

    private void createDefaults() {
        FileConfiguration defaults = new YamlConfiguration();

        defaults.set("config-version", CURRENT_CONFIG_VERSION);
        defaults.setComments("config-version", List.of("Blue Moon Mode configuration. Do not edit config-version."));

        defaults.set("timer.default-seconds", 900);
        defaults.setComments("timer.default-seconds", List.of("Default mode duration in seconds (900 = 15 minutes)."));
        defaults.set("timer.max-seconds", 1800);

        defaults.set("music.sound-id", "");
        defaults.setComments("music.sound-id", List.of("Custom sound ID for background music. Leave empty for no music."));
        defaults.set("music.loop", true);
        defaults.set("music.duration-ticks", 6000);

        defaults.set("on-start-commands", new ArrayList<>());
        defaults.set("on-end-commands", new ArrayList<>());
        defaults.set("on-player-ready-commands", new ArrayList<>());
        defaults.setComments("on-player-ready-commands", List.of(
                "Commands run for each player when they exit title screen or change world during this mode.",
                "Supports wait <ticks>, done, and PlaceholderAPI. Use %player% for the player's name."));
        defaults.set("on-reset-commands", new ArrayList<>());
        defaults.setComments("on-reset-commands", List.of(
                "Per-mode reset commands. Available for manual use or future expansion."));
        defaults.set("exempt-players", new ArrayList<>());
        defaults.set("max-events-per-player", 5);
        defaults.set("rewards.commands", new ArrayList<>());
        defaults.set("rewards.early-kill-bonus-commands", new ArrayList<>());
        defaults.setComments("rewards.early-kill-bonus-commands", List.of(
                "Commands to run if the boss is killed before the timer expires."));

        defaults.set("world", "");
        defaults.setComments("world", List.of(
                "World name for Blue Moon. Leave empty to use the first loaded Overworld."));
        defaults.set("force-night", true);
        defaults.setComments("force-night", List.of(
                "Force the world to midnight when the mode starts. Time restores on end."));

        defaults.set("spawn.base-interval-ticks", 50);
        defaults.setComments("spawn.base-interval-ticks", List.of(
                "Ticks between attack spawn attempts. Lower = more frequent attacks."));
        defaults.set("spawn.max-events-per-player", 5);
        defaults.set("spawn.offset-radius", 10.0);

        // Boss
        defaults.set("boss.enabled", true);
        defaults.setComments("boss.enabled", List.of(
                "Whether the Blue Moon boss spawns. Requires MythicMobs + ModelEngine."));
        defaults.set("boss.mythicmob-id", "blue_moon_boss");
        defaults.set("boss.modelengine-id", "blue_moon_boss");
        defaults.set("boss.health", 500.0);
        defaults.set("boss.float-height", 25.0);
        defaults.setComments("boss.float-height", List.of("Y offset above the nearest player the boss hovers at."));
        defaults.set("boss.orbit-radius", 15.0);
        defaults.set("boss.orbit-speed", 0.02);
        defaults.set("boss.phase2-threshold", 0.75);
        defaults.set("boss.phase3-threshold", 0.50);
        defaults.set("boss.phase4-threshold", 0.25);
        defaults.set("boss.phase4-enrage-seconds", 60);
        defaults.setComments("boss.phase4-enrage-seconds", List.of(
                "Seconds in phase 4 before boss enrages (heals 10% and re-enters phase 3)."));
        defaults.set("boss.spawn-delay-ticks", 100);

        // Super laser
        defaults.set("boss.super-laser.enabled", true);
        defaults.setComments("boss.super-laser.enabled", List.of(
                "The signature inescapable boss attack. Fires a massive beam from the moon."));
        defaults.set("boss.super-laser.damage", 12.0);
        defaults.setComments("boss.super-laser.damage", List.of(
                "Unavoidable damage dealt to ALL players during the super laser (half-hearts)."));
        defaults.set("boss.super-laser.beam-damage-multiplier", 3.0);
        defaults.setComments("boss.super-laser.beam-damage-multiplier", List.of(
                "Damage multiplier for players directly IN the beam path."));
        defaults.set("boss.super-laser.charge-ticks", 40);
        defaults.set("boss.super-laser.duration-ticks", 60);
        defaults.set("boss.super-laser.cooldown-ticks", 600);

        defaults.set("timer-hud.display-name", "BLUE MOON");
        defaults.set("timer-hud.color", "#88CCFF");
        defaults.set("timer-hud.flash-color", "red");
        defaults.setComments("timer-hud.flash-color", List.of(
                "Color name when timer is flashing (below threshold). Default: red."));
        defaults.set("timer-hud.flash-threshold-seconds", 60);
        defaults.setComments("timer-hud.flash-threshold-seconds", List.of(
                "Seconds remaining when the timer starts flashing. Default: 60."));

        // ── Player Restrictions ──────────────────────────────────────────────────
        defaults.set("restrictions.allow-world-change", false);
        defaults.setComments("restrictions.allow-world-change", List.of(
            "Whether players can change worlds during this mode. Default: false."));
        defaults.set("restrictions.water-to-glass", false);
        defaults.setComments("restrictions.water-to-glass", List.of(
            "Replace water with light blue glass to prevent AI abuse. Default: false."));
        defaults.set("restrictions.allow-respawn", true);
        defaults.setComments("restrictions.allow-respawn", List.of(
            "If false, dead players enter spectator mode until the mode ends. Default: true."));
        defaults.set("restrictions.allow-elytra", false);
        defaults.setComments("restrictions.allow-elytra", List.of(
            "Whether players can use elytra during this mode. Default: false."));
        defaults.set("restrictions.blocked-commands", new ArrayList<>());
        defaults.setComments("restrictions.blocked-commands", List.of(
            "Commands blocked during this mode. Example: home, tpa, spawn, warp"));

        // ── Universal Mob Spawning ──────────────────────────────────────
        MobSpawnConfig.writeDefaults(defaults, List.of(
                new MobSpawnConfig.MobSpawnDefaultEntry("PHANTOM", "vanilla", 8, 1, 3, 1.5, 1.2),
                new MobSpawnConfig.MobSpawnDefaultEntry("VEX", "vanilla", 5, 2, 4, 1.0, 1.0),
                new MobSpawnConfig.MobSpawnDefaultEntry("SKELETON", "vanilla", 6, 1, 2, 1.3, 1.0)
        ));

        try {
            defaults.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[BlueMoon] Failed to create default config: " + e.getMessage());
        }
    }

    // ========================
    // Getters
    // ========================

    public long getDefaultTimerSeconds() { return config.getLong("timer.default-seconds", 900); }
    public long getMaxTimerSeconds() { return config.getLong("timer.max-seconds", 1800); }
    public String getMusicSoundId() { return config.getString("music.sound-id", ""); }
    public boolean isMusicLoop() { return config.getBoolean("music.loop", true); }
    public int getMusicDurationTicks() { return config.getInt("music.duration-ticks", 6000); }
    public List<String> getOnStartCommands() { return config.getStringList("on-start-commands"); }
    public List<String> getOnEndCommands() { return config.getStringList("on-end-commands"); }
    public List<String> getExemptPlayers() { return config.getStringList("exempt-players"); }
    public int getMaxEventsPerPlayer() { return config.getInt("max-events-per-player", 5); }
    public List<String> getRewardCommands() { return config.getStringList("rewards.commands"); }
    public List<String> getEarlyKillBonusCommands() { return config.getStringList("rewards.early-kill-bonus-commands"); }

    public String getWorldName() { return config.getString("world", ""); }
    public boolean isForceNight() { return config.getBoolean("force-night", true); }
    public int getBaseSpawnInterval() { return config.getInt("spawn.base-interval-ticks", 50); }
    public int getSpawnMaxEventsPerPlayer() { return config.getInt("spawn.max-events-per-player", 5); }
    public double getSpawnOffsetRadius() { return config.getDouble("spawn.offset-radius", 10.0); }

    // Boss
    public boolean isBossEnabled() { return config.getBoolean("boss.enabled", true); }
    public String getBossMythicMobId() { return config.getString("boss.mythicmob-id", "blue_moon_boss"); }
    public String getBossModelEngineId() { return config.getString("boss.modelengine-id", "blue_moon_boss"); }
    public double getBossHealth() { return config.getDouble("boss.health", 500.0); }
    public double getBossFloatHeight() { return config.getDouble("boss.float-height", 25.0); }
    public double getBossOrbitRadius() { return config.getDouble("boss.orbit-radius", 15.0); }
    public double getBossOrbitSpeed() { return config.getDouble("boss.orbit-speed", 0.02); }
    public double getBossPhase2Threshold() { return config.getDouble("boss.phase2-threshold", 0.75); }
    public double getBossPhase3Threshold() { return config.getDouble("boss.phase3-threshold", 0.50); }
    public double getBossPhase4Threshold() { return config.getDouble("boss.phase4-threshold", 0.25); }
    public int getBossPhase4EnrageSeconds() { return config.getInt("boss.phase4-enrage-seconds", 60); }
    public int getBossSpawnDelayTicks() { return config.getInt("boss.spawn-delay-ticks", 100); }

    // Super laser
    public boolean isSuperLaserEnabled() { return config.getBoolean("boss.super-laser.enabled", true); }
    public double getSuperLaserDamage() { return config.getDouble("boss.super-laser.damage", 12.0); }
    public double getSuperLaserBeamMultiplier() { return config.getDouble("boss.super-laser.beam-damage-multiplier", 3.0); }
    public int getSuperLaserChargeTicks() { return config.getInt("boss.super-laser.charge-ticks", 40); }
    public int getSuperLaserDurationTicks() { return config.getInt("boss.super-laser.duration-ticks", 60); }
    public int getSuperLaserCooldownTicks() { return config.getInt("boss.super-laser.cooldown-ticks", 200); }

    // Laser sounds (all configurable)
    public String getLaserChargeSound() { return config.getString("boss.super-laser.sounds.charge", "minecraft:block.beacon.activate"); }
    public float getLaserChargeSoundVolume() { return (float) config.getDouble("boss.super-laser.sounds.charge-volume", 3.0); }
    public float getLaserChargeSoundPitch() { return (float) config.getDouble("boss.super-laser.sounds.charge-pitch", 0.3); }
    public String getLaserFireSound() { return config.getString("boss.super-laser.sounds.fire", "minecraft:entity.ender_dragon.growl"); }
    public float getLaserFireSoundVolume() { return (float) config.getDouble("boss.super-laser.sounds.fire-volume", 2.0); }
    public float getLaserFireSoundPitch() { return (float) config.getDouble("boss.super-laser.sounds.fire-pitch", 0.3); }
    public String getLaserEndSound() { return config.getString("boss.super-laser.sounds.end", "minecraft:block.beacon.deactivate"); }
    public float getLaserEndSoundVolume() { return (float) config.getDouble("boss.super-laser.sounds.end-volume", 2.0); }
    public float getLaserEndSoundPitch() { return (float) config.getDouble("boss.super-laser.sounds.end-pitch", 0.8); }

    // Laser beam behavior
    /** Radians per tick the beam sweeps. 0.05 = full 360° in ~125 ticks. Higher = faster sweep. */
    public double getSuperLaserSweepSpeed() { return config.getDouble("boss.super-laser.sweep-speed", 0.05); }
    /** Radius of the beam's circular sweep path around the boss (blocks). */
    public double getSuperLaserOrbitRadius() { return config.getDouble("boss.super-laser.orbit-radius", 5.0); }
    /** Players within this distance of the beam column take multiplied damage. */
    public double getSuperLaserBeamHitRadius() { return config.getDouble("boss.super-laser.beam-hit-radius", 3.0); }
    /** Ticks between each damage application during the fire phase. 20 = once per second. */
    public int getSuperLaserDamageInterval() { return config.getInt("boss.super-laser.damage-interval-ticks", 20); }

    // Boss attack cooldown per phase (ticks between BOSS-type attack spawns)
    public int getBossAttackCooldownPhase1() { return config.getInt("boss.attack-cooldown.phase-1", 120); }
    public int getBossAttackCooldownPhase2() { return config.getInt("boss.attack-cooldown.phase-2", 100); }
    public int getBossAttackCooldownPhase3() { return config.getInt("boss.attack-cooldown.phase-3", 80); }
    public int getBossAttackCooldownPhase4() { return config.getInt("boss.attack-cooldown.phase-4", 60); }

    // Laser ModelEngine model (spawns inside the boss during laser)
    public String getLaserModelEngineId() { return config.getString("boss.super-laser.modelengine-id", "chaoscraft_laser"); }

    // Timer HUD
    public String getTimerHudDisplayName() { return config.getString("timer-hud.display-name", "BLUE MOON"); }
    public String getTimerHudColor() { return config.getString("timer-hud.color", "#88CCFF"); }
}
