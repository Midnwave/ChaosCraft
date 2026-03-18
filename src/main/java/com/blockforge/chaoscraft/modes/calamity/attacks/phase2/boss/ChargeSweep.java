package com.blockforge.chaoscraft.modes.calamity.attacks.phase2.boss;

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
 * Phase 2 Boss Attacks — GROUP 1: CHARGE / SWEEP (#1-10)
 * Body charges, direction changes, spiral dives — the Devourer of Gods'
 * movement as a weapon. Crystalline worm segments smash through the arena.
 * NO status effects — damage only.
 */
public final class ChargeSweep {

    private ChargeSweep() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new StraightCharge(plugin));
        registry.register(new DoubleBackSweep(plugin));
        registry.register(new SpiralDive(plugin));
        registry.register(new LateralSweep(plugin));
        registry.register(new GroundSkim(plugin));
        registry.register(new TightCircleCrush(plugin));
        registry.register(new SCurveWhip(plugin));
        registry.register(new RollingSpiralCharge(plugin));
        registry.register(new DiveBomb(plugin));
        registry.register(new ZigzagRun(plugin));
    }

    // ================================================================
    // 1. STRAIGHT CHARGE — DoG rockets forward in a rigid line
    // ================================================================
    public static class StraightCharge extends BossAttack {
        private final List<BlockDisplayHandle> spineHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> trailHandles = new ArrayList<>();
        private boolean chargeFired = false;
        private int chargeTick = 0;

        public StraightCharge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_straight_charge", AttackType.BOSS, 2), "dog");
            config.setDamage(12.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(160);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Telegraph: spine segments align into a rigid horizontal line
            for (int i = 0; i < 12; i++) {
                Location loc = center.clone().add(-18 + i * 1.2, 4, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(1.0f, 0.8f, 0.8f).glow(0, 200, 255).interpolation(3, 0);
                spineHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            // Telegraph line of spark particles along charge path
            DisplayBuilder.cyanDust(center.clone().add(0, 4, 0), 20, 16.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Head dips and spine straightens (0-30 ticks)
            if (ticksAlive < 30 && !chargeFired) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 4, 0), 8, 14.0);
                }
            }
            // Charge fires at tick 30
            else if (ticksAlive == 30 && !chargeFired) {
                chargeFired = true;
                chargeTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 1.2f);
            }
            // Charge sweeps across arena (30-130 ticks)
            else if (chargeFired && chargeTick < 100) {
                chargeTick++;
                float sweepX = -18 + (chargeTick / 100.0f) * 36;
                for (int i = 0; i < spineHandles.size(); i++) {
                    float segX = sweepX - i * 1.2f;
                    spineHandles.get(i).entity().teleport(
                        center.clone().add(segX, 4, 0));
                }
                // Comet trail particles
                if (chargeTick % 4 == 0) {
                    Location trailLoc = center.clone().add(sweepX - 6, 4, 0);
                    DisplayBuilder.cyanDust(trailLoc, 6, 1.5);
                    // Crystal debris trail
                    BlockDisplayHandle trail = displayBuilder.spawnBlock(
                        trailLoc, Material.AMETHYST_CLUSTER);
                    trail.scale(0.3f, 0.3f, 0.3f).glow(0, 200, 255).interpolation(2, 0);
                    trailHandles.add(trail);
                    spawnedEntities.add(trail.entity());
                }
                if (chargeTick % 8 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StraightCharge(plugin); }
    }

    // ================================================================
    // 2. DOUBLE-BACK SWEEP — Figure-eight crossing arc
    // ================================================================
    public static class DoubleBackSweep extends BossAttack {
        private final List<BlockDisplayHandle> bodyHandles = new ArrayList<>();
        private int sweepPhase = 0;

        public DoubleBackSweep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_double_back_sweep", AttackType.BOSS, 2), "dog");
            config.setDamage(12.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Worm body segments in arc formation
            for (int i = 0; i < 16; i++) {
                Location loc = center.clone().add(-12 + i * 1.5, 6, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.9f, 0.9f, 0.9f).glow(0, 200, 255).interpolation(2, 0);
                bodyHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.cyanDust(center.clone().add(0, 6, 0), 15, 8.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph: pulsing blue/white flickers (0-30 ticks)
            if (ticksAlive < 30) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 6, 0), 10, 10.0);
                }
            }
            // First pass: arc overhead (30-80 ticks)
            else if (ticksAlive >= 30 && ticksAlive < 80) {
                int t = ticksAlive - 30;
                float progress = t / 50.0f;
                for (int i = 0; i < bodyHandles.size(); i++) {
                    float segProgress = Math.max(0, Math.min(1, progress - i * 0.03f));
                    double arcAngle = segProgress * Math.PI;
                    double x = Math.cos(arcAngle) * 14;
                    double y = 6 + Math.sin(arcAngle) * 8;
                    bodyHandles.get(i).entity().teleport(center.clone().add(x, y, 0));
                }
                if (t % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 5, 3.0);
                }
            }
            // Second pass: reverse curve back through (80-130 ticks)
            else if (ticksAlive >= 80 && ticksAlive < 130) {
                if (sweepPhase == 0) {
                    sweepPhase = 1;
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.8f);
                }
                int t = ticksAlive - 80;
                float progress = t / 50.0f;
                for (int i = 0; i < bodyHandles.size(); i++) {
                    float segProgress = Math.max(0, Math.min(1, progress - i * 0.03f));
                    double arcAngle = Math.PI + segProgress * Math.PI;
                    double x = Math.cos(arcAngle) * 14;
                    double y = 6 + Math.sin(arcAngle) * 6;
                    double z = Math.sin(segProgress * Math.PI) * 4;
                    bodyHandles.get(i).entity().teleport(center.clone().add(x, y, z));
                }
                if (t % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 6, 0), 8, 4.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DoubleBackSweep(plugin); }
    }

    // ================================================================
    // 3. SPIRAL DIVE — Corkscrew descent from high altitude
    // ================================================================
    public static class SpiralDive extends BossAttack {
        private final List<BlockDisplayHandle> bodyHandles = new ArrayList<>();
        private boolean diveFired = false;

        public SpiralDive(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_spiral_dive", AttackType.BOSS, 2), "dog");
            config.setDamage(12.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // DoG rises high — segments at apex
            for (int i = 0; i < 10; i++) {
                double angle = (Math.PI * 2 / 10) * i;
                Location loc = center.clone().add(Math.cos(angle) * 2, 28 + i * 0.3, Math.sin(angle) * 2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(1.0f, 1.0f, 1.0f).glow(0, 200, 255).interpolation(2, 0);
                bodyHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            // Rain of reverse portal particles marking landing zone
            DisplayBuilder.cyanDust(center, 25, 5.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.5f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Apex hover telegraph (0-20 ticks)
            if (ticksAlive < 20) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center, 10, 5.0);
                }
            }
            // Corkscrew spiral descent (20-120 ticks)
            else if (ticksAlive >= 20 && ticksAlive < 120) {
                if (!diveFired) {
                    diveFired = true;
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 1.0f);
                }
                int t = ticksAlive - 20;
                float progress = t / 100.0f;
                for (int i = 0; i < bodyHandles.size(); i++) {
                    float segProgress = Math.max(0, Math.min(1, progress - i * 0.04f));
                    double spiralAngle = segProgress * Math.PI * 6; // 3 full rotations
                    double spiralRadius = 4 * (1 - segProgress * 0.5);
                    double x = Math.cos(spiralAngle) * spiralRadius;
                    double z = Math.sin(spiralAngle) * spiralRadius;
                    double y = 28 * (1 - segProgress);
                    bodyHandles.get(i).entity().teleport(center.clone().add(x, y, z));
                }
                if (t % 4 == 0) {
                    double spiralAngle = progress * Math.PI * 6;
                    DisplayBuilder.cyanDust(
                        center.clone().add(Math.cos(spiralAngle) * 4, 28 * (1 - progress), Math.sin(spiralAngle) * 4),
                        5, 1.5);
                }
            }
            // Landing shockwave (tick 120)
            else if (ticksAlive == 120) {
                for (int i = 0; i < 20; i++) {
                    double a = (Math.PI * 2 / 20) * i;
                    DisplayBuilder.cyanDust(center.clone().add(Math.cos(a) * 6, 0.5, Math.sin(a) * 6), 4, 1.0);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpiralDive(plugin); }
    }

    // ================================================================
    // 4. LATERAL SWEEP — Blade-like horizontal pass at constant altitude
    // ================================================================
    public static class LateralSweep extends BossAttack {
        private final List<BlockDisplayHandle> bodyHandles = new ArrayList<>();
        private boolean sweepFired = false;
        private int sweepTick = 0;

        public LateralSweep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_lateral_sweep", AttackType.BOSS, 2), "dog");
            config.setDamage(12.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Rigid body at sweep altitude (12 blocks up)
            for (int i = 0; i < 18; i++) {
                Location loc = center.clone().add(-20 + i * 0.5, 12, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PRISMARINE);
                h.scale(0.8f, 0.5f, 0.8f).glow(0, 200, 255).interpolation(2, 0);
                bodyHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            // Horizontal plane telegraph at sweep altitude
            DisplayBuilder.cyanDust(center.clone().add(0, 12, 0), 30, 16.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph sparks at altitude (0-30 ticks)
            if (ticksAlive < 30 && !sweepFired) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 12, 0), 12, 14.0);
                }
            }
            // Sweep fires (tick 30)
            else if (ticksAlive == 30 && !sweepFired) {
                sweepFired = true;
                sweepTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 2.0f, 1.5f);
            }
            // Sweep across island (30-80 ticks = 2.5 seconds)
            else if (sweepFired && sweepTick < 50) {
                sweepTick++;
                float sweepZ = -20 + (sweepTick / 50.0f) * 40;
                for (int i = 0; i < bodyHandles.size(); i++) {
                    bodyHandles.get(i).entity().teleport(
                        center.clone().add(-20 + i * 0.5, 12, sweepZ));
                }
                if (sweepTick % 3 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 12, sweepZ), 6, 2.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LateralSweep(plugin); }
    }

    // ================================================================
    // 5. GROUND SKIM — Low-altitude high-speed surface pass
    // ================================================================
    public static class GroundSkim extends BossAttack {
        private final List<BlockDisplayHandle> bodyHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> wakeHandles = new ArrayList<>();
        private boolean skimFired = false;
        private int skimTick = 0;

        public GroundSkim(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_ground_skim", AttackType.BOSS, 2), "dog");
            config.setDamage(10.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Low body 1-2 blocks above ground
            for (int i = 0; i < 14; i++) {
                Location loc = center.clone().add(-18 + i * 1.3, 1.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.8f, 0.6f, 0.8f).glow(0, 200, 255).interpolation(2, 0);
                bodyHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            // Ground-level rumbling telegraph
            DisplayBuilder.cyanDust(center.clone().add(0, 0.3, 0), 15, 12.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Ground rumble telegraph (0-30 ticks)
            if (ticksAlive < 30 && !skimFired) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.3, 0), 8, 10.0);
                }
            }
            // Skim fires (tick 30)
            else if (ticksAlive == 30 && !skimFired) {
                skimFired = true;
                skimTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 1.0f);
            }
            // Skim across ground with slight curve (30-90 ticks)
            else if (skimFired && skimTick < 60) {
                skimTick++;
                float progress = skimTick / 60.0f;
                float sweepX = -18 + progress * 36;
                float curveZ = (float) Math.sin(progress * Math.PI) * 6;
                for (int i = 0; i < bodyHandles.size(); i++) {
                    float segProgress = Math.max(0, progress - i * 0.02f);
                    float segX = -18 + segProgress * 36;
                    float segZ = (float) Math.sin(segProgress * Math.PI) * 6;
                    float yOscillation = (float) Math.sin(segProgress * Math.PI * 8) * 0.4f;
                    bodyHandles.get(i).entity().teleport(
                        center.clone().add(segX, 1.5 + yOscillation, segZ));
                }
                // Churning wake particles
                if (skimTick % 3 == 0) {
                    Location wakeLoc = center.clone().add(sweepX - 4, 0.3, curveZ);
                    BlockDisplayHandle wake = displayBuilder.spawnBlock(wakeLoc, Material.POLISHED_BLACKSTONE);
                    wake.scale(0.4f, 0.15f, 0.4f).glow(0, 200, 255).interpolation(2, 0);
                    wakeHandles.add(wake);
                    spawnedEntities.add(wake.entity());
                    DisplayBuilder.cyanDust(wakeLoc, 4, 1.5);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GroundSkim(plugin); }
    }

    // ================================================================
    // 6. TIGHT CIRCLE CRUSH — Coiling orbit that tightens around a point
    // ================================================================
    public static class TightCircleCrush extends BossAttack {
        private final List<BlockDisplayHandle> bodyHandles = new ArrayList<>();
        private float currentRadius = 16.0f;
        private int loopCount = 0;

        public TightCircleCrush(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_tight_circle_crush", AttackType.BOSS, 2), "dog");
            config.setDamage(12.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(500);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Initial wide orbit segments
            for (int i = 0; i < 12; i++) {
                double a = (Math.PI * 2 / 12) * i;
                Location loc = center.clone().add(Math.cos(a) * 16, 5, Math.sin(a) * 16);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(1.0f, 0.8f, 1.0f).glow(0, 200, 255).interpolation(2, 0);
                bodyHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 12, 16.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.5f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Orbit tightens over time — radius decreases by 3 blocks per loop
            float angularSpeed = 0.08f;
            float angle = ticksAlive * angularSpeed;

            // Tighten every full rotation (~78 ticks per loop)
            int currentLoop = (int) (angle / (Math.PI * 2));
            if (currentLoop > loopCount && currentLoop <= 4) {
                loopCount = currentLoop;
                currentRadius = Math.max(4.0f, 16.0f - loopCount * 3.0f);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.8f + loopCount * 0.2f);
            }

            // Animate orbiting segments
            for (int i = 0; i < bodyHandles.size(); i++) {
                double segAngle = angle + (Math.PI * 2 / bodyHandles.size()) * i;
                bodyHandles.get(i).entity().teleport(
                    center.clone().add(Math.cos(segAngle) * currentRadius, 5, Math.sin(segAngle) * currentRadius));
            }

            if (ticksAlive % 6 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 5, currentRadius);
            }

            // Final crush pulse when radius reaches minimum
            if (currentRadius <= 4.0f && ticksAlive % 40 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 20, 5.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.2f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TightCircleCrush(plugin); }
    }

    // ================================================================
    // 7. S-CURVE WHIP — Violent S-curve snap with tail whip
    // ================================================================
    public static class SCurveWhip extends BossAttack {
        private final List<BlockDisplayHandle> bodyHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> whipHandles = new ArrayList<>();
        private boolean snapped = false;

        public SCurveWhip(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_s_curve_whip", AttackType.BOSS, 2), "dog");
            config.setDamage(8.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Body forming S-curve shape
            for (int i = 0; i < 16; i++) {
                float t = i / 15.0f;
                double x = (t - 0.5) * 24;
                double z = Math.sin(t * Math.PI * 2) * 6;
                Location loc = center.clone().add(x, 5, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.9f, 0.7f, 0.9f).glow(0, 200, 255).interpolation(3, 0);
                bodyHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            // Tail end-rod trails
            DisplayBuilder.cyanDust(center.clone().add(12, 5, 0), 10, 3.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // S-curve forming and tensing (0-30 ticks)
            if (ticksAlive < 30 && !snapped) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(12, 5, 0), 6, 2.0);
                }
                // Exaggerate the S-curve
                float tension = ticksAlive / 30.0f;
                for (int i = 0; i < bodyHandles.size(); i++) {
                    float t = i / 15.0f;
                    double x = (t - 0.5) * 24;
                    double z = Math.sin(t * Math.PI * 2) * (6 + tension * 4);
                    bodyHandles.get(i).entity().teleport(center.clone().add(x, 5, z));
                }
            }
            // SNAP — S straightens violently, tail whips outward (tick 30)
            else if (ticksAlive == 30 && !snapped) {
                snapped = true;
                // Straighten body
                for (int i = 0; i < bodyHandles.size(); i++) {
                    float t = i / 15.0f;
                    double x = (t - 0.5) * 24;
                    bodyHandles.get(i).entity().teleport(center.clone().add(x, 5, 0));
                }
                // Tail whip arc — 10-block fan of crystal shards
                for (int a = -5; a <= 5; a++) {
                    double whipAngle = Math.toRadians(a * 9);
                    for (int r = 6; r <= 10; r++) {
                        Location whipLoc = center.clone().add(
                            12 + Math.cos(whipAngle) * r, 5, Math.sin(whipAngle) * r);
                        BlockDisplayHandle wh = displayBuilder.spawnBlock(whipLoc, Material.AMETHYST_CLUSTER);
                        wh.scale(0.5f, 0.5f, 0.5f).glow(128, 0, 255).interpolation(1, 0);
                        whipHandles.add(wh);
                        spawnedEntities.add(wh.entity());
                    }
                }
                DisplayBuilder.cyanDust(center.clone().add(12, 5, 0), 25, 10.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.5f);
            }
            // Whip dissipates (30-60 ticks)
            else if (ticksAlive > 40 && ticksAlive <= 60) {
                for (BlockDisplayHandle wh : whipHandles) {
                    Location loc = wh.entity().getLocation();
                    wh.entity().teleport(loc.clone().add(0, 0.15, 0));
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SCurveWhip(plugin); }
    }

    // ================================================================
    // 8. ROLLING SPIRAL CHARGE — Corkscrew roll while charging forward
    // ================================================================
    public static class RollingSpiralCharge extends BossAttack {
        private final List<BlockDisplayHandle> bodyHandles = new ArrayList<>();
        private boolean chargeFired = false;
        private int chargeTick = 0;

        public RollingSpiralCharge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_rolling_spiral_charge", AttackType.BOSS, 2), "dog");
            config.setDamage(14.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Body at starting position with spinning indicator
            for (int i = 0; i < 14; i++) {
                Location loc = center.clone().add(-18, 6 + i * 0.3, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                h.scale(0.9f, 0.7f, 0.9f).glow(0, 200, 255).interpolation(2, 0);
                bodyHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.cyanDust(center.clone().add(-18, 6, 0), 15, 3.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Spin-up telegraph with color cycling (0-20 ticks)
            if (ticksAlive < 20 && !chargeFired) {
                float spinAngle = ticksAlive * 0.8f;
                for (int i = 0; i < bodyHandles.size(); i++) {
                    double yOff = Math.sin(spinAngle + i * 0.5) * 1.5;
                    double zOff = Math.cos(spinAngle + i * 0.5) * 1.5;
                    bodyHandles.get(i).entity().teleport(
                        center.clone().add(-18, 6 + yOff, zOff));
                }
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(-18, 6, 0), 8, 2.5);
                }
            }
            // Charge fires (tick 20)
            else if (ticksAlive == 20 && !chargeFired) {
                chargeFired = true;
                chargeTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 1.5f);
            }
            // Drilling spiral charge across arena (20-56 ticks = 1.8 seconds)
            else if (chargeFired && chargeTick < 36) {
                chargeTick++;
                float progress = chargeTick / 36.0f;
                for (int i = 0; i < bodyHandles.size(); i++) {
                    float segProgress = Math.max(0, progress - i * 0.02f);
                    float sweepX = -18 + segProgress * 36;
                    double rollAngle = segProgress * Math.PI * 12; // Many rotations
                    double yOff = Math.sin(rollAngle + i * 0.5) * 2;
                    double zOff = Math.cos(rollAngle + i * 0.5) * 2;
                    bodyHandles.get(i).entity().teleport(
                        center.clone().add(sweepX, 6 + yOff, zOff));
                }
                if (chargeTick % 3 == 0) {
                    float sweepX = -18 + progress * 36;
                    DisplayBuilder.cyanDust(center.clone().add(sweepX, 6, 0), 6, 3.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RollingSpiralCharge(plugin); }
    }

    // ================================================================
    // 9. DIVE BOMB — Near-vertical plunge from maximum height
    // ================================================================
    public static class DiveBomb extends BossAttack {
        private final List<BlockDisplayHandle> bodyHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> impactHandles = new ArrayList<>();
        private boolean diveStarted = false;
        private boolean impacted = false;

        public DiveBomb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_dive_bomb", AttackType.BOSS, 2), "dog");
            config.setDamage(16.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // DoG at maximum height, head pointing down
            for (int i = 0; i < 10; i++) {
                Location loc = center.clone().add(0, 35 - i * 0.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                float scale = 1.2f - i * 0.06f;
                h.scale(scale, 0.8f, scale).glow(0, 200, 255).interpolation(2, 0);
                bodyHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            // Crosshair on ground marking target
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2 / 8) * i;
                Location crossLoc = center.clone().add(Math.cos(a) * 3, 0.1, Math.sin(a) * 3);
                BlockDisplayHandle ch = displayBuilder.spawnBlock(crossLoc, Material.END_ROD);
                ch.scale(0.3f, 0.1f, 0.3f).glow(0, 200, 255).interpolation(3, 0);
                impactHandles.add(ch);
                spawnedEntities.add(ch.entity());
            }
            DisplayBuilder.cyanDust(center, 15, 3.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Descending pillar telegraph and crosshair pulse (0-30 ticks)
            if (ticksAlive < 30 && !diveStarted) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 20, 0), 8, 1.0);
                    DisplayBuilder.cyanDust(center, 5, 3.0);
                }
            }
            // Dive starts (tick 30)
            else if (ticksAlive == 30 && !diveStarted) {
                diveStarted = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 1.8f);
            }
            // Near-vertical dive (30-55 ticks)
            else if (diveStarted && !impacted && ticksAlive < 55) {
                int t = ticksAlive - 30;
                float progress = t / 25.0f;
                for (int i = 0; i < bodyHandles.size(); i++) {
                    float segProgress = Math.max(0, Math.min(1, progress - i * 0.04f));
                    double y = 35 * (1 - segProgress);
                    bodyHandles.get(i).entity().teleport(center.clone().add(0, y, 0));
                }
                if (t % 3 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 35 * (1 - progress), 0), 6, 1.5);
                }
            }
            // Impact (tick 55)
            else if (ticksAlive == 55 && !impacted) {
                impacted = true;
                // Remove crosshair
                for (BlockDisplayHandle ch : impactHandles) ch.entity().remove();
                impactHandles.clear();
                // Ground shockwave ring
                for (int i = 0; i < 24; i++) {
                    double a = (Math.PI * 2 / 24) * i;
                    for (int r = 2; r <= 5; r++) {
                        Location shockLoc = center.clone().add(Math.cos(a) * r, 0.3, Math.sin(a) * r);
                        BlockDisplayHandle sh = displayBuilder.spawnBlock(shockLoc, Material.POLISHED_BLACKSTONE);
                        sh.scale(0.6f, 0.3f, 0.6f).glow(0, 200, 255).interpolation(2, 0);
                        impactHandles.add(sh);
                        spawnedEntities.add(sh.entity());
                    }
                }
                DisplayBuilder.cyanDust(center, 40, 6.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DiveBomb(plugin); }
    }

    // ================================================================
    // 10. ZIGZAG RUN — Erratic angular zigzag across arena
    // ================================================================
    public static class ZigzagRun extends BossAttack {
        private final List<BlockDisplayHandle> bodyHandles = new ArrayList<>();
        private int zigCount = 0;
        private int lastZigTick = 0;
        private double currentX = -16;
        private double currentZ = 0;
        private double dirX = 1;
        private double dirZ = 1;

        public ZigzagRun(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_zigzag_run", AttackType.BOSS, 2), "dog");
            config.setDamage(12.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Stiff jerky body at starting edge
            for (int i = 0; i < 10; i++) {
                Location loc = center.clone().add(-16, 5, i * 0.5 - 2.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.8f, 0.6f, 0.8f).glow(0, 200, 255).interpolation(1, 0);
                bodyHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.cyanDust(center.clone().add(-16, 5, 0), 10, 3.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.5f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Jerky telegraph (0-20 ticks)
            if (ticksAlive < 20) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(
                        center.clone().add(currentX, 5, currentZ), 4, 1.0);
                }
                return;
            }

            // Zigzag movement: direction changes every 16 ticks (~0.8 seconds)
            if (ticksAlive - lastZigTick >= 16 && zigCount < 8) {
                lastZigTick = ticksAlive;
                zigCount++;
                // 90-degree direction snap
                dirZ = -dirZ;
                if (zigCount % 2 == 0) dirX = -dirX;
                DisplayBuilder.playSound(center.clone().add(currentX, 5, currentZ),
                    Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.5f);
            }

            // Move body along current direction
            currentX += dirX * 0.6;
            currentZ += dirZ * 0.6;

            // Clamp to arena bounds
            currentX = Math.max(-18, Math.min(18, currentX));
            currentZ = Math.max(-18, Math.min(18, currentZ));

            for (int i = 0; i < bodyHandles.size(); i++) {
                // Segments trail behind with slight delay
                double lagX = currentX - dirX * i * 0.4;
                double lagZ = currentZ - dirZ * i * 0.4;
                bodyHandles.get(i).entity().teleport(
                    center.clone().add(lagX, 5, lagZ));
            }

            if (ticksAlive % 4 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(currentX, 5, currentZ), 4, 1.5);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ZigzagRun(plugin); }
    }
}
