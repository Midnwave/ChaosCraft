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
 * Phase 2 (DoG) Block Display -- GROUP 4: LASER WEAPON ARRAYS
 * 10 structures: rotating weapon formations with laser beam hazards.
 * Devourer of Gods palette: amethyst, cyan, dark prismarine, polished blackstone.
 *
 * Design rules:
 * - NO status effects
 * - Always spawn straight (yaw=0, pitch=0)
 * - DoG glow colors: cyan(0,200,255), violet(128,0,255), white(240,240,255)
 * - All weapons represented as block display stand-ins (end rods, amethyst clusters)
 */
public final class LaserArrays {

    private LaserArrays() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new SwordCompass(plugin));
        registry.register(new TridentCage(plugin));
        registry.register(new BladeHorizon(plugin));
        registry.register(new PhalanxWall(plugin));
        registry.register(new RadiantCrossbowBattery(plugin));
        registry.register(new OrbitalBladeRing(plugin));
        registry.register(new ThroneOfBlades(plugin));
        registry.register(new SpearWall(plugin));
        registry.register(new CrystalGunshipBattery(plugin));
        registry.register(new SentinelArray(plugin));
    }

    // ================================================================
    // 26. SWORD COMPASS -- 8 radial blades at Y+2
    // ================================================================
    public static class SwordCompass extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> swordBlades = new ArrayList<>();
        private final List<BlockDisplayHandle> endRodMarkers = new ArrayList<>();
        private BlockDisplayHandle centerBlock;

        public SwordCompass(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sword_compass", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Center amethyst block
            centerBlock = displayBuilder.spawnBlock(center.clone().add(0, 2, 0), Material.AMETHYST_BLOCK);
            centerBlock.scale(0.8f, 0.8f, 0.8f).glow(0, 200, 255).interpolation(2, 0);
            spawnedEntities.add(centerBlock.entity());

            // 8 sword blades (end rods as blade stand-ins) radiating outward
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                for (int seg = 1; seg <= 3; seg++) {
                    double dist = seg * 0.8;
                    Location loc = center.clone().add(Math.cos(angle) * dist, 2.1, Math.sin(angle) * dist);
                    Material mat = seg < 3 ? Material.END_ROD : Material.AMETHYST_CLUSTER;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(0.2f, 0.2f, 0.8f).glow(0, 200, 255).interpolation(2, 0);
                    swordBlades.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // 8 end rod markers between swords
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8 + (Math.PI / 8);
                Location loc = center.clone().add(Math.cos(angle) * 1.5, 2, Math.sin(angle) * 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.END_ROD);
                h.scale(0.15f, 1.0f, 0.15f).glow(240, 240, 255).interpolation(2, 0);
                endRodMarkers.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Entire compass rotates at 1 deg/tick
            float rot = ticksAlive * 0.0175f;

            // Scale pulse: 1.0 to 1.1, 90-tick cycle
            float scale = 1.0f + 0.1f * (float) Math.sin(ticksAlive * (2 * Math.PI / 90));

            // Update sword blade positions
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8 + rot;
                float bob = 0.1f * (float) Math.sin((ticksAlive + i * 4) * (2 * Math.PI / 35));
                for (int seg = 0; seg < 3; seg++) {
                    int idx = i * 3 + seg;
                    if (idx < swordBlades.size()) {
                        double dist = (seg + 1) * 0.8 * scale;
                        Location loc = center.clone().add(
                                Math.cos(angle) * dist, 2.1 + bob, Math.sin(angle) * dist);
                        swordBlades.get(idx).entity().teleport(loc);
                    }
                }
            }

            // 8 laser beams radiating outward from blade tips, rotating
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 * i) / 8 + rot;
                    for (int p = 0; p < 5; p++) {
                        double dist = 2.5 + p * 1.0;
                        Location beamPt = center.clone().add(Math.cos(angle) * dist, 2.1, Math.sin(angle) * dist);
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, beamPt, 1, 0, 0, 0, 0);
                    }
                }
            }

            // Cyan dust orbiting at 4-block radius
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 5, 4.0);
            }

            // End rod particles from blade tips
            if (ticksAlive % 8 == 0) {
                for (int i = 0; i < 8; i++) {
                    int tipIdx = i * 3 + 2;
                    if (tipIdx < swordBlades.size()) {
                        center.getWorld().spawnParticle(Particle.END_ROD,
                                swordBlades.get(tipIdx).entity().getLocation(), 1, 0.05, 0.1, 0.05, 0.02);
                    }
                }
            }

            // Sound every 80 ticks
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SwordCompass(plugin); }
    }

    // ================================================================
    // 27. TRIDENT CAGE -- Spherical cage of converging spikes at Y+3
    // ================================================================
    public static class TridentCage extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> tridentSpikes = new ArrayList<>();
        private final List<BlockDisplayHandle> cageBars = new ArrayList<>();
        private BlockDisplayHandle centerLantern;

        public TridentCage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("trident_cage", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location cageCenter = center.clone().add(0, 3, 0);

            // 12 trident spikes (end rods) in icosahedral approximation pointing inward
            double[][] positions = {
                    {0, 2, 0}, {0, -2, 0},
                    {1.6, 1, 0}, {-1.6, 1, 0}, {0, 1, 1.6}, {0, 1, -1.6},
                    {1.6, -1, 0}, {-1.6, -1, 0}, {0, -1, 1.6}, {0, -1, -1.6},
                    {1, 0, 1.2}, {-1, 0, -1.2}
            };
            for (double[] pos : positions) {
                Location loc = cageCenter.clone().add(pos[0], pos[1], pos[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.END_ROD);
                h.scale(0.2f, 1.2f, 0.2f).glow(240, 240, 255).interpolation(2, 0);
                tridentSpikes.add(h);
                spawnedEntities.add(h.entity());
            }

            // Dark prismarine cage bars between adjacent spikes
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Location loc = cageCenter.clone().add(Math.cos(angle) * 1.8, 0, Math.sin(angle) * 1.8);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.3f, 0.3f, 0.3f).glow(0, 200, 255).interpolation(2, 0);
                cageBars.add(h);
                spawnedEntities.add(h.entity());
            }

            // Center sea lantern
            centerLantern = displayBuilder.spawnBlock(cageCenter, Material.SEA_LANTERN);
            centerLantern.scale(0.5f, 0.5f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
            spawnedEntities.add(centerLantern.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            Location cageCenter = center.clone().add(0, 3, 0);

            // Cage rotates around vertical at 0.8 deg/tick, horizontal X at 0.3 deg/tick
            float yRot = ticksAlive * 0.014f;
            float xRot = ticksAlive * 0.00524f;

            // Scale breathes: 1.0 to 1.2, 100-tick cycle
            float breathScale = 1.0f + 0.2f * (float) Math.sin(ticksAlive * (2 * Math.PI / 100));

            // Rotate cage bars
            for (int i = 0; i < cageBars.size(); i++) {
                double baseAngle = (Math.PI * 2 * i) / 8 + yRot;
                Location loc = cageCenter.clone().add(
                        Math.cos(baseAngle) * 1.8 * breathScale, 0, Math.sin(baseAngle) * 1.8 * breathScale);
                cageBars.get(i).entity().teleport(loc);
            }

            // Laser beams from each spike to center
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle spike : tridentSpikes) {
                    Location from = spike.entity().getLocation();
                    for (int p = 0; p < 3; p++) {
                        double t = p / 2.0;
                        Location particle = from.clone().add(
                                (cageCenter.getX() - from.getX()) * t,
                                (cageCenter.getY() - from.getY()) * t,
                                (cageCenter.getZ() - from.getZ()) * t
                        );
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particle, 1, 0, 0, 0, 0);
                    }
                }
            }

            // Scale burst at maximum: electric spark outward
            if (ticksAlive % 100 == 50) {
                center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                        cageCenter, 20, 3.0, 3.0, 3.0, 0.08);
            }

            // Cyan dust orbiting cage
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.cyanDust(cageCenter, 4, 5.0);
            }

            // Sound every 100 ticks
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentCage(plugin); }
    }

    // ================================================================
    // 28. BLADE HORIZON -- Floor-level rotating sword ring at Y+1
    // ================================================================
    public static class BladeHorizon extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> swordRing = new ArrayList<>();
        private final List<BlockDisplayHandle> shardRing = new ArrayList<>();
        private BlockDisplayHandle centerMount;

        public BladeHorizon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blade_horizon", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 16 swords (end rods) in horizontal ring at Y+1, 8-block diameter
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16;
                Location loc = center.clone().add(Math.cos(angle) * 4, 1, Math.sin(angle) * 4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.END_ROD);
                h.scale(0.15f, 0.15f, 1.5f).glow(240, 240, 255).interpolation(2, 0);
                swordRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // 16 amethyst shards at floor level between swords
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16 + (Math.PI / 16);
                Location loc = center.clone().add(Math.cos(angle) * 4, 0, Math.sin(angle) * 4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.4f, 0.3f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
                shardRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Center mount: 2x2 polished blackstone
            centerMount = displayBuilder.spawnBlock(center, Material.POLISHED_BLACKSTONE);
            centerMount.scale(2.0f, 1.0f, 2.0f).glow(80, 80, 100).interpolation(2, 0);
            spawnedEntities.add(centerMount.entity());

            // 4 end rods above center pointing outward
            double[][] rodOffsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (double[] off : rodOffsets) {
                BlockDisplayHandle rod = displayBuilder.spawnBlock(
                        center.clone().add(off[0] * 0.5, 2, off[1] * 0.5), Material.END_ROD);
                rod.scale(0.8f, 0.2f, 0.2f).glow(240, 240, 255).interpolation(2, 0);
                spawnedEntities.add(rod.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Sword ring rotates at 2 deg/tick
            float swordRot = ticksAlive * 0.035f;
            // Shard ring counter-rotates at 1 deg/tick
            float shardRot = -ticksAlive * 0.0175f;

            // Update sword positions with wave height
            for (int i = 0; i < swordRing.size(); i++) {
                double angle = (Math.PI * 2 * i) / 16 + swordRot;
                float wave = 0.2f * (float) Math.sin((ticksAlive + i * 3) * (2 * Math.PI / 50));
                Location loc = center.clone().add(Math.cos(angle) * 4, 1 + wave, Math.sin(angle) * 4);
                swordRing.get(i).entity().teleport(loc);
            }

            // Update shard ring
            for (int i = 0; i < shardRing.size(); i++) {
                double angle = (Math.PI * 2 * i) / 16 + (Math.PI / 16) + shardRot;
                Location loc = center.clone().add(Math.cos(angle) * 4, 0, Math.sin(angle) * 4);
                shardRing.get(i).entity().teleport(loc);
            }

            // 16 converging laser beams from sword tips to center
            if (ticksAlive % 4 == 0) {
                Location centerPt = center.clone().add(0, 1, 0);
                for (int i = 0; i < 4; i++) {
                    int idx = (ticksAlive / 4 + i * 4) % swordRing.size();
                    Location from = swordRing.get(idx).entity().getLocation();
                    for (int p = 0; p < 4; p++) {
                        double t = p / 3.0;
                        Location particle = from.clone().add(
                                (centerPt.getX() - from.getX()) * t,
                                (centerPt.getY() - from.getY()) * t,
                                (centerPt.getZ() - from.getZ()) * t
                        );
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particle, 1, 0, 0, 0, 0);
                    }
                }
            }

            // Convergence point pulse every 20 ticks
            if (ticksAlive % 20 == 0) {
                center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                        center.clone().add(0, 1, 0), 10, 1.5, 0.5, 1.5, 0.05);
            }

            // Cyan-green dust along shard ring
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 0, 0), 4, 4.0);
            }

            // Sound every 120 ticks
            if (ticksAlive % 120 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BladeHorizon(plugin); }
    }

    // ================================================================
    // 29. PHALANX WALL -- Shield wall formation that advances
    // ================================================================
    public static class PhalanxWall extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> shieldBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> swordBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> backingBlocks = new ArrayList<>();
        private float advanceOffset = 0;

        public PhalanxWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phalanx_wall", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(8.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 6x3 shield grid (polished blackstone as shields)
            for (int x = 0; x < 6; x++) {
                for (int y = 0; y < 3; y++) {
                    Location loc = center.clone().add((x - 2.5) * 1.5, y * 0.7, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                    h.scale(1.2f, 0.6f, 0.3f).glow(80, 80, 100).interpolation(2, 0);
                    shieldBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // End rod swords between shields
            for (int x = 0; x < 5; x++) {
                for (int y = 0; y < 3; y++) {
                    Location loc = center.clone().add((x - 2) * 1.5, y * 0.7, 0.3);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.END_ROD);
                    h.scale(0.1f, 0.1f, 0.8f).glow(240, 240, 255).interpolation(2, 0);
                    swordBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Backing: dark prismarine structural support
            for (int x = 0; x < 6; x++) {
                for (int y = 0; y < 3; y++) {
                    Location loc = center.clone().add((x - 2.5) * 1.5, y * 0.7, -0.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                    h.scale(1.2f, 0.6f, 0.3f).glow(0, 200, 255).interpolation(2, 0);
                    backingBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Corner tridents (end rods pointing diagonally up)
            for (int side = -1; side <= 1; side += 2) {
                Location loc = center.clone().add(side * 4, 2, 0);
                BlockDisplayHandle rod = displayBuilder.spawnBlock(loc, Material.END_ROD);
                rod.scale(0.2f, 1.5f, 0.2f).glow(240, 240, 255).interpolation(2, 0);
                spawnedEntities.add(rod.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Wall oscillates toward player area: 0.2 block amplitude, 200-tick cycle
            advanceOffset = 0.2f * (float) Math.sin(ticksAlive * (2 * Math.PI / 200));

            // Individual shields oscillate 5 degrees on staggered 40-tick cycles
            for (int i = 0; i < shieldBlocks.size(); i++) {
                float wobble = 0.087f * (float) Math.sin((ticksAlive + i * 5) * (2 * Math.PI / 40));
                BlockDisplay bd = shieldBlocks.get(i).entity();
                int x = i / 3;
                int y = i % 3;
                Location loc = center.clone().add((x - 2.5) * 1.5, y * 0.7, advanceOffset);
                bd.teleport(loc);
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.6f, -0.3f, -0.15f),
                        new AxisAngle4f(wobble, 0, 1, 0),
                        new Vector3f(1.2f, 0.6f, 0.3f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(2);
            }

            // 3 horizontal laser lines at Y+0.5, Y+1.0, Y+1.5
            if (ticksAlive % 4 == 0) {
                float[] laserYs = {0.35f, 0.7f, 1.05f};
                for (float ly : laserYs) {
                    for (int p = 0; p < 8; p++) {
                        double x = (p - 3.5) * 1.2;
                        Location beamPt = center.clone().add(x, ly, advanceOffset + 0.3);
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, beamPt, 1, 0, 0, 0, 0);
                    }
                }
            }

            // Blue dust drifting from shield faces
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 1, advanceOffset + 0.5), 5, 4.0);
            }

            // Sound every 200 ticks
            if (ticksAlive % 200 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PhalanxWall(plugin); }
    }

    // ================================================================
    // 30. RADIANT CROSSBOW BATTERY -- Tiered firing platform
    // ================================================================
    public static class RadiantCrossbowBattery extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> towerBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> crossbows = new ArrayList<>();
        private float aimAngle = 0;
        private boolean aimingRight = true;

        public RadiantCrossbowBattery(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("radiant_crossbow_battery", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 3-block tower: 2x2 of amethyst + dark prismarine
            for (int y = 0; y < 3; y++) {
                for (int x = 0; x <= 1; x++) {
                    for (int z = 0; z <= 1; z++) {
                        Material mat = ((x + y + z) % 2 == 0) ? Material.AMETHYST_BLOCK : Material.DARK_PRISMARINE;
                        Location loc = center.clone().add(x - 0.5, y, z - 0.5);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                        h.scale(0.9f, 0.9f, 0.9f).glow(0, 200, 255).interpolation(2, 0);
                        towerBlocks.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // Top platform: 4x4 prismarine
            for (int x = -1; x <= 2; x++) {
                for (int z = -1; z <= 2; z++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x - 0.5, 3, z - 0.5), Material.PRISMARINE);
                    h.scale(0.9f, 0.3f, 0.9f).glow(0, 200, 255).interpolation(2, 0);
                    spawnedEntities.add(h.entity());
                }
            }

            // 9 crossbows (end rods pointing forward) in 3x3 grid on platform
            for (int x = 0; x < 3; x++) {
                for (int z = 0; z < 3; z++) {
                    Location loc = center.clone().add(x - 1, 3.5, z - 1);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.END_ROD);
                    h.scale(0.2f, 0.2f, 1.0f).glow(240, 240, 255).interpolation(2, 0);
                    crossbows.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Flanking trident pillars (large amethyst clusters as stand-ins)
            for (int side = -1; side <= 1; side += 2) {
                BlockDisplayHandle trident = displayBuilder.spawnBlock(
                        center.clone().add(side * 2, 2, 0), Material.AMETHYST_CLUSTER);
                trident.scale(0.4f, 1.5f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
                spawnedEntities.add(trident.entity());
            }

            // Platform edge end rods
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4;
                Location loc = center.clone().add(Math.cos(angle) * 1.8, 3.3, Math.sin(angle) * 1.8);
                BlockDisplayHandle rod = displayBuilder.spawnBlock(loc, Material.END_ROD);
                rod.scale(0.15f, 0.15f, 0.5f).glow(240, 240, 255).interpolation(2, 0);
                spawnedEntities.add(rod.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Battery swivels: 90 degrees left, pause, 90 right, pause
            int cyclePos = ticksAlive % 300;
            if (cyclePos < 90) {
                aimAngle = (cyclePos / 90.0f) * (float) (Math.PI / 2);
            } else if (cyclePos < 150) {
                // Pause at right
            } else if (cyclePos < 240) {
                aimAngle = ((240 - cyclePos) / 90.0f) * (float) (Math.PI / 2);
            }
            // Pause at center for remainder

            // Individual crossbow loading pulse
            for (int i = 0; i < crossbows.size(); i++) {
                float pulse = 1.0f + 0.15f * (float) Math.sin((ticksAlive + i * 3) * (2 * Math.PI / 25));
                crossbows.get(i).scale(0.2f * pulse, 0.2f * pulse, 1.0f);
                crossbows.get(i).interpolation(2, 0);
            }

            // 9 parallel laser beams in aimed direction
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < crossbows.size(); i++) {
                    Location from = crossbows.get(i).entity().getLocation();
                    for (int p = 0; p < 4; p++) {
                        double dist = (p + 1) * 2.0;
                        Location beamPt = from.clone().add(
                                Math.sin(aimAngle) * dist, 0, Math.cos(aimAngle) * dist);
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, beamPt, 1, 0, 0, 0, 0);
                    }
                }
            }

            // Amber dust from muzzles
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 3.5, 0), 4, 1.5);
            }

            // End rod particle trails
            if (ticksAlive % 8 == 0) {
                center.getWorld().spawnParticle(Particle.END_ROD,
                        center.clone().add(0, 3.5, 0), 2, 1.5, 0.1, 1.5, 0.02);
            }

            // Sound every 160 ticks
            if (ticksAlive % 160 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RadiantCrossbowBattery(plugin); }
    }

    // ================================================================
    // 31. ORBITAL BLADE RING -- 24 swords at Y+4, fastest rotation
    // ================================================================
    public static class OrbitalBladeRing extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> swords = new ArrayList<>();
        private final List<BlockDisplayHandle> endRodFence = new ArrayList<>();
        private BlockDisplayHandle centerCluster;

        public OrbitalBladeRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("orbital_blade_ring", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 24 swords (end rods) in horizontal ring, radius 5 blocks, at Y+4
            for (int i = 0; i < 24; i++) {
                double angle = (Math.PI * 2 * i) / 24;
                Location loc = center.clone().add(Math.cos(angle) * 5, 4, Math.sin(angle) * 5);
                Material mat = (i % 6 == 0) ? Material.AMETHYST_CLUSTER : Material.END_ROD;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                if (i % 6 == 0) {
                    h.scale(0.4f, 1.0f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
                } else {
                    h.scale(0.15f, 0.8f, 0.15f).glow(240, 240, 255).interpolation(2, 0);
                }
                swords.add(h);
                spawnedEntities.add(h.entity());
            }

            // 24 end rod fence posts between swords
            for (int i = 0; i < 24; i++) {
                double angle = (Math.PI * 2 * i) / 24 + (Math.PI / 24);
                Location loc = center.clone().add(Math.cos(angle) * 5, 4, Math.sin(angle) * 5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.END_ROD);
                h.scale(0.1f, 2.0f, 0.1f).glow(240, 240, 255).interpolation(2, 0);
                endRodFence.add(h);
                spawnedEntities.add(h.entity());
            }

            // Center: large amethyst cluster at Y+4
            centerCluster = displayBuilder.spawnBlock(center.clone().add(0, 4, 0), Material.AMETHYST_CLUSTER);
            centerCluster.scale(1.5f, 1.5f, 1.5f).glow(128, 0, 255).interpolation(2, 0);
            spawnedEntities.add(centerCluster.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Ring rotates at 3 deg/tick -- FAST
            float rot = ticksAlive * 0.0524f;

            for (int i = 0; i < swords.size(); i++) {
                double angle = (Math.PI * 2 * i) / 24 + rot;
                // Radial in/out oscillation: 0.3 blocks, staggered 20-tick
                float radOff = 0.3f * (float) Math.sin((ticksAlive + i * 2) * (2 * Math.PI / 20));
                Location loc = center.clone().add(Math.cos(angle) * (5 + radOff), 4, Math.sin(angle) * (5 + radOff));
                swords.get(i).entity().teleport(loc);

                // Enchanted swords (every 6th) flare scale
                if (i % 6 == 0) {
                    float flare = 1.0f + 0.5f * (float) Math.sin(ticksAlive * (2 * Math.PI / 40));
                    swords.get(i).scale(0.4f * flare, 1.0f * flare, 0.4f * flare);
                    swords.get(i).interpolation(2, 0);
                }
            }

            // Update end rod fence positions
            for (int i = 0; i < endRodFence.size(); i++) {
                double angle = (Math.PI * 2 * i) / 24 + (Math.PI / 24) + rot;
                Location loc = center.clone().add(Math.cos(angle) * 5, 4, Math.sin(angle) * 5);
                endRodFence.get(i).entity().teleport(loc);
            }

            // 4 enchanted sword laser beams radiating outward
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 4; i++) {
                    int swordIdx = i * 6;
                    if (swordIdx < swords.size()) {
                        Location from = swords.get(swordIdx).entity().getLocation();
                        double angle = (Math.PI * 2 * swordIdx) / 24 + rot;
                        for (int p = 0; p < 4; p++) {
                            double dist = 1.0 + p * 1.0;
                            Location beamPt = from.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                            center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, beamPt, 1, 0, 0, 0, 0);
                        }
                    }
                }
            }

            // Cyan dust along ring path
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 4, 0), 5, 5.0);
            }

            // Electric spark arcs between adjacent blade tips (crackling rim)
            if (ticksAlive % 6 == 0) {
                int idx = (ticksAlive / 6) % swords.size();
                int next = (idx + 1) % swords.size();
                Location from = swords.get(idx).entity().getLocation();
                Location to = swords.get(next).entity().getLocation();
                center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                        from.clone().add((to.getX() - from.getX()) * 0.5, 0, (to.getZ() - from.getZ()) * 0.5),
                        2, 0.1, 0.1, 0.1, 0);
            }

            // Sound every 60 ticks
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new OrbitalBladeRing(plugin); }
    }

    // ================================================================
    // 32. THRONE OF BLADES -- Static throne set piece with weapon crown
    // ================================================================
    public static class ThroneOfBlades extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> weaponDisplay = new ArrayList<>();
        private final List<BlockDisplayHandle> cornerClusters = new ArrayList<>();

        public ThroneOfBlades(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("throne_of_blades", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Seat: 3x3 polished blackstone, 2 blocks tall
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 0; z++) {
                    for (int y = 0; y < 2; y++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(x, y, z), Material.POLISHED_BLACKSTONE);
                        h.scale(0.9f, 0.9f, 0.9f).glow(80, 80, 100).interpolation(2, 0);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // Backrest: 3 wide x 4 tall of dark prismarine
            for (int x = -1; x <= 1; x++) {
                for (int y = 0; y < 4; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, y, -1.5), Material.DARK_PRISMARINE);
                    h.scale(0.9f, 0.9f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
                    spawnedEntities.add(h.entity());
                }
            }

            // Crown: 3 crying obsidian at top of backrest
            for (int x = -1; x <= 1; x++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, 4, -1.5), Material.CRYING_OBSIDIAN);
                h.scale(0.9f, 0.7f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                spawnedEntities.add(h.entity());
            }

            // Armrests: dark prismarine columns
            for (int side = -1; side <= 1; side += 2) {
                for (int y = 0; y < 3; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(side * 2, y, -0.5), Material.DARK_PRISMARINE);
                    h.scale(0.5f, 0.9f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
                    spawnedEntities.add(h.entity());
                }
            }

            // Weapons adorning backrest: 16 end rods + 4 large amethyst clusters in sunburst
            for (int i = 0; i < 20; i++) {
                double angle = (Math.PI * i) / 19 - (Math.PI / 2);
                double x = Math.cos(angle) * 2.0;
                double y = 2.5 + Math.sin(angle) * 2.0;
                Location loc = center.clone().add(x, y, -1.8);
                Material mat = (i % 5 == 0) ? Material.AMETHYST_CLUSTER : Material.END_ROD;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                if (i % 5 == 0) {
                    h.scale(0.3f, 0.8f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
                } else {
                    h.scale(0.1f, 0.6f, 0.1f).glow(240, 240, 255).interpolation(2, 0);
                }
                weaponDisplay.add(h);
                spawnedEntities.add(h.entity());
            }

            // Corner amethyst clusters
            double[][] corners = {{-1.5, 0, 0.5}, {1.5, 0, 0.5}, {-1.5, 0, -2}, {1.5, 0, -2}};
            for (double[] c : corners) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(c[0], c[1], c[2]), Material.AMETHYST_CLUSTER);
                h.scale(0.5f, 0.6f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                cornerClusters.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Weapons very subtle oscillation: 0.05 block amplitude, 150-tick cycle
            for (int i = 0; i < weaponDisplay.size(); i++) {
                float bob = 0.05f * (float) Math.sin((ticksAlive + i * 8) * (2 * Math.PI / 150));
                BlockDisplay bd = weaponDisplay.get(i).entity();
                Location base = bd.getLocation();
                bd.teleport(base.clone().add(0, bob, 0));
            }

            // Corner clusters scale pulse: 0.95 to 1.05, 80-tick cycle
            for (int i = 0; i < cornerClusters.size(); i++) {
                float scale = 0.5f + 0.05f * (float) Math.sin((ticksAlive + i * 20) * (2 * Math.PI / 80));
                cornerClusters.get(i).scale(scale, 0.6f, scale);
                cornerClusters.get(i).interpolation(2, 0);
            }

            // Electric spark crown arcs along backrest weapons
            if (ticksAlive % 6 == 0 && weaponDisplay.size() >= 2) {
                int idx = (ticksAlive / 6) % (weaponDisplay.size() - 1);
                Location from = weaponDisplay.get(idx).entity().getLocation();
                Location to = weaponDisplay.get(idx + 1).entity().getLocation();
                for (int p = 0; p < 3; p++) {
                    double t = p / 2.0;
                    Location particle = from.clone().add(
                            (to.getX() - from.getX()) * t,
                            (to.getY() - from.getY()) * t,
                            0
                    );
                    center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particle, 1, 0, 0, 0, 0);
                }
            }

            // Dragon breath from seat
            if (ticksAlive % 6 == 0) {
                center.getWorld().spawnParticle(Particle.DRAGON_BREATH,
                        center.clone().add(0, 1, 0), 3, 0.5, 0.3, 0.5, 0.01);
            }

            // Portal particles from throne interior
            if (ticksAlive % 8 == 0) {
                center.getWorld().spawnParticle(Particle.PORTAL,
                        center.clone().add(0, 2, -1.5), 3, 0.5, 1.0, 0.2, 0.05);
            }

            // Deep purple dust from crying obsidian crown
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.purpleDust(center.clone().add(0, 4.5, -1.5), 3, 1.0);
            }

            // Crown burst every 60 ticks: 3 hearts radius
            if (ticksAlive % 60 == 0) {
                center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                        center.clone().add(0, 3, -1.5), 20, 3.0, 2.0, 1.0, 0.05);
            }

            // Sound every 240 ticks
            if (ticksAlive % 240 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.3f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ThroneOfBlades(plugin); }
    }

    // ================================================================
    // 33. SPEAR WALL -- Advancing trident formation
    // ================================================================
    public static class SpearWall extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> tridents = new ArrayList<>();
        private final List<BlockDisplayHandle> glassBacking = new ArrayList<>();
        private float chargeProgress = 0;

        public SpearWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spear_wall", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(8.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 20 tridents (end rods) in 5x4 grid
            for (int x = 0; x < 5; x++) {
                for (int y = 0; y < 4; y++) {
                    Location loc = center.clone().add((x - 2) * 1.2, y * 0.5, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.END_ROD);
                    h.scale(0.15f, 0.15f, 1.2f).glow(240, 240, 255).interpolation(2, 0);
                    tridents.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Cyan glass backing
            for (int x = 0; x < 5; x++) {
                for (int y = 0; y < 4; y++) {
                    Location loc = center.clone().add((x - 2) * 1.2, y * 0.5, -0.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
                    h.scale(1.0f, 0.4f, 0.15f).glow(0, 200, 255).interpolation(2, 0);
                    glassBacking.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Edge end rods
            for (int side = -1; side <= 1; side += 2) {
                for (int i = 0; i < 3; i++) {
                    Location loc = center.clone().add(side * 3, i * 0.5, 0.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.END_ROD);
                    h.scale(0.1f, 0.1f, 0.6f).glow(240, 240, 255).interpolation(2, 0);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Charge forward: 3 blocks over 180 ticks, then snap back
            int chargeCycle = ticksAlive % 180;
            chargeProgress = (chargeCycle / 180.0f) * 3.0f;

            // Update trident and glass positions
            for (int i = 0; i < tridents.size(); i++) {
                int x = i / 4;
                int y = i % 4;
                // Jitter: random forward oscillation
                float jitter = 0.05f * (float) Math.sin((ticksAlive + i * 7) * 1.0f);
                Location loc = center.clone().add((x - 2) * 1.2, y * 0.5, chargeProgress + jitter);
                tridents.get(i).entity().teleport(loc);
            }
            for (int i = 0; i < glassBacking.size(); i++) {
                int x = i / 4;
                int y = i % 4;
                Location loc = center.clone().add((x - 2) * 1.2, y * 0.5, chargeProgress - 0.5);
                glassBacking.get(i).entity().teleport(loc);
            }

            // 20 parallel forward beams from trident tips
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < tridents.size(); i += 4) {
                    Location from = tridents.get(i).entity().getLocation();
                    for (int p = 0; p < 4; p++) {
                        Location beamPt = from.clone().add(0, 0, (p + 1) * 1.0);
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, beamPt, 1, 0, 0, 0, 0);
                    }
                }
            }

            // Cyan dust blown backward from trident tips
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 1, chargeProgress + 1), 4, 2.5);
            }

            // End rod trail from forward motion
            if (ticksAlive % 4 == 0) {
                center.getWorld().spawnParticle(Particle.END_ROD,
                        center.clone().add(0, 1, chargeProgress), 2, 2.5, 0.8, 0.2, 0.01);
            }

            // Sound every 100 ticks
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpearWall(plugin); }
    }

    // ================================================================
    // 34. CRYSTAL GUNSHIP BATTERY -- Floating platform at Y+10
    // ================================================================
    public static class CrystalGunshipBattery extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> platformBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> forwardGuns = new ArrayList<>();
        private final List<BlockDisplayHandle> rearGuns = new ArrayList<>();

        public CrystalGunshipBattery(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_gunship_battery", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Platform body: 5x3 dark prismarine at Y+10
            for (int x = -2; x <= 2; x++) {
                for (int z = -1; z <= 1; z++) {
                    Location loc = center.clone().add(x, 10, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                    h.scale(0.9f, 0.3f, 0.9f).glow(0, 200, 255).interpolation(2, 0);
                    platformBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // 4 corner supports pointing down
            double[][] corners = {{-2, 10, -1}, {-2, 10, 1}, {2, 10, -1}, {2, 10, 1}};
            for (double[] c : corners) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(c[0], c[1] - 0.5, c[2]), Material.POLISHED_BLACKSTONE);
                h.scale(0.4f, 0.5f, 0.4f).glow(80, 80, 100).interpolation(2, 0);
                spawnedEntities.add(h.entity());
            }

            // 3 forward-pointing crossbows (end rods)
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(i - 1, 10.3, 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.END_ROD);
                h.scale(0.15f, 0.15f, 1.0f).glow(240, 240, 255).interpolation(2, 0);
                forwardGuns.add(h);
                spawnedEntities.add(h.entity());
            }

            // 3 backward-pointing crossbows
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(i - 1, 10.3, -1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.END_ROD);
                h.scale(0.15f, 0.15f, 1.0f).glow(240, 240, 255).interpolation(2, 0);
                rearGuns.add(h);
                spawnedEntities.add(h.entity());
            }

            // 2 large amethyst clusters on platform between gun rows
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add(i - 0.5, 10.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.5f, 1.0f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                spawnedEntities.add(h.entity());
            }

            // Underside end rods (landing gear)
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(corners[i][0], 9, corners[i][2]);
                BlockDisplayHandle rod = displayBuilder.spawnBlock(loc, Material.END_ROD);
                rod.scale(0.15f, 0.8f, 0.15f).glow(240, 240, 255).interpolation(2, 0);
                spawnedEntities.add(rod.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Platform circles at 0.3 deg/tick, 4-block radius
            float circleAngle = ticksAlive * 0.00524f;
            float circleX = (float) Math.cos(circleAngle) * 4;
            float circleZ = (float) Math.sin(circleAngle) * 4;
            // Bob vertically
            float bob = 0.5f * (float) Math.sin(ticksAlive * (2 * Math.PI / 120));

            Location platformCenter = center.clone().add(circleX, 10 + bob, circleZ);

            // Forward beams: triple laser
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < forwardGuns.size(); i++) {
                    Location from = platformCenter.clone().add(i - 1, 0.3, 1.5);
                    double beamAngle = circleAngle + Math.PI / 2;
                    for (int p = 0; p < 4; p++) {
                        double dist = (p + 1) * 2.0;
                        Location beamPt = from.clone().add(
                                Math.cos(beamAngle) * dist, 0, Math.sin(beamAngle) * dist);
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, beamPt, 1, 0, 0, 0, 0);
                    }
                }
            }

            // Rear beams: triple laser opposite direction
            if (ticksAlive % 4 == 2) {
                for (int i = 0; i < rearGuns.size(); i++) {
                    Location from = platformCenter.clone().add(i - 1, 0.3, -1.5);
                    double beamAngle = circleAngle - Math.PI / 2;
                    for (int p = 0; p < 4; p++) {
                        double dist = (p + 1) * 2.0;
                        Location beamPt = from.clone().add(
                                Math.cos(beamAngle) * dist, 0, Math.sin(beamAngle) * dist);
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, beamPt, 1, 0, 0, 0, 0);
                    }
                }
            }

            // Cyan dust falling from platform underside
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.cyanDust(platformCenter.clone().add(0, -1, 0), 4, 2.0);
            }

            // Portal from amethyst clusters
            if (ticksAlive % 6 == 0) {
                center.getWorld().spawnParticle(Particle.PORTAL,
                        platformCenter.clone().add(0, 0.5, 0), 2, 0.5, 0.2, 0.5, 0.05);
            }

            // Sound every 140 ticks
            if (ticksAlive % 140 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalGunshipBattery(plugin); }
    }

    // ================================================================
    // 35. SENTINEL ARRAY -- 6 turrets in hexagonal ring
    // ================================================================
    public static class SentinelArray extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> sentinelBases = new ArrayList<>();
        private final List<BlockDisplayHandle> sentinelGuns = new ArrayList<>();
        private BlockDisplayHandle centerColumn;

        public SentinelArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sentinel_array", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(8.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 6 sentinels in hexagonal ring, 10-block diameter
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                Location baseLoc = center.clone().add(Math.cos(angle) * 5, 0, Math.sin(angle) * 5);

                // Base: amethyst block 1x1x2
                BlockDisplayHandle base = displayBuilder.spawnBlock(baseLoc, Material.AMETHYST_BLOCK);
                base.scale(0.8f, 1.5f, 0.8f).glow(0, 200, 255).interpolation(2, 0);
                sentinelBases.add(base);
                spawnedEntities.add(base.entity());

                // Gun: end rod on top
                BlockDisplayHandle gun = displayBuilder.spawnBlock(baseLoc.clone().add(0, 2, 0), Material.END_ROD);
                gun.scale(0.3f, 0.3f, 1.3f).glow(240, 240, 255).interpolation(2, 0);
                sentinelGuns.add(gun);
                spawnedEntities.add(gun.entity());
            }

            // End rods between sentinels pointing inward
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6 + (Math.PI / 6);
                Location loc = center.clone().add(Math.cos(angle) * 4, 0.5, Math.sin(angle) * 4);
                BlockDisplayHandle rod = displayBuilder.spawnBlock(loc, Material.END_ROD);
                rod.scale(0.15f, 0.8f, 0.15f).glow(240, 240, 255).interpolation(2, 0);
                spawnedEntities.add(rod.entity());
            }

            // Center column: large amethyst cluster, 3 blocks tall, spinning
            centerColumn = displayBuilder.spawnBlock(center, Material.AMETHYST_CLUSTER);
            centerColumn.scale(0.8f, 3.0f, 0.8f).glow(128, 0, 255).interpolation(2, 0);
            spawnedEntities.add(centerColumn.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Sentinel guns rotate toward center at 1 deg/tick
            float gunRot = ticksAlive * 0.0175f;

            // Center column counter-rotates at 2 deg/tick
            if (centerColumn != null) {
                BlockDisplay bd = centerColumn.entity();
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.4f, -1.5f, -0.4f),
                        new AxisAngle4f(-ticksAlive * 0.035f, 0, 1, 0),
                        new Vector3f(0.8f, 3.0f, 0.8f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(2);
            }

            // Sentinel fire cycle: one fires every 60 ticks
            int firingIdx = (ticksAlive / 10) % 6;
            boolean isFiring = (ticksAlive % 60) < 10;

            if (isFiring && firingIdx < sentinelGuns.size()) {
                // Fire animation: scale up to 1.6x
                float fireScale = 1.0f + 0.6f * ((ticksAlive % 10) / 10.0f);
                sentinelGuns.get(firingIdx).scale(0.3f * fireScale, 0.3f * fireScale, 1.3f);
                sentinelGuns.get(firingIdx).interpolation(2, 0);

                // Firing beam pulse
                if (ticksAlive % 3 == 0) {
                    Location from = sentinelGuns.get(firingIdx).entity().getLocation();
                    for (int p = 0; p < 8; p++) {
                        double t = p / 7.0;
                        Location particle = from.clone().add(
                                (center.getX() - from.getX()) * t,
                                (center.getY() + 1.5 - from.getY()) * t,
                                (center.getZ() - from.getZ()) * t
                        );
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particle, 2, 0, 0, 0, 0);
                    }
                }
            }

            // Persistent inward laser beams from all 6 guns
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle gun : sentinelGuns) {
                    Location from = gun.entity().getLocation();
                    for (int p = 0; p < 4; p++) {
                        double t = p / 3.0;
                        Location particle = from.clone().add(
                                (center.getX() - from.getX()) * t,
                                (center.getY() + 1.5 - from.getY()) * t,
                                (center.getZ() - from.getZ()) * t
                        );
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particle, 1, 0, 0, 0, 0);
                    }
                }
            }

            // Cyan dust circling hexagon perimeter
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 5, 5.0);
            }

            // End rod particles inward from sentinels
            if (ticksAlive % 8 == 0) {
                for (BlockDisplayHandle base : sentinelBases) {
                    center.getWorld().spawnParticle(Particle.END_ROD,
                            base.entity().getLocation().add(0, 1.5, 0), 1, 0.1, 0.1, 0.1, 0.02);
                }
            }

            // Sound on fire cycle
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SentinelArray(plugin); }
    }
}
