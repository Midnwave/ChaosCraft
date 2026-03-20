package com.blockforge.chaoscraft.modes.freezingice.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class FrostCreepEffects {
    private FrostCreepEffects() {}

    // Frost palette
    private static final int FROST_R = 100, FROST_G = 180, FROST_B = 255;
    private static final int ICE_R = 220, ICE_G = 240, ICE_B = 255;

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new GroundFreeze(plugin));
        registry.register(new PowderSnowTrap(plugin));
        registry.register(new BlackIce(plugin));
        registry.register(new FrostRime(plugin));
        registry.register(new IceSlick(plugin));
        registry.register(new PermafrostSpread(plugin));
        registry.register(new SnowDrift(plugin));
        registry.register(new FreezingFog(plugin));
        registry.register(new CrystalGrowth(plugin));
        registry.register(new FrostLine(plugin));
        registry.register(new IcePatches(plugin));
        registry.register(new GlacialAdvance(plugin));
        registry.register(new FlashFreeze(plugin));
    }

    // ================================================================================
    // 1. GroundFreeze — Circular frost particle zone on ground, radius grows 2 -> 8
    // ================================================================================
    public static class GroundFreeze extends EnvironmentalAttack {
        private Location center;

        public GroundFreeze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ground_freeze", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World world = center.getWorld();
            if (world == null) return;

            // Initial freeze crack sound
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 1.5f, 0.6f);

            // Initial frost burst at center
            DisplayBuilder.dustParticles(center, 30, 1.0, FROST_R, FROST_G, FROST_B, 1.5f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World world = center.getWorld();

            // Radius grows from 2 to 8 over the duration
            double progress = Math.min(1.0, (double) tick / config.getDurationTicks());
            double radius = 2.0 + (6.0 * progress);

            // Ground-level frost particle ring (expanding)
            int ringPoints = (int) (radius * 10);
            Particle.DustOptions frostDust = new Particle.DustOptions(
                    Color.fromRGB(FROST_R, FROST_G, FROST_B), 1.3f);
            for (int i = 0; i < ringPoints; i++) {
                double angle = (2.0 * Math.PI * i) / ringPoints;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location point = center.clone().add(x, 0.1, z);
                world.spawnParticle(Particle.DUST, point, 1, 0.2, 0, 0.2, 0, frostDust);
            }

            // Fill interior with scattered snowflakes every 3 ticks
            if (tick % 3 == 0) {
                int fillCount = (int) (radius * 5);
                for (int i = 0; i < fillCount; i++) {
                    double angle = ThreadLocalRandom.current().nextDouble(2.0 * Math.PI);
                    double dist = ThreadLocalRandom.current().nextDouble(radius);
                    Location fill = center.clone().add(
                            Math.cos(angle) * dist, 0.15, Math.sin(angle) * dist);
                    world.spawnParticle(Particle.SNOWFLAKE, fill, 1, 0.1, 0.05, 0.1, 0.01);
                }
            }

            // Icy crackle sound periodically
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.6f, 1.8f);
            }

            // Update damage radius to match visual radius
            config.setDamageRadius(radius);
        }

        @Override public AbstractAttack newInstance() { return new GroundFreeze(plugin); }
    }

    // ================================================================================
    // 2. PowderSnowTrap — Dense snowflake particles in 5-block area, heavy damage/10t
    // ================================================================================
    public static class PowderSnowTrap extends EnvironmentalAttack {
        private Location center;

        public PowderSnowTrap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("powder_snow_trap", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World world = center.getWorld();
            if (world == null) return;

            // Heavy snowfall start sound
            DisplayBuilder.playSound(center, Sound.WEATHER_RAIN, 1.5f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_FALL, 1.5f, 0.8f);

            // Initial dense burst
            world.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 1, 0),
                    80, 2.5, 1.5, 2.5, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World world = center.getWorld();

            double radius = 5.0;

            // Dense falling snowflakes throughout the trap area
            for (int i = 0; i < 40; i++) {
                double angle = ThreadLocalRandom.current().nextDouble(2.0 * Math.PI);
                double dist = ThreadLocalRandom.current().nextDouble(radius);
                double height = ThreadLocalRandom.current().nextDouble(3.0);
                Location snow = center.clone().add(
                        Math.cos(angle) * dist, height, Math.sin(angle) * dist);
                world.spawnParticle(Particle.SNOWFLAKE, snow, 1, 0.1, 0.3, 0.1, 0.01);
            }

            // Ground-level accumulation particles (white dust)
            if (tick % 2 == 0) {
                for (int i = 0; i < 20; i++) {
                    double angle = ThreadLocalRandom.current().nextDouble(2.0 * Math.PI);
                    double dist = ThreadLocalRandom.current().nextDouble(radius);
                    Location ground = center.clone().add(
                            Math.cos(angle) * dist, 0.1, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(ground, 2, 0.3, ICE_R, ICE_G, ICE_B, 1.8f);
                }
            }

            // Powder snow step sounds
            if (tick % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 1.0f, 0.7f);
            }

            // Boundary ring outline
            if (tick % 5 == 0) {
                DisplayBuilder.particleRing(center.clone().add(0, 0.2, 0), radius,
                        Particle.DUST, 30,
                        new Particle.DustOptions(Color.fromRGB(ICE_R, ICE_G, ICE_B), 1.0f));
            }
        }

        @Override public AbstractAttack newInstance() { return new PowderSnowTrap(plugin); }
    }

    // ================================================================================
    // 3. BlackIce — NO visible particles (stealth!), just damage + faint crack sounds
    // ================================================================================
    public static class BlackIce extends EnvironmentalAttack {
        private Location center;

        public BlackIce(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("black_ice", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            // Only a very faint initial crack — no visual warning
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.3f, 2.0f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            // Faint ice cracking sounds at irregular intervals — the only clue
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.25f, 1.9f);
            }
            if (tick % 37 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.2f, 2.0f);
            }
            if (tick % 60 == 0) {
                // Slightly louder crack to give a small audio hint
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.4f, 1.5f);
            }

            // NO particles at all — this is a stealth attack
        }

        @Override public AbstractAttack newInstance() { return new BlackIce(plugin); }
    }

    // ================================================================================
    // 4. FrostRime — Frost particles coat area, damage every 15 ticks, ice sounds
    // ================================================================================
    public static class FrostRime extends EnvironmentalAttack {
        private Location center;

        public FrostRime(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_rime", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World world = center.getWorld();
            if (world == null) return;

            // Initial ice formation sounds
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 1.5f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.6f);

            // Burst of frost coating particles
            DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), 60, 3.5,
                    ICE_R, ICE_G, ICE_B, 2.0f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World world = center.getWorld();

            double radius = 7.0;

            // Frost rime coating on the ground — scattered white dust particles
            if (tick % 2 == 0) {
                for (int i = 0; i < 25; i++) {
                    double angle = ThreadLocalRandom.current().nextDouble(2.0 * Math.PI);
                    double dist = ThreadLocalRandom.current().nextDouble(radius);
                    Location rime = center.clone().add(
                            Math.cos(angle) * dist, 0.05, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(rime, 2, 0.2, ICE_R, ICE_G, ICE_B, 1.6f);
                }
            }

            // Floating frost motes slightly above ground
            if (tick % 4 == 0) {
                for (int i = 0; i < 10; i++) {
                    double angle = ThreadLocalRandom.current().nextDouble(2.0 * Math.PI);
                    double dist = ThreadLocalRandom.current().nextDouble(radius);
                    Location mote = center.clone().add(
                            Math.cos(angle) * dist, 0.3 + ThreadLocalRandom.current().nextDouble(0.5),
                            Math.sin(angle) * dist);
                    world.spawnParticle(Particle.SNOWFLAKE, mote, 1, 0.1, 0.05, 0.1, 0.005);
                }
            }

            // Rime edge ring
            if (tick % 6 == 0) {
                DisplayBuilder.particleRing(center.clone().add(0, 0.1, 0), radius,
                        Particle.DUST, 40,
                        new Particle.DustOptions(Color.fromRGB(FROST_R, FROST_G, FROST_B), 1.2f));
            }

            // Ice crackle sounds
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.7f, 1.6f);
            }
            if (tick % 45 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 0.5f, 0.5f);
            }
        }

        @Override public AbstractAttack newInstance() { return new FrostRime(plugin); }
    }

    // ================================================================================
    // 5. IceSlick — Ground-level particles in 8-block radius, damage + knockback
    // ================================================================================
    public static class IceSlick extends EnvironmentalAttack {
        private Location center;

        public IceSlick(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_slick", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World world = center.getWorld();
            if (world == null) return;

            // Slick ice forming sound
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 1.0f, 1.5f);

            // Ground-level ice sheen burst
            DisplayBuilder.dustParticles(center.clone().add(0, 0.1, 0), 50, 4.0,
                    FROST_R, FROST_G, FROST_B, 1.0f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World world = center.getWorld();

            double radius = 8.0;

            // Ground-level slick ice particles (very low, looks like a frozen sheet)
            if (tick % 2 == 0) {
                for (int i = 0; i < 30; i++) {
                    double angle = ThreadLocalRandom.current().nextDouble(2.0 * Math.PI);
                    double dist = ThreadLocalRandom.current().nextDouble(radius);
                    Location ice = center.clone().add(
                            Math.cos(angle) * dist, 0.05, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(ice, 1, 0.1, FROST_R, FROST_G, FROST_B, 0.8f);
                }
            }

            // Shimmer effect (lighter ice particles)
            if (tick % 5 == 0) {
                for (int i = 0; i < 8; i++) {
                    double angle = ThreadLocalRandom.current().nextDouble(2.0 * Math.PI);
                    double dist = ThreadLocalRandom.current().nextDouble(radius);
                    Location shimmer = center.clone().add(
                            Math.cos(angle) * dist, 0.1, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(shimmer, 1, 0.1, ICE_R, ICE_G, ICE_B, 1.2f);
                }
            }

            // Knockback: push players outward from center every 20 ticks
            if (tick % 20 == 0) {
                for (Player player : world.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= radius * radius) {
                        Vector push = player.getLocation().toVector()
                                .subtract(center.toVector()).normalize().multiply(0.8);
                        push.setY(0.15); // Slight upward lift
                        player.setVelocity(player.getVelocity().add(push));
                        DisplayBuilder.playSound(player.getLocation(),
                                Sound.BLOCK_GLASS_BREAK, 0.4f, 2.0f);
                    }
                }
            }

            // Sliding sound
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 0.6f, 1.8f);
            }
        }

        @Override public AbstractAttack newInstance() { return new IceSlick(plugin); }
    }

    // ================================================================================
    // 6. PermafrostSpread — Expanding frost ring from center, grows 0.5 blocks/tick
    // ================================================================================
    public static class PermafrostSpread extends EnvironmentalAttack {
        private Location center;

        public PermafrostSpread(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("permafrost_spread", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World world = center.getWorld();
            if (world == null) return;

            // Deep rumble and crack as permafrost begins spreading
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 0.8f, 0.4f);

            // Center frost burst
            DisplayBuilder.dustParticles(center, 40, 0.5, FROST_R, FROST_G, FROST_B, 2.0f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World world = center.getWorld();

            // Ring radius grows at 0.5 blocks per tick, capped at 10
            double currentRadius = Math.min(10.0, tick * 0.5);

            // Leading edge ring — bright frost particles
            int ringPoints = Math.max(16, (int) (currentRadius * 12));
            Particle.DustOptions frostEdge = new Particle.DustOptions(
                    Color.fromRGB(FROST_R, FROST_G, FROST_B), 1.5f);
            for (int i = 0; i < ringPoints; i++) {
                double angle = (2.0 * Math.PI * i) / ringPoints;
                Location point = center.clone().add(
                        Math.cos(angle) * currentRadius, 0.1,
                        Math.sin(angle) * currentRadius);
                world.spawnParticle(Particle.DUST, point, 1, 0.1, 0, 0.1, 0, frostEdge);
            }

            // Interior fill — lighter frosty surface behind the ring
            if (tick % 3 == 0 && currentRadius > 1.0) {
                int fillCount = (int) (currentRadius * 4);
                for (int i = 0; i < fillCount; i++) {
                    double angle = ThreadLocalRandom.current().nextDouble(2.0 * Math.PI);
                    double dist = ThreadLocalRandom.current().nextDouble(currentRadius * 0.9);
                    Location fill = center.clone().add(
                            Math.cos(angle) * dist, 0.05, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(fill, 1, 0.15, ICE_R, ICE_G, ICE_B, 1.0f);
                }
            }

            // Snowflake motes rising from the frozen ground
            if (tick % 4 == 0) {
                for (int i = 0; i < 6; i++) {
                    double angle = ThreadLocalRandom.current().nextDouble(2.0 * Math.PI);
                    double dist = ThreadLocalRandom.current().nextDouble(currentRadius);
                    Location mote = center.clone().add(Math.cos(angle) * dist, 0.2, Math.sin(angle) * dist);
                    world.spawnParticle(Particle.SNOWFLAKE, mote, 1, 0.05, 0.15, 0.05, 0.01);
                }
            }

            // Cracking sounds as it spreads
            if (tick % 10 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.5f, 1.2f + (float)(currentRadius * 0.05));
            }

            // Update damage radius to match current visual spread
            config.setDamageRadius(currentRadius);
        }

        @Override public AbstractAttack newInstance() { return new PermafrostSpread(plugin); }
    }

    // ================================================================================
    // 7. SnowDrift — Particle pile growing at center (y increases), damage grows
    // ================================================================================
    public static class SnowDrift extends EnvironmentalAttack {
        private Location center;

        public SnowDrift(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("snow_drift", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World world = center.getWorld();
            if (world == null) return;

            // Wind and snow sounds
            DisplayBuilder.playSound(center, Sound.WEATHER_RAIN, 0.8f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World world = center.getWorld();

            // Drift height grows over time (0 to 4 blocks)
            double progress = Math.min(1.0, (double) tick / config.getDurationTicks());
            double driftHeight = progress * 4.0;
            double driftRadius = 3.0 + (progress * 2.0); // Widens slightly as it grows

            // Snow pile — dense particles filling the drift shape
            for (int i = 0; i < 35; i++) {
                double angle = ThreadLocalRandom.current().nextDouble(2.0 * Math.PI);
                double dist = ThreadLocalRandom.current().nextDouble(driftRadius);
                // Taper toward the top: narrower at higher y
                double yPos = ThreadLocalRandom.current().nextDouble(driftHeight);
                double taperFactor = 1.0 - (yPos / (driftHeight + 0.1)) * 0.6;
                Location snow = center.clone().add(
                        Math.cos(angle) * dist * taperFactor, yPos,
                        Math.sin(angle) * dist * taperFactor);
                world.spawnParticle(Particle.SNOWFLAKE, snow, 1, 0.15, 0.1, 0.15, 0.005);
            }

            // White dust particles for density at the base
            if (tick % 3 == 0) {
                for (int i = 0; i < 15; i++) {
                    double angle = ThreadLocalRandom.current().nextDouble(2.0 * Math.PI);
                    double dist = ThreadLocalRandom.current().nextDouble(driftRadius);
                    double yPos = ThreadLocalRandom.current().nextDouble(Math.max(0.5, driftHeight * 0.5));
                    Location base = center.clone().add(
                            Math.cos(angle) * dist, yPos, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(base, 2, 0.2, ICE_R, ICE_G, ICE_B, 1.5f);
                }
            }

            // Falling snowflakes onto the drift from above
            if (tick % 2 == 0) {
                for (int i = 0; i < 8; i++) {
                    double angle = ThreadLocalRandom.current().nextDouble(2.0 * Math.PI);
                    double dist = ThreadLocalRandom.current().nextDouble(driftRadius);
                    Location falling = center.clone().add(
                            Math.cos(angle) * dist, driftHeight + 1.5 + ThreadLocalRandom.current().nextDouble(1.0),
                            Math.sin(angle) * dist);
                    world.spawnParticle(Particle.SNOWFLAKE, falling, 1, 0.1, 0.3, 0.1, 0.02);
                }
            }

            // Wind sounds as drift builds
            if (tick % 35 == 0) {
                DisplayBuilder.playSound(center, Sound.WEATHER_RAIN, 0.6f, 0.3f);
            }
            if (tick % 50 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_FALL, 0.8f, 0.6f);
            }

            // Scale damage with drift height (bigger drift = more dangerous)
            config.setDamageRadius(driftRadius);
        }

        @Override public AbstractAttack newInstance() { return new SnowDrift(plugin); }
    }

    // ================================================================================
    // 8. FreezingFog — Dense ground fog (y+0 to y+2), continuous damage, creeping sound
    // ================================================================================
    public static class FreezingFog extends EnvironmentalAttack {
        private Location center;

        public FreezingFog(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("freezing_fog", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(12);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World world = center.getWorld();
            if (world == null) return;

            // Eerie fog rolling in
            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 1.0f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 1.2f, 0.3f);

            // Initial fog puff
            DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 80, 4.0,
                    ICE_R, ICE_G, ICE_B, 2.5f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World world = center.getWorld();

            double radius = 8.0;

            // Dense ground fog — white dust particles from y+0 to y+2
            for (int i = 0; i < 40; i++) {
                double angle = ThreadLocalRandom.current().nextDouble(2.0 * Math.PI);
                double dist = ThreadLocalRandom.current().nextDouble(radius);
                double height = ThreadLocalRandom.current().nextDouble(2.0);
                Location fog = center.clone().add(
                        Math.cos(angle) * dist, height, Math.sin(angle) * dist);
                DisplayBuilder.dustParticles(fog, 1, 0.3, ICE_R, ICE_G, ICE_B, 2.0f);
            }

            // Frost dust mixed in for cold feeling
            if (tick % 2 == 0) {
                for (int i = 0; i < 15; i++) {
                    double angle = ThreadLocalRandom.current().nextDouble(2.0 * Math.PI);
                    double dist = ThreadLocalRandom.current().nextDouble(radius);
                    double height = ThreadLocalRandom.current().nextDouble(1.5);
                    Location frost = center.clone().add(
                            Math.cos(angle) * dist, height, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(frost, 1, 0.2, FROST_R, FROST_G, FROST_B, 1.5f);
                }
            }

            // Snowflake motes drifting in the fog
            if (tick % 3 == 0) {
                for (int i = 0; i < 8; i++) {
                    double angle = ThreadLocalRandom.current().nextDouble(2.0 * Math.PI);
                    double dist = ThreadLocalRandom.current().nextDouble(radius);
                    Location mote = center.clone().add(
                            Math.cos(angle) * dist,
                            ThreadLocalRandom.current().nextDouble(2.0),
                            Math.sin(angle) * dist);
                    world.spawnParticle(Particle.SNOWFLAKE, mote, 1, 0.2, 0.1, 0.2, 0.005);
                }
            }

            // Creeping ambient sounds
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.6f, 0.4f);
            }
            if (tick % 55 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 0.5f, 0.3f);
            }
        }

        @Override public AbstractAttack newInstance() { return new FreezingFog(plugin); }
    }

    // ================================================================================
    // 9. CrystalGrowth — Radiating END_ROD particles from center, radius+damage grow
    // ================================================================================
    public static class CrystalGrowth extends EnvironmentalAttack {
        private Location center;

        public CrystalGrowth(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_growth", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(12);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World world = center.getWorld();
            if (world == null) return;

            // Crystal formation sound
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 0.6f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 1.0f, 1.5f);

            // Initial crystal spark at center
            world.spawnParticle(Particle.END_ROD, center.clone().add(0, 0.5, 0),
                    30, 0.3, 0.5, 0.3, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World world = center.getWorld();

            // Crystal radius grows over time: 1 -> 10
            double progress = Math.min(1.0, (double) tick / config.getDurationTicks());
            double currentRadius = 1.0 + (9.0 * progress);

            // Radiating END_ROD crystal spikes outward from center
            int spikes = 8;
            for (int s = 0; s < spikes; s++) {
                double spikeAngle = (2.0 * Math.PI * s) / spikes + (tick * 0.02);
                int pointsAlongSpike = (int) (currentRadius * 3);
                for (int p = 0; p < pointsAlongSpike; p++) {
                    double dist = (currentRadius * p) / pointsAlongSpike;
                    double spikeHeight = 0.3 + (1.5 * (1.0 - (double) p / pointsAlongSpike));
                    Location crystal = center.clone().add(
                            Math.cos(spikeAngle) * dist, spikeHeight,
                            Math.sin(spikeAngle) * dist);
                    world.spawnParticle(Particle.END_ROD, crystal, 1, 0.05, 0.05, 0.05, 0.001);
                }
            }

            // Frost dust between the spikes for fill
            if (tick % 3 == 0) {
                for (int i = 0; i < 12; i++) {
                    double angle = ThreadLocalRandom.current().nextDouble(2.0 * Math.PI);
                    double dist = ThreadLocalRandom.current().nextDouble(currentRadius);
                    Location fill = center.clone().add(
                            Math.cos(angle) * dist, 0.2 + ThreadLocalRandom.current().nextDouble(0.8),
                            Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(fill, 1, 0.1, FROST_R, FROST_G, FROST_B, 1.0f);
                }
            }

            // Crystal ring at the growth edge
            if (tick % 4 == 0) {
                DisplayBuilder.particleRing(center.clone().add(0, 0.3, 0), currentRadius,
                        Particle.END_ROD, (int)(currentRadius * 6), null);
            }

            // Crystal chime sounds as it grows
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f,
                        0.5f + (float)(progress * 1.0));
            }
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 0.5f, 1.2f);
            }

            // Update damage radius to match crystal growth
            config.setDamageRadius(currentRadius);
        }

        @Override public AbstractAttack newInstance() { return new CrystalGrowth(plugin); }
    }

    // ================================================================================
    // 10. FrostLine — Line of frost from center toward target player, damage along line
    // ================================================================================
    public static class FrostLine extends EnvironmentalAttack {
        private Location center;
        private Vector direction;

        public FrostLine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_line", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(2.0); // Narrow line damage
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World world = center.getWorld();
            if (world == null) return;

            // Calculate direction toward target player
            Player target = getTargetPlayer();
            if (target != null && target.isOnline()) {
                direction = target.getLocation().toVector()
                        .subtract(center.toVector()).normalize();
            } else {
                // Random direction if no target
                double angle = ThreadLocalRandom.current().nextDouble(2.0 * Math.PI);
                direction = new Vector(Math.cos(angle), 0, Math.sin(angle));
            }
            direction.setY(0).normalize();

            // Ice crack sound
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.3f, 0.8f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null || direction == null) return;
            World world = center.getWorld();

            // Line extends over time: 0 to 15 blocks from center
            double progress = Math.min(1.0, (double) tick / (config.getDurationTicks() * 0.6));
            double lineLength = progress * 15.0;

            // Draw frost line from center outward
            Particle.DustOptions frostDust = new Particle.DustOptions(
                    Color.fromRGB(FROST_R, FROST_G, FROST_B), 1.4f);
            Particle.DustOptions iceDust = new Particle.DustOptions(
                    Color.fromRGB(ICE_R, ICE_G, ICE_B), 1.0f);

            int linePoints = (int) (lineLength * 4);
            for (int i = 0; i <= linePoints; i++) {
                double dist = (lineLength * i) / Math.max(1, linePoints);
                Location point = center.clone().add(
                        direction.getX() * dist, 0.1, direction.getZ() * dist);

                // Central frost line
                world.spawnParticle(Particle.DUST, point, 2, 0.1, 0, 0.1, 0, frostDust);

                // Side frost particles (width ~1 block each side)
                if (i % 2 == 0) {
                    Location left = point.clone().add(-direction.getZ() * 0.5, 0, direction.getX() * 0.5);
                    Location right = point.clone().add(direction.getZ() * 0.5, 0, -direction.getX() * 0.5);
                    world.spawnParticle(Particle.DUST, left, 1, 0.15, 0, 0.15, 0, iceDust);
                    world.spawnParticle(Particle.DUST, right, 1, 0.15, 0, 0.15, 0, iceDust);
                }
            }

            // Leading edge burst at the tip
            if (lineLength > 0.5) {
                Location tip = center.clone().add(
                        direction.getX() * lineLength, 0.2, direction.getZ() * lineLength);
                world.spawnParticle(Particle.SNOWFLAKE, tip, 5, 0.3, 0.2, 0.3, 0.02);
                DisplayBuilder.dustParticles(tip, 3, 0.3, FROST_R, FROST_G, FROST_B, 1.6f);
            }

            // Damage players along the line (manual check since base class uses radial)
            if (tick % 10 == 0) {
                for (Player player : world.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location pLoc = player.getLocation();
                    // Check if player is within 2 blocks of the frost line
                    for (double d = 0; d <= lineLength; d += 1.0) {
                        Location linePoint = center.clone().add(
                                direction.getX() * d, 0, direction.getZ() * d);
                        if (pLoc.distanceSquared(linePoint) <= 4.0) { // 2-block radius
                            player.damage(config.getDamage());
                            player.setNoDamageTicks(0);
                            break;
                        }
                    }
                }
            }

            // Cracking sound as line extends
            if (tick % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.6f, 1.5f);
            }
        }

        @Override public AbstractAttack newInstance() { return new FrostLine(plugin); }
    }

    // ================================================================================
    // 11. IcePatches — 5 random small frost zones (radius 2), damage on contact
    // ================================================================================
    public static class IcePatches extends EnvironmentalAttack {
        private Location center;
        private final List<Location> patchCenters = new ArrayList<>();

        public IcePatches(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_patches", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(2.0); // Per-patch radius; actual damage is manual
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            patchCenters.clear();
            World world = center.getWorld();
            if (world == null) return;

            // Generate 5 random patch positions within 10 blocks of center
            for (int i = 0; i < 5; i++) {
                double angle = ThreadLocalRandom.current().nextDouble(2.0 * Math.PI);
                double dist = 2.0 + ThreadLocalRandom.current().nextDouble(8.0);
                Location patch = center.clone().add(
                        Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                patchCenters.add(patch);

                // Spawn sound per patch
                DisplayBuilder.playSound(patch, Sound.BLOCK_GLASS_PLACE, 0.7f, 1.5f);
            }

            // Overall formation sound
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World world = center.getWorld();

            double patchRadius = 2.0;

            // Render each patch
            for (Location patch : patchCenters) {
                // Frost patch circle on ground
                if (tick % 3 == 0) {
                    DisplayBuilder.particleRing(patch.clone().add(0, 0.1, 0), patchRadius,
                            Particle.DUST, 16,
                            new Particle.DustOptions(Color.fromRGB(FROST_R, FROST_G, FROST_B), 1.2f));
                }

                // Fill interior with ice particles
                for (int i = 0; i < 6; i++) {
                    double angle = ThreadLocalRandom.current().nextDouble(2.0 * Math.PI);
                    double dist = ThreadLocalRandom.current().nextDouble(patchRadius);
                    Location ice = patch.clone().add(
                            Math.cos(angle) * dist, 0.05, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(ice, 1, 0.1, ICE_R, ICE_G, ICE_B, 1.0f);
                }

                // Occasional snowflake
                if (tick % 5 == 0) {
                    world.spawnParticle(Particle.SNOWFLAKE, patch.clone().add(0, 0.3, 0),
                            3, 1.0, 0.2, 1.0, 0.01);
                }
            }

            // Manual damage check: player within 2 blocks of any patch
            if (tick % 15 == 0) {
                for (Player player : world.getPlayers()) {
                    if (isExempt(player)) continue;
                    for (Location patch : patchCenters) {
                        if (player.getLocation().distanceSquared(patch) <= patchRadius * patchRadius) {
                            player.damage(config.getDamage());
                            player.setNoDamageTicks(0);
                            DisplayBuilder.playSound(player.getLocation(),
                                    Sound.BLOCK_GLASS_BREAK, 0.4f, 1.8f);
                            break; // Only damage once per tick even if on multiple patches
                        }
                    }
                }
            }

            // Ambient crackle
            if (tick % 30 == 0) {
                Location randomPatch = patchCenters.get(
                        ThreadLocalRandom.current().nextInt(patchCenters.size()));
                DisplayBuilder.playSound(randomPatch, Sound.BLOCK_GLASS_BREAK, 0.4f, 1.9f);
            }
        }

        @Override public AbstractAttack newInstance() { return new IcePatches(plugin); }
    }

    // ================================================================================
    // 12. GlacialAdvance — Wall of frost advancing from one direction across 20 blocks
    // ================================================================================
    public static class GlacialAdvance extends EnvironmentalAttack {
        private Location center;
        private Vector advanceDirection;
        private Location wallStart;

        public GlacialAdvance(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_advance", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(3.0); // Wave thickness
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World world = center.getWorld();
            if (world == null) return;

            // Random cardinal direction for the wall to advance
            int dir = ThreadLocalRandom.current().nextInt(4);
            switch (dir) {
                case 0 -> advanceDirection = new Vector(1, 0, 0);  // East
                case 1 -> advanceDirection = new Vector(-1, 0, 0); // West
                case 2 -> advanceDirection = new Vector(0, 0, 1);  // South
                case 3 -> advanceDirection = new Vector(0, 0, -1); // North
            }

            // Wall starts 10 blocks behind center in the advance direction
            wallStart = center.clone().add(
                    -advanceDirection.getX() * 10, 0, -advanceDirection.getZ() * 10);

            // Rumble as glacial wall begins
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.3f);
            DisplayBuilder.playSound(wallStart, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null || advanceDirection == null) return;
            World world = center.getWorld();

            // Wall advances across 20 blocks over the duration
            double progress = Math.min(1.0, (double) tick / config.getDurationTicks());
            double advanceDistance = progress * 20.0;

            // Current wall position
            Location wallCenter = wallStart.clone().add(
                    advanceDirection.getX() * advanceDistance, 0,
                    advanceDirection.getZ() * advanceDistance);

            // The wall is perpendicular to the advance direction
            Vector perpendicular = new Vector(-advanceDirection.getZ(), 0, advanceDirection.getX());
            double wallWidth = 10.0; // 10 blocks wide
            double wallHeight = 3.0;

            // Draw the frost wall
            Particle.DustOptions wallDust = new Particle.DustOptions(
                    Color.fromRGB(FROST_R, FROST_G, FROST_B), 1.8f);
            Particle.DustOptions wallDustLight = new Particle.DustOptions(
                    Color.fromRGB(ICE_R, ICE_G, ICE_B), 1.3f);

            int widthPoints = 30;
            int heightPoints = 8;
            for (int w = 0; w < widthPoints; w++) {
                double wOffset = ((double) w / widthPoints - 0.5) * wallWidth * 2;
                for (int h = 0; h < heightPoints; h++) {
                    double hOffset = ((double) h / heightPoints) * wallHeight;
                    Location wallPoint = wallCenter.clone().add(
                            perpendicular.getX() * wOffset, hOffset,
                            perpendicular.getZ() * wOffset);

                    // Alternate between frost and ice colors
                    if ((w + h) % 2 == 0) {
                        world.spawnParticle(Particle.DUST, wallPoint, 1, 0.15, 0.1, 0.15, 0, wallDust);
                    } else {
                        world.spawnParticle(Particle.DUST, wallPoint, 1, 0.15, 0.1, 0.15, 0, wallDustLight);
                    }
                }
            }

            // Snowflakes trailing behind the wall
            if (tick % 2 == 0) {
                for (int i = 0; i < 10; i++) {
                    double wOffset = (ThreadLocalRandom.current().nextDouble() - 0.5) * wallWidth * 2;
                    double trailDist = ThreadLocalRandom.current().nextDouble(3.0);
                    Location trail = wallCenter.clone().add(
                            perpendicular.getX() * wOffset - advanceDirection.getX() * trailDist,
                            ThreadLocalRandom.current().nextDouble(wallHeight),
                            perpendicular.getZ() * wOffset - advanceDirection.getZ() * trailDist);
                    world.spawnParticle(Particle.SNOWFLAKE, trail, 1, 0.2, 0.1, 0.2, 0.01);
                }
            }

            // Damage players caught in the advancing wall (3-block thickness)
            if (tick % 10 == 0) {
                for (Player player : world.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location pLoc = player.getLocation();

                    // Project player position onto advance axis
                    Vector toPlayer = pLoc.toVector().subtract(wallCenter.toVector());
                    double advanceDot = toPlayer.dot(advanceDirection);
                    double perpDot = toPlayer.dot(perpendicular);

                    // Within wall thickness (3 blocks) and wall width
                    if (Math.abs(advanceDot) <= 1.5 && Math.abs(perpDot) <= wallWidth
                            && pLoc.getY() >= wallCenter.getY() - 1
                            && pLoc.getY() <= wallCenter.getY() + wallHeight + 1) {
                        player.damage(config.getDamage());
                        player.setNoDamageTicks(0);
                    }
                }
            }

            // Rumble sounds as wall advances
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(wallCenter, Sound.ENTITY_IRON_GOLEM_HURT, 0.6f, 0.4f);
                DisplayBuilder.playSound(wallCenter, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.6f);
            }
        }

        @Override public AbstractAttack newInstance() { return new GlacialAdvance(plugin); }
    }

    // ================================================================================
    // 13. FlashFreeze — 30-tick buildup (gathering particles), then instant burst
    // ================================================================================
    public static class FlashFreeze extends EnvironmentalAttack {
        private Location center;
        private static final int BUILDUP_TICKS = 30;

        public FlashFreeze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("flash_freeze", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(60); // Short: 30 tick buildup + 30 tick aftermath
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(999); // We handle damage manually for the burst
            config.setDamageDelayTicks(999);   // Disable automatic damage entirely
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World world = center.getWorld();
            if (world == null) return;

            // Warning sound — rising pitch
            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.5f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World world = center.getWorld();

            double burstRadius = 10.0;

            if (tick < BUILDUP_TICKS) {
                // ---- BUILDUP PHASE: particles gather inward toward center ----
                double gatherProgress = (double) tick / BUILDUP_TICKS;

                // Particles converging from the outer radius toward center
                int gatherCount = 10 + (int)(gatherProgress * 30);
                double gatherRadius = burstRadius * (1.0 - gatherProgress * 0.7);

                for (int i = 0; i < gatherCount; i++) {
                    double angle = ThreadLocalRandom.current().nextDouble(2.0 * Math.PI);
                    double dist = gatherRadius + ThreadLocalRandom.current().nextDouble(2.0);
                    double height = ThreadLocalRandom.current().nextDouble(3.0);
                    Location gather = center.clone().add(
                            Math.cos(angle) * dist, height, Math.sin(angle) * dist);

                    // Particles move inward
                    Vector toCenter = center.toVector().add(new Vector(0, 1, 0))
                            .subtract(gather.toVector()).normalize().multiply(0.3);
                    world.spawnParticle(Particle.SNOWFLAKE, gather, 1,
                            toCenter.getX(), toCenter.getY(), toCenter.getZ(), 0.05);
                }

                // Center glow intensifies
                DisplayBuilder.dustParticles(center.clone().add(0, 1, 0),
                        (int)(gatherProgress * 25), 0.5 + gatherProgress,
                        FROST_R, FROST_G, FROST_B, 1.0f + (float)(gatherProgress * 1.5f));

                // Rising pitch warning sounds
                if (tick % 5 == 0) {
                    float pitch = 0.5f + (float)(gatherProgress * 1.5f);
                    DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_CHIME,
                            0.8f + (float)(gatherProgress * 0.7f), pitch);
                }

                // Converging ring getting tighter
                if (tick % 2 == 0) {
                    DisplayBuilder.particleRing(center.clone().add(0, 0.2, 0), gatherRadius,
                            Particle.DUST, (int)(gatherRadius * 8),
                            new Particle.DustOptions(Color.fromRGB(FROST_R, FROST_G, FROST_B),
                                    1.0f + (float)(gatherProgress)));
                }

            } else if (tick == BUILDUP_TICKS) {
                // ---- FLASH FREEZE BURST ----

                // Massive frost explosion visuals
                world.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 1, 0),
                        200, burstRadius, 3.0, burstRadius, 0.1);
                DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 150, burstRadius,
                        FROST_R, FROST_G, FROST_B, 2.5f);
                DisplayBuilder.dustParticles(center.clone().add(0, 1.5, 0), 100, burstRadius,
                        ICE_R, ICE_G, ICE_B, 2.0f);

                // Expanding rings at multiple heights
                for (double y = 0; y <= 3.0; y += 0.5) {
                    DisplayBuilder.particleRing(center.clone().add(0, y, 0), burstRadius,
                            Particle.SNOWFLAKE, 40, null);
                }

                // END_ROD sparkle burst
                world.spawnParticle(Particle.END_ROD, center.clone().add(0, 1.5, 0),
                        60, burstRadius * 0.5, 2.0, burstRadius * 0.5, 0.08);

                // Massive boom sounds
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 1.5f);

                // DEAL BURST DAMAGE to all players in radius
                for (Player player : world.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= burstRadius * burstRadius) {
                        player.damage(config.getDamage() * 2.0); // Double damage for the burst
                        player.setNoDamageTicks(0);
                    }
                }

            } else {
                // ---- AFTERMATH: lingering frost settling ----
                int afterTick = tick - BUILDUP_TICKS;
                double fadeProgress = (double) afterTick / (config.getDurationTicks() - BUILDUP_TICKS);

                // Settling snowflakes
                int settleCount = (int) (30 * (1.0 - fadeProgress));
                for (int i = 0; i < settleCount; i++) {
                    double angle = ThreadLocalRandom.current().nextDouble(2.0 * Math.PI);
                    double dist = ThreadLocalRandom.current().nextDouble(burstRadius);
                    Location settle = center.clone().add(
                            Math.cos(angle) * dist,
                            ThreadLocalRandom.current().nextDouble(2.0) * (1.0 - fadeProgress),
                            Math.sin(angle) * dist);
                    world.spawnParticle(Particle.SNOWFLAKE, settle, 1, 0.1, 0.2, 0.1, 0.005);
                }

                // Fading frost dust
                if (afterTick % 3 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0),
                            (int)(15 * (1.0 - fadeProgress)), burstRadius * 0.8,
                            ICE_R, ICE_G, ICE_B, 1.5f * (float)(1.0 - fadeProgress));
                }

                // Ice crackle settling
                if (afterTick % 10 == 0 && fadeProgress < 0.8) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.3f, 1.8f);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new FlashFreeze(plugin); }
    }
}
