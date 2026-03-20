package com.blockforge.chaoscraft.modes.freezingice.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class TemperatureDrop {
    private TemperatureDrop() {}

    // ICE color palette
    private static final int ICE_R1 = 100, ICE_G1 = 180, ICE_B1 = 255; // Light ice blue
    private static final int ICE_R2 = 220, ICE_G2 = 240, ICE_B2 = 255; // Pale frost white
    private static final int ICE_R3 = 30,  ICE_G3 = 80,  ICE_B3 = 160; // Deep cold blue

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ArcticChill(plugin));
        registry.register(new PermafrostPulse(plugin));
        registry.register(new FrostBite(plugin));
        registry.register(new HypothermiaWave(plugin));
        registry.register(new DeepFreeze(plugin));
        registry.register(new ColdSnap(plugin));
        registry.register(new ShiveringFit(plugin));
        registry.register(new BreathCloud(plugin));
        registry.register(new BodyHeatDrain(plugin));
        registry.register(new WindChill(plugin));
        registry.register(new FrostCreepEnv(plugin));
        registry.register(new CoreFreeze(plugin));
        registry.register(new ThawFakeout(plugin));
    }

    // ========================================================================
    // 1. ArcticChill — Frost particles around all nearby players, area damage
    //    pulse every 20 ticks
    // ========================================================================
    public static class ArcticChill extends EnvironmentalAttack {

        public ArcticChill(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("arctic_chill", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20); // Damage pulse every 20 ticks
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.5f, 0.6f);
            // Initial burst of frost particles
            World world = center.getWorld();
            if (world == null) return;
            world.spawnParticle(Particle.SNOWFLAKE, center, 60, 4, 2, 4, 0.05);
            DisplayBuilder.dustParticles(center, 40, 3.0, ICE_R1, ICE_G1, ICE_B1, 1.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World world = center.getWorld();

            // Frost particles around every nearby player
            for (Player player : world.getPlayers()) {
                if (player.getLocation().distanceSquared(center) <= 64) { // 8 block radius
                    Location pLoc = player.getLocation();
                    DisplayBuilder.dustParticles(pLoc, 8, 1.0, ICE_R2, ICE_G2, ICE_B2, 1.2f);
                    world.spawnParticle(Particle.SNOWFLAKE, pLoc.clone().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.02);
                }
            }

            // Ambient snowflake field
            if (tick % 5 == 0) {
                world.spawnParticle(Particle.SNOWFLAKE, center, 20, 5, 3, 5, 0.02);
            }

            // Pulse visual + sound every 20 ticks (matches damage interval)
            if (tick % 20 == 0) {
                DisplayBuilder.particleRing(center, 8.0, Particle.DUST, 40,
                        new Particle.DustOptions(Color.fromRGB(ICE_R1, ICE_G1, ICE_B1), 1.8f));
                DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.8f);
            }
        }

        @Override public AbstractAttack newInstance() { return new ArcticChill(plugin); }
    }

    // ========================================================================
    // 2. PermafrostPulse — Expanding ring of frost from center, damage on ring
    //    front
    // ========================================================================
    public static class PermafrostPulse extends EnvironmentalAttack {
        private double currentRadius = 0;

        public PermafrostPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("permafrost_pulse", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(0); // We handle damage manually on the ring front
            config.setDamageRadius(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(35.0);
            config.setImpactRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            currentRadius = 0;
            DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 2.0f, 0.5f);
            DisplayBuilder.dustParticles(center, 30, 0.5, ICE_R3, ICE_G3, ICE_B3, 2.0f);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World world = center.getWorld();

            // Expand ring: 0.15 blocks/tick => reaches ~10 blocks at tick ~67, then resets
            currentRadius += 0.15;
            if (currentRadius > 10.0) {
                currentRadius = 0; // Reset and pulse again
                DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 1.5f, 0.6f);
            }

            // Draw the expanding frost ring
            DisplayBuilder.particleRing(center, currentRadius, Particle.DUST, 36,
                    new Particle.DustOptions(Color.fromRGB(ICE_R1, ICE_G1, ICE_B1), 1.5f));
            DisplayBuilder.particleRing(center, currentRadius, Particle.SNOWFLAKE, 18, null);

            // Damage players near the ring front (within 2 blocks of ring edge)
            if (tick % 4 == 0) {
                for (Player player : world.getPlayers()) {
                    if (isExempt(player)) continue;
                    double dist = player.getLocation().distance(center);
                    if (Math.abs(dist - currentRadius) <= 2.0) {
                        triggerImpactDamage(player.getLocation());
                    }
                }
            }

            // Ambient sound
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 1.2f);
            }
        }

        @Override public AbstractAttack newInstance() { return new PermafrostPulse(plugin); }
    }

    // ========================================================================
    // 3. FrostBite — Damage only hits stationary players (velocity < 0.1),
    //    frost at feet
    // ========================================================================
    public static class FrostBite extends EnvironmentalAttack {

        public FrostBite(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_bite_env", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(0); // Manual damage — only stationary players
            config.setDamageRadius(0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 1.5f, 0.4f);
            World world = center.getWorld();
            if (world == null) return;
            world.spawnParticle(Particle.SNOWFLAKE, center, 40, 5, 0.5, 5, 0.01);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World world = center.getWorld();
            double radius = 8.0;

            // Ground-level frost particles across the area
            if (tick % 3 == 0) {
                for (int i = 0; i < 10; i++) {
                    double rx = (Math.random() - 0.5) * radius * 2;
                    double rz = (Math.random() - 0.5) * radius * 2;
                    Location ground = center.clone().add(rx, 0, rz);
                    DisplayBuilder.dustParticles(ground, 3, 0.3, ICE_R2, ICE_G2, ICE_B2, 1.0f);
                }
            }

            // Every 10 ticks: check players, damage stationary ones
            if (tick % 10 == 0) {
                for (Player player : world.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) > radius * radius) continue;

                    Vector vel = player.getVelocity();
                    double speed = Math.sqrt(vel.getX() * vel.getX() + vel.getZ() * vel.getZ());

                    if (speed < 0.1) {
                        // Stationary — frost bites!
                        player.damage(35.0);
                        player.setNoDamageTicks(0);
                        Location feet = player.getLocation();
                        DisplayBuilder.dustParticles(feet, 20, 0.5, ICE_R3, ICE_G3, ICE_B3, 1.8f);
                        world.spawnParticle(Particle.SNOWFLAKE, feet, 15, 0.3, 0.2, 0.3, 0.05);
                        DisplayBuilder.playSound(feet, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 1.5f);
                    } else {
                        // Moving — light frost hint at feet
                        DisplayBuilder.dustParticles(player.getLocation(), 5, 0.3, ICE_R2, ICE_G2, ICE_B2, 0.8f);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new FrostBite(plugin); }
    }

    // ========================================================================
    // 4. HypothermiaWave — Damage starts at 0.5x config, increases 0.1x every
    //    40 ticks
    // ========================================================================
    public static class HypothermiaWave extends EnvironmentalAttack {
        private double damageMultiplier;

        public HypothermiaWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hypothermia_wave", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(0); // Manual damage with scaling multiplier
            config.setDamageRadius(0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            damageMultiplier = 0.5;
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 1.0f);
            DisplayBuilder.dustParticles(center, 30, 3.0, ICE_R1, ICE_G1, ICE_B1, 1.2f);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World world = center.getWorld();
            double radius = 8.0;
            double baseDamage = 30.0;

            // Increase multiplier every 40 ticks
            if (tick % 40 == 0 && tick > 0) {
                damageMultiplier += 0.1;
            }

            // Ambient particles — intensity scales with multiplier
            int particleCount = (int) (10 * damageMultiplier);
            world.spawnParticle(Particle.SNOWFLAKE, center, particleCount, 4, 2, 4, 0.03);

            // Frost wave visual every 10 ticks
            if (tick % 10 == 0) {
                double ringRadius = (tick % 40) * 0.25; // Pulsing ring
                if (ringRadius > 0 && ringRadius <= radius) {
                    DisplayBuilder.particleRing(center, ringRadius, Particle.DUST, 30,
                            new Particle.DustOptions(Color.fromRGB(ICE_R1, ICE_G1, ICE_B1), 1.3f));
                }
            }

            // Apply scaled damage every 15 ticks
            if (tick % 15 == 0) {
                double scaledDamage = baseDamage * damageMultiplier;
                for (Player player : world.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= radius * radius) {
                        player.damage(scaledDamage);
                        player.setNoDamageTicks(0);
                        DisplayBuilder.dustParticles(player.getLocation(), 10, 0.5,
                                ICE_R3, ICE_G3, ICE_B3, 1.5f);
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f,
                        (float) (0.5 + damageMultiplier * 0.3));
            }
        }

        @Override public AbstractAttack newInstance() { return new HypothermiaWave(plugin); }
    }

    // ========================================================================
    // 5. DeepFreeze — Heavy snowflake storm particles, high constant area damage
    // ========================================================================
    public static class DeepFreeze extends EnvironmentalAttack {

        public DeepFreeze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("deep_freeze", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0); // High constant damage
            config.setDamageRadius(10.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10); // Frequent damage ticks
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 2.0f, 0.3f);
            World world = center.getWorld();
            if (world == null) return;
            // Massive initial snowflake burst
            world.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 5, 0), 100, 6, 5, 6, 0.1);
            DisplayBuilder.dustParticles(center, 60, 5.0, ICE_R3, ICE_G3, ICE_B3, 2.0f);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World world = center.getWorld();

            // Heavy snowflake storm — constant dense particles every tick
            world.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 6, 0), 40, 6, 4, 6, 0.08);
            world.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 3, 0), 30, 5, 2, 5, 0.06);

            // Ground-level frost clouds
            if (tick % 2 == 0) {
                world.spawnParticle(Particle.CLOUD, center, 10, 5, 0.5, 5, 0.02);
                DisplayBuilder.dustParticles(center, 15, 5.0, ICE_R2, ICE_G2, ICE_B2, 1.5f);
            }

            // Deep blue frost pillars rising
            if (tick % 8 == 0) {
                double angle = Math.random() * Math.PI * 2;
                double r = Math.random() * 8;
                Location pillar = center.clone().add(Math.cos(angle) * r, 0, Math.sin(angle) * r);
                for (double y = 0; y < 4; y += 0.5) {
                    DisplayBuilder.dustParticles(pillar.clone().add(0, y, 0), 3, 0.2,
                            ICE_R3, ICE_G3, ICE_B3, 1.8f);
                }
            }

            // Storm rumble sound
            if (tick % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.5f, 0.4f);
                DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 1.2f, 0.5f);
            }
        }

        @Override public AbstractAttack newInstance() { return new DeepFreeze(plugin); }
    }

    // ========================================================================
    // 6. ColdSnap — Single massive damage burst at tick 20, ice shatter particles
    // ========================================================================
    public static class ColdSnap extends EnvironmentalAttack {
        private boolean snapped;

        public ColdSnap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cold_snap", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(0); // No continuous damage — single burst
            config.setDamageRadius(0);
            config.setDurationTicks(60); // Short attack — buildup then snap
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            snapped = false;
            DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 1.5f, 0.3f);
            // Ominous quiet frost gathering
            DisplayBuilder.dustParticles(center, 20, 2.0, ICE_R2, ICE_G2, ICE_B2, 0.8f);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World world = center.getWorld();
            double radius = 10.0;

            if (tick < 20) {
                // Buildup: frost particles slowly gathering inward
                double shrinkRadius = radius * (1.0 - tick / 20.0);
                DisplayBuilder.particleRing(center, shrinkRadius, Particle.SNOWFLAKE, 20, null);
                DisplayBuilder.dustParticles(center, 5, shrinkRadius, ICE_R1, ICE_G1, ICE_B1, 1.0f);

                // Ticking tension sound
                if (tick % 5 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 0.5f,
                            0.5f + (tick / 20.0f) * 1.5f);
                }
            } else if (tick == 20 && !snapped) {
                // THE SNAP — massive burst damage
                snapped = true;
                for (Player player : world.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= radius * radius) {
                        player.damage(60.0); // Massive burst
                        player.setNoDamageTicks(0);
                    }
                }

                // Ice shatter particle explosion
                world.spawnParticle(Particle.ITEM_SNOWBALL, center, 120, 5, 3, 5, 0.3);
                world.spawnParticle(Particle.END_ROD, center, 50, 5, 3, 5, 0.15);
                DisplayBuilder.dustParticles(center, 80, 6.0, ICE_R1, ICE_G1, ICE_B1, 2.5f);
                DisplayBuilder.dustParticles(center, 60, 4.0, ICE_R3, ICE_G3, ICE_B3, 2.0f);

                // Shatter sound
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 2.0f, 0.3f);
            } else {
                // Post-snap: fading shards
                int fade = tick - 20;
                int count = Math.max(1, 30 - fade * 2);
                world.spawnParticle(Particle.ITEM_SNOWBALL, center, count, 6, 2, 6, 0.05);
                DisplayBuilder.dustParticles(center, count / 2, 5.0, ICE_R2, ICE_G2, ICE_B2, 1.0f);
            }
        }

        @Override public AbstractAttack newInstance() { return new ColdSnap(plugin); }
    }

    // ========================================================================
    // 7. ShiveringFit — Light damage ticks every 30 ticks, tremor particles at
    //    player feet
    // ========================================================================
    public static class ShiveringFit extends EnvironmentalAttack {

        public ShiveringFit(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shivering_fit", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(15.0); // Light damage
            config.setDamageRadius(7.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(30); // Every 30 ticks
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 1.0f, 1.2f);
            DisplayBuilder.dustParticles(center, 20, 3.0, ICE_R2, ICE_G2, ICE_B2, 0.8f);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World world = center.getWorld();

            // Tremor particles at every nearby player's feet
            for (Player player : world.getPlayers()) {
                if (player.getLocation().distanceSquared(center) > 49) continue; // 7 block radius

                Location feet = player.getLocation();

                // Jittering/tremoring frost dust at feet — small offsets for shaking effect
                if (tick % 3 == 0) {
                    double jitterX = (Math.random() - 0.5) * 0.4;
                    double jitterZ = (Math.random() - 0.5) * 0.4;
                    DisplayBuilder.dustParticles(feet.clone().add(jitterX, 0.1, jitterZ),
                            4, 0.15, ICE_R2, ICE_G2, ICE_B2, 0.9f);
                }

                // Small snowflake puffs
                if (tick % 6 == 0) {
                    world.spawnParticle(Particle.SNOWFLAKE, feet.clone().add(0, 0.2, 0),
                            3, 0.2, 0.1, 0.2, 0.01);
                }
            }

            // Damage pulse sound
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.7f, 1.5f);
            }

            // Ambient area frost
            if (tick % 10 == 0) {
                DisplayBuilder.dustParticles(center, 8, 4.0, ICE_R1, ICE_G1, ICE_B1, 0.7f);
            }
        }

        @Override public AbstractAttack newInstance() { return new ShiveringFit(plugin); }
    }

    // ========================================================================
    // 8. BreathCloud — Small frost cloud near player face height (y+1.5),
    //    light damage
    // ========================================================================
    public static class BreathCloud extends EnvironmentalAttack {

        public BreathCloud(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("breath_cloud", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(12.0); // Light damage
            config.setDamageRadius(6.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World world = center.getWorld();

            // Small frost breath clouds near each player's face
            for (Player player : world.getPlayers()) {
                if (player.getLocation().distanceSquared(center) > 36) continue; // 6 block radius

                Location face = player.getLocation().clone().add(0, 1.5, 0);

                // Get player facing direction for breath puff offset
                Vector dir = player.getLocation().getDirection().normalize().multiply(0.4);
                Location breathLoc = face.clone().add(dir.getX(), 0, dir.getZ());

                if (tick % 4 == 0) {
                    // Small puff of frost cloud
                    world.spawnParticle(Particle.CLOUD, breathLoc, 3, 0.15, 0.1, 0.15, 0.01);
                    DisplayBuilder.dustParticles(breathLoc, 5, 0.2, ICE_R2, ICE_G2, ICE_B2, 0.7f);
                }

                // Occasional snowflake in breath
                if (tick % 8 == 0) {
                    world.spawnParticle(Particle.SNOWFLAKE, breathLoc, 2, 0.1, 0.1, 0.1, 0.01);
                }
            }

            // Ambient cold air particles
            if (tick % 6 == 0) {
                world.spawnParticle(Particle.CLOUD, center.clone().add(0, 1.5, 0), 5, 3, 0.5, 3, 0.01);
            }

            // Quiet ambient sound
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.4f, 1.8f);
            }
        }

        @Override public AbstractAttack newInstance() { return new BreathCloud(plugin); }
    }

    // ========================================================================
    // 9. BodyHeatDrain — Continuous low damage drain, frost particles rising
    //    from players
    // ========================================================================
    public static class BodyHeatDrain extends EnvironmentalAttack {

        public BodyHeatDrain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("body_heat_drain", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(10.0); // Low continuous drain
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(8); // Frequent small ticks
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.2f, 0.7f);
            DisplayBuilder.dustParticles(center, 30, 4.0, ICE_R3, ICE_G3, ICE_B3, 1.3f);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World world = center.getWorld();

            // Frost particles rising from each nearby player — heat leaving body
            for (Player player : world.getPlayers()) {
                if (player.getLocation().distanceSquared(center) > 64) continue; // 8 block radius

                Location pLoc = player.getLocation();

                if (tick % 3 == 0) {
                    // Frost rising from player body — multiple heights
                    for (double y = 0; y < 2.0; y += 0.5) {
                        double offX = (Math.random() - 0.5) * 0.6;
                        double offZ = (Math.random() - 0.5) * 0.6;
                        Location riseLoc = pLoc.clone().add(offX, y, offZ);
                        DisplayBuilder.dustParticles(riseLoc, 2, 0.1, ICE_R1, ICE_G1, ICE_B1, 1.0f);
                    }
                    // Upward-moving snowflakes (simulating heat leaving)
                    world.spawnParticle(Particle.SNOWFLAKE, pLoc.clone().add(0, 1, 0),
                            4, 0.3, 0.5, 0.3, 0.04);
                }

                // End rod particles rising (represents escaping warmth)
                if (tick % 6 == 0) {
                    world.spawnParticle(Particle.END_ROD, pLoc.clone().add(0, 0.5, 0),
                            2, 0.2, 0.8, 0.2, 0.03);
                }
            }

            // Ambient draining sound
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 0.8f, 0.4f);
            }
        }

        @Override public AbstractAttack newInstance() { return new BodyHeatDrain(plugin); }
    }

    // ========================================================================
    // 10. WindChill — Directional particles blowing one direction, damage if in
    //     wind path
    // ========================================================================
    public static class WindChill extends EnvironmentalAttack {
        private double windAngle; // Radians — direction the wind blows
        private double windDirX, windDirZ;

        public WindChill(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("wind_chill", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(0); // Manual damage — only in wind path
            config.setDamageRadius(0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            // Random fixed wind direction for this attack instance
            windAngle = Math.random() * Math.PI * 2;
            windDirX = Math.cos(windAngle);
            windDirZ = Math.sin(windAngle);

            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.5f, 1.2f);
            DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World world = center.getWorld();
            double pathWidth = 5.0; // Width of wind corridor
            double pathLength = 10.0; // Length of wind corridor

            // Spawn directional wind particles along the wind path
            if (tick % 2 == 0) {
                // Perpendicular direction for width spread
                double perpX = -windDirZ;
                double perpZ = windDirX;

                for (int i = 0; i < 15; i++) {
                    // Random position along wind corridor
                    double along = Math.random() * pathLength;
                    double across = (Math.random() - 0.5) * pathWidth;
                    double height = Math.random() * 3.0;

                    Location particleLoc = center.clone().add(
                            windDirX * along + perpX * across,
                            height,
                            windDirZ * along + perpZ * across
                    );

                    // Snowflake particles moving in wind direction
                    world.spawnParticle(Particle.SNOWFLAKE, particleLoc, 1,
                            windDirX * 0.3, 0, windDirZ * 0.3, 0.1);
                }

                // Frost dust streaks
                for (int i = 0; i < 8; i++) {
                    double along = Math.random() * pathLength;
                    double across = (Math.random() - 0.5) * pathWidth;
                    Location dustLoc = center.clone().add(
                            windDirX * along + perpX * across,
                            1.0 + Math.random() * 2,
                            windDirZ * along + perpZ * across
                    );
                    DisplayBuilder.dustParticles(dustLoc, 2, 0.2, ICE_R1, ICE_G1, ICE_B1, 1.2f);
                }
            }

            // Damage players in the wind corridor every 12 ticks
            if (tick % 12 == 0) {
                double perpX = -windDirZ;
                double perpZ = windDirX;

                for (Player player : world.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location pLoc = player.getLocation();

                    // Project player position onto wind axis
                    double dx = pLoc.getX() - center.getX();
                    double dz = pLoc.getZ() - center.getZ();

                    // Along-wind distance
                    double alongDist = dx * windDirX + dz * windDirZ;
                    // Cross-wind distance
                    double crossDist = Math.abs(dx * perpX + dz * perpZ);

                    // Player in wind corridor?
                    if (alongDist >= -1 && alongDist <= pathLength && crossDist <= pathWidth / 2) {
                        player.damage(28.0);
                        player.setNoDamageTicks(0);
                        DisplayBuilder.dustParticles(pLoc, 12, 0.5, ICE_R3, ICE_G3, ICE_B3, 1.5f);
                        world.spawnParticle(Particle.SNOWFLAKE, pLoc.clone().add(0, 1, 0),
                                8, 0.3, 0.5, 0.3, 0.08);
                    }
                }
            }

            // Wind howl sound
            if (tick % 18 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 1.4f);
            }
        }

        @Override public AbstractAttack newInstance() { return new WindChill(plugin); }
    }

    // ========================================================================
    // 11. FrostCreepEnv — Expanding ground frost ring, grows 0.5 blocks/tick,
    //     damage inside
    // ========================================================================
    public static class FrostCreepEnv extends EnvironmentalAttack {
        private double frostRadius;

        public FrostCreepEnv(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_creep_env", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(0); // Manual damage based on expanding radius
            config.setDamageRadius(0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            frostRadius = 0;
            DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 1.5f, 0.3f);
            // Initial ice crack at center
            DisplayBuilder.dustParticles(center, 15, 0.3, ICE_R3, ICE_G3, ICE_B3, 2.0f);
            center.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, center, 10, 0.2, 0.1, 0.2, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World world = center.getWorld();

            // Grow frost radius — 0.5 blocks per tick, cap at 10
            frostRadius = Math.min(10.0, frostRadius + 0.5);

            // Ground frost ring at the expanding edge
            DisplayBuilder.particleRing(center, frostRadius, Particle.DUST, (int) (frostRadius * 6),
                    new Particle.DustOptions(Color.fromRGB(ICE_R2, ICE_G2, ICE_B2), 1.3f));
            DisplayBuilder.particleRing(center, frostRadius, Particle.SNOWFLAKE,
                    (int) (frostRadius * 3), null);

            // Inner frost ground coverage — scattered particles inside the circle
            if (tick % 3 == 0) {
                for (int i = 0; i < (int) (frostRadius * 2); i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double dist = Math.random() * frostRadius;
                    Location ground = center.clone().add(
                            Math.cos(angle) * dist, 0.1, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(ground, 2, 0.2, ICE_R1, ICE_G1, ICE_B1, 0.8f);
                }
            }

            // Damage every player inside the frost circle every 10 ticks
            if (tick % 10 == 0) {
                for (Player player : world.getPlayers()) {
                    if (isExempt(player)) continue;
                    double dist = player.getLocation().distance(center);
                    if (dist <= frostRadius) {
                        // More damage closer to center
                        double dmgFactor = 1.0 - (dist / frostRadius) * 0.5;
                        player.damage(25.0 * dmgFactor);
                        player.setNoDamageTicks(0);
                        DisplayBuilder.dustParticles(player.getLocation(), 8, 0.3,
                                ICE_R3, ICE_G3, ICE_B3, 1.4f);
                    }
                }
            }

            // Creeping ice sound
            if (tick % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 1.0f, 0.5f);
            }

            // Glass cracking sound when radius hits milestones
            if (frostRadius > 0 && frostRadius % 2.5 < 0.5 && tick % 5 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.6f, 1.5f);
            }
        }

        @Override public AbstractAttack newInstance() { return new FrostCreepEnv(plugin); }
    }

    // ========================================================================
    // 12. CoreFreeze — Delayed: 40 tick warmup with gathering particles, then
    //     burst damage
    // ========================================================================
    public static class CoreFreeze extends EnvironmentalAttack {
        private boolean burst;

        public CoreFreeze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("core_freeze", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(0); // No continuous — single delayed burst
            config.setDamageRadius(0);
            config.setDurationTicks(100); // 40 warmup + 60 aftermath
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            burst = false;
            DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World world = center.getWorld();
            double radius = 8.0;

            if (tick < 40) {
                // WARMUP PHASE: particles gathering inward toward center
                double gatherRadius = radius * (1.0 - tick / 40.0);

                // Particles spiraling inward
                int points = 24;
                double spiralOffset = tick * 0.15;
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points + spiralOffset;
                    Location point = center.clone().add(
                            Math.cos(angle) * gatherRadius, 0.5, Math.sin(angle) * gatherRadius);
                    DisplayBuilder.dustParticles(point, 2, 0.1, ICE_R1, ICE_G1, ICE_B1, 1.2f);
                }

                // Snowflakes converging
                world.spawnParticle(Particle.SNOWFLAKE, center, 5 + tick / 4,
                        gatherRadius, 1, gatherRadius, 0.02);

                // End rod sparks gathering
                if (tick % 4 == 0) {
                    world.spawnParticle(Particle.END_ROD, center, 3,
                            gatherRadius * 0.5, 0.5, gatherRadius * 0.5, 0.03);
                }

                // Intensifying sound
                if (tick % 8 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 0.6f + tick * 0.02f,
                            0.5f + tick * 0.03f);
                }

                // Center glow intensifies
                DisplayBuilder.dustParticles(center, 3 + tick / 5, 0.3,
                        ICE_R3, ICE_G3, ICE_B3, 1.5f + tick * 0.02f);

            } else if (tick == 40 && !burst) {
                // BURST — massive freeze explosion
                burst = true;

                for (Player player : world.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= radius * radius) {
                        player.damage(55.0);
                        player.setNoDamageTicks(0);
                    }
                }

                // Explosion of ice particles
                world.spawnParticle(Particle.ITEM_SNOWBALL, center, 100, 4, 3, 4, 0.25);
                world.spawnParticle(Particle.SNOWFLAKE, center, 80, 5, 3, 5, 0.15);
                world.spawnParticle(Particle.END_ROD, center, 40, 3, 2, 3, 0.2);
                DisplayBuilder.dustParticles(center, 80, 5.0, ICE_R1, ICE_G1, ICE_B1, 2.5f);
                DisplayBuilder.dustParticles(center, 50, 3.0, ICE_R3, ICE_G3, ICE_B3, 2.0f);

                // Shatter sounds
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.4f);
                DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 2.0f, 0.3f);

            } else if (tick > 40) {
                // AFTERMATH: fading frost residue
                int fade = tick - 40;
                int count = Math.max(2, 40 - fade);
                world.spawnParticle(Particle.SNOWFLAKE, center, count / 2, 5, 2, 5, 0.02);
                DisplayBuilder.dustParticles(center, count / 3, 4.0, ICE_R2, ICE_G2, ICE_B2, 0.8f);
            }
        }

        @Override public AbstractAttack newInstance() { return new CoreFreeze(plugin); }
    }

    // ========================================================================
    // 13. ThawFakeout — Warm orange particles first 60 ticks (no damage), then
    //     sudden frost burst damage
    // ========================================================================
    public static class ThawFakeout extends EnvironmentalAttack {
        private boolean frostBurst;

        public ThawFakeout(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("thaw_fakeout", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(0); // Manual — no damage during warm phase, burst after
            config.setDamageRadius(0);
            config.setDurationTicks(120); // 60 warm + 60 frost
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            frostBurst = false;
            // "Warming up" — comforting sounds
            DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World world = center.getWorld();
            double radius = 8.0;

            if (tick < 60) {
                // WARM PHASE: Orange/amber "thawing" particles — no damage
                // Players think the cold is ending...

                // Warm orange dust particles
                if (tick % 3 == 0) {
                    DisplayBuilder.dustParticles(center, 15, 4.0, 255, 160, 50, 1.3f); // Orange
                    DisplayBuilder.dustParticles(center, 10, 3.0, 255, 200, 80, 1.0f); // Pale amber
                }

                // Warm ember-like end rods
                if (tick % 5 == 0) {
                    world.spawnParticle(Particle.END_ROD, center.clone().add(0, 0.5, 0),
                            8, 3, 1, 3, 0.02);
                }

                // Comforting warm rings expanding outward
                if (tick % 10 == 0) {
                    double ringRadius = (tick % 30) * 0.3;
                    DisplayBuilder.particleRing(center, ringRadius, Particle.DUST, 20,
                            new Particle.DustOptions(Color.fromRGB(255, 180, 60), 1.2f));
                }

                // "Safe" ambient sound
                if (tick % 20 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 0.5f, 1.8f);
                }

            } else if (tick == 60 && !frostBurst) {
                // THE FAKEOUT — sudden frost explosion!
                frostBurst = true;

                for (Player player : world.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= radius * radius) {
                        player.damage(50.0); // Punishing burst
                        player.setNoDamageTicks(0);
                    }
                }

                // Dramatic transition: warm -> frozen
                world.spawnParticle(Particle.ITEM_SNOWBALL, center, 100, 5, 3, 5, 0.3);
                world.spawnParticle(Particle.SNOWFLAKE, center, 80, 5, 3, 5, 0.15);
                DisplayBuilder.dustParticles(center, 80, 5.0, ICE_R1, ICE_G1, ICE_B1, 2.5f);
                DisplayBuilder.dustParticles(center, 60, 4.0, ICE_R3, ICE_G3, ICE_B3, 2.0f);

                // Shattering freeze sounds
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 2.0f, 0.2f);

            } else if (tick > 60) {
                // POST-BURST: lingering frost, light continuous damage
                int elapsed = tick - 60;

                // Frost particles lingering
                if (tick % 3 == 0) {
                    world.spawnParticle(Particle.SNOWFLAKE, center, 15, 4, 2, 4, 0.03);
                    DisplayBuilder.dustParticles(center, 10, 4.0, ICE_R2, ICE_G2, ICE_B2, 1.0f);
                }

                // Light residual damage every 15 ticks after burst
                if (elapsed % 15 == 0) {
                    for (Player player : world.getPlayers()) {
                        if (isExempt(player)) continue;
                        if (player.getLocation().distanceSquared(center) <= radius * radius) {
                            player.damage(15.0);
                            player.setNoDamageTicks(0);
                            DisplayBuilder.dustParticles(player.getLocation(), 6, 0.3,
                                    ICE_R3, ICE_G3, ICE_B3, 1.2f);
                        }
                    }
                    DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 1.0f);
                }

                // Cloud residue fading
                if (tick % 5 == 0) {
                    int fadeCount = Math.max(2, 20 - elapsed / 2);
                    world.spawnParticle(Particle.CLOUD, center, fadeCount, 3, 1, 3, 0.01);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new ThawFakeout(plugin); }
    }
}
