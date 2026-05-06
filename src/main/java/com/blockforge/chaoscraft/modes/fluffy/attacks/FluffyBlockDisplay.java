package com.blockforge.chaoscraft.modes.fluffy.attacks;

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
 * Fluffy Mode — BLOCK DISPLAY ATTACKS 1-10 (Paws, Toys, Impact)
 *
 * Cute/fluffy themed structures that are out to kill you. Pure random attack
 * survival. Each attack uses 25+ block displays animated via Transformation
 * (AxisAngle4f rotation, Vector3f scale/translation). Never teleport for
 * rotation — use setInterpolationDuration for smooth animation.
 *
 * Fluffy palette:
 *  - Pure white wool: RGB(245, 245, 245)
 *  - Bubblegum pink: RGB(255, 100, 180)
 *  - Soft orange teddy: RGB(220, 140, 80)
 *  - Pastel blue: RGB(140, 200, 240)
 *  - Sunshine gold: RGB(255, 215, 90)
 *
 * Materials: WHITE_WOOL, PINK_CONCRETE, ORANGE_WOOL, LIGHT_BLUE_WOOL,
 *            BLACK_CONCRETE, GOLD_BLOCK, COPPER_BLOCK, BIRCH_PLANKS,
 *            CHERRY_PLANKS, RED_WOOL, DARK_OAK_PLANKS, BONE_BLOCK,
 *            WHITE_CONCRETE
 *
 * Particles: HEART, CRIT, ENCHANT, FIREWORK_SPARK, CLOUD, FLAME,
 *            SMOKE, SNOWFLAKE, LANDING_LAVA
 * Sounds: ENTITY_GENERIC_BIG_FALL, ENTITY_ZOMBIE_BREAK_WOODEN_DOOR,
 *         ENTITY_SLIME_SQUISH_SMALL, BLOCK_WOOL_PLACE, ENTITY_TNT_PRIMED,
 *         ENTITY_GENERIC_EXPLODE, BLOCK_TRIPWIRE_ATTACH,
 *         BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, ENTITY_FIREWORK_ROCKET_BLAST,
 *         ENTITY_CAT_STEP, ENTITY_SPIDER_STEP, ENTITY_IRON_GOLEM_ATTACK
 *
 * Attacks:
 *  1. GiantPawSlam        — 26 blocks, 5-toed paw drops + curls, impact-only
 *  2. TeddyBearCrush      — 45 blocks, seated teddy slams arms shut, impact-only
 *  3. GiantYarnBall       — 40 blocks, orbiting rings drift toward player, constant
 *  4. BountyBomb          — 26 blocks, fuse-burning bomb sphere, impact-only
 *  5. CatCradleNet        — 42 blocks, post + beam tightening cradle, constant
 *  6. WindUpKey           — 26 blocks, embedded key rotating on shaft, constant
 *  7. GinormousLollipop   — 26 blocks, spinning candy + stick falls, impact-only
 *  8. PawPrintField       — 42 blocks, 6 pawprints rise & breathe, constant
 *  9. PlushtrapSpider     — 42 blocks, plush body + 8 unfolding legs, impact-only
 * 10. BunnyEarBlades      — 26 blocks, bunny ears scissor shut, impact-only
 */
public final class FluffyBlockDisplay {
    private FluffyBlockDisplay() {}

    private static final String MODE_PATH = "modes/fluffy/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new GiantPawSlam(plugin));
        registry.register(new TeddyBearCrush(plugin));
        registry.register(new GiantYarnBall(plugin));
        registry.register(new BountyBomb(plugin));
        registry.register(new CatCradleNet(plugin));
        registry.register(new WindUpKey(plugin));
        registry.register(new GinormousLollipop(plugin));
        registry.register(new PawPrintField(plugin));
        registry.register(new PlushtrapSpider(plugin));
        registry.register(new BunnyEarBlades(plugin));
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
    // #1 — GIANT PAW SLAM
    // 26 blocks: 8 pad (WHITE_WOOL flat oblate), 12 toe segments (4 toes × 3
    // tapered), 2 thumb blocks, 4 toe joints. Drops from Y+20 with accelerating
    // fall, toes curl inward during fall. Impact-only 5.5r, 26 hearts.
    // ================================================================
    public static class GiantPawSlam extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> padBlocks = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> toeColumns = new ArrayList<>();
        private final List<BlockDisplayHandle> thumbBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> jointBlocks = new ArrayList<>();
        private boolean impacted = false;
        private float toeCurl = 0f;

        public GiantPawSlam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("giant_paw_slam", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(26.0);
            config.setImpactRadius(5.5);
            config.setDamage(0);
            config.setDurationTicks(180);
            config.setCooldownTicks(220);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Main pad: 8 WHITE_WOOL blocks scaled flat (2.5x0.6x3.0), arranged in oblate disc
            double[][] padOffsets = {
                    {-1.2, 0, -0.8}, {0.0, 0, -1.0}, {1.2, 0, -0.8},
                    {-1.5, 0, 0.5},  {0.0, 0, 0.7},  {1.5, 0, 0.5},
                    {-0.8, 0, 1.4},  {0.8, 0, 1.4}
            };
            for (double[] off : padOffsets) {
                Location loc = center.clone().add(off[0], 20, off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_WOOL);
                h.scale(1.0f, 0.6f, 1.0f).glow(245, 245, 245).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                padBlocks.add(h);
            }

            // 4 toes, each 3-block column (tapering scale 0.9 -> 0.7 -> 0.5)
            double[][] toeBases = {{-1.6, 0, -1.6}, {-0.6, 0, -2.0}, {0.6, 0, -2.0}, {1.6, 0, -1.6}};
            for (double[] base : toeBases) {
                List<BlockDisplayHandle> column = new ArrayList<>();
                for (int s = 0; s < 3; s++) {
                    Location loc = center.clone().add(base[0], 20 + 0.3 + s * 0.5, base[2]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_WOOL);
                    float scaleXY = 0.9f - s * 0.2f;
                    h.scale(scaleXY, 0.55f, scaleXY).glow(245, 245, 245).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    column.add(h);
                }
                toeColumns.add(column);
            }

            // Thumb: 2 PINK_CONCRETE blocks on the side
            for (int t = 0; t < 2; t++) {
                Location loc = center.clone().add(2.2, 20 + 0.2 + t * 0.4, 0.4 + t * 0.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PINK_CONCRETE);
                h.scale(0.7f, 0.55f, 0.7f).glow(255, 100, 180).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                thumbBlocks.add(h);
            }

            // Toe joints: 4 PINK_CONCRETE pads at base of each toe (paw beans)
            for (double[] base : toeBases) {
                Location loc = center.clone().add(base[0], 20 + 0.05, base[2] + 0.4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PINK_CONCRETE);
                h.scale(0.5f, 0.3f, 0.5f).glow(255, 100, 180).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                jointBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_BIG_FALL, 1.0f, 0.6f);
            w.spawnParticle(Particle.CLOUD, center.clone().add(0, 18, 0), 20, 1, 0.5, 1, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Falling phase: ticks 0-30. Y offsets: -2, -4, -7, -11, -16, -20 (accelerating).
            // We translate all blocks down via Transformation translation, interpolating each step.
            if (tick < 30 && !impacted) {
                // Accelerating drop: target Y offset relative to spawn (which is +20 above ground).
                // We progressively translate blocks DOWN by an additional negative Y offset.
                int step = tick / 5; // 0..5
                float[] dropTable = {-2f, -4f, -7f, -11f, -16f, -20f};
                if (tick % 5 == 0 && step < dropTable.length) {
                    float dropY = dropTable[step];
                    // Curl toes inward as we fall (0 -> 25 deg over the drop)
                    toeCurl = (float) Math.toRadians(25.0 * (step / 5.0));
                    applyDrop(dropY, toeCurl);

                    // Trail particles
                    c.getWorld().spawnParticle(Particle.CLOUD, c.clone().add(0, 12, 0), 8, 1.5, 0.5, 1.5, 0.05);
                }

                // Trigger impact at end of fall
                if (tick == 28) {
                    Location impactLoc = c.clone();
                    triggerImpactDamage(impactLoc);
                    impacted = true;
                    DisplayBuilder.playSound(c, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.5f, 0.3f);
                    c.getWorld().spawnParticle(Particle.CRIT, c.clone().add(0, 0.3, 0), 60, 4, 0.3, 4, 0.5);
                    DisplayBuilder.dustParticles(c.clone().add(0, 0.3, 0), 80, 5.0, 245, 245, 245, 1.6f);
                    c.getWorld().spawnParticle(Particle.CLOUD, c.clone().add(0, 0.5, 0), 50, 5, 1, 5, 0.1);
                }
            }

            // Active phase: toes flex slowly (10 deg cycle)
            if (tick > 30 && tick < 130) {
                if (tick % 4 == 0) {
                    float flex = (float) Math.toRadians(25.0 + 10.0 * Math.sin(tick * 0.1));
                    applyDrop(-20f, flex);
                }
                if (tick % 6 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 0.3, 0), 6, 3.0, 245, 245, 245, 1.0f);
                }
            }

            // Lift phase: ticks 130-180 — paw rises back to Y+20
            if (tick >= 130 && tick < 175) {
                int liftStep = (tick - 130) / 9; // 0..5
                float[] liftTable = {-16f, -11f, -7f, -4f, -2f, 0f};
                if ((tick - 130) % 9 == 0 && liftStep < liftTable.length) {
                    applyDrop(liftTable[liftStep], toeCurl);
                }
            }
        }

        private void applyDrop(float dropY, float curlAngle) {
            // Pad: just drop translation Y
            for (BlockDisplayHandle h : padBlocks) {
                applyTranslate(h.entity(), 0f, dropY, 0f);
            }
            // Toes: drop + X-axis curl rotation
            for (List<BlockDisplayHandle> col : toeColumns) {
                for (BlockDisplayHandle h : col) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(5);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(-0.5f, dropY - 0.5f, -0.5f),
                            new AxisAngle4f(curlAngle, 1f, 0f, 0f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }
            for (BlockDisplayHandle h : thumbBlocks) applyTranslate(h.entity(), 0f, dropY, 0f);
            for (BlockDisplayHandle h : jointBlocks) applyTranslate(h.entity(), 0f, dropY, 0f);
        }

        private void applyTranslate(BlockDisplay e, float x, float y, float z) {
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(5);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    new Vector3f(x - 0.5f, y - 0.5f, z - 0.5f),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    t.getScale(),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new GiantPawSlam(plugin); }
    }

    // ================================================================
    // #2 — TEDDY BEAR CRUSH
    // 45 blocks: head (8 ORANGE_WOOL oblate sphere), 2 ears (3 ea = 6),
    // body (14 ORANGE_WOOL oval), 2 arms (4 ea = 8), 2 legs (3 ea = 6),
    // belly patch (3 LIGHT_BLUE_WOOL). Materializes piece by piece, arms
    // open then SNAP shut. Impact-only 3.5r, 22 hearts when arms close.
    // ================================================================
    public static class TeddyBearCrush extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> leftArm = new ArrayList<>();
        private final List<BlockDisplayHandle> rightArm = new ArrayList<>();
        private final List<BlockDisplayHandle> head = new ArrayList<>();
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private boolean armsClosed = false;
        private float headTilt = 0f;

        public TeddyBearCrush(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("teddy_bear_crush", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(22.0);
            config.setImpactRadius(3.5);
            config.setDamage(0);
            config.setDurationTicks(220);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Body: 14 ORANGE_WOOL blocks, oval barrel, Y=0..2
            double[][] bodyOffsets = {
                    {-1.1, 0.5, 0}, {-0.6, 0.0, 0}, {0.0, 0.0, 0}, {0.6, 0.0, 0}, {1.1, 0.5, 0},
                    {-1.1, 1.5, 0}, {-0.6, 2.0, 0}, {0.0, 2.0, 0}, {0.6, 2.0, 0}, {1.1, 1.5, 0},
                    {-0.5, 1.0, 0.6}, {0.5, 1.0, 0.6}, {-0.5, 1.0, -0.6}, {0.5, 1.0, -0.6}
            };
            for (double[] off : bodyOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ORANGE_WOOL);
                h.scale(0.0f, 0.0f, 0.0f).glow(220, 140, 80).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
                // Schedule grow-in
                growBlock(h, 1.0f, 12);
            }

            // Belly patch: 3 LIGHT_BLUE_WOOL on chest
            double[][] bellyOffsets = {{-0.3, 1.0, 0.7}, {0.3, 1.0, 0.7}, {0.0, 0.7, 0.7}};
            for (double[] off : bellyOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIGHT_BLUE_WOOL);
                h.scale(0.0f, 0.0f, 0.0f).glow(140, 200, 240).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
                growBlock(h, 0.5f, 14);
            }

            // Head: 8 ORANGE_WOOL blocks oblate sphere at Y=3
            double[][] headOffsets = {
                    {-0.7, 3.0, 0}, {0.7, 3.0, 0}, {0, 3.0, 0.7}, {0, 3.0, -0.7},
                    {-0.4, 3.6, 0.4}, {0.4, 3.6, 0.4}, {-0.4, 3.6, -0.4}, {0.4, 3.6, -0.4}
            };
            for (double[] off : headOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ORANGE_WOOL);
                h.scale(0.0f, 0.0f, 0.0f).glow(220, 140, 80).interpolation(6, 0);
                spawnedEntities.add(h.entity());
                head.add(h);
                allBlocks.add(h);
                growBlock(h, 0.85f, 8);
            }

            // Ears: 2 sets of 3 ORANGE_WOOL
            double[][] leftEar = {{-0.9, 4.2, 0.0}, {-1.0, 4.6, 0.1}, {-0.7, 4.6, -0.1}};
            double[][] rightEar = {{0.9, 4.2, 0.0}, {1.0, 4.6, 0.1}, {0.7, 4.6, -0.1}};
            for (double[] off : leftEar) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ORANGE_WOOL);
                h.scale(0.0f, 0.0f, 0.0f).glow(220, 140, 80).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
                growBlock(h, 0.55f, 12);
            }
            for (double[] off : rightEar) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ORANGE_WOOL);
                h.scale(0.0f, 0.0f, 0.0f).glow(220, 140, 80).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
                growBlock(h, 0.55f, 12);
            }

            // Left arm: 4 ORANGE_WOOL angled outward
            for (int s = 0; s < 4; s++) {
                Location loc = center.clone().add(-1.6 - s * 0.45, 1.6 - s * 0.1, 0.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ORANGE_WOOL);
                h.scale(0.0f, 0.0f, 0.0f).glow(220, 140, 80).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                leftArm.add(h);
                allBlocks.add(h);
                growBlock(h, 0.6f, 14);
            }
            // Right arm
            for (int s = 0; s < 4; s++) {
                Location loc = center.clone().add(1.6 + s * 0.45, 1.6 - s * 0.1, 0.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ORANGE_WOOL);
                h.scale(0.0f, 0.0f, 0.0f).glow(220, 140, 80).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                rightArm.add(h);
                allBlocks.add(h);
                growBlock(h, 0.6f, 14);
            }

            // Legs: 2x3 stub legs
            for (int s = 0; s < 3; s++) {
                Location loc = center.clone().add(-0.6, -0.4 - s * 0.3, 0.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ORANGE_WOOL);
                h.scale(0.0f, 0.0f, 0.0f).glow(220, 140, 80).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
                growBlock(h, 0.65f, 14);
            }
            for (int s = 0; s < 3; s++) {
                Location loc = center.clone().add(0.6, -0.4 - s * 0.3, 0.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ORANGE_WOOL);
                h.scale(0.0f, 0.0f, 0.0f).glow(220, 140, 80).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
                growBlock(h, 0.65f, 14);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_WOOL_PLACE, 1.0f, 1.2f);
            w.spawnParticle(Particle.HEART, center.clone().add(0, 3.5, 0), 5, 1, 1, 1, 0);
        }

        private void growBlock(BlockDisplayHandle h, float targetScale, int duration) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    new Vector3f(targetScale, targetScale, targetScale),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Sparkle particles during active
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.ENCHANT, c.clone().add(0, 2.5, 0), 6, 1.5, 1.5, 1.5, 0.5);
            }

            // Head tilt: gently tilts left/right
            if (tick > 20 && tick < 130 && tick % 6 == 0) {
                headTilt = (float) Math.toRadians(15.0 * Math.sin(tick * 0.05));
                for (BlockDisplayHandle h : head) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(6);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(headTilt, 0f, 0f, 1f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Arms open wide gradually (ticks 30-100)
            if (tick > 30 && tick < 100 && tick % 5 == 0) {
                float openAngle = (float) Math.toRadians(20.0 + (tick - 30) * 0.5);
                rotateArm(leftArm, openAngle, 0f, 0f, 1f);
                rotateArm(rightArm, -openAngle, 0f, 0f, 1f);
            }

            // SNAP shut at tick 140 — 2-tick interpolation, impact damage
            if (tick == 140 && !armsClosed) {
                armsClosed = true;
                snapArm(leftArm, (float) Math.toRadians(-90.0), 0f, 0f, 1f);
                snapArm(rightArm, (float) Math.toRadians(90.0), 0f, 0f, 1f);

                triggerImpactDamage(c.clone().add(0, 1.5, 0));
                DisplayBuilder.playSound(c, Sound.ENTITY_SLIME_SQUISH_SMALL, 1.4f, 0.8f);
                DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), 60, 3.0, 255, 100, 180, 1.5f);
                c.getWorld().spawnParticle(Particle.HEART, c.clone().add(0, 2, 0), 20, 1.5, 1.5, 1.5, 0);
            }

            // Fade via scale shrink (ticks 200-215)
            if (tick >= 200 && tick % 2 == 0) {
                float remaining = 1.0f - ((tick - 200) / 15.0f);
                if (remaining < 0f) remaining = 0f;
                for (BlockDisplayHandle h : allBlocks) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    Vector3f s = t.getScale();
                    e.setInterpolationDuration(2);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(s.x * remaining, s.y * remaining, s.z * remaining),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }
        }

        private void rotateArm(List<BlockDisplayHandle> arm, float angle, float ax, float ay, float az) {
            for (BlockDisplayHandle h : arm) {
                BlockDisplay e = h.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(5);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(angle, ax, ay, az),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }
        }

        private void snapArm(List<BlockDisplayHandle> arm, float angle, float ax, float ay, float az) {
            for (BlockDisplayHandle h : arm) {
                BlockDisplay e = h.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(2);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(angle, ax, ay, az),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new TeddyBearCrush(plugin); }
    }

    // ================================================================
    // #3 — GIANT YARN BALL
    // 40 blocks: 2 outer rings (12 ea WHITE_WOOL = 24), equatorial RED_WOOL
    // band (8), inner LIGHT_BLUE_WOOL ring at 45° (8). Outer co-rotate, inner
    // counter-rotates, ball drifts toward player. Constant 4.5r, 9 dmg/15t.
    // ================================================================
    public static class GiantYarnBall extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> outerRingA = new ArrayList<>();
        private final List<BlockDisplayHandle> outerRingB = new ArrayList<>();
        private final List<BlockDisplayHandle> equatorBand = new ArrayList<>();
        private final List<BlockDisplayHandle> innerRing = new ArrayList<>();
        private float outerRotation = 0f;
        private float innerRotation = 0f;

        public GiantYarnBall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("giant_yarn_ball", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(9.0);
            config.setDamageRadius(4.5);
            config.setTicksBetweenDamage(15);
            config.setDamageDelayTicks(10);
            config.setDurationTicks(280);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double radius = 2.4;

            // Outer ring A — horizontal at Y+2, 12 WHITE_WOOL blocks
            for (int i = 0; i < 12; i++) {
                double angle = (2.0 * Math.PI * i) / 12;
                double px = Math.cos(angle) * radius;
                double pz = Math.sin(angle) * radius;
                Location loc = center.clone().add(px, 2.5, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_WOOL);
                h.scale(0.45f, 0.45f, 0.45f).glow(245, 245, 245).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                outerRingA.add(h);
            }

            // Outer ring B — vertical (XY plane) at Z=0, 12 WHITE_WOOL
            for (int i = 0; i < 12; i++) {
                double angle = (2.0 * Math.PI * i) / 12;
                double px = Math.cos(angle) * radius;
                double py = Math.sin(angle) * radius + 2.5;
                Location loc = center.clone().add(px, py, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_WOOL);
                h.scale(0.45f, 0.45f, 0.45f).glow(245, 245, 245).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                outerRingB.add(h);
            }

            // Equatorial RED_WOOL band, 8 blocks
            for (int i = 0; i < 8; i++) {
                double angle = (2.0 * Math.PI * i) / 8;
                double px = Math.cos(angle) * radius * 1.05;
                double pz = Math.sin(angle) * radius * 1.05;
                Location loc = center.clone().add(px, 2.5, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.RED_WOOL);
                h.scale(0.55f, 0.35f, 0.55f).glow(220, 30, 60).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                equatorBand.add(h);
            }

            // Inner LIGHT_BLUE_WOOL ring offset 45°, 8 blocks
            for (int i = 0; i < 8; i++) {
                double angle = (2.0 * Math.PI * i) / 8;
                // Tilted plane: rotate XY ring by 45 deg around X axis
                double cosTilt = Math.cos(Math.toRadians(45));
                double sinTilt = Math.sin(Math.toRadians(45));
                double rawX = Math.cos(angle) * radius * 0.85;
                double rawY = Math.sin(angle) * radius * 0.85;
                double px = rawX;
                double py = rawY * cosTilt + 2.5;
                double pz = rawY * sinTilt;
                Location loc = center.clone().add(px, py, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIGHT_BLUE_WOOL);
                h.scale(0.4f, 0.4f, 0.4f).glow(140, 200, 240).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                innerRing.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_WOOL_PLACE, 1.2f, 0.9f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 2.5, 0), 25, 2, 2, 2, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            outerRotation += 0.04f;
            innerRotation -= 0.06f;

            // Rotate outer rings via Transformation (Y axis for ring A, Z axis for ring B)
            if (tick % 2 == 0) {
                rotateRing(outerRingA, outerRotation, 0f, 1f, 0f);
                rotateRing(outerRingB, outerRotation, 0f, 0f, 1f);
                rotateRing(equatorBand, outerRotation, 0f, 1f, 0f);
                rotateRing(innerRing, innerRotation, 1f, 0.5f, 0f);
            }

            // Setpiece: stays at spawn location (no per-tick player tracking).

            // SNOWFLAKE wisps every 2 ticks
            if (tick % 2 == 0) {
                double angle = tick * 0.2;
                double px = Math.cos(angle) * 2.4;
                double pz = Math.sin(angle) * 2.4;
                c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        c.clone().add(px, 2.5, pz), 2, 0.1, 0.1, 0.1, 0.02);
            }

            if (tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_WOOL_PLACE, 0.6f, 1.4f);
            }
        }

        private void rotateRing(List<BlockDisplayHandle> ring, float angle, float ax, float ay, float az) {
            for (BlockDisplayHandle h : ring) {
                BlockDisplay e = h.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(2);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(angle, ax, ay, az),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new GiantYarnBall(plugin); }
    }

    // ================================================================
    // #4 — BOUNTY BOMB
    // 26 blocks: 20 BLACK_CONCRETE fibonacci sphere + 6 DARK_OAK fuse helix.
    // Drops Y+12, bounces. Fuse burns down (5 ticks/segment), sphere vibrates,
    // detonates at tick 30. Impact-only 6.0r, 28 hearts.
    // ================================================================
    public static class BountyBomb extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> sphereBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> fuseBlocks = new ArrayList<>();
        private boolean detonated = false;
        private int fuseRemoved = 0;

        public BountyBomb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("bounty_bomb", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(28.0);
            config.setImpactRadius(6.0);
            config.setDamage(0);
            config.setDurationTicks(80);
            config.setCooldownTicks(220);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 20 BLACK_CONCRETE in fibonacci sphere distribution at Y+12
            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            double sphereRadius = 1.6;
            for (int i = 0; i < 20; i++) {
                double y = 1.0 - (2.0 * i / 19.0);
                double radiusAtY = Math.sqrt(1 - y * y);
                double theta = goldenAngle * i;
                double px = Math.cos(theta) * radiusAtY * sphereRadius;
                double pz = Math.sin(theta) * radiusAtY * sphereRadius;
                Location loc = center.clone().add(px, y * sphereRadius + 12, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
                h.scale(0.55f, 0.55f, 0.55f).glow(40, 40, 40).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                sphereBlocks.add(h);
            }

            // 6 DARK_OAK fuse blocks in helix above the sphere (Y+13.6 .. +16)
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * i) / 3.0;
                double px = Math.cos(angle) * 0.35;
                double pz = Math.sin(angle) * 0.35;
                double py = 13.6 + i * 0.45;
                Location loc = center.clone().add(px, py, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_OAK_PLANKS);
                h.scale(0.2f, 0.2f, 0.8f).glow(110, 70, 30).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                fuseBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_TNT_PRIMED, 1.3f, 0.9f);
            w.spawnParticle(Particle.SMOKE, center.clone().add(0, 14, 0), 15, 1, 1, 1, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Drop bounce: ticks 0-20, Y motion: 0..12 -> -10 (i.e. Y+2), bounce up to +2 then fall to -12 (ground)
            // We translate sphere + fuse together. Phases: 0-10 fall to ground, 10-14 bounce up, 14-20 fall again.
            if (tick <= 20) {
                float dropY;
                if (tick <= 10) {
                    dropY = -10f * (tick / 10f); // 0 -> -10
                } else if (tick <= 14) {
                    dropY = -10f + 2f * ((tick - 10) / 4f); // -10 -> -8
                } else {
                    dropY = -8f - 4f * ((tick - 14) / 6f); // -8 -> -12
                }
                if (tick % 2 == 0) {
                    translateGroup(sphereBlocks, dropY);
                    translateGroup(fuseBlocks, dropY);
                }
                if (tick == 10) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_WOOL_PLACE, 0.8f, 0.4f);
                }
            }

            // Fuse burn: remove one block every 5 ticks starting tick 5, total 6 over 30 ticks
            if (tick >= 5 && tick <= 35 && (tick - 5) % 5 == 0 && fuseRemoved < fuseBlocks.size()) {
                BlockDisplayHandle h = fuseBlocks.get(fuseBlocks.size() - 1 - fuseRemoved);
                BlockDisplay e = h.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(2);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f().set(t.getLeftRotation()),
                        new Vector3f(0f, 0f, 0f),
                        new AxisAngle4f().set(t.getRightRotation())
                ));
                fuseRemoved++;
                Location flameLoc = e.getLocation().clone();
                c.getWorld().spawnParticle(Particle.FLAME, flameLoc, 8, 0.2, 0.2, 0.2, 0.05);
            }

            // FLAME along remaining fuse
            if (tick % 2 == 0) {
                for (int i = 0; i < fuseBlocks.size() - fuseRemoved; i++) {
                    Location fuseLoc = fuseBlocks.get(i).entity().getLocation();
                    c.getWorld().spawnParticle(Particle.FLAME, fuseLoc, 1, 0.05, 0.05, 0.05, 0.02);
                    c.getWorld().spawnParticle(Particle.SMOKE, fuseLoc, 1, 0.05, 0.05, 0.05, 0.01);
                }
            }

            // Sphere vibrates (0.1 scale pulse) ticks 20-50
            if (tick > 20 && tick < 60 && tick % 3 == 0) {
                float pulseScale = 0.55f + 0.05f * (float) Math.sin(tick * 0.6);
                for (BlockDisplayHandle h : sphereBlocks) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(3);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(pulseScale, pulseScale, pulseScale),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // DETONATE at tick 60 (wait for sphere to settle on ground after fall)
            if (tick == 60 && !detonated) {
                detonated = true;
                Location bombLoc = c.clone();
                triggerImpactDamage(bombLoc);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.6f);
                c.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, bombLoc, 3, 1, 1, 1, 0);
                c.getWorld().spawnParticle(Particle.LAVA, bombLoc, 30, 3, 1, 3, 0);
                c.getWorld().spawnParticle(Particle.FLAME, bombLoc, 80, 4, 2, 4, 0.3);

                // Scatter sphere blocks outward via animateTo
                for (int i = 0; i < sphereBlocks.size(); i++) {
                    BlockDisplayHandle h = sphereBlocks.get(i);
                    double angle = (2.0 * Math.PI * i) / sphereBlocks.size();
                    float ox = (float) (Math.cos(angle) * 5.0);
                    float oz = (float) (Math.sin(angle) * 5.0);
                    h.animateTo(new Vector3f(ox - 0.5f, 1.5f - 0.5f, oz - 0.5f),
                            new AxisAngle4f((float) Math.PI, (float) Math.random(), (float) Math.random(), (float) Math.random()),
                            new Vector3f(0.2f, 0.2f, 0.2f), 15);
                }
            }
        }

        private void translateGroup(List<BlockDisplayHandle> group, float dropY) {
            for (BlockDisplayHandle h : group) {
                BlockDisplay e = h.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(2);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        new Vector3f(-0.5f, dropY - 0.5f, -0.5f),
                        new AxisAngle4f().set(t.getLeftRotation()),
                        t.getScale(),
                        new AxisAngle4f().set(t.getRightRotation())
                ));
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new BountyBomb(plugin); }
    }

    // ================================================================
    // #5 — CAT CRADLE NET
    // 42 blocks: 6 BIRCH posts (4 ea = 24), 18 WHITE_CONCRETE beams scaled
    // thin (0.08x0.08x4.0). Posts rise, beams materialize, beams tighten,
    // structure rotates Y. Constant 3.0r, 10 dmg/10t (20-tick delay).
    // ================================================================
    public static class CatCradleNet extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> postBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> beamBlocks = new ArrayList<>();
        private float rotationY = 0f;

        public CatCradleNet(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cat_cradle_net", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(10.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(10);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(280);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 6 posts arranged in hexagon, each 4 BIRCH_PLANKS stacked
            double hexRadius = 3.0;
            double[][] postPositions = new double[6][2];
            for (int i = 0; i < 6; i++) {
                double angle = (2.0 * Math.PI * i) / 6;
                postPositions[i][0] = Math.cos(angle) * hexRadius;
                postPositions[i][1] = Math.sin(angle) * hexRadius;

                for (int s = 0; s < 4; s++) {
                    Location loc = center.clone().add(postPositions[i][0], -1, postPositions[i][1]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BIRCH_PLANKS);
                    h.scale(0.7f, 1.0f, 0.7f).glow(220, 200, 150).interpolation(10, 0);
                    spawnedEntities.add(h.entity());
                    // Animate rise: target Y offset = s
                    final int segmentIndex = s;
                    BlockDisplay e = h.entity();
                    e.setInterpolationDuration(10);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(-0.5f, segmentIndex * 1.0f - 0.5f, -0.5f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.7f, 1.0f, 0.7f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    postBlocks.add(h);
                }
            }

            // 18 WHITE_CONCRETE beams connecting posts at varying heights
            // 6 beams at top (each connecting adjacent posts), 6 at mid, 6 cross-diagonals
            int delayTicker = 0;
            for (int i = 0; i < 6; i++) {
                int next = (i + 1) % 6;
                addBeam(center, postPositions[i][0], 4.0, postPositions[i][1],
                        postPositions[next][0], 4.0, postPositions[next][1], delayTicker);
                delayTicker += 2;
            }
            for (int i = 0; i < 6; i++) {
                int next = (i + 1) % 6;
                addBeam(center, postPositions[i][0], 2.0, postPositions[i][1],
                        postPositions[next][0], 2.0, postPositions[next][1], delayTicker);
                delayTicker += 2;
            }
            for (int i = 0; i < 6; i++) {
                int across = (i + 3) % 6;
                addBeam(center, postPositions[i][0], 4.0, postPositions[i][1],
                        postPositions[across][0], 0.5, postPositions[across][1], delayTicker);
                delayTicker += 2;
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_TRIPWIRE_ATTACH, 1.0f, 0.7f);
            w.spawnParticle(Particle.ENCHANT, center.clone().add(0, 2.5, 0), 30, 2, 2, 2, 0.5);
        }

        private void addBeam(Location center, double x1, double y1, double z1,
                              double x2, double y2, double z2, int delay) {
            double mx = (x1 + x2) / 2.0;
            double my = (y1 + y2) / 2.0;
            double mz = (z1 + z2) / 2.0;
            double dx = x2 - x1;
            double dy = y2 - y1;
            double dz = z2 - z1;
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            float yaw = (float) Math.atan2(dz, dx);
            float pitch = (float) Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));

            Location loc = center.clone().add(mx, my, mz);
            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_CONCRETE);
            h.scale(0.08f, 0.08f, (float) length);
            // Rotate around Y axis to align with horizontal direction; then we apply pitch via X axis approximation
            h.rotate(yaw, 0f, 1f, 0f);
            h.glow(245, 245, 245).interpolation(3, delay);
            spawnedEntities.add(h.entity());
            beamBlocks.add(h);
            // Apply pitch as a separate rotation by setting full transformation
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f(yaw, 0f, 1f, 0f),
                    t.getScale(),
                    new AxisAngle4f(-pitch, (float) Math.sin(yaw), 0f, (float) Math.cos(yaw))
            ));
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Whole structure rotates Y at 0.01 rad/tick — we apply rotation to all blocks each cycle
            rotationY += 0.01f;
            if (tick % 3 == 0) {
                for (BlockDisplayHandle h : postBlocks) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(3);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(rotationY, 0f, 1f, 0f),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Beams tighten (Z scale shrinks slowly from 4.0 toward smaller)
            if (tick > 30 && tick < 250 && tick % 5 == 0) {
                float tightenFactor = 1.0f - ((tick - 30) / 700f);
                if (tightenFactor < 0.5f) tightenFactor = 0.5f;
                for (BlockDisplayHandle h : beamBlocks) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    Vector3f oldScale = t.getScale();
                    e.setInterpolationDuration(5);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(oldScale.x, oldScale.y, oldScale.z * tightenFactor / 1.0f),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // ENCHANT sparks at intersections every 3 ticks
            if (tick % 3 == 0) {
                for (int i = 0; i < 4; i++) {
                    double a = Math.random() * 2 * Math.PI;
                    double r = 0.5 + Math.random() * 2.5;
                    Location p = c.clone().add(Math.cos(a) * r, 1.5 + Math.random() * 2.5, Math.sin(a) * r);
                    c.getWorld().spawnParticle(Particle.ENCHANT, p, 3, 0.1, 0.1, 0.1, 0.5);
                }
            }

            if (tick % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_TRIPWIRE_ATTACH, 0.5f, 1.4f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new CatCradleNet(plugin); }
    }

    // ================================================================
    // #6 — WIND UP KEY
    // 26 blocks: 8 GOLD_BLOCK bow ring, 6 GOLD_BLOCK shaft, 2 GOLD bit,
    // 10 COPPER decorative collar. Embedded in ground at 45°, rotates on
    // shaft axis, plays xylophone clicks. Constant 2.5r, 8 dmg/20t.
    // ================================================================
    public static class WindUpKey extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private float shaftRotation = 0f;
        private int rotationsSinceSound = 0;

        public WindUpKey(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("wind_up_key", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(8.0);
            config.setDamageRadius(2.5);
            config.setTicksBetweenDamage(20);
            config.setDamageDelayTicks(15);
            config.setDurationTicks(280);
            config.setCooldownTicks(220);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Key axis is tilted 45 deg. We set up the geometry with the shaft along
            // the (cos(45), sin(45), 0) direction. Bow at "near" end, blade at far end.
            double tilt = Math.toRadians(45);

            // 8 GOLD_BLOCK bow ring blocks (circle in the plane perpendicular to shaft)
            double bowRadius = 0.9;
            for (int i = 0; i < 8; i++) {
                double a = (2.0 * Math.PI * i) / 8;
                // Local circle in YZ plane, then rotate around Z by 45 deg so it sits perpendicular to shaft
                double localY = Math.cos(a) * bowRadius;
                double localZ = Math.sin(a) * bowRadius;
                // Rotate (X=0, Y=localY) by tilt around Z so axis aligns
                double px = -localY * Math.sin(tilt);
                double py = localY * Math.cos(tilt) + 1.0;
                double pz = localZ;
                Location loc = center.clone().add(px, py, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.GOLD_BLOCK);
                h.scale(0.6f, 0.6f, 0.6f).glow(255, 215, 90).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // 6 GOLD_BLOCK shaft blocks rising along tilted axis
            for (int i = 0; i < 6; i++) {
                double along = 1.0 + i * 0.6;
                double px = Math.cos(tilt) * along;
                double py = Math.sin(tilt) * along + 0.2;
                Location loc = center.clone().add(px, py, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.GOLD_BLOCK);
                h.scale(0.4f, 0.4f, 0.55f).glow(255, 215, 90).interpolation(10, 0);
                h.rotate((float) tilt, 0f, 0f, 1f);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // 2 GOLD_BLOCK bit (the protruding key teeth)
            for (int i = 0; i < 2; i++) {
                double along = 4.6 + i * 0.3;
                double px = Math.cos(tilt) * along;
                double py = Math.sin(tilt) * along + 0.2;
                double pz = (i == 0) ? 0.5 : -0.5;
                Location loc = center.clone().add(px, py, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.GOLD_BLOCK);
                h.scale(0.4f, 0.4f, 0.4f).glow(255, 215, 90).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // 10 COPPER_BLOCK decorative collar ring at base of shaft
            double collarRadius = 1.1;
            for (int i = 0; i < 10; i++) {
                double a = (2.0 * Math.PI * i) / 10;
                double localY = Math.cos(a) * collarRadius;
                double localZ = Math.sin(a) * collarRadius;
                double px = -localY * Math.sin(tilt) + Math.cos(tilt) * 1.3;
                double py = localY * Math.cos(tilt) + Math.sin(tilt) * 1.3 + 0.2;
                double pz = localZ;
                Location loc = center.clone().add(px, py, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.COPPER_BLOCK);
                h.scale(0.35f, 0.35f, 0.35f).glow(220, 130, 60).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1.2f, 1.6f);
            w.spawnParticle(Particle.ENCHANT, center.clone().add(0, 2, 0), 25, 1.5, 1.5, 1.5, 0.6);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rotate around shaft axis (45-deg tilted axis: cos(45), sin(45), 0)
            shaftRotation += 0.03f;
            if (tick % 3 == 0) {
                float ax = (float) Math.cos(Math.toRadians(45));
                float ay = (float) Math.sin(Math.toRadians(45));
                for (BlockDisplayHandle h : allBlocks) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(3);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(shaftRotation, ax, ay, 0f),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Every full rotation (~210 ticks at 0.03 rad/tick) play wind-up + CRIT burst
            if (shaftRotation >= (rotationsSinceSound + 1) * (Math.PI * 2 / 6)) {
                rotationsSinceSound++;
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 0.7f, 1.8f);
                c.getWorld().spawnParticle(Particle.CRIT, c.clone().add(0, 1.5, 0), 15, 1, 1, 1, 0.3);
            }

            // ENCHANT sparks during rotation
            if (tick % 2 == 0) {
                double a = tick * 0.2;
                double px = Math.cos(a) * 1.0;
                double py = 1.5 + Math.sin(a) * 0.5;
                c.getWorld().spawnParticle(Particle.ENCHANT,
                        c.clone().add(px, py, 0), 2, 0.1, 0.1, 0.1, 0.4);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new WindUpKey(plugin); }
    }

    // ================================================================
    // #7 — GINORMOUS LOLLIPOP
    // 26 blocks: 18 candy sphere (PINK_CONCRETE 9 + WHITE_CONCRETE 9 spiral),
    // 8 CHERRY_PLANKS stick. Falls Y+18 stick-first, spinning 1.5 rad/tick.
    // Wobbles on impact. Impact-only 4.5r, 20 hearts.
    // ================================================================
    public static class GinormousLollipop extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> candyBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> stickBlocks = new ArrayList<>();
        private boolean impacted = false;
        private float candySpin = 0f;

        public GinormousLollipop(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ginormous_lollipop", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(20.0);
            config.setImpactRadius(4.5);
            config.setDamage(0);
            config.setDurationTicks(180);
            config.setCooldownTicks(220);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 18 candy sphere blocks in alternating spiral (PINK_CONCRETE / WHITE_CONCRETE)
            // — bumped from 12 to 18 to ensure 25+ block total.
            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            double sphereRadius = 1.5;
            for (int i = 0; i < 18; i++) {
                double y = 1.0 - (2.0 * i / 17.0);
                double radiusAtY = Math.sqrt(1 - y * y);
                double theta = goldenAngle * i;
                double px = Math.cos(theta) * radiusAtY * sphereRadius;
                double pz = Math.sin(theta) * radiusAtY * sphereRadius;
                Location loc = center.clone().add(px, y * sphereRadius + 18, pz);
                Material mat = (i % 2 == 0) ? Material.PINK_CONCRETE : Material.WHITE_CONCRETE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                int gr = (i % 2 == 0) ? 255 : 245;
                int gg = (i % 2 == 0) ? 100 : 245;
                int gb = (i % 2 == 0) ? 180 : 245;
                h.scale(0.55f, 0.55f, 0.55f).glow(gr, gg, gb).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                candyBlocks.add(h);
            }

            // 8 CHERRY_PLANKS stick blocks (thin, tall) below the sphere
            for (int i = 0; i < 8; i++) {
                Location loc = center.clone().add(0, 16 - i * 0.6, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHERRY_PLANKS);
                h.scale(0.3f, 0.6f, 0.3f).glow(220, 140, 130).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                stickBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_WOOL_PLACE, 0.7f, 1.6f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Falling phase: ticks 0-25, Y drops -18 over span. Spinning candy on Y axis.
            if (tick < 25 && !impacted) {
                float dropY = -18f * (tick / 25f);
                candySpin += 0.3f;
                if (tick % 2 == 0) {
                    for (BlockDisplayHandle h : candyBlocks) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(2);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                new Vector3f(-0.5f, dropY - 0.5f, -0.5f),
                                new AxisAngle4f(candySpin, 0f, 1f, 0f),
                                t.getScale(),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                    for (BlockDisplayHandle h : stickBlocks) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(2);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                new Vector3f(-0.5f, dropY - 0.5f, -0.5f),
                                new AxisAngle4f(candySpin, 0f, 1f, 0f),
                                t.getScale(),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                }

                // Trail particles
                if (tick % 2 == 0) {
                    Location trailLoc = c.clone().add(0, 18 + dropY, 0);
                    c.getWorld().spawnParticle(Particle.CRIT, trailLoc, 4, 0.5, 0.5, 0.5, 0.1);
                    DisplayBuilder.dustParticles(trailLoc, 6, 1.0, 255, 100, 180, 1.2f);
                }
            }

            // IMPACT at tick 25
            if (tick == 25 && !impacted) {
                impacted = true;
                Location impactLoc = c.clone();
                triggerImpactDamage(impactLoc);
                DisplayBuilder.playSound(c, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.4f, 1.5f);
                c.getWorld().spawnParticle(Particle.FIREWORK, impactLoc.clone().add(0, 0.5, 0), 60, 3, 1, 3, 0.3);
                DisplayBuilder.dustParticles(impactLoc, 80, 4.0, 255, 100, 180, 1.6f);
            }

            // Wobble + spin during active phase
            if (tick > 25 && tick < 130) {
                candySpin += 0.05f;
                if (tick % 4 == 0) {
                    float wobbleY = (float) (Math.sin(tick * 0.3) * 0.2);
                    for (BlockDisplayHandle h : candyBlocks) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(4);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                new Vector3f(-0.5f, -18f + wobbleY - 0.5f, -0.5f),
                                new AxisAngle4f(candySpin, 0f, 1f, 0f),
                                t.getScale(),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                }
            }

            // Shatter outward at tick 130
            if (tick == 130) {
                for (int i = 0; i < candyBlocks.size(); i++) {
                    double angle = (2.0 * Math.PI * i) / candyBlocks.size();
                    float ox = (float) (Math.cos(angle) * 6.0);
                    float oz = (float) (Math.sin(angle) * 6.0);
                    candyBlocks.get(i).animateTo(
                            new Vector3f(ox - 0.5f, -17f - 0.5f, oz - 0.5f),
                            new AxisAngle4f((float) Math.PI, 1f, 1f, 0f),
                            new Vector3f(0.2f, 0.2f, 0.2f), 25);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new GinormousLollipop(plugin); }
    }

    // ================================================================
    // #8 — PAW PRINT FIELD
    // 42 blocks: 6 pawprints (1 large pad + 4 toe circles + 2 extra toe pads
    // = 7 ea × 6 = 42). All rise simultaneously, breathe pulse, then sink.
    // Combined zone: single radius 6.0 covering all prints, 10 dmg/12t.
    // ================================================================
    public static class PawPrintField extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private float breathePhase = 0f;

        public PawPrintField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("paw_print_field", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(10.0);
            // Combined radius covers all 6 prints (stride pattern fits within ~6 blocks)
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(5);
            config.setDurationTicks(220);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 6 pawprints arranged in a walking stride pattern around the center
            double[][] pawPositions = {
                    {-3.0, 2.0}, {-1.0, -2.0}, {1.0, 2.0},
                    {3.0, -2.0}, {0.0, 0.0}, {-2.0, 0.5}
            };

            for (int p = 0; p < 6; p++) {
                double cx = pawPositions[p][0];
                double cz = pawPositions[p][1];
                int spawnDelay = p * 3;

                // Large oval pad (1 block, scaled 1.8 x 0.3 x 2.4)
                Location padLoc = center.clone().add(cx, -0.3, cz);
                BlockDisplayHandle pad = displayBuilder.spawnBlock(padLoc, Material.PINK_CONCRETE);
                pad.scale(1.8f, 0.3f, 2.4f).glow(255, 100, 180).interpolation(5, spawnDelay);
                spawnedEntities.add(pad.entity());
                allBlocks.add(pad);
                animateRise(pad, 0.0f, 5, spawnDelay);

                // 4 toe circles in arc above pad
                double[][] toeOffsets = {{-0.7, 1.3}, {-0.25, 1.55}, {0.25, 1.55}, {0.7, 1.3}};
                for (double[] toff : toeOffsets) {
                    Location tloc = center.clone().add(cx + toff[0], -0.3, cz + toff[1]);
                    BlockDisplayHandle toe = displayBuilder.spawnBlock(tloc, Material.PINK_CONCRETE);
                    toe.scale(0.8f, 0.3f, 0.8f).glow(255, 100, 180).interpolation(5, spawnDelay);
                    spawnedEntities.add(toe.entity());
                    allBlocks.add(toe);
                    animateRise(toe, 0.0f, 5, spawnDelay);
                }

                // 2 extra paw bean pads (extra beans for richer print) — gives 7 per print
                double[][] extraOffsets = {{-0.45, -0.7}, {0.45, -0.7}};
                for (double[] eoff : extraOffsets) {
                    Location eloc = center.clone().add(cx + eoff[0], -0.3, cz + eoff[1]);
                    BlockDisplayHandle bean = displayBuilder.spawnBlock(eloc, Material.PINK_CONCRETE);
                    bean.scale(0.5f, 0.3f, 0.5f).glow(255, 100, 180).interpolation(5, spawnDelay);
                    spawnedEntities.add(bean.entity());
                    allBlocks.add(bean);
                    animateRise(bean, 0.0f, 5, spawnDelay);
                }

                // Cat step sound staggered
                final Location stepLoc = padLoc;
                plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                        DisplayBuilder.playSound(stepLoc, Sound.ENTITY_CAT_PURR, 1.3f, 0.9f), spawnDelay);
            }
        }

        private void animateRise(BlockDisplayHandle h, float targetY, int duration, int delay) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(delay);
            e.setTransformation(new Transformation(
                    new Vector3f(-0.5f, targetY - 0.5f, -0.5f),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    t.getScale(),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Breathing pulse: Y scale 0.3 -> 0.35 -> 0.3
            breathePhase += 0.1f;
            if (tick > 30 && tick < 180 && tick % 4 == 0) {
                float yScale = 0.3f + 0.05f * (float) Math.sin(breathePhase);
                for (BlockDisplayHandle h : allBlocks) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    Vector3f s = t.getScale();
                    e.setInterpolationDuration(4);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(s.x, yScale, s.z),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Pink dust shimmer
            if (tick % 3 == 0) {
                double[][] pawPositions = {
                        {-3.0, 2.0}, {-1.0, -2.0}, {1.0, 2.0},
                        {3.0, -2.0}, {0.0, 0.0}, {-2.0, 0.5}
                };
                for (double[] p : pawPositions) {
                    DisplayBuilder.dustParticles(c.clone().add(p[0], 0.3, p[1]),
                            3, 0.8, 255, 100, 180, 1.0f);
                }
            }

            // Sink phase: ticks 200-220 — return to Y=-0.3
            if (tick >= 200 && tick % 2 == 0) {
                float sinkY = -0.3f * ((tick - 200) / 20f);
                for (BlockDisplayHandle h : allBlocks) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(2);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(-0.5f, sinkY - 0.5f, -0.5f),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new PawPrintField(plugin); }
    }

    // ================================================================
    // #9 — PLUSHTRAP SPIDER
    // 42 blocks: 8 LIGHT_BLUE_WOOL body sphere, 8 legs × 4 BONE_BLOCK = 32,
    // 2 BLACK_CONCRETE button eyes. Descends on enchant threads, legs unfold
    // outward 8t staggered. Impact-only 3.0r, 18 hearts.
    // ================================================================
    public static class PlushtrapSpider extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> bodyBlocks = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> legs = new ArrayList<>();
        private final List<BlockDisplayHandle> eyes = new ArrayList<>();
        private boolean impacted = false;
        private float bodyBob = 0f;

        public PlushtrapSpider(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("plushtrap_spider", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(18.0);
            config.setImpactRadius(3.0);
            config.setDamage(0);
            config.setDurationTicks(220);
            config.setCooldownTicks(220);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 8 LIGHT_BLUE_WOOL body sphere blocks (golden angle), starts at Y+15
            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            double bodyRadius = 1.0;
            for (int i = 0; i < 8; i++) {
                double y = 1.0 - (2.0 * i / 7.0);
                double radiusAtY = Math.sqrt(1 - y * y);
                double theta = goldenAngle * i;
                double px = Math.cos(theta) * radiusAtY * bodyRadius;
                double pz = Math.sin(theta) * radiusAtY * bodyRadius;
                Location loc = center.clone().add(px, y * bodyRadius + 15, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIGHT_BLUE_WOOL);
                h.scale(0.55f, 0.55f, 0.55f).glow(140, 200, 240).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                bodyBlocks.add(h);
            }

            // 8 legs (4 BONE_BLOCK each), starting at body, folded up (vertical)
            for (int legIdx = 0; legIdx < 8; legIdx++) {
                List<BlockDisplayHandle> leg = new ArrayList<>();
                double legAngle = (2.0 * Math.PI * legIdx) / 8;
                for (int s = 0; s < 4; s++) {
                    Location loc = center.clone().add(
                            Math.cos(legAngle) * 0.5,
                            15 + 0.2 + s * 0.4,
                            Math.sin(legAngle) * 0.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BONE_BLOCK);
                    h.scale(0.25f, 0.25f, 1.2f).glow(240, 230, 210).interpolation(4, 0);
                    // Initially vertical (rotated 90 deg around horizontal axis perpendicular to leg)
                    float perpX = (float) -Math.sin(legAngle);
                    float perpZ = (float) Math.cos(legAngle);
                    h.rotate((float) Math.toRadians(90), perpX, 0f, perpZ);
                    spawnedEntities.add(h.entity());
                    leg.add(h);
                }
                legs.add(leg);
            }

            // 2 button eyes
            for (int i = 0; i < 2; i++) {
                double ex = (i == 0) ? -0.35 : 0.35;
                Location loc = center.clone().add(ex, 15.7, 0.85);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
                h.scale(0.3f, 0.3f, 0.1f).glow(20, 20, 20).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                eyes.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_SPIDER_AMBIENT, 1.2f, 1.4f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Descent phase: ticks 0-20 — body drops from Y+15 to Y+0.5
            if (tick <= 20 && !impacted) {
                float dropY = -14.5f * (tick / 20f);
                if (tick % 2 == 0) {
                    translateAll(dropY);
                }
                // ENCHANT thread lines from Y+15 down to body
                for (int i = 0; i < 4; i++) {
                    Location threadStart = c.clone().add(
                            (Math.random() - 0.5) * 1.5,
                            15.0,
                            (Math.random() - 0.5) * 1.5);
                    Location threadEnd = c.clone().add(0, 15 + dropY, 0);
                    DisplayBuilder.particleLine(threadStart, threadEnd, Particle.ENCHANT, 2, null);
                }
            }

            // IMPACT at tick 20
            if (tick == 20 && !impacted) {
                impacted = true;
                Location impactLoc = c.clone();
                triggerImpactDamage(impactLoc);
                c.getWorld().spawnParticle(Particle.CRIT, impactLoc.clone().add(0, 0.5, 0), 50, 2.5, 0.5, 2.5, 0.4);
                DisplayBuilder.dustParticles(impactLoc, 50, 3.0, 140, 200, 240, 1.5f);
            }

            // Leg unfold: ticks 20-28, staggered. Rotate from vertical (90°) to horizontal (0° outward sweep).
            if (tick > 20 && tick < 30 && tick % 1 == 0) {
                int unfoldStep = tick - 20;
                for (int legIdx = 0; legIdx < 8; legIdx++) {
                    int legStart = legIdx; // staggered: each leg starts on its own tick
                    int progress = unfoldStep - legStart;
                    if (progress < 0 || progress > 4) continue;
                    float t = progress / 4f;
                    // Smoothly rotate leg from vertical (90°) to outstretched (0° outward)
                    float angle = (float) Math.toRadians(90.0 * (1.0 - t));
                    double legAngle = (2.0 * Math.PI * legIdx) / 8;
                    float perpX = (float) -Math.sin(legAngle);
                    float perpZ = (float) Math.cos(legAngle);
                    for (BlockDisplayHandle h : legs.get(legIdx)) {
                        BlockDisplay e = h.entity();
                        Transformation tr = e.getTransformation();
                        e.setInterpolationDuration(2);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                tr.getTranslation(),
                                new AxisAngle4f(angle, perpX, 0f, perpZ),
                                tr.getScale(),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                    // Spider step sound staggered
                    if (progress == 0) {
                        DisplayBuilder.playSound(c, Sound.ENTITY_SPIDER_STEP, 0.9f, 1.3f);
                    }
                }
            }

            // Active body bob: Y 0.0 -> 0.2 cycle
            if (tick > 30 && tick < 200 && tick % 4 == 0) {
                bodyBob = (float) (Math.sin(tick * 0.15) * 0.2);
                for (BlockDisplayHandle h : bodyBlocks) {
                    BlockDisplay e = h.entity();
                    Transformation tr = e.getTransformation();
                    e.setInterpolationDuration(4);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(-0.5f, -14.5f + bodyBob - 0.5f, -0.5f),
                            new AxisAngle4f().set(tr.getLeftRotation()),
                            tr.getScale(),
                            new AxisAngle4f().set(tr.getRightRotation())
                    ));
                }
            }

            // Rise back up: ticks 200-220
            if (tick >= 200 && tick % 2 == 0) {
                float riseY = -14.5f + 14.5f * ((tick - 200) / 20f);
                translateAll(riseY);
            }
        }

        private void translateAll(float dropY) {
            for (BlockDisplayHandle h : bodyBlocks) translateBlock(h, dropY);
            for (List<BlockDisplayHandle> leg : legs) {
                for (BlockDisplayHandle h : leg) translateBlock(h, dropY);
            }
            for (BlockDisplayHandle h : eyes) translateBlock(h, dropY);
        }

        private void translateBlock(BlockDisplayHandle h, float dropY) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(2);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    new Vector3f(-0.5f, dropY - 0.5f, -0.5f),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    t.getScale(),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new PlushtrapSpider(plugin); }
    }

    // ================================================================
    // #10 — BUNNY EAR BLADES
    // 26 blocks: 2 ears (8 ea = 16: 4 WHITE_WOOL inner + 4 PINK_CONCRETE outer
    // each), 8 ORANGE_WOOL oval base head, 2 PINK_CONCRETE eye buttons. Ears
    // rise then SCISSOR shut. Impact-only 4.0r, 30 hearts.
    // ================================================================
    public static class BunnyEarBlades extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> leftEar = new ArrayList<>();
        private final List<BlockDisplayHandle> rightEar = new ArrayList<>();
        private final List<BlockDisplayHandle> headBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> eyeBlocks = new ArrayList<>();
        private boolean snapped = false;
        private float openAngle = 0f;

        public BunnyEarBlades(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("bunny_ear_blades", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(30.0);
            config.setImpactRadius(4.0);
            config.setDamage(0);
            config.setDurationTicks(180);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Left ear: 4 WHITE_WOOL inner stack + 4 PINK_CONCRETE outer stack = 8 blocks
            // — bumped from 7 to 8 per ear for 25+ block total.
            for (int s = 0; s < 4; s++) {
                Location loc = center.clone().add(-0.7, -1, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_WOOL);
                h.scale(0.55f, 1.0f, 0.55f).glow(245, 245, 245).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                BlockDisplay e = h.entity();
                e.setTransformation(new Transformation(
                        new Vector3f(-0.5f, s * 1.0f - 0.5f, -0.5f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.55f, 1.0f, 0.55f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                leftEar.add(h);
            }
            for (int s = 0; s < 4; s++) {
                Location loc = center.clone().add(-0.85, -1, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PINK_CONCRETE);
                h.scale(0.35f, 1.0f, 0.35f).glow(255, 100, 180).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                BlockDisplay e = h.entity();
                e.setTransformation(new Transformation(
                        new Vector3f(-0.5f, s * 1.0f - 0.5f, -0.5f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.35f, 1.0f, 0.35f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                leftEar.add(h);
            }

            // Right ear: 4 WHITE_WOOL inner + 4 PINK_CONCRETE outer
            for (int s = 0; s < 4; s++) {
                Location loc = center.clone().add(0.7, -1, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_WOOL);
                h.scale(0.55f, 1.0f, 0.55f).glow(245, 245, 245).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                BlockDisplay e = h.entity();
                e.setTransformation(new Transformation(
                        new Vector3f(-0.5f, s * 1.0f - 0.5f, -0.5f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.55f, 1.0f, 0.55f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                rightEar.add(h);
            }
            for (int s = 0; s < 4; s++) {
                Location loc = center.clone().add(0.85, -1, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PINK_CONCRETE);
                h.scale(0.35f, 1.0f, 0.35f).glow(255, 100, 180).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                BlockDisplay e = h.entity();
                e.setTransformation(new Transformation(
                        new Vector3f(-0.5f, s * 1.0f - 0.5f, -0.5f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.35f, 1.0f, 0.35f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                rightEar.add(h);
            }

            // 8 ORANGE_WOOL oval base head
            double[][] headOffsets = {
                    {-0.6, 0.0, 0.0}, {0.6, 0.0, 0.0}, {0.0, 0.0, 0.6}, {0.0, 0.0, -0.6},
                    {-0.4, 0.5, 0.4}, {0.4, 0.5, 0.4}, {-0.4, 0.5, -0.4}, {0.4, 0.5, -0.4}
            };
            for (double[] off : headOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ORANGE_WOOL);
                h.scale(0.7f, 0.7f, 0.7f).glow(220, 140, 80).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                headBlocks.add(h);
            }

            // 2 PINK eye buttons
            for (int i = 0; i < 2; i++) {
                double ex = (i == 0) ? -0.3 : 0.3;
                Location loc = center.clone().add(ex, 0.6, 0.6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PINK_CONCRETE);
                h.scale(0.25f, 0.25f, 0.1f).glow(255, 100, 180).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                eyeBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_WOOL_PLACE, 1.0f, 1.4f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rise phase: ticks 0-15. Ears rise from Y=-1 to Y=0 by translating each block.
            if (tick < 15) {
                float riseProgress = tick / 15f;
                if (tick % 2 == 0) {
                    for (int s = 0; s < 4; s++) {
                        // Inner blocks (indices 0..3 of each ear) and outer (indices 4..7)
                        for (int outerOffset = 0; outerOffset < 8; outerOffset += 4) {
                            float scaleX = (outerOffset == 0) ? 0.55f : 0.35f;
                            translateEarBlock(leftEar.get(s + outerOffset), s, riseProgress, scaleX, 0f, 0f);
                            translateEarBlock(rightEar.get(s + outerOffset), s, riseProgress, scaleX, 0f, 0f);
                        }
                    }
                }
            }

            // Open phase: ears slowly open wider (ticks 30-100), Z-axis outward rotation
            if (tick > 30 && tick < 100 && tick % 5 == 0) {
                openAngle = (float) Math.toRadians(15.0 + (tick - 30) * 0.4);
                rotateEar(leftEar, -openAngle, 0f, 0f, 1f);
                rotateEar(rightEar, openAngle, 0f, 0f, 1f);
                if (tick % 20 == 0) {
                    // CRIT trail along ear edges as they open
                    c.getWorld().spawnParticle(Particle.CRIT, c.clone().add(-0.85, 3, 0), 6, 0.2, 1.5, 0.2, 0.1);
                    c.getWorld().spawnParticle(Particle.CRIT, c.clone().add(0.85, 3, 0), 6, 0.2, 1.5, 0.2, 0.1);
                }
            }

            // SNAP shut at tick 120 — 2-tick interpolation, impact damage
            if (tick == 120 && !snapped) {
                snapped = true;
                snapEar(leftEar, (float) Math.toRadians(75.0), 0f, 0f, 1f);
                snapEar(rightEar, (float) Math.toRadians(-75.0), 0f, 0f, 1f);

                triggerImpactDamage(c.clone().add(0, 2.5, 0));
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.6f, 1.6f);
                DisplayBuilder.dustParticles(c.clone().add(0, 2.5, 0), 80, 4.0, 245, 245, 245, 1.6f);
                c.getWorld().spawnParticle(Particle.CRIT, c.clone().add(0, 2.5, 0), 60, 3.5, 1, 3.5, 0.3);
            }

            // Sink phase: ticks 150-180, ears go back into ground
            if (tick > 150 && tick % 2 == 0) {
                float sinkProgress = 1.0f - (tick - 150) / 30f;
                if (sinkProgress < 0) sinkProgress = 0;
                for (int s = 0; s < 4; s++) {
                    for (int outerOffset = 0; outerOffset < 8; outerOffset += 4) {
                        float scaleX = (outerOffset == 0) ? 0.55f : 0.35f;
                        translateEarBlock(leftEar.get(s + outerOffset), s, sinkProgress, scaleX, 0f, 0f);
                        translateEarBlock(rightEar.get(s + outerOffset), s, sinkProgress, scaleX, 0f, 0f);
                    }
                }
            }
        }

        private void translateEarBlock(BlockDisplayHandle h, int segmentIdx, float riseProgress,
                                        float scaleX, float ax, float ay) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(2);
            e.setInterpolationDelay(0);
            float yOff = -1.0f + (segmentIdx + 1) * 1.0f * riseProgress;
            e.setTransformation(new Transformation(
                    new Vector3f(-0.5f, yOff - 0.5f, -0.5f),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    t.getScale(),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        private void rotateEar(List<BlockDisplayHandle> ear, float angle, float ax, float ay, float az) {
            for (BlockDisplayHandle h : ear) {
                BlockDisplay e = h.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(5);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(angle, ax, ay, az),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }
        }

        private void snapEar(List<BlockDisplayHandle> ear, float angle, float ax, float ay, float az) {
            for (BlockDisplayHandle h : ear) {
                BlockDisplay e = h.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(2);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(angle, ax, ay, az),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new BunnyEarBlades(plugin); }
    }
}
