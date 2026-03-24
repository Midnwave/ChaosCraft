package com.blockforge.chaoscraft.modes.tutorial;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tutorial Mode configuration loader.
 * Reads from plugins/ChaosCraft/modes/tutorial/tutorial.yml
 */
public class TutorialConfig {

    private static final int CURRENT_CONFIG_VERSION = 2;
    private final ChaosCraftPlugin plugin;
    private final File configFile;
    private FileConfiguration config;

    public TutorialConfig(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        File modeDir = new File(plugin.getDataFolder(), "modes/tutorial");
        if (!modeDir.exists()) modeDir.mkdirs();
        this.configFile = new File(modeDir, "tutorial.yml");
        load();
    }

    public void load() {
        if (!configFile.exists()) {
            createDefaults();
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        boolean needsSave = false;

        // ── Base mode keys ──
        if (!config.contains("config-version")) { config.set("config-version", CURRENT_CONFIG_VERSION); needsSave = true; }
        if (config.getInt("config-version") < CURRENT_CONFIG_VERSION) { config.set("config-version", CURRENT_CONFIG_VERSION); needsSave = true; }
        if (!config.contains("timer.default-seconds")) { config.set("timer.default-seconds", 210); needsSave = true; }
        if (!config.contains("timer.max-seconds")) { config.set("timer.max-seconds", 300); needsSave = true; }
        if (!config.contains("music.sound-id")) { config.set("music.sound-id", ""); needsSave = true; }
        if (!config.contains("music.loop")) { config.set("music.loop", false); needsSave = true; }
        if (!config.contains("music.duration-ticks")) { config.set("music.duration-ticks", 4200); needsSave = true; }
        if (!config.contains("on-start-commands")) { config.set("on-start-commands", new ArrayList<>()); needsSave = true; }
        if (!config.contains("on-end-commands")) { config.set("on-end-commands", new ArrayList<>()); needsSave = true; }
        if (!config.contains("on-player-ready-commands")) { config.set("on-player-ready-commands", new ArrayList<>()); needsSave = true; }
        if (!config.contains("on-reset-commands")) { config.set("on-reset-commands", new ArrayList<>()); needsSave = true; }
        if (!config.contains("exempt-players")) { config.set("exempt-players", new ArrayList<>()); needsSave = true; }
        if (!config.contains("max-events-per-player")) { config.set("max-events-per-player", 3); needsSave = true; }
        if (!config.contains("rewards.commands")) { config.set("rewards.commands", new ArrayList<>()); needsSave = true; }

        // ── Tutorial-specific keys ──
        if (!config.contains("tutorial.random-design")) { config.set("tutorial.random-design", true); needsSave = true; }
        if (!config.contains("tutorial.forced-design")) { config.set("tutorial.forced-design", ""); needsSave = true; }
        if (!config.contains("tutorial.show-step-titles")) { config.set("tutorial.show-step-titles", true); needsSave = true; }
        if (!config.contains("tutorial.show-step-actionbar")) { config.set("tutorial.show-step-actionbar", true); needsSave = true; }
        if (!config.contains("tutorial.step-complete-sound")) { config.set("tutorial.step-complete-sound", "entity.player.levelup"); needsSave = true; }
        if (!config.contains("tutorial.give-items-on-step")) { config.set("tutorial.give-items-on-step", true); needsSave = true; }
        if (!config.contains("completion.bonus-commands")) { config.set("completion.bonus-commands", new ArrayList<>()); needsSave = true; }
        if (!config.contains("completion.bonus-message")) { config.set("completion.bonus-message", "&aYou completed the tutorial!"); needsSave = true; }
        if (!config.contains("world")) { config.set("world", ""); needsSave = true; }
        if (!config.contains("spawn.base-interval-ticks")) { config.set("spawn.base-interval-ticks", 80); needsSave = true; }
        if (!config.contains("spawn.max-events-per-player")) { config.set("spawn.max-events-per-player", 3); needsSave = true; }
        if (!config.contains("spawn.offset-radius")) { config.set("spawn.offset-radius", 8.0); needsSave = true; }
        if (!config.contains("timer-hud.display-name")) { config.set("timer-hud.display-name", "TUTORIAL"); needsSave = true; }
        if (!config.contains("timer-hud.color")) { config.set("timer-hud.color", "gold"); needsSave = true; }
        if (!config.contains("timer-hud.flash-color")) { config.set("timer-hud.flash-color", "red"); needsSave = true; }
        if (!config.contains("timer-hud.flash-threshold-seconds")) { config.set("timer-hud.flash-threshold-seconds", 30); needsSave = true; }

        if (needsSave) {
            save();
            config = YamlConfiguration.loadConfiguration(configFile);
        }
    }

    public void save() {
        try { config.save(configFile); }
        catch (IOException e) { plugin.getLogger().severe("Failed to save tutorial config: " + e.getMessage()); }
    }

    public FileConfiguration get() { return config; }

    // ── Timer ──
    public long getDefaultTimerSeconds() { return config.getLong("timer.default-seconds", 210); }
    public long getMaxTimerSeconds() { return config.getLong("timer.max-seconds", 300); }

    // ── Spawning ──
    public int getBaseSpawnInterval() { return config.getInt("spawn.base-interval-ticks", 80); }
    public int getMaxEventsPerPlayer() { return config.getInt("spawn.max-events-per-player", 3); }
    public double getSpawnOffsetRadius() { return config.getDouble("spawn.offset-radius", 8.0); }

    // ── Music ──
    public String getMusicSoundId() { return config.getString("music.sound-id", ""); }
    public boolean isMusicLooped() { return config.getBoolean("music.loop", false); }
    public long getMusicDurationTicks() { return config.getLong("music.duration-ticks", 4200); }

    // ── Lifecycle ──
    public List<String> getOnStartCommands() { return config.getStringList("on-start-commands"); }
    public List<String> getOnEndCommands() { return config.getStringList("on-end-commands"); }
    public List<String> getExemptPlayers() { return config.getStringList("exempt-players"); }
    public List<String> getRewardCommands() { return config.getStringList("rewards.commands"); }
    public String getWorldName() { return config.getString("world", ""); }

    // ── Tutorial-specific ──
    public boolean isRandomDesign() { return config.getBoolean("tutorial.random-design", true); }
    public String getForcedDesign() { return config.getString("tutorial.forced-design", ""); }
    public boolean showStepTitles() { return config.getBoolean("tutorial.show-step-titles", true); }
    public boolean showStepActionbar() { return config.getBoolean("tutorial.show-step-actionbar", true); }
    public String getStepCompleteSound() { return config.getString("tutorial.step-complete-sound", "entity.player.levelup"); }
    public boolean giveItemsOnStep() { return config.getBoolean("tutorial.give-items-on-step", true); }

    // ── Completion ──
    public List<String> getCompletionBonusCommands() { return config.getStringList("completion.bonus-commands"); }
    public String getCompletionBonusMessage() { return config.getString("completion.bonus-message", "&aYou completed the tutorial!"); }

    // ── Default config creation ──
    private void createDefaults() {
        FileConfiguration d = new YamlConfiguration();

        d.set("config-version", CURRENT_CONFIG_VERSION);
        d.setComments("config-version", List.of("Internal version number — do NOT edit manually."));

        d.set("timer.default-seconds", 210);
        d.setComments("timer.default-seconds", List.of("Default tutorial duration in seconds. 210 = 3 minutes 30 seconds."));
        d.set("timer.max-seconds", 300);
        d.setComments("timer.max-seconds", List.of("Maximum timer value allowed via /cc modes tutorial start <seconds>."));

        d.set("tutorial.random-design", true);
        d.setComments("tutorial.random-design", List.of(
                "Whether to randomly select a tutorial design each run.",
                "Set to false and use forced-design to always use a specific design."));
        d.set("tutorial.forced-design", "");
        d.setComments("tutorial.forced-design", List.of(
                "Force a specific design by name (e.g., 'classic', 'speedrunner').",
                "Only used when random-design is false. Leave empty for random."));
        d.set("tutorial.show-step-titles", true);
        d.setComments("tutorial.show-step-titles", List.of("Show title messages when steps are completed."));
        d.set("tutorial.show-step-actionbar", true);
        d.setComments("tutorial.show-step-actionbar", List.of("Show current step progress in the actionbar."));
        d.set("tutorial.step-complete-sound", "entity.player.levelup");
        d.setComments("tutorial.step-complete-sound", List.of("Sound played when a step is completed."));
        d.set("tutorial.give-items-on-step", true);
        d.setComments("tutorial.give-items-on-step", List.of(
                "Give players the materials they need for the next step on completion.",
                "Critical for ensuring tutorial is completable within the time limit."));

        d.set("completion.bonus-commands", new ArrayList<>());
        d.setComments("completion.bonus-commands", List.of(
                "Extra commands run for players who complete ALL tutorial steps.",
                "These run IN ADDITION to the regular survival rewards.",
                "Use %player% placeholder. Example: give %player% diamond_block 1"));
        d.set("completion.bonus-message", "&aYou completed the tutorial!");
        d.setComments("completion.bonus-message", List.of("Message shown to players who complete the tutorial."));

        d.set("world", "");
        d.setComments("world", List.of("World where Tutorial Mode runs. Leave empty for first loaded world."));

        d.set("spawn.base-interval-ticks", 80);
        d.setComments("spawn.base-interval-ticks", List.of("Ticks between block display spawn attempts. 80 = every 4 seconds (slower pace for tutorial)."));
        d.set("spawn.max-events-per-player", 3);
        d.setComments("spawn.max-events-per-player", List.of("Max simultaneous displays per player. Keep low for tutorial."));
        d.set("spawn.offset-radius", 8.0);
        d.setComments("spawn.offset-radius", List.of("Max distance from player that displays spawn."));

        d.set("music.sound-id", "");
        d.setComments("music.sound-id", List.of("Background music sound ID. Leave empty to disable."));
        d.set("music.loop", false);
        d.setComments("music.loop", List.of("Whether to loop the music track."));
        d.set("music.duration-ticks", 4200);
        d.setComments("music.duration-ticks", List.of("Duration of music in ticks. 4200 = 3.5 minutes."));

        d.set("on-start-commands", new ArrayList<>());
        d.setComments("on-start-commands", List.of("Console commands run when tutorial starts."));
        d.set("on-end-commands", new ArrayList<>());
        d.setComments("on-end-commands", List.of("Console commands run when tutorial ends."));
        d.set("on-player-ready-commands", new ArrayList<>());
        d.setComments("on-player-ready-commands", List.of(
                "Commands run for each player when they exit title screen or change world during this mode.",
                "Supports wait <ticks>, done, and PlaceholderAPI. Use %player% for the player's name."));
        d.set("on-reset-commands", new ArrayList<>());
        d.setComments("on-reset-commands", List.of(
                "Per-mode reset commands. Available for manual use or future expansion."));
        d.set("exempt-players", new ArrayList<>());
        d.setComments("exempt-players", List.of("Players exempt from tutorial attacks."));
        d.set("max-events-per-player", 3);
        d.set("rewards.commands", new ArrayList<>());
        d.setComments("rewards.commands", List.of("Commands run for each surviving player when tutorial ends.", "Use %player% placeholder."));

        d.set("timer-hud.display-name", "TUTORIAL");
        d.setComments("timer-hud.display-name", List.of("Text shown on the BetterHud timer bar."));
        d.set("timer-hud.color", "gold");
        d.set("timer-hud.flash-color", "red");
        d.set("timer-hud.flash-threshold-seconds", 30);

        try { d.save(configFile); }
        catch (IOException e) { plugin.getLogger().severe("Failed to create tutorial config: " + e.getMessage()); }
    }
}
