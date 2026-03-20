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
 * Freezing Ice Environmental — GROUP: THAW AND REFREEZE
 * 13 hot/cold cycle attacks — the unique mechanic is warm phases (safe)
 * that transition into freezing phases (damage). Particles + damage only.
 * ICE = RGB(100,180,255), WARM = RGB(255,160,50), STEAM = RGB(200,200,200).
 * NO potion effects. NO block displays. Radius 5-10 blocks.
 */
public final class ThawAndRefreeze {
    private ThawAndRefreeze() {}

    // Shared dust options
    private static final Particle.DustOptions ICE_DUST = new Particle.DustOptions(Color.fromRGB(100, 180, 255), 1.4f);
    private static final Particle.DustOptions WARM_DUST = new Particle.DustOptions(Color.fromRGB(255, 160, 50), 1.4f);
    private static final Particle.DustOptions STEAM_DUST = new Particle.DustOptions(Color.fromRGB(200, 200, 200), 1.2f);
    private static final Particle.DustOptions ICE_DUST_LARGE = new Particle.DustOptions(Color.fromRGB(100, 180, 255), 2.0f);
    private static final Particle.DustOptions WARM_DUST_LARGE = new Particle.DustOptions(Color.fromRGB(255, 160, 50), 2.0f);

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ThawBurst(plugin));
        registry.register(new HeatThenFreeze(plugin));
        registry.register(new MeltdownRefreeze(plugin));
        registry.register(new ThermalShock(plugin));
        registry.register(new SpringFakeout(plugin));
        registry.register(new VolcanicIce(plugin));
        registry.register(new SteamExplosion(plugin));
        registry.register(new GeyserFreeze(plugin));
        registry.register(new ThawTrap(plugin));
        registry.register(new CrackAndMelt(plugin));
        registry.register(new SunBurst(plugin));
        registry.register(new FrostFireCycle(plugin));
        registry.register(new Permathaw(plugin));
    }

    // ================================================================
    // 1. THAW BURST — Warm orange particles for 60 ticks (safe),
    //    then ice explosion damage burst at tick 60
    // ================================================================
    public static class ThawBurst extends EnvironmentalAttack {
        private Location center;

        public ThawBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("thaw_burst", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(35.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(300);
            config.setDamageOnImpactOnly(true);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (tick < 60) {
                // Warm phase — safe, orange particles pulse outward
                double spread = 3.0 + (tick / 60.0) * 4.0;
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 15, spread, 255, 160, 50, 1.4f);

                // Gentle warm ring expanding
                if (tick % 5 == 0) {
                    double ringRadius = 2.0 + (tick / 60.0) * 5.0;
                    DisplayBuilder.particleRing(c.clone().add(0, 0.5, 0), ringRadius, Particle.DUST, 20, WARM_DUST);
                }

                // Warning sounds escalating
                if (tick % 20 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 0.6f + (tick / 60.0f) * 0.4f, 1.2f);
                }
            } else if (tick == 60) {
                // ICE EXPLOSION — sudden burst
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.8f);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.5f);

                // Dense ice particle burst
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 80, 7.0, 100, 180, 255, 2.0f);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1.5, 0), 60, 6.0, 2.0, 6.0, 0.05);

                // Frost ring at burst radius
                DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), 7.0, Particle.DUST, 40, ICE_DUST_LARGE);
                DisplayBuilder.particleRing(c.clone().add(0, 1.0, 0), 5.0, Particle.DUST, 30, ICE_DUST);

                // Impact damage
                triggerImpactDamage(c);
            } else {
                // Lingering frost particles fading out
                int remaining = 100 - tick;
                DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), remaining / 4, 6.0, 100, 180, 255, 1.0f);
            }
        }

        @Override public AbstractAttack newInstance() { return new ThawBurst(plugin); }
    }

    // ================================================================
    // 2. HEAT THEN FREEZE — Warm dust 0-40, steam transition at 40,
    //    cold damage ticks 40+
    // ================================================================
    public static class HeatThenFreeze extends EnvironmentalAttack {
        private Location center;

        public HeatThenFreeze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("heat_then_freeze", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(300);
            config.setDamageDelayTicks(40);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 0.8f, 1.2f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (tick < 40) {
                // Warm phase — orange dust filling the area
                DisplayBuilder.dustParticles(c.clone().add(0, 0.8, 0), 20, 5.0, 255, 160, 50, 1.3f);
                DisplayBuilder.dustParticles(c.clone().add(0, 2.0, 0), 8, 3.0, 255, 180, 80, 1.0f);

                if (tick % 10 == 0) {
                    DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), 6.0, Particle.DUST, 24, WARM_DUST);
                }
            } else if (tick < 50) {
                // Steam transition — mix of warm, steam, and cold
                double transitionProgress = (tick - 40) / 10.0;

                DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), 25, 6.0, 200, 200, 200, 1.5f);
                c.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, c.clone().add(0, 1.0, 0), 5, 4.0, 1.0, 4.0, 0.01);

                // Transition ring — steam colored
                DisplayBuilder.particleRing(c.clone().add(0, 0.5, 0), 7.0, Particle.DUST, 28, STEAM_DUST);

                DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_EXTINGUISH, 0.8f, 1.5f);
            } else {
                // Cold phase — ice damage active, blue particles
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 25, 6.0, 100, 180, 255, 1.5f);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1.5, 0), 12, 5.0, 1.5, 5.0, 0.02);

                if (tick % 8 == 0) {
                    DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), 8.0, Particle.DUST, 32, ICE_DUST);
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.4f, 1.8f);
                }

                // Snowflake bursts
                if (tick % 15 == 0) {
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 3.0, 0), 20, 6.0, 0.5, 6.0, 0.03);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new HeatThenFreeze(plugin); }
    }

    // ================================================================
    // 3. MELTDOWN REFREEZE — Water/blue dripping particles, then
    //    flash-freeze: instant burst + frost ring
    // ================================================================
    public static class MeltdownRefreeze extends EnvironmentalAttack {
        private Location center;

        public MeltdownRefreeze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("meltdown_refreeze", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(300);
            config.setDamageOnImpactOnly(true);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            DisplayBuilder.playSound(center, Sound.WEATHER_RAIN, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (tick < 70) {
                // Meltdown phase — dripping water particles, blue dust, safe
                w.spawnParticle(Particle.DRIPPING_WATER, c.clone().add(0, 4.0, 0), 15, 5.0, 0.5, 5.0, 0);
                w.spawnParticle(Particle.FALLING_WATER, c.clone().add(0, 3.0, 0), 10, 4.0, 1.0, 4.0, 0);
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 12, 4.0, 80, 140, 220, 1.2f);

                // Puddle ring slowly growing
                if (tick % 6 == 0) {
                    double puddleRadius = 2.0 + (tick / 70.0) * 5.0;
                    DisplayBuilder.particleRing(c.clone().add(0, 0.1, 0), puddleRadius, Particle.DRIPPING_WATER, 16, null);
                    DisplayBuilder.particleRing(c.clone().add(0, 0.2, 0), puddleRadius * 0.7, Particle.DUST, 16,
                            new Particle.DustOptions(Color.fromRGB(80, 140, 220), 1.0f));
                }

                // Drip sounds
                if (tick % 15 == 0) {
                    DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 0.5f, 1.4f);
                }
            } else if (tick == 70) {
                // FLASH FREEZE — instant burst
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 2.0f);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.4f);

                // Massive frost particle burst
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 100, 8.0, 100, 180, 255, 2.2f);
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1.5, 0), 80, 7.0, 2.0, 7.0, 0.06);

                // Concentric frost rings
                for (double r = 2.0; r <= 8.0; r += 2.0) {
                    DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), r, Particle.DUST, 24, ICE_DUST_LARGE);
                }

                triggerImpactDamage(c);
            } else {
                // Lingering frost crystals
                int fade = 140 - tick;
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 0.5, 0), fade / 5, 6.0, 0.5, 6.0, 0.01);
                DisplayBuilder.dustParticles(c.clone().add(0, 0.3, 0), fade / 7, 5.0, 100, 180, 255, 1.0f);
            }
        }

        @Override public AbstractAttack newInstance() { return new MeltdownRefreeze(plugin); }
    }

    // ================================================================
    // 4. THERMAL SHOCK — Rapid oscillation: warm (10 ticks, safe),
    //    cold (10 ticks, damage), repeat
    // ================================================================
    public static class ThermalShock extends EnvironmentalAttack {
        private Location center;

        public ThermalShock(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("thermal_shock", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(25.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            int cycle = tick % 20;
            boolean warmPhase = cycle < 10;

            if (warmPhase) {
                // Warm phase — orange particles, no damage (base class handles via radius check)
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 20, 5.0, 255, 160, 50, 1.3f);
                DisplayBuilder.dustParticles(c.clone().add(0, 2.5, 0), 8, 3.0, 255, 200, 100, 1.0f);

                if (cycle == 0) {
                    DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), 7.0, Particle.DUST, 28, WARM_DUST);
                    DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 0.7f, 1.5f);
                }

                // Temporarily set damage to 0 for warm phase
                config.setDamage(0.0);
            } else {
                // Cold phase — blue particles, damage active
                config.setDamage(25.0);
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 25, 5.0, 100, 180, 255, 1.5f);
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1.5, 0), 10, 5.0, 1.0, 5.0, 0.02);

                if (cycle == 10) {
                    DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), 7.0, Particle.DUST, 28, ICE_DUST);
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.7f, 1.8f);
                }

                // Frost shards shooting outward
                if (cycle == 15) {
                    for (int i = 0; i < 8; i++) {
                        double angle = (Math.PI * 2 * i) / 8;
                        Location tip = c.clone().add(Math.cos(angle) * 6.0, 1.0, Math.sin(angle) * 6.0);
                        DisplayBuilder.particleLine(c.clone().add(0, 1.0, 0), tip, Particle.DUST, 3, ICE_DUST);
                    }
                }
            }

            // Transition flash between phases
            if (cycle == 0 || cycle == 10) {
                DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), 15, 4.0, 200, 200, 200, 1.2f);
            }
        }

        @Override public AbstractAttack newInstance() { return new ThermalShock(plugin); }
    }

    // ================================================================
    // 5. SPRING FAKEOUT — Flower-colored particles (green/pink/yellow)
    //    for 80 ticks, then sudden blizzard damage burst
    // ================================================================
    public static class SpringFakeout extends EnvironmentalAttack {
        private Location center;

        public SpringFakeout(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spring_fakeout", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(9.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(300);
            config.setDamageOnImpactOnly(true);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            // Bird chirp / pleasant sound
            DisplayBuilder.playSound(center, Sound.ENTITY_PARROT_AMBIENT, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (tick < 80) {
                // Spring phase — green, pink, yellow flower particles
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 10, 5.0, 100, 220, 80, 1.2f);   // green
                DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), 8, 4.0, 255, 130, 170, 1.2f);    // pink
                DisplayBuilder.dustParticles(c.clone().add(0, 0.8, 0), 8, 4.0, 255, 230, 50, 1.0f);     // yellow

                // Gentle flower petals floating up
                if (tick % 4 == 0) {
                    w.spawnParticle(Particle.CHERRY_LEAVES, c.clone().add(0, 0.5, 0), 5, 5.0, 0.5, 5.0, 0.01);
                }

                // Happy spring ring
                if (tick % 10 == 0) {
                    double ringR = 3.0 + (tick / 80.0) * 5.0;
                    Particle.DustOptions greenDust = new Particle.DustOptions(Color.fromRGB(100, 220, 80), 1.3f);
                    DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), ringR, Particle.DUST, 20, greenDust);
                }

                // Ambient nature sounds
                if (tick % 30 == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_PARROT_AMBIENT, 0.5f, 1.5f);
                }
            } else if (tick == 80) {
                // SUDDEN BLIZZARD — the spring was a lie
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.5f);
                DisplayBuilder.playSound(c, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 2.0f);

                // Massive ice particle explosion
                DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), 120, 9.0, 100, 180, 255, 2.5f);
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 2.0, 0), 100, 8.0, 3.0, 8.0, 0.08);

                // Multiple frost rings
                for (double r = 1.0; r <= 9.0; r += 1.5) {
                    DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), r, Particle.DUST, 30, ICE_DUST_LARGE);
                }

                triggerImpactDamage(c);
            } else {
                // Blizzard aftermath — snowflakes settling
                int fade = 140 - tick;
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 3.0, 0), fade, 8.0, 2.0, 8.0, 0.02);
                DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), fade / 3, 7.0, 100, 180, 255, 1.0f);
            }
        }

        @Override public AbstractAttack newInstance() { return new SpringFakeout(plugin); }
    }

    // ================================================================
    // 6. VOLCANIC ICE — Alternating fire ring (orange, damage) then
    //    frost ring (blue, damage) every 20 ticks
    // ================================================================
    public static class VolcanicIce extends EnvironmentalAttack {
        private Location center;

        public VolcanicIce(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("volcanic_ice", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(28.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(8);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            int cycle = tick % 40;
            boolean firePhase = cycle < 20;

            if (firePhase) {
                // Fire ring expanding outward
                double ringRadius = 2.0 + (cycle / 20.0) * 6.0;
                DisplayBuilder.particleRing(c.clone().add(0, 0.5, 0), ringRadius, Particle.DUST, 32, WARM_DUST);
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 15, ringRadius * 0.6, 255, 160, 50, 1.5f);

                // Fire-like particles
                w.spawnParticle(Particle.LAVA, c.clone().add(0, 0.3, 0), 3, ringRadius * 0.5, 0.3, ringRadius * 0.5, 0);

                // Inner glow
                if (cycle % 5 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), 10, 2.0, 255, 100, 20, 1.8f);
                }

                if (cycle == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.7f);
                }
            } else {
                // Frost ring expanding outward
                double ringRadius = 2.0 + ((cycle - 20) / 20.0) * 6.0;
                DisplayBuilder.particleRing(c.clone().add(0, 0.5, 0), ringRadius, Particle.DUST, 32, ICE_DUST);
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 15, ringRadius * 0.6, 100, 180, 255, 1.5f);

                // Snowflake particles
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1.0, 0), 8, ringRadius * 0.5, 0.5, ringRadius * 0.5, 0.01);

                if ((cycle - 20) % 5 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), 10, 2.0, 60, 130, 255, 1.8f);
                }

                if (cycle == 20) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.5f);
                }
            }

            // Transition steam between phases
            if (cycle == 0 || cycle == 20) {
                DisplayBuilder.dustParticles(c.clone().add(0, 2.0, 0), 20, 5.0, 200, 200, 200, 1.3f);
                w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, c.clone().add(0, 1.5, 0), 8, 3.0, 0.5, 3.0, 0.01);
            }
        }

        @Override public AbstractAttack newInstance() { return new VolcanicIce(plugin); }
    }

    // ================================================================
    // 7. STEAM EXPLOSION — Steam particles gathering at center for
    //    40 ticks, then explosion (large ring + heavy damage)
    // ================================================================
    public static class SteamExplosion extends EnvironmentalAttack {
        private Location center;

        public SteamExplosion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("steam_explosion", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(45.0);
            config.setDamageRadius(9.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(300);
            config.setDamageOnImpactOnly(true);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_EXTINGUISH, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (tick < 40) {
                // Steam gathering phase — particles converge toward center
                double gatherRadius = 8.0 - (tick / 40.0) * 6.0;
                int density = 10 + (int)(tick / 40.0 * 30);

                DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), density, gatherRadius, 200, 200, 200, 1.3f);
                w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, c.clone().add(0, 1.0, 0), 5 + tick / 8, gatherRadius, 1.0, gatherRadius, 0.005);

                // Convergence ring shrinking
                if (tick % 4 == 0) {
                    DisplayBuilder.particleRing(c.clone().add(0, 0.5, 0), gatherRadius, Particle.DUST, 24, STEAM_DUST);
                }

                // Building pressure sounds
                if (tick % 10 == 0) {
                    float pitch = 0.5f + (tick / 40.0f) * 1.5f;
                    DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_EXTINGUISH, 0.5f + tick / 80.0f, pitch);
                }

                // Dense core at center grows
                if (tick > 20) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 15, 1.0, 220, 220, 220, 2.0f);
                }
            } else if (tick == 40) {
                // STEAM EXPLOSION
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
                DisplayBuilder.playSound(c, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 2.0f);

                // Massive expanding ring of steam and frost
                DisplayBuilder.dustParticles(c.clone().add(0, 2.0, 0), 150, 9.0, 200, 200, 200, 2.5f);
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 80, 8.0, 100, 180, 255, 2.0f);
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 2.0, 0), 60, 8.0, 3.0, 8.0, 0.08);
                w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, c.clone().add(0, 1.5, 0), 30, 6.0, 2.0, 6.0, 0.03);

                // Explosion rings at multiple heights
                for (double y = 0.3; y <= 3.0; y += 0.6) {
                    DisplayBuilder.particleRing(c.clone().add(0, y, 0), 9.0, Particle.DUST, 40, ICE_DUST_LARGE);
                }

                triggerImpactDamage(c);
            } else {
                // Dissipating steam
                int fade = 100 - tick;
                w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, c.clone().add(0, 2.0, 0), fade / 4, 8.0, 2.0, 8.0, 0.01);
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), fade / 5, 8.0, 180, 180, 180, 0.8f);
            }
        }

        @Override public AbstractAttack newInstance() { return new SteamExplosion(plugin); }
    }

    // ================================================================
    // 8. GEYSER FREEZE — Water particles erupting upward for 30 ticks,
    //    then freeze mid-air (frost replaces water, damage column)
    // ================================================================
    public static class GeyserFreeze extends EnvironmentalAttack {
        private Location center;

        public GeyserFreeze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("geyser_freeze", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(35.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(300);
            config.setDamageOnImpactOnly(true);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_SPLASH, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (tick < 30) {
                // Geyser eruption — water particles shooting upward
                double height = 2.0 + (tick / 30.0) * 8.0;
                double spread = 1.5;

                w.spawnParticle(Particle.SPLASH, c.clone().add(0, height * 0.5, 0), 20, spread, height * 0.4, spread, 0.1);
                w.spawnParticle(Particle.DRIPPING_WATER, c.clone().add(0, height, 0), 10, spread, 0.5, spread, 0);
                DisplayBuilder.dustParticles(c.clone().add(0, height * 0.3, 0), 12, spread, 80, 140, 220, 1.2f);

                // Base splash ring
                if (tick % 3 == 0) {
                    DisplayBuilder.particleRing(c.clone().add(0, 0.2, 0), 2.0 + tick * 0.05, Particle.SPLASH, 12, null);
                }

                // Escalating geyser sounds
                if (tick % 8 == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_SPLASH, 0.8f, 0.6f + tick / 60.0f);
                }
            } else if (tick == 30) {
                // FREEZE MID-AIR — water turns to ice instantly
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.3f);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 2.0f);

                // Frost column replacing the geyser
                for (double y = 0; y <= 10.0; y += 0.8) {
                    DisplayBuilder.dustParticles(c.clone().add(0, y, 0), 15, 2.0, 100, 180, 255, 2.0f);
                    w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, y, 0), 8, 1.5, 0.3, 1.5, 0.02);
                }

                // Frost shockwave ring at base
                DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), 6.0, Particle.DUST, 30, ICE_DUST_LARGE);

                triggerImpactDamage(c);
            } else {
                // Frozen column slowly shattering / falling
                int fade = 120 - tick;
                double colHeight = 10.0 * (fade / 90.0);

                for (double y = 0; y <= colHeight; y += 1.5) {
                    w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, y, 0), 3, 1.5, 0.5, 1.5, 0.01);
                }
                DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), fade / 8, 3.0, 100, 180, 255, 1.0f);

                // Ice shard sounds
                if (tick % 20 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.4f, 2.0f);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new GeyserFreeze(plugin); }
    }

    // ================================================================
    // 9. THAW TRAP — Warm safe zone ring for 60 ticks, then snaps
    //    to frost zone with heavy damage inside
    // ================================================================
    public static class ThawTrap extends EnvironmentalAttack {
        private Location center;

        public ThawTrap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("thaw_trap", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(45.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(300);
            config.setDamageOnImpactOnly(true);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            // Inviting warm sound
            DisplayBuilder.playSound(center, Sound.BLOCK_CAMPFIRE_CRACKLE, 1.0f, 1.0f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (tick < 60) {
                // Warm safe zone — inviting orange ring, lures players in
                DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), 7.0, Particle.DUST, 28, WARM_DUST);
                DisplayBuilder.particleRing(c.clone().add(0, 1.0, 0), 5.0, Particle.DUST, 20, WARM_DUST);

                // Cozy warm fill particles
                DisplayBuilder.dustParticles(c.clone().add(0, 0.8, 0), 12, 4.0, 255, 160, 50, 1.2f);
                DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), 6, 3.0, 255, 200, 100, 1.0f);

                // Campfire-like warmth
                if (tick % 6 == 0) {
                    w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, c.clone().add(0, 0.3, 0), 3, 2.0, 0.3, 2.0, 0.005);
                }

                // Soothing crackle
                if (tick % 25 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CAMPFIRE_CRACKLE, 0.7f, 1.2f);
                }

                // Warning flickers near the end (tick 50-59)
                if (tick >= 50 && tick % 3 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 5, 5.0, 100, 180, 255, 0.8f);
                }
            } else if (tick == 60) {
                // SNAP TO FROST — the trap springs
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.8f);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.3f);
                DisplayBuilder.playSound(c, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 2.0f);

                // Massive frost burst inside the warm zone
                DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), 100, 7.0, 100, 180, 255, 2.5f);
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 2.0, 0), 80, 6.0, 2.0, 6.0, 0.06);

                // Frost ring replacing warm ring
                DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), 7.0, Particle.DUST, 40, ICE_DUST_LARGE);
                DisplayBuilder.particleRing(c.clone().add(0, 1.5, 0), 5.0, Particle.DUST, 30, ICE_DUST_LARGE);

                // Inward frost lines (trap closing)
                for (int i = 0; i < 12; i++) {
                    double angle = (Math.PI * 2 * i) / 12;
                    Location edge = c.clone().add(Math.cos(angle) * 7.0, 1.0, Math.sin(angle) * 7.0);
                    DisplayBuilder.particleLine(edge, c.clone().add(0, 1.0, 0), Particle.DUST, 4, ICE_DUST);
                }

                triggerImpactDamage(c);
            } else {
                // Lingering frost zone
                int fade = 140 - tick;
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1.0, 0), fade / 4, 5.0, 1.0, 5.0, 0.01);
                DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), fade / 5, 5.0, 100, 180, 255, 1.0f);

                if (tick % 15 == 0) {
                    DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), 7.0 * (fade / 80.0), Particle.DUST, 20, ICE_DUST);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new ThawTrap(plugin); }
    }

    // ================================================================
    // 10. CRACK AND MELT — Ice crack sounds + warm particles in lines,
    //     then refreeze burst with damage ring
    // ================================================================
    public static class CrackAndMelt extends EnvironmentalAttack {
        private Location center;
        private final double[] crackAngles = new double[8];

        public CrackAndMelt(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crack_and_melt", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(35.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(300);
            config.setDamageOnImpactOnly(true);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            // Generate 8 random crack directions
            for (int i = 0; i < 8; i++) {
                crackAngles[i] = (Math.PI * 2 * i) / 8 + (Math.random() - 0.5) * 0.4;
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (tick < 60) {
                // Cracking and melting phase — warm lines radiating outward
                double lineLength = 1.0 + (tick / 60.0) * 7.0;

                for (int i = 0; i < 8; i++) {
                    double angle = crackAngles[i];
                    Location end = c.clone().add(Math.cos(angle) * lineLength, 0.2, Math.sin(angle) * lineLength);
                    DisplayBuilder.particleLine(c.clone().add(0, 0.2, 0), end, Particle.DUST, 3, WARM_DUST);
                }

                // Warm melt particles along cracks
                DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), 8, lineLength * 0.5, 255, 160, 50, 1.0f);

                // Crack sounds at intervals
                if (tick % 12 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.6f, 0.5f + (float)(Math.random() * 0.3));
                    DisplayBuilder.playSound(c, Sound.ENTITY_ITEM_BREAK, 0.4f, 0.8f);
                }

                // Dripping melt particles
                if (tick % 5 == 0) {
                    w.spawnParticle(Particle.DRIPPING_WATER, c.clone().add(0, 0.3, 0), 4, lineLength * 0.4, 0.1, lineLength * 0.4, 0);
                }

                // Steam wisps from cracks
                if (tick > 30 && tick % 8 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 0.8, 0), 6, lineLength * 0.3, 200, 200, 200, 1.0f);
                }
            } else if (tick == 60) {
                // REFREEZE BURST — everything snaps back to ice
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.5f);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.2f);

                // Dense frost burst
                DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), 100, 8.0, 100, 180, 255, 2.2f);
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1.0, 0), 70, 7.0, 2.0, 7.0, 0.05);

                // Frost lines snapping along crack paths
                for (int i = 0; i < 8; i++) {
                    double angle = crackAngles[i];
                    Location end = c.clone().add(Math.cos(angle) * 8.0, 0.5, Math.sin(angle) * 8.0);
                    DisplayBuilder.particleLine(c.clone().add(0, 0.5, 0), end, Particle.DUST, 5, ICE_DUST_LARGE);
                }

                // Damage ring
                DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), 8.0, Particle.DUST, 40, ICE_DUST_LARGE);

                triggerImpactDamage(c);
            } else {
                // Frozen aftermath
                int fade = 140 - tick;
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 0.5, 0), fade / 6, 6.0, 0.5, 6.0, 0.01);
                DisplayBuilder.dustParticles(c.clone().add(0, 0.3, 0), fade / 8, 5.0, 100, 180, 255, 0.8f);
            }
        }

        @Override public AbstractAttack newInstance() { return new CrackAndMelt(plugin); }
    }

    // ================================================================
    // 11. SUN BURST — Bright yellow/white END_ROD flash (warmth),
    //     40 tick delay, then deep frost damage wave
    // ================================================================
    public static class SunBurst extends EnvironmentalAttack {
        private Location center;

        public SunBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sun_burst", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(38.0);
            config.setDamageRadius(9.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(300);
            config.setDamageOnImpactOnly(true);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            // Initial bright flash
            center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(0, 3.0, 0), 60, 0.5, 0.5, 0.5, 0.15);
            DisplayBuilder.dustParticles(center.clone().add(0, 3.0, 0), 40, 2.0, 255, 255, 200, 2.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (tick < 20) {
                // Sun burst — bright white/yellow warmth expanding
                double burstRadius = 1.0 + tick * 0.5;
                w.spawnParticle(Particle.END_ROD, c.clone().add(0, 2.5, 0), 15 - tick / 2, burstRadius, 1.0, burstRadius, 0.05);
                DisplayBuilder.dustParticles(c.clone().add(0, 2.0, 0), 20 - tick, burstRadius, 255, 255, 180, 1.5f);

                // Warm yellow ring
                if (tick % 4 == 0) {
                    Particle.DustOptions yellowDust = new Particle.DustOptions(Color.fromRGB(255, 240, 100), 1.5f);
                    DisplayBuilder.particleRing(c.clone().add(0, 0.5, 0), burstRadius, Particle.DUST, 20, yellowDust);
                }
            } else if (tick < 40) {
                // Fading warmth — the sun retreats, ominous silence
                double fadeAmount = (tick - 20) / 20.0;
                int particleCount = (int)(8 * (1.0 - fadeAmount));
                w.spawnParticle(Particle.END_ROD, c.clone().add(0, 2.0, 0), particleCount, 3.0, 1.0, 3.0, 0.02);

                // Temperature warning — mixing warm and cold
                if (tick > 30 && tick % 3 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 5, 5.0, 100, 180, 255, 0.8f);
                }
            } else if (tick == 40) {
                // DEEP FROST WAVE — the cold rushes back
                DisplayBuilder.playSound(c, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 2.0f);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.5f);

                // Dense frost wave expanding
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 120, 9.0, 100, 180, 255, 2.5f);
                DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), 60, 8.0, 60, 120, 220, 2.0f);
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 2.0, 0), 80, 8.0, 2.0, 8.0, 0.06);

                // Frost wave rings at multiple radii
                for (double r = 2.0; r <= 9.0; r += 1.5) {
                    DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), r, Particle.DUST, 30, ICE_DUST_LARGE);
                }

                triggerImpactDamage(c);
            } else {
                // Deep frost settling
                int fade = 120 - tick;
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1.5, 0), fade / 3, 8.0, 1.5, 8.0, 0.015);
                DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), fade / 4, 7.0, 80, 150, 230, 1.0f);

                // Occasional frost crackle
                if (tick % 20 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.3f, 2.0f);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new SunBurst(plugin); }
    }

    // ================================================================
    // 12. FROST FIRE CYCLE — Fire ring expands (0-20, orange dust),
    //     frost ring expands (20-40, blue dust), repeat. Damage on both.
    // ================================================================
    public static class FrostFireCycle extends EnvironmentalAttack {
        private Location center;

        public FrostFireCycle(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_fire_cycle", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(26.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(6);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.6f, 1.0f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.6f, 1.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            int cycle = tick % 40;

            if (cycle < 20) {
                // FIRE RING expanding outward
                double ringRadius = 1.0 + (cycle / 20.0) * 7.0;
                double ringThickness = 1.5;

                // Main fire ring
                DisplayBuilder.particleRing(c.clone().add(0, 0.5, 0), ringRadius, Particle.DUST, 32, WARM_DUST);
                DisplayBuilder.particleRing(c.clone().add(0, 1.0, 0), ringRadius * 0.8, Particle.DUST, 20, WARM_DUST_LARGE);

                // Fill particles near ring edge
                DisplayBuilder.dustParticles(c.clone().add(0, 0.8, 0), 10, ringThickness, 255, 160, 50, 1.2f);

                // Fire-like ambient
                if (cycle % 4 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        Location p = c.clone().add(Math.cos(angle) * ringRadius, 0.3, Math.sin(angle) * ringRadius);
                        w.spawnParticle(Particle.LAVA, p, 1, 0.3, 0.1, 0.3, 0);
                    }
                }

                if (cycle == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_BLAZE_SHOOT, 0.7f, 0.8f);
                }
            } else {
                // FROST RING expanding outward
                double ringRadius = 1.0 + ((cycle - 20) / 20.0) * 7.0;
                double ringThickness = 1.5;

                // Main frost ring
                DisplayBuilder.particleRing(c.clone().add(0, 0.5, 0), ringRadius, Particle.DUST, 32, ICE_DUST);
                DisplayBuilder.particleRing(c.clone().add(0, 1.0, 0), ringRadius * 0.8, Particle.DUST, 20, ICE_DUST_LARGE);

                // Fill particles near ring edge
                DisplayBuilder.dustParticles(c.clone().add(0, 0.8, 0), 10, ringThickness, 100, 180, 255, 1.2f);

                // Snowflake ambient
                if ((cycle - 20) % 4 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        Location p = c.clone().add(Math.cos(angle) * ringRadius, 0.5, Math.sin(angle) * ringRadius);
                        w.spawnParticle(Particle.SNOWFLAKE, p, 3, 0.3, 0.2, 0.3, 0.01);
                    }
                }

                if (cycle == 20) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.7f, 1.6f);
                }
            }

            // Transition steam burst at phase switches
            if (cycle == 0 || cycle == 20) {
                DisplayBuilder.dustParticles(c.clone().add(0, 2.0, 0), 18, 3.0, 200, 200, 200, 1.5f);
                w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, c.clone().add(0, 1.5, 0), 6, 2.0, 0.5, 2.0, 0.01);
            }

            // Central core — always visible, alternates color
            if (tick % 3 == 0) {
                boolean fire = cycle < 20;
                int r = fire ? 255 : 100;
                int g = fire ? 160 : 180;
                int b = fire ? 50 : 255;
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 8, 1.0, r, g, b, 1.8f);
            }
        }

        @Override public AbstractAttack newInstance() { return new FrostFireCycle(plugin); }
    }

    // ================================================================
    // 13. PERMATHAW — Slow warm buildup: orange particles grow for
    //     100 ticks (no damage, players relax), then LONGEST freeze
    //     burst (100 ticks of heavy frost damage + dense snowflakes)
    // ================================================================
    public static class Permathaw extends EnvironmentalAttack {
        private Location center;

        public Permathaw(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("permathaw", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(32.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(400);
            config.setDamageDelayTicks(100);
            config.setTicksBetweenDamage(8);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            // Subtle warm start
            DisplayBuilder.playSound(center, Sound.BLOCK_CAMPFIRE_CRACKLE, 0.5f, 1.0f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (tick < 100) {
                // WARM BUILDUP PHASE — grows slowly, players feel safe
                double progress = tick / 100.0;
                double spread = 2.0 + progress * 8.0;
                int density = 5 + (int)(progress * 20);

                // Orange warm particles growing
                DisplayBuilder.dustParticles(c.clone().add(0, 0.8, 0), density, spread, 255, 160, 50, 1.0f + (float)progress * 0.5f);

                // Warm ring growing outward
                if (tick % 8 == 0) {
                    double ringR = 2.0 + progress * 8.0;
                    DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), ringR, Particle.DUST, 24, WARM_DUST);
                }

                // Gentle ambient particles
                if (tick % 6 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 2.0, 0), 4, spread * 0.6, 255, 200, 100, 0.8f);
                }

                // Cozy sounds getting slightly louder
                if (tick % 20 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CAMPFIRE_CRACKLE, 0.3f + (float)progress * 0.4f, 1.0f + (float)progress * 0.2f);
                }

                // Lava particles appearing late in buildup
                if (tick > 60 && tick % 10 == 0) {
                    w.spawnParticle(Particle.LAVA, c.clone().add(0, 0.3, 0), 2, spread * 0.3, 0.2, spread * 0.3, 0);
                }

                // Very subtle warning at tick 90-99 — tiny ice flickers
                if (tick >= 90 && tick % 2 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), 3, 3.0, 100, 180, 255, 0.6f);
                }
            } else if (tick == 100) {
                // TRANSITION — the warmth dies, cold takes over
                DisplayBuilder.playSound(c, Sound.ENTITY_WITHER_AMBIENT, 1.2f, 1.8f);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 2.0f);

                // Initial frost shockwave
                DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), 80, 10.0, 100, 180, 255, 2.5f);
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 2.0, 0), 60, 9.0, 2.0, 9.0, 0.06);

                // Frost rings
                for (double r = 2.0; r <= 10.0; r += 2.0) {
                    DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), r, Particle.DUST, 36, ICE_DUST_LARGE);
                }
            } else {
                // EXTENDED FREEZE PHASE — 100 ticks of heavy frost damage + dense snowflakes
                double freezeProgress = (tick - 100) / 140.0;
                int density = 30 + (int)(freezeProgress * 20);

                // Dense frost particles — the longest freeze
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), density, 8.0, 100, 180, 255, 1.8f);
                DisplayBuilder.dustParticles(c.clone().add(0, 2.5, 0), density / 2, 7.0, 80, 150, 240, 1.4f);

                // Dense snowflake shower
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 5.0, 0), 25 + (int)(freezeProgress * 15), 8.0, 3.0, 8.0, 0.04);

                // Frost rings pulsing
                if (tick % 10 == 0) {
                    double pulseRadius = 5.0 + Math.sin(tick * 0.15) * 3.0;
                    DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), pulseRadius, Particle.DUST, 32, ICE_DUST);
                    DisplayBuilder.particleRing(c.clone().add(0, 1.5, 0), pulseRadius * 0.7, Particle.DUST, 24, ICE_DUST);
                }

                // Frost column particles
                if (tick % 6 == 0) {
                    for (double y = 0; y <= 4.0; y += 1.0) {
                        DisplayBuilder.dustParticles(c.clone().add(0, y, 0), 5, 2.0, 100, 180, 255, 1.5f);
                    }
                }

                // Periodic frost crackle sounds
                if (tick % 15 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.6f, 1.5f + (float)(Math.random() * 0.5));
                }

                // End rod sparkles in the frost cloud
                if (tick % 12 == 0) {
                    w.spawnParticle(Particle.END_ROD, c.clone().add(0, 2.0, 0), 5, 6.0, 2.0, 6.0, 0.02);
                }

                // Final intensification in last 30 ticks
                if (tick > 210) {
                    int extra = tick - 210;
                    DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), extra * 2, 9.0, 60, 130, 255, 2.0f);
                    w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 4.0, 0), extra, 9.0, 2.0, 9.0, 0.05);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new Permathaw(plugin); }
    }
}
