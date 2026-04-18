package com.blockforge.chaoscraft.modes.doom.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.ItemDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Doom Mode — ENVIRONMENTAL ATTACKS (15 attacks, heavily enhanced).
 * Each attack uses a mix of: particles, item displays, block displays,
 * orbital animations, and interpolated transformations to produce dense,
 * recognizable effects rather than plain particle emitters.
 */
public final class DoomEnvironmental {
    private DoomEnvironmental() {}

    private static final String MODE_PATH = "modes/doom/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new HeatDistortion(plugin));
        registry.register(new EmberStorm(plugin));
        registry.register(new GroundTremor(plugin));
        registry.register(new SulfurCloud(plugin));
        registry.register(new MagmaGasVent(plugin));
        registry.register(new LavaDroplets(plugin));
        registry.register(new ScorchingWind(plugin));
        registry.register(new MoltenGroundPatch(plugin));
        registry.register(new InfernalWhispers(plugin));
        registry.register(new AshfallBlanket(plugin));
        registry.register(new NetherQuake(plugin));
        registry.register(new FireWhirl(plugin));
        registry.register(new LavaburstGeyser(plugin));
        registry.register(new VolcanicAshCloud(plugin));
        registry.register(new HellstormLightning(plugin));
    }

    // ================================================================
    // 1. HEAT DISTORTION
    //    Shimmering heat column with 6 orbiting magma-cream item displays
    //    and rising flame particles in a lattice.
    // ================================================================
    public static class HeatDistortion extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> orbs = new ArrayList<>();

        public HeatDistortion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("heat_distortion", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.9f, 0.5f);
            // 6 orbiting heat orbs at varying heights
            for (int i = 0; i < 6; i++) {
                ItemDisplayHandle h = displayBuilder.spawnItem(
                        center.clone().add(0, 1.5 + i * 0.4, 0),
                        new ItemStack(Material.MAGMA_CREAM));
                h.scale(0.9f, 0.9f, 0.9f).glow(255, 120, 40).interpolation(8, 0);
                orbs.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            double radius = config.getDamageRadius();

            // Orbit update every 4 ticks with interpolation
            if (tick % 4 == 0) {
                for (int i = 0; i < orbs.size(); i++) {
                    double angle = tick * 0.08 + (Math.PI * 2 * i / orbs.size());
                    double orbitR = 2.5 + Math.sin(tick * 0.05 + i) * 0.5;
                    float tx = (float) (Math.cos(angle) * orbitR);
                    float ty = 1.5f + i * 0.4f + (float) Math.sin(tick * 0.1 + i) * 0.3f;
                    float tz = (float) (Math.sin(angle) * orbitR);
                    orbs.get(i).animateTo(
                            new Vector3f(tx - 0.5f, ty, tz - 0.5f),
                            new AxisAngle4f((float) (tick * 0.1), 0, 1, 0),
                            new Vector3f(0.9f, 0.9f, 0.9f), 4);
                }
            }

            // Dense shimmer particles in a lattice
            if (tick % 2 == 0) {
                for (int i = 0; i < 14; i++) {
                    double ox = (Math.random() - 0.5) * radius * 2;
                    double oz = (Math.random() - 0.5) * radius * 2;
                    double oy = Math.random() * 4;
                    Location loc = getCenter().clone().add(ox, oy, oz);
                    w.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, loc, 1, 0.2, 0.3, 0.2, 0.01);
                    w.spawnParticle(Particle.FLAME, loc, 1, 0.15, 0.15, 0.15, 0.01);
                }
                DisplayBuilder.dustParticles(getCenter().clone().add(0, 1, 0), 3, radius * 0.5,
                        255, 140, 40, 1.3f);
            }

            if (tick % 30 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_FIRE_AMBIENT, 0.7f, 0.4f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); orbs.clear(); }
        @Override public AbstractAttack newInstance() { return new HeatDistortion(plugin); }
    }

    // ================================================================
    // 2. EMBER STORM
    //    Falling ItemDisplay ember blocks + dense lava/flame particle rain
    //    with an overhead ring of floating magma blocks.
    // ================================================================
    public static class EmberStorm extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> sky = new ArrayList<>();
        private final List<ItemDisplayHandle> falling = new ArrayList<>();

        public EmberStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ember_storm", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.0);
            config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(220);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 1.2f, 0.4f);
            // Ring of 12 magma blocks floating overhead
            for (int i = 0; i < 12; i++) {
                double angle = Math.PI * 2 * i / 12;
                Location p = center.clone().add(Math.cos(angle) * 6, 10, Math.sin(angle) * 6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.MAGMA_BLOCK);
                h.scale(0.7f, 0.7f, 0.7f).glow(255, 80, 0).interpolation(12, 0);
                sky.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            double radius = config.getDamageRadius();

            // Slowly rotate the overhead ring
            if (tick % 6 == 0) {
                for (int i = 0; i < sky.size(); i++) {
                    double angle = tick * 0.04 + (Math.PI * 2 * i / sky.size());
                    float tx = (float) (Math.cos(angle) * 6);
                    float ty = 10 + (float) Math.sin(tick * 0.08 + i) * 0.6f;
                    float tz = (float) (Math.sin(angle) * 6);
                    sky.get(i).animateTo(
                            new Vector3f(tx - 0.5f, ty, tz - 0.5f),
                            new AxisAngle4f((float) (tick * 0.15), 1, 1, 0),
                            new Vector3f(0.7f, 0.7f, 0.7f), 6);
                }
            }

            // Spawn falling ember item-displays periodically
            if (tick % 10 == 0 && falling.size() < 10) {
                double ox = (Math.random() - 0.5) * radius * 1.5;
                double oz = (Math.random() - 0.5) * radius * 1.5;
                Location drop = getCenter().clone().add(ox, 12, oz);
                ItemDisplayHandle h = displayBuilder.spawnItem(drop, new ItemStack(Material.BLAZE_POWDER));
                h.scale(0.6f, 0.6f, 0.6f).glow(255, 120, 0).interpolation(30, 0);
                // Fall to ground over 30 ticks
                h.animateTo(new Vector3f(-0.5f, -12f, -0.5f),
                        new AxisAngle4f((float) (Math.random() * Math.PI * 2), 0, 1, 0),
                        new Vector3f(0.6f, 0.6f, 0.6f), 30);
                falling.add(h);
            }

            // Dense particle rain
            if (tick % 2 == 0) {
                for (int i = 0; i < 20; i++) {
                    double ox = (Math.random() - 0.5) * radius * 2;
                    double oz = (Math.random() - 0.5) * radius * 2;
                    Location drop = getCenter().clone().add(ox, 6 + Math.random() * 6, oz);
                    w.spawnParticle(Particle.LAVA, drop, 2, 0.3, 0.3, 0.3, 0.05);
                    w.spawnParticle(Particle.FLAME, drop, 1, 0.2, 0.2, 0.2, 0.01);
                }
            }

            if (tick % 18 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_LAVA_AMBIENT, 0.8f, 0.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); sky.clear(); falling.clear(); }
        @Override public AbstractAttack newInstance() { return new EmberStorm(plugin); }
    }

    // ================================================================
    // 3. GROUND TREMOR
    //    Cracking netherrack BlockDisplays erupt up from ground in a grid
    //    with heavy particles + vertical player pulse.
    // ================================================================
    public static class GroundTremor extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> chunks = new ArrayList<>();

        public GroundTremor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ground_tremor", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(4.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(40);
            config.setDurationTicks(140);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.3f);
            if (center.getWorld() != null) {
                center.getWorld().spawnParticle(Particle.BLOCK, center, 60, 6, 0.5, 6, 0.1,
                        Material.NETHERRACK.createBlockData());
            }
            // Pre-spawn 16 jagged chunks along a grid hidden in ground
            for (int i = 0; i < 16; i++) {
                double angle = Math.PI * 2 * i / 16;
                double r = 2 + Math.random() * 5;
                Location p = center.clone().add(Math.cos(angle) * r, -0.5, Math.sin(angle) * r);
                Material mat = (i % 3 == 0) ? Material.MAGMA_BLOCK : Material.NETHERRACK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, mat);
                h.scale(0.01f, 0.01f, 0.01f).interpolation(0, 0);
                if (mat == Material.MAGMA_BLOCK) h.glow(255, 100, 0);
                chunks.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Sequentially erupt chunks up out of the ground
            int eruptIdx = tick / 6;
            if (eruptIdx < chunks.size() && tick % 6 == 0) {
                BlockDisplayHandle h = chunks.get(eruptIdx);
                float sc = 1.1f + (float) Math.random() * 0.6f;
                h.animateTo(new Vector3f(-sc/2f, 0.5f, -sc/2f),
                        new AxisAngle4f((float) (Math.random() * Math.PI), 1, 1, 0),
                        new Vector3f(sc, sc * 1.5f, sc), 10);
            }

            if (tick % 8 == 0) {
                w.spawnParticle(Particle.BLOCK, getCenter(), 30, 5, 0.3, 5, 0.05,
                        Material.NETHERRACK.createBlockData());
                w.spawnParticle(Particle.LAVA, getCenter(), 8, 4, 0.2, 4, 0.02);

                // Pulse players upward slightly
                double radius = config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    if (p.getLocation().distanceSquared(getCenter()) <= radius * radius) {
                        double yPulse = (Math.random() > 0.5) ? 0.15 : -0.1;
                        p.setVelocity(p.getVelocity().add(new Vector(0, yPulse, 0)));
                    }
                }
            }

            if (tick % 25 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_ANVIL_LAND, 0.5f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); chunks.clear(); }
        @Override public AbstractAttack newInstance() { return new GroundTremor(plugin); }
    }

    // ================================================================
    // 4. SULFUR CLOUD
    //    8 orbiting yellow gunpowder item-displays at torso height with
    //    drifting yellow dust and ramped damage.
    // ================================================================
    public static class SulfurCloud extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> cores = new ArrayList<>();

        public SulfurCloud(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sulfur_cloud", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(1.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(220);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_CREEPER_PRIMED, 0.6f, 0.3f);
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * 2 * i / 8;
                Location p = center.clone().add(Math.cos(angle) * 3, 1.5, Math.sin(angle) * 3);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GUNPOWDER));
                h.scale(1.1f, 1.1f, 1.1f).glow(220, 220, 30).interpolation(10, 0);
                cores.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            double progress = Math.min(1.0, (double) tick / config.getDurationTicks());
            config.setDamage(1.0 + progress * 4.0);

            if (tick % 5 == 0) {
                for (int i = 0; i < cores.size(); i++) {
                    double angle = -tick * 0.06 + Math.PI * 2 * i / cores.size();
                    float tx = (float) (Math.cos(angle) * 3);
                    float ty = 1.5f + (float) Math.sin(tick * 0.07 + i) * 0.8f;
                    float tz = (float) (Math.sin(angle) * 3);
                    cores.get(i).animateTo(new Vector3f(tx - 0.5f, ty, tz - 0.5f),
                            new AxisAngle4f((float) (tick * 0.2), 0, 1, 0),
                            new Vector3f(1.1f, 1.1f, 1.1f), 5);
                }
            }

            if (tick % 3 == 0) {
                double radius = config.getDamageRadius();
                for (int i = 0; i < 14; i++) {
                    double ox = (Math.random() - 0.5) * radius * 2;
                    double oy = Math.random() * 3;
                    double oz = (Math.random() - 0.5) * radius * 2;
                    Location loc = getCenter().clone().add(ox, oy, oz);
                    DisplayBuilder.dustParticles(loc, 2, 0.5, 220, 200, 30, 1.8f);
                    w.spawnParticle(Particle.SMALL_FLAME, loc, 1, 0.1, 0.1, 0.1, 0.002);
                }
            }

            if (tick % 25 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_LAVA_EXTINGUISH, 0.5f, 0.4f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); cores.clear(); }
        @Override public AbstractAttack newInstance() { return new SulfurCloud(plugin); }
    }

    // ================================================================
    // 5. MAGMA GAS VENT
    //    Vertical column of BlockDisplay magma chunks shooting up with
    //    dense flame+smoke particle spiral.
    // ================================================================
    public static class MagmaGasVent extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> pillars = new ArrayList<>();

        public MagmaGasVent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_gas_vent", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(5.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(160);
            config.setCooldownTicks(220);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.3f);
            for (int i = 0; i < 10; i++) {
                Location p = center.clone().add(0, i * 0.9, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.MAGMA_BLOCK);
                h.scale(0.45f, 0.45f, 0.45f).glow(255, 80, 0).interpolation(8, 0);
                pillars.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Spiral each magma block
            if (tick % 3 == 0) {
                for (int i = 0; i < pillars.size(); i++) {
                    double angle = tick * 0.15 + i * 0.6;
                    float tx = (float) (Math.cos(angle) * 0.6);
                    float ty = i * 0.9f + (float) (Math.sin(tick * 0.2 + i) * 0.25);
                    float tz = (float) (Math.sin(angle) * 0.6);
                    pillars.get(i).animateTo(new Vector3f(tx - 0.225f, ty, tz - 0.225f),
                            new AxisAngle4f((float) (tick * 0.2), 1, 0, 1),
                            new Vector3f(0.45f, 0.45f, 0.45f), 3);
                }
            }

            if (tick % 2 == 0) {
                for (int y = 0; y < 12; y++) {
                    Location loc = getCenter().clone().add(
                            (Math.random() - 0.5) * 1.0, y * 0.8, (Math.random() - 0.5) * 1.0);
                    w.spawnParticle(Particle.FLAME, loc, 3, 0.2, 0.15, 0.2, 0.03);
                    w.spawnParticle(Particle.SMOKE, loc, 1, 0.25, 0.15, 0.25, 0.02);
                    if (y % 2 == 0) {
                        DisplayBuilder.dustParticles(loc, 1, 0.3, 255, 90, 0, 1.2f);
                    }
                }
            }

            if (tick % 18 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_BLAZE_AMBIENT, 0.7f, 0.4f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); pillars.clear(); }
        @Override public AbstractAttack newInstance() { return new MagmaGasVent(plugin); }
    }

    // ================================================================
    // 6. LAVA DROPLETS
    //    Continuously spawning falling magma-cream ItemDisplay droplets.
    // ================================================================
    public static class LavaDroplets extends EnvironmentalAttack {
        public LavaDroplets(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lava_droplets", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(200);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 0.9f, 0.6f);
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            double radius = config.getDamageRadius();

            // Continuously spawn droplet item displays that fall with interpolation
            if (tick % 6 == 0) {
                for (int i = 0; i < 4; i++) {
                    double ox = (Math.random() - 0.5) * radius * 2;
                    double oz = (Math.random() - 0.5) * radius * 2;
                    Location start = getCenter().clone().add(ox, 10, oz);
                    ItemDisplayHandle drop = displayBuilder.spawnItem(start, new ItemStack(Material.MAGMA_CREAM));
                    drop.scale(0.35f, 0.35f, 0.35f).glow(255, 100, 0).interpolation(20, 0);
                    drop.animateTo(new Vector3f(-0.175f, -10f, -0.175f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.35f, 0.5f, 0.35f), 20);
                }
            }

            if (tick % 2 == 0) {
                for (int i = 0; i < 10; i++) {
                    double ox = (Math.random() - 0.5) * radius * 2;
                    double oz = (Math.random() - 0.5) * radius * 2;
                    Location dropLoc = getCenter().clone().add(ox, 10, oz);
                    w.spawnParticle(Particle.DRIPPING_LAVA, dropLoc, 3, 0.5, 0.2, 0.5, 0);
                }
            }

            if (tick % 8 == 0) {
                w.spawnParticle(Particle.LAVA, getCenter(), 5, radius * 0.5, 0.1, radius * 0.5, 0.01);
                w.spawnParticle(Particle.LANDING_LAVA, getCenter(), 3, radius * 0.5, 0.1, radius * 0.5, 0);
            }

            if (tick % 30 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_LAVA_POP, 0.6f, 0.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new LavaDroplets(plugin); }
    }

    // ================================================================
    // 7. SCORCHING WIND
    //    Directional stream of 8 flame-banner item displays flying past
    //    along a wind vector + player knockback.
    // ================================================================
    public static class ScorchingWind extends EnvironmentalAttack {
        private double windDirX, windDirZ;
        private final List<ItemDisplayHandle> wisps = new ArrayList<>();

        public ScorchingWind(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("scorching_wind", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.0);
            config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(180);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            double angle = Math.random() * 2 * Math.PI;
            windDirX = Math.cos(angle);
            windDirZ = Math.sin(angle);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.3f);

            for (int i = 0; i < 8; i++) {
                Location p = center.clone().add(-windDirX * 5, 1 + i * 0.3, -windDirZ * 5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.FIRE_CHARGE));
                h.scale(0.8f, 0.8f, 0.8f).glow(255, 140, 0).interpolation(20, 0);
                wisps.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Recycle wisps: each flows in wind direction and re-spawns upstream when it passes through
            if (tick % 20 == 0) {
                for (int i = 0; i < wisps.size(); i++) {
                    float baseTY = 1 + i * 0.3f;
                    // Animate from upstream to downstream over 60 ticks
                    float tx = (float) (windDirX * 10);
                    float tz = (float) (windDirZ * 10);
                    wisps.get(i).animateTo(new Vector3f(tx - 0.4f, baseTY, tz - 0.4f),
                            new AxisAngle4f((float) (tick * 0.3), 0, 1, 0),
                            new Vector3f(0.8f, 0.8f, 0.8f), 20);
                }
            }

            if (tick % 2 == 0) {
                for (int i = 0; i < 20; i++) {
                    double dist = i * 0.6;
                    Location loc = getCenter().clone().add(
                            windDirX * dist + (Math.random() - 0.5) * 2,
                            0.5 + Math.random() * 2.5,
                            windDirZ * dist + (Math.random() - 0.5) * 2);
                    w.spawnParticle(Particle.SMOKE, loc, 2, 0.3, 0.2, 0.3, 0.02);
                    w.spawnParticle(Particle.FLAME, loc, 1, 0.15, 0.15, 0.15, 0.015);
                    if (i % 3 == 0) DisplayBuilder.dustParticles(loc, 1, 0.3, 255, 120, 20, 1.2f);
                }
            }

            if (tick % 8 == 0) {
                double radius = config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    if (p.getLocation().distanceSquared(getCenter()) <= radius * radius) {
                        p.setVelocity(p.getVelocity().add(new Vector(windDirX * 0.35, 0.05, windDirZ * 0.35)));
                    }
                }
            }

            if (tick % 22 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_PHANTOM_FLAP, 0.7f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); wisps.clear(); }
        @Override public AbstractAttack newInstance() { return new ScorchingWind(plugin); }
    }

    // ================================================================
    // 8. MOLTEN GROUND PATCH
    //    Rotating ring of 16 magma-block displays at ground level with
    //    dense surface particles + glowing center ember.
    // ================================================================
    public static class MoltenGroundPatch extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> ring = new ArrayList<>();
        private ItemDisplayHandle centerOrb;

        public MoltenGroundPatch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("molten_ground_patch", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(4.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(200);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 1.0f, 0.5f);
            if (center.getWorld() != null) {
                center.getWorld().spawnParticle(Particle.LAVA, center, 40, 3, 0.2, 3, 0.05);
            }
            double r = config.getDamageRadius();
            for (int i = 0; i < 16; i++) {
                double angle = Math.PI * 2 * i / 16;
                Location p = center.clone().add(Math.cos(angle) * r, 0.1, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.MAGMA_BLOCK);
                h.scale(0.5f, 0.25f, 0.5f).glow(255, 100, 0).interpolation(8, 0);
                ring.add(h);
            }
            centerOrb = displayBuilder.spawnItem(center.clone().add(0, 0.3, 0),
                    new ItemStack(Material.FIRE_CHARGE));
            centerOrb.scale(1.5f, 1.5f, 1.5f).glow(255, 80, 0).interpolation(10, 0);
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            double radius = config.getDamageRadius();

            if (tick % 4 == 0) {
                for (int i = 0; i < ring.size(); i++) {
                    double angle = tick * 0.05 + Math.PI * 2 * i / ring.size();
                    float tx = (float) (Math.cos(angle) * radius);
                    float ty = 0.1f + (float) Math.sin(tick * 0.15 + i) * 0.15f;
                    float tz = (float) (Math.sin(angle) * radius);
                    ring.get(i).animateTo(new Vector3f(tx - 0.25f, ty, tz - 0.25f),
                            new AxisAngle4f((float) (tick * 0.1), 0, 1, 0),
                            new Vector3f(0.5f, 0.25f, 0.5f), 4);
                }
                if (centerOrb != null) {
                    float s = 1.5f + (float) Math.sin(tick * 0.15) * 0.3f;
                    centerOrb.animateTo(new Vector3f(-s / 2f, 0.3f, -s / 2f),
                            new AxisAngle4f((float) (tick * 0.2), 0, 1, 0),
                            new Vector3f(s, s, s), 4);
                }
            }

            if (tick % 3 == 0) {
                int points = 24;
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points;
                    double rr = Math.random() * radius;
                    Location loc = getCenter().clone().add(Math.cos(angle) * rr, 0.1, Math.sin(angle) * rr);
                    w.spawnParticle(Particle.LANDING_LAVA, loc, 1, 0.3, 0.05, 0.3, 0);
                    w.spawnParticle(Particle.FLAME, loc, 1, 0.2, 0.05, 0.2, 0.01);
                }
                DisplayBuilder.dustParticles(getCenter().clone().add(0, 0.3, 0),
                        3, radius * 0.8, 255, 80, 0, 1.5f);
            }

            if (tick % 25 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_LAVA_POP, 0.7f, 0.4f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); ring.clear(); centerOrb = null; }
        @Override public AbstractAttack newInstance() { return new MoltenGroundPatch(plugin); }
    }

    // ================================================================
    // 9. INFERNAL WHISPERS
    //    5 orbiting skull item displays at head height with sculk soul
    //    particles and whisper sounds.
    // ================================================================
    public static class InfernalWhispers extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> skulls = new ArrayList<>();

        public InfernalWhispers(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_whispers", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(30);
            config.setDurationTicks(220);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_VEX_AMBIENT, 0.8f, 0.3f);
            for (int i = 0; i < 5; i++) {
                double angle = Math.PI * 2 * i / 5;
                Location p = center.clone().add(Math.cos(angle) * 3, 2, Math.sin(angle) * 3);
                ItemDisplayHandle h = displayBuilder.spawnItem(p,
                        new ItemStack(Material.WITHER_SKELETON_SKULL));
                h.scale(1.0f, 1.0f, 1.0f).glow(80, 0, 160).interpolation(10, 0);
                skulls.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            if (tick % 5 == 0) {
                for (int i = 0; i < skulls.size(); i++) {
                    double angle = tick * 0.05 + Math.PI * 2 * i / skulls.size();
                    double r = 3 + Math.sin(tick * 0.04 + i) * 0.8;
                    float tx = (float) (Math.cos(angle) * r);
                    float ty = 2 + (float) Math.sin(tick * 0.08 + i * 1.1) * 0.8f;
                    float tz = (float) (Math.sin(angle) * r);
                    skulls.get(i).animateTo(new Vector3f(tx - 0.5f, ty, tz - 0.5f),
                            new AxisAngle4f((float) (tick * 0.08), 0, 1, 0),
                            new Vector3f(1.0f, 1.0f, 1.0f), 5);
                }
            }

            if (tick % 4 == 0) {
                double radius = config.getDamageRadius();
                for (int i = 0; i < 10; i++) {
                    double ox = (Math.random() - 0.5) * radius * 2;
                    double oy = Math.random() * 3;
                    double oz = (Math.random() - 0.5) * radius * 2;
                    Location loc = getCenter().clone().add(ox, oy, oz);
                    w.spawnParticle(Particle.SCULK_SOUL, loc, 1, 0.3, 0.3, 0.3, 0.02);
                    if (Math.random() < 0.3) {
                        DisplayBuilder.dustParticles(loc, 1, 0.2, 80, 0, 160, 1.5f);
                    }
                }
            }

            if (tick % 20 == 0) {
                float pitch = 0.2f + (float) (Math.random() * 0.3);
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_VEX_AMBIENT, 0.6f, pitch);
            }
            if (tick % 35 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_PHANTOM_AMBIENT, 0.4f, 0.2f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); skulls.clear(); }
        @Override public AbstractAttack newInstance() { return new InfernalWhispers(plugin); }
    }

    // ================================================================
    // 10. ASHFALL BLANKET
    //     Continuous falling ItemDisplay ash flakes + gray dust cloud.
    // ================================================================
    public static class AshfallBlanket extends EnvironmentalAttack {
        public AshfallBlanket(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ashfall_blanket", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(1.0);
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(260);
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
            double radius = config.getDamageRadius();

            // Spawn ash flake item displays that fall slowly
            if (tick % 8 == 0) {
                for (int i = 0; i < 3; i++) {
                    double ox = (Math.random() - 0.5) * radius * 2;
                    double oz = (Math.random() - 0.5) * radius * 2;
                    Location start = getCenter().clone().add(ox, 14, oz);
                    ItemDisplayHandle flake = displayBuilder.spawnItem(start,
                            new ItemStack(Material.PAPER));
                    flake.scale(0.3f, 0.3f, 0.3f).glow(140, 140, 140).interpolation(80, 0);
                    flake.animateTo(new Vector3f(
                                    (float) ((Math.random() - 0.5) * 3) - 0.15f,
                                    -14f,
                                    (float) ((Math.random() - 0.5) * 3) - 0.15f),
                            new AxisAngle4f((float) (Math.random() * Math.PI * 4), 0, 1, 0),
                            new Vector3f(0.3f, 0.3f, 0.3f), 80);
                }
            }

            if (tick % 2 == 0) {
                for (int i = 0; i < 20; i++) {
                    double ox = (Math.random() - 0.5) * radius * 2;
                    double oz = (Math.random() - 0.5) * radius * 2;
                    double oy = 6 + Math.random() * 8;
                    Location loc = getCenter().clone().add(ox, oy, oz);
                    w.spawnParticle(Particle.SMOKE, loc, 1, 0.5, 0.3, 0.5, 0.005);
                    DisplayBuilder.dustParticles(loc, 1, 0.4, 120, 120, 120, 1.2f);
                }
            }

            if (tick % 40 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.WEATHER_RAIN, 0.3f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new AshfallBlanket(plugin); }
    }

    // ================================================================
    // 11. NETHER QUAKE
    //     12 basalt/magma chunks erupt upward with explosion arcs + strong
    //     Y+ pulse + heavy particle burst.
    // ================================================================
    public static class NetherQuake extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> debris = new ArrayList<>();

        public NetherQuake(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nether_quake", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(6.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(60);
            config.setDurationTicks(140);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            if (center.getWorld() == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_ROAR, 1.0f, 0.3f);
            center.getWorld().spawnParticle(Particle.BLOCK, center, 80, 6, 0.5, 6, 0.1,
                    Material.MAGMA_BLOCK.createBlockData());
            for (int i = 0; i < 12; i++) {
                double angle = Math.PI * 2 * i / 12;
                double r = 1.5 + Math.random() * 4;
                Location p = center.clone().add(Math.cos(angle) * r, 0.2, Math.sin(angle) * r);
                Material mat = (i % 2 == 0) ? Material.BASALT : Material.MAGMA_BLOCK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, mat);
                h.scale(0.9f, 0.9f, 0.9f).interpolation(15, 0);
                if (mat == Material.MAGMA_BLOCK) h.glow(255, 80, 0);
                debris.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            if (tick % 20 == 0) {
                // Re-launch all debris chunks in an eruption arc
                for (int i = 0; i < debris.size(); i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double r = 1.5 + Math.random() * 5;
                    float tx = (float) (Math.cos(angle) * r);
                    float ty = 2 + (float) Math.random() * 4;
                    float tz = (float) (Math.sin(angle) * r);
                    debris.get(i).animateTo(new Vector3f(tx - 0.45f, ty, tz - 0.45f),
                            new AxisAngle4f((float) (Math.random() * Math.PI * 2), 1, 1, 0),
                            new Vector3f(0.9f, 0.9f, 0.9f), 20);
                }

                w.spawnParticle(Particle.BLOCK, getCenter(), 60, 5, 0.3, 5, 0.1,
                        Material.NETHERRACK.createBlockData());
                w.spawnParticle(Particle.BLOCK, getCenter(), 30, 4, 0.2, 4, 0.05,
                        Material.MAGMA_BLOCK.createBlockData());
                w.spawnParticle(Particle.LAVA, getCenter(), 15, 3, 0.5, 3, 0.05);

                double radius = config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    if (p.getLocation().distanceSquared(getCenter()) <= radius * radius) {
                        p.setVelocity(p.getVelocity().add(new Vector(0, 0.5, 0)));
                    }
                }

                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.3f);
            }

            if (tick % 4 == 0) {
                double r2 = config.getDamageRadius();
                DisplayBuilder.dustParticles(getCenter().clone().add(0, 0.3, 0),
                        4, r2 * 0.8, 255, 60, 0, 1.8f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); debris.clear(); }
        @Override public AbstractAttack newInstance() { return new NetherQuake(plugin); }
    }

    // ================================================================
    // 12. FIRE WHIRL
    //     Spinning FLAME tornado with 10 orbiting flame-charge item
    //     displays in a double helix + thick dust spiral.
    // ================================================================
    public static class FireWhirl extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> helix = new ArrayList<>();

        public FireWhirl(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fire_whirl", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(10);
            config.setTracksPlayer(true);
            config.setDurationTicks(200);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f);
            for (int i = 0; i < 10; i++) {
                Location p = center.clone().add(0, 0.5 + i * 0.5, 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.FIRE_CHARGE));
                h.scale(0.7f, 0.7f, 0.7f).glow(255, 140, 0).interpolation(4, 0);
                helix.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            if (tick % 2 == 0) {
                for (int i = 0; i < helix.size(); i++) {
                    double angle = tick * 0.35 + i * 0.9;
                    double r = 0.6 + i * 0.15;
                    float tx = (float) (Math.cos(angle) * r);
                    float ty = 0.5f + i * 0.5f;
                    float tz = (float) (Math.sin(angle) * r);
                    helix.get(i).animateTo(new Vector3f(tx - 0.35f, ty, tz - 0.35f),
                            new AxisAngle4f((float) (tick * 0.4), 0, 1, 0),
                            new Vector3f(0.7f, 0.7f, 0.7f), 2);
                }

                // Dense particle spiral
                for (int y = 0; y < 10; y++) {
                    double radius = 0.6 + y * 0.45;
                    double angle = tick * 0.35 + y * 0.9;
                    double px = Math.cos(angle) * radius;
                    double pz = Math.sin(angle) * radius;
                    Location loc = getCenter().clone().add(px, y * 0.55, pz);
                    w.spawnParticle(Particle.FLAME, loc, 3, 0.1, 0.1, 0.1, 0.015);
                    double px2 = Math.cos(angle + Math.PI) * radius * 0.8;
                    double pz2 = Math.sin(angle + Math.PI) * radius * 0.8;
                    Location loc2 = getCenter().clone().add(px2, y * 0.55, pz2);
                    w.spawnParticle(Particle.SMOKE, loc2, 2, 0.1, 0.1, 0.1, 0.01);
                    DisplayBuilder.dustParticles(loc, 1, 0.1, 255, 100, 0, 1.2f);
                }
            }

            if (tick % 16 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_BLAZE_AMBIENT, 0.7f, 0.6f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); helix.clear(); }
        @Override public AbstractAttack newInstance() { return new FireWhirl(plugin); }
    }

    // ================================================================
    // 13. LAVABURST GEYSER
    //     Erupting BlockDisplay lava column + arcing magma chunks shooting
    //     outward in 8 directions.
    // ================================================================
    public static class LavaburstGeyser extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> column = new ArrayList<>();
        private final List<BlockDisplayHandle> arcs = new ArrayList<>();

        public LavaburstGeyser(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lavaburst_geyser", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(120);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            if (center.getWorld() == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.3f, 0.4f);
            center.getWorld().spawnParticle(Particle.LAVA, center, 60, 1.5, 4.0, 1.5, 0.15);
            center.getWorld().spawnParticle(Particle.FLAME, center, 50, 1.2, 4.5, 1.2, 0.08);

            // Central erupting column — 8 magma blocks
            for (int i = 0; i < 8; i++) {
                Location p = center.clone().add(0, 0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.MAGMA_BLOCK);
                h.scale(0.6f, 0.6f, 0.6f).glow(255, 80, 0).interpolation(10, 0);
                h.animateTo(new Vector3f(-0.3f, i * 1.0f, -0.3f),
                        new AxisAngle4f((float) (Math.random() * Math.PI), 1, 0, 1),
                        new Vector3f(0.6f, 0.6f, 0.6f), 10);
                column.add(h);
            }

            // 8 arcing chunks shooting outward
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * 2 * i / 8;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, 1, 0),
                        Material.NETHERRACK);
                h.scale(0.5f, 0.5f, 0.5f).interpolation(40, 0);
                float tx = (float) (Math.cos(angle) * 5);
                float ty = 4;
                float tz = (float) (Math.sin(angle) * 5);
                h.animateTo(new Vector3f(tx - 0.25f, ty, tz - 0.25f),
                        new AxisAngle4f((float) Math.PI * 2, 1, 1, 0),
                        new Vector3f(0.5f, 0.5f, 0.5f), 40);
                arcs.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            if (tick % 3 == 0) {
                int height = 8 + (int) (Math.sin(tick * 0.15) * 3);
                for (int y = 0; y < height; y++) {
                    Location loc = getCenter().clone().add(
                            (Math.random() - 0.5) * 1.2, y * 0.9, (Math.random() - 0.5) * 1.2);
                    w.spawnParticle(Particle.LAVA, loc, 3, 0.25, 0.25, 0.25, 0.06);
                    w.spawnParticle(Particle.FLAME, loc, 3, 0.18, 0.18, 0.18, 0.025);
                }
            }

            if (tick % 6 == 0) {
                w.spawnParticle(Particle.LANDING_LAVA, getCenter(), 8, 2.5, 0.1, 2.5, 0);
                DisplayBuilder.dustParticles(getCenter().clone().add(0, 1, 0),
                        4, 2, 255, 80, 0, 1.6f);
            }

            // Arcs fall back after their up-arc
            if (tick == 40) {
                for (BlockDisplayHandle h : arcs) {
                    double angle = Math.random() * Math.PI * 2;
                    float tx = (float) (Math.cos(angle) * 7);
                    float tz = (float) (Math.sin(angle) * 7);
                    h.animateTo(new Vector3f(tx - 0.25f, -1f, tz - 0.25f),
                            new AxisAngle4f((float) Math.PI * 4, 1, 0, 1),
                            new Vector3f(0.5f, 0.5f, 0.5f), 30);
                }
            }

            if (tick % 18 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_LAVA_POP, 0.9f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); column.clear(); arcs.clear(); }
        @Override public AbstractAttack newInstance() { return new LavaburstGeyser(plugin); }
    }

    // ================================================================
    // 14. VOLCANIC ASH CLOUD
    //     Overhead rotating ring of 10 basalt block displays at Y+10 with
    //     large_smoke + ash particle rain.
    // ================================================================
    public static class VolcanicAshCloud extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> cloudRing = new ArrayList<>();

        public VolcanicAshCloud(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("volcanic_ash_cloud", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2.0);
            config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(240);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 0.2f);
            for (int i = 0; i < 10; i++) {
                double angle = Math.PI * 2 * i / 10;
                Location p = center.clone().add(Math.cos(angle) * 7, 10, Math.sin(angle) * 7);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BASALT);
                h.scale(1.2f, 0.8f, 1.2f).glow(60, 60, 60).interpolation(18, 0);
                cloudRing.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            double radius = config.getDamageRadius();

            if (tick % 8 == 0) {
                for (int i = 0; i < cloudRing.size(); i++) {
                    double angle = tick * 0.03 + Math.PI * 2 * i / cloudRing.size();
                    float tx = (float) (Math.cos(angle) * 7);
                    float ty = 10 + (float) Math.sin(tick * 0.05 + i) * 0.8f;
                    float tz = (float) (Math.sin(angle) * 7);
                    cloudRing.get(i).animateTo(new Vector3f(tx - 0.6f, ty, tz - 0.4f),
                            new AxisAngle4f((float) (tick * 0.06), 0, 1, 0),
                            new Vector3f(1.2f, 0.8f, 1.2f), 8);
                }
            }

            if (tick % 2 == 0) {
                for (int i = 0; i < 16; i++) {
                    double ox = (Math.random() - 0.5) * radius * 2;
                    double oz = (Math.random() - 0.5) * radius * 2;
                    double oy = 7 + Math.random() * 6;
                    Location loc = getCenter().clone().add(ox, oy, oz);
                    w.spawnParticle(Particle.LARGE_SMOKE, loc, 1, 1.2, 0.3, 1.2, 0.008);
                }
                for (int i = 0; i < 6; i++) {
                    double ox = (Math.random() - 0.5) * radius * 1.5;
                    double oz = (Math.random() - 0.5) * radius * 1.5;
                    Location fallLoc = getCenter().clone().add(ox, 6, oz);
                    DisplayBuilder.dustParticles(fallLoc, 2, 0.4, 110, 110, 110, 1.2f);
                }
            }

            if (tick % 35 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.WEATHER_RAIN, 0.5f, 0.2f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); cloudRing.clear(); }
        @Override public AbstractAttack newInstance() { return new VolcanicAshCloud(plugin); }
    }

    // ================================================================
    // 15. HELLSTORM LIGHTNING
    //     CONTINUOUS lightning ring — 12 fixed strike positions around a
    //     ring at damage radius; every 4 ticks 2-3 random positions
    //     instantly strike. Overhead storm cloud (6 basalt blocks) rotates
    //     slowly and emits crackle particles. Center glow orb item display
    //     pulses red. Impact damage on each strike position hit.
    // ================================================================
    public static class HellstormLightning extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> stormCloud = new ArrayList<>();
        private ItemDisplayHandle centerOrb;
        private static final int RING_POINTS = 12;

        public HellstormLightning(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellstorm_lightning", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(10.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(3.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.6f, 0.5f);

            // Overhead storm cloud — 6 basalt blocks at Y+12
            for (int i = 0; i < 6; i++) {
                double angle = Math.PI * 2 * i / 6;
                Location p = center.clone().add(Math.cos(angle) * 3, 12, Math.sin(angle) * 3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BASALT);
                h.scale(1.5f, 0.7f, 1.5f).glow(80, 80, 80).interpolation(16, 0);
                stormCloud.add(h);
            }

            // Center glow orb — red glowstone dust item display
            centerOrb = displayBuilder.spawnItem(center.clone().add(0, 2.5, 0),
                    new ItemStack(Material.GLOWSTONE_DUST));
            centerOrb.scale(1.8f, 1.8f, 1.8f).glow(255, 50, 50).interpolation(8, 0);
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            double radius = config.getDamageRadius();

            // Overhead storm rotation
            if (tick % 8 == 0) {
                for (int i = 0; i < stormCloud.size(); i++) {
                    double angle = tick * 0.025 + Math.PI * 2 * i / stormCloud.size();
                    float tx = (float) (Math.cos(angle) * 3);
                    float ty = 12 + (float) Math.sin(tick * 0.06 + i) * 0.5f;
                    float tz = (float) (Math.sin(angle) * 3);
                    stormCloud.get(i).animateTo(new Vector3f(tx - 0.75f, ty, tz - 0.35f),
                            new AxisAngle4f((float) (tick * 0.08), 0, 1, 0),
                            new Vector3f(1.5f, 0.7f, 1.5f), 8);
                }
            }

            // Center orb pulse
            if (tick % 4 == 0 && centerOrb != null) {
                float s = 1.8f + (float) Math.sin(tick * 0.2) * 0.4f;
                centerOrb.animateTo(new Vector3f(-s / 2f, 2.5f, -s / 2f),
                        new AxisAngle4f((float) (tick * 0.3), 0, 1, 0),
                        new Vector3f(s, s, s), 4);
            }

            // CONTINUOUS ring visualization — 12 dust markers at strike positions
            if (tick % 2 == 0) {
                for (int i = 0; i < RING_POINTS; i++) {
                    double angle = Math.PI * 2 * i / RING_POINTS;
                    double px = Math.cos(angle) * radius;
                    double pz = Math.sin(angle) * radius;
                    Location ringLoc = getCenter().clone().add(px, 0.2, pz);
                    DisplayBuilder.dustParticles(ringLoc, 2, 0.15, 255, 200, 255, 1.4f);
                    // Vertical tracer line low to the ground
                    for (int y = 0; y < 3; y++) {
                        DisplayBuilder.dustParticles(ringLoc.clone().add(0, y * 0.8, 0),
                                1, 0.1, 180, 180, 255, 1.0f);
                    }
                }
            }

            // NONSTOP strikes — every 4 ticks, pick 2 random ring positions and strike
            if (tick % 4 == 0) {
                int strikesThisPhase = 2 + (int) (Math.random() * 2); // 2-3 per phase
                for (int s = 0; s < strikesThisPhase; s++) {
                    int ringIdx = (int) (Math.random() * RING_POINTS);
                    double angle = Math.PI * 2 * ringIdx / RING_POINTS
                            + (Math.random() - 0.5) * (Math.PI / RING_POINTS);
                    double px = Math.cos(angle) * radius;
                    double pz = Math.sin(angle) * radius;
                    Location strikeLoc = getCenter().clone().add(px, 0, pz);

                    // Tall white lightning bolt from Y+12 to ground
                    for (int y = 0; y < 14; y++) {
                        double jx = (Math.random() - 0.5) * 0.4;
                        double jz = (Math.random() - 0.5) * 0.4;
                        Location lineLoc = strikeLoc.clone().add(jx, y, jz);
                        DisplayBuilder.dustParticles(lineLoc, 2, 0.05, 255, 255, 255, 2.0f);
                        DisplayBuilder.dustParticles(lineLoc, 1, 0.2, 255, 180, 80, 1.3f);
                    }

                    // Impact burst
                    w.spawnParticle(Particle.FLAME, strikeLoc, 18, 1.2, 0.3, 1.2, 0.04);
                    w.spawnParticle(Particle.SMOKE, strikeLoc, 10, 0.8, 0.4, 0.8, 0.02);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, strikeLoc, 20, 1.0, 1.0, 1.0, 0.4);
                    w.spawnParticle(Particle.LAVA, strikeLoc, 4, 0.6, 0.2, 0.6, 0.05);

                    DisplayBuilder.playSound(strikeLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.1f,
                            0.7f + (float) Math.random() * 0.3f);
                    triggerImpactDamage(strikeLoc);
                }
            }

            // Low rumble
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 0.3f);
            }

            // Periodic center dome flash
            if (tick % 18 == 0) {
                w.spawnParticle(Particle.FLASH, getCenter().clone().add(0, 2, 0), 1, 0, 0, 0, 0);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); stormCloud.clear(); centerOrb = null; }
        @Override public AbstractAttack newInstance() { return new HellstormLightning(plugin); }
    }
}
