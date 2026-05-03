package com.blockforge.chaoscraft.modes.fluffy;

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
 * Fluffy Mode configuration loader.
 * Reads from plugins/ChaosCraft/modes/fluffy/config.yml
 *
 * Fluffy Mode is a no-boss horror survival mode where cute-looking
 * cuddly creatures stalk and overwhelm the players. Random weighted
 * attack scheduler, custom Java mob AI, raining-cats-and-dogs spawner,
 * and version-aware world flora effects.
 */
public class FluffyConfig {

    private static final int CURRENT_CONFIG_VERSION = 1;

    private final ChaosCraftPlugin plugin;
    private final File configFile;
    private FileConfiguration config;

    public FluffyConfig(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        File modeDir = new File(plugin.getDataFolder(), "modes/fluffy");
        if (!modeDir.exists()) modeDir.mkdirs();
        this.configFile = new File(modeDir, "config.yml");
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
        if (!config.contains("scheduler.base-spawn-interval-ticks")) { config.set("scheduler.base-spawn-interval-ticks", 40); needsSave = true; }
        if (!config.contains("scheduler.spawn-offset-radius")) { config.set("scheduler.spawn-offset-radius", 8.0); needsSave = true; }
        if (!config.contains("scheduler.max-events-per-player")) { config.set("scheduler.max-events-per-player", 3); needsSave = true; }
        if (!config.contains("scheduler.type-weight-block-display")) { config.set("scheduler.type-weight-block-display", 1.0); needsSave = true; }
        if (!config.contains("scheduler.type-weight-environmental")) { config.set("scheduler.type-weight-environmental", 1.0); needsSave = true; }
        if (!config.contains("scheduler.type-weight-model-engine")) { config.set("scheduler.type-weight-model-engine", 1.0); needsSave = true; }

        // ── Mob spawning (shared section) ───────────────────────────────
        if (MobSpawnConfig.ensureKeys(config)) needsSave = true;

        // ── Custom AI ───────────────────────────────────────────────────
        if (!config.contains("mob-ai.enabled")) { config.set("mob-ai.enabled", true); needsSave = true; }
        if (!config.contains("mob-ai.tick-interval")) { config.set("mob-ai.tick-interval", 4); needsSave = true; }
        if (!config.contains("mob-ai.detection-range")) { config.set("mob-ai.detection-range", 14.0); needsSave = true; }
        if (!config.contains("mob-ai.attack-range")) { config.set("mob-ai.attack-range", 2.5); needsSave = true; }
        if (!config.contains("mob-ai.attack-cooldown-ticks")) { config.set("mob-ai.attack-cooldown-ticks", 40); needsSave = true; }
        if (!config.contains("mob-ai.flee-hp-percent")) { config.set("mob-ai.flee-hp-percent", 25); needsSave = true; }
        if (!config.contains("mob-ai.approach-speed-multiplier")) { config.set("mob-ai.approach-speed-multiplier", 1.1); needsSave = true; }
        if (!config.contains("mob-ai.animation-speed")) { config.set("mob-ai.animation-speed", 2.0); needsSave = true; }
        if (!config.contains("mob-ai.bunny.hop-y-offset")) { config.set("mob-ai.bunny.hop-y-offset", 0.35); needsSave = true; }
        if (!config.contains("mob-ai.bunny.hop-interval-ticks")) { config.set("mob-ai.bunny.hop-interval-ticks", 10); needsSave = true; }
        if (!config.contains("mob-ai.bear.speed-multiplier")) { config.set("mob-ai.bear.speed-multiplier", 0.7); needsSave = true; }
        if (!config.contains("mob-ai.bear.windup-ticks")) { config.set("mob-ai.bear.windup-ticks", 30); needsSave = true; }
        if (!config.contains("mob-ai.fox.circle-strafe-ticks")) { config.set("mob-ai.fox.circle-strafe-ticks", 20); needsSave = true; }
        if (!config.contains("mob-ai.fox.strafe-radius")) { config.set("mob-ai.fox.strafe-radius", 3.0); needsSave = true; }
        if (!config.contains("mob-ai.dog.pack-alert-range")) { config.set("mob-ai.dog.pack-alert-range", 10.0); needsSave = true; }
        if (!config.contains("mob-ai.bird.bob-amplitude")) { config.set("mob-ai.bird.bob-amplitude", 0.2); needsSave = true; }

        // ── Herd Pulse ──────────────────────────────────────────────────
        if (!config.contains("herd-pulse.enabled")) { config.set("herd-pulse.enabled", true); needsSave = true; }
        if (!config.contains("herd-pulse.interval-ticks")) { config.set("herd-pulse.interval-ticks", 1200); needsSave = true; }
        if (!config.contains("herd-pulse.pulse-duration-ticks")) { config.set("herd-pulse.pulse-duration-ticks", 40); needsSave = true; }

        // ── Rain from sky ───────────────────────────────────────────────
        if (!config.contains("rain-from-sky.enabled")) { config.set("rain-from-sky.enabled", true); needsSave = true; }
        if (!config.contains("rain-from-sky.spawns-per-window")) { config.set("rain-from-sky.spawns-per-window", 3); needsSave = true; }
        if (!config.contains("rain-from-sky.window-seconds")) { config.set("rain-from-sky.window-seconds", 8); needsSave = true; }
        if (!config.contains("rain-from-sky.drop-height")) { config.set("rain-from-sky.drop-height", 20); needsSave = true; }
        if (!config.contains("rain-from-sky.target-mode")) { config.set("rain-from-sky.target-mode", "near_players"); needsSave = true; }
        if (!config.contains("rain-from-sky.scatter-radius")) { config.set("rain-from-sky.scatter-radius", 12.0); needsSave = true; }
        if (!config.contains("rain-from-sky.max-falling-mobs")) { config.set("rain-from-sky.max-falling-mobs", 20); needsSave = true; }
        if (!config.contains("rain-from-sky.mobs")) {
            config.set("rain-from-sky.mobs", buildDefaultRainMobs());
            needsSave = true;
        }

        // ── World effects ───────────────────────────────────────────────
        if (!config.contains("world-effects.enabled")) { config.set("world-effects.enabled", true); needsSave = true; }
        if (!config.contains("world-effects.petal-weather.density")) { config.set("world-effects.petal-weather.density", 5); needsSave = true; }
        if (!config.contains("world-effects.petal-weather.spawn-y-offset")) { config.set("world-effects.petal-weather.spawn-y-offset", 25); needsSave = true; }
        if (!config.contains("world-effects.glowing-footprints.enabled")) { config.set("world-effects.glowing-footprints.enabled", true); needsSave = true; }
        if (!config.contains("world-effects.squeaky-tiles.enabled")) { config.set("world-effects.squeaky-tiles.enabled", true); needsSave = true; }
        if (!config.contains("world-effects.squeaky-tiles.chance")) { config.set("world-effects.squeaky-tiles.chance", 0.08); needsSave = true; }
        if (!config.contains("world-effects.singing-flowers.enabled")) { config.set("world-effects.singing-flowers.enabled", true); needsSave = true; }
        if (!config.contains("world-effects.singing-flowers.detection-radius")) { config.set("world-effects.singing-flowers.detection-radius", 3.0); needsSave = true; }
        if (!config.contains("world-effects.singing-flowers.chance")) { config.set("world-effects.singing-flowers.chance", 0.05); needsSave = true; }
        if (!config.contains("world-effects.glowing-eyes.enabled")) { config.set("world-effects.glowing-eyes.enabled", true); needsSave = true; }
        if (!config.contains("world-effects.glowing-eyes.first-fire-ticks")) { config.set("world-effects.glowing-eyes.first-fire-ticks", 600); needsSave = true; }
        if (!config.contains("world-effects.glowing-eyes.interval-ticks")) { config.set("world-effects.glowing-eyes.interval-ticks", 700); needsSave = true; }
        if (!config.contains("world-effects.adoptable-fluff.enabled")) { config.set("world-effects.adoptable-fluff.enabled", true); needsSave = true; }
        if (!config.contains("world-effects.adoptable-fluff.first-fire-ticks")) { config.set("world-effects.adoptable-fluff.first-fire-ticks", 600); needsSave = true; }
        if (!config.contains("world-effects.adoptable-fluff.interval-ticks")) { config.set("world-effects.adoptable-fluff.interval-ticks", 1200); needsSave = true; }
        if (!config.contains("world-effects.adoptable-fluff.lifetime-ticks")) { config.set("world-effects.adoptable-fluff.lifetime-ticks", 1200); needsSave = true; }
        if (!config.contains("world-effects.picnic-spot.enabled")) { config.set("world-effects.picnic-spot.enabled", true); needsSave = true; }
        if (!config.contains("world-effects.picnic-spot.first-fire-ticks")) { config.set("world-effects.picnic-spot.first-fire-ticks", 1600); needsSave = true; }
        if (!config.contains("world-effects.picnic-spot.interval-ticks")) { config.set("world-effects.picnic-spot.interval-ticks", 1600); needsSave = true; }
        if (!config.contains("world-effects.picnic-spot.lifetime-ticks")) { config.set("world-effects.picnic-spot.lifetime-ticks", 900); needsSave = true; }
        if (!config.contains("world-effects.picnic-spot.heal-radius")) { config.set("world-effects.picnic-spot.heal-radius", 3.0); needsSave = true; }
        if (!config.contains("world-effects.picnic-spot.heal-amount")) { config.set("world-effects.picnic-spot.heal-amount", 0.5); needsSave = true; }
        if (!config.contains("world-effects.picnic-spot.heal-interval-ticks")) { config.set("world-effects.picnic-spot.heal-interval-ticks", 20); needsSave = true; }
        if (!config.contains("world-effects.rainbow-arc.enabled")) { config.set("world-effects.rainbow-arc.enabled", true); needsSave = true; }
        if (!config.contains("world-effects.rainbow-arc.fire-tick")) { config.set("world-effects.rainbow-arc.fire-tick", 1800); needsSave = true; }
        if (!config.contains("world-effects.rainbow-arc.duration-ticks")) { config.set("world-effects.rainbow-arc.duration-ticks", 600); needsSave = true; }
        if (!config.contains("world-effects.flora-bloom.enabled")) { config.set("world-effects.flora-bloom.enabled", true); needsSave = true; }
        if (!config.contains("world-effects.flora-bloom.interval-ticks")) { config.set("world-effects.flora-bloom.interval-ticks", 600); needsSave = true; }
        if (!config.contains("world-effects.flora-bloom.small-chance")) { config.set("world-effects.flora-bloom.small-chance", 0.6); needsSave = true; }
        if (!config.contains("world-effects.flora-bloom.medium-chance")) { config.set("world-effects.flora-bloom.medium-chance", 0.3); needsSave = true; }
        if (!config.contains("world-effects.flora-bloom.large-chance")) { config.set("world-effects.flora-bloom.large-chance", 0.1); needsSave = true; }
        if (!config.contains("world-effects.flora-bloom.small-radius")) { config.set("world-effects.flora-bloom.small-radius", 5); needsSave = true; }
        if (!config.contains("world-effects.flora-bloom.medium-radius")) { config.set("world-effects.flora-bloom.medium-radius", 10); needsSave = true; }
        if (!config.contains("world-effects.flora-bloom.large-radius")) { config.set("world-effects.flora-bloom.large-radius", 20); needsSave = true; }
        if (!config.contains("world-effects.flora-bloom.revert-on-end")) { config.set("world-effects.flora-bloom.revert-on-end", true); needsSave = true; }

        // ── Ambient sounds ──────────────────────────────────────────────
        if (!config.contains("ambient-sounds.enabled")) { config.set("ambient-sounds.enabled", true); needsSave = true; }
        if (!config.contains("ambient-sounds.min-interval-ticks")) { config.set("ambient-sounds.min-interval-ticks", 80); needsSave = true; }
        if (!config.contains("ambient-sounds.max-interval-ticks")) { config.set("ambient-sounds.max-interval-ticks", 200); needsSave = true; }
        if (!config.contains("ambient-sounds.volume")) { config.set("ambient-sounds.volume", 0.4); needsSave = true; }
        if (!config.contains("ambient-sounds.sounds")) {
            config.set("ambient-sounds.sounds", Arrays.asList(
                    "ENTITY_CAT_PURR", "ENTITY_CAT_STRAY_AMBIENT",
                    "ENTITY_FOX_AMBIENT", "ENTITY_WOLF_GROWL"));
            needsSave = true;
        }

        // ── Music ───────────────────────────────────────────────────────
        if (!config.contains("music.track")) { config.set("music.track", "fluffy_main"); needsSave = true; }
        if (!config.contains("music.volume")) { config.set("music.volume", 0.6); needsSave = true; }

        // ── Rewards ─────────────────────────────────────────────────────
        if (!config.contains("rewards.survival-commands")) { config.set("rewards.survival-commands", new ArrayList<>()); needsSave = true; }

        if (needsSave) {
            save();
            config = YamlConfiguration.loadConfiguration(configFile);
        }
    }

    private List<Map<String, Object>> buildDefaultRainMobs() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(rainMob("CAT", 12, 1.5, 1.5));
        list.add(rainMob("WOLF", 8, 2.0, 1.8));
        list.add(rainMob("RABBIT", 10, 1.5, 1.5));
        list.add(rainMob("OCELOT", 6, 1.7, 1.6));
        list.add(rainMob("FOX", 5, 1.8, 1.7));
        list.add(rainMob("PARROT", 4, 1.5, 1.5));
        return list;
    }

    private Map<String, Object> rainMob(String id, int weight, double hp, double dmg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("type", "vanilla");
        m.put("weight", weight);
        m.put("min-count", 1);
        m.put("max-count", 1);
        m.put("health-multiplier", hp);
        m.put("damage-multiplier", dmg);
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
            plugin.getLogger().severe("Failed to save fluffy config: " + e.getMessage());
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

    public int getBaseSpawnInterval() { return config.getInt("scheduler.base-spawn-interval-ticks", 40); }
    public double getSpawnOffsetRadius() { return config.getDouble("scheduler.spawn-offset-radius", 8.0); }
    public int getMaxEventsPerPlayer() { return config.getInt("scheduler.max-events-per-player", 3); }
    public double getTypeWeightBlockDisplay() { return config.getDouble("scheduler.type-weight-block-display", 1.0); }
    public double getTypeWeightEnvironmental() { return config.getDouble("scheduler.type-weight-environmental", 1.0); }
    public double getTypeWeightModelEngine() { return config.getDouble("scheduler.type-weight-model-engine", 0.0); }

    // ========================
    // Mob AI
    // ========================

    public boolean isMobAiEnabled() { return config.getBoolean("mob-ai.enabled", true); }
    public int getMobAiTickInterval() { return config.getInt("mob-ai.tick-interval", 4); }
    public double getMobAiDetectionRange() { return config.getDouble("mob-ai.detection-range", 14.0); }
    public double getMobAiAttackRange() { return config.getDouble("mob-ai.attack-range", 2.5); }
    public int getMobAiAttackCooldownTicks() { return config.getInt("mob-ai.attack-cooldown-ticks", 40); }
    public int getMobAiFleeHpPercent() { return config.getInt("mob-ai.flee-hp-percent", 25); }
    public double getMobAiApproachSpeedMult() { return config.getDouble("mob-ai.approach-speed-multiplier", 1.1); }
    public double getMobAiAnimationSpeed() { return config.getDouble("mob-ai.animation-speed", 2.0); }
    public double getBunnyHopY() { return config.getDouble("mob-ai.bunny.hop-y-offset", 0.35); }
    public int getBunnyHopInterval() { return config.getInt("mob-ai.bunny.hop-interval-ticks", 10); }
    public double getBearSpeedMult() { return config.getDouble("mob-ai.bear.speed-multiplier", 0.7); }
    public int getBearWindupTicks() { return config.getInt("mob-ai.bear.windup-ticks", 30); }
    public int getFoxStrafeTicks() { return config.getInt("mob-ai.fox.circle-strafe-ticks", 20); }
    public double getFoxStrafeRadius() { return config.getDouble("mob-ai.fox.strafe-radius", 3.0); }
    public double getDogPackAlertRange() { return config.getDouble("mob-ai.dog.pack-alert-range", 10.0); }
    public double getBirdBobAmplitude() { return config.getDouble("mob-ai.bird.bob-amplitude", 0.2); }

    // ========================
    // Herd Pulse
    // ========================

    public boolean isHerdPulseEnabled() { return config.getBoolean("herd-pulse.enabled", true); }
    public int getHerdPulseInterval() { return config.getInt("herd-pulse.interval-ticks", 1200); }
    public int getHerdPulseDuration() { return config.getInt("herd-pulse.pulse-duration-ticks", 40); }

    // ========================
    // Rain from sky
    // ========================

    public boolean isRainEnabled() { return config.getBoolean("rain-from-sky.enabled", true); }
    public int getRainSpawnsPerWindow() { return config.getInt("rain-from-sky.spawns-per-window", 3); }
    public int getRainWindowSeconds() { return config.getInt("rain-from-sky.window-seconds", 8); }
    public int getRainDropHeight() { return config.getInt("rain-from-sky.drop-height", 20); }
    public String getRainTargetMode() { return config.getString("rain-from-sky.target-mode", "near_players"); }
    public double getRainScatterRadius() { return config.getDouble("rain-from-sky.scatter-radius", 12.0); }
    public int getRainMaxFallingMobs() { return config.getInt("rain-from-sky.max-falling-mobs", 20); }
    public List<Map<?, ?>> getRainMobsRaw() { return config.getMapList("rain-from-sky.mobs"); }

    // ========================
    // World effects
    // ========================

    public boolean isWorldEffectsEnabled() { return config.getBoolean("world-effects.enabled", true); }

    public int getPetalDensity() { return config.getInt("world-effects.petal-weather.density", 5); }
    public int getPetalSpawnYOffset() { return config.getInt("world-effects.petal-weather.spawn-y-offset", 25); }

    public boolean isFootprintsEnabled() { return config.getBoolean("world-effects.glowing-footprints.enabled", true); }

    public boolean isSqueakyTilesEnabled() { return config.getBoolean("world-effects.squeaky-tiles.enabled", true); }
    public double getSqueakyChance() { return config.getDouble("world-effects.squeaky-tiles.chance", 0.08); }

    public boolean isSingingFlowersEnabled() { return config.getBoolean("world-effects.singing-flowers.enabled", true); }
    public double getSingingFlowersRadius() { return config.getDouble("world-effects.singing-flowers.detection-radius", 3.0); }
    public double getSingingFlowersChance() { return config.getDouble("world-effects.singing-flowers.chance", 0.05); }

    public boolean isGlowingEyesEnabled() { return config.getBoolean("world-effects.glowing-eyes.enabled", true); }
    public int getGlowingEyesFirstFire() { return config.getInt("world-effects.glowing-eyes.first-fire-ticks", 600); }
    public int getGlowingEyesInterval() { return config.getInt("world-effects.glowing-eyes.interval-ticks", 700); }

    public boolean isAdoptableFluffEnabled() { return config.getBoolean("world-effects.adoptable-fluff.enabled", true); }
    public int getAdoptableFluffFirstFire() { return config.getInt("world-effects.adoptable-fluff.first-fire-ticks", 600); }
    public int getAdoptableFluffInterval() { return config.getInt("world-effects.adoptable-fluff.interval-ticks", 1200); }
    public int getAdoptableFluffLifetime() { return config.getInt("world-effects.adoptable-fluff.lifetime-ticks", 1200); }

    public boolean isPicnicEnabled() { return config.getBoolean("world-effects.picnic-spot.enabled", true); }
    public int getPicnicFirstFire() { return config.getInt("world-effects.picnic-spot.first-fire-ticks", 1600); }
    public int getPicnicInterval() { return config.getInt("world-effects.picnic-spot.interval-ticks", 1600); }
    public int getPicnicLifetime() { return config.getInt("world-effects.picnic-spot.lifetime-ticks", 900); }
    public double getPicnicHealRadius() { return config.getDouble("world-effects.picnic-spot.heal-radius", 3.0); }
    public double getPicnicHealAmount() { return config.getDouble("world-effects.picnic-spot.heal-amount", 0.5); }
    public int getPicnicHealInterval() { return config.getInt("world-effects.picnic-spot.heal-interval-ticks", 20); }

    public boolean isRainbowArcEnabled() { return config.getBoolean("world-effects.rainbow-arc.enabled", true); }
    public int getRainbowArcFireTick() { return config.getInt("world-effects.rainbow-arc.fire-tick", 1800); }
    public int getRainbowArcDuration() { return config.getInt("world-effects.rainbow-arc.duration-ticks", 600); }

    public boolean isFloraBloomEnabled() { return config.getBoolean("world-effects.flora-bloom.enabled", true); }
    public int getFloraBloomInterval() { return config.getInt("world-effects.flora-bloom.interval-ticks", 600); }
    public double getFloraBloomSmallChance() { return config.getDouble("world-effects.flora-bloom.small-chance", 0.6); }
    public double getFloraBloomMediumChance() { return config.getDouble("world-effects.flora-bloom.medium-chance", 0.3); }
    public double getFloraBloomLargeChance() { return config.getDouble("world-effects.flora-bloom.large-chance", 0.1); }
    public int getFloraBloomSmallRadius() { return config.getInt("world-effects.flora-bloom.small-radius", 5); }
    public int getFloraBloomMediumRadius() { return config.getInt("world-effects.flora-bloom.medium-radius", 10); }
    public int getFloraBloomLargeRadius() { return config.getInt("world-effects.flora-bloom.large-radius", 20); }
    public boolean isFloraBloomRevertOnEnd() { return config.getBoolean("world-effects.flora-bloom.revert-on-end", true); }

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

    public String getMusicTrack() { return config.getString("music.track", "fluffy_main"); }
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
                "Fluffy Mode configuration. Do not edit config-version manually."));

        defaults.set("duration-seconds", 180);
        defaults.setComments("duration-seconds", List.of(
                "Total mode duration in seconds. Default: 180 (3 minutes)."));

        defaults.set("arena-radius", 50);
        defaults.setComments("arena-radius", List.of(
                "Radius (in blocks) of the arena area used for spawn picking and",
                "boundary enforcement. Centered on the first online player at start."));

        defaults.set("enforce-boundary", true);
        defaults.setComments("enforce-boundary", List.of(
                "Whether to enforce arena boundaries by pushing players back inward."));

        defaults.set("exempt-players", new ArrayList<>());
        defaults.setComments("exempt-players", List.of(
                "Player names that receive ZERO damage from all attacks."));

        defaults.set("on-start-commands", new ArrayList<>());
        defaults.setComments("on-start-commands", List.of(
                "Console commands run when Fluffy Mode starts."));

        defaults.set("on-end-commands", new ArrayList<>());
        defaults.setComments("on-end-commands", List.of(
                "Console commands run when Fluffy Mode ends."));

        defaults.set("world", "");
        defaults.setComments("world", List.of(
                "World name where Fluffy Mode runs. Leave empty for the first loaded world."));

        // Scheduler
        defaults.set("scheduler.base-spawn-interval-ticks", 40);
        defaults.setComments("scheduler.base-spawn-interval-ticks", List.of(
                "",
                "=== ATTACK SCHEDULER ===",
                "Ticks between attack spawn attempts. 20 ticks = 1 second.",
                "40 = attempt every 2 seconds."));
        defaults.set("scheduler.spawn-offset-radius", 8.0);
        defaults.setComments("scheduler.spawn-offset-radius", List.of(
                "Max distance from the target player that attacks can spawn."));
        defaults.set("scheduler.max-events-per-player", 3);
        defaults.setComments("scheduler.max-events-per-player", List.of(
                "Maximum simultaneous active attacks per player."));
        defaults.set("scheduler.type-weight-block-display", 1.0);
        defaults.setComments("scheduler.type-weight-block-display", List.of(
                "Spawn weight for block-display attacks. Higher = more likely.",
                "All weights are relative — they do not need to sum to 1.0."));
        defaults.set("scheduler.type-weight-environmental", 1.0);
        defaults.setComments("scheduler.type-weight-environmental", List.of(
                "Spawn weight for environmental attacks."));
        defaults.set("scheduler.type-weight-model-engine", 1.0);
        defaults.setComments("scheduler.type-weight-model-engine", List.of(
                "Spawn weight for ModelEngine VFX attacks (20 cute/fluffy bbmodel skills).",
                "Set to 0.0 to disable ME attacks entirely."));

        // Mob spawning section
        MobSpawnConfig.writeDefaults(defaults, new ArrayList<>());

        // Mob AI
        defaults.set("mob-ai.enabled", true);
        defaults.setComments("mob-ai.enabled", List.of(
                "",
                "=== CUSTOM MOB AI ===",
                "Custom Java state-machine AI applied to MythicMobs / vanilla mobs",
                "tagged with the scoreboard tag 'fluffy:managed'.",
                "Set to false to fall back to default vanilla / MM AI."));
        defaults.set("mob-ai.tick-interval", 4);
        defaults.setComments("mob-ai.tick-interval", List.of(
                "Ticks between AI evaluations. 4 = every 0.2 sec (responsive but cheap)."));
        defaults.set("mob-ai.detection-range", 14.0);
        defaults.setComments("mob-ai.detection-range", List.of(
                "Range (blocks) at which a managed mob acquires a player target."));
        defaults.set("mob-ai.attack-range", 2.5);
        defaults.setComments("mob-ai.attack-range", List.of(
                "Range (blocks) at which a managed mob enters its ATTACK state."));
        defaults.set("mob-ai.attack-cooldown-ticks", 40);
        defaults.setComments("mob-ai.attack-cooldown-ticks", List.of(
                "Ticks between consecutive attack swings. 40 = 2 seconds."));
        defaults.set("mob-ai.flee-hp-percent", 25);
        defaults.setComments("mob-ai.flee-hp-percent", List.of(
                "HP percentage threshold at which a managed mob enters FLEE state.",
                "25 = flees below 25% health."));
        defaults.set("mob-ai.approach-speed-multiplier", 1.1);
        defaults.setComments("mob-ai.approach-speed-multiplier", List.of(
                "Pathfinder speed multiplier while approaching a player."));
        defaults.set("mob-ai.animation-speed", 2.0);
        defaults.setComments("mob-ai.animation-speed", List.of(
                "ModelEngine animation playback speed multiplier for managed mobs.",
                "1.0 = original speed, 2.0 = double speed (matches the modogs",
                "Geckolib animations being slow at native rate)."));
        defaults.set("mob-ai.bunny.hop-y-offset", 0.35);
        defaults.set("mob-ai.bunny.hop-interval-ticks", 10);
        defaults.setComments("mob-ai.bunny.hop-y-offset", List.of(
                "Bunny-type mobs gain a small upward boost every hop-interval-ticks",
                "while approaching, simulating a hopping gait."));
        defaults.set("mob-ai.bear.speed-multiplier", 0.7);
        defaults.set("mob-ai.bear.windup-ticks", 30);
        defaults.setComments("mob-ai.bear.speed-multiplier", List.of(
                "Bear-type mobs lumber slowly (0.7x), then telegraph their attack",
                "for windup-ticks before swinging."));
        defaults.set("mob-ai.fox.circle-strafe-ticks", 20);
        defaults.set("mob-ai.fox.strafe-radius", 3.0);
        defaults.setComments("mob-ai.fox.circle-strafe-ticks", List.of(
                "Fox-type mobs circle-strafe a target before attacking."));
        defaults.set("mob-ai.dog.pack-alert-range", 10.0);
        defaults.setComments("mob-ai.dog.pack-alert-range", List.of(
                "Dog-type mobs in ATTACK state alert other dogs within this range."));
        defaults.set("mob-ai.bird.bob-amplitude", 0.2);
        defaults.setComments("mob-ai.bird.bob-amplitude", List.of(
                "Bird-type mobs bob up/down by this amount per AI tick."));

        // Herd pulse
        defaults.set("herd-pulse.enabled", true);
        defaults.setComments("herd-pulse.enabled", List.of(
                "",
                "=== HERD PULSE ===",
                "Periodically all managed mobs freeze in place for a brief moment",
                "(setAI(false) for pulse-duration-ticks). The pause is unnerving",
                "and signals the herd is 'listening'."));
        defaults.set("herd-pulse.interval-ticks", 1200);
        defaults.setComments("herd-pulse.interval-ticks", List.of(
                "Ticks between herd pulse events. 1200 = once per minute."));
        defaults.set("herd-pulse.pulse-duration-ticks", 40);
        defaults.setComments("herd-pulse.pulse-duration-ticks", List.of(
                "How long the freeze lasts. 40 ticks = 2 seconds."));

        // Rain from sky
        defaults.set("rain-from-sky.enabled", true);
        defaults.setComments("rain-from-sky.enabled", List.of(
                "",
                "=== RAINING CATS AND DOGS ===",
                "Continuous rain of fluffy mobs falling from the sky onto the arena.",
                "Each falling mob is invulnerable + AI-disabled while in flight,",
                "then enables on landing. Adds a constant aerial-pressure feel."));
        defaults.set("rain-from-sky.spawns-per-window", 3);
        defaults.set("rain-from-sky.window-seconds", 8);
        defaults.setComments("rain-from-sky.spawns-per-window", List.of(
                "Mobs dropped per window-seconds window. 3 per 8s = ~1 every 2.6s."));
        defaults.set("rain-from-sky.drop-height", 20);
        defaults.setComments("rain-from-sky.drop-height", List.of(
                "Blocks above the highest block where each mob spawns."));
        defaults.set("rain-from-sky.target-mode", "near_players");
        defaults.setComments("rain-from-sky.target-mode", List.of(
                "How XZ drop coordinates are picked.",
                "Options:",
                "  near_players — drop near a random online player",
                "  random_arena — drop anywhere within arena-radius of arena center"));
        defaults.set("rain-from-sky.scatter-radius", 12.0);
        defaults.setComments("rain-from-sky.scatter-radius", List.of(
                "How far from the chosen target XZ each drop is scattered."));
        defaults.set("rain-from-sky.max-falling-mobs", 20);
        defaults.setComments("rain-from-sky.max-falling-mobs", List.of(
                "Hard cap on simultaneous in-flight (falling) mobs.",
                "When the cap is hit, drops are skipped until some land."));
        defaults.set("rain-from-sky.mobs", buildDefaultRainMobs());
        defaults.setComments("rain-from-sky.mobs", List.of(
                "Mobs that can spawn during rain-from-sky.",
                "Same format as the universal mob-spawning.mobs entries.",
                "id / type / weight / min-count / max-count / health-multiplier / damage-multiplier"));

        // World effects
        defaults.set("world-effects.enabled", true);
        defaults.setComments("world-effects.enabled", List.of(
                "",
                "=== WORLD EFFECTS ===",
                "Eight passive ambient effects that make the world feel cute-but-wrong.",
                "Each can be toggled individually."));
        defaults.set("world-effects.petal-weather.density", 5);
        defaults.set("world-effects.petal-weather.spawn-y-offset", 25);
        defaults.setComments("world-effects.petal-weather.density", List.of(
                "Petal-weather: cherry leaf + pink dust drifting downward across the arena.",
                "density = particles per tick. spawn-y-offset = blocks above arena center."));
        defaults.set("world-effects.glowing-footprints.enabled", true);
        defaults.setComments("world-effects.glowing-footprints.enabled", List.of(
                "Glowing-footprints: pink + enchant particles trail player movement."));
        defaults.set("world-effects.squeaky-tiles.enabled", true);
        defaults.set("world-effects.squeaky-tiles.chance", 0.08);
        defaults.setComments("world-effects.squeaky-tiles.enabled", List.of(
                "Squeaky-tiles: random chance to play a squeaky-toy sound when",
                "a player walks onto a new block. chance = 0.08 = 8% per step."));
        defaults.set("world-effects.singing-flowers.enabled", true);
        defaults.set("world-effects.singing-flowers.detection-radius", 3.0);
        defaults.set("world-effects.singing-flowers.chance", 0.05);
        defaults.setComments("world-effects.singing-flowers.enabled", List.of(
                "Singing-flowers: when a player is near placed flora, a soft note",
                "plays. detection-radius = scan distance, chance per tick per player."));
        defaults.set("world-effects.glowing-eyes.enabled", true);
        defaults.set("world-effects.glowing-eyes.first-fire-ticks", 600);
        defaults.set("world-effects.glowing-eyes.interval-ticks", 700);
        defaults.setComments("world-effects.glowing-eyes.enabled", List.of(
                "Glowing-eyes: pairs of red/green particle dots blink at the arena perimeter."));
        defaults.set("world-effects.adoptable-fluff.enabled", true);
        defaults.set("world-effects.adoptable-fluff.first-fire-ticks", 600);
        defaults.set("world-effects.adoptable-fluff.interval-ticks", 1200);
        defaults.set("world-effects.adoptable-fluff.lifetime-ticks", 1200);
        defaults.setComments("world-effects.adoptable-fluff.enabled", List.of(
                "Adoptable-fluff: a frozen, glowing vanilla cat / rabbit / fox spawns",
                "in the arena and despawns after lifetime-ticks. Decoy / bait."));
        defaults.set("world-effects.picnic-spot.enabled", true);
        defaults.set("world-effects.picnic-spot.first-fire-ticks", 1600);
        defaults.set("world-effects.picnic-spot.interval-ticks", 1600);
        defaults.set("world-effects.picnic-spot.lifetime-ticks", 900);
        defaults.set("world-effects.picnic-spot.heal-radius", 3.0);
        defaults.set("world-effects.picnic-spot.heal-amount", 0.5);
        defaults.set("world-effects.picnic-spot.heal-interval-ticks", 20);
        defaults.setComments("world-effects.picnic-spot.enabled", List.of(
                "Picnic-spot: a temporary heal zone marked by floating food displays.",
                "Players inside heal-radius regen heal-amount HP every heal-interval-ticks."));
        defaults.set("world-effects.rainbow-arc.enabled", true);
        defaults.set("world-effects.rainbow-arc.fire-tick", 1800);
        defaults.set("world-effects.rainbow-arc.duration-ticks", 600);
        defaults.setComments("world-effects.rainbow-arc.enabled", List.of(
                "Rainbow-arc: a one-shot 7-color particle arc across the sky."));
        defaults.set("world-effects.flora-bloom.enabled", true);
        defaults.set("world-effects.flora-bloom.interval-ticks", 600);
        defaults.set("world-effects.flora-bloom.small-chance", 0.6);
        defaults.set("world-effects.flora-bloom.medium-chance", 0.3);
        defaults.set("world-effects.flora-bloom.large-chance", 0.1);
        defaults.set("world-effects.flora-bloom.small-radius", 5);
        defaults.set("world-effects.flora-bloom.medium-radius", 10);
        defaults.set("world-effects.flora-bloom.large-radius", 20);
        defaults.set("world-effects.flora-bloom.revert-on-end", true);
        defaults.setComments("world-effects.flora-bloom.enabled", List.of(
                "Flora-bloom: random patches of flowers / wildflowers / fireflies sprout",
                "in the arena. small / medium / large radii are picked by chance.",
                "revert-on-end restores all replaced blocks when the mode ends."));

        // Ambient sounds
        defaults.set("ambient-sounds.enabled", true);
        defaults.set("ambient-sounds.min-interval-ticks", 80);
        defaults.set("ambient-sounds.max-interval-ticks", 200);
        defaults.set("ambient-sounds.volume", 0.4);
        defaults.set("ambient-sounds.sounds", Arrays.asList(
                "ENTITY_CAT_PURR", "ENTITY_CAT_STRAY_AMBIENT",
                "ENTITY_FOX_AMBIENT", "ENTITY_WOLF_GROWL"));
        defaults.setComments("ambient-sounds.enabled", List.of(
                "",
                "=== AMBIENT SOUNDS ===",
                "Random animal sound played at a random player at a random interval.",
                "Sounds are Bukkit Sound enum names (e.g. ENTITY_CAT_PURR)."));

        // Music
        defaults.set("music.track", "fluffy_main");
        defaults.set("music.volume", 0.6);
        defaults.setComments("music.track", List.of(
                "Music track id (registered via the resource pack / MusicManager).",
                "volume is a multiplier applied at playback (0.0–1.0)."));

        // Rewards
        defaults.set("rewards.survival-commands", new ArrayList<>());
        defaults.setComments("rewards.survival-commands", List.of(
                "Console commands run for each player who survives the mode.",
                "Use %player% for the player's name."));

        try {
            defaults.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create default fluffy config: " + e.getMessage());
        }
    }
}
