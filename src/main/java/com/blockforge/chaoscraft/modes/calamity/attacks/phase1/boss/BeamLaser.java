package com.blockforge.chaoscraft.modes.calamity.attacks.phase1.boss;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

/**
 * GROUP 5: BEAM / LASER
 * Sustained line attacks, sweeping beams, and death rays.
 * The Voidmaw fires through the island itself.
 * Attacks 41-50 from the Voidmaw boss design document.
 */
public final class BeamLaser {

    private BeamLaser() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CoreBeam(plugin));
        registry.register(new SweepLaser(plugin));
        registry.register(new PillarPierce(plugin));
        registry.register(new EyeBeam(plugin));
        registry.register(new CrossCut(plugin));
        registry.register(new DeathSpiralBeam(plugin));
        registry.register(new VoidRayBurst(plugin));
        registry.register(new ResonanceBeam(plugin));
        registry.register(new TunnelVision(plugin));
        registry.register(new OmegaRay(plugin));
    }

    // -------------------------------------------------------------------------
    // 41. Core Beam
    // A beam of pure void energy shoots straight upward through the island,
    // emerging as a 3-block wide column. Slowly rotates 90 degrees over 4 sec.
    // -------------------------------------------------------------------------
    public static class CoreBeam extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean beamActive = false;

        public CoreBeam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("core_beam", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(130);
            config.setCooldownTicks(400);
            config.setDamageOnImpactOnly(false);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location beamOrigin = center.clone().add(3, 0, 2);

            // Telegraph: surface crackles at beam point
            BlockDisplayHandle surface = displayBuilder.spawnBlock(beamOrigin.clone().add(0, -0.1, 0),
                    Material.PURPLE_STAINED_GLASS);
            surface.scale(3.0f, 0.1f, 3.0f).glow(128, 0, 255).interpolation(3, 0);
            handles.add(surface);
            spawnedEntities.add(surface.entity());

            // Beam column — 3 blocks wide, shoots upward through 20 blocks
            for (int h = 0; h < 20; h++) {
                Location beamLoc = beamOrigin.clone().add(0, h * 1.0, 0);
                BlockDisplayHandle beamSeg = displayBuilder.spawnBlock(beamLoc, Material.OBSIDIAN);
                beamSeg.scale(3.0f, 1.0f, 3.0f).glow(80, 0, 160).interpolation(2, 0);
                handles.add(beamSeg);
                spawnedEntities.add(beamSeg.entity());
            }

            DisplayBuilder.playSound(beamOrigin, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            // Phase 1 (0-50): telegraph — surface crackles, beam hidden below
            if (ticksAlive < 50) {
                float buildUp = ticksAlive / 50f;
                if (ticksAlive % 5 == 0) {
                    Location beamOrigin = center.clone().add(3, 0, 2);
                    DisplayBuilder.purpleDust(beamOrigin.clone().add(0, 0.3, 0), (int)(10 * buildUp) + 2, 1.5);
                }

                // Scale up the surface marker
                BlockDisplay surf = (BlockDisplay) handles.get(0).entity();
                float s = 3.0f * buildUp;
                surf.setTransformation(new Transformation(
                    new Vector3f(-s * 0.5f, -0.05f, -s * 0.5f),
                    new AxisAngle4f(ticksAlive * 0.05f, 0, 1, 0),
                    new Vector3f(s, 0.1f, s),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                surf.setInterpolationDelay(0);
                surf.setInterpolationDuration(3);
                return;
            }

            // Phase 2 (50): beam fires
            if (!beamActive) {
                beamActive = true;
                DisplayBuilder.playSound(center.clone().add(3, 0, 2), Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.3f);
            }

            // Phase 2 (50-130): beam is active and rotates slowly 90 degrees
            if (ticksAlive >= 50 && ticksAlive <= 130) {
                float beamAge = (ticksAlive - 50) / 80f;
                // Rotate origin 90 degrees around island center over duration
                double baseAngle = Math.toRadians(beamAge * 90.0);
                double beamR = 3.6; // distance from center
                double bx = Math.cos(baseAngle) * beamR;
                double bz = Math.sin(baseAngle) * beamR;

                // Reposition all beam segments along the rotated column
                for (int h = 0; h < 20; h++) {
                    int idx = 1 + h;
                    if (idx >= handles.size()) break;
                    Location beamLoc = center.clone().add(bx, h * 1.0, bz);
                    handles.get(idx).entity().teleport(beamLoc);

                    BlockDisplay bd = (BlockDisplay) handles.get(idx).entity();
                    float pulse = 3.0f + (float)Math.sin(ticksAlive * 0.3 + h * 0.2) * 0.3f;
                    bd.setTransformation(new Transformation(
                        new Vector3f(-pulse * 0.5f, -0.5f, -pulse * 0.5f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(pulse, 1.0f, pulse),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(2);
                }

                // Continuous damage along beam column
                if (ticksAlive % 20 == 0) {
                    Location beamBase = center.clone().add(bx, 0, bz);
                    triggerImpactDamage(beamBase);
                    triggerImpactDamage(beamBase.clone().add(0, 5, 0));
                    triggerImpactDamage(beamBase.clone().add(0, 10, 0));
                    DisplayBuilder.purpleDust(beamBase.clone().add(0, 8, 0), 15, 3.0);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new CoreBeam(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 42. Sweep Laser
    // A horizontal beam emerges at waist height from the island edge and
    // sweeps 180 degrees across the surface at a consistent speed.
    // -------------------------------------------------------------------------
    public static class SweepLaser extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean sweepStarted = false;

        public SweepLaser(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sweep_laser", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(110);
            config.setCooldownTicks(440);
            config.setDamageOnImpactOnly(false);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Telegraph: origin point glows at one island edge
            Location originEdge = center.clone().add(-20, 1.5, 0);
            BlockDisplayHandle originGlow = displayBuilder.spawnBlock(originEdge, Material.PURPLE_STAINED_GLASS);
            originGlow.scale(1.5f, 1.5f, 1.5f).glow(128, 0, 255).interpolation(3, 0);
            handles.add(originGlow);
            spawnedEntities.add(originGlow.entity());

            DisplayBuilder.playSound(originEdge, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.4f);
            DisplayBuilder.purpleDust(originEdge, 20, 3.0);

            // Beam segments — horizontal bar extending across the island
            for (int i = 0; i < 20; i++) {
                Location beamSeg = originEdge.clone().add(i * 2.0, 0, 0);
                BlockDisplayHandle seg = displayBuilder.spawnBlock(beamSeg, Material.OBSIDIAN);
                seg.scale(2.0f, 2.0f, 0.3f).glow(128, 0, 255).interpolation(1, 0);
                handles.add(seg);
                spawnedEntities.add(seg.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            // Phase 1 (0-40): telegraph — origin point glows, particle direction hint
            if (ticksAlive < 40) {
                if (ticksAlive % 5 == 0) {
                    Location originEdge = center.clone().add(-20, 1.5, 0);
                    DisplayBuilder.purpleDust(originEdge, 12, 2.0);
                    // Particle flow hints at sweep direction
                    for (int i = 0; i < 5; i++) {
                        DisplayBuilder.purpleDust(originEdge.clone().add(i * 3.0, 0, 0), 3, 0.5);
                    }
                }
                return;
            }

            // Phase 2 (40-100): beam sweeps 180 degrees
            if (!sweepStarted) {
                sweepStarted = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.5f);
            }

            if (ticksAlive >= 40 && ticksAlive <= 100) {
                float sweepProgress = (ticksAlive - 40) / 60f;
                // Sweep from -90 degrees to +90 degrees (180 total)
                double sweepAngle = Math.toRadians(-90 + sweepProgress * 180.0);

                // Orient beam from island center outward at current sweep angle
                for (int i = 0; i < 20; i++) {
                    int idx = 1 + i;
                    if (idx >= handles.size()) break;
                    double r = i * 1.1;
                    double bx = Math.cos(sweepAngle) * r;
                    double bz = Math.sin(sweepAngle) * r;
                    handles.get(idx).entity().teleport(center.clone().add(bx, 1.5, bz));

                    BlockDisplay bd = (BlockDisplay) handles.get(idx).entity();
                    // Beam always points radially outward
                    bd.setTransformation(new Transformation(
                        new Vector3f(-1.0f, -1.0f, -0.15f),
                        new AxisAngle4f((float)sweepAngle, 0, 1, 0),
                        new Vector3f(2.0f, 2.0f, 0.3f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(1);
                }

                // Damage along beam path
                if (ticksAlive % 10 == 0) {
                    for (int i = 2; i <= 18; i += 4) {
                        double r = i * 1.1;
                        Location dmgLoc = center.clone().add(Math.cos(sweepAngle) * r, 1.5, Math.sin(sweepAngle) * r);
                        triggerImpactDamage(dmgLoc);
                    }
                    // Trailing particles
                    for (int i = 0; i < 6; i++) {
                        double r = i * 3.5;
                        Location trailLoc = center.clone().add(Math.cos(sweepAngle) * r, 1.8, Math.sin(sweepAngle) * r);
                        DisplayBuilder.purpleDust(trailLoc, 6, 1.5);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new SweepLaser(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 43. Pillar Pierce
    // Five narrow 1-block wide void beams shoot upward simultaneously in a
    // cross pattern centered on the player cluster.
    // -------------------------------------------------------------------------
    public static class PillarPierce extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean fired = false;

        public PillarPierce(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pillar_pierce", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(12.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(480);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0);
            config.setImpactRadius(1.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Cross pattern: center + 4 cardinal points 8 blocks out
            double[][] beamOffsets = {
                {0, 0},      // center
                {8, 0},      // east
                {-8, 0},     // west
                {0, 8},      // south
                {0, -8}      // north
            };

            DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 1.5f);

            for (double[] offset : beamOffsets) {
                Location beamBase = center.clone().add(offset[0], -0.1, offset[1]);

                // Ground marker
                BlockDisplayHandle marker = displayBuilder.spawnBlock(beamBase, Material.PURPLE_STAINED_GLASS);
                marker.scale(1.0f, 0.1f, 1.0f).glow(128, 0, 255).interpolation(3, 0);
                handles.add(marker);
                spawnedEntities.add(marker.entity());

                // Beam column (hidden below initially)
                for (int h = 0; h < 15; h++) {
                    Location beamLoc = beamBase.clone().add(0, h * 1.0 - 15, 0);
                    BlockDisplayHandle beamSeg = displayBuilder.spawnBlock(beamLoc, Material.OBSIDIAN);
                    beamSeg.scale(1.0f, 1.0f, 1.0f).glow(128, 0, 255).interpolation(1, 0);
                    handles.add(beamSeg);
                    spawnedEntities.add(beamSeg.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            double[][] beamOffsets = {
                {0, 0}, {8, 0}, {-8, 0}, {0, 8}, {0, -8}
            };
            int perBeam = 1 + 15; // 1 marker + 15 segments

            // Phase 1 (0-30): telegraph — markers glow and grow
            if (ticksAlive < 30) {
                float buildUp = ticksAlive / 30f;
                for (int b = 0; b < 5; b++) {
                    int markerIdx = b * perBeam;
                    if (markerIdx >= handles.size()) break;
                    BlockDisplay marker = (BlockDisplay) handles.get(markerIdx).entity();
                    float s = buildUp;
                    marker.setTransformation(new Transformation(
                        new Vector3f(-s * 0.5f, -0.05f, -s * 0.5f),
                        new AxisAngle4f(ticksAlive * 0.1f, 0, 1, 0),
                        new Vector3f(s, 0.1f, s),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    marker.setInterpolationDelay(0);
                    marker.setInterpolationDuration(3);

                    if (ticksAlive % 5 == 0) {
                        Location markerLoc = center.clone().add(beamOffsets[b][0], 0.3, beamOffsets[b][1]);
                        DisplayBuilder.purpleDust(markerLoc, 8, 1.0);
                    }
                }
                return;
            }

            // Phase 2 (30): beams fire simultaneously
            if (!fired && ticksAlive == 30) {
                fired = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.5f);
                DisplayBuilder.purpleDust(center, 30, 6.0);

                for (int b = 0; b < 5; b++) {
                    Location beamBase = center.clone().add(beamOffsets[b][0], 0, beamOffsets[b][1]);
                    for (int h = 0; h < 15; h++) {
                        int idx = b * perBeam + 1 + h;
                        if (idx >= handles.size()) break;
                        handles.get(idx).entity().teleport(beamBase.clone().add(0, h * 1.0, 0));
                    }
                    triggerImpactDamage(beamBase.clone().add(0, 1, 0));
                    DisplayBuilder.purpleDust(beamBase.clone().add(0, 8, 0), 20, 1.0);
                }
            }

            // Phase 3 (30-80): beams sustain then dissipate
            if (fired && ticksAlive > 30) {
                float decayT = (ticksAlive - 30) / 50f;

                for (int b = 0; b < 5; b++) {
                    for (int h = 0; h < 15; h++) {
                        int idx = b * perBeam + 1 + h;
                        if (idx >= handles.size()) break;
                        BlockDisplay bd = (BlockDisplay) handles.get(idx).entity();
                        float scale = 1.0f * (1f - decayT * 0.8f);
                        if (scale < 0.01f) scale = 0.01f;
                        bd.setTransformation(new Transformation(
                            new Vector3f(-scale * 0.5f, -0.5f, -scale * 0.5f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(scale, 1.0f, scale),
                            new AxisAngle4f(0, 0, 1, 0)
                        ));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(4);
                    }
                }

                if (ticksAlive % 15 == 0 && decayT < 0.8f) {
                    for (int b = 0; b < 5; b++) {
                        Location beamBase = center.clone().add(beamOffsets[b][0], 0, beamOffsets[b][1]);
                        DisplayBuilder.purpleDust(beamBase.clone().add(0, 10, 0), 6, 0.8);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new PillarPierce(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 44. Eye Beam
    // A gigantic void eye opens in the island surface and fires a single
    // tracking beam at the highest-health player for 3 seconds.
    // -------------------------------------------------------------------------
    public static class EyeBeam extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean beamFired = false;
        private Location beamTarget;

        public EyeBeam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("eye_beam", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(6.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(120);
            config.setCooldownTicks(500);
            config.setDamageOnImpactOnly(false);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Target position (static approximation — center of arena)
            beamTarget = center.clone().add(0, 2, 0);

            // The eye — 6-block diameter circle on the island surface
            for (int ring = 0; ring < 3; ring++) {
                for (int i = 0; i < 20; i++) {
                    double angle = Math.toRadians(i * 18);
                    double r = (ring + 1) * 1.0;
                    Location eyeLoc = center.clone().add(Math.cos(angle) * r, -0.05, Math.sin(angle) * r);
                    Material mat = ring == 0 ? Material.CRIMSON_NYLIUM : (ring == 1 ? Material.OBSIDIAN : Material.PURPLE_STAINED_GLASS);
                    BlockDisplayHandle ring_h = displayBuilder.spawnBlock(eyeLoc, mat);
                    ring_h.scale(0.8f, 0.05f, 0.8f).glow(ring == 0 ? 200 : 128, 0, ring == 0 ? 50 : 255).interpolation(3, 0);
                    handles.add(ring_h);
                    spawnedEntities.add(ring_h.entity());
                }
            }

            // Iris — rotates to track target
            BlockDisplayHandle iris = displayBuilder.spawnBlock(center.clone().add(0, -0.05, 0), Material.OBSIDIAN);
            iris.scale(1.5f, 0.06f, 1.5f).glow(200, 0, 50).interpolation(2, 0);
            handles.add(iris);
            spawnedEntities.add(iris.entity());

            // Beam segment (initially hidden below)
            for (int h = 0; h < 20; h++) {
                Location beamLoc = center.clone().add(0, h * 1.0, 0);
                BlockDisplayHandle beamSeg = displayBuilder.spawnBlock(beamLoc, Material.CRYING_OBSIDIAN);
                beamSeg.scale(0.5f, 1.0f, 0.5f).glow(200, 0, 50).interpolation(1, 0);
                handles.add(beamSeg);
                spawnedEntities.add(beamSeg.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_SHULKER_OPEN, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            int eyeNodeCount = 60; // 3 rings * 20 nodes
            int irisIdx = eyeNodeCount;
            int beamBase = irisIdx + 1;

            // Phase 1 (0-40): eye opens — iris rotates to track
            if (ticksAlive <= 40) {
                float openT = ticksAlive / 40f;

                // Rings expand as eye opens
                for (int ring = 0; ring < 3; ring++) {
                    for (int i = 0; i < 20; i++) {
                        int idx = ring * 20 + i;
                        if (idx >= irisIdx) break;
                        double angle = Math.toRadians(i * 18 + ticksAlive * 1.5);
                        double r = (ring + 1) * 1.0 * (0.5 + openT * 0.5);
                        Location eyeLoc = center.clone().add(Math.cos(angle) * r, -0.05, Math.sin(angle) * r);
                        handles.get(idx).entity().teleport(eyeLoc);
                    }
                }

                // Iris rotation tracks the beam direction
                if (irisIdx < handles.size()) {
                    BlockDisplay iris = (BlockDisplay) handles.get(irisIdx).entity();
                    iris.setTransformation(new Transformation(
                        new Vector3f(-0.75f, -0.03f, -0.75f),
                        new AxisAngle4f(ticksAlive * 0.08f, 0, 1, 0),
                        new Vector3f(1.5f, 0.06f, 1.5f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    iris.setInterpolationDelay(0);
                    iris.setInterpolationDuration(2);
                }

                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.crimsonDust(center, 8, 3.0);
                    DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.5f, 0.4f);
                }
                return;
            }

            // Phase 2 (40): beam fires
            if (!beamFired) {
                beamFired = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.3f);
                DisplayBuilder.crimsonDust(center.clone().add(0, 8, 0), 30, 3.0);
            }

            // Phase 3 (40-100): beam tracks upward from eye toward target
            if (ticksAlive >= 40 && ticksAlive <= 100) {
                // Beam fires straight upward from the eye
                for (int h = 0; h < 20; h++) {
                    int idx = beamBase + h;
                    if (idx >= handles.size()) break;
                    Location beamLoc = center.clone().add(0, h * 1.0, 0);
                    handles.get(idx).entity().teleport(beamLoc);

                    BlockDisplay bd = (BlockDisplay) handles.get(idx).entity();
                    float pulse = 0.5f + (float)Math.sin(ticksAlive * 0.4 + h * 0.1) * 0.15f;
                    bd.setTransformation(new Transformation(
                        new Vector3f(-pulse * 0.5f, -0.5f, -pulse * 0.5f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(pulse, 1.0f, pulse),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(2);
                }

                // Damage along beam height
                if (ticksAlive % 20 == 0) {
                    triggerImpactDamage(center.clone().add(0, 2, 0));
                    triggerImpactDamage(center.clone().add(0, 6, 0));
                    triggerImpactDamage(center.clone().add(0, 12, 0));
                    DisplayBuilder.crimsonDust(center.clone().add(0, 10, 0), 15, 2.0);
                }

                // Eye continues to track
                if (irisIdx < handles.size()) {
                    BlockDisplay iris = (BlockDisplay) handles.get(irisIdx).entity();
                    iris.setTransformation(new Transformation(
                        new Vector3f(-0.75f, -0.03f, -0.75f),
                        new AxisAngle4f(ticksAlive * 0.15f, 0, 1, 0),
                        new Vector3f(1.5f, 0.06f, 1.5f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    iris.setInterpolationDelay(0);
                    iris.setInterpolationDuration(2);
                }
            }

            // Phase 4 (100-120): beam fades
            if (ticksAlive > 100) {
                float decay = (ticksAlive - 100) / 20f;
                for (int h = 0; h < 20; h++) {
                    int idx = beamBase + h;
                    if (idx >= handles.size()) break;
                    BlockDisplay bd = (BlockDisplay) handles.get(idx).entity();
                    float scale = 0.5f * (1f - decay);
                    if (scale < 0.01f) scale = 0.01f;
                    bd.setTransformation(new Transformation(
                        new Vector3f(-scale * 0.5f, -0.5f, -scale * 0.5f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(scale, 1.0f, scale),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(3);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new EyeBeam(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 45. Cross Cut
    // Two beams fire from perpendicular edges and sweep inward, meeting at the
    // island center. Intersection zone deals double damage.
    // -------------------------------------------------------------------------
    public static class CrossCut extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean sweepStarted = false;

        public CrossCut(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cross_cut", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(130);
            config.setCooldownTicks(600);
            config.setDamageOnImpactOnly(false);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.5f);

            // Telegraph: two beam lines shown as particles from origin edges
            // Beam 1: from north edge, sweeps east-west
            for (int i = -10; i <= 10; i++) {
                Location tele1 = center.clone().add(i, 1.5, -20);
                DisplayBuilder.purpleDust(tele1, 3, 0.3);
            }
            // Beam 2: from east edge, sweeps north-south
            for (int i = -10; i <= 10; i++) {
                Location tele2 = center.clone().add(20, 1.5, i);
                DisplayBuilder.purpleDust(tele2, 3, 0.3);
            }

            // Beam 1 segments (north-south sweep, starts at north edge)
            for (int i = 0; i < 20; i++) {
                double r = i * 1.1;
                Location seg1Loc = center.clone().add(0, 1.5, -r);
                BlockDisplayHandle seg1 = displayBuilder.spawnBlock(seg1Loc, Material.OBSIDIAN);
                seg1.scale(3.0f, 2.0f, 0.3f).glow(128, 0, 255).interpolation(1, 0);
                handles.add(seg1);
                spawnedEntities.add(seg1.entity());
            }

            // Beam 2 segments (east-west sweep, starts at east edge)
            for (int i = 0; i < 20; i++) {
                double r = i * 1.1;
                Location seg2Loc = center.clone().add(r, 1.5, 0);
                BlockDisplayHandle seg2 = displayBuilder.spawnBlock(seg2Loc, Material.CRYING_OBSIDIAN);
                seg2.scale(0.3f, 2.0f, 3.0f).glow(80, 0, 160).interpolation(1, 0);
                handles.add(seg2);
                spawnedEntities.add(seg2.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.size() < 40) return;

            // Phase 1 (0-40): telegraph
            if (ticksAlive < 40) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 1.5, -20), 10, 4.0);
                    DisplayBuilder.purpleDust(center.clone().add(20, 1.5, 0), 10, 4.0);
                }
                return;
            }

            // Phase 2 (40): beams start
            if (!sweepStarted) {
                sweepStarted = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.6f);
            }

            // Phase 2 (40-100): beams sweep toward center simultaneously
            if (ticksAlive >= 40 && ticksAlive <= 100) {
                float sweepT = (ticksAlive - 40) / 60f;
                // Beam 1 sweeps from north (+z direction) to south (-z direction) — i.e. Z decreases
                // Beam 2 sweeps from east (-x direction) to west (+x direction)
                double beam1Z = -22 + sweepT * 22; // north edge to center
                double beam2X = 22 - sweepT * 22;  // east edge to center

                // Advance beam 1 position
                for (int i = 0; i < 20; i++) {
                    if (i >= handles.size()) break;
                    double r = i * 1.1;
                    // Beam 1 is a horizontal bar at constant z, spanning x
                    Location seg1Loc = center.clone().add(-22 + i * 2.2, 1.5, beam1Z);
                    handles.get(i).entity().teleport(seg1Loc);
                }

                // Advance beam 2 position
                for (int i = 0; i < 20; i++) {
                    int idx = 20 + i;
                    if (idx >= handles.size()) break;
                    // Beam 2 is a horizontal bar at constant x, spanning z
                    Location seg2Loc = center.clone().add(beam2X, 1.5, -22 + i * 2.2);
                    handles.get(idx).entity().teleport(seg2Loc);
                }

                // Damage along beam paths
                if (ticksAlive % 10 == 0) {
                    // Beam 1 damage strip
                    for (int i = -3; i <= 3; i++) {
                        triggerImpactDamage(center.clone().add(i * 4.0, 1.5, beam1Z));
                    }
                    // Beam 2 damage strip
                    for (int i = -3; i <= 3; i++) {
                        triggerImpactDamage(center.clone().add(beam2X, 1.5, i * 4.0));
                    }
                    // Enhanced damage at intersection
                    if (Math.abs(beam1Z) < 4 && Math.abs(beam2X) < 4) {
                        triggerImpactDamage(center.clone().add(0, 1.5, 0));
                        DisplayBuilder.purpleDust(center.clone().add(0, 2, 0), 25, 4.0);
                    }

                    DisplayBuilder.purpleDust(center.clone().add(0, 1.5, beam1Z), 8, 3.0);
                    DisplayBuilder.purpleDust(center.clone().add(beam2X, 1.5, 0), 8, 3.0);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new CrossCut(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 46. Death Spiral Beam
    // A single beam fires upward from the center, then its origin begins
    // rotating outward — inscribing a growing spiral across the island.
    // -------------------------------------------------------------------------
    public static class DeathSpiralBeam extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean spiralActive = false;

        public DeathSpiralBeam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("death_spiral_beam", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(9.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(640);
            config.setDamageOnImpactOnly(false);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Center glow telegraph
            BlockDisplayHandle centerGlow = displayBuilder.spawnBlock(center.clone().add(0, -0.05, 0),
                    Material.PURPLE_STAINED_GLASS);
            centerGlow.scale(2.0f, 0.1f, 2.0f).glow(128, 0, 255).interpolation(4, 0);
            handles.add(centerGlow);
            spawnedEntities.add(centerGlow.entity());

            // Spiral preview particles
            for (int i = 0; i < 30; i++) {
                double t = i / 30.0;
                double spiralAngle = t * Math.PI * 4; // 2 full rotations
                double r = t * 18.0;
                Location previewLoc = center.clone().add(Math.cos(spiralAngle) * r, 0.5, Math.sin(spiralAngle) * r);
                DisplayBuilder.purpleDust(previewLoc, 3, 0.5);
            }

            // Beam segments — will be repositioned to trace spiral
            for (int i = 0; i < 12; i++) {
                Location beamLoc = center.clone().add(0, i * 1.5, 0);
                BlockDisplayHandle seg = displayBuilder.spawnBlock(beamLoc, Material.OBSIDIAN);
                seg.scale(1.5f, 1.5f, 1.5f).glow(128, 0, 255).interpolation(2, 0);
                handles.add(seg);
                spawnedEntities.add(seg.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            // Phase 1 (0-50): center glows — telegraph
            if (ticksAlive < 50) {
                float buildUp = ticksAlive / 50f;
                BlockDisplay centerGlow = (BlockDisplay) handles.get(0).entity();
                float s = 2.0f * buildUp + 0.5f;
                centerGlow.setTransformation(new Transformation(
                    new Vector3f(-s * 0.5f, -0.05f, -s * 0.5f),
                    new AxisAngle4f(ticksAlive * 0.1f, 0, 1, 0),
                    new Vector3f(s, 0.1f, s),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                centerGlow.setInterpolationDelay(0);
                centerGlow.setInterpolationDuration(4);

                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.purpleDust(center, 10, 3.0);
                }
                return;
            }

            // Phase 2 (50): spiral begins
            if (!spiralActive) {
                spiralActive = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.4f);
            }

            // Phase 3 (50-170): spiral expands outward
            if (ticksAlive >= 50 && ticksAlive <= 170) {
                float spiralAge = (ticksAlive - 50) / 120f;
                // Rotation advances outward: angle increases, radius grows
                double spiralAngle = spiralAge * Math.PI * 3; // 1.5 rotations total
                double spiralRadius = spiralAge * 18.0; // 0 to 18 blocks

                // Beam tip position
                double tipX = Math.cos(spiralAngle) * spiralRadius;
                double tipZ = Math.sin(spiralAngle) * spiralRadius;

                // Reposition beam segments at the current spiral arm tip
                for (int i = 0; i < 12 && i + 1 < handles.size(); i++) {
                    double segAngle = spiralAngle - i * 0.15;
                    double segR = Math.max(0, spiralRadius - i * 1.5);
                    double sx = Math.cos(segAngle) * segR;
                    double sz = Math.sin(segAngle) * segR;
                    handles.get(i + 1).entity().teleport(center.clone().add(sx, 0.5, sz));

                    BlockDisplay bd = (BlockDisplay) handles.get(i + 1).entity();
                    float pulse = 1.5f + (float)Math.sin(ticksAlive * 0.3 + i) * 0.3f;
                    bd.setTransformation(new Transformation(
                        new Vector3f(-pulse * 0.5f, -0.75f, -pulse * 0.5f),
                        new AxisAngle4f((float)segAngle, 0, 1, 0),
                        new Vector3f(pulse, 1.5f, pulse),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(2);
                }

                // Damage at beam tip
                if (ticksAlive % 15 == 0) {
                    Location tipLoc = center.clone().add(tipX, 0.5, tipZ);
                    triggerImpactDamage(tipLoc);
                    DisplayBuilder.purpleDust(tipLoc, 12, 2.0);
                }

                // Burning trail particles
                if (ticksAlive % 3 == 0) {
                    Location trailLoc = center.clone().add(tipX, 0.5, tipZ);
                    DisplayBuilder.purpleDust(trailLoc, 4, 1.0);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new DeathSpiralBeam(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 47. Void Ray Burst
    // 8 beams fire simultaneously in a radial burst from below — an asterisk
    // pattern at 45-degree intervals. Safe zones are the 8 gaps between beams.
    // -------------------------------------------------------------------------
    public static class VoidRayBurst extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean burst = false;

        public VoidRayBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_ray_burst", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(560);
            config.setDamageOnImpactOnly(false);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 8 beam origin points glow — telegraph
            for (int b = 0; b < 8; b++) {
                double angle = Math.toRadians(b * 45);
                Location originLoc = center.clone().add(0, -0.1, 0);
                BlockDisplayHandle originGlow = displayBuilder.spawnBlock(originLoc, Material.PURPLE_STAINED_GLASS);
                originGlow.scale(0.5f, 0.1f, 0.5f).glow(128, 0, 255).interpolation(3, 0);
                handles.add(originGlow);
                spawnedEntities.add(originGlow.entity());

                // 18 segments per beam extending to island edge
                for (int s = 0; s < 18; s++) {
                    double r = (s + 1) * 1.1;
                    double bx = Math.cos(angle) * r;
                    double bz = Math.sin(angle) * r;
                    Location beamLoc = center.clone().add(bx, 0.5 - 5, bz); // hidden below initially
                    BlockDisplayHandle seg = displayBuilder.spawnBlock(beamLoc, Material.OBSIDIAN);
                    seg.scale(2.0f, 0.6f, 2.0f).glow(128, 0, 255).interpolation(1, 0);
                    handles.add(seg);
                    spawnedEntities.add(seg.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            int perBeam = 1 + 18; // origin glow + 18 segments

            // Phase 1 (0-40): telegraph — origin glows brighten
            if (ticksAlive < 40) {
                float buildUp = ticksAlive / 40f;
                for (int b = 0; b < 8; b++) {
                    int originIdx = b * perBeam;
                    if (originIdx >= handles.size()) break;
                    BlockDisplay origin = (BlockDisplay) handles.get(originIdx).entity();
                    float s = 0.5f + buildUp * 1.5f;
                    origin.setTransformation(new Transformation(
                        new Vector3f(-s * 0.5f, -0.05f, -s * 0.5f),
                        new AxisAngle4f(ticksAlive * 0.1f, 0, 1, 0),
                        new Vector3f(s, 0.1f, s),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    origin.setInterpolationDelay(0);
                    origin.setInterpolationDuration(3);

                    if (ticksAlive % 5 == 0) {
                        double angle = Math.toRadians(b * 45);
                        DisplayBuilder.purpleDust(center.clone().add(Math.cos(angle) * 8, 0.5, Math.sin(angle) * 8), 6, 1.5);
                    }
                }
                return;
            }

            // Phase 2 (40): burst fires
            if (!burst && ticksAlive == 40) {
                burst = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.5f);
                DisplayBuilder.purpleDust(center, 40, 6.0);

                // Raise all beam segments to surface
                for (int b = 0; b < 8; b++) {
                    double angle = Math.toRadians(b * 45);
                    for (int s = 0; s < 18; s++) {
                        int idx = b * perBeam + 1 + s;
                        if (idx >= handles.size()) break;
                        double r = (s + 1) * 1.1;
                        handles.get(idx).entity().teleport(
                            center.clone().add(Math.cos(angle) * r, 0.5, Math.sin(angle) * r)
                        );
                    }
                }
            }

            // Phase 3 (40-80): beams sustain with pulse animation
            if (burst && ticksAlive >= 40 && ticksAlive <= 80) {
                for (int b = 0; b < 8; b++) {
                    for (int s = 0; s < 18; s++) {
                        int idx = b * perBeam + 1 + s;
                        if (idx >= handles.size()) break;
                        BlockDisplay bd = (BlockDisplay) handles.get(idx).entity();
                        float pulse = 2.0f + (float)Math.sin(ticksAlive * 0.4 + s * 0.2) * 0.4f;
                        bd.setTransformation(new Transformation(
                            new Vector3f(-pulse * 0.5f, -0.3f, -pulse * 0.5f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(pulse, 0.6f, pulse),
                            new AxisAngle4f(0, 0, 1, 0)
                        ));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(2);
                    }
                }

                // Damage every 15 ticks
                if (ticksAlive % 15 == 0) {
                    for (int b = 0; b < 8; b++) {
                        double angle = Math.toRadians(b * 45);
                        for (int s = 3; s < 18; s += 5) {
                            double r = (s + 1) * 1.1;
                            Location dmgLoc = center.clone().add(Math.cos(angle) * r, 0.5, Math.sin(angle) * r);
                            triggerImpactDamage(dmgLoc);
                        }
                    }
                    DisplayBuilder.purpleDust(center, 10, 5.0);
                }
            }

            // Phase 4 (80-100): fade out
            if (burst && ticksAlive > 80) {
                float decay = (ticksAlive - 80) / 20f;
                for (int b = 0; b < 8; b++) {
                    for (int s = 0; s < 18; s++) {
                        int idx = b * perBeam + 1 + s;
                        if (idx >= handles.size()) break;
                        BlockDisplay bd = (BlockDisplay) handles.get(idx).entity();
                        float scale = 2.0f * (1f - decay);
                        if (scale < 0.01f) scale = 0.01f;
                        bd.setTransformation(new Transformation(
                            new Vector3f(-scale * 0.5f, -0.3f, -scale * 0.5f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(scale, 0.6f, scale),
                            new AxisAngle4f(0, 0, 1, 0)
                        ));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(4);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new VoidRayBurst(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 48. Resonance Beam
    // A wide 5-block diameter beam (glowing red) sweeps in a slow pendulum
    // across the island. Second hit deals triple damage.
    // -------------------------------------------------------------------------
    public static class ResonanceBeam extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean swingBack = false;

        public ResonanceBeam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("resonance_beam", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(5.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(700);
            config.setDamageOnImpactOnly(false);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Humming resonance — red biological beam, NOT black like others
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.6f);

            // Wide red beam column — 5-block diameter (use crimson material)
            // Starts at western edge
            for (int h = 0; h < 15; h++) {
                Location beamLoc = center.clone().add(-22, h * 1.0, 0);
                BlockDisplayHandle seg = displayBuilder.spawnBlock(beamLoc, Material.CRIMSON_NYLIUM);
                seg.scale(5.0f, 1.0f, 5.0f).glow(200, 0, 50).interpolation(3, 0);
                handles.add(seg);
                spawnedEntities.add(seg.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            // Phase 1 (0-20): humming telegraph
            if (ticksAlive < 20) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.crimsonDust(center, 12, 6.0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, 0.5f);
                }
                return;
            }

            // Phase 2 (20-120): first pendulum sweep — west to east
            if (ticksAlive >= 20 && ticksAlive <= 120) {
                float sweep1T = (ticksAlive - 20) / 100f;
                double beamX = -22 + sweep1T * 44; // -22 to +22

                for (int h = 0; h < handles.size(); h++) {
                    handles.get(h).entity().teleport(center.clone().add(beamX, h * 1.0, 0));

                    BlockDisplay bd = (BlockDisplay) handles.get(h).entity();
                    float pulse = 5.0f + (float)Math.sin(ticksAlive * 0.15 + h * 0.1) * 0.5f;
                    bd.setTransformation(new Transformation(
                        new Vector3f(-pulse * 0.5f, -0.5f, -pulse * 0.5f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(pulse, 1.0f, pulse),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(3);
                }

                if (ticksAlive % 20 == 0) {
                    triggerImpactDamage(center.clone().add(beamX, 3, 0));
                    triggerImpactDamage(center.clone().add(beamX, 8, 0));
                    DisplayBuilder.crimsonDust(center.clone().add(beamX, 8, 0), 15, 5.0);
                }
            }

            // Phase 3 (120-220): swing back — east to west (consecutive hit zone)
            if (ticksAlive >= 120 && ticksAlive <= 220) {
                if (!swingBack) {
                    swingBack = true;
                    DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.4f);
                }

                float sweep2T = (ticksAlive - 120) / 100f;
                double beamX = 22 - sweep2T * 44; // +22 back to -22

                for (int h = 0; h < handles.size(); h++) {
                    handles.get(h).entity().teleport(center.clone().add(beamX, h * 1.0, 0));
                }

                if (ticksAlive % 20 == 0) {
                    triggerImpactDamage(center.clone().add(beamX, 3, 0));
                    triggerImpactDamage(center.clone().add(beamX, 8, 0));
                    // Triple damage indication with more particles
                    DisplayBuilder.crimsonDust(center.clone().add(beamX, 8, 0), 25, 5.0);
                    DisplayBuilder.purpleDust(center.clone().add(beamX, 4, 0), 15, 4.0);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new ResonanceBeam(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 49. Tunnel Vision
    // A single ultra-narrow beam fires straight up and rotates at high speed,
    // drawing a burning circle that expands outward like a record playing.
    // -------------------------------------------------------------------------
    public static class TunnelVision extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean beamActive = false;

        public TunnelVision(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tunnel_vision", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(11.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(160);
            config.setCooldownTicks(600);
            config.setDamageOnImpactOnly(false);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Axis glow telegraph at center
            BlockDisplayHandle axisGlow = displayBuilder.spawnBlock(center.clone().add(0, -0.05, 0),
                    Material.PURPLE_STAINED_GLASS);
            axisGlow.scale(1.5f, 0.1f, 1.5f).glow(128, 0, 255).interpolation(3, 0);
            handles.add(axisGlow);
            spawnedEntities.add(axisGlow.entity());

            // Rotating beam arm — thin, extends outward
            for (int i = 0; i < 20; i++) {
                Location beamLoc = center.clone().add(i * 0.9, 0.5, 0);
                BlockDisplayHandle beamSeg = displayBuilder.spawnBlock(beamLoc, Material.OBSIDIAN);
                beamSeg.scale(0.9f, 1.5f, 0.9f).glow(128, 0, 255).interpolation(1, 0);
                handles.add(beamSeg);
                spawnedEntities.add(beamSeg.entity());
            }

            // Burn track ring (expands)
            for (int i = 0; i < 24; i++) {
                double angle = Math.toRadians(i * 15);
                Location trackLoc = center.clone().add(Math.cos(angle) * 5.0, -0.05, Math.sin(angle) * 5.0);
                BlockDisplayHandle track = displayBuilder.spawnBlock(trackLoc, Material.ORANGE_CONCRETE);
                track.scale(0.4f, 0.05f, 0.4f).glow(200, 80, 0).interpolation(3, 0);
                handles.add(track);
                spawnedEntities.add(track.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 2.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            int beamSegCount = 20;
            int trackBase = 1 + beamSegCount; // after axis glow + beam segs

            // Phase 1 (0-30): telegraph — axis glows
            if (ticksAlive < 30) {
                float buildUp = ticksAlive / 30f;
                BlockDisplay axisGlow = (BlockDisplay) handles.get(0).entity();
                float s = 1.5f * buildUp + 0.3f;
                axisGlow.setTransformation(new Transformation(
                    new Vector3f(-s * 0.5f, -0.05f, -s * 0.5f),
                    new AxisAngle4f(ticksAlive * 0.15f, 0, 1, 0),
                    new Vector3f(s, 0.1f, s),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                axisGlow.setInterpolationDelay(0);
                axisGlow.setInterpolationDuration(3);

                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.purpleDust(center, 8, 2.0);
                }
                return;
            }

            // Phase 2 (30): beam activates
            if (!beamActive) {
                beamActive = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.5f);
            }

            // Phase 3 (30-150): beam rotates, circle expands from r=5 to r=20
            if (ticksAlive >= 30 && ticksAlive <= 150) {
                float beamAge = (ticksAlive - 30) / 120f;

                // Rotation — 2 full rotations over 6 seconds (120 ticks)
                double beamAngle = beamAge * Math.PI * 4;
                // Radius expansion: 5 → 20 blocks
                float currentR = 5.0f + beamAge * 15.0f;

                // Reposition beam segments along rotating arm
                for (int i = 0; i < beamSegCount && i + 1 < handles.size(); i++) {
                    double r = (i + 1) * (currentR / beamSegCount);
                    double bx = Math.cos(beamAngle) * r;
                    double bz = Math.sin(beamAngle) * r;
                    handles.get(i + 1).entity().teleport(center.clone().add(bx, 0.5, bz));
                }

                // Expand burn track ring
                for (int i = 0; i < 24; i++) {
                    int idx = trackBase + i;
                    if (idx >= handles.size()) break;
                    double angle = Math.toRadians(i * 15);
                    Location trackLoc = center.clone().add(
                        Math.cos(angle) * currentR, -0.05, Math.sin(angle) * currentR
                    );
                    handles.get(idx).entity().teleport(trackLoc);

                    BlockDisplay trackBd = (BlockDisplay) handles.get(idx).entity();
                    trackBd.setTransformation(new Transformation(
                        new Vector3f(-0.2f, -0.025f, -0.2f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.4f, 0.05f, 0.4f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    trackBd.setInterpolationDelay(0);
                    trackBd.setInterpolationDuration(3);
                }

                // Damage at beam tip and along ring
                if (ticksAlive % 15 == 0) {
                    Location tipLoc = center.clone().add(
                        Math.cos(beamAngle) * currentR, 0.5, Math.sin(beamAngle) * currentR
                    );
                    triggerImpactDamage(tipLoc);
                    DisplayBuilder.purpleDust(tipLoc, 12, 2.0);

                    // Fire damage on the burning track
                    double dmgAngle = beamAngle - Math.PI * 0.1;
                    Location fireLoc = center.clone().add(Math.cos(dmgAngle) * currentR, 0.3, Math.sin(dmgAngle) * currentR);
                    DisplayBuilder.crimsonDust(fireLoc, 8, 1.5);
                }

                // Particle trail behind beam tip
                if (ticksAlive % 2 == 0) {
                    double trailAngle = beamAngle - 0.2;
                    double trailR = currentR * 0.95;
                    Location trailLoc = center.clone().add(Math.cos(trailAngle) * trailR, 0.4, Math.sin(trailAngle) * trailR);
                    DisplayBuilder.purpleDust(trailLoc, 4, 0.5);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new TunnelVision(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 50. Omega Ray
    // The ultimate beam — 8 blocks in diameter, firing from the island center
    // for 6 full seconds. Colossal buildup, scorches the sky permanently.
    // -------------------------------------------------------------------------
    public static class OmegaRay extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean firing = false;

        public OmegaRay(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("omega_ray", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(15.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(1200);
            config.setDamageOnImpactOnly(false);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Long buildup: 4 seconds (80 ticks) of convergence
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.2f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.3f);

            // Convergence particles — massive ring around island that shrinks inward
            for (int ring = 0; ring < 5; ring++) {
                for (int i = 0; i < 32; i++) {
                    double angle = Math.toRadians(i * (360.0 / 32));
                    double r = (ring + 1) * 5.0;
                    Location convergeLoc = center.clone().add(Math.cos(angle) * r, 3.0, Math.sin(angle) * r);
                    BlockDisplayHandle converge = displayBuilder.spawnBlock(convergeLoc, Material.PURPLE_STAINED_GLASS);
                    converge.scale(0.5f, 0.5f, 0.5f).glow(128, 0, 255).interpolation(4, 0);
                    handles.add(converge);
                    spawnedEntities.add(converge.entity());
                }
            }

            // Omega beam itself — massive column, 8-block wide
            for (int h = 0; h < 30; h++) {
                Location beamLoc = center.clone().add(0, h * 1.0 - 30, 0); // hidden below
                BlockDisplayHandle beamSeg = displayBuilder.spawnBlock(beamLoc, Material.OBSIDIAN);
                beamSeg.scale(8.0f, 1.0f, 8.0f).glow(200, 200, 255).interpolation(2, 0);
                handles.add(beamSeg);
                spawnedEntities.add(beamSeg.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            int convergeNodeCount = 5 * 32; // 160
            int beamBase = convergeNodeCount;

            // Phase 1 (0-80): convergence buildup — particles spiral inward
            if (ticksAlive < 80) {
                float buildT = ticksAlive / 80f;

                for (int ring = 0; ring < 5; ring++) {
                    float ringProgress = buildT * (1.0f + ring * 0.1f);
                    float r = (ring + 1) * 5.0f * (1f - ringProgress * 0.9f);
                    if (r < 0.1f) r = 0.1f;

                    for (int i = 0; i < 32; i++) {
                        int idx = ring * 32 + i;
                        if (idx >= convergeNodeCount || idx >= handles.size()) break;
                        double angle = Math.toRadians(i * (360.0 / 32) + ticksAlive * 2.0);
                        double y = 3.0 - buildT * 2.5;
                        handles.get(idx).entity().teleport(
                            center.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r)
                        );

                        BlockDisplay bd = (BlockDisplay) handles.get(idx).entity();
                        float scale = 0.5f + buildT * 0.8f;
                        bd.setTransformation(new Transformation(
                            new Vector3f(-scale * 0.5f, -scale * 0.5f, -scale * 0.5f),
                            new AxisAngle4f(ticksAlive * 0.08f, 0, 1, 0),
                            new Vector3f(scale, scale, scale),
                            new AxisAngle4f(0, 0, 1, 0)
                        ));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(4);
                    }
                }

                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 2, 0), (int)(buildT * 30) + 5, 5.0 + buildT * 5.0);
                }

                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.8f + buildT * 0.4f, 0.3f + buildT * 0.3f);
                }
                return;
            }

            // Phase 2 (80): OMEGA RAY FIRES
            if (!firing) {
                firing = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.1f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.1f);
                DisplayBuilder.purpleDust(center, 80, 12.0);

                // Raise beam segments to surface
                for (int h = 0; h < 30; h++) {
                    int idx = beamBase + h;
                    if (idx >= handles.size()) break;
                    handles.get(idx).entity().teleport(center.clone().add(0, h * 1.0, 0));
                }
            }

            // Phase 3 (80-200): beam fires — massive static column
            if (firing && ticksAlive >= 80 && ticksAlive <= 200) {
                for (int h = 0; h < 30; h++) {
                    int idx = beamBase + h;
                    if (idx >= handles.size()) break;
                    BlockDisplay bd = (BlockDisplay) handles.get(idx).entity();
                    float pulse = 8.0f + (float)Math.sin(ticksAlive * 0.2 + h * 0.1) * 0.8f;
                    bd.setTransformation(new Transformation(
                        new Vector3f(-pulse * 0.5f, -0.5f, -pulse * 0.5f),
                        new AxisAngle4f(ticksAlive * 0.03f, 0, 1, 0),
                        new Vector3f(pulse, 1.0f, pulse),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(2);
                }

                // Heavy damage every 20 ticks inside beam
                if (ticksAlive % 20 == 0) {
                    triggerImpactDamage(center);
                    triggerImpactDamage(center.clone().add(0, 5, 0));
                    triggerImpactDamage(center.clone().add(0, 10, 0));
                    triggerImpactDamage(center.clone().add(0, 18, 0));
                    DisplayBuilder.purpleDust(center.clone().add(0, 15, 0), 30, 8.0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.8f, 0.3f);
                }

                // Crackling static at beam edges
                if (ticksAlive % 3 == 0) {
                    double edgeAngle = Math.toRadians(ticksAlive * 15);
                    Location edgeLoc = center.clone().add(Math.cos(edgeAngle) * 4, 10 + Math.random() * 5, Math.sin(edgeAngle) * 4);
                    DisplayBuilder.purpleDust(edgeLoc, 5, 1.0);
                }
            }

            // Phase 4 (200-280): beam fades, scorched sky lingering effect
            if (firing && ticksAlive > 200) {
                float decay = (ticksAlive - 200) / 80f;
                for (int h = 0; h < 30; h++) {
                    int idx = beamBase + h;
                    if (idx >= handles.size()) break;
                    BlockDisplay bd = (BlockDisplay) handles.get(idx).entity();
                    float scale = 8.0f * (1f - decay);
                    if (scale < 0.01f) scale = 0.01f;
                    bd.setTransformation(new Transformation(
                        new Vector3f(-scale * 0.5f, -0.5f, -scale * 0.5f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(scale, 1.0f, scale),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(6);
                }

                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 20, 0), (int)(15 * (1f - decay)) + 2, 6.0);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new OmegaRay(plugin);
        }
    }
}
