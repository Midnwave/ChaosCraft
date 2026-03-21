package com.blockforge.chaoscraft.modes.seer;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Seer Mode configuration loader.
 * Reads from plugins/ChaosCraft/modes/seer/seer.yml
 */
public class SeerConfig {

    private static final int CURRENT_CONFIG_VERSION = 1;

    private final ChaosCraftPlugin plugin;
    private final File configFile;
    private FileConfiguration config;

    public SeerConfig(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        File modeDir = new File(plugin.getDataFolder(), "modes/seer");
        if (!modeDir.exists()) modeDir.mkdirs();
        this.configFile = new File(modeDir, "seer.yml");
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
        if (!config.contains("timer.default-seconds")) { config.set("timer.default-seconds", 1200); needsSave = true; }
        if (!config.contains("timer.max-seconds")) { config.set("timer.max-seconds", 3600); needsSave = true; }
        if (!config.contains("music.sound-id")) { config.set("music.sound-id", ""); needsSave = true; }
        if (!config.contains("music.loop")) { config.set("music.loop", true); needsSave = true; }
        if (!config.contains("music.duration-ticks")) { config.set("music.duration-ticks", 6000); needsSave = true; }
        if (!config.contains("on-start-commands")) { config.set("on-start-commands", new ArrayList<>()); needsSave = true; }
        if (!config.contains("on-end-commands")) { config.set("on-end-commands", new ArrayList<>()); needsSave = true; }
        if (!config.contains("exempt-players")) { config.set("exempt-players", new ArrayList<>()); needsSave = true; }
        if (!config.contains("max-events-per-player")) { config.set("max-events-per-player", 4); needsSave = true; }
        if (!config.contains("rewards.commands")) { config.set("rewards.commands", new ArrayList<>()); needsSave = true; }

        // ── Seer specific keys ──────────────────────────────────────────
        if (!config.contains("world")) { config.set("world", "seer_arena"); needsSave = true; }
        if (!config.contains("spawn.base-interval-ticks")) { config.set("spawn.base-interval-ticks", 60); needsSave = true; }
        if (!config.contains("spawn.max-events-per-player")) { config.set("spawn.max-events-per-player", 4); needsSave = true; }
        if (!config.contains("spawn.offset-radius")) { config.set("spawn.offset-radius", 12.0); needsSave = true; }

        // ── Boss config ─────────────────────────────────────────────────
        if (!config.contains("boss.mythicmob-id")) { config.set("boss.mythicmob-id", "seer_boss"); needsSave = true; }
        if (!config.contains("boss.modelengine-id")) { config.set("boss.modelengine-id", "eyeboss"); needsSave = true; }
        if (!config.contains("boss.scale")) { config.set("boss.scale", 3.0); needsSave = true; }
        if (!config.contains("boss.float-height")) { config.set("boss.float-height", 15.0); needsSave = true; }
        if (!config.contains("boss.move-speed")) { config.set("boss.move-speed", 0.3); needsSave = true; }
        if (!config.contains("boss.detection-range")) { config.set("boss.detection-range", 500.0); needsSave = true; }
        if (!config.contains("boss.beam-range")) { config.set("boss.beam-range", 50.0); needsSave = true; }
        if (!config.contains("boss.beam-charge-ticks")) { config.set("boss.beam-charge-ticks", 100); needsSave = true; }
        if (!config.contains("boss.beam-damage-per-tick")) { config.set("boss.beam-damage-per-tick", 1.0); needsSave = true; }
        if (!config.contains("boss.ambient-sound")) { config.set("boss.ambient-sound", ""); needsSave = true; }
        if (!config.contains("boss.ambient-sound-interval")) { config.set("boss.ambient-sound-interval", 40); needsSave = true; }
        if (!config.contains("boss.ambient-sound-volume")) { config.set("boss.ambient-sound-volume", 2.0); needsSave = true; }

        // ── Orb config ──────────────────────────────────────────────────
        if (!config.contains("orbs.count")) { config.set("orbs.count", 10); needsSave = true; }
        if (!config.contains("orbs.material")) { config.set("orbs.material", "CRYING_OBSIDIAN"); needsSave = true; }
        if (!config.contains("orbs.hardness-multiplier")) { config.set("orbs.hardness-multiplier", 5.0); needsSave = true; }
        if (!config.contains("orbs.health-per-orb")) { config.set("orbs.health-per-orb", 10000000); needsSave = true; }
        if (!config.contains("orbs.break-health-divisor")) { config.set("orbs.break-health-divisor", 2.0); needsSave = true; }
        if (!config.contains("orbs.break-knockback")) { config.set("orbs.break-knockback", 2.0); needsSave = true; }

        // ── Orb positions ───────────────────────────────────────────────
        for (int i = 1; i <= 10; i++) {
            String key = "orbs.positions." + i;
            if (!config.contains(key + ".world")) { config.set(key + ".world", ""); needsSave = true; }
            if (!config.contains(key + ".x")) { config.set(key + ".x", 0); needsSave = true; }
            if (!config.contains(key + ".y")) { config.set(key + ".y", 64); needsSave = true; }
            if (!config.contains(key + ".z")) { config.set(key + ".z", 0); needsSave = true; }
        }

        // ── Timer HUD ───────────────────────────────────────────────────
        if (!config.contains("timer-hud.display-name")) { config.set("timer-hud.display-name", "SEER"); needsSave = true; }
        if (!config.contains("timer-hud.color")) { config.set("timer-hud.color", "#AA00FF"); needsSave = true; }
        if (!config.contains("timer-hud.flash-color")) { config.set("timer-hud.flash-color", "dark_purple"); needsSave = true; }
        if (!config.contains("timer-hud.flash-threshold-seconds")) { config.set("timer-hud.flash-threshold-seconds", 60); needsSave = true; }

        if (needsSave) {
            save();
            config = YamlConfiguration.loadConfiguration(configFile);
        }
    }

    private void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[Seer] Failed to save config: " + e.getMessage());
        }
    }

    private void createDefaults() {
        FileConfiguration defaults = new YamlConfiguration();

        defaults.set("config-version", CURRENT_CONFIG_VERSION);
        defaults.setComments("config-version", List.of("Seer Mode configuration. Do not edit config-version."));

        defaults.set("timer.default-seconds", 1200);
        defaults.setComments("timer.default-seconds", List.of("Default mode duration in seconds (1200 = 20 minutes)."));
        defaults.set("timer.max-seconds", 3600);

        defaults.set("music.sound-id", "");
        defaults.setComments("music.sound-id", List.of("Custom sound ID for background music. Leave empty for no music."));
        defaults.set("music.loop", true);
        defaults.set("music.duration-ticks", 6000);

        defaults.set("on-start-commands", new ArrayList<>());
        defaults.set("on-end-commands", new ArrayList<>());
        defaults.set("exempt-players", new ArrayList<>());
        defaults.set("max-events-per-player", 4);
        defaults.set("rewards.commands", new ArrayList<>());

        defaults.set("world", "seer_arena");
        defaults.setComments("world", List.of(
                "World name for the Seer arena. Must be loaded before mode starts."));

        defaults.set("spawn.base-interval-ticks", 60);
        defaults.setComments("spawn.base-interval-ticks", List.of(
                "Ticks between attack spawn attempts. Lower = more frequent attacks."));
        defaults.set("spawn.max-events-per-player", 4);
        defaults.set("spawn.offset-radius", 12.0);

        // Boss
        defaults.set("boss.mythicmob-id", "seer_boss");
        defaults.setComments("boss.mythicmob-id", List.of(
                "MythicMobs mob ID for the Seer boss. Requires MythicMobs + ModelEngine."));
        defaults.set("boss.modelengine-id", "eyeboss");
        defaults.set("boss.scale", 3.0);
        defaults.set("boss.float-height", 15.0);
        defaults.setComments("boss.float-height", List.of("Y offset above the nearest player the boss hovers at."));
        defaults.set("boss.move-speed", 0.3);
        defaults.set("boss.detection-range", 500.0);
        defaults.set("boss.beam-range", 50.0);
        defaults.setComments("boss.beam-range", List.of("Max distance from boss to target before beam powers down."));
        defaults.set("boss.beam-charge-ticks", 100);
        defaults.set("boss.beam-damage-per-tick", 1.0);

        // Orbs
        defaults.set("orbs.count", 10);
        defaults.setComments("orbs.count", List.of(
                "Number of crying obsidian orbs. Each orb destroyed reduces boss max HP by health-per-orb."));
        defaults.set("orbs.material", "CRYING_OBSIDIAN");
        defaults.set("orbs.hardness-multiplier", 5.0);
        defaults.set("orbs.health-per-orb", 10000000);
        defaults.setComments("orbs.health-per-orb", List.of("HP removed from boss max health per orb destroyed (10M default)."));
        defaults.set("orbs.break-health-divisor", 2.0);
        defaults.setComments("orbs.break-health-divisor", List.of(
                "Player's current health is divided by this when they break an orb. 2.0 = halved."));
        defaults.set("orbs.break-knockback", 2.0);

        // Orb positions
        for (int i = 1; i <= 10; i++) {
            String key = "orbs.positions." + i;
            defaults.set(key + ".world", "");
            defaults.set(key + ".x", 0);
            defaults.set(key + ".y", 64);
            defaults.set(key + ".z", 0);
        }
        defaults.setComments("orbs.positions.1.world", List.of(
                "Orb positions in the arena. Set via /chaos seer setorb <index> or manually."));

        // Timer HUD
        defaults.set("timer-hud.display-name", "SEER");
        defaults.set("timer-hud.color", "#AA00FF");
        defaults.set("timer-hud.flash-color", "dark_purple");
        defaults.setComments("timer-hud.flash-color", List.of(
                "Color name when timer is flashing (below threshold). Default: dark_purple."));
        defaults.set("timer-hud.flash-threshold-seconds", 60);

        try {
            defaults.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[Seer] Failed to create default config: " + e.getMessage());
        }
    }

    // ========================
    // Getters
    // ========================

    public long getDefaultTimerSeconds() { return config.getLong("timer.default-seconds", 1200); }
    public long getMaxTimerSeconds() { return config.getLong("timer.max-seconds", 3600); }
    public String getMusicSoundId() { return config.getString("music.sound-id", ""); }
    public boolean isMusicLoop() { return config.getBoolean("music.loop", true); }
    public int getMusicDurationTicks() { return config.getInt("music.duration-ticks", 6000); }
    public List<String> getOnStartCommands() { return config.getStringList("on-start-commands"); }
    public List<String> getOnEndCommands() { return config.getStringList("on-end-commands"); }
    public List<String> getExemptPlayers() { return config.getStringList("exempt-players"); }
    public int getMaxEventsPerPlayer() { return config.getInt("max-events-per-player", 4); }
    public List<String> getRewardCommands() { return config.getStringList("rewards.commands"); }

    public String getWorldName() { return config.getString("world", "seer_arena"); }
    public int getBaseSpawnInterval() { return config.getInt("spawn.base-interval-ticks", 60); }
    public int getSpawnMaxEventsPerPlayer() { return config.getInt("spawn.max-events-per-player", 4); }
    public double getSpawnOffsetRadius() { return config.getDouble("spawn.offset-radius", 12.0); }

    // Boss
    public String getBossMythicMobId() { return config.getString("boss.mythicmob-id", "seer_boss"); }
    public String getBossModelEngineId() { return config.getString("boss.modelengine-id", "eyeboss"); }
    public double getBossScale() { return config.getDouble("boss.scale", 3.0); }
    public double getBossFloatHeight() { return config.getDouble("boss.float-height", 15.0); }
    public double getBossMoveSpeed() { return config.getDouble("boss.move-speed", 0.3); }
    public double getBossDetectionRange() { return config.getDouble("boss.detection-range", 500.0); }
    public double getBossBeamRange() { return config.getDouble("boss.beam-range", 50.0); }
    public int getBossBeamChargeTicks() { return config.getInt("boss.beam-charge-ticks", 100); }
    public double getBossBeamDamagePerTick() { return config.getDouble("boss.beam-damage-per-tick", 1.0); }
    public String getBossAmbientSound() { return config.getString("boss.ambient-sound", ""); }
    public int getBossAmbientSoundInterval() { return config.getInt("boss.ambient-sound-interval", 40); }
    public float getBossAmbientSoundVolume() { return (float) config.getDouble("boss.ambient-sound-volume", 2.0); }

    // Orbs
    public int getOrbCount() { return config.getInt("orbs.count", 10); }
    public String getOrbMaterial() { return config.getString("orbs.material", "CRYING_OBSIDIAN"); }
    public double getOrbHardnessMultiplier() { return config.getDouble("orbs.hardness-multiplier", 5.0); }
    public long getOrbHealthPerOrb() { return config.getLong("orbs.health-per-orb", 10000000); }
    public double getOrbBreakHealthDivisor() { return config.getDouble("orbs.break-health-divisor", 2.0); }
    public double getOrbBreakKnockback() { return config.getDouble("orbs.break-knockback", 2.0); }

    /**
     * Get an orb position from config by index (1-based).
     * Returns null if the world is not loaded or the position is not set.
     */
    public Location getOrbPosition(int index) {
        String key = "orbs.positions." + index;
        String worldName = config.getString(key + ".world", "");
        if (worldName.isEmpty()) {
            worldName = getWorldName();
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        double x = config.getDouble(key + ".x", 0);
        double y = config.getDouble(key + ".y", 64);
        double z = config.getDouble(key + ".z", 0);
        return new Location(world, x, y, z);
    }

    /**
     * Save an orb position to config by index (1-based).
     */
    public void setOrbPosition(int index, Location loc) {
        String key = "orbs.positions." + index;
        config.set(key + ".world", loc.getWorld().getName());
        config.set(key + ".x", loc.getX());
        config.set(key + ".y", loc.getY());
        config.set(key + ".z", loc.getZ());
        save();
    }

    // Timer HUD
    public String getTimerHudDisplayName() { return config.getString("timer-hud.display-name", "SEER"); }
    public String getTimerHudColor() { return config.getString("timer-hud.color", "#AA00FF"); }
    public String getTimerHudFlashColor() { return config.getString("timer-hud.flash-color", "dark_purple"); }
    public int getTimerHudFlashThresholdSeconds() { return config.getInt("timer-hud.flash-threshold-seconds", 60); }
}
