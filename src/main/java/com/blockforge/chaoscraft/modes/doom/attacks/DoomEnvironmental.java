package com.blockforge.chaoscraft.modes.doom.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Doom Mode -- ENVIRONMENTAL ATTACKS (15 attacks)
 * Heat, tremor, infernal, and volcanic environmental effects.
 * These attacks use particles, sounds, and player effects -- NO block displays.
 * All configurable via AttackConfig YAML.
 *
 * Doom palette particles:
 * - FLAME, LAVA, SMOKE, CAMPFIRE_SIGNAL_SMOKE for fire/heat
 * - BLOCK (NETHERRACK/MAGMA_BLOCK) for ground effects
 * - DUST (custom colors) for atmospheric effects
 * - DRIPPING_LAVA for overhead drip effects
 * - LANDING_LAVA for ground pooling
 */
public final class DoomEnvironmental {
    private DoomEnvironmental() {}

    private static final String MODE_PATH = "modes/doom/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new HeatDistortion(plugin));       // 1
        registry.register(new EmberStorm(plugin));            // 2
        registry.register(new GroundTremor(plugin));          // 3
        registry.register(new SulfurCloud(plugin));           // 4
        registry.register(new MagmaGasVent(plugin));          // 5
        registry.register(new LavaDroplets(plugin));          // 6
        registry.register(new ScorchingWind(plugin));         // 7
        registry.register(new MoltenGroundPatch(plugin));     // 8
        registry.register(new InfernalWhispers(plugin));      // 9
        registry.register(new AshfallBlanket(plugin));        // 10
        registry.register(new NetherQuake(plugin));           // 11
        registry.register(new FireWhirl(plugin));             // 12
        registry.register(new LavaburstGeyser(plugin));       // 13
        registry.register(new VolcanicAshCloud(plugin));      // 14
        registry.register(new HellstormLightning(plugin));    // 15
    }

    // ================================================================
    // 1. HEAT DISTORTION
    //    Shimmering heat haze: CAMPFIRE_SIGNAL_SMOKE + FLAME.
    //    2 damage / 20 ticks-between, 8r.
    // ================================================================
    public static class HeatDistortion extends EnvironmentalAttack {
        public HeatDistortion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("heat_distortion", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(160);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            if (tick % 4 == 0) {
                double radius = config.getDamageRadius();
                for (int i = 0; i < 8; i++) {
                    double ox = (Math.random() - 0.5) * radius * 2;
                    double oz = (Math.random() - 0.5) * radius * 2;
                    Location loc = getCenter().clone().add(ox, Math.random() * 3, oz);
                    w.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, loc, 1, 0.3, 0.5, 0.3, 0.005);
                    w.spawnParticle(Particle.FLAME, loc, 1, 0.2, 0.2, 0.2, 0.005);
                }
            }
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_FIRE_AMBIENT, 0.6f, 0.4f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new HeatDistortion(plugin); }
    }

    // ================================================================
    // 2. EMBER STORM
    //    LAVA + FLAME rain from above. 3 damage / 15 ticks-between, 10r.
    // ================================================================
    public static class EmberStorm extends EnvironmentalAttack {
        public EmberStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ember_storm", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.0);
            config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(200);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 1.2f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            if (tick % 3 == 0) {
                double radius = config.getDamageRadius();
                for (int i = 0; i < 12; i++) {
                    double ox = (Math.random() - 0.5) * radius * 2;
                    double oz = (Math.random() - 0.5) * radius * 2;
                    Location drop = getCenter().clone().add(ox, 12 + Math.random() * 5, oz);
                    w.spawnParticle(Particle.LAVA, drop, 2, 0.3, 0.3, 0.3, 0.05);
                    w.spawnParticle(Particle.FLAME, drop, 1, 0.2, 0.2, 0.2, 0.01);
                }
            }
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_LAVA_AMBIENT, 0.8f, 0.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new EmberStorm(plugin); }
    }

    // ================================================================
    // 3. GROUND TREMOR
    //    BLOCK_CRACK(NETHERRACK) + velocity Y pulse +/-0.1.
    //    4 damage / 40 ticks-between, 8r.
    // ================================================================
    public static class GroundTremor extends EnvironmentalAttack {
        public GroundTremor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ground_tremor", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(4.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(40);
            config.setDurationTicks(120);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            if (center.getWorld() == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.3f);
            center.getWorld().spawnParticle(Particle.BLOCK, center, 50, 6, 0.5, 6, 0.1,
                    Material.NETHERRACK.createBlockData());
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            if (tick % 8 == 0) {
                w.spawnParticle(Particle.BLOCK, getCenter(), 20, 5, 0.3, 5, 0.05,
                        Material.NETHERRACK.createBlockData());

                // Velocity Y pulse for nearby players
                double radius = config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    if (p.getLocation().distanceSquared(getCenter()) <= radius * radius) {
                        double yPulse = (Math.random() > 0.5) ? 0.1 : -0.1;
                        p.setVelocity(p.getVelocity().add(new Vector(0, yPulse, 0)));
                    }
                }
            }

            if (tick % 30 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_ANVIL_LAND, 0.4f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new GroundTremor(plugin); }
    }

    // ================================================================
    // 4. SULFUR CLOUD
    //    Yellow DUST drifting, damage ramps from 1 to 5 over duration. 6r.
    // ================================================================
    public static class SulfurCloud extends EnvironmentalAttack {
        public SulfurCloud(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sulfur_cloud", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(1.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_CREEPER_PRIMED, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Ramp damage from 1 to 5 over duration
            double progress = (double) tick / config.getDurationTicks();
            config.setDamage(1.0 + progress * 4.0);

            if (tick % 4 == 0) {
                double radius = config.getDamageRadius();
                for (int i = 0; i < 10; i++) {
                    double ox = (Math.random() - 0.5) * radius * 2;
                    double oy = Math.random() * 3;
                    double oz = (Math.random() - 0.5) * radius * 2;
                    Location loc = getCenter().clone().add(ox, oy, oz);
                    // Yellow sulfur dust
                    DisplayBuilder.dustParticles(loc, 2, 0.5, 220, 180, 30, 1.5f);
                }
            }

            if (tick % 30 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_LAVA_EXTINGUISH, 0.5f, 0.4f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new SulfurCloud(plugin); }
    }

    // ================================================================
    // 5. MAGMA GAS VENT
    //    FLAME + SMOKE vertical column. 5 damage / 20 ticks-between, 3r column.
    // ================================================================
    public static class MagmaGasVent extends EnvironmentalAttack {
        public MagmaGasVent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_gas_vent", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(5.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(140);
            config.setCooldownTicks(220);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            if (tick % 3 == 0) {
                // Vertical column of flame + smoke
                for (int y = 0; y < 10; y++) {
                    Location loc = getCenter().clone().add(
                            (Math.random() - 0.5) * 0.8, y * 0.8, (Math.random() - 0.5) * 0.8);
                    w.spawnParticle(Particle.FLAME, loc, 2, 0.15, 0.1, 0.15, 0.02);
                    w.spawnParticle(Particle.SMOKE, loc, 1, 0.2, 0.1, 0.2, 0.01);
                }
            }

            if (tick % 25 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_BLAZE_AMBIENT, 0.7f, 0.4f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new MagmaGasVent(plugin); }
    }

    // ================================================================
    // 6. LAVA DROPLETS
    //    DRIPPING_LAVA from Y+10. 2 contact damage per drip tick.
    // ================================================================
    public static class LavaDroplets extends EnvironmentalAttack {
        public LavaDroplets(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lava_droplets", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(180);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            if (tick % 4 == 0) {
                double radius = config.getDamageRadius();
                for (int i = 0; i < 8; i++) {
                    double ox = (Math.random() - 0.5) * radius * 2;
                    double oz = (Math.random() - 0.5) * radius * 2;
                    Location dropLoc = getCenter().clone().add(ox, 10, oz);
                    w.spawnParticle(Particle.DRIPPING_LAVA, dropLoc, 3, 0.5, 0.2, 0.5, 0);
                }
            }

            if (tick % 10 == 0) {
                // Ground splatter particles where drips land
                w.spawnParticle(Particle.LAVA, getCenter(), 5, 4, 0.1, 4, 0.01);
            }

            if (tick % 35 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_LAVA_POP, 0.5f, 0.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new LavaDroplets(plugin); }
    }

    // ================================================================
    // 7. SCORCHING WIND
    //    Directional SMOKE + knockback velocity. 3 damage / 15 ticks-between, 10r line.
    // ================================================================
    public static class ScorchingWind extends EnvironmentalAttack {
        private double windDirX;
        private double windDirZ;

        public ScorchingWind(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("scorching_wind", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.0);
            config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(160);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            // Random wind direction
            double angle = Math.random() * 2 * Math.PI;
            windDirX = Math.cos(angle);
            windDirZ = Math.sin(angle);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            if (tick % 3 == 0) {
                // Directional smoke line
                for (int i = 0; i < 15; i++) {
                    double dist = i * 0.7;
                    Location loc = getCenter().clone().add(
                            windDirX * dist + (Math.random() - 0.5) * 1.5,
                            0.5 + Math.random() * 2,
                            windDirZ * dist + (Math.random() - 0.5) * 1.5);
                    w.spawnParticle(Particle.SMOKE, loc, 2, 0.3, 0.2, 0.3, 0.01);
                    w.spawnParticle(Particle.FLAME, loc, 1, 0.1, 0.1, 0.1, 0.005);
                }
            }

            // Knockback players in wind direction
            if (tick % 10 == 0) {
                double radius = config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    if (p.getLocation().distanceSquared(getCenter()) <= radius * radius) {
                        p.setVelocity(p.getVelocity().add(new Vector(windDirX * 0.3, 0.05, windDirZ * 0.3)));
                    }
                }
            }

            if (tick % 25 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_PHANTOM_FLAP, 0.6f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ScorchingWind(plugin); }
    }

    // ================================================================
    // 8. MOLTEN GROUND PATCH
    //    LANDING_LAVA circle ground. 4 damage / 10 ticks-between standing, 5r.
    // ================================================================
    public static class MoltenGroundPatch extends EnvironmentalAttack {
        public MoltenGroundPatch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("molten_ground_patch", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(4.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(180);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 1.0f, 0.5f);
            if (center.getWorld() != null) {
                center.getWorld().spawnParticle(Particle.LAVA, center, 30, 3, 0.2, 3, 0.05);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            if (tick % 5 == 0) {
                double radius = config.getDamageRadius();
                // Ground-level lava particles in circle
                int points = 20;
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points;
                    double r = Math.random() * radius;
                    Location loc = getCenter().clone().add(Math.cos(angle) * r, 0.1, Math.sin(angle) * r);
                    w.spawnParticle(Particle.LANDING_LAVA, loc, 1, 0.3, 0.05, 0.3, 0);
                }
                // Center glow
                w.spawnParticle(Particle.FLAME, getCenter().clone().add(0, 0.2, 0), 3, 1.5, 0.1, 1.5, 0.005);
            }

            if (tick % 30 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_LAVA_POP, 0.6f, 0.4f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new MoltenGroundPatch(plugin); }
    }

    // ================================================================
    // 9. INFERNAL WHISPERS
    //    SCULK_SOUL particles + VEX_AMBIENT sounds. 2 damage / 30 ticks-between, 8r.
    // ================================================================
    public static class InfernalWhispers extends EnvironmentalAttack {
        public InfernalWhispers(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_whispers", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(30);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_VEX_AMBIENT, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            if (tick % 6 == 0) {
                double radius = config.getDamageRadius();
                for (int i = 0; i < 6; i++) {
                    double ox = (Math.random() - 0.5) * radius * 2;
                    double oy = Math.random() * 2.5;
                    double oz = (Math.random() - 0.5) * radius * 2;
                    Location loc = getCenter().clone().add(ox, oy, oz);
                    w.spawnParticle(Particle.SCULK_SOUL, loc, 1, 0.2, 0.2, 0.2, 0.01);
                }
            }

            // Whisper sounds at varying intervals
            if (tick % 25 == 0) {
                float pitch = 0.3f + (float) (Math.random() * 0.4);
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_VEX_AMBIENT, 0.5f, pitch);
            }
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_PHANTOM_AMBIENT, 0.3f, 0.2f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new InfernalWhispers(plugin); }
    }

    // ================================================================
    // 10. ASHFALL BLANKET
    //     SMOKE + gray DUST falling. 1 damage / 10 ticks-between, 12r.
    // ================================================================
    public static class AshfallBlanket extends EnvironmentalAttack {
        public AshfallBlanket(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ashfall_blanket", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(1.0);
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(240);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.WEATHER_RAIN, 0.5f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            if (tick % 3 == 0) {
                double radius = config.getDamageRadius();
                for (int i = 0; i < 15; i++) {
                    double ox = (Math.random() - 0.5) * radius * 2;
                    double oz = (Math.random() - 0.5) * radius * 2;
                    double oy = 8 + Math.random() * 6;
                    Location loc = getCenter().clone().add(ox, oy, oz);
                    w.spawnParticle(Particle.SMOKE, loc, 1, 0.5, 0.3, 0.5, 0.005);
                    // Gray ash dust
                    DisplayBuilder.dustParticles(loc, 1, 0.3, 120, 120, 120, 1.0f);
                }
            }

            if (tick % 50 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.WEATHER_RAIN, 0.3f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new AshfallBlanket(plugin); }
    }

    // ================================================================
    // 11. NETHER QUAKE
    //     BLOCK_CRACK burst + Y velocity push 0.4. 6 damage / 60 ticks-between, 8r.
    // ================================================================
    public static class NetherQuake extends EnvironmentalAttack {
        public NetherQuake(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nether_quake", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(6.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(60);
            config.setDurationTicks(120);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            if (center.getWorld() == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_ROAR, 1.0f, 0.3f);
            center.getWorld().spawnParticle(Particle.BLOCK, center, 60, 6, 0.5, 6, 0.1,
                    Material.MAGMA_BLOCK.createBlockData());
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Quake burst every 20 ticks
            if (tick % 20 == 0) {
                w.spawnParticle(Particle.BLOCK, getCenter(), 40, 5, 0.3, 5, 0.1,
                        Material.NETHERRACK.createBlockData());
                w.spawnParticle(Particle.BLOCK, getCenter(), 20, 4, 0.2, 4, 0.05,
                        Material.MAGMA_BLOCK.createBlockData());

                // Y velocity push for nearby players
                double radius = config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    if (p.getLocation().distanceSquared(getCenter()) <= radius * radius) {
                        p.setVelocity(p.getVelocity().add(new Vector(0, 0.4, 0)));
                    }
                }

                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new NetherQuake(plugin); }
    }

    // ================================================================
    // 12. FIRE WHIRL
    //     Spinning FLAME tornado that tracks player. 3 damage / 10 ticks-between, 4r.
    // ================================================================
    public static class FireWhirl extends EnvironmentalAttack {
        private double whirlAngle = 0;

        public FireWhirl(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fire_whirl", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(10);
            config.setTracksPlayer(true);
            config.setDurationTicks(180);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            whirlAngle += 0.3;

            if (tick % 2 == 0) {
                // Tornado spiral: particles at increasing heights with rotation
                for (int y = 0; y < 8; y++) {
                    double radius = 0.5 + y * 0.4;
                    double angle = whirlAngle + y * 0.8;
                    double px = Math.cos(angle) * radius;
                    double pz = Math.sin(angle) * radius;
                    Location loc = getCenter().clone().add(px, y * 0.5, pz);
                    w.spawnParticle(Particle.FLAME, loc, 2, 0.1, 0.1, 0.1, 0.01);

                    // Counter-spiral
                    double px2 = Math.cos(angle + Math.PI) * radius * 0.6;
                    double pz2 = Math.sin(angle + Math.PI) * radius * 0.6;
                    Location loc2 = getCenter().clone().add(px2, y * 0.5, pz2);
                    w.spawnParticle(Particle.SMOKE, loc2, 1, 0.1, 0.1, 0.1, 0.005);
                }
            }

            if (tick % 20 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_BLAZE_AMBIENT, 0.6f, 0.6f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FireWhirl(plugin); }
    }

    // ================================================================
    // 13. LAVABURST GEYSER
    //     LAVA + FLAME eruption. 8 impact center, 3r.
    // ================================================================
    public static class LavaburstGeyser extends EnvironmentalAttack {
        public LavaburstGeyser(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lavaburst_geyser", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(100);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            if (center.getWorld() == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.4f);
            center.getWorld().spawnParticle(Particle.LAVA, center, 40, 1.0, 3.0, 1.0, 0.1);
            center.getWorld().spawnParticle(Particle.FLAME, center, 30, 0.8, 4.0, 0.8, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Eruption column
            if (tick % 4 == 0) {
                int height = 6 + (int) (Math.sin(tick * 0.1) * 3);
                for (int y = 0; y < height; y++) {
                    Location loc = getCenter().clone().add(
                            (Math.random() - 0.5) * 1.0, y * 0.8, (Math.random() - 0.5) * 1.0);
                    w.spawnParticle(Particle.LAVA, loc, 2, 0.2, 0.2, 0.2, 0.05);
                    w.spawnParticle(Particle.FLAME, loc, 2, 0.15, 0.15, 0.15, 0.02);
                }
            }

            // Splatter at base
            if (tick % 8 == 0) {
                w.spawnParticle(Particle.LANDING_LAVA, getCenter(), 5, 2.0, 0.1, 2.0, 0);
            }

            if (tick % 20 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_LAVA_POP, 0.8f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new LavaburstGeyser(plugin); }
    }

    // ================================================================
    // 14. VOLCANIC ASH CLOUD
    //     LARGE_SMOKE overhead. 2 damage / 15 ticks-between, 10r.
    // ================================================================
    public static class VolcanicAshCloud extends EnvironmentalAttack {
        public VolcanicAshCloud(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("volcanic_ash_cloud", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2.0);
            config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(220);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 0.2f);
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            if (tick % 4 == 0) {
                double radius = config.getDamageRadius();
                // Overhead cloud layer at Y+8 to Y+12
                for (int i = 0; i < 12; i++) {
                    double ox = (Math.random() - 0.5) * radius * 2;
                    double oz = (Math.random() - 0.5) * radius * 2;
                    double oy = 8 + Math.random() * 4;
                    Location loc = getCenter().clone().add(ox, oy, oz);
                    w.spawnParticle(Particle.LARGE_SMOKE, loc, 1, 1.0, 0.3, 1.0, 0.005);
                }
                // Some ash falling from cloud
                for (int i = 0; i < 4; i++) {
                    double ox = (Math.random() - 0.5) * radius * 1.5;
                    double oz = (Math.random() - 0.5) * radius * 1.5;
                    Location fallLoc = getCenter().clone().add(ox, 7, oz);
                    DisplayBuilder.dustParticles(fallLoc, 1, 0.3, 100, 100, 100, 0.8f);
                }
            }

            if (tick % 40 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.WEATHER_RAIN, 0.4f, 0.2f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new VolcanicAshCloud(plugin); }
    }

    // ================================================================
    // 15. HELLSTORM LIGHTNING
    //     White DUST line from sky + damage 10 on impact. 4r.
    // ================================================================
    public static class HellstormLightning extends EnvironmentalAttack {
        private int nextStrikeTick = 0;

        public HellstormLightning(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellstorm_lightning", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(10.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(20);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(4.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            nextStrikeTick = 20;
            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            if (tick == nextStrikeTick) {
                // Schedule next strike
                nextStrikeTick = tick + 30 + (int) (Math.random() * 30);

                // Random strike location within radius
                double radius = config.getDamageRadius() * 2;
                double ox = (Math.random() - 0.5) * radius;
                double oz = (Math.random() - 0.5) * radius;
                Location strikeLoc = getCenter().clone().add(ox, 0, oz);

                // White dust line from sky to ground (lightning bolt visual)
                for (int y = 0; y < 15; y++) {
                    Location lineLoc = strikeLoc.clone().add(
                            (Math.random() - 0.5) * 0.3, y, (Math.random() - 0.5) * 0.3);
                    DisplayBuilder.dustParticles(lineLoc, 3, 0.1, 255, 255, 255, 2.0f);
                    // Orange edges
                    DisplayBuilder.dustParticles(lineLoc, 1, 0.3, 255, 150, 50, 1.0f);
                }

                // Impact burst
                w.spawnParticle(Particle.FLAME, strikeLoc, 15, 1.5, 0.3, 1.5, 0.03);
                w.spawnParticle(Particle.SMOKE, strikeLoc, 10, 1.0, 0.5, 1.0, 0.02);

                DisplayBuilder.playSound(strikeLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.5f, 0.6f);
                triggerImpactDamage(strikeLoc);
            }

            // Ambient rumble between strikes
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new HellstormLightning(plugin); }
    }
}
