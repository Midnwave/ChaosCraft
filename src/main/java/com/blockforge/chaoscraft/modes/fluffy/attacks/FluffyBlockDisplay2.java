package com.blockforge.chaoscraft.modes.fluffy.attacks;

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
 * Fluffy Mode — ANIMAL-THEMED BLOCK DISPLAY ATTACKS (attacks 11–20)
 * 10 cute-but-dangerous animal-themed BlockDisplay attacks with 25+ block displays each.
 * All animations via Transformation interpolation (no per-tick teleports for rotation).
 *
 * Fluffy palette:
 * - Soft pastels: PINK_CONCRETE, LIGHT_BLUE_CONCRETE, WHITE_WOOL, YELLOW_CONCRETE
 * - Naturals: BROWN_WOOL, ORANGE_WOOL, BONE_BLOCK, OAK_LOG
 * - Accents: BLACK_CONCRETE, GRAY_WOOL, LIME_WOOL
 *
 * Attacks:
 * 11. GiantOwlForm        — 65 blocks, perched owl with rotating head
 * 12. FluffySerpent       — 44 blocks, undulating S-curve snake
 * 13. ButterflyWings      — 46 blocks, flapping butterfly
 * 14. GiantHedgehog       — 46 blocks, rolling spiked ball
 * 15. FoxSweepTail        — 24 blocks, sweeping tail arc
 * 16. PandaSit            — 43 blocks, sitting panda lunge (impact-only)
 * 17. GoldfishSwarm       — 48 blocks, orbiting fish school
 * 18. DeerAntlerCrash     — 34 blocks, antlers clashing inward (impact-only)
 * 19. WaddlePenguin       — 42 blocks, belly-flopping penguin (impact-only)
 * 20. DragonflyDive       — 28 blocks, hovering then diving (impact-only)
 */
public final class FluffyBlockDisplay2 {
    private FluffyBlockDisplay2() {}

    private static final String MODE_PATH = "modes/fluffy/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new GiantOwlForm(plugin));
        registry.register(new FluffySerpent(plugin));
        registry.register(new ButterflyWings(plugin));
        registry.register(new GiantHedgehog(plugin));
        registry.register(new FoxSweepTail(plugin));
        registry.register(new PandaSit(plugin));
        registry.register(new GoldfishSwarm(plugin));
        registry.register(new DeerAntlerCrash(plugin));
        registry.register(new WaddlePenguin(plugin));
        registry.register(new DragonflyDive(plugin));
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
    // #11 — GIANT OWL FORM
    // 65 blocks: perched owl. Round head (10 WHITE_WOOL), ear tufts (4 GRAY),
    // disc eyes (8 BLACK + center accents), beak (2 YELLOW), body (16),
    // wings folded (16), tail fan (5), talons (6).
    // Head rotates 180° L then 180° R via interpolation. Eyes glow.
    // 3.5r constant, 22.0 dmg, 15t interval, 25t delay.
    // ================================================================
    public static class GiantOwlForm extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> headBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> eyeBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> bodyBlocks = new ArrayList<>();
        private float headRotation = 0f;
        private boolean rotatingRight = false;

        public GiantOwlForm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("giant_owl_form", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(11.0);
            config.setDamageRadius(3.5);
            config.setTicksBetweenDamage(15);
            config.setDamageDelayTicks(25);
            config.setDurationTicks(360);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Talons: 6 BONE_BLOCK at base (3 each foot, Y=0)
            for (int i = 0; i < 6; i++) {
                double x = (i < 3 ? -0.6 : 0.6) + ((i % 3) - 1) * 0.25;
                Location loc = center.clone().add(x, 0.0, 0.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BONE_BLOCK);
                h.scale(0.25f, 0.4f, 0.25f).glow(240, 240, 220).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                bodyBlocks.add(h);
            }

            // Body: 16 gray-white blocks in oval (Y 0.6–2.4)
            double[][] bodyOffsets = {
                    {-0.5, 0.6, 0}, {0.5, 0.6, 0}, {0, 0.6, -0.4}, {0, 0.6, 0.4},
                    {-0.7, 1.2, 0}, {0.7, 1.2, 0}, {0, 1.2, -0.5}, {0, 1.2, 0.5},
                    {-0.7, 1.8, 0}, {0.7, 1.8, 0}, {0, 1.8, -0.5}, {0, 1.8, 0.5},
                    {-0.5, 2.4, 0}, {0.5, 2.4, 0}, {0, 2.4, -0.4}, {0, 2.4, 0.4}
            };
            for (int i = 0; i < bodyOffsets.length; i++) {
                Location loc = center.clone().add(bodyOffsets[i][0], bodyOffsets[i][1], bodyOffsets[i][2]);
                Material mat = (i % 2 == 0) ? Material.WHITE_WOOL : Material.LIGHT_GRAY_WOOL;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.55f, 0.55f, 0.55f).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                bodyBlocks.add(h);
            }

            // Wings folded: 8 each side (16 GRAY_WOOL), Y 1.0–2.0
            for (int side = -1; side <= 1; side += 2) {
                for (int i = 0; i < 8; i++) {
                    double yOff = 1.0 + (i / 2) * 0.35;
                    double xOff = side * (0.85 + (i % 2) * 0.18);
                    double zOff = (i % 2 == 0) ? -0.15 : 0.15;
                    Location loc = center.clone().add(xOff, yOff, zOff);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.GRAY_WOOL);
                    h.scale(0.4f, 0.5f, 0.4f).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    bodyBlocks.add(h);
                }
            }

            // Tail fan: 5 LIGHT_GRAY at back-bottom
            for (int i = 0; i < 5; i++) {
                double xOff = -0.5 + i * 0.25;
                Location loc = center.clone().add(xOff, 0.4, 0.55);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIGHT_GRAY_WOOL);
                h.scale(0.35f, 0.25f, 0.5f).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                bodyBlocks.add(h);
            }

            // Head: 10 WHITE_WOOL round at top
            double[][] headOffsets = {
                    {0, 3.0, 0}, {-0.4, 3.0, 0}, {0.4, 3.0, 0},
                    {0, 3.0, -0.4}, {0, 3.0, 0.4},
                    {-0.3, 3.4, -0.3}, {0.3, 3.4, -0.3},
                    {-0.3, 3.4, 0.3}, {0.3, 3.4, 0.3},
                    {0, 3.6, 0}
            };
            for (double[] off : headOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_WOOL);
                h.scale(0.5f, 0.5f, 0.5f).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                headBlocks.add(h);
            }

            // Ear tufts: 4 GRAY_WOOL (2 each side)
            double[][] earOffsets = {
                    {-0.5, 3.85, 0}, {-0.5, 4.1, 0},
                    {0.5, 3.85, 0}, {0.5, 4.1, 0}
            };
            for (double[] off : earOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.GRAY_WOOL);
                h.scale(0.22f, 0.38f, 0.22f).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                headBlocks.add(h);
            }

            // Eyes: 6 BLACK_CONCRETE disc + 2 YELLOW center = 8 (3 black per side + 1 yellow per side)
            double[][] eyeOffsets = {
                    {-0.35, 3.15, -0.45}, {-0.45, 3.15, -0.4}, {-0.35, 3.25, -0.45},
                    {0.35, 3.15, -0.45}, {0.45, 3.15, -0.4}, {0.35, 3.25, -0.45}
            };
            for (double[] off : eyeOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
                h.scale(0.18f, 0.18f, 0.12f).glow(255, 220, 0).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                eyeBlocks.add(h);
                headBlocks.add(h);
            }
            // 2 YELLOW pupils
            for (int side = -1; side <= 1; side += 2) {
                Location loc = center.clone().add(side * 0.4, 3.2, -0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.YELLOW_CONCRETE);
                h.scale(0.1f, 0.1f, 0.08f).glow(255, 230, 0).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                eyeBlocks.add(h);
                headBlocks.add(h);
            }

            // Beak: 2 YELLOW pyramid forward
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add(0, 3.05 - i * 0.15, -0.55);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.YELLOW_CONCRETE);
                h.scale(0.18f - i * 0.05f, 0.18f, 0.25f).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                headBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BAT_AMBIENT, 1.0f, 0.5f);
            w.spawnParticle(Particle.ENCHANT, center.clone().add(0, 3.2, 0), 30, 1.5, 1.5, 1.5, 0.5);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Head rotation: 180° left, pause, 180° right (60-tick cycle)
            int phase = tick % 90;
            if (phase == 0) {
                // Rotate to left (-180°) over 30 ticks
                headRotation = (float) -Math.PI;
                rotatingRight = false;
                animateHead(30);
            } else if (phase == 45) {
                // Rotate to right (+180°) over 30 ticks
                headRotation = (float) Math.PI;
                rotatingRight = true;
                animateHead(30);
            }

            // END_ROD eye particles every 4 ticks
            if (tick % 4 == 0) {
                for (int side = -1; side <= 1; side += 2) {
                    Location eyeLoc = c.clone().add(side * 0.4, 3.2, -0.55);
                    c.getWorld().spawnParticle(Particle.END_ROD, eyeLoc, 2, 0.05, 0.05, 0.1, 0.02);
                }
            }
            // Feather wisps
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.ENCHANT, c.clone().add(0, 2.5, 0), 8, 1.2, 1.2, 1.2, 0.3);
            }
            // Sound
            if (tick % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_BAT_AMBIENT, 0.7f, 0.5f);
            }
        }

        private void animateHead(int durationTicks) {
            for (BlockDisplayHandle h : headBlocks) {
                BlockDisplay e = h.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(durationTicks);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(headRotation, 0f, 1f, 0f),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new GiantOwlForm(plugin); }
    }

    // ================================================================
    // #12 — FLUFFY SERPENT
    // 44 blocks: 9 segments (4 LIME_WOOL each = 36, tapering scale),
    // 6 YELLOW diamond head, 2 RED forked tongue.
    // Body undulates via sin wave, head leads.
    // 2.0r constant along body, 18.0 dmg, 10t interval.
    // ================================================================
    public static class FluffySerpent extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> segments = new ArrayList<>();
        private final List<BlockDisplayHandle> head = new ArrayList<>();
        private final List<BlockDisplayHandle> tongue = new ArrayList<>();
        private final double[] segmentBaseX = new double[9];
        private final double[] segmentBaseZ = new double[9];

        public FluffySerpent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fluffy_serpent", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(9.0);
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(320);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 9 segments along Z axis, each 4 blocks (1×1 cross), tapering 1.2 → 0.6
            for (int s = 0; s < 9; s++) {
                List<BlockDisplayHandle> segBlocks = new ArrayList<>();
                float scale = 1.2f - (s * 0.07f);
                double zPos = -4.0 + s * 1.0;
                segmentBaseX[s] = 0;
                segmentBaseZ[s] = zPos;
                // S-curve: alternate slight X offset
                double xOff = Math.sin(s * 0.7) * 0.6;
                // 4 blocks per segment in 1x1 cross-section
                double[][] pts = {{0.2, 0}, {-0.2, 0}, {0, 0.2}, {0, -0.2}};
                for (double[] p : pts) {
                    Location loc = center.clone().add(xOff + p[0], 1.0 + p[1], zPos);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIME_WOOL);
                    h.scale(scale * 0.5f, scale * 0.5f, scale * 0.5f).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    segBlocks.add(h);
                }
                segments.add(segBlocks);
            }

            // Head: 6 YELLOW_CONCRETE diamond shape at front (z = +5)
            double[][] headOffsets = {
                    {0, 1.0, 5.0}, {0.3, 1.0, 5.3}, {-0.3, 1.0, 5.3},
                    {0, 1.3, 5.0}, {0, 0.7, 5.0}, {0, 1.0, 5.6}
            };
            for (double[] off : headOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.YELLOW_CONCRETE);
                h.scale(0.45f, 0.45f, 0.55f).glow(255, 230, 50).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                head.add(h);
            }

            // Forked tongue: 2 RED thin blocks
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add((i == 0 ? -0.15 : 0.15), 1.0, 6.1);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.RED_CONCRETE);
                h.scale(0.08f, 0.08f, 0.4f).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                tongue.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_AMBIENT, 1.0f, 1.6f);
            w.spawnParticle(Particle.SPORE_BLOSSOM_AIR, center.clone().add(0, 1, 0), 25, 3, 0.5, 3, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Body undulation: each segment Y follows sin(tick*0.1 + segment*0.7) * 0.8
            if (tick % 3 == 0) {
                for (int s = 0; s < segments.size(); s++) {
                    float yOff = (float) (Math.sin(tick * 0.1 + s * 0.7) * 0.8);
                    float xOff = (float) (Math.sin(tick * 0.08 + s * 0.5) * 0.4);
                    for (BlockDisplayHandle h : segments.get(s)) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(3);
                        e.setInterpolationDelay(0);
                        Vector3f curScale = t.getScale();
                        e.setTransformation(new Transformation(
                                new Vector3f(xOff - 0.5f, yOff - 0.5f, -0.5f),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                curScale,
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                }
            }

            // Particles
            if (tick % 5 == 0) {
                c.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, c.clone().add(0, 1.5, 0), 6, 3, 0.5, 4, 0.02);
            }
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.ENCHANT, c.clone().add(0, 1.0, 5.5), 5, 0.4, 0.4, 0.4, 0.5);
            }

            // Sound
            if (tick % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GUARDIAN_AMBIENT, 0.6f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FluffySerpent(plugin); }
    }

    // ================================================================
    // #13 — BUTTERFLY WINGS
    // 46 blocks: 2 wings (21 each = 42 PINK/PURPLE/LIGHT_BLUE) + 4 WHITE body.
    // Wings open from folded, then slow flap (X-axis 0–15° cycle).
    // 2.5r constant in body, 20.0 dmg, 12t interval, 30t delay.
    // ================================================================
    public static class ButterflyWings extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> leftWing = new ArrayList<>();
        private final List<BlockDisplayHandle> rightWing = new ArrayList<>();
        private final List<BlockDisplayHandle> body = new ArrayList<>();

        public ButterflyWings(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("butterfly_wings", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(10.0);
            config.setDamageRadius(2.5);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(360);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Body: 4 WHITE_WOOL vertical at center
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(0, 1.5 + i * 0.35, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_WOOL);
                h.scale(0.25f, 0.35f, 0.25f).glow(255, 255, 255).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                body.add(h);
            }

            // Wings: 21 blocks each side. Outer scallop (10), middle (7), inner (4)
            Material[] palette = {Material.PINK_CONCRETE, Material.PURPLE_CONCRETE, Material.LIGHT_BLUE_CONCRETE};
            for (int side = -1; side <= 1; side += 2) {
                List<BlockDisplayHandle> wingList = (side == -1) ? leftWing : rightWing;
                // outer row: 10 blocks (curved scallop edge)
                for (int i = 0; i < 10; i++) {
                    double yOff = 0.6 + i * 0.32;
                    double xOff = side * (2.4 - Math.abs(i - 4.5) * 0.2);
                    Location loc = center.clone().add(xOff, yOff, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, palette[i % 3]);
                    h.scale(0.45f, 0.45f, 0.18f).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    wingList.add(h);
                }
                // middle row: 7
                for (int i = 0; i < 7; i++) {
                    double yOff = 0.85 + i * 0.32;
                    double xOff = side * (1.6 - Math.abs(i - 3) * 0.15);
                    Location loc = center.clone().add(xOff, yOff, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, palette[(i + 1) % 3]);
                    h.scale(0.42f, 0.42f, 0.18f).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    wingList.add(h);
                }
                // inner row: 4
                for (int i = 0; i < 4; i++) {
                    double yOff = 1.2 + i * 0.32;
                    double xOff = side * (0.85);
                    Location loc = center.clone().add(xOff, yOff, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, palette[(i + 2) % 3]);
                    h.scale(0.38f, 0.38f, 0.18f).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    wingList.add(h);
                }
                // start folded: rotate wings 90° on X-axis (vertical fold)
                float foldAngle = (float) Math.toRadians(90);
                for (BlockDisplayHandle h : wingList) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(foldAngle, 0, 0, side),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Open wings over 20 ticks
            for (int side = -1; side <= 1; side += 2) {
                List<BlockDisplayHandle> wingList = (side == -1) ? leftWing : rightWing;
                for (BlockDisplayHandle h : wingList) {
                    h.animateTo(
                            new Vector3f(-0.5f, -0.5f, -0.5f),
                            new AxisAngle4f(0, 0, 0, 1),
                            h.entity().getTransformation().getScale(),
                            20
                    );
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BEE_LOOP, 1.0f, 1.4f);
            w.spawnParticle(Particle.ENCHANT, center.clone().add(0, 2, 0), 30, 2, 1, 0.5, 1.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Wing flap: 0° → 15° → 0° on X-axis, 20-tick period after open delay
            if (tick > 20 && tick % 20 == 0) {
                float flapAngle = (float) Math.toRadians(15);
                for (int side = -1; side <= 1; side += 2) {
                    List<BlockDisplayHandle> wingList = (side == -1) ? leftWing : rightWing;
                    for (BlockDisplayHandle h : wingList) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(15);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f(flapAngle, 0, 0, side),
                                t.getScale(),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                }
            }
            if (tick > 20 && tick % 20 == 10) {
                for (int side = -1; side <= 1; side += 2) {
                    List<BlockDisplayHandle> wingList = (side == -1) ? leftWing : rightWing;
                    for (BlockDisplayHandle h : wingList) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(15);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f(0, 0, 0, 1),
                                t.getScale(),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                }
            }

            // DUST color-shifted on wing edges
            if (tick % 2 == 0) {
                int hue = (tick * 4) % 360;
                int r = (int) (200 + 55 * Math.sin(hue * 0.0174));
                int g = (int) (150 + 100 * Math.sin(hue * 0.0174 + 2.0));
                int b = (int) (200 + 55 * Math.cos(hue * 0.0174));
                DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), 4, 2.5,
                        Math.max(0, Math.min(255, r)),
                        Math.max(0, Math.min(255, g)),
                        Math.max(0, Math.min(255, b)), 1.0f);
            }
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.ENCHANT, c.clone().add(0, 2.0, 0), 5, 2, 1, 0.5, 0.5);
            }

            if (tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_BEE_LOOP, 0.6f, 1.4f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ButterflyWings(plugin); }
    }

    // ================================================================
    // #14 — GIANT HEDGEHOG
    // 46 blocks: 14 BROWN_WOOL body, 30 BONE spines (2 each), 2 PINK snout.
    // Spines extend out, body inflates, then rolls toward player.
    // 3.0r constant when rolling, 25.0 dmg, 8t interval.
    // ================================================================
    public static class GiantHedgehog extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> bodyBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> spines = new ArrayList<>();
        private final List<double[]> spineDirs = new ArrayList<>();
        private final List<BlockDisplayHandle> snout = new ArrayList<>();
        private double rollX = 0, rollZ = 0;
        // Cached spawn-time roll direction (set in onSpawn, used by onTick).
        private double cachedDirX = 0, cachedDirZ = 0;

        public GiantHedgehog(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("giant_hedgehog", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(12.5);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(8);
            config.setDurationTicks(320);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Body: 14 BROWN_WOOL in oblate sphere
            double[][] bodyOffsets = {
                    {0, 1.0, 0}, {0.7, 1.0, 0}, {-0.7, 1.0, 0},
                    {0, 1.0, 0.7}, {0, 1.0, -0.7},
                    {0.5, 1.0, 0.5}, {-0.5, 1.0, 0.5},
                    {0.5, 1.0, -0.5}, {-0.5, 1.0, -0.5},
                    {0.4, 1.5, 0.4}, {-0.4, 1.5, 0.4},
                    {0.4, 1.5, -0.4}, {-0.4, 1.5, -0.4},
                    {0, 1.7, 0}
            };
            for (double[] off : bodyOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BROWN_WOOL);
                h.scale(0.7f, 0.7f, 0.7f).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                bodyBlocks.add(h);
            }

            // 15 spine pairs (30 BONE_BLOCK total) radial pattern
            for (int i = 0; i < 15; i++) {
                double theta = (2 * Math.PI * i) / 15;
                double phi = (i % 2 == 0) ? Math.PI / 4 : Math.PI / 2.5;
                double dx = Math.cos(theta) * Math.sin(phi);
                double dz = Math.sin(theta) * Math.sin(phi);
                double dy = Math.cos(phi);
                // 2 BONE_BLOCK per spine (inner + outer tip)
                for (int s = 0; s < 2; s++) {
                    double r = 0.7 + s * 0.5;
                    Location loc = center.clone().add(dx * r, 1.2 + dy * r, dz * r);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BONE_BLOCK);
                    // Compute angle pointing outward
                    float spineAngle = (float) Math.atan2(dx, dz);
                    h.scale(0.15f, 0.15f, 1.0f).rotate(spineAngle, 0, 1, 0).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    spines.add(h);
                    spineDirs.add(new double[]{dx, dy, dz});
                }
            }

            // Snout: 2 PINK_CONCRETE
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add(0, 1.0 + i * 0.18, -0.85);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PINK_CONCRETE);
                h.scale(0.18f - i * 0.06f, 0.18f, 0.22f).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                snout.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ARMADILLO_ROLL, 1.0f, 0.6f);
            w.spawnParticle(Particle.CRIT, center.clone().add(0, 1, 0), 25, 1.5, 1.5, 1.5, 0.2);

            // Cache spawn-time direction toward nearest player (used for the roll
            // phase — direction is fixed at spawn so the hedgehog rolls in a
            // straight line and does not chase the player).
            Player initialTarget = findNearestPlayer(center, 25.0);
            if (initialTarget != null) {
                Vector dir = initialTarget.getLocation().toVector().subtract(center.toVector());
                if (dir.lengthSquared() > 0.01) {
                    dir = dir.normalize();
                    cachedDirX = dir.getX();
                    cachedDirZ = dir.getZ();
                }
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1 (0-30): roll into ball — body inflates 1.0→1.3, spines retract
            if (tick == 30) {
                for (BlockDisplayHandle h : bodyBlocks) {
                    h.animateTo(
                            h.entity().getTransformation().getTranslation(),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.9f, 0.9f, 0.9f),
                            10
                    );
                }
                // Spines retract slightly inward
                for (int i = 0; i < spines.size(); i++) {
                    BlockDisplayHandle h = spines.get(i);
                    double[] dir = spineDirs.get(i);
                    h.animateTo(
                            new Vector3f((float) (dir[0] * 0.3 - 0.5), (float) (dir[1] * 0.3 - 0.5), (float) (dir[2] * 0.3 - 0.5)),
                            new AxisAngle4f((float) Math.atan2(dir[0], dir[2]), 0, 1, 0),
                            new Vector3f(0.12f, 0.12f, 0.7f),
                            10
                    );
                }
            }

            // Phase 2 (40+): roll along the cached spawn-time direction at 0.1/tick.
            // (Direction is fixed at spawn — does not chase the player live.)
            if (tick > 40 && tick % 2 == 0 && (cachedDirX != 0 || cachedDirZ != 0)) {
                {
                    rollX += cachedDirX * 0.1;
                    rollZ += cachedDirZ * 0.1;
                    // Apply translation to all body and spines
                    for (BlockDisplayHandle h : bodyBlocks) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        Vector3f cur = t.getTranslation();
                        e.setInterpolationDuration(2);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                new Vector3f((float) (cur.x() + cachedDirX * 0.1), cur.y(), (float) (cur.z() + cachedDirZ * 0.1)),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                t.getScale(),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                    for (BlockDisplayHandle h : spines) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        Vector3f cur = t.getTranslation();
                        e.setInterpolationDuration(2);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                new Vector3f((float) (cur.x() + cachedDirX * 0.1), cur.y(), (float) (cur.z() + cachedDirZ * 0.1)),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                t.getScale(),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                    for (BlockDisplayHandle h : snout) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        Vector3f cur = t.getTranslation();
                        e.setInterpolationDuration(2);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                new Vector3f((float) (cur.x() + cachedDirX * 0.1), cur.y(), (float) (cur.z() + cachedDirZ * 0.1)),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                t.getScale(),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                }
            }

            // CRIT particles when rolling, BLOCK(DIRT) trail
            if (tick > 30 && tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.CRIT,
                        c.clone().add(rollX, 1.0, rollZ), 6, 0.5, 0.5, 0.5, 0.1);
                c.getWorld().spawnParticle(Particle.BLOCK,
                        c.clone().add(rollX, 0.2, rollZ), 8, 0.7, 0.1, 0.7, 0.05,
                        Material.DIRT.createBlockData());
            }

            if (tick % 15 == 0 && tick > 30) {
                DisplayBuilder.playSound(c, Sound.ENTITY_SLIME_JUMP, 0.7f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new GiantHedgehog(plugin); }
    }

    // ================================================================
    // #15 — FOX SWEEP TAIL
    // 24 blocks: 8 ORANGE thick base, 10 graduated body, 6 WHITE tip.
    // Sweeps 180° horizontal arc -90° → +90° → -90°.
    // 1.5r constant along tail, 24.0 dmg, 6t interval.
    // ================================================================
    public static class FoxSweepTail extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> tail = new ArrayList<>();
        private float currentAngle = (float) Math.toRadians(-90);
        private boolean sweepingRight = true;

        public FoxSweepTail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fox_sweep_tail", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(12.0);
            config.setDamageRadius(1.5);
            config.setTicksBetweenDamage(6);
            config.setDurationTicks(280);
            config.setCooldownTicks(220);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Tail extends along +Z from center (pivot at center)
            // 8 ORANGE base (z = 0.0 to 1.4, scale 1.4)
            for (int i = 0; i < 8; i++) {
                double zOff = 0.2 + i * 0.18;
                Location loc = center.clone().add(0, 1.0, zOff);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ORANGE_WOOL);
                h.scale(0.55f, 0.55f, 0.4f).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                tail.add(h);
            }
            // 10 graduated body (1.4→0.6 scale)
            for (int i = 0; i < 10; i++) {
                float scale = 0.55f - (i * 0.03f);
                double zOff = 1.4 + i * 0.22;
                Location loc = center.clone().add(0, 1.0, zOff);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ORANGE_WOOL);
                h.scale(scale, scale, 0.35f).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                tail.add(h);
            }
            // 6 WHITE tip
            for (int i = 0; i < 6; i++) {
                float scale = 0.28f - (i * 0.025f);
                double zOff = 3.6 + i * 0.18;
                Location loc = center.clone().add(0, 1.0, zOff);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_WOOL);
                h.scale(scale, scale, 0.3f).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                tail.add(h);
            }

            // Initial angle: -90° (left side)
            applyAngle(currentAngle, 1);

            DisplayBuilder.playSound(center, Sound.ENTITY_FOX_AMBIENT, 1.0f, 1.0f);
            w.spawnParticle(Particle.ENCHANT, center.clone().add(0, 1.5, 1.5), 20, 1, 0.3, 1, 0.5);
        }

        private void applyAngle(float angle, int durationTicks) {
            for (BlockDisplayHandle h : tail) {
                BlockDisplay e = h.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(durationTicks);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(angle, 0f, 1f, 0f),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Sweep cycle: every 50 ticks, switch direction
            if (tick > 0 && tick % 50 == 0) {
                sweepingRight = !sweepingRight;
                currentAngle = (float) Math.toRadians(sweepingRight ? 90 : -90);
                applyAngle(currentAngle, 40);
                DisplayBuilder.playSound(c, Sound.ENTITY_FOX_BITE, 1.0f, 1.0f);
            }

            // Tail sweep particles every tick
            if (tick % 1 == 0) {
                double angle = currentAngle;
                for (int i = 0; i < 5; i++) {
                    double r = 1.0 + i * 0.7;
                    double x = -Math.sin(angle) * r;
                    double z = Math.cos(angle) * r;
                    Location pLoc = c.clone().add(x, 1.0, z);
                    c.getWorld().spawnParticle(Particle.ENCHANT, pLoc, 1, 0.1, 0.1, 0.1, 0.1);
                    DisplayBuilder.dustParticles(pLoc, 1, 0.15, 255, 140, 0, 1.0f);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FoxSweepTail(plugin); }
    }

    // ================================================================
    // #16 — PANDA SIT (impact-only on lunge)
    // 43 blocks: 16 WHITE body, 8 BLACK eye patches, 4 BLACK ears,
    // 8 BLACK arms, 6 WHITE legs, 1 PINK nose.
    // Idle 40 ticks, then LUNGE forward 3 blocks rapidly.
    // Impact 3.5r, 48.0 dmg.
    // ================================================================
    public static class PandaSit extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private boolean lunged = false;
        // Cached spawn-time lunge direction.
        private double cachedDirX = 0, cachedDirZ = -1;

        public PandaSit(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("panda_sit", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(24.0);
            config.setImpactRadius(3.5);
            config.setDurationTicks(180);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Body: 16 WHITE_WOOL round seated
            double[][] bodyOff = {
                    {-0.5, 0.6, 0}, {0.5, 0.6, 0}, {-0.5, 0.6, -0.4}, {0.5, 0.6, -0.4},
                    {-0.5, 0.6, 0.4}, {0.5, 0.6, 0.4},
                    {-0.7, 1.2, 0}, {0.7, 1.2, 0}, {0, 1.2, 0.4}, {0, 1.2, -0.4},
                    {-0.5, 1.2, 0.4}, {0.5, 1.2, 0.4},
                    {-0.5, 1.8, 0}, {0.5, 1.8, 0}, {0, 1.8, -0.4}, {0, 1.8, 0.4}
            };
            for (double[] off : bodyOff) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_WOOL);
                h.scale(0.55f, 0.55f, 0.55f).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // Eye patches: 8 BLACK (4 each)
            double[][] eyePatch = {
                    {-0.45, 2.4, -0.4}, {-0.55, 2.4, -0.3}, {-0.4, 2.55, -0.4}, {-0.5, 2.55, -0.3},
                    {0.45, 2.4, -0.4}, {0.55, 2.4, -0.3}, {0.4, 2.55, -0.4}, {0.5, 2.55, -0.3}
            };
            for (double[] off : eyePatch) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
                h.scale(0.18f, 0.18f, 0.12f).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // Ears: 4 BLACK (2 each)
            double[][] earOff = {
                    {-0.5, 2.85, 0}, {-0.5, 3.05, 0},
                    {0.5, 2.85, 0}, {0.5, 3.05, 0}
            };
            for (double[] off : earOff) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
                h.scale(0.22f, 0.22f, 0.22f).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // Arms: 8 BLACK in lap (4 each)
            double[][] armOff = {
                    {-0.7, 1.0, -0.6}, {-0.85, 1.0, -0.45}, {-0.7, 1.0, -0.3}, {-0.85, 1.0, -0.15},
                    {0.7, 1.0, -0.6}, {0.85, 1.0, -0.45}, {0.7, 1.0, -0.3}, {0.85, 1.0, -0.15}
            };
            for (double[] off : armOff) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
                h.scale(0.3f, 0.3f, 0.3f).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // Legs: 6 WHITE (3 each)
            double[][] legOff = {
                    {-0.4, 0.2, -0.5}, {-0.55, 0.2, -0.5}, {-0.4, 0.4, -0.55},
                    {0.4, 0.2, -0.5}, {0.55, 0.2, -0.5}, {0.4, 0.4, -0.55}
            };
            for (double[] off : legOff) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_WOOL);
                h.scale(0.3f, 0.3f, 0.3f).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // Nose: 1 PINK
            Location noseLoc = center.clone().add(0, 2.4, -0.5);
            BlockDisplayHandle nose = displayBuilder.spawnBlock(noseLoc, Material.PINK_CONCRETE);
            nose.scale(0.12f, 0.12f, 0.12f).interpolation(3, 0);
            spawnedEntities.add(nose.entity());
            allBlocks.add(nose);

            DisplayBuilder.playSound(center, Sound.ENTITY_PANDA_AMBIENT, 1.0f, 0.8f);
            w.spawnParticle(Particle.ENCHANT, center.clone().add(0, 1.5, 0), 25, 1.5, 1.5, 1.5, 0.5);

            // Cache spawn-time lunge direction (does not chase live).
            Player initialTarget = findNearestPlayer(center, 30.0);
            if (initialTarget != null) {
                Vector dir = initialTarget.getLocation().toVector().subtract(center.toVector());
                if (dir.lengthSquared() > 0.01) {
                    dir = dir.normalize();
                    cachedDirX = dir.getX();
                    cachedDirZ = dir.getZ();
                }
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Idle calm sparkle 0-39
            if (tick < 40 && tick % 5 == 0) {
                c.getWorld().spawnParticle(Particle.ENCHANT, c.clone().add(0, 1.5, 0), 6, 1.0, 1.0, 1.0, 0.4);
            }

            // Grumble before lunge
            if (tick == 35) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PANDA_CANT_BREED, 1.2f, 0.7f);
            }

            // LUNGE at tick 40 — translate forward 3 blocks along cached direction.
            if (tick == 40 && !lunged) {
                lunged = true;
                Vector dir = new Vector(cachedDirX, 0, cachedDirZ);
                float dx = (float) (dir.getX() * 3.0);
                float dz = (float) (dir.getZ() * 3.0);
                for (BlockDisplayHandle h : allBlocks) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    Vector3f cur = t.getTranslation();
                    e.setInterpolationDuration(2);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(cur.x() + dx, cur.y(), cur.z() + dz),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
                Location impactLoc = c.clone().add(dir.getX() * 3.0, 1.0, dir.getZ() * 3.0);
                triggerImpactDamage(impactLoc);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_PANDA_BITE, 1.3f, 1.0f);
                c.getWorld().spawnParticle(Particle.CRIT, impactLoc, 40, 2, 2, 2, 0.3);
            }

            // Retract 20 ticks after lunge
            if (tick == 60) {
                for (BlockDisplayHandle h : allBlocks) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    Vector3f cur = t.getTranslation();
                    e.setInterpolationDuration(20);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(-0.5f, cur.y(), -0.5f),
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
        public AbstractAttack newInstance() { return new PandaSit(plugin); }
    }

    // ================================================================
    // #17 — GOLDFISH SWARM
    // 48 blocks: 8 fish (4 GOLD body + 2 ORANGE tail = 6 each).
    // School orbits center, periodically contracts to crush.
    // 2.0r constant at center crush (and 4.5r orbit zone), 24.0 dmg, 8t interval.
    // ================================================================
    public static class GoldfishSwarm extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> fish = new ArrayList<>();
        private double currentRadius = 4.0;

        public GoldfishSwarm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("goldfish_swarm", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(12.0);
            config.setDamageRadius(4.5);
            config.setTicksBetweenDamage(8);
            config.setDurationTicks(360);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            for (int f = 0; f < 8; f++) {
                List<BlockDisplayHandle> fishBlocks = new ArrayList<>();
                double angle = (2 * Math.PI * f) / 8;
                double fx = Math.cos(angle) * 4.0;
                double fz = Math.sin(angle) * 4.0;
                // Body: 4 GOLD_BLOCK oval
                double[][] bodyPts = {{0, 0}, {0.25, 0.05}, {-0.25, 0.05}, {0, 0.15}};
                for (double[] p : bodyPts) {
                    Location loc = center.clone().add(fx + p[0], 1.5, fz + p[1]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.GOLD_BLOCK);
                    h.scale(0.3f, 0.3f, 0.4f).glow(255, 200, 50).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    fishBlocks.add(h);
                }
                // Tail: 2 ORANGE fan
                for (int t = 0; t < 2; t++) {
                    double tOff = 0.25 + t * 0.15;
                    Location loc = center.clone().add(fx, 1.5 + (t == 0 ? 0.1 : -0.1), fz + tOff);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ORANGE_CONCRETE);
                    h.scale(0.18f, 0.32f, 0.18f).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    fishBlocks.add(h);
                }
                fish.add(fishBlocks);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_FISH_SWIM, 1.0f, 1.5f);
            w.spawnParticle(Particle.BUBBLE, center.clone().add(0, 1.5, 0), 25, 4, 0.5, 4, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Contract every 60 ticks: school crushes to radius 1.5 then expands back
            if (tick % 60 == 0 && tick > 0) {
                currentRadius = 1.5;
            } else if (tick % 60 == 20) {
                currentRadius = 4.0;
            }

            // Move each fish to its orbit position via interpolation
            if (tick % 3 == 0) {
                double speed = 0.05;
                for (int f = 0; f < fish.size(); f++) {
                    double phase = (2 * Math.PI * f) / 8;
                    double angle = phase + tick * speed;
                    double fx = Math.cos(angle) * currentRadius;
                    double fz = Math.sin(angle) * currentRadius;
                    int idx = 0;
                    double[][] bodyPts = {{0, 0}, {0.25, 0.05}, {-0.25, 0.05}, {0, 0.15}};
                    for (double[] p : bodyPts) {
                        BlockDisplay e = fish.get(f).get(idx++).entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(3);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                new Vector3f((float) (fx + p[0]) - 0.5f, 1.5f - 0.5f, (float) (fz + p[1]) - 0.5f),
                                new AxisAngle4f((float) angle, 0, 1, 0),
                                t.getScale(),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                    // Tail
                    for (int ti = 0; ti < 2; ti++) {
                        double tOff = 0.25 + ti * 0.15;
                        BlockDisplay e = fish.get(f).get(idx++).entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(3);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                new Vector3f((float) fx - 0.5f, (1.5f + (ti == 0 ? 0.1f : -0.1f)) - 0.5f, (float) (fz + tOff) - 0.5f),
                                new AxisAngle4f((float) angle, 0, 1, 0),
                                t.getScale(),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                }
            }

            // Particles
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.BUBBLE,
                        c.clone().add(0, 1.5, 0), 6, currentRadius, 0.3, currentRadius, 0.05);
                c.getWorld().spawnParticle(Particle.ENCHANT,
                        c.clone().add(0, 1.5, 0), 4, currentRadius * 0.6, 0.3, currentRadius * 0.6, 0.2);
            }

            if (tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_FISH_SWIM, 0.7f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new GoldfishSwarm(plugin); }
    }

    // ================================================================
    // #18 — DEER ANTLER CRASH (impact-only)
    // 34 blocks: 2 antlers — 5 BONE main spike + 12 branches each = 17 per side.
    // Rise from opposite sides, arc inward, then clash at center.
    // Impact 5.0r, 60.0 dmg at clash.
    // ================================================================
    public static class DeerAntlerCrash extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> leftAntler = new ArrayList<>();
        private final List<BlockDisplayHandle> rightAntler = new ArrayList<>();
        private boolean crashed = false;

        public DeerAntlerCrash(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("deer_antler_crash", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(30.0);
            config.setImpactRadius(5.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Left antler: x = -3.0, angled inward 15°
            double[] sides = {-3.0, 3.0};
            for (int side = 0; side < 2; side++) {
                double sx = sides[side];
                List<BlockDisplayHandle> list = (side == 0) ? leftAntler : rightAntler;
                int dir = (side == 0) ? 1 : -1;
                // 5 main spike blocks (vertical, slight outward angle 15°)
                for (int i = 0; i < 5; i++) {
                    double yOff = 0.5 + i * 0.7;
                    double xOff = sx + dir * (i * 0.18);
                    Location loc = center.clone().add(xOff, yOff, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BONE_BLOCK);
                    h.scale(0.4f, 0.7f, 0.4f).glow(240, 240, 220).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    list.add(h);
                }
                // 3 branch pairs (each = 2 blocks at 45°) = 6 branches × 2 blocks = 12
                for (int b = 0; b < 3; b++) {
                    double baseY = 1.5 + b * 0.9;
                    double baseX = sx + dir * (b * 0.18 + 0.5);
                    // Branch pair: forward and back
                    for (int br = 0; br < 6; br++) {
                        double forwardZ = (br < 3) ? 0.5 + (br * 0.3) : -(0.5 + ((br - 3) * 0.3));
                        double brX = baseX + dir * (Math.abs(br - 2.5) * 0.2);
                        Location loc = center.clone().add(brX, baseY + Math.abs(br - 2.5) * 0.15, forwardZ);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BONE_BLOCK);
                        h.scale(0.22f, 0.22f, 0.5f).glow(240, 240, 220).interpolation(3, 0);
                        spawnedEntities.add(h.entity());
                        list.add(h);
                    }
                }
            }

            // Rise from below: animateTo Y=actual over 15 ticks
            for (BlockDisplayHandle h : leftAntler) {
                BlockDisplay e = h.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(0);
                e.setTransformation(new Transformation(
                        new Vector3f(-0.5f, -3.5f, -0.5f),
                        t.getLeftRotation(), t.getScale(), t.getRightRotation()
                ));
                e.setInterpolationDuration(15);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        new Vector3f(-0.5f, -0.5f, -0.5f),
                        t.getLeftRotation(), t.getScale(), t.getRightRotation()
                ));
            }
            for (BlockDisplayHandle h : rightAntler) {
                BlockDisplay e = h.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(0);
                e.setTransformation(new Transformation(
                        new Vector3f(-0.5f, -3.5f, -0.5f),
                        t.getLeftRotation(), t.getScale(), t.getRightRotation()
                ));
                e.setInterpolationDuration(15);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        new Vector3f(-0.5f, -0.5f, -0.5f),
                        t.getLeftRotation(), t.getScale(), t.getRightRotation()
                ));
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BONE_BLOCK_PLACE, 1.0f, 0.7f);
            w.spawnParticle(Particle.ENCHANT, center.clone().add(0, 1.5, 0), 30, 3, 1, 1, 0.5);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase: rise (0-15), arc inward (15-75), crash (75)
            if (tick > 15 && tick <= 75) {
                // Slowly arc inward — translate toward center over remaining ticks
                if (tick == 16) {
                    for (BlockDisplayHandle h : leftAntler) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(60);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                new Vector3f(2.5f, -0.5f, -0.5f),
                                t.getLeftRotation(), t.getScale(), t.getRightRotation()
                        ));
                    }
                    for (BlockDisplayHandle h : rightAntler) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(60);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                new Vector3f(-2.5f, -0.5f, -0.5f),
                                t.getLeftRotation(), t.getScale(), t.getRightRotation()
                        ));
                    }
                }
            }

            // CRASH at tick 75: rapid 2-tick translate to same point + impact
            if (tick == 75 && !crashed) {
                crashed = true;
                for (BlockDisplayHandle h : leftAntler) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(2);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(3.0f, -0.5f, -0.5f),
                            t.getLeftRotation(), t.getScale(), t.getRightRotation()
                    ));
                }
                for (BlockDisplayHandle h : rightAntler) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(2);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(-3.0f, -0.5f, -0.5f),
                            t.getLeftRotation(), t.getScale(), t.getRightRotation()
                    ));
                }
                Location impactLoc = c.clone().add(0, 2.0, 0);
                triggerImpactDamage(impactLoc);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_IRON_GOLEM_DEATH, 1.4f, 1.8f);
                c.getWorld().spawnParticle(Particle.CRIT, impactLoc, 60, 3, 2, 3, 0.5);
                c.getWorld().spawnParticle(Particle.SMOKE, impactLoc, 40, 2, 2, 2, 0.1);
            }

            // Particles during rise/arc
            if (tick < 75 && tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.ENCHANT, c.clone().add(0, 2, 0), 5, 2.5, 1, 1, 0.4);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new DeerAntlerCrash(plugin); }
    }

    // ================================================================
    // #19 — WADDLE PENGUIN (impact-only on belly-flop)
    // 42 blocks: round body (14 WHITE + 10 BLACK), 6 BLACK flippers,
    // 4 ORANGE feet, head 6, beak 2 ORANGE.
    // Waddles forward, then belly-flops.
    // Impact 4.0r, 40.0 dmg.
    // ================================================================
    public static class WaddlePenguin extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private boolean flopped = false;
        private float waddleAngle = 0f;
        private float walkX = 0, walkZ = 0;
        // Cached spawn-time waddle/flop direction.
        private double cachedDirX = 0, cachedDirZ = -1;

        public WaddlePenguin(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("waddle_penguin", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(20.0);
            config.setImpactRadius(4.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Body: 14 WHITE + 10 BLACK in oval (24 blocks)
            for (int i = 0; i < 14; i++) {
                double angle = (2 * Math.PI * i) / 14;
                double r = 0.6;
                double xOff = Math.cos(angle) * r * 0.7;
                double yOff = 0.8 + (i / 7) * 0.6;
                double zOff = Math.sin(angle) * r * 0.5;
                Location loc = center.clone().add(xOff, yOff, zOff);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_WOOL);
                h.scale(0.5f, 0.5f, 0.5f).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }
            for (int i = 0; i < 10; i++) {
                double angle = (2 * Math.PI * i) / 10;
                double r = 0.7;
                double xOff = Math.cos(angle) * r;
                double yOff = 0.7 + (i / 5) * 0.7;
                double zOff = Math.sin(angle) * r * 0.5 + 0.3;
                Location loc = center.clone().add(xOff, yOff, zOff);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
                h.scale(0.45f, 0.45f, 0.45f).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // Flippers: 6 BLACK (3 each side)
            for (int side = -1; side <= 1; side += 2) {
                for (int i = 0; i < 3; i++) {
                    Location loc = center.clone().add(side * 0.85, 1.0 + i * 0.35, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
                    h.scale(0.18f, 0.4f, 0.3f).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    allBlocks.add(h);
                }
            }

            // Feet: 4 ORANGE (2 each)
            for (int side = -1; side <= 1; side += 2) {
                for (int i = 0; i < 2; i++) {
                    Location loc = center.clone().add(side * 0.3, 0.1, -0.2 + i * 0.3);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ORANGE_CONCRETE);
                    h.scale(0.22f, 0.12f, 0.35f).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    allBlocks.add(h);
                }
            }

            // Head: 6 mixed
            double[][] headOff = {
                    {0, 2.4, 0}, {-0.3, 2.4, 0}, {0.3, 2.4, 0},
                    {0, 2.7, 0}, {-0.2, 2.4, -0.25}, {0.2, 2.4, -0.25}
            };
            Material[] headMat = {Material.BLACK_CONCRETE, Material.BLACK_CONCRETE, Material.BLACK_CONCRETE,
                    Material.BLACK_CONCRETE, Material.WHITE_WOOL, Material.WHITE_WOOL};
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(headOff[i][0], headOff[i][1], headOff[i][2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, headMat[i]);
                h.scale(0.4f, 0.4f, 0.4f).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // Beak: 2 ORANGE
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add(0, 2.45 - i * 0.12, -0.45);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ORANGE_CONCRETE);
                h.scale(0.16f - i * 0.04f, 0.13f, 0.22f).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_PANDA_CANT_BREED, 1.0f, 1.5f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 1.5, 0), 20, 1.5, 1.5, 1.5, 0.05);

            // Cache spawn-time direction (waddle and flop both use this fixed dir).
            Player initialTarget = findNearestPlayer(center, 30.0);
            if (initialTarget != null) {
                Vector dir = initialTarget.getLocation().toVector().subtract(center.toVector());
                if (dir.lengthSquared() > 0.01) {
                    dir = dir.normalize();
                    cachedDirX = dir.getX();
                    cachedDirZ = dir.getZ();
                }
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Waddle 0-60: oscillate Z-axis ±8°, slowly travel along cached direction.
            if (tick < 60) {
                if (tick % 5 == 0) {
                    waddleAngle = (float) (Math.toRadians(8) * Math.sin(tick * 0.3));
                    Vector dir = new Vector(cachedDirX, 0, cachedDirZ);
                    walkX += dir.getX() * 0.2;
                    walkZ += dir.getZ() * 0.2;
                    for (BlockDisplayHandle h : allBlocks) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        Vector3f cur = t.getTranslation();
                        e.setInterpolationDuration(5);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                new Vector3f(walkX - 0.5f, cur.y(), walkZ - 0.5f),
                                new AxisAngle4f(waddleAngle, 0, 0, 1),
                                t.getScale(),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                }
                if (tick % 15 == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_PANDA_CANT_BREED, 0.6f, 1.5f);
                }
            }

            // BELLY-FLOP at tick 60 — uses cached spawn-time direction.
            if (tick == 60 && !flopped) {
                flopped = true;
                Vector dir = new Vector(cachedDirX, 0, cachedDirZ);
                float dx = (float) (dir.getX() * 4.0);
                float dz = (float) (dir.getZ() * 4.0);
                float flopAngle = (float) Math.toRadians(90);
                for (BlockDisplayHandle h : allBlocks) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    Vector3f cur = t.getTranslation();
                    e.setInterpolationDuration(8);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(cur.x() + dx, cur.y() - 0.5f, cur.z() + dz),
                            new AxisAngle4f(flopAngle, 1, 0, 0),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
                Location impactLoc = c.clone().add(walkX + dx, 1, walkZ + dz);
                triggerImpactDamage(impactLoc);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_BIG_FALL, 1.4f, 1.0f);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, impactLoc, 50, 3, 1, 3, 0.2);
                c.getWorld().spawnParticle(Particle.CRIT, impactLoc, 30, 2, 1, 2, 0.3);
            }

            // SNOWFLAKE slide trail during/after flop
            if (tick > 60 && tick % 2 == 0) {
                c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        c.clone().add(walkX, 0.5, walkZ), 6, 0.8, 0.3, 0.8, 0.05);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new WaddlePenguin(plugin); }
    }

    // ================================================================
    // #20 — DRAGONFLY DIVE (impact-only on dive)
    // 28 blocks: 8 LIGHT_BLUE body segmented, 12 main wings + 8 secondary.
    // Hovers Y+6, tracks player, then dives nose-down.
    // Impact 2.5r, 44.0 dmg on dive.
    // ================================================================
    public static class DragonflyDive extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> body = new ArrayList<>();
        private final List<BlockDisplayHandle> wings = new ArrayList<>();
        private boolean dived = false;
        private float trackX = 0, trackZ = 0;

        public DragonflyDive(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragonfly_dive", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(22.0);
            config.setImpactRadius(2.5);
            config.setDurationTicks(180);
            config.setCooldownTicks(220);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Body: 8 LIGHT_BLUE segments, stepped scale 0.7→0.3, along Z at Y+6
            for (int i = 0; i < 8; i++) {
                float scale = 0.7f - i * 0.05f;
                double zOff = -1.5 + i * 0.4;
                Location loc = center.clone().add(0, 6.0, zOff);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIGHT_BLUE_WOOL);
                h.scale(scale, scale, scale).glow(150, 200, 255).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                body.add(h);
            }

            // Main wings: 2 (6 blocks each = 12), wide horizontal
            for (int side = -1; side <= 1; side += 2) {
                for (int i = 0; i < 6; i++) {
                    double xOff = side * (0.4 + i * 0.35);
                    Location loc = center.clone().add(xOff, 6.0, -0.3);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_CONCRETE);
                    h.scale(2.0f, 0.08f, 1.0f).glow(0, 255, 255).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    wings.add(h);
                }
            }
            // Secondary wings: 2 (4 each = 8), behind main, angled
            for (int side = -1; side <= 1; side += 2) {
                for (int i = 0; i < 4; i++) {
                    double xOff = side * (0.4 + i * 0.3);
                    Location loc = center.clone().add(xOff, 6.0, 0.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_CONCRETE);
                    h.scale(1.6f, 0.08f, 0.7f).glow(0, 220, 255).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    wings.add(h);
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BEE_LOOP, 1.0f, 1.8f);
            w.spawnParticle(Particle.BUBBLE, center.clone().add(0, 6, 0), 25, 2, 1, 2, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Hover phase 0-40: wings flap ±20° on Y, track player slowly at Y+6
            if (tick < 40) {
                // Wing flap
                if (tick % 6 == 0) {
                    float flapAngle = (float) Math.toRadians(20 * Math.sin(tick * 0.5));
                    for (BlockDisplayHandle h : wings) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(6);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f(flapAngle, 0, 1, 0),
                                t.getScale(),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                }
                // Setpiece hover: stays at spawn location (no per-tick player tracking).
                // Bubble trail during hover
                if (tick % 4 == 0) {
                    c.getWorld().spawnParticle(Particle.BUBBLE, c.clone().add(trackX, 6, trackZ), 4, 0.5, 0.3, 0.5, 0.02);
                }
            }

            // Dive prep tick 40: rotate nose-down 0°→-80° over 5 ticks
            if (tick == 40) {
                float diveRot = (float) Math.toRadians(-80);
                for (BlockDisplayHandle h : body) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(5);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(diveRot, 1, 0, 0),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // DIVE tick 45: rapid translate Y+6 → Y-0.5 over 8 ticks
            if (tick == 45 && !dived) {
                dived = true;
                for (BlockDisplayHandle h : body) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    Vector3f cur = t.getTranslation();
                    e.setInterpolationDuration(8);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(cur.x(), -7.0f, cur.z()),
                            t.getLeftRotation(),
                            t.getScale(),
                            t.getRightRotation()
                    ));
                }
                for (BlockDisplayHandle h : wings) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    Vector3f cur = t.getTranslation();
                    e.setInterpolationDuration(8);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(cur.x(), -7.0f, cur.z()),
                            t.getLeftRotation(),
                            t.getScale(),
                            t.getRightRotation()
                    ));
                }
            }

            // Impact at tick 53 (dive lands)
            if (tick == 53 && dived) {
                Location impactLoc = c.clone().add(trackX, 0, trackZ);
                triggerImpactDamage(impactLoc);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_BEE_STING, 1.4f, 1.8f);
                c.getWorld().spawnParticle(Particle.CRIT, impactLoc, 40, 2, 1, 2, 0.3);
                c.getWorld().spawnParticle(Particle.SONIC_BOOM, impactLoc, 1, 0, 0, 0, 0);
            }

            // Rise back up after impact
            if (tick == 80) {
                for (BlockDisplayHandle h : body) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    Vector3f cur = t.getTranslation();
                    e.setInterpolationDuration(20);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(cur.x(), 0f, cur.z()),
                            t.getLeftRotation(),
                            t.getScale(),
                            t.getRightRotation()
                    ));
                }
                for (BlockDisplayHandle h : wings) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    Vector3f cur = t.getTranslation();
                    e.setInterpolationDuration(20);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(cur.x(), 0f, cur.z()),
                            t.getLeftRotation(),
                            t.getScale(),
                            t.getRightRotation()
                    ));
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new DragonflyDive(plugin); }
    }
}
