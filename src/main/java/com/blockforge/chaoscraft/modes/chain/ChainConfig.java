package com.blockforge.chaoscraft.modes.chain;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

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

        // Ensure chain-specific keys exist (ModeConfig may have created the file first)
        boolean needsSave = false;
        if (!config.contains("world")) { config.set("world", ""); needsSave = true; }
        if (!config.contains("spawn.base-interval-ticks")) { config.set("spawn.base-interval-ticks", 50); needsSave = true; }
        if (!config.contains("spawn.max-events-per-player")) { config.set("spawn.max-events-per-player", 5); needsSave = true; }
        if (!config.contains("spawn.offset-radius")) { config.set("spawn.offset-radius", 10.0); needsSave = true; }
        if (!config.contains("timer-hud.display-name")) { config.set("timer-hud.display-name", "CHAIN MODE"); needsSave = true; }
        if (!config.contains("timer-hud.color")) { config.set("timer-hud.color", "gray"); needsSave = true; }
        if (!config.contains("timer-hud.flash-color")) { config.set("timer-hud.flash-color", "red"); needsSave = true; }
        if (!config.contains("timer-hud.flash-threshold-seconds")) { config.set("timer-hud.flash-threshold-seconds", 60); needsSave = true; }
        if (needsSave) {
            save();
            config = YamlConfiguration.loadConfiguration(configFile);
        }
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
    // Default config creation
    // ========================

    private void createDefaults() {
        FileConfiguration defaults = new YamlConfiguration();

        // World (empty = first loaded world / Overworld)
        defaults.set("world", "");

        // Timer
        defaults.set("timer.default-seconds", 600);
        defaults.set("timer.max-seconds", 1800);

        // Spawning
        defaults.set("spawn.base-interval-ticks", 50);
        defaults.set("spawn.max-events-per-player", 5);
        defaults.set("spawn.offset-radius", 10.0);

        // Music
        defaults.set("music.sound-id", "");
        defaults.set("music.loop", true);
        defaults.set("music.duration-ticks", 6000);

        // Lifecycle
        defaults.set("on-start-commands", new ArrayList<>());
        defaults.set("on-end-commands", new ArrayList<>());
        defaults.set("exempt-players", new ArrayList<>());
        defaults.set("rewards.commands", new ArrayList<>());

        try {
            defaults.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create default chain config: " + e.getMessage());
        }
    }
}
