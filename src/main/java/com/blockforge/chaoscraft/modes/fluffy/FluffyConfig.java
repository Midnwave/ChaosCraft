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
 * Reads from plugins/ChaosCraft/modes/fluffy/fluffy.yml (shared with ModeConfig
 * — keys for both classes coexist in the same file, matching the pattern used
 * by every other mode in the codebase).
 *
 * Fluffy Mode is a no-boss horror survival mode where cute-looking
 * cuddly creatures stalk and overwhelm the players. Random weighted
 * attack scheduler, custom Java mob AI, raining-cats-and-dogs spawner,
 * and version-aware world flora effects.
 */
public class FluffyConfig {

    private static final int CURRENT_CONFIG_VERSION = 4;

    private final ChaosCraftPlugin plugin;
    private final File configFile;
    private FileConfiguration config;

    public FluffyConfig(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        File modeDir = new File(plugin.getDataFolder(), "modes/fluffy");
        if (!modeDir.exists()) modeDir.mkdirs();
        this.configFile = new File(modeDir, "fluffy.yml");
        // Auto-migration: if a legacy config.yml exists from a previous install
        // and no fluffy.yml exists yet, rename config.yml -> fluffy.yml so
        // existing user customisations are preserved.
        File legacyFile = new File(modeDir, "config.yml");
        if (legacyFile.exists() && !this.configFile.exists()) {
            try {
                java.nio.file.Files.move(legacyFile.toPath(), this.configFile.toPath());
                plugin.getLogger().info("[Fluffy] Migrated legacy config.yml -> fluffy.yml");
            } catch (Exception e) {
                plugin.getLogger().warning("[Fluffy] Failed to migrate config.yml: " + e.getMessage());
            }
        }
        load();
    }

    public void load() {
        if (!configFile.exists()) {
            createDefaults();
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        boolean needsSave = false;

        // ── Top-level ───────────────────────────────────────────────────
        int existingVersion = config.contains("config-version") ? config.getInt("config-version") : 0;
        if (!config.contains("config-version")) { config.set("config-version", CURRENT_CONFIG_VERSION); needsSave = true; }

        // Upgrade path: v2 → v3 — replace the pure-vanilla rain pool with the
        // curated MythicMobs-aware default pool. Only replace if the existing
        // pool has NO mythicmobs entries (i.e. user has not customised it).
        if (existingVersion > 0 && existingVersion < 3 && config.contains("rain-from-sky.mobs")) {
            List<Map<?, ?>> existingPool = config.getMapList("rain-from-sky.mobs");
            boolean hasMythicEntry = false;
            for (Map<?, ?> entry : existingPool) {
                Object t = entry.get("type");
                if (t != null && "mythicmobs".equalsIgnoreCase(t.toString())) {
                    hasMythicEntry = true;
                    break;
                }
            }
            if (!hasMythicEntry) {
                config.set("rain-from-sky.mobs", buildDefaultRainMobs());
                plugin.getLogger().info("[FluffyConfig] Upgraded rain-from-sky.mobs to v3 default pool (47 dogs + 7 cats + 3 squirrels + 6 vanilla backups).");
                needsSave = true;
            }
        }

        if (config.getInt("config-version") < CURRENT_CONFIG_VERSION) { config.set("config-version", CURRENT_CONFIG_VERSION); needsSave = true; }
        if (!config.contains("duration-seconds")) { config.set("duration-seconds", 180); needsSave = true; }
        if (!config.contains("difficulty-multiplier")) { config.set("difficulty-multiplier", 15.0); needsSave = true; }
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
        if (!config.contains("scheduler.type-weight-model-engine")) { config.set("scheduler.type-weight-model-engine", 1.0); needsSave = true; }

        // ── Mob spawning (shared section) ───────────────────────────────
        if (MobSpawnConfig.ensureKeys(config)) needsSave = true;

        // ── Custom AI ───────────────────────────────────────────────────
        if (!config.contains("mob-ai.enabled")) { config.set("mob-ai.enabled", true); needsSave = true; }
        if (!config.contains("mob-ai.tick-interval")) { config.set("mob-ai.tick-interval", 4); needsSave = true; }
        if (!config.contains("mob-ai.detection-range")) { config.set("mob-ai.detection-range", 14.0); needsSave = true; }
        if (!config.contains("mob-ai.attack-range")) { config.set("mob-ai.attack-range", 4.0); needsSave = true; }
        if (!config.contains("mob-ai.attack-cooldown-ticks")) { config.set("mob-ai.attack-cooldown-ticks", 8); needsSave = true; }
        if (!config.contains("mob-ai.flee-hp-percent")) { config.set("mob-ai.flee-hp-percent", 25); needsSave = true; }
        if (!config.contains("mob-ai.approach-speed-multiplier")) { config.set("mob-ai.approach-speed-multiplier", 1.5); needsSave = true; }
        if (!config.contains("mob-ai.animation-speed")) { config.set("mob-ai.animation-speed", 2.0); needsSave = true; }
        if (!config.contains("mob-ai.bunny.hop-y-offset")) { config.set("mob-ai.bunny.hop-y-offset", 0.35); needsSave = true; }
        if (!config.contains("mob-ai.bunny.hop-interval-ticks")) { config.set("mob-ai.bunny.hop-interval-ticks", 10); needsSave = true; }
        if (!config.contains("mob-ai.bear.speed-multiplier")) { config.set("mob-ai.bear.speed-multiplier", 0.7); needsSave = true; }
        if (!config.contains("mob-ai.bear.windup-ticks")) { config.set("mob-ai.bear.windup-ticks", 30); needsSave = true; }
        if (!config.contains("mob-ai.fox.circle-strafe-ticks")) { config.set("mob-ai.fox.circle-strafe-ticks", 20); needsSave = true; }
        if (!config.contains("mob-ai.fox.strafe-radius")) { config.set("mob-ai.fox.strafe-radius", 3.0); needsSave = true; }
        if (!config.contains("mob-ai.dog.pack-alert-range")) { config.set("mob-ai.dog.pack-alert-range", 10.0); needsSave = true; }
        if (!config.contains("mob-ai.bird.bob-amplitude")) { config.set("mob-ai.bird.bob-amplitude", 0.2); needsSave = true; }

        // ── Per-type attack roster (config-version 2) ───────────────────
        if (!config.contains("mob-ai.special-roll-on-attack")) { config.set("mob-ai.special-roll-on-attack", 0.55); needsSave = true; }

        // bunny specials
        if (!config.contains("mob-ai.bunny.tackle-damage-multiplier")) { config.set("mob-ai.bunny.tackle-damage-multiplier", 1.5); needsSave = true; }
        if (!config.contains("mob-ai.bunny.tackle-leap-y")) { config.set("mob-ai.bunny.tackle-leap-y", 0.5); needsSave = true; }
        if (!config.contains("mob-ai.bunny.tackle-leap-forward")) { config.set("mob-ai.bunny.tackle-leap-forward", 0.6); needsSave = true; }
        if (!config.contains("mob-ai.bunny.tackle-cooldown-ticks")) { config.set("mob-ai.bunny.tackle-cooldown-ticks", 100); needsSave = true; }
        if (!config.contains("mob-ai.bunny.multiply-chance")) { config.set("mob-ai.bunny.multiply-chance", 0.4); needsSave = true; }
        if (!config.contains("mob-ai.bunny.multiply-hp-percent")) { config.set("mob-ai.bunny.multiply-hp-percent", 60); needsSave = true; }
        if (!config.contains("mob-ai.bunny.multiply-spawn-count")) { config.set("mob-ai.bunny.multiply-spawn-count", 1); needsSave = true; }
        if (!config.contains("mob-ai.bunny.multiply-cooldown-ticks")) { config.set("mob-ai.bunny.multiply-cooldown-ticks", 300); needsSave = true; }
        if (!config.contains("mob-ai.bunny.multiply-child-hp-multiplier")) { config.set("mob-ai.bunny.multiply-child-hp-multiplier", 0.5); needsSave = true; }

        // bear specials
        if (!config.contains("mob-ai.bear.paw-cone-radius")) { config.set("mob-ai.bear.paw-cone-radius", 2.5); needsSave = true; }
        if (!config.contains("mob-ai.bear.paw-cone-angle-degrees")) { config.set("mob-ai.bear.paw-cone-angle-degrees", 120); needsSave = true; }
        if (!config.contains("mob-ai.bear.paw-damage-multiplier")) { config.set("mob-ai.bear.paw-damage-multiplier", 1.5); needsSave = true; }
        if (!config.contains("mob-ai.bear.paw-cooldown-ticks")) { config.set("mob-ai.bear.paw-cooldown-ticks", 80); needsSave = true; }
        if (!config.contains("mob-ai.bear.slam-radius")) { config.set("mob-ai.bear.slam-radius", 3.0); needsSave = true; }
        if (!config.contains("mob-ai.bear.slam-knockup-y")) { config.set("mob-ai.bear.slam-knockup-y", 0.7); needsSave = true; }
        if (!config.contains("mob-ai.bear.slam-damage-multiplier")) { config.set("mob-ai.bear.slam-damage-multiplier", 1.2); needsSave = true; }
        if (!config.contains("mob-ai.bear.slam-cooldown-ticks")) { config.set("mob-ai.bear.slam-cooldown-ticks", 100); needsSave = true; }

        // fox specials
        if (!config.contains("mob-ai.fox.pounce-distance")) { config.set("mob-ai.fox.pounce-distance", 4.0); needsSave = true; }
        if (!config.contains("mob-ai.fox.pounce-damage-multiplier")) { config.set("mob-ai.fox.pounce-damage-multiplier", 1.4); needsSave = true; }
        if (!config.contains("mob-ai.fox.pounce-y-velocity")) { config.set("mob-ai.fox.pounce-y-velocity", 0.5); needsSave = true; }
        if (!config.contains("mob-ai.fox.pounce-cooldown-ticks")) { config.set("mob-ai.fox.pounce-cooldown-ticks", 80); needsSave = true; }

        // dog specials
        if (!config.contains("mob-ai.dog.pack-lunge-speed-multiplier")) { config.set("mob-ai.dog.pack-lunge-speed-multiplier", 1.6); needsSave = true; }
        if (!config.contains("mob-ai.dog.pack-lunge-cooldown-ticks")) { config.set("mob-ai.dog.pack-lunge-cooldown-ticks", 120); needsSave = true; }

        // bird specials
        if (!config.contains("mob-ai.bird.dive-y-rise")) { config.set("mob-ai.bird.dive-y-rise", 3.0); needsSave = true; }
        if (!config.contains("mob-ai.bird.dive-y-drop-velocity")) { config.set("mob-ai.bird.dive-y-drop-velocity", -1.4); needsSave = true; }
        if (!config.contains("mob-ai.bird.dive-damage-multiplier")) { config.set("mob-ai.bird.dive-damage-multiplier", 1.4); needsSave = true; }
        if (!config.contains("mob-ai.bird.dive-cooldown-ticks")) { config.set("mob-ai.bird.dive-cooldown-ticks", 90); needsSave = true; }
        if (!config.contains("mob-ai.bird.buffet-range")) { config.set("mob-ai.bird.buffet-range", 2.0); needsSave = true; }
        if (!config.contains("mob-ai.bird.buffet-knockback-strength")) { config.set("mob-ai.bird.buffet-knockback-strength", 0.9); needsSave = true; }
        if (!config.contains("mob-ai.bird.buffet-damage-multiplier")) { config.set("mob-ai.bird.buffet-damage-multiplier", 0.5); needsSave = true; }
        if (!config.contains("mob-ai.bird.buffet-cooldown-ticks")) { config.set("mob-ai.bird.buffet-cooldown-ticks", 40); needsSave = true; }

        // fluffy_cat (NEW)
        if (!config.contains("mob-ai.fluffy_cat.wiggle-pounce-chance")) { config.set("mob-ai.fluffy_cat.wiggle-pounce-chance", 0.3); needsSave = true; }
        if (!config.contains("mob-ai.fluffy_cat.stalk-ticks")) { config.set("mob-ai.fluffy_cat.stalk-ticks", 20); needsSave = true; }
        if (!config.contains("mob-ai.fluffy_cat.wiggle-ticks")) { config.set("mob-ai.fluffy_cat.wiggle-ticks", 30); needsSave = true; }
        if (!config.contains("mob-ai.fluffy_cat.prepare-attack-ticks")) { config.set("mob-ai.fluffy_cat.prepare-attack-ticks", 14); needsSave = true; }
        if (!config.contains("mob-ai.fluffy_cat.pounce-distance")) { config.set("mob-ai.fluffy_cat.pounce-distance", 3.5); needsSave = true; }
        if (!config.contains("mob-ai.fluffy_cat.pounce-y-velocity")) { config.set("mob-ai.fluffy_cat.pounce-y-velocity", 0.55); needsSave = true; }
        if (!config.contains("mob-ai.fluffy_cat.pounce-damage-multiplier")) { config.set("mob-ai.fluffy_cat.pounce-damage-multiplier", 1.6); needsSave = true; }
        if (!config.contains("mob-ai.fluffy_cat.triple-swipe-chance")) { config.set("mob-ai.fluffy_cat.triple-swipe-chance", 0.25); needsSave = true; }
        if (!config.contains("mob-ai.fluffy_cat.triple-swipe-interval-ticks")) { config.set("mob-ai.fluffy_cat.triple-swipe-interval-ticks", 10); needsSave = true; }
        if (!config.contains("mob-ai.fluffy_cat.triple-swipe-damage-multiplier")) { config.set("mob-ai.fluffy_cat.triple-swipe-damage-multiplier", 0.8); needsSave = true; }
        if (!config.contains("mob-ai.fluffy_cat.special-cooldown-ticks")) { config.set("mob-ai.fluffy_cat.special-cooldown-ticks", 100); needsSave = true; }

        // fluffy_squirrel (NEW)
        if (!config.contains("mob-ai.fluffy_squirrel.jump-pounce-distance")) { config.set("mob-ai.fluffy_squirrel.jump-pounce-distance", 3.0); needsSave = true; }
        if (!config.contains("mob-ai.fluffy_squirrel.jump-pounce-y-velocity")) { config.set("mob-ai.fluffy_squirrel.jump-pounce-y-velocity", 0.45); needsSave = true; }
        if (!config.contains("mob-ai.fluffy_squirrel.jump-pounce-damage-multiplier")) { config.set("mob-ai.fluffy_squirrel.jump-pounce-damage-multiplier", 1.3); needsSave = true; }
        if (!config.contains("mob-ai.fluffy_squirrel.jump-pounce-cooldown-ticks")) { config.set("mob-ai.fluffy_squirrel.jump-pounce-cooldown-ticks", 50); needsSave = true; }
        if (!config.contains("mob-ai.fluffy_squirrel.dart-hp-percent")) { config.set("mob-ai.fluffy_squirrel.dart-hp-percent", 50); needsSave = true; }
        if (!config.contains("mob-ai.fluffy_squirrel.dart-hop-count")) { config.set("mob-ai.fluffy_squirrel.dart-hop-count", 3); needsSave = true; }
        if (!config.contains("mob-ai.fluffy_squirrel.dart-hop-distance")) { config.set("mob-ai.fluffy_squirrel.dart-hop-distance", 2.0); needsSave = true; }
        if (!config.contains("mob-ai.fluffy_squirrel.dart-hop-interval-ticks")) { config.set("mob-ai.fluffy_squirrel.dart-hop-interval-ticks", 6); needsSave = true; }
        if (!config.contains("mob-ai.fluffy_squirrel.dart-cooldown-ticks")) { config.set("mob-ai.fluffy_squirrel.dart-cooldown-ticks", 120); needsSave = true; }

        // ── Herd Pulse ──────────────────────────────────────────────────
        if (!config.contains("herd-pulse.enabled")) { config.set("herd-pulse.enabled", true); needsSave = true; }
        if (!config.contains("herd-pulse.interval-ticks")) { config.set("herd-pulse.interval-ticks", 1200); needsSave = true; }
        if (!config.contains("herd-pulse.pulse-duration-ticks")) { config.set("herd-pulse.pulse-duration-ticks", 40); needsSave = true; }

        // ── Rain from sky ───────────────────────────────────────────────
        if (!config.contains("rain-from-sky.enabled")) { config.set("rain-from-sky.enabled", true); needsSave = true; }
        if (!config.contains("rain-from-sky.spawns-per-window")) { config.set("rain-from-sky.spawns-per-window", 6); needsSave = true; }
        if (!config.contains("rain-from-sky.window-seconds")) { config.set("rain-from-sky.window-seconds", 6); needsSave = true; }
        if (!config.contains("rain-from-sky.drop-height")) { config.set("rain-from-sky.drop-height", 20); needsSave = true; }
        if (!config.contains("rain-from-sky.target-mode")) { config.set("rain-from-sky.target-mode", "near_players"); needsSave = true; }
        if (!config.contains("rain-from-sky.scatter-radius")) { config.set("rain-from-sky.scatter-radius", 12.0); needsSave = true; }
        if (!config.contains("rain-from-sky.max-falling-mobs")) { config.set("rain-from-sky.max-falling-mobs", 35); needsSave = true; }
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

    /**
     * Curated default rain pool: 47 dogs (one per breed) + 7 fluffy cats +
     * 3 fluffy squirrels + 6 vanilla backups = 63 weighted entries.
     *
     * Weight scheme:
     *   Small dogs:           8 (most common — fast pressure)
     *   Medium dogs:          6
     *   Large dogs:           3 (rare heavy hitters)
     *   Fluffy cats (6):      5
     *   Fluffy cat Fallen:    2 (boss-tier)
     *   Fluffy squirrels (3): 7 (frantic darters)
     *   Vanilla backup (6):   2 (low — fallback only)
     *
     * MM mobs use HP/damage multipliers of 1.0 (their YAMLs already set
     * proper baselines). Vanilla mobs get 1.5/1.5.
     */
    private List<Map<String, Object>> buildDefaultRainMobs() {
        List<Map<String, Object>> list = new ArrayList<>();

        // ── Small dogs (14, weight 8 each) ──────────────────────────────
        list.add(mmMob("DogBeagleTan", 8));
        list.add(mmMob("DogBasenjiBlack", 8));
        list.add(mmMob("DogBostonTerrierSeal", 8));
        list.add(mmMob("DogBullTerrierBlack", 8));
        list.add(mmMob("DogCockerSpanielBlack", 8));
        list.add(mmMob("DogDachshundBlackTan", 8));
        list.add(mmMob("DogCkCharlesSpanielBlenheim", 8));
        list.add(mmMob("DogItalianGreyhoundBlue", 8));
        list.add(mmMob("DogMiniPinscherRed", 8));
        list.add(mmMob("DogPugFawn", 8));
        list.add(mmMob("DogRussellTerrierTriBrown", 8));
        list.add(mmMob("DogScottishTerrierBlack", 8));
        list.add(mmMob("DogShibaInuRed", 8));
        list.add(mmMob("DogWhippetBlue", 8));

        // ── Medium dogs (28, weight 6 each) ─────────────────────────────
        list.add(mmMob("DogAiredaleTerrierMedium", 6));
        list.add(mmMob("DogAustralianShepherdBlack", 6));
        list.add(mmMob("DogAmericanFoxhoundMedium", 6));
        list.add(mmMob("DogBloodhoundRed", 6));
        list.add(mmMob("DogBorderCollieBlack", 6));
        list.add(mmMob("DogBoxerMedium", 6));
        list.add(mmMob("DogBulldogFawn", 6));
        list.add(mmMob("DogCardiganCorgiSable", 6));
        list.add(mmMob("DogCollieSable", 6));
        list.add(mmMob("DogDalmatianWhite", 6));
        list.add(mmMob("DogDobermanBlack", 6));
        list.add(mmMob("DogGermanShepherdStandard", 6));
        list.add(mmMob("DogGermanSpitzRed", 6));
        list.add(mmMob("DogGoldenRetrieverMedium", 6));
        list.add(mmMob("DogGreyhoundWhite", 6));
        list.add(mmMob("DogHuskyGray", 6));
        list.add(mmMob("DogIrishSetterMedium", 6));
        list.add(mmMob("DogLabRetrieverYellow", 6));
        list.add(mmMob("DogMudiBrown", 6));
        list.add(mmMob("DogNorwegianElkhoundMedium", 6));
        list.add(mmMob("DogPembrokeCorgiRed", 6));
        list.add(mmMob("DogPitBullBrown", 6));
        list.add(mmMob("DogPoodleBlack", 6));
        list.add(mmMob("DogRedboneCoonhoundRed", 6));
        list.add(mmMob("DogRottweilerMahogany", 6));
        list.add(mmMob("DogSchnauzerPepperSalt", 6));
        list.add(mmMob("DogShetlandSheepdogSable", 6));
        list.add(mmMob("DogTreeWalkHoundTricolor", 6));

        // ── Large dogs (5, weight 3 each) ───────────────────────────────
        list.add(mmMob("DogAlaskanMalamuteGray", 3));
        list.add(mmMob("DogBerneseMountainDogTan", 3));
        list.add(mmMob("DogGreatDaneFawn", 3));
        list.add(mmMob("DogMastiffFawn", 3));
        list.add(mmMob("DogSaintBernardBrown", 3));

        // ── Fluffy cats (7) — 6 regular @ weight 5, Fallen @ weight 2 ──
        list.add(mmMob("Fluffy_NocsyCat-Munchkin", 5));
        list.add(mmMob("Fluffy_NocsyCat-Bombay", 5));
        list.add(mmMob("Fluffy_NocsyCat-Siamese", 5));
        list.add(mmMob("Fluffy_NocsyCat-ScottishFold", 5));
        list.add(mmMob("Fluffy_NocsyCat-MaineCoon", 5));
        list.add(mmMob("Fluffy_NocsyCat-European", 5));
        list.add(mmMob("Fluffy_NocsyCat-Fallen", 2));

        // ── Fluffy squirrels (3, weight 7 each) ─────────────────────────
        list.add(mmMob("Fluffy_NogSquirrel-Brown", 7));
        list.add(mmMob("Fluffy_NogSquirrel-Gray", 7));
        list.add(mmMob("Fluffy_NogSquirrel-Red", 7));

        // ── Vanilla backups (6, weight 2 each, hp/dmg 1.5) ──────────────
        list.add(vanillaMob("CAT", 2));
        list.add(vanillaMob("WOLF", 2));
        list.add(vanillaMob("RABBIT", 2));
        list.add(vanillaMob("OCELOT", 2));
        list.add(vanillaMob("FOX", 2));
        list.add(vanillaMob("PARROT", 2));

        return list;
    }

    /** MythicMobs entry helper. Multipliers default to 1.0 (MM YAML sets HP/damage). */
    private Map<String, Object> mmMob(String id, int weight) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("type", "mythicmobs");
        m.put("weight", weight);
        m.put("min-count", 1);
        m.put("max-count", 1);
        m.put("health-multiplier", 1.0);
        m.put("damage-multiplier", 1.0);
        return m;
    }

    /** Vanilla entry helper. Multipliers default to 1.5/1.5 (vanilla baselines are weak). */
    private Map<String, Object> vanillaMob(String id, int weight) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("type", "vanilla");
        m.put("weight", weight);
        m.put("min-count", 1);
        m.put("max-count", 1);
        m.put("health-multiplier", 1.5);
        m.put("damage-multiplier", 1.5);
        return m;
    }

    /**
     * Build the equal-tier-weighted default pool for the mob-spawning ground
     * spawner. Each TIER (squirrels / cats / dogs / vanilla) gets ≈25% of
     * spawns regardless of how many entries are in it. So when a ground
     * spawn fires, players see variety across types — not dog-dominated just
     * because there are 47 dog entries.
     *
     * Tier balance:
     *   Squirrels  3 × weight 15 = 45  (≈ 25%)
     *   Cats       7 × weight 7  = 49  (≈ 27%)
     *   Dogs      47 × weight 1  = 47  (≈ 26%)
     *   Vanilla    6 × weight 7  = 42  (≈ 23%)
     */
    private List<MobSpawnConfig.MobSpawnDefaultEntry> buildDefaultMobSpawnEntries() {
        List<MobSpawnConfig.MobSpawnDefaultEntry> list = new ArrayList<>();

        // Squirrels — weight 15 each
        for (String id : new String[]{
                "Fluffy_NogSquirrel-Brown", "Fluffy_NogSquirrel-Gray", "Fluffy_NogSquirrel-Red"}) {
            list.add(new MobSpawnConfig.MobSpawnDefaultEntry(id, "mythicmobs", 15, 1, 1, 1.0, 1.0));
        }

        // Cats — weight 7 each (Fallen too — it's already tougher via the MM YAML)
        for (String id : new String[]{
                "Fluffy_NocsyCat-Munchkin", "Fluffy_NocsyCat-Bombay", "Fluffy_NocsyCat-Siamese",
                "Fluffy_NocsyCat-ScottishFold", "Fluffy_NocsyCat-MaineCoon", "Fluffy_NocsyCat-European",
                "Fluffy_NocsyCat-Fallen"}) {
            list.add(new MobSpawnConfig.MobSpawnDefaultEntry(id, "mythicmobs", 7, 1, 1, 1.0, 1.0));
        }

        // Dogs (47 variants, one per breed) — weight 1 each
        for (String id : new String[]{
                // Small (14)
                "DogBeagleTan", "DogBasenjiBlack", "DogBostonTerrierSeal", "DogBullTerrierBlack",
                "DogCockerSpanielBlack", "DogDachshundBlackTan", "DogCkCharlesSpanielBlenheim",
                "DogItalianGreyhoundBlue", "DogMiniPinscherRed", "DogPugFawn",
                "DogRussellTerrierTriBrown", "DogScottishTerrierBlack", "DogShibaInuRed", "DogWhippetBlue",
                // Medium (28)
                "DogAiredaleTerrierMedium", "DogAustralianShepherdBlack", "DogAmericanFoxhoundMedium",
                "DogBloodhoundRed", "DogBorderCollieBlack", "DogBoxerMedium", "DogBulldogFawn",
                "DogCardiganCorgiSable", "DogCollieSable", "DogDalmatianWhite", "DogDobermanBlack",
                "DogGermanShepherdStandard", "DogGermanSpitzRed", "DogGoldenRetrieverMedium",
                "DogGreyhoundWhite", "DogHuskyGray", "DogIrishSetterMedium", "DogLabRetrieverYellow",
                "DogMudiBrown", "DogNorwegianElkhoundMedium", "DogPembrokeCorgiRed", "DogPitBullBrown",
                "DogPoodleBlack", "DogRedboneCoonhoundRed", "DogRottweilerMahogany",
                "DogSchnauzerPepperSalt", "DogShetlandSheepdogSable", "DogTreeWalkHoundTricolor",
                // Large (5)
                "DogAlaskanMalamuteGray", "DogBerneseMountainDogTan", "DogGreatDaneFawn",
                "DogMastiffFawn", "DogSaintBernardBrown"}) {
            list.add(new MobSpawnConfig.MobSpawnDefaultEntry(id, "mythicmobs", 1, 1, 1, 1.0, 1.0));
        }

        // Vanilla — weight 7 each, 1.5×/1.5× HP/damage so they aren't trivially weak
        list.add(new MobSpawnConfig.MobSpawnDefaultEntry("CAT", "vanilla", 7, 1, 1, 1.5, 1.5));
        list.add(new MobSpawnConfig.MobSpawnDefaultEntry("WOLF", "vanilla", 7, 1, 1, 2.0, 1.8));
        list.add(new MobSpawnConfig.MobSpawnDefaultEntry("RABBIT", "vanilla", 7, 1, 1, 1.5, 1.5));
        list.add(new MobSpawnConfig.MobSpawnDefaultEntry("OCELOT", "vanilla", 7, 1, 1, 1.7, 1.6));
        list.add(new MobSpawnConfig.MobSpawnDefaultEntry("FOX", "vanilla", 7, 1, 1, 1.7, 1.7));
        list.add(new MobSpawnConfig.MobSpawnDefaultEntry("PARROT", "vanilla", 7, 1, 1, 1.5, 1.5));

        return list;
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
    public double getDifficultyMultiplier() { return config.getDouble("difficulty-multiplier", 15.0); }
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

    // ── Per-type specials (config-version 2) ────────────────────────
    public double getSpecialRollOnAttack() { return config.getDouble("mob-ai.special-roll-on-attack", 0.35); }

    // bunny
    public double getBunnyTackleDamageMult() { return config.getDouble("mob-ai.bunny.tackle-damage-multiplier", 1.5); }
    public double getBunnyTackleLeapY() { return config.getDouble("mob-ai.bunny.tackle-leap-y", 0.5); }
    public double getBunnyTackleLeapForward() { return config.getDouble("mob-ai.bunny.tackle-leap-forward", 0.6); }
    public int getBunnyTackleCooldownTicks() { return config.getInt("mob-ai.bunny.tackle-cooldown-ticks", 200); }
    public double getBunnyMultiplyChance() { return config.getDouble("mob-ai.bunny.multiply-chance", 0.4); }
    public int getBunnyMultiplyHpPercent() { return config.getInt("mob-ai.bunny.multiply-hp-percent", 60); }
    public int getBunnyMultiplySpawnCount() { return config.getInt("mob-ai.bunny.multiply-spawn-count", 1); }
    public int getBunnyMultiplyCooldownTicks() { return config.getInt("mob-ai.bunny.multiply-cooldown-ticks", 600); }
    public double getBunnyMultiplyChildHpMult() { return config.getDouble("mob-ai.bunny.multiply-child-hp-multiplier", 0.5); }

    // bear
    public double getBearPawConeRadius() { return config.getDouble("mob-ai.bear.paw-cone-radius", 2.5); }
    public double getBearPawConeAngleDeg() { return config.getDouble("mob-ai.bear.paw-cone-angle-degrees", 120); }
    public double getBearPawDamageMult() { return config.getDouble("mob-ai.bear.paw-damage-multiplier", 1.5); }
    public int getBearPawCooldownTicks() { return config.getInt("mob-ai.bear.paw-cooldown-ticks", 160); }
    public double getBearSlamRadius() { return config.getDouble("mob-ai.bear.slam-radius", 3.0); }
    public double getBearSlamKnockupY() { return config.getDouble("mob-ai.bear.slam-knockup-y", 0.7); }
    public double getBearSlamDamageMult() { return config.getDouble("mob-ai.bear.slam-damage-multiplier", 1.2); }
    public int getBearSlamCooldownTicks() { return config.getInt("mob-ai.bear.slam-cooldown-ticks", 200); }

    // fox
    public double getFoxPounceDistance() { return config.getDouble("mob-ai.fox.pounce-distance", 4.0); }
    public double getFoxPounceDamageMult() { return config.getDouble("mob-ai.fox.pounce-damage-multiplier", 1.4); }
    public double getFoxPounceYVel() { return config.getDouble("mob-ai.fox.pounce-y-velocity", 0.5); }
    public int getFoxPounceCooldownTicks() { return config.getInt("mob-ai.fox.pounce-cooldown-ticks", 160); }

    // dog
    public double getDogPackLungeSpeedMult() { return config.getDouble("mob-ai.dog.pack-lunge-speed-multiplier", 1.6); }
    public int getDogPackLungeCooldownTicks() { return config.getInt("mob-ai.dog.pack-lunge-cooldown-ticks", 240); }

    // bird
    public double getBirdDiveYRise() { return config.getDouble("mob-ai.bird.dive-y-rise", 3.0); }
    public double getBirdDiveYDropVel() { return config.getDouble("mob-ai.bird.dive-y-drop-velocity", -1.4); }
    public double getBirdDiveDamageMult() { return config.getDouble("mob-ai.bird.dive-damage-multiplier", 1.4); }
    public int getBirdDiveCooldownTicks() { return config.getInt("mob-ai.bird.dive-cooldown-ticks", 180); }
    public double getBirdBuffetRange() { return config.getDouble("mob-ai.bird.buffet-range", 2.0); }
    public double getBirdBuffetKnockback() { return config.getDouble("mob-ai.bird.buffet-knockback-strength", 0.9); }
    public double getBirdBuffetDamageMult() { return config.getDouble("mob-ai.bird.buffet-damage-multiplier", 0.5); }
    public int getBirdBuffetCooldownTicks() { return config.getInt("mob-ai.bird.buffet-cooldown-ticks", 80); }

    // fluffy_cat
    public double getCatWigglePounceChance() { return config.getDouble("mob-ai.fluffy_cat.wiggle-pounce-chance", 0.3); }
    public int getCatStalkTicks() { return config.getInt("mob-ai.fluffy_cat.stalk-ticks", 20); }
    public int getCatWiggleTicks() { return config.getInt("mob-ai.fluffy_cat.wiggle-ticks", 30); }
    public int getCatPrepareAttackTicks() { return config.getInt("mob-ai.fluffy_cat.prepare-attack-ticks", 14); }
    public double getCatPounceDistance() { return config.getDouble("mob-ai.fluffy_cat.pounce-distance", 3.5); }
    public double getCatPounceYVel() { return config.getDouble("mob-ai.fluffy_cat.pounce-y-velocity", 0.55); }
    public double getCatPounceDamageMult() { return config.getDouble("mob-ai.fluffy_cat.pounce-damage-multiplier", 1.6); }
    public double getCatTripleSwipeChance() { return config.getDouble("mob-ai.fluffy_cat.triple-swipe-chance", 0.25); }
    public int getCatTripleSwipeIntervalTicks() { return config.getInt("mob-ai.fluffy_cat.triple-swipe-interval-ticks", 10); }
    public double getCatTripleSwipeDamageMult() { return config.getDouble("mob-ai.fluffy_cat.triple-swipe-damage-multiplier", 0.8); }
    public int getCatSpecialCooldownTicks() { return config.getInt("mob-ai.fluffy_cat.special-cooldown-ticks", 200); }

    // fluffy_squirrel
    public double getSquirrelJumpPounceDistance() { return config.getDouble("mob-ai.fluffy_squirrel.jump-pounce-distance", 3.0); }
    public double getSquirrelJumpPounceYVel() { return config.getDouble("mob-ai.fluffy_squirrel.jump-pounce-y-velocity", 0.45); }
    public double getSquirrelJumpPounceDamageMult() { return config.getDouble("mob-ai.fluffy_squirrel.jump-pounce-damage-multiplier", 1.3); }
    public int getSquirrelJumpPounceCooldownTicks() { return config.getInt("mob-ai.fluffy_squirrel.jump-pounce-cooldown-ticks", 100); }
    public int getSquirrelDartHpPercent() { return config.getInt("mob-ai.fluffy_squirrel.dart-hp-percent", 50); }
    public int getSquirrelDartHopCount() { return config.getInt("mob-ai.fluffy_squirrel.dart-hop-count", 3); }
    public double getSquirrelDartHopDistance() { return config.getDouble("mob-ai.fluffy_squirrel.dart-hop-distance", 2.0); }
    public int getSquirrelDartHopIntervalTicks() { return config.getInt("mob-ai.fluffy_squirrel.dart-hop-interval-ticks", 6); }
    public int getSquirrelDartCooldownTicks() { return config.getInt("mob-ai.fluffy_squirrel.dart-cooldown-ticks", 240); }

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

        defaults.set("difficulty-multiplier", 15.0);
        defaults.setComments("difficulty-multiplier", List.of(
                "Global damage and aggression multiplier applied to:",
                "  - all attack damage (BlockDisplay/Environmental/ModelEngine attacks)",
                "  - mob AI bite damage",
                "  - rain mob HP and damage scaling",
                "Default 15.0 means everything hits ~15x harder than baseline.",
                "Set to 1.0 for vanilla baseline."));

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
        defaults.set("scheduler.base-spawn-interval-ticks", 25);
        defaults.setComments("scheduler.base-spawn-interval-ticks", List.of(
                "",
                "=== ATTACK SCHEDULER ===",
                "Ticks between attack spawn attempts. 20 ticks = 1 second.",
                "25 = attempt every 1.25 seconds."));
        defaults.set("scheduler.spawn-offset-radius", 8.0);
        defaults.setComments("scheduler.spawn-offset-radius", List.of(
                "Max distance from the target player that attacks can spawn."));
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
        defaults.set("scheduler.type-weight-model-engine", 1.0);
        defaults.setComments("scheduler.type-weight-model-engine", List.of(
                "Spawn weight for ModelEngine VFX attacks (20 cute/fluffy bbmodel skills).",
                "Set to 0.0 to disable ME attacks entirely."));

        // Mob spawning section — populated with equal-chance-per-type weights.
        // Squirrels (3) × 15 ≈ 25%, cats (7) × 7 ≈ 27%, dogs (47) × 1 ≈ 26%,
        // vanilla (6) × 7 ≈ 23%. So when the user enables ground spawning,
        // they get balanced variety instead of dogs dominating just because
        // there are 47 of them.
        MobSpawnConfig.writeDefaults(defaults, buildDefaultMobSpawnEntries());

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
        defaults.set("mob-ai.attack-range", 4.0);
        defaults.setComments("mob-ai.attack-range", List.of(
                "Range (blocks) at which a managed mob enters its ATTACK state."));
        defaults.set("mob-ai.attack-cooldown-ticks", 8);
        defaults.setComments("mob-ai.attack-cooldown-ticks", List.of(
                "Ticks between consecutive attack swings. 25 = 1.25 seconds."));
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

        // ── Per-type attack roster (config-version 2) ───────────────────
        defaults.set("mob-ai.special-roll-on-attack", 0.55);
        defaults.setComments("mob-ai.special-roll-on-attack", List.of(
                "",
                "=== PER-TYPE SPECIAL ATTACKS ===",
                "Each managed mob has a primary melee bite plus one or more",
                "specials gated by chance, cooldown, and (sometimes) HP threshold.",
                "When a mob enters ATTACK state, it rolls this chance once",
                "to decide whether to fire a special instead of plain melee."));

        // bunny specials
        defaults.set("mob-ai.bunny.tackle-damage-multiplier", 1.5);
        defaults.set("mob-ai.bunny.tackle-leap-y", 0.5);
        defaults.set("mob-ai.bunny.tackle-leap-forward", 0.6);
        defaults.set("mob-ai.bunny.tackle-cooldown-ticks", 100);
        defaults.set("mob-ai.bunny.multiply-chance", 0.4);
        defaults.set("mob-ai.bunny.multiply-hp-percent", 60);
        defaults.set("mob-ai.bunny.multiply-spawn-count", 1);
        defaults.set("mob-ai.bunny.multiply-cooldown-ticks", 300);
        // (cooldowns halved — see difficulty-multiplier comment block)
        defaults.set("mob-ai.bunny.multiply-child-hp-multiplier", 0.5);
        defaults.setComments("mob-ai.bunny.tackle-damage-multiplier", List.of(
                "Bunny specials: 'tackle' (leap with bonus damage on landing) and",
                "'multiply' (HP-gated — spawns 1-2 baby bunnies with halved HP).",
                "",
                "Damage multiplier applied to the bunny's ATTACK_DAMAGE on tackle landing.",
                "1.5 = 150% of normal bite damage."));
        defaults.setComments("mob-ai.bunny.tackle-leap-y", List.of(
                "Upward velocity added when the tackle launches.",
                "Bukkit velocity units (~0.42 = vanilla jump height)."));
        defaults.setComments("mob-ai.bunny.tackle-leap-forward", List.of(
                "Forward velocity added toward the target on tackle launch.",
                "Bukkit velocity units; combined with tackle-leap-y."));
        defaults.setComments("mob-ai.bunny.tackle-cooldown-ticks", List.of(
                "Ticks between consecutive tackles by the same bunny.",
                "20 ticks = 1s; 200 = 10s."));
        defaults.setComments("mob-ai.bunny.multiply-chance", List.of(
                "Probability (0.0-1.0) the bunny commits to multiply once HP gate passes.",
                "0.4 = 40% chance per eligible roll."));
        defaults.setComments("mob-ai.bunny.multiply-hp-percent", List.of(
                "HP threshold (0-100) below which the bunny becomes eligible to multiply.",
                "60 = only triggers when bunny is at or below 60% max HP."));
        defaults.setComments("mob-ai.bunny.multiply-spawn-count", List.of(
                "Number of baby bunnies spawned per successful multiply trigger."));
        defaults.setComments("mob-ai.bunny.multiply-cooldown-ticks", List.of(
                "Ticks before the same bunny can multiply again.",
                "20 ticks = 1s; 600 = 30s."));
        defaults.setComments("mob-ai.bunny.multiply-child-hp-multiplier", List.of(
                "Max HP of spawned children as a fraction of parent max HP.",
                "0.5 = babies have half the parent's max HP."));

        // bear specials
        defaults.set("mob-ai.bear.paw-cone-radius", 2.5);
        defaults.set("mob-ai.bear.paw-cone-angle-degrees", 120);
        defaults.set("mob-ai.bear.paw-damage-multiplier", 1.5);
        defaults.set("mob-ai.bear.paw-cooldown-ticks", 80);
        defaults.set("mob-ai.bear.slam-radius", 3.0);
        defaults.set("mob-ai.bear.slam-knockup-y", 0.7);
        defaults.set("mob-ai.bear.slam-damage-multiplier", 1.2);
        defaults.set("mob-ai.bear.slam-cooldown-ticks", 100);
        defaults.setComments("mob-ai.bear.paw-cone-radius", List.of(
                "Bear specials: 'paw_swipe' (frontal cone after windup) and",
                "'slam' (radial AoE with knockup at melee range).",
                "",
                "Reach of the paw_swipe frontal cone, in blocks.",
                "Players within this distance and inside the cone angle are hit."));
        defaults.setComments("mob-ai.bear.paw-cone-angle-degrees", List.of(
                "Total sweep arc of the paw_swipe cone, in degrees.",
                "120 = ±60° from the bear's facing direction."));
        defaults.setComments("mob-ai.bear.paw-damage-multiplier", List.of(
                "Damage multiplier applied to ATTACK_DAMAGE for each player hit by paw_swipe.",
                "1.5 = 150% of normal bite damage."));
        defaults.setComments("mob-ai.bear.paw-cooldown-ticks", List.of(
                "Ticks between paw_swipe specials by the same bear.",
                "20 ticks = 1s; 160 = 8s."));
        defaults.setComments("mob-ai.bear.slam-radius", List.of(
                "Radial AoE radius of the slam, in blocks.",
                "All players within this radius take damage and knockup."));
        defaults.setComments("mob-ai.bear.slam-knockup-y", List.of(
                "Upward velocity applied to players hit by slam.",
                "Bukkit velocity units (~0.42 = vanilla jump height)."));
        defaults.setComments("mob-ai.bear.slam-damage-multiplier", List.of(
                "Damage multiplier applied to ATTACK_DAMAGE for slam hits.",
                "1.2 = 120% of normal bite damage."));
        defaults.setComments("mob-ai.bear.slam-cooldown-ticks", List.of(
                "Ticks between slam specials by the same bear.",
                "20 ticks = 1s; 200 = 10s."));

        // fox specials
        defaults.set("mob-ai.fox.pounce-distance", 4.0);
        defaults.set("mob-ai.fox.pounce-damage-multiplier", 1.4);
        defaults.set("mob-ai.fox.pounce-y-velocity", 0.5);
        defaults.set("mob-ai.fox.pounce-cooldown-ticks", 80);
        defaults.setComments("mob-ai.fox.pounce-distance", List.of(
                "Fox special: 'pounce' — a forward lunge toward the target",
                "that deals damage on landing.",
                "",
                "Maximum forward lunge distance, in blocks.",
                "Used to scale forward velocity for the pounce."));
        defaults.setComments("mob-ai.fox.pounce-damage-multiplier", List.of(
                "Damage multiplier applied to ATTACK_DAMAGE on pounce landing.",
                "1.4 = 140% of normal bite damage."));
        defaults.setComments("mob-ai.fox.pounce-y-velocity", List.of(
                "Upward velocity component of the pounce launch.",
                "Bukkit velocity units (~0.42 = vanilla jump height)."));
        defaults.setComments("mob-ai.fox.pounce-cooldown-ticks", List.of(
                "Ticks between pounce specials by the same fox.",
                "20 ticks = 1s; 160 = 8s."));

        // dog specials
        defaults.set("mob-ai.dog.pack-lunge-speed-multiplier", 1.6);
        defaults.set("mob-ai.dog.pack-lunge-cooldown-ticks", 120);
        defaults.setComments("mob-ai.dog.pack-lunge-speed-multiplier", List.of(
                "Dog special: 'pack_lunge' — alerts every other dog within",
                "pack-alert-range to converge on the same target.",
                "",
                "Velocity multiplier applied to each dog's converge nudge.",
                "1.6 = 160% of normal approach speed during the lunge."));
        defaults.setComments("mob-ai.dog.pack-lunge-cooldown-ticks", List.of(
                "Ticks between pack_lunge calls by the same dog (the caller).",
                "20 ticks = 1s; 240 = 12s."));

        // bird specials
        defaults.set("mob-ai.bird.dive-y-rise", 3.0);
        defaults.set("mob-ai.bird.dive-y-drop-velocity", -1.4);
        defaults.set("mob-ai.bird.dive-damage-multiplier", 1.4);
        defaults.set("mob-ai.bird.dive-cooldown-ticks", 90);
        defaults.set("mob-ai.bird.buffet-range", 2.0);
        defaults.set("mob-ai.bird.buffet-knockback-strength", 0.9);
        defaults.set("mob-ai.bird.buffet-damage-multiplier", 0.5);
        defaults.set("mob-ai.bird.buffet-cooldown-ticks", 40);
        defaults.setComments("mob-ai.bird.dive-y-rise", List.of(
                "Bird specials: 'dive' (rise then plummet for AoE damage on impact)",
                "and 'wing_buffet' (close-range push with light damage).",
                "Bird picks dive 60% / buffet 40% when both are off cooldown.",
                "",
                "How many blocks the bird climbs above the target before plunging.",
                "Larger values = longer telegraph, more dramatic dive."));
        defaults.setComments("mob-ai.bird.dive-y-drop-velocity", List.of(
                "Downward velocity at the start of the plunge phase.",
                "Negative Bukkit velocity units; -1.4 = fast plummet."));
        defaults.setComments("mob-ai.bird.dive-damage-multiplier", List.of(
                "Damage multiplier applied to ATTACK_DAMAGE on dive impact.",
                "1.4 = 140% of normal peck damage."));
        defaults.setComments("mob-ai.bird.dive-cooldown-ticks", List.of(
                "Ticks between dive specials by the same bird.",
                "20 ticks = 1s; 180 = 9s."));
        defaults.setComments("mob-ai.bird.buffet-range", List.of(
                "Maximum range (blocks) at which wing_buffet can target a player.",
                "Buffet is suppressed if the player is farther than this."));
        defaults.setComments("mob-ai.bird.buffet-knockback-strength", List.of(
                "Horizontal push velocity applied to players hit by wing_buffet.",
                "Bukkit velocity units; 0.9 ≈ a strong shove."));
        defaults.setComments("mob-ai.bird.buffet-damage-multiplier", List.of(
                "Damage multiplier applied to ATTACK_DAMAGE on buffet hit.",
                "0.5 = 50% of normal peck damage (buffet is mostly a push)."));
        defaults.setComments("mob-ai.bird.buffet-cooldown-ticks", List.of(
                "Ticks between buffet specials by the same bird.",
                "20 ticks = 1s; 80 = 4s."));

        // fluffy_cat (NEW)
        defaults.set("mob-ai.fluffy_cat.wiggle-pounce-chance", 0.3);
        defaults.set("mob-ai.fluffy_cat.stalk-ticks", 20);
        defaults.set("mob-ai.fluffy_cat.wiggle-ticks", 30);
        defaults.set("mob-ai.fluffy_cat.prepare-attack-ticks", 14);
        defaults.set("mob-ai.fluffy_cat.pounce-distance", 3.5);
        defaults.set("mob-ai.fluffy_cat.pounce-y-velocity", 0.55);
        defaults.set("mob-ai.fluffy_cat.pounce-damage-multiplier", 1.6);
        defaults.set("mob-ai.fluffy_cat.triple-swipe-chance", 0.25);
        defaults.set("mob-ai.fluffy_cat.triple-swipe-interval-ticks", 10);
        defaults.set("mob-ai.fluffy_cat.triple-swipe-damage-multiplier", 0.8);
        defaults.set("mob-ai.fluffy_cat.special-cooldown-ticks", 100);
        defaults.setComments("mob-ai.fluffy_cat.wiggle-pounce-chance", List.of(
                "fluffy_cat specials: 'wiggle_pounce' (sit -> wiggling -> prepare ->",
                "pounce theatre, big damage on land) and 'triple_swipe' (3 quick hits).",
                "Vanilla CAT/OCELOT entities resolve to this type.",
                "",
                "Probability (0.0-1.0) the cat picks wiggle_pounce when a special fires.",
                "Normalized against triple-swipe-chance and a plain-melee fallback."));
        defaults.setComments("mob-ai.fluffy_cat.stalk-ticks", List.of(
                "Length of the sit_loop stalking phase before wiggling begins.",
                "20 ticks = 1s."));
        defaults.setComments("mob-ai.fluffy_cat.wiggle-ticks", List.of(
                "Length of the wiggling phase (anticipation).",
                "20 ticks = 1s; 30 = 1.5s."));
        defaults.setComments("mob-ai.fluffy_cat.prepare-attack-ticks", List.of(
                "Length of the prepare_attack phase right before the pounce launches.",
                "20 ticks = 1s; 14 = 0.7s."));
        defaults.setComments("mob-ai.fluffy_cat.pounce-distance", List.of(
                "Maximum forward lunge distance of the wiggle_pounce, in blocks.",
                "Used to scale forward velocity for the pounce."));
        defaults.setComments("mob-ai.fluffy_cat.pounce-y-velocity", List.of(
                "Upward velocity component of the pounce launch.",
                "Bukkit velocity units (~0.42 = vanilla jump height)."));
        defaults.setComments("mob-ai.fluffy_cat.pounce-damage-multiplier", List.of(
                "Damage multiplier applied to ATTACK_DAMAGE on pounce landing.",
                "1.6 = 160% of normal claw damage."));
        defaults.setComments("mob-ai.fluffy_cat.triple-swipe-chance", List.of(
                "Probability (0.0-1.0) the cat picks triple_swipe when a special fires.",
                "Normalized against wiggle-pounce-chance and a plain-melee fallback."));
        defaults.setComments("mob-ai.fluffy_cat.triple-swipe-interval-ticks", List.of(
                "Ticks between each of the three swipes in triple_swipe.",
                "20 ticks = 1s; 10 = 0.5s spacing → ~1s total."));
        defaults.setComments("mob-ai.fluffy_cat.triple-swipe-damage-multiplier", List.of(
                "Damage multiplier applied to ATTACK_DAMAGE per swipe (3 hits).",
                "0.8 = 80% per swipe → 240% total if all three connect."));
        defaults.setComments("mob-ai.fluffy_cat.special-cooldown-ticks", List.of(
                "Ticks between any special (wiggle_pounce or triple_swipe) by the same cat.",
                "20 ticks = 1s; 200 = 10s."));

        // fluffy_squirrel (NEW)
        defaults.set("mob-ai.fluffy_squirrel.jump-pounce-distance", 3.0);
        defaults.set("mob-ai.fluffy_squirrel.jump-pounce-y-velocity", 0.45);
        defaults.set("mob-ai.fluffy_squirrel.jump-pounce-damage-multiplier", 1.3);
        defaults.set("mob-ai.fluffy_squirrel.jump-pounce-cooldown-ticks", 50);
        defaults.set("mob-ai.fluffy_squirrel.dart-hp-percent", 50);
        defaults.set("mob-ai.fluffy_squirrel.dart-hop-count", 3);
        defaults.set("mob-ai.fluffy_squirrel.dart-hop-distance", 2.0);
        defaults.set("mob-ai.fluffy_squirrel.dart-hop-interval-ticks", 6);
        defaults.set("mob-ai.fluffy_squirrel.dart-cooldown-ticks", 120);
        defaults.setComments("mob-ai.fluffy_squirrel.jump-pounce-distance", List.of(
                "fluffy_squirrel specials: 'jump_pounce' (small forward leap with",
                "damage on landing) and 'dart' (HP-gated frantic teleport-hops",
                "ending in a bite). Only triggered via scoreboard tag",
                "'fluffy:type:fluffy_squirrel' — no vanilla equivalent.",
                "Note: hurt animations are not auto-played (no listener wired).",
                "",
                "Maximum forward leap distance for jump_pounce, in blocks.",
                "Used to scale forward velocity at launch."));
        defaults.setComments("mob-ai.fluffy_squirrel.jump-pounce-y-velocity", List.of(
                "Upward velocity component of the jump_pounce launch.",
                "Bukkit velocity units (~0.42 = vanilla jump height)."));
        defaults.setComments("mob-ai.fluffy_squirrel.jump-pounce-damage-multiplier", List.of(
                "Damage multiplier applied to ATTACK_DAMAGE on jump_pounce landing.",
                "1.3 = 130% of normal bite damage."));
        defaults.setComments("mob-ai.fluffy_squirrel.jump-pounce-cooldown-ticks", List.of(
                "Ticks between jump_pounce specials by the same squirrel.",
                "20 ticks = 1s; 100 = 5s."));
        defaults.setComments("mob-ai.fluffy_squirrel.dart-hp-percent", List.of(
                "HP threshold (0-100) below which the dart special becomes eligible.",
                "50 = only triggers when squirrel is at or below 50% max HP."));
        defaults.setComments("mob-ai.fluffy_squirrel.dart-hop-count", List.of(
                "Number of frantic teleport-hops in a single dart sequence",
                "before the final bite resolves."));
        defaults.setComments("mob-ai.fluffy_squirrel.dart-hop-distance", List.of(
                "Distance of each dart hop, in blocks.",
                "Hops are short, erratic teleport steps toward the target."));
        defaults.setComments("mob-ai.fluffy_squirrel.dart-hop-interval-ticks", List.of(
                "Ticks between each hop in the dart sequence.",
                "20 ticks = 1s; 6 ≈ 0.3s spacing → frantic feel."));
        defaults.setComments("mob-ai.fluffy_squirrel.dart-cooldown-ticks", List.of(
                "Ticks between dart specials by the same squirrel.",
                "20 ticks = 1s; 240 = 12s."));

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
        defaults.set("rain-from-sky.spawns-per-window", 6);
        defaults.set("rain-from-sky.window-seconds", 6);
        defaults.setComments("rain-from-sky.spawns-per-window", List.of(
                "Mobs dropped per window-seconds window. 6 per 6s = ~1 per second."));
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
        defaults.set("rain-from-sky.max-falling-mobs", 35);
        defaults.setComments("rain-from-sky.max-falling-mobs", List.of(
                "Hard cap on simultaneous in-flight (falling) mobs.",
                "When the cap is hit, drops are skipped until some land."));
        defaults.set("rain-from-sky.mobs", buildDefaultRainMobs());
        defaults.setComments("rain-from-sky.mobs", List.of(
                "Mobs that can spawn during rain-from-sky.",
                "Fully editable — add/remove/reweight any entry.",
                "Default pool (63 entries): 47 dogs (one per breed) + 7 fluffy cats",
                "+ 3 fluffy squirrels + 6 vanilla backups.",
                "",
                "Entry format:",
                "  id                  Mob type id (vanilla EntityType OR MythicMob id)",
                "  type                'vanilla' or 'mythicmobs'",
                "  weight              Relative pick weight (higher = more common)",
                "  min-count           Min mobs per spawn event (usually 1)",
                "  max-count           Max mobs per spawn event (usually 1)",
                "  health-multiplier   Multiplier on base HP (MM mobs use 1.0)",
                "  damage-multiplier   Multiplier on base damage (MM mobs use 1.0)",
                "",
                "Available MythicMob ids ship with FluffyMode:",
                "  - 168 dog variants — see plugins/MythicMobs/Mobs/modogs.yml",
                "    (Dog<Breed><Color> e.g. DogBeagleTan, DogPoodleSilver, ...)",
                "  - 7 cats — Fluffy_NocsyCat-{Munchkin,Bombay,Siamese,",
                "    ScottishFold,MaineCoon,European,Fallen}",
                "  - 3 squirrels — Fluffy_NogSquirrel-{Brown,Gray,Red}",
                "",
                "Add additional dog variants by copying the helper line and",
                "swapping the id; full list in modogs.yml."));

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
