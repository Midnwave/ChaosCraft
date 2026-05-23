package com.blockforge.chaoscraft.modes.chain;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.services.mobspawn.MobSpawnConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private static final int CURRENT_CONFIG_VERSION = 3;

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

        // ── Gimmick: Chain Attack (mobs leash players) ──────────────────
        if (!config.contains("gimmick.enabled")) { config.set("gimmick.enabled", true); needsSave = true; }
        if (!config.contains("gimmick.chain-attack.enabled")) { config.set("gimmick.chain-attack.enabled", true); needsSave = true; }
        if (!config.contains("gimmick.chain-attack.global-cooldown-per-player-ticks")) {
            config.set("gimmick.chain-attack.global-cooldown-per-player-ticks", 120); needsSave = true;
        }
        if (!config.contains("gimmick.chain-attack.per-mob-cooldown-ticks")) {
            config.set("gimmick.chain-attack.per-mob-cooldown-ticks", 80); needsSave = true;
        }
        if (!config.contains("gimmick.chain-attack.default-reach-radius")) {
            config.set("gimmick.chain-attack.default-reach-radius", 12.0); needsSave = true;
        }
        if (!config.contains("gimmick.chain-attack.default-effect")) {
            config.set("gimmick.chain-attack.default-effect", "PULL"); needsSave = true;
        }
        if (!config.contains("gimmick.chain-attack.default-damage")) {
            config.set("gimmick.chain-attack.default-damage", 100.0); needsSave = true;
        }
        if (!config.contains("gimmick.chain-attack.default-effect-duration-ticks")) {
            config.set("gimmick.chain-attack.default-effect-duration-ticks", 30); needsSave = true;
        }
        if (!config.contains("gimmick.chain-attack.proximity-roll-chance")) {
            config.set("gimmick.chain-attack.proximity-roll-chance", 0.02); needsSave = true;
        }
        if (!config.contains("gimmick.chain-attack.chain-link-spacing")) {
            config.set("gimmick.chain-attack.chain-link-spacing", 1.0); needsSave = true;
        }
        if (!config.contains("gimmick.chain-attack.chain-link-scale")) {
            config.set("gimmick.chain-attack.chain-link-scale", 0.25); needsSave = true;
        }
        if (!config.contains("gimmick.chain-attack.pull-strength")) {
            config.set("gimmick.chain-attack.pull-strength", 1.4); needsSave = true;
        }
        if (!config.contains("gimmick.chain-attack.swing-rate-rad-per-tick")) {
            config.set("gimmick.chain-attack.swing-rate-rad-per-tick", 0.25); needsSave = true;
        }
        if (!config.contains("gimmick.chain-attack.swing-radius")) {
            config.set("gimmick.chain-attack.swing-radius", 3.0); needsSave = true;
        }
        if (!config.contains("gimmick.chain-attack.slam-velocity-y")) {
            config.set("gimmick.chain-attack.slam-velocity-y", -1.8); needsSave = true;
        }
        if (!config.contains("gimmick.chain-attack.launch-strength")) {
            config.set("gimmick.chain-attack.launch-strength", 1.8); needsSave = true;
        }

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

    /**
     * Default mob pool — 9 chain-themed MythicMobs spread across 3 sub-vibes
     * (industrial / cursed / spectral). Each entry's chain-attack override
     * (effect, reach, damage, duration) is written separately by
     * {@link #buildDefaultChainMobs()} once the base mob list has been laid
     * down. This split exists because MobSpawnConfig.MobSpawnDefaultEntry
     * only writes the canonical mob-spawning fields.
     */
    private List<MobSpawnConfig.MobSpawnDefaultEntry> buildDefaultMobSpawnEntries() {
        List<MobSpawnConfig.MobSpawnDefaultEntry> list = new ArrayList<>();
        // Industrial
        list.add(new MobSpawnConfig.MobSpawnDefaultEntry("ChainBrute",          "mythicmobs", 10, 1, 1, 1.0, 1.0));
        list.add(new MobSpawnConfig.MobSpawnDefaultEntry("IronInquisitor",      "mythicmobs",  8, 1, 1, 1.0, 1.0));
        list.add(new MobSpawnConfig.MobSpawnDefaultEntry("AnchorSentinel",      "mythicmobs",  5, 1, 1, 1.0, 1.0));
        // Cursed
        list.add(new MobSpawnConfig.MobSpawnDefaultEntry("BoundWarden",         "mythicmobs",  8, 1, 1, 1.0, 1.0));
        list.add(new MobSpawnConfig.MobSpawnDefaultEntry("ShackleWraith",       "mythicmobs",  8, 1, 1, 1.0, 1.0));
        list.add(new MobSpawnConfig.MobSpawnDefaultEntry("CursedExecutioner",   "mythicmobs",  6, 1, 1, 1.0, 1.0));
        // Spectral
        list.add(new MobSpawnConfig.MobSpawnDefaultEntry("SpectralConvict",     "mythicmobs",  9, 1, 1, 1.0, 1.0));
        list.add(new MobSpawnConfig.MobSpawnDefaultEntry("GhostSentenceBearer", "mythicmobs",  8, 1, 1, 1.0, 1.0));
        list.add(new MobSpawnConfig.MobSpawnDefaultEntry("IronMaidenPhantom",   "mythicmobs",  6, 1, 1, 1.0, 1.0));
        return list;
    }

    /**
     * Full chain-mode mob pool with per-mob chain-attack overrides.
     * Used by {@link #createDefaults()} to write a richer mob-spawning.mobs
     * list than the base MobSpawnConfig writer supports (which only knows
     * the canonical fields, not chain-attack overrides).
     */
    private List<Map<String, Object>> buildDefaultChainMobs() {
        List<Map<String, Object>> mobs = new ArrayList<>();
        // Industrial
        mobs.add(chainMob("ChainBrute",          10, 1.0, 1.0, "PULL",        12.0, 120.0, 30));
        mobs.add(chainMob("IronInquisitor",       8, 1.0, 1.0, "SLAM",        11.0, 160.0, 28));
        mobs.add(chainMob("AnchorSentinel",       5, 1.0, 1.0, "SWING",       14.0, 130.0, 40));
        // Cursed
        mobs.add(chainMob("BoundWarden",          8, 1.0, 1.0, "LAUNCH",      12.0, 140.0, 24));
        mobs.add(chainMob("ShackleWraith",        8, 1.0, 1.0, "ANCHOR",      11.0, 100.0, 35));
        mobs.add(chainMob("CursedExecutioner",    6, 1.0, 1.0, "DAMAGE_ONLY", 10.0, 220.0, 18));
        // Spectral
        mobs.add(chainMob("SpectralConvict",      9, 1.0, 1.0, "SWING",       12.0, 110.0, 32));
        mobs.add(chainMob("GhostSentenceBearer",  8, 1.0, 1.0, "PULL",        13.0, 130.0, 28));
        mobs.add(chainMob("IronMaidenPhantom",    6, 1.0, 1.0, "SLAM",        11.0, 170.0, 26));
        return mobs;
    }

    /**
     * Build a single mob-spawning.mobs[] entry with a chain-attack override.
     * @param id              MythicMobs internal ID
     * @param weight          spawn weight (higher = more frequent)
     * @param hpMult          health multiplier vs. base
     * @param dmgMult         damage multiplier vs. base
     * @param effect          chain effect (PULL/SWING/SLAM/LAUNCH/ANCHOR/DAMAGE_ONLY)
     * @param reach           chain reach radius in blocks
     * @param damage          chain attach damage
     * @param durationTicks   effect phase duration in ticks
     */
    private Map<String, Object> chainMob(String id, int weight, double hpMult, double dmgMult,
                                         String effect, double reach, double damage, int durationTicks) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("type", "mythicmobs");
        m.put("weight", weight);
        m.put("min-count", 1);
        m.put("max-count", 1);
        m.put("health-multiplier", hpMult);
        m.put("damage-multiplier", dmgMult);
        Map<String, Object> chain = new LinkedHashMap<>();
        chain.put("enabled", true);
        chain.put("reach-radius", reach);
        chain.put("effect", effect);
        chain.put("damage", damage);
        chain.put("effect-duration-ticks", durationTicks);
        m.put("chain-attack", chain);
        return m;
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
    // Gimmick — Chain Attack
    // ========================

    /** Master gimmick toggle. False disables every gimmick subsystem for the mode. */
    public boolean isGimmickEnabled() { return config.getBoolean("gimmick.enabled", true); }

    /** Whether the chain-attack gimmick is enabled (mobs fire leashing chains at players). */
    public boolean isChainAttackEnabled() { return config.getBoolean("gimmick.chain-attack.enabled", true); }

    /** Ticks of global cooldown applied to a player after being chained. Prevents chain-stun-lock. */
    public int getChainAttackGlobalCooldownTicks() {
        return config.getInt("gimmick.chain-attack.global-cooldown-per-player-ticks", 120);
    }

    /** Ticks of cooldown applied to a mob after it fires its chain. Prevents single-mob spam. */
    public int getChainAttackPerMobCooldownTicks() {
        return config.getInt("gimmick.chain-attack.per-mob-cooldown-ticks", 80);
    }

    /** Max distance (blocks) the chain can reach from a mob to a player. */
    public double getChainAttackDefaultReachRadius() {
        return config.getDouble("gimmick.chain-attack.default-reach-radius", 12.0);
    }

    /** Default effect type. One of PULL / SWING / SLAM / LAUNCH / ANCHOR / DAMAGE_ONLY. */
    public String getChainAttackDefaultEffect() {
        return config.getString("gimmick.chain-attack.default-effect", "PULL");
    }

    /** Default damage applied on chain attach (uses vanilla damage pipeline). */
    public double getChainAttackDefaultDamage() {
        return config.getDouble("gimmick.chain-attack.default-damage", 100.0);
    }

    /** Default duration (ticks) of the effect phase (pull / swing / etc.). */
    public int getChainAttackDefaultEffectDurationTicks() {
        return config.getInt("gimmick.chain-attack.default-effect-duration-ticks", 30);
    }

    /**
     * Per 4-tick scan chance the chain fires when a player is in reach and all
     * cooldowns are clear. 0.02 (2%) per 4t = roughly once per 5 seconds.
     */
    public double getChainProximityRollChance() {
        return config.getDouble("gimmick.chain-attack.proximity-roll-chance", 0.02);
    }

    /** One CHAIN BlockDisplay link per N blocks of distance (1.0 = link/block). */
    public double getChainLinkSpacing() {
        return config.getDouble("gimmick.chain-attack.chain-link-spacing", 1.0);
    }

    /** Visual cross-section scale of each chain link. */
    public double getChainLinkScale() {
        return config.getDouble("gimmick.chain-attack.chain-link-scale", 0.25);
    }

    /** PULL effect: velocity magnitude applied each tick toward the mob. */
    public double getChainPullStrength() {
        return config.getDouble("gimmick.chain-attack.pull-strength", 1.4);
    }

    /** SWING effect: angular speed (radians/tick) around the mob. */
    public double getChainSwingRateRadPerTick() {
        return config.getDouble("gimmick.chain-attack.swing-rate-rad-per-tick", 0.25);
    }

    /** SWING effect: orbital radius (blocks). */
    public double getChainSwingRadius() {
        return config.getDouble("gimmick.chain-attack.swing-radius", 3.0);
    }

    /** SLAM effect: downward Y velocity applied at landing phase. */
    public double getChainSlamVelocityY() {
        return config.getDouble("gimmick.chain-attack.slam-velocity-y", -1.8);
    }

    /** LAUNCH effect: outward push magnitude (one-shot at effect start). */
    public double getChainLaunchStrength() {
        return config.getDouble("gimmick.chain-attack.launch-strength", 1.8);
    }

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

        // Mob spawning section — write the canonical block first, then
        // (a) flip enabled to true and tune chain-mode-appropriate caps,
        // (b) overwrite the .mobs list with our full pool including the
        //     per-mob chain-attack overrides MobSpawnConfig.writeDefaults
        //     doesn't itself know about.
        MobSpawnConfig.writeDefaults(defaults, buildDefaultMobSpawnEntries());
        defaults.set("mob-spawning.enabled", true);
        defaults.set("mob-spawning.spawn-interval-ticks", 200);
        defaults.set("mob-spawning.max-total-mobs", 12);
        defaults.set("mob-spawning.max-mobs-per-player", 4);
        defaults.set("mob-spawning.mobs", buildDefaultChainMobs());

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

        // Gimmick: Chain Attack
        defaults.set("gimmick.enabled", true);
        defaults.setComments("gimmick.enabled", List.of(
                "",
                "=== GIMMICK SUBSYSTEMS ===",
                "Master gimmick toggle. False disables the chain attack system entirely.",
                "When false, the gimmick.chain-attack.* keys below are ignored."));

        defaults.set("gimmick.chain-attack.enabled", true);
        defaults.setComments("gimmick.chain-attack.enabled", List.of(
                "",
                "--- Chain Attack ---",
                "Master toggle for the chain attack ability.",
                "When true, every mob spawned by Chain mode (via mob-spawning) can",
                "fire a leashing chain at nearby players with a configurable effect."));

        defaults.set("gimmick.chain-attack.global-cooldown-per-player-ticks", 120);
        defaults.setComments("gimmick.chain-attack.global-cooldown-per-player-ticks", List.of(
                "Cooldown applied to a player after being chained — no other mob can",
                "chain them until this expires. Prevents chain-stun-lock. 120 ticks = 6s."));

        defaults.set("gimmick.chain-attack.per-mob-cooldown-ticks", 80);
        defaults.setComments("gimmick.chain-attack.per-mob-cooldown-ticks", List.of(
                "Cooldown applied to the firing mob — same mob can't chain again until",
                "this expires. Prevents one mob from monopolizing a player. 80 ticks = 4s."));

        defaults.set("gimmick.chain-attack.default-reach-radius", 12.0);
        defaults.setComments("gimmick.chain-attack.default-reach-radius", List.of(
                "Maximum distance (blocks) the chain can reach from mob to player.",
                "Overridable per-mob in the mob-spawning.mobs list via chain-attack.reach-radius."));

        defaults.set("gimmick.chain-attack.default-effect", "PULL");
        defaults.setComments("gimmick.chain-attack.default-effect", List.of(
                "Default effect type when the chain hits. Options:",
                "  PULL          — yank player toward mob over duration",
                "  SWING         — orbit player around mob",
                "  SLAM          — pull then slam down on landing",
                "  LAUNCH        — throw player away from mob",
                "  ANCHOR        — pin player in place",
                "  DAMAGE_ONLY   — instant damage, no movement effect"));

        defaults.set("gimmick.chain-attack.default-damage", 100.0);
        defaults.setComments("gimmick.chain-attack.default-damage", List.of(
                "Default damage applied on chain attach (uses vanilla damage pipeline,",
                "respects armor and resistance effects)."));

        defaults.set("gimmick.chain-attack.default-effect-duration-ticks", 30);
        defaults.setComments("gimmick.chain-attack.default-effect-duration-ticks", List.of(
                "How many ticks the effect (pull / swing / etc.) lasts.",
                "30 ticks = 1.5 seconds of being yanked / orbited / pinned."));

        defaults.set("gimmick.chain-attack.proximity-roll-chance", 0.02);
        defaults.setComments("gimmick.chain-attack.proximity-roll-chance", List.of(
                "Per-tick chance the chain fires when a player is in reach AND cooldowns",
                "are clear. 0.02 (2%) per 4-tick check = roughly 1 fire per 5 seconds of",
                "the player being in range."));

        defaults.set("gimmick.chain-attack.chain-link-spacing", 1.0);
        defaults.setComments("gimmick.chain-attack.chain-link-spacing", List.of(
                "Visual: one CHAIN BlockDisplay link per N blocks of distance.",
                "1.0 = a chain link every block. Smaller values = denser visual."));

        defaults.set("gimmick.chain-attack.chain-link-scale", 0.25);
        defaults.setComments("gimmick.chain-attack.chain-link-scale", List.of(
                "Per-link visual scale (cross-section). 0.25 = quarter-block thick chain."));

        defaults.set("gimmick.chain-attack.pull-strength", 1.4);
        defaults.setComments("gimmick.chain-attack.pull-strength", List.of(
                "PULL effect: velocity magnitude applied toward the mob each tick.",
                "Higher = more violent yank. Vanilla movement velocity is ~0.2."));

        defaults.set("gimmick.chain-attack.swing-rate-rad-per-tick", 0.25);
        defaults.setComments("gimmick.chain-attack.swing-rate-rad-per-tick", List.of(
                "SWING effect: angular speed around the mob in radians per tick.",
                "2π ≈ 6.283. 0.25 rad/t = full revolution in ~25 ticks (1.25s)."));

        defaults.set("gimmick.chain-attack.swing-radius", 3.0);
        defaults.setComments("gimmick.chain-attack.swing-radius", List.of(
                "SWING effect: orbital radius (blocks) around the mob."));

        defaults.set("gimmick.chain-attack.slam-velocity-y", -1.8);
        defaults.setComments("gimmick.chain-attack.slam-velocity-y", List.of(
                "SLAM effect: downward Y velocity applied at the landing phase.",
                "Negative = downward. -1.8 = hard slam (fall-damage worthy)."));

        defaults.set("gimmick.chain-attack.launch-strength", 1.8);
        defaults.setComments("gimmick.chain-attack.launch-strength", List.of(
                "LAUNCH effect: outward push magnitude (one-shot at effect start).",
                "Direction = mob -> player, normalized."));

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
