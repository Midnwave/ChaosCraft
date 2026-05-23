package com.blockforge.chaoscraft.modes.chain.attacks;

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
 * Chain Mode — BLOCK DISPLAY ATTACKS 21-30 (Ground Eruptions &amp; Rising)
 *
 * Heavy industrial iron/chain ground-eruption attacks. Each starts with a
 * ground-crack telegraph (BLOCK particle + sound), then a structure erupts
 * up from below the ground (Y animation -2 → +3 over ~30t, staggered),
 * holds active with dangerous geometry (constant damage radius), and finally
 * sinks back down with a PISTON_CONTRACT cue.
 *
 * Palette: IRON_BLOCK, CHAIN, NETHERITE_BLOCK, ANVIL, POLISHED_BLACKSTONE,
 *          GILDED_BLACKSTONE, DARK_OAK_LOG, GRAY_CONCRETE
 *
 * Particles: BLOCK (dirt/stone/iron crack accent), LARGE_SMOKE, LAVA forge
 *            accent, CRIT, ELECTRIC_SPARK, SOUL_FIRE_FLAME, EXPLOSION_EMITTER
 * Sounds:   BLOCK_CHAIN_PLACE, BLOCK_GRINDSTONE_USE, BLOCK_NETHERITE_BLOCK_PLACE,
 *           BLOCK_ANVIL_PLACE, ENTITY_IRON_GOLEM_HURT, BLOCK_PISTON_EXTEND/CONTRACT
 *
 * Attacks:
 *  21. WreckingBallCraterArray — multi-spot bombardment, impact-only
 *  22. IronCageGrid            — closing cage grid
 *  23. ChainEruption           — vertical eruption column
 *  24. IronSpikeRing           — concentric spike ring
 *  25. ChainGeyser             — chain fountain pillar
 *  26. IronHand                — grasping reach
 *  27. ChainForest             — multi-pillar field
 *  28. BindingRing             — closing horizontal ring
 *  29. IronCrossEruption       — cross-pattern eruption
 *  30. ChainWellspring         — spreading wellspring
 */
public final class ChainBlockDisplay3 {
    private ChainBlockDisplay3() {}

    private static final String MODE_PATH = "modes/chain/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new WreckingBallCraterArray(plugin));
        registry.register(new IronCageGrid(plugin));
        registry.register(new ChainEruption(plugin));
        registry.register(new IronSpikeRing(plugin));
        registry.register(new ChainGeyser(plugin));
        registry.register(new IronHand(plugin));
        registry.register(new ChainForest(plugin));
        registry.register(new BindingRing(plugin));
        registry.register(new IronCrossEruption(plugin));
        registry.register(new ChainWellspring(plugin));
    }

    // ================================================================
    // Shared low-level helpers (mirrored at the top of each attack class
    // would be redundant — put them on the outer class so every nested
    // attack can call them).
    // ================================================================

    /** Set the scale of a display (over given duration). */
    private static void scaleTo(BlockDisplayHandle h, float sx, float sy, float sz, int duration) {
        BlockDisplay e = h.entity();
        Transformation t = e.getTransformation();
        e.setInterpolationDuration(duration);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                t.getTranslation(),
                new AxisAngle4f().set(t.getLeftRotation()),
                new Vector3f(sx, sy, sz),
                new AxisAngle4f().set(t.getRightRotation())
        ));
    }

    /** Uniform-scale convenience. */
    private static void scaleTo(BlockDisplayHandle h, float s, int duration) {
        scaleTo(h, s, s, s, duration);
    }

    /** Shrink to zero — used during dissipate. */
    private static void shrinkTo(BlockDisplayHandle h, int duration) {
        scaleTo(h, 0f, 0f, 0f, duration);
    }

    /** Translate to a given offset (relative to the -0.5/-0.5/-0.5 origin). */
    private static void translateTo(BlockDisplayHandle h, float tx, float ty, float tz, int duration) {
        BlockDisplay e = h.entity();
        Transformation t = e.getTransformation();
        e.setInterpolationDuration(duration);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                new Vector3f(tx - 0.5f, ty - 0.5f, tz - 0.5f),
                new AxisAngle4f().set(t.getLeftRotation()),
                t.getScale(),
                new AxisAngle4f().set(t.getRightRotation())
        ));
    }

    /** Rotate around a single axis (replaces current rotation). */
    private static void rotateAxis(BlockDisplayHandle h, float angle, float ax, float ay, float az, int duration) {
        BlockDisplay e = h.entity();
        Transformation t = e.getTransformation();
        e.setInterpolationDuration(duration);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                t.getTranslation(),
                new AxisAngle4f(angle, ax, ay, az),
                t.getScale(),
                new AxisAngle4f(0, 0, 1, 0)
        ));
    }

    /** Eruption telegraph: dust ring + cracks + grindstone whine. */
    private static void telegraph(Location ground, double radius) {
        World w = ground.getWorld();
        if (w == null) return;
        Particle.DustOptions warning = new Particle.DustOptions(Color.fromRGB(220, 60, 30), 1.4f);
        for (int i = 0; i < 32; i++) {
            double a = (2 * Math.PI * i) / 32.0;
            Location p = ground.clone().add(Math.cos(a) * radius, 0.1, Math.sin(a) * radius);
            w.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, warning);
        }
        w.spawnParticle(Particle.BLOCK, ground.clone().add(0, 0.1, 0), 24,
                radius * 0.5, 0.1, radius * 0.5, 0.05, Material.DIRT.createBlockData());
        w.spawnParticle(Particle.BLOCK, ground.clone().add(0, 0.1, 0), 16,
                radius * 0.5, 0.1, radius * 0.5, 0.05, Material.STONE.createBlockData());
        DisplayBuilder.playSound(ground, Sound.BLOCK_GRINDSTONE_USE, 1.4f, 0.5f);
    }

    /** Eruption burst: BLOCK iron crack + LARGE_SMOKE + EXPLOSION_EMITTER + sound. */
    private static void eruptBurst(Location pos, float volume) {
        World w = pos.getWorld();
        if (w == null) return;
        w.spawnParticle(Particle.BLOCK, pos, 30, 1.0, 0.3, 1.0, 0.1,
                Material.IRON_BLOCK.createBlockData());
        w.spawnParticle(Particle.LARGE_SMOKE, pos, 12, 0.8, 0.3, 0.8, 0.02);
        w.spawnParticle(Particle.LAVA, pos, 3, 0.2, 0.1, 0.2, 0);
        DisplayBuilder.playSound(pos, Sound.BLOCK_PISTON_EXTEND, volume, 0.6f);
        DisplayBuilder.playSound(pos, Sound.BLOCK_NETHERITE_BLOCK_PLACE, volume * 0.9f, 0.5f);
    }

    // ================================================================
    // #21 — WRECKING BALL CRATER ARRAY ("The Bombardment")
    // 8 craters drop sequentially in a square pattern. Each crater is
    // an IRON sphere + chain trail + ANVIL impact disc.
    // Impact-only damage.
    // 32 displays total (8 craters × 4 = 32).
    // ================================================================
    public static class WreckingBallCraterArray extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> balls = new ArrayList<>();
        private final List<BlockDisplayHandle> chains = new ArrayList<>();
        private final List<BlockDisplayHandle> discs = new ArrayList<>();
        private final List<BlockDisplayHandle> poles = new ArrayList<>();
        private final double[][] cratPos = new double[8][2];
        private final boolean[] impactDone = new boolean[8];

        public WreckingBallCraterArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("wrecking_ball_crater_array", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(240.0);
            config.setImpactRadius(2.0);
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setTicksBetweenDamage(20);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(140);
            config.setCooldownTicks(280);
            config.setChance(5.0);
            config.setDesignType("Multi-spot ground bombardment (find safe cell)");
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 8 crater positions arranged in a ring (radius 4.5, every 45°)
            for (int i = 0; i < 8; i++) {
                double a = (2 * Math.PI * i) / 8.0;
                cratPos[i][0] = Math.cos(a) * 4.5;
                cratPos[i][1] = Math.sin(a) * 4.5;
            }

            // Pre-spawn high-altitude wrecking balls + chain trails for each crater
            for (int i = 0; i < 8; i++) {
                double cx = cratPos[i][0];
                double cz = cratPos[i][1];
                Location skyLoc = center.clone().add(cx, 14.0, cz);

                // Ball (IRON_BLOCK sphere core)
                BlockDisplayHandle ball = displayBuilder.spawnBlock(skyLoc, Material.IRON_BLOCK);
                ball.scale(0.0f, 0.0f, 0.0f).glow(180, 180, 180).interpolation(8, 0);
                spawnedEntities.add(ball.entity());
                balls.add(ball);
                scaleTo(ball, 1.4f, 8);

                // Chain trail above ball (CHAIN)
                BlockDisplayHandle chain = displayBuilder.spawnBlock(skyLoc.clone().add(0, 1.2, 0), Material.CHAIN);
                chain.scale(0.0f, 0.0f, 0.0f).glow(160, 160, 160).interpolation(8, 0);
                spawnedEntities.add(chain.entity());
                chains.add(chain);
                scaleTo(chain, 0.45f, 2.6f, 0.45f, 8);

                // Telegraph disc (gray concrete on ground)
                BlockDisplayHandle disc = displayBuilder.spawnBlock(center.clone().add(cx, 0.05, cz), Material.GRAY_CONCRETE);
                disc.scale(0.0f, 0.0f, 0.0f).glow(220, 60, 30).interpolation(12, 0);
                spawnedEntities.add(disc.entity());
                discs.add(disc);
                scaleTo(disc, 2.0f, 0.04f, 2.0f, 12);

                // Telegraph pole (anvil column under ball)
                BlockDisplayHandle pole = displayBuilder.spawnBlock(skyLoc.clone().add(0, -0.7, 0), Material.ANVIL);
                pole.scale(0.0f, 0.0f, 0.0f).glow(80, 80, 80).interpolation(10, 4);
                spawnedEntities.add(pole.entity());
                poles.add(pole);
                scaleTo(pole, 0.5f, 0.6f, 0.5f, 10);

                telegraph(center.clone().add(cx, 0, cz), 2.2);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.6f, 0.4f);
            w.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, 0.2, 0), 40, 4, 0.3, 4, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Drop balls sequentially at ticks 30, 42, 54, 66, 78, 90, 102, 114
            for (int i = 0; i < 8; i++) {
                int dropTick = 30 + i * 12;
                if (tick == dropTick) {
                    BlockDisplayHandle ball = balls.get(i);
                    BlockDisplayHandle chain = chains.get(i);
                    BlockDisplayHandle pole = poles.get(i);
                    // Translate ball down to ground over 10 ticks (from y=14 → y=0)
                    translateTo(ball, 0f, -14f, 0f, 10);
                    translateTo(chain, 0f, -14f, 0f, 10);
                    translateTo(pole, 0f, -14f, 0f, 10);
                }
                int impactTick = 30 + i * 12 + 10;
                if (tick == impactTick && !impactDone[i]) {
                    impactDone[i] = true;
                    Location impactLoc = c.clone().add(cratPos[i][0], 0, cratPos[i][1]);
                    eruptBurst(impactLoc, 1.8f);
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, impactLoc, 1, 0, 0, 0, 0);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_ANVIL_PLACE, 1.8f, 0.4f);
                    DisplayBuilder.playSound(impactLoc, Sound.ENTITY_IRON_GOLEM_HURT, 1.5f, 0.5f);
                    triggerImpactDamage(impactLoc);
                    // Crater disc expands and craters down
                    BlockDisplayHandle disc = discs.get(i);
                    scaleTo(disc, 2.8f, 0.1f, 2.8f, 6);
                }
            }

            // Sparks at unfired wrecking balls during sky-hold
            if (tick % 4 == 0) {
                for (int i = 0; i < 8; i++) {
                    if (impactDone[i]) continue;
                    if (tick < 30 + i * 12 - 4) continue;
                    Location bp = balls.get(i).entity().getLocation();
                    w.spawnParticle(Particle.ELECTRIC_SPARK, bp, 2, 0.3, 0.3, 0.3, 0.05);
                }
            }

            // Dissipate last 15 ticks — discs sink, balls shrink
            if (tick == 125) {
                for (BlockDisplayHandle h : balls) shrinkTo(h, 14);
                for (BlockDisplayHandle h : chains) shrinkTo(h, 14);
                for (BlockDisplayHandle h : poles) shrinkTo(h, 14);
                for (BlockDisplayHandle h : discs) {
                    translateTo(h, 0f, -1.5f, 0f, 14);
                    shrinkTo(h, 14);
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_PISTON_CONTRACT, 1.4f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new WreckingBallCraterArray(plugin); }
    }

    // ================================================================
    // #22 — IRON CAGE GRID ("The Floor")
    // 4×4 grid of IRON_BLOCK floor tiles erupts up from under the ground.
    // Vertical CHAIN posts at every corner. NETHERITE_BLOCK lock pads
    // appear after delay (cage "locks"). Constant damage radius.
    // 36 displays total (16 tiles + 16 corner posts + 4 lock pads).
    // ================================================================
    public static class IronCageGrid extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> tiles = new ArrayList<>();
        private final List<BlockDisplayHandle> posts = new ArrayList<>();
        private final List<BlockDisplayHandle> locks = new ArrayList<>();
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();

        public IronCageGrid(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_cage_grid", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(280.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(6);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(240);
            config.setCooldownTicks(260);
            config.setChance(5.0);
            config.setDesignType("Closing cage grid (escape before lock)");
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            telegraph(center, 4.0);

            // 16 floor tiles (4x4, 1.6 spacing)
            for (int gx = 0; gx < 4; gx++) {
                for (int gz = 0; gz < 4; gz++) {
                    double px = (gx - 1.5) * 1.6;
                    double pz = (gz - 1.5) * 1.6;
                    Location loc = center.clone().add(px, -2.0, pz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                    h.scale(1.4f, 0.18f, 1.4f).glow(160, 160, 170).interpolation(0, 0);
                    spawnedEntities.add(h.entity());
                    tiles.add(h);
                    allBlocks.add(h);
                    int delay = (gx + gz);
                    h.entity().setInterpolationDuration(28);
                    h.entity().setInterpolationDelay(delay);
                    translateTo(h, 0f, 2.0f, 0f, 28);
                }
            }

            // 16 vertical CHAIN posts (one at each tile corner)
            for (int gx = 0; gx < 4; gx++) {
                for (int gz = 0; gz < 4; gz++) {
                    double px = (gx - 1.5) * 1.6;
                    double pz = (gz - 1.5) * 1.6;
                    Location loc = center.clone().add(px, -2.0, pz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                    h.scale(0.22f, 1.6f, 0.22f).glow(80, 80, 100).interpolation(0, 5 + (gx + gz));
                    spawnedEntities.add(h.entity());
                    posts.add(h);
                    allBlocks.add(h);
                    h.entity().setInterpolationDuration(30);
                    h.entity().setInterpolationDelay(15 + (gx + gz));
                    translateTo(h, 0f, 2.6f, 0f, 30);
                }
            }

            // 4 NETHERITE_BLOCK lock pads at the four cardinal sides (pre-staged underground)
            double[][] lockPos = {{0, 3.2}, {0, -3.2}, {3.2, 0}, {-3.2, 0}};
            for (double[] off : lockPos) {
                Location loc = center.clone().add(off[0], -1.5, off[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0.0f, 0.0f, 0.0f).glow(60, 50, 90).interpolation(0, 0);
                spawnedEntities.add(h.entity());
                locks.add(h);
                allBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_PISTON_EXTEND, 1.8f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_NETHERITE_BLOCK_PLACE, 1.6f, 0.5f);
            eruptBurst(center, 1.6f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // At tick 30 (after grid up), lock pads slam down
            if (tick == 30) {
                for (BlockDisplayHandle h : locks) {
                    scaleTo(h, 0.7f, 0.4f, 0.7f, 8);
                    translateTo(h, 0f, 1.6f, 0f, 8);
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 2.0f, 0.4f);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.6f, 0.4f);
                w.spawnParticle(Particle.EXPLOSION_EMITTER, c.clone().add(0, 0.5, 0), 1, 0, 0, 0, 0);
            }

            // Active phase: sparks on the grid corners
            if (tick > 30 && tick % 4 == 0) {
                for (int i = 0; i < posts.size(); i += 2) {
                    Location pl = posts.get(i).entity().getLocation();
                    w.spawnParticle(Particle.ELECTRIC_SPARK, pl, 2, 0.1, 0.6, 0.1, 0.04);
                }
            }
            if (tick > 30 && tick % 7 == 0) {
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, c.clone().add(0, 0.5, 0), 6, 1.8, 0.3, 1.8, 0.02);
                w.spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, 0.3, 0), 4, 2.0, 0.3, 2.0, 0.02);
            }

            // Active sound loop
            if (tick > 30 && tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.9f, 0.5f);
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 0.7f, 0.4f);
            }

            // Dissipate ticks 220-240
            if (tick == 220) {
                for (BlockDisplayHandle h : tiles) translateTo(h, 0f, -2.0f, 0f, 20);
                for (BlockDisplayHandle h : posts) translateTo(h, 0f, -2.6f, 0f, 20);
                for (BlockDisplayHandle h : locks) shrinkTo(h, 18);
                DisplayBuilder.playSound(c, Sound.BLOCK_PISTON_CONTRACT, 1.6f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IronCageGrid(plugin); }
    }

    // ================================================================
    // #23 — CHAIN ERUPTION ("The Rise")
    // A single tall vertical column erupts upward from one tile.
    // CHAIN links stacked + IRON_BLOCK cap + NETHERITE base + ANVIL tip.
    // 32 displays.
    // ================================================================
    public static class ChainEruption extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> column = new ArrayList<>();
        private final List<BlockDisplayHandle> caps = new ArrayList<>();
        private final List<BlockDisplayHandle> sleeves = new ArrayList<>();

        public ChainEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_eruption", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(320.0);
            config.setDamageRadius(2.8);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(25);
            config.setDurationTicks(220);
            config.setCooldownTicks(240);
            config.setChance(6.0);
            config.setDesignType("Vertical eruption (stay clear of erupting tile)");
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            telegraph(center, 2.8);

            // 24 stacked CHAIN links (column height ~6 blocks)
            for (int i = 0; i < 24; i++) {
                Location loc = center.clone().add(0, -2.0 + i * 0.25, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0.55f, 0.45f, 0.55f).glow(140, 140, 160).interpolation(0, 0);
                spawnedEntities.add(h.entity());
                column.add(h);
                int delay = i;
                h.entity().setInterpolationDuration(20);
                h.entity().setInterpolationDelay(20 + delay);
                translateTo(h, 0f, 3.0f, 0f, 20);
            }

            // 4 IRON_BLOCK caps (top crown)
            double[][] capPos = {{0, 5.6, 0}, {0.5, 5.4, 0}, {-0.5, 5.4, 0}, {0, 5.8, 0.4}};
            for (double[] off : capPos) {
                Location loc = center.clone().add(off[0], off[1] - 4, off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0.55f, 0.55f, 0.55f).glow(200, 200, 220).interpolation(20, 30);
                spawnedEntities.add(h.entity());
                caps.add(h);
                h.entity().setInterpolationDuration(20);
                h.entity().setInterpolationDelay(40);
                translateTo(h, 0f, 4f, 0f, 20);
            }

            // 3 NETHERITE_BLOCK sleeve sections + 1 ANVIL tip
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(0, -2.0 + i * 0.6, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0.85f, 0.5f, 0.85f).glow(40, 30, 60).interpolation(0, 0);
                spawnedEntities.add(h.entity());
                sleeves.add(h);
                h.entity().setInterpolationDuration(22);
                h.entity().setInterpolationDelay(10 + i * 2);
                translateTo(h, 0f, 1.6f, 0f, 22);
            }
            // 1 ANVIL tip cap
            BlockDisplayHandle anvilTip = displayBuilder.spawnBlock(center.clone().add(0, -1.5, 0), Material.ANVIL);
            anvilTip.scale(0.7f, 0.5f, 0.7f).glow(50, 50, 50).interpolation(0, 0);
            spawnedEntities.add(anvilTip.entity());
            caps.add(anvilTip);
            anvilTip.entity().setInterpolationDuration(20);
            anvilTip.entity().setInterpolationDelay(45);
            translateTo(anvilTip, 0f, 7.4f, 0f, 20);

            eruptBurst(center, 1.8f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 2.0f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Pulse the column (Y-scale wobble) once erected
            if (tick > 45 && tick % 6 == 0) {
                float pulse = 0.45f + 0.06f * (float) Math.sin(tick * 0.25);
                for (BlockDisplayHandle h : column) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(6);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(0.55f, pulse, 0.55f),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Constant active particles + sounds
            if (tick > 30 && tick % 4 == 0) {
                w.spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, 2.5, 0), 8, 0.5, 2.5, 0.5, 0.05);
                w.spawnParticle(Particle.LAVA, c.clone().add(0, 0.2, 0), 2, 0.6, 0.1, 0.6, 0);
            }
            if (tick > 30 && tick % 18 == 0) {
                w.spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, 0.5, 0), 10, 1.0, 0.5, 1.0, 0.02);
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_PLACE, 0.9f, 0.4f);
            }
            if (tick > 30 && tick % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.4f);
            }

            // Dissipate
            if (tick == 200) {
                for (BlockDisplayHandle h : column) translateTo(h, 0f, -3.0f, 0f, 20);
                for (BlockDisplayHandle h : caps) translateTo(h, 0f, -6.0f, 0f, 20);
                for (BlockDisplayHandle h : sleeves) translateTo(h, 0f, -2.0f, 0f, 20);
                DisplayBuilder.playSound(c, Sound.BLOCK_PISTON_CONTRACT, 1.4f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ChainEruption(plugin); }
    }

    // ================================================================
    // #24 — IRON SPIKE RING ("The Crown of Iron")
    // 12 iron spike assemblies arranged in a 5.0r ring, each tilted 25°
    // inward. Spikes erupt clockwise.
    // 36 displays (12 spikes × 3 segments).
    // ================================================================
    public static class IronSpikeRing extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> spikes = new ArrayList<>();
        private final List<float[]> spikeAngles = new ArrayList<>();

        public IronSpikeRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_spike_ring", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(240.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(6);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(240);
            config.setChance(6.0);
            config.setDesignType("Concentric ring (move to safe radius)");
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            telegraph(center, 5.0);

            float innerTilt = (float) Math.toRadians(25);
            for (int i = 0; i < 12; i++) {
                double angle = (2 * Math.PI * i) / 12.0;
                double sx = Math.cos(angle) * 5.0;
                double sz = Math.sin(angle) * 5.0;

                // Tilt direction: spike leans inward → axis is tangent to circle
                float axisX = -(float) Math.sin(angle);
                float axisZ = (float) Math.cos(angle);

                // 3 stacked IRON_BLOCK segments per spike (tapering)
                for (int s = 0; s < 3; s++) {
                    Location loc = center.clone().add(sx, -1.5 + s * 0.6, sz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                    float xy = 0.4f - s * 0.1f;
                    float yz = 0.65f - s * 0.12f;
                    h.scale(xy, yz, xy).glow(180, 180, 200).interpolation(0, 0);
                    rotateAxis(h, innerTilt, axisX, 0f, axisZ, 0);
                    spawnedEntities.add(h.entity());
                    spikes.add(h);
                    spikeAngles.add(new float[]{innerTilt, axisX, 0f, axisZ});
                    int delay = i + s * 12;
                    h.entity().setInterpolationDuration(14);
                    h.entity().setInterpolationDelay(delay);
                    translateTo(h, 0f, 1.6f + s * 0.2f, 0f, 14);
                }
            }

            eruptBurst(center, 1.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_PISTON_EXTEND, 1.8f, 0.6f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Sequential erupt sounds during emergence
            if (tick < 12 && tick % 1 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 0.7f, 0.5f + tick * 0.04f);
            }
            if (tick == 14) {
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 1.6f, 0.4f);
            }

            // Active: tip sparks
            if (tick > 25 && tick % 5 == 0) {
                for (int i = 0; i < 12; i++) {
                    double angle = (2 * Math.PI * i) / 12.0;
                    double sx = Math.cos(angle) * 5.0;
                    double sz = Math.sin(angle) * 5.0;
                    Location tip = c.clone().add(sx, 1.5, sz);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, tip, 2, 0.15, 0.15, 0.15, 0.05);
                    w.spawnParticle(Particle.CRIT, tip, 2, 0.15, 0.15, 0.15, 0.05);
                }
            }
            if (tick > 25 && tick % 20 == 0) {
                w.spawnParticle(Particle.LAVA, c.clone().add(0, 0.3, 0), 3, 2.5, 0.2, 2.5, 0);
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_PLACE, 0.8f, 0.5f);
            }

            // Pulse glow tilt: gently increase tilt to 35° at active peak
            if (tick == 60) {
                float strongTilt = (float) Math.toRadians(35);
                for (int i = 0; i < spikes.size(); i++) {
                    float[] a = spikeAngles.get(i);
                    BlockDisplay e = spikes.get(i).entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(20);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(strongTilt, a[1], a[2], a[3]),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 1.2f, 0.4f);
            }

            // Dissipate
            if (tick == 180) {
                for (BlockDisplayHandle h : spikes) translateTo(h, 0f, -2.5f, 0f, 18);
                DisplayBuilder.playSound(c, Sound.BLOCK_PISTON_CONTRACT, 1.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IronSpikeRing(plugin); }
    }

    // ================================================================
    // #25 — CHAIN GEYSER ("The Wellspring")
    // 6 arc streams of CHAIN links erupt outward in fountain pattern from
    // a central NETHERITE plinth. Endpoints carry IRON_BLOCK weights.
    // 36 displays (6×5 chains + 6 weights).
    // ================================================================
    public static class ChainGeyser extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> arcs = new ArrayList<>();
        private final List<BlockDisplayHandle> weights = new ArrayList<>();
        private final List<BlockDisplayHandle> plinth = new ArrayList<>();

        public ChainGeyser(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_geyser", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(380.0);
            config.setDamageRadius(2.4);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(240);
            config.setCooldownTicks(280);
            config.setChance(5.0);
            config.setDesignType("Vertical pillar (sidestep clear)");
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            telegraph(center, 2.5);

            // Central plinth: 4 stacked NETHERITE blocks
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(0, -2.0 + i * 0.25, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0.7f, 0.3f, 0.7f).glow(40, 35, 60).interpolation(0, 0);
                spawnedEntities.add(h.entity());
                plinth.add(h);
                h.entity().setInterpolationDuration(18);
                h.entity().setInterpolationDelay(i * 2);
                translateTo(h, 0f, 2.2f, 0f, 18);
            }

            // 6 arc streams × 5 links each
            for (int s = 0; s < 6; s++) {
                double base = (2 * Math.PI * s) / 6.0;
                for (int k = 0; k < 5; k++) {
                    double t = (k + 1) / 5.0;
                    double rad = t * 2.6;
                    double yOff = Math.sin(t * Math.PI) * 3.0;
                    Location loc = center.clone().add(Math.cos(base) * rad, -1.5 + yOff * 0.05, Math.sin(base) * rad);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                    h.scale(0.3f, 0.4f, 0.3f).glow(150, 150, 170).interpolation(0, 0);
                    spawnedEntities.add(h.entity());
                    arcs.add(h);
                    int delay = 22 + s * 5 + k;
                    h.entity().setInterpolationDuration(16);
                    h.entity().setInterpolationDelay(delay);
                    translateTo(h, 0f, 1.5f + (float) yOff, 0f, 16);
                }

                // Endpoint IRON weight (one per stream)
                Location endLoc = center.clone().add(Math.cos(base) * 2.8, -1.0, Math.sin(base) * 2.8);
                BlockDisplayHandle weight = displayBuilder.spawnBlock(endLoc, Material.IRON_BLOCK);
                weight.scale(0.35f, 0.35f, 0.35f).glow(180, 180, 190).interpolation(0, 0);
                spawnedEntities.add(weight.entity());
                weights.add(weight);
                weight.entity().setInterpolationDuration(18);
                weight.entity().setInterpolationDelay(45 + s * 3);
                translateTo(weight, 0f, 1.3f, 0f, 18);
            }

            // 2 GILDED_BLACKSTONE deco rim slabs (visual extra)
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add(0, -1.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.GILDED_BLACKSTONE);
                h.scale(1.0f, 0.1f, 1.0f).glow(220, 180, 60).interpolation(0, 0);
                spawnedEntities.add(h.entity());
                plinth.add(h);
                h.entity().setInterpolationDuration(20);
                h.entity().setInterpolationDelay(8 + i * 4);
                translateTo(h, 0f, 1.7f + i * 0.1f, 0f, 20);
            }

            eruptBurst(center, 2.0f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 2.0f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Arc undulation (Y wobble) after streams up
            if (tick > 70 && tick % 5 == 0) {
                for (int i = 0; i < arcs.size(); i++) {
                    float wob = (float) (Math.sin((tick + i * 5) * 0.2) * 0.15);
                    BlockDisplay e = arcs.get(i).entity();
                    Transformation t = e.getTransformation();
                    Vector3f tr = t.getTranslation();
                    e.setInterpolationDuration(5);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(tr.x, tr.y + wob, tr.z),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Constant active forge accents
            if (tick > 25 && tick % 4 == 0) {
                w.spawnParticle(Particle.LAVA, c.clone().add(0, 0.5, 0), 3, 0.5, 0.5, 0.5, 0);
                w.spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, 2.5, 0), 6, 1.5, 1.0, 1.5, 0.05);
            }
            if (tick > 25 && tick % 18 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_PLACE, 0.9f, 0.3f);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, c.clone().add(0, 0.4, 0), 8, 0.6, 0.3, 0.6, 0.02);
            }
            if (tick > 25 && tick % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.3f);
            }

            // Dissipate
            if (tick == 220) {
                for (BlockDisplayHandle h : arcs) translateTo(h, 0f, -3.0f, 0f, 18);
                for (BlockDisplayHandle h : weights) translateTo(h, 0f, -2.0f, 0f, 18);
                for (BlockDisplayHandle h : plinth) translateTo(h, 0f, -2.5f, 0f, 18);
                DisplayBuilder.playSound(c, Sound.BLOCK_PISTON_CONTRACT, 1.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ChainGeyser(plugin); }
    }

    // ================================================================
    // #26 — IRON HAND ("The Grasp")
    // Palm + 4 fingers + thumb + wrist rises from the ground; fingers
    // curl after spawn closing on the target.
    // 35 displays (3 palm + 4×3 fingers + 2 thumb + 1 wrist + 6 chain
    // bracers + 6 knuckle bolts = 30; with arm ring = 35).
    // ================================================================
    public static class IronHand extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> palm = new ArrayList<>();
        private final List<BlockDisplayHandle> fingers = new ArrayList<>();
        private final List<BlockDisplayHandle> thumb = new ArrayList<>();
        private final List<BlockDisplayHandle> wrist = new ArrayList<>();
        private final List<BlockDisplayHandle> bracers = new ArrayList<>();
        private final List<BlockDisplayHandle> bolts = new ArrayList<>();
        private boolean curled = false;

        public IronHand(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_hand", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(300.0);
            config.setDamageRadius(3.2);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(25);
            config.setDurationTicks(220);
            config.setCooldownTicks(260);
            config.setChance(6.0);
            config.setDesignType("Grasping reach (sprint perpendicular)");
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            telegraph(center, 3.2);

            // Wrist: 1 wider IRON_BLOCK at base
            BlockDisplayHandle wr = displayBuilder.spawnBlock(center.clone().add(0, -2.0, -0.3), Material.IRON_BLOCK);
            wr.scale(1.4f, 0.6f, 0.7f).glow(180, 180, 200).interpolation(0, 0);
            spawnedEntities.add(wr.entity());
            wrist.add(wr);
            wr.entity().setInterpolationDuration(15);
            wr.entity().setInterpolationDelay(0);
            translateTo(wr, 0f, 2.0f, 0f, 15);

            // Palm: 3 IRON_BLOCK cubes side by side
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(-0.6 + i * 0.6, -2.0, 0.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0.6f, 0.4f, 0.7f).glow(190, 190, 210).interpolation(0, 0);
                spawnedEntities.add(h.entity());
                palm.add(h);
                h.entity().setInterpolationDuration(15);
                h.entity().setInterpolationDelay(3);
                translateTo(h, 0f, 2.4f, 0f, 15);
            }

            // 4 fingers, each 3 IRON_BLOCK segments tapering, vertical
            for (int f = 0; f < 4; f++) {
                double fx = -0.7 + f * 0.45;
                for (int s = 0; s < 3; s++) {
                    Location loc = center.clone().add(fx, -2.0 + s * 0.45, 0.7 + s * 0.15);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                    float sc = 0.22f - s * 0.04f;
                    h.scale(sc, 0.45f - s * 0.05f, sc).glow(170, 170, 190).interpolation(0, 0);
                    spawnedEntities.add(h.entity());
                    fingers.add(h);
                    h.entity().setInterpolationDuration(16);
                    h.entity().setInterpolationDelay(6 + s * 2);
                    translateTo(h, 0f, 2.6f + s * 0.4f, 0f, 16);
                }
            }

            // Thumb: 2 IRON_BLOCK segments offset outward (left side)
            for (int s = 0; s < 2; s++) {
                Location loc = center.clone().add(-1.0 + s * 0.15, -2.0 + s * 0.35, 0.4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0.22f - s * 0.03f, 0.4f, 0.22f - s * 0.03f).glow(170, 170, 190).interpolation(0, 0);
                spawnedEntities.add(h.entity());
                thumb.add(h);
                h.entity().setInterpolationDuration(16);
                h.entity().setInterpolationDelay(10 + s * 2);
                translateTo(h, 0f, 2.5f, 0f, 16);
            }

            // 6 CHAIN bracers wrapped around the wrist (ring at Y+0.2)
            for (int i = 0; i < 6; i++) {
                double a = (2 * Math.PI * i) / 6.0;
                Location loc = center.clone().add(Math.cos(a) * 0.85, -1.5, Math.sin(a) * 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0.18f, 0.45f, 0.18f).glow(120, 120, 140).interpolation(0, 0);
                spawnedEntities.add(h.entity());
                bracers.add(h);
                h.entity().setInterpolationDuration(15);
                h.entity().setInterpolationDelay(4);
                translateTo(h, 0f, 2.0f, 0f, 15);
            }

            // 6 NETHERITE knuckle bolts on the fingers (Y high, one per finger × 1.5)
            for (int b = 0; b < 6; b++) {
                double bx = -0.85 + (b % 4) * 0.45;
                double by = 0.4 + (b / 4) * 0.45;
                Location loc = center.clone().add(bx, -2.0 + by, 0.95);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0.18f, 0.18f, 0.18f).glow(45, 35, 60).interpolation(0, 0);
                spawnedEntities.add(h.entity());
                bolts.add(h);
                h.entity().setInterpolationDuration(18);
                h.entity().setInterpolationDelay(12);
                translateTo(h, 0f, 2.6f + (float) by * 0.3f, 0f, 18);
            }

            eruptBurst(center, 1.8f);
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 1.6f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_NETHERITE_BLOCK_PLACE, 1.4f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Curl fingers at tick 50 (20t after hand fully out)
            if (tick == 50 && !curled) {
                curled = true;
                float curlAng = (float) Math.toRadians(40);
                for (BlockDisplayHandle h : fingers) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(20);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(curlAng, 1f, 0f, 0f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
                for (BlockDisplayHandle h : thumb) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(20);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(curlAng, 1f, 0f, 0.4f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 1.6f, 0.3f);
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 1.4f, 0.5f);
            }

            // Knuckle sparks on curl + active
            if (tick > 50 && tick % 5 == 0) {
                for (BlockDisplayHandle h : bolts) {
                    Location bp = h.entity().getLocation();
                    w.spawnParticle(Particle.ELECTRIC_SPARK, bp, 2, 0.1, 0.1, 0.1, 0.05);
                }
            }
            if (tick > 30 && tick % 8 == 0) {
                w.spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, 0.6, 0.2), 6, 1.2, 0.4, 1.2, 0.02);
                w.spawnParticle(Particle.LAVA, c.clone().add(0, 0.2, 0), 2, 0.6, 0.1, 0.6, 0);
            }
            if (tick > 30 && tick % 28 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.9f, 0.4f);
            }

            // Dissipate
            if (tick == 200) {
                for (BlockDisplayHandle h : palm) translateTo(h, 0f, -2.5f, 0f, 20);
                for (BlockDisplayHandle h : fingers) translateTo(h, 0f, -3.0f, 0f, 20);
                for (BlockDisplayHandle h : thumb) translateTo(h, 0f, -2.5f, 0f, 20);
                for (BlockDisplayHandle h : wrist) translateTo(h, 0f, -2.0f, 0f, 20);
                for (BlockDisplayHandle h : bracers) translateTo(h, 0f, -2.0f, 0f, 20);
                for (BlockDisplayHandle h : bolts) translateTo(h, 0f, -3.0f, 0f, 20);
                DisplayBuilder.playSound(c, Sound.BLOCK_PISTON_CONTRACT, 1.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IronHand(plugin); }
    }

    // ================================================================
    // #27 — CHAIN FOREST ("The Thicket")
    // 7 chain posts of varying heights erupt in a scattered field.
    // Each post = 5 CHAIN links stacked with slight Y-rotation per link.
    // 35 displays (7×5 = 35).
    // ================================================================
    public static class ChainForest extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> postLinks = new ArrayList<>();
        private final double[][] postPos = {
                {0.0, 0.0}, {3.0, -1.5}, {-2.5, 2.0}, {1.5, 3.0},
                {-3.5, -2.5}, {3.5, 2.5}, {-1.0, -3.5}
        };
        private final float[] postHeights = {3.0f, 4.0f, 2.5f, 3.5f, 4.5f, 3.0f, 2.8f};

        public ChainForest(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_forest", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(260.0);
            config.setDamageRadius(5.5);
            config.setTicksBetweenDamage(7);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(280);
            config.setCooldownTicks(300);
            config.setChance(5.0);
            config.setDesignType("Multi-pillar field (thread between pillars)");
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            telegraph(center, 5.5);

            for (int p = 0; p < 7; p++) {
                double px = postPos[p][0];
                double pz = postPos[p][1];
                float h = postHeights[p];
                int eruptDelay = p * 5; // sequential

                // 5 chain links per post
                for (int k = 0; k < 5; k++) {
                    Location loc = center.clone().add(px, -2.0 + k * 0.3, pz);
                    BlockDisplayHandle link = displayBuilder.spawnBlock(loc, Material.CHAIN);
                    float yScale = h / 5.0f;
                    link.scale(0.25f, yScale, 0.25f).glow(140, 140, 160).interpolation(0, 0);
                    spawnedEntities.add(link.entity());
                    postLinks.add(link);
                    int delay = eruptDelay + k;
                    link.entity().setInterpolationDuration(20);
                    link.entity().setInterpolationDelay(delay);
                    translateTo(link, 0f, 2.0f + k * (h * 0.2f), 0f, 20);
                    // Y-rotation per link gives spiral twist
                    float twist = (float) Math.toRadians(k * 18);
                    rotateAxis(link, twist, 0f, 1f, 0f, 0);
                }
            }

            eruptBurst(center, 1.8f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.8f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Sequential erupt bursts
            for (int p = 0; p < 7; p++) {
                int erupt = p * 5 + 20;
                if (tick == erupt) {
                    Location pl = c.clone().add(postPos[p][0], 0.2, postPos[p][1]);
                    w.spawnParticle(Particle.BLOCK, pl, 20, 0.6, 0.2, 0.6, 0.1, Material.IRON_BLOCK.createBlockData());
                    w.spawnParticle(Particle.LARGE_SMOKE, pl, 6, 0.4, 0.2, 0.4, 0.02);
                    DisplayBuilder.playSound(pl, Sound.BLOCK_NETHERITE_BLOCK_PLACE, 1.0f, 0.4f + p * 0.05f);
                }
            }

            // Forest sway: oscillate Y on every 5th link
            if (tick > 60 && tick % 6 == 0) {
                for (int i = 0; i < postLinks.size(); i++) {
                    if (i % 5 != 4) continue; // only top link
                    BlockDisplay e = postLinks.get(i).entity();
                    Transformation t = e.getTransformation();
                    float twist = (float) (Math.sin((tick + i * 8) * 0.1) * Math.toRadians(8));
                    e.setInterpolationDuration(6);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(twist, 1f, 0f, 0.3f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Active ambience
            if (tick > 35 && tick % 6 == 0) {
                for (int p = 0; p < 7; p++) {
                    if (Math.random() < 0.5) {
                        Location pl = c.clone().add(postPos[p][0], 1.5 + Math.random() * 2, postPos[p][1]);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, pl, 1, 0.15, 0.3, 0.15, 0.03);
                    }
                }
            }
            if (tick > 35 && tick % 24 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.7f, 0.4f);
            }
            if (tick > 35 && tick % 40 == 0) {
                w.spawnParticle(Particle.LAVA, c.clone().add(0, 0.3, 0), 4, 3.0, 0.2, 3.0, 0);
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 0.9f, 0.3f);
            }

            // Dissipate
            if (tick == 260) {
                for (BlockDisplayHandle h : postLinks) translateTo(h, 0f, -3.0f, 0f, 20);
                DisplayBuilder.playSound(c, Sound.BLOCK_PISTON_CONTRACT, 1.6f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ChainForest(plugin); }
    }

    // ================================================================
    // #28 — BINDING RING ("The Shackle")
    // 16 CHAIN ring blocks at ankle height, 8 IRON connectors, 1 NETHERITE
    // lock plate, 6 chain droplets, 1 anvil base = 32 displays.
    // ================================================================
    public static class BindingRing extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> ringLinks = new ArrayList<>();
        private final List<BlockDisplayHandle> connectors = new ArrayList<>();
        private final List<BlockDisplayHandle> droplets = new ArrayList<>();
        private BlockDisplayHandle lock;
        private BlockDisplayHandle anvilBase;

        public BindingRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("binding_ring", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(320.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(25);
            config.setDurationTicks(220);
            config.setCooldownTicks(260);
            config.setChance(6.0);
            config.setDesignType("Closing ring (escape inward or outward)");
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            telegraph(center, 4.0);

            // 16 CHAIN ring links at radius 2.0 (start radius 0.5 — will expand)
            for (int i = 0; i < 16; i++) {
                double a = (2 * Math.PI * i) / 16.0;
                Location loc = center.clone().add(Math.cos(a) * 0.5, -1.5, Math.sin(a) * 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0.5f, 0.3f, 0.5f).glow(160, 160, 180).interpolation(0, 0);
                rotateAxis(h, (float) a, 0f, 1f, 0f, 0);
                spawnedEntities.add(h.entity());
                ringLinks.add(h);
                h.entity().setInterpolationDuration(20);
                h.entity().setInterpolationDelay(i / 2);
                translateTo(h, (float) (Math.cos(a) * 1.5), 1.7f, (float) (Math.sin(a) * 1.5), 20);
            }

            // 8 IRON connectors between alternating links (radius 2.0)
            for (int i = 0; i < 8; i++) {
                double a = (2 * Math.PI * i) / 8.0 + Math.PI / 16.0;
                Location loc = center.clone().add(Math.cos(a) * 2.0, -1.0, Math.sin(a) * 2.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0.18f, 0.18f, 0.18f).glow(190, 190, 210).interpolation(0, 0);
                spawnedEntities.add(h.entity());
                connectors.add(h);
                h.entity().setInterpolationDuration(22);
                h.entity().setInterpolationDelay(10 + i);
                translateTo(h, 0f, 1.2f, 0f, 22);
            }

            // 1 NETHERITE lock plate (heavy) at front of ring
            Location lockLoc = center.clone().add(2.0, -1.0, 0);
            lock = displayBuilder.spawnBlock(lockLoc, Material.NETHERITE_BLOCK);
            lock.scale(0.0f, 0.0f, 0.0f).glow(60, 50, 80).interpolation(0, 0);
            spawnedEntities.add(lock.entity());
            lock.entity().setInterpolationDuration(24);
            lock.entity().setInterpolationDelay(20);
            scaleTo(lock, 0.6f, 0.5f, 0.4f, 24);
            translateTo(lock, 0f, 1.3f, 0f, 24);

            // 1 ANVIL anchor base under lock
            anvilBase = displayBuilder.spawnBlock(center.clone().add(0, -1.7, 0), Material.ANVIL);
            anvilBase.scale(1.2f, 0.3f, 1.2f).glow(60, 60, 60).interpolation(0, 0);
            spawnedEntities.add(anvilBase.entity());
            anvilBase.entity().setInterpolationDuration(18);
            anvilBase.entity().setInterpolationDelay(0);
            translateTo(anvilBase, 0f, 1.5f, 0f, 18);

            // 6 CHAIN droplets dangling at ring midpoints (decorative droplets)
            for (int i = 0; i < 6; i++) {
                double a = (2 * Math.PI * i) / 6.0;
                Location loc = center.clone().add(Math.cos(a) * 2.0, -1.0, Math.sin(a) * 2.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0.18f, 0.7f, 0.18f).glow(130, 130, 150).interpolation(0, 0);
                spawnedEntities.add(h.entity());
                droplets.add(h);
                h.entity().setInterpolationDuration(22);
                h.entity().setInterpolationDelay(28 + i);
                translateTo(h, 0f, 0.8f, 0f, 22);
            }

            eruptBurst(center, 1.6f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 2.0f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Spin ring on Y at ~3°/tick after up
            if (tick > 35 && tick % 3 == 0) {
                float spinAng = (float) Math.toRadians(tick);
                for (int i = 0; i < ringLinks.size(); i++) {
                    double base = (2 * Math.PI * i) / 16.0;
                    BlockDisplay e = ringLinks.get(i).entity();
                    Transformation t = e.getTransformation();
                    float drift = (float) (base + tick * 0.05);
                    e.setInterpolationDuration(3);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f((float) (Math.cos(drift) * 1.5) - 0.5f,
                                    t.getTranslation().y,
                                    (float) (Math.sin(drift) * 1.5) - 0.5f),
                            new AxisAngle4f(drift, 0f, 1f, 0f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Lock sparks
            if (tick > 35 && tick % 5 == 0 && lock != null) {
                Location lp = lock.entity().getLocation();
                w.spawnParticle(Particle.ELECTRIC_SPARK, lp, 3, 0.2, 0.2, 0.2, 0.05);
            }

            // Active accents
            if (tick > 35 && tick % 7 == 0) {
                w.spawnParticle(Particle.LAVA, c.clone().add(0, 0.2, 0), 3, 1.5, 0.1, 1.5, 0);
                w.spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, 0.3, 0), 5, 1.2, 0.2, 1.2, 0.02);
            }
            if (tick > 35 && tick % 22 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.7f, 0.5f);
                DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.8f, 0.4f);
            }
            if (tick > 35 && tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.4f);
            }

            // Dissipate
            if (tick == 200) {
                for (BlockDisplayHandle h : ringLinks) translateTo(h, 0f, -2.0f, 0f, 18);
                for (BlockDisplayHandle h : connectors) translateTo(h, 0f, -2.0f, 0f, 18);
                for (BlockDisplayHandle h : droplets) shrinkTo(h, 18);
                if (lock != null) shrinkTo(lock, 18);
                if (anvilBase != null) translateTo(anvilBase, 0f, -1.6f, 0f, 18);
                DisplayBuilder.playSound(c, Sound.BLOCK_PISTON_CONTRACT, 1.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new BindingRing(plugin); }
    }

    // ================================================================
    // #29 — IRON CROSS ERUPTION ("The Seal")
    // 4 arms × 4 IRON cubes each + 4 GRAY_CONCRETE trim per arm (×4=16)
    // + 1 NETHERITE center = 4*4 + 4 + 16 = ugh let's keep it 32:
    //   16 arm cubes + 8 trim strips (2 per arm) + 4 chain anchors + 4
    //   bolts = 32.
    // ================================================================
    public static class IronCrossEruption extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> armCubes = new ArrayList<>();
        private final List<BlockDisplayHandle> trims = new ArrayList<>();
        private final List<BlockDisplayHandle> anchors = new ArrayList<>();
        private final List<BlockDisplayHandle> bolts = new ArrayList<>();
        private BlockDisplayHandle centerBlock;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public IronCrossEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_cross_eruption", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(300.0);
            config.setDamageRadius(4.5);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(240);
            config.setCooldownTicks(280);
            config.setChance(5.0);
            config.setDesignType("Cross-pattern eruption (find diagonal safe)");
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            telegraph(center, 4.5);

            // Arm directions: N(0,1), S(0,-1), E(1,0), W(-1,0)
            int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
            for (int d = 0; d < 4; d++) {
                int dx = dirs[d][0];
                int dz = dirs[d][1];
                // 4 cubes along the arm
                for (int s = 0; s < 4; s++) {
                    double dist = 0.6 + s * 0.6;
                    Location loc = center.clone().add(dx * dist, -1.5, dz * dist);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                    h.scale(0.5f, 0.4f, 0.5f).glow(190, 190, 210).interpolation(0, 0);
                    spawnedEntities.add(h.entity());
                    armCubes.add(h);
                    all.add(h);
                    h.entity().setInterpolationDuration(14);
                    h.entity().setInterpolationDelay(s);
                    translateTo(h, 0f, 1.5f, 0f, 14);
                }
                // 2 GRAY_CONCRETE trim strips on each arm
                for (int s = 0; s < 2; s++) {
                    double dist = 0.9 + s * 1.2;
                    Location loc = center.clone().add(dx * dist, -1.5, dz * dist);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.GRAY_CONCRETE);
                    h.scale(0.45f, 0.06f, 0.45f).glow(100, 100, 120).interpolation(0, 0);
                    spawnedEntities.add(h.entity());
                    trims.add(h);
                    all.add(h);
                    h.entity().setInterpolationDuration(15);
                    h.entity().setInterpolationDelay(2 + s);
                    translateTo(h, 0f, 1.6f, 0f, 15);
                }
                // 1 CHAIN anchor at arm tip
                Location anchorLoc = center.clone().add(dx * 2.8, -1.5, dz * 2.8);
                BlockDisplayHandle anchor = displayBuilder.spawnBlock(anchorLoc, Material.CHAIN);
                anchor.scale(0.3f, 0.5f, 0.3f).glow(130, 130, 150).interpolation(0, 0);
                spawnedEntities.add(anchor.entity());
                anchors.add(anchor);
                all.add(anchor);
                anchor.entity().setInterpolationDuration(16);
                anchor.entity().setInterpolationDelay(6);
                translateTo(anchor, 0f, 2.0f, 0f, 16);

                // 1 NETHERITE bolt on each arm tip top
                Location boltLoc = center.clone().add(dx * 1.8, -1.5, dz * 1.8);
                BlockDisplayHandle bolt = displayBuilder.spawnBlock(boltLoc, Material.NETHERITE_BLOCK);
                bolt.scale(0.2f, 0.2f, 0.2f).glow(50, 35, 70).interpolation(0, 0);
                spawnedEntities.add(bolt.entity());
                bolts.add(bolt);
                all.add(bolt);
                bolt.entity().setInterpolationDuration(14);
                bolt.entity().setInterpolationDelay(8);
                translateTo(bolt, 0f, 1.6f, 0f, 14);
            }

            // Center: large NETHERITE block (seal)
            centerBlock = displayBuilder.spawnBlock(center.clone().add(0, -1.5, 0), Material.NETHERITE_BLOCK);
            centerBlock.scale(0.85f, 0.55f, 0.85f).glow(80, 60, 120).interpolation(0, 0);
            spawnedEntities.add(centerBlock.entity());
            all.add(centerBlock);
            centerBlock.entity().setInterpolationDuration(18);
            centerBlock.entity().setInterpolationDelay(0);
            translateTo(centerBlock, 0f, 1.4f, 0f, 18);

            eruptBurst(center, 2.0f);
            DisplayBuilder.playSound(center, Sound.BLOCK_PISTON_EXTEND, 2.0f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_PLACE, 1.6f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_NETHERITE_BLOCK_PLACE, 1.6f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Active pulse: center block scale wobble
            if (tick > 35 && tick % 6 == 0 && centerBlock != null) {
                float pulse = 0.85f + 0.06f * (float) Math.sin(tick * 0.2);
                scaleTo(centerBlock, pulse, 0.55f, pulse, 6);
            }

            // Particles along arms (cross-shaped sparks)
            if (tick > 35 && tick % 4 == 0) {
                int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
                for (int[] d : dirs) {
                    for (double dist = 0.6; dist <= 3.0; dist += 0.6) {
                        Location pl = c.clone().add(d[0] * dist, 0.3, d[1] * dist);
                        if (Math.random() < 0.4) {
                            w.spawnParticle(Particle.ELECTRIC_SPARK, pl, 1, 0.1, 0.2, 0.1, 0.04);
                        }
                    }
                }
            }
            if (tick > 35 && tick % 12 == 0) {
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, c.clone().add(0, 0.5, 0), 12, 1.5, 0.3, 1.5, 0.02);
                w.spawnParticle(Particle.LAVA, c.clone().add(0, 0.2, 0), 4, 1.5, 0.1, 1.5, 0);
            }
            if (tick > 35 && tick % 26 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.9f, 0.4f);
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 0.9f, 0.3f);
            }

            // Dissipate
            if (tick == 220) {
                for (BlockDisplayHandle h : all) translateTo(h, 0f, -2.0f, 0f, 18);
                DisplayBuilder.playSound(c, Sound.BLOCK_PISTON_CONTRACT, 1.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IronCrossEruption(plugin); }
    }

    // ================================================================
    // #30 — CHAIN WELLSPRING ("The Overflow")
    // Octagonal well structure rises; chains overflow over the rim.
    // 8 base + 8 walls + 8 rim + 8 chains + 2 crossbeam + 1 hang = 35.
    // Spawn fattens count to 36 by adding 1 gilded center cap.
    // ================================================================
    public static class ChainWellspring extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> base = new ArrayList<>();
        private final List<BlockDisplayHandle> walls = new ArrayList<>();
        private final List<BlockDisplayHandle> rim = new ArrayList<>();
        private final List<BlockDisplayHandle> overflow = new ArrayList<>();
        private final List<BlockDisplayHandle> crossbeam = new ArrayList<>();
        private BlockDisplayHandle hang;
        private BlockDisplayHandle cap;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public ChainWellspring(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_wellspring", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(360.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(25);
            config.setDurationTicks(260);
            config.setCooldownTicks(300);
            config.setChance(5.0);
            config.setDesignType("Spreading wellspring (outward escape)");
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            telegraph(center, 4.0);

            double rBase = 0.75;
            // 8 POLISHED_BLACKSTONE base ring
            for (int i = 0; i < 8; i++) {
                double a = (2 * Math.PI * i) / 8.0;
                Location loc = center.clone().add(Math.cos(a) * rBase, -2.0, Math.sin(a) * rBase);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.5f, 0.5f, 0.5f).glow(40, 30, 50).interpolation(0, 0);
                spawnedEntities.add(h.entity());
                base.add(h);
                all.add(h);
                h.entity().setInterpolationDuration(20);
                h.entity().setInterpolationDelay(i / 2);
                translateTo(h, 0f, 1.7f, 0f, 20);
            }

            // 8 POLISHED_BLACKSTONE_BRICKS walls on top
            for (int i = 0; i < 8; i++) {
                double a = (2 * Math.PI * i) / 8.0;
                Location loc = center.clone().add(Math.cos(a) * rBase, -1.4, Math.sin(a) * rBase);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE_BRICKS);
                h.scale(0.45f, 0.65f, 0.45f).glow(60, 50, 70).interpolation(0, 0);
                spawnedEntities.add(h.entity());
                walls.add(h);
                all.add(h);
                h.entity().setInterpolationDuration(22);
                h.entity().setInterpolationDelay(4 + i / 2);
                translateTo(h, 0f, 1.8f, 0f, 22);
            }

            // 8 GILDED_BLACKSTONE rim slabs (tangential)
            for (int i = 0; i < 8; i++) {
                double a = (2 * Math.PI * i) / 8.0;
                Location loc = center.clone().add(Math.cos(a) * rBase, -0.8, Math.sin(a) * rBase);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.GILDED_BLACKSTONE);
                h.scale(0.52f, 0.1f, 0.18f).glow(220, 180, 60).interpolation(0, 0);
                rotateAxis(h, (float) a, 0f, 1f, 0f, 0);
                spawnedEntities.add(h.entity());
                rim.add(h);
                all.add(h);
                h.entity().setInterpolationDuration(20);
                h.entity().setInterpolationDelay(10 + i);
                translateTo(h, 0f, 1.8f, 0f, 20);
            }

            // 8 CHAIN overflow draping over the rim outward
            for (int i = 0; i < 8; i++) {
                double a = (2 * Math.PI * i) / 8.0;
                Location loc = center.clone().add(Math.cos(a) * (rBase + 0.3), -0.6, Math.sin(a) * (rBase + 0.3));
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0.0f, 0.0f, 0.0f).glow(150, 150, 170).interpolation(0, 0);
                rotateAxis(h, (float) a, 0f, 1f, 0f, 0);
                spawnedEntities.add(h.entity());
                overflow.add(h);
                all.add(h);
                h.entity().setInterpolationDuration(8);
                h.entity().setInterpolationDelay(28 + i);
                scaleTo(h, 0.22f, 1.1f, 0.14f, 8);
                translateTo(h, 0f, 1.4f, 0f, 24);
            }

            // 2 DARK_OAK_LOG crossbeam pieces above the well
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add(0, -1.0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_OAK_LOG);
                h.scale(0.9f, 0.18f, 0.18f).glow(80, 50, 30).interpolation(0, 0);
                if (i == 1) rotateAxis(h, (float) Math.toRadians(90), 0f, 1f, 0f, 0);
                spawnedEntities.add(h.entity());
                crossbeam.add(h);
                all.add(h);
                h.entity().setInterpolationDuration(22);
                h.entity().setInterpolationDelay(18);
                translateTo(h, 0f, 2.6f, 0f, 22);
            }

            // 1 CHAIN hanging from beam into well center
            hang = displayBuilder.spawnBlock(center.clone().add(0, -1.0, 0), Material.CHAIN);
            hang.scale(0.15f, 1.4f, 0.15f).glow(140, 140, 160).interpolation(0, 0);
            spawnedEntities.add(hang.entity());
            all.add(hang);
            hang.entity().setInterpolationDuration(22);
            hang.entity().setInterpolationDelay(22);
            translateTo(hang, 0f, 2.2f, 0f, 22);

            // 1 GILDED_BLACKSTONE cap on top center (extra display)
            cap = displayBuilder.spawnBlock(center.clone().add(0, -0.6, 0), Material.GILDED_BLACKSTONE);
            cap.scale(0.4f, 0.18f, 0.4f).glow(220, 180, 60).interpolation(0, 0);
            spawnedEntities.add(cap.entity());
            all.add(cap);
            cap.entity().setInterpolationDuration(20);
            cap.entity().setInterpolationDelay(14);
            translateTo(cap, 0f, 1.7f, 0f, 20);

            eruptBurst(center, 1.8f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 2.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_NETHERITE_BLOCK_PLACE, 1.4f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Slow rim rotation (~0.5°/tick) once up
            if (tick > 50 && tick % 4 == 0) {
                for (int i = 0; i < rim.size(); i++) {
                    double a = (2 * Math.PI * i) / 8.0 + Math.toRadians(tick * 0.5);
                    BlockDisplay e = rim.get(i).entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(4);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f((float) a, 0f, 1f, 0f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Overflow chains sway (Y bob ±0.1)
            if (tick > 50 && tick % 5 == 0) {
                for (int i = 0; i < overflow.size(); i++) {
                    float bob = (float) (Math.sin((tick + i * 6) * 0.13) * 0.1);
                    BlockDisplay e = overflow.get(i).entity();
                    Transformation t = e.getTransformation();
                    Vector3f tr = t.getTranslation();
                    e.setInterpolationDuration(5);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(tr.x, tr.y + bob, tr.z),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Active accents
            if (tick > 35 && tick % 4 == 0) {
                w.spawnParticle(Particle.LAVA, c.clone().add(0, 1.5, 0), 3, 0.6, 0.3, 0.6, 0);
                w.spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, 1.5, 0), 4, 0.5, 0.3, 0.5, 0.04);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, c.clone().add(0, 1.4, 0), 4, 0.4, 0.2, 0.4, 0.02);
            }
            if (tick > 35 && tick % 12 == 0) {
                w.spawnParticle(Particle.BLOCK, c.clone().add(0, 0.5, 0), 8, 1.5, 0.2, 1.5, 0.04,
                        Material.IRON_BLOCK.createBlockData());
            }
            if (tick > 35 && tick % 24 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.8f, 0.4f);
            }
            if (tick > 35 && tick % 38 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.3f);
                DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.9f, 0.4f);
            }

            // Dissipate
            if (tick == 240) {
                for (BlockDisplayHandle h : base) translateTo(h, 0f, -1.7f, 0f, 18);
                for (BlockDisplayHandle h : walls) translateTo(h, 0f, -1.8f, 0f, 18);
                for (BlockDisplayHandle h : rim) translateTo(h, 0f, -1.8f, 0f, 18);
                for (BlockDisplayHandle h : overflow) shrinkTo(h, 18);
                for (BlockDisplayHandle h : crossbeam) translateTo(h, 0f, -2.6f, 0f, 18);
                if (hang != null) translateTo(hang, 0f, -2.2f, 0f, 18);
                if (cap != null) translateTo(cap, 0f, -1.7f, 0f, 18);
                DisplayBuilder.playSound(c, Sound.BLOCK_PISTON_CONTRACT, 1.6f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ChainWellspring(plugin); }
    }
}
