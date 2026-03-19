package com.blockforge.chaoscraft.modes.corruption;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Corrupted Corruption mode configuration loader.
 * Reads from plugins/ChaosCraft/modes/corruption/corruption.yml
 *
 * Corruption Mode is an event-driven environmental horror mode with 48 corruption-themed events.
 * Features floating corrupted blocks, block replacement spreading, mob glitching,
 * and ambient horror effects that gradually corrupt the world around players.
 */
public class CorruptionConfig {

    private final ChaosCraftPlugin plugin;
    private final File configFile;
    private FileConfiguration config;

    public CorruptionConfig(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        File modeDir = new File(plugin.getDataFolder(), "modes/corruption");
        if (!modeDir.exists()) modeDir.mkdirs();
        this.configFile = new File(modeDir, "corruption.yml");
        load();
    }

    public void load() {
        if (!configFile.exists()) {
            createDefaults();
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save corruption config: " + e.getMessage());
        }
    }

    public FileConfiguration get() {
        return config;
    }

    // ========================
    // World
    // ========================

    /** The world name where Corruption Mode runs (default: "world"). */
    public String getWorldName() {
        return config.getString("world", "world");
    }

    // ========================
    // Timer
    // ========================

    public int getTimerSeconds() {
        return config.getInt("timer.default-seconds", 900);
    }

    public long getMaxTimerSeconds() {
        return config.getLong("timer.max-seconds", 1800);
    }

    // ========================
    // Floating Blocks
    // ========================

    public boolean isFloatingBlocksEnabled() {
        return config.getBoolean("floating-blocks.enabled", true);
    }

    public int getFloatingBlocksMaxPerChunk() {
        return config.getInt("floating-blocks.max-per-chunk", 30);
    }

    public int getFloatingBlocksSpreadRateTicks() {
        return config.getInt("floating-blocks.spread-rate-ticks", 40);
    }

    public double getFloatingBlocksMovementSpeed() {
        return config.getDouble("floating-blocks.movement-speed", 0.05);
    }

    public double getFloatingBlocksDamage() {
        return config.getDouble("floating-blocks.damage", 20.0);
    }

    public int getFloatingBlocksDarknessDurationSeconds() {
        return config.getInt("floating-blocks.darkness-duration-seconds", 5);
    }

    public int getFloatingBlocksDarknessAmplifier() {
        return config.getInt("floating-blocks.darkness-amplifier", 0);
    }

    // ========================
    // Block Replacement
    // ========================

    public boolean isBlockReplacementEnabled() {
        return config.getBoolean("block-replacement.enabled", true);
    }

    public int getBlockReplacementBlocksPerTick() {
        return config.getInt("block-replacement.blocks-per-tick", 5);
    }

    public int getBlockReplacementMaxRadiusChunks() {
        return config.getInt("block-replacement.max-radius-chunks", 5);
    }

    /**
     * Get the list of vanilla block materials that can be replaced by corruption spread.
     * Returns Material enums parsed from the config string list.
     */
    public List<Material> getBlockReplacementVanillaBlocks() {
        List<String> names = config.getStringList("block-replacement.vanilla-blocks");
        return names.stream()
                .map(name -> {
                    try {
                        return Material.valueOf(name.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("[CorruptionConfig] Unknown vanilla material: " + name);
                        return null;
                    }
                })
                .filter(m -> m != null)
                .collect(Collectors.toList());
    }

    /**
     * Get the list of ItemsAdder block IDs that can be replaced by corruption spread.
     */
    public List<String> getBlockReplacementItemsAdderBlocks() {
        return config.getStringList("block-replacement.itemsadder-blocks");
    }

    /**
     * Get the list of CraftEngine block IDs that can be replaced by corruption spread.
     */
    public List<String> getBlockReplacementCraftEngineBlocks() {
        return config.getStringList("block-replacement.craftengine-blocks");
    }

    // ========================
    // Restoration
    // ========================

    public int getRestorationBlocksPerTick() {
        return config.getInt("restoration.blocks-per-tick", 20);
    }

    public int getRestorationDelayAfterEndTicks() {
        return config.getInt("restoration.delay-after-end-ticks", 0);
    }

    // ========================
    // Mob Glitch
    // ========================

    public boolean isMobGlitchEnabled() {
        return config.getBoolean("mob-glitch.enabled", true);
    }

    public boolean isMobGlitchHostileOnly() {
        return config.getBoolean("mob-glitch.hostile-only", true);
    }

    public double getMobGlitchIntensity() {
        return config.getDouble("mob-glitch.intensity", 0.3);
    }

    public int getMobGlitchRadiusChunks() {
        return config.getInt("mob-glitch.radius-chunks", 5);
    }

    // ========================
    // Ambient
    // ========================

    public boolean isDarkParticlesEnabled() {
        return config.getBoolean("ambient.dark-particles", true);
    }

    public boolean isCorruptionFogEnabled() {
        return config.getBoolean("ambient.corruption-fog", true);
    }

    public boolean isAmbientSoundsEnabled() {
        return config.getBoolean("ambient.ambient-sounds", true);
    }

    public int getAmbientSoundIntervalTicks() {
        return config.getInt("ambient.sound-interval-ticks", 200);
    }

    // ========================
    // Claims
    // ========================

    public boolean isRespectClaims() {
        return config.getBoolean("respect-claims", true);
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

        // World
        defaults.set("world", "world");

        // Timer
        defaults.set("timer.default-seconds", 900);
        defaults.set("timer.max-seconds", 1800);

        // Floating Blocks
        defaults.set("floating-blocks.enabled", true);
        defaults.set("floating-blocks.max-per-chunk", 30);
        defaults.set("floating-blocks.spread-rate-ticks", 40);
        defaults.set("floating-blocks.movement-speed", 0.05);
        defaults.set("floating-blocks.damage", 20.0);
        defaults.set("floating-blocks.darkness-duration-seconds", 5);
        defaults.set("floating-blocks.darkness-amplifier", 0);

        // Block Replacement
        defaults.set("block-replacement.enabled", true);
        defaults.set("block-replacement.blocks-per-tick", 5);
        defaults.set("block-replacement.max-radius-chunks", 5);

        List<String> defaultVanillaBlocks = List.of(
                "GRASS_BLOCK", "DIRT", "STONE", "COBBLESTONE", "OAK_LOG", "OAK_LEAVES",
                "BIRCH_LOG", "BIRCH_LEAVES", "SPRUCE_LOG", "SPRUCE_LEAVES",
                "SAND", "GRAVEL", "OAK_PLANKS", "BIRCH_PLANKS", "SPRUCE_PLANKS"
        );
        defaults.set("block-replacement.vanilla-blocks", defaultVanillaBlocks);
        defaults.set("block-replacement.itemsadder-blocks", new ArrayList<>());
        defaults.set("block-replacement.craftengine-blocks", new ArrayList<>());

        // Restoration
        defaults.set("restoration.blocks-per-tick", 20);
        defaults.set("restoration.delay-after-end-ticks", 0);

        // Mob Glitch
        defaults.set("mob-glitch.enabled", true);
        defaults.set("mob-glitch.hostile-only", true);
        defaults.set("mob-glitch.intensity", 0.3);
        defaults.set("mob-glitch.radius-chunks", 5);

        // Ambient
        defaults.set("ambient.dark-particles", true);
        defaults.set("ambient.corruption-fog", true);
        defaults.set("ambient.ambient-sounds", true);
        defaults.set("ambient.sound-interval-ticks", 200);

        // Claims
        defaults.set("respect-claims", true);

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
            plugin.getLogger().severe("Failed to create default corruption config: " + e.getMessage());
        }
    }
}
