package com.blockforge.chaoscraft.modes.freezingice.attacks;

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

/**
 * FreezingIce Mode — MODELENGINE ATTACKS
 *
 * 25 ME VFX attacks themed around freezing ice / cryo / glacial menace.
 *
 *  Ground Eruption  (1) glacial_floor_shatter, (2) absolute_zero_spike_forest,
 *                   (3) glacier_fist_eruption, (4) tundra_crack_array,
 *                   (5) cryo_pillar_cross
 *  AOE Burst        (6) blizzard_nova, (7) frozen_time_ring,
 *                   (8) crystalline_aurora_burst, (9) cryo_pressure_collapse,
 *                   (10) ice_age_terminus
 *  Projectile       (11) cryo_lance, (12) avalanche_shot, (13) frozen_comet_me,
 *                   (14) frost_breath_beam, (15) icicle_volley_me
 *  Aerial/Orbital   (16) frozen_sentinel_array, (17) permafrost_throne,
 *                   (18) cryo_satellite_ring, (19) ice_wyrm_orbital,
 *                   (20) frozen_clock_me
 *  Cosmic           (21) avalanche_orrery, (22) frozen_mirror_array,
 *                   (23) snowstorm_vortex, (24) hypothermia_halo,
 *                   (25) glacial_memory
 *
 * Particle palette: SNOWFLAKE, CLOUD, ELECTRIC_SPARK, ITEM_SNOWBALL,
 *   FALLING_DUST, GLOW, END_ROD, SOUL_FIRE_FLAME, DUST with cold colors
 *   (icy blue #A8D8FF, deep cyan #60B8E0, white frost #E8F8FF,
 *   sickly teal #20D890 for tundra_crack_array).
 * Sounds: BLOCK_GLASS_BREAK/HIT/PLACE, ITEM_TRIDENT_RIPTIDE_1/2/3,
 *   BLOCK_NOTE_BLOCK_CHIME, BLOCK_AMETHYST_BLOCK_CHIME,
 *   ENTITY_GLOW_SQUID_AMBIENT, ENTITY_GENERIC_EXPLODE,
 *   BLOCK_BEACON_ACTIVATE/AMBIENT, ENTITY_WITHER_SPAWN,
 *   ENTITY_ENDER_DRAGON_GROWL.
 *
 * All 25 bbmodel files live in src/main/resources/models/freezingice/me_attacks/.
 */
public final class FreezingIceModelEngine {
    private FreezingIceModelEngine() {}

    private static final String MODE_PATH = "modes/freezingice/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        // Ground Eruption (impact-only)
        registry.register(new GlacialFloorShatter(plugin));
        registry.register(new AbsoluteZeroSpikeForest(plugin));
        registry.register(new GlacierFistEruption(plugin));
        registry.register(new TundraCrackArray(plugin));
        registry.register(new CryoPillarCross(plugin));
        // AOE Burst
        registry.register(new BlizzardNova(plugin));
        registry.register(new FrozenTimeRing(plugin));
        registry.register(new CrystallineAuroraBurst(plugin));
        registry.register(new CryoPressureCollapse(plugin));
        registry.register(new IceAgeTerminus(plugin));
        // Projectile (impact-only on landing)
        registry.register(new CryoLance(plugin));
        registry.register(new AvalancheShot(plugin));
        registry.register(new FrozenCometMe(plugin));
        registry.register(new FrostBreathBeam(plugin));
        registry.register(new IcicleVolleyMe(plugin));
        // Aerial / Orbital
        registry.register(new FrozenSentinelArray(plugin));
        registry.register(new PermafrostThrone(plugin));
        registry.register(new CryoSatelliteRing(plugin));
        registry.register(new IceWyrmOrbital(plugin));
        registry.register(new FrozenClockMe(plugin));
        // Cosmic
        registry.register(new AvalancheOrrery(plugin));
        registry.register(new FrozenMirrorArray(plugin));
        registry.register(new SnowstormVortex(plugin));
        registry.register(new HypothermiaHalo(plugin));
        registry.register(new GlacialMemory(plugin));
    }

    // ============================================================
    // Shared cold-palette particle helpers.
    // ============================================================

    /** Icy blue dust — primary FreezingIce color. */
    private static void coldDust(Location c, int count, double spread) {
        if (c == null || c.getWorld() == null) return;
        c.getWorld().spawnParticle(Particle.DUST, c, count, spread, spread, spread, 0,
                new Particle.DustOptions(Color.fromRGB(0xA8, 0xD8, 0xFF), 1.4f));
    }

    /** Deep cyan dust — secondary cold accent. */
    private static void cyanDust(Location c, int count, double spread) {
        if (c == null || c.getWorld() == null) return;
        c.getWorld().spawnParticle(Particle.DUST, c, count, spread, spread, spread, 0,
                new Particle.DustOptions(Color.fromRGB(0x60, 0xB8, 0xE0), 1.6f));
    }

    /** White frost dust — bright highlight. */
    private static void whiteFrostDust(Location c, int count, double spread) {
        if (c == null || c.getWorld() == null) return;
        c.getWorld().spawnParticle(Particle.DUST, c, count, spread, spread, spread, 0,
                new Particle.DustOptions(Color.fromRGB(0xE8, 0xF8, 0xFF), 1.5f));
    }

    /** Sickly teal dust — only for tundra_crack_array's cursed veins. */
    private static void cursedTealDust(Location c, int count, double spread) {
        if (c == null || c.getWorld() == null) return;
        c.getWorld().spawnParticle(Particle.DUST, c, count, spread, spread, spread, 0,
                new Particle.DustOptions(Color.fromRGB(0x20, 0xD8, 0x90), 1.5f));
    }

    // ============================================================
    // 1. GLACIAL FLOOR SHATTER — ground cavity collapse, impact-only
    // ============================================================
    public static class GlacialFloorShatter extends ModelEngineAttack {
        public GlacialFloorShatter(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_floor_shatter", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6600.0); config.setImpactRadius(16.5);
            config.setDurationTicks(140); config.setCooldownTicks(60);
        }
        @Override protected String getModelId() { return "glacial_floor_shatter"; }
        @Override protected double getModelScale() { return 2.5; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.5f);
            coldDust(c.clone().add(0, 1, 0), 60, 4);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick == 22) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.6f, 0.4f);
                triggerImpactDamage(c);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1, 0), 80, 6, 1, 6, 0.1);
                whiteFrostDust(c.clone().add(0, 0.5, 0), 70, 6);
            }
        }
        @Override public AbstractAttack newInstance() { return new GlacialFloorShatter(plugin); }
    }

    // ============================================================
    // 2. ABSOLUTE ZERO SPIKE FOREST — ground spike eruption
    // ============================================================
    public static class AbsoluteZeroSpikeForest extends ModelEngineAttack {
        public AbsoluteZeroSpikeForest(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("absolute_zero_spike_forest", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(5760.0); config.setImpactRadius(15.0);
            config.setDurationTicks(130); config.setCooldownTicks(60);
        }
        @Override protected String getModelId() { return "absolute_zero_spike_forest"; }
        @Override protected double getModelScale() { return 2.2; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_HIT, 1.4f, 0.4f);
            DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.0f, 0.5f);
            coldDust(c, 40, 3.5);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick == 18) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
                triggerImpactDamage(c);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 2, 0), 60, 5, 2, 5, 0.05);
                cyanDust(c.clone().add(0, 1.5, 0), 50, 5);
            }
            if (tick > 4 && tick < 18 && tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, 1, 0), 6, 3, 1, 3, 0.02);
            }
        }
        @Override public AbstractAttack newInstance() { return new AbsoluteZeroSpikeForest(plugin); }
    }

    // ============================================================
    // 3. GLACIER FIST ERUPTION — massive ice fist punch up
    // ============================================================
    public static class GlacierFistEruption extends ModelEngineAttack {
        public GlacierFistEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacier_fist_eruption", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(7200.0); config.setImpactRadius(18.0);
            config.setDurationTicks(160); config.setCooldownTicks(60);
        }
        @Override protected String getModelId() { return "glacier_fist_eruption"; }
        @Override protected double getModelScale() { return 2.5; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_WITHER_SPAWN, 0.9f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.3f);
            cyanDust(c.clone().add(0, 2, 0), 70, 4);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick == 26) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.8f, 0.3f);
                triggerImpactDamage(c);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 3, 0), 100, 7, 3, 7, 0.1);
                whiteFrostDust(c.clone().add(0, 1, 0), 80, 6);
            }
            if (tick > 8 && tick < 26 && tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.FALLING_DUST, c.clone().add(0, 2, 0), 15, 4, 2, 4,
                        org.bukkit.Material.BLUE_ICE.createBlockData());
            }
        }
        @Override public AbstractAttack newInstance() { return new GlacierFistEruption(plugin); }
    }

    // ============================================================
    // 4. TUNDRA CRACK ARRAY — long cursed-vein cracks across floor
    // ============================================================
    public static class TundraCrackArray extends ModelEngineAttack {
        public TundraCrackArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tundra_crack_array", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(5040.0); config.setImpactRadius(18.0);
            config.setDurationTicks(140); config.setCooldownTicks(60);
        }
        @Override protected String getModelId() { return "tundra_crack_array"; }
        @Override protected double getModelScale() { return 2.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_HIT, 1.3f, 0.5f);
            DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.4f, 0.7f);
            cursedTealDust(c.clone().add(0, 0.3, 0), 60, 5);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick == 22) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
                triggerImpactDamage(c);
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, c.clone().add(0, 0.5, 0), 70, 8, 0.5, 8, 0.02);
                cursedTealDust(c, 90, 8);
            }
            if (tick % 6 == 0 && tick < 22) {
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, 0.3, 0), 8, 6, 0.3, 6, 0.05);
            }
        }
        @Override public AbstractAttack newInstance() { return new TundraCrackArray(plugin); }
    }

    // ============================================================
    // 5. CRYO PILLAR CROSS — cross-shaped pillar eruption
    // ============================================================
    public static class CryoPillarCross extends ModelEngineAttack {
        public CryoPillarCross(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cryo_pillar_cross", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6000.0); config.setImpactRadius(15.0);
            config.setDurationTicks(150); config.setCooldownTicks(60);
        }
        @Override protected String getModelId() { return "cryo_pillar_cross"; }
        @Override protected double getModelScale() { return 2.3; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.4f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.6f);
            coldDust(c.clone().add(0, 2, 0), 50, 4);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick == 24) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.6f, 0.4f);
                triggerImpactDamage(c);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 2, 0), 80, 6, 3, 6, 0.08);
                whiteFrostDust(c.clone().add(0, 2, 0), 60, 5);
            }
            if (tick > 6 && tick < 24 && tick % 4 == 0) {
                for (int i = 0; i < 4; i++) {
                    double a = Math.PI / 2 * i;
                    Location arm = c.clone().add(Math.cos(a) * 6, 1, Math.sin(a) * 6);
                    c.getWorld().spawnParticle(Particle.END_ROD, arm, 4, 0.4, 0.4, 0.4, 0.02);
                }
            }
        }
        @Override public AbstractAttack newInstance() { return new CryoPillarCross(plugin); }
    }

    // ============================================================
    // 6. BLIZZARD NOVA — AOE constant-radius burst with snow
    // ============================================================
    public static class BlizzardNova extends ModelEngineAttack {
        public BlizzardNova(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blizzard_nova", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(2760.0); config.setDamageRadius(18.0);
            config.setModelengineScale("18.0");
            config.setTicksBetweenDamage(4); config.setDamageDelayTicks(0);
            config.setDurationTicks(100); config.setCooldownTicks(60);
        }
        @Override protected String getModelId() { return "blizzard_nova"; }
        @Override protected double getModelScale() { return 2.5; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.6f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.7f);
            coldDust(c.clone().add(0, 1, 0), 60, 5);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 5 == 0) {
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1.5, 0), 30, 10, 2, 10, 0.1);
                c.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, c.clone().add(0, 1, 0), 20, 9, 2, 9, 0.05);
            }
            if (tick % 12 == 0) {
                cyanDust(c.clone().add(0, 1, 0), 35, 9);
            }
        }
        @Override public AbstractAttack newInstance() { return new BlizzardNova(plugin); }
    }

    // ============================================================
    // 7. FROZEN TIME RING — clock-like ring AOE
    // ============================================================
    public static class FrozenTimeRing extends ModelEngineAttack {
        public FrozenTimeRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_time_ring", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(2400.0); config.setDamageRadius(16.5);
            config.setModelengineScale("16.5");
            config.setTicksBetweenDamage(4); config.setDamageDelayTicks(0);
            config.setDurationTicks(140); config.setCooldownTicks(60);
        }
        @Override protected String getModelId() { return "frozen_time_ring"; }
        @Override protected double getModelScale() { return 2.4; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.4f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 1.1f, 0.7f);
            whiteFrostDust(c.clone().add(0, 1, 0), 50, 6);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 12 == 0) {
                double phase = (tick / 12.0) * (Math.PI / 6);
                for (int i = 0; i < 12; i++) {
                    double a = phase + Math.PI * 2 * i / 12;
                    Location dot = c.clone().add(Math.cos(a) * 9, 1.2, Math.sin(a) * 9);
                    c.getWorld().spawnParticle(Particle.END_ROD, dot, 3, 0.2, 0.2, 0.2, 0.01);
                }
            }
            if (tick % 8 == 0) {
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1.5, 0), 12, 8, 1, 8, 0.03);
            }
        }
        @Override public AbstractAttack newInstance() { return new FrozenTimeRing(plugin); }
    }

    // ============================================================
    // 8. CRYSTALLINE AURORA BURST — rainbow aurora cold burst
    // ============================================================
    public static class CrystallineAuroraBurst extends ModelEngineAttack {
        public CrystallineAuroraBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystalline_aurora_burst", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(2880.0); config.setDamageRadius(18.0);
            config.setModelengineScale("18.0");
            config.setTicksBetweenDamage(4); config.setDamageDelayTicks(0);
            config.setDurationTicks(120); config.setCooldownTicks(60);
        }
        @Override protected String getModelId() { return "crystalline_aurora_burst"; }
        @Override protected double getModelScale() { return 2.2; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 0.8f);
            cyanDust(c.clone().add(0, 2, 0), 60, 6);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.GLOW, c.clone().add(0, 2.5, 0), 20, 10, 3, 10, 0.04);
            }
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, 1.5, 0), 12, 8, 2, 8, 0.06);
                whiteFrostDust(c.clone().add(0, 1.5, 0), 18, 8);
            }
        }
        @Override public AbstractAttack newInstance() { return new CrystallineAuroraBurst(plugin); }
    }

    // ============================================================
    // 9. CRYO PRESSURE COLLAPSE — implodes then bursts (impact-only)
    // ============================================================
    public static class CryoPressureCollapse extends ModelEngineAttack {
        public CryoPressureCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cryo_pressure_collapse", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6960.0); config.setImpactRadius(18.0);
            config.setDurationTicks(130); config.setCooldownTicks(60);
        }
        @Override protected String getModelId() { return "cryo_pressure_collapse"; }
        @Override protected double getModelScale() { return 2.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 1.5f, 0.4f);
            cyanDust(c.clone().add(0, 1, 0), 50, 5);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            // Implosion warning particles
            if (tick > 6 && tick < 30 && tick % 3 == 0) {
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * 2 * i / 8;
                    double r = 8.0 - (tick * 0.2);
                    if (r > 0.5) {
                        Location p = c.clone().add(Math.cos(a) * r, 1, Math.sin(a) * r);
                        c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, p, 3, 0.1, 0.1, 0.1, 0.05);
                    }
                }
            }
            if (tick == 30) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.8f, 0.3f);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.4f);
                triggerImpactDamage(c);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1.5, 0), 120, 8, 3, 8, 0.15);
                whiteFrostDust(c.clone().add(0, 1, 0), 100, 8);
            }
        }
        @Override public AbstractAttack newInstance() { return new CryoPressureCollapse(plugin); }
    }

    // ============================================================
    // 10. ICE AGE TERMINUS — advancing wall AOE
    // ============================================================
    public static class IceAgeTerminus extends ModelEngineAttack {
        public IceAgeTerminus(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_age_terminus", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(3120.0); config.setDamageRadius(18.0);
            config.setModelengineScale("18.0");
            config.setTicksBetweenDamage(4); config.setDamageDelayTicks(0);
            config.setDurationTicks(160); config.setCooldownTicks(60);
        }
        @Override protected String getModelId() { return "ice_age_terminus"; }
        @Override protected double getModelScale() { return 2.6; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.3f, 0.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.5f);
            coldDust(c.clone().add(0, 2, 0), 70, 7);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 5 == 0) {
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 2, 0), 35, 12, 3, 12, 0.08);
            }
            if (tick % 10 == 0) {
                c.getWorld().spawnParticle(Particle.FALLING_DUST, c.clone().add(0, 3, 0), 20, 10, 2, 10,
                        org.bukkit.Material.PACKED_ICE.createBlockData());
                cyanDust(c.clone().add(0, 1.5, 0), 40, 10);
            }
        }
        @Override public AbstractAttack newInstance() { return new IceAgeTerminus(plugin); }
    }

    // ============================================================
    // 11. CRYO LANCE — projectile (impact-only on landing)
    // ============================================================
    public static class CryoLance extends ModelEngineAttack {
        public CryoLance(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cryo_lance", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(5520.0); config.setImpactRadius(12.0);
            config.setDurationTicks(100); config.setCooldownTicks(60);
        }
        @Override protected String getModelId() { return "cryo_lance"; }
        @Override protected double getModelScale() { return 2.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_2, 1.5f, 0.7f);
            cyanDust(c, 35, 2.5);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick > 2 && tick < 20 && tick % 2 == 0) {
                c.getWorld().spawnParticle(Particle.END_ROD, c, 6, 0.4, 0.4, 0.4, 0.04);
            }
            if (tick == 20) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.5f);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.3f, 0.7f);
                triggerImpactDamage(c);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c, 60, 4, 2, 4, 0.1);
                whiteFrostDust(c, 50, 5);
            }
        }
        @Override public AbstractAttack newInstance() { return new CryoLance(plugin); }
    }

    // ============================================================
    // 12. AVALANCHE SHOT — projectile of compressed snow/rock
    // ============================================================
    public static class AvalancheShot extends ModelEngineAttack {
        public AvalancheShot(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("avalanche_shot", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6240.0); config.setImpactRadius(16.5);
            config.setDurationTicks(120); config.setCooldownTicks(60);
        }
        @Override protected String getModelId() { return "avalanche_shot"; }
        @Override protected double getModelScale() { return 2.2; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.4f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_HIT, 1.3f, 0.6f);
            coldDust(c, 40, 3);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick > 4 && tick < 24 && tick % 2 == 0) {
                c.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, c, 10, 0.6, 0.6, 0.6, 0.05);
                c.getWorld().spawnParticle(Particle.CLOUD, c, 4, 0.4, 0.4, 0.4, 0.02);
            }
            if (tick == 24) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.7f, 0.4f);
                triggerImpactDamage(c);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1, 0), 90, 6, 2, 6, 0.12);
                cyanDust(c.clone().add(0, 1, 0), 70, 6);
            }
        }
        @Override public AbstractAttack newInstance() { return new AvalancheShot(plugin); }
    }

    // ============================================================
    // 13. FROZEN COMET — high-arc icy comet (impact-only on landing)
    // ============================================================
    public static class FrozenCometMe extends ModelEngineAttack {
        public FrozenCometMe(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_comet_me", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(7200.0); config.setImpactRadius(18.0);
            config.setDurationTicks(130); config.setCooldownTicks(60);
        }
        @Override protected String getModelId() { return "frozen_comet_me"; }
        @Override protected double getModelScale() { return 2.4; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 0.6f);
            cyanDust(c, 60, 4);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick > 4 && tick < 30 && tick % 2 == 0) {
                c.getWorld().spawnParticle(Particle.END_ROD, c, 10, 0.5, 0.5, 0.5, 0.05);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c, 8, 0.6, 0.6, 0.6, 0.04);
            }
            if (tick == 30) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.9f, 0.3f);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.7f, 0.4f);
                triggerImpactDamage(c);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1, 0), 120, 7, 3, 7, 0.15);
                whiteFrostDust(c.clone().add(0, 1, 0), 90, 7);
            }
        }
        @Override public AbstractAttack newInstance() { return new FrozenCometMe(plugin); }
    }

    // ============================================================
    // 14. FROST BREATH BEAM — cone-shaped frost breath
    // ============================================================
    public static class FrostBreathBeam extends ModelEngineAttack {
        public FrostBreathBeam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_breath_beam", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(5040.0); config.setImpactRadius(15.0);
            config.setDurationTicks(100); config.setCooldownTicks(60);
        }
        @Override protected String getModelId() { return "frost_breath_beam"; }
        @Override protected double getModelScale() { return 2.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.7f);
            DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.2f, 0.6f);
            coldDust(c.clone().add(0, 1.5, 0), 50, 4);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick > 2 && tick < 20 && tick % 2 == 0) {
                c.getWorld().spawnParticle(Particle.CLOUD, c.clone().add(0, 1.5, 0), 14, 3, 1, 3, 0.06);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1.5, 0), 12, 3, 1, 3, 0.05);
            }
            if (tick == 20) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.7f);
                triggerImpactDamage(c);
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, c.clone().add(0, 1.5, 0), 60, 5, 2, 5, 0.08);
                cyanDust(c.clone().add(0, 1.5, 0), 60, 5);
            }
        }
        @Override public AbstractAttack newInstance() { return new FrostBreathBeam(plugin); }
    }

    // ============================================================
    // 15. ICICLE VOLLEY — multi-icicle barrage (impact-only)
    // ============================================================
    public static class IcicleVolleyMe extends ModelEngineAttack {
        public IcicleVolleyMe(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("icicle_volley_me", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(4560.0); config.setImpactRadius(13.5);
            config.setDurationTicks(110); config.setCooldownTicks(60);
        }
        @Override protected String getModelId() { return "icicle_volley_me"; }
        @Override protected double getModelScale() { return 2.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_2, 1.4f, 0.8f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_HIT, 1.3f, 0.7f);
            whiteFrostDust(c, 40, 3);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick > 4 && tick < 22 && tick % 2 == 0) {
                c.getWorld().spawnParticle(Particle.END_ROD, c, 8, 1.5, 1.5, 1.5, 0.04);
            }
            if (tick == 22) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.5f);
                triggerImpactDamage(c);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1, 0), 70, 5, 2, 5, 0.1);
                coldDust(c.clone().add(0, 1, 0), 60, 5);
            }
        }
        @Override public AbstractAttack newInstance() { return new IcicleVolleyMe(plugin); }
    }

    // ============================================================
    // 16. FROZEN SENTINEL ARRAY — orbiting sentinels
    // ============================================================
    public static class FrozenSentinelArray extends ModelEngineAttack {
        public FrozenSentinelArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_sentinel_array", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(2640.0); config.setDamageRadius(16.5);
            config.setModelengineScale("16.5");
            config.setTicksBetweenDamage(4); config.setDamageDelayTicks(1);
            config.setDurationTicks(160); config.setCooldownTicks(60);
        }
        @Override protected String getModelId() { return "frozen_sentinel_array"; }
        @Override protected double getModelScale() { return 2.3; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_ACTIVATE, 1.3f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.1f, 0.6f);
            cyanDust(c.clone().add(0, 2, 0), 50, 5);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 8 == 0) {
                double phase = tick * 0.05;
                for (int i = 0; i < 6; i++) {
                    double a = phase + Math.PI * 2 * i / 6;
                    Location s = c.clone().add(Math.cos(a) * 8, 2.5 + Math.sin(phase * 1.3) * 0.5, Math.sin(a) * 8);
                    c.getWorld().spawnParticle(Particle.END_ROD, s, 4, 0.3, 0.3, 0.3, 0.02);
                    whiteFrostDust(s, 5, 0.3);
                }
            }
        }
        @Override public AbstractAttack newInstance() { return new FrozenSentinelArray(plugin); }
    }

    // ============================================================
    // 17. PERMAFROST THRONE — massive aerial throne
    // ============================================================
    public static class PermafrostThrone extends ModelEngineAttack {
        public PermafrostThrone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("permafrost_throne", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(3000.0); config.setDamageRadius(18.0);
            config.setModelengineScale("18.0");
            config.setTicksBetweenDamage(4); config.setDamageDelayTicks(1);
            config.setDurationTicks(180); config.setCooldownTicks(60);
        }
        @Override protected String getModelId() { return "permafrost_throne"; }
        @Override protected double getModelScale() { return 2.5; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_ACTIVATE, 1.4f, 0.6f);
            cyanDust(c.clone().add(0, 3, 0), 70, 6);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 10 == 0) {
                c.getWorld().spawnParticle(Particle.GLOW, c.clone().add(0, 3, 0), 18, 6, 2, 6, 0.04);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 4, 0), 22, 7, 2, 7, 0.06);
            }
            if (tick % 16 == 0) {
                whiteFrostDust(c.clone().add(0, 2.5, 0), 35, 8);
            }
        }
        @Override public AbstractAttack newInstance() { return new PermafrostThrone(plugin); }
    }

    // ============================================================
    // 18. CRYO SATELLITE RING — orbiting satellites
    // ============================================================
    public static class CryoSatelliteRing extends ModelEngineAttack {
        public CryoSatelliteRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cryo_satellite_ring", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(2520.0); config.setDamageRadius(16.5);
            config.setModelengineScale("16.5");
            config.setTicksBetweenDamage(4); config.setDamageDelayTicks(1);
            config.setDurationTicks(160); config.setCooldownTicks(60);
        }
        @Override protected String getModelId() { return "cryo_satellite_ring"; }
        @Override protected double getModelScale() { return 2.2; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.5f, 0.7f);
            DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.2f, 0.8f);
            coldDust(c.clone().add(0, 2.5, 0), 50, 5);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 6 == 0) {
                double phase = tick * 0.08;
                for (int i = 0; i < 8; i++) {
                    double a = phase + Math.PI * 2 * i / 8;
                    Location s = c.clone().add(Math.cos(a) * 9, 2.8, Math.sin(a) * 9);
                    c.getWorld().spawnParticle(Particle.END_ROD, s, 3, 0.2, 0.2, 0.2, 0.02);
                }
            }
            if (tick % 12 == 0) {
                cyanDust(c.clone().add(0, 2, 0), 30, 8);
            }
        }
        @Override public AbstractAttack newInstance() { return new CryoSatelliteRing(plugin); }
    }

    // ============================================================
    // 19. ICE WYRM ORBITAL — serpentine ice wyrm overhead
    // ============================================================
    public static class IceWyrmOrbital extends ModelEngineAttack {
        public IceWyrmOrbital(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_wyrm_orbital", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(2880.0); config.setDamageRadius(18.0);
            config.setModelengineScale("18.0");
            config.setTicksBetweenDamage(4); config.setDamageDelayTicks(0);
            config.setDurationTicks(150); config.setCooldownTicks(60);
        }
        @Override protected String getModelId() { return "ice_wyrm_orbital"; }
        @Override protected double getModelScale() { return 2.4; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.4f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_HIT, 1.2f, 0.6f);
            cyanDust(c.clone().add(0, 3, 0), 60, 6);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 5 == 0) {
                double phase = tick * 0.12;
                Location head = c.clone().add(Math.cos(phase) * 10, 3 + Math.sin(phase * 2) * 1.5, Math.sin(phase) * 10);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, head, 15, 1, 1, 1, 0.08);
                c.getWorld().spawnParticle(Particle.END_ROD, head, 8, 0.6, 0.6, 0.6, 0.03);
            }
            if (tick % 14 == 0) {
                whiteFrostDust(c.clone().add(0, 3, 0), 30, 9);
            }
        }
        @Override public AbstractAttack newInstance() { return new IceWyrmOrbital(plugin); }
    }

    // ============================================================
    // 20. FROZEN CLOCK — overhead clock face, ticks
    // ============================================================
    public static class FrozenClockMe extends ModelEngineAttack {
        public FrozenClockMe(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_clock_me", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(2400.0); config.setDamageRadius(16.5);
            config.setModelengineScale("16.5");
            config.setTicksBetweenDamage(4); config.setDamageDelayTicks(1);
            config.setDurationTicks(200); config.setCooldownTicks(60);
        }
        @Override protected String getModelId() { return "frozen_clock_me"; }
        @Override protected double getModelScale() { return 2.2; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.6f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.7f);
            whiteFrostDust(c.clone().add(0, 3, 0), 50, 5);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            // Clock tick every 20 ticks (1 second)
            if (tick % 20 == 0 && tick > 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6f, 1.4f);
                double angle = (tick / 20.0) * (Math.PI * 2 / 12);
                Location hand = c.clone().add(Math.cos(angle) * 8, 3, Math.sin(angle) * 8);
                c.getWorld().spawnParticle(Particle.END_ROD, hand, 15, 0.6, 0.6, 0.6, 0.05);
                cyanDust(hand, 20, 1);
            }
            if (tick % 8 == 0) {
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 3, 0), 10, 9, 1, 9, 0.04);
            }
        }
        @Override public AbstractAttack newInstance() { return new FrozenClockMe(plugin); }
    }

    // ============================================================
    // 21. AVALANCHE ORRERY — cosmic clockwork machine
    // ============================================================
    public static class AvalancheOrrery extends ModelEngineAttack {
        public AvalancheOrrery(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("avalanche_orrery", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(2760.0); config.setDamageRadius(18.0);
            config.setModelengineScale("18.0");
            config.setTicksBetweenDamage(4); config.setDamageDelayTicks(1);
            config.setDurationTicks(180); config.setCooldownTicks(60);
        }
        @Override protected String getModelId() { return "avalanche_orrery"; }
        @Override protected double getModelScale() { return 2.4; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_ACTIVATE, 1.4f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.3f, 0.5f);
            cyanDust(c.clone().add(0, 3, 0), 60, 6);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 6 == 0) {
                double phase = tick * 0.06;
                for (int ring = 0; ring < 3; ring++) {
                    double r = 5 + ring * 3;
                    double offset = phase * (1 + ring * 0.3);
                    for (int i = 0; i < 4; i++) {
                        double a = offset + Math.PI * 2 * i / 4;
                        Location p = c.clone().add(Math.cos(a) * r, 3 + ring * 0.4, Math.sin(a) * r);
                        c.getWorld().spawnParticle(Particle.END_ROD, p, 3, 0.2, 0.2, 0.2, 0.02);
                    }
                }
            }
            if (tick % 14 == 0) {
                whiteFrostDust(c.clone().add(0, 3, 0), 30, 9);
            }
        }
        @Override public AbstractAttack newInstance() { return new AvalancheOrrery(plugin); }
    }

    // ============================================================
    // 22. FROZEN MIRROR ARRAY — shatters at tick 35 (impact-only)
    // ============================================================
    public static class FrozenMirrorArray extends ModelEngineAttack {
        public FrozenMirrorArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_mirror_array", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(5760.0); config.setImpactRadius(16.5);
            config.setDurationTicks(140); config.setCooldownTicks(60);
        }
        @Override protected String getModelId() { return "frozen_mirror_array"; }
        @Override protected double getModelScale() { return 2.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.4f, 0.6f);
            DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.3f, 0.5f);
            whiteFrostDust(c.clone().add(0, 2, 0), 50, 5);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick > 6 && tick < 35 && tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.GLOW, c.clone().add(0, 2, 0), 14, 5, 1.5, 5, 0.03);
                cyanDust(c.clone().add(0, 2, 0), 18, 6);
            }
            if (tick == 35) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.8f, 0.4f);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
                triggerImpactDamage(c);
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, 2, 0), 100, 7, 3, 7, 0.12);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 2, 0), 80, 6, 3, 6, 0.1);
                whiteFrostDust(c.clone().add(0, 2, 0), 80, 7);
            }
        }
        @Override public AbstractAttack newInstance() { return new FrozenMirrorArray(plugin); }
    }

    // ============================================================
    // 23. SNOWSTORM VORTEX — pull/swirl vortex AOE
    // ============================================================
    public static class SnowstormVortex extends ModelEngineAttack {
        public SnowstormVortex(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("snowstorm_vortex", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(3120.0); config.setDamageRadius(18.0);
            config.setModelengineScale("18.0");
            config.setTicksBetweenDamage(4); config.setDamageDelayTicks(0);
            config.setDurationTicks(160); config.setCooldownTicks(60);
        }
        @Override protected String getModelId() { return "snowstorm_vortex"; }
        @Override protected double getModelScale() { return 2.5; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.6f, 0.6f);
            DisplayBuilder.playSound(c, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.7f);
            coldDust(c.clone().add(0, 2, 0), 70, 7);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            // Swirling vortex particles
            if (tick % 2 == 0) {
                double phase = tick * 0.25;
                for (int i = 0; i < 5; i++) {
                    double a = phase + Math.PI * 2 * i / 5;
                    double r = 9 - (tick % 30) * 0.2;
                    if (r > 1) {
                        Location p = c.clone().add(Math.cos(a) * r, 0.5 + (tick % 30) * 0.15, Math.sin(a) * r);
                        c.getWorld().spawnParticle(Particle.SNOWFLAKE, p, 5, 0.3, 0.3, 0.3, 0.04);
                        c.getWorld().spawnParticle(Particle.CLOUD, p, 2, 0.2, 0.2, 0.2, 0.03);
                    }
                }
            }
            if (tick % 12 == 0) {
                cyanDust(c.clone().add(0, 2, 0), 30, 9);
            }
        }
        @Override public AbstractAttack newInstance() { return new SnowstormVortex(plugin); }
    }

    // ============================================================
    // 24. HYPOTHERMIA HALO — overhead halo AOE
    // ============================================================
    public static class HypothermiaHalo extends ModelEngineAttack {
        public HypothermiaHalo(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hypothermia_halo", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(2640.0); config.setDamageRadius(18.0);
            config.setModelengineScale("18.0");
            config.setTicksBetweenDamage(4); config.setDamageDelayTicks(1);
            config.setDurationTicks(170); config.setCooldownTicks(60);
        }
        @Override protected String getModelId() { return "hypothermia_halo"; }
        @Override protected double getModelScale() { return 2.3; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 1.5f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.3f, 0.6f);
            whiteFrostDust(c.clone().add(0, 3, 0), 60, 6);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 8 == 0) {
                double phase = tick * 0.04;
                for (int i = 0; i < 16; i++) {
                    double a = phase + Math.PI * 2 * i / 16;
                    Location p = c.clone().add(Math.cos(a) * 8, 3.5, Math.sin(a) * 8);
                    c.getWorld().spawnParticle(Particle.GLOW, p, 3, 0.2, 0.2, 0.2, 0.02);
                }
            }
            if (tick % 14 == 0) {
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 3.5, 0), 22, 8, 1, 8, 0.05);
                cyanDust(c.clone().add(0, 3, 0), 28, 8);
            }
        }
        @Override public AbstractAttack newInstance() { return new HypothermiaHalo(plugin); }
    }

    // ============================================================
    // 25. GLACIAL MEMORY — calving wall, frozen memories
    // ============================================================
    public static class GlacialMemory extends ModelEngineAttack {
        public GlacialMemory(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_memory", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(2880.0); config.setDamageRadius(18.0);
            config.setModelengineScale("18.0");
            config.setTicksBetweenDamage(4); config.setDamageDelayTicks(1);
            config.setDurationTicks(180); config.setCooldownTicks(60);
        }
        @Override protected String getModelId() { return "glacial_memory"; }
        @Override protected double getModelScale() { return 2.6; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 0.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.3f, 0.4f);
            cyanDust(c.clone().add(0, 3, 0), 70, 7);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            // Calving chunks falling
            if (tick % 10 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_HIT, 0.8f, 0.4f);
                c.getWorld().spawnParticle(Particle.FALLING_DUST, c.clone().add(0, 4, 0), 25, 10, 2, 10,
                        org.bukkit.Material.BLUE_ICE.createBlockData());
            }
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 2.5, 0), 22, 11, 2, 11, 0.06);
            }
            if (tick % 16 == 0) {
                whiteFrostDust(c.clone().add(0, 2.5, 0), 40, 10);
            }
        }
        @Override public AbstractAttack newInstance() { return new GlacialMemory(plugin); }
    }
}
