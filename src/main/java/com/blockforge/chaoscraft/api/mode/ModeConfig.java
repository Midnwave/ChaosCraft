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

    public List<String> getOnPlayerReadyCommands() {
        return config.getStringList("on-player-ready-commands");
    }

    public List<String> getOnResetCommands() {
        return config.getStringList("on-reset-commands");
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
                "Use %player% for the player who triggered the start, or omit for global effects.",
                "Example:",
                "  - \"broadcast &aThe mode has started!\"",
                "  - \"give %player% golden_apple 1\""));
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

        // ── Rewards ────────────────────────────────────────────────────────────
        defaults.set("rewards.commands", new ArrayList<>());
        defaults.setComments("rewards.commands", List.of(
                "Commands run for each surviving player when the mode ends successfully.",
                "Use %player% as a placeholder for each player's name.",
                "Example:",
                "  - \"give %player% diamond 5\"",
                "  - \"eco give %player% 1000\""));

        try {
            defaults.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create default config for mode: " + modeName);
        }
    }
}
