package com.blockforge.chaoscraft.modes.corruption;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.blockforge.chaoscraft.services.mobspawn.MobSpawnConfig;

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

        // Ensure ALL keys exist — both base mode keys and corruption-specific keys.
        boolean needsSave = false;

        // ── Base mode keys ───────────────────────────────────────────────────
        if (!config.contains("config-version")) { config.set("config-version", 2); needsSave = true; }
        if (config.getInt("config-version") < 2) { config.set("config-version", 2); needsSave = true; }
        if (!config.contains("timer.default-seconds")) { config.set("timer.default-seconds", 1200); needsSave = true; }
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

        // ── Corruption-specific keys ─────────────────────────────────────────
        if (!config.contains("floating-blocks")) { ensureCorruptionDefaults(); needsSave = true; }
        if (!config.contains("block-replacement")) { ensureCorruptionDefaults(); needsSave = true; }
        if (!config.contains("mob-glitch")) { ensureCorruptionDefaults(); needsSave = true; }
        if (!config.contains("ambient")) { ensureCorruptionDefaults(); needsSave = true; }
        if (!config.contains("world")) { config.set("world", "world"); needsSave = true; }
        if (!config.contains("respect-claims")) { config.set("respect-claims", true); needsSave = true; }

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

    /**
     * Add corruption-specific defaults to the existing config without overwriting user values.
     * Also sets comments so that newly-added keys are documented in the file.
     */
    private void ensureCorruptionDefaults() {
        // ── Floating Blocks ────────────────────────────────────────────────────
        if (!config.contains("floating-blocks.enabled")) {
            config.set("floating-blocks.enabled", true);
            config.setComments("floating-blocks.enabled", List.of(
                    "Enable or disable the floating corrupted block entities entirely.",
                    "When enabled, blocks rise from the ground and drift toward players during the mode."));
        }
        if (!config.contains("floating-blocks.max-per-chunk")) {
            config.set("floating-blocks.max-per-chunk", 30);
            config.setComments("floating-blocks.max-per-chunk", List.of(
                    "Maximum number of floating block entities allowed per loaded chunk at once.",
                    "Higher = denser corruption but more entities = more server load. Recommended: 15–50."));
        }
        if (!config.contains("floating-blocks.spread-rate-ticks")) {
            config.set("floating-blocks.spread-rate-ticks", 40);
            config.setComments("floating-blocks.spread-rate-ticks", List.of(
                    "How often (in ticks) the floating block system attempts to spawn new blocks.",
                    "20 = every second, 40 = every 2 seconds. Lower = faster spreading corruption."));
        }
        if (!config.contains("floating-blocks.movement-speed")) {
            config.set("floating-blocks.movement-speed", 0.05);
            config.setComments("floating-blocks.movement-speed", List.of(
                    "Velocity at which floating blocks move toward the nearest player each tick.",
                    "0.05 = slow creeping, 0.1 = moderate, 0.2 = fast and aggressive."));
        }
        if (!config.contains("floating-blocks.damage")) {
            config.set("floating-blocks.damage", 20.0);
            config.setComments("floating-blocks.damage", List.of(
                    "Damage dealt to a player when a floating block collides with them.",
                    "In half-hearts: 20.0 = 10 hearts. This is a one-time impact hit, not per-tick."));
        }
        if (!config.contains("floating-blocks.darkness-duration-seconds")) {
            config.set("floating-blocks.darkness-duration-seconds", 5);
            config.setComments("floating-blocks.darkness-duration-seconds", List.of(
                    "Duration in seconds of the Darkness potion effect applied when a floating block hits a player.",
                    "The Darkness effect dims the player's vision. Set to 0 to disable the effect."));
        }
        if (!config.contains("floating-blocks.darkness-amplifier")) {
            config.set("floating-blocks.darkness-amplifier", 0);
            config.setComments("floating-blocks.darkness-amplifier", List.of(
                    "Amplifier level for the Darkness effect (0 = level 1, 1 = level 2, etc.).",
                    "Higher amplifier = darker screen. Level 1 (0) is already very dark."));
        }

        // ── Block Replacement (World Corruption) ───────────────────────────────
        if (!config.contains("block-replacement.enabled")) {
            config.set("block-replacement.enabled", true);
            config.setComments("block-replacement.enabled", List.of(
                    "Enable or disable the world corruption block replacement system.",
                    "When enabled, vanilla blocks near players are gradually replaced with corruption blocks."));
        }
        if (!config.contains("block-replacement.blocks-per-tick")) {
            config.set("block-replacement.blocks-per-tick", 5);
            config.setComments("block-replacement.blocks-per-tick", List.of(
                    "How many blocks are replaced with corruption blocks per tick across the world.",
                    "Higher = faster world corruption spread. Recommended: 3–10 for smooth spread."));
        }
        if (!config.contains("block-replacement.max-radius-chunks")) {
            config.set("block-replacement.max-radius-chunks", 5);
            config.setComments("block-replacement.max-radius-chunks", List.of(
                    "Maximum radius in chunks from any online player that corruption can spread.",
                    "1 chunk = 16 blocks. 5 = 80 blocks radius. Larger radius = wider corruption zone."));
        }
        if (!config.contains("block-replacement.vanilla-blocks")) {
            config.set("block-replacement.vanilla-blocks", List.of(
                    "CRYING_OBSIDIAN", "BLACKSTONE", "DEEPSLATE", "SCULK", "COAL_BLOCK"));
            config.setComments("block-replacement.vanilla-blocks", List.of(
                    "List of vanilla Minecraft material names that corruption will replace nearby blocks WITH.",
                    "These are the block types the world gets corrupted INTO — the \"corruption palette\".",
                    "Use exact Material enum names (uppercase). Example: CRYING_OBSIDIAN, SCULK, DEEPSLATE."));
        }
        if (!config.contains("block-replacement.itemsadder-blocks")) {
            config.set("block-replacement.itemsadder-blocks", new java.util.ArrayList<>());
            config.setComments("block-replacement.itemsadder-blocks", List.of(
                    "List of ItemsAdder custom block IDs that can also be used in the corruption palette.",
                    "Only used if ItemsAdder is installed. Format: \"namespace:block_id\".",
                    "Example: \"chaoscraft:corrupted_stone\""));
        }
        if (!config.contains("block-replacement.craftengine-blocks")) {
            config.set("block-replacement.craftengine-blocks", new java.util.ArrayList<>());
            config.setComments("block-replacement.craftengine-blocks", List.of(
                    "List of CraftEngine custom block IDs that can also be used in the corruption palette.",
                    "Only used if CraftEngine is installed. Format: \"namespace:block_id\"."));
        }

        // ── Restoration ────────────────────────────────────────────────────────
        if (!config.contains("restoration.blocks-per-tick")) {
            config.set("restoration.blocks-per-tick", 20);
            config.setComments("restoration.blocks-per-tick", List.of(
                    "How many corrupted blocks are restored back to their original state per tick",
                    "after the mode ends. Higher = faster world cleanup. Recommended: 10–50."));
        }
        if (!config.contains("restoration.delay-after-end-ticks")) {
            config.set("restoration.delay-after-end-ticks", 0);
            config.setComments("restoration.delay-after-end-ticks", List.of(
                    "Ticks to wait after the mode ends before starting block restoration.",
                    "0 = restore immediately. 100 = wait 5 seconds before cleanup begins."));
        }

        // ── Mob Glitch ─────────────────────────────────────────────────────────
        if (!config.contains("mob-glitch.enabled")) {
            config.set("mob-glitch.enabled", true);
            config.setComments("mob-glitch.enabled", List.of(
                    "Enable or disable the mob glitch visual effect during Corruption Mode.",
                    "When enabled, nearby mobs flicker and glitch as if corrupted by the environment."));
        }
        if (!config.contains("mob-glitch.hostile-only")) {
            config.set("mob-glitch.hostile-only", true);
            config.setComments("mob-glitch.hostile-only", List.of(
                    "If true, only hostile mobs (monsters) are affected by the glitch effect.",
                    "Set to false to also glitch passive mobs like cows, pigs, and villagers."));
        }
        if (!config.contains("mob-glitch.intensity")) {
            config.set("mob-glitch.intensity", 0.3);
            config.setComments("mob-glitch.intensity", List.of(
                    "Intensity of the glitch effect on affected mobs. Range: 0.0 (none) to 1.0 (maximum).",
                    "Higher = more frequent and severe flickering/teleporting visual glitches."));
        }
        if (!config.contains("mob-glitch.radius-chunks")) {
            config.set("mob-glitch.radius-chunks", 5);
            config.setComments("mob-glitch.radius-chunks", List.of(
                    "Radius in chunks around each player within which mobs are affected by the glitch.",
                    "1 chunk = 16 blocks. 5 = 80 blocks radius."));
        }

        // ── Ambient Effects ────────────────────────────────────────────────────
        if (!config.contains("ambient.dark-particles")) {
            config.set("ambient.dark-particles", true);
            config.setComments("ambient.dark-particles", List.of(
                    "Enable or disable dark particle effects that appear around players during the mode.",
                    "Creates an atmospheric horror visual of corruption floating in the air."));
        }
        if (!config.contains("ambient.corruption-fog")) {
            config.set("ambient.corruption-fog", true);
            config.setComments("ambient.corruption-fog", List.of(
                    "Enable or disable the corruption fog effect applied to players' screens.",
                    "Uses a Blindness/Darkness overlay to create a claustrophobic, foggy atmosphere."));
        }
        if (!config.contains("ambient.ambient-sounds")) {
            config.set("ambient.ambient-sounds", true);
            config.setComments("ambient.ambient-sounds", List.of(
                    "Enable or disable periodic ambient horror sound effects played to players.",
                    "Uses Minecraft's cave/warden ambient sounds to build tension."));
        }
        if (!config.contains("ambient.sound-interval-ticks")) {
            config.set("ambient.sound-interval-ticks", 200);
            config.setComments("ambient.sound-interval-ticks", List.of(
                    "How often (in ticks) an ambient sound is played to each player. 200 = every 10 seconds.",
                    "Lower = more frequent unsettling sounds. Higher = rare, sudden sound stings."));
        }

        // ── Claims Integration ─────────────────────────────────────────────────
        if (!config.contains("respect-claims")) {
            config.set("respect-claims", true);
            config.setComments("respect-claims", List.of(
                    "If true, block replacement corruption will NOT spread into claimed land areas.",
                    "Requires ChaosCraft Claims (or GriefPrevention) to be active.",
                    "Set to false to allow corruption to spread everywhere regardless of claims."));
        }

        // ── Spawning ───────────────────────────────────────────────────────────
        if (!config.contains("spawn.base-interval-ticks")) {
            config.set("spawn.base-interval-ticks", 50);
            config.setComments("spawn.base-interval-ticks", List.of(
                    "Ticks between automatic corruption event spawn attempts per player. 20 ticks = 1 second.",
                    "50 = attempt to spawn an event every 2.5 seconds per player."));
        }
        if (!config.contains("spawn.max-events-per-player")) {
            config.set("spawn.max-events-per-player", 5);
            config.setComments("spawn.max-events-per-player", List.of(
                    "Maximum simultaneous active corruption events targeting one player at once.",
                    "New events won't spawn for a player who already has this many active events."));
        }
        if (!config.contains("spawn.offset-radius")) {
            config.set("spawn.offset-radius", 10.0);
            config.setComments("spawn.offset-radius", List.of(
                    "Maximum distance in blocks from the player that corruption events can spawn.",
                    "Events appear at a random position within this radius around the player."));
        }

        // ── Timer HUD (BetterHud) ──────────────────────────────────────────────
        if (!config.contains("timer-hud.display-name")) {
            config.set("timer-hud.display-name", "CORRUPTED CORRUPTION");
            config.setComments("timer-hud.display-name", List.of(
                    "Text shown on the right side of the BetterHud mode timer bar.",
                    "This is the label players see during the session."));
        }
        if (!config.contains("timer-hud.color")) {
            config.set("timer-hud.color", "dark_purple");
            config.setComments("timer-hud.color", List.of(
                    "Color of the mode name text in the BetterHud timer bar (normal state).",
                    "Use MiniMessage color names: dark_purple, red, aqua, gold, gray, etc."));
        }
        if (!config.contains("timer-hud.flash-color")) {
            config.set("timer-hud.flash-color", "red");
            config.setComments("timer-hud.flash-color", List.of(
                    "Color the timer text flashes to during the low-time warning.",
                    "Alternates between white and this color every 10 ticks when below the threshold."));
        }
        if (!config.contains("timer-hud.flash-threshold-seconds")) {
            config.set("timer-hud.flash-threshold-seconds", 60);
            config.setComments("timer-hud.flash-threshold-seconds", List.of(
                    "Seconds remaining at which the timer text starts flashing. 60 = 1 minute warning.",
                    "Set to 0 to disable the flashing warning entirely."));
        }
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

        // ── World ──────────────────────────────────────────────────────────────
        defaults.set("world", "world");
        defaults.setComments("world", List.of(
                "World name where Corruption Mode runs (floating blocks, corruption spread, ambient effects).",
                "Use the exact world folder name. Example: \"world\", \"survival\", \"skyworld\"."));

        // ── Timer ──────────────────────────────────────────────────────────────
        defaults.set("timer.default-seconds", 900);
        defaults.setComments("timer.default-seconds", List.of(
                "Default duration of Corruption Mode in seconds when started without a time argument.",
                "900 = 15 min | 600 = 10 min | 1200 = 20 min."));
        defaults.set("timer.max-seconds", 1800);
        defaults.setComments("timer.max-seconds", List.of(
                "Maximum timer value (in seconds) allowed via: /cc modes corruption start <seconds>",
                "Prevents excessively long sessions."));

        // ── Floating Blocks ────────────────────────────────────────────────────
        defaults.set("floating-blocks.enabled", true);
        defaults.setComments("floating-blocks.enabled", List.of(
                "Enable or disable the floating corrupted block entities entirely.",
                "When enabled, blocks rise from the ground and drift toward players during the mode."));
        defaults.set("floating-blocks.max-per-chunk", 30);
        defaults.setComments("floating-blocks.max-per-chunk", List.of(
                "Maximum number of floating block entities allowed per loaded chunk at once.",
                "Higher = denser corruption but more entities = more server load. Recommended: 15–50."));
        defaults.set("floating-blocks.spread-rate-ticks", 40);
        defaults.setComments("floating-blocks.spread-rate-ticks", List.of(
                "How often (in ticks) the floating block system attempts to spawn new blocks.",
                "20 = every second, 40 = every 2 seconds. Lower = faster spreading corruption."));
        defaults.set("floating-blocks.movement-speed", 0.05);
        defaults.setComments("floating-blocks.movement-speed", List.of(
                "Velocity at which floating blocks move toward the nearest player each tick.",
                "0.05 = slow creeping, 0.1 = moderate, 0.2 = fast and aggressive."));
        defaults.set("floating-blocks.damage", 20.0);
        defaults.setComments("floating-blocks.damage", List.of(
                "Damage dealt to a player when a floating block collides with them (in half-hearts).",
                "20.0 = 10 hearts. This is a one-time impact hit, not continuous damage."));
        defaults.set("floating-blocks.darkness-duration-seconds", 5);
        defaults.setComments("floating-blocks.darkness-duration-seconds", List.of(
                "Duration in seconds of the Darkness effect applied when a floating block hits a player.",
                "Darkness dims the player's vision. Set to 0 to disable the effect entirely."));
        defaults.set("floating-blocks.darkness-amplifier", 0);
        defaults.setComments("floating-blocks.darkness-amplifier", List.of(
                "Amplifier level for the Darkness effect (0 = level 1, 1 = level 2, etc.).",
                "Higher amplifier = darker screen. Level 1 (0) is already very dark."));

        // ── Block Replacement (World Corruption Spread) ────────────────────────
        defaults.set("block-replacement.enabled", true);
        defaults.setComments("block-replacement.enabled", List.of(
                "Enable or disable the world corruption block replacement system.",
                "When enabled, natural blocks near players are gradually replaced with corruption blocks."));
        defaults.set("block-replacement.blocks-per-tick", 5);
        defaults.setComments("block-replacement.blocks-per-tick", List.of(
                "How many blocks are replaced with corruption blocks per server tick.",
                "Higher = faster world corruption spread. Recommended: 3–10 for smooth performance."));
        defaults.set("block-replacement.max-radius-chunks", 5);
        defaults.setComments("block-replacement.max-radius-chunks", List.of(
                "Maximum radius in chunks from any online player that corruption can spread.",
                "1 chunk = 16 blocks. 5 = 80 blocks radius. Larger = wider corruption zone."));
        defaults.set("block-replacement.vanilla-blocks", List.of(
                "GRASS_BLOCK", "DIRT", "STONE", "COBBLESTONE", "OAK_LOG", "OAK_LEAVES",
                "BIRCH_LOG", "BIRCH_LEAVES", "SPRUCE_LOG", "SPRUCE_LEAVES",
                "SAND", "GRAVEL", "OAK_PLANKS", "BIRCH_PLANKS", "SPRUCE_PLANKS"));
        defaults.setComments("block-replacement.vanilla-blocks", List.of(
                "List of vanilla block material names that the corruption system can REPLACE.",
                "These are blocks that get swapped OUT — the terrain that gets corrupted.",
                "Use exact Material enum names (uppercase). Add any blocks you want corruption to affect."));
        defaults.set("block-replacement.itemsadder-blocks", new ArrayList<>());
        defaults.setComments("block-replacement.itemsadder-blocks", List.of(
                "ItemsAdder custom block IDs to include in the replaceable block list.",
                "Only used if ItemsAdder is installed. Format: \"namespace:block_id\"."));
        defaults.set("block-replacement.craftengine-blocks", new ArrayList<>());
        defaults.setComments("block-replacement.craftengine-blocks", List.of(
                "CraftEngine custom block IDs to include in the replaceable block list.",
                "Only used if CraftEngine is installed. Format: \"namespace:block_id\"."));

        // ── Restoration ────────────────────────────────────────────────────────
        defaults.set("restoration.blocks-per-tick", 20);
        defaults.setComments("restoration.blocks-per-tick", List.of(
                "How many corrupted blocks are restored to their original state per tick after the mode ends.",
                "Higher = faster world cleanup. Recommended: 10–50."));
        defaults.set("restoration.delay-after-end-ticks", 0);
        defaults.setComments("restoration.delay-after-end-ticks", List.of(
                "Ticks to wait after the mode ends before block restoration begins.",
                "0 = start restoring immediately. 100 = wait 5 seconds before cleanup."));

        // ── Mob Glitch ─────────────────────────────────────────────────────────
        defaults.set("mob-glitch.enabled", true);
        defaults.setComments("mob-glitch.enabled", List.of(
                "Enable or disable the mob glitch visual effect during Corruption Mode.",
                "Nearby mobs flicker and visually glitch as if corrupted by the environment."));
        defaults.set("mob-glitch.hostile-only", true);
        defaults.setComments("mob-glitch.hostile-only", List.of(
                "If true, only hostile mobs (monsters) are affected by the glitch effect.",
                "Set to false to also apply glitch visuals to passive mobs like cows and villagers."));
        defaults.set("mob-glitch.intensity", 0.3);
        defaults.setComments("mob-glitch.intensity", List.of(
                "Intensity of the glitch effect on affected mobs. Range: 0.0 (none) to 1.0 (maximum).",
                "Higher = more frequent and severe flickering/positional glitch visuals."));
        defaults.set("mob-glitch.radius-chunks", 5);
        defaults.setComments("mob-glitch.radius-chunks", List.of(
                "Radius in chunks around each player where mobs are affected by the glitch effect.",
                "1 chunk = 16 blocks. 5 = 80 blocks. Larger = more mobs affected at once."));

        // ── Ambient Effects ────────────────────────────────────────────────────
        defaults.set("ambient.dark-particles", true);
        defaults.setComments("ambient.dark-particles", List.of(
                "Enable dark particle effects that appear around players during the mode.",
                "Creates an atmospheric visual of corruption particles floating in the air."));
        defaults.set("ambient.corruption-fog", true);
        defaults.setComments("ambient.corruption-fog", List.of(
                "Enable the corruption fog effect (Blindness/Darkness overlay) applied to players.",
                "Creates a claustrophobic, foggy atmosphere during the mode."));
        defaults.set("ambient.ambient-sounds", true);
        defaults.setComments("ambient.ambient-sounds", List.of(
                "Enable periodic ambient horror sound effects played to all players.",
                "Uses Minecraft cave/warden ambient sounds at configurable intervals."));
        defaults.set("ambient.sound-interval-ticks", 200);
        defaults.setComments("ambient.sound-interval-ticks", List.of(
                "How often (in ticks) an ambient sound is played to each player. 200 = every 10 seconds.",
                "Lower = more frequent unsettling sounds. Higher = rare, sudden sound stings."));

        // ── Claims Integration ─────────────────────────────────────────────────
        defaults.set("respect-claims", true);
        defaults.setComments("respect-claims", List.of(
                "If true, block replacement corruption will NOT spread into claimed land areas.",
                "Requires ChaosCraft Claims or GriefPrevention to be active.",
                "Set to false to allow corruption to spread everywhere regardless of claims."));

        // ── Spawning ───────────────────────────────────────────────────────────
        defaults.set("spawn.base-interval-ticks", 50);
        defaults.setComments("spawn.base-interval-ticks", List.of(
                "Ticks between automatic corruption event spawn attempts per player. 20 ticks = 1 second.",
                "50 = attempt to spawn an event every 2.5 seconds per player."));
        defaults.set("spawn.max-events-per-player", 5);
        defaults.setComments("spawn.max-events-per-player", List.of(
                "Maximum simultaneous active corruption events targeting one player at once.",
                "New events won't spawn for a player who already has this many active. Recommended: 3–8."));
        defaults.set("spawn.offset-radius", 10.0);
        defaults.setComments("spawn.offset-radius", List.of(
                "Maximum distance in blocks from the player that corruption events can appear.",
                "Events spawn at a random position within this radius around each player."));

        // ── Timer HUD (BetterHud) ──────────────────────────────────────────────
        defaults.set("timer-hud.display-name", "CORRUPTED CORRUPTION");
        defaults.setComments("timer-hud.display-name", List.of(
                "Text shown on the right side of the BetterHud mode timer bar during this mode."));
        defaults.set("timer-hud.color", "dark_purple");
        defaults.setComments("timer-hud.color", List.of(
                "Color of the mode name text in the BetterHud timer bar (normal state).",
                "Use MiniMessage color names: dark_purple, red, aqua, gold, gray, etc."));
        defaults.set("timer-hud.flash-color", "red");
        defaults.setComments("timer-hud.flash-color", List.of(
                "Color the timer text flashes to when time is running low.",
                "Alternates between white and this color every 10 ticks below the threshold."));
        defaults.set("timer-hud.flash-threshold-seconds", 60);
        defaults.setComments("timer-hud.flash-threshold-seconds", List.of(
                "Seconds remaining at which the timer text starts flashing. 60 = 1 minute warning.",
                "Set to 0 to disable the flashing warning entirely."));

        // ── Music ──────────────────────────────────────────────────────────────
        defaults.set("music.sound-id", "");
        defaults.setComments("music.sound-id", List.of(
                "Namespaced sound ID to play as background music during Corruption Mode.",
                "Example: chaoscraft:music.corruption_theme   |   Leave empty (\"\") to disable."));
        defaults.set("music.loop", true);
        defaults.setComments("music.loop", List.of(
                "Whether the background music track loops continuously throughout the mode."));
        defaults.set("music.duration-ticks", 6000);
        defaults.setComments("music.duration-ticks", List.of(
                "Duration of one music loop in ticks before it replays. 6000 = 5 minutes.",
                "Set this to match your actual audio file length."));

        // ── Lifecycle Commands ─────────────────────────────────────────────────
        defaults.set("on-start-commands", new ArrayList<>());
        defaults.setComments("on-start-commands", List.of(
                "Console commands run when Corruption Mode starts. %player% = player who started it.",
                "Example:",
                "  - \"broadcast &5The world is being corrupted!\""));
        defaults.set("on-end-commands", new ArrayList<>());
        defaults.setComments("on-end-commands", List.of(
                "Console commands run when Corruption Mode ends."));
        defaults.set("on-player-ready-commands", new ArrayList<>());
        defaults.setComments("on-player-ready-commands", List.of(
                "Commands run for each player when they exit title screen or change world during this mode.",
                "Supports wait <ticks>, done, and PlaceholderAPI. Use %player% for the player's name."));
        defaults.set("on-reset-commands", new ArrayList<>());
        defaults.setComments("on-reset-commands", List.of(
                "Per-mode reset commands. Available for manual use or future expansion."));
        defaults.set("exempt-players", new ArrayList<>());
        defaults.setComments("exempt-players", List.of(
                "Player names that receive ZERO damage from all corruption attacks (staff bypass)."));
        defaults.set("rewards.commands", new ArrayList<>());
        defaults.setComments("rewards.commands", List.of(
                "Commands run for each surviving player when the mode ends successfully.",
                "Use %player% as a placeholder for each player's name."));

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
        MobSpawnConfig.writeDefaults(defaults, List.of(
                new MobSpawnConfig.MobSpawnDefaultEntry("ZOMBIE", "vanilla", 10, 2, 4, 1.5, 1.0),
                new MobSpawnConfig.MobSpawnDefaultEntry("PHANTOM", "vanilla", 5, 1, 2, 1.2, 1.0),
                new MobSpawnConfig.MobSpawnDefaultEntry("ENDERMITE", "vanilla", 3, 2, 5, 1.0, 0.8)
        ));

        try {
            defaults.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create default corruption config: " + e.getMessage());
        }
    }
}
