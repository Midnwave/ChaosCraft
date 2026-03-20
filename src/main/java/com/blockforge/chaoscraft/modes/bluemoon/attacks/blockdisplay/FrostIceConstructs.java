package com.blockforge.chaoscraft.modes.bluemoon.attacks.blockdisplay;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import java.util.*;

/**
 * Blue Moon Block Display — GROUP: FROST & ICE CONSTRUCTS
 * 13 frost/ice-themed BlockDisplay attacks.
 * Blue Moon palette: pale blue (180,210,255), silver (200,200,220), moonlight white (240,240,255).
 * NO status effects. Min 10 BlockDisplays per attack.
 */
public final class FrostIceConstructs {

    private FrostIceConstructs() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new IceChandelier(plugin));
        registry.register(new FrostCage(plugin));
        registry.register(new CrystalSpire(plugin));
        registry.register(new GlacialSaw(plugin));
        registry.register(new SnowflakeMandala(plugin));
        registry.register(new IcicleBarrage(plugin));
        registry.register(new FrozenThrone(plugin));
        registry.register(new PermafrostPillars(plugin));
        registry.register(new HailstoneCluster(plugin));
        registry.register(new IceMirror(plugin));
        registry.register(new FrostHelix(plugin));
        registry.register(new CrystallineMeteor(plugin));
        registry.register(new AvalancheWall(plugin));
    }

    // ================================================================
    // 1. ICE CHANDELIER — Ornate hanging structure that drops shards
    // ================================================================
    public static class IceChandelier extends BlockDisplayAttack {

        private BlockDisplayHandle centerPiece;
        private final List<BlockDisplayHandle> arms = new ArrayList<>();
        private final List<BlockDisplayHandle> ornaments = new ArrayList<>();
        private final List<BlockDisplayHandle> shards = new ArrayList<>();
        private final List<Double> shardY = new ArrayList<>();
        private int nextShardIndex = 0;

        public IceChandelier(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_chandelier", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0);
            config.setImpactRadius(2.0);
            config.setDurationTicks(360);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double cy = 8.0;

            // Central hub
            centerPiece = displayBuilder.spawnBlock(center.clone().add(0, cy, 0), Material.BLUE_ICE);
            centerPiece.scale(1.5f, 1.5f, 1.5f).glow(180, 210, 255).interpolation(3, 0);
            spawnedEntities.add(centerPiece.entity());

            // 6 radial arms
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                double x = Math.cos(angle) * 2.5;
                double z = Math.sin(angle) * 2.5;
                BlockDisplayHandle arm = displayBuilder.spawnBlock(center.clone().add(x, cy - 0.3, z), Material.PACKED_ICE);
                arm.scale(0.4f, 0.4f, 2.0f).glow(200, 200, 220).interpolation(3, 0)
                   .rotate((float) angle, 0, 1, 0);
                arms.add(arm);
                spawnedEntities.add(arm.entity());
            }

            // 6 amethyst cluster ornaments at arm tips
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                double x = Math.cos(angle) * 3.0;
                double z = Math.sin(angle) * 3.0;
                BlockDisplayHandle ornament = displayBuilder.spawnBlock(center.clone().add(x, cy - 0.8, z), Material.AMETHYST_CLUSTER);
                ornament.scale(0.5f, 0.7f, 0.5f).glow(200, 160, 255).interpolation(3, 0);
                ornaments.add(ornament);
                spawnedEntities.add(ornament.entity());
            }

            // 6 hanging shard positions (will drop one by one)
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6 + Math.PI / 6;
                double x = Math.cos(angle) * 1.8;
                double z = Math.sin(angle) * 1.8;
                BlockDisplayHandle shard = displayBuilder.spawnBlock(center.clone().add(x, cy - 1.5, z), Material.BLUE_ICE);
                shard.scale(0.3f, 1.2f, 0.3f).glow(180, 210, 255).interpolation(3, 0);
                shards.add(shard);
                shardY.add(cy - 1.5);
                spawnedEntities.add(shard.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, cy, 0), Sound.BLOCK_GLASS_PLACE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Gentle sway of the whole chandelier
            double sway = Math.sin(ticksAlive * 0.03) * 0.15;
            centerPiece.entity().teleport(c.clone().add(sway, 8.0, 0));

            // Drop a shard every 10 ticks
            if (ticksAlive % 10 == 0 && nextShardIndex < shards.size()) {
                int idx = nextShardIndex++;
                // Mark this shard as falling
                shardY.set(idx, shardY.get(idx)); // already stored
            }

            // Animate falling shards
            for (int i = 0; i < nextShardIndex && i < shards.size(); i++) {
                double y = shardY.get(i);
                if (y > 0) {
                    y -= 0.5;
                    shardY.set(i, y);
                    double angle = (Math.PI * 2 * i) / 6 + Math.PI / 6;
                    double x = Math.cos(angle) * 1.8;
                    double z = Math.sin(angle) * 1.8;
                    shards.get(i).entity().teleport(c.clone().add(x + sway, y, z));

                    if (y <= 0.5) {
                        // Impact
                        Location impactLoc = c.clone().add(x, 0, z);
                        triggerImpactDamage(impactLoc);
                        DisplayBuilder.dustParticles(impactLoc, 15, 1.0, 180, 210, 255, 1.5f);
                        DisplayBuilder.playSound(impactLoc, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.5f);
                    }
                }
            }

            // Frost particles from chandelier
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 7.5, 0), 8, 2.0, 240, 240, 255, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new IceChandelier(plugin); }
    }

    // ================================================================
    // 2. FROST CAGE — Packed ice cube contracting around player
    // ================================================================
    public static class FrostCage extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> wallBlocks = new ArrayList<>();
        private final List<double[]> wallOffsets = new ArrayList<>();
        private double currentScale = 4.0;
        private static final double CONTRACT_RATE = 4.0 / 60.0; // contracts over 60 ticks

        public FrostCage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_cage", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Build a cage: 4 walls, each 4 blocks tall, 4 blocks wide = 16 blocks
            // Front wall (4 blocks)
            for (int y = 0; y < 4; y++) {
                wallOffsets.add(new double[]{0, y, currentScale});
                wallOffsets.add(new double[]{0, y, -currentScale});
                wallOffsets.add(new double[]{currentScale, y, 0});
                wallOffsets.add(new double[]{-currentScale, y, 0});
            }

            for (double[] offset : wallOffsets) {
                Location loc = center.clone().add(offset[0], offset[1], offset[2]);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                block.scale(1.0f, 1.0f, 1.0f).glow(180, 210, 255).interpolation(5, 0);
                wallBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Contract the cage
            if (currentScale > 0.5) {
                currentScale -= CONTRACT_RATE;
                if (currentScale < 0.5) currentScale = 0.5;
            }

            // Update wall positions
            int idx = 0;
            for (int y = 0; y < 4; y++) {
                wallBlocks.get(idx++).entity().teleport(c.clone().add(0, y, currentScale));
                wallBlocks.get(idx++).entity().teleport(c.clone().add(0, y, -currentScale));
                wallBlocks.get(idx++).entity().teleport(c.clone().add(currentScale, y, 0));
                wallBlocks.get(idx++).entity().teleport(c.clone().add(-currentScale, y, 0));
            }

            // Ice crack particles as walls close in
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 2, 0), 10, currentScale, 200, 200, 220, 1.2f);
            }

            // Creaking sound as it contracts
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FrostCage(plugin); }
    }

    // ================================================================
    // 3. CRYSTAL SPIRE — Tall twisted spire with frost particles
    // ================================================================
    public static class CrystalSpire extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> spireBlocks = new ArrayList<>();
        private float rotationAngle = 0;

        public CrystalSpire(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_spire", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Main spire: 10 amethyst blocks spiraling upward
            for (int i = 0; i < 10; i++) {
                double angle = (Math.PI * 2 * i) / 10 * 2; // double spiral
                double radius = 0.6 - (i * 0.04);
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location loc = center.clone().add(x, i * 0.8, z);
                Material mat = (i % 3 == 0) ? Material.CALCITE : Material.AMETHYST_BLOCK;
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                float taper = 1.0f - (i * 0.06f);
                block.scale(taper, 0.8f, taper).glow(200, 160, 255).interpolation(3, 0);
                spireBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            // Base ring: 4 calcite foundation blocks
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4;
                double x = Math.cos(angle) * 1.2;
                double z = Math.sin(angle) * 1.2;
                BlockDisplayHandle base = displayBuilder.spawnBlock(center.clone().add(x, 0, z), Material.CALCITE);
                base.scale(0.8f, 0.5f, 0.8f).glow(220, 220, 230).interpolation(3, 0);
                spireBlocks.add(base);
                spawnedEntities.add(base.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_PLACE, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            rotationAngle += 0.02f;

            // Slowly rotate spire blocks around center axis
            for (int i = 0; i < Math.min(10, spireBlocks.size()); i++) {
                double baseAngle = (Math.PI * 2 * i) / 10 * 2 + rotationAngle;
                double radius = 0.6 - (i * 0.04);
                double x = Math.cos(baseAngle) * radius;
                double z = Math.sin(baseAngle) * radius;
                spireBlocks.get(i).entity().teleport(c.clone().add(x, i * 0.8, z));
            }

            // Frost particles spiraling upward
            if (ticksAlive % 3 == 0) {
                double pAngle = rotationAngle * 3;
                double px = Math.cos(pAngle) * 1.0;
                double pz = Math.sin(pAngle) * 1.0;
                double py = (ticksAlive % 60) * 0.13;
                DisplayBuilder.dustParticles(c.clone().add(px, py, pz), 5, 0.3, 180, 210, 255, 1.2f);
            }

            // Ambient chime
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalSpire(plugin); }
    }

    // ================================================================
    // 4. GLACIAL SAW — Flat disc tilted 45 degrees rolling toward player
    // ================================================================
    public static class GlacialSaw extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> discBlocks = new ArrayList<>();
        private double offsetX = 0;
        private double offsetZ = 0;
        private double dirX, dirZ;
        private float spinAngle = 0;
        private double travelDist = 0;

        public GlacialSaw(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_saw", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Random direction to roll
            double angle = Math.random() * Math.PI * 2;
            dirX = Math.cos(angle);
            dirZ = Math.sin(angle);

            // 12 blocks forming a flat disc
            for (int i = 0; i < 12; i++) {
                double a = (Math.PI * 2 * i) / 12;
                double r = 2.0;
                double lx = Math.cos(a) * r;
                double lz = Math.sin(a) * r;
                Location loc = center.clone().add(lx, 1.5, lz);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                block.scale(0.6f, 0.15f, 0.6f).glow(180, 210, 255).interpolation(2, 0)
                     .rotate((float)(Math.PI / 4), 1, 0, 0); // 45 degree tilt
                discBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GRINDSTONE_USE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Roll forward
            travelDist += 0.4;
            offsetX = dirX * travelDist;
            offsetZ = dirZ * travelDist;
            spinAngle += 0.3f;

            // Update disc positions
            for (int i = 0; i < discBlocks.size(); i++) {
                double a = (Math.PI * 2 * i) / 12 + spinAngle;
                double r = 2.0;
                double lx = Math.cos(a) * r + offsetX;
                double lz = Math.sin(a) * r + offsetZ;
                discBlocks.get(i).entity().teleport(c.clone().add(lx, 1.5, lz));
                discBlocks.get(i).rotate((float)(Math.PI / 4), 1, 0, 0);
            }

            // Ice trail particles
            if (ticksAlive % 2 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(offsetX, 0.5, offsetZ), 6, 1.5, 200, 200, 220, 1.0f);
            }

            // Grinding sound
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c.clone().add(offsetX, 1.5, offsetZ), Sound.BLOCK_GRINDSTONE_USE, 0.6f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GlacialSaw(plugin); }
    }

    // ================================================================
    // 5. SNOWFLAKE MANDALA — 6-fold symmetric rotating snowflake
    // ================================================================
    public static class SnowflakeMandala extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> snowflakeBlocks = new ArrayList<>();
        private float rotationAngle = 0;

        public SnowflakeMandala(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("snowflake_mandala", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(5.5);
            config.setDurationTicks(350);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double height = 3.0;

            // 6-fold symmetry: 6 arms, each arm = 3 blocks + 1 tip = 4 per arm
            // Plus 2 center blocks = 26 total but we'll use 20 for clean pattern
            // Center hub
            BlockDisplayHandle hub = displayBuilder.spawnBlock(center.clone().add(0, height, 0), Material.ICE);
            hub.scale(1.2f, 0.15f, 1.2f).glow(240, 240, 255).interpolation(3, 0);
            snowflakeBlocks.add(hub);
            spawnedEntities.add(hub.entity());

            BlockDisplayHandle hub2 = displayBuilder.spawnBlock(center.clone().add(0, height + 0.1, 0), Material.WHITE_STAINED_GLASS);
            hub2.scale(0.8f, 0.1f, 0.8f).glow(240, 240, 255).interpolation(3, 0);
            snowflakeBlocks.add(hub2);
            spawnedEntities.add(hub2.entity());

            // 6 main arms
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                // 3 blocks per arm at increasing radius
                for (int j = 1; j <= 3; j++) {
                    double r = j * 1.5;
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;
                    Material mat = (j == 3) ? Material.WHITE_STAINED_GLASS : Material.ICE;
                    BlockDisplayHandle block = displayBuilder.spawnBlock(center.clone().add(x, height, z), mat);
                    float s = 0.6f - (j * 0.1f);
                    block.scale(s, 0.12f, s).glow(200, 200, 220).interpolation(3, 0);
                    snowflakeBlocks.add(block);
                    spawnedEntities.add(block.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            rotationAngle += 0.015f;
            double height = 3.0;

            // Rotate hub (index 0, 1 stay centered)
            snowflakeBlocks.get(0).entity().teleport(c.clone().add(0, height, 0));
            snowflakeBlocks.get(1).entity().teleport(c.clone().add(0, height + 0.1, 0));

            // Rotate arms
            int idx = 2;
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6 + rotationAngle;
                for (int j = 1; j <= 3; j++) {
                    double r = j * 1.5;
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;
                    if (idx < snowflakeBlocks.size()) {
                        snowflakeBlocks.get(idx).entity().teleport(c.clone().add(x, height, z));
                    }
                    idx++;
                }
            }

            // Sparkle particles at tips
            if (ticksAlive % 5 == 0) {
                for (int i = 0; i < 6; i++) {
                    double angle = (Math.PI * 2 * i) / 6 + rotationAngle;
                    double x = Math.cos(angle) * 4.5;
                    double z = Math.sin(angle) * 4.5;
                    DisplayBuilder.dustParticles(c.clone().add(x, height, z), 3, 0.3, 240, 240, 255, 0.8f);
                }
            }

            // Gentle snowfall beneath
            if (ticksAlive % 3 == 0) {
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, height - 0.5, 0), 4, 3.0, 0.5, 3.0, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SnowflakeMandala(plugin); }
    }

    // ================================================================
    // 6. ICICLE BARRAGE — Thin icicles at Y+12, drop one by one
    // ================================================================
    public static class IcicleBarrage extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> icicles = new ArrayList<>();
        private final List<double[]> iciclePositions = new ArrayList<>();
        private final List<Double> icicleY = new ArrayList<>();
        private final List<Boolean> icicleDropped = new ArrayList<>();
        private int nextDrop = 0;

        public IcicleBarrage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("icicle_barrage", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(2.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 15 icicles spread across a 6x6 area at Y+12
            Random rand = new Random();
            for (int i = 0; i < 15; i++) {
                double ox = (rand.nextDouble() - 0.5) * 6.0;
                double oz = (rand.nextDouble() - 0.5) * 6.0;
                double y = 12.0 + rand.nextDouble() * 2.0;
                iciclePositions.add(new double[]{ox, oz});
                icicleY.add(y);
                icicleDropped.add(false);

                Location loc = center.clone().add(ox, y, oz);
                BlockDisplayHandle icicle = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                icicle.scale(0.3f, 2.0f, 0.3f).glow(180, 210, 255).interpolation(2, 0);
                icicles.add(icicle);
                spawnedEntities.add(icicle.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, 12, 0), Sound.BLOCK_GLASS_PLACE, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Drop one icicle every 8 ticks
            if (ticksAlive % 8 == 0 && nextDrop < icicles.size()) {
                icicleDropped.set(nextDrop, true);
                DisplayBuilder.playSound(c.clone().add(0, 12, 0), Sound.BLOCK_GLASS_BREAK, 0.5f, 1.8f);
                nextDrop++;
            }

            // Animate falling icicles
            for (int i = 0; i < icicles.size(); i++) {
                if (!icicleDropped.get(i)) {
                    // Menacing wobble
                    double wobble = Math.sin(ticksAlive * 0.2 + i) * 0.05;
                    double[] pos = iciclePositions.get(i);
                    icicles.get(i).entity().teleport(c.clone().add(pos[0] + wobble, icicleY.get(i), pos[1]));
                    continue;
                }

                double y = icicleY.get(i);
                if (y > 0.3) {
                    y -= 0.6; // fast fall
                    icicleY.set(i, y);
                    double[] pos = iciclePositions.get(i);
                    icicles.get(i).entity().teleport(c.clone().add(pos[0], y, pos[1]));

                    // Trail
                    if (y > 2) {
                        DisplayBuilder.dustParticles(c.clone().add(pos[0], y + 1, pos[1]), 3, 0.2, 200, 200, 220, 0.8f);
                    }
                } else if (y > -1) {
                    // Impact
                    icicleY.set(i, -10.0); // mark done
                    double[] pos = iciclePositions.get(i);
                    Location impactLoc = c.clone().add(pos[0], 0, pos[1]);
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.dustParticles(impactLoc, 20, 1.5, 180, 210, 255, 1.5f);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.2f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new IcicleBarrage(plugin); }
    }

    // ================================================================
    // 7. FROZEN THRONE — Stationary throne with frost aura
    // ================================================================
    public static class FrozenThrone extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> throneBlocks = new ArrayList<>();

        public FrozenThrone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_throne", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Seat (wide base)
            BlockDisplayHandle seat = displayBuilder.spawnBlock(center.clone().add(0, 0.5, 0), Material.QUARTZ_BLOCK);
            seat.scale(2.0f, 0.5f, 1.5f).glow(220, 220, 230).interpolation(3, 0);
            throneBlocks.add(seat);
            spawnedEntities.add(seat.entity());

            // Back rest (tall)
            BlockDisplayHandle back = displayBuilder.spawnBlock(center.clone().add(0, 1.5, -0.6), Material.BLUE_ICE);
            back.scale(2.0f, 3.0f, 0.5f).glow(180, 210, 255).interpolation(3, 0);
            throneBlocks.add(back);
            spawnedEntities.add(back.entity());

            // Left armrest
            BlockDisplayHandle leftArm = displayBuilder.spawnBlock(center.clone().add(-1.2, 0.8, 0), Material.QUARTZ_BLOCK);
            leftArm.scale(0.4f, 1.2f, 1.2f).glow(220, 220, 230).interpolation(3, 0);
            throneBlocks.add(leftArm);
            spawnedEntities.add(leftArm.entity());

            // Right armrest
            BlockDisplayHandle rightArm = displayBuilder.spawnBlock(center.clone().add(1.2, 0.8, 0), Material.QUARTZ_BLOCK);
            rightArm.scale(0.4f, 1.2f, 1.2f).glow(220, 220, 230).interpolation(3, 0);
            throneBlocks.add(rightArm);
            spawnedEntities.add(rightArm.entity());

            // Crown spikes on back (3 spikes)
            for (int i = -1; i <= 1; i++) {
                BlockDisplayHandle spike = displayBuilder.spawnBlock(center.clone().add(i * 0.7, 3.5, -0.6), Material.BLUE_ICE);
                spike.scale(0.3f, 1.5f, 0.3f).glow(200, 200, 220).interpolation(3, 0);
                throneBlocks.add(spike);
                spawnedEntities.add(spike.entity());
            }

            // Base steps (2 steps)
            BlockDisplayHandle step1 = displayBuilder.spawnBlock(center.clone().add(0, 0, 1.0), Material.QUARTZ_BLOCK);
            step1.scale(2.5f, 0.3f, 1.0f).glow(220, 220, 230).interpolation(3, 0);
            throneBlocks.add(step1);
            spawnedEntities.add(step1.entity());

            BlockDisplayHandle step2 = displayBuilder.spawnBlock(center.clone().add(0, -0.2, 2.0), Material.QUARTZ_BLOCK);
            step2.scale(3.0f, 0.3f, 1.0f).glow(220, 220, 230).interpolation(3, 0);
            throneBlocks.add(step2);
            spawnedEntities.add(step2.entity());

            // Decorative ice crystals on armrests
            BlockDisplayHandle lcDeco = displayBuilder.spawnBlock(center.clone().add(-1.2, 1.6, 0), Material.BLUE_ICE);
            lcDeco.scale(0.25f, 0.5f, 0.25f).glow(180, 210, 255).interpolation(3, 0);
            throneBlocks.add(lcDeco);
            spawnedEntities.add(lcDeco.entity());

            BlockDisplayHandle rcDeco = displayBuilder.spawnBlock(center.clone().add(1.2, 1.6, 0), Material.BLUE_ICE);
            rcDeco.scale(0.25f, 0.5f, 0.25f).glow(180, 210, 255).interpolation(3, 0);
            throneBlocks.add(rcDeco);
            spawnedEntities.add(rcDeco.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Frost aura particles in a ring
            if (ticksAlive % 4 == 0) {
                double angle = (ticksAlive * 0.1) % (Math.PI * 2);
                for (int i = 0; i < 3; i++) {
                    double a = angle + (Math.PI * 2 * i) / 3;
                    double x = Math.cos(a) * 5.0;
                    double z = Math.sin(a) * 5.0;
                    DisplayBuilder.dustParticles(c.clone().add(x, 1.0, z), 4, 0.5, 180, 210, 255, 1.5f);
                }
            }

            // Snowflake particles drifting down from throne
            if (ticksAlive % 6 == 0) {
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 3.5, 0), 3, 1.0, 0.3, 1.0, 0);
            }

            // Ambient frost sound
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_STEP, 0.8f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FrozenThrone(plugin); }
    }

    // ================================================================
    // 8. PERMAFROST PILLARS — 12 pillars rise, lean inward, collapse
    // ================================================================
    public static class PermafrostPillars extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> pillars = new ArrayList<>();
        private final List<double[]> pillarPositions = new ArrayList<>();
        private float pillarHeight = 0;
        private float leanAngle = 0;
        private boolean collapsed = false;
        private static final float MAX_HEIGHT = 3.0f;
        private static final float RISE_SPEED = 0.1f;
        private static final float LEAN_SPEED = 0.02f;

        public PermafrostPillars(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("permafrost_pillars", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0);
            config.setImpactRadius(4.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 12 pillars in a circle radius 5
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                double x = Math.cos(angle) * 5.0;
                double z = Math.sin(angle) * 5.0;
                pillarPositions.add(new double[]{x, z, angle});

                Location loc = center.clone().add(x, 0, z);
                BlockDisplayHandle pillar = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                pillar.scale(0.5f, 0.1f, 0.5f).glow(180, 210, 255).interpolation(3, 0);
                pillars.add(pillar);
                spawnedEntities.add(pillar.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BASALT_BREAK, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (collapsed) return;

            // Phase 1: Rise (0-30 ticks)
            if (pillarHeight < MAX_HEIGHT) {
                pillarHeight += RISE_SPEED;
                if (pillarHeight > MAX_HEIGHT) pillarHeight = MAX_HEIGHT;

                for (int i = 0; i < pillars.size(); i++) {
                    double[] pos = pillarPositions.get(i);
                    pillars.get(i).entity().teleport(c.clone().add(pos[0], 0, pos[1]));
                    pillars.get(i).scale(0.5f, pillarHeight, 0.5f);
                }

                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_BASALT_PLACE, 0.4f, 0.6f);
                }
            }
            // Phase 2: Lean inward (30-90 ticks)
            else if (leanAngle < Math.PI / 4) {
                leanAngle += LEAN_SPEED;

                for (int i = 0; i < pillars.size(); i++) {
                    double[] pos = pillarPositions.get(i);
                    double angle = pos[2];
                    // Lean toward center
                    double leanX = pos[0] - Math.cos(angle) * Math.sin(leanAngle) * 2.0;
                    double leanZ = pos[1] - Math.sin(angle) * Math.sin(leanAngle) * 2.0;
                    double leanY = Math.cos(leanAngle) * pillarHeight * 0.5;
                    pillars.get(i).entity().teleport(c.clone().add(leanX, leanY, leanZ));
                    pillars.get(i).rotate((float) leanAngle, (float) -Math.sin(angle), 0, (float) Math.cos(angle));
                }

                // Cracking particles
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), 8, 3.0, 200, 200, 220, 1.0f);
                }
            }
            // Phase 3: Collapse
            else {
                collapsed = true;
                triggerImpactDamage(c);

                // Scatter particles
                for (int i = 0; i < 12; i++) {
                    double[] pos = pillarPositions.get(i);
                    DisplayBuilder.dustParticles(c.clone().add(pos[0] * 0.3, 0.5, pos[1] * 0.3), 10, 1.5, 180, 210, 255, 2.0f);
                }

                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_DAMAGE, 1.0f, 0.5f);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c, 40, 3.0, 1.5, 3.0, 0.1);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PermafrostPillars(plugin); }
    }

    // ================================================================
    // 9. HAILSTONE CLUSTER — 10 falling blocks of varying sizes
    // ================================================================
    public static class HailstoneCluster extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> hailstones = new ArrayList<>();
        private final List<double[]> hailPos = new ArrayList<>();
        private final List<Double> hailY = new ArrayList<>();
        private final List<Float> hailScale = new ArrayList<>();
        private final List<Boolean> hailImpacted = new ArrayList<>();

        public HailstoneCluster(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hailstone_cluster", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Random rand = new Random();
            Material[] mats = {Material.PACKED_ICE, Material.BLUE_ICE, Material.ICE, Material.SNOW_BLOCK};

            for (int i = 0; i < 10; i++) {
                double ox = (rand.nextDouble() - 0.5) * 8.0;
                double oz = (rand.nextDouble() - 0.5) * 8.0;
                double startY = 15.0 + rand.nextDouble() * 5.0;
                float scale = 0.5f + rand.nextFloat() * 1.5f;

                hailPos.add(new double[]{ox, oz});
                hailY.add(startY);
                hailScale.add(scale);
                hailImpacted.add(false);

                Location loc = center.clone().add(ox, startY, oz);
                BlockDisplayHandle stone = displayBuilder.spawnBlock(loc, mats[rand.nextInt(mats.length)]);
                stone.scale(scale, scale, scale).glow(200, 200, 220).interpolation(2, 0)
                     .rotate(rand.nextFloat() * 3.14f, rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
                hailstones.add(stone);
                spawnedEntities.add(stone.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, 15, 0), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < hailstones.size(); i++) {
                if (hailImpacted.get(i)) continue;

                double y = hailY.get(i);
                // Accelerating fall
                double fallSpeed = 0.3 + (20.0 - y) * 0.02;
                if (fallSpeed < 0.3) fallSpeed = 0.3;
                y -= fallSpeed;
                hailY.set(i, y);

                double[] pos = hailPos.get(i);
                hailstones.get(i).entity().teleport(c.clone().add(pos[0], y, pos[1]));

                // Tumble rotation
                float spin = ticksAlive * 0.15f + i;
                hailstones.get(i).rotate(spin, 1, 0.5f, 0);

                if (y <= 0.5) {
                    hailImpacted.set(i, true);
                    Location impactLoc = c.clone().add(pos[0], 0, pos[1]);
                    triggerImpactDamage(impactLoc);

                    float scale = hailScale.get(i);
                    int particleCount = (int) (scale * 15);
                    DisplayBuilder.dustParticles(impactLoc, particleCount, scale * 1.5, 180, 210, 255, 1.5f);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.8f);
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, impactLoc, 10, 1.0, 0.5, 1.0, 0.1);
                }
            }

            // Shadow/warning circles on ground
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < hailstones.size(); i++) {
                    if (!hailImpacted.get(i) && hailY.get(i) < 10) {
                        double[] pos = hailPos.get(i);
                        DisplayBuilder.dustParticles(c.clone().add(pos[0], 0.1, pos[1]), 3, 0.5, 100, 100, 120, 0.8f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HailstoneCluster(plugin); }
    }

    // ================================================================
    // 10. ICE MIRROR — Flat vertical mirror sweeping light beam
    // ================================================================
    public static class IceMirror extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> mirrorBlocks = new ArrayList<>();
        private float sweepAngle = 0;

        public IceMirror(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_mirror", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 14-block vertical mirror: 7 wide x 2 tall arrangement
            for (int x = -3; x <= 3; x++) {
                for (int y = 0; y < 2; y++) {
                    Location loc = center.clone().add(x * 0.8, 2.0 + y, 0);
                    BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.LIGHT_BLUE_STAINED_GLASS);
                    block.scale(0.75f, 1.0f, 0.1f).glow(200, 220, 255).interpolation(3, 0)
                         .brightness(15, 15);
                    mirrorBlocks.add(block);
                    spawnedEntities.add(block.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            sweepAngle += 0.04f;

            // Rotate the mirror by repositioning blocks
            int idx = 0;
            for (int x = -3; x <= 3; x++) {
                for (int y = 0; y < 2; y++) {
                    double rx = x * 0.8 * Math.cos(sweepAngle);
                    double rz = x * 0.8 * Math.sin(sweepAngle);
                    if (idx < mirrorBlocks.size()) {
                        mirrorBlocks.get(idx).entity().teleport(c.clone().add(rx, 2.0 + y, rz));
                        mirrorBlocks.get(idx).rotate(sweepAngle, 0, 1, 0);
                    }
                    idx++;
                }
            }

            // Light beam extending from mirror surface
            if (ticksAlive % 2 == 0) {
                double beamDirX = Math.cos(sweepAngle + Math.PI / 2);
                double beamDirZ = Math.sin(sweepAngle + Math.PI / 2);
                for (int d = 1; d <= 8; d++) {
                    DisplayBuilder.dustParticles(
                            c.clone().add(beamDirX * d, 2.5, beamDirZ * d),
                            2, 0.2, 240, 240, 255, 1.0f);
                }
            }

            // Shimmer particles on mirror surface
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 2.5, 0), 6, 2.0, 200, 220, 255, 0.8f);
            }

            // Ambient sound
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new IceMirror(plugin); }
    }

    // ================================================================
    // 11. FROST HELIX — Double helix that rotates and rises
    // ================================================================
    public static class FrostHelix extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> helixA = new ArrayList<>();
        private final List<BlockDisplayHandle> helixB = new ArrayList<>();
        private float riseOffset = 0;
        private float rotationOffset = 0;

        public FrostHelix(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_helix", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double radius = 2.0;
            // Strand A: 8 blocks
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                double y = i * 0.8;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                BlockDisplayHandle block = displayBuilder.spawnBlock(center.clone().add(x, y, z), Material.BLUE_ICE);
                block.scale(0.6f, 0.6f, 0.6f).glow(180, 210, 255).interpolation(3, 0);
                helixA.add(block);
                spawnedEntities.add(block.entity());
            }

            // Strand B: 8 blocks (offset by PI)
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8 + Math.PI;
                double y = i * 0.8;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                BlockDisplayHandle block = displayBuilder.spawnBlock(center.clone().add(x, y, z), Material.BLUE_ICE);
                block.scale(0.6f, 0.6f, 0.6f).glow(200, 200, 220).interpolation(3, 0);
                helixB.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            rotationOffset += 0.05f;
            riseOffset += 0.01f;
            double radius = 2.0;

            for (int i = 0; i < helixA.size(); i++) {
                double angle = (Math.PI * 2 * i) / 8 + rotationOffset;
                double y = (i * 0.8 + riseOffset) % 7.0;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                helixA.get(i).entity().teleport(c.clone().add(x, y, z));
            }

            for (int i = 0; i < helixB.size(); i++) {
                double angle = (Math.PI * 2 * i) / 8 + Math.PI + rotationOffset;
                double y = (i * 0.8 + riseOffset) % 7.0;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                helixB.get(i).entity().teleport(c.clone().add(x, y, z));
            }

            // Connecting particles between strands
            if (ticksAlive % 4 == 0) {
                int idx = ticksAlive / 4 % 8;
                double angleA = (Math.PI * 2 * idx) / 8 + rotationOffset;
                double angleB = angleA + Math.PI;
                double y = (idx * 0.8 + riseOffset) % 7.0;
                Location a = c.clone().add(Math.cos(angleA) * radius, y, Math.sin(angleA) * radius);
                Location b = c.clone().add(Math.cos(angleB) * radius, y, Math.sin(angleB) * radius);
                DisplayBuilder.particleLine(a, b, Particle.SNOWFLAKE, 3, null);
            }

            // Frost ambient
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 5, 2.5, 240, 240, 255, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FrostHelix(plugin); }
    }

    // ================================================================
    // 12. CRYSTALLINE METEOR — Jagged rock falling from Y+30 spinning
    // ================================================================
    public static class CrystallineMeteor extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> meteorBlocks = new ArrayList<>();
        private double meteorY = 30.0;
        private float spinAngle = 0;
        private boolean impacted = false;

        public CrystallineMeteor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystalline_meteor", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(16.0);
            config.setImpactRadius(6.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Core: 4 amethyst blocks
            double[][] coreOffsets = {{0, 0, 0}, {0.5, 0.3, 0}, {-0.3, 0.5, 0.3}, {0, -0.3, -0.4}};
            for (double[] off : coreOffsets) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], meteorY + off[1], off[2]), Material.AMETHYST_BLOCK);
                block.scale(1.2f, 1.2f, 1.2f).glow(200, 160, 255).interpolation(2, 0);
                meteorBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            // Shell: 4 diamond blocks
            double[][] shellOffsets = {{1.0, 0, 0}, {-1.0, 0, 0}, {0, 0, 1.0}, {0, 0, -1.0}};
            for (double[] off : shellOffsets) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], meteorY + off[1], off[2]), Material.DIAMOND_BLOCK);
                block.scale(0.8f, 0.8f, 0.8f).glow(180, 210, 255).interpolation(2, 0);
                meteorBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            // Jagged spikes: 4 amethyst spikes
            double[][] spikeOffsets = {{0.7, 1.2, 0.7}, {-0.7, 1.0, -0.7}, {0.8, -1.0, -0.5}, {-0.6, -0.8, 0.6}};
            for (double[] off : spikeOffsets) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], meteorY + off[1], off[2]), Material.AMETHYST_BLOCK);
                block.scale(0.3f, 1.0f, 0.3f).glow(220, 180, 255).interpolation(2, 0);
                meteorBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, 30, 0), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || impacted) return;

            // Accelerating fall
            double fallSpeed = 0.2 + ticksAlive * 0.015;
            meteorY -= fallSpeed;
            spinAngle += 0.12f;

            // Update all block positions with spin
            double[][] allOffsets = {
                    {0, 0, 0}, {0.5, 0.3, 0}, {-0.3, 0.5, 0.3}, {0, -0.3, -0.4},
                    {1.0, 0, 0}, {-1.0, 0, 0}, {0, 0, 1.0}, {0, 0, -1.0},
                    {0.7, 1.2, 0.7}, {-0.7, 1.0, -0.7}, {0.8, -1.0, -0.5}, {-0.6, -0.8, 0.6}
            };

            for (int i = 0; i < meteorBlocks.size() && i < allOffsets.length; i++) {
                double[] off = allOffsets[i];
                // Apply spin around Y axis
                double rx = off[0] * Math.cos(spinAngle) - off[2] * Math.sin(spinAngle);
                double rz = off[0] * Math.sin(spinAngle) + off[2] * Math.cos(spinAngle);
                meteorBlocks.get(i).entity().teleport(c.clone().add(rx, meteorY + off[1], rz));
            }

            // Trail particles
            if (ticksAlive % 2 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, meteorY + 2, 0), 10, 1.0, 180, 210, 255, 2.0f);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, meteorY + 1.5, 0), 5, 0.8, 0.8, 0.8, 0);
            }

            // Warning circle on ground
            if (meteorY < 20 && ticksAlive % 3 == 0) {
                double warningRadius = 6.0 * (1 - meteorY / 30.0);
                DisplayBuilder.particleRing(c.clone().add(0, 0.1, 0), warningRadius, Particle.DUST, 20,
                        new Particle.DustOptions(Color.fromRGB(180, 210, 255), 1.0f));
            }

            // Impact
            if (meteorY <= 1.0) {
                impacted = true;
                triggerImpactDamage(c);

                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c, 80, 4.0, 2.0, 4.0, 0.3);
                DisplayBuilder.dustParticles(c, 50, 5.0, 180, 210, 255, 2.5f);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.6f);
                displayBuilder.removeAll();
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystallineMeteor(plugin); }
    }

    // ================================================================
    // 13. AVALANCHE WALL — Wall of snow/ice rushing forward
    // ================================================================
    public static class AvalancheWall extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> wallBlocks = new ArrayList<>();
        private double travelDist = 0;
        private double dirX, dirZ;
        private boolean impacted = false;

        public AvalancheWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("avalanche_wall", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(5.0);
            config.setDurationTicks(150);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Random direction
            double angle = Math.random() * Math.PI * 2;
            dirX = Math.cos(angle);
            dirZ = Math.sin(angle);

            // Wall: 6 wide x 3 tall = 18 blocks
            // Perpendicular to movement direction
            double perpX = -dirZ;
            double perpZ = dirX;

            for (int col = -3; col <= 2; col++) {
                for (int row = 0; row < 3; row++) {
                    double wx = perpX * col * 1.2 - dirX * 8; // start 8 blocks back
                    double wz = perpZ * col * 1.2 - dirZ * 8;
                    Material mat = (row == 0) ? Material.PACKED_ICE : Material.SNOW_BLOCK;
                    if (col == -3 || col == 2) mat = Material.PACKED_ICE; // edge pillars

                    Location loc = center.clone().add(wx, row, wz);
                    BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                    float scaleY = 1.0f + (row == 1 ? 0.3f : 0);
                    block.scale(1.2f, scaleY, 1.2f).glow(200, 200, 220).interpolation(2, 0);
                    wallBlocks.add(block);
                    spawnedEntities.add(block.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || impacted) return;

            // Rush forward: 0.5 blocks/tick
            travelDist += 0.5;

            double perpX = -dirZ;
            double perpZ = dirX;

            int idx = 0;
            for (int col = -3; col <= 2; col++) {
                for (int row = 0; row < 3; row++) {
                    double wx = perpX * col * 1.2 - dirX * 8 + dirX * travelDist;
                    double wz = perpZ * col * 1.2 - dirZ * 8 + dirZ * travelDist;
                    if (idx < wallBlocks.size()) {
                        wallBlocks.get(idx).entity().teleport(c.clone().add(wx, row, wz));
                    }
                    idx++;
                }
            }

            // Snow debris trail
            if (ticksAlive % 2 == 0) {
                double trailX = -dirX * 8 + dirX * (travelDist - 1);
                double trailZ = -dirZ * 8 + dirZ * (travelDist - 1);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(trailX, 1.0, trailZ), 8, 2.0, 1.0, 2.0, 0.1);
                DisplayBuilder.dustParticles(c.clone().add(trailX, 0.5, trailZ), 6, 2.0, 240, 240, 255, 1.2f);
            }

            // Rumble sound
            if (ticksAlive % 10 == 0) {
                double frontX = -dirX * 8 + dirX * travelDist;
                double frontZ = -dirZ * 8 + dirZ * travelDist;
                DisplayBuilder.playSound(c.clone().add(frontX, 1, frontZ), Sound.ENTITY_IRON_GOLEM_HURT, 0.6f, 0.4f);
            }

            // Impact after traveling 16 blocks (8 start + 8 forward = at center)
            if (travelDist >= 16) {
                impacted = true;
                double impX = -dirX * 8 + dirX * travelDist;
                double impZ = -dirZ * 8 + dirZ * travelDist;
                Location impactLoc = c.clone().add(impX, 0, impZ);
                triggerImpactDamage(impactLoc);

                c.getWorld().spawnParticle(Particle.SNOWFLAKE, impactLoc, 60, 4.0, 2.0, 4.0, 0.2);
                DisplayBuilder.dustParticles(impactLoc, 30, 4.0, 200, 200, 220, 2.0f);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AvalancheWall(plugin); }
    }
}
