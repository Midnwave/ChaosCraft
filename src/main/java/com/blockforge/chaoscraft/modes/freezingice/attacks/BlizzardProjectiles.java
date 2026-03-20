package com.blockforge.chaoscraft.modes.freezingice.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Freezing Ice Mode — BLIZZARD PROJECTILE ATTACKS
 * 13 ice-themed BlockDisplay attacks: projectiles flying through the air.
 * Each uses 10+ block displays with ICE materials, SNOWFLAKE / END_ROD particles,
 * and ice-blue dust RGB(100, 180, 255).
 *
 * Color palette:
 * - Ice blue: RGB(100, 180, 255)
 * - Frost white: RGB(200, 220, 255)
 * - Deep ice: RGB(60, 120, 200)
 */
public final class BlizzardProjectiles {
    private BlizzardProjectiles() {}

    private static final Material[] ICE_MATS = {
        Material.BLUE_ICE, Material.PACKED_ICE, Material.ICE,
        Material.SNOW_BLOCK, Material.PRISMARINE, Material.WHITE_CONCRETE
    };

    private static Material iceMat(int i) { return ICE_MATS[Math.abs(i) % ICE_MATS.length]; }

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new IceShardBarrage(plugin));
        registry.register(new HailStones(plugin));
        registry.register(new FrostDebris(plugin));
        registry.register(new IcicleRain(plugin));
        registry.register(new SnowballVolley(plugin));
        registry.register(new FrostMissile(plugin));
        registry.register(new CrystalShrapnel(plugin));
        registry.register(new BlizzardWall(plugin));
        registry.register(new IceTornado(plugin));
        registry.register(new FrostComet(plugin));
        registry.register(new GlacialBullets(plugin));
        registry.register(new IceBoomerang(plugin));
        registry.register(new FrostNova(plugin));
    }

    // ================================================================
    // 1. ICE SHARD BARRAGE — 12 sharp angular shards flying horizontally
    //    from one direction, staggered, sweep across arena
    // ================================================================
    public static class IceShardBarrage extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> shards = new ArrayList<>();
        private final List<Double> offsetsZ = new ArrayList<>();
        private final List<Integer> spawnDelays = new ArrayList<>();
        private final List<Float> xPositions = new ArrayList<>();
        private static final int SHARD_COUNT = 12;
        private static final float START_X = -18.0f;
        private static final float SPEED = 0.6f;

        public IceShardBarrage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_shard_barrage", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < SHARD_COUNT; i++) {
                double oz = (i - SHARD_COUNT / 2.0) * 1.2;
                offsetsZ.add(oz);
                spawnDelays.add(i * 3);
                xPositions.add(START_X);
                shards.add(null);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < SHARD_COUNT; i++) {
                if (tick < spawnDelays.get(i)) continue;

                // Spawn shard on its delay tick
                if (shards.get(i) == null) {
                    Location spawnLoc = c.clone().add(START_X, 1.5 + (i % 3) * 0.5, offsetsZ.get(i));
                    BlockDisplayHandle handle = displayBuilder.spawnBlock(spawnLoc, iceMat(i));
                    handle.scale(0.4f, 0.3f, 1.8f)
                          .glow(100, 180, 255)
                          .interpolation(2, 0);
                    spawnedEntities.add(handle.entity());
                    shards.set(i, handle);
                    DisplayBuilder.playSound(spawnLoc, Sound.BLOCK_GLASS_BREAK, 0.5f, 1.4f + (float)(Math.random() * 0.4));
                }

                // Move horizontally
                float x = xPositions.get(i) + SPEED;
                xPositions.set(i, x);

                BlockDisplayHandle shard = shards.get(i);
                Location newLoc = c.clone().add(x, 1.5 + (i % 3) * 0.5, offsetsZ.get(i));
                shard.entity().teleport(newLoc);

                // Trailing frost particles
                if (tick % 2 == 0) {
                    DisplayBuilder.dustParticles(newLoc, 3, 0.15, 100, 180, 255, 1.0f);
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, newLoc, 2, 0.1, 0.1, 0.1, 0.01);
                }

                // Damage at shard location
                if (tick % 10 == 0 && x > -8 && x < 8) {
                    triggerImpactDamage(newLoc);
                }
            }

            // Ambient wind sound
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.ITEM_ELYTRA_FLYING, 0.6f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new IceShardBarrage(plugin); }
    }

    // ================================================================
    // 2. HAIL STONES — 10 large hailstone spheres (scale 1.5) falling
    //    from Y+15, impact damage on ground hit
    // ================================================================
    public static class HailStones extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> stones = new ArrayList<>();
        private final List<Float> currentY = new ArrayList<>();
        private final List<Double> offsetsX = new ArrayList<>();
        private final List<Double> offsetsZ = new ArrayList<>();
        private final List<Float> fallSpeeds = new ArrayList<>();
        private final List<Integer> spawnDelays = new ArrayList<>();
        private final List<Boolean> impacted = new ArrayList<>();
        private static final int STONE_COUNT = 10;
        private static final float START_Y = 15.0f;

        public HailStones(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hail_stones", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(55.0);
            config.setImpactRadius(7.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < STONE_COUNT; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = Math.random() * 6.0;
                offsetsX.add(Math.cos(angle) * dist);
                offsetsZ.add(Math.sin(angle) * dist);
                fallSpeeds.add(0.2f + (float)(Math.random() * 0.2));
                currentY.add(START_Y);
                spawnDelays.add(i * 6);
                impacted.add(false);
                stones.add(null);
            }

            DisplayBuilder.playSound(center.clone().add(0, START_Y, 0), Sound.WEATHER_RAIN_ABOVE, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < STONE_COUNT; i++) {
                if (tick < spawnDelays.get(i)) continue;
                if (impacted.get(i)) continue;

                if (stones.get(i) == null) {
                    Location spawnLoc = c.clone().add(offsetsX.get(i), START_Y, offsetsZ.get(i));
                    BlockDisplayHandle handle = displayBuilder.spawnBlock(spawnLoc, iceMat(i));
                    handle.scale(1.5f, 1.5f, 1.5f)
                          .glow(100, 180, 255)
                          .interpolation(2, 0);
                    spawnedEntities.add(handle.entity());
                    stones.set(i, handle);
                    DisplayBuilder.playSound(spawnLoc, Sound.BLOCK_GLASS_BREAK, 0.6f, 0.6f);
                }

                // Fall
                float y = currentY.get(i) - fallSpeeds.get(i);
                currentY.set(i, y);

                BlockDisplayHandle stone = stones.get(i);
                Location newLoc = c.clone().add(offsetsX.get(i), y, offsetsZ.get(i));
                stone.entity().teleport(newLoc);

                // Trailing particles
                if (tick % 3 == 0) {
                    DisplayBuilder.dustParticles(newLoc.clone().add(0, 0.75, 0), 4, 0.3, 200, 220, 255, 1.2f);
                    w.spawnParticle(Particle.SNOWFLAKE, newLoc.clone().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0.02);
                }

                // Ground impact
                if (y <= 0) {
                    currentY.set(i, 0f);
                    impacted.set(i, true);

                    Location impactLoc = c.clone().add(offsetsX.get(i), 0, offsetsZ.get(i));
                    triggerImpactDamage(impactLoc);

                    // Shockwave ring
                    DisplayBuilder.particleRing(impactLoc, 4.0, Particle.DUST, 28,
                            new Particle.DustOptions(Color.fromRGB(100, 180, 255), 1.5f));
                    w.spawnParticle(Particle.BLOCK, impactLoc, 35, 2.0, 0.4, 2.0, 0.2,
                            Material.BLUE_ICE.createBlockData());
                    w.spawnParticle(Particle.SNOWFLAKE, impactLoc, 20, 2.0, 1.0, 2.0, 0.05);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.4f);
                }
            }

            // Ambient hail ambiance
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 0.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HailStones(plugin); }
    }

    // ================================================================
    // 3. FROST DEBRIS — 14 random ice objects flying diagonally in wind
    //    with tumbling rotation, scattered approach angles
    // ================================================================
    public static class FrostDebris extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> debris = new ArrayList<>();
        private final List<Double> dirX = new ArrayList<>();
        private final List<Double> dirY = new ArrayList<>();
        private final List<Double> dirZ = new ArrayList<>();
        private final List<Double> posX = new ArrayList<>();
        private final List<Double> posY = new ArrayList<>();
        private final List<Double> posZ = new ArrayList<>();
        private final List<Float> rotSpeeds = new ArrayList<>();
        private static final int DEBRIS_COUNT = 14;

        public FrostDebris(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_debris", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(35.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < DEBRIS_COUNT; i++) {
                // Random start position at edge of arena
                double angle = Math.random() * Math.PI * 2;
                double startDist = 12.0 + Math.random() * 4.0;
                double sx = Math.cos(angle) * startDist;
                double sy = 2.0 + Math.random() * 8.0;
                double sz = Math.sin(angle) * startDist;
                posX.add(sx);
                posY.add(sy);
                posZ.add(sz);

                // Direction toward center with slight randomness
                double speed = 0.25 + Math.random() * 0.2;
                dirX.add(-Math.cos(angle) * speed + (Math.random() - 0.5) * 0.1);
                dirY.add((Math.random() - 0.5) * 0.08);
                dirZ.add(-Math.sin(angle) * speed + (Math.random() - 0.5) * 0.1);
                rotSpeeds.add(0.1f + (float)(Math.random() * 0.3));

                Location spawnLoc = center.clone().add(sx, sy, sz);
                float scaleX = 0.4f + (float)(Math.random() * 0.8);
                float scaleY = 0.4f + (float)(Math.random() * 0.6);
                float scaleZ = 0.4f + (float)(Math.random() * 0.8);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(spawnLoc, iceMat(i));
                handle.scale(scaleX, scaleY, scaleZ)
                      .glow(100, 180, 255)
                      .interpolation(2, 0);
                spawnedEntities.add(handle.entity());
                debris.add(handle);
            }

            DisplayBuilder.playSound(center, Sound.ITEM_ELYTRA_FLYING, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < DEBRIS_COUNT; i++) {
                double px = posX.get(i) + dirX.get(i);
                double py = posY.get(i) + dirY.get(i);
                double pz = posZ.get(i) + dirZ.get(i);
                posX.set(i, px);
                posY.set(i, py);
                posZ.set(i, pz);

                BlockDisplayHandle piece = debris.get(i);
                Location newLoc = c.clone().add(px, py, pz);
                piece.entity().teleport(newLoc);

                // Tumbling rotation via transformation
                float rot = tick * rotSpeeds.get(i);
                Transformation t = new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(rot, 1, 0.5f, 0),
                    piece.entity().getTransformation().getScale(),
                    new AxisAngle4f(0, 0, 0, 1)
                );
                piece.entity().setTransformation(t);
                piece.entity().setInterpolationDuration(3);
                piece.entity().setInterpolationDelay(0);

                // Trailing frost
                if (tick % 3 == 0) {
                    DisplayBuilder.dustParticles(newLoc, 2, 0.2, 100, 180, 255, 0.8f);
                    w.spawnParticle(Particle.SNOWFLAKE, newLoc, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }

            // Damage near center where debris converges
            if (tick % 8 == 0) {
                triggerImpactDamage(c.clone().add(0, 2, 0));
            }

            if (tick % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.ITEM_ELYTRA_FLYING, 0.5f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FrostDebris(plugin); }
    }

    // ================================================================
    // 4. ICICLE RAIN — 16 thin icicles (scale 0.3x0.3x2.0) falling
    //    straight down continuously, re-spawning at top when they hit ground
    // ================================================================
    public static class IcicleRain extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> icicles = new ArrayList<>();
        private final List<Float> currentY = new ArrayList<>();
        private final List<Double> offsetsX = new ArrayList<>();
        private final List<Double> offsetsZ = new ArrayList<>();
        private final List<Float> fallSpeeds = new ArrayList<>();
        private static final int ICICLE_COUNT = 16;
        private static final float START_Y = 14.0f;

        public IcicleRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("icicle_rain", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(38.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(260);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(45.0);
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < ICICLE_COUNT; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = Math.random() * 7.0;
                double ox = Math.cos(angle) * dist;
                double oz = Math.sin(angle) * dist;
                offsetsX.add(ox);
                offsetsZ.add(oz);
                // Stagger initial Y so they don't all fall at once
                float startY = START_Y - (float)(Math.random() * 10.0);
                currentY.add(startY);
                fallSpeeds.add(0.3f + (float)(Math.random() * 0.2));

                Location spawnLoc = center.clone().add(ox, startY, oz);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(spawnLoc, iceMat(i));
                handle.scale(0.3f, 2.0f, 0.3f)
                      .glow(200, 220, 255)
                      .interpolation(2, 0);
                spawnedEntities.add(handle.entity());
                icicles.add(handle);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.6f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < ICICLE_COUNT; i++) {
                float y = currentY.get(i) - fallSpeeds.get(i);

                // Reset to top when hitting ground
                if (y <= 0) {
                    Location impactLoc = c.clone().add(offsetsX.get(i), 0, offsetsZ.get(i));
                    triggerImpactDamage(impactLoc);

                    w.spawnParticle(Particle.SNOWFLAKE, impactLoc, 8, 0.5, 0.2, 0.5, 0.03);
                    DisplayBuilder.dustParticles(impactLoc, 5, 0.4, 100, 180, 255, 1.0f);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_GLASS_BREAK, 0.4f, 1.8f);

                    // Re-randomize position and reset
                    double angle = Math.random() * Math.PI * 2;
                    double dist = Math.random() * 7.0;
                    offsetsX.set(i, Math.cos(angle) * dist);
                    offsetsZ.set(i, Math.sin(angle) * dist);
                    y = START_Y;
                }

                currentY.set(i, y);
                BlockDisplayHandle icicle = icicles.get(i);
                Location newLoc = c.clone().add(offsetsX.get(i), y, offsetsZ.get(i));
                icicle.entity().teleport(newLoc);

                // Trailing frost particles
                if (tick % 4 == 0) {
                    w.spawnParticle(Particle.END_ROD, newLoc.clone().add(0, 1, 0), 1, 0.05, 0.3, 0.05, 0.005);
                }
            }

            // Ambient cracking
            if (tick % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.4f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new IcicleRain(plugin); }
    }

    // ================================================================
    // 5. SNOWBALL VOLLEY — 8 snow blocks launched in arc toward player,
    //    spread pattern, lobbed parabolic trajectory
    // ================================================================
    public static class SnowballVolley extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> balls = new ArrayList<>();
        private final List<Double> startX = new ArrayList<>();
        private final List<Double> startZ = new ArrayList<>();
        private final List<Double> targetX = new ArrayList<>();
        private final List<Double> targetZ = new ArrayList<>();
        private final List<Float> arcProgress = new ArrayList<>();
        private final List<Boolean> impacted = new ArrayList<>();
        private static final int BALL_COUNT = 8;
        private static final float ARC_HEIGHT = 8.0f;
        private static final float ARC_SPEED = 0.025f;
        private double playerOffX, playerOffZ;

        public SnowballVolley(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("snowball_volley", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(36.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(230);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(50.0);
            config.setImpactRadius(6.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Target nearest player or center
            Player target = getTargetPlayer();
            if (target != null && target.isOnline()) {
                playerOffX = target.getLocation().getX() - center.getX();
                playerOffZ = target.getLocation().getZ() - center.getZ();
            } else {
                playerOffX = 0;
                playerOffZ = 0;
            }

            // Launch point 10 blocks away
            double launchAngle = Math.atan2(playerOffZ, playerOffX) + Math.PI;

            for (int i = 0; i < BALL_COUNT; i++) {
                double spreadAngle = launchAngle + (i - BALL_COUNT / 2.0) * 0.15;
                double launchDist = 10.0;
                double sx = Math.cos(spreadAngle) * launchDist;
                double sz = Math.sin(spreadAngle) * launchDist;
                startX.add(sx);
                startZ.add(sz);

                // Target with spread
                double tx = playerOffX + (Math.random() - 0.5) * 3.0;
                double tz = playerOffZ + (Math.random() - 0.5) * 3.0;
                targetX.add(tx);
                targetZ.add(tz);
                arcProgress.add(0.0f);
                impacted.add(false);

                Location spawnLoc = center.clone().add(sx, 1.0, sz);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(spawnLoc, Material.SNOW_BLOCK);
                handle.scale(0.8f, 0.8f, 0.8f)
                      .glow(200, 220, 255)
                      .interpolation(2, 0);
                spawnedEntities.add(handle.entity());
                balls.add(handle);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_SNOWBALL_THROW, 1.2f, 0.6f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < BALL_COUNT; i++) {
                if (impacted.get(i)) continue;

                float progress = arcProgress.get(i) + ARC_SPEED;
                arcProgress.set(i, progress);

                if (progress >= 1.0f) {
                    impacted.set(i, true);
                    Location impactLoc = c.clone().add(targetX.get(i), 0, targetZ.get(i));
                    triggerImpactDamage(impactLoc);

                    DisplayBuilder.particleRing(impactLoc, 3.0, Particle.DUST, 24,
                            new Particle.DustOptions(Color.fromRGB(200, 220, 255), 1.2f));
                    w.spawnParticle(Particle.SNOWFLAKE, impactLoc, 15, 1.5, 0.5, 1.5, 0.04);
                    w.spawnParticle(Particle.BLOCK, impactLoc, 20, 1.0, 0.3, 1.0, 0.1,
                            Material.SNOW_BLOCK.createBlockData());
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_SNOW_BREAK, 0.8f, 0.5f);
                    continue;
                }

                // Parabolic arc: lerp XZ, parabola Y
                double lx = startX.get(i) + (targetX.get(i) - startX.get(i)) * progress;
                double lz = startZ.get(i) + (targetZ.get(i) - startZ.get(i)) * progress;
                double ly = ARC_HEIGHT * 4.0 * progress * (1.0 - progress); // parabola

                BlockDisplayHandle ball = balls.get(i);
                Location newLoc = c.clone().add(lx, ly, lz);
                ball.entity().teleport(newLoc);

                if (tick % 3 == 0) {
                    w.spawnParticle(Particle.SNOWFLAKE, newLoc, 2, 0.1, 0.1, 0.1, 0.01);
                    DisplayBuilder.dustParticles(newLoc, 2, 0.15, 200, 220, 255, 0.8f);
                }
            }

            if (tick % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_SNOWBALL_THROW, 0.5f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SnowballVolley(plugin); }
    }

    // ================================================================
    // 6. FROST MISSILE — 10 blocks forming single large projectile,
    //    HOMING on nearest player, accelerates over time
    // ================================================================
    public static class FrostMissile extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> parts = new ArrayList<>();
        private final List<Double> localX = new ArrayList<>();
        private final List<Double> localY = new ArrayList<>();
        private final List<Double> localZ = new ArrayList<>();
        private double missileX, missileY, missileZ;
        private double velX, velY, velZ;
        private boolean hit = false;
        private static final int PART_COUNT = 10;

        public FrostMissile(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_missile", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(45.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(65.0);
            config.setImpactRadius(8.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Start offset from center
            missileX = -14.0;
            missileY = 6.0;
            missileZ = 0.0;
            velX = 0.3;
            velY = 0.0;
            velZ = 0.0;

            // Build missile shape: nose cone + body
            double[][] offsets = {
                {0, 0, 0}, {-0.8, 0, 0}, {-1.6, 0, 0}, {-2.4, 0, 0}, // body
                {-0.8, 0.5, 0}, {-0.8, -0.5, 0}, {-0.8, 0, 0.5}, {-0.8, 0, -0.5}, // ring
                {-1.6, 0.4, 0.4}, {-1.6, -0.4, -0.4} // tail fins
            };

            for (int i = 0; i < PART_COUNT; i++) {
                localX.add(offsets[i][0]);
                localY.add(offsets[i][1]);
                localZ.add(offsets[i][2]);

                Location spawnLoc = center.clone().add(missileX + offsets[i][0], missileY + offsets[i][1], missileZ + offsets[i][2]);
                Material mat = i == 0 ? Material.BLUE_ICE : iceMat(i);
                float s = i == 0 ? 0.6f : 0.5f;
                BlockDisplayHandle handle = displayBuilder.spawnBlock(spawnLoc, mat);
                handle.scale(s, s, s)
                      .glow(100, 180, 255)
                      .interpolation(2, 0);
                spawnedEntities.add(handle.entity());
                parts.add(handle);
            }

            DisplayBuilder.playSound(center.clone().add(missileX, missileY, 0), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (hit) return;
            World w = c.getWorld();

            // Home toward target player
            Player target = getTargetPlayer();
            if (target != null && target.isOnline()) {
                double tx = target.getLocation().getX() - c.getX() - missileX;
                double ty = target.getLocation().getY() - c.getY() - missileY + 1.0;
                double tz = target.getLocation().getZ() - c.getZ() - missileZ;
                double dist = Math.sqrt(tx * tx + ty * ty + tz * tz);
                if (dist > 0.5) {
                    double homingStr = 0.02 + tick * 0.0005; // accelerating homing
                    velX += (tx / dist) * homingStr;
                    velY += (ty / dist) * homingStr;
                    velZ += (tz / dist) * homingStr;
                }
            }

            // Cap speed
            double speed = Math.sqrt(velX * velX + velY * velY + velZ * velZ);
            double maxSpeed = 0.8;
            if (speed > maxSpeed) {
                velX = (velX / speed) * maxSpeed;
                velY = (velY / speed) * maxSpeed;
                velZ = (velZ / speed) * maxSpeed;
            }

            missileX += velX;
            missileY += velY;
            missileZ += velZ;

            // Move all parts
            for (int i = 0; i < PART_COUNT; i++) {
                Location newLoc = c.clone().add(missileX + localX.get(i), missileY + localY.get(i), missileZ + localZ.get(i));
                parts.get(i).entity().teleport(newLoc);
            }

            // Trail particles
            Location tailLoc = c.clone().add(missileX - 2.4, missileY, missileZ);
            if (tick % 2 == 0) {
                w.spawnParticle(Particle.END_ROD, tailLoc, 4, 0.2, 0.2, 0.2, 0.02);
                DisplayBuilder.dustParticles(tailLoc, 5, 0.3, 100, 180, 255, 1.2f);
                w.spawnParticle(Particle.SNOWFLAKE, tailLoc, 3, 0.3, 0.3, 0.3, 0.02);
            }

            // Proximity check for impact
            Location missileLoc = c.clone().add(missileX, missileY, missileZ);
            if (missileY <= 0.5 || (target != null && target.isOnline()
                    && missileLoc.distanceSquared(target.getLocation()) < 9.0)) {
                hit = true;
                triggerImpactDamage(missileLoc);

                DisplayBuilder.particleRing(missileLoc, 5.0, Particle.DUST, 36,
                        new Particle.DustOptions(Color.fromRGB(100, 180, 255), 2.0f));
                w.spawnParticle(Particle.SNOWFLAKE, missileLoc, 40, 3.0, 2.0, 3.0, 0.08);
                w.spawnParticle(Particle.END_ROD, missileLoc, 25, 2.0, 2.0, 2.0, 0.1);
                w.spawnParticle(Particle.BLOCK, missileLoc, 40, 2.5, 1.5, 2.5, 0.2,
                        Material.BLUE_ICE.createBlockData());
                DisplayBuilder.playSound(missileLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.4f);
            }

            if (tick % 5 == 0) {
                DisplayBuilder.playSound(missileLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.3f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FrostMissile(plugin); }
    }

    // ================================================================
    // 7. CRYSTAL SHRAPNEL — 14 blocks clustered at center, pause briefly,
    //    then explode outward in all directions
    // ================================================================
    public static class CrystalShrapnel extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> shards = new ArrayList<>();
        private final List<Double> dirX = new ArrayList<>();
        private final List<Double> dirY = new ArrayList<>();
        private final List<Double> dirZ = new ArrayList<>();
        private boolean exploded = false;
        private static final int SHARD_COUNT = 14;
        private static final int CHARGE_TICKS = 40;
        private static final float EXPLODE_SPEED = 0.5f;

        public CrystalShrapnel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_shrapnel", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < SHARD_COUNT; i++) {
                // Tight cluster at center
                double ox = (Math.random() - 0.5) * 1.5;
                double oy = 2.0 + (Math.random() - 0.5) * 1.0;
                double oz = (Math.random() - 0.5) * 1.5;

                Location spawnLoc = center.clone().add(ox, oy, oz);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(spawnLoc, iceMat(i));
                handle.scale(0.5f, 0.5f, 0.5f)
                      .glow(100, 180, 255)
                      .interpolation(2, 0);
                spawnedEntities.add(handle.entity());
                shards.add(handle);

                // Pre-calculate explosion direction
                double angle = (Math.PI * 2.0 / SHARD_COUNT) * i;
                double elevAngle = (Math.random() - 0.3) * Math.PI * 0.5;
                dirX.add(Math.cos(angle) * Math.cos(elevAngle) * EXPLODE_SPEED);
                dirY.add(Math.sin(elevAngle) * EXPLODE_SPEED * 0.5);
                dirZ.add(Math.sin(angle) * Math.cos(elevAngle) * EXPLODE_SPEED);
            }

            DisplayBuilder.playSound(center.clone().add(0, 2, 0), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 1.8f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (tick < CHARGE_TICKS) {
                // Charging phase: pulse glow and converge particles
                if (tick % 5 == 0) {
                    Location clusterCenter = c.clone().add(0, 2, 0);
                    DisplayBuilder.dustParticles(clusterCenter, 8, 0.8, 60, 120, 200, 1.5f);
                    w.spawnParticle(Particle.END_ROD, clusterCenter, 5, 0.5, 0.5, 0.5, 0.02);
                }
                // Vibrate shards
                for (int i = 0; i < SHARD_COUNT; i++) {
                    double ox = (Math.random() - 0.5) * 1.5;
                    double oy = 2.0 + (Math.random() - 0.5) * 1.0;
                    double oz = (Math.random() - 0.5) * 1.5;
                    // Tighten cluster as charge progresses
                    double squeeze = 1.0 - (tick / (double) CHARGE_TICKS) * 0.7;
                    Location loc = c.clone().add(ox * squeeze, oy, oz * squeeze);
                    shards.get(i).entity().teleport(loc);
                }

                if (tick % 10 == 0) {
                    DisplayBuilder.playSound(c.clone().add(0, 2, 0), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.6f, 1.0f + tick * 0.02f);
                }
                return;
            }

            // Explosion frame
            if (!exploded) {
                exploded = true;
                Location boom = c.clone().add(0, 2, 0);
                DisplayBuilder.playSound(boom, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.6f);
                w.spawnParticle(Particle.SNOWFLAKE, boom, 30, 1.0, 1.0, 1.0, 0.1);
                DisplayBuilder.particleRing(boom, 3.0, Particle.DUST, 30,
                        new Particle.DustOptions(Color.fromRGB(100, 180, 255), 2.0f));
            }

            // Fly outward
            int explodeTick = tick - CHARGE_TICKS;
            for (int i = 0; i < SHARD_COUNT; i++) {
                double px = dirX.get(i) * explodeTick;
                double py = 2.0 + dirY.get(i) * explodeTick - 0.01 * explodeTick * explodeTick; // gravity
                double pz = dirZ.get(i) * explodeTick;

                Location newLoc = c.clone().add(px, Math.max(py, 0), pz);
                shards.get(i).entity().teleport(newLoc);

                if (explodeTick % 3 == 0) {
                    DisplayBuilder.dustParticles(newLoc, 2, 0.15, 100, 180, 255, 0.8f);
                    w.spawnParticle(Particle.SNOWFLAKE, newLoc, 1, 0.05, 0.05, 0.05, 0.01);
                }
            }

            // Expanding damage ring
            if (explodeTick % 5 == 0 && explodeTick < 30) {
                double damageRing = explodeTick * 0.4;
                DisplayBuilder.particleRing(c.clone().add(0, 1, 0), damageRing, Particle.DUST, 20,
                        new Particle.DustOptions(Color.fromRGB(100, 180, 255), 1.0f));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalShrapnel(plugin); }
    }

    // ================================================================
    // 8. BLIZZARD WALL — 16 blocks forming horizontal wall sweeping
    //    across at head height, 4 wide x 4 tall grid
    // ================================================================
    public static class BlizzardWall extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> blocks = new ArrayList<>();
        private final List<Double> localX = new ArrayList<>();
        private final List<Double> localY = new ArrayList<>();
        private float sweepZ = 0;
        private static final int BLOCK_COUNT = 16;
        private static final float WALL_WIDTH = 8.0f;
        private static final float WALL_HEIGHT = 4.0f;
        private static final float START_Z = -14.0f;
        private static final float SWEEP_SPEED = 0.25f;

        public BlizzardWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blizzard_wall", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            sweepZ = START_Z;

            // 4 columns x 4 rows = 16 blocks
            int idx = 0;
            for (int col = 0; col < 4; col++) {
                for (int row = 0; row < 4; row++) {
                    if (idx >= BLOCK_COUNT) break;
                    double lx = -WALL_WIDTH / 2 + col * (WALL_WIDTH / 3.0);
                    double ly = row * (WALL_HEIGHT / 3.0) + 0.5;
                    localX.add(lx);
                    localY.add(ly);

                    Location spawnLoc = center.clone().add(lx, ly, START_Z);
                    BlockDisplayHandle handle = displayBuilder.spawnBlock(spawnLoc, iceMat(idx));
                    handle.scale(2.2f, 1.2f, 0.6f)
                          .glow(100, 180, 255)
                          .interpolation(2, 0);
                    spawnedEntities.add(handle.entity());
                    blocks.add(handle);
                    idx++;
                }
            }

            DisplayBuilder.playSound(center.clone().add(0, 2, START_Z), Sound.ITEM_ELYTRA_FLYING, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            sweepZ += SWEEP_SPEED;

            for (int i = 0; i < blocks.size(); i++) {
                Location newLoc = c.clone().add(localX.get(i), localY.get(i), sweepZ);
                blocks.get(i).entity().teleport(newLoc);
            }

            // Leading edge particles
            if (tick % 2 == 0) {
                for (int i = 0; i < 5; i++) {
                    double px = -WALL_WIDTH / 2 + Math.random() * WALL_WIDTH;
                    double py = Math.random() * WALL_HEIGHT + 0.5;
                    Location particleLoc = c.clone().add(px, py, sweepZ + 0.5);
                    w.spawnParticle(Particle.SNOWFLAKE, particleLoc, 2, 0.3, 0.3, 0.1, 0.02);
                    DisplayBuilder.dustParticles(particleLoc, 2, 0.2, 100, 180, 255, 1.0f);
                }
            }

            // Frost trail behind wall
            if (tick % 4 == 0) {
                Location trailLoc = c.clone().add(0, 1, sweepZ - 1.0);
                w.spawnParticle(Particle.END_ROD, trailLoc, 5, WALL_WIDTH / 2, 1.0, 0.3, 0.01);
            }

            // Ambient wind
            if (tick % 15 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 2, sweepZ), Sound.ITEM_ELYTRA_FLYING, 0.6f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BlizzardWall(plugin); }
    }

    // ================================================================
    // 9. ICE TORNADO — 16 blocks orbiting in rising spiral, column
    //    moves across arena, continuous damage in radius
    // ================================================================
    public static class IceTornado extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> blocks = new ArrayList<>();
        private final List<Double> baseAngles = new ArrayList<>();
        private final List<Double> heightOffsets = new ArrayList<>();
        private final List<Double> radiusOffsets = new ArrayList<>();
        private double tornadoX, tornadoZ;
        private double moveAngle;
        private static final int BLOCK_COUNT = 16;

        public IceTornado(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_tornado", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(38.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(270);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            tornadoX = 0;
            tornadoZ = 0;
            moveAngle = Math.random() * Math.PI * 2;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                double baseAngle = (Math.PI * 2 / BLOCK_COUNT) * i;
                double height = (i / (double) BLOCK_COUNT) * 10.0; // spiral from 0 to 10
                double radius = 1.0 + (i / (double) BLOCK_COUNT) * 2.5; // widen toward top
                baseAngles.add(baseAngle);
                heightOffsets.add(height);
                radiusOffsets.add(radius);

                double ox = Math.cos(baseAngle) * radius;
                double oz = Math.sin(baseAngle) * radius;
                Location spawnLoc = center.clone().add(ox, height, oz);
                float scale = 0.5f + (float)(i / (double) BLOCK_COUNT) * 0.5f;
                BlockDisplayHandle handle = displayBuilder.spawnBlock(spawnLoc, iceMat(i));
                handle.scale(scale, scale, scale)
                      .glow(100, 180, 255)
                      .interpolation(2, 0);
                spawnedEntities.add(handle.entity());
                blocks.add(handle);
            }

            DisplayBuilder.playSound(center, Sound.ITEM_ELYTRA_FLYING, 1.2f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Move tornado across arena
            double moveSpeed = 0.12;
            tornadoX += Math.cos(moveAngle) * moveSpeed;
            tornadoZ += Math.sin(moveAngle) * moveSpeed;

            // Gentle wandering
            moveAngle += (Math.random() - 0.5) * 0.1;

            double spinSpeed = tick * 0.08;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                double angle = baseAngles.get(i) + spinSpeed;
                double radius = radiusOffsets.get(i);
                double height = heightOffsets.get(i);

                double ox = tornadoX + Math.cos(angle) * radius;
                double oy = height;
                double oz = tornadoZ + Math.sin(angle) * radius;

                Location newLoc = c.clone().add(ox, oy, oz);
                blocks.get(i).entity().teleport(newLoc);

                // Spiral particles
                if (tick % 3 == 0 && i % 3 == 0) {
                    w.spawnParticle(Particle.SNOWFLAKE, newLoc, 2, 0.2, 0.2, 0.2, 0.02);
                    DisplayBuilder.dustParticles(newLoc, 2, 0.15, 100, 180, 255, 0.8f);
                }
            }

            // Base particles (ground swirl)
            if (tick % 2 == 0) {
                Location baseLoc = c.clone().add(tornadoX, 0.5, tornadoZ);
                DisplayBuilder.particleRing(baseLoc, 2.5, Particle.DUST, 16,
                        new Particle.DustOptions(Color.fromRGB(200, 220, 255), 1.0f));
                w.spawnParticle(Particle.SNOWFLAKE, baseLoc, 5, 1.5, 0.3, 1.5, 0.03);
            }

            // Top cloud particles
            if (tick % 4 == 0) {
                Location topLoc = c.clone().add(tornadoX, 10, tornadoZ);
                w.spawnParticle(Particle.END_ROD, topLoc, 4, 2.0, 0.5, 2.0, 0.02);
            }

            // Ambient wind howl
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(c.clone().add(tornadoX, 5, tornadoZ), Sound.ITEM_ELYTRA_FLYING, 0.8f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new IceTornado(plugin); }
    }

    // ================================================================
    // 10. FROST COMET — 8 comet head + 6 tail blocks, diagonal descent
    //     from sky, massive impact explosion on ground hit
    // ================================================================
    public static class FrostComet extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> headBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> tailBlocks = new ArrayList<>();
        private double cometX, cometY, cometZ;
        private boolean impacted = false;
        private static final double VEL_X = 0.3;
        private static final double VEL_Y = -0.25;
        private static final double VEL_Z = 0.15;
        private static final int HEAD_COUNT = 8;
        private static final int TAIL_COUNT = 6;

        public FrostComet(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_comet", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(50.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(70.0);
            config.setImpactRadius(8.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            cometX = -10.0;
            cometY = 18.0;
            cometZ = -5.0;

            // Head cluster (8 blocks in sphere shape)
            double[][] headOffsets = {
                {0, 0, 0}, {0.6, 0, 0}, {-0.6, 0, 0}, {0, 0.6, 0},
                {0, -0.6, 0}, {0, 0, 0.6}, {0, 0, -0.6}, {0.4, 0.4, 0.4}
            };
            for (int i = 0; i < HEAD_COUNT; i++) {
                Location loc = center.clone().add(cometX + headOffsets[i][0], cometY + headOffsets[i][1], cometZ + headOffsets[i][2]);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(loc, i < 4 ? Material.BLUE_ICE : Material.PACKED_ICE);
                handle.scale(0.8f, 0.8f, 0.8f)
                      .glow(60, 120, 200)
                      .interpolation(2, 0);
                spawnedEntities.add(handle.entity());
                headBlocks.add(handle);
            }

            // Tail (6 blocks trailing behind, getting smaller)
            for (int i = 0; i < TAIL_COUNT; i++) {
                double tailDist = -(i + 1) * 1.2;
                Location loc = center.clone().add(cometX + tailDist * -VEL_X / 0.3, cometY + tailDist * -VEL_Y / 0.3, cometZ + tailDist * -VEL_Z / 0.3);
                float s = 0.6f - i * 0.08f;
                BlockDisplayHandle handle = displayBuilder.spawnBlock(loc, Material.WHITE_CONCRETE);
                handle.scale(s, s, s)
                      .glow(200, 220, 255)
                      .interpolation(2, 0);
                spawnedEntities.add(handle.entity());
                tailBlocks.add(handle);
            }

            DisplayBuilder.playSound(center.clone().add(cometX, cometY, cometZ), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.2f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (impacted) return;
            World w = c.getWorld();

            cometX += VEL_X;
            cometY += VEL_Y;
            cometZ += VEL_Z;

            // Move head blocks
            double[][] headOffsets = {
                {0, 0, 0}, {0.6, 0, 0}, {-0.6, 0, 0}, {0, 0.6, 0},
                {0, -0.6, 0}, {0, 0, 0.6}, {0, 0, -0.6}, {0.4, 0.4, 0.4}
            };
            for (int i = 0; i < HEAD_COUNT; i++) {
                Location newLoc = c.clone().add(cometX + headOffsets[i][0], cometY + headOffsets[i][1], cometZ + headOffsets[i][2]);
                headBlocks.get(i).entity().teleport(newLoc);
            }

            // Move tail blocks (trail behind)
            for (int i = 0; i < TAIL_COUNT; i++) {
                double tailDist = (i + 1) * 1.2;
                double tx = cometX - (VEL_X / 0.3) * tailDist;
                double ty = cometY - (VEL_Y / 0.3) * tailDist;
                double tz = cometZ - (VEL_Z / 0.3) * tailDist;
                Location tailLoc = c.clone().add(tx, ty, tz);
                tailBlocks.get(i).entity().teleport(tailLoc);
            }

            // Trail particles
            Location headLoc = c.clone().add(cometX, cometY, cometZ);
            if (tick % 2 == 0) {
                w.spawnParticle(Particle.END_ROD, headLoc, 5, 0.3, 0.3, 0.3, 0.03);
                DisplayBuilder.dustParticles(headLoc, 6, 0.5, 100, 180, 255, 1.5f);
                w.spawnParticle(Particle.SNOWFLAKE, headLoc, 4, 0.5, 0.5, 0.5, 0.03);
            }

            // Ground impact
            if (cometY <= 0.5) {
                impacted = true;
                Location impactLoc = c.clone().add(cometX, 0, cometZ);
                triggerImpactDamage(impactLoc);

                // Massive explosion visuals
                DisplayBuilder.particleRing(impactLoc, 6.0, Particle.DUST, 48,
                        new Particle.DustOptions(Color.fromRGB(100, 180, 255), 2.5f));
                DisplayBuilder.particleRing(impactLoc.clone().add(0, 0.5, 0), 4.0, Particle.DUST, 32,
                        new Particle.DustOptions(Color.fromRGB(200, 220, 255), 2.0f));
                w.spawnParticle(Particle.SNOWFLAKE, impactLoc, 60, 4.0, 2.0, 4.0, 0.1);
                w.spawnParticle(Particle.END_ROD, impactLoc, 30, 3.0, 3.0, 3.0, 0.15);
                w.spawnParticle(Particle.BLOCK, impactLoc, 50, 3.0, 1.0, 3.0, 0.3,
                        Material.BLUE_ICE.createBlockData());
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
                DisplayBuilder.playSound(impactLoc, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.3f);
            }

            if (tick % 8 == 0) {
                DisplayBuilder.playSound(headLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.4f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FrostComet(plugin); }
    }

    // ================================================================
    // 11. GLACIAL BULLETS — 20 small (scale 0.3) cubes rapid-fired
    //     from a fixed point toward player, machine-gun style
    // ================================================================
    public static class GlacialBullets extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> bullets = new ArrayList<>();
        private final List<Double> bulletVelX = new ArrayList<>();
        private final List<Double> bulletVelY = new ArrayList<>();
        private final List<Double> bulletVelZ = new ArrayList<>();
        private final List<Double> bulletPosX = new ArrayList<>();
        private final List<Double> bulletPosY = new ArrayList<>();
        private final List<Double> bulletPosZ = new ArrayList<>();
        private final List<Boolean> bulletActive = new ArrayList<>();
        private static final int BULLET_COUNT = 20;
        private static final double TURRET_X = 8.0;
        private static final double TURRET_Y = 4.0;
        private static final double TURRET_Z = 0.0;
        private int nextBullet = 0;

        public GlacialBullets(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_bullets", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(35.0);
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spawn turret marker blocks (2 large ice blocks at turret location)
            Location turretLoc = center.clone().add(TURRET_X, TURRET_Y, TURRET_Z);
            BlockDisplayHandle turretBase = displayBuilder.spawnBlock(turretLoc, Material.BLUE_ICE);
            turretBase.scale(1.0f, 1.0f, 1.0f).glow(60, 120, 200).interpolation(2, 0);
            spawnedEntities.add(turretBase.entity());

            BlockDisplayHandle turretTop = displayBuilder.spawnBlock(turretLoc.clone().add(0, 0.8, 0), Material.PACKED_ICE);
            turretTop.scale(0.6f, 0.6f, 0.6f).glow(100, 180, 255).interpolation(2, 0);
            spawnedEntities.add(turretTop.entity());

            // Pre-allocate bullet slots
            for (int i = 0; i < BULLET_COUNT; i++) {
                bullets.add(null);
                bulletVelX.add(0.0);
                bulletVelY.add(0.0);
                bulletVelZ.add(0.0);
                bulletPosX.add(TURRET_X);
                bulletPosY.add(TURRET_Y);
                bulletPosZ.add(TURRET_Z);
                bulletActive.add(false);
            }

            DisplayBuilder.playSound(turretLoc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Fire a bullet every 5 ticks
            if (tick % 5 == 0 && nextBullet < BULLET_COUNT) {
                int i = nextBullet++;

                // Aim at target player or center
                double tx = 0, ty = 1.5, tz = 0;
                Player target = getTargetPlayer();
                if (target != null && target.isOnline()) {
                    tx = target.getLocation().getX() - c.getX();
                    ty = target.getLocation().getY() - c.getY() + 1.0;
                    tz = target.getLocation().getZ() - c.getZ();
                }

                double dx = tx - TURRET_X;
                double dy = ty - TURRET_Y;
                double dz = tz - TURRET_Z;
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                double bulletSpeed = 0.7;
                if (dist > 0.1) {
                    // Add slight spread
                    bulletVelX.set(i, (dx / dist) * bulletSpeed + (Math.random() - 0.5) * 0.08);
                    bulletVelY.set(i, (dy / dist) * bulletSpeed + (Math.random() - 0.5) * 0.04);
                    bulletVelZ.set(i, (dz / dist) * bulletSpeed + (Math.random() - 0.5) * 0.08);
                }

                bulletPosX.set(i, TURRET_X);
                bulletPosY.set(i, TURRET_Y);
                bulletPosZ.set(i, TURRET_Z);
                bulletActive.set(i, true);

                Location spawnLoc = c.clone().add(TURRET_X, TURRET_Y, TURRET_Z);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(spawnLoc, iceMat(i));
                handle.scale(0.3f, 0.3f, 0.3f)
                      .glow(200, 220, 255)
                      .interpolation(1, 0);
                spawnedEntities.add(handle.entity());
                bullets.set(i, handle);

                DisplayBuilder.playSound(spawnLoc, Sound.BLOCK_GLASS_BREAK, 0.5f, 2.0f);
            }

            // Move all active bullets
            for (int i = 0; i < BULLET_COUNT; i++) {
                if (!bulletActive.get(i) || bullets.get(i) == null) continue;

                double px = bulletPosX.get(i) + bulletVelX.get(i);
                double py = bulletPosY.get(i) + bulletVelY.get(i);
                double pz = bulletPosZ.get(i) + bulletVelZ.get(i);
                bulletPosX.set(i, px);
                bulletPosY.set(i, py);
                bulletPosZ.set(i, pz);

                Location newLoc = c.clone().add(px, py, pz);
                bullets.get(i).entity().teleport(newLoc);

                // Trail
                if (tick % 2 == 0) {
                    w.spawnParticle(Particle.END_ROD, newLoc, 1, 0.02, 0.02, 0.02, 0.005);
                }

                // Impact check: hit ground or traveled far
                double travelDist = Math.sqrt(
                    (px - TURRET_X) * (px - TURRET_X) +
                    (py - TURRET_Y) * (py - TURRET_Y) +
                    (pz - TURRET_Z) * (pz - TURRET_Z));

                boolean hitGround = py <= 0.2;
                boolean hitPlayer = false;
                Player target = getTargetPlayer();
                if (target != null && target.isOnline()) {
                    hitPlayer = newLoc.distanceSquared(target.getLocation()) < 4.0;
                }

                if (hitGround || hitPlayer || travelDist > 20) {
                    bulletActive.set(i, false);
                    if (hitGround || hitPlayer) {
                        triggerImpactDamage(newLoc);
                        w.spawnParticle(Particle.SNOWFLAKE, newLoc, 6, 0.3, 0.3, 0.3, 0.03);
                        DisplayBuilder.dustParticles(newLoc, 4, 0.3, 100, 180, 255, 0.8f);
                    }
                }
            }

            // Turret glow pulses
            if (tick % 10 == 0) {
                Location turretLoc = c.clone().add(TURRET_X, TURRET_Y, TURRET_Z);
                DisplayBuilder.dustParticles(turretLoc, 4, 0.3, 60, 120, 200, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GlacialBullets(plugin); }
    }

    // ================================================================
    // 12. ICE BOOMERANG — 10 curved blocks forming arc shape, arcs away
    //     from center then curves back from behind, damages both passes
    // ================================================================
    public static class IceBoomerang extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> blocks = new ArrayList<>();
        private final List<Double> localOffsets = new ArrayList<>();
        private double pathAngle = 0;
        private double pathRadius = 0;
        private boolean returning = false;
        private double boomerangX, boomerangZ;
        private static final int BLOCK_COUNT = 10;
        private static final double MAX_DIST = 12.0;

        public IceBoomerang(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_boomerang", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            boomerangX = 0;
            boomerangZ = 0;
            pathAngle = 0;
            pathRadius = 0;

            // Arc shape: blocks arranged in a curve
            for (int i = 0; i < BLOCK_COUNT; i++) {
                double curveAngle = (i - BLOCK_COUNT / 2.0) * 0.3;
                localOffsets.add(curveAngle);

                double lx = Math.cos(curveAngle) * 1.5;
                double lz = Math.sin(curveAngle) * 0.8;
                Location spawnLoc = center.clone().add(lx, 1.5, lz);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(spawnLoc, iceMat(i));
                handle.scale(0.6f, 0.4f, 0.9f)
                      .glow(100, 180, 255)
                      .interpolation(2, 0);
                spawnedEntities.add(handle.entity());
                blocks.add(handle);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_SNOWBALL_THROW, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Boomerang path: arc outward, curve, then come back
            double speed = 0.08;
            if (!returning) {
                pathAngle += speed;
                pathRadius += 0.15;
                if (pathRadius >= MAX_DIST) {
                    returning = true;
                }
            } else {
                pathAngle += speed;
                pathRadius -= 0.15;
            }

            boomerangX = Math.cos(pathAngle) * pathRadius;
            boomerangZ = Math.sin(pathAngle) * pathRadius;

            // Rotate the arc shape with movement
            double facingAngle = pathAngle + Math.PI / 2;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                double curveAngle = localOffsets.get(i);
                double lx = Math.cos(facingAngle + curveAngle) * 1.5;
                double lz = Math.sin(facingAngle + curveAngle) * 1.5;

                Location newLoc = c.clone().add(boomerangX + lx, 1.5, boomerangZ + lz);
                blocks.get(i).entity().teleport(newLoc);
            }

            // Spinning rotation on each block
            float spin = tick * 0.15f;
            for (int i = 0; i < BLOCK_COUNT; i++) {
                Transformation t = new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(spin, 0, 1, 0),
                    blocks.get(i).entity().getTransformation().getScale(),
                    new AxisAngle4f(0, 0, 0, 1)
                );
                blocks.get(i).entity().setTransformation(t);
                blocks.get(i).entity().setInterpolationDuration(3);
                blocks.get(i).entity().setInterpolationDelay(0);
            }

            // Trail particles
            Location boomLoc = c.clone().add(boomerangX, 1.5, boomerangZ);
            if (tick % 2 == 0) {
                w.spawnParticle(Particle.SNOWFLAKE, boomLoc, 4, 0.8, 0.3, 0.8, 0.02);
                DisplayBuilder.dustParticles(boomLoc, 3, 0.5, 100, 180, 255, 1.0f);
                w.spawnParticle(Particle.END_ROD, boomLoc, 2, 0.5, 0.2, 0.5, 0.01);
            }

            // Whoosh sound
            if (tick % 12 == 0) {
                DisplayBuilder.playSound(boomLoc, Sound.ENTITY_SNOWBALL_THROW, 0.6f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new IceBoomerang(plugin); }
    }

    // ================================================================
    // 13. FROST NOVA — 14 shards expanding outward from center at
    //     ground level, radial explosion like a shockwave
    // ================================================================
    public static class FrostNova extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> shards = new ArrayList<>();
        private final List<Double> dirX = new ArrayList<>();
        private final List<Double> dirZ = new ArrayList<>();
        private final List<Float> distances = new ArrayList<>();
        private boolean started = false;
        private static final int SHARD_COUNT = 14;
        private static final float MAX_DISTANCE = 12.0f;
        private static final float EXPAND_SPEED = 0.35f;
        private static final int CHARGE_TICKS = 30;

        public FrostNova(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_nova", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < SHARD_COUNT; i++) {
                double angle = (Math.PI * 2.0 / SHARD_COUNT) * i;
                dirX.add(Math.cos(angle));
                dirZ.add(Math.sin(angle));
                distances.add(0.0f);

                // Start all shards at center
                Location spawnLoc = center.clone().add(0, 0.5, 0);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(spawnLoc, iceMat(i));
                handle.scale(0.6f, 0.4f, 1.2f)
                      .glow(100, 180, 255)
                      .interpolation(2, 0);
                spawnedEntities.add(handle.entity());
                shards.add(handle);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (tick < CHARGE_TICKS) {
                // Charge-up: pulsing particles at center
                if (tick % 4 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 1, 0), 10, 1.0, 60, 120, 200, 1.5f);
                    w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1, 0), 8, 0.8, 0.5, 0.8, 0.02);

                    // Draw inward ring to show charge
                    double chargeRadius = 3.0 * (1.0 - tick / (double) CHARGE_TICKS);
                    DisplayBuilder.particleRing(c.clone().add(0, 0.5, 0), chargeRadius, Particle.DUST, 16,
                            new Particle.DustOptions(Color.fromRGB(100, 180, 255), 1.2f));
                }

                if (tick % 8 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.6f, 0.8f + tick * 0.03f);
                }

                // Vibrate shards slightly
                for (int i = 0; i < SHARD_COUNT; i++) {
                    double jitter = (Math.random() - 0.5) * 0.3;
                    Location loc = c.clone().add(jitter, 0.5, jitter);
                    shards.get(i).entity().teleport(loc);
                }
                return;
            }

            // Burst sound on first expansion tick
            if (!started) {
                started = true;
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.5f);
                w.spawnParticle(Particle.END_ROD, c.clone().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
            }

            // Expand shards outward
            for (int i = 0; i < SHARD_COUNT; i++) {
                float dist = distances.get(i) + EXPAND_SPEED;
                if (dist > MAX_DISTANCE) dist = MAX_DISTANCE;
                distances.set(i, dist);

                double px = dirX.get(i) * dist;
                double pz = dirZ.get(i) * dist;
                Location newLoc = c.clone().add(px, 0.5, pz);
                shards.get(i).entity().teleport(newLoc);

                // Orient shard to face outward
                float facingAngle = (float) Math.atan2(dirZ.get(i), dirX.get(i));
                Transformation t = new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(facingAngle, 0, 1, 0),
                    shards.get(i).entity().getTransformation().getScale(),
                    new AxisAngle4f(0, 0, 0, 1)
                );
                shards.get(i).entity().setTransformation(t);
                shards.get(i).entity().setInterpolationDuration(3);
                shards.get(i).entity().setInterpolationDelay(0);

                // Trail behind each shard
                if (tick % 3 == 0) {
                    DisplayBuilder.dustParticles(newLoc, 2, 0.2, 100, 180, 255, 0.8f);
                    w.spawnParticle(Particle.SNOWFLAKE, newLoc, 2, 0.1, 0.1, 0.1, 0.01);
                }
            }

            // Expanding frost ring on ground
            int expandTick = tick - CHARGE_TICKS;
            if (expandTick % 4 == 0) {
                double ringRadius = expandTick * EXPAND_SPEED;
                if (ringRadius <= MAX_DISTANCE) {
                    DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), ringRadius, Particle.DUST, 24,
                            new Particle.DustOptions(Color.fromRGB(200, 220, 255), 1.0f));
                }
            }

            // Ground frost particles spreading
            if (expandTick % 6 == 0) {
                double frostDist = expandTick * EXPAND_SPEED * 0.8;
                for (int i = 0; i < 8; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double d = Math.random() * frostDist;
                    Location frostLoc = c.clone().add(Math.cos(angle) * d, 0.2, Math.sin(angle) * d);
                    w.spawnParticle(Particle.SNOWFLAKE, frostLoc, 3, 0.3, 0.1, 0.3, 0.01);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FrostNova(plugin); }
    }
}
