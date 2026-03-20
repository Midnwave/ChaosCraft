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
 * Doom Mode — BLOCK DISPLAY ATTACKS
 * 17 hellfire and doom-themed BlockDisplay attacks forming recognizable structures.
 * Each attack has 10+ block display entities, real geometry, and smooth animations.
 *
 * Doom palette:
 * - Lava orange: RGB(255, 80, 20)
 * - Hellfire red: RGB(200, 40, 10)
 * - Deep char: RGB(100, 20, 5)
 * - Ember glow: RGB(255, 150, 50)
 *
 * Materials: MAGMA_BLOCK, NETHERRACK, NETHER_BRICKS, RED_NETHER_BRICKS, BLACKSTONE,
 *            POLISHED_BLACKSTONE, CRYING_OBSIDIAN, SHROOMLIGHT, GLOWSTONE, BASALT,
 *            POLISHED_BASALT, DEEPSLATE, COAL_BLOCK, OBSIDIAN, RED_CONCRETE,
 *            BLACK_CONCRETE, ORANGE_CONCRETE
 *
 * Particles: LAVA, FLAME, SMOKE, LANDING_LAVA, FALLING_LAVA
 * Dust colors: Color(255,80,20), Color(200,40,10), Color(100,20,5), Color(255,150,50)
 * Sounds: BLOCK_LAVA_POP, ENTITY_BLAZE_SHOOT, ENTITY_GENERIC_EXPLODE,
 *         BLOCK_FIRE_AMBIENT, ENTITY_ENDER_DRAGON_GROWL, BLOCK_ANVIL_LAND, BLOCK_LAVA_EXTINGUISH
 */
public final class DoomBlockDisplay {
    private DoomBlockDisplay() {}

    private static final String MODE_PATH = "modes/doom/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        // Ground Eruptions (1-5)
        registry.register(new MagmaSpikeBurst(plugin));
        registry.register(new HellfirePyramid(plugin));
        registry.register(new LavaCauldronCluster(plugin));
        registry.register(new NetherPortalFrame(plugin));
        registry.register(new BrimstoneRing(plugin));
        // Falling/Impact Attacks (6-10)
        registry.register(new MagmaMeteor(plugin));
        registry.register(new ObsidianHammer(plugin));
        registry.register(new NetherBrickRain(plugin));
        registry.register(new InfernalAnvil(plugin));
        registry.register(new MoltenBoulder(plugin));
        // Structures (11-14)
        registry.register(new DemonObelisk(plugin));
        registry.register(new HellfireWall(plugin));
        registry.register(new InfernalCage(plugin));
        registry.register(new NetherGateway(plugin));
        // Spinning/Rotating (15-17)
        registry.register(new MagmaWhirlwind(plugin));
        registry.register(new InfernalBlade(plugin));
        registry.register(new HellfireSphere(plugin));
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
    // GROUND ERUPTION 1: MAGMA SPIKE BURST
    // 12 magma block spikes erupting from ground in a circle, each spike
    // 3 blocks tall tapered (wide base -> narrow tip). Slow Y rotation idle.
    // ================================================================
    public static class MagmaSpikeBurst extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> spikes = new ArrayList<>();
        private float rotationAngle = 0f;

        public MagmaSpikeBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_spike_burst", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(30.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(30);
            config.setDurationTicks(240);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double ringRadius = 3.5;
            for (int i = 0; i < 12; i++) {
                double angle = (2 * Math.PI * i) / 12;
                double bx = Math.cos(angle) * ringRadius;
                double bz = Math.sin(angle) * ringRadius;
                List<BlockDisplayHandle> spike = new ArrayList<>();

                // Base block — widest
                Location baseLoc = center.clone().add(bx, 0, bz);
                BlockDisplayHandle base = displayBuilder.spawnBlock(baseLoc, Material.MAGMA_BLOCK);
                base.scale(1.2f, 1.0f, 1.2f).glow(255, 80, 20).interpolation(5, 0);
                spawnedEntities.add(base.entity());
                spike.add(base);

                // Middle block — medium
                Location midLoc = center.clone().add(bx, 1.0, bz);
                BlockDisplayHandle mid = displayBuilder.spawnBlock(midLoc, Material.MAGMA_BLOCK);
                mid.scale(0.8f, 1.0f, 0.8f).glow(200, 40, 10).interpolation(5, 0);
                spawnedEntities.add(mid.entity());
                spike.add(mid);

                // Tip block — narrowest
                Location tipLoc = center.clone().add(bx, 2.0, bz);
                BlockDisplayHandle tip = displayBuilder.spawnBlock(tipLoc, Material.NETHERRACK);
                tip.scale(0.4f, 1.0f, 0.4f).glow(255, 150, 50).interpolation(5, 0);
                spawnedEntities.add(tip.entity());
                spike.add(tip);

                spikes.add(spike);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 1.0f, 0.6f);
            w.spawnParticle(Particle.LAVA, center.clone().add(0, 2, 0), 40, 3, 2, 3, 0.1);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            rotationAngle += 0.02f;

            // Rotate each spike slowly on Y axis using Transformation
            for (int i = 0; i < spikes.size(); i++) {
                for (BlockDisplayHandle block : spikes.get(i)) {
                    BlockDisplay entity = block.entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(5);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(rotationAngle, 0f, 1f, 0f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Ambient lava particles
            if (tick % 5 == 0) {
                c.getWorld().spawnParticle(Particle.LAVA, c.clone().add(0, 1.5, 0), 8, 3, 1, 3, 0.02);
                DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), 5, 3.5, 255, 80, 20, 1.5f);
            }

            // Ambient sound
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_POP, 0.6f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new MagmaSpikeBurst(plugin); }
    }

    // ================================================================
    // GROUND ERUPTION 2: HELLFIRE PYRAMID
    // 4-sided pyramid: 16 netherrack blocks stacked in layers (4x4 base,
    // 3x3, 2x2, 1x1 top). Slowly rotates on Y. Top block glows brighter.
    // ================================================================
    public static class HellfirePyramid extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> blocks = new ArrayList<>();
        private BlockDisplayHandle apex;
        private float rotationAngle = 0f;

        public HellfirePyramid(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellfire_pyramid", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(25.0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(260);
            config.setCooldownTicks(220);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Layer 0 (ground): 4x4 = offset positions covering a 2-block square
            // Using a 2x2 grid of blocks at each layer, shrinking upward
            double[][] layer0 = {{-0.75, 0, -0.75}, {0.75, 0, -0.75}, {-0.75, 0, 0.75}, {0.75, 0, 0.75},
                                  {-0.75, 0, 0}, {0.75, 0, 0}, {0, 0, -0.75}, {0, 0, 0.75}};
            // 8 blocks on base layer (ring of 8 forming a square perimeter)
            // But we need 16 total in 4 layers: 7 + 5 + 3 + 1 = 16
            // Layer 0: 7 blocks (square base perimeter)
            double[][] base = {{-1.0, 0, -1.0}, {0.0, 0, -1.0}, {1.0, 0, -1.0},
                               {-1.0, 0, 0.0},                   {1.0, 0, 0.0},
                               {-1.0, 0, 1.0},  {0.0, 0, 1.0}};
            for (double[] off : base) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.NETHERRACK);
                h.scale(1.0f, 1.0f, 1.0f).glow(200, 40, 10).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                blocks.add(h);
            }

            // Layer 1: 5 blocks
            double[][] mid1 = {{-0.5, 1.0, -0.5}, {0.5, 1.0, -0.5}, {0.0, 1.0, 0.0},
                               {-0.5, 1.0, 0.5}, {0.5, 1.0, 0.5}};
            for (double[] off : mid1) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.NETHERRACK);
                h.scale(0.9f, 1.0f, 0.9f).glow(255, 80, 20).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                blocks.add(h);
            }

            // Layer 2: 3 blocks
            double[][] mid2 = {{-0.25, 2.0, 0.0}, {0.25, 2.0, 0.0}, {0.0, 2.0, 0.25}};
            for (double[] off : mid2) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.RED_NETHER_BRICKS);
                h.scale(0.7f, 1.0f, 0.7f).glow(255, 80, 20).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                blocks.add(h);
            }

            // Layer 3 (apex): 1 block — glowing shroomlight
            Location apexLoc = center.clone().add(0, 3.0, 0);
            apex = displayBuilder.spawnBlock(apexLoc, Material.SHROOMLIGHT);
            apex.scale(0.5f, 0.8f, 0.5f).glow(255, 150, 50).interpolation(8, 0);
            spawnedEntities.add(apex.entity());
            blocks.add(apex);

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 0.8f, 0.7f);
            w.spawnParticle(Particle.FLAME, center.clone().add(0, 3, 0), 25, 1, 1, 1, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            rotationAngle += 0.03f;

            // Rotate all blocks on Y axis
            for (BlockDisplayHandle block : blocks) {
                BlockDisplay entity = block.entity();
                Transformation t = entity.getTransformation();
                entity.setInterpolationDuration(8);
                entity.setInterpolationDelay(0);
                entity.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(rotationAngle, 0f, 1f, 0f),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Apex glow pulse — scale oscillation
            if (apex != null && tick % 10 == 0) {
                float pulse = 0.5f + (float) Math.sin(tick * 0.1) * 0.15f;
                BlockDisplay apexEntity = apex.entity();
                Transformation t = apexEntity.getTransformation();
                apexEntity.setInterpolationDuration(10);
                apexEntity.setInterpolationDelay(0);
                apexEntity.setTransformation(new Transformation(
                        t.getTranslation(),
                        t.getLeftRotation(),
                        new Vector3f(pulse, 0.8f, pulse),
                        t.getRightRotation()
                ));
            }

            // Fire particles from top
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.FLAME, c.clone().add(0, 3.5, 0), 6, 0.3, 0.3, 0.3, 0.03);
                DisplayBuilder.dustParticles(c.clone().add(0, 1, 0), 3, 1.5, 200, 40, 10, 1.2f);
            }

            if (tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 0.8f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new HellfirePyramid(plugin); }
    }

    // ================================================================
    // GROUND ERUPTION 3: LAVA CAULDRON CLUSTER
    // 10 magma blocks in a cross pattern at ground level, each with a
    // shroomlight block on top. Center block larger. Radiates heat damage.
    // ================================================================
    public static class LavaCauldronCluster extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> baseBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> topBlocks = new ArrayList<>();
        private BlockDisplayHandle centerBase;
        private BlockDisplayHandle centerTop;

        public LavaCauldronCluster(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lava_cauldron_cluster", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(20.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(220);
            config.setCooldownTicks(180);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Cross pattern offsets: center + 4 cardinal arms (2 blocks each arm) = 9 + center = 10
            double[][] crossOffsets = {
                    {0, 0, 0},     // center
                    {1.5, 0, 0}, {3.0, 0, 0},    // east arm
                    {-1.5, 0, 0}, {-3.0, 0, 0},   // west arm
                    {0, 0, 1.5}, {0, 0, 3.0},     // south arm
                    {0, 0, -1.5}, {0, 0, -3.0},   // north arm
            };
            // Need 10 — add one diagonal block
            double[][] allOffsets = new double[10][];
            System.arraycopy(crossOffsets, 0, allOffsets, 0, 9);
            allOffsets[9] = new double[]{1.5, 0, 1.5}; // diagonal accent

            for (int i = 0; i < allOffsets.length; i++) {
                double[] off = allOffsets[i];
                boolean isCenter = (i == 0);

                // Base magma block
                Location baseLoc = center.clone().add(off[0], 0, off[2]);
                BlockDisplayHandle base = displayBuilder.spawnBlock(baseLoc, Material.MAGMA_BLOCK);
                float baseScale = isCenter ? 1.6f : 1.0f;
                base.scale(baseScale, 0.6f, baseScale).glow(255, 80, 20).interpolation(5, 0);
                spawnedEntities.add(base.entity());
                baseBlocks.add(base);
                if (isCenter) centerBase = base;

                // Top shroomlight
                Location topLoc = center.clone().add(off[0], 0.6, off[2]);
                BlockDisplayHandle top = displayBuilder.spawnBlock(topLoc, Material.SHROOMLIGHT);
                float topScale = isCenter ? 1.2f : 0.7f;
                top.scale(topScale, 0.5f, topScale).glow(255, 150, 50).interpolation(5, 0);
                spawnedEntities.add(top.entity());
                topBlocks.add(top);
                if (isCenter) centerTop = top;
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 1.0f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 1.0f, 0.6f);
            w.spawnParticle(Particle.LAVA, center.clone().add(0, 1, 0), 30, 3, 0.5, 3, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Pulsing glow on top blocks — scale oscillation
            if (tick % 8 == 0) {
                for (int i = 0; i < topBlocks.size(); i++) {
                    BlockDisplayHandle top = topBlocks.get(i);
                    boolean isCenter = (i == 0);
                    float baseScale = isCenter ? 1.2f : 0.7f;
                    float pulse = baseScale + (float) Math.sin(tick * 0.12 + i * 0.5) * 0.15f;
                    BlockDisplay entity = top.entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(8);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            t.getLeftRotation(),
                            new Vector3f(pulse, 0.5f, pulse),
                            t.getRightRotation()
                    ));
                }
            }

            // Heat shimmer particles
            if (tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(0, 1, 0), 4, 2.5, 0.3, 2.5, 0.01);
                c.getWorld().spawnParticle(Particle.FLAME, c.clone().add(0, 0.8, 0), 3, 2, 0.2, 2, 0.01);
            }

            if (tick % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_POP, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new LavaCauldronCluster(plugin); }
    }

    // ================================================================
    // GROUND ERUPTION 4: NETHER PORTAL FRAME
    // Rectangular portal shape: 12 obsidian blocks forming a 4-wide 5-tall
    // frame. Crying obsidian at corners. Interior filled with purple-glowing
    // block displays. Pulls players toward center with velocity.
    // ================================================================
    public static class NetherPortalFrame extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> frameBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> interiorBlocks = new ArrayList<>();

        public NetherPortalFrame(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nether_portal_frame", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(35.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(30);
            config.setDurationTicks(280);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Frame: 4 wide, 5 tall portal shape
            // Bottom row: 4 blocks across
            for (int x = -1; x <= 2; x++) {
                Location loc = center.clone().add(x * 1.0 - 0.5, 0, 0);
                Material mat = (x == -1 || x == 2) ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(1.0f, 1.0f, 1.0f).glow(100, 20, 5).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                frameBlocks.add(h);
            }
            // Left pillar (3 blocks up)
            for (int y = 1; y <= 3; y++) {
                Location loc = center.clone().add(-1.5, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(1.0f, 1.0f, 1.0f).glow(100, 20, 5).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                frameBlocks.add(h);
            }
            // Right pillar (3 blocks up)
            for (int y = 1; y <= 3; y++) {
                Location loc = center.clone().add(2.5, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(1.0f, 1.0f, 1.0f).glow(100, 20, 5).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                frameBlocks.add(h);
            }
            // Top row: 4 blocks across (with corner crying obsidian)
            for (int x = -1; x <= 2; x++) {
                Location loc = center.clone().add(x * 1.0 - 0.5, 4, 0);
                Material mat = (x == -1 || x == 2) ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(1.0f, 1.0f, 1.0f).glow(100, 20, 5).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                frameBlocks.add(h);
            }

            // Interior portal fill: 6 purple-glowing blocks (2 wide x 3 tall)
            for (int x = 0; x <= 1; x++) {
                for (int y = 1; y <= 3; y++) {
                    Location loc = center.clone().add(x * 1.0 - 0.0, y, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_STAINED_GLASS);
                    h.scale(1.0f, 1.0f, 0.2f).glow(150, 50, 200).interpolation(5, 0);
                    spawnedEntities.add(h.entity());
                    interiorBlocks.add(h);
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Pull players toward center
            if (tick % 5 == 0) {
                Location portalCenter = c.clone().add(0.5, 2, 0);
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    double dist = p.getLocation().distance(portalCenter);
                    if (dist < 8.0 && dist > 0.5) {
                        Vector dir = portalCenter.toVector().subtract(p.getLocation().toVector()).normalize();
                        p.setVelocity(p.getVelocity().add(dir.multiply(0.15)));
                    }
                }
            }

            // Interior shimmer — scale pulse on interior blocks
            if (tick % 6 == 0) {
                for (int i = 0; i < interiorBlocks.size(); i++) {
                    BlockDisplay entity = interiorBlocks.get(i).entity();
                    float zPulse = 0.2f + (float) Math.sin(tick * 0.15 + i * 0.8) * 0.1f;
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(6);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            t.getLeftRotation(),
                            new Vector3f(1.0f, 1.0f, zPulse),
                            t.getRightRotation()
                    ));
                }
            }

            // Particles
            if (tick % 3 == 0) {
                Location portalCenter = c.clone().add(0.5, 2, 0);
                DisplayBuilder.dustParticles(portalCenter, 6, 1.0, 150, 50, 200, 1.5f);
                c.getWorld().spawnParticle(Particle.SMOKE, portalCenter, 3, 0.5, 1.0, 0.2, 0.01);
            }

            if (tick % 45 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.4f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new NetherPortalFrame(plugin); }
    }

    // ================================================================
    // GROUND ERUPTION 5: BRIMSTONE RING
    // 14 basalt blocks arranged in a flat ring, each slightly tilted outward
    // at 22.5 degrees. Center has a glowing magma block. Ring slowly rotates on Y.
    // ================================================================
    public static class BrimstoneRing extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> ringBlocks = new ArrayList<>();
        private BlockDisplayHandle centerBlock;
        private float rotationAngle = 0f;

        public BrimstoneRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_ring", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(15.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(300);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double ringRadius = 4.0;
            for (int i = 0; i < 14; i++) {
                double angle = (2 * Math.PI * i) / 14;
                double bx = Math.cos(angle) * ringRadius;
                double bz = Math.sin(angle) * ringRadius;

                Location loc = center.clone().add(bx, 0.2, bz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BASALT);
                // Tilt outward by ~22.5 degrees (0.3927 radians) along the radial axis
                float tiltAngle = 0.3927f;
                float tiltAxisX = (float) Math.cos(angle);
                float tiltAxisZ = (float) Math.sin(angle);
                h.scale(0.8f, 0.5f, 0.8f).glow(100, 20, 5).interpolation(8, 0);
                h.rotate(tiltAngle, tiltAxisX, 0f, tiltAxisZ);
                spawnedEntities.add(h.entity());
                ringBlocks.add(h);
            }

            // Center glowing magma block
            centerBlock = displayBuilder.spawnBlock(center.clone().add(0, 0.1, 0), Material.MAGMA_BLOCK);
            centerBlock.scale(1.5f, 0.8f, 1.5f).glow(255, 150, 50).interpolation(8, 0);
            spawnedEntities.add(centerBlock.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 1.0f, 0.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.3f);
            DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), ringRadius, Particle.FLAME, 28, null);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            rotationAngle += 0.025f;

            // Rotate ring blocks around center by teleporting in rotated positions
            double ringRadius = 4.0;
            for (int i = 0; i < ringBlocks.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 14;
                double rotatedAngle = baseAngle + rotationAngle;
                double bx = Math.cos(rotatedAngle) * ringRadius;
                double bz = Math.sin(rotatedAngle) * ringRadius;

                Location newLoc = c.clone().add(bx, 0.2, bz);
                newLoc.setYaw(0);
                newLoc.setPitch(0);
                ringBlocks.get(i).entity().teleport(newLoc);
            }

            // Center pulse
            if (tick % 10 == 0) {
                float pulse = 1.5f + (float) Math.sin(tick * 0.08) * 0.3f;
                BlockDisplay entity = centerBlock.entity();
                Transformation t = entity.getTransformation();
                entity.setInterpolationDuration(10);
                entity.setInterpolationDelay(0);
                entity.setTransformation(new Transformation(
                        t.getTranslation(),
                        t.getLeftRotation(),
                        new Vector3f(pulse, 0.8f, pulse),
                        t.getRightRotation()
                ));
            }

            // Ring fire particles
            if (tick % 4 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), ringRadius, Particle.FLAME, 14, null);
                DisplayBuilder.dustParticles(c, 4, 1.0, 255, 80, 20, 1.0f);
            }

            if (tick % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 0.6f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneRing(plugin); }
    }

    // ================================================================
    // FALLING/IMPACT 6: MAGMA METEOR
    // Sphere of 12 magma blocks falling from Y+20 to ground. On impact:
    // 10 block radius, 50 impact damage, spawns debris particles.
    // ================================================================
    public static class MagmaMeteor extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> meteorBlocks = new ArrayList<>();
        private double currentY;
        private double groundY;
        private boolean impacted = false;

        public MagmaMeteor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_meteor", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(50.0);
            config.setImpactRadius(10.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setDamageDelayTicks(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            this.groundY = center.getY();
            this.currentY = center.getY() + 20;

            // Spawn 12 blocks in a sphere arrangement at elevated position
            Location spawnCenter = center.clone().add(0, 20, 0);
            double radius = 1.5;
            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < 12; i++) {
                double y = 1.0 - (2.0 * i / 11.0);
                double radiusAtY = Math.sqrt(1 - y * y);
                double theta = goldenAngle * i;
                double ox = Math.cos(theta) * radiusAtY * radius;
                double oy = y * radius;
                double oz = Math.sin(theta) * radiusAtY * radius;

                Location loc = spawnCenter.clone().add(ox, oy, oz);
                Material mat = (i % 3 == 0) ? Material.NETHERRACK : Material.MAGMA_BLOCK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float sc = 0.8f + (float) (Math.random() * 0.4);
                h.scale(sc, sc, sc).glow(255, 80, 20).interpolation(3, 0);
                // Random rotation for rough look
                h.rotate((float) (Math.random() * Math.PI), (float) Math.random(), (float) Math.random(), (float) Math.random());
                spawnedEntities.add(h.entity());
                meteorBlocks.add(h);
            }

            DisplayBuilder.playSound(spawnCenter, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (impacted) return;

            // Fall speed accelerates
            double fallSpeed = 0.3 + (tick * 0.02);
            currentY -= fallSpeed;

            if (currentY <= groundY) {
                // IMPACT
                currentY = groundY;
                impacted = true;

                Location impactLoc = c.clone();
                impactLoc.setY(groundY);

                // Impact effects
                c.getWorld().spawnParticle(Particle.LAVA, impactLoc, 80, 5, 2, 5, 0.3);
                c.getWorld().spawnParticle(Particle.FLAME, impactLoc, 60, 4, 3, 4, 0.2);
                c.getWorld().spawnParticle(Particle.SMOKE, impactLoc, 40, 3, 2, 3, 0.1);
                DisplayBuilder.dustParticles(impactLoc, 30, 5, 255, 80, 20, 2.5f);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.4f);
                DisplayBuilder.playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.3f);

                // Debris ring particles
                DisplayBuilder.particleRing(impactLoc, 5, Particle.LAVA, 40, null);
                DisplayBuilder.particleRing(impactLoc, 8, Particle.FLAME, 30, null);

                triggerImpactDamage(impactLoc);
                setCenter(impactLoc);
                return;
            }

            // Move all blocks down
            double dy = currentY - (groundY + 20);
            for (int i = 0; i < meteorBlocks.size(); i++) {
                double goldenAngle = Math.PI * (3 - Math.sqrt(5));
                double y = 1.0 - (2.0 * i / 11.0);
                double radiusAtY = Math.sqrt(1 - y * y);
                double theta = goldenAngle * i;
                double ox = Math.cos(theta) * radiusAtY * 1.5;
                double oy = y * 1.5;
                double oz = Math.sin(theta) * radiusAtY * 1.5;

                Location newLoc = c.clone().add(ox, currentY - groundY + oy, oz);
                newLoc.setYaw(0);
                newLoc.setPitch(0);
                meteorBlocks.get(i).entity().teleport(newLoc);
            }

            // Trailing fire particles
            if (tick % 2 == 0) {
                Location trailLoc = c.clone().add(0, currentY - groundY, 0);
                c.getWorld().spawnParticle(Particle.FLAME, trailLoc, 8, 1, 1, 1, 0.05);
                c.getWorld().spawnParticle(Particle.SMOKE, trailLoc.clone().add(0, 2, 0), 5, 0.5, 0.5, 0.5, 0.02);
            }

            // Falling sound
            if (tick % 10 == 0) {
                Location soundLoc = c.clone().add(0, currentY - groundY, 0);
                DisplayBuilder.playSound(soundLoc, Sound.ENTITY_BLAZE_SHOOT, 0.6f, 0.3f);
            }
        }

        @Override
        protected void onImpact(Location impactLocation) {
            // Impact effects already handled in onTick
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new MagmaMeteor(plugin); }
    }

    // ================================================================
    // FALLING/IMPACT 7: OBSIDIAN HAMMER
    // Hammer shape: 10 obsidian blocks for head (2x2x3), 4 basalt blocks for
    // handle. Falls from Y+15, rotates on Z during fall. Impact: 8-block radius,
    // 60 impact damage.
    // ================================================================
    public static class ObsidianHammer extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> headBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> handleBlocks = new ArrayList<>();
        private double currentY;
        private double groundY;
        private boolean impacted = false;
        private float fallRotation = 0f;

        public ObsidianHammer(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("obsidian_hammer", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(60.0);
            config.setImpactRadius(8.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(280);
            config.setDamageDelayTicks(5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            this.groundY = center.getY();
            this.currentY = center.getY() + 15;

            Location spawnCenter = center.clone().add(0, 15, 0);

            // Hammer head: 2 wide x 2 deep x 3 tall = 10 blocks (not all filled, shaped)
            // Front face: 2x3
            double[][] headOffsets = {
                    {-0.5, 0, -0.5}, {0.5, 0, -0.5},     // bottom row
                    {-0.5, 1.0, -0.5}, {0.5, 1.0, -0.5},  // middle row
                    {-0.5, 2.0, -0.5}, {0.5, 2.0, -0.5},  // top row
                    // Back face: 2x2
                    {-0.5, 0, 0.5}, {0.5, 0, 0.5},
                    {-0.5, 1.0, 0.5}, {0.5, 1.0, 0.5},
            };
            for (double[] off : headOffsets) {
                Location loc = spawnCenter.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(1.0f, 1.0f, 1.0f).glow(100, 20, 5).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                headBlocks.add(h);
            }

            // Handle: 4 basalt blocks extending downward from center of head
            for (int i = 1; i <= 4; i++) {
                Location loc = spawnCenter.clone().add(0, -i * 1.0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BASALT);
                h.scale(0.5f, 1.0f, 0.5f).glow(200, 40, 10).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                handleBlocks.add(h);
            }

            DisplayBuilder.playSound(spawnCenter, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (impacted) return;

            double fallSpeed = 0.4 + (tick * 0.025);
            currentY -= fallSpeed;
            fallRotation += 0.12f;

            if (currentY <= groundY) {
                currentY = groundY;
                impacted = true;

                Location impactLoc = c.clone();
                impactLoc.setY(groundY);

                c.getWorld().spawnParticle(Particle.LAVA, impactLoc, 50, 4, 1, 4, 0.15);
                c.getWorld().spawnParticle(Particle.SMOKE, impactLoc, 30, 3, 2, 3, 0.1);
                DisplayBuilder.playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.3f);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);

                DisplayBuilder.particleRing(impactLoc, 4, Particle.FLAME, 32, null);
                DisplayBuilder.particleRing(impactLoc, 7, Particle.SMOKE, 24, null);

                triggerImpactDamage(impactLoc);
                setCenter(impactLoc);
                return;
            }

            // Move and rotate hammer — apply Z rotation via Transformation
            double yOffset = currentY - groundY;
            double[][] headOffsets = {
                    {-0.5, 0, -0.5}, {0.5, 0, -0.5},
                    {-0.5, 1.0, -0.5}, {0.5, 1.0, -0.5},
                    {-0.5, 2.0, -0.5}, {0.5, 2.0, -0.5},
                    {-0.5, 0, 0.5}, {0.5, 0, 0.5},
                    {-0.5, 1.0, 0.5}, {0.5, 1.0, 0.5},
            };
            for (int i = 0; i < headBlocks.size(); i++) {
                Location newLoc = c.clone().add(headOffsets[i][0], yOffset + headOffsets[i][1], headOffsets[i][2]);
                newLoc.setYaw(0);
                newLoc.setPitch(0);
                headBlocks.get(i).entity().teleport(newLoc);

                // Apply rotation on Z
                BlockDisplay entity = headBlocks.get(i).entity();
                Transformation t = entity.getTransformation();
                entity.setInterpolationDuration(3);
                entity.setInterpolationDelay(0);
                entity.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(fallRotation, 0f, 0f, 1f),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            for (int i = 0; i < handleBlocks.size(); i++) {
                Location newLoc = c.clone().add(0, yOffset - (i + 1) * 1.0, 0);
                newLoc.setYaw(0);
                newLoc.setPitch(0);
                handleBlocks.get(i).entity().teleport(newLoc);

                BlockDisplay entity = handleBlocks.get(i).entity();
                Transformation t = entity.getTransformation();
                entity.setInterpolationDuration(3);
                entity.setInterpolationDelay(0);
                entity.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(fallRotation, 0f, 0f, 1f),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Trail particles
            if (tick % 2 == 0) {
                Location trailLoc = c.clone().add(0, yOffset + 1, 0);
                c.getWorld().spawnParticle(Particle.FLAME, trailLoc, 5, 0.8, 0.5, 0.8, 0.03);
            }
        }

        @Override
        protected void onImpact(Location impactLocation) {
            // Impact effects handled in onTick
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ObsidianHammer(plugin); }
    }

    // ================================================================
    // FALLING/IMPACT 8: NETHER BRICK RAIN
    // 15 individual nether brick blocks falling from different heights in a
    // 6x6 area. Each deals 20 impact damage in 3-block radius on landing.
    // Staggered 5 ticks apart.
    // ================================================================
    public static class NetherBrickRain extends BlockDisplayAttack {
        private static final int BLOCK_COUNT = 15;
        private final BlockDisplayHandle[] blocks = new BlockDisplayHandle[BLOCK_COUNT];
        private final double[] startX = new double[BLOCK_COUNT];
        private final double[] startZ = new double[BLOCK_COUNT];
        private final double[] startY = new double[BLOCK_COUNT];
        private final double[] groundYs = new double[BLOCK_COUNT];
        private final int[] spawnTick = new int[BLOCK_COUNT];
        private final boolean[] impacted = new boolean[BLOCK_COUNT];
        private double baseGroundY;

        public NetherBrickRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nether_brick_rain", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(20.0);
            config.setImpactRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            this.baseGroundY = center.getY();

            // Pre-calculate random positions for each falling block
            for (int i = 0; i < BLOCK_COUNT; i++) {
                startX[i] = (Math.random() - 0.5) * 6.0; // -3 to +3
                startZ[i] = (Math.random() - 0.5) * 6.0;
                startY[i] = 12 + Math.random() * 8;       // 12-20 blocks up
                groundYs[i] = baseGroundY;
                spawnTick[i] = i * 5;                      // 5 ticks apart
                impacted[i] = false;

                // Spawn block high up
                Location loc = center.clone().add(startX[i], startY[i], startZ[i]);
                Material mat;
                int r = i % 3;
                if (r == 0) mat = Material.NETHER_BRICKS;
                else if (r == 1) mat = Material.RED_NETHER_BRICKS;
                else mat = Material.NETHERRACK;

                blocks[i] = displayBuilder.spawnBlock(loc, mat);
                float sc = 0.7f + (float) (Math.random() * 0.5);
                blocks[i].scale(sc, sc, sc).glow(200, 40, 10).interpolation(2, 0);
                blocks[i].rotate((float) (Math.random() * Math.PI * 2), (float) Math.random(), (float) Math.random(), (float) Math.random());
                spawnedEntities.add(blocks[i].entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, 10, 0), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                if (impacted[i]) continue;
                if (tick < spawnTick[i]) continue; // Not yet spawned

                int localTick = tick - spawnTick[i];
                double fallSpeed = 0.5 + (localTick * 0.03);
                double currentBlockY = startY[i] - (localTick * fallSpeed * 0.5);

                if (currentBlockY <= 0) {
                    // Impact!
                    impacted[i] = true;
                    Location impactLoc = c.clone().add(startX[i], 0, startZ[i]);

                    c.getWorld().spawnParticle(Particle.LAVA, impactLoc, 15, 1.5, 0.5, 1.5, 0.05);
                    c.getWorld().spawnParticle(Particle.SMOKE, impactLoc, 10, 1, 0.5, 1, 0.03);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 0.8f, 0.6f + (float) (Math.random() * 0.4));

                    triggerImpactDamage(impactLoc);

                    // Teleport block to ground
                    Location groundLoc = c.clone().add(startX[i], 0, startZ[i]);
                    groundLoc.setYaw(0);
                    groundLoc.setPitch(0);
                    blocks[i].entity().teleport(groundLoc);
                } else {
                    // Move block down
                    Location newLoc = c.clone().add(startX[i], currentBlockY, startZ[i]);
                    newLoc.setYaw(0);
                    newLoc.setPitch(0);
                    blocks[i].entity().teleport(newLoc);

                    // Trail
                    if (localTick % 3 == 0) {
                        c.getWorld().spawnParticle(Particle.FLAME, newLoc, 2, 0.2, 0.2, 0.2, 0.01);
                    }
                }
            }
        }

        @Override
        protected void onImpact(Location impactLocation) {
            // Impact effects handled per-block in onTick
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new NetherBrickRain(plugin); }
    }

    // ================================================================
    // FALLING/IMPACT 9: INFERNAL ANVIL
    // Giant anvil shape from 14 blocks (wide top, narrow middle, flat base).
    // Drops from Y+25 slowly. Impact: 7-block radius, 55 damage.
    // Leaves crater ring of particles.
    // ================================================================
    public static class InfernalAnvil extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> anvilBlocks = new ArrayList<>();
        private double currentY;
        private double groundY;
        private boolean impacted = false;

        public InfernalAnvil(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_anvil", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(55.0);
            config.setImpactRadius(7.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(300);
            config.setDamageDelayTicks(5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            this.groundY = center.getY();
            this.currentY = center.getY() + 25;

            Location spawnCenter = center.clone().add(0, 25, 0);

            // Top plate: 5 blocks wide (3x2 area minus corners for shape)
            double[][] topPlate = {
                    {-1.5, 3.0, -0.5}, {-0.5, 3.0, -0.5}, {0.5, 3.0, -0.5}, {1.5, 3.0, -0.5},
                    {-1.0, 3.0, 0.5}, {0.0, 3.0, 0.5}, {1.0, 3.0, 0.5},
            };
            for (double[] off : topPlate) {
                Location loc = spawnCenter.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DEEPSLATE);
                h.scale(1.0f, 0.8f, 1.0f).glow(100, 20, 5).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                anvilBlocks.add(h);
            }

            // Narrow middle: 2 blocks (thin column)
            for (int y = 1; y <= 2; y++) {
                Location loc = spawnCenter.clone().add(0, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.6f, 1.0f, 0.6f).glow(200, 40, 10).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                anvilBlocks.add(h);
            }

            // Base plate: 5 blocks wide (flat base)
            double[][] basePlate = {
                    {-1.0, 0, -0.5}, {0.0, 0, -0.5}, {1.0, 0, -0.5},
                    {-0.5, 0, 0.5}, {0.5, 0, 0.5},
            };
            for (double[] off : basePlate) {
                Location loc = spawnCenter.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(1.0f, 0.6f, 1.0f).glow(100, 20, 5).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                anvilBlocks.add(h);
            }

            DisplayBuilder.playSound(spawnCenter, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 0.2f);
            w.spawnParticle(Particle.SMOKE, spawnCenter, 20, 2, 1, 2, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (impacted) return;

            // Slow fall then accelerate
            double fallSpeed = 0.15 + (tick * 0.015);
            currentY -= fallSpeed;

            if (currentY <= groundY) {
                currentY = groundY;
                impacted = true;

                Location impactLoc = c.clone();
                impactLoc.setY(groundY);

                // Massive impact effects
                c.getWorld().spawnParticle(Particle.LAVA, impactLoc, 60, 4, 1, 4, 0.2);
                c.getWorld().spawnParticle(Particle.SMOKE, impactLoc, 50, 3, 3, 3, 0.15);
                c.getWorld().spawnParticle(Particle.FLAME, impactLoc, 40, 5, 1, 5, 0.1);
                DisplayBuilder.playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.2f);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.3f);

                // Crater ring particles
                DisplayBuilder.particleRing(impactLoc, 3, Particle.LAVA, 30, null);
                DisplayBuilder.particleRing(impactLoc, 5, Particle.FLAME, 24, null);
                DisplayBuilder.particleRing(impactLoc, 7, Particle.SMOKE, 20, null);

                triggerImpactDamage(impactLoc);
                setCenter(impactLoc);
                return;
            }

            // Move all anvil blocks
            double yOffset = currentY - groundY;

            // Rebuild offset arrays in matching order
            double[][] topPlate = {
                    {-1.5, 3.0, -0.5}, {-0.5, 3.0, -0.5}, {0.5, 3.0, -0.5}, {1.5, 3.0, -0.5},
                    {-1.0, 3.0, 0.5}, {0.0, 3.0, 0.5}, {1.0, 3.0, 0.5},
            };
            double[][] midColumn = {{0, 1.0, 0}, {0, 2.0, 0}};
            double[][] basePlate = {
                    {-1.0, 0, -0.5}, {0.0, 0, -0.5}, {1.0, 0, -0.5},
                    {-0.5, 0, 0.5}, {0.5, 0, 0.5},
            };

            int idx = 0;
            for (double[] off : topPlate) {
                if (idx < anvilBlocks.size()) {
                    Location newLoc = c.clone().add(off[0], yOffset + off[1], off[2]);
                    newLoc.setYaw(0);
                    newLoc.setPitch(0);
                    anvilBlocks.get(idx).entity().teleport(newLoc);
                    idx++;
                }
            }
            for (double[] off : midColumn) {
                if (idx < anvilBlocks.size()) {
                    Location newLoc = c.clone().add(off[0], yOffset + off[1], off[2]);
                    newLoc.setYaw(0);
                    newLoc.setPitch(0);
                    anvilBlocks.get(idx).entity().teleport(newLoc);
                    idx++;
                }
            }
            for (double[] off : basePlate) {
                if (idx < anvilBlocks.size()) {
                    Location newLoc = c.clone().add(off[0], yOffset + off[1], off[2]);
                    newLoc.setYaw(0);
                    newLoc.setPitch(0);
                    anvilBlocks.get(idx).entity().teleport(newLoc);
                    idx++;
                }
            }

            // Shadow on ground growing larger as anvil approaches
            if (tick % 5 == 0) {
                double shadowRadius = 1.0 + (1.0 - (yOffset / 25.0)) * 3.0;
                DisplayBuilder.particleRing(c, shadowRadius, Particle.SMOKE, 16, null);
            }

            // Warning sound as it gets close
            if (yOffset < 10 && tick % 10 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, yOffset, 0), Sound.ENTITY_BLAZE_SHOOT, 0.6f, 0.3f);
            }
        }

        @Override
        protected void onImpact(Location impactLocation) {
            // Impact effects handled in onTick
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new InfernalAnvil(plugin); }
    }

    // ================================================================
    // FALLING/IMPACT 10: MOLTEN BOULDER
    // 10 randomly-rotated magma/netherrack blocks forming a rough sphere.
    // Rolls forward (translates on X/Z while rotating) toward nearest player.
    // Contact: 40 damage in 4-block radius. Tracking attack.
    // ================================================================
    public static class MoltenBoulder extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> boulderBlocks = new ArrayList<>();
        private final double[][] blockOffsets = new double[10][3];
        private double posX, posY, posZ;
        private float rollAngle = 0f;

        public MoltenBoulder(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("molten_boulder", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(40.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(250);
            config.setCooldownTicks(220);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            this.posX = center.getX();
            this.posY = center.getY() + 1.0;
            this.posZ = center.getZ();

            // Sphere of 10 blocks with random offsets
            double radius = 1.2;
            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < 10; i++) {
                double y = 1.0 - (2.0 * i / 9.0);
                double radiusAtY = Math.sqrt(1 - y * y);
                double theta = goldenAngle * i;
                blockOffsets[i][0] = Math.cos(theta) * radiusAtY * radius;
                blockOffsets[i][1] = y * radius;
                blockOffsets[i][2] = Math.sin(theta) * radiusAtY * radius;

                Location loc = center.clone().add(blockOffsets[i][0], 1.0 + blockOffsets[i][1], blockOffsets[i][2]);
                Material mat = (i % 2 == 0) ? Material.MAGMA_BLOCK : Material.NETHERRACK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float sc = 0.7f + (float) (Math.random() * 0.4);
                h.scale(sc, sc, sc).glow(255, 80, 20).interpolation(3, 0);
                h.rotate((float) (Math.random() * Math.PI * 2),
                        (float) Math.random(), (float) Math.random(), (float) Math.random());
                spawnedEntities.add(h.entity());
                boulderBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Track toward nearest player
            Player nearest = findNearestPlayer(new Location(c.getWorld(), posX, posY, posZ), 30);
            if (nearest != null) {
                double dx = nearest.getLocation().getX() - posX;
                double dz = nearest.getLocation().getZ() - posZ;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 0.5) {
                    double speed = 0.25;
                    posX += (dx / dist) * speed;
                    posZ += (dz / dist) * speed;
                }
            }

            rollAngle += 0.15f;

            // Update boulder center for damage calculations
            Location boulderCenter = new Location(c.getWorld(), posX, posY, posZ);
            boulderCenter.setYaw(0);
            boulderCenter.setPitch(0);
            setCenter(boulderCenter);

            // Move and rotate all blocks
            for (int i = 0; i < boulderBlocks.size(); i++) {
                // Rotate offset around Z axis to simulate rolling
                double ox = blockOffsets[i][0];
                double oy = blockOffsets[i][1];
                double oz = blockOffsets[i][2];

                // Apply roll rotation (around X axis for forward rolling)
                double cosR = Math.cos(rollAngle);
                double sinR = Math.sin(rollAngle);
                double rotY = oy * cosR - oz * sinR;
                double rotZ = oy * sinR + oz * cosR;

                Location newLoc = new Location(c.getWorld(), posX + ox, posY + rotY, posZ + rotZ);
                newLoc.setYaw(0);
                newLoc.setPitch(0);
                boulderBlocks.get(i).entity().teleport(newLoc);

                // Rotate block display itself
                BlockDisplay entity = boulderBlocks.get(i).entity();
                Transformation t = entity.getTransformation();
                entity.setInterpolationDuration(3);
                entity.setInterpolationDelay(0);
                entity.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(rollAngle, 1f, 0f, 0f),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Ground trail
            if (tick % 2 == 0) {
                Location trailLoc = new Location(c.getWorld(), posX, posY - 1, posZ);
                c.getWorld().spawnParticle(Particle.LAVA, trailLoc, 3, 0.5, 0.1, 0.5, 0.01);
                DisplayBuilder.dustParticles(trailLoc, 2, 0.5, 255, 80, 20, 1.0f);
            }

            // Rumbling sound
            if (tick % 15 == 0) {
                DisplayBuilder.playSound(boulderCenter, Sound.BLOCK_LAVA_POP, 0.7f, 0.3f);
            }

            if (tick % 30 == 0) {
                c.getWorld().spawnParticle(Particle.FLAME, boulderCenter, 8, 1, 1, 1, 0.03);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new MoltenBoulder(plugin); }
    }

    // ================================================================
    // STRUCTURE 11: DEMON OBELISK
    // Tall pillar: 15 blackstone blocks stacked with slight scale decrease
    // upward. Top 3 blocks are shroomlight (glowing). Pillar breathes
    // (scale pulses).
    // ================================================================
    public static class DemonObelisk extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> pillarBlocks = new ArrayList<>();
        private static final int PILLAR_HEIGHT = 15;

        public DemonObelisk(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("demon_obelisk", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(25.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(300);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < PILLAR_HEIGHT; i++) {
                Location loc = center.clone().add(0, i * 0.9, 0);
                boolean isGlowing = i >= (PILLAR_HEIGHT - 3);
                Material mat = isGlowing ? Material.SHROOMLIGHT : Material.BLACKSTONE;

                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                // Scale decreases upward: 1.5 at base, 0.6 at top
                float scaleXZ = 1.5f - (i * 0.065f);
                if (isGlowing) {
                    h.scale(scaleXZ, 0.9f, scaleXZ).glow(255, 150, 50).interpolation(10, 0);
                } else {
                    h.scale(scaleXZ, 0.9f, scaleXZ).glow(100, 20, 5).interpolation(10, 0);
                }
                spawnedEntities.add(h.entity());
                pillarBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.4f);
            w.spawnParticle(Particle.FLAME, center.clone().add(0, PILLAR_HEIGHT * 0.9, 0), 20, 0.5, 0.5, 0.5, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Breathing effect — scale oscillation on all blocks
            if (tick % 10 == 0) {
                float breathPhase = (float) Math.sin(tick * 0.05);

                for (int i = 0; i < pillarBlocks.size(); i++) {
                    BlockDisplay entity = pillarBlocks.get(i).entity();
                    float baseScale = 1.5f - (i * 0.065f);
                    float breathOffset = breathPhase * 0.1f * (1.0f + i * 0.03f); // More at top
                    float newScale = baseScale + breathOffset;

                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(10);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            t.getLeftRotation(),
                            new Vector3f(newScale, 0.9f, newScale),
                            t.getRightRotation()
                    ));
                }
            }

            // Top fire particles
            if (tick % 4 == 0) {
                Location topLoc = c.clone().add(0, PILLAR_HEIGHT * 0.9, 0);
                c.getWorld().spawnParticle(Particle.FLAME, topLoc, 5, 0.3, 0.5, 0.3, 0.03);
                DisplayBuilder.dustParticles(topLoc, 3, 0.5, 255, 150, 50, 1.5f);
            }

            // Base smoke
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(0, 0.5, 0), 3, 0.8, 0.2, 0.8, 0.01);
            }

            if (tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 0.6f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new DemonObelisk(plugin); }
    }

    // ================================================================
    // STRUCTURE 12: HELLFIRE WALL
    // Wall of 20 blocks: 5 wide x 4 tall of alternating magma and netherrack.
    // Blocks shift positions slightly in idle (wobble).
    // ================================================================
    public static class HellfireWall extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> wallBlocks = new ArrayList<>();
        private final double[][] wallOffsets = new double[20][3];

        public HellfireWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellfire_wall", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(20.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(260);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            int idx = 0;
            for (int x = -2; x <= 2; x++) {
                for (int y = 0; y < 4; y++) {
                    double ox = x * 1.3;
                    double oy = y * 1.3;
                    wallOffsets[idx] = new double[]{ox, oy, 0};

                    Location loc = center.clone().add(ox, oy, 0);
                    Material mat = ((x + y) % 2 == 0) ? Material.MAGMA_BLOCK : Material.NETHERRACK;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(1.3f, 1.3f, 0.6f).glow(255, 80, 20).interpolation(8, 0);
                    spawnedEntities.add(h.entity());
                    wallBlocks.add(h);
                    idx++;
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_EXTINGUISH, 1.2f, 0.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.5f);
            w.spawnParticle(Particle.FLAME, center.clone().add(0, 2.5, 0), 30, 3, 2, 0.5, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Wobble effect — each block shifts slightly with sine wave
            if (tick % 6 == 0) {
                for (int i = 0; i < wallBlocks.size(); i++) {
                    BlockDisplay entity = wallBlocks.get(i).entity();
                    double wobbleX = Math.sin(tick * 0.08 + i * 0.7) * 0.12;
                    double wobbleY = Math.cos(tick * 0.1 + i * 0.5) * 0.08;
                    double wobbleZ = Math.sin(tick * 0.06 + i * 0.9) * 0.06;

                    Location newLoc = c.clone().add(
                            wallOffsets[i][0] + wobbleX,
                            wallOffsets[i][1] + wobbleY,
                            wallOffsets[i][2] + wobbleZ
                    );
                    newLoc.setYaw(0);
                    newLoc.setPitch(0);
                    entity.teleport(newLoc);
                }
            }

            // Heat haze particles
            if (tick % 4 == 0) {
                Location wallCenter = c.clone().add(0, 2.5, 0);
                c.getWorld().spawnParticle(Particle.FLAME, wallCenter, 4, 3, 2, 0.3, 0.02);
                c.getWorld().spawnParticle(Particle.SMOKE, wallCenter.clone().add(0, 3, 0), 3, 2, 0.5, 0.3, 0.01);
            }

            if (tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 0.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new HellfireWall(plugin); }
    }

    // ================================================================
    // STRUCTURE 13: INFERNAL CAGE
    // 16 thin tall basalt blocks arranged in a 4x4 grid around a center point.
    // Center has glowing core. Cage slowly rotates. Traps nearest player.
    // ================================================================
    public static class InfernalCage extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> bars = new ArrayList<>();
        private BlockDisplayHandle core;
        private float cageRotation = 0f;

        public InfernalCage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_cage", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(30.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(240);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 16 bars in a 4x4 perimeter arrangement (bars form the edges of a square)
            double spacing = 1.5;
            // Top and bottom edges: 4 + 4 bars
            for (int i = 0; i < 4; i++) {
                double x = (i - 1.5) * spacing;
                // Front row
                Location frontLoc = center.clone().add(x, 0, -1.5 * spacing);
                BlockDisplayHandle front = displayBuilder.spawnBlock(frontLoc, Material.BASALT);
                front.scale(0.3f, 3.5f, 0.3f).glow(100, 20, 5).interpolation(8, 0);
                spawnedEntities.add(front.entity());
                bars.add(front);
                // Back row
                Location backLoc = center.clone().add(x, 0, 1.5 * spacing);
                BlockDisplayHandle back = displayBuilder.spawnBlock(backLoc, Material.BASALT);
                back.scale(0.3f, 3.5f, 0.3f).glow(100, 20, 5).interpolation(8, 0);
                spawnedEntities.add(back.entity());
                bars.add(back);
            }
            // Left and right edges (minus corners already placed): 4 + 4 bars
            for (int i = 0; i < 4; i++) {
                double z = (i - 1.5) * spacing;
                // Left column
                Location leftLoc = center.clone().add(-1.5 * spacing, 0, z);
                BlockDisplayHandle left = displayBuilder.spawnBlock(leftLoc, Material.BASALT);
                left.scale(0.3f, 3.5f, 0.3f).glow(100, 20, 5).interpolation(8, 0);
                spawnedEntities.add(left.entity());
                bars.add(left);
                // Right column
                Location rightLoc = center.clone().add(1.5 * spacing, 0, z);
                BlockDisplayHandle right = displayBuilder.spawnBlock(rightLoc, Material.BASALT);
                right.scale(0.3f, 3.5f, 0.3f).glow(100, 20, 5).interpolation(8, 0);
                spawnedEntities.add(right.entity());
                bars.add(right);
            }

            // Glowing core at center
            core = displayBuilder.spawnBlock(center.clone().add(0, 1.5, 0), Material.SHROOMLIGHT);
            core.scale(0.8f, 0.8f, 0.8f).glow(255, 150, 50).interpolation(8, 0);
            spawnedEntities.add(core.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            cageRotation += 0.03f;

            // Rotate all bars around center
            double spacing = 1.5;
            int barIdx = 0;

            // Recalculate rotated positions for all 16 bars
            // Front/back rows
            for (int i = 0; i < 4; i++) {
                double x = (i - 1.5) * spacing;
                // Front bar
                double rx1 = x * Math.cos(cageRotation) - (-1.5 * spacing) * Math.sin(cageRotation);
                double rz1 = x * Math.sin(cageRotation) + (-1.5 * spacing) * Math.cos(cageRotation);
                Location loc1 = c.clone().add(rx1, 0, rz1);
                loc1.setYaw(0);
                loc1.setPitch(0);
                if (barIdx < bars.size()) bars.get(barIdx).entity().teleport(loc1);
                barIdx++;
                // Back bar
                double rx2 = x * Math.cos(cageRotation) - (1.5 * spacing) * Math.sin(cageRotation);
                double rz2 = x * Math.sin(cageRotation) + (1.5 * spacing) * Math.cos(cageRotation);
                Location loc2 = c.clone().add(rx2, 0, rz2);
                loc2.setYaw(0);
                loc2.setPitch(0);
                if (barIdx < bars.size()) bars.get(barIdx).entity().teleport(loc2);
                barIdx++;
            }
            // Left/right columns
            for (int i = 0; i < 4; i++) {
                double z = (i - 1.5) * spacing;
                // Left bar
                double rx3 = (-1.5 * spacing) * Math.cos(cageRotation) - z * Math.sin(cageRotation);
                double rz3 = (-1.5 * spacing) * Math.sin(cageRotation) + z * Math.cos(cageRotation);
                Location loc3 = c.clone().add(rx3, 0, rz3);
                loc3.setYaw(0);
                loc3.setPitch(0);
                if (barIdx < bars.size()) bars.get(barIdx).entity().teleport(loc3);
                barIdx++;
                // Right bar
                double rx4 = (1.5 * spacing) * Math.cos(cageRotation) - z * Math.sin(cageRotation);
                double rz4 = (1.5 * spacing) * Math.sin(cageRotation) + z * Math.cos(cageRotation);
                Location loc4 = c.clone().add(rx4, 0, rz4);
                loc4.setYaw(0);
                loc4.setPitch(0);
                if (barIdx < bars.size()) bars.get(barIdx).entity().teleport(loc4);
                barIdx++;
            }

            // Core glow pulse
            if (tick % 8 == 0 && core != null) {
                float pulse = 0.8f + (float) Math.sin(tick * 0.12) * 0.3f;
                BlockDisplay entity = core.entity();
                Transformation t = entity.getTransformation();
                entity.setInterpolationDuration(8);
                entity.setInterpolationDelay(0);
                entity.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(cageRotation * 2, 0f, 1f, 0f),
                        new Vector3f(pulse, pulse, pulse),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Cage fire particles
            if (tick % 5 == 0) {
                c.getWorld().spawnParticle(Particle.FLAME, c.clone().add(0, 1.5, 0), 6, 1.5, 1.5, 1.5, 0.02);
                DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), 4, 2, 255, 80, 20, 1.0f);
            }

            if (tick % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_POP, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new InfernalCage(plugin); }
    }

    // ================================================================
    // STRUCTURE 14: NETHER GATEWAY
    // Arch: 12 polished blackstone blocks forming a doorway (2 pillars of 4
    // + 4 across top). Crying obsidian accents. Damage near gateway.
    // ================================================================
    public static class NetherGateway extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> archBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> accentBlocks = new ArrayList<>();

        public NetherGateway(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nether_gateway", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(25.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(280);
            config.setCooldownTicks(230);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Left pillar: 4 blocks up
            for (int y = 0; y < 4; y++) {
                Location loc = center.clone().add(-2.0, y * 1.2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(1.0f, 1.2f, 1.0f).glow(100, 20, 5).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                archBlocks.add(h);
            }

            // Right pillar: 4 blocks up
            for (int y = 0; y < 4; y++) {
                Location loc = center.clone().add(2.0, y * 1.2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(1.0f, 1.2f, 1.0f).glow(100, 20, 5).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                archBlocks.add(h);
            }

            // Top arch: 4 blocks across (connecting pillars at the top)
            for (int x = -1; x <= 2; x++) {
                Location loc = center.clone().add(x * 1.1 - 0.5, 4.8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(1.1f, 1.0f, 1.0f).glow(100, 20, 5).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                archBlocks.add(h);
            }

            // Crying obsidian accents at the tops of pillars (2) and center of arch (2)
            double[][] accentOffsets = {
                    {-2.0, 4.0, 0}, {2.0, 4.0, 0},   // pillar caps
                    {-0.5, 5.5, 0}, {0.5, 5.5, 0},    // arch crown
            };
            for (double[] off : accentOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.6f, 0.6f, 0.6f).glow(200, 40, 10).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                accentBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_EXTINGUISH, 0.6f, 0.6f);
            w.spawnParticle(Particle.SMOKE, center.clone().add(0, 3, 0), 20, 1.5, 2, 0.5, 0.03);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Crying obsidian accents pulse
            if (tick % 12 == 0) {
                for (int i = 0; i < accentBlocks.size(); i++) {
                    BlockDisplay entity = accentBlocks.get(i).entity();
                    float pulse = 0.6f + (float) Math.sin(tick * 0.1 + i * 1.5) * 0.2f;
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(12);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            t.getLeftRotation(),
                            new Vector3f(pulse, pulse, pulse),
                            t.getRightRotation()
                    ));
                }
            }

            // Gateway interior particles — vertical curtain of fire between pillars
            if (tick % 3 == 0) {
                for (int i = 0; i < 3; i++) {
                    double px = (Math.random() - 0.5) * 3.5;
                    double py = Math.random() * 4.5;
                    Location particleLoc = c.clone().add(px, py, 0);
                    c.getWorld().spawnParticle(Particle.FLAME, particleLoc, 2, 0.1, 0.1, 0.1, 0.01);
                }
                DisplayBuilder.dustParticles(c.clone().add(0, 2.5, 0), 4, 1.5, 200, 40, 10, 1.2f);
            }

            // Smoke from top
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(0, 5.5, 0), 5, 1.0, 0.3, 0.5, 0.02);
            }

            if (tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new NetherGateway(plugin); }
    }

    // ================================================================
    // SPINNING 15: MAGMA WHIRLWIND
    // 12 magma blocks arranged in a spiral at different heights, spinning
    // rapidly on Y axis. Each block at a different radius from center.
    // ================================================================
    public static class MagmaWhirlwind extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> spiralBlocks = new ArrayList<>();
        private final double[] baseAngles = new double[12];
        private final double[] radii = new double[12];
        private final double[] heights = new double[12];
        private float spinAngle = 0f;

        public MagmaWhirlwind(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_whirlwind", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(35.0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(260);
            config.setCooldownTicks(220);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spiral: each block at increasing height and angle, varying radius
            for (int i = 0; i < 12; i++) {
                baseAngles[i] = (2 * Math.PI * i) / 12;
                radii[i] = 1.5 + (i * 0.3); // Expanding spiral radius
                heights[i] = i * 0.6;         // Ascending height

                double bx = Math.cos(baseAngles[i]) * radii[i];
                double bz = Math.sin(baseAngles[i]) * radii[i];

                Location loc = center.clone().add(bx, heights[i], bz);
                Material mat = (i % 3 == 0) ? Material.SHROOMLIGHT : Material.MAGMA_BLOCK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float sc = 0.8f + (float) (i * 0.05);
                h.scale(sc, sc, sc).glow(255, 80, 20).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                spiralBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            spinAngle += 0.15f; // Rapid spin

            // Move blocks in spinning spiral
            for (int i = 0; i < spiralBlocks.size(); i++) {
                double angle = baseAngles[i] + spinAngle;
                double bx = Math.cos(angle) * radii[i];
                double bz = Math.sin(angle) * radii[i];
                // Add vertical bobbing
                double yBob = Math.sin(tick * 0.08 + i * 0.5) * 0.3;

                Location newLoc = c.clone().add(bx, heights[i] + yBob, bz);
                newLoc.setYaw(0);
                newLoc.setPitch(0);
                spiralBlocks.get(i).entity().teleport(newLoc);

                // Rotate each block on its Y axis
                BlockDisplay entity = spiralBlocks.get(i).entity();
                Transformation t = entity.getTransformation();
                entity.setInterpolationDuration(3);
                entity.setInterpolationDelay(0);
                entity.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(spinAngle * 2, 0f, 1f, 0f),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Sweeping fire trail
            if (tick % 2 == 0) {
                for (int i = 0; i < 3; i++) {
                    int idx = (int) (Math.random() * spiralBlocks.size());
                    Location blockLoc = spiralBlocks.get(idx).entity().getLocation();
                    c.getWorld().spawnParticle(Particle.FLAME, blockLoc, 3, 0.3, 0.2, 0.3, 0.02);
                }
                DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 5, 3, 255, 80, 20, 1.0f);
            }

            // Whooshing sound
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new MagmaWhirlwind(plugin); }
    }

    // ================================================================
    // SPINNING 16: INFERNAL BLADE
    // Giant sword shape: 10 blocks forming blade (long tapered from 1 to 3
    // wide), 4 blocks for hilt. Entire sword spins on Y at center point.
    // ================================================================
    public static class InfernalBlade extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> bladeBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> hiltBlocks = new ArrayList<>();
        private final double[][] bladeOffsets = new double[10][3];
        private final double[][] hiltOffsets = new double[4][3];
        private float bladeRotation = 0f;

        public InfernalBlade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_blade", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(240);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Blade: tapered from narrow tip to wide base
            // Tip (1 block narrow) → mid (2 blocks wide) → base (3 blocks wide)
            // Total 10 blade blocks extending along Z axis
            double[][] blade = {
                    // Tip (narrow, far end)
                    {0, 1.5, 4.5},
                    // Upper mid (1 block)
                    {0, 1.5, 3.5},
                    // Mid section (2 wide)
                    {-0.4, 1.5, 2.5}, {0.4, 1.5, 2.5},
                    // Lower mid (2 wide)
                    {-0.5, 1.5, 1.5}, {0.5, 1.5, 1.5},
                    // Base section (3 wide)
                    {-0.8, 1.5, 0.5}, {0.0, 1.5, 0.5}, {0.8, 1.5, 0.5},
                    // Guard (wide accent)
                    {0.0, 1.5, -0.2},
            };
            for (int i = 0; i < blade.length; i++) {
                bladeOffsets[i] = blade[i].clone();
                Location loc = center.clone().add(blade[i][0], blade[i][1], blade[i][2]);
                Material mat = (i < 2) ? Material.RED_NETHER_BRICKS : Material.DEEPSLATE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float scaleX = (i == 0) ? 0.4f : (i < 4) ? 0.6f : 0.8f;
                h.scale(scaleX, 0.3f, 1.0f).glow(200, 40, 10).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                bladeBlocks.add(h);
            }

            // Hilt: 4 blocks extending downward from blade base
            double[][] hilt = {
                    {0, 1.5, -1.0},   // cross guard left
                    {0, 1.5, -1.8},   // grip upper
                    {0, 1.5, -2.6},   // grip lower
                    {0, 1.5, -3.4},   // pommel
            };
            for (int i = 0; i < hilt.length; i++) {
                hiltOffsets[i] = hilt[i].clone();
                Location loc = center.clone().add(hilt[i][0], hilt[i][1], hilt[i][2]);
                Material mat = (i == 0 || i == 3) ? Material.BLACKSTONE : Material.COAL_BLOCK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float scaleX = (i == 0) ? 1.4f : (i == 3) ? 0.7f : 0.4f;
                h.scale(scaleX, 0.4f, 0.8f).glow(100, 20, 5).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                hiltBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);
            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.6f, 1.2f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            bladeRotation += 0.1f;

            // Spin entire sword on Y axis around center
            double cosR = Math.cos(bladeRotation);
            double sinR = Math.sin(bladeRotation);

            // Rotate blade blocks
            for (int i = 0; i < bladeBlocks.size(); i++) {
                double ox = bladeOffsets[i][0];
                double oz = bladeOffsets[i][2];
                double rx = ox * cosR - oz * sinR;
                double rz = ox * sinR + oz * cosR;

                Location newLoc = c.clone().add(rx, bladeOffsets[i][1], rz);
                newLoc.setYaw(0);
                newLoc.setPitch(0);
                bladeBlocks.get(i).entity().teleport(newLoc);

                // Apply rotation to block display
                BlockDisplay entity = bladeBlocks.get(i).entity();
                Transformation t = entity.getTransformation();
                entity.setInterpolationDuration(3);
                entity.setInterpolationDelay(0);
                entity.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(bladeRotation, 0f, 1f, 0f),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Rotate hilt blocks
            for (int i = 0; i < hiltBlocks.size(); i++) {
                double ox = hiltOffsets[i][0];
                double oz = hiltOffsets[i][2];
                double rx = ox * cosR - oz * sinR;
                double rz = ox * sinR + oz * cosR;

                Location newLoc = c.clone().add(rx, hiltOffsets[i][1], rz);
                newLoc.setYaw(0);
                newLoc.setPitch(0);
                hiltBlocks.get(i).entity().teleport(newLoc);

                BlockDisplay entity = hiltBlocks.get(i).entity();
                Transformation t = entity.getTransformation();
                entity.setInterpolationDuration(3);
                entity.setInterpolationDelay(0);
                entity.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(bladeRotation, 0f, 1f, 0f),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Blade trail particles — fire at tip
            if (tick % 2 == 0) {
                double tipAngle = bladeRotation;
                double tipX = Math.cos(-tipAngle) * 0 - Math.sin(-tipAngle) * (-4.5);
                double tipZ = Math.sin(-tipAngle) * 0 + Math.cos(-tipAngle) * (-4.5);
                // Use the same rotation as the blade to find tip
                double btx = 0 * cosR - 4.5 * sinR;
                double btz = 0 * sinR + 4.5 * cosR;
                Location tipLoc = c.clone().add(btx, 1.5, btz);
                c.getWorld().spawnParticle(Particle.FLAME, tipLoc, 4, 0.2, 0.1, 0.2, 0.02);
                DisplayBuilder.dustParticles(tipLoc, 2, 0.3, 255, 80, 20, 1.5f);
            }

            // Whoosh sound
            if (tick % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_BLAZE_SHOOT, 0.4f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new InfernalBlade(plugin); }
    }

    // ================================================================
    // SPINNING 17: HELLFIRE SPHERE
    // 14 blocks arranged in overlapping ring pattern approximating a sphere
    // (3 rings of 4-5 blocks on different axes). Each ring rotates on its
    // own axis.
    // ================================================================
    public static class HellfireSphere extends BlockDisplayAttack {
        // 3 rings: horizontal (5 blocks), vertical-XY (5 blocks), vertical-XZ (4 blocks) = 14
        private final List<BlockDisplayHandle> ringH = new ArrayList<>();   // horizontal ring (5)
        private final List<BlockDisplayHandle> ringXY = new ArrayList<>();  // XY plane ring (5)
        private final List<BlockDisplayHandle> ringXZ = new ArrayList<>();  // XZ plane ring (4)
        private float angleH = 0f, angleXY = 0f, angleXZ = 0f;
        private static final double RING_RADIUS = 2.5;

        public HellfireSphere(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellfire_sphere", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(20.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(300);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location sphereCenter = center.clone().add(0, 2.5, 0);

            // Ring 1: Horizontal ring (XZ plane) — 5 blocks
            for (int i = 0; i < 5; i++) {
                double angle = (2 * Math.PI * i) / 5;
                double bx = Math.cos(angle) * RING_RADIUS;
                double bz = Math.sin(angle) * RING_RADIUS;
                Location loc = sphereCenter.clone().add(bx, 0, bz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(0.9f, 0.9f, 0.9f).glow(255, 80, 20).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                ringH.add(h);
            }

            // Ring 2: Vertical ring (XY plane) — 5 blocks
            for (int i = 0; i < 5; i++) {
                double angle = (2 * Math.PI * i) / 5;
                double bx = Math.cos(angle) * RING_RADIUS;
                double by = Math.sin(angle) * RING_RADIUS;
                Location loc = sphereCenter.clone().add(bx, by, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERRACK);
                h.scale(0.9f, 0.9f, 0.9f).glow(200, 40, 10).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                ringXY.add(h);
            }

            // Ring 3: Vertical ring (YZ plane) — 4 blocks
            for (int i = 0; i < 4; i++) {
                double angle = (2 * Math.PI * i) / 4;
                double by = Math.cos(angle) * RING_RADIUS;
                double bz = Math.sin(angle) * RING_RADIUS;
                Location loc = sphereCenter.clone().add(0, by, bz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.RED_NETHER_BRICKS);
                h.scale(0.9f, 0.9f, 0.9f).glow(255, 150, 50).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                ringXZ.add(h);
            }

            DisplayBuilder.playSound(sphereCenter, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.3f);
            DisplayBuilder.playSound(sphereCenter, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 0.5f);
            w.spawnParticle(Particle.FLAME, sphereCenter, 30, 2, 2, 2, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            Location sphereCenter = c.clone().add(0, 2.5, 0);

            // Different rotation speeds for each ring
            angleH += 0.08f;
            angleXY += 0.1f;
            angleXZ += 0.12f;

            // Ring 1: Horizontal ring rotates on Y axis
            for (int i = 0; i < ringH.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 5;
                double angle = baseAngle + angleH;
                double bx = Math.cos(angle) * RING_RADIUS;
                double bz = Math.sin(angle) * RING_RADIUS;
                Location newLoc = sphereCenter.clone().add(bx, 0, bz);
                newLoc.setYaw(0);
                newLoc.setPitch(0);
                ringH.get(i).entity().teleport(newLoc);

                BlockDisplay entity = ringH.get(i).entity();
                Transformation t = entity.getTransformation();
                entity.setInterpolationDuration(3);
                entity.setInterpolationDelay(0);
                entity.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(angleH, 0f, 1f, 0f),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Ring 2: XY ring rotates on Z axis
            for (int i = 0; i < ringXY.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 5;
                double angle = baseAngle + angleXY;
                double bx = Math.cos(angle) * RING_RADIUS;
                double by = Math.sin(angle) * RING_RADIUS;
                Location newLoc = sphereCenter.clone().add(bx, by, 0);
                newLoc.setYaw(0);
                newLoc.setPitch(0);
                ringXY.get(i).entity().teleport(newLoc);

                BlockDisplay entity = ringXY.get(i).entity();
                Transformation t = entity.getTransformation();
                entity.setInterpolationDuration(3);
                entity.setInterpolationDelay(0);
                entity.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(angleXY, 0f, 0f, 1f),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Ring 3: YZ ring rotates on X axis
            for (int i = 0; i < ringXZ.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 4;
                double angle = baseAngle + angleXZ;
                double by = Math.cos(angle) * RING_RADIUS;
                double bz = Math.sin(angle) * RING_RADIUS;
                Location newLoc = sphereCenter.clone().add(0, by, bz);
                newLoc.setYaw(0);
                newLoc.setPitch(0);
                ringXZ.get(i).entity().teleport(newLoc);

                BlockDisplay entity = ringXZ.get(i).entity();
                Transformation t = entity.getTransformation();
                entity.setInterpolationDuration(3);
                entity.setInterpolationDelay(0);
                entity.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(angleXZ, 1f, 0f, 0f),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Center fire core particles
            if (tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.FLAME, sphereCenter, 8, 1.0, 1.0, 1.0, 0.03);
                DisplayBuilder.dustParticles(sphereCenter, 4, 2.0, 255, 80, 20, 1.5f);
            }

            // Outer ring particles
            if (tick % 6 == 0) {
                DisplayBuilder.particleRing(sphereCenter, RING_RADIUS, Particle.LAVA, 10, null);
            }

            if (tick % 25 == 0) {
                DisplayBuilder.playSound(sphereCenter, Sound.ENTITY_BLAZE_SHOOT, 0.4f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new HellfireSphere(plugin); }
    }
}
