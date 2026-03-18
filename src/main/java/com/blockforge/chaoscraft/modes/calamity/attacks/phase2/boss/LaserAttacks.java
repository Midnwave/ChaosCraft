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
 * Phase 2 Boss Attacks — GROUP 3: LASER ATTACKS (#21-30)
 * Beam sweeps, multi-beam fans, sustained rays from DoG's eyes and mouth.
 * Crystalline laser beams rendered with END_ROD and SEA_LANTERN displays.
 * NO status effects — damage only.
 */
public final class LaserAttacks {

    private LaserAttacks() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new EyeBeam(plugin));
        registry.register(new MouthSweepBeam(plugin));
        registry.register(new FanBeamBurst(plugin));
        registry.register(new GroundScarLaser(plugin));
        registry.register(new ConvergenceBeam(plugin));
        registry.register(new SustainedPressureRay(plugin));
        registry.register(new HelixLaserRing(plugin));
        registry.register(new StrobeLaserBurst(plugin));
        registry.register(new LaserCage(plugin));
        registry.register(new OrbitalLaserSweep(plugin));
    }

    // ================================================================
    // 21. EYE BEAM — Dual tracking beams from DoG's eyes
    // ================================================================
    public static class EyeBeam extends BossAttack {
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private boolean beamsActive = false;
        private int beamStartTick = 0;

        public EyeBeam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_eye_beam", AttackType.BOSS, 2), "dog");
            config.setDamage(8.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Eye socket glow — two bright points
            for (int eye = 0; eye < 2; eye++) {
                Location eyeLoc = center.clone().add(-10, 10, eye == 0 ? -1 : 1);
                BlockDisplayHandle glow = displayBuilder.spawnBlock(eyeLoc, Material.SEA_LANTERN);
                glow.scale(0.5f, 0.5f, 0.5f).glow(0, 200, 255).interpolation(3, 0);
                beamHandles.add(glow);
                spawnedEntities.add(glow.entity());
            }
            DisplayBuilder.cyanDust(center.clone().add(-10, 10, 0), 12, 2.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Eye glow pulsing telegraph (0-30 ticks)
            if (ticksAlive < 30 && !beamsActive) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(-10, 10, 0), 8, 1.5);
                }
                // Pulse the eye glows
                if (ticksAlive == 20) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
                }
            }
            // Beams fire (tick 30) — sustained for 60 ticks (3 seconds)
            else if (ticksAlive == 30 && !beamsActive) {
                beamsActive = true;
                beamStartTick = ticksAlive;
                // Create beam line segments from each eye toward center
                for (int eye = 0; eye < 2; eye++) {
                    float eyeZ = eye == 0 ? -1 : 1;
                    for (int i = 0; i < 20; i++) {
                        Location beamPt = center.clone().add(-10 + i * 1.2, 10 - i * 0.4, eyeZ);
                        BlockDisplayHandle seg = displayBuilder.spawnBlock(beamPt, Material.END_ROD);
                        seg.scale(0.15f, 0.15f, 0.6f).glow(0, 200, 255).interpolation(1, 0);
                        beamHandles.add(seg);
                        spawnedEntities.add(seg.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 2.0f, 1.0f);
            }
            // Beams track — convergence point follows slightly
            else if (beamsActive && ticksAlive - beamStartTick < 60) {
                int beamTick = ticksAlive - beamStartTick;
                float trackOffset = (float) Math.sin(beamTick * 0.05) * 3;
                // Update beam positions with slight tracking movement
                for (int seg = 2; seg < beamHandles.size(); seg++) {
                    int eye = (seg - 2) / 20;
                    int idx = (seg - 2) % 20;
                    float eyeZ = eye == 0 ? -1 : 1;
                    float blendZ = eyeZ + (trackOffset - eyeZ) * (idx / 20.0f);
                    beamHandles.get(seg).entity().teleport(
                        center.clone().add(-10 + idx * 1.2, 10 - idx * 0.4, blendZ));
                }
                // Convergence point burst
                if (beamTick % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(14, 2, trackOffset), 6, 1.0);
                }
            }
            // Beams dissipate
            else if (beamsActive && ticksAlive - beamStartTick >= 60) {
                beamsActive = false;
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EyeBeam(plugin); }
    }

    // ================================================================
    // 22. MOUTH SWEEP BEAM — Broad rotating beam sweeping 360 degrees
    // ================================================================
    public static class MouthSweepBeam extends BossAttack {
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private boolean beamActive = false;
        private int sweepStartTick = 0;

        public MouthSweepBeam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_mouth_sweep_beam", AttackType.BOSS, 2), "dog");
            config.setDamage(10.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Mouth aura building up
            BlockDisplayHandle mouthGlow = displayBuilder.spawnBlock(
                center.clone().add(0, 8, 0), Material.SEA_LANTERN);
            mouthGlow.scale(2.5f, 2.0f, 2.5f).glow(0, 200, 255).interpolation(4, 0);
            beamHandles.add(mouthGlow);
            spawnedEntities.add(mouthGlow.entity());
            DisplayBuilder.cyanDust(center.clone().add(0, 8, 0), 15, 3.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Charge-up with slow rotation start (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !beamActive) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 8, 0), 10, 3.0);
                }
                if (ticksAlive == 30) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.6f);
                }
            }
            // Beam fires (tick 40) — sweeps 180 degrees, then reverses
            else if (ticksAlive == 40 && !beamActive) {
                beamActive = true;
                sweepStartTick = ticksAlive;
                // Create thick beam segments
                for (int i = 0; i < 16; i++) {
                    Location beamPt = center.clone().add(i * 1.2, 3, 0);
                    BlockDisplayHandle seg = displayBuilder.spawnBlock(beamPt, Material.SEA_LANTERN);
                    seg.scale(0.8f, 0.6f, 1.5f).glow(0, 200, 255).interpolation(1, 0);
                    beamHandles.add(seg);
                    spawnedEntities.add(seg.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 2.0f, 0.6f);
            }
            // Beam sweeps (40-160 ticks = 6 seconds, two 180-degree passes)
            else if (beamActive) {
                int sweepTick = ticksAlive - sweepStartTick;
                if (sweepTick > 120) return;

                // First pass 0-60 ticks, second pass 60-120 ticks (reversed)
                double sweepAngle;
                if (sweepTick < 60) {
                    sweepAngle = (sweepTick / 60.0) * Math.PI;
                } else {
                    sweepAngle = Math.PI - ((sweepTick - 60) / 60.0) * Math.PI;
                }

                // Update beam segment positions
                for (int i = 1; i < beamHandles.size(); i++) {
                    int idx = i - 1;
                    double dist = idx * 1.2;
                    double x = Math.cos(sweepAngle) * dist;
                    double z = Math.sin(sweepAngle) * dist;
                    beamHandles.get(i).entity().teleport(center.clone().add(x, 3, z));
                }

                if (sweepTick % 5 == 0) {
                    double tipDist = 16 * 1.2;
                    DisplayBuilder.cyanDust(
                        center.clone().add(Math.cos(sweepAngle) * tipDist, 3, Math.sin(sweepAngle) * tipDist),
                        6, 2.0);
                }
                if (sweepTick == 60) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 1.0f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MouthSweepBeam(plugin); }
    }

    // ================================================================
    // 23. FAN BEAM BURST — 5-beam fan pattern, 3 rounds
    // ================================================================
    public static class FanBeamBurst extends BossAttack {
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private int roundsFired = 0;
        private int lastRoundTick = 0;

        public FanBeamBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_fan_beam_burst", AttackType.BOSS, 2), "dog");
            config.setDamage(10.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(350);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 5 distinct glow nodes on snout
            for (int i = 0; i < 5; i++) {
                double angle = Math.toRadians(-30 + i * 15);
                Location nodeLoc = center.clone().add(
                    -10 + Math.cos(angle) * 0.8, 8 + Math.sin(angle) * 0.8, 0);
                BlockDisplayHandle node = displayBuilder.spawnBlock(nodeLoc, Material.SEA_LANTERN);
                node.scale(0.4f, 0.4f, 0.4f).glow(0, 200, 255).interpolation(3, 0);
                beamHandles.add(node);
                spawnedEntities.add(node.entity());
            }
            DisplayBuilder.cyanDust(center.clone().add(-10, 8, 0), 15, 2.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.2f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-24 ticks = 1.2 seconds)
            if (ticksAlive < 24) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(-10, 8, 0), 8, 2.0);
                }
                return;
            }

            // Fire 3 rounds of fan beams, 20 ticks apart, rotating 30 degrees each
            if (roundsFired < 3 && ticksAlive - lastRoundTick >= 20) {
                lastRoundTick = ticksAlive;
                roundsFired++;

                // Clear old beam segments
                for (int i = beamHandles.size() - 1; i >= 5; i--) {
                    beamHandles.get(i).entity().remove();
                    beamHandles.remove(i);
                }

                double baseAngle = Math.toRadians((roundsFired - 1) * 30);
                Location origin = center.clone().add(-10, 8, 0);

                // 5 beams in 60-degree arc (15 degrees apart)
                for (int beam = 0; beam < 5; beam++) {
                    double beamAngle = baseAngle + Math.toRadians(-30 + beam * 15);
                    for (int seg = 0; seg < 18; seg++) {
                        double dist = seg * 1.0;
                        Location segLoc = origin.clone().add(
                            Math.cos(beamAngle) * dist + 10,
                            -seg * 0.3,
                            Math.sin(beamAngle) * dist);
                        BlockDisplayHandle s = displayBuilder.spawnBlock(segLoc, Material.END_ROD);
                        s.scale(0.12f, 0.12f, 0.6f).glow(0, 200, 255).interpolation(1, 0);
                        beamHandles.add(s);
                        spawnedEntities.add(s.entity());
                    }
                }
                DisplayBuilder.cyanDust(origin, 20, 8.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.5f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FanBeamBurst(plugin); }
    }

    // ================================================================
    // 24. GROUND SCAR LASER — Persistent ground-trace hazard lines
    // ================================================================
    public static class GroundScarLaser extends BossAttack {
        private final List<BlockDisplayHandle> scarHandles = new ArrayList<>();
        private boolean laserActive = false;
        private int scarLength = 0;

        public GroundScarLaser(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_ground_scar_laser", AttackType.BOSS, 2), "dog");
            config.setDamage(8.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(500);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Head angled downward — nose pointing at ground
            BlockDisplayHandle headGlow = displayBuilder.spawnBlock(
                center.clone().add(-12, 6, 0), Material.SEA_LANTERN);
            headGlow.scale(1.2f, 0.8f, 1.2f).glow(0, 200, 255).interpolation(3, 0);
            scarHandles.add(headGlow);
            spawnedEntities.add(headGlow.entity());
            // Faint ground-level trace
            DisplayBuilder.cyanDust(center.clone().add(-12, 0.2, 0), 10, 8.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-16 ticks)
            if (ticksAlive < 16 && !laserActive) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(-12, 0.2, 0), 5, 6.0);
                }
            }
            // Laser fires and drags along ground (tick 16+)
            else if (ticksAlive >= 16) {
                if (!laserActive) {
                    laserActive = true;
                    DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.5f, 0.8f);
                }

                // Add scar segment every 4 ticks while moving forward
                if (ticksAlive % 4 == 0 && scarLength < 25) {
                    scarLength++;
                    float scarX = -12 + scarLength * 1.2f;
                    Location scarLoc = center.clone().add(scarX, 0.08, 0);

                    // Persistent ground scar — glowing line segment
                    BlockDisplayHandle scar = displayBuilder.spawnBlock(scarLoc, Material.SEA_LANTERN);
                    scar.scale(0.8f, 0.08f, 0.4f).glow(0, 200, 255).interpolation(2, 0);
                    scarHandles.add(scar);
                    spawnedEntities.add(scar.entity());

                    // Active beam from head to ground contact
                    DisplayBuilder.cyanDust(scarLoc.clone().add(0, 3, 0), 4, 1.0);
                    if (scarLength % 3 == 0) {
                        DisplayBuilder.playSound(scarLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.2f);
                    }
                }

                // Move head glow along with beam
                float headX = -12 + scarLength * 1.2f;
                scarHandles.get(0).entity().teleport(center.clone().add(headX, 6, 0));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GroundScarLaser(plugin); }
    }

    // ================================================================
    // 25. CONVERGENCE BEAM — 4 beams from body segments converge on point
    // ================================================================
    public static class ConvergenceBeam extends BossAttack {
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private boolean converged = false;

        public ConvergenceBeam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_convergence_beam", AttackType.BOSS, 2), "dog");
            config.setDamage(12.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 4 firing segments glow independently
            double[][] origins = {{-10, 10, -5}, {-10, 10, 5}, {8, 8, -6}, {8, 8, 6}};
            for (double[] o : origins) {
                BlockDisplayHandle glow = displayBuilder.spawnBlock(
                    center.clone().add(o[0], o[1], o[2]), Material.SEA_LANTERN);
                glow.scale(0.8f, 0.8f, 0.8f).glow(0, 200, 255).interpolation(3, 0);
                beamHandles.add(glow);
                spawnedEntities.add(glow.entity());
            }
            // Focal point marker
            BlockDisplayHandle focal = displayBuilder.spawnBlock(center.clone().add(0, 3, 0),
                Material.AMETHYST_CLUSTER);
            focal.scale(0.5f, 0.5f, 0.5f).glow(128, 0, 255).interpolation(4, 0);
            beamHandles.add(focal);
            spawnedEntities.add(focal.entity());
            DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 10, 1.5);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Segment glows pulse (0-30 ticks)
            if (ticksAlive < 30 && !converged) {
                if (ticksAlive % 8 == 0) {
                    for (int i = 0; i < 4; i++) {
                        DisplayBuilder.cyanDust(beamHandles.get(i).entity().getLocation(), 4, 0.8);
                    }
                }
                // Focal point pulses
                if (ticksAlive == 20) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 8, 1.0);
                }
            }
            // Beams fire simultaneously (tick 30)
            else if (ticksAlive == 30 && !converged) {
                converged = true;
                Location focalPoint = center.clone().add(0, 3, 0);
                double[][] origins = {{-10, 10, -5}, {-10, 10, 5}, {8, 8, -6}, {8, 8, 6}};

                // Create beam lines from each origin to focal point
                for (double[] o : origins) {
                    Location origin = center.clone().add(o[0], o[1], o[2]);
                    double dx = focalPoint.getX() - origin.getX();
                    double dy = focalPoint.getY() - origin.getY();
                    double dz = focalPoint.getZ() - origin.getZ();
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    int segs = (int) (dist / 0.8);

                    for (int s = 0; s < segs; s++) {
                        float t = s / (float) segs;
                        Location segLoc = origin.clone().add(dx * t, dy * t, dz * t);
                        BlockDisplayHandle seg = displayBuilder.spawnBlock(segLoc, Material.END_ROD);
                        seg.scale(0.12f, 0.12f, 0.5f).glow(0, 200, 255).interpolation(1, 0);
                        beamHandles.add(seg);
                        spawnedEntities.add(seg.entity());
                    }
                }

                // Convergence explosion at focal point
                DisplayBuilder.cyanDust(focalPoint, 40, 5.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.0f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 2.0f, 0.8f);
            }
            // Beams linger briefly then fade (30-40 ticks)
            else if (converged && ticksAlive == 40) {
                // Remove beam segments
                for (int i = beamHandles.size() - 1; i >= 5; i--) {
                    beamHandles.get(i).entity().remove();
                    beamHandles.remove(i);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ConvergenceBeam(plugin); }
    }

    // ================================================================
    // 26. SUSTAINED PRESSURE RAY — 5-block-wide static beam, 4 seconds
    // ================================================================
    public static class SustainedPressureRay extends BossAttack {
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private boolean beamFired = false;
        private int fireStartTick = 0;

        public SustainedPressureRay(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_sustained_pressure_ray", AttackType.BOSS, 2), "dog");
            config.setDamage(12.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // DoG stops circling — rare tell. Entire mouth blazes white
            BlockDisplayHandle mouthBlaze = displayBuilder.spawnBlock(
                center.clone().add(-12, 8, 0), Material.SEA_LANTERN);
            mouthBlaze.scale(3.0f, 3.0f, 3.0f).glow(0, 200, 255).interpolation(5, 0);
            beamHandles.add(mouthBlaze);
            spawnedEntities.add(mouthBlaze.entity());
            DisplayBuilder.cyanDust(center.clone().add(-12, 8, 0), 20, 4.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Long charge-up: DoG stops circling (0-50 ticks = 2.5 seconds)
            if (ticksAlive < 50 && !beamFired) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(-12, 8, 0), 12, 4.0);
                }
            }
            // Beam fires (tick 50) — 5-block-wide static column, 80 ticks (4 seconds)
            else if (ticksAlive == 50 && !beamFired) {
                beamFired = true;
                fireStartTick = ticksAlive;
                // Massive thick beam extending from mouth across arena
                for (int i = 0; i < 24; i++) {
                    Location beamPt = center.clone().add(-12 + i * 1.5, 4, 0);
                    for (int w2 = -2; w2 <= 2; w2++) {
                        BlockDisplayHandle seg = displayBuilder.spawnBlock(
                            beamPt.clone().add(0, 0, w2), Material.SEA_LANTERN);
                        seg.scale(1.0f, 2.0f, 0.8f).glow(0, 200, 255).interpolation(1, 0);
                        beamHandles.add(seg);
                        spawnedEntities.add(seg.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.5f);
            }
            // Beam sustained (50-130 ticks)
            else if (beamFired && ticksAlive - fireStartTick < 80) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 4, 0), 15, 12.0);
                }
            }
            // Beam ends (tick 130)
            else if (beamFired && ticksAlive - fireStartTick >= 80 && ticksAlive - fireStartTick < 85) {
                // Remove beam segments
                for (int i = beamHandles.size() - 1; i >= 1; i--) {
                    beamHandles.get(i).entity().remove();
                    beamHandles.remove(i);
                }
                DisplayBuilder.cyanDust(center, 20, 6.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SustainedPressureRay(plugin); }
    }

    // ================================================================
    // 27. HELIX LASER RING — 8-spoke beam wheel perpendicular to body
    // ================================================================
    public static class HelixLaserRing extends BossAttack {
        private final List<BlockDisplayHandle> spokeHandles = new ArrayList<>();
        private boolean ringActive = false;
        private int ringStartTick = 0;

        public HelixLaserRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_helix_laser_ring", AttackType.BOSS, 2), "dog");
            config.setDamage(8.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Mid-body segment glow ring
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                Location nodeLoc = center.clone().add(Math.cos(angle) * 2, 6 + Math.sin(angle) * 2, 0);
                BlockDisplayHandle node = displayBuilder.spawnBlock(nodeLoc, Material.SEA_LANTERN);
                node.scale(0.4f, 0.4f, 0.4f).glow(0, 200, 255).interpolation(3, 0);
                spokeHandles.add(node);
                spawnedEntities.add(node.entity());
            }
            DisplayBuilder.cyanDust(center.clone().add(0, 6, 0), 12, 3.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Ring formation telegraph (0-30 ticks)
            if (ticksAlive < 30 && !ringActive) {
                if (ticksAlive % 6 == 0) {
                    for (int i = 0; i < spokeHandles.size(); i++) {
                        double angle = (Math.PI * 2 / 8) * i + ticksAlive * 0.05;
                        spokeHandles.get(i).entity().teleport(
                            center.clone().add(Math.cos(angle) * 2, 6 + Math.sin(angle) * 2, 0));
                    }
                }
            }
            // Ring fires (tick 30) — 8 spokes extending 12 blocks
            else if (ticksAlive == 30 && !ringActive) {
                ringActive = true;
                ringStartTick = ticksAlive;
                // Extend spokes outward
                for (int spoke = 0; spoke < 8; spoke++) {
                    double angle = (Math.PI * 2 / 8) * spoke;
                    for (int seg = 1; seg <= 12; seg++) {
                        Location segLoc = center.clone().add(
                            Math.cos(angle) * seg, 6 + Math.sin(angle) * seg, 0);
                        BlockDisplayHandle s = displayBuilder.spawnBlock(segLoc, Material.END_ROD);
                        s.scale(0.1f, 0.1f, 0.4f).glow(0, 200, 255).interpolation(1, 0);
                        spokeHandles.add(s);
                        spawnedEntities.add(s.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.5f, 1.2f);
            }
            // Ring moves with body through arena (30-100 ticks)
            else if (ringActive) {
                int ringTick = ticksAlive - ringStartTick;
                if (ringTick > 70) return;

                float moveX = (ringTick / 70.0f) * 30 - 15;
                float rotation = ringTick * 0.04f;

                // Update spoke positions (rotate ring as it moves)
                for (int spoke = 0; spoke < 8; spoke++) {
                    double baseAngle = (Math.PI * 2 / 8) * spoke + rotation;
                    // Node
                    spokeHandles.get(spoke).entity().teleport(
                        center.clone().add(moveX + Math.cos(baseAngle) * 2, 6 + Math.sin(baseAngle) * 2, 0));
                    // Segments
                    for (int seg = 0; seg < 12; seg++) {
                        int idx = 8 + spoke * 12 + seg;
                        if (idx < spokeHandles.size()) {
                            double dist = (seg + 1);
                            spokeHandles.get(idx).entity().teleport(
                                center.clone().add(
                                    moveX + Math.cos(baseAngle) * dist,
                                    6 + Math.sin(baseAngle) * dist, 0));
                        }
                    }
                }

                if (ringTick % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(moveX, 6, 0), 6, 6.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HelixLaserRing(plugin); }
    }

    // ================================================================
    // 28. STROBE LASER BURST — Rapid-fire strobing pulses in a cone
    // ================================================================
    public static class StrobeLaserBurst extends BossAttack {
        private final List<BlockDisplayHandle> pulseHandles = new ArrayList<>();
        private boolean strobeActive = false;
        private int strobeStartTick = 0;

        public StrobeLaserBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_strobe_laser_burst", AttackType.BOSS, 2), "dog");
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Eye sockets flicker (dimmer pre-fire)
            for (int eye = 0; eye < 2; eye++) {
                Location eyeLoc = center.clone().add(-10, 10, eye == 0 ? -1 : 1);
                BlockDisplayHandle glow = displayBuilder.spawnBlock(eyeLoc, Material.AMETHYST_CLUSTER);
                glow.scale(0.4f, 0.4f, 0.4f).glow(0, 200, 255).interpolation(2, 0);
                pulseHandles.add(glow);
                spawnedEntities.add(glow.entity());
            }
            DisplayBuilder.cyanDust(center.clone().add(-10, 10, 0), 8, 1.5);
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.2f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Pre-fire flicker (0-20 ticks)
            if (ticksAlive < 20 && !strobeActive) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(-10, 10, 0), 4, 1.0);
                }
            }
            // Strobe fires (tick 20) — 60 ticks (3 seconds) of rapid pulses
            else if (ticksAlive == 20 && !strobeActive) {
                strobeActive = true;
                strobeStartTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.5f, 2.0f);
            }
            // Strobing pulses (20-80 ticks)
            else if (strobeActive) {
                int strobeTick = ticksAlive - strobeStartTick;
                if (strobeTick > 60) return;

                // Fire a pulse every 2 ticks (10 Hz) — random within 30-degree cone
                if (strobeTick % 2 == 0) {
                    // Clear previous pulse visuals
                    for (int i = pulseHandles.size() - 1; i >= 2; i--) {
                        pulseHandles.get(i).entity().remove();
                        pulseHandles.remove(i);
                    }

                    // Random angle within cone
                    double angle = Math.toRadians((Math.random() - 0.5) * 30);
                    Location origin = center.clone().add(-10, 10, 0);
                    for (int seg = 0; seg < 15; seg++) {
                        double dist = seg * 1.2;
                        Location pulseLoc = origin.clone().add(
                            dist + 10,
                            -seg * 0.5,
                            Math.sin(angle) * dist);
                        BlockDisplayHandle p = displayBuilder.spawnBlock(pulseLoc, Material.SEA_LANTERN);
                        p.scale(0.08f, 0.08f, 0.4f).glow(0, 200, 255).interpolation(0, 0);
                        pulseHandles.add(p);
                        spawnedEntities.add(p.entity());
                    }

                    if (strobeTick % 10 == 0) {
                        DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 2.0f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StrobeLaserBurst(plugin); }
    }

    // ================================================================
    // 29. LASER CAGE — Hexagonal contracting laser prison
    // ================================================================
    public static class LaserCage extends BossAttack {
        private final List<BlockDisplayHandle> cageHandles = new ArrayList<>();
        private boolean cageActive = false;
        private int cageStartTick = 0;
        private float cageRadius = 6.0f;

        public LaserCage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_laser_cage", AttackType.BOSS, 2), "dog");
            config.setDamage(8.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(500);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dimmed cage frame forming — 6 vertical pillars in hexagon
            for (int pillar = 0; pillar < 6; pillar++) {
                double angle = (Math.PI * 2 / 6) * pillar;
                for (int y = 0; y <= 6; y++) {
                    Location pillarLoc = center.clone().add(
                        Math.cos(angle) * 6, y, Math.sin(angle) * 6);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(pillarLoc, Material.END_ROD);
                    h.scale(0.15f, 0.8f, 0.15f).glow(0, 200, 255).interpolation(3, 0);
                    cageHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            DisplayBuilder.cyanDust(center, 15, 6.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: cage forming (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !cageActive) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center, 8, 6.0);
                }
            }
            // Cage activates (tick 30)
            else if (ticksAlive == 30 && !cageActive) {
                cageActive = true;
                cageStartTick = ticksAlive;
                cageRadius = 6.0f;
                // Beam walls connect pillars — horizontal ring at top and bottom
                for (int ring = 0; ring <= 6; ring += 6) {
                    for (int seg = 0; seg < 6; seg++) {
                        double a1 = (Math.PI * 2 / 6) * seg;
                        double a2 = (Math.PI * 2 / 6) * ((seg + 1) % 6);
                        for (int t = 0; t < 5; t++) {
                            float blend = t / 5.0f;
                            double x = Math.cos(a1) * cageRadius * (1 - blend) + Math.cos(a2) * cageRadius * blend;
                            double z = Math.sin(a1) * cageRadius * (1 - blend) + Math.sin(a2) * cageRadius * blend;
                            Location wallLoc = center.clone().add(x, ring, z);
                            BlockDisplayHandle wall = displayBuilder.spawnBlock(wallLoc, Material.SEA_LANTERN);
                            wall.scale(0.3f, 0.3f, 0.3f).glow(0, 200, 255).interpolation(1, 0);
                            cageHandles.add(wall);
                            spawnedEntities.add(wall.entity());
                        }
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.5f, 1.0f);
            }
            // Cage contracts (30-110 ticks = 4 seconds)
            else if (cageActive) {
                int cageTick = ticksAlive - cageStartTick;
                if (cageTick > 80) return;

                // Contract 0.5 blocks per second = 0.025 per tick
                cageRadius = 6.0f - (cageTick * 0.025f);
                if (cageRadius < 4.0f) cageRadius = 4.0f;

                // Update pillar positions
                for (int pillar = 0; pillar < 6; pillar++) {
                    double angle = (Math.PI * 2 / 6) * pillar;
                    for (int y = 0; y <= 6; y++) {
                        int idx = pillar * 7 + y;
                        if (idx < cageHandles.size()) {
                            cageHandles.get(idx).entity().teleport(
                                center.clone().add(Math.cos(angle) * cageRadius, y, Math.sin(angle) * cageRadius));
                        }
                    }
                }

                if (cageTick % 10 == 0) {
                    DisplayBuilder.cyanDust(center, 6, cageRadius);
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.0f + cageTick * 0.01f);
                }

                // Final crush at minimum radius
                if (cageRadius <= 4.0f && cageTick == 80) {
                    DisplayBuilder.cyanDust(center, 30, 4.0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.0f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LaserCage(plugin); }
    }

    // ================================================================
    // 30. ORBITAL LASER SWEEP — 360-degree sweep while circling overhead
    // ================================================================
    public static class OrbitalLaserSweep extends BossAttack {
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private boolean sweepActive = false;
        private int sweepStartTick = 0;

        public OrbitalLaserSweep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_orbital_laser_sweep", AttackType.BOSS, 2), "dog");
            config.setDamage(10.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(480);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // DoG rises to elevated circle pattern
            BlockDisplayHandle headGlow = displayBuilder.spawnBlock(
                center.clone().add(14, 15, 0), Material.AMETHYST_BLOCK);
            headGlow.scale(1.5f, 1.2f, 1.5f).glow(0, 200, 255).interpolation(3, 0);
            beamHandles.add(headGlow);
            spawnedEntities.add(headGlow.entity());
            DisplayBuilder.cyanDust(center.clone().add(14, 15, 0), 10, 3.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Rising to overhead circle (0-30 ticks)
            if (ticksAlive < 30 && !sweepActive) {
                float orbitAngle = ticksAlive * 0.1f;
                beamHandles.get(0).entity().teleport(
                    center.clone().add(Math.cos(orbitAngle) * 14, 15, Math.sin(orbitAngle) * 14));
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(beamHandles.get(0).entity().getLocation(), 5, 2.0);
                }
            }
            // Sweep begins (tick 30) — beam perpendicular to body
            else if (ticksAlive == 30 && !sweepActive) {
                sweepActive = true;
                sweepStartTick = ticksAlive;
                // Create beam extending from DoG outward
                for (int seg = 0; seg < 20; seg++) {
                    Location segLoc = center.clone().add(seg * 1.0, 2, 0);
                    BlockDisplayHandle s = displayBuilder.spawnBlock(segLoc, Material.SEA_LANTERN);
                    s.scale(0.6f, 0.5f, 1.2f).glow(0, 200, 255).interpolation(1, 0);
                    beamHandles.add(s);
                    spawnedEntities.add(s.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 2.0f, 0.6f);
            }
            // Beam sweeps 360 degrees twice (30-150 ticks per rotation, 2 rotations)
            else if (sweepActive) {
                int sweepTick = ticksAlive - sweepStartTick;
                if (sweepTick > 240) return;

                // Orbit angle — 120 ticks per full rotation
                double sweepAngle;
                if (sweepTick < 120) {
                    sweepAngle = (sweepTick / 120.0) * Math.PI * 2;
                } else {
                    // Reverse direction for second pass
                    sweepAngle = Math.PI * 2 - ((sweepTick - 120) / 120.0) * Math.PI * 2;
                }

                // Update DoG head position on orbit
                beamHandles.get(0).entity().teleport(
                    center.clone().add(Math.cos(sweepAngle) * 14, 15, Math.sin(sweepAngle) * 14));

                // Beam extends from DoG position outward perpendicular to orbit
                double beamAngle = sweepAngle + Math.PI / 2; // Perpendicular
                for (int seg = 1; seg < beamHandles.size(); seg++) {
                    int idx = seg - 1;
                    double dist = idx * 1.0;
                    // Beam points from orbit toward center and beyond
                    double beamX = Math.cos(sweepAngle) * 14 - Math.cos(sweepAngle) * dist;
                    double beamZ = Math.sin(sweepAngle) * 14 - Math.sin(sweepAngle) * dist;
                    double beamY = 15 - (dist / 20.0) * 13; // Descend toward ground
                    if (beamY < 2) beamY = 2;
                    beamHandles.get(seg).entity().teleport(center.clone().add(beamX, beamY, beamZ));
                }

                if (sweepTick % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 8, 10.0);
                }
                if (sweepTick == 120) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 1.0f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new OrbitalLaserSweep(plugin); }
    }
}
