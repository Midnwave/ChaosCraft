package com.blockforge.chaoscraft.modes.freezingice.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Freezing Ice Mode — MODEL ENGINE VFX ATTACKS
 * 25 ice-themed ModelEngine 4 attacks. Each spawns a .bbmodel blueprint,
 * plays animations, and deals damage through unique mechanics.
 *
 * Color palette:
 * - Ice blue: RGB(100, 180, 255)
 * - Frost white: RGB(220, 240, 255)
 * - Deep ice: RGB(30, 80, 160)
 * - Glacial glow: RGB(150, 210, 255)
 *
 * Particles: SNOWFLAKE, END_ROD, DUST with ice colors
 * Sounds: BLOCK_GLASS_BREAK, ENTITY_PLAYER_HURT_FREEZE,
 *         BLOCK_POWDER_SNOW_STEP, BLOCK_AMETHYST_BLOCK_CHIME
 */
public final class IceModelEngine {
    private IceModelEngine() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new GlacialThrone(plugin));
        registry.register(new CrystallineSnowflake(plugin));
        registry.register(new FrozenColossusHand(plugin));
        registry.register(new CryoCoreReactor(plugin));
        registry.register(new ArcticRift(plugin));
        registry.register(new IceLanternCage(plugin));
        registry.register(new PermafrostGlyph(plugin));
        registry.register(new BlizzardEye(plugin));
        registry.register(new FrozenClockwork(plugin));
        registry.register(new GlacialAnchor(plugin));
        registry.register(new IcePillarCathedral(plugin));
        registry.register(new CryoSpiderWeb(plugin));
        registry.register(new AvalancheFist(plugin));
        registry.register(new FrostWraithSilhouette(plugin));
        registry.register(new CrystalHourglass(plugin));
        registry.register(new PolarVortexRing(plugin));
        registry.register(new IceMirror(plugin));
        registry.register(new GlacierFangMaw(plugin));
        registry.register(new ArcticCompassRose(plugin));
        registry.register(new FrozenChandelier(plugin));
        registry.register(new PermafrostNova(plugin));
        registry.register(new IceObeliskTrio(plugin));
        registry.register(new CryogenicSatellite(plugin));
        registry.register(new BlizzardColossusSkull(plugin));
        registry.register(new FrozenOrrery(plugin));
    }

    // ================================================================
    // Shared helpers
    // ================================================================

    /** Find nearest non-exempt survival player within range. */
    private static Player findNearestPlayer(Location center, double range) {
        if (center.getWorld() == null) return null;
        Player nearest = null;
        double nearestDist = range * range;
        for (Player p : center.getWorld().getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL) continue;
            if (p.isInvulnerable()) continue;
            double dist = p.getLocation().distanceSquared(center);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = p;
            }
        }
        return nearest;
    }

    /** Ice blue dust particles. */
    private static void iceDust(Location loc, int count, double spread) {
        DisplayBuilder.dustParticles(loc, count, spread, 100, 180, 255, 1.2f);
    }

    /** Frost white dust particles. */
    private static void frostDust(Location loc, int count, double spread) {
        DisplayBuilder.dustParticles(loc, count, spread, 220, 240, 255, 1.0f);
    }

    /** Deep ice dust particles. */
    private static void deepIceDust(Location loc, int count, double spread) {
        DisplayBuilder.dustParticles(loc, count, spread, 30, 80, 160, 1.4f);
    }

    /** Snowflake particles at location. */
    private static void snowflakes(Location loc, int count, double spread) {
        if (loc.getWorld() == null) return;
        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, count, spread, spread, spread, 0.02);
    }

    /** End rod particles at location. */
    private static void endRods(Location loc, int count, double spread) {
        if (loc.getWorld() == null) return;
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, count, spread, spread, spread, 0.01);
    }

    /** Damage all survival players within radius of a location. */
    private static void damageNearby(Location center, double radius, double damage) {
        if (center.getWorld() == null) return;
        double r2 = radius * radius;
        for (Player p : center.getWorld().getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL) continue;
            if (p.isInvulnerable()) continue;
            if (p.getLocation().distanceSquared(center) <= r2) {
                p.damage(damage);
                p.setNoDamageTicks(0);
            }
        }
    }

    /** Freeze sound. */
    private static void freezeSound(Location loc, float volume, float pitch) {
        DisplayBuilder.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, volume, pitch);
    }

    /** Glass shatter sound. */
    private static void shatterSound(Location loc, float volume, float pitch) {
        DisplayBuilder.playSound(loc, Sound.BLOCK_GLASS_BREAK, volume, pitch);
    }

    /** Amethyst chime sound. */
    private static void chimeSound(Location loc, float volume, float pitch) {
        DisplayBuilder.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, volume, pitch);
    }

    /** Powder snow step sound. */
    private static void snowStepSound(Location loc, float volume, float pitch) {
        DisplayBuilder.playSound(loc, Sound.BLOCK_POWDER_SNOW_STEP, volume, pitch);
    }

    // ================================================================
    // 1. GLACIAL THRONE — Ice throne erupts from ground.
    //    Slows + cold damage every 40 ticks within 5 blocks.
    //    Frost runes orbit dealing contact damage.
    // ================================================================
    public static class GlacialThrone extends ModelEngineAttack {
        private double runeAngle = 0;

        public GlacialThrone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_throne", AttackType.MODEL_ENGINE, 1, "modes/freezingice/attacks"));
            config.setDamage(8.0);       // 4 hearts cold damage
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(40);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
        }

        @Override protected String getModelId() { return "glacial_throne"; }
        @Override protected double getModelScale() { return 2.0; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center);
            playAnimation("emerge", 0.0, false);
            shatterSound(center, 1.2f, 0.5f);
            freezeSound(center, 1.0f, 0.3f);
            snowflakes(center, 30, 2.0);
            deepIceDust(center, 20, 1.5);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            runeAngle += 0.08;

            // Frost runes orbit the throne — 3 rune positions
            for (int i = 0; i < 3; i++) {
                double angle = runeAngle + (i * (2 * Math.PI / 3));
                double rx = Math.cos(angle) * 3.5;
                double rz = Math.sin(angle) * 3.5;
                Location runeLoc = c.clone().add(rx, 1.5, rz);

                if (tick % 2 == 0) {
                    iceDust(runeLoc, 3, 0.2);
                    endRods(runeLoc, 1, 0.1);
                }

                // Contact damage from runes — 3 hearts (6.0 damage) on touch
                damageNearby(runeLoc, 1.2, 6.0);
            }

            // Cold aura particles
            if (tick % 5 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), 5.0, Particle.SNOWFLAKE, 16, null);
                frostDust(c.clone().add(0, 2.0, 0), 5, 1.5);
            }

            // Ambient chime
            if (tick % 60 == 0) {
                chimeSound(c, 0.6f, 0.4f);
            }

            // Slow players within radius
            if (tick % 40 == 0) {
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    if (p.getLocation().distanceSquared(c) <= 25.0) { // 5^2
                        p.setFreezeTicks(Math.min(p.getFreezeTicks() + 60, 140));
                    }
                }
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new GlacialThrone(plugin); }
    }

    // ================================================================
    // 2. CRYSTALLINE SNOWFLAKE — Giant snowflake descends from sky.
    //    Landing shockwave: 8 hearts in 7-block radius.
    //    Idle: rotating arms deal 3 hearts on contact.
    // ================================================================
    public static class CrystallineSnowflake extends ModelEngineAttack {
        private boolean landed = false;
        private int landTick = 0;
        private double spinAngle = 0;
        private Location spawnLoc;

        public CrystallineSnowflake(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystalline_snowflake", AttackType.MODEL_ENGINE, 1, "modes/freezingice/attacks"));
            config.setDamage(6.0);       // 3 hearts contact
            config.setDamageRadius(2.0); // arm tip contact
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(260);
            config.setCooldownTicks(240);
            config.setDamageOnImpactOnly(false);
            config.setImpactDamage(16.0); // 8 hearts shockwave
            config.setImpactRadius(7.0);
        }

        @Override protected String getModelId() { return "crystalline_snowflake"; }
        @Override protected double getModelScale() { return 2.5; }

        @Override
        protected void onSpawn(Location center) {
            spawnLoc = center.clone().add(0, 20, 0); // Start high
            spawnModel(spawnLoc);
            playAnimation("descend", 0.0, false);
            chimeSound(center, 1.0f, 1.5f);
            endRods(spawnLoc, 20, 3.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (!landed) {
                // Descend toward ground
                if (spawnLoc.getY() > c.getY() + 1.0) {
                    spawnLoc.subtract(0, 0.4, 0);
                    if (modelHost != null && modelHost.isValid()) {
                        modelHost.teleport(spawnLoc);
                    }
                    // Falling particles
                    if (tick % 2 == 0) {
                        snowflakes(spawnLoc, 8, 2.0);
                        frostDust(spawnLoc, 4, 1.5);
                    }
                } else {
                    // Landed — shockwave
                    landed = true;
                    landTick = tick;
                    if (modelHost != null && modelHost.isValid()) {
                        modelHost.teleport(c);
                    }
                    playAnimation("land", 0.0, false);
                    triggerImpactDamage(c);
                    shatterSound(c, 1.5f, 0.6f);
                    freezeSound(c, 1.2f, 0.4f);
                    DisplayBuilder.particleRing(c, 7.0, Particle.SNOWFLAKE, 40, null);
                    deepIceDust(c, 30, 3.0);

                    // Freeze tick shockwave
                    for (Player p : c.getWorld().getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL) continue;
                        if (p.getLocation().distanceSquared(c) <= 49.0) { // 7^2
                            p.setFreezeTicks(Math.min(p.getFreezeTicks() + 80, 140));
                        }
                    }
                }
            } else {
                // Idle phase — spin and deal contact damage on arms
                spinAngle += 0.05;

                // 6 arm tips radiating outward from center
                for (int i = 0; i < 6; i++) {
                    double angle = spinAngle + (i * Math.PI / 3);
                    double tipX = Math.cos(angle) * 4.0;
                    double tipZ = Math.sin(angle) * 4.0;
                    Location tip = c.clone().add(tipX, 0.5, tipZ);

                    if (tick % 3 == 0) {
                        iceDust(tip, 2, 0.3);
                        endRods(tip, 1, 0.1);
                    }

                    // Contact damage on arm tips — 3 hearts
                    damageNearby(tip, 1.5, 6.0);
                }

                // Idle particle ring
                if (tick % 10 == 0) {
                    DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), 3.0, Particle.SNOWFLAKE, 12, null);
                }

                // Ambient sound
                if (tick % 50 == 0) {
                    chimeSound(c, 0.5f, 1.8f);
                }
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new CrystallineSnowflake(plugin); }
    }

    // ================================================================
    // 3. FROZEN COLOSSUS HAND — Giant ice hand reaches from ground.
    //    Fingers curl inward grabbing players, 6 hearts every 20 ticks
    //    while within 3 blocks of palm center.
    // ================================================================
    public static class FrozenColossusHand extends ModelEngineAttack {
        private boolean grabbing = false;
        private int grabCycle = 0;

        public FrozenColossusHand(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_colossus_hand", AttackType.MODEL_ENGINE, 1, "modes/freezingice/attacks"));
            config.setDamage(12.0);      // 6 hearts crushing
            config.setDamageRadius(3.0); // palm center
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(280);
            config.setCooldownTicks(260);
            config.setDamageDelayTicks(30); // Fingers need time to emerge
        }

        @Override protected String getModelId() { return "frozen_colossus_hand"; }
        @Override protected double getModelScale() { return 2.5; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center);
            playAnimation("emerge", 0.0, false);
            shatterSound(center, 1.5f, 0.4f);
            freezeSound(center, 1.0f, 0.3f);

            // Ground eruption particles
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                Location debris = center.clone().add(Math.cos(angle) * 2.0, 0.5, Math.sin(angle) * 2.0);
                deepIceDust(debris, 5, 0.5);
                snowflakes(debris, 3, 0.3);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            grabCycle++;

            // Alternate between open and grabbing every 60 ticks
            if (grabCycle % 60 == 0) {
                grabbing = !grabbing;
                if (grabbing) {
                    playAnimation("grab", 0.0, false);
                    shatterSound(c, 0.8f, 0.7f);
                } else {
                    playAnimation("open", 0.0, false);
                    snowStepSound(c, 0.6f, 0.5f);
                }
            }

            // Palm center is slightly above ground center
            Location palm = c.clone().add(0, 2.5, 0);

            // Crushing damage while grabbing — 6 hearts every 20 ticks
            if (grabbing && tick % 20 == 0) {
                damageNearby(palm, 3.0, 12.0);

                // Crushing particles
                deepIceDust(palm, 10, 1.5);
                shatterSound(palm, 0.5f, 1.2f);
            }

            // Pull players toward palm when grabbing
            if (grabbing) {
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    double dist = p.getLocation().distance(palm);
                    if (dist <= 5.0 && dist > 0.5) {
                        Vector pull = palm.toVector().subtract(p.getLocation().toVector()).normalize().multiply(0.15);
                        p.setVelocity(p.getVelocity().add(pull));
                    }
                }
            }

            // Ambient frost particles on fingers
            if (tick % 4 == 0) {
                for (int i = 0; i < 5; i++) {
                    double angle = (2 * Math.PI * i) / 5;
                    Location finger = palm.clone().add(Math.cos(angle) * 2.0, 1.0, Math.sin(angle) * 2.0);
                    iceDust(finger, 2, 0.3);
                }
                snowflakes(palm, 4, 1.0);
            }

            // Ambient sound
            if (tick % 40 == 0) {
                freezeSound(c, 0.7f, 0.5f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new FrozenColossusHand(plugin); }
    }

    // ================================================================
    // 4. CRYO CORE REACTOR — Floating cryo orb with gyroscopic rings.
    //    Pulsing cold aura: 4 hearts every 30 ticks within 6 blocks.
    //    Ring rotation sweep: bonus 2 hearts on contact.
    // ================================================================
    public static class CryoCoreReactor extends ModelEngineAttack {
        private double ringAngle1 = 0;
        private double ringAngle2 = 0;

        public CryoCoreReactor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cryo_core_reactor", AttackType.MODEL_ENGINE, 1, "modes/freezingice/attacks"));
            config.setDamage(8.0);       // 4 hearts pulsing aura
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(30);
            config.setDurationTicks(320);
            config.setCooldownTicks(300);
        }

        @Override protected String getModelId() { return "cryo_core_reactor"; }
        @Override protected double getModelScale() { return 1.5; }

        @Override
        protected void onSpawn(Location center) {
            Location floatLoc = center.clone().add(0, 3, 0);
            spawnModel(floatLoc);
            playAnimation("activate", 0.0, false);
            chimeSound(center, 1.2f, 0.6f);
            freezeSound(center, 0.8f, 0.4f);
            endRods(floatLoc, 25, 2.0);
            iceDust(floatLoc, 15, 1.5);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            Location core = c.clone().add(0, 3, 0);
            ringAngle1 += 0.1;
            ringAngle2 += 0.07;

            // Gyroscopic ring 1 — horizontal orbit
            for (int i = 0; i < 8; i++) {
                double angle = ringAngle1 + (i * Math.PI / 4);
                double rx = Math.cos(angle) * 3.0;
                double rz = Math.sin(angle) * 3.0;
                Location ringPoint = core.clone().add(rx, 0, rz);

                if (tick % 3 == 0) {
                    iceDust(ringPoint, 1, 0.1);
                }

                // Ring contact damage — 2 hearts bonus
                damageNearby(ringPoint, 1.0, 4.0);
            }

            // Gyroscopic ring 2 — vertical orbit
            for (int i = 0; i < 8; i++) {
                double angle = ringAngle2 + (i * Math.PI / 4);
                double rx = Math.cos(angle) * 3.0;
                double ry = Math.sin(angle) * 3.0;
                Location ringPoint = core.clone().add(rx, ry, 0);

                if (tick % 3 == 0) {
                    deepIceDust(ringPoint, 1, 0.1);
                }

                // Ring contact damage — 2 hearts bonus
                damageNearby(ringPoint, 1.0, 4.0);
            }

            // Pulsing cold aura visual
            if (tick % 30 == 0) {
                DisplayBuilder.particleRing(core, 6.0, Particle.SNOWFLAKE, 24, null);
                freezeSound(core, 0.6f, 0.6f);
            }

            // Core glow
            if (tick % 5 == 0) {
                endRods(core, 3, 0.5);
                frostDust(core, 2, 0.3);
            }

            // Ambient hum
            if (tick % 40 == 0) {
                chimeSound(core, 0.4f, 0.8f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new CryoCoreReactor(plugin); }
    }

    // ================================================================
    // 5. ARCTIC RIFT — Horizontal crack in reality with ice shards.
    //    Standing on rift line (2 blocks wide): 5 hearts/sec.
    //    Erupting shards: 3 hearts impact damage.
    // ================================================================
    public static class ArcticRift extends ModelEngineAttack {
        private double riftAngle;
        private int nextShardTick = 40;

        public ArcticRift(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("arctic_rift", AttackType.MODEL_ENGINE, 1, "modes/freezingice/attacks"));
            config.setDamage(10.0);      // 5 hearts/sec on rift line
            config.setDamageRadius(1.0); // checked manually per-line
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(240);
            config.setCooldownTicks(220);
        }

        @Override protected String getModelId() { return "arctic_rift"; }
        @Override protected double getModelScale() { return 2.0; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center);
            playAnimation("open", 0.0, false);
            riftAngle = Math.random() * 2 * Math.PI;
            shatterSound(center, 1.5f, 0.3f);
            freezeSound(center, 1.0f, 0.2f);

            // Rift crack particles along the line
            for (int i = -8; i <= 8; i++) {
                Location point = center.clone().add(Math.cos(riftAngle) * i, 0.1, Math.sin(riftAngle) * i);
                deepIceDust(point, 3, 0.3);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rift line damage — check players standing on the 2-block wide rift
            if (tick % 20 == 0) {
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    Location pLoc = p.getLocation();
                    // Distance from player to the rift line
                    double dx = pLoc.getX() - c.getX();
                    double dz = pLoc.getZ() - c.getZ();
                    // Project onto perpendicular of rift direction
                    double perpDist = Math.abs(-Math.sin(riftAngle) * dx + Math.cos(riftAngle) * dz);
                    // Along-rift distance
                    double alongDist = Math.abs(Math.cos(riftAngle) * dx + Math.sin(riftAngle) * dz);
                    if (perpDist <= 1.0 && alongDist <= 8.0) {
                        p.damage(10.0); // 5 hearts
                        p.setNoDamageTicks(0);
                        freezeSound(pLoc, 0.5f, 0.8f);
                    }
                }
            }

            // Shard eruptions at random positions along the rift
            if (tick >= nextShardTick) {
                nextShardTick = tick + 30 + (int)(Math.random() * 20);
                double shardOffset = (Math.random() - 0.5) * 14.0;
                Location shardLoc = c.clone().add(
                        Math.cos(riftAngle) * shardOffset, 0,
                        Math.sin(riftAngle) * shardOffset
                );
                // Shard erupts upward
                shatterSound(shardLoc, 0.8f, 1.5f);
                for (double y = 0; y < 3; y += 0.5) {
                    iceDust(shardLoc.clone().add(0, y, 0), 3, 0.3);
                    endRods(shardLoc.clone().add(0, y, 0), 1, 0.1);
                }
                // Shard impact damage — 3 hearts
                damageNearby(shardLoc, 2.0, 6.0);
            }

            // Rift line ambient particles
            if (tick % 4 == 0) {
                double randOffset = (Math.random() - 0.5) * 16.0;
                Location point = c.clone().add(Math.cos(riftAngle) * randOffset, 0.2, Math.sin(riftAngle) * randOffset);
                deepIceDust(point, 2, 0.4);
                snowflakes(point, 1, 0.2);
            }

            // Rift ambient sound
            if (tick % 50 == 0) {
                freezeSound(c, 0.5f, 0.3f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new ArcticRift(plugin); }
    }

    // ================================================================
    // 6. ICE LANTERN CAGE — Ornate cage with frost orb inside.
    //    Traps nearest player, 2 hearts/sec while inside.
    //    Breaking free requires taking 20 hearts total.
    // ================================================================
    public static class IceLanternCage extends ModelEngineAttack {
        private UUID trappedPlayer = null;
        private double totalDamageDealt = 0;
        private static final double BREAK_FREE_THRESHOLD = 40.0; // 20 hearts = 40 damage

        public IceLanternCage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_lantern_cage", AttackType.MODEL_ENGINE, 1, "modes/freezingice/attacks"));
            config.setDamage(4.0);       // 2 hearts/sec
            config.setDamageRadius(2.5);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(300);
            config.setCooldownTicks(350);
        }

        @Override protected String getModelId() { return "ice_lantern_cage"; }
        @Override protected double getModelScale() { return 1.8; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center);
            playAnimation("close", 0.0, false);
            chimeSound(center, 1.0f, 0.5f);
            shatterSound(center, 0.8f, 0.8f);
            iceDust(center, 15, 2.0);
            endRods(center, 10, 1.5);

            // Find and trap nearest player
            Player nearest = findNearestPlayer(center, 8.0);
            if (nearest != null) {
                trappedPlayer = nearest.getUniqueId();
                nearest.teleport(center.clone().add(0, 0.5, 0));
                freezeSound(center, 1.2f, 0.4f);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Check if cage is broken
            if (totalDamageDealt >= BREAK_FREE_THRESHOLD) {
                playAnimation("shatter", 0.0, false);
                shatterSound(c, 1.5f, 1.2f);
                // Shatter explosion
                for (int i = 0; i < 12; i++) {
                    double angle = (2 * Math.PI * i) / 12;
                    Location shard = c.clone().add(Math.cos(angle) * 2.0, 1.0, Math.sin(angle) * 2.0);
                    iceDust(shard, 5, 0.5);
                }
                trappedPlayer = null;
                // Force cleanup through duration
                return;
            }

            // Damage trapped player
            if (trappedPlayer != null && tick % 20 == 0) {
                Player trapped = Bukkit.getPlayer(trappedPlayer);
                if (trapped != null && trapped.isOnline() && trapped.getGameMode() == GameMode.SURVIVAL) {
                    // Keep player in cage
                    Location cageLoc = c.clone().add(0, 0.5, 0);
                    if (trapped.getLocation().distanceSquared(cageLoc) > 4.0) {
                        trapped.teleport(cageLoc);
                    }
                    trapped.damage(4.0); // 2 hearts
                    trapped.setNoDamageTicks(0);
                    totalDamageDealt += 4.0;
                    trapped.setFreezeTicks(Math.min(trapped.getFreezeTicks() + 40, 140));
                    freezeSound(c, 0.4f, 0.6f);
                } else {
                    trappedPlayer = null; // Player left
                }
            }

            // Frost orb glow inside
            if (tick % 3 == 0) {
                Location orbLoc = c.clone().add(0, 1.5, 0);
                endRods(orbLoc, 2, 0.3);
                frostDust(orbLoc, 2, 0.4);
            }

            // Cage bars frost
            if (tick % 8 == 0) {
                for (int i = 0; i < 4; i++) {
                    double angle = (Math.PI / 2) * i;
                    Location bar = c.clone().add(Math.cos(angle) * 1.2, 1.0, Math.sin(angle) * 1.2);
                    iceDust(bar, 2, 0.2);
                }
            }

            // Ambient sound
            if (tick % 40 == 0) {
                chimeSound(c, 0.5f, 0.3f);
            }
        }

        @Override protected void onModelCleanup() { trappedPlayer = null; }
        @Override public AbstractAttack newInstance() { return new IceLanternCage(plugin); }
    }

    // ================================================================
    // 7. PERMAFROST GLYPH — Flat rune sigil on ground.
    //    Standing on rings: stacking cold (1, 2, 3 hearts per tick).
    //    Center = instant 10 hearts.
    // ================================================================
    public static class PermafrostGlyph extends ModelEngineAttack {
        private final Map<UUID, Integer> stackTracker = new HashMap<>();

        public PermafrostGlyph(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("permafrost_glyph", AttackType.MODEL_ENGINE, 1, "modes/freezingice/attacks"));
            config.setDamage(2.0);       // base 1 heart (stacks)
            config.setDamageRadius(6.0); // outer ring radius
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(180);
        }

        @Override protected String getModelId() { return "permafrost_glyph"; }
        @Override protected double getModelScale() { return 2.0; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center);
            playAnimation("inscribe", 0.0, false);
            chimeSound(center, 1.0f, 0.4f);
            freezeSound(center, 0.8f, 0.5f);

            // Glyph appear particles — concentric rings
            DisplayBuilder.particleRing(center.clone().add(0, 0.1, 0), 2.0, Particle.END_ROD, 12, null);
            DisplayBuilder.particleRing(center.clone().add(0, 0.1, 0), 4.0, Particle.END_ROD, 18, null);
            DisplayBuilder.particleRing(center.clone().add(0, 0.1, 0), 6.0, Particle.END_ROD, 24, null);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Stacking damage check
            if (tick % 20 == 0) {
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    double dist = p.getLocation().distance(c);

                    if (dist <= 1.0) {
                        // CENTER — instant 10 hearts
                        p.damage(20.0);
                        p.setNoDamageTicks(0);
                        shatterSound(p.getLocation(), 1.0f, 0.5f);
                        deepIceDust(p.getLocation(), 15, 0.5);
                        stackTracker.remove(p.getUniqueId());
                    } else if (dist <= 6.0) {
                        // On rings — stacking cold damage
                        int stacks = stackTracker.getOrDefault(p.getUniqueId(), 0) + 1;
                        if (stacks > 3) stacks = 3;
                        stackTracker.put(p.getUniqueId(), stacks);

                        double damage = stacks * 2.0; // 1, 2, 3 hearts
                        p.damage(damage);
                        p.setNoDamageTicks(0);
                        p.setFreezeTicks(Math.min(p.getFreezeTicks() + 30 * stacks, 140));
                        freezeSound(p.getLocation(), 0.4f, 0.5f + (stacks * 0.2f));
                    } else {
                        // Off the glyph — reset stacks
                        stackTracker.remove(p.getUniqueId());
                    }
                }
            }

            // Glyph ring glow
            if (tick % 6 == 0) {
                Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(100, 180, 255), 1.0f);
                DisplayBuilder.particleRing(c.clone().add(0, 0.15, 0), 2.0, Particle.DUST, 8, dust);
                DisplayBuilder.particleRing(c.clone().add(0, 0.15, 0), 4.0, Particle.DUST, 12, dust);
                DisplayBuilder.particleRing(c.clone().add(0, 0.15, 0), 6.0, Particle.DUST, 16, dust);
            }

            // Center glow pulse
            if (tick % 10 == 0) {
                endRods(c.clone().add(0, 0.3, 0), 5, 0.5);
            }

            // Ambient
            if (tick % 50 == 0) {
                chimeSound(c, 0.4f, 0.6f);
            }
        }

        @Override
        protected void onModelCleanup() {
            stackTracker.clear();
        }

        @Override public AbstractAttack newInstance() { return new PermafrostGlyph(plugin); }
    }

    // ================================================================
    // 8. BLIZZARD EYE — Vertical eye shape.
    //    Blinks every 60 ticks: 6 hearts to all within 10 blocks.
    //    Looking at iris (within 15 degrees) doubles damage.
    // ================================================================
    public static class BlizzardEye extends ModelEngineAttack {
        private boolean eyeOpen = true;

        public BlizzardEye(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blizzard_eye", AttackType.MODEL_ENGINE, 1, "modes/freezingice/attacks"));
            config.setDamage(12.0);      // 6 hearts blink
            config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(60);
            config.setDurationTicks(360);
            config.setCooldownTicks(320);
        }

        @Override protected String getModelId() { return "blizzard_eye"; }
        @Override protected double getModelScale() { return 2.0; }

        @Override
        protected void onSpawn(Location center) {
            Location eyeLoc = center.clone().add(0, 4, 0);
            spawnModel(eyeLoc);
            playAnimation("open", 0.0, false);
            freezeSound(center, 1.0f, 0.2f);
            chimeSound(center, 0.8f, 0.3f);
            endRods(eyeLoc, 20, 2.5);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            Location eyeCenter = c.clone().add(0, 4, 0);

            // Blink cycle every 60 ticks
            if (tick % 60 == 0) {
                eyeOpen = !eyeOpen;
                if (!eyeOpen) {
                    // BLINK — deal damage
                    playAnimation("blink", 0.0, false);
                    shatterSound(eyeCenter, 1.2f, 0.5f);
                    freezeSound(eyeCenter, 1.5f, 0.3f);
                    DisplayBuilder.particleRing(eyeCenter, 10.0, Particle.SNOWFLAKE, 40, null);
                    deepIceDust(eyeCenter, 20, 5.0);

                    for (Player p : c.getWorld().getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL) continue;
                        if (p.isInvulnerable()) continue;
                        if (p.getLocation().distanceSquared(eyeCenter) > 100.0) continue; // 10^2

                        double damage = 12.0; // 6 hearts

                        // Check if player is looking at the iris — within 15 degrees
                        Vector toEye = eyeCenter.toVector().subtract(p.getEyeLocation().toVector()).normalize();
                        Vector lookDir = p.getEyeLocation().getDirection().normalize();
                        double dot = toEye.dot(lookDir);
                        // cos(15 deg) = 0.966
                        if (dot >= 0.966) {
                            damage *= 2; // Double damage for looking at iris
                            deepIceDust(p.getEyeLocation(), 10, 0.3);
                        }

                        p.damage(damage);
                        p.setNoDamageTicks(0);
                        p.setFreezeTicks(Math.min(p.getFreezeTicks() + 60, 140));
                    }
                } else {
                    playAnimation("open", 0.0, false);
                }
            }

            // Eye iris glow
            if (tick % 4 == 0 && eyeOpen) {
                endRods(eyeCenter, 3, 0.5);
                iceDust(eyeCenter, 2, 0.3);
            }

            // Ambient pupil tracking toward nearest player
            if (tick % 10 == 0) {
                Player nearest = findNearestPlayer(eyeCenter, 15.0);
                if (nearest != null && eyeOpen) {
                    Vector dir = nearest.getLocation().toVector().subtract(eyeCenter.toVector()).normalize();
                    Location gazeParticle = eyeCenter.clone().add(dir.multiply(1.5));
                    deepIceDust(gazeParticle, 3, 0.2);
                }
            }

            // Ambient
            if (tick % 45 == 0) {
                chimeSound(eyeCenter, 0.3f, 0.2f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new BlizzardEye(plugin); }
    }

    // ================================================================
    // 9. FROZEN CLOCKWORK — Interlocking ice gears.
    //    Between meshing gears: 8 hearts crushing.
    //    Gear teeth: 2 hearts on contact while spinning.
    // ================================================================
    public static class FrozenClockwork extends ModelEngineAttack {
        private double gear1Angle = 0;
        private double gear2Angle = Math.PI;

        public FrozenClockwork(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_clockwork", AttackType.MODEL_ENGINE, 1, "modes/freezingice/attacks"));
            config.setDamage(16.0);      // 8 hearts crushing between gears
            config.setDamageRadius(2.0); // mesh zone
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(280);
            config.setCooldownTicks(260);
        }

        @Override protected String getModelId() { return "frozen_clockwork"; }
        @Override protected double getModelScale() { return 2.0; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center);
            playAnimation("start", 0.0, false);
            shatterSound(center, 1.0f, 0.7f);
            chimeSound(center, 0.8f, 0.5f);
            iceDust(center, 20, 3.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            gear1Angle += 0.06;
            gear2Angle -= 0.06;

            // Gear 1 center — offset to the left
            Location gear1 = c.clone().add(-2.0, 1.0, 0);
            // Gear 2 center — offset to the right
            Location gear2 = c.clone().add(2.0, 1.0, 0);
            // Mesh zone — between the gears
            Location meshZone = c.clone().add(0, 1.0, 0);

            // Crushing damage in mesh zone — 8 hearts
            if (tick % 20 == 0) {
                damageNearby(meshZone, 2.0, 16.0);
            }

            // Gear teeth contact damage — 2 hearts
            for (int i = 0; i < 8; i++) {
                double angle1 = gear1Angle + (i * Math.PI / 4);
                Location tooth1 = gear1.clone().add(Math.cos(angle1) * 2.5, 0, Math.sin(angle1) * 2.5);
                damageNearby(tooth1, 0.8, 4.0);

                double angle2 = gear2Angle + (i * Math.PI / 4);
                Location tooth2 = gear2.clone().add(Math.cos(angle2) * 2.5, 0, Math.sin(angle2) * 2.5);
                damageNearby(tooth2, 0.8, 4.0);

                // Tooth particles
                if (tick % 4 == 0 && i % 2 == 0) {
                    iceDust(tooth1, 1, 0.1);
                    iceDust(tooth2, 1, 0.1);
                }
            }

            // Gear rotation particles
            if (tick % 5 == 0) {
                Particle.DustOptions gearDust = new Particle.DustOptions(Color.fromRGB(100, 180, 255), 0.8f);
                DisplayBuilder.particleRing(gear1, 2.5, Particle.DUST, 10, gearDust);
                DisplayBuilder.particleRing(gear2, 2.5, Particle.DUST, 10, gearDust);
            }

            // Mesh zone sparks
            if (tick % 6 == 0) {
                endRods(meshZone, 3, 0.5);
                deepIceDust(meshZone, 2, 0.3);
            }

            // Grinding sound
            if (tick % 30 == 0) {
                snowStepSound(c, 0.7f, 0.3f);
                shatterSound(meshZone, 0.3f, 1.5f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new FrozenClockwork(plugin); }
    }

    // ================================================================
    // 10. GLACIAL ANCHOR — Massive anchor drops from sky.
    //     Impact: 12 hearts in 5-block radius. Chain links: 3 hearts contact.
    //     Embedded anchor radiates 2 hearts/sec in 4 blocks.
    // ================================================================
    public static class GlacialAnchor extends ModelEngineAttack {
        private boolean dropped = false;
        private Location anchorLoc;
        private double chainSwing = 0;

        public GlacialAnchor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_anchor", AttackType.MODEL_ENGINE, 1, "modes/freezingice/attacks"));
            config.setDamage(4.0);       // 2 hearts/sec radiation
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
            config.setDamageOnImpactOnly(false);
            config.setImpactDamage(24.0); // 12 hearts
            config.setImpactRadius(5.0);
        }

        @Override protected String getModelId() { return "glacial_anchor"; }
        @Override protected double getModelScale() { return 2.5; }

        @Override
        protected void onSpawn(Location center) {
            anchorLoc = center.clone().add(0, 25, 0);
            spawnModel(anchorLoc);
            playAnimation("fall", 0.0, false);
            freezeSound(center, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (!dropped) {
                // Falling anchor
                anchorLoc.subtract(0, 0.8, 0);
                if (modelHost != null && modelHost.isValid()) {
                    modelHost.teleport(anchorLoc);
                }

                // Wind particles as it falls
                if (tick % 2 == 0) {
                    snowflakes(anchorLoc, 5, 1.5);
                    frostDust(anchorLoc, 3, 1.0);
                }

                // Impact check
                if (anchorLoc.getY() <= c.getY() + 1.0) {
                    dropped = true;
                    anchorLoc = c.clone();
                    if (modelHost != null && modelHost.isValid()) {
                        modelHost.teleport(anchorLoc);
                    }
                    playAnimation("impact", 0.0, false);
                    triggerImpactDamage(c);

                    // Impact FX
                    shatterSound(c, 2.0f, 0.3f);
                    freezeSound(c, 1.5f, 0.2f);
                    DisplayBuilder.particleRing(c, 5.0, Particle.SNOWFLAKE, 30, null);
                    deepIceDust(c, 30, 3.0);
                    // Ground crack particles
                    for (int i = 0; i < 12; i++) {
                        double angle = (2 * Math.PI * i) / 12;
                        Location crack = c.clone().add(Math.cos(angle) * 3.0, 0.1, Math.sin(angle) * 3.0);
                        iceDust(crack, 4, 0.3);
                    }
                }
            } else {
                chainSwing += 0.08;

                // Chain links swing — 4 chain segments above anchor
                for (int i = 1; i <= 4; i++) {
                    double swing = Math.sin(chainSwing + i * 0.5) * 1.5;
                    Location chainLoc = c.clone().add(swing, i * 1.5, Math.cos(chainSwing + i * 0.3) * 0.8);

                    // Chain contact damage — 3 hearts
                    damageNearby(chainLoc, 1.5, 6.0);

                    if (tick % 3 == 0) {
                        iceDust(chainLoc, 2, 0.2);
                    }
                }

                // Embedded anchor radiation particles
                if (tick % 5 == 0) {
                    DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), 4.0, Particle.SNOWFLAKE, 12, null);
                    frostDust(c, 3, 1.5);
                }

                // Ambient creak
                if (tick % 45 == 0) {
                    snowStepSound(c, 0.6f, 0.4f);
                }
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new GlacialAnchor(plugin); }
    }

    // ================================================================
    // 11. ICE PILLAR CATHEDRAL — 5 pillars in cross pattern.
    //     Each pillar radiates cold in 3 blocks (2 hearts/sec).
    //     Between 2+ pillars stacks effect. Center of cross = 3x damage.
    // ================================================================
    public static class IcePillarCathedral extends ModelEngineAttack {
        private static final double[][] PILLAR_OFFSETS = {
            {0, 0},    // center
            {5, 0},    // north
            {-5, 0},   // south
            {0, 5},    // east
            {0, -5}    // west
        };

        public IcePillarCathedral(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_pillar_cathedral", AttackType.MODEL_ENGINE, 1, "modes/freezingice/attacks"));
            config.setDamage(4.0);       // 2 hearts/sec per pillar
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(320);
            config.setCooldownTicks(300);
        }

        @Override protected String getModelId() { return "ice_pillar_cathedral"; }
        @Override protected double getModelScale() { return 2.0; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center);
            playAnimation("rise", 0.0, false);
            shatterSound(center, 1.5f, 0.4f);
            freezeSound(center, 1.0f, 0.3f);

            // Eruption particles at each pillar position
            for (double[] off : PILLAR_OFFSETS) {
                Location pillar = center.clone().add(off[0], 0, off[1]);
                deepIceDust(pillar, 10, 1.0);
                snowflakes(pillar, 5, 0.5);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (tick % 20 == 0) {
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;

                    // Count how many pillar auras the player is inside
                    int pillarCount = 0;
                    for (double[] off : PILLAR_OFFSETS) {
                        Location pillar = c.clone().add(off[0], 0, off[1]);
                        if (p.getLocation().distanceSquared(pillar) <= 9.0) { // 3^2
                            pillarCount++;
                        }
                    }

                    if (pillarCount > 0) {
                        // Check if in center of cross (within 2 blocks of center pillar)
                        boolean inCenter = p.getLocation().distanceSquared(c) <= 4.0;
                        double multiplier = inCenter ? 3.0 : pillarCount;
                        double damage = 4.0 * multiplier; // 2 hearts * multiplier

                        p.damage(damage);
                        p.setNoDamageTicks(0);
                        p.setFreezeTicks(Math.min(p.getFreezeTicks() + 20 * pillarCount, 140));
                        freezeSound(p.getLocation(), 0.3f + (0.1f * pillarCount), 0.5f);
                    }
                }
            }

            // Pillar glow particles
            if (tick % 6 == 0) {
                for (double[] off : PILLAR_OFFSETS) {
                    Location pillar = c.clone().add(off[0], 0, off[1]);
                    // Vertical frost line up the pillar
                    for (double y = 0; y < 5; y += 1.0) {
                        iceDust(pillar.clone().add(0, y, 0), 1, 0.2);
                    }
                    DisplayBuilder.particleRing(pillar.clone().add(0, 0.2, 0), 3.0, Particle.SNOWFLAKE, 6, null);
                }
            }

            // Connection beams between pillars (visual only)
            if (tick % 8 == 0) {
                Location center = c.clone().add(0, 3, 0);
                Particle.DustOptions beam = new Particle.DustOptions(Color.fromRGB(150, 210, 255), 0.6f);
                for (int i = 1; i < PILLAR_OFFSETS.length; i++) {
                    Location pillar = c.clone().add(PILLAR_OFFSETS[i][0], 3, PILLAR_OFFSETS[i][1]);
                    DisplayBuilder.particleLine(center, pillar, Particle.DUST, 2, beam);
                }
            }

            // Ambient
            if (tick % 50 == 0) {
                chimeSound(c, 0.6f, 0.4f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new IcePillarCathedral(plugin); }
    }

    // ================================================================
    // 12. CRYO SPIDER WEB — Radiating ice web.
    //     Touching web line: slows to 0 movement for 40 ticks + 3 hearts.
    //     Web intersection nodes explode if player is on line (5 hearts burst).
    // ================================================================
    public static class CryoSpiderWeb extends ModelEngineAttack {
        private static final int WEB_ARMS = 8;
        private static final double WEB_RADIUS = 7.0;

        public CryoSpiderWeb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cryo_spider_web", AttackType.MODEL_ENGINE, 1, "modes/freezingice/attacks"));
            config.setDamage(6.0);       // 3 hearts web contact
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(260);
            config.setCooldownTicks(240);
        }

        @Override protected String getModelId() { return "cryo_spider_web"; }
        @Override protected double getModelScale() { return 2.0; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center);
            playAnimation("weave", 0.0, false);
            chimeSound(center, 0.8f, 1.0f);
            frostDust(center, 15, 3.0);

            // Web arm particle lines
            for (int i = 0; i < WEB_ARMS; i++) {
                double angle = (2 * Math.PI * i) / WEB_ARMS;
                Location end = center.clone().add(Math.cos(angle) * WEB_RADIUS, 0.1, Math.sin(angle) * WEB_RADIUS);
                Particle.DustOptions webDust = new Particle.DustOptions(Color.fromRGB(220, 240, 255), 0.6f);
                DisplayBuilder.particleLine(center.clone().add(0, 0.1, 0), end, Particle.DUST, 3, webDust);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (tick % 10 == 0) {
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    Location pLoc = p.getLocation();
                    double dx = pLoc.getX() - c.getX();
                    double dz = pLoc.getZ() - c.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > WEB_RADIUS || dist < 0.5) continue;

                    // Check proximity to any web arm line
                    boolean onLine = false;
                    for (int i = 0; i < WEB_ARMS; i++) {
                        double angle = (2 * Math.PI * i) / WEB_ARMS;
                        // Perpendicular distance from player to this web arm
                        double armDirX = Math.cos(angle);
                        double armDirZ = Math.sin(angle);
                        double perpDist = Math.abs(-armDirZ * dx + armDirX * dz);
                        // Along-arm distance (must be positive = on the arm)
                        double alongDist = armDirX * dx + armDirZ * dz;
                        if (perpDist <= 0.8 && alongDist > 0 && alongDist <= WEB_RADIUS) {
                            onLine = true;
                            break;
                        }
                    }

                    if (onLine) {
                        // Web contact: slow + 3 hearts
                        p.damage(6.0);
                        p.setNoDamageTicks(0);
                        p.setFreezeTicks(Math.min(p.getFreezeTicks() + 40, 140));
                        // Slow to near-zero movement
                        p.setVelocity(p.getVelocity().multiply(0.05));
                        freezeSound(p.getLocation(), 0.5f, 0.8f);

                        // Check if near a node intersection (every 3.5 blocks along arms)
                        if (dist > 2.5 && dist < 4.5) {
                            // Node explosion — 5 hearts burst
                            damageNearby(pLoc, 2.0, 10.0);
                            shatterSound(pLoc, 0.8f, 1.2f);
                            iceDust(pLoc, 10, 1.0);
                        }
                    }
                }
            }

            // Web visual refresh
            if (tick % 8 == 0) {
                Particle.DustOptions webDust = new Particle.DustOptions(Color.fromRGB(220, 240, 255), 0.4f);
                for (int i = 0; i < WEB_ARMS; i++) {
                    double angle = (2 * Math.PI * i) / WEB_ARMS;
                    Location end = c.clone().add(Math.cos(angle) * WEB_RADIUS, 0.1, Math.sin(angle) * WEB_RADIUS);
                    DisplayBuilder.particleLine(c.clone().add(0, 0.1, 0), end, Particle.DUST, 2, webDust);
                }
            }

            // Node glow at intersection rings
            if (tick % 12 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.15, 0), 3.5, Particle.END_ROD, 8, null);
            }

            // Ambient
            if (tick % 40 == 0) {
                snowStepSound(c, 0.4f, 1.5f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new CryoSpiderWeb(plugin); }
    }

    // ================================================================
    // 13. AVALANCHE FIST — Compressed ice ball slams down.
    //     Impact: 10 hearts + knockback in 6 blocks.
    //     Flying chunks: 2 hearts each. Idle orbiting chunks: 1 heart.
    // ================================================================
    public static class AvalancheFist extends ModelEngineAttack {
        private boolean impacted = false;
        private Location fistLoc;
        private double orbitAngle = 0;
        private final double[][] chunkAngles = new double[6][2]; // azimuth, elevation

        public AvalancheFist(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("avalanche_fist", AttackType.MODEL_ENGINE, 1, "modes/freezingice/attacks"));
            config.setDamage(2.0);       // 1 heart idle orbiting chunks
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(260);
            config.setCooldownTicks(240);
            config.setImpactDamage(20.0); // 10 hearts
            config.setImpactRadius(6.0);
        }

        @Override protected String getModelId() { return "avalanche_fist"; }
        @Override protected double getModelScale() { return 2.0; }

        @Override
        protected void onSpawn(Location center) {
            fistLoc = center.clone().add(0, 18, 0);
            spawnModel(fistLoc);
            playAnimation("descend", 0.0, false);
            freezeSound(center, 0.8f, 0.4f);

            // Initialize chunk orbit angles
            for (int i = 0; i < chunkAngles.length; i++) {
                chunkAngles[i][0] = (2 * Math.PI * i) / chunkAngles.length;
                chunkAngles[i][1] = Math.random() * Math.PI;
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (!impacted) {
                // Descend
                fistLoc.subtract(0, 0.6, 0);
                if (modelHost != null && modelHost.isValid()) {
                    modelHost.teleport(fistLoc);
                }
                if (tick % 2 == 0) {
                    snowflakes(fistLoc, 5, 1.5);
                }

                if (fistLoc.getY() <= c.getY() + 1.0) {
                    impacted = true;
                    fistLoc = c.clone();
                    if (modelHost != null && modelHost.isValid()) {
                        modelHost.teleport(fistLoc);
                    }
                    playAnimation("impact", 0.0, false);
                    triggerImpactDamage(c);

                    // Knockback all nearby players
                    for (Player p : c.getWorld().getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL) continue;
                        double dist = p.getLocation().distance(c);
                        if (dist <= 6.0 && dist > 0.1) {
                            Vector kb = p.getLocation().toVector().subtract(c.toVector()).normalize().multiply(1.5);
                            kb.setY(0.6);
                            p.setVelocity(kb);
                        }
                    }

                    shatterSound(c, 2.0f, 0.4f);
                    freezeSound(c, 1.5f, 0.3f);
                    deepIceDust(c, 40, 4.0);
                    DisplayBuilder.particleRing(c, 6.0, Particle.SNOWFLAKE, 30, null);

                    // Flying chunks dealing 2 hearts each
                    for (int i = 0; i < 8; i++) {
                        double angle = (2 * Math.PI * i) / 8;
                        Location chunk = c.clone().add(Math.cos(angle) * 4.0, 2.0, Math.sin(angle) * 4.0);
                        iceDust(chunk, 5, 0.5);
                        damageNearby(chunk, 1.5, 4.0);
                    }
                }
            } else {
                // Idle orbiting chunks
                orbitAngle += 0.06;
                for (int i = 0; i < chunkAngles.length; i++) {
                    chunkAngles[i][0] += 0.05 + (i * 0.01);
                    double ox = Math.cos(chunkAngles[i][0]) * 3.0;
                    double oy = 1.5 + Math.sin(chunkAngles[i][1] + tick * 0.04) * 1.0;
                    double oz = Math.sin(chunkAngles[i][0]) * 3.0;
                    Location chunkLoc = c.clone().add(ox, oy, oz);

                    // 1 heart contact damage
                    damageNearby(chunkLoc, 1.0, 2.0);

                    if (tick % 4 == 0) {
                        iceDust(chunkLoc, 1, 0.2);
                    }
                }

                // Central cold aura
                if (tick % 8 == 0) {
                    snowflakes(c.clone().add(0, 0.5, 0), 3, 1.5);
                }

                // Ambient
                if (tick % 50 == 0) {
                    snowStepSound(c, 0.5f, 0.5f);
                }
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new AvalancheFist(plugin); }
    }

    // ================================================================
    // 14. FROST WRAITH SILHOUETTE — Abstract ghost shape.
    //     Wraith drifts toward nearest player (tracking): 4 hearts overlap.
    //     Arms sweep: 3 hearts in arc. Dissipate freezes player 60 ticks.
    // ================================================================
    public static class FrostWraithSilhouette extends ModelEngineAttack {
        private double posX, posZ;
        private double armSweepAngle = 0;

        public FrostWraithSilhouette(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_wraith_silhouette", AttackType.MODEL_ENGINE, 1, "modes/freezingice/attacks"));
            config.setDamage(8.0);       // 4 hearts overlap
            config.setDamageRadius(2.0); // body overlap
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(280);
            config.setCooldownTicks(250);
            config.setTracksPlayer(true);
        }

        @Override protected String getModelId() { return "frost_wraith_silhouette"; }
        @Override protected double getModelScale() { return 1.8; }

        @Override
        protected void onSpawn(Location center) {
            posX = center.getX();
            posZ = center.getZ();
            spawnModel(center);
            playAnimation("manifest", 0.0, false);
            freezeSound(center, 1.2f, 0.2f);
            frostDust(center, 20, 2.0);
            endRods(center, 10, 1.5);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Drift toward nearest player
            Player nearest = findNearestPlayer(c, 20.0);
            if (nearest != null) {
                double dx = nearest.getLocation().getX() - posX;
                double dz = nearest.getLocation().getZ() - posZ;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 1.0) {
                    posX += (dx / dist) * 0.18;
                    posZ += (dz / dist) * 0.18;
                }
            }

            Location wraithLoc = new Location(c.getWorld(), posX, c.getY() + 0.5, posZ);
            wraithLoc.setYaw(0);
            wraithLoc.setPitch(0);
            if (modelHost != null && modelHost.isValid()) {
                modelHost.teleport(wraithLoc);
            }
            setCenter(wraithLoc);

            // Body overlap damage — 4 hearts
            // Handled by config damage radius

            // Arms sweep — 3 hearts in arc
            armSweepAngle += 0.12;
            for (int arm = 0; arm < 2; arm++) {
                double baseAngle = armSweepAngle + (arm * Math.PI);
                double sweepWidth = Math.PI / 3; // 60 degree arc
                for (int i = 0; i < 3; i++) {
                    double angle = baseAngle + (i - 1) * (sweepWidth / 2);
                    double ax = Math.cos(angle) * 2.5;
                    double az = Math.sin(angle) * 2.5;
                    Location armTip = wraithLoc.clone().add(ax, 0.5, az);

                    damageNearby(armTip, 1.2, 6.0); // 3 hearts

                    if (tick % 3 == 0 && i == 1) {
                        frostDust(armTip, 2, 0.2);
                    }
                }
            }

            // Ghost trail particles
            if (tick % 2 == 0) {
                frostDust(wraithLoc, 3, 0.8);
                endRods(wraithLoc.clone().add(0, 1.0, 0), 1, 0.4);
            }

            // Eerie sound
            if (tick % 35 == 0) {
                freezeSound(wraithLoc, 0.6f, 0.15f);
            }
        }

        @Override
        protected void onModelCleanup() {
            // Dissipate — freeze nearby players for 60 ticks
            Location c = getCenter();
            if (c != null && c.getWorld() != null) {
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    if (p.getLocation().distanceSquared(c) <= 25.0) { // 5 block range
                        p.setFreezeTicks(Math.min(p.getFreezeTicks() + 60, 140));
                        freezeSound(p.getLocation(), 0.8f, 0.4f);
                        frostDust(p.getLocation(), 10, 0.5);
                    }
                }
                shatterSound(c, 1.0f, 1.5f);
                endRods(c, 15, 2.0);
            }
        }

        @Override public AbstractAttack newInstance() { return new FrostWraithSilhouette(plugin); }
    }

    // ================================================================
    // 15. CRYSTAL HOURGLASS — Ice hourglass with falling particles.
    //     Top cone drains 1 heart/sec above midpoint.
    //     Bottom cone drains 1 heart/sec below midpoint.
    //     Exact center = 5 hearts burst.
    // ================================================================
    public static class CrystalHourglass extends ModelEngineAttack {
        private static final double MID_HEIGHT = 3.0;

        public CrystalHourglass(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_hourglass", AttackType.MODEL_ENGINE, 1, "modes/freezingice/attacks"));
            config.setDamage(2.0);       // 1 heart/sec cone drain
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
        }

        @Override protected String getModelId() { return "crystal_hourglass"; }
        @Override protected double getModelScale() { return 2.0; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center);
            playAnimation("start", 0.0, false);
            chimeSound(center, 1.0f, 0.6f);
            freezeSound(center, 0.6f, 0.5f);
            endRods(center.clone().add(0, MID_HEIGHT, 0), 20, 1.5);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            Location midpoint = c.clone().add(0, MID_HEIGHT, 0);

            if (tick % 20 == 0) {
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    double horizDist = Math.sqrt(
                            Math.pow(p.getLocation().getX() - c.getX(), 2) +
                            Math.pow(p.getLocation().getZ() - c.getZ(), 2)
                    );
                    if (horizDist > 4.0) continue;

                    double playerY = p.getLocation().getY();
                    double midY = midpoint.getY();
                    double vertDist = Math.abs(playerY - midY);

                    // Exact center check (within 1 block of midpoint)
                    if (vertDist <= 1.0 && horizDist <= 1.0) {
                        p.damage(10.0); // 5 hearts burst
                        p.setNoDamageTicks(0);
                        shatterSound(p.getLocation(), 1.0f, 0.8f);
                        deepIceDust(p.getLocation(), 15, 0.5);
                        endRods(p.getLocation(), 5, 0.3);
                    } else if (playerY > midY && vertDist <= 4.0) {
                        // Top cone — drain 1 heart/sec
                        p.damage(2.0);
                        p.setNoDamageTicks(0);
                        frostDust(p.getLocation(), 3, 0.3);
                    } else if (playerY < midY && vertDist <= 4.0) {
                        // Bottom cone — drain 1 heart/sec
                        p.damage(2.0);
                        p.setNoDamageTicks(0);
                        iceDust(p.getLocation(), 3, 0.3);
                    }
                }
            }

            // Falling sand particles through the neck
            if (tick % 2 == 0) {
                Location neck = midpoint.clone().add(
                        (Math.random() - 0.5) * 0.3, 0, (Math.random() - 0.5) * 0.3);
                frostDust(neck, 2, 0.1);
                endRods(neck, 1, 0.05);
            }

            // Top/bottom cone aura
            if (tick % 10 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, MID_HEIGHT + 2, 0), 2.0, Particle.SNOWFLAKE, 8, null);
                DisplayBuilder.particleRing(c.clone().add(0, MID_HEIGHT - 2, 0), 2.0, Particle.SNOWFLAKE, 8, null);
                DisplayBuilder.particleRing(midpoint, 0.5, Particle.END_ROD, 4, null);
            }

            // Ambient
            if (tick % 45 == 0) {
                chimeSound(midpoint, 0.4f, 0.8f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new CrystalHourglass(plugin); }
    }

    // ================================================================
    // 16. POLAR VORTEX RING — Spinning horizontal ring with funnel.
    //     Funnel sucks players inward (velocity). Ring edge: 4 hearts contact.
    //     Center of vortex: 6 hearts/sec.
    // ================================================================
    public static class PolarVortexRing extends ModelEngineAttack {
        private double ringAngle = 0;

        public PolarVortexRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("polar_vortex_ring", AttackType.MODEL_ENGINE, 1, "modes/freezingice/attacks"));
            config.setDamage(12.0);      // 6 hearts/sec center
            config.setDamageRadius(1.5); // center zone
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(240);
            config.setCooldownTicks(220);
        }

        @Override protected String getModelId() { return "polar_vortex_ring"; }
        @Override protected double getModelScale() { return 2.0; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center.clone().add(0, 1, 0));
            playAnimation("spin", 0.0, true);
            freezeSound(center, 1.0f, 0.3f);
            snowflakes(center, 25, 4.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            Location vortexCenter = c.clone().add(0, 1, 0);
            ringAngle += 0.15;

            // Pull players toward center (funnel effect)
            for (Player p : c.getWorld().getPlayers()) {
                if (p.getGameMode() != GameMode.SURVIVAL) continue;
                double dist = p.getLocation().distance(vortexCenter);
                if (dist <= 8.0 && dist > 0.5) {
                    Vector pull = vortexCenter.toVector().subtract(p.getLocation().toVector()).normalize();
                    double strength = 0.12 * (1.0 - (dist / 8.0)); // Stronger near center
                    p.setVelocity(p.getVelocity().add(pull.multiply(strength)));
                }
            }

            // Ring edge damage — 4 hearts on contact
            double ringRadius = 4.0;
            for (int i = 0; i < 12; i++) {
                double angle = ringAngle + (i * Math.PI / 6);
                Location edgePoint = vortexCenter.clone().add(
                        Math.cos(angle) * ringRadius, 0, Math.sin(angle) * ringRadius);
                damageNearby(edgePoint, 1.0, 8.0); // 4 hearts
            }

            // Visual spinning ring
            if (tick % 3 == 0) {
                Particle.DustOptions vortexDust = new Particle.DustOptions(Color.fromRGB(100, 180, 255), 1.2f);
                DisplayBuilder.particleRing(vortexCenter, ringRadius, Particle.DUST, 20, vortexDust);
            }

            // Center danger zone particles
            if (tick % 4 == 0) {
                deepIceDust(vortexCenter, 5, 0.5);
                endRods(vortexCenter, 2, 0.3);
            }

            // Funnel wind particles spiraling inward
            if (tick % 2 == 0) {
                double spiralAngle = ringAngle * 3;
                for (double r = 7.0; r > 1.0; r -= 2.0) {
                    Location spiralPoint = vortexCenter.clone().add(
                            Math.cos(spiralAngle + r) * r, -0.2 * (8 - r), Math.sin(spiralAngle + r) * r);
                    snowflakes(spiralPoint, 1, 0.2);
                }
            }

            // Ambient
            if (tick % 30 == 0) {
                freezeSound(vortexCenter, 0.7f, 0.5f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new PolarVortexRing(plugin); }
    }

    // ================================================================
    // 17. ICE MIRROR — Ornate mirror.
    //     "Reflection" copies player's last damage source, re-applies as cold.
    //     Mirror shattering: 8 hearts in 5 blocks.
    // ================================================================
    public static class IceMirror extends ModelEngineAttack {
        private final Map<UUID, Double> lastDamageTracker = new HashMap<>();

        public IceMirror(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_mirror", AttackType.MODEL_ENGINE, 1, "modes/freezingice/attacks"));
            config.setDamage(4.0);       // 2 hearts reflection minimum
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(40);
            config.setDurationTicks(240);
            config.setCooldownTicks(260);
        }

        @Override protected String getModelId() { return "ice_mirror"; }
        @Override protected double getModelScale() { return 1.5; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center.clone().add(0, 1.5, 0));
            playAnimation("appear", 0.0, false);
            chimeSound(center, 1.2f, 0.7f);
            shatterSound(center, 0.5f, 1.5f);
            endRods(center.clone().add(0, 2.0, 0), 15, 1.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            Location mirrorFace = c.clone().add(0, 2.5, 0);

            // Reflection damage: track each player's HP changes, mirror them back
            if (tick % 40 == 0) {
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    if (p.getLocation().distanceSquared(c) > 36.0) continue; // 6^2

                    double currentHealth = p.getHealth();
                    Double lastHealth = lastDamageTracker.get(p.getUniqueId());

                    if (lastHealth != null && lastHealth > currentHealth) {
                        // Player took damage since last check — reflect it
                        double damageTaken = lastHealth - currentHealth;
                        double reflectedDamage = Math.max(4.0, damageTaken); // Minimum 2 hearts
                        p.damage(reflectedDamage);
                        p.setNoDamageTicks(0);

                        // Mirror flash effect
                        endRods(mirrorFace, 8, 0.5);
                        frostDust(p.getLocation(), 8, 0.5);
                        freezeSound(p.getLocation(), 0.6f, 0.8f);

                        // Reflection beam from mirror to player
                        Particle.DustOptions beam = new Particle.DustOptions(Color.fromRGB(220, 240, 255), 0.8f);
                        DisplayBuilder.particleLine(mirrorFace, p.getLocation().add(0, 1, 0), Particle.DUST, 3, beam);
                    }
                    lastDamageTracker.put(p.getUniqueId(), currentHealth);
                }
            }

            // Mirror surface shimmer
            if (tick % 5 == 0) {
                Location shimmer = mirrorFace.clone().add(
                        (Math.random() - 0.5) * 1.5, (Math.random() - 0.5) * 2.0, 0.1);
                endRods(shimmer, 2, 0.1);
                frostDust(shimmer, 1, 0.2);
            }

            // Mirror frame frost
            if (tick % 8 == 0) {
                iceDust(mirrorFace.clone().add(0, 1.5, 0), 2, 0.5);
                iceDust(mirrorFace.clone().add(0, -1.5, 0), 2, 0.5);
                iceDust(mirrorFace.clone().add(1.0, 0, 0), 2, 0.5);
                iceDust(mirrorFace.clone().add(-1.0, 0, 0), 2, 0.5);
            }

            // Ambient
            if (tick % 50 == 0) {
                chimeSound(mirrorFace, 0.5f, 1.0f);
            }
        }

        @Override
        protected void onModelCleanup() {
            // Mirror shatter — 8 hearts in 5 blocks
            Location c = getCenter();
            if (c != null && c.getWorld() != null) {
                Location mirrorFace = c.clone().add(0, 2.5, 0);
                damageNearby(mirrorFace, 5.0, 16.0);
                shatterSound(mirrorFace, 2.0f, 1.0f);
                // Glass shard explosion
                for (int i = 0; i < 20; i++) {
                    Location shard = mirrorFace.clone().add(
                            (Math.random() - 0.5) * 6, (Math.random() - 0.5) * 4, (Math.random() - 0.5) * 6);
                    frostDust(shard, 3, 0.3);
                    endRods(shard, 1, 0.1);
                }
            }
            lastDamageTracker.clear();
        }

        @Override public AbstractAttack newInstance() { return new IceMirror(plugin); }
    }

    // ================================================================
    // 18. GLACIER FANG MAW — Upper/lower ice jaws.
    //     Jaws clamp shut every 80 ticks: 15 hearts to anyone between them.
    //     Dripping crystals: 1 heart on contact.
    // ================================================================
    public static class GlacierFangMaw extends ModelEngineAttack {
        private boolean jawsOpen = true;
        private int jawTimer = 0;

        public GlacierFangMaw(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacier_fang_maw", AttackType.MODEL_ENGINE, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);      // 15 hearts clamp
            config.setDamageRadius(3.0); // between jaws
            config.setTicksBetweenDamage(80);
            config.setDurationTicks(400);
            config.setCooldownTicks(350);
        }

        @Override protected String getModelId() { return "glacier_fang_maw"; }
        @Override protected double getModelScale() { return 2.5; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center);
            playAnimation("open", 0.0, false);
            shatterSound(center, 1.5f, 0.3f);
            freezeSound(center, 1.0f, 0.2f);
            deepIceDust(center, 25, 2.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            Location mawCenter = c.clone().add(0, 2, 0);
            jawTimer++;

            // Jaw clamp cycle
            if (jawTimer >= 80) {
                jawTimer = 0;
                if (jawsOpen) {
                    // CLAMP SHUT — devastating damage
                    jawsOpen = false;
                    playAnimation("bite", 0.0, false);
                    shatterSound(c, 2.0f, 0.4f);
                    freezeSound(c, 1.5f, 0.3f);

                    // 15 hearts to anyone between the jaws
                    for (Player p : c.getWorld().getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL) continue;
                        double dist = p.getLocation().distance(mawCenter);
                        if (dist <= 3.0) {
                            p.damage(30.0); // 15 hearts
                            p.setNoDamageTicks(0);
                            deepIceDust(p.getLocation(), 15, 0.5);
                        }
                    }
                    // Clamp visual
                    for (int i = 0; i < 10; i++) {
                        double angle = (2 * Math.PI * i) / 10;
                        Location shockPoint = mawCenter.clone().add(Math.cos(angle) * 2.0, 0, Math.sin(angle) * 2.0);
                        iceDust(shockPoint, 3, 0.3);
                    }
                } else {
                    // Reopen jaws
                    jawsOpen = true;
                    playAnimation("open", 0.0, false);
                    snowStepSound(c, 0.8f, 0.4f);
                }
            }

            // Dripping crystals from upper jaw
            if (tick % 15 == 0 && jawsOpen) {
                double dripX = (Math.random() - 0.5) * 4.0;
                double dripZ = (Math.random() - 0.5) * 4.0;
                Location drip = mawCenter.clone().add(dripX, 2.0, dripZ);

                // Crystal falls down
                for (double y = 2.0; y >= 0; y -= 0.5) {
                    Location dropPoint = mawCenter.clone().add(dripX, y, dripZ);
                    frostDust(dropPoint, 1, 0.1);
                }

                // Contact damage at landing — 1 heart
                Location landing = mawCenter.clone().add(dripX, 0, dripZ);
                damageNearby(landing, 1.0, 2.0);
            }

            // Jaw edge frost
            if (tick % 6 == 0) {
                for (int i = 0; i < 6; i++) {
                    double angle = (Math.PI * i) / 5; // Upper half arc = teeth
                    Location fang = mawCenter.clone().add(Math.cos(angle) * 2.5, 2.0, Math.sin(angle) * 2.5);
                    iceDust(fang, 1, 0.1);
                    Location lowerFang = mawCenter.clone().add(Math.cos(angle) * 2.5, -0.5, Math.sin(angle) * 2.5);
                    deepIceDust(lowerFang, 1, 0.1);
                }
            }

            // Ambient
            if (tick % 40 == 0) {
                freezeSound(c, 0.5f, 0.2f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new GlacierFangMaw(plugin); }
    }

    // ================================================================
    // 19. ARCTIC COMPASS ROSE — 8 directional spikes.
    //     Each spike fires a cold beam every 60 ticks in its direction
    //     (3 hearts, 10-block range). Hub rotates, sweeping beams.
    // ================================================================
    public static class ArcticCompassRose extends ModelEngineAttack {
        private double hubRotation = 0;

        public ArcticCompassRose(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("arctic_compass_rose", AttackType.MODEL_ENGINE, 1, "modes/freezingice/attacks"));
            config.setDamage(6.0);       // 3 hearts per beam hit
            config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(60);
            config.setDurationTicks(360);
            config.setCooldownTicks(320);
        }

        @Override protected String getModelId() { return "arctic_compass_rose"; }
        @Override protected double getModelScale() { return 1.8; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center.clone().add(0, 0.5, 0));
            playAnimation("activate", 0.0, false);
            chimeSound(center, 1.0f, 0.5f);
            shatterSound(center, 0.7f, 1.0f);
            endRods(center.clone().add(0, 1.0, 0), 15, 2.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            Location hub = c.clone().add(0, 1.0, 0);
            hubRotation += 0.02;

            // Fire beams every 60 ticks
            if (tick % 60 == 0) {
                playAnimation("fire", 0.0, false);
                shatterSound(hub, 1.0f, 1.2f);

                for (int i = 0; i < 8; i++) {
                    double angle = hubRotation + (i * Math.PI / 4);
                    double dirX = Math.cos(angle);
                    double dirZ = Math.sin(angle);

                    Location beamEnd = hub.clone().add(dirX * 10, 0, dirZ * 10);

                    // Beam visual
                    Particle.DustOptions beamDust = new Particle.DustOptions(Color.fromRGB(100, 180, 255), 1.0f);
                    DisplayBuilder.particleLine(hub, beamEnd, Particle.DUST, 4, beamDust);

                    // Damage along beam line — 3 hearts
                    for (Player p : c.getWorld().getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL) continue;
                        Location pLoc = p.getLocation();
                        double dx = pLoc.getX() - hub.getX();
                        double dz = pLoc.getZ() - hub.getZ();
                        // Perpendicular distance to beam line
                        double perpDist = Math.abs(-dirZ * dx + dirX * dz);
                        double alongDist = dirX * dx + dirZ * dz;
                        if (perpDist <= 1.0 && alongDist > 0 && alongDist <= 10.0) {
                            p.damage(6.0); // 3 hearts
                            p.setNoDamageTicks(0);
                            p.setFreezeTicks(Math.min(p.getFreezeTicks() + 40, 140));
                            iceDust(pLoc, 8, 0.5);
                        }
                    }
                }
            }

            // Rotating spike tips visual
            if (tick % 4 == 0) {
                for (int i = 0; i < 8; i++) {
                    double angle = hubRotation + (i * Math.PI / 4);
                    Location tipLoc = hub.clone().add(Math.cos(angle) * 3.0, 0, Math.sin(angle) * 3.0);
                    iceDust(tipLoc, 1, 0.1);
                }
            }

            // Hub glow
            if (tick % 8 == 0) {
                endRods(hub, 3, 0.3);
                DisplayBuilder.particleRing(hub, 1.0, Particle.END_ROD, 4, null);
            }

            // Ambient
            if (tick % 45 == 0) {
                chimeSound(hub, 0.4f, 0.6f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new ArcticCompassRose(plugin); }
    }

    // ================================================================
    // 20. FROZEN CHANDELIER — Hanging chandelier.
    //     Stalactites drop every 40 ticks targeting below (4 hearts impact).
    //     Swaying creates cold wind zones (2 hearts in path).
    // ================================================================
    public static class FrozenChandelier extends ModelEngineAttack {
        private double swayAngle = 0;
        private double swayX = 0, swayZ = 0;

        public FrozenChandelier(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_chandelier", AttackType.MODEL_ENGINE, 1, "modes/freezingice/attacks"));
            config.setDamage(4.0);       // 2 hearts wind zone
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(320);
            config.setCooldownTicks(280);
        }

        @Override protected String getModelId() { return "frozen_chandelier"; }
        @Override protected double getModelScale() { return 2.0; }

        @Override
        protected void onSpawn(Location center) {
            Location hangLoc = center.clone().add(0, 8, 0);
            spawnModel(hangLoc);
            playAnimation("sway", 0.0, true);
            chimeSound(center, 1.2f, 0.8f);
            endRods(hangLoc, 20, 2.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            swayAngle += 0.04;
            double prevSwayX = swayX;
            double prevSwayZ = swayZ;
            swayX = Math.sin(swayAngle) * 2.0;
            swayZ = Math.cos(swayAngle * 0.7) * 1.5;

            Location chandelierPos = c.clone().add(swayX, 8, swayZ);

            // Stalactite drops every 40 ticks
            if (tick % 40 == 0) {
                // Target a player below
                Player target = findNearestPlayer(chandelierPos, 10.0);
                Location dropTarget = (target != null) ? target.getLocation().clone() : chandelierPos.clone().add(0, -8, 0);

                // Stalactite drop visual — particles falling from chandelier to ground
                shatterSound(chandelierPos, 0.8f, 1.4f);
                for (double y = 8; y >= 0; y -= 0.5) {
                    Location fallPoint = dropTarget.clone().add(
                            (chandelierPos.getX() - dropTarget.getX()) * (y / 8.0),
                            y,
                            (chandelierPos.getZ() - dropTarget.getZ()) * (y / 8.0)
                    );
                    iceDust(fallPoint, 2, 0.2);
                }

                // Impact damage — 4 hearts at drop target
                damageNearby(dropTarget, 2.5, 8.0);
                freezeSound(dropTarget, 0.6f, 0.6f);
                deepIceDust(dropTarget, 8, 0.5);
            }

            // Cold wind zone along sway path — 2 hearts
            Location windZone = c.clone().add(swayX, 2, swayZ);
            if (tick % 10 == 0) {
                damageNearby(windZone, 3.0, 4.0);
            }

            // Wind particles along sway direction
            if (tick % 3 == 0) {
                double windDirX = swayX - prevSwayX;
                double windDirZ = swayZ - prevSwayZ;
                for (int i = 0; i < 3; i++) {
                    Location windParticle = windZone.clone().add(
                            windDirX * i * 2, (Math.random() - 0.5) * 3, windDirZ * i * 2);
                    snowflakes(windParticle, 2, 0.5);
                }
            }

            // Chandelier sparkle
            if (tick % 6 == 0) {
                endRods(chandelierPos, 3, 1.5);
                frostDust(chandelierPos, 2, 1.0);
            }

            // Ambient
            if (tick % 35 == 0) {
                chimeSound(chandelierPos, 0.5f, 1.2f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new FrozenChandelier(plugin); }
    }

    // ================================================================
    // 21. PERMAFROST NOVA — 3-layer shard sphere burst.
    //     Layer 1 at 3 blocks (3 hearts), Layer 2 at 6 blocks (5 hearts),
    //     Layer 3 at 9 blocks (7 hearts). 20 ticks between layers.
    // ================================================================
    public static class PermafrostNova extends ModelEngineAttack {
        private boolean layer1Fired = false;
        private boolean layer2Fired = false;
        private boolean layer3Fired = false;
        private int chargeUpTicks = 40; // charge before first layer

        public PermafrostNova(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("permafrost_nova", AttackType.MODEL_ENGINE, 1, "modes/freezingice/attacks"));
            config.setDamage(6.0);       // 3 hearts layer 1
            config.setDamageRadius(9.0); // outer range
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }

        @Override protected String getModelId() { return "permafrost_nova"; }
        @Override protected double getModelScale() { return 1.5; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center.clone().add(0, 2, 0));
            playAnimation("charge", 0.0, false);
            freezeSound(center, 1.0f, 0.3f);
            chimeSound(center, 0.8f, 0.4f);
            // Charging particles — pull inward
            for (int i = 0; i < 16; i++) {
                double angle = (2 * Math.PI * i) / 16;
                Location pullPoint = center.clone().add(Math.cos(angle) * 5, 2, Math.sin(angle) * 5);
                endRods(pullPoint, 2, 0.2);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            Location novaCenter = c.clone().add(0, 2, 0);

            // Charge-up phase
            if (tick < chargeUpTicks) {
                if (tick % 4 == 0) {
                    double shrinkRadius = 5.0 * (1.0 - ((double) tick / chargeUpTicks));
                    DisplayBuilder.particleRing(novaCenter, shrinkRadius, Particle.END_ROD, 12, null);
                    deepIceDust(novaCenter, 3, 0.5);
                }
                return;
            }

            int novaTime = tick - chargeUpTicks;

            // Layer 1 — 3 blocks, 3 hearts
            if (!layer1Fired && novaTime >= 0) {
                layer1Fired = true;
                playAnimation("burst_1", 0.0, false);
                shatterSound(novaCenter, 1.2f, 0.8f);
                DisplayBuilder.particleRing(novaCenter, 3.0, Particle.SNOWFLAKE, 20, null);
                Particle.DustOptions l1Dust = new Particle.DustOptions(Color.fromRGB(220, 240, 255), 1.5f);
                DisplayBuilder.particleRing(novaCenter, 3.0, Particle.DUST, 16, l1Dust);
                damageNearby(novaCenter, 3.0, 6.0); // 3 hearts
            }

            // Layer 2 — 6 blocks, 5 hearts (20 ticks later)
            if (!layer2Fired && novaTime >= 20) {
                layer2Fired = true;
                playAnimation("burst_2", 0.0, false);
                shatterSound(novaCenter, 1.5f, 0.6f);
                DisplayBuilder.particleRing(novaCenter, 6.0, Particle.SNOWFLAKE, 30, null);
                Particle.DustOptions l2Dust = new Particle.DustOptions(Color.fromRGB(100, 180, 255), 1.8f);
                DisplayBuilder.particleRing(novaCenter, 6.0, Particle.DUST, 24, l2Dust);
                damageNearby(novaCenter, 6.0, 10.0); // 5 hearts
            }

            // Layer 3 — 9 blocks, 7 hearts (40 ticks from start)
            if (!layer3Fired && novaTime >= 40) {
                layer3Fired = true;
                playAnimation("burst_3", 0.0, false);
                shatterSound(novaCenter, 2.0f, 0.4f);
                freezeSound(novaCenter, 1.5f, 0.2f);
                DisplayBuilder.particleRing(novaCenter, 9.0, Particle.SNOWFLAKE, 40, null);
                Particle.DustOptions l3Dust = new Particle.DustOptions(Color.fromRGB(30, 80, 160), 2.0f);
                DisplayBuilder.particleRing(novaCenter, 9.0, Particle.DUST, 32, l3Dust);
                damageNearby(novaCenter, 9.0, 14.0); // 7 hearts

                // Freeze all hit players
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    if (p.getLocation().distanceSquared(novaCenter) <= 81.0) { // 9^2
                        p.setFreezeTicks(Math.min(p.getFreezeTicks() + 80, 140));
                    }
                }
            }

            // Post-nova residual frost
            if (layer3Fired && tick % 8 == 0) {
                snowflakes(novaCenter, 5, 4.0);
                frostDust(novaCenter, 3, 3.0);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new PermafrostNova(plugin); }
    }

    // ================================================================
    // 22. ICE OBELISK TRIO — 3 obelisks in triangle.
    //     Beams connecting obelisks: 4 hearts crossing.
    //     Inside triangle: 3 hearts/sec amplified cold.
    //     Obelisks pulse, temporarily disabling beams.
    // ================================================================
    public static class IceObeliskTrio extends ModelEngineAttack {
        private static final double TRIANGLE_RADIUS = 5.0;
        private final double[][] obeliskOffsets = new double[3][2];
        private boolean beamsActive = true;
        private int pulseTimer = 0;

        public IceObeliskTrio(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_obelisk_trio", AttackType.MODEL_ENGINE, 1, "modes/freezingice/attacks"));
            config.setDamage(6.0);       // 3 hearts/sec inside triangle
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(320);
            config.setCooldownTicks(300);
        }

        @Override protected String getModelId() { return "ice_obelisk_trio"; }
        @Override protected double getModelScale() { return 2.0; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center);
            playAnimation("rise", 0.0, false);

            // Calculate triangle vertices
            for (int i = 0; i < 3; i++) {
                double angle = (2 * Math.PI * i) / 3 - Math.PI / 2;
                obeliskOffsets[i][0] = Math.cos(angle) * TRIANGLE_RADIUS;
                obeliskOffsets[i][1] = Math.sin(angle) * TRIANGLE_RADIUS;
            }

            shatterSound(center, 1.2f, 0.5f);
            freezeSound(center, 1.0f, 0.3f);

            // Obelisk eruption particles
            for (double[] off : obeliskOffsets) {
                Location obelisk = center.clone().add(off[0], 0, off[1]);
                deepIceDust(obelisk, 10, 1.0);
                endRods(obelisk, 5, 0.5);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            pulseTimer++;

            // Beam disable pulse every 80 ticks (disabled for 15 ticks)
            if (pulseTimer % 80 == 0) {
                beamsActive = false;
                playAnimation("pulse", 0.0, false);
                chimeSound(c, 1.0f, 0.3f);
                for (double[] off : obeliskOffsets) {
                    Location obelisk = c.clone().add(off[0], 0, off[1]);
                    endRods(obelisk.clone().add(0, 3, 0), 10, 0.5);
                }
            }
            if (pulseTimer % 80 == 15) {
                beamsActive = true;
                playAnimation("active", 0.0, false);
                freezeSound(c, 0.8f, 0.5f);
            }

            // Beam damage — 4 hearts crossing any beam line
            if (beamsActive && tick % 10 == 0) {
                for (int i = 0; i < 3; i++) {
                    int j = (i + 1) % 3;
                    Location a = c.clone().add(obeliskOffsets[i][0], 1.5, obeliskOffsets[i][1]);
                    Location b = c.clone().add(obeliskOffsets[j][0], 1.5, obeliskOffsets[j][1]);

                    for (Player p : c.getWorld().getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL) continue;
                        // Distance from player to line segment a-b
                        double dist = distToSegment(p.getLocation(), a, b);
                        if (dist <= 1.0) {
                            p.damage(8.0); // 4 hearts
                            p.setNoDamageTicks(0);
                            iceDust(p.getLocation(), 5, 0.3);
                            freezeSound(p.getLocation(), 0.3f, 0.8f);
                        }
                    }
                }
            }

            // Inside triangle amplified cold — 3 hearts/sec
            if (tick % 20 == 0) {
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    if (isInsideTriangle(p.getLocation(), c)) {
                        p.damage(6.0); // 3 hearts
                        p.setNoDamageTicks(0);
                        p.setFreezeTicks(Math.min(p.getFreezeTicks() + 40, 140));
                        deepIceDust(p.getLocation(), 5, 0.3);
                    }
                }
            }

            // Beam visuals
            if (beamsActive && tick % 5 == 0) {
                Particle.DustOptions beamDust = new Particle.DustOptions(Color.fromRGB(100, 180, 255), 0.8f);
                for (int i = 0; i < 3; i++) {
                    int j = (i + 1) % 3;
                    Location a = c.clone().add(obeliskOffsets[i][0], 1.5, obeliskOffsets[i][1]);
                    Location b = c.clone().add(obeliskOffsets[j][0], 1.5, obeliskOffsets[j][1]);
                    DisplayBuilder.particleLine(a, b, Particle.DUST, 3, beamDust);
                }
            }

            // Obelisk ambient glow
            if (tick % 8 == 0) {
                for (double[] off : obeliskOffsets) {
                    Location top = c.clone().add(off[0], 4, off[1]);
                    endRods(top, 2, 0.3);
                }
            }

            // Ambient
            if (tick % 50 == 0) {
                chimeSound(c, 0.4f, 0.5f);
            }
        }

        /** Distance from a point to a line segment (XZ plane). */
        private double distToSegment(Location point, Location a, Location b) {
            double dx = b.getX() - a.getX();
            double dz = b.getZ() - a.getZ();
            double len2 = dx * dx + dz * dz;
            if (len2 == 0) return point.distance(a);
            double t = ((point.getX() - a.getX()) * dx + (point.getZ() - a.getZ()) * dz) / len2;
            t = Math.max(0, Math.min(1, t));
            double projX = a.getX() + t * dx;
            double projZ = a.getZ() + t * dz;
            double pdx = point.getX() - projX;
            double pdz = point.getZ() - projZ;
            return Math.sqrt(pdx * pdx + pdz * pdz);
        }

        /** Check if point is inside the triangle (XZ plane). */
        private boolean isInsideTriangle(Location point, Location center) {
            double px = point.getX() - center.getX();
            double pz = point.getZ() - center.getZ();

            // Use barycentric coordinate test
            for (int i = 0; i < 3; i++) {
                int j = (i + 1) % 3;
                double ax = obeliskOffsets[i][0], az = obeliskOffsets[i][1];
                double bx = obeliskOffsets[j][0], bz = obeliskOffsets[j][1];
                double cross = (bx - ax) * (pz - az) - (bz - az) * (px - ax);
                if (cross > 0) return false;
            }
            return true;
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new IceObeliskTrio(plugin); }
    }

    // ================================================================
    // 23. CRYOGENIC SATELLITE — Floating sci-fi device.
    //     Wings charge, fire targeted cold laser at nearest player
    //     (8 hearts, narrow line). 100 tick cooldown. Ring spins up.
    // ================================================================
    public static class CryogenicSatellite extends ModelEngineAttack {
        private int fireCooldown = 60; // Initial charge time
        private boolean charging = false;
        private int chargeTimer = 0;
        private double ringSpeed = 0;

        public CryogenicSatellite(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cryogenic_satellite", AttackType.MODEL_ENGINE, 1, "modes/freezingice/attacks"));
            config.setDamage(16.0);      // 8 hearts laser
            config.setDamageRadius(12.0); // laser range
            config.setTicksBetweenDamage(100);
            config.setDurationTicks(400);
            config.setCooldownTicks(350);
        }

        @Override protected String getModelId() { return "cryogenic_satellite"; }
        @Override protected double getModelScale() { return 1.5; }

        @Override
        protected void onSpawn(Location center) {
            Location floatLoc = center.clone().add(0, 10, 0);
            spawnModel(floatLoc);
            playAnimation("deploy", 0.0, false);
            chimeSound(center, 0.8f, 0.6f);
            endRods(floatLoc, 15, 2.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            Location satPos = c.clone().add(0, 10, 0);
            fireCooldown--;

            // Spin-up ring visual as cooldown approaches
            if (fireCooldown <= 30 && fireCooldown > 0) {
                charging = true;
                chargeTimer++;
                ringSpeed = Math.min(ringSpeed + 0.01, 0.3);

                // Charging particles
                if (chargeTimer % 3 == 0) {
                    Particle.DustOptions chargeDust = new Particle.DustOptions(Color.fromRGB(30, 80, 160), 1.5f);
                    DisplayBuilder.particleRing(satPos, 2.0 * (1.0 - (double) chargeTimer / 30), Particle.DUST, 10, chargeDust);
                    endRods(satPos, 2, 0.5);
                }
                if (chargeTimer % 5 == 0) {
                    chimeSound(satPos, 0.3f + (chargeTimer * 0.02f), 0.5f + (chargeTimer * 0.03f));
                }
            }

            // FIRE laser
            if (fireCooldown <= 0) {
                fireCooldown = 100;
                charging = false;
                chargeTimer = 0;
                ringSpeed = 0;

                Player target = findNearestPlayer(satPos, 15.0);
                if (target != null) {
                    playAnimation("fire", 0.0, false);
                    Location targetLoc = target.getLocation().clone().add(0, 1, 0);

                    // Laser beam visual
                    Particle.DustOptions laserDust = new Particle.DustOptions(Color.fromRGB(100, 180, 255), 1.5f);
                    DisplayBuilder.particleLine(satPos, targetLoc, Particle.DUST, 6, laserDust);
                    DisplayBuilder.particleLine(satPos, targetLoc, Particle.END_ROD, 3, null);

                    // Laser damage — 8 hearts to target and anyone in the line
                    for (Player p : c.getWorld().getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL) continue;
                        // Check proximity to laser line
                        double dist = distToLine(p.getLocation(), satPos, targetLoc);
                        if (dist <= 1.0) {
                            p.damage(16.0); // 8 hearts
                            p.setNoDamageTicks(0);
                            p.setFreezeTicks(Math.min(p.getFreezeTicks() + 60, 140));
                            deepIceDust(p.getLocation(), 10, 0.5);
                        }
                    }

                    // Impact at target
                    shatterSound(targetLoc, 1.5f, 0.8f);
                    freezeSound(targetLoc, 1.0f, 0.4f);
                    iceDust(targetLoc, 15, 1.0);

                    // Firing sound at satellite
                    shatterSound(satPos, 1.0f, 1.5f);
                }
            }

            // Idle satellite visual
            if (tick % 6 == 0 && !charging) {
                endRods(satPos, 2, 1.0);
                frostDust(satPos, 1, 0.5);
            }

            // Ambient
            if (tick % 60 == 0 && !charging) {
                chimeSound(satPos, 0.3f, 0.8f);
            }
        }

        /** Distance from point to line segment in 3D. */
        private double distToLine(Location point, Location a, Location b) {
            double abx = b.getX() - a.getX(), aby = b.getY() - a.getY(), abz = b.getZ() - a.getZ();
            double apx = point.getX() - a.getX(), apy = point.getY() - a.getY(), apz = point.getZ() - a.getZ();
            double ab2 = abx * abx + aby * aby + abz * abz;
            if (ab2 == 0) return point.distance(a);
            double t = (apx * abx + apy * aby + apz * abz) / ab2;
            t = Math.max(0, Math.min(1, t));
            double projX = a.getX() + t * abx;
            double projY = a.getY() + t * aby;
            double projZ = a.getZ() + t * abz;
            double dx = point.getX() - projX;
            double dy = point.getY() - projY;
            double dz = point.getZ() - projZ;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new CryogenicSatellite(plugin); }
    }

    // ================================================================
    // 24. BLIZZARD COLOSSUS SKULL — Massive ice skull.
    //     Jaw breathing cold fog: 3 hearts/sec in 8-block cone.
    //     Eye socket beams every 50 ticks: 5 hearts, 12-block line.
    // ================================================================
    public static class BlizzardColossusSkull extends ModelEngineAttack {
        private double facingAngle;
        private boolean breathing = false;
        private int breathTimer = 0;

        public BlizzardColossusSkull(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blizzard_colossus_skull", AttackType.MODEL_ENGINE, 1, "modes/freezingice/attacks"));
            config.setDamage(6.0);       // 3 hearts/sec breath
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(360);
            config.setCooldownTicks(320);
        }

        @Override protected String getModelId() { return "blizzard_colossus_skull"; }
        @Override protected double getModelScale() { return 2.5; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center.clone().add(0, 2, 0));
            playAnimation("emerge", 0.0, false);
            shatterSound(center, 2.0f, 0.3f);
            freezeSound(center, 1.5f, 0.2f);
            deepIceDust(center, 30, 3.0);

            // Face toward nearest player
            Player nearest = findNearestPlayer(center, 20.0);
            if (nearest != null) {
                double dx = nearest.getLocation().getX() - center.getX();
                double dz = nearest.getLocation().getZ() - center.getZ();
                facingAngle = Math.atan2(dz, dx);
            } else {
                facingAngle = Math.random() * 2 * Math.PI;
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            Location skullCenter = c.clone().add(0, 3, 0);

            // Breath attack — cold fog in cone in front of skull
            breathTimer++;
            if (breathTimer >= 40) {
                breathing = !breathing;
                breathTimer = 0;
                if (breathing) {
                    playAnimation("breathe", 0.0, false);
                    freezeSound(skullCenter, 1.2f, 0.2f);
                }
            }

            if (breathing) {
                // Cone fog damage — 3 hearts/sec within 8-block cone in front
                double coneDirX = Math.cos(facingAngle);
                double coneDirZ = Math.sin(facingAngle);

                if (tick % 5 == 0) {
                    // Fog particles in cone
                    for (double dist = 1; dist <= 8; dist += 0.8) {
                        double spread = dist * 0.4;
                        Location fogPoint = skullCenter.clone().add(
                                coneDirX * dist + (Math.random() - 0.5) * spread,
                                -0.5 + (Math.random() - 0.5) * spread,
                                coneDirZ * dist + (Math.random() - 0.5) * spread
                        );
                        frostDust(fogPoint, 2, 0.3);
                        snowflakes(fogPoint, 1, 0.2);
                    }
                }

                if (tick % 20 == 0) {
                    for (Player p : c.getWorld().getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL) continue;
                        Location pLoc = p.getLocation();
                        double dx = pLoc.getX() - skullCenter.getX();
                        double dz = pLoc.getZ() - skullCenter.getZ();
                        double dist = Math.sqrt(dx * dx + dz * dz);
                        if (dist > 8.0 || dist < 0.5) continue;

                        // Check if in cone (60 degree half-angle)
                        double dot = (coneDirX * dx + coneDirZ * dz) / dist;
                        if (dot >= 0.5) { // cos(60 degrees) = 0.5
                            p.damage(6.0); // 3 hearts
                            p.setNoDamageTicks(0);
                            p.setFreezeTicks(Math.min(p.getFreezeTicks() + 40, 140));
                        }
                    }
                }
            }

            // Eye socket beams every 50 ticks — 5 hearts, 12-block line
            if (tick % 50 == 0) {
                playAnimation("eye_beam", 0.0, false);
                shatterSound(skullCenter, 1.0f, 1.0f);

                // Two eye sockets — slightly offset from skull center
                double perpX = -Math.sin(facingAngle);
                double perpZ = Math.cos(facingAngle);
                Location leftEye = skullCenter.clone().add(perpX * 0.8, 0.5, perpZ * 0.8);
                Location rightEye = skullCenter.clone().add(-perpX * 0.8, 0.5, -perpZ * 0.8);

                for (Location eye : new Location[]{leftEye, rightEye}) {
                    Location beamEnd = eye.clone().add(
                            Math.cos(facingAngle) * 12, 0, Math.sin(facingAngle) * 12);

                    Particle.DustOptions eyeBeam = new Particle.DustOptions(Color.fromRGB(30, 80, 160), 1.2f);
                    DisplayBuilder.particleLine(eye, beamEnd, Particle.DUST, 5, eyeBeam);
                    DisplayBuilder.particleLine(eye, beamEnd, Particle.END_ROD, 2, null);

                    // Beam damage — 5 hearts
                    for (Player p : c.getWorld().getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL) continue;
                        double dist = distToBeam(p.getLocation(), eye, beamEnd);
                        if (dist <= 1.2) {
                            p.damage(10.0); // 5 hearts
                            p.setNoDamageTicks(0);
                            deepIceDust(p.getLocation(), 8, 0.5);
                        }
                    }
                }
            }

            // Skull ambient — jaw drip, eye glow
            if (tick % 6 == 0) {
                Location jawTip = skullCenter.clone().add(Math.cos(facingAngle) * 1.5, -1.0, Math.sin(facingAngle) * 1.5);
                iceDust(jawTip, 2, 0.3);
            }
            if (tick % 10 == 0) {
                endRods(skullCenter.clone().add(0, 0.5, 0), 2, 0.5);
            }

            // Ambient
            if (tick % 45 == 0) {
                freezeSound(c, 0.6f, 0.15f);
            }
        }

        /** Distance from point to line segment in 3D (for beam checks). */
        private double distToBeam(Location point, Location a, Location b) {
            double abx = b.getX() - a.getX(), aby = b.getY() - a.getY(), abz = b.getZ() - a.getZ();
            double apx = point.getX() - a.getX(), apy = point.getY() - a.getY(), apz = point.getZ() - a.getZ();
            double ab2 = abx * abx + aby * aby + abz * abz;
            if (ab2 == 0) return point.distance(a);
            double t = (apx * abx + apy * aby + apz * abz) / ab2;
            t = Math.max(0, Math.min(1, t));
            double px = a.getX() + t * abx - point.getX();
            double py = a.getY() + t * aby - point.getY();
            double pz = a.getZ() + t * abz - point.getZ();
            return Math.sqrt(px * px + py * py + pz * pz);
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new BlizzardColossusSkull(plugin); }
    }

    // ================================================================
    // 25. FROZEN ORRERY — Armillary sphere with orbiting crystals.
    //     Orbiting crystal planets: 3 hearts contact each.
    //     Rings themselves: 2 hearts. Core pulses every 80 ticks:
    //     6 hearts in 5 blocks. Most complex attack.
    // ================================================================
    public static class FrozenOrrery extends ModelEngineAttack {
        private double ring1Angle = 0;
        private double ring2Angle = Math.PI / 3;
        private double ring3Angle = 2 * Math.PI / 3;
        private final double[] planetAngles = new double[5];
        private int coreTimer = 0;

        public FrozenOrrery(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_orrery", AttackType.MODEL_ENGINE, 1, "modes/freezingice/attacks"));
            config.setDamage(4.0);       // 2 hearts ring contact
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(400);
            config.setCooldownTicks(350);
        }

        @Override protected String getModelId() { return "frozen_orrery"; }
        @Override protected double getModelScale() { return 2.0; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center.clone().add(0, 3, 0));
            playAnimation("activate", 0.0, false);
            chimeSound(center, 1.2f, 0.5f);
            freezeSound(center, 0.8f, 0.3f);
            endRods(center.clone().add(0, 3, 0), 25, 2.5);

            // Initialize planet orbital angles
            for (int i = 0; i < planetAngles.length; i++) {
                planetAngles[i] = (2 * Math.PI * i) / planetAngles.length;
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            Location core = c.clone().add(0, 3, 0);

            // Rotate rings at different speeds
            ring1Angle += 0.08;  // Fast horizontal ring
            ring2Angle += 0.05;  // Medium tilted ring
            ring3Angle += 0.03;  // Slow vertical ring

            // Ring 1 — horizontal orbit (radius 4)
            double r1Radius = 4.0;
            for (int i = 0; i < 12; i++) {
                double angle = ring1Angle + (i * Math.PI / 6);
                double rx = Math.cos(angle) * r1Radius;
                double rz = Math.sin(angle) * r1Radius;
                Location ringPoint = core.clone().add(rx, 0, rz);

                // Ring contact damage — 2 hearts
                damageNearby(ringPoint, 0.8, 4.0);

                if (tick % 4 == 0 && i % 3 == 0) {
                    iceDust(ringPoint, 1, 0.1);
                }
            }

            // Ring 2 — tilted 45 degrees (radius 3.5)
            double r2Radius = 3.5;
            for (int i = 0; i < 10; i++) {
                double angle = ring2Angle + (i * Math.PI / 5);
                double rx = Math.cos(angle) * r2Radius;
                double ry = Math.sin(angle) * r2Radius * 0.7; // tilted
                double rz = Math.sin(angle) * r2Radius * 0.7;
                Location ringPoint = core.clone().add(rx, ry, rz);

                damageNearby(ringPoint, 0.8, 4.0);

                if (tick % 4 == 0 && i % 3 == 0) {
                    deepIceDust(ringPoint, 1, 0.1);
                }
            }

            // Ring 3 — vertical ring (radius 3)
            double r3Radius = 3.0;
            for (int i = 0; i < 10; i++) {
                double angle = ring3Angle + (i * Math.PI / 5);
                double rx = Math.cos(angle) * r3Radius;
                double ry = Math.sin(angle) * r3Radius;
                Location ringPoint = core.clone().add(rx, ry, 0);

                damageNearby(ringPoint, 0.8, 4.0);

                if (tick % 4 == 0 && i % 3 == 0) {
                    frostDust(ringPoint, 1, 0.1);
                }
            }

            // Orbiting crystal planets — 5 crystals, each on its own orbit
            for (int i = 0; i < planetAngles.length; i++) {
                planetAngles[i] += 0.04 + (i * 0.008);
                double orbitRadius = 2.0 + (i * 0.6);
                double tilt = i * (Math.PI / 5);

                double px = Math.cos(planetAngles[i]) * orbitRadius;
                double py = Math.sin(planetAngles[i]) * Math.sin(tilt) * orbitRadius * 0.5;
                double pz = Math.sin(planetAngles[i]) * Math.cos(tilt) * orbitRadius;
                Location planetLoc = core.clone().add(px, py, pz);

                // Crystal contact damage — 3 hearts
                damageNearby(planetLoc, 1.2, 6.0);

                if (tick % 3 == 0) {
                    endRods(planetLoc, 1, 0.15);
                    if (i % 2 == 0) {
                        iceDust(planetLoc, 1, 0.15);
                    } else {
                        deepIceDust(planetLoc, 1, 0.15);
                    }
                }
            }

            // Core pulse every 80 ticks — 6 hearts in 5 blocks
            coreTimer++;
            if (coreTimer >= 80) {
                coreTimer = 0;
                playAnimation("pulse", 0.0, false);
                damageNearby(core, 5.0, 12.0); // 6 hearts
                shatterSound(core, 1.5f, 0.6f);
                freezeSound(core, 1.0f, 0.4f);
                DisplayBuilder.particleRing(core, 5.0, Particle.SNOWFLAKE, 30, null);
                deepIceDust(core, 20, 2.5);
                endRods(core, 10, 1.0);

                // Freeze anyone hit by pulse
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    if (p.getLocation().distanceSquared(core) <= 25.0) { // 5^2
                        p.setFreezeTicks(Math.min(p.getFreezeTicks() + 60, 140));
                    }
                }
            }

            // Core idle glow
            if (tick % 5 == 0 && coreTimer > 10) {
                endRods(core, 3, 0.3);
                // Charging glow intensifies as pulse approaches
                if (coreTimer > 60) {
                    Particle.DustOptions chargeDust = new Particle.DustOptions(Color.fromRGB(30, 80, 160), 1.5f);
                    DisplayBuilder.particleRing(core, 1.5, Particle.DUST, 6, chargeDust);
                }
            }

            // Ring visual trails
            if (tick % 6 == 0) {
                Particle.DustOptions r1Dust = new Particle.DustOptions(Color.fromRGB(100, 180, 255), 0.5f);
                DisplayBuilder.particleRing(core, r1Radius, Particle.DUST, 12, r1Dust);
            }

            // Ambient
            if (tick % 40 == 0) {
                chimeSound(core, 0.5f, 0.7f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new FrozenOrrery(plugin); }
    }
}
