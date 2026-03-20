package com.blockforge.chaoscraft.modes.bluemoon.attacks.blockdisplay;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import java.util.*;

/**
 * Blue Moon Mode — CELESTIAL STRUCTURES
 * 13 block display attacks themed around moons, stars, and celestial phenomena.
 * Blue Moon particle palette: pale blue (180,210,255) / silver (200,200,220) / frost cyan (150,230,255)
 */
public final class CelestialStructures {

    private CelestialStructures() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CrescentBlade(plugin));
        registry.register(new FullMoonDisc(plugin));
        registry.register(new StarSpike(plugin));
        registry.register(new BinaryStars(plugin));
        registry.register(new CometTrail(plugin));
        registry.register(new LunarRing(plugin));
        registry.register(new ConstellationMap(plugin));
        registry.register(new SupernovaBloom(plugin));
        registry.register(new MoonShardCluster(plugin));
        registry.register(new OrbitalRings(plugin));
        registry.register(new EclipseCrown(plugin));
        registry.register(new NeutronPulse(plugin));
        registry.register(new VoidCrescent(plugin));
    }

    // ================================================================
    // 1. CRESCENT BLADE — 14 BLUE_ICE blocks in crescent arc, rotating horizontally
    // ================================================================
    public static class CrescentBlade extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> crescentBlocks = new ArrayList<>();

        public CrescentBlade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crescent_blade", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 14 blocks arranged in a crescent arc (~200 degrees)
            for (int i = 0; i < 14; i++) {
                double angle = Math.toRadians(-100 + (200.0 * i / 13));
                double radius = 3.0;
                Location loc = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                float scale = 0.6f + 0.2f * (float) Math.sin(Math.PI * i / 13.0);
                h.scale(scale, scale, scale).glow(200, 200, 220).interpolation(3, 0);
                crescentBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double rotationAngle = Math.toRadians(ticksAlive * 3.0);

            for (int i = 0; i < crescentBlocks.size(); i++) {
                double arcAngle = Math.toRadians(-100 + (200.0 * i / 13));
                double totalAngle = arcAngle + rotationAngle;
                double radius = 3.0;
                double x = Math.cos(totalAngle) * radius;
                double z = Math.sin(totalAngle) * radius;
                crescentBlocks.get(i).entity().teleport(c.clone().add(x, 0, z));
            }

            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c, 8, 3.0, 200, 200, 220, 1.2f);
            }

            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_STEP, 0.3f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrescentBlade(plugin); }
    }

    // ================================================================
    // 2. FULL MOON DISC — 16 PACKED_ICE in filled circle, pulse scaling
    // ================================================================
    public static class FullMoonDisc extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> discBlocks = new ArrayList<>();

        public FullMoonDisc(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("full_moon_disc", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Filled disc: center + inner ring of 6 + outer ring of 9
            BlockDisplayHandle core = displayBuilder.spawnBlock(center, Material.PACKED_ICE);
            core.scale(0.8f, 0.8f, 0.8f).glow(180, 210, 255).interpolation(3, 0);
            discBlocks.add(core);
            spawnedEntities.add(core.entity());

            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                Location loc = center.clone().add(Math.cos(angle) * 1.2, 0, Math.sin(angle) * 1.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.7f, 0.7f, 0.7f).glow(180, 210, 255).interpolation(3, 0);
                discBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            for (int i = 0; i < 9; i++) {
                double angle = (Math.PI * 2 * i) / 9;
                Location loc = center.clone().add(Math.cos(angle) * 2.5, 0, Math.sin(angle) * 2.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.6f, 0.6f, 0.6f).glow(180, 210, 255).interpolation(3, 0);
                discBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Pulse scale 1.0 -> 1.3 -> 1.0 over 40 ticks
            int cyclePos = ticksAlive % 40;
            float pulseProgress = cyclePos / 40.0f;
            float scaleMult = 1.0f + 0.3f * (float) Math.sin(pulseProgress * Math.PI * 2);

            for (BlockDisplayHandle h : discBlocks) {
                float baseScale = 0.7f;
                h.scale(baseScale * scaleMult, baseScale * scaleMult, baseScale * scaleMult);
                h.interpolation(3, 0);
            }

            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(c, 12, 2.5, 180, 210, 255, 1.5f);
            }

            // Glow brightens at peak
            if (cyclePos == 10) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.3f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FullMoonDisc(plugin); }
    }

    // ================================================================
    // 3. STAR SPIKE — 12 QUARTZ_BLOCK in 5-pointed star, spinning
    // ================================================================
    public static class StarSpike extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> starBlocks = new ArrayList<>();
        // Pre-computed star offsets (relative to center)
        private final double[][] starOffsets = new double[12][2];

        public StarSpike(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("star_spike", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(280);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 5-pointed star: 5 outer tips + 5 inner vertices + 2 center blocks = 12
            int idx = 0;
            // Outer tips (radius 3.0)
            for (int i = 0; i < 5; i++) {
                double angle = Math.toRadians(-90 + 72.0 * i);
                starOffsets[idx][0] = Math.cos(angle) * 3.0;
                starOffsets[idx][1] = Math.sin(angle) * 3.0;
                idx++;
            }
            // Inner vertices (radius 1.3, offset by 36 degrees)
            for (int i = 0; i < 5; i++) {
                double angle = Math.toRadians(-90 + 72.0 * i + 36);
                starOffsets[idx][0] = Math.cos(angle) * 1.3;
                starOffsets[idx][1] = Math.sin(angle) * 1.3;
                idx++;
            }
            // Two center blocks
            starOffsets[10][0] = 0.3;
            starOffsets[10][1] = 0.0;
            starOffsets[11][0] = -0.3;
            starOffsets[11][1] = 0.0;

            for (int i = 0; i < 12; i++) {
                Location loc = center.clone().add(starOffsets[i][0], 0, starOffsets[i][1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.QUARTZ_BLOCK);
                float s = i < 5 ? 0.5f : (i < 10 ? 0.4f : 0.6f);
                h.scale(s, s, s).glow(200, 200, 220).interpolation(3, 0);
                starBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double rotAngle = Math.toRadians(ticksAlive * 5.0);

            for (int i = 0; i < starBlocks.size(); i++) {
                double ox = starOffsets[i][0];
                double oz = starOffsets[i][1];
                // Rotate around center
                double rx = ox * Math.cos(rotAngle) - oz * Math.sin(rotAngle);
                double rz = ox * Math.sin(rotAngle) + oz * Math.cos(rotAngle);
                starBlocks.get(i).entity().teleport(c.clone().add(rx, 0, rz));
            }

            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c, 6, 3.0, 200, 200, 220, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StarSpike(plugin); }
    }

    // ================================================================
    // 4. BINARY STARS — Two groups of 5 blocks orbiting each other
    // ================================================================
    public static class BinaryStars extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> starA = new ArrayList<>();
        private final List<BlockDisplayHandle> starB = new ArrayList<>();

        public BinaryStars(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("binary_stars", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Star A: 5 SEA_LANTERN in small sphere
            for (int i = 0; i < 5; i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 5);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                double x = Math.sin(phi) * Math.cos(theta) * 0.6;
                double y = Math.cos(phi) * 0.6;
                double z = Math.sin(phi) * Math.sin(theta) * 0.6;
                Location loc = center.clone().add(2 + x, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                h.scale(0.4f, 0.4f, 0.4f).glow(150, 230, 255).interpolation(3, 0);
                starA.add(h);
                spawnedEntities.add(h.entity());
            }

            // Star B: 5 AMETHYST_BLOCK in small sphere
            for (int i = 0; i < 5; i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 5);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                double x = Math.sin(phi) * Math.cos(theta) * 0.6;
                double y = Math.cos(phi) * 0.6;
                double z = Math.sin(phi) * Math.sin(theta) * 0.6;
                Location loc = center.clone().add(-2 + x, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.4f, 0.4f, 0.4f).glow(180, 210, 255).interpolation(3, 0);
                starB.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.6f, 1.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double orbitAngle = Math.toRadians(ticksAlive * 3.0);
            double orbitRadius = 2.5;

            // Star A orbit position
            double ax = Math.cos(orbitAngle) * orbitRadius;
            double az = Math.sin(orbitAngle) * orbitRadius;
            // Star B opposite side
            double bx = Math.cos(orbitAngle + Math.PI) * orbitRadius;
            double bz = Math.sin(orbitAngle + Math.PI) * orbitRadius;

            for (int i = 0; i < 5; i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 5);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i + ticksAlive * 0.05;
                double sx = Math.sin(phi) * Math.cos(theta) * 0.6;
                double sy = Math.cos(phi) * 0.6;
                double sz = Math.sin(phi) * Math.sin(theta) * 0.6;

                starA.get(i).entity().teleport(c.clone().add(ax + sx, sy, az + sz));
                starB.get(i).entity().teleport(c.clone().add(bx + sx, sy, bz + sz));
            }

            // Cyan particle trails
            if (ticksAlive % 2 == 0) {
                Location trailA = c.clone().add(ax, 0, az);
                Location trailB = c.clone().add(bx, 0, bz);
                DisplayBuilder.dustParticles(trailA, 4, 0.3, 150, 230, 255, 1.0f);
                DisplayBuilder.dustParticles(trailB, 4, 0.3, 150, 230, 255, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BinaryStars(plugin); }
    }

    // ================================================================
    // 5. COMET TRAIL — 5 BLUE_ICE head + 10 trail blocks, flies forward
    // ================================================================
    public static class CometTrail extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> headBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> trailBlocks = new ArrayList<>();
        private double travelX;
        private double travelZ;

        public CometTrail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("comet_trail", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(16.0);
            config.setImpactRadius(5.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Random travel direction
            double dirAngle = Math.random() * Math.PI * 2;
            travelX = Math.cos(dirAngle) * 0.5;
            travelZ = Math.sin(dirAngle) * 0.5;

            // Head: 5 BLUE_ICE clustered
            for (int i = 0; i < 5; i++) {
                double ox = (Math.random() - 0.5) * 0.8;
                double oy = (Math.random() - 0.5) * 0.8;
                double oz = (Math.random() - 0.5) * 0.8;
                Location loc = center.clone().add(ox, oy, oz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.5f, 0.5f, 0.5f).glow(180, 210, 255).interpolation(3, 0);
                headBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Trail: 10 LIGHT_BLUE_STAINED_GLASS fading in scale behind head
            for (int i = 0; i < 10; i++) {
                double dist = -(i + 1) * 0.8;
                Location loc = center.clone().add(-travelX * (i + 1) * 1.6, 0, -travelZ * (i + 1) * 1.6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIGHT_BLUE_STAINED_GLASS);
                float scale = 0.45f - (i * 0.035f);
                h.scale(scale, scale, scale).glow(150, 230, 255).interpolation(3, 0);
                h.brightness(15, 15);
                trailBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Move head forward
            Location headPos = c.clone().add(travelX * ticksAlive, 0, travelZ * ticksAlive);

            for (int i = 0; i < headBlocks.size(); i++) {
                double ox = (Math.sin(ticksAlive * 0.3 + i) * 0.3);
                double oy = (Math.cos(ticksAlive * 0.3 + i * 1.5) * 0.3);
                headBlocks.get(i).entity().teleport(headPos.clone().add(ox, oy, 0));
            }

            // Trail follows behind
            for (int i = 0; i < trailBlocks.size(); i++) {
                double lagTicks = ticksAlive - (i + 1) * 1.6;
                Location trailPos = c.clone().add(travelX * lagTicks, 0, travelZ * lagTicks);
                trailBlocks.get(i).entity().teleport(trailPos);
            }

            // Frost cyan particles behind head
            if (ticksAlive % 2 == 0) {
                DisplayBuilder.dustParticles(headPos, 6, 0.5, 150, 230, 255, 1.2f);
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(headPos, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.4f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CometTrail(plugin); }
    }

    // ================================================================
    // 6. LUNAR RING — 18 CALCITE in vertical ring, rotates around vertical axis
    // ================================================================
    public static class LunarRing extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ringBlocks = new ArrayList<>();

        public LunarRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lunar_ring", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(280);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 18 blocks in vertical ring (XY plane initially)
            double radius = 3.0;
            for (int i = 0; i < 18; i++) {
                double angle = (Math.PI * 2 * i) / 18;
                double x = Math.cos(angle) * radius;
                double y = Math.sin(angle) * radius;
                Location loc = center.clone().add(x, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CALCITE);
                h.scale(0.4f, 0.4f, 0.4f).glow(200, 200, 220).interpolation(3, 0);
                ringBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.7f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double yawAngle = Math.toRadians(ticksAlive * 2.0);
            double radius = 3.0;

            for (int i = 0; i < ringBlocks.size(); i++) {
                double ringAngle = (Math.PI * 2 * i) / 18;
                // Vertical ring in local XY, then rotate around Y axis
                double localX = Math.cos(ringAngle) * radius;
                double localY = Math.sin(ringAngle) * radius;
                // Rotate localX around Y axis by yawAngle
                double worldX = localX * Math.cos(yawAngle);
                double worldZ = localX * Math.sin(yawAngle);

                ringBlocks.get(i).entity().teleport(c.clone().add(worldX, localY, worldZ));
            }

            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c, 8, 3.0, 200, 200, 220, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LunarRing(plugin); }
    }

    // ================================================================
    // 7. CONSTELLATION MAP — 12 SEA_LANTERN at constellation positions, particle lines
    // ================================================================
    public static class ConstellationMap extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> starHandles = new ArrayList<>();
        // Pre-defined constellation positions (relative offsets)
        private final double[][] positions = {
            { 0.0, 2.0, 0.0 },   // top
            { 1.5, 1.5, 0.5 },
            { 2.5, 0.8, -0.3 },
            { 3.0, 0.0, 0.2 },
            { 2.0, -1.0, 0.8 },
            { 0.5, -1.5, -0.5 },
            { -1.0, -1.0, 0.3 },
            { -2.5, 0.0, -0.2 },
            { -2.0, 1.0, 0.6 },
            { -1.0, 2.0, -0.4 },
            { 0.5, 0.5, 0.0 },   // inner
            { -0.5, -0.3, 0.0 }  // inner
        };
        // Edges connecting stars (index pairs)
        private final int[][] edges = {
            {0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6},
            {6, 7}, {7, 8}, {8, 9}, {9, 0}, {10, 11}, {0, 10}
        };

        public ConstellationMap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("constellation_map", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (double[] pos : positions) {
                Location loc = center.clone().add(pos[0], pos[1], pos[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                h.scale(0.3f, 0.3f, 0.3f).glow(180, 210, 255).interpolation(3, 0);
                h.brightness(15, 15);
                starHandles.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Gentle twinkle: pulse brightness per star
            for (int i = 0; i < starHandles.size(); i++) {
                float pulse = 0.3f + 0.15f * (float) Math.sin(ticksAlive * 0.15 + i * 0.8);
                starHandles.get(i).scale(pulse, pulse, pulse);
                starHandles.get(i).interpolation(5, 0);
            }

            // Draw particle lines between connected stars every 3 ticks
            if (ticksAlive % 3 == 0) {
                for (int[] edge : edges) {
                    Location a = c.clone().add(positions[edge[0]][0], positions[edge[0]][1], positions[edge[0]][2]);
                    Location b = c.clone().add(positions[edge[1]][0], positions[edge[1]][1], positions[edge[1]][2]);
                    drawParticleLine(a, b, c.getWorld());
                }
            }
        }

        private void drawParticleLine(Location a, Location b, World w) {
            double dist = a.distance(b);
            int steps = (int) Math.ceil(dist * 3);
            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                double x = a.getX() + (b.getX() - a.getX()) * t;
                double y = a.getY() + (b.getY() - a.getY()) * t;
                double z = a.getZ() + (b.getZ() - a.getZ()) * t;
                Location point = new Location(w, x, y, z);
                DisplayBuilder.dustParticles(point, 1, 0.0, 180, 210, 255, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ConstellationMap(plugin); }
    }

    // ================================================================
    // 8. SUPERNOVA BLOOM — 16 DIAMOND_BLOCK, compressed then explode outward
    // ================================================================
    public static class SupernovaBloom extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> novaBlocks = new ArrayList<>();
        private final double[][] directions = new double[16][3];

        public SupernovaBloom(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("supernova_bloom", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(14.0);
            config.setImpactRadius(5.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 16 blocks start compressed at center, each with a random outward direction
            for (int i = 0; i < 16; i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 16);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                directions[i][0] = Math.sin(phi) * Math.cos(theta);
                directions[i][1] = Math.cos(phi);
                directions[i][2] = Math.sin(phi) * Math.sin(theta);

                Location loc = center.clone().add(directions[i][0] * 0.3, directions[i][1] * 0.3, directions[i][2] * 0.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.3f, 0.3f, 0.3f).glow(150, 230, 255).interpolation(3, 0);
                novaBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1 (0-40): compressed, building energy
            // Phase 2 (40-80): explode outward to 5-block sphere
            // Phase 3 (80-120): hold at max
            // Phase 4 (120-160): contract back
            // Phase 5 (160-200): compressed again

            float radius;
            if (ticksAlive < 40) {
                // Compressed, slight vibration
                radius = 0.3f + 0.1f * (float) Math.sin(ticksAlive * 0.5);
            } else if (ticksAlive < 80) {
                // Explode outward
                float progress = (ticksAlive - 40) / 40.0f;
                radius = 0.3f + progress * 4.7f;
            } else if (ticksAlive < 120) {
                // Hold at max
                radius = 5.0f;
            } else if (ticksAlive < 160) {
                // Contract back
                float progress = (ticksAlive - 120) / 40.0f;
                radius = 5.0f - progress * 4.7f;
            } else {
                radius = 0.3f;
            }

            float blockScale = 0.3f + (radius / 5.0f) * 0.5f;

            for (int i = 0; i < novaBlocks.size(); i++) {
                Location loc = c.clone().add(
                    directions[i][0] * radius,
                    directions[i][1] * radius,
                    directions[i][2] * radius
                );
                novaBlocks.get(i).entity().teleport(loc);
                novaBlocks.get(i).scale(blockScale, blockScale, blockScale);
                novaBlocks.get(i).interpolation(3, 0);
            }

            // Explosion burst at tick 40
            if (ticksAlive == 40) {
                c.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, c, 1, 0, 0, 0, 0);
                DisplayBuilder.dustParticles(c, 40, 5.0, 150, 230, 255, 2.5f);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 0.7f);
            }

            if (ticksAlive % 4 == 0 && ticksAlive < 40) {
                DisplayBuilder.dustParticles(c, 6, 0.5, 180, 210, 255, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SupernovaBloom(plugin); }
    }

    // ================================================================
    // 9. MOON SHARD CLUSTER — 12 CALCITE+QUARTZ at random angles, slowly rotate
    // ================================================================
    public static class MoonShardCluster extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> shardBlocks = new ArrayList<>();
        private final double[][] shardOffsets = new double[12][3];
        private final double[] shardAngles = new double[12];

        public MoonShardCluster(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("moon_shard_cluster", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(4.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Random rand = new Random();
            for (int i = 0; i < 12; i++) {
                // Random sphere position at radius 1.5-3.5
                double r = 1.5 + rand.nextDouble() * 2.0;
                double phi = Math.acos(1 - 2.0 * rand.nextDouble());
                double theta = rand.nextDouble() * Math.PI * 2;
                shardOffsets[i][0] = Math.sin(phi) * Math.cos(theta) * r;
                shardOffsets[i][1] = Math.cos(phi) * r;
                shardOffsets[i][2] = Math.sin(phi) * Math.sin(theta) * r;
                shardAngles[i] = rand.nextDouble() * Math.PI * 2;

                Material mat = (i % 2 == 0) ? Material.CALCITE : Material.QUARTZ_BLOCK;
                Location loc = center.clone().add(shardOffsets[i][0], shardOffsets[i][1], shardOffsets[i][2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float scale = 0.3f + rand.nextFloat() * 0.3f;
                h.scale(scale, 0.7f, scale).glow(200, 200, 220).interpolation(3, 0);
                shardBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CALCITE_BREAK, 0.7f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double rotAngle = Math.toRadians(ticksAlive * 1.5);

            for (int i = 0; i < shardBlocks.size(); i++) {
                double ox = shardOffsets[i][0];
                double oy = shardOffsets[i][1];
                double oz = shardOffsets[i][2];
                // Slow rotation around Y axis
                double rx = ox * Math.cos(rotAngle) - oz * Math.sin(rotAngle);
                double rz = ox * Math.sin(rotAngle) + oz * Math.cos(rotAngle);
                shardBlocks.get(i).entity().teleport(c.clone().add(rx, oy, rz));
            }

            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(c, 5, 3.5, 200, 200, 220, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MoonShardCluster(plugin); }
    }

    // ================================================================
    // 10. ORBITAL RINGS — 3 rings of 5 blocks at different tilts and speeds
    // ================================================================
    public static class OrbitalRings extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ringA = new ArrayList<>();
        private final List<BlockDisplayHandle> ringB = new ArrayList<>();
        private final List<BlockDisplayHandle> ringC = new ArrayList<>();

        public OrbitalRings(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("orbital_rings", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(320);
            config.setCooldownTicks(220);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double radius = 3.0;

            // Ring A: horizontal (XZ plane) — PACKED_ICE
            for (int i = 0; i < 5; i++) {
                double angle = (Math.PI * 2 * i) / 5;
                Location loc = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.4f, 0.4f, 0.4f).glow(180, 210, 255).interpolation(3, 0);
                ringA.add(h);
                spawnedEntities.add(h.entity());
            }

            // Ring B: tilted 60 degrees — BLUE_ICE
            for (int i = 0; i < 5; i++) {
                double angle = (Math.PI * 2 * i) / 5;
                double lx = Math.cos(angle) * radius;
                double ly = Math.sin(angle) * radius * Math.sin(Math.toRadians(60));
                double lz = Math.sin(angle) * radius * Math.cos(Math.toRadians(60));
                Location loc = center.clone().add(lx, ly, lz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.4f, 0.4f, 0.4f).glow(150, 230, 255).interpolation(3, 0);
                ringB.add(h);
                spawnedEntities.add(h.entity());
            }

            // Ring C: tilted -45 degrees — CALCITE
            for (int i = 0; i < 5; i++) {
                double angle = (Math.PI * 2 * i) / 5;
                double lx = Math.cos(angle) * radius * Math.cos(Math.toRadians(45));
                double ly = Math.sin(angle) * radius;
                double lz = Math.cos(angle) * radius * Math.sin(Math.toRadians(45));
                Location loc = center.clone().add(lx, ly, lz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CALCITE);
                h.scale(0.4f, 0.4f, 0.4f).glow(200, 200, 220).interpolation(3, 0);
                ringC.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double radius = 3.0;

            // Ring A: rotate at 2 deg/tick (horizontal)
            double angleA = Math.toRadians(ticksAlive * 2.0);
            for (int i = 0; i < 5; i++) {
                double ringAngle = (Math.PI * 2 * i) / 5 + angleA;
                double x = Math.cos(ringAngle) * radius;
                double z = Math.sin(ringAngle) * radius;
                ringA.get(i).entity().teleport(c.clone().add(x, 0, z));
            }

            // Ring B: rotate at 3 deg/tick (tilted 60)
            double angleB = Math.toRadians(ticksAlive * 3.0);
            for (int i = 0; i < 5; i++) {
                double ringAngle = (Math.PI * 2 * i) / 5 + angleB;
                double lx = Math.cos(ringAngle) * radius;
                double ly = Math.sin(ringAngle) * radius * Math.sin(Math.toRadians(60));
                double lz = Math.sin(ringAngle) * radius * Math.cos(Math.toRadians(60));
                ringB.get(i).entity().teleport(c.clone().add(lx, ly, lz));
            }

            // Ring C: rotate at 4 deg/tick (tilted -45)
            double angleC = Math.toRadians(ticksAlive * 4.0);
            for (int i = 0; i < 5; i++) {
                double ringAngle = (Math.PI * 2 * i) / 5 + angleC;
                double lx = Math.cos(ringAngle) * radius * Math.cos(Math.toRadians(45));
                double ly = Math.sin(ringAngle) * radius;
                double lz = Math.cos(ringAngle) * radius * Math.sin(Math.toRadians(45));
                ringC.get(i).entity().teleport(c.clone().add(lx, ly, lz));
            }

            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c, 6, 3.0, 180, 210, 255, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new OrbitalRings(plugin); }
    }

    // ================================================================
    // 11. ECLIPSE CROWN — DEEPSLATE ring + SEA_LANTERN corona, descends from Y+10
    // ================================================================
    public static class EclipseCrown extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ringBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> coronaBlocks = new ArrayList<>();

        public EclipseCrown(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("eclipse_crown", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location spawnCenter = center.clone().add(0, 10, 0);

            // 8 DEEPSLATE_BRICKS in ring (inner dark disc)
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Location loc = spawnCenter.clone().add(Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DEEPSLATE_BRICKS);
                h.scale(0.6f, 0.6f, 0.6f).glow(80, 80, 100).interpolation(3, 0);
                ringBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 6 SEA_LANTERN as corona rays
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                Location loc = spawnCenter.clone().add(Math.cos(angle) * 3.0, 0, Math.sin(angle) * 3.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                h.scale(0.45f, 0.45f, 0.45f).glow(180, 210, 255).interpolation(3, 0);
                h.brightness(15, 15);
                coronaBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 0.5f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Descend from Y+10 to Y+1 over first 60 ticks, then hover
            double yOffset;
            if (ticksAlive < 60) {
                double progress = ticksAlive / 60.0;
                yOffset = 10.0 - progress * 9.0;
            } else {
                // Gentle bob at Y+1
                yOffset = 1.0 + Math.sin(ticksAlive * 0.05) * 0.3;
            }

            // Slow rotation for corona
            double rotAngle = Math.toRadians(ticksAlive * 1.5);

            // Update ring blocks (inner dark disc)
            for (int i = 0; i < ringBlocks.size(); i++) {
                double angle = (Math.PI * 2 * i) / 8 + rotAngle * 0.5;
                Location loc = c.clone().add(Math.cos(angle) * 1.5, yOffset, Math.sin(angle) * 1.5);
                ringBlocks.get(i).entity().teleport(loc);
            }

            // Update corona blocks (outer glow)
            for (int i = 0; i < coronaBlocks.size(); i++) {
                double angle = (Math.PI * 2 * i) / 6 + rotAngle;
                // Corona pulses outward
                double coronaRadius = 3.0 + 0.3 * Math.sin(ticksAlive * 0.1 + i);
                Location loc = c.clone().add(Math.cos(angle) * coronaRadius, yOffset, Math.sin(angle) * coronaRadius);
                coronaBlocks.get(i).entity().teleport(loc);
            }

            // Silver particle ring
            if (ticksAlive % 4 == 0) {
                Location ringCenter = c.clone().add(0, yOffset, 0);
                DisplayBuilder.particleRing(ringCenter, 2.5, Particle.DUST, 12,
                        new Particle.DustOptions(Color.fromRGB(200, 200, 220), 1.2f));
            }

            // Descent sound
            if (ticksAlive == 1) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_AMBIENT, 0.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EclipseCrown(plugin); }
    }

    // ================================================================
    // 12. NEUTRON PULSE — 10 DIAMOND_BLOCK in sphere, contract/expand pulses
    // ================================================================
    public static class NeutronPulse extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> pulseBlocks = new ArrayList<>();
        private final double[][] unitDirs = new double[10][3];

        public NeutronPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("neutron_pulse", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(10.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 10; i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 10);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                unitDirs[i][0] = Math.sin(phi) * Math.cos(theta);
                unitDirs[i][1] = Math.cos(phi);
                unitDirs[i][2] = Math.sin(phi) * Math.sin(theta);

                double r = 1.5;
                Location loc = center.clone().add(unitDirs[i][0] * r, unitDirs[i][1] * r, unitDirs[i][2] * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.4f, 0.4f, 0.4f).glow(150, 230, 255).interpolation(3, 0);
                pulseBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CONDUIT_ACTIVATE, 0.8f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Pulse cycle every 40 ticks: contract to 0.5, expand to 2.0
            int cyclePos = ticksAlive % 40;
            double radius;
            float blockScale;

            if (cyclePos < 15) {
                // Contract from 1.5 to 0.5
                double progress = cyclePos / 15.0;
                radius = 1.5 - progress * 1.0;
                blockScale = 0.4f - (float) progress * 0.15f;
            } else if (cyclePos < 20) {
                // Hold compressed
                radius = 0.5;
                blockScale = 0.25f;
            } else if (cyclePos < 30) {
                // Expand from 0.5 to 2.0
                double progress = (cyclePos - 20) / 10.0;
                radius = 0.5 + progress * 1.5;
                blockScale = 0.25f + (float) progress * 0.35f;
            } else {
                // Contract back to 1.5
                double progress = (cyclePos - 30) / 10.0;
                radius = 2.0 - progress * 0.5;
                blockScale = 0.6f - (float) progress * 0.2f;
            }

            for (int i = 0; i < pulseBlocks.size(); i++) {
                Location loc = c.clone().add(
                    unitDirs[i][0] * radius,
                    unitDirs[i][1] * radius,
                    unitDirs[i][2] * radius
                );
                pulseBlocks.get(i).entity().teleport(loc);
                pulseBlocks.get(i).scale(blockScale, blockScale, blockScale);
                pulseBlocks.get(i).interpolation(3, 0);
            }

            // Particle burst at expansion peak
            if (cyclePos == 20) {
                DisplayBuilder.dustParticles(c, 30, 2.5, 150, 230, 255, 2.0f);
                DisplayBuilder.playSound(c, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.6f, 1.5f);
            }

            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c, 4, (float) radius, 180, 210, 255, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NeutronPulse(plugin); }
    }

    // ================================================================
    // 13. VOID CRESCENT — 13 CRYING_OBSIDIAN in vertical crescent, pendulum swing
    // ================================================================
    public static class VoidCrescent extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> crescentBlocks = new ArrayList<>();
        private final double[][] localOffsets = new double[13][2]; // local Y, local perpendicular

        public VoidCrescent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_crescent", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0);
            config.setImpactRadius(3.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(220);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 13 blocks in vertical crescent arc (~180 degrees in vertical plane)
            for (int i = 0; i < 13; i++) {
                double arcAngle = Math.toRadians(-90 + (180.0 * i / 12));
                double radius = 3.0;
                localOffsets[i][0] = Math.sin(arcAngle) * radius; // Y offset
                localOffsets[i][1] = Math.cos(arcAngle) * radius; // horizontal offset

                Location loc = center.clone().add(localOffsets[i][1], localOffsets[i][0], 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                float scale = 0.45f + 0.15f * (float) Math.sin(Math.PI * i / 12.0);
                h.scale(scale, scale, scale).glow(150, 100, 200).interpolation(3, 0);
                crescentBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.7f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Pendulum swing: oscillate the whole crescent like a pendulum
            // Swing angle: +/- 60 degrees from vertical, period ~80 ticks
            double swingAngle = Math.toRadians(60.0 * Math.sin(ticksAlive * Math.PI * 2 / 80.0));

            for (int i = 0; i < crescentBlocks.size(); i++) {
                double localY = localOffsets[i][0];
                double localH = localOffsets[i][1];

                // Apply pendulum rotation (rotate the horizontal offset into XZ plane)
                double worldX = localH * Math.cos(swingAngle);
                double worldZ = localH * Math.sin(swingAngle);
                double worldY = localY;

                crescentBlocks.get(i).entity().teleport(c.clone().add(worldX, worldY, worldZ));
            }

            // At swing endpoints (peak swing), trigger impact particles
            double swingVelocity = Math.cos(ticksAlive * Math.PI * 2 / 80.0);
            if (Math.abs(swingVelocity) < 0.05 && ticksAlive > 5) {
                // Near endpoint
                double endX = 3.0 * Math.cos(swingAngle);
                double endZ = 3.0 * Math.sin(swingAngle);
                Location impactLoc = c.clone().add(endX, 0, endZ);
                DisplayBuilder.dustParticles(impactLoc, 15, 2.0, 150, 100, 200, 1.5f);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.3f, 1.8f);
            }

            if (ticksAlive % 3 == 0) {
                DisplayBuilder.dustParticles(c, 4, 3.0, 200, 200, 220, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidCrescent(plugin); }
    }
}
