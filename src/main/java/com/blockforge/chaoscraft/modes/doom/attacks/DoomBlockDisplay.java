package com.blockforge.chaoscraft.modes.doom.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Doom Mode — ADVANCED BLOCK DISPLAY ATTACKS (v2)
 * 10 hellfire and doom-themed BlockDisplay attacks with 25-35 block displays each.
 * All animations via Transformation (AxisAngle4f rotation, Vector3f scale/translation).
 * Never teleport for rotation — use setInterpolationDuration for smooth animation.
 *
 * Doom palette:
 * - Lava orange: RGB(255, 80, 20)
 * - Hellfire red: RGB(200, 40, 10)
 * - Deep char: RGB(100, 20, 5)
 * - Ember glow: RGB(255, 150, 50)
 *
 * Materials: MAGMA_BLOCK, NETHERRACK, NETHER_BRICKS, BLACKSTONE, SHROOMLIGHT,
 *            BASALT, OBSIDIAN, CRYING_OBSIDIAN, GLOWSTONE
 *
 * Particles: LAVA, FLAME, SMOKE, LANDING_LAVA
 * Sounds: BLOCK_LAVA_POP, ENTITY_BLAZE_SHOOT, ENTITY_GENERIC_EXPLODE,
 *         BLOCK_FIRE_AMBIENT, BLOCK_ANVIL_LAND, BLOCK_BEACON_AMBIENT
 *
 * Attacks:
 *  1. InfernalPyramid         — 30 blocks, 4-sided pyramid with Y-spin
 *  2. MagmaSphereOrbit        — 32 blocks, golden-angle sphere + orbiting satellites
 *  3. HellfireObeliskArray    — 28 blocks, 4 obelisks with connecting beams
 *  4. BrimstoneGateway        — 32 blocks, grand arch with portal shimmer + player pull
 *  5. LavaGeyserTriple        — 30 blocks, 3 erupting columns with sequential stagger
 *  6. NetherCrystalFormation  — 27 blocks, crystal cluster with rocking motion
 *  7. DoomRitualCircle        — 35 blocks, ground ritual circle with rune pillars
 *  8. MoltenSerpentCoil       — 30 blocks, helical coil with head tracking
 *  9. InfernalCrucible        — 28 blocks, cauldron with bubbling lava surface
 * 10. BrimstoneStaircaseSpiral— 32 blocks, spiral staircase with sequential glow
 */
public final class DoomBlockDisplay {
    private DoomBlockDisplay() {}

    private static final String MODE_PATH = "modes/doom/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new InfernalPyramid(plugin));
        registry.register(new MagmaSphereOrbit(plugin));
        registry.register(new HellfireObeliskArray(plugin));
        registry.register(new BrimstoneGateway(plugin));
        registry.register(new LavaGeyserTriple(plugin));
        registry.register(new NetherCrystalFormation(plugin));
        registry.register(new DoomRitualCircle(plugin));
        registry.register(new MoltenSerpentCoil(plugin));
        registry.register(new InfernalCrucible(plugin));
        registry.register(new BrimstoneStaircaseSpiral(plugin));
    }

    // ================================================================
    // Helper: find nearest non-exempt survival player within range
    // ================================================================
    private static Player findNearestPlayer(Location center, double range) {
        if (center.getWorld() == null) return null;
        Player nearest = null;
        double nearestDist = range * range;
        for (Player p : center.getWorld().getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL) continue;
            double dist = p.getLocation().distanceSquared(center);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = p;
            }
        }
        return nearest;
    }

    // ================================================================
    // #1 — INFERNAL PYRAMID
    // 30 blocks: 12 base ring (flat plates), 8 mid tier, 6 upper tier,
    // 4 apex (shroomlight, tilted 22.5 deg). Full structure Y-spins.
    // FLAME spiral particles. 8r damage, 25dmg/25t.
    // ================================================================
    public static class InfernalPyramid extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private float yRotation = 0f;

        public InfernalPyramid(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_pyramid", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(25.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(300);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Base ring: 12 flat plates at radius 3.5, Y=0
            double baseRadius = 3.5;
            for (int i = 0; i < 12; i++) {
                double angle = (2.0 * Math.PI * i) / 12;
                double px = Math.cos(angle) * baseRadius;
                double pz = Math.sin(angle) * baseRadius;
                Location loc = center.clone().add(px, 0, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHER_BRICKS);
                h.scale(1.5f, 0.15f, 1.5f).glow(200, 40, 10).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // Mid tier: 8 blocks at radius 2.2, Y=1.5
            double midRadius = 2.2;
            for (int i = 0; i < 8; i++) {
                double angle = (2.0 * Math.PI * i) / 8 + Math.toRadians(22.5);
                double px = Math.cos(angle) * midRadius;
                double pz = Math.sin(angle) * midRadius;
                Location loc = center.clone().add(px, 1.5, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(1.1f, 0.8f, 1.1f).glow(255, 80, 20).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // Upper tier: 6 blocks at radius 1.2, Y=3.0
            double upperRadius = 1.2;
            for (int i = 0; i < 6; i++) {
                double angle = (2.0 * Math.PI * i) / 6;
                double px = Math.cos(angle) * upperRadius;
                double pz = Math.sin(angle) * upperRadius;
                Location loc = center.clone().add(px, 3.0, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.8f, 0.7f, 0.8f).glow(100, 20, 5).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // Apex: 4 shroomlight blocks tilted 22.5 deg outward, Y=4.5
            for (int i = 0; i < 4; i++) {
                double angle = (2.0 * Math.PI * i) / 4 + Math.toRadians(45);
                double px = Math.cos(angle) * 0.4;
                double pz = Math.sin(angle) * 0.4;
                Location loc = center.clone().add(px, 4.5, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SHROOMLIGHT);
                float tiltAngle = (float) Math.toRadians(22.5);
                float axisX = (float) -Math.sin(angle);
                float axisZ = (float) Math.cos(angle);
                h.scale(0.5f, 0.5f, 0.5f).rotate(tiltAngle, axisX, 0, axisZ);
                h.glow(255, 150, 50).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 0.8f, 0.7f);
            w.spawnParticle(Particle.LAVA, center.clone().add(0, 3, 0), 30, 2, 2, 2, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Y-rotation via Transformation
            yRotation += 0.03f;
            if (tick % 3 == 0) {
                for (BlockDisplayHandle h : allBlocks) {
                    BlockDisplay entity = h.entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(3);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(yRotation, 0f, 1f, 0f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // FLAME spiral particles
            if (tick % 2 == 0) {
                double spiralAngle = tick * 0.2;
                double spiralR = 2.0 + 0.5 * Math.sin(tick * 0.1);
                double spiralY = (tick % 60) / 60.0 * 5.0;
                Location pLoc = c.clone().add(
                        Math.cos(spiralAngle) * spiralR,
                        spiralY,
                        Math.sin(spiralAngle) * spiralR
                );
                c.getWorld().spawnParticle(Particle.FLAME, pLoc, 3, 0.1, 0.1, 0.1, 0.01);
            }

            // Ambient lava particles
            if (tick % 8 == 0) {
                c.getWorld().spawnParticle(Particle.LAVA, c.clone().add(0, 2, 0), 6, 3, 1, 3, 0.02);
            }

            // Sound
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 0.7f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new InfernalPyramid(plugin); }
    }

    // ================================================================
    // #2 — MAGMA SPHERE ORBIT
    // 32 blocks: 24 golden-angle sphere blocks + 8 satellites orbiting
    // on 3 planes via cos/sin recalc with interpolation. 7r, 20dmg/15t.
    // ================================================================
    public static class MagmaSphereOrbit extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> sphereBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> satellites = new ArrayList<>();
        private final double[] satPhases = new double[8];
        private float coreRotation = 0f;

        public MagmaSphereOrbit(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_sphere_orbit", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(20.0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(280);
            config.setCooldownTicks(220);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Core sphere: 24 blocks using golden angle distribution
            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            double sphereRadius = 2.5;
            for (int i = 0; i < 24; i++) {
                double y = 1.0 - (2.0 * i / 23.0);
                double radiusAtY = Math.sqrt(1 - y * y);
                double theta = goldenAngle * i;
                double px = Math.cos(theta) * radiusAtY * sphereRadius;
                double pz = Math.sin(theta) * radiusAtY * sphereRadius;
                Location loc = center.clone().add(px, y * sphereRadius + 2.5, pz);
                Material mat = (i % 3 == 0) ? Material.SHROOMLIGHT : (i % 3 == 1) ? Material.MAGMA_BLOCK : Material.NETHERRACK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.6f, 0.6f, 0.6f).glow(255, 80, 20).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                sphereBlocks.add(h);
            }

            // 8 satellites on 3 orbital planes
            Material[] satMats = {Material.GLOWSTONE, Material.CRYING_OBSIDIAN, Material.SHROOMLIGHT,
                    Material.GLOWSTONE, Material.CRYING_OBSIDIAN, Material.SHROOMLIGHT,
                    Material.GLOWSTONE, Material.CRYING_OBSIDIAN};
            for (int i = 0; i < 8; i++) {
                satPhases[i] = (2.0 * Math.PI * i) / 8.0;
                double orbitR = 4.5;
                double px, py, pz;
                int plane = i % 3;
                double angle = satPhases[i];
                if (plane == 0) { // XZ plane
                    px = Math.cos(angle) * orbitR;
                    py = 2.5;
                    pz = Math.sin(angle) * orbitR;
                } else if (plane == 1) { // XY plane
                    px = Math.cos(angle) * orbitR;
                    py = Math.sin(angle) * orbitR + 2.5;
                    pz = 0;
                } else { // YZ plane
                    px = 0;
                    py = Math.sin(angle) * orbitR + 2.5;
                    pz = Math.cos(angle) * orbitR;
                }
                Location loc = center.clone().add(px, py, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, satMats[i]);
                h.scale(0.4f, 0.4f, 0.4f).glow(255, 150, 50).interpolation(2, 0);
                spawnedEntities.add(h.entity());
                satellites.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.6f);
            w.spawnParticle(Particle.FLAME, center.clone().add(0, 2.5, 0), 40, 2, 2, 2, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Core sphere Y-rotation via Transformation
            coreRotation += 0.025f;
            if (tick % 2 == 0) {
                for (BlockDisplayHandle h : sphereBlocks) {
                    BlockDisplay entity = h.entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(2);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(coreRotation, 0f, 1f, 0f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Satellites: recalculate orbital positions via translation interpolation
            if (tick % 3 == 0) {
                double orbitR = 4.5;
                double speed = 0.06;
                for (int i = 0; i < satellites.size(); i++) {
                    double angle = satPhases[i] + tick * speed;
                    int plane = i % 3;
                    float tx, ty, tz;
                    if (plane == 0) {
                        tx = (float) (Math.cos(angle) * orbitR) - 0.5f;
                        ty = 2.0f;
                        tz = (float) (Math.sin(angle) * orbitR) - 0.5f;
                    } else if (plane == 1) {
                        tx = (float) (Math.cos(angle) * orbitR) - 0.5f;
                        ty = (float) (Math.sin(angle) * orbitR) + 2.0f;
                        tz = -0.5f;
                    } else {
                        tx = -0.5f;
                        ty = (float) (Math.sin(angle) * orbitR) + 2.0f;
                        tz = (float) (Math.cos(angle) * orbitR) - 0.5f;
                    }
                    BlockDisplay entity = satellites.get(i).entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(3);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            new Vector3f(tx, ty, tz),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Particles
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.FLAME, c.clone().add(0, 2.5, 0), 5, 2.5, 2.5, 2.5, 0.01);
                c.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(0, 2.5, 0), 3, 3, 3, 3, 0.01);
            }

            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.LAVA, c.clone().add(0, 2.5, 0), 4, 1.5, 1.5, 1.5, 0.02);
            }

            // Sound
            if (tick % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.6f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new MagmaSphereOrbit(plugin); }
    }

    // ================================================================
    // #3 — HELLFIRE OBELISK ARRAY
    // 28 blocks: 4 obelisks (5 stacked + 1 cap each = 24), 8 beam blocks
    // connecting tops (scale(0.15,0.15,3.0)). Obelisks rotate Y independently.
    // Beams pulse scale. 6r, 30dmg/30t.
    // ================================================================
    public static class HellfireObeliskArray extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> obelisks = new ArrayList<>();
        private final List<BlockDisplayHandle> beams = new ArrayList<>();
        private final float[] obeliskRotations = new float[4];
        private float beamPulse = 0f;

        public HellfireObeliskArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellfire_obelisk_array", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(30.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(30);
            config.setDurationTicks(360);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 4 obelisks at corners of a 5-block square
            double[][] corners = {{-2.5, 0, -2.5}, {2.5, 0, -2.5}, {2.5, 0, 2.5}, {-2.5, 0, 2.5}};
            Material[] obeliskMats = {Material.BLACKSTONE, Material.NETHER_BRICKS, Material.BLACKSTONE, Material.NETHER_BRICKS};

            for (int o = 0; o < 4; o++) {
                List<BlockDisplayHandle> pillar = new ArrayList<>();
                obeliskRotations[o] = 0f;

                // 5 stacked blocks, tapering upward
                for (int y = 0; y < 5; y++) {
                    float taper = 1.0f - (y * 0.12f); // Slight taper: 1.0, 0.88, 0.76, 0.64, 0.52
                    Location loc = center.clone().add(corners[o][0], y * 1.1, corners[o][2]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, obeliskMats[o]);
                    h.scale(taper, 1.1f, taper).glow(100, 20, 5).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    pillar.add(h);
                }

                // Cap block (shroomlight)
                Location capLoc = center.clone().add(corners[o][0], 5.5, corners[o][2]);
                BlockDisplayHandle cap = displayBuilder.spawnBlock(capLoc, Material.SHROOMLIGHT);
                cap.scale(0.6f, 0.4f, 0.6f).glow(255, 150, 50).interpolation(3, 0);
                spawnedEntities.add(cap.entity());
                pillar.add(cap);

                obelisks.add(pillar);
            }

            // 8 beams connecting obelisk tops (2 per connection, 4 connections)
            int[][] connections = {{0, 1}, {1, 2}, {2, 3}, {3, 0}};
            for (int[] conn : connections) {
                double mx = (corners[conn[0]][0] + corners[conn[1]][0]) / 2.0;
                double mz = (corners[conn[0]][2] + corners[conn[1]][2]) / 2.0;

                // Calculate beam direction angle
                double dx = corners[conn[1]][0] - corners[conn[0]][0];
                double dz = corners[conn[1]][2] - corners[conn[0]][2];
                float beamYaw = (float) Math.atan2(dz, dx);

                for (int b = 0; b < 2; b++) {
                    double t = (b == 0) ? 0.33 : 0.66;
                    double bx = corners[conn[0]][0] + dx * t;
                    double bz = corners[conn[0]][2] + dz * t;
                    Location loc = center.clone().add(bx, 5.2, bz);
                    BlockDisplayHandle beam = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                    beam.scale(0.15f, 0.15f, 3.0f);
                    beam.rotate(beamYaw, 0, 1, 0);
                    beam.glow(255, 80, 20).interpolation(3, 0);
                    spawnedEntities.add(beam.entity());
                    beams.add(beam);
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.8f, 0.4f);
            w.spawnParticle(Particle.SMOKE, center.clone().add(0, 3, 0), 30, 3, 2, 3, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Each obelisk rotates Y at different speeds
            float[] speeds = {0.02f, -0.03f, 0.025f, -0.015f};
            if (tick % 3 == 0) {
                for (int o = 0; o < 4; o++) {
                    obeliskRotations[o] += speeds[o];
                    for (BlockDisplayHandle h : obelisks.get(o)) {
                        BlockDisplay entity = h.entity();
                        Transformation t = entity.getTransformation();
                        entity.setInterpolationDuration(3);
                        entity.setInterpolationDelay(0);
                        entity.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f(obeliskRotations[o], 0f, 1f, 0f),
                                t.getScale(),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                }
            }

            // Beam scale pulse
            beamPulse += 0.08f;
            if (tick % 3 == 0) {
                float pulseScale = 0.15f + 0.08f * (float) Math.sin(beamPulse);
                for (BlockDisplayHandle beam : beams) {
                    BlockDisplay entity = beam.entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(3);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(pulseScale, pulseScale, 3.0f),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Particles from obelisk tops
            if (tick % 6 == 0) {
                double[][] corners = {{-2.5, 5.5, -2.5}, {2.5, 5.5, -2.5}, {2.5, 5.5, 2.5}, {-2.5, 5.5, 2.5}};
                for (double[] corner : corners) {
                    Location pLoc = c.clone().add(corner[0], corner[1], corner[2]);
                    c.getWorld().spawnParticle(Particle.FLAME, pLoc, 3, 0.2, 0.3, 0.2, 0.02);
                }
            }

            if (tick % 10 == 0) {
                c.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(0, 5, 0), 8, 3, 0.5, 3, 0.01);
            }

            // Sound
            if (tick % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 0.6f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new HellfireObeliskArray(plugin); }
    }

    // ================================================================
    // #4 — BRIMSTONE GATEWAY
    // 32 blocks: 2 pillars (6 tapered each=12), crossbar (4), 6 portal
    // blocks (thin scale(0.8,2.0,0.05)), 4 horn spikes at 45 deg, 6 base
    // detail. Portal Z-scale shimmer. Player velocity pull. 5r, 35dmg/30t.
    // ================================================================
    public static class BrimstoneGateway extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> portalBlocks = new ArrayList<>();
        private float shimmerPhase = 0f;

        public BrimstoneGateway(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_gateway", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(35.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(30);
            config.setDurationTicks(420);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Left pillar: 6 blocks tapering, at X=-2
            for (int y = 0; y < 6; y++) {
                float taper = 1.2f - (y * 0.1f);
                Location loc = center.clone().add(-2, y * 1.1, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(taper, 1.1f, taper).glow(100, 20, 5).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // Right pillar: 6 blocks tapering, at X=+2
            for (int y = 0; y < 6; y++) {
                float taper = 1.2f - (y * 0.1f);
                Location loc = center.clone().add(2, y * 1.1, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(taper, 1.1f, taper).glow(100, 20, 5).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // Crossbar: 4 blocks spanning the top
            for (int i = 0; i < 4; i++) {
                double xOff = -1.5 + i;
                Location loc = center.clone().add(xOff, 6.6, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHER_BRICKS);
                h.scale(1.2f, 0.5f, 1.2f).glow(200, 40, 10).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // Portal blocks: 6 thin blocks filling the archway
            for (int i = 0; i < 6; i++) {
                double yOff = 0.5 + i * 1.0;
                Location loc = center.clone().add(0, yOff, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.8f, 2.0f, 0.05f).glow(200, 40, 10).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
                portalBlocks.add(h);
            }

            // Horn spikes: 4 at 45 deg angles on top corners
            double[][] hornOffsets = {{-2.5, 7.0, 0}, {2.5, 7.0, 0}, {-2.5, 7.0, 0}, {2.5, 7.0, 0}};
            float[] hornAngles = {
                    (float) Math.toRadians(45), (float) Math.toRadians(-45),
                    (float) Math.toRadians(45), (float) Math.toRadians(-45)
            };
            float[][] hornAxes = {{0, 0, 1}, {0, 0, 1}, {1, 0, 0}, {1, 0, 0}};
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(hornOffsets[i][0], hornOffsets[i][1], hornOffsets[i][2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BASALT);
                h.scale(0.3f, 1.2f, 0.3f);
                h.rotate(hornAngles[i], hornAxes[i][0], hornAxes[i][1], hornAxes[i][2]);
                h.glow(100, 20, 5).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // Base detail: 6 slabs flanking the base
            for (int i = 0; i < 6; i++) {
                double xOff = (i < 3) ? -3.0 + i * 0.5 : 1.5 + (i - 3) * 0.5;
                Location loc = center.clone().add(xOff, -0.1, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHER_BRICKS);
                h.scale(0.6f, 0.15f, 1.0f).glow(200, 40, 10).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.4f);
            w.spawnParticle(Particle.SMOKE, center.clone().add(0, 3, 0), 25, 1, 3, 1, 0.03);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Portal Z-scale shimmer
            shimmerPhase += 0.1f;
            if (tick % 2 == 0) {
                float zScale = 0.05f + 0.03f * (float) Math.sin(shimmerPhase);
                for (BlockDisplayHandle portal : portalBlocks) {
                    BlockDisplay entity = portal.entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(2);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(0.8f, 2.0f, zScale),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Player velocity pull toward gateway center
            if (tick % 5 == 0) {
                double pullRange = 10.0;
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    double dist = p.getLocation().distance(c);
                    if (dist <= pullRange && dist > 1.0) {
                        Vector dir = c.toVector().subtract(p.getLocation().toVector()).normalize();
                        double strength = 0.15 * (1.0 - dist / pullRange);
                        p.setVelocity(p.getVelocity().add(dir.multiply(strength)));
                    }
                }
            }

            // Portal particles
            if (tick % 3 == 0) {
                for (int i = 0; i < 3; i++) {
                    double yOff = Math.random() * 6.0;
                    c.getWorld().spawnParticle(Particle.FLAME, c.clone().add(
                            (Math.random() - 0.5) * 1.5, yOff, 0), 2, 0.1, 0.1, 0.01, 0.01);
                }
                c.getWorld().spawnParticle(Particle.LANDING_LAVA, c.clone().add(0, 0.2, 0), 3, 1, 0, 1, 0);
            }

            // Sound
            if (tick % 60 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneGateway(plugin); }
    }

    // ================================================================
    // #5 — LAVA GEYSER TRIPLE
    // 30 blocks: 3 columns (10 each): 6 stacked segments erupting via
    // Y-translation interpolation + 4 cap burst at 22.5 deg. Sequential
    // stagger 20t. Impact-only 8r, 45 impact damage.
    // ================================================================
    public static class LavaGeyserTriple extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> columns = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> caps = new ArrayList<>();
        private final int[] spawnTicks = {0, 20, 40};
        private final boolean[] erupted = {false, false, false};

        public LavaGeyserTriple(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lava_geyser_triple", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(45.0);
            config.setImpactRadius(8.0);
            config.setDamage(0);
            config.setDurationTicks(300);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 3 geyser positions in a triangle
            double[][] geyserPos = {{0, 0, -3}, {-2.6, 0, 1.5}, {2.6, 0, 1.5}};

            for (int g = 0; g < 3; g++) {
                List<BlockDisplayHandle> column = new ArrayList<>();
                List<BlockDisplayHandle> cap = new ArrayList<>();

                // 6 stacked segments, initially compressed at Y=0
                for (int s = 0; s < 6; s++) {
                    Location loc = center.clone().add(geyserPos[g][0], 0.1, geyserPos[g][2]);
                    Material mat = (s < 3) ? Material.MAGMA_BLOCK : Material.NETHERRACK;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    float widthTaper = 0.8f - s * 0.08f;
                    h.scale(widthTaper, 0.8f, widthTaper);
                    h.translate(-0.5f, -0.5f, -0.5f); // Default centered
                    h.glow(255, 80, 20).interpolation(5, 0);
                    spawnedEntities.add(h.entity());
                    column.add(h);
                }

                // 4 cap blocks at 22.5 deg angles
                for (int c = 0; c < 4; c++) {
                    Location loc = center.clone().add(geyserPos[g][0], 0.1, geyserPos[g][2]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SHROOMLIGHT);
                    float tiltAngle = (float) Math.toRadians(22.5);
                    double capAngle = (2.0 * Math.PI * c) / 4.0;
                    float axisX = (float) Math.cos(capAngle);
                    float axisZ = (float) Math.sin(capAngle);
                    h.scale(0.4f, 0.6f, 0.4f);
                    h.rotate(tiltAngle, axisX, 0, axisZ);
                    h.glow(255, 150, 50).interpolation(5, 0);
                    spawnedEntities.add(h.entity());
                    cap.add(h);
                }

                columns.add(column);
                caps.add(cap);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double[][] geyserPos = {{0, 0, -3}, {-2.6, 0, 1.5}, {2.6, 0, 1.5}};

            for (int g = 0; g < 3; g++) {
                int elapsed = tick - spawnTicks[g];
                if (elapsed < 0) continue;

                // Eruption phase: translate blocks upward over 30 ticks
                if (elapsed <= 30 && !erupted[g]) {
                    float progress = elapsed / 30.0f;
                    List<BlockDisplayHandle> column = columns.get(g);
                    for (int s = 0; s < column.size(); s++) {
                        float targetY = s * 1.0f * progress;
                        BlockDisplay entity = column.get(s).entity();
                        Transformation t = entity.getTransformation();
                        entity.setInterpolationDuration(3);
                        entity.setInterpolationDelay(0);
                        entity.setTransformation(new Transformation(
                                new Vector3f(-0.5f, targetY - 0.5f, -0.5f),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                t.getScale(),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                    // Cap blocks rise to top
                    List<BlockDisplayHandle> cap = caps.get(g);
                    for (BlockDisplayHandle capH : cap) {
                        float capY = 6.0f * progress;
                        BlockDisplay entity = capH.entity();
                        Transformation t = entity.getTransformation();
                        entity.setInterpolationDuration(3);
                        entity.setInterpolationDelay(0);
                        entity.setTransformation(new Transformation(
                                new Vector3f(-0.5f, capY - 0.5f, -0.5f),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                t.getScale(),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }

                    if (elapsed == 30) {
                        erupted[g] = true;
                        Location impactLoc = c.clone().add(geyserPos[g][0], 3, geyserPos[g][2]);
                        triggerImpactDamage(impactLoc);
                        DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.6f);
                        c.getWorld().spawnParticle(Particle.LAVA, impactLoc, 40, 2, 3, 2, 0.1);
                        c.getWorld().spawnParticle(Particle.FLAME, impactLoc, 25, 1, 2, 1, 0.05);
                    }
                }

                // Post-eruption: ambient particles
                if (erupted[g] && tick % 8 == 0) {
                    Location topLoc = c.clone().add(geyserPos[g][0], 6, geyserPos[g][2]);
                    c.getWorld().spawnParticle(Particle.FLAME, topLoc, 4, 0.3, 0.5, 0.3, 0.02);
                    c.getWorld().spawnParticle(Particle.SMOKE, topLoc, 3, 0.3, 0.5, 0.3, 0.01);
                }
            }

            // Ambient sound
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_POP, 0.5f, 0.7f);
            }
        }

        @Override
        protected void onImpact(Location impactLocation) {
            // Additional impact visual handled in onTick
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new LavaGeyserTriple(plugin); }
    }

    // ================================================================
    // #6 — NETHER CRYSTAL FORMATION
    // 27 blocks: 9 main crystals (scale(0.3,1.8,0.3) each rotated 15-45 deg),
    // 9 smaller crystals, 9 base plates. Crystals rock +/-5 deg via Transform.
    // BLOCK_BEACON_AMBIENT sound. 6r, 20dmg/20t.
    // ================================================================
    public static class NetherCrystalFormation extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> mainCrystals = new ArrayList<>();
        private final List<BlockDisplayHandle> smallCrystals = new ArrayList<>();
        private final List<BlockDisplayHandle> basePlates = new ArrayList<>();
        private final float[] crystalBaseAngles = new float[9];
        private float rockPhase = 0f;

        public NetherCrystalFormation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nether_crystal_formation", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(20.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(280);
            config.setCooldownTicks(220);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            java.util.Random rng = new java.util.Random(7);

            // 9 main crystals in a cluster formation
            double clusterRadius = 2.0;
            for (int i = 0; i < 9; i++) {
                double angle = (2.0 * Math.PI * i) / 9 + (rng.nextDouble() * 0.3);
                double r = clusterRadius * (0.3 + rng.nextDouble() * 0.7);
                double px = Math.cos(angle) * r;
                double pz = Math.sin(angle) * r;
                Location loc = center.clone().add(px, 0.5, pz);

                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                float tiltDeg = 15.0f + rng.nextFloat() * 30.0f; // 15-45 deg
                crystalBaseAngles[i] = (float) Math.toRadians(tiltDeg);
                float axisX = (float) Math.cos(angle);
                float axisZ = (float) Math.sin(angle);
                h.scale(0.3f, 1.8f, 0.3f);
                h.rotate(crystalBaseAngles[i], axisX, 0, axisZ);
                h.glow(200, 40, 10).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                mainCrystals.add(h);
            }

            // 9 smaller crystals interspersed
            for (int i = 0; i < 9; i++) {
                double angle = (2.0 * Math.PI * i) / 9 + Math.PI / 9.0;
                double r = clusterRadius * (0.5 + rng.nextDouble() * 0.5);
                double px = Math.cos(angle) * r;
                double pz = Math.sin(angle) * r;
                Location loc = center.clone().add(px, 0.3, pz);

                Material mat = (i % 2 == 0) ? Material.CRYING_OBSIDIAN : Material.BASALT;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float tiltDeg = 10.0f + rng.nextFloat() * 20.0f;
                float axisX = (float) -Math.sin(angle);
                float axisZ = (float) Math.cos(angle);
                h.scale(0.2f, 1.0f, 0.2f);
                h.rotate((float) Math.toRadians(tiltDeg), axisX, 0, axisZ);
                h.glow(100, 20, 5).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                smallCrystals.add(h);
            }

            // 9 base plates
            for (int i = 0; i < 9; i++) {
                double angle = (2.0 * Math.PI * i) / 9;
                double r = clusterRadius * 0.8;
                double px = Math.cos(angle) * r;
                double pz = Math.sin(angle) * r;
                Location loc = center.clone().add(px, -0.05, pz);

                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.8f, 0.1f, 0.8f).glow(50, 10, 5).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                basePlates.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 1.5f);
            w.spawnParticle(Particle.SMOKE, center.clone().add(0, 1, 0), 20, 2, 1, 2, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Crystal rocking motion +/-5 deg
            rockPhase += 0.08f;
            if (tick % 3 == 0) {
                for (int i = 0; i < mainCrystals.size(); i++) {
                    float rockOffset = (float) Math.toRadians(5.0 * Math.sin(rockPhase + i * 0.7));
                    float totalAngle = crystalBaseAngles[i] + rockOffset;
                    double angle = (2.0 * Math.PI * i) / 9;
                    float axisX = (float) Math.cos(angle);
                    float axisZ = (float) Math.sin(angle);

                    BlockDisplay entity = mainCrystals.get(i).entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(3);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(totalAngle, axisX, 0, axisZ),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Particles: tips of main crystals
            if (tick % 5 == 0) {
                for (int i = 0; i < 3; i++) {
                    int idx = (tick / 5 + i) % mainCrystals.size();
                    Location tipLoc = mainCrystals.get(idx).entity().getLocation().clone().add(0, 1.5, 0);
                    c.getWorld().spawnParticle(Particle.FLAME, tipLoc, 2, 0.1, 0.2, 0.1, 0.01);
                }
            }

            if (tick % 8 == 0) {
                c.getWorld().spawnParticle(Particle.LAVA, c.clone().add(0, 0.5, 0), 4, 2, 0.5, 2, 0.01);
            }

            // Sound
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.4f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new NetherCrystalFormation(plugin); }
    }

    // ================================================================
    // #7 — DOOM RITUAL CIRCLE
    // 35 blocks: 16 outer ring (flat plates R=5), 8 inner ring (R=3),
    // 4 rune pillars (3 blocks each tapered = 12 total, but counted as
    // 4 pillar groups), 3 altar center. Y-rotation. Pillars pulse scale.
    // 7r, 25dmg/20t.
    //
    // Block count: 16 outer + 8 inner + 4*3 pillar = 36; trim pillar to
    // 2 blocks each: 16 + 8 + 4*2 + 3 = 35.
    // ================================================================
    public static class DoomRitualCircle extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> outerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> innerRing = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> runePillars = new ArrayList<>();
        private final List<BlockDisplayHandle> altar = new ArrayList<>();
        private float yRotation = 0f;
        private float pillarPulse = 0f;

        public DoomRitualCircle(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_ritual_circle", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(25.0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(320);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Outer ring: 16 flat plates at R=5
            for (int i = 0; i < 16; i++) {
                double angle = (2.0 * Math.PI * i) / 16;
                double px = Math.cos(angle) * 5.0;
                double pz = Math.sin(angle) * 5.0;
                Location loc = center.clone().add(px, 0.05, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHER_BRICKS);
                h.scale(0.8f, 0.08f, 0.8f).glow(200, 40, 10).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                outerRing.add(h);
            }

            // Inner ring: 8 flat plates at R=3
            for (int i = 0; i < 8; i++) {
                double angle = (2.0 * Math.PI * i) / 8 + Math.toRadians(22.5);
                double px = Math.cos(angle) * 3.0;
                double pz = Math.sin(angle) * 3.0;
                Location loc = center.clone().add(px, 0.05, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(0.8f, 0.08f, 0.8f).glow(255, 80, 20).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                innerRing.add(h);
            }

            // 4 rune pillars at cardinal directions, R=4, 2 blocks each tapered
            double[][] pillarPositions = {{4, 0, 0}, {0, 0, 4}, {-4, 0, 0}, {0, 0, -4}};
            for (int p = 0; p < 4; p++) {
                List<BlockDisplayHandle> pillar = new ArrayList<>();
                for (int y = 0; y < 2; y++) {
                    float taper = 0.6f - y * 0.15f;
                    Location loc = center.clone().add(pillarPositions[p][0], y * 1.2 + 0.1, pillarPositions[p][2]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                    h.scale(taper, 1.2f, taper).glow(100, 20, 5).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    pillar.add(h);
                }
                // Pillar cap (shroomlight)
                Location capLoc = center.clone().add(pillarPositions[p][0], 2.5, pillarPositions[p][2]);
                BlockDisplayHandle cap = displayBuilder.spawnBlock(capLoc, Material.SHROOMLIGHT);
                cap.scale(0.35f, 0.3f, 0.35f).glow(255, 150, 50).interpolation(3, 0);
                spawnedEntities.add(cap.entity());
                pillar.add(cap);
                runePillars.add(pillar);
            }

            // Altar center: 3 blocks
            BlockDisplayHandle altarBase = displayBuilder.spawnBlock(center.clone().add(0, 0.1, 0), Material.OBSIDIAN);
            altarBase.scale(1.0f, 0.5f, 1.0f).glow(50, 10, 5).interpolation(3, 0);
            spawnedEntities.add(altarBase.entity());
            altar.add(altarBase);

            BlockDisplayHandle altarMid = displayBuilder.spawnBlock(center.clone().add(0, 0.6, 0), Material.CRYING_OBSIDIAN);
            altarMid.scale(0.7f, 0.4f, 0.7f).glow(200, 40, 10).interpolation(3, 0);
            spawnedEntities.add(altarMid.entity());
            altar.add(altarMid);

            BlockDisplayHandle altarTop = displayBuilder.spawnBlock(center.clone().add(0, 1.0, 0), Material.GLOWSTONE);
            altarTop.scale(0.4f, 0.3f, 0.4f).glow(255, 150, 50).interpolation(3, 0);
            spawnedEntities.add(altarTop.entity());
            altar.add(altarTop);

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 0.5f);
            w.spawnParticle(Particle.FLAME, center.clone().add(0, 0.3, 0), 30, 5, 0.2, 5, 0.01);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Y-rotation for rings
            yRotation += 0.02f;
            if (tick % 3 == 0) {
                List<BlockDisplayHandle> allRingBlocks = new ArrayList<>();
                allRingBlocks.addAll(outerRing);
                allRingBlocks.addAll(innerRing);
                for (BlockDisplayHandle h : allRingBlocks) {
                    BlockDisplay entity = h.entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(3);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(yRotation, 0f, 1f, 0f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Pillar scale pulse
            pillarPulse += 0.06f;
            if (tick % 3 == 0) {
                float pulse = 1.0f + 0.15f * (float) Math.sin(pillarPulse);
                for (List<BlockDisplayHandle> pillar : runePillars) {
                    for (BlockDisplayHandle h : pillar) {
                        BlockDisplay entity = h.entity();
                        Transformation t = entity.getTransformation();
                        Vector3f oldScale = t.getScale();
                        entity.setInterpolationDuration(3);
                        entity.setInterpolationDelay(0);
                        entity.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                new Vector3f(oldScale.x * pulse / (pulse - 0.15f * (float) Math.sin(pillarPulse - 0.06f) + 0.001f),
                                        oldScale.y, oldScale.z),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                }
            }

            // Ring particles
            if (tick % 4 == 0) {
                double pAngle = tick * 0.15;
                Location outerP = c.clone().add(Math.cos(pAngle) * 5.0, 0.3, Math.sin(pAngle) * 5.0);
                Location innerP = c.clone().add(Math.cos(-pAngle) * 3.0, 0.3, Math.sin(-pAngle) * 3.0);
                c.getWorld().spawnParticle(Particle.FLAME, outerP, 2, 0.1, 0.05, 0.1, 0.01);
                c.getWorld().spawnParticle(Particle.FLAME, innerP, 2, 0.1, 0.05, 0.1, 0.01);
            }

            // Altar glow particles
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.LAVA, c.clone().add(0, 1.5, 0), 3, 0.3, 0.3, 0.3, 0.01);
            }

            // Sound
            if (tick % 45 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new DoomRitualCircle(plugin); }
    }

    // ================================================================
    // #8 — MOLTEN SERPENT COIL
    // 30 blocks: 20 helix segments spiraling Y=0->6 (3 revolutions,
    // scale(0.7,0.5,0.7)), head (3 larger), tail (2 thin), 5 crest
    // accents. Helix Y-rotation via Transform. Head tracks nearest player.
    // 5r along body, 35dmg/20t.
    // ================================================================
    public static class MoltenSerpentCoil extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> helixBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> headBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> tailBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> crestBlocks = new ArrayList<>();
        private float helixRotation = 0f;

        public MoltenSerpentCoil(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("molten_serpent_coil", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(35.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(320);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double helixRadius = 2.0;

            // 20 helix segments: 3 full revolutions over Y=0->6
            for (int i = 0; i < 20; i++) {
                double t = i / 19.0;
                double angle = t * 3.0 * 2.0 * Math.PI;
                double yPos = t * 6.0;
                double px = Math.cos(angle) * helixRadius;
                double pz = Math.sin(angle) * helixRadius;
                Location loc = center.clone().add(px, yPos, pz);
                Material mat = (i % 3 == 0) ? Material.MAGMA_BLOCK : (i % 3 == 1) ? Material.NETHERRACK : Material.NETHER_BRICKS;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.7f, 0.5f, 0.7f).glow(255, 80, 20).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                helixBlocks.add(h);
            }

            // Head: 3 larger blocks at top of helix
            double headAngle = 3.0 * 2.0 * Math.PI;
            for (int i = 0; i < 3; i++) {
                double ha = headAngle + i * 0.3;
                double px = Math.cos(ha) * (helixRadius - 0.3);
                double pz = Math.sin(ha) * (helixRadius - 0.3);
                Location loc = center.clone().add(px, 6.0 + i * 0.5, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                float headScale = 1.0f - i * 0.15f;
                h.scale(headScale, 0.7f, headScale).glow(255, 150, 50).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                headBlocks.add(h);
            }

            // Tail: 2 thin blocks at bottom
            for (int i = 0; i < 2; i++) {
                double ta = i * 0.4;
                double px = Math.cos(ta) * helixRadius;
                double pz = Math.sin(ta) * helixRadius;
                Location loc = center.clone().add(px, -0.3 - i * 0.4, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BASALT);
                h.scale(0.3f, 0.8f, 0.3f).glow(100, 20, 5).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                tailBlocks.add(h);
            }

            // 5 crest accents along the helix
            for (int i = 0; i < 5; i++) {
                int idx = i * 4; // Every 4th helix segment
                double t = idx / 19.0;
                double angle = t * 3.0 * 2.0 * Math.PI;
                double yPos = t * 6.0 + 0.6;
                double px = Math.cos(angle) * (helixRadius + 0.5);
                double pz = Math.sin(angle) * (helixRadius + 0.5);
                Location loc = center.clone().add(px, yPos, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SHROOMLIGHT);
                h.scale(0.25f, 0.4f, 0.25f).glow(255, 150, 50).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                crestBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.4f);
            w.spawnParticle(Particle.FLAME, center.clone().add(0, 3, 0), 30, 2, 3, 2, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Helix Y-rotation via Transformation
            helixRotation += 0.04f;
            if (tick % 2 == 0) {
                List<BlockDisplayHandle> allBody = new ArrayList<>();
                allBody.addAll(helixBlocks);
                allBody.addAll(tailBlocks);
                allBody.addAll(crestBlocks);
                for (BlockDisplayHandle h : allBody) {
                    BlockDisplay entity = h.entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(2);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(helixRotation, 0f, 1f, 0f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Head tracks nearest player via translation offset
            if (tick % 4 == 0) {
                Player target = findNearestPlayer(c, 15.0);
                if (target != null) {
                    Vector dir = target.getLocation().toVector().subtract(c.toVector()).normalize();
                    float headOffX = (float) (dir.getX() * 0.5);
                    float headOffZ = (float) (dir.getZ() * 0.5);
                    for (int i = 0; i < headBlocks.size(); i++) {
                        BlockDisplay entity = headBlocks.get(i).entity();
                        Transformation t = entity.getTransformation();
                        float baseY = 6.0f + i * 0.5f - 0.5f;
                        entity.setInterpolationDuration(4);
                        entity.setInterpolationDelay(0);
                        entity.setTransformation(new Transformation(
                                new Vector3f(headOffX - 0.5f, baseY, headOffZ - 0.5f),
                                new AxisAngle4f(helixRotation, 0f, 1f, 0f),
                                t.getScale(),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                }
            }

            // Particles along body
            if (tick % 4 == 0) {
                int segIdx = (tick / 4) % helixBlocks.size();
                Location segLoc = helixBlocks.get(segIdx).entity().getLocation();
                c.getWorld().spawnParticle(Particle.FLAME, segLoc, 3, 0.2, 0.2, 0.2, 0.01);
            }

            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(0, 6.5, 0), 4, 0.5, 0.3, 0.5, 0.01);
                c.getWorld().spawnParticle(Particle.LAVA, c.clone().add(0, 3, 0), 3, 1.5, 2, 1.5, 0.01);
            }

            // Sound
            if (tick % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_POP, 0.6f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new MoltenSerpentCoil(plugin); }
    }

    // ================================================================
    // #9 — INFERNAL CRUCIBLE
    // 28 blocks: 12 wall blocks in circle (scale(0.6,1.2,0.3) tilted 15 deg
    // outward), 4 legs, 8 lava surface (magma plates Y=1, Y-scale pulse for
    // bubbling), 4 rim blocks. Breathing walls. LAVA_POP sound. 6r, 20dmg/15t.
    // ================================================================
    public static class InfernalCrucible extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> wallBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> legBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> lavaBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> rimBlocks = new ArrayList<>();
        private float breathPhase = 0f;
        private float bubblePhase = 0f;

        public InfernalCrucible(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_crucible", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(20.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(260);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double wallRadius = 2.0;

            // 12 wall blocks forming a cylinder, tilted 15 deg outward
            for (int i = 0; i < 12; i++) {
                double angle = (2.0 * Math.PI * i) / 12;
                double px = Math.cos(angle) * wallRadius;
                double pz = Math.sin(angle) * wallRadius;
                Location loc = center.clone().add(px, 0.5, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHER_BRICKS);
                float tiltAngle = (float) Math.toRadians(15);
                float axisX = (float) Math.cos(angle);
                float axisZ = (float) Math.sin(angle);
                h.scale(0.6f, 1.2f, 0.3f);
                h.rotate(tiltAngle, axisX, 0, axisZ);
                h.glow(200, 40, 10).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                wallBlocks.add(h);
            }

            // 4 legs at cardinal positions
            double[][] legPos = {{-1.8, -0.5, 0}, {1.8, -0.5, 0}, {0, -0.5, -1.8}, {0, -0.5, 1.8}};
            for (double[] pos : legPos) {
                Location loc = center.clone().add(pos[0], pos[1], pos[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.4f, 0.6f, 0.4f).glow(100, 20, 5).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                legBlocks.add(h);
            }

            // 8 lava surface blocks (magma plates at Y=1)
            for (int i = 0; i < 8; i++) {
                double angle = (2.0 * Math.PI * i) / 8;
                double r = 1.2;
                double px = Math.cos(angle) * r;
                double pz = Math.sin(angle) * r;
                Location loc = center.clone().add(px, 1.0, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(0.7f, 0.15f, 0.7f).glow(255, 80, 20).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                lavaBlocks.add(h);
            }

            // 4 rim blocks
            double[][] rimPos = {{-2.2, 1.5, 0}, {2.2, 1.5, 0}, {0, 1.5, -2.2}, {0, 1.5, 2.2}};
            for (double[] pos : rimPos) {
                Location loc = center.clone().add(pos[0], pos[1], pos[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BASALT);
                h.scale(0.5f, 0.2f, 0.5f).glow(100, 20, 5).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                rimBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 1.0f, 0.6f);
            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 0.4f);
            w.spawnParticle(Particle.LAVA, center.clone().add(0, 1.2, 0), 20, 1.5, 0.5, 1.5, 0.03);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Breathing walls: scale pulse on X/Z
            breathPhase += 0.07f;
            if (tick % 2 == 0) {
                float breathScale = 1.0f + 0.08f * (float) Math.sin(breathPhase);
                for (int i = 0; i < wallBlocks.size(); i++) {
                    BlockDisplay entity = wallBlocks.get(i).entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(2);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(0.6f * breathScale, 1.2f, 0.3f * breathScale),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Lava surface bubbling: Y-scale pulse
            bubblePhase += 0.12f;
            if (tick % 3 == 0) {
                for (int i = 0; i < lavaBlocks.size(); i++) {
                    float bubble = 0.15f + 0.1f * (float) Math.sin(bubblePhase + i * 0.8f);
                    BlockDisplay entity = lavaBlocks.get(i).entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(3);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(0.7f, bubble, 0.7f),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Particles: steam/smoke from lava surface
            if (tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(0, 1.3, 0), 4, 1.0, 0.3, 1.0, 0.02);
                c.getWorld().spawnParticle(Particle.LANDING_LAVA, c.clone().add(0, 1.2, 0), 3, 1.0, 0.1, 1.0, 0);
            }

            if (tick % 8 == 0) {
                c.getWorld().spawnParticle(Particle.FLAME, c.clone().add(0, 1.5, 0), 5, 0.8, 0.3, 0.8, 0.015);
            }

            // Sound
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_POP, 0.5f, 0.8f + (float) (Math.random() * 0.4));
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new InfernalCrucible(plugin); }
    }

    // ================================================================
    // #10 — BRIMSTONE STAIRCASE SPIRAL
    // 32 blocks: 24 steps ascending (scale(1.2,0.15,0.6) plates, increasing
    // Y + angle), 4 center pillar, 4 railing accents. Y-rotation.
    // Sequential glow illumination. 5r, 25dmg/25t.
    // ================================================================
    public static class BrimstoneStaircaseSpiral extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> steps = new ArrayList<>();
        private final List<BlockDisplayHandle> pillar = new ArrayList<>();
        private final List<BlockDisplayHandle> railing = new ArrayList<>();
        private float yRotation = 0f;
        private int glowIndex = 0;

        public BrimstoneStaircaseSpiral(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_staircase_spiral", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(25.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(340);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double stairRadius = 3.0;

            // 24 ascending steps in a spiral
            for (int i = 0; i < 24; i++) {
                double angle = (2.0 * Math.PI * i) / 12.0; // 2 full revolutions over 24 steps
                double yPos = i * 0.35;
                double px = Math.cos(angle) * stairRadius;
                double pz = Math.sin(angle) * stairRadius;
                Location loc = center.clone().add(px, yPos, pz);
                Material mat = (i % 4 == 0) ? Material.MAGMA_BLOCK : (i % 4 == 2) ? Material.NETHER_BRICKS : Material.BLACKSTONE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(1.2f, 0.15f, 0.6f);
                // Rotate step to face outward from center
                float stepAngle = (float) angle;
                h.rotate(stepAngle, 0, 1, 0);
                h.glow(100, 20, 5).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                steps.add(h);
            }

            // Center pillar: 4 blocks stacked
            for (int y = 0; y < 4; y++) {
                Location loc = center.clone().add(0, y * 2.0 + 0.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BASALT);
                h.scale(0.5f, 2.0f, 0.5f).glow(100, 20, 5).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                pillar.add(h);
            }

            // 4 railing accents at quarter-turn positions
            for (int i = 0; i < 4; i++) {
                int stepIdx = i * 6; // Every 6th step
                double angle = (2.0 * Math.PI * stepIdx) / 12.0;
                double yPos = stepIdx * 0.35 + 0.5;
                double px = Math.cos(angle) * (stairRadius + 0.5);
                double pz = Math.sin(angle) * (stairRadius + 0.5);
                Location loc = center.clone().add(px, yPos, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SHROOMLIGHT);
                h.scale(0.2f, 0.6f, 0.2f).glow(255, 150, 50).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                railing.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.7f, 0.4f);
            w.spawnParticle(Particle.SMOKE, center.clone().add(0, 4, 0), 20, 2, 3, 2, 0.03);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Y-rotation for entire structure via Transformation
            yRotation += 0.025f;
            if (tick % 3 == 0) {
                List<BlockDisplayHandle> allBlocks = new ArrayList<>();
                allBlocks.addAll(steps);
                allBlocks.addAll(pillar);
                allBlocks.addAll(railing);
                for (BlockDisplayHandle h : allBlocks) {
                    BlockDisplay entity = h.entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(3);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(yRotation, 0f, 1f, 0f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Sequential glow illumination: light up steps one by one
            if (tick % 5 == 0) {
                glowIndex = (glowIndex + 1) % steps.size();
                for (int i = 0; i < steps.size(); i++) {
                    if (i == glowIndex || i == (glowIndex + 1) % steps.size() || i == (glowIndex + 2) % steps.size()) {
                        steps.get(i).glow(255, 150, 50);
                    } else {
                        steps.get(i).glow(100, 20, 5);
                    }
                }
            }

            // Particles: flame trail along illuminated steps
            if (tick % 4 == 0) {
                for (int offset = 0; offset < 3; offset++) {
                    int idx = (glowIndex + offset) % steps.size();
                    Location stepLoc = steps.get(idx).entity().getLocation();
                    c.getWorld().spawnParticle(Particle.FLAME, stepLoc.clone().add(0, 0.3, 0), 2, 0.2, 0.1, 0.2, 0.01);
                }
            }

            // Center pillar smoke
            if (tick % 8 == 0) {
                c.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(0, 8, 0), 5, 0.3, 0.5, 0.3, 0.01);
            }

            if (tick % 10 == 0) {
                c.getWorld().spawnParticle(Particle.LAVA, c.clone().add(0, 4, 0), 3, 2, 3, 2, 0.01);
            }

            // Sound
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 0.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneStaircaseSpiral(plugin); }
    }
}
