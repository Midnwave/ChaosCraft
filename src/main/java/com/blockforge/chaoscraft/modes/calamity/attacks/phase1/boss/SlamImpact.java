package com.blockforge.chaoscraft.modes.calamity.attacks.phase1.boss;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

/**
 * GROUP 1: SLAM / IMPACT
 * Physical impacts, body slams, and crushing force attacks.
 * Attacks 1-10 from the Voidmaw boss design document.
 */
public final class SlamImpact {

    private SlamImpact() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new UndertowSlam(plugin));
        registry.register(new MawBreach(plugin));
        registry.register(new SpineCrash(plugin));
        registry.register(new DeadWeight(plugin));
        registry.register(new TailWhip(plugin));
        registry.register(new GroundPoundEcho(plugin));
        registry.register(new CrushColumn(plugin));
        registry.register(new SeismicRoar(plugin));
        registry.register(new FissureErupt(plugin));
        registry.register(new IslandEdgeSlam(plugin));
    }

    // -------------------------------------------------------------------------
    // 1. Undertow Slam
    // A massive tentacle-like appendage erupts from the void below and crashes
    // down onto the End stone surface. Large impact area, heavy knockback.
    // -------------------------------------------------------------------------
    public static class UndertowSlam extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean impactFired = false;

        public UndertowSlam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("undertow_slam", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(5.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(360);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(2.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Telegraph: particle plumes rising from below
            DisplayBuilder.purpleDust(center.clone().add(0, 0.5, 0), 20, 3.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 0.4f);

            // Main tentacle appendage — starts high, crashes down
            Location spawnHigh = center.clone().add(0, 22, 0);
            BlockDisplayHandle arm = displayBuilder.spawnBlock(spawnHigh, Material.OBSIDIAN);
            arm.scale(2.5f, 5f, 2.5f).glow(128, 0, 255).interpolation(2, 0);
            handles.add(arm);
            spawnedEntities.add(arm.entity());

            // Secondary appendage segments for visual bulk
            for (int i = 0; i < 3; i++) {
                double angle = Math.toRadians(i * 120);
                Location segLoc = spawnHigh.clone().add(
                    Math.cos(angle) * 1.5, -i * 2.5, Math.sin(angle) * 1.5
                );
                BlockDisplayHandle seg = displayBuilder.spawnBlock(segLoc, Material.CRYING_OBSIDIAN);
                seg.scale(1.5f, 3f, 1.5f).glow(80, 0, 160).interpolation(2, 0);
                handles.add(seg);
                spawnedEntities.add(seg.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (handles.isEmpty()) return;

            // Phase 1 (0-25): descend from high (Y+22) toward surface
            // Phase 2 (26-30): impact flash
            // Phase 3 (31-60): withdraw back down

            BlockDisplay main = (BlockDisplay) handles.get(0).entity();

            if (ticksAlive <= 25) {
                float t = ticksAlive / 25f;
                float yOffset = 22f * (1f - t);
                Location target = center.clone().add(0, yOffset, 0);
                main.teleport(target);

                // Animate spin as it falls
                main.setTransformation(new Transformation(
                    new Vector3f(-1.25f, -2.5f, -1.25f),
                    new AxisAngle4f(ticksAlive * 0.08f, 0, 1, 0),
                    new Vector3f(2.5f, 5f, 2.5f),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                main.setInterpolationDelay(0);
                main.setInterpolationDuration(2);

                // Move segments trailing behind
                for (int i = 1; i < handles.size(); i++) {
                    double angle = Math.toRadians(i * 120 + ticksAlive * 5);
                    Location segTarget = center.clone().add(
                        Math.cos(angle) * 1.5, yOffset + i * 2.5, Math.sin(angle) * 1.5
                    );
                    handles.get(i).entity().teleport(segTarget);
                }
            } else if (ticksAlive == 26 && !impactFired) {
                impactFired = true;
                DisplayBuilder.purpleDust(center, 40, 5.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 0.3f);
                triggerImpactDamage(center);
            } else if (ticksAlive > 30) {
                // Withdraw downward
                float t = (ticksAlive - 30) / 30f;
                float yOffset = -(15f * t);
                Location withdrawLoc = center.clone().add(0, yOffset, 0);
                main.teleport(withdrawLoc);
            }

            // Ambient particle trail during fall
            if (ticksAlive % 3 == 0 && ticksAlive < 26) {
                DisplayBuilder.purpleDust(center.clone().add(0, 5, 0), 8, 2.0);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new UndertowSlam(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 2. Maw Breach
    // The island surface erupts upward as the Voidmaw's crown of teeth
    // briefly surfaces, launching End stone chunks outward radially.
    // -------------------------------------------------------------------------
    public static class MawBreach extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean breachFired = false;

        public MawBreach(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("maw_breach", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(5.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(440);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(3.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Telegraph: ground shake particles
            DisplayBuilder.purpleDust(center, 25, 4.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.5f);

            // Bulge effect — blocks rising from below
            for (int i = 0; i < 7; i++) {
                double angle = Math.toRadians(i * (360.0 / 7));
                Location ringLoc = center.clone().add(
                    Math.cos(angle) * 3.0, -1.0, Math.sin(angle) * 3.0
                );
                BlockDisplayHandle chunk = displayBuilder.spawnBlock(ringLoc, Material.END_STONE);
                chunk.scale(1.2f, 1.2f, 1.2f).glow(80, 0, 160).interpolation(2, 0);
                handles.add(chunk);
                spawnedEntities.add(chunk.entity());
            }

            // Central crown piece
            BlockDisplayHandle crown = displayBuilder.spawnBlock(center.clone().add(0, -2, 0), Material.OBSIDIAN);
            crown.scale(3f, 2f, 3f).glow(128, 0, 255).interpolation(2, 0);
            handles.add(crown);
            spawnedEntities.add(crown.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            if (ticksAlive <= 20) {
                // Rise telegraph: blocks rise upward slowly
                float t = ticksAlive / 20f;
                for (int i = 0; i < handles.size() - 1; i++) {
                    double angle = Math.toRadians(i * (360.0 / 7));
                    Location ringTarget = center.clone().add(
                        Math.cos(angle) * 3.0, -1.0 + t * 2.0, Math.sin(angle) * 3.0
                    );
                    handles.get(i).entity().teleport(ringTarget);
                }
                // Crown rises
                BlockDisplay crown = (BlockDisplay) handles.get(handles.size() - 1).entity();
                crown.teleport(center.clone().add(0, -2.0 + t * 3.0, 0));

                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 0.5, 0), 10, 3.5);
                }
            } else if (ticksAlive == 21 && !breachFired) {
                breachFired = true;
                // Eruption: launch chunks outward
                DisplayBuilder.purpleDust(center.clone().add(0, 1, 0), 50, 5.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.6f);
                triggerImpactDamage(center);

                // Scatter chunks in radial burst
                for (int i = 0; i < handles.size() - 1; i++) {
                    double angle = Math.toRadians(i * (360.0 / 7));
                    Location scatter = center.clone().add(
                        Math.cos(angle) * (8 + Math.random() * 10),
                        1.5,
                        Math.sin(angle) * (8 + Math.random() * 10)
                    );
                    handles.get(i).entity().teleport(scatter);
                }
            } else if (ticksAlive > 21 && ticksAlive <= 50) {
                // Crown sinks back down
                float t = (ticksAlive - 21) / 29f;
                BlockDisplay crown = (BlockDisplay) handles.get(handles.size() - 1).entity();
                crown.teleport(center.clone().add(0, 1.0 - t * 4.0, 0));
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new MawBreach(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 3. Spine Crash
    // A row of enormous black dorsal spines erupts sequentially across the
    // island in a wave, each slamming down 0.5 seconds apart.
    // -------------------------------------------------------------------------
    public static class SpineCrash extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final int[] impactTicks = {10, 20, 30, 40, 50, 60, 70, 80};
        private final boolean[] fired = new boolean[8];

        public SpineCrash(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spine_crash", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(7.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(500);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(7.0);
            config.setImpactRadius(2.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 0.7f);

            // Telegraph: particle line traces the path
            for (int i = 0; i < 8; i++) {
                Location spineBase = center.clone().add(-10 + i * 2.8, 0.5, 0);
                DisplayBuilder.purpleDust(spineBase, 5, 1.0);
            }

            // Spawn 8 spines above the surface, ready to crash
            for (int i = 0; i < 8; i++) {
                Location spineHigh = center.clone().add(-10 + i * 2.8, 15, 0);
                BlockDisplayHandle spine = displayBuilder.spawnBlock(spineHigh, Material.OBSIDIAN);
                spine.scale(1.0f, 4.0f, 1.0f).glow(80, 0, 160).interpolation(2, 0);
                handles.add(spine);
                spawnedEntities.add(spine.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.size() < 8) return;

            for (int i = 0; i < 8; i++) {
                if (!fired[i] && ticksAlive >= impactTicks[i]) {
                    fired[i] = true;
                    Location impactLoc = center.clone().add(-10 + i * 2.8, 0, 0);
                    // Crash spine to surface
                    BlockDisplay spine = (BlockDisplay) handles.get(i).entity();
                    spine.teleport(impactLoc.clone().add(0, 0.5, 0));
                    spine.setTransformation(new Transformation(
                        new Vector3f(-0.5f, 0f, -0.5f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(1.0f, 4.0f, 1.0f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    spine.setInterpolationDelay(0);
                    spine.setInterpolationDuration(2);

                    DisplayBuilder.purpleDust(impactLoc.clone().add(0, 1, 0), 15, 2.0);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_STONE_PLACE, 0.8f, 0.5f);
                    triggerImpactDamage(impactLoc);
                } else if (!fired[i]) {
                    // Animate fall approach
                    float progress = ticksAlive / (float) impactTicks[i];
                    float yPos = 15f * (1f - progress);
                    Location fallingLoc = center.clone().add(-10 + i * 2.8, yPos, 0);
                    handles.get(i).entity().teleport(fallingLoc);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new SpineCrash(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 4. Dead Weight
    // Full-island shockwave. The Voidmaw presses against the underside then
    // drops, sending ripples across the entire surface simultaneously.
    // -------------------------------------------------------------------------
    public static class DeadWeight extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean impactFired = false;

        public DeadWeight(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dead_weight", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(9.0);
            config.setDamageRadius(30.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Silence precedes the boom — minimal telegraph
            // Spawn ripple rings at center, expanding outward
            for (int ring = 0; ring < 5; ring++) {
                for (int i = 0; i < 12; i++) {
                    double angle = Math.toRadians(i * 30);
                    double radius = (ring + 1) * 4.0;
                    Location ringLoc = center.clone().add(
                        Math.cos(angle) * radius, -0.3, Math.sin(angle) * radius
                    );
                    BlockDisplayHandle ripple = displayBuilder.spawnBlock(ringLoc, Material.PURPLE_STAINED_GLASS);
                    ripple.scale(0.6f, 0.15f, 0.6f).glow(128, 0, 255).interpolation(2, 0);
                    handles.add(ripple);
                    spawnedEntities.add(ripple.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Phase 1 (0-40): silent buildup — island edge pulses
            if (ticksAlive < 40) {
                if (ticksAlive % 8 == 0) {
                    // Edge glow pulses from below
                    for (int i = 0; i < 4; i++) {
                        double angle = Math.toRadians(i * 90);
                        Location edgeLoc = center.clone().add(Math.cos(angle) * 20, 0, Math.sin(angle) * 20);
                        DisplayBuilder.purpleDust(edgeLoc, 12, 3.0);
                    }
                }
                return;
            }

            // Phase 2 (40): BOOM — impact
            if (ticksAlive == 40 && !impactFired) {
                impactFired = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.3f);
                DisplayBuilder.purpleDust(center, 60, 8.0);
                triggerImpactDamage(center);
            }

            // Phase 3 (40-80): ripple rings expand outward
            if (ticksAlive >= 40 && !handles.isEmpty()) {
                int ringTick = ticksAlive - 40;
                float expansionFactor = ringTick / 40f;

                for (int ring = 0; ring < 5; ring++) {
                    float ringDelay = ring * 0.15f;
                    float t = Math.max(0, expansionFactor - ringDelay);
                    float alpha = 1.0f - t;
                    if (alpha < 0) alpha = 0;

                    for (int i = 0; i < 12; i++) {
                        int handleIdx = ring * 12 + i;
                        if (handleIdx >= handles.size()) break;
                        double angle = Math.toRadians(i * 30);
                        double radius = (ring + 1) * 4.0 + t * 12.0;
                        Location newLoc = center.clone().add(
                            Math.cos(angle) * radius, -0.3 + t * 0.5, Math.sin(angle) * radius
                        );
                        handles.get(handleIdx).entity().teleport(newLoc);

                        BlockDisplay bd = (BlockDisplay) handles.get(handleIdx).entity();
                        float scaleY = 0.15f * alpha;
                        if (scaleY < 0.01f) scaleY = 0.01f;
                        bd.setTransformation(new Transformation(
                            new Vector3f(-0.3f, 0f, -0.3f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.6f, scaleY, 0.6f),
                            new AxisAngle4f(0, 0, 1, 0)
                        ));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(2);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new DeadWeight(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 5. Tail Whip
    // A massive void-black tail sweeps horizontally across the island at
    // ground level — one continuous horizontal sweep.
    // -------------------------------------------------------------------------
    public static class TailWhip extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean sweepActive = false;

        public TailWhip(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tail_whip", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(11.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(560);
            config.setDamageOnImpactOnly(false);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Telegraph: void particles stream from one side
            Location startEdge = center.clone().add(-25, 1.5, 0);
            DisplayBuilder.purpleDust(startEdge, 30, 4.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 1.2f);

            // Tail segments — horizontal bar that sweeps across
            for (int i = 0; i < 8; i++) {
                Location segLoc = startEdge.clone().add(i * 1.5, 0, 0);
                BlockDisplayHandle seg = displayBuilder.spawnBlock(segLoc, Material.OBSIDIAN);
                seg.scale(1.5f, 2.0f, 1.5f).glow(128, 0, 255).interpolation(2, 0);
                handles.add(seg);
                spawnedEntities.add(seg.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            // Phase 1 (0-20): telegraph wind-up
            if (ticksAlive < 20) {
                if (ticksAlive % 5 == 0) {
                    Location startEdge = center.clone().add(-25, 1.5, 0);
                    DisplayBuilder.purpleDust(startEdge, 12, 2.5);
                }
                return;
            }

            // Phase 2 (20-60): tail sweeps across at high speed
            sweepActive = true;
            float sweepProgress = (ticksAlive - 20) / 40f;
            float xOffset = -25f + sweepProgress * 50f; // sweeps -25 to +25

            for (int i = 0; i < handles.size(); i++) {
                Location tailLoc = center.clone().add(xOffset + i * 1.5, 1.0, 0);
                handles.get(i).entity().teleport(tailLoc);

                BlockDisplay bd = (BlockDisplay) handles.get(i).entity();
                bd.setTransformation(new Transformation(
                    new Vector3f(-0.75f, -1.0f, -0.75f),
                    new AxisAngle4f(ticksAlive * 0.1f, 1, 0, 0),
                    new Vector3f(1.5f, 2.0f, 1.5f),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(2);
            }

            // Damage zone along sweep line
            if (sweepActive && ticksAlive % 10 == 0 && ticksAlive <= 60) {
                Location sweepLoc = center.clone().add(xOffset, 1.0, 0);
                triggerImpactDamage(sweepLoc);
                DisplayBuilder.purpleDust(sweepLoc, 8, 2.0);
            }

            // Phase 3 (60+): tail exits and particles fade
            if (ticksAlive > 60) {
                DisplayBuilder.cyanDust(center, 5, 1.0);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new TailWhip(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 6. Ground Pound Echo
    // Three sequential shockwaves emanate from the island center outward.
    // Each wave is stronger than the last.
    // -------------------------------------------------------------------------
    public static class GroundPoundEcho extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final boolean[] waveFired = {false, false, false};

        public GroundPoundEcho(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ground_pound_echo", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(4.0);
            config.setDamageRadius(20.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 0.5f);
            DisplayBuilder.purpleDust(center, 15, 2.0);

            // Three wave rings, initially at center
            for (int wave = 0; wave < 3; wave++) {
                for (int i = 0; i < 16; i++) {
                    double angle = Math.toRadians(i * 22.5);
                    Location ringLoc = center.clone().add(Math.cos(angle), -0.2, Math.sin(angle));
                    BlockDisplayHandle node = displayBuilder.spawnBlock(ringLoc, Material.PURPLE_STAINED_GLASS);
                    node.scale(0.5f, 0.2f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                    handles.add(node);
                    spawnedEntities.add(node.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.size() < 48) return;

            // Wave 1 fires at tick 20, Wave 2 at tick 40, Wave 3 at tick 60
            int[] waveTriggerTicks = {20, 40, 60};
            double[] waveDamage = {4.0, 5.0, 7.0};

            for (int wave = 0; wave < 3; wave++) {
                int trigger = waveTriggerTicks[wave];
                if (!waveFired[wave] && ticksAlive >= trigger) {
                    waveFired[wave] = true;
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.8f, 0.6f + wave * 0.1f);
                    triggerImpactDamage(center);
                }

                if (waveFired[wave]) {
                    int waveAge = ticksAlive - waveTriggerTicks[wave];
                    float radius = 1.0f + waveAge * 0.6f;
                    float alpha = 1.0f - waveAge / 40f;
                    if (alpha < 0) alpha = 0;

                    for (int i = 0; i < 16; i++) {
                        int handleIdx = wave * 16 + i;
                        if (handleIdx >= handles.size()) break;
                        double angle = Math.toRadians(i * 22.5);
                        Location ringLoc = center.clone().add(
                            Math.cos(angle) * radius, -0.2, Math.sin(angle) * radius
                        );
                        handles.get(handleIdx).entity().teleport(ringLoc);

                        BlockDisplay bd = (BlockDisplay) handles.get(handleIdx).entity();
                        float scaleVal = 0.5f + wave * 0.3f;
                        float scaleY = 0.2f * (1f + alpha);
                        bd.setTransformation(new Transformation(
                            new Vector3f(-scaleVal * 0.5f, 0f, -scaleVal * 0.5f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(scaleVal, scaleY, scaleVal),
                            new AxisAngle4f(0, 0, 1, 0)
                        ));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(2);
                    }

                    if (ticksAlive % 6 == 0 && alpha > 0.3f) {
                        double angle = Math.toRadians(ticksAlive * 40);
                        Location sparkLoc = center.clone().add(
                            Math.cos(angle) * radius, 0.3, Math.sin(angle) * radius
                        );
                        DisplayBuilder.purpleDust(sparkLoc, 6, 1.5);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new GroundPoundEcho(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 7. Crush Column
    // A colossal cylindrical appendage descends from overhead onto a targeted
    // point — a growing shadow telegraphs the exact impact location.
    // -------------------------------------------------------------------------
    public static class CrushColumn extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean impactFired = false;

        public CrushColumn(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crush_column", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(640);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(14.0);
            config.setImpactRadius(2.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Shadow growing on ground — circular, darkening
            for (int ring = 0; ring < 3; ring++) {
                for (int i = 0; i < 20; i++) {
                    double angle = Math.toRadians(i * 18);
                    double r = (ring + 1) * 0.8;
                    Location shadowLoc = center.clone().add(Math.cos(angle) * r, -0.05, Math.sin(angle) * r);
                    BlockDisplayHandle shadow = displayBuilder.spawnBlock(shadowLoc, Material.BLACK_CONCRETE);
                    shadow.scale(0.3f, 0.05f, 0.3f).glow(80, 0, 160).interpolation(4, 0);
                    handles.add(shadow);
                    spawnedEntities.add(shadow.entity());
                }
            }

            // The column itself — high overhead
            BlockDisplayHandle column = displayBuilder.spawnBlock(center.clone().add(0, 30, 0), Material.OBSIDIAN);
            column.scale(2.5f, 8f, 2.5f).glow(128, 0, 255).interpolation(2, 0);
            handles.add(column);
            spawnedEntities.add(column.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            int columnIdx = handles.size() - 1;

            // Phase 1 (0-60): shadow grows, column descends slowly
            if (ticksAlive <= 60) {
                float progress = ticksAlive / 60f;

                // Shadow expands and darkens
                for (int ring = 0; ring < 3; ring++) {
                    float ringScale = 0.3f + progress * (1.5f + ring * 0.5f);
                    for (int i = 0; i < 20; i++) {
                        int idx = ring * 20 + i;
                        if (idx >= columnIdx) break;
                        double angle = Math.toRadians(i * 18);
                        double r = (ring + 1) * 0.8 * (1 + progress * 0.5);
                        Location shadowLoc = center.clone().add(Math.cos(angle) * r, -0.05, Math.sin(angle) * r);
                        handles.get(idx).entity().teleport(shadowLoc);

                        BlockDisplay bd = (BlockDisplay) handles.get(idx).entity();
                        bd.setTransformation(new Transformation(
                            new Vector3f(-ringScale * 0.5f, 0f, -ringScale * 0.5f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(ringScale, 0.05f, ringScale),
                            new AxisAngle4f(0, 0, 1, 0)
                        ));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(4);
                    }
                }

                // Column falls slowly
                float yPos = 30f * (1f - progress);
                handles.get(columnIdx).entity().teleport(center.clone().add(0, yPos, 0));

                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, yPos + 2, 0), 10, 2.5);
                }
            }

            // Phase 2 (61): IMPACT
            else if (ticksAlive == 61 && !impactFired) {
                impactFired = true;
                handles.get(columnIdx).entity().teleport(center.clone().add(0, 0.5, 0));
                DisplayBuilder.purpleDust(center.clone().add(0, 1, 0), 50, 5.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 0.2f);
                triggerImpactDamage(center);
            }

            // Phase 3 (61-80): column retracts upward
            else if (ticksAlive > 61) {
                float t = (ticksAlive - 61) / 19f;
                handles.get(columnIdx).entity().teleport(center.clone().add(0, t * 20, 0));
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new CrushColumn(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 8. Seismic Roar
    // No physical impact — a soundwave from below. The island surface visibly
    // vibrates. Affects all players touching surfaces.
    // -------------------------------------------------------------------------
    public static class SeismicRoar extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean hit1Fired = false;
        private boolean hit2Fired = false;

        public SeismicRoar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("seismic_roar", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(5.0);
            config.setDamageRadius(25.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.4f);

            // Vibration visuals: small particles dancing off the surface
            for (int i = 0; i < 20; i++) {
                double angle = Math.toRadians(i * 18);
                double r = 3.0 + Math.random() * 15.0;
                Location vibrLoc = center.clone().add(Math.cos(angle) * r, 0.3, Math.sin(angle) * r);
                BlockDisplayHandle node = displayBuilder.spawnBlock(vibrLoc, Material.PURPLE_STAINED_GLASS);
                node.scale(0.2f, 0.2f, 0.2f).glow(128, 0, 255).interpolation(1, 0);
                handles.add(node);
                spawnedEntities.add(node.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Sub-bass buildup (0-20)
            if (ticksAlive < 20) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 0.3, 0), 8, 4.0);
                }
            }

            // Hit 1 at tick 20
            if (!hit1Fired && ticksAlive == 20) {
                hit1Fired = true;
                DisplayBuilder.purpleDust(center, 40, 6.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.5f);
                triggerImpactDamage(center);

                // Vibrate all nodes
                for (BlockDisplayHandle h : handles) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                        new Vector3f(-0.1f, 0f, -0.1f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.4f, 0.5f, 0.4f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(1);
                }
            }

            // Hit 2 at tick 30 (0.5 seconds later)
            if (!hit2Fired && ticksAlive == 30) {
                hit2Fired = true;
                DisplayBuilder.purpleDust(center, 35, 5.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.6f);
                triggerImpactDamage(center);
            }

            // Vibration decay
            if (ticksAlive > 30) {
                float decay = (ticksAlive - 30) / 30f;
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 0.3, 0), (int)(8 * (1f - decay)) + 1, 3.0);
                }
            }

            // Animate vibration shimmer
            if (ticksAlive % 2 == 0 && !handles.isEmpty()) {
                for (int i = 0; i < handles.size(); i++) {
                    BlockDisplay bd = (BlockDisplay) handles.get(i).entity();
                    float jitter = (float)(Math.sin(ticksAlive * 0.8 + i) * 0.15);
                    Location jitterLoc = center.clone().add(
                        (Math.cos(Math.toRadians(i * 18)) * (3.0 + (i % 5) * 3.0)) + jitter,
                        0.3 + jitter * 0.5,
                        (Math.sin(Math.toRadians(i * 18)) * (3.0 + (i % 5) * 3.0)) + jitter
                    );
                    bd.teleport(jitterLoc);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new SeismicRoar(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 9. Fissure Erupt
    // A jagged Z-pattern crack splits across the island. Void energy vents
    // upward in a sustained column for 3 seconds.
    // -------------------------------------------------------------------------
    public static class FissureErupt extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean ventActive = false;
        private int ventStartTick = 0;

        public FissureErupt(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fissure_erupt", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(540);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 0.5f);

            // Fissure crack forms in Z-pattern: 15 blocks long
            // Segment 1: center-6 to center
            // Segment 2: center to center+3 offset
            // Segment 3: center+3 to center+6
            double[][] fissurePoints = {
                {-7, 0, -3}, {-5, 0, -3}, {-3, 0, -3},
                {-1, 0, 0},  {1, 0, 0},   {3, 0, 0},
                {5, 0, 3},   {7, 0, 3}
            };

            for (double[] pt : fissurePoints) {
                Location crackLoc = center.clone().add(pt[0], -0.1, pt[2]);
                BlockDisplayHandle crack = displayBuilder.spawnBlock(crackLoc, Material.OBSIDIAN);
                crack.scale(1.0f, 0.1f, 1.0f).glow(128, 0, 255).interpolation(3, 0);
                handles.add(crack);
                spawnedEntities.add(crack.entity());
            }

            // Vent columns (invisible until activated) above each crack point
            for (double[] pt : fissurePoints) {
                for (int h = 0; h < 4; h++) {
                    Location ventLoc = center.clone().add(pt[0], h + 0.5, pt[2]);
                    BlockDisplayHandle vent = displayBuilder.spawnBlock(ventLoc, Material.PURPLE_STAINED_GLASS);
                    vent.scale(0.8f, 1.0f, 0.8f).glow(128, 0, 255).interpolation(2, 0);
                    handles.add(vent);
                    spawnedEntities.add(vent.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            int crackCount = 8;
            int ventLayers = 4;

            // Phase 1 (0-30): crack forms slowly — visible telegraph
            if (ticksAlive < 30) {
                float progress = ticksAlive / 30f;
                for (int i = 0; i < crackCount; i++) {
                    if (i >= handles.size()) break;
                    BlockDisplay bd = (BlockDisplay) handles.get(i).entity();
                    float scaleX = (i < (int)(progress * crackCount)) ? 1.0f : 0.01f;
                    bd.setTransformation(new Transformation(
                        new Vector3f(-scaleX * 0.5f, 0f, -0.5f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(scaleX, 0.1f, 1.0f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(3);
                }
                // Hide vents during telegraph
                for (int i = crackCount; i < handles.size(); i++) {
                    BlockDisplay bd = (BlockDisplay) handles.get(i).entity();
                    bd.setTransformation(new Transformation(
                        new Vector3f(-0.001f, 0, -0.001f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.001f, 0.001f, 0.001f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(2);
                }
                return;
            }

            // Phase 2 (30-90): vent active
            if (!ventActive) {
                ventActive = true;
                ventStartTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 1.5f);
                DisplayBuilder.purpleDust(center, 30, 4.0);
            }

            if (ventActive && ticksAlive <= 90) {
                // Show vents
                for (int ci = 0; ci < crackCount; ci++) {
                    for (int layer = 0; layer < ventLayers; layer++) {
                        int idx = crackCount + ci * ventLayers + layer;
                        if (idx >= handles.size()) break;
                        BlockDisplay bd = (BlockDisplay) handles.get(idx).entity();
                        float pulse = 0.6f + (float)Math.sin(ticksAlive * 0.3 + layer * 0.5) * 0.2f;
                        bd.setTransformation(new Transformation(
                            new Vector3f(-pulse * 0.5f, -0.5f, -pulse * 0.5f),
                            new AxisAngle4f(ticksAlive * 0.05f, 0, 1, 0),
                            new Vector3f(pulse, 1.0f, pulse),
                            new AxisAngle4f(0, 0, 1, 0)
                        ));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(2);
                    }
                }

                // Damage along crack line every 20 ticks
                if (ticksAlive % 20 == 0) {
                    double[][] fissurePoints = {
                        {-7, 0, -3}, {-5, 0, -3}, {-3, 0, -3},
                        {-1, 0, 0},  {1, 0, 0},   {3, 0, 0},
                        {5, 0, 3},   {7, 0, 3}
                    };
                    for (double[] pt : fissurePoints) {
                        Location ventLoc = center.clone().add(pt[0], 1, pt[2]);
                        triggerImpactDamage(ventLoc);
                        DisplayBuilder.purpleDust(ventLoc, 8, 1.5);
                    }
                }
            }

            // Phase 3 (90+): crack seals
            if (ticksAlive > 90) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center, 5, 2.0);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new FissureErupt(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 10. Island Edge Slam
    // The Voidmaw rises along the outer edge and slams against the island rim,
    // caving in a section of the perimeter.
    // -------------------------------------------------------------------------
    public static class IslandEdgeSlam extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean slamFired = false;

        public IslandEdgeSlam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("island_edge_slam", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(8.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(800);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Edge impact target — north edge
            Location edgeLoc = center.clone().add(0, 0, -20);

            // Telegraph: void churns at the edge
            DisplayBuilder.purpleDust(edgeLoc, 30, 5.0);
            DisplayBuilder.playSound(edgeLoc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.6f);

            // Void mass rising from below the edge — massive body press
            for (int i = 0; i < 12; i++) {
                double angle = Math.toRadians(i * 30);
                Location riseLoc = edgeLoc.clone().add(
                    Math.cos(angle) * 4.0, -3.0 + i * 0.3, Math.sin(angle) * 2.0
                );
                BlockDisplayHandle slab = displayBuilder.spawnBlock(riseLoc, Material.OBSIDIAN);
                slab.scale(2.0f, 1.5f, 1.0f).glow(80, 0, 160).interpolation(3, 0);
                handles.add(slab);
                spawnedEntities.add(slab.entity());
            }

            // Main impact block — representing the body slam
            BlockDisplayHandle body = displayBuilder.spawnBlock(edgeLoc.clone().add(0, -5, 0), Material.CRYING_OBSIDIAN);
            body.scale(6f, 4f, 3f).glow(128, 0, 255).interpolation(2, 0);
            handles.add(body);
            spawnedEntities.add(body.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            Location edgeLoc = center.clone().add(0, 0, -20);
            int bodyIdx = handles.size() - 1;

            // Phase 1 (0-50): mass rises toward surface from below
            if (ticksAlive <= 50) {
                float progress = ticksAlive / 50f;
                float yRise = -5f + progress * 7f;

                handles.get(bodyIdx).entity().teleport(edgeLoc.clone().add(0, yRise, 0));

                for (int i = 0; i < bodyIdx; i++) {
                    double angle = Math.toRadians(i * 30);
                    Location segLoc = edgeLoc.clone().add(
                        Math.cos(angle) * 4.0,
                        -3.0 + i * 0.3 + progress * 5.0,
                        Math.sin(angle) * 2.0
                    );
                    handles.get(i).entity().teleport(segLoc);
                }

                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.purpleDust(edgeLoc.clone().add(0, 1, 0), 15, 4.0);
                }
            }

            // Phase 2 (51): SLAM
            else if (ticksAlive == 51 && !slamFired) {
                slamFired = true;
                handles.get(bodyIdx).entity().teleport(edgeLoc.clone().add(0, 1.5, 0));
                DisplayBuilder.purpleDust(edgeLoc.clone().add(0, 2, 0), 60, 7.0);
                DisplayBuilder.playSound(edgeLoc, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.3f);
                triggerImpactDamage(edgeLoc);
            }

            // Phase 3 (51-100): mass retracts
            else if (ticksAlive > 51) {
                float t = (ticksAlive - 51) / 49f;
                float yDrop = 1.5f - t * 8f;
                handles.get(bodyIdx).entity().teleport(edgeLoc.clone().add(0, yDrop, 0));

                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(edgeLoc, 5, 2.0);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new IslandEdgeSlam(plugin);
        }
    }
}
