package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.environmental;

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
 * Phase 4E Environmental -- APOCALYPSE A
 * Attacks #131-140: Echo-based attacks from previous bosses and Phase 4 transitions.
 * Voidmaw column detonation, inheritance trail merge, Void Emperor blade rings,
 * inheritance crescendo, memory bleed, echo convergence, pale remembrance,
 * crimson tide, monolith convergence.
 *
 * Design notes:
 * - Calamitas palette: crimson (200,0,50), orange (255,100,0), soul blue (0,150,255), purple (128,0,255)
 * - Damage range: 10.0-20.0 HP (escalating)
 * - Boss echo mechanics -- prior bosses' signatures haunt the arena
 */
public final class ApocalypseA {

    private ApocalypseA() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidmawColumnDetonation(plugin));
        registry.register(new DoGLatticeSurge(plugin));
        registry.register(new VoidEmperorBladeRingEchoI(plugin));
        registry.register(new VoidEmperorBladeRingEchoII(plugin));
        registry.register(new InheritanceCrescendo(plugin));
        registry.register(new MemoryBleed(plugin));
        registry.register(new EchoConvergencePoint(plugin));
        registry.register(new PaleRemembrance(plugin));
        registry.register(new CrimsonTide(plugin));
        registry.register(new MonolithConvergence(plugin));
    }

    // =========================================================================
    // 131. VOIDMAW COLUMN DETONATION -- void column erupts at center
    // =========================================================================
    public static class VoidmawColumnDetonation extends EnvironmentalAttack {

        private boolean detonated = false;

        public VoidmawColumnDetonation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("voidmaw_column_detonation", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(16.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(1800);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {}

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks -- void tear circle at center
            if (ticksAlive <= 60) {
                double radius = 3 + ticksAlive * 0.03;
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), radius,
                            Particle.DUST, 15,
                            new Particle.DustOptions(Color.fromRGB(20, 0, 40), 2.0f));
                    w.spawnParticle(Particle.PORTAL, center.clone().add(0, 1, 0),
                            10, 2.0, 1.0, 2.0, 0.02);
                }
                if (ticksAlive == 50) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_AMBIENT, 1.0f, 0.3f);
                }
                return;
            }

            // Column eruption
            if (!detonated) {
                detonated = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.4f);
            }

            int detTick = ticksAlive - 60;

            // Void column: Y+0 to Y+20, 5 blocks wide
            if (detTick <= 40) {
                double columnHeight = Math.min(detTick * 0.5, 20);
                if (detTick % 2 == 0) {
                    for (int y = 0; y <= (int) columnHeight; y += 2) {
                        DisplayBuilder.dustParticles(center.clone().add(0, y, 0),
                                20, 2.5, 10, 0, 20, 3.0f);
                        w.spawnParticle(Particle.PORTAL, center.clone().add(0, y, 0),
                                8, 1.5, 0.5, 1.5, 0.03);
                    }
                }
            }

            // Detonation ring at 12 blocks
            if (detTick == 30) {
                DisplayBuilder.particleRing(center.clone().add(0, 1, 0), 12.0,
                        Particle.DUST, 60,
                        new Particle.DustOptions(Color.fromRGB(40, 0, 80), 3.0f));
                w.spawnParticle(Particle.DRAGON_BREATH, center.clone().add(0, 1, 0),
                        40, 6.0, 0.5, 6.0, 0.03);

                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 25.0) {
                        p.damage(16.0);
                    }
                }
            }

            // Floor scar aftermath
            if (detTick > 40 && detTick % 8 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 0.3, 0),
                        6, 3.0, 10, 0, 20, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidmawColumnDetonation(plugin); }
    }

    // =========================================================================
    // 132. DOG LATTICE SURGE -- DoG-echo crystal lattice in NE quadrant
    // =========================================================================
    public static class DoGLatticeSurge extends EnvironmentalAttack {

        private final List<Location> gridLines = new ArrayList<>();
        private boolean surgeActive = false;

        public DoGLatticeSurge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_lattice_surge", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(14.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(1400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            // 4x4 lattice in NE quadrant
            for (int x = 1; x <= 7; x += 2) {
                for (int z = -7; z <= -1; z += 2) {
                    gridLines.add(center.clone().add(x, 0, z));
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks -- lattice warms up
            if (ticksAlive <= 60) {
                if (ticksAlive % 4 == 0) {
                    for (Location node : gridLines) {
                        DisplayBuilder.dustParticles(node.clone().add(0, 0.3, 0),
                                4, 0.3, 0, 200, 255, 1.2f);
                    }
                }
                if (ticksAlive == 55) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 0.5f);
                }
                return;
            }

            // Surge
            if (!surgeActive) {
                surgeActive = true;
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.2f, 0.4f);
            }

            int surgeTick = ticksAlive - 60;

            if (surgeTick <= 60) {
                double height = Math.min(surgeTick * 0.25, 8);
                if (surgeTick % 2 == 0) {
                    for (Location node : gridLines) {
                        DisplayBuilder.dustParticles(node.clone().add(0, height / 2, 0),
                                12, 0.3, 0, 200, 255, 2.0f);
                        w.spawnParticle(Particle.ENCHANTED_HIT, node.clone().add(0, height, 0),
                                5, 0.2, 0.3, 0.2, 0.03);
                    }
                }

                if (surgeTick % 10 == 0) {
                    for (Location node : gridLines) {
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            double dx = p.getLocation().getX() - node.getX();
                            double dz = p.getLocation().getZ() - node.getZ();
                            if (dx * dx + dz * dz <= 2.25) {
                                p.damage(14.0);
                            }
                        }
                    }
                }
            }

            // Floor scars
            if (surgeTick > 60 && surgeTick % 6 == 0) {
                for (Location node : gridLines) {
                    DisplayBuilder.dustParticles(node.clone().add(0, 0.2, 0),
                            3, 0.3, 0, 150, 200, 0.8f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DoGLatticeSurge(plugin); }
    }

    // =========================================================================
    // 133. VOID EMPEROR BLADE RING (ECHO I) -- psychological false threat
    // =========================================================================
    public static class VoidEmperorBladeRingEchoI extends EnvironmentalAttack {

        private Player targetPlayer;
        private double ringAngle = 0;

        public VoidEmperorBladeRingEchoI(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_emperor_blade_echo_i", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(2400);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            List<Player> players = new ArrayList<>(w.getPlayers());
            if (!players.isEmpty()) {
                targetPlayer = players.get((int) (Math.random() * players.size()));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            if (targetPlayer == null || !targetPlayer.isOnline()) return;
            World w = targetPlayer.getWorld();
            if (w == null) return;

            Location playerLoc = targetPlayer.getLocation();
            ringAngle += Math.toRadians(6); // one revolution per second

            // Buildup: 40 ticks -- faint ring forms
            if (ticksAlive <= 40) {
                double pulse = ticksAlive % 20 < 10 ? 1.0 : 1.5;
                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.particleRing(playerLoc.clone().add(0, 0.5, 0), 5.0,
                            Particle.DUST, (int) (12 * pulse),
                            new Particle.DustOptions(Color.fromRGB(80, 0, 120), 1.5f));
                }
                if (ticksAlive == 5) {
                    DisplayBuilder.playSound(playerLoc, Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 0.7f);
                }
                return;
            }

            // Ring persists for 100 ticks -- ZERO damage
            int ringTick = ticksAlive - 40;

            if (ringTick <= 100) {
                if (ringTick % 2 == 0) {
                    // Rotating ring around player
                    for (int i = 0; i < 12; i++) {
                        double a = ringAngle + (2 * Math.PI * i) / 12;
                        Location ringPos = playerLoc.clone().add(Math.cos(a) * 5, 0.5, Math.sin(a) * 5);
                        w.spawnParticle(Particle.DUST, ringPos, 1, 0, 0, 0, 0,
                                new Particle.DustOptions(Color.fromRGB(80, 0, 120), 1.5f));
                    }

                    // Pulse peaks
                    if (ringTick % 20 == 0) {
                        DisplayBuilder.particleRing(playerLoc.clone().add(0, 0.5, 0), 5.0,
                                Particle.DUST, 8,
                                new Particle.DustOptions(Color.fromRGB(140, 0, 200), 1.5f));
                    }
                }
            }

            // Visual detonation at end -- still zero damage
            if (ringTick == 100) {
                DisplayBuilder.playSound(playerLoc, Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 0.8f);
                for (int i = 0; i < 60; i++) {
                    double a = (2 * Math.PI * i) / 60;
                    double d = 0.1 * i;
                    Location burstPos = playerLoc.clone().add(Math.cos(a) * d, 0.5, Math.sin(a) * d);
                    w.spawnParticle(Particle.DUST, burstPos, 1, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(140, 0, 200), 2.0f));
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidEmperorBladeRingEchoI(plugin); }
    }

    // =========================================================================
    // 134. VOID EMPEROR BLADE RING (ECHO II -- WANDERING)
    // =========================================================================
    public static class VoidEmperorBladeRingEchoII extends EnvironmentalAttack {

        private Location ringPos;
        private Player lockedTarget;
        private double ringAngle = 0;

        public VoidEmperorBladeRingEchoII(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_emperor_blade_echo_ii", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(3000);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            ringPos = center.clone();
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || ringPos == null) return;
            World w = center.getWorld();
            if (w == null) return;

            ringAngle += Math.toRadians(6);

            // Buildup: 60 ticks -- ring wanders toward nearest player
            if (ticksAlive <= 60) {
                Player nearest = null;
                double nearestDist = Double.MAX_VALUE;
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distanceSquared(ringPos);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = p;
                    }
                }

                if (nearest != null) {
                    double dx = nearest.getLocation().getX() - ringPos.getX();
                    double dz = nearest.getLocation().getZ() - ringPos.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > 0.1) {
                        ringPos.add(dx / dist * 0.3, 0, dz / dist * 0.3);
                    }
                    if (dist < 2.0) {
                        lockedTarget = nearest;
                    }
                }

                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.particleRing(ringPos.clone().add(0, 0.5, 0), 4.0,
                            Particle.DUST, 10,
                            new Particle.DustOptions(Color.fromRGB(80, 0, 120), 1.5f));
                }
                if (ticksAlive == 5) {
                    DisplayBuilder.playSound(ringPos, Sound.BLOCK_CONDUIT_AMBIENT, 0.8f, 0.6f);
                }
                return;
            }

            // Lock-on phase: 100 ticks -- ring orbits locked target
            int lockTick = ticksAlive - 60;
            Location trackPos = lockedTarget != null ? lockedTarget.getLocation() : ringPos;

            if (lockTick <= 100) {
                if (lockTick % 2 == 0) {
                    for (int i = 0; i < 14; i++) {
                        double a = ringAngle + (2 * Math.PI * i) / 14;
                        Location rPos = trackPos.clone().add(Math.cos(a) * 4, 0.5, Math.sin(a) * 4);
                        w.spawnParticle(Particle.DUST, rPos, 1, 0, 0, 0, 0,
                                new Particle.DustOptions(Color.fromRGB(100, 0, 150), 1.5f));
                    }
                }
            }

            // Implosion visual at end -- zero damage
            if (lockTick == 100) {
                DisplayBuilder.playSound(trackPos, Sound.BLOCK_CONDUIT_DEACTIVATE, 1.0f, 0.8f);
                for (int i = 0; i < 40; i++) {
                    double a = (2 * Math.PI * i) / 40;
                    double startDist = 4.0;
                    Location implodePos = trackPos.clone().add(
                            Math.cos(a) * startDist * (1.0 - i / 40.0), 0.5,
                            Math.sin(a) * startDist * (1.0 - i / 40.0));
                    w.spawnParticle(Particle.DUST, implodePos, 1, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(160, 0, 220), 2.0f));
                }
                // Final burst
                w.spawnParticle(Particle.DUST, trackPos.clone().add(0, 0.5, 0), 20, 0.3, 0.3, 0.3, 0,
                        new Particle.DustOptions(Color.fromRGB(200, 0, 255), 2.5f));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidEmperorBladeRingEchoII(plugin); }
    }

    // =========================================================================
    // 135. INHERITANCE CRESCENDO -- all four boss echoes simultaneously
    // =========================================================================
    public static class InheritanceCrescendo extends EnvironmentalAttack {

        private boolean crescendoFired = false;

        public InheritanceCrescendo(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("inheritance_crescendo", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(20.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(360);
            config.setCooldownTicks(99999);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {}

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 120 ticks -- all four echo indicators appear simultaneously
            if (ticksAlive <= 120) {
                if (ticksAlive % 4 == 0) {
                    // Voidmaw tear circle at center
                    DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0),
                            12, 3.0, 20, 0, 40, 1.5f);
                    // DoG lattice grid in NE
                    for (int i = 0; i < 4; i++) {
                        double a = (2 * Math.PI * i) / 4;
                        DisplayBuilder.dustParticles(center.clone().add(
                                5 + Math.cos(a) * 3, 0.5, -5 + Math.sin(a) * 3),
                                4, 0.3, 0, 200, 255, 1.2f);
                    }
                    // Dweller twin embers from N and S
                    DisplayBuilder.dustParticles(center.clone().add(0, 0.5, -8),
                            6, 0.5, 200, 50, 0, 1.5f);
                    DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 8),
                            6, 0.5, 200, 50, 0, 1.5f);
                    // Emperor blade ring drifting
                    DisplayBuilder.particleRing(center.clone().add(3, 0.5, 3), 4.0,
                            Particle.DUST, 8,
                            new Particle.DustOptions(Color.fromRGB(80, 0, 120), 1.2f));
                }
                if (ticksAlive == 100) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.4f);
                }
                return;
            }

            // All four fire simultaneously
            if (!crescendoFired) {
                crescendoFired = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.3f);
            }

            int fireTick = ticksAlive - 120;

            // 12 seconds of arena-wide chaos at 150% emission
            if (fireTick <= 240) {
                if (fireTick % 2 == 0) {
                    // Voidmaw column
                    for (int y = 0; y <= 20; y += 3) {
                        DisplayBuilder.dustParticles(center.clone().add(0, y, 0),
                                15, 2.5, 10, 0, 20, 2.5f);
                    }
                    // DoG lattice spikes
                    for (int i = 0; i < 8; i++) {
                        double a = (2 * Math.PI * i) / 8;
                        DisplayBuilder.dustParticles(center.clone().add(
                                5 + Math.cos(a) * 2, fireTick * 0.03, -5 + Math.sin(a) * 2),
                                6, 0.3, 0, 200, 255, 1.5f);
                    }
                    // Dweller ember trails
                    double crawl = fireTick * 0.08;
                    DisplayBuilder.dustParticles(center.clone().add(0, 0.5, -8 + crawl),
                            10, 0.5, 200, 50, 0, 2.0f);
                    DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 8 - crawl),
                            10, 0.5, 200, 50, 0, 2.0f);
                    // Emperor blade ring (zero damage)
                    DisplayBuilder.particleRing(center.clone().add(3, 0.5, 3), 5.0,
                            Particle.DUST, 12,
                            new Particle.DustOptions(Color.fromRGB(80, 0, 120), 1.5f));
                }

                // Damage from real echoes
                if (fireTick % 20 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(center) <= 25.0) {
                            p.damage(20.0);
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new InheritanceCrescendo(plugin); }
    }

    // =========================================================================
    // 136. MEMORY BLEED -- previous echo scars reactivate as hazards
    // =========================================================================
    public static class MemoryBleed extends EnvironmentalAttack {

        private final List<Location> scarPositions = new ArrayList<>();
        private boolean bleedActive = false;

        public MemoryBleed(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("memory_bleed", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(1400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            for (int i = 0; i < 4; i++) {
                double a = Math.random() * 2 * Math.PI;
                double d = 3 + Math.random() * 6;
                scarPositions.add(center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 40 ticks -- scars triple emission
            if (ticksAlive <= 40) {
                if (ticksAlive % 3 == 0) {
                    for (Location scar : scarPositions) {
                        int type = scarPositions.indexOf(scar) % 4;
                        switch (type) {
                            case 0: DisplayBuilder.dustParticles(scar.clone().add(0, 0.3, 0), 10, 0.8, 60, 0, 80, 1.5f); break;
                            case 1: DisplayBuilder.dustParticles(scar.clone().add(0, 0.3, 0), 10, 0.8, 0, 240, 255, 1.5f); break;
                            case 2: DisplayBuilder.dustParticles(scar.clone().add(0, 0.3, 0), 10, 0.8, 220, 60, 0, 1.5f); break;
                            default: DisplayBuilder.dustParticles(scar.clone().add(0, 0.3, 0), 6, 0.5, 80, 0, 120, 1.0f); break;
                        }
                    }
                }
                if (ticksAlive == 30) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_FLOP, 0.6f, 0.5f);
                }
                return;
            }

            // Active bleed: 160 ticks
            if (!bleedActive) {
                bleedActive = true;
            }

            int bleedTick = ticksAlive - 40;

            if (bleedTick <= 160) {
                if (bleedTick % 3 == 0) {
                    for (Location scar : scarPositions) {
                        DisplayBuilder.dustParticles(scar.clone().add(0, 0.3, 0),
                                20, 1.0, 180, 0, 80, 1.5f);
                    }
                }

                // Contact damage
                if (bleedTick % 20 == 0) {
                    for (Location scar : scarPositions) {
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            if (p.getLocation().distanceSquared(scar) <= 4.0) {
                                p.damage(10.0);
                                DisplayBuilder.playSound(p.getLocation(), Sound.ENTITY_CREEPER_HURT, 0.4f, 0.6f);
                            }
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MemoryBleed(plugin); }
    }

    // =========================================================================
    // 137. ECHO CONVERGENCE POINT -- active scars detonate at shared center
    // =========================================================================
    public static class EchoConvergencePoint extends EnvironmentalAttack {

        private final List<Location> scarPositions = new ArrayList<>();
        private Location convergencePoint;
        private boolean detonated = false;

        public EchoConvergencePoint(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("echo_convergence_point", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(20.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(3200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            convergencePoint = center.clone().add((Math.random() - 0.5) * 6, 0, (Math.random() - 0.5) * 6);
            for (int i = 0; i < 3; i++) {
                double a = Math.random() * 2 * Math.PI;
                double d = 4 + Math.random() * 5;
                scarPositions.add(convergencePoint.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            if (convergencePoint == null) return;
            World w = convergencePoint.getWorld();
            if (w == null) return;

            // Buildup: 80 ticks -- tether lines from scars to convergence point
            if (ticksAlive <= 80) {
                if (ticksAlive % 3 == 0) {
                    for (Location scar : scarPositions) {
                        // Draw line from scar to convergence
                        int steps = 10;
                        for (int s = 0; s < steps; s++) {
                            double t = s / (double) steps;
                            Location linePos = scar.clone().add(
                                    (convergencePoint.getX() - scar.getX()) * t, 0.5,
                                    (convergencePoint.getZ() - scar.getZ()) * t);
                            w.spawnParticle(Particle.DUST, linePos, 1, 0, 0, 0, 0,
                                    new Particle.DustOptions(Color.fromRGB(100, 0, 100), 1.2f));
                        }
                        // Scar upward columns
                        DisplayBuilder.dustParticles(scar.clone().add(0, 3, 0),
                                4, 0.3, 60, 0, 80, 1.0f);
                    }
                }
                if (ticksAlive == 70) {
                    DisplayBuilder.playSound(convergencePoint, Sound.BLOCK_BEACON_AMBIENT, 1.2f, 0.4f);
                }
                return;
            }

            // Detonation
            if (!detonated) {
                detonated = true;
                DisplayBuilder.playSound(convergencePoint, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);

                // Mixed-color burst at convergence
                DisplayBuilder.dustParticles(convergencePoint.clone().add(0, 1, 0), 50, 3.0, 80, 0, 100, 3.0f);
                DisplayBuilder.dustParticles(convergencePoint.clone().add(0, 1, 0), 30, 3.0, 0, 180, 200, 2.5f);
                DisplayBuilder.dustParticles(convergencePoint.clone().add(0, 1, 0), 30, 3.0, 180, 30, 0, 2.5f);

                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(convergencePoint) <= 25.0) {
                        p.damage(20.0);
                    }
                }
            }

            // Post-detonation: ender dragon flap ambient
            int detTick = ticksAlive - 80;
            if (detTick == 10) {
                DisplayBuilder.playSound(convergencePoint, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EchoConvergencePoint(plugin); }
    }

    // =========================================================================
    // 138. PALE REMEMBRANCE -- silent near-invisible silhouette burst
    // =========================================================================
    public static class PaleRemembrance extends EnvironmentalAttack {

        private Location silhouettePos;
        private boolean detonated = false;

        public PaleRemembrance(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pale_remembrance", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(20.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(3600);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            double a = Math.random() * 2 * Math.PI;
            double d = 3 + Math.random() * 6;
            silhouettePos = center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);
        }

        @Override
        protected void onTick(int ticksAlive) {
            if (silhouettePos == null) return;
            World w = silhouettePos.getWorld();
            if (w == null) return;

            // NO SOUND AT ALL -- silence is the design

            // 100 ticks: barely visible silhouette forms
            if (ticksAlive <= 100) {
                if (ticksAlive % 10 == 0) {
                    // 2 block tall humanoid shape, extremely sparse
                    DisplayBuilder.dustParticles(silhouettePos.clone().add(0, 1.0, 0),
                            2, 0.2, 200, 200, 200, 1.0f);
                    if (ticksAlive > 60) {
                        DisplayBuilder.dustParticles(silhouettePos.clone().add(0, 0.5, 0),
                                1, 0.15, 200, 200, 200, 0.8f);
                        DisplayBuilder.dustParticles(silhouettePos.clone().add(0, 1.6, 0),
                                1, 0.1, 200, 200, 200, 0.7f);
                    }
                }
                return;
            }

            // Burst at 100 ticks
            if (!detonated) {
                detonated = true;
                // SPELL_WITCH magenta burst -- still no sound
                w.spawnParticle(Particle.WITCH, silhouettePos.clone().add(0, 1, 0),
                        80, 1.5, 1.5, 1.5, 0);

                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(silhouettePos) <= 9.0) {
                        p.damage(20.0);
                    }
                }
            }
            // Nothing remains. No trace. The memory evaporates completely.
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PaleRemembrance(plugin); }
    }

    // =========================================================================
    // 139. CRIMSON TIDE -- rising crimson particle flood punishes stillness
    // =========================================================================
    public static class CrimsonTide extends EnvironmentalAttack {

        private double tideLevel = 0;
        private boolean tideReceding = false;

        public CrimsonTide(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crimson_tide", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(16.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(3600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {}

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks -- floor seeps red
            if (ticksAlive <= 60) {
                if (ticksAlive % 4 == 0) {
                    for (int x = -9; x <= 9; x += 4) {
                        for (int z = -9; z <= 9; z += 4) {
                            DisplayBuilder.dustParticles(center.clone().add(x, 0.2, z),
                                    (int) (ticksAlive / 20.0), 1.0, 180, 0, 0, 1.0f);
                        }
                    }
                }
                if (ticksAlive == 50) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_AMBIENT, 0.9f, 0.3f);
                }
                return;
            }

            int tideTick = ticksAlive - 60;

            // Rising: 150 ticks to Y+12
            if (!tideReceding && tideTick <= 150) {
                tideLevel = Math.min(tideTick * 0.08, 12);

                if (tideTick % 3 == 0) {
                    for (int x = -8; x <= 8; x += 3) {
                        for (int z = -8; z <= 8; z += 3) {
                            DisplayBuilder.dustParticles(center.clone().add(x, tideLevel * 0.4, z),
                                    8, 1.5, 180, 0, 0, 2.0f);
                        }
                    }
                }

                // Stationary player damage every 2 seconds
                if (tideTick % 40 == 0 && tideTick > 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(center) <= 100.0 &&
                                p.getVelocity().lengthSquared() < 0.01) {
                            p.damage(16.0);
                        }
                    }
                }

                if (tideTick == 150) {
                    tideReceding = true;
                    DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 1.2f, 0.25f);
                }
            }

            // Receding: 100 ticks
            if (tideReceding) {
                tideLevel = Math.max(tideLevel - 0.12, 0);
                if (tideTick % 4 == 0 && tideLevel > 0) {
                    for (int x = -8; x <= 8; x += 4) {
                        for (int z = -8; z <= 8; z += 4) {
                            DisplayBuilder.dustParticles(center.clone().add(x, tideLevel * 0.3, z),
                                    4, 1.0, 180, 0, 0, 1.2f);
                        }
                    }
                }
            }

            // Permanent low stain
            if (tideTick > 250 && tideTick % 10 == 0) {
                for (int x = -8; x <= 8; x += 6) {
                    for (int z = -8; z <= 8; z += 6) {
                        DisplayBuilder.dustParticles(center.clone().add(x, 0.2, z),
                                1, 1.0, 180, 0, 0, 0.5f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrimsonTide(plugin); }
    }

    // =========================================================================
    // 140. MONOLITH CONVERGENCE -- all monolith beams lock simultaneously
    // =========================================================================
    public static class MonolithConvergence extends EnvironmentalAttack {

        private final List<Location> monolithPositions = new ArrayList<>();
        private final List<Double> beamAngles = new ArrayList<>();
        private boolean beamsLocked = false;

        public MonolithConvergence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("monolith_convergence", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(12.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(260);
            config.setCooldownTicks(2800);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            // 4-6 monoliths at arena perimeter
            int count = 4 + (int) (Math.random() * 3);
            for (int i = 0; i < count; i++) {
                double a = (2 * Math.PI * i) / count;
                monolithPositions.add(center.clone().add(Math.cos(a) * 10, 0, Math.sin(a) * 10));
                beamAngles.add(Math.random() * 2 * Math.PI);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 80 ticks -- beams sweep independently
            if (ticksAlive <= 80) {
                for (int i = 0; i < monolithPositions.size(); i++) {
                    Location mono = monolithPositions.get(i);
                    double speed = 0.5 + Math.random() * 1.5;
                    beamAngles.set(i, beamAngles.get(i) + Math.toRadians(speed));

                    if (ticksAlive % 3 == 0) {
                        DisplayBuilder.dustParticles(mono.clone().add(0, 2, 0),
                                10, 0.5, 180, 0, 0, 1.5f);
                        w.spawnParticle(Particle.END_ROD, mono.clone().add(0, 2, 0),
                                4, 0.3, 0.5, 0.3, 0.01);
                    }
                }
                if (ticksAlive == 70) {
                    for (int i = 0; i < monolithPositions.size(); i++) {
                        DisplayBuilder.playSound(monolithPositions.get(i), Sound.BLOCK_ANVIL_LAND,
                                0.5f, 0.4f);
                    }
                }
                return;
            }

            // Beams lock and fire at maximum intensity
            if (!beamsLocked) {
                beamsLocked = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.3f, 0.5f);
            }

            int lockTick = ticksAlive - 80;

            // Locked beams for 100 ticks
            if (lockTick <= 100) {
                if (lockTick % 2 == 0) {
                    for (int i = 0; i < monolithPositions.size(); i++) {
                        Location mono = monolithPositions.get(i);
                        double angle = beamAngles.get(i);

                        // Beam extends 18 blocks from monolith
                        for (int d = 0; d <= 18; d += 1) {
                            Location beamPos = mono.clone().add(
                                    Math.cos(angle) * d, 2.0, Math.sin(angle) * d);
                            w.spawnParticle(Particle.DRAGON_BREATH, beamPos, 6, 0.2, 0.2, 0.2, 0.005);
                            w.spawnParticle(Particle.END_ROD, beamPos, 3, 0.1, 0.1, 0.1, 0.005);
                        }
                    }
                }

                // Beam contact damage
                if (lockTick % 20 == 0) {
                    for (int i = 0; i < monolithPositions.size(); i++) {
                        Location mono = monolithPositions.get(i);
                        double angle = beamAngles.get(i);

                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            // Check if player is near beam line
                            for (int d = 0; d <= 18; d += 2) {
                                Location beamPos = mono.clone().add(
                                        Math.cos(angle) * d, 2.0, Math.sin(angle) * d);
                                if (p.getLocation().distanceSquared(beamPos) <= 2.25) {
                                    p.damage(12.0);
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            // Release at 100 ticks
            if (lockTick == 100) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MonolithConvergence(plugin); }
    }
}
