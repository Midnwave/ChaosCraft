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
 * Fluffy Mode — BLOCK DISPLAY ATTACKS Pt. 3 (attacks 21–30, Furniture & Play).
 * 10 cute-but-deadly themed BlockDisplay attacks with 25+ block displays each.
 * All animations via Transformation (AxisAngle4f rotation, Vector3f scale/translation).
 * Never teleport for rotation — use setInterpolationDuration for smooth animation.
 *
 * Fluffy palette:
 * - Cherry pink: RGB(255, 180, 200)
 * - Cream white: RGB(250, 240, 220)
 * - Pastel blue: RGB(180, 220, 255)
 * - Soft gold: RGB(255, 220, 130)
 * - Plush purple: RGB(200, 160, 240)
 *
 * Attacks:
 * 21. CarouselSpin           — 76 blocks, rotating carousel with bobbing horses
 * 22. GiantMousetrap         — 25 blocks, snapping spring trap (impact-only)
 * 23. CatTowerStructure      — 49 blocks, 3-tier scratching tower with topple
 * 24. HamsterWheelCage       — 51 blocks, spinning wheel inside cage frame
 * 25. BabyMobile             — 45 blocks, descending mobile with rotating toys
 * 26. GiantCradle            — 42 blocks, rocking cradle structure
 * 27. ToyChestLaunch         — 36 blocks, chest with flying toys (impact-only)
 * 28. PinwheelGarden         — 55 blocks, 5 pinwheels spinning at varied speeds
 * 29. JackInBox              — 52 blocks, winding box with popping head (impact-only)
 * 30. DollhouseStructure     — 66 blocks, 2-story dollhouse with collapsing walls
 */
public final class FluffyBlockDisplay3 {
    private FluffyBlockDisplay3() {}

    private static final String MODE_PATH = "modes/fluffy/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CarouselSpin(plugin));
        registry.register(new GiantMousetrap(plugin));
        registry.register(new CatTowerStructure(plugin));
        registry.register(new HamsterWheelCage(plugin));
        registry.register(new BabyMobile(plugin));
        registry.register(new GiantCradle(plugin));
        registry.register(new ToyChestLaunch(plugin));
        registry.register(new PinwheelGarden(plugin));
        registry.register(new JackInBox(plugin));
        registry.register(new DollhouseStructure(plugin));
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
    // #21 — CAROUSEL SPIN
    // 76 blocks: 12 cherry plank base ring (R=4.5), 4 birch poles
    // (5 stacked each = 20), 4 horses (9 quartz blocks each = 36), 8 conical
    // top ring blocks. Whole carousel spins Y-axis 0.02 rad/tick. Horses
    // bob up/down on poles (sin wave per horse, 90 deg phase offset).
    // Constant damage radius 4.5, 10 hearts, 12-tick interval, 15-tick delay.
    // ================================================================
    public static class CarouselSpin extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> baseRing = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> poles = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> horses = new ArrayList<>();
        private final List<BlockDisplayHandle> topRing = new ArrayList<>();
        private float yRotation = 0f;
        private float horseBobPhase = 0f;

        public CarouselSpin(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("carousel_spin", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(10.0);
            config.setDamageRadius(4.5);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(15);
            config.setDurationTicks(360);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Base ring: 12 cherry plank blocks at R=4.5
            double baseRadius = 4.5;
            for (int i = 0; i < 12; i++) {
                double angle = (2.0 * Math.PI * i) / 12;
                double px = Math.cos(angle) * baseRadius;
                double pz = Math.sin(angle) * baseRadius;
                Location loc = center.clone().add(px, 0, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHERRY_PLANKS);
                h.scale(1.2f, 0.25f, 1.2f).glow(255, 180, 200).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                baseRing.add(h);
            }

            // 4 poles at cardinal directions (R=3.0), each 5 birch blocks tall
            double poleRadius = 3.0;
            for (int p = 0; p < 4; p++) {
                List<BlockDisplayHandle> pole = new ArrayList<>();
                double angle = (2.0 * Math.PI * p) / 4;
                double px = Math.cos(angle) * poleRadius;
                double pz = Math.sin(angle) * poleRadius;
                for (int y = 0; y < 5; y++) {
                    Location loc = center.clone().add(px, 0.5 + y * 1.0, pz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BIRCH_PLANKS);
                    h.scale(0.3f, 1.0f, 0.3f).glow(250, 240, 220).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    pole.add(h);
                }
                poles.add(pole);
            }

            // 4 horses (9 quartz blocks each: 3 body + 2 head + 4 legs)
            for (int h = 0; h < 4; h++) {
                List<BlockDisplayHandle> horse = new ArrayList<>();
                double angle = (2.0 * Math.PI * h) / 4;
                double px = Math.cos(angle) * poleRadius;
                double pz = Math.sin(angle) * poleRadius;

                // Body (3 blocks at Y=2.0..2.6)
                for (int b = 0; b < 3; b++) {
                    Location loc = center.clone().add(px + (b - 1) * 0.4, 2.0, pz);
                    BlockDisplayHandle bd = displayBuilder.spawnBlock(loc, Material.QUARTZ_BLOCK);
                    bd.scale(0.4f, 0.4f, 0.6f).glow(250, 240, 220).interpolation(3, 0);
                    spawnedEntities.add(bd.entity());
                    horse.add(bd);
                }
                // Head (2 blocks at Y=2.5..3.0, forward of body)
                for (int hd = 0; hd < 2; hd++) {
                    Location loc = center.clone().add(px + 0.8, 2.5 + hd * 0.4, pz);
                    BlockDisplayHandle hb = displayBuilder.spawnBlock(loc, Material.QUARTZ_BLOCK);
                    hb.scale(0.35f, 0.35f, 0.35f).glow(250, 240, 220).interpolation(3, 0);
                    spawnedEntities.add(hb.entity());
                    horse.add(hb);
                }
                // Legs (4 blocks at Y=1.4)
                double[][] legOffsets = {{-0.4, 0.2}, {-0.4, -0.2}, {0.4, 0.2}, {0.4, -0.2}};
                for (double[] leg : legOffsets) {
                    Location loc = center.clone().add(px + leg[0], 1.4, pz + leg[1]);
                    BlockDisplayHandle lg = displayBuilder.spawnBlock(loc, Material.QUARTZ_BLOCK);
                    lg.scale(0.18f, 0.6f, 0.18f).glow(250, 240, 220).interpolation(3, 0);
                    spawnedEntities.add(lg.entity());
                    horse.add(lg);
                }
                horses.add(horse);
            }

            // Top ring: 8 conical blocks at R=3.5, Y=6.0
            double topRadius = 3.5;
            for (int i = 0; i < 8; i++) {
                double angle = (2.0 * Math.PI * i) / 8;
                double px = Math.cos(angle) * topRadius;
                double pz = Math.sin(angle) * topRadius;
                Location loc = center.clone().add(px, 6.0, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PINK_CONCRETE);
                float tilt = (float) Math.toRadians(30);
                float axisX = (float) -Math.sin(angle);
                float axisZ = (float) Math.cos(angle);
                h.scale(0.7f, 0.45f, 0.7f);
                h.rotate(tilt, axisX, 0, axisZ);
                h.glow(255, 180, 200).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                topRing.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 0.8f);
            DisplayBuilder.playSound(center, Sound.ENTITY_HORSE_GALLOP, 0.8f, 1.2f);
            w.spawnParticle(Particle.ENCHANT, center.clone().add(0, 4, 0), 30, 3, 2, 3, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Whole carousel rotates Y at 0.02 rad/tick
            yRotation += 0.02f;
            horseBobPhase += 0.12f;

            if (tick % 3 == 0) {
                // Apply Y-rotation to all parts
                List<BlockDisplayHandle> all = new ArrayList<>();
                all.addAll(baseRing);
                for (List<BlockDisplayHandle> pole : poles) all.addAll(pole);
                all.addAll(topRing);
                for (BlockDisplayHandle h : all) {
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

                // Horses bob up/down with 90 deg phase offset each
                for (int hi = 0; hi < horses.size(); hi++) {
                    float phaseOffset = (float) (Math.PI / 2.0 * hi);
                    float bobY = (float) (Math.sin(horseBobPhase + phaseOffset) * 0.4);
                    for (BlockDisplayHandle h : horses.get(hi)) {
                        BlockDisplay entity = h.entity();
                        Transformation t = entity.getTransformation();
                        entity.setInterpolationDuration(3);
                        entity.setInterpolationDelay(0);
                        Vector3f curScale = t.getScale();
                        entity.setTransformation(new Transformation(
                                new Vector3f(-0.5f, bobY - 0.5f, -0.5f),
                                new AxisAngle4f(yRotation, 0f, 1f, 0f),
                                curScale,
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                }
            }

            // ENCHANT sparkles from poles, CRIT from horse hooves
            if (tick % 4 == 0) {
                for (int p = 0; p < 4; p++) {
                    double angle = (2.0 * Math.PI * p) / 4 + yRotation;
                    double px = Math.cos(angle) * 3.0;
                    double pz = Math.sin(angle) * 3.0;
                    c.getWorld().spawnParticle(Particle.ENCHANT,
                            c.clone().add(px, 3.0, pz), 4, 0.2, 0.5, 0.2, 0.02);
                    c.getWorld().spawnParticle(Particle.CRIT,
                            c.clone().add(px, 1.4, pz), 3, 0.2, 0.1, 0.2, 0.02);
                }
            }

            // Sound loop
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.1f);
            }
            if (tick % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_HORSE_GALLOP, 0.5f, 1.3f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new CarouselSpin(plugin); }
    }

    // ================================================================
    // #22 — GIANT MOUSETRAP
    // 25 blocks: 12 cherry plank flat board (4x3 grid), 5 iron block U-spring
    // (raised vertical at start), 2 bait platform, 3 trigger bar, 2 latch,
    // + 1 extra pivot block. Spring snaps down on trigger.
    // Impact-only on snap, radius 4.0, 28 hearts.
    // ================================================================
    public static class GiantMousetrap extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> board = new ArrayList<>();
        private final List<BlockDisplayHandle> spring = new ArrayList<>();
        private final List<BlockDisplayHandle> bait = new ArrayList<>();
        private final List<BlockDisplayHandle> trigger = new ArrayList<>();
        private final List<BlockDisplayHandle> latch = new ArrayList<>();
        private boolean snapped = false;

        public GiantMousetrap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("giant_mousetrap", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(28.0);
            config.setImpactRadius(4.0);
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

            // Board: 4x3 grid of cherry planks (12 blocks)
            for (int x = -2; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Location loc = center.clone().add(x * 1.0, 0.05, z * 1.0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHERRY_PLANKS);
                    h.scale(1.0f, 0.25f, 1.0f).glow(255, 180, 200).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    board.add(h);
                }
            }

            // U-shaped spring bar: 5 iron blocks raised vertical (cocked)
            // Two upright posts + horizontal top bar
            double[][] springOffsets = {
                    {-1.5, 0, 0},   // left post base — will be tall
                    {1.5, 0, 0},    // right post base
                    {-1.5, 0, 0},   // left post tip
                    {1.5, 0, 0},    // right post tip
                    {0.0, 0, 0}     // top crossbar
            };
            // Posts vertical (raised)
            for (int s = 0; s < 2; s++) {
                Location loc = center.clone().add(springOffsets[s][0], 1.0, springOffsets[s][2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0.2f, 2.0f, 0.2f).glow(220, 220, 230).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                spring.add(h);
            }
            // Two upper joints
            for (int s = 2; s < 4; s++) {
                Location loc = center.clone().add(springOffsets[s][0], 2.4, springOffsets[s][2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0.2f, 0.4f, 0.2f).glow(220, 220, 230).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                spring.add(h);
            }
            // Top crossbar
            {
                Location loc = center.clone().add(0, 2.6, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(3.2f, 0.2f, 0.2f).glow(220, 220, 230).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                spring.add(h);
            }

            // Bait platform: 2 yellow concrete blocks at center
            for (int b = 0; b < 2; b++) {
                Location loc = center.clone().add(b == 0 ? -0.3 : 0.3, 0.3, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.YELLOW_CONCRETE);
                h.scale(0.4f, 0.3f, 0.4f).glow(255, 220, 130).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                bait.add(h);
            }

            // Trigger bar: 3 thin blocks
            for (int tg = 0; tg < 3; tg++) {
                Location loc = center.clone().add(-0.5 + tg * 0.5, 0.4, 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0.4f, 0.08f, 0.08f).glow(220, 220, 230).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                trigger.add(h);
            }

            // Latch: 2 small blocks at edge
            for (int lt = 0; lt < 2; lt++) {
                Location loc = center.clone().add(2.0, 0.3 + lt * 0.3, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0.2f, 0.2f, 0.2f).glow(220, 220, 230).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                latch.add(h);
            }

            // Extra pivot block (to bump count to 25)
            {
                Location loc = center.clone().add(0, 0.6, -0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0.3f, 0.15f, 0.15f).glow(220, 220, 230).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                latch.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_TRIPWIRE_ATTACH, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Telegraph phase: vibration on spring
            if (!snapped && tick < 40) {
                if (tick % 4 == 0) {
                    float pulse = 1.0f + 0.05f * (float) Math.sin(tick * 0.5);
                    for (BlockDisplayHandle h : spring) {
                        BlockDisplay entity = h.entity();
                        Transformation t = entity.getTransformation();
                        entity.setInterpolationDuration(2);
                        entity.setInterpolationDelay(0);
                        Vector3f s = t.getScale();
                        entity.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                new Vector3f(s.x * pulse, s.y, s.z * pulse),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                }
                if (tick % 6 == 0) {
                    c.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(0, 0.5, 0), 3, 0.4, 0.1, 0.4, 0.01);
                }
            }

            // Snap on tick 40
            if (!snapped && tick >= 40) {
                snapped = true;
                // Spring rotates 180 deg (X-axis) instantly (1-tick interpolation)
                float snapAngle = (float) Math.PI;
                for (BlockDisplayHandle h : spring) {
                    BlockDisplay entity = h.entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(1);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(snapAngle, 1f, 0f, 0f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }

                triggerImpactDamage(c.clone().add(0, 0.5, 0));
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.4f, 0.8f);
                DisplayBuilder.playSound(c, Sound.BLOCK_TRIPWIRE_CLICK_ON, 1.2f, 0.7f);
                c.getWorld().spawnParticle(Particle.CRIT, c.clone().add(0, 0.8, 0), 40, 2, 0.5, 2, 0.2);
            }

            // After-snap: spring slowly rises back
            if (snapped && tick > 80 && tick < 140) {
                if (tick % 10 == 0) {
                    float liftProgress = (tick - 80) / 60.0f;
                    float angle = (float) Math.PI * (1.0f - liftProgress);
                    for (BlockDisplayHandle h : spring) {
                        BlockDisplay entity = h.entity();
                        Transformation t = entity.getTransformation();
                        entity.setInterpolationDuration(10);
                        entity.setInterpolationDelay(0);
                        entity.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f(angle, 1f, 0f, 0f),
                                t.getScale(),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new GiantMousetrap(plugin); }
    }

    // ================================================================
    // #23 — CAT TOWER STRUCTURE
    // 49 blocks: 3 tiers (each = 8 dark oak ring + 5 stripped oak post = 13)
    // = 39, plus 10-block top perch. Tiers rise staggered. Whole tower sways
    // Z-axis ±5 deg. Topples in player direction at end.
    // Constant radius 2.0, 9 hearts, 15-tick interval. Topple impact 4.0, 20 hearts.
    // ================================================================
    public static class CatTowerStructure extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private float swayPhase = 0f;
        private boolean toppled = false;
        private float toppleAngle = 0f;
        private double toppleAxisX = 1, toppleAxisZ = 0;
        // Cached spawn-time direction toward player.
        private double cachedDirX = 0, cachedDirZ = -1;

        public CatTowerStructure(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cat_tower_structure", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(9.0);
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(320);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 3 tiers stacked
            double[] tierY = {0.5, 2.5, 4.5};
            double[] tierR = {2.0, 1.6, 1.2};
            for (int t = 0; t < 3; t++) {
                // Round platform: 8 dark oak blocks in ring
                for (int i = 0; i < 8; i++) {
                    double angle = (2.0 * Math.PI * i) / 8;
                    double px = Math.cos(angle) * tierR[t];
                    double pz = Math.sin(angle) * tierR[t];
                    Location loc = center.clone().add(px, tierY[t], pz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_OAK_PLANKS);
                    h.scale(0.7f, 0.2f, 0.7f).glow(120, 80, 50).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    allBlocks.add(h);
                }
                // Connecting post: 5 stripped oak (cylindrical) blocks
                for (int p = 0; p < 5; p++) {
                    Location loc = center.clone().add(0, tierY[t] + 0.2 + p * 0.35, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.STRIPPED_OAK_LOG);
                    h.scale(0.35f, 0.4f, 0.35f).glow(200, 160, 120).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    allBlocks.add(h);
                }
            }

            // Top perch: 10 blocks (cluster on top)
            double[][] perchOffsets = {
                    {0, 6.0, 0}, {0.4, 6.0, 0}, {-0.4, 6.0, 0}, {0, 6.0, 0.4}, {0, 6.0, -0.4},
                    {0.3, 6.3, 0.3}, {-0.3, 6.3, 0.3}, {0.3, 6.3, -0.3}, {-0.3, 6.3, -0.3}, {0, 6.5, 0}
            };
            for (double[] off : perchOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_WOOL);
                h.scale(0.45f, 0.4f, 0.45f).glow(250, 240, 220).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_CAT_PURR, 1.0f, 1.0f);
            w.spawnParticle(Particle.FALLING_DUST, center.clone().add(0, 4, 0), 20,
                    Material.WHITE_WOOL.createBlockData());

            // Cache spawn-time topple direction toward nearest player.
            Player initialTarget = findNearestPlayer(center, 12.0);
            if (initialTarget != null) {
                double dxs = initialTarget.getLocation().getX() - center.getX();
                double dzs = initialTarget.getLocation().getZ() - center.getZ();
                double mag = Math.sqrt(dxs * dxs + dzs * dzs);
                if (mag > 0.001) {
                    cachedDirX = dxs / mag;
                    cachedDirZ = dzs / mag;
                }
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Topple at tick 240 — uses cached spawn-time player direction.
            if (!toppled && tick >= 240) {
                toppled = true;
                // Topple axis perpendicular to cached player direction.
                toppleAxisX = -cachedDirZ;
                toppleAxisZ = cachedDirX;
                triggerImpactDamage(c.clone().add(toppleAxisZ * 3.0, 0.5, -toppleAxisX * 3.0));
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_BIG_FALL, 1.5f, 0.6f);
                c.getWorld().spawnParticle(Particle.CRIT, c.clone().add(0, 1, 0), 60, 3, 2, 3, 0.2);
            }

            // Topple animation: rotate 0 -> 90 deg over 10 ticks
            if (toppled) {
                int toppleTick = tick - 240;
                if (toppleTick <= 10) {
                    toppleAngle = (float) (Math.PI / 2.0 * (toppleTick / 10.0));
                    for (BlockDisplayHandle h : allBlocks) {
                        BlockDisplay entity = h.entity();
                        Transformation t = entity.getTransformation();
                        entity.setInterpolationDuration(2);
                        entity.setInterpolationDelay(0);
                        entity.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f(toppleAngle, (float) toppleAxisX, 0f, (float) toppleAxisZ),
                                t.getScale(),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                }
                return;
            }

            // Sway: Z-axis rotation ±5 deg, 40-tick period
            swayPhase += (2.0 * Math.PI / 40.0);
            if (tick % 4 == 0) {
                float swayAngle = (float) (Math.toRadians(5) * Math.sin(swayPhase));
                for (BlockDisplayHandle h : allBlocks) {
                    BlockDisplay entity = h.entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(20);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(swayAngle, 0f, 0f, 1f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Particles
            if (tick % 8 == 0) {
                c.getWorld().spawnParticle(Particle.FALLING_DUST,
                        c.clone().add(0, 3.0 + Math.random() * 2, 0), 3,
                        0.2, 0.2, 0.2, 0,
                        Material.WHITE_WOOL.createBlockData());
                c.getWorld().spawnParticle(Particle.ENCHANT, c.clone().add(0, 6.5, 0), 4, 0.5, 0.3, 0.5, 0.02);
            }

            if (tick % 60 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_CAT_PURR, 0.7f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new CatTowerStructure(plugin); }
    }

    // ================================================================
    // #24 — HAMSTER WHEEL CAGE
    // 51 blocks: vertical wheel (12 birch outer ring + 12 spokes + 3 hub = 27),
    // cage (16 corner posts + 8 top ring = 24). Wheel spins Z-axis 0.08 rad/tick.
    // Constant radius 2.0, 10 hearts, 10-tick interval, 20-tick delay.
    // ================================================================
    public static class HamsterWheelCage extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> wheel = new ArrayList<>();
        private final List<BlockDisplayHandle> cage = new ArrayList<>();
        private float wheelRotation = 0f;
        private float wheelSpeed = 0.02f;

        public HamsterWheelCage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hamster_wheel_cage", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(10);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(320);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Cage: 4 corner posts (4 stacked = 16)
            double[][] corners = {{-1.8, 0, -1.8}, {1.8, 0, -1.8}, {1.8, 0, 1.8}, {-1.8, 0, 1.8}};
            for (double[] cor : corners) {
                for (int y = 0; y < 4; y++) {
                    Location loc = center.clone().add(cor[0], 0.5 + y * 1.0, cor[2]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OAK_PLANKS);
                    h.scale(0.18f, 1.0f, 0.18f).glow(180, 140, 90).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    cage.add(h);
                }
            }
            // Cage top ring (8 blocks at Y=4.5)
            for (int i = 0; i < 8; i++) {
                double angle = (2.0 * Math.PI * i) / 8;
                double px = Math.cos(angle) * 2.4;
                double pz = Math.sin(angle) * 2.4;
                Location loc = center.clone().add(px, 4.5, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OAK_PLANKS);
                h.scale(0.5f, 0.15f, 0.5f).glow(180, 140, 90).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                cage.add(h);
            }

            // Wheel — vertical (Z-axis spin), in XY plane
            double wheelR = 1.4;
            // Outer ring: 12 birch blocks
            for (int i = 0; i < 12; i++) {
                double angle = (2.0 * Math.PI * i) / 12;
                double px = Math.cos(angle) * wheelR;
                double py = Math.sin(angle) * wheelR + 2.0;
                Location loc = center.clone().add(px, py, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BIRCH_PLANKS);
                h.scale(0.3f, 0.3f, 0.18f).glow(250, 240, 200).interpolation(2, 0);
                spawnedEntities.add(h.entity());
                wheel.add(h);
            }
            // 6 pairs of spoke blocks (12 total)
            for (int i = 0; i < 6; i++) {
                double angle = (2.0 * Math.PI * i) / 6;
                for (int s = 0; s < 2; s++) {
                    double r = (s + 1) * 0.45;
                    double px = Math.cos(angle) * r;
                    double py = Math.sin(angle) * r + 2.0;
                    Location loc = center.clone().add(px, py, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BIRCH_PLANKS);
                    h.scale(0.15f, 0.15f, 0.12f).glow(250, 240, 200).interpolation(2, 0);
                    spawnedEntities.add(h.entity());
                    wheel.add(h);
                }
            }
            // Center hub: 3 blocks
            for (int hb = 0; hb < 3; hb++) {
                Location loc = center.clone().add(0, 2.0, (hb - 1) * 0.15);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.STRIPPED_OAK_LOG);
                h.scale(0.3f, 0.3f, 0.12f).glow(220, 180, 120).interpolation(2, 0);
                spawnedEntities.add(h.entity());
                wheel.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GRAVEL_STEP, 1.0f, 1.4f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Speed ramps up from 0.02 -> 0.08 over 30 ticks
            if (tick < 30) {
                wheelSpeed = 0.02f + (0.06f * tick / 30.0f);
            } else {
                wheelSpeed = 0.08f;
            }
            wheelRotation += wheelSpeed;

            // Wheel rotation Z-axis (vertical plane spin)
            if (tick % 2 == 0) {
                for (BlockDisplayHandle h : wheel) {
                    BlockDisplay entity = h.entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(2);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(wheelRotation, 0f, 0f, 1f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Particles
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.CRIT, c.clone().add(0, 2, 0), 4, 1.4, 1.4, 0.1, 0.05);
            }
            if (tick % 8 == 0) {
                c.getWorld().spawnParticle(Particle.BLOCK,
                        c.clone().add(0, 0.1, 0), 8, 1.5, 0, 1.5, 0,
                        Material.SAND.createBlockData());
            }

            // Sound: rapid gravel step for wheel spin
            if (tick % 5 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GRAVEL_STEP, 0.6f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new HamsterWheelCage(plugin); }
    }

    // ================================================================
    // #25 — BABY MOBILE
    // 45 blocks: 8 quartz top ring, 5 arms (3 each = 15), hanging toys
    // (star 5 + bunny 4 + duck 4 + ball 3 + bear head 6 = 22).
    // Hovers at Y+8. Mobile rotates Y 0.01 rad/tick. Each toy rotates
    // independently. Constant radius 3.0, 9 hearts, 15-tick interval, 20-tick delay.
    // ================================================================
    public static class BabyMobile extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> ring = new ArrayList<>();
        private final List<BlockDisplayHandle> arms = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> toys = new ArrayList<>();
        private final float[] toySpeeds = {0.02f, 0.03f, 0.015f, 0.025f, 0.02f};
        private final float[] toyRotations = new float[5];
        private float mobileRotation = 0f;
        private float bobPhase = 0f;

        public BabyMobile(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("baby_mobile", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(9.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(15);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(340);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double mobileY = 8.0;

            // Top ring: 8 quartz blocks at radius 2.0
            double ringR = 2.0;
            for (int i = 0; i < 8; i++) {
                double angle = (2.0 * Math.PI * i) / 8;
                double px = Math.cos(angle) * ringR;
                double pz = Math.sin(angle) * ringR;
                Location loc = center.clone().add(px, mobileY, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.QUARTZ_BLOCK);
                h.scale(0.4f, 0.2f, 0.4f).glow(250, 240, 220).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                ring.add(h);
            }

            // 5 arms extending downward from ring
            double[][] toyAnchors = new double[5][2];
            for (int a = 0; a < 5; a++) {
                double angle = (2.0 * Math.PI * a) / 5;
                double px = Math.cos(angle) * ringR;
                double pz = Math.sin(angle) * ringR;
                toyAnchors[a][0] = px;
                toyAnchors[a][1] = pz;
                for (int s = 0; s < 3; s++) {
                    Location loc = center.clone().add(px, mobileY - 0.4 - s * 0.5, pz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_CONCRETE);
                    h.scale(0.08f, 0.5f, 0.08f).glow(250, 240, 220).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    arms.add(h);
                }
            }

            // Toy 0: star (5 gold blocks at 5 points)
            List<BlockDisplayHandle> star = new ArrayList<>();
            double sx = toyAnchors[0][0], sz = toyAnchors[0][1];
            double sy = mobileY - 2.5;
            double[][] starPts = {{0, 0.4}, {0.4, 0.1}, {0.25, -0.3}, {-0.25, -0.3}, {-0.4, 0.1}};
            for (double[] sp : starPts) {
                Location loc = center.clone().add(sx + sp[0], sy, sz + sp[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.GOLD_BLOCK);
                h.scale(0.25f, 0.25f, 0.25f).glow(255, 220, 130).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                star.add(h);
            }
            toys.add(star);

            // Toy 1: bunny (4 white blocks)
            List<BlockDisplayHandle> bunny = new ArrayList<>();
            double bx = toyAnchors[1][0], bz = toyAnchors[1][1];
            double by = mobileY - 2.5;
            double[][] bunnyPts = {{0, 0, 0}, {0.2, 0.3, 0}, {-0.2, 0.3, 0}, {0, -0.2, 0}};
            for (double[] bp : bunnyPts) {
                Location loc = center.clone().add(bx + bp[0], by + bp[1], bz + bp[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_CONCRETE);
                h.scale(0.25f, 0.25f, 0.25f).glow(250, 240, 220).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                bunny.add(h);
            }
            toys.add(bunny);

            // Toy 2: duck (4 yellow blocks)
            List<BlockDisplayHandle> duck = new ArrayList<>();
            double dx = toyAnchors[2][0], dz = toyAnchors[2][1];
            double dy = mobileY - 2.5;
            double[][] duckPts = {{0, 0, 0}, {0.25, 0.15, 0}, {0.4, 0.1, 0}, {-0.15, -0.1, 0}};
            for (double[] dp : duckPts) {
                Location loc = center.clone().add(dx + dp[0], dy + dp[1], dz + dp[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.YELLOW_CONCRETE);
                h.scale(0.25f, 0.25f, 0.25f).glow(255, 220, 130).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                duck.add(h);
            }
            toys.add(duck);

            // Toy 3: ball (3 pink blocks sphere)
            List<BlockDisplayHandle> ball = new ArrayList<>();
            double ballx = toyAnchors[3][0], ballz = toyAnchors[3][1];
            double bally = mobileY - 2.5;
            double[][] ballPts = {{0, 0, 0}, {0.2, 0.2, 0}, {-0.2, -0.1, 0.1}};
            for (double[] bp : ballPts) {
                Location loc = center.clone().add(ballx + bp[0], bally + bp[1], ballz + bp[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PINK_CONCRETE);
                h.scale(0.3f, 0.3f, 0.3f).glow(255, 180, 200).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                ball.add(h);
            }
            toys.add(ball);

            // Toy 4: bear head (6 orange blocks)
            List<BlockDisplayHandle> bear = new ArrayList<>();
            double brx = toyAnchors[4][0], brz = toyAnchors[4][1];
            double bry = mobileY - 2.5;
            double[][] bearPts = {{0, 0, 0}, {0.2, 0.2, 0}, {-0.2, 0.2, 0}, {0.15, -0.15, 0}, {-0.15, -0.15, 0}, {0, 0.35, 0}};
            for (double[] bp : bearPts) {
                Location loc = center.clone().add(brx + bp[0], bry + bp[1], brz + bp[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ORANGE_CONCRETE);
                h.scale(0.22f, 0.22f, 0.22f).glow(255, 160, 90).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                bear.add(h);
            }
            toys.add(bear);

            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.4f);
            w.spawnParticle(Particle.ENCHANT, center.clone().add(0, mobileY, 0), 30, 2, 1, 2, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            mobileRotation += 0.01f;
            bobPhase += 0.1f;

            // Mobile (ring + arms) rotate Y
            if (tick % 3 == 0) {
                List<BlockDisplayHandle> all = new ArrayList<>();
                all.addAll(ring);
                all.addAll(arms);
                for (BlockDisplayHandle h : all) {
                    BlockDisplay entity = h.entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(3);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(mobileRotation, 0f, 1f, 0f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }

                // Each toy rotates independently with its own bob phase
                for (int i = 0; i < toys.size(); i++) {
                    toyRotations[i] += toySpeeds[i];
                    float phaseOffset = (float) (Math.PI * 2.0 / 5.0 * i);
                    float bob = (float) (Math.sin(bobPhase + phaseOffset) * 0.15);
                    for (BlockDisplayHandle h : toys.get(i)) {
                        BlockDisplay entity = h.entity();
                        Transformation t = entity.getTransformation();
                        entity.setInterpolationDuration(3);
                        entity.setInterpolationDelay(0);
                        entity.setTransformation(new Transformation(
                                new Vector3f(-0.5f, bob - 0.5f, -0.5f),
                                new AxisAngle4f(toyRotations[i], 0f, 1f, 0f),
                                t.getScale(),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                }
            }

            // Particles
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.ENCHANT, c.clone().add(0, 6.5, 0), 5, 2, 1, 2, 0.05);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 5.5, 0), 4, 2, 1.5, 2, 0.01);
            }

            // Sound
            if (tick % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_BELL, 0.6f, 1.4f);
            }
            if (tick % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new BabyMobile(plugin); }
    }

    // ================================================================
    // #26 — GIANT CRADLE
    // 42 blocks: 2 curved rocker pieces (6 cherry plank each = 12), hood arch (8),
    // 2 side rails (8 each white wool = 16), bottom floor (6).
    // Rocks Z-axis ±15 deg, 30-tick period. Constant radius 2.5, 8 hearts,
    // 15-tick interval, 10-tick delay.
    // ================================================================
    public static class GiantCradle extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private float rockPhase = 0f;

        public GiantCradle(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("giant_cradle", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(8.0);
            config.setDamageRadius(2.5);
            config.setTicksBetweenDamage(15);
            config.setDamageDelayTicks(10);
            config.setDurationTicks(300);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 2 curved rocker pieces (6 cherry plank blocks each) at Y near ground
            for (int side = 0; side < 2; side++) {
                double zOff = (side == 0) ? -1.5 : 1.5;
                for (int i = 0; i < 6; i++) {
                    double angle = (Math.PI * (i / 5.0));
                    double xPos = -1.8 + i * 0.72;
                    double yArc = -0.2 + 0.1 * Math.sin(angle);
                    Location loc = center.clone().add(xPos, yArc, zOff);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHERRY_PLANKS);
                    h.scale(0.7f, 0.2f, 0.4f).glow(255, 180, 200).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    allBlocks.add(h);
                }
            }

            // Hood arch (8 blocks) — semicircle over one end
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * (i / 7.0);
                double px = 1.5 - Math.cos(angle) * 0.9;
                double py = 1.5 + Math.sin(angle) * 1.0;
                Location loc = center.clone().add(px, py, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PINK_CONCRETE);
                h.scale(0.3f, 0.3f, 1.4f).glow(255, 180, 200).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // Side rails (8 white wool each = 16)
            for (int side = 0; side < 2; side++) {
                double zOff = (side == 0) ? -1.5 : 1.5;
                for (int i = 0; i < 8; i++) {
                    double xPos = -1.6 + i * 0.45;
                    Location loc = center.clone().add(xPos, 0.6, zOff);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_WOOL);
                    h.scale(0.4f, 0.6f, 0.18f).glow(250, 240, 220).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    allBlocks.add(h);
                }
            }

            // Bottom floor: 6 cherry plank blocks
            for (int i = 0; i < 6; i++) {
                double xPos = -1.5 + i * 0.6;
                Location loc = center.clone().add(xPos, 0.2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHERRY_PLANKS);
                h.scale(0.6f, 0.15f, 2.8f).glow(255, 180, 200).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.4f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 1, 0), 20, 1.5, 0.5, 1.5, 0.01);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rock back and forth: ±15 deg, 30-tick period
            rockPhase += (2.0 * Math.PI / 30.0);
            if (tick % 5 == 0) {
                float rockAngle = (float) (Math.toRadians(15) * Math.sin(rockPhase));
                for (BlockDisplayHandle h : allBlocks) {
                    BlockDisplay entity = h.entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(15);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(rockAngle, 0f, 0f, 1f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Particles
            if (tick % 8 == 0) {
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 0.8, 0), 4, 1.2, 0.3, 1.2, 0.01);
                c.getWorld().spawnParticle(Particle.ENCHANT, c.clone().add(1.5, 2.5, 0), 3, 0.4, 0.4, 0.4, 0.02);
            }

            // Sound: lullaby chimes
            if (tick % 25 == 0) {
                float pitch = 1.0f + (float) (Math.sin(tick * 0.15) * 0.3);
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, pitch);
            }

            // On dissipate: cradle splits
            if (tick == config.getDurationTicks() - 10) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WITCH_CELEBRATE, 1.0f, 0.4f);
                c.getWorld().spawnParticle(Particle.HEART, c.clone().add(0, 1, 0), 20, 1.5, 0.8, 1.5, 0.05);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new GiantCradle(plugin); }
    }

    // ================================================================
    // #27 — TOY CHEST LAUNCH
    // 36 blocks: chest box (4 sides x 6 = 24 oak), bottom (6), lid (6),
    // 6 toy projectile groups (3 blocks each = 18 extras). Total 54 blocks.
    // Lid creaks open, then 6 toy groups fly outward in 6 directions.
    // Impact-only on each toy landing, radius 1.5, 15 hearts each.
    // ================================================================
    public static class ToyChestLaunch extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> chest = new ArrayList<>();
        private final List<BlockDisplayHandle> lid = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> toyGroups = new ArrayList<>();
        private boolean lidOpened = false;
        private boolean toysLaunched = false;

        public ToyChestLaunch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("toy_chest_launch", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(15.0);
            config.setImpactRadius(1.5);
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

            // 4 sides: 6 oak planks each (vertical strips)
            int[][] sides = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] s : sides) {
                for (int i = 0; i < 6; i++) {
                    double angle = i / 5.0;
                    double off = -0.8 + angle * 1.6;
                    double px = s[0] != 0 ? s[0] * 1.0 : off;
                    double pz = s[1] != 0 ? s[1] * 1.0 : off;
                    Location loc = center.clone().add(px, 0.3 + (s[0] != 0 || s[1] != 0 ? (i % 2) * 0.5 : 0), pz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OAK_PLANKS);
                    h.scale(s[0] != 0 ? 0.18f : 0.3f, 0.5f, s[1] != 0 ? 0.18f : 0.3f);
                    h.glow(180, 140, 90).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    chest.add(h);
                }
            }

            // Bottom (6 blocks)
            for (int i = 0; i < 6; i++) {
                double xOff = -0.7 + (i % 3) * 0.7;
                double zOff = -0.4 + (i / 3) * 0.8;
                Location loc = center.clone().add(xOff, 0.05, zOff);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OAK_PLANKS);
                h.scale(0.7f, 0.1f, 0.7f).glow(180, 140, 90).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                chest.add(h);
            }

            // Lid (6 blocks, separate group for animation)
            for (int i = 0; i < 6; i++) {
                double xOff = -0.7 + (i % 3) * 0.7;
                double zOff = -0.4 + (i / 3) * 0.8;
                Location loc = center.clone().add(xOff, 1.4, zOff);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OAK_PLANKS);
                h.scale(0.7f, 0.15f, 0.7f).glow(180, 140, 90).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                lid.add(h);
            }

            // 6 toy projectile groups (3 blocks each, different colors)
            Material[] toyMats = {Material.RED_CONCRETE, Material.YELLOW_CONCRETE, Material.BLUE_CONCRETE,
                    Material.GREEN_CONCRETE, Material.PURPLE_CONCRETE, Material.PINK_CONCRETE};
            for (int t = 0; t < 6; t++) {
                List<BlockDisplayHandle> toy = new ArrayList<>();
                for (int b = 0; b < 3; b++) {
                    Location loc = center.clone().add(0, 1.0 + b * 0.2, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, toyMats[t]);
                    h.scale(0.25f, 0.25f, 0.25f).glow(255, 180, 200).interpolation(4, 0);
                    spawnedEntities.add(h.entity());
                    toy.add(h);
                }
                toyGroups.add(toy);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_SHULKER_OPEN, 0.5f, 0.6f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Lid creak open over 20 ticks (X-axis 0 -> 90 deg)
            if (!lidOpened && tick <= 20) {
                float liftAngle = (float) (Math.PI / 2.0 * (tick / 20.0));
                for (BlockDisplayHandle h : lid) {
                    BlockDisplay entity = h.entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(2);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(liftAngle, 1f, 0f, 0f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
                if (tick == 20) {
                    lidOpened = true;
                    DisplayBuilder.playSound(c, Sound.ENTITY_SHULKER_OPEN, 1.0f, 1.0f);
                    c.getWorld().spawnParticle(Particle.ENCHANT, c.clone().add(0, 1.5, 0), 30, 0.5, 0.5, 0.5, 0.1);
                }
            }

            // Launch toys at tick 30
            if (lidOpened && !toysLaunched && tick >= 30) {
                toysLaunched = true;
                DisplayBuilder.playSound(c, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.2f, 1.5f);
                // 6 directions evenly spaced
                for (int i = 0; i < 6; i++) {
                    double angle = (2.0 * Math.PI * i) / 6;
                    float tx = (float) (Math.cos(angle) * 3.0) - 0.5f;
                    float tz = (float) (Math.sin(angle) * 3.0) - 0.5f;
                    for (BlockDisplayHandle h : toyGroups.get(i)) {
                        BlockDisplay entity = h.entity();
                        Transformation t = entity.getTransformation();
                        entity.setInterpolationDuration(4);
                        entity.setInterpolationDelay(0);
                        entity.setTransformation(new Transformation(
                                new Vector3f(tx, 0.5f, tz),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                t.getScale(),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                }
            }

            // Toy landings: tick 36 (after 6 ticks of flight)
            if (toysLaunched && tick == 36) {
                for (int i = 0; i < 6; i++) {
                    double angle = (2.0 * Math.PI * i) / 6;
                    Location landLoc = c.clone().add(Math.cos(angle) * 3.0, 0.5, Math.sin(angle) * 3.0);
                    triggerImpactDamage(landLoc);
                    c.getWorld().spawnParticle(Particle.CRIT, landLoc, 15, 0.4, 0.4, 0.4, 0.1);
                }
            }

            // Particles
            if (tick % 8 == 0 && lidOpened) {
                c.getWorld().spawnParticle(Particle.ENCHANT, c.clone().add(0, 1.6, 0), 4, 0.3, 0.2, 0.3, 0.05);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ToyChestLaunch(plugin); }
    }

    // ================================================================
    // #28 — PINWHEEL GARDEN
    // 55 blocks: 5 pinwheels — each = 4 blades (2 blocks each = 8) + 3 post = 11.
    // 5 colors (PINK, YELLOW, BLUE, WHITE, RED concrete). Different speeds.
    // Constant radius 1.2 each pinwheel = 5 zones, 9 hearts, 8-tick interval, 5-tick delay.
    // ================================================================
    public static class PinwheelGarden extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> pinwheels = new ArrayList<>();
        private final float[] speeds = {0.06f, 0.04f, 0.08f, 0.05f, 0.07f};
        private final float[] rotations = new float[5];

        public PinwheelGarden(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pinwheel_garden", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(9.0);
            config.setDamageRadius(1.2);
            config.setTicksBetweenDamage(8);
            config.setDamageDelayTicks(5);
            config.setDurationTicks(300);
            config.setCooldownTicks(220);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 5 positions: center, N, S, E, W
            double[][] positions = {{0, 0, 0}, {0, 0, -3.0}, {0, 0, 3.0}, {3.0, 0, 0}, {-3.0, 0, 0}};
            Material[] colors = {
                    Material.PINK_CONCRETE, Material.YELLOW_CONCRETE, Material.BLUE_CONCRETE,
                    Material.WHITE_CONCRETE, Material.RED_CONCRETE
            };
            int[][] glowRGB = {
                    {255, 180, 200}, {255, 220, 130}, {180, 220, 255}, {250, 240, 220}, {255, 100, 100}
            };

            for (int p = 0; p < 5; p++) {
                List<BlockDisplayHandle> pinwheel = new ArrayList<>();
                double cx = positions[p][0];
                double cz = positions[p][2];

                // Post (3 blocks)
                for (int s = 0; s < 3; s++) {
                    Location loc = center.clone().add(cx, 0.4 + s * 0.6, cz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.STRIPPED_OAK_LOG);
                    h.scale(0.15f, 0.6f, 0.15f).glow(180, 140, 90).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    pinwheel.add(h);
                }

                // 4 blades (2 blocks each = 8)
                for (int b = 0; b < 4; b++) {
                    double bladeAngle = (2.0 * Math.PI * b) / 4;
                    for (int seg = 0; seg < 2; seg++) {
                        double r = 0.3 + seg * 0.4;
                        double bx = Math.cos(bladeAngle) * r;
                        double bz = Math.sin(bladeAngle) * r;
                        Location loc = center.clone().add(cx + bx, 2.4, cz + bz);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, colors[p]);
                        h.scale(1.2f, 0.08f, 0.6f);
                        h.rotate((float) bladeAngle, 0, 1, 0);
                        h.glow(glowRGB[p][0], glowRGB[p][1], glowRGB[p][2]).interpolation(3, 0);
                        spawnedEntities.add(h.entity());
                        pinwheel.add(h);
                    }
                }
                pinwheels.add(pinwheel);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_FLUTE, 0.8f, 1.2f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Each pinwheel rotates at its own speed (only blade portion)
            if (tick % 3 == 0) {
                for (int p = 0; p < pinwheels.size(); p++) {
                    rotations[p] += speeds[p];
                    List<BlockDisplayHandle> pw = pinwheels.get(p);
                    // First 3 blocks are post (no rotation), remaining 8 are blades
                    for (int i = 3; i < pw.size(); i++) {
                        BlockDisplay entity = pw.get(i).entity();
                        Transformation t = entity.getTransformation();
                        entity.setInterpolationDuration(3);
                        entity.setInterpolationDelay(0);
                        entity.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f(rotations[p], 0f, 1f, 0f),
                                t.getScale(),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                }
            }

            // Particles: CRIT trail from blade tips
            if (tick % 4 == 0) {
                double[][] positions = {{0, 0, 0}, {0, 0, -3.0}, {0, 0, 3.0}, {3.0, 0, 0}, {-3.0, 0, 0}};
                for (double[] pos : positions) {
                    c.getWorld().spawnParticle(Particle.CRIT,
                            c.clone().add(pos[0], 2.4, pos[2]), 4, 0.7, 0.1, 0.7, 0.05);
                }
            }

            // Sound: each pinwheel a different note pitch
            if (tick % 18 == 0) {
                float[] pitches = {1.5f, 1.7f, 1.4f, 1.9f, 1.6f};
                int idx = (tick / 18) % 5;
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_FLUTE, 0.5f, pitches[idx]);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new PinwheelGarden(plugin); }
    }

    // ================================================================
    // #29 — JACK IN THE BOX
    // 52 blocks: box (24 terracotta sides), crank (4), spring (6), head (8 white),
    // hat (4 purple), collar (6) = 52. Crank winds, then lid pops, spring extends,
    // head appears.
    // Impact-only on spring extension, radius 3.5, 22 hearts.
    // ================================================================
    public static class JackInBox extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> box = new ArrayList<>();
        private final List<BlockDisplayHandle> crank = new ArrayList<>();
        private final List<BlockDisplayHandle> spring = new ArrayList<>();
        private final List<BlockDisplayHandle> headParts = new ArrayList<>();
        private boolean popped = false;
        private float crankRotation = 0f;

        public JackInBox(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("jack_in_box", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
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

            // Box (24 blocks total) — 6 sides × 4 blocks per face
            Material[] terracottaColors = {
                    Material.PINK_TERRACOTTA, Material.YELLOW_TERRACOTTA, Material.BLUE_TERRACOTTA,
                    Material.GREEN_TERRACOTTA, Material.RED_TERRACOTTA, Material.ORANGE_TERRACOTTA
            };
            int boxIdx = 0;
            // 4 vertical sides, 4 blocks each
            int[][] sides = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] s : sides) {
                for (int i = 0; i < 4; i++) {
                    double off = -0.6 + (i / 2) * 1.2;
                    double yOff = 0.4 + (i % 2) * 0.8;
                    double px = s[0] != 0 ? s[0] * 1.0 : off;
                    double pz = s[1] != 0 ? s[1] * 1.0 : off;
                    Location loc = center.clone().add(px, yOff, pz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, terracottaColors[boxIdx % 6]);
                    h.scale(s[0] != 0 ? 0.15f : 0.6f, 0.6f, s[1] != 0 ? 0.15f : 0.6f);
                    h.glow(220, 180, 200).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    box.add(h);
                    boxIdx++;
                }
            }
            // Bottom (4 blocks)
            for (int i = 0; i < 4; i++) {
                double xOff = -0.5 + (i % 2) * 1.0;
                double zOff = -0.5 + (i / 2) * 1.0;
                Location loc = center.clone().add(xOff, 0.05, zOff);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, terracottaColors[boxIdx % 6]);
                h.scale(0.55f, 0.1f, 0.55f).glow(220, 180, 200).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                box.add(h);
                boxIdx++;
            }
            // Top lid (4 blocks)
            for (int i = 0; i < 4; i++) {
                double xOff = -0.5 + (i % 2) * 1.0;
                double zOff = -0.5 + (i / 2) * 1.0;
                Location loc = center.clone().add(xOff, 1.6, zOff);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, terracottaColors[boxIdx % 6]);
                h.scale(0.55f, 0.1f, 0.55f).glow(220, 180, 200).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                box.add(h);
                boxIdx++;
            }

            // Crank (4 blocks on side)
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(1.2, 0.7 + i * 0.15, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0.3f, 0.1f, 0.1f).glow(220, 220, 230).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                crank.add(h);
            }

            // Spring (6 blocks) — start hidden inside box (Y near bottom)
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(0, 0.3 + i * 0.05, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0.15f, 0.1f, 0.15f).glow(220, 220, 230).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                spring.add(h);
            }

            // Head (8 white) + hat (4 purple) + collar (6) — start hidden below lid
            // Head: 8 white blocks in rough sphere
            double[][] headOffsets = {
                    {0, 0, 0}, {0.3, 0.1, 0}, {-0.3, 0.1, 0}, {0, 0.3, 0},
                    {0, -0.3, 0}, {0, 0, 0.3}, {0, 0, -0.3}, {0, 0.4, 0}
            };
            for (double[] hp : headOffsets) {
                Location loc = center.clone().add(hp[0], 0.5 + hp[1], hp[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_CONCRETE);
                h.scale(0.3f, 0.3f, 0.3f).glow(250, 240, 220).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                headParts.add(h);
            }
            // Hat (4 purple) on top of head
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(0, 0.6 + i * 0.15, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_CONCRETE);
                float taper = 0.4f - i * 0.08f;
                h.scale(taper, 0.15f, taper).glow(200, 160, 240).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                headParts.add(h);
            }
            // Collar (6 blocks, ring around base of head)
            for (int i = 0; i < 6; i++) {
                double angle = (2.0 * Math.PI * i) / 6;
                double px = Math.cos(angle) * 0.4;
                double pz = Math.sin(angle) * 0.4;
                Location loc = center.clone().add(px, 0.45, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIME_CONCRETE);
                h.scale(0.2f, 0.15f, 0.2f).glow(180, 240, 180).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                headParts.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Wind phase: tick 0..30 — crank spins
            if (!popped && tick < 30) {
                crankRotation += 0.3f;
                if (tick % 2 == 0) {
                    for (BlockDisplayHandle h : crank) {
                        BlockDisplay entity = h.entity();
                        Transformation t = entity.getTransformation();
                        entity.setInterpolationDuration(2);
                        entity.setInterpolationDelay(0);
                        entity.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f(crankRotation, 1f, 0f, 0f),
                                t.getScale(),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                }
                if (tick % 4 == 0) {
                    c.getWorld().spawnParticle(Particle.ENCHANT, c.clone().add(0, 1.0, 0), 4, 0.5, 0.5, 0.5, 0.05);
                    DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.4f + (tick / 30.0f) * 0.5f);
                }
            }

            // Pop at tick 30
            if (!popped && tick >= 30) {
                popped = true;
                // Spring extends Y 0 -> 3 over 3 ticks
                for (int i = 0; i < spring.size(); i++) {
                    BlockDisplay entity = spring.get(i).entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(3);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            new Vector3f(-0.5f, (i * 0.5f) + 0.5f - 0.5f, -0.5f),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
                // Head jumps up to spring top
                for (BlockDisplayHandle h : headParts) {
                    BlockDisplay entity = h.entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(3);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            new Vector3f(-0.5f, 2.5f, -0.5f),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }

                triggerImpactDamage(c.clone().add(0, 2.0, 0));
                DisplayBuilder.playSound(c, Sound.ENTITY_WITCH_CELEBRATE, 1.5f, 1.8f);
                c.getWorld().spawnParticle(Particle.CRIT, c.clone().add(0, 2.5, 0), 50, 1, 1, 1, 0.3);
                c.getWorld().spawnParticle(Particle.FIREWORK, c.clone().add(0, 2.5, 0), 30, 1, 1, 1, 0.2);
            }

            // After-pop ambient
            if (popped && tick % 12 == 0) {
                c.getWorld().spawnParticle(Particle.ENCHANT, c.clone().add(0, 3.0, 0), 5, 0.5, 0.5, 0.5, 0.05);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new JackInBox(plugin); }
    }

    // ================================================================
    // #30 — DOLLHOUSE STRUCTURE
    // 66 blocks: ground floor (front 8, sides 8, back 8 = 24), second floor (24),
    // roof pyramid (12 pink terracotta), door (2), windows (4) = 66.
    // Lights flicker (glow color cycle every 8 ticks).
    // Constant footprint 3.0, 9 hearts, 15-tick interval, 20-tick delay.
    // ================================================================
    public static class DollhouseStructure extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> walls = new ArrayList<>();
        private final List<BlockDisplayHandle> roof = new ArrayList<>();
        private final List<BlockDisplayHandle> windows = new ArrayList<>();
        private float pulsePhase = 0f;
        private boolean lightOn = true;

        public DollhouseStructure(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dollhouse_structure", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(9.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(15);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(340);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Ground floor + second floor walls (24 blocks per floor)
            // Each floor: front 8 + sides 8 + back 8 = 24
            for (int floor = 0; floor < 2; floor++) {
                double yBase = 0.4 + floor * 1.6;
                Material wallMat = (floor == 0) ? Material.PINK_TERRACOTTA : Material.WHITE_TERRACOTTA;

                // Front wall (8 blocks): 4 wide × 2 high at z=-1.5
                for (int x = 0; x < 4; x++) {
                    for (int y = 0; y < 2; y++) {
                        Location loc = center.clone().add(-1.5 + x * 1.0, yBase + y * 0.7, -1.5);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, wallMat);
                        h.scale(1.0f, 0.7f, 0.18f).glow(255, 180, 200).interpolation(3, 0);
                        spawnedEntities.add(h.entity());
                        walls.add(h);
                    }
                }
                // Back wall (8 blocks): 4 wide × 2 high at z=+1.5
                for (int x = 0; x < 4; x++) {
                    for (int y = 0; y < 2; y++) {
                        Location loc = center.clone().add(-1.5 + x * 1.0, yBase + y * 0.7, 1.5);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, wallMat);
                        h.scale(1.0f, 0.7f, 0.18f).glow(255, 180, 200).interpolation(3, 0);
                        spawnedEntities.add(h.entity());
                        walls.add(h);
                    }
                }
                // Side walls (4 each = 8 blocks): 2 deep × 2 high on each side
                for (int side = 0; side < 2; side++) {
                    double xPos = (side == 0) ? -1.5 : 1.5;
                    for (int z = 0; z < 2; z++) {
                        for (int y = 0; y < 2; y++) {
                            Location loc = center.clone().add(xPos, yBase + y * 0.7, -0.5 + z * 1.0);
                            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, wallMat);
                            h.scale(0.18f, 0.7f, 1.0f).glow(255, 180, 200).interpolation(3, 0);
                            spawnedEntities.add(h.entity());
                            walls.add(h);
                        }
                    }
                }
            }

            // Roof: 12 pink terracotta in pyramid shape
            // Bottom layer of pyramid (8 blocks around perimeter at Y=4.0)
            double[][] roofBase = {
                    {-1.5, 0, -1.5}, {-0.5, 0, -1.5}, {0.5, 0, -1.5}, {1.5, 0, -1.5},
                    {-1.5, 0, 1.5}, {-0.5, 0, 1.5}, {0.5, 0, 1.5}, {1.5, 0, 1.5}
            };
            for (double[] rb : roofBase) {
                Location loc = center.clone().add(rb[0], 4.0, rb[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PINK_TERRACOTTA);
                h.scale(1.0f, 0.4f, 1.0f).glow(255, 150, 180).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                roof.add(h);
            }
            // Top tier of roof (4 blocks at Y=4.6)
            double[][] roofTop = {{-0.5, 0, -0.5}, {0.5, 0, -0.5}, {-0.5, 0, 0.5}, {0.5, 0, 0.5}};
            for (double[] rt : roofTop) {
                Location loc = center.clone().add(rt[0], 4.6, rt[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PINK_TERRACOTTA);
                h.scale(0.9f, 0.4f, 0.9f).glow(255, 130, 160).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                roof.add(h);
            }

            // Door (2 blocks at center front)
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add(0, 0.4 + i * 0.7, -1.55);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OAK_PLANKS);
                h.scale(0.5f, 0.7f, 0.1f).glow(180, 120, 80).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                walls.add(h);
            }

            // Windows (4 yellow stained glass — light source)
            double[][] winOffsets = {{-1.0, 1.5, -1.55}, {1.0, 1.5, -1.55}, {-1.0, 3.0, -1.55}, {1.0, 3.0, -1.55}};
            for (double[] wo : winOffsets) {
                Location loc = center.clone().add(wo[0], wo[1], wo[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.YELLOW_STAINED_GLASS);
                h.scale(0.5f, 0.5f, 0.08f).glow(255, 220, 180).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                windows.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITCH_CELEBRATE, 0.6f, 1.5f);
            w.spawnParticle(Particle.ENCHANT, center.clone().add(0, 2, 0), 30, 1.5, 2, 1.5, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Light flicker: window glow color cycles every 8 ticks
            if (tick % 8 == 0) {
                lightOn = !lightOn;
                int r = lightOn ? 255 : 200;
                int g = lightOn ? 220 : 150;
                int b = lightOn ? 180 : 120;
                for (BlockDisplayHandle h : windows) {
                    h.entity().setGlowColorOverride(Color.fromRGB(r, g, b));
                }
            }

            // Gentle scale pulse 1.0 -> 1.02 -> 1.0
            pulsePhase += 0.08f;
            if (tick % 5 == 0) {
                float pulse = 1.0f + 0.02f * (float) Math.sin(pulsePhase);
                for (BlockDisplayHandle h : walls) {
                    BlockDisplay entity = h.entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(5);
                    entity.setInterpolationDelay(0);
                    Vector3f s = t.getScale();
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(s.x * pulse / Math.max(1e-3f, pulse), s.y, s.z),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Particles: ENCHANT from windows, SMOKE from chimney
            if (tick % 6 == 0) {
                double[][] winOffsets = {{-1.0, 1.5, -1.55}, {1.0, 1.5, -1.55}, {-1.0, 3.0, -1.55}, {1.0, 3.0, -1.55}};
                for (double[] wo : winOffsets) {
                    c.getWorld().spawnParticle(Particle.ENCHANT,
                            c.clone().add(wo[0], wo[1], wo[2]), 2, 0.2, 0.2, 0.2, 0.02);
                }
            }
            if (tick % 10 == 0) {
                c.getWorld().spawnParticle(Particle.SMOKE,
                        c.clone().add(0.5, 4.8, 0), 3, 0.1, 0.3, 0.1, 0.02);
            }

            // Sound
            if (tick % 60 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WITCH_CELEBRATE, 0.4f, 1.6f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new DollhouseStructure(plugin); }
    }
}
