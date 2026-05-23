package com.blockforge.chaoscraft.modes.chain;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.services.mobspawn.MobSpawnConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Chain Mode configuration loader.
 *
 * Reads from {@code plugins/ChaosCraft/modes/chain/chain.yml}.
 * Foundation phase — no gimmick subsystems yet (chain-specific gimmicks
 * come in a later phase, see TODO marker below).
 *
 * Keeps: config-version, duration-seconds, arena-radius, enforce-boundary,
 * exempt-players, on-start / on-end commands, world, scheduler section,
 * mob-spawning section, ambient-sounds, music, rewards.
 */
public class ChainConfig {

    private static final int CURRENT_CONFIG_VERSION = 1;

    private final ChaosCraftPlugin plugin;
    private final File configFile;
    private FileConfiguration config;

    public ChainConfig(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        File modeDir = new File(plugin.getDataFolder(), "modes/chain");
        if (!modeDir.exists()) modeDir.mkdirs();
        this.configFile = new File(modeDir, "chain.yml");
        load();
    }

    public void load() {
        if (!configFile.exists()) {
            createDefaults();
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        boolean needsSave = false;

        // ── Top-level ───────────────────────────────────────────────────
        if (!config.contains("config-version")) { config.set("config-version", CURRENT_CONFIG_VERSION); needsSave = true; }
        if (config.getInt("config-version") < CURRENT_CONFIG_VERSION) { config.set("config-version", CURRENT_CONFIG_VERSION); needsSave = true; }
        if (!config.contains("duration-seconds")) { config.set("duration-seconds", 180); needsSave = true; }
        if (!config.contains("arena-radius")) { config.set("arena-radius", 50); needsSave = true; }
        if (!config.contains("enforce-boundary")) { config.set("enforce-boundary", true); needsSave = true; }
        if (!config.contains("exempt-players")) { config.set("exempt-players", new ArrayList<>()); needsSave = true; }
        if (!config.contains("on-start-commands")) { config.set("on-start-commands", new ArrayList<>()); needsSave = true; }
        if (!config.contains("on-end-commands")) { config.set("on-end-commands", new ArrayList<>()); needsSave = true; }
        if (!config.contains("world")) { config.set("world", ""); needsSave = true; }

        // ── Scheduler ───────────────────────────────────────────────────
        if (!config.contains("scheduler.base-spawn-interval-ticks")) { config.set("scheduler.base-spawn-interval-ticks", 25); needsSave = true; }
        if (!config.contains("scheduler.spawn-offset-radius")) { config.set("scheduler.spawn-offset-radius", 8.0); needsSave = true; }
        if (!config.contains("scheduler.max-events-per-player")) { config.set("scheduler.max-events-per-player", 5); needsSave = true; }
        if (!config.contains("scheduler.type-weight-block-display")) { config.set("scheduler.type-weight-block-display", 1.0); needsSave = true; }
        if (!config.contains("scheduler.type-weight-environmental")) { config.set("scheduler.type-weight-environmental", 1.0); needsSave = true; }
        if (!config.contains("scheduler.type-weight-model-engine")) { config.set("scheduler.type-weight-model-engine", 0.0); needsSave = true; }

        // ── Mob spawning (shared section) ───────────────────────────────
        if (MobSpawnConfig.ensureKeys(config)) needsSave = true;

        // ── Ambient sounds ──────────────────────────────────────────────
        if (!config.contains("ambient-sounds.enabled")) { config.set("ambient-sounds.enabled", true); needsSave = true; }
        if (!config.contains("ambient-sounds.min-interval-ticks")) { config.set("ambient-sounds.min-interval-ticks", 80); needsSave = true; }
        if (!config.contains("ambient-sounds.max-interval-ticks")) { config.set("ambient-sounds.max-interval-ticks", 200); needsSave = true; }
        if (!config.contains("ambient-sounds.volume")) { config.set("ambient-sounds.volume", 0.4); needsSave = true; }
        if (!config.contains("ambient-sounds.sounds")) {
            config.set("ambient-sounds.sounds", buildDefaultAmbientSounds());
            needsSave = true;
        }

        // ── Gimmick placeholder (chain-specific gimmicks TBD in later phase) ─
        //   Reserved keys (not yet read): gimmick.enabled, gimmick.<feature>.*
        //   Add the gimmick block here when the gimmick subsystems are implemented.

        // ── Music ───────────────────────────────────────────────────────
        if (!config.contains("music.track")) { config.set("music.track", "chain_main"); needsSave = true; }
        if (!config.contains("music.volume")) { config.set("music.volume", 0.6); needsSave = true; }

        // ── Rewards ─────────────────────────────────────────────────────
        if (!config.contains("rewards.survival-commands")) { config.set("rewards.survival-commands", new ArrayList<>()); needsSave = true; }

        if (needsSave) {
            save();
            config = YamlConfiguration.loadConfiguration(configFile);
        }
    }

    /** Chain-themed ambient sounds — chains clinking, iron scraping, distant rattling. */
    private List<String> buildDefaultAmbientSounds() {
        return Arrays.asList(
                "BLOCK_CHAIN_BREAK",
                "BLOCK_CHAIN_FALL",
                "BLOCK_CHAIN_HIT",
                "BLOCK_CHAIN_PLACE",
                "BLOCK_CHAIN_STEP",
                "BLOCK_ANVIL_LAND",
                "BLOCK_IRON_DOOR_OPEN",
                "BLOCK_IRON_DOOR_CLOSE");
    }

    /** Empty mob pool by default — chain ground spawner is opt-in. */
    private List<MobSpawnConfig.MobSpawnDefaultEntry> buildDefaultMobSpawnEntries() {
        return Collections.emptyList();
    }

    /** Returns a MobSpawnConfig backed by this mode's YAML. */
    public MobSpawnConfig getMobSpawnConfig() {
        return new MobSpawnConfig(config);
    }

    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save chain config: " + e.getMessage());
        }
    }

    public FileConfiguration get() { return config; }

    // ========================
    // Top-level
    // ========================

    public int getDurationSeconds() { return config.getInt("duration-seconds", 180); }
    public int getArenaRadius() { return config.getInt("arena-radius", 50); }
    public boolean isEnforceBoundary() { return config.getBoolean("enforce-boundary", true); }
    public List<String> getExemptPlayers() { return config.getStringList("exempt-players"); }
    public List<String> getOnStartCommands() { return config.getStringList("on-start-commands"); }
    public List<String> getOnEndCommands() { return config.getStringList("on-end-commands"); }
    public String getWorldName() { return config.getString("world", ""); }

    // ========================
    // Scheduler
    // ========================

    public int getBaseSpawnInterval() { return config.getInt("scheduler.base-spawn-interval-ticks", 25); }
    public double getSpawnOffsetRadius() { return config.getDouble("scheduler.spawn-offset-radius", 8.0); }
    public int getMaxEventsPerPlayer() { return config.getInt("scheduler.max-events-per-player", 5); }
    public double getTypeWeightBlockDisplay() { return config.getDouble("scheduler.type-weight-block-display", 1.0); }
    public double getTypeWeightEnvironmental() { return config.getDouble("scheduler.type-weight-environmental", 1.0); }
    public double getTypeWeightModelEngine() { return config.getDouble("scheduler.type-weight-model-engine", 0.0); }

    // ========================
    // Ambient sounds
    // ========================

    public boolean isAmbientSoundsEnabled() { return config.getBoolean("ambient-sounds.enabled", true); }
    public int getAmbientSoundMinInterval() { return config.getInt("ambient-sounds.min-interval-ticks", 80); }
    public int getAmbientSoundMaxInterval() { return config.getInt("ambient-sounds.max-interval-ticks", 200); }
    public double getAmbientSoundVolume() { return config.getDouble("ambient-sounds.volume", 0.4); }
    public List<String> getAmbientSounds() { return config.getStringList("ambient-sounds.sounds"); }

    // ========================
    // Music
    // ========================

    public String getMusicTrack() { return config.getString("music.track", "chain_main"); }
    public double getMusicVolume() { return config.getDouble("music.volume", 0.6); }

    // ========================
    // Rewards
    // ========================

    public List<String> getSurvivalCommands() { return config.getStringList("rewards.survival-commands"); }

    // ========================
    // Default config creation
    // ========================

    private void createDefaults() {
        FileConfiguration defaults = new YamlConfiguration();

        defaults.set("config-version", CURRENT_CONFIG_VERSION);
        defaults.setComments("config-version", List.of(
                "Chain Mode configuration. Do not edit config-version manually.",
                "The plugin auto-upgrades this file when new config keys are added."));

        defaults.set("duration-seconds", 180);
        defaults.setComments("duration-seconds", List.of(
                "Total mode duration in seconds. Default: 180 (3 minutes).",
                "Players who survive past this time receive survival-commands rewards."));

        defaults.set("arena-radius", 50);
        defaults.setComments("arena-radius", List.of(
                "Radius (in blocks) of the arena area used for spawn picking and",
                "boundary enforcement. Centered on the first online player at start."));

        defaults.set("enforce-boundary", true);
        defaults.setComments("enforce-boundary", List.of(
                "Whether to enforce arena boundaries by pushing players back inward.",
                "Set to false to let players roam freely."));

        defaults.set("exempt-players", new ArrayList<>());
        defaults.setComments("exempt-players", List.of(
                "Player names that receive ZERO damage from all Chain mode attacks.",
                "Add admins / spectators here while testing."));

        defaults.set("on-start-commands", new ArrayList<>());
        defaults.setComments("on-start-commands", List.of(
                "Console commands run when Chain Mode starts.",
                "Use %player% to substitute online player names if needed."));

        defaults.set("on-end-commands", new ArrayList<>());
        defaults.setComments("on-end-commands", List.of(
                "Console commands run when Chain Mode ends."));

        defaults.set("world", "");
        defaults.setComments("world", List.of(
                "World name where Chain Mode runs. Leave empty for the first loaded world."));

        // Scheduler
        defaults.set("scheduler.base-spawn-interval-ticks", 25);
        defaults.setComments("scheduler.base-spawn-interval-ticks", List.of(
                "",
                "=== ATTACK SCHEDULER ===",
                "Ticks between attack spawn attempts. 20 ticks = 1 second.",
                "25 = attempt every 1.25 seconds."));
        defaults.set("scheduler.spawn-offset-radius", 8.0);
        defaults.setComments("scheduler.spawn-offset-radius", List.of(
                "Max distance from the target player that non-tracking attacks can spawn."));
        defaults.set("scheduler.max-events-per-player", 5);
        defaults.setComments("scheduler.max-events-per-player", List.of(
                "Maximum simultaneous active attacks per player."));
        defaults.set("scheduler.type-weight-block-display", 1.0);
        defaults.setComments("scheduler.type-weight-block-display", List.of(
                "Spawn weight for block-display attacks. Higher = more likely.",
                "All weights are relative — they do not need to sum to 1.0."));
        defaults.set("scheduler.type-weight-environmental", 1.0);
        defaults.setComments("scheduler.type-weight-environmental", List.of(
                "Spawn weight for environmental attacks."));
        defaults.set("scheduler.type-weight-model-engine", 0.0);
        defaults.setComments("scheduler.type-weight-model-engine", List.of(
                "Spawn weight for ModelEngine VFX attacks.",
                "Defaults to 0.0 — no ME attacks are registered for Chain mode yet."));

        // Mob spawning section
        MobSpawnConfig.writeDefaults(defaults, buildDefaultMobSpawnEntries());

        // Ambient sounds
        defaults.set("ambient-sounds.enabled", true);
        defaults.setComments("ambient-sounds.enabled", List.of(
                "",
                "=== AMBIENT SOUNDS ===",
                "Random chain-themed sounds played at a random player at a random interval.",
                "Sounds are Bukkit Sound enum names (e.g. BLOCK_CHAIN_BREAK)."));
        defaults.set("ambient-sounds.min-interval-ticks", 80);
        defaults.setComments("ambient-sounds.min-interval-ticks", List.of(
                "Minimum ticks between ambient sound plays. 20 ticks = 1 second.",
                "80 = at least 4 seconds between sounds. Each play targets a random player."));
        defaults.set("ambient-sounds.max-interval-ticks", 200);
        defaults.setComments("ambient-sounds.max-interval-ticks", List.of(
                "Maximum ticks between ambient sound plays. 20 ticks = 1 second.",
                "200 = at most 10 seconds between sounds. The scheduler picks a random",
                "delay in [min, max] after each play."));
        defaults.set("ambient-sounds.volume", 0.4);
        defaults.setComments("ambient-sounds.volume", List.of(
                "Volume multiplier (0.0-1.0). Subtle by design — these are background",
                "atmosphere, not full-volume effects. 0.4 = 40% volume."));
        defaults.set("ambient-sounds.sounds", buildDefaultAmbientSounds());
        defaults.setComments("ambient-sounds.sounds", List.of(
                "Bukkit Sound enum names. Must be valid constants on the running server",
                "version — unknown names are skipped at runtime. Defaults are chain /",
                "iron / anvil themed (chain rattle, anvil clang, iron door, ...)."));

        // Music
        defaults.set("music.track", "chain_main");
        defaults.setComments("music.track", List.of(
                "Music track id (registered via the resource pack / MusicManager).",
                "Default 'chain_main' is a placeholder — provide a real track id later.",
                "volume is a multiplier applied at playback (0.0–1.0)."));
        defaults.set("music.volume", 0.6);
        defaults.setComments("music.volume", List.of(
                "Playback volume multiplier (0.0-1.0). 0.6 = 60% volume.",
                "Applied on top of each client's music slider."));

        // Rewards
        defaults.set("rewards.survival-commands", new ArrayList<>());
        defaults.setComments("rewards.survival-commands", List.of(
                "Console commands run for each player who survives the mode.",
                "Use %player% for the player's name."));

        try {
            defaults.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create default chain config: " + e.getMessage());
        }
    }
}
