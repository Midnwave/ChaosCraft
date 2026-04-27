package com.blockforge.chaoscraft.modes.devilsdream.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.ItemDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * DevilsDream Mode — ENVIRONMENTAL ATTACKS 41-50.
 * Pure particle + ItemDisplay attacks. NO BlockDisplays.
 * Plague Wind, Architect's Nightmare, Celestial Wound, Drowning Room,
 * Cursed Candelabra Array, Devil's Exhale, Petrified Congregation,
 * Temporal Shatter, Lullaby Machine, The Last Dream.
 */
public final class DDEnvFX5 {
    private DDEnvFX5() {}

    private static final String MODE_PATH = "modes/devilsdream/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new PlagueWind(plugin));
        registry.register(new ArchitectsNightmare(plugin));
        registry.register(new CelestialWound(plugin));
        registry.register(new DrowningRoom(plugin));
        registry.register(new CursedCandelabraArray(plugin));
        registry.register(new DevilsExhale(plugin));
        registry.register(new PetrifiedCongregation(plugin));
        registry.register(new TemporalShatter(plugin));
        registry.register(new LullabyMachine(plugin));
        registry.register(new TheLastDream(plugin));
    }

    // ================================================================
    // 41. THE PLAGUE WIND
    //     Horizontal directional wind: warped_spore + ash blast.
    //     Ground item-displays (dead bushes, ferns) blow with the wind.
    // ================================================================
    public static class PlagueWind extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> debris = new ArrayList<>();
        private double windAngle = 0;

        public PlagueWind(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("plague_wind", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(6.0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(6);
            config.setDamageDelayTicks(15);
            config.setDurationTicks(300);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            windAngle = Math.random() * Math.PI * 2;
            DisplayBuilder.playSound(center, Sound.ENTITY_GHAST_AMBIENT, 1.4f, 0.4f);
            Material[] palette = { Material.DEAD_BUSH, Material.FERN, Material.KELP, Material.GLOW_LICHEN };
            for (int i = 0; i < 14; i++) {
                double a = Math.PI * 2 * i / 14 + Math.random() * 0.4;
                double r = 2 + Math.random() * 5;
                Location p = center.clone().add(Math.cos(a) * r, 0.05, Math.sin(a) * r);
                Material mat = palette[i % palette.length];
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(mat));
                h.scale(0.7f, 0.7f, 0.7f).glow(120, 140, 60).interpolation(20, 0);
                debris.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            double radius = config.getDamageRadius();

            int phase = (tick < 50) ? 0 : (tick < 220) ? 1 : 2;
            double dx = Math.cos(windAngle);
            double dz = Math.sin(windAngle);

            if (phase == 0) {
                if (tick % 4 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double ox = (Math.random() - 0.5) * radius * 2;
                        double oz = (Math.random() - 0.5) * radius * 2;
                        Location p = getCenter().clone().add(ox, 1 + Math.random() * 2, oz);
                        w.spawnParticle(Particle.WARPED_SPORE, p, 2, 0.2, 0.2, 0.2, 0.02);
                    }
                }
                if (tick == 30) DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GHAST_WARN, 1.2f, 0.5f);
            } else if (phase == 1) {
                for (int i = 0; i < 32; i++) {
                    double off = (Math.random() - 0.5) * radius * 2;
                    double px = -dz * off;
                    double pz = dx * off;
                    double sx = (Math.random() - 0.5) * radius * 0.8 * dx;
                    double sz = (Math.random() - 0.5) * radius * 0.8 * dz;
                    double y = 0.5 + Math.random() * 3.5;
                    Location p = getCenter().clone().add(px + sx - dx * radius, y, pz + sz - dz * radius);
                    w.spawnParticle(Particle.WARPED_SPORE, p, 2, dx * 0.6, 0.0, dz * 0.6, 1.6);
                    w.spawnParticle(Particle.ASH, p, 2, dx * 0.5, 0.05, dz * 0.5, 1.4);
                    w.spawnParticle(Particle.LARGE_SMOKE, p, 1, dx * 0.4, 0.0, dz * 0.4, 1.0);
                    if (i % 5 == 0)
                        DisplayBuilder.dustParticles(p, 1, 0.2, 176, 160, 32, 1.4f);
                }
                if (tick % 20 == 0) {
                    for (int i = 0; i < debris.size(); i++) {
                        float tx = (float) (dx * 0.6 * ((tick - 50) / 20.0));
                        float tz = (float) (dz * 0.6 * ((tick - 50) / 20.0));
                        debris.get(i).animateTo(
                                new Vector3f(tx - 0.35f + (i % 4) * 0.1f, 0f, tz - 0.35f + (i % 3) * 0.1f),
                                new AxisAngle4f((float) (windAngle + Math.sin(tick * 0.05 + i) * 0.4), 0, 1, 0),
                                new Vector3f(0.7f, 0.7f, 0.7f), 20);
                    }
                }
                if (tick % 18 == 0)
                    DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GHAST_AMBIENT, 1.6f, 0.4f);
                if (tick % 35 == 0)
                    DisplayBuilder.playSound(getCenter(), Sound.BLOCK_DEEPSLATE_BREAK, 0.7f, 0.5f);
            } else {
                if (tick % 3 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double ox = (Math.random() - 0.5) * radius * 2;
                        double oz = (Math.random() - 0.5) * radius * 2;
                        Location p = getCenter().clone().add(ox, 1 + Math.random() * 2, oz);
                        w.spawnParticle(Particle.ASH, p, 1, dx * 0.1, 0, dz * 0.1, 0.1);
                    }
                }
            }

            if (tick % config.getTicksBetweenDamage() == 0 && tick >= config.getDamageDelayTicks()) {
                double r2 = radius * radius;
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(getCenter()) <= r2) {
                        p.damage(config.getDamage()); p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new PlagueWind(plugin); }
    }

    // ================================================================
    // 42. THE ARCHITECT'S NIGHTMARE
    //     Half-built crooked structures: ItemDisplays of bricks/stairs/doors
    //     at wrong angles. Construction smoke. Phasing in/out.
    // ================================================================
    public static class ArchitectsNightmare extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> structure = new ArrayList<>();
        private final List<ItemDisplayHandle> supplies = new ArrayList<>();

        public ArchitectsNightmare(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("architects_nightmare", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.75);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(800);
            config.setCooldownTicks(900);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 0.8f, 0.3f);
            Material[] palette = { Material.POLISHED_BLACKSTONE_BRICK_STAIRS, Material.DEEPSLATE_BRICK_WALL,
                    Material.DEEPSLATE_TILE_STAIRS, Material.NETHER_BRICK_FENCE, Material.IRON_DOOR,
                    Material.DARK_OAK_DOOR, Material.IRON_BARS };

            for (int i = 0; i < 16; i++) {
                double a = Math.PI * 2 * i / 16;
                double r = 4 + Math.random() * 4;
                Location p = center.clone().add(Math.cos(a) * r, 0.5 + Math.random() * 3, Math.sin(a) * r);
                Material m = palette[i % palette.length];
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(m));
                float scale = 0.9f + (float) Math.random() * 0.8f;
                float tilt = (float) (Math.random() * 0.6 - 0.3);
                h.scale(scale, scale * (0.6f + (float) Math.random() * 1.2f), scale)
                        .rotate(tilt, 0, 0, 1)
                        .rotate((float) (Math.random() * 0.4), 1, 0, 0)
                        .glow(60, 60, 60)
                        .interpolation(20, 0);
                structure.add(h);
            }

            Material[] items = { Material.COBBLESTONE, Material.STONE_BRICKS, Material.COBBLED_DEEPSLATE,
                    Material.BLACKSTONE, Material.FLINT, Material.IRON_INGOT };
            for (int i = 0; i < 10; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = 1 + Math.random() * 5;
                Location p = center.clone().add(Math.cos(a) * r, 0.1, Math.sin(a) * r);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(items[i % items.length]));
                h.scale(0.5f, 0.5f, 0.5f)
                        .rotate((float) (Math.random() * Math.PI * 2), 0, 1, 0)
                        .interpolation(20, 0);
                supplies.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            if (tick % 4 == 0) {
                for (ItemDisplayHandle h : structure) {
                    if (h.entity() == null) continue;
                    Location pos = h.entity().getLocation();
                    w.spawnParticle(Particle.LARGE_SMOKE, pos.clone().add(0, 1.0, 0),
                            1, 0.3, 0.2, 0.3, 0.01);
                    w.spawnParticle(Particle.ASH, pos.clone().add(0, 0.8, 0),
                            2, 0.4, 0.3, 0.4, 0.02);
                    if (Math.random() < 0.15)
                        DisplayBuilder.dustParticles(pos.clone().add(0, 1.0, 0), 1, 0.3, 216, 208, 192, 0.9f);
                }
            }

            if (tick % 30 == 0 && !structure.isEmpty()) {
                int idx = (int) (Math.random() * structure.size());
                ItemDisplayHandle h = structure.get(idx);
                boolean phaseOut = ((tick / 30) + idx) % 2 == 0;
                float s = phaseOut ? 0.001f : 0.9f + (float) Math.random() * 0.8f;
                h.animateTo(new Vector3f(0, 0, 0),
                        new AxisAngle4f((float) (Math.random() * 0.6 - 0.3), 0, 0, 1),
                        new Vector3f(s, s * 1.2f, s), 20);
            }

            if (tick % 60 == 0) {
                for (int i = 0; i < supplies.size(); i++) {
                    supplies.get(i).animateTo(
                            new Vector3f((float) (Math.random() * 0.2 - 0.1) - 0.25f,
                                    0f,
                                    (float) (Math.random() * 0.2 - 0.1) - 0.25f),
                            new AxisAngle4f((float) (tick * 0.01 + i), 0, 1, 0),
                            new Vector3f(0.5f, 0.5f, 0.5f), 30);
                }
            }

            if (tick % 25 == 0) {
                Location p = getCenter().clone().add(
                        (Math.random() - 0.5) * 12, 1 + Math.random() * 3, (Math.random() - 0.5) * 12);
                DisplayBuilder.playSound(p, Sound.BLOCK_STONE_PLACE, 0.6f, 0.7f);
            }
            if (tick % 90 == 0)
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_IRON_GOLEM_HURT, 0.5f, 0.4f);
            if (tick % 50 == 0)
                DisplayBuilder.playSound(getCenter(), Sound.AMBIENT_CAVE, 0.4f, 0.6f);

            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(getCenter()) <= r2) {
                        p.damage(config.getDamage()); p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new ArchitectsNightmare(plugin); }
    }

    // ================================================================
    // 43. CELESTIAL WOUND
    //     Massive ragged hole in sky: ring of obsidian item-displays high
    //     overhead, narrow end_rod spotlight column descending to floor.
    // ================================================================
    public static class CelestialWound extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> wound = new ArrayList<>();
        private static final double SKY_HEIGHT = 16.0;

        public CelestialWound(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("celestial_wound", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.0);
            config.setDamageRadius(3.5);
            config.setTicksBetweenDamage(8);
            config.setDurationTicks(900);
            config.setCooldownTicks(1000);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.5f);
            for (int i = 0; i < 16; i++) {
                double a = Math.PI * 2 * i / 16;
                double r = 3.5 + Math.sin(a * 3) * 0.8 + Math.random() * 0.5;
                Location p = center.clone().add(Math.cos(a) * r, SKY_HEIGHT, Math.sin(a) * r);
                Material m = (i % 2 == 0) ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN;
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(m));
                float s = 1.2f + (float) Math.random() * 0.7f;
                h.scale(s, s, s).glow(60, 20, 120).interpolation(20, 0);
                wound.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            if (tick % 2 == 0) {
                for (int i = 0; i < 14; i++) {
                    double a = Math.PI * 2 * i / 14 + tick * 0.01;
                    double r = 3.8;
                    Location edge = getCenter().clone().add(
                            Math.cos(a) * r, SKY_HEIGHT, Math.sin(a) * r);
                    double ix = -Math.cos(a) * 0.3;
                    double iy = -0.4;
                    double iz = -Math.sin(a) * 0.3;
                    w.spawnParticle(Particle.PORTAL, edge, 3, ix, iy, iz, 1.0);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, edge, 1, ix * 0.5, iy * 0.5, iz * 0.5, 0.6);
                    w.spawnParticle(Particle.SQUID_INK, edge, 1, 0.2, 0.0, 0.2, 0.05);
                    w.spawnParticle(Particle.REVERSE_PORTAL, edge, 1, 0.2, 0.0, 0.2, 0.05);
                }
            }

            for (double y = 0.3; y < SKY_HEIGHT; y += 0.4) {
                Location p = getCenter().clone().add(
                        (Math.random() - 0.5) * 0.3, y, (Math.random() - 0.5) * 0.3);
                w.spawnParticle(Particle.END_ROD, p, 1, 0.02, 0.0, 0.02, 0.0);
                if (Math.random() < 0.1)
                    DisplayBuilder.dustParticles(p, 1, 0.1, 128, 48, 192, 1.2f);
            }

            if (tick % 10 == 0) {
                for (int i = 0; i < wound.size(); i++) {
                    double a = Math.PI * 2 * i / wound.size() + tick * 0.005;
                    double r = 3.5 + Math.sin(a * 3 + tick * 0.02) * 0.8;
                    float tx = (float) (Math.cos(a) * r);
                    float tz = (float) (Math.sin(a) * r);
                    wound.get(i).animateTo(
                            new Vector3f(tx - 0.6f, 0f, tz - 0.6f),
                            new AxisAngle4f((float) (tick * 0.02), 0, 1, 0),
                            new Vector3f(1.4f, 1.4f, 1.4f), 10);
                }
            }

            if (tick % 40 == 0)
                DisplayBuilder.playSound(getCenter().clone().add(0, SKY_HEIGHT, 0),
                        Sound.BLOCK_PORTAL_AMBIENT, 1.2f, 0.6f);
            if (tick % 600 == 0 && tick > 0)
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.9f, 0.5f);
            if (tick % 200 == 0)
                DisplayBuilder.playSound(getCenter().clone().add(0, SKY_HEIGHT, 0),
                        Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.8f, 0.5f);

            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(getCenter()) <= r2) {
                        p.damage(config.getDamage()); p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new CelestialWound(plugin); }
    }

    // ================================================================
    // 44. THE DROWNING ROOM
    //     Rising dark fog (large_smoke + squid_ink) up to ~3 blocks.
    //     8 ground-level soul lantern item-displays glow through fog.
    // ================================================================
    public static class DrowningRoom extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> lanterns = new ArrayList<>();

        public DrowningRoom(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("drowning_room", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(5.25);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(7);
            config.setDamageDelayTicks(50);
            config.setDurationTicks(400);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.AMBIENT_UNDERWATER_LOOP, 0.4f, 0.7f);
            for (int i = 0; i < 10; i++) {
                double a = Math.PI * 2 * i / 10;
                double r = 3 + (i % 2) * 2;
                Location p = center.clone().add(Math.cos(a) * r, 0.3, Math.sin(a) * r);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SOUL_LANTERN));
                h.scale(0.8f, 0.8f, 0.8f).glow(80, 200, 240).interpolation(20, 0);
                lanterns.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            double radius = config.getDamageRadius();
            double level = Math.min(3.0, (tick / (double) config.getDurationTicks()) * 3.0);

            if (tick % 2 == 0) {
                for (int i = 0; i < 36; i++) {
                    double ox = (Math.random() - 0.5) * radius * 2;
                    double oz = (Math.random() - 0.5) * radius * 2;
                    double oy = Math.random() * level;
                    Location p = getCenter().clone().add(ox, oy, oz);
                    w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.3, 0.05, 0.3, 0.01);
                    if (i % 3 == 0)
                        w.spawnParticle(Particle.SQUID_INK, p, 1, 0.2, 0.05, 0.2, 0.02);
                    if (i % 5 == 0)
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0.15, 0.1, 0.15, 0.02);
                    if (i % 6 == 0)
                        w.spawnParticle(Particle.SOUL, p, 1, 0.2, 0.1, 0.2, 0.03);
                    if (i % 8 == 0)
                        DisplayBuilder.dustParticles(p, 1, 0.2, 128, 24, 24, 1.2f);
                }
            }

            if (tick % 30 == 0) {
                for (int i = 0; i < lanterns.size(); i++) {
                    float yOff = (float) (Math.sin(tick * 0.03 + i) * 0.15);
                    lanterns.get(i).animateTo(
                            new Vector3f(-0.4f, yOff, -0.4f),
                            new AxisAngle4f((float) (tick * 0.01 + i), 0, 1, 0),
                            new Vector3f(0.8f, 0.8f, 0.8f), 30);
                }
            }

            if (tick % 60 == 0) {
                float vol = 0.4f + (float) (level / 3.0) * 0.8f;
                DisplayBuilder.playSound(getCenter(), Sound.AMBIENT_UNDERWATER_LOOP, vol, 0.7f);
            }
            if (tick % 80 == 0)
                DisplayBuilder.playSound(getCenter(), Sound.AMBIENT_UNDERWATER_LOOP_ADDITIONS, 0.6f, 0.5f);

            if (tick % config.getTicksBetweenDamage() == 0 && tick >= config.getDamageDelayTicks()) {
                double r2 = radius * radius;
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(getCenter()) <= r2) {
                        p.damage(config.getDamage()); p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new DrowningRoom(plugin); }
    }

    // ================================================================
    // 45. CURSED CANDELABRA ARRAY
    //     5x5 grid of blaze rod "poles" with red-candle item-displays on
    //     top. Candles flicker out/in; pattern slowly converges to an X.
    // ================================================================
    public static class CursedCandelabraArray extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> poles = new ArrayList<>();
        private final List<ItemDisplayHandle> candles = new ArrayList<>();
        private final boolean[] lit = new boolean[25];
        private final boolean[] finalPattern = {
                true, false, false, false, true,
                false, true, false, true, false,
                false, false, true, false, false,
                false, true, false, true, false,
                true, false, false, false, true
        };

        public CursedCandelabraArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cursed_candelabra_array", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.75);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(1000);
            config.setCooldownTicks(1100);
        }

        @Override
        protected void onSpawn(Location center) {
            for (int i = 0; i < 25; i++) lit[i] = true;
            DisplayBuilder.playSound(center, Sound.BLOCK_CANDLE_AMBIENT, 1.2f, 0.7f);
            for (int gx = 0; gx < 5; gx++) {
                for (int gz = 0; gz < 5; gz++) {
                    double x = (gx - 2) * 2.2;
                    double z = (gz - 2) * 2.2;
                    // tall pole = stacked blaze rod / end rod item-displays (3 high)
                    for (int y = 0; y < 3; y++) {
                        Location p = center.clone().add(x, 0.5 + y * 1.5, z);
                        Material polMat = (y == 1) ? Material.END_ROD : Material.BLAZE_ROD;
                        ItemDisplayHandle pole = displayBuilder.spawnItem(p, new ItemStack(polMat));
                        pole.scale(0.4f, 1.4f, 0.4f).glow(50, 30, 20).interpolation(15, 0);
                        poles.add(pole);
                    }
                    Location top = center.clone().add(x, 5.1, z);
                    ItemDisplayHandle c = displayBuilder.spawnItem(top, new ItemStack(Material.RED_CANDLE));
                    c.scale(0.8f, 0.8f, 0.8f).glow(255, 60, 30).interpolation(15, 0);
                    candles.add(c);
                }
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            if (tick % 2 == 0) {
                int idx = 0;
                for (int gx = 0; gx < 5; gx++) {
                    for (int gz = 0; gz < 5; gz++) {
                        if (lit[idx]) {
                            double x = (gx - 2) * 2.2;
                            double z = (gz - 2) * 2.2;
                            Location flame = getCenter().clone().add(x, 5.6, z);
                            w.spawnParticle(Particle.FLAME, flame, 1, 0.04, 0.1, 0.04, 0.005);
                            w.spawnParticle(Particle.SMALL_FLAME, flame, 1, 0.03, 0.05, 0.03, 0.002);
                            if (tick % 8 == 0)
                                w.spawnParticle(Particle.SMOKE, flame.clone().add(0, 0.4, 0), 1, 0.05, 0.05, 0.05, 0.01);
                            if (tick % 12 == 0)
                                DisplayBuilder.dustParticles(flame, 1, 0.05, 255, 60, 30, 0.6f);
                        }
                        idx++;
                    }
                }
            }

            if (tick > 0 && tick % 200 == 0) {
                double progress = Math.min(1.0, tick / (double) config.getDurationTicks());
                int targetIdx;
                if (progress < 0.7) {
                    targetIdx = (int) (Math.random() * 25);
                } else {
                    targetIdx = -1;
                    for (int i = 0; i < 25; i++) {
                        if (lit[i] && !finalPattern[i]) { targetIdx = i; break; }
                    }
                    if (targetIdx == -1) targetIdx = (int) (Math.random() * 25);
                }
                if (targetIdx >= 0 && targetIdx < 25) {
                    boolean wasLit = lit[targetIdx];
                    lit[targetIdx] = !wasLit;
                    ItemDisplayHandle c = candles.get(targetIdx);
                    int gx = targetIdx / 5; int gz = targetIdx % 5;
                    Location flame = getCenter().clone().add((gx - 2) * 2.2, 5.6, (gz - 2) * 2.2);
                    if (wasLit) {
                        c.glow(20, 20, 20);
                        c.animateTo(new Vector3f(-0.4f, 0f, -0.4f),
                                new AxisAngle4f(0f, 0, 1, 0),
                                new Vector3f(0.6f, 0.6f, 0.6f), 10);
                        DisplayBuilder.playSound(flame, Sound.ENTITY_BLAZE_DEATH, 1.0f, 1.4f);
                        for (int p = 0; p < 8; p++)
                            w.spawnParticle(Particle.LARGE_SMOKE, flame, 1, 0.1, 0.1, 0.1, 0.05);
                    } else {
                        c.glow(255, 60, 30);
                        c.animateTo(new Vector3f(-0.4f, 0f, -0.4f),
                                new AxisAngle4f(0f, 0, 1, 0),
                                new Vector3f(0.8f, 0.8f, 0.8f), 10);
                        DisplayBuilder.playSound(flame, Sound.ENTITY_BLAZE_AMBIENT, 0.9f, 1.6f);
                        w.spawnParticle(Particle.CRIT, flame, 16, 0.3, 0.3, 0.3, 0.4);
                    }
                }
            }

            if (tick % 80 == 0)
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_CANDLE_AMBIENT, 0.8f, 0.7f);

            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(getCenter()) <= r2) {
                        p.damage(config.getDamage()); p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new CursedCandelabraArray(plugin); }
    }

    // ================================================================
    // 46. THE DEVIL'S EXHALE
    //     Stillness phase, then huge directional cone blast of large_smoke
    //     + soul_fire_flame from center outward. Soul-fire item indicators
    //     telegraph cone direction. Impact damage on the wave moment.
    // ================================================================
    public static class DevilsExhale extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> indicators = new ArrayList<>();
        private double exhaleAngle = 0;
        private boolean impactFired = false;

        public DevilsExhale(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("devils_exhale", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(9.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(12);
            config.setDurationTicks(280);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            exhaleAngle = Math.random() * Math.PI * 2;
            for (int i = 0; i < 10; i++) {
                double r = 1 + i * 0.9;
                Location p = center.clone().add(
                        Math.cos(exhaleAngle) * r, 1.3, Math.sin(exhaleAngle) * r);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SOUL_LANTERN));
                h.scale(0.001f, 0.001f, 0.001f).glow(180, 80, 220).interpolation(20, 0);
                indicators.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            double radius = config.getDamageRadius();

            if (tick == 5)
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_NOTE_BLOCK_SNARE, 0.001f, 0.5f);

            if (tick < 50) {
                if (tick % 10 == 0) {
                    float grow = Math.min(1.0f, tick / 50.0f) * 0.8f;
                    for (int i = 0; i < indicators.size(); i++) {
                        indicators.get(i).animateTo(
                                new Vector3f(-0.5f, 0f, -0.5f),
                                new AxisAngle4f((float) (tick * 0.05 + i * 0.5), 0, 1, 0),
                                new Vector3f(grow * (0.4f + i * 0.05f), grow, grow * (0.4f + i * 0.05f)), 10);
                    }
                }
                if (tick % 4 == 0) {
                    double dx = Math.cos(exhaleAngle);
                    double dz = Math.sin(exhaleAngle);
                    for (int i = 0; i < 10; i++) {
                        double r = i * 0.9;
                        Location p = getCenter().clone().add(dx * r, 1.3, dz * r);
                        w.spawnParticle(Particle.SOUL, p, 1, 0.15, 0.1, 0.15, 0.02);
                        w.spawnParticle(Particle.SCULK_SOUL, p, 1, 0.1, 0.1, 0.1, 0.0);
                    }
                }
                if (tick == 30)
                    DisplayBuilder.playSound(getCenter(), Sound.BLOCK_BEACON_DEACTIVATE, 0.6f, 0.4f);
            } else if (tick < 90) {
                double dx = Math.cos(exhaleAngle);
                double dz = Math.sin(exhaleAngle);
                double progress = (tick - 50) / 40.0;
                double frontR = progress * (radius * 1.4);

                for (int i = 0; i < 60; i++) {
                    double r = Math.random() * frontR;
                    double spread = Math.random() * (Math.PI / 4);
                    double s = (Math.random() < 0.5 ? -1 : 1) * spread;
                    double a = exhaleAngle + s;
                    double y = 0.5 + Math.random() * 3.0;
                    Location p = getCenter().clone().add(
                            Math.cos(a) * r, y, Math.sin(a) * r);
                    w.spawnParticle(Particle.LARGE_SMOKE, p, 2, dx * 0.5, 0, dz * 0.5, 1.4);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 2, dx * 0.3, 0.05, dz * 0.3, 1.0);
                    if (i % 4 == 0)
                        w.spawnParticle(Particle.SOUL, p, 1, 0.15, 0.2, 0.15, 0.05);
                    if (i % 6 == 0)
                        DisplayBuilder.dustParticles(p, 1, 0.4, 128, 48, 192, 1.4f);
                }
                if (tick == 50) {
                    DisplayBuilder.playSound(getCenter(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.4f, 0.4f);
                    DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WITHER_SHOOT, 1.4f, 0.5f);
                }
                if (!impactFired && tick == 60) {
                    impactFired = true;
                    Location impact = getCenter().clone().add(dx * radius * 0.7, 1.0, dz * radius * 0.7);
                    triggerImpactDamage(impact);
                }
            } else {
                if (tick % 4 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double r = Math.random() * radius;
                        double a = exhaleAngle + (Math.random() - 0.5) * (Math.PI / 4);
                        Location p = getCenter().clone().add(Math.cos(a) * r, 0.5 + Math.random() * 2.5, Math.sin(a) * r);
                        w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.2, 0.1, 0.2, 0.02);
                    }
                }
                if (tick % 40 == 0)
                    DisplayBuilder.playSound(getCenter(), Sound.BLOCK_CAMPFIRE_CRACKLE, 0.6f, 0.6f);
                if (tick == 100) {
                    for (ItemDisplayHandle h : indicators) {
                        h.animateTo(new Vector3f(-0.5f, 0f, -0.5f),
                                new AxisAngle4f(0f, 0, 1, 0),
                                new Vector3f(0.001f, 0.001f, 0.001f), 30);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new DevilsExhale(plugin); }
    }

    // ================================================================
    // 47. THE PETRIFIED CONGREGATION
    //     12 humanoid figures (skeleton skull head + bone bodies) facing
    //     focal point. Soul flame rises beneath. Orbiting soul lantern
    //     above each. Heads turn to nearby player.
    // ================================================================
    public static class PetrifiedCongregation extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> bodies = new ArrayList<>();
        private final List<ItemDisplayHandle> heads = new ArrayList<>();
        private final List<ItemDisplayHandle> lanterns = new ArrayList<>();
        private final List<Location> figureLocs = new ArrayList<>();
        private static final int FIGURES = 12;

        public PetrifiedCongregation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("petrified_congregation", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(900);
            config.setCooldownTicks(1000);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.7f, 0.4f);
            for (int i = 0; i < FIGURES; i++) {
                double a = -Math.PI / 2 + (Math.PI * i / (FIGURES - 1));
                double r = 5 + (i % 2) * 0.8;
                Location p = center.clone().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                figureLocs.add(p);
                // body — bone item display, tall
                ItemDisplayHandle body = displayBuilder.spawnItem(p.clone().add(0, 0.5, 0), new ItemStack(Material.BONE));
                body.scale(0.8f, 1.6f, 0.8f).glow(216, 208, 192).interpolation(20, 0);
                bodies.add(body);
                // head — skeleton skull
                ItemDisplayHandle head = displayBuilder.spawnItem(p.clone().add(0, 1.45, 0), new ItemStack(Material.SKELETON_SKULL));
                double facingYaw = Math.atan2(center.getZ() - p.getZ(), center.getX() - p.getX());
                head.scale(0.8f, 0.8f, 0.8f)
                        .rotate((float) -facingYaw, 0, 1, 0)
                        .glow(216, 208, 192)
                        .interpolation(20, 0);
                heads.add(head);
                // floating soul lantern
                ItemDisplayHandle lantern = displayBuilder.spawnItem(
                        p.clone().add(0, 2.4, 0), new ItemStack(Material.SOUL_LANTERN));
                lantern.scale(0.5f, 0.5f, 0.5f).glow(80, 220, 240).interpolation(20, 0);
                lanterns.add(lantern);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            if (tick % 3 == 0) {
                for (Location p : figureLocs) {
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, p.clone().add(0, 0.1, 0),
                            2, 0.2, 0.05, 0.2, 0.01);
                    w.spawnParticle(Particle.SOUL, p.clone().add(0, 0.3, 0),
                            1, 0.1, 0.1, 0.1, 0.01);
                    if (Math.random() < 0.2)
                        w.spawnParticle(Particle.SCULK_SOUL, p.clone().add(0, 0.4, 0),
                                1, 0.15, 0.1, 0.15, 0.0);
                }
            }
            if (tick % 4 == 0) {
                for (int i = 0; i < 14; i++) {
                    double ox = (Math.random() - 0.5) * 14;
                    double oz = (Math.random() - 0.5) * 14;
                    double oy = 1 + Math.random() * 3;
                    w.spawnParticle(Particle.ASH, getCenter().clone().add(ox, oy, oz),
                            1, 0.3, 0.2, 0.3, 0.01);
                }
            }

            if (tick % 5 == 0) {
                for (int i = 0; i < lanterns.size(); i++) {
                    double a = tick * 0.04 + i * 0.5;
                    float ox = (float) (Math.cos(a) * 0.6);
                    float oz = (float) (Math.sin(a) * 0.6);
                    lanterns.get(i).animateTo(
                            new Vector3f(ox - 0.25f, 1.9f, oz - 0.25f),
                            new AxisAngle4f((float) a, 0, 1, 0),
                            new Vector3f(0.5f, 0.5f, 0.5f), 5);
                }
            }

            if (tick % 15 == 0) {
                Player target = getTargetPlayer();
                if (target != null) {
                    Location pl = target.getLocation();
                    for (int i = 0; i < heads.size(); i++) {
                        Location fl = figureLocs.get(i);
                        if (fl.distance(pl) < 4.0) {
                            double yaw = Math.atan2(pl.getZ() - fl.getZ(), pl.getX() - fl.getX());
                            float rot = (float) -yaw;
                            heads.get(i).animateTo(
                                    new Vector3f(-0.4f, 0.95f, -0.4f),
                                    new AxisAngle4f(rot, 0, 1, 0),
                                    new Vector3f(0.8f, 0.8f, 0.8f), 12);
                            if (Math.random() < 0.15)
                                DisplayBuilder.playSound(fl, Sound.ENTITY_ENDERMAN_STARE, 0.5f, 0.6f);
                        }
                    }
                }
            }

            if (tick % 70 == 0)
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.3f);
            if (tick % 110 == 0)
                DisplayBuilder.playSound(getCenter(), Sound.AMBIENT_CAVE, 0.6f, 0.4f);
        }

        @Override public AbstractAttack newInstance() { return new PetrifiedCongregation(plugin); }
    }

    // ================================================================
    // 48. THE TEMPORAL SHATTER
    //     3 frozen "moments": (a) mid-fall bone item at head height,
    //     (b) frozen lava droplet stream (8 magma cream items),
    //     (c) frozen smoke explosion sphere (12 black-dye items).
    // ================================================================
    public static class TemporalShatter extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> sphereDisplays = new ArrayList<>();
        private final List<ItemDisplayHandle> lavaStream = new ArrayList<>();
        private ItemDisplayHandle fallingBone;

        public TemporalShatter(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("temporal_shatter", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(1.5);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(800);
            config.setCooldownTicks(900);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.4f);

            // (a) frozen falling bone at head height, mid-tumble
            Location aLoc = center.clone().add(-3, 1.6, 0);
            fallingBone = displayBuilder.spawnItem(aLoc, new ItemStack(Material.BONE));
            fallingBone.scale(1.1f, 1.1f, 1.1f)
                    .rotate(0.6f, 1, 0.5f, 0)
                    .glow(220, 220, 200).interpolation(40, 0);

            // (b) frozen lava droplet stream
            for (int i = 0; i < 10; i++) {
                Location p = center.clone().add(2.5, 4.0 - i * 0.4, 1.5 + i * 0.2);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.MAGMA_CREAM));
                h.scale(0.45f, 0.65f, 0.45f).glow(255, 100, 30).interpolation(40, 0);
                lavaStream.add(h);
            }

            // (c) frozen explosion bloom — 14 obsidian item-displays in a sphere shell
            Location cLoc = center.clone().add(2, 1.5, -3);
            for (int i = 0; i < 14; i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 14);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                double sx = 2.0 * Math.sin(phi) * Math.cos(theta);
                double sy = 2.0 * Math.cos(phi);
                double sz = 2.0 * Math.sin(phi) * Math.sin(theta);
                Location p = cLoc.clone().add(sx, sy, sz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.OBSIDIAN));
                h.scale(0.55f, 0.55f, 0.55f).glow(60, 60, 60).interpolation(40, 0);
                sphereDisplays.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            if (tick % 3 == 0) {
                Location aLoc = getCenter().clone().add(-3, 1.6, 0);
                w.spawnParticle(Particle.PORTAL, aLoc.clone().add(0, 0.3, 0),
                        3, 0.6, 0.4, 0.6, 0.0);
                w.spawnParticle(Particle.ENCHANT, aLoc.clone().add(0, 0.3, 0),
                        2, 0.5, 0.4, 0.5, 0.0);
                if (Math.random() < 0.3)
                    w.spawnParticle(Particle.REVERSE_PORTAL, aLoc.clone().add(0, 0.3, 0),
                            1, 0.3, 0.3, 0.3, 0.05);
            }

            if (tick % 2 == 0) {
                for (int i = 0; i < lavaStream.size(); i++) {
                    Location p = getCenter().clone().add(2.5, 4.0 - i * 0.4, 1.5 + i * 0.2);
                    w.spawnParticle(Particle.LAVA, p, 1, 0.0, 0.0, 0.0, 0.0);
                    w.spawnParticle(Particle.DRIPPING_DRIPSTONE_LAVA, p.clone().add(0, 0.2, 0),
                            1, 0.05, 0.05, 0.05, 0.0);
                }
            }

            if (tick % 2 == 0) {
                Location cLoc = getCenter().clone().add(2, 1.5, -3);
                for (int i = 0; i < 14; i++) {
                    double phi = Math.acos(1 - 2.0 * (i + 0.5) / 14);
                    double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                    double sx = 2.0 * Math.sin(phi) * Math.cos(theta);
                    double sy = 2.0 * Math.cos(phi);
                    double sz = 2.0 * Math.sin(phi) * Math.sin(theta);
                    Location p = cLoc.clone().add(sx, sy, sz);
                    w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.05, 0.05, 0.05, 0.0);
                    if (i % 4 == 0)
                        DisplayBuilder.dustParticles(p, 1, 0.1, 60, 60, 60, 1.0f);
                }
            }

            if (tick % 25 == 0)
                DisplayBuilder.playSound(getCenter().clone().add(-3, 1.6, 0),
                        Sound.ENTITY_ENDERMAN_TELEPORT, 0.3f, 0.5f);
            if (tick % 35 == 0)
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_PORTAL_AMBIENT, 0.4f, 0.3f);

            // rare "skip" — frozen bone jitters
            if (tick % 200 == 50 && fallingBone != null) {
                fallingBone.animateTo(new Vector3f(-0.55f, 0f, -0.55f),
                        new AxisAngle4f(0.6f + (float) (Math.random() * 0.2 - 0.1), 1, 0.5f, 0),
                        new Vector3f(1.1f, 1.1f, 1.1f), 6);
            }

            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(getCenter()) <= r2) {
                        p.damage(config.getDamage()); p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new TemporalShatter(plugin); }
    }

    // ================================================================
    // 49. THE LULLABY MACHINE
    //     16 note block item-displays in 4x4 grid at mixed heights.
    //     Each pulses with NOTE particles independently. Dissonant chord
    //     every ~80t. Smoke wisps between.
    // ================================================================
    public static class LullabyMachine extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> notes = new ArrayList<>();
        private final List<Location> noteLocs = new ArrayList<>();
        private final float[] hues = new float[16];
        private final int[] timings = new int[16];

        public LullabyMachine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lullaby_machine", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(1000);
            config.setCooldownTicks(1100);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_HARP, 0.7f, 0.5f);
            int idx = 0;
            for (int gx = 0; gx < 4; gx++) {
                for (int gz = 0; gz < 4; gz++) {
                    double x = (gx - 1.5) * 1.8;
                    double z = (gz - 1.5) * 1.8;
                    double y = 1.5 + ((gx + gz) % 3) * 0.7;
                    Location p = center.clone().add(x, y, z);
                    noteLocs.add(p);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NOTE_BLOCK));
                    int rgb = (int) (Math.random() * 255);
                    h.scale(0.7f, 0.7f, 0.7f).glow(rgb, 255 - rgb, 200).interpolation(15, 0);
                    notes.add(h);
                    hues[idx] = (float) Math.random();
                    timings[idx] = 18 + (int) (Math.random() * 26);
                    idx++;
                }
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            for (int i = 0; i < 16; i++) {
                if (tick > 0 && tick % timings[i] == 0) {
                    Location p = noteLocs.get(i);
                    w.spawnParticle(Particle.NOTE, p.clone().add(0, 0.6, 0), 0, 0.0, 0.0, 0.0, hues[i]);
                    for (int k = 0; k < 4; k++) {
                        w.spawnParticle(Particle.NOTE, p.clone().add(
                                (Math.random() - 0.5) * 0.4, 0.6 + Math.random() * 0.6, (Math.random() - 0.5) * 0.4),
                                0, 0.0, 0.0, 0.0, hues[i]);
                    }
                    notes.get(i).animateTo(
                            new Vector3f(-0.45f, 0f, -0.45f),
                            new AxisAngle4f((float) (tick * 0.05 + i), 0, 1, 0),
                            new Vector3f(0.9f, 0.9f, 0.9f), 6);
                    Sound s;
                    int sel = i % 3;
                    if (sel == 0) s = Sound.BLOCK_NOTE_BLOCK_BASS;
                    else if (sel == 1) s = Sound.BLOCK_NOTE_BLOCK_HARP;
                    else s = Sound.BLOCK_NOTE_BLOCK_PLING;
                    float[] pitches = { 0.5f, 0.595f, 0.667f, 0.749f, 0.841f, 0.891f, 1.059f };
                    float pitch = pitches[i % pitches.length];
                    DisplayBuilder.playSound(p, s, 0.7f, pitch);
                    notes.get(i).animateTo(
                            new Vector3f(-0.35f, 0f, -0.35f),
                            new AxisAngle4f((float) (tick * 0.05 + i), 0, 1, 0),
                            new Vector3f(0.7f, 0.7f, 0.7f), 8);
                }
            }

            if (tick % 4 == 0) {
                for (int i = 0; i < 8; i++) {
                    Location p = noteLocs.get((int) (Math.random() * 16));
                    w.spawnParticle(Particle.SMOKE, p.clone().add(
                            (Math.random() - 0.5) * 0.6, 0.4, (Math.random() - 0.5) * 0.6),
                            1, 0.1, 0.2, 0.1, 0.005);
                    if (Math.random() < 0.2)
                        w.spawnParticle(Particle.WITCH, p.clone().add(0, 0.6, 0),
                                1, 0.2, 0.2, 0.2, 0.01);
                }
            }

            if (tick > 0 && tick % 80 == 0) {
                for (int i = 0; i < 16; i++) {
                    Location p = noteLocs.get(i);
                    for (int k = 0; k < 6; k++) {
                        w.spawnParticle(Particle.NOTE, p.clone().add(0, 0.6, 0),
                                0, 0.0, 0.0, 0.0, (i / 16.0f));
                    }
                    w.spawnParticle(Particle.ENCHANT, p.clone().add(0, 0.6, 0),
                            8, 0.2, 0.4, 0.2, 0.5);
                }
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.2f, 0.5f);
            }
        }

        @Override public AbstractAttack newInstance() { return new LullabyMachine(plugin); }
    }

    // ================================================================
    // 50. THE LAST DREAM
    //     Finale: 3 ticks of silence, then absolute chaos burst, then
    //     slow ambient state with a single dim soul lantern remaining.
    //     20 surface lanterns ignite simultaneously.
    // ================================================================
    public static class TheLastDream extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> lanterns = new ArrayList<>();
        private final List<Location> lanternLocs = new ArrayList<>();
        private ItemDisplayHandle lastLantern;
        private boolean burstFired = false;
        private boolean impactFired = false;

        public TheLastDream(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("last_dream", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(10);
            config.setDamageDelayTicks(15);
            config.setDurationTicks(1000);
            config.setCooldownTicks(1200);
        }

        @Override
        protected void onSpawn(Location center) {
            for (int i = 0; i < 20; i++) {
                double a = Math.PI * 2 * i / 20;
                double r = 6 + (i % 3) * 1.2;
                double y = (i % 4 == 0) ? 0.2 : (i % 4 == 1) ? 6.0 : (i % 4 == 2) ? 3.0 : 1.5;
                Location p = center.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
                lanternLocs.add(p);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SOUL_LANTERN));
                h.scale(0.001f, 0.001f, 0.001f).glow(80, 220, 240).interpolation(20, 0);
                lanterns.add(h);
            }
            lastLantern = displayBuilder.spawnItem(
                    center.clone().add(1.5, 1.0, 0), new ItemStack(Material.SOUL_LANTERN));
            lastLantern.scale(0.001f, 0.001f, 0.001f).glow(80, 220, 240).interpolation(20, 0);
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            double radius = config.getDamageRadius();

            if (tick == 0)
                DisplayBuilder.playSound(getCenter(), Sound.AMBIENT_CAVE, 0.001f, 0.1f);
            if (tick < 60) return;

            if (tick == 60 && !burstFired) {
                burstFired = true;
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WITHER_DEATH, 1.6f, 0.6f);
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_ENDER_DRAGON_DEATH, 1.6f, 0.5f);
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.4f);
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.4f, 0.6f);
                for (int i = 0; i < lanterns.size(); i++) {
                    lanterns.get(i).animateTo(
                            new Vector3f(-0.4f, 0f, -0.4f),
                            new AxisAngle4f((float) (Math.random() * Math.PI * 2), 0, 1, 0),
                            new Vector3f(0.8f, 0.8f, 0.8f), 6);
                }
                for (int i = 0; i < 260; i++) {
                    double ox = (Math.random() - 0.5) * radius * 2.4;
                    double oz = (Math.random() - 0.5) * radius * 2.4;
                    double oy = Math.random() * 8;
                    Location p = getCenter().clone().add(ox, oy, oz);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 3, 0.4, 0.4, 0.4, 0.3);
                    w.spawnParticle(Particle.END_ROD, p, 2, 0.5, 0.5, 0.5, 0.4);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, p, 2, 0.4, 0.4, 0.4, 0.3);
                    w.spawnParticle(Particle.PORTAL, p, 4, 0.6, 0.6, 0.6, 0.5);
                    if (i % 3 == 0)
                        w.spawnParticle(Particle.FLASH, p, 1, 0.0, 0.0, 0.0, 0.0);
                    if (i % 5 == 0)
                        w.spawnParticle(Particle.SCULK_CHARGE_POP, p, 1, 0.3, 0.3, 0.3, 0.2);
                    if (i % 7 == 0)
                        DisplayBuilder.dustParticles(p, 1, 0.5, 128, 48, 192, 1.6f);
                }
            }

            if (tick > 60 && tick < 90) {
                for (int i = 0; i < 100; i++) {
                    double ox = (Math.random() - 0.5) * radius * 2.4;
                    double oz = (Math.random() - 0.5) * radius * 2.4;
                    double oy = Math.random() * 8;
                    Location p = getCenter().clone().add(ox, oy, oz);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0.3, 0.3, 0.3, 0.2);
                    if (i % 2 == 0)
                        w.spawnParticle(Particle.END_ROD, p, 1, 0.4, 0.4, 0.4, 0.3);
                    if (i % 4 == 0)
                        w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.3, 0.3, 0.3, 0.2);
                }
                if (tick == 75 && !impactFired) {
                    impactFired = true;
                    triggerImpactDamage(getCenter().clone().add(0, 1, 0));
                }
            }

            if (tick == 90) {
                for (int i = 0; i < lanterns.size(); i++) {
                    lanterns.get(i).animateTo(
                            new Vector3f(-0.4f, 0f, -0.4f),
                            new AxisAngle4f(0f, 0, 1, 0),
                            new Vector3f(0.001f, 0.001f, 0.001f), 60);
                }
                if (lastLantern != null) {
                    lastLantern.animateTo(
                            new Vector3f(-0.4f, 0f, -0.4f),
                            new AxisAngle4f(0f, 0, 1, 0),
                            new Vector3f(0.8f, 0.8f, 0.8f), 30);
                }
            }

            if (tick > 90) {
                if (tick % 4 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double ox = (Math.random() - 0.5) * radius * 2;
                        double oz = (Math.random() - 0.5) * radius * 2;
                        double oy = Math.random() * 4;
                        w.spawnParticle(Particle.ASH, getCenter().clone().add(ox, oy, oz),
                                1, 0.3, 0.2, 0.3, 0.01);
                    }
                }
                if (lastLantern != null) {
                    if (tick % 6 == 0) {
                        Location p = getCenter().clone().add(1.5, 1.4, 0);
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0.05, 0.1, 0.05, 0.005);
                    }
                    if (tick % 80 == 0)
                        DisplayBuilder.playSound(getCenter().clone().add(1.5, 1.0, 0),
                                Sound.BLOCK_CANDLE_AMBIENT, 0.6f, 0.6f);
                    if (tick % 200 == 0) {
                        float remain = Math.max(0.05f, 0.8f * (1.0f - (tick - 90) / 900.0f));
                        lastLantern.animateTo(
                                new Vector3f(-0.4f, 0f, -0.4f),
                                new AxisAngle4f(0f, 0, 1, 0),
                                new Vector3f(remain, remain, remain), 80);
                    }
                }
            }

            if (tick % config.getTicksBetweenDamage() == 0 && tick >= config.getDamageDelayTicks()) {
                double r2 = radius * radius;
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(getCenter()) <= r2) {
                        p.damage(config.getDamage()); p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new TheLastDream(plugin); }
    }
}
