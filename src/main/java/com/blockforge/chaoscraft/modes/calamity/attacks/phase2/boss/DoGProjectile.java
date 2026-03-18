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
 * Phase 2 Boss Attacks — GROUP 2: PROJECTILE (#11-20)
 * Cosmic flames, energy bolts, crystal shards fired from DoG's head.
 * Crystalline projectile attacks with cyan/violet glow aesthetics.
 * NO status effects — damage only.
 */
public final class DoGProjectile {

    private DoGProjectile() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CosmicFlameBurst(plugin));
        registry.register(new ScatteredFlameVolley(plugin));
        registry.register(new HomingFlameOrb(plugin));
        registry.register(new CrystalShardBurst(plugin));
        registry.register(new EnergyBoltLine(plugin));
        registry.register(new OrbitingFlameSatellites(plugin));
        registry.register(new TailSpikeVolley(plugin));
        registry.register(new CosmicFlamePillar(plugin));
        registry.register(new ChargedEnergyBall(plugin));
        registry.register(new MultiTargetFlameSpread(plugin));
    }

    // ================================================================
    // 11. COSMIC FLAME BURST — Three slow flame spheres fired in sequence
    // ================================================================
    public static class CosmicFlameBurst extends BossAttack {
        private final List<BlockDisplayHandle> orbHandles = new ArrayList<>();
        private int shotsFired = 0;
        private int lastShotTick = 0;

        public CosmicFlameBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_cosmic_flame_burst", AttackType.BOSS, 2), "dog");
            config.setDamage(10.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Charge-up aura at DoG's snout position
            BlockDisplayHandle chargeGlow = displayBuilder.spawnBlock(
                center.clone().add(-14, 8, 0), Material.SEA_LANTERN);
            chargeGlow.scale(1.5f, 1.5f, 1.5f).glow(0, 200, 255).interpolation(4, 0);
            orbHandles.add(chargeGlow);
            spawnedEntities.add(chargeGlow.entity());
            DisplayBuilder.cyanDust(center.clone().add(-14, 8, 0), 15, 2.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Charge-up glow (0-30 ticks)
            if (ticksAlive < 30) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(-14, 8, 0), 8, 2.0);
                }
            }
            // Fire 3 spheres in sequence, 16 ticks (0.8 seconds) apart
            else if (shotsFired < 3 && ticksAlive - lastShotTick >= 16) {
                lastShotTick = ticksAlive;
                shotsFired++;
                // Spawn flame sphere projectile
                Location spawnLoc = center.clone().add(-14, 8, 0);
                BlockDisplayHandle orb = displayBuilder.spawnBlock(spawnLoc, Material.SEA_LANTERN);
                orb.scale(1.8f, 1.8f, 1.8f).glow(0, 200, 255).interpolation(2, 0);
                orbHandles.add(orb);
                spawnedEntities.add(orb.entity());
                DisplayBuilder.playSound(spawnLoc, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.2f);
            }

            // Animate projectiles moving toward center (outward from snout)
            if (shotsFired > 0) {
                for (int i = 1; i < orbHandles.size(); i++) {
                    Location loc = orbHandles.get(i).entity().getLocation();
                    // Move toward center at 8 blocks per second = 0.4 blocks per tick
                    double dx = center.getX() - loc.getX();
                    double dz = center.getZ() - loc.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > 0.5) {
                        orbHandles.get(i).entity().teleport(
                            loc.clone().add(dx / dist * 0.4, -0.05, dz / dist * 0.4));
                    }
                }
                // Trail particles
                if (ticksAlive % 4 == 0) {
                    for (int i = 1; i < orbHandles.size(); i++) {
                        DisplayBuilder.cyanDust(orbHandles.get(i).entity().getLocation(), 4, 1.0);
                    }
                }
            }

            // Explosion on arrival near center
            for (int i = orbHandles.size() - 1; i >= 1; i--) {
                Location loc = orbHandles.get(i).entity().getLocation();
                if (loc.distanceSquared(center) < 4) {
                    DisplayBuilder.cyanDust(loc, 20, 4.0);
                    DisplayBuilder.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
                    orbHandles.get(i).entity().remove();
                    orbHandles.remove(i);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CosmicFlameBurst(plugin); }
    }

    // ================================================================
    // 12. SCATTERED FLAME VOLLEY — Shotgun-spread of small projectiles
    // ================================================================
    public static class ScatteredFlameVolley extends BossAttack {
        private final List<BlockDisplayHandle> shardHandles = new ArrayList<>();
        private final double[] shardAngles = new double[10];
        private boolean volleyFired = false;

        public ScatteredFlameVolley(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_scattered_flame_volley", AttackType.BOSS, 2), "dog");
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Snout glow spreading across surface
            BlockDisplayHandle snoutGlow = displayBuilder.spawnBlock(
                center.clone().add(0, 10, -16), Material.SEA_LANTERN);
            snoutGlow.scale(2.0f, 1.5f, 1.5f).glow(0, 200, 255).interpolation(3, 0);
            shardHandles.add(snoutGlow);
            spawnedEntities.add(snoutGlow.entity());
            DisplayBuilder.cyanDust(center.clone().add(0, 10, -16), 12, 2.5);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Sweep telegraph (0-20 ticks)
            if (ticksAlive < 20 && !volleyFired) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 10, -16), 8, 3.0);
                }
            }
            // Fire volley (tick 20) — 10 small projectiles in a 90-degree fan
            else if (ticksAlive == 20 && !volleyFired) {
                volleyFired = true;
                Location origin = center.clone().add(0, 10, -16);
                for (int i = 0; i < 10; i++) {
                    double angle = Math.toRadians(-45 + i * 10 + (Math.random() - 0.5) * 5);
                    shardAngles[i] = angle;
                    BlockDisplayHandle shard = displayBuilder.spawnBlock(origin.clone(), Material.AMETHYST_CLUSTER);
                    shard.scale(0.5f, 0.5f, 0.5f).glow(0, 200, 255).interpolation(1, 0);
                    shardHandles.add(shard);
                    spawnedEntities.add(shard.entity());
                }
                DisplayBuilder.playSound(origin, Sound.ENTITY_WITHER_SHOOT, 1.5f, 1.5f);
            }
            // Projectiles fly outward (20-60 ticks)
            else if (volleyFired && ticksAlive > 20 && ticksAlive < 60) {
                int t = ticksAlive - 20;
                for (int i = 1; i < shardHandles.size() && i <= 10; i++) {
                    double angle = shardAngles[i - 1];
                    double dist = t * 0.7; // 14 blocks/sec
                    double x = Math.sin(angle) * dist;
                    double z = -16 + Math.cos(angle) * dist;
                    double y = 10 - t * 0.12;
                    shardHandles.get(i).entity().teleport(center.clone().add(x, y, z));
                }
                if (t % 4 == 0) {
                    for (int i = 1; i < shardHandles.size() && i <= 10; i++) {
                        DisplayBuilder.cyanDust(shardHandles.get(i).entity().getLocation(), 2, 0.5);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ScatteredFlameVolley(plugin); }
    }

    // ================================================================
    // 13. HOMING FLAME ORB — Slow-then-fast tracking orb with two passes
    // ================================================================
    public static class HomingFlameOrb extends BossAttack {
        private final List<BlockDisplayHandle> orbHandles = new ArrayList<>();
        private boolean orbLaunched = false;
        private int passCount = 0;
        private double orbX = 0, orbY = 10, orbZ = -14;
        private double velX = 0, velZ = 0;

        public HomingFlameOrb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_homing_flame_orb", AttackType.BOSS, 2), "dog");
            config.setDamage(12.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Orb forming in DoG's mouth
            BlockDisplayHandle core = displayBuilder.spawnBlock(
                center.clone().add(0, 10, -14), Material.SEA_LANTERN);
            core.scale(0.5f, 0.5f, 0.5f).glow(0, 200, 255).interpolation(4, 0);
            orbHandles.add(core);
            spawnedEntities.add(core.entity());
            DisplayBuilder.cyanDust(center.clone().add(0, 10, -14), 10, 1.5);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Orb grows during charge-up (0-30 ticks)
            if (ticksAlive < 30 && !orbLaunched) {
                float scale = 0.5f + (ticksAlive / 30.0f) * 1.5f;
                BlockDisplay bd = (BlockDisplay) orbHandles.get(0).entity();
                bd.setTransformation(new Transformation(
                    new Vector3f(-scale / 2, -scale / 2, -scale / 2),
                    new AxisAngle4f(ticksAlive * 0.1f, 0, 1, 0),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0, 0, 1, 0)));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(3);
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(orbX, orbY, orbZ), 6, 1.5);
                }
            }
            // Launch orb (tick 30)
            else if (ticksAlive == 30 && !orbLaunched) {
                orbLaunched = true;
                // Initial slow velocity toward center
                velX = 0;
                velZ = 0.2; // 4 blocks/sec initially
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.5f, 0.8f);
            }
            // Orb tracking movement
            else if (orbLaunched && passCount < 2) {
                // Accelerate toward center (homing)
                double dx = center.getX() - (center.getX() + orbX);
                double dz = center.getZ() - (center.getZ() + orbZ);
                double dist = Math.sqrt(dx * dx + dz * dz);

                // Accelerate from 4 to 16 blocks/sec over 30 ticks
                int flightTick = ticksAlive - 30;
                double speed = Math.min(0.8, 0.2 + flightTick * 0.02);

                if (dist > 0.3) {
                    double turnRate = 0.15;
                    velX += (dx / dist) * turnRate;
                    velZ += (dz / dist) * turnRate;
                    // Normalize velocity to speed
                    double velMag = Math.sqrt(velX * velX + velZ * velZ);
                    if (velMag > 0) {
                        velX = (velX / velMag) * speed;
                        velZ = (velZ / velMag) * speed;
                    }
                }

                orbX += velX;
                orbZ += velZ;
                orbY = 10 - flightTick * 0.08;
                if (orbY < 2) orbY = 2;

                orbHandles.get(0).entity().teleport(center.clone().add(orbX, orbY, orbZ));

                // Comet tail particles
                if (flightTick % 3 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(orbX, orbY, orbZ), 5, 1.0);
                }

                // Check if passed through center (close pass)
                if (dist < 2.0) {
                    passCount++;
                    if (passCount < 2) {
                        // Decelerate, stop, re-target
                        velX = -velX * 0.3;
                        velZ = -velZ * 0.3;
                        DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
                    }
                }

                // Despawn after 160 ticks of flight (8 seconds)
                if (flightTick > 160) passCount = 2;
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HomingFlameOrb(plugin); }
    }

    // ================================================================
    // 14. CRYSTAL SHARD BURST — Radial shrapnel burst from snout
    // ================================================================
    public static class CrystalShardBurst extends BossAttack {
        private final List<BlockDisplayHandle> shardHandles = new ArrayList<>();
        private final double[][] shardVectors = new double[16][3];
        private boolean burstFired = false;

        public CrystalShardBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_crystal_shard_burst", AttackType.BOSS, 2), "dog");
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Rearing motion indicator — dense crit particles at snout
            BlockDisplayHandle snout = displayBuilder.spawnBlock(
                center.clone().add(0, 8, 0), Material.AMETHYST_CLUSTER);
            snout.scale(2.0f, 2.0f, 2.0f).glow(128, 0, 255).interpolation(3, 0);
            shardHandles.add(snout);
            spawnedEntities.add(snout.entity());
            DisplayBuilder.cyanDust(center.clone().add(0, 8, 0), 20, 2.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Head rearing back telegraph (0-20 ticks)
            if (ticksAlive < 20 && !burstFired) {
                if (ticksAlive % 5 == 0) {
                    // Rearing animation — snout moves upward
                    shardHandles.get(0).entity().teleport(
                        center.clone().add(0, 8 + ticksAlive * 0.1, 0));
                    DisplayBuilder.cyanDust(center.clone().add(0, 8 + ticksAlive * 0.1, 0), 10, 1.5);
                }
            }
            // Burst fires (tick 20) — 16 shards in sphere pattern
            else if (ticksAlive == 20 && !burstFired) {
                burstFired = true;
                Location burstCenter = center.clone().add(0, 10, 0);
                for (int i = 0; i < 16; i++) {
                    // Fibonacci sphere distribution
                    double phi = Math.acos(1 - 2.0 * (i + 0.5) / 16);
                    double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                    shardVectors[i][0] = Math.sin(phi) * Math.cos(theta);
                    shardVectors[i][1] = Math.sin(phi) * Math.sin(theta) * 0.3;
                    shardVectors[i][2] = Math.cos(phi);

                    BlockDisplayHandle shard = displayBuilder.spawnBlock(burstCenter.clone(),
                        Material.AMETHYST_CLUSTER);
                    shard.scale(0.4f, 0.6f, 0.4f).glow(128, 0, 255).interpolation(1, 0);
                    shardHandles.add(shard);
                    spawnedEntities.add(shard.entity());
                }
                DisplayBuilder.cyanDust(burstCenter, 30, 3.0);
                DisplayBuilder.playSound(burstCenter, Sound.BLOCK_GLASS_BREAK, 2.0f, 1.5f);
                DisplayBuilder.playSound(burstCenter, Sound.ENTITY_WITHER_SHOOT, 1.5f, 1.0f);
            }
            // Shards fly outward (20-60 ticks = 2 seconds)
            else if (burstFired && ticksAlive > 20 && ticksAlive < 60) {
                int t = ticksAlive - 20;
                for (int i = 0; i < 16 && i + 1 < shardHandles.size(); i++) {
                    double dist = t * 0.6; // 12 blocks/sec
                    shardHandles.get(i + 1).entity().teleport(
                        center.clone().add(
                            shardVectors[i][0] * dist,
                            10 + shardVectors[i][1] * dist,
                            shardVectors[i][2] * dist));
                }
                if (t % 5 == 0) {
                    for (int i = 1; i < shardHandles.size(); i++) {
                        DisplayBuilder.cyanDust(shardHandles.get(i).entity().getLocation(), 2, 0.3);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalShardBurst(plugin); }
    }

    // ================================================================
    // 15. ENERGY BOLT LINE — Near-instant piercing bolt barrage
    // ================================================================
    public static class EnergyBoltLine extends BossAttack {
        private final List<BlockDisplayHandle> boltHandles = new ArrayList<>();
        private int boltsFired = 0;
        private int lastBoltTick = 0;

        public EnergyBoltLine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_energy_bolt_line", AttackType.BOSS, 2), "dog");
            config.setDamage(8.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(6);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Laser-pointer warning line from snout toward target
            Location snout = center.clone().add(-12, 8, 0);
            for (int i = 0; i < 20; i++) {
                Location linePt = snout.clone().add(i * 1.2, -i * 0.3, 0);
                BlockDisplayHandle pt = displayBuilder.spawnBlock(linePt, Material.END_ROD);
                pt.scale(0.15f, 0.15f, 0.8f).glow(0, 200, 255).interpolation(2, 0);
                boltHandles.add(pt);
                spawnedEntities.add(pt.entity());
            }
            DisplayBuilder.cyanDust(snout, 10, 1.5);
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.5f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning line visible (0-16 ticks = 0.8 seconds)
            if (ticksAlive < 16) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(-12, 8, 0), 6, 1.0);
                }
            }
            // Fire 5 bolts, 6 ticks (0.3 seconds) apart
            else if (boltsFired < 5 && ticksAlive - lastBoltTick >= 6) {
                lastBoltTick = ticksAlive;
                boltsFired++;
                // Clear old warning line on first bolt
                if (boltsFired == 1) {
                    for (BlockDisplayHandle h : boltHandles) h.entity().remove();
                    boltHandles.clear();
                }
                // Near-instant bolt line from snout through center and beyond
                Location snout = center.clone().add(-12, 8, 0);
                for (int i = 0; i < 30; i++) {
                    Location boltPt = snout.clone().add(i * 0.9, -i * 0.2, 0);
                    BlockDisplayHandle bt = displayBuilder.spawnBlock(boltPt, Material.SEA_LANTERN);
                    bt.scale(0.1f, 0.1f, 0.6f).glow(0, 200, 255).interpolation(1, 0);
                    boltHandles.add(bt);
                    spawnedEntities.add(bt.entity());
                }
                // White spark afterimage
                DisplayBuilder.cyanDust(center, 15, 12.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.2f, 2.0f);

                // Remove bolt after 4 ticks (brief flash)
            }
            // Clean up old bolt visuals
            if (boltsFired > 0 && ticksAlive % 6 == 2) {
                for (int i = boltHandles.size() - 1; i >= 0; i--) {
                    boltHandles.get(i).entity().remove();
                    boltHandles.remove(i);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EnergyBoltLine(plugin); }
    }

    // ================================================================
    // 16. ORBITING FLAME SATELLITES — Three orbs circling a fixed point
    // ================================================================
    public static class OrbitingFlameSatellites extends BossAttack {
        private final List<BlockDisplayHandle> orbHandles = new ArrayList<>();
        private boolean orbitsActive = false;
        private int orbitStartTick = 0;

        public OrbitingFlameSatellites(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_orbiting_flame_satellites", AttackType.BOSS, 2), "dog");
            config.setDamage(8.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Three sparks emerge from snout and hover
            for (int i = 0; i < 3; i++) {
                Location sparkLoc = center.clone().add(-10 + i * 2, 8, 0);
                BlockDisplayHandle spark = displayBuilder.spawnBlock(sparkLoc, Material.SEA_LANTERN);
                spark.scale(0.6f, 0.6f, 0.6f).glow(0, 200, 255).interpolation(3, 0);
                orbHandles.add(spark);
                spawnedEntities.add(spark.entity());
            }
            DisplayBuilder.cyanDust(center.clone().add(-10, 8, 0), 10, 3.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Hover delay before orbiting (0-16 ticks = 0.8 seconds)
            if (ticksAlive < 16 && !orbitsActive) {
                // Move sparks toward orbit center
                for (int i = 0; i < orbHandles.size(); i++) {
                    Location target = center.clone().add(0, 2, 0);
                    Location current = orbHandles.get(i).entity().getLocation();
                    double dx = target.getX() - current.getX();
                    double dy = target.getY() - current.getY();
                    double dz = target.getZ() - current.getZ();
                    orbHandles.get(i).entity().teleport(current.clone().add(dx * 0.1, dy * 0.1, dz * 0.1));
                }
            }
            // Begin orbital pattern (tick 16)
            else if (ticksAlive == 16 && !orbitsActive) {
                orbitsActive = true;
                orbitStartTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 1.2f);
            }
            // Orbit around center at radius 3 blocks for 120 ticks (6 seconds)
            else if (orbitsActive) {
                int orbitTick = ticksAlive - orbitStartTick;
                if (orbitTick > 120) return; // Orbit expires

                float angularSpeed = 0.12f;
                for (int i = 0; i < orbHandles.size(); i++) {
                    double angle = orbitTick * angularSpeed + (Math.PI * 2 / 3) * i;
                    orbHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * 3, 2, Math.sin(angle) * 3));
                }
                // Trail particles
                if (orbitTick % 5 == 0) {
                    for (BlockDisplayHandle orb : orbHandles) {
                        DisplayBuilder.cyanDust(orb.entity().getLocation(), 3, 0.5);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new OrbitingFlameSatellites(plugin); }
    }

    // ================================================================
    // 17. TAIL SPIKE VOLLEY — Rear-facing crystal spike cone
    // ================================================================
    public static class TailSpikeVolley extends BossAttack {
        private final List<BlockDisplayHandle> spikeHandles = new ArrayList<>();
        private boolean volleyFired = false;

        public TailSpikeVolley(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_tail_spike_volley", AttackType.BOSS, 2), "dog");
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Tail segments flaring — glow at tail end
            for (int i = 0; i < 5; i++) {
                Location tailLoc = center.clone().add(14 + i * 0.8, 6, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(tailLoc, Material.AMETHYST_BLOCK);
                h.scale(0.7f + i * 0.1f, 0.6f, 0.7f + i * 0.1f).glow(128, 0, 255).interpolation(2, 0);
                spikeHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.cyanDust(center.clone().add(16, 6, 0), 10, 2.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Tail flare telegraph (0-16 ticks)
            if (ticksAlive < 16 && !volleyFired) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(16, 6, 0), 6, 2.5);
                }
            }
            // Fire 10 spikes in rear-facing 120-degree cone (tick 16)
            else if (ticksAlive == 16 && !volleyFired) {
                volleyFired = true;
                Location tailPos = center.clone().add(16, 6, 0);
                for (int i = 0; i < 10; i++) {
                    double angle = Math.toRadians(120 + i * 12); // Rear-facing cone
                    BlockDisplayHandle spike = displayBuilder.spawnBlock(tailPos.clone(),
                        Material.AMETHYST_CLUSTER);
                    spike.scale(0.3f, 0.8f, 0.3f).glow(128, 0, 255).interpolation(1, 0);
                    spikeHandles.add(spike);
                    spawnedEntities.add(spike.entity());
                }
                DisplayBuilder.playSound(tailPos, Sound.ENTITY_WITHER_SHOOT, 1.5f, 1.3f);
            }
            // Spikes fly backward (16-56 ticks = 2 seconds)
            else if (volleyFired && ticksAlive > 16 && ticksAlive < 56) {
                int t = ticksAlive - 16;
                for (int i = 5; i < spikeHandles.size() && i - 5 < 10; i++) {
                    int spikeIdx = i - 5;
                    double angle = Math.toRadians(120 + spikeIdx * 12);
                    double dist = t * 0.5; // 10 blocks/sec
                    spikeHandles.get(i).entity().teleport(
                        center.clone().add(
                            16 + Math.cos(angle) * dist,
                            6 + Math.sin(angle) * dist * 0.3,
                            Math.sin(angle) * dist));
                }
                if (t % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(16, 6, 0), 4, 3.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TailSpikeVolley(plugin); }
    }

    // ================================================================
    // 18. COSMIC FLAME PILLAR — Ground-trace fire pillars along path
    // ================================================================
    public static class CosmicFlamePillar extends BossAttack {
        private final List<BlockDisplayHandle> pillarHandles = new ArrayList<>();
        private boolean breathStarted = false;
        private int pillarCount = 0;

        public CosmicFlamePillar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_cosmic_flame_pillar", AttackType.BOSS, 2), "dog");
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Head dips downward telegraph
            BlockDisplayHandle headGlow = displayBuilder.spawnBlock(
                center.clone().add(-12, 3, 0), Material.SEA_LANTERN);
            headGlow.scale(1.5f, 1.0f, 1.5f).glow(0, 200, 255).interpolation(3, 0);
            pillarHandles.add(headGlow);
            spawnedEntities.add(headGlow.entity());
            DisplayBuilder.cyanDust(center.clone().add(-12, 3, 0), 10, 2.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Head dip telegraph (0-30 ticks)
            if (ticksAlive < 30 && !breathStarted) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(-12, 3, 0), 6, 2.0);
                }
            }
            // Breath starts — creating flame pillars along path (tick 30+)
            else if (ticksAlive >= 30) {
                if (!breathStarted) {
                    breathStarted = true;
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.5f, 1.0f);
                }
                // Place a pillar every 8 ticks while moving forward
                if (ticksAlive % 8 == 0 && pillarCount < 12) {
                    pillarCount++;
                    float pillarX = -12 + pillarCount * 2.5f;
                    Location pillarLoc = center.clone().add(pillarX, 0, 0);

                    // Crystal fire pillar — 2x2 base, extends upward
                    for (int y = 0; y <= 4; y++) {
                        BlockDisplayHandle p = displayBuilder.spawnBlock(
                            pillarLoc.clone().add(0, y, 0), Material.SEA_LANTERN);
                        float scale = 1.0f - y * 0.15f;
                        p.scale(scale, 0.8f, scale).glow(0, 200, 255).interpolation(2, 0);
                        pillarHandles.add(p);
                        spawnedEntities.add(p.entity());
                    }
                    DisplayBuilder.cyanDust(pillarLoc, 12, 2.0);
                    DisplayBuilder.playSound(pillarLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.0f);
                }
                // Move head glow along with breath
                if (pillarHandles.size() > 0) {
                    float headX = -12 + pillarCount * 2.5f;
                    pillarHandles.get(0).entity().teleport(center.clone().add(headX, 3, 0));
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CosmicFlamePillar(plugin); }
    }

    // ================================================================
    // 19. CHARGED ENERGY BALL — Large slow-moving detonation sphere
    // ================================================================
    public static class ChargedEnergyBall extends BossAttack {
        private final List<BlockDisplayHandle> ballHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> fieldHandles = new ArrayList<>();
        private boolean ballLaunched = false;
        private boolean detonated = false;
        private double ballX = -14, ballY = 10, ballZ = 0;

        public ChargedEnergyBall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_charged_energy_ball", AttackType.BOSS, 2), "dog");
            config.setDamage(10.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Ball charging — starts small
            BlockDisplayHandle core = displayBuilder.spawnBlock(
                center.clone().add(-14, 10, 0), Material.SEA_LANTERN);
            core.scale(0.5f, 0.5f, 0.5f).glow(0, 200, 255).interpolation(4, 0);
            ballHandles.add(core);
            spawnedEntities.add(core.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Charge-up: ball grows over 40 ticks (2 seconds)
            if (ticksAlive < 40 && !ballLaunched) {
                float scale = 0.5f + (ticksAlive / 40.0f) * 2.5f;
                BlockDisplay bd = (BlockDisplay) ballHandles.get(0).entity();
                bd.setTransformation(new Transformation(
                    new Vector3f(-scale / 2, -scale / 2, -scale / 2),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0, 0, 1, 0)));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(3);
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(ballX, ballY, ballZ), 8, 2.0);
                }
            }
            // Launch ball (tick 40)
            else if (ticksAlive == 40 && !ballLaunched) {
                ballLaunched = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.5f);
            }
            // Ball drifts toward center at walking pace (40-140 ticks = 5 seconds max)
            else if (ballLaunched && !detonated) {
                double dx = center.getX() - (center.getX() + ballX);
                double dz = center.getZ() - (center.getZ() + ballZ);
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 0.5) {
                    ballX += (dx / dist) * 0.25; // 5 blocks/sec
                    ballZ += (dz / dist) * 0.25;
                    ballY = Math.max(2, ballY - 0.06);
                }
                ballHandles.get(0).entity().teleport(center.clone().add(ballX, ballY, ballZ));

                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(ballX, ballY, ballZ), 5, 2.0);
                }

                // Detonation: arrived at center or 5 seconds elapsed
                if (dist < 2.0 || ticksAlive > 140) {
                    detonated = true;
                    // 8-block radius explosion
                    for (int i = 0; i < 24; i++) {
                        double a = (Math.PI * 2 / 24) * i;
                        for (int r = 2; r <= 8; r += 2) {
                            Location fieldLoc = center.clone().add(
                                ballX + Math.cos(a) * r, 0.5, ballZ + Math.sin(a) * r);
                            BlockDisplayHandle fh = displayBuilder.spawnBlock(fieldLoc, Material.SEA_LANTERN);
                            fh.scale(0.4f, 0.2f, 0.4f).glow(0, 200, 255).interpolation(2, 0);
                            fieldHandles.add(fh);
                            spawnedEntities.add(fh.entity());
                        }
                    }
                    DisplayBuilder.cyanDust(center.clone().add(ballX, ballY, ballZ), 50, 8.0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.6f);
                    DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChargedEnergyBall(plugin); }
    }

    // ================================================================
    // 20. MULTI-TARGET FLAME SPREAD — Splitting orb targeting multiple players
    // ================================================================
    public static class MultiTargetFlameSpread extends BossAttack {
        private final List<BlockDisplayHandle> orbHandles = new ArrayList<>();
        private boolean mainOrbLaunched = false;
        private boolean splitOccurred = false;
        private double mainX = -12, mainY = 10, mainZ = 0;

        public MultiTargetFlameSpread(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_multi_target_flame_spread", AttackType.BOSS, 2), "dog");
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Broad diffuse orb forming at snout — visually different from standard
            BlockDisplayHandle mainOrb = displayBuilder.spawnBlock(
                center.clone().add(-12, 10, 0), Material.SEA_LANTERN);
            mainOrb.scale(2.5f, 2.5f, 2.5f).glow(0, 200, 255).interpolation(4, 0);
            orbHandles.add(mainOrb);
            spawnedEntities.add(mainOrb.entity());
            DisplayBuilder.cyanDust(center.clone().add(-12, 10, 0), 15, 3.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Charge-up (0-24 ticks = 1.2 seconds)
            if (ticksAlive < 24 && !mainOrbLaunched) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(mainX, mainY, mainZ), 8, 2.5);
                }
            }
            // Launch main orb (tick 24)
            else if (ticksAlive == 24 && !mainOrbLaunched) {
                mainOrbLaunched = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.8f);
            }
            // Main orb travels straight for 40 ticks (2 seconds) before splitting
            else if (mainOrbLaunched && !splitOccurred) {
                int flightTick = ticksAlive - 24;
                mainX += 0.6;
                mainY -= 0.1;
                if (mainY < 4) mainY = 4;
                orbHandles.get(0).entity().teleport(center.clone().add(mainX, mainY, mainZ));

                if (flightTick % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(mainX, mainY, mainZ), 5, 2.0);
                }

                // Split at tick 40 of flight (2 seconds)
                if (flightTick >= 40) {
                    splitOccurred = true;
                    Location splitLoc = center.clone().add(mainX, mainY, mainZ);

                    // Remove main orb visual, spawn 3 smaller homing orbs
                    BlockDisplay mainBd = (BlockDisplay) orbHandles.get(0).entity();
                    mainBd.setTransformation(new Transformation(
                        new Vector3f(-0.5f, -0.5f, -0.5f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(1.0f, 1.0f, 1.0f),
                        new AxisAngle4f(0, 0, 1, 0)));
                    mainBd.setInterpolationDelay(0);
                    mainBd.setInterpolationDuration(2);

                    // Starburst at split point
                    DisplayBuilder.cyanDust(splitLoc, 30, 4.0);

                    // 3 split orbs diverging in different directions
                    double[] splitAngles = {-Math.PI / 3, 0, Math.PI / 3};
                    for (int i = 0; i < 3; i++) {
                        BlockDisplayHandle splitOrb = displayBuilder.spawnBlock(splitLoc.clone(),
                            Material.SEA_LANTERN);
                        splitOrb.scale(1.0f, 1.0f, 1.0f).glow(128, 0, 255).interpolation(1, 0);
                        orbHandles.add(splitOrb);
                        spawnedEntities.add(splitOrb.entity());
                    }
                    DisplayBuilder.playSound(splitLoc, Sound.BLOCK_GLASS_BREAK, 1.5f, 1.5f);
                }
            }
            // Split orbs fly outward in diverging paths (post-split)
            else if (splitOccurred) {
                int postSplit = ticksAlive - 64;
                double[] splitAngles = {-Math.PI / 3, 0, Math.PI / 3};
                for (int i = 1; i < orbHandles.size() && i <= 3; i++) {
                    double angle = splitAngles[i - 1];
                    double dist = postSplit * 0.8; // 16 blocks/sec
                    orbHandles.get(i).entity().teleport(
                        center.clone().add(
                            mainX + Math.cos(angle) * dist,
                            mainY - postSplit * 0.05,
                            mainZ + Math.sin(angle) * dist));
                }
                if (postSplit % 4 == 0 && postSplit < 40) {
                    for (int i = 1; i < orbHandles.size() && i <= 3; i++) {
                        DisplayBuilder.cyanDust(orbHandles.get(i).entity().getLocation(), 3, 0.8);
                    }
                }
                // Explosion on each split orb after 30 ticks of travel
                if (postSplit == 30) {
                    for (int i = 1; i < orbHandles.size() && i <= 3; i++) {
                        DisplayBuilder.cyanDust(orbHandles.get(i).entity().getLocation(), 15, 2.0);
                    }
                    DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MultiTargetFlameSpread(plugin); }
    }
}
