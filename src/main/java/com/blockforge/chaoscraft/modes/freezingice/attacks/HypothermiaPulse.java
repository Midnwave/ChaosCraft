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
 * Freezing Ice Mode — HYPOTHERMIA PULSE ATTACKS
 * 13 environmental attacks themed around progressive cold damage escalation.
 * Pure particle + damage effects — no block displays.
 *
 * Color palette:
 * - Ice blue: RGB(100, 180, 255)
 * - Frost white: RGB(220, 240, 255)
 * - Deep ice: RGB(30, 80, 160)
 *
 * Particles: SNOWFLAKE, END_ROD, DUST with ice colors
 * Sounds: ENTITY_PLAYER_HURT_FREEZE, BLOCK_POWDER_SNOW_STEP, BLOCK_GLASS_BREAK,
 *          BLOCK_AMETHYST_BLOCK_CHIME, ITEM_ELYTRA_FLYING
 *
 * All 13 attacks implement progressive cold damage mechanics — damage that
 * escalates over time through different mechanisms (density, interval, velocity, phases).
 */
public final class HypothermiaPulse {
    private HypothermiaPulse() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new SlowFreeze(plugin));
        registry.register(new FrostAccumulation(plugin));
        registry.register(new ColdSap(plugin));
        registry.register(new HypothermiaStage1(plugin));
        registry.register(new HypothermiaStage2(plugin));
        registry.register(new HypothermiaStage3(plugin));
        registry.register(new CoreTemperatureDrop(plugin));
        registry.register(new ExtremitiesFreeze(plugin));
        registry.register(new WindChillFactor(plugin));
        registry.register(new FrostShock(plugin));
        registry.register(new ColdSweat(plugin));
        registry.register(new NumbnessSpread(plugin));
        registry.register(new FinalChill(plugin));
    }

    // ================================================================
    // Helper: apply damage to all non-exempt survival players in radius
    // ================================================================
    private static void damagePlayersInRadius(Location center, double radius, double damage,
                                               EnvironmentalAttack attack) {
        if (center.getWorld() == null) return;
        double r2 = radius * radius;
        for (Player p : center.getWorld().getPlayers()) {
            if (p.hasPermission("chaoscraft.mode.exempt")) continue;
            if (p.getLocation().distanceSquared(center) <= r2) {
                p.damage(damage);
                p.setNoDamageTicks(0);
            }
        }
    }

    // ================================================================
    // 1. SLOW FREEZE — Damage starts at 0.3x config, increases by 0.1x
    //    every 40 ticks. Frost particles grow denser over time.
    //    Radius 8. Slow escalation from gentle chill to dangerous cold.
    // ================================================================
    public static class SlowFreeze extends EnvironmentalAttack {
        private Location center;

        public SlowFreeze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("slow_freeze", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(999); // Manual damage timing
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 1.2f);
            // Initial light frost ring
            DisplayBuilder.particleRing(center, 8.0, Particle.SNOWFLAKE, 24, null);
            DisplayBuilder.dustParticles(center, 15, 3.0, 100, 180, 255, 1.0f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Escalation: damage multiplier increases by 0.1x every 40 ticks
            // tick 1-40: 0.3x, tick 41-80: 0.4x, tick 81-120: 0.5x, etc.
            int stage = tick / 40;
            double multiplier = 0.3 + (stage * 0.1);
            double damage = config.getDamage() * multiplier;

            // Particle density grows with stage
            int baseParticles = 5 + (stage * 8);
            double spread = 4.0;

            // Frost particles — grow denser each stage
            if (tick % 3 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 1, 0), baseParticles, spread,
                        100, 180, 255, 1.2f);
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1.5, 0),
                        baseParticles / 2, spread, 1.5, spread, 0.02);
            }

            // Frost ring pulses — grows brighter with stage
            if (tick % 10 == 0) {
                DisplayBuilder.particleRing(c, 8.0, Particle.DUST, 20 + (stage * 6),
                        new Particle.DustOptions(Color.fromRGB(100, 180, 255), 1.0f + (stage * 0.3f)));
            }

            // Apply damage every 20 ticks
            if (tick % 20 == 0) {
                damagePlayersInRadius(c, 8.0, damage, this);
            }

            // Ambient freeze sound at stage transitions
            if (tick % 40 == 0 && tick > 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.5f + (stage * 0.15f), 1.0f);
            }
        }

        @Override public AbstractAttack newInstance() { return new SlowFreeze(plugin); }
    }

    // ================================================================
    // 2. FROST ACCUMULATION — Snowflake density increases every 20 ticks,
    //    damage increases proportionally. Visible snowfall gets heavier.
    //    Radius 7. Feels like a blizzard intensifying.
    // ================================================================
    public static class FrostAccumulation extends EnvironmentalAttack {
        private Location center;

        public FrostAccumulation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_accumulation", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 1.0f, 0.6f);
            DisplayBuilder.dustParticles(center.clone().add(0, 3, 0), 20, 5.0, 220, 240, 255, 1.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Accumulation level increases every 20 ticks (0 -> 1 -> 2 -> ... -> 9)
            int accumLevel = tick / 20;
            double damageMult = 0.2 + (accumLevel * 0.1);
            double damage = config.getDamage() * damageMult;

            // Snowflake density scales with accumulation
            int snowflakeCount = 3 + (accumLevel * 5);
            double radius = 7.0;

            // Falling snowflakes from above — density increases
            if (tick % 2 == 0) {
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 5, 0),
                        snowflakeCount, radius, 2.0, radius, 0.01);
                // Ground-level frost mist
                DisplayBuilder.dustParticles(c.clone().add(0, 0.3, 0),
                        snowflakeCount / 2, radius, 220, 240, 255, 0.8f + (accumLevel * 0.1f));
            }

            // Deep ice dust swirls at high accumulation
            if (accumLevel >= 4 && tick % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0),
                        accumLevel * 3, 5.0, 30, 80, 160, 1.5f);
            }

            // Frost buildup ring on ground
            if (tick % 20 == 0) {
                DisplayBuilder.particleRing(c, radius, Particle.DUST, 24 + (accumLevel * 4),
                        new Particle.DustOptions(Color.fromRGB(220, 240, 255), 1.0f + (accumLevel * 0.15f)));
                DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_STEP, 0.4f + (accumLevel * 0.06f), 0.8f);
            }

            // Damage scales with accumulation — applied every 15 ticks
            if (tick % 15 == 0) {
                damagePlayersInRadius(c, radius, damage, this);
            }

            // Wind howl at higher accumulation
            if (accumLevel >= 5 && tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.ITEM_ELYTRA_FLYING, 0.5f, 1.8f);
            }
        }

        @Override public AbstractAttack newInstance() { return new FrostAccumulation(plugin); }
    }

    // ================================================================
    // 3. COLD SAP — Continuous low damage every 15 ticks. Doubles the
    //    damage rate if player velocity < 0.1 (stationary).
    //    Radius 8. Punishes standing still — keep moving or freeze.
    // ================================================================
    public static class ColdSap extends EnvironmentalAttack {
        private Location center;

        public ColdSap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cold_sap", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.6f, 1.4f);
            // Ground-level cold mist
            DisplayBuilder.dustParticles(center.clone().add(0, 0.2, 0), 30, 6.0, 100, 180, 255, 1.0f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            double radius = 8.0;
            double baseDamage = config.getDamage() * 0.25; // Low continuous damage

            // Ambient frost mist
            if (tick % 3 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 0.3, 0), 10, 6.0, 100, 180, 255, 0.9f);
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 0.5, 0),
                        6, radius * 0.8, 0.5, radius * 0.8, 0.005);
            }

            // Cold sap ring at ground level
            if (tick % 15 == 0) {
                DisplayBuilder.particleRing(c, radius, Particle.DUST, 28,
                        new Particle.DustOptions(Color.fromRGB(30, 80, 160), 1.2f));
            }

            // Damage every 15 ticks — but check player velocity
            // Stationary players (velocity < 0.1) get hit every 15 ticks with DOUBLE damage
            // Moving players get hit every 15 ticks with normal damage
            if (tick % 15 == 0) {
                double r2 = radius * radius;
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(c) > r2) continue;

                    double velocity = p.getVelocity().length();
                    double damage;
                    if (velocity < 0.1) {
                        // Stationary — double damage
                        damage = baseDamage * 2.0;
                        // Visual warning: concentrated frost at stationary player
                        DisplayBuilder.dustParticles(p.getLocation().add(0, 1, 0), 15, 0.5,
                                30, 80, 160, 1.5f);
                        w.spawnParticle(Particle.SNOWFLAKE, p.getLocation().add(0, 1.5, 0),
                                8, 0.3, 0.3, 0.3, 0.01);
                    } else {
                        damage = baseDamage;
                    }
                    p.damage(damage);
                    p.setNoDamageTicks(0);
                }
            }

            // Also check mid-interval for stationary players (effectively doubles rate)
            if (tick % 15 == 7) {
                double r2 = radius * radius;
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(c) > r2) continue;

                    double velocity = p.getVelocity().length();
                    if (velocity < 0.1) {
                        // Extra damage tick for stationary players
                        p.damage(baseDamage);
                        p.setNoDamageTicks(0);
                        // Extra frost cling particles
                        DisplayBuilder.dustParticles(p.getLocation().add(0, 0.5, 0), 8, 0.4,
                                220, 240, 255, 1.0f);
                    }
                }
            }

            // Ambient sound
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_STEP, 0.5f, 0.5f);
            }
        }

        @Override public AbstractAttack newInstance() { return new ColdSap(plugin); }
    }

    // ================================================================
    // 4. HYPOTHERMIA STAGE 1 — Light frost particles at feet level,
    //    damage every 40 ticks, frost ring at ground level.
    //    Radius 7. Gentle early-stage hypothermia.
    // ================================================================
    public static class HypothermiaStage1 extends EnvironmentalAttack {
        private Location center;

        public HypothermiaStage1(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hypothermia_stage1", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 0.7f, 1.0f);
            // Light frost ring at ground
            DisplayBuilder.particleRing(center, 7.0, Particle.DUST, 20,
                    new Particle.DustOptions(Color.fromRGB(220, 240, 255), 0.8f));
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            double radius = 7.0;

            // Light frost particles at feet level (y+0 to y+0.5)
            if (tick % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 0.2, 0), 8, radius * 0.8,
                        220, 240, 255, 0.8f);
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 0.3, 0),
                        4, radius * 0.7, 0.2, radius * 0.7, 0.005);
            }

            // Frost ring at ground pulses every 20 ticks
            if (tick % 20 == 0) {
                DisplayBuilder.particleRing(c, radius, Particle.DUST, 24,
                        new Particle.DustOptions(Color.fromRGB(220, 240, 255), 0.9f));
            }

            // Gentle damage every 40 ticks — stage 1 is mild
            if (tick % 40 == 0) {
                damagePlayersInRadius(c, radius, config.getDamage() * 0.3, this);
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.3f, 1.5f);
            }

            // End rod sparkle for frosty ambience
            if (tick % 8 == 0) {
                w.spawnParticle(Particle.END_ROD, c.clone().add(0, 0.4, 0),
                        2, radius * 0.6, 0.3, radius * 0.6, 0.005);
            }
        }

        @Override public AbstractAttack newInstance() { return new HypothermiaStage1(plugin); }
    }

    // ================================================================
    // 5. HYPOTHERMIA STAGE 2 — Heavier frost particles up to waist level,
    //    damage every 25 ticks, larger frost ring.
    //    Radius 8. Medium-stage hypothermia — noticeably more dangerous.
    // ================================================================
    public static class HypothermiaStage2 extends EnvironmentalAttack {
        private Location center;

        public HypothermiaStage2(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hypothermia_stage2", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.7f, 0.9f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.4f, 1.6f);
            // Heavier frost ring
            DisplayBuilder.particleRing(center, 8.0, Particle.DUST, 30,
                    new Particle.DustOptions(Color.fromRGB(100, 180, 255), 1.2f));
            DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), 20, 5.0,
                    100, 180, 255, 1.0f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            double radius = 8.0;

            // Heavier frost particles from ground to waist (y+0 to y+1.0)
            if (tick % 3 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 0.3, 0), 12, radius * 0.8,
                        100, 180, 255, 1.1f);
                DisplayBuilder.dustParticles(c.clone().add(0, 0.8, 0), 8, radius * 0.6,
                        220, 240, 255, 0.9f);
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 0.6, 0),
                        6, radius * 0.7, 0.5, radius * 0.7, 0.01);
            }

            // Larger frost ring at ground — pulses
            if (tick % 15 == 0) {
                DisplayBuilder.particleRing(c, radius, Particle.DUST, 32,
                        new Particle.DustOptions(Color.fromRGB(100, 180, 255), 1.2f));
                // Inner ring for layered effect
                DisplayBuilder.particleRing(c, radius * 0.6, Particle.DUST, 20,
                        new Particle.DustOptions(Color.fromRGB(30, 80, 160), 1.0f));
            }

            // Damage every 25 ticks — heavier than stage 1
            if (tick % 25 == 0) {
                damagePlayersInRadius(c, radius, config.getDamage() * 0.45, this);
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.4f, 1.1f);
            }

            // Ice crystal sparkles
            if (tick % 6 == 0) {
                w.spawnParticle(Particle.END_ROD, c.clone().add(0, 0.8, 0),
                        3, radius * 0.5, 0.5, radius * 0.5, 0.008);
            }

            // Periodic cracking sound
            if (tick % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.3f, 1.8f);
            }
        }

        @Override public AbstractAttack newInstance() { return new HypothermiaStage2(plugin); }
    }

    // ================================================================
    // 6. HYPOTHERMIA STAGE 3 — Full body frost particles, damage every
    //    15 ticks, massive frost ring + snowflake storm.
    //    Radius 10. Maximum severity — overwhelming visual + damage.
    // ================================================================
    public static class HypothermiaStage3 extends EnvironmentalAttack {
        private Location center;

        public HypothermiaStage3(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hypothermia_stage3", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.8f);
            // Massive initial frost burst
            DisplayBuilder.dustParticles(center.clone().add(0, 1.0, 0), 50, 8.0,
                    100, 180, 255, 1.5f);
            DisplayBuilder.dustParticles(center.clone().add(0, 2.0, 0), 30, 6.0,
                    220, 240, 255, 1.2f);
            DisplayBuilder.particleRing(center, 10.0, Particle.DUST, 40,
                    new Particle.DustOptions(Color.fromRGB(30, 80, 160), 1.5f));
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            double radius = 10.0;

            // Full body frost particles (y+0 to y+2.0) — extremely dense
            if (tick % 2 == 0) {
                // Ground level
                DisplayBuilder.dustParticles(c.clone().add(0, 0.2, 0), 15, radius * 0.8,
                        30, 80, 160, 1.3f);
                // Waist level
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 15, radius * 0.7,
                        100, 180, 255, 1.2f);
                // Head level
                DisplayBuilder.dustParticles(c.clone().add(0, 1.8, 0), 10, radius * 0.6,
                        220, 240, 255, 1.0f);

                // Snowflake storm
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 3.0, 0),
                        20, radius, 2.0, radius, 0.03);
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1.0, 0),
                        10, radius * 0.8, 1.0, radius * 0.8, 0.02);
            }

            // Massive frost ring — triple layered
            if (tick % 10 == 0) {
                DisplayBuilder.particleRing(c, radius, Particle.DUST, 40,
                        new Particle.DustOptions(Color.fromRGB(30, 80, 160), 1.5f));
                DisplayBuilder.particleRing(c, radius * 0.7, Particle.DUST, 30,
                        new Particle.DustOptions(Color.fromRGB(100, 180, 255), 1.3f));
                DisplayBuilder.particleRing(c, radius * 0.4, Particle.DUST, 20,
                        new Particle.DustOptions(Color.fromRGB(220, 240, 255), 1.1f));
            }

            // Heavy damage every 15 ticks — stage 3 is brutal
            if (tick % 15 == 0) {
                damagePlayersInRadius(c, radius, config.getDamage() * 0.6, this);
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.6f, 0.7f);
            }

            // Ice crystal spray
            if (tick % 4 == 0) {
                w.spawnParticle(Particle.END_ROD, c.clone().add(0, 1.5, 0),
                        5, radius * 0.6, 1.0, radius * 0.6, 0.01);
            }

            // Wind howl — constant during stage 3
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.ITEM_ELYTRA_FLYING, 0.7f, 1.6f);
            }

            // Crackling ice sounds
            if (tick % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.5f, 1.4f);
            }
        }

        @Override public AbstractAttack newInstance() { return new HypothermiaStage3(plugin); }
    }

    // ================================================================
    // 7. CORE TEMPERATURE DROP — Frost particles converge inward toward
    //    center point, damage pulses every 10 ticks.
    //    Radius 8. Particles visibly pull inward like heat draining away.
    // ================================================================
    public static class CoreTemperatureDrop extends EnvironmentalAttack {
        private Location center;

        public CoreTemperatureDrop(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("core_temp_drop", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.3f);
            // Initial outward frost burst — will then begin converging inward
            DisplayBuilder.particleRing(center, 8.0, Particle.DUST, 32,
                    new Particle.DustOptions(Color.fromRGB(100, 180, 255), 1.2f));
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            double maxRadius = 8.0;

            // Converging frost particles — spawn at outer edge and move inward
            // Use animated rings that shrink over time
            if (tick % 2 == 0) {
                // Calculate converging ring radius — cycles: expands then contracts
                double cycleProgress = (tick % 40) / 40.0;
                double ringRadius = maxRadius * (1.0 - cycleProgress);
                if (ringRadius < 0.5) ringRadius = 0.5;

                int points = (int) (ringRadius * 4) + 8;
                for (int i = 0; i < points; i++) {
                    double angle = (2.0 * Math.PI * i) / points + (tick * 0.05);
                    double x = Math.cos(angle) * ringRadius;
                    double z = Math.sin(angle) * ringRadius;
                    Location particleLoc = c.clone().add(x, 1.0, z);

                    // Particle moves inward — dust trail
                    DisplayBuilder.dustParticles(particleLoc, 1, 0.1, 100, 180, 255, 1.0f);
                }

                // Core frost accumulation at center
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0),
                        3 + (int) (10 * cycleProgress), 0.5 + cycleProgress,
                        30, 80, 160, 1.3f + (float) cycleProgress * 0.5f);
            }

            // Snowflakes converge from above
            if (tick % 4 == 0) {
                for (int i = 0; i < 6; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double dist = maxRadius * (0.3 + Math.random() * 0.7);
                    Location snowLoc = c.clone().add(Math.cos(angle) * dist, 2.5, Math.sin(angle) * dist);
                    w.spawnParticle(Particle.SNOWFLAKE, snowLoc, 1, 0, -0.1, 0, 0.02);
                }
            }

            // Damage pulse every 10 ticks — rapid
            if (tick % 10 == 0) {
                damagePlayersInRadius(c, maxRadius, config.getDamage() * 0.2, this);

                // Pulse visual: brief bright ring at center
                DisplayBuilder.particleRing(c.clone().add(0, 0.5, 0), 1.5, Particle.DUST, 12,
                        new Particle.DustOptions(Color.fromRGB(220, 240, 255), 1.5f));
            }

            // Deep freeze pulse sound
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.4f + (tick / 400.0f));
            }

            // Core whirring sound intensifies
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.5f, 0.6f);
            }
        }

        @Override public AbstractAttack newInstance() { return new CoreTemperatureDrop(plugin); }
    }

    // ================================================================
    // 8. EXTREMITIES FREEZE — Small damage ticks every 30 ticks with
    //    frost at player hands/feet (y+0 and y+1.5).
    //    Radius 7. Targets the player's extremities visually.
    // ================================================================
    public static class ExtremitiesFreeze extends EnvironmentalAttack {
        private Location center;

        public ExtremitiesFreeze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("extremities_freeze", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.5f, 1.6f);
            // Light ambient frost mist
            DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), 15, 5.0,
                    220, 240, 255, 0.8f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            double radius = 7.0;
            double r2 = radius * radius;

            // Light ambient frost in area
            if (tick % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), 6, radius * 0.6,
                        220, 240, 255, 0.7f);
            }

            // Per-player extremity frost visuals — frost at hands and feet
            if (tick % 3 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(c) > r2) continue;

                    Location feet = p.getLocation().clone();
                    Location hands = p.getLocation().clone().add(0, 1.5, 0);

                    // Frost at feet (y+0)
                    DisplayBuilder.dustParticles(feet, 3, 0.3, 100, 180, 255, 0.8f);
                    w.spawnParticle(Particle.SNOWFLAKE, feet, 2, 0.2, 0.1, 0.2, 0.005);

                    // Frost at hands (y+1.5)
                    DisplayBuilder.dustParticles(hands, 3, 0.3, 100, 180, 255, 0.8f);
                    w.spawnParticle(Particle.SNOWFLAKE, hands, 2, 0.3, 0.1, 0.3, 0.005);
                }
            }

            // Damage every 30 ticks — mild but persistent
            if (tick % 30 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(c) > r2) continue;

                    p.damage(config.getDamage() * 0.25);
                    p.setNoDamageTicks(0);

                    // Frost burst at extremities on damage tick
                    Location feet = p.getLocation().clone();
                    Location hands = p.getLocation().clone().add(0, 1.5, 0);
                    DisplayBuilder.dustParticles(feet, 8, 0.4, 30, 80, 160, 1.2f);
                    DisplayBuilder.dustParticles(hands, 8, 0.4, 30, 80, 160, 1.2f);
                    w.spawnParticle(Particle.END_ROD, feet, 3, 0.2, 0.1, 0.2, 0.01);
                    w.spawnParticle(Particle.END_ROD, hands, 3, 0.2, 0.1, 0.2, 0.01);
                }

                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.4f, 1.3f);
            }

            // Ground frost ring
            if (tick % 25 == 0) {
                DisplayBuilder.particleRing(c, radius, Particle.DUST, 24,
                        new Particle.DustOptions(Color.fromRGB(220, 240, 255), 0.9f));
            }
        }

        @Override public AbstractAttack newInstance() { return new ExtremitiesFreeze(plugin); }
    }

    // ================================================================
    // 9. WIND CHILL FACTOR — Checks player velocity: faster players
    //    take MORE damage. Wind particles scale with player speed.
    //    Radius 9. Opposite of ColdSap — running makes it worse.
    // ================================================================
    public static class WindChillFactor extends EnvironmentalAttack {
        private Location center;

        public WindChillFactor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("wind_chill_factor", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(9.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            DisplayBuilder.playSound(center, Sound.ITEM_ELYTRA_FLYING, 0.8f, 1.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.5f, 1.0f);
            // Wind gusts
            DisplayBuilder.dustParticles(center.clone().add(0, 1.5, 0), 25, 7.0,
                    220, 240, 255, 1.0f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            double radius = 9.0;
            double r2 = radius * radius;

            // Ambient wind particles — directional, blowing across area
            if (tick % 2 == 0) {
                double windAngle = tick * 0.03;
                double windX = Math.cos(windAngle) * 0.15;
                double windZ = Math.sin(windAngle) * 0.15;

                for (int i = 0; i < 8; i++) {
                    double rx = (Math.random() - 0.5) * radius * 2;
                    double rz = (Math.random() - 0.5) * radius * 2;
                    Location windLoc = c.clone().add(rx, 1.0 + Math.random() * 1.5, rz);
                    w.spawnParticle(Particle.SNOWFLAKE, windLoc, 1, windX, 0, windZ, 0.02);
                }

                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 5, radius * 0.7,
                        220, 240, 255, 0.8f);
            }

            // Per-player wind chill: velocity determines damage and particle intensity
            if (tick % 15 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(c) > r2) continue;

                    double velocity = p.getVelocity().length();
                    // Scale: velocity 0.0 = 0.15x damage, velocity 0.3+ = 0.7x damage
                    double speedFactor = Math.min(velocity / 0.3, 1.0);
                    double damage = config.getDamage() * (0.15 + speedFactor * 0.55);

                    p.damage(damage);
                    p.setNoDamageTicks(0);

                    // Wind particles scale with speed — more particles for faster players
                    int windCount = 3 + (int) (speedFactor * 15);
                    Location playerLoc = p.getLocation().add(0, 1.0, 0);
                    DisplayBuilder.dustParticles(playerLoc, windCount, 0.6 + speedFactor,
                            100, 180, 255, 1.0f + (float) speedFactor * 0.8f);
                    w.spawnParticle(Particle.SNOWFLAKE, playerLoc, windCount / 2,
                            0.3 + speedFactor * 0.5, 0.3, 0.3 + speedFactor * 0.5, 0.02 + speedFactor * 0.03);

                    // High-speed: extra deep ice particles
                    if (speedFactor > 0.6) {
                        DisplayBuilder.dustParticles(playerLoc, 8, 0.5,
                                30, 80, 160, 1.5f);
                    }
                }
            }

            // Wind ring
            if (tick % 20 == 0) {
                DisplayBuilder.particleRing(c, radius, Particle.DUST, 30,
                        new Particle.DustOptions(Color.fromRGB(220, 240, 255), 1.0f));
            }

            // Wind howl sound
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.ITEM_ELYTRA_FLYING, 0.6f, 1.2f + (float)(Math.random() * 0.4));
            }
        }

        @Override public AbstractAttack newInstance() { return new WindChillFactor(plugin); }
    }

    // ================================================================
    // 10. FROST SHOCK — Sudden burst at tick 20: 200 snowflake particles
    //     + heavy damage. Then rapid visual decay over remaining ticks.
    //     Radius 8. One big hit then fading aftermath.
    // ================================================================
    public static class FrostShock extends EnvironmentalAttack {
        private Location center;
        private boolean shockTriggered = false;

        public FrostShock(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_shock", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.shockTriggered = false;
            // Buildup warning — growing frost hum
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            double radius = 8.0;

            // Phase 1: Buildup (ticks 0-19) — growing frost tension
            if (tick < 20) {
                double buildProgress = tick / 20.0;
                int buildParticles = (int) (3 + buildProgress * 12);

                if (tick % 3 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), buildParticles,
                            radius * buildProgress, 100, 180, 255, 0.7f + (float) buildProgress * 0.5f);
                    // Converging ring
                    double shrinkRadius = radius * (1.0 - buildProgress * 0.5);
                    DisplayBuilder.particleRing(c, shrinkRadius, Particle.DUST, 16,
                            new Particle.DustOptions(Color.fromRGB(30, 80, 160),
                                    0.8f + (float) buildProgress * 0.7f));
                }

                // Intensifying warning hum
                if (tick == 10) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.8f);
                }
                if (tick == 15) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
                }
                return;
            }

            // Phase 2: SHOCK (tick 20) — massive burst
            if (!shockTriggered) {
                shockTriggered = true;

                // 200 snowflake particles — massive frost explosion
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1.5, 0),
                        200, radius, 3.0, radius, 0.08);

                // Frost dust burst in all three ice colors
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 60, radius,
                        100, 180, 255, 2.0f);
                DisplayBuilder.dustParticles(c.clone().add(0, 2.0, 0), 40, radius * 0.8,
                        220, 240, 255, 1.8f);
                DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), 30, radius * 0.6,
                        30, 80, 160, 1.5f);

                // Triple ring burst
                DisplayBuilder.particleRing(c, radius, Particle.DUST, 48,
                        new Particle.DustOptions(Color.fromRGB(220, 240, 255), 2.0f));
                DisplayBuilder.particleRing(c, radius * 0.6, Particle.DUST, 36,
                        new Particle.DustOptions(Color.fromRGB(100, 180, 255), 1.8f));
                DisplayBuilder.particleRing(c, radius * 0.3, Particle.DUST, 20,
                        new Particle.DustOptions(Color.fromRGB(30, 80, 160), 1.5f));

                // End rod sparkle burst
                w.spawnParticle(Particle.END_ROD, c.clone().add(0, 2.0, 0),
                        30, radius * 0.5, 2.0, radius * 0.5, 0.05);

                // Heavy damage
                damagePlayersInRadius(c, radius, config.getDamage() * 0.8, this);

                // Impact sounds
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.2f, 0.4f);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.6f);
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
                return;
            }

            // Phase 3: Rapid decay (ticks 21+) — frost dissipates
            int decayTick = tick - 20;
            double decayFactor = Math.max(0, 1.0 - (decayTick / 80.0));

            if (decayFactor <= 0) return;

            if (tick % 3 == 0) {
                int decayParticles = (int) (20 * decayFactor);
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), decayParticles,
                        radius * decayFactor, 100, 180, 255, (float) decayFactor * 1.5f);
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1.5, 0),
                        (int) (15 * decayFactor), radius * decayFactor, 2.0, radius * decayFactor, 0.02);
            }

            // Mild residual damage during decay — every 30 ticks
            if (decayTick % 30 == 0 && decayFactor > 0.3) {
                damagePlayersInRadius(c, radius * decayFactor, config.getDamage() * 0.15 * decayFactor, this);
            }
        }

        @Override public AbstractAttack newInstance() { return new FrostShock(plugin); }
    }

    // ================================================================
    // 11. COLD SWEAT — Warm orange particles ticks 0-80 (no damage),
    //     then sudden frost burst at tick 80+ with heavy damage.
    //     Radius 7. False sense of warmth followed by cold shock.
    // ================================================================
    public static class ColdSweat extends EnvironmentalAttack {
        private Location center;
        private boolean frostPhaseStarted = false;

        public ColdSweat(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cold_sweat", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.frostPhaseStarted = false;
            // Warm ambience — deceptive calm
            DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 0.5f, 1.0f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            double radius = 7.0;

            // Phase 1: Warm orange particles (ticks 0-79) — NO DAMAGE
            if (tick < 80) {
                double warmProgress = tick / 80.0;

                // Warm orange/amber dust — false warmth
                if (tick % 3 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0),
                            6 + (int) (warmProgress * 10), radius * 0.6,
                            255, 160, 50, 1.0f + (float) warmProgress * 0.3f);
                    // Hints of orange
                    DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0),
                            4, radius * 0.5,
                            255, 120, 30, 0.8f);
                }

                // Warm glow ring
                if (tick % 20 == 0) {
                    DisplayBuilder.particleRing(c, radius * 0.5, Particle.DUST, 16,
                            new Particle.DustOptions(Color.fromRGB(255, 160, 50), 1.0f));
                }

                // Transition warning: orange starts mixing with frost blue near tick 70+
                if (tick >= 70) {
                    double mixFactor = (tick - 70) / 10.0;
                    if (tick % 4 == 0) {
                        DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0),
                                (int) (5 * mixFactor), radius * 0.5,
                                100, 180, 255, 0.8f);
                    }
                }

                // Crackling warm sound
                if (tick % 40 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 0.3f, 1.2f);
                }
                return;
            }

            // Phase 2: Frost burst at tick 80 — transition moment
            if (!frostPhaseStarted) {
                frostPhaseStarted = true;

                // Sudden massive frost explosion replacing warmth
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1.5, 0),
                        100, radius, 2.5, radius, 0.06);
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 40, radius,
                        100, 180, 255, 1.8f);
                DisplayBuilder.dustParticles(c.clone().add(0, 2.0, 0), 25, radius * 0.7,
                        30, 80, 160, 1.5f);

                // Shock ring
                DisplayBuilder.particleRing(c, radius, Particle.DUST, 36,
                        new Particle.DustOptions(Color.fromRGB(100, 180, 255), 1.8f));

                // Heavy initial frost damage
                damagePlayersInRadius(c, radius, config.getDamage() * 0.7, this);

                // Transition sounds
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.5f);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.8f);
                return;
            }

            // Phase 3: Continued frost (ticks 81+) — cold damage persists
            int frostTick = tick - 80;

            if (tick % 3 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 12, radius * 0.7,
                        100, 180, 255, 1.2f);
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1.5, 0),
                        8, radius * 0.6, 1.5, radius * 0.6, 0.02);
            }

            // Frost ring
            if (tick % 15 == 0) {
                DisplayBuilder.particleRing(c, radius, Particle.DUST, 28,
                        new Particle.DustOptions(Color.fromRGB(220, 240, 255), 1.2f));
            }

            // Continued damage every 20 ticks
            if (frostTick % 20 == 0) {
                damagePlayersInRadius(c, radius, config.getDamage() * 0.4, this);
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.4f, 1.0f);
            }
        }

        @Override public AbstractAttack newInstance() { return new ColdSweat(plugin); }
    }

    // ================================================================
    // 12. NUMBNESS SPREAD — Damage tick interval shrinks over time:
    //     starts at 50 ticks, goes to 40, 30, 20, 10 tick intervals.
    //     Radius 8. Feels like numbness creeping in faster and faster.
    // ================================================================
    public static class NumbnessSpread extends EnvironmentalAttack {
        private Location center;
        private int lastDamageTick = 0;

        public NumbnessSpread(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("numbness_spread", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.lastDamageTick = 0;
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.6f, 1.2f);
            DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), 15, 5.0,
                    220, 240, 255, 0.8f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            double radius = 8.0;

            // Calculate current damage interval based on progress
            // 0-40 ticks: interval 50, 40-80: interval 40, 80-120: interval 30,
            // 120-160: interval 20, 160+: interval 10
            int interval;
            int stage;
            if (tick < 40) {
                interval = 50;
                stage = 0;
            } else if (tick < 80) {
                interval = 40;
                stage = 1;
            } else if (tick < 120) {
                interval = 30;
                stage = 2;
            } else if (tick < 160) {
                interval = 20;
                stage = 3;
            } else {
                interval = 10;
                stage = 4;
            }

            // Particle density increases with stage
            int particleDensity = 4 + (stage * 4);
            float particleSize = 0.8f + (stage * 0.2f);

            // Ambient frost particles — intensity grows
            if (tick % (4 - Math.min(stage, 3)) == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), particleDensity,
                        radius * 0.7, 100, 180, 255, particleSize);
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1.0, 0),
                        particleDensity / 2, radius * 0.6, 0.8, radius * 0.6, 0.01);

                // Deep ice particles at higher stages
                if (stage >= 2) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0),
                            stage * 2, radius * 0.5, 30, 80, 160, particleSize + 0.3f);
                }
            }

            // Frost ring — pulses faster at higher stages
            if (tick % (20 - stage * 3) == 0) {
                DisplayBuilder.particleRing(c, radius, Particle.DUST, 24 + (stage * 4),
                        new Particle.DustOptions(Color.fromRGB(100, 180, 255), particleSize));
            }

            // Numbness creep visual: spreading inner ring
            if (stage >= 2 && tick % 15 == 0) {
                double innerRadius = 2.0 + stage * 1.0;
                DisplayBuilder.particleRing(c, innerRadius, Particle.DUST, 16 + stage * 2,
                        new Particle.DustOptions(Color.fromRGB(30, 80, 160), 1.3f));
            }

            // Damage at the dynamically shrinking interval
            if (tick - lastDamageTick >= interval) {
                lastDamageTick = tick;
                damagePlayersInRadius(c, radius, config.getDamage() * 0.3, this);

                // Damage pulse visual
                w.spawnParticle(Particle.END_ROD, c.clone().add(0, 1.0, 0),
                        3 + stage * 2, radius * 0.4, 0.5, radius * 0.4, 0.01);

                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE,
                        0.3f + (stage * 0.1f), 0.8f + (stage * 0.15f));
            }

            // Stage transition sound
            if (tick == 40 || tick == 80 || tick == 120 || tick == 160) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.5f, 1.0f + (stage * 0.2f));
            }
        }

        @Override public AbstractAttack newInstance() { return new NumbnessSpread(plugin); }
    }

    // ================================================================
    // 13. FINAL CHILL — No damage for first 60 ticks (ominous buildup),
    //     then massive continuous frost damage + full particle storm.
    //     Radius 10. The ultimate hypothermia attack — long windup,
    //     devastating payoff.
    // ================================================================
    public static class FinalChill extends EnvironmentalAttack {
        private Location center;
        private boolean chillActive = false;

        public FinalChill(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("final_chill", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.chillActive = false;
            // Ominous low tone — something is coming
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 0.2f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            double radius = 10.0;

            // Phase 1: Ominous buildup (ticks 0-59) — NO DAMAGE
            if (tick < 60) {
                double buildProgress = tick / 60.0;

                // Slow-growing frost mist — eerie and foreboding
                if (tick % 4 == 0) {
                    int mist = (int) (3 + buildProgress * 15);
                    double mistRadius = radius * buildProgress;
                    DisplayBuilder.dustParticles(c.clone().add(0, 0.3, 0), mist, mistRadius,
                            30, 80, 160, 0.6f + (float) buildProgress * 0.6f);
                }

                // Slow converging snowflakes from edges
                if (tick % 6 == 0) {
                    for (int i = 0; i < (int) (2 + buildProgress * 6); i++) {
                        double angle = Math.random() * Math.PI * 2;
                        double dist = radius * (0.5 + (1.0 - buildProgress) * 0.5);
                        Location snowLoc = c.clone().add(
                                Math.cos(angle) * dist, 2.0 + Math.random(), Math.sin(angle) * dist);
                        w.spawnParticle(Particle.SNOWFLAKE, snowLoc, 1, 0, -0.05, 0, 0.01);
                    }
                }

                // Building frost ring — grows from nothing
                if (tick % 10 == 0 && buildProgress > 0.2) {
                    double ringRadius = radius * buildProgress;
                    DisplayBuilder.particleRing(c, ringRadius, Particle.DUST,
                            (int) (8 + buildProgress * 20),
                            new Particle.DustOptions(Color.fromRGB(30, 80, 160),
                                    0.5f + (float) buildProgress * 0.8f));
                }

                // Escalating warning sounds
                if (tick == 20) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.4f);
                }
                if (tick == 40) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 0.6f);
                    DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.4f, 1.5f);
                }
                if (tick == 55) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.0f);
                    // Pre-detonation frost pulse
                    DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 25, radius * 0.6,
                            100, 180, 255, 1.2f);
                }
                return;
            }

            // Phase 2: THE FINAL CHILL HITS (tick 60) — transition moment
            if (!chillActive) {
                chillActive = true;

                // Massive frost detonation
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 2.0, 0),
                        150, radius, 3.0, radius, 0.1);
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 0.5, 0),
                        100, radius * 0.8, 1.0, radius * 0.8, 0.06);

                // Three-color frost explosion
                DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), 50, radius,
                        30, 80, 160, 2.0f);
                DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), 50, radius * 0.8,
                        100, 180, 255, 1.8f);
                DisplayBuilder.dustParticles(c.clone().add(0, 2.5, 0), 40, radius * 0.6,
                        220, 240, 255, 1.5f);

                // Massive triple ring
                DisplayBuilder.particleRing(c, radius, Particle.DUST, 50,
                        new Particle.DustOptions(Color.fromRGB(30, 80, 160), 2.0f));
                DisplayBuilder.particleRing(c, radius * 0.65, Particle.DUST, 40,
                        new Particle.DustOptions(Color.fromRGB(100, 180, 255), 1.8f));
                DisplayBuilder.particleRing(c, radius * 0.3, Particle.DUST, 24,
                        new Particle.DustOptions(Color.fromRGB(220, 240, 255), 1.5f));

                // End rod crystal burst
                w.spawnParticle(Particle.END_ROD, c.clone().add(0, 2.0, 0),
                        40, radius * 0.5, 2.5, radius * 0.5, 0.06);

                // Heavy initial damage
                damagePlayersInRadius(c, radius, config.getDamage() * 0.8, this);

                // Detonation sounds
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.2f, 0.3f);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
                DisplayBuilder.playSound(c, Sound.ITEM_ELYTRA_FLYING, 0.8f, 1.8f);
                return;
            }

            // Phase 3: Full particle storm + continuous damage (ticks 61+)
            int stormTick = tick - 60;

            // Full-body snowflake storm — absolutely overwhelming
            if (tick % 2 == 0) {
                // Heavy snowflake downpour
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 4.0, 0),
                        25, radius, 2.0, radius, 0.04);
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1.5, 0),
                        15, radius * 0.8, 1.0, radius * 0.8, 0.03);

                // Multi-layer frost dust
                DisplayBuilder.dustParticles(c.clone().add(0, 0.3, 0), 12, radius * 0.8,
                        30, 80, 160, 1.4f);
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 12, radius * 0.7,
                        100, 180, 255, 1.2f);
                DisplayBuilder.dustParticles(c.clone().add(0, 1.8, 0), 8, radius * 0.6,
                        220, 240, 255, 1.0f);
            }

            // Frost rings pulse rapidly
            if (tick % 8 == 0) {
                DisplayBuilder.particleRing(c, radius, Particle.DUST, 40,
                        new Particle.DustOptions(Color.fromRGB(30, 80, 160), 1.5f));
                DisplayBuilder.particleRing(c, radius * 0.5, Particle.DUST, 24,
                        new Particle.DustOptions(Color.fromRGB(100, 180, 255), 1.3f));
            }

            // End rod ice crystals
            if (tick % 5 == 0) {
                w.spawnParticle(Particle.END_ROD, c.clone().add(0, 1.5, 0),
                        6, radius * 0.6, 1.5, radius * 0.6, 0.015);
            }

            // Massive continuous damage every 10 ticks
            if (stormTick % 10 == 0) {
                damagePlayersInRadius(c, radius, config.getDamage() * 0.5, this);
            }

            // Storm sounds
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.6f, 0.5f);
                DisplayBuilder.playSound(c, Sound.ITEM_ELYTRA_FLYING, 0.5f, 1.5f);
            }
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.4f, 1.2f);
            }
        }

        @Override public AbstractAttack newInstance() { return new FinalChill(plugin); }
    }
}
