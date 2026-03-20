package com.blockforge.chaoscraft.modes.bluemoon.attacks.blockdisplay;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import java.util.*;

/**
 * Blue Moon Block Display — LUNAR ARCHITECTURE
 * 13 ancient moon ruins/structures themed around temples, gates, and monoliths.
 * Blue Moon particle palette: pale blue (180,210,255) / silver (200,200,220) / moonlight white (240,240,255)
 * Sounds: BLOCK_STONE_BREAK, BLOCK_AMETHYST_BLOCK_CHIME, BLOCK_DEEPSLATE_BREAK
 */
public final class LunarArchitecture {

    private LunarArchitecture() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new MoonGate(plugin));
        registry.register(new RuinedPillar(plugin));
        registry.register(new LunarAltar(plugin));
        registry.register(new CrumblingArch(plugin));
        registry.register(new ObeliskRow(plugin));
        registry.register(new MoonDial(plugin));
        registry.register(new TempleSteps(plugin));
        registry.register(new SeleniteTower(plugin));
        registry.register(new AncientWall(plugin));
        registry.register(new LunarFountain(plugin));
        registry.register(new RunestoneCircle(plugin));
        registry.register(new CollapsedDome(plugin));
        registry.register(new Ziggurat(plugin));
    }

    // ================================================================
    // 1. MOON GATE — 16 QUARTZ+CALCITE archway rising from ground
    // ================================================================
    public static class MoonGate extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> gateBlocks = new ArrayList<>();
        private final double[][] targetPositions = new double[16][3];
        private double riseProgress = 0;

        public MoonGate(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("moon_gate", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] mats = {Material.QUARTZ_BLOCK, Material.CALCITE};

            // Build archway: two pillars of 5 blocks each + 6 arch blocks on top
            // Left pillar (5 blocks)
            for (int i = 0; i < 5; i++) {
                targetPositions[i][0] = -2.0;
                targetPositions[i][1] = i * 1.0;
                targetPositions[i][2] = 0;
            }
            // Right pillar (5 blocks)
            for (int i = 0; i < 5; i++) {
                targetPositions[5 + i][0] = 2.0;
                targetPositions[5 + i][1] = i * 1.0;
                targetPositions[5 + i][2] = 0;
            }
            // Arch top (6 blocks in semicircle)
            for (int i = 0; i < 6; i++) {
                double angle = Math.PI * i / 5.0;
                targetPositions[10 + i][0] = Math.cos(angle) * 2.0;
                targetPositions[10 + i][1] = 4.0 + Math.sin(angle) * 1.5;
                targetPositions[10 + i][2] = 0;
            }

            // Spawn all underground initially
            for (int i = 0; i < 16; i++) {
                Location loc = center.clone().add(targetPositions[i][0], -2, targetPositions[i][2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[i % 2]);
                float s = (i >= 10) ? 0.7f : 0.8f;
                h.scale(s, s, s).glow(240, 240, 255).interpolation(5, 0);
                gateBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 1.2f, 0.5f);
            DisplayBuilder.dustParticles(center, 30, 3.0, 240, 240, 255, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rise over 40 ticks
            if (riseProgress < 1.0) {
                riseProgress += 0.025;
                if (riseProgress > 1.0) riseProgress = 1.0;

                for (int i = 0; i < gateBlocks.size(); i++) {
                    double y = -2.0 + (targetPositions[i][1] + 2.0) * riseProgress;
                    gateBlocks.get(i).entity().teleport(c.clone().add(targetPositions[i][0], y, targetPositions[i][2]));
                }

                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_STONE_BREAK, 0.4f, 0.7f);
                }
            }

            // Moonlight particles through arch
            if (ticksAlive % 3 == 0 && riseProgress >= 1.0) {
                Location archCenter = c.clone().add(0, 3.5, 0);
                DisplayBuilder.dustParticles(archCenter, 8, 1.5, 240, 240, 255, 1.2f);
                c.getWorld().spawnParticle(Particle.END_ROD, archCenter, 3, 1.0, 1.5, 0.2, 0.01);
            }

            // Ambient glow
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 2, 0), 6, 2.5, 180, 210, 255, 0.8f);
            }

            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MoonGate(plugin); }
    }

    // ================================================================
    // 2. RUINED PILLAR — 12 CALCITE+DEEPSLATE broken column, leans and falls
    // ================================================================
    public static class RuinedPillar extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> pillarBlocks = new ArrayList<>();
        private final double[] blockHeights = new double[12];
        private double leanAngle = 0;
        private boolean fallen = false;
        private double dirX, dirZ;

        public RuinedPillar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ruined_pillar", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(4.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(220);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double angle = Math.random() * Math.PI * 2;
            dirX = Math.cos(angle);
            dirZ = Math.sin(angle);

            Material[] mats = {Material.CALCITE, Material.DEEPSLATE};
            Random rand = new Random();
            for (int i = 0; i < 12; i++) {
                blockHeights[i] = i * 0.8;
                double ox = (rand.nextDouble() - 0.5) * 0.3;
                double oz = (rand.nextDouble() - 0.5) * 0.3;
                Location loc = center.clone().add(ox, blockHeights[i], oz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[i % 2]);
                // Irregular broken look
                float sx = 0.7f + (float)(rand.nextDouble() * 0.3);
                float sy = 0.7f + (float)(rand.nextDouble() * 0.2);
                h.scale(sx, sy, sx).glow(200, 200, 220).interpolation(3, 0);
                if (i > 8) {
                    // Top blocks are cracked/smaller
                    h.scale(0.5f, 0.5f, 0.5f);
                }
                pillarBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 1.0f, 0.6f);
            DisplayBuilder.dustParticles(center, 15, 1.0, 200, 200, 220, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (!fallen) {
                // Lean over 40 ticks
                if (ticksAlive < 40) {
                    leanAngle += 2.25; // reaches 90 degrees at tick 40
                } else {
                    fallen = true;
                    // Impact where top lands
                    double fallDist = 9.6 * 0.8; // 12 blocks * 0.8 height
                    Location impactLoc = c.clone().add(dirX * fallDist * 0.5, 0, dirZ * fallDist * 0.5);
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.playSound(c, Sound.BLOCK_STONE_BREAK, 1.5f, 0.4f);
                    DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.6f);
                    DisplayBuilder.dustParticles(impactLoc, 30, 4.0, 200, 200, 220, 1.5f);
                }

                double leanRad = Math.toRadians(leanAngle);
                for (int i = 0; i < pillarBlocks.size(); i++) {
                    double height = blockHeights[i];
                    // Rotate around base: height becomes horizontal distance, y decreases
                    double newY = height * Math.cos(leanRad);
                    double offset = height * Math.sin(leanRad);
                    pillarBlocks.get(i).entity().teleport(c.clone().add(dirX * offset, newY, dirZ * offset));
                }

                // Cracking particles
                if (ticksAlive % 5 == 0 && ticksAlive < 40) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 4, 0), 5, 0.5, 200, 200, 220, 0.8f);
                    DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, 0.4f, 0.8f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RuinedPillar(plugin); }
    }

    // ================================================================
    // 3. LUNAR ALTAR — 18 QUARTZ+AMETHYST+SEA_LANTERN tiered altar with beam
    // ================================================================
    public static class LunarAltar extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> altarBlocks = new ArrayList<>();
        private double riseProgress = 0;

        public LunarAltar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lunar_altar", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] mats = {Material.QUARTZ_BLOCK, Material.AMETHYST_BLOCK, Material.SEA_LANTERN};

            // Tier 1 (base): 8 QUARTZ in 3x3 ring (no center)
            double[][] tier1 = {{-1,0,-1},{0,0,-1},{1,0,-1},{-1,0,0},{1,0,0},{-1,0,1},{0,0,1},{1,0,1}};
            for (int i = 0; i < 8; i++) {
                Location loc = center.clone().add(tier1[i][0], -2, tier1[i][2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.QUARTZ_BLOCK);
                h.scale(0.9f, 0.5f, 0.9f).glow(240, 240, 255).interpolation(5, 0);
                altarBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Tier 2 (middle): 6 AMETHYST in smaller ring
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                Location loc = center.clone().add(Math.cos(angle) * 0.7, -2, Math.sin(angle) * 0.7);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.7f, 0.5f, 0.7f).glow(180, 160, 255).interpolation(5, 0);
                altarBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Tier 3 (top): 3 SEA_LANTERN crown
            for (int i = 0; i < 3; i++) {
                double angle = (Math.PI * 2 * i) / 3;
                Location loc = center.clone().add(Math.cos(angle) * 0.3, -2, Math.sin(angle) * 0.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                h.scale(0.5f, 0.6f, 0.5f).glow(150, 230, 255).interpolation(5, 0);
                altarBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Central beacon block
            BlockDisplayHandle beacon = displayBuilder.spawnBlock(center.clone().add(0, -2, 0), Material.SEA_LANTERN);
            beacon.scale(0.4f, 0.8f, 0.4f).glow(200, 240, 255).interpolation(5, 0);
            altarBlocks.add(beacon);
            spawnedEntities.add(beacon.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rise over 30 ticks
            if (riseProgress < 1.0) {
                riseProgress += 0.033;
                if (riseProgress > 1.0) riseProgress = 1.0;

                double[][] tier1 = {{-1,0,-1},{0,0,-1},{1,0,-1},{-1,0,0},{1,0,0},{-1,0,1},{0,0,1},{1,0,1}};
                for (int i = 0; i < 8; i++) {
                    double y = -2.0 + 2.0 * riseProgress;
                    altarBlocks.get(i).entity().teleport(c.clone().add(tier1[i][0], y, tier1[i][2]));
                }
                for (int i = 0; i < 6; i++) {
                    double angle = (Math.PI * 2 * i) / 6;
                    double y = -2.0 + 2.5 * riseProgress;
                    altarBlocks.get(8 + i).entity().teleport(c.clone().add(Math.cos(angle) * 0.7, y, Math.sin(angle) * 0.7));
                }
                for (int i = 0; i < 3; i++) {
                    double angle = (Math.PI * 2 * i) / 3;
                    double y = -2.0 + 3.0 * riseProgress;
                    altarBlocks.get(14 + i).entity().teleport(c.clone().add(Math.cos(angle) * 0.3, y, Math.sin(angle) * 0.3));
                }
                // Beacon
                double y = -2.0 + 3.5 * riseProgress;
                altarBlocks.get(17).entity().teleport(c.clone().add(0, y, 0));

                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_STONE_BREAK, 0.3f, 0.7f);
                }
            }

            // Upward beam from altar top
            if (riseProgress >= 1.0 && ticksAlive % 2 == 0) {
                for (int i = 0; i < 5; i++) {
                    Location beamLoc = c.clone().add(0, 2.0 + i * 1.5, 0);
                    DisplayBuilder.dustParticles(beamLoc, 4, 0.2, 200, 240, 255, 1.5f);
                }
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, 8, 0), 3, 0.1, 0.5, 0.1, 0.02);
            }

            // Ambient particles
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 1, 0), 6, 2.0, 180, 210, 255, 0.8f);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LunarAltar(plugin); }
    }

    // ================================================================
    // 4. CRUMBLING ARCH — 14 CALCITE arch that cracks, blocks fall individually
    // ================================================================
    public static class CrumblingArch extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> archBlocks = new ArrayList<>();
        private final double[][] archPositions = new double[14][3];
        private final boolean[] fallen = new boolean[14];
        private final double[] fallY = new double[14];
        private int nextFall = 0;
        private boolean builtUp = false;

        public CrumblingArch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crumbling_arch", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0);
            config.setImpactRadius(2.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(220);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Arch: semicircle of 14 blocks
            for (int i = 0; i < 14; i++) {
                double angle = Math.PI * i / 13.0;
                archPositions[i][0] = Math.cos(angle) * 3.5;
                archPositions[i][1] = Math.sin(angle) * 3.5;
                archPositions[i][2] = 0;
                fallen[i] = false;

                Location loc = center.clone().add(archPositions[i][0], archPositions[i][1], archPositions[i][2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CALCITE);
                float s = 0.7f + (float)(Math.sin(angle) * 0.2);
                h.scale(s, s, s).glow(240, 240, 255).interpolation(3, 0);
                archBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Arch stands for 40 ticks, then blocks start falling from top (middle) outward
            if (ticksAlive < 40) {
                // Gentle sway
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 4, 2.0, 240, 240, 255, 0.6f);
                }
                return;
            }

            if (!builtUp) {
                builtUp = true;
                // Build fall order: from top (index 7) outward alternating
                // Already in order by arch index, we'll drop from middle out
            }

            // Drop a block every 8 ticks, from center of arch outward
            if (ticksAlive % 8 == 0 && nextFall < 14) {
                // Drop order: 7, 6, 8, 5, 9, 4, 10, 3, 11, 2, 12, 1, 13, 0
                int[] dropOrder = {7, 6, 8, 5, 9, 4, 10, 3, 11, 2, 12, 1, 13, 0};
                int idx = dropOrder[nextFall];
                fallen[idx] = true;
                fallY[idx] = archPositions[idx][1];
                nextFall++;

                DisplayBuilder.playSound(c.clone().add(archPositions[idx][0], fallY[idx], 0), Sound.BLOCK_STONE_BREAK, 0.6f, 0.8f);
                DisplayBuilder.dustParticles(c.clone().add(archPositions[idx][0], fallY[idx], 0), 8, 0.5, 240, 240, 255, 0.8f);
            }

            // Animate falling blocks
            for (int i = 0; i < 14; i++) {
                if (fallen[i] && fallY[i] > -5) {
                    fallY[i] -= 0.6;
                    archBlocks.get(i).entity().teleport(c.clone().add(archPositions[i][0], Math.max(0, fallY[i]), archPositions[i][2]));

                    if (fallY[i] <= 0 && fallY[i] > -1) {
                        Location impactLoc = c.clone().add(archPositions[i][0], 0, archPositions[i][2]);
                        triggerImpactDamage(impactLoc);
                        DisplayBuilder.playSound(impactLoc, Sound.BLOCK_STONE_BREAK, 0.5f, 0.5f);
                        DisplayBuilder.dustParticles(impactLoc, 10, 2.0, 240, 240, 255, 1.0f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrumblingArch(plugin); }
    }

    // ================================================================
    // 5. OBELISK ROW — 15 QUARTZ pillars (5x3) rising in sequence with arcs
    // ================================================================
    public static class ObeliskRow extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> obeliskBlocks = new ArrayList<>();
        private final double[][] obeliskPositions = new double[5][2]; // 5 obelisk X,Z
        private final double[] riseProgress = new double[5];

        public ObeliskRow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("obelisk_row", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(4.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(260);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 5 obelisks in a line, each 3 blocks tall
            double angle = Math.random() * Math.PI;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);

            for (int o = 0; o < 5; o++) {
                double offset = (o - 2) * 2.5;
                obeliskPositions[o][0] = dx * offset;
                obeliskPositions[o][1] = dz * offset;
                riseProgress[o] = 0;

                for (int b = 0; b < 3; b++) {
                    Location loc = center.clone().add(obeliskPositions[o][0], -3, obeliskPositions[o][1]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.QUARTZ_BLOCK);
                    float sy = (b == 2) ? 0.5f : 0.8f; // top block tapered
                    h.scale(0.6f, sy, 0.6f).glow(240, 240, 255).interpolation(4, 0);
                    obeliskBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rise obelisks in sequence, 10 ticks apart
            for (int o = 0; o < 5; o++) {
                int obeliskStart = o * 10;
                if (ticksAlive >= obeliskStart && riseProgress[o] < 1.0) {
                    riseProgress[o] += 0.05;
                    if (riseProgress[o] > 1.0) riseProgress[o] = 1.0;

                    if (riseProgress[o] < 0.1) {
                        DisplayBuilder.playSound(c.clone().add(obeliskPositions[o][0], 0, obeliskPositions[o][1]),
                                Sound.BLOCK_STONE_BREAK, 0.5f, 0.6f);
                    }
                }

                for (int b = 0; b < 3; b++) {
                    double targetY = b * 0.9;
                    double y = -3.0 + (targetY + 3.0) * riseProgress[o];
                    obeliskBlocks.get(o * 3 + b).entity().teleport(
                            c.clone().add(obeliskPositions[o][0], y, obeliskPositions[o][1]));
                }
            }

            // Energy arcs between obelisks (once all risen)
            boolean allRisen = true;
            for (int o = 0; o < 5; o++) {
                if (riseProgress[o] < 1.0) { allRisen = false; break; }
            }

            if (allRisen && ticksAlive % 4 == 0) {
                for (int o = 0; o < 4; o++) {
                    Location from = c.clone().add(obeliskPositions[o][0], 2.5, obeliskPositions[o][1]);
                    Location to = c.clone().add(obeliskPositions[o + 1][0], 2.5, obeliskPositions[o + 1][1]);
                    // Particles along the arc
                    for (int p = 0; p < 5; p++) {
                        double t = p / 4.0;
                        Location mid = from.clone().add(
                                (to.getX() - from.getX()) * t,
                                Math.sin(t * Math.PI) * 1.0,
                                (to.getZ() - from.getZ()) * t
                        );
                        DisplayBuilder.dustParticles(mid, 2, 0.1, 150, 230, 255, 1.0f);
                    }
                }
            }

            // Top glow
            if (ticksAlive % 6 == 0) {
                for (int o = 0; o < 5; o++) {
                    if (riseProgress[o] >= 1.0) {
                        Location top = c.clone().add(obeliskPositions[o][0], 2.8, obeliskPositions[o][1]);
                        c.getWorld().spawnParticle(Particle.END_ROD, top, 1, 0, 0.2, 0, 0.01);
                    }
                }
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ObeliskRow(plugin); }
    }

    // ================================================================
    // 6. MOON DIAL — 16 QUARTZ+IRON_BLOCK sundial with rotating shadow arm
    // ================================================================
    public static class MoonDial extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> dialBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> armBlocks = new ArrayList<>();
        private float armAngle = 0;

        public MoonDial(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("moon_dial", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(320);
            config.setCooldownTicks(260);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Dial face: 10 blocks in a flat circle
            for (int i = 0; i < 10; i++) {
                double angle = (Math.PI * 2 * i) / 10;
                double r = 2.5;
                Location loc = center.clone().add(Math.cos(angle) * r, 0.1, Math.sin(angle) * r);
                Material mat = (i % 3 == 0) ? Material.IRON_BLOCK : Material.QUARTZ_BLOCK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.8f, 0.15f, 0.8f).glow(200, 200, 220).interpolation(3, 0);
                dialBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Center post
            BlockDisplayHandle post = displayBuilder.spawnBlock(center.clone().add(0, 0.3, 0), Material.IRON_BLOCK);
            post.scale(0.3f, 0.6f, 0.3f).glow(200, 200, 220).interpolation(3, 0);
            dialBlocks.add(post);
            spawnedEntities.add(post.entity());

            // Shadow arm: 5 DEEPSLATE blocks extending from center
            for (int i = 0; i < 5; i++) {
                double dist = (i + 1) * 0.7;
                Location loc = center.clone().add(dist, 0.2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DEEPSLATE);
                h.scale(0.6f, 0.2f, 0.3f).glow(80, 80, 100).interpolation(3, 0);
                armBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 0.8f, 0.7f);
            DisplayBuilder.dustParticles(center, 20, 3.0, 200, 200, 220, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rotate arm like a clock
            armAngle += 3.0f;
            double armRad = Math.toRadians(armAngle);

            for (int i = 0; i < armBlocks.size(); i++) {
                double dist = (i + 1) * 0.7;
                double x = Math.cos(armRad) * dist;
                double z = Math.sin(armRad) * dist;
                armBlocks.get(i).entity().teleport(c.clone().add(x, 0.2, z));
            }

            // Damage in arm's path
            // (handled by config continuous damage at center)

            // Shadow trail particles
            if (ticksAlive % 3 == 0) {
                double tipX = Math.cos(armRad) * 3.5;
                double tipZ = Math.sin(armRad) * 3.5;
                DisplayBuilder.dustParticles(c.clone().add(tipX, 0.3, tipZ), 4, 0.5, 80, 80, 100, 1.0f);
            }

            // Hour markers glow
            if (ticksAlive % 8 == 0) {
                int markerIndex = (int)(armAngle / 36) % 10;
                double angle = (Math.PI * 2 * markerIndex) / 10;
                Location marker = c.clone().add(Math.cos(angle) * 2.5, 0.3, Math.sin(angle) * 2.5);
                DisplayBuilder.dustParticles(marker, 6, 0.3, 150, 230, 255, 1.2f);
                DisplayBuilder.playSound(marker, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.2f + markerIndex * 0.1f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MoonDial(plugin); }
    }

    // ================================================================
    // 7. TEMPLE STEPS — 14 CALCITE ascending stairs, top explodes
    // ================================================================
    public static class TempleSteps extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> stepBlocks = new ArrayList<>();
        private final double[] stepTargetY = new double[14];
        private final double[] stepCurrentY = new double[14];
        private int nextRise = 0;
        private boolean topExploded = false;

        public TempleSteps(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("temple_steps", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0);
            config.setImpactRadius(3.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double angle = Math.random() * Math.PI * 2;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);

            for (int i = 0; i < 14; i++) {
                double dist = i * 0.8;
                stepTargetY[i] = i * 0.5;
                stepCurrentY[i] = -2.0;

                Location loc = center.clone().add(dx * dist, -2, dz * dist);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CALCITE);
                float sx = 1.2f - i * 0.04f;
                h.scale(sx, 0.5f, sx).glow(240, 240, 255).interpolation(4, 0);
                stepBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double angle = Math.atan2(
                    stepBlocks.get(1).entity().getLocation().getZ() - stepBlocks.get(0).entity().getLocation().getZ(),
                    stepBlocks.get(1).entity().getLocation().getX() - stepBlocks.get(0).entity().getLocation().getX()
            );
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);

            // Rise steps in sequence, 5 ticks apart
            if (ticksAlive % 5 == 0 && nextRise < 14) {
                nextRise++;
                DisplayBuilder.playSound(c, Sound.BLOCK_STONE_BREAK, 0.3f, 0.6f + nextRise * 0.05f);
            }

            for (int i = 0; i < 14; i++) {
                if (i < nextRise) {
                    if (stepCurrentY[i] < stepTargetY[i]) {
                        stepCurrentY[i] += 0.15;
                        if (stepCurrentY[i] > stepTargetY[i]) stepCurrentY[i] = stepTargetY[i];
                    }
                }
                double dist = i * 0.8;
                stepBlocks.get(i).entity().teleport(c.clone().add(dx * dist, stepCurrentY[i], dz * dist));
            }

            // Dust from rising
            if (ticksAlive % 4 == 0 && nextRise > 0 && nextRise <= 14) {
                int idx = nextRise - 1;
                double dist = idx * 0.8;
                DisplayBuilder.dustParticles(c.clone().add(dx * dist, stepCurrentY[idx], dz * dist), 4, 0.3, 240, 240, 255, 0.8f);
            }

            // Top explodes after all risen + 30 ticks
            if (nextRise >= 14 && !topExploded) {
                boolean allReached = true;
                for (int i = 0; i < 14; i++) {
                    if (stepCurrentY[i] < stepTargetY[i] - 0.01) { allReached = false; break; }
                }

                if (allReached && ticksAlive % 1 == 0) {
                    // Wait 30 more ticks after built, track via ticksAlive
                    topExploded = true;
                    double dist = 13 * 0.8;
                    Location topLoc = c.clone().add(dx * dist, stepTargetY[13], dz * dist);
                    triggerImpactDamage(topLoc);
                    DisplayBuilder.playSound(topLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
                    DisplayBuilder.dustParticles(topLoc, 40, 3.0, 240, 240, 255, 2.0f);
                    DisplayBuilder.particleRing(topLoc, 3.0, Particle.END_ROD, 15, null);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TempleSteps(plugin); }
    }

    // ================================================================
    // 8. SELENITE TOWER — 18 AMETHYST+QUARTZ tall faceted tower, rotates
    // ================================================================
    public static class SeleniteTower extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> towerBlocks = new ArrayList<>();
        private double riseProgress = 0;
        private float rotAngle = 0;

        public SeleniteTower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("selenite_tower", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] mats = {Material.AMETHYST_BLOCK, Material.QUARTZ_BLOCK};

            // 18 blocks: 6 layers of 3 blocks each, rotating slightly per layer
            for (int layer = 0; layer < 6; layer++) {
                double y = layer * 1.2;
                double radius = 1.2 - layer * 0.12; // taper
                double layerRotation = layer * Math.PI / 9; // twist

                for (int b = 0; b < 3; b++) {
                    double angle = (Math.PI * 2 * b) / 3 + layerRotation;
                    Location loc = center.clone().add(Math.cos(angle) * radius, -3, Math.sin(angle) * radius);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[(layer + b) % 2]);
                    float s = 0.6f - layer * 0.04f;
                    h.scale(s, 1.0f, s).glow(180, 160, 255).interpolation(4, 0);
                    // Faceted look: slight rotation per block
                    h.rotate((float)(angle * 0.3), 0, 1, 0);
                    towerBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rise over 30 ticks
            if (riseProgress < 1.0) {
                riseProgress += 0.033;
                if (riseProgress > 1.0) riseProgress = 1.0;
            }

            rotAngle += 1.5f;
            double rotRad = Math.toRadians(rotAngle);

            for (int layer = 0; layer < 6; layer++) {
                double targetY = layer * 1.2;
                double y = -3.0 + (targetY + 3.0) * riseProgress;
                double radius = 1.2 - layer * 0.12;
                double layerRotation = layer * Math.PI / 9 + rotRad;

                for (int b = 0; b < 3; b++) {
                    double angle = (Math.PI * 2 * b) / 3 + layerRotation;
                    int idx = layer * 3 + b;
                    towerBlocks.get(idx).entity().teleport(c.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius));
                }
            }

            // Reflecting beam particles from top
            if (riseProgress >= 1.0 && ticksAlive % 3 == 0) {
                double beamAngle = Math.toRadians(rotAngle * 2);
                for (int i = 0; i < 3; i++) {
                    double bAngle = beamAngle + (Math.PI * 2 * i) / 3;
                    double dist = 2.0 + i * 0.5;
                    Location beamLoc = c.clone().add(Math.cos(bAngle) * dist, 5.5 + i * 0.3, Math.sin(bAngle) * dist);
                    DisplayBuilder.dustParticles(beamLoc, 2, 0.2, 180, 160, 255, 1.0f);
                }
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, 6, 0), 2, 0.3, 0.3, 0.3, 0.02);
            }

            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.2f);
            }

            // Ambient shimmer
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 5, 1.5, 180, 160, 255, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SeleniteTower(plugin); }
    }

    // ================================================================
    // 9. ANCIENT WALL — 12 DEEPSLATE+CALCITE wall sliding toward player
    // ================================================================
    public static class AncientWall extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> wallBlocks = new ArrayList<>();
        private double dirX, dirZ;
        private double slideOffset = 0;

        public AncientWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ancient_wall", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Direction toward nearest player or random
            double angle = Math.random() * Math.PI * 2;
            Player nearest = null;
            double nearestDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                double d = p.getLocation().distanceSquared(center);
                if (d < nearestDist) {
                    nearestDist = d;
                    nearest = p;
                }
            }
            if (nearest != null) {
                Vector dir = nearest.getLocation().toVector().subtract(center.toVector());
                angle = Math.atan2(dir.getZ(), dir.getX());
            }
            dirX = Math.cos(angle);
            dirZ = Math.sin(angle);

            // 12 blocks: 4 wide x 3 tall wall, perpendicular to movement
            Material[] mats = {Material.DEEPSLATE, Material.CALCITE};
            for (int col = 0; col < 4; col++) {
                for (int row = 0; row < 3; row++) {
                    double spread = (col - 1.5) * 1.0;
                    double perpX = -dirZ * spread;
                    double perpZ = dirX * spread;
                    Location loc = center.clone().add(perpX - dirX * 8, row * 1.0, perpZ - dirZ * 8);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[(col + row) % 2]);
                    h.scale(0.9f, 0.9f, 0.9f).glow(160, 160, 180).interpolation(3, 0);
                    wallBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.4f);
            DisplayBuilder.dustParticles(center.clone().add(-dirX * 8, 1, -dirZ * 8), 25, 2.0, 160, 160, 180, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            slideOffset += 0.4;

            for (int col = 0; col < 4; col++) {
                for (int row = 0; row < 3; row++) {
                    double spread = (col - 1.5) * 1.0;
                    double perpX = -dirZ * spread;
                    double perpZ = dirX * spread;
                    int idx = col * 3 + row;
                    Location target = c.clone().add(
                            perpX + dirX * (-8 + slideOffset),
                            row * 1.0,
                            perpZ + dirZ * (-8 + slideOffset)
                    );
                    wallBlocks.get(idx).entity().teleport(target);
                }
            }

            // Update damage center to wall position
            setCenter(c.clone().add(dirX * (-8 + slideOffset), 0, dirZ * (-8 + slideOffset)));

            // Knockback players in front
            if (ticksAlive % 5 == 0) {
                Location wallCenter = getCenter();
                for (Player player : c.getWorld().getPlayers()) {
                    if (player.getLocation().distanceSquared(wallCenter) <= 25) {
                        Vector kb = new Vector(dirX * 1.0, 0.2, dirZ * 1.0);
                        player.setVelocity(player.getVelocity().add(kb));
                    }
                }
            }

            // Dust trail
            if (ticksAlive % 3 == 0) {
                Location wallFront = getCenter().clone().add(0, 1.5, 0);
                DisplayBuilder.dustParticles(wallFront, 8, 2.0, 160, 160, 180, 1.0f);
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_DEEPSLATE_BREAK, 0.6f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AncientWall(plugin); }
    }

    // ================================================================
    // 10. LUNAR FOUNTAIN — 14 PRISMARINE+SEA_LANTERN fountain with ice spray
    // ================================================================
    public static class LunarFountain extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> basinBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> spoutBlocks = new ArrayList<>();
        private double riseProgress = 0;

        public LunarFountain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lunar_fountain", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Basin ring: 8 PRISMARINE blocks
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Location loc = center.clone().add(Math.cos(angle) * 2.0, -2, Math.sin(angle) * 2.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PRISMARINE);
                h.scale(0.8f, 0.6f, 0.8f).glow(100, 180, 180).interpolation(4, 0);
                basinBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Central spout: 4 SEA_LANTERN stacked
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(0, -2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                float s = 0.5f - i * 0.08f;
                h.scale(s, 0.7f, s).glow(150, 230, 255).interpolation(4, 0);
                spoutBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Basin floor: 2 PRISMARINE flat
            for (int i = 0; i < 2; i++) {
                double ox = (i == 0) ? -0.5 : 0.5;
                Location loc = center.clone().add(ox, -2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PRISMARINE);
                h.scale(1.5f, 0.2f, 1.5f).glow(100, 180, 180).interpolation(4, 0);
                basinBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rise over 25 ticks
            if (riseProgress < 1.0) {
                riseProgress += 0.04;
                if (riseProgress > 1.0) riseProgress = 1.0;

                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 * i) / 8;
                    double y = -2.0 + 2.0 * riseProgress;
                    basinBlocks.get(i).entity().teleport(c.clone().add(Math.cos(angle) * 2.0, y, Math.sin(angle) * 2.0));
                }
                for (int i = 0; i < 4; i++) {
                    double y = -2.0 + (i * 0.7 + 0.3 + 2.0) * riseProgress;
                    spoutBlocks.get(i).entity().teleport(c.clone().add(0, y, 0));
                }
                for (int i = 0; i < 2; i++) {
                    double ox = (i == 0) ? -0.5 : 0.5;
                    double y = -2.0 + 2.0 * riseProgress;
                    basinBlocks.get(8 + i).entity().teleport(c.clone().add(ox, y - 0.3, 0));
                }
            }

            // Ice particle spray from top of spout
            if (riseProgress >= 1.0 && ticksAlive % 2 == 0) {
                Location spoutTop = c.clone().add(0, 3.2, 0);
                // Spray outward in all directions
                for (int i = 0; i < 4; i++) {
                    double angle = (Math.PI * 2 * i) / 4 + ticksAlive * 0.1;
                    Location spray = spoutTop.clone().add(Math.cos(angle) * 1.0, 0.5, Math.sin(angle) * 1.0);
                    DisplayBuilder.dustParticles(spray, 3, 0.3, 150, 230, 255, 1.0f);
                }
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, spoutTop, 5, 1.0, 0.8, 1.0, 0.02);
                c.getWorld().spawnParticle(Particle.DRIPPING_WATER, spoutTop, 3, 0.5, 0.3, 0.5, 0);
            }

            // Basin shimmer
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), 5, 2.0, 100, 180, 180, 0.8f);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LunarFountain(plugin); }
    }

    // ================================================================
    // 11. RUNESTONE CIRCLE — 16 DEEPSLATE in circle with glow effects
    // ================================================================
    public static class RunestoneCircle extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> stoneBlocks = new ArrayList<>();
        private double riseProgress = 0;

        public RunestoneCircle(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("runestone_circle", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(4.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(260);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 12 standing stones in a circle
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                double r = 4.0;
                Location loc = center.clone().add(Math.cos(angle) * r, -2, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DEEPSLATE);
                float height = 1.2f + (float)(Math.random() * 0.5);
                h.scale(0.5f, height, 0.5f).glow(100, 100, 120).interpolation(4, 0);
                // Slight outward lean
                h.rotate(0.1f, (float)-Math.sin(angle), 0, (float)Math.cos(angle));
                stoneBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 inner marker stones
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4 + Math.PI / 4;
                Location loc = center.clone().add(Math.cos(angle) * 1.5, -2, Math.sin(angle) * 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DEEPSLATE);
                h.scale(0.3f, 0.6f, 0.3f).glow(150, 150, 180).interpolation(4, 0);
                stoneBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rise over 30 ticks
            if (riseProgress < 1.0) {
                riseProgress += 0.033;
                if (riseProgress > 1.0) riseProgress = 1.0;

                for (int i = 0; i < 12; i++) {
                    double angle = (Math.PI * 2 * i) / 12;
                    double r = 4.0;
                    double y = -2.0 + 2.0 * riseProgress;
                    stoneBlocks.get(i).entity().teleport(c.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r));
                }
                for (int i = 0; i < 4; i++) {
                    double angle = (Math.PI * 2 * i) / 4 + Math.PI / 4;
                    double y = -2.0 + 2.0 * riseProgress;
                    stoneBlocks.get(12 + i).entity().teleport(c.clone().add(Math.cos(angle) * 1.5, y, Math.sin(angle) * 1.5));
                }

                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, 0.4f, 0.6f);
                }
            }

            // Rune glow effects: rotating highlight
            if (riseProgress >= 1.0 && ticksAlive % 3 == 0) {
                int glowIdx = (ticksAlive / 3) % 12;
                double angle = (Math.PI * 2 * glowIdx) / 12;
                Location glowLoc = c.clone().add(Math.cos(angle) * 4.0, 1.0, Math.sin(angle) * 4.0);
                DisplayBuilder.dustParticles(glowLoc, 8, 0.3, 150, 230, 255, 1.5f);

                // Connecting line to center
                for (int p = 0; p < 3; p++) {
                    double t = (p + 1) / 4.0;
                    Location lineLoc = c.clone().add(Math.cos(angle) * 4.0 * (1 - t), 0.5, Math.sin(angle) * 4.0 * (1 - t));
                    DisplayBuilder.dustParticles(lineLoc, 2, 0.1, 180, 210, 255, 0.8f);
                }
            }

            // Inner circle energy
            if (ticksAlive % 5 == 0 && riseProgress >= 1.0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), 2.0, Particle.END_ROD, 8, null);
            }

            // Rune particle effect (enchantment-like)
            if (ticksAlive % 4 == 0 && riseProgress >= 1.0) {
                c.getWorld().spawnParticle(Particle.ENCHANT, c.clone().add(0, 1, 0), 10, 3.0, 0.5, 3.0, 0.5);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RunestoneCircle(plugin); }
    }

    // ================================================================
    // 12. COLLAPSED DOME — 15 CALCITE+QUARTZ dome caving inward
    // ================================================================
    public static class CollapsedDome extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> domeBlocks = new ArrayList<>();
        private final double[][] domePositions = new double[15][3];
        private double collapseProgress = 0;
        private boolean collapsed = false;

        public CollapsedDome(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("collapsed_dome", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0);
            config.setImpactRadius(5.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] mats = {Material.CALCITE, Material.QUARTZ_BLOCK};

            // Dome: hemisphere of 15 blocks
            for (int i = 0; i < 15; i++) {
                // Distribute on hemisphere using fibonacci sphere
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 30); // only top half
                double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                double x = Math.sin(phi) * Math.cos(theta) * 4.0;
                double y = Math.cos(phi) * 4.0;
                double z = Math.sin(phi) * Math.sin(theta) * 4.0;

                if (y < 0) y = Math.abs(y) * 0.5; // keep above ground

                domePositions[i][0] = x;
                domePositions[i][1] = y;
                domePositions[i][2] = z;

                Location loc = center.clone().add(x, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[i % 2]);
                float s = 0.8f + (float)(Math.random() * 0.3);
                h.scale(s, s, s).glow(240, 240, 255).interpolation(3, 0);
                domeBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Stand for 30 ticks, then collapse over 40 ticks
            if (ticksAlive < 30) {
                // Cracking particles
                if (ticksAlive % 8 == 0) {
                    int idx = (ticksAlive / 8) % 15;
                    Location crackLoc = c.clone().add(domePositions[idx][0], domePositions[idx][1], domePositions[idx][2]);
                    DisplayBuilder.dustParticles(crackLoc, 6, 0.3, 240, 240, 255, 0.8f);
                    DisplayBuilder.playSound(crackLoc, Sound.BLOCK_STONE_BREAK, 0.3f, 1.0f);
                }
                return;
            }

            if (!collapsed) {
                collapseProgress += 0.025;
                if (collapseProgress > 1.0) collapseProgress = 1.0;

                // Move all blocks toward center and downward
                for (int i = 0; i < 15; i++) {
                    double x = domePositions[i][0] * (1.0 - collapseProgress * 0.8);
                    double y = domePositions[i][1] * (1.0 - collapseProgress);
                    double z = domePositions[i][2] * (1.0 - collapseProgress * 0.8);
                    domeBlocks.get(i).entity().teleport(c.clone().add(x, Math.max(0, y), z));
                }

                // Falling debris particles
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 2 * (1 - collapseProgress), 0), 10, 3.0, 240, 240, 255, 1.0f);
                }

                if (collapseProgress >= 1.0) {
                    collapsed = true;
                    triggerImpactDamage(c);
                    DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
                    DisplayBuilder.playSound(c, Sound.BLOCK_STONE_BREAK, 1.5f, 0.3f);
                    DisplayBuilder.dustParticles(c, 60, 5.0, 240, 240, 255, 2.5f);
                    DisplayBuilder.particleRing(c, 5.0, Particle.EXPLOSION, 6, null);

                    // Hide blocks
                    for (BlockDisplayHandle h : domeBlocks) {
                        h.entity().teleport(c.clone().add(0, -10, 0));
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CollapsedDome(plugin); }
    }

    // ================================================================
    // 13. ZIGGURAT — 18 blocks stepped pyramid rising, energy pulse from top
    // ================================================================
    public static class Ziggurat extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> zigBlocks = new ArrayList<>();
        private double riseProgress = 0;

        public Ziggurat(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ziggurat", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] mats = {Material.QUARTZ_BLOCK, Material.CALCITE, Material.DEEPSLATE};

            // 5 tiers: bottom 5x5 area (8 edge), then 6, 4, then cap
            // Tier 1 (base): 8 blocks around 3.0 radius
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                double r = 2.5;
                Location loc = center.clone().add(Math.cos(angle) * r, -3, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[0]);
                h.scale(1.0f, 0.7f, 1.0f).glow(240, 240, 255).interpolation(5, 0);
                zigBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Tier 2: 5 blocks at radius 1.8
            for (int i = 0; i < 5; i++) {
                double angle = (Math.PI * 2 * i) / 5 + Math.PI / 10;
                Location loc = center.clone().add(Math.cos(angle) * 1.8, -3, Math.sin(angle) * 1.8);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[1]);
                h.scale(0.9f, 0.7f, 0.9f).glow(240, 240, 255).interpolation(5, 0);
                zigBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Tier 3: 3 blocks at radius 1.0
            for (int i = 0; i < 3; i++) {
                double angle = (Math.PI * 2 * i) / 3;
                Location loc = center.clone().add(Math.cos(angle) * 1.0, -3, Math.sin(angle) * 1.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[2]);
                h.scale(0.8f, 0.7f, 0.8f).glow(200, 200, 220).interpolation(5, 0);
                zigBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Cap: 2 blocks at center
            for (int i = 0; i < 2; i++) {
                double ox = (i == 0) ? -0.2 : 0.2;
                Location loc = center.clone().add(ox, -3, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                h.scale(0.6f, 0.8f, 0.6f).glow(150, 230, 255).interpolation(5, 0);
                zigBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 1.2f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rise over 30 ticks
            if (riseProgress < 1.0) {
                riseProgress += 0.033;
                if (riseProgress > 1.0) riseProgress = 1.0;

                // Tier 1 base
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 * i) / 8;
                    double y = -3.0 + 3.0 * riseProgress;
                    zigBlocks.get(i).entity().teleport(c.clone().add(Math.cos(angle) * 2.5, y, Math.sin(angle) * 2.5));
                }
                // Tier 2
                for (int i = 0; i < 5; i++) {
                    double angle = (Math.PI * 2 * i) / 5 + Math.PI / 10;
                    double y = -3.0 + 3.7 * riseProgress;
                    zigBlocks.get(8 + i).entity().teleport(c.clone().add(Math.cos(angle) * 1.8, y, Math.sin(angle) * 1.8));
                }
                // Tier 3
                for (int i = 0; i < 3; i++) {
                    double angle = (Math.PI * 2 * i) / 3;
                    double y = -3.0 + 4.4 * riseProgress;
                    zigBlocks.get(13 + i).entity().teleport(c.clone().add(Math.cos(angle) * 1.0, y, Math.sin(angle) * 1.0));
                }
                // Cap
                for (int i = 0; i < 2; i++) {
                    double ox = (i == 0) ? -0.2 : 0.2;
                    double y = -3.0 + 5.0 * riseProgress;
                    zigBlocks.get(16 + i).entity().teleport(c.clone().add(ox, y, 0));
                }

                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_STONE_BREAK, 0.3f, 0.5f + (float)riseProgress * 0.5f);
                }
            }

            // Energy pulse from top (expanding damage radius over time)
            if (riseProgress >= 1.0) {
                // Growing radius: 5.0 base, grows to ~7.0 over duration
                double growFactor = Math.min(1.0, (ticksAlive - 30) / 320.0);
                double currentRadius = 5.0 + growFactor * 2.0;
                config.setDamageRadius(currentRadius);

                // Pulse visual every 20 ticks
                if (ticksAlive % 20 == 0) {
                    double pulseRadius = 2.0 + growFactor * 3.0;
                    DisplayBuilder.particleRing(c.clone().add(0, 2, 0), pulseRadius, Particle.END_ROD, 15, null);
                    DisplayBuilder.dustParticles(c.clone().add(0, 2.5, 0), 15, pulseRadius, 150, 230, 255, 1.2f);
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.8f);
                }

                // Beam from top
                if (ticksAlive % 3 == 0) {
                    Location top = c.clone().add(0, 2.5, 0);
                    c.getWorld().spawnParticle(Particle.END_ROD, top, 3, 0.1, 1.5, 0.1, 0.03);
                    DisplayBuilder.dustParticles(top, 4, 0.3, 200, 240, 255, 1.5f);
                }

                // Base energy ring
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.particleRing(c.clone().add(0, 0.2, 0), 3.0, Particle.ENCHANT, 10, null);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Ziggurat(plugin); }
    }
}
