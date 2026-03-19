package com.blockforge.chaoscraft.modes.devilsdream.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Devil's Dream — DREAM DISTORTION ATTACKS
 * 13 reality-warping BlockDisplay attacks featuring gravity inversions,
 * dimension bleeds, recursive structures, and space-folding geometry.
 *
 * Color palette (surreal/distortion):
 * - Prismarine teal: prismarine, dark_prismarine
 * - End pale: end_stone, end_stone_bricks, purpur_block
 * - Amethyst purple: amethyst_block, budding_amethyst
 * - Glass/translucent: stained glass variants
 * - Copper/oxidized: oxidized_copper, weathered_copper
 */
public final class DreamDistortions {

    private DreamDistortions() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new RealityTear(plugin));
        registry.register(new GravityInversion(plugin));
        registry.register(new SizeDistortion(plugin));
        registry.register(new TimeLoop(plugin));
        registry.register(new InvertedRain(plugin));
        registry.register(new WarpedGrid(plugin));
        registry.register(new FoldingCube(plugin));
        registry.register(new StretchedCorridor(plugin));
        registry.register(new FragmentedSphere(plugin));
        registry.register(new DimensionBleed(plugin));
        registry.register(new MobiusRing(plugin));
        registry.register(new RecursivePyramid(plugin));
        registry.register(new PhaseShift(plugin));
    }

    // ================================================================
    // 1. REALITY TEAR — Vertical rip in space: 2 jagged edges (6 each)
    //    with void/portal particles in between. Edges slowly widen.
    // ================================================================
    public static class RealityTear extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> leftEdge = new ArrayList<>();
        private final List<BlockDisplayHandle> rightEdge = new ArrayList<>();
        private float tearWidth = 0.2f;

        public RealityTear(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("reality_tear", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
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

            for (int i = 0; i < 6; i++) {
                double y = i * 1.2;
                double jagged = (i % 2 == 0) ? -0.3 : 0.3;

                BlockDisplayHandle left = displayBuilder.spawnBlock(
                        center.clone().add(-tearWidth + jagged, y, 0), Material.END_STONE);
                left.scale(0.4f, 1.2f, 0.5f).glow(180, 160, 120).interpolation(4, 0);
                leftEdge.add(left);
                spawnedEntities.add(left.entity());

                BlockDisplayHandle right = displayBuilder.spawnBlock(
                        center.clone().add(tearWidth - jagged, y, 0), Material.END_STONE);
                right.scale(0.4f, 1.2f, 0.5f).glow(180, 160, 120).interpolation(4, 0);
                rightEdge.add(right);
                spawnedEntities.add(right.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Widen the tear
            if (tearWidth < 2.5f) tearWidth += 0.01f;

            for (int i = 0; i < 6; i++) {
                double y = i * 1.2;
                double jagged = (i % 2 == 0) ? -0.3 : 0.3;
                leftEdge.get(i).entity().teleport(c.clone().add(-tearWidth + jagged, y, 0));
                rightEdge.get(i).entity().teleport(c.clone().add(tearWidth - jagged, y, 0));
            }

            // Portal particles in the gap
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 4; i++) {
                    double y = Math.random() * 7;
                    double x = (Math.random() - 0.5) * tearWidth * 1.5;
                    c.getWorld().spawnParticle(Particle.REVERSE_PORTAL,
                            c.clone().add(x, y, 0), 3, 0.2, 0.3, 0.1, 0.05);
                    c.getWorld().spawnParticle(Particle.PORTAL,
                            c.clone().add(x, y, 0), 2, 0.1, 0.2, 0.1, 0.3);
                }
            }

            // Tearing sound
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.4f, 0.3f + (float)(Math.random() * 0.3));
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new RealityTear(plugin); }
    }

    // ================================================================
    // 2. GRAVITY INVERSION — 12 blocks float upward from ground level,
    //    accelerating as they rise. Blocks are chunks of "ground" that
    //    detach and fly up. Dirt, grass, stone materials.
    // ================================================================
    public static class GravityInversion extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> chunks = new ArrayList<>();
        private final List<Float> yPositions = new ArrayList<>();
        private final List<Float> riseSpeeds = new ArrayList<>();
        private final List<Double> xOffsets = new ArrayList<>();
        private final List<Double> zOffsets = new ArrayList<>();
        private final List<Float> rotSpeeds = new ArrayList<>();

        public GravityInversion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gravity_inversion", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(50.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] mats = {Material.DIRT, Material.STONE, Material.COBBLESTONE,
                    Material.GRAVEL, Material.DEEPSLATE, Material.ANDESITE};

            for (int i = 0; i < 12; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = 1.0 + Math.random() * 5.0;
                xOffsets.add(Math.cos(angle) * dist);
                zOffsets.add(Math.sin(angle) * dist);
                yPositions.add(0.0f);
                riseSpeeds.add(0.02f + (float)(Math.random() * 0.04));
                rotSpeeds.add((float)(Math.random() * 0.1 - 0.05));

                Material mat = mats[i % mats.length];
                BlockDisplayHandle chunk = displayBuilder.spawnBlock(
                        center.clone().add(xOffsets.get(i), 0, zOffsets.get(i)), mat);
                float scale = 0.8f + (float)(Math.random() * 1.2);
                chunk.scale(scale, scale * 0.6f, scale).glow(100, 120, 80).interpolation(3, 0);
                chunks.add(chunk);
                spawnedEntities.add(chunk.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GRAVEL_BREAK, 1.0f, 0.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_SHULKER_TELEPORT, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < chunks.size(); i++) {
                // Accelerating rise
                float speed = riseSpeeds.get(i) + ticksAlive * 0.0005f;
                float y = yPositions.get(i) + speed;
                yPositions.set(i, y);

                chunks.get(i).entity().teleport(
                        c.clone().add(xOffsets.get(i), y, zOffsets.get(i)));
                chunks.get(i).rotate(ticksAlive * rotSpeeds.get(i), 1, 0.5f, 0);

                // Debris particles falling from rising chunks
                if (ticksAlive % 6 == 0 && Math.random() < 0.3) {
                    Location chunkLoc = c.clone().add(xOffsets.get(i), y - 0.5, zOffsets.get(i));
                    c.getWorld().spawnParticle(Particle.BLOCK,
                            chunkLoc, 3, 0.3, 0.1, 0.3, 0.02,
                            Material.DIRT.createBlockData());
                }
            }

            // Upward draft particles
            if (ticksAlive % 4 == 0) {
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL,
                        c.clone().add(Math.random() * 8 - 4, 0.5, Math.random() * 8 - 4),
                        3, 0.5, 0.5, 0.5, 0.08);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_SHULKER_AMBIENT, 0.4f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new GravityInversion(plugin); }
    }

    // ================================================================
    // 3. SIZE DISTORTION — 10 identical blocks that randomly grow
    //    and shrink at different rates. Creates disorienting scale
    //    differences. Amethyst blocks pulsing in/out.
    // ================================================================
    public static class SizeDistortion extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> blocks = new ArrayList<>();
        private final List<Double> pulsePhases = new ArrayList<>();
        private final List<Double> pulseSpeeds = new ArrayList<>();

        public SizeDistortion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("size_distortion", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(50.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 10; i++) {
                double angle = (2 * Math.PI * i) / 10;
                double x = Math.cos(angle) * 4.0;
                double z = Math.sin(angle) * 4.0;

                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(x, 2, z), Material.AMETHYST_BLOCK);
                block.scale(1.0f, 1.0f, 1.0f).glow(160, 100, 220).interpolation(5, 0);
                blocks.add(block);
                spawnedEntities.add(block.entity());
                pulsePhases.add(Math.random() * Math.PI * 2);
                pulseSpeeds.add(0.04 + Math.random() * 0.08);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < blocks.size(); i++) {
                double phase = pulsePhases.get(i);
                double speed = pulseSpeeds.get(i);
                float scale = 0.5f + (float)(Math.sin(ticksAlive * speed + phase) + 1.0) * 1.5f;

                blocks.get(i).animateTo(
                        new Vector3f(-scale / 2, -scale / 2, -scale / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(scale, scale, scale), 5);

                // Shimmer particles on large blocks
                if (scale > 2.5f && ticksAlive % 6 == 0) {
                    Location bLoc = blocks.get(i).entity().getLocation();
                    c.getWorld().spawnParticle(Particle.END_ROD, bLoc, 2, 0.5, 0.5, 0.5, 0.02);
                }
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.5f,
                        0.3f + (float)(Math.random() * 0.5));
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SizeDistortion(plugin); }
    }

    // ================================================================
    // 4. TIME LOOP — 12 blocks arranged in a clock circle that spin,
    //    stop, spin backward, stop — erratic temporal stutter.
    //    Copper blocks in various oxidation states.
    // ================================================================
    public static class TimeLoop extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> loopBlocks = new ArrayList<>();
        private double rotationSpeed = 0.05;
        private double totalRotation = 0;
        private int phase = 0;

        public TimeLoop(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("time_loop", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(50.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] mats = {Material.COPPER_BLOCK, Material.EXPOSED_COPPER,
                    Material.WEATHERED_COPPER, Material.OXIDIZED_COPPER};

            for (int i = 0; i < 12; i++) {
                double angle = (2 * Math.PI * i) / 12;
                double x = Math.cos(angle) * 4.0;
                double z = Math.sin(angle) * 4.0;

                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(x, 3, z), mats[i % 4]);
                block.scale(1.0f, 1.0f, 1.0f).glow(120, 160, 100).interpolation(2, 0);
                loopBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase changes: forward, stop, backward, stop
            if (ticksAlive % 50 == 0) {
                phase = (phase + 1) % 4;
                if (phase == 0) rotationSpeed = 0.06;
                else if (phase == 1) rotationSpeed = 0;
                else if (phase == 2) rotationSpeed = -0.08;
                else rotationSpeed = 0;

                // Stutter sound
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_HAT, 0.7f,
                        (phase == 2) ? 0.3f : 0.8f);
            }

            totalRotation += rotationSpeed;

            for (int i = 0; i < loopBlocks.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 12;
                double angle = baseAngle + totalRotation;
                double x = Math.cos(angle) * 4.0;
                double z = Math.sin(angle) * 4.0;
                double y = 3.0 + Math.sin(angle * 2) * 0.5;

                loopBlocks.get(i).entity().teleport(c.clone().add(x, y, z));
                loopBlocks.get(i).rotate((float) totalRotation, 0, 1, 0);
            }

            // Temporal particles
            if (ticksAlive % 4 == 0) {
                double angle = Math.random() * Math.PI * 2;
                Location pLoc = c.clone().add(Math.cos(angle) * 4, 3, Math.sin(angle) * 4);
                c.getWorld().spawnParticle(Particle.ENCHANT, pLoc, 5, 0.3, 0.3, 0.3, 0.5);
            }

            // Ticking sound
            if (ticksAlive % 10 == 0 && rotationSpeed != 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_HAT, 0.3f, 1.0f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new TimeLoop(plugin); }
    }

    // ================================================================
    // 5. INVERTED RAIN — 10 blocks rise from the ground like reverse
    //    rain. Once at height, they pause then crash back down.
    //    Prismarine blocks with bubble particles.
    // ================================================================
    public static class InvertedRain extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> drops = new ArrayList<>();
        private final List<Float> yPos = new ArrayList<>();
        private final List<Float> speeds = new ArrayList<>();
        private final List<Double> xOff = new ArrayList<>();
        private final List<Double> zOff = new ArrayList<>();
        private final List<Boolean> falling = new ArrayList<>();
        private final List<Integer> pauseAt = new ArrayList<>();

        public InvertedRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("inverted_rain", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(45.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(280);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(65.0);
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 10; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = Math.random() * 5.0;
                xOff.add(Math.cos(angle) * dist);
                zOff.add(Math.sin(angle) * dist);
                yPos.add(0.0f);
                speeds.add(0.3f + (float)(Math.random() * 0.2));
                falling.add(false);
                pauseAt.add(12 + (int)(Math.random() * 6));

                Material mat = (i % 2 == 0) ? Material.PRISMARINE : Material.DARK_PRISMARINE;
                BlockDisplayHandle drop = displayBuilder.spawnBlock(
                        center.clone().add(xOff.get(i), 0, zOff.get(i)), mat);
                drop.scale(0.7f, 0.7f, 0.7f).glow(80, 160, 150).interpolation(2, 0);
                drops.add(drop);
                spawnedEntities.add(drop.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < drops.size(); i++) {
                float y = yPos.get(i);

                if (!falling.get(i)) {
                    // Rising
                    y += speeds.get(i);
                    if (y >= pauseAt.get(i)) {
                        falling.set(i, true);
                        speeds.set(i, 0.0f); // Brief pause
                    }
                } else {
                    // Falling (accelerating)
                    float sp = speeds.get(i) - 0.06f;
                    speeds.set(i, sp);
                    y += sp;

                    if (y <= 0) {
                        y = 0;
                        Location impLoc = c.clone().add(xOff.get(i), 0, zOff.get(i));
                        triggerImpactDamage(impLoc);
                        c.getWorld().spawnParticle(Particle.SPLASH, impLoc, 10, 0.5, 0.2, 0.5, 0.05);
                        DisplayBuilder.playSound(impLoc, Sound.ENTITY_PLAYER_SPLASH, 0.5f, 0.8f);
                    }
                }

                yPos.set(i, y);
                drops.get(i).entity().teleport(c.clone().add(xOff.get(i), y, zOff.get(i)));

                // Upward trail when rising
                if (!falling.get(i) && ticksAlive % 3 == 0) {
                    c.getWorld().spawnParticle(Particle.BUBBLE_POP,
                            c.clone().add(xOff.get(i), y - 0.5, zOff.get(i)),
                            2, 0.1, 0.1, 0.1, 0.02);
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new InvertedRain(plugin); }
    }

    // ================================================================
    // 6. WARPED GRID — 16 blocks in a 4×4 grid that distort: some
    //    rise, some sink, some scale up, creating an uneven terrain.
    //    Purpur blocks warping in a wave pattern.
    // ================================================================
    public static class WarpedGrid extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> gridBlocks = new ArrayList<>();

        public WarpedGrid(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("warped_grid", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
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

            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) {
                    double x = (col - 1.5) * 2.5;
                    double z = (row - 1.5) * 2.5;
                    Material mat = ((row + col) % 2 == 0) ? Material.PURPUR_BLOCK : Material.END_STONE_BRICKS;
                    BlockDisplayHandle block = displayBuilder.spawnBlock(
                            center.clone().add(x, 0.5, z), mat);
                    block.scale(2.5f, 0.5f, 2.5f).glow(160, 120, 180).interpolation(5, 0);
                    gridBlocks.add(block);
                    spawnedEntities.add(block.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_FRAME_FILL, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) {
                    int idx = row * 4 + col;
                    double x = (col - 1.5) * 2.5;
                    double z = (row - 1.5) * 2.5;

                    // Wave height distortion
                    double waveY = Math.sin(ticksAlive * 0.05 + col * 0.8) * 2.0
                            + Math.cos(ticksAlive * 0.03 + row * 0.6) * 1.5;
                    // Scale distortion
                    float scalePulse = 2.5f + (float) Math.sin(ticksAlive * 0.04 + (row + col) * 0.5) * 0.8f;

                    gridBlocks.get(idx).entity().teleport(c.clone().add(x, 0.5 + waveY, z));
                    gridBlocks.get(idx).scale(scalePulse, 0.5f, scalePulse);
                }
            }

            // Distortion particles
            if (ticksAlive % 6 == 0) {
                double x = (Math.random() * 4 - 2) * 2.5;
                double z = (Math.random() * 4 - 2) * 2.5;
                c.getWorld().spawnParticle(Particle.ENCHANT, c.clone().add(x, 2, z),
                        5, 0.5, 0.5, 0.5, 0.5);
            }

            if (ticksAlive % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHORUS_FLOWER_GROW, 0.5f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new WarpedGrid(plugin); }
    }

    // ================================================================
    // 7. FOLDING CUBE — 6-face cube (12 blocks, 2 per face) that
    //    unfolds, flattens, then refolds in a different configuration.
    //    Quartz and prismarine for clean geometric look.
    // ================================================================
    public static class FoldingCube extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> faces = new ArrayList<>();
        private float foldProgress = 0;
        private boolean unfolding = true;

        public FoldingCube(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("folding_cube", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(50.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location cubeCenter = center.clone().add(0, 4, 0);

            Material[] mats = {Material.QUARTZ_BLOCK, Material.PRISMARINE};
            // 6 faces × 2 blocks each
            float size = 2.0f;
            // Top/Bottom
            faces.add(spawnFace(cubeCenter, 0, size, 0, size * 2, 0.3f, size * 2, mats[0]));
            faces.add(spawnFace(cubeCenter, 0, size, 0, size, 0.3f, size, mats[1]));
            faces.add(spawnFace(cubeCenter, 0, -size, 0, size * 2, 0.3f, size * 2, mats[0]));
            faces.add(spawnFace(cubeCenter, 0, -size, 0, size, 0.3f, size, mats[1]));
            // Front/Back
            faces.add(spawnFace(cubeCenter, 0, 0, size, size * 2, size * 2, 0.3f, mats[0]));
            faces.add(spawnFace(cubeCenter, 0, 0, size, size, size, 0.3f, mats[1]));
            faces.add(spawnFace(cubeCenter, 0, 0, -size, size * 2, size * 2, 0.3f, mats[0]));
            faces.add(spawnFace(cubeCenter, 0, 0, -size, size, size, 0.3f, mats[1]));
            // Left/Right
            faces.add(spawnFace(cubeCenter, size, 0, 0, 0.3f, size * 2, size * 2, mats[0]));
            faces.add(spawnFace(cubeCenter, size, 0, 0, 0.3f, size, size, mats[1]));
            faces.add(spawnFace(cubeCenter, -size, 0, 0, 0.3f, size * 2, size * 2, mats[0]));
            faces.add(spawnFace(cubeCenter, -size, 0, 0, 0.3f, size, size, mats[1]));

            DisplayBuilder.playSound(cubeCenter, Sound.BLOCK_PISTON_EXTEND, 0.8f, 0.5f);
        }

        private BlockDisplayHandle spawnFace(Location center, double ox, double oy, double oz,
                                             float sx, float sy, float sz, Material mat) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(140, 160, 170).interpolation(5, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            Location cubeCenter = c.clone().add(0, 4, 0);

            // Cycle unfold/fold
            if (unfolding) {
                foldProgress += 0.005f;
                if (foldProgress >= 1.0f) { foldProgress = 1.0f; unfolding = false; }
            } else {
                foldProgress -= 0.005f;
                if (foldProgress <= 0.0f) { foldProgress = 0.0f; unfolding = true; }
            }

            // Rotate the whole thing
            float rot = ticksAlive * 0.02f;

            // Expand outward when unfolding
            double expand = 2.0 + foldProgress * 3.0;

            // Update face positions with expansion
            double[][] offsets = {
                    {0, expand, 0}, {0, expand, 0},
                    {0, -expand, 0}, {0, -expand, 0},
                    {0, 0, expand}, {0, 0, expand},
                    {0, 0, -expand}, {0, 0, -expand},
                    {expand, 0, 0}, {expand, 0, 0},
                    {-expand, 0, 0}, {-expand, 0, 0}
            };

            for (int i = 0; i < faces.size(); i++) {
                double ox = offsets[i][0];
                double oy = offsets[i][1];
                double oz = offsets[i][2];
                // Apply rotation
                double rx = ox * Math.cos(rot) - oz * Math.sin(rot);
                double rz = ox * Math.sin(rot) + oz * Math.cos(rot);
                faces.get(i).entity().teleport(cubeCenter.clone().add(rx, oy, rz));
                faces.get(i).rotate(rot, 0, 1, 0);
            }

            // Geometric particles
            if (ticksAlive % 6 == 0) {
                c.getWorld().spawnParticle(Particle.END_ROD, cubeCenter, 3, expand, expand, expand, 0.01);
            }

            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(cubeCenter, Sound.BLOCK_PISTON_CONTRACT, 0.5f, 0.6f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new FoldingCube(plugin); }
    }

    // ================================================================
    // 8. STRETCHED CORRIDOR — 10 blocks forming a tunnel that stretches
    //    longer and longer, perspective distortion effect.
    //    Blocks spread apart over time.
    // ================================================================
    public static class StretchedCorridor extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> leftWall = new ArrayList<>();
        private final List<BlockDisplayHandle> rightWall = new ArrayList<>();
        private float stretchFactor = 1.0f;

        public StretchedCorridor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("stretched_corridor", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(50.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 5; i++) {
                double z = i * 2.0;
                // Scale gets smaller further away (perspective)
                float perspScale = 1.0f - i * 0.12f;

                BlockDisplayHandle left = displayBuilder.spawnBlock(
                        center.clone().add(-2, 1.5, z), Material.PRISMARINE_BRICKS);
                left.scale(0.4f, 3.0f * perspScale, 2.0f).glow(100, 140, 130).interpolation(3, 0);
                leftWall.add(left);
                spawnedEntities.add(left.entity());

                BlockDisplayHandle right = displayBuilder.spawnBlock(
                        center.clone().add(2, 1.5, z), Material.PRISMARINE_BRICKS);
                right.scale(0.4f, 3.0f * perspScale, 2.0f).glow(100, 140, 130).interpolation(3, 0);
                rightWall.add(right);
                spawnedEntities.add(right.entity());
            }

            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Stretch the corridor
            stretchFactor += 0.005f;

            for (int i = 0; i < 5; i++) {
                double z = i * 2.0 * stretchFactor;
                float perspScale = 1.0f - i * 0.12f;
                float wallGap = 2.0f - i * 0.15f; // Walls converge in distance

                leftWall.get(i).entity().teleport(c.clone().add(-wallGap, 1.5, z));
                rightWall.get(i).entity().teleport(c.clone().add(wallGap, 1.5, z));
                leftWall.get(i).scale(0.4f, 3.0f * perspScale, 2.0f * stretchFactor * 0.3f);
                rightWall.get(i).scale(0.4f, 3.0f * perspScale, 2.0f * stretchFactor * 0.3f);
            }

            // Depth particles
            if (ticksAlive % 6 == 0) {
                double z = Math.random() * 5 * 2 * stretchFactor;
                c.getWorld().spawnParticle(Particle.END_ROD,
                        c.clone().add(0, 1.5, z), 2, 0.5, 1.0, 0.3, 0.01);
            }

            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 0.4f, 0.5f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new StretchedCorridor(plugin); }
    }

    // ================================================================
    // 9. FRAGMENTED SPHERE — 14 blocks in sphere formation that
    //    explode outward then reassemble. Repeating cycle.
    //    End stone + purpur fragments.
    // ================================================================
    public static class FragmentedSphere extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> fragments = new ArrayList<>();
        private final List<double[]> basePositions = new ArrayList<>();
        private float fragmentProgress = 0;
        private boolean exploding = true;

        public FragmentedSphere(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fragmented_sphere", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(50.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location sphereCenter = center.clone().add(0, 4, 0);

            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < 14; i++) {
                double y = 1 - (2.0 * i / 13);
                double rAtY = Math.sqrt(1 - y * y);
                double theta = goldenAngle * i;
                double x = Math.cos(theta) * rAtY * 2.5;
                double z = Math.sin(theta) * rAtY * 2.5;
                basePositions.add(new double[]{x, y * 2.5, z});

                Material mat = (i % 2 == 0) ? Material.END_STONE_BRICKS : Material.PURPUR_BLOCK;
                BlockDisplayHandle frag = displayBuilder.spawnBlock(
                        sphereCenter.clone().add(x, y * 2.5, z), mat);
                frag.scale(1.2f, 1.2f, 1.2f).glow(180, 150, 200).interpolation(3, 0);
                fragments.add(frag);
                spawnedEntities.add(frag.entity());
            }

            DisplayBuilder.playSound(sphereCenter, Sound.BLOCK_END_PORTAL_SPAWN, 0.6f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            Location sphereCenter = c.clone().add(0, 4, 0);

            // Cycle explode/reassemble
            if (exploding) {
                fragmentProgress += 0.008f;
                if (fragmentProgress >= 1.0f) { fragmentProgress = 1.0f; exploding = false; }
            } else {
                fragmentProgress -= 0.008f;
                if (fragmentProgress <= 0.0f) { fragmentProgress = 0.0f; exploding = true; }
            }

            double expandMult = 1.0 + fragmentProgress * 3.0;

            for (int i = 0; i < fragments.size(); i++) {
                double[] base = basePositions.get(i);
                double x = base[0] * expandMult;
                double y = base[1] * expandMult;
                double z = base[2] * expandMult;
                fragments.get(i).entity().teleport(sphereCenter.clone().add(x, y, z));
                fragments.get(i).rotate(ticksAlive * 0.03f + i * 0.5f, 1, 1, 0);
            }

            // Particles connecting fragments
            if (fragmentProgress > 0.3f && ticksAlive % 4 == 0) {
                c.getWorld().spawnParticle(Particle.ENCHANT, sphereCenter,
                        8, expandMult * 2, expandMult * 2, expandMult * 2, 0.3);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(sphereCenter, Sound.BLOCK_BEACON_AMBIENT, 0.4f, 0.5f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new FragmentedSphere(plugin); }
    }

    // ================================================================
    // 10. DIMENSION BLEED — 3 overlapping square frames (4 blocks each
    //     = 12) at different angles, like 3 dimensions bleeding into
    //     one space. Nether, End, and Overworld colored.
    // ================================================================
    public static class DimensionBleed extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> netherFrame = new ArrayList<>();
        private final List<BlockDisplayHandle> endFrame = new ArrayList<>();
        private final List<BlockDisplayHandle> overFrame = new ArrayList<>();

        public DimensionBleed(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dimension_bleed", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
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
            Location frameCenter = center.clone().add(0, 4, 0);
            double size = 3.0;

            // Nether frame (red) — XY plane
            spawnFrame(frameCenter, netherFrame, Material.RED_NETHER_BRICKS, size, 0, 200, 50, 20);
            // End frame (yellow) — XZ plane
            spawnFrame(frameCenter, endFrame, Material.END_STONE_BRICKS, size, 1, 200, 200, 100);
            // Overworld frame (green) — YZ plane
            spawnFrame(frameCenter, overFrame, Material.MOSS_BLOCK, size, 2, 50, 180, 50);

            DisplayBuilder.playSound(frameCenter, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.4f);
        }

        private void spawnFrame(Location center, List<BlockDisplayHandle> list,
                                Material mat, double size, int plane, int gr, int gg, int gb) {
            double[][] offsets;
            float sx, sy, sz;
            if (plane == 0) { // XY
                offsets = new double[][]{{-size, size, 0}, {size, size, 0}, {size, -size, 0}, {-size, -size, 0}};
                sx = 0.5f; sy = 0.5f; sz = 0.3f;
            } else if (plane == 1) { // XZ
                offsets = new double[][]{{-size, 0, size}, {size, 0, size}, {size, 0, -size}, {-size, 0, -size}};
                sx = 0.5f; sy = 0.3f; sz = 0.5f;
            } else { // YZ
                offsets = new double[][]{{0, size, size}, {0, size, -size}, {0, -size, -size}, {0, -size, size}};
                sx = 0.3f; sy = 0.5f; sz = 0.5f;
            }

            for (double[] off : offsets) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), mat);
                h.scale(sx, sy, sz).glow(gr, gg, gb).interpolation(3, 0);
                list.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            Location fc = c.clone().add(0, 4, 0);

            double size = 3.0;
            // Rotate each frame on its own axis
            double netherRot = ticksAlive * 0.03;
            double endRot = ticksAlive * 0.025;
            double overRot = ticksAlive * 0.035;

            rotateFrame(netherFrame, fc, size, netherRot, 0);
            rotateFrame(endFrame, fc, size, endRot, 1);
            rotateFrame(overFrame, fc, size, overRot, 2);

            // Dimension bleed particles
            if (ticksAlive % 5 == 0) {
                c.getWorld().spawnParticle(Particle.PORTAL, fc, 5, 2, 2, 2, 0.5);
                DisplayBuilder.dustParticles(fc, 3, 2.0, 200, 50, 20, 1.0f);
                DisplayBuilder.dustParticles(fc, 3, 2.0, 200, 200, 100, 1.0f);
                DisplayBuilder.dustParticles(fc, 3, 2.0, 50, 180, 50, 1.0f);
            }

            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(fc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 0.5f);
            }
        }

        private void rotateFrame(List<BlockDisplayHandle> frame, Location center,
                                 double size, double angle, int plane) {
            double[][] base;
            if (plane == 0) {
                base = new double[][]{{-size, size, 0}, {size, size, 0}, {size, -size, 0}, {-size, -size, 0}};
            } else if (plane == 1) {
                base = new double[][]{{-size, 0, size}, {size, 0, size}, {size, 0, -size}, {-size, 0, -size}};
            } else {
                base = new double[][]{{0, size, size}, {0, size, -size}, {0, -size, -size}, {0, -size, size}};
            }

            for (int i = 0; i < frame.size(); i++) {
                double ox = base[i][0], oy = base[i][1], oz = base[i][2];
                double rx, ry, rz;
                if (plane == 0) { // Rotate around Z
                    rx = ox * Math.cos(angle) - oy * Math.sin(angle);
                    ry = ox * Math.sin(angle) + oy * Math.cos(angle);
                    rz = oz;
                } else if (plane == 1) { // Rotate around Y
                    rx = ox * Math.cos(angle) - oz * Math.sin(angle);
                    ry = oy;
                    rz = ox * Math.sin(angle) + oz * Math.cos(angle);
                } else { // Rotate around X
                    rx = ox;
                    ry = oy * Math.cos(angle) - oz * Math.sin(angle);
                    rz = oy * Math.sin(angle) + oz * Math.cos(angle);
                }
                frame.get(i).entity().teleport(center.clone().add(rx, ry, rz));
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new DimensionBleed(plugin); }
    }

    // ================================================================
    // 11. MOBIUS RING — 12 blocks in a twisted ring (Möbius strip).
    //     Ring rotates while maintaining the half-twist. Blocks flip
    //     as they traverse. Sea lantern + prismarine.
    // ================================================================
    public static class MobiusRing extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> stripBlocks = new ArrayList<>();
        private static final int STRIP_COUNT = 12;

        public MobiusRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("mobius_ring", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(50.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location ringCenter = center.clone().add(0, 4, 0);

            for (int i = 0; i < STRIP_COUNT; i++) {
                Material mat = (i % 2 == 0) ? Material.SEA_LANTERN : Material.DARK_PRISMARINE;
                BlockDisplayHandle block = displayBuilder.spawnBlock(ringCenter, mat);
                block.scale(1.5f, 0.3f, 1.0f).glow(100, 180, 180).interpolation(2, 0);
                stripBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(ringCenter, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            Location ringCenter = c.clone().add(0, 4, 0);

            double ringRadius = 4.0;
            double spinOffset = ticksAlive * 0.03;

            for (int i = 0; i < STRIP_COUNT; i++) {
                double t = (double) i / STRIP_COUNT; // 0 to 1 around the loop
                double angle = t * 2 * Math.PI + spinOffset;

                double x = Math.cos(angle) * ringRadius;
                double z = Math.sin(angle) * ringRadius;
                // Möbius twist: half rotation around the ring
                double twist = t * Math.PI; // 180° over full loop
                double y = Math.sin(twist) * 1.0;

                stripBlocks.get(i).entity().teleport(ringCenter.clone().add(x, y, z));
                // Each block rotates to follow the twist
                stripBlocks.get(i).rotate((float)(angle + twist), 0, 1, 0);
            }

            // Flowing particles along the strip
            if (ticksAlive % 4 == 0) {
                double t = (ticksAlive * 0.02) % 1.0;
                double angle = t * 2 * Math.PI + spinOffset;
                Location pLoc = ringCenter.clone().add(
                        Math.cos(angle) * ringRadius, Math.sin(t * Math.PI), Math.sin(angle) * ringRadius);
                DisplayBuilder.dustParticles(pLoc, 3, 0.2, 100, 180, 180, 1.2f);
            }

            if (ticksAlive % 35 == 0) {
                DisplayBuilder.playSound(ringCenter, Sound.BLOCK_BEACON_AMBIENT, 0.4f, 0.6f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new MobiusRing(plugin); }
    }

    // ================================================================
    // 12. RECURSIVE PYRAMID — Pyramid of 10 blocks that contains
    //     smaller pyramids that orbit around it. Fractal-like.
    //     Amethyst + calcite + smooth basalt.
    // ================================================================
    public static class RecursivePyramid extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> mainPyramid = new ArrayList<>();
        private final List<BlockDisplayHandle> miniPyramids = new ArrayList<>();

        public RecursivePyramid(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("recursive_pyramid", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(50.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location pyrCenter = center.clone().add(0, 2, 0);

            // Main pyramid (base 3×3 = 5 + middle 2×2 ish = 3 + top = 2 = 10)
            // Base layer
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue; // Skip center of base for variety
                    BlockDisplayHandle b = displayBuilder.spawnBlock(
                            pyrCenter.clone().add(x * 1.5, 0, z * 1.5), Material.AMETHYST_BLOCK);
                    b.scale(1.5f, 1.0f, 1.5f).glow(160, 100, 220).interpolation(3, 0);
                    mainPyramid.add(b);
                    spawnedEntities.add(b.entity());
                }
            }
            // Middle layer
            BlockDisplayHandle mid = displayBuilder.spawnBlock(
                    pyrCenter.clone().add(0, 1.5, 0), Material.CALCITE);
            mid.scale(2.0f, 1.0f, 2.0f).glow(200, 200, 210);
            mainPyramid.add(mid);
            spawnedEntities.add(mid.entity());

            // Apex
            BlockDisplayHandle apex = displayBuilder.spawnBlock(
                    pyrCenter.clone().add(0, 3, 0), Material.BUDDING_AMETHYST);
            apex.scale(1.0f, 1.5f, 1.0f).glow(180, 120, 255);
            mainPyramid.add(apex);
            spawnedEntities.add(apex.entity());

            // 4 mini pyramids orbiting
            for (int i = 0; i < 4; i++) {
                BlockDisplayHandle mini = displayBuilder.spawnBlock(pyrCenter, Material.AMETHYST_BLOCK);
                mini.scale(0.6f, 0.8f, 0.6f).glow(160, 100, 220).interpolation(2, 0);
                miniPyramids.add(mini);
                spawnedEntities.add(mini.entity());
            }

            DisplayBuilder.playSound(pyrCenter, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            Location pyrCenter = c.clone().add(0, 2, 0);

            // Slow rotation of main pyramid
            float mainRot = ticksAlive * 0.02f;

            // Mini pyramids orbit
            for (int i = 0; i < miniPyramids.size(); i++) {
                double angle = (2 * Math.PI * i) / 4 + ticksAlive * 0.05;
                double orbitR = 4.0 + Math.sin(ticksAlive * 0.03 + i) * 0.5;
                double y = 2.0 + Math.sin(ticksAlive * 0.04 + i * 1.5) * 1.5;
                double x = Math.cos(angle) * orbitR;
                double z = Math.sin(angle) * orbitR;

                miniPyramids.get(i).entity().teleport(pyrCenter.clone().add(x, y, z));
                miniPyramids.get(i).rotate(ticksAlive * 0.08f + i, 0, 1, 0);
            }

            // Crystal particles
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(pyrCenter.clone().add(0, 2, 0),
                        5, 2.0, 160, 100, 220, 1.2f);
                c.getWorld().spawnParticle(Particle.END_ROD, pyrCenter.clone().add(0, 3, 0),
                        2, 0.3, 0.3, 0.3, 0.02);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(pyrCenter, Sound.BLOCK_AMETHYST_CLUSTER_HIT, 0.5f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new RecursivePyramid(plugin); }
    }

    // ================================================================
    // 13. PHASE SHIFT — 10 blocks that flicker between visible and
    //     near-invisible (tiny scale), creating a strobe/phase effect.
    //     Blocks phase in and out at different rates.
    //     Mixed materials for chaotic dimension-hopping look.
    // ================================================================
    public static class PhaseShift extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> phaseBlocks = new ArrayList<>();
        private final List<Double> phaseOffsets = new ArrayList<>();
        private final List<Double> phaseSpeeds = new ArrayList<>();

        public PhaseShift(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phase_shift", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
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

            Material[] mats = {Material.NETHERRACK, Material.END_STONE, Material.MOSS_BLOCK,
                    Material.SOUL_SAND, Material.PURPUR_BLOCK, Material.PRISMARINE,
                    Material.AMETHYST_BLOCK, Material.DEEPSLATE, Material.SANDSTONE, Material.ICE};

            for (int i = 0; i < 10; i++) {
                double angle = (2 * Math.PI * i) / 10;
                double x = Math.cos(angle) * 4.0;
                double z = Math.sin(angle) * 4.0;

                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(x, 2, z), mats[i]);
                block.scale(1.5f, 1.5f, 1.5f).glow(150, 150, 150).interpolation(2, 0);
                phaseBlocks.add(block);
                spawnedEntities.add(block.entity());
                phaseOffsets.add(Math.random() * Math.PI * 2);
                phaseSpeeds.add(0.05 + Math.random() * 0.1);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < phaseBlocks.size(); i++) {
                double phase = phaseOffsets.get(i);
                double speed = phaseSpeeds.get(i);
                double sineVal = Math.sin(ticksAlive * speed + phase);

                // Phase between full size and nearly invisible
                float scale;
                if (sineVal > 0.3) {
                    scale = 1.5f; // Visible
                } else if (sineVal > -0.3) {
                    scale = 0.5f + (float)((sineVal + 0.3) / 0.6) * 1.0f; // Transitioning
                } else {
                    scale = 0.05f; // Nearly invisible
                }

                phaseBlocks.get(i).scale(scale, scale, scale);

                // Teleport flash on phase-in
                if (sineVal > 0.28 && Math.sin((ticksAlive - 1) * speed + phase) <= 0.28) {
                    Location bLoc = phaseBlocks.get(i).entity().getLocation();
                    c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, bLoc, 5, 0.3, 0.3, 0.3, 0.1);
                    DisplayBuilder.playSound(bLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.3f, 1.0f);
                }
            }

            // Ambient dimensional instability
            if (ticksAlive % 8 == 0) {
                double angle = Math.random() * Math.PI * 2;
                c.getWorld().spawnParticle(Particle.PORTAL,
                        c.clone().add(Math.cos(angle) * 4, 2, Math.sin(angle) * 4),
                        5, 0.3, 0.3, 0.3, 0.5);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new PhaseShift(plugin); }
    }
}
