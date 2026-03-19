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
    public static final int CURRENT_CONFIG_VERSION = 1;

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
        defaults.set("timer.default-seconds", 1200);
        defaults.set("timer.max-seconds", 1500);
        defaults.set("music.sound-id", "");
        defaults.set("music.loop", true);
        defaults.set("music.duration-ticks", 6000);
        defaults.set("on-start-commands", new ArrayList<>());
        defaults.set("on-end-commands", new ArrayList<>());
        defaults.set("exempt-players", new ArrayList<>());
        defaults.set("max-events-per-player", 5);
        defaults.set("rewards.commands", new ArrayList<>());
        try {
            defaults.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create default config for mode: " + modeName);
        }
    }
}
