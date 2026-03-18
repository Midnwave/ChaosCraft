package com.blockforge.chaoscraft.modes.calamity.attacks.phase2.blockdisplay;

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
 * Phase 2 (DoG) Block Display -- GROUP 2: COSMIC OVERHEAD STRUCTURES
 * 10 structures suspended above the arena: orreries, chandeliers, prisms, weapons.
 * Devourer of Gods palette: amethyst, cyan, dark prismarine, polished blackstone.
 *
 * Design rules:
 * - NO status effects
 * - Always spawn straight (yaw=0, pitch=0)
 * - DoG glow colors: cyan(0,200,255), violet(128,0,255), white(240,240,255)
 */
public final class CosmicOverhead {

    private CosmicOverhead() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CelestialOrrery(plugin));
        registry.register(new CrystalChandelier(plugin));
        registry.register(new VoidAnchor(plugin));
        registry.register(new MeteorCluster(plugin));
        registry.register(new ConstellationWeb(plugin));
        registry.register(new DescendingPrism(plugin));
        registry.register(new CrownOfThorns(plugin));
        registry.register(new AstralLens(plugin));
        registry.register(new HangingGardenOfShards(plugin));
        registry.register(new OrbitalWeaponPlatform(plugin));
    }

    // ================================================================
    // 11. CELESTIAL ORRERY -- Multi-ring orbital system at Y+12
    // ================================================================
    public static class CelestialOrrery extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> outerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> middleRing = new ArrayList<>();
        private final List<BlockDisplayHandle> endRods = new ArrayList<>();
        private BlockDisplayHandle centerSun;
        private final List<BlockDisplayHandle> innerGlass = new ArrayList<>();

        public CelestialOrrery(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("celestial_orrery", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location overhead = center.clone().add(0, 12, 0);

            // Outer ring: 16 amethyst blocks in 6-block radius circle
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16;
                Location loc = overhead.clone().add(Math.cos(angle) * 6, 0, Math.sin(angle) * 6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.7f, 0.7f, 0.7f).glow(0, 200, 255).interpolation(2, 0);
                outerRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Middle ring: 8 large amethyst clusters at 3.5-block radius
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Location loc = overhead.clone().add(Math.cos(angle) * 3.5, 0, Math.sin(angle) * 3.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.6f, 0.8f, 0.6f).glow(128, 0, 255).interpolation(2, 0);
                middleRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // 16 end rods between outer and middle ring
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16;
                Location loc = overhead.clone().add(Math.cos(angle) * 4.8, 0, Math.sin(angle) * 4.8);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.END_ROD);
                h.scale(0.2f, 0.8f, 0.2f).glow(240, 240, 255).interpolation(2, 0);
                endRods.add(h);
                spawnedEntities.add(h.entity());
            }

            // Inner ring: 4 sea lantern "eyes" at 1.5-block radius (representing Eyes of Ender)
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4;
                Location loc = overhead.clone().add(Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                h.scale(0.5f, 0.5f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
                spawnedEntities.add(h.entity());
            }

            // 4 cyan stained glass in cross pattern
            double[][] glassOffsets = {{0.5, 0, 0}, {-0.5, 0, 0}, {0, 0, 0.5}, {0, 0, -0.5}};
            for (double[] off : glassOffsets) {
                Location loc = overhead.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
                h.scale(0.4f, 0.4f, 0.4f).glow(0, 200, 255).interpolation(2, 0);
                innerGlass.add(h);
                spawnedEntities.add(h.entity());
            }

            // Center sun: sea lantern scaled 1.5x
            centerSun = displayBuilder.spawnBlock(overhead, Material.SEA_LANTERN);
            centerSun.scale(1.5f, 1.5f, 1.5f).glow(0, 200, 255).interpolation(2, 0);
            spawnedEntities.add(centerSun.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 0.4f);
            DisplayBuilder.cyanDust(overhead, 25, 5.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            Location overhead = center.clone().add(0, 12, 0);

            // Assembly breathes downward: descends 2 blocks, rises back over 240-tick cycle
            float breathOffset = -2.0f * (float) Math.sin(ticksAlive * (2 * Math.PI / 240));
            Location breathed = overhead.clone().add(0, breathOffset, 0);

            // Outer ring rotates clockwise at 0.8 deg/tick
            float outerAngle = ticksAlive * 0.014f;
            for (int i = 0; i < outerRing.size(); i++) {
                double angle = (Math.PI * 2 * i) / 16 + outerAngle;
                Location loc = breathed.clone().add(Math.cos(angle) * 6, 0, Math.sin(angle) * 6);
                outerRing.get(i).entity().teleport(loc);
            }

            // Middle ring counter-rotates at 1.2 deg/tick
            float middleAngle = -ticksAlive * 0.021f;
            for (int i = 0; i < middleRing.size(); i++) {
                double angle = (Math.PI * 2 * i) / 8 + middleAngle;
                Location loc = breathed.clone().add(Math.cos(angle) * 3.5, 0, Math.sin(angle) * 3.5);
                middleRing.get(i).entity().teleport(loc);
            }

            // Center sun scale pulse: 1.0 to 1.4, 60-tick cycle
            float sunScale = 1.5f + 0.4f * (float) Math.sin(ticksAlive * (2 * Math.PI / 60));
            if (centerSun != null) {
                centerSun.entity().teleport(breathed);
                centerSun.scale(sunScale, sunScale, sunScale);
                centerSun.interpolation(2, 0);
            }

            // Laser lines from center to each inner eye (4 beams)
            if (ticksAlive % 5 == 0) {
                float innerAngle = ticksAlive * 0.035f;
                for (int i = 0; i < 4; i++) {
                    double angle = (Math.PI * 2 * i) / 4 + innerAngle;
                    Location eyeLoc = breathed.clone().add(Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5);
                    for (int p = 0; p < 6; p++) {
                        double t = p / 5.0;
                        Location particle = breathed.clone().add(
                                (eyeLoc.getX() - breathed.getX()) * t,
                                (eyeLoc.getY() - breathed.getY()) * t,
                                (eyeLoc.getZ() - breathed.getZ()) * t
                        );
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particle, 1, 0, 0, 0, 0);
                    }
                }
            }

            // End rod particles from outer ring
            if (ticksAlive % 10 == 0) {
                for (BlockDisplayHandle h : outerRing) {
                    center.getWorld().spawnParticle(Particle.END_ROD,
                            h.entity().getLocation(), 1, 0.1, 0, 0.1, 0.02);
                }
            }

            // Descent minimum portal burst
            if (ticksAlive % 240 == 120) {
                center.getWorld().spawnParticle(Particle.PORTAL, breathed, 30, 5.0, 1.0, 5.0, 0.5);
            }

            // Sound every 200 ticks
            if (ticksAlive % 200 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.4f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CelestialOrrery(plugin); }
    }

    // ================================================================
    // 12. CRYSTAL CHANDELIER -- Downward-pointing chandelier at Y+28
    // ================================================================
    public static class CrystalChandelier extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> mountBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> longArms = new ArrayList<>();
        private final List<BlockDisplayHandle> shortArms = new ArrayList<>();
        private final List<BlockDisplayHandle> glassRing = new ArrayList<>();

        public CrystalChandelier(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_chandelier", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(4.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location ceiling = center.clone().add(0, 28, 0);

            // Top mount: 3x3x2 amethyst cluster
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            ceiling.clone().add(x * 0.5, 0, z * 0.5), Material.AMETHYST_BLOCK);
                    h.scale(0.6f, 1.0f, 0.6f).glow(0, 200, 255).interpolation(2, 0);
                    mountBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // 4 sea lanterns inside mount
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4 + (Math.PI / 4);
                BlockDisplayHandle sl = displayBuilder.spawnBlock(
                        ceiling.clone().add(Math.cos(angle) * 0.3, -0.5, Math.sin(angle) * 0.3), Material.SEA_LANTERN);
                sl.scale(0.3f, 0.3f, 0.3f).glow(0, 200, 255).interpolation(2, 0);
                spawnedEntities.add(sl.entity());
            }

            // 4 long arms (hanging to Y+20, length 5): amethyst shards + cluster tip
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4;
                for (int seg = 0; seg < 5; seg++) {
                    float yOff = -(seg + 1) * 1.6f;
                    Material mat = seg < 4 ? Material.AMETHYST_CLUSTER : Material.AMETHYST_CLUSTER;
                    Location loc = ceiling.clone().add(Math.cos(angle) * 1.5, yOff, Math.sin(angle) * 1.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(0.4f, 0.6f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
                    longArms.add(h);
                    spawnedEntities.add(h.entity());
                }
                // End rods at long arm tips
                for (int side = -1; side <= 1; side += 2) {
                    Location rodLoc = ceiling.clone().add(Math.cos(angle) * 1.5 + side * 0.4, -8, Math.sin(angle) * 1.5);
                    BlockDisplayHandle rod = displayBuilder.spawnBlock(rodLoc, Material.END_ROD);
                    rod.scale(0.7f, 0.2f, 0.2f).glow(240, 240, 255).interpolation(2, 0);
                    spawnedEntities.add(rod.entity());
                }
            }

            // 4 short arms (hanging to Y+22, length 3)
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4 + (Math.PI / 4);
                for (int seg = 0; seg < 3; seg++) {
                    float yOff = -(seg + 1) * 2.0f;
                    Material mat = seg < 2 ? Material.AMETHYST_CLUSTER : Material.AMETHYST_CLUSTER;
                    Location loc = ceiling.clone().add(Math.cos(angle) * 1.5, yOff, Math.sin(angle) * 1.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(0.4f, 0.6f, 0.4f).glow(0, 200, 255).interpolation(2, 0);
                    shortArms.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Cyan stained glass octagonal ring between arm tips
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Location loc = ceiling.clone().add(Math.cos(angle) * 2.0, -7, Math.sin(angle) * 2.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
                h.scale(0.8f, 0.3f, 0.3f).glow(0, 200, 255).interpolation(2, 0);
                glassRing.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            Location ceiling = center.clone().add(0, 28, 0);

            // Entire chandelier rotates at 0.4 deg/tick
            float rot = ticksAlive * 0.007f;

            // Long arms oscillate up/down 0.5 blocks, 80-tick cycle
            float longBob = 0.5f * (float) Math.sin(ticksAlive * (2 * Math.PI / 80));
            // Short arms inverse phase
            float shortBob = -longBob;

            // Update long arm positions
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4 + rot;
                for (int seg = 0; seg < 5; seg++) {
                    int idx = i * 5 + seg;
                    if (idx < longArms.size()) {
                        float yOff = -(seg + 1) * 1.6f + longBob;
                        Location loc = ceiling.clone().add(Math.cos(angle) * 1.5, yOff, Math.sin(angle) * 1.5);
                        longArms.get(idx).entity().teleport(loc);
                    }
                }
            }

            // Update short arm positions
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4 + (Math.PI / 4) + rot;
                for (int seg = 0; seg < 3; seg++) {
                    int idx = i * 3 + seg;
                    if (idx < shortArms.size()) {
                        float yOff = -(seg + 1) * 2.0f + shortBob;
                        Location loc = ceiling.clone().add(Math.cos(angle) * 1.5, yOff, Math.sin(angle) * 1.5);
                        shortArms.get(idx).entity().teleport(loc);
                    }
                }
            }

            // Rotating electric spark ring at arm tips
            if (ticksAlive % 8 == 0) {
                int sparkIdx = (ticksAlive / 8) % 8;
                int nextIdx = (sparkIdx + 1) % 8;
                if (sparkIdx < glassRing.size() && nextIdx < glassRing.size()) {
                    Location from = glassRing.get(sparkIdx).entity().getLocation();
                    Location to = glassRing.get(nextIdx).entity().getLocation();
                    for (int p = 0; p < 5; p++) {
                        double t = p / 4.0;
                        Location particle = from.clone().add(
                                (to.getX() - from.getX()) * t,
                                (to.getY() - from.getY()) * t,
                                (to.getZ() - from.getZ()) * t
                        );
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particle, 1, 0, 0, 0, 0);
                    }
                }
            }

            // End rod particles falling from tips
            if (ticksAlive % 6 == 0) {
                for (int i = 0; i < longArms.size(); i += 5) {
                    if (i + 4 < longArms.size()) {
                        center.getWorld().spawnParticle(Particle.END_ROD,
                                longArms.get(i + 4).entity().getLocation(), 1, 0, 0, 0, 0.03);
                    }
                }
            }

            // Cyan dust orbiting glass ring
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.cyanDust(ceiling.clone().add(0, -7, 0), 4, 2.0);
            }

            // Sound every 120 ticks
            if (ticksAlive % 120 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalChandelier(plugin); }
    }

    // ================================================================
    // 13. VOID ANCHOR -- Downward spike from Y+25 to Y+16
    // ================================================================
    public static class VoidAnchor extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> spikeBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> collarClusters = new ArrayList<>();
        private BlockDisplayHandle tipCluster;

        public VoidAnchor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_anchor", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 5x5 cap at Y+25
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (Math.abs(x) + Math.abs(z) > 3) continue;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x * 0.5, 25, z * 0.5), Material.POLISHED_BLACKSTONE);
                    h.scale(0.5f, 0.4f, 0.5f).glow(80, 80, 100).interpolation(2, 0);
                    spawnedEntities.add(h.entity());
                }
            }

            // Spike body: tapering from Y+25 to Y+16
            Material[] spikeMats = {Material.POLISHED_BLACKSTONE, Material.POLISHED_BLACKSTONE, Material.POLISHED_BLACKSTONE,
                    Material.DARK_PRISMARINE, Material.DARK_PRISMARINE,
                    Material.DARK_PRISMARINE, Material.DARK_PRISMARINE,
                    Material.AMETHYST_BLOCK, Material.AMETHYST_BLOCK};
            for (int i = 0; i < spikeMats.length; i++) {
                float taper = 1.0f - (i * 0.08f);
                Location loc = center.clone().add(0, 24 - i, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, spikeMats[i]);
                h.scale(taper, 1.0f, taper).glow(0, 200, 255).interpolation(2, 0);
                spikeBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Tip: large amethyst cluster pointing downward at Y+16
            tipCluster = displayBuilder.spawnBlock(center.clone().add(0, 16, 0), Material.AMETHYST_CLUSTER);
            tipCluster.scale(0.4f, 0.8f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
            spawnedEntities.add(tipCluster.entity());

            // Collar: 8 large amethyst clusters at Y+20 pointing outward
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Location loc = center.clone().add(Math.cos(angle) * 1.0, 20, Math.sin(angle) * 1.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.5f, 0.6f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                collarClusters.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.6f, 0.6f);
            DisplayBuilder.cyanDust(center.clone().add(0, 20, 0), 20, 3.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Spike breathes vertically: tip oscillates 0.5 blocks, 120-tick cycle
            float breathOff = 0.5f * (float) Math.sin(ticksAlive * (2 * Math.PI / 120));

            if (tipCluster != null) {
                tipCluster.entity().teleport(center.clone().add(0, 16 + breathOff, 0));
            }

            // Collar rotates at 1 deg/tick
            float collarRot = ticksAlive * 0.0175f;
            for (int i = 0; i < collarClusters.size(); i++) {
                double angle = (Math.PI * 2 * i) / 8 + collarRot;
                Location loc = center.clone().add(Math.cos(angle) * 1.0, 20, Math.sin(angle) * 1.0);
                collarClusters.get(i).entity().teleport(loc);
            }

            // Laser beams from 4 cardinal points at Y+22 converging on tip
            if (ticksAlive % 5 == 0) {
                Location tipLoc = center.clone().add(0, 16 + breathOff, 0);
                for (int i = 0; i < 4; i++) {
                    double angle = (Math.PI * 2 * i) / 4 - ticksAlive * 0.00875f;
                    Location swordPos = center.clone().add(Math.cos(angle) * 1.5, 22, Math.sin(angle) * 1.5);
                    for (int p = 0; p < 6; p++) {
                        double t = p / 5.0;
                        Location particle = swordPos.clone().add(
                                (tipLoc.getX() - swordPos.getX()) * t,
                                (tipLoc.getY() - swordPos.getY()) * t,
                                (tipLoc.getZ() - swordPos.getZ()) * t
                        );
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particle, 1, 0, 0, 0, 0);
                    }
                }
            }

            // Portal particles falling from tip at max descent
            if (breathOff < -0.3f && ticksAlive % 4 == 0) {
                center.getWorld().spawnParticle(Particle.PORTAL,
                        center.clone().add(0, 16 + breathOff - 1, 0), 4, 0.2, 0.5, 0.2, 0.05);
            }

            // Dragon breath at collar
            if (ticksAlive % 6 == 0) {
                center.getWorld().spawnParticle(Particle.DRAGON_BREATH,
                        center.clone().add(0, 20, 0), 3, 1.0, 0.2, 1.0, 0.01);
            }

            // Sound every 60 ticks (heartbeat-like)
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.4f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidAnchor(plugin); }
    }

    // ================================================================
    // 14. METEOR CLUSTER -- 12 orbiting crystal meteors
    // ================================================================
    public static class MeteorCluster extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> meteors = new ArrayList<>();
        private final double[] orbitRadii = new double[12];
        private final double[] orbitSpeeds = new double[12];
        private final double[] orbitTilts = new double[12];
        private final double[] orbitPhases = new double[12];
        private final float[] meteorYBases = new float[12];

        public MeteorCluster(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("meteor_cluster", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location clusterCenter = center.clone().add(0, 20, 0);

            Material[] largeMats = {Material.AMETHYST_BLOCK, Material.AMETHYST_BLOCK,
                    Material.AMETHYST_BLOCK, Material.AMETHYST_BLOCK};
            Material[] medMats = {Material.DARK_PRISMARINE, Material.DARK_PRISMARINE,
                    Material.DARK_PRISMARINE, Material.DARK_PRISMARINE};

            for (int i = 0; i < 12; i++) {
                orbitRadii[i] = 2.0 + (i * 0.3);
                orbitSpeeds[i] = 0.005 + (i * 0.001);
                orbitTilts[i] = (i * 15.0) * (Math.PI / 180.0);
                orbitPhases[i] = (Math.PI * 2 * i) / 12;
                meteorYBases[i] = 15 + (i % 3) * 3.5f;

                Material mat;
                float scale;
                if (i < 4) {
                    mat = largeMats[i];
                    scale = 1.2f;
                } else if (i < 8) {
                    mat = medMats[i - 4];
                    scale = 0.8f;
                } else {
                    mat = Material.AMETHYST_CLUSTER;
                    scale = 1.3f * 0.5f;
                }

                Location loc = clusterCenter.clone().add(orbitRadii[i], 0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(scale, scale, scale).glow(0, 200, 255).interpolation(2, 0);
                meteors.add(h);
                spawnedEntities.add(h.entity());

                // End rods attached to each meteor
                for (int r = 0; r < 2; r++) {
                    BlockDisplayHandle rod = displayBuilder.spawnBlock(loc.clone().add(r * 0.5 - 0.25, 0, 0), Material.END_ROD);
                    rod.scale(0.15f, 0.15f, 0.6f).glow(240, 240, 255).interpolation(2, 0);
                    spawnedEntities.add(rod.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Each meteor orbits on unique elliptical path
            for (int i = 0; i < meteors.size(); i++) {
                double angle = orbitPhases[i] + ticksAlive * orbitSpeeds[i];
                double x = Math.cos(angle) * orbitRadii[i];
                double z = Math.sin(angle) * orbitRadii[i];
                double y = meteorYBases[i] + Math.sin(angle + orbitTilts[i]) * 1.5;

                Location loc = center.clone().add(x, y, z);
                meteors.get(i).entity().teleport(loc);

                // Local rotation
                BlockDisplay bd = meteors.get(i).entity();
                float localRot = ticksAlive * (0.01f + i * 0.002f);
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.4f, -0.4f, -0.4f),
                        new AxisAngle4f(localRot, 0.3f, 1, 0.2f),
                        new Vector3f(0.8f, 0.8f, 0.8f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(2);
            }

            // Electric spark connections between nearest pairs
            if (ticksAlive % 6 == 0) {
                for (int conn = 0; conn < 3; conn++) {
                    int a = (ticksAlive / 6 + conn * 4) % meteors.size();
                    int b = (a + 1) % meteors.size();
                    Location from = meteors.get(a).entity().getLocation();
                    Location to = meteors.get(b).entity().getLocation();
                    for (int p = 0; p < 5; p++) {
                        double t = p / 4.0;
                        Location particle = from.clone().add(
                                (to.getX() - from.getX()) * t,
                                (to.getY() - from.getY()) * t,
                                (to.getZ() - from.getZ()) * t
                        );
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particle, 1, 0, 0, 0, 0);
                    }
                }
            }

            // End rod particle trails from each meteor
            if (ticksAlive % 4 == 0) {
                for (BlockDisplayHandle h : meteors) {
                    center.getWorld().spawnParticle(Particle.END_ROD,
                            h.entity().getLocation(), 1, 0.1, 0.1, 0.1, 0.01);
                }
            }

            // Purple dust orbiting each meteor
            if (ticksAlive % 8 == 0) {
                int idx = (ticksAlive / 8) % meteors.size();
                DisplayBuilder.purpleDust(meteors.get(idx).entity().getLocation(), 3, 1.0);
            }

            // Sound every 200 ticks
            if (ticksAlive % 200 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MeteorCluster(plugin); }
    }

    // ================================================================
    // 15. CONSTELLATION WEB -- S-curve of star nodes with laser links
    // ================================================================
    public static class ConstellationWeb extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> starNodes = new ArrayList<>();

        public ConstellationWeb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("constellation_web", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 10 star nodes in S-curve at Y+18-24
            for (int i = 0; i < 10; i++) {
                double t = i / 9.0;
                double x = (t - 0.5) * 20; // Span 20 blocks
                double z = Math.sin(t * Math.PI * 2) * 4; // S-curve
                double y = 18 + (i % 3) * 3;
                Material mat = (i % 2 == 0) ? Material.SEA_LANTERN : Material.AMETHYST_BLOCK;
                float scale = (i % 2 == 0) ? 0.6f : 0.8f;

                Location loc = center.clone().add(x, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(scale, scale, scale).glow(0, 200, 255).interpolation(2, 0);
                starNodes.add(h);
                spawnedEntities.add(h.entity());

                // 3 end rod starburst around each node
                for (int r = 0; r < 3; r++) {
                    double rAngle = (Math.PI * 2 * r) / 3;
                    Location rodLoc = loc.clone().add(Math.cos(rAngle) * 0.5, 0, Math.sin(rAngle) * 0.5);
                    BlockDisplayHandle rod = displayBuilder.spawnBlock(rodLoc, Material.END_ROD);
                    rod.scale(0.15f, 0.6f, 0.15f).glow(240, 240, 255).interpolation(2, 0);
                    spawnedEntities.add(rod.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Entire constellation rotates at 0.2 deg/tick
            float rot = ticksAlive * 0.0035f;

            // Update node positions with rotation and individual bob
            for (int i = 0; i < starNodes.size(); i++) {
                double t = i / 9.0;
                double baseX = (t - 0.5) * 20;
                double baseZ = Math.sin(t * Math.PI * 2) * 4;
                double baseY = 18 + (i % 3) * 3;

                // Apply rotation around center
                double rotX = baseX * Math.cos(rot) - baseZ * Math.sin(rot);
                double rotZ = baseX * Math.sin(rot) + baseZ * Math.cos(rot);

                // Individual bob
                int bobCycle = 50 + i * 5;
                float bob = 0.3f * (float) Math.sin(ticksAlive * (2 * Math.PI / bobCycle));

                Location loc = center.clone().add(rotX, baseY + bob, rotZ);
                starNodes.get(i).entity().teleport(loc);
            }

            // Laser lines between adjacent star nodes
            if (ticksAlive % 5 == 0) {
                for (int i = 0; i < starNodes.size() - 1; i++) {
                    Location from = starNodes.get(i).entity().getLocation();
                    Location to = starNodes.get(i + 1).entity().getLocation();
                    for (int p = 0; p < 4; p++) {
                        double t = p / 3.0;
                        Location particle = from.clone().add(
                                (to.getX() - from.getX()) * t,
                                (to.getY() - from.getY()) * t,
                                (to.getZ() - from.getZ()) * t
                        );
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particle, 1, 0, 0, 0, 0);
                    }
                }
            }

            // End rod particles from each node outward
            if (ticksAlive % 8 == 0) {
                for (BlockDisplayHandle h : starNodes) {
                    center.getWorld().spawnParticle(Particle.END_ROD,
                            h.entity().getLocation(), 1, 0.3, 0.3, 0.3, 0.01);
                }
            }

            // Yellow-white dust orbiting nodes
            if (ticksAlive % 6 == 0) {
                int idx = (ticksAlive / 6) % starNodes.size();
                DisplayBuilder.cyanDust(starNodes.get(idx).entity().getLocation(), 3, 0.5);
            }

            // Sound every 160 ticks
            if (ticksAlive % 160 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ConstellationWeb(plugin); }
    }

    // ================================================================
    // 16. DESCENDING PRISM -- Diamond shape at Y+18 with rotating beams
    // ================================================================
    public static class DescendingPrism extends BlockDisplayAttack {

        private BlockDisplayHandle topVertex;
        private BlockDisplayHandle bottomVertex;
        private final List<BlockDisplayHandle> widestRing = new ArrayList<>();
        private final List<BlockDisplayHandle> endRodTips = new ArrayList<>();

        public DescendingPrism(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("descending_prism", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Top vertex at Y+18
            topVertex = displayBuilder.spawnBlock(center.clone().add(0, 18, 0), Material.AMETHYST_BLOCK);
            topVertex.scale(0.6f, 0.6f, 0.6f).glow(128, 0, 255).interpolation(2, 0);
            spawnedEntities.add(topVertex.entity());

            // Upper ring: 6 large amethyst clusters at Y+17
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                Location loc = center.clone().add(Math.cos(angle) * 1.0, 17, Math.sin(angle) * 1.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.5f, 0.6f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                spawnedEntities.add(h.entity());
            }

            // Widest ring at Y+16: 8 cyan stained glass octagon
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Location loc = center.clone().add(Math.cos(angle) * 1.8, 16, Math.sin(angle) * 1.8);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
                h.scale(0.7f, 0.5f, 0.7f).glow(0, 200, 255).interpolation(2, 0);
                widestRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // 8 end rods pointing outward at widest ring
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Location loc = center.clone().add(Math.cos(angle) * 2.5, 16, Math.sin(angle) * 2.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.END_ROD);
                h.scale(0.2f, 0.3f, 0.8f).glow(240, 240, 255).interpolation(2, 0);
                endRodTips.add(h);
                spawnedEntities.add(h.entity());
            }

            // Lower ring: 6 amethyst shard blocks at Y+15
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                Location loc = center.clone().add(Math.cos(angle) * 1.0, 15, Math.sin(angle) * 1.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.5f, 0.5f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
                spawnedEntities.add(h.entity());
            }

            // Bottom vertex: large amethyst cluster at Y+14
            bottomVertex = displayBuilder.spawnBlock(center.clone().add(0, 14, 0), Material.AMETHYST_CLUSTER);
            bottomVertex.scale(0.5f, 0.7f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
            spawnedEntities.add(bottomVertex.entity());

            // 3 internal sea lanterns
            for (int y = 15; y <= 17; y++) {
                BlockDisplayHandle sl = displayBuilder.spawnBlock(center.clone().add(0, y, 0), Material.SEA_LANTERN);
                sl.scale(0.4f, 0.4f, 0.4f).glow(0, 200, 255).interpolation(2, 0);
                spawnedEntities.add(sl.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Rotate at 1 deg/tick
            float rot = ticksAlive * 0.0175f;

            // Scale: 1.0 to 1.5 over 300 ticks, then snap back
            int scaleCycle = ticksAlive % 300;
            float scale = 1.0f + 0.5f * (scaleCycle / 300.0f);

            // Vertical oscillation: 1 block up and down, 150-tick cycle
            float yOsc = (float) Math.sin(ticksAlive * (2 * Math.PI / 150));

            // Update end rod tip positions (rotating beams)
            for (int i = 0; i < endRodTips.size(); i++) {
                double angle = (Math.PI * 2 * i) / 8 + rot;
                Location loc = center.clone().add(
                        Math.cos(angle) * 2.5 * scale, 16 + yOsc, Math.sin(angle) * 2.5 * scale);
                endRodTips.get(i).entity().teleport(loc);
            }

            // 8 rotating horizontal laser beams from end rod tips
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < endRodTips.size(); i++) {
                    Location tipLoc = endRodTips.get(i).entity().getLocation();
                    double angle = (Math.PI * 2 * i) / 8 + rot;
                    for (int p = 0; p < 4; p++) {
                        double dist = (p + 1) * 2.0;
                        Location beamPt = tipLoc.clone().add(
                                Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, beamPt, 1, 0, 0, 0, 0);
                    }
                }
            }

            // Cyan dust leaking from glass faces
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 16 + yOsc, 0), 5, 2.0);
            }

            // Portal column from bottom vertex downward
            if (ticksAlive % 4 == 0) {
                center.getWorld().spawnParticle(Particle.PORTAL,
                        center.clone().add(0, 14 + yOsc, 0), 3, 0.1, 1.0, 0.1, 0.1);
            }

            // Scale snap sound
            if (ticksAlive % 300 == 299) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 0.5f);
            }

            // Sound every 240 ticks
            if (ticksAlive % 240 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.4f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DescendingPrism(plugin); }
    }

    // ================================================================
    // 17. CROWN OF THORNS -- Hovering thorn ring at Y+14
    // ================================================================
    public static class CrownOfThorns extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ringBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> thornClusters = new ArrayList<>();
        private BlockDisplayHandle centerEye;
        private final List<BlockDisplayHandle> shadowDisc = new ArrayList<>();

        public CrownOfThorns(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crown_of_thorns", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(8.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Ring body: 20 dark prismarine in 10-block diameter circle
            for (int i = 0; i < 20; i++) {
                double angle = (Math.PI * 2 * i) / 20;
                Location loc = center.clone().add(Math.cos(angle) * 5.0, 14, Math.sin(angle) * 5.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.6f, 0.5f, 0.6f).glow(0, 200, 255).interpolation(2, 0);
                ringBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 20 outward-upward thorn clusters between ring blocks
            for (int i = 0; i < 20; i++) {
                double angle = (Math.PI * 2 * i) / 20 + (Math.PI / 20);
                Location loc = center.clone().add(Math.cos(angle) * 5.0, 14.5, Math.sin(angle) * 5.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.4f, 0.7f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
                thornClusters.add(h);
                spawnedEntities.add(h.entity());
            }

            // Center eye at Y+14 (sea lantern representing Eye of Ender)
            centerEye = displayBuilder.spawnBlock(center.clone().add(0, 14, 0), Material.SEA_LANTERN);
            centerEye.scale(0.8f, 0.8f, 0.8f).glow(0, 200, 255).interpolation(2, 0);
            spawnedEntities.add(centerEye.entity());

            // Shadow disc at Y+12: 12 amethyst shards
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                Location loc = center.clone().add(Math.cos(angle) * 4.0, 12, Math.sin(angle) * 4.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.3f, 0.1f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
                shadowDisc.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Ring rotates at 1.5 deg/tick
            float ringRot = ticksAlive * 0.0262f;

            for (int i = 0; i < ringBlocks.size(); i++) {
                double angle = (Math.PI * 2 * i) / 20 + ringRot;
                Location loc = center.clone().add(Math.cos(angle) * 5.0, 14, Math.sin(angle) * 5.0);
                ringBlocks.get(i).entity().teleport(loc);
            }

            // Thorn clusters rotate with ring and bob
            for (int i = 0; i < thornClusters.size(); i++) {
                double angle = (Math.PI * 2 * i) / 20 + (Math.PI / 20) + ringRot;
                float bob = 0.2f * (float) Math.sin((ticksAlive + i * 2) * (2 * Math.PI / 40));
                Location loc = center.clone().add(Math.cos(angle) * 5.0, 14.5 + bob, Math.sin(angle) * 5.0);
                thornClusters.get(i).entity().teleport(loc);
            }

            // Eye counter-rotates at 0.5 deg/tick
            if (centerEye != null) {
                BlockDisplay bd = centerEye.entity();
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.4f, -0.4f, -0.4f),
                        new AxisAngle4f(-ticksAlive * 0.00875f, 0, 1, 0),
                        new Vector3f(0.8f, 0.8f, 0.8f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(2);
            }

            // Converging laser beams from ring blocks to center (cycling)
            if (ticksAlive % 4 == 0) {
                Location eyeLoc = center.clone().add(0, 14, 0);
                for (int i = 0; i < 5; i++) {
                    int blockIdx = (ticksAlive / 4 + i * 4) % ringBlocks.size();
                    Location from = ringBlocks.get(blockIdx).entity().getLocation();
                    for (int p = 0; p < 4; p++) {
                        double t = p / 3.0;
                        Location particle = from.clone().add(
                                (eyeLoc.getX() - from.getX()) * t,
                                (eyeLoc.getY() - from.getY()) * t,
                                (eyeLoc.getZ() - from.getZ()) * t
                        );
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particle, 1, 0, 0, 0, 0);
                    }
                }
            }

            // Dragon breath from eye downward
            if (ticksAlive % 6 == 0) {
                center.getWorld().spawnParticle(Particle.DRAGON_BREATH,
                        center.clone().add(0, 14, 0), 3, 0.2, 0.5, 0.2, 0.02);
            }

            // Magenta/purple dust at ring
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.purpleDust(center.clone().add(0, 14, 0), 5, 5.0);
            }

            // Sound every 200 ticks
            if (ticksAlive % 200 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.3f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrownOfThorns(plugin); }
    }

    // ================================================================
    // 18. ASTRAL LENS -- Convex lens at Y+20 with downward beam
    // ================================================================
    public static class AstralLens extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> frontFace = new ArrayList<>();
        private final List<BlockDisplayHandle> backFace = new ArrayList<>();
        private final List<BlockDisplayHandle> perimeterFrame = new ArrayList<>();

        public AstralLens(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("astral_lens", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(8.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location lensCenter = center.clone().add(0, 20, 0);

            // Front face: 12 cyan + 4 light blue stained glass in concentric rings
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                double radius = 3.5;
                Location loc = lensCenter.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
                h.scale(0.8f, 0.3f, 0.8f).glow(0, 200, 255).interpolation(2, 0);
                frontFace.add(h);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4;
                Location loc = lensCenter.clone().add(Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIGHT_BLUE_STAINED_GLASS);
                h.scale(0.8f, 0.3f, 0.8f).glow(0, 200, 255).interpolation(2, 0);
                frontFace.add(h);
                spawnedEntities.add(h.entity());
            }

            // Back face: 12 light blue stained glass offset 0.5 blocks
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                Location loc = lensCenter.clone().add(Math.cos(angle) * 3.5, 0.5, Math.sin(angle) * 3.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIGHT_BLUE_STAINED_GLASS);
                h.scale(0.7f, 0.3f, 0.7f).glow(240, 240, 255).interpolation(2, 0);
                backFace.add(h);
                spawnedEntities.add(h.entity());
            }

            // Central "spyglass" -- sea lantern lens core
            BlockDisplayHandle core = displayBuilder.spawnBlock(lensCenter, Material.SEA_LANTERN);
            core.scale(1.2f, 0.8f, 1.2f).glow(0, 200, 255).interpolation(2, 0);
            spawnedEntities.add(core.entity());

            // 8 amethyst block frame at perimeter
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Location loc = lensCenter.clone().add(Math.cos(angle) * 4.0, 0.25, Math.sin(angle) * 4.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.6f, 0.5f, 0.6f).glow(128, 0, 255).interpolation(2, 0);
                perimeterFrame.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 clusters at compass points
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4;
                Location loc = lensCenter.clone().add(Math.cos(angle) * 4.0, 0.7, Math.sin(angle) * 4.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.4f, 0.6f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            Location lensCenter = center.clone().add(0, 20, 0);

            // Lens rotates slowly at 0.25 deg/tick
            float rot = ticksAlive * 0.00436f;

            // Downward laser beam from lens center to arena floor
            if (ticksAlive % 3 == 0) {
                // Beam sweeps as lens rotates
                double beamAngle = rot * 3; // Amplified sweep
                double beamOffsetX = Math.sin(beamAngle) * 3;
                double beamOffsetZ = Math.cos(beamAngle) * 3;

                for (int y = 0; y < 20; y++) {
                    double t = y / 19.0;
                    Location beamPt = lensCenter.clone().add(
                            beamOffsetX * t, -y, beamOffsetZ * t);
                    center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, beamPt, 1, 0.05, 0, 0.05, 0);
                }
            }

            // Cyan dust at perimeter in slow orbit
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.cyanDust(lensCenter, 4, 4.0);
            }

            // End rod particles from front face outward
            if (ticksAlive % 8 == 0) {
                for (int i = 0; i < frontFace.size(); i += 3) {
                    center.getWorld().spawnParticle(Particle.END_ROD,
                            frontFace.get(i).entity().getLocation(), 1, 0.2, 0.1, 0.2, 0.02);
                }
            }

            // Core scale pulse: 1.0 to 1.3, 80-tick cycle
            // (applied visually through particles since core is a single block)
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.cyanDust(lensCenter, 10, 1.5);
            }

            // Sound every 160 ticks
            if (ticksAlive % 160 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.4f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AstralLens(plugin); }
    }

    // ================================================================
    // 19. HANGING GARDEN OF SHARDS -- Stalactite garden at ceiling
    // ================================================================
    public static class HangingGardenOfShards extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> stalactites = new ArrayList<>();
        private final int[] stalactiteCycles;

        public HangingGardenOfShards(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hanging_garden_of_shards", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(4.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(280);
            stalactiteCycles = new int[30];
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 30 stalactites distributed across 16x16 ceiling area at Y+28-22
            for (int i = 0; i < 30; i++) {
                double x = (i % 6 - 2.5) * 2.5 + (i % 3) * 0.5;
                double z = (i / 6 - 2.5) * 2.5 + (i % 2) * 0.5;
                int segments;
                Material tipMat;
                float tipScale;

                if (i < 8) { // Large stalactites
                    segments = 4;
                    tipMat = Material.AMETHYST_CLUSTER;
                    tipScale = 0.6f;
                } else if (i < 20) { // Medium
                    segments = 3;
                    tipMat = Material.AMETHYST_CLUSTER;
                    tipScale = 0.5f;
                } else { // Small
                    segments = 2;
                    tipMat = Material.AMETHYST_CLUSTER;
                    tipScale = 0.4f;
                }

                stalactiteCycles[i] = 45 + (i * 2);

                for (int seg = 0; seg < segments; seg++) {
                    Material mat;
                    if (seg < segments - 1) {
                        mat = (i < 20) ? Material.AMETHYST_BLOCK : Material.AMETHYST_CLUSTER;
                    } else {
                        mat = tipMat;
                    }
                    Location loc = center.clone().add(x, 28 - seg * 1.5, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    float s = tipScale - (seg * 0.05f);
                    h.scale(s, 0.8f, s).glow(0, 200, 255).interpolation(2, 0);
                    stalactites.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // 8 sea lanterns at Y+27
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Location loc = center.clone().add(Math.cos(angle) * 5, 27, Math.sin(angle) * 5);
                BlockDisplayHandle sl = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                sl.scale(0.5f, 0.5f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
                spawnedEntities.add(sl.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Each stalactite oscillates on unique phase/cycle
            for (int i = 0; i < stalactites.size(); i++) {
                int cycle = stalactiteCycles[Math.min(i / 3, stalactiteCycles.length - 1)];
                float amplitude = 0.2f + (i % 5) * 0.06f;
                float yOff = amplitude * (float) Math.sin((ticksAlive + i * 7) * (2 * Math.PI / cycle));
                BlockDisplay bd = stalactites.get(i).entity();
                Location base = bd.getLocation();
                bd.teleport(base.clone().add(0, yOff, 0));
            }

            // End rod particles dripping from tips
            if (ticksAlive % 10 == 0) {
                int tipIdx = (ticksAlive / 10) % 15;
                // Approximate tip positions (every few blocks in stalactite list)
                int approxIdx = Math.min(tipIdx * 3 + 2, stalactites.size() - 1);
                center.getWorld().spawnParticle(Particle.END_ROD,
                        stalactites.get(approxIdx).entity().getLocation(), 1, 0, 0.3, 0, 0.01);
            }

            // Cyan dust flowing between adjacent tips
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 25, 0), 5, 6.0);
            }

            // Spore blossom particles filling ceiling zone
            if (ticksAlive % 4 == 0) {
                center.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR,
                        center.clone().add(0, 26, 0), 6, 7.0, 1.0, 7.0, 0);
            }

            // Sound every 80 ticks
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HangingGardenOfShards(plugin); }
    }

    // ================================================================
    // 20. ORBITAL WEAPON PLATFORM -- Circular platform at Y+16
    // ================================================================
    public static class OrbitalWeaponPlatform extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> platformRing = new ArrayList<>();
        private final List<BlockDisplayHandle> weaponBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> floatingCaps = new ArrayList<>();

        public OrbitalWeaponPlatform(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("orbital_weapon_platform", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(8.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Platform ring: 16 dark prismarine blocks in 8-block diameter circle
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16;
                Location loc = center.clone().add(Math.cos(angle) * 4, 16, Math.sin(angle) * 4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.8f, 0.4f, 0.8f).glow(0, 200, 255).interpolation(2, 0);
                platformRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // 8 weapon positions (alternating amethyst cluster and end rod = sword/trident stand-ins)
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Location loc = center.clone().add(Math.cos(angle) * 4, 16.5, Math.sin(angle) * 4);
                Material mat = (i % 2 == 0) ? Material.AMETHYST_CLUSTER : Material.END_ROD;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.3f, 1.2f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
                weaponBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Amethyst clusters between weapons
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8 + (Math.PI / 8);
                Location loc = center.clone().add(Math.cos(angle) * 4, 16.3, Math.sin(angle) * 4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.4f, 0.6f, 0.4f).glow(0, 200, 255).interpolation(2, 0);
                spawnedEntities.add(h.entity());
            }

            // Floating amethyst caps above each weapon
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Location loc = center.clone().add(Math.cos(angle) * 4, 18, Math.sin(angle) * 4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.4f, 0.4f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
                floatingCaps.add(h);
                spawnedEntities.add(h.entity());
            }

            // 8 end rods pointing downward from platform underside
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Location loc = center.clone().add(Math.cos(angle) * 3.5, 15, Math.sin(angle) * 3.5);
                BlockDisplayHandle rod = displayBuilder.spawnBlock(loc, Material.END_ROD);
                rod.scale(0.2f, 1.0f, 0.2f).glow(240, 240, 255).interpolation(2, 0);
                spawnedEntities.add(rod.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Platform rotates at 2 deg/tick
            float platRot = ticksAlive * 0.035f;
            // Bobs 0.5 blocks on 120-tick cycle
            float platBob = 0.5f * (float) Math.sin(ticksAlive * (2 * Math.PI / 120));

            // Update platform ring positions
            for (int i = 0; i < platformRing.size(); i++) {
                double angle = (Math.PI * 2 * i) / 16 + platRot;
                Location loc = center.clone().add(Math.cos(angle) * 4, 16 + platBob, Math.sin(angle) * 4);
                platformRing.get(i).entity().teleport(loc);
            }

            // Weapons oscillate with staggered bob
            for (int i = 0; i < weaponBlocks.size(); i++) {
                double angle = (Math.PI * 2 * i) / 8 + platRot;
                float wBob = 0.3f * (float) Math.sin((ticksAlive + i * 4) * (2 * Math.PI / 30));
                Location loc = center.clone().add(Math.cos(angle) * 4, 16.5 + platBob + wBob, Math.sin(angle) * 4);
                weaponBlocks.get(i).entity().teleport(loc);
            }

            // Floating caps counter-rotate at 1 deg/tick
            float capRot = -ticksAlive * 0.0175f;
            for (int i = 0; i < floatingCaps.size(); i++) {
                double angle = (Math.PI * 2 * i) / 8 + capRot;
                Location loc = center.clone().add(Math.cos(angle) * 4, 18 + platBob, Math.sin(angle) * 4);
                floatingCaps.get(i).entity().teleport(loc);
            }

            // Laser from each weapon upward to cap
            if (ticksAlive % 5 == 0) {
                for (int i = 0; i < weaponBlocks.size(); i++) {
                    Location wLoc = weaponBlocks.get(i).entity().getLocation();
                    Location cLoc = floatingCaps.get(i).entity().getLocation();
                    for (int p = 0; p < 3; p++) {
                        double t = p / 2.0;
                        Location particle = wLoc.clone().add(
                                (cLoc.getX() - wLoc.getX()) * t,
                                (cLoc.getY() - wLoc.getY()) * t,
                                (cLoc.getZ() - wLoc.getZ()) * t
                        );
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particle, 1, 0, 0, 0, 0);
                    }
                }
            }

            // Converging downward beams from weapons through center hole
            if (ticksAlive % 6 == 0) {
                Location convergence = center.clone().add(0, 8 + platBob, 0);
                for (int i = 0; i < 4; i++) {
                    Location wLoc = weaponBlocks.get(i * 2).entity().getLocation();
                    for (int p = 0; p < 4; p++) {
                        double t = p / 3.0;
                        Location particle = wLoc.clone().add(
                                (convergence.getX() - wLoc.getX()) * t,
                                (convergence.getY() - wLoc.getY()) * t,
                                (convergence.getZ() - wLoc.getZ()) * t
                        );
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particle, 1, 0, 0, 0, 0);
                    }
                }

                // Convergence point radial burst every 40 ticks
                if (ticksAlive % 40 == 0) {
                    center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, convergence, 15, 1.5, 1.5, 1.5, 0.05);
                }
            }

            // Cyan dust orbiting platform ring
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 16 + platBob, 0), 5, 4.0);
            }

            // Sound every 120 ticks
            if (ticksAlive % 120 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new OrbitalWeaponPlatform(plugin); }
    }
}
