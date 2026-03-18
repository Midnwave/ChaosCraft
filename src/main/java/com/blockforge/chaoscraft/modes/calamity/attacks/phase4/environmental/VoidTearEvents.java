package com.blockforge.chaoscraft.modes.calamity.attacks.phase4.environmental;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 4D Environmental -- GROUP 1: VOID TEAR EVENTS
 * Attacks #1-10: Sky stream convergences, whips, lances, and the Zenith Collapse.
 * The three permanent sky streams (SPELL_WITCH, TOTEM, END_ROD) weaponize.
 *
 * Design notes:
 * - No status effects
 * - Dragon palette: magenta (180,0,200), cyan (0,200,255), crimson (200,0,50), purple (128,0,255)
 * - Materials: OBSIDIAN, CRYING_OBSIDIAN, AMETHYST_BLOCK, END_STONE, PURPUR_BLOCK, END_ROD
 * - Damage range: 6.0-14.0 HP
 */
public final class VoidTearEvents {

    private VoidTearEvents() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new StreamConvergenceStrike(plugin));
        registry.register(new StreamWhip(plugin));
        registry.register(new TripleLock(plugin));
        registry.register(new StreamSpiralDescent(plugin));
        registry.register(new GoldStreamFlare(plugin));
        registry.register(new WhiteStreamLance(plugin));
        registry.register(new StreamCrossPulse(plugin));
        registry.register(new PurpleRainSaturation(plugin));
        registry.register(new StreamCage(plugin));
        registry.register(new ZenithCollapse(plugin));
    }

    // =========================================================================
    // 1. STREAM CONVERGENCE STRIKE -- three sky streams converge into ground beam
    // =========================================================================
    public static class StreamConvergenceStrike extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> scorchHandles = new ArrayList<>();
        private Location targetZone;
        private boolean beamFired = false;
        private int beamTick = 0;

        public StreamConvergenceStrike(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("stream_convergence_strike", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(900);
            config.setTicksBetweenDamage(4);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double angle = Math.random() * 2 * Math.PI;
            double dist = Math.random() * 20;
            targetZone = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

            DisplayBuilder.playSound(targetZone, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // T-60 to T-30: streams slow, density doubles
            if (ticksAlive <= 60) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.dustParticles(targetZone.clone().add(0, 40, 0), 30, 15.0, 180, 0, 200, 1.2f);
                    DisplayBuilder.dustParticles(targetZone.clone().add(0, 50, 0), 20, 12.0, 128, 0, 255, 1.0f);
                }
                if (ticksAlive == 40) {
                    DisplayBuilder.playSound(targetZone, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 0.3f);
                }
                return;
            }

            // T-30 to T-0: streams bend, ground ring warning
            if (ticksAlive <= 100 && !beamFired) {
                double ringRadius = 5.0 * (1.0 - (ticksAlive - 60) / 40.0) + 1.0;
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.particleRing(targetZone.clone().add(0, 0.5, 0), ringRadius,
                            Particle.DUST, 20, new Particle.DustOptions(Color.fromRGB(255, 0, 255), 1.8f));
                    w.spawnParticle(Particle.END_ROD, targetZone.clone().add(0, 60, 0), 15, 2, 10, 2, 0.01);
                }
                return;
            }

            // Beam fires
            if (!beamFired) {
                beamFired = true;
                beamTick = 0;
                DisplayBuilder.playSound(targetZone, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.8f);
            }

            beamTick++;

            // Beam persists 40 ticks
            if (beamTick <= 40) {
                // Beam column particles
                if (beamTick % 2 == 0) {
                    for (int y = 0; y <= 80; y += 4) {
                        Location beamLoc = targetZone.clone().add(0, y, 0);
                        w.spawnParticle(Particle.END_ROD, beamLoc, 8, 2.0, 0.5, 2.0, 0.01);
                        DisplayBuilder.dustParticles(beamLoc, 4, 0.5, 218, 112, 214, 2.0f);
                    }
                    w.spawnParticle(Particle.EXPLOSION, targetZone.clone().add(0, 0.5, 0), 3, 1.0, 0.2, 1.0, 0);
                }

                // Enderman scream loop
                if (beamTick % 10 == 0) {
                    DisplayBuilder.playSound(targetZone, Sound.ENTITY_ENDERMAN_SCREAM, 0.4f, 2.0f);
                }

                // Damage players in beam column
                if (beamTick % 4 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dx = p.getLocation().getX() - targetZone.getX();
                        double dz = p.getLocation().getZ() - targetZone.getZ();
                        if (dx * dx + dz * dz <= 16.0) { // 4 block wide
                            p.damage(6.0);
                        }
                    }
                }
            }

            // Beam despawn
            if (beamTick == 41) {
                DisplayBuilder.playSound(targetZone, Sound.BLOCK_GLASS_BREAK, 0.7f, 0.5f);
                w.spawnParticle(Particle.FLASH, targetZone.clone().add(0, 1, 0), 8, 3.0, 0.5, 3.0, 0);

                // Scorch mark: permanent calcite displays
                for (int i = 0; i < 6; i++) {
                    double a = (2 * Math.PI * i) / 6;
                    Location loc = targetZone.clone().add(Math.cos(a) * 2, 0.02, Math.sin(a) * 2);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CALCITE);
                    h.scale(1.2f, 0.1f, 1.2f).glow(180, 0, 200).interpolation(5, 0);
                    scorchHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StreamConvergenceStrike(plugin); }
    }

    // =========================================================================
    // 2. STREAM WHIP -- single stream sweeps laterally across the arena
    // =========================================================================
    public static class StreamWhip extends EnvironmentalAttack {

        private double sweepAngle;
        private double sweepProgress = 0;
        private boolean sweepStarted = false;
        private int streamType; // 0=witch, 1=totem, 2=endrod

        public StreamWhip(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("stream_whip", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(10.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            sweepAngle = Math.random() * 2 * Math.PI;
            streamType = (int) (Math.random() * 3);

            // Buildup: brighten chosen stream
            Location skyLoc = center.clone().add(0, 40, 0);
            Particle streamParticle = switch (streamType) {
                case 0 -> Particle.WITCH;
                case 1 -> Particle.TOTEM_OF_UNDYING;
                default -> Particle.END_ROD;
            };
            w.spawnParticle(streamParticle, skyLoc, 40, 15, 5, 15, 0.01);
            DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST_FAR, 0.9f, 1.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks overhead glow
            if (ticksAlive <= 40) {
                if (ticksAlive % 5 == 0) {
                    Location skyPos = center.clone().add(Math.cos(sweepAngle) * -20, 5, Math.sin(sweepAngle) * -20);
                    DisplayBuilder.dustParticles(skyPos, 20, 3.0, 180, 0, 200, 2.5f);
                }
                return;
            }

            // Sweep: 5 ticks to cross 40 blocks
            if (!sweepStarted) {
                sweepStarted = true;
                sweepProgress = -20;
                DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST_FAR, 0.9f, 1.4f);
            }

            sweepProgress += 8; // 8 blocks per tick
            if (sweepProgress > 20) return;

            // Sweep line position
            double sweepX = center.getX() + Math.cos(sweepAngle) * sweepProgress;
            double sweepZ = center.getZ() + Math.sin(sweepAngle) * sweepProgress;
            Location sweepLoc = new Location(w, sweepX, center.getY() + 1, sweepZ);

            // Particles along sweep line (perpendicular to sweep direction)
            Particle.DustOptions dustOpt = switch (streamType) {
                case 0 -> new Particle.DustOptions(Color.fromRGB(255, 0, 180), 1.5f);
                case 1 -> new Particle.DustOptions(Color.fromRGB(255, 200, 0), 1.5f);
                default -> new Particle.DustOptions(Color.fromRGB(240, 240, 255), 1.5f);
            };

            double perpX = -Math.sin(sweepAngle);
            double perpZ = Math.cos(sweepAngle);
            for (int i = -10; i <= 10; i++) {
                Location linePos = sweepLoc.clone().add(perpX * i, 0, perpZ * i);
                for (int y = 0; y < 3; y++) {
                    w.spawnParticle(Particle.DUST, linePos.clone().add(0, y, 0), 3, 0.3, 0.3, 0.3, 0, dustOpt);
                }
                w.spawnParticle(Particle.SMOKE, linePos, 2, 0.2, 0.5, 0.2, 0.01);
            }

            // Damage players in sweep
            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                Location pl = p.getLocation();
                // Check distance to sweep line
                double dx = pl.getX() - sweepLoc.getX();
                double dz = pl.getZ() - sweepLoc.getZ();
                double lineProj = dx * perpX + dz * perpZ;
                double lineDist = Math.abs(dx * Math.cos(sweepAngle) + dz * Math.sin(sweepAngle));
                if (lineDist <= 2.0 && Math.abs(lineProj) <= 12.0 && pl.getY() <= center.getY() + 3) {
                    p.damage(10.0);
                    DisplayBuilder.playSound(pl, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.4f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StreamWhip(plugin); }
    }

    // =========================================================================
    // 3. TRIPLE LOCK -- all three streams form triangular grid, fire targeting pulses
    // =========================================================================
    public static class TripleLock extends EnvironmentalAttack {

        private boolean gridFormed = false;
        private int gridTick = 0;
        private final List<Location> pulseTargets = new ArrayList<>();

        public TripleLock(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("triple_lock", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(12.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(2400);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.6f);

            // Dust ring warning at Y+40
            DisplayBuilder.particleRing(center.clone().add(0, 40, 0), 15,
                    Particle.DUST, 30, new Particle.DustOptions(Color.fromRGB(255, 215, 0), 2.5f));
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 100 ticks -- rings contract overhead
            if (ticksAlive <= 100) {
                if (ticksAlive % 10 == 0) {
                    double shrink = 15.0 * (1.0 - ticksAlive / 100.0) + 2.0;
                    DisplayBuilder.particleRing(center.clone().add(0, 40, 0), shrink,
                            Particle.DUST, 20, new Particle.DustOptions(Color.fromRGB(255, 215, 0), 2.0f));
                    DisplayBuilder.particleRing(center.clone().add(0, 20, 0), shrink * 0.7,
                            Particle.DUST, 15, new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.5f));
                }
                if (ticksAlive == 95) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 2.0f);
                }
                return;
            }

            // Grid formed: overhead triangular ceiling
            if (!gridFormed) {
                gridFormed = true;
                gridTick = 0;

                // Triangular grid particles at Y+60
                for (int i = 0; i < 3; i++) {
                    double a = (2 * Math.PI * i) / 3;
                    Location corner = center.clone().add(Math.cos(a) * 15, 60, Math.sin(a) * 15);
                    w.spawnParticle(Particle.END_ROD, corner, 30, 1, 1, 1, 0.01);
                }
            }

            gridTick++;

            // Grid overhead particle display
            if (gridTick % 5 == 0) {
                for (int i = 0; i < 3; i++) {
                    double a1 = (2 * Math.PI * i) / 3;
                    double a2 = (2 * Math.PI * ((i + 1) % 3)) / 3;
                    Location p1 = center.clone().add(Math.cos(a1) * 15, 60, Math.sin(a1) * 15);
                    Location p2 = center.clone().add(Math.cos(a2) * 15, 60, Math.sin(a2) * 15);
                    DisplayBuilder.particleLine(p1, p2, Particle.END_ROD, 2, null);
                }
            }

            // Fire targeting pulse every 20 ticks (6 total in 120)
            if (gridTick % 20 == 0 && gridTick <= 120) {
                // Find nearest player
                Player nearest = null;
                double nearestDist = Double.MAX_VALUE;
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distanceSquared(center);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = p;
                    }
                }

                if (nearest != null) {
                    Location target = nearest.getLocation().clone();
                    pulseTargets.add(target);
                    DisplayBuilder.playSound(target, Sound.ENTITY_ARROW_SHOOT, 0.8f, 0.4f);

                    // Red targeting dot
                    DisplayBuilder.dustParticles(target.clone().add(0, 1, 0), 10, 0.5,
                            255, 50, 50, 2.0f);

                    // Schedule pulse descent (instant visual, damage check)
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        // Pulse column
                        for (int y = 0; y <= 60; y += 3) {
                            DisplayBuilder.dustParticles(target.clone().add(0, y, 0), 5, 0.3,
                                    255, 255, 255, 1.0f);
                        }
                        w.spawnParticle(Particle.FLASH, target, 3, 0.5, 0.5, 0.5, 0);
                        DisplayBuilder.playSound(target, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 1.6f);

                        // Damage at impact
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            double dist = p.getLocation().distanceSquared(target);
                            if (dist <= 4.0) {
                                p.damage(12.0); // 6 hearts direct
                            } else if (dist <= 16.0) {
                                p.damage(6.0); // 3 hearts splash
                            }
                        }
                    }, 7L); // ~0.35s for pulse to travel
                }
            }

            // Grid releases at 120 ticks
            if (gridTick >= 120) {
                // Final particles
                if (gridTick == 120) {
                    for (int i = 0; i < 3; i++) {
                        double a = (2 * Math.PI * i) / 3;
                        w.spawnParticle(Particle.END_ROD, center.clone().add(Math.cos(a) * 15, 60, Math.sin(a) * 15),
                                20, 2, 2, 2, 0.02);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TripleLock(plugin); }
    }

    // =========================================================================
    // 4. STREAM SPIRAL DESCENT -- SPELL_WITCH stream coils into arena descent
    // =========================================================================
    public static class StreamSpiralDescent extends EnvironmentalAttack {

        private boolean spiralActive = false;
        private int spiralTick = 0;
        private double spiralAngle = 0;

        public StreamSpiralDescent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("stream_spiral_descent", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(1400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Coiling overhead starts
            DisplayBuilder.dustParticles(center.clone().add(0, 50, 0), 30, 20.0, 200, 0, 255, 1.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 80 ticks -- spiral forms overhead
            if (ticksAlive <= 80) {
                double height = 50 - (ticksAlive * 0.2);
                double radius = 20 - (ticksAlive * 0.1);
                spiralAngle += 0.15;
                if (ticksAlive % 3 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double a = spiralAngle + (2 * Math.PI * i) / 8;
                        Location pos = center.clone().add(Math.cos(a) * radius, height, Math.sin(a) * radius);
                        DisplayBuilder.dustParticles(pos, 5, 0.5, 200, 0, 255, 1.6f);
                    }
                }
                if (ticksAlive == 60) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.7f, 1.5f);
                }
                return;
            }

            // Descent: spiral drops to Y+5
            if (ticksAlive <= 100) {
                double descentProgress = (ticksAlive - 80) / 20.0;
                double height = 35 - (descentProgress * 30);
                double radius = 15 - (descentProgress * 3);
                spiralAngle += 0.2;
                if (ticksAlive % 2 == 0) {
                    for (int i = 0; i < 10; i++) {
                        double a = spiralAngle + (2 * Math.PI * i) / 10;
                        Location pos = center.clone().add(Math.cos(a) * radius, height, Math.sin(a) * radius);
                        DisplayBuilder.dustParticles(pos, 4, 0.3, 200, 0, 255, 1.6f);
                    }
                }
                if (ticksAlive == 80) {
                    DisplayBuilder.playSound(center, Sound.AMBIENT_BASALT_DELTAS_ADDITIONS, 0.5f, 0.8f);
                }
                return;
            }

            // Holding at Y+5 for 80 ticks
            if (!spiralActive) {
                spiralActive = true;
                spiralTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 0.3f, 0.7f);
            }

            spiralTick++;
            if (spiralTick > 80) return;

            spiralAngle += 0.25;

            // Spiral ring at Y+5, 12 block diameter, damage on perimeter (10-12 block radius)
            if (spiralTick % 2 == 0) {
                for (int i = 0; i < 12; i++) {
                    double a = spiralAngle + (2 * Math.PI * i) / 12;
                    Location pos = center.clone().add(Math.cos(a) * 11, 5, Math.sin(a) * 11);
                    DisplayBuilder.dustParticles(pos, 5, 0.5, 200, 0, 255, 1.6f);
                    w.spawnParticle(Particle.REVERSE_PORTAL, pos.clone().add(0, -2, 0), 3, 0.3, 1.0, 0.3, 0.01);
                }
                // Lava hazard markers on perimeter
                DisplayBuilder.particleRing(center.clone().add(0, 5, 0), 11,
                        Particle.LAVA, 6, null);
            }

            // Damage players on perimeter ring (10-12 radius)
            if (spiralTick % 20 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = Math.sqrt(
                            Math.pow(p.getLocation().getX() - center.getX(), 2) +
                            Math.pow(p.getLocation().getZ() - center.getZ(), 2));
                    if (dist >= 10 && dist <= 12 && p.getLocation().getY() <= center.getY() + 8) {
                        p.damage(8.0); // 4 hearts per second
                        p.setVelocity(p.getVelocity().setY(0.4)); // Levitation-like
                    }
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
            Location center = getCenter();
            if (center != null) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 0.5f);
            }
        }

        @Override
        public AbstractAttack newInstance() { return new StreamSpiralDescent(plugin); }
    }

    // =========================================================================
    // 5. GOLD STREAM FLARE -- TOTEM stream fragments into falling gold clusters
    // =========================================================================
    public static class GoldStreamFlare extends EnvironmentalAttack {

        private final List<Location> clusterTargets = new ArrayList<>();
        private boolean clustersLaunched = false;
        private int clusterCount;
        private final List<BlockDisplayHandle> scorchHandles = new ArrayList<>();

        public GoldStreamFlare(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gold_stream_flare", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(1100);
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(2.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            clusterCount = 6 + (int) (Math.random() * 5); // 6-10

            DisplayBuilder.playSound(center, Sound.BLOCK_GILDED_BLACKSTONE_PLACE, 0.8f, 0.6f);

            // TOTEM stream flare overhead
            w.spawnParticle(Particle.TOTEM_OF_UNDYING, center.clone().add(0, 60, 0), 60, 15, 3, 15, 0.1);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 60 ticks -- fragments breaking off overhead
            if (ticksAlive <= 60) {
                if (ticksAlive % 8 == 0) {
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, center.clone().add(0, 55, 0), 20, 10, 5, 10, 0.05);
                }
                if (ticksAlive >= 45 && ticksAlive % 5 == 0) {
                    // Fragments separating
                    double a = Math.random() * 2 * Math.PI;
                    Location frag = center.clone().add(Math.cos(a) * 8, 55, Math.sin(a) * 8);
                    DisplayBuilder.playSound(frag, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.5f, 1.2f);
                    DisplayBuilder.dustParticles(frag, 10, 1.0, 255, 200, 0, 1.8f);
                }
                return;
            }

            // Launch clusters
            if (!clustersLaunched) {
                clustersLaunched = true;

                // Generate spread-checked positions
                for (int i = 0; i < clusterCount; i++) {
                    Location target;
                    int attempts = 0;
                    do {
                        double a = Math.random() * 2 * Math.PI;
                        double d = Math.random() * 25;
                        target = center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);
                        attempts++;
                    } while (isTooClose(target) && attempts < 20);
                    clusterTargets.add(target);
                }
            }

            // Clusters falling: 60 ticks to descend
            int fallTick = ticksAlive - 60;
            if (fallTick > 60) return;

            for (int i = 0; i < clusterTargets.size(); i++) {
                Location target = clusterTargets.get(i);
                double fallRate = 0.8 + (i * 0.07); // varied rates
                double currentY = 60 - (fallTick * fallRate);

                if (currentY <= 0) {
                    // Landing burst (only on first frame)
                    if (currentY > -fallRate) {
                        w.spawnParticle(Particle.LAVA, target, 15, 1.0, 0.5, 1.0, 0);
                        w.spawnParticle(Particle.FLASH, target.clone().add(0, 0.5, 0), 3, 0.3, 0.3, 0.3, 0);
                        w.spawnParticle(Particle.SMOKE, target.clone().add(0, 1, 0), 8, 0.5, 1.0, 0.5, 0.02);
                        DisplayBuilder.playSound(target, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.7f, 1.6f);

                        // Impact damage
                        triggerImpactDamage(target);

                        // Scorch mark
                        BlockDisplayHandle h = displayBuilder.spawnBlock(target.clone().add(0, 0.02, 0),
                                Material.POLISHED_BLACKSTONE);
                        h.scale(1.0f, 0.08f, 1.0f).glow(255, 200, 0).interpolation(3, 0);
                        scorchHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                    continue;
                }

                // Falling particles
                if (fallTick % 3 == 0) {
                    Location fallPos = target.clone().add(0, currentY, 0);
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, fallPos, 5, 0.3, 0.5, 0.3, 0.01);
                    DisplayBuilder.dustParticles(fallPos, 3, 0.3, 255, 200, 0, 1.8f);
                }
            }
        }

        private boolean isTooClose(Location target) {
            for (Location existing : clusterTargets) {
                if (target.distanceSquared(existing) < 9.0) return true; // 3 block min
            }
            return false;
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GoldStreamFlare(plugin); }
    }

    // =========================================================================
    // 6. WHITE STREAM LANCE -- END_ROD stream fires a tracking lance at stationary player
    // =========================================================================
    public static class WhiteStreamLance extends EnvironmentalAttack {

        private Location targetPos;
        private Player targetedPlayer;
        private boolean lanceFired = false;
        private int trackTick = 0;

        public WhiteStreamLance(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("white_stream_lance", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(14.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(999);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Find most stationary player (random fallback)
            targetedPlayer = null;
            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                if (targetedPlayer == null) {
                    targetedPlayer = p;
                } else if (Math.random() < 0.5) {
                    targetedPlayer = p;
                }
            }

            if (targetedPlayer != null) {
                targetPos = targetedPlayer.getLocation().clone();
            } else {
                targetPos = center.clone();
            }

            // Stream narrows: beam sound
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks -- stream brightens overhead
            if (ticksAlive <= 60) {
                if (ticksAlive % 5 == 0) {
                    w.spawnParticle(Particle.END_ROD, center.clone().add(0, 60, 0), 30, 10, 2, 10, 0.01);
                }
                // Ground target ring at T-10
                if (ticksAlive >= 50) {
                    double ringRadius = 1.0 + (ticksAlive - 50) * 0.2;
                    if (targetedPlayer != null && targetedPlayer.isOnline()) {
                        targetPos = targetedPlayer.getLocation().clone(); // tracking
                    }
                    DisplayBuilder.particleRing(targetPos.clone().add(0, 0.5, 0), ringRadius,
                            Particle.DUST, 12, new Particle.DustOptions(Color.fromRGB(255, 255, 255), 3.0f));
                }
                return;
            }

            // Tracking period: 20 ticks
            if (ticksAlive <= 80) {
                trackTick++;
                if (targetedPlayer != null && targetedPlayer.isOnline()) {
                    // Slow track toward player
                    Location pl = targetedPlayer.getLocation();
                    double dx = (pl.getX() - targetPos.getX()) * 0.15;
                    double dz = (pl.getZ() - targetPos.getZ()) * 0.15;
                    targetPos.add(dx, 0, dz);
                }
                // Target dot
                DisplayBuilder.dustParticles(targetPos.clone().add(0, 0.5, 0), 6, 0.3,
                        255, 255, 255, 3.0f);
                return;
            }

            // Fire lance
            if (!lanceFired) {
                lanceFired = true;
                DisplayBuilder.playSound(targetPos, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 2.0f);

                // Lance column
                for (int y = 0; y <= 80; y += 2) {
                    Location lanceLoc = targetPos.clone().add(0, y, 0);
                    w.spawnParticle(Particle.END_ROD, lanceLoc, 15, 0.3, 0.3, 0.3, 0.01);
                    w.spawnParticle(Particle.FLASH, lanceLoc, 2, 0.1, 0.1, 0.1, 0);
                }

                // Ground impact
                w.spawnParticle(Particle.BLOCK, targetPos, 80, 1.0, 0.5, 1.0, 0,
                        Material.CALCITE.createBlockData());
                DisplayBuilder.particleRing(targetPos, 2.0, Particle.END_ROD, 20, null);
                DisplayBuilder.playSound(targetPos, Sound.BLOCK_CALCITE_BREAK, 1.0f, 0.4f);

                // Damage: 14 HP direct, 6 HP splash
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distanceSquared(targetPos);
                    if (dist <= 1.0) {
                        p.damage(14.0);
                    } else if (dist <= 4.0) {
                        p.damage(6.0);
                    }
                }
            }

            // Retract lance at +10 ticks
            int postFire = ticksAlive - 80;
            if (postFire == 10) {
                DisplayBuilder.playSound(targetPos, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.8f);
                for (int y = 0; y <= 80; y += 5) {
                    DisplayBuilder.dustParticles(targetPos.clone().add(0, y, 0), 4, 0.5,
                            200, 200, 255, 1.0f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WhiteStreamLance(plugin); }
    }

    // =========================================================================
    // 7. STREAM CROSS-PULSE -- three streams intersect, fire ground shockwaves
    // =========================================================================
    public static class StreamCrossPulse extends EnvironmentalAttack {

        private boolean detonated = false;
        private int detonationTick = 0;
        private final double[] waveAngles = new double[3];
        private final double[] waveProgress = new double[3];

        public StreamCrossPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("stream_cross_pulse", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(1800);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Three shockwave directions with random offset
            double offset = Math.random() * Math.PI / 6; // 0-30 degrees
            for (int i = 0; i < 3; i++) {
                waveAngles[i] = (2 * Math.PI * i) / 3 + offset;
                waveProgress[i] = 0;
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 100 ticks -- asynchronous throbbing
            if (ticksAlive <= 100) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 55, 0), 15, 10.0, 180, 0, 200, 1.5f);
                }
                if (ticksAlive % 11 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 55, 0), 12, 8.0, 255, 200, 0, 1.3f);
                }
                if (ticksAlive % 13 == 0) {
                    w.spawnParticle(Particle.END_ROD, center.clone().add(0, 55, 0), 10, 6.0, 3.0, 6.0, 0.01);
                }
                return;
            }

            // Detonation
            if (!detonated) {
                detonated = true;
                detonationTick = 0;

                // Intersection burst at Y+60
                w.spawnParticle(Particle.END_ROD, center.clone().add(0, 60, 0), 80, 3, 3, 3, 0.05);
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, center.clone().add(0, 60, 0), 60, 3, 3, 3, 0.1);
                DisplayBuilder.dustParticles(center.clone().add(0, 60, 0), 40, 2.0, 180, 0, 200, 2.0f);

                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_HURT, 1.0f, 1.8f);
            }

            detonationTick++;

            // Three shockwaves travel outward at 4 blocks/tick
            if (detonationTick > 40) return; // waves done

            for (int i = 0; i < 3; i++) {
                waveProgress[i] += 4;
                if (waveProgress[i] > 25) continue;

                double angle = waveAngles[i];

                // Draw wave front (perpendicular wall of particles)
                double perpX = -Math.sin(angle);
                double perpZ = Math.cos(angle);
                Location waveCenter = center.clone().add(
                        Math.cos(angle) * waveProgress[i], 0,
                        Math.sin(angle) * waveProgress[i]);

                for (int seg = -5; seg <= 5; seg++) {
                    Location segLoc = waveCenter.clone().add(perpX * seg, 0, perpZ * seg);
                    for (int y = 0; y <= 2; y++) {
                        Particle.DustOptions dustOpt = switch (i) {
                            case 0 -> new Particle.DustOptions(Color.fromRGB(180, 0, 200), 1.5f);
                            case 1 -> new Particle.DustOptions(Color.fromRGB(255, 200, 0), 1.5f);
                            default -> new Particle.DustOptions(Color.fromRGB(240, 240, 255), 1.5f);
                        };
                        w.spawnParticle(Particle.DUST, segLoc.clone().add(0, y, 0), 2, 0.3, 0.2, 0.3, 0, dustOpt);
                    }
                }

                // Sound at leading edge
                if (detonationTick % 10 == 0) {
                    DisplayBuilder.playSound(waveCenter, Sound.BLOCK_STONE_FALL, 0.6f, 0.8f);
                }

                // Damage players in wave
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pl = p.getLocation();
                    double dx = pl.getX() - waveCenter.getX();
                    double dz = pl.getZ() - waveCenter.getZ();
                    double dist = Math.abs(dx * Math.cos(angle) + dz * Math.sin(angle));
                    double perpDist = Math.abs(dx * perpX + dz * perpZ);
                    if (dist <= 2.0 && perpDist <= 6.0 && pl.getY() <= center.getY() + 2.5) {
                        p.damage(8.0);
                        // Knockback in wave direction
                        p.setVelocity(p.getVelocity().add(new org.bukkit.util.Vector(
                                Math.cos(angle) * 0.6, 0.2, Math.sin(angle) * 0.6)));
                        DisplayBuilder.playSound(pl, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.6f, 0.6f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StreamCrossPulse(plugin); }
    }

    // =========================================================================
    // 8. PURPLE RAIN SATURATION -- visibility reduction + hidden charged zone
    // =========================================================================
    public static class PurpleRainSaturation extends EnvironmentalAttack {

        private Location chargedZone;
        private boolean rainActive = false;

        public PurpleRainSaturation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("purple_rain_saturation", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(1200);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Choose charged zone
            double a = Math.random() * 2 * Math.PI;
            double d = Math.random() * 15;
            chargedZone = center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks -- scattered dripping tears
            if (ticksAlive <= 60) {
                if (ticksAlive % 3 == 0) {
                    for (int i = 0; i < 5; i++) {
                        double rx = (Math.random() - 0.5) * 40;
                        double rz = (Math.random() - 0.5) * 40;
                        w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR,
                                center.clone().add(rx, 30 + Math.random() * 20, rz), 1, 0, 0, 0, 0);
                    }
                }
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 50, 0), 30, 20.0, 180, 0, 200, 1.5f);
                }
                return;
            }

            // Rain active: 100 ticks
            if (!rainActive) {
                rainActive = true;
                DisplayBuilder.playSound(center, Sound.WEATHER_RAIN, 0.8f, 0.7f);
            }

            int rainTick = ticksAlive - 60;
            if (rainTick > 100) return;

            // Dense purple rain across arena
            if (rainTick % 2 == 0) {
                for (int i = 0; i < 25; i++) {
                    double rx = (Math.random() - 0.5) * 40;
                    double rz = (Math.random() - 0.5) * 40;
                    double ry = 40 + Math.random() * 40;
                    DisplayBuilder.dustParticles(center.clone().add(rx, ry, rz), 2, 1.0, 180, 0, 200, 1.2f);
                }
            }

            // Charged zone: darker purple + reverse portal
            if (rainTick % 3 == 0) {
                DisplayBuilder.dustParticles(chargedZone.clone().add(0, 5, 0), 15, 10.0, 140, 0, 180, 2.0f);
                w.spawnParticle(Particle.REVERSE_PORTAL, chargedZone.clone().add(0, 0.5, 0), 5, 5, 0.5, 5, 0.01);
            }

            // Ambient sound in charged zone
            if (rainTick % 60 == 0) {
                DisplayBuilder.playSound(chargedZone, Sound.ENTITY_ENDERMAN_AMBIENT, 0.4f, 0.5f);
            }

            // Damage in charged zone: 4 HP per second
            if (rainTick % 20 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(chargedZone) <= 100.0) { // 10 block radius
                        p.damage(4.0);
                        DisplayBuilder.playSound(p.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 0.3f, 1.5f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PurpleRainSaturation(plugin); }
    }

    // =========================================================================
    // 9. STREAM CAGE -- all streams descend as walls forming an enclosure
    // =========================================================================
    public static class StreamCage extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> cornerHandles = new ArrayList<>();
        private boolean cageFormed = false;
        private int cageTick = 0;

        public StreamCage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("stream_cage", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(8.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(2400);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 80 ticks -- streams descending as curtains
            if (ticksAlive <= 80) {
                double wallY = 25 * (1.0 - ticksAlive / 80.0);
                if (ticksAlive % 5 == 0) {
                    // Four wall particle curtains at 20-block offsets
                    for (int side = 0; side < 4; side++) {
                        double angle = (Math.PI / 2) * side;
                        for (int seg = -10; seg <= 10; seg++) {
                            double perpX = -Math.sin(angle);
                            double perpZ = Math.cos(angle);
                            Location wallPos = center.clone().add(
                                    Math.cos(angle) * 20 + perpX * seg, wallY, Math.sin(angle) * 20 + perpZ * seg);
                            DisplayBuilder.dustParticles(wallPos, 2, 0.3, 180, 0, 200, 1.0f);
                        }
                    }
                }
                return;
            }

            // Cage forms
            if (!cageFormed) {
                cageFormed = true;
                cageTick = 0;
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.8f, 0.5f);

                // Corner gold block displays
                for (int i = 0; i < 4; i++) {
                    double a = (Math.PI / 2) * i + Math.PI / 4;
                    Location cornerLoc = center.clone().add(Math.cos(a) * 20, 0, Math.sin(a) * 20);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(cornerLoc, Material.POLISHED_BLACKSTONE);
                    h.scale(1.5f, 25.0f, 1.5f).glow(180, 0, 200).interpolation(5, 0);
                    cornerHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            cageTick++;

            // Cage active for 90 ticks
            if (cageTick > 90) {
                // Cage lifts
                if (cageTick == 91) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.6f);
                }
                return;
            }

            // Particle walls
            if (cageTick % 4 == 0) {
                for (int side = 0; side < 4; side++) {
                    double angle = (Math.PI / 2) * side;
                    for (int seg = -8; seg <= 8; seg += 2) {
                        double perpX = -Math.sin(angle);
                        double perpZ = Math.cos(angle);
                        Location wallPos = center.clone().add(
                                Math.cos(angle) * 20 + perpX * seg, 12, Math.sin(angle) * 20 + perpZ * seg);
                        w.spawnParticle(Particle.END_ROD, wallPos, 3, 0.5, 10, 0.5, 0.01);
                        DisplayBuilder.dustParticles(wallPos, 2, 0.5, 128, 0, 255, 1.0f);
                    }
                }
                // Ceiling ash
                DisplayBuilder.dustParticles(center.clone().add(0, 25, 0), 10, 18.0, 255, 255, 200, 1.0f);
            }

            // Corner glow pulse
            if (cageTick % 15 == 0) {
                for (BlockDisplayHandle h : cornerHandles) {
                    boolean on = (cageTick / 15) % 2 == 0;
                    if (on) {
                        h.glow(255, 200, 0);
                    } else {
                        h.glow(128, 100, 0);
                    }
                }
            }

            // Damage on wall crossing
            if (cageTick % 5 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pl = p.getLocation();
                    double dx = Math.abs(pl.getX() - center.getX());
                    double dz = Math.abs(pl.getZ() - center.getZ());
                    // Check if near any wall (20 blocks from center in cardinal)
                    boolean nearWall = (Math.abs(dx - 20) <= 1.5 && dz <= 20) ||
                                       (Math.abs(dz - 20) <= 1.5 && dx <= 20);
                    if (nearWall) {
                        p.damage(8.0);
                        DisplayBuilder.playSound(pl, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.8f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StreamCage(plugin); }
    }

    // =========================================================================
    // 10. ZENITH COLLAPSE -- once only at 25% HP, all streams detonate
    // =========================================================================
    public static class ZenithCollapse extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> hexCrystals = new ArrayList<>();
        private boolean convergenceStarted = false;
        private boolean ringFired = false;
        private int ringTick = 0;

        public ZenithCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("zenith_collapse", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(12.0);
            config.setDamageRadius(25.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(99999);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            // All streams freeze for 10 ticks -- silence
            // (audio silence handled conceptually)
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // First 10 ticks: silence -- streams frozen
            if (ticksAlive <= 10) return;

            // Convergence: 100 ticks -- streams retreat to zenith
            if (ticksAlive <= 110) {
                if (!convergenceStarted) {
                    convergenceStarted = true;
                    DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.3f);
                }

                double progress = (ticksAlive - 10) / 100.0;
                int flashCount = (int) (5 + progress * 195);

                if (ticksAlive % 3 == 0) {
                    // Increasing flash particles at zenith
                    w.spawnParticle(Particle.FLASH, center.clone().add(0, 80, 0),
                            Math.min(flashCount / 10, 20), 2, 2, 2, 0);
                    w.spawnParticle(Particle.END_ROD, center.clone().add(0, 80, 0),
                            Math.min(flashCount / 5, 40), 3, 3, 3, 0.02);
                    DisplayBuilder.dustParticles(center.clone().add(0, 80, 0),
                            Math.min(flashCount / 8, 25), 4.0, 180, 0, 200, 2.0f);
                }
                return;
            }

            // Detonation
            if (!ringFired) {
                ringFired = true;
                ringTick = 0;

                // Massive zenith detonation
                w.spawnParticle(Particle.END_ROD, center.clone().add(0, 80, 0), 150, 5, 5, 5, 0.1);
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, center.clone().add(0, 80, 0), 120, 5, 5, 5, 0.15);
                DisplayBuilder.dustParticles(center.clone().add(0, 80, 0), 80, 5.0, 180, 0, 200, 2.5f);

                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 1.5f);

                // Spawn hexagonal crystals at Y+5
                for (int i = 0; i < 6; i++) {
                    double a = (2 * Math.PI * i) / 6;
                    Location crystalLoc = center.clone().add(Math.cos(a) * 8, 5, Math.sin(a) * 8);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(crystalLoc, Material.AMETHYST_BLOCK);
                    h.scale(1.5f, 1.5f, 1.5f).glow(128, 0, 255).interpolation(3, 0);
                    hexCrystals.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            ringTick++;

            // Descending ring: 90 ticks to reach ground
            if (ringTick <= 90) {
                double ringY = 80 - (ringTick * (80.0 / 90.0));
                double ringRadius = ringTick * (25.0 / 90.0);

                if (ringTick % 2 == 0) {
                    DisplayBuilder.particleRing(center.clone().add(0, ringY, 0), ringRadius,
                            Particle.END_ROD, 30, null);
                    DisplayBuilder.particleRing(center.clone().add(0, ringY, 0), ringRadius,
                            Particle.DUST, 20,
                            new Particle.DustOptions(Color.fromRGB(255, 255, 255), 3.0f));
                }

                // Dragon growl during descent
                if (ringTick % 20 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.4f);
                }

                // Ring hits ground
                if (ringTick == 90) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.6f);
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, center.clone().add(0, 1, 0), 8, 12, 0.5, 12, 0);

                    // Damage all players: 12 HP grounded, 4 HP airborne
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().getY() > center.getY() + 1.5) {
                            p.damage(4.0); // Airborne reduced
                        } else {
                            p.damage(12.0); // Full ground hit
                        }
                    }
                }
            }

            // Hexagonal crystal rotation
            if (ringTick % 3 == 0 && !hexCrystals.isEmpty()) {
                float rotAngle = (ringTick * 30.0f) * (float) (Math.PI / 180.0);
                for (BlockDisplayHandle h : hexCrystals) {
                    h.rotate(rotAngle, 0, 1, 0);
                }
            }

            // Island-wide earthquake shake (30 ticks post-detonation)
            if (ringTick > 90 && ringTick <= 120) {
                // Visual shake feedback
                if (ringTick % 4 == 0) {
                    double shakeY = (ringTick % 8 < 4) ? 0.3 : -0.3;
                    DisplayBuilder.dustParticles(center.clone().add(0, shakeY, 0), 10, 20.0, 128, 0, 255, 0.8f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ZenithCollapse(plugin); }
    }
}
