package com.blockforge.chaoscraft.modes.corruption.attacks;

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
 * Corruption Mode — VOID TENDRILS & DARK VINES
 * 15 corruption-themed BlockDisplay attacks featuring dark tendrils,
 * vines, roots, and creeping corruption.
 *
 * Color palette:
 * - Dark purple: RGB(45, 0, 64)
 * - Crimson:     RGB(74, 0, 0)
 * - Green:       RGB(26, 58, 0)
 * - Void blue:   RGB(10, 0, 48)
 *
 * Materials: SCULK, DEEPSLATE, BLACKSTONE, CRYING_OBSIDIAN, OBSIDIAN,
 *            COAL_BLOCK, NETHERRACK, SOUL_SOIL, TINTED_GLASS
 *
 * Sounds: BLOCK_SCULK_SPREAD, BLOCK_SCULK_BREAK, BLOCK_DEEPSLATE_BREAK,
 *         ENTITY_WARDEN_HEARTBEAT
 */
public final class VoidTendrils {

    private VoidTendrils() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CorruptionTentacle(plugin));
        registry.register(new ShadowVines(plugin));
        registry.register(new DarkRoots(plugin));
        registry.register(new CorruptionWhip(plugin));
        registry.register(new GraspingHand(plugin));
        registry.register(new VoidTendrilsAttack(plugin));
        registry.register(new CorruptionLasso(plugin));
        registry.register(new ShadowWeb(plugin));
        registry.register(new CreepingCorruption(plugin));
        registry.register(new DarkEmbrace(plugin));
        registry.register(new CorruptionSerpent(plugin));
        registry.register(new VoidThreads(plugin));
        registry.register(new ThornVine(plugin));
        registry.register(new CorruptionCrawler(plugin));
        registry.register(new DarkIvy(plugin));
    }

    // === Helper: pick a random corruption material ===
    private static final Material[] CORRUPTION_MATERIALS = {
            Material.SCULK, Material.DEEPSLATE, Material.BLACKSTONE,
            Material.CRYING_OBSIDIAN, Material.OBSIDIAN, Material.COAL_BLOCK,
            Material.NETHERRACK, Material.SOUL_SOIL, Material.TINTED_GLASS
    };

    private static Material randomMaterial() {
        return CORRUPTION_MATERIALS[(int) (Math.random() * CORRUPTION_MATERIALS.length)];
    }

    // === Helper: pick a random corruption glow color ===
    private static void applyCorruptionGlow(BlockDisplayHandle handle) {
        switch ((int) (Math.random() * 4)) {
            case 0 -> handle.glow(45, 0, 64);   // dark purple
            case 1 -> handle.glow(74, 0, 0);     // crimson
            case 2 -> handle.glow(26, 58, 0);    // green
            case 3 -> handle.glow(10, 0, 48);    // void blue
        }
    }

    // === Helper: corruption dust particles ===
    private static void corruptionDust(Location loc, int count, double spread) {
        switch ((int) (Math.random() * 4)) {
            case 0 -> DisplayBuilder.dustParticles(loc, count, spread, 45, 0, 64, 1.3f);
            case 1 -> DisplayBuilder.dustParticles(loc, count, spread, 74, 0, 0, 1.3f);
            case 2 -> DisplayBuilder.dustParticles(loc, count, spread, 26, 58, 0, 1.3f);
            case 3 -> DisplayBuilder.dustParticles(loc, count, spread, 10, 0, 48, 1.3f);
        }
    }

    // ================================================================
    // 16. CORRUPTION TENTACLE — 14 blocks snake upward with sine-wave
    //     motion, writhing dark tendril rising from the ground
    // ================================================================
    public static class CorruptionTentacle extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 14;
        private final List<BlockDisplayHandle> segments = new ArrayList<>();
        private float growProgress = 0;

        public CorruptionTentacle(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_tentacle", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(210.0);
            config.setDamageRadius(5.2);
            config.setDurationTicks(640);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                Location loc = center.clone().add(0, i * 0.8, 0);
                BlockDisplayHandle seg = displayBuilder.spawnBlock(loc, randomMaterial());
                seg.scale(0.6f, 0.9f, 0.6f)
                   .interpolation(3, 0);
                applyCorruptionGlow(seg);
                segments.add(seg);
                spawnedEntities.add(seg.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.2f, 0.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            growProgress = Math.min(1.0f, ticksAlive / 60.0f);

            for (int i = 0; i < segments.size(); i++) {
                float segHeight = i * 0.8f * growProgress;
                float sineX = (float) Math.sin(ticksAlive * 0.08 + i * 0.6) * (i * 0.12f);
                float sineZ = (float) Math.cos(ticksAlive * 0.08 + i * 0.6) * (i * 0.12f);

                Location newLoc = c.clone().add(sineX, segHeight, sineZ);
                segments.get(i).entity().teleport(newLoc);

                float tiltAngle = (float) Math.sin(ticksAlive * 0.08 + i * 0.6) * 0.3f;
                segments.get(i).rotate(tiltAngle, 0, 0, 1);
            }

            if (ticksAlive % 5 == 0) {
                int topIdx = (int) (segments.size() * growProgress);
                topIdx = Math.min(topIdx, segments.size() - 1);
                Location tipLoc = segments.get(topIdx).entity().getLocation();
                corruptionDust(tipLoc, 4, 0.3);
            }

            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.6f, 0.5f + (float)(Math.random() * 0.3));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionTentacle(plugin); }
    }

    // ================================================================
    // 17. SHADOW VINES — 20 small blocks descend from 15 blocks up,
    //     sway independently like hanging vines of darkness
    // ================================================================
    public static class ShadowVines extends BlockDisplayAttack {

        private static final int VINE_COUNT = 20;
        private final List<BlockDisplayHandle> vines = new ArrayList<>();
        private final List<Double> offsetsX = new ArrayList<>();
        private final List<Double> offsetsZ = new ArrayList<>();
        private final List<Float> swayPhases = new ArrayList<>();
        private final List<Float> hangLengths = new ArrayList<>();
        private float descendProgress = 0;

        public ShadowVines(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_vines", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(200.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(720);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < VINE_COUNT; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = Math.random() * 5.0;
                double ox = Math.cos(angle) * dist;
                double oz = Math.sin(angle) * dist;
                offsetsX.add(ox);
                offsetsZ.add(oz);
                swayPhases.add((float) (Math.random() * Math.PI * 2));
                hangLengths.add(2.0f + (float) (Math.random() * 4.0));

                Location loc = center.clone().add(ox, 15, oz);
                BlockDisplayHandle vine = displayBuilder.spawnBlock(loc, randomMaterial());
                vine.scale(0.25f, 0.25f, 0.25f)
                    .interpolation(3, 0);
                applyCorruptionGlow(vine);
                vines.add(vine);
                spawnedEntities.add(vine.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, 15, 0), Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            descendProgress = Math.min(1.0f, ticksAlive / 80.0f);
            float currentY = 15.0f - descendProgress * hangLengths.get(0);

            for (int i = 0; i < vines.size(); i++) {
                float vineY = 15.0f - descendProgress * hangLengths.get(i);
                float swayX = (float) Math.sin(ticksAlive * 0.06 + swayPhases.get(i)) * 0.4f;
                float swayZ = (float) Math.cos(ticksAlive * 0.07 + swayPhases.get(i) * 1.3) * 0.4f;

                Location newLoc = c.clone().add(offsetsX.get(i) + swayX, vineY, offsetsZ.get(i) + swayZ);
                vines.get(i).entity().teleport(newLoc);

                float swayAngle = (float) Math.sin(ticksAlive * 0.06 + swayPhases.get(i)) * 0.2f;
                vines.get(i).rotate(swayAngle, 0, 0, 1);
            }

            if (ticksAlive % 6 == 0) {
                int idx = (int) (Math.random() * vines.size());
                corruptionDust(vines.get(idx).entity().getLocation(), 3, 0.2);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowVines(plugin); }
    }

    // ================================================================
    // 18. DARK ROOTS — 15 blocks emerge from ground in branching root
    //     patterns, spreading outward like a corrupted root system
    // ================================================================
    public static class DarkRoots extends BlockDisplayAttack {

        private static final int ROOT_COUNT = 15;
        private final List<BlockDisplayHandle> roots = new ArrayList<>();
        private final List<Double> branchAngles = new ArrayList<>();
        private final List<Double> branchLengths = new ArrayList<>();
        private final List<Integer> branchDepths = new ArrayList<>();
        private float growProgress = 0;

        public DarkRoots(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_roots", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(220.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(680);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Generate branching structure: 3 main roots, each branches further
            int idx = 0;
            for (int branch = 0; branch < 3 && idx < ROOT_COUNT; branch++) {
                double mainAngle = (Math.PI * 2 / 3) * branch + Math.random() * 0.4;
                for (int seg = 0; seg < 5 && idx < ROOT_COUNT; seg++) {
                    double subAngle = mainAngle + (Math.random() - 0.5) * 0.6;
                    double length = 1.2 + seg * 1.0 + Math.random() * 0.5;
                    branchAngles.add(subAngle);
                    branchLengths.add(length);
                    branchDepths.add(seg);

                    Location loc = center.clone().add(
                            Math.cos(subAngle) * length,
                            -0.3 * seg,
                            Math.sin(subAngle) * length
                    );
                    BlockDisplayHandle root = displayBuilder.spawnBlock(loc, randomMaterial());
                    float thickness = 0.8f - seg * 0.1f;
                    root.scale(thickness, 0.4f, thickness)
                        .interpolation(3, 0);
                    applyCorruptionGlow(root);
                    roots.add(root);
                    spawnedEntities.add(root.entity());
                    idx++;
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_BREAK, 1.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.7f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            growProgress = Math.min(1.0f, ticksAlive / 70.0f);

            for (int i = 0; i < roots.size(); i++) {
                double angle = branchAngles.get(i);
                double length = branchLengths.get(i) * growProgress;
                int depth = branchDepths.get(i);

                float pulseY = (float) Math.sin(ticksAlive * 0.1 + i * 0.5) * 0.08f;
                Location newLoc = c.clone().add(
                        Math.cos(angle) * length,
                        -0.3 * depth + pulseY,
                        Math.sin(angle) * length
                );
                roots.get(i).entity().teleport(newLoc);

                float rotAngle = (float) Math.atan2(Math.sin(angle), Math.cos(angle));
                roots.get(i).rotate(rotAngle, 0, 1, 0);
            }

            if (ticksAlive % 4 == 0) {
                int idx = (int) (Math.random() * roots.size());
                corruptionDust(roots.get(idx).entity().getLocation(), 3, 0.4);
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkRoots(plugin); }
    }

    // ================================================================
    // 19. CORRUPTION WHIP — 12 blocks form long thin tendril that
    //     snaps and cracks toward a random direction
    // ================================================================
    public static class CorruptionWhip extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 12;
        private final List<BlockDisplayHandle> segments = new ArrayList<>();
        private double whipAngle;
        private boolean snapped = false;
        private int snapTick = 0;

        public CorruptionWhip(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_whip", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(240.0);
            config.setDamageRadius(4.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            whipAngle = Math.random() * Math.PI * 2;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                Location loc = center.clone().add(0, 2.0, 0);
                BlockDisplayHandle seg = displayBuilder.spawnBlock(loc, randomMaterial());
                seg.scale(0.3f, 0.3f, 0.3f + i * 0.05f)
                   .interpolation(2, 0);
                applyCorruptionGlow(seg);
                segments.add(seg);
                spawnedEntities.add(seg.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Wind-up phase: coil back (ticks 0-40)
            if (ticksAlive < 40) {
                float coilProgress = ticksAlive / 40.0f;
                for (int i = 0; i < segments.size(); i++) {
                    double backAngle = whipAngle + Math.PI;
                    float dist = i * 0.4f * coilProgress;
                    float height = 2.0f + (float) Math.sin(i * 0.5) * 0.5f * coilProgress;
                    Location loc = c.clone().add(
                            Math.cos(backAngle) * dist,
                            height,
                            Math.sin(backAngle) * dist
                    );
                    segments.get(i).entity().teleport(loc);
                    float coilRot = coilProgress * i * 0.15f;
                    segments.get(i).rotate(coilRot, 0, 1, 0);
                }
            }
            // Snap phase: whip cracks forward (ticks 40-55)
            else if (ticksAlive < 55) {
                if (!snapped) {
                    snapped = true;
                    snapTick = ticksAlive;
                    DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 1.5f);
                }
                float snapProgress = (ticksAlive - 40) / 15.0f;
                for (int i = 0; i < segments.size(); i++) {
                    float segDelay = Math.max(0, snapProgress - i * 0.06f);
                    segDelay = Math.min(1.0f, segDelay);
                    float dist = i * 0.6f * segDelay;
                    float height = 2.0f - segDelay * 0.8f;
                    Location loc = c.clone().add(
                            Math.cos(whipAngle) * dist,
                            height,
                            Math.sin(whipAngle) * dist
                    );
                    segments.get(i).entity().teleport(loc);
                    float crackRot = segDelay * 0.4f;
                    segments.get(i).rotate(crackRot, 0, 0, 1);
                }
            }
            // Recoil phase: tendril settles (ticks 55+)
            else {
                float recoil = Math.min(1.0f, (ticksAlive - 55) / 30.0f);
                for (int i = 0; i < segments.size(); i++) {
                    float dist = i * 0.6f * (1.0f - recoil * 0.3f);
                    float height = 1.2f + recoil * 0.5f;
                    float sway = (float) Math.sin(ticksAlive * 0.1 + i) * 0.15f * (1.0f - recoil);
                    Location loc = c.clone().add(
                            Math.cos(whipAngle) * dist + sway,
                            height,
                            Math.sin(whipAngle) * dist
                    );
                    segments.get(i).entity().teleport(loc);
                }
            }

            if (ticksAlive % 4 == 0) {
                corruptionDust(segments.get(segments.size() - 1).entity().getLocation(), 4, 0.3);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionWhip(plugin); }
    }

    // ================================================================
    // 20. GRASPING HAND — 15 blocks form giant hand rising from
    //     ground, fingers slowly close into a fist
    // ================================================================
    public static class GraspingHand extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 15;
        private final List<BlockDisplayHandle> blocks = new ArrayList<>();
        // 5 fingers x 3 segments each
        private static final int FINGERS = 5;
        private static final int SEGS_PER_FINGER = 3;
        private float riseProgress = 0;
        private float closeProgress = 0;

        public GraspingHand(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("grasping_hand", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(230.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(700);
            config.setCooldownTicks(340);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int finger = 0; finger < FINGERS; finger++) {
                for (int seg = 0; seg < SEGS_PER_FINGER; seg++) {
                    Location loc = center.clone();
                    BlockDisplayHandle block = displayBuilder.spawnBlock(loc, randomMaterial());
                    float segScale = 0.7f - seg * 0.15f;
                    block.scale(segScale, segScale * 1.5f, segScale)
                         .interpolation(3, 0);
                    applyCorruptionGlow(block);
                    blocks.add(block);
                    spawnedEntities.add(block.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.2f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            riseProgress = Math.min(1.0f, ticksAlive / 60.0f);
            closeProgress = Math.max(0, Math.min(1.0f, (ticksAlive - 80) / 60.0f));

            double[] fingerAngles = { 0, Math.PI * 0.35, Math.PI * 0.7, Math.PI * 1.05, Math.PI * 1.4 };
            double baseRadius = 2.5;

            for (int finger = 0; finger < FINGERS; finger++) {
                double angle = fingerAngles[finger];
                for (int seg = 0; seg < SEGS_PER_FINGER; seg++) {
                    int idx = finger * SEGS_PER_FINGER + seg;
                    if (idx >= blocks.size()) break;

                    float segHeight = (seg + 1) * 1.5f * riseProgress;
                    double outward = baseRadius * (1.0 - closeProgress * 0.6) * (1.0 - seg * 0.15);
                    // Fingers curl inward as closeProgress increases
                    double curlAngle = angle + closeProgress * seg * 0.3;
                    float curlInward = closeProgress * seg * 0.4f;

                    Location loc = c.clone().add(
                            Math.cos(curlAngle) * (outward - curlInward),
                            segHeight,
                            Math.sin(curlAngle) * (outward - curlInward)
                    );
                    blocks.get(idx).entity().teleport(loc);

                    float bendAngle = closeProgress * seg * 0.5f;
                    blocks.get(idx).rotate(bendAngle, (float) Math.cos(angle), 0, (float) Math.sin(angle));
                }
            }

            if (ticksAlive % 5 == 0) {
                corruptionDust(c.clone().add(0, riseProgress * 4, 0), 6, 1.0);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GraspingHand(plugin); }
    }

    // ================================================================
    // 21. VOID TENDRILS — 12 thin tendrils reach outward in all
    //     cardinal + ordinal directions from center
    // ================================================================
    public static class VoidTendrilsAttack extends BlockDisplayAttack {

        private static final int TENDRIL_COUNT = 12;
        private final List<BlockDisplayHandle> tendrils = new ArrayList<>();
        private final List<Double> directions = new ArrayList<>();
        private float reachProgress = 0;

        public VoidTendrilsAttack(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_tendrils", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(210.0);
            config.setDamageRadius(7.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 8 cardinal/ordinal directions + 4 diagonal up/down
            for (int i = 0; i < TENDRIL_COUNT; i++) {
                double angle = (Math.PI * 2 / TENDRIL_COUNT) * i;
                directions.add(angle);

                Location loc = center.clone();
                BlockDisplayHandle tendril = displayBuilder.spawnBlock(loc, randomMaterial());
                tendril.scale(0.2f, 0.2f, 1.5f)
                       .interpolation(3, 0);
                applyCorruptionGlow(tendril);
                tendrils.add(tendril);
                spawnedEntities.add(tendril.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.2f, 0.2f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.9f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            reachProgress = Math.min(1.0f, ticksAlive / 50.0f);
            float maxReach = 6.0f;

            for (int i = 0; i < tendrils.size(); i++) {
                double angle = directions.get(i);
                float reach = maxReach * reachProgress;
                float verticalWave = (float) Math.sin(ticksAlive * 0.08 + i) * 0.4f;
                float wobble = (float) Math.sin(ticksAlive * 0.12 + i * 1.5) * 0.2f;

                Location loc = c.clone().add(
                        Math.cos(angle) * reach + wobble,
                        0.5 + verticalWave,
                        Math.sin(angle) * reach + wobble
                );
                tendrils.get(i).entity().teleport(loc);

                // Point tendril outward
                float rotAngle = (float) angle;
                tendrils.get(i).rotate(rotAngle, 0, 1, 0);
            }

            if (ticksAlive % 4 == 0) {
                int idx = (int) (Math.random() * tendrils.size());
                corruptionDust(tendrils.get(idx).entity().getLocation(), 3, 0.3);
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_BREAK, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidTendrilsAttack(plugin); }
    }

    // ================================================================
    // 22. CORRUPTION LASSO — 14 blocks form loop tightening in
    //     a circle around the area, constricting inward
    // ================================================================
    public static class CorruptionLasso extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 14;
        private final List<BlockDisplayHandle> links = new ArrayList<>();
        private float constrict = 0;
        private static final float INITIAL_RADIUS = 7.0f;
        private static final float FINAL_RADIUS = 1.5f;

        public CorruptionLasso(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_lasso", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(220.0);
            config.setDamageRadius(4.5);
            config.setDurationTicks(560);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                double angle = (Math.PI * 2 / BLOCK_COUNT) * i;
                Location loc = center.clone().add(
                        Math.cos(angle) * INITIAL_RADIUS, 1.5,
                        Math.sin(angle) * INITIAL_RADIUS
                );
                BlockDisplayHandle link = displayBuilder.spawnBlock(loc, randomMaterial());
                link.scale(0.5f, 0.5f, 0.5f)
                    .interpolation(3, 0);
                applyCorruptionGlow(link);
                links.add(link);
                spawnedEntities.add(link.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            constrict = Math.min(1.0f, ticksAlive / 100.0f);
            float radius = INITIAL_RADIUS - constrict * (INITIAL_RADIUS - FINAL_RADIUS);
            float rotationOffset = ticksAlive * 0.04f;

            for (int i = 0; i < links.size(); i++) {
                double angle = (Math.PI * 2 / links.size()) * i + rotationOffset;
                float yBob = (float) Math.sin(ticksAlive * 0.1 + i * 0.8) * 0.3f;

                Location loc = c.clone().add(
                        Math.cos(angle) * radius,
                        1.5 + yBob,
                        Math.sin(angle) * radius
                );
                links.get(i).entity().teleport(loc);

                // Tilt links to face center
                float tiltAngle = (float) angle + (float) Math.PI * 0.5f;
                links.get(i).rotate(tiltAngle, 0, 1, 0);
            }

            // Particle trail connecting links
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < links.size(); i++) {
                    Location a = links.get(i).entity().getLocation();
                    Location b = links.get((i + 1) % links.size()).entity().getLocation();
                    Location mid = a.clone().add(b).multiply(0.5);
                    corruptionDust(mid, 2, 0.2);
                }
            }

            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, 0.7f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionLasso(plugin); }
    }

    // ================================================================
    // 23. SHADOW WEB — 18 blocks form spider-web pattern between
    //     4 anchor points, threads stretch between them
    // ================================================================
    public static class ShadowWeb extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 18;
        private final List<BlockDisplayHandle> webBlocks = new ArrayList<>();
        private final double[][] anchors = new double[4][2]; // 4 anchor XZ positions
        private float weaveProgress = 0;

        public ShadowWeb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_web", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(200.0);
            config.setDamageRadius(7.5);
            config.setDurationTicks(720);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 4 anchor points in a square pattern
            double anchorDist = 4.0;
            anchors[0] = new double[] { anchorDist,  anchorDist};
            anchors[1] = new double[] { anchorDist, -anchorDist};
            anchors[2] = new double[] {-anchorDist, -anchorDist};
            anchors[3] = new double[] {-anchorDist,  anchorDist};

            for (int i = 0; i < BLOCK_COUNT; i++) {
                Location loc = center.clone().add(0, 3, 0);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, randomMaterial());
                block.scale(0.15f, 0.15f, 0.15f)
                     .interpolation(3, 0);
                applyCorruptionGlow(block);
                webBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            weaveProgress = Math.min(1.0f, ticksAlive / 80.0f);

            int idx = 0;
            // 4 threads from anchors to center (4 blocks each = 16)
            for (int a = 0; a < 4 && idx < webBlocks.size(); a++) {
                int blocksPerThread = 4;
                for (int seg = 0; seg < blocksPerThread && idx < webBlocks.size(); seg++) {
                    float t = (seg + 1.0f) / blocksPerThread * weaveProgress;
                    double x = anchors[a][0] * (1.0 - t);
                    double z = anchors[a][1] * (1.0 - t);
                    float sag = (float) Math.sin(t * Math.PI) * 0.6f;
                    float shimmer = (float) Math.sin(ticksAlive * 0.1 + idx) * 0.08f;

                    Location loc = c.clone().add(x, 3.0 - sag + shimmer, z);
                    webBlocks.get(idx).entity().teleport(loc);
                    idx++;
                }
            }

            // 2 cross-threads between opposing anchors (1 block each for remainder)
            for (; idx < webBlocks.size(); idx++) {
                int crossIdx = idx - 16;
                float t = (crossIdx + 1.0f) / (webBlocks.size() - 16) * weaveProgress;
                // Alternate between the two diagonals
                int fromA = (crossIdx % 2 == 0) ? 0 : 1;
                int toA = fromA + 2;
                double x = anchors[fromA][0] * (1.0 - t) + anchors[toA][0] * t;
                double z = anchors[fromA][1] * (1.0 - t) + anchors[toA][1] * t;
                float sag = (float) Math.sin(t * Math.PI) * 0.4f;
                float shimmer = (float) Math.sin(ticksAlive * 0.12 + idx) * 0.06f;

                Location loc = c.clone().add(x, 3.0 - sag + shimmer, z);
                webBlocks.get(idx).entity().teleport(loc);
            }

            if (ticksAlive % 6 == 0) {
                int rIdx = (int) (Math.random() * webBlocks.size());
                corruptionDust(webBlocks.get(rIdx).entity().getLocation(), 2, 0.15);
            }

            if (ticksAlive % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_BREAK, 0.4f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowWeb(plugin); }
    }

    // ================================================================
    // 24. CREEPING CORRUPTION — 12 blocks move along ground like a
    //     wave of darkness spreading outward in a ring
    // ================================================================
    public static class CreepingCorruption extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 12;
        private final List<BlockDisplayHandle> creeps = new ArrayList<>();
        private final List<Double> angles = new ArrayList<>();
        private float spreadProgress = 0;

        public CreepingCorruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("creeping_corruption", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(200.0);
            config.setDamageRadius(6.8);
            config.setDurationTicks(600);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                double angle = (Math.PI * 2 / BLOCK_COUNT) * i + Math.random() * 0.3;
                angles.add(angle);

                Location loc = center.clone();
                BlockDisplayHandle creep = displayBuilder.spawnBlock(loc, randomMaterial());
                creep.scale(0.8f, 0.2f, 0.8f)
                     .interpolation(3, 0);
                applyCorruptionGlow(creep);
                creeps.add(creep);
                spawnedEntities.add(creep.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.2f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.7f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            spreadProgress = Math.min(1.0f, ticksAlive / 90.0f);
            float maxSpread = 8.0f;

            for (int i = 0; i < creeps.size(); i++) {
                double angle = angles.get(i);
                float dist = maxSpread * spreadProgress;
                // Wave-like undulation
                float wave = (float) Math.sin(ticksAlive * 0.15 - dist * 0.8 + i) * 0.15f;
                float yOscillate = (float) Math.sin(ticksAlive * 0.1 + i * 0.7) * 0.1f;

                // Slightly erratic path
                float wobble = (float) Math.sin(ticksAlive * 0.05 + i * 2.0) * 0.3f;

                Location loc = c.clone().add(
                        Math.cos(angle + wobble * 0.1) * dist,
                        wave + yOscillate,
                        Math.sin(angle + wobble * 0.1) * dist
                );
                creeps.get(i).entity().teleport(loc);

                // Flat rotation matching spread direction
                float rotAngle = (float) angle;
                creeps.get(i).rotate(rotAngle, 0, 1, 0);
            }

            if (ticksAlive % 3 == 0) {
                int idx = (int) (Math.random() * creeps.size());
                Location trailLoc = creeps.get(idx).entity().getLocation();
                corruptionDust(trailLoc, 4, 0.5);
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.4f + spreadProgress * 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CreepingCorruption(plugin); }
    }

    // ================================================================
    // 25. DARK EMBRACE — 16 blocks form two arms closing inward
    //     like a giant trap, crushing from both sides
    // ================================================================
    public static class DarkEmbrace extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 16;
        private static final int BLOCKS_PER_ARM = 8;
        private final List<BlockDisplayHandle> leftArm = new ArrayList<>();
        private final List<BlockDisplayHandle> rightArm = new ArrayList<>();
        private float closeProgress = 0;

        public DarkEmbrace(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_embrace", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(250.0);
            config.setDamageRadius(5.2);
            config.setDurationTicks(600);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Left arm: arcs from left side
            for (int i = 0; i < BLOCKS_PER_ARM; i++) {
                Location loc = center.clone().add(-5, 0, 0);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, randomMaterial());
                block.scale(0.6f, 0.8f, 0.6f)
                     .interpolation(3, 0);
                applyCorruptionGlow(block);
                leftArm.add(block);
                spawnedEntities.add(block.entity());
            }

            // Right arm: arcs from right side
            for (int i = 0; i < BLOCKS_PER_ARM; i++) {
                Location loc = center.clone().add(5, 0, 0);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, randomMaterial());
                block.scale(0.6f, 0.8f, 0.6f)
                     .interpolation(3, 0);
                applyCorruptionGlow(block);
                rightArm.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            closeProgress = Math.min(1.0f, ticksAlive / 80.0f);
            float armSpread = 5.0f * (1.0f - closeProgress * 0.85f);

            // Left arm: sweeps from left, curving upward then inward
            for (int i = 0; i < leftArm.size(); i++) {
                float t = (float) i / (BLOCKS_PER_ARM - 1);
                float arcAngle = (float) (Math.PI * 0.5 + t * Math.PI * 0.6);
                float x = -armSpread + (float) Math.cos(arcAngle) * (2.0f + t * 2.0f);
                float y = (float) Math.sin(arcAngle) * (2.0f + t * 1.5f);
                float pulse = (float) Math.sin(ticksAlive * 0.08 + i * 0.4) * 0.1f;

                Location loc = c.clone().add(x, y + pulse, 0);
                leftArm.get(i).entity().teleport(loc);
                leftArm.get(i).rotate(arcAngle, 0, 0, 1);
            }

            // Right arm: mirrors left
            for (int i = 0; i < rightArm.size(); i++) {
                float t = (float) i / (BLOCKS_PER_ARM - 1);
                float arcAngle = (float) (Math.PI * 0.5 - t * Math.PI * 0.6);
                float x = armSpread - (float) Math.cos(arcAngle) * (2.0f + t * 2.0f);
                float y = (float) Math.sin(arcAngle) * (2.0f + t * 1.5f);
                float pulse = (float) Math.sin(ticksAlive * 0.08 + i * 0.4) * 0.1f;

                Location loc = c.clone().add(x, y + pulse, 0);
                rightArm.get(i).entity().teleport(loc);
                rightArm.get(i).rotate(-arcAngle, 0, 0, 1);
            }

            if (ticksAlive % 4 == 0) {
                corruptionDust(c.clone().add(0, 2, 0), 6, 1.5);
            }

            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkEmbrace(plugin); }
    }

    // ================================================================
    // 26. CORRUPTION SERPENT — 15 blocks form snake body, slithers
    //     with sine motion along the ground
    // ================================================================
    public static class CorruptionSerpent extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 15;
        private final List<BlockDisplayHandle> body = new ArrayList<>();
        private double headAngle;
        private float slitherSpeed = 0.03f;

        public CorruptionSerpent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_serpent", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(220.0);
            config.setDamageRadius(4.5);
            config.setDurationTicks(800);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            headAngle = Math.random() * Math.PI * 2;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                Location loc = center.clone();
                BlockDisplayHandle seg = displayBuilder.spawnBlock(loc, randomMaterial());
                float segScale = 0.7f - i * 0.03f;
                seg.scale(segScale, 0.4f, segScale)
                   .interpolation(2, 0);
                applyCorruptionGlow(seg);
                body.add(seg);
                spawnedEntities.add(seg.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Head slowly turns
            headAngle += Math.sin(ticksAlive * 0.02) * slitherSpeed;
            float forwardProgress = ticksAlive * 0.06f;

            for (int i = 0; i < body.size(); i++) {
                // Each segment follows the one ahead with a time delay
                float segTime = forwardProgress - i * 0.5f;
                double segAngle = headAngle + Math.sin(segTime * 0.3) * 0.8;

                float dist = Math.max(0, segTime * 0.8f);
                float sineOffset = (float) Math.sin(segTime * 0.6 + i * 0.4) * 1.2f;

                // Convert angle + distance to position
                double x = Math.cos(segAngle) * dist;
                double z = Math.sin(segAngle) * dist;
                // Perpendicular sine wave for slithering
                double perpX = Math.cos(segAngle + Math.PI * 0.5) * sineOffset;
                double perpZ = Math.sin(segAngle + Math.PI * 0.5) * sineOffset;

                float yBob = (float) Math.sin(ticksAlive * 0.12 + i * 0.3) * 0.08f;

                Location loc = c.clone().add(x + perpX, 0.3 + yBob, z + perpZ);
                body.get(i).entity().teleport(loc);

                float rotAngle = (float) segAngle;
                body.get(i).rotate(rotAngle, 0, 1, 0);
            }

            if (ticksAlive % 5 == 0) {
                corruptionDust(body.get(0).entity().getLocation(), 3, 0.3);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionSerpent(plugin); }
    }

    // ================================================================
    // 27. VOID THREADS — 20 tiny blocks form thin lines connecting
    //     random 3D points in space, weaving a void lattice
    // ================================================================
    public static class VoidThreads extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 20;
        private final List<BlockDisplayHandle> threads = new ArrayList<>();
        private final double[][] targetPoints = new double[10][3]; // 10 random 3D anchor points
        private final int[] fromPoint = new int[BLOCK_COUNT];
        private final int[] toPoint = new int[BLOCK_COUNT];
        private float weaveProgress = 0;

        public VoidThreads(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_threads", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(200.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(680);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Generate 10 random anchor points within a 6-block sphere
            for (int i = 0; i < 10; i++) {
                targetPoints[i] = new double[] {
                        (Math.random() - 0.5) * 12,
                        1.0 + Math.random() * 6,
                        (Math.random() - 0.5) * 12
                };
            }

            // Each thread connects two random anchor points
            for (int i = 0; i < BLOCK_COUNT; i++) {
                fromPoint[i] = (int) (Math.random() * 10);
                int to;
                do { to = (int) (Math.random() * 10); } while (to == fromPoint[i]);
                toPoint[i] = to;

                Location loc = center.clone();
                BlockDisplayHandle thread = displayBuilder.spawnBlock(loc, randomMaterial());
                thread.scale(0.1f, 0.1f, 0.1f)
                      .interpolation(3, 0);
                applyCorruptionGlow(thread);
                threads.add(thread);
                spawnedEntities.add(thread.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 0.8f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            weaveProgress = Math.min(1.0f, ticksAlive / 60.0f);

            for (int i = 0; i < threads.size(); i++) {
                double[] from = targetPoints[fromPoint[i]];
                double[] to = targetPoints[toPoint[i]];
                float t = ((float) (i % 5) / 5.0f + 0.1f) * weaveProgress;

                double x = from[0] * (1.0 - t) + to[0] * t;
                double y = from[1] * (1.0 - t) + to[1] * t;
                double z = from[2] * (1.0 - t) + to[2] * t;

                float shimmer = (float) Math.sin(ticksAlive * 0.15 + i * 0.8) * 0.12f;
                float pulse = (float) Math.sin(ticksAlive * 0.06 + i * 1.2) * 0.08f;

                Location loc = c.clone().add(x + shimmer, y + pulse, z + shimmer);
                threads.get(i).entity().teleport(loc);

                // Orient along the thread direction
                float dirAngle = (float) Math.atan2(to[2] - from[2], to[0] - from[0]);
                threads.get(i).rotate(dirAngle, 0, 1, 0);
            }

            if (ticksAlive % 5 == 0) {
                int idx = (int) (Math.random() * threads.size());
                corruptionDust(threads.get(idx).entity().getLocation(), 2, 0.15);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_BREAK, 0.4f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidThreads(plugin); }
    }

    // ================================================================
    // 28. THORN VINE — 14 blocks with spiky protrusions, wraps
    //     spiraling upward like a corrupted thorn-covered vine
    // ================================================================
    public static class ThornVine extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 14;
        private final List<BlockDisplayHandle> thorns = new ArrayList<>();
        private float spiralProgress = 0;

        public ThornVine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("thorn_vine", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(230.0);
            config.setDamageRadius(4.5);
            config.setDurationTicks(640);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                Location loc = center.clone();
                BlockDisplayHandle thorn = displayBuilder.spawnBlock(loc, randomMaterial());
                // Alternate between vine segments and spiky thorn protrusions
                if (i % 3 == 2) {
                    // Thorn: small spiky block
                    thorn.scale(0.2f, 0.5f, 0.2f);
                } else {
                    // Vine segment
                    thorn.scale(0.4f, 0.6f, 0.4f);
                }
                thorn.interpolation(3, 0);
                applyCorruptionGlow(thorn);
                thorns.add(thorn);
                spawnedEntities.add(thorn.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            spiralProgress = Math.min(1.0f, ticksAlive / 70.0f);

            float spiralRadius = 1.5f;
            float heightPerBlock = 0.7f;
            float spinRate = ticksAlive * 0.03f;

            for (int i = 0; i < thorns.size(); i++) {
                float height = i * heightPerBlock * spiralProgress;
                double spiralAngle = i * 0.6 + spinRate;
                float radius = spiralRadius;

                // Thorns protrude outward further
                if (i % 3 == 2) {
                    radius += 0.6f;
                }

                float x = (float) Math.cos(spiralAngle) * radius;
                float z = (float) Math.sin(spiralAngle) * radius;
                float pulse = (float) Math.sin(ticksAlive * 0.1 + i * 0.5) * 0.06f;

                Location loc = c.clone().add(x, height + pulse, z);
                thorns.get(i).entity().teleport(loc);

                // Thorns point outward, vine segments align with spiral
                float rotAngle = (float) spiralAngle;
                if (i % 3 == 2) {
                    // Thorn: tilt outward
                    thorns.get(i).rotate(0.5f, (float) Math.cos(spiralAngle), 0, (float) Math.sin(spiralAngle));
                } else {
                    thorns.get(i).rotate(rotAngle, 0, 1, 0);
                }
            }

            if (ticksAlive % 4 == 0) {
                int topIdx = Math.min((int) (thorns.size() * spiralProgress), thorns.size() - 1);
                corruptionDust(thorns.get(topIdx).entity().getLocation(), 3, 0.3);
            }

            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_BREAK, 0.6f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ThornVine(plugin); }
    }

    // ================================================================
    // 29. CORRUPTION CRAWLER — 12 blocks form spider shape, walks
    //     across the ground with animated legs
    // ================================================================
    public static class CorruptionCrawler extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 12;
        // 4 blocks = body, 8 blocks = legs (4 pairs)
        private final List<BlockDisplayHandle> bodyBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> legBlocks = new ArrayList<>();
        private double crawlAngle;
        private float crawlDist = 0;

        public CorruptionCrawler(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_crawler", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(220.0);
            config.setDamageRadius(4.5);
            config.setDurationTicks(720);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            crawlAngle = Math.random() * Math.PI * 2;

            // Body: 4 blocks
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone();
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, randomMaterial());
                float bodyScale = (i == 0 || i == 3) ? 0.5f : 0.7f;
                block.scale(bodyScale, 0.4f, bodyScale)
                     .interpolation(2, 0);
                applyCorruptionGlow(block);
                bodyBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            // Legs: 8 blocks (4 pairs, left and right)
            for (int i = 0; i < 8; i++) {
                Location loc = center.clone();
                BlockDisplayHandle leg = displayBuilder.spawnBlock(loc, randomMaterial());
                leg.scale(0.15f, 0.15f, 0.6f)
                   .interpolation(2, 0);
                applyCorruptionGlow(leg);
                legBlocks.add(leg);
                spawnedEntities.add(leg.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slowly turn and crawl
            crawlAngle += Math.sin(ticksAlive * 0.015) * 0.03;
            crawlDist += 0.04f;

            double cx = Math.cos(crawlAngle) * crawlDist;
            double cz = Math.sin(crawlAngle) * crawlDist;
            Location bodyCenter = c.clone().add(cx, 0.5, cz);

            // Perpendicular direction for body width
            double perpAngle = crawlAngle + Math.PI * 0.5;
            double forwardX = Math.cos(crawlAngle);
            double forwardZ = Math.sin(crawlAngle);

            // Position body segments along crawl direction
            for (int i = 0; i < bodyBlocks.size(); i++) {
                float offset = (i - 1.5f) * 0.5f;
                float bob = (float) Math.sin(ticksAlive * 0.15 + i) * 0.04f;
                Location loc = bodyCenter.clone().add(
                        forwardX * offset, bob, forwardZ * offset
                );
                bodyBlocks.get(i).entity().teleport(loc);
                bodyBlocks.get(i).rotate((float) crawlAngle, 0, 1, 0);
            }

            // Animate legs: 4 pairs, alternating gait
            for (int i = 0; i < 8; i++) {
                int pair = i / 2;
                boolean isLeft = (i % 2 == 0);
                float pairOffset = (pair - 1.5f) * 0.5f;

                // Alternating leg raise pattern
                float legPhase = ticksAlive * 0.2f + pair * (float) Math.PI * 0.5f;
                float legLift = Math.max(0, (float) Math.sin(legPhase)) * 0.3f;
                float legReach = (float) Math.cos(legPhase) * 0.2f;

                float side = isLeft ? -1.0f : 1.0f;
                double legX = forwardX * (pairOffset + legReach) + Math.cos(perpAngle) * side * 1.0;
                double legZ = forwardZ * (pairOffset + legReach) + Math.sin(perpAngle) * side * 1.0;

                Location legLoc = bodyCenter.clone().add(legX, 0.1 + legLift, legZ);
                legBlocks.get(i).entity().teleport(legLoc);

                // Legs angle outward
                float legAngle = (float) (crawlAngle + (isLeft ? -0.4 : 0.4));
                legBlocks.get(i).rotate(legAngle, 0, 1, 0);
            }

            if (ticksAlive % 6 == 0) {
                corruptionDust(bodyCenter, 3, 0.3);
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(bodyCenter, Sound.BLOCK_DEEPSLATE_BREAK, 0.5f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionCrawler(plugin); }
    }

    // ================================================================
    // 30. DARK IVY — 18 small blocks climb up invisible wall,
    //     spreading pattern like corrupted ivy growing upward
    // ================================================================
    public static class DarkIvy extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 18;
        private final List<BlockDisplayHandle> ivyBlocks = new ArrayList<>();
        private final List<Double> wallOffsets = new ArrayList<>(); // X offset along wall
        private final List<Float> targetHeights = new ArrayList<>();
        private final List<Float> branchSpreads = new ArrayList<>();
        private float growProgress = 0;
        private double wallAngle;

        public DarkIvy(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_ivy", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(200.0);
            config.setDamageRadius(5.2);
            config.setDurationTicks(760);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            wallAngle = Math.random() * Math.PI * 2;

            // Generate ivy growth pattern: starts at base, branches upward and outward
            for (int i = 0; i < BLOCK_COUNT; i++) {
                float height = 0.5f + (float) (Math.random() * 8.0);
                float spread = (float) ((Math.random() - 0.5) * 4.0);
                float branchSpread = (float) ((Math.random() - 0.5) * 1.0);

                wallOffsets.add((double) spread);
                targetHeights.add(height);
                branchSpreads.add(branchSpread);

                Location loc = center.clone();
                BlockDisplayHandle ivy = displayBuilder.spawnBlock(loc, randomMaterial());
                ivy.scale(0.25f, 0.25f, 0.15f)
                   .interpolation(3, 0);
                applyCorruptionGlow(ivy);
                ivyBlocks.add(ivy);
                spawnedEntities.add(ivy.entity());
            }

            // Sort by target height for bottom-up growth
            // (Indices stay the same, growth controlled by progress)

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            growProgress = Math.min(1.0f, ticksAlive / 100.0f);

            double wallNormalX = Math.cos(wallAngle);
            double wallNormalZ = Math.sin(wallAngle);
            double wallTangentX = Math.cos(wallAngle + Math.PI * 0.5);
            double wallTangentZ = Math.sin(wallAngle + Math.PI * 0.5);

            for (int i = 0; i < ivyBlocks.size(); i++) {
                float targetH = targetHeights.get(i);
                float currentH = targetH * growProgress;
                double wallOff = wallOffsets.get(i);
                float branchOff = branchSpreads.get(i) * growProgress;

                // Only show blocks that have "grown" to their position
                float blockGrowThreshold = targetH / 8.5f; // Normalized 0-1 based on height
                if (growProgress < blockGrowThreshold * 0.8f) {
                    // Block hasn't grown yet, keep at ground level hidden
                    Location loc = c.clone().add(wallNormalX * 3, -1, wallNormalZ * 3);
                    ivyBlocks.get(i).entity().teleport(loc);
                    continue;
                }

                // Small sway animation
                float sway = (float) Math.sin(ticksAlive * 0.06 + i * 0.7) * 0.08f;
                float wallDist = 3.0f + branchOff;

                Location loc = c.clone().add(
                        wallNormalX * wallDist + wallTangentX * wallOff + sway,
                        currentH,
                        wallNormalZ * wallDist + wallTangentZ * wallOff + sway
                );
                ivyBlocks.get(i).entity().teleport(loc);

                // Flatten against the wall
                float flatAngle = (float) wallAngle;
                ivyBlocks.get(i).rotate(flatAngle, 0, 1, 0);
            }

            if (ticksAlive % 5 == 0) {
                int idx = (int) (Math.random() * ivyBlocks.size());
                corruptionDust(ivyBlocks.get(idx).entity().getLocation(), 2, 0.2);
            }

            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.5f + growProgress * 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkIvy(plugin); }
    }
}
