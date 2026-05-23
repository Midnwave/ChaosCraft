package com.blockforge.chaoscraft.modes.chain.attacks;

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
 * Chain Mode — BLOCK DISPLAY ATTACKS 1-10 (Pendulums / Swinging)
 *
 * Heavy-industrial iron/chain palette: NETHERITE_BLOCK, CHAIN, IRON_BLOCK,
 * GRAY_CONCRETE, POLISHED_BLACKSTONE, CHISELED_POLISHED_BLACKSTONE,
 * DARK_OAK_LOG, CRYING_OBSIDIAN, RED_CONCRETE. Pendulums, weights, blades,
 * hooks — distinct from Fluffy (paws/yarn), FreezingIce (creatures),
 * BlueMoon (chandeliers/cages), Devilsdream, Calamity.
 *
 * Particles: BLOCK(NETHERITE/IRON), CRIT, SMOKE, LARGE_SMOKE,
 *            ELECTRIC_SPARK, LAVA, SOUL_FIRE_FLAME, FALLING_DUST
 * Sounds: BLOCK_CHAIN_*, BLOCK_NETHERITE_BLOCK_*, BLOCK_ANVIL_*,
 *         ENTITY_IRON_GOLEM_*, BLOCK_GRINDSTONE_USE
 *
 * Attacks:
 *  1. WreckingBallPendulum  — 35 blocks, 180° swing arc, ball+chain+anchor
 *  2. ChainFlailThreeHeads  — 37 blocks, 3 spiked balls on shared ring, async
 *  3. PendulumSweepWall     — 30 blocks, wide netherite blade guillotine
 *  4. OverheadChainLariat   — 32 blocks, expanding horizontal lasso ring
 *  5. DoublePendulum        — 34 blocks, inner+outer chaotic arm, ghost trail
 *  6. ChainWhipCrack        — 30 blocks, telegraph→extend→snap whip, impact
 *  7. TriPendulumFan        — 36 blocks, 3 balls at 120°, drifting disk
 *  8. ChainSkipBouncer      — 30 blocks, bouncing iron ball + shockwaves
 *  9. InvertedPendulum      — 30 blocks, ground-anchored, ball arcs up
 * 10. WreckingBallOrbit     — 30 blocks, ball orbits target, fixed radius
 */
public final class ChainBlockDisplay {
    private ChainBlockDisplay() {}

    private static final String MODE_PATH = "modes/chain/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new WreckingBallPendulum(plugin));
        registry.register(new ChainFlailThreeHeads(plugin));
        registry.register(new PendulumSweepWall(plugin));
        registry.register(new OverheadChainLariat(plugin));
        registry.register(new DoublePendulum(plugin));
        registry.register(new ChainWhipCrack(plugin));
        registry.register(new TriPendulumFan(plugin));
        registry.register(new ChainSkipBouncer(plugin));
        registry.register(new InvertedPendulum(plugin));
        registry.register(new WreckingBallOrbit(plugin));
    }

    // ================================================================
    // Shared transformation helpers (kept inside this file so the 10
    // pendulum attacks can swing/bounce/rotate without duplicating math).
    // ================================================================

    /** Set translation on a BlockDisplay (preserves rotation/scale). */
    private static void setTranslation(BlockDisplayHandle h, float x, float y, float z, int dur) {
        BlockDisplay e = h.entity();
        Transformation t = e.getTransformation();
        e.setInterpolationDuration(dur);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                new Vector3f(x - 0.5f, y - 0.5f, z - 0.5f),
                new AxisAngle4f().set(t.getLeftRotation()),
                t.getScale(),
                new AxisAngle4f().set(t.getRightRotation())
        ));
    }

    /** Set rotation only (axis-angle). */
    private static void setRotation(BlockDisplayHandle h, float angle, float ax, float ay, float az, int dur) {
        BlockDisplay e = h.entity();
        Transformation t = e.getTransformation();
        e.setInterpolationDuration(dur);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                t.getTranslation(),
                new AxisAngle4f(angle, ax, ay, az),
                t.getScale(),
                new AxisAngle4f(0, 0, 1, 0)
        ));
    }

    /** Set both translation (block coords) and rotation (Y-axis). */
    private static void setPosRotY(BlockDisplayHandle h, double localX, double localY, double localZ, float yawRad, int dur) {
        BlockDisplay e = h.entity();
        Transformation t = e.getTransformation();
        e.setInterpolationDuration(dur);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                new Vector3f((float) localX - 0.5f, (float) localY - 0.5f, (float) localZ - 0.5f),
                new AxisAngle4f(yawRad, 0, 1, 0),
                t.getScale(),
                new AxisAngle4f(0, 0, 1, 0)
        ));
    }

    /** Grow a block from scale 0 to uniform target scale. */
    private static void growBlock(BlockDisplayHandle h, float targetScale, int duration) {
        growXYZ(h, targetScale, targetScale, targetScale, duration);
    }

    private static void growXYZ(BlockDisplayHandle h, float sx, float sy, float sz, int duration) {
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

    private static void shrinkToZero(BlockDisplayHandle h, int duration) {
        BlockDisplay e = h.entity();
        Transformation t = e.getTransformation();
        e.setInterpolationDuration(duration);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                t.getTranslation(),
                new AxisAngle4f().set(t.getLeftRotation()),
                new Vector3f(0f, 0f, 0f),
                new AxisAngle4f().set(t.getRightRotation())
        ));
    }

    /** Teleport entity to world location (used when displays must move farther than interpolation can handle). */
    private static void teleportEntity(BlockDisplayHandle h, Location loc) {
        try {
            h.entity().teleport(loc);
        } catch (Throwable ignored) {}
    }

    /** Find nearest non-exempt survival player within range (XZ). */
    private static Player findNearestPlayer(Location center, double range) {
        if (center == null || center.getWorld() == null) return null;
        Player nearest = null;
        double bestSq = range * range;
        for (Player p : center.getWorld().getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL) continue;
            double dx = p.getLocation().getX() - center.getX();
            double dz = p.getLocation().getZ() - center.getZ();
            double dsq = dx * dx + dz * dz;
            if (dsq < bestSq) { bestSq = dsq; nearest = p; }
        }
        return nearest;
    }

    // ================================================================
    // #1 — WRECKING BALL PENDULUM — "The Swing"
    // 35 blocks: 14 CHAIN cable + 1 NETHERITE core + 6 NETHERITE poles
    // + 8 IRON corners + 6 GRAY_CONCRETE weld bands + 1 anchor +
    // 4 IRON ring fittings (35 total).
    // ================================================================
    public static class WreckingBallPendulum extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> cableLinks = new ArrayList<>();
        private final List<BlockDisplayHandle> ballParts = new ArrayList<>();
        private final List<BlockDisplayHandle> weldBands = new ArrayList<>();
        private final List<BlockDisplayHandle> anchorParts = new ArrayList<>();
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private Location pivot;
        private float angleRad = 0f;
        private float angularVel = 0f;
        private float ballSpinY = 0f;
        private boolean lastDirNeg = false;

        public WreckingBallPendulum(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("wrecking_ball_pendulum", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(280.0);
            config.setDamageRadius(2.5);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(15);
            config.setDurationTicks(200);
            config.setCooldownTicks(200);
            config.setChance(9);
            config.setEnabled(true);
            config.setDesignType("Pendulum sweep (timing dodge)");
        }

        @Override
        protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            World w = c.getWorld();
            if (w == null) return;
            pivot = c.clone().add(0, 9.0, 0);
            angleRad = (float) Math.toRadians(-90); // start at one side
            angularVel = 0f;

            // Anchor: 1 CHISELED_POLISHED_BLACKSTONE pivot block.
            BlockDisplayHandle anchor = displayBuilder.spawnBlock(pivot.clone(), Material.CHISELED_POLISHED_BLACKSTONE);
            anchor.scale(0f, 0f, 0f).glow(60, 60, 60).interpolation(10, 0);
            spawnedEntities.add(anchor.entity()); anchorParts.add(anchor); allBlocks.add(anchor);
            growBlock(anchor, 0.6f, 10);

            // 4 IRON_BLOCK ring fittings around pivot.
            double[][] ringOffsets = {{-0.35, 0, 0}, {0.35, 0, 0}, {0, 0, -0.35}, {0, 0, 0.35}};
            for (double[] o : ringOffsets) {
                BlockDisplayHandle r = displayBuilder.spawnBlock(pivot.clone().add(o[0], o[1], o[2]), Material.IRON_BLOCK);
                r.scale(0f, 0f, 0f).glow(180, 180, 200).interpolation(12, 4);
                spawnedEntities.add(r.entity()); anchorParts.add(r); allBlocks.add(r);
                growXYZ(r, 0.12f, 0.12f, 0.12f, 12);
            }

            // 14 CHAIN cable links (top-to-bottom, materialise progressively).
            for (int i = 0; i < 14; i++) {
                double t = (i + 1) / 14.0;
                Location lloc = pivot.clone().add(0, -t * 6.5, 0); // initial vertical
                BlockDisplayHandle link = displayBuilder.spawnBlock(lloc, Material.CHAIN);
                link.scale(0f, 0f, 0f).glow(80, 80, 90).interpolation(8, 2 * i);
                spawnedEntities.add(link.entity()); cableLinks.add(link); allBlocks.add(link);
                growXYZ(link, 0.18f, 0.5f, 0.18f, 8);
            }

            // Ball core: 1 NETHERITE_BLOCK (15-disp sphere: 1 core + 6 poles + 8 corners).
            Location ballSeed = pivot.clone().add(0, -7.0, 0);
            BlockDisplayHandle core = displayBuilder.spawnBlock(ballSeed.clone(), Material.NETHERITE_BLOCK);
            core.scale(0f, 0f, 0f).glow(40, 40, 40).interpolation(15, 28);
            spawnedEntities.add(core.entity()); ballParts.add(core); allBlocks.add(core);
            growBlock(core, 1.1f, 15);

            double[][] poles = {{0.85, 0, 0}, {-0.85, 0, 0}, {0, 0.85, 0}, {0, -0.85, 0}, {0, 0, 0.85}, {0, 0, -0.85}};
            for (double[] o : poles) {
                BlockDisplayHandle p = displayBuilder.spawnBlock(ballSeed.clone().add(o[0], o[1], o[2]), Material.NETHERITE_BLOCK);
                p.scale(0f, 0f, 0f).glow(40, 40, 40).interpolation(14, 30);
                spawnedEntities.add(p.entity()); ballParts.add(p); allBlocks.add(p);
                growBlock(p, 0.52f, 14);
            }
            double d = 0.7;
            double[][] corners = {{d, d, d}, {d, d, -d}, {d, -d, d}, {d, -d, -d},
                    {-d, d, d}, {-d, d, -d}, {-d, -d, d}, {-d, -d, -d}};
            for (double[] o : corners) {
                BlockDisplayHandle p = displayBuilder.spawnBlock(ballSeed.clone().add(o[0], o[1], o[2]), Material.IRON_BLOCK);
                p.scale(0f, 0f, 0f).glow(160, 160, 180).interpolation(14, 32);
                spawnedEntities.add(p.entity()); ballParts.add(p); allBlocks.add(p);
                growBlock(p, 0.36f, 14);
            }

            // 6 GRAY_CONCRETE weld bands around the ball equator/meridians.
            double[][] bands = {
                    {0, 0, 0.9}, {0, 0, -0.9},
                    {0.9, 0, 0}, {-0.9, 0, 0},
                    {0, 0.9, 0}, {0, -0.9, 0}
            };
            for (double[] o : bands) {
                BlockDisplayHandle b = displayBuilder.spawnBlock(ballSeed.clone().add(o[0], o[1], o[2]), Material.GRAY_CONCRETE);
                b.scale(0f, 0f, 0f).glow(110, 110, 110).interpolation(14, 34);
                spawnedEntities.add(b.entity()); weldBands.add(b); allBlocks.add(b);
                growXYZ(b, 0.08f, 0.05f, 1.0f, 14);
            }

            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.4f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_PLACE, 1.5f, 0.6f);
            w.spawnParticle(Particle.SMOKE, pivot, 25, 0.4, 0.2, 0.4, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || pivot == null) return;
            World w = c.getWorld();

            // Simple harmonic motion. angle oscillates between -PI/2 and +PI/2.
            if (tick > 30) {
                // angular vel = derivative of cos. amplitude = PI/2.
                float t = (tick - 30) * 0.05f;
                float newAngle = (float) (Math.PI / 2.0 * Math.sin(t));
                // detect direction reversal
                boolean dirNeg = (newAngle - angleRad) < 0;
                if (dirNeg != lastDirNeg && tick > 35) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 1.0f, 0.8f);
                    w.spawnParticle(Particle.CRIT, getBallWorldPos(), 12, 0.3, 0.3, 0.3, 0.2);
                    lastDirNeg = dirNeg;
                }
                angleRad = newAngle;
                ballSpinY += (float) Math.toRadians(8);

                // Recompute every link + ball-part world position around pivot.
                updateCableAndBall(w);
            }

            // Trail particle every 2 ticks during active swing.
            if (tick > 35 && tick % 2 == 0) {
                w.spawnParticle(Particle.BLOCK, getBallWorldPos(), 4, 0.4, 0.4, 0.4, 0.02, Material.NETHERITE_BLOCK.createBlockData());
            }

            // Anchor smoke continuous.
            if (tick % 5 == 0) {
                w.spawnParticle(Particle.SMOKE, pivot.clone().add(0, 0.5, 0), 2, 0.1, 0.1, 0.1, 0);
            }

            // Swing loop sound every 10t.
            if (tick > 30 && tick % 10 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.4f, 0.9f);
            }

            // Dissipate over the last 16 ticks.
            int dur = config.getDurationTicks();
            if (tick == dur - 16) {
                for (BlockDisplayHandle h : ballParts) shrinkToZero(h, 8);
                for (BlockDisplayHandle h : weldBands) shrinkToZero(h, 8);
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.4f, 0.5f);
            }
            if (tick >= dur - 14) {
                int idx = cableLinks.size() - 1 - (tick - (dur - 14));
                if (idx >= 0 && idx < cableLinks.size()) shrinkToZero(cableLinks.get(idx), 2);
            }
            if (tick == dur - 3) {
                for (BlockDisplayHandle h : anchorParts) shrinkToZero(h, 3);
            }
        }

        private Location getBallWorldPos() {
            // Ball position rotates around pivot in X-Y plane (angleRad = swing).
            double r = 7.0;
            double bx = Math.sin(angleRad) * r;
            double by = -Math.cos(angleRad) * r;
            return pivot.clone().add(bx, by, 0);
        }

        private void updateCableAndBall(World w) {
            // Cable links interpolate along the swing arc.
            for (int i = 0; i < cableLinks.size(); i++) {
                double t = (i + 1) / 14.0;
                double r = 7.0 * t;
                double sagOffset = (i > 2 && i < 11) ? Math.sin(angularVel * 0.1) * 0.15 : 0;
                double lx = Math.sin(angleRad) * r;
                double ly = -Math.cos(angleRad) * r;
                Location lloc = pivot.clone().add(lx + sagOffset, ly, 0);
                teleportEntity(cableLinks.get(i), lloc);
            }
            // Ball seed + offsets (15 displays).
            Location ballSeed = getBallWorldPos();
            // ballParts ordering: index 0 = core, 1-6 = poles, 7-14 = corners (matches spawn order)
            double[][] poles = {{0, 0, 0}, {0.85, 0, 0}, {-0.85, 0, 0}, {0, 0.85, 0}, {0, -0.85, 0}, {0, 0, 0.85}, {0, 0, -0.85}};
            double dCorn = 0.7;
            double[][] corners = {{dCorn, dCorn, dCorn}, {dCorn, dCorn, -dCorn}, {dCorn, -dCorn, dCorn}, {dCorn, -dCorn, -dCorn},
                    {-dCorn, dCorn, dCorn}, {-dCorn, dCorn, -dCorn}, {-dCorn, -dCorn, dCorn}, {-dCorn, -dCorn, -dCorn}};
            // Combined offsets including the welds (6 bands)
            double[][] welds = {{0, 0, 0.9}, {0, 0, -0.9}, {0.9, 0, 0}, {-0.9, 0, 0}, {0, 0.9, 0}, {0, -0.9, 0}};
            int idx = 0;
            // core + poles
            for (double[] o : poles) {
                if (idx < ballParts.size()) {
                    Location pos = ballSeed.clone().add(o[0], o[1], o[2]);
                    teleportEntity(ballParts.get(idx), pos);
                    if (idx > 0) setRotation(ballParts.get(idx), ballSpinY, 0, 1, 0, 2);
                }
                idx++;
            }
            for (double[] o : corners) {
                if (idx < ballParts.size()) {
                    Location pos = ballSeed.clone().add(o[0], o[1], o[2]);
                    teleportEntity(ballParts.get(idx), pos);
                }
                idx++;
            }
            for (int i = 0; i < weldBands.size() && i < welds.length; i++) {
                Location pos = ballSeed.clone().add(welds[i][0], welds[i][1], welds[i][2]);
                teleportEntity(weldBands.get(i), pos);
            }

            // Keep attack center at the ball for radius damage.
            setCenter(ballSeed);
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new WreckingBallPendulum(plugin); }
    }

    // ================================================================
    // #2 — CHAIN FLAIL "THE THREE HEADS"
    // 37 blocks: 1 IRON collar + 1 POLISHED_BLACKSTONE ceiling post +
    // 3 × (7 CHAIN + 1 NETHERITE core + 3 IRON spikes) = 35 + 2 mount.
    // ================================================================
    public static class ChainFlailThreeHeads extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> mount = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> chains = new ArrayList<>(); // 3 sets
        private final List<List<BlockDisplayHandle>> balls = new ArrayList<>();  // 3 sets (core+3 spikes)
        private Location pivot;
        private final float[] periods = {40f, 52f, 64f};
        private final float[] phases = {0f, (float) (Math.PI * 2.0 / 3.0), (float) (Math.PI * 4.0 / 3.0)};
        private final float[] lastAngles = {0f, 0f, 0f};
        private float collarYaw = 0f;

        public ChainFlailThreeHeads(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_flail_three_heads", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(220.0);
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(220);
            config.setCooldownTicks(240);
            config.setChance(7);
            config.setEnabled(true);
            config.setDesignType("Multi-pendulum (read 3 arcs)");
        }

        @Override
        protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            World w = c.getWorld();
            if (w == null) return;
            pivot = c.clone().add(0, 9.0, 0);

            // Collar + ceiling post.
            BlockDisplayHandle collar = displayBuilder.spawnBlock(pivot.clone(), Material.IRON_BLOCK);
            collar.scale(0f, 0f, 0f).glow(170, 170, 200).interpolation(10, 0);
            spawnedEntities.add(collar.entity()); mount.add(collar);
            growBlock(collar, 0.4f, 10);

            BlockDisplayHandle post = displayBuilder.spawnBlock(pivot.clone().add(0, 0.6, 0), Material.POLISHED_BLACKSTONE);
            post.scale(0f, 0f, 0f).glow(40, 40, 50).interpolation(10, 2);
            spawnedEntities.add(post.entity()); mount.add(post);
            growXYZ(post, 0.2f, 1.0f, 0.2f, 10);

            // Three arms, each 7 chain links + 4 ball parts (core + 3 spikes).
            for (int arm = 0; arm < 3; arm++) {
                List<BlockDisplayHandle> chainList = new ArrayList<>();
                List<BlockDisplayHandle> ballList = new ArrayList<>();
                double azimuth = arm * (Math.PI * 2.0 / 3.0);
                double startX = Math.cos(azimuth) * 0.4;
                double startZ = Math.sin(azimuth) * 0.4;

                for (int i = 0; i < 7; i++) {
                    double t = (i + 1) / 7.0;
                    Location lloc = pivot.clone().add(startX, -t * 4.5, startZ);
                    BlockDisplayHandle link = displayBuilder.spawnBlock(lloc, Material.CHAIN);
                    link.scale(0f, 0f, 0f).glow(80, 80, 90).interpolation(8, 4 + i);
                    spawnedEntities.add(link.entity()); chainList.add(link);
                    growXYZ(link, 0.16f, 0.5f, 0.16f, 8);
                }

                Location ballSeed = pivot.clone().add(startX, -5.0, startZ);
                BlockDisplayHandle core = displayBuilder.spawnBlock(ballSeed.clone(), Material.NETHERITE_BLOCK);
                core.scale(0f, 0f, 0f).glow(40, 40, 40).interpolation(15, 14);
                spawnedEntities.add(core.entity()); ballList.add(core);
                growBlock(core, 0.75f, 14);

                // 3 IRON spikes at 120° around the ball horizontal plane.
                for (int s = 0; s < 3; s++) {
                    double sangle = s * (Math.PI * 2.0 / 3.0);
                    double sx = Math.cos(sangle) * 0.65;
                    double sz = Math.sin(sangle) * 0.65;
                    BlockDisplayHandle spike = displayBuilder.spawnBlock(ballSeed.clone().add(sx, 0, sz), Material.IRON_BLOCK);
                    spike.scale(0f, 0f, 0f).glow(190, 190, 210).interpolation(14, 16);
                    spawnedEntities.add(spike.entity()); ballList.add(spike);
                    growXYZ(spike, 0.10f, 0.32f, 0.10f, 14);
                }

                chains.add(chainList);
                balls.add(ballList);
            }

            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.5f, 0.7f);
            w.spawnParticle(Particle.ELECTRIC_SPARK, pivot, 15, 0.4, 0.2, 0.4, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || pivot == null) return;
            World w = c.getWorld();
            if (tick <= 30) return;

            // Slowly rotate the collar plane on Y (1°/tick).
            collarYaw += (float) Math.toRadians(1);
            for (BlockDisplayHandle m : mount) setRotation(m, collarYaw, 0, 1, 0, 2);

            // Update each of 3 arms independently.
            for (int arm = 0; arm < 3; arm++) {
                float period = periods[arm];
                float phase = phases[arm];
                float armAngle = (float) (Math.PI / 2.0 * Math.sin((tick - 30) * (Math.PI * 2.0 / period) + phase));
                float azimuth = (float) (arm * (Math.PI * 2.0 / 3.0)) + collarYaw;

                // Reversal detection — if sign of (angle - lastAngle) flips.
                if (Math.signum(armAngle - lastAngles[arm]) != Math.signum(0)
                        && Math.abs(armAngle) > 1.3f && tick % 4 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 0.8f, 0.7f + arm * 0.15f);
                }
                lastAngles[arm] = armAngle;

                // Update chain links of this arm.
                List<BlockDisplayHandle> chainList = chains.get(arm);
                for (int i = 0; i < chainList.size(); i++) {
                    double t = (i + 1) / 7.0;
                    double r = 4.5 * t;
                    double swingX = Math.sin(armAngle) * r;
                    double swingY = -Math.cos(armAngle) * r;
                    double lx = swingX * Math.cos(azimuth);
                    double lz = swingX * Math.sin(azimuth);
                    Location lloc = pivot.clone().add(lx, swingY, lz);
                    teleportEntity(chainList.get(i), lloc);
                }
                // Ball + spikes (offsets relative to ball seed).
                List<BlockDisplayHandle> ballList = balls.get(arm);
                double swingX = Math.sin(armAngle) * 4.5;
                double swingY = -Math.cos(armAngle) * 4.5;
                double bx = swingX * Math.cos(azimuth);
                double bz = swingX * Math.sin(azimuth);
                Location ballSeed = pivot.clone().add(bx, swingY, bz);
                if (!ballList.isEmpty()) teleportEntity(ballList.get(0), ballSeed.clone());
                for (int s = 1; s < ballList.size(); s++) {
                    double sangle = (s - 1) * (Math.PI * 2.0 / 3.0) + tick * 0.05;
                    double sx = Math.cos(sangle) * 0.65;
                    double sz = Math.sin(sangle) * 0.65;
                    teleportEntity(ballList.get(s), ballSeed.clone().add(sx, 0, sz));
                }
                // Particle trail per ball.
                if (tick % 3 == 0) {
                    w.spawnParticle(Particle.CRIT, ballSeed, 3, 0.2, 0.2, 0.2, 0.1);
                }
            }

            // Central ring spark every 10t.
            if (tick % 10 == 0) {
                w.spawnParticle(Particle.ELECTRIC_SPARK, pivot.clone(), 6, 0.3, 0.2, 0.3, 0.05);
            }
            // Iron golem roar at max swing every 40t.
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.7f, 0.9f);
            }

            // Update center to ball 1 (the fastest arm) for radius damage.
            float a0 = (float) (Math.PI / 2.0 * Math.sin((tick - 30) * (Math.PI * 2.0 / periods[0]) + phases[0]));
            double az0 = collarYaw;
            double sx0 = Math.sin(a0) * 4.5 * Math.cos(az0);
            double sz0 = Math.sin(a0) * 4.5 * Math.sin(az0);
            double sy0 = -Math.cos(a0) * 4.5;
            setCenter(pivot.clone().add(sx0, sy0, sz0));

            // Dissipate last 18 ticks.
            int dur = config.getDurationTicks();
            if (tick == dur - 18) {
                for (List<BlockDisplayHandle> bl : balls) for (BlockDisplayHandle h : bl) shrinkToZero(h, 10);
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_DEATH, 1.2f, 0.6f);
            }
            if (tick == dur - 8) {
                for (List<BlockDisplayHandle> ch : chains) for (BlockDisplayHandle h : ch) shrinkToZero(h, 6);
                for (BlockDisplayHandle h : mount) shrinkToZero(h, 6);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ChainFlailThreeHeads(plugin); }
    }

    // ================================================================
    // #3 — PENDULUM SWEEP WALL — "The Guillotine"
    // 30 blocks: 3 NETHERITE blade slabs + 6 IRON edge trims +
    // 1 DARK_OAK crossbar + 2 IRON brackets + 10 CHAIN suspension +
    // 8 IRON_BLOCK spine reinforcement = 30 total.
    // ================================================================
    public static class PendulumSweepWall extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> bladeSlabs = new ArrayList<>();
        private final List<BlockDisplayHandle> edgeTrims = new ArrayList<>();
        private final List<BlockDisplayHandle> chainsHang = new ArrayList<>();
        private final List<BlockDisplayHandle> mountParts = new ArrayList<>();
        private final List<BlockDisplayHandle> reinforce = new ArrayList<>();
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private Location pivot;
        private float angleRad = 0f;
        private boolean lastDirNeg = false;

        public PendulumSweepWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pendulum_sweep_wall", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(380.0);
            config.setDamageRadius(1.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(240);
            config.setCooldownTicks(280);
            config.setChance(5);
            config.setEnabled(true);
            config.setDesignType("Bisecting sweep wall (sprint between gaps)");
        }

        @Override
        protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            World w = c.getWorld();
            if (w == null) return;
            pivot = c.clone().add(0, 8.0, 0);
            angleRad = (float) Math.toRadians(-80);

            // Crossbar: DARK_OAK_LOG above pivot.
            BlockDisplayHandle bar = displayBuilder.spawnBlock(pivot.clone(), Material.DARK_OAK_LOG);
            bar.scale(0f, 0f, 0f).glow(70, 50, 30).interpolation(10, 0);
            spawnedEntities.add(bar.entity()); mountParts.add(bar); allBlocks.add(bar);
            growXYZ(bar, 2.0f, 0.25f, 0.25f, 10);

            // 2 IRON brackets.
            for (int i = 0; i < 2; i++) {
                double xo = (i == 0) ? -0.9 : 0.9;
                BlockDisplayHandle bk = displayBuilder.spawnBlock(pivot.clone().add(xo, 0, 0), Material.IRON_BLOCK);
                bk.scale(0f, 0f, 0f).glow(180, 180, 200).interpolation(10, 3);
                spawnedEntities.add(bk.entity()); mountParts.add(bk); allBlocks.add(bk);
                growXYZ(bk, 0.3f, 0.4f, 0.3f, 10);
            }

            // 10 CHAIN suspension links in Y-split (5 each side).
            for (int side = 0; side < 2; side++) {
                double sx = (side == 0) ? -0.8 : 0.8;
                for (int i = 0; i < 5; i++) {
                    double t = (i + 1) / 5.0;
                    double linkX = sx * (1 - t * 0.5);
                    Location lloc = pivot.clone().add(linkX, -t * 2.5, 0);
                    BlockDisplayHandle ch = displayBuilder.spawnBlock(lloc, Material.CHAIN);
                    ch.scale(0f, 0f, 0f).glow(80, 80, 90).interpolation(8, 5 + i);
                    spawnedEntities.add(ch.entity()); chainsHang.add(ch); allBlocks.add(ch);
                    growXYZ(ch, 0.16f, 0.5f, 0.16f, 8);
                }
            }

            // 3 NETHERITE blade slabs side by side.
            for (int i = 0; i < 3; i++) {
                double xo = -1.8 + i * 1.8;
                Location bloc = pivot.clone().add(xo, -4.0, 0);
                BlockDisplayHandle b = displayBuilder.spawnBlock(bloc, Material.NETHERITE_BLOCK);
                b.scale(0f, 0f, 0f).glow(30, 30, 30).interpolation(15, 12);
                spawnedEntities.add(b.entity()); bladeSlabs.add(b); allBlocks.add(b);
                float wid = (i == 1) ? 1.85f : 1.8f;
                growXYZ(b, wid, 2.0f, 0.2f, 15);
            }

            // 6 IRON edge trims (top + bottom per slab).
            for (int i = 0; i < 3; i++) {
                double xo = -1.8 + i * 1.8;
                for (int e = 0; e < 2; e++) {
                    double yo = (e == 0) ? -3.0 : -5.0;
                    Location eloc = pivot.clone().add(xo, yo, 0);
                    BlockDisplayHandle t = displayBuilder.spawnBlock(eloc, Material.IRON_BLOCK);
                    t.scale(0f, 0f, 0f).glow(220, 220, 230).interpolation(12, 14);
                    spawnedEntities.add(t.entity()); edgeTrims.add(t); allBlocks.add(t);
                    growXYZ(t, 1.8f, 0.1f, 0.12f, 12);
                }
            }

            // 8 IRON reinforcement studs on blade face.
            for (int i = 0; i < 8; i++) {
                double xo = -1.6 + (i % 4) * 1.1;
                double yo = -3.5 - (i / 4) * 1.0;
                Location sloc = pivot.clone().add(xo, yo, 0.15);
                BlockDisplayHandle s = displayBuilder.spawnBlock(sloc, Material.IRON_BLOCK);
                s.scale(0f, 0f, 0f).glow(200, 200, 220).interpolation(10, 16);
                spawnedEntities.add(s.entity()); reinforce.add(s); allBlocks.add(s);
                growXYZ(s, 0.18f, 0.18f, 0.06f, 10);
            }

            DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_PLACE, 1.6f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.4f, 0.7f);
            w.spawnParticle(Particle.SMOKE, pivot, 30, 1.5, 0.3, 0.3, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || pivot == null) return;
            World w = c.getWorld();

            if (tick > 30) {
                float t = (tick - 30) * 0.05f;
                float newAngle = (float) (Math.toRadians(90) * Math.sin(t));
                boolean dirNeg = (newAngle - angleRad) < 0;
                if (dirNeg != lastDirNeg && tick > 40 && Math.abs(newAngle) > 1.3f) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.4f, 0.5f);
                    lastDirNeg = dirNeg;
                }
                angleRad = newAngle;

                // All blade parts rotate around pivot in X-Y plane.
                updateBlade();

                // SMOKE_LARGE trail off the blade face.
                if (tick % 2 == 0) {
                    Location bladeMid = bladeMidPos();
                    w.spawnParticle(Particle.LARGE_SMOKE, bladeMid, 6, 0.5, 0.4, 0.3, 0.02);
                    w.spawnParticle(Particle.BLOCK, bladeMid, 3, 0.4, 0.4, 0.4, 0.05, Material.NETHERITE_BLOCK.createBlockData());
                }
                if (tick % 6 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.6f, 0.7f);
                }
                if (tick % 14 == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.6f, 0.8f);
                }
            }

            int dur = config.getDurationTicks();
            if (tick == dur - 18) {
                for (BlockDisplayHandle h : bladeSlabs) shrinkToZero(h, 10);
                for (BlockDisplayHandle h : edgeTrims) shrinkToZero(h, 10);
                for (BlockDisplayHandle h : reinforce) shrinkToZero(h, 10);
                DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 1.4f, 0.5f);
            }
            if (tick == dur - 6) {
                for (BlockDisplayHandle h : chainsHang) shrinkToZero(h, 5);
                for (BlockDisplayHandle h : mountParts) shrinkToZero(h, 5);
            }
        }

        private Location bladeMidPos() {
            double r = 4.0;
            double bx = Math.sin(angleRad) * r;
            double by = -Math.cos(angleRad) * r;
            return pivot.clone().add(bx, by, 0);
        }

        private void updateBlade() {
            // Recompute every part as offset relative to pivot, swung by angleRad.
            // Crossbar + brackets stay attached at pivot (no swing for them).
            double r = 4.0;
            double bladeX = Math.sin(angleRad) * r;
            double bladeY = -Math.cos(angleRad) * r;
            // Chain suspension: 5 per side, distributed along the 4-block path.
            for (int i = 0; i < chainsHang.size(); i++) {
                int side = i / 5;
                int idx = i % 5;
                double sx = (side == 0) ? -0.8 : 0.8;
                double t = (idx + 1) / 5.0;
                double rl = r * t;
                double localX = sx * (1 - t * 0.5);
                double swungX = Math.sin(angleRad) * rl + localX * Math.cos(angleRad);
                double swungY = -Math.cos(angleRad) * rl + localX * Math.sin(angleRad);
                teleportEntity(chainsHang.get(i), pivot.clone().add(swungX, swungY, 0));
            }
            // 3 blade slabs.
            for (int i = 0; i < bladeSlabs.size(); i++) {
                double xo = -1.8 + i * 1.8;
                // rotate (xo, -r) by angle
                double sx = xo * Math.cos(angleRad) - (-r) * Math.sin(angleRad);
                double sy = xo * Math.sin(angleRad) + (-r) * Math.cos(angleRad);
                teleportEntity(bladeSlabs.get(i), pivot.clone().add(sx, sy, 0));
                setRotation(bladeSlabs.get(i), angleRad, 0, 0, 1, 2);
            }
            // edge trims (6).
            for (int i = 0; i < edgeTrims.size(); i++) {
                int slot = i / 2;
                int edge = i % 2;
                double xo = -1.8 + slot * 1.8;
                double yo = (edge == 0) ? (-r + 1.0) : (-r - 1.0);
                double sx = xo * Math.cos(angleRad) - yo * Math.sin(angleRad);
                double sy = xo * Math.sin(angleRad) + yo * Math.cos(angleRad);
                teleportEntity(edgeTrims.get(i), pivot.clone().add(sx, sy, 0));
                setRotation(edgeTrims.get(i), angleRad, 0, 0, 1, 2);
            }
            // 8 reinforcement studs.
            for (int i = 0; i < reinforce.size(); i++) {
                double xo = -1.6 + (i % 4) * 1.1;
                double yo = -r + 0.5 - (i / 4) * 1.0;
                double sx = xo * Math.cos(angleRad) - yo * Math.sin(angleRad);
                double sy = xo * Math.sin(angleRad) + yo * Math.cos(angleRad);
                teleportEntity(reinforce.get(i), pivot.clone().add(sx, sy, 0.15));
                setRotation(reinforce.get(i), angleRad, 0, 0, 1, 2);
            }
            setCenter(bladeMidPos());
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new PendulumSweepWall(plugin); }
    }

    // ================================================================
    // #4 — OVERHEAD CHAIN LARIAT — "The Loop"
    // 32 blocks: 16 CHAIN loop ring + 6 CHAIN dangling links +
    // 1 IRON swivel hook + 8 IRON knot accents + 1 IRON_BLOCK hub spar.
    // ================================================================
    public static class OverheadChainLariat extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> ringLinks = new ArrayList<>();
        private final List<BlockDisplayHandle> dangleLinks = new ArrayList<>();
        private final List<BlockDisplayHandle> knots = new ArrayList<>();
        private BlockDisplayHandle hook;
        private BlockDisplayHandle spar;
        private Location pivot;
        private double currentRadius = 0.5;
        private float ringYaw = 0f;

        public OverheadChainLariat(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("overhead_chain_lariat", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(240.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(6);
            config.setDamageDelayTicks(15);
            config.setDurationTicks(200);
            config.setCooldownTicks(220);
            config.setChance(7);
            config.setEnabled(true);
            config.setDesignType("Overhead lariat (duck/sprint past)");
        }

        @Override
        protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            World w = c.getWorld();
            if (w == null) return;
            pivot = c.clone().add(0, 5.0, 0);

            // Hook + central spar.
            hook = displayBuilder.spawnBlock(pivot.clone().add(0, 1.0, 0), Material.IRON_BLOCK);
            hook.scale(0f, 0f, 0f).glow(200, 200, 220).interpolation(10, 0);
            spawnedEntities.add(hook.entity());
            growXYZ(hook, 0.25f, 0.3f, 0.25f, 10);

            spar = displayBuilder.spawnBlock(pivot.clone().add(0, 1.6, 0), Material.IRON_BLOCK);
            spar.scale(0f, 0f, 0f).glow(170, 170, 200).interpolation(10, 2);
            spawnedEntities.add(spar.entity());
            growXYZ(spar, 0.15f, 0.6f, 0.15f, 10);

            // 16 CHAIN ring links, start clustered at small radius.
            for (int i = 0; i < 16; i++) {
                double a = (Math.PI * 2.0 * i) / 16;
                double x = Math.cos(a) * currentRadius;
                double z = Math.sin(a) * currentRadius;
                Location lloc = pivot.clone().add(x, 0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(lloc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(80, 80, 90).interpolation(8, 4 + i / 4);
                spawnedEntities.add(h.entity()); ringLinks.add(h);
                growXYZ(h, 0.18f, 0.18f, 0.18f, 8);
                // Tangential rotation.
                setRotation(h, (float) a, 0, 1, 0, 8);
            }

            // 6 dangling links underneath the ring (every other position).
            for (int i = 0; i < 6; i++) {
                double a = (Math.PI * 2.0 * i) / 6;
                double x = Math.cos(a) * currentRadius;
                double z = Math.sin(a) * currentRadius;
                Location lloc = pivot.clone().add(x, -0.4, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(lloc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(80, 80, 90).interpolation(8, 8);
                spawnedEntities.add(h.entity()); dangleLinks.add(h);
                growXYZ(h, 0.14f, 0.4f, 0.14f, 8);
            }

            // 8 IRON knot accents on alternating ring positions.
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2.0 * i) / 8;
                double x = Math.cos(a) * currentRadius;
                double z = Math.sin(a) * currentRadius;
                Location lloc = pivot.clone().add(x, 0.08, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(lloc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(210, 210, 230).interpolation(8, 10);
                spawnedEntities.add(h.entity()); knots.add(h);
                growXYZ(h, 0.12f, 0.12f, 0.12f, 8);
            }

            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.5f, 1.2f);
            w.spawnParticle(Particle.ELECTRIC_SPARK, pivot, 18, 1, 0.3, 1, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || pivot == null) return;
            World w = c.getWorld();

            // Phase: expand for first 80t, hold 30t, contract for 30t.
            int dur = config.getDurationTicks();
            int expandEnd = 90;
            int holdEnd = 130;
            int contractEnd = dur - 20;

            if (tick > 15 && tick <= expandEnd) {
                currentRadius = 0.5 + (tick - 15) * 0.04375; // 0.5 → 4.0 over ~80 ticks
            } else if (tick > holdEnd && tick <= contractEnd) {
                double prog = (tick - holdEnd) / (double) (contractEnd - holdEnd);
                currentRadius = 4.0 * (1.0 - prog);
            }

            // Update ring and rotate.
            ringYaw += (float) Math.toRadians(12);
            updateRing(tick);

            // Center moves with the ring (cylinder check finds players in lasso path).
            setCenter(pivot.clone().add(0, -0.5, 0));
            // Override damage radius based on current ring radius so the damage zone hugs the ring.
            config.setDamageRadius(Math.max(0.8, currentRadius + 0.3));

            if (tick % 3 == 0) {
                // ELECTRIC_SPARK trail at a random ring position.
                int idx = (tick / 3) % ringLinks.size();
                Location p = ringLinks.get(idx).entity().getLocation();
                w.spawnParticle(Particle.ELECTRIC_SPARK, p, 4, 0.1, 0.1, 0.1, 0.08);
            }
            if (tick % 5 == 0) {
                w.spawnParticle(Particle.BLOCK, pivot.clone().add(currentRadius, 0, 0), 3, 0.2, 0.2, 0.2, 0.05, Material.CHAIN.createBlockData());
            }
            if (tick % 8 == 0) {
                float pitch = (float) Math.min(1.6, 1.0 + currentRadius * 0.15);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.7f, pitch);
            }
            // Hook pulse every 6t.
            if (hook != null && tick % 6 == 0) {
                float s = 0.25f + (tick % 12 == 0 ? 0.05f : -0.05f);
                growXYZ(hook, s, 0.3f, s, 5);
            }
            if (tick == expandEnd) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.4f, 1.1f);
                w.spawnParticle(Particle.ELECTRIC_SPARK, pivot, 30, 2, 0.3, 2, 0.2);
            }
            if (tick == contractEnd + 2) {
                for (BlockDisplayHandle h : ringLinks) shrinkToZero(h, 8);
                for (BlockDisplayHandle h : dangleLinks) shrinkToZero(h, 8);
                for (BlockDisplayHandle h : knots) shrinkToZero(h, 8);
                if (hook != null) shrinkToZero(hook, 8);
                if (spar != null) shrinkToZero(spar, 8);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 1.2f, 0.7f);
            }
        }

        private void updateRing(int tick) {
            for (int i = 0; i < ringLinks.size(); i++) {
                double a = (Math.PI * 2.0 * i) / ringLinks.size() + ringYaw;
                double x = Math.cos(a) * currentRadius;
                double z = Math.sin(a) * currentRadius;
                teleportEntity(ringLinks.get(i), pivot.clone().add(x, 0, z));
                setRotation(ringLinks.get(i), (float) a, 0, 1, 0, 2);
            }
            float dangleX = (float) Math.toRadians(35) * (float) Math.min(1.0, (currentRadius - 0.5) / 3.5);
            for (int i = 0; i < dangleLinks.size(); i++) {
                double a = (Math.PI * 2.0 * i) / dangleLinks.size() + ringYaw;
                double x = Math.cos(a) * currentRadius;
                double z = Math.sin(a) * currentRadius;
                teleportEntity(dangleLinks.get(i), pivot.clone().add(x, -0.4, z));
                setRotation(dangleLinks.get(i), dangleX, (float) Math.cos(a), 0, (float) Math.sin(a), 2);
            }
            for (int i = 0; i < knots.size(); i++) {
                double a = (Math.PI * 2.0 * i) / knots.size() + ringYaw * 0.5;
                double x = Math.cos(a) * currentRadius;
                double z = Math.sin(a) * currentRadius;
                teleportEntity(knots.get(i), pivot.clone().add(x, 0.08, z));
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new OverheadChainLariat(plugin); }
    }

    // ================================================================
    // #5 — DOUBLE PENDULUM — "The Chaos Machine"
    // 34 blocks: 7 inner-arm CHAIN + 1 IRON elbow + 7 outer-arm CHAIN
    // + 1 NETHERITE core + 6 IRON poles + 1 POLISHED_BLACKSTONE anchor
    // + 4 CRYING_OBSIDIAN ghost trail + 7 GRAY_CONCRETE supports = 34.
    // ================================================================
    public static class DoublePendulum extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> innerChain = new ArrayList<>();
        private final List<BlockDisplayHandle> outerChain = new ArrayList<>();
        private final List<BlockDisplayHandle> ballParts = new ArrayList<>();
        private final List<BlockDisplayHandle> ghostTrail = new ArrayList<>();
        private final List<BlockDisplayHandle> supports = new ArrayList<>();
        private BlockDisplayHandle anchor;
        private BlockDisplayHandle elbow;
        private Location pivot;
        private double theta1 = 1.2, theta2 = 0.5;
        private double omega1 = 0, omega2 = 0;
        private final double L1 = 3.5, L2 = 3.0;
        private int ghostIdx = 0;

        public DoublePendulum(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("double_pendulum", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(320.0);
            config.setDamageRadius(2.4);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(25);
            config.setDurationTicks(260);
            config.setCooldownTicks(300);
            config.setChance(4);
            config.setEnabled(true);
            config.setDesignType("Chaos pendulum (unpredictable arc, read late)");
        }

        @Override
        protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            World w = c.getWorld();
            if (w == null) return;
            pivot = c.clone().add(0, 9.0, 0);

            anchor = displayBuilder.spawnBlock(pivot.clone(), Material.POLISHED_BLACKSTONE);
            anchor.scale(0f, 0f, 0f).glow(50, 50, 60).interpolation(10, 0);
            spawnedEntities.add(anchor.entity());
            growBlock(anchor, 0.4f, 10);

            // Inner chain: 7 links.
            for (int i = 0; i < 7; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(pivot.clone().add(0, -(i + 1) * 0.5, 0), Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(90, 90, 100).interpolation(8, 2 + i);
                spawnedEntities.add(h.entity()); innerChain.add(h);
                growXYZ(h, 0.15f, 0.45f, 0.15f, 8);
            }
            // Elbow pivot.
            elbow = displayBuilder.spawnBlock(pivot.clone().add(0, -L1, 0), Material.IRON_BLOCK);
            elbow.scale(0f, 0f, 0f).glow(180, 180, 200).interpolation(10, 10);
            spawnedEntities.add(elbow.entity());
            growBlock(elbow, 0.4f, 10);

            // Outer chain: 7 links.
            for (int i = 0; i < 7; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(pivot.clone().add(0, -L1 - (i + 1) * 0.4, 0), Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(90, 90, 100).interpolation(8, 12 + i);
                spawnedEntities.add(h.entity()); outerChain.add(h);
                growXYZ(h, 0.15f, 0.42f, 0.15f, 8);
            }
            // Outer ball: 1 core + 6 poles.
            Location ballSeed = pivot.clone().add(0, -L1 - L2, 0);
            BlockDisplayHandle core = displayBuilder.spawnBlock(ballSeed.clone(), Material.NETHERITE_BLOCK);
            core.scale(0f, 0f, 0f).glow(30, 30, 30).interpolation(15, 20);
            spawnedEntities.add(core.entity()); ballParts.add(core);
            growBlock(core, 0.7f, 15);
            double[][] poleOffsets = {{0.55, 0, 0}, {-0.55, 0, 0}, {0, 0.55, 0}, {0, -0.55, 0}, {0, 0, 0.55}, {0, 0, -0.55}};
            for (double[] o : poleOffsets) {
                BlockDisplayHandle p = displayBuilder.spawnBlock(ballSeed.clone().add(o[0], o[1], o[2]), Material.IRON_BLOCK);
                p.scale(0f, 0f, 0f).glow(190, 190, 210).interpolation(13, 22);
                spawnedEntities.add(p.entity()); ballParts.add(p);
                growBlock(p, 0.28f, 13);
            }

            // 4 CRYING_OBSIDIAN ghost trail slabs (start invisible).
            for (int i = 0; i < 4; i++) {
                BlockDisplayHandle g = displayBuilder.spawnBlock(ballSeed.clone(), Material.CRYING_OBSIDIAN);
                g.scale(0f, 0f, 0f).glow(80, 0, 120).interpolation(5, 0);
                spawnedEntities.add(g.entity()); ghostTrail.add(g);
            }

            // 7 GRAY_CONCRETE support brackets clustered around inner+outer junction.
            for (int i = 0; i < 7; i++) {
                double yo = -1.0 - i * 0.5;
                double xo = (i % 2 == 0) ? 0.3 : -0.3;
                BlockDisplayHandle s = displayBuilder.spawnBlock(pivot.clone().add(xo, yo, 0), Material.GRAY_CONCRETE);
                s.scale(0f, 0f, 0f).glow(120, 120, 130).interpolation(10, 6 + i);
                spawnedEntities.add(s.entity()); supports.add(s);
                growXYZ(s, 0.1f, 0.1f, 0.1f, 10);
            }

            DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_PLACE, 1.4f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.3f, 0.8f);
            w.spawnParticle(Particle.SOUL_FIRE_FLAME, pivot, 12, 0.3, 0.2, 0.3, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || pivot == null) return;
            World w = c.getWorld();
            if (tick <= 30) return;

            // Simplified double-pendulum dynamics (Euler integration, equal mass).
            double g = 0.012; // server-tick gravity constant
            double m = 1.0;
            double num1 = -3 * g * Math.sin(theta1) - g * Math.sin(theta1 - 2 * theta2)
                    - 2 * Math.sin(theta1 - theta2) * (omega2 * omega2 * L2 + omega1 * omega1 * L1 * Math.cos(theta1 - theta2));
            double den1 = L1 * (3 - Math.cos(2 * theta1 - 2 * theta2));
            double a1 = num1 / den1;
            double num2 = 2 * Math.sin(theta1 - theta2) * (omega1 * omega1 * L1 + g * Math.cos(theta1)
                    + omega2 * omega2 * L2 * Math.cos(theta1 - theta2));
            double den2 = L2 * (3 - Math.cos(2 * theta1 - 2 * theta2));
            double a2 = num2 / den2;
            omega1 += a1;
            omega2 += a2;
            // Friction so it doesn't escape simulation.
            omega1 *= 0.999;
            omega2 *= 0.999;
            theta1 += omega1;
            theta2 += omega2;

            // Inner-arm end position.
            double ex = pivot.getX() + L1 * Math.sin(theta1);
            double ey = pivot.getY() - L1 * Math.cos(theta1);
            // Outer-arm end (ball) position.
            double bx = ex + L2 * Math.sin(theta2);
            double by = ey - L2 * Math.cos(theta2);
            Location ballSeed = new Location(pivot.getWorld(), bx, by, pivot.getZ());

            // Position inner chain.
            for (int i = 0; i < innerChain.size(); i++) {
                double t = (i + 1) / 7.0;
                double lx = pivot.getX() + L1 * t * Math.sin(theta1);
                double ly = pivot.getY() - L1 * t * Math.cos(theta1);
                teleportEntity(innerChain.get(i), new Location(pivot.getWorld(), lx, ly, pivot.getZ()));
            }
            // Elbow.
            if (elbow != null) teleportEntity(elbow, new Location(pivot.getWorld(), ex, ey, pivot.getZ()));
            // Outer chain.
            for (int i = 0; i < outerChain.size(); i++) {
                double t = (i + 1) / 7.0;
                double lx = ex + L2 * t * Math.sin(theta2);
                double ly = ey - L2 * t * Math.cos(theta2);
                teleportEntity(outerChain.get(i), new Location(pivot.getWorld(), lx, ly, pivot.getZ()));
            }
            // Ball parts (1 core + 6 poles).
            double[][] poles = {{0, 0, 0}, {0.55, 0, 0}, {-0.55, 0, 0}, {0, 0.55, 0}, {0, -0.55, 0}, {0, 0, 0.55}, {0, 0, -0.55}};
            for (int i = 0; i < ballParts.size() && i < poles.length; i++) {
                teleportEntity(ballParts.get(i), ballSeed.clone().add(poles[i][0], poles[i][1], poles[i][2]));
            }

            // Ghost trail: oldest slab moves to current ball pos.
            if (tick % 3 == 0 && !ghostTrail.isEmpty()) {
                BlockDisplayHandle ghost = ghostTrail.get(ghostIdx);
                teleportEntity(ghost, ballSeed.clone());
                growXYZ(ghost, 0.1f, 0.04f, 0.1f, 1);
                ghostIdx = (ghostIdx + 1) % ghostTrail.size();
                // Fade out the next-oldest (slowly).
                BlockDisplayHandle older = ghostTrail.get(ghostIdx);
                shrinkToZero(older, 12);
            }

            // Particles + sounds.
            if (tick % 1 == 0) {
                Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(80, 80, 120), 1.2f);
                w.spawnParticle(Particle.DUST, ballSeed, 1, 0.05, 0.05, 0.05, 0, dust);
            }
            if (tick % 12 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.6f, 0.8f);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 0.6f, 1.1f);
            }
            if (Math.abs(omega2) > 0.2 && tick % 8 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 0.5f, 1.2f);
            }

            setCenter(ballSeed);

            int dur = config.getDurationTicks();
            if (tick == dur - 20) {
                for (BlockDisplayHandle h : ballParts) shrinkToZero(h, 10);
                for (BlockDisplayHandle h : ghostTrail) shrinkToZero(h, 10);
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.3f, 0.5f);
            }
            if (tick == dur - 8) {
                for (BlockDisplayHandle h : innerChain) shrinkToZero(h, 6);
                for (BlockDisplayHandle h : outerChain) shrinkToZero(h, 6);
                for (BlockDisplayHandle h : supports) shrinkToZero(h, 6);
                if (elbow != null) shrinkToZero(elbow, 6);
                if (anchor != null) shrinkToZero(anchor, 6);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new DoublePendulum(plugin); }
    }

    // ================================================================
    // #6 — CHAIN WHIP CRACK — "The Lash"
    // 30 blocks: 14 CHAIN whip body + 2 DARK_OAK_LOG grip + 8 IRON_BLOCK
    // crack starburst + 4 GRAY_CONCRETE wind-up guide + 2 IRON_BLOCK
    // wrist fittings = 30 total.
    // Tracks player; damage-on-impact-only.
    // ================================================================
    public static class ChainWhipCrack extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> whipBody = new ArrayList<>();
        private final List<BlockDisplayHandle> grip = new ArrayList<>();
        private final List<BlockDisplayHandle> starburst = new ArrayList<>();
        private final List<BlockDisplayHandle> guide = new ArrayList<>();
        private final List<BlockDisplayHandle> wrist = new ArrayList<>();
        private Location handleLoc;
        private Location tipLoc;
        private Location crackTarget;
        private boolean cracked = false;

        public ChainWhipCrack(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_whip_crack", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(350.0);
            config.setImpactRadius(1.8);
            config.setDurationTicks(80);
            config.setCooldownTicks(180);
            config.setChance(8);
            config.setTracksPlayer(true);
            config.setEnabled(true);
            config.setDesignType("Whip-crack snipe (read arc + tight window)");
        }

        @Override
        protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            World w = c.getWorld();
            if (w == null) return;
            handleLoc = c.clone().add(-5, 4, 0); // handle off to the side
            tipLoc = handleLoc.clone();
            crackTarget = c.clone().add(0, 1.5, 0); // target the player

            // 2 DARK_OAK_LOG grip cubes at handle.
            for (int i = 0; i < 2; i++) {
                BlockDisplayHandle g = displayBuilder.spawnBlock(handleLoc.clone().add(0, i * 0.5, 0), Material.DARK_OAK_LOG);
                g.scale(0f, 0f, 0f).glow(60, 40, 20).interpolation(8, 0);
                spawnedEntities.add(g.entity()); grip.add(g);
                growXYZ(g, 0.3f, 0.6f, 0.3f, 8);
            }
            // 2 IRON_BLOCK wrist fittings.
            for (int i = 0; i < 2; i++) {
                BlockDisplayHandle wr = displayBuilder.spawnBlock(handleLoc.clone().add(0, 1.0 + i * 0.2, 0), Material.IRON_BLOCK);
                wr.scale(0f, 0f, 0f).glow(190, 190, 210).interpolation(8, 1);
                spawnedEntities.add(wr.entity()); wrist.add(wr);
                growXYZ(wr, 0.2f, 0.12f, 0.2f, 8);
            }

            // 14 CHAIN whip body links, scale tapers handle→tip.
            for (int i = 0; i < 14; i++) {
                double t = (i + 1) / 14.0;
                Location lloc = handleLoc.clone().add(t * 2.0, 0, 0); // start coiled close
                BlockDisplayHandle h = displayBuilder.spawnBlock(lloc, Material.CHAIN);
                float s = (float) (0.30 - i * 0.016);
                h.scale(0f, 0f, 0f).glow(80, 80, 90).interpolation(8, 2 + i / 2);
                spawnedEntities.add(h.entity()); whipBody.add(h);
                growXYZ(h, s, s * 1.2f, s, 8);
            }

            // 4 GRAY_CONCRETE wind-up guide slabs along the arc.
            for (int i = 0; i < 4; i++) {
                double prog = (i + 1) / 4.0;
                Location gl = handleLoc.clone().add((crackTarget.getX() - handleLoc.getX()) * prog,
                        (crackTarget.getY() - handleLoc.getY()) * prog,
                        (crackTarget.getZ() - handleLoc.getZ()) * prog);
                BlockDisplayHandle g = displayBuilder.spawnBlock(gl, Material.GRAY_CONCRETE);
                g.scale(0f, 0f, 0f).glow(140, 140, 140).interpolation(6, 1);
                spawnedEntities.add(g.entity()); guide.add(g);
                growXYZ(g, 0.4f, 0.02f, 0.08f, 6);
            }

            // 8 IRON starburst slabs at crackTarget (initially hidden).
            for (int i = 0; i < 8; i++) {
                BlockDisplayHandle s = displayBuilder.spawnBlock(crackTarget.clone(), Material.IRON_BLOCK);
                s.scale(0f, 0f, 0f).glow(255, 240, 200).interpolation(2, 0);
                spawnedEntities.add(s.entity()); starburst.add(s);
            }

            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.5f, 0.6f);
            w.spawnParticle(Particle.SMOKE, handleLoc, 12, 0.4, 0.3, 0.4, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Update crack target each tick to current center (tracksPlayer pulls it for us).
            if (tick % 5 == 0) {
                crackTarget = c.clone().add(0, 1.5, 0);
                // Move guide slabs along the up-to-date arc.
                for (int i = 0; i < guide.size(); i++) {
                    double prog = (i + 1) / 4.0;
                    Location gl = handleLoc.clone().add((crackTarget.getX() - handleLoc.getX()) * prog,
                            (crackTarget.getY() - handleLoc.getY()) * prog,
                            (crackTarget.getZ() - handleLoc.getZ()) * prog);
                    teleportEntity(guide.get(i), gl);
                }
            }

            // Telegraph (0-15): wind-up sound builds, whip recoils.
            if (tick == 5) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.3f, 0.5f);
            }
            if (tick >= 1 && tick < 15) {
                // Whip body coils backward — each link slightly nearer handle.
                for (int i = 0; i < whipBody.size(); i++) {
                    double t = (i + 1) / 14.0;
                    double recoilScale = 0.5; // tight coil
                    Location lloc = handleLoc.clone().add(t * 2.0 * recoilScale, Math.sin(t * Math.PI) * 0.4, 0);
                    teleportEntity(whipBody.get(i), lloc);
                }
            }

            // Extension (15-30): whip extends forward toward crackTarget along S-curve.
            if (tick >= 15 && tick < 30) {
                double prog = (tick - 15) / 15.0;
                for (int i = 0; i < whipBody.size(); i++) {
                    double t = (i + 1) / 14.0;
                    double interp = prog;
                    double x = handleLoc.getX() + (crackTarget.getX() - handleLoc.getX()) * t * interp;
                    double y = handleLoc.getY() + (crackTarget.getY() - handleLoc.getY()) * t * interp + Math.sin(t * Math.PI) * 0.4;
                    double z = handleLoc.getZ() + (crackTarget.getZ() - handleLoc.getZ()) * t * interp;
                    Location lloc = new Location(handleLoc.getWorld(), x, y, z);
                    BlockDisplayHandle h = whipBody.get(i);
                    h.entity().setInterpolationDuration(15);
                    h.entity().setInterpolationDelay(0);
                    teleportEntity(h, lloc);
                }
                tipLoc = whipBody.get(whipBody.size() - 1).entity().getLocation();
                if (tick % 4 == 0) {
                    w.spawnParticle(Particle.LARGE_SMOKE, tipLoc, 4, 0.2, 0.2, 0.2, 0.02);
                }
            }

            // Crack (30-35): tip slams to crackTarget very fast, starburst appears.
            if (tick == 30) {
                for (int i = 0; i < whipBody.size(); i++) {
                    double t = (i + 1) / 14.0;
                    Location lloc = handleLoc.clone().add(
                            (crackTarget.getX() - handleLoc.getX()) * t,
                            (crackTarget.getY() - handleLoc.getY()) * t,
                            (crackTarget.getZ() - handleLoc.getZ()) * t);
                    BlockDisplayHandle h = whipBody.get(i);
                    h.entity().setInterpolationDuration(3);
                    h.entity().setInterpolationDelay(0);
                    teleportEntity(h, lloc);
                }
                // Starburst grows.
                for (int i = 0; i < starburst.size(); i++) {
                    double a = (Math.PI * 2.0 * i) / starburst.size();
                    Location sp = crackTarget.clone().add(Math.cos(a) * 0.4, 0, Math.sin(a) * 0.4);
                    teleportEntity(starburst.get(i), sp);
                    growXYZ(starburst.get(i), 0.12f, 0.04f, 0.12f, 2);
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.6f, 1.4f);
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 1.2f);
                w.spawnParticle(Particle.EXPLOSION, crackTarget, 8, 0.3, 0.3, 0.3, 0.02);
                w.spawnParticle(Particle.ELECTRIC_SPARK, crackTarget, 20, 0.5, 0.5, 0.5, 0.5);
                // IMPACT.
                if (!cracked) {
                    cracked = true;
                    triggerImpactDamage(crackTarget);
                }
            }

            // Starburst expansion (32-35).
            if (tick >= 32 && tick <= 35) {
                float s = 0.12f + (tick - 32) * 0.23f;
                for (int i = 0; i < starburst.size(); i++) {
                    double a = (Math.PI * 2.0 * i) / starburst.size();
                    Location sp = crackTarget.clone().add(Math.cos(a) * (0.4 + (tick - 32) * 0.6), 0, Math.sin(a) * (0.4 + (tick - 32) * 0.6));
                    teleportEntity(starburst.get(i), sp);
                    growXYZ(starburst.get(i), s, 0.04f, s, 1);
                }
            }
            if (tick == 36) {
                for (BlockDisplayHandle h : starburst) shrinkToZero(h, 3);
                for (BlockDisplayHandle h : guide) shrinkToZero(h, 3);
            }

            // Retract (35-50): whip body collapses tip→handle.
            if (tick >= 36) {
                int retractIdx = whipBody.size() - 1 - (tick - 36);
                if (retractIdx >= 0 && retractIdx < whipBody.size()) {
                    shrinkToZero(whipBody.get(retractIdx), 2);
                }
            }
            if (tick == 55) {
                for (BlockDisplayHandle h : grip) shrinkToZero(h, 4);
                for (BlockDisplayHandle h : wrist) shrinkToZero(h, 4);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ChainWhipCrack(plugin); }
    }

    // ================================================================
    // #7 — TRI-PENDULUM FAN — "The Three Sectors"
    // 36 blocks: 3 POLISHED_BLACKSTONE disk slabs + 1 IRON hub +
    // 3 × (6 CHAIN + 1 IRON core + 4 GRAY_CONCRETE poles) = 33 + 4 = 37,
    // truncated to 36 by dropping 1 pole on the last arm.
    // (Final: 4 mount + 3 arms × (6 chain + 1 core + 4 poles - last arm 3) = 36)
    // ================================================================
    public static class TriPendulumFan extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> mount = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> chains = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> balls = new ArrayList<>();
        private Location pivot;
        private float diskYaw = 0f;
        private boolean acceleratedRotation = false;

        public TriPendulumFan(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tri_pendulum_fan", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(260.0);
            config.setDamageRadius(2.2);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(240);
            config.setCooldownTicks(260);
            config.setChance(5);
            config.setEnabled(true);
            config.setDesignType("Tri-sector fan (read which sector is safe)");
        }

        @Override
        protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            World w = c.getWorld();
            if (w == null) return;
            pivot = c.clone().add(0, 9.0, 0);

            // 3 POLISHED_BLACKSTONE disk slabs in a triangle.
            double[][] diskOffsets = {{0, 0, 0.3}, {0.26, 0, -0.15}, {-0.26, 0, -0.15}};
            for (double[] o : diskOffsets) {
                BlockDisplayHandle d = displayBuilder.spawnBlock(pivot.clone().add(o[0], o[1], o[2]), Material.POLISHED_BLACKSTONE);
                d.scale(0f, 0f, 0f).glow(40, 40, 50).interpolation(10, 0);
                spawnedEntities.add(d.entity()); mount.add(d);
                growXYZ(d, 0.7f, 0.08f, 0.7f, 10);
            }
            // 1 IRON hub.
            BlockDisplayHandle hub = displayBuilder.spawnBlock(pivot.clone().add(0, 0.2, 0), Material.IRON_BLOCK);
            hub.scale(0f, 0f, 0f).glow(180, 180, 200).interpolation(10, 2);
            spawnedEntities.add(hub.entity()); mount.add(hub);
            growXYZ(hub, 0.4f, 0.3f, 0.4f, 10);

            // 3 arms.
            int totalBlocks = 4; // mount counts so far
            for (int arm = 0; arm < 3; arm++) {
                List<BlockDisplayHandle> chList = new ArrayList<>();
                List<BlockDisplayHandle> blList = new ArrayList<>();
                double az = arm * (Math.PI * 2.0 / 3.0);
                double sx = Math.cos(az) * 0.35;
                double sz = Math.sin(az) * 0.35;
                // 6 chain links.
                for (int i = 0; i < 6; i++) {
                    double t = (i + 1) / 6.0;
                    Location ll = pivot.clone().add(sx, -t * 3.6, sz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(ll, Material.CHAIN);
                    h.scale(0f, 0f, 0f).glow(80, 80, 90).interpolation(8, 3 + i);
                    spawnedEntities.add(h.entity()); chList.add(h);
                    growXYZ(h, 0.15f, 0.45f, 0.15f, 8);
                    totalBlocks++;
                }
                // 1 IRON core.
                Location ballSeed = pivot.clone().add(sx, -4.0, sz);
                BlockDisplayHandle core = displayBuilder.spawnBlock(ballSeed.clone(), Material.IRON_BLOCK);
                core.scale(0f, 0f, 0f).glow(160, 160, 180).interpolation(13, 9);
                spawnedEntities.add(core.entity()); blList.add(core);
                growBlock(core, 0.65f, 13);
                totalBlocks++;
                // 4 GRAY_CONCRETE poles (last arm has only 3 to land at 36 total).
                int poleCount = (arm == 2) ? 3 : 4;
                for (int p = 0; p < poleCount; p++) {
                    double pa = p * (Math.PI * 2.0 / poleCount);
                    double px = Math.cos(pa) * 0.55;
                    double pz = Math.sin(pa) * 0.55;
                    BlockDisplayHandle pl = displayBuilder.spawnBlock(ballSeed.clone().add(px, 0, pz), Material.GRAY_CONCRETE);
                    pl.scale(0f, 0f, 0f).glow(130, 130, 140).interpolation(12, 11);
                    spawnedEntities.add(pl.entity()); blList.add(pl);
                    growXYZ(pl, 0.28f, 0.28f, 0.28f, 12);
                    totalBlocks++;
                }
                chains.add(chList);
                balls.add(blList);
            }

            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.5f, 0.8f);
            w.spawnParticle(Particle.SMOKE, pivot, 20, 0.4, 0.2, 0.4, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || pivot == null) return;
            World w = c.getWorld();
            if (tick <= 25) return;

            int dur = config.getDurationTicks();
            // Disk rotation rate accelerates at 50% duration.
            double rotRate = Math.toRadians(0.4);
            if (tick > dur / 2 && !acceleratedRotation) {
                acceleratedRotation = true;
                DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 1.3f, 0.8f);
            }
            if (acceleratedRotation) rotRate = Math.toRadians(0.8);
            diskYaw += (float) rotRate;
            for (BlockDisplayHandle m : mount) setRotation(m, diskYaw, 0, 1, 0, 2);

            // All three arms swing in sync, period 50 ticks, starting angles staggered 120°.
            float ballSwing = (float) (Math.PI / 2.0 * Math.sin((tick - 25) * (Math.PI * 2.0 / 50.0)));
            for (int arm = 0; arm < 3; arm++) {
                double az = arm * (Math.PI * 2.0 / 3.0) + diskYaw;
                double cosAz = Math.cos(az);
                double sinAz = Math.sin(az);

                List<BlockDisplayHandle> chList = chains.get(arm);
                for (int i = 0; i < chList.size(); i++) {
                    double t = (i + 1) / 6.0;
                    double r = 3.6 * t;
                    double swingPlanar = Math.sin(ballSwing) * r;
                    double swingY = -Math.cos(ballSwing) * r;
                    double lx = swingPlanar * cosAz;
                    double lz = swingPlanar * sinAz;
                    teleportEntity(chList.get(i), pivot.clone().add(lx, swingY, lz));
                }

                List<BlockDisplayHandle> blList = balls.get(arm);
                double swingPlanar = Math.sin(ballSwing) * 3.6;
                double swingY = -Math.cos(ballSwing) * 3.6;
                Location ballSeed = pivot.clone().add(swingPlanar * cosAz, swingY, swingPlanar * sinAz);
                // First entry = core.
                if (!blList.isEmpty()) teleportEntity(blList.get(0), ballSeed.clone());
                for (int p = 1; p < blList.size(); p++) {
                    double pa = (p - 1) * (Math.PI * 2.0 / (blList.size() - 1));
                    double px = Math.cos(pa) * 0.55;
                    double pz = Math.sin(pa) * 0.55;
                    teleportEntity(blList.get(p), ballSeed.clone().add(px, 0, pz));
                }
                if (tick % 4 == 0) {
                    w.spawnParticle(Particle.CRIT, ballSeed, 3, 0.2, 0.2, 0.2, 0.05);
                    w.spawnParticle(Particle.SMOKE, ballSeed, 2, 0.2, 0.2, 0.2, 0);
                }
            }

            if (tick % 50 == 25) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 0.9f, 0.7f);
            }
            if (tick % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 0.9f, 0.9f);
            }

            // Center = ball 0 (so radius damage applies to that arm's location).
            double az0 = diskYaw;
            double swingPlanar0 = Math.sin(ballSwing) * 3.6;
            double swingY0 = -Math.cos(ballSwing) * 3.6;
            setCenter(pivot.clone().add(swingPlanar0 * Math.cos(az0), swingY0, swingPlanar0 * Math.sin(az0)));

            if (tick == dur - 20) {
                for (List<BlockDisplayHandle> bl : balls) for (BlockDisplayHandle h : bl) shrinkToZero(h, 10);
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_DEATH, 1.2f, 0.7f);
            }
            if (tick == dur - 8) {
                for (List<BlockDisplayHandle> ch : chains) for (BlockDisplayHandle h : ch) shrinkToZero(h, 6);
                for (BlockDisplayHandle h : mount) shrinkToZero(h, 6);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new TriPendulumFan(plugin); }
    }

    // ================================================================
    // #8 — CHAIN SKIP — "The Bouncer"
    // 30 blocks: 1 IRON core + 6 IRON poles + 8 GRAY_CONCRETE corners
    // (sphere) + 8 CHAIN ghost trail + 6 IRON shockwave slabs +
    // 1 NETHERITE detail = 30. Damage-on-impact-only (per bounce).
    // ================================================================
    public static class ChainSkipBouncer extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> ball = new ArrayList<>();
        private final List<BlockDisplayHandle> ghostTrail = new ArrayList<>();
        private final List<BlockDisplayHandle> shockwave = new ArrayList<>();
        private BlockDisplayHandle detail;
        private Location origin;
        private double posX, posY, posZ;
        private double velY = 0, velX = 0;
        private int bounceCount = 0;
        private int ghostIdx = 0;
        private int shockwaveTick = -1;
        private boolean[] impacts = {false, false, false};

        public ChainSkipBouncer(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_skip_bouncer", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(200.0);
            config.setDamageRadius(1.6);
            config.setTicksBetweenDamage(6);
            config.setDamageDelayTicks(10);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(200.0);
            config.setImpactRadius(2.5);
            config.setDurationTicks(180);
            config.setCooldownTicks(220);
            config.setChance(8);
            config.setTracksPlayer(false);
            config.setEnabled(true);
            config.setDesignType("Bouncing chain (read landing pattern)");
        }

        @Override
        protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            World w = c.getWorld();
            if (w == null) return;
            origin = c.clone().add(-3, 10, 0);
            posX = origin.getX(); posY = origin.getY(); posZ = origin.getZ();
            velX = 0.12; velY = -0.1;

            // Sphere: 1 core + 6 poles + 8 corners = 15.
            BlockDisplayHandle core = displayBuilder.spawnBlock(origin.clone(), Material.IRON_BLOCK);
            core.scale(0f, 0f, 0f).glow(180, 180, 200).interpolation(10, 0);
            spawnedEntities.add(core.entity()); ball.add(core);
            growBlock(core, 0.9f, 10);

            double[][] poles = {{0.65, 0, 0}, {-0.65, 0, 0}, {0, 0.65, 0}, {0, -0.65, 0}, {0, 0, 0.65}, {0, 0, -0.65}};
            for (double[] o : poles) {
                BlockDisplayHandle p = displayBuilder.spawnBlock(origin.clone().add(o[0], o[1], o[2]), Material.IRON_BLOCK);
                p.scale(0f, 0f, 0f).glow(200, 200, 220).interpolation(10, 2);
                spawnedEntities.add(p.entity()); ball.add(p);
                growBlock(p, 0.40f, 10);
            }
            double d = 0.55;
            double[][] corners = {{d, d, d}, {d, d, -d}, {d, -d, d}, {d, -d, -d},
                    {-d, d, d}, {-d, d, -d}, {-d, -d, d}, {-d, -d, -d}};
            for (double[] o : corners) {
                BlockDisplayHandle p = displayBuilder.spawnBlock(origin.clone().add(o[0], o[1], o[2]), Material.GRAY_CONCRETE);
                p.scale(0f, 0f, 0f).glow(130, 130, 140).interpolation(10, 3);
                spawnedEntities.add(p.entity()); ball.add(p);
                growBlock(p, 0.28f, 10);
            }
            // 1 NETHERITE detail tag.
            detail = displayBuilder.spawnBlock(origin.clone().add(0, 0.8, 0), Material.NETHERITE_BLOCK);
            detail.scale(0f, 0f, 0f).glow(30, 30, 30).interpolation(10, 4);
            spawnedEntities.add(detail.entity());
            growXYZ(detail, 0.2f, 0.1f, 0.2f, 10);

            // 8 CHAIN ghost trail (hidden initially).
            for (int i = 0; i < 8; i++) {
                BlockDisplayHandle g = displayBuilder.spawnBlock(origin.clone(), Material.CHAIN);
                g.scale(0f, 0f, 0f).glow(80, 80, 90).interpolation(5, 0);
                spawnedEntities.add(g.entity()); ghostTrail.add(g);
            }
            // 6 IRON shockwave slabs (hidden, used per bounce).
            for (int i = 0; i < 6; i++) {
                BlockDisplayHandle s = displayBuilder.spawnBlock(origin.clone(), Material.IRON_BLOCK);
                s.scale(0f, 0f, 0f).glow(220, 220, 230).interpolation(2, 0);
                spawnedEntities.add(s.entity()); shockwave.add(s);
            }

            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 1.2f, 1.1f);
            w.spawnParticle(Particle.FALLING_DUST, origin, 10, 0.5, 0.5, 0.5, 0, Material.IRON_BLOCK.createBlockData());
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || origin == null) return;
            World w = c.getWorld();

            // Physics — gravity + forward.
            velY -= 0.06;
            posY += velY;
            posX += velX;

            double groundY = origin.getY() - 10.0; // assumed ground level

            // Bounce check.
            if (posY <= groundY && bounceCount < 3) {
                posY = groundY;
                if (bounceCount == 0) velY = 0.65;
                else if (bounceCount == 1) velY = 0.40;
                else velY = 0.20;
                velX += 0.18;
                // Trigger impact + shockwave at this position.
                Location impactLoc = new Location(w, posX, posY, posZ);
                if (!impacts[bounceCount]) {
                    impacts[bounceCount] = true;
                    triggerImpactDamage(impactLoc);
                }
                spawnShockwaveAt(impactLoc, bounceCount);
                placeGhostChainAt(impactLoc);
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.0f - bounceCount * 0.2f, 0.7f + bounceCount * 0.1f);
                w.spawnParticle(Particle.BLOCK, impactLoc, 25, 0.6, 0.1, 0.6, 0.1, Material.IRON_BLOCK.createBlockData());
                w.spawnParticle(Particle.CRIT, impactLoc, 15, 0.5, 0.3, 0.5, 0.3);
                shockwaveTick = tick;
                bounceCount++;
            }

            // Update ball parts.
            Location ballSeed = new Location(w, posX, posY, posZ);
            double[][] poles = {{0, 0, 0}, {0.65, 0, 0}, {-0.65, 0, 0}, {0, 0.65, 0}, {0, -0.65, 0}, {0, 0, 0.65}, {0, 0, -0.65}};
            double d = 0.55;
            double[][] corners = {{d, d, d}, {d, d, -d}, {d, -d, d}, {d, -d, -d},
                    {-d, d, d}, {-d, d, -d}, {-d, -d, d}, {-d, -d, -d}};
            int idx = 0;
            for (double[] o : poles) {
                if (idx < ball.size()) teleportEntity(ball.get(idx), ballSeed.clone().add(o[0], o[1], o[2]));
                idx++;
            }
            for (double[] o : corners) {
                if (idx < ball.size()) teleportEntity(ball.get(idx), ballSeed.clone().add(o[0], o[1], o[2]));
                idx++;
            }
            if (detail != null) teleportEntity(detail, ballSeed.clone().add(0, 0.8, 0));

            // Trail particles during flight.
            if (tick % 2 == 0) {
                w.spawnParticle(Particle.FALLING_DUST, ballSeed, 4, 0.3, 0.3, 0.3, 0, Material.IRON_BLOCK.createBlockData());
            }
            if (tick % 6 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.5f, 1.4f);
            }

            // Expand shockwave outward after bounce.
            if (shockwaveTick >= 0 && tick - shockwaveTick < 8) {
                double prog = (tick - shockwaveTick) / 8.0;
                double r = prog * 2.5;
                Location lastBounce = new Location(w, posX, groundY, posZ);
                for (int i = 0; i < shockwave.size(); i++) {
                    double a = (Math.PI * 2.0 * i) / shockwave.size();
                    Location sp = lastBounce.clone().add(Math.cos(a) * r, 0.05, Math.sin(a) * r);
                    teleportEntity(shockwave.get(i), sp);
                }
            } else if (shockwaveTick >= 0 && tick - shockwaveTick == 9) {
                for (BlockDisplayHandle h : shockwave) shrinkToZero(h, 3);
                shockwaveTick = -1;
            }

            setCenter(ballSeed);

            int dur = config.getDurationTicks();
            if (tick == dur - 12) {
                for (BlockDisplayHandle h : ball) shrinkToZero(h, 8);
                if (detail != null) shrinkToZero(detail, 8);
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.2f, 0.5f);
            }
            if (tick == dur - 4) {
                for (BlockDisplayHandle h : ghostTrail) shrinkToZero(h, 4);
                for (BlockDisplayHandle h : shockwave) shrinkToZero(h, 4);
            }
        }

        private void spawnShockwaveAt(Location at, int bounce) {
            for (int i = 0; i < shockwave.size(); i++) {
                double a = (Math.PI * 2.0 * i) / shockwave.size();
                Location sp = at.clone().add(Math.cos(a) * 0.1, 0.05, Math.sin(a) * 0.1);
                teleportEntity(shockwave.get(i), sp);
                growXYZ(shockwave.get(i), 0.5f, 0.04f, 0.08f, 2);
            }
        }

        private void placeGhostChainAt(Location at) {
            if (ghostTrail.isEmpty()) return;
            BlockDisplayHandle g = ghostTrail.get(ghostIdx);
            teleportEntity(g, at.clone().add(0, 0.5, 0));
            growXYZ(g, 0.12f, 0.55f, 0.12f, 1);
            ghostIdx = (ghostIdx + 1) % ghostTrail.size();
            // Fade older one.
            int prev = (ghostIdx + ghostTrail.size() - 2) % ghostTrail.size();
            shrinkToZero(ghostTrail.get(prev), 12);
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ChainSkipBouncer(plugin); }
    }

    // ================================================================
    // #9 — INVERTED PENDULUM — "The Underswing"
    // 30 blocks: 1 IRON ground anchor + 2 CHAIN fittings + 10 CHAIN cable
    // + 1 NETHERITE core + 6 NETHERITE poles + 8 IRON corners +
    // 2 IRON tension brackets = 30 total.
    // ================================================================
    public static class InvertedPendulum extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> anchorParts = new ArrayList<>();
        private final List<BlockDisplayHandle> cable = new ArrayList<>();
        private final List<BlockDisplayHandle> ballParts = new ArrayList<>();
        private final List<BlockDisplayHandle> brackets = new ArrayList<>();
        private Location groundAnchor;
        private float angleRad = 0f;
        private boolean lastDirNeg = false;

        public InvertedPendulum(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("inverted_pendulum", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(240.0);
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(15);
            config.setDurationTicks(220);
            config.setCooldownTicks(240);
            config.setChance(7);
            config.setEnabled(true);
            config.setDesignType("Inverted pendulum (jump over)");
        }

        @Override
        protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            World w = c.getWorld();
            if (w == null) return;
            groundAnchor = c.clone().add(0, 0.5, 0);
            angleRad = (float) Math.toRadians(-55);

            // Ground anchor IRON.
            BlockDisplayHandle a = displayBuilder.spawnBlock(groundAnchor.clone(), Material.IRON_BLOCK);
            a.scale(0f, 0f, 0f).glow(180, 180, 200).interpolation(10, 0);
            spawnedEntities.add(a.entity()); anchorParts.add(a);
            growXYZ(a, 0.5f, 0.5f, 0.5f, 10);

            // 2 CHAIN fitting cubes beside it.
            for (int i = 0; i < 2; i++) {
                double xo = (i == 0) ? -0.4 : 0.4;
                BlockDisplayHandle ch = displayBuilder.spawnBlock(groundAnchor.clone().add(xo, 0, 0), Material.CHAIN);
                ch.scale(0f, 0f, 0f).glow(80, 80, 90).interpolation(10, 2);
                spawnedEntities.add(ch.entity()); anchorParts.add(ch);
                growXYZ(ch, 0.15f, 0.15f, 0.15f, 10);
            }

            // 10 CHAIN cable links along cable.
            for (int i = 0; i < 10; i++) {
                double t = (i + 1) / 10.0;
                Location ll = groundAnchor.clone().add(0, t * 1.5, 0);
                BlockDisplayHandle ch = displayBuilder.spawnBlock(ll, Material.CHAIN);
                ch.scale(0f, 0f, 0f).glow(80, 80, 90).interpolation(8, 3 + i);
                spawnedEntities.add(ch.entity()); cable.add(ch);
                growXYZ(ch, 0.17f, 0.45f, 0.17f, 8);
            }

            // 2 tension brackets (visible cable tension indicators).
            for (int i = 0; i < 2; i++) {
                double xo = (i == 0) ? -0.2 : 0.2;
                BlockDisplayHandle b = displayBuilder.spawnBlock(groundAnchor.clone().add(xo, 0.8, 0), Material.IRON_BLOCK);
                b.scale(0f, 0f, 0f).glow(220, 220, 240).interpolation(10, 6);
                spawnedEntities.add(b.entity()); brackets.add(b);
                growXYZ(b, 0.1f, 0.15f, 0.1f, 10);
            }

            // 15-display ball at ball seed height (1.6m above ground).
            Location ballSeed = groundAnchor.clone().add(0, 1.6, 0);
            BlockDisplayHandle core = displayBuilder.spawnBlock(ballSeed.clone(), Material.NETHERITE_BLOCK);
            core.scale(0f, 0f, 0f).glow(30, 30, 30).interpolation(15, 16);
            spawnedEntities.add(core.entity()); ballParts.add(core);
            growBlock(core, 0.85f, 15);
            double[][] poles = {{0.6, 0, 0}, {-0.6, 0, 0}, {0, 0.6, 0}, {0, -0.6, 0}, {0, 0, 0.6}, {0, 0, -0.6}};
            for (double[] o : poles) {
                BlockDisplayHandle p = displayBuilder.spawnBlock(ballSeed.clone().add(o[0], o[1], o[2]), Material.NETHERITE_BLOCK);
                p.scale(0f, 0f, 0f).glow(40, 40, 40).interpolation(14, 18);
                spawnedEntities.add(p.entity()); ballParts.add(p);
                growBlock(p, 0.38f, 14);
            }
            double d = 0.5;
            double[][] corners = {{d, d, d}, {d, d, -d}, {d, -d, d}, {d, -d, -d},
                    {-d, d, d}, {-d, d, -d}, {-d, -d, d}, {-d, -d, -d}};
            for (double[] o : corners) {
                BlockDisplayHandle p = displayBuilder.spawnBlock(ballSeed.clone().add(o[0], o[1], o[2]), Material.IRON_BLOCK);
                p.scale(0f, 0f, 0f).glow(190, 190, 210).interpolation(14, 20);
                spawnedEntities.add(p.entity()); ballParts.add(p);
                growBlock(p, 0.27f, 14);
            }

            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.4f, 1.2f);
            DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_PLACE, 1.4f, 0.7f);
            w.spawnParticle(Particle.SOUL_FIRE_FLAME, groundAnchor, 15, 0.3, 0.1, 0.3, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || groundAnchor == null) return;
            World w = c.getWorld();
            if (tick <= 25) return;

            // Pendulum swings ±55° from vertical, period 36t.
            float t = (tick - 25) * (float) (Math.PI * 2.0 / 36.0);
            float newAngle = (float) (Math.toRadians(55) * Math.sin(t));
            boolean dirNeg = (newAngle - angleRad) < 0;
            if (dirNeg != lastDirNeg && Math.abs(newAngle) > 0.8 && tick > 30) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 1.0f, 1.3f);
                w.spawnParticle(Particle.CRIT, getBallPos(), 10, 0.3, 0.2, 0.3, 0.15);
                lastDirNeg = dirNeg;
            }
            angleRad = newAngle;

            // Update cable + ball.
            for (int i = 0; i < cable.size(); i++) {
                double tt = (i + 1) / 10.0;
                double r = 2.0 * tt;
                double lx = Math.sin(angleRad) * r;
                double ly = Math.cos(angleRad) * r;
                Location ll = groundAnchor.clone().add(lx, ly, 0);
                teleportEntity(cable.get(i), ll);
            }
            Location ballSeed = getBallPos();
            double[][] poles = {{0, 0, 0}, {0.6, 0, 0}, {-0.6, 0, 0}, {0, 0.6, 0}, {0, -0.6, 0}, {0, 0, 0.6}, {0, 0, -0.6}};
            double d = 0.5;
            double[][] corners = {{d, d, d}, {d, d, -d}, {d, -d, d}, {d, -d, -d},
                    {-d, d, d}, {-d, d, -d}, {-d, -d, d}, {-d, -d, -d}};
            int idx = 0;
            for (double[] o : poles) {
                if (idx < ballParts.size()) teleportEntity(ballParts.get(idx), ballSeed.clone().add(o[0], o[1], o[2]));
                idx++;
            }
            for (double[] o : corners) {
                if (idx < ballParts.size()) teleportEntity(ballParts.get(idx), ballSeed.clone().add(o[0], o[1], o[2]));
                idx++;
            }

            if (tick % 3 == 0) {
                w.spawnParticle(Particle.BLOCK, ballSeed, 3, 0.3, 0.3, 0.3, 0.05, Material.NETHERITE_BLOCK.createBlockData());
            }
            if (tick % 8 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.7f, 1.3f);
            }
            if (tick % 18 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_STEP, 0.7f, 1.0f);
            }
            setCenter(ballSeed);

            int dur = config.getDurationTicks();
            if (tick == dur - 16) {
                for (BlockDisplayHandle h : ballParts) shrinkToZero(h, 10);
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.3f, 0.6f);
            }
            if (tick == dur - 4) {
                for (BlockDisplayHandle h : cable) shrinkToZero(h, 4);
                for (BlockDisplayHandle h : anchorParts) shrinkToZero(h, 4);
                for (BlockDisplayHandle h : brackets) shrinkToZero(h, 4);
            }
        }

        private Location getBallPos() {
            double r = 2.0; // chain length
            double bx = Math.sin(angleRad) * r;
            double by = Math.cos(angleRad) * r; // UPWARD (inverted)
            return groundAnchor.clone().add(bx, by, 0);
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new InvertedPendulum(plugin); }
    }

    // ================================================================
    // #10 — WRECKING BALL ORBIT — "The Satellite"
    // 30 blocks: 1 POLISHED_BLACKSTONE post + 10 CHAIN cable +
    // 1 NETHERITE core + 6 NETHERITE poles + 8 IRON corners +
    // 4 CHAIN velocity-indicator slabs = 30 total.
    // ================================================================
    public static class WreckingBallOrbit extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> cable = new ArrayList<>();
        private final List<BlockDisplayHandle> ballParts = new ArrayList<>();
        private final List<BlockDisplayHandle> trailSlabs = new ArrayList<>();
        private BlockDisplayHandle post;
        private Location orbitCenter;
        private double angleRad = 0f;
        private float ballSpin = 0f;
        private static final double ORBIT_RADIUS = 4.0;

        public WreckingBallOrbit(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("wrecking_ball_orbit", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(280.0);
            config.setDamageRadius(2.6);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(25);
            config.setDurationTicks(280);
            config.setCooldownTicks(280);
            config.setChance(5);
            config.setTracksPlayer(false); // we sample player position ourselves at 4t cadence
            config.setEnabled(true);
            config.setDesignType("Orbiting satellite (find orbit gap)");
        }

        @Override
        protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            World w = c.getWorld();
            if (w == null) return;
            orbitCenter = c.clone();
            angleRad = 0;

            post = displayBuilder.spawnBlock(orbitCenter.clone().add(0, 8.0, 0), Material.POLISHED_BLACKSTONE);
            post.scale(0f, 0f, 0f).glow(40, 40, 50).interpolation(10, 0);
            spawnedEntities.add(post.entity());
            growXYZ(post, 0.25f, 2.0f, 0.25f, 10);

            // 10 CHAIN cable links from post to ball (will recalculate each update).
            Location ballStart = orbitCenter.clone().add(ORBIT_RADIUS, 3.0, 0);
            for (int i = 0; i < 10; i++) {
                double t = (i + 1) / 10.0;
                double lx = (ballStart.getX() - orbitCenter.getX()) * t;
                double ly = 8.0 - t * 5.0;
                Location ll = orbitCenter.clone().add(lx, ly, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(ll, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(80, 80, 90).interpolation(8, 1 + i);
                spawnedEntities.add(h.entity()); cable.add(h);
                growXYZ(h, 0.18f, 0.5f, 0.18f, 8);
            }

            // 15-display ball.
            BlockDisplayHandle core = displayBuilder.spawnBlock(ballStart.clone(), Material.NETHERITE_BLOCK);
            core.scale(0f, 0f, 0f).glow(30, 30, 30).interpolation(15, 12);
            spawnedEntities.add(core.entity()); ballParts.add(core);
            growBlock(core, 0.9f, 15);
            double[][] poles = {{0.65, 0, 0}, {-0.65, 0, 0}, {0, 0.65, 0}, {0, -0.65, 0}, {0, 0, 0.65}, {0, 0, -0.65}};
            for (double[] o : poles) {
                BlockDisplayHandle p = displayBuilder.spawnBlock(ballStart.clone().add(o[0], o[1], o[2]), Material.NETHERITE_BLOCK);
                p.scale(0f, 0f, 0f).glow(40, 40, 40).interpolation(14, 14);
                spawnedEntities.add(p.entity()); ballParts.add(p);
                growBlock(p, 0.42f, 14);
            }
            double d = 0.55;
            double[][] corners = {{d, d, d}, {d, d, -d}, {d, -d, d}, {d, -d, -d},
                    {-d, d, d}, {-d, d, -d}, {-d, -d, d}, {-d, -d, -d}};
            for (double[] o : corners) {
                BlockDisplayHandle p = displayBuilder.spawnBlock(ballStart.clone().add(o[0], o[1], o[2]), Material.IRON_BLOCK);
                p.scale(0f, 0f, 0f).glow(180, 180, 200).interpolation(14, 16);
                spawnedEntities.add(p.entity()); ballParts.add(p);
                growBlock(p, 0.29f, 14);
            }

            // 4 CHAIN trail slabs behind the ball.
            for (int i = 0; i < 4; i++) {
                BlockDisplayHandle t = displayBuilder.spawnBlock(ballStart.clone(), Material.CHAIN);
                t.scale(0f, 0f, 0f).glow(110, 110, 120).interpolation(5, 18);
                spawnedEntities.add(t.entity()); trailSlabs.add(t);
                growXYZ(t, 0.1f, 0.04f, 0.4f, 5);
            }

            DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_PLACE, 1.5f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.4f, 0.8f);
            w.spawnParticle(Particle.LAVA, post.entity().getLocation(), 4, 0.3, 0.3, 0.3, 0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || orbitCenter == null) return;
            World w = c.getWorld();
            if (tick <= 25) return;

            // Sample player position every 4 ticks and update orbit center.
            if (tick % 4 == 0) {
                Player p = findNearestPlayer(orbitCenter, 30.0);
                if (p != null) {
                    Location target = p.getLocation();
                    orbitCenter = new Location(w, target.getX(), target.getY(), target.getZ());
                }
                angleRad += Math.toRadians(8); // 8° per 4-tick step
            }
            ballSpin += (float) Math.toRadians(10);

            // Update post (keep it overhead of orbit center).
            if (post != null) {
                teleportEntity(post, orbitCenter.clone().add(0, 8.0, 0));
            }

            // Ball world position.
            double bx = Math.cos(angleRad) * ORBIT_RADIUS;
            double bz = Math.sin(angleRad) * ORBIT_RADIUS;
            Location ballSeed = orbitCenter.clone().add(bx, 3.0, bz);

            // Cable: 10 links from post (above orbitCenter) straight-curved out to ballSeed.
            Location top = orbitCenter.clone().add(0, 8.0, 0);
            for (int i = 0; i < cable.size(); i++) {
                double t = (i + 1) / 10.0;
                // Slight centrifugal arc (push outward as t grows).
                double arc = Math.sin(t * Math.PI) * 0.6;
                double lx = top.getX() + (ballSeed.getX() - top.getX()) * t + Math.cos(angleRad) * arc;
                double ly = top.getY() + (ballSeed.getY() - top.getY()) * t;
                double lz = top.getZ() + (ballSeed.getZ() - top.getZ()) * t + Math.sin(angleRad) * arc;
                teleportEntity(cable.get(i), new Location(w, lx, ly, lz));
            }

            // Ball parts.
            double[][] poles = {{0, 0, 0}, {0.65, 0, 0}, {-0.65, 0, 0}, {0, 0.65, 0}, {0, -0.65, 0}, {0, 0, 0.65}, {0, 0, -0.65}};
            double d = 0.55;
            double[][] corners = {{d, d, d}, {d, d, -d}, {d, -d, d}, {d, -d, -d},
                    {-d, d, d}, {-d, d, -d}, {-d, -d, d}, {-d, -d, -d}};
            int idx = 0;
            for (double[] o : poles) {
                if (idx < ballParts.size()) {
                    teleportEntity(ballParts.get(idx), ballSeed.clone().add(o[0], o[1], o[2]));
                    if (idx > 0) setRotation(ballParts.get(idx), ballSpin, 0, 1, 0, 2);
                }
                idx++;
            }
            for (double[] o : corners) {
                if (idx < ballParts.size()) teleportEntity(ballParts.get(idx), ballSeed.clone().add(o[0], o[1], o[2]));
                idx++;
            }

            // Trail slabs behind the ball.
            for (int i = 0; i < trailSlabs.size(); i++) {
                double trailAngle = angleRad - Math.toRadians(12 + i * 8);
                double tx = Math.cos(trailAngle) * ORBIT_RADIUS;
                double tz = Math.sin(trailAngle) * ORBIT_RADIUS;
                Location tloc = orbitCenter.clone().add(tx, 3.0, tz);
                teleportEntity(trailSlabs.get(i), tloc);
                setRotation(trailSlabs.get(i), (float) trailAngle, 0, 1, 0, 4);
            }

            if (tick % 4 == 0) {
                w.spawnParticle(Particle.SMOKE, ballSeed, 2, 0.2, 0.2, 0.2, 0.02);
                w.spawnParticle(Particle.BLOCK, ballSeed, 2, 0.2, 0.2, 0.2, 0.02, Material.NETHERITE_BLOCK.createBlockData());
            }
            if (tick % 12 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.6f, 0.9f);
            }
            // Half-orbit step sound (every ~22 ticks since 22 * 8° ≈ 180°).
            if (tick % 22 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_STEP, 0.8f, 0.9f);
            }

            setCenter(ballSeed);

            int dur = config.getDurationTicks();
            if (tick == dur - 18) {
                for (BlockDisplayHandle h : ballParts) shrinkToZero(h, 10);
                for (BlockDisplayHandle h : trailSlabs) shrinkToZero(h, 10);
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.4f, 0.5f);
            }
            if (tick == dur - 6) {
                for (BlockDisplayHandle h : cable) shrinkToZero(h, 5);
                if (post != null) shrinkToZero(post, 5);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new WreckingBallOrbit(plugin); }
    }
}
