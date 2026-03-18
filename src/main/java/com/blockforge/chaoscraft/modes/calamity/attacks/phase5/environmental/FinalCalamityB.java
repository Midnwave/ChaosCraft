package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.environmental;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 4E Environmental -- FINAL CALAMITY B
 * Attacks #161-170: Arena dissolution begins. Crimson web, sweep salvo, stream phase shift,
 * peak saturation, floor crack grid, void tendrils, first/second/third floor collapse,
 * north monolith fall.
 *
 * Design notes:
 * - Calamitas palette: crimson (200,0,50), orange (255,100,0), soul blue (0,150,255), purple (128,0,255)
 * - Damage range: 14.0-28.0 HP (escalating finale)
 * - Arena dissolution -- the island itself starts to fail
 */
public final class FinalCalamityB {

    private FinalCalamityB() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CrimsonWeb(plugin));
        registry.register(new AllFiveSweepSalvo(plugin));
        registry.register(new StreamPhaseShift(plugin));
        registry.register(new PeakSaturation(plugin));
        registry.register(new FloorCrackGrid(plugin));
        registry.register(new VoidTendrils(plugin));
        registry.register(new FirstFloorCollapse(plugin));
        registry.register(new SecondFloorCollapse(plugin));
        registry.register(new ThirdFloorCollapse(plugin));
        registry.register(new NorthMonolithFall(plugin));
    }

    // =========================================================================
    // 161. CRIMSON WEB -- 16 red streams form descending grid
    // =========================================================================
    public static class CrimsonWeb extends EnvironmentalAttack {
        private double gridHeight = 20;
        public CrimsonWeb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crimson_web", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(14.0); config.setDamageRadius(1.0);
            config.setDurationTicks(260); config.setCooldownTicks(1300); config.setTicksBetweenDamage(20);
        }
        @Override protected void onSpawn(Location center) {}
        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;
            if (ticksAlive <= 60) {
                if (ticksAlive % 3 == 0) {
                    for (int i = -8; i <= 8; i += 2) {
                        DisplayBuilder.dustParticles(center.clone().add(i, 20, 0), 4, 0.3, 180, 0, 0, 2.0f);
                        DisplayBuilder.dustParticles(center.clone().add(0, 20, i), 4, 0.3, 180, 0, 0, 2.0f);
                    }
                }
                return;
            }
            int gridTick = ticksAlive - 60;
            if (gridTick <= 40) gridHeight = 20 - gridTick * 0.3;
            else if (gridTick <= 100) gridHeight = 8;
            else gridHeight = 8 + (gridTick - 100) * 0.3;

            if (gridTick % 3 == 0) {
                for (int i = -8; i <= 8; i += 2) {
                    DisplayBuilder.dustParticles(center.clone().add(i, gridHeight, 0), 6, 0.3, 180, 0, 0, 2.0f);
                    DisplayBuilder.dustParticles(center.clone().add(0, gridHeight, i), 6, 0.3, 180, 0, 0, 2.0f);
                }
            }
            if (gridTick > 30 && gridTick <= 100 && gridTick % 20 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (Math.abs(p.getLocation().getY() - (center.getY() + gridHeight)) <= 2.0 && p.getLocation().distanceSquared(center) <= 100.0) {
                        p.damage(14.0);
                    }
                }
            }
            if (gridTick == 5) DisplayBuilder.playSound(center, Sound.ENTITY_SPIDER_AMBIENT, 0.4f, 0.3f);
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new CrimsonWeb(plugin); }
    }

    // =========================================================================
    // 162. ALL FIVE SWEEP SALVO -- simultaneous horizontal sweeps in altitude bands
    // =========================================================================
    public static class AllFiveSweepSalvo extends EnvironmentalAttack {
        private boolean sweepFired = false;
        public AllFiveSweepSalvo(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("all_five_sweep_salvo", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(20.0); config.setDamageRadius(2.0);
            config.setDurationTicks(240); config.setCooldownTicks(99999); config.setTicksBetweenDamage(10);
        }
        @Override protected void onSpawn(Location center) {}
        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;
            // Buildup: 120 ticks -- sky darkens, vibrates, converges
            if (ticksAlive <= 120) {
                if (ticksAlive == 1) { /* 2-tick blackout */ }
                if (ticksAlive == 3) {
                    for (int i = 0; i < 40; i++) {
                        Location pos = center.clone().add((Math.random()-0.5)*18, 5+Math.random()*10, (Math.random()-0.5)*18);
                        switch ((int)(Math.random()*5)) {
                            case 0: w.spawnParticle(Particle.DRAGON_BREATH, pos, 6, 1.0, 0.5, 1.0, 0.01); break;
                            case 1: DisplayBuilder.dustParticles(pos, 6, 1.0, 180, 0, 0, 2.0f); break;
                            case 2: w.spawnParticle(Particle.WITCH, pos, 6, 1.0, 0.5, 1.0, 0); break;
                            case 3: w.spawnParticle(Particle.TOTEM_OF_UNDYING, pos, 4, 1.0, 0.5, 1.0, 0.01); break;
                            default: w.spawnParticle(Particle.END_ROD, pos, 6, 1.0, 0.5, 1.0, 0.005); break;
                        }
                    }
                }
                return;
            }
            // Five simultaneous sweeps for 30 ticks
            if (!sweepFired) {
                sweepFired = true;
                for (int i = 0; i < 5; i++) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.3f + i * 0.2f);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.5f);
            }
            int sweepTick = ticksAlive - 120;
            if (sweepTick <= 30) {
                double sweepX = -10 + sweepTick * 0.67;
                double[] bandYs = {0.5, 3.5, 6.5, 9.5, 12.5};
                Particle[] bandParticles = {Particle.DRAGON_BREATH, null, Particle.WITCH, Particle.TOTEM_OF_UNDYING, Particle.END_ROD};
                double[] bandDmg = {14.0, 16.0, 14.0, 16.0, 20.0};

                if (sweepTick % 2 == 0) {
                    for (int b = 0; b < 5; b++) {
                        for (int z = -8; z <= 8; z += 2) {
                            Location sweepPos = center.clone().add(sweepX, bandYs[b], z);
                            if (bandParticles[b] != null) w.spawnParticle(bandParticles[b], sweepPos, 8, 0.5, 0.5, 0.5, 0.01);
                            else DisplayBuilder.dustParticles(sweepPos, 8, 0.5, 180, 0, 0, 2.0f);
                        }
                    }
                }

                if (sweepTick % 5 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double pX = p.getLocation().getX() - center.getX();
                        if (Math.abs(pX - sweepX) <= 2.0) {
                            double pY = p.getLocation().getY() - center.getY();
                            for (int b = 0; b < 5; b++) {
                                if (Math.abs(pY - bandYs[b]) <= 1.5) {
                                    p.damage(bandDmg[b]);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            // Post-sweep: 10s pause at 50% density
            if (sweepTick > 30 && sweepTick % 6 == 0) {
                for (int i = 0; i < 5; i++) {
                    Location pos = center.clone().add((Math.random()-0.5)*16, 10+Math.random()*10, (Math.random()-0.5)*16);
                    w.spawnParticle(Particle.END_ROD, pos, 3, 0.5, 0.5, 0.5, 0.005);
                }
            }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new AllFiveSweepSalvo(plugin); }
    }

    // =========================================================================
    // 163. STREAM PHASE SHIFT -- all trajectories rotate 90 degrees
    // =========================================================================
    public static class StreamPhaseShift extends EnvironmentalAttack {
        public StreamPhaseShift(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("stream_phase_shift", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDurationTicks(480); config.setCooldownTicks(2200); config.setTicksBetweenDamage(999);
        }
        @Override protected void onSpawn(Location center) {}
        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;
            // Visual rotation indicator
            if (ticksAlive == 5) DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 0.8f);
            if (ticksAlive % 4 == 0) {
                double rotAngle = Math.min(ticksAlive * 0.9, 90.0);
                for (int i = 0; i < 6; i++) {
                    double baseAngle = (2*Math.PI*i)/6 + Math.toRadians(rotAngle);
                    Location pos = center.clone().add(Math.cos(baseAngle)*10, 15+Math.random()*5, Math.sin(baseAngle)*10);
                    w.spawnParticle(Particle.END_ROD, pos, 4, 0.5, 0.5, 0.5, 0.005);
                    w.spawnParticle(Particle.WITCH, pos, 3, 0.5, 0.5, 0.5, 0);
                }
            }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new StreamPhaseShift(plugin); }
    }

    // =========================================================================
    // 164. PEAK SATURATION -- sky at absolute maximum, 10x Phase 1 baseline
    // =========================================================================
    public static class PeakSaturation extends EnvironmentalAttack {
        public PeakSaturation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("peak_saturation", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDurationTicks(600); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999);
        }
        @Override protected void onSpawn(Location center) {}
        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;
            double density = Math.min(4.0 + ticksAlive * 0.03, 10.0);
            if (ticksAlive % 2 == 0) {
                int count = (int)(density * 8);
                for (int i = 0; i < count; i++) {
                    Location pos = center.clone().add((Math.random()-0.5)*20, 10+Math.random()*20, (Math.random()-0.5)*20);
                    switch ((int)(Math.random()*5)) {
                        case 0: w.spawnParticle(Particle.WITCH, pos, 6, 1.0, 0.5, 1.0, 0); break;
                        case 1: w.spawnParticle(Particle.TOTEM_OF_UNDYING, pos, 4, 1.0, 0.5, 1.0, 0.01); break;
                        case 2: w.spawnParticle(Particle.END_ROD, pos, 6, 1.0, 0.5, 1.0, 0.005); break;
                        case 3: w.spawnParticle(Particle.DRAGON_BREATH, pos, 4, 1.0, 0.5, 1.0, 0.005); break;
                        default: DisplayBuilder.dustParticles(pos, 4, 1.0, 180, 0, 0, 1.0f); break;
                    }
                }
            }
            if (ticksAlive == 5) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.7f, 1.5f);
            }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new PeakSaturation(plugin); }
    }

    // =========================================================================
    // 165. FLOOR CRACK GRID -- continuous random tile hazards
    // =========================================================================
    public static class FloorCrackGrid extends EnvironmentalAttack {
        private final List<Location> crackedTiles = new ArrayList<>();
        private int nextCrackTick = 0;
        public FloorCrackGrid(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("floor_crack_grid", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(14.0); config.setDamageRadius(0.5);
            config.setDurationTicks(600); config.setCooldownTicks(99999); config.setTicksBetweenDamage(20);
        }
        @Override protected void onSpawn(Location center) { nextCrackTick = 160 + (int)(Math.random()*140); }
        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;
            // Spawn new crack tile
            if (ticksAlive >= nextCrackTick) {
                double ox = (Math.random()-0.5)*16;
                double oz = (Math.random()-0.5)*16;
                Location crack = center.clone().add(ox, 0, oz);
                crackedTiles.add(crack);
                DisplayBuilder.playSound(crack, Sound.BLOCK_STONE_BREAK, 0.6f, 0.5f);
                DisplayBuilder.dustParticles(crack.clone().add(0, 0.3, 0), 10, 0.5, 120, 80, 50, 1.5f);
                // Crack display
                BlockDisplayHandle crackDisp = displayBuilder.spawnBlock(crack.clone().add(0, 0.1, 0), Material.CRACKED_STONE_BRICKS);
                crackDisp.scale(1.0f, 0.05f, 1.0f).glow(120, 80, 50).interpolation(3, 0);
                spawnedEntities.add(crackDisp.entity());
                nextCrackTick = ticksAlive + 160 + (int)(Math.random()*140);
            }
            // Active crack damage
            if (ticksAlive % 20 == 0) {
                for (Location crack : crackedTiles) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dx = p.getLocation().getX()-crack.getX();
                        double dz = p.getLocation().getZ()-crack.getZ();
                        if (dx*dx+dz*dz <= 1.0) p.damage(14.0);
                    }
                }
            }
            // Subtle particle from cracks
            if (ticksAlive % 8 == 0) {
                for (Location crack : crackedTiles) {
                    w.spawnParticle(Particle.LAVA, crack.clone().add(0, 0.3, 0), 1, 0.3, 0.1, 0.3, 0);
                    w.spawnParticle(Particle.SMOKE, crack.clone().add(0, 0.3, 0), 2, 0.2, 0.2, 0.2, 0.005);
                }
            }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new FloorCrackGrid(plugin); }
    }

    // =========================================================================
    // 166. VOID TENDRILS -- portal particle lines creep from edges
    // =========================================================================
    public static class VoidTendrils extends EnvironmentalAttack {
        private double tendrilReach = 0;
        public VoidTendrils(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_tendrils", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(14.0); config.setDamageRadius(1.0);
            config.setDurationTicks(300); config.setCooldownTicks(900); config.setTicksBetweenDamage(20);
        }
        @Override protected void onSpawn(Location center) {}
        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;
            // Buildup: 60 ticks -- portal particles at edges
            if (ticksAlive <= 60) {
                tendrilReach = ticksAlive * 0.1;
                if (ticksAlive % 3 == 0) {
                    for (int dir = 0; dir < 4; dir++) {
                        for (int i = 0; i < 2; i++) {
                            double offset = (Math.random()-0.5)*8;
                            Location edgePos;
                            switch (dir) {
                                case 0: edgePos = center.clone().add(-10+tendrilReach, 0.5, offset); break;
                                case 1: edgePos = center.clone().add(10-tendrilReach, 0.5, offset); break;
                                case 2: edgePos = center.clone().add(offset, 0.5, -10+tendrilReach); break;
                                default: edgePos = center.clone().add(offset, 0.5, 10-tendrilReach); break;
                            }
                            w.spawnParticle(Particle.PORTAL, edgePos, 8, 0.3, 0.5, 0.3, 0.01);
                            DisplayBuilder.dustParticles(edgePos, 4, 0.3, 100, 0, 160, 1.2f);
                        }
                    }
                }
                return;
            }
            // Tendrils extend to 18 blocks inward
            int tendrilTick = ticksAlive - 60;
            tendrilReach = 6 + Math.min(tendrilTick * 0.3, 12);
            if (tendrilTick <= 120 && tendrilTick % 2 == 0) {
                for (int dir = 0; dir < 4; dir++) {
                    for (int i = 0; i < 2; i++) {
                        double offset = (Math.random()-0.5)*6;
                        for (double d = 0; d <= tendrilReach; d += 2) {
                            Location tPos;
                            switch (dir) {
                                case 0: tPos = center.clone().add(-10+d, 1.5, offset); break;
                                case 1: tPos = center.clone().add(10-d, 1.5, offset); break;
                                case 2: tPos = center.clone().add(offset, 1.5, -10+d); break;
                                default: tPos = center.clone().add(offset, 1.5, 10-d); break;
                            }
                            w.spawnParticle(Particle.PORTAL, tPos, 6, 0.2, 0.3, 0.2, 0.005);
                        }
                    }
                }
            }
            // Tendril contact: damage + void-edge knockback
            if (tendrilTick % 20 == 0 && tendrilTick <= 120) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double px = Math.abs(p.getLocation().getX()-center.getX());
                    double pz = Math.abs(p.getLocation().getZ()-center.getZ());
                    if (px > 10-tendrilReach || pz > 10-tendrilReach) {
                        p.damage(14.0);
                        double angle = Math.atan2(p.getLocation().getZ()-center.getZ(), p.getLocation().getX()-center.getX());
                        p.setVelocity(p.getVelocity().add(new org.bukkit.util.Vector(Math.cos(angle)*0.6, 0.1, Math.sin(angle)*0.6)));
                    }
                }
            }
            if (ticksAlive == 65) DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 0.6f);
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new VoidTendrils(plugin); }
    }

    // =========================================================================
    // 167. FIRST FLOOR COLLAPSE -- 2x2 permanent void hole
    // =========================================================================
    public static class FirstFloorCollapse extends EnvironmentalAttack {
        private Location holeCenter;
        private boolean holeOpened = false;
        public FirstFloorCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("first_floor_collapse", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDurationTicks(180); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999);
        }
        @Override protected void onSpawn(Location center) {
            holeCenter = center.clone().add((Math.random()-0.5)*10, 0, (Math.random()-0.5)*10);
        }
        @Override
        protected void onTick(int ticksAlive) {
            if (holeCenter == null) return;
            World w = holeCenter.getWorld();
            if (w == null) return;
            // Sinking: 60 ticks
            if (ticksAlive <= 60) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.dustParticles(holeCenter.clone().add(0, 0.5, 0), 50, 1.0, 180, 0, 0, 2.0f);
                }
                if (ticksAlive == 50) DisplayBuilder.playSound(holeCenter, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.3f);
                return;
            }
            // Hole opens
            if (!holeOpened) {
                holeOpened = true;
                DisplayBuilder.playSound(holeCenter, Sound.BLOCK_SCULK_SENSOR_CLICKING_STOP, 1.0f, 0.2f);
                // Crying obsidian frame
                BlockDisplayHandle frame = displayBuilder.spawnBlock(holeCenter.clone().add(0, 0.05, 0), Material.CRYING_OBSIDIAN);
                frame.scale(2.2f, 0.05f, 2.2f).glow(128, 0, 255).interpolation(3, 0);
                spawnedEntities.add(frame.entity());
            }
            // Void particles rising from hole
            if (ticksAlive % 3 == 0) {
                w.spawnParticle(Particle.PORTAL, holeCenter.clone().add(0, -2, 0), 15, 0.5, 2.0, 0.5, 0.01);
                w.spawnParticle(Particle.REVERSE_PORTAL, holeCenter.clone().add(0, 0.5, 0), 10, 0.5, 0.5, 0.5, 0.01);
            }
            // Void kill check
            if (ticksAlive % 10 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dx = p.getLocation().getX()-holeCenter.getX();
                    double dz = p.getLocation().getZ()-holeCenter.getZ();
                    if (dx*dx+dz*dz <= 1.0 && p.getLocation().getY() < holeCenter.getY()-1) {
                        p.damage(1000.0);
                    }
                }
            }
        }
        @Override protected void onCleanup() { /* Keep displays for permanent hole */ }
        @Override public AbstractAttack newInstance() { return new FirstFloorCollapse(plugin); }
    }

    // =========================================================================
    // 168. SECOND FLOOR COLLAPSE -- identical mechanics, opposite side
    // =========================================================================
    public static class SecondFloorCollapse extends EnvironmentalAttack {
        private Location holeCenter;
        private boolean holeOpened = false;
        public SecondFloorCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("second_floor_collapse", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDurationTicks(180); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999);
        }
        @Override protected void onSpawn(Location center) {
            holeCenter = center.clone().add(-(Math.random()*5+3), 0, -(Math.random()*5+3));
        }
        @Override
        protected void onTick(int ticksAlive) {
            if (holeCenter == null) return;
            World w = holeCenter.getWorld();
            if (w == null) return;
            if (ticksAlive <= 60) {
                if (ticksAlive % 3 == 0) DisplayBuilder.dustParticles(holeCenter.clone().add(0, 0.5, 0), 50, 1.0, 180, 0, 0, 2.0f);
                if (ticksAlive == 50) DisplayBuilder.playSound(holeCenter, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.3f);
                return;
            }
            if (!holeOpened) {
                holeOpened = true;
                DisplayBuilder.playSound(holeCenter, Sound.BLOCK_SCULK_SENSOR_CLICKING_STOP, 1.0f, 0.2f);
                BlockDisplayHandle frame = displayBuilder.spawnBlock(holeCenter.clone().add(0, 0.05, 0), Material.CRYING_OBSIDIAN);
                frame.scale(2.2f, 0.05f, 2.2f).glow(128, 0, 255).interpolation(3, 0);
                spawnedEntities.add(frame.entity());
            }
            if (ticksAlive % 3 == 0) {
                w.spawnParticle(Particle.PORTAL, holeCenter.clone().add(0, -2, 0), 15, 0.5, 2.0, 0.5, 0.01);
                w.spawnParticle(Particle.REVERSE_PORTAL, holeCenter.clone().add(0, 0.5, 0), 10, 0.5, 0.5, 0.5, 0.01);
            }
            if (ticksAlive % 10 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dx = p.getLocation().getX()-holeCenter.getX();
                    double dz = p.getLocation().getZ()-holeCenter.getZ();
                    if (dx*dx+dz*dz <= 1.0 && p.getLocation().getY() < holeCenter.getY()-1) p.damage(1000.0);
                }
            }
        }
        @Override protected void onCleanup() {}
        @Override public AbstractAttack newInstance() { return new SecondFloorCollapse(plugin); }
    }

    // =========================================================================
    // 169. THIRD FLOOR COLLAPSE -- with explosion burst addition
    // =========================================================================
    public static class ThirdFloorCollapse extends EnvironmentalAttack {
        private Location holeCenter;
        private boolean holeOpened = false;
        public ThirdFloorCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("third_floor_collapse", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDurationTicks(180); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999);
        }
        @Override protected void onSpawn(Location center) {
            holeCenter = center.clone().add((Math.random()*5+2), 0, (Math.random()*5+2));
        }
        @Override
        protected void onTick(int ticksAlive) {
            if (holeCenter == null) return;
            World w = holeCenter.getWorld();
            if (w == null) return;
            // Floor flash before sinking
            if (ticksAlive == 1) {
                for (int x = -8; x <= 8; x += 2) {
                    for (int z = -8; z <= 8; z += 2) {
                        Location flPos = (getCenter() != null ? getCenter() : holeCenter).clone().add(x, 0.3, z);
                        w.spawnParticle(Particle.END_ROD, flPos, 3, 0.3, 0.1, 0.3, 0.01);
                    }
                }
            }
            if (ticksAlive <= 60) {
                if (ticksAlive % 3 == 0) DisplayBuilder.dustParticles(holeCenter.clone().add(0, 0.5, 0), 50, 1.0, 180, 0, 0, 2.0f);
                if (ticksAlive == 50) DisplayBuilder.playSound(holeCenter, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.3f);
                return;
            }
            if (!holeOpened) {
                holeOpened = true;
                DisplayBuilder.playSound(holeCenter, Sound.BLOCK_SCULK_SENSOR_CLICKING_STOP, 1.0f, 0.2f);
                // Double click (echo)
                plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                    DisplayBuilder.playSound(holeCenter, Sound.BLOCK_SCULK_SENSOR_CLICKING_STOP, 1.0f, 0.2f), 2);
                // Explosion burst
                w.spawnParticle(Particle.EXPLOSION, holeCenter.clone().add(0, 0.5, 0), 30, 0.5, 0.5, 0.5, 0);
                BlockDisplayHandle frame = displayBuilder.spawnBlock(holeCenter.clone().add(0, 0.05, 0), Material.CRYING_OBSIDIAN);
                frame.scale(2.2f, 0.05f, 2.2f).glow(128, 0, 255).interpolation(3, 0);
                spawnedEntities.add(frame.entity());
            }
            if (ticksAlive % 3 == 0) {
                w.spawnParticle(Particle.PORTAL, holeCenter.clone().add(0, -2, 0), 15, 0.5, 2.0, 0.5, 0.01);
                w.spawnParticle(Particle.REVERSE_PORTAL, holeCenter.clone().add(0, 0.5, 0), 10, 0.5, 0.5, 0.5, 0.01);
            }
            if (ticksAlive % 10 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dx = p.getLocation().getX()-holeCenter.getX();
                    double dz = p.getLocation().getZ()-holeCenter.getZ();
                    if (dx*dx+dz*dz <= 1.0 && p.getLocation().getY() < holeCenter.getY()-1) p.damage(1000.0);
                }
            }
        }
        @Override protected void onCleanup() {}
        @Override public AbstractAttack newInstance() { return new ThirdFloorCollapse(plugin); }
    }

    // =========================================================================
    // 170. NORTH MONOLITH FALL -- border obelisk topples inward
    // =========================================================================
    public static class NorthMonolithFall extends EnvironmentalAttack {
        private Location monolithBase;
        private boolean fallen = false;
        public NorthMonolithFall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("north_monolith_fall", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(16.0); config.setDamageRadius(3.0);
            config.setDurationTicks(200); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999);
        }
        @Override protected void onSpawn(Location center) { monolithBase = center.clone().add(0, 0, -10); }
        @Override
        protected void onTick(int ticksAlive) {
            if (monolithBase == null) return;
            World w = monolithBase.getWorld();
            if (w == null) return;
            // Vibration: 80 ticks
            if (ticksAlive <= 80) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.dustParticles(monolithBase.clone().add(0, 7, 0), 20, 0.5, 200, 150, 80, 1.5f);
                    w.spawnParticle(Particle.BLOCK, monolithBase.clone().add(0, 5, 0), 8, 0.3, 2.0, 0.3, 0, Material.STONE.createBlockData());
                }
                if (ticksAlive == 70) DisplayBuilder.playSound(monolithBase, Sound.BLOCK_STONE_BREAK, 0.7f, 0.3f);
                return;
            }
            // Fall: 40 ticks -- rotates 90 degrees inward
            int fallTick = ticksAlive - 80;
            if (fallTick <= 40) {
                double fallAngle = (fallTick / 40.0) * 90.0;
                double fallHeight = 14 * Math.cos(Math.toRadians(fallAngle));
                double fallDist = 14 * Math.sin(Math.toRadians(fallAngle));
                if (fallTick % 3 == 0) {
                    Location fallingTop = monolithBase.clone().add(0, fallHeight, fallDist);
                    DisplayBuilder.dustParticles(fallingTop, 10, 0.5, 200, 150, 80, 2.0f);
                    w.spawnParticle(Particle.SMOKE, fallingTop, 5, 0.5, 0.5, 0.5, 0.02);
                }
                return;
            }
            // Impact
            if (!fallen) {
                fallen = true;
                Location impactZone = monolithBase.clone().add(0, 0, 7);
                DisplayBuilder.playSound(impactZone, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.4f);
                w.spawnParticle(Particle.BLOCK, impactZone, 100, 2.0, 1.0, 2.0, 0, Material.STONE.createBlockData());
                w.spawnParticle(Particle.SMOKE, impactZone.clone().add(0, 1, 0), 50, 2.0, 1.0, 2.0, 0.03);
                w.spawnParticle(Particle.CRIT, impactZone, 30, 2.0, 0.5, 2.0, 0.1);
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(impactZone) <= 9.0) p.damage(16.0);
                }
            }
            // Settling dust
            if (fallTick > 40 && fallTick % 10 == 0) {
                w.spawnParticle(Particle.SMOKE, monolithBase.clone().add(0, 1, 7), 5, 3.0, 0.5, 1.0, 0.01);
            }
        }
        @Override protected void onCleanup() {}
        @Override public AbstractAttack newInstance() { return new NorthMonolithFall(plugin); }
    }
}
