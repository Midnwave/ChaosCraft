package com.blockforge.chaoscraft.modes.fluffy.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.AbstractAttack;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackConfig;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackRegistry;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackType;
import com.blockforge.chaoscraft.modes.calamity.attacks.ModelEngineAttack;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;

/**
 * Fluffy Mode — MODELENGINE ATTACKS
 *
 * 20 ME VFX attacks themed around cute/fluffy menace:
 *  ground eruption  (1)  paw_slam_boop, (2) bunny_burrow_surprise, (3) yarn_ball_tangled
 *  AOE shockwave    (4) kitty_headbutt_love_bump, (5) teddy_hug_squeeze,
 *                   (6) sakura_petal_bloom, (7) rainbow_crash_full_spectrum
 *  projectile/beam  (8) rubber_duck_squeak, (9) candy_cannon_hyperglycemic,
 *                   (10) laser_pointer_fixation
 *  summon/orbital   (11) plush_dragon_summon, (12) teddy_giant_mr_cuddles,
 *                   (13) carousel_horse_merry_go, (14) cat_goddess_nine_lives,
 *                   (15) rabbit_army_multiplication, (16) duck_flock_armada,
 *                   (17) plushie_hydra_good_boy, (18) jack_in_box_surprise,
 *                   (19) butterfly_swarm_flutter, (20) candy_planet_sugar_rush
 *
 * Particle palette: CHERRY_LEAVES, ENCHANT, HEART, GLOW, SNOWFLAKE,
 *   DUST with pastel RGB (pink #ffc8d8, mint #b8ffd0, lavender #d8b8ff,
 *   coral #ffb8a0, butter #fff0b8, baby blue #b8e8ff, gold #ffd040).
 * Sounds: BLOCK_NOTE_BLOCK_*, ENTITY_CAT_PURR, ENTITY_CAT_AMBIENT,
 *   BLOCK_AMETHYST_BLOCK_CHIME, ENTITY_PLAYER_LEVELUP, ENTITY_FIREWORK_ROCKET_BLAST.
 *
 * All 20 bbmodel files live in src/main/resources/models/fluffy/me_attacks/.
 */
public final class FluffyModelEngine {
    private FluffyModelEngine() {}

    private static final String MODE_PATH = "modes/fluffy/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new PawSlamBoop(plugin));
        registry.register(new BunnyBurrowSurprise(plugin));
        registry.register(new YarnBallTangled(plugin));
        registry.register(new KittyHeadbuttLoveBump(plugin));
        registry.register(new TeddyHugSqueeze(plugin));
        registry.register(new SakuraPetalBloom(plugin));
        registry.register(new RainbowCrashFullSpectrum(plugin));
        registry.register(new RubberDuckSqueak(plugin));
        registry.register(new CandyCannonHyperglycemic(plugin));
        registry.register(new LaserPointerFixation(plugin));
        registry.register(new PlushDragonSummon(plugin));
        registry.register(new TeddyGiantMrCuddles(plugin));
        registry.register(new CarouselHorseMerryGo(plugin));
        registry.register(new CatGoddessNineLives(plugin));
        registry.register(new RabbitArmyMultiplication(plugin));
        registry.register(new DuckFlockArmada(plugin));
        registry.register(new PlushieHydraGoodBoy(plugin));
        registry.register(new JackInBoxSurprise(plugin));
        registry.register(new ButterflySwarmFlutter(plugin));
        registry.register(new CandyPlanetSugarRush(plugin));
    }

    /** Pastel pink dust at center for ambient cute effect. */
    private static void pastelPinkDust(Location c, int count, double spread) {
        if (c == null || c.getWorld() == null) return;
        c.getWorld().spawnParticle(Particle.DUST, c, count, spread, spread, spread, 0,
                new Particle.DustOptions(Color.fromRGB(0xff, 0xc8, 0xd8), 1.4f));
    }

    private static void mintDust(Location c, int count, double spread) {
        if (c == null || c.getWorld() == null) return;
        c.getWorld().spawnParticle(Particle.DUST, c, count, spread, spread, spread, 0,
                new Particle.DustOptions(Color.fromRGB(0xb8, 0xff, 0xd0), 1.4f));
    }

    private static void lavenderDust(Location c, int count, double spread) {
        if (c == null || c.getWorld() == null) return;
        c.getWorld().spawnParticle(Particle.DUST, c, count, spread, spread, spread, 0,
                new Particle.DustOptions(Color.fromRGB(0xd8, 0xb8, 0xff), 1.4f));
    }

    // ============================================================
    // 1. GIANT PAW SLAM — Boop
    // Impact-only ground slam. Big radius, single big damage at impact.
    // ============================================================
    public static class PawSlamBoop extends ModelEngineAttack {
        public PawSlamBoop(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("paw_slam_boop", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(0.0); config.setDamageRadius(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(390.0); config.setImpactRadius(5.5);
            config.setDurationTicks(80); config.setCooldownTicks(180);
        }
        @Override protected String getModelId() { return "paw_slam_boop"; }
        @Override protected double getModelScale() { return 2.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.2f, 0.4f);
            pastelPinkDust(c.clone().add(0, 12, 0), 30, 2.5);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null) return;
            if (tick == 15) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_BIG_FALL, 1.5f, 0.3f);
                triggerImpactDamage(c);
                if (c.getWorld() != null) {
                    c.getWorld().spawnParticle(Particle.HEART, c.clone().add(0, 1, 0), 12, 3, 0.5, 3, 0);
                    pastelPinkDust(c.clone().add(0, 0.5, 0), 40, 4);
                }
            }
        }
        @Override public AbstractAttack newInstance() { return new PawSlamBoop(plugin); }
    }

    // ============================================================
    // 2. BUNNY BURROW ERUPTION — Surprise
    // Ring of bunny ears erupting. Persistent damage zone for duration.
    // ============================================================
    public static class BunnyBurrowSurprise extends ModelEngineAttack {
        public BunnyBurrowSurprise(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("bunny_burrow_surprise", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(120.0); config.setDamageRadius(12.0);
            config.setModelengineScale("12.0");
            config.setTicksBetweenDamage(12); config.setDamageDelayTicks(4);
            config.setDurationTicks(180); config.setCooldownTicks(140);
        }
        @Override protected String getModelId() { return "bunny_burrow_surprise"; }
        @Override protected double getModelScale() { return 1.6; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRAVEL_BREAK, 0.9f, 1.4f);
            DisplayBuilder.playSound(c, Sound.ENTITY_RABBIT_JUMP, 1.0f, 1.6f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 20 == 0) {
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * 2 * i / 8;
                    Location ear = c.clone().add(Math.cos(a) * 8, 1.5, Math.sin(a) * 8);
                    pastelPinkDust(ear, 4, 0.4);
                }
            }
        }
        @Override public AbstractAttack newInstance() { return new BunnyBurrowSurprise(plugin); }
    }

    // ============================================================
    // 3. YARN BALL IMPACT — Tangled
    // Ball drops + bounces + strands. Impact damage at landing, strand zone after.
    // ============================================================
    public static class YarnBallTangled extends ModelEngineAttack {
        public YarnBallTangled(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("yarn_ball_tangled", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(105.0); config.setDamageRadius(8.0);
            config.setModelengineScale("8.0");
            config.setTicksBetweenDamage(12); config.setDamageDelayTicks(10);
            config.setDurationTicks(120); config.setCooldownTicks(130);
            config.setImpactDamage(300.0); config.setImpactRadius(4.0);
        }
        @Override protected String getModelId() { return "yarn_ball_tangled"; }
        @Override protected double getModelScale() { return 1.8; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_SLIME_SQUISH, 1.2f, 1.2f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick == 16) {
                DisplayBuilder.playSound(c, Sound.ENTITY_SLIME_JUMP, 1.4f, 0.8f);
                triggerImpactDamage(c);
                pastelPinkDust(c, 60, 4);
            }
            if (tick > 20 && tick % 8 == 0) {
                c.getWorld().spawnParticle(Particle.CHERRY_LEAVES, c.clone().add(0, 1, 0), 6, 3, 0.5, 3, 0);
            }
        }
        @Override public AbstractAttack newInstance() { return new YarnBallTangled(plugin); }
    }

    // ============================================================
    // 4. KITTY HEADBUTT NOVA — Love Bump
    // Ring shockwave AOE with HEART particles.
    // ============================================================
    public static class KittyHeadbuttLoveBump extends ModelEngineAttack {
        public KittyHeadbuttLoveBump(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("kitty_headbutt_love_bump", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(135.0); config.setDamageRadius(15.0);
            config.setModelengineScale("15.0");
            config.setTicksBetweenDamage(12); config.setDamageDelayTicks(2);
            config.setDurationTicks(140); config.setCooldownTicks(130);
        }
        @Override protected String getModelId() { return "kitty_headbutt_love_bump"; }
        @Override protected double getModelScale() { return 1.6; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_CAT_PURR, 1.5f, 1.0f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 8 == 0) {
                c.getWorld().spawnParticle(Particle.HEART, c.clone().add(0, 1.5, 0), 8, 6, 1, 6, 0);
            }
            if (tick % 14 == 0) {
                pastelPinkDust(c.clone().add(0, 0.3, 0), 20, 8);
            }
        }
        @Override public AbstractAttack newInstance() { return new KittyHeadbuttLoveBump(plugin); }
    }

    // ============================================================
    // 5. TEDDY BEAR HUG RING — Squeeze
    // 8 arms close in. Damage zone grows as arms converge.
    // ============================================================
    public static class TeddyHugSqueeze extends ModelEngineAttack {
        public TeddyHugSqueeze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("teddy_hug_squeeze", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(150.0); config.setDamageRadius(8.0);
            config.setModelengineScale("8.0");
            config.setTicksBetweenDamage(12); config.setDamageDelayTicks(5);
            config.setDurationTicks(160); config.setCooldownTicks(140);
        }
        @Override protected String getModelId() { return "teddy_hug_squeeze"; }
        @Override protected double getModelScale() { return 1.5; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_WOOL_PLACE, 1.0f, 0.8f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 18 == 0) {
                c.getWorld().spawnParticle(Particle.HEART, c.clone().add(0, 2, 0), 4, 1, 0.5, 1, 0);
                if (c.getWorld() != null) c.getWorld().spawnParticle(Particle.GLOW,
                        c.clone().add(0, 1, 0), 8, 3, 0.5, 3, 0);
            }
        }
        @Override public AbstractAttack newInstance() { return new TeddyHugSqueeze(plugin); }
    }

    // ============================================================
    // 6. SAKURA PETAL STORM BURST — Bloom
    // Petal cloud explodes outward. Uses CHERRY_LEAVES heavily.
    // ============================================================
    public static class SakuraPetalBloom extends ModelEngineAttack {
        public SakuraPetalBloom(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sakura_petal_bloom", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(105.0); config.setDamageRadius(12.0);
            config.setModelengineScale("12.0");
            config.setTicksBetweenDamage(10); config.setDamageDelayTicks(2);
            config.setDurationTicks(120); config.setCooldownTicks(110);
        }
        @Override protected String getModelId() { return "sakura_petal_bloom"; }
        @Override protected double getModelScale() { return 1.7; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHERRY_LEAVES_PLACE, 1.3f, 1.0f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.6f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.CHERRY_LEAVES, c.clone().add(0, 3, 0), 12, 6, 3, 6, 0.05);
            }
        }
        @Override public AbstractAttack newInstance() { return new SakuraPetalBloom(plugin); }
    }

    // ============================================================
    // 7. RAINBOW CRASH WAVE — Full Spectrum
    // Expanding rainbow ring. Damage on outer ring frontier.
    // ============================================================
    public static class RainbowCrashFullSpectrum extends ModelEngineAttack {
        public RainbowCrashFullSpectrum(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rainbow_crash_full_spectrum", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(165.0); config.setDamageRadius(12.0);
            config.setModelengineScale("12.0");
            config.setTicksBetweenDamage(10); config.setDamageDelayTicks(3);
            config.setDurationTicks(120); config.setCooldownTicks(130);
        }
        @Override protected String getModelId() { return "rainbow_crash_full_spectrum"; }
        @Override protected double getModelScale() { return 1.6; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_LEVELUP, 1.4f, 1.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 4 == 0) {
                int[] rgb = {0xff6060, 0xffb040, 0xffe040, 0x80e860, 0x60b0ff, 0x8060e0, 0xc060e0};
                int color = rgb[(tick / 4) % 7];
                c.getWorld().spawnParticle(Particle.DUST, c.clone().add(0, 1, 0), 16, 6, 0.5, 6, 0,
                        new Particle.DustOptions(Color.fromRGB((color >> 16) & 0xff, (color >> 8) & 0xff, color & 0xff), 1.5f));
            }
        }
        @Override public AbstractAttack newInstance() { return new RainbowCrashFullSpectrum(plugin); }
    }

    // ============================================================
    // 8. RUBBER DUCK LAUNCH — Squeak
    // Projectile-style. Tracks player. High impact damage on landing.
    // ============================================================
    public static class RubberDuckSqueak extends ModelEngineAttack {
        public RubberDuckSqueak(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rubber_duck_squeak", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(0.0); config.setDamageRadius(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(330.0); config.setImpactRadius(3.0);
            config.setTracksPlayer(true);
            config.setDurationTicks(60); config.setCooldownTicks(110);
        }
        @Override protected String getModelId() { return "rubber_duck_squeak"; }
        @Override protected double getModelScale() { return 1.5; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_PARROT_AMBIENT, 1.4f, 1.6f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick == 50) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PARROT_AMBIENT, 1.5f, 2.0f);
                triggerImpactDamage(c);
                c.getWorld().spawnParticle(Particle.SPLASH, c.clone().add(0, 1, 0), 30, 1, 0.5, 1, 0);
            }
        }
        @Override public AbstractAttack newInstance() { return new RubberDuckSqueak(plugin); }
    }

    // ============================================================
    // 9. CANDY CANNON SHOT — Hyperglycemic
    // Projectile. Sparkle trail. Shatters on impact.
    // ============================================================
    public static class CandyCannonHyperglycemic extends ModelEngineAttack {
        public CandyCannonHyperglycemic(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("candy_cannon_hyperglycemic", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(0.0); config.setDamageRadius(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(270.0); config.setImpactRadius(3.5);
            config.setTracksPlayer(true);
            config.setDurationTicks(50); config.setCooldownTicks(90);
        }
        @Override protected String getModelId() { return "candy_cannon_hyperglycemic"; }
        @Override protected double getModelScale() { return 1.4; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.6f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 2 == 0) {
                pastelPinkDust(c.clone().add(0, 1, 0), 4, 0.4);
                mintDust(c.clone().add(0, 1, 0), 2, 0.4);
            }
            if (tick == 40) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.3f, 1.4f);
                triggerImpactDamage(c);
                c.getWorld().spawnParticle(Particle.FIREWORK, c.clone().add(0, 1, 0), 30, 2, 1, 2, 0.1);
            }
        }
        @Override public AbstractAttack newInstance() { return new CandyCannonHyperglycemic(plugin); }
    }

    // ============================================================
    // 10. LASER POINTER DOT BEAM — Fixation
    // Channeled. Damage when player is on the dot for cumulative ticks.
    // ============================================================
    public static class LaserPointerFixation extends ModelEngineAttack {
        public LaserPointerFixation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("laser_pointer_fixation", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(150.0); config.setDamageRadius(6.0);
            config.setModelengineScale("6.0");
            config.setTicksBetweenDamage(8); config.setDamageDelayTicks(2);
            config.setTracksPlayer(true);
            config.setDurationTicks(160); config.setCooldownTicks(120);
        }
        @Override protected String getModelId() { return "laser_pointer_fixation"; }
        @Override protected double getModelScale() { return 1.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_CAT_AMBIENT, 1.0f, 1.8f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.DUST, c.clone().add(0, 0.2, 0), 6, 0.6, 0.05, 0.6, 0,
                        new Particle.DustOptions(Color.fromRGB(0xff, 0x20, 0x20), 1.0f));
            }
        }
        @Override public AbstractAttack newInstance() { return new LaserPointerFixation(plugin); }
    }

    // ============================================================
    // 11. PLUSH DRAGON SUMMON — Stuffed Apocalypse
    // Stationary summon. Persistent damage zone. Lavender dust.
    // ============================================================
    public static class PlushDragonSummon extends ModelEngineAttack {
        public PlushDragonSummon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("plush_dragon_summon", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(165.0); config.setDamageRadius(8.0);
            config.setModelengineScale("8.0");
            config.setTicksBetweenDamage(12); config.setDamageDelayTicks(10);
            config.setDurationTicks(220); config.setCooldownTicks(170);
        }
        @Override protected String getModelId() { return "plush_dragon_summon"; }
        @Override protected double getModelScale() { return 1.8; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 2.0f);
            DisplayBuilder.playSound(c, Sound.BLOCK_WOOL_PLACE, 1.2f, 1.0f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 12 == 0) {
                lavenderDust(c.clone().add(0, 2, 0), 12, 1.5);
                mintDust(c.clone().add(0, 1.8, 0), 6, 1.2);
            }
        }
        @Override public AbstractAttack newInstance() { return new PlushDragonSummon(plugin); }
    }

    // ============================================================
    // 12. GIANT TEDDY BEAR SUMMON — Mr. Cuddles
    // Tall stationary summon. Big radius, breathing rhythm damage.
    // ============================================================
    public static class TeddyGiantMrCuddles extends ModelEngineAttack {
        public TeddyGiantMrCuddles(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("teddy_giant_mr_cuddles", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(195.0); config.setDamageRadius(8.0);
            config.setModelengineScale("8.0");
            config.setTicksBetweenDamage(12); config.setDamageDelayTicks(15);
            config.setDurationTicks(280); config.setCooldownTicks(190);
        }
        @Override protected String getModelId() { return "teddy_giant_mr_cuddles"; }
        @Override protected double getModelScale() { return 2.2; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_POLAR_BEAR_AMBIENT, 1.0f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_WOOL_PLACE, 1.3f, 0.7f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 30 == 0) {
                c.getWorld().spawnParticle(Particle.HEART, c.clone().add(0, 4, 0), 4, 2, 1, 2, 0);
            }
        }
        @Override public AbstractAttack newInstance() { return new TeddyGiantMrCuddles(plugin); }
    }

    // ============================================================
    // 13. CAROUSEL HORSE ORBITAL — Merry Go
    // Tracks player. Persistent damage from rotating horses.
    // ============================================================
    public static class CarouselHorseMerryGo extends ModelEngineAttack {
        public CarouselHorseMerryGo(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("carousel_horse_merry_go", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(120.0); config.setDamageRadius(12.0);
            config.setModelengineScale("12.0");
            config.setTicksBetweenDamage(10); config.setDamageDelayTicks(10);
            // Setpiece: stays at spawn (tracksPlayer disabled).
            config.setTracksPlayer(false);
            config.setDurationTicks(240); config.setCooldownTicks(160);
        }
        @Override protected String getModelId() { return "carousel_horse_merry_go"; }
        @Override protected double getModelScale() { return 1.6; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_BELL, 1.2f, 1.2f);
            DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.9f, 1.4f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 15 == 0) {
                c.getWorld().spawnParticle(Particle.ENCHANT, c.clone().add(0, 8, 0), 12, 4, 2, 4, 0.2);
            }
            if (tick % 80 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.4f);
        }
        @Override public AbstractAttack newInstance() { return new CarouselHorseMerryGo(plugin); }
    }

    // ============================================================
    // 14. CAT GODDESS HALO ORBITAL — Nine Lives
    // Tracks player. Multiple orbiters mean wide ring damage.
    // ============================================================
    public static class CatGoddessNineLives extends ModelEngineAttack {
        public CatGoddessNineLives(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cat_goddess_nine_lives", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(120.0); config.setDamageRadius(12.0);
            config.setModelengineScale("12.0");
            config.setTicksBetweenDamage(10); config.setDamageDelayTicks(7);
            // Setpiece: stays at spawn (tracksPlayer disabled).
            config.setTracksPlayer(false);
            config.setDurationTicks(240); config.setCooldownTicks(160);
        }
        @Override protected String getModelId() { return "cat_goddess_nine_lives"; }
        @Override protected double getModelScale() { return 1.4; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_CAT_PURR, 1.2f, 1.2f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.6f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 8 == 0) {
                mintDust(c.clone().add(0, 4, 0), 9, 5);
                c.getWorld().spawnParticle(Particle.DUST, c.clone().add(0, 4, 0), 4, 4, 2, 4, 0,
                        new Particle.DustOptions(Color.fromRGB(0xff, 0xd0, 0x40), 1.4f));
            }
        }
        @Override public AbstractAttack newInstance() { return new CatGoddessNineLives(plugin); }
    }

    // ============================================================
    // 15. STUFFED RABBIT ARMY ORBITAL — Multiplication
    // Tracks player. Solemn ritual orbit.
    // ============================================================
    public static class RabbitArmyMultiplication extends ModelEngineAttack {
        public RabbitArmyMultiplication(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rabbit_army_multiplication", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(135.0); config.setDamageRadius(12.0);
            config.setModelengineScale("12.0");
            config.setTicksBetweenDamage(12); config.setDamageDelayTicks(10);
            // Setpiece: stays at spawn (tracksPlayer disabled).
            config.setTracksPlayer(false);
            config.setDurationTicks(240); config.setCooldownTicks(160);
        }
        @Override protected String getModelId() { return "rabbit_army_multiplication"; }
        @Override protected double getModelScale() { return 1.5; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_RABBIT_AMBIENT, 1.0f, 1.4f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 10 == 0) {
                pastelPinkDust(c.clone().add(0, 1.2, 0), 8, 4);
            }
        }
        @Override public AbstractAttack newInstance() { return new RabbitArmyMultiplication(plugin); }
    }

    // ============================================================
    // 16. GIANT RUBBER DUCK FLOCK — Armada
    // Tracks player. 4 ducks orbit + bob.
    // ============================================================
    public static class DuckFlockArmada extends ModelEngineAttack {
        public DuckFlockArmada(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("duck_flock_armada", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(135.0); config.setDamageRadius(12.0);
            config.setModelengineScale("12.0");
            config.setTicksBetweenDamage(10); config.setDamageDelayTicks(10);
            // Setpiece: stays at spawn (tracksPlayer disabled).
            config.setTracksPlayer(false);
            config.setDurationTicks(220); config.setCooldownTicks(160);
        }
        @Override protected String getModelId() { return "duck_flock_armada"; }
        @Override protected double getModelScale() { return 1.6; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_PARROT_AMBIENT, 1.4f, 1.5f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 10 == 0) {
                c.getWorld().spawnParticle(Particle.SPLASH, c.clone().add(0, 0.3, 0), 8, 6, 0.2, 6, 0);
            }
            if (tick % 50 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_PARROT_AMBIENT, 0.8f, 1.8f);
        }
        @Override public AbstractAttack newInstance() { return new DuckFlockArmada(plugin); }
    }

    // ============================================================
    // 17. PLUSHIE HYDRA SUMMON — Good Boy x3
    // 3 heads, 3 colors. Persistent stationary zone.
    // ============================================================
    public static class PlushieHydraGoodBoy extends ModelEngineAttack {
        public PlushieHydraGoodBoy(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("plushie_hydra_good_boy", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(165.0); config.setDamageRadius(8.0);
            config.setModelengineScale("8.0");
            config.setTicksBetweenDamage(12); config.setDamageDelayTicks(14);
            config.setDurationTicks(240); config.setCooldownTicks(180);
        }
        @Override protected String getModelId() { return "plushie_hydra_good_boy"; }
        @Override protected double getModelScale() { return 1.7; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_HOGLIN_AMBIENT, 1.0f, 1.6f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 10 == 0) {
                pastelPinkDust(c.clone().add(0, 4, 0), 4, 0.6);
                mintDust(c.clone().add(0, 4, 0), 4, 0.6);
                lavenderDust(c.clone().add(0, 4, 0), 4, 0.6);
            }
        }
        @Override public AbstractAttack newInstance() { return new PlushieHydraGoodBoy(plugin); }
    }

    // ============================================================
    // 18. JACK-IN-THE-BOX SUMMON — Surprise!
    // Wind-up spawn → spring pop. Brief but high-impact pop damage.
    // ============================================================
    public static class JackInBoxSurprise extends ModelEngineAttack {
        public JackInBoxSurprise(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("jack_in_box_surprise", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(120.0); config.setDamageRadius(8.0);
            config.setModelengineScale("8.0");
            config.setTicksBetweenDamage(12); config.setDamageDelayTicks(12);
            config.setDurationTicks(180); config.setCooldownTicks(140);
            config.setImpactDamage(330.0); config.setImpactRadius(5.5);
        }
        @Override protected String getModelId() { return "jack_in_box_surprise"; }
        @Override protected double getModelScale() { return 1.5; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_FLUTE, 0.9f, 1.6f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick == 18) DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1.0f, 1.4f);
            if (tick == 18) DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1.0f, 1.2f);
            if (tick == 18) {
                DisplayBuilder.playSound(c, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.5f, 1.6f);
                triggerImpactDamage(c);
                c.getWorld().spawnParticle(Particle.FIREWORK, c.clone().add(0, 4, 0), 30, 2, 2, 2, 0.3);
            }
        }
        @Override public AbstractAttack newInstance() { return new JackInBoxSurprise(plugin); }
    }

    // ============================================================
    // 19. BUTTERFLY SWARM ORBITAL — Flutter
    // Tracks player. 12 butterflies orbit at varied heights.
    // ============================================================
    public static class ButterflySwarmFlutter extends ModelEngineAttack {
        public ButterflySwarmFlutter(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("butterfly_swarm_flutter", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(105.0); config.setDamageRadius(12.0);
            config.setModelengineScale("12.0");
            config.setTicksBetweenDamage(12); config.setDamageDelayTicks(7);
            // Setpiece: stays at spawn (tracksPlayer disabled).
            config.setTracksPlayer(false);
            config.setDurationTicks(240); config.setCooldownTicks(150);
        }
        @Override protected String getModelId() { return "butterfly_swarm_flutter"; }
        @Override protected double getModelScale() { return 1.4; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_BEE_LOOP, 0.6f, 1.6f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.CHERRY_LEAVES, c.clone().add(0, 3, 0), 8, 5, 3, 5, 0);
            }
        }
        @Override public AbstractAttack newInstance() { return new ButterflySwarmFlutter(plugin); }
    }

    // ============================================================
    // 20. CANDY PLANET ORBITAL — Sugar Rush  (ULTIMATE)
    // Tracks player. Largest model, longest duration, biggest radius.
    // ============================================================
    public static class CandyPlanetSugarRush extends ModelEngineAttack {
        public CandyPlanetSugarRush(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("candy_planet_sugar_rush", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(150.0); config.setDamageRadius(15.0);
            config.setModelengineScale("15.0");
            config.setTicksBetweenDamage(8); config.setDamageDelayTicks(20);
            // Setpiece: stays at spawn (tracksPlayer disabled).
            config.setTracksPlayer(false);
            config.setChance(0.4); // ultimate — rare
            config.setDurationTicks(360); config.setCooldownTicks(300);
        }
        @Override protected String getModelId() { return "candy_planet_sugar_rush"; }
        @Override protected double getModelScale() { return 1.8; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_LEVELUP, 1.6f, 1.8f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f, 1.4f);
            DisplayBuilder.playSound(c, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.3f, 1.0f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            if (tick % 4 == 0) {
                int[] rgb = {0xff, 0xff, 0xff, 0xb8, 0xff, 0xb8, 0xb8, 0xb8, 0xfc, 0xff, 0xff, 0xb0, 0x88, 0xff, 0xb8, 0xe8};
                int idx = ((tick / 4) % 5) * 3;
                int r = rgb[idx], g = rgb[idx + 1], b = rgb[idx + 2];
                w.spawnParticle(Particle.DUST, c.clone().add(0, 8, 0), 12, 6, 4, 6, 0,
                        new Particle.DustOptions(Color.fromRGB(r, g, b), 1.6f));
            }
            if (tick % 10 == 0) {
                w.spawnParticle(Particle.END_ROD, c.clone().add(0, 8, 0), 4, 8, 5, 8, 0.05);
            }
            if (tick % 80 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.6f);
        }
        @Override public AbstractAttack newInstance() { return new CandyPlanetSugarRush(plugin); }
    }
}
