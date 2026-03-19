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
 * Corruption Mode — DECAY RAIN ATTACKS (Dark Entities & Shapes)
 * 15 corruption-themed BlockDisplay attacks featuring sinister creatures,
 * phantoms, and colossal dark figures that descend upon the arena.
 *
 * Color palette:
 * - Dark purple: RGB(45, 0, 64)
 * - Crimson: RGB(74, 0, 0)
 * - Void blue: RGB(10, 0, 48)
 *
 * Materials: SCULK, DEEPSLATE, BLACKSTONE, CRYING_OBSIDIAN,
 *            OBSIDIAN, COAL_BLOCK, NETHERRACK, SOUL_SOIL
 *
 * Sounds: BLOCK_SCULK_SPREAD, BLOCK_SCULK_BREAK,
 *         BLOCK_DEEPSLATE_BREAK, ENTITY_WARDEN_HEARTBEAT
 */
public final class DecayRains {

    private DecayRains() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ShadowFigure(plugin));
        registry.register(new CorruptionSkull(plugin));
        registry.register(new DarkWings(plugin));
        registry.register(new VoidWatcher(plugin));
        registry.register(new CorruptionGiantHand(plugin));
        registry.register(new ShadowBeast(plugin));
        registry.register(new DarkSerpentHead(plugin));
        registry.register(new CorruptionSpider(plugin));
        registry.register(new VoidPhantom(plugin));
        registry.register(new ShadowSentinel(plugin));
        registry.register(new DarkSwarm(plugin));
        registry.register(new CorruptionLeviathan(plugin));
        registry.register(new VoidSpecter(plugin));
        registry.register(new ShadowHound(plugin));
        registry.register(new DarkColossus(plugin));
    }

    // ================================================================
    // 76. SHADOW FIGURE — 14 blocks humanoid silhouette, glides toward
    //     player area with eerie heartbeat and sculk particles
    // ================================================================
    public static class ShadowFigure extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> bodyParts = new ArrayList<>();
        private float glideProgress = 0;
        private static final int BLOCK_COUNT = 14;
        private static final float GLIDE_DISTANCE = 12.0f;
        private static final int GLIDE_TICKS = 120;

        // Humanoid silhouette offsets: head, torso(4), arms(4), legs(4), shoulders(1)
        private static final double[][] BODY_OFFSETS = {
            {0, 4.5, 0},       // head
            {0, 3.5, 0},       // upper torso
            {0, 2.5, 0},       // mid torso
            {0, 1.5, 0},       // lower torso
            {0, 0.5, 0},       // pelvis
            {-1.0, 3.5, 0},    // left shoulder
            {1.0, 3.5, 0},     // right shoulder
            {-1.0, 2.5, 0},    // left upper arm
            {1.0, 2.5, 0},     // right upper arm
            {-1.2, 1.5, 0},    // left hand
            {1.2, 1.5, 0},     // right hand
            {-0.4, -0.5, 0},   // left foot
            {0.4, -0.5, 0},    // right foot
            {0, 5.0, 0}        // crown / top of head
        };

        public ShadowFigure(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_figure", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location spawnBase = center.clone().add(0, 0, -GLIDE_DISTANCE / 2);
            for (double[] offset : BODY_OFFSETS) {
                Location loc = spawnBase.clone().add(offset[0], offset[1], offset[2]);
                BlockDisplayHandle part = displayBuilder.spawnBlock(loc, Material.DEEPSLATE);
                part.scale(0.8f, 0.9f, 0.5f)
                    .glow(45, 0, 64)
                    .interpolation(3, 0);
                bodyParts.add(part);
                spawnedEntities.add(part.entity());
            }

            DisplayBuilder.playSound(spawnBase, Sound.ENTITY_WARDEN_HEARTBEAT, 1.2f, 0.5f);
            DisplayBuilder.playSound(spawnBase, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (ticksAlive > GLIDE_TICKS) return;

            glideProgress = (float) ticksAlive / GLIDE_TICKS;
            float zOffset = -GLIDE_DISTANCE / 2 + glideProgress * GLIDE_DISTANCE;

            // Sway the figure side-to-side as it glides
            float sway = (float) Math.sin(ticksAlive * 0.08) * 0.4f;

            for (int i = 0; i < bodyParts.size(); i++) {
                double[] offset = BODY_OFFSETS[i];
                Location newLoc = c.clone().add(offset[0] + sway, offset[1], zOffset + offset[2]);
                bodyParts.get(i).entity().teleport(newLoc);

                // Arms sway more
                if (i >= 7 && i <= 10) {
                    float armAngle = (float) Math.sin(ticksAlive * 0.1 + i) * 0.3f;
                    bodyParts.get(i).rotate(armAngle, 1, 0, 0);
                }
            }

            // Dark trailing particles
            if (ticksAlive % 3 == 0) {
                Location trailLoc = c.clone().add(sway, 2.5, zOffset - 1.0);
                DisplayBuilder.dustParticles(trailLoc, 6, 0.5, 45, 0, 64, 1.5f);
            }

            // Heartbeat ambient
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 2, zOffset), Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.6f);
            }

            // Sculk spread sounds
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 0, zOffset), Sound.BLOCK_SCULK_SPREAD, 0.6f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowFigure(plugin); }
    }

    // ================================================================
    // 77. CORRUPTION SKULL — 12 blocks skull shape, jaw opens,
    //     dark breath particles stream outward
    // ================================================================
    public static class CorruptionSkull extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> skullBlocks = new ArrayList<>();
        private BlockDisplayHandle jaw;
        private static final int BLOCK_COUNT = 12;
        private float jawAngle = 0;
        private boolean jawOpening = true;

        // Skull shape: cranium(6), eye sockets(2), jaw(3), nose bridge(1)
        private static final double[][] SKULL_OFFSETS = {
            {0, 3.0, 0},       // cranium top
            {-0.5, 2.5, 0},    // cranium left
            {0.5, 2.5, 0},     // cranium right
            {0, 2.5, 0.5},     // cranium back
            {-0.3, 2.0, -0.4}, // left temple
            {0.3, 2.0, -0.4},  // right temple
            {-0.4, 1.5, -0.5}, // left eye socket
            {0.4, 1.5, -0.5},  // right eye socket
            {0, 1.8, -0.5},    // nose bridge
            {-0.3, 0.8, -0.3}, // jaw left
            {0.3, 0.8, -0.3},  // jaw right
            {0, 0.8, -0.4}     // jaw center (animated)
        };

        public CorruptionSkull(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_skull", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location base = center.clone().add(0, 3, 0);
            for (int i = 0; i < SKULL_OFFSETS.length; i++) {
                double[] offset = SKULL_OFFSETS[i];
                Location loc = base.clone().add(offset[0], offset[1], offset[2]);
                Material mat = (i == 6 || i == 7) ? Material.CRYING_OBSIDIAN : Material.BLACKSTONE;
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                block.scale(0.7f, 0.7f, 0.7f)
                     .glow(74, 0, 0)
                     .interpolation(2, 0);
                skullBlocks.add(block);
                spawnedEntities.add(block.entity());

                if (i == 11) {
                    jaw = block;
                }
            }

            DisplayBuilder.playSound(base, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.3f);
            DisplayBuilder.playSound(base, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            Location base = c.clone().add(0, 3, 0);

            // Jaw open/close cycle
            if (jawOpening) {
                jawAngle += 0.04f;
                if (jawAngle >= 0.5f) jawOpening = false;
            } else {
                jawAngle -= 0.04f;
                if (jawAngle <= 0.0f) jawOpening = true;
            }

            // Animate jaw pieces (indices 9, 10, 11)
            for (int i = 9; i < SKULL_OFFSETS.length; i++) {
                double[] offset = SKULL_OFFSETS[i];
                Location jawLoc = base.clone().add(offset[0], offset[1] - jawAngle, offset[2]);
                skullBlocks.get(i).entity().teleport(jawLoc);
                skullBlocks.get(i).rotate(jawAngle * 0.5f, 1, 0, 0);
            }

            // Dark breath particles when jaw is open
            if (jawAngle > 0.2f && ticksAlive % 2 == 0) {
                Location breathLoc = base.clone().add(0, 0.8, -1.0);
                DisplayBuilder.dustParticles(breathLoc, 8, 0.4, 45, 0, 64, 2.0f);
                DisplayBuilder.dustParticles(breathLoc.add(0, 0, -0.5), 5, 0.3, 10, 0, 48, 1.5f);
                w.spawnParticle(Particle.SMOKE, breathLoc, 4, 0.3, 0.2, 0.5, 0.02);
            }

            // Skull float bob
            if (ticksAlive % 5 == 0) {
                float bobY = (float) Math.sin(ticksAlive * 0.06) * 0.3f;
                for (int i = 0; i < 9; i++) {
                    double[] offset = SKULL_OFFSETS[i];
                    Location loc = base.clone().add(offset[0], offset[1] + bobY, offset[2]);
                    skullBlocks.get(i).entity().teleport(loc);
                }
            }

            // Eye glow pulse
            if (ticksAlive % 10 == 0) {
                Location leftEye = base.clone().add(SKULL_OFFSETS[6][0], SKULL_OFFSETS[6][1], SKULL_OFFSETS[6][2]);
                Location rightEye = base.clone().add(SKULL_OFFSETS[7][0], SKULL_OFFSETS[7][1], SKULL_OFFSETS[7][2]);
                DisplayBuilder.dustParticles(leftEye, 3, 0.1, 74, 0, 0, 1.0f);
                DisplayBuilder.dustParticles(rightEye, 3, 0.1, 74, 0, 0, 1.0f);
            }

            // Ambient sounds
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(base, Sound.BLOCK_SCULK_BREAK, 0.6f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionSkull(plugin); }
    }

    // ================================================================
    // 78. DARK WINGS — 16 blocks bat/demon wings, flap slowly
    //     with wind sounds and trailing void particles
    // ================================================================
    public static class DarkWings extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> leftWing = new ArrayList<>();
        private final List<BlockDisplayHandle> rightWing = new ArrayList<>();
        private static final int BLOCK_COUNT = 16;
        private float flapAngle = 0;

        // Wing offsets — left wing (8 blocks), mirrored for right
        private static final double[][] LEFT_WING = {
            {-0.5, 3.0, 0},    // wing root
            {-1.5, 3.2, 0},    // inner span
            {-2.5, 3.5, 0},    // mid span
            {-3.5, 3.8, 0},    // outer span
            {-4.5, 3.5, 0},    // wing tip
            {-1.0, 2.5, 0},    // lower membrane 1
            {-2.0, 2.2, 0},    // lower membrane 2
            {-3.0, 2.0, 0}     // lower membrane 3
        };

        public DarkWings(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_wings", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(320);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location base = center.clone().add(0, 5, 0);

            // Left wing
            for (double[] offset : LEFT_WING) {
                Location loc = base.clone().add(offset[0], offset[1], offset[2]);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.SCULK);
                block.scale(0.9f, 0.4f, 0.8f)
                     .glow(10, 0, 48)
                     .interpolation(3, 0);
                leftWing.add(block);
                spawnedEntities.add(block.entity());
            }

            // Right wing (mirrored)
            for (double[] offset : LEFT_WING) {
                Location loc = base.clone().add(-offset[0], offset[1], offset[2]);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.SCULK);
                block.scale(0.9f, 0.4f, 0.8f)
                     .glow(10, 0, 48)
                     .interpolation(3, 0);
                rightWing.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(base, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            Location base = c.clone().add(0, 5, 0);

            // Slow flapping cycle
            flapAngle = (float) Math.sin(ticksAlive * 0.05) * 0.6f;

            for (int i = 0; i < LEFT_WING.length; i++) {
                double[] offset = LEFT_WING[i];
                // Progressive flap — outer blocks move more
                float flapMultiplier = (float) (i + 1) / LEFT_WING.length;
                float yFlap = flapAngle * flapMultiplier * 1.5f;

                // Left wing
                Location leftLoc = base.clone().add(offset[0], offset[1] + yFlap, offset[2]);
                leftWing.get(i).entity().teleport(leftLoc);
                leftWing.get(i).rotate(flapAngle * flapMultiplier * 0.3f, 0, 0, 1);

                // Right wing (mirrored)
                Location rightLoc = base.clone().add(-offset[0], offset[1] + yFlap, offset[2]);
                rightWing.get(i).entity().teleport(rightLoc);
                rightWing.get(i).rotate(-flapAngle * flapMultiplier * 0.3f, 0, 0, 1);
            }

            // Trailing void particles on downstroke
            if (flapAngle < -0.2f && ticksAlive % 3 == 0) {
                for (int i = 0; i < 3; i++) {
                    double x = (Math.random() - 0.5) * 8.0;
                    Location trailLoc = base.clone().add(x, 2.0, -0.5);
                    DisplayBuilder.dustParticles(trailLoc, 4, 0.3, 10, 0, 48, 1.2f);
                }
            }

            // Wind sounds on flap
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(base, Sound.BLOCK_SCULK_SPREAD, 0.7f, 0.6f);
            }

            // Heartbeat ambient
            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(base, Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkWings(plugin); }
    }

    // ================================================================
    // 79. VOID WATCHER — 10 blocks floating eye, rotates to face
    //     nearest player with pulsing crimson glow
    // ================================================================
    public static class VoidWatcher extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> eyeBlocks = new ArrayList<>();
        private BlockDisplayHandle pupil;
        private static final int BLOCK_COUNT = 10;
        private float targetYaw = 0;

        // Eye shape offsets: outer ring(7), iris(2), pupil(1)
        private static final double[][] EYE_OFFSETS = {
            {0, 3.0, 0},       // center
            {0.8, 3.0, 0},     // right
            {-0.8, 3.0, 0},    // left
            {0, 3.8, 0},       // top
            {0, 2.2, 0},       // bottom
            {0.6, 3.6, 0},     // top-right
            {-0.6, 3.6, 0},    // top-left
            {0.3, 3.0, -0.3},  // iris right
            {-0.3, 3.0, -0.3}, // iris left
            {0, 3.0, -0.4}     // pupil center
        };

        public VoidWatcher(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_watcher", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location base = center.clone().add(0, 4, 0);
            for (int i = 0; i < EYE_OFFSETS.length; i++) {
                double[] offset = EYE_OFFSETS[i];
                Location loc = base.clone().add(offset[0], offset[1], offset[2]);
                Material mat;
                if (i == 9) mat = Material.CRYING_OBSIDIAN;        // pupil
                else if (i >= 7) mat = Material.OBSIDIAN;          // iris
                else mat = Material.SCULK;                         // outer

                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                float scale = (i >= 7) ? 0.5f : 0.9f;
                block.scale(scale, scale, scale)
                     .glow(74, 0, 0)
                     .interpolation(2, 0);
                eyeBlocks.add(block);
                spawnedEntities.add(block.entity());

                if (i == 9) pupil = block;
            }

            DisplayBuilder.playSound(base, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            Location base = c.clone().add(0, 4, 0);

            // Find nearest player to rotate toward
            Player nearest = null;
            double nearestDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                double dist = p.getLocation().distanceSquared(base);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = p;
                }
            }

            if (nearest != null) {
                Location pLoc = nearest.getLocation();
                double dx = pLoc.getX() - base.getX();
                double dz = pLoc.getZ() - base.getZ();
                float desiredYaw = (float) Math.atan2(dx, dz);
                // Smooth rotation toward player
                targetYaw += (desiredYaw - targetYaw) * 0.05f;
            }

            // Rotate entire eye structure
            for (int i = 0; i < eyeBlocks.size(); i++) {
                double[] offset = EYE_OFFSETS[i];
                double rotX = offset[0] * Math.cos(targetYaw) - offset[2] * Math.sin(targetYaw);
                double rotZ = offset[0] * Math.sin(targetYaw) + offset[2] * Math.cos(targetYaw);
                Location loc = base.clone().add(rotX, offset[1], rotZ);
                eyeBlocks.get(i).entity().teleport(loc);
                eyeBlocks.get(i).rotate(targetYaw, 0, 1, 0);
            }

            // Pulsing glow on pupil
            if (ticksAlive % 8 == 0 && pupil != null) {
                float pulse = (float) (Math.sin(ticksAlive * 0.15) * 0.5 + 0.5);
                int r = (int) (74 * pulse);
                Location pupilLoc = pupil.entity().getLocation();
                DisplayBuilder.dustParticles(pupilLoc, 4, 0.2, 74, 0, 0, 1.5f * pulse + 0.5f);
            }

            // Ambient heartbeat
            if (ticksAlive % 35 == 0) {
                DisplayBuilder.playSound(base, Sound.ENTITY_WARDEN_HEARTBEAT, 0.7f, 0.5f);
            }

            // Sculk whisper
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(base, Sound.BLOCK_SCULK_SPREAD, 0.4f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidWatcher(plugin); }
    }

    // ================================================================
    // 80. CORRUPTION GIANT HAND — 15 blocks giant hand above,
    //     slams down with shockwave and sculk particles
    // ================================================================
    public static class CorruptionGiantHand extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> handBlocks = new ArrayList<>();
        private float slamProgress = 0;
        private boolean slamComplete = false;
        private static final int BLOCK_COUNT = 15;
        private static final float START_Y = 18.0f;
        private static final float SLAM_Y = 1.0f;
        private static final int HOVER_TICKS = 60;
        private static final int SLAM_TICKS = 15;

        // Hand shape: palm(5), thumb(2), fingers(8)
        private static final double[][] HAND_OFFSETS = {
            {0, 0, 0},         // palm center
            {-0.8, 0, 0},      // palm left
            {0.8, 0, 0},       // palm right
            {0, 0, -0.7},      // palm front
            {0, 0, 0.7},       // palm back
            {-1.5, 0, 0.5},    // thumb base
            {-2.0, 0.3, 0.8},  // thumb tip
            {-0.6, 0, -1.4},   // index
            {-0.2, 0, -1.6},   // index tip
            {0.2, 0, -1.4},    // middle
            {0.2, 0, -1.8},    // middle tip
            {0.6, 0, -1.3},    // ring
            {0.6, 0, -1.6},    // ring tip
            {1.0, 0, -1.1},    // pinky
            {1.0, 0, -1.4}     // pinky tip
        };

        public CorruptionGiantHand(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_giant_hand", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(48.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(350);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(48.0);
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location base = center.clone().add(0, START_Y, 0);
            for (double[] offset : HAND_OFFSETS) {
                Location loc = base.clone().add(offset[0] * 1.5, offset[1], offset[2] * 1.5);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.DEEPSLATE);
                block.scale(1.3f, 0.8f, 1.3f)
                     .glow(45, 0, 64)
                     .interpolation(2, 0);
                handBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(base, Sound.BLOCK_SCULK_SPREAD, 1.2f, 0.2f);
            DisplayBuilder.playSound(base, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (slamComplete) return;

            float currentY;

            if (ticksAlive < HOVER_TICKS) {
                // Hover phase — slight menacing sway
                currentY = START_Y;
                float sway = (float) Math.sin(ticksAlive * 0.1) * 0.3f;
                for (int i = 0; i < handBlocks.size(); i++) {
                    double[] offset = HAND_OFFSETS[i];
                    Location loc = c.clone().add(offset[0] * 1.5 + sway, START_Y + offset[1], offset[2] * 1.5);
                    handBlocks.get(i).entity().teleport(loc);
                }

                // Shadow particles below
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 1, 0), 10, 2.0, 10, 0, 48, 2.0f);
                }

                // Menacing heartbeat
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.playSound(c.clone().add(0, START_Y, 0), Sound.ENTITY_WARDEN_HEARTBEAT, 0.9f, 0.4f);
                }
            } else {
                // Slam phase
                int slamTick = ticksAlive - HOVER_TICKS;
                float slamT = Math.min(1.0f, (float) slamTick / SLAM_TICKS);
                // Ease-in for accelerating slam
                float easedT = slamT * slamT;
                currentY = START_Y - (START_Y - SLAM_Y) * easedT;

                for (int i = 0; i < handBlocks.size(); i++) {
                    double[] offset = HAND_OFFSETS[i];
                    Location loc = c.clone().add(offset[0] * 1.5, currentY + offset[1], offset[2] * 1.5);
                    handBlocks.get(i).entity().teleport(loc);

                    // Fingers curl inward during slam
                    if (i >= 7) {
                        float curl = easedT * 0.4f;
                        handBlocks.get(i).rotate(curl, 1, 0, 0);
                    }
                }

                // Impact
                if (slamT >= 1.0f) {
                    slamComplete = true;
                    triggerImpactDamage(c);

                    // Massive shockwave
                    DisplayBuilder.particleRing(c, 5.0, Particle.DUST, 36,
                            new Particle.DustOptions(Color.fromRGB(45, 0, 64), 2.5f));
                    DisplayBuilder.particleRing(c, 3.0, Particle.DUST, 24,
                            new Particle.DustOptions(Color.fromRGB(74, 0, 0), 2.0f));
                    w.spawnParticle(Particle.BLOCK, c, 50, 3.0, 0.5, 3.0, 0.3,
                            Material.DEEPSLATE.createBlockData());

                    DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.2f);
                    DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_BREAK, 1.2f, 0.3f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionGiantHand(plugin); }
    }

    // ================================================================
    // 81. SHADOW BEAST — 18 blocks four-legged creature, charges
    //     forward with ground-shaking deepslate impacts
    // ================================================================
    public static class ShadowBeast extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> beastBlocks = new ArrayList<>();
        private float chargeProgress = 0;
        private static final int BLOCK_COUNT = 18;
        private static final float CHARGE_DISTANCE = 14.0f;
        private static final int CHARGE_TICKS = 80;

        // Four-legged beast: body(6), head(3), legs(8), tail(1)
        private static final double[][] BEAST_OFFSETS = {
            {0, 2.0, 0},       // body center
            {0, 2.0, -0.8},    // body front
            {0, 2.0, 0.8},     // body rear
            {-0.6, 2.0, 0},    // body left
            {0.6, 2.0, 0},     // body right
            {0, 2.2, 0.3},     // body hump
            {0, 2.5, -1.5},    // head
            {-0.3, 2.7, -1.8}, // head left horn
            {0.3, 2.7, -1.8},  // head right horn
            {-0.8, 1.0, -0.6}, // front left upper leg
            {0.8, 1.0, -0.6},  // front right upper leg
            {-0.8, 0.0, -0.6}, // front left lower leg
            {0.8, 0.0, -0.6},  // front right lower leg
            {-0.8, 1.0, 0.6},  // rear left upper leg
            {0.8, 1.0, 0.6},   // rear right upper leg
            {-0.8, 0.0, 0.6},  // rear left lower leg
            {0.8, 0.0, 0.6},   // rear right lower leg
            {0, 2.0, 1.8}      // tail
        };

        public ShadowBeast(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_beast", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(46.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(280);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location base = center.clone().add(0, 0, -CHARGE_DISTANCE / 2);
            for (double[] offset : BEAST_OFFSETS) {
                Location loc = base.clone().add(offset[0], offset[1], offset[2]);
                Material mat = Material.COAL_BLOCK;
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                block.scale(0.9f, 0.9f, 0.9f)
                     .glow(45, 0, 64)
                     .interpolation(2, 0);
                beastBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(base, Sound.ENTITY_WARDEN_HEARTBEAT, 1.2f, 0.4f);
            DisplayBuilder.playSound(base, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (ticksAlive > CHARGE_TICKS) return;

            chargeProgress = (float) ticksAlive / CHARGE_TICKS;
            float zOffset = -CHARGE_DISTANCE / 2 + chargeProgress * CHARGE_DISTANCE;

            // Galloping leg animation
            float gallop = (float) Math.sin(ticksAlive * 0.4) * 0.5f;

            for (int i = 0; i < beastBlocks.size(); i++) {
                double[] offset = BEAST_OFFSETS[i];
                float legAnim = 0;

                // Animate legs with gallop
                if (i >= 9 && i <= 16) {
                    boolean isFrontLeg = i <= 12;
                    boolean isLeft = (i == 9 || i == 11 || i == 13 || i == 15);
                    float phase = isLeft ? gallop : -gallop;
                    if (!isFrontLeg) phase = -phase; // Opposite phase for rear
                    legAnim = phase * 0.4f;
                }

                // Body bob during gallop
                float bodyBob = (i <= 5) ? (float) Math.abs(Math.sin(ticksAlive * 0.4)) * 0.2f : 0;

                Location loc = c.clone().add(offset[0], offset[1] + bodyBob + legAnim, zOffset + offset[2]);
                beastBlocks.get(i).entity().teleport(loc);

                // Leg rotation animation
                if (i >= 9 && i <= 16) {
                    float legRotation = (float) Math.sin(ticksAlive * 0.4 + (i % 2 == 0 ? 0 : Math.PI)) * 0.3f;
                    beastBlocks.get(i).rotate(legRotation, 1, 0, 0);
                }
            }

            // Ground impact particles during gallop
            if (ticksAlive % 5 == 0) {
                Location groundLoc = c.clone().add(0, 0.1, zOffset);
                w.spawnParticle(Particle.BLOCK, groundLoc, 8, 0.5, 0.1, 0.5, 0.1,
                        Material.DEEPSLATE.createBlockData());
                DisplayBuilder.dustParticles(groundLoc, 5, 0.4, 45, 0, 64, 1.0f);
            }

            // Charging growl sounds
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 1, zOffset), Sound.BLOCK_SCULK_BREAK, 0.8f, 0.4f);
            }

            // Heartbeat during charge
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 2, zOffset), Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowBeast(plugin); }
    }

    // ================================================================
    // 82. DARK SERPENT HEAD — 14 blocks snake head, lunges forward
    //     and retracts with hissing sculk sounds
    // ================================================================
    public static class DarkSerpentHead extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> serpentBlocks = new ArrayList<>();
        private float lungeProgress = 0;
        private boolean retracting = false;
        private static final int BLOCK_COUNT = 14;
        private static final float LUNGE_DISTANCE = 8.0f;
        private static final int LUNGE_TICKS = 20;
        private static final int RETRACT_TICKS = 40;
        private static final int COIL_TICKS = 60;

        // Serpent head: head(4), jaw(3), neck(5), fangs(2)
        private static final double[][] SERPENT_OFFSETS = {
            {0, 3.0, 0},       // head top
            {-0.4, 2.8, -0.3}, // head left
            {0.4, 2.8, -0.3},  // head right
            {0, 2.8, -0.6},    // snout
            {-0.3, 2.3, -0.4}, // jaw left
            {0.3, 2.3, -0.4},  // jaw right
            {0, 2.2, -0.5},    // jaw center
            {-0.2, 2.3, -0.7}, // left fang
            {0.2, 2.3, -0.7},  // right fang
            {0, 2.6, 0.6},     // neck 1
            {0, 2.4, 1.2},     // neck 2
            {0, 2.2, 1.8},     // neck 3
            {0, 2.0, 2.4},     // neck 4
            {0, 1.8, 3.0}      // neck base
        };

        public DarkSerpentHead(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_serpent_head", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(320);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location base = center.clone().add(0, 0, 4);
            for (int i = 0; i < SERPENT_OFFSETS.length; i++) {
                double[] offset = SERPENT_OFFSETS[i];
                Location loc = base.clone().add(offset[0], offset[1], offset[2]);
                Material mat = (i == 7 || i == 8) ? Material.NETHERRACK : Material.BLACKSTONE;
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                float scale = (i >= 9) ? 0.8f : 0.7f; // neck segments slightly bigger
                block.scale(scale, scale, scale)
                     .glow(74, 0, 0)
                     .interpolation(2, 0);
                serpentBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(base, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            float zShift = 4.0f; // Starting Z offset

            if (ticksAlive < COIL_TICKS) {
                // Coiling phase — serpent sways
                float sway = (float) Math.sin(ticksAlive * 0.12) * 0.5f;
                for (int i = 0; i < serpentBlocks.size(); i++) {
                    double[] offset = SERPENT_OFFSETS[i];
                    float segmentSway = sway * ((float) i / serpentBlocks.size());
                    Location loc = c.clone().add(offset[0] + segmentSway, offset[1], zShift + offset[2]);
                    serpentBlocks.get(i).entity().teleport(loc);
                }

                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.playSound(c.clone().add(0, 3, zShift), Sound.BLOCK_SCULK_SPREAD, 0.5f, 1.2f);
                }
            } else if (ticksAlive < COIL_TICKS + LUNGE_TICKS) {
                // Lunge forward
                int lungeTick = ticksAlive - COIL_TICKS;
                float lungeT = (float) lungeTick / LUNGE_TICKS;
                float lungeZ = -lungeT * LUNGE_DISTANCE;

                for (int i = 0; i < serpentBlocks.size(); i++) {
                    double[] offset = SERPENT_OFFSETS[i];
                    // Head leads, neck follows with delay
                    float segDelay = (float) i / serpentBlocks.size();
                    float segLunge = lungeZ * Math.max(0, 1.0f - segDelay * 0.8f);
                    Location loc = c.clone().add(offset[0], offset[1], zShift + offset[2] + segLunge);
                    serpentBlocks.get(i).entity().teleport(loc);
                }

                // Hiss on lunge start
                if (lungeTick == 0) {
                    DisplayBuilder.playSound(c.clone().add(0, 3, zShift), Sound.BLOCK_SCULK_BREAK, 1.2f, 1.5f);
                }
            } else if (ticksAlive < COIL_TICKS + LUNGE_TICKS + RETRACT_TICKS) {
                // Retract
                int retractTick = ticksAlive - COIL_TICKS - LUNGE_TICKS;
                float retractT = (float) retractTick / RETRACT_TICKS;
                float retractZ = -LUNGE_DISTANCE * (1.0f - retractT);

                for (int i = 0; i < serpentBlocks.size(); i++) {
                    double[] offset = SERPENT_OFFSETS[i];
                    float segDelay = (float) i / serpentBlocks.size();
                    float segRetract = retractZ * Math.max(0, 1.0f - segDelay * 0.8f);
                    Location loc = c.clone().add(offset[0], offset[1], zShift + offset[2] + segRetract);
                    serpentBlocks.get(i).entity().teleport(loc);
                }
            }

            // Venom drip particles
            if (ticksAlive % 4 == 0) {
                Location fangLoc = serpentBlocks.get(7).entity().getLocation();
                DisplayBuilder.dustParticles(fangLoc, 3, 0.1, 74, 0, 0, 0.8f);
            }

            // Ambient heartbeat
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 3, 0), Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkSerpentHead(plugin); }
    }

    // ================================================================
    // 83. CORRUPTION SPIDER — 16 blocks spider, legs animate
    //     as it advances with chittering sculk sounds
    // ================================================================
    public static class CorruptionSpider extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> spiderBlocks = new ArrayList<>();
        private float walkProgress = 0;
        private static final int BLOCK_COUNT = 16;
        private static final float WALK_DISTANCE = 10.0f;
        private static final int WALK_TICKS = 100;

        // Spider: body(3), head(1), legs(8x = inner+outer), mandibles(2), eyes(2)
        private static final double[][] SPIDER_OFFSETS = {
            {0, 1.5, 0},       // abdomen
            {0, 1.5, -0.8},    // thorax
            {0, 1.5, -1.6},    // cephalothorax
            {0, 1.8, -2.2},    // head
            {-1.2, 0.8, -0.4}, // left leg 1
            {1.2, 0.8, -0.4},  // right leg 1
            {-1.4, 0.8, 0},    // left leg 2
            {1.4, 0.8, 0},     // right leg 2
            {-1.2, 0.8, 0.4},  // left leg 3
            {1.2, 0.8, 0.4},   // right leg 3
            {-1.0, 0.8, 0.8},  // left leg 4
            {1.0, 0.8, 0.8},   // right leg 4
            {-0.2, 1.6, -2.5}, // left mandible
            {0.2, 1.6, -2.5},  // right mandible
            {-0.15, 1.95, -2.3}, // left eye
            {0.15, 1.95, -2.3}   // right eye
        };

        public CorruptionSpider(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_spider", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location base = center.clone().add(0, 0, WALK_DISTANCE / 2);
            for (int i = 0; i < SPIDER_OFFSETS.length; i++) {
                double[] offset = SPIDER_OFFSETS[i];
                Location loc = base.clone().add(offset[0], offset[1], offset[2]);
                Material mat;
                if (i == 14 || i == 15) mat = Material.CRYING_OBSIDIAN; // eyes
                else if (i >= 4 && i <= 11) mat = Material.SCULK;      // legs
                else mat = Material.BLACKSTONE;                          // body

                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                float sx = (i >= 4 && i <= 11) ? 0.3f : 0.8f;
                float sy = (i >= 4 && i <= 11) ? 0.3f : 0.7f;
                float sz = (i >= 4 && i <= 11) ? 1.2f : 0.8f;
                if (i == 14 || i == 15) { sx = 0.25f; sy = 0.25f; sz = 0.25f; }
                block.scale(sx, sy, sz)
                     .glow(10, 0, 48)
                     .interpolation(2, 0);
                spiderBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(base, Sound.BLOCK_SCULK_SPREAD, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (ticksAlive > WALK_TICKS) return;

            walkProgress = (float) ticksAlive / WALK_TICKS;
            float zOffset = WALK_DISTANCE / 2 - walkProgress * WALK_DISTANCE;

            // Leg skitter animation
            float legCycle = ticksAlive * 0.5f;

            for (int i = 0; i < spiderBlocks.size(); i++) {
                double[] offset = SPIDER_OFFSETS[i];
                float legAnim = 0;

                // Animate legs in alternating pairs
                if (i >= 4 && i <= 11) {
                    boolean isLeft = (i % 2 == 0);
                    int legIndex = (i - 4) / 2;
                    float phase = legIndex * 0.8f + (isLeft ? 0 : (float) Math.PI);
                    legAnim = (float) Math.sin(legCycle + phase) * 0.3f;
                }

                // Body bob
                float bodyBob = (i <= 3) ? (float) Math.abs(Math.sin(legCycle * 0.5)) * 0.1f : 0;

                // Mandible twitch
                float mandibleAnim = 0;
                if (i == 12) mandibleAnim = (float) Math.sin(ticksAlive * 0.2) * 0.15f;
                if (i == 13) mandibleAnim = -(float) Math.sin(ticksAlive * 0.2) * 0.15f;

                Location loc = c.clone().add(
                    offset[0] + mandibleAnim,
                    offset[1] + bodyBob + legAnim,
                    zOffset + offset[2]
                );
                spiderBlocks.get(i).entity().teleport(loc);

                // Leg rotation
                if (i >= 4 && i <= 11) {
                    int legIndex = (i - 4) / 2;
                    float phase = legIndex * 0.8f + ((i % 2 == 0) ? 0 : (float) Math.PI);
                    float legRot = (float) Math.sin(legCycle + phase) * 0.25f;
                    spiderBlocks.get(i).rotate(legRot, 0, 0, 1);
                }
            }

            // Chittering sounds
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 1.5, zOffset), Sound.BLOCK_SCULK_BREAK, 0.5f, 1.5f);
            }

            // Web trail particles
            if (ticksAlive % 6 == 0) {
                Location trailLoc = c.clone().add(0, 0.5, zOffset + 1.0);
                DisplayBuilder.dustParticles(trailLoc, 4, 0.3, 10, 0, 48, 0.8f);
            }

            // Heartbeat
            if (ticksAlive % 35 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 1.5, zOffset), Sound.ENTITY_WARDEN_HEARTBEAT, 0.4f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionSpider(plugin); }
    }

    // ================================================================
    // 84. VOID PHANTOM — 12 blocks ghost, phases in/out with
    //     scale 0->1->0 pulse and eerie sculk whispers
    // ================================================================
    public static class VoidPhantom extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> phantomBlocks = new ArrayList<>();
        private float phaseScale = 0;
        private static final int BLOCK_COUNT = 12;
        private static final int PULSE_PERIOD = 60;

        // Ghost shape: body(5), head(2), arms(3), tattered cloak(2)
        private static final double[][] PHANTOM_OFFSETS = {
            {0, 3.5, 0},       // head top
            {0, 3.0, -0.3},    // face
            {0, 2.2, 0},       // upper body
            {0, 1.5, 0},       // mid body
            {0, 0.8, 0},       // lower body
            {-0.6, 0.3, 0},    // tattered left
            {0.6, 0.3, 0},     // tattered right
            {-0.8, 2.2, -0.2}, // left arm
            {0.8, 2.2, -0.2},  // right arm
            {-0.9, 1.6, -0.3}, // left hand
            {0.9, 1.6, -0.3},  // right hand reaching
            {0, 1.0, 0.3}      // cloak back
        };

        public VoidPhantom(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_phantom", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(360);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location base = center.clone();
            for (double[] offset : PHANTOM_OFFSETS) {
                Location loc = base.clone().add(offset[0], offset[1], offset[2]);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.SOUL_SOIL);
                block.scale(0.01f, 0.01f, 0.01f) // Start invisible (near-zero scale)
                     .glow(10, 0, 48)
                     .interpolation(5, 0);
                phantomBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(base, Sound.BLOCK_SCULK_SPREAD, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Scale pulse: 0 -> 1 -> 0 sinusoidal
            phaseScale = (float) (Math.sin(ticksAlive * Math.PI * 2.0 / PULSE_PERIOD) * 0.5 + 0.5);
            float scaleFactor = phaseScale;

            // Float upward slowly
            float floatY = ticksAlive * 0.02f;

            // Drift sideways
            float driftX = (float) Math.sin(ticksAlive * 0.04) * 1.5f;

            for (int i = 0; i < phantomBlocks.size(); i++) {
                double[] offset = PHANTOM_OFFSETS[i];

                // Tattered edges have extra wave motion
                float wave = 0;
                if (i == 5 || i == 6 || i == 11) {
                    wave = (float) Math.sin(ticksAlive * 0.15 + i) * 0.3f;
                }

                Location loc = c.clone().add(
                    offset[0] * scaleFactor + driftX + wave,
                    offset[1] + floatY,
                    offset[2] * scaleFactor
                );
                phantomBlocks.get(i).entity().teleport(loc);

                // Scale animation
                float s = Math.max(0.01f, scaleFactor * 0.8f);
                phantomBlocks.get(i).scale(s, s * 0.9f, s);

                // Arms reach forward
                if (i >= 7 && i <= 10) {
                    float reach = (float) Math.sin(ticksAlive * 0.08) * 0.2f;
                    phantomBlocks.get(i).rotate(reach, 1, 0, 0);
                }
            }

            // Ghostly particles when materializing
            if (phaseScale > 0.5f && ticksAlive % 3 == 0) {
                Location ghostLoc = c.clone().add(driftX, 2.0 + floatY, 0);
                DisplayBuilder.dustParticles(ghostLoc, 6, 0.5, 10, 0, 48, 1.5f);
            }

            // Phase-in sound
            if (ticksAlive % PULSE_PERIOD == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 2, 0), Sound.BLOCK_SCULK_SPREAD, 0.8f, 0.4f);
            }

            // Whisper
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c.clone().add(driftX, 2 + floatY, 0), Sound.BLOCK_SCULK_BREAK, 0.4f, 0.6f);
            }

            // Heartbeat when fully visible
            if (phaseScale > 0.8f && ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidPhantom(plugin); }
    }

    // ================================================================
    // 85. SHADOW SENTINEL — 15 blocks armored figure, swings
    //     weapon overhead with crushing deepslate impact
    // ================================================================
    public static class ShadowSentinel extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> sentinelBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> weaponBlocks = new ArrayList<>();
        private float swingAngle = 0;
        private boolean swinging = false;
        private static final int BLOCK_COUNT = 15;
        private static final int WIND_UP_TICKS = 50;
        private static final int SWING_TICKS = 12;

        // Armored figure: body(6), head(2), weapon(4), legs(2), arms connected to weapon(1)
        private static final double[][] BODY_OFFSETS = {
            {0, 4.5, 0},       // helmet top
            {0, 3.8, -0.2},    // visor
            {0, 3.0, 0},       // chest plate
            {-0.5, 3.0, 0},    // left pauldron
            {0.5, 3.0, 0},     // right pauldron
            {0, 2.0, 0},       // waist
            {-0.4, 1.0, 0},    // left greave
            {0.4, 1.0, 0},     // right greave
            {-0.4, 0.0, 0},    // left boot
            {0.4, 0.0, 0},     // right boot
            {0, 3.2, 0.3}      // back plate
        };

        // Weapon: hilt to blade tip (animated separately)
        private static final double[][] WEAPON_OFFSETS = {
            {0.8, 3.5, 0.3},   // hand grip
            {0.8, 4.5, 0.3},   // lower blade
            {0.8, 5.5, 0.3},   // mid blade
            {0.8, 6.5, 0.3}    // blade tip
        };

        public ShadowSentinel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_sentinel", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(46.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location base = center.clone();
            // Body
            for (double[] offset : BODY_OFFSETS) {
                Location loc = base.clone().add(offset[0], offset[1], offset[2]);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.DEEPSLATE);
                block.scale(0.9f, 0.9f, 0.7f)
                     .glow(45, 0, 64)
                     .interpolation(2, 0);
                sentinelBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            // Weapon
            for (double[] offset : WEAPON_OFFSETS) {
                Location loc = base.clone().add(offset[0], offset[1], offset[2]);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                block.scale(0.3f, 1.0f, 0.15f)
                     .glow(74, 0, 0)
                     .interpolation(2, 0);
                weaponBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(base, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.3f);
            DisplayBuilder.playSound(base, Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            int cyclePos = ticksAlive % (WIND_UP_TICKS + SWING_TICKS + 40);

            if (cyclePos < WIND_UP_TICKS) {
                // Wind-up phase — weapon raises behind
                swingAngle = -(float) cyclePos / WIND_UP_TICKS * 2.5f;
                swinging = false;
            } else if (cyclePos < WIND_UP_TICKS + SWING_TICKS) {
                // Swing phase — weapon swings forward
                int swingTick = cyclePos - WIND_UP_TICKS;
                float swingT = (float) swingTick / SWING_TICKS;
                swingAngle = -2.5f + swingT * 5.0f; // -2.5 to 2.5 radians

                if (!swinging) {
                    swinging = true;
                    DisplayBuilder.playSound(c.clone().add(0, 4, 0), Sound.BLOCK_SCULK_BREAK, 1.2f, 0.6f);
                }

                // Impact at end of swing
                if (swingTick == SWING_TICKS - 1) {
                    Location impactLoc = c.clone().add(0, 0.5, -2.0);
                    DisplayBuilder.particleRing(impactLoc, 3.0, Particle.DUST, 24,
                            new Particle.DustOptions(Color.fromRGB(45, 0, 64), 2.0f));
                    w.spawnParticle(Particle.BLOCK, impactLoc, 20, 1.5, 0.2, 1.5, 0.2,
                            Material.DEEPSLATE.createBlockData());
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.2f);
                }
            } else {
                // Recovery — weapon returns
                swingAngle *= 0.9f;
            }

            // Position weapon with swing angle
            for (int i = 0; i < weaponBlocks.size(); i++) {
                double[] offset = WEAPON_OFFSETS[i];
                double pivotY = 3.5; // pivot at hand grip
                double relY = offset[1] - pivotY;
                double relZ = offset[2];
                double rotY = relY * Math.cos(swingAngle) - relZ * Math.sin(swingAngle);
                double rotZ = relY * Math.sin(swingAngle) + relZ * Math.cos(swingAngle);

                Location loc = c.clone().add(offset[0], pivotY + rotY, rotZ);
                weaponBlocks.get(i).entity().teleport(loc);
                weaponBlocks.get(i).rotate(swingAngle, 1, 0, 0);
            }

            // Sentinel body breathing
            float breath = (float) Math.sin(ticksAlive * 0.06) * 0.05f;
            for (int i = 0; i < sentinelBlocks.size(); i++) {
                double[] offset = BODY_OFFSETS[i];
                Location loc = c.clone().add(offset[0], offset[1] + breath, offset[2]);
                sentinelBlocks.get(i).entity().teleport(loc);
            }

            // Ambient heartbeat
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 3, 0), Sound.ENTITY_WARDEN_HEARTBEAT, 0.7f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowSentinel(plugin); }
    }

    // ================================================================
    // 86. DARK SWARM — 20 small blocks chaotic swarm movement
    //     with buzzing sculk sounds and erratic particle trails
    // ================================================================
    public static class DarkSwarm extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> swarmBlocks = new ArrayList<>();
        private final List<Double> targetX = new ArrayList<>();
        private final List<Double> targetY = new ArrayList<>();
        private final List<Double> targetZ = new ArrayList<>();
        private final List<Double> currentX = new ArrayList<>();
        private final List<Double> currentY = new ArrayList<>();
        private final List<Double> currentZ = new ArrayList<>();
        private static final int BLOCK_COUNT = 20;
        private static final double SWARM_RADIUS = 4.0;

        public DarkSwarm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_swarm", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(4.5);
            config.setDurationTicks(350);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                double x = (Math.random() - 0.5) * SWARM_RADIUS * 2;
                double y = 1.0 + Math.random() * 4.0;
                double z = (Math.random() - 0.5) * SWARM_RADIUS * 2;
                currentX.add(x);
                currentY.add(y);
                currentZ.add(z);
                // Random initial targets
                targetX.add((Math.random() - 0.5) * SWARM_RADIUS * 2);
                targetY.add(1.0 + Math.random() * 4.0);
                targetZ.add((Math.random() - 0.5) * SWARM_RADIUS * 2);

                Location loc = center.clone().add(x, y, z);
                Material mat = (i % 3 == 0) ? Material.SCULK : (i % 3 == 1) ? Material.COAL_BLOCK : Material.SOUL_SOIL;
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                block.scale(0.3f, 0.3f, 0.3f)
                     .glow(45, 0, 64)
                     .interpolation(1, 0);
                swarmBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, 2, 0), Sound.BLOCK_SCULK_SPREAD, 1.2f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < BLOCK_COUNT; i++) {
                // Reassign target periodically for chaotic motion
                if (ticksAlive % (15 + i * 2) == 0) {
                    targetX.set(i, (Math.random() - 0.5) * SWARM_RADIUS * 2);
                    targetY.set(i, 1.0 + Math.random() * 4.0);
                    targetZ.set(i, (Math.random() - 0.5) * SWARM_RADIUS * 2);
                }

                // Move toward target
                double cx = currentX.get(i);
                double cy = currentY.get(i);
                double cz = currentZ.get(i);
                double tx = targetX.get(i);
                double ty = targetY.get(i);
                double tz = targetZ.get(i);

                cx += (tx - cx) * 0.08;
                cy += (ty - cy) * 0.08;
                cz += (tz - cz) * 0.08;

                // Add jitter
                cx += (Math.random() - 0.5) * 0.15;
                cy += (Math.random() - 0.5) * 0.1;
                cz += (Math.random() - 0.5) * 0.15;

                currentX.set(i, cx);
                currentY.set(i, cy);
                currentZ.set(i, cz);

                Location loc = c.clone().add(cx, cy, cz);
                swarmBlocks.get(i).entity().teleport(loc);

                // Spin chaotically
                float spin = ticksAlive * 0.3f + i * 0.5f;
                swarmBlocks.get(i).rotate(spin, (float) Math.sin(i), (float) Math.cos(i), 0.5f);

                // Trail particles for some blocks
                if (i % 4 == 0 && ticksAlive % 3 == 0) {
                    DisplayBuilder.dustParticles(loc, 2, 0.1, 45, 0, 64, 0.6f);
                }
            }

            // Buzzing sounds
            if (ticksAlive % 8 == 0) {
                int idx = ticksAlive / 8 % BLOCK_COUNT;
                Location buzzLoc = c.clone().add(currentX.get(idx), currentY.get(idx), currentZ.get(idx));
                DisplayBuilder.playSound(buzzLoc, Sound.BLOCK_SCULK_BREAK, 0.3f, 1.8f);
            }

            // Periodic sculk spread
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 2, 0), Sound.BLOCK_SCULK_SPREAD, 0.5f, 1.0f);
            }

            // Heartbeat
            if (ticksAlive % 45 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.4f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkSwarm(plugin); }
    }

    // ================================================================
    // 87. CORRUPTION LEVIATHAN — 18 blocks massive serpent
    //     undulating through the air with deep rumbles
    // ================================================================
    public static class CorruptionLeviathan extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> segmentBlocks = new ArrayList<>();
        private static final int BLOCK_COUNT = 18;
        private static final double UNDULATE_RADIUS = 5.0;

        public CorruptionLeviathan(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_leviathan", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(46.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(360);
            config.setCooldownTicks(340);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location base = center.clone().add(0, 5, 0);
            for (int i = 0; i < BLOCK_COUNT; i++) {
                double angle = (double) i / BLOCK_COUNT * Math.PI * 2;
                double x = Math.cos(angle) * UNDULATE_RADIUS;
                double z = Math.sin(angle) * UNDULATE_RADIUS;
                double y = Math.sin(angle * 2) * 2.0;
                Location loc = base.clone().add(x, y, z);

                Material mat;
                if (i == 0) mat = Material.CRYING_OBSIDIAN;             // head
                else if (i == BLOCK_COUNT - 1) mat = Material.NETHERRACK; // tail
                else mat = Material.BLACKSTONE;                           // body

                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                // Head larger, tapers to tail
                float segScale = 1.2f - (float) i / BLOCK_COUNT * 0.6f;
                block.scale(segScale, segScale, segScale)
                     .glow(74, 0, 0)
                     .interpolation(3, 0);
                segmentBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(base, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.2f);
            DisplayBuilder.playSound(base, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            Location base = c.clone().add(0, 5, 0);

            // Serpentine undulation — each segment follows a wave path
            float timeOffset = ticksAlive * 0.06f;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                double segAngle = timeOffset + (double) i / BLOCK_COUNT * Math.PI * 2;
                double x = Math.cos(segAngle) * UNDULATE_RADIUS;
                double z = Math.sin(segAngle) * UNDULATE_RADIUS;
                double y = Math.sin(segAngle * 2 + ticksAlive * 0.03) * 2.5;

                Location loc = base.clone().add(x, y, z);
                segmentBlocks.get(i).entity().teleport(loc);

                // Rotate each segment to face movement direction
                float faceAngle = (float) segAngle + (float) Math.PI / 2;
                segmentBlocks.get(i).rotate(faceAngle, 0, 1, 0);
            }

            // Trail particles from head
            if (ticksAlive % 2 == 0) {
                Location headLoc = segmentBlocks.get(0).entity().getLocation();
                DisplayBuilder.dustParticles(headLoc, 5, 0.3, 74, 0, 0, 1.5f);
            }

            // Body trail particles
            if (ticksAlive % 4 == 0) {
                int midIdx = BLOCK_COUNT / 2;
                Location midLoc = segmentBlocks.get(midIdx).entity().getLocation();
                DisplayBuilder.dustParticles(midLoc, 3, 0.2, 45, 0, 64, 1.0f);
            }

            // Deep rumbling sounds
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(base, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.2f);
            }

            // Heartbeat
            if (ticksAlive % 35 == 0) {
                Location headLoc = segmentBlocks.get(0).entity().getLocation();
                DisplayBuilder.playSound(headLoc, Sound.ENTITY_WARDEN_HEARTBEAT, 0.7f, 0.3f);
            }

            // Sculk ambience
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(base, Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionLeviathan(plugin); }
    }

    // ================================================================
    // 88. VOID SPECTER — 12 blocks cloaked figure, glides then
    //     sudden particle burst with sculk explosion
    // ================================================================
    public static class VoidSpecter extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> specterBlocks = new ArrayList<>();
        private boolean burstTriggered = false;
        private static final int BLOCK_COUNT = 12;
        private static final float GLIDE_DISTANCE = 10.0f;
        private static final int GLIDE_TICKS = 80;
        private static final int BURST_TICK = 85;

        // Cloaked figure: hood(2), cloak(6), arms(2), trailing wisps(2)
        private static final double[][] SPECTER_OFFSETS = {
            {0, 4.0, 0},       // hood top
            {0, 3.5, -0.3},    // hood face
            {-0.4, 3.0, 0},    // left shoulder cloak
            {0.4, 3.0, 0},     // right shoulder cloak
            {0, 2.5, 0},       // chest cloak
            {0, 1.8, 0},       // mid cloak
            {-0.3, 1.0, 0},    // lower left cloak
            {0.3, 1.0, 0},     // lower right cloak
            {-0.8, 2.5, -0.3}, // left arm extended
            {0.8, 2.5, -0.3},  // right arm extended
            {-0.5, 0.3, 0.2},  // left wisp trail
            {0.5, 0.3, 0.2}    // right wisp trail
        };

        public VoidSpecter(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_specter", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(280);
            config.setCooldownTicks(300);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(44.0);
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location base = center.clone().add(0, 0, GLIDE_DISTANCE / 2);
            for (double[] offset : SPECTER_OFFSETS) {
                Location loc = base.clone().add(offset[0], offset[1], offset[2]);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.SOUL_SOIL);
                block.scale(0.7f, 0.8f, 0.5f)
                     .glow(10, 0, 48)
                     .interpolation(3, 0);
                specterBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(base, Sound.BLOCK_SCULK_SPREAD, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (burstTriggered) return;

            if (ticksAlive < GLIDE_TICKS) {
                // Silent glide
                float glideT = (float) ticksAlive / GLIDE_TICKS;
                float zOffset = GLIDE_DISTANCE / 2 - glideT * GLIDE_DISTANCE;

                // Ghostly hovering
                float hover = (float) Math.sin(ticksAlive * 0.1) * 0.2f;

                for (int i = 0; i < specterBlocks.size(); i++) {
                    double[] offset = SPECTER_OFFSETS[i];
                    // Cloak billows
                    float billow = 0;
                    if (i >= 6 && i <= 7) {
                        billow = (float) Math.sin(ticksAlive * 0.15 + i) * 0.2f;
                    }
                    // Wisp trails lag behind
                    float lag = (i >= 10) ? 0.5f : 0;

                    Location loc = c.clone().add(
                        offset[0] + billow,
                        offset[1] + hover,
                        zOffset + offset[2] + lag
                    );
                    specterBlocks.get(i).entity().teleport(loc);
                }

                // Faint trail
                if (ticksAlive % 5 == 0) {
                    Location trailLoc = c.clone().add(0, 1.5, zOffset + 1.5);
                    DisplayBuilder.dustParticles(trailLoc, 3, 0.3, 10, 0, 48, 0.8f);
                }

                // Whisper sounds
                if (ticksAlive % 25 == 0) {
                    DisplayBuilder.playSound(c.clone().add(0, 2, zOffset), Sound.BLOCK_SCULK_SPREAD, 0.3f, 0.5f);
                }
            } else if (ticksAlive >= BURST_TICK && !burstTriggered) {
                // Sudden particle burst explosion
                burstTriggered = true;
                Location burstLoc = c.clone().add(0, 2, 0);

                triggerImpactDamage(burstLoc);

                // Massive void burst
                DisplayBuilder.particleRing(burstLoc, 5.0, Particle.DUST, 48,
                        new Particle.DustOptions(Color.fromRGB(10, 0, 48), 3.0f));
                DisplayBuilder.particleRing(burstLoc, 3.0, Particle.DUST, 32,
                        new Particle.DustOptions(Color.fromRGB(45, 0, 64), 2.5f));
                DisplayBuilder.dustParticles(burstLoc, 30, 2.5, 74, 0, 0, 2.0f);
                w.spawnParticle(Particle.SMOKE, burstLoc, 40, 2.0, 2.0, 2.0, 0.1);

                DisplayBuilder.playSound(burstLoc, Sound.BLOCK_SCULK_BREAK, 1.5f, 0.2f);
                DisplayBuilder.playSound(burstLoc, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.3f);
                DisplayBuilder.playSound(burstLoc, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.2f);

                // Fling all specter blocks outward
                for (int i = 0; i < specterBlocks.size(); i++) {
                    double[] offset = SPECTER_OFFSETS[i];
                    double angle = Math.atan2(offset[0], offset[2]);
                    double dist = 3.0 + Math.random() * 2.0;
                    Location flingLoc = burstLoc.clone().add(
                        Math.sin(angle) * dist,
                        Math.random() * 3.0,
                        Math.cos(angle) * dist
                    );
                    specterBlocks.get(i).entity().teleport(flingLoc);
                    specterBlocks.get(i).scale(0.3f, 0.3f, 0.3f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidSpecter(plugin); }
    }

    // ================================================================
    // 89. SHADOW HOUND — 14 blocks wolf, runs in circle with
    //     growl sounds and corruption trail
    // ================================================================
    public static class ShadowHound extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> houndBlocks = new ArrayList<>();
        private float circleAngle = 0;
        private static final int BLOCK_COUNT = 14;
        private static final double CIRCLE_RADIUS = 5.0;
        private static final float CIRCLE_SPEED = 0.08f;

        // Wolf shape: head(3), body(4), legs(4), tail(2), ears(1)
        private static final double[][] HOUND_OFFSETS = {
            {0, 1.8, -1.2},    // head
            {-0.25, 2.1, -1.0},// left ear
            {0.25, 2.1, -1.0}, // right ear (merged as 1 block)
            {0, 1.5, -0.4},    // upper body
            {0, 1.5, 0},       // mid body
            {0, 1.5, 0.5},     // lower body
            {0, 1.3, 0.9},     // haunch
            {-0.4, 0.5, -0.5}, // front left leg
            {0.4, 0.5, -0.5},  // front right leg
            {-0.4, 0.5, 0.6},  // rear left leg
            {0.4, 0.5, 0.6},   // rear right leg
            {0, 1.6, 1.3},     // tail base
            {0, 1.9, 1.6},     // tail tip
            {0, 1.7, -1.5}     // snout
        };

        public ShadowHound(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_hound", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(320);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location base = center.clone().add(CIRCLE_RADIUS, 0, 0);
            for (int i = 0; i < HOUND_OFFSETS.length; i++) {
                double[] offset = HOUND_OFFSETS[i];
                Location loc = base.clone().add(offset[0], offset[1], offset[2]);
                Material mat = (i == 0 || i == 13) ? Material.NETHERRACK : Material.COAL_BLOCK;
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                float sx = (i >= 7 && i <= 10) ? 0.35f : 0.7f;
                float sy = (i >= 7 && i <= 10) ? 0.8f : 0.7f;
                float sz = (i >= 7 && i <= 10) ? 0.35f : 0.7f;
                block.scale(sx, sy, sz)
                     .glow(74, 0, 0)
                     .interpolation(2, 0);
                houndBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(base, Sound.BLOCK_SCULK_BREAK, 1.0f, 0.5f);
            DisplayBuilder.playSound(base, Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            circleAngle += CIRCLE_SPEED;

            double circleX = Math.cos(circleAngle) * CIRCLE_RADIUS;
            double circleZ = Math.sin(circleAngle) * CIRCLE_RADIUS;

            // Direction the hound is facing (tangent to circle)
            float faceAngle = circleAngle + (float) Math.PI / 2;

            // Galloping leg animation
            float gallop = (float) Math.sin(ticksAlive * 0.5) * 0.4f;

            for (int i = 0; i < houndBlocks.size(); i++) {
                double[] offset = HOUND_OFFSETS[i];

                // Rotate offset by facing direction
                double rotX = offset[2] * Math.cos(faceAngle) - offset[0] * Math.sin(faceAngle);
                double rotZ = offset[2] * Math.sin(faceAngle) + offset[0] * Math.cos(faceAngle);

                // Leg gallop animation
                float legAnim = 0;
                if (i >= 7 && i <= 10) {
                    boolean isFront = (i <= 8);
                    boolean isLeft = (i == 7 || i == 9);
                    float phase = isLeft ? gallop : -gallop;
                    if (!isFront) phase = -phase;
                    legAnim = phase * 0.3f;
                }

                // Tail wag
                float tailWag = 0;
                if (i == 11 || i == 12) {
                    tailWag = (float) Math.sin(ticksAlive * 0.3) * 0.3f;
                }

                Location loc = c.clone().add(circleX + rotX + tailWag, offset[1] + legAnim, circleZ + rotZ);
                houndBlocks.get(i).entity().teleport(loc);
                houndBlocks.get(i).rotate(faceAngle, 0, 1, 0);
            }

            // Corruption trail particles
            if (ticksAlive % 3 == 0) {
                Location trailLoc = c.clone().add(circleX, 0.3, circleZ);
                DisplayBuilder.dustParticles(trailLoc, 4, 0.3, 45, 0, 64, 1.0f);
            }

            // Growl sounds
            if (ticksAlive % 20 == 0) {
                Location houndLoc = c.clone().add(circleX, 1.5, circleZ);
                DisplayBuilder.playSound(houndLoc, Sound.BLOCK_SCULK_BREAK, 0.7f, 0.4f);
            }

            // Panting / heartbeat
            if (ticksAlive % 15 == 0) {
                Location houndLoc = c.clone().add(circleX, 1.5, circleZ);
                DisplayBuilder.playSound(houndLoc, Sound.ENTITY_WARDEN_HEARTBEAT, 0.4f, 0.8f);
            }

            // Ground impact from running
            if (ticksAlive % 8 == 0) {
                Location groundLoc = c.clone().add(circleX, 0.1, circleZ);
                w.spawnParticle(Particle.BLOCK, groundLoc, 4, 0.3, 0.05, 0.3, 0.05,
                        Material.DEEPSLATE.createBlockData());
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowHound(plugin); }
    }

    // ================================================================
    // 90. DARK COLOSSUS — 16 blocks massive foot stamps down
    //     with ground crack particles and seismic deepslate impact
    // ================================================================
    public static class DarkColossus extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> footBlocks = new ArrayList<>();
        private float stompProgress = 0;
        private boolean stompComplete = false;
        private static final int BLOCK_COUNT = 16;
        private static final float START_Y = 20.0f;
        private static final int HOVER_TICKS = 40;
        private static final int STOMP_TICKS = 10;

        // Giant foot: sole(6), toes(5), ankle(3), shin lower(2)
        private static final double[][] FOOT_OFFSETS = {
            {0, 0, 0},         // sole center
            {-1.0, 0, 0},      // sole left
            {1.0, 0, 0},       // sole right
            {0, 0, -1.0},      // sole front
            {0, 0, 1.0},       // sole back
            {0, 0.3, 0},       // sole top
            {-0.6, 0, -1.5},   // left toe
            {-0.2, 0, -1.7},   // toe 2
            {0.2, 0, -1.7},    // toe 3
            {0.6, 0, -1.5},    // right toe
            {0, 0, -1.9},      // big toe
            {0, 1.5, 0.5},     // ankle
            {-0.3, 1.5, 0.3},  // ankle left
            {0.3, 1.5, 0.3},   // ankle right
            {0, 3.0, 0.5},     // lower shin
            {0, 4.0, 0.5}      // shin (fades into darkness above)
        };

        public DarkColossus(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_colossus", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(50.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(360);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(50.0);
            config.setImpactRadius(6.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location base = center.clone().add(0, START_Y, 0);
            for (int i = 0; i < FOOT_OFFSETS.length; i++) {
                double[] offset = FOOT_OFFSETS[i];
                Location loc = base.clone().add(offset[0] * 1.8, offset[1], offset[2] * 1.8);
                Material mat;
                if (i >= 14) mat = Material.OBSIDIAN;       // shin fades to dark
                else if (i >= 11) mat = Material.BLACKSTONE; // ankle
                else mat = Material.DEEPSLATE;               // sole and toes

                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                block.scale(1.5f, 1.0f, 1.5f)
                     .glow(45, 0, 64)
                     .interpolation(2, 0);
                footBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(base, Sound.ENTITY_WARDEN_HEARTBEAT, 1.2f, 0.2f);
            DisplayBuilder.playSound(base, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (stompComplete) return;

            if (ticksAlive < HOVER_TICKS) {
                // Hover menacingly — slight sway
                float sway = (float) Math.sin(ticksAlive * 0.08) * 0.3f;
                for (int i = 0; i < footBlocks.size(); i++) {
                    double[] offset = FOOT_OFFSETS[i];
                    Location loc = c.clone().add(offset[0] * 1.8 + sway, START_Y + offset[1], offset[2] * 1.8);
                    footBlocks.get(i).entity().teleport(loc);
                }

                // Shadow below grows darker
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), 12, 2.5, 10, 0, 48, 2.5f);
                }

                // Menacing heartbeat accelerates
                int heartbeatInterval = Math.max(10, 30 - ticksAlive / 2);
                if (ticksAlive % heartbeatInterval == 0) {
                    DisplayBuilder.playSound(c.clone().add(0, START_Y, 0), Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.3f);
                }
            } else {
                // Stomp down
                int stompTick = ticksAlive - HOVER_TICKS;
                float stompT = Math.min(1.0f, (float) stompTick / STOMP_TICKS);
                // Aggressive ease-in
                float easedT = stompT * stompT * stompT;
                float currentY = START_Y * (1.0f - easedT);

                for (int i = 0; i < footBlocks.size(); i++) {
                    double[] offset = FOOT_OFFSETS[i];
                    Location loc = c.clone().add(offset[0] * 1.8, currentY + offset[1], offset[2] * 1.8);
                    footBlocks.get(i).entity().teleport(loc);
                }

                // Impact
                if (stompT >= 1.0f) {
                    stompComplete = true;
                    triggerImpactDamage(c);

                    // Massive ground crack effect — multiple concentric rings
                    for (double r = 2.0; r <= 6.0; r += 1.5) {
                        DisplayBuilder.particleRing(c, r, Particle.DUST, (int) (r * 8),
                                new Particle.DustOptions(Color.fromRGB(45, 0, 64), 2.5f));
                    }

                    // Ground shatter particles
                    w.spawnParticle(Particle.BLOCK, c, 80, 4.0, 0.3, 4.0, 0.4,
                            Material.DEEPSLATE.createBlockData());
                    w.spawnParticle(Particle.BLOCK, c, 40, 3.0, 0.2, 3.0, 0.3,
                            Material.BLACKSTONE.createBlockData());

                    // Radial dust lines (crack pattern)
                    for (int i = 0; i < 8; i++) {
                        double angle = (Math.PI * 2 * i) / 8;
                        Location lineEnd = c.clone().add(Math.cos(angle) * 6, 0.1, Math.sin(angle) * 6);
                        DisplayBuilder.particleLine(c.clone().add(0, 0.1, 0), lineEnd,
                                Particle.DUST, 3, new Particle.DustOptions(Color.fromRGB(74, 0, 0), 1.5f));
                    }

                    // Seismic sounds
                    DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, 2.0f, 0.1f);
                    DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_BREAK, 1.5f, 0.2f);
                    DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 1.5f, 0.2f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkColossus(plugin); }
    }
}
