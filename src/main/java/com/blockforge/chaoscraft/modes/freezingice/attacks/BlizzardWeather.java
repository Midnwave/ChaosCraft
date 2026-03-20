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

public final class BlizzardWeather {
    private BlizzardWeather() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new Blizzard(plugin));
        registry.register(new IceStorm(plugin));
        registry.register(new FrostWind(plugin));
        registry.register(new WhiteoutBurst(plugin));
        registry.register(new SleetRain(plugin));
        registry.register(new ArcticGale(plugin));
        registry.register(new SnowSquall(plugin));
        registry.register(new FrostMist(plugin));
        registry.register(new CrystalShower(plugin));
        registry.register(new PolarVortex(plugin));
        registry.register(new FrostCyclone(plugin));
        registry.register(new IceNeedles(plugin));
        registry.register(new SubzeroBlast(plugin));
    }

    // =========================================================================
    // 1. Blizzard — Dense snowflake rain in 10-block radius, damage every 10 ticks
    // =========================================================================
    public static class Blizzard extends EnvironmentalAttack {
        private Location center;

        public Blizzard(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blizzard_env", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
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
            DisplayBuilder.playSound(center, Sound.WEATHER_RAIN_ABOVE, 2.0f, 0.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_SNOW_GOLEM_AMBIENT, 2.0f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World world = c.getWorld();
            ThreadLocalRandom rng = ThreadLocalRandom.current();

            // Dense snowflake rain across 10-block radius
            for (int i = 0; i < 80; i++) {
                double x = c.getX() + rng.nextDouble(-10, 10);
                double z = c.getZ() + rng.nextDouble(-10, 10);
                double y = c.getY() + rng.nextDouble(5, 12);
                world.spawnParticle(Particle.SNOWFLAKE, x, y, z, 1, 0.2, -0.5, 0.2, 0.02);
            }

            // Icy blue dust mixed in
            for (int i = 0; i < 20; i++) {
                double x = c.getX() + rng.nextDouble(-10, 10);
                double z = c.getZ() + rng.nextDouble(-10, 10);
                double y = c.getY() + rng.nextDouble(0, 8);
                DisplayBuilder.dustParticles(new Location(world, x, y, z), 1, 0.3, 100, 180, 255, 1.5f);
            }

            // Ambient wind sound every 40 ticks
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.WEATHER_RAIN_ABOVE, 1.5f, 0.4f);
            }
        }

        @Override public AbstractAttack newInstance() { return new Blizzard(plugin); }
    }

    // =========================================================================
    // 2. IceStorm — Random impact damage zones (3-block radius) every 15 ticks
    // =========================================================================
    public static class IceStorm extends EnvironmentalAttack {
        private Location center;

        public IceStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_storm_env", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World world = center.getWorld();
            if (world == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.8f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World world = c.getWorld();
            ThreadLocalRandom rng = ThreadLocalRandom.current();

            // Background snow particles
            for (int i = 0; i < 30; i++) {
                double x = c.getX() + rng.nextDouble(-8, 8);
                double z = c.getZ() + rng.nextDouble(-8, 8);
                double y = c.getY() + rng.nextDouble(3, 10);
                world.spawnParticle(Particle.SNOWFLAKE, x, y, z, 1, 0.1, -0.3, 0.1, 0.01);
            }

            // Every 15 ticks, spawn a random impact zone
            if (tick % 15 == 0) {
                double offX = rng.nextDouble(-8, 8);
                double offZ = rng.nextDouble(-8, 8);
                Location impact = c.clone().add(offX, 0, offZ);

                // Impact burst — dense snowflakes and dust in 3-block radius
                world.spawnParticle(Particle.SNOWFLAKE, impact, 40, 1.5, 0.5, 1.5, 0.05);
                DisplayBuilder.dustParticles(impact, 25, 1.5, 220, 240, 255, 2.0f);
                world.spawnParticle(Particle.CLOUD, impact, 10, 1.0, 0.3, 1.0, 0.02);

                // Impact sound
                DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_BREAK, 1.5f, 1.5f);

                // Damage players in impact zone
                for (Player player : world.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(impact) <= 9.0) { // 3-block radius
                        player.damage(config.getDamage());
                        player.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new IceStorm(plugin); }
    }

    // =========================================================================
    // 3. FrostWind — Directional white dust blowing sideways, damage in wind path
    // =========================================================================
    public static class FrostWind extends EnvironmentalAttack {
        private Location center;
        private double windAngle; // radians — random direction chosen on spawn
        private double windDirX;
        private double windDirZ;

        public FrostWind(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_wind_env", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            // Random wind direction
            this.windAngle = ThreadLocalRandom.current().nextDouble(0, 2 * Math.PI);
            this.windDirX = Math.cos(windAngle);
            this.windDirZ = Math.sin(windAngle);

            World world = center.getWorld();
            if (world == null) return;
            DisplayBuilder.playSound(center, Sound.WEATHER_RAIN_ABOVE, 2.0f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World world = c.getWorld();
            ThreadLocalRandom rng = ThreadLocalRandom.current();

            // Perpendicular direction for width spread
            double perpX = -windDirZ;
            double perpZ = windDirX;

            // Spawn particles along the wind path — emitters behind center blowing forward
            for (int i = 0; i < 60; i++) {
                // Start position: behind the center relative to wind direction, spread perpendicular
                double along = rng.nextDouble(-8, 8);
                double across = rng.nextDouble(-6, 6);
                double height = rng.nextDouble(0, 4);

                double x = c.getX() + windDirX * along + perpX * across;
                double z = c.getZ() + windDirZ * along + perpZ * across;
                double y = c.getY() + height;

                // Dust particles blowing in wind direction
                DisplayBuilder.dustParticles(new Location(world, x, y, z), 1, 0.1,
                        220, 240, 255, 1.3f);
            }

            // Snowflakes driven by wind
            for (int i = 0; i < 25; i++) {
                double along = rng.nextDouble(-8, 8);
                double across = rng.nextDouble(-6, 6);
                double height = rng.nextDouble(0, 5);

                double x = c.getX() + windDirX * along + perpX * across;
                double z = c.getZ() + windDirZ * along + perpZ * across;
                double y = c.getY() + height;

                world.spawnParticle(Particle.SNOWFLAKE, x, y, z, 1,
                        windDirX * 0.5, -0.1, windDirZ * 0.5, 0.08);
            }

            // Wind howl every 30 ticks
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_FLAP, 1.5f, 0.3f);
            }
        }

        @Override public AbstractAttack newInstance() { return new FrostWind(plugin); }
    }

    // =========================================================================
    // 4. WhiteoutBurst — Extreme density white particles for 100 ticks, high damage/5 ticks
    // =========================================================================
    public static class WhiteoutBurst extends EnvironmentalAttack {
        private Location center;

        public WhiteoutBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("whiteout_burst", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World world = center.getWorld();
            if (world == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 1.8f);
            DisplayBuilder.playSound(center, Sound.WEATHER_RAIN_ABOVE, 2.0f, 0.2f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World world = c.getWorld();
            ThreadLocalRandom rng = ThreadLocalRandom.current();

            // Extreme density white particles — 150 per tick for maximum whiteout
            for (int i = 0; i < 150; i++) {
                double x = c.getX() + rng.nextDouble(-8, 8);
                double z = c.getZ() + rng.nextDouble(-8, 8);
                double y = c.getY() + rng.nextDouble(0, 8);
                DisplayBuilder.dustParticles(new Location(world, x, y, z), 1, 0.5,
                        220, 240, 255, 2.5f);
            }

            // Dense snowflakes
            for (int i = 0; i < 80; i++) {
                double x = c.getX() + rng.nextDouble(-8, 8);
                double z = c.getZ() + rng.nextDouble(-8, 8);
                double y = c.getY() + rng.nextDouble(2, 10);
                world.spawnParticle(Particle.SNOWFLAKE, x, y, z, 1, 0.5, -0.5, 0.5, 0.05);
            }

            // Clouds for extra opacity
            for (int i = 0; i < 30; i++) {
                double x = c.getX() + rng.nextDouble(-8, 8);
                double z = c.getZ() + rng.nextDouble(-8, 8);
                double y = c.getY() + rng.nextDouble(0, 5);
                world.spawnParticle(Particle.CLOUD, x, y, z, 1, 0.3, 0.1, 0.3, 0.01);
            }

            // Constant storm sound
            if (tick % 10 == 0) {
                DisplayBuilder.playSound(c, Sound.WEATHER_RAIN_ABOVE, 2.0f, 0.2f);
            }
        }

        @Override public AbstractAttack newInstance() { return new WhiteoutBurst(plugin); }
    }

    // =========================================================================
    // 5. SleetRain — Mixed RAIN + SNOWFLAKE particles, damage every 20 ticks, knockback
    // =========================================================================
    public static class SleetRain extends EnvironmentalAttack {
        private Location center;

        public SleetRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sleet_rain", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
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
            DisplayBuilder.playSound(center, Sound.WEATHER_RAIN_ABOVE, 2.0f, 0.8f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World world = c.getWorld();
            ThreadLocalRandom rng = ThreadLocalRandom.current();

            // Mixed rain + snow particles
            for (int i = 0; i < 50; i++) {
                double x = c.getX() + rng.nextDouble(-8, 8);
                double z = c.getZ() + rng.nextDouble(-8, 8);
                double y = c.getY() + rng.nextDouble(5, 12);

                // Alternate between snowflake and blue dust (rain-like)
                if (i % 2 == 0) {
                    world.spawnParticle(Particle.SNOWFLAKE, x, y, z, 1, 0.1, -0.6, 0.1, 0.02);
                } else {
                    DisplayBuilder.dustParticles(new Location(world, x, y, z), 1, 0.2,
                            100, 180, 255, 1.0f);
                }
            }

            // Cloud layer at top
            for (int i = 0; i < 15; i++) {
                double x = c.getX() + rng.nextDouble(-8, 8);
                double z = c.getZ() + rng.nextDouble(-8, 8);
                double y = c.getY() + rng.nextDouble(8, 12);
                world.spawnParticle(Particle.CLOUD, x, y, z, 1, 0.5, 0.1, 0.5, 0.005);
            }

            // Apply knockback on damage ticks
            if (tick % 20 == 0) {
                double radius = config.getDamageRadius();
                for (Player player : world.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(c) <= radius * radius) {
                        // Random knockback direction simulating sleet impact
                        double kbX = rng.nextDouble(-0.3, 0.3);
                        double kbZ = rng.nextDouble(-0.3, 0.3);
                        player.setVelocity(player.getVelocity().add(new Vector(kbX, 0.15, kbZ)));
                    }
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_STEP, 1.5f, 0.5f);
            }

            // Rain ambience
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.WEATHER_RAIN_ABOVE, 1.5f, 0.8f);
            }
        }

        @Override public AbstractAttack newInstance() { return new SleetRain(plugin); }
    }

    // =========================================================================
    // 6. ArcticGale — Strong directional wind, continuous damage, pushes players
    // =========================================================================
    public static class ArcticGale extends EnvironmentalAttack {
        private Location center;
        private double windAngle;
        private double windDirX;
        private double windDirZ;

        public ArcticGale(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("arctic_gale", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.windAngle = ThreadLocalRandom.current().nextDouble(0, 2 * Math.PI);
            this.windDirX = Math.cos(windAngle);
            this.windDirZ = Math.sin(windAngle);

            World world = center.getWorld();
            if (world == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_FLAP, 2.0f, 0.2f);
            DisplayBuilder.playSound(center, Sound.WEATHER_RAIN_ABOVE, 2.0f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World world = c.getWorld();
            ThreadLocalRandom rng = ThreadLocalRandom.current();

            double perpX = -windDirZ;
            double perpZ = windDirX;

            // Dense directional wind particles
            for (int i = 0; i < 70; i++) {
                double along = rng.nextDouble(-10, 2);
                double across = rng.nextDouble(-6, 6);
                double height = rng.nextDouble(0, 3);

                double x = c.getX() + windDirX * along + perpX * across;
                double z = c.getZ() + windDirZ * along + perpZ * across;
                double y = c.getY() + height;

                // Strong wind streaks as dust
                DisplayBuilder.dustParticles(new Location(world, x, y, z), 1, 0.05,
                        220, 240, 255, 1.8f);
            }

            // Snowflakes driven hard sideways
            for (int i = 0; i < 40; i++) {
                double along = rng.nextDouble(-10, 2);
                double across = rng.nextDouble(-6, 6);
                double height = rng.nextDouble(0, 4);

                double x = c.getX() + windDirX * along + perpX * across;
                double z = c.getZ() + windDirZ * along + perpZ * across;
                double y = c.getY() + height;

                world.spawnParticle(Particle.SNOWFLAKE, x, y, z, 1,
                        windDirX * 0.8, 0, windDirZ * 0.8, 0.12);
            }

            // Push players in wind direction every tick
            double radius = config.getDamageRadius();
            for (Player player : world.getPlayers()) {
                if (isExempt(player)) continue;
                if (player.getLocation().distanceSquared(c) <= radius * radius) {
                    Vector push = new Vector(windDirX * 0.08, 0, windDirZ * 0.08);
                    player.setVelocity(player.getVelocity().add(push));
                }
            }

            // Wind howl sounds
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_FLAP, 2.0f, 0.2f);
            }
            if (tick % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.WEATHER_RAIN_ABOVE, 1.5f, 0.3f);
            }
        }

        @Override public AbstractAttack newInstance() { return new ArcticGale(plugin); }
    }

    // =========================================================================
    // 7. SnowSquall — 60 tick burst, 40 tick clear, repeat
    // =========================================================================
    public static class SnowSquall extends EnvironmentalAttack {
        private Location center;

        public SnowSquall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("snow_squall", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(8);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World world = center.getWorld();
            if (world == null) return;
            DisplayBuilder.playSound(center, Sound.WEATHER_RAIN_ABOVE, 2.0f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World world = c.getWorld();
            ThreadLocalRandom rng = ThreadLocalRandom.current();

            // 100-tick cycle: 60 active, 40 clear
            int cyclePos = tick % 100;
            boolean squallActive = cyclePos < 60;

            if (squallActive) {
                // Heavy burst of snow
                for (int i = 0; i < 100; i++) {
                    double x = c.getX() + rng.nextDouble(-8, 8);
                    double z = c.getZ() + rng.nextDouble(-8, 8);
                    double y = c.getY() + rng.nextDouble(3, 12);
                    world.spawnParticle(Particle.SNOWFLAKE, x, y, z, 1, 0.3, -0.6, 0.3, 0.03);
                }

                // Dense white dust
                for (int i = 0; i < 40; i++) {
                    double x = c.getX() + rng.nextDouble(-8, 8);
                    double z = c.getZ() + rng.nextDouble(-8, 8);
                    double y = c.getY() + rng.nextDouble(0, 6);
                    DisplayBuilder.dustParticles(new Location(world, x, y, z), 1, 0.3,
                            220, 240, 255, 1.8f);
                }

                // Damage during squall — handled by base class via config ticks
                // Sound ramp-up at squall start
                if (cyclePos == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_SNOW_GOLEM_AMBIENT, 2.0f, 0.3f);
                    DisplayBuilder.playSound(c, Sound.WEATHER_RAIN_ABOVE, 2.0f, 0.4f);
                }

                // Continuous rumble
                if (cyclePos % 20 == 0) {
                    DisplayBuilder.playSound(c, Sound.WEATHER_RAIN_ABOVE, 1.5f, 0.5f);
                }
            } else {
                // Clear phase — minimal lingering snow
                for (int i = 0; i < 8; i++) {
                    double x = c.getX() + rng.nextDouble(-8, 8);
                    double z = c.getZ() + rng.nextDouble(-8, 8);
                    double y = c.getY() + rng.nextDouble(2, 6);
                    world.spawnParticle(Particle.SNOWFLAKE, x, y, z, 1, 0.1, -0.2, 0.1, 0.01);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new SnowSquall(plugin); }
    }

    // =========================================================================
    // 8. FrostMist — Ground-level (y+0 to y+1) white dust fog, damage while standing
    // =========================================================================
    public static class FrostMist extends EnvironmentalAttack {
        private Location center;

        public FrostMist(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_mist", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
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
            DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 2.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_SNOW_GOLEM_AMBIENT, 1.5f, 0.2f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World world = c.getWorld();
            ThreadLocalRandom rng = ThreadLocalRandom.current();

            // Ground-level fog — dense white dust from y+0 to y+1
            for (int i = 0; i < 80; i++) {
                double x = c.getX() + rng.nextDouble(-10, 10);
                double z = c.getZ() + rng.nextDouble(-10, 10);
                double y = c.getY() + rng.nextDouble(0, 1.0);

                DisplayBuilder.dustParticles(new Location(world, x, y, z), 1, 0.2,
                        220, 240, 255, 2.0f);
            }

            // Subtle cloud wisps at ground level
            for (int i = 0; i < 20; i++) {
                double x = c.getX() + rng.nextDouble(-10, 10);
                double z = c.getZ() + rng.nextDouble(-10, 10);
                double y = c.getY() + rng.nextDouble(0, 0.8);
                world.spawnParticle(Particle.CLOUD, x, y, z, 1, 0.3, 0.05, 0.3, 0.003);
            }

            // A few snowflakes rising from the mist
            if (tick % 3 == 0) {
                for (int i = 0; i < 10; i++) {
                    double x = c.getX() + rng.nextDouble(-10, 10);
                    double z = c.getZ() + rng.nextDouble(-10, 10);
                    double y = c.getY() + rng.nextDouble(0, 0.5);
                    world.spawnParticle(Particle.SNOWFLAKE, x, y, z, 1, 0.1, 0.15, 0.1, 0.01);
                }
            }

            // Eerie mist sound
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_STEP, 1.5f, 0.3f);
            }
        }

        @Override public AbstractAttack newInstance() { return new FrostMist(plugin); }
    }

    // =========================================================================
    // 9. CrystalShower — Prismarine-colored dust (RGB 0,220,220) falling like rain
    // =========================================================================
    public static class CrystalShower extends EnvironmentalAttack {
        private Location center;

        public CrystalShower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_shower", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World world = center.getWorld();
            if (world == null) return;
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 2.0f, 1.5f);
            DisplayBuilder.playSound(center, Sound.WEATHER_RAIN_ABOVE, 1.5f, 1.2f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World world = c.getWorld();
            ThreadLocalRandom rng = ThreadLocalRandom.current();

            // Prismarine-colored crystal rain
            for (int i = 0; i < 60; i++) {
                double x = c.getX() + rng.nextDouble(-8, 8);
                double z = c.getZ() + rng.nextDouble(-8, 8);
                double y = c.getY() + rng.nextDouble(4, 12);

                // Prismarine cyan-teal dust falling
                DisplayBuilder.dustParticles(new Location(world, x, y, z), 1, 0.1,
                        0, 220, 220, 1.5f);
            }

            // End rod sparkles for crystal shimmer
            for (int i = 0; i < 20; i++) {
                double x = c.getX() + rng.nextDouble(-8, 8);
                double z = c.getZ() + rng.nextDouble(-8, 8);
                double y = c.getY() + rng.nextDouble(2, 10);
                world.spawnParticle(Particle.END_ROD, x, y, z, 1, 0.1, -0.3, 0.1, 0.02);
            }

            // Snowflake accents
            for (int i = 0; i < 15; i++) {
                double x = c.getX() + rng.nextDouble(-8, 8);
                double z = c.getZ() + rng.nextDouble(-8, 8);
                double y = c.getY() + rng.nextDouble(3, 10);
                world.spawnParticle(Particle.SNOWFLAKE, x, y, z, 1, 0.1, -0.4, 0.1, 0.02);
            }

            // Crystal chime sounds
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, rng.nextFloat(1.0f, 2.0f));
            }
        }

        @Override public AbstractAttack newInstance() { return new CrystalShower(plugin); }
    }

    // =========================================================================
    // 10. PolarVortex — Spiral particles pulling inward, damage increases near center
    // =========================================================================
    public static class PolarVortex extends EnvironmentalAttack {
        private Location center;

        public PolarVortex(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("polar_vortex", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            // Base class handles outer damage; we override with distance-based in onTick
            config.setDamage(0.0); // Disable base damage — we handle it manually
            config.setDamageRadius(10.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World world = center.getWorld();
            if (world == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 2.0f);
            DisplayBuilder.playSound(center, Sound.WEATHER_RAIN_ABOVE, 2.0f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World world = c.getWorld();
            double maxRadius = 10.0;

            // Spiral particles pulling inward — multiple rings at different heights
            for (int ring = 0; ring < 5; ring++) {
                double ringRadius = maxRadius - (ring * 1.5);
                if (ringRadius < 1) ringRadius = 1;
                double rotationOffset = (tick * 0.15) + (ring * Math.PI * 0.4);
                double yOffset = ring * 0.6;

                int pointCount = (int) (ringRadius * 4);
                for (int i = 0; i < pointCount; i++) {
                    double angle = (2 * Math.PI * i / pointCount) + rotationOffset;
                    double x = c.getX() + Math.cos(angle) * ringRadius;
                    double z = c.getZ() + Math.sin(angle) * ringRadius;
                    double y = c.getY() + yOffset;

                    // Inward motion vectors
                    double inX = (c.getX() - x) * 0.05;
                    double inZ = (c.getZ() - z) * 0.05;

                    world.spawnParticle(Particle.SNOWFLAKE, x, y, z, 1, inX, 0.05, inZ, 0.02);
                }

                // Dust ring
                DisplayBuilder.particleRing(c.clone().add(0, yOffset, 0), ringRadius,
                        Particle.DUST, pointCount,
                        new Particle.DustOptions(Color.fromRGB(100, 180, 255), 1.3f));
            }

            // Central updraft
            for (int i = 0; i < 10; i++) {
                double y = c.getY() + (i * 0.3);
                DisplayBuilder.dustParticles(c.clone().add(0, y - c.getY(), 0), 2, 0.3,
                        220, 240, 255, 2.0f);
            }

            // Distance-based damage — closer to center = more damage, every 10 ticks
            if (tick % 10 == 0) {
                for (Player player : world.getPlayers()) {
                    if (isExempt(player)) continue;
                    double dist = player.getLocation().distance(c);
                    if (dist <= maxRadius) {
                        // Damage scales: 30 at center, less further out
                        double damageMult = 1.0 - (dist / maxRadius);
                        double damage = 10.0 + (20.0 * damageMult); // 10-30 damage range
                        player.damage(damage);
                        player.setNoDamageTicks(0);
                    }
                }
            }

            // Vortex sound
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_FLAP, 2.0f, 0.3f);
            }
        }

        @Override public AbstractAttack newInstance() { return new PolarVortex(plugin); }
    }

    // =========================================================================
    // 11. FrostCyclone — Rotating cylinder of snow (radius 3) that moves randomly
    // =========================================================================
    public static class FrostCyclone extends EnvironmentalAttack {
        private Location center;
        private Location cyclonePos;
        private double moveAngle;

        public FrostCyclone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_cyclone", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            // Damage handled manually based on cyclone position
            config.setDamage(0.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.cyclonePos = center.clone();
            this.moveAngle = ThreadLocalRandom.current().nextDouble(0, 2 * Math.PI);

            World world = center.getWorld();
            if (world == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_FLAP, 2.0f, 0.2f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World world = c.getWorld();
            ThreadLocalRandom rng = ThreadLocalRandom.current();

            // Randomly drift the cyclone direction
            moveAngle += rng.nextDouble(-0.3, 0.3);
            double speed = 0.25;
            double newX = cyclonePos.getX() + Math.cos(moveAngle) * speed;
            double newZ = cyclonePos.getZ() + Math.sin(moveAngle) * speed;

            // Keep cyclone within 8 blocks of center
            double distFromCenter = Math.sqrt(
                    Math.pow(newX - c.getX(), 2) + Math.pow(newZ - c.getZ(), 2));
            if (distFromCenter > 8.0) {
                // Redirect toward center
                moveAngle = Math.atan2(c.getZ() - cyclonePos.getZ(), c.getX() - cyclonePos.getX());
                newX = cyclonePos.getX() + Math.cos(moveAngle) * speed;
                newZ = cyclonePos.getZ() + Math.sin(moveAngle) * speed;
            }

            cyclonePos.setX(newX);
            cyclonePos.setZ(newZ);
            cyclonePos.setY(c.getY());

            // Rotating cylinder of snow particles — radius 3, height 6
            double cylinderRadius = 3.0;
            int cylinderHeight = 6;
            double rotation = tick * 0.25;

            for (int h = 0; h < cylinderHeight; h++) {
                double y = cyclonePos.getY() + h;
                int points = 16;
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i / points) + rotation + (h * 0.3);
                    double x = cyclonePos.getX() + Math.cos(angle) * cylinderRadius;
                    double z = cyclonePos.getZ() + Math.sin(angle) * cylinderRadius;

                    world.spawnParticle(Particle.SNOWFLAKE, x, y, z, 1, 0.05, 0.1, 0.05, 0.01);
                }
            }

            // Inner dust column
            for (int i = 0; i < 15; i++) {
                double y = cyclonePos.getY() + rng.nextDouble(0, 6);
                double angle = rng.nextDouble(0, 2 * Math.PI);
                double r = rng.nextDouble(0, 1.5);
                double x = cyclonePos.getX() + Math.cos(angle) * r;
                double z = cyclonePos.getZ() + Math.sin(angle) * r;
                DisplayBuilder.dustParticles(new Location(world, x, y, z), 1, 0.1,
                        220, 240, 255, 1.5f);
            }

            // Cloud base
            for (int i = 0; i < 8; i++) {
                double angle = rng.nextDouble(0, 2 * Math.PI);
                double r = rng.nextDouble(0, 2);
                double x = cyclonePos.getX() + Math.cos(angle) * r;
                double z = cyclonePos.getZ() + Math.sin(angle) * r;
                world.spawnParticle(Particle.CLOUD, x, cyclonePos.getY(), z, 1, 0.2, 0.1, 0.2, 0.01);
            }

            // Contact damage — players within cyclone radius
            if (tick % 5 == 0) {
                for (Player player : world.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(cyclonePos) <= cylinderRadius * cylinderRadius) {
                        player.damage(30.0);
                        player.setNoDamageTicks(0);
                        // Cyclone push — upward + outward
                        Vector outward = player.getLocation().toVector().subtract(cyclonePos.toVector()).normalize();
                        player.setVelocity(player.getVelocity().add(outward.multiply(0.3).setY(0.2)));
                    }
                }
            }

            // Wind sound
            if (tick % 15 == 0) {
                DisplayBuilder.playSound(cyclonePos, Sound.ENTITY_PHANTOM_FLAP, 2.0f, 0.3f);
            }
        }

        @Override public AbstractAttack newInstance() { return new FrostCyclone(plugin); }
    }

    // =========================================================================
    // 12. IceNeedles — END_ROD particles raining at 45-degree angles, damage every 10 ticks
    // =========================================================================
    public static class IceNeedles extends EnvironmentalAttack {
        private Location center;

        public IceNeedles(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_needles", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World world = center.getWorld();
            if (world == null) return;
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 2.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_SNOW_GOLEM_AMBIENT, 1.5f, 1.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World world = c.getWorld();
            ThreadLocalRandom rng = ThreadLocalRandom.current();

            // END_ROD needles raining at 45-degree angles from various directions
            for (int i = 0; i < 50; i++) {
                // Random position in area, spawning high
                double x = c.getX() + rng.nextDouble(-8, 8);
                double z = c.getZ() + rng.nextDouble(-8, 8);
                double y = c.getY() + rng.nextDouble(6, 12);

                // 45-degree angle: equal horizontal and vertical velocity
                // Random horizontal direction
                double hAngle = rng.nextDouble(0, 2 * Math.PI);
                double hSpeed = 0.15;
                double vSpeed = -0.15; // falling at 45 degrees

                world.spawnParticle(Particle.END_ROD, x, y, z, 1,
                        Math.cos(hAngle) * hSpeed, vSpeed, Math.sin(hAngle) * hSpeed, 0.05);
            }

            // Ice-blue dust streaks at 45 degrees
            for (int i = 0; i < 25; i++) {
                double x = c.getX() + rng.nextDouble(-8, 8);
                double z = c.getZ() + rng.nextDouble(-8, 8);
                double y = c.getY() + rng.nextDouble(3, 10);

                DisplayBuilder.dustParticles(new Location(world, x, y, z), 1, 0.1,
                        100, 180, 255, 0.8f);
            }

            // Occasional shatter sound for needle impacts
            if (tick % 10 == 0) {
                Location impactLoc = c.clone().add(rng.nextDouble(-6, 6), 0, rng.nextDouble(-6, 6));
                DisplayBuilder.playSound(impactLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.8f);
            }

            // Ground-level impact sparkles
            for (int i = 0; i < 10; i++) {
                double x = c.getX() + rng.nextDouble(-8, 8);
                double z = c.getZ() + rng.nextDouble(-8, 8);
                world.spawnParticle(Particle.END_ROD, x, c.getY() + 0.1, z, 1, 0.05, 0.1, 0.05, 0.01);
            }
        }

        @Override public AbstractAttack newInstance() { return new IceNeedles(plugin); }
    }

    // =========================================================================
    // 13. SubzeroBlast — Expanding sphere of frost from center over 60 ticks
    // =========================================================================
    public static class SubzeroBlast extends EnvironmentalAttack {
        private Location center;
        private static final double MAX_RADIUS = 10.0;
        private static final int EXPAND_TICKS = 60;

        public SubzeroBlast(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("subzero_blast", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            // Damage on wavefront only — handled manually
            config.setDamage(0.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World world = center.getWorld();
            if (world == null) return;
            // Initial blast sound
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 2.0f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.5f);

            // Central burst
            world.spawnParticle(Particle.SNOWFLAKE, center, 60, 0.5, 0.5, 0.5, 0.1);
            DisplayBuilder.dustParticles(center, 40, 0.5, 100, 180, 255, 2.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World world = c.getWorld();

            if (tick <= EXPAND_TICKS) {
                // Expanding phase — sphere grows outward
                double progress = (double) tick / EXPAND_TICKS;
                double currentRadius = MAX_RADIUS * progress;
                double prevRadius = MAX_RADIUS * Math.max(0, (tick - 1.0) / EXPAND_TICKS);

                // Wavefront sphere shell — particles on the surface of the expanding sphere
                int particleCount = (int) (currentRadius * 20);
                double goldenAngle = Math.PI * (3 - Math.sqrt(5));

                for (int i = 0; i < particleCount; i++) {
                    double y = 1 - (2.0 * i / Math.max(1, particleCount - 1));
                    double radiusAtY = Math.sqrt(1 - y * y);
                    double theta = goldenAngle * i;

                    double px = c.getX() + Math.cos(theta) * radiusAtY * currentRadius;
                    double py = c.getY() + y * currentRadius;
                    double pz = c.getZ() + Math.sin(theta) * radiusAtY * currentRadius;

                    // Only render the shell (wavefront)
                    if (i % 3 == 0) {
                        world.spawnParticle(Particle.SNOWFLAKE, px, py, pz, 1, 0.05, 0.05, 0.05, 0.01);
                    }
                    if (i % 5 == 0) {
                        DisplayBuilder.dustParticles(new Location(world, px, py, pz), 1, 0.05,
                                100, 180, 255, 1.5f);
                    }
                }

                // Horizontal ring at center height for visibility
                DisplayBuilder.particleRing(c, currentRadius, Particle.SNOWFLAKE, (int) (currentRadius * 8), null);
                DisplayBuilder.particleRing(c, currentRadius, Particle.DUST, (int) (currentRadius * 6),
                        new Particle.DustOptions(Color.fromRGB(220, 240, 255), 1.8f));

                // Wavefront damage — players on the expanding shell take damage
                if (tick % 3 == 0) {
                    double wavefrontWidth = 1.5; // 1.5 block thick wavefront
                    for (Player player : world.getPlayers()) {
                        if (isExempt(player)) continue;
                        double dist = player.getLocation().distance(c);
                        if (dist >= prevRadius - wavefrontWidth && dist <= currentRadius + wavefrontWidth) {
                            player.damage(30.0);
                            player.setNoDamageTicks(0);
                        }
                    }
                }

                // Expansion sound pulses
                if (tick % 10 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.5f, 1.5f + (float) progress);
                }
            } else {
                // Post-expansion: lingering frost particles in the full sphere
                ThreadLocalRandom rng = ThreadLocalRandom.current();
                for (int i = 0; i < 30; i++) {
                    double angle1 = rng.nextDouble(0, 2 * Math.PI);
                    double angle2 = rng.nextDouble(-Math.PI / 2, Math.PI / 2);
                    double r = rng.nextDouble(0, MAX_RADIUS);
                    double px = c.getX() + Math.cos(angle1) * Math.cos(angle2) * r;
                    double py = c.getY() + Math.sin(angle2) * r;
                    double pz = c.getZ() + Math.sin(angle1) * Math.cos(angle2) * r;

                    world.spawnParticle(Particle.SNOWFLAKE, px, py, pz, 1, 0.1, 0.1, 0.1, 0.005);
                }

                // Light lingering ice dust
                for (int i = 0; i < 10; i++) {
                    double x = c.getX() + rng.nextDouble(-MAX_RADIUS, MAX_RADIUS);
                    double z = c.getZ() + rng.nextDouble(-MAX_RADIUS, MAX_RADIUS);
                    double y = c.getY() + rng.nextDouble(-2, 2);
                    DisplayBuilder.dustParticles(new Location(world, x, y, z), 1, 0.2,
                            220, 240, 255, 1.0f);
                }

                // Ambient crackle
                if (tick % 30 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_STEP, 1.5f, 0.5f);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new SubzeroBlast(plugin); }
    }
}
