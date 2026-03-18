package com.blockforge.chaoscraft.modes.calamity.attacks.phase3.blockdisplay;

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
 * Phase 3 Block Display — GROUP 2: SUSPENDED BRIMSTONE LATTICE (Structures 9-17)
 * Mid-air floating structures forming the arena ceiling.
 * Dynamic hazard indicators that escalate with tier thresholds.
 *
 * Dweller theme: Brimstone Inversion — Hell consuming The End.
 * Palette: netherrack, blackstone, magma, soul soil/sand, obsidian, nether wart.
 * NO status effects. Damage in HP (not hearts).
 */
public final class SuspendedLattice {

    private SuspendedLattice() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ShardClusterA(plugin));
        registry.register(new ShardClusterB(plugin));
        registry.register(new MagmaChandelier(plugin));
        registry.register(new SuspendedBasaltCross(plugin));
        registry.register(new HangingSoulSandFaces(plugin));
        registry.register(new ObsidianStalactiteArray(plugin));
        registry.register(new RedGlassCeilingGrid(plugin));
        registry.register(new CryingObsidianCrown(plugin));
        registry.register(new NetherWartGroundArray(plugin));
    }

    // ================================================================
    // 9. FLOATING NETHERRACK SHARD CLUSTER A — Northwest, Y+14 to Y+22
    //    22 asymmetric shards in rough sphere, tumbling 3-axis rotation
    // ================================================================
    public static class ShardClusterA extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> shards = new ArrayList<>();
        private float descentProgress = 0;

        public ShardClusterA(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shard_cluster_a", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(6.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(5000);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location clusterCenter = center.clone().add(-8, 50, -8); // Start at Y+50

            // 22 shards in rough sphere, radius 3
            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < 22; i++) {
                double y = 1.0 - (2.0 * i / 21.0);
                double radiusAtY = Math.sqrt(1 - y * y);
                double theta = goldenAngle * i;
                double x = Math.cos(theta) * radiusAtY * 3.0;
                double z = Math.sin(theta) * radiusAtY * 3.0;

                Location loc = clusterCenter.clone().add(x, y * 3.0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERRACK);
                // Asymmetric scaling for shard appearance
                float sx = 0.3f + (float) (Math.random() * 0.5);
                float sy = 1.0f + (float) (Math.random() * 1.5);
                float sz = 0.3f + (float) (Math.random() * 0.5);
                h.scale(sx, sy, sz).glow(255, 100, 0).interpolation(3, 0);
                // Random outward-pointing rotation
                float randAngle = (float) (Math.random() * Math.PI);
                h.rotate(randAngle, (float) x, (float) y, (float) z);
                shards.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center.clone().add(-8, 18, -8),
                    Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            Location targetCenter = center.clone().add(-8, 18, -8);

            // Descent from Y+50 to Y+18 over 80 ticks with overshoot
            if (ticksAlive <= 80) {
                descentProgress = ticksAlive / 80.0f;
                float eased = descentProgress * descentProgress;
                // Overshoot: drop 3 blocks past, then rise back
                float targetY;
                if (descentProgress < 0.8f) {
                    targetY = 50.0f + (15.0f - 50.0f) * (eased / 0.64f);
                } else {
                    float bounce = (descentProgress - 0.8f) / 0.2f;
                    targetY = 15.0f + 3.0f * bounce;
                }
                for (BlockDisplayHandle h : shards) {
                    BlockDisplay bd = h.entity();
                    Location loc = bd.getLocation();
                    float dy = targetY - (float) loc.getY() + (float) center.getY();
                    bd.teleport(loc.clone().add(0, dy * 0.1, 0));
                }
            }

            // Tumbling rotation: X 0.3, Y 0.7, Z -0.2 deg/tick
            if (ticksAlive > 80) {
                float rotX = (float) Math.toRadians((ticksAlive - 80) * 0.3);
                float rotY = (float) Math.toRadians((ticksAlive - 80) * 0.7);
                for (BlockDisplayHandle h : shards) {
                    h.rotate(rotX + rotY, 0.3f, 0.7f, -0.2f);
                    h.interpolation(3, 0);
                }
            }

            // Lava + flame particles inside cluster
            if (ticksAlive % 2 == 0 && ticksAlive > 80) {
                Location particleLoc = targetCenter.clone().add(
                        (Math.random() - 0.5) * 3, (Math.random() - 0.5) * 3,
                        (Math.random() - 0.5) * 3);
                w.spawnParticle(Particle.LAVA, particleLoc, 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.FLAME, particleLoc, 2, 0.3, 0.3, 0.3, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShardClusterA(plugin); }
    }

    // ================================================================
    // 10. FLOATING NETHERRACK SHARD CLUSTER B — Northeast, blackstone shards,
    //     counter-rotation, soul fire particles
    // ================================================================
    public static class ShardClusterB extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> shards = new ArrayList<>();
        private float descentProgress = 0;

        public ShardClusterB(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shard_cluster_b", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(6.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(5000);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location clusterCenter = center.clone().add(8, 50, -8);

            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < 22; i++) {
                double y = 1.0 - (2.0 * i / 21.0);
                double radiusAtY = Math.sqrt(1 - y * y);
                double theta = goldenAngle * i;
                double x = Math.cos(theta) * radiusAtY * 3.0;
                double z = Math.sin(theta) * radiusAtY * 3.0;

                Location loc = clusterCenter.clone().add(x, y * 3.0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                float sx = 0.3f + (float) (Math.random() * 0.5);
                float sy = 1.0f + (float) (Math.random() * 1.5);
                float sz = 0.3f + (float) (Math.random() * 0.5);
                h.scale(sx, sy, sz).glow(0, 150, 255).interpolation(3, 0);
                float randAngle = (float) (Math.random() * Math.PI);
                h.rotate(randAngle, (float) x, (float) y, (float) z);
                shards.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center.clone().add(8, 18, -8),
                    Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            Location targetCenter = center.clone().add(8, 18, -8);

            // Descent with overshoot (same as Cluster A, 10-tick delay)
            if (ticksAlive <= 90) {
                int adjusted = Math.max(0, ticksAlive - 10);
                descentProgress = adjusted / 80.0f;
                if (descentProgress > 1.0f) descentProgress = 1.0f;
                float eased = descentProgress * descentProgress;
                float targetY;
                if (descentProgress < 0.8f) {
                    targetY = 50.0f + (15.0f - 50.0f) * (eased / 0.64f);
                } else {
                    float bounce = (descentProgress - 0.8f) / 0.2f;
                    targetY = 15.0f + 3.0f * bounce;
                }
                for (BlockDisplayHandle h : shards) {
                    BlockDisplay bd = h.entity();
                    Location loc = bd.getLocation();
                    float dy = targetY - (float) loc.getY() + (float) center.getY();
                    bd.teleport(loc.clone().add(0, dy * 0.1, 0));
                }
            }

            // Counter-rotation: reversed axes from Cluster A
            if (ticksAlive > 90) {
                float rotX = (float) Math.toRadians((ticksAlive - 90) * -0.3);
                float rotY = (float) Math.toRadians((ticksAlive - 90) * -0.7);
                for (BlockDisplayHandle h : shards) {
                    h.rotate(rotX + rotY, -0.3f, -0.7f, 0.2f);
                    h.interpolation(3, 0);
                }
            }

            // Soul fire flame particles inside cluster
            if (ticksAlive % 2 == 0 && ticksAlive > 90) {
                Location particleLoc = targetCenter.clone().add(
                        (Math.random() - 0.5) * 3, (Math.random() - 0.5) * 3,
                        (Math.random() - 0.5) * 3);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 3, 0.3, 0.3, 0.3, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShardClusterB(plugin); }
    }

    // ================================================================
    // 11. MAGMA BLOCK CHANDELIER — Central hanging chandelier Y+18 to Y+22,
    //     8 chains with spinning magma cream items, pulsing brightness
    // ================================================================
    public static class MagmaChandelier extends BlockDisplayAttack {

        private BlockDisplayHandle centralBlock;
        private final List<List<BlockDisplayHandle>> chains = new ArrayList<>();

        public MagmaChandelier(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_chandelier", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(5.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location chandelierCenter = center.clone().add(0, 60, 0); // Start at Y+60

            // Central magma block at Y+22
            centralBlock = displayBuilder.spawnBlock(chandelierCenter, Material.MAGMA_BLOCK);
            centralBlock.glow(255, 100, 0).interpolation(3, 0);
            spawnedEntities.add(centralBlock.entity());

            // 8 chains hanging at radius 2
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                double x = Math.cos(angle) * 2.0;
                double z = Math.sin(angle) * 2.0;

                List<BlockDisplayHandle> chain = new ArrayList<>();
                // 3 chain links per chain (magma blocks scaled thin)
                for (int link = 0; link < 3; link++) {
                    Location linkLoc = chandelierCenter.clone().add(x, -(1 + link), z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(linkLoc, Material.MAGMA_BLOCK);
                    h.scale(0.4f, 1.0f, 0.4f).glow(255, 100, 0).interpolation(3, 0);
                    chain.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Bottom pendant: obsidian block as weight
                Location pendantLoc = chandelierCenter.clone().add(x, -4, z);
                BlockDisplayHandle pendant = displayBuilder.spawnBlock(pendantLoc, Material.OBSIDIAN);
                pendant.scale(0.5f, 0.5f, 0.5f).glow(200, 0, 50).interpolation(3, 0);
                chain.add(pendant);
                spawnedEntities.add(pendant.entity());

                chains.add(chain);
            }

            DisplayBuilder.playSound(center.clone().add(0, 22, 0),
                    Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Descent from Y+60 to Y+22 over 100 ticks
            if (ticksAlive <= 100) {
                float t = ticksAlive / 100.0f;
                float eased = t * t;
                float currentY = 60.0f + (22.0f - 60.0f) * eased;
                if (centralBlock != null) {
                    centralBlock.entity().teleport(center.clone().add(0, currentY, 0));
                }
                for (int i = 0; i < chains.size(); i++) {
                    double angle = (2 * Math.PI * i) / 8;
                    double x = Math.cos(angle) * 2.0;
                    double z = Math.sin(angle) * 2.0;
                    List<BlockDisplayHandle> chain = chains.get(i);
                    for (int j = 0; j < chain.size(); j++) {
                        float yOff = -(1 + j);
                        chain.get(j).entity().teleport(
                                center.clone().add(x, currentY + yOff, z));
                    }
                }
            }

            // Central block brightness pulse: 25-tick period
            if (ticksAlive > 100 && centralBlock != null) {
                int brightness = (int) (10 + 5 * Math.sin(ticksAlive * Math.PI / 12.5));
                centralBlock.brightness(brightness, brightness);
            }

            // Chain sway: each chain oscillates on X or Z, 100-tick period, offset
            if (ticksAlive > 100) {
                for (int i = 0; i < chains.size(); i++) {
                    float swayOffset = i * 12.5f;
                    float swayAngle = (float) Math.sin((ticksAlive + swayOffset) * Math.PI / 50.0) * 0.087f; // 5 degrees
                    boolean swayX = (i % 2 == 0);
                    for (BlockDisplayHandle h : chains.get(i)) {
                        if (swayX) {
                            h.rotate(swayAngle, 1, 0, 0);
                        } else {
                            h.rotate(swayAngle, 0, 0, 1);
                        }
                        h.interpolation(5, 0);
                    }
                }
            }

            // Lava drip from chain bottoms (2/sec per chain = every 10 ticks)
            if (ticksAlive % 10 == 0 && ticksAlive > 100) {
                for (List<BlockDisplayHandle> chain : chains) {
                    if (!chain.isEmpty()) {
                        BlockDisplayHandle bottom = chain.get(chain.size() - 1);
                        Location drip = bottom.entity().getLocation().add(0, -0.5, 0);
                        w.spawnParticle(Particle.DRIPPING_LAVA, drip, 2, 0.1, 0, 0.1, 0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MagmaChandelier(plugin); }
    }

    // ================================================================
    // 12. SUSPENDED BASALT CROSS — Y+16, 10 blocks east, spinning cross
    //     of smooth basalt with separated pieces, portal particles
    // ================================================================
    public static class SuspendedBasaltCross extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> crossBlocks = new ArrayList<>();
        private BlockDisplayHandle centerPiece;
        private boolean spinForward = true;

        public SuspendedBasaltCross(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("suspended_basalt_cross", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(5.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(5000);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location crossCenter = center.clone().add(10, 16, 0);

            // Horizontal arm: 5 blocks with 0.15 gap
            for (int i = -2; i <= 2; i++) {
                if (i == 0) continue; // Center handled separately
                Location loc = crossCenter.clone().add(i * 1.15, 0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SMOOTH_BASALT);
                h.glow(200, 0, 50).interpolation(3, 0);
                crossBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Vertical arm: 5 blocks with 0.15 gap
            for (int i = -2; i <= 2; i++) {
                if (i == 0) continue;
                Location loc = crossCenter.clone().add(0, i * 1.15, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SMOOTH_BASALT);
                h.glow(200, 0, 50).interpolation(3, 0);
                crossBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Center piece: obsidian, slightly oversized
            centerPiece = displayBuilder.spawnBlock(crossCenter, Material.OBSIDIAN);
            centerPiece.scale(1.1f, 1.1f, 1.1f).glow(255, 100, 0)
                       .brightness(15, 15).interpolation(3, 0);
            spawnedEntities.add(centerPiece.entity());

            DisplayBuilder.playSound(crossCenter, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Z-axis spin at 0.5 deg/tick with direction reversal every 200 ticks
            if (ticksAlive % 200 == 0 && ticksAlive > 0) {
                spinForward = !spinForward;
            }

            float spinDir = spinForward ? 1.0f : -1.0f;
            // Acceleration spike at reversal (3 deg/tick for 10 ticks)
            float spinRate;
            int tickInCycle = ticksAlive % 200;
            if (tickInCycle < 10) {
                spinRate = 3.0f * spinDir;
            } else {
                spinRate = 0.5f * spinDir;
            }

            float rot = (float) Math.toRadians(ticksAlive * spinRate * 0.5);
            for (BlockDisplayHandle h : crossBlocks) {
                h.rotate(rot, 0, 0, 1);
                h.interpolation(3, 0);
            }
            if (centerPiece != null) {
                centerPiece.rotate(rot, 0, 0, 1);
                centerPiece.interpolation(3, 0);
            }

            // Individual block micro-drift: tiny circle orbit around grid position
            for (BlockDisplayHandle h : crossBlocks) {
                float driftAngle = (float) (ticksAlive * Math.PI / 40.0);
                float driftX = (float) Math.cos(driftAngle) * 0.05f;
                float driftY = (float) Math.sin(driftAngle) * 0.05f;
                BlockDisplay bd = h.entity();
                bd.teleport(bd.getLocation().add(driftX * 0.1, driftY * 0.1, 0));
            }

            // Portal particles from center outward along cross arms
            if (ticksAlive % 5 == 0 && centerPiece != null) {
                Location crossLoc = centerPiece.entity().getLocation();
                w.spawnParticle(Particle.PORTAL, crossLoc, 8, 1.5, 1.5, 0.1, 0.3);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SuspendedBasaltCross(plugin); }
    }

    // ================================================================
    // 13. HANGING SOUL SAND FACES — 6 soul sand blocks at Y+12-16,
    //     radius 18, random rotations, portal particle "threads"
    // ================================================================
    public static class HangingSoulSandFaces extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> faceBlocks = new ArrayList<>();
        private final float[] swayPeriods = new float[6];
        private final float[] swayAxes = new float[6];

        public HangingSoulSandFaces(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hanging_soul_faces", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6;
                double x = Math.cos(angle) * 18.0;
                double z = Math.sin(angle) * 18.0;
                double y = 12.0 + Math.random() * 4.0; // Y+12 to Y+16

                Location loc = center.clone().add(x, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SOUL_SAND);
                // Random rotation on X and Y axes (+-30 degrees)
                float rotX = (float) ((Math.random() - 0.5) * 1.05); // ~30 deg
                float rotY = (float) ((Math.random() - 0.5) * 1.05);
                h.rotate(rotX + rotY, rotX > 0 ? 1 : -1, rotY > 0 ? 1 : -1, 0);
                h.glow(0, 150, 255).interpolation(3, 0);
                faceBlocks.add(h);
                spawnedEntities.add(h.entity());

                // Random sway parameters
                swayPeriods[i] = 80 + (float) (Math.random() * 70); // 80-150 ticks
                swayAxes[i] = (float) (Math.random() * Math.PI);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.3f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Independent sway for each face
            for (int i = 0; i < faceBlocks.size(); i++) {
                float swayAmount = (float) Math.sin(ticksAlive * Math.PI * 2.0 / swayPeriods[i]) * 0.4f;
                float prevSway = (float) Math.sin((ticksAlive - 1) * Math.PI * 2.0 / swayPeriods[i]) * 0.4f;
                float deltaSway = swayAmount - prevSway;
                BlockDisplay bd = faceBlocks.get(i).entity();
                float swayX = (float) Math.cos(swayAxes[i]) * deltaSway;
                float swayZ = (float) Math.sin(swayAxes[i]) * deltaSway;
                bd.teleport(bd.getLocation().add(swayX, 0, swayZ));
            }

            // Portal particle "threads" from each block to Y+30
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle h : faceBlocks) {
                    Location blockLoc = h.entity().getLocation();
                    Location ceiling = blockLoc.clone().add(0, 30 - blockLoc.getY() + center.getY(), 0);
                    // Draw 5 particles along the thread
                    for (int p = 0; p < 5; p++) {
                        double t = p / 4.0;
                        Location point = blockLoc.clone().add(
                                0, (ceiling.getY() - blockLoc.getY()) * t, 0);
                        w.spawnParticle(Particle.PORTAL, point, 1, 0.05, 0.05, 0.05, 0);
                    }
                }
            }

            // Soul fire flame from top of each block — drifting upward (cold fire)
            if (ticksAlive % 20 == 0) {
                for (BlockDisplayHandle h : faceBlocks) {
                    Location flameLoc = h.entity().getLocation().add(0.5, 1.0, 0.5);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, flameLoc, 2, 0.1, 0.3, 0.1, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HangingSoulSandFaces(plugin); }
    }

    // ================================================================
    // 14. OBSIDIAN STALACTITE ARRAY — 7 stalactites at Y+20 to Y+30,
    //     tapering point-down, slowly descending over fight duration
    // ================================================================
    public static class ObsidianStalactiteArray extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> stalactites = new ArrayList<>();
        private final double[] baseYPositions = new double[7];
        private float descentAccumulated = 0;

        public ObsidianStalactiteArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("obsidian_stalactites", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(5000);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 7 stalactites at various positions above arena
            double[][] positions = {
                {0, 0}, {5, -5}, {-5, -5}, {8, 3}, {-8, 3},
                {0, -10}, {0, 8}
            };

            for (int s = 0; s < 7; s++) {
                double startY = 60; // Drop from Y+60
                baseYPositions[s] = 26 + Math.random() * 4; // Y+26 to Y+30

                List<BlockDisplayHandle> stalactite = new ArrayList<>();
                float[] scales = {1.0f, 0.85f, 0.70f, 0.50f};

                for (int block = 0; block < 4; block++) {
                    Location loc = center.clone().add(
                            positions[s][0], startY - block, positions[s][1]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    h.scale(scales[block], 1.0f, scales[block])
                     .glow(200, 0, 50).interpolation(3, 0);
                    stalactite.add(h);
                    spawnedEntities.add(h.entity());
                }

                stalactites.add(stalactite);
            }

            DisplayBuilder.playSound(center.clone().add(0, 28, 0),
                    Sound.BLOCK_STONE_PLACE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            double[][] positions = {
                {0, 0}, {5, -5}, {-5, -5}, {8, 3}, {-8, 3},
                {0, -10}, {0, 8}
            };

            // Drop from Y+60 to final position over 80 ticks
            if (ticksAlive <= 80) {
                float t = ticksAlive / 80.0f;
                float eased = t * t * t; // Gravity cubic ease-in
                for (int s = 0; s < stalactites.size(); s++) {
                    double targetY = baseYPositions[s];
                    double currentY = 60.0 + (targetY - 60.0) * eased;
                    List<BlockDisplayHandle> stal = stalactites.get(s);
                    for (int block = 0; block < stal.size(); block++) {
                        Location loc = center.clone().add(
                                positions[s][0], currentY - block, positions[s][1]);
                        stal.get(block).entity().teleport(loc);
                    }
                }
            }

            // Slow descent after settling: 0.5 blocks per 400 ticks
            if (ticksAlive > 80) {
                descentAccumulated += 0.5f / 400.0f;
                for (int s = 0; s < stalactites.size(); s++) {
                    for (BlockDisplayHandle h : stalactites.get(s)) {
                        h.entity().teleport(
                                h.entity().getLocation().add(0, -0.5 / 400.0, 0));
                    }
                }
            }

            // Dripping lava from stalactite tips (3/sec -> every 7 ticks)
            if (ticksAlive % 7 == 0 && ticksAlive > 80) {
                for (List<BlockDisplayHandle> stal : stalactites) {
                    if (!stal.isEmpty()) {
                        BlockDisplayHandle tip = stal.get(stal.size() - 1);
                        Location drip = tip.entity().getLocation().add(0, -0.5, 0);
                        w.spawnParticle(Particle.DRIPPING_LAVA, drip, 3, 0.1, 0, 0.1, 0);
                    }
                }
            }

            // Sound from stalactite tips every 300 ticks
            if (ticksAlive % 300 == 0 && ticksAlive > 80) {
                for (List<BlockDisplayHandle> stal : stalactites) {
                    if (!stal.isEmpty()) {
                        Location tipLoc = stal.get(stal.size() - 1).entity().getLocation();
                        DisplayBuilder.playSound(tipLoc,
                                Sound.BLOCK_DEEPSLATE_BREAK, 0.3f, 0.5f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ObsidianStalactiteArray(plugin); }
    }

    // ================================================================
    // 15. RED GLASS CEILING PANEL GRID — 4x4 grid of flat panes at Y+30,
    //     checkerboard red/orange, undulating bob, slow descent
    // ================================================================
    public static class RedGlassCeilingGrid extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> panels = new ArrayList<>();

        public RedGlassCeilingGrid(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("red_glass_ceiling", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(4.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(5000);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 4x4 grid of 5x0.1x5 panels at Y+60 (descend to Y+30)
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) {
                    double x = -10.0 + col * 5.0 + 2.5;
                    double z = -10.0 + row * 5.0 + 2.5;
                    boolean isRed = (row + col) % 2 == 0;
                    Material mat = isRed ? Material.RED_STAINED_GLASS : Material.ORANGE_STAINED_GLASS;

                    Location loc = center.clone().add(x, 60, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(5.0f, 0.1f, 5.0f).glow(200, 0, 50).interpolation(3, 0);
                    panels.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center.clone().add(0, 30, 0),
                    Sound.BLOCK_GLASS_BREAK, 0.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Descent from Y+60 to Y+30 over 80 ticks
            if (ticksAlive <= 80) {
                float t = ticksAlive / 80.0f;
                float eased = t * t;
                float currentY = 60.0f + (30.0f - 60.0f) * eased;
                for (int i = 0; i < panels.size(); i++) {
                    int row = i / 4;
                    int col = i % 4;
                    double x = -10.0 + col * 5.0 + 2.5;
                    double z = -10.0 + row * 5.0 + 2.5;
                    panels.get(i).entity().teleport(center.clone().add(x, currentY, z));
                }
            }

            // Slow ongoing descent: 0.1 blocks per 200 ticks
            if (ticksAlive > 80) {
                for (BlockDisplayHandle h : panels) {
                    h.entity().teleport(h.entity().getLocation().add(0, -0.1 / 200.0, 0));
                }
            }

            // Individual panel bob: +-0.2 blocks, 60-tick period, offset per panel
            if (ticksAlive > 80) {
                for (int i = 0; i < panels.size(); i++) {
                    float bobOffset = i * 4;
                    float bob = (float) Math.sin((ticksAlive + bobOffset) * Math.PI / 30.0) * 0.2f;
                    float prevBob = (float) Math.sin((ticksAlive - 1 + bobOffset) * Math.PI / 30.0) * 0.2f;
                    panels.get(i).entity().teleport(
                            panels.get(i).entity().getLocation().add(0, bob - prevBob, 0));
                }
            }

            // Lava particles between panels (2/sec per panel = every 10 ticks)
            if (ticksAlive % 10 == 0 && ticksAlive > 80) {
                for (int i = 0; i < panels.size(); i++) {
                    Location pLoc = panels.get(i).entity().getLocation();
                    w.spawnParticle(Particle.LAVA, pLoc.clone().add(2.5, -0.1, 2.5), 2, 1, 0, 1, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RedGlassCeilingGrid(plugin); }
    }

    // ================================================================
    // 16. CRYING OBSIDIAN CROWN — 18-block ring at Y+10, radius 8,
    //     rotating Y-axis creating helix drip pattern
    // ================================================================
    public static class CryingObsidianCrown extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> crownBlocks = new ArrayList<>();

        public CryingObsidianCrown(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crying_obsidian_crown", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(5.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(6000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 18 crying obsidian blocks in ring, radius 8, at Y+10
            for (int i = 0; i < 18; i++) {
                double angle = (2 * Math.PI * i) / 18;
                double x = Math.cos(angle) * 8.0;
                double z = Math.sin(angle) * 8.0;

                Location loc = center.clone().add(x, 10, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                // 15 degree outward radial tilt
                float tiltAngle = 0.26f;
                float axisX = (float) Math.cos(angle);
                float axisZ = (float) Math.sin(angle);
                h.rotate(tiltAngle, axisX, 0, axisZ);
                h.glow(128, 0, 255).interpolation(3, 0);
                crownBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, 10, 0),
                    Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Ring Y-axis rotation: 0.8 deg/tick
            float rotAngle = (float) Math.toRadians(ticksAlive * 0.8);
            for (int i = 0; i < crownBlocks.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 18 + rotAngle;
                double x = Math.cos(baseAngle) * 8.0;
                double z = Math.sin(baseAngle) * 8.0;
                Location target = center.clone().add(x, 10, z);
                crownBlocks.get(i).entity().teleport(target);
            }

            // Continuous dripping from each block — creates helix as ring turns
            if (ticksAlive % 3 == 0) {
                for (BlockDisplayHandle h : crownBlocks) {
                    Location drip = h.entity().getLocation().add(0, -0.5, 0);
                    w.spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, drip, 2, 0.1, 0.2, 0.1, 0);
                }
            }

            // Crimson dust accent
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.crimsonDust(center.clone().add(0, 10, 0), 8, 8.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CryingObsidianCrown(plugin); }
    }

    // ================================================================
    // 17. NETHER WART GROUND ARRAY — 24 nether wart blocks at ground level,
    //     ripple eruption from center, breathing pulse animation
    // ================================================================
    public static class NetherWartGroundArray extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> wartBlocks = new ArrayList<>();
        private final double[] baseYValues = new double[24];
        private final float[] baseScales = new float[24];

        public NetherWartGroundArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nether_wart_array", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(4.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(6000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 24 blocks scattered across arena floor, avoiding center (radius < 5)
            for (int i = 0; i < 24; i++) {
                double angle = (2 * Math.PI * i) / 24 + (Math.random() - 0.5) * 0.3;
                double radius = 5.0 + Math.random() * 12.0; // radius 5-17
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = Math.random() * 3.0; // Y+0 to Y+3
                baseYValues[i] = y;

                // Varied scale
                float scale = 0.6f + (float) (Math.random() * 0.7); // 0.6 to 1.3
                baseScales[i] = scale;

                Location loc = center.clone().add(x, -1, z); // Start below ground
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHER_WART_BLOCK);
                h.scale(scale, scale, scale).glow(200, 0, 50).interpolation(3, 0);
                wartBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.7f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Ripple eruption: each block rises over 20 ticks, staggered 2 ticks apart
            if (ticksAlive <= 70) {
                for (int i = 0; i < wartBlocks.size(); i++) {
                    int startTick = i * 2;
                    if (ticksAlive < startTick) continue;
                    int elapsed = Math.min(ticksAlive - startTick, 20);
                    float t = elapsed / 20.0f;

                    double targetY = baseYValues[i];
                    double currentY = -1.0 + (targetY + 1.0) * t;
                    BlockDisplay bd = wartBlocks.get(i).entity();
                    Location loc = bd.getLocation();
                    // Only adjust Y to target
                    bd.teleport(loc.clone().add(0, (currentY - (loc.getY() - center.getY())) * 0.3, 0));
                }
            }

            // Breathing pulse: scale 1.0 -> 1.15 -> 1.0, 40-tick period
            if (ticksAlive > 70) {
                for (int i = 0; i < wartBlocks.size(); i++) {
                    float breathe = 1.0f + (float) Math.sin(ticksAlive * Math.PI / 20.0) * 0.15f;
                    float scaledSize = baseScales[i] * breathe;
                    wartBlocks.get(i).scale(scaledSize, scaledSize, scaledSize);
                    wartBlocks.get(i).interpolation(3, 0);
                }
            }

            // Flame wisps between clusters at ground level
            if (ticksAlive % 10 == 0 && ticksAlive > 70) {
                for (int i = 0; i < wartBlocks.size(); i += 3) {
                    Location flameLoc = wartBlocks.get(i).entity().getLocation().add(0.5, 0.3, 0.5);
                    w.spawnParticle(Particle.FLAME, flameLoc, 2, 0.5, 0.1, 0.5, 0);
                }
            }

            // Spore blossom particles (alien contrast in End dimension)
            if (ticksAlive % 15 == 0 && ticksAlive > 70) {
                for (BlockDisplayHandle h : wartBlocks) {
                    Location sporeLoc = h.entity().getLocation().add(0.5, 0.8, 0.5);
                    w.spawnParticle(Particle.SPORE_BLOSSOM_AIR, sporeLoc, 1, 0.2, 0.1, 0.2, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NetherWartGroundArray(plugin); }
    }
}
