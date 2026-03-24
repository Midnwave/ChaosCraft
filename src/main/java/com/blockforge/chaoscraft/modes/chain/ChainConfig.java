package com.blockforge.chaoscraft.modes.chain;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.blockforge.chaoscraft.services.mobspawn.MobSpawnConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Chain Mode configuration loader.
 * Reads from plugins/ChaosCraft/modes/chain/chain.yml
 *
 * Chain Mode is a pure survival timer mode — no bosses, no phases,
 * just survive the chain attacks for X minutes to win.
 */
public class ChainConfig {

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

        // Ensure ALL keys exist — both base mode keys and chain-specific keys.
        // ModeConfig may have created the file with only generic keys, or the file
        // may have been manually edited and is missing some sections.
        boolean needsSave = false;

        // ── Base mode keys (same as ModeConfig) ──────────────────────────────
        if (!config.contains("config-version")) { config.set("config-version", 2); needsSave = true; }
        if (config.getInt("config-version") < 2) { config.set("config-version", 2); needsSave = true; }
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

        // ── Chain-specific keys ──────────────────────────────────────────────
        if (!config.contains("world")) { config.set("world", ""); needsSave = true; }
        if (!config.contains("spawn.base-interval-ticks")) { config.set("spawn.base-interval-ticks", 50); needsSave = true; }
        if (!config.contains("spawn.max-events-per-player")) { config.set("spawn.max-events-per-player", 5); needsSave = true; }
        if (!config.contains("spawn.offset-radius")) { config.set("spawn.offset-radius", 10.0); needsSave = true; }

        // ── Mob (Citizens NPC) keys ──────────────────────────────────────────
        if (!config.contains("mobs.enabled")) { config.set("mobs.enabled", true); needsSave = true; }
        if (!config.contains("mobs.display-name")) { config.set("mobs.display-name", "&7Chain Walker"); needsSave = true; }
        if (!config.contains("mobs.skin-player-name")) { config.set("mobs.skin-player-name", ""); needsSave = true; }
        if (!config.contains("mobs.skin-url")) { config.set("mobs.skin-url", ""); needsSave = true; }
        if (!config.contains("mobs.skin-file")) { config.set("mobs.skin-file", ""); needsSave = true; }
        if (!config.contains("mobs.health")) { config.set("mobs.health", 40.0); needsSave = true; }
        if (!config.contains("mobs.damage")) { config.set("mobs.damage", 6.0); needsSave = true; }
        if (!config.contains("mobs.speed")) { config.set("mobs.speed", 0.28); needsSave = true; }
        if (!config.contains("mobs.detection-range")) { config.set("mobs.detection-range", 32.0); needsSave = true; }
        if (!config.contains("mobs.attack-range")) { config.set("mobs.attack-range", 6.0); needsSave = true; }
        if (!config.contains("mobs.spawn-distance")) { config.set("mobs.spawn-distance", 20.0); needsSave = true; }
        if (!config.contains("mobs.spawn-interval-ticks")) { config.set("mobs.spawn-interval-ticks", 200); needsSave = true; }
        if (!config.contains("mobs.skill-cooldown-ticks")) { config.set("mobs.skill-cooldown-ticks", 60); needsSave = true; }
        if (!config.contains("mobs.max-per-player")) { config.set("mobs.max-per-player", 3); needsSave = true; }
        if (!config.contains("mobs.max-total")) { config.set("mobs.max-total", 15); needsSave = true; }
        if (!config.contains("timer-hud.display-name")) { config.set("timer-hud.display-name", "CHAIN MODE"); needsSave = true; }
        if (!config.contains("timer-hud.color")) { config.set("timer-hud.color", "gray"); needsSave = true; }
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

    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save chain config: " + e.getMessage());
        }
    }

    public FileConfiguration get() {
        return config;
    }

    // ========================
    // World
    // ========================

    /** The world name where Chain Mode runs (default: first loaded world / Overworld). */
    public String getWorldName() {
        return config.getString("world", "");
    }

    // ========================
    // Timer
    // ========================

    public long getDefaultTimerSeconds() {
        return config.getLong("timer.default-seconds", 600);
    }

    public long getMaxTimerSeconds() {
        return config.getLong("timer.max-seconds", 1800);
    }

    // ========================
    // Spawning
    // ========================

    /** Base ticks between automatic attack spawn attempts. */
    public int getBaseSpawnInterval() {
        return config.getInt("spawn.base-interval-ticks", 50);
    }

    /** Max simultaneous active attacks per player. */
    public int getMaxEventsPerPlayer() {
        return config.getInt("spawn.max-events-per-player", 5);
    }

    /** Spawn offset radius — how far from the player attacks can spawn. */
    public double getSpawnOffsetRadius() {
        return config.getDouble("spawn.offset-radius", 10.0);
    }

    // ========================
    // Music
    // ========================

    public String getMusicSoundId() {
        return config.getString("music.sound-id", "");
    }

    public boolean isMusicLooped() {
        return config.getBoolean("music.loop", true);
    }

    public long getMusicDurationTicks() {
        return config.getLong("music.duration-ticks", 6000);
    }

    // ========================
    // Lifecycle
    // ========================

    public List<String> getOnStartCommands() {
        return config.getStringList("on-start-commands");
    }

    public List<String> getOnEndCommands() {
        return config.getStringList("on-end-commands");
    }

    public List<String> getExemptPlayers() {
        return config.getStringList("exempt-players");
    }

    public List<String> getRewardCommands() {
        return config.getStringList("rewards.commands");
    }

    // ========================
    // Mobs (Citizens NPCs)
    // ========================

    public boolean areMobsEnabled() { return config.getBoolean("mobs.enabled", true); }
    public String getMobDisplayName() { return config.getString("mobs.display-name", "&7Chain Walker"); }
    public String getMobSkinPlayerName() { return config.getString("mobs.skin-player-name", ""); }
    public String getMobSkinUrl() { return config.getString("mobs.skin-url", ""); }
    public String getMobSkinFile() { return config.getString("mobs.skin-file", ""); }
    public double getMobHealth() { return config.getDouble("mobs.health", 40.0); }
    public double getMobDamage() { return config.getDouble("mobs.damage", 6.0); }
    public double getMobSpeed() { return config.getDouble("mobs.speed", 0.28); }
    public double getMobDetectionRange() { return config.getDouble("mobs.detection-range", 32.0); }
    public double getMobAttackRange() { return config.getDouble("mobs.attack-range", 6.0); }
    public double getMobSpawnDistance() { return config.getDouble("mobs.spawn-distance", 20.0); }
    public int getMobSpawnIntervalTicks() { return config.getInt("mobs.spawn-interval-ticks", 200); }
    public int getMobSkillCooldownTicks() { return config.getInt("mobs.skill-cooldown-ticks", 60); }
    public int getMobMaxPerPlayer() { return config.getInt("mobs.max-per-player", 3); }
    public int getMobMaxTotal() { return config.getInt("mobs.max-total", 15); }

    // ========================
    // Default config creation
    // ========================

    private void createDefaults() {
        FileConfiguration defaults = new YamlConfiguration();

        // ── World ──────────────────────────────────────────────────────────────
        defaults.set("world", "");
        defaults.setComments("world", List.of(
                "World name where Chain Mode runs its attacks and timer.",
                "Leave empty (\"\") to use the first loaded world (usually the Overworld).",
                "Example: \"world\", \"survival\", \"skyblock_world\""));

        // ── Timer ──────────────────────────────────────────────────────────────
        defaults.set("timer.default-seconds", 600);
        defaults.setComments("timer.default-seconds", List.of(
                "Default duration of Chain Mode in seconds when started without a time argument.",
                "600 = 10 min | 900 = 15 min | 1200 = 20 min."));
        defaults.set("timer.max-seconds", 1800);
        defaults.setComments("timer.max-seconds", List.of(
                "Maximum timer value (in seconds) allowed via: /cc modes chain start <seconds>",
                "Prevents excessively long sessions from being configured by command."));

        // ── Spawning ───────────────────────────────────────────────────────────
        defaults.set("spawn.base-interval-ticks", 50);
        defaults.setComments("spawn.base-interval-ticks", List.of(
                "Ticks between automatic attack spawn attempts per online player. 20 ticks = 1 second.",
                "Lower = more frequent spawning and higher intensity. 50 = attempt every 2.5 seconds.",
                "Each attempt may or may not spawn an attack depending on the max-events-per-player limit."));
        defaults.set("spawn.max-events-per-player", 5);
        defaults.setComments("spawn.max-events-per-player", List.of(
                "Maximum number of simultaneous active chain attacks targeting one player at once.",
                "Once this cap is reached, no new attacks spawn for that player until old ones expire.",
                "Higher = more chaos, more server load. Recommended: 3–8."));
        defaults.set("spawn.offset-radius", 10.0);
        defaults.setComments("spawn.offset-radius", List.of(
                "Maximum distance in blocks from the player that attacks can spawn.",
                "Attacks spawn at a random position within a circle of this radius around the player.",
                "Larger radius = attacks appear further away, giving more reaction time."));

        // ── Timer HUD (BetterHud) ──────────────────────────────────────────────
        defaults.set("timer-hud.display-name", "CHAIN MODE");
        defaults.setComments("timer-hud.display-name", List.of(
                "Text shown on the right side of the BetterHud mode timer bar.",
                "This is the mode label players see during the session (e.g., \"CHAIN MODE\")."));
        defaults.set("timer-hud.color", "gray");
        defaults.setComments("timer-hud.color", List.of(
                "Color of the mode name text in the BetterHud timer bar (normal state).",
                "Use MiniMessage/BetterHud color names: black, dark_blue, dark_green, dark_aqua,",
                "dark_red, dark_purple, gold, gray, dark_gray, blue, green, aqua, red, light_purple, yellow, white."));
        defaults.set("timer-hud.flash-color", "red");
        defaults.setComments("timer-hud.flash-color", List.of(
                "Color of the timer text when flashing during the low-time warning period.",
                "The text alternates between white and this color every 10 ticks when time is low."));
        defaults.set("timer-hud.flash-threshold-seconds", 60);
        defaults.setComments("timer-hud.flash-threshold-seconds", List.of(
                "Seconds remaining at which the timer text starts flashing to warn players.",
                "Default 60 = start flashing with 1 minute left. Set to 0 to disable flashing."));

        // ── Music ──────────────────────────────────────────────────────────────
        defaults.set("music.sound-id", "");
        defaults.setComments("music.sound-id", List.of(
                "Namespaced sound ID to play as background music during Chain Mode.",
                "Example: chaoscraft:music.chain_theme   |   Leave empty (\"\") to disable."));
        defaults.set("music.loop", true);
        defaults.setComments("music.loop", List.of(
                "Whether the background music track loops continuously.",
                "Set to false for a one-shot track that plays once then stops."));
        defaults.set("music.duration-ticks", 6000);
        defaults.setComments("music.duration-ticks", List.of(
                "Duration of one music loop in ticks before it replays. 6000 = 5 minutes.",
                "Set this to match your actual audio file length to avoid gaps or early replays."));

        // ── Lifecycle Commands ─────────────────────────────────────────────────
        defaults.set("on-start-commands", new ArrayList<>());
        defaults.setComments("on-start-commands", List.of(
                "Console commands run when Chain Mode starts. Use %player% for the starting player.",
                "Example:",
                "  - \"broadcast &6Chain Mode has started — survive the chains!\""));
        defaults.set("on-end-commands", new ArrayList<>());
        defaults.setComments("on-end-commands", List.of(
                "Console commands run when Chain Mode ends (timer expires or /cc modes stop)."));
        defaults.set("on-player-ready-commands", new ArrayList<>());
        defaults.setComments("on-player-ready-commands", List.of(
                "Commands run for each player when they exit title screen or change world during this mode.",
                "Supports wait <ticks>, done, and PlaceholderAPI. Use %player% for the player's name."));
        defaults.set("on-reset-commands", new ArrayList<>());
        defaults.setComments("on-reset-commands", List.of(
                "Per-mode reset commands. Available for manual use or future expansion."));
        defaults.set("exempt-players", new ArrayList<>());
        defaults.setComments("exempt-players", List.of(
                "Player names that receive ZERO damage from all chain attacks.",
                "Staff/spectator bypass list. Example:",
                "  - \"StaffName\""));
        defaults.set("rewards.commands", new ArrayList<>());
        defaults.setComments("rewards.commands", List.of(
                "Commands run for each surviving player when the mode ends successfully.",
                "Use %player% as a placeholder. Example:",
                "  - \"give %player% diamond 3\""));

        // ── Mobs (Citizens NPCs) ────────────────────────────────────────────
        defaults.set("mobs.enabled", true);
        defaults.setComments("mobs.enabled", List.of(
                "Whether chain walker NPCs spawn during Chain Mode. Requires Citizens plugin.",
                "Set to false to run Chain Mode with only block display attacks (no mobs)."));
        defaults.set("mobs.display-name", "&7Chain Walker");
        defaults.setComments("mobs.display-name", List.of(
                "Display name shown above each chain mob NPC. Supports & color codes."));
        defaults.set("mobs.skin-player-name", "");
        defaults.setComments("mobs.skin-player-name", List.of(
                "Player name whose skin the NPCs will use. Leave empty for default Steve skin.",
                "Example: \"Notch\" — uses Notch's skin from Mojang.",
                "Ignored if skin-url is set (skin-url takes priority)."));
        defaults.set("mobs.skin-url", "");
        defaults.setComments("mobs.skin-url", List.of(
                "HTTP/HTTPS URL to a skin PNG image. Citizens sends this to mineskin.org for processing.",
                "Must be a publicly accessible URL (not a local file path).",
                "Example: \"https://example.com/skins/chain_walker.png\"",
                "You can upload your skin PNG to imgur, GitHub, or any image host.",
                "Takes priority over skin-player-name. Leave empty to use skin-player-name instead."));
        defaults.set("mobs.skin-file", "");
        defaults.setComments("mobs.skin-file", List.of(
                "(DEPRECATED) Local PNG files cannot be used — Citizens requires a public URL.",
                "Use skin-url with a publicly accessible HTTP link instead."));
        defaults.set("mobs.health", 40.0);
        defaults.setComments("mobs.health", List.of(
                "Maximum health of each chain mob NPC (in half-hearts). 40 = 20 hearts."));
        defaults.set("mobs.damage", 6.0);
        defaults.setComments("mobs.damage", List.of(
                "Base damage dealt by chain mob skills (in half-hearts). 6 = 3 hearts.",
                "Different skills multiply this: hook (1x), lash (0.8x), slam (1.2x), snare (0.4x/tick)."));
        defaults.set("mobs.speed", 0.28);
        defaults.setComments("mobs.speed", List.of(
                "Movement speed of chain mobs. Default player speed is 0.2.",
                "0.28 = slightly faster than players. 0.35 = noticeably fast."));
        defaults.set("mobs.detection-range", 32.0);
        defaults.setComments("mobs.detection-range", List.of(
                "Maximum distance (blocks) at which a chain mob can detect and target a player."));
        defaults.set("mobs.attack-range", 6.0);
        defaults.setComments("mobs.attack-range", List.of(
                "Distance (blocks) within which a chain mob will execute its chain skills.",
                "The mob navigates toward the player until within this range, then attacks."));
        defaults.set("mobs.spawn-distance", 20.0);
        defaults.setComments("mobs.spawn-distance", List.of(
                "How far (blocks) from the target player a new chain mob spawns.",
                "Higher = more warning time before the mob reaches the player."));
        defaults.set("mobs.spawn-interval-ticks", 200);
        defaults.setComments("mobs.spawn-interval-ticks", List.of(
                "Ticks between spawn attempts. 200 = every 10 seconds. 20 ticks = 1 second.",
                "Lower = more frequent spawning (more intense). Each attempt checks mob caps first."));
        defaults.set("mobs.skill-cooldown-ticks", 60);
        defaults.setComments("mobs.skill-cooldown-ticks", List.of(
                "Ticks a chain mob must wait between skill uses. 60 = 3 seconds.",
                "Lower = more aggressive attacks. Higher = mobs spend more time chasing."));
        defaults.set("mobs.max-per-player", 3);
        defaults.setComments("mobs.max-per-player", List.of(
                "Maximum chain mobs that can be near (within 30 blocks of) one player at once."));
        defaults.set("mobs.max-total", 15);
        defaults.setComments("mobs.max-total", List.of(
                "Maximum total chain mobs alive at once across the entire mode.",
                "Prevents server overload. Recommended: 10–20 depending on player count."));

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
        // Chain Mode already has Citizens NPC mobs above. This section is for
        // additional MythicMobs or vanilla mob waves alongside the chain walkers.
        MobSpawnConfig.writeDefaults(defaults, List.of());

        try {
            defaults.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create default chain config: " + e.getMessage());
        }
    }
}
