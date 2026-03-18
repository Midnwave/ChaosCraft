package com.blockforge.chaoscraft.modes.calamity.attacks.phase2.boss;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 2 Boss Attacks - GROUP 7: COSMIC LASER BARRAGE (#61-70)
 * White/blinding laser attacks, multi-target beams, laser walls.
 * Phase 2 laser language is blinding and near-inescapable.
 * NO status effects - damage only.
 */
public final class CosmicLaserBarrage {

    private CosmicLaserBarrage() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new SolarFlareBeam(plugin));
        registry.register(new MultiTargetBeamLock(plugin));
        registry.register(new LaserWall(plugin));
        registry.register(new BlindingCross(plugin));
        registry.register(new LaserRain(plugin));
        registry.register(new RetributionArc(plugin));
        registry.register(new PinpointConvergence(plugin));
        registry.register(new SpiralLaser(plugin));
        registry.register(new WhiteOutPulse(plugin));
        registry.register(new Superbeam(plugin));
    }

    // ================================================================
    // 61. SOLAR FLARE BEAM - Pure white 4-block-wide column
    // Locks to vector at firing, predictive aiming at player velocity
    // ================================================================
    public static class SolarFlareBeam extends BossAttack {
        private final List<BlockDisplayHandle> chargeHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private boolean beamFired = false;
        private double beamAngle = 0;

        public SolarFlareBeam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("solar_flare_beam", AttackType.BOSS, 2), "dog");
            config.setDamage(12.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Head charge glow - blazing white sphere
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2 / 8) * i;
                Location loc = center.clone().add(Math.cos(a) * 1.5, 3, Math.sin(a) * 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                h.scale(0.6f, 0.6f, 0.6f).glow(240, 240, 255).interpolation(3, 0);
                chargeHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            beamAngle = Math.random() * Math.PI * 2;
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Head charge buildup (0-40 ticks = 2 sec)
            if (ticksAlive < 40 && !beamFired) {
                float intensity = ticksAlive / 40.0f;
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), (int)(10 * intensity + 3), 2.0 * intensity);
                }
                // Charge blocks orbit and brighten
                float angle = ticksAlive * 0.1f;
                for (int i = 0; i < chargeHandles.size(); i++) {
                    double a = angle + (Math.PI * 2 / chargeHandles.size()) * i;
                    float r = 1.5f - intensity * 0.8f;
                    chargeHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(a) * r, 3, Math.sin(a) * r));
                }
            }
            // Beam fires (tick 40)
            else if (ticksAlive == 40 && !beamFired) {
                beamFired = true;
                // Remove charge glow
                for (BlockDisplayHandle h : chargeHandles) h.entity().remove();
                chargeHandles.clear();

                // 4-block-wide beam extending 25 blocks in locked direction
                for (int seg = 0; seg < 25; seg++) {
                    double bx = Math.cos(beamAngle) * seg;
                    double bz = Math.sin(beamAngle) * seg;
                    Location beamLoc = center.clone().add(bx, 2, bz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(beamLoc, Material.SEA_LANTERN);
                    h.scale(4.0f, 3.0f, 1.0f).glow(240, 240, 255).interpolation(1, 0);
                    beamHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.5f);
            }
            // Beam sustained and pulsing (40-140 ticks = 5 sec)
            else if (beamFired && ticksAlive < 140) {
                if (ticksAlive % 8 == 0) {
                    for (int seg = 0; seg < beamHandles.size(); seg += 5) {
                        DisplayBuilder.cyanDust(beamHandles.get(seg).entity().getLocation(), 6, 2.0);
                    }
                }
            }
            // Beam fades (140+)
            else if (beamFired && ticksAlive == 140) {
                for (BlockDisplayHandle h : beamHandles) h.entity().remove();
                beamHandles.clear();
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SolarFlareBeam(plugin); }
    }

    // ================================================================
    // 62. MULTI-TARGET BEAM LOCK - Separate beam per body segment
    // Web of beams radiating toward all player positions
    // ================================================================
    public static class MultiTargetBeamLock extends BossAttack {
        private final List<BlockDisplayHandle> sourceHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private boolean beamsFired = false;
        private final double[] beamAngles = new double[6];

        public MultiTargetBeamLock(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("multi_target_beam_lock", AttackType.BOSS, 2), "dog");
            config.setDamage(14.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 6 source segments along a line representing body
            for (int i = 0; i < 6; i++) {
                Location segLoc = center.clone().add(i * 2 - 5, 3, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.AMETHYST_BLOCK);
                h.scale(0.8f, 0.8f, 0.8f).glow(0, 200, 255).interpolation(3, 0);
                sourceHandles.add(h);
                spawnedEntities.add(h.entity());
                beamAngles[i] = (Math.PI * 2 / 6) * i + Math.random() * 0.5;
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.2f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Source segments glow (0-24 ticks = 1.2 sec telegraph)
            if (ticksAlive < 24 && !beamsFired) {
                int segToGlow = ticksAlive / 4;
                if (segToGlow < sourceHandles.size() && ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(sourceHandles.get(segToGlow).entity().getLocation(), 8, 0.8);
                }
            }
            // Beams fire (tick 24)
            else if (ticksAlive == 24 && !beamsFired) {
                beamsFired = true;
                for (int seg = 0; seg < 6; seg++) {
                    Location source = center.clone().add(seg * 2 - 5, 3, 0);
                    double angle = beamAngles[seg];
                    // Each beam extends 18 blocks
                    for (int b = 1; b <= 18; b++) {
                        Location beamLoc = source.clone().add(
                            Math.cos(angle) * b, -1, Math.sin(angle) * b);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(beamLoc, Material.END_ROD);
                        h.scale(0.3f, 0.3f, 1.5f).glow(240, 240, 255).interpolation(1, 0);
                        beamHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.5f, 0.5f);
            }
            // Beams sustained with particles (24-84 ticks = 3 sec)
            else if (beamsFired && ticksAlive < 84) {
                if (ticksAlive % 10 == 0) {
                    for (int i = 0; i < beamHandles.size(); i += 6) {
                        DisplayBuilder.cyanDust(beamHandles.get(i).entity().getLocation(), 3, 0.5);
                    }
                }
            }
            // Beams dissipate (tick 84)
            else if (ticksAlive == 84) {
                for (BlockDisplayHandle h : beamHandles) h.entity().remove();
                beamHandles.clear();
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MultiTargetBeamLock(plugin); }
    }

    // ================================================================
    // 63. LASER WALL - Horizontal beam wall stacked at multiple heights
    // Moves across arena; two walls in succession with small gaps
    // ================================================================
    public static class LaserWall extends BossAttack {
        private final List<BlockDisplayHandle> wallHandles = new ArrayList<>();
        private int wallsSpawned = 0;
        private float wallPosition = -20;

        public LaserWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("laser_wall", AttackType.BOSS, 2), "dog");
            config.setDamage(12.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Wall 1 forms (0-30 ticks = 1.5 sec telegraph)
            if (ticksAlive < 30 && wallsSpawned == 0) {
                if (ticksAlive % 6 == 0) {
                    int layerIdx = ticksAlive / 6;
                    double[] heights = {6, 4.5, 3, 1.5, 0};
                    if (layerIdx < heights.length) {
                        // Wall at starting edge
                        for (int z = -12; z <= 12; z += 2) {
                            BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(-20, heights[layerIdx], z), Material.SEA_LANTERN);
                            h.scale(0.3f, 0.3f, 2.0f).glow(240, 240, 255).interpolation(2, 0);
                            wallHandles.add(h);
                            spawnedEntities.add(h.entity());
                        }
                    }
                }
            }
            // Wall 1 spawned
            if (ticksAlive == 30 && wallsSpawned == 0) {
                wallsSpawned = 1;
                wallPosition = -20;
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.5f, 0.4f);
            }
            // Wall 1 advances (30-96 ticks ~ 6 blocks/sec movement)
            if (wallsSpawned >= 1 && ticksAlive >= 30 && ticksAlive < 100) {
                wallPosition += 0.6f;
                for (BlockDisplayHandle h : wallHandles) {
                    Location loc = h.entity().getLocation();
                    h.entity().teleport(center.clone().add(wallPosition, loc.getY() - center.getY(), loc.getZ() - center.getZ()));
                }
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(wallPosition, 3, 0), 8, 6.0);
                }
            }
            // Wall 2 spawns (tick 90, 3 sec after first)
            if (ticksAlive == 90 && wallsSpawned == 1) {
                wallsSpawned = 2;
                double[] heights2 = {5.5, 4, 2.5, 1, 0};
                for (double h : heights2) {
                    for (int z = -12; z <= 12; z += 2) {
                        BlockDisplayHandle bh = displayBuilder.spawnBlock(
                            center.clone().add(-20, h, z), Material.SEA_LANTERN);
                        bh.scale(0.3f, 0.3f, 2.0f).glow(240, 240, 255).interpolation(2, 0);
                        wallHandles.add(bh);
                        spawnedEntities.add(bh.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LaserWall(plugin); }
    }

    // ================================================================
    // 64. BLINDING CROSS - Two beams forming a rotating cross
    // Divides arena into 4 quadrants, rotates at 15 deg/sec
    // ================================================================
    public static class BlindingCross extends BossAttack {
        private final List<BlockDisplayHandle> crossHandles = new ArrayList<>();
        private boolean crossFormed = false;

        public BlindingCross(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blinding_cross", AttackType.BOSS, 2), "dog");
            config.setDamage(14.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(340);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Telegraph: two source segments glow
            BlockDisplayHandle hHead = displayBuilder.spawnBlock(
                center.clone().add(2, 4, 0), Material.AMETHYST_BLOCK);
            hHead.scale(0.8f, 0.8f, 0.8f).glow(0, 200, 255).interpolation(3, 0);
            crossHandles.add(hHead);
            spawnedEntities.add(hHead.entity());

            BlockDisplayHandle hMid = displayBuilder.spawnBlock(
                center.clone().add(-2, 4, 0), Material.AMETHYST_BLOCK);
            hMid.scale(0.8f, 0.8f, 0.8f).glow(0, 200, 255).interpolation(3, 0);
            crossHandles.add(hMid);
            spawnedEntities.add(hMid.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph glow (0-30 ticks)
            if (ticksAlive < 30 && !crossFormed) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(2, 4, 0), 6, 0.8);
                    DisplayBuilder.cyanDust(center.clone().add(-2, 4, 0), 6, 0.8);
                }
            }
            // Form cross (tick 30)
            else if (ticksAlive == 30 && !crossFormed) {
                crossFormed = true;
                // Remove telegraph blocks
                for (BlockDisplayHandle h : crossHandles) h.entity().remove();
                crossHandles.clear();

                // Two perpendicular beams, each 20 blocks long
                for (int arm = 0; arm < 2; arm++) {
                    for (int seg = -10; seg <= 10; seg++) {
                        Location beamLoc;
                        if (arm == 0) {
                            beamLoc = center.clone().add(seg, 1.5, 0);
                        } else {
                            beamLoc = center.clone().add(0, 1.5, seg);
                        }
                        BlockDisplayHandle h = displayBuilder.spawnBlock(beamLoc, Material.SEA_LANTERN);
                        h.scale(1.0f, 2.5f, 0.4f).glow(240, 240, 255).interpolation(1, 0);
                        crossHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.5f, 0.5f);
            }
            // Cross rotates (30-190 ticks = 8 sec)
            else if (crossFormed && ticksAlive < 190) {
                float rotAngle = (ticksAlive - 30) * 0.013f; // ~15 deg/sec
                int totalPerArm = crossHandles.size() / 2;
                for (int arm = 0; arm < 2; arm++) {
                    double armBaseAngle = arm * (Math.PI / 2) + rotAngle;
                    for (int i = 0; i < totalPerArm; i++) {
                        int idx = arm * totalPerArm + i;
                        if (idx >= crossHandles.size()) break;
                        double dist = (i - totalPerArm / 2);
                        double bx = Math.cos(armBaseAngle) * dist;
                        double bz = Math.sin(armBaseAngle) * dist;
                        crossHandles.get(idx).entity().teleport(
                            center.clone().add(bx, 1.5, bz));
                    }
                }
                if (ticksAlive % 12 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 1.5, 0), 10, 3.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BlindingCross(plugin); }
    }

    // ================================================================
    // 65. LASER RAIN - 20 thin beams fall from sky, 4 waves
    // Random distribution biased toward player-dense areas
    // ================================================================
    public static class LaserRain extends BossAttack {
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private int wavesSpawned = 0;

        public LaserRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("laser_rain", AttackType.BOSS, 2), "dog");
            config.setDamage(14.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(480);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Sky fills with light points
            for (int i = 0; i < 20; i++) {
                double x = (Math.random() - 0.5) * 24;
                double z = (Math.random() - 0.5) * 24;
                Location starLoc = center.clone().add(x, 18 + Math.random() * 4, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(starLoc, Material.END_ROD);
                h.scale(0.15f, 0.15f, 0.15f).glow(240, 240, 255).interpolation(2, 0);
                beamHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph: sky stars visible (0-20 ticks)
            if (ticksAlive < 20) {
                if (ticksAlive % 5 == 0) {
                    for (int i = 0; i < beamHandles.size(); i += 5) {
                        DisplayBuilder.cyanDust(beamHandles.get(i).entity().getLocation(), 2, 0.3);
                    }
                }
            }
            // Wave spawns every 50 ticks (4 waves of 20 beams)
            int waveIdx = (ticksAlive - 20) / 50;
            if (ticksAlive >= 20 && waveIdx >= 0 && waveIdx < 4 && wavesSpawned <= waveIdx
                    && (ticksAlive - 20) % 50 == 0) {
                wavesSpawned = waveIdx + 1;

                // Remove old star indicators for new wave
                for (BlockDisplayHandle h : beamHandles) h.entity().remove();
                beamHandles.clear();

                // 20 new beams drop at random positions
                for (int i = 0; i < 20; i++) {
                    double x = (Math.random() - 0.5) * 24;
                    double z = (Math.random() - 0.5) * 24;
                    // Beam column from sky to ground
                    for (int y = 0; y < 15; y++) {
                        Location beamLoc = center.clone().add(x, y, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(beamLoc, Material.END_ROD);
                        h.scale(0.15f, 1.0f, 0.15f).glow(240, 240, 255).interpolation(1, 0);
                        beamHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.5f);
            }
            // Beams persist 40 ticks then clear (before next wave)
            if (ticksAlive >= 20 && (ticksAlive - 20) % 50 == 40 && !beamHandles.isEmpty()) {
                for (BlockDisplayHandle h : beamHandles) h.entity().remove();
                beamHandles.clear();
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LaserRain(plugin); }
    }

    // ================================================================
    // 66. RETRIBUTION ARC - Wide sweeping beam targeting highest DPS
    // 5-block-wide beam sweeps 90 degrees in 1.5 sec
    // ================================================================
    public static class RetributionArc extends BossAttack {
        private final List<BlockDisplayHandle> arcHandles = new ArrayList<>();
        private boolean sweepActive = false;
        private double sweepBaseAngle = 0;

        public RetributionArc(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("retribution_arc", AttackType.BOSS, 2), "dog");
            config.setDamage(14.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            sweepBaseAngle = Math.random() * Math.PI * 2;
            // Ground arc indicator
            for (int i = 0; i < 8; i++) {
                double angle = sweepBaseAngle - Math.PI / 4 + (Math.PI / 2 / 8) * i;
                for (int r = 3; r <= 15; r += 4) {
                    Location arcLoc = center.clone().add(Math.cos(angle) * r, 0.05, Math.sin(angle) * r);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(arcLoc, Material.SEA_LANTERN);
                    h.scale(0.3f, 0.05f, 0.3f).glow(240, 240, 255).interpolation(2, 0);
                    arcHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph: arc indicator visible (0-20 ticks)
            if (ticksAlive < 20 && !sweepActive) {
                if (ticksAlive % 5 == 0) {
                    double midAngle = sweepBaseAngle;
                    DisplayBuilder.cyanDust(
                        center.clone().add(Math.cos(midAngle) * 8, 1.5, Math.sin(midAngle) * 8), 8, 2.0);
                }
            }
            // Sweep fires (tick 20)
            else if (ticksAlive == 20 && !sweepActive) {
                sweepActive = true;
                // Remove arc indicators
                for (BlockDisplayHandle h : arcHandles) h.entity().remove();
                arcHandles.clear();
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.5f, 0.4f);
            }
            // Sweep in progress (20-50 ticks = 1.5 sec)
            else if (sweepActive && ticksAlive >= 20 && ticksAlive < 50) {
                float progress = (ticksAlive - 20) / 30.0f;
                double currentAngle = sweepBaseAngle - Math.PI / 4 + progress * (Math.PI / 2);

                // Clear and rebuild sweep beam at current angle
                for (BlockDisplayHandle h : arcHandles) h.entity().remove();
                arcHandles.clear();

                for (int r = 1; r <= 18; r++) {
                    Location beamLoc = center.clone().add(
                        Math.cos(currentAngle) * r, 1.5, Math.sin(currentAngle) * r);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(beamLoc, Material.SEA_LANTERN);
                    float width = 2.0f + (r / 18.0f) * 3.0f;
                    h.scale(width, 2.0f, 0.5f).glow(240, 240, 255).interpolation(1, 0);
                    arcHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(
                        center.clone().add(Math.cos(currentAngle) * 10, 1.5, Math.sin(currentAngle) * 10), 12, 3.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RetributionArc(plugin); }
    }

    // ================================================================
    // 67. PINPOINT CONVERGENCE - 6 beams from different segments
    // All converge on single point, explosion on intersection
    // ================================================================
    public static class PinpointConvergence extends BossAttack {
        private final List<BlockDisplayHandle> sourceHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private boolean converged = false;
        private boolean exploded = false;

        public PinpointConvergence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pinpoint_convergence", AttackType.BOSS, 2), "dog");
            config.setDamage(16.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 6 source segments in a circle at range
            for (int i = 0; i < 6; i++) {
                double a = (Math.PI * 2 / 6) * i;
                Location loc = center.clone().add(Math.cos(a) * 14, 4, Math.sin(a) * 14);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.8f, 0.8f, 0.8f).glow(0, 200, 255).interpolation(3, 0);
                sourceHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.2f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Sources light up one by one (0-30 ticks = 1.5 sec)
            if (ticksAlive < 30 && !converged) {
                int segToGlow = ticksAlive / 5;
                if (segToGlow < sourceHandles.size() && ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(sourceHandles.get(segToGlow).entity().getLocation(), 10, 1.0);
                    DisplayBuilder.playSound(sourceHandles.get(segToGlow).entity().getLocation(),
                        Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.8f + segToGlow * 0.2f);
                }
            }
            // Beams converge (tick 30)
            else if (ticksAlive == 30 && !converged) {
                converged = true;
                Location target = center.clone().add(0, 1.5, 0);
                for (int i = 0; i < 6; i++) {
                    double a = (Math.PI * 2 / 6) * i;
                    Location source = center.clone().add(Math.cos(a) * 14, 4, Math.sin(a) * 14);
                    // Beam from source to center
                    for (int seg = 0; seg < 14; seg++) {
                        double frac = seg / 14.0;
                        Location beamLoc = source.clone().add(
                            (target.getX() - source.getX()) * frac,
                            (target.getY() - source.getY()) * frac,
                            (target.getZ() - source.getZ()) * frac);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(beamLoc, Material.END_ROD);
                        h.scale(0.25f, 0.25f, 1.0f).glow(240, 240, 255).interpolation(1, 0);
                        beamHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.5f, 0.6f);
            }
            // Convergence explosion (tick 50)
            else if (converged && ticksAlive == 50 && !exploded) {
                exploded = true;
                // Remove beams
                for (BlockDisplayHandle h : beamHandles) h.entity().remove();
                beamHandles.clear();

                // Explosion sphere
                for (int i = 0; i < 20; i++) {
                    double yaw = (Math.PI * 2 / 20) * i;
                    double pitch = Math.PI * (i % 4) / 4.0 - Math.PI / 4;
                    for (int r = 1; r <= 5; r++) {
                        DisplayBuilder.cyanDust(
                            center.clone().add(
                                Math.cos(yaw) * Math.cos(pitch) * r,
                                1.5 + Math.sin(pitch) * r,
                                Math.sin(yaw) * Math.cos(pitch) * r), 4, 0.5);
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PinpointConvergence(plugin); }
    }

    // ================================================================
    // 68. SPIRAL LASER - Beam spirals outward from arena center
    // Two counter-rotating spirals, 2-block to 20-block radius over 8 sec
    // ================================================================
    public static class SpiralLaser extends BossAttack {
        private final List<BlockDisplayHandle> spiralHandles = new ArrayList<>();
        private boolean spiralActive = false;
        private float spiralRadius = 2.0f;

        public SpiralLaser(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spiral_laser", AttackType.BOSS, 2), "dog");
            config.setDamage(12.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(360);
            config.setCooldownTicks(520);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Central origin sphere
            for (int i = 0; i < 6; i++) {
                double a = (Math.PI * 2 / 6) * i;
                Location loc = center.clone().add(Math.cos(a) * 0.8, 1.5, Math.sin(a) * 0.8);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                h.scale(0.4f, 0.4f, 0.4f).glow(240, 240, 255).interpolation(3, 0);
                spiralHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Central glow telegraph (0-40 ticks = 2 sec)
            if (ticksAlive < 40 && !spiralActive) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 1.5, 0), 10, 1.5);
                }
                // Pulse the central sphere
                float pulse = (float) Math.sin(ticksAlive * 0.3) * 0.15f + 0.4f;
                for (int i = 0; i < spiralHandles.size(); i++) {
                    double a = (Math.PI * 2 / spiralHandles.size()) * i;
                    spiralHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(a) * 0.8, 1.5, Math.sin(a) * 0.8));
                }
            }
            // Spiral initiates (tick 40)
            else if (ticksAlive == 40 && !spiralActive) {
                spiralActive = true;
                spiralRadius = 2.0f;
                // Remove central sphere
                for (BlockDisplayHandle h : spiralHandles) h.entity().remove();
                spiralHandles.clear();
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.5f, 0.4f);
            }
            // Spiral expands (40-200 ticks = 8 sec)
            else if (spiralActive && ticksAlive < 200) {
                spiralRadius = 2.0f + ((ticksAlive - 40) / 160.0f) * 18.0f;
                float rotAngle = (ticksAlive - 40) * 0.06f;

                // Clear old spiral blocks
                for (BlockDisplayHandle h : spiralHandles) h.entity().remove();
                spiralHandles.clear();

                // Two counter-rotating arms
                for (int arm = 0; arm < 2; arm++) {
                    double armAngle = rotAngle + arm * Math.PI;
                    // Beam segments along the spiral arm
                    for (int seg = 0; seg < 12; seg++) {
                        double segR = spiralRadius * (seg / 12.0);
                        if (segR < 1) segR = 1;
                        double spiralA = armAngle + seg * 0.15;
                        Location segLoc = center.clone().add(
                            Math.cos(spiralA) * segR, 1.5, Math.sin(spiralA) * segR);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.SEA_LANTERN);
                        h.scale(0.4f, 1.5f, 0.4f).glow(240, 240, 255).interpolation(1, 0);
                        spiralHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 1.5, 0), 6, spiralRadius * 0.5);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpiralLaser(plugin); }
    }

    // ================================================================
    // 69. WHITE OUT PULSE - Arena-wide light pulse with hidden beams
    // Full whiteout for 1.5 sec; 10 beams fire during blindness
    // ================================================================
    public static class WhiteOutPulse extends BossAttack {
        private final List<BlockDisplayHandle> flashHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private boolean whiteoutTriggered = false;
        private boolean beamsFired = false;

        public WhiteOutPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("white_out_pulse", AttackType.BOSS, 2), "dog");
            config.setDamage(14.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(480);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Brightness escalation (0-10 ticks = 0.5 sec)
            if (ticksAlive < 10 && !whiteoutTriggered) {
                float intensity = ticksAlive / 10.0f;
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 0),
                        (int)(15 * intensity + 5), 8.0 * intensity + 2);
                }
            }
            // Whiteout (tick 10): arena-covering flash blocks
            else if (ticksAlive == 10 && !whiteoutTriggered) {
                whiteoutTriggered = true;
                // Dense grid of white blocks to simulate whiteout
                for (int x = -10; x <= 10; x += 4) {
                    for (int z = -10; z <= 10; z += 4) {
                        Location loc = center.clone().add(x, 5, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                        h.scale(4.0f, 0.3f, 4.0f).glow(240, 240, 255).interpolation(2, 0);
                        flashHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 2.0f);
            }
            // Hidden beams fire during whiteout (tick 15)
            else if (ticksAlive == 15 && !beamsFired) {
                beamsFired = true;
                for (int beam = 0; beam < 10; beam++) {
                    double angle = Math.random() * Math.PI * 2;
                    for (int seg = 0; seg < 15; seg++) {
                        Location beamLoc = center.clone().add(
                            Math.cos(angle) * seg, 1.5, Math.sin(angle) * seg);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(beamLoc, Material.END_ROD);
                        h.scale(0.2f, 0.2f, 1.0f).glow(240, 240, 255).interpolation(1, 0);
                        beamHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }
            // Whiteout fades (tick 40): remove flash, beams persist 20 more ticks
            else if (ticksAlive == 40) {
                for (BlockDisplayHandle h : flashHandles) h.entity().remove();
                flashHandles.clear();
            }
            // Beams visible after whiteout then fade (tick 60)
            else if (ticksAlive == 60) {
                for (BlockDisplayHandle h : beamHandles) h.entity().remove();
                beamHandles.clear();
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WhiteOutPulse(plugin); }
    }

    // ================================================================
    // 70. SUPERBEAM - Massive 8-block-wide column from DoG's mouth
    // Slow 20-deg/sec tracking, 3-second sustained fire
    // ================================================================
    public static class Superbeam extends BossAttack {
        private final List<BlockDisplayHandle> jawHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private boolean beamActive = false;
        private double beamAngle = 0;

        public Superbeam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("superbeam", AttackType.BOSS, 2), "dog");
            config.setDamage(14.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(360);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            beamAngle = Math.random() * Math.PI * 2;
            // Jaw opening visual
            Location jawLoc = center.clone().add(0, 3, 0);
            for (int i = 0; i < 4; i++) {
                double a = (Math.PI * 2 / 4) * i;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                    jawLoc.clone().add(Math.cos(a) * 1, 0, Math.sin(a) * 1), Material.AMETHYST_BLOCK);
                h.scale(1.5f, 1.5f, 1.5f).glow(0, 200, 255).interpolation(4, 0);
                jawHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Jaw opening (0-40 ticks = 2 sec)
            if (ticksAlive < 40 && !beamActive) {
                float openAmount = ticksAlive / 40.0f;
                for (int i = 0; i < jawHandles.size(); i++) {
                    double a = (Math.PI * 2 / jawHandles.size()) * i;
                    double r = 1 + openAmount * 3;
                    jawHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(a) * r, 3, Math.sin(a) * r));
                }
                // Precursor beam growing
                if (ticksAlive % 5 == 0) {
                    double precursorWidth = 2 + openAmount * 6;
                    DisplayBuilder.cyanDust(center.clone().add(
                        Math.cos(beamAngle) * 3, 2.5, Math.sin(beamAngle) * 3),
                        (int)(precursorWidth * 2), precursorWidth * 0.5);
                }
            }
            // Beam fires (tick 40)
            else if (ticksAlive == 40 && !beamActive) {
                beamActive = true;
                // Remove jaw
                for (BlockDisplayHandle h : jawHandles) h.entity().remove();
                jawHandles.clear();
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 2.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.3f);
            }
            // Beam sustained with slow tracking (40-100 ticks = 3 sec)
            else if (beamActive && ticksAlive >= 40 && ticksAlive < 100) {
                beamAngle += 0.017; // ~20 deg/sec
                // Rebuild beam at current angle
                for (BlockDisplayHandle h : beamHandles) h.entity().remove();
                beamHandles.clear();

                for (int seg = 0; seg < 20; seg++) {
                    Location beamLoc = center.clone().add(
                        Math.cos(beamAngle) * seg * 1.5, 2, Math.sin(beamAngle) * seg * 1.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(beamLoc, Material.SEA_LANTERN);
                    h.scale(8.0f, 4.0f, 1.5f).glow(240, 240, 255).interpolation(1, 0);
                    beamHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(
                        Math.cos(beamAngle) * 15, 2, Math.sin(beamAngle) * 15), 20, 5.0);
                }
            }
            // Beam ends (tick 100)
            else if (ticksAlive == 100) {
                for (BlockDisplayHandle h : beamHandles) h.entity().remove();
                beamHandles.clear();
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Superbeam(plugin); }
    }
}
