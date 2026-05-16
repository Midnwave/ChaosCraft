package com.blockforge.chaoscraft.modes.freezingice;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.services.mobspawn.MobSpawnConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import org.bukkit.Material;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * FreezingIce Mode configuration loader.
 *
 * Reads from plugins/ChaosCraft/modes/freezingice/freezingice.yml.
 * Fresh-start Phase 1 config — no world-effects / herd-pulse / rain-from-sky
 * / mob-ai sections (those don't exist for FreezingIce yet).
 *
 * Keeps: config-version, duration-seconds, arena-radius, enforce-boundary,
 * exempt-players, on-start / on-end commands, world, scheduler section,
 * mob-spawning section, ambient-sounds, music, rewards.
 */
public class FreezingIceConfig {

    private static final int CURRENT_CONFIG_VERSION = 3;

    /** Default exempt-blocks list — see {@link #buildDefaultIcePhysicsExempt()}. */
    private static final List<String> DEFAULT_ICE_PHYSICS_EXEMPT = Arrays.asList(
            "LAVA", "WATER", "COBWEB", "SLIME_BLOCK", "SCAFFOLDING",
            "LADDER", "VINE", "HONEY_BLOCK", "POWDER_SNOW");

    private final ChaosCraftPlugin plugin;
    private final File configFile;
    private FileConfiguration config;

    public FreezingIceConfig(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        File modeDir = new File(plugin.getDataFolder(), "modes/freezingice");
        if (!modeDir.exists()) modeDir.mkdirs();
        this.configFile = new File(modeDir, "freezingice.yml");
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

        // ── Gimmick: Frostbite + Bonfire Network ────────────────────────
        if (!config.contains("gimmick.enabled")) { config.set("gimmick.enabled", true); needsSave = true; }
        if (!config.contains("gimmick.frostbite.cold-start-ticks-per-percent")) { config.set("gimmick.frostbite.cold-start-ticks-per-percent", 60); needsSave = true; }
        if (!config.contains("gimmick.frostbite.cold-end-ticks-per-percent")) { config.set("gimmick.frostbite.cold-end-ticks-per-percent", 15); needsSave = true; }
        if (!config.contains("gimmick.frostbite.warmth-recovery-ticks-per-percent")) { config.set("gimmick.frostbite.warmth-recovery-ticks-per-percent", 8); needsSave = true; }
        if (!config.contains("gimmick.frostbite.damage-at-100")) { config.set("gimmick.frostbite.damage-at-100", 6.0); needsSave = true; }
        if (!config.contains("gimmick.frostbite.damage-interval-ticks")) { config.set("gimmick.frostbite.damage-interval-ticks", 20); needsSave = true; }
        if (!config.contains("gimmick.frostbite.damage-ignores-armor")) { config.set("gimmick.frostbite.damage-ignores-armor", true); needsSave = true; }
        if (!config.contains("gimmick.bonfire.min-active")) { config.set("gimmick.bonfire.min-active", 3); needsSave = true; }
        if (!config.contains("gimmick.bonfire.max-active")) { config.set("gimmick.bonfire.max-active", 5); needsSave = true; }
        if (!config.contains("gimmick.bonfire.lifetime-ticks")) { config.set("gimmick.bonfire.lifetime-ticks", 600); needsSave = true; }
        if (!config.contains("gimmick.bonfire.heat-radius")) { config.set("gimmick.bonfire.heat-radius", 5.0); needsSave = true; }
        if (!config.contains("gimmick.bonfire.respawn-min-delay-ticks")) { config.set("gimmick.bonfire.respawn-min-delay-ticks", 80); needsSave = true; }
        if (!config.contains("gimmick.bonfire.particle-density")) { config.set("gimmick.bonfire.particle-density", 1.0); needsSave = true; }
        if (!config.contains("gimmick.bonfire.beam-height")) { config.set("gimmick.bonfire.beam-height", 30); needsSave = true; }

        // ── Gimmick: Global Ice Physics ─────────────────────────────────
        if (!config.contains("gimmick.ice-physics.enabled")) { config.set("gimmick.ice-physics.enabled", true); needsSave = true; }
        if (!config.contains("gimmick.ice-physics.friction")) { config.set("gimmick.ice-physics.friction", 0.93); needsSave = true; }
        if (!config.contains("gimmick.ice-physics.exempt-blocks")) {
            config.set("gimmick.ice-physics.exempt-blocks", buildDefaultIcePhysicsExempt());
            needsSave = true;
        }

        // ── Music ───────────────────────────────────────────────────────
        if (!config.contains("music.track")) { config.set("music.track", "freezingice_main"); needsSave = true; }
        if (!config.contains("music.volume")) { config.set("music.volume", 0.6); needsSave = true; }

        // ── Rewards ─────────────────────────────────────────────────────
        if (!config.contains("rewards.survival-commands")) { config.set("rewards.survival-commands", new ArrayList<>()); needsSave = true; }

        if (needsSave) {
            save();
            config = YamlConfiguration.loadConfiguration(configFile);
        }
    }

    /** Cold-themed ambient sounds — glass chime, ice break, frost step, etc. */
    private List<String> buildDefaultAmbientSounds() {
        return Arrays.asList(
                "ENTITY_GLOW_SQUID_AMBIENT",
                "BLOCK_AMETHYST_BLOCK_CHIME",
                "ITEM_ARMOR_EQUIP_ICE",
                "BLOCK_GLASS_BREAK",
                "BLOCK_POWDER_SNOW_BREAK",
                "BLOCK_NOTE_BLOCK_CHIME");
    }

    /** Empty mob pool by default — FreezingIce ground spawner is opt-in. */
    private List<MobSpawnConfig.MobSpawnDefaultEntry> buildDefaultMobSpawnEntries() {
        return Collections.emptyList();
    }

    /**
     * Default exempt-blocks list for the global ice-physics override.
     * These blocks have movement mechanics (climbing, sinking, sticking,
     * bouncing, fluid drag) that would break or feel wrong if their
     * friction were globally overridden.
     */
    private List<String> buildDefaultIcePhysicsExempt() {
        return new ArrayList<>(DEFAULT_ICE_PHYSICS_EXEMPT);
    }

    /** Returns a MobSpawnConfig backed by this mode's YAML. */
    public MobSpawnConfig getMobSpawnConfig() {
        return new MobSpawnConfig(config);
    }

    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save freezingice config: " + e.getMessage());
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
    // Gimmick: Frostbite + Bonfire Network
    // ========================

    public boolean isGimmickEnabled() { return config.getBoolean("gimmick.enabled", true); }
    public int getFrostbiteColdStartTicksPerPercent() { return config.getInt("gimmick.frostbite.cold-start-ticks-per-percent", 60); }
    public int getFrostbiteColdEndTicksPerPercent() { return config.getInt("gimmick.frostbite.cold-end-ticks-per-percent", 15); }
    public int getWarmthRecoveryTicksPerPercent() { return config.getInt("gimmick.frostbite.warmth-recovery-ticks-per-percent", 8); }
    public double getFrostbiteDamageAt100() { return config.getDouble("gimmick.frostbite.damage-at-100", 6.0); }
    public int getFrostbiteDamageIntervalTicks() { return config.getInt("gimmick.frostbite.damage-interval-ticks", 20); }
    public boolean isFrostbiteDamageIgnoresArmor() { return config.getBoolean("gimmick.frostbite.damage-ignores-armor", true); }
    public int getBonfireMinCount() { return config.getInt("gimmick.bonfire.min-active", 3); }
    public int getBonfireMaxCount() { return config.getInt("gimmick.bonfire.max-active", 5); }
    public int getBonfireLifetimeTicks() { return config.getInt("gimmick.bonfire.lifetime-ticks", 600); }
    public double getBonfireHeatRadius() { return config.getDouble("gimmick.bonfire.heat-radius", 5.0); }
    public int getBonfireRespawnMinDelayTicks() { return config.getInt("gimmick.bonfire.respawn-min-delay-ticks", 80); }
    public double getBonfireParticleDensity() { return config.getDouble("gimmick.bonfire.particle-density", 1.0); }
    public int getBonfireBeamHeight() { return config.getInt("gimmick.bonfire.beam-height", 30); }

    // ── Global ice physics ────────────────────────────────────────
    public boolean isIcePhysicsEnabled() { return config.getBoolean("gimmick.ice-physics.enabled", true); }
    public double getIcePhysicsFriction() {
        double f = config.getDouble("gimmick.ice-physics.friction", 0.93);
        // Clamp to safe range — anything outside this is dangerous / nonsensical
        if (f < 0.6) f = 0.6;
        if (f > 0.999) f = 0.999;
        return f;
    }
    /**
     * Parse the exempt-blocks string list into a Set<Material>. Unknown names
     * are skipped with a warning. Returns an empty set if the list is absent.
     */
    public Set<Material> getIcePhysicsExemptBlocks() {
        List<String> raw = config.getStringList("gimmick.ice-physics.exempt-blocks");
        if (raw == null || raw.isEmpty()) {
            raw = DEFAULT_ICE_PHYSICS_EXEMPT;
        }
        Set<Material> out = EnumSet.noneOf(Material.class);
        Set<String> warned = new LinkedHashSet<>();
        for (String name : raw) {
            if (name == null || name.isBlank()) continue;
            try {
                Material m = Material.valueOf(name.trim().toUpperCase());
                out.add(m);
            } catch (IllegalArgumentException ex) {
                if (warned.add(name)) {
                    plugin.getLogger().warning("[FreezingIce] Unknown ice-physics exempt-blocks entry: " + name);
                }
            }
        }
        return out;
    }

    // ========================
    // Music
    // ========================

    public String getMusicTrack() { return config.getString("music.track", "freezingice_main"); }
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
                "FreezingIce Mode configuration. Do not edit config-version manually.",
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
                "Player names that receive ZERO damage from all FreezingIce attacks.",
                "Add admins / spectators here while testing."));

        defaults.set("on-start-commands", new ArrayList<>());
        defaults.setComments("on-start-commands", List.of(
                "Console commands run when FreezingIce Mode starts.",
                "Use %player% to substitute online player names if needed."));

        defaults.set("on-end-commands", new ArrayList<>());
        defaults.setComments("on-end-commands", List.of(
                "Console commands run when FreezingIce Mode ends."));

        defaults.set("world", "");
        defaults.setComments("world", List.of(
                "World name where FreezingIce Mode runs. Leave empty for the first loaded world."));

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
                "Defaults to 0.0 — no ME attacks are registered for FreezingIce yet."));

        // Mob spawning section
        MobSpawnConfig.writeDefaults(defaults, buildDefaultMobSpawnEntries());

        // Ambient sounds
        defaults.set("ambient-sounds.enabled", true);
        defaults.setComments("ambient-sounds.enabled", List.of(
                "",
                "=== AMBIENT SOUNDS ===",
                "Random cold-themed sounds played at a random player at a random interval.",
                "Sounds are Bukkit Sound enum names (e.g. BLOCK_POWDER_SNOW_BREAK)."));
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
                "version — unknown names are skipped at runtime. Defaults are cold / glass",
                "themed (powder snow, amethyst chime, glass break, ice armor)."));

        // Gimmick: Frostbite + Bonfire Network
        defaults.set("gimmick.enabled", true);
        defaults.setComments("gimmick.enabled", List.of(
                "",
                "=== FROSTBITE + BONFIRE NETWORK GIMMICK ===",
                "Master toggle for the FreezingIce flagship mechanic. When true, every",
                "player in the FreezingIce world has a 0-100% frostbite meter that climbs",
                "while away from warmth and falls when near a bonfire OR on fire. At 100%,",
                "freeze-typed damage starts ticking. Visual feedback is the vanilla",
                "powder-snow freeze overlay — no titles or bossbars are shown.",
                "Bonfires spawn around the arena and expire after a lifetime, so players",
                "have to hunt them down. Attacks still spawn in bonfire areas — bonfires",
                "only protect against frostbite, not against attacks."));

        defaults.set("gimmick.frostbite.cold-start-ticks-per-percent", 60);
        defaults.setComments("gimmick.frostbite.cold-start-ticks-per-percent", List.of(
                "Ticks required to gain one frostbite % when the meter is near 0.",
                "20 ticks = 1 second. 60 = a slow climb at the start (5 seconds per % at 0%)."));
        defaults.set("gimmick.frostbite.cold-end-ticks-per-percent", 15);
        defaults.setComments("gimmick.frostbite.cold-end-ticks-per-percent", List.of(
                "Ticks required to gain one frostbite % when the meter is near 100.",
                "The rate linearly accelerates from cold-start-ticks toward this value as",
                "the meter climbs. 15 = a fast climb at the end (~0.75s per %)."));
        defaults.set("gimmick.frostbite.warmth-recovery-ticks-per-percent", 8);
        defaults.setComments("gimmick.frostbite.warmth-recovery-ticks-per-percent", List.of(
                "Constant ticks per % decrease while warm (in bonfire heat radius or on fire).",
                "8 = ~0.4s per % — gives players a clear incentive to seek warmth."));
        defaults.set("gimmick.frostbite.damage-at-100", 6.0);
        defaults.setComments("gimmick.frostbite.damage-at-100", List.of(
                "Damage dealt each interval once frostbite reaches 100%.",
                "Uses FREEZE damage type on Paper 1.20.5+ (correct sound + death message).",
                "6.0 = 3 hearts per hit; tune relative to player gear."));
        defaults.set("gimmick.frostbite.damage-interval-ticks", 20);
        defaults.setComments("gimmick.frostbite.damage-interval-ticks", List.of(
                "Ticks between freeze damage hits while the meter is at 100%.",
                "20 = once per second."));
        defaults.set("gimmick.frostbite.damage-ignores-armor", true);
        defaults.setComments("gimmick.frostbite.damage-ignores-armor", List.of(
                "If true, frostbite damage bypasses armor and enchantments by adjusting",
                "health directly after firing a tiny damage event (for sound + tagging).",
                "If false, damage is applied normally and may be reduced by armor."));

        defaults.set("gimmick.bonfire.min-active", 3);
        defaults.setComments("gimmick.bonfire.min-active", List.of(
                "Minimum number of bonfires always active in the arena.",
                "The system auto-spawns new ones as old ones expire."));
        defaults.set("gimmick.bonfire.max-active", 5);
        defaults.setComments("gimmick.bonfire.max-active", List.of(
                "Hard cap on simultaneous bonfires (admin /spawnbonfire can hit this).",
                "Auto-spawner never exceeds min-active on its own."));
        defaults.set("gimmick.bonfire.lifetime-ticks", 600);
        defaults.setComments("gimmick.bonfire.lifetime-ticks", List.of(
                "How long each bonfire lives before despawning. 20 ticks = 1 second.",
                "600 = 30 seconds. After this, the bonfire fades out and a new one",
                "spawns elsewhere — preventing players from camping a single safe spot."));
        defaults.set("gimmick.bonfire.heat-radius", 5.0);
        defaults.setComments("gimmick.bonfire.heat-radius", List.of(
                "Blocks. Players within this distance of a bonfire are 'warm'",
                "and their frostbite drops. Visualized in-world by an orange dust ring."));
        defaults.set("gimmick.bonfire.respawn-min-delay-ticks", 80);
        defaults.setComments("gimmick.bonfire.respawn-min-delay-ticks", List.of(
                "Minimum ticks between auto-spawn attempts after one succeeds.",
                "Prevents the system from flooding new bonfires the moment one expires."));
        defaults.set("gimmick.bonfire.particle-density", 1.0);
        defaults.setComments("gimmick.bonfire.particle-density", List.of(
                "Multiplier on per-bonfire particle counts (FLAME / SOUL_FIRE_FLAME / CRIT).",
                "1.0 = default; 0.5 = lower-end-PC friendly; 2.0 = extra spectacle."));
        defaults.set("gimmick.bonfire.beam-height", 30);
        defaults.setComments("gimmick.bonfire.beam-height", List.of(
                "Vertical height (blocks) of the END_ROD particle beam that marks each",
                "bonfire's location from across the arena. 30 = visible from far away.",
                "0 = no beam."));

        // Gimmick: Global Ice Physics (slippery world)
        defaults.set("gimmick.ice-physics.enabled", true);
        defaults.setComments("gimmick.ice-physics.enabled", List.of(
                "",
                "=== GLOBAL ICE PHYSICS ===",
                "When true, every non-exempt block in the registry has its friction",
                "field temporarily overridden while FreezingIce mode is active. Originals",
                "are captured on apply and restored exactly on mode end / plugin disable.",
                "Requires gimmick.enabled = true above."));
        defaults.set("gimmick.ice-physics.friction", 0.93);
        defaults.setComments("gimmick.ice-physics.friction", List.of(
                "Friction value applied to every block. Each tick a player keeps this",
                "fraction of horizontal velocity.",
                "Vanilla default = 0.6 (stop in <1s). Vanilla ice = 0.98 (skating rink).",
                "Recommended range 0.85-0.97. Going above 0.98 makes movement nearly",
                "uncontrollable. Clamped at runtime to [0.6, 0.999]."));
        defaults.set("gimmick.ice-physics.exempt-blocks", buildDefaultIcePhysicsExempt());
        defaults.setComments("gimmick.ice-physics.exempt-blocks", List.of(
                "Materials excluded from the global friction override. These blocks",
                "rely on non-friction movement mechanics (climbing, sticking, bouncing,",
                "fluid drag, sinking) that would break if overridden.",
                "Use Bukkit Material enum names. Unknown entries are skipped with a warning."));

        // Music
        defaults.set("music.track", "freezingice_main");
        defaults.setComments("music.track", List.of(
                "Music track id (registered via the resource pack / MusicManager).",
                "Default 'freezingice_main' is a placeholder — provide a real track id later.",
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
            plugin.getLogger().severe("Failed to create default freezingice config: " + e.getMessage());
        }
    }
}
