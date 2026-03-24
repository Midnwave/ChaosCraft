package com.blockforge.chaoscraft.modes.doom;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.services.mobspawn.MobSpawnConfig;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Doom Mode configuration loader.
 * Reads from plugins/ChaosCraft/modes/doom/doom.yml
 *
 * Doom Mode is a lava-rise survival mode where players are trapped in a
 * defined arena while lava rises from below. Features block display,
 * environmental, and ModelEngine attacks alongside the rising lava.
 */
public class DoomConfig {

    private static final int CURRENT_CONFIG_VERSION = 2;

    private final ChaosCraftPlugin plugin;
    private final File configFile;
    private FileConfiguration config;

    public DoomConfig(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        File modeDir = new File(plugin.getDataFolder(), "modes/doom");
        if (!modeDir.exists()) modeDir.mkdirs();
        this.configFile = new File(modeDir, "doom.yml");
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
        if (config.getInt("config-version") < CURRENT_CONFIG_VERSION) { config.set("config-version", CURRENT_CONFIG_VERSION); needsSave = true; }
        if (!config.contains("timer.default-seconds")) { config.set("timer.default-seconds", 600); needsSave = true; }
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

        // ── Doom-specific: world ────────────────────────────────────────
        if (!config.contains("world")) { config.set("world", ""); needsSave = true; }

        // ── Doom-specific: arena ────────────────────────────────────────
        if (!config.contains("arena.pos1.x")) { config.set("arena.pos1.x", 0); needsSave = true; }
        if (!config.contains("arena.pos1.y")) { config.set("arena.pos1.y", 0); needsSave = true; }
        if (!config.contains("arena.pos1.z")) { config.set("arena.pos1.z", 0); needsSave = true; }
        if (!config.contains("arena.pos2.x")) { config.set("arena.pos2.x", 0); needsSave = true; }
        if (!config.contains("arena.pos2.y")) { config.set("arena.pos2.y", 0); needsSave = true; }
        if (!config.contains("arena.pos2.z")) { config.set("arena.pos2.z", 0); needsSave = true; }
        if (!config.contains("arena.world")) { config.set("arena.world", ""); needsSave = true; }
        if (!config.contains("arena.enforce-boundary")) { config.set("arena.enforce-boundary", true); needsSave = true; }
        if (!config.contains("arena.boundary-push-strength")) { config.set("arena.boundary-push-strength", 0.5); needsSave = true; }

        // ── Doom-specific: lava rise ────────────────────────────────────
        if (!config.contains("lava-rise.enabled")) { config.set("lava-rise.enabled", true); needsSave = true; }
        if (!config.contains("lava-rise.start-y")) { config.set("lava-rise.start-y", 0); needsSave = true; }
        if (!config.contains("lava-rise.max-y")) { config.set("lava-rise.max-y", 100); needsSave = true; }
        if (!config.contains("lava-rise.blocks-per-level-tick")) { config.set("lava-rise.blocks-per-level-tick", 500); needsSave = true; }
        if (!config.contains("lava-rise.rise-interval-ticks")) { config.set("lava-rise.rise-interval-ticks", 200); needsSave = true; }
        if (!config.contains("lava-rise.rise-amount")) { config.set("lava-rise.rise-amount", 1); needsSave = true; }
        if (!config.contains("lava-rise.cleanup-on-end")) { config.set("lava-rise.cleanup-on-end", true); needsSave = true; }

        // ── Doom-specific: lava damage ──────────────────────────────────
        if (!config.contains("lava-damage.enabled")) { config.set("lava-damage.enabled", true); needsSave = true; }
        if (!config.contains("lava-damage.damage-per-tick")) { config.set("lava-damage.damage-per-tick", 2.0); needsSave = true; }
        if (!config.contains("lava-damage.damage-interval-ticks")) { config.set("lava-damage.damage-interval-ticks", 20); needsSave = true; }

        // ── Spawning ────────────────────────────────────────────────────
        if (!config.contains("spawn.base-interval-ticks")) { config.set("spawn.base-interval-ticks", 50); needsSave = true; }
        if (!config.contains("spawn.max-events-per-player")) { config.set("spawn.max-events-per-player", 5); needsSave = true; }
        if (!config.contains("spawn.offset-radius")) { config.set("spawn.offset-radius", 10.0); needsSave = true; }
        if (!config.contains("spawn.type-weights.block-display")) { config.set("spawn.type-weights.block-display", 0.45); needsSave = true; }
        if (!config.contains("spawn.type-weights.environmental")) { config.set("spawn.type-weights.environmental", 0.35); needsSave = true; }
        if (!config.contains("spawn.type-weights.model-engine")) { config.set("spawn.type-weights.model-engine", 0.20); needsSave = true; }

        // ── Timer HUD ───────────────────────────────────────────────────
        if (!config.contains("timer-hud.display-name")) { config.set("timer-hud.display-name", "DOOM"); needsSave = true; }
        if (!config.contains("timer-hud.color")) { config.set("timer-hud.color", "red"); needsSave = true; }
        if (!config.contains("timer-hud.flash-color")) { config.set("timer-hud.flash-color", "dark_red"); needsSave = true; }
        if (!config.contains("timer-hud.flash-threshold-seconds")) { config.set("timer-hud.flash-threshold-seconds", 60); needsSave = true; }

        // ── Universal mob spawning ──────────────────────────────────────
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
            plugin.getLogger().severe("Failed to save doom config: " + e.getMessage());
        }
    }

    public FileConfiguration get() { return config; }

    // ========================
    // Arena position setters (called from /cc function commands)
    // ========================

    public void setArenaPos1(Location loc) {
        config.set("arena.pos1.x", loc.getBlockX());
        config.set("arena.pos1.y", loc.getBlockY());
        config.set("arena.pos1.z", loc.getBlockZ());
        config.set("arena.world", loc.getWorld() != null ? loc.getWorld().getName() : "");
        save();
    }

    public void setArenaPos2(Location loc) {
        config.set("arena.pos2.x", loc.getBlockX());
        config.set("arena.pos2.y", loc.getBlockY());
        config.set("arena.pos2.z", loc.getBlockZ());
        if (config.getString("arena.world", "").isEmpty() && loc.getWorld() != null) {
            config.set("arena.world", loc.getWorld().getName());
        }
        save();
    }

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
    // Arena
    // ========================

    public int getArenaPos1X() { return config.getInt("arena.pos1.x", 0); }
    public int getArenaPos1Y() { return config.getInt("arena.pos1.y", 0); }
    public int getArenaPos1Z() { return config.getInt("arena.pos1.z", 0); }
    public int getArenaPos2X() { return config.getInt("arena.pos2.x", 0); }
    public int getArenaPos2Y() { return config.getInt("arena.pos2.y", 0); }
    public int getArenaPos2Z() { return config.getInt("arena.pos2.z", 0); }
    public String getArenaWorldName() { return config.getString("arena.world", ""); }
    public boolean isEnforceBoundary() { return config.getBoolean("arena.enforce-boundary", true); }
    public double getBoundaryPushStrength() { return config.getDouble("arena.boundary-push-strength", 0.5); }

    // ========================
    // Lava Rise
    // ========================

    public boolean isLavaRiseEnabled() { return config.getBoolean("lava-rise.enabled", true); }
    public int getLavaRiseStartY() { return config.getInt("lava-rise.start-y", 0); }
    public int getLavaRiseMaxY() { return config.getInt("lava-rise.max-y", 100); }
    public int getBlocksPerLevelTick() { return config.getInt("lava-rise.blocks-per-level-tick", 500); }
    public int getRiseIntervalTicks() { return config.getInt("lava-rise.rise-interval-ticks", 200); }
    public int getRiseAmount() { return config.getInt("lava-rise.rise-amount", 1); }
    public boolean isCleanupOnEnd() { return config.getBoolean("lava-rise.cleanup-on-end", true); }

    // ========================
    // Lava Damage
    // ========================

    public boolean isLavaDamageEnabled() { return config.getBoolean("lava-damage.enabled", true); }
    public double getLavaDamagePerTick() { return config.getDouble("lava-damage.damage-per-tick", 2.0); }
    public int getLavaDamageIntervalTicks() { return config.getInt("lava-damage.damage-interval-ticks", 20); }

    // ========================
    // Spawning
    // ========================

    public int getBaseSpawnInterval() { return config.getInt("spawn.base-interval-ticks", 50); }
    public int getMaxEventsPerPlayer() { return config.getInt("spawn.max-events-per-player", 5); }
    public double getSpawnOffsetRadius() { return config.getDouble("spawn.offset-radius", 10.0); }
    public double getTypeWeightBlockDisplay() { return config.getDouble("spawn.type-weights.block-display", 0.45); }
    public double getTypeWeightEnvironmental() { return config.getDouble("spawn.type-weights.environmental", 0.35); }
    public double getTypeWeightModelEngine() { return config.getDouble("spawn.type-weights.model-engine", 0.20); }

    // ========================
    // Timer HUD
    // ========================

    public String getTimerHudDisplayName() { return config.getString("timer-hud.display-name", "DOOM"); }
    public String getTimerHudColor() { return config.getString("timer-hud.color", "red"); }
    public String getTimerHudFlashColor() { return config.getString("timer-hud.flash-color", "dark_red"); }
    public int getTimerHudFlashThreshold() { return config.getInt("timer-hud.flash-threshold-seconds", 60); }

    // ========================
    // Default config creation
    // ========================

    private void createDefaults() {
        FileConfiguration defaults = new YamlConfiguration();

        defaults.set("config-version", CURRENT_CONFIG_VERSION);
        defaults.setComments("config-version", List.of(
                "Doom Mode configuration. Do not edit config-version manually."));

        // ── World ────────────────────────────────────────────────────────
        defaults.set("world", "");
        defaults.setComments("world", List.of(
                "World name where Doom Mode runs. Leave empty for the first loaded world.",
                "This is the world players must be in for attacks to target them."));

        // ── Timer ────────────────────────────────────────────────────────
        defaults.set("timer.default-seconds", 600);
        defaults.setComments("timer.default-seconds", List.of(
                "Default duration in seconds when started without a time argument.",
                "600 = 10 min. The lava keeps rising the whole time."));
        defaults.set("timer.max-seconds", 1800);
        defaults.setComments("timer.max-seconds", List.of(
                "Maximum timer value allowed via command. 1800 = 30 min."));

        // ── Arena ────────────────────────────────────────────────────────
        defaults.set("arena.pos1.x", 0);
        defaults.set("arena.pos1.y", 0);
        defaults.set("arena.pos1.z", 0);
        defaults.setComments("arena.pos1.x", List.of(
                "",
                "=== ARENA ===",
                "The arena is a cuboid defined by two corner positions (like WorldEdit).",
                "Set these using: /cc function setdoommodepos1 and /cc function setdoommodepos2",
                "Stand at a corner and run the command — your current position is saved.",
                "BOTH positions must be set before the mode can start."));
        defaults.set("arena.pos2.x", 0);
        defaults.set("arena.pos2.y", 0);
        defaults.set("arena.pos2.z", 0);
        defaults.set("arena.world", "");
        defaults.setComments("arena.world", List.of(
                "World the arena is in. Automatically set when you use the pos commands."));
        defaults.set("arena.enforce-boundary", true);
        defaults.setComments("arena.enforce-boundary", List.of(
                "Whether to enforce arena boundaries. If true, players cannot leave the arena.",
                "Players who try to leave are pushed back inward."));
        defaults.set("arena.boundary-push-strength", 0.5);
        defaults.setComments("arena.boundary-push-strength", List.of(
                "How hard players are pushed back when hitting the arena boundary.",
                "0.3 = gentle nudge, 0.5 = firm push, 1.0 = violent shove."));

        // ── Lava Rise ────────────────────────────────────────────────────
        defaults.set("lava-rise.enabled", true);
        defaults.setComments("lava-rise.enabled", List.of(
                "",
                "=== LAVA RISE ===",
                "The core Doom Mode mechanic. Real lava blocks fill the arena from below.",
                "Uses batched block placement (setType with physics disabled) for zero lag.",
                "Only AIR blocks are replaced — existing structures and terrain are preserved."));
        defaults.set("lava-rise.start-y", 0);
        defaults.setComments("lava-rise.start-y", List.of(
                "The Y coordinate where lava starts filling from.",
                "Set this to the floor level of your arena."));
        defaults.set("lava-rise.max-y", 100);
        defaults.setComments("lava-rise.max-y", List.of(
                "The Y coordinate where lava stops rising.",
                "Set this to the ceiling of your arena or slightly below."));
        defaults.set("lava-rise.blocks-per-level-tick", 500);
        defaults.setComments("lava-rise.blocks-per-level-tick", List.of(
                "How many blocks are processed per server tick when filling a lava layer.",
                "Higher = faster fill but more work per tick. 500 = a 100x100 arena fills",
                "one Y level in 20 ticks (1 second). Increase for larger arenas.",
                "This is the key performance setting — keep it at 500-1000 for smooth performance."));
        defaults.set("lava-rise.rise-interval-ticks", 200);
        defaults.setComments("lava-rise.rise-interval-ticks", List.of(
                "Ticks between each lava rise. 20 ticks = 1 second.",
                "200 = lava rises every 10 seconds. Lower = more intense.",
                "Recommended: 100 (5 sec, aggressive) to 400 (20 sec, relaxed)."));
        defaults.set("lava-rise.rise-amount", 1);
        defaults.setComments("lava-rise.rise-amount", List.of(
                "How many Y levels the lava rises each interval.",
                "1 = one block layer per interval. 2 = two layers (faster rise)."));
        defaults.set("lava-rise.cleanup-on-end", true);
        defaults.setComments("lava-rise.cleanup-on-end", List.of(
                "Whether to remove all lava blocks when the mode ends.",
                "Uses the same batched approach for lag-free cleanup.",
                "Set to false if you want to manually reset the arena (e.g. with WorldEdit)."));

        // ── Lava Damage ──────────────────────────────────────────────────
        defaults.set("lava-damage.enabled", true);
        defaults.setComments("lava-damage.enabled", List.of(
                "",
                "=== LAVA DAMAGE ===",
                "Override vanilla lava damage with a configurable amount.",
                "Vanilla lava deals 4 hearts/sec — this lets you tune it for your server."));
        defaults.set("lava-damage.damage-per-tick", 2.0);
        defaults.setComments("lava-damage.damage-per-tick", List.of(
                "Damage dealt per damage application in half-hearts.",
                "2.0 = 1 heart per application. 4.0 = 2 hearts. 10.0 = 5 hearts."));
        defaults.set("lava-damage.damage-interval-ticks", 20);
        defaults.setComments("lava-damage.damage-interval-ticks", List.of(
                "Ticks between lava damage applications. 20 = once per second.",
                "10 = twice per second (aggressive). 40 = every 2 seconds (lenient)."));

        // ── Spawning ─────────────────────────────────────────────────────
        defaults.set("spawn.base-interval-ticks", 50);
        defaults.setComments("spawn.base-interval-ticks", List.of(
                "",
                "=== ATTACK SPAWNING ===",
                "Ticks between attack spawn attempts. 20 ticks = 1 second.",
                "50 = attempt every 2.5 seconds."));
        defaults.set("spawn.max-events-per-player", 5);
        defaults.setComments("spawn.max-events-per-player", List.of(
                "Maximum simultaneous active attacks per player. Recommended: 3-8."));
        defaults.set("spawn.offset-radius", 10.0);
        defaults.setComments("spawn.offset-radius", List.of(
                "Max distance from the player that attacks can spawn."));
        defaults.set("spawn.type-weights.block-display", 0.45);
        defaults.setComments("spawn.type-weights.block-display", List.of(
                "Spawn weight for block display attacks. Higher = more likely.",
                "All three weights are relative — they don't need to sum to 1.0."));
        defaults.set("spawn.type-weights.environmental", 0.35);
        defaults.setComments("spawn.type-weights.environmental", List.of(
                "Spawn weight for environmental effect attacks."));
        defaults.set("spawn.type-weights.model-engine", 0.20);
        defaults.setComments("spawn.type-weights.model-engine", List.of(
                "Spawn weight for ModelEngine animated VFX attacks.",
                "Requires ModelEngine 4 plugin. Set to 0 to disable ME attacks."));

        // ── Timer HUD ────────────────────────────────────────────────────
        defaults.set("timer-hud.display-name", "DOOM");
        defaults.setComments("timer-hud.display-name", List.of(
                "Mode name displayed on the BetterHud timer bar."));
        defaults.set("timer-hud.color", "red");
        defaults.setComments("timer-hud.color", List.of(
                "Color of the mode name text (normal state)."));
        defaults.set("timer-hud.flash-color", "dark_red");
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
        defaults.set("music.duration-ticks", 6000);

        // ── Lifecycle Commands ───────────────────────────────────────────
        defaults.set("on-start-commands", new ArrayList<>());
        defaults.setComments("on-start-commands", List.of(
                "Console commands run when Doom Mode starts. Use %player% for the starting player."));
        defaults.set("on-end-commands", new ArrayList<>());
        defaults.set("on-player-ready-commands", new ArrayList<>());
        defaults.setComments("on-player-ready-commands", List.of(
                "Commands run for each player when they exit title screen or change world during this mode.",
                "Supports wait <ticks>, done, and PlaceholderAPI. Use %player% for the player's name."));
        defaults.set("on-reset-commands", new ArrayList<>());
        defaults.setComments("on-reset-commands", List.of(
                "Per-mode reset commands. Available for manual use or future expansion."));
        defaults.set("exempt-players", new ArrayList<>());
        defaults.setComments("exempt-players", List.of(
                "Player names that receive ZERO damage from all attacks and lava."));
        defaults.set("rewards.commands", new ArrayList<>());
        defaults.setComments("rewards.commands", List.of(
                "Commands run for each surviving player. Use %player%."));

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

        // ── Universal Mob Spawning ───────────────────────────────────────
        MobSpawnConfig.writeDefaults(defaults, List.of(
                new MobSpawnConfig.MobSpawnDefaultEntry("MAGMA_CUBE", "vanilla", 8, 1, 3, 1.5, 1.0),
                new MobSpawnConfig.MobSpawnDefaultEntry("BLAZE", "vanilla", 6, 1, 2, 1.2, 1.0),
                new MobSpawnConfig.MobSpawnDefaultEntry("WITHER_SKELETON", "vanilla", 4, 1, 1, 2.0, 1.5)
        ));

        try {
            defaults.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create default doom config: " + e.getMessage());
        }
    }
}
