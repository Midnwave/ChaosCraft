package com.blockforge.chaoscraft.modes.corruption.attacks;

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
 * Corruption Mode — GLITCH ANOMALIES (Void Rifts & Tears)
 * 15 corruption-themed BlockDisplay attacks featuring dimensional
 * tears, void rifts, and reality fractures.
 *
 * Color palette:
 * - Dark purple: RGB(45, 0, 64)
 * - Crimson: RGB(74, 0, 0)
 * - Void blue: RGB(10, 0, 48)
 *
 * Materials: SCULK, DEEPSLATE, BLACKSTONE, CRYING_OBSIDIAN, OBSIDIAN, COAL_BLOCK, TINTED_GLASS
 * Sounds: BLOCK_SCULK_SPREAD, BLOCK_SCULK_BREAK, BLOCK_DEEPSLATE_BREAK, ENTITY_WARDEN_HEARTBEAT,
 *         BLOCK_RESPAWN_ANCHOR_DEPLETE
 */
public final class GlitchAnomalies {

    private GlitchAnomalies() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidRift(plugin));
        registry.register(new RealityCrack(plugin));
        registry.register(new CorruptionPortal(plugin));
        registry.register(new DimensionalTear(plugin));
        registry.register(new VoidEye(plugin));
        registry.register(new RealityFracture(plugin));
        registry.register(new DarkSingularity(plugin));
        registry.register(new CorruptionWormhole(plugin));
        registry.register(new ShadowGate(plugin));
        registry.register(new VoidMaw(plugin));
        registry.register(new RealityGlitch(plugin));
        registry.register(new CorruptionSinkhole(plugin));
        registry.register(new DarkMirror(plugin));
        registry.register(new VoidBubble(plugin));
        registry.register(new RealityScar(plugin));
    }

    // ================================================================
    // 31. VOID RIFT — 16 blocks form a vertical tear in space,
    //     jagged edges glow dark purple, interior is void-dark
    // ================================================================
    public static class VoidRift extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> edgeBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> interiorBlocks = new ArrayList<>();
        private static final int EDGE_COUNT = 10;
        private static final int INTERIOR_COUNT = 6;

        public VoidRift(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_rift", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Jagged vertical edges — sculk and deepslate in a zig-zag line
            for (int i = 0; i < EDGE_COUNT; i++) {
                double yOff = -2.0 + (4.0 / (EDGE_COUNT - 1)) * i;
                double xJag = (i % 2 == 0 ? 0.6 : -0.6) + (Math.random() * 0.3 - 0.15);
                Location loc = center.clone().add(xJag, yOff + 2.0, 0);
                Material mat = (i % 2 == 0) ? Material.SCULK : Material.DEEPSLATE;
                BlockDisplayHandle edge = displayBuilder.spawnBlock(loc, mat);
                edge.scale(0.4f, 0.5f, 0.3f)
                    .glow(45, 0, 64)
                    .interpolation(3, 0);
                edgeBlocks.add(edge);
                spawnedEntities.add(edge.entity());
            }

            // Dark interior — coal and obsidian filling the rift center
            for (int i = 0; i < INTERIOR_COUNT; i++) {
                double yOff = -1.5 + (3.0 / (INTERIOR_COUNT - 1)) * i;
                Location loc = center.clone().add(0, yOff + 2.0, 0);
                Material mat = (i % 2 == 0) ? Material.COAL_BLOCK : Material.OBSIDIAN;
                BlockDisplayHandle interior = displayBuilder.spawnBlock(loc, mat);
                interior.scale(0.5f, 0.6f, 0.15f)
                        .glow(10, 0, 48)
                        .interpolation(3, 0);
                interiorBlocks.add(interior);
                spawnedEntities.add(interior.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.2f, 0.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Edges pulse — scale oscillation
            float pulse = 1.0f + 0.15f * (float) Math.sin(ticksAlive * 0.12);
            for (int i = 0; i < edgeBlocks.size(); i++) {
                BlockDisplayHandle edge = edgeBlocks.get(i);
                float jitter = (float) Math.sin(ticksAlive * 0.2 + i * 0.7) * 0.08f;
                edge.scale(0.4f * pulse + jitter, 0.5f * pulse, 0.3f);
                edge.rotate(jitter * 0.5f, 0, 0, 1);
            }

            // Interior flicker — slight scale and rotation shifts
            for (int i = 0; i < interiorBlocks.size(); i++) {
                BlockDisplayHandle block = interiorBlocks.get(i);
                float flicker = (float) Math.sin(ticksAlive * 0.3 + i) * 0.05f;
                block.scale(0.5f + flicker, 0.6f, 0.15f);
            }

            // Void particles leaking from rift
            if (ticksAlive % 3 == 0) {
                Location riftCenter = c.clone().add(0, 2, 0);
                DisplayBuilder.dustParticles(riftCenter, 4, 0.6, 45, 0, 64, 1.3f);
                DisplayBuilder.dustParticles(riftCenter, 2, 0.3, 10, 0, 48, 1.0f);
            }

            // Deep heartbeat sound
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.5f);
            }

            // Sculk ambient
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidRift(plugin); }
    }

    // ================================================================
    // 32. REALITY CRACK — 14 blocks form a jagged crack in midair,
    //     dark particles leak from the seam
    // ================================================================
    public static class RealityCrack extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> crackBlocks = new ArrayList<>();
        private final List<Double> crackXOffsets = new ArrayList<>();
        private final List<Double> crackYOffsets = new ArrayList<>();
        private static final int BLOCK_COUNT = 14;

        public RealityCrack(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("reality_crack", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(280);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Jagged diagonal crack — blocks placed along an erratic line
            double baseAngle = Math.random() * Math.PI;
            for (int i = 0; i < BLOCK_COUNT; i++) {
                double t = (i - BLOCK_COUNT / 2.0) * 0.5;
                double xOff = Math.cos(baseAngle) * t + (Math.random() * 0.5 - 0.25);
                double yOff = Math.sin(baseAngle) * t * 0.4 + (Math.random() * 0.3 - 0.15);
                crackXOffsets.add(xOff);
                crackYOffsets.add(yOff);

                Location loc = center.clone().add(xOff, 2.5 + yOff, 0);
                Material mat;
                if (i % 3 == 0) mat = Material.BLACKSTONE;
                else if (i % 3 == 1) mat = Material.DEEPSLATE;
                else mat = Material.CRYING_OBSIDIAN;

                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                block.scale(0.35f, 0.35f, 0.2f)
                     .glow(45, 0, 64)
                     .interpolation(2, 0);
                // Jagged rotation for each piece
                float angle = (float) (Math.random() * 0.8 - 0.4);
                block.rotate(angle, 0, 0, 1);
                crackBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Crack widens over time — blocks drift apart
            float widening = Math.min(ticksAlive * 0.003f, 0.4f);
            for (int i = 0; i < crackBlocks.size(); i++) {
                BlockDisplayHandle block = crackBlocks.get(i);
                double xOff = crackXOffsets.get(i);
                double yOff = crackYOffsets.get(i);
                // Blocks drift perpendicular to crack direction
                double perpX = (i % 2 == 0 ? 1 : -1) * widening;
                double perpY = (i % 2 == 0 ? 0.3 : -0.3) * widening;
                Location newLoc = c.clone().add(xOff + perpX, 2.5 + yOff + perpY, 0);
                block.entity().teleport(newLoc);

                // Slight vibration
                float vibration = (float) Math.sin(ticksAlive * 0.25 + i) * 0.06f;
                block.rotate(vibration, 0, 0, 1);
            }

            // Particles leaking from crack seam
            if (ticksAlive % 2 == 0) {
                Location seam = c.clone().add(0, 2.5, 0);
                DisplayBuilder.dustParticles(seam, 5, 1.0, 74, 0, 0, 1.2f);
                DisplayBuilder.dustParticles(seam, 3, 0.5, 10, 0, 48, 0.8f);
            }

            // Periodic crack sounds
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, 0.7f, 0.4f + (float)(Math.random() * 0.3));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RealityCrack(plugin); }
    }

    // ================================================================
    // 33. CORRUPTION PORTAL — 12 blocks form a ring, swirling
    //     dark particles spiral inside the opening
    // ================================================================
    public static class CorruptionPortal extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ringBlocks = new ArrayList<>();
        private static final int BLOCK_COUNT = 12;
        private static final double RING_RADIUS = 2.0;

        public CorruptionPortal(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_portal", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(46.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(320);
            config.setCooldownTicks(340);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Ring of blocks forming a vertical portal
            for (int i = 0; i < BLOCK_COUNT; i++) {
                double angle = (2 * Math.PI * i) / BLOCK_COUNT;
                double xOff = Math.cos(angle) * RING_RADIUS;
                double yOff = Math.sin(angle) * RING_RADIUS;
                Location loc = center.clone().add(xOff, 2.5 + yOff, 0);
                Material mat = (i % 3 == 0) ? Material.CRYING_OBSIDIAN
                             : (i % 3 == 1) ? Material.SCULK
                             : Material.BLACKSTONE;
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                block.scale(0.6f, 0.6f, 0.3f)
                     .glow(45, 0, 64)
                     .interpolation(3, 0);
                // Face outward from ring center
                float rotAngle = (float) angle;
                block.rotate(rotAngle, 0, 0, 1);
                ringBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 0.9f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Rotate entire ring slowly
            float ringRotation = ticksAlive * 0.04f;
            for (int i = 0; i < ringBlocks.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / BLOCK_COUNT + ringRotation;
                double xOff = Math.cos(baseAngle) * RING_RADIUS;
                double yOff = Math.sin(baseAngle) * RING_RADIUS;
                Location newLoc = c.clone().add(xOff, 2.5 + yOff, 0);
                ringBlocks.get(i).entity().teleport(newLoc);
                ringBlocks.get(i).rotate((float) baseAngle, 0, 0, 1);
            }

            // Swirling particles inside the portal
            if (ticksAlive % 2 == 0) {
                for (int j = 0; j < 3; j++) {
                    double pAngle = ticksAlive * 0.15 + j * (Math.PI * 2 / 3);
                    double pRadius = 0.8 + Math.sin(ticksAlive * 0.1) * 0.4;
                    double px = Math.cos(pAngle) * pRadius;
                    double py = Math.sin(pAngle) * pRadius;
                    Location particleLoc = c.clone().add(px, 2.5 + py, 0);
                    DisplayBuilder.dustParticles(particleLoc, 3, 0.15, 45, 0, 64, 1.4f);
                }
                // Center void particle
                DisplayBuilder.dustParticles(c.clone().add(0, 2.5, 0), 2, 0.2, 10, 0, 48, 1.8f);
            }

            // Eerie hum
            if (ticksAlive % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionPortal(plugin); }
    }

    // ================================================================
    // 34. DIMENSIONAL TEAR — 15 blocks in X-shape, opens wider
    //     over duration, then snaps shut at end
    // ================================================================
    public static class DimensionalTear extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> armBlocks = new ArrayList<>();
        private final List<Double> baseXOffsets = new ArrayList<>();
        private final List<Double> baseYOffsets = new ArrayList<>();
        private static final int BLOCK_COUNT = 15;
        private boolean snapping = false;

        public DimensionalTear(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dimensional_tear", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(48.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // X-shape: 4 arms radiating from center, plus a center block
            // Each arm has ~3-4 blocks
            int idx = 0;
            double[][] armDirs = { {1, 1}, {1, -1}, {-1, 1}, {-1, -1} };
            for (double[] dir : armDirs) {
                for (int j = 1; j <= 3; j++) {
                    if (idx >= BLOCK_COUNT - 3) break;
                    double xOff = dir[0] * j * 0.7;
                    double yOff = dir[1] * j * 0.7;
                    baseXOffsets.add(xOff);
                    baseYOffsets.add(yOff);

                    Location loc = center.clone().add(xOff, 2.5 + yOff, 0);
                    Material mat = (j == 1) ? Material.CRYING_OBSIDIAN
                                 : (j == 2) ? Material.DEEPSLATE
                                 : Material.SCULK;
                    BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                    block.scale(0.45f, 0.45f, 0.25f)
                         .glow(74, 0, 0)
                         .interpolation(3, 0);
                    float rot = (float) Math.atan2(dir[1], dir[0]);
                    block.rotate(rot, 0, 0, 1);
                    armBlocks.add(block);
                    spawnedEntities.add(block.entity());
                    idx++;
                }
            }

            // Center cluster — 3 blocks
            for (int i = 0; i < 3; i++) {
                double ox = (Math.random() * 0.3 - 0.15);
                double oy = (Math.random() * 0.3 - 0.15);
                baseXOffsets.add(ox);
                baseYOffsets.add(oy);
                Location loc = center.clone().add(ox, 2.5 + oy, 0);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                block.scale(0.5f, 0.5f, 0.3f)
                     .glow(45, 0, 64)
                     .interpolation(3, 0);
                armBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_BREAK, 1.2f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            int duration = config.getDurationTicks();
            // Last 20 ticks: snap shut
            boolean isSnapping = ticksAlive > (duration - 20);

            float openFactor;
            if (isSnapping) {
                // Rapidly close: 1.0 → 0.0 over 20 ticks
                openFactor = Math.max(0, 1.0f - (ticksAlive - (duration - 20)) / 20.0f);
                if (!snapping) {
                    snapping = true;
                    DisplayBuilder.playSound(c, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.5f, 1.5f);
                }
            } else {
                // Gradually open: 1.0 → 2.0 over the main duration
                openFactor = 1.0f + Math.min(ticksAlive / (float)(duration - 20), 1.0f);
            }

            for (int i = 0; i < armBlocks.size(); i++) {
                double bx = baseXOffsets.get(i) * openFactor;
                double by = baseYOffsets.get(i) * openFactor;
                Location newLoc = c.clone().add(bx, 2.5 + by, 0);
                armBlocks.get(i).entity().teleport(newLoc);

                // Vibration during opening
                float vibration = (float) Math.sin(ticksAlive * 0.2 + i) * 0.04f * openFactor;
                armBlocks.get(i).rotate(vibration, 0, 0, 1);
            }

            // Crimson particles along the tear
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 2.5, 0), 6, 1.5 * openFactor, 74, 0, 0, 1.3f);
            }

            // Snap shockwave on final close
            if (isSnapping && ticksAlive == duration - 1) {
                DisplayBuilder.particleRing(c.clone().add(0, 2.5, 0), 4.0, Particle.DUST, 32,
                        new Particle.DustOptions(Color.fromRGB(45, 0, 64), 2.0f));
                DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.3f);
            }

            if (ticksAlive % 30 == 0 && !isSnapping) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DimensionalTear(plugin); }
    }

    // ================================================================
    // 35. VOID EYE — 16 blocks form an eye shape, the central
    //     pupil rotates to track the nearest player
    // ================================================================
    public static class VoidEye extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> outerBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> pupilBlocks = new ArrayList<>();
        private static final int OUTER_COUNT = 12;
        private static final int PUPIL_COUNT = 4;
        private double pupilAngle = 0;

        public VoidEye(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_eye", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(360);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Outer eye shape — elliptical ring
            for (int i = 0; i < OUTER_COUNT; i++) {
                double angle = (2 * Math.PI * i) / OUTER_COUNT;
                double xOff = Math.cos(angle) * 2.5;
                double yOff = Math.sin(angle) * 1.2; // Squashed vertically for eye shape
                Location loc = center.clone().add(xOff, 2.5 + yOff, 0);
                Material mat = (i % 2 == 0) ? Material.SCULK : Material.DEEPSLATE;
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                block.scale(0.5f, 0.4f, 0.25f)
                     .glow(45, 0, 64)
                     .interpolation(3, 0);
                block.rotate((float) angle, 0, 0, 1);
                outerBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            // Pupil — 4 obsidian blocks clustered at center
            for (int i = 0; i < PUPIL_COUNT; i++) {
                double angle = (2 * Math.PI * i) / PUPIL_COUNT;
                double xOff = Math.cos(angle) * 0.35;
                double yOff = Math.sin(angle) * 0.35;
                Location loc = center.clone().add(xOff, 2.5 + yOff, 0);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                block.scale(0.4f, 0.4f, 0.3f)
                     .glow(10, 0, 48)
                     .interpolation(2, 0);
                pupilBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Find nearest player for pupil tracking
            Location eyeCenter = c.clone().add(0, 2.5, 0);
            Player nearest = null;
            double nearestDist = Double.MAX_VALUE;
            for (Player player : w.getPlayers()) {
                double dist = player.getLocation().distanceSquared(eyeCenter);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = player;
                }
            }

            // Pupil tracks nearest player
            double pupilOffX = 0, pupilOffY = 0;
            if (nearest != null) {
                Location pLoc = nearest.getLocation();
                double dx = pLoc.getX() - eyeCenter.getX();
                double dy = pLoc.getY() - eyeCenter.getY();
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist > 0.1) {
                    pupilOffX = (dx / dist) * 0.7;
                    pupilOffY = (dy / dist) * 0.4;
                }
                pupilAngle = Math.atan2(dy, dx);
            }

            // Move pupil blocks toward tracked direction
            for (int i = 0; i < PUPIL_COUNT; i++) {
                double angle = (2 * Math.PI * i) / PUPIL_COUNT;
                double xOff = Math.cos(angle) * 0.35 + pupilOffX;
                double yOff = Math.sin(angle) * 0.35 + pupilOffY;
                Location newLoc = c.clone().add(xOff, 2.5 + yOff, 0);
                pupilBlocks.get(i).entity().teleport(newLoc);
                pupilBlocks.get(i).rotate((float) pupilAngle, 0, 0, 1);
            }

            // Outer eye subtle pulse
            float pulse = 1.0f + 0.08f * (float) Math.sin(ticksAlive * 0.08);
            for (int i = 0; i < outerBlocks.size(); i++) {
                double angle = (2 * Math.PI * i) / OUTER_COUNT;
                double xOff = Math.cos(angle) * 2.5 * pulse;
                double yOff = Math.sin(angle) * 1.2 * pulse;
                Location newLoc = c.clone().add(xOff, 2.5 + yOff, 0);
                outerBlocks.get(i).entity().teleport(newLoc);
            }

            // Eerie watching particles
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(eyeCenter.clone().add(pupilOffX, pupilOffY, 0), 3, 0.3, 74, 0, 0, 1.0f);
            }

            // Heartbeat — speeds up when player is close
            int heartRate = (nearestDist < 25) ? 20 : 40;
            if (ticksAlive % heartRate == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.7f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidEye(plugin); }
    }

    // ================================================================
    // 36. REALITY FRACTURE — 20 small blocks scattered midair
    //     like shattered glass floating in suspension
    // ================================================================
    public static class RealityFracture extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> shards = new ArrayList<>();
        private final List<Double> xPos = new ArrayList<>();
        private final List<Double> yPos = new ArrayList<>();
        private final List<Double> zPos = new ArrayList<>();
        private final List<Float> spinRates = new ArrayList<>();
        private final List<Float> driftSpeeds = new ArrayList<>();
        private static final int SHARD_COUNT = 20;

        public RealityFracture(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("reality_fracture", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < SHARD_COUNT; i++) {
                double ox = (Math.random() - 0.5) * 6.0;
                double oy = 1.0 + Math.random() * 4.0;
                double oz = (Math.random() - 0.5) * 6.0;
                xPos.add(ox);
                yPos.add(oy);
                zPos.add(oz);
                spinRates.add(0.05f + (float)(Math.random() * 0.15));
                driftSpeeds.add(0.01f + (float)(Math.random() * 0.02));

                Location loc = center.clone().add(ox, oy, oz);
                Material mat;
                int matIdx = i % 4;
                if (matIdx == 0) mat = Material.TINTED_GLASS;
                else if (matIdx == 1) mat = Material.BLACKSTONE;
                else if (matIdx == 2) mat = Material.DEEPSLATE;
                else mat = Material.OBSIDIAN;

                BlockDisplayHandle shard = displayBuilder.spawnBlock(loc, mat);
                // Small irregular shards
                float sx = 0.15f + (float)(Math.random() * 0.25);
                float sy = 0.15f + (float)(Math.random() * 0.25);
                float sz = 0.05f + (float)(Math.random() * 0.1);
                shard.scale(sx, sy, sz)
                     .glow(45, 0, 64)
                     .interpolation(2, 0);
                // Random initial rotation
                float randAngle = (float)(Math.random() * Math.PI * 2);
                float ax = (float)(Math.random()); float ay = (float)(Math.random()); float az = (float)(Math.random());
                float len = (float) Math.sqrt(ax*ax + ay*ay + az*az);
                shard.rotate(randAngle, ax/len, ay/len, az/len);
                shards.add(shard);
                spawnedEntities.add(shard.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 1.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_BREAK, 0.9f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < shards.size(); i++) {
                BlockDisplayHandle shard = shards.get(i);
                // Slow drift — floating in suspension
                double ox = xPos.get(i) + Math.sin(ticksAlive * driftSpeeds.get(i) + i) * 0.3;
                double oy = yPos.get(i) + Math.sin(ticksAlive * 0.03 + i * 0.5) * 0.2;
                double oz = zPos.get(i) + Math.cos(ticksAlive * driftSpeeds.get(i) + i) * 0.3;
                Location newLoc = c.clone().add(ox, oy, oz);
                shard.entity().teleport(newLoc);

                // Slow tumbling rotation
                float spinAngle = ticksAlive * spinRates.get(i);
                shard.rotate(spinAngle, 0.7f, 0.5f, 0.3f);
            }

            // Glitchy void particles around shards
            if (ticksAlive % 4 == 0) {
                int idx = ticksAlive / 4 % SHARD_COUNT;
                Location shardLoc = c.clone().add(xPos.get(idx), yPos.get(idx), zPos.get(idx));
                DisplayBuilder.dustParticles(shardLoc, 3, 0.3, 10, 0, 48, 0.8f);
            }

            // Glass shatter ambient
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, 0.4f, 1.8f);
            }

            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.3f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RealityFracture(plugin); }
    }

    // ================================================================
    // 37. DARK SINGULARITY — 10 blocks orbit a central point,
    //     spiral particles drawn inward to center
    // ================================================================
    public static class DarkSingularity extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> orbitBlocks = new ArrayList<>();
        private static final int BLOCK_COUNT = 10;
        private static final double ORBIT_RADIUS = 2.5;
        private final List<Double> orbitHeights = new ArrayList<>();
        private final List<Double> orbitPhases = new ArrayList<>();

        public DarkSingularity(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_singularity", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(50.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(340);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                double phase = (2 * Math.PI * i) / BLOCK_COUNT;
                double height = (Math.random() - 0.5) * 2.0;
                orbitPhases.add(phase);
                orbitHeights.add(height);

                double xOff = Math.cos(phase) * ORBIT_RADIUS;
                double zOff = Math.sin(phase) * ORBIT_RADIUS;
                Location loc = center.clone().add(xOff, 3.0 + height, zOff);
                Material mat = (i % 2 == 0) ? Material.COAL_BLOCK : Material.OBSIDIAN;
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                block.scale(0.4f, 0.4f, 0.4f)
                     .glow(10, 0, 48)
                     .interpolation(2, 0);
                orbitBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.2f, 0.2f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Orbit speed increases over time, radius shrinks
            float speedMult = 1.0f + ticksAlive * 0.002f;
            double radiusShrink = ORBIT_RADIUS * Math.max(0.3, 1.0 - ticksAlive * 0.001);

            for (int i = 0; i < orbitBlocks.size(); i++) {
                double phase = orbitPhases.get(i) + ticksAlive * 0.06 * speedMult;
                double height = orbitHeights.get(i) * Math.cos(ticksAlive * 0.03 + i);
                double xOff = Math.cos(phase) * radiusShrink;
                double zOff = Math.sin(phase) * radiusShrink;
                Location newLoc = c.clone().add(xOff, 3.0 + height, zOff);
                orbitBlocks.get(i).entity().teleport(newLoc);

                // Spin blocks as they orbit
                float spin = (float)(ticksAlive * 0.1 * speedMult);
                orbitBlocks.get(i).rotate(spin, 0.5f, 1.0f, 0.3f);
            }

            // Inward-spiraling particles (drawn toward center)
            if (ticksAlive % 2 == 0) {
                for (int j = 0; j < 4; j++) {
                    double pAngle = Math.random() * Math.PI * 2;
                    double pRadius = radiusShrink + 1.0 + Math.random() * 2.0;
                    double px = Math.cos(pAngle) * pRadius;
                    double pz = Math.sin(pAngle) * pRadius;
                    Location particleLoc = c.clone().add(px, 3.0 + (Math.random() - 0.5), pz);
                    DisplayBuilder.dustParticles(particleLoc, 2, 0.1, 45, 0, 64, 1.0f);
                }
                // Central dark core
                DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 3, 0.2, 10, 0, 48, 1.8f);
            }

            // Gravitational hum — intensifies
            if (ticksAlive % 25 == 0) {
                float vol = Math.min(1.0f, 0.4f + ticksAlive * 0.002f);
                DisplayBuilder.playSound(c, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, vol, 0.3f);
            }

            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkSingularity(plugin); }
    }

    // ================================================================
    // 38. CORRUPTION WORMHOLE — 14 blocks, two connected rings
    //     with a dark particle stream between them
    // ================================================================
    public static class CorruptionWormhole extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ring1Blocks = new ArrayList<>();
        private final List<BlockDisplayHandle> ring2Blocks = new ArrayList<>();
        private static final int RING_SIZE = 7;
        private static final double RING_RADIUS = 1.5;
        private static final double RING_SEPARATION = 5.0;

        public CorruptionWormhole(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_wormhole", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(46.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(320);
            config.setCooldownTicks(340);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Ring 1 — vertical ring at -Z
            for (int i = 0; i < RING_SIZE; i++) {
                double angle = (2 * Math.PI * i) / RING_SIZE;
                double xOff = Math.cos(angle) * RING_RADIUS;
                double yOff = Math.sin(angle) * RING_RADIUS;
                Location loc = center.clone().add(xOff, 2.5 + yOff, -RING_SEPARATION / 2);
                Material mat = (i % 2 == 0) ? Material.SCULK : Material.CRYING_OBSIDIAN;
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                block.scale(0.5f, 0.5f, 0.3f)
                     .glow(45, 0, 64)
                     .interpolation(3, 0);
                block.rotate((float) angle, 0, 0, 1);
                ring1Blocks.add(block);
                spawnedEntities.add(block.entity());
            }

            // Ring 2 — vertical ring at +Z
            for (int i = 0; i < RING_SIZE; i++) {
                double angle = (2 * Math.PI * i) / RING_SIZE;
                double xOff = Math.cos(angle) * RING_RADIUS;
                double yOff = Math.sin(angle) * RING_RADIUS;
                Location loc = center.clone().add(xOff, 2.5 + yOff, RING_SEPARATION / 2);
                Material mat = (i % 2 == 0) ? Material.BLACKSTONE : Material.OBSIDIAN;
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                block.scale(0.5f, 0.5f, 0.3f)
                     .glow(74, 0, 0)
                     .interpolation(3, 0);
                block.rotate((float) angle, 0, 0, 1);
                ring2Blocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Both rings rotate in opposite directions
            float rot1 = ticksAlive * 0.05f;
            float rot2 = -ticksAlive * 0.05f;

            for (int i = 0; i < RING_SIZE; i++) {
                double angle1 = (2 * Math.PI * i) / RING_SIZE + rot1;
                double xOff1 = Math.cos(angle1) * RING_RADIUS;
                double yOff1 = Math.sin(angle1) * RING_RADIUS;
                ring1Blocks.get(i).entity().teleport(c.clone().add(xOff1, 2.5 + yOff1, -RING_SEPARATION / 2));
                ring1Blocks.get(i).rotate((float) angle1, 0, 0, 1);

                double angle2 = (2 * Math.PI * i) / RING_SIZE + rot2;
                double xOff2 = Math.cos(angle2) * RING_RADIUS;
                double yOff2 = Math.sin(angle2) * RING_RADIUS;
                ring2Blocks.get(i).entity().teleport(c.clone().add(xOff2, 2.5 + yOff2, RING_SEPARATION / 2));
                ring2Blocks.get(i).rotate((float) angle2, 0, 0, 1);
            }

            // Dark particle stream between the two rings
            if (ticksAlive % 2 == 0) {
                int streamPoints = 6;
                for (int j = 0; j < streamPoints; j++) {
                    double t = (double) j / streamPoints;
                    double streamZ = -RING_SEPARATION / 2 + t * RING_SEPARATION;
                    // Spiral path between rings
                    double spiralAngle = ticksAlive * 0.2 + t * Math.PI * 2;
                    double spiralR = 0.4;
                    double sx = Math.cos(spiralAngle) * spiralR;
                    double sy = Math.sin(spiralAngle) * spiralR;
                    Location streamLoc = c.clone().add(sx, 2.5 + sy, streamZ);
                    DisplayBuilder.dustParticles(streamLoc, 2, 0.1, 10, 0, 48, 1.2f);
                }
            }

            // Ambient sounds
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 2.5, -RING_SEPARATION / 2), Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.5f);
                DisplayBuilder.playSound(c.clone().add(0, 2.5, RING_SEPARATION / 2), Sound.BLOCK_SCULK_BREAK, 0.5f, 0.5f);
            }

            if (ticksAlive % 45 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionWormhole(plugin); }
    }

    // ================================================================
    // 39. SHADOW GATE — 16 blocks form an archway, dark energy
    //     fills the interior with swirling void particles
    // ================================================================
    public static class ShadowGate extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> archBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> fillBlocks = new ArrayList<>();
        private static final int ARCH_COUNT = 12;
        private static final int FILL_COUNT = 4;

        public ShadowGate(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_gate", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(340);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Archway — semicircle of blocks on top, two pillars on sides
            // Left pillar (3 blocks)
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(-1.5, i * 1.2, 0);
                Material mat = (i % 2 == 0) ? Material.BLACKSTONE : Material.DEEPSLATE;
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                block.scale(0.6f, 1.1f, 0.5f)
                     .glow(45, 0, 64)
                     .interpolation(3, 0);
                archBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            // Right pillar (3 blocks)
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(1.5, i * 1.2, 0);
                Material mat = (i % 2 == 0) ? Material.BLACKSTONE : Material.DEEPSLATE;
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                block.scale(0.6f, 1.1f, 0.5f)
                     .glow(45, 0, 64)
                     .interpolation(3, 0);
                archBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            // Arch top — semicircle of 6 blocks
            for (int i = 0; i < 6; i++) {
                double angle = Math.PI * i / 5.0; // 0 to PI (semicircle)
                double xOff = Math.cos(angle) * 1.5;
                double yOff = Math.sin(angle) * 1.2 + 3.6;
                Location loc = center.clone().add(xOff, yOff, 0);
                Material mat = (i % 2 == 0) ? Material.SCULK : Material.CRYING_OBSIDIAN;
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                block.scale(0.55f, 0.55f, 0.5f)
                     .glow(74, 0, 0)
                     .interpolation(3, 0);
                block.rotate((float) angle, 0, 0, 1);
                archBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            // Interior fill — dark energy blocks
            for (int i = 0; i < FILL_COUNT; i++) {
                double yOff = 0.5 + (2.5 / (FILL_COUNT - 1)) * i;
                Location loc = center.clone().add(0, yOff, 0);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.COAL_BLOCK);
                block.scale(2.0f, 0.7f, 0.1f)
                     .glow(10, 0, 48)
                     .interpolation(3, 0);
                fillBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.9f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Interior blocks undulate — dark energy ripple
            for (int i = 0; i < fillBlocks.size(); i++) {
                float wave = (float) Math.sin(ticksAlive * 0.1 + i * 1.2) * 0.15f;
                double yOff = 0.5 + (2.5 / (FILL_COUNT - 1)) * i;
                Location newLoc = c.clone().add(wave, yOff, 0);
                fillBlocks.get(i).entity().teleport(newLoc);
                // Opacity pulse via scale change on Z
                float zScale = 0.1f + 0.05f * (float) Math.sin(ticksAlive * 0.15 + i);
                fillBlocks.get(i).scale(2.0f, 0.7f, zScale);
            }

            // Arch blocks subtle glow pulse
            if (ticksAlive % 5 == 0) {
                float pulse = 0.5f + 0.5f * (float) Math.sin(ticksAlive * 0.06);
                for (BlockDisplayHandle block : archBlocks) {
                    float vibration = (float) Math.sin(ticksAlive * 0.1 + Math.random()) * 0.02f;
                    block.rotate(vibration, 0, 1, 0);
                }
            }

            // Swirling void particles inside the gate
            if (ticksAlive % 3 == 0) {
                for (int j = 0; j < 3; j++) {
                    double px = (Math.random() - 0.5) * 2.0;
                    double py = Math.random() * 3.5;
                    Location particleLoc = c.clone().add(px, py, 0);
                    DisplayBuilder.dustParticles(particleLoc, 3, 0.2, 45, 0, 64, 1.2f);
                }
                DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), 2, 0.5, 10, 0, 48, 1.5f);
            }

            // Gate hum
            if (ticksAlive % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.6f, 0.3f);
            }
            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowGate(plugin); }
    }

    // ================================================================
    // 40. VOID MAW — 12 blocks form a mouth/jaw shape, opens
    //     and closes with grinding deepslate sounds
    // ================================================================
    public static class VoidMaw extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> upperJaw = new ArrayList<>();
        private final List<BlockDisplayHandle> lowerJaw = new ArrayList<>();
        private static final int TEETH_PER_JAW = 6;

        public VoidMaw(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_maw", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(48.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Upper jaw — row of pointed teeth
            for (int i = 0; i < TEETH_PER_JAW; i++) {
                double xOff = (i - TEETH_PER_JAW / 2.0 + 0.5) * 0.55;
                Location loc = center.clone().add(xOff, 3.5, 0);
                Material mat = (i % 2 == 0) ? Material.DEEPSLATE : Material.BLACKSTONE;
                BlockDisplayHandle tooth = displayBuilder.spawnBlock(loc, mat);
                // Teeth are tall and thin — pointed downward
                tooth.scale(0.4f, 0.7f, 0.3f)
                     .glow(74, 0, 0)
                     .interpolation(3, 0);
                tooth.rotate(0.15f, 0, 0, 1); // Slightly angled
                upperJaw.add(tooth);
                spawnedEntities.add(tooth.entity());
            }

            // Lower jaw — mirrored teeth
            for (int i = 0; i < TEETH_PER_JAW; i++) {
                double xOff = (i - TEETH_PER_JAW / 2.0 + 0.5) * 0.55;
                Location loc = center.clone().add(xOff, 1.5, 0);
                Material mat = (i % 2 == 0) ? Material.SCULK : Material.COAL_BLOCK;
                BlockDisplayHandle tooth = displayBuilder.spawnBlock(loc, mat);
                tooth.scale(0.4f, 0.7f, 0.3f)
                     .glow(45, 0, 64)
                     .interpolation(3, 0);
                tooth.rotate(-0.15f, 0, 0, 1);
                lowerJaw.add(tooth);
                spawnedEntities.add(tooth.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Jaw open/close cycle — sinusoidal
            // Faster biting pattern: bite every ~40 ticks
            float jawPhase = (float) Math.sin(ticksAlive * 0.16);
            float openAmount = Math.max(0, jawPhase) * 1.5f; // Only opens on positive phase
            boolean isBiting = jawPhase < -0.8f;

            for (int i = 0; i < TEETH_PER_JAW; i++) {
                double xOff = (i - TEETH_PER_JAW / 2.0 + 0.5) * 0.55;

                // Upper jaw moves up when open
                Location upperLoc = c.clone().add(xOff, 3.5 + openAmount, 0);
                upperJaw.get(i).entity().teleport(upperLoc);
                float upperTilt = 0.15f + openAmount * 0.1f;
                upperJaw.get(i).rotate(upperTilt, 0, 0, 1);

                // Lower jaw moves down when open
                Location lowerLoc = c.clone().add(xOff, 1.5 - openAmount, 0);
                lowerJaw.get(i).entity().teleport(lowerLoc);
                float lowerTilt = -0.15f - openAmount * 0.1f;
                lowerJaw.get(i).rotate(lowerTilt, 0, 0, 1);
            }

            // Bite sound when jaws snap shut
            if (isBiting && ticksAlive % 40 < 2) {
                DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.4f);
                // Crimson particle burst on bite
                DisplayBuilder.dustParticles(c.clone().add(0, 2.5, 0), 8, 0.8, 74, 0, 0, 1.5f);
            }

            // Saliva/corruption dripping particles
            if (ticksAlive % 4 == 0) {
                double rx = (Math.random() - 0.5) * 2.0;
                Location dripLoc = c.clone().add(rx, 2.5, 0);
                DisplayBuilder.dustParticles(dripLoc, 2, 0.1, 45, 0, 64, 0.8f);
            }

            // Grinding sounds while open
            if (openAmount > 0.3f && ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_BREAK, 0.6f, 0.5f);
            }

            // Heartbeat
            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidMaw(plugin); }
    }

    // ================================================================
    // 41. REALITY GLITCH — 15 blocks rapidly teleport/flicker
    //     like visual distortion, erratic position changes
    // ================================================================
    public static class RealityGlitch extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> glitchBlocks = new ArrayList<>();
        private final List<Double> baseX = new ArrayList<>();
        private final List<Double> baseY = new ArrayList<>();
        private final List<Double> baseZ = new ArrayList<>();
        private static final int BLOCK_COUNT = 15;

        public RealityGlitch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("reality_glitch", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(4.5);
            config.setDurationTicks(260);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                double ox = (Math.random() - 0.5) * 5.0;
                double oy = 1.0 + Math.random() * 4.0;
                double oz = (Math.random() - 0.5) * 5.0;
                baseX.add(ox);
                baseY.add(oy);
                baseZ.add(oz);

                Location loc = center.clone().add(ox, oy, oz);
                Material mat;
                int mi = i % 5;
                if (mi == 0) mat = Material.SCULK;
                else if (mi == 1) mat = Material.TINTED_GLASS;
                else if (mi == 2) mat = Material.BLACKSTONE;
                else if (mi == 3) mat = Material.CRYING_OBSIDIAN;
                else mat = Material.DEEPSLATE;

                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                block.scale(0.35f, 0.35f, 0.35f)
                     .glow(45, 0, 64)
                     .interpolation(1, 0);
                glitchBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_BREAK, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Every few ticks, blocks teleport to random nearby positions (glitch effect)
            boolean isGlitchFrame = (ticksAlive % 3 == 0);

            for (int i = 0; i < glitchBlocks.size(); i++) {
                BlockDisplayHandle block = glitchBlocks.get(i);

                if (isGlitchFrame) {
                    // Glitch teleport — jump to a random offset from base
                    double glitchX = baseX.get(i) + (Math.random() - 0.5) * 3.0;
                    double glitchY = baseY.get(i) + (Math.random() - 0.5) * 2.0;
                    double glitchZ = baseZ.get(i) + (Math.random() - 0.5) * 3.0;
                    Location glitchLoc = c.clone().add(glitchX, glitchY, glitchZ);
                    block.entity().teleport(glitchLoc);

                    // Random rotation snap
                    float rAngle = (float)(Math.random() * Math.PI * 2);
                    block.rotate(rAngle, (float)Math.random(), (float)Math.random(), (float)Math.random());

                    // Random scale flicker
                    float s = 0.2f + (float)(Math.random() * 0.4);
                    block.scale(s, s, s);
                } else {
                    // Return to base between glitches
                    Location baseLoc = c.clone().add(baseX.get(i), baseY.get(i), baseZ.get(i));
                    block.entity().teleport(baseLoc);
                    block.scale(0.35f, 0.35f, 0.35f);
                }
            }

            // Static/distortion particles
            if (ticksAlive % 2 == 0) {
                for (int j = 0; j < 4; j++) {
                    double px = (Math.random() - 0.5) * 5.0;
                    double py = 1.0 + Math.random() * 4.0;
                    double pz = (Math.random() - 0.5) * 5.0;
                    Location pLoc = c.clone().add(px, py, pz);
                    DisplayBuilder.dustParticles(pLoc, 2, 0.1, 74, 0, 0, 0.6f);
                }
            }

            // Digital corruption sounds
            if (ticksAlive % 8 == 0) {
                float pitch = 0.5f + (float)(Math.random() * 1.5);
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_BREAK, 0.4f, pitch);
            }

            if (ticksAlive % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.5f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RealityGlitch(plugin); }
    }

    // ================================================================
    // 42. CORRUPTION SINKHOLE — 14 blocks spiral descending
    //     into the ground, pulling particles inward
    // ================================================================
    public static class CorruptionSinkhole extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> spiralBlocks = new ArrayList<>();
        private static final int BLOCK_COUNT = 14;
        private final List<Double> spiralPhases = new ArrayList<>();
        private final List<Double> spiralRadii = new ArrayList<>();
        private final List<Double> spiralHeights = new ArrayList<>();

        public CorruptionSinkhole(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_sinkhole", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(320);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spiral descending arrangement — wide at top, narrow at bottom
            for (int i = 0; i < BLOCK_COUNT; i++) {
                double t = (double) i / BLOCK_COUNT;
                double phase = t * Math.PI * 4; // Two full rotations
                double radius = 3.0 * (1.0 - t * 0.7); // Narrows toward bottom
                double height = 4.0 * (1.0 - t); // Top to bottom

                spiralPhases.add(phase);
                spiralRadii.add(radius);
                spiralHeights.add(height);

                double xOff = Math.cos(phase) * radius;
                double zOff = Math.sin(phase) * radius;
                Location loc = center.clone().add(xOff, height, zOff);
                Material mat;
                if (i % 3 == 0) mat = Material.SCULK;
                else if (i % 3 == 1) mat = Material.DEEPSLATE;
                else mat = Material.BLACKSTONE;

                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                block.scale(0.5f, 0.4f, 0.5f)
                     .glow(45, 0, 64)
                     .interpolation(2, 0);
                spiralBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.2f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Spiral rotates and slowly descends further
            float rotSpeed = ticksAlive * 0.06f;
            float sinkProgress = Math.min(ticksAlive * 0.005f, 1.0f);

            for (int i = 0; i < spiralBlocks.size(); i++) {
                double phase = spiralPhases.get(i) + rotSpeed;
                double radius = spiralRadii.get(i) * (1.0 - sinkProgress * 0.3);
                double height = spiralHeights.get(i) * (1.0 - sinkProgress * 0.5);

                double xOff = Math.cos(phase) * radius;
                double zOff = Math.sin(phase) * radius;
                Location newLoc = c.clone().add(xOff, height, zOff);
                spiralBlocks.get(i).entity().teleport(newLoc);

                // Tilt blocks along spiral
                float tilt = (float)(phase * 0.3);
                spiralBlocks.get(i).rotate(tilt, 0, 1, 0);
            }

            // Inward-pulling particles — converge toward center ground level
            if (ticksAlive % 3 == 0) {
                for (int j = 0; j < 5; j++) {
                    double pAngle = Math.random() * Math.PI * 2;
                    double pRadius = 2.0 + Math.random() * 3.0;
                    double px = Math.cos(pAngle) * pRadius;
                    double pz = Math.sin(pAngle) * pRadius;
                    Location pLoc = c.clone().add(px, Math.random() * 3.0, pz);
                    DisplayBuilder.dustParticles(pLoc, 2, 0.15, 10, 0, 48, 1.0f);
                }
                // Center drain particles
                DisplayBuilder.dustParticles(c.clone().add(0, 0.3, 0), 4, 0.3, 45, 0, 64, 1.5f);
            }

            // Grinding descent sounds
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, 0.6f, 0.4f);
            }

            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionSinkhole(plugin); }
    }

    // ================================================================
    // 43. DARK MIRROR — 16 flat tinted glass in a vertical plane,
    //     dark reflection particles shimmer on the surface
    // ================================================================
    public static class DarkMirror extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> mirrorPanes = new ArrayList<>();
        private static final int BLOCK_COUNT = 16;
        private static final int COLS = 4;
        private static final int ROWS = 4;

        public DarkMirror(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_mirror", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 4x4 grid of tinted glass panes forming a flat mirror surface
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    double xOff = (col - COLS / 2.0 + 0.5) * 0.9;
                    double yOff = (row - ROWS / 2.0 + 0.5) * 0.9 + 2.5;
                    Location loc = center.clone().add(xOff, yOff, 0);
                    BlockDisplayHandle pane = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                    pane.scale(0.85f, 0.85f, 0.08f)
                        .glow(10, 0, 48)
                        .interpolation(3, 0);
                    mirrorPanes.add(pane);
                    spawnedEntities.add(pane.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 0.8f, 0.8f);
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.6f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Surface ripple effect — each pane shifts slightly in Z
            for (int i = 0; i < mirrorPanes.size(); i++) {
                int row = i / COLS;
                int col = i % COLS;
                double xOff = (col - COLS / 2.0 + 0.5) * 0.9;
                double yOff = (row - ROWS / 2.0 + 0.5) * 0.9 + 2.5;

                // Ripple wave across the surface
                float ripple = (float) Math.sin(ticksAlive * 0.1 + col * 0.8 + row * 0.6) * 0.15f;
                Location newLoc = c.clone().add(xOff, yOff, ripple);
                mirrorPanes.get(i).entity().teleport(newLoc);

                // Slight tilt following the ripple
                float tiltAngle = ripple * 0.3f;
                mirrorPanes.get(i).rotate(tiltAngle, 1, 0, 0);
            }

            // Dark reflection particles — shimmer across the mirror surface
            if (ticksAlive % 3 == 0) {
                // Moving highlight across the glass
                int highlightCol = (ticksAlive / 6) % COLS;
                int highlightRow = (ticksAlive / 8) % ROWS;
                double hx = (highlightCol - COLS / 2.0 + 0.5) * 0.9;
                double hy = (highlightRow - ROWS / 2.0 + 0.5) * 0.9 + 2.5;
                Location highlightLoc = c.clone().add(hx, hy, 0.1);
                DisplayBuilder.dustParticles(highlightLoc, 4, 0.3, 45, 0, 64, 1.2f);

                // Constant dark shimmer
                double rx = (Math.random() - 0.5) * 3.0;
                double ry = 1.5 + Math.random() * 2.5;
                Location shimmerLoc = c.clone().add(rx, ry, 0.15);
                DisplayBuilder.dustParticles(shimmerLoc, 2, 0.1, 74, 0, 0, 0.7f);
            }

            // Eerie mirror hum
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.4f, 0.6f);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.3f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkMirror(plugin); }
    }

    // ================================================================
    // 44. VOID BUBBLE — 12 blocks arranged in a sphere, expands
    //     outward then pops with a shockwave burst
    // ================================================================
    public static class VoidBubble extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> shellBlocks = new ArrayList<>();
        private final List<double[]> shellOffsets = new ArrayList<>();
        private static final int BLOCK_COUNT = 12;
        private boolean popped = false;

        public VoidBubble(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_bubble", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(46.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(260);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(46.0);
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Distribute blocks roughly on a sphere surface
            // Using Fibonacci sphere for even distribution
            double goldenAngle = Math.PI * (3.0 - Math.sqrt(5.0));
            for (int i = 0; i < BLOCK_COUNT; i++) {
                double y = 1.0 - (2.0 * i) / (BLOCK_COUNT - 1);
                double radiusAtY = Math.sqrt(1.0 - y * y);
                double theta = goldenAngle * i;
                double x = Math.cos(theta) * radiusAtY;
                double z = Math.sin(theta) * radiusAtY;
                shellOffsets.add(new double[]{x, y, z});

                Location loc = center.clone().add(x * 0.5, 2.5 + y * 0.5, z * 0.5);
                Material mat;
                if (i % 3 == 0) mat = Material.TINTED_GLASS;
                else if (i % 3 == 1) mat = Material.OBSIDIAN;
                else mat = Material.SCULK;

                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                block.scale(0.4f, 0.4f, 0.4f)
                     .glow(45, 0, 64)
                     .interpolation(2, 0);
                shellBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            int popTick = config.getDurationTicks() - 40; // Pop 2 seconds before end

            if (ticksAlive < popTick && !popped) {
                // Expand phase — radius grows from 0.5 to 3.0
                float progress = (float) ticksAlive / popTick;
                double radius = 0.5 + progress * 2.5;

                for (int i = 0; i < shellBlocks.size(); i++) {
                    double[] off = shellOffsets.get(i);
                    Location newLoc = c.clone().add(off[0] * radius, 2.5 + off[1] * radius, off[2] * radius);
                    shellBlocks.get(i).entity().teleport(newLoc);

                    // Pulsate as it grows
                    float pulse = 0.4f + 0.1f * (float) Math.sin(ticksAlive * 0.15 + i);
                    shellBlocks.get(i).scale(pulse, pulse, pulse);

                    // Rotate each block to face outward
                    float spin = ticksAlive * 0.03f + i;
                    shellBlocks.get(i).rotate(spin, (float) off[0], (float) off[1], (float) off[2]);
                }

                // Inner void particles
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 2.5, 0), 3, radius * 0.5, 10, 0, 48, 1.5f);
                }

                // Pressure building sound
                if (ticksAlive % 30 == 0) {
                    float pitch = 0.3f + progress * 0.8f;
                    DisplayBuilder.playSound(c, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.6f + progress * 0.4f, pitch);
                }
            } else if (!popped) {
                // POP! — shockwave burst
                popped = true;
                Location burstCenter = c.clone().add(0, 2.5, 0);
                triggerImpactDamage(burstCenter);

                // Fling shell blocks outward rapidly
                for (int i = 0; i < shellBlocks.size(); i++) {
                    double[] off = shellOffsets.get(i);
                    Location flingLoc = c.clone().add(off[0] * 8.0, 2.5 + off[1] * 8.0, off[2] * 8.0);
                    shellBlocks.get(i).entity().teleport(flingLoc);
                    shellBlocks.get(i).scale(0.2f, 0.2f, 0.2f); // Shrink on pop
                }

                // Massive shockwave ring
                DisplayBuilder.particleRing(burstCenter, 5.0, Particle.DUST, 48,
                        new Particle.DustOptions(Color.fromRGB(45, 0, 64), 2.5f));
                DisplayBuilder.particleRing(burstCenter, 3.0, Particle.DUST, 32,
                        new Particle.DustOptions(Color.fromRGB(74, 0, 0), 2.0f));
                DisplayBuilder.dustParticles(burstCenter, 20, 3.0, 10, 0, 48, 2.0f);

                DisplayBuilder.playSound(c, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.5f, 0.2f);
                DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.5f);
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.3f);
            }
            // After pop — blocks fly outward (already teleported), wait for cleanup
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidBubble(plugin); }
    }

    // ================================================================
    // 45. REALITY SCAR — 18 blocks form a long horizontal slash
    //     at chest height, pulsing with crimson energy
    // ================================================================
    public static class RealityScar extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> scarBlocks = new ArrayList<>();
        private static final int BLOCK_COUNT = 18;
        private static final double SCAR_LENGTH = 8.0;

        public RealityScar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("reality_scar", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Long horizontal slash — slightly jagged line at y+1.5 (chest height)
            for (int i = 0; i < BLOCK_COUNT; i++) {
                double t = (double) i / (BLOCK_COUNT - 1);
                double xOff = -SCAR_LENGTH / 2 + t * SCAR_LENGTH;
                double yJag = (Math.sin(i * 1.3) * 0.2); // Slight vertical jaggedness
                double zJag = (Math.cos(i * 0.9) * 0.15); // Slight depth jaggedness

                Location loc = center.clone().add(xOff, 1.5 + yJag, zJag);
                Material mat;
                if (i % 4 == 0) mat = Material.CRYING_OBSIDIAN;
                else if (i % 4 == 1) mat = Material.DEEPSLATE;
                else if (i % 4 == 2) mat = Material.SCULK;
                else mat = Material.BLACKSTONE;

                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                // Wide, thin slashes
                block.scale(0.5f, 0.25f, 0.15f)
                     .glow(74, 0, 0)
                     .interpolation(3, 0);
                // Slight rotation variation for jagged look
                float tilt = (float)(Math.sin(i * 0.7) * 0.2);
                block.rotate(tilt, 0, 0, 1);
                scarBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_BREAK, 1.2f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Crimson energy pulse traveling along the scar
            float pulsePos = (ticksAlive * 0.08f) % 1.0f; // 0..1 position along scar
            int pulseIdx = (int)(pulsePos * BLOCK_COUNT);

            for (int i = 0; i < scarBlocks.size(); i++) {
                BlockDisplayHandle block = scarBlocks.get(i);
                double t = (double) i / (BLOCK_COUNT - 1);
                double xOff = -SCAR_LENGTH / 2 + t * SCAR_LENGTH;
                double yJag = Math.sin(i * 1.3) * 0.2;
                double zJag = Math.cos(i * 0.9) * 0.15;

                // Blocks near pulse position swell
                int dist = Math.abs(i - pulseIdx);
                float swell = (dist <= 2) ? 1.0f + (2 - dist) * 0.15f : 1.0f;
                float vibration = (float) Math.sin(ticksAlive * 0.2 + i) * 0.03f;

                Location newLoc = c.clone().add(xOff, 1.5 + yJag + vibration, zJag);
                block.entity().teleport(newLoc);
                block.scale(0.5f * swell, 0.25f * swell, 0.15f);

                // Tilt follows pulse
                float tilt = (float)(Math.sin(i * 0.7 + ticksAlive * 0.05) * 0.2);
                block.rotate(tilt, 0, 0, 1);
            }

            // Crimson energy particles along the scar
            if (ticksAlive % 2 == 0) {
                double pulseX = -SCAR_LENGTH / 2 + pulsePos * SCAR_LENGTH;
                Location pulseLoc = c.clone().add(pulseX, 1.5, 0);
                DisplayBuilder.dustParticles(pulseLoc, 5, 0.4, 74, 0, 0, 1.5f);
                DisplayBuilder.dustParticles(pulseLoc, 3, 0.2, 45, 0, 64, 1.0f);
            }

            // Ambient bleeding particles along length
            if (ticksAlive % 5 == 0) {
                double rx = (Math.random() - 0.5) * SCAR_LENGTH;
                Location bleedLoc = c.clone().add(rx, 1.3, 0);
                DisplayBuilder.dustParticles(bleedLoc, 2, 0.1, 10, 0, 48, 0.7f);
            }

            // Wound sounds
            if (ticksAlive % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.5f);
            }

            if (ticksAlive % 45 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.4f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RealityScar(plugin); }
    }
}
