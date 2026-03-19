package com.blockforge.chaoscraft.modes.devilsdream.attacks;

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
 * Devil's Dream — HELLFIRE FORMATION ATTACKS
 * 13 fire and brimstone BlockDisplay attacks featuring infernal pillars,
 * magma serpents, volcanic eruptions, and hellfire rings.
 *
 * Color palette (infernal):
 * - Magma orange: magma_block
 * - Netherrack red: netherrack, red_nether_bricks
 * - Blackstone dark: blackstone, polished_blackstone
 * - Soul blue: soul_sand, soul_soil, soul_fire
 * - Lava gold: orange_concrete, yellow_concrete
 */
public final class HellfireFormations {

    private HellfireFormations() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new InfernoPillar(plugin));
        registry.register(new BrimstoneGeyser(plugin));
        registry.register(new HellfireRing(plugin));
        registry.register(new MagmaSerpent(plugin));
        registry.register(new LavaPillarRow(plugin));
        registry.register(new InfernalCross(plugin));
        registry.register(new FireStorm(plugin));
        registry.register(new BrimstoneWall(plugin));
        registry.register(new HellfireStarfish(plugin));
        registry.register(new MoltenRain(plugin));
        registry.register(new InfernalCage(plugin));
        registry.register(new VolcanicSpine(plugin));
        registry.register(new BrimstoneMeteor(plugin));
    }

    // ================================================================
    // 1. INFERNO PILLAR — 12-block tall pillar of magma/netherrack
    //    that erupts from the ground, rising block by block with fire
    //    particles, then pulses with heat waves.
    // ================================================================
    public static class InfernoPillar extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> pillarBlocks = new ArrayList<>();
        private static final int PILLAR_HEIGHT = 12;
        private int blocksRevealed = 0;

        public InfernoPillar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("inferno_pillar", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(55.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Pre-spawn all blocks at ground level, hidden (tiny scale)
            for (int i = 0; i < PILLAR_HEIGHT; i++) {
                Material mat = (i % 3 == 0) ? Material.MAGMA_BLOCK
                        : (i % 3 == 1) ? Material.NETHERRACK
                        : Material.RED_NETHER_BRICKS;
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(0, 0, 0), mat);
                block.scale(0.01f, 0.01f, 0.01f).glow(255, 120, 30).interpolation(4, 0);
                pillarBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Reveal blocks one by one (every 4 ticks)
            if (ticksAlive % 4 == 0 && blocksRevealed < PILLAR_HEIGHT) {
                BlockDisplayHandle block = pillarBlocks.get(blocksRevealed);
                block.entity().teleport(c.clone().add(0, blocksRevealed, 0));
                float width = 2.5f - (blocksRevealed * 0.1f); // Tapers slightly
                block.animateTo(
                        new Vector3f(-width / 2, -0.5f, -width / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(width, 1.0f, width), 4);

                // Eruption particles at each level
                Location blockLoc = c.clone().add(0, blocksRevealed, 0);
                c.getWorld().spawnParticle(Particle.FLAME, blockLoc, 15, 0.8, 0.3, 0.8, 0.05);
                c.getWorld().spawnParticle(Particle.LAVA, blockLoc, 5, 0.5, 0.2, 0.5, 0);
                DisplayBuilder.playSound(blockLoc, Sound.BLOCK_FIRE_AMBIENT, 0.8f, 0.5f + blocksRevealed * 0.05f);

                blocksRevealed++;
            }

            // Heat pulse effect after fully risen
            if (blocksRevealed >= PILLAR_HEIGHT && ticksAlive % 10 == 0) {
                float pulse = 1.0f + (float) Math.sin(ticksAlive * 0.15) * 0.3f;
                for (int i = 0; i < pillarBlocks.size(); i++) {
                    float baseWidth = 2.5f - (i * 0.1f);
                    pillarBlocks.get(i).scale(baseWidth * pulse, 1.0f, baseWidth * pulse);
                }
                // Heat wave particles
                DisplayBuilder.particleRing(c.clone().add(0, 1, 0), 4.0, Particle.FLAME, 16, null);
            }

            // Ambient fire
            if (ticksAlive % 8 == 0) {
                Location fireLoc = c.clone().add(
                        Math.random() * 2 - 1, Math.random() * blocksRevealed, Math.random() * 2 - 1);
                c.getWorld().spawnParticle(Particle.FLAME, fireLoc, 3, 0.2, 0.2, 0.2, 0.02);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_POP, 0.6f, 0.6f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new InfernoPillar(plugin); }
    }

    // ================================================================
    // 2. BRIMSTONE GEYSER — 10 blocks shoot upward from a central
    //    point like a geyser, spreading outward as they rise.
    //    Sulfur yellow + magma blocks. Impact damage on landing.
    // ================================================================
    public static class BrimstoneGeyser extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> fragments = new ArrayList<>();
        private final List<Double> angleOffsets = new ArrayList<>();
        private final List<Float> velocities = new ArrayList<>();
        private final List<Float> yPositions = new ArrayList<>();
        private final List<Boolean> landed = new ArrayList<>();

        public BrimstoneGeyser(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_geyser", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(45.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(70.0);
            config.setImpactRadius(6.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 10; i++) {
                double angle = (2 * Math.PI * i) / 10;
                angleOffsets.add(angle);
                velocities.add(0.5f + (float)(Math.random() * 0.4));
                yPositions.add(0.0f);
                landed.add(false);

                Material mat = (i % 2 == 0) ? Material.MAGMA_BLOCK : Material.YELLOW_CONCRETE;
                BlockDisplayHandle frag = displayBuilder.spawnBlock(center, mat);
                frag.scale(1.2f, 1.2f, 1.2f).glow(255, 200, 50).interpolation(2, 0);
                fragments.add(frag);
                spawnedEntities.add(frag.entity());
            }

            // Initial eruption effect
            w.spawnParticle(Particle.EXPLOSION, center, 2, 0.5, 0.5, 0.5, 0);
            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < fragments.size(); i++) {
                if (landed.get(i)) continue;

                float vel = velocities.get(i);
                float y = yPositions.get(i);

                // Phase 1: shoot up (first 30 ticks)
                if (ticksAlive < 30) {
                    y += vel;
                } else {
                    // Phase 2: arc outward and fall
                    vel -= 0.04f; // Gravity
                    y += vel;
                    velocities.set(i, vel);
                }
                yPositions.set(i, y);

                double angle = angleOffsets.get(i);
                double spreadRadius = ticksAlive * 0.08;
                double x = Math.cos(angle) * spreadRadius;
                double z = Math.sin(angle) * spreadRadius;

                fragments.get(i).entity().teleport(c.clone().add(x, Math.max(0, y), z));
                fragments.get(i).rotate(ticksAlive * 0.1f, 1, 0, 1);

                // Trail particles
                if (ticksAlive % 3 == 0) {
                    Location trail = c.clone().add(x, Math.max(0.5, y), z);
                    c.getWorld().spawnParticle(Particle.FLAME, trail, 3, 0.2, 0.2, 0.2, 0.02);
                    c.getWorld().spawnParticle(Particle.SMOKE, trail, 2, 0.1, 0.1, 0.1, 0.01);
                }

                // Landing
                if (y <= 0 && ticksAlive > 30) {
                    landed.set(i, true);
                    Location impactLoc = c.clone().add(x, 0, z);
                    triggerImpactDamage(impactLoc);
                    c.getWorld().spawnParticle(Particle.LAVA, impactLoc, 10, 0.5, 0.2, 0.5, 0);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_BASALT_BREAK, 0.8f, 0.5f);
                }
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_POP, 0.5f, 0.7f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new BrimstoneGeyser(plugin); }
    }

    // ================================================================
    // 3. HELLFIRE RING — 12 magma blocks in a ring that spins and
    //    slowly constricts, leaving fire trails behind. Ring rises
    //    and falls in a wave pattern.
    // ================================================================
    public static class HellfireRing extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> ringBlocks = new ArrayList<>();
        private double ringRadius = 6.0;
        private static final int RING_COUNT = 12;

        public HellfireRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellfire_ring", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(55.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < RING_COUNT; i++) {
                double angle = (2 * Math.PI * i) / RING_COUNT;
                double x = Math.cos(angle) * ringRadius;
                double z = Math.sin(angle) * ringRadius;

                Material mat = (i % 3 == 0) ? Material.MAGMA_BLOCK
                        : (i % 3 == 1) ? Material.ORANGE_CONCRETE
                        : Material.RED_NETHER_BRICKS;
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(x, 1, z), mat);
                block.scale(1.5f, 1.5f, 1.5f).glow(255, 140, 20).interpolation(2, 0);
                ringBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Constrict
            if (ringRadius > 2.0) {
                ringRadius -= 0.015;
            }

            double spinOffset = ticksAlive * 0.04;

            for (int i = 0; i < RING_COUNT; i++) {
                double baseAngle = (2 * Math.PI * i) / RING_COUNT;
                double angle = baseAngle + spinOffset;
                double x = Math.cos(angle) * ringRadius;
                double z = Math.sin(angle) * ringRadius;
                // Wave height
                double y = 1.0 + Math.sin(ticksAlive * 0.08 + i * 0.5) * 1.5;

                ringBlocks.get(i).entity().teleport(c.clone().add(x, y, z));
                ringBlocks.get(i).rotate(ticksAlive * 0.05f, 0, 1, 0);

                // Fire trail
                if (ticksAlive % 3 == 0) {
                    c.getWorld().spawnParticle(Particle.FLAME, c.clone().add(x, y, z),
                            3, 0.2, 0.2, 0.2, 0.02);
                }
            }

            // Periodic fire ring on ground
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.5, 0), ringRadius,
                        Particle.FLAME, 20, null);
            }

            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 0.8f, 0.5f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new HellfireRing(plugin); }
    }

    // ================================================================
    // 4. MAGMA SERPENT — 14 magma blocks arranged as a serpentine body
    //    that slithers in a sine wave pattern around the player.
    //    Head block is larger, body tapers. Leaves lava drip trail.
    // ================================================================
    public static class MagmaSerpent extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> bodySegments = new ArrayList<>();
        private static final int SEGMENT_COUNT = 14;

        public MagmaSerpent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_serpent", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(60.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < SEGMENT_COUNT; i++) {
                Material mat = (i == 0) ? Material.MAGMA_BLOCK // Head
                        : (i % 2 == 0) ? Material.NETHERRACK
                        : Material.MAGMA_BLOCK;
                BlockDisplayHandle seg = displayBuilder.spawnBlock(center, mat);
                float scale = (i == 0) ? 2.0f : Math.max(0.6f, 1.6f - i * 0.07f);
                seg.scale(scale, scale * 0.7f, scale).glow(255, 100, 20).interpolation(2, 0);
                bodySegments.add(seg);
                spawnedEntities.add(seg.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_MAGMA_CUBE_SQUISH, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double baseAngle = ticksAlive * 0.04;
            double circleRadius = 5.0;

            for (int i = 0; i < SEGMENT_COUNT; i++) {
                // Each segment follows the one ahead with delay
                double segAngle = baseAngle - i * 0.3;
                double x = Math.cos(segAngle) * circleRadius;
                double z = Math.sin(segAngle) * circleRadius;
                // Undulating height
                double y = 1.0 + Math.sin(segAngle * 2) * 1.5;

                bodySegments.get(i).entity().teleport(c.clone().add(x, y, z));
                bodySegments.get(i).rotate((float) segAngle, 0, 1, 0);

                // Lava drip from body
                if (ticksAlive % 6 == 0 && i % 3 == 0) {
                    c.getWorld().spawnParticle(Particle.DRIPPING_LAVA,
                            c.clone().add(x, y - 0.5, z), 2, 0.2, 0.1, 0.2, 0);
                }
            }

            // Head fire particles
            if (ticksAlive % 3 == 0) {
                double headAngle = baseAngle;
                Location headLoc = c.clone().add(
                        Math.cos(headAngle) * circleRadius, 1.5, Math.sin(headAngle) * circleRadius);
                c.getWorld().spawnParticle(Particle.FLAME, headLoc, 5, 0.3, 0.3, 0.3, 0.03);
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_MAGMA_CUBE_SQUISH, 0.5f, 0.6f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new MagmaSerpent(plugin); }
    }

    // ================================================================
    // 5. LAVA PILLAR ROW — 5 pillars of 3 blocks each (15 total)
    //    that erupt sequentially in a line toward the player.
    //    Each pillar rises with a delay. Fire between pillars.
    // ================================================================
    public static class LavaPillarRow extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> pillars = new ArrayList<>();
        private final List<Float> pillarHeights = new ArrayList<>();
        private static final int PILLAR_COUNT = 5;

        public LavaPillarRow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lava_pillar_row", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(50.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int p = 0; p < PILLAR_COUNT; p++) {
                List<BlockDisplayHandle> pillar = new ArrayList<>();
                double x = (p - 2) * 3.0; // Spread 3 blocks apart

                for (int y = 0; y < 3; y++) {
                    Material mat = (y == 0) ? Material.MAGMA_BLOCK
                            : (y == 1) ? Material.NETHERRACK
                            : Material.RED_NETHER_BRICKS;
                    BlockDisplayHandle block = displayBuilder.spawnBlock(
                            center.clone().add(x, -3 + y, 0), mat);
                    block.scale(2.0f, 1.5f, 2.0f).glow(255, 100, 30).interpolation(4, 0);
                    pillar.add(block);
                    spawnedEntities.add(block.entity());
                }
                pillars.add(pillar);
                pillarHeights.add(-3.0f);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BASALT_BREAK, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int p = 0; p < PILLAR_COUNT; p++) {
                int activationTick = p * 20; // Sequential with 1-second delay
                if (ticksAlive < activationTick) continue;

                float height = pillarHeights.get(p);
                if (height < 0) {
                    height += 0.3f;
                    if (height > 0) height = 0;
                    pillarHeights.set(p, height);

                    // Rumble when rising
                    if ((ticksAlive - activationTick) == 1) {
                        DisplayBuilder.playSound(c.clone().add((p - 2) * 3.0, 0, 0),
                                Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 0.5f);
                    }
                }

                double x = (p - 2) * 3.0;
                for (int y = 0; y < 3; y++) {
                    pillars.get(p).get(y).entity().teleport(
                            c.clone().add(x, height + y * 1.5, 0));
                }

                // Fire on top of risen pillars
                if (height >= 0 && ticksAlive % 5 == 0) {
                    Location top = c.clone().add(x, 4, 0);
                    c.getWorld().spawnParticle(Particle.FLAME, top, 5, 0.5, 0.3, 0.5, 0.03);
                }
            }

            // Fire line between pillars
            if (ticksAlive % 8 == 0 && ticksAlive > PILLAR_COUNT * 20) {
                for (int p = 0; p < PILLAR_COUNT - 1; p++) {
                    double x1 = (p - 2) * 3.0;
                    double x2 = (p - 1) * 3.0;
                    Location start = c.clone().add(x1, 1, 0);
                    Location end = c.clone().add(x2, 1, 0);
                    DisplayBuilder.particleLine(start, end, Particle.FLAME, 3, null);
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new LavaPillarRow(plugin); }
    }

    // ================================================================
    // 6. INFERNAL CROSS — 4 arms of 3 blocks each + center (13 total)
    //    forming a burning cross on the ground. Arms rotate slowly.
    //    Fire particles trace the cross shape.
    // ================================================================
    public static class InfernalCross extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> crossBlocks = new ArrayList<>();
        private BlockDisplayHandle centerBlock;

        public InfernalCross(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_cross", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(55.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Center block
            centerBlock = displayBuilder.spawnBlock(center.clone().add(0, 0.5, 0), Material.MAGMA_BLOCK);
            centerBlock.scale(2.0f, 0.5f, 2.0f).glow(255, 80, 20).interpolation(3, 0);
            spawnedEntities.add(centerBlock.entity());

            // 4 arms, 3 blocks each
            for (int arm = 0; arm < 4; arm++) {
                double angle = (Math.PI / 2) * arm;
                for (int seg = 1; seg <= 3; seg++) {
                    double x = Math.cos(angle) * seg * 2.0;
                    double z = Math.sin(angle) * seg * 2.0;
                    Material mat = (seg == 3) ? Material.NETHERRACK
                            : (seg == 2) ? Material.RED_NETHER_BRICKS
                            : Material.MAGMA_BLOCK;
                    BlockDisplayHandle block = displayBuilder.spawnBlock(
                            center.clone().add(x, 0.5, z), mat);
                    block.scale(1.8f, 0.5f, 1.8f).glow(255, 100, 20).interpolation(3, 0);
                    crossBlocks.add(block);
                    spawnedEntities.add(block.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double rotOffset = ticksAlive * 0.02;

            // Rotate arms
            for (int arm = 0; arm < 4; arm++) {
                double baseAngle = (Math.PI / 2) * arm + rotOffset;
                for (int seg = 0; seg < 3; seg++) {
                    int idx = arm * 3 + seg;
                    double dist = (seg + 1) * 2.0;
                    double x = Math.cos(baseAngle) * dist;
                    double z = Math.sin(baseAngle) * dist;
                    crossBlocks.get(idx).entity().teleport(c.clone().add(x, 0.5, z));
                }
            }

            // Fire particles along arms
            if (ticksAlive % 4 == 0) {
                for (int arm = 0; arm < 4; arm++) {
                    double angle = (Math.PI / 2) * arm + rotOffset;
                    for (double d = 0.5; d <= 6; d += 1.5) {
                        Location fireLoc = c.clone().add(Math.cos(angle) * d, 1, Math.sin(angle) * d);
                        c.getWorld().spawnParticle(Particle.FLAME, fireLoc, 2, 0.2, 0.3, 0.2, 0.02);
                    }
                }
            }

            // Pulsing center glow
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 0.6f, 0.5f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new InfernalCross(plugin); }
    }

    // ================================================================
    // 7. FIRE STORM — 15 small fire blocks orbiting chaotically at
    //    varying speeds and heights, like a firestorm tornado.
    //    Blocks move in elliptical paths with random perturbation.
    // ================================================================
    public static class FireStorm extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> fireBlocks = new ArrayList<>();
        private final List<Double> orbitRadii = new ArrayList<>();
        private final List<Double> orbitSpeeds = new ArrayList<>();
        private final List<Double> baseHeights = new ArrayList<>();
        private final List<Double> phaseOffsets = new ArrayList<>();

        public FireStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fire_storm", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(50.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 15; i++) {
                Material mat = (i % 3 == 0) ? Material.MAGMA_BLOCK
                        : (i % 3 == 1) ? Material.ORANGE_CONCRETE
                        : Material.YELLOW_CONCRETE;
                BlockDisplayHandle block = displayBuilder.spawnBlock(center, mat);
                float scale = 0.6f + (float)(Math.random() * 0.8);
                block.scale(scale, scale, scale).glow(255, 160, 30).interpolation(2, 0);
                fireBlocks.add(block);
                spawnedEntities.add(block.entity());

                orbitRadii.add(2.0 + Math.random() * 5.0);
                orbitSpeeds.add(0.03 + Math.random() * 0.08);
                baseHeights.add(0.5 + Math.random() * 6.0);
                phaseOffsets.add(Math.random() * Math.PI * 2);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < fireBlocks.size(); i++) {
                double angle = ticksAlive * orbitSpeeds.get(i) + phaseOffsets.get(i);
                double r = orbitRadii.get(i);
                double x = Math.cos(angle) * r;
                double z = Math.sin(angle) * r * 0.7; // Elliptical
                double y = baseHeights.get(i) + Math.sin(angle * 1.5) * 1.5;

                fireBlocks.get(i).entity().teleport(c.clone().add(x, y, z));
                fireBlocks.get(i).rotate(ticksAlive * 0.08f, 1, 1, 0);

                // Fire trail
                if (ticksAlive % 4 == 0 && i % 3 == 0) {
                    c.getWorld().spawnParticle(Particle.FLAME,
                            c.clone().add(x, y, z), 2, 0.1, 0.1, 0.1, 0.02);
                }
            }

            // Central updraft
            if (ticksAlive % 6 == 0) {
                c.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, c.clone().add(0, 0.5, 0),
                        3, 0.5, 0.3, 0.5, 0.03);
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 0.7f, 0.6f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new FireStorm(plugin); }
    }

    // ================================================================
    // 8. BRIMSTONE WALL — 3 rows × 5 columns (15 blocks) forming a
    //    wall of brimstone that sweeps toward the player. Wall has
    //    magma seams between blocks. Hard to dodge wide barrier.
    // ================================================================
    public static class BrimstoneWall extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> wallBlocks = new ArrayList<>();
        private float sweepZ = -8.0f;
        private static final float SWEEP_SPEED = 0.12f;
        private static final float WALL_WIDTH = 10.0f;

        public BrimstoneWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_wall", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(65.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 5; col++) {
                    double x = (col - 2) * (WALL_WIDTH / 5);
                    double y = row * 1.8;
                    Material mat = (row == 1 && col % 2 == 0) ? Material.MAGMA_BLOCK
                            : Material.RED_NETHER_BRICKS;
                    BlockDisplayHandle block = displayBuilder.spawnBlock(
                            center.clone().add(x, y, sweepZ), mat);
                    block.scale(WALL_WIDTH / 5, 1.8f, 0.8f).glow(200, 80, 20).interpolation(3, 0);
                    wallBlocks.add(block);
                    spawnedEntities.add(block.entity());
                }
            }

            DisplayBuilder.playSound(center.clone().add(0, 0, sweepZ),
                    Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            sweepZ += SWEEP_SPEED;

            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 5; col++) {
                    int idx = row * 5 + col;
                    double x = (col - 2) * (WALL_WIDTH / 5);
                    double y = row * 1.8;
                    wallBlocks.get(idx).entity().teleport(c.clone().add(x, y, sweepZ));
                }
            }

            // Leading edge fire
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 4; i++) {
                    double x = Math.random() * WALL_WIDTH - WALL_WIDTH / 2;
                    double y = Math.random() * 5;
                    c.getWorld().spawnParticle(Particle.FLAME,
                            c.clone().add(x, y, sweepZ + 0.5), 2, 0.2, 0.3, 0.1, 0.02);
                }
            }

            // Rumble
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 0, sweepZ), Sound.BLOCK_BASALT_BREAK, 0.7f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new BrimstoneWall(plugin); }
    }

    // ================================================================
    // 9. HELLFIRE STARFISH — 5 arms of 2 blocks each + center (11 total)
    //    radiating from center at ground level. Arms extend outward
    //    then retract, pulsing. Fire erupts from tips on extension.
    // ================================================================
    public static class HellfireStarfish extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> armBlocks = new ArrayList<>();
        private BlockDisplayHandle center_block;
        private double armExtension = 0;

        public HellfireStarfish(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellfire_starfish", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(50.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            center_block = displayBuilder.spawnBlock(center.clone().add(0, 0.5, 0), Material.MAGMA_BLOCK);
            center_block.scale(2.5f, 1.0f, 2.5f).glow(255, 120, 20);
            spawnedEntities.add(center_block.entity());

            for (int arm = 0; arm < 5; arm++) {
                double angle = (2 * Math.PI * arm) / 5;
                for (int seg = 1; seg <= 2; seg++) {
                    double dist = seg * 2.0;
                    double x = Math.cos(angle) * dist;
                    double z = Math.sin(angle) * dist;
                    Material mat = (seg == 1) ? Material.NETHERRACK : Material.MAGMA_BLOCK;
                    BlockDisplayHandle block = displayBuilder.spawnBlock(
                            center.clone().add(x, 0.5, z), mat);
                    block.scale(1.8f, 0.8f, 1.8f).glow(255, 100, 20).interpolation(3, 0);
                    armBlocks.add(block);
                    spawnedEntities.add(block.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Pulsing extension
            armExtension = 1.0 + Math.sin(ticksAlive * 0.06) * 1.5;

            for (int arm = 0; arm < 5; arm++) {
                double angle = (2 * Math.PI * arm) / 5;
                for (int seg = 0; seg < 2; seg++) {
                    int idx = arm * 2 + seg;
                    double baseDist = (seg + 1) * 2.0;
                    double dist = baseDist + armExtension * (seg + 1) * 0.5;
                    double x = Math.cos(angle) * dist;
                    double z = Math.sin(angle) * dist;
                    armBlocks.get(idx).entity().teleport(c.clone().add(x, 0.5, z));
                }

                // Fire at tips on full extension
                if (armExtension > 2.0 && ticksAlive % 5 == 0) {
                    double tipDist = 4.0 + armExtension;
                    Location tip = c.clone().add(
                            Math.cos(angle) * tipDist, 1, Math.sin(angle) * tipDist);
                    c.getWorld().spawnParticle(Particle.FLAME, tip, 8, 0.3, 0.5, 0.3, 0.04);
                }
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_POP, 0.5f, 0.7f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new HellfireStarfish(plugin); }
    }

    // ================================================================
    // 10. MOLTEN RAIN — 12 small magma blocks fall from 15 blocks up,
    //     tracking the player. Each leaves a fire trail. Impact creates
    //     small lava splashes. Staggered spawns.
    // ================================================================
    public static class MoltenRain extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> drops = new ArrayList<>();
        private final List<Float> yPositions = new ArrayList<>();
        private final List<Float> fallSpeeds = new ArrayList<>();
        private final List<Double> xOffsets = new ArrayList<>();
        private final List<Double> zOffsets = new ArrayList<>();
        private final List<Integer> spawnDelays = new ArrayList<>();
        private final List<Boolean> landed = new ArrayList<>();

        public MoltenRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("molten_rain", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(280);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(55.0);
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 12; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = Math.random() * 5.0;
                xOffsets.add(Math.cos(angle) * dist);
                zOffsets.add(Math.sin(angle) * dist);
                yPositions.add(15.0f);
                fallSpeeds.add(0.2f + (float)(Math.random() * 0.2));
                spawnDelays.add(i * 5);
                landed.add(false);
                drops.add(null);
            }

            DisplayBuilder.playSound(center.clone().add(0, 15, 0), Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < 12; i++) {
                if (ticksAlive < spawnDelays.get(i)) continue;
                if (landed.get(i)) continue;

                if (drops.get(i) == null) {
                    Location spawnLoc = c.clone().add(xOffsets.get(i), 15, zOffsets.get(i));
                    Material mat = (i % 2 == 0) ? Material.MAGMA_BLOCK : Material.ORANGE_CONCRETE;
                    BlockDisplayHandle drop = displayBuilder.spawnBlock(spawnLoc, mat);
                    drop.scale(0.8f, 0.8f, 0.8f).glow(255, 150, 30).interpolation(1, 0);
                    drops.set(i, drop);
                    spawnedEntities.add(drop.entity());
                }

                float y = yPositions.get(i) - fallSpeeds.get(i);
                yPositions.set(i, y);

                drops.get(i).entity().teleport(c.clone().add(xOffsets.get(i), y, zOffsets.get(i)));
                drops.get(i).rotate(ticksAlive * 0.1f, 1, 0, 1);

                // Fire trail
                if (ticksAlive % 2 == 0) {
                    Location trail = c.clone().add(xOffsets.get(i), y + 0.5, zOffsets.get(i));
                    c.getWorld().spawnParticle(Particle.FLAME, trail, 2, 0.1, 0.1, 0.1, 0.01);
                }

                // Landing
                if (y <= 0) {
                    landed.set(i, true);
                    Location impactLoc = c.clone().add(xOffsets.get(i), 0, zOffsets.get(i));
                    triggerImpactDamage(impactLoc);
                    c.getWorld().spawnParticle(Particle.LAVA, impactLoc, 8, 0.5, 0.2, 0.5, 0);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_LAVA_EXTINGUISH, 0.6f, 0.8f);
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new MoltenRain(plugin); }
    }

    // ================================================================
    // 11. INFERNAL CAGE — 8 vertical bars + 4 horizontal bars (12 total)
    //     forming a cage around the player that slowly heats up.
    //     Bars start blackstone, gain magma glow over time.
    // ================================================================
    public static class InfernalCage extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> verticalBars = new ArrayList<>();
        private final List<BlockDisplayHandle> horizontalBars = new ArrayList<>();
        private float cageRadius = 4.0f;

        public InfernalCage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_cage", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(55.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 8 vertical bars around circumference
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                double x = Math.cos(angle) * cageRadius;
                double z = Math.sin(angle) * cageRadius;
                BlockDisplayHandle bar = displayBuilder.spawnBlock(
                        center.clone().add(x, 2, z), Material.POLISHED_BLACKSTONE);
                bar.scale(0.5f, 5.0f, 0.5f).glow(60, 50, 50).interpolation(4, 0);
                verticalBars.add(bar);
                spawnedEntities.add(bar.entity());
            }

            // 4 horizontal connecting bars (top)
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI / 2) * i;
                double x = Math.cos(angle) * cageRadius * 0.7;
                double z = Math.sin(angle) * cageRadius * 0.7;
                BlockDisplayHandle hBar = displayBuilder.spawnBlock(
                        center.clone().add(x, 4.5, z), Material.POLISHED_BLACKSTONE);
                hBar.scale(cageRadius * 1.5f, 0.4f, 0.4f).glow(60, 50, 50).interpolation(4, 0);
                hBar.rotate((float) angle, 0, 1, 0);
                horizontalBars.add(hBar);
                spawnedEntities.add(hBar.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_PLACE, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Cage slowly heats up — glow color shifts from gray to orange to red
            float heatProgress = Math.min(1.0f, ticksAlive / 300.0f);
            int r = (int)(60 + heatProgress * 195);
            int g = (int)(50 + heatProgress * 70);
            int b = (int)(50 - heatProgress * 30);

            for (BlockDisplayHandle bar : verticalBars) {
                bar.glow(r, g, b);
            }
            for (BlockDisplayHandle bar : horizontalBars) {
                bar.glow(r, g, b);
            }

            // Cage constricts slightly
            if (cageRadius > 2.5f) {
                cageRadius -= 0.005f;
                for (int i = 0; i < 8; i++) {
                    double angle = (2 * Math.PI * i) / 8;
                    double x = Math.cos(angle) * cageRadius;
                    double z = Math.sin(angle) * cageRadius;
                    verticalBars.get(i).entity().teleport(c.clone().add(x, 2, z));
                }
            }

            // Heat particles when hot
            if (heatProgress > 0.5f && ticksAlive % 6 == 0) {
                double angle = Math.random() * Math.PI * 2;
                Location pLoc = c.clone().add(
                        Math.cos(angle) * cageRadius, Math.random() * 4, Math.sin(angle) * cageRadius);
                c.getWorld().spawnParticle(Particle.FLAME, pLoc, 2, 0.1, 0.3, 0.1, 0.02);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_AMBIENT, 0.5f, 0.6f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new InfernalCage(plugin); }
    }

    // ================================================================
    // 12. VOLCANIC SPINE — 10 blocks forming a jagged ridge/spine
    //     that erupts from the ground diagonally. Each segment rises
    //     at different heights creating a mountain ridge shape.
    // ================================================================
    public static class VolcanicSpine extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> spineBlocks = new ArrayList<>();
        private final List<Float> targetHeights = new ArrayList<>();
        private final List<Float> currentHeights = new ArrayList<>();

        public VolcanicSpine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("volcanic_spine", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(55.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 10; i++) {
                double x = (i - 4.5) * 1.5;
                // Ridge shape — higher in middle
                float targetH = (float)(4.0 * Math.exp(-0.15 * (i - 4.5) * (i - 4.5)));
                targetH += (float)(Math.random() * 1.5);
                targetHeights.add(targetH);
                currentHeights.add(-1.0f);

                Material mat = (i % 3 == 0) ? Material.BASALT
                        : (i % 3 == 1) ? Material.NETHERRACK
                        : Material.BLACKSTONE;
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(x, -1, 0), mat);
                // Pointed/jagged shapes
                float width = 1.2f + (float)(Math.random() * 0.6);
                block.scale(width, 2.0f, width).glow(180, 80, 30).interpolation(4, 0);
                // Tilt slightly for natural jagged look
                float tilt = (float)((Math.random() - 0.5) * 0.3);
                block.rotate(tilt, 0, 0, 1);
                spineBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < spineBlocks.size(); i++) {
                int activationDelay = i * 8;
                if (ticksAlive < activationDelay) continue;

                float current = currentHeights.get(i);
                float target = targetHeights.get(i);
                if (current < target) {
                    current += 0.2f;
                    if (current > target) current = target;
                    currentHeights.set(i, current);

                    // Rising sound
                    if (ticksAlive == activationDelay + 1) {
                        DisplayBuilder.playSound(c.clone().add((i - 4.5) * 1.5, 0, 0),
                                Sound.BLOCK_BASALT_BREAK, 0.6f, 0.5f);
                    }
                }

                double x = (i - 4.5) * 1.5;
                spineBlocks.get(i).entity().teleport(c.clone().add(x, current, 0));

                // Steam/fire at tips
                if (current >= target * 0.8 && ticksAlive % 8 == 0) {
                    Location tipLoc = c.clone().add(x, current + 1.5, 0);
                    c.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, tipLoc,
                            2, 0.2, 0.3, 0.2, 0.02);
                    c.getWorld().spawnParticle(Particle.FLAME, tipLoc, 1, 0.1, 0.2, 0.1, 0.01);
                }
            }

            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_POP, 0.4f, 0.7f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new VolcanicSpine(plugin); }
    }

    // ================================================================
    // 13. BRIMSTONE METEOR — 15-block sphere of magma/netherrack that
    //     forms in the sky, tracks the player, then slams down.
    //     Massive impact damage and explosion particles.
    // ================================================================
    public static class BrimstoneMeteor extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> meteorBlocks = new ArrayList<>();
        private float meteorY = 18.0f;
        private boolean falling = false;
        private boolean impacted = false;

        public BrimstoneMeteor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_meteor", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(40.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(350);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(100.0);
            config.setImpactRadius(9.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location meteorCenter = center.clone().add(0, meteorY, 0);
            List<BlockDisplayHandle> sphere = displayBuilder.spawnSphere(
                    meteorCenter, Material.MAGMA_BLOCK, 2.5, 15);
            for (BlockDisplayHandle block : sphere) {
                block.scale(1.3f, 1.3f, 1.3f).glow(255, 120, 20).interpolation(2, 0);
                meteorBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(meteorCenter, Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (impacted) return;

            // Phase 1: Hover and charge (0-60 ticks)
            if (ticksAlive < 60) {
                float bob = (float) Math.sin(ticksAlive * 0.1) * 0.5f;
                meteorY = 18.0f + bob;

                // Charging particles
                if (ticksAlive % 4 == 0) {
                    Location meteorLoc = c.clone().add(0, meteorY, 0);
                    c.getWorld().spawnParticle(Particle.FLAME, meteorLoc, 8, 2, 2, 2, 0.03);
                    DisplayBuilder.dustParticles(meteorLoc, 5, 2.0, 255, 120, 20, 1.5f);
                }

                // Warning sound
                if (ticksAlive == 50) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_TNT_PRIMED, 1.0f, 0.5f);
                }
            }
            // Phase 2: Fall (60+ ticks)
            else {
                if (!falling) {
                    falling = true;
                    DisplayBuilder.playSound(c.clone().add(0, meteorY, 0),
                            Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 0.3f);
                }

                meteorY -= 0.8f;

                // Fire trail
                if (ticksAlive % 2 == 0) {
                    Location trailLoc = c.clone().add(0, meteorY + 2, 0);
                    c.getWorld().spawnParticle(Particle.FLAME, trailLoc, 10, 1, 1, 1, 0.05);
                    c.getWorld().spawnParticle(Particle.SMOKE, trailLoc, 5, 0.5, 0.5, 0.5, 0.03);
                }

                // Impact
                if (meteorY <= 1.0f) {
                    impacted = true;
                    meteorY = 1.0f;
                    triggerImpactDamage(c);

                    // Massive explosion effect
                    c.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, c, 3, 1, 1, 1, 0);
                    c.getWorld().spawnParticle(Particle.FLAME, c, 40, 4, 2, 4, 0.1);
                    c.getWorld().spawnParticle(Particle.LAVA, c, 20, 3, 1, 3, 0);
                    DisplayBuilder.particleRing(c, 8.0, Particle.FLAME, 32, null);
                    DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.4f);
                    DisplayBuilder.playSound(c, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 0.3f);
                }
            }

            // Move all meteor blocks
            if (!impacted) {
                Location meteorCenter = c.clone().add(0, meteorY, 0);
                // Reposition sphere blocks (approximate — offset from center)
                double goldenAngle = Math.PI * (3 - Math.sqrt(5));
                for (int i = 0; i < meteorBlocks.size(); i++) {
                    double yi = 1 - (2.0 * i / (meteorBlocks.size() - 1));
                    double radiusAtY = Math.sqrt(1 - yi * yi);
                    double theta = goldenAngle * i + ticksAlive * 0.02; // Slow spin
                    double x = Math.cos(theta) * radiusAtY * 2.5;
                    double z = Math.sin(theta) * radiusAtY * 2.5;
                    meteorBlocks.get(i).entity().teleport(
                            meteorCenter.clone().add(x, yi * 2.5, z));
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new BrimstoneMeteor(plugin); }
    }
}
