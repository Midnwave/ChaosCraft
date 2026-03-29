package com.blockforge.chaoscraft.modes.bluemoon.attacks;

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
 * Blue Moon Mode — MODEL ENGINE VFX ATTACKS
 * 25 lunar-themed ModelEngine 4 attacks. Each spawns a .bbmodel blueprint,
 * plays animations, and deals damage through unique mechanics.
 *
 * Color palette:
 * - Moonlight blue:  RGB(168, 200, 240)
 * - Lunar core:      RGB(96, 144, 232)
 * - Pale moon:       RGB(192, 216, 248)
 * - Void midnight:   RGB(10, 15, 46)
 *
 * Particles: END_ROD, SNOWFLAKE, DUST with moon colors
 * Sounds: BLOCK_AMETHYST_BLOCK_CHIME, ENTITY_ALLAY_AMBIENT_WITH_ITEM,
 *         BLOCK_BEACON_AMBIENT, BLOCK_RESPAWN_ANCHOR_CHARGE,
 *         ENTITY_PLAYER_HURT_FREEZE
 */
public final class BlueMoonModelEngine {
    private BlueMoonModelEngine() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        // AOE (1-10)
        registry.register(new TideBreaker(plugin));
        registry.register(new MoonfallCrater(plugin));
        registry.register(new FullPull(plugin));
        registry.register(new Totality(plugin));
        registry.register(new FaultLine(plugin));
        registry.register(new EventHorizon(plugin));
        registry.register(new Perigee(plugin));
        registry.register(new Scar(plugin));
        registry.register(new TheWeight(plugin));
        registry.register(new BlueMidnight(plugin));
        // Projectile/Beam (11-16)
        registry.register(new SeleniteSpear(plugin));
        registry.register(new RayOfSelene(plugin));
        registry.register(new CraterMaker(plugin));
        registry.register(new Riptide(plugin));
        registry.register(new DarkHalf(plugin));
        registry.register(new Cartography(plugin));
        // Summon/Persistent (17-20)
        registry.register(new TidalSentinel(plugin));
        registry.register(new LunarMonolith(plugin));
        registry.register(new MoonPhaseTotem(plugin));
        registry.register(new SilverTreeOfTides(plugin));
        // Environmental (21-25)
        registry.register(new LunarFog(plugin));
        registry.register(new MoondustFall(plugin));
        registry.register(new TidalPool(plugin));
        registry.register(new GravityWell(plugin));
        registry.register(new CelestialAurora(plugin));
    }

    // ================================================================
    // Shared helpers
    // ================================================================

    private static final String MODE_PATH = "modes/bluemoon/attacks";

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

    /** Moonlight blue dust — RGB(168, 200, 240). */
    private static void moonDust(Location loc, int count, double spread) {
        DisplayBuilder.dustParticles(loc, count, spread, 168, 200, 240, 1.2f);
    }

    /** Lunar core dust — RGB(96, 144, 232). */
    private static void lunarDust(Location loc, int count, double spread) {
        DisplayBuilder.dustParticles(loc, count, spread, 96, 144, 232, 1.4f);
    }

    /** Pale moon dust — RGB(192, 216, 248). */
    private static void paleDust(Location loc, int count, double spread) {
        DisplayBuilder.dustParticles(loc, count, spread, 192, 216, 248, 1.0f);
    }

    /** Void midnight dust — RGB(10, 15, 46). */
    private static void voidDust(Location loc, int count, double spread) {
        DisplayBuilder.dustParticles(loc, count, spread, 10, 15, 46, 1.5f);
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

    /** Pull players toward a center point with given velocity strength. */
    private static void pullPlayersToward(Location center, double radius, double strength) {
        if (center.getWorld() == null) return;
        double r2 = radius * radius;
        for (Player p : center.getWorld().getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL) continue;
            if (p.isInvulnerable()) continue;
            if (p.getLocation().distanceSquared(center) <= r2) {
                Vector dir = center.toVector().subtract(p.getLocation().toVector()).normalize().multiply(strength);
                p.setVelocity(p.getVelocity().add(dir));
            }
        }
    }

    /** Amethyst chime sound. */
    private static void chimeSound(Location loc, float volume, float pitch) {
        DisplayBuilder.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, volume, pitch);
    }

    /** Allay ambient sound — ethereal. */
    private static void allaySound(Location loc, float volume, float pitch) {
        DisplayBuilder.playSound(loc, Sound.ENTITY_ALLAY_AMBIENT_WITH_ITEM, volume, pitch);
    }

    /** Beacon ambient sound — deep hum. */
    private static void beaconSound(Location loc, float volume, float pitch) {
        DisplayBuilder.playSound(loc, Sound.BLOCK_BEACON_AMBIENT, volume, pitch);
    }

    /** Respawn anchor charge sound — heavy pulse. */
    private static void anchorSound(Location loc, float volume, float pitch) {
        DisplayBuilder.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, volume, pitch);
    }

    /** Freeze hurt sound. */
    private static void freezeSound(Location loc, float volume, float pitch) {
        DisplayBuilder.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, volume, pitch);
    }

    // ================================================================
    // 1. TIDE BREAKER — 3 expanding rings with wave crests.
    //    Expanding ring edge: 6 hearts as it passes.
    //    Inner ring: 8 hearts. Center after expansion: 3 hearts/sec.
    // ================================================================
    public static class TideBreaker extends ModelEngineAttack {
        private double ringRadius1 = 1.0;
        private double ringRadius2 = 0.5;
        private double ringRadius3 = 0.0;
        private boolean fullyExpanded = false;

        public TideBreaker(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tide_breaker", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(6.0);        // 3 hearts/sec center residual
            config.setDamageRadius(3.0);  // center residual radius
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(240);
            config.setCooldownTicks(220);
        }

        @Override protected String getModelId() { return "tide_breaker"; }
        @Override protected double getModelScale() { return 2.0; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center);
            playAnimation("spawn", 0.0, false);
            anchorSound(center, 1.2f, 0.6f);
            chimeSound(center, 0.8f, 1.2f);
            lunarDust(center, 25, 2.0);
            endRods(center, 15, 1.5);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Expand rings outward
            if (ringRadius1 < 10.0) {
                ringRadius1 += 0.15;
                ringRadius2 += 0.15;
                ringRadius3 += 0.15;
            } else {
                fullyExpanded = true;
            }

            // Ring 1 — outermost, 12 hearts (6 hearts = 12 damage)
            if (tick % 2 == 0 && ringRadius1 > 0 && ringRadius1 <= 10.0) {
                DisplayBuilder.particleRing(c, ringRadius1, Particle.END_ROD, 20, null);
                moonDust(c.clone().add(0, 0.5, 0), 5, ringRadius1 * 0.5);

                // Damage players on the ring edge (within 1.5 blocks of the ring line)
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    double dist = Math.sqrt(p.getLocation().distanceSquared(c));
                    if (Math.abs(dist - ringRadius1) < 1.5) {
                        p.damage(12.0); // 6 hearts
                        p.setNoDamageTicks(0);
                    }
                }
            }

            // Ring 2 — middle, 16 hearts (8 hearts = 16 damage)
            if (tick % 3 == 0 && ringRadius2 > 0 && ringRadius2 <= 8.0) {
                DisplayBuilder.particleRing(c, ringRadius2, Particle.SNOWFLAKE, 16, null);
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    double dist = Math.sqrt(p.getLocation().distanceSquared(c));
                    if (Math.abs(dist - ringRadius2) < 1.2) {
                        p.damage(16.0); // 8 hearts
                        p.setNoDamageTicks(0);
                    }
                }
            }

            // Ring 3 — innermost crest particles
            if (ringRadius3 > 0 && ringRadius3 <= 6.0 && tick % 4 == 0) {
                DisplayBuilder.particleRing(c, ringRadius3, Particle.END_ROD, 12, null);
                lunarDust(c, 3, ringRadius3 * 0.3);
            }

            // After full expansion — center residual damage: 3 hearts/sec
            if (fullyExpanded && tick % 20 == 0) {
                damageNearby(c, 3.0, 6.0);
                paleDust(c, 8, 1.5);
                snowflakes(c, 5, 1.0);
            }

            // Ambient sounds
            if (tick % 40 == 0) {
                beaconSound(c, 0.6f, 0.8f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new TideBreaker(plugin); }
    }

    // ================================================================
    // 2. MOONFALL CRATER — Crater rim, impact spikes, floating chunks.
    //    Impact: 10 hearts in 5 blocks. Rim: 2 hearts/sec.
    //    Orbiting chunks: 3 hearts on contact.
    // ================================================================
    public static class MoonfallCrater extends ModelEngineAttack {
        private boolean impacted = false;
        private double chunkAngle = 0;
        private Location spawnLoc;

        public MoonfallCrater(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("moonfall_crater", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(4.0);        // 2 hearts/sec rim
            config.setDamageRadius(6.0);  // rim zone
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
            config.setImpactDamage(20.0); // 10 hearts
            config.setImpactRadius(5.0);
        }

        @Override protected String getModelId() { return "moonfall_crater"; }
        @Override protected double getModelScale() { return 2.5; }

        @Override
        protected void onSpawn(Location center) {
            spawnLoc = center.clone().add(0, 25, 0);
            spawnModel(spawnLoc);
            playAnimation("descend", 0.0, false);
            beaconSound(center, 1.0f, 0.4f);
            voidDust(spawnLoc, 20, 3.0);
            endRods(spawnLoc, 15, 2.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (!impacted) {
                // Descend toward ground
                if (spawnLoc.getY() > c.getY() + 1.0) {
                    spawnLoc.subtract(0, 0.6, 0);
                    if (modelHost != null && modelHost.isValid()) {
                        modelHost.teleport(spawnLoc);
                    }
                    if (tick % 2 == 0) {
                        lunarDust(spawnLoc, 8, 2.5);
                        endRods(spawnLoc, 4, 1.0);
                    }
                } else {
                    // Impact
                    impacted = true;
                    if (modelHost != null && modelHost.isValid()) {
                        modelHost.teleport(c);
                    }
                    playAnimation("impact", 0.0, false);
                    triggerImpactDamage(c);
                    anchorSound(c, 1.5f, 0.3f);
                    freezeSound(c, 1.2f, 0.5f);
                    DisplayBuilder.particleRing(c, 5.0, Particle.END_ROD, 30, null);
                    voidDust(c, 40, 4.0);
                    moonDust(c, 25, 3.0);
                }
            } else {
                // Rim zone damage — 2 hearts/sec
                if (tick % 20 == 0) {
                    for (Player p : c.getWorld().getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                        double dist = Math.sqrt(p.getLocation().distanceSquared(c));
                        if (dist >= 4.0 && dist <= 6.0) {
                            p.damage(4.0); // 2 hearts
                            p.setNoDamageTicks(0);
                        }
                    }
                }

                // Floating chunk orbit — 4 chunks, 3 hearts on contact
                chunkAngle += 0.06;
                for (int i = 0; i < 4; i++) {
                    double angle = chunkAngle + (i * Math.PI / 2);
                    double cx = Math.cos(angle) * 4.5;
                    double cy = 2.0 + Math.sin(tick * 0.05) * 0.5;
                    double cz = Math.sin(angle) * 4.5;
                    Location chunkLoc = c.clone().add(cx, cy, cz);

                    if (tick % 3 == 0) {
                        moonDust(chunkLoc, 3, 0.3);
                        endRods(chunkLoc, 1, 0.1);
                    }
                    damageNearby(chunkLoc, 1.2, 6.0); // 3 hearts
                }

                // Ambient particles
                if (tick % 10 == 0) {
                    DisplayBuilder.particleRing(c.clone().add(0, 0.2, 0), 6.0, Particle.SNOWFLAKE, 16, null);
                }

                if (tick % 50 == 0) {
                    chimeSound(c, 0.5f, 0.6f);
                }
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new MoonfallCrater(plugin); }
    }

    // ================================================================
    // 3. FULL PULL — 12 tide arrows pointing inward, gravity node.
    //    Pull phase drags players inward (0.3/tick velocity).
    //    Release burst: 8 hearts in 7 blocks.
    //    Arrow contact: 2 hearts.
    // ================================================================
    public static class FullPull extends ModelEngineAttack {
        private boolean released = false;
        private int pullDuration = 0;
        private double arrowAngle = 0;

        public FullPull(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("full_pull", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(4.0);        // 2 hearts arrow contact
            config.setDamageRadius(1.5);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(280);
            config.setCooldownTicks(260);
            config.setImpactDamage(16.0); // 8 hearts burst
            config.setImpactRadius(7.0);
        }

        @Override protected String getModelId() { return "full_pull"; }
        @Override protected double getModelScale() { return 2.0; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center);
            playAnimation("spawn", 0.0, false);
            beaconSound(center, 1.0f, 0.5f);
            allaySound(center, 0.8f, 0.4f);
            lunarDust(center, 30, 3.0);
            voidDust(center, 15, 2.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            pullDuration++;

            if (!released) {
                // Pull phase — drag players inward for 120 ticks
                if (pullDuration < 120) {
                    pullPlayersToward(c, 12.0, 0.3);

                    // 12 arrow positions pointing inward
                    for (int i = 0; i < 12; i++) {
                        double angle = (2 * Math.PI * i) / 12;
                        double radius = 8.0 - (pullDuration * 0.04);
                        double ax = Math.cos(angle) * Math.max(radius, 2.0);
                        double az = Math.sin(angle) * Math.max(radius, 2.0);
                        Location arrowLoc = c.clone().add(ax, 1.0, az);

                        if (tick % 3 == 0) {
                            lunarDust(arrowLoc, 2, 0.2);
                            endRods(arrowLoc, 1, 0.1);
                        }
                        // Arrow contact damage — 2 hearts
                        damageNearby(arrowLoc, 1.5, 4.0);
                    }

                    // Central gravity node particles
                    if (tick % 4 == 0) {
                        voidDust(c.clone().add(0, 1.5, 0), 6, 0.8);
                        endRods(c.clone().add(0, 2.0, 0), 3, 0.5);
                    }

                    if (tick % 30 == 0) {
                        beaconSound(c, 0.7f, 0.3f);
                    }
                } else {
                    // Release burst — 8 hearts in 7 blocks
                    released = true;
                    playAnimation("burst", 0.0, false);
                    triggerImpactDamage(c);
                    anchorSound(c, 1.5f, 0.5f);
                    freezeSound(c, 1.0f, 0.6f);
                    moonDust(c, 40, 5.0);
                    endRods(c, 25, 4.0);
                    DisplayBuilder.particleRing(c, 7.0, Particle.END_ROD, 30, null);
                }
            } else {
                // Post-burst dissipation particles
                if (tick % 8 == 0) {
                    paleDust(c, 4, 3.0);
                    snowflakes(c, 3, 2.0);
                }
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new FullPull(plugin); }
    }

    // ================================================================
    // 4. TOTALITY — Vertical eclipse disc with corona rays.
    //    Disc contact: 6 hearts. Corona tips: 4 hearts.
    //    Staring at disc (within 20 deg): 8 hearts burst every 50 ticks.
    // ================================================================
    public static class Totality extends ModelEngineAttack {
        private double coronaAngle = 0;

        public Totality(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("totality", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(12.0);       // 6 hearts disc contact
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
        }

        @Override protected String getModelId() { return "totality"; }
        @Override protected double getModelScale() { return 2.0; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center.clone().add(0, 5, 0));
            playAnimation("spawn", 0.0, false);
            beaconSound(center, 1.2f, 0.3f);
            allaySound(center, 1.0f, 0.8f);
            voidDust(center.clone().add(0, 5, 0), 30, 3.0);
            endRods(center.clone().add(0, 5, 0), 20, 2.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            Location discCenter = c.clone().add(0, 5, 0);

            coronaAngle += 0.04;

            // Disc contact damage — 6 hearts
            if (tick % 20 == 0) {
                damageNearby(discCenter, 3.0, 12.0);
            }

            // 8 corona ray tips
            for (int i = 0; i < 8; i++) {
                double angle = coronaAngle + (i * Math.PI / 4);
                double rx = Math.cos(angle) * 5.0;
                double ry = Math.sin(angle) * 5.0;
                Location tip = discCenter.clone().add(rx, ry, 0);

                if (tick % 3 == 0) {
                    moonDust(tip, 2, 0.3);
                    endRods(tip, 1, 0.1);
                }
                // Corona tip damage — 4 hearts
                damageNearby(tip, 1.5, 8.0);
            }

            // Staring check — within 20 degrees of disc direction = 8 hearts burst
            if (tick % 50 == 0) {
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(discCenter) > 400) continue; // 20 block range
                    Vector toDisc = discCenter.toVector().subtract(p.getEyeLocation().toVector()).normalize();
                    Vector lookDir = p.getEyeLocation().getDirection().normalize();
                    double dot = lookDir.dot(toDisc);
                    // cos(20 deg) ~ 0.94
                    if (dot >= 0.94) {
                        p.damage(16.0); // 8 hearts
                        p.setNoDamageTicks(0);
                        freezeSound(p.getLocation(), 0.8f, 0.5f);
                        voidDust(p.getEyeLocation(), 8, 0.5);
                    }
                }
            }

            // Ambient disc glow
            if (tick % 6 == 0) {
                DisplayBuilder.particleRing(discCenter, 2.5, Particle.END_ROD, 12, null);
                voidDust(discCenter, 4, 1.5);
            }

            if (tick % 60 == 0) {
                chimeSound(discCenter, 0.6f, 1.4f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new Totality(plugin); }
    }

    // ================================================================
    // 5. FAULT LINE — Linear rupture with 7 clusters, central spikes.
    //    Standing on line (2 blocks wide): 5 hearts/sec.
    //    Spikes: 4 hearts impact. Ejected shards: 2 hearts.
    // ================================================================
    public static class FaultLine extends ModelEngineAttack {
        private int spikePhase = 0;

        public FaultLine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fault_line", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(10.0);       // 5 hearts/sec line
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(260);
            config.setCooldownTicks(240);
        }

        @Override protected String getModelId() { return "fault_line"; }
        @Override protected double getModelScale() { return 2.0; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center);
            playAnimation("rupture", 0.0, false);
            anchorSound(center, 1.3f, 0.4f);
            freezeSound(center, 1.0f, 0.6f);

            // Rupture particles along X axis
            for (int i = -7; i <= 7; i++) {
                Location rupture = center.clone().add(i * 1.5, 0, 0);
                lunarDust(rupture, 5, 0.5);
                voidDust(rupture, 3, 0.3);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            spikePhase++;

            // Line damage — 5 hearts/sec to anyone standing on the line (2 blocks wide along Z)
            if (tick % 20 == 0) {
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    Location pl = p.getLocation();
                    double dz = Math.abs(pl.getZ() - c.getZ());
                    double dx = Math.abs(pl.getX() - c.getX());
                    if (dz <= 2.0 && dx <= 10.5) {
                        p.damage(10.0); // 5 hearts
                        p.setNoDamageTicks(0);
                    }
                }
            }

            // 7 cluster positions along the line
            for (int i = -3; i <= 3; i++) {
                Location cluster = c.clone().add(i * 3.0, 0, 0);
                if (tick % 5 == 0) {
                    lunarDust(cluster, 2, 0.4);
                }
            }

            // Spike eruptions every 40 ticks — 4 hearts impact
            if (spikePhase % 40 == 0) {
                int spikeIndex = (spikePhase / 40) % 7;
                Location spikeLoc = c.clone().add((spikeIndex - 3) * 3.0, 0, 0);
                playAnimation("spike", 0.0, false);
                damageNearby(spikeLoc, 2.5, 8.0); // 4 hearts
                moonDust(spikeLoc, 15, 1.5);
                endRods(spikeLoc.clone().add(0, 2, 0), 8, 0.8);
                anchorSound(spikeLoc, 0.7f, 0.8f);

                // Ejected shards — 2 hearts, scatter outward along Z
                for (int s = -2; s <= 2; s++) {
                    if (s == 0) continue;
                    Location shard = spikeLoc.clone().add(0, 1.0, s * 2.0);
                    paleDust(shard, 3, 0.3);
                    damageNearby(shard, 1.0, 4.0); // 2 hearts
                }
            }

            // Ambient cracks
            if (tick % 8 == 0) {
                int idx = tick % 7;
                Location crack = c.clone().add((idx - 3) * 3.0, 0.1, 0);
                voidDust(crack, 2, 0.5);
            }

            if (tick % 50 == 0) {
                beaconSound(c, 0.5f, 0.4f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new FaultLine(plugin); }
    }

    // ================================================================
    // 6. EVENT HORIZON — Collapsing ring that crushes inward.
    //    Ring edge: 6 hearts as it passes during collapse.
    //    Center detonation: 12 hearts in 4 blocks.
    //    Inside ring: pulled inward.
    // ================================================================
    public static class EventHorizon extends ModelEngineAttack {
        private double ringRadius = 12.0;
        private boolean detonated = false;

        public EventHorizon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("event_horizon", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(12.0);       // 6 hearts ring edge
            config.setDamageRadius(1.5);
            config.setTicksBetweenDamage(5);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
            config.setImpactDamage(24.0); // 12 hearts center detonation
            config.setImpactRadius(4.0);
        }

        @Override protected String getModelId() { return "event_horizon"; }
        @Override protected double getModelScale() { return 2.5; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center);
            playAnimation("spawn", 0.0, false);
            beaconSound(center, 1.2f, 0.3f);
            allaySound(center, 0.8f, 0.5f);
            DisplayBuilder.particleRing(center, 12.0, Particle.END_ROD, 40, null);
            voidDust(center, 20, 6.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (!detonated) {
                // Collapse ring inward
                if (ringRadius > 0.5) {
                    ringRadius -= 0.08;

                    // Ring visual
                    if (tick % 2 == 0) {
                        DisplayBuilder.particleRing(c, ringRadius, Particle.END_ROD, 24, null);
                        DisplayBuilder.particleRing(c.clone().add(0, 0.5, 0), ringRadius, Particle.SNOWFLAKE, 16, null);
                    }

                    // Ring edge damage — 6 hearts
                    for (Player p : c.getWorld().getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                        double dist = Math.sqrt(p.getLocation().distanceSquared(c));
                        if (Math.abs(dist - ringRadius) < 1.5) {
                            p.damage(12.0); // 6 hearts
                            p.setNoDamageTicks(0);
                        }
                    }

                    // Pull players inside ring inward
                    pullPlayersToward(c, ringRadius, 0.2);

                    // Ambient collapse sound
                    if (tick % 30 == 0) {
                        beaconSound(c, 0.8f, 0.4f + (float)(1.0 - ringRadius / 12.0));
                    }
                } else {
                    // Center detonation — 12 hearts in 4 blocks
                    detonated = true;
                    playAnimation("detonate", 0.0, false);
                    triggerImpactDamage(c);
                    anchorSound(c, 1.5f, 0.2f);
                    freezeSound(c, 1.3f, 0.4f);
                    voidDust(c, 50, 4.0);
                    moonDust(c, 30, 3.0);
                    endRods(c, 25, 3.0);
                }
            } else {
                // Post-detonation residual particles
                if (tick % 10 == 0) {
                    voidDust(c, 5, 2.0);
                    paleDust(c, 3, 1.5);
                }
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new EventHorizon(plugin); }
    }

    // ================================================================
    // 7. PERIGEE — Upward storm column of spiral discs.
    //    Standing in column: 4 hearts/sec + launched upward (Y 0.5).
    //    Debris chunks: 3 hearts on contact. Ground burst: 5 hearts.
    // ================================================================
    public static class Perigee extends ModelEngineAttack {
        private double spiralAngle = 0;
        private boolean groundBurst = false;

        public Perigee(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("perigee", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(8.0);        // 4 hearts/sec column
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(260);
            config.setCooldownTicks(240);
        }

        @Override protected String getModelId() { return "perigee"; }
        @Override protected double getModelScale() { return 2.0; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center);
            playAnimation("erupt", 0.0, false);
            anchorSound(center, 1.3f, 0.5f);
            beaconSound(center, 1.0f, 0.6f);
            lunarDust(center, 30, 2.0);
            endRods(center, 20, 1.5);

            // Ground burst — 5 hearts at spawn
            damageNearby(center, 4.0, 10.0);
            groundBurst = true;
            moonDust(center, 20, 3.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            spiralAngle += 0.12;

            // Column damage — 4 hearts/sec + launch upward
            if (tick % 20 == 0) {
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    Location pl = p.getLocation();
                    double hDist = Math.sqrt(
                        Math.pow(pl.getX() - c.getX(), 2) + Math.pow(pl.getZ() - c.getZ(), 2)
                    );
                    if (hDist <= 3.0 && pl.getY() >= c.getY() && pl.getY() <= c.getY() + 15) {
                        p.damage(8.0); // 4 hearts
                        p.setNoDamageTicks(0);
                        p.setVelocity(p.getVelocity().add(new Vector(0, 0.5, 0)));
                    }
                }
            }

            // Spiral disc particles going upward
            for (int layer = 0; layer < 5; layer++) {
                double y = (tick * 0.3 + layer * 3.0) % 15.0;
                double angle = spiralAngle + layer * 0.8;
                double rx = Math.cos(angle) * 2.5;
                double rz = Math.sin(angle) * 2.5;
                Location disc = c.clone().add(rx, y, rz);

                if (tick % 3 == 0) {
                    lunarDust(disc, 2, 0.3);
                    endRods(disc, 1, 0.1);
                }
            }

            // Debris chunks — 3 hearts on contact, flung outward
            if (tick % 25 == 0) {
                for (int i = 0; i < 3; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = 3.0 + Math.random() * 3.0;
                    double dy = 2.0 + Math.random() * 8.0;
                    Location debris = c.clone().add(Math.cos(angle) * dist, dy, Math.sin(angle) * dist);
                    moonDust(debris, 4, 0.4);
                    damageNearby(debris, 1.5, 6.0); // 3 hearts
                }
            }

            // Ambient column sound
            if (tick % 40 == 0) {
                allaySound(c.clone().add(0, 7, 0), 0.6f, 0.5f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new Perigee(plugin); }
    }

    // ================================================================
    // 8. SCAR — Ground brand rune, crescent, rune marks.
    //    Brand ring: 3 hearts/sec. Crescent zone: 4 hearts/sec.
    //    Rune marks pulse: 5 hearts every 60 ticks to all on brand.
    // ================================================================
    public static class Scar extends ModelEngineAttack {
        private double crescentAngle = 0;

        public Scar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("scar", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(6.0);        // 3 hearts/sec brand ring
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
        }

        @Override protected String getModelId() { return "scar"; }
        @Override protected double getModelScale() { return 2.0; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center);
            playAnimation("brand", 0.0, false);
            anchorSound(center, 1.0f, 0.4f);
            chimeSound(center, 0.8f, 0.6f);

            // Brand rune particles
            DisplayBuilder.particleRing(center, 5.0, Particle.END_ROD, 24, null);
            voidDust(center, 20, 3.0);
            lunarDust(center, 15, 2.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            crescentAngle += 0.03;

            // Brand ring — 3 hearts/sec to anyone standing on it (radius 4-5 blocks)
            if (tick % 20 == 0) {
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    double dist = Math.sqrt(p.getLocation().distanceSquared(c));
                    if (dist >= 3.5 && dist <= 5.5) {
                        p.damage(6.0); // 3 hearts
                        p.setNoDamageTicks(0);
                    }
                }
            }

            // Crescent zone — semicircle, 4 hearts/sec
            if (tick % 20 == 0) {
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    double dist = Math.sqrt(p.getLocation().distanceSquared(c));
                    if (dist <= 3.5) {
                        // Check if player is in the crescent half
                        double dx = p.getLocation().getX() - c.getX();
                        double dz = p.getLocation().getZ() - c.getZ();
                        double playerAngle = Math.atan2(dz, dx);
                        double diff = Math.abs(playerAngle - crescentAngle);
                        if (diff > Math.PI) diff = 2 * Math.PI - diff;
                        if (diff <= Math.PI / 2) {
                            p.damage(8.0); // 4 hearts
                            p.setNoDamageTicks(0);
                        }
                    }
                }
            }

            // Rune mark pulse — 5 hearts every 60 ticks to ALL on brand
            if (tick % 60 == 0) {
                playAnimation("pulse", 0.0, false);
                damageNearby(c, 5.5, 10.0); // 5 hearts
                moonDust(c, 20, 4.0);
                endRods(c, 10, 3.0);
                anchorSound(c, 0.8f, 0.7f);
            }

            // Brand ring particles
            if (tick % 6 == 0) {
                DisplayBuilder.particleRing(c, 5.0, Particle.END_ROD, 16, null);
                lunarDust(c, 3, 2.0);
            }

            // Crescent visual
            if (tick % 4 == 0) {
                for (int i = -4; i <= 4; i++) {
                    double angle = crescentAngle + (i * 0.15);
                    double cx = Math.cos(angle) * 3.0;
                    double cz = Math.sin(angle) * 3.0;
                    paleDust(c.clone().add(cx, 0.2, cz), 1, 0.2);
                }
            }

            if (tick % 50 == 0) {
                beaconSound(c, 0.5f, 0.5f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new Scar(plugin); }
    }

    // ================================================================
    // 9. THE WEIGHT — Descending pressure disc from Y=15.
    //    Descent pushes players down (negative Y velocity).
    //    Impact: 10 hearts in 6 blocks.
    //    Stalactites: 3 hearts on contact. Under disc: 2 hearts/sec.
    // ================================================================
    public static class TheWeight extends ModelEngineAttack {
        private double discY = 15.0;
        private boolean impacted = false;

        public TheWeight(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_weight", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(4.0);        // 2 hearts/sec compression
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
            config.setImpactDamage(20.0); // 10 hearts
            config.setImpactRadius(6.0);
        }

        @Override protected String getModelId() { return "the_weight"; }
        @Override protected double getModelScale() { return 2.5; }

        @Override
        protected void onSpawn(Location center) {
            Location spawnLoc = center.clone().add(0, 15, 0);
            spawnModel(spawnLoc);
            playAnimation("spawn", 0.0, false);
            beaconSound(center, 1.0f, 0.3f);
            voidDust(spawnLoc, 25, 3.0);
            endRods(spawnLoc, 15, 2.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            Location discLoc = c.clone().add(0, discY, 0);

            if (!impacted) {
                // Descend
                if (discY > 1.0) {
                    discY -= 0.1;
                    if (modelHost != null && modelHost.isValid()) {
                        modelHost.teleport(c.clone().add(0, discY, 0));
                    }

                    // Push players beneath the disc downward
                    for (Player p : c.getWorld().getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                        Location pl = p.getLocation();
                        double hDist = Math.sqrt(
                            Math.pow(pl.getX() - c.getX(), 2) + Math.pow(pl.getZ() - c.getZ(), 2)
                        );
                        if (hDist <= 5.0 && pl.getY() <= c.getY() + discY && pl.getY() >= c.getY()) {
                            p.setVelocity(p.getVelocity().add(new Vector(0, -0.15, 0)));
                        }
                    }

                    // Disc particles
                    if (tick % 3 == 0) {
                        DisplayBuilder.particleRing(discLoc, 4.0, Particle.END_ROD, 16, null);
                        voidDust(discLoc, 4, 2.0);
                    }

                    // Stalactites hanging below disc — 3 hearts on contact
                    if (tick % 10 == 0) {
                        for (int i = 0; i < 5; i++) {
                            double angle = (2 * Math.PI * i) / 5;
                            double sx = Math.cos(angle) * 2.5;
                            double sz = Math.sin(angle) * 2.5;
                            Location stalactite = discLoc.clone().add(sx, -2.0, sz);
                            lunarDust(stalactite, 2, 0.3);
                            damageNearby(stalactite, 1.2, 6.0); // 3 hearts
                        }
                    }

                    if (tick % 30 == 0) {
                        beaconSound(c, 0.7f, 0.3f + (float)(1.0 - discY / 15.0) * 0.5f);
                    }
                } else {
                    // Impact — 10 hearts in 6 blocks
                    impacted = true;
                    playAnimation("crush", 0.0, false);
                    triggerImpactDamage(c);
                    anchorSound(c, 1.5f, 0.2f);
                    freezeSound(c, 1.3f, 0.5f);
                    voidDust(c, 40, 5.0);
                    moonDust(c, 30, 4.0);
                    DisplayBuilder.particleRing(c, 6.0, Particle.END_ROD, 30, null);
                }
            } else {
                // Post-impact — 2 hearts/sec compression to players underneath
                if (tick % 20 == 0) {
                    damageNearby(c, 5.0, 4.0);
                    voidDust(c, 4, 2.0);
                }

                if (tick % 8 == 0) {
                    paleDust(c, 3, 2.0);
                }
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new TheWeight(plugin); }
    }

    // ================================================================
    // 10. BLUE MIDNIGHT — Moon nova sphere with corona.
    //     Sphere contact: 6 hearts. Corona rays: 4 hearts each.
    //     Detonation pulse: 12 hearts in 8 blocks.
    //     Crescent orbits: 3 hearts. The big one.
    // ================================================================
    public static class BlueMidnight extends ModelEngineAttack {
        private double coronaAngle = 0;
        private double crescentAngle = 0;
        private boolean detonated = false;
        private int buildupTicks = 0;

        public BlueMidnight(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blue_midnight", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(12.0);       // 6 hearts sphere contact
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(360);
            config.setCooldownTicks(340);
            config.setImpactDamage(24.0); // 12 hearts detonation
            config.setImpactRadius(8.0);
        }

        @Override protected String getModelId() { return "blue_midnight"; }
        @Override protected double getModelScale() { return 2.5; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center.clone().add(0, 6, 0));
            playAnimation("materialize", 0.0, false);
            beaconSound(center, 1.5f, 0.3f);
            allaySound(center, 1.0f, 0.5f);
            anchorSound(center, 0.8f, 0.4f);
            voidDust(center.clone().add(0, 6, 0), 40, 4.0);
            moonDust(center.clone().add(0, 6, 0), 30, 3.0);
            endRods(center.clone().add(0, 6, 0), 25, 3.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            Location sphereCenter = c.clone().add(0, 6, 0);

            buildupTicks++;
            coronaAngle += 0.05;
            crescentAngle += 0.07;

            if (!detonated) {
                // Sphere contact damage — 6 hearts
                if (tick % 20 == 0) {
                    damageNearby(sphereCenter, 4.0, 12.0);
                }

                // 6 corona rays — 4 hearts each
                for (int i = 0; i < 6; i++) {
                    double angle = coronaAngle + (i * Math.PI / 3);
                    double rx = Math.cos(angle) * 6.0;
                    double ry = Math.sin(angle) * 3.0;
                    double rz = Math.sin(angle + Math.PI / 4) * 6.0;
                    Location tip = sphereCenter.clone().add(rx, ry, rz);

                    if (tick % 3 == 0) {
                        moonDust(tip, 2, 0.3);
                        endRods(tip, 1, 0.15);
                    }
                    damageNearby(tip, 1.5, 8.0); // 4 hearts
                }

                // 3 crescent orbits — 3 hearts on contact
                for (int i = 0; i < 3; i++) {
                    double angle = crescentAngle + (i * 2 * Math.PI / 3);
                    double orbitX = Math.cos(angle) * 5.0;
                    double orbitY = Math.sin(tick * 0.03 + i) * 2.0;
                    double orbitZ = Math.sin(angle) * 5.0;
                    Location crescent = sphereCenter.clone().add(orbitX, orbitY, orbitZ);

                    if (tick % 4 == 0) {
                        paleDust(crescent, 3, 0.4);
                        lunarDust(crescent, 2, 0.2);
                    }
                    damageNearby(crescent, 1.3, 6.0); // 3 hearts
                }

                // Buildup — detonation at tick 200
                if (buildupTicks >= 200) {
                    detonated = true;
                    playAnimation("detonate", 0.0, false);
                    triggerImpactDamage(sphereCenter);
                    anchorSound(sphereCenter, 2.0f, 0.2f);
                    freezeSound(sphereCenter, 1.5f, 0.3f);
                    beaconSound(sphereCenter, 1.5f, 0.5f);
                    voidDust(sphereCenter, 60, 6.0);
                    moonDust(sphereCenter, 50, 5.0);
                    endRods(sphereCenter, 40, 5.0);
                    DisplayBuilder.particleRing(sphereCenter, 8.0, Particle.END_ROD, 40, null);
                    DisplayBuilder.particleRing(c, 8.0, Particle.SNOWFLAKE, 30, null);
                }

                // Ambient sphere glow
                if (tick % 5 == 0) {
                    DisplayBuilder.particleRing(sphereCenter, 3.5, Particle.END_ROD, 12, null);
                    voidDust(sphereCenter, 3, 2.0);
                }

                // Buildup intensity increases
                if (tick % 40 == 0) {
                    chimeSound(sphereCenter, 0.6f + buildupTicks * 0.003f, 0.8f + buildupTicks * 0.002f);
                }
            } else {
                // Post-detonation residual
                if (tick % 8 == 0) {
                    voidDust(sphereCenter, 6, 3.0);
                    paleDust(c, 4, 4.0);
                    snowflakes(c, 3, 3.0);
                }
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new BlueMidnight(plugin); }
    }

    // ================================================================
    // 11. SELENITE SPEAR — Crystalline moon spear projectile.
    //     Tip contact: 8 hearts piercing. Shaft: 4 hearts.
    //     Impact dissipate: 6 hearts burst in 3 blocks.
    //     Tracks nearest player.
    // ================================================================
    public static class SeleniteSpear extends ModelEngineAttack {
        private Location tipLoc;
        private Vector direction;
        private boolean dissipated = false;

        public SeleniteSpear(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("selenite_spear", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(8.0);        // 4 hearts shaft
            config.setDamageRadius(1.5);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(200);
            config.setCooldownTicks(180);
            config.setTracksPlayer(true);
            config.setImpactDamage(12.0); // 6 hearts burst
            config.setImpactRadius(3.0);
        }

        @Override protected String getModelId() { return "selenite_spear"; }
        @Override protected double getModelScale() { return 1.5; }

        @Override
        protected void onSpawn(Location center) {
            tipLoc = center.clone().add(0, 3, 0);
            spawnModel(tipLoc);
            playAnimation("form", 0.0, false);
            chimeSound(center, 1.0f, 1.5f);
            allaySound(center, 0.8f, 1.0f);
            moonDust(tipLoc, 15, 1.5);
            endRods(tipLoc, 10, 1.0);
            direction = new Vector(0, 0, 1); // Will be aimed at player
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (dissipated) {
                if (tick % 10 == 0) {
                    paleDust(tipLoc, 3, 1.5);
                }
                return;
            }

            // Track nearest player
            Player target = findNearestPlayer(tipLoc, 20.0);
            if (target != null) {
                Vector toPlayer = target.getLocation().add(0, 1, 0).toVector()
                    .subtract(tipLoc.toVector()).normalize();
                // Smooth tracking — blend toward player direction
                direction = direction.multiply(0.85).add(toPlayer.multiply(0.15)).normalize();
            }

            // Move spear forward
            tipLoc.add(direction.clone().multiply(0.6));
            if (modelHost != null && modelHost.isValid()) {
                modelHost.teleport(tipLoc);
            }

            // Tip damage — 8 hearts piercing
            damageNearby(tipLoc, 1.5, 16.0);

            // Shaft damage — 4 hearts, trail behind tip
            Location shaft = tipLoc.clone().subtract(direction.clone().multiply(2.0));
            damageNearby(shaft, 1.5, 8.0);

            // Trail particles
            if (tick % 2 == 0) {
                lunarDust(tipLoc, 3, 0.3);
                endRods(shaft, 2, 0.3);
                moonDust(shaft, 2, 0.4);
            }

            // Check for wall collision or max travel distance
            if (!tipLoc.getBlock().isPassable() || tipLoc.distanceSquared(c) > 900) { // 30 blocks
                dissipated = true;
                playAnimation("dissipate", 0.0, false);
                triggerImpactDamage(tipLoc);
                anchorSound(tipLoc, 0.8f, 0.8f);
                moonDust(tipLoc, 15, 2.0);
                endRods(tipLoc, 10, 1.5);
            }

            if (tick % 20 == 0) {
                chimeSound(tipLoc, 0.4f, 1.8f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new SeleniteSpear(plugin); }
    }

    // ================================================================
    // 12. RAY OF SELENE — Channeled moonlight beam.
    //     Beam core: 5 hearts/sec continuous. Beam walls: 3 hearts.
    //     Impact starburst: 4 hearts. Emitter petals: 2 hearts.
    // ================================================================
    public static class RayOfSelene extends ModelEngineAttack {
        private double beamAngle = 0;

        public RayOfSelene(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ray_of_selene", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(10.0);       // 5 hearts/sec beam core
            config.setDamageRadius(1.5);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(240);
            config.setCooldownTicks(220);
        }

        @Override protected String getModelId() { return "ray_of_selene"; }
        @Override protected double getModelScale() { return 1.8; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center.clone().add(0, 8, 0));
            playAnimation("charge", 0.0, false);
            allaySound(center, 1.2f, 1.0f);
            beaconSound(center, 1.0f, 0.6f);
            moonDust(center.clone().add(0, 8, 0), 20, 2.0);
            paleDust(center.clone().add(0, 8, 0), 15, 1.5);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            Location emitter = c.clone().add(0, 8, 0);

            beamAngle += 0.03;

            // Beam fires downward from emitter to ground
            // Beam core — 5 hearts/sec continuous
            if (tick % 20 == 0) {
                for (double y = 0; y <= 8; y += 0.5) {
                    Location beamPoint = emitter.clone().subtract(0, y, 0);
                    damageNearby(beamPoint, 1.5, 10.0); // 5 hearts
                }
            }

            // Beam visual
            if (tick % 2 == 0) {
                for (double y = 0; y <= 8; y += 1.0) {
                    Location beamPoint = emitter.clone().subtract(0, y, 0);
                    endRods(beamPoint, 2, 0.3);
                    paleDust(beamPoint, 1, 0.2);
                }
            }

            // Beam walls — 3 hearts on contact (ring around beam)
            if (tick % 15 == 0) {
                for (double y = 0; y <= 8; y += 2.0) {
                    Location wallRing = emitter.clone().subtract(0, y, 0);
                    DisplayBuilder.particleRing(wallRing, 2.0, Particle.SNOWFLAKE, 8, null);
                    for (Player p : c.getWorld().getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                        double dist = Math.sqrt(p.getLocation().distanceSquared(wallRing));
                        if (dist >= 1.5 && dist <= 2.5) {
                            p.damage(6.0); // 3 hearts
                            p.setNoDamageTicks(0);
                        }
                    }
                }
            }

            // Impact starburst at ground — 4 hearts
            if (tick % 30 == 0) {
                damageNearby(c, 3.0, 8.0); // 4 hearts
                DisplayBuilder.particleRing(c, 3.0, Particle.END_ROD, 16, null);
                moonDust(c, 8, 2.0);
            }

            // Emitter petals — 4 rotating petals, 2 hearts
            for (int i = 0; i < 4; i++) {
                double angle = beamAngle + (i * Math.PI / 2);
                double px = Math.cos(angle) * 2.5;
                double pz = Math.sin(angle) * 2.5;
                Location petal = emitter.clone().add(px, 0, pz);
                if (tick % 4 == 0) {
                    lunarDust(petal, 2, 0.2);
                }
                damageNearby(petal, 1.0, 4.0); // 2 hearts
            }

            if (tick % 40 == 0) {
                chimeSound(emitter, 0.6f, 1.5f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new RayOfSelene(plugin); }
    }

    // ================================================================
    // 13. CRATER MAKER — Moon rock boulder projectile.
    //     Boulder impact: 12 hearts + knockback.
    //     Chip fragments: 2 hearts each. Trailing debris: 1 heart.
    // ================================================================
    public static class CraterMaker extends ModelEngineAttack {
        private Location boulderLoc;
        private Vector velocity;
        private boolean impacted = false;

        public CraterMaker(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crater_maker", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(2.0);        // 1 heart trailing debris
            config.setDamageRadius(1.5);
            config.setTicksBetweenDamage(5);
            config.setDurationTicks(200);
            config.setCooldownTicks(180);
            config.setImpactDamage(24.0); // 12 hearts
            config.setImpactRadius(5.0);
        }

        @Override protected String getModelId() { return "crater_maker"; }
        @Override protected double getModelScale() { return 2.0; }

        @Override
        protected void onSpawn(Location center) {
            // Spawn high and arc toward center
            boulderLoc = center.clone().add(15, 20, 0);
            spawnModel(boulderLoc);
            playAnimation("spin", 0.0, true);
            beaconSound(center, 1.2f, 0.3f);
            voidDust(boulderLoc, 20, 2.0);

            // Calculate arc velocity toward center
            Vector toCenter = center.toVector().subtract(boulderLoc.toVector());
            velocity = toCenter.normalize().multiply(0.8);
            velocity.setY(-0.3);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (impacted) {
                // Chip fragment scatter after impact
                if (tick % 10 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double dist = 1.0 + Math.random() * 4.0;
                        Location chip = boulderLoc.clone().add(Math.cos(angle) * dist, 0.5, Math.sin(angle) * dist);
                        moonDust(chip, 3, 0.3);
                        damageNearby(chip, 1.0, 4.0); // 2 hearts
                    }
                }
                if (tick % 15 == 0) {
                    voidDust(boulderLoc, 4, 2.0);
                }
                return;
            }

            // Move boulder along arc
            velocity.setY(velocity.getY() - 0.02); // Gravity
            boulderLoc.add(velocity);
            if (modelHost != null && modelHost.isValid()) {
                modelHost.teleport(boulderLoc);
            }

            // Trailing debris — 1 heart on contact
            if (tick % 3 == 0) {
                Location trail = boulderLoc.clone().subtract(velocity.clone().normalize().multiply(2.0));
                voidDust(trail, 3, 0.5);
                moonDust(trail, 2, 0.4);
                damageNearby(trail, 1.5, 2.0); // 1 heart
            }

            // Check for ground hit
            if (!boulderLoc.getBlock().isPassable() || boulderLoc.getY() <= c.getY()) {
                impacted = true;
                boulderLoc = c.clone(); // Snap to ground
                playAnimation("shatter", 0.0, false);
                triggerImpactDamage(boulderLoc);

                // Knockback
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(boulderLoc) <= 25.0) {
                        Vector kb = p.getLocation().toVector().subtract(boulderLoc.toVector()).normalize().multiply(1.5);
                        kb.setY(0.5);
                        p.setVelocity(kb);
                    }
                }

                anchorSound(boulderLoc, 1.5f, 0.3f);
                freezeSound(boulderLoc, 1.0f, 0.5f);
                voidDust(boulderLoc, 40, 4.0);
                moonDust(boulderLoc, 30, 3.0);
                DisplayBuilder.particleRing(boulderLoc, 5.0, Particle.END_ROD, 24, null);
            }

            if (tick % 15 == 0) {
                beaconSound(boulderLoc, 0.6f, 0.4f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new CraterMaker(plugin); }
    }

    // ================================================================
    // 14. RIPTIDE — Wide tidal force beam.
    //     Beam layers: 4 hearts/sec. Ripple crests: 2 hearts extra.
    //     Impact fan: 5 hearts burst. Players pushed sideways by flow.
    // ================================================================
    public static class Riptide extends ModelEngineAttack {
        private double sweepAngle = 0;
        private Vector beamDir;

        public Riptide(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("riptide", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(8.0);        // 4 hearts/sec beam layers
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(260);
            config.setCooldownTicks(240);
        }

        @Override protected String getModelId() { return "riptide"; }
        @Override protected double getModelScale() { return 1.8; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center);
            playAnimation("surge", 0.0, false);
            anchorSound(center, 1.2f, 0.5f);
            beaconSound(center, 0.8f, 0.6f);
            lunarDust(center, 20, 2.0);
            snowflakes(center, 15, 1.5);

            // Random initial beam direction
            double angle = Math.random() * 2 * Math.PI;
            beamDir = new Vector(Math.cos(angle), 0, Math.sin(angle)).normalize();
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            sweepAngle += 0.02;

            // Slowly rotate beam direction
            double angle = Math.atan2(beamDir.getZ(), beamDir.getX()) + 0.02;
            beamDir = new Vector(Math.cos(angle), 0, Math.sin(angle)).normalize();

            // Beam extends 12 blocks from center
            Vector perpendicular = new Vector(-beamDir.getZ(), 0, beamDir.getX());

            for (double d = 0; d <= 12; d += 1.0) {
                Location beamPoint = c.clone().add(beamDir.clone().multiply(d));

                // Beam layer damage — 4 hearts/sec
                if (tick % 20 == 0) {
                    damageNearby(beamPoint, 3.0, 8.0);

                    // Push players perpendicular to beam flow
                    for (Player p : c.getWorld().getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                        if (p.getLocation().distanceSquared(beamPoint) <= 9.0) {
                            p.setVelocity(p.getVelocity().add(perpendicular.clone().multiply(0.4)));
                        }
                    }
                }

                // Beam visual
                if (tick % 2 == 0 && d % 2 == 0) {
                    lunarDust(beamPoint, 2, 0.5);
                    endRods(beamPoint, 1, 0.3);
                }
            }

            // Ripple crests every 3 blocks — 2 hearts extra
            if (tick % 15 == 0) {
                for (double d = 3; d <= 12; d += 3) {
                    Location crest = c.clone().add(beamDir.clone().multiply(d));
                    DisplayBuilder.particleRing(crest, 2.0, Particle.SNOWFLAKE, 10, null);
                    damageNearby(crest, 2.5, 4.0); // 2 hearts extra
                    moonDust(crest, 4, 0.5);
                }
            }

            // Impact fan at beam end — 5 hearts burst
            if (tick % 40 == 0) {
                Location beamEnd = c.clone().add(beamDir.clone().multiply(12));
                damageNearby(beamEnd, 4.0, 10.0); // 5 hearts
                DisplayBuilder.particleRing(beamEnd, 4.0, Particle.END_ROD, 16, null);
                moonDust(beamEnd, 10, 2.0);
                anchorSound(beamEnd, 0.6f, 0.7f);
            }

            if (tick % 30 == 0) {
                beaconSound(c, 0.5f, 0.5f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new Riptide(plugin); }
    }

    // ================================================================
    // 15. DARK HALF — Half-light half-shadow eclipse projectile.
    //     Light hemisphere: 6 hearts cold. Dark hemisphere: 6 hearts void.
    //     Equator ring: 4 hearts. Split on impact: 8 hearts burst.
    // ================================================================
    public static class DarkHalf extends ModelEngineAttack {
        private Location projectileLoc;
        private Vector velocity;
        private boolean split = false;
        private double spinAngle = 0;

        public DarkHalf(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_half", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(8.0);        // 4 hearts equator
            config.setDamageRadius(1.5);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(220);
            config.setCooldownTicks(200);
            config.setImpactDamage(16.0); // 8 hearts split burst
            config.setImpactRadius(4.0);
        }

        @Override protected String getModelId() { return "dark_half"; }
        @Override protected double getModelScale() { return 1.5; }

        @Override
        protected void onSpawn(Location center) {
            projectileLoc = center.clone().add(0, 4, -10);
            spawnModel(projectileLoc);
            playAnimation("form", 0.0, false);
            allaySound(center, 1.0f, 0.6f);
            beaconSound(center, 0.8f, 0.4f);
            paleDust(projectileLoc, 15, 1.5);
            voidDust(projectileLoc, 15, 1.5);

            // Aim toward center
            velocity = center.toVector().add(new Vector(0, 2, 0)).subtract(projectileLoc.toVector()).normalize().multiply(0.5);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (split) {
                if (tick % 10 == 0) {
                    paleDust(projectileLoc, 4, 2.0);
                    voidDust(projectileLoc, 4, 2.0);
                }
                return;
            }

            spinAngle += 0.1;

            // Move projectile
            projectileLoc.add(velocity);
            if (modelHost != null && modelHost.isValid()) {
                modelHost.teleport(projectileLoc);
            }

            // Light hemisphere — 6 hearts cold (one side)
            Location lightSide = projectileLoc.clone().add(
                Math.cos(spinAngle) * 1.5, 0, Math.sin(spinAngle) * 1.5);
            damageNearby(lightSide, 1.5, 12.0); // 6 hearts
            if (tick % 3 == 0) {
                paleDust(lightSide, 2, 0.3);
                endRods(lightSide, 1, 0.1);
            }

            // Dark hemisphere — 6 hearts void (opposite side)
            Location darkSide = projectileLoc.clone().add(
                Math.cos(spinAngle + Math.PI) * 1.5, 0, Math.sin(spinAngle + Math.PI) * 1.5);
            damageNearby(darkSide, 1.5, 12.0); // 6 hearts
            if (tick % 3 == 0) {
                voidDust(darkSide, 2, 0.3);
            }

            // Equator ring — 4 hearts
            if (tick % 10 == 0) {
                DisplayBuilder.particleRing(projectileLoc, 1.5, Particle.END_ROD, 10, null);
                damageNearby(projectileLoc, 1.5, 8.0);
            }

            // Check for wall collision or near center
            if (!projectileLoc.getBlock().isPassable() ||
                projectileLoc.distanceSquared(c) < 4.0 ||
                tick > 160) {
                split = true;
                playAnimation("split", 0.0, false);
                triggerImpactDamage(projectileLoc);
                anchorSound(projectileLoc, 1.0f, 0.5f);
                freezeSound(projectileLoc, 0.8f, 0.6f);
                paleDust(projectileLoc, 25, 3.0);
                voidDust(projectileLoc, 25, 3.0);
                endRods(projectileLoc, 15, 2.0);
            }

            if (tick % 25 == 0) {
                chimeSound(projectileLoc, 0.5f, 0.8f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new DarkHalf(plugin); }
    }

    // ================================================================
    // 16. CARTOGRAPHY — Map-drawing selenograph bolt.
    //     Diamond tip: 7 hearts piercing.
    //     Trail lines mark ground (2 hearts/sec for 100 ticks).
    // ================================================================
    public static class Cartography extends ModelEngineAttack {
        private Location boltLoc;
        private Vector velocity;
        private boolean finished = false;
        /** Marked ground positions with the tick they were marked at. */
        private final Map<Location, Integer> markedGround = new HashMap<>();

        public Cartography(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cartography", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(4.0);        // 2 hearts/sec marked ground
            config.setDamageRadius(1.5);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(260);
            config.setCooldownTicks(240);
        }

        @Override protected String getModelId() { return "cartography"; }
        @Override protected double getModelScale() { return 1.2; }

        @Override
        protected void onSpawn(Location center) {
            boltLoc = center.clone().add(8, 3, 0);
            spawnModel(boltLoc);
            playAnimation("launch", 0.0, false);
            chimeSound(center, 1.0f, 1.8f);
            allaySound(center, 0.8f, 1.2f);
            moonDust(boltLoc, 10, 1.0);
            endRods(boltLoc, 8, 0.8);

            // Aim toward nearest player or center
            Player target = findNearestPlayer(center, 15.0);
            if (target != null) {
                velocity = target.getLocation().add(0, 1, 0).toVector()
                    .subtract(boltLoc.toVector()).normalize().multiply(0.7);
            } else {
                velocity = center.toVector().subtract(boltLoc.toVector()).normalize().multiply(0.7);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Marked ground damage — 2 hearts/sec for 100 ticks after marking
            markedGround.entrySet().removeIf(entry -> tick - entry.getValue() > 100);
            if (tick % 20 == 0) {
                for (Location mark : markedGround.keySet()) {
                    damageNearby(mark, 1.5, 4.0); // 2 hearts
                    lunarDust(mark, 2, 0.2);
                }
            }

            // Marked ground ambient particles
            if (tick % 8 == 0) {
                for (Location mark : markedGround.keySet()) {
                    voidDust(mark, 1, 0.2);
                }
            }

            if (finished) return;

            // Move bolt
            boltLoc.add(velocity);
            if (modelHost != null && modelHost.isValid()) {
                modelHost.teleport(boltLoc);
            }

            // Diamond tip damage — 7 hearts piercing
            damageNearby(boltLoc, 1.5, 14.0);

            // Mark ground below bolt
            Location ground = boltLoc.clone();
            ground.setY(c.getY());
            markedGround.put(ground.clone(), tick);

            // Trail visual
            if (tick % 2 == 0) {
                moonDust(boltLoc, 2, 0.3);
                endRods(boltLoc, 1, 0.1);
                // Line from bolt to ground
                DisplayBuilder.particleLine(boltLoc, ground, Particle.DUST, 2,
                    new Particle.DustOptions(Color.fromRGB(96, 144, 232), 0.8f));
            }

            // Check if bolt has traveled far enough or hit a wall
            if (!boltLoc.getBlock().isPassable() || boltLoc.distanceSquared(c) > 625) { // 25 blocks
                finished = true;
                playAnimation("dissipate", 0.0, false);
                paleDust(boltLoc, 10, 1.5);
                chimeSound(boltLoc, 0.6f, 2.0f);
            }

            if (tick % 20 == 0) {
                chimeSound(boltLoc, 0.3f, 1.6f);
            }
        }

        @Override protected void onModelCleanup() { markedGround.clear(); }
        @Override public AbstractAttack newInstance() { return new Cartography(plugin); }
    }

    // ================================================================
    // 17. TIDAL SENTINEL — Humanoid moon guardian.
    //     Sword arm sweep: 5 hearts in arc. Shield bash: 3 hearts + KB.
    //     Gravitational aura: 2 hearts/sec in 4 blocks. Tracks players.
    // ================================================================
    public static class TidalSentinel extends ModelEngineAttack {
        private int attackCycle = 0;
        private double facingAngle = 0;

        public TidalSentinel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tidal_sentinel", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(4.0);        // 2 hearts/sec gravitational aura
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(320);
            config.setCooldownTicks(300);
            config.setTracksPlayer(true);
        }

        @Override protected String getModelId() { return "tidal_sentinel"; }
        @Override protected double getModelScale() { return 2.0; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center);
            playAnimation("summon", 0.0, false);
            anchorSound(center, 1.2f, 0.5f);
            beaconSound(center, 1.0f, 0.4f);
            lunarDust(center, 25, 2.0);
            endRods(center, 15, 1.5);
            voidDust(center, 10, 1.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            attackCycle++;

            // Track nearest player — face toward them
            Player target = findNearestPlayer(c, 15.0);
            if (target != null) {
                Vector toTarget = target.getLocation().toVector().subtract(c.toVector());
                facingAngle = Math.atan2(toTarget.getZ(), toTarget.getX());

                // Move slowly toward player
                if (c.distanceSquared(target.getLocation()) > 9.0) { // > 3 blocks
                    Vector move = toTarget.normalize().multiply(0.15);
                    c.add(move);
                    if (modelHost != null && modelHost.isValid()) {
                        modelHost.teleport(c);
                    }
                }
            }

            // Gravitational aura — 2 hearts/sec in 4 blocks
            if (tick % 20 == 0) {
                damageNearby(c, 4.0, 4.0);
            }

            // Aura particles
            if (tick % 5 == 0) {
                DisplayBuilder.particleRing(c, 4.0, Particle.SNOWFLAKE, 12, null);
                lunarDust(c, 3, 2.0);
            }

            // Sword arm sweep every 60 ticks — 5 hearts in arc
            if (attackCycle % 60 == 0) {
                playAnimation("slash", 0.0, false);
                anchorSound(c, 0.8f, 0.8f);
                for (int i = -2; i <= 2; i++) {
                    double angle = facingAngle + (i * 0.3);
                    double sx = Math.cos(angle) * 3.5;
                    double sz = Math.sin(angle) * 3.5;
                    Location sweep = c.clone().add(sx, 1.5, sz);
                    damageNearby(sweep, 1.5, 10.0); // 5 hearts
                    moonDust(sweep, 3, 0.3);
                    endRods(sweep, 1, 0.1);
                }
            }

            // Shield bash every 90 ticks — 3 hearts + knockback
            if (attackCycle % 90 == 30) {
                playAnimation("bash", 0.0, false);
                freezeSound(c, 0.7f, 0.6f);
                Location bashLoc = c.clone().add(Math.cos(facingAngle) * 2.5, 1.0, Math.sin(facingAngle) * 2.5);
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(bashLoc) <= 4.0) {
                        p.damage(6.0); // 3 hearts
                        p.setNoDamageTicks(0);
                        Vector kb = p.getLocation().toVector().subtract(c.toVector()).normalize().multiply(1.2);
                        kb.setY(0.4);
                        p.setVelocity(kb);
                    }
                }
                voidDust(bashLoc, 8, 1.0);
            }

            if (tick % 50 == 0) {
                allaySound(c, 0.5f, 0.5f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new TidalSentinel(plugin); }
    }

    // ================================================================
    // 18. LUNAR MONOLITH — Tall featureless rectangle.
    //     Proximity aura: 3 hearts/sec in 5 blocks.
    //     Pulse every 60 ticks: 6 hearts to all in 8 blocks.
    //     Gazing (within 15 deg): 4 hearts extra.
    // ================================================================
    public static class LunarMonolith extends ModelEngineAttack {

        public LunarMonolith(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lunar_monolith", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(6.0);        // 3 hearts/sec proximity
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(360);
            config.setCooldownTicks(340);
        }

        @Override protected String getModelId() { return "lunar_monolith"; }
        @Override protected double getModelScale() { return 2.5; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center);
            playAnimation("materialize", 0.0, false);
            beaconSound(center, 1.5f, 0.2f);
            allaySound(center, 1.0f, 0.3f);
            voidDust(center, 30, 3.0);
            endRods(center.clone().add(0, 4, 0), 15, 1.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            Location monolithTop = c.clone().add(0, 6, 0);

            // Proximity aura — 3 hearts/sec in 5 blocks
            if (tick % 20 == 0) {
                damageNearby(c, 5.0, 6.0);
            }

            // Pulse every 60 ticks — 6 hearts to all in 8 blocks
            if (tick % 60 == 0) {
                playAnimation("pulse", 0.0, false);
                damageNearby(c, 8.0, 12.0); // 6 hearts
                anchorSound(c, 1.0f, 0.4f);
                moonDust(c, 20, 5.0);
                DisplayBuilder.particleRing(c, 8.0, Particle.END_ROD, 24, null);
                voidDust(c, 15, 4.0);
            }

            // Gaze check — looking within 15 degrees of monolith = 4 hearts extra
            if (tick % 20 == 0) {
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) > 225) continue; // 15 block range
                    Vector toMonolith = monolithTop.toVector().subtract(p.getEyeLocation().toVector()).normalize();
                    Vector lookDir = p.getEyeLocation().getDirection().normalize();
                    double dot = lookDir.dot(toMonolith);
                    // cos(15 deg) ~ 0.966
                    if (dot >= 0.966) {
                        p.damage(8.0); // 4 hearts
                        p.setNoDamageTicks(0);
                        voidDust(p.getEyeLocation(), 6, 0.5);
                        freezeSound(p.getLocation(), 0.6f, 0.4f);
                    }
                }
            }

            // Ambient monolith particles
            if (tick % 5 == 0) {
                voidDust(c.clone().add(0, 3, 0), 3, 0.5);
                endRods(monolithTop, 2, 0.3);
            }

            if (tick % 8 == 0) {
                // Vertical particle line on the monolith
                for (double y = 0; y <= 6; y += 1.5) {
                    lunarDust(c.clone().add(0, y, 0), 1, 0.2);
                }
            }

            if (tick % 50 == 0) {
                beaconSound(c, 0.6f, 0.3f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new LunarMonolith(plugin); }
    }

    // ================================================================
    // 19. MOON PHASE TOTEM — Rotating totem with 4 phase faces.
    //     Cycles through 4 phases every 50 ticks.
    //     New moon: 2 hearts/sec aura. Crescent: 4 hearts directional.
    //     Half: 6 hearts burst. Full: 8 hearts nova.
    // ================================================================
    public static class MoonPhaseTotem extends ModelEngineAttack {
        private int currentPhase = 0; // 0=new, 1=crescent, 2=half, 3=full
        private int phaseTimer = 0;
        private double totemAngle = 0;

        public MoonPhaseTotem(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("moon_phase_totem", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(4.0);        // 2 hearts/sec new moon aura
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(340);
            config.setCooldownTicks(320);
        }

        @Override protected String getModelId() { return "moon_phase_totem"; }
        @Override protected double getModelScale() { return 1.8; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center);
            playAnimation("emerge", 0.0, false);
            chimeSound(center, 1.0f, 0.8f);
            beaconSound(center, 0.8f, 0.5f);
            lunarDust(center, 20, 2.0);
            endRods(center, 12, 1.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            phaseTimer++;
            totemAngle += 0.04;

            // Phase transition every 50 ticks
            if (phaseTimer >= 50) {
                phaseTimer = 0;
                currentPhase = (currentPhase + 1) % 4;
                playAnimation("phase_" + currentPhase, 0.0, false);
                chimeSound(c, 0.7f, 0.8f + currentPhase * 0.3f);
            }

            switch (currentPhase) {
                case 0 -> { // New Moon — 2 hearts/sec aura in 5 blocks
                    if (tick % 20 == 0) {
                        damageNearby(c, 5.0, 4.0); // 2 hearts
                        voidDust(c, 8, 3.0);
                    }
                    if (tick % 6 == 0) {
                        voidDust(c.clone().add(0, 2, 0), 3, 1.0);
                    }
                }
                case 1 -> { // Crescent — 4 hearts directional in facing direction
                    if (tick % 20 == 0) {
                        for (int i = -2; i <= 2; i++) {
                            double angle = totemAngle + (i * 0.25);
                            double dx = Math.cos(angle) * 4.0;
                            double dz = Math.sin(angle) * 4.0;
                            Location crescentHit = c.clone().add(dx, 1.5, dz);
                            damageNearby(crescentHit, 2.0, 8.0); // 4 hearts
                            moonDust(crescentHit, 3, 0.3);
                        }
                    }
                    if (tick % 4 == 0) {
                        double cx = Math.cos(totemAngle) * 3.5;
                        double cz = Math.sin(totemAngle) * 3.5;
                        paleDust(c.clone().add(cx, 2, cz), 2, 0.3);
                    }
                }
                case 2 -> { // Half — 6 hearts burst every 20 ticks in 4 blocks
                    if (tick % 20 == 0) {
                        damageNearby(c, 4.0, 12.0); // 6 hearts
                        DisplayBuilder.particleRing(c, 4.0, Particle.END_ROD, 16, null);
                        lunarDust(c, 10, 2.5);
                        anchorSound(c, 0.6f, 0.7f);
                    }
                    if (tick % 5 == 0) {
                        lunarDust(c.clone().add(0, 2, 0), 3, 1.0);
                    }
                }
                case 3 -> { // Full — 8 hearts nova in 6 blocks every 20 ticks
                    if (tick % 20 == 0) {
                        damageNearby(c, 6.0, 16.0); // 8 hearts
                        DisplayBuilder.particleRing(c, 6.0, Particle.END_ROD, 24, null);
                        moonDust(c, 15, 4.0);
                        endRods(c, 10, 3.0);
                        anchorSound(c, 0.8f, 0.5f);
                        freezeSound(c, 0.6f, 0.6f);
                    }
                    if (tick % 4 == 0) {
                        moonDust(c.clone().add(0, 3, 0), 4, 1.5);
                        endRods(c.clone().add(0, 2, 0), 2, 0.5);
                    }
                }
            }

            // Totem rotation visual
            if (tick % 6 == 0) {
                for (int i = 0; i < 4; i++) {
                    double angle = totemAngle + (i * Math.PI / 2);
                    Location face = c.clone().add(Math.cos(angle) * 1.0, 2.0, Math.sin(angle) * 1.0);
                    endRods(face, 1, 0.1);
                }
            }

            if (tick % 60 == 0) {
                beaconSound(c, 0.5f, 0.4f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new MoonPhaseTotem(plugin); }
    }

    // ================================================================
    // 20. SILVER TREE OF TIDES — Crystalline tree shape.
    //     Roots: 2 hearts on contact. Trunk proximity: 3 hearts/sec.
    //     Branch tips fire 4 hearts bolts every 40 ticks at nearest player.
    // ================================================================
    public static class SilverTreeOfTides extends ModelEngineAttack {
        private int boltTimer = 0;

        public SilverTreeOfTides(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("silver_tree_of_tides", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(6.0);        // 3 hearts/sec trunk proximity
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(360);
            config.setCooldownTicks(340);
        }

        @Override protected String getModelId() { return "silver_tree_of_tides"; }
        @Override protected double getModelScale() { return 2.5; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center);
            playAnimation("grow", 0.0, false);
            chimeSound(center, 1.2f, 0.6f);
            allaySound(center, 1.0f, 0.8f);
            beaconSound(center, 0.8f, 0.4f);
            lunarDust(center, 25, 2.5);
            paleDust(center.clone().add(0, 4, 0), 15, 2.0);
            endRods(center.clone().add(0, 6, 0), 10, 1.5);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            boltTimer++;

            // Root damage — 2 hearts on contact, 6 roots radiating outward
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6;
                for (double d = 2.0; d <= 5.0; d += 1.5) {
                    Location root = c.clone().add(Math.cos(angle) * d, 0, Math.sin(angle) * d);
                    damageNearby(root, 1.0, 4.0); // 2 hearts

                    if (tick % 8 == 0 && d == 3.5) {
                        lunarDust(root, 1, 0.2);
                    }
                }
            }

            // Trunk proximity — 3 hearts/sec
            if (tick % 20 == 0) {
                damageNearby(c, 3.0, 6.0);
            }

            // Trunk particles
            if (tick % 5 == 0) {
                for (double y = 0; y <= 5; y += 1.0) {
                    paleDust(c.clone().add(0, y, 0), 1, 0.3);
                }
                endRods(c.clone().add(0, 5, 0), 2, 1.0);
            }

            // Branch tip bolts every 40 ticks — 4 hearts at nearest player
            if (boltTimer % 40 == 0) {
                Player target = findNearestPlayer(c, 15.0);
                if (target != null) {
                    playAnimation("fire", 0.0, false);

                    // 5 branch tips at different angles and heights
                    double[] branchAngles = {0, Math.PI * 0.4, Math.PI * 0.8, Math.PI * 1.2, Math.PI * 1.6};
                    for (double bAngle : branchAngles) {
                        Location branchTip = c.clone().add(
                            Math.cos(bAngle) * 3.5, 4.5 + Math.sin(bAngle) * 1.5, Math.sin(bAngle) * 3.5
                        );

                        // Visual bolt toward player
                        DisplayBuilder.particleLine(branchTip, target.getLocation().add(0, 1, 0),
                            Particle.END_ROD, 3, null);
                        moonDust(branchTip, 4, 0.4);
                    }

                    // Damage at player location — 4 hearts
                    damageNearby(target.getLocation(), 2.0, 8.0);
                    chimeSound(target.getLocation(), 0.5f, 1.5f);
                    lunarDust(target.getLocation(), 6, 0.8);
                }
            }

            // Root spread ambient
            if (tick % 10 == 0) {
                DisplayBuilder.particleRing(c, 4.0, Particle.SNOWFLAKE, 10, null);
            }

            if (tick % 50 == 0) {
                allaySound(c.clone().add(0, 4, 0), 0.5f, 0.7f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new SilverTreeOfTides(plugin); }
    }

    // ================================================================
    // 21. LUNAR FOG — Low-hanging fog cloud.
    //     Inside fog: 1 heart/sec + freeze ticks 40.
    //     Drifts slowly toward nearest player.
    // ================================================================
    public static class LunarFog extends ModelEngineAttack {
        private Location fogCenter;

        public LunarFog(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lunar_fog", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(2.0);        // 1 heart/sec
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(400);
            config.setCooldownTicks(380);
        }

        @Override protected String getModelId() { return "lunar_fog"; }
        @Override protected double getModelScale() { return 3.0; }

        @Override
        protected void onSpawn(Location center) {
            fogCenter = center.clone();
            spawnModel(fogCenter);
            playAnimation("spread", 0.0, false);
            allaySound(center, 0.8f, 0.3f);
            paleDust(center, 30, 4.0);
            snowflakes(center, 20, 3.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Drift toward nearest player
            Player target = findNearestPlayer(fogCenter, 20.0);
            if (target != null) {
                Vector toPlayer = target.getLocation().toVector().subtract(fogCenter.toVector());
                if (toPlayer.lengthSquared() > 4.0) {
                    fogCenter.add(toPlayer.normalize().multiply(0.08));
                    if (modelHost != null && modelHost.isValid()) {
                        modelHost.teleport(fogCenter);
                    }
                }
            }

            // Damage + freeze players inside fog — 1 heart/sec
            if (tick % 20 == 0) {
                for (Player p : fogCenter.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    double hDist = Math.sqrt(
                        Math.pow(p.getLocation().getX() - fogCenter.getX(), 2) +
                        Math.pow(p.getLocation().getZ() - fogCenter.getZ(), 2)
                    );
                    double vDist = Math.abs(p.getLocation().getY() - fogCenter.getY());
                    if (hDist <= 6.0 && vDist <= 2.5) {
                        p.damage(2.0); // 1 heart
                        p.setNoDamageTicks(0);
                        p.setFreezeTicks(Math.min(p.getFreezeTicks() + 40, 140));
                    }
                }
            }

            // Fog visual particles
            if (tick % 3 == 0) {
                for (int i = 0; i < 5; i++) {
                    double ox = (Math.random() - 0.5) * 10.0;
                    double oy = (Math.random() - 0.5) * 2.0;
                    double oz = (Math.random() - 0.5) * 10.0;
                    Location fogParticle = fogCenter.clone().add(ox, oy, oz);
                    paleDust(fogParticle, 1, 0.5);
                    snowflakes(fogParticle, 1, 0.3);
                }
            }

            if (tick % 5 == 0) {
                moonDust(fogCenter, 4, 3.0);
            }

            if (tick % 60 == 0) {
                allaySound(fogCenter, 0.4f, 0.3f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new LunarFog(plugin); }
    }

    // ================================================================
    // 22. MOONDUST FALL — Falling particle column.
    //     Standing under: 2 hearts/sec.
    //     Stacking: +1 heart/sec per 60 ticks inside, max 5 hearts/sec.
    // ================================================================
    public static class MoondustFall extends ModelEngineAttack {
        private final Map<UUID, Integer> exposureTicks = new HashMap<>();

        public MoondustFall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("moondust_fall", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(4.0);        // 2 hearts/sec base
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(360);
            config.setCooldownTicks(340);
        }

        @Override protected String getModelId() { return "moondust_fall"; }
        @Override protected double getModelScale() { return 2.0; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center.clone().add(0, 12, 0));
            playAnimation("fall", 0.0, true);
            chimeSound(center, 0.8f, 1.2f);
            beaconSound(center, 0.6f, 0.6f);
            paleDust(center.clone().add(0, 12, 0), 20, 2.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Falling dust visual from Y+12 down to ground
            if (tick % 2 == 0) {
                for (int i = 0; i < 4; i++) {
                    double ox = (Math.random() - 0.5) * 4.0;
                    double oy = Math.random() * 12.0;
                    double oz = (Math.random() - 0.5) * 4.0;
                    Location dustLoc = c.clone().add(ox, oy, oz);
                    paleDust(dustLoc, 1, 0.2);
                    if (Math.random() < 0.3) {
                        endRods(dustLoc, 1, 0.1);
                    }
                }
            }

            // Stacking damage to players standing under
            if (tick % 20 == 0) {
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    double hDist = Math.sqrt(
                        Math.pow(p.getLocation().getX() - c.getX(), 2) +
                        Math.pow(p.getLocation().getZ() - c.getZ(), 2)
                    );
                    if (hDist <= 3.0) {
                        UUID pid = p.getUniqueId();
                        int exposure = exposureTicks.getOrDefault(pid, 0) + 20;
                        exposureTicks.put(pid, exposure);

                        // Base 2 hearts/sec + 1 heart/sec per 60 ticks exposure, max 5 hearts/sec
                        int extraHearts = Math.min(exposure / 60, 3); // max +3 = 5 total
                        double dmg = 4.0 + (extraHearts * 2.0); // 2 hearts + extra
                        p.damage(dmg);
                        p.setNoDamageTicks(0);
                        moonDust(p.getLocation().add(0, 2, 0), 3, 0.5);
                    } else {
                        // Player left — reset exposure slowly
                        UUID pid = p.getUniqueId();
                        int exposure = exposureTicks.getOrDefault(pid, 0);
                        if (exposure > 0) {
                            exposureTicks.put(pid, Math.max(0, exposure - 10));
                        }
                    }
                }
            }

            // Ground accumulation visual
            if (tick % 8 == 0) {
                DisplayBuilder.particleRing(c, 3.0, Particle.SNOWFLAKE, 10, null);
                moonDust(c, 3, 1.5);
            }

            if (tick % 50 == 0) {
                chimeSound(c.clone().add(0, 6, 0), 0.4f, 1.4f);
            }
        }

        @Override protected void onModelCleanup() { exposureTicks.clear(); }
        @Override public AbstractAttack newInstance() { return new MoondustFall(plugin); }
    }

    // ================================================================
    // 23. TIDAL POOL — Ground water-like pool.
    //     Stepping on pool: slow + 2 hearts/sec.
    //     Center: 4 hearts/sec + pull inward. Edge: 1 heart/sec.
    // ================================================================
    public static class TidalPool extends ModelEngineAttack {

        public TidalPool(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tidal_pool", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(4.0);        // 2 hearts/sec general pool
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(360);
            config.setCooldownTicks(340);
        }

        @Override protected String getModelId() { return "tidal_pool"; }
        @Override protected double getModelScale() { return 2.5; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center);
            playAnimation("form", 0.0, false);
            anchorSound(center, 1.0f, 0.5f);
            allaySound(center, 0.8f, 0.4f);
            lunarDust(center, 25, 3.0);
            snowflakes(center, 15, 2.5);
            DisplayBuilder.particleRing(center, 5.0, Particle.END_ROD, 20, null);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (tick % 20 == 0) {
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    double dist = Math.sqrt(p.getLocation().distanceSquared(c));
                    double vDist = Math.abs(p.getLocation().getY() - c.getY());

                    if (vDist > 2.0) continue; // Must be at pool level

                    if (dist <= 2.0) {
                        // Center — 4 hearts/sec + pull inward
                        p.damage(8.0); // 4 hearts
                        p.setNoDamageTicks(0);
                        pullPlayersToward(c, 2.5, 0.25);
                        // Slow effect via freeze ticks
                        p.setFreezeTicks(Math.min(p.getFreezeTicks() + 30, 100));
                    } else if (dist <= 4.0) {
                        // Pool body — 2 hearts/sec + slow
                        p.damage(4.0); // 2 hearts
                        p.setNoDamageTicks(0);
                        p.setFreezeTicks(Math.min(p.getFreezeTicks() + 20, 80));
                    } else if (dist <= 5.5) {
                        // Edge — 1 heart/sec
                        p.damage(2.0); // 1 heart
                        p.setNoDamageTicks(0);
                    }
                }
            }

            // Pool surface ripples
            if (tick % 4 == 0) {
                double rippleRadius = 2.0 + (tick % 40) * 0.1;
                if (rippleRadius <= 5.0) {
                    DisplayBuilder.particleRing(c.clone().add(0, 0.1, 0), rippleRadius, Particle.SNOWFLAKE, 12, null);
                }
            }

            // Center swirl
            if (tick % 3 == 0) {
                double angle = tick * 0.15;
                Location swirl = c.clone().add(Math.cos(angle) * 1.5, 0.2, Math.sin(angle) * 1.5);
                lunarDust(swirl, 2, 0.2);
                endRods(swirl, 1, 0.1);
            }

            // Edge shimmer
            if (tick % 6 == 0) {
                for (int i = 0; i < 3; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    Location edge = c.clone().add(Math.cos(angle) * 5.0, 0.1, Math.sin(angle) * 5.0);
                    paleDust(edge, 1, 0.2);
                }
            }

            if (tick % 50 == 0) {
                beaconSound(c, 0.4f, 0.6f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new TidalPool(plugin); }
    }

    // ================================================================
    // 24. GRAVITY WELL — Invisible force pulling inward.
    //     Pull velocity toward center. Closer = stronger + more damage.
    //     1-6 hearts/sec scaling with distance. Center: 8 hearts burst.
    // ================================================================
    public static class GravityWell extends ModelEngineAttack {

        public GravityWell(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gravity_well", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(2.0);        // base (overridden by distance scaling)
            config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
        }

        @Override protected String getModelId() { return "gravity_well"; }
        @Override protected double getModelScale() { return 1.5; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center);
            playAnimation("open", 0.0, false);
            beaconSound(center, 1.0f, 0.3f);
            voidDust(center, 20, 2.0);
            endRods(center, 10, 1.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (tick % 20 == 0) {
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    double dist = Math.sqrt(p.getLocation().distanceSquared(c));

                    if (dist > 10.0) continue;

                    // Pull strength scales with proximity (closer = stronger)
                    double pullStrength = 0.1 + (1.0 - dist / 10.0) * 0.4;
                    Vector pull = c.toVector().subtract(p.getLocation().toVector()).normalize().multiply(pullStrength);
                    p.setVelocity(p.getVelocity().add(pull));

                    // Damage scales: 1 heart at edge (10 blocks) to 6 hearts at close range (1 block)
                    double damageHearts = 1.0 + (1.0 - dist / 10.0) * 5.0;
                    double dmg = damageHearts * 2.0;
                    p.damage(dmg);
                    p.setNoDamageTicks(0);

                    // Center burst — 8 hearts if within 1.5 blocks
                    if (dist <= 1.5) {
                        p.damage(16.0); // 8 hearts
                        p.setNoDamageTicks(0);
                        voidDust(p.getLocation(), 10, 0.5);
                        freezeSound(p.getLocation(), 0.7f, 0.4f);
                    }
                }
            }

            // Gravitational distortion particles — subtle inward pull visual
            if (tick % 3 == 0) {
                for (int i = 0; i < 4; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = 3.0 + Math.random() * 7.0;
                    Location outer = c.clone().add(Math.cos(angle) * dist, Math.random() * 2.0, Math.sin(angle) * dist);
                    voidDust(outer, 1, 0.1);
                }
            }

            // Center singularity visual
            if (tick % 4 == 0) {
                endRods(c, 2, 0.3);
                lunarDust(c, 2, 0.5);
            }

            // Concentric rings showing gravity field
            if (tick % 10 == 0) {
                DisplayBuilder.particleRing(c, 3.0, Particle.END_ROD, 8, null);
                DisplayBuilder.particleRing(c, 6.0, Particle.SNOWFLAKE, 10, null);
                DisplayBuilder.particleRing(c, 9.0, Particle.SNOWFLAKE, 12, null);
            }

            if (tick % 40 == 0) {
                beaconSound(c, 0.6f, 0.3f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new GravityWell(plugin); }
    }

    // ================================================================
    // 25. CELESTIAL AURORA — Shimmering curtain of moonlight.
    //     Curtain sweeps across area: 3 hearts to all it passes through.
    //     Sweeps every 40 ticks in alternating directions.
    //     Standing in path: 2 hearts/sec.
    // ================================================================
    public static class CelestialAurora extends ModelEngineAttack {
        private double sweepX = -10.0;
        private boolean sweepRight = true;
        private int sweepCooldown = 0;

        public CelestialAurora(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("celestial_aurora", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(4.0);        // 2 hearts/sec standing in path
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(360);
            config.setCooldownTicks(340);
        }

        @Override protected String getModelId() { return "celestial_aurora"; }
        @Override protected double getModelScale() { return 3.0; }

        @Override
        protected void onSpawn(Location center) {
            spawnModel(center.clone().add(0, 8, 0));
            playAnimation("shimmer", 0.0, true);
            allaySound(center, 1.2f, 1.0f);
            chimeSound(center, 1.0f, 1.2f);
            moonDust(center.clone().add(0, 8, 0), 30, 5.0);
            paleDust(center.clone().add(0, 6, 0), 20, 4.0);
            endRods(center.clone().add(0, 10, 0), 15, 3.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            sweepCooldown++;

            // Curtain sweep every 40 ticks
            if (sweepCooldown >= 40) {
                sweepCooldown = 0;
                sweepRight = !sweepRight;
                sweepX = sweepRight ? -10.0 : 10.0;
            }

            // Move curtain across the area
            if (sweepRight) {
                sweepX += 0.5;
            } else {
                sweepX -= 0.5;
            }

            // Curtain is a vertical plane at sweepX, spanning Z -8 to +8 and Y 0 to 12
            Location curtainBase = c.clone().add(sweepX, 0, 0);

            // Curtain sweep damage — 3 hearts to all it passes through
            for (Player p : c.getWorld().getPlayers()) {
                if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                double dx = Math.abs(p.getLocation().getX() - curtainBase.getX());
                double dz = Math.abs(p.getLocation().getZ() - c.getZ());
                double dy = p.getLocation().getY() - c.getY();
                if (dx <= 1.5 && dz <= 8.0 && dy >= 0 && dy <= 12.0) {
                    p.damage(6.0); // 3 hearts
                    p.setNoDamageTicks(0);
                    moonDust(p.getLocation(), 4, 0.5);
                }
            }

            // Curtain visual — vertical shimmer
            if (tick % 2 == 0) {
                for (double z = -8; z <= 8; z += 2.0) {
                    for (double y = 0; y <= 12; y += 3.0) {
                        Location curtainPoint = curtainBase.clone().add(0, y, z);
                        if (Math.random() < 0.4) {
                            moonDust(curtainPoint, 1, 0.3);
                        }
                        if (Math.random() < 0.2) {
                            paleDust(curtainPoint, 1, 0.2);
                        }
                        if (Math.random() < 0.1) {
                            endRods(curtainPoint, 1, 0.1);
                        }
                    }
                }
            }

            // Standing in the swept path — 2 hearts/sec (the trail behind the curtain)
            if (tick % 20 == 0) {
                double trailStart = sweepRight ? c.getX() - 10 : c.getX() + 10;
                double trailEnd = curtainBase.getX();
                double minX = Math.min(trailStart, trailEnd);
                double maxX = Math.max(trailStart, trailEnd);

                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    double px = p.getLocation().getX();
                    double pz = Math.abs(p.getLocation().getZ() - c.getZ());
                    double py = p.getLocation().getY() - c.getY();
                    if (px >= minX && px <= maxX && pz <= 8.0 && py >= 0 && py <= 12.0) {
                        p.damage(4.0); // 2 hearts
                        p.setNoDamageTicks(0);
                    }
                }
            }

            // Aurora shimmer at top
            if (tick % 6 == 0) {
                for (int i = 0; i < 3; i++) {
                    double ox = (Math.random() - 0.5) * 16.0;
                    double oz = (Math.random() - 0.5) * 12.0;
                    Location shimmer = c.clone().add(ox, 10 + Math.random() * 2, oz);
                    lunarDust(shimmer, 1, 0.3);
                    endRods(shimmer, 1, 0.2);
                }
            }

            if (tick % 40 == 0) {
                allaySound(c.clone().add(0, 8, 0), 0.5f, 1.0f);
                chimeSound(c.clone().add(sweepX, 6, 0), 0.4f, 1.4f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new CelestialAurora(plugin); }
    }
}
