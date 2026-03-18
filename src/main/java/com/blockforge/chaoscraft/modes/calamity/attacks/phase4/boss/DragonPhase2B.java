package com.blockforge.chaoscraft.modes.calamity.attacks.phase4.boss;

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
 * Phase 4D Boss — THE VOID EMPEROR (Ender Dragon)
 * Phase 2B: Attacks #56-68
 * Mid Phase 2: Escalating absorbed-boss mechanics.
 * Mini Boss Summon Gates (#60, #68) activate Dragon immunity.
 * Phase 2 damage range: 14-24 HP.
 * NO status effects — damage only.
 */
public final class DragonPhase2B {

    private DragonPhase2B() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidMaelstrom(plugin));
        registry.register(new CrystallineCagePrison(plugin));
        registry.register(new BrimstoneGeyserGrid(plugin));
        registry.register(new WingShockwaveDouble(plugin));
        registry.register(new FirstSummonCall(plugin));
        registry.register(new ResonantScream(plugin));
        registry.register(new VoidAnchor(plugin));
        registry.register(new PrismBurst(plugin));
        registry.register(new ConsumedVoidMemoryDrain(plugin));
        registry.register(new ShatteredWingBlade(plugin));
        registry.register(new BrimstoneEruptionCircle(plugin));
        registry.register(new VoidStorm(plugin));
        registry.register(new SecondSummonFracture(plugin));
    }

    // ================================================================
    // #56 — VOID MAELSTROM — Pulling vortex at arena center
    // ================================================================
    public static class VoidMaelstrom extends BossAttack {
        private final List<BlockDisplayHandle> vortexHandles = new ArrayList<>();
        private boolean vortexActive = false;
        private int vortexTick = 0;

        public VoidMaelstrom(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_void_maelstrom", AttackType.BOSS, 4), "dragon");
            config.setDamage(18.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Growing spiral at arena center
            BlockDisplayHandle spiralCore = displayBuilder.spawnBlock(
                center.clone().add(0, 1, 0), Material.CRYING_OBSIDIAN);
            spiralCore.scale(3.0f, 15.0f, 3.0f).glow(100, 0, 200).interpolation(4, 0);
            vortexHandles.add(spiralCore);
            spawnedEntities.add(spiralCore.entity());
            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 20 && !vortexActive) {
                float grow = 3 + ticksAlive * 0.25f;
                vortexHandles.get(0).entity().setTransformation(new Transformation(
                    new Vector3f(-grow / 2, 0, -grow / 2),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(grow, 15.0f, grow),
                    new AxisAngle4f(0, 0, 1, 0)));
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 10, grow);
                }
            }
            else if (ticksAlive == 20 && !vortexActive) {
                vortexActive = true;
                vortexTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.2f, 0.6f);
            }
            // Vortex pulls (20-120 ticks = 5 seconds)
            else if (vortexActive && ticksAlive - vortexTick < 100) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 7, 0), 8, 8.0);
                    triggerImpactDamage(center);
                }
            }
            // Vortex dissipation shockwave (tick 120)
            else if (vortexActive && ticksAlive - vortexTick == 100) {
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 30, 10.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidMaelstrom(plugin); }
    }

    // ================================================================
    // #57 — CRYSTALLINE CAGE PRISON — Faster DoG cage, spike ceiling
    // ================================================================
    public static class CrystallineCagePrison extends BossAttack {
        private final List<BlockDisplayHandle> prisonHandles = new ArrayList<>();
        private boolean prisonActive = false;
        private int prisonTick = 0;

        public CrystallineCagePrison(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_crystalline_cage_prison", AttackType.BOSS, 4), "dragon");
            config.setDamage(20.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(350);
            config.setCooldownTicks(840);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Spinning target lock on highest-damage player
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                Location lockPt = center.clone().add(Math.cos(angle) * 2, 1, Math.sin(angle) * 2);
                BlockDisplayHandle lockRing = displayBuilder.spawnBlock(lockPt, Material.AMETHYST_BLOCK);
                lockRing.scale(0.3f, 0.1f, 0.3f).glow(150, 240, 255).interpolation(2, 0);
                prisonHandles.add(lockRing);
                spawnedEntities.add(lockRing.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 20 && !prisonActive) {
                for (int i = 0; i < prisonHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 8) * i + ticksAlive * 0.3;
                    prisonHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * 2, 1, Math.sin(angle) * 2));
                }
            }
            else if (ticksAlive == 20 && !prisonActive) {
                prisonActive = true;
                prisonTick = ticksAlive;
                // Build crystal cage — 6 walls + spiked ceiling
                for (int w = 0; w < 6; w++) {
                    double angle = (Math.PI * 2 / 6) * w;
                    for (int y = 0; y < 5; y++) {
                        Location wallLoc = center.clone().add(Math.cos(angle) * 3, y, Math.sin(angle) * 3);
                        BlockDisplayHandle wall = displayBuilder.spawnBlock(wallLoc, Material.AMETHYST_BLOCK);
                        wall.scale(1.5f, 1.0f, 0.4f).glow(150, 240, 255).interpolation(1, 0);
                        spawnedEntities.add(wall.entity());
                    }
                }
                BlockDisplayHandle ceiling = displayBuilder.spawnBlock(
                    center.clone().add(0, 5, 0), Material.AMETHYST_BLOCK);
                ceiling.scale(6.0f, 0.5f, 6.0f).glow(150, 240, 255).interpolation(2, 0);
                spawnedEntities.add(ceiling.entity());
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.9f, 0.7f);
            }
            // Prison active — beam charges (20-140 ticks = 6 seconds)
            else if (prisonActive && ticksAlive - prisonTick < 120) {
                if (ticksAlive % 20 == 0) {
                    triggerImpactDamage(center);
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 8, 3.0);
                }
                if (ticksAlive % 40 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 1.0f);
                }
            }
            // Prison shatters — outward shards (tick 140)
            else if (prisonActive && ticksAlive - prisonTick == 120) {
                triggerImpactDamage(center);
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 / 8) * i;
                    Location shardLoc = center.clone().add(Math.cos(angle) * 8, 2, Math.sin(angle) * 8);
                    BlockDisplayHandle shard = displayBuilder.spawnBlock(shardLoc, Material.AMETHYST_BLOCK);
                    shard.scale(0.3f, 0.3f, 0.8f).glow(150, 240, 255).interpolation(1, 0);
                    spawnedEntities.add(shard.entity());
                    DisplayBuilder.cyanDust(shardLoc, 5, 1.0);
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystallineCagePrison(plugin); }
    }

    // ================================================================
    // #58 — BRIMSTONE GEYSER GRID — 20 geysers in 5 waves of 4
    // ================================================================
    public static class BrimstoneGeyserGrid extends BossAttack {
        private final List<BlockDisplayHandle> geyserHandles = new ArrayList<>();
        private int currentWave = 0;

        public BrimstoneGeyserGrid(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_brimstone_geyser_grid", AttackType.BOSS, 4), "dragon");
            config.setDamage(22.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(640);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Full arena floor flash
            BlockDisplayHandle flash = displayBuilder.spawnBlock(
                center.clone().add(0, 0.02, 0), Material.MAGMA_BLOCK);
            flash.scale(30.0f, 0.05f, 30.0f).glow(255, 80, 0).interpolation(2, 0);
            geyserHandles.add(flash);
            spawnedEntities.add(flash.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // 5 waves: ticks 20, 44, 68, 92, 116 (24 ticks apart)
            int waveTime = ticksAlive - 20;
            if (ticksAlive >= 20 && waveTime >= 0 && waveTime % 24 == 0 && currentWave < 5) {
                currentWave++;
                for (int i = 0; i < 4; i++) {
                    double ox = (Math.random() - 0.5) * 28;
                    double oz = (Math.random() - 0.5) * 28;
                    Location geyserLoc = center.clone().add(ox, 0.1, oz);
                    // Warning marker
                    BlockDisplayHandle warning = displayBuilder.spawnBlock(geyserLoc, Material.MAGMA_BLOCK);
                    warning.scale(1.0f, 0.1f, 1.0f).glow(255, 100, 0).interpolation(1, 0);
                    geyserHandles.add(warning);
                    spawnedEntities.add(warning.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 0.8f, 1.0f);
            }

            // Each wave erupts 10 ticks after placement
            if (ticksAlive >= 30 && (ticksAlive - 30) % 24 == 0 && (ticksAlive - 30) / 24 < 5) {
                int waveIdx = (ticksAlive - 30) / 24;
                int handleStart = 1 + waveIdx * 4;
                for (int i = handleStart; i < handleStart + 4 && i < geyserHandles.size(); i++) {
                    Location geyserLoc = geyserHandles.get(i).entity().getLocation();
                    for (int y = 0; y < 6; y++) {
                        BlockDisplayHandle col = displayBuilder.spawnBlock(
                            geyserLoc.clone().add(0, y, 0), Material.MAGMA_BLOCK);
                        col.scale(0.5f, 1.0f, 0.5f).glow(255, 60, 0).interpolation(1, 0);
                        spawnedEntities.add(col.entity());
                    }
                    triggerImpactDamage(geyserLoc);
                    DisplayBuilder.cyanDust(geyserLoc, 10, 1.0);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.9f, 0.6f);
            }

            // Aftermath burn patches ambient
            if (ticksAlive > 140 && ticksAlive % 20 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 0), 4, 14.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneGeyserGrid(plugin); }
    }

    // ================================================================
    // #59 — WING SHOCKWAVE DOUBLE — Two sequential ground slams
    // ================================================================
    public static class WingShockwaveDouble extends BossAttack {
        private final List<BlockDisplayHandle> slamHandles = new ArrayList<>();
        private int slamCount = 0;

        public WingShockwaveDouble(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_wing_shockwave_double", AttackType.BOSS, 4), "dragon");
            config.setDamage(18.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // First slam at tick 20, second at tick 44
            if ((ticksAlive == 20 || ticksAlive == 44) && slamCount < 2) {
                slamCount++;
                double ox = (Math.random() - 0.5) * 16;
                double oz = (Math.random() - 0.5) * 16;
                Location slamLoc = center.clone().add(ox, 0.1, oz);
                // Impact crater display
                BlockDisplayHandle crater = displayBuilder.spawnBlock(slamLoc, Material.CRYING_OBSIDIAN);
                crater.scale(3.0f, 0.2f, 3.0f).glow(180, 60, 0).interpolation(2, 0);
                slamHandles.add(crater);
                spawnedEntities.add(crater.entity());
                // Shockwave ring segments
                for (int i = 0; i < 12; i++) {
                    double angle = (Math.PI * 2 / 12) * i;
                    Location ringPt = slamLoc.clone().add(Math.cos(angle) * 10, 0.2, Math.sin(angle) * 10);
                    BlockDisplayHandle ring = displayBuilder.spawnBlock(ringPt, Material.CRYING_OBSIDIAN);
                    ring.scale(1.0f, 0.15f, 1.0f).glow(200, 100, 0).interpolation(1, 0);
                    spawnedEntities.add(ring.entity());
                }
                triggerImpactDamage(slamLoc);
                DisplayBuilder.cyanDust(slamLoc, 20, 6.0);
                DisplayBuilder.playSound(slamLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
                if (slamCount == 2) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.9f);
                }
            }
            // Crater pulsing aftermath (44-204 ticks = 8 seconds)
            if (ticksAlive > 44 && ticksAlive < 204 && ticksAlive % 15 == 0) {
                for (BlockDisplayHandle crater : slamHandles) {
                    DisplayBuilder.cyanDust(crater.entity().getLocation(), 4, 3.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WingShockwaveDouble(plugin); }
    }

    // ================================================================
    // #60 — FIRST SUMMON: THE VOID EMPEROR CALLS — Mini Boss 1 gate
    // ================================================================
    public static class FirstSummonCall extends BossAttack {
        private final List<BlockDisplayHandle> summonHandles = new ArrayList<>();
        private boolean ritualComplete = false;

        public FirstSummonCall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_first_summon_call", AttackType.BOSS, 4), "dragon");
            config.setDamage(20.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(0);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Three ritual locations in equilateral triangle
            double[] angles = {0, Math.PI * 2 / 3, Math.PI * 4 / 3};
            Material[] mats = {Material.CRYING_OBSIDIAN, Material.MAGMA_BLOCK, Material.AMETHYST_BLOCK};
            int[][] colors = {{100, 0, 200}, {255, 80, 0}, {0, 200, 255}};
            for (int i = 0; i < 3; i++) {
                Location locPt = center.clone().add(Math.cos(angles[i]) * 12, 0.05, Math.sin(angles[i]) * 12);
                BlockDisplayHandle ritualRing = displayBuilder.spawnBlock(locPt, mats[i]);
                ritualRing.scale(2.0f, 0.1f, 2.0f).glow(colors[i][0], colors[i][1], colors[i][2]).interpolation(3, 0);
                summonHandles.add(ritualRing);
                spawnedEntities.add(ritualRing.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_DEATH, 0.5f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Extended telegraph (0-60 ticks = 3 seconds) — sequential location pulses
            if (ticksAlive < 60 && !ritualComplete) {
                int pulseIdx = ticksAlive / 20;
                if (ticksAlive % 20 == 0 && pulseIdx < summonHandles.size()) {
                    DisplayBuilder.cyanDust(summonHandles.get(pulseIdx).entity().getLocation(), 10, 2.0);
                }
            }
            // Triple beams fire from Dragon to ritual locations (tick 60)
            else if (ticksAlive == 60 && !ritualComplete) {
                for (BlockDisplayHandle ring : summonHandles) {
                    Location ritualLoc = ring.entity().getLocation();
                    // Beam column from Y+25 to ground
                    BlockDisplayHandle beam = displayBuilder.spawnBlock(
                        ritualLoc.clone().add(0, 12, 0), Material.PURPLE_STAINED_GLASS);
                    beam.scale(2.0f, 25.0f, 2.0f).glow(140, 0, 220).interpolation(2, 0);
                    spawnedEntities.add(beam.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.4f);
            }
            // Eruption pillars at all 3 locations (tick 100)
            else if (ticksAlive == 100 && !ritualComplete) {
                ritualComplete = true;
                for (BlockDisplayHandle ring : summonHandles) {
                    Location eruptLoc = ring.entity().getLocation();
                    triggerImpactDamage(eruptLoc);
                    // Pillar eruption
                    for (int y = 0; y < 10; y++) {
                        BlockDisplayHandle pillar = displayBuilder.spawnBlock(
                            eruptLoc.clone().add(0, y, 0), Material.PURPLE_STAINED_GLASS);
                        pillar.scale(5.0f, 1.0f, 5.0f).glow(160, 0, 200).interpolation(1, 0);
                        spawnedEntities.add(pillar.entity());
                    }
                    DisplayBuilder.cyanDust(eruptLoc, 25, 5.0);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 0.8f, 1.2f);
            }
            // Mini Boss 1 spawns — Dragon immune (aftermath terrain hazards)
            if (ticksAlive > 100 && ticksAlive % 20 == 0) {
                for (BlockDisplayHandle ring : summonHandles) {
                    DisplayBuilder.cyanDust(ring.entity().getLocation(), 4, 4.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FirstSummonCall(plugin); }
    }

    // ================================================================
    // #61 — RESONANT SCREAM — Sonic shockwave rings from Dragon's maw
    // ================================================================
    public static class ResonantScream extends BossAttack {
        private final List<BlockDisplayHandle> screamHandles = new ArrayList<>();
        private boolean screamActive = false;
        private int screamTick = 0;

        public ResonantScream(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_resonant_scream", AttackType.BOSS, 4), "dragon");
            config.setDamage(15.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            BlockDisplayHandle mouthVortex = displayBuilder.spawnBlock(
                center.clone().add(0, 12, 0), Material.PURPLE_STAINED_GLASS);
            mouthVortex.scale(1.5f, 1.5f, 1.5f).glow(160, 0, 80).interpolation(3, 0);
            screamHandles.add(mouthVortex);
            spawnedEntities.add(mouthVortex.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.5f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 20 && !screamActive) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 12, 0), 8, 2.0);
                }
            }
            else if (ticksAlive == 20 && !screamActive) {
                screamActive = true;
                screamTick = ticksAlive;
            }
            // 3 rings expand: tick 20, 40, 60 (one every 20 ticks)
            else if (screamActive) {
                int elapsed = ticksAlive - screamTick;
                if (elapsed == 0 || elapsed == 20 || elapsed == 40) {
                    // Create ring segments
                    float baseRadius = 2;
                    for (int i = 0; i < 16; i++) {
                        double angle = (Math.PI * 2 / 16) * i;
                        Location ringPt = center.clone().add(Math.cos(angle) * baseRadius, 10, Math.sin(angle) * baseRadius);
                        BlockDisplayHandle ring = displayBuilder.spawnBlock(ringPt, Material.PURPLE_STAINED_GLASS);
                        ring.scale(1.5f, 4.0f, 0.5f).glow(160, 0, 80).interpolation(2, 0);
                        screamHandles.add(ring);
                        spawnedEntities.add(ring.entity());
                    }
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);
                }
                // Expand all ring segments outward
                if (elapsed < 60 && elapsed % 4 == 0) {
                    float expandDist = elapsed * 1.5f;
                    int ringIdx = 1;
                    for (int r = 0; r < 3 && r * 20 <= elapsed; r++) {
                        float ringDist = (elapsed - r * 20) * 1.5f;
                        if (ringDist > 0) {
                            for (int i = 0; i < 16; i++) {
                                if (ringIdx < screamHandles.size()) {
                                    double angle = (Math.PI * 2 / 16) * i;
                                    screamHandles.get(ringIdx).entity().teleport(
                                        center.clone().add(Math.cos(angle) * (2 + ringDist), 10, Math.sin(angle) * (2 + ringDist)));
                                    ringIdx++;
                                }
                            }
                        }
                    }
                    triggerImpactDamage(center);
                }
                // Ring hits — sonic impact on proximity (tick 80)
                if (elapsed == 60) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 0.8f);
                }
            }
            // Aftermath — drifting particles (80-240 ticks = 8 seconds)
            if (ticksAlive > 80 && ticksAlive < 240 && ticksAlive % 15 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 6, 0), 4, 12.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ResonantScream(plugin); }
    }

    // ================================================================
    // #62 — VOID ANCHOR — Slow tracking orb that immobilizes on hit
    // ================================================================
    public static class VoidAnchor extends BossAttack {
        private final List<BlockDisplayHandle> anchorHandles = new ArrayList<>();
        private boolean orbLaunched = false;
        private int launchTick = 0;

        public VoidAnchor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_void_anchor", AttackType.BOSS, 4), "dragon");
            config.setDamage(24.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(480);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            BlockDisplayHandle orbCore = displayBuilder.spawnBlock(
                center.clone().add(0, 10, 0), Material.CRYING_OBSIDIAN);
            orbCore.scale(2.0f, 2.0f, 2.0f).glow(90, 0, 160).interpolation(3, 0);
            anchorHandles.add(orbCore);
            spawnedEntities.add(orbCore.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 20 && !orbLaunched) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 6, 2.0);
                }
            }
            else if (ticksAlive == 20 && !orbLaunched) {
                orbLaunched = true;
                launchTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.7f);
            }
            // Orb travels slowly toward target (20-90 ticks)
            else if (orbLaunched && ticksAlive - launchTick < 70) {
                float travel = (ticksAlive - launchTick) * 0.3f;
                anchorHandles.get(0).entity().teleport(
                    center.clone().add(travel, 10 - travel * 0.1, 0));
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(anchorHandles.get(0).entity().getLocation(), 4, 2.0);
                }
            }
            // Anchor impact — freeze + Dragon dive (tick 90)
            else if (orbLaunched && ticksAlive - launchTick == 70) {
                Location impactLoc = anchorHandles.get(0).entity().getLocation();
                triggerImpactDamage(impactLoc);
                // Anchor cage visual
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 / 8) * i;
                    Location cagePt = impactLoc.clone().add(Math.cos(angle) * 2, 1, Math.sin(angle) * 2);
                    BlockDisplayHandle cageBar = displayBuilder.spawnBlock(cagePt, Material.CRYING_OBSIDIAN);
                    cageBar.scale(0.2f, 2.0f, 0.2f).glow(90, 0, 160).interpolation(2, 0);
                    spawnedEntities.add(cageBar.entity());
                }
                DisplayBuilder.cyanDust(impactLoc, 15, 2.0);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 1.0f);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_HURT, 0.5f, 0.6f);
            }
            // Void patch aftermath (90-210 ticks = 6 seconds)
            else if (orbLaunched && ticksAlive - launchTick < 190) {
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.cyanDust(anchorHandles.get(0).entity().getLocation(), 3, 2.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidAnchor(plugin); }
    }

    // ================================================================
    // #63 — PRISM BURST — 24 crystal shards omnidirectional explosion
    // ================================================================
    public static class PrismBurst extends BossAttack {
        private final List<BlockDisplayHandle> shardHandles = new ArrayList<>();
        private boolean burstFired = false;
        private int fireTick = 0;

        public PrismBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_prism_burst", AttackType.BOSS, 4), "dragon");
            config.setDamage(14.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(250);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Spine telegraph ripple (0-20 ticks)
            if (ticksAlive < 20 && !burstFired) {
                if (ticksAlive % 7 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 10, 3.0);
                }
            }
            // Burst fires (tick 20) — 24 shards in 3 layers
            else if (ticksAlive == 20 && !burstFired) {
                burstFired = true;
                fireTick = ticksAlive;
                for (int layer = 0; layer < 3; layer++) {
                    float yAngle = layer == 0 ? 0 : (layer == 1 ? 0.52f : -0.52f);
                    for (int i = 0; i < 8; i++) {
                        double angle = (Math.PI * 2 / 8) * i;
                        Location shardLoc = center.clone().add(
                            Math.cos(angle) * 2, 10 + Math.sin(yAngle) * 2, Math.sin(angle) * 2);
                        BlockDisplayHandle shard = displayBuilder.spawnBlock(shardLoc, Material.AMETHYST_BLOCK);
                        shard.scale(0.2f, 0.2f, 0.6f).glow(200, 255, 255).interpolation(1, 0);
                        shardHandles.add(shard);
                        spawnedEntities.add(shard.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ARROW_SHOOT, 0.6f, 1.5f);
            }
            // Shards travel outward (20-50 ticks)
            else if (burstFired && ticksAlive - fireTick < 30) {
                float dist = (ticksAlive - fireTick) * 0.6f;
                int idx = 0;
                for (int layer = 0; layer < 3; layer++) {
                    float yAngle = layer == 0 ? 0 : (layer == 1 ? 0.52f : -0.52f);
                    for (int i = 0; i < 8; i++) {
                        if (idx < shardHandles.size()) {
                            double angle = (Math.PI * 2 / 8) * i;
                            shardHandles.get(idx).entity().teleport(
                                center.clone().add(
                                    Math.cos(angle) * (2 + dist), 10 + Math.sin(yAngle) * (2 + dist * 0.5), Math.sin(angle) * (2 + dist)));
                            idx++;
                        }
                    }
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 6, dist);
                }
            }
            // Shards dissolve — wisps linger (tick 50)
            else if (burstFired && ticksAlive - fireTick == 30) {
                for (BlockDisplayHandle shard : shardHandles) {
                    DisplayBuilder.cyanDust(shard.entity().getLocation(), 3, 1.0);
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PrismBurst(plugin); }
    }

    // ================================================================
    // #64 — CONSUMED VOID: MEMORY DRAIN — HP leech beams to all players
    // ================================================================
    public static class ConsumedVoidMemoryDrain extends BossAttack {
        private final List<BlockDisplayHandle> drainHandles = new ArrayList<>();
        private boolean drainActive = false;
        private int drainTick = 0;

        public ConsumedVoidMemoryDrain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_consumed_void_memory_drain", AttackType.BOSS, 4), "dragon");
            config.setDamage(16.0);
            config.setDamageRadius(20.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Thread tendrils extending from Dragon's chest
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 / 6) * i;
                Location threadLoc = center.clone().add(Math.cos(angle) * 2, 10, Math.sin(angle) * 2);
                BlockDisplayHandle thread = displayBuilder.spawnBlock(threadLoc, Material.PURPLE_STAINED_GLASS);
                thread.scale(0.15f, 0.15f, 2.0f).glow(100, 0, 180).interpolation(2, 0);
                drainHandles.add(thread);
                spawnedEntities.add(thread.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_AMBIENT, 0.6f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 20 && !drainActive) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 6, 3.0);
                }
            }
            else if (ticksAlive == 20 && !drainActive) {
                drainActive = true;
                drainTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 0.6f);
            }
            // Drain active — beams to all players (20-120 ticks = 5 seconds)
            else if (drainActive && ticksAlive - drainTick < 100) {
                if (ticksAlive % 10 == 0) {
                    // Extend drain thread visuals outward
                    for (int i = 0; i < drainHandles.size(); i++) {
                        double angle = (Math.PI * 2 / 6) * i;
                        float dist = 2 + ((ticksAlive - drainTick) / 100.0f) * 18;
                        drainHandles.get(i).entity().teleport(
                            center.clone().add(Math.cos(angle) * dist, 10 - dist * 0.3, Math.sin(angle) * dist));
                    }
                    DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 6, 5.0);
                    triggerImpactDamage(center);
                }
                // Dragon "brightens" — golden shimmer
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.6f, 0.7f);
                }
            }
            // Drain exhale shockwave (tick 120)
            else if (drainActive && ticksAlive - drainTick == 100) {
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 8, 0), 25, 12.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ConsumedVoidMemoryDrain(plugin); }
    }

    // ================================================================
    // #65 — SHATTERED WING BLADE — Wing strike + crystal fragment spray
    // ================================================================
    public static class ShatteredWingBlade extends BossAttack {
        private final List<BlockDisplayHandle> wingHandles = new ArrayList<>();
        private boolean wingStruck = false;
        private int strikeTick = 0;

        public ShatteredWingBlade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_shattered_wing_blade", AttackType.BOSS, 4), "dragon");
            config.setDamage(20.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(520);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Wing fracture lines telegraph
            BlockDisplayHandle wing = displayBuilder.spawnBlock(
                center.clone().add(5, 8, 0), Material.AMETHYST_BLOCK);
            wing.scale(8.0f, 0.5f, 3.0f).glow(180, 230, 255).interpolation(3, 0);
            wingHandles.add(wing);
            spawnedEntities.add(wing.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_REPAIR, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 20 && !wingStruck) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(5, 8, 0), 6, 4.0);
                }
            }
            // Wing sweeps 180-degree arc (tick 20)
            else if (ticksAlive == 20 && !wingStruck) {
                wingStruck = true;
                strikeTick = ticksAlive;
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 4, 0), 20, 10.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.3f, 0.7f);
            }
            // Wing shatters — 8 fragments spray (tick 24)
            else if (wingStruck && ticksAlive - strikeTick == 4) {
                for (int i = 0; i < 8; i++) {
                    double angle = Math.toRadians(-22.5 + i * 5.6);
                    Location fragLoc = center.clone().add(Math.cos(angle) * 5, 6, Math.sin(angle) * 5);
                    BlockDisplayHandle frag = displayBuilder.spawnBlock(fragLoc, Material.AMETHYST_BLOCK);
                    frag.scale(0.3f, 0.3f, 0.6f).glow(180, 230, 255).interpolation(1, 0);
                    wingHandles.add(frag);
                    spawnedEntities.add(frag.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.8f);
            }
            // Fragments fly outward (24-44 ticks)
            else if (wingStruck && ticksAlive - strikeTick > 4 && ticksAlive - strikeTick < 24) {
                float dist = (ticksAlive - strikeTick - 4) * 0.7f;
                for (int i = 1; i < wingHandles.size(); i++) {
                    double angle = Math.toRadians(-22.5 + (i - 1) * 5.6);
                    wingHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * (5 + dist), 6 - dist * 0.2, Math.sin(angle) * (5 + dist)));
                }
            }
            // Fragment impacts — crater patches (tick 44)
            else if (wingStruck && ticksAlive - strikeTick == 24) {
                for (int i = 1; i < wingHandles.size(); i++) {
                    Location impactLoc = wingHandles.get(i).entity().getLocation();
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.cyanDust(impactLoc, 5, 1.0);
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.7f, 1.1f);
            }
            // Wing reconstruction visual (44-104 ticks = 3 seconds)
            else if (wingStruck && ticksAlive - strikeTick < 84) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(5, 8, 0), 4, 3.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShatteredWingBlade(plugin); }
    }

    // ================================================================
    // #66 — BRIMSTONE ERUPTION CIRCLE — Shrinking fire ring
    // ================================================================
    public static class BrimstoneEruptionCircle extends BossAttack {
        private final List<BlockDisplayHandle> ringHandles = new ArrayList<>();
        private int currentRing = 0;

        public BrimstoneEruptionCircle(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_brimstone_eruption_circle", AttackType.BOSS, 4), "dragon");
            config.setDamage(20.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Outermost ring telegraph at 18-block radius
            for (int i = 0; i < 30; i++) {
                double angle = (Math.PI * 2 / 30) * i;
                Location ringPt = center.clone().add(Math.cos(angle) * 18, 0.05, Math.sin(angle) * 18);
                BlockDisplayHandle marker = displayBuilder.spawnBlock(ringPt, Material.MAGMA_BLOCK);
                marker.scale(0.8f, 0.1f, 0.8f).glow(255, 100, 0).interpolation(2, 0);
                ringHandles.add(marker);
                spawnedEntities.add(marker.entity());
            }
            DisplayBuilder.playSound(center, Sound.AMBIENT_BASALT_DELTAS_MOOD, 0.9f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // 3 shrinking rings: tick 20 (R=18), tick 50 (R=14), tick 80 (R=10)
            double[] radii = {18, 14, 10};
            int[] counts = {30, 24, 18};
            int[] triggers = {20, 50, 80};

            for (int r = 0; r < 3; r++) {
                if (ticksAlive == triggers[r] && currentRing <= r) {
                    currentRing = r + 1;
                    for (int i = 0; i < counts[r]; i++) {
                        double angle = (Math.PI * 2 / counts[r]) * i;
                        Location geyserLoc = center.clone().add(Math.cos(angle) * radii[r], 0.1, Math.sin(angle) * radii[r]);
                        for (int y = 0; y < 5; y++) {
                            BlockDisplayHandle geyser = displayBuilder.spawnBlock(
                                geyserLoc.clone().add(0, y, 0), Material.MAGMA_BLOCK);
                            geyser.scale(0.5f, 1.0f, 0.5f).glow(255, 60, 0).interpolation(1, 0);
                            spawnedEntities.add(geyser.entity());
                        }
                        if (i % 5 == 0) triggerImpactDamage(geyserLoc);
                    }
                    DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 0.7f, 0.9f);
                }
            }

            // Aftermath — 3-ring ground hazard
            if (ticksAlive > 80 && ticksAlive < 280 && ticksAlive % 20 == 0) {
                for (double r : radii) {
                    DisplayBuilder.cyanDust(center.clone().add(r * 0.5, 0.2, 0), 3, r);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneEruptionCircle(plugin); }
    }

    // ================================================================
    // #67 — VOID STORM — 5-second void bolt downpour from sky
    // ================================================================
    public static class VoidStorm extends BossAttack {
        private final List<BlockDisplayHandle> boltHandles = new ArrayList<>();
        private boolean stormActive = false;
        private int stormTick = 0;

        public VoidStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_void_storm", AttackType.BOSS, 4), "dragon");
            config.setDamage(14.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(350);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 20 && !stormActive) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 30, 0), 12, 15.0);
                }
            }
            else if (ticksAlive == 20 && !stormActive) {
                stormActive = true;
                stormTick = ticksAlive;
            }
            // Void bolt rain (20-120 ticks = 5 seconds, 4 per second = every 5 ticks)
            else if (stormActive && ticksAlive - stormTick < 100) {
                if (ticksAlive % 5 == 0) {
                    double ox = (Math.random() - 0.5) * 28;
                    double oz = (Math.random() - 0.5) * 28;
                    Location boltLoc = center.clone().add(ox, 0.1, oz);
                    BlockDisplayHandle bolt = displayBuilder.spawnBlock(boltLoc, Material.PURPLE_STAINED_GLASS);
                    bolt.scale(0.8f, 0.3f, 0.8f).glow(100, 0, 200).interpolation(1, 0);
                    boltHandles.add(bolt);
                    spawnedEntities.add(bolt.entity());
                    triggerImpactDamage(boltLoc);
                    DisplayBuilder.cyanDust(boltLoc, 4, 1.5);
                    DisplayBuilder.playSound(boltLoc, Sound.ENTITY_ARROW_HIT, 0.4f, 0.8f);
                }
            }
            // Void patch aftermath (120-280 ticks = 8 seconds)
            else if (stormActive && ticksAlive - stormTick < 260) {
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 0), 6, 14.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidStorm(plugin); }
    }

    // ================================================================
    // #68 — SECOND SUMMON: THE EMPEROR FRACTURES — Dual mini boss gate
    // ================================================================
    public static class SecondSummonFracture extends BossAttack {
        private final List<BlockDisplayHandle> fractureHandles = new ArrayList<>();
        private boolean impacted = false;
        private boolean riftsOpened = false;

        public SecondSummonFracture(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_second_summon_fracture", AttackType.BOSS, 4), "dragon");
            config.setDamage(24.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(0);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 1.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_ROAR, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Dragon free-falls (0-60 ticks = 3 seconds telegraph)
            if (ticksAlive < 60 && !impacted) {
                float fallY = 25 - ticksAlive * 0.4f;
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, fallY, 0), 10, 3.0);
                }
            }
            // Ground impact — catastrophic crater (tick 60)
            else if (ticksAlive == 60 && !impacted) {
                impacted = true;
                // Crater explosion — all essence particles
                BlockDisplayHandle crater = displayBuilder.spawnBlock(
                    center.clone().add(0, 0.1, 0), Material.CRYING_OBSIDIAN);
                crater.scale(15.0f, 0.5f, 15.0f).glow(100, 0, 160).interpolation(3, 0);
                fractureHandles.add(crater);
                spawnedEntities.add(crater.entity());
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 40, 15.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.4f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
            }
            // Dragon convulses — flickers between essences (60-90 ticks)
            else if (impacted && ticksAlive < 90 && !riftsOpened) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 10, 4.0);
                }
            }
            // Rifts open — 2 mini bosses emerge (tick 90)
            else if (ticksAlive == 90 && !riftsOpened) {
                riftsOpened = true;
                for (int i = 0; i < 2; i++) {
                    double angle = i * Math.PI;
                    Location riftLoc = center.clone().add(Math.cos(angle) * 5, 0.1, Math.sin(angle) * 5);
                    for (int y = 0; y < 8; y++) {
                        BlockDisplayHandle rift = displayBuilder.spawnBlock(
                            riftLoc.clone().add(0, y, 0), Material.PURPLE_STAINED_GLASS);
                        rift.scale(3.0f, 1.0f, 0.5f).glow(120, 0, 200).interpolation(2, 0);
                        fractureHandles.add(rift);
                        spawnedEntities.add(rift.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.8f, 0.8f);
            }
            // Dragon rises immune — crater hazard zone active
            if (ticksAlive > 90 && ticksAlive % 20 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 0.5, 0), 6, 8.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SecondSummonFracture(plugin); }
    }
}
