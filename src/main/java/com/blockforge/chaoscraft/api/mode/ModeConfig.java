package com.blockforge.chaoscraft.api.mode;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads and manages a per-mode YAML config file from plugins/ChaosCraft/modes/{modeName}/
 */
public class ModeConfig {

    /**
     * Bump when adding new config keys or changing defaults.
     * Files with an older version are re-saved with new keys while preserving user edits.
     */
    public static final int CURRENT_CONFIG_VERSION = 2;

    private final ChaosCraftPlugin plugin;
    private final String modeName;
    private final File configFile;
    private FileConfiguration config;

    public ModeConfig(ChaosCraftPlugin plugin, String modeName) {
        this.plugin = plugin;
        this.modeName = modeName;

        File modesDir = new File(plugin.getDataFolder(), "modes/" + modeName);
        if (!modesDir.exists()) {
            modesDir.mkdirs();
        }
        this.configFile = new File(modesDir, modeName + ".yml");
        load();
    }

    public void load() {
        if (!configFile.exists()) {
            createDefaults();
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // Config version check — re-save with new keys if outdated
        int fileVersion = config.getInt("config-version", 0);
        if (fileVersion < CURRENT_CONFIG_VERSION) {
            plugin.getLogger().info("[" + modeName + "] Upgrading config from v" + fileVersion + " to v" + CURRENT_CONFIG_VERSION);
            config.set("config-version", CURRENT_CONFIG_VERSION);
            // Re-save preserves user values, adds any new keys from createDefaults logic
            save();
        }
    }

    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save config for mode: " + modeName);
            e.printStackTrace();
        }
    }

    public FileConfiguration get() {
        return config;
    }

    public File getModeFolder() {
        return configFile.getParentFile();
    }

    // ---- Common config accessors ----

    public long getDefaultTimerSeconds() {
        return config.getLong("timer.default-seconds", 1200); // 20 minutes default
    }

    public long getMaxTimerSeconds() {
        return config.getLong("timer.max-seconds", 1500); // 25 minutes soft cap
    }

    public String getMusic() {
        return config.getString("music.sound-id", "");
    }

    public boolean isMusicLooped() {
        return config.getBoolean("music.loop", true);
    }

    public long getMusicDurationTicks() {
        return config.getLong("music.duration-ticks", 6000);
    }

    public List<String> getOnStartCommands() {
        return config.getStringList("on-start-commands");
    }

    public List<String> getOnEndCommands() {
        return config.getStringList("on-end-commands");
    }

    /**
     * Returns on-player-ready-commands PLUS any on-start-commands flagged with --includeplayerready.
     * Flags are stripped from the returned command strings.
     */
    public List<String> getOnPlayerReadyCommands() {
        List<String> result = new ArrayList<>(config.getStringList("on-player-ready-commands"));
        for (String cmd : config.getStringList("on-start-commands")) {
            if (cmd.contains("--includeplayerready")) {
                result.add(stripFlags(cmd));
            }
        }
        return result;
    }

    /**
     * Returns on-reset-commands PLUS any on-start-commands flagged with --includereset.
     * Flags are stripped from the returned command strings.
     */
    public List<String> getOnResetCommands() {
        List<String> result = new ArrayList<>(config.getStringList("on-reset-commands"));
        for (String cmd : config.getStringList("on-start-commands")) {
            if (cmd.contains("--includereset")) {
                result.add(stripFlags(cmd));
            }
        }
        return result;
    }

    /** Strip --includeplayerready and --includereset flags from a command string. */
    private static String stripFlags(String cmd) {
        return cmd.replace("--includeplayerready", "").replace("--includereset", "").trim()
                .replaceAll("\\s+", " "); // Collapse double spaces
    }

    public List<String> getExemptPlayers() {
        return config.getStringList("exempt-players");
    }

    public int getMaxEventsPerPlayer() {
        return config.getInt("max-events-per-player", 5);
    }

    public List<String> getRewardCommands() {
        return config.getStringList("rewards.commands");
    }

    // ── Player Restriction Getters ────────────────────────────────────────────

    /** Whether players can change worlds during this mode. Default false. */
    public boolean isWorldChangeAllowed() {
        return config.getBoolean("restrictions.allow-world-change", false);
    }

    /** Whether water/lakes are replaced with light blue glass to prevent AI abuse. Default false. */
    public boolean isWaterToGlassEnabled() {
        return config.getBoolean("restrictions.water-to-glass", false);
    }

    /** Whether players respawn normally on death. If false, they go to spectator mode. Default true. */
    public boolean isRespawnAllowed() {
        return config.getBoolean("restrictions.allow-respawn", true);
    }

    /** Whether players can use elytra during this mode. Default false. */
    public boolean isElytraAllowed() {
        return config.getBoolean("restrictions.allow-elytra", false);
    }

    /** List of blocked command aliases (e.g. "home", "tpa", "spawn"). */
    public List<String> getBlockedCommands() {
        return config.getStringList("restrictions.blocked-commands");
    }

    private void createDefaults() {
        FileConfiguration defaults = new YamlConfiguration();

        defaults.set("config-version", CURRENT_CONFIG_VERSION);
        defaults.setComments("config-version", List.of(
                "Internal version number — do NOT edit this manually.",
                "The plugin bumps this when new config keys are added and will auto-upgrade your file."));

        // ── Timer ──────────────────────────────────────────────────────────────
        defaults.set("timer.default-seconds", 1200);
        defaults.setComments("timer.default-seconds", List.of(
                "Default duration of the mode in seconds when started without a time argument.",
                "1200 = 20 min | 900 = 15 min | 600 = 10 min | 1800 = 30 min."));
        defaults.set("timer.max-seconds", 1500);
        defaults.setComments("timer.max-seconds", List.of(
                "Maximum timer value (seconds) allowed when using: /cc modes <mode> start <seconds>",
                "Prevents staff from accidentally starting an excessively long session."));

        // ── Music ──────────────────────────────────────────────────────────────
        defaults.set("music.sound-id", "");
        defaults.setComments("music.sound-id", List.of(
                "Namespaced sound ID to play as background music when this mode starts.",
                "Example: chaoscraft:music.chain_theme   |   Leave empty (\"\") to disable music."));
        defaults.set("music.loop", true);
        defaults.setComments("music.loop", List.of(
                "Whether the background music track loops continuously throughout the mode.",
                "Set to false for one-shot tracks that play once then stop."));
        defaults.set("music.duration-ticks", 6000);
        defaults.setComments("music.duration-ticks", List.of(
                "Duration of one music loop in ticks. The plugin replays the track after this many ticks.",
                "6000 = 5 minutes. Set this to match your actual audio file length."));

        // ── Lifecycle Commands ─────────────────────────────────────────────────
        defaults.set("on-start-commands", new ArrayList<>());
        defaults.setComments("on-start-commands", List.of(
                "Console commands run automatically when this mode starts.",
                "Supports scripting: 'wait <ticks>' to pause, 'done' to start attacks/spawning.",
                "Supports PlaceholderAPI: %chaoscraft_join_ticks%, %player%, etc.",
                "",
                "FLAGS (append to any command):",
                "  --includeplayerready  Also run this command when a player exits title screen or changes world",
                "  --includereset        Also run this command as a reset when a player joins with no active mode",
                "",
                "Example:",
                "  - \"playsound minecraft:chaoscraft.chain master %player% --includeplayerready\"",
                "  - \"wait %chaoscraft_join_ticks%\"",
                "  - \"betterhud hud a all cc_timer\"",
                "  - \"wait 20\"",
                "  - \"betterhud hud r all cc_timer\"",
                "  - \"betterhud hud a all cc_timer_static\"",
                "  - \"cc function startmodetimer 2:30 0:30 1\"",
                "  - \"done\""));
        defaults.set("on-end-commands", new ArrayList<>());
        defaults.setComments("on-end-commands", List.of(
                "Console commands run when this mode ends (naturally or via /cc modes stop).",
                "Same %player% placeholder support as on-start-commands."));

        defaults.set("on-player-ready-commands", new ArrayList<>());
        defaults.setComments("on-player-ready-commands", List.of(
                "Commands run for each player when they become ready during an active mode.",
                "Triggers: exiting title screen (via /cc function verifyexittitlescreen),",
                "          changing world/dimension during an active mode.",
                "Fires EVERY time the trigger occurs (not just once per session).",
                "Supports wait <ticks>, done, and PlaceholderAPI placeholders.",
                "Use %player% for the player's name.",
                "Example:",
                "  - \"playsound minecraft:chaoscraft.chain master %player%\"",
                "  - \"title %player% subtitle {\\\"text\\\":\\\"Mode Active!\\\"}\""));

        defaults.set("on-reset-commands", new ArrayList<>());
        defaults.setComments("on-reset-commands", List.of(
                "Per-mode reset commands. Available for manual use or future expansion.",
                "Not auto-triggered — use global-reset-commands in config.yml for join resets.",
                "Use %player% for the player's name."));

        // ── Exempt Players ─────────────────────────────────────────────────────
        defaults.set("exempt-players", new ArrayList<>());
        defaults.setComments("exempt-players", List.of(
                "Player names listed here receive ZERO damage from all mode attacks.",
                "Useful for staff members or spectators who need to observe without being targeted.",
                "Example:",
                "  - \"Notch\"",
                "  - \"jeb_\""));

        defaults.set("max-events-per-player", 5);
        defaults.setComments("max-events-per-player", List.of(
                "Maximum number of simultaneous active attacks that can target one player at once.",
                "Higher = more chaotic but also higher server load. Recommended range: 3–8."));

        // ── Player Restrictions ──────────────────────────────────────────────────
        defaults.set("restrictions.allow-world-change", false);
        defaults.setComments("restrictions.allow-world-change", List.of(
                "Whether players can change worlds/dimensions during this mode.",
                "If false, world change events are cancelled (player stays in mode world)."));
        defaults.set("restrictions.water-to-glass", false);
        defaults.setComments("restrictions.water-to-glass", List.of(
                "If true, water and lakes in the mode world are visually replaced with",
                "light blue stained glass to prevent AI abuse (mobs can't swim/drown).",
                "Blocks are restored when the mode ends."));
        defaults.set("restrictions.allow-respawn", true);
        defaults.setComments("restrictions.allow-respawn", List.of(
                "Whether players respawn normally on death during this mode.",
                "If false, dead players are put in spectator mode to watch others.",
                "They are restored to survival mode at a safe position when the mode ends."));
        defaults.set("restrictions.allow-elytra", false);
        defaults.setComments("restrictions.allow-elytra", List.of(
                "Whether players can use elytra during this mode.",
                "If false, elytra gliding is cancelled."));
        defaults.set("restrictions.blocked-commands", new ArrayList<>());
        defaults.setComments("restrictions.blocked-commands", List.of(
                "Command aliases that players cannot use during this mode.",
                "Blocks the command for non-exempt, non-op players.",
                "Example:",
                "  - \"home\"",
                "  - \"tpa\"",
                "  - \"spawn\"",
                "  - \"warp\""));

        // ── Rewards (tiered) ──────────────────────────────────────────────────
        // Survived rewards — given to players who stayed alive the entire mode
        defaults.set("rewards.survived.money", 0);
        defaults.setComments("rewards.survived.money", List.of(
                "Vault money given to players who survived the mode. 0 = no money reward."));
        defaults.set("rewards.survived.items", new ArrayList<>());
        defaults.setComments("rewards.survived.items", List.of(
                "Items given to survivors. Format: \"material_name quantity\"",
                "Example:",
                "  - \"diamond 3\"",
                "  - \"golden_apple 1\""));
        defaults.set("rewards.survived.badges", new ArrayList<>());
        defaults.setComments("rewards.survived.badges", List.of(
                "Badge IDs granted to survivors. Must match badge IDs in badges.yml.",
                "Example:",
                "  - \"survivor_chain\""));
        defaults.set("rewards.survived.commands", new ArrayList<>());
        defaults.setComments("rewards.survived.commands", List.of(
                "Console commands run for each survivor. Use %player%.",
                "Example:",
                "  - \"give %player% experience_bottle 5\""));

        // Died rewards — given to players who died during the mode
        defaults.set("rewards.died.money", 0);
        defaults.setComments("rewards.died.money", List.of(
                "Consolation money for players who died. 0 = nothing."));
        defaults.set("rewards.died.items", new ArrayList<>());
        defaults.set("rewards.died.badges", new ArrayList<>());
        defaults.set("rewards.died.commands", new ArrayList<>());

        // Bonus rewards — triggered by mode-specific events (e.g., tutorial completion)
        defaults.setComments("rewards.bonus", List.of(
                "Bonus rewards triggered by mode-specific events.",
                "Each key is a bonus ID that mode code can trigger via ModeResultsService.grantBonus().",
                "Example:",
                "  tutorial-complete:",
                "    money: 1000",
                "    badge: \"tutorial_master\"",
                "    commands:",
                "      - \"broadcast %player% completed the tutorial!\""));

        try {
            defaults.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create default config for mode: " + modeName);
        }
    }
}
