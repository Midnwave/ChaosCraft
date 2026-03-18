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
 * Phase 4E (Boss 5: Supreme Calamitas) Environmental Attacks #51-60
 * BRIMSTONE HELLSCAPE -- vein ignition stages, skull emergence, magma eruptions
 *
 * Design: No status effects. Damage 6.0-18.0 HP. AxisAngle4f only.
 */
public final class TotalChaosA {

    private TotalChaosA() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VeinIgnitionStageOne(plugin));
        registry.register(new VeinIgnitionStageTwo(plugin));
        registry.register(new VeinIgnitionStageThree(plugin));
        registry.register(new BrimstoneSkullSingle(plugin));
        registry.register(new BrimstoneSkullCluster(plugin));
        registry.register(new MagmaEruptionIsolated(plugin));
        registry.register(new MagmaEruptionChain(plugin));
        registry.register(new BrimstoneMaze(plugin));
        registry.register(new BrimstoneMazeCollapsing(plugin));
        registry.register(new SoulFireRainLight(plugin));
    }

    // =========================================================================
    // 51. VEIN IGNITION STAGE ONE -- visual-only; cracks glow crimson
    // =========================================================================
    public static class VeinIgnitionStageOne extends EnvironmentalAttack {

        private final List<Location> crackPositions = new ArrayList<>();

        public VeinIgnitionStageOne(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("vein_ignition_stage_one", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(99999);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 15; i++) {
                crackPositions.add(center.clone().add((Math.random() - 0.5) * 18, 0, (Math.random() - 0.5) * 18));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;
            if (ticksAlive <= 60) {
                if (ticksAlive % 6 == 0) {
                    for (Location cp : crackPositions) {
                        DisplayBuilder.dustParticles(cp.clone().add(0, 0.3, 0), 5, 0.3, 200, 40, 0, 1.0f);
                        w.spawnParticle(Particle.LAVA, cp.clone().add(0, 0.5, 0), 1, 0.2, 0.1, 0.2, 0);
                    }
                    if (ticksAlive % 12 == 0) DisplayBuilder.playSound(center, Sound.BLOCK_NETHER_BRICKS_STEP, 0.3f, 0.5f);
                }
                return;
            }
            boolean pulseBright = ((ticksAlive / 24) % 2 == 0);
            if (ticksAlive % 4 == 0) {
                for (Location cp : crackPositions) {
                    int count = pulseBright ? 8 : 4;
                    DisplayBuilder.dustParticles(cp.clone().add(0, 0.5, 0), count, 0.3, 200, 40, 0, 1.0f);
                    for (double y = 0; y < 3; y += 1.0) {
                        DisplayBuilder.dustParticles(cp.clone().add(0, y, 0), 3, 0.2, 200, 40, 0, 0.8f);
                    }
                    w.spawnParticle(Particle.LAVA, cp.clone().add(0, 0.3, 0), 1, 0.1, 0.05, 0.1, 0);
                    w.spawnParticle(Particle.SMOKE, cp.clone().add(0, 1, 0), 2, 0.2, 0.3, 0.2, 0.01);
                }
            }
            if (ticksAlive % 40 == 0) DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 0.6f);
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VeinIgnitionStageOne(plugin); }
    }

    // =========================================================================
    // 52. VEIN IGNITION STAGE TWO -- eruption, magma carpet, 2 hearts/sec on cracks
    // =========================================================================
    public static class VeinIgnitionStageTwo extends EnvironmentalAttack {

        private final List<Location> crackPositions = new ArrayList<>();

        public VeinIgnitionStageTwo(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("vein_ignition_stage_two", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(6.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(99999);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 15; i++) crackPositions.add(center.clone().add((Math.random() - 0.5) * 18, 0, (Math.random() - 0.5) * 18));
            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.3f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;
            if (ticksAlive <= 40) { if (ticksAlive % 5 == 0) for (Location cp : crackPositions) DisplayBuilder.dustParticles(cp.clone().add(0, 1, 0), 10, 0.5, 255, 60, 0, 1.2f); return; }
            int eruptTick = ticksAlive - 41;
            if (eruptTick >= 0 && eruptTick <= 160) {
                if (eruptTick % 3 == 0) {
                    for (Location cp : crackPositions) {
                        w.spawnParticle(Particle.LAVA, cp.clone().add(0, 0.5, 0), 5, 0.3, 2.5, 0.3, 0);
                        w.spawnParticle(Particle.FLAME, cp.clone().add(0, 0.2, 0), 3, 0.5, 0.1, 0.5, 0.01);
                    }
                }
                if (eruptTick % 10 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        for (Location cp : crackPositions) { if (p.getLocation().distanceSquared(cp) <= 2.25) { p.damage(6.0); break; } }
                    }
                }
                if (eruptTick % 40 == 0) { DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 0.6f, 0.5f); DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 0.7f, 0.6f); }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VeinIgnitionStageTwo(plugin); }
    }

    // =========================================================================
    // 53. VEIN IGNITION STAGE THREE -- full island surface burn
    // =========================================================================
    public static class VeinIgnitionStageThree extends EnvironmentalAttack {

        private final List<Location> crackPositions = new ArrayList<>();

        public VeinIgnitionStageThree(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("vein_ignition_stage_three", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(6.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(99999);
            config.setTicksBetweenDamage(7);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 15; i++) crackPositions.add(center.clone().add((Math.random() - 0.5) * 18, 0, (Math.random() - 0.5) * 18));
            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;
            if (ticksAlive <= 100) { if (ticksAlive % 6 == 0) for (Location cp : crackPositions) DisplayBuilder.dustParticles(cp.clone().add(0, 0.5, 0), 8, 0.5, 200, 40, 0, 1.0f); return; }
            int burnTick = ticksAlive - 101;
            if (burnTick >= 0 && burnTick <= 300) {
                if (burnTick % 3 == 0) {
                    for (int i = 0; i < 15; i++) {
                        double fx = center.getX() + (Math.random() - 0.5) * 20;
                        double fz = center.getZ() + (Math.random() - 0.5) * 20;
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, new Location(w, fx, center.getY() + 0.2, fz), 3, 0.3, 0.1, 0.3, 0.01);
                    }
                    for (Location cp : crackPositions) {
                        DisplayBuilder.dustParticles(cp.clone().add(0, 1, 0), 5, 0.3, 180, 0, 0, 1.2f);
                    }
                }
                if (burnTick % 7 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        for (Location cp : crackPositions) { if (p.getLocation().distanceSquared(cp) <= 2.25) { p.damage(6.0); break; } }
                    }
                }
                if (burnTick % 30 == 0) DisplayBuilder.playSound(center, Sound.BLOCK_SOUL_SAND_FALL, 0.8f, 0.5f);
                if (burnTick > 240) {
                    double recede = (burnTick - 240) / 60.0;
                    if (burnTick % 5 == 0) for (int i = 0; i < (int) (10 * (1 - recede)); i++) {
                        double fx = center.getX() + (Math.random() - 0.5) * 20 * (1 - recede);
                        double fz = center.getZ() + (Math.random() - 0.5) * 20 * (1 - recede);
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, new Location(w, fx, center.getY() + 0.2, fz), 2, 0.2, 0.1, 0.2, 0.01);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VeinIgnitionStageThree(plugin); }
    }

    // =========================================================================
    // 54. BRIMSTONE SKULL SINGLE -- one skull rises from floor and launches
    // =========================================================================
    public static class BrimstoneSkullSingle extends EnvironmentalAttack {

        private Location skullPos;
        private boolean launched = false;
        private Location target;

        public BrimstoneSkullSingle(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_skull_single", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(1800);
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            skullPos = center.clone().add((Math.random() - 0.5) * 14, -1, (Math.random() - 0.5) * 14);
            DisplayBuilder.playSound(skullPos, Sound.BLOCK_NETHERRACK_BREAK, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || skullPos == null) return;
            World w = center.getWorld();
            if (w == null) return;
            if (ticksAlive <= 80) {
                double rise = (ticksAlive / 80.0) * 3;
                skullPos.setY(center.getY() - 1 + rise);
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.dustParticles(skullPos, 10, 0.5, 200, 0, 0, 1.2f);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, skullPos, 3, 0.3, 0.3, 0.3, 0.01);
                }
                return;
            }
            if (ticksAlive > 80 && ticksAlive <= 140) {
                if (ticksAlive % 3 == 0) w.spawnParticle(Particle.SOUL_FIRE_FLAME, skullPos, 5, 0.3, 0.3, 0.3, 0.02);
                if (ticksAlive == 81) {
                    Player nearest = null; double nearDist = Double.MAX_VALUE;
                    for (Player p : w.getPlayers()) { if (isExempt(p)) continue; double d = p.getLocation().distanceSquared(skullPos); if (d < nearDist) { nearDist = d; nearest = p; } }
                    target = nearest != null ? nearest.getLocation().clone() : center.clone();
                }
                return;
            }
            if (!launched && target != null) {
                launched = true;
                DisplayBuilder.playSound(skullPos, Sound.ENTITY_WITHER_SHOOT, 0.8f, 0.7f);
                triggerImpactDamage(target);
                w.spawnParticle(Particle.EXPLOSION, target, 1, 0, 0, 0, 0);
                DisplayBuilder.dustParticles(target, 30, 1.5, 255, 50, 0, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneSkullSingle(plugin); }
    }

    // =========================================================================
    // 55. BRIMSTONE SKULL CLUSTER -- five skulls emerge and launch simultaneously
    // =========================================================================
    public static class BrimstoneSkullCluster extends EnvironmentalAttack {

        private final List<Location> skullPositions = new ArrayList<>();

        public BrimstoneSkullCluster(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_skull_cluster", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0); config.setDamageRadius(0.0); config.setDurationTicks(180); config.setCooldownTicks(1500); config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true); config.setImpactDamage(8.0); config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld(); if (w == null) return;
            for (int i = 0; i < 5; i++) skullPositions.add(center.clone().add((Math.random() - 0.5) * 16, 0, (Math.random() - 0.5) * 16));
            DisplayBuilder.playSound(center, Sound.BLOCK_NETHERRACK_BREAK, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return;
            World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 100) {
                if (ticksAlive % 4 == 0) for (Location sp : skullPositions) { DisplayBuilder.dustParticles(sp.clone().add(0, ticksAlive / 100.0 * 2, 0), 8, 0.4, 200, 0, 0, 1.0f); w.spawnParticle(Particle.SOUL_FIRE_FLAME, sp.clone().add(0, 1.5, 0), 3, 0.2, 0.3, 0.2, 0.01); }
                return;
            }
            if (ticksAlive == 101) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.6f, 0.6f);
                List<Player> players = new ArrayList<>(w.getPlayers()); players.removeIf(this::isExempt);
                for (Location sp : skullPositions) {
                    Player nearest = null; double nd = Double.MAX_VALUE;
                    for (Player p : players) { double d = p.getLocation().distanceSquared(sp); if (d < nd) { nd = d; nearest = p; } }
                    Location tgt = nearest != null ? nearest.getLocation().clone() : center;
                    triggerImpactDamage(tgt);
                    DisplayBuilder.playSound(sp, Sound.ENTITY_WITHER_SHOOT, 0.7f, 0.7f);
                    w.spawnParticle(Particle.EXPLOSION, tgt, 1, 0, 0, 0, 0);
                    DisplayBuilder.dustParticles(tgt, 25, 1.5, 255, 50, 0, 1.2f);
                }
            }
            if (ticksAlive > 101 && ticksAlive % 8 == 0) for (Location sp : skullPositions) DisplayBuilder.dustParticles(sp, 5, 0.5, 80, 20, 0, 0.6f);
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneSkullCluster(plugin); }
    }

    // =========================================================================
    // 56. MAGMA ERUPTION ISOLATED -- 3x3 zone erupts with lava
    // =========================================================================
    public static class MagmaEruptionIsolated extends EnvironmentalAttack {

        private Location eruptPos;
        private boolean erupted = false;

        public MagmaEruptionIsolated(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_eruption_isolated", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(6.0); config.setDamageRadius(0.0); config.setDurationTicks(180); config.setCooldownTicks(1200); config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld(); if (w == null) return;
            eruptPos = center.clone().add((Math.random() - 0.5) * 14, 0, (Math.random() - 0.5) * 14);
            DisplayBuilder.playSound(eruptPos, Sound.BLOCK_LAVA_AMBIENT, 0.6f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null || eruptPos == null) return;
            World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 60) {
                if (ticksAlive % 4 == 0) { w.spawnParticle(Particle.LAVA, eruptPos.clone().add(0, 0.5, 0), 3, 0.5, 0.2, 0.5, 0); DisplayBuilder.particleRing(eruptPos.clone().add(0, 0.2, 0), 1.5, Particle.DUST, 12, new Particle.DustOptions(Color.fromRGB(255, 100, 0), 1.0f)); }
                return;
            }
            if (!erupted) {
                erupted = true; DisplayBuilder.playSound(eruptPos, Sound.BLOCK_LAVA_POP, 0.8f, 0.5f);
                for (double y = 0; y < 6; y += 0.5) w.spawnParticle(Particle.LAVA, eruptPos.clone().add(0, y, 0), 5, 0.3, 0.2, 0.3, 0);
                for (double r = 0; r < 4; r += 0.5) DisplayBuilder.particleRing(eruptPos.clone().add(0, 0.3, 0), r, Particle.FLAME, (int) (r * 5), null);
                for (Player p : w.getPlayers()) { if (isExempt(p)) continue; if (p.getLocation().distanceSquared(eruptPos) <= 4.5) p.damage(6.0); }
            }
            int activeTick = ticksAlive - 61;
            if (activeTick >= 0 && activeTick <= 120) {
                if (activeTick % 4 == 0) { w.spawnParticle(Particle.LAVA, eruptPos.clone().add(0, 0.3, 0), 3, 1.0, 0.1, 1.0, 0); w.spawnParticle(Particle.FLAME, eruptPos.clone().add(0, 0.2, 0), 5, 1.0, 0.1, 1.0, 0.01); }
                if (activeTick % 10 == 0) for (Player p : w.getPlayers()) { if (isExempt(p)) continue; if (p.getLocation().distanceSquared(eruptPos) <= 4.5) p.damage(6.0); }
            }
            if (activeTick == 120) { DisplayBuilder.playSound(eruptPos, Sound.BLOCK_LAVA_EXTINGUISH, 0.7f, 0.6f); DisplayBuilder.dustParticles(eruptPos.clone().add(0, 0.3, 0), 20, 1.5, 80, 20, 0, 0.8f); }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MagmaEruptionIsolated(plugin); }
    }

    // =========================================================================
    // 57. MAGMA ERUPTION CHAIN -- rolling wave of eruptions across arena
    // =========================================================================
    public static class MagmaEruptionChain extends EnvironmentalAttack {

        private int waveDirection; // 0=N->S, 1=E->W

        public MagmaEruptionChain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_eruption_chain", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(6.0); config.setDamageRadius(0.0); config.setDurationTicks(200); config.setCooldownTicks(2400); config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) { waveDirection = Math.random() < 0.5 ? 0 : 1; DisplayBuilder.playSound(center, Sound.BLOCK_NETHERRACK_BREAK, 0.6f, 0.4f); }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return;
            World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 80) { if (ticksAlive % 5 == 0) { double wavePos = -10 + (ticksAlive / 80.0) * 5; for (double perp = -10; perp <= 10; perp += 2) { Location tracePos = waveDirection == 0 ? center.clone().add(perp, 0.1, wavePos) : center.clone().add(wavePos, 0.1, perp); DisplayBuilder.dustParticles(tracePos, 3, 0.3, 200, 40, 0, 0.8f); } } return; }
            int waveTick = ticksAlive - 81;
            if (waveTick >= 0 && waveTick <= 100) {
                double wavePos = -10 + (waveTick / 100.0) * 20;
                if (waveTick % 3 == 0) {
                    for (double perp = -10; perp <= 10; perp += 1) {
                        Location eruptPos = waveDirection == 0 ? center.clone().add(perp, 0.2, wavePos) : center.clone().add(wavePos, 0.2, perp);
                        w.spawnParticle(Particle.LAVA, eruptPos.clone().add(0, 0.3, 0), 3, 0.3, 2, 0.3, 0);
                        w.spawnParticle(Particle.FLAME, eruptPos, 2, 0.3, 0.1, 0.3, 0.01);
                    }
                }
                if (waveTick % 6 == 0) DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 0.5f, 0.6f);
                if (waveTick % 10 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double playerWavePos = waveDirection == 0 ? p.getLocation().getZ() - center.getZ() : p.getLocation().getX() - center.getX();
                        if (Math.abs(playerWavePos - wavePos) < 2.0) p.damage(6.0);
                    }
                }
            }
            if (waveTick > 100 && waveTick % 6 == 0) { w.spawnParticle(Particle.SMOKE, center.clone().add(0, 0.5, 0), 10, 10, 0.3, 10, 0.02); }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MagmaEruptionChain(plugin); }
    }

    // =========================================================================
    // 58. BRIMSTONE MAZE -- soul fire walls form maze pattern
    // =========================================================================
    public static class BrimstoneMaze extends EnvironmentalAttack {

        private final List<double[]> wallSegments = new ArrayList<>();

        public BrimstoneMaze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_maze", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(6.0); config.setDamageRadius(0.0); config.setDurationTicks(240); config.setCooldownTicks(1800); config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld(); if (w == null) return;
            // L-shaped + corridor layout
            wallSegments.add(new double[]{-5, -8, -5, 0});    // Vertical wall 1
            wallSegments.add(new double[]{-5, 0, 0, 0});      // Horizontal wall 1
            wallSegments.add(new double[]{5, -8, 5, 0});       // Vertical wall 2
            wallSegments.add(new double[]{0, 4, 5, 4});        // Horizontal wall 2
            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.7f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return;
            World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 80) {
                if (ticksAlive % 4 == 0) for (double[] seg : wallSegments) {
                    double steps = Math.max(Math.abs(seg[2] - seg[0]), Math.abs(seg[3] - seg[1]));
                    for (double t = 0; t <= 1; t += 1.0 / Math.max(1, steps)) {
                        double x = seg[0] + (seg[2] - seg[0]) * t;
                        double z = seg[1] + (seg[3] - seg[1]) * t;
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(x, 0.2, z), 2, 0.1, 0.1, 0.1, 0.005);
                    }
                }
                return;
            }
            int wallTick = ticksAlive - 81;
            if (wallTick >= 0 && wallTick <= 160) {
                if (wallTick % 3 == 0) for (double[] seg : wallSegments) {
                    double steps = Math.max(Math.abs(seg[2] - seg[0]), Math.abs(seg[3] - seg[1]));
                    for (double t = 0; t <= 1; t += 1.0 / Math.max(1, steps)) {
                        double x = seg[0] + (seg[2] - seg[0]) * t;
                        double z = seg[1] + (seg[3] - seg[1]) * t;
                        for (double y = 0; y < 3; y += 1.0) w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(x, y, z), 3, 0.1, 0.2, 0.1, 0.005);
                    }
                }
                if (wallTick % 10 == 0) for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    for (double[] seg : wallSegments) {
                        double px = p.getLocation().getX() - center.getX(); double pz = p.getLocation().getZ() - center.getZ();
                        double segDx = seg[2] - seg[0]; double segDz = seg[3] - seg[1]; double segLen = Math.sqrt(segDx * segDx + segDz * segDz);
                        if (segLen < 0.1) continue;
                        double t = Math.max(0, Math.min(1, ((px - seg[0]) * segDx + (pz - seg[1]) * segDz) / (segLen * segLen)));
                        double closestX = seg[0] + segDx * t; double closestZ = seg[1] + segDz * t;
                        double dist = Math.sqrt((px - closestX) * (px - closestX) + (pz - closestZ) * (pz - closestZ));
                        if (dist <= 1.0 && p.getLocation().getY() < center.getY() + 3) { p.damage(6.0); break; }
                    }
                }
                if (wallTick == 0) DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.3f, 0.5f);
            }
            if (wallTick == 160) { DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.2f, 0.5f); for (double[] seg : wallSegments) { double mx = (seg[0] + seg[2]) / 2; double mz = (seg[1] + seg[3]) / 2; w.spawnParticle(Particle.END_ROD, center.clone().add(mx, 1.5, mz), 20, 2, 1, 2, 0.05); } }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneMaze(plugin); }
    }

    // =========================================================================
    // 59. BRIMSTONE MAZE COLLAPSING -- maze variant with shifting safe path
    // =========================================================================
    public static class BrimstoneMazeCollapsing extends EnvironmentalAttack {

        private final List<double[]> wallSegments = new ArrayList<>();
        private final List<double[]> shiftedSegments = new ArrayList<>();
        private boolean shifted = false;

        public BrimstoneMazeCollapsing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_maze_collapsing", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(6.0); config.setDamageRadius(0.0); config.setDurationTicks(240); config.setCooldownTicks(1600); config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            wallSegments.add(new double[]{-6, -6, -6, 2});
            wallSegments.add(new double[]{-6, 2, 0, 2});
            wallSegments.add(new double[]{6, -6, 6, 2});
            wallSegments.add(new double[]{0, 5, 6, 5});
            shiftedSegments.add(new double[]{-4, -6, -4, 4});
            shiftedSegments.add(new double[]{-4, 4, 4, 4});
            shiftedSegments.add(new double[]{4, -6, 4, 0});
            shiftedSegments.add(new double[]{-2, -2, 4, -2});
            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.7f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return;
            World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 60) { if (ticksAlive % 4 == 0) for (double[] seg : wallSegments) drawWallTrace(w, center, seg); return; }
            int wallTick = ticksAlive - 61;
            List<double[]> activeWalls = (!shifted) ? wallSegments : shiftedSegments;
            if (wallTick >= 0 && wallTick <= 160) {
                if (wallTick == 100 && !shifted) {
                    shifted = true;
                    DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 0.7f, 0.5f);
                    for (double[] seg : wallSegments) { double mx = (seg[0] + seg[2]) / 2; double mz = (seg[1] + seg[3]) / 2; w.spawnParticle(Particle.LAVA, center.clone().add(mx, 1, mz), 10, 1, 0.5, 1, 0); w.spawnParticle(Particle.SMOKE, center.clone().add(mx, 1, mz), 15, 1, 0.5, 1, 0.02); }
                }
                if (wallTick % 3 == 0) for (double[] seg : activeWalls) drawWallFull(w, center, seg);
                if (wallTick % 10 == 0) for (Player p : w.getPlayers()) { if (isExempt(p)) continue; for (double[] seg : activeWalls) if (isNearSegment(p, center, seg)) { p.damage(6.0); break; } }
            }
            if (wallTick == 160) { DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.2f, 0.5f); for (double[] seg : activeWalls) { double mx = (seg[0] + seg[2]) / 2; double mz = (seg[1] + seg[3]) / 2; w.spawnParticle(Particle.END_ROD, center.clone().add(mx, 1.5, mz), 20, 2, 1, 2, 0.05); } }
        }

        private void drawWallTrace(World w, Location center, double[] seg) {
            double steps = Math.max(Math.abs(seg[2] - seg[0]), Math.abs(seg[3] - seg[1]));
            for (double t = 0; t <= 1; t += 1.0 / Math.max(1, steps)) {
                double x = seg[0] + (seg[2] - seg[0]) * t; double z = seg[1] + (seg[3] - seg[1]) * t;
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(x, 0.2, z), 2, 0.1, 0.1, 0.1, 0.005);
            }
        }

        private void drawWallFull(World w, Location center, double[] seg) {
            double steps = Math.max(Math.abs(seg[2] - seg[0]), Math.abs(seg[3] - seg[1]));
            for (double t = 0; t <= 1; t += 1.0 / Math.max(1, steps)) {
                double x = seg[0] + (seg[2] - seg[0]) * t; double z = seg[1] + (seg[3] - seg[1]) * t;
                for (double y = 0; y < 3; y += 1.0) w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(x, y, z), 3, 0.1, 0.2, 0.1, 0.005);
            }
        }

        private boolean isNearSegment(Player p, Location center, double[] seg) {
            double px = p.getLocation().getX() - center.getX(); double pz = p.getLocation().getZ() - center.getZ();
            double segDx = seg[2] - seg[0]; double segDz = seg[3] - seg[1]; double segLen = Math.sqrt(segDx * segDx + segDz * segDz);
            if (segLen < 0.1) return false;
            double t = Math.max(0, Math.min(1, ((px - seg[0]) * segDx + (pz - seg[1]) * segDz) / (segLen * segLen)));
            double closestX = seg[0] + segDx * t; double closestZ = seg[1] + segDz * t;
            double dist = Math.sqrt((px - closestX) * (px - closestX) + (pz - closestZ) * (pz - closestZ));
            return dist <= 1.0 && p.getLocation().getY() < center.getY() + 3;
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneMazeCollapsing(plugin); }
    }

    // =========================================================================
    // 60. SOUL FIRE RAIN LIGHT -- gentle soul fire rain from Y+40
    // =========================================================================
    public static class SoulFireRainLight extends EnvironmentalAttack {

        public SoulFireRainLight(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_fire_rain_light", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0); config.setDamageRadius(0.0); config.setDurationTicks(240); config.setCooldownTicks(1400); config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true); config.setImpactDamage(6.0); config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) { DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.4f, 0.5f); }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return;
            World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 40) { if (ticksAlive % 10 == 0) for (int i = 0; i < 3; i++) { double rx = center.getX() + (Math.random() - 0.5) * 20; double rz = center.getZ() + (Math.random() - 0.5) * 20; w.spawnParticle(Particle.SOUL_FIRE_FLAME, new Location(w, rx, center.getY() + 40, rz), 3, 0.5, 0.5, 0.5, 0.01); } return; }
            int rainTick = ticksAlive - 41;
            if (rainTick >= 0 && rainTick <= 200) {
                int particlesPerTick = 4 + (int) (Math.random() * 5); // ~8/tick
                for (int i = 0; i < particlesPerTick; i++) {
                    double rx = center.getX() + (Math.random() - 0.5) * 20;
                    double rz = center.getZ() + (Math.random() - 0.5) * 20;
                    double ry = center.getY() + 40 - (rainTick % 40) * 1.0;
                    if (ry <= center.getY() + 0.5) {
                        Location splatter = new Location(w, rx, center.getY() + 0.2, rz);
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, splatter, 15, 1.5, 0.2, 1.5, 0.02);
                        if (Math.random() < 0.3) DisplayBuilder.playSound(splatter, Sound.BLOCK_GLASS_BREAK, 0.2f, 1.4f);
                        for (Player p : w.getPlayers()) { if (isExempt(p)) continue; if (p.getLocation().distanceSquared(splatter) <= 9.0) p.damage(6.0); }
                    } else {
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, new Location(w, rx, ry, rz), 1, 0, 0, 0, 0);
                    }
                }
                if (rainTick % 20 == 0) DisplayBuilder.playSound(center, Sound.BLOCK_SOUL_SAND_FALL, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulFireRainLight(plugin); }
    }
}
